package cams.controller;

import cams.dto.CompanyResponse;
import cams.dto.CompanyUpdateRequest;
import cams.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Companies", description = "Company profile retrieval and management")
@RestController
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

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
            description = "Partial update — only description, website, location and logoUrl are editable. The company name cannot be changed here.")
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
}
