CREATE TABLE candidate_courses (
    id                    BIGSERIAL PRIMARY KEY,
    candidate_profile_id  BIGINT NOT NULL,
    name                  VARCHAR(255) NOT NULL,
    issuing_organization  VARCHAR(255) NOT NULL,
    completion_date       DATE,
    credential_url        VARCHAR(500),
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_candidate_courses_profile
        FOREIGN KEY (candidate_profile_id) REFERENCES candidate_profiles(id)
);
