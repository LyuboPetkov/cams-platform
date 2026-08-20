package cams.dto;

import cams.entity.JobLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "Payload for creating a job listing. The owning company and the posting employer are taken from the authenticated user, never from this body.")
public class JobListingCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be at most 255 characters")
    @Schema(description = "Job title", example = "Backend developer")
    private String title;

    @NotBlank(message = "Description is required")
    @Schema(description = "Full description of the role", example = "You will build and maintain our Spring services.")
    private String description;

    @Size(max = 255, message = "Location must be at most 255 characters")
    @Schema(description = "Where the job is based", example = "Sofia, Bulgaria")
    private String location;

    @NotNull(message = "Remote is required")
    @Schema(description = "Whether the role can be done remotely")
    private Boolean remote;

    @NotNull(message = "Level is required")
    @Schema(description = "Seniority level of the role", example = "MID")
    private JobLevel level;

    @Schema(description = "Ids of the required skills; may be omitted or empty", example = "[12, 3481, 907]")
    private List<Long> skillIds;
}
