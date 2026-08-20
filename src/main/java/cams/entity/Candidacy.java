package cams.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// Not a join entity, despite the two-FK shape: a Candidacy carries independent
// state (status, and accept/reject from Phase 15 on) that the real join entities
// here never do. That is also why its FKs are NO ACTION rather than CASCADE.
@Entity
@Table(name = "candidacies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Candidacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Points at User, not CandidateProfile — the same precedent JobListing.postedBy
    // sets: ownership FKs target User, and the profile entities (CandidateProfile,
    // Company) stay description-only.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_listing_id", nullable = false)
    private JobListing jobListing;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false)
    private CandidacyStatus status;

    // Deliberately distinct from createdAt: "when this happened" (business fact)
    // versus "when the row was written" (audit fact), the same split
    // JobApplication.appliedDate already keeps alongside its own createdAt.
    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
