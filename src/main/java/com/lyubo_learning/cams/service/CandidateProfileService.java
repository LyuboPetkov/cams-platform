package com.lyubo_learning.cams.service;

import com.lyubo_learning.cams.dto.CandidateProfileResponse;
import com.lyubo_learning.cams.dto.CandidateProfileUpdateRequest;
import com.lyubo_learning.cams.entity.CandidateProfile;
import com.lyubo_learning.cams.entity.CandidateProfileSkill;
import com.lyubo_learning.cams.entity.Skill;
import com.lyubo_learning.cams.entity.User;
import com.lyubo_learning.cams.event.CandidateProfileChangedEvent;
import com.lyubo_learning.cams.exception.ResourceNotFoundException;
import com.lyubo_learning.cams.mapper.CandidateProfileMapper;
import com.lyubo_learning.cams.repository.CandidateProfileRepository;
import com.lyubo_learning.cams.repository.CandidateProfileSkillRepository;
import com.lyubo_learning.cams.repository.SkillRepository;
import com.lyubo_learning.cams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateProfileService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateProfileSkillRepository candidateProfileSkillRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final CandidateProfileMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Every registration creates a profile (AuthService.register), so this is an
    // invariant rather than a case to handle — the throw only covers accounts
    // created before candidate profiles existed.
    private CandidateProfile getMyProfileEntity() {
        User user = getAuthenticatedUser();

        return candidateProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No candidate profile found for this user"));
    }

    private List<Skill> getSkillsOf(CandidateProfile profile) {
        return candidateProfileSkillRepository.findByCandidateProfileId(profile.getId())
                .stream()
                .map(CandidateProfileSkill::getSkill)
                .toList();
    }

    @Transactional(readOnly = true)
    public CandidateProfileResponse getMyProfile() {
        CandidateProfile profile = getMyProfileEntity();

        return mapper.toResponse(profile, getSkillsOf(profile));
    }

    @Transactional
    public CandidateProfileResponse updateMyProfile(CandidateProfileUpdateRequest request) {
        CandidateProfile profile = getMyProfileEntity();

        if (request.getHeadline() != null) {
            profile.setHeadline(request.getHeadline());
        }
        if (request.getDescription() != null) {
            profile.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation());
        }
        if (request.getOpenToRemote() != null) {
            profile.setOpenToRemote(request.getOpenToRemote());
        }
        if (request.getFlexibleHours() != null) {
            profile.setFlexibleHours(request.getFlexibleHours());
        }
        if (request.getDesiredWorkingHours() != null) {
            profile.setDesiredWorkingHours(request.getDesiredWorkingHours());
        }

        CandidateProfile saved = candidateProfileRepository.save(profile);
        eventPublisher.publishEvent(new CandidateProfileChangedEvent(saved.getId()));
        return mapper.toResponse(saved, getSkillsOf(saved));
    }

    @Transactional
    public CandidateProfileResponse setMySkills(List<Long> skillIds) {
        CandidateProfile profile = getMyProfileEntity();

        // The same id submitted twice is one selection, not two join rows.
        Set<Long> desired = new LinkedHashSet<>(skillIds);

        List<Skill> found = skillRepository.findAllById(desired);
        if (found.size() != desired.size()) {
            Set<Long> existing = found.stream().map(Skill::getId).collect(Collectors.toSet());
            List<Long> missing = desired.stream().filter(id -> !existing.contains(id)).toList();
            throw new ResourceNotFoundException("Unknown skill ids: " + missing);
        }

        List<CandidateProfileSkill> current = candidateProfileSkillRepository.findByCandidateProfileId(profile.getId());
        Set<Long> currentIds = current.stream().map(link -> link.getSkill().getId()).collect(Collectors.toSet());

        List<CandidateProfileSkill> toRemove = current.stream()
                .filter(link -> !desired.contains(link.getSkill().getId()))
                .toList();
        candidateProfileSkillRepository.deleteAll(toRemove);

        List<CandidateProfileSkill> toAdd = found.stream()
                .filter(skill -> !currentIds.contains(skill.getId()))
                .map(skill -> CandidateProfileSkill.builder()
                        .candidateProfile(profile)
                        .skill(skill)
                        .build())
                .toList();
        candidateProfileSkillRepository.saveAll(toAdd);

        eventPublisher.publishEvent(new CandidateProfileChangedEvent(profile.getId()));
        return mapper.toResponse(profile, getSkillsOf(profile));
    }
}
