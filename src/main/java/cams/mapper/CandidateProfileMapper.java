package cams.mapper;

import cams.dto.CandidateCourseResponse;
import cams.dto.CandidateEducationResponse;
import cams.dto.CandidateExperienceResponse;
import cams.dto.CandidateProfileResponse;
import cams.entity.CandidateCourse;
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
                                                List<CandidateEducation> educations,
                                                List<CandidateCourse> courses) {
        return CandidateProfileResponse.builder()
                .id(entity.getId())
                .headline(entity.getHeadline())
                .description(entity.getDescription())
                .location(entity.getLocation())
                .phoneNumber(entity.getPhoneNumber())
                .pictureUrl(entity.getPictureUrl())
                .openToRemote(entity.getOpenToRemote())
                .flexibleHours(entity.getFlexibleHours())
                .desiredWorkingHours(entity.getDesiredWorkingHours())
                .skills(skills.stream().map(skillMapper::toResponse).toList())
                .experience(experiences.stream().map(this::toResponse).toList())
                .education(educations.stream().map(this::toResponse).toList())
                .courses(courses.stream().map(this::toResponse).toList())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public CandidateExperienceResponse toResponse(CandidateExperience entity) {
        return CandidateExperienceResponse.builder()
                .id(entity.getId())
                .companyName(entity.getCompanyName())
                .roleTitle(entity.getRoleTitle())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .description(entity.getDescription())
                .build();
    }

    public CandidateEducationResponse toResponse(CandidateEducation entity) {
        return CandidateEducationResponse.builder()
                .id(entity.getId())
                .institutionName(entity.getInstitutionName())
                .specialty(entity.getSpecialty())
                .level(entity.getLevel())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .build();
    }

    public CandidateCourseResponse toResponse(CandidateCourse entity) {
        return CandidateCourseResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .issuingOrganization(entity.getIssuingOrganization())
                .completionDate(entity.getCompletionDate())
                .credentialUrl(entity.getCredentialUrl())
                .build();
    }
}
