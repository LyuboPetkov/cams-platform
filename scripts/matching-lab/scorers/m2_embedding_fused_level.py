"""
M2 - embedding_fused_level: M1 with seniority text added to the LISTING side.

The one change vs M1: "Level: MID" is appended to the listing's text, and
"Requires: BACHELORS" too when requiredEducationLevel is set. The candidate
side is untouched. Nothing else differs.

This isolates the listing-text seniority fix. If M2 beats M1, the gain is
attributable to the listing text having carried no seniority signal at all
(weakness 1) - not to any scoring change, because there isn't one.

The appended parts join with ". " like every other part, so the resulting
string is what buildEmbeddingText would produce if extraParts were non-empty
on the listing path.
"""

from components import (
    build_embedding_text,
    candidate_embedding_text,
    location_eligible,
)


def listing_text_with_level(listing: dict) -> str:
    extra_parts = []
    if listing.get("level"):
        extra_parts.append(f"Level: {listing['level']}")
    if listing.get("requiredEducationLevel"):
        extra_parts.append(f"Requires: {listing['requiredEducationLevel']}")

    return build_embedding_text(
        listing.get("title"),
        listing.get("description"),
        listing.get("skills", []),
        extra_parts,
    )


def score(candidate: dict, listing: dict, ctx) -> tuple[float, dict]:
    if not location_eligible(candidate.get("location"), listing.get("location"), listing.get("remote")):
        return 0.0, {"excluded": "location"}

    candidate_text = candidate_embedding_text(candidate)
    listing_text = listing_text_with_level(listing)
    raw_cosine = ctx.similarity(candidate_text, listing_text)

    return max(0.0, raw_cosine), {"cosine": raw_cosine}
