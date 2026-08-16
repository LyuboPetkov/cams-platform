package com.lyubo_learning.cams.service;

import com.lyubo_learning.cams.dto.JobListingBrowseResponse;
import com.lyubo_learning.cams.dto.JobListingCreateRequest;
import com.lyubo_learning.cams.dto.JobListingResponse;
import com.lyubo_learning.cams.dto.JobListingUpdateRequest;
import com.lyubo_learning.cams.dto.PageResponse;
import com.lyubo_learning.cams.entity.Company;
import com.lyubo_learning.cams.entity.JobLevel;
import com.lyubo_learning.cams.entity.JobListing;
import com.lyubo_learning.cams.entity.JobListingSkill;
import com.lyubo_learning.cams.entity.JobListingStatus;
import com.lyubo_learning.cams.entity.Skill;
import com.lyubo_learning.cams.entity.User;
import com.lyubo_learning.cams.exception.JobListingAlreadyArchivedException;
import com.lyubo_learning.cams.exception.ResourceNotFoundException;
import com.lyubo_learning.cams.exception.UnauthorizedAccessException;
import com.lyubo_learning.cams.mapper.JobListingMapper;
import com.lyubo_learning.cams.repository.JobListingRepository;
import com.lyubo_learning.cams.repository.JobListingSkillRepository;
import com.lyubo_learning.cams.repository.SkillRepository;
import com.lyubo_learning.cams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobListingService {

    // Hard ceiling on ?size — see browseListings().
    private static final int MAX_PAGE_SIZE = 50;

    private final JobListingRepository jobListingRepository;
    private final JobListingSkillRepository jobListingSkillRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final JobListingMapper mapper;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // An EMPLOYER account with no company row shouldn't exist after Phase 10's
    // approval flow, but leftovers from earlier testing still can.
    private Company getMyCompany() {
        Company company = getAuthenticatedUser().getCompany();

        if (company == null) {
            throw new ResourceNotFoundException("No company found for this user");
        }

        return company;
    }

    // Access is company-scoped, not poster-scoped: any employer at the owning
    // company may read, edit and archive the listing.
    //
    // Public rather than private so CandidacyService can resolve-and-authorize a
    // listing on the employer's behalf. Resolving the company from
    // SecurityContextHolder stays correct across that call: the invoking request
    // is still the employer's own JWT.
    public JobListing getListingOwnedByCompany(Long id) {
        JobListing listing = jobListingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job listing not found"));

        if (!listing.getCompany().getId().equals(getMyCompany().getId())) {
            throw new UnauthorizedAccessException("You do not have access to this job listing");
        }

        return listing;
    }

    private List<Skill> getSkillsOf(JobListing listing) {
        return jobListingSkillRepository.findByJobListingId(listing.getId())
                .stream()
                .map(JobListingSkill::getSkill)
                .toList();
    }

    // Replaces the listing's skills with the submitted set: the removed subset is
    // deleted and the added subset inserted, so untouched join rows survive.
    private void reconcileSkills(JobListing listing, List<Long> skillIds) {
        // The same id submitted twice is one requirement, not two join rows.
        Set<Long> desired = new LinkedHashSet<>(skillIds);

        List<Skill> found = skillRepository.findAllById(desired);
        if (found.size() != desired.size()) {
            Set<Long> existing = found.stream().map(Skill::getId).collect(Collectors.toSet());
            List<Long> missing = desired.stream().filter(id -> !existing.contains(id)).toList();
            throw new ResourceNotFoundException("Unknown skill ids: " + missing);
        }

        List<JobListingSkill> current = jobListingSkillRepository.findByJobListingId(listing.getId());
        Set<Long> currentIds = current.stream().map(link -> link.getSkill().getId()).collect(Collectors.toSet());

        List<JobListingSkill> toRemove = current.stream()
                .filter(link -> !desired.contains(link.getSkill().getId()))
                .toList();
        jobListingSkillRepository.deleteAll(toRemove);

        List<JobListingSkill> toAdd = found.stream()
                .filter(skill -> !currentIds.contains(skill.getId()))
                .map(skill -> JobListingSkill.builder()
                        .jobListing(listing)
                        .skill(skill)
                        .build())
                .toList();
        jobListingSkillRepository.saveAll(toAdd);
    }

    @Transactional
    public JobListingResponse createListing(JobListingCreateRequest request) {
        User user = getAuthenticatedUser();
        Company company = user.getCompany();

        if (company == null) {
            throw new ResourceNotFoundException("No company found for this user");
        }

        JobListing listing = JobListing.builder()
                .company(company)
                .postedBy(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .remote(request.getRemote())
                .level(request.getLevel())
                .status(JobListingStatus.OPEN)
                .build();

        JobListing saved = jobListingRepository.save(listing);

        // An unknown skill id rolls the whole transaction back, so no orphan
        // listing is left behind.
        if (request.getSkillIds() != null) {
            reconcileSkills(saved, request.getSkillIds());
        }

        return mapper.toResponse(saved, getSkillsOf(saved));
    }

    @Transactional(readOnly = true)
    public List<JobListingResponse> getMyListings() {
        Company company = getMyCompany();

        return jobListingRepository.findByCompanyId(company.getId())
                .stream()
                .map(listing -> mapper.toResponse(listing, getSkillsOf(listing)))
                .toList();
    }

    @Transactional(readOnly = true)
    public JobListingResponse getById(Long id) {
        JobListing listing = getListingOwnedByCompany(id);

        return mapper.toResponse(listing, getSkillsOf(listing));
    }

    @Transactional
    public JobListingResponse updateListing(Long id, JobListingUpdateRequest request) {
        JobListing listing = getListingOwnedByCompany(id);

        if (request.getTitle() != null) {
            listing.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            listing.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            listing.setLocation(request.getLocation());
        }
        if (request.getRemote() != null) {
            listing.setRemote(request.getRemote());
        }
        if (request.getLevel() != null) {
            listing.setLevel(request.getLevel());
        }

        JobListing saved = jobListingRepository.save(listing);
        return mapper.toResponse(saved, getSkillsOf(saved));
    }

    @Transactional
    public JobListingResponse setListingSkills(Long id, List<Long> skillIds) {
        JobListing listing = getListingOwnedByCompany(id);

        reconcileSkills(listing, skillIds);

        return mapper.toResponse(listing, getSkillsOf(listing));
    }

    // The candidate-facing browse. Deliberately the naive, structured version of
    // this query: filters are ANDed, skills are ANY-of, and results are not ranked
    // by fit at all — that comparison baseline is the point.
    @Transactional(readOnly = true)
    public PageResponse<JobListingBrowseResponse> browseListings(String location,
                                                                 Boolean remote,
                                                                 JobLevel level,
                                                                 List<Long> skillIds,
                                                                 int page,
                                                                 int size) {
        // Clamped rather than rejected: PageRequest.of(-1, 20) throws, and an
        // unbounded size turns ?size=100000 into a full-table dump.
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        String safeLocation = (location == null || location.isBlank()) ? null : location.trim();

        // No skill filter submitted: the flag short-circuits the EXISTS before the
        // IN is ever reached, so the sentinel list is never actually compared
        // against anything. It exists only to give the query a non-empty list.
        boolean skillFilterOff = skillIds == null || skillIds.isEmpty();
        List<Long> safeSkillIds = skillFilterOff
                ? List.of(-1L)
                : List.copyOf(new LinkedHashSet<>(skillIds));

        // Fixed sort. The id tiebreaker is not decoration: created_at defaults to
        // NOW(), so listings created in one transaction tie exactly, and without a
        // total ordering OFFSET paging silently duplicates and skips rows.
        PageRequest pageRequest = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<JobListing> listings = jobListingRepository.browse(
                safeLocation, remote, level, skillFilterOff, safeSkillIds, pageRequest);

        Map<Long, List<Skill>> skillsByListing = getSkillsOfAll(listings.getContent());

        List<JobListingBrowseResponse> content = listings.getContent()
                .stream()
                .map(listing -> mapper.toBrowseResponse(
                        listing, skillsByListing.getOrDefault(listing.getId(), List.of())))
                .toList();

        return PageResponse.of(listings, content);
    }

    // One query for the whole page, with the skill fetch-joined. getSkillsOf()
    // above is per-listing and would N+1 over a page; it is left as-is because it
    // serves the Phase 12 employer paths, but browse must not copy it.
    private Map<Long, List<Skill>> getSkillsOfAll(List<JobListing> listings) {
        if (listings.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = listings.stream().map(JobListing::getId).toList();

        return jobListingSkillRepository.findByJobListingIdInFetchingSkill(ids)
                .stream()
                .collect(Collectors.groupingBy(
                        link -> link.getJobListing().getId(),
                        Collectors.mapping(JobListingSkill::getSkill, Collectors.toList())));
    }

    @Transactional
    public JobListingResponse archiveListing(Long id) {
        JobListing listing = getListingOwnedByCompany(id);

        if (listing.getStatus() == JobListingStatus.ARCHIVED) {
            throw new JobListingAlreadyArchivedException("This listing has already been archived");
        }

        listing.setStatus(JobListingStatus.ARCHIVED);

        JobListing saved = jobListingRepository.save(listing);
        return mapper.toResponse(saved, getSkillsOf(saved));
    }

}
