"""
M6 - hybrid_level_adaptive: M5 with a weight profile chosen by listing level.

The only difference from M5 is which weight triple goes in. The idea being
tested is that seniority fit should not carry the same weight everywhere: a
JUNIOR listing is mostly a question of domain fit, while a SENIOR listing is
largely a question of tenure.

    INTERN   skills 0.30  semantic 0.55  seniority 0.15
    JUNIOR   skills 0.35  semantic 0.45  seniority 0.20
    MID      skills 0.35  semantic 0.35  seniority 0.30
    SENIOR   skills 0.30  semantic 0.30  seniority 0.40

A listing with no level falls back to ctx.weights, the flat M4/M5 triple, since
there is no band to adapt to.

Since the weights are the only difference, an M5 to M6 delta measures whether
level-adaptive weighting is worth the extra complexity. A delta near zero is a
useful answer too: it says keep the simpler rung.
"""

from scorers.m5_hybrid_relevance_weighted import score_with_weights


def score(candidate: dict, listing: dict, ctx) -> tuple[float, dict]:
    level = listing.get("level")
    weights = ctx.level_weights.get(level, ctx.weights)
    total, explanation = score_with_weights(candidate, listing, ctx, weights)
    if "excluded" not in explanation:
        explanation["level_profile"] = level or "default"
    return total, explanation
