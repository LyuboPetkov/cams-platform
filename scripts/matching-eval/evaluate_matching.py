"""
CAMS Phase 18 — evaluate the embedding-based matcher against the rule-based
baseline over the curated dataset, reporting Precision@5 and nDCG@5.

Connects to Postgres directly (psycopg) and reads embeddings/skills/fields
straight out of the tables seed_dataset.py already populated — no dependency
on the backend or embedding-service being up, per Phase 18 brief Section 3.4.

Usage (after seed_dataset.py has run once and labels.json has real grades):
    set -a; source .env; set +a
    python evaluate_matching.py

Any (candidateKey, listingKey) pair absent from labels.json is treated as
relevance 0 when scoring, not skipped and not excluded from the ranking —
standard practice for a closed hand-graded set (Phase 18 brief Section 3.7).
"""

import csv
import json
import math
import os
import sys
from pathlib import Path

import numpy as np
import psycopg

import rule_based_baseline

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = os.environ.get("DB_PORT", "5433")
DB_USER = os.environ.get("DB_USER", "cams_user")
DB_NAME = os.environ.get("DB_NAME", "cams_db")
DB_PASSWORD = os.environ.get("DB_PASSWORD")

DATASET_PATH = Path(__file__).parent / "dataset.json"
LABELS_PATH = Path(__file__).parent / "labels.json"
RESULTS_PATH = Path(__file__).parent / "results.csv"

K = 5


def die(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(1)


def parse_vector(raw) -> np.ndarray | None:
    """pgvector comes back over psycopg as its text form, '[0.1,0.2,...]'."""
    if raw is None:
        return None
    return np.array([float(x) for x in raw.strip("[]").split(",")], dtype=np.float64)


def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    return float(np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b)))


def load_candidate(cur, email: str, key: str) -> dict:
    cur.execute(
        """
        SELECT cp.id, cp.headline, cp.location, cp.open_to_remote, cp.embedding::text
        FROM candidate_profiles cp
        JOIN users u ON u.id = cp.user_id
        WHERE u.email = %s
        """,
        (email,),
    )
    row = cur.fetchone()
    if row is None:
        die(f"candidate key '{key}' (email {email}) does not resolve to a candidate_profiles row — run seed_dataset.py first")
    profile_id, headline, location, open_to_remote, embedding_text = row

    cur.execute(
        "SELECT skill_id FROM candidate_profile_skills WHERE candidate_profile_id = %s",
        (profile_id,),
    )
    skill_ids = {r[0] for r in cur.fetchall()}

    embedding = parse_vector(embedding_text)
    if embedding is None:
        die(f"candidate key '{key}' has no embedding yet — seed_dataset.py should have polled for this")

    return {
        "key": key,
        "headline": headline,
        "location": location,
        "open_to_remote": open_to_remote,
        "skill_ids": skill_ids,
        "embedding": embedding,
    }


def load_listing(cur, title: str, company_name: str, key: str) -> dict:
    cur.execute(
        """
        SELECT jl.id, jl.title, jl.location, jl.remote, jl.embedding::text
        FROM job_listings jl
        JOIN companies c ON c.id = jl.company_id
        WHERE jl.title = %s AND c.name = %s
        """,
        (title, company_name),
    )
    row = cur.fetchone()
    if row is None:
        die(f"listing key '{key}' (title '{title}' @ '{company_name}') does not resolve to a job_listings row — run seed_dataset.py first")
    listing_id, title, location, remote, embedding_text = row

    cur.execute(
        "SELECT skill_id FROM job_listing_skills WHERE job_listing_id = %s",
        (listing_id,),
    )
    skill_ids = {r[0] for r in cur.fetchall()}

    embedding = parse_vector(embedding_text)
    if embedding is None:
        die(f"listing key '{key}' has no embedding yet — seed_dataset.py should have polled for this")

    return {
        "key": key,
        "title": title,
        "location": location,
        "remote": remote,
        "skill_ids": skill_ids,
        "embedding": embedding,
    }


def grade_of(labels_by_pair: dict, candidate_key: str, listing_key: str) -> int:
    # Absent pairs default to 0 — Phase 18 brief Section 3.7, stated in code
    # per the brief's explicit instruction, not left implicit.
    return labels_by_pair.get((candidate_key, listing_key), 0)


def dcg(grades: list[int]) -> float:
    return sum((2 ** g - 1) / math.log2(rank + 1) for rank, g in enumerate(grades, start=1))


def precision_at_k(ranked_keys: list[str], labels_by_pair: dict, candidate_key: str, k: int) -> float:
    top_k = ranked_keys[:k]
    relevant = sum(1 for lk in top_k if grade_of(labels_by_pair, candidate_key, lk) >= 1)
    return relevant / k


def ndcg_at_k(ranked_keys: list[str], labels_by_pair: dict, candidate_key: str, k: int, all_listing_keys: list[str]) -> float:
    top_k_grades = [grade_of(labels_by_pair, candidate_key, lk) for lk in ranked_keys[:k]]
    actual_dcg = dcg(top_k_grades)

    graded_grades = sorted(
        (grade_of(labels_by_pair, candidate_key, lk) for lk in all_listing_keys),
        reverse=True,
    )
    ideal_dcg = dcg(graded_grades[:k])

    if ideal_dcg == 0:
        return 0.0
    return actual_dcg / ideal_dcg


def rank_by_embedding(candidate: dict, listings: list[dict]) -> list[str]:
    scored = [(cosine_similarity(candidate["embedding"], listing["embedding"]), listing["key"]) for listing in listings]
    scored.sort(key=lambda pair: pair[0], reverse=True)
    return [key for _, key in scored]


def rank_by_rule_based(candidate: dict, listings: list[dict]) -> list[str]:
    candidate_dict = {
        "headline": candidate["headline"],
        "location": candidate["location"],
        "open_to_remote": candidate["open_to_remote"],
        "skill_ids": candidate["skill_ids"],
    }
    scored = []
    for listing in listings:
        listing_dict = {
            "title": listing["title"],
            "location": listing["location"],
            "remote": listing["remote"],
            "skill_ids": listing["skill_ids"],
        }
        scored.append((rule_based_baseline.score(candidate_dict, listing_dict), listing["key"]))
    scored.sort(key=lambda pair: pair[0], reverse=True)
    return [key for _, key in scored]


def main() -> None:
    if not DB_PASSWORD:
        die("DB_PASSWORD is not set (see .env)")

    if not LABELS_PATH.exists():
        die(f"{LABELS_PATH} does not exist — author it first (see README.md)")

    dataset = json.loads(DATASET_PATH.read_text(encoding="utf-8"))
    labels = json.loads(LABELS_PATH.read_text(encoding="utf-8"))

    labels_by_pair = {
        (g["candidateKey"], g["listingKey"]): g["grade"] for g in labels["grades"]
    }
    query_candidate_keys = sorted({g["candidateKey"] for g in labels["grades"]})
    if not query_candidate_keys:
        die("labels.json has no grades — nothing to evaluate")

    candidates_by_key = {c["key"]: c for c in dataset["candidates"]}
    companies_by_key = {c["key"]: c["name"] for c in dataset["companies"]}

    conn = psycopg.connect(host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASSWORD, dbname=DB_NAME)

    with conn.cursor() as cur:
        listings = [
            load_listing(cur, l["title"], companies_by_key[l["companyKey"]], l["key"])
            for l in dataset["listings"]
        ]
        all_listing_keys = [l["key"] for l in listings]

        rows = []
        for candidate_key in query_candidate_keys:
            if candidate_key not in candidates_by_key:
                die(f"labels.json references unknown candidateKey '{candidate_key}'")
            fixture = candidates_by_key[candidate_key]
            candidate = load_candidate(cur, fixture["email"], candidate_key)

            embedding_ranking = rank_by_embedding(candidate, listings)
            rule_based_ranking = rank_by_rule_based(candidate, listings)

            row = {
                "candidateKey": candidate_key,
                "embedding_precision_at_5": precision_at_k(embedding_ranking, labels_by_pair, candidate_key, K),
                "embedding_ndcg_at_5": ndcg_at_k(embedding_ranking, labels_by_pair, candidate_key, K, all_listing_keys),
                "rule_based_precision_at_5": precision_at_k(rule_based_ranking, labels_by_pair, candidate_key, K),
                "rule_based_ndcg_at_5": ndcg_at_k(rule_based_ranking, labels_by_pair, candidate_key, K, all_listing_keys),
            }
            rows.append(row)

    conn.close()

    header = f"{'candidateKey':<24} {'emb P@5':>8} {'emb nDCG@5':>11} {'rule P@5':>9} {'rule nDCG@5':>12}"
    print(header)
    print("-" * len(header))
    for row in rows:
        print(
            f"{row['candidateKey']:<24} "
            f"{row['embedding_precision_at_5']:>8.3f} "
            f"{row['embedding_ndcg_at_5']:>11.3f} "
            f"{row['rule_based_precision_at_5']:>9.3f} "
            f"{row['rule_based_ndcg_at_5']:>12.3f}"
        )

    def mean(field: str) -> float:
        return sum(r[field] for r in rows) / len(rows)

    print("-" * len(header))
    print(
        f"{'MEAN':<24} "
        f"{mean('embedding_precision_at_5'):>8.3f} "
        f"{mean('embedding_ndcg_at_5'):>11.3f} "
        f"{mean('rule_based_precision_at_5'):>9.3f} "
        f"{mean('rule_based_ndcg_at_5'):>12.3f}"
    )

    with open(RESULTS_PATH, "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=[
            "candidateKey", "embedding_precision_at_5", "embedding_ndcg_at_5",
            "rule_based_precision_at_5", "rule_based_ndcg_at_5",
        ])
        writer.writeheader()
        writer.writerows(rows)
        writer.writerow({
            "candidateKey": "MEAN",
            "embedding_precision_at_5": mean("embedding_precision_at_5"),
            "embedding_ndcg_at_5": mean("embedding_ndcg_at_5"),
            "rule_based_precision_at_5": mean("rule_based_precision_at_5"),
            "rule_based_ndcg_at_5": mean("rule_based_ndcg_at_5"),
        })

    print(f"\nWrote {RESULTS_PATH}")


if __name__ == "__main__":
    main()
