package org.example.wayveesystem.dto.response;

import java.util.List;

public record LocationPageResponse(
        List<LocationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
