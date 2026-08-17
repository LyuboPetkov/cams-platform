package com.lyubo_learning.cams.dto;

import com.lyubo_learning.cams.entity.DesiredWorkingHours;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Getter
@NoArgsConstructor
@Schema(description = "Payload for updating the caller's own candidate profile. Omitted or null fields are left unchanged.")
public class CandidateProfileUpdateRequest {

    @Size(max = 255, message = "Headline must be at most 255 characters")
    @Schema(description = "Headline, doubling as the desired position", example = "Backend developer")
    private String headline;

    @Schema(description = "Free-text profile summary", example = "Six years building Spring services.")
    private String description;

    @Size(max = 255, message = "Location must be at most 255 characters")
    @Schema(description = "Where the candidate is based", example = "Sofia, Bulgaria")
    private String location;

    @URL(message = "Picture URL must be a valid URL")
    @Size(max = 500, message = "Picture URL must be at most 500 characters")
    @Schema(description = "URL of the candidate's profile picture")
    private String pictureUrl;

    @Schema(description = "Whether the candidate is open to remote work")
    private Boolean openToRemote;

    @Schema(description = "Whether the candidate wants flexible hours")
    private Boolean flexibleHours;

    @Schema(description = "Desired working hours category")
    private DesiredWorkingHours desiredWorkingHours;
}
