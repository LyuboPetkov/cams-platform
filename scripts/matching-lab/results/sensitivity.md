# Sensitivity of the result to TAU and the M4/M5 weights

Written by `sensitivity.py`.

A conclusion that only holds at one value of an arbitrary constant is not worth much.
This file checks that, and says so plainly if the check fails.

Embedding HTTP calls for the whole sweep: 0 (everything reuses the cache).

## Reference: rungs no swept constant reaches

M0-M3 never call `rescale()` and never read the weight triple, so they are a flat line
across both sweeps and are scored once.

| Approach | nDCG@5 | P@5 |
|---|---|---|
| M0_rule_based_v1 | 0.9308 | 0.1650 |
| M1_embedding_fused | 0.9564 | 0.1650 |
| M2_embedding_fused_level | 0.9647 | 0.1650 |
| M3_embedding_sections | 0.9780 | 0.1650 |

## Sweep 1: TAU (0.20 - 0.50)

| TAU | M4_hybrid_structural | M5_hybrid_relevance_weighted | M6_hybrid_level_adaptive | best overall |
|---|---|---|---|---|
| 0.20 | 0.8797 | 0.8639 | 0.8907 | M3_embedding_sections |
| 0.25 | 0.8777 | 0.8562 | 0.8669 | M3_embedding_sections |
| 0.30 | 0.8778 | 0.8511 | 0.8574 | M3_embedding_sections |
| 0.35 | 0.8762 | 0.8443 | 0.8542 | M3_embedding_sections |
| 0.40 | 0.8637 | 0.8292 | 0.8447 | M3_embedding_sections |
| 0.45 | 0.8451 | 0.8195 | 0.8210 | M3_embedding_sections |
| 0.50 | 0.8430 | 0.7986 | 0.7986 | M3_embedding_sections |

**Winner across the TAU sweep:** `M3_embedding_sections` at every value - stable.

- `M4_hybrid_structural` nDCG@5 ranges 0.8430 - 0.8797 (spread 0.0367) across TAU.
- `M5_hybrid_relevance_weighted` nDCG@5 ranges 0.7986 - 0.8639 (spread 0.0653) across TAU.
- `M6_hybrid_level_adaptive` nDCG@5 ranges 0.7986 - 0.8907 (spread 0.0921) across TAU.

## Sweep 2: the M4/M5 weight triple

Coarse grid of 23 triples summing to 1.0, each component held within 0.15-0.45, at TAU = 0.35.

### `M4_hybrid_structural`

- Best triple: skills=0.45, semantic=0.4, seniority=0.15 -> nDCG@5 0.8985
- Worst triple: skills=0.25, semantic=0.3, seniority=0.45 -> nDCG@5 0.8310
- Spread across the grid: 0.0674
- The declared default triple (0.35, 0.4, 0.25) scores 0.8762, +0.0223 from the grid's best.

### `M5_hybrid_relevance_weighted`

- Best triple: skills=0.45, semantic=0.4, seniority=0.15 -> nDCG@5 0.8845
- Worst triple: skills=0.25, semantic=0.3, seniority=0.45 -> nDCG@5 0.8090
- Spread across the grid: 0.0755
- The declared default triple (0.35, 0.4, 0.25) scores 0.8443, +0.0402 from the grid's best.

## How to read this

A small spread means the rung's ranking comes from its structure rather than from the
constants, and the headline result stands on its own. A large spread, or a winner that
changes across the sweep, means the opposite, and then the thing to report is the range
rather than the best cell in it.

The constants were not changed afterwards to match whichever cell scored best. Picking
them from this sweep and rerunning the headline experiment would be tuning on the test
set.

