package cams.controller;

import cams.dto.CandidateCourseUpdateRequest;
import cams.dto.CandidateEducationUpdateRequest;
import cams.dto.CandidateExperienceUpdateRequest;
import cams.dto.CandidateProfileResponse;
import cams.dto.CandidateProfileUpdateRequest;
import cams.dto.CandidateSkillsUpdateRequest;
import cams.service.CandidateProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            description = "Partial update - omitted or null fields are left unchanged.")
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

    @Operation(summary = "Upload the authenticated user's profile picture",
            description = "Multipart upload. Accepts PNG, JPEG or WebP up to 5MB; anything else is rejected. " +
                    "Uploads to object storage and stores the resulting URL as pictureUrl.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Picture uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "File is not an accepted image type, or exceeds 5MB"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "No candidate profile found for this user")
    })
    @PostMapping(value = "/api/candidate-profiles/me/picture", consumes = "multipart/form-data")
    public ResponseEntity<CandidateProfileResponse> uploadMyPicture(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(candidateProfileService.uploadMyPicture(file));
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

    @Operation(summary = "Set the authenticated user's work experience",
            description = "Replaces the current experience list with the submitted one. An empty list clears it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Experience updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "No candidate profile found for this user")
    })
    @PutMapping("/api/candidate-profiles/me/experience")
    public ResponseEntity<CandidateProfileResponse> setMyExperience(
            @Valid @RequestBody CandidateExperienceUpdateRequest request) {
        return ResponseEntity.ok(candidateProfileService.setMyExperience(request.getExperiences()));
    }

    @Operation(summary = "Set the authenticated user's education",
            description = "Replaces the current education list with the submitted one. An empty list clears it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Education updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed, e.g. endDate before startDate"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "No candidate profile found for this user")
    })
    @PutMapping("/api/candidate-profiles/me/education")
    public ResponseEntity<CandidateProfileResponse> setMyEducation(
            @Valid @RequestBody CandidateEducationUpdateRequest request) {
        return ResponseEntity.ok(candidateProfileService.setMyEducation(request.getEducations()));
    }

    @Operation(summary = "Set the authenticated user's courses/certificates",
            description = "Replaces the current course list with the submitted one. An empty list clears it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courses updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "No candidate profile found for this user")
    })
    @PutMapping("/api/candidate-profiles/me/courses")
    public ResponseEntity<CandidateProfileResponse> setMyCourses(
            @Valid @RequestBody CandidateCourseUpdateRequest request) {
        return ResponseEntity.ok(candidateProfileService.setMyCourses(request.getCourses()));
    }
}
