package org.example.wayveesystem.service.replanning;

import org.example.wayveesystem.common.enums.LocationPriority;
import org.example.wayveesystem.common.enums.ReplanAction;
import org.example.wayveesystem.common.enums.ReplanSeverity;
import org.example.wayveesystem.common.enums.TripLocationStatus;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.dto.request.LocationStatusUpdateRequest;
import org.example.wayveesystem.dto.request.ReplanConfirmRequest;
import org.example.wayveesystem.dto.request.ReplanPreviewRequest;
import org.example.wayveesystem.dto.response.ReplanProposalResponse;
import org.example.wayveesystem.dto.response.TripLocationResponse;
import org.example.wayveesystem.dto.response.TripResponse;
import org.example.wayveesystem.model.Trip;
import org.example.wayveesystem.model.TripLocation;
import org.example.wayveesystem.model.User;
import org.example.wayveesystem.repository.LocationOpeningHourRepository;
import org.example.wayveesystem.repository.TripLocationRepository;
import org.example.wayveesystem.repository.TripRepository;
import org.example.wayveesystem.repository.UserRepository;
import org.example.wayveesystem.service.impl.TripReplanningServiceImpl;
import org.example.wayveesystem.service.replanning.constraint.*;
import org.example.wayveesystem.service.replanning.distance.HaversineDistanceCalculator;
import org.example.wayveesystem.service.replanning.planner.InsertionPlanner;
import org.example.wayveesystem.service.replanning.planner.LocalSearchOptimizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripReplanningServiceTest {

    @Mock
    TripRepository tripRepository;

    @Mock
    TripLocationRepository tripLocationRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    LocationOpeningHourRepository openingHourRepository;

    @Mock
    SecurityContext securityContext;

    @Mock
    Authentication authentication;

    HaversineDistanceCalculator distanceCalculator;
    ScheduleTimeEvaluator scheduleTimeEvaluator;
    FixedDayChecker fixedDayChecker;
    OpeningHoursChecker openingHoursChecker;
    DayTimeBudgetChecker dayTimeBudgetChecker;
    LocalSearchOptimizer localSearchOptimizer;
    InsertionPlanner insertionPlanner;
    ReplanProposalStore proposalStore;
    TripReplanningServiceImpl tripReplanningService;

    User testUser;
    Trip testTrip;

    @BeforeEach
    void setUp() {
        distanceCalculator = new HaversineDistanceCalculator(30.0);
        scheduleTimeEvaluator = new ScheduleTimeEvaluator();
        fixedDayChecker = new FixedDayChecker();
        openingHoursChecker = new OpeningHoursChecker(scheduleTimeEvaluator);
        dayTimeBudgetChecker = new DayTimeBudgetChecker(scheduleTimeEvaluator);

        List<ReplanConstraintChecker> checkers = List.of(fixedDayChecker, openingHoursChecker, dayTimeBudgetChecker);
        localSearchOptimizer = new LocalSearchOptimizer(scheduleTimeEvaluator);
        insertionPlanner = new InsertionPlanner(checkers, scheduleTimeEvaluator, localSearchOptimizer);
        proposalStore = new ReplanProposalStore();

        tripReplanningService = new TripReplanningServiceImpl(
                tripRepository,
                tripLocationRepository,
                userRepository,
                openingHourRepository,
                distanceCalculator,
                insertionPlanner,
                scheduleTimeEvaluator,
                proposalStore
        );

        testUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .build();

        testTrip = Trip.builder()
                .tripId(100L)
                .user(testUser)
                .tripName("Hanoi - Saigon Tour")
                .startDate(LocalDate.of(2026, 10, 10))
                .endDate(LocalDate.of(2026, 10, 12))
                .version(1L)
                .deleted(false)
                .build();

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("1");
        SecurityContextHolder.setContext(securityContext);
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        lenient().when(tripRepository.findByTripIdAndUserAndDeletedFalse(100L, testUser)).thenReturn(Optional.of(testTrip));
    }

    @Test
    @DisplayName("1. Replan with available slots: Unvisited point from Day 1 is successfully inserted into Day 2")
    void testReplan_WithAvailableSlots_SuccessfullyInserted() {
        // Day 1: Loc1 (VISITED), Loc2 (PLANNED - missed)
        // Day 2: Loc3 (PLANNED)
        TripLocation loc1 = TripLocation.builder()
                .tripLocationId(1L)
                .trip(testTrip)
                .osmId(101L)
                .placeName("Ben Thanh Market")
                .latitude(10.772)
                .longitude(106.698)
                .plannedDay(1)
                .visitOrder(1)
                .status(TripLocationStatus.VISITED)
                .stayDurationMinutes(60)
                .build();

        TripLocation loc2 = TripLocation.builder()
                .tripLocationId(2L)
                .trip(testTrip)
                .osmId(102L)
                .placeName("War Remnants Museum")
                .latitude(10.779)
                .longitude(106.692)
                .plannedDay(1)
                .visitOrder(2)
                .status(TripLocationStatus.PLANNED)
                .stayDurationMinutes(60)
                .build();

        TripLocation loc3 = TripLocation.builder()
                .tripLocationId(3L)
                .trip(testTrip)
                .osmId(103L)
                .placeName("Independence Palace")
                .latitude(10.777)
                .longitude(106.695)
                .plannedDay(2)
                .visitOrder(1)
                .status(TripLocationStatus.PLANNED)
                .stayDurationMinutes(60)
                .build();

        when(tripLocationRepository.findByTripOrderByPlannedDayAscVisitOrderAsc(testTrip))
                .thenReturn(List.of(loc1, loc2, loc3));

        ReplanPreviewRequest request = ReplanPreviewRequest.builder()
                .currentDay(1)
                .build();

        ReplanProposalResponse response = tripReplanningService.previewReplan(100L, request);

        assertNotNull(response);
        assertNotNull(response.getProposalId());
        assertTrue(response.getUnplaceableLocations().isEmpty(), "All locations should be placed");
        assertTrue(response.getMovedLocations().stream().anyMatch(m -> m.getTripLocationId().equals(2L) && m.getToDay() == 2),
                "Loc2 should be moved to Day 2");
    }

    @Test
    @DisplayName("2. Replan when all days are full: Returns unplaceable list with suggested actions")
    void testReplan_AllDaysFull_ReturnsUnplaceableList() {
        // Loc1 missed on Day 1 (takes 60 mins)
        // Day 2 is already packed to maximum end time 21:00 (e.g. starting at 08:00 and 13 hours total duration)
        TripLocation loc1 = TripLocation.builder()
                .tripLocationId(1L)
                .trip(testTrip)
                .osmId(101L)
                .placeName("Missed Attraction")
                .latitude(10.772)
                .longitude(106.698)
                .plannedDay(1)
                .visitOrder(1)
                .status(TripLocationStatus.PLANNED)
                .stayDurationMinutes(120)
                .build();

        TripLocation packedDay2Loc = TripLocation.builder()
                .tripLocationId(2L)
                .trip(testTrip)
                .osmId(102L)
                .placeName("Super Long Tour")
                .latitude(10.779)
                .longitude(106.692)
                .plannedDay(2)
                .visitOrder(1)
                .status(TripLocationStatus.PLANNED)
                .stayDurationMinutes(760) // 12 hours 40 mins -> finish at 20:40+
                .build();

        when(tripLocationRepository.findByTripOrderByPlannedDayAscVisitOrderAsc(testTrip))
                .thenReturn(List.of(loc1, packedDay2Loc));

        ReplanPreviewRequest request = ReplanPreviewRequest.builder()
                .currentDay(1)
                .build();

        ReplanProposalResponse response = tripReplanningService.previewReplan(100L, request);

        assertNotNull(response);
        assertEquals(1, response.getUnplaceableLocations().size());
        assertEquals(1L, response.getUnplaceableLocations().get(0).getTripLocationId());
        assertTrue(response.getUnplaceableLocations().get(0).getSuggestedActions().contains(ReplanAction.SKIP));
        assertTrue(response.getUnplaceableLocations().get(0).getSuggestedActions().contains(ReplanAction.ADD_EXTRA_DAY));
    }

    @Test
    @DisplayName("3. Replan respects fixed_day constraint: Fixed day location cannot be moved to another day")
    void testReplan_RespectsFixedDayConstraint() {
        // Loc1 missed on Day 1, but has fixedDay = 1
        // Day 2 has free slots
        TripLocation loc1 = TripLocation.builder()
                .tripLocationId(1L)
                .trip(testTrip)
                .osmId(101L)
                .placeName("Fixed Concert")
                .latitude(10.772)
                .longitude(106.698)
                .plannedDay(1)
                .visitOrder(1)
                .status(TripLocationStatus.PLANNED)
                .fixedDay(1) // CANNOT MOVE TO DAY 2
                .stayDurationMinutes(60)
                .build();

        TripLocation loc2 = TripLocation.builder()
                .tripLocationId(2L)
                .trip(testTrip)
                .osmId(102L)
                .placeName("Park")
                .latitude(10.779)
                .longitude(106.692)
                .plannedDay(2)
                .visitOrder(1)
                .status(TripLocationStatus.PLANNED)
                .stayDurationMinutes(60)
                .build();

        when(tripLocationRepository.findByTripOrderByPlannedDayAscVisitOrderAsc(testTrip))
                .thenReturn(List.of(loc1, loc2));

        ReplanPreviewRequest request = ReplanPreviewRequest.builder()
                .currentDay(1)
                .build();

        ReplanProposalResponse response = tripReplanningService.previewReplan(100L, request);

        assertNotNull(response);
        assertEquals(1, response.getUnplaceableLocations().size(), "Fixed day location cannot be placed on day 2");
        assertEquals(1L, response.getUnplaceableLocations().get(0).getTripLocationId());
    }

    @Test
    @DisplayName("4. Replan respects opening hours: Insertion respects opening window and waiting time")
    void testReplan_RespectsOpeningHours() {
        // Loc1 on Day 1 unvisited. Opens 10:00, Closes 12:00.
        // Day 2 has Loc2 (Opens 08:00, stay 60 mins -> departs 09:00).
        // Loc1 should arrive ~09:05, wait until 10:00, stay 60 mins -> departs 11:00 (before 12:00 close).
        TripLocation loc1 = TripLocation.builder()
                .tripLocationId(1L)
                .trip(testTrip)
                .osmId(101L)
                .placeName("Morning Art Gallery")
                .latitude(10.772)
                .longitude(106.698)
                .plannedDay(1)
                .visitOrder(1)
                .status(TripLocationStatus.PLANNED)
                .openTime(LocalTime.of(10, 0))
                .closeTime(LocalTime.of(12, 0))
                .stayDurationMinutes(60)
                .build();

        TripLocation loc2 = TripLocation.builder()
                .tripLocationId(2L)
                .trip(testTrip)
                .osmId(102L)
                .placeName("Cafe")
                .latitude(10.773)
                .longitude(106.699)
                .plannedDay(2)
                .visitOrder(1)
                .status(TripLocationStatus.PLANNED)
                .stayDurationMinutes(60)
                .build();

        when(tripLocationRepository.findByTripOrderByPlannedDayAscVisitOrderAsc(testTrip))
                .thenReturn(List.of(loc1, loc2));

        ReplanPreviewRequest request = ReplanPreviewRequest.builder()
                .currentDay(1)
                .build();

        ReplanProposalResponse response = tripReplanningService.previewReplan(100L, request);

        assertNotNull(response);
        assertTrue(response.getUnplaceableLocations().isEmpty(), "Gallery should fit within its 10:00-12:00 window");
        assertTrue(response.getMovedLocations().stream().anyMatch(m -> m.getTripLocationId().equals(1L) && m.getToDay() == 2),
                "Loc1 should be moved to Day 2");
    }

    @Test
    @DisplayName("5. Insertion causes subsequent point to miss closing time: Rejected by forward slack check")
    void testReplan_InsertionCausesSubsequentPointToMissClosingTime() {
        // Loc1 missed on Day 1 (takes 3 hours)
        // Day 2 has Loc2: closes strictly at 09:30. Day 2 starts at 08:00.
        // If Loc1 is inserted before Loc2, Loc2 arrives at 11:00+ > 09:30 closing time.
        // If Loc1 is inserted after Loc2, check if Loc1 itself fits.
        TripLocation loc1 = TripLocation.builder()
                .tripLocationId(1L)
                .trip(testTrip)
                .osmId(101L)
                .placeName("Long Workshop")
                .latitude(10.772)
                .longitude(106.698)
                .plannedDay(1)
                .visitOrder(1)
                .status(TripLocationStatus.PLANNED)
                .stayDurationMinutes(180)
                .build();

        TripLocation loc2 = TripLocation.builder()
                .tripLocationId(2L)
                .trip(testTrip)
                .osmId(102L)
                .placeName("Early Morning Bakery")
                .latitude(10.773)
                .longitude(106.699)
                .plannedDay(2)
                .visitOrder(1)
                .status(TripLocationStatus.PLANNED)
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(9, 30))
                .stayDurationMinutes(60)
                .build();

        when(tripLocationRepository.findByTripOrderByPlannedDayAscVisitOrderAsc(testTrip))
                .thenReturn(List.of(loc1, loc2));

        ReplanPreviewRequest request = ReplanPreviewRequest.builder()
                .currentDay(1)
                .build();

        ReplanProposalResponse response = tripReplanningService.previewReplan(100L, request);

        // Loc1 should be inserted AFTER Loc2 (order 2), so Loc2 is visited at 08:00-09:00 before 09:30 close
        assertNotNull(response);
        assertTrue(response.getUnplaceableLocations().isEmpty());
        assertEquals(2, response.getNewSchedule().get(1).getLocations().size());
        assertEquals("Early Morning Bakery", response.getNewSchedule().get(1).getLocations().get(0).getPlaceName());
        assertEquals("Long Workshop", response.getNewSchedule().get(1).getLocations().get(1).getPlaceName());
    }

    @Test
    @DisplayName("6. Replan with empty unvisited set: All visited, returns unchanged schedule")
    void testReplan_EmptyUnvisitedSet() {
        TripLocation loc1 = TripLocation.builder()
                .tripLocationId(1L)
                .trip(testTrip)
                .osmId(101L)
                .placeName("Point 1")
                .plannedDay(1)
                .visitOrder(1)
                .status(TripLocationStatus.VISITED)
                .stayDurationMinutes(60)
                .build();

        TripLocation loc2 = TripLocation.builder()
                .tripLocationId(2L)
                .trip(testTrip)
                .osmId(102L)
                .placeName("Point 2")
                .plannedDay(2)
                .visitOrder(1)
                .status(TripLocationStatus.PLANNED)
                .stayDurationMinutes(60)
                .build();

        when(tripLocationRepository.findByTripOrderByPlannedDayAscVisitOrderAsc(testTrip))
                .thenReturn(List.of(loc1, loc2));

        ReplanPreviewRequest request = ReplanPreviewRequest.builder()
                .currentDay(1)
                .build();

        ReplanProposalResponse response = tripReplanningService.previewReplan(100L, request);

        assertNotNull(response);
        assertTrue(response.getMovedLocations().isEmpty(), "No locations should be moved");
        assertTrue(response.getUnplaceableLocations().isEmpty());
    }

    @Test
    @DisplayName("7. Confirm replan with modified trip version throws TRIP_CONFLICT (409)")
    void testReplan_ConfirmWithModifiedTripVersion_ThrowsConflictException() {
        // Generate proposal with version 1
        TripLocation loc1 = TripLocation.builder()
                .tripLocationId(1L)
                .trip(testTrip)
                .osmId(101L)
                .placeName("Point 1")
                .plannedDay(1)
                .visitOrder(1)
                .status(TripLocationStatus.PLANNED)
                .stayDurationMinutes(60)
                .build();

        when(tripLocationRepository.findByTripOrderByPlannedDayAscVisitOrderAsc(testTrip))
                .thenReturn(List.of(loc1));

        ReplanPreviewRequest previewReq = ReplanPreviewRequest.builder().currentDay(1).build();
        ReplanProposalResponse proposal = tripReplanningService.previewReplan(100L, previewReq);

        // Simulate concurrent update on Trip (version incremented to 2)
        testTrip.setVersion(2L);

        ReplanConfirmRequest confirmReq = ReplanConfirmRequest.builder()
                .proposalId(proposal.getProposalId())
                .build();

        AppException ex = assertThrows(AppException.class, () -> tripReplanningService.confirmReplan(100L, confirmReq));
        assertEquals(ErrorCode.TRIP_CONFLICT, ex.getErrorCode());
    }

    @Test
    @DisplayName("8. Rolling replan when current time is past closing time: Point pushed to unplaceable/future")
    void testReplan_RollingReplanWhenCurrentTimePastClosingTime() {
        // Current time: 18:00
        // Day 1 has Loc1 (closeTime: 17:00).
        TripLocation loc1 = TripLocation.builder()
                .tripLocationId(1L)
                .trip(testTrip)
                .osmId(101L)
                .placeName("Evening Museum")
                .latitude(10.772)
                .longitude(106.698)
                .plannedDay(1)
                .visitOrder(1)
                .status(TripLocationStatus.PLANNED)
                .closeTime(LocalTime.of(17, 0))
                .stayDurationMinutes(60)
                .build();

        when(tripLocationRepository.findByTripOrderByPlannedDayAscVisitOrderAsc(testTrip))
                .thenReturn(List.of(loc1));

        ReplanPreviewRequest request = ReplanPreviewRequest.builder()
                .currentDay(1)
                .currentLatitude(10.770)
                .currentLongitude(106.690)
                .currentTime(LocalTime.of(18, 0)) // PAST 17:00
                .build();

        ReplanProposalResponse response = tripReplanningService.previewReplan(100L, request);

        assertNotNull(response);
        // Museum cannot be done today at 18:00, so it's pushed to unplaceable / future days
        assertEquals(1, response.getUnplaceableLocations().size());
        assertEquals("Evening Museum", response.getUnplaceableLocations().get(0).getPlaceName());
    }

    @Test
    @DisplayName("9. MUST_GO bumps NICE_TO_HAVE when target day is full")
    void testReplan_MustGoBumpsNiceToHave_WhenTargetDayIsFull() {
        // Loc1: MUST_GO from Day 1 (takes 120 mins)
        // Day 2: packed with NICE_TO_HAVE Loc2 (takes 720 mins)
        TripLocation loc1 = TripLocation.builder()
                .tripLocationId(1L)
                .trip(testTrip)
                .osmId(101L)
                .placeName("Crucial Conference")
                .latitude(10.772)
                .longitude(106.698)
                .plannedDay(1)
                .visitOrder(1)
                .priority(LocationPriority.MUST_GO)
                .status(TripLocationStatus.PLANNED)
                .stayDurationMinutes(120)
                .build();

        TripLocation loc2 = TripLocation.builder()
                .tripLocationId(2L)
                .trip(testTrip)
                .osmId(102L)
                .placeName("Optional Sightseeing")
                .latitude(10.773)
                .longitude(106.699)
                .plannedDay(2)
                .visitOrder(1)
                .priority(LocationPriority.NICE_TO_HAVE)
                .status(TripLocationStatus.PLANNED)
                .stayDurationMinutes(720)
                .build();

        when(tripLocationRepository.findByTripOrderByPlannedDayAscVisitOrderAsc(testTrip))
                .thenReturn(List.of(loc1, loc2));

        ReplanPreviewRequest request = ReplanPreviewRequest.builder()
                .currentDay(1)
                .build();

        ReplanProposalResponse response = tripReplanningService.previewReplan(100L, request);

        assertNotNull(response);
        assertEquals(ReplanSeverity.HIGH, response.getSeverity());
        assertEquals(1, response.getUnplaceableLocations().size());
        // Loc2 (NICE_TO_HAVE) got bumped!
        assertEquals("Optional Sightseeing", response.getUnplaceableLocations().get(0).getPlaceName());
        assertEquals(ReplanSeverity.HIGH, response.getUnplaceableLocations().get(0).getSeverity());
    }

    @Test
    @DisplayName("10. Update location status within 100 meters: Auto check-in")
    void testUpdateLocationStatus_Within100Meters_AutoCheckin() {
        TripLocation loc = TripLocation.builder()
                .tripLocationId(1L)
                .trip(testTrip)
                .osmId(101L)
                .placeName("Cafe Spot")
                .latitude(10.772000)
                .longitude(106.698000)
                .status(TripLocationStatus.PLANNED)
                .build();

        when(tripLocationRepository.findByTripLocationIdAndTrip(1L, testTrip))
                .thenReturn(Optional.of(loc));
        when(tripLocationRepository.save(any(TripLocation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // GPS point ~30 meters away
        LocationStatusUpdateRequest request = LocationStatusUpdateRequest.builder()
                .currentLatitude(10.772200)
                .currentLongitude(106.698100)
                .build();

        TripLocationResponse response = tripReplanningService.updateLocationStatus(100L, 1L, request);

        assertNotNull(response);
        assertEquals(TripLocationStatus.VISITED, response.getStatus());
        assertNotNull(response.getVisitedAt());
    }
}
