package cams.dto;


import cams.entity.ApplicationSource;
import cams.entity.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Job application data returned by the API")
public class JobApplicationResponse {

    @Schema(description = "Unique identifier of the application", example = "1")
    private Long id;

    @Schema(description = "Name of the company", example = "Google")
    private String companyName;

    @Schema(description = "Title of the position applied for", example = "Backend Engineer")
    private String jobTitle;

    @Schema(description = "Current status of the application")
    private ApplicationStatus status;

    @Schema(description = "How the application was submitted")
    private ApplicationSource source;

    @Schema(description = "Link to the job posting", example = "https://careers.google.com/jobs/123", nullable = true)
    private String jobUrl;

    @Schema(description = "Personal notes about the application", nullable = true)
    private String notes;

    @Schema(description = "Date the application was submitted", example = "2026-06-01")
    private LocalDate appliedDate;

    @Schema(description = "Timestamp when this record was created")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when this record was last updated")
    private LocalDateTime updatedAt;
}
