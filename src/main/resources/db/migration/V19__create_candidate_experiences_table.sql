CREATE TABLE candidate_experiences (
    id                    BIGSERIAL PRIMARY KEY,
    candidate_profile_id  BIGINT NOT NULL,
    role_title            VARCHAR(255) NOT NULL,
    years_of_experience   INTEGER NOT NULL,
    description           TEXT,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_candidate_experiences_profile
        FOREIGN KEY (candidate_profile_id) REFERENCES candidate_profiles(id)
);
