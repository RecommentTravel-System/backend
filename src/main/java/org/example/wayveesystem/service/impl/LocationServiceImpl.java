package org.example.wayveesystem.service.impl;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.client.OverpassApiClient;

import org.example.wayveesystem.common.enums.LocationSource;
import org.example.wayveesystem.common.enums.PoiStatus;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.dto.GridCacheKey;
import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.dto.request.LocationFilterRequest;
import org.example.wayveesystem.dto.response.*;
import org.example.wayveesystem.mapper.OsmPlaceMapper;
import org.example.wayveesystem.mapper.PlaceResponseMapper;
import org.example.wayveesystem.model.Category;
import org.example.wayveesystem.model.PoiCatalog;
import org.example.wayveesystem.repository.CategoryRepository;
import org.example.wayveesystem.repository.PoiCatalogRepository;
import org.example.wayveesystem.repository.SavedPlaceRepository;

import org.example.wayveesystem.service.LocationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationServiceImpl implements LocationService {

    private static final int MAX_GRID_RADIUS_METERS = 10000;
    private static final long CATEGORY_CACHE_TTL_MS = 300_000L; // 5 mins

    private final OverpassApiClient overpassApiClient;
    private final RestClient nominatimRestClient;
    private final OsmPlaceMapper osmPlaceMapper;
    private final PlaceResponseMapper placeResponseMapper;

    private final SavedPlaceRepository savedPlaceRepository;
    private final PoiCatalogRepository poiCatalogRepository;
    private final CategoryRepository categoryRepository;
    private final AsyncLoadingCache<GridCacheKey, List<OsmPlace>> poiAsyncLoadingCache;

    private volatile Map<Long, String> cachedCategoryMap = null;
    private volatile long lastCategoryLoadTime = 0L;

    @Value("${wayvee.cache.grid-precision:0.02}")
    private double gridPrecision;

    @Override
    public LocationPageResponse searchNearbyLocations(LocationFilterRequest request) {
        double userLat = request.lat();
        double userLng = request.lng();
        int requestedRadius = request.radiusMeters() != null ? request.radiusMeters() : 1000;

        // 1. Ưu tiên tìm kiếm từ Database (poi_catalog) đã import
        List<OsmPlace> dbPlaces = searchFromDatabase(request, userLat, userLng, requestedRadius);

        List<OsmPlace> candidatePlaces;
        if (!dbPlaces.isEmpty()) {
            log.info("Found {} matching POIs in database for coords [{}, {}], radius={}m",
                    dbPlaces.size(), userLat, userLng, requestedRadius);
            candidatePlaces = dbPlaces;
        } else {
            // 2. Fallback: Nếu trong DB không có thì mới gọi Overpass API
            log.info("No POIs found in database for coords [{}, {}]. Falling back to Overpass API cache/fetcher",
                    userLat, userLng);
            double gridLat = GridCacheKey.snapToGrid(userLat, gridPrecision);
            double gridLng = GridCacheKey.snapToGrid(userLng, gridPrecision);
            GridCacheKey cacheKey = new GridCacheKey(gridLat, gridLng, MAX_GRID_RADIUS_METERS);

            List<OsmPlace> allGridPlaces = getFromCacheOrFetchAll(cacheKey);

            candidatePlaces = allGridPlaces.stream()
                    .map(this::enrichPlaceData)
                    .filter(place -> matchesDistance(place, userLat, userLng, requestedRadius))
                    .filter(place -> matchesCategories(place, request.categories()))
                    .filter(place -> matchesCuisines(place, request.cuisines()))
                    .filter(place -> matchesMinRating(place, request.minRating()))
                    .filter(place -> matchesKeyword(place, request.keyword()))
                    .sorted(Comparator.comparingDouble(place -> calculateDistanceMeters(
                            userLat, userLng, place.latitude(), place.longitude())))
                    .toList();
        }

        int totalElements = candidatePlaces.size();
        int fromIndex = Math.min(request.page() * request.size(), totalElements);
        int toIndex = Math.min(fromIndex + request.size(), totalElements);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / request.size());
        List<LocationResponse> pageLocations = candidatePlaces.subList(fromIndex, toIndex).parallelStream()
                .map(place -> placeResponseMapper.toResponse(place, userLat, userLng))
                .toList();

        return new LocationPageResponse(
                pageLocations,
                request.page(), request.size(), totalElements, totalPages
        );
    }

    /**
     * Tìm kiếm POI trong Database theo bounding box toạ độ, ánh xạ Category và áp dụng các bộ lọc.
     */
    private List<OsmPlace> searchFromDatabase(LocationFilterRequest request, double userLat, double userLng, int requestedRadius) {
        double latDelta = requestedRadius / 111320.0;
        double lngDelta = requestedRadius / (111320.0 * Math.max(0.0001, Math.cos(Math.toRadians(userLat))));

        float minLat = (float) (userLat - latDelta);
        float maxLat = (float) (userLat + latDelta);
        float minLon = (float) (userLng - lngDelta);
        float maxLon = (float) (userLng + lngDelta);

        List<PoiCatalog> pois = poiCatalogRepository.findByLatBetweenAndLonBetweenAndStatus(
                minLat, maxLat, minLon, maxLon, PoiStatus.ACTIVE
        );

        if (pois == null || pois.isEmpty()) {
            return List.of();
        }

        Map<Long, String> categoryMap = loadCategoryMap();

        return pois.stream()
                .map(poi -> {
                    String categoryCode = poi.getCategoryId() != null ? categoryMap.getOrDefault(poi.getCategoryId(), "OTHER") : "OTHER";
                    return placeResponseMapper.toOsmPlace(poi, categoryCode);
                })
                .filter(Objects::nonNull)
                .map(this::enrichPlaceData)
                .filter(place -> matchesDistance(place, userLat, userLng, requestedRadius))
                .filter(place -> matchesCategories(place, request.categories()))
                .filter(place -> matchesCuisines(place, request.cuisines()))
                .filter(place -> matchesMinRating(place, request.minRating()))
                .filter(place -> matchesKeyword(place, request.keyword()))
                .sorted(Comparator.comparingDouble(place -> calculateDistanceMeters(
                        userLat, userLng, place.latitude(), place.longitude())))
                .toList();
    }

    private Map<Long, String> loadCategoryMap() {
        long now = System.currentTimeMillis();
        if (cachedCategoryMap != null && (now - lastCategoryLoadTime) < CATEGORY_CACHE_TTL_MS) {
            return cachedCategoryMap;
        }
        Map<Long, String> map = new HashMap<>();
        for (Category cat : categoryRepository.findAll()) {
            if (cat.getCategoryId() != null && cat.getCode() != null) {
                map.put(cat.getCategoryId(), cat.getCode().toUpperCase());
            }
        }
        cachedCategoryMap = map;
        lastCategoryLoadTime = now;
        return map;
    }

    /**
     * Core fix for concurrent-user timeouts:
     * AsyncLoadingCache.get(key) returns the SAME in-flight CompletableFuture
     * to every caller requesting the same grid key. If 100 users hit an empty
     * grid at once, only ONE fetchAllCategoriesForGrid() runs; the other 99
     * simply await the same future — no duplicate Overpass calls, no stampede.
     */
    private List<OsmPlace> getFromCacheOrFetchAll(GridCacheKey cacheKey) {
        try {
            return poiAsyncLoadingCache.get(cacheKey).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.EXTERNAL_MAP_SERVICE_UNAVAILABLE);
        } catch (ExecutionException | CompletionException e) {
            log.error("Failed to load POIs for grid [{}, {}]: {}",
                    cacheKey.gridLat(), cacheKey.gridLng(), e.getMessage());
            throw new AppException(ErrorCode.EXTERNAL_MAP_SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public LocationResponse getPlaceByOsmId(Long osmId) {
        if (osmId == null) {
            throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
        }

        // 1. Kiểm tra trong DB (poi_catalog) trước
        Optional<PoiCatalog> poiOpt = poiCatalogRepository.findBySourceAndSourceId(LocationSource.OSM, String.valueOf(osmId));
        if (poiOpt.isEmpty()) {
            poiOpt = poiCatalogRepository.findById(osmId);
        }

        if (poiOpt.isPresent()) {
            PoiCatalog poi = poiOpt.get();
            Map<Long, String> categoryMap = loadCategoryMap();
            String categoryCode = poi.getCategoryId() != null ? categoryMap.getOrDefault(poi.getCategoryId(), "OTHER") : "OTHER";
            OsmPlace rawPlace = placeResponseMapper.toOsmPlace(poi, categoryCode);
            if (rawPlace != null) {
                return buildEnrichedPlaceResponse(rawPlace, osmId);
            }
        }

        // 2. Nếu DB không có, fallback sang Overpass
        String query = String.format("[out:json][timeout:10];(node(%d);way(%d););out center qt 1;", osmId, osmId);
        OverpassResponse response = overpassApiClient.fetchSingleElement(query);

        if (response == null || response.elements() == null || response.elements().isEmpty()) {
            throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
        }

        OsmPlace rawPlace = osmPlaceMapper.toOsmPlace(response.elements().getFirst());
        if (rawPlace == null) {
            throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
        }

        return buildEnrichedPlaceResponse(rawPlace, osmId);
    }

    private LocationResponse buildEnrichedPlaceResponse(OsmPlace rawPlace, Long osmId) {
        OsmPlace enrichedPlace = enrichPlaceData(rawPlace);
        // Không cần resolve image nữa — FE sử dụng icon theo category
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
                        if (statusCode == 429) throw new AppException(ErrorCode.EXTERNAL_MAP_SERVICE_UNAVAILABLE);
                        if (statusCode == 403) throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
                        throw new AppException(ErrorCode.INVALID_KEY);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        log.error("Nominatim reverse geocode server error HTTP {}", resp.getStatusCode().value());
                        throw new AppException(ErrorCode.EXTERNAL_MAP_SERVICE_UNAVAILABLE);
                    })
                    .body(NominatimReverseResponse.class);

            if (result == null) throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
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

    private OsmPlace enrichPlaceData(OsmPlace place) {
        if (place == null) return null;
        Map<String, String> tags = place.tags() != null ? new HashMap<>(place.tags()) : new HashMap<>();

        if (!tags.containsKey("rating")) {
            double simulatedRating = 4.0 + (Math.abs(Objects.hashCode(place.osmId())) % 10) / 10.0;
            tags.put("rating", String.format(Locale.US, "%.1f", simulatedRating));
        }
        if (!tags.containsKey("price_level")) {
            int priceLevel = 1 + (Math.abs(Objects.hashCode(place.osmId())) % 3);
            tags.put("price_level", String.valueOf(priceLevel));
        }
        return new OsmPlace(
                place.osmId(), place.name(), place.categoryCode(), place.address(),
                place.latitude(), place.longitude(), place.imageUrl(), place.imageSource(), place.openingHours(),
                place.phone(), place.website(), tags
        );
    }



    private boolean matchesDistance(OsmPlace place, double lat, double lng, int maxRadius) {
        if (place.latitude() == null || place.longitude() == null) return false;
        return calculateDistanceMeters(lat, lng, place.latitude(), place.longitude()) <= maxRadius;
    }

    private boolean matchesCategories(OsmPlace place, List<String> categories) {
        if (categories == null || categories.isEmpty()) return true;
        if (place.categoryCode() == null) return false;
        return categories.stream().anyMatch(cat -> cat.equalsIgnoreCase(place.categoryCode()));
    }

    private boolean matchesCuisines(OsmPlace place, List<String> cuisines) {
        if (cuisines == null || cuisines.isEmpty()) return true;
        if (place.tags() == null || !place.tags().containsKey("cuisine")) return false;
        String placeCuisine = place.tags().get("cuisine").toLowerCase();
        return cuisines.stream().anyMatch(c -> placeCuisine.contains(c.toLowerCase()));
    }

    private boolean matchesMinRating(OsmPlace place, Double minRating) {
        if (minRating == null) return true;
        if (place.tags() == null || !place.tags().containsKey("rating")) return true;
        try {
            return Double.parseDouble(place.tags().get("rating")) >= minRating;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private boolean matchesKeyword(OsmPlace place, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
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
