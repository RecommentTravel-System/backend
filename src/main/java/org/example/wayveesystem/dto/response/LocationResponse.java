package org.example.wayveesystem.dto.response;

import lombok.Builder;

@Builder
public record LocationResponse(
        Long osmId,
        String name,
        String categoryCode,
        String address,
        Double latitude,
        Double longitude,
        String imageUrl,
        String openingHours,
        String phone,
        String website,
        Double distanceMeters
) {}
