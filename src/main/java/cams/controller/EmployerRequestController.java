package cams.controller;

import cams.dto.EmployerRequestResponse;
import cams.dto.EmployerRequestReviewRequest;
import cams.entity.EmployerRequestStatus;
import cams.service.EmployerRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Employer Requests", description = "Submit and review requests to register as an employer")
@RestController
@RequiredArgsConstructor
public class EmployerRequestController {

    private final EmployerRequestService employerRequestService;

    @Operation(summary = "Get the authenticated user's most recent employer request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "No employer request found for this user")
    })
    @GetMapping("/api/employer-requests/me")
    public ResponseEntity<EmployerRequestResponse> getMyRequest() {
        return ResponseEntity.ok(employerRequestService.getMyRequest());
    }

    @Operation(summary = "List employer requests (admin only)",
            description = "Filters by status; defaults to PENDING when omitted.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Requests retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an admin")
    })
    @GetMapping("/api/admin/employer-requests")
    public ResponseEntity<List<EmployerRequestResponse>> list(
            @RequestParam(required = false) EmployerRequestStatus status) {
        return ResponseEntity.ok(employerRequestService.listForAdmin(status));
    }

    @Operation(summary = "Approve an employer request (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request approved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an admin"),
            @ApiResponse(responseCode = "404", description = "Employer request not found"),
            @ApiResponse(responseCode = "409", description = "Request has already been reviewed")
    })
    @PostMapping("/api/admin/employer-requests/{id}/approve")
    public ResponseEntity<EmployerRequestResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(employerRequestService.approve(id));
    }

    @Operation(summary = "Reject an employer request (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed - rejection reason is required"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an admin"),
            @ApiResponse(responseCode = "404", description = "Employer request not found"),
            @ApiResponse(responseCode = "409", description = "Request has already been reviewed")
    })
    @PostMapping("/api/admin/employer-requests/{id}/reject")
    public ResponseEntity<EmployerRequestResponse> reject(@PathVariable Long id,
                                                            @Valid @RequestBody EmployerRequestReviewRequest request) {
        return ResponseEntity.ok(employerRequestService.reject(id, request.getRejectionReason()));
    }
}
