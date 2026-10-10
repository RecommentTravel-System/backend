package org.example.wayveesystem.service;

import org.example.wayveesystem.common.enums.LocationSource;
import org.example.wayveesystem.common.enums.PoiStatus;
import org.example.wayveesystem.configuration.PoiImportProperties;
import org.example.wayveesystem.dto.response.PoiImportResultResponse;
import org.example.wayveesystem.model.Category;
import org.example.wayveesystem.model.PoiCatalog;
import org.example.wayveesystem.repository.CategoryRepository;
import org.example.wayveesystem.repository.PoiCatalogBulkRepository;
import org.example.wayveesystem.service.impl.PoiImportServiceImpl;
import org.example.wayveesystem.service.poi.strategy.OsmPoiSourceStrategy;
import org.example.wayveesystem.service.poi.strategy.OverturePoiSourceStrategy;
import org.example.wayveesystem.service.poi.strategy.PoiSourceStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PoiImportServiceTest {

    @Mock
    private PoiCatalogBulkRepository bulkRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private PoiImportProperties properties;
    private PoiImportServiceImpl importService;

    @BeforeEach
    void setUp() {
        properties = new PoiImportProperties();
        properties.setBatchSize(2);
        properties.setMinConfidence(0.6f);
        properties.getCategoryMapping().put("cafe", "CAFE");
        properties.getCategoryMapping().put("coffee_shop", "CAFE");
        properties.getCategoryMapping().put("restaurant", "RESTAURANT");

        PoiSourceStrategyFactory strategyFactory = new PoiSourceStrategyFactory(List.of(
                new OsmPoiSourceStrategy(),
                new OverturePoiSourceStrategy()
        ));

        importService = new PoiImportServiceImpl(bulkRepository, categoryRepository, strategyFactory, properties);

        Category cafeCat = Category.builder().categoryId(1L).code("CAFE").name("Quán Cà Phê").build();
        Category restCat = Category.builder().categoryId(2L).code("RESTAURANT").name("Nhà Hàng").build();
        lenient().when(categoryRepository.findAll()).thenReturn(List.of(cafeCat, restCat));
    }

    @Test
    @DisplayName("Should parse and import valid OSM CSV records with batching")
    void testImportOsmCsvSuccess() {
        String csvContent = """
                source_id,name,category,address,lat,lon,confidence,operating_status
                osm_101,"Phở Thìn Lò Đúc",restaurant,"13 Lò Đúc, Hai Bà Trưng, Hà Nội",21.0185,105.8562,,active
                osm_102," Highlands Coffee ",cafe,"Đang cập nhật",21.0285,105.8542,,
                osm_103,"Quán Đã Đóng Cửa",cafe,"Hà Nội",21.0200,105.8500,,closed
                osm_104,"Outside VN Place",cafe,"Tokyo",35.6762,139.6503,,active
                """;

        when(bulkRepository.batchUpsert(anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());

        MockMultipartFile file = new MockMultipartFile(
                "file", "osm_test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        PoiImportResultResponse result = importService.importFromMultipart(LocationSource.OSM, file);

        assertThat(result.totalRowsRead()).isEqualTo(4);
        assertThat(result.insertedOrUpdatedCount()).isEqualTo(2); // 101, 102 valid; 103 closed; 104 outside VN
        assertThat(result.skippedCount()).isEqualTo(2);
        assertThat(result.skippedBreakdown()).containsEntry("CLOSED_OR_INACTIVE", 1L);
        assertThat(result.skippedBreakdown()).containsEntry("OUTSIDE_VIETNAM_BOUNDING_BOX", 1L);

        // Verify bulk repository batch invocations
        verify(bulkRepository, times(1)).batchUpsert(anyList());
    }

    @Test
    @DisplayName("Should skip Overture records below minimum confidence threshold")
    void testImportOvertureConfidenceFilter() {
        String csvContent = """
                source_id,name,category,address,lat,lon,confidence,operating_status
                ov_1,"High Confidence Cafe",cafe,"District 1, HCMC",10.7769,106.7009,0.85,active
                ov_2,"Low Confidence Rest",restaurant,"District 3, HCMC",10.7780,106.6900,0.40,active
                """;

        when(bulkRepository.batchUpsert(anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());

        ByteArrayInputStream is = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        PoiImportResultResponse result = importService.processStream(LocationSource.OVERTURE, is);

        assertThat(result.totalRowsRead()).isEqualTo(2);
        assertThat(result.insertedOrUpdatedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.skippedBreakdown()).containsEntry("LOW_CONFIDENCE", 1L);
    }

    @Test
    @DisplayName("Should normalize Vietnamese NFC unicode and address placeholders")
    void testNormalizationLogic() {
        String input = "  Cà   Phê   Sữa   Đá  ";
        String normalized = importService.normalizeText(input, 255);
        assertThat(normalized).isEqualTo("Cà Phê Sữa Đá");

        assertThat(importService.normalizeAddress("  đang cập nhật  ")).isNull();
        assertThat(importService.normalizeAddress("N/A")).isNull();
        assertThat(importService.normalizeAddress("")).isNull();
        assertThat(importService.normalizeAddress("123 Nguyễn Huệ, Q1")).isEqualTo("123 Nguyễn Huệ, Q1");
    }

    @Test
    @DisplayName("Should correctly parse complex CSV with quotes and commas")
    void testCsvTokenParsing() {
        String line = "osm_999,\"Nhà hàng, Quán ăn \"\"Ngon\"\"\",restaurant,\"18 Ba Trieu, Hanoi\",21.02,105.85,,active";
        List<String> tokens = PoiImportServiceImpl.parseCsvLineTokens(line);

        assertThat(tokens).hasSize(8);
        assertThat(tokens.get(0)).isEqualTo("osm_999");
        assertThat(tokens.get(1)).isEqualTo("Nhà hàng, Quán ăn \"Ngon\"");
        assertThat(tokens.get(2)).isEqualTo("restaurant");
        assertThat(tokens.get(3)).isEqualTo("18 Ba Trieu, Hanoi");
    }
}
