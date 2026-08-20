package cams.entity;

// The full value set is defined upfront even though only SUBMITTED is reachable
// in Phase 14 — same as ApplicationStatus/ApplicationSource, which were never
// grown incrementally. ACCEPTED and REJECTED become reachable in Phase 15.
public enum CandidacyStatus {
    SUBMITTED,
    ACCEPTED,
    REJECTED
}
