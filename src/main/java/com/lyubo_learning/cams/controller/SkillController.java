package com.lyubo_learning.cams.controller;

import com.lyubo_learning.cams.dto.SkillResponse;
import com.lyubo_learning.cams.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Skills", description = "The fixed ESCO-sourced skill vocabulary")
@RestController
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @Operation(summary = "Search the skill vocabulary",
            description = "Case-insensitive substring match on the skill name. Omitting 'search' returns the entire "
                    + "vocabulary (thousands of rows), so callers should normally pass it. 'limit' is optional and "
                    + "caps the number of results returned; omitting it returns every match, as before.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Skills retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @GetMapping("/api/skills")
    public ResponseEntity<List<SkillResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(skillService.search(search, limit));
    }
}
