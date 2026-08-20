package cams.dto;

import cams.entity.JobLevel;
import cams.entity.JobListingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// JobListingResponse minus postedByUserId, and nothing else removed. This is not
// a summary DTO: there is no candidate-facing detail endpoint, so browse is the
// only place a candidate can read a description or the required skills, and both
// have to be here. postedByUserId is dropped because it leaks the internal user
// ids of employer staff to every browsing user and serves no candidate purpose.
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "A job listing as seen by a candidate browsing the platform")
public class JobListingBrowseResponse {

    @Schema(description = "Unique identifier of the listing", example = "1")
    private Long id;

    @Schema(description = "Job title", example = "Backend developer")
    private String title;

    @Schema(description = "Full description of the role", example = "You will build and maintain our Spring services.")
    private String description;

    @Schema(description = "The company the listing belongs to")
    private CompanyResponse company;

    @Schema(description = "Where the job is based; set per listing, not copied from the company", example = "Sofia, Bulgaria", nullable = true)
    private String location;

    @Schema(description = "Whether the role can be done remotely")
    private Boolean remote;

    @Schema(description = "Seniority level of the role")
    private JobLevel level;

    @Schema(description = "Always OPEN here — browse never returns archived listings")
    private JobListingStatus status;

    @Schema(description = "Skills required for this listing")
    private List<SkillResponse> skills;

    @Schema(description = "Timestamp when the listing was created")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the listing was last updated")
    private LocalDateTime updatedAt;
}
