package org.example.wayveesystem.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.enums.LocationSource;
import org.example.wayveesystem.common.enums.PoiStatus;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.common.exception.PoiImportException;
import org.example.wayveesystem.configuration.PoiImportProperties;
import org.example.wayveesystem.dto.PoiCsvRow;
import org.example.wayveesystem.dto.response.PoiImportResultResponse;
import org.example.wayveesystem.model.Category;
import org.example.wayveesystem.model.PoiCatalog;
import org.example.wayveesystem.repository.CategoryRepository;
import org.example.wayveesystem.repository.PoiCatalogBulkRepository;
import org.example.wayveesystem.service.PoiImportService;
import org.example.wayveesystem.service.poi.strategy.PoiSourceStrategy;
import org.example.wayveesystem.service.poi.strategy.PoiSourceStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoiImportServiceImpl implements PoiImportService {

    private final PoiCatalogBulkRepository poiCatalogBulkRepository;
    private final CategoryRepository categoryRepository;
    private final PoiSourceStrategyFactory strategyFactory;
    private final PoiImportProperties properties;

    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_ADDRESS_LENGTH = 500;

    @Override
    public PoiImportResultResponse importFromMultipart(LocationSource source, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PoiImportException(ErrorCode.INVALID_POI_FILE);
        }

        String originalFilename = file.getOriginalFilename();
        log.info("Starting POI import for source [{}] from uploaded file: {}", source, originalFilename);

        try (InputStream inputStream = file.getInputStream()) {
            return processStream(source, inputStream);
        } catch (IOException e) {
            log.error("Failed to read POI multipart file", e);
            throw new PoiImportException(ErrorCode.POI_IMPORT_FAILED);
        }
    }

    @Override
    public PoiImportResultResponse importFromServerFile(LocationSource source, String filePath) {
        String effectivePath = (filePath != null && !filePath.isBlank()) ? filePath.trim() : properties.getDefaultServerFilePath();
        Path path = Paths.get(effectivePath);

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            log.error("Server POI file not found at: {}", path.toAbsolutePath());
            throw new PoiImportException(ErrorCode.POI_FILE_NOT_FOUND);
        }

        log.info("Starting POI import for source [{}] from server path: {}", source, path.toAbsolutePath());

        try (InputStream inputStream = Files.newInputStream(path)) {
            return processStream(source, inputStream);
        } catch (IOException e) {
            log.error("Failed to read POI file from server path: {}", path.toAbsolutePath(), e);
            throw new PoiImportException(ErrorCode.POI_IMPORT_FAILED);
        }
    }

    /**
     * Reads the CSV input stream line-by-line, filters/normalizes, and flushes in batches.
     */
    public PoiImportResultResponse processStream(LocationSource source, InputStream inputStream) {
        long startTime = System.currentTimeMillis();
        PoiSourceStrategy strategy = strategyFactory.getStrategy(source);

        // Pre-load category mapping table into memory once
        Map<String, Long> categoryLookup = loadCategoryLookupMap();

        int batchSize = Math.max(100, properties.getBatchSize());
        float minConfidence = properties.getMinConfidence();
        PoiImportProperties.BoundingBox boundingBox = properties.getBoundingBox();

        long totalRowsRead = 0;
        long insertedOrUpdatedCount = 0;
        long skippedCount = 0;
        Map<String, Long> skippedBreakdown = new LinkedHashMap<>();

        List<PoiCatalog> batch = new ArrayList<>(batchSize);
        LocalDateTime importTimestamp = LocalDateTime.now();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new PoiImportException(ErrorCode.INVALID_POI_FILE);
            }

            // Remove UTF-8 BOM if present
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            Map<String, Integer> headerIndexMap = parseHeaderIndices(headerLine);

            String line;
            long lineNum = 1;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) {
                    continue;
                }

                totalRowsRead++;
                PoiCsvRow row = parseCsvLine(line, headerIndexMap, lineNum);

                // 1. Validate sourceId
                String cleanSourceId = normalizeText(row.sourceId(), 128);
                if (cleanSourceId == null || cleanSourceId.isBlank()) {
                    recordSkip(skippedBreakdown, "EMPTY_SOURCE_ID");
                    skippedCount++;
                    continue;
                }

                // 2. Validate and normalize Name
                String cleanName = normalizeText(row.name(), MAX_NAME_LENGTH);
                if (cleanName == null || cleanName.isBlank()) {
                    recordSkip(skippedBreakdown, "EMPTY_NAME");
                    skippedCount++;
                    continue;
                }

                // 3. Parse and validate Coordinates
                Float lat = parseCoordinate(row.latStr());
                Float lon = parseCoordinate(row.lonStr());
                if (lat == null || lon == null) {
                    recordSkip(skippedBreakdown, "INVALID_COORDINATES");
                    skippedCount++;
                    continue;
                }

                if (!boundingBox.contains(lat, lon)) {
                    recordSkip(skippedBreakdown, "OUTSIDE_VIETNAM_BOUNDING_BOX");
                    skippedCount++;
                    continue;
                }

                // 4. Validate Operating Status
                if (strategy.shouldSkipByStatus(row.operatingStatus())) {
                    recordSkip(skippedBreakdown, "CLOSED_OR_INACTIVE");
                    skippedCount++;
                    continue;
                }

                // 5. Parse and validate Confidence
                Float confidence = parseConfidence(row.confidenceStr());
                if (strategy.shouldSkipByConfidence(confidence, minConfidence)) {
                    recordSkip(skippedBreakdown, "LOW_CONFIDENCE");
                    skippedCount++;
                    continue;
                }

                // 6. Map Category
                Long categoryId = resolveCategoryId(row.category(), categoryLookup);
                if (categoryId == null) {
                    recordSkip(skippedBreakdown, "UNMAPPED_CATEGORY");
                    skippedCount++;
                    continue;
                }

                // 7. Normalize Address (null if empty or placeholder)
                String cleanAddress = normalizeAddress(row.address());

                PoiStatus poiStatus = strategy.resolveStatus(row.operatingStatus());

                PoiCatalog entity = PoiCatalog.builder()
                        .source(source)
                        .sourceId(cleanSourceId)
                        .name(cleanName)
                        .categoryId(categoryId)
                        .address(cleanAddress)
                        .lat(lat)
                        .lon(lon)
                        .confidence(confidence)
                        .status(poiStatus)
                        .importedAt(importTimestamp)
                        .build();

                batch.add(entity);

                if (batch.size() >= batchSize) {
                    int affected = poiCatalogBulkRepository.batchUpsert(batch);
                    insertedOrUpdatedCount += affected;
                    batch.clear();

                    if (totalRowsRead % 10000 == 0 || totalRowsRead == batchSize) {
                        log.info("POI Import [{}]: Read {} rows, Saved {} items, Skipped {} items so far...",
                                source, totalRowsRead, insertedOrUpdatedCount, skippedCount);
                    }
                }
            }

            // Flush remaining items
            if (!batch.isEmpty()) {
                int affected = poiCatalogBulkRepository.batchUpsert(batch);
                insertedOrUpdatedCount += affected;
                batch.clear();
            }

        } catch (IOException e) {
            log.error("Error occurred while streaming POI CSV lines", e);
            throw new PoiImportException(ErrorCode.POI_IMPORT_FAILED);
        }

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("POI Import [{}] completed in {} ms. Total read: {}, Upserted: {}, Skipped: {}",
                source, durationMs, totalRowsRead, insertedOrUpdatedCount, skippedCount);

        return new PoiImportResultResponse(
                source.name(),
                totalRowsRead,
                insertedOrUpdatedCount,
                skippedCount,
                skippedBreakdown,
                durationMs,
                "POI data imported successfully"
        );
    }

    /**
     * Pre-loads categories from the database and constructs a normalized code/name lookup map.
     */
    private Map<String, Long> loadCategoryLookupMap() {
        Map<String, Long> lookup = new HashMap<>();
        List<Category> allCategories = categoryRepository.findAll();

        for (Category cat : allCategories) {
            if (cat.getCode() != null) {
                lookup.put(cat.getCode().trim().toUpperCase(), cat.getCategoryId());
            }
            if (cat.getName() != null) {
                lookup.put(cat.getName().trim().toLowerCase(), cat.getCategoryId());
            }
            if (cat.getOsmValue() != null) {
                lookup.put(cat.getOsmValue().trim().toLowerCase(), cat.getCategoryId());
            }
        }
        return lookup;
    }

    /**
     * Resolves raw category text into an existing database categoryId.
     */
    private Long resolveCategoryId(String rawCategory, Map<String, Long> categoryLookup) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return null;
        }

        String clean = rawCategory.trim().toLowerCase();

        // Check if directly matched in DB codes / names / osm values
        Long id = categoryLookup.get(clean.toUpperCase());
        if (id != null) {
            return id;
        }
        id = categoryLookup.get(clean);
        if (id != null) {
            return id;
        }

        // Check configuration mapping
        Map<String, String> mapping = properties.getCategoryMapping();
        if (mapping != null && mapping.containsKey(clean)) {
            String targetCode = mapping.get(clean);
            return categoryLookup.get(targetCode.toUpperCase());
        }

        return null;
    }

    /**
     * Normalizes text string using Unicode NFC, strips extra spaces, and caps length.
     */
    public String normalizeText(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Normalize Unicode to NFC for Vietnamese accented characters
        String nfc = Normalizer.normalize(trimmed, Normalizer.Form.NFC);
        // Replace multiple consecutive spaces/tabs with a single space
        String collapsed = MULTI_SPACE_PATTERN.matcher(nfc).replaceAll(" ");
        if (collapsed.length() > maxLength) {
            return collapsed.substring(0, maxLength).trim();
        }
        return collapsed;
    }

    /**
     * Normalizes address: returns null if empty or containing placeholder text.
     */
    public String normalizeAddress(String address) {
        String clean = normalizeText(address, MAX_ADDRESS_LENGTH);
        if (clean == null || clean.isBlank()) {
            return null;
        }
        String lower = clean.toLowerCase();
        if (lower.equals("đang cập nhật") || lower.equals("chưa có") || lower.equals("n/a") || lower.equals("null")) {
            return null;
        }
        return clean;
    }

    private Float parseCoordinate(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            return Float.parseFloat(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Float parseConfidence(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            return Float.parseFloat(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void recordSkip(Map<String, Long> breakdown, String reason) {
        breakdown.put(reason, breakdown.getOrDefault(reason, 0L) + 1L);
    }

    /**
     * Parses the CSV header columns into index mapping.
     */
    private Map<String, Integer> parseHeaderIndices(String headerLine) {
        Map<String, Integer> indexMap = new HashMap<>();
        List<String> tokens = parseCsvLineTokens(headerLine);

        for (int i = 0; i < tokens.size(); i++) {
            String col = tokens.get(i).trim().toLowerCase().replaceAll("^\"|\"$", "");
            indexMap.put(col, i);
        }
        return indexMap;
    }

    /**
     * Parses a CSV line into a PoiCsvRow record.
     */
    private PoiCsvRow parseCsvLine(String line, Map<String, Integer> headerIndexMap, long lineNum) {
        List<String> tokens = parseCsvLineTokens(line);

        String sourceId = getColumnValue(tokens, headerIndexMap, "source_id", 0);
        String name = getColumnValue(tokens, headerIndexMap, "name", 1);
        String category = getColumnValue(tokens, headerIndexMap, "category", 2);
        String address = getColumnValue(tokens, headerIndexMap, "address", 3);
        String latStr = getColumnValue(tokens, headerIndexMap, "lat", 4);
        String lonStr = getColumnValue(tokens, headerIndexMap, "lon", 5);
        String confidenceStr = getColumnValue(tokens, headerIndexMap, "confidence", 6);
        String operatingStatus = getColumnValue(tokens, headerIndexMap, "operating_status", 7);

        return new PoiCsvRow(sourceId, name, category, address, latStr, lonStr, confidenceStr, operatingStatus, lineNum);
    }

    private String getColumnValue(List<String> tokens, Map<String, Integer> headerIndexMap, String colName, int defaultIdx) {
        Integer idx = headerIndexMap.get(colName);
        if (idx == null) {
            idx = defaultIdx;
        }
        if (idx >= 0 && idx < tokens.size()) {
            return tokens.get(idx);
        }
        return null;
    }

    /**
     * RFC 4180 compliant CSV token reader supporting quotes and commas within quotes.
     */
    public static List<String> parseCsvLineTokens(String line) {
        List<String> tokens = new ArrayList<>();
        if (line == null) {
            return tokens;
        }

        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }
}
