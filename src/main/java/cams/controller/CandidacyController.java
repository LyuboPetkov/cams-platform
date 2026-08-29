package cams.controller;

import cams.dto.CandidacyApplyRequest;
import cams.dto.CandidacyResponse;
import cams.dto.CandidateProfileResponse;
import cams.service.CandidacyService;
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

    @Operation(summary = "Get a candidate's full profile for a candidacy",
            description = """
                    Employer-scoped: the caller must be an employer at the company owning the listing this \
                    candidacy is against, same ownership check as accept/reject. Returns the same profile \
                    shape as the candidate's own GET /api/candidate-profiles/me, including sections not shown \
                    on the applicants list - experience, education and courses.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidate profile retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Caller is not an employer, or the listing belongs to another company"),
            @ApiResponse(responseCode = "404", description = "Candidacy not found, or the candidate has no profile")
    })
    @GetMapping("/api/candidacies/{id}/candidate-profile")
    public ResponseEntity<CandidateProfileResponse> getCandidateProfile(@PathVariable Long id) {
        return ResponseEntity.ok(candidacyService.getCandidateProfileForEmployer(id));
    }

    // A dedicated endpoint per decision rather than one status field - the endpoint
    // is the choice. Same dedicated-action shape as POST /api/job-listings/{id}/archive.
    @Operation(summary = "Accept a candidacy",
            description = """
                    Moves the candidacy SUBMITTED -> ACCEPTED and the candidate's linked tracker entry to \
                    INTERVIEWING, in one transaction. Any employer at the company owning the listing may \
                    decide. Only a SUBMITTED candidacy can be accepted; the post-interview move out of \
                    ACCEPTED is /offer or /reject.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidacy accepted"),
            @ApiResponse(responseCode = "403", description = "Caller is not an employer, or the listing belongs to another company"),
            @ApiResponse(responseCode = "404", description = "Candidacy not found"),
            @ApiResponse(responseCode = "409", description = "Candidacy is not SUBMITTED, so it cannot be accepted")
    })
    @PostMapping("/api/candidacies/{id}/accept")
    public ResponseEntity<CandidacyResponse> accept(@PathVariable Long id) {
        return ResponseEntity.ok(candidacyService.accept(id));
    }

    @Operation(summary = "Reject a candidacy",
            description = """
                    Moves the candidacy to REJECTED and the candidate's linked tracker entry to REJECTED, \
                    in one transaction. Allowed from SUBMITTED (a plain rejection) or from ACCEPTED (a \
                    post-interview rejection). Terminal once reached.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidacy rejected"),
            @ApiResponse(responseCode = "403", description = "Caller is not an employer, or the listing belongs to another company"),
            @ApiResponse(responseCode = "404", description = "Candidacy not found"),
            @ApiResponse(responseCode = "409", description = "Candidacy is neither SUBMITTED nor ACCEPTED, so it cannot be rejected")
    })
    @PostMapping("/api/candidacies/{id}/reject")
    public ResponseEntity<CandidacyResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(candidacyService.reject(id));
    }

    @Operation(summary = "Mark a candidacy as offered",
            description = """
                    Moves the candidacy ACCEPTED -> OFFERED and the candidate's linked tracker entry to \
                    OFFERED, in one transaction. The post-interview outcome: only an already-ACCEPTED \
                    candidacy can be offered. Terminal once reached.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidacy marked as offered"),
            @ApiResponse(responseCode = "403", description = "Caller is not an employer, or the listing belongs to another company"),
            @ApiResponse(responseCode = "404", description = "Candidacy not found"),
            @ApiResponse(responseCode = "409", description = "Candidacy is not ACCEPTED, so it cannot be offered")
    })
    @PostMapping("/api/candidacies/{id}/offer")
    public ResponseEntity<CandidacyResponse> offer(@PathVariable Long id) {
        return ResponseEntity.ok(candidacyService.offer(id));
    }
}
