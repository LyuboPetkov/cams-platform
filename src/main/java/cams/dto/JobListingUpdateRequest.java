package cams.dto;

import cams.entity.JobLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "Payload for updating a job listing. Omitted or null fields are left unchanged. Status is not settable here — use the archive endpoint.")
public class JobListingUpdateRequest {

    @Size(max = 255, message = "Title must be at most 255 characters")
    @Schema(description = "Job title", example = "Senior backend developer")
    private String title;

    @Schema(description = "Full description of the role", example = "You will build and maintain our Spring services.")
    private String description;

    @Size(max = 255, message = "Location must be at most 255 characters")
    @Schema(description = "Where the job is based", example = "Sofia, Bulgaria")
    private String location;

    @Schema(description = "Whether the role can be done remotely")
    private Boolean remote;

    @Schema(description = "Seniority level of the role", example = "SENIOR")
    private JobLevel level;
}
