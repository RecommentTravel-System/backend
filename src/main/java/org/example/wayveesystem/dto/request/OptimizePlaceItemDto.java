package org.example.wayveesystem.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OptimizePlaceItemDto {
    String id;
    Long osmId;
    String name;
    String address;
    Double latitude;
    Double longitude;
    String category;
    String categoryVi;
    String categoryEn;
    String categoryColor;
    String barColor;
    Double rating;
    String reviewsCount;
    String tagVi;
    String tagEn;
    String image;
    Integer stayDurationMinutes;
    LocalTime openTime;
    LocalTime closeTime;
    Integer fixedDay;
}
