package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "A single course/certificate entry")
public class CandidateCourseResponse {

    @Schema(description = "Unique identifier of the course entry", example = "1")
    private Long id;

    @Schema(description = "Course/certificate title", example = "AWS Certified Solutions Architect")
    private String name;

    @Schema(description = "Issuing organization", example = "Amazon Web Services")
    private String issuingOrganization;

    @Schema(description = "Completion date; null means ongoing/no date given", nullable = true)
    private LocalDate completionDate;

    @Schema(description = "Optional link to the credential", nullable = true)
    private String credentialUrl;

}
