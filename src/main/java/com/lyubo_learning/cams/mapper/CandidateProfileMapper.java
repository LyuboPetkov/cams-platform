package com.lyubo_learning.cams.mapper;

import com.lyubo_learning.cams.dto.CandidateProfileResponse;
import com.lyubo_learning.cams.entity.CandidateProfile;
import com.lyubo_learning.cams.entity.Skill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CandidateProfileMapper {

    private final SkillMapper skillMapper;

    public CandidateProfileResponse toResponse(CandidateProfile entity, List<Skill> skills) {
        return CandidateProfileResponse.builder()
                .id(entity.getId())
                .headline(entity.getHeadline())
                .description(entity.getDescription())
                .location(entity.getLocation())
                .openToRemote(entity.getOpenToRemote())
                .flexibleHours(entity.getFlexibleHours())
                .desiredWorkingHours(entity.getDesiredWorkingHours())
                .skills(skills.stream().map(skillMapper::toResponse).toList())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
