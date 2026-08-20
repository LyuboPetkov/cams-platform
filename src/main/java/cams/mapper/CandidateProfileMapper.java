package cams.mapper;

import cams.dto.CandidateEducationResponse;
import cams.dto.CandidateExperienceResponse;
import cams.dto.CandidateProfileResponse;
import cams.entity.CandidateEducation;
import cams.entity.CandidateExperience;
import cams.entity.CandidateProfile;
import cams.entity.Skill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CandidateProfileMapper {

    private final SkillMapper skillMapper;

    public CandidateProfileResponse toResponse(CandidateProfile entity, List<Skill> skills,
                                                List<CandidateExperience> experiences,
                                                List<CandidateEducation> educations) {
        return CandidateProfileResponse.builder()
                .id(entity.getId())
                .headline(entity.getHeadline())
                .description(entity.getDescription())
                .location(entity.getLocation())
                .pictureUrl(entity.getPictureUrl())
                .openToRemote(entity.getOpenToRemote())
                .flexibleHours(entity.getFlexibleHours())
                .desiredWorkingHours(entity.getDesiredWorkingHours())
                .skills(skills.stream().map(skillMapper::toResponse).toList())
                .experience(experiences.stream().map(this::toResponse).toList())
                .education(educations.stream().map(this::toResponse).toList())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public CandidateExperienceResponse toResponse(CandidateExperience entity) {
        return CandidateExperienceResponse.builder()
                .id(entity.getId())
                .roleTitle(entity.getRoleTitle())
                .yearsOfExperience(entity.getYearsOfExperience())
                .description(entity.getDescription())
                .build();
    }

    public CandidateEducationResponse toResponse(CandidateEducation entity) {
        return CandidateEducationResponse.builder()
                .id(entity.getId())
                .institutionName(entity.getInstitutionName())
                .level(entity.getLevel())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .build();
    }
}
