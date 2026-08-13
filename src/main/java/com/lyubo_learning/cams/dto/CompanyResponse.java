package com.lyubo_learning.cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Company profile data returned by the API")
public class CompanyResponse {

    @Schema(description = "Unique identifier of the company", example = "1")
    private Long id;

    @Schema(description = "Name of the company", example = "Acme Inc.")
    private String name;

    @Schema(description = "Description of the company", example = "We build widgets.", nullable = true)
    private String description;

    @Schema(description = "Company website", example = "https://acme.example.com", nullable = true)
    private String website;

    @Schema(description = "Company location", example = "Sofia, Bulgaria", nullable = true)
    private String location;

    @Schema(description = "URL of the company logo", nullable = true)
    private String logoUrl;

    @Schema(description = "Timestamp when the company was created")
    private LocalDateTime createdAt;
}
