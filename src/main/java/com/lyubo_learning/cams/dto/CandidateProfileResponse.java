package com.lyubo_learning.cams.dto;

import com.lyubo_learning.cams.entity.DesiredWorkingHours;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Candidate profile data returned by the API")
public class CandidateProfileResponse {

    @Schema(description = "Unique identifier of the profile", example = "1")
    private Long id;

    @Schema(description = "Headline, doubling as the desired position", example = "Backend developer", nullable = true)
    private String headline;

    @Schema(description = "Free-text profile summary", example = "Six years building Spring services.", nullable = true)
    private String description;

    @Schema(description = "Where the candidate is based", example = "Sofia, Bulgaria", nullable = true)
    private String location;

    @Schema(description = "Whether the candidate is open to remote work; null means not stated", nullable = true)
    private Boolean openToRemote;

    @Schema(description = "Whether the candidate wants flexible hours; null means not stated", nullable = true)
    private Boolean flexibleHours;

    @Schema(description = "Desired working hours category; null means not stated", nullable = true)
    private DesiredWorkingHours desiredWorkingHours;

    @Schema(description = "Skills attached to this profile")
    private List<SkillResponse> skills;

    @Schema(description = "Work experience entries attached to this profile")
    private List<CandidateExperienceResponse> experience;

    @Schema(description = "Education entries attached to this profile")
    private List<CandidateEducationResponse> education;

    @Schema(description = "Timestamp when the profile was created")
    private LocalDateTime createdAt;
}
