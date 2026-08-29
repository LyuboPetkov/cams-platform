package cams.controller;


import cams.dto.auth.AuthResponse;
import cams.dto.auth.LoginRequest;
import cams.dto.auth.RegisterRequest;
import cams.dto.auth.RegisterResponse;
import cams.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Register a new account and log in to receive a JWT token")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user account",
            description = "Candidate registration returns a JWT token immediately. Employer registration " +
                    "submits a request for admin review instead - no token is issued until it's approved.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created - candidate: JWT token issued; employer: submitted for review"),
            @ApiResponse(responseCode = "400", description = "Validation failed - missing or invalid fields"),
            @ApiResponse(responseCode = "409", description = "Email or company name is already in use")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Log in and receive a JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful - returns JWT token"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
