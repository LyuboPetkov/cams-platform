package cams.service;

import cams.dto.CompanyResponse;
import cams.dto.CompanyUpdateRequest;
import cams.dto.PageResponse;
import cams.entity.Company;
import cams.entity.EmployerRequest;
import cams.entity.EmployerRequestStatus;
import cams.entity.User;
import cams.exception.CompanyNameAlreadyExistsException;
import cams.exception.ResourceNotFoundException;
import cams.mapper.CompanyMapper;
import cams.repository.CompanyRepository;
import cams.repository.EmployerRequestRepository;
import cams.repository.UserRepository;
import cams.util.FileKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CompanyService {

    // Same clamp as JobListingService.browseListings() - an unbounded size
    // would turn ?size=100000 into a full-table dump.
    private static final int MAX_PAGE_SIZE = 50;

    private final CompanyRepository companyRepository;
    private final EmployerRequestRepository employerRequestRepository;
    private final UserRepository userRepository;
    private final CompanyMapper mapper;
    private final StorageService storageService;
    private final FileKeys fileKeys;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Shared by AuthService and EmployerRequestService so company-name
    // normalisation is consistent everywhere it's checked or stored.
    public String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    public void ensureNameAvailable(String normalizedName) {
        if (companyRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new CompanyNameAlreadyExistsException("A company named '" + normalizedName + "' already exists");
        }
    }

    // Submission-time check: also rejects a name colliding with another PENDING
    // request. Not used at approval time, where the request would reject itself.
    public void ensureNameAvailableForNewRequest(String normalizedName) {
        ensureNameAvailable(normalizedName);

        if (employerRequestRepository.existsByCompanyNameIgnoreCaseAndStatus(
                normalizedName, EmployerRequestStatus.PENDING)) {
            throw new CompanyNameAlreadyExistsException(
                    "A company named '" + normalizedName + "' already has a pending request");
        }
    }

    public Company createFromEmployerRequest(EmployerRequest request) {
        String normalisedName = normalizeName(request.getCompanyName());
        ensureNameAvailable(normalisedName);

        Company company = Company.builder()
                .name(normalisedName)
                .description(request.getCompanyDescription())
                .website(request.getCompanyWebsite())
                .location(request.getCompanyLocation())
                .build();

        return companyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> listAll(String query, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        String safeQuery = (query == null || query.isBlank()) ? null : query.trim();

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.asc("name")));
        Page<Company> companies = safeQuery == null
                ? companyRepository.findAll(pageRequest)
                : companyRepository.findByNameContainingIgnoreCase(safeQuery, pageRequest);

        return PageResponse.of(companies, companies.getContent().stream().map(mapper::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        return mapper.toResponse(company);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getMyCompany() {
        Company company = getAuthenticatedUser().getCompany();

        if (company == null) {
            throw new ResourceNotFoundException("No company found for this user");
        }

        return mapper.toResponse(company);
    }

    // Separate from updateMyCompany() only because the input is multipart, not
    // JSON, and it fails through StorageService's own checks. See uploadMyPicture().
    @Transactional
    public CompanyResponse uploadMyLogo(MultipartFile file) {
        Company company = getAuthenticatedUser().getCompany();

        if (company == null) {
            throw new ResourceNotFoundException("No company found for this user");
        }

        String key = "company-logos/%d/%s".formatted(company.getId(), fileKeys.uniqueName(file));
        String url = storageService.upload(file, key);

        company.setLogoUrl(url);
        Company saved = companyRepository.save(company);
        return mapper.toResponse(saved);
    }

    @Transactional
    public CompanyResponse updateMyCompany(CompanyUpdateRequest request) {
        Company company = getAuthenticatedUser().getCompany();

        if (company == null) {
            throw new ResourceNotFoundException("No company found for this user");
        }

        if (request.getDescription() != null) {
            company.setDescription(request.getDescription());
        }
        if (request.getWebsite() != null) {
            company.setWebsite(request.getWebsite());
        }
        if (request.getLocation() != null) {
            company.setLocation(request.getLocation());
        }
        if (request.getLogoUrl() != null) {
            company.setLogoUrl(request.getLogoUrl());
        }

        Company saved = companyRepository.save(company);
        return mapper.toResponse(saved);
    }

}
