"""
M5 - hybrid_relevance_weighted: M4 with the two ideas that answer the audit.

Same shape and same weights as M4, with two substitutions:

1. RELEVANCE-WEIGHTED TENURE (fixes weakness 2 - irrelevant experience).
   total_years -> relevant_years, where each entry's years are scaled by how
   semantically close that entry is to the listing:

       w_i            = rescale(cosine(embed(exp_i), embed(title + description)))
       relevant_years = sum of w_i * years_i

   Five years of electrical engineering contribute ~0 effective years to a
   software listing; three years of backend work contribute ~3. Embeddings
   decide relevance, arithmetic does the counting, and every intermediate
   number is inspectable in the explanation dict - which matters at a defence
   far more than it matters to the metric.

2. SEMANTIC SKILL BACKFILL (fixes weakness 3 without losing the ESCO-gap
   strength). Structural coverage handles what is countable; each UNCOVERED
   required skill then earns partial credit if the candidate's free text
   supports it:

       credit_r = BACKFILL_CAP * rescale(max cosine(embed(r), embed(evidence)))

   Evidence texts are the headline, the description, and each experience
   description. Credit is capped below a real structured match (0.7) because
   inferred evidence is weaker evidence. Extra unrelated skills still cost
   nothing, because coverage is still recall over the requirement.
"""

from components import (
    describe_experience,
    normalise_skill,
    normalise_skills,
    rescale,
    seniority_score,
    years_between,
)
from scorers.m4_hybrid_structural import check_gates, semantic_role_texts


def listing_domain_text(listing: dict) -> str:
    return ". ".join(p for p in [listing.get("title"), listing.get("description")] if p and p.strip())


def relevant_years(candidate: dict, listing: dict, ctx) -> tuple[float, list]:
    """
    Returns (relevant_years, per-entry trace). The trace is what makes the
    cross-domain-tenure diagnostic readable in the results.
    """
    domain_text = listing_domain_text(listing)
    trace = []
    total = 0.0

    for experience in candidate.get("experiences", []):
        years = years_between(experience.get("startDate"), experience.get("endDate"), ctx.today)
        relevance = ctx.similarity(describe_experience(experience), domain_text)
        weight = rescale(relevance, ctx.tau)
        total += weight * years
        trace.append({
            "role": experience.get("roleTitle"),
            "years": years,
            "relevance": relevance,
            "weight": weight,
        })

    return total, trace


def backfilled_coverage(candidate: dict, listing: dict, ctx) -> tuple[float, float, list]:
    """
    Returns (coverage, structural_coverage, backfill trace).

    Structural coverage first, then semantic credit for what it missed. The
    final figure is capped at 1.0 so backfill can never manufacture more
    coverage than the listing actually requires.
    """
    candidate_skills = normalise_skills(candidate.get("skills"))
    required = normalise_skills(listing.get("skills"))
    if not required:
        return 1.0, 1.0, []

    covered = candidate_skills & required
    structural = len(covered) / len(required)

    evidence_texts = [candidate.get("headline"), candidate.get("description")]
    evidence_texts += [e.get("description") for e in candidate.get("experiences", [])]
    evidence_texts = [t for t in evidence_texts if t and t.strip()]

    # Original-cased names, so what gets embedded is the real skill label
    # rather than its lowercased comparison key.
    uncovered = [name for name in listing.get("skills", []) if normalise_skill(name) not in covered]

    credit_total = 0.0
    trace = []
    for skill_name in uncovered:
        best = max((ctx.similarity(skill_name, evidence) for evidence in evidence_texts), default=0.0)
        credit = ctx.backfill_cap * rescale(best, ctx.tau)
        credit_total += credit
        trace.append({"skill": skill_name, "best_cos": best, "credit": credit})

    coverage = min(1.0, (len(covered) + credit_total) / len(required))
    return coverage, structural, trace


def score_with_weights(candidate: dict, listing: dict, ctx, weights: dict) -> tuple[float, dict]:
    """Shared with M6, which passes a level-dependent weight triple."""
    blocked = check_gates(candidate, listing)
    if blocked is not None:
        return 0.0, blocked

    coverage, structural, backfill_trace = backfilled_coverage(candidate, listing, ctx)

    candidate_text, listing_text = semantic_role_texts(candidate, listing)
    semantic_role = ctx.similarity(candidate_text, listing_text)
    semantic = rescale(semantic_role, ctx.tau)

    years, years_trace = relevant_years(candidate, listing, ctx)
    seniority = seniority_score(years, listing.get("level"))

    total = (
        weights["skills"] * coverage
        + weights["semantic"] * semantic
        + weights["seniority"] * seniority
    )

    explanation = {
        "coverage": coverage,
        "structural_coverage": structural,
        "backfill_credit": coverage - structural if coverage >= structural else 0.0,
        "semantic_cos": semantic_role,
        "semantic": semantic,
        "relevant_years": years,
        "seniority": seniority,
        "w_skills": weights["skills"],
        "w_semantic": weights["semantic"],
        "w_seniority": weights["seniority"],
        "years_trace": years_trace,
        "backfill_trace": backfill_trace,
    }
    return total, explanation


def score(candidate: dict, listing: dict, ctx) -> tuple[float, dict]:
    return score_with_weights(candidate, listing, ctx, ctx.weights)
