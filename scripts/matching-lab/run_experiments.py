"""
Matching-algorithm lab runner.

Scores every candidate against every listing under all seven approaches in the
ladder, ranks them, evaluates the rankings against the by-construction labels,
and writes the result files under results/.

Usage (only embedding-service needs to be running - no Postgres, no backend):

    docker compose -f docker-compose.local.yml up embedding-service
    cd scripts/matching-lab
    python run_experiments.py

Options:
    --today YYYY-MM-DD   pin the reference date used for ongoing tenure, so a
                         run reproduces exactly months later. Defaults to
                         today, and whichever date was used is recorded in
                         results/report.md.

Nothing here reads labels.json until the metrics stage: scorers are handed
candidates and listings only, never a grade.
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

# What each diagnostic tag is actually testing - printed into diagnostics.md so
# the file explains itself.
DIAGNOSTIC_NOTES = {
    "cross-domain-tenure": "5+ years in an unrelated field, applying in another. Tests weakness 2: does irrelevant tenure add noise instead of seniority signal?",
    "skill-superset": "Identical to a paired candidate except for extra unrelated skills. Tests weakness 3: does mean-pooling dilution push a superset candidate below its own twin?",
    "esco-gap": "UNDER-CLAIMING a skill that IS in the seeded vocabulary: `Spring Boot` exists (it is in the manual skill supplement) but this candidate never selected it, though their work history describes the work plainly. Tests the genuine strength of the embedding approach, which any replacement must not lose.",
    "vocabulary-gap": "A skill the seeded vocabulary CANNOT express: the candidate's history is explicit about Scrum, but `Scrum` is in no seeded skill list, so no structured skill could have been selected. The listing requires `Agile project management`, which is in the vocabulary. Structural coverage alone cannot reach this pair; only semantic evidence can. This is the case `esco-gap` does not cover.",
    "overqualified": "Far more tenure than the listing's band. Tests that over-qualification is penalised gently.",
    "underqualified": "Far less tenure than the listing's band. Tests that under-qualification is penalised harder than over-qualification.",
    "career-changer": "Years in one area, most recent years in another. Tests whether recent, relevant work outweighs older, irrelevant work. The labels vary across the three bands of the new area and are deliberately INVERTED against total tenure, so M4 (total_years) and M5 (relevant_years) cannot score it identically.",
    "education-gate-pass": "Holds more than the listing requires. Tests that the ordinal gate passes on >=, not on ==.",
    "education-gate-fail": "Holds less than the listing requires. Tests that the gate actually excludes, and that the exclusion is visible rather than silent.",
    "ongoing-degree": "Degree in progress against a listing requiring it. Tests the 'ongoing counts at its stated level' decision.",
    "thin-profile": "Strong structured skills, almost no free text. Tests how much each approach depends on prose.",
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
    Warn about any fixture skill name that is not in CAMS's seeded vocabulary.

    A name that is not in the vocabulary contributes nothing to structural
    coverage, which in the results looks like an algorithm finding rather than
    the typo it actually is. Regenerate the vocabulary with build_vocabulary.py
    when the skill migrations change.
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
        print("  (deliberate only for an esco-gap fixture - otherwise this is a typo that "
              "silently zeroes structural coverage)", file=sys.stderr)
    return unknown


def flatten_explanation(explanation: dict) -> dict:
    """
    rankings.csv gets one column per named sub-score. Nested traces (M5's
    per-experience relevance weights, its per-skill backfill credits) are
    JSON-encoded into a single cell rather than exploded into a variable
    number of columns, which would make the CSV shape depend on the data.
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
    One row per approach. The four means, then the paired-bootstrap 95%
    interval around the two headline metrics - so nobody reads a mean off
    this file without the interval that says whether it is distinguishable
    from the row above it.
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
    One section per diagnostic tag present in the fixtures: what it tests, how
    every approach ranked the listings in the candidate's own area, and a
    one-line read. For skill-superset, the score delta between the pair is
    reported explicitly - that number IS the measurement of dilution.
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
             "Aggregate metrics say *which* approach wins; these say *why*. Each fixture below is a",
             "designed case where the approaches are expected to diverge.", ""]

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
                          "Both members are identical apart from the extra skills, so any non-zero delta is",
                          "attributable to dilution alone. Negative means the superset candidate scored *lower*.", "",
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
    For each gate, how many pairs it zeroed and what the labels say about
    them. A gate that only ever excludes grade-0 pairs agrees with the ground
    truth; one that excludes graded-relevant pairs is over-penalising, and
    that is a measurable cost rather than an opinion.

    Measured on the first approach that applies BOTH gates - that is M4, and
    M5/M6 gate identically, so one is representative and the count would be
    triple-counted otherwise. Approaches applying only the location gate are
    skipped: reporting M0's location count here would understate the hybrids'
    education exclusions, since M4 checks education first and attributes the
    overlapping pairs there.
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
        "_Re-running with unchanged fixtures reproduces every result file byte for byte. The run date",
        "and the HTTP-call line above are the only content that varies between runs, and neither is a",
        "measurement - a warm-cache run reports 0 calls precisely because the cache is working._",
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
        lines += [f"> **{labels['_note']}**", ""]

    # Candidates with no pair graded >= 2 contribute a constant 0 to the three
    # set-based metrics under the raised threshold. Counted and stated rather
    # than left for a reader to discover in per_candidate.csv.
    graded = labels.get("grades", [])
    best_by_candidate = {}
    for row in graded:
        key = row["candidateKey"]
        best_by_candidate[key] = max(best_by_candidate.get(key, 0), row["grade"])
    no_shortlist = sorted(k for k, v in best_by_candidate.items() if v < metrics.RELEVANT_THRESHOLD)

    # Which metrics came out identical across every approach. Detected from
    # the data rather than asserted, so this paragraph cannot go stale if a
    # later fixture change makes one of them discriminate again.
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
            + " came out IDENTICAL for all seven approaches, and discriminate nothing on this "
              "fixture set.** This is a property of the benchmark's shape, not a bug and not a "
              "result: with only 18 listings, a top-5 cut is more than a quarter of the pool, and "
              "every approach places every grade->=2 listing inside it for every candidate. "
              "Recall@10 is worse still - two thirds of the pool. Raising the relevance threshold "
              "to 2 fixed the saturation those metrics had at threshold 1, but it could not fix "
              "the pool being too small for a top-k cut to separate anything. "
              "**Read nDCG@5 and nothing else for the comparison between approaches.** Reporting "
              "the identical P@5 figures as though seven approaches 'tied' would be misleading: "
              "they did not tie, the metric simply cannot see them. Measuring precision and recall "
              "meaningfully would need a listing pool several times larger, which is a fixture "
              "change, not an algorithm change."
            if flat_metrics else
            "All four metrics vary across approaches on this fixture set."
        ),
        "",
        f"**Relevance threshold.** Precision@5, Recall@10 and MRR count a pair as relevant at "
        f"**grade >= {metrics.RELEVANT_THRESHOLD}** (\"a genuine shortlist entry\"), not at the "
        "grade >= 1 the matching-eval harness used. The rubric's floor rule (rubric.md 3.4) puts "
        "nearly every same-area pair at 1 or above, so at a threshold of 1 those three metrics "
        "collapse into \"did the approach get the professional area right\", which every approach "
        "does, and they saturate. **nDCG@5 is unchanged** - it reads the graded gains directly and "
        "never had a threshold - which is why it is the headline metric here: the threshold decision "
        "cannot have moved it. The cost is that the matching-eval harness's P@5, Recall and MRR "
        "figures are no longer directly comparable; its nDCG figures still are.",
        "",
        f"**{len(no_shortlist)} of {len(candidates)} candidates have no pair graded >= "
        f"{metrics.RELEVANT_THRESHOLD} anywhere in the fixture set**"
        + (f" ({', '.join(no_shortlist)})" if no_shortlist else "") + ". They contribute a constant "
        "0 to P@5, Recall@10 and MRR for *every* approach, which deflates those three absolute "
        "numbers without affecting any comparison between approaches. Three of them are diagnostics "
        "where that is the intended result; the rest are specialists with no matching listing in an "
        "18-listing set. Their nDCG is unaffected, since a pool of grade-1 pairs still has a non-zero "
        "ideal DCG.",
        "",
        "**The education gate is deliberately not harmonised.** The rubric caps an unmet education "
        "requirement at grade 1 (rubric.md section 4); M4, M5 and M6 return a hard 0 and exclude the "
        "pair. This disagreement is intentional and was not reconciled: it is what lets the data show "
        "whether a hard structural gate **over-penalises** relative to how a recruiter would actually "
        "read an unmet formal requirement. Watch `mkt-education-gate-fail-1` and "
        "`ops-logistics-senior-1` in `diagnostics.md` for the effect.",
        "",
        "**On the ESCO vocabulary gap.** CAMS's manual skill supplement has partly closed the "
        "framework-name gap in the ESCO taxonomy. `Spring Boot`, `React`, `Docker`, `Kubernetes`, "
        "`CI/CD` and `DevOps` are all in the seeded vocabulary today - so the `esco-gap` fixture "
        "tests **under-claiming a skill that exists**, not vocabulary absence. The `vocabulary-gap` "
        "fixture tests the remaining, narrower case: `Scrum` is genuinely absent, so no structured "
        "skill could express it and only semantic evidence can reach it. Any claim that the "
        "vocabulary cannot express modern framework names should be narrowed accordingly.",
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
                lines.append(f"**The interval straddles zero, so this is INCONCLUSIVE.** "
                             f"{winner['approach']} has the highest mean, but the benchmark cannot "
                             f"distinguish it from {runner_up['approach']}, and reporting it as a win "
                             f"would overstate what 40 query candidates can show. See the pairwise "
                             f"table below for which differences, if any, are conclusive.")
    lines += ["",
              "See `diagnostics.md` for the per-fixture traces, and read them before trusting this line -",
              "a diagnostic that contradicts the aggregate is the more interesting result.",
              ""]

    lines += ["## Pairwise differences (paired bootstrap)", "",
              f"{boot['iterations']} iterations, seed {boot['seed']}, resampling the "
              f"{boot['n_candidates']} query candidates with replacement. Every approach is scored on "
              "the *same* resample in each iteration, so the shared \"this resample was easy\" effect "
              "cancels in the difference. `diff` is B minus A: positive means B scored higher.",
              "",
              "**A difference whose interval contains zero is inconclusive, not a win.**",
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
                  "M4, M5 and M6 return a hard 0 and exclude a pair when a gate fails. The rubric does "
                  "not agree with them about education: it caps an unmet education requirement at "
                  "grade 1 rather than 0 (rubric.md section 4), and that disagreement was left "
                  "deliberately unreconciled so the cost could be measured instead of argued about. "
                  "Counted below on `" + str(gates["approach"]) + "` (all three hybrids gate identically).",
                  "",
                  "| Gate | Pairs excluded | Of those, labelled relevant (grade >= 1) | Reading |",
                  "|---|---|---|---|"]
        for kind in sorted(gates["counts"]):
            bucket = gates["counts"][kind]
            if bucket["relevant"] == 0:
                reading = "Agrees with the labels on every pair it excludes"
            else:
                reading = f"**Over-penalises: {bucket['relevant']} pair(s) the rubric grades >= 1 are forced to 0**"
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
                      "This is a real and quantified cost of the hard gate, and it is part of why the "
                      "hybrid rungs score below the embedding rungs here. It is **not** the whole "
                      "story - see the ablation deltas - and this benchmark deliberately does not "
                      "re-run the hybrids with a softened gate, because choosing a gate policy after "
                      "seeing which one scores better is tuning on the test set. Porting a soft "
                      "education score and re-measuring is a next-phase question with the "
                      "measurement now in hand.",
                      ""]

    lines += [
        "## Stated limitations",
        "",
        "- This is a **synthetic benchmark**. Its labels encode the designer's model of relevance.",
        "  It supports *relative* comparison between approaches on identical data; it is not a claim",
        "  about absolute real-world matching quality.",
        "- Labels are authored **by construction**, from candidate and listing content alone, with no",
        "  algorithm output consulted. `audit_agreement.md` reports how well an independent blind",
        "  grading of a ~10% sample agrees with them - that figure, not this one, is the honest answer",
        "  to \"how do you know your labels are right\".",
        "- No number here rests on a single arbitrary constant without a check: see `sensitivity.md`",
        "  for the TAU and weight sweep.",
        "- Overlapping experience entries are summed rather than de-overlapped (see `total_years`).",
        "- **The seniority labels are contaminated, not convergent.** The rubric was authored with",
        "  the algorithm design already in hand, so M4+'s asymmetric",
        "  `seniority_score` and its `LEVEL_BANDS` had a direct path into the ground truth. Any",
        "  M3 -> M4 gain is partly the labels agreeing with M4's design rather than evidence that M4",
        "  is better, and must be read that way. M4 -> M5 -> M6 is unaffected: all three share the",
        "  identical seniority component. See rubric.md section 8.2.",
        "- The bootstrap quantifies sampling variability over these 40 candidates only. It says",
        "  nothing about whether the fixture set is representative, and a tight interval around a",
        "  contaminated difference is still a contaminated difference.",
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
