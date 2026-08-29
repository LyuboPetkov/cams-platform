-- Hand-curated supplement to the skills table: names ESCO v1.2.1 does not model
-- well (modern dev/cloud tooling, some soft skills, Product Management). Kept
-- separate from the ESCO-derived V30, checked case-insensitively against it. 24 rows.

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
    ('SQLite', NOW(), NOW()),
    ('Product Management', NOW(), NOW()),
    ('Spring', NOW(), NOW()),
    ('Spring Boot', NOW(), NOW()),
    ('REST APIs', NOW(), NOW()),
    ('JWT', NOW(), NOW()),
    ('CI/CD', NOW(), NOW()),
    ('Git', NOW(), NOW()),
    ('Jira', NOW(), NOW()),
    ('Communication', NOW(), NOW()),
    ('Teamwork', NOW(), NOW()),
    ('Problem Solving', NOW(), NOW()),
    ('Time Management', NOW(), NOW()),
    ('Leadership', NOW(), NOW()),
    ('Adaptability', NOW(), NOW());
