package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "The candidate's full course/certificate list. Replaces the current entries rather than adding to them.")
public class CandidateCourseUpdateRequest {

    @NotNull(message = "courses is required")
    @Valid
    @Schema(description = "The full set of course entries; an empty list clears it")
    private List<CandidateCourseRequest> courses;

}
