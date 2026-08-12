CREATE TYPE employer_request_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED'
);

CREATE TABLE employer_requests (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    company_name            VARCHAR(255) NOT NULL,
    company_description     TEXT,
    company_website         VARCHAR(500),
    company_location        VARCHAR(255),
    status                  employer_request_status NOT NULL DEFAULT 'PENDING',
    requested_at            TIMESTAMPTZ NOT NULL,
    reviewed_at             TIMESTAMPTZ,
    reviewed_by_id          BIGINT,
    rejection_reason        VARCHAR(1000),

    CONSTRAINT fk_employer_requests_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_employer_requests_reviewed_by
        FOREIGN KEY (reviewed_by_id) REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE UNIQUE INDEX idx_employer_requests_one_pending_per_user
    ON employer_requests(user_id)
    WHERE status = 'PENDING';
