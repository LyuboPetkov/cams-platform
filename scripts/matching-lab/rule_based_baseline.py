# VERBATIM COPY of scripts/matching-eval/rule_based_baseline.py, copied not
# imported because that folder is gitignored and this harness must run from a
# fresh clone. DO NOT EDIT - its value is continuity with the earlier baseline
# numbers; re-copy if the original changes rather than patching here.

import difflib


def score(candidate: dict, listing: dict) -> float:
    """
    candidate: {"headline": str, "location": str, "open_to_remote": bool,
                "skill_ids": set[int]}
    listing:   {"title": str, "location": str, "remote": bool,
                "skill_ids": set[int]}
    Returns a single 0.0-1.0 weighted score. The weights are a stated design
    choice, not a fitted/validated weighting.
    """
    skills_score = _skills_score(candidate["skill_ids"], listing["skill_ids"])
    title_score = _title_score(candidate.get("headline"), listing.get("title"))
    location_score = _location_score(candidate.get("location"), listing.get("location"))
    remote_score = 1.0 if listing.get("remote") == candidate.get("open_to_remote") else 0.0

    return (
        0.5 * skills_score
        + 0.2 * title_score
        + 0.15 * location_score
        + 0.15 * remote_score
    )


def _skills_score(candidate_skill_ids: set, listing_skill_ids: set) -> float:
    if not listing_skill_ids:
        return 0.0
    return len(candidate_skill_ids & listing_skill_ids) / len(listing_skill_ids)


def _title_score(headline, title) -> float:
    if not headline or not title:
        return 0.0
    return difflib.SequenceMatcher(None, headline.lower().strip(), title.lower().strip()).ratio()


def _location_score(candidate_location, listing_location) -> float:
    if not candidate_location or not listing_location:
        return 0.0
    return 1.0 if candidate_location.lower().strip() == listing_location.lower().strip() else 0.0
