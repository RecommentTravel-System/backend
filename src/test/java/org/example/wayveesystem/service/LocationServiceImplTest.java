package org.example.wayveesystem.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.example.wayveesystem.common.client.OverpassApiClient;
import org.example.wayveesystem.common.enums.LocationSource;
import org.example.wayveesystem.common.enums.PoiStatus;
import org.example.wayveesystem.dto.GridCacheKey;
import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.dto.request.LocationFilterRequest;
import org.example.wayveesystem.dto.response.LocationPageResponse;
import org.example.wayveesystem.dto.response.LocationResponse;
import org.example.wayveesystem.dto.response.OverpassElementResponse;
import org.example.wayveesystem.dto.response.OverpassResponse;
import org.example.wayveesystem.mapper.OsmPlaceMapper;
import org.example.wayveesystem.mapper.PlaceResponseMapper;
import org.example.wayveesystem.model.Category;
import org.example.wayveesystem.model.PoiCatalog;
import org.example.wayveesystem.repository.CategoryRepository;
import org.example.wayveesystem.repository.PoiCatalogRepository;
import org.example.wayveesystem.repository.SavedPlaceRepository;
import org.example.wayveesystem.service.impl.LocationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    @Mock
    private OverpassApiClient overpassApiClient;

    @Mock
    private RestClient nominatimRestClient;

    @Mock
    private SavedPlaceRepository savedPlaceRepository;

    @Mock
    private PoiCatalogRepository poiCatalogRepository;

    @Mock
    private CategoryRepository categoryRepository;



    @Mock
    private AsyncLoadingCache<GridCacheKey, List<OsmPlace>> poiAsyncLoadingCache;

    private PlaceResponseMapper placeResponseMapper;
    private OsmPlaceMapper osmPlaceMapper;
    private LocationServiceImpl locationService;

    @BeforeEach
    void setUp() {
        placeResponseMapper = new PlaceResponseMapper();
        osmPlaceMapper = new OsmPlaceMapper();

        locationService = new LocationServiceImpl(
                overpassApiClient,
                nominatimRestClient,
                osmPlaceMapper,
                placeResponseMapper,
                savedPlaceRepository,
                poiCatalogRepository,
                categoryRepository,
                poiAsyncLoadingCache
        );

        ReflectionTestUtils.setField(locationService, "gridPrecision", 0.02);

        Category cafeCat = Category.builder().categoryId(1L).code("CAFE").name("Quán Cà Phê").build();
        Category restCat = Category.builder().categoryId(2L).code("RESTAURANT").name("Nhà Hàng").build();
        lenient().when(categoryRepository.findAll()).thenReturn(List.of(cafeCat, restCat));
        lenient().when(savedPlaceRepository.findByOsmId(anyLong())).thenReturn(Optional.empty());

    }

    @Test
    @DisplayName("Should return POIs from database when available without calling Overpass")
    void testSearchNearby_FoundInDatabase() {
        double lat = 10.7769;
        double lng = 106.7009;

        PoiCatalog poi = PoiCatalog.builder()
                .id(100L)
                .source(LocationSource.OSM)
                .sourceId("99901")
                .name("Cà Phê Sài Gòn")
                .categoryId(1L)
                .address("123 Lê Lợi, Q1, TP.HCM")
                .lat((float) lat)
                .lon((float) lng)
                .status(PoiStatus.ACTIVE)
                .importedAt(LocalDateTime.now())
                .build();

        when(poiCatalogRepository.findByLatBetweenAndLonBetweenAndStatus(
                anyFloat(), anyFloat(), anyFloat(), anyFloat(), eq(PoiStatus.ACTIVE)
        )).thenReturn(List.of(poi));

        LocationFilterRequest request = new LocationFilterRequest(
                lat, lng, 1000, null, null, null, null, 0, 10
        );

        LocationPageResponse response = locationService.searchNearbyLocations(request);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().name()).isEqualTo("Cà Phê Sài Gòn");
        assertThat(response.content().getFirst().categoryCode()).isEqualTo("CAFE");
        assertThat(response.totalElements()).isEqualTo(1);

        // Verify Overpass cache was NOT touched because DB had results
        verifyNoInteractions(poiAsyncLoadingCache);
        verifyNoInteractions(overpassApiClient);
    }

    @Test
    @DisplayName("Should fallback to Overpass cache when database has no matching POIs")
    void testSearchNearby_FallbackToOverpass() {
        double lat = 16.4637;
        double lng = 107.5909;

        // DB returns empty
        when(poiCatalogRepository.findByLatBetweenAndLonBetweenAndStatus(
                anyFloat(), anyFloat(), anyFloat(), anyFloat(), eq(PoiStatus.ACTIVE)
        )).thenReturn(Collections.emptyList());

        OsmPlace overpassPlace = new OsmPlace(
                88801L,
                "Quán Ăn Huế",
                "RESTAURANT",
                "Huế",
                lat,
                lng,
                null,
                null,
                null,
                null,
                null,
                Map.of()
        );

        CompletableFuture<List<OsmPlace>> future = CompletableFuture.completedFuture(List.of(overpassPlace));
        when(poiAsyncLoadingCache.get(any(GridCacheKey.class))).thenReturn(future);

        LocationFilterRequest request = new LocationFilterRequest(
                lat, lng, 1000, null, null, null, null, 0, 10
        );

        LocationPageResponse response = locationService.searchNearbyLocations(request);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().name()).isEqualTo("Quán Ăn Huế");
        assertThat(response.totalElements()).isEqualTo(1);

        // Verify fallback to cache occurred
        verify(poiAsyncLoadingCache, times(1)).get(any(GridCacheKey.class));
    }

    @Test
    @DisplayName("Should find place by OSM ID in database first")
    void testGetPlaceByOsmId_FoundInDatabase() {
        Long osmId = 12345L;

        PoiCatalog poi = PoiCatalog.builder()
                .id(50L)
                .source(LocationSource.OSM)
                .sourceId(String.valueOf(osmId))
                .name("Nhà Hàng Ngon")
                .categoryId(2L)
                .address("78 Pasteur, Q1")
                .lat(10.777f)
                .lon(106.701f)
                .status(PoiStatus.ACTIVE)
                .importedAt(LocalDateTime.now())
                .build();

        when(poiCatalogRepository.findBySourceAndSourceId(LocationSource.OSM, String.valueOf(osmId)))
                .thenReturn(Optional.of(poi));

        LocationResponse response = locationService.getPlaceByOsmId(osmId);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Nhà Hàng Ngon");
        assertThat(response.categoryCode()).isEqualTo("RESTAURANT");

        // Verify Overpass API client was not called
        verifyNoInteractions(overpassApiClient);
    }

    @Test
    @DisplayName("Should fallback to Overpass client when OSM ID is not found in database")
    void testGetPlaceByOsmId_FallbackToOverpass() {
        Long osmId = 99999L;

        when(poiCatalogRepository.findBySourceAndSourceId(LocationSource.OSM, String.valueOf(osmId)))
                .thenReturn(Optional.empty());
        when(poiCatalogRepository.findById(osmId)).thenReturn(Optional.empty());

        OverpassElementResponse element = new OverpassElementResponse(
                osmId,
                "node",
                10.777,
                106.701,
                null,
                Map.of("name", "Overpass Cafe", "amenity", "cafe")
        );
        OverpassResponse overpassResponse = new OverpassResponse(List.of(element));
        when(overpassApiClient.fetchSingleElement(anyString())).thenReturn(overpassResponse);

        LocationResponse response = locationService.getPlaceByOsmId(osmId);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Overpass Cafe");
        assertThat(response.categoryCode()).isEqualTo("CAFE");

        verify(overpassApiClient, times(1)).fetchSingleElement(anyString());
    }
}
