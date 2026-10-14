package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.ReplanAction;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReplanConfirmRequest {

    @NotBlank(message = "Proposal ID is required")
    String proposalId;

    Long expectedVersion;

    /**
     * Map of tripLocationId -> chosen ReplanAction (SKIP, ADD_EXTRA_DAY) for unplaceable items.
     */
    Map<Long, ReplanAction> unplaceableDecisions;
}
