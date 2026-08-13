package com.lyubo_learning.cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Getter
@NoArgsConstructor
@Schema(description = "Payload for updating the caller's own company profile")
public class CompanyUpdateRequest {

    @Schema(description = "Description of the company", example = "We build widgets.")
    private String description;

    @URL(message = "Website must be a valid URL")
    @Size(max = 500, message = "Website must be at most 500 characters")
    @Schema(description = "Company website", example = "https://acme.example.com")
    private String website;

    @Size(max = 255, message = "Location must be at most 255 characters")
    @Schema(description = "Company location", example = "Sofia, Bulgaria")
    private String location;

    @URL(message = "Logo URL must be a valid URL")
    @Size(max = 500, message = "Logo URL must be at most 500 characters")
    @Schema(description = "URL of the company logo")
    private String logoUrl;
}
