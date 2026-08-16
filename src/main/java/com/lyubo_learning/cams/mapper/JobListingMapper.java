package com.lyubo_learning.cams.mapper;

import com.lyubo_learning.cams.dto.JobListingBrowseResponse;
import com.lyubo_learning.cams.dto.JobListingResponse;
import com.lyubo_learning.cams.entity.JobListing;
import com.lyubo_learning.cams.entity.Skill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JobListingMapper {

    private final CompanyMapper companyMapper;
    private final SkillMapper skillMapper;

    public JobListingResponse toResponse(JobListing entity, List<Skill> skills) {
        return JobListingResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .company(companyMapper.toResponse(entity.getCompany()))
                // Reading only the id off the lazy proxy — this does not initialise it.
                .postedByUserId(entity.getPostedBy().getId())
                .location(entity.getLocation())
                .remote(entity.getRemote())
                .level(entity.getLevel())
                .status(entity.getStatus())
                .skills(skills.stream().map(skillMapper::toResponse).toList())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // The candidate-facing shape: the same fields minus postedByUserId.
    public JobListingBrowseResponse toBrowseResponse(JobListing entity, List<Skill> skills) {
        return JobListingBrowseResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .company(companyMapper.toResponse(entity.getCompany()))
                .location(entity.getLocation())
                .remote(entity.getRemote())
                .level(entity.getLevel())
                .status(entity.getStatus())
                .skills(skills.stream().map(skillMapper::toResponse).toList())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
