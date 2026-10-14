package org.example.wayveesystem.service;

import org.example.wayveesystem.common.enums.LocationSource;
import org.example.wayveesystem.dto.response.PoiImportResultResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PoiImportService {

    /**
     * Imports POI data from an uploaded multipart CSV file.
     *
     * @param source Location source (OSM or OVERTURE).
     * @param file   Uploaded CSV file.
     * @return Summary of the import operation.
     */
    PoiImportResultResponse importFromMultipart(LocationSource source, MultipartFile file);

    /**
     * Imports POI data from a file stored directly on the server filesystem.
     *
     * @param source   Location source (OSM or OVERTURE).
     * @param filePath Absolute or relative path to the CSV file.
     * @return Summary of the import operation.
     */
    PoiImportResultResponse importFromServerFile(LocationSource source, String filePath);
}
