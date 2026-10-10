package org.example.wayveesystem.dto;

/**
 * Immutable representation of a single parsed CSV row.
 */
public record PoiCsvRow(
        String sourceId,
        String name,
        String category,
        String address,
        String latStr,
        String lonStr,
        String confidenceStr,
        String operatingStatus,
        long lineNumber
) {
}
