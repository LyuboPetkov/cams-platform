CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE candidate_profiles
    ADD COLUMN embedding vector(384),
    ADD COLUMN embedding_updated_at TIMESTAMP;

ALTER TABLE job_listings
    ADD COLUMN embedding vector(384),
    ADD COLUMN embedding_updated_at TIMESTAMP;
