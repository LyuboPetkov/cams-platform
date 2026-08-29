CREATE TYPE application_status AS ENUM (
    'APPLIED',
    'INTERVIEWING',
    'OFFERED',
    'REJECTED',
    'WITHDRAWN'
);

CREATE TYPE application_source AS ENUM (
    'LINKEDIN',
    'COMPANY_WEBSITE',
    'REFERRAL',
    'JOB_BOARD',
    'OTHER'
);

CREATE TABLE job_applications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    company_name    VARCHAR(255) NOT NULL,
    job_title       VARCHAR(255) NOT NULL,
    status          application_status NOT NULL DEFAULT 'APPLIED',
    source          application_source,
    job_url         VARCHAR(500),
    notes           TEXT,
    applied_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_job_applications_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);