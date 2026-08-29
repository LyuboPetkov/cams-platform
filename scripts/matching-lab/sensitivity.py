"""
Sensitivity sweep over TAU and the M4/M5 weight triple.

The point is not to find better constants. It is to show that the conclusion
does not rest on one arbitrary number - and to report honestly if it does.
A winner that only wins at TAU = 0.35 is not a winner.

    python sensitivity.py                 # writes results/sensitivity.csv + .md
    python sensitivity.py --today 2026-08-27

Two sweeps:

1. TAU, 0.20 to 0.50 in steps of 0.05. Reaches every rung that calls
   rescale() - M4, M5, M6. M0-M3 do not use TAU at all, so they are scored
   once and carried across as a flat reference line; re-scoring them per tau
   would produce seven identical rows and imply a dependence that does not
   exist.

2. The M4/M5 weight triple, over a coarse grid of triples summing to 1.0.
   M6 is excluded because its weights come from the level profile, not from
   this triple - sweeping it here would sweep a constant it ignores.

Every combination reuses the same embedding cache, so the whole sweep costs
zero additional HTTP calls once run_experiments.py has been run once.
"""

import argparse
import csv
import json
from datetime import date
from pathlib import Path

import metrics
import run_experiments
from components import Context, DEFAULT_WEIGHTS, TAU
from embedding_client import EmbeddingClient, EmbeddingServiceUnavailable, cosine
from scorers import REGISTRY, TAU_SENSITIVE, WEIGHT_SENSITIVE

BASE = Path(__file__).parent
FIXTURES = BASE / "fixtures"
RESULTS = BASE / "results"

TAU_VALUES = [0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50]

# Coarse grid: skills and semantic vary, seniority is whatever is left, and
# triples that would push a component outside 0.15-0.45 are dropped as
# implausible rather than swept for completeness.
GRID_VALUES = [0.25, 0.30, 0.35, 0.40, 0.45]
COMPONENT_MIN, COMPONENT_MAX = 0.15, 0.45


def weight_grid() -> list:
    triples = []
    for skills in GRID_VALUES:
        for semantic in GRID_VALUES:
            seniority = round(1.0 - skills - semantic, 4)
            if COMPONENT_MIN <= seniority <= COMPONENT_MAX:
                triples.append({"skills": skills, "semantic": semantic, "seniority": seniority})
    return triples


def score_approaches(approaches: list, candidates: list, listings: list,
                     labels_by_pair: dict, ctx: Context) -> dict:
    """Mean metrics per approach, for a subset of the registry."""
    all_listing_keys = [listing["key"] for listing in listings]
    output = {}

    for approach in approaches:
        score_fn = REGISTRY[approach]
        per_candidate = []
        for candidate in candidates:
            scored_pairs = []
            for listing in listings:
                value, _ = score_fn(candidate, listing, ctx)
                scored_pairs.append((value, listing["key"]))
            ranked = metrics.rank_listings(scored_pairs)
            per_candidate.append(metrics.evaluate(ranked, labels_by_pair, candidate["key"], all_listing_keys))

        output[approach] = {
            field: run_experiments.mean([m[field] for m in per_candidate])
            for field in metrics.METRIC_FIELDS
        }
    return output


def main() -> None:
    parser = argparse.ArgumentParser(description="Sweep TAU and the M4/M5 weight triple.")
    parser.add_argument("--today", help="Reference date (YYYY-MM-DD). Defaults to today.")
    args = parser.parse_args()
    today = date.fromisoformat(args.today) if args.today else date.today()

    candidates = run_experiments.load_json(FIXTURES / "candidates.json")["candidates"]
    listings = run_experiments.load_json(FIXTURES / "listings.json")["listings"]
    labels = run_experiments.load_json(FIXTURES / "labels.json")
    labels_by_pair = {(g["candidateKey"], g["listingKey"]): g["grade"] for g in labels["grades"]}

    RESULTS.mkdir(exist_ok=True)
    client = EmbeddingClient()

    rows = []
    tau_invariant = [a for a in REGISTRY if a not in TAU_SENSITIVE]

    try:
        # Reference line: the rungs no swept constant reaches.
        reference_ctx = Context(embed=client.embed, cosine=cosine, today=today)
        reference = score_approaches(tau_invariant, candidates, listings, labels_by_pair, reference_ctx)
        for approach, values in reference.items():
            rows.append({"sweep": "reference", "approach": approach, "tau": "", "w_skills": "",
                         "w_semantic": "", "w_seniority": "", **values})

        # Sweep 1: TAU.
        tau_results = {}
        for tau in TAU_VALUES:
            ctx = Context(embed=client.embed, cosine=cosine, today=today, tau=tau)
            scored = score_approaches(TAU_SENSITIVE, candidates, listings, labels_by_pair, ctx)
            tau_results[tau] = scored
            for approach, values in scored.items():
                rows.append({"sweep": "tau", "approach": approach, "tau": tau,
                             "w_skills": ctx.weights["skills"], "w_semantic": ctx.weights["semantic"],
                             "w_seniority": ctx.weights["seniority"], **values})
            print(f"  tau={tau:.2f} done")

        # Sweep 2: the M4/M5 weight triple, at the default TAU.
        weight_results = {}
        for weights in weight_grid():
            ctx = Context(embed=client.embed, cosine=cosine, today=today, weights=dict(weights))
            scored = score_approaches(WEIGHT_SENSITIVE, candidates, listings, labels_by_pair, ctx)
            key = (weights["skills"], weights["semantic"], weights["seniority"])
            weight_results[key] = scored
            for approach, values in scored.items():
                rows.append({"sweep": "weights", "approach": approach, "tau": ctx.tau,
                             "w_skills": weights["skills"], "w_semantic": weights["semantic"],
                             "w_seniority": weights["seniority"], **values})
        print(f"  {len(weight_results)} weight triples done")
    except EmbeddingServiceUnavailable as exc:
        client.flush()
        run_experiments.die(str(exc))

    client.flush()

    fieldnames = ["sweep", "approach", "tau", "w_skills", "w_semantic", "w_seniority"] + metrics.METRIC_FIELDS
    with open(RESULTS / "sensitivity.csv", "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    write_markdown(reference, tau_results, weight_results, client)
    print(f"\nEmbedding HTTP calls: {client.http_calls} (cache hits: {client.cache_hits})")
    print(f"Wrote {RESULTS.name}/sensitivity.csv and sensitivity.md")


def write_markdown(reference: dict, tau_results: dict, weight_results: dict, client: EmbeddingClient) -> None:
    lines = [
        "# Sensitivity of the result to TAU and the M4/M5 weights",
        "",
        "A conclusion that only holds at one value of an arbitrary constant is not a conclusion.",
        "This file exists to check that, and to say so plainly if the check fails.",
        "",
        f"Embedding HTTP calls for the whole sweep: {client.http_calls} (everything reuses the cache).",
        "",
        "## Reference: rungs no swept constant reaches",
        "",
        "M0-M3 never call `rescale()` and never read the weight triple, so they are a flat line",
        "across both sweeps and are scored once.",
        "",
        "| Approach | nDCG@5 | P@5 |",
        "|---|---|---|",
    ]
    for approach, values in reference.items():
        lines.append(f"| {approach} | {values['ndcg_at_5']:.4f} | {values['precision_at_5']:.4f} |")

    lines += ["", "## Sweep 1: TAU (0.20 - 0.50)", "",
              "| TAU | " + " | ".join(TAU_SENSITIVE) + " | best overall |",
              "|" + "---|" * (len(TAU_SENSITIVE) + 2)]

    winners = []
    for tau in TAU_VALUES:
        scored = tau_results[tau]
        combined = {**{a: v["ndcg_at_5"] for a, v in reference.items()},
                    **{a: v["ndcg_at_5"] for a, v in scored.items()}}
        best = max(combined, key=combined.get)
        winners.append(best)
        cells = " | ".join(f"{scored[a]['ndcg_at_5']:.4f}" for a in TAU_SENSITIVE)
        lines.append(f"| {tau:.2f} | {cells} | {best} |")

    distinct_winners = sorted(set(winners))
    lines += ["", f"**Winner across the TAU sweep:** "
                  + (f"`{distinct_winners[0]}` at every value - stable."
                     if len(distinct_winners) == 1
                     else f"changes with TAU ({', '.join(distinct_winners)}) - **the result is TAU-dependent "
                          f"and must be reported as such.**"), ""]

    for approach in TAU_SENSITIVE:
        values = [tau_results[t][approach]["ndcg_at_5"] for t in TAU_VALUES]
        lines.append(f"- `{approach}` nDCG@5 ranges {min(values):.4f} - {max(values):.4f} "
                     f"(spread {max(values) - min(values):.4f}) across TAU.")

    lines += ["", "## Sweep 2: the M4/M5 weight triple", "",
              f"Coarse grid of {len(weight_results)} triples summing to 1.0, each component held within "
              f"{COMPONENT_MIN}-{COMPONENT_MAX}, at TAU = {TAU}.", ""]

    for approach in WEIGHT_SENSITIVE:
        scored = [(key, values[approach]["ndcg_at_5"]) for key, values in weight_results.items()]
        scored.sort(key=lambda pair: pair[1], reverse=True)
        best_key, best_value = scored[0]
        worst_key, worst_value = scored[-1]
        default_key = (DEFAULT_WEIGHTS["skills"], DEFAULT_WEIGHTS["semantic"], DEFAULT_WEIGHTS["seniority"])
        default_value = weight_results.get(default_key, {}).get(approach, {}).get("ndcg_at_5")

        lines += [f"### `{approach}`", "",
                  f"- Best triple: skills={best_key[0]}, semantic={best_key[1]}, seniority={best_key[2]} "
                  f"-> nDCG@5 {best_value:.4f}",
                  f"- Worst triple: skills={worst_key[0]}, semantic={worst_key[1]}, seniority={worst_key[2]} "
                  f"-> nDCG@5 {worst_value:.4f}",
                  f"- Spread across the grid: {best_value - worst_value:.4f}"]
        if default_value is not None:
            lines.append(f"- The declared default triple {default_key} scores {default_value:.4f}, "
                         f"{best_value - default_value:+.4f} from the grid's best.")
        lines.append("")

    lines += ["## How to read this", "",
              "A small spread means the rung's ranking is driven by its structure rather than by the",
              "constants, and the headline result stands on its own. A large spread, or a winner that",
              "changes across the sweep, means the opposite - and in that case the honest report is the",
              "range, not the best cell in it. **Do not select the best-performing constants from this",
              "sweep and rerun the headline experiment with them: that is tuning on the test set, and it",
              "would make the result indefensible.**",
              ""]

    (RESULTS / "sensitivity.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
