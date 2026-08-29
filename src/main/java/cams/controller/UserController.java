package cams.controller;

import cams.dto.UpdateFullNameRequest;
import cams.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "The authenticated user's own account-level settings")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @Operation(summary = "Update the authenticated user's full name")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Full name updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @PatchMapping("/api/users/me")
    public ResponseEntity<Void> updateMyFullName(@Valid @RequestBody UpdateFullNameRequest request) {
        authService.updateMyFullName(request.getFullName());
        return ResponseEntity.noContent().build();
    }
}
