package cams.mapper;

import cams.dto.SkillResponse;
import cams.entity.Skill;
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
