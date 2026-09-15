"""
Main entry point for the matching lab.

Scores every candidate against every listing under all seven approaches, ranks
them, compares the rankings to the hand-graded labels, and writes the result
files under results/.

Usage (only embedding-service needs to be running):

    docker compose -f docker-compose.local.yml up embedding-service
    cd scripts/matching-lab
    python run_experiments.py

Options:
    --today YYYY-MM-DD   pin the reference date for ongoing tenure, so a run
                         months later gives the same numbers. Defaults to today,
                         and whichever date was used is recorded in report.md.

labels.json is not read until the metrics stage. Scorers are handed candidates
and listings only, never a grade.
"""

import argparse
import csv
import json
import sys
from collections import defaultdict
from datetime import date, datetime
from pathlib import Path

import bootstrap
import metrics
from components import Context
from embedding_client import EmbeddingClient, EmbeddingServiceUnavailable, cosine
from scorers import REGISTRY

BASE = Path(__file__).parent
FIXTURES = BASE / "fixtures"
RESULTS = BASE / "results"

# What each diagnostic tag is testing, printed into diagnostics.md so the file
# explains itself.
DIAGNOSTIC_NOTES = {
    "cross-domain-tenure": "Five or more years in an unrelated field, applying in another. Does irrelevant tenure add noise instead of seniority signal?",
    "skill-superset": "Identical to a paired candidate except for extra unrelated skills. Does mean-pooling dilution push the superset candidate below its own twin?",
    "esco-gap": "A candidate under-claiming a skill that is in the seeded vocabulary. `Spring Boot` is there (the manual supplement added it) but this candidate never selected it, although their work history describes the work plainly. This is a real strength of the embedding approach and any replacement has to keep it.",
    "vocabulary-gap": "A skill the seeded vocabulary cannot express. The candidate's history is explicit about Scrum, but `Scrum` is in no seeded skill list, so there was no structured skill to select. The listing requires `Agile project management`, which is in the vocabulary. Structural coverage cannot reach this pair; only semantic evidence can.",
    "overqualified": "Far more tenure than the listing's band. Is over-qualification penalised gently?",
    "underqualified": "Far less tenure than the listing's band. Is under-qualification penalised harder than over-qualification?",
    "career-changer": "Years in one area, most recent years in another. Does recent relevant work outweigh older irrelevant work? The labels vary across the three bands of the new area and run opposite to total tenure, so M4 (total_years) and M5 (relevant_years) cannot score it identically.",
    "education-gate-pass": "Holds more than the listing requires. Does the ordinal gate pass on >= rather than ==?",
    "education-gate-fail": "Holds less than the listing requires. Does the gate exclude, and is the exclusion visible rather than silent?",
    "ongoing-degree": "Degree in progress against a listing requiring it. Checks the 'ongoing counts at its stated level' decision.",
    "thin-profile": "Strong structured skills, almost no free text. How much does each approach depend on prose?",
}


def die(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(1)


def load_json(path: Path) -> dict:
    if not path.exists():
        die(f"{path} does not exist")
    return json.loads(path.read_text(encoding="utf-8"))


def check_vocabulary(candidates: list, listings: list) -> list:
    """
    Warn about any fixture skill name missing from the seeded vocabulary.

    Such a name scores zero structural coverage, which in the results looks like
    an algorithm finding rather than the typo it usually is.
    """
    vocabulary_path = FIXTURES / "vocabulary.txt"
    if not vocabulary_path.exists():
        print(f"WARNING: {vocabulary_path.name} is missing - skipping the skill vocabulary check. "
              f"Run `python build_vocabulary.py` to generate it.", file=sys.stderr)
        return []

    vocabulary = {line.strip().lower() for line in vocabulary_path.read_text(encoding="utf-8").splitlines() if line.strip()}

    unknown = []
    for holder, kind in [(candidates, "candidate"), (listings, "listing")]:
        for item in holder:
            for name in item.get("skills", []):
                if name.strip().lower() not in vocabulary:
                    unknown.append((kind, item["key"], name))

    if unknown:
        print(f"WARNING: {len(unknown)} fixture skill name(s) are not in the seeded vocabulary:", file=sys.stderr)
        for kind, key, name in unknown:
            print(f"  {kind} {key}: {name!r}", file=sys.stderr)
        print("  (only deliberate for an esco-gap fixture; otherwise it is a typo)", file=sys.stderr)
    return unknown


def flatten_explanation(explanation: dict) -> dict:
    """
    One column per named sub-score for rankings.csv. Nested traces (M5's
    per-experience relevance weights and per-skill backfill credits) go into a
    single JSON cell, so the CSV's shape does not depend on the data.
    """
    flat = {}
    for key, value in explanation.items():
        if isinstance(value, (list, dict)):
            flat[key] = json.dumps(value, separators=(",", ":"))
        elif isinstance(value, float):
            flat[key] = round(value, 6)
        else:
            flat[key] = value
    return flat


def run(candidates: list, listings: list, labels_by_pair: dict, ctx: Context) -> dict:
    """
    Returns {approach: {candidate_key: {"ranked": [...], "metrics": {...},
                                        "scores": {listing_key: (score, explanation)}}}}
    """
    all_listing_keys = [listing["key"] for listing in listings]
    results = {}

    for approach, score_fn in REGISTRY.items():
        per_candidate = {}
        for candidate in candidates:
            scores = {}
            scored_pairs = []
            for listing in listings:
                value, explanation = score_fn(candidate, listing, ctx)
                scores[listing["key"]] = (value, explanation)
                scored_pairs.append((value, listing["key"]))

            ranked = metrics.rank_listings(scored_pairs)
            per_candidate[candidate["key"]] = {
                "ranked": ranked,
                "scores": scores,
                "metrics": metrics.evaluate(ranked, labels_by_pair, candidate["key"], all_listing_keys),
            }
        results[approach] = per_candidate
        print(f"  scored {approach}")

    return results


def mean(values: list) -> float:
    return sum(values) / len(values) if values else 0.0


def write_summary(results: dict, boot: dict) -> list:
    """
    One row per approach: the four means, then the bootstrap interval around the
    two headline metrics, so a mean is never read without it.
    """
    rows = []
    for approach, per_candidate in results.items():
        row = {"approach": approach}
        for field in metrics.METRIC_FIELDS:
            row[field] = mean([c["metrics"][field] for c in per_candidate.values()])
        for metric in bootstrap.BOOTSTRAP_METRICS:
            interval = boot["per_approach"][approach][metric]
            row[f"{metric}_ci_low"] = round(interval["ci_low"], 6)
            row[f"{metric}_ci_high"] = round(interval["ci_high"], 6)
        rows.append(row)

    ci_fields = [f"{m}_ci_{b}" for m in bootstrap.BOOTSTRAP_METRICS for b in ("low", "high")]
    with open(RESULTS / "summary.csv", "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=["approach"] + metrics.METRIC_FIELDS + ci_fields)
        writer.writeheader()
        writer.writerows(rows)
    return rows


def write_bootstrap_pairwise(boot: dict) -> None:
    """Every pairwise approach difference, with its interval and verdict."""
    with open(RESULTS / "bootstrap_pairwise.csv", "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(
            fh,
            fieldnames=["metric", "approach_a", "approach_b", "diff_b_minus_a",
                        "ci_low", "ci_high", "verdict"],
        )
        writer.writeheader()
        for row in boot["pairwise"]:
            writer.writerow({
                "metric": row["metric"],
                "approach_a": row["a"],
                "approach_b": row["b"],
                "diff_b_minus_a": round(row["diff"], 6),
                "ci_low": round(row["ci_low"], 6),
                "ci_high": round(row["ci_high"], 6),
                "verdict": row["verdict"],
            })


def write_per_candidate(results: dict, candidates: list) -> None:
    area_by_key = {c["key"]: c.get("area") for c in candidates}
    diagnostic_by_key = {c["key"]: c.get("diagnostic") for c in candidates}

    with open(RESULTS / "per_candidate.csv", "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(
            fh,
            fieldnames=["approach", "candidateKey", "area", "diagnostic"] + metrics.METRIC_FIELDS,
        )
        writer.writeheader()
        for approach, per_candidate in results.items():
            for candidate_key, payload in per_candidate.items():
                writer.writerow({
                    "approach": approach,
                    "candidateKey": candidate_key,
                    "area": area_by_key.get(candidate_key),
                    "diagnostic": diagnostic_by_key.get(candidate_key) or "",
                    **{f: payload["metrics"][f] for f in metrics.METRIC_FIELDS},
                })


def write_rankings(results: dict, labels_by_pair: dict) -> None:
    rows = []
    explanation_keys = []

    for approach, per_candidate in results.items():
        for candidate_key, payload in per_candidate.items():
            rank_of = {key: i + 1 for i, key in enumerate(payload["ranked"])}
            for listing_key, (value, explanation) in payload["scores"].items():
                flat = flatten_explanation(explanation)
                for key in flat:
                    if key not in explanation_keys:
                        explanation_keys.append(key)
                rows.append({
                    "approach": approach,
                    "candidateKey": candidate_key,
                    "listingKey": listing_key,
                    "score": round(value, 6),
                    "rank": rank_of[listing_key],
                    "grade": metrics.grade_of(labels_by_pair, candidate_key, listing_key),
                    **{f"exp_{k}": v for k, v in flat.items()},
                })

    fieldnames = ["approach", "candidateKey", "listingKey", "score", "rank", "grade"]
    fieldnames += [f"exp_{k}" for k in explanation_keys]

    rows.sort(key=lambda r: (r["approach"], r["candidateKey"], r["rank"]))
    with open(RESULTS / "rankings.csv", "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=fieldnames, restval="")
        writer.writeheader()
        writer.writerows(rows)


def write_diagnostics(results: dict, candidates: list, listings: list, labels_by_pair: dict) -> None:
    """
    One section per diagnostic tag in the fixtures: what it tests, and how each
    approach ranked the listings in that candidate's area. For skill-superset
    the score difference between the twins is reported too, since that number is
    the dilution measurement.
    """
    by_key = {c["key"]: c for c in candidates}
    listings_by_area = defaultdict(list)
    for listing in listings:
        listings_by_area[listing.get("area")].append(listing["key"])

    tagged = defaultdict(list)
    for candidate in candidates:
        if candidate.get("diagnostic"):
            tagged[candidate["diagnostic"]].append(candidate["key"])

    lines = ["# Diagnostic fixtures", "",
             "Written by `run_experiments.py`.", "",
             "The aggregate metrics say which approach scores best. These say why. Each fixture",
             "below is a case built to make the approaches disagree, shown against the listings in",
             "that candidate's own area.", ""]

    if not tagged:
        lines += ["_No candidate carries a `diagnostic` tag in the current fixture set._", ""]

    for tag in sorted(tagged):
        lines += [f"## `{tag}`", "", DIAGNOSTIC_NOTES.get(tag, "_No description registered for this tag._"), ""]

        for candidate_key in tagged[tag]:
            candidate = by_key[candidate_key]
            area_listings = listings_by_area.get(candidate.get("area"), [])
            lines += [f"**{candidate_key}** (area: {candidate.get('area')})", ""]

            if not area_listings:
                lines += ["_No listings in this candidate's area._", ""]
                continue

            header = "| Approach | " + " | ".join(f"{k} rank (score)" for k in area_listings) + " |"
            lines += [header, "|" + "---|" * (len(area_listings) + 1)]
            for approach, per_candidate in results.items():
                payload = per_candidate[candidate_key]
                rank_of = {key: i + 1 for i, key in enumerate(payload["ranked"])}
                cells = []
                for listing_key in area_listings:
                    value = payload["scores"][listing_key][0]
                    cells.append(f"{rank_of[listing_key]} ({value:.3f})")
                lines.append(f"| {approach} | " + " | ".join(cells) + " |")

            grades = ", ".join(
                f"{k}={metrics.grade_of(labels_by_pair, candidate_key, k)}" for k in area_listings
            )
            lines += ["", f"Intended grades: {grades}", ""]

            if tag == "skill-superset" and candidate.get("pairedWith"):
                pair_key = candidate["pairedWith"]
                lines += [f"### Dilution measurement vs `{pair_key}`", "",
                          "The two candidates are identical apart from the extra skills, so any non-zero",
                          "delta comes from dilution alone. Negative means the superset candidate scored lower.", "",
                          "| Approach | " + " | ".join(f"delta on {k}" for k in area_listings) + " |",
                          "|" + "---|" * (len(area_listings) + 1)]
                for approach, per_candidate in results.items():
                    if pair_key not in per_candidate:
                        continue
                    cells = []
                    for listing_key in area_listings:
                        delta = (per_candidate[candidate_key]["scores"][listing_key][0]
                                 - per_candidate[pair_key]["scores"][listing_key][0])
                        cells.append(f"{delta:+.4f}")
                    lines.append(f"| {approach} | " + " | ".join(cells) + " |")
                lines.append("")

    (RESULTS / "diagnostics.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def gate_alignment(results: dict, labels_by_pair: dict) -> dict:
    """
    For each gate, how many pairs it zeroed and what the labels say about them.
    A gate that only excludes grade-0 pairs agrees with the labels; one that
    excludes relevant pairs is over-penalising, and that is countable.

    Measured on the first approach applying both gates, which is M4. M5 and M6
    gate identically, so counting all three would triple the numbers.
    Location-only approaches are skipped, because M4 checks education first and
    attributes the overlapping pairs there.
    """
    best = None
    for approach, per_candidate in results.items():
        counts = {}
        found = False
        for candidate_key, payload in per_candidate.items():
            for listing_key, (_, explanation) in payload["scores"].items():
                kind = explanation.get("excluded")
                if not kind:
                    continue
                found = True
                grade = metrics.grade_of(labels_by_pair, candidate_key, listing_key)
                bucket = counts.setdefault(kind, {"total": 0, "relevant": 0, "pairs": []})
                bucket["total"] += 1
                if grade >= 1:
                    bucket["relevant"] += 1
                    bucket["pairs"].append((candidate_key, listing_key, grade))
        if not found:
            continue
        if "education" in counts:
            return {"approach": approach, "counts": counts}
        if best is None:
            best = {"approach": approach, "counts": counts}
    return best or {"approach": None, "counts": {}}


def write_report(summary_rows: list, candidates: list, listings: list, labels: dict,
                 ctx: Context, unknown_skills: list, client: EmbeddingClient, boot: dict,
                 gates: dict) -> str:
    def find(approach: str) -> dict:
        return next((r for r in summary_rows if r["approach"] == approach), None)

    def delta_line(from_id: str, to_id: str) -> str:
        a, b = find(from_id), find(to_id)
        if a is None or b is None:
            return f"| {from_id} -> {to_id} | n/a | n/a | n/a |"
        nd = bootstrap.difference(boot, "ndcg_at_5", from_id, to_id)
        ci = f"[{nd['ci_low']:+.4f}, {nd['ci_high']:+.4f}]" if nd else "n/a"
        verdict = "**conclusive**" if (nd and nd["verdict"] == "conclusive") else "inconclusive"
        return (f"| {from_id} -> {to_id} | "
                f"{b['ndcg_at_5'] - a['ndcg_at_5']:+.4f} | {ci} | "
                f"{b['precision_at_5'] - a['precision_at_5']:+.4f} | {verdict} |")

    winner = max(summary_rows, key=lambda r: r["ndcg_at_5"])
    runner_up = sorted(summary_rows, key=lambda r: r["ndcg_at_5"], reverse=True)[1] if len(summary_rows) > 1 else None

    lines = [
        "# Matching Algorithm Comparison",
        "",
        f"Run date: {datetime.now().strftime('%Y-%m-%d %H:%M')}  ",
        f"Reference date used for ongoing tenure (`today`): **{ctx.today.isoformat()}**  ",
        f"TAU: {ctx.tau} · BACKFILL_CAP: {ctx.backfill_cap} · "
        f"M4/M5 weights: {ctx.weights}  ",
        f"Embedding HTTP calls this run: {client.http_calls} · cache hits: {client.cache_hits}",
        "",
        "_Written by `run_experiments.py`. Re-running with unchanged fixtures reproduces every",
        "result file byte for byte; the run date and the HTTP-call line above are the only parts",
        "that vary. Neither is a measurement, and a warm-cache run reports 0 calls because the",
        "cache is doing its job._",
        "",
        "## Fixture set",
        "",
        f"- Candidates: {len(candidates)}",
        f"- Listings: {len(listings)}",
        f"- Graded pairs in `labels.json`: {len(labels.get('grades', []))} "
        f"(complete set would be {len(candidates) * len(listings)})",
        f"- Fixture skill names not found in the seeded vocabulary: {len(unknown_skills)}",
        "",
    ]

    if labels.get("_note"):
        lines += [f"> {labels['_note']}", ""]

    # Candidates with no pair graded >= 2 contribute a constant 0 to the three
    # set-based metrics. Counted here rather than left to be found in
    # per_candidate.csv.
    graded = labels.get("grades", [])
    best_by_candidate = {}
    for row in graded:
        key = row["candidateKey"]
        best_by_candidate[key] = max(best_by_candidate.get(key, 0), row["grade"])
    no_shortlist = sorted(k for k, v in best_by_candidate.items() if v < metrics.RELEVANT_THRESHOLD)

    # Which metrics came out identical across every approach. Detected from the
    # data, so the paragraph below cannot go stale if a fixture change later
    # makes one of them separate the approaches again.
    flat_metrics = [
        field for field in metrics.METRIC_FIELDS
        if len({round(row[field], 9) for row in summary_rows}) == 1
    ]

    lines += [
        "## Reading these numbers",
        "",
        (
            "**"
            + ", ".join(flat_metrics)
            + " came out identical for all seven approaches** and separate nothing here. "
              "That is the shape of the benchmark rather than a result: with 18 listings a top-5 "
              "cut is over a quarter of the pool, and every approach puts every grade->=2 listing "
              "inside it for every candidate. Recall@10 is worse, at two thirds of the pool. "
              "Raising the relevance threshold to 2 fixed the saturation these metrics had at a "
              "threshold of 1, but it could not fix the pool being too small for a top-k cut to "
              "separate anything. So compare the approaches on nDCG@5 and nothing else. The "
              "matching P@5 figures are not seven approaches tying; the metric simply cannot see "
              "the difference between them. Measuring precision and recall properly would need a "
              "listing pool several times larger, which is a fixture change rather than an "
              "algorithm change."
            if flat_metrics else
            "All four metrics vary across approaches on this fixture set."
        ),
        "",
        f"**Relevance threshold.** Precision@5, Recall@10 and MRR count a pair as relevant at "
        f"grade >= {metrics.RELEVANT_THRESHOLD}, which rubric.md calls a genuine shortlist entry, "
        "rather than at grade >= 1. The floor rule (rubric.md 3.4) puts nearly every same-area "
        "pair at 1 or above, so at a threshold of 1 those three metrics turn into \"did the "
        "approach get the professional area right\", which all of them do, and they saturate. "
        "nDCG@5 reads the graded gains directly and has no threshold, so this decision cannot "
        "have moved it. That is the main reason it is the headline metric here.",
        "",
        f"**{len(no_shortlist)} of {len(candidates)} candidates have no pair graded >= "
        f"{metrics.RELEVANT_THRESHOLD} anywhere in the fixture set**"
        + (f" ({', '.join(no_shortlist)})" if no_shortlist else "") + ". They contribute a constant "
        "0 to P@5, Recall@10 and MRR under every approach, which pulls those three absolute numbers "
        "down without changing any comparison between approaches. Three of them are diagnostics "
        "where that is the intended result; the rest are specialists with no matching listing in a "
        "pool this small. Their nDCG is unaffected, since a pool of grade-1 pairs still has a "
        "non-zero ideal DCG.",
        "",
        "**The rubric and the hybrids disagree about education, on purpose.** The rubric caps an "
        "unmet education requirement at grade 1 (rubric.md section 4), while M4, M5 and M6 return a "
        "hard 0 and drop the pair. Leaving that unreconciled is what lets the data show whether a "
        "hard structural gate over-penalises compared with how a recruiter would read an unmet "
        "formal requirement. `mkt-education-gate-fail-1` and `ops-logistics-senior-1` in "
        "`diagnostics.md` show the effect.",
        "",
        "**On the ESCO vocabulary gap.** The manual skill supplement has already closed most of the "
        "framework-name gap in the ESCO taxonomy. `Spring Boot`, `React`, `Docker`, `Kubernetes`, "
        "`CI/CD` and `DevOps` are all in the seeded vocabulary now, so the `esco-gap` fixture tests "
        "a candidate under-claiming a skill that exists rather than the vocabulary missing one. "
        "`vocabulary-gap` covers the narrower remaining case: `Scrum` really is absent, so no "
        "structured skill can express it and only semantic evidence reaches it. A claim that the "
        "vocabulary cannot handle modern framework names should be narrowed to that.",
        "",
        "## Summary",
        "",
        "| Approach | P@5 | nDCG@5 | nDCG@5 95% CI | Recall@10 | MRR |",
        "|---|---|---|---|---|---|",
    ]
    for row in summary_rows:
        lines.append(
            f"| {row['approach']} | {row['precision_at_5']:.4f} | {row['ndcg_at_5']:.4f} | "
            f"[{row['ndcg_at_5_ci_low']:.4f}, {row['ndcg_at_5_ci_high']:.4f}] | "
            f"{row['recall_at_10']:.4f} | {row['mrr']:.4f} |"
        )

    lines += ["", "## Ablation deltas", "",
              "Each rung adds exactly one mechanism, so a delta is attributable to that mechanism.",
              "",
              "| Step | nDCG@5 delta | 95% CI of the delta | P@5 delta | Verdict |",
              "|---|---|---|---|---|",
              delta_line("M1_embedding_fused", "M2_embedding_fused_level"),
              delta_line("M2_embedding_fused_level", "M3_embedding_sections"),
              delta_line("M3_embedding_sections", "M4_hybrid_structural"),
              delta_line("M4_hybrid_structural", "M5_hybrid_relevance_weighted"),
              delta_line("M5_hybrid_relevance_weighted", "M6_hybrid_level_adaptive"),
              delta_line("M0_rule_based_v1", "M1_embedding_fused"),
              ""]

    lines += ["## Result", "",
              f"Highest mean nDCG@5: **{winner['approach']}** ({winner['ndcg_at_5']:.4f})."]
    if runner_up is not None:
        head = bootstrap.difference(boot, "ndcg_at_5", runner_up["approach"], winner["approach"])
        margin = winner["ndcg_at_5"] - runner_up["ndcg_at_5"]
        lines.append(f"Runner-up: {runner_up['approach']} ({runner_up['ndcg_at_5']:.4f}), "
                     f"a margin of {margin:+.4f}.")
        if head is not None:
            lines += ["",
                      f"Paired bootstrap on that margin: 95% CI "
                      f"[{head['ci_low']:+.4f}, {head['ci_high']:+.4f}] over {boot['iterations']} "
                      f"resamples of the {boot['n_candidates']} candidates (seed {boot['seed']})."]
            if head["verdict"] == "conclusive":
                lines.append(f"The interval excludes zero, so {winner['approach']} is ahead of "
                             f"{runner_up['approach']} by more than resampling noise on this fixture set.")
            else:
                lines.append(f"**The interval straddles zero, so this one is inconclusive.** "
                             f"{winner['approach']} has the highest mean, but the benchmark cannot "
                             f"tell it apart from {runner_up['approach']}, and calling it a win "
                             f"would claim more than {boot['n_candidates']} candidates can support. "
                             f"The pairwise table below shows which differences are conclusive.")
    lines += ["",
              "`diagnostics.md` has the per-fixture traces. Read those before trusting this line: a",
              "diagnostic that contradicts the aggregate is the more interesting result.",
              ""]

    lines += ["## Pairwise differences (paired bootstrap)", "",
              f"{boot['iterations']} iterations, seed {boot['seed']}, resampling the "
              f"{boot['n_candidates']} query candidates with replacement. Every approach is scored on "
              "the same resample in each iteration, so the shared \"this resample was an easy one\" "
              "effect cancels out of the difference. `diff` is B minus A, so a positive value means "
              "B scored higher.",
              "",
              "A difference whose interval contains zero is inconclusive, not a win.",
              "",
              "| Metric | A | B | diff (B-A) | 95% CI | Verdict |",
              "|---|---|---|---|---|---|"]
    for row in boot["pairwise"]:
        flag = "conclusive" if row["verdict"] == "conclusive" else "_inconclusive_"
        lines.append(f"| {row['metric']} | {row['a']} | {row['b']} | {row['diff']:+.4f} | "
                     f"[{row['ci_low']:+.4f}, {row['ci_high']:+.4f}] | {flag} |")
    conclusive = sum(1 for r in boot["pairwise"] if r["verdict"] == "conclusive")
    lines += ["",
              f"{conclusive} of {len(boot['pairwise'])} pairwise differences are conclusive at 95%.",
              "Full table also in `bootstrap_pairwise.csv`.",
              ""]

    if gates.get("counts"):
        lines += ["## Gate alignment: does a hard gate over-penalise?", "",
                  "M4, M5 and M6 return a hard 0 and drop a pair when a gate fails. The rubric caps "
                  "an unmet education requirement at grade 1 instead (rubric.md section 4). That "
                  "disagreement was left in place so the cost of the hard gate could be counted "
                  "rather than argued about. Counted below on `" + str(gates["approach"]) + "`; all "
                  "three hybrids gate the same way.",
                  "",
                  "| Gate | Pairs excluded | Of those, labelled relevant (grade >= 1) | Reading |",
                  "|---|---|---|---|"]
        for kind in sorted(gates["counts"]):
            bucket = gates["counts"][kind]
            if bucket["relevant"] == 0:
                reading = "Agrees with the labels on every pair it excludes"
            else:
                reading = f"Over-penalises: {bucket['relevant']} pair(s) the rubric grades >= 1 are forced to 0"
            lines.append(f"| {kind} | {bucket['total']} | {bucket['relevant']} | {reading} |")

        offenders = gates["counts"].get("education", {}).get("pairs", [])
        if offenders:
            lines += ["",
                      "The education-gated pairs the labels disagree with, each forced from a "
                      "shortlist-adjacent grade to the bottom of an 18-listing ranking:",
                      "",
                      "| Candidate | Listing | Label |", "|---|---|---|"]
            for candidate_key, listing_key, grade in sorted(offenders):
                lines.append(f"| `{candidate_key}` | `{listing_key}` | {grade} |")
            lines += ["",
                      "That is a measured cost of the hard gate, and part of why the hybrid rungs "
                      "score below the embedding rungs here. It is not the whole story, as the "
                      "ablation deltas show. The hybrids are not re-run with a softened gate: "
                      "picking a gate policy after seeing which one scores better is tuning on the "
                      "test set. Softening it and measuring again would be a separate piece of work.",
                      ""]

    lines += [
        "## Stated limitations",
        "",
        "- This is a **synthetic benchmark**. Its labels encode the designer's model of relevance.",
        "  It supports *relative* comparison between approaches on identical data; it is not a claim",
        "  about absolute real-world matching quality.",
        "- Labels were graded by hand from candidate and listing content alone, with no algorithm",
        "  output consulted. `audit_agreement.md` reports how closely a second blind pass over a",
        "  sample of them agrees with the originals.",
        "- No number here rests on a single arbitrary constant without a check: see `sensitivity.md`",
        "  for the TAU and weight sweep.",
        "- Overlapping experience entries are summed rather than de-overlapped (see `total_years`).",
        "- **The seniority labels are contaminated, not convergent.** The rubric was written with the",
        "  algorithm design already in hand, so M4's asymmetric `seniority_score` and its",
        "  `LEVEL_BANDS` had a direct path into the ground truth. An M3 to M4 gain is partly the",
        "  labels agreeing with M4's design rather than evidence that M4 is better, and should be",
        "  read that way. M4 to M5 to M6 is unaffected, since all three share the same seniority",
        "  component. See rubric.md section 8.2.",
        "- The bootstrap covers sampling variability over these 40 candidates and nothing else. It",
        "  says nothing about whether the fixture set is representative, and a tight interval around",
        "  a contaminated difference is still a contaminated difference.",
        "",
    ]

    report = "\n".join(lines) + "\n"
    (RESULTS / "report.md").write_text(report, encoding="utf-8")
    return report


def print_summary(summary_rows: list) -> None:
    header = f"{'approach':<32} {'P@5':>8} {'nDCG@5':>8} {'R@10':>8} {'MRR':>8}"
    print()
    print(header)
    print("-" * len(header))
    for row in summary_rows:
        print(f"{row['approach']:<32} {row['precision_at_5']:>8.4f} {row['ndcg_at_5']:>8.4f} "
              f"{row['recall_at_10']:>8.4f} {row['mrr']:>8.4f}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the matching-algorithm comparison.")
    parser.add_argument("--today", help="Reference date (YYYY-MM-DD) for ongoing tenure. Defaults to today.")
    args = parser.parse_args()

    today = date.fromisoformat(args.today) if args.today else date.today()

    candidates = load_json(FIXTURES / "candidates.json")["candidates"]
    listings = load_json(FIXTURES / "listings.json")["listings"]
    labels = load_json(FIXTURES / "labels.json")
    labels_by_pair = {(g["candidateKey"], g["listingKey"]): g["grade"] for g in labels["grades"]}

    unknown_skills = check_vocabulary(candidates, listings)

    RESULTS.mkdir(exist_ok=True)

    client = EmbeddingClient()
    ctx = Context(embed=client.embed, cosine=cosine, today=today)

    print(f"Scoring {len(candidates)} candidates x {len(listings)} listings "
          f"x {len(REGISTRY)} approaches (today={today.isoformat()})")
    try:
        results = run(candidates, listings, labels_by_pair, ctx)
    except EmbeddingServiceUnavailable as exc:
        client.flush()
        die(str(exc))

    client.flush()

    boot = bootstrap.run(results, [c["key"] for c in candidates])

    summary_rows = write_summary(results, boot)
    write_bootstrap_pairwise(boot)
    write_per_candidate(results, candidates)
    write_rankings(results, labels_by_pair)
    write_diagnostics(results, candidates, listings, labels_by_pair)
    gates = gate_alignment(results, labels_by_pair)
    write_report(summary_rows, candidates, listings, labels, ctx, unknown_skills, client, boot, gates)

    print_summary(summary_rows)
    print(f"\nEmbedding HTTP calls: {client.http_calls} (cache hits: {client.cache_hits})")
    print(f"Wrote {RESULTS.name}/summary.csv, per_candidate.csv, rankings.csv, bootstrap_pairwise.csv, diagnostics.md, report.md")


if __name__ == "__main__":
    main()
