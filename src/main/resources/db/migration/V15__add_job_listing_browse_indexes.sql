-- The one index that actually serves the browse query: it filters to OPEN and
-- supplies the sort order, so the planner can scan and LIMIT with no sort step.
-- A plain index on (status) is deliberately NOT used: two values, most rows OPEN,
-- so it is a low-cardinality B-tree the planner would ignore, and it could not
-- eliminate the sort in any case.
CREATE INDEX idx_job_listings_open_created
    ON job_listings (created_at DESC, id DESC)
    WHERE status = 'OPEN'::job_listing_status;

-- uq_job_listing_skills indexes (job_listing_id, skill_id) — the wrong leading
-- column for "which listings require skill X", which is exactly what the filter asks.
CREATE INDEX idx_job_listing_skills_skill
    ON job_listing_skills (skill_id);

-- PostgreSQL does not auto-index foreign keys (only PK and UNIQUE), so V13 left
-- this column unindexed. Serves GET /mine and Phase 15 rather than this phase's
-- own query; included because it is one line in a migration that is being written
-- anyway, and leaving it means Phase 15 rediscovers it.
CREATE INDEX idx_job_listings_company
    ON job_listings (company_id);
