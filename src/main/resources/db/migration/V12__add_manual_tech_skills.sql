-- Hand-curated supplement to the skills table.
--
-- Source: 2025 Stack Overflow Developer Survey, technology section
--   https://survey.stackoverflow.co/2025/technology  (retrieved August 2026)
--
-- Rationale: ESCO v1.2.1 predates, or simply does not model, most modern
-- framework and tooling names. V10 seeded 10,430 Skills/Transversal-skills
-- concepts and V11 added 365 from the ISCED-F 06 (ICT) branch of the
-- Knowledge hierarchy, which supplied the languages (Java, Python, SQL,
-- JavaScript) but no containerisation, cloud or JS-framework entries.
-- Docker, React, Kubernetes and HTML do not exist in ESCO under any filter.
--
-- Not ESCO-derived. This comment is the provenance record: a source column
-- on skills was considered and rejected, since the migration history already
-- records origin more precisely than a per-row label could.
--
-- 10 rows. Checked case-insensitively against the live table first;
-- skipped as already present (all seeded by V11): CSS, PostgreSQL, MySQL.

INSERT INTO skills (name, created_at, updated_at) VALUES
    ('AWS', NOW(), NOW()),
    ('Docker', NOW(), NOW()),
    ('HTML', NOW(), NOW()),
    ('Kubernetes', NOW(), NOW()),
    ('Next.js', NOW(), NOW()),
    ('Node.js', NOW(), NOW()),
    ('npm', NOW(), NOW()),
    ('React', NOW(), NOW()),
    ('Redis', NOW(), NOW()),
    ('SQLite', NOW(), NOW());
