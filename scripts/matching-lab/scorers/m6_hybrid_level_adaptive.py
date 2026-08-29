"""
M6 - hybrid_level_adaptive: M5 with a weight profile chosen by listing level.

The only difference from M5 is which weight triple goes in. The premise being
tested is that seniority fit should not carry the same weight everywhere: a
JUNIOR listing is mostly a domain-fit question, while a SENIOR listing is
substantially a tenure question.

    INTERN   skills 0.30  semantic 0.55  seniority 0.15
    JUNIOR   skills 0.35  semantic 0.45  seniority 0.20
    MID      skills 0.35  semantic 0.35  seniority 0.30
    SENIOR   skills 0.30  semantic 0.30  seniority 0.40

A listing with no level falls back to ctx.weights (the flat M4/M5 triple),
since there is no band to adapt to.

Because M6 differs from M5 only in the weights, an M5 -> M6 delta is a clean
measurement of whether level-adaptive weighting is worth its extra complexity
at all. If the delta is ~0, that is a publishable finding: it says keep the
simpler rung.
"""

from scorers.m5_hybrid_relevance_weighted import score_with_weights


def score(candidate: dict, listing: dict, ctx) -> tuple[float, dict]:
    level = listing.get("level")
    weights = ctx.level_weights.get(level, ctx.weights)
    total, explanation = score_with_weights(candidate, listing, ctx, weights)
    if "excluded" not in explanation:
        explanation["level_profile"] = level or "default"
    return total, explanation
