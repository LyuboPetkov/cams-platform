package cams.dto;

import cams.entity.ApplicationSource;
import cams.entity.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Schema(description = "Payload for creating or updating a job application")
public class JobApplicationRequest {

    @NotBlank
    @Schema(description = "Name of the company", example = "Google")
    private String companyName;

    @NotBlank
    @Schema(description = "Title of the position applied for",
    example = "Backend Engineer")
    private String jobTitle;

    @NotNull
    @Schema(description = "Current status of the application")
    private ApplicationStatus status;

    @NotNull
    @Schema(description = "How the application was submitted")
    private ApplicationSource source;

    @Schema(description = "Link to the jpb posting", example = "https://careers.google.com/jobs/123",
    nullable = true)
    private String jobUrl;

    @Schema(description = "Personal notes about the application", example = "Referral from Bob", nullable = true)
    private String notes;

    @Schema(description = "Date the application was submitted", example = "2026-06-01")
    @NotNull
    private LocalDate appliedDate;

}
