"""
M4 - hybrid_structural: embeddings for semantics, explicit logic for counting.

The first rung that stops asking the embedding to do arithmetic. Skill coverage
is counted, seniority is compared against a band, and education is an ordinal
gate. The embedding is left to decide whether this person's work is in the same
domain as this job, which is what it is good at.

    education gate fails  -> 0.0, {"excluded": "education"}
    location gate fails   -> 0.0, {"excluded": "location"}
    semantic_role = cosine(headline + description + experience text,
                           title + description)
    score = 0.35 * skill_coverage
          + 0.40 * rescale(semantic_role)
          + 0.25 * seniority_score(total_years, level)

Ineligible pairs score 0.0 and stay in the ranking rather than being dropped.
Dropping them would leave each approach ranking a different pool, and the
metrics would stop being comparable between rungs.
"""

from components import (
    describe_experience,
    location_eligible,
    meets_education,
    normalise_skills,
    rescale,
    seniority_score,
    skill_coverage,
    total_years,
)


def semantic_role_texts(candidate: dict, listing: dict) -> tuple[str, str]:
    """
    Candidate side is headline + description + every experience description.
    Listing side is title + description. Experience text is in there because what
    someone actually did is the strongest domain evidence available, and because
    M5 measures its relevance weights against the same pairing.
    """
    candidate_parts = [candidate.get("headline"), candidate.get("description")]
    candidate_parts += [describe_experience(e) for e in candidate.get("experiences", [])]
    candidate_text = ". ".join(p for p in candidate_parts if p and p.strip())
    listing_text = ". ".join(p for p in [listing.get("title"), listing.get("description")] if p and p.strip())
    return candidate_text, listing_text


def check_gates(candidate: dict, listing: dict) -> dict | None:
    """Returns an explanation dict if a gate fails, else None."""
    if not meets_education(candidate.get("educations", []), listing.get("requiredEducationLevel")):
        return {"excluded": "education"}
    if not location_eligible(candidate.get("location"), listing.get("location"), listing.get("remote")):
        return {"excluded": "location"}
    return None


def score(candidate: dict, listing: dict, ctx) -> tuple[float, dict]:
    blocked = check_gates(candidate, listing)
    if blocked is not None:
        return 0.0, blocked

    coverage = skill_coverage(
        normalise_skills(candidate.get("skills")),
        normalise_skills(listing.get("skills")),
    )

    candidate_text, listing_text = semantic_role_texts(candidate, listing)
    semantic_role = ctx.similarity(candidate_text, listing_text)
    semantic = rescale(semantic_role, ctx.tau)

    years = total_years(candidate.get("experiences", []), ctx.today)
    seniority = seniority_score(years, listing.get("level"))

    weights = ctx.weights
    total = (
        weights["skills"] * coverage
        + weights["semantic"] * semantic
        + weights["seniority"] * seniority
    )

    explanation = {
        "coverage": coverage,
        "semantic_cos": semantic_role,
        "semantic": semantic,
        "years": years,
        "seniority": seniority,
    }
    return total, explanation
