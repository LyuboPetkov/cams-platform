package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Request DTOs in this codebase use @Getter @NoArgsConstructor plus jakarta
// validation — not the @Builder/@AllArgsConstructor shape the response DTOs use.
// Jackson needs the no-arg constructor to deserialise this.
@Getter
@NoArgsConstructor
@Schema(description = "Payload for applying to a job listing")
public class CandidacyApplyRequest {

    @NotNull(message = "jobListingId is required")
    @Schema(description = "Id of the listing being applied to", example = "12")
    private Long jobListingId;
}
