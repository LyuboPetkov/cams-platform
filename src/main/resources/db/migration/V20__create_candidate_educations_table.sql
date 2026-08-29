CREATE TYPE education_level AS ENUM (
    'SECONDARY', 'BACHELORS', 'MASTERS', 'DOCTORATE', 'OTHER'
);

CREATE TABLE candidate_educations (
    id                    BIGSERIAL PRIMARY KEY,
    candidate_profile_id  BIGINT NOT NULL,
    institution_name      VARCHAR(255) NOT NULL,
    level                 education_level NOT NULL,
    start_date            DATE NOT NULL,
    end_date              DATE,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_candidate_educations_profile
        FOREIGN KEY (candidate_profile_id) REFERENCES candidate_profiles(id)
);
