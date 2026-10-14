package org.example.wayveesystem.dto.response;

import java.util.Map;

/**
 * Summary result response for POI batch import operations.
 */
public record PoiImportResultResponse(
        String source,
        long totalRowsRead,
        long insertedOrUpdatedCount,
        long skippedCount,
        Map<String, Long> skippedBreakdown,
        long durationMs,
        String message
) {
}
