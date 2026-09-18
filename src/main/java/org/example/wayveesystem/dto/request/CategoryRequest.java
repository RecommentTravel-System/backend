package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    String name;

    @NotBlank(message = "Category code is required")
    String code;

    String osmKey;

    String osmValue;

    String description;

    String iconUrl;
}
