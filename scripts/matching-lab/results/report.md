# Matching Algorithm Comparison

Run date: 2026-09-15 08:27  
Reference date used for ongoing tenure (`today`): **2026-08-27**  
TAU: 0.35 · BACKFILL_CAP: 0.7 · M4/M5 weights: {'skills': 0.35, 'semantic': 0.4, 'seniority': 0.25}  
Embedding HTTP calls this run: 0 · cache hits: 34742

_Written by `run_experiments.py`. Re-running with unchanged fixtures reproduces every
result file byte for byte; the run date and the HTTP-call line above are the only parts
that vary. Neither is a measurement, and a warm-cache run reports 0 calls because the
cache is doing its job._

## Fixture set

- Candidates: 40
- Listings: 18
- Graded pairs in `labels.json`: 720 (complete set would be 720)
- Fixture skill names not found in the seeded vocabulary: 0

> All 720 (candidate, listing) pairs, graded 0-3 against fixtures/rubric.md. The rubric was written first, and no approach had been run on these fixtures when the grades were authored, so no score or ranking was consulted. 126 pairs are graded individually with a reason of their own. The other 594 are pairs where the candidate has no evidence of work in the listing's professional area, which rubric 3.1 grades 0 by rule. That bucket follows the evidence, not the `area` tag: dsg-career-changer-1's Marketing pairs and it-cross-domain-1's IT pairs are graded individually because tag and evidence disagree there. it-backend-mid-1-superset's 18 grades are copied from it-backend-mid-1 rather than re-authored, so the two halves of the skill-superset pair cannot drift apart.

## Reading these numbers

**precision_at_5, recall_at_10 came out identical for all seven approaches** and separate nothing here. That is the shape of the benchmark rather than a result: with 18 listings a top-5 cut is over a quarter of the pool, and every approach puts every grade->=2 listing inside it for every candidate. Recall@10 is worse, at two thirds of the pool. Raising the relevance threshold to 2 fixed the saturation these metrics had at a threshold of 1, but it could not fix the pool being too small for a top-k cut to separate anything. So compare the approaches on nDCG@5 and nothing else. The matching P@5 figures are not seven approaches tying; the metric simply cannot see the difference between them. Measuring precision and recall properly would need a listing pool several times larger, which is a fixture change rather than an algorithm change.

**Relevance threshold.** Precision@5, Recall@10 and MRR count a pair as relevant at grade >= 2, which rubric.md calls a genuine shortlist entry, rather than at grade >= 1. The floor rule (rubric.md 3.4) puts nearly every same-area pair at 1 or above, so at a threshold of 1 those three metrics turn into "did the approach get the professional area right", which all of them do, and they saturate. nDCG@5 reads the graded gains directly and has no threshold, so this decision cannot have moved it. That is the main reason it is the headline metric here.

**7 of 40 candidates have no pair graded >= 2 anywhere in the fixture set** (fin-audit-mid-1, hr-training-mid-1, it-cross-domain-1, mkt-education-gate-fail-1, ops-logistics-senior-1, ops-procurement-mid-1, ops-warehouse-junior-1). They contribute a constant 0 to P@5, Recall@10 and MRR under every approach, which pulls those three absolute numbers down without changing any comparison between approaches. Three of them are diagnostics where that is the intended result; the rest are specialists with no matching listing in a pool this small. Their nDCG is unaffected, since a pool of grade-1 pairs still has a non-zero ideal DCG.

**The rubric and the hybrids disagree about education, on purpose.** The rubric caps an unmet education requirement at grade 1 (rubric.md section 4), while M4, M5 and M6 return a hard 0 and drop the pair. Leaving that unreconciled is what lets the data show whether a hard structural gate over-penalises compared with how a recruiter would read an unmet formal requirement. `mkt-education-gate-fail-1` and `ops-logistics-senior-1` in `diagnostics.md` show the effect.

**On the ESCO vocabulary gap.** The manual skill supplement has already closed most of the framework-name gap in the ESCO taxonomy. `Spring Boot`, `React`, `Docker`, `Kubernetes`, `CI/CD` and `DevOps` are all in the seeded vocabulary now, so the `esco-gap` fixture tests a candidate under-claiming a skill that exists rather than the vocabulary missing one. `vocabulary-gap` covers the narrower remaining case: `Scrum` really is absent, so no structured skill can express it and only semantic evidence reaches it. A claim that the vocabulary cannot handle modern framework names should be narrowed to that.

## Summary

| Approach | P@5 | nDCG@5 | nDCG@5 95% CI | Recall@10 | MRR |
|---|---|---|---|---|---|
| M0_rule_based_v1 | 0.1650 | 0.9308 | [0.9026, 0.9550] | 0.8250 | 0.8125 |
| M1_embedding_fused | 0.1650 | 0.9564 | [0.9262, 0.9803] | 0.8250 | 0.8000 |
| M2_embedding_fused_level | 0.1650 | 0.9647 | [0.9366, 0.9846] | 0.8250 | 0.8125 |
| M3_embedding_sections | 0.1650 | 0.9780 | [0.9592, 0.9920] | 0.8250 | 0.8125 |
| M4_hybrid_structural | 0.1650 | 0.8762 | [0.8289, 0.9145] | 0.8250 | 0.8063 |
| M5_hybrid_relevance_weighted | 0.1650 | 0.8443 | [0.7779, 0.8962] | 0.8250 | 0.8125 |
| M6_hybrid_level_adaptive | 0.1650 | 0.8542 | [0.7907, 0.9039] | 0.8250 | 0.8125 |

## Ablation deltas

Each rung adds exactly one mechanism, so a delta is attributable to that mechanism.

| Step | nDCG@5 delta | 95% CI of the delta | P@5 delta | Verdict |
|---|---|---|---|---|
| M1_embedding_fused -> M2_embedding_fused_level | +0.0082 | [-0.0026, +0.0219] | +0.0000 | inconclusive |
| M2_embedding_fused_level -> M3_embedding_sections | +0.0134 | [-0.0046, +0.0353] | +0.0000 | inconclusive |
| M3_embedding_sections -> M4_hybrid_structural | -0.1018 | [-0.1468, -0.0668] | +0.0000 | **conclusive** |
| M4_hybrid_structural -> M5_hybrid_relevance_weighted | -0.0318 | [-0.0961, +0.0176] | +0.0000 | inconclusive |
| M5_hybrid_relevance_weighted -> M6_hybrid_level_adaptive | +0.0098 | [+0.0001, +0.0244] | +0.0000 | **conclusive** |
| M0_rule_based_v1 -> M1_embedding_fused | +0.0257 | [-0.0033, +0.0524] | +0.0000 | inconclusive |

## Result

Highest mean nDCG@5: **M3_embedding_sections** (0.9780).
Runner-up: M2_embedding_fused_level (0.9647), a margin of +0.0134.

Paired bootstrap on that margin: 95% CI [-0.0046, +0.0353] over 2000 resamples of the 40 candidates (seed 35).
**The interval straddles zero, so this one is inconclusive.** M3_embedding_sections has the highest mean, but the benchmark cannot tell it apart from M2_embedding_fused_level, and calling it a win would claim more than 40 candidates can support. The pairwise table below shows which differences are conclusive.

`diagnostics.md` has the per-fixture traces. Read those before trusting this line: a
diagnostic that contradicts the aggregate is the more interesting result.

## Pairwise differences (paired bootstrap)

2000 iterations, seed 35, resampling the 40 query candidates with replacement. Every approach is scored on the same resample in each iteration, so the shared "this resample was an easy one" effect cancels out of the difference. `diff` is B minus A, so a positive value means B scored higher.

A difference whose interval contains zero is inconclusive, not a win.

| Metric | A | B | diff (B-A) | 95% CI | Verdict |
|---|---|---|---|---|---|
| ndcg_at_5 | M0_rule_based_v1 | M1_embedding_fused | +0.0257 | [-0.0033, +0.0524] | _inconclusive_ |
| ndcg_at_5 | M0_rule_based_v1 | M2_embedding_fused_level | +0.0339 | [+0.0045, +0.0604] | conclusive |
| ndcg_at_5 | M0_rule_based_v1 | M3_embedding_sections | +0.0473 | [+0.0273, +0.0703] | conclusive |
| ndcg_at_5 | M0_rule_based_v1 | M4_hybrid_structural | -0.0546 | [-0.0946, -0.0249] | conclusive |
| ndcg_at_5 | M0_rule_based_v1 | M5_hybrid_relevance_weighted | -0.0864 | [-0.1513, -0.0362] | conclusive |
| ndcg_at_5 | M0_rule_based_v1 | M6_hybrid_level_adaptive | -0.0766 | [-0.1395, -0.0304] | conclusive |
| ndcg_at_5 | M1_embedding_fused | M2_embedding_fused_level | +0.0082 | [-0.0026, +0.0219] | _inconclusive_ |
| ndcg_at_5 | M1_embedding_fused | M3_embedding_sections | +0.0216 | [+0.0020, +0.0443] | conclusive |
| ndcg_at_5 | M1_embedding_fused | M4_hybrid_structural | -0.0803 | [-0.1290, -0.0373] | conclusive |
| ndcg_at_5 | M1_embedding_fused | M5_hybrid_relevance_weighted | -0.1121 | [-0.1801, -0.0557] | conclusive |
| ndcg_at_5 | M1_embedding_fused | M6_hybrid_level_adaptive | -0.1023 | [-0.1665, -0.0491] | conclusive |
| ndcg_at_5 | M2_embedding_fused_level | M3_embedding_sections | +0.0134 | [-0.0046, +0.0353] | _inconclusive_ |
| ndcg_at_5 | M2_embedding_fused_level | M4_hybrid_structural | -0.0885 | [-0.1374, -0.0460] | conclusive |
| ndcg_at_5 | M2_embedding_fused_level | M5_hybrid_relevance_weighted | -0.1203 | [-0.1866, -0.0667] | conclusive |
| ndcg_at_5 | M2_embedding_fused_level | M6_hybrid_level_adaptive | -0.1105 | [-0.1721, -0.0579] | conclusive |
| ndcg_at_5 | M3_embedding_sections | M4_hybrid_structural | -0.1018 | [-0.1468, -0.0668] | conclusive |
| ndcg_at_5 | M3_embedding_sections | M5_hybrid_relevance_weighted | -0.1337 | [-0.1972, -0.0845] | conclusive |
| ndcg_at_5 | M3_embedding_sections | M6_hybrid_level_adaptive | -0.1238 | [-0.1840, -0.0766] | conclusive |
| ndcg_at_5 | M4_hybrid_structural | M5_hybrid_relevance_weighted | -0.0318 | [-0.0961, +0.0176] | _inconclusive_ |
| ndcg_at_5 | M4_hybrid_structural | M6_hybrid_level_adaptive | -0.0220 | [-0.0852, +0.0258] | _inconclusive_ |
| ndcg_at_5 | M5_hybrid_relevance_weighted | M6_hybrid_level_adaptive | +0.0098 | [+0.0001, +0.0244] | conclusive |
| precision_at_5 | M0_rule_based_v1 | M1_embedding_fused | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M0_rule_based_v1 | M2_embedding_fused_level | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M0_rule_based_v1 | M3_embedding_sections | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M0_rule_based_v1 | M4_hybrid_structural | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M0_rule_based_v1 | M5_hybrid_relevance_weighted | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M0_rule_based_v1 | M6_hybrid_level_adaptive | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M1_embedding_fused | M2_embedding_fused_level | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M1_embedding_fused | M3_embedding_sections | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M1_embedding_fused | M4_hybrid_structural | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M1_embedding_fused | M5_hybrid_relevance_weighted | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M1_embedding_fused | M6_hybrid_level_adaptive | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M2_embedding_fused_level | M3_embedding_sections | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M2_embedding_fused_level | M4_hybrid_structural | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M2_embedding_fused_level | M5_hybrid_relevance_weighted | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M2_embedding_fused_level | M6_hybrid_level_adaptive | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M3_embedding_sections | M4_hybrid_structural | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M3_embedding_sections | M5_hybrid_relevance_weighted | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M3_embedding_sections | M6_hybrid_level_adaptive | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M4_hybrid_structural | M5_hybrid_relevance_weighted | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M4_hybrid_structural | M6_hybrid_level_adaptive | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |
| precision_at_5 | M5_hybrid_relevance_weighted | M6_hybrid_level_adaptive | +0.0000 | [+0.0000, +0.0000] | _inconclusive_ |

16 of 42 pairwise differences are conclusive at 95%.
Full table also in `bootstrap_pairwise.csv`.

## Gate alignment: does a hard gate over-penalise?

M4, M5 and M6 return a hard 0 and drop a pair when a gate fails. The rubric caps an unmet education requirement at grade 1 instead (rubric.md section 4). That disagreement was left in place so the cost of the hard gate could be counted rather than argued about. Counted below on `M4_hybrid_structural`; all three hybrids gate the same way.

| Gate | Pairs excluded | Of those, labelled relevant (grade >= 1) | Reading |
|---|---|---|---|
| education | 111 | 8 | Over-penalises: 8 pair(s) the rubric grades >= 1 are forced to 0 |
| location | 52 | 0 | Agrees with the labels on every pair it excludes |

The education-gated pairs the labels disagree with, each forced from a shortlist-adjacent grade to the bottom of an 18-listing ranking:

| Candidate | Listing | Label |
|---|---|---|
| `dsg-career-changer-1` | `mkt-brand-senior` | 1 |
| `dsg-graphic-mid-1` | `mkt-brand-senior` | 1 |
| `mkt-digital-mid-1` | `mkt-brand-senior` | 1 |
| `mkt-education-gate-fail-1` | `mkt-content-junior` | 1 |
| `ops-coordinator-junior-1` | `ops-supplychain-mid` | 1 |
| `ops-logistics-senior-1` | `ops-manager-senior` | 1 |
| `ops-procurement-mid-1` | `ops-manager-senior` | 1 |
| `ops-supplychain-mid-1` | `ops-manager-senior` | 1 |

That is a measured cost of the hard gate, and part of why the hybrid rungs score below the embedding rungs here. It is not the whole story, as the ablation deltas show. The hybrids are not re-run with a softened gate: picking a gate policy after seeing which one scores better is tuning on the test set. Softening it and measuring again would be a separate piece of work.

## Stated limitations

- This is a **synthetic benchmark**. Its labels encode the designer's model of relevance.
  It supports *relative* comparison between approaches on identical data; it is not a claim
  about absolute real-world matching quality.
- Labels were graded by hand from candidate and listing content alone, with no algorithm
  output consulted. `audit_agreement.md` reports how closely a second blind pass over a
  sample of them agrees with the originals.
- No number here rests on a single arbitrary constant without a check: see `sensitivity.md`
  for the TAU and weight sweep.
- Overlapping experience entries are summed rather than de-overlapped (see `total_years`).
- **The seniority labels are contaminated, not convergent.** The rubric was written with the
  algorithm design already in hand, so M4's asymmetric `seniority_score` and its
  `LEVEL_BANDS` had a direct path into the ground truth. An M3 to M4 gain is partly the
  labels agreeing with M4's design rather than evidence that M4 is better, and should be
  read that way. M4 to M5 to M6 is unaffected, since all three share the same seniority
  component. See rubric.md section 8.2.
- The bootstrap covers sampling variability over these 40 candidates and nothing else. It
  says nothing about whether the fixture set is representative, and a tight interval around
  a contaminated difference is still a contaminated difference.

