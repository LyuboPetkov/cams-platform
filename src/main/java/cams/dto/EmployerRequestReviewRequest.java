package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "Payload for rejecting an employer request")
public class EmployerRequestReviewRequest {

    @NotBlank(message = "Rejection reason is required")
    @Schema(description = "Reason the request is being rejected", example = "Company website could not be verified")
    private String rejectionReason;
}
