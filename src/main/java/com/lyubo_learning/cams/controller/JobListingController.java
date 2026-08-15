package com.lyubo_learning.cams.controller;

import com.lyubo_learning.cams.dto.JobListingCreateRequest;
import com.lyubo_learning.cams.dto.JobListingResponse;
import com.lyubo_learning.cams.dto.JobListingSkillsUpdateRequest;
import com.lyubo_learning.cams.dto.JobListingUpdateRequest;
import com.lyubo_learning.cams.service.JobListingService;
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

@Tag(name = "Job Listings", description = "Employer-side creation and management of job listings")
@RestController
@RequiredArgsConstructor
public class JobListingController {

    private final JobListingService jobListingService;

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
}
