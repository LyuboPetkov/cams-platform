package com.lyubo_learning.cams.controller;

import com.lyubo_learning.cams.dto.CandidacyApplyRequest;
import com.lyubo_learning.cams.dto.CandidacyResponse;
import com.lyubo_learning.cams.service.CandidacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Candidacies", description = "Applying to job listings and reviewing your own applications")
@RestController
@RequiredArgsConstructor
public class CandidacyController {

    private final CandidacyService candidacyService;

    @Operation(summary = "Apply to a job listing",
            description = """
                    Creates the candidacy and, in the same transaction, a linked entry in the caller's own \
                    job application tracker with source JOB_BOARD. Only OPEN listings accept applications, \
                    and a candidate may apply to a given listing only once.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed - jobListingId is required"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "Job listing not found"),
            @ApiResponse(responseCode = "409", description = "Listing is not open, or the caller has already applied to it")
    })
    @PostMapping("/api/candidacies")
    public ResponseEntity<CandidacyResponse> apply(@Valid @RequestBody CandidacyApplyRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(candidacyService.apply(request.getJobListingId()));
    }

    @Operation(summary = "List the authenticated user's own candidacies",
            description = "Newest first. Returns only the caller's own applications.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidacies retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT token")
    })
    @GetMapping("/api/candidacies/mine")
    public ResponseEntity<List<CandidacyResponse>> getMine() {
        return ResponseEntity.ok(candidacyService.getMyCandidacies());
    }
}
