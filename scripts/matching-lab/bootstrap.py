"""
Paired bootstrap confidence intervals over the ladder.

With only 40 query candidates, a 0.02 nDCG@5 difference between two approaches
is inside resampling noise; this module puts an interval around every number so
a difference indistinguishable from zero is reported as inconclusive, not a win.

"Paired": each iteration draws one resample of the 40 candidate keys and scores
every approach on it, so the per-iteration difference between two approaches
cancels the shared "this resample was easy" effect. An unpaired bootstrap would
be far too conservative. The resample is over candidates, the unit of evaluation.

Deterministic (fixed seed and ordering). Quantifies sampling variability over
these 40 candidates only - it says nothing about fixture representativeness and
cannot correct for the label contamination in rubric.md 8.2.
"""

import numpy as np

DEFAULT_ITERATIONS = 2000
DEFAULT_SEED = 35
CI_LOW_PCT = 2.5
CI_HIGH_PCT = 97.5

# Metrics the bootstrap covers, reported per approach and per pairwise difference.
BOOTSTRAP_METRICS = ["ndcg_at_5", "precision_at_5"]


def _metric_matrix(results: dict, approaches: list, candidate_keys: list, metric: str) -> np.ndarray:
    """
    Rows = approaches, columns = candidates, in the given fixed orders.
    Built once so every iteration is a cheap column-index operation rather
    than a re-walk of the results dict.
    """
    return np.array(
        [[results[a][c]["metrics"][metric] for c in candidate_keys] for a in approaches],
        dtype=np.float64,
    )


def run(results: dict, candidate_keys: list,
        iterations: int = DEFAULT_ITERATIONS, seed: int = DEFAULT_SEED) -> dict:
    """
    Returns:
      {
        "iterations": int, "seed": int, "n_candidates": int,
        "per_approach": {approach: {metric: {"mean","ci_low","ci_high"}}},
        "pairwise": [ {"metric","a","b","diff","ci_low","ci_high","verdict"} ],
      }

    `diff` is always b - a, so a positive value means b scored higher.
    `verdict` is "conclusive" only when the interval excludes zero.
    """
    approaches = list(results.keys())
    candidate_keys = list(candidate_keys)
    n = len(candidate_keys)

    rng = np.random.default_rng(seed)
    # One index matrix reused across metrics: the SAME resamples must be seen
    # by every metric and every approach, otherwise the pairing is broken.
    draws = rng.integers(0, n, size=(iterations, n))

    per_approach = {a: {} for a in approaches}
    pairwise = []

    for metric in BOOTSTRAP_METRICS:
        matrix = _metric_matrix(results, approaches, candidate_keys, metric)

        # (iterations x approaches): the mean each approach scores on each
        # resample. matrix[:, draws] is (approaches x iterations x n).
        resampled_means = matrix[:, draws].mean(axis=2).T

        for i, approach in enumerate(approaches):
            column = resampled_means[:, i]
            per_approach[approach][metric] = {
                "mean": float(matrix[i].mean()),
                "ci_low": float(np.percentile(column, CI_LOW_PCT)),
                "ci_high": float(np.percentile(column, CI_HIGH_PCT)),
            }

        for i in range(len(approaches)):
            for j in range(i + 1, len(approaches)):
                # Per-iteration difference on the SAME resample - this is the
                # pairing, and it is why the intervals are usable at n=40.
                diffs = resampled_means[:, j] - resampled_means[:, i]
                low = float(np.percentile(diffs, CI_LOW_PCT))
                high = float(np.percentile(diffs, CI_HIGH_PCT))
                pairwise.append({
                    "metric": metric,
                    "a": approaches[i],
                    "b": approaches[j],
                    "diff": float(matrix[j].mean() - matrix[i].mean()),
                    "ci_low": low,
                    "ci_high": high,
                    "verdict": "conclusive" if (low > 0.0 or high < 0.0) else "inconclusive",
                })

    return {
        "iterations": iterations,
        "seed": seed,
        "n_candidates": n,
        "per_approach": per_approach,
        "pairwise": pairwise,
    }


def difference(report: dict, metric: str, a: str, b: str) -> dict | None:
    """Look up one pairwise row in either orientation, flipping the sign if needed."""
    for row in report["pairwise"]:
        if row["metric"] != metric:
            continue
        if row["a"] == a and row["b"] == b:
            return row
        if row["a"] == b and row["b"] == a:
            return {**row, "a": a, "b": b, "diff": -row["diff"],
                    "ci_low": -row["ci_high"], "ci_high": -row["ci_low"]}
    return None
