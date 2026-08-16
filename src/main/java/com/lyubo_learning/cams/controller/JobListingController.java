package com.lyubo_learning.cams.controller;

import com.lyubo_learning.cams.dto.CandidacyResponse;
import com.lyubo_learning.cams.dto.JobListingBrowseResponse;
import com.lyubo_learning.cams.dto.JobListingCreateRequest;
import com.lyubo_learning.cams.dto.JobListingResponse;
import com.lyubo_learning.cams.dto.JobListingSkillsUpdateRequest;
import com.lyubo_learning.cams.dto.JobListingUpdateRequest;
import com.lyubo_learning.cams.dto.PageResponse;
import com.lyubo_learning.cams.entity.JobLevel;
import com.lyubo_learning.cams.service.CandidacyService;
import com.lyubo_learning.cams.service.JobListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Job Listings", description = "Employer-side creation and management of job listings, plus the candidate-facing browse")
@RestController
@RequiredArgsConstructor
public class JobListingController {

    private final JobListingService jobListingService;
    private final CandidacyService candidacyService;

    @Operation(summary = "Browse open job listings",
            description = """
                    Returns OPEN listings from across the whole platform — archived listings are never \
                    included and cannot be requested. Readable by any authenticated user, not just candidates.

                    Filters are optional and ANDed together: an omitted filter means "don't care". \
                    location is a case-insensitive substring match, so ?location=Bulgaria works as a \
                    country filter; listings with no location never match it, and are reached via ?remote=true. \
                    remote has three states — omitted means "don't care", true means remote only, false means onsite only. \
                    skillIds is ANY-of, not all-of: ?skillIds=3&skillIds=17 returns listings requiring \
                    either skill. An unknown skill id simply matches nothing rather than being an error.

                    page is 0-based and defaults to 0. size defaults to 20 and is capped at 50. Out-of-range \
                    values are clamped rather than rejected. The sort is fixed to newest-first and cannot be \
                    changed; results are not ranked by relevance.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of matching listings retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "A query parameter could not be parsed, e.g. an unknown level or a non-numeric page"),
            // Documented as 403 rather than the 401 the other endpoints here claim,
            // because 403 is what this application actually returns for a missing
            // token: no AuthenticationEntryPoint is configured, so Spring Security
            // falls back to Http403ForbiddenEntryPoint. The older 401s in this file
            // are inaccurate for the same reason and are left as they are.
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT token")
    })
    @GetMapping("/api/job-listings")
    public ResponseEntity<PageResponse<JobListingBrowseResponse>> browse(
            @Parameter(description = "Case-insensitive substring of the listing's location", example = "Sofia")
            @RequestParam(required = false) String location,
            // Boolean, never boolean: with a primitive an absent parameter would
            // bind to false, silently turning "don't care" into "onsite only".
            @Parameter(description = "Omit for both, true for remote only, false for onsite only")
            @RequestParam(required = false) Boolean remote,
            @Parameter(description = "Seniority level of the role")
            @RequestParam(required = false) JobLevel level,
            @Parameter(description = "Match listings requiring ANY of these skills", example = "3")
            @RequestParam(required = false) List<Long> skillIds,
            @Parameter(description = "Page index, 0-based", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Results per page, capped at 50", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(jobListingService.browseListings(location, remote, level, skillIds, page, size));
    }

    @Operation(summary = "Create a job listing",
            description = "The owning company and the posting employer are taken from the authenticated user. New listings are always created OPEN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Listing created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an employer"),
            @ApiResponse(responseCode = "404", description = "No company found for this user, or an unknown skill id was submitted")
    })
    @PostMapping("/api/job-listings")
    public ResponseEntity<JobListingResponse> create(@Valid @RequestBody JobListingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobListingService.createListing(request));
    }

    @Operation(summary = "List the listings of the authenticated employer's company",
            description = "Returns every listing owned by the caller's company, whatever its status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an employer"),
            @ApiResponse(responseCode = "404", description = "No company found for this user")
    })
    @GetMapping("/api/job-listings/mine")
    public ResponseEntity<List<JobListingResponse>> getMine() {
        return ResponseEntity.ok(jobListingService.getMyListings());
    }

    @Operation(summary = "Get a single listing owned by the authenticated employer's company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an employer, or the listing belongs to another company"),
            @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    @GetMapping("/api/job-listings/{id}")
    public ResponseEntity<JobListingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobListingService.getById(id));
    }

    @Operation(summary = "Update a job listing",
            description = "Partial update — omitted or null fields are left unchanged. Status cannot be changed here; use the archive endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an employer, or the listing belongs to another company"),
            @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    @PutMapping("/api/job-listings/{id}")
    public ResponseEntity<JobListingResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody JobListingUpdateRequest request) {
        return ResponseEntity.ok(jobListingService.updateListing(id, request));
    }

    @Operation(summary = "Set a listing's required skills",
            description = "Replaces the current selection with the submitted list. An empty list clears it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Skills updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed - skillIds is required"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an employer, or the listing belongs to another company"),
            @ApiResponse(responseCode = "404", description = "Listing not found, or an unknown skill id was submitted")
    })
    @PutMapping("/api/job-listings/{id}/skills")
    public ResponseEntity<JobListingResponse> setSkills(@PathVariable Long id,
                                                        @Valid @RequestBody JobListingSkillsUpdateRequest request) {
        return ResponseEntity.ok(jobListingService.setListingSkills(id, request.getSkillIds()));
    }

    @Operation(summary = "Archive a job listing",
            description = "The only removal path — listings are never hard-deleted. There is no unarchive endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing archived successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an employer, or the listing belongs to another company"),
            @ApiResponse(responseCode = "404", description = "Listing not found"),
            @ApiResponse(responseCode = "409", description = "Listing has already been archived")
    })
    @PostMapping("/api/job-listings/{id}/archive")
    public ResponseEntity<JobListingResponse> archive(@PathVariable Long id) {
        return ResponseEntity.ok(jobListingService.archiveListing(id));
    }

    // Lives here rather than in CandidacyController on purpose: the EMPLOYER role
    // gate is keyed on the /api/job-listings/** path, not on the controller class,
    // so this route inherits it for free. That makes this the first controller in
    // the codebase to depend on a second service — a deliberate shape, not drift.
    @Operation(summary = "List the candidacies submitted against one of the company's listings",
            description = "Company-scoped through the same ownership check as the other listing routes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidacies retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an employer, or the listing belongs to another company"),
            @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    @GetMapping("/api/job-listings/{id}/candidacies")
    public ResponseEntity<List<CandidacyResponse>> getCandidacies(@PathVariable Long id) {
        return ResponseEntity.ok(candidacyService.getCandidaciesForListing(id));
    }
}
