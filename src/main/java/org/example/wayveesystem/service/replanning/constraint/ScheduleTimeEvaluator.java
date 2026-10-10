package org.example.wayveesystem.service.replanning.constraint;

import org.example.wayveesystem.service.replanning.distance.DistanceMatrixCalculator;
import org.example.wayveesystem.service.replanning.model.DaySchedule;
import org.example.wayveesystem.service.replanning.model.ReplanLocationItem;
import org.example.wayveesystem.service.replanning.model.ScheduleEvaluationResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class ScheduleTimeEvaluator {

    public static final LocalTime DEFAULT_START_TIME = LocalTime.of(8, 0);
    public static final LocalTime DEFAULT_MAX_END_TIME = LocalTime.of(21, 0);

    public ScheduleEvaluationResult evaluate(DaySchedule schedule, DistanceMatrixCalculator distanceCalculator) {
        if (schedule == null || schedule.getLocations() == null || schedule.getLocations().isEmpty()) {
            return ScheduleEvaluationResult.builder()
                    .feasible(true)
                    .totalDistanceKm(0.0)
                    .totalTravelMinutes(0)
                    .totalStayMinutes(0)
                    .totalWaitingMinutes(0)
                    .finalDepartureTime(schedule != null && schedule.getStartTime() != null ? schedule.getStartTime() : DEFAULT_START_TIME)
                    .evaluatedLocations(new ArrayList<>())
                    .build();
        }

        LocalTime currentTime = schedule.getStartTime() != null ? schedule.getStartTime() : DEFAULT_START_TIME;
        LocalTime maxEndTime = schedule.getMaxEndTime() != null ? schedule.getMaxEndTime() : DEFAULT_MAX_END_TIME;

        double totalDistKm = 0.0;
        int totalTravelMin = 0;
        int totalStayMin = 0;
        int totalWaitingMin = 0;

        List<ReplanLocationItem> evaluated = new ArrayList<>();
        Double prevLat = schedule.getStartLatitude();
        Double prevLon = schedule.getStartLongitude();

        List<ReplanLocationItem> rawLocations = schedule.getLocations();

        for (int i = 0; i < rawLocations.size(); i++) {
            ReplanLocationItem original = rawLocations.get(i);
            ReplanLocationItem item = ReplanLocationItem.copyOf(original);
            item.setVisitOrder(i + 1);
            item.setPlannedDay(schedule.getDay());

            double curLat = item.getLatitude() != null ? item.getLatitude() : 0.0;
            double curLon = item.getLongitude() != null ? item.getLongitude() : 0.0;

            int travelMin = 0;
            if (prevLat != null && prevLon != null) {
                double dist = distanceCalculator.distanceKm(prevLat, prevLon, curLat, curLon);
                totalDistKm += dist;
                travelMin = distanceCalculator.durationMinutes(prevLat, prevLon, curLat, curLon);
                totalTravelMin += travelMin;
            }

            LocalTime arrivalTime = currentTime.plusMinutes(travelMin);
            item.setEstimatedArrivalTime(arrivalTime);

            LocalTime visitStartTime = arrivalTime;
            int waitingMin = 0;
            if (item.getOpenTime() != null && arrivalTime.isBefore(item.getOpenTime())) {
                waitingMin = (int) Duration.between(arrivalTime, item.getOpenTime()).toMinutes();
                visitStartTime = item.getOpenTime();
                totalWaitingMin += waitingMin;
            }

            // Check opening hours / closing time constraint
            if (item.getCloseTime() != null) {
                if (visitStartTime.isAfter(item.getCloseTime())) {
                    return ScheduleEvaluationResult.builder()
                            .feasible(false)
                            .rejectionReason(String.format("Location '%s' misses closing time %s (arrival/visit: %s)",
                                    item.getPlaceName(), item.getCloseTime(), visitStartTime))
                            .build();
                }
            }

            int stayMin = item.getStayDurationMinutes() != null && item.getStayDurationMinutes() > 0 ? item.getStayDurationMinutes() : 60;
            totalStayMin += stayMin;
            LocalTime departureTime = visitStartTime.plusMinutes(stayMin);

            // If departure is past day's max budget
            if (maxEndTime != null && departureTime.isAfter(maxEndTime)) {
                return ScheduleEvaluationResult.builder()
                        .feasible(false)
                        .rejectionReason(String.format("Schedule for Day %d exceeds maximum daily end time %s (finish at %s at '%s')",
                                schedule.getDay(), maxEndTime, departureTime, item.getPlaceName()))
                        .build();
            }

            evaluated.add(item);
            currentTime = departureTime;
            prevLat = curLat;
            prevLon = curLon;
        }

        return ScheduleEvaluationResult.builder()
                .feasible(true)
                .totalDistanceKm(totalDistKm)
                .totalTravelMinutes(totalTravelMin)
                .totalStayMinutes(totalStayMin)
                .totalWaitingMinutes(totalWaitingMin)
                .finalDepartureTime(currentTime)
                .evaluatedLocations(evaluated)
                .build();
    }
}
