"""
The algorithm ladder. Every scorer exposes the identical signature:

    score(candidate: dict, listing: dict, ctx: Context) -> tuple[float, dict]

returning a score in [0, 1] and a mandatory explanation dict of named
sub-scores. The explanation is not optional decoration: it is what makes
results/rankings.csv and the diagnostics narrative possible, and it is how a
given ranking is explained after the fact.

REGISTRY order is the ladder order (M0 -> M6) and is the order results are
reported in. Each rung adds exactly one mechanism over the previous one, so
any metric gain is attributable to that mechanism rather than to an
undifferentiated "the hybrid works better".
"""

from scorers import (
    m0_rule_based,
    m1_embedding_fused,
    m2_embedding_fused_level,
    m3_embedding_sections,
    m4_hybrid_structural,
    m5_hybrid_relevance_weighted,
    m6_hybrid_level_adaptive,
)

REGISTRY = {
    "M0_rule_based_v1": m0_rule_based.score,
    "M1_embedding_fused": m1_embedding_fused.score,
    "M2_embedding_fused_level": m2_embedding_fused_level.score,
    "M3_embedding_sections": m3_embedding_sections.score,
    "M4_hybrid_structural": m4_hybrid_structural.score,
    "M5_hybrid_relevance_weighted": m5_hybrid_relevance_weighted.score,
    "M6_hybrid_level_adaptive": m6_hybrid_level_adaptive.score,
}

# Which rungs each swept constant actually reaches - used by sensitivity.py so
# it does not waste embedding calls re-running rungs a constant cannot touch.
TAU_SENSITIVE = ["M4_hybrid_structural", "M5_hybrid_relevance_weighted", "M6_hybrid_level_adaptive"]
WEIGHT_SENSITIVE = ["M4_hybrid_structural", "M5_hybrid_relevance_weighted"]
