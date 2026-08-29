"""
M0 - rule_based_v1: the earlier weighted rule-based baseline, unchanged.

skills 0.5 / title 0.2 / location 0.15 / remote 0.15, computed by
rule_based_baseline.py, a verbatim copy of the matching-eval module (see that
file's header for why it is copied rather than imported).

Stated deviation: the original intersected skill *ids* read out of Postgres.
The lab has no DB, so skills are matched by normalised *name* instead -
equivalent for these fixtures, where names are the join key and are validated
against the seeded vocabulary.

The private helpers are called a second time to populate the explanation dict,
reading rule_based_baseline's internals without modifying them.
"""

import rule_based_baseline
from components import location_eligible, normalise_skills


def score(candidate: dict, listing: dict, ctx) -> tuple[float, dict]:
    if not location_eligible(candidate.get("location"), listing.get("location"), listing.get("remote")):
        return 0.0, {"excluded": "location"}

    candidate_skills = normalise_skills(candidate.get("skills"))
    listing_skills = normalise_skills(listing.get("skills"))

    candidate_dict = {
        "headline": candidate.get("headline"),
        "location": candidate.get("location"),
        "open_to_remote": candidate.get("openToRemote"),
        "skill_ids": candidate_skills,
    }
    listing_dict = {
        "title": listing.get("title"),
        "location": listing.get("location"),
        "remote": listing.get("remote"),
        "skill_ids": listing_skills,
    }

    total = rule_based_baseline.score(candidate_dict, listing_dict)

    explanation = {
        "skills": rule_based_baseline._skills_score(candidate_skills, listing_skills),
        "title": rule_based_baseline._title_score(candidate.get("headline"), listing.get("title")),
        "location": rule_based_baseline._location_score(candidate.get("location"), listing.get("location")),
        "remote": 1.0 if listing.get("remote") == candidate.get("openToRemote") else 0.0,
    }
    return total, explanation
