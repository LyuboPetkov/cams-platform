package com.lyubo_learning.cams.dto;

import com.lyubo_learning.cams.entity.EmployerRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Employer request data returned by the API")
public class EmployerRequestResponse {

    @Schema(description = "Unique identifier of the request", example = "1")
    private Long id;

    @Schema(description = "Current status of the request")
    private EmployerRequestStatus status;

    @Schema(description = "Name of the company", example = "Acme Inc.")
    private String companyName;

    @Schema(description = "Timestamp when the request was submitted")
    private Instant requestedAt;

    @Schema(description = "Timestamp when the request was reviewed", nullable = true)
    private Instant reviewedAt;

    @Schema(description = "Reason given if the request was rejected", nullable = true)
    private String rejectionReason;
}
