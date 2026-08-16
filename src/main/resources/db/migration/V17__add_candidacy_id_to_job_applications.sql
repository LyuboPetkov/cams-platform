-- Nullable back-reference. Freeform rows from Phases 1-8 keep candidacy_id NULL
-- and need no backfill; only job-board-sourced rows created by the apply flow
-- carry a value. NO ACTION on delete, consistent with every other FK here.
ALTER TABLE job_applications
    ADD COLUMN candidacy_id BIGINT,
    ADD CONSTRAINT fk_job_applications_candidacy
        FOREIGN KEY (candidacy_id) REFERENCES candidacies(id);
