ALTER TABLE candidate_experiences
    ADD COLUMN start_date DATE,
    ADD COLUMN end_date DATE;

-- Best-effort backfill: infer a start date from the self-reported year count,
-- treat existing rows as ongoing (no real date data exists to recover).
UPDATE candidate_experiences
SET start_date = (CURRENT_DATE - (years_of_experience || ' years')::interval)::date,
    end_date = NULL;

ALTER TABLE candidate_experiences
    ALTER COLUMN start_date SET NOT NULL;

ALTER TABLE candidate_experiences
    DROP COLUMN years_of_experience;
