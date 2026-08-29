package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "Payload for updating the caller's own full name")
public class UpdateFullNameRequest {

    @NotBlank(message = "Full name is required")
    @Schema(description = "New full name", example = "John Smith")
    private String fullName;

}
