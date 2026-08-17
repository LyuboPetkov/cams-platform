"""
CAMS Phase 18 — seed the curated matching-evaluation dataset.

Walks dataset.json and creates every candidate/employer/company/listing
through the real running API (never raw SQL), so the existing async
embedding pipeline (Phase 16) computes a real embedding for each row
exactly as it would for any other save. See Phase 18 brief Section 3.5.

Requires the local stack up first:
    set -a; source .env; set +a
    docker compose -f docker-compose.local.yml up --build db embedding-service backend

Then, in another terminal, with the same env loaded:
    python -m venv .venv && .venv\\Scripts\\activate   (or source .venv/bin/activate)
    pip install -r requirements.txt
    python seed_dataset.py

Idempotent by design (Phase 18 brief Section 3.5's "decide and document"
requirement): re-running against an already-seeded database detects
existing accounts (register -> 409 -> falls back to login), an
already-approved company (GET /companies/me -> 200 -> skips the
employer-request approval flow), and already-created listings (matched
by title within GET /job-listings/mine -> skipped) rather than
duplicating any row. Candidate/company profile fields are always
re-applied via their idempotent PUT endpoints regardless.

Env vars (same convention as scripts/smoke-test-phase17.sh):
    API              backend base URL, default http://localhost:8080
    CAMS_ADMIN_EMAIL, CAMS_ADMIN_PASSWORD   an existing ADMIN account
    DB_PASSWORD, DB_HOST, DB_PORT, DB_USER, DB_NAME   for the post-seed
                     embedding_updated_at poll (defaults: localhost,
                     5432, cams_user, cams_db)
"""

import json
import os
import sys
import time
from pathlib import Path

import psycopg
import requests

API = os.environ.get("API", "http://localhost:8080")
ADMIN_EMAIL = os.environ.get("CAMS_ADMIN_EMAIL")
ADMIN_PASSWORD = os.environ.get("CAMS_ADMIN_PASSWORD")

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = os.environ.get("DB_PORT", "5432")
DB_USER = os.environ.get("DB_USER", "cams_user")
DB_NAME = os.environ.get("DB_NAME", "cams_db")
DB_PASSWORD = os.environ.get("DB_PASSWORD")

DATASET_PATH = Path(__file__).parent / "dataset.json"

# Fixed fixture password for every eval-* account. Not a real credential —
# these are throwaway local-dev accounts, same spirit as smoke-test-phase17.sh's
# hardcoded "password123".
EMPLOYER_PASSWORD = "eval-fixture-password-not-real"


def die(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(1)


def register_or_login(email: str, password: str, full_name: str, company_fields: dict | None = None) -> str:
    """Returns a JWT. Registers a fresh account, or logs in if it already exists."""
    body = {"email": email, "password": password, "fullName": full_name}
    if company_fields:
        body.update(company_fields)

    resp = requests.post(f"{API}/api/auth/register", json=body)
    if resp.status_code == 201:
        return resp.json()["token"]
    if resp.status_code == 409:
        resp = requests.post(f"{API}/api/auth/login", json={"email": email, "password": password})
        if resp.status_code != 200:
            die(f"account {email} exists but login failed (HTTP {resp.status_code}): {resp.text}")
        return resp.json()["token"]
    die(f"registering {email} failed (HTTP {resp.status_code}): {resp.text}")


def auth_headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


def ensure_company_approved(employer_token: str, admin_token: str, company_name: str) -> int:
    """Returns the company id, approving the pending employer request if needed."""
    resp = requests.get(f"{API}/api/companies/me", headers=auth_headers(employer_token))
    if resp.status_code == 200:
        return resp.json()["id"]
    if resp.status_code != 403:
        die(f"unexpected GET /companies/me status for {company_name} (HTTP {resp.status_code}): {resp.text}")

    resp = requests.get(f"{API}/api/employer-requests/me", headers=auth_headers(employer_token))
    if resp.status_code != 200:
        die(f"could not find pending employer request for {company_name} (HTTP {resp.status_code}): {resp.text}")
    request_id = resp.json()["id"]

    resp = requests.post(f"{API}/api/admin/employer-requests/{request_id}/approve", headers=auth_headers(admin_token))
    if resp.status_code != 200:
        die(f"could not approve employer request for {company_name} (HTTP {resp.status_code}): {resp.text}")

    resp = requests.get(f"{API}/api/companies/me", headers=auth_headers(employer_token))
    if resp.status_code != 200:
        die(f"company still unreachable for {company_name} after approval (HTTP {resp.status_code}): {resp.text}")
    return resp.json()["id"]


def resolve_skill_ids(token: str, names: list[str], cache: dict[str, int]) -> list[int]:
    ids = []
    for name in names:
        if name in cache:
            ids.append(cache[name])
            continue

        resp = requests.get(f"{API}/api/skills", params={"search": name}, headers=auth_headers(token))
        if resp.status_code != 200:
            die(f"skill search for '{name}' failed (HTTP {resp.status_code}): {resp.text}")

        exact = [s for s in resp.json() if s["name"].lower() == name.lower()]
        if not exact:
            die(f"skill '{name}' does not exist in the seeded vocabulary — dataset.json is out of sync")
        cache[name] = exact[0]["id"]
        ids.append(exact[0]["id"])
    return ids


def seed_companies_and_employers(dataset: dict, admin_token: str) -> dict[str, dict]:
    """Returns companyKey -> {"token": employer JWT, "company_id": int}."""
    result = {}
    for company in dataset["companies"]:
        key = company["key"]
        email = f"eval-employer-{key}@cams-eval.local"
        print(f"  employer/company '{key}' ({email})")

        token = register_or_login(
            email,
            EMPLOYER_PASSWORD,
            f"Eval Employer ({key})",
            company_fields={
                "companyName": company["name"],
                "companyDescription": company.get("description"),
                "companyWebsite": company.get("website"),
                "companyLocation": company.get("location"),
            },
        )
        company_id = ensure_company_approved(token, admin_token, company["name"])
        result[key] = {"token": token, "company_id": company_id}
    return result


def seed_candidates(dataset: dict, skill_cache: dict[str, int]) -> None:
    for candidate in dataset["candidates"]:
        key = candidate["key"]
        print(f"  candidate '{key}' ({candidate['email']})")

        token = register_or_login(candidate["email"], candidate["password"], f"Eval Candidate ({key})")

        resp = requests.put(
            f"{API}/api/candidate-profiles/me",
            headers=auth_headers(token),
            json={
                "headline": candidate["headline"],
                "description": candidate["description"],
                "location": candidate["location"],
                "openToRemote": candidate["openToRemote"],
                "flexibleHours": candidate["flexibleHours"],
            },
        )
        if resp.status_code != 200:
            die(f"updating profile for {key} failed (HTTP {resp.status_code}): {resp.text}")

        # Any authenticated token can search skills — use the candidate's own.
        skill_ids = resolve_skill_ids(token, candidate["skills"], skill_cache)
        resp = requests.put(
            f"{API}/api/candidate-profiles/me/skills",
            headers=auth_headers(token),
            json={"skillIds": skill_ids},
        )
        if resp.status_code != 200:
            die(f"setting skills for {key} failed (HTTP {resp.status_code}): {resp.text}")


def seed_listings(dataset: dict, companies: dict[str, dict], skill_cache: dict[str, int]) -> None:
    for listing in dataset["listings"]:
        key = listing["key"]
        company = companies.get(listing["companyKey"])
        if company is None:
            die(f"listing '{key}' references unknown companyKey '{listing['companyKey']}'")
        token = company["token"]

        resp = requests.get(f"{API}/api/job-listings/mine", headers=auth_headers(token))
        if resp.status_code != 200:
            die(f"listing /mine lookup failed for '{key}' (HTTP {resp.status_code}): {resp.text}")
        existing_titles = {listing_row["title"] for listing_row in resp.json()}
        if listing["title"] in existing_titles:
            print(f"  listing '{key}' already exists — skipping")
            continue

        print(f"  listing '{key}' ({listing['title']})")
        skill_ids = resolve_skill_ids(token, listing["skills"], skill_cache)
        resp = requests.post(
            f"{API}/api/job-listings",
            headers=auth_headers(token),
            json={
                "title": listing["title"],
                "description": listing["description"],
                "location": listing["location"],
                "remote": listing["remote"],
                "level": listing["level"],
                "skillIds": skill_ids,
            },
        )
        if resp.status_code != 201:
            die(f"creating listing '{key}' failed (HTTP {resp.status_code}): {resp.text}")


def poll_embeddings(dataset: dict) -> None:
    if not DB_PASSWORD:
        die("DB_PASSWORD is not set — cannot poll embedding_updated_at. Add it to .env and re-source it.")

    conn = psycopg.connect(
        host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASSWORD, dbname=DB_NAME
    )
    candidate_emails = [c["email"] for c in dataset["candidates"]]
    listing_titles = [(l["title"], next(c["name"] for c in dataset["companies"] if c["key"] == l["companyKey"]))
                       for l in dataset["listings"]]

    deadline = time.time() + 120
    with conn.cursor() as cur:
        while time.time() < deadline:
            cur.execute(
                """
                SELECT u.email FROM candidate_profiles cp
                JOIN users u ON u.id = cp.user_id
                WHERE u.email = ANY(%s) AND cp.embedding_updated_at IS NULL
                """,
                (candidate_emails,),
            )
            pending_candidates = {row[0] for row in cur.fetchall()}

            cur.execute(
                """
                SELECT jl.title, c.name FROM job_listings jl
                JOIN companies c ON c.id = jl.company_id
                WHERE jl.embedding_updated_at IS NULL
                """
            )
            pending_listing_rows = {(row[0], row[1]) for row in cur.fetchall()}
            pending_listings = [t for t in listing_titles if t in pending_listing_rows]

            if not pending_candidates and not pending_listings:
                print("  all candidate and listing embeddings are ready")
                conn.close()
                return

            print(f"  waiting on {len(pending_candidates)} candidate(s), {len(pending_listings)} listing(s)...")
            time.sleep(2)

    conn.close()
    die(
        "timed out waiting for embeddings to populate — check the embedding-service "
        "container is healthy and reachable from the backend"
    )


def main() -> None:
    if not ADMIN_EMAIL or not ADMIN_PASSWORD:
        die("CAMS_ADMIN_EMAIL and CAMS_ADMIN_PASSWORD must be set (see .env)")

    dataset = json.loads(DATASET_PATH.read_text(encoding="utf-8"))

    print(f"Seeding against {API}")

    resp = requests.post(f"{API}/api/auth/login", json={"email": ADMIN_EMAIL, "password": ADMIN_PASSWORD})
    if resp.status_code != 200:
        die(f"admin login failed (HTTP {resp.status_code}): {resp.text}")
    admin_token = resp.json()["token"]

    skill_cache: dict[str, int] = {}

    print("\nCompanies and employers")
    companies = seed_companies_and_employers(dataset, admin_token)

    print("\nCandidates")
    seed_candidates(dataset, skill_cache)

    print("\nListings")
    seed_listings(dataset, companies, skill_cache)

    print("\nWaiting for the async embedding pipeline")
    poll_embeddings(dataset)

    print("\nSeed complete.")


if __name__ == "__main__":
    main()
