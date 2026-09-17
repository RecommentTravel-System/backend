package org.example.wayveesystem.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.client.OverpassApiClient;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.common.strategy.OsmFilterFactory;
import org.example.wayveesystem.dto.GridCacheKey;
import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.dto.request.LocationFilterRequest;
import org.example.wayveesystem.dto.request.OverpassRequest;
import org.example.wayveesystem.dto.response.LocationResponse;
import org.example.wayveesystem.dto.response.OverpassElementResponse;
import org.example.wayveesystem.dto.response.OverpassResponse;
import org.example.wayveesystem.mapper.OsmPlaceMapper;
import org.example.wayveesystem.mapper.PlaceResponseMapper;
import org.example.wayveesystem.service.LocationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationServiceImpl implements LocationService {

    private final OverpassApiClient overpassApiClient;
    private final OsmFilterFactory osmFilterFactory;
    private final OsmPlaceMapper osmPlaceMapper;
    private final PlaceResponseMapper placeResponseMapper;
    private final CacheManager cacheManager;

    @Value("${wayvee.cache.grid-precision}")
    private double gridPrecision;

    @Override
    public List<LocationResponse> searchNearbyLocations(LocationFilterRequest request) {
        double lat = request.lat();
        double lng = request.lng();
        int radius = request.radiusMeters() != null ? request.radiusMeters() : 1000;

        List<String> categories = (request.categories() != null && !request.categories().isEmpty())
                ? request.categories().stream().sorted().toList()
                : List.of("CAFE", "FAST_FOOD", "RESTAURANT");

        List<String> cuisines = (request.cuisines() != null && !request.cuisines().isEmpty())
                ? request.cuisines().stream().sorted().toList()
                : List.of();

        // Snap coordinates to grid for better cache hit rate
        double gridLat = GridCacheKey.snapToGrid(lat, gridPrecision);
        double gridLng = GridCacheKey.snapToGrid(lng, gridPrecision);
        GridCacheKey cacheKey = new GridCacheKey(gridLat, gridLng, categories, cuisines, radius);

        // Check cache first
        List<OsmPlace> places = getFromCacheOrFetch(cacheKey, lat, lng, radius, categories, cuisines);

        // Apply in-memory filters (keyword, minRating) and map to response
        return places.stream()
                .filter(place -> matchesKeyword(place, request.keyword()))
                .map(place -> placeResponseMapper.toResponse(place, lat, lng))
                .sorted(Comparator.comparingDouble(LocationResponse::distanceMeters))
                .toList();
    }

    @Override
    public LocationResponse getPlaceByOsmId(Long osmId) {
        // First try to find in cache
        Cache poiCache = cacheManager.getCache("poiCache");
        if (poiCache != null) {
            // Search through all cached entries is not efficient with Caffeine,
            // so we do a single-element Overpass lookup instead
        }

        // Fetch single element from Overpass
        String query = String.format("[out:json][timeout:10];(node(%d);way(%d););out center qt;", osmId, osmId);
        OverpassResponse response = overpassApiClient.fetchSingleElement(query);

        if (response == null || response.elements() == null || response.elements().isEmpty()) {
            throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
        }

        OsmPlace place = osmPlaceMapper.toOsmPlace(response.elements().getFirst());
        if (place == null) {
            throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
        }

        return placeResponseMapper.toResponse(place, place.latitude(), place.longitude());
    }

    @SuppressWarnings("unchecked")
    private List<OsmPlace> getFromCacheOrFetch(GridCacheKey cacheKey, double lat, double lng,
                                                int radius, List<String> categories, List<String> cuisines) {
        Cache poiCache = cacheManager.getCache("poiCache");
        if (poiCache != null) {
            Cache.ValueWrapper cached = poiCache.get(cacheKey);
            if (cached != null) {
                log.debug("Cache HIT for grid [{}, {}]", cacheKey.gridLat(), cacheKey.gridLng());
                return (List<OsmPlace>) cached.get();
            }
        }

        log.debug("Cache MISS for grid [{}, {}], calling Overpass API", cacheKey.gridLat(), cacheKey.gridLng());

        // Build Overpass query using grid center for better cache reuse
        List<String> queryClauses = new ArrayList<>();
        queryClauses.addAll(osmFilterFactory.buildFilterClauses("category", categories,
                cacheKey.gridLat(), cacheKey.gridLng(), radius));

        if (!cuisines.isEmpty()) {
            queryClauses.addAll(osmFilterFactory.buildFilterClauses("cuisine", cuisines,
                    cacheKey.gridLat(), cacheKey.gridLng(), radius));
        }

        OverpassRequest overpassRequest = OverpassRequest.of(cacheKey.gridLat(), cacheKey.gridLng(), radius, queryClauses);
        OverpassResponse overpassResponse = overpassApiClient.fetchNearbyPois(overpassRequest);

        List<OsmPlace> places;
        if (overpassResponse != null && overpassResponse.elements() != null) {
            places = overpassResponse.elements().stream()
                    .map(osmPlaceMapper::toOsmPlace)
                    .filter(Objects::nonNull)
                    .toList();
        } else {
            places = List.of();
        }

        // Store in cache
        if (poiCache != null) {
            poiCache.put(cacheKey, places);
        }

        return places;
    }

    private boolean matchesKeyword(OsmPlace place, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return (place.name() != null && place.name().toLowerCase().contains(lowerKeyword))
                || (place.address() != null && place.address().toLowerCase().contains(lowerKeyword));
    }
}
