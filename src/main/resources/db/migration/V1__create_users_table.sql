CREATE TYPE role AS ENUM (
    'CANDIDATE',
    'EMPLOYER',
    'ADMIN'
);

CREATE TYPE account_status AS ENUM (
    'ACTIVE',
    'PENDING_APPROVAL',
    'REJECTED'
);

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    role        role NOT NULL DEFAULT 'CANDIDATE',
    status      account_status NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
