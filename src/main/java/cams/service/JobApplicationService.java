package cams.service;

import cams.dto.JobApplicationRequest;
import cams.dto.JobApplicationResponse;
import cams.entity.ApplicationSource;
import cams.entity.ApplicationStatus;
import cams.entity.Candidacy;
import cams.entity.JobApplication;
import cams.entity.User;
import cams.exception.JobApplicationLinkedToCandidacyException;
import cams.exception.ResourceNotFoundException;
import cams.exception.UnauthorizedAccessException;
import cams.mapper.JobApplicationMapper;
import cams.repository.JobApplicationRepository;
import cams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.print.attribute.standard.JobName;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    private final JobApplicationMapper mapper;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private JobApplication getApplicationOwnedByUser(Long applicationId, Long userId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You do not have access to this application");
        }

        return application;
    }

    public JobApplicationResponse create(JobApplicationRequest request) {
        User user = getAuthenticatedUser();
        JobApplication application = mapper.toEntity(request, user);
        JobApplication saved = jobApplicationRepository.save(application);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public JobApplicationResponse getById(Long id) {
        User user = getAuthenticatedUser();
        JobApplication application = getApplicationOwnedByUser(id, user.getId());
        return mapper.toResponse(application);
    }

    public List<JobApplicationResponse> getAllForUser() {
        User user = getAuthenticatedUser();
        return jobApplicationRepository.findByUserId(user.getId())
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    // Called only from CandidacyService.apply(), inside its transaction, so a
    // failure rolls this row back with the candidacy.
    public JobApplication createFromCandidacy(Candidacy candidacy) {
        JobApplication application = JobApplication.builder()
                .user(candidacy.getCandidate())
                .companyName(candidacy.getJobListing().getCompany().getName())
                .jobTitle(candidacy.getJobListing().getTitle())
                .status(ApplicationStatus.APPLIED)
                .source(ApplicationSource.JOB_BOARD)
                .appliedDate(candidacy.getAppliedAt().toLocalDate())
                .candidacy(candidacy)
                .build();

        return jobApplicationRepository.save(application);
    }

    // Called only from CandidacyService.transitionStatus(), inside its transaction.
    // Lives here so every JobApplication write still goes through this service.
    public void syncStatusFromCandidacy(Candidacy candidacy, ApplicationStatus newStatus) {
        JobApplication application = jobApplicationRepository.findByCandidacyId(candidacy.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No linked job application found for candidacy " + candidacy.getId()));

        application.setStatus(newStatus);
        jobApplicationRepository.save(application);
    }

    @Transactional
    public JobApplicationResponse update(Long id, JobApplicationRequest request) {
        User user = getAuthenticatedUser();
        JobApplication application = getApplicationOwnedByUser(id, user.getId());

        // On a job-board-sourced row the facts belong to the Candidacy, so only
        // notes are editable; the rest is ignored, not rejected. Freeform rows
        // keep full overwrite.
        if (application.getCandidacy() == null) {
            application.setCompanyName(request.getCompanyName());
            application.setJobTitle(request.getJobTitle());
            application.setStatus(request.getStatus());
            application.setSource(request.getSource());
            application.setAppliedDate(request.getAppliedDate());
            application.setJobUrl(request.getJobUrl());
        }

        application.setNotes(request.getNotes());

        JobApplication saved = jobApplicationRepository.save(application);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        User user = getAuthenticatedUser();
        JobApplication application = getApplicationOwnedByUser(id, user.getId());

        // Deleting this row wouldn't touch the Candidacy, so it would stay live
        // to the employer. No withdraw path exists yet, so refuse.
        if (application.getCandidacy() != null) {
            throw new JobApplicationLinkedToCandidacyException(
                    "This application was created by applying through the job board and cannot be deleted");
        }

        jobApplicationRepository.delete(application);
    }

    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getFiltered(ApplicationStatus status, ApplicationSource source) {
        User user = getAuthenticatedUser();
        Long userId = user.getId();

        List<JobApplication> results;

        if (status != null && source != null) results = jobApplicationRepository.findByUserIdAndStatusAndSource(userId, status, source);
        else if (status != null) results = jobApplicationRepository.findByUserIdAndStatus(userId, status);
        else if (source != null) results = jobApplicationRepository.findByUserIdAndSource(userId, source);
        else results = jobApplicationRepository.findByUserId(userId);

        return results.stream()
                .map(mapper::toResponse)
                .toList();

    }

}
