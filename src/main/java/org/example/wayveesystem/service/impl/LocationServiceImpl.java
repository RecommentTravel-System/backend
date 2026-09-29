package org.example.wayveesystem.service.impl;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.client.OverpassApiClient;
import org.example.wayveesystem.common.enums.ImageSource;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.dto.GridCacheKey;
import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.dto.request.LocationFilterRequest;
import org.example.wayveesystem.dto.response.*;
import org.example.wayveesystem.mapper.OsmPlaceMapper;
import org.example.wayveesystem.mapper.PlaceResponseMapper;
import org.example.wayveesystem.repository.SavedPlaceRepository;
import org.example.wayveesystem.service.ImageResolverService;
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

    private final OverpassApiClient overpassApiClient;
    private final RestClient nominatimRestClient;
    private final OsmPlaceMapper osmPlaceMapper;
    private final PlaceResponseMapper placeResponseMapper;
    private final ImageResolverService imageResolverService;
    private final SavedPlaceRepository savedPlaceRepository;
    private final AsyncLoadingCache<GridCacheKey, List<OsmPlace>> poiAsyncLoadingCache;

    @Value("${wayvee.cache.grid-precision:0.02}")
    private double gridPrecision;

    @Override
    public LocationPageResponse searchNearbyLocations(LocationFilterRequest request) {
        double userLat = request.lat();
        double userLng = request.lng();
        int requestedRadius = request.radiusMeters() != null ? request.radiusMeters() : 1000;

        double gridLat = GridCacheKey.snapToGrid(userLat, gridPrecision);
        double gridLng = GridCacheKey.snapToGrid(userLng, gridPrecision);
        GridCacheKey cacheKey = new GridCacheKey(gridLat, gridLng, MAX_GRID_RADIUS_METERS);

        List<OsmPlace> allGridPlaces = getFromCacheOrFetchAll(cacheKey);

        List<OsmPlace> filteredPlaces = allGridPlaces.stream()
                .map(this::enrichPlaceData)
                .filter(place -> matchesDistance(place, userLat, userLng, requestedRadius))
                .filter(place -> matchesCategories(place, request.categories()))
                .filter(place -> matchesCuisines(place, request.cuisines()))
                .filter(place -> matchesMinRating(place, request.minRating()))
                .filter(place -> matchesKeyword(place, request.keyword()))
                .sorted(Comparator.comparingDouble(place -> calculateDistanceMeters(
                        userLat, userLng, place.latitude(), place.longitude())))
                .toList();

        int totalElements = filteredPlaces.size();
        int fromIndex = Math.min(request.page() * request.size(), totalElements);
        int toIndex = Math.min(fromIndex + request.size(), totalElements);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / request.size());
        List<LocationResponse> pageLocations = filteredPlaces.subList(fromIndex, toIndex).parallelStream()
                .map(this::resolveImage)
                .map(place -> placeResponseMapper.toResponse(place, userLat, userLng))
                .toList();

        return new LocationPageResponse(
                pageLocations,
                request.page(), request.size(), totalElements, totalPages
        );
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
        var savedPlace = savedPlaceRepository.findByOsmId(osmId).orElse(null);
        if (savedPlace != null && savedPlace.getImageUrl() != null && !savedPlace.getImageUrl().isBlank()) {
            enrichedPlace = withImage(enrichedPlace, savedPlace.getImageUrl(), savedPlace.getImageSource());
        } else {
            var resolution = imageResolverService.resolve(enrichedPlace);
            if (resolution.imageUrl() != null) {
                enrichedPlace = withImage(enrichedPlace, resolution.imageUrl(), resolution.imageSource().name());
                if (savedPlace != null) {
                    savedPlace.setImageUrl(resolution.imageUrl());
                    savedPlace.setImageSource(resolution.imageSource().name());
                    savedPlaceRepository.save(savedPlace);
                }
            }
        }
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

    private OsmPlace withImage(OsmPlace place, String imageUrl, String source) {
        ImageSource imageSource;
        try { imageSource = source == null ? null : ImageSource.valueOf(source); }
        catch (IllegalArgumentException ex) { imageSource = null; }
        return new OsmPlace(place.osmId(), place.name(), place.categoryCode(), place.address(), place.latitude(),
                place.longitude(), imageUrl, imageSource, place.openingHours(), place.phone(), place.website(), place.tags());
    }

    private OsmPlace resolveImage(OsmPlace place) {
        if (place.imageUrl() != null && !place.imageUrl().isBlank()) {
            return place;
        }
        var resolution = imageResolverService.resolve(place);
        return resolution.imageUrl() == null ? place
                : withImage(place, resolution.imageUrl(), resolution.imageSource().name());
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
