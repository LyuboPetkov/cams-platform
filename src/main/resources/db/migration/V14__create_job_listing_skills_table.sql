CREATE TABLE job_listing_skills (
    id                      BIGSERIAL PRIMARY KEY,
    job_listing_id          BIGINT NOT NULL,
    skill_id                BIGINT NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_job_listing_skills_listing
        FOREIGN KEY (job_listing_id) REFERENCES job_listings(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_job_listing_skills_skill
        FOREIGN KEY (skill_id) REFERENCES skills(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_job_listing_skills
        UNIQUE (job_listing_id, skill_id)
);
