# Matching algorithm lab

An offline experiment comparing seven ways of scoring a candidate against a job
listing, measured on a fixture benchmark of 40 candidates and 18 listings with
all 720 pairs graded by hand.

Nothing here runs in the deployed application. It talks to `embedding-service`
and nothing else: no Postgres, no backend, no migration, no schema change. The
`requiredEducationLevel` field exists in the fixture JSON and nowhere else in
the repo. If one of the alternatives had turned out to be worth porting, that
would have been separate work.

The folder is committed with its results, the same way `scripts/skill-scope/` is,
because the results are the record of what was measured. Only `.venv/`,
`__pycache__/` and the embedding cache are ignored.

## Why the experiment exists

The live matcher (`cams.service.EmbeddingService`) builds one block of text per
side and takes one cosine similarity. Three things about that looked weak:

1. The listing text never mentions the seniority level, so a junior and a senior
   version of the same job embed almost identically.
2. Years of experience are not read at all, so five years in an unrelated field
   count the same as five years in the right one.
3. Mean pooling dilutes. Adding unrelated skills to a profile can push it away
   from a listing it already matched.

Each of those has an obvious-looking fix, and picking one by guessing is no
better than leaving it alone. So the alternatives are measured against the
current behaviour on a small benchmark instead.

## How the experiment was run

In this order. Each step was finished before the next one started, which is the
whole point of step 3.

**1. Fixtures.** 40 candidate profiles and 18 job listings were written by hand
into `fixtures/candidates.json` and `fixtures/listings.json`, spread over six
professional areas (IT, Marketing, Sales, Finance & Accounting, HR, Operations,
Design). Each area has a junior, a mid and a senior listing. 11 of the 40
candidates carry a `diagnostic` tag: they are deliberately awkward cases built
to make the approaches disagree (a career changer, an over-qualified applicant,
a profile with almost no prose, and so on). The rest are ordinary profiles that
fill out the pool.

**2. Rubric.** Before any grading, `fixtures/rubric.md` was written: what the
0-3 scale means, which signals apply in what order, the two hard blockers, and
how each diagnostic fixture should come out. Writing the rules first is what
stops the grades from drifting toward whichever algorithm looks most promising.

**3. Labels.** All 720 (candidate, listing) pairs were then graded by hand
against that rubric into `fixtures/labels.json`, working from the profile and
listing text only. 126 pairs are individually graded with a one-line reason
each: those are the pairs where the candidate has real evidence of work in the
listing's area.
The remaining 594 are cross-area pairs, which rubric 3.1 grades 0 by rule, so
they were written out in bulk with the same reason rather than left implicit.
No approach had been run at this point, so there were no scores to look at.

**4. Vocabulary check.** `build_vocabulary.py` rebuilds `fixtures/vocabulary.txt`
from the two skill seed migrations (`V30`, `V31`). The runner warns if a fixture
uses a skill name that is not in the real seeded vocabulary, because such a name
scores zero structural coverage and looks like a result rather than the typo it
usually is.

**5. Scorers.** `scorers/` holds the seven approaches, written as a ladder where
each rung adds exactly one mechanism to the one below it. That way a change in
the score belongs to that mechanism rather than to "the hybrid is better".

**6. The run.** `run_experiments.py` scores every candidate against every
listing under all seven approaches, ranks them, compares the rankings to the
labels, and writes everything under `results/`.

**7. Confidence intervals.** The first run gave differences of around 0.01-0.02
nDCG between neighbouring rungs, which on 40 candidates is not obviously more
than noise. `bootstrap.py` puts a paired bootstrap interval around every
difference so a gap that could be noise is reported as inconclusive.

**8. Label audit.** The obvious objection is that the same person designed the
fixtures and graded them. `audit_sample.py --emit` writes a worksheet of 60 pairs
with the profile and listing text and nothing else: no grades, no scores, no keys
to recognise. The grades were filled in again from scratch, then `--score`
compared that second pass against the original labels and wrote the agreement
figures.

**9. Sensitivity sweep.** `sensitivity.py` re-runs the affected rungs across a
range of TAU values and a grid of weight triples, so the result does not rest on
one arbitrary constant. The constants were not changed afterwards to match
whichever cell scored best.

## The ladder

| ID | Name | What it adds over the rung below |
|----|------|----------------------------------|
| M0 | `rule_based_v1` | Rule-based baseline: skills 0.5, title 0.2, location 0.15, remote 0.15 |
| M1 | `embedding_fused` | Production as it is today. One fused text per side, one cosine |
| M2 | `embedding_fused_level` | `level` and `requiredEducationLevel` added to the listing text |
| M3 | `embedding_sections` | Per-section embeddings, weighted cosine sum |
| M4 | `hybrid_structural` | Counted skill coverage, seniority band, education gate |
| M5 | `hybrid_relevance_weighted` | Relevance-weighted tenure, semantic skill backfill |
| M6 | `hybrid_level_adaptive` | Weight profile varies with the listing's seniority |

## Files

| File | What it is |
|---|---|
| `components.py` | Shared scoring primitives (tenure, seniority bands, education gate, skill coverage, location gate, `rescale`), plus Python mirrors of `EmbeddingService`'s text builders with the Java quoted alongside. Pure functions, no I/O |
| `embedding_client.py` | HTTP client for `embedding-service` with an on-disk cache, and the `cosine` used by `EmbeddingSimilarity.java` |
| `rule_based_baseline.py` | The rule-based baseline's scoring, kept frozen so the bottom rung does not move between runs |
| `scorers/` | The seven approaches, one per file, all sharing one signature. `scorers/__init__.py` holds the registry and the ladder order |
| `metrics.py` | P@5, nDCG@5, Recall@10, MRR |
| `bootstrap.py` | Paired bootstrap over the 40 query candidates |
| `run_experiments.py` | Main entry point |
| `audit_sample.py` | Blind label audit: emits a worksheet, then scores agreement |
| `sensitivity.py` | TAU and weight sweep |
| `build_vocabulary.py` | Rebuilds `fixtures/vocabulary.txt` from the skill seed migrations |
| `fixtures/` | `candidates.json` (40), `listings.json` (18), `labels.json` (720 graded pairs), `rubric.md`, `vocabulary.txt` |

One note on `metrics.py`: Precision@5, Recall@10 and MRR count a pair as
relevant at grade >= 2 rather than >= 1. The rubric's floor rule puts nearly
every same-area pair at 1, so a threshold of 1 turns those three metrics into
"did it get the professional area right", which everything does. nDCG has no
threshold and is unaffected. The module docstring has the reasoning.

## Results, and what writes each one

Everything below lives in `results/` and is committed.

| File | Written by | Contents |
|---|---|---|
| `summary.csv` | `run_experiments.py` | One row per approach: the four mean metrics plus bootstrap intervals on the two headline ones |
| `bootstrap_pairwise.csv` | `run_experiments.py` | Every pairwise difference in nDCG@5 and P@5, with interval and verdict |
| `per_candidate.csv` | `run_experiments.py` | Approach x candidate x the four metrics |
| `rankings.csv` | `run_experiments.py` | Approach x candidate x listing, with score, rank, grade and the sub-scores behind the score |
| `diagnostics.md` | `run_experiments.py` | One section per diagnostic fixture: what it tests, and how each approach ranked the listings in that candidate's area. This is where the approaches visibly disagree, which the aggregate numbers hide |
| `report.md` | `run_experiments.py` | The write-up: run date, fixture counts, summary table, ablation deltas, gate alignment, limitations |
| `audit_worksheet.csv` | `audit_sample.py --emit` | The 60 sampled pairs, with the second-pass grades filled in by hand |
| `audit_agreement.md` | `audit_sample.py --score` | Agreement between those grades and the original labels |
| `sensitivity.csv` / `sensitivity.md` | `sensitivity.py` | The TAU and weight sweeps |

Start with `report.md`, and read its "Reading these numbers" section before
quoting any figure from it. Three of the four metrics are flat across all seven
approaches on a pool of this size; nDCG@5 is the only one doing real work.

## Running it

### 1. Start embedding-service

Only this one service needs to be up.

```bash
docker compose -f docker-compose.local.yml up embedding-service
```

It publishes host port 8000. Set `EMBEDDING_SERVICE_URL` if yours differs.

### 2. Python environment

```bash
cd scripts/matching-lab
python -m venv .venv
.venv\Scripts\activate       # or: source .venv/bin/activate
pip install -r requirements.txt
```

### 3. Run

```bash
python run_experiments.py
python run_experiments.py --today 2026-08-27   # pin the tenure reference date
```

`--today` fixes the date that ongoing experience entries (`endDate: null`) are
measured against, so a run months later gives the same numbers. Whichever date
was used is recorded in `report.md`.

Every embedded text is cached in `.embedding_cache.json` under `sha256(text)`,
so a second run with unchanged fixtures makes no HTTP calls at all and
reproduces the previous result files byte for byte. The `/health` probe is lazy
for that reason: it only fires when a cache miss is about to force a real
request. The run prints its HTTP call count so this can be checked rather than
taken on trust. Delete the cache file to force a clean re-fetch.

### 4. Audit the labels

```bash
python audit_sample.py --emit     # results/audit_worksheet.csv, 60 of 720 pairs
# fill in human_grade (0-3) without looking at labels.json or any results
python audit_sample.py --score    # results/audit_agreement.md
```

The worksheet carries candidate and listing text only. The sample is drawn with
a fixed seed, recorded in the worksheet and in the report, and agreement comes
out as Cohen's kappa (quadratic weights) and Spearman rho.

The sample is stratified rather than uniform: 40 same-area and 20 cross-area
pairs, and within the same-area quota it fills seniority-mismatch pairs (18) and
education-capped pairs (10) first. Those are the hard cases, and the seniority
ones are the part of the ground truth most likely to be contaminated by the
algorithm design (see `rubric.md` 8.2), so they are where a problem would show.
A uniform 10% draw would be about 83% cross-area zeroes and would score well
without proving much.

Both files are committed with the worksheet filled in and scored.

### 5. Sensitivity

```bash
python sensitivity.py
```

Sweeps TAU over 0.20-0.50 and the M4/M5 weight triple over a coarse grid,
writing `results/sensitivity.csv` and `results/sensitivity.md`.

One warning for any later rerun: do not pick the best-performing constants out
of this sweep and rerun the headline experiment with them. That is tuning on the
test set, and it would make the result worthless.

## Notes and deviations

- `scipy` is not used. Cohen's kappa is not in it, and Spearman rho is about ten
  lines of numpy, so both statistics are implemented in `audit_sample.py`.
- `fixtures/vocabulary.txt` exists because the lab has to run without Postgres
  but still wants to check fixture skill names against the real seeded set.
- `pairedWith` on the candidate schema lets the `skill-superset` diagnostic find
  its twin and report the score difference. Ignored on every other candidate.
- The relevance threshold is grade >= 2 for P@5, Recall@10 and MRR. nDCG@5 has
  no threshold, which is part of why it is the headline metric.
- There are two vocabulary-related diagnostics because they test different
  things. `Spring Boot` is in the seeded vocabulary (the manual supplement added
  it), so `esco-gap` tests a candidate under-claiming a skill that exists.
  `Scrum` is genuinely absent, so `vocabulary-gap` tests a skill no structured
  field could express. To keep the pair count at 720 it replaced a plain filler
  candidate rather than being added alongside one.
- `it-backend-mid` requires a fourth skill, `Agile project management`, so that
  the `vocabulary-gap` fixture had somewhere to live without adding a 19th
  listing.
- M0 matches skills by name rather than by id, because the lab has no database.
  Noted again in that module's docstring.
