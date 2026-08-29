-- Serves the browse query: filters to OPEN and supplies the sort order, so the
-- planner scans and LIMITs with no sort step. A plain (status) index would be
-- ignored - two values, most rows OPEN.
CREATE INDEX idx_job_listings_open_created
    ON job_listings (created_at DESC, id DESC)
    WHERE status = 'OPEN'::job_listing_status;

-- uq_job_listing_skills indexes (job_listing_id, skill_id) — the wrong leading
-- column for "which listings require skill X", which is exactly what the filter asks.
CREATE INDEX idx_job_listing_skills_skill
    ON job_listing_skills (skill_id);

-- PostgreSQL does not auto-index foreign keys, so V13 left this unindexed.
-- Serves GET /mine, not this migration's own query - added while we're here.
CREATE INDEX idx_job_listings_company
    ON job_listings (company_id);
