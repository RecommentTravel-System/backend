package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.wayveesystem.common.enums.LocationSource;

/**
 * Request payload for importing POI data from an existing file path on the server.
 */
public record PoiImportServerFileRequest(
        @NotNull(message = "SOURCE_REQUIRED")
        LocationSource source,

        @NotBlank(message = "FILE_PATH_REQUIRED")
        String filePath
) {
}
