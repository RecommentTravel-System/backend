package org.example.wayveesystem.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.client.OverpassApiClient;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.common.strategy.OsmFilterFactory;
import org.example.wayveesystem.dto.request.LocationFilterRequest;
import org.example.wayveesystem.dto.request.OverpassRequest;
import org.example.wayveesystem.dto.response.LocationResponse;
import org.example.wayveesystem.dto.response.OverpassElementResponse;
import org.example.wayveesystem.dto.response.OverpassResponse;
import org.example.wayveesystem.mapper.LocationMapper;
import org.example.wayveesystem.mapper.OsmPlaceMapper;
import org.example.wayveesystem.model.Location;
import org.example.wayveesystem.respository.LocationRepository;
import org.example.wayveesystem.service.LocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationServiceImpl implements LocationService {

    private final OverpassApiClient overpassApiClient;
    private final OsmFilterFactory osmFilterFactory;
    private final OsmPlaceMapper osmPlaceMapper;
    private final LocationMapper locationMapper;
    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public List<LocationResponse> searchNearbyLocations(LocationFilterRequest request) {
        double lat = request.lat();
        double lng = request.lng();
        int radius = request.radiusMeters() != null ? request.radiusMeters() : 1000;

        List<String> categories = (request.categories() != null && !request.categories().isEmpty())
                ? request.categories()
                : List.of("RESTAURANT", "CAFE", "FAST_FOOD");

        List<String> queryClauses = new ArrayList<>();
        queryClauses.addAll(osmFilterFactory.buildFilterClauses("category", categories, lat, lng, radius));

        if (request.cuisines() != null && !request.cuisines().isEmpty()) {
            queryClauses.addAll(osmFilterFactory.buildFilterClauses("cuisine", request.cuisines(), lat, lng, radius));
        }

        OverpassRequest overpassRequest = OverpassRequest.of(lat, lng, radius, queryClauses);
        OverpassResponse overpassResponse = overpassApiClient.fetchNearbyPois(overpassRequest);

        List<Location> mappedLocations = new ArrayList<>();
        if (overpassResponse != null && overpassResponse.elements() != null) {
            for (OverpassElementResponse element : overpassResponse.elements()) {
                Location mappedLocation = osmPlaceMapper.toLocation(element);
                if (mappedLocation != null) {
                    mappedLocations.add(mappedLocation);
                }
            }
        }

        List<Location> savedLocations = batchSyncWithDatabase(mappedLocations);

        return savedLocations.stream()
                .filter(loc -> matchesKeyword(loc, request.keyword()))
                .filter(loc -> matchesMinRating(loc, request.minRating()))
                .map(loc -> locationMapper.toResponse(loc, lat, lng))
                .sorted(Comparator.comparingDouble(LocationResponse::distanceMeters))
                .toList();
    }

    @Override
    public LocationResponse getLocationById(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
        return locationMapper.toResponse(location, location.getLatitude(), location.getLongitude());
    }

    private List<Location> batchSyncWithDatabase(List<Location> mappedLocations) {
        if (mappedLocations.isEmpty()) {
            return List.of();
        }

        Map<Long, Location> uniqueMapped = new LinkedHashMap<>();
        List<Location> withoutOsmId = new ArrayList<>();
        for (Location loc : mappedLocations) {
            if (loc.getSourceOsmId() != null) {
                uniqueMapped.putIfAbsent(loc.getSourceOsmId(), loc);
            } else {
                withoutOsmId.add(loc);
            }
        }

        List<Long> osmIds = new ArrayList<>(uniqueMapped.keySet());

        Map<Long, Location> existingMap = locationRepository.findBySourceOsmIdIn(osmIds).stream()
                .collect(Collectors.toMap(Location::getSourceOsmId, loc -> loc, (existing, replacement) -> existing));

        List<Location> toSave = new ArrayList<>();
        for (Location loc : uniqueMapped.values()) {
            Location existing = existingMap.get(loc.getSourceOsmId());
            if (existing != null) {
                existing.setName(loc.getName());
                existing.setAddress(loc.getAddress());
                existing.setLatitude(loc.getLatitude());
                existing.setLongitude(loc.getLongitude());
                toSave.add(existing);
            } else {
                toSave.add(loc);
            }
        }
        toSave.addAll(withoutOsmId);

        return locationRepository.saveAll(toSave);
    }

    private boolean matchesKeyword(Location location, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return (location.getName() != null && location.getName().toLowerCase().contains(lowerKeyword))
                || (location.getAddress() != null && location.getAddress().toLowerCase().contains(lowerKeyword));
    }

    private boolean matchesMinRating(Location location, Double minRating) {
        if (minRating == null || minRating <= 0) {
            return true;
        }
        return location.getRating() != null && location.getRating() >= minRating;
    }
}
