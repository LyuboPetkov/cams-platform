# Matching Algorithm Comparison

Run date: 2026-08-29 20:40  
Reference date used for ongoing tenure (`today`): **2026-08-27**  
TAU: 0.35 · BACKFILL_CAP: 0.7 · M4/M5 weights: {'skills': 0.35, 'semantic': 0.4, 'seniority': 0.25}  
Embedding HTTP calls this run: 0 · cache hits: 34742

_Re-running with unchanged fixtures reproduces every result file byte for byte. The run date
and the HTTP-call line above are the only content that varies between runs, and neither is a
measurement - a warm-cache run reports 0 calls precisely because the cache is working._

## Fixture set

- Candidates: 40
- Listings: 18
- Graded pairs in `labels.json`: 720 (complete set would be 720)
- Fixture skill names not found in the seeded vocabulary: 0

> **Ground truth: all 720 (candidate, listing) pairs, graded 0-3 against fixtures/rubric.md, which was written before any grade existed. No algorithm had been run on these fixtures when these grades were authored and no score, ranking or approach output was consulted. 126 pairs are individually authored with a specific reason; the remaining 594 are pairs where the candidate has no evidence of work in the listing's professional area, which rubric 3.1 grades 0 by rule. That bucket is determined by EVIDENCE, never by the `area` tag: dsg-career-changer-1's Marketing pairs and it-cross-domain-1's IT pairs are individually authored precisely because tag and evidence disagree there. it-backend-mid-1-superset's 18 grades are COPIED from it-backend-mid-1 rather than re-authored, so the skill-superset pair's labels cannot drift apart.**

## Reading these numbers

**precision_at_5, recall_at_10 came out IDENTICAL for all seven approaches, and discriminate nothing on this fixture set.** This is a property of the benchmark's shape, not a bug and not a result: with only 18 listings, a top-5 cut is more than a quarter of the pool, and every approach places every grade->=2 listing inside it for every candidate. Recall@10 is worse still - two thirds of the pool. Raising the relevance threshold to 2 fixed the saturation those metrics had at threshold 1, but it could not fix the pool being too small for a top-k cut to separate anything. **Read nDCG@5 and nothing else for the comparison between approaches.** Reporting the identical P@5 figures as though seven approaches 'tied' would be misleading: they did not tie, the metric simply cannot see them. Measuring precision and recall meaningfully would need a listing pool several times larger, which is a fixture change, not an algorithm change.

**Relevance threshold.** Precision@5, Recall@10 and MRR count a pair as relevant at **grade >= 2** ("a genuine shortlist entry"), not at the grade >= 1 the matching-eval harness used. The rubric's floor rule (rubric.md 3.4) puts nearly every same-area pair at 1 or above, so at a threshold of 1 those three metrics collapse into "did the approach get the professional area right", which every approach does, and they saturate. **nDCG@5 is unchanged** - it reads the graded gains directly and never had a threshold - which is why it is the headline metric here: the threshold decision cannot have moved it. The cost is that the matching-eval harness's P@5, Recall and MRR figures are no longer directly comparable; its nDCG figures still are.

**7 of 40 candidates have no pair graded >= 2 anywhere in the fixture set** (fin-audit-mid-1, hr-training-mid-1, it-cross-domain-1, mkt-education-gate-fail-1, ops-logistics-senior-1, ops-procurement-mid-1, ops-warehouse-junior-1). They contribute a constant 0 to P@5, Recall@10 and MRR for *every* approach, which deflates those three absolute numbers without affecting any comparison between approaches. Three of them are diagnostics where that is the intended result; the rest are specialists with no matching listing in an 18-listing set. Their nDCG is unaffected, since a pool of grade-1 pairs still has a non-zero ideal DCG.

**The education gate is deliberately not harmonised.** The rubric caps an unmet education requirement at grade 1 (rubric.md section 4); M4, M5 and M6 return a hard 0 and exclude the pair. This disagreement is intentional and was not reconciled: it is what lets the data show whether a hard structural gate **over-penalises** relative to how a recruiter would actually read an unmet formal requirement. Watch `mkt-education-gate-fail-1` and `ops-logistics-senior-1` in `diagnostics.md` for the effect.

**On the ESCO vocabulary gap.** CAMS's manual skill supplement has partly closed the framework-name gap in the ESCO taxonomy. `Spring Boot`, `React`, `Docker`, `Kubernetes`, `CI/CD` and `DevOps` are all in the seeded vocabulary today - so the `esco-gap` fixture tests **under-claiming a skill that exists**, not vocabulary absence. The `vocabulary-gap` fixture tests the remaining, narrower case: `Scrum` is genuinely absent, so no structured skill could express it and only semantic evidence can reach it. Any claim that the vocabulary cannot express modern framework names should be narrowed accordingly.

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
**The interval straddles zero, so this is INCONCLUSIVE.** M3_embedding_sections has the highest mean, but the benchmark cannot distinguish it from M2_embedding_fused_level, and reporting it as a win would overstate what 40 query candidates can show. See the pairwise table below for which differences, if any, are conclusive.

See `diagnostics.md` for the per-fixture traces, and read them before trusting this line -
a diagnostic that contradicts the aggregate is the more interesting result.

## Pairwise differences (paired bootstrap)

2000 iterations, seed 35, resampling the 40 query candidates with replacement. Every approach is scored on the *same* resample in each iteration, so the shared "this resample was easy" effect cancels in the difference. `diff` is B minus A: positive means B scored higher.

**A difference whose interval contains zero is inconclusive, not a win.**

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

M4, M5 and M6 return a hard 0 and exclude a pair when a gate fails. The rubric does not agree with them about education: it caps an unmet education requirement at grade 1 rather than 0 (rubric.md section 4), and that disagreement was left deliberately unreconciled so the cost could be measured instead of argued about. Counted below on `M4_hybrid_structural` (all three hybrids gate identically).

| Gate | Pairs excluded | Of those, labelled relevant (grade >= 1) | Reading |
|---|---|---|---|
| education | 111 | 8 | **Over-penalises: 8 pair(s) the rubric grades >= 1 are forced to 0** |
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

This is a real and quantified cost of the hard gate, and it is part of why the hybrid rungs score below the embedding rungs here. It is **not** the whole story - see the ablation deltas - and this benchmark deliberately does not re-run the hybrids with a softened gate, because choosing a gate policy after seeing which one scores better is tuning on the test set. Porting a soft education score and re-measuring is a next-phase question with the measurement now in hand.

## Stated limitations

- This is a **synthetic benchmark**. Its labels encode the designer's model of relevance.
  It supports *relative* comparison between approaches on identical data; it is not a claim
  about absolute real-world matching quality.
- Labels are authored **by construction**, from candidate and listing content alone, with no
  algorithm output consulted. `audit_agreement.md` reports how well an independent blind
  grading of a ~10% sample agrees with them - that figure, not this one, is the honest answer
  to "how do you know your labels are right".
- No number here rests on a single arbitrary constant without a check: see `sensitivity.md`
  for the TAU and weight sweep.
- Overlapping experience entries are summed rather than de-overlapped (see `total_years`).
- **The seniority labels are contaminated, not convergent.** The rubric was authored with
  the algorithm design already in hand, so M4+'s asymmetric
  `seniority_score` and its `LEVEL_BANDS` had a direct path into the ground truth. Any
  M3 -> M4 gain is partly the labels agreeing with M4's design rather than evidence that M4
  is better, and must be read that way. M4 -> M5 -> M6 is unaffected: all three share the
  identical seniority component. See rubric.md section 8.2.
- The bootstrap quantifies sampling variability over these 40 candidates only. It says
  nothing about whether the fixture set is representative, and a tight interval around a
  contaminated difference is still a contaminated difference.

