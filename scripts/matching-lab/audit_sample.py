"""
Blind re-grade of the labels, which answers the obvious objection that the same
person designed the fixtures and graded them. Emits a fixed-seed sample of
candidate and listing text with no grades or scores attached, ready to be graded
again from scratch, then reports how closely the two passes agree.

    python audit_sample.py --emit     # write results/audit_worksheet.csv
    ... fill in the human_grade column by hand ...
    python audit_sample.py --score    # write results/audit_agreement.md

Cohen's kappa and Spearman rho are implemented here in numpy rather than pulling
in scipy or scikit-learn for two functions.
"""

import argparse
import csv
import json
import random
import sys
from datetime import date
from pathlib import Path

import numpy as np

from components import LEVEL_BANDS, meets_education, total_years

BASE = Path(__file__).parent
FIXTURES = BASE / "fixtures"
RESULTS = BASE / "results"
WORKSHEET = RESULTS / "audit_worksheet.csv"
AGREEMENT = RESULTS / "audit_agreement.md"

DEFAULT_SEED = 35
MIN_GRADE, MAX_GRADE = 0, 3

# 60 of 720, stratified rather than uniform. A uniform draw is mostly cross-area
# pairs graded 0 by rule, which are trivial to agree on. These strata favour the
# hard pairs, with a cross-area quota kept so the easy end is still represented.
SAME_AREA_TARGET = 40
CROSS_AREA_TARGET = 20

# Within the same-area quota, fill the hardest strata first.
SENIORITY_MISMATCH_TARGET = 18
EDUCATION_CAPPED_TARGET = 10


def die(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(1)


def load_json(path: Path) -> dict:
    if not path.exists():
        die(f"{path} does not exist")
    return json.loads(path.read_text(encoding="utf-8"))


def render_candidate(candidate: dict) -> str:
    """
    Everything a grader needs and nothing that would prime them: no key, no area
    tag, no diagnostic tag, no grade.
    """
    lines = [
        f"HEADLINE: {candidate.get('headline') or '-'}",
        f"ABOUT: {candidate.get('description') or '-'}",
        f"LOCATION: {candidate.get('location') or '-'} | open to remote: {candidate.get('openToRemote')}",
        f"SKILLS: {', '.join(candidate.get('skills') or []) or '-'}",
    ]
    for experience in candidate.get("experiences", []):
        end = experience.get("endDate") or "present"
        lines.append(f"EXPERIENCE: {experience.get('roleTitle')} ({experience.get('startDate')} - {end}): "
                     f"{experience.get('description') or '-'}")
    for education in candidate.get("educations", []):
        end = education.get("endDate") or "ongoing"
        lines.append(f"EDUCATION: {education.get('level')} at {education.get('institutionName')} "
                     f"in {education.get('specialty') or '-'} ({education.get('startDate')} - {end})")
    for course in candidate.get("courses", []):
        lines.append(f"COURSE: {course.get('name')} ({course.get('issuingOrganization')})")
    return "\n".join(lines)


def render_listing(listing: dict) -> str:
    return "\n".join([
        f"TITLE: {listing.get('title')}",
        f"COMPANY: {listing.get('companyName') or '-'}",
        f"DESCRIPTION: {listing.get('description') or '-'}",
        f"LOCATION: {listing.get('location') or '-'} | remote: {listing.get('remote')}",
        f"LEVEL: {listing.get('level') or '-'}",
        f"REQUIRED EDUCATION: {listing.get('requiredEducationLevel') or 'none'}",
        f"REQUIRED SKILLS: {', '.join(listing.get('skills') or []) or '-'}",
    ])


def band_index(level) -> int | None:
    order = ["INTERN", "JUNIOR", "MID", "SENIOR"]
    return order.index(level) if level in order else None


def candidate_band(candidate: dict) -> int | None:
    """
    Which band the candidate's total tenure falls in. Total rather than relevant,
    because this only picks strata and never grades anything.
    """
    years = total_years(candidate.get("experiences", []), date.today())
    for index, level in enumerate(["INTERN", "JUNIOR", "MID", "SENIOR"]):
        low, high = LEVEL_BANDS[level]
        if low <= years <= high:
            return index
    return None


def classify(candidate: dict, listing: dict) -> str:
    """
    Which stratum a pair belongs to, computed from the fixtures only and never
    from labels.json, so building the worksheet cannot leak a grade into the
    selection.
    """
    if candidate.get("area") != listing.get("area"):
        return "cross-area"
    if not meets_education(candidate.get("educations", []), listing.get("requiredEducationLevel")):
        return "education-capped"
    listing_band = band_index(listing.get("level"))
    own_band = candidate_band(candidate)
    if listing_band is not None and own_band is not None and own_band != listing_band:
        return "seniority-mismatch"
    return "same-area-other"


def emit(seed: int) -> None:
    candidates = load_json(FIXTURES / "candidates.json")["candidates"]
    listings = load_json(FIXTURES / "listings.json")["listings"]

    pairs = [(c, l) for c in candidates for l in listings]
    # Sorted first, then sampled from a seeded RNG, so the sample depends on the
    # seed alone and not on JSON key order.
    pairs.sort(key=lambda pair: (pair[0]["key"], pair[1]["key"]))

    strata = {}
    for candidate, listing in pairs:
        strata.setdefault(classify(candidate, listing), []).append((candidate, listing))

    rng = random.Random(seed)

    def take(name, wanted):
        pool = strata.get(name, [])
        return rng.sample(pool, min(wanted, len(pool)))

    sample = []
    sample += take("seniority-mismatch", SENIORITY_MISMATCH_TARGET)
    sample += take("education-capped", EDUCATION_CAPPED_TARGET)
    sample += take("same-area-other", max(0, SAME_AREA_TARGET - len(sample)))
    sample += take("cross-area", CROSS_AREA_TARGET)

    counts = {name: sum(1 for c, l in sample if classify(c, l) == name) for name in sorted(strata)}
    sample.sort(key=lambda pair: (pair[0]["key"], pair[1]["key"]))

    RESULTS.mkdir(exist_ok=True)
    with open(WORKSHEET, "w", newline="", encoding="utf-8") as fh:
        writer = csv.writer(fh)
        writer.writerow(["# seed", seed, "# sample", f"{len(sample)}/{len(pairs)}",
                         "# scale", "0=not relevant, 1=weak/partial, 2=good, 3=excellent"])
        writer.writerow(["# strata", json.dumps(counts, separators=(",", ":")),
                         "# note", "Stratified, not uniform. Favours seniority-mismatch and "
                                   "education-capped pairs, which are the hardest cases. Agreement "
                                   "measured on this sample is conservative: a uniform sample would "
                                   "score higher, being mostly cross-area zeroes."])
        writer.writerow(["candidateKey", "listingKey", "candidate_text", "listing_text", "human_grade"])
        for candidate, listing in sample:
            writer.writerow([
                candidate["key"], listing["key"],
                render_candidate(candidate), render_listing(listing),
                "",
            ])

    print(f"Wrote {WORKSHEET.relative_to(BASE)} - {len(sample)} of {len(pairs)} pairs (seed {seed}).")
    print(f"Strata: {counts}")
    print("Fill in the human_grade column (0-3) without looking at labels.json or any results, "
          "then run: python audit_sample.py --score")


def read_worksheet() -> tuple[list, int]:
    if not WORKSHEET.exists():
        die(f"{WORKSHEET} does not exist - run `python audit_sample.py --emit` first")

    with open(WORKSHEET, newline="", encoding="utf-8") as fh:
        rows = list(csv.reader(fh))

    header_row = rows[0]
    seed = int(header_row[1]) if len(header_row) > 1 and header_row[0].startswith("# seed") else DEFAULT_SEED

    # Banner rows start with "#". The first non-banner row is the column header,
    # found rather than indexed so an extra banner line cannot eat a data row.
    # Zipped by hand because csv.DictReader wants raw lines, not lists.
    header_index = next(i for i, r in enumerate(rows) if r and not r[0].startswith("#"))
    header = rows[header_index]
    graded = []
    for values in rows[header_index + 1:]:
        if not values:
            continue
        row = dict(zip(header, values))
        raw = (row.get("human_grade") or "").strip()
        if raw == "":
            continue
        try:
            grade = int(raw)
        except ValueError:
            die(f"human_grade {raw!r} for ({row['candidateKey']}, {row['listingKey']}) is not an integer")
        if not MIN_GRADE <= grade <= MAX_GRADE:
            die(f"human_grade {grade} for ({row['candidateKey']}, {row['listingKey']}) is outside 0-3")
        graded.append((row["candidateKey"], row["listingKey"], grade))

    return graded, seed


def quadratic_weighted_kappa(a: np.ndarray, b: np.ndarray) -> float:
    """
    Cohen's kappa with quadratic weights over the 0-3 grade scale.

    Weighted rather than plain because the scale is ordinal: grading a pair 2
    where the label says 3 is a much smaller disagreement than grading it 0, and
    an unweighted kappa counts both as simply wrong.

    1.0 is perfect agreement, 0.0 is chance, negative is worse than chance.
    """
    categories = list(range(MIN_GRADE, MAX_GRADE + 1))
    n = len(categories)

    observed = np.zeros((n, n), dtype=np.float64)
    for x, y in zip(a, b):
        observed[x - MIN_GRADE, y - MIN_GRADE] += 1
    observed /= observed.sum()

    hist_a = np.array([np.sum(a == c) for c in categories], dtype=np.float64) / len(a)
    hist_b = np.array([np.sum(b == c) for c in categories], dtype=np.float64) / len(b)
    expected = np.outer(hist_a, hist_b)

    indices = np.arange(n)
    weights = (indices[:, None] - indices[None, :]) ** 2 / (n - 1) ** 2

    denominator = float(np.sum(weights * expected))
    if denominator == 0.0:
        # Both raters used exactly one category. Kappa is undefined; agreement
        # is total or nil depending on whether it is the same category.
        return 1.0 if np.array_equal(a, b) else 0.0
    return 1.0 - float(np.sum(weights * observed)) / denominator


def _average_ranks(values: np.ndarray) -> np.ndarray:
    """Ranks with ties averaged, which is what Spearman requires."""
    order = np.argsort(values, kind="mergesort")
    ranks = np.empty(len(values), dtype=np.float64)
    ranks[order] = np.arange(1, len(values) + 1, dtype=np.float64)

    for value in np.unique(values):
        mask = values == value
        ranks[mask] = ranks[mask].mean()
    return ranks


def spearman_rho(a: np.ndarray, b: np.ndarray) -> float:
    """Pearson correlation of the average ranks. NaN-safe on zero variance."""
    rank_a = _average_ranks(a.astype(np.float64))
    rank_b = _average_ranks(b.astype(np.float64))
    if rank_a.std() == 0.0 or rank_b.std() == 0.0:
        return float("nan")
    return float(np.corrcoef(rank_a, rank_b)[0, 1])


def interpret_kappa(kappa: float) -> str:
    # Landis & Koch (1977) bands, the conventional reading of a kappa value.
    if kappa < 0.0:
        return "worse than chance"
    if kappa < 0.20:
        return "slight"
    if kappa < 0.40:
        return "fair"
    if kappa < 0.60:
        return "moderate"
    if kappa < 0.80:
        return "substantial"
    return "almost perfect"


def score() -> None:
    graded, seed = read_worksheet()
    if not graded:
        die(f"{WORKSHEET.name} has no filled-in human_grade values yet")

    labels = load_json(FIXTURES / "labels.json")
    labels_by_pair = {(g["candidateKey"], g["listingKey"]): g["grade"] for g in labels["grades"]}

    human, construction, missing = [], [], []
    for candidate_key, listing_key, grade in graded:
        if (candidate_key, listing_key) not in labels_by_pair:
            missing.append((candidate_key, listing_key))
        human.append(grade)
        # Absent pairs default to 0, the same convention the metrics use.
        construction.append(labels_by_pair.get((candidate_key, listing_key), 0))

    human_array = np.array(human)
    construction_array = np.array(construction)

    kappa = quadratic_weighted_kappa(construction_array, human_array)
    rho = spearman_rho(construction_array, human_array)
    exact = float(np.mean(human_array == construction_array))
    within_one = float(np.mean(np.abs(human_array - construction_array) <= 1))

    lines = [
        "# Label audit: agreement between the original labels and a blind re-grade",
        "",
        "Written by `audit_sample.py --score`, from the worksheet emitted by `--emit` and graded",
        "by hand in between.",
        "",
        f"Sample: {len(graded)} pairs, drawn with seed {seed}.  ",
        "Graded from candidate and listing text alone. No original grade, no score and no algorithm",
        "output appeared on the worksheet.",
        "",
        "The sample is stratified rather than uniform. It favours seniority-mismatch pairs, which",
        "are the part of the ground truth most exposed to the algorithm design (rubric.md 8.2), and",
        "education-capped pairs, where the rubric disagrees with the M4 hard gate on purpose. A",
        "cross-area quota keeps the easy end of the scale represented. Agreement measured this way",
        "is conservative: a uniform 10% draw would be about 83% cross-area zeroes and would score",
        "higher without showing much.",
        "",
        "| Statistic | Value |",
        "|---|---|",
        f"| Cohen's kappa (quadratic weights) | {kappa:.3f} |",
        f"| Spearman rho | {rho:.3f} |",
        f"| Exact agreement | {exact:.1%} |",
        f"| Agreement within 1 grade | {within_one:.1%} |",
        "",
        "## Reading",
        "",
        f"A quadratic-weighted kappa of {kappa:.3f} counts as {interpret_kappa(kappa)} agreement on the",
        "usual Landis & Koch bands. Kappa corrects for the agreement chance alone would produce, and",
        "the quadratic weighting means a one-grade disagreement counts for much less than a",
        f"three-grade one, which suits an ordinal 0-3 scale. Spearman rho ({rho:.3f}) says the same",
        "thing about ordering rather than about exact values.",
        "",
        "This does not make the benchmark objective. The sample was graded by the same person who",
        "designed the fixtures, only without sight of the original grades. What it does is bound how",
        "much of the result could be an artefact of one particular grading pass.",
        "",
    ]

    if missing:
        lines += [f"> {len(missing)} audited pair(s) have no row in `labels.json` and were compared "
                  f"against the default grade of 0.", ""]

    AGREEMENT.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print(f"kappa (quadratic) = {kappa:.3f} ({interpret_kappa(kappa)}), spearman rho = {rho:.3f}, "
          f"exact = {exact:.1%}, within 1 = {within_one:.1%}")
    print(f"Wrote {AGREEMENT.relative_to(BASE)}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Emit or score the blind label-audit worksheet.")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--emit", action="store_true", help="write the blind worksheet")
    group.add_argument("--score", action="store_true", help="score a filled-in worksheet")
    parser.add_argument("--seed", type=int, default=DEFAULT_SEED, help=f"sample seed (default {DEFAULT_SEED})")
    args = parser.parse_args()

    if args.emit:
        emit(args.seed)
    else:
        score()


if __name__ == "__main__":
    main()
