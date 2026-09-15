"""
M3 - embedding_sections: four separately-embedded sections, weighted cosine sum.

Instead of pooling everything into one vector and losing the structure, each
section is embedded on its own and compared to the listing text that section
should actually be judged against:

    role        0.35   headline + description        vs  title + description
    skills      0.25   "Skills: ..."                 vs  "Skills: ..."
    experience  0.30   joined describeExperience     vs  title + description + "Level: X"
    education   0.10   joined describeEducation      vs  "Requires: X", else title + description
                       + course parts

The weights are renormalised over the sections whose candidate text is
non-empty. Without that, a candidate with no courses and no education would be
capped at 0.90 however good a fit they are, which penalises a thin profile for a
missing section rather than for a bad match. A candidate with only a role
section is scored on role alone, at full scale.

The listing side is never empty in practice, since title and description always
exist, so only the candidate side drives the renormalisation.
"""

from components import (
    describe_course,
    describe_education,
    describe_experience,
    location_eligible,
)

SECTION_WEIGHTS = {"role": 0.35, "skills": 0.25, "experience": 0.30, "education": 0.10}


def _join(parts: list) -> str:
    return ". ".join(p for p in parts if p and p.strip())


def candidate_sections(candidate: dict) -> dict:
    education_parts = [describe_education(e) for e in candidate.get("educations", [])]
    education_parts += [describe_course(c) for c in candidate.get("courses", [])]

    skills = candidate.get("skills", [])
    return {
        "role": _join([candidate.get("headline"), candidate.get("description")]),
        "skills": "Skills: " + ", ".join(skills) if skills else "",
        "experience": _join([describe_experience(e) for e in candidate.get("experiences", [])]),
        "education": _join(education_parts),
    }


def listing_sections(listing: dict) -> dict:
    role_text = _join([listing.get("title"), listing.get("description")])
    skills = listing.get("skills", [])

    experience_text = role_text
    if listing.get("level"):
        experience_text = _join([role_text, f"Level: {listing['level']}"])

    if listing.get("requiredEducationLevel"):
        education_text = f"Requires: {listing['requiredEducationLevel']}"
    else:
        education_text = role_text

    return {
        "role": role_text,
        "skills": "Skills: " + ", ".join(skills) if skills else "",
        "experience": experience_text,
        "education": education_text,
    }


def score(candidate: dict, listing: dict, ctx) -> tuple[float, dict]:
    if not location_eligible(candidate.get("location"), listing.get("location"), listing.get("remote")):
        return 0.0, {"excluded": "location"}

    cand = candidate_sections(candidate)
    lst = listing_sections(listing)

    explanation = {}
    weighted_sum = 0.0
    active_weight = 0.0

    for section, weight in SECTION_WEIGHTS.items():
        candidate_text = cand[section]
        if not candidate_text.strip():
            explanation[f"{section}_cos"] = 0.0
            explanation[f"{section}_active"] = 0
            continue

        section_cosine = ctx.similarity(candidate_text, lst[section])
        explanation[f"{section}_cos"] = section_cosine
        explanation[f"{section}_active"] = 1
        weighted_sum += weight * max(0.0, section_cosine)
        active_weight += weight

    if active_weight == 0.0:
        return 0.0, {**explanation, "active_weight": 0.0}

    explanation["active_weight"] = active_weight
    return weighted_sum / active_weight, explanation
