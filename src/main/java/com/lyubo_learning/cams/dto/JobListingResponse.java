package com.lyubo_learning.cams.dto;

import com.lyubo_learning.cams.entity.JobLevel;
import com.lyubo_learning.cams.entity.JobListingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Job listing data returned by the API")
public class JobListingResponse {

    @Schema(description = "Unique identifier of the listing", example = "1")
    private Long id;

    @Schema(description = "Job title", example = "Backend developer")
    private String title;

    @Schema(description = "Full description of the role", example = "You will build and maintain our Spring services.")
    private String description;

    @Schema(description = "The company the listing belongs to")
    private CompanyResponse company;

    @Schema(description = "Id of the employer who posted the listing", example = "7")
    private Long postedByUserId;

    @Schema(description = "Where the job is based; set per listing, not copied from the company", example = "Sofia, Bulgaria", nullable = true)
    private String location;

    @Schema(description = "Whether the role can be done remotely")
    private Boolean remote;

    @Schema(description = "Seniority level of the role")
    private JobLevel level;

    @Schema(description = "Whether the listing is open or archived")
    private JobListingStatus status;

    @Schema(description = "Skills required for this listing")
    private List<SkillResponse> skills;

    @Schema(description = "Timestamp when the listing was created")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the listing was last updated")
    private LocalDateTime updatedAt;
}
