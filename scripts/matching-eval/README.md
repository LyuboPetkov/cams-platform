# CAMS Phase 18 — matching evaluation harness

Compares the embedding-based matcher (Phase 16-17) against a Python
reimplementation of a weighted rule-based baseline, on a curated,
hand-graded dataset, reporting Precision@5 and nDCG@5 for both. See
`docs/CAMS_Phase18_ClaudeCode_Brief.md` for the full design rationale —
this README only covers how to run it.

## Files

- `rule_based_baseline.py` — the baseline scorer. Plain Python, no DB/HTTP,
  importable in isolation.
- `dataset.json` — 18 curated candidates, 18 job listings and 8 companies.
- `seed_dataset.py` — creates all of the above through the real running API
  (never raw SQL), so the async embedding pipeline computes a real embedding
  for each row exactly as it would for any other save.
- `labels.json` — hand-graded relevance labels, 0-2, for a subset of query
  candidates against their *entire* listing pool. Ships with an empty
  `"grades": []` — authoring the actual grades is a separate, manual step
  (see below), not part of this harness.
- `evaluate_matching.py` — connects to Postgres directly, resolves dataset
  keys to real ids, ranks each query candidate's listings both ways, scores
  Precision@5/nDCG@5 against `labels.json`, prints a table and writes
  `results.csv`.

## 1. Start the local stack

```bash
set -a; source .env; set +a
docker compose -f docker-compose.local.yml up --build db embedding-service backend
```

`.env` needs `DB_PASSWORD`, `JWT_SECRET`, `CAMS_ADMIN_EMAIL`, `CAMS_ADMIN_PASSWORD`
— same convention as `scripts/smoke-test-phase17.sh`. An ADMIN account must
already exist (register normally, then `UPDATE users SET role = 'ADMIN' ...`).

`docker-compose.local.yml`'s `db` service now publishes 5432 to the host
(added in this phase) — `evaluate_matching.py` needs to reach Postgres
directly, without the backend running at all.

## 2. Set up the Python environment

```bash
cd scripts/matching-eval
python -m venv .venv
.venv\Scripts\activate       # or: source .venv/bin/activate
pip install -r requirements.txt
```

## 3. Seed the dataset

In a second terminal, with the same `.env` loaded:

```bash
set -a; source .env; set +a
cd scripts/matching-eval
python seed_dataset.py
```

This registers every candidate/employer, approves each employer's company as
admin, sets candidate/listing skills, and then polls `embedding_updated_at`
in Postgres (up to 2 minutes) until every row has a real embedding before
exiting.

**Idempotent by design.** Re-running it against an already-seeded database
does not duplicate rows: account registration falls back to login on a 409,
an already-approved company is detected via `GET /companies/me` and skips
the approval flow, and a listing is skipped if a listing with the same title
already exists in that company's `/mine`. Candidate/company profile fields
are always re-applied via their idempotent `PUT` endpoints regardless.

## 4. Author `labels.json`

This is a manual, human step — not scripted, and not something to invent
generically. Pick a representative handful of the seeded candidates and, for
each one, grade **every** listing in the pool (not just the ones you expect
to match) 0/1/2:

- `0` — not relevant
- `1` — partial match
- `2` — strong match

Grade `backend-java-1` against its full pool in particular: it's the
designated ESCO-gap fixture (brief Section 3.6) — its real Spring Boot
experience only exists in free text (`description`), since `"Spring Boot"`
is not itself a skill in the seeded ESCO vocabulary (only `"Java (computer
programming)"` is). This is the case that should concretely show the
embedding ranker surfacing a genuine semantic match that the rule-based
scorer's skill-recall criterion structurally cannot see.

Any `(candidateKey, listingKey)` pair you don't grade is scored as
relevance `0`, not skipped — see brief Section 3.7.

## 5. Run the evaluation

```bash
python evaluate_matching.py
```

Reads Postgres directly via `psycopg` — the backend does not need to be
running for this step, only `db`. Prints a per-candidate table plus the
overall means, and writes `results.csv` alongside this README.

## Re-seeding from scratch

There is no teardown script. To start over, drop and recreate the `cams_db`
database (or its `eval-*` rows specifically) before re-running
`seed_dataset.py` — the idempotency behavior above is about *safe re-runs*,
not a way to reset fixture content after editing `dataset.json`.
