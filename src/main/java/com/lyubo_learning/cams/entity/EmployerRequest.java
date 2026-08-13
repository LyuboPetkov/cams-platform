package com.lyubo_learning.cams.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.time.Instant;

@Entity
@Table(name = "employer_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "company_description", columnDefinition = "TEXT")
    private String companyDescription;

    @Column(name = "company_website")
    private String companyWebsite;

    @Column(name = "company_location")
    private String companyLocation;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false)
    private EmployerRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_company_id")
    private Company createdCompany;

}
