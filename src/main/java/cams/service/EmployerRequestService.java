package cams.service;

import cams.dto.EmployerRequestResponse;
import cams.entity.Company;
import cams.entity.EmployerRequest;
import cams.entity.EmployerRequestStatus;
import cams.entity.Role;
import cams.entity.User;
import cams.exception.InvalidRequestStateException;
import cams.exception.ResourceNotFoundException;
import cams.mapper.EmployerRequestMapper;
import cams.repository.EmployerRequestRepository;
import cams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployerRequestService {

    private final EmployerRequestRepository employerRequestRepository;
    private final UserRepository userRepository;
    private final EmployerRequestMapper mapper;
    private final CompanyService companyService;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private EmployerRequest getPendingRequest(Long id) {
        EmployerRequest request = employerRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employer request not found"));

        if (request.getStatus() != EmployerRequestStatus.PENDING) {
            throw new InvalidRequestStateException("This request has already been reviewed");
        }

        return request;
    }

    @Transactional(readOnly = true)
    public EmployerRequestResponse getMyRequest() {
        User user = getAuthenticatedUser();
        EmployerRequest request = employerRequestRepository.findFirstByUserIdOrderByRequestedAtDesc(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No employer request found for this user"));

        return mapper.toResponse(request);
    }

    @Transactional(readOnly = true)
    public List<EmployerRequestResponse> listForAdmin(EmployerRequestStatus status) {
        EmployerRequestStatus filter = status != null ? status : EmployerRequestStatus.PENDING;

        return employerRequestRepository.findByStatus(filter)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public EmployerRequestResponse approve(Long id) {
        User admin = getAuthenticatedUser();
        EmployerRequest request = getPendingRequest(id);

        if (request.getUser().getCompany() != null) {
            throw new InvalidRequestStateException("This user is already linked to a company");
        }

        Company company = companyService.createFromEmployerRequest(request);

        User requester = request.getUser();
        requester.setRole(Role.EMPLOYER);
        requester.setCompany(company);
        userRepository.save(requester);

        request.setStatus(EmployerRequestStatus.APPROVED);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(admin);
        request.setCreatedCompany(company);

        EmployerRequest saved = employerRequestRepository.save(request);
        return mapper.toResponse(saved);
    }

    @Transactional
    public EmployerRequestResponse reject(Long id, String rejectionReason) {
        User admin = getAuthenticatedUser();
        EmployerRequest request = getPendingRequest(id);

        request.setStatus(EmployerRequestStatus.REJECTED);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(admin);
        request.setRejectionReason(rejectionReason);

        EmployerRequest saved = employerRequestRepository.save(request);
        return mapper.toResponse(saved);
    }

}
