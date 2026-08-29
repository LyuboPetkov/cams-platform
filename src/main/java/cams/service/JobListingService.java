package cams.service;

import cams.dto.JobListingBrowseResponse;
import cams.dto.JobListingCreateRequest;
import cams.dto.JobListingResponse;
import cams.dto.JobListingUpdateRequest;
import cams.dto.PageResponse;
import cams.entity.Candidacy;
import cams.entity.CandidacyStatus;
import cams.entity.Company;
import cams.entity.JobLevel;
import cams.entity.JobListing;
import cams.entity.JobListingSkill;
import cams.entity.JobListingStatus;
import cams.entity.Role;
import cams.entity.Skill;
import cams.entity.User;
import cams.event.JobListingChangedEvent;
import cams.exception.JobListingAlreadyArchivedException;
import cams.exception.ResourceNotFoundException;
import cams.exception.UnauthorizedAccessException;
import cams.mapper.JobListingMapper;
import cams.repository.CandidacyRepository;
import cams.repository.JobListingRepository;
import cams.repository.JobListingSkillRepository;
import cams.repository.SkillRepository;
import cams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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

    // Hard ceiling on ?size - see browseListings().
    private static final int MAX_PAGE_SIZE = 50;

    private final JobListingRepository jobListingRepository;
    private final JobListingSkillRepository jobListingSkillRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final CandidacyRepository candidacyRepository;
    private final JobListingMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // An EMPLOYER account with no company row shouldn't exist after the
    // employer approval flow, but leftovers from earlier testing still can.
    private Company getMyCompany() {
        Company company = getAuthenticatedUser().getCompany();

        if (company == null) {
            throw new ResourceNotFoundException("No company found for this user");
        }

        return company;
    }

    // Company-scoped, not poster-scoped: any employer at the owning company may
    // read, edit and archive. Public so CandidacyService can resolve-and-authorize
    // a listing on the employer's behalf.
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

        eventPublisher.publishEvent(new JobListingChangedEvent(saved.getId()));
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
        eventPublisher.publishEvent(new JobListingChangedEvent(saved.getId()));
        return mapper.toResponse(saved, getSkillsOf(saved));
    }

    @Transactional
    public JobListingResponse setListingSkills(Long id, List<Long> skillIds) {
        JobListing listing = getListingOwnedByCompany(id);

        reconcileSkills(listing, skillIds);

        eventPublisher.publishEvent(new JobListingChangedEvent(listing.getId()));
        return mapper.toResponse(listing, getSkillsOf(listing));
    }

    // Candidate-facing browse. Deliberately the naive version: filters ANDed,
    // skills ANY-of, no ranking by fit - it's the comparison baseline.
    @Transactional(readOnly = true)
    public PageResponse<JobListingBrowseResponse> browseListings(String location,
                                                                 Boolean remote,
                                                                 JobLevel level,
                                                                 Long companyId,
                                                                 List<Long> skillIds,
                                                                 int page,
                                                                 int size) {
        // Clamped rather than rejected: PageRequest.of(-1, 20) throws, and an
        // unbounded size turns ?size=100000 into a full-table dump.
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        String safeLocation = (location == null || location.isBlank()) ? null : location.trim();

        // With no skill filter, the flag short-circuits the EXISTS before the IN
        // is reached; the sentinel list only keeps the query from getting an empty IN.
        boolean skillFilterOff = skillIds == null || skillIds.isEmpty();
        List<Long> safeSkillIds = skillFilterOff
                ? List.of(-1L)
                : List.copyOf(new LinkedHashSet<>(skillIds));

        // Fixed sort. The id tiebreaker keeps a total ordering - created_at
        // defaults to NOW(), so same-transaction rows tie and OFFSET paging
        // would otherwise duplicate and skip rows.
        PageRequest pageRequest = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<JobListing> listings = jobListingRepository.browse(
                safeLocation, remote, level, companyId, skillFilterOff, safeSkillIds, pageRequest);

        Map<Long, List<Skill>> skillsByListing = getSkillsOfAll(listings.getContent());
        Map<Long, CandidacyStatus> myCandidacyStatusByListing = getMyCandidacyStatusByListing(listings.getContent());

        List<JobListingBrowseResponse> content = listings.getContent()
                .stream()
                .map(listing -> mapper.toBrowseResponse(
                        listing,
                        skillsByListing.getOrDefault(listing.getId(), List.of()),
                        myCandidacyStatusByListing.get(listing.getId())))
                .toList();

        return PageResponse.of(listings, content);
    }

    // Null for every listing unless the caller is a candidate - "have I applied"
    // is meaningless for other roles. One batched query per page, not per listing.
    private Map<Long, CandidacyStatus> getMyCandidacyStatusByListing(List<JobListing> listings) {
        if (listings.isEmpty()) {
            return Map.of();
        }

        User user = getAuthenticatedUser();
        if (user.getRole() != Role.CANDIDATE) {
            return Map.of();
        }

        List<Long> listingIds = listings.stream().map(JobListing::getId).toList();

        return candidacyRepository.findByCandidateIdAndJobListingIdIn(user.getId(), listingIds)
                .stream()
                .collect(Collectors.toMap(
                        candidacy -> candidacy.getJobListing().getId(),
                        Candidacy::getStatus));
    }

    // One query for the whole page, with the skill fetch-joined - getSkillsOf()
    // above is per-listing and would N+1 over a page if reused here.
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

    // Candidate-facing complement to getListingOwnedByCompany: gated on having a
    // Candidacy against this listing, not company ownership. Unlike browse it
    // does not require status == OPEN, so an applied-to listing stays visible after archiving.
    @Transactional(readOnly = true)
    public JobListingBrowseResponse getListingForCandidate(Long id) {
        User user = getAuthenticatedUser();
        JobListing listing = jobListingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job listing not found"));

        if (!candidacyRepository.existsByCandidateIdAndJobListingId(user.getId(), id)) {
            throw new UnauthorizedAccessException("You do not have access to this job listing");
        }

        List<Skill> skills = getSkillsOf(listing);
        CandidacyStatus myStatus = getMyCandidacyStatusByListing(List.of(listing)).get(id);
        return mapper.toBrowseResponse(listing, skills, myStatus);
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
