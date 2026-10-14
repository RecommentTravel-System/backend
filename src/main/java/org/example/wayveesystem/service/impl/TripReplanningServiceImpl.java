package org.example.wayveesystem.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.LocationPriority;
import org.example.wayveesystem.common.enums.ReplanAction;
import org.example.wayveesystem.common.enums.TripLocationStatus;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.dto.request.LocationStatusUpdateRequest;
import org.example.wayveesystem.dto.request.ReplanConfirmRequest;
import org.example.wayveesystem.dto.request.ReplanPreviewRequest;
import org.example.wayveesystem.dto.response.*;
import org.example.wayveesystem.model.LocationOpeningHour;
import org.example.wayveesystem.model.Trip;
import org.example.wayveesystem.model.TripLocation;
import org.example.wayveesystem.model.User;
import org.example.wayveesystem.repository.LocationOpeningHourRepository;
import org.example.wayveesystem.repository.TripLocationRepository;
import org.example.wayveesystem.repository.TripRepository;
import org.example.wayveesystem.repository.UserRepository;
import org.example.wayveesystem.service.TripReplanningService;
import org.example.wayveesystem.service.replanning.ReplanProposalStore;
import org.example.wayveesystem.service.replanning.constraint.ScheduleTimeEvaluator;
import org.example.wayveesystem.service.replanning.distance.DistanceMatrixCalculator;
import org.example.wayveesystem.service.replanning.model.*;
import org.example.wayveesystem.service.replanning.planner.InsertionPlanner;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TripReplanningServiceImpl implements TripReplanningService {

    TripRepository tripRepository;
    TripLocationRepository tripLocationRepository;
    UserRepository userRepository;
    LocationOpeningHourRepository openingHourRepository;
    DistanceMatrixCalculator distanceCalculator;
    InsertionPlanner insertionPlanner;
    ScheduleTimeEvaluator scheduleTimeEvaluator;
    ReplanProposalStore proposalStore;

    @Override
    @Transactional(readOnly = true)
    public ReplanProposalResponse previewReplan(Long tripId, ReplanPreviewRequest request) {
        Trip trip = getActiveTrip(tripId);
        List<TripLocation> allDbLocations = tripLocationRepository.findByTripOrderByPlannedDayAscVisitOrderAsc(trip);

        int currentDay = request.getCurrentDay() != null ? request.getCurrentDay() : 1;
        LocalDate startDate = trip.getStartDate() != null ? trip.getStartDate() : LocalDate.now();

        // 1. Group items by day and populate opening hours if not set
        Map<Integer, List<ReplanLocationItem>> itemsByDay = new TreeMap<>();
        int maxDay = currentDay;

        for (TripLocation loc : allDbLocations) {
            int day = loc.getPlannedDay() != null ? loc.getPlannedDay() : 1;
            if (day > maxDay) maxDay = day;

            ReplanLocationItem item = toReplanLocationItem(loc, startDate, day);

            // Apply overrides from request if any
            if (request.getCompletedLocationIds() != null && request.getCompletedLocationIds().contains(loc.getTripLocationId())) {
                item.setStatus(TripLocationStatus.VISITED);
                item.setVisitedAt(LocalDateTime.now());
            } else if (request.getSkippedLocationIds() != null && request.getSkippedLocationIds().contains(loc.getTripLocationId())) {
                item.setStatus(TripLocationStatus.SKIPPED);
            } else if (request.getPostponedLocationIds() != null && request.getPostponedLocationIds().contains(loc.getTripLocationId())) {
                item.setStatus(TripLocationStatus.POSTPONED);
            }

            itemsByDay.computeIfAbsent(day, k -> new ArrayList<>()).add(item);
        }

        // Calculate original baseline metrics
        double oldTotalDistance = 0.0;
        int oldTotalTravelTime = 0;
        for (int d = 1; d <= maxDay; d++) {
            List<ReplanLocationItem> dayLocs = itemsByDay.getOrDefault(d, Collections.emptyList());
            DaySchedule ds = DaySchedule.builder()
                    .day(d)
                    .date(startDate.plusDays(d - 1))
                    .locations(dayLocs)
                    .build();
            ScheduleEvaluationResult eval = scheduleTimeEvaluator.evaluate(ds, distanceCalculator);
            if (eval.isFeasible()) {
                oldTotalDistance += eval.getTotalDistanceKm();
                oldTotalTravelTime += eval.getTotalTravelMinutes();
            }
        }

        // 2. Identify frozen past days, unvisited locations U, and target days
        List<DaySchedule> finalSchedules = new ArrayList<>();
        List<ReplanLocationItem> unvisitedU = new ArrayList<>();
        List<DaySchedule> targetFutureDays = new ArrayList<>();

        // Past days (< currentDay): completely frozen
        for (int d = 1; d < currentDay; d++) {
            List<ReplanLocationItem> locs = itemsByDay.getOrDefault(d, new ArrayList<>());
            DaySchedule frozenDay = DaySchedule.builder()
                    .day(d)
                    .date(startDate.plusDays(d - 1))
                    .locations(locs)
                    .build();
            ScheduleEvaluationResult eval = scheduleTimeEvaluator.evaluate(frozenDay, distanceCalculator);
            if (eval.getEvaluatedLocations() != null) {
                frozenDay.setLocations(eval.getEvaluatedLocations());
            }
            finalSchedules.add(frozenDay);
        }

        // Current Day processing
        List<ReplanLocationItem> currentDayItems = itemsByDay.getOrDefault(currentDay, new ArrayList<>());
        boolean isRollingHorizon = request.getCurrentLatitude() != null && request.getCurrentLongitude() != null && request.getCurrentTime() != null;

        if (isRollingHorizon) {
            // Rolling horizon: keep VISITED, evaluate remaining PLANNED
            List<ReplanLocationItem> currentVisited = new ArrayList<>();
            List<ReplanLocationItem> currentRemaining = new ArrayList<>();

            for (ReplanLocationItem item : currentDayItems) {
                if (item.getStatus() == TripLocationStatus.VISITED || item.getStatus() == TripLocationStatus.SKIPPED) {
                    currentVisited.add(item);
                } else {
                    currentRemaining.add(item);
                }
            }

            DaySchedule rollingDay = DaySchedule.builder()
                    .day(currentDay)
                    .date(startDate.plusDays(currentDay - 1))
                    .startTime(request.getCurrentTime())
                    .startLatitude(request.getCurrentLatitude())
                    .startLongitude(request.getCurrentLongitude())
                    .locations(currentRemaining)
                    .build();

            ScheduleEvaluationResult rollingEval = scheduleTimeEvaluator.evaluate(rollingDay, distanceCalculator);
            if (rollingEval.isFeasible()) {
                // All fit in current day
                List<ReplanLocationItem> combined = new ArrayList<>(currentVisited);
                combined.addAll(rollingEval.getEvaluatedLocations());
                rollingDay.setLocations(combined);
                finalSchedules.add(rollingDay);
            } else {
                // Push NICE_TO_HAVE to U, prioritize MUST_GO in today
                List<ReplanLocationItem> todayMustGo = new ArrayList<>();
                for (ReplanLocationItem item : currentRemaining) {
                    if (item.getPriority() == LocationPriority.MUST_GO) {
                        todayMustGo.add(item);
                    } else {
                        unvisitedU.add(item);
                    }
                }

                rollingDay.setLocations(todayMustGo);
                ScheduleEvaluationResult mustGoEval = scheduleTimeEvaluator.evaluate(rollingDay, distanceCalculator);
                if (!mustGoEval.isFeasible()) {
                    // Even MUST_GO doesn't fit, push remaining must go to U as well
                    unvisitedU.addAll(todayMustGo);
                    rollingDay.setLocations(currentVisited);
                } else {
                    List<ReplanLocationItem> combined = new ArrayList<>(currentVisited);
                    combined.addAll(mustGoEval.getEvaluatedLocations());
                    rollingDay.setLocations(combined);
                }
                finalSchedules.add(rollingDay);
            }
        } else {
            // End of day: separate visited vs unvisited
            List<ReplanLocationItem> visitedToday = new ArrayList<>();
            for (ReplanLocationItem item : currentDayItems) {
                if (item.getStatus() == TripLocationStatus.VISITED || item.getStatus() == TripLocationStatus.SKIPPED) {
                    visitedToday.add(item);
                } else {
                    // Unvisited (PLANNED / POSTPONED) goes to U
                    unvisitedU.add(item);
                }
            }
            DaySchedule daySchedule = DaySchedule.builder()
                    .day(currentDay)
                    .date(startDate.plusDays(currentDay - 1))
                    .locations(visitedToday)
                    .build();
            ScheduleEvaluationResult eval = scheduleTimeEvaluator.evaluate(daySchedule, distanceCalculator);
            if (eval.getEvaluatedLocations() != null) {
                daySchedule.setLocations(eval.getEvaluatedLocations());
            }
            finalSchedules.add(daySchedule);
        }

        // Future Days (> currentDay)
        for (int d = currentDay + 1; d <= maxDay; d++) {
            List<ReplanLocationItem> futureItems = itemsByDay.getOrDefault(d, new ArrayList<>());
            List<ReplanLocationItem> futurePlanned = new ArrayList<>();
            for (ReplanLocationItem item : futureItems) {
                if (item.getStatus() == TripLocationStatus.POSTPONED) {
                    unvisitedU.add(item);
                } else {
                    futurePlanned.add(item);
                }
            }
            DaySchedule futureDay = DaySchedule.builder()
                    .day(d)
                    .date(startDate.plusDays(d - 1))
                    .locations(futurePlanned)
                    .build();
            targetFutureDays.add(futureDay);
        }

        // 3. Execute InsertionPlanner
        InsertionPlanResult planResult = insertionPlanner.planInsertions(targetFutureDays, unvisitedU, distanceCalculator);
        finalSchedules.addAll(planResult.getSchedules());

        // 4. Calculate new metrics & diff
        double newTotalDistance = 0.0;
        int newTotalTravelTime = 0;
        List<DayScheduleDto> newScheduleDtos = new ArrayList<>();
        Map<Long, ReplanLocationItem> newPositionMap = new HashMap<>();

        for (DaySchedule ds : finalSchedules) {
            ScheduleEvaluationResult eval = scheduleTimeEvaluator.evaluate(ds, distanceCalculator);
            newTotalDistance += eval.getTotalDistanceKm();
            newTotalTravelTime += eval.getTotalTravelMinutes();

            List<ReplanItemDto> itemDtos = new ArrayList<>();
            if (eval.getEvaluatedLocations() != null) {
                for (ReplanLocationItem item : eval.getEvaluatedLocations()) {
                    itemDtos.add(toReplanItemDto(item));
                    if (item.getTripLocationId() != null) {
                        newPositionMap.put(item.getTripLocationId(), item);
                    }
                }
            }

            newScheduleDtos.add(DayScheduleDto.builder()
                    .day(ds.getDay())
                    .date(ds.getDate())
                    .startTime(ds.getStartTime() != null ? ds.getStartTime() : ScheduleTimeEvaluator.DEFAULT_START_TIME)
                    .endTime(eval.getFinalDepartureTime())
                    .totalDistanceKm(eval.getTotalDistanceKm())
                    .totalTravelMinutes(eval.getTotalTravelMinutes())
                    .totalStayMinutes(eval.getTotalStayMinutes())
                    .locations(itemDtos)
                    .build());
        }

        // Build moved locations list
        List<MovedLocationDto> movedLocations = new ArrayList<>();
        for (TripLocation orig : allDbLocations) {
            ReplanLocationItem newItem = newPositionMap.get(orig.getTripLocationId());
            if (newItem != null) {
                boolean dayChanged = !Objects.equals(orig.getPlannedDay(), newItem.getPlannedDay());
                boolean orderChanged = !Objects.equals(orig.getVisitOrder(), newItem.getVisitOrder());
                if (dayChanged || orderChanged) {
                    movedLocations.add(MovedLocationDto.builder()
                            .tripLocationId(orig.getTripLocationId())
                            .osmId(orig.getOsmId())
                            .placeName(orig.getPlaceName())
                            .fromDay(orig.getPlannedDay())
                            .toDay(newItem.getPlannedDay())
                            .fromOrder(orig.getVisitOrder())
                            .toOrder(newItem.getVisitOrder())
                            .reason(dayChanged ? "MOVED_TO_DAY_" + newItem.getPlannedDay() : "REORDERED_IN_DAY")
                            .build());
                }
            }
        }

        List<UnplaceableLocationDto> unplaceableDtos = planResult.getUnplaceableLocations().stream()
                .map(u -> UnplaceableLocationDto.builder()
                        .tripLocationId(u.getTripLocationId())
                        .osmId(u.getOsmId())
                        .placeName(u.getPlaceName())
                        .priority(u.getPriority())
                        .originalDay(u.getOriginalDay())
                        .reason(u.getReason())
                        .severity(u.getSeverity())
                        .suggestedActions(u.getSuggestedActions())
                        .build())
                .collect(Collectors.toList());

        MetricsDiffDto metricsDiff = MetricsDiffDto.builder()
                .oldTotalDistanceKm(round(oldTotalDistance))
                .newTotalDistanceKm(round(newTotalDistance))
                .distanceDeltaKm(round(newTotalDistance - oldTotalDistance))
                .oldTotalTravelMinutes(oldTotalTravelTime)
                .newTotalTravelMinutes(newTotalTravelTime)
                .travelMinutesDelta(newTotalTravelTime - oldTotalTravelTime)
                .build();

        String proposalId = UUID.randomUUID().toString();
        ReplanProposal proposal = ReplanProposal.builder()
                .proposalId(proposalId)
                .tripId(trip.getTripId())
                .tripVersion(trip.getVersion())
                .currentDay(currentDay)
                .createdAt(LocalDateTime.now())
                .newSchedules(finalSchedules)
                .unplaceableLocations(planResult.getUnplaceableLocations())
                .movedLocations(movedLocations)
                .metricsDiff(metricsDiff)
                .build();

        proposalStore.save(proposal);

        return ReplanProposalResponse.builder()
                .proposalId(proposalId)
                .tripId(trip.getTripId())
                .tripVersion(trip.getVersion())
                .currentDay(currentDay)
                .severity(planResult.getSeverity())
                .movedLocations(movedLocations)
                .unplaceableLocations(unplaceableDtos)
                .newSchedule(newScheduleDtos)
                .metricsDiff(metricsDiff)
                .build();
    }

    @Override
    @Transactional
    public TripResponse confirmReplan(Long tripId, ReplanConfirmRequest request) {
        Trip trip = getActiveTrip(tripId);

        ReplanProposal proposal = proposalStore.findById(request.getProposalId())
                .orElseThrow(() -> new AppException(ErrorCode.REPLAN_PROPOSAL_EXPIRED));

        if (!proposal.getTripId().equals(trip.getTripId())) {
            throw new AppException(ErrorCode.REPLAN_PROPOSAL_INVALID);
        }

        // Optimistic locking check: Trip version must match proposal tripVersion
        if (trip.getVersion() != null && proposal.getTripVersion() != null && !trip.getVersion().equals(proposal.getTripVersion())) {
            throw new AppException(ErrorCode.TRIP_CONFLICT);
        }

        // Re-validate constraints before committing
        for (DaySchedule daySchedule : proposal.getNewSchedules()) {
            ScheduleEvaluationResult eval = scheduleTimeEvaluator.evaluate(daySchedule, distanceCalculator);
            if (!eval.isFeasible()) {
                throw new AppException(ErrorCode.REPLAN_PROPOSAL_INVALID);
            }
        }

        // Apply new schedules to TripLocation entities
        int maxDayAssigned = 1;
        for (DaySchedule ds : proposal.getNewSchedules()) {
            if (ds.getDay() != null && ds.getDay() > maxDayAssigned) {
                maxDayAssigned = ds.getDay();
            }
            if (ds.getLocations() != null) {
                for (ReplanLocationItem item : ds.getLocations()) {
                    if (item.getTripLocationId() != null) {
                        TripLocation loc = tripLocationRepository.findById(item.getTripLocationId())
                                .orElse(null);
                        if (loc != null && loc.getTrip().getTripId().equals(trip.getTripId())) {
                            loc.setPlannedDay(ds.getDay());
                            loc.setVisitOrder(item.getVisitOrder());
                            loc.setEstimatedArrivalTime(item.getEstimatedArrivalTime());
                            loc.setStatus(item.getStatus() != null ? item.getStatus() : TripLocationStatus.PLANNED);
                            tripLocationRepository.save(loc);
                        }
                    }
                }
            }
        }

        // Handle unplaceable locations
        if (proposal.getUnplaceableLocations() != null) {
            Map<Long, ReplanAction> decisions = request.getUnplaceableDecisions() != null ? request.getUnplaceableDecisions() : Collections.emptyMap();
            for (UnplaceableLocationItem unplaceable : proposal.getUnplaceableLocations()) {
                if (unplaceable.getTripLocationId() != null) {
                    TripLocation loc = tripLocationRepository.findById(unplaceable.getTripLocationId()).orElse(null);
                    if (loc != null) {
                        ReplanAction action = decisions.getOrDefault(unplaceable.getTripLocationId(), ReplanAction.SKIP);
                        if (action == ReplanAction.ADD_EXTRA_DAY) {
                            maxDayAssigned++;
                            loc.setPlannedDay(maxDayAssigned);
                            loc.setVisitOrder(1);
                            loc.setStatus(TripLocationStatus.PLANNED);
                            if (trip.getEndDate() != null) {
                                trip.setEndDate(trip.getEndDate().plusDays(1));
                            }
                        } else {
                            loc.setStatus(TripLocationStatus.SKIPPED);
                        }
                        tripLocationRepository.save(loc);
                    }
                }
            }
        }

        trip.setItineraryArranged(true);
        Trip savedTrip = tripRepository.save(trip);

        // Evict proposal from cache
        proposalStore.evict(request.getProposalId());

        return toTripResponse(savedTrip);
    }

    @Override
    @Transactional
    public TripLocationResponse updateLocationStatus(Long tripId, Long locationId, LocationStatusUpdateRequest request) {
        Trip trip = getActiveTrip(tripId);
        TripLocation location = tripLocationRepository.findByTripLocationIdAndTrip(locationId, trip)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_LOCATION_NOT_FOUND));

        if (request.getCurrentLatitude() != null && request.getCurrentLongitude() != null) {
            double distKm = distanceCalculator.distanceKm(
                    request.getCurrentLatitude(), request.getCurrentLongitude(),
                    location.getLatitude() != null ? location.getLatitude() : 0.0,
                    location.getLongitude() != null ? location.getLongitude() : 0.0
            );
            if (distKm <= 0.100) { // Within 100 meters
                location.setStatus(TripLocationStatus.VISITED);
                location.setVisitedAt(request.getVisitedAt() != null ? request.getVisitedAt() : LocalDateTime.now());
            }
        }

        if (request.getStatus() != null) {
            location.setStatus(request.getStatus());
            if (request.getStatus() == TripLocationStatus.VISITED && location.getVisitedAt() == null) {
                location.setVisitedAt(request.getVisitedAt() != null ? request.getVisitedAt() : LocalDateTime.now());
            }
        }

        TripLocation saved = tripLocationRepository.save(location);
        return toTripLocationResponse(saved);
    }

    private ReplanLocationItem toReplanLocationItem(TripLocation loc, LocalDate startDate, int day) {
        LocalTime open = loc.getOpenTime();
        LocalTime close = loc.getCloseTime();

        if (open == null && close == null && loc.getOsmId() != null) {
            LocalDate dayDate = startDate.plusDays(day - 1);
            DayOfWeek dow = dayDate.getDayOfWeek();
            Optional<LocationOpeningHour> oh = openingHourRepository.findByOsmIdAndDayOfWeek(loc.getOsmId(), dow);
            if (oh.isPresent() && !Boolean.TRUE.equals(oh.get().getClosed())) {
                open = oh.get().getOpenTime();
                close = oh.get().getCloseTime();
            }
        }

        return ReplanLocationItem.builder()
                .tripLocationId(loc.getTripLocationId())
                .osmId(loc.getOsmId())
                .placeName(loc.getPlaceName())
                .latitude(loc.getLatitude())
                .longitude(loc.getLongitude())
                .visitOrder(loc.getVisitOrder())
                .plannedDay(day)
                .status(loc.getStatus() != null ? loc.getStatus() : TripLocationStatus.PLANNED)
                .visitedAt(loc.getVisitedAt())
                .priority(loc.getPriority() != null ? loc.getPriority() : LocationPriority.NICE_TO_HAVE)
                .fixedDay(loc.getFixedDay())
                .stayDurationMinutes(loc.getStayDurationMinutes() != null ? loc.getStayDurationMinutes() : 60)
                .openTime(open)
                .closeTime(close)
                .estimatedArrivalTime(loc.getEstimatedArrivalTime())
                .notes(loc.getNotes())
                .build();
    }

    private ReplanItemDto toReplanItemDto(ReplanLocationItem item) {
        return ReplanItemDto.builder()
                .tripLocationId(item.getTripLocationId())
                .osmId(item.getOsmId())
                .placeName(item.getPlaceName())
                .latitude(item.getLatitude())
                .longitude(item.getLongitude())
                .visitOrder(item.getVisitOrder())
                .plannedDay(item.getPlannedDay())
                .status(item.getStatus())
                .visitedAt(item.getVisitedAt())
                .priority(item.getPriority())
                .fixedDay(item.getFixedDay())
                .stayDurationMinutes(item.getStayDurationMinutes())
                .openTime(item.getOpenTime())
                .closeTime(item.getCloseTime())
                .estimatedArrivalTime(item.getEstimatedArrivalTime())
                .notes(item.getNotes())
                .build();
    }

    private TripLocationResponse toTripLocationResponse(TripLocation loc) {
        return TripLocationResponse.builder()
                .tripLocationId(loc.getTripLocationId())
                .tripId(loc.getTrip() != null ? loc.getTrip().getTripId() : null)
                .osmId(loc.getOsmId())
                .placeName(loc.getPlaceName())
                .latitude(loc.getLatitude())
                .longitude(loc.getLongitude())
                .visitOrder(loc.getVisitOrder())
                .plannedDay(loc.getPlannedDay())
                .status(loc.getStatus())
                .visitedAt(loc.getVisitedAt())
                .priority(loc.getPriority())
                .fixedDay(loc.getFixedDay())
                .stayDurationMinutes(loc.getStayDurationMinutes())
                .openTime(loc.getOpenTime())
                .closeTime(loc.getCloseTime())
                .estimatedArrivalTime(loc.getEstimatedArrivalTime())
                .notes(loc.getNotes())
                .createdAt(loc.getCreatedAt())
                .build();
    }

    private TripResponse toTripResponse(Trip trip) {
        return TripResponse.builder()
                .tripId(trip.getTripId())
                .tripName(trip.getTripName())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .budget(trip.getBudget())
                .status(trip.getStatus())
                .itineraryArranged(trip.getItineraryArranged())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }

    private User getCurrentUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private Trip getActiveTrip(Long tripId) {
        return tripRepository.findByTripIdAndUserAndDeletedFalse(tripId, getCurrentUser())
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
