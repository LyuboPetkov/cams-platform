ALTER TABLE candidate_experiences
    ADD COLUMN company_name VARCHAR(255);

-- Existing rows predate the column and carry no employer data to recover.
UPDATE candidate_experiences
SET company_name = 'Not specified'
WHERE company_name IS NULL;

ALTER TABLE candidate_experiences
    ALTER COLUMN company_name SET NOT NULL;
