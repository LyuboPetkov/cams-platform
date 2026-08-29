CREATE TABLE candidate_profile_skills (
    id                      BIGSERIAL PRIMARY KEY,
    candidate_profile_id    BIGINT NOT NULL,
    skill_id                BIGINT NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_candidate_profile_skills_profile
        FOREIGN KEY (candidate_profile_id) REFERENCES candidate_profiles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_candidate_profile_skills_skill
        FOREIGN KEY (skill_id) REFERENCES skills(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_candidate_profile_skills
        UNIQUE (candidate_profile_id, skill_id)
);
