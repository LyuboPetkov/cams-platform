CREATE TYPE job_level AS ENUM (
    'INTERN',
    'JUNIOR',
    'MID',
    'SENIOR'
);

CREATE TYPE job_listing_status AS ENUM (
    'OPEN',
    'ARCHIVED'
);

CREATE TABLE job_listings (
    id                      BIGSERIAL PRIMARY KEY,
    company_id              BIGINT NOT NULL,
    posted_by_user_id       BIGINT NOT NULL,
    title                   VARCHAR(255) NOT NULL,
    description             TEXT NOT NULL,
    location                VARCHAR(255),
    remote                  BOOLEAN NOT NULL,
    level                   job_level NOT NULL,
    status                  job_listing_status NOT NULL DEFAULT 'OPEN',
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_job_listings_company
        FOREIGN KEY (company_id) REFERENCES companies(id),

    CONSTRAINT fk_job_listings_posted_by
        FOREIGN KEY (posted_by_user_id) REFERENCES users(id)
);
