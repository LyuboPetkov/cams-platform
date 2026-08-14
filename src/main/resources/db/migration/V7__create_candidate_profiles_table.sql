CREATE TYPE desired_working_hours AS ENUM (
    'FULL_TIME',
    'PART_TIME',
    'EITHER'
);

CREATE TABLE candidate_profiles (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL UNIQUE,
    headline                VARCHAR(255),
    description             TEXT,
    location                VARCHAR(255),
    open_to_remote          BOOLEAN,
    flexible_hours          BOOLEAN,
    desired_working_hours   desired_working_hours,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_candidate_profiles_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);
