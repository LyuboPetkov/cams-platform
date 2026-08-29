# Matching algorithm lab

Compares seven matching approaches — the rule-based baseline carried over from
`scripts/matching-eval/`, the production embedding matcher as it is today, and
five candidate replacements — on one fixture benchmark with by-construction
relevance labels, reporting Precision@5, nDCG@5, Recall@10 and MRR for each.

**Offline experiment only.** Nothing here changes the runtime behaviour of the
deployed application. No Java, no Flyway migration, no schema change.
`requiredEducationLevel` exists as a fixture JSON field and nowhere else. If a
hybrid wins, porting it is separate work — with the measurement already in hand.

This is a **sibling** of `scripts/matching-eval/`, not a replacement; that
harness is untouched. Unlike it, this folder is **tracked**, following the
`scripts/skill-scope/` precedent: the results are the evaluation record, so
`results/` is committed too. Only `.venv/`, `__pycache__/` and the embedding
cache are gitignored.

## Status

Complete. The benchmark is 40 candidates, 18 listings across 6 professional
areas, all 720 pairs graded against `fixtures/rubric.md`, which was written
before any grade existed. The numbers in `results/` are the record.

Read `results/report.md` first, and read its "Reading these numbers" section
before quoting any figure — three of the four metrics are near-degenerate on a
pool of only 18 listings, and **nDCG@5 is the only metric doing real work.**

## Files

- `components.py` — shared scoring primitives (tenure, seniority bands, the
  education ordinal gate, skill coverage, the location gate, `rescale`), plus
  faithful Python mirrors of `EmbeddingService`'s text builders with the Java
  quoted alongside them. Pure functions; no I/O.
- `embedding_client.py` — HTTP client for `embedding-service` with an on-disk
  cache, and the `cosine` implementation matching `EmbeddingSimilarity.java`.
- `rule_based_baseline.py` — verbatim copy of the `matching-eval` baseline.
  Copied rather than imported because `scripts/matching-eval/` is gitignored and
  this folder is not; see the file header. **Do not edit.**
- `scorers/` — the seven rungs of the ladder, all sharing one signature and
  all returning an explanation dict. `scorers/__init__.py` holds the registry.
- `metrics.py` — P@5, nDCG@5, Recall@10, MRR. Same conventions as
  `matching-eval`, with one deliberate departure: **"relevant" is grade >= 2,
  not >= 1**, because the rubric floor rule puts nearly every same-area pair at 1
  and a threshold of 1 turns the set-based metrics into "did you get the area
  right". nDCG is unchanged and has no threshold. Full reasoning in the module
  docstring.
- `bootstrap.py` — paired bootstrap over the 40 query candidates. Every
  approach is scored on the same resample each iteration, so a pairwise
  difference is measured with the shared per-resample difficulty cancelled
  out. Produces the 95% intervals in `summary.csv` and the verdicts in
  `bootstrap_pairwise.csv`. **A difference whose interval contains zero is
  reported as inconclusive, never as a win.**
- `run_experiments.py` — the main entry point.
- `audit_sample.py` — blind label audit: emits a worksheet, scores agreement.
- `sensitivity.py` — TAU and weight sweep.
- `build_vocabulary.py` — regenerates `fixtures/vocabulary.txt` from the skill
  seed migrations (`V30`, `V31`). Run only when those migrations change.
- `fixtures/` — `candidates.json` (40), `listings.json` (18), `labels.json`
  (all 720 pairs, each with a reason), `vocabulary.txt`, and `rubric.md`, the
  grading rubric, written before any label existed and not revised afterwards.

## The ladder

| ID | Name | What it adds vs. the previous rung |
|----|------|------------------------------------|
| M0 | `rule_based_v1` | The rule-based baseline, unchanged — skills 0.5 / title 0.2 / location 0.15 / remote 0.15 |
| M1 | `embedding_fused` | Production as-is. One fused text each side, one cosine |
| M2 | `embedding_fused_level` | `level` and `requiredEducationLevel` added to the listing text |
| M3 | `embedding_sections` | Per-section embeddings, weighted cosine sum |
| M4 | `hybrid_structural` | Structural skill coverage + seniority band + education gate |
| M5 | `hybrid_relevance_weighted` | Relevance-weighted tenure + semantic skill backfill |
| M6 | `hybrid_level_adaptive` | Weight profile varies by listing seniority |

Each rung adds exactly one mechanism, so any metric gain is attributable to
that mechanism rather than to an undifferentiated "the hybrid works better".

## 1. Start embedding-service

Only `embedding-service` needs to run — **no Postgres, no backend, no API
seeding**, unlike the `matching-eval` harness.

```bash
docker compose -f docker-compose.local.yml up embedding-service
```

It publishes host port **8000**. Override with `EMBEDDING_SERVICE_URL` if
yours differs.

## 2. Set up the Python environment

```bash
cd scripts/matching-lab
python -m venv .venv
.venv\Scripts\activate       # or: source .venv/bin/activate
pip install -r requirements.txt
```

## 3. Run

```bash
python run_experiments.py
python run_experiments.py --today 2026-08-27   # pin the tenure reference date
```

`--today` fixes the date that ongoing experience entries (`endDate: null`) are
measured against, so a run reproduces exactly months later. Whichever date was
used is recorded in `results/report.md`.

Every embedded text is cached to `.embedding_cache.json` by `sha256(text)`, so
a second run with unchanged fixtures makes **zero** HTTP calls and reproduces
the previous run byte for byte. The `/health` probe is lazy for exactly this
reason — it fires only when a cache miss is about to force a real request. The
run prints its HTTP call count so this is verifiable rather than asserted.

Delete `.embedding_cache.json` to force a clean re-fetch.

## 4. Outputs

All under `results/`, all committed:

- `summary.csv` — one row per approach: mean P@5, nDCG@5, Recall@10, MRR,
  plus the paired-bootstrap 95% interval on the two headline metrics.
- `bootstrap_pairwise.csv` — every pairwise approach difference in nDCG@5 and
  P@5 with its 95% CI and a conclusive/inconclusive verdict.
- `per_candidate.csv` — approach × candidate × the four metrics.
- `rankings.csv` — approach × candidate × listing × score × rank × grade,
  plus flattened explanation columns. Nested traces (M5's per-experience
  relevance weights, its per-skill backfill credits) are JSON in one cell so
  the CSV's shape does not depend on the data.
- `diagnostics.md` — one section per diagnostic tag. For `skill-superset`, the
  score delta between the pair under each approach — that number is the direct
  measurement of mean-pooling dilution.
- `report.md` — the assembled write-up: run date, `today`, fixture counts,
  summary table, ablation deltas, and the stated limitations.

Only `report.md` differs between two identical runs, and only in its
wall-clock run-date line.

## 5. Audit the labels

Guards against the circularity objection: the same person designed the
fixtures and graded them, so agreement with an independent blind pass is what
tells you the labels are not just the author's algorithm intuition written
down.

```bash
python audit_sample.py --emit     # results/audit_worksheet.csv, 60 of 720 pairs
# fill in human_grade (0-3) without looking at labels.json or any results
python audit_sample.py --score    # results/audit_agreement.md
```

The worksheet carries candidate and listing text only — no by-construction
grade, no scores, no algorithm output. The sample is drawn with a fixed seed,
recorded in the worksheet and in the report. Agreement is reported as Cohen's
kappa (quadratic weights) and Spearman rho.

**The sample is stratified, not uniform**: 40 same-area and 20 cross-area
pairs, and within the same-area quota it fills seniority-mismatch pairs (18)
and education-capped pairs (10) first. Those are the hard cases — and the
seniority ones are the part of the ground truth contaminated by the algorithm
design (`rubric.md` 8.2), so they are exactly where an artefact would show. A
uniform 10% draw would be ~83% cross-area zeroes and would produce a flattering
number that says nothing. **The agreement figure this produces is therefore
conservative.**

`results/audit_worksheet.csv` and `results/audit_agreement.md` are committed
with the worksheet filled in and scored.

## 6. Sensitivity

```bash
python sensitivity.py
```

Sweeps `TAU` over 0.20–0.50 and the M4/M5 weight triple over a coarse grid,
writing `results/sensitivity.csv` and `results/sensitivity.md`. The point is
to show the conclusion does not rest on one arbitrary constant — and to report
honestly if it does.

**Do not** pick the best-performing constants out of this sweep and rerun the
headline experiment with them. That is tuning on the test set, and it would
make the result indefensible. A negative result is publishable; a tuned one is
not.

## Design notes

- **scipy dropped.** Cohen's kappa is not in scipy, so both agreement
  statistics are implemented in numpy. Full reasoning in `requirements.txt`.
- **`build_vocabulary.py` and `fixtures/vocabulary.txt` added.** The lab warns
  about any fixture skill name not in the seeded vocabulary, but must run
  without Postgres. The vocabulary is therefore reconstructed from the skill
  seed migrations (`V30`, `V31`) into a committed text file.
- **`pairedWith` added to the candidate schema.** Needed so the
  `skill-superset` diagnostic can find its twin and report the score delta.
  Optional; ignored on every other candidate.
- **`bootstrap.py` added**, and `summary.csv` gained CI columns. With 40 query
  candidates, a 0.02 nDCG difference is inside resampling noise, and reporting
  it as a win would be a claim the data does not support.
- **Relevance threshold raised to grade >= 2** for P@5, Recall@10 and MRR. See
  `metrics.py`. The `matching-eval` figures for those three are no longer
  directly comparable; its nDCG figures still are.
- **An 11th diagnostic fixture, `vocabulary-gap`.** It covers the case
  `esco-gap` does not: `Spring Boot` is *in* the seeded vocabulary (the manual
  supplement added it), so `esco-gap` tests under-claiming a skill that exists,
  while `vocabulary-gap` tests a skill the vocabulary genuinely cannot express
  (`Scrum`). To keep the pair count at exactly 720, it replaced a plain filler
  candidate rather than being added alongside.
- **`it-backend-mid` requires a 4th skill, `Agile project management`**, to
  host the `vocabulary-gap` fixture without adding a 19th listing.
- **M0 matches skills by name, not id.** The lab has no DB. Noted in that
  module's docstring.
