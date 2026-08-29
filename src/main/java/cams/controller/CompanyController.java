package cams.controller;

import cams.dto.CompanyResponse;
import cams.dto.CompanyUpdateRequest;
import cams.dto.PageResponse;
import cams.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Companies", description = "Company profile retrieval and management")
@RestController
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "List all companies",
            description = "Paginated, alphabetical by name. Readable by any authenticated user. " +
                    "query is an optional case-insensitive substring match on name; omitted means \"don't care\".")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of companies retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT token")
    })
    @GetMapping("/api/companies")
    public ResponseEntity<PageResponse<CompanyResponse>> listAll(
            @Parameter(description = "Case-insensitive substring of the company's name", example = "Acme")
            @RequestParam(required = false) String query,
            @Parameter(description = "Page index, 0-based", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Results per page, capped at 50", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(companyService.listAll(query, page, size));
    }

    @Operation(summary = "Get a company's public profile by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    @GetMapping("/api/companies/{id}")
    public ResponseEntity<CompanyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getById(id));
    }

    @Operation(summary = "Get the authenticated employer's own company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an employer"),
            @ApiResponse(responseCode = "404", description = "No company found for this user")
    })
    @GetMapping("/api/companies/me")
    public ResponseEntity<CompanyResponse> getMyCompany() {
        return ResponseEntity.ok(companyService.getMyCompany());
    }

    @Operation(summary = "Update the authenticated employer's own company",
            description = "Partial update - only description, website, location and logoUrl are editable. The company name cannot be changed here.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an employer"),
            @ApiResponse(responseCode = "404", description = "No company found for this user")
    })
    @PutMapping("/api/companies/me")
    public ResponseEntity<CompanyResponse> updateMyCompany(@Valid @RequestBody CompanyUpdateRequest request) {
        return ResponseEntity.ok(companyService.updateMyCompany(request));
    }

    @Operation(summary = "Upload the authenticated employer's company logo",
            description = "Multipart upload. Accepts PNG, JPEG or WebP up to 5MB; anything else is rejected. " +
                    "Uploads to object storage and stores the resulting URL as logoUrl.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logo uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "File is not an accepted image type, or exceeds 5MB"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an employer"),
            @ApiResponse(responseCode = "404", description = "No company found for this user")
    })
    @PostMapping(value = "/api/companies/me/logo", consumes = "multipart/form-data")
    public ResponseEntity<CompanyResponse> uploadMyLogo(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(companyService.uploadMyLogo(file));
    }
}
