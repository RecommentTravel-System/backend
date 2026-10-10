package org.example.wayveesystem.service.replanning.constraint;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConstraintValidationResult {
    boolean valid;
    String reason;

    public static ConstraintValidationResult success() {
        return new ConstraintValidationResult(true, null);
    }

    public static ConstraintValidationResult fail(String reason) {
        return new ConstraintValidationResult(false, reason);
    }
}
