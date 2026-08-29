package cams.controller;

import cams.dto.JobApplicationRequest;
import cams.dto.JobApplicationResponse;
import cams.entity.ApplicationSource;
import cams.entity.ApplicationStatus;
import cams.service.JobApplicationService;
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

@Tag(name = "Job Applications", description = "Create, retrieve, update and delete job applications for the authenticated user")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @Operation(summary = "Crearte a new job application")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed - missing or invalid fields"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @PostMapping
    public ResponseEntity<JobApplicationResponse> create(@Valid @RequestBody JobApplicationRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(jobApplicationService.create(request));
    }

    @Operation(summary = "Get all job applications for the authenticated user",
            description = "Both filter parameters are optional. Omit them to retrieve all applications.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @GetMapping
    public ResponseEntity<List<JobApplicationResponse>> getAll(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false)ApplicationSource source) {
        return ResponseEntity.ok(jobApplicationService.getFiltered(status, source));
    }

    @Operation(summary = "Get a single job application by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Application belongs to a different user"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobApplicationService.getById(id));
    }

    @Operation(summary = "Update an existing job application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed = missing or invalid fields"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Application belongs to another user"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> update(@PathVariable Long id,
                                                         @Valid @RequestBody JobApplicationRequest request) {
        return ResponseEntity.ok(jobApplicationService.update(id, request));
    }

    @Operation(summary = "Delete a job application")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Application deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Application belongs to a different user"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobApplicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
