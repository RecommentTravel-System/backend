package org.example.wayveesystem.dto.response;

import lombok.Builder;

@Builder
public record LocationResponse(
        Long locationId,
        Long sourceOsmId,
        String name,
        String categoryCode,
        String address,
        Double latitude,
        Double longitude,
        String imageUrl,
        Double rating,
        Integer reviewCount,
        Double distanceMeters
) {}
