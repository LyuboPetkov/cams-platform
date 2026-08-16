CREATE TYPE candidacy_status AS ENUM (
    'SUBMITTED',
    'ACCEPTED',
    'REJECTED'
);

-- Both FKs are NO ACTION, not ON DELETE CASCADE: a candidacy carries its own
-- state and is a real business record, on the same tier as job_applications and
-- employer_requests — not disposable join-table glue like job_listing_skills.
CREATE TABLE candidacies (
    id                  BIGSERIAL PRIMARY KEY,
    candidate_id        BIGINT NOT NULL,
    job_listing_id      BIGINT NOT NULL,
    status              candidacy_status NOT NULL DEFAULT 'SUBMITTED',
    applied_at          TIMESTAMP NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_candidacies_candidate
        FOREIGN KEY (candidate_id) REFERENCES users(id),

    CONSTRAINT fk_candidacies_job_listing
        FOREIGN KEY (job_listing_id) REFERENCES job_listings(id),

    -- The real guarantee against double-applying; the service check in front of
    -- it only exists to turn the constraint violation into a clean 409.
    CONSTRAINT uq_candidacies_candidate_listing
        UNIQUE (candidate_id, job_listing_id)
);

CREATE INDEX idx_candidacies_candidate ON candidacies (candidate_id);
CREATE INDEX idx_candidacies_job_listing ON candidacies (job_listing_id);
