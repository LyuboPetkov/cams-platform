package cams.controller;

import cams.dto.PromoteUserRequest;
import cams.dto.ReembedCandidatesResponse;
import cams.dto.ReembedListingsResponse;
import cams.service.AdminUserService;
import cams.service.CandidateReembedService;
import cams.service.ListingReembedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin", description = "Admin-only account management")
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;
    private final ListingReembedService listingReembedService;
    private final CandidateReembedService candidateReembedService;

    @Operation(summary = "Promote an existing user to ADMIN (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User promoted successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an admin"),
            @ApiResponse(responseCode = "404", description = "No user found with the given email")
    })
    @PostMapping("/api/admin/users/promote")
    public ResponseEntity<Void> promote(@Valid @RequestBody PromoteUserRequest request) {
        adminUserService.promoteToAdmin(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    // Rebuilds every listing's embedding after a change to the embedded text.
    // Gated by the same ADMIN-only rule as the promote endpoint above.
    @Operation(summary = "Re-embed every job listing (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Re-embedding queued for all listings"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an admin")
    })
    @PostMapping("/api/admin/reembed-listings")
    public ResponseEntity<ReembedListingsResponse> reembedListings() {
        int queued = listingReembedService.reembedAllListings();
        return ResponseEntity.ok(new ReembedListingsResponse(queued));
    }

    // Candidate counterpart to reembed-listings; same ADMIN gate and response shape.
    @Operation(summary = "Re-embed every candidate profile (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Re-embedding queued for all candidate profiles"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an admin")
    })
    @PostMapping("/api/admin/reembed-candidates")
    public ResponseEntity<ReembedCandidatesResponse> reembedCandidates() {
        int queued = candidateReembedService.reembedAllCandidateProfiles();
        return ResponseEntity.ok(new ReembedCandidatesResponse(queued));
    }
}
