package com.lyubo_learning.cams.mapper;

import com.lyubo_learning.cams.dto.SkillResponse;
import com.lyubo_learning.cams.entity.Skill;
import org.springframework.stereotype.Component;

@Component
public class SkillMapper {

    public SkillResponse toResponse(Skill entity) {
        return SkillResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
