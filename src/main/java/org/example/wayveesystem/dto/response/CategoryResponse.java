package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryResponse {

    Long categoryId;
    String name;
    String code;
    String osmKey;
    String osmValue;
    String description;
    String iconUrl;
    LocalDateTime createdAt;
}
