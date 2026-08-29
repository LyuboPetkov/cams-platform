ALTER TABLE users
    ADD COLUMN company_id BIGINT,
    ADD CONSTRAINT fk_users_company
        FOREIGN KEY (company_id) REFERENCES companies(id);

ALTER TABLE employer_requests
    ADD COLUMN created_company_id BIGINT,
    ADD CONSTRAINT fk_employer_requests_created_company
        FOREIGN KEY (created_company_id) REFERENCES companies(id)
        ON DELETE SET NULL;
