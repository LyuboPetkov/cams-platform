-- Nullable back-reference: only job-board-sourced rows carry a value, freeform
-- rows stay NULL. NO ACTION on delete, like every other FK here.
ALTER TABLE job_applications
    ADD COLUMN candidacy_id BIGINT,
    ADD CONSTRAINT fk_job_applications_candidacy
        FOREIGN KEY (candidacy_id) REFERENCES candidacies(id);
