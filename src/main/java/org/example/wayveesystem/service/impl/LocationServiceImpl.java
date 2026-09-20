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
import org.example.wayveesystem.dto.response.OverpassResponse;
import org.example.wayveesystem.dto.response.NominatimReverseResponse;
import org.example.wayveesystem.mapper.OsmPlaceMapper;
import org.example.wayveesystem.mapper.PlaceResponseMapper;
import org.example.wayveesystem.service.LocationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationServiceImpl implements LocationService {

    private static final int MAX_GRID_RADIUS_METERS = 3000;
    private static final List<List<String>> CATEGORY_BATCHES = List.of(
            List.of("RESTAURANT", "CAFE", "FAST_FOOD"),
            List.of("BAR", "BAKERY", "SHOPPING"),
            List.of("ATTRACTION", "ACCOMMODATION")
    );

    private final OverpassApiClient overpassApiClient;
    private final RestClient nominatimRestClient;
    private final OsmFilterFactory osmFilterFactory;
    private final OsmPlaceMapper osmPlaceMapper;
    private final PlaceResponseMapper placeResponseMapper;
    private final CacheManager cacheManager;

    @Value("${wayvee.cache.grid-precision:0.02}")
    private double gridPrecision;

    @Override
    public List<LocationResponse> searchNearbyLocations(LocationFilterRequest request) {
        double userLat = request.lat();
        double userLng = request.lng();
        int requestedRadius = request.radiusMeters() != null ? request.radiusMeters() : 1000;

        double gridLat = GridCacheKey.snapToGrid(userLat, gridPrecision);
        double gridLng = GridCacheKey.snapToGrid(userLng, gridPrecision);
        GridCacheKey cacheKey = new GridCacheKey(gridLat, gridLng, MAX_GRID_RADIUS_METERS);

        // Fetch all cached POIs for grid (3km radius)
        List<OsmPlace> allGridPlaces = getFromCacheOrFetchAll(cacheKey);

        // Filter cached POIs in-memory using Java Stream
        return allGridPlaces.stream()
                .filter(place -> matchesDistance(place, userLat, userLng, requestedRadius))
                .filter(place -> matchesCategories(place, request.categories()))
                .filter(place -> matchesCuisines(place, request.cuisines()))
                .filter(place -> matchesMinRating(place, request.minRating()))
                .filter(place -> matchesKeyword(place, request.keyword()))
                .map(place -> placeResponseMapper.toResponse(place, userLat, userLng))
                .sorted(Comparator.comparingDouble(LocationResponse::distanceMeters))
                .toList();
    }

    @Override
    public LocationResponse getPlaceByOsmId(Long osmId) {
        String query = String.format("[out:json][timeout:10];(node(%d);way(%d););out center qt 1;", osmId, osmId);
        OverpassResponse response = overpassApiClient.fetchSingleElement(query);

        if (response == null || response.elements() == null || response.elements().isEmpty()) {
            throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
        }

        OsmPlace rawPlace = osmPlaceMapper.toOsmPlace(response.elements().getFirst());
        if (rawPlace == null) {
            throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
        }

        OsmPlace enrichedPlace = enrichPlaceData(rawPlace);
        return placeResponseMapper.toResponse(enrichedPlace, enrichedPlace.latitude(), enrichedPlace.longitude());
    }

    @Override
    @Cacheable(value = "reverseGeocodeCache", key = "T(java.lang.String).format('%.4f,%.4f', #lat, #lng)")
    public NominatimReverseResponse reverseGeocode(Double lat, Double lng) {
        if (lat == null || lng == null || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        try {
            log.info("Executing reverse geocode for coordinates: lat={}, lng={}", lat, lng);
            NominatimReverseResponse result = nominatimRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("format", "jsonv2")
                            .queryParam("lat", String.valueOf(lat))
                            .queryParam("lon", String.valueOf(lng))
                            .queryParam("addressdetails", "1")
                            .queryParam("accept-language", "vi")
                            .queryParam("zoom", "14")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        int statusCode = resp.getStatusCode().value();
                        log.warn("Nominatim reverse geocode returned HTTP {}", statusCode);
                        if (statusCode == 429) {
                            throw new AppException(ErrorCode.EXTERNAL_MAP_SERVICE_UNAVAILABLE);
                        } else if (statusCode == 403) {
                            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
                        }
                        throw new AppException(ErrorCode.INVALID_KEY);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        log.error("Nominatim reverse geocode server error HTTP {}", resp.getStatusCode().value());
                        throw new AppException(ErrorCode.EXTERNAL_MAP_SERVICE_UNAVAILABLE);
                    })
                    .body(NominatimReverseResponse.class);

            if (result == null) {
                throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
            }

            return result;
        } catch (HttpClientErrorException ex) {
            log.warn("HttpClientErrorException calling Nominatim: {}", ex.getStatusCode());
            throw new AppException(ErrorCode.EXTERNAL_MAP_SERVICE_UNAVAILABLE);
        } catch (ResourceAccessException ex) {
            log.warn("Timeout calling Nominatim reverse geocode: {}", ex.getMessage());
            throw new AppException(ErrorCode.EXTERNAL_MAP_TIMEOUT);
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error in reverse geocode: {}", ex.getMessage(), ex);
            throw new AppException(ErrorCode.EXTERNAL_MAP_SERVICE_UNAVAILABLE);
        }
    }

    @SuppressWarnings("unchecked")
    private List<OsmPlace> getFromCacheOrFetchAll(GridCacheKey cacheKey) {
        Cache poiCache = cacheManager.getCache("poiCache");
        if (poiCache != null) {
            Cache.ValueWrapper cached = poiCache.get(cacheKey);
            if (cached != null && cached.get() != null) {
                log.debug("Cache HIT for grid [{}, {}]", cacheKey.gridLat(), cacheKey.gridLng());
                return (List<OsmPlace>) cached.get();
            }
        }

        log.debug("Cache MISS for grid [{}, {}], fetching POIs in category batches from Overpass API (radius: {}m)",
                cacheKey.gridLat(), cacheKey.gridLng(), MAX_GRID_RADIUS_METERS);

        List<OsmPlace> combinedPlaces = new ArrayList<>();
        java.util.Set<Long> seenOsmIds = new java.util.HashSet<>();

        // Query Overpass in small category batches to prevent Overpass query timeouts
        for (List<String> batch : CATEGORY_BATCHES) {
            List<String> queryClauses = osmFilterFactory.buildFilterClauses(
                    "category", batch, cacheKey.gridLat(), cacheKey.gridLng(), MAX_GRID_RADIUS_METERS
            );

            OverpassRequest overpassRequest = OverpassRequest.of(
                    cacheKey.gridLat(), cacheKey.gridLng(), MAX_GRID_RADIUS_METERS, queryClauses
            );

            try {
                OverpassResponse overpassResponse = overpassApiClient.fetchNearbyPois(overpassRequest);
                if (overpassResponse != null && overpassResponse.elements() != null) {
                    for (var elem : overpassResponse.elements()) {
                        OsmPlace rawPlace = osmPlaceMapper.toOsmPlace(elem);
                        if (rawPlace != null && rawPlace.osmId() != null && seenOsmIds.add(rawPlace.osmId())) {
                            combinedPlaces.add(enrichPlaceData(rawPlace));
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Error fetching POI batch {} for grid [{}, {}]: {}", batch, cacheKey.gridLat(), cacheKey.gridLng(), ex.getMessage());
            }
        }

        if (poiCache != null) {
            poiCache.put(cacheKey, combinedPlaces);
        }

        return combinedPlaces;
    }

    /**
     * Enriches OsmPlace with internal repository/service metadata (ratings, price levels, etc.)
     */
    private OsmPlace enrichPlaceData(OsmPlace place) {
        if (place == null) {
            return null;
        }

        Map<String, String> tags = place.tags() != null ? new HashMap<>(place.tags()) : new HashMap<>();

        // Example internal rating calculation/enrichment if missing in OSM
        if (!tags.containsKey("rating")) {
            double simulatedRating = 4.0 + (Math.abs(Objects.hashCode(place.osmId())) % 10) / 10.0;
            tags.put("rating", String.format(java.util.Locale.US, "%.1f", simulatedRating));
        }

        // Example price level enrichment (1 = $, 2 = $$, 3 = $$$, 4 = $$$$)
        if (!tags.containsKey("price_level")) {
            int priceLevel = 1 + (Math.abs(Objects.hashCode(place.osmId())) % 3);
            tags.put("price_level", String.valueOf(priceLevel));
        }

        return new OsmPlace(
                place.osmId(), place.name(), place.categoryCode(), place.address(),
                place.latitude(), place.longitude(), place.imageUrl(), place.openingHours(),
                place.phone(), place.website(), tags
        );
    }

    private boolean matchesDistance(OsmPlace place, double lat, double lng, int maxRadius) {
        if (place.latitude() == null || place.longitude() == null) {
            return false;
        }
        double dist = calculateDistanceMeters(lat, lng, place.latitude(), place.longitude());
        return dist <= maxRadius;
    }

    private boolean matchesCategories(OsmPlace place, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return true;
        }
        if (place.categoryCode() == null) {
            return false;
        }
        return categories.stream().anyMatch(cat -> cat.equalsIgnoreCase(place.categoryCode()));
    }

    private boolean matchesCuisines(OsmPlace place, List<String> cuisines) {
        if (cuisines == null || cuisines.isEmpty()) {
            return true;
        }
        if (place.tags() == null || !place.tags().containsKey("cuisine")) {
            return false;
        }
        String placeCuisine = place.tags().get("cuisine").toLowerCase();
        return cuisines.stream().anyMatch(c -> placeCuisine.contains(c.toLowerCase()));
    }

    private boolean matchesMinRating(OsmPlace place, Double minRating) {
        if (minRating == null) {
            return true;
        }
        if (place.tags() == null || !place.tags().containsKey("rating")) {
            return true;
        }
        try {
            double rating = Double.parseDouble(place.tags().get("rating"));
            return rating >= minRating;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private boolean matchesKeyword(OsmPlace place, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return (place.name() != null && place.name().toLowerCase().contains(lowerKeyword))
                || (place.address() != null && place.address().toLowerCase().contains(lowerKeyword));
    }

    private static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

