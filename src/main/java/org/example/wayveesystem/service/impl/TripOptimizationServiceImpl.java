package org.example.wayveesystem.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.dto.request.ItineraryOptimizeRequest;
import org.example.wayveesystem.dto.request.OptimizePlaceItemDto;
import org.example.wayveesystem.dto.response.ItineraryOptimizeResponse;
import org.example.wayveesystem.dto.response.OptimizedDayDto;
import org.example.wayveesystem.service.TripOptimizationService;
import org.example.wayveesystem.service.replanning.constraint.ScheduleTimeEvaluator;
import org.example.wayveesystem.service.replanning.distance.DistanceMatrixCalculator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TripOptimizationServiceImpl implements TripOptimizationService {

    DistanceMatrixCalculator distanceCalculator;

    @Override
    public ItineraryOptimizeResponse optimizeItinerary(ItineraryOptimizeRequest request) {
        int totalDays = request.getTotalDays() != null && request.getTotalDays() > 0 ? request.getTotalDays() : 1;
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        LocalTime startTime = request.getDailyStartTime() != null ? request.getDailyStartTime() : ScheduleTimeEvaluator.DEFAULT_START_TIME;

        List<OptimizePlaceItemDto> rawPlaces = request.getPlaces() != null ? request.getPlaces() : Collections.emptyList();
        if (rawPlaces.isEmpty()) {
            List<OptimizedDayDto> emptyDays = new ArrayList<>();
            for (int d = 1; d <= totalDays; d++) {
                emptyDays.add(OptimizedDayDto.builder()
                        .day(d)
                        .date(startDate.plusDays(d - 1))
                        .startTime(startTime)
                        .endTime(startTime)
                        .totalDistanceKm(0.0)
                        .totalTravelMinutes(0)
                        .items(new ArrayList<>())
                        .build());
            }
            return ItineraryOptimizeResponse.builder()
                    .days(emptyDays)
                    .unassignedPlaces(new ArrayList<>())
                    .totalDistanceKm(0.0)
                    .totalTravelMinutes(0)
                    .build();
        }

        // 1. Cluster/Distribute items into K days
        Map<Integer, List<OptimizePlaceItemDto>> dayMap = clusterPlacesIntoDays(rawPlaces, totalDays);

        // 2. Order items within each day using Nearest Neighbor + 2-opt
        List<OptimizedDayDto> optimizedDays = new ArrayList<>();
        double grandTotalDistance = 0.0;
        int grandTotalTravelMinutes = 0;

        for (int d = 1; d <= totalDays; d++) {
            List<OptimizePlaceItemDto> dayItems = dayMap.getOrDefault(d, new ArrayList<>());
            List<OptimizePlaceItemDto> orderedItems = orderPlacesInDay(dayItems);

            // Compute travel times & arrival times
            double dayDistance = 0.0;
            int dayTravelMin = 0;
            LocalTime curTime = startTime;

            for (int i = 0; i < orderedItems.size(); i++) {
                OptimizePlaceItemDto item = orderedItems.get(i);
                int travelMin = 0;
                if (i > 0) {
                    OptimizePlaceItemDto prev = orderedItems.get(i - 1);
                    double dist = distance(prev, item);
                    dayDistance += dist;
                    travelMin = distanceCalculator.durationMinutes(
                            getLat(prev), getLon(prev),
                            getLat(item), getLon(item)
                    );
                    dayTravelMin += travelMin;
                }

                curTime = curTime.plusMinutes(travelMin);
                if (item.getOpenTime() != null && curTime.isBefore(item.getOpenTime())) {
                    curTime = item.getOpenTime();
                }

                int stayMin = item.getStayDurationMinutes() != null && item.getStayDurationMinutes() > 0 ? item.getStayDurationMinutes() : 60;
                curTime = curTime.plusMinutes(stayMin);
            }

            grandTotalDistance += dayDistance;
            grandTotalTravelMinutes += dayTravelMin;

            optimizedDays.add(OptimizedDayDto.builder()
                    .day(d)
                    .date(startDate.plusDays(d - 1))
                    .startTime(startTime)
                    .endTime(curTime)
                    .totalDistanceKm(round(dayDistance))
                    .totalTravelMinutes(dayTravelMin)
                    .items(orderedItems)
                    .build());
        }

        return ItineraryOptimizeResponse.builder()
                .days(optimizedDays)
                .unassignedPlaces(new ArrayList<>())
                .totalDistanceKm(round(grandTotalDistance))
                .totalTravelMinutes(grandTotalTravelMinutes)
                .build();
    }

    private Map<Integer, List<OptimizePlaceItemDto>> clusterPlacesIntoDays(List<OptimizePlaceItemDto> places, int totalDays) {
        Map<Integer, List<OptimizePlaceItemDto>> result = new HashMap<>();
        for (int d = 1; d <= totalDays; d++) {
            result.put(d, new ArrayList<>());
        }

        List<OptimizePlaceItemDto> unfixed = new ArrayList<>();
        for (OptimizePlaceItemDto p : places) {
            if (p.getFixedDay() != null && p.getFixedDay() >= 1 && p.getFixedDay() <= totalDays) {
                result.get(p.getFixedDay()).add(p);
            } else {
                unfixed.add(p);
            }
        }

        if (unfixed.isEmpty()) {
            return result;
        }

        // If totalDays == 1, all unfixed go to day 1
        if (totalDays == 1) {
            result.get(1).addAll(unfixed);
            return result;
        }

        // Balanced K-means clustering
        // Check if places have valid lat/lon
        boolean hasGeo = unfixed.stream().anyMatch(p -> p.getLatitude() != null && p.getLongitude() != null && p.getLatitude() != 0.0);

        if (!hasGeo) {
            // Round robin distribution
            for (int i = 0; i < unfixed.size(); i++) {
                int day = (i % totalDays) + 1;
                result.get(day).add(unfixed.get(i));
            }
            return result;
        }

        // K-Means centroids
        List<double[]> centroids = initializeCentroids(unfixed, totalDays);
        int maxIterations = 20;

        for (int iter = 0; iter < maxIterations; iter++) {
            // Assign places to nearest centroid considering capacity
            int maxCap = (int) Math.ceil((double) places.size() / totalDays) + 1;
            Map<Integer, List<OptimizePlaceItemDto>> currentClusters = new HashMap<>();
            for (int d = 1; d <= totalDays; d++) currentClusters.put(d, new ArrayList<>());

            // Sort unfixed places by distance to their nearest centroid
            List<OptimizePlaceItemDto> sortedPlaces = new ArrayList<>(unfixed);
            sortedPlaces.sort(Comparator.comparingDouble(p -> minDistanceToCentroids(p, centroids)));

            for (OptimizePlaceItemDto p : sortedPlaces) {
                int bestCluster = 1;
                double minDist = Double.MAX_VALUE;

                for (int c = 0; c < centroids.size(); c++) {
                    int day = c + 1;
                    if (currentClusters.get(day).size() + result.get(day).size() >= maxCap) {
                        continue;
                    }
                    double dist = distanceToPoint(p, centroids.get(c)[0], centroids.get(c)[1]);
                    if (dist < minDist) {
                        minDist = dist;
                        bestCluster = day;
                    }
                }
                currentClusters.get(bestCluster).add(p);
            }

            // Update centroids
            boolean changed = false;
            for (int c = 0; c < centroids.size(); c++) {
                int day = c + 1;
                List<OptimizePlaceItemDto> clusterList = currentClusters.get(day);
                if (!clusterList.isEmpty()) {
                    double avgLat = clusterList.stream().mapToDouble(this::getLat).average().orElse(centroids.get(c)[0]);
                    double avgLon = clusterList.stream().mapToDouble(this::getLon).average().orElse(centroids.get(c)[1]);
                    if (Math.abs(avgLat - centroids.get(c)[0]) > 0.0001 || Math.abs(avgLon - centroids.get(c)[1]) > 0.0001) {
                        centroids.set(c, new double[]{avgLat, avgLon});
                        changed = true;
                    }
                }
            }

            if (!changed || iter == maxIterations - 1) {
                for (int d = 1; d <= totalDays; d++) {
                    result.get(d).addAll(currentClusters.get(d));
                }
                break;
            }
        }

        return result;
    }

    private List<double[]> initializeCentroids(List<OptimizePlaceItemDto> places, int k) {
        List<double[]> centroids = new ArrayList<>();
        if (places.isEmpty()) return centroids;

        // Pick first centroid
        centroids.add(new double[]{getLat(places.get(0)), getLon(places.get(0))});

        for (int i = 1; i < k; i++) {
            OptimizePlaceItemDto bestNext = places.get(0);
            double maxDist = -1;

            for (OptimizePlaceItemDto p : places) {
                double minDist = centroids.stream()
                        .mapToDouble(c -> distanceToPoint(p, c[0], c[1]))
                        .min().orElse(0.0);
                if (minDist > maxDist) {
                    maxDist = minDist;
                    bestNext = p;
                }
            }
            centroids.add(new double[]{getLat(bestNext), getLon(bestNext)});
        }
        return centroids;
    }

    private List<OptimizePlaceItemDto> orderPlacesInDay(List<OptimizePlaceItemDto> places) {
        if (places == null || places.size() <= 2) {
            return places != null ? new ArrayList<>(places) : new ArrayList<>();
        }

        // 1. Nearest Neighbor
        List<OptimizePlaceItemDto> remaining = new ArrayList<>(places);
        List<OptimizePlaceItemDto> tour = new ArrayList<>();

        // Start with the westernmost/first location
        OptimizePlaceItemDto current = remaining.remove(0);
        tour.add(current);

        while (!remaining.isEmpty()) {
            OptimizePlaceItemDto next = null;
            double minDist = Double.MAX_VALUE;
            int nextIdx = -1;

            for (int i = 0; i < remaining.size(); i++) {
                double d = distance(current, remaining.get(i));
                if (d < minDist) {
                    minDist = d;
                    next = remaining.get(i);
                    nextIdx = i;
                }
            }
            if (nextIdx >= 0) {
                tour.add(remaining.remove(nextIdx));
                current = next;
            }
        }

        // 2. 2-opt improvement
        boolean improved = true;
        int maxPasses = 30;
        int pass = 0;

        while (improved && pass < maxPasses) {
            improved = false;
            pass++;
            int n = tour.size();
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    double delta = compute2OptDelta(tour, i, j);
                    if (delta < -0.001) {
                        Collections.reverse(tour.subList(i, j + 1));
                        improved = true;
                        break;
                    }
                }
                if (improved) break;
            }
        }

        return tour;
    }

    private double compute2OptDelta(List<OptimizePlaceItemDto> tour, int i, int j) {
        int n = tour.size();
        OptimizePlaceItemDto a = i > 0 ? tour.get(i - 1) : null;
        OptimizePlaceItemDto b = tour.get(i);
        OptimizePlaceItemDto c = tour.get(j);
        OptimizePlaceItemDto d = j < n - 1 ? tour.get(j + 1) : null;

        double oldCost = 0.0;
        double newCost = 0.0;

        if (a != null) oldCost += distance(a, b);
        if (d != null) oldCost += distance(c, d);

        if (a != null) newCost += distance(a, c);
        if (d != null) newCost += distance(b, d);

        return newCost - oldCost;
    }

    private double distance(OptimizePlaceItemDto a, OptimizePlaceItemDto b) {
        return distanceCalculator.distanceKm(getLat(a), getLon(a), getLat(b), getLon(b));
    }

    private double distanceToPoint(OptimizePlaceItemDto p, double lat, double lon) {
        return distanceCalculator.distanceKm(getLat(p), getLon(p), lat, lon);
    }

    private double minDistanceToCentroids(OptimizePlaceItemDto p, List<double[]> centroids) {
        return centroids.stream()
                .mapToDouble(c -> distanceToPoint(p, c[0], c[1]))
                .min().orElse(0.0);
    }

    private double getLat(OptimizePlaceItemDto p) {
        return p.getLatitude() != null ? p.getLatitude() : 21.0285;
    }

    private double getLon(OptimizePlaceItemDto p) {
        return p.getLongitude() != null ? p.getLongitude() : 105.8542;
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
