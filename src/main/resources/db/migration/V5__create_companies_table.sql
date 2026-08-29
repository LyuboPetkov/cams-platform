CREATE TABLE companies (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    website                 VARCHAR(500),
    location                VARCHAR(255),
    logo_url                VARCHAR(500),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_companies_name_lower ON companies (lower(name));
