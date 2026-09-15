"""
Ranking metrics for the lab.

Conventions: gain is 2**g - 1, ideal DCG comes from the full graded pool
truncated to k, and an absent (candidate, listing) pair counts as grade 0 and
stays in the ranking. K = 5 for precision and nDCG, K = 10 for recall.

"Relevant" means grade >= 2 rather than >= 1. The rubric's floor rule
(rubric.md 3.4) puts nearly every same-area pair at 1, so at a threshold of 1
precision, recall and MRR turn into "did the approach get the area right" and
saturate. nDCG reads the graded gains directly and has no threshold, which is
why it is the headline metric.
"""

import math

K_PRECISION = 5
K_NDCG = 5
K_RECALL = 10

# Grade at or above which a pair counts as relevant for the set-based metrics.
# 2 is "a genuine shortlist entry" in rubric.md's scale.
RELEVANT_THRESHOLD = 2


def grade_of(labels_by_pair: dict, candidate_key: str, listing_key: str) -> int:
    # Absent pairs default to 0. The fixtures write cross-area zeroes out
    # explicitly anyway, so this is a safety net, not the mechanism.
    return labels_by_pair.get((candidate_key, listing_key), 0)


def dcg(grades: list) -> float:
    return sum((2 ** g - 1) / math.log2(rank + 1) for rank, g in enumerate(grades, start=1))


def precision_at_k(ranked_keys: list, labels_by_pair: dict, candidate_key: str, k: int = K_PRECISION) -> float:
    """
    Fraction of the top k that is relevant (grade >= RELEVANT_THRESHOLD).
    Divided by k, not by len(top_k), so a pool smaller than k caps below 1.0 -
    fine as long as every approach is capped identically.
    """
    top_k = ranked_keys[:k]
    relevant = sum(1 for key in top_k if grade_of(labels_by_pair, candidate_key, key) >= RELEVANT_THRESHOLD)
    return relevant / k


def ndcg_at_k(ranked_keys: list, labels_by_pair: dict, candidate_key: str,
              all_listing_keys: list, k: int = K_NDCG) -> float:
    top_k_grades = [grade_of(labels_by_pair, candidate_key, key) for key in ranked_keys[:k]]
    actual = dcg(top_k_grades)

    ideal_grades = sorted(
        (grade_of(labels_by_pair, candidate_key, key) for key in all_listing_keys),
        reverse=True,
    )
    ideal = dcg(ideal_grades[:k])

    if ideal == 0:
        return 0.0
    return actual / ideal


def recall_at_k(ranked_keys: list, labels_by_pair: dict, candidate_key: str,
                all_listing_keys: list, k: int = K_RECALL) -> float:
    """
    Of everything relevant in the pool, how much made the top k.

    A candidate with nothing relevant anywhere returns 0.0 rather than 1.0.
    Vacuously "recalling all zero relevant items" would inflate the mean with
    a perfect score for a candidate the approach was never tested on - the
    same reason ndcg returns 0.0 when ideal DCG is 0.
    """
    total_relevant = sum(
        1 for key in all_listing_keys
        if grade_of(labels_by_pair, candidate_key, key) >= RELEVANT_THRESHOLD
    )
    if total_relevant == 0:
        return 0.0

    found = sum(
        1 for key in ranked_keys[:k]
        if grade_of(labels_by_pair, candidate_key, key) >= RELEVANT_THRESHOLD
    )
    return found / total_relevant


def mrr(ranked_keys: list, labels_by_pair: dict, candidate_key: str) -> float:
    """Reciprocal rank of the first relevant listing; 0.0 if there is none."""
    for rank, key in enumerate(ranked_keys, start=1):
        if grade_of(labels_by_pair, candidate_key, key) >= RELEVANT_THRESHOLD:
            return 1.0 / rank
    return 0.0


def rank_listings(scored: list) -> list:
    """
    scored: list of (score, listing_key). Returns listing keys, best first.

    Ties break deterministically on listing key (ascending), so two runs of
    the same approach produce identical rankings.
    """
    return [key for _, key in sorted(scored, key=lambda pair: (-pair[0], pair[1]))]


def evaluate(ranked_keys: list, labels_by_pair: dict, candidate_key: str, all_listing_keys: list) -> dict:
    """All four metrics for one (approach, candidate) pair."""
    return {
        "precision_at_5": precision_at_k(ranked_keys, labels_by_pair, candidate_key),
        "ndcg_at_5": ndcg_at_k(ranked_keys, labels_by_pair, candidate_key, all_listing_keys),
        "recall_at_10": recall_at_k(ranked_keys, labels_by_pair, candidate_key, all_listing_keys),
        "mrr": mrr(ranked_keys, labels_by_pair, candidate_key),
    }


METRIC_FIELDS = ["precision_at_5", "ndcg_at_5", "recall_at_10", "mrr"]
