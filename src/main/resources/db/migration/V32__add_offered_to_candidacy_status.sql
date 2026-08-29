-- candidacy_status is a real Postgres enum (V16), so a new value has to be added
-- to the type itself, not just the Java enum, or persisting OFFERED fails at the
-- DB. OFFERED is the post-interview outcome for an already-ACCEPTED candidacy.
ALTER TYPE candidacy_status ADD VALUE 'OFFERED';
