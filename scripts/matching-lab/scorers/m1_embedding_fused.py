"""
M1 - embedding_fused: production exactly as it is today.

One fused text per side, one cosine. This rung is the control every other rung
is measured against, so it must be a faithful mirror of
cams.service.EmbeddingService - the text builders live in components.py with
the Java quoted alongside them.

    candidate text = headline. description. Skills: a, b. <experiences>. <educations>. <courses>
    listing text   = title. description. Skills: x, y
    score          = cosineSimilarity(embed(candidate), embed(listing))

The score is clamped at 0 on the low side to honour the scorer contract's
[0, 1] range. MiniLM cosines on this kind of text are effectively never
negative, and clamping cannot change a ranking except among already-tied
zeroes, so the raw value is kept in the explanation dict for inspection.
"""

from components import candidate_embedding_text, listing_embedding_text, location_eligible


def score(candidate: dict, listing: dict, ctx) -> tuple[float, dict]:
    if not location_eligible(candidate.get("location"), listing.get("location"), listing.get("remote")):
        return 0.0, {"excluded": "location"}

    candidate_text = candidate_embedding_text(candidate)
    listing_text = listing_embedding_text(listing)
    raw_cosine = ctx.similarity(candidate_text, listing_text)

    return max(0.0, raw_cosine), {"cosine": raw_cosine}
