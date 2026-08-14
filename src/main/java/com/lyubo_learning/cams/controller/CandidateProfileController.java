package com.lyubo_learning.cams.controller;

import com.lyubo_learning.cams.dto.CandidateProfileResponse;
import com.lyubo_learning.cams.dto.CandidateProfileUpdateRequest;
import com.lyubo_learning.cams.dto.CandidateSkillsUpdateRequest;
import com.lyubo_learning.cams.service.CandidateProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Candidate Profiles", description = "The authenticated user's own candidate profile and skill selection")
@RestController
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateProfileService candidateProfileService;

    @Operation(summary = "Get the authenticated user's candidate profile",
            description = "Every account gets an empty profile at registration, so this returns 200 even before any edit.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "No candidate profile found for this user")
    })
    @GetMapping("/api/candidate-profiles/me")
    public ResponseEntity<CandidateProfileResponse> getMyProfile() {
        return ResponseEntity.ok(candidateProfileService.getMyProfile());
    }

    @Operation(summary = "Update the authenticated user's candidate profile",
            description = "Partial update — omitted or null fields are left unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "No candidate profile found for this user")
    })
    @PutMapping("/api/candidate-profiles/me")
    public ResponseEntity<CandidateProfileResponse> updateMyProfile(
            @Valid @RequestBody CandidateProfileUpdateRequest request) {
        return ResponseEntity.ok(candidateProfileService.updateMyProfile(request));
    }

    @Operation(summary = "Set the authenticated user's skills",
            description = "Replaces the current selection with the submitted list. An empty list clears it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Skills updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed - skillIds is required"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "No candidate profile found, or an unknown skill id was submitted")
    })
    @PutMapping("/api/candidate-profiles/me/skills")
    public ResponseEntity<CandidateProfileResponse> setMySkills(
            @Valid @RequestBody CandidateSkillsUpdateRequest request) {
        return ResponseEntity.ok(candidateProfileService.setMySkills(request.getSkillIds()));
    }
}
