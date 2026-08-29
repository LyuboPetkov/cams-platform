"""
Derives the department-scoped ESCO skill keep list that defines the seeded
skill vocabulary for the departments CAMS targets. Writes
scripts/skill-scope/keep_list.csv; the V30 seed migration is generated from
this list rather than the cut being expressed inline in SQL.

Requires the ESCO v1.2.1 English CSV bundle (skills_en.csv,
occupations_en.csv, occupationSkillRelations_en.csv, ISCOGroups_en.csv) in
esco-v1.2.1-en/ at the repo root.

Usage:
    python derive_target_skills.py
"""

import csv
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
ESCO_DIR = REPO_ROOT / "esco-v1.2.1-en"
MIGRATION_DIR = REPO_ROOT / "src" / "main" / "resources" / "db" / "migration"
OUTPUT_PATH = Path(__file__).parent / "keep_list.csv"

# Department -> ISCO-08 group list, verified against ISCOGroups_en.csv and
# occupations_en.csv (HR and Sales narrowed from broader parent groups that
# pulled in unrelated roles).
DEPARTMENT_ISCO_CODES = {
    "IT / Software": ["25", "35"],
    "Marketing & PR": ["243"],
    "Sales": ["2433", "2434", "332", "52"],
    "Finance & Accounting": ["241", "331"],
    "HR": ["2423", "4416"],
    "Admin / Clerical / Office support": ["41", "43"],
    "Management": ["11", "12"],
    "Customer Service": ["422"],
    "Operations": ["132", "3323", "4321"],
    "Legal": ["261", "3411"],
    "Design": ["216"],
}

# Skills in the transversal sub-scheme (soft/cross-cutting skills) are kept
# unconditionally, regardless of the department join.
TRANSVERSAL_SCHEME = "http://data.europa.eu/esco/concept-scheme/skill-transversal-groups"

# Hand-curated, non-ESCO skill names, kept in the keep list unconditionally.
MANUAL_SUPPLEMENT_MIGRATIONS = [
    "V31__add_manual_supplemental_skills.sql",
]

# Sanity-check labels: expected present / absent after a run.
SPOT_CHECK_EXPECT_PRESENT = ["Java (computer programming)", "Communication", "Project management"]
SPOT_CHECK_EXPECT_ABSENT = ["guide ships into docks", "slaughter animals"]


def read_csv(path: Path) -> list[dict]:
    with path.open(encoding="utf-8") as f:
        return list(csv.DictReader(f))


def clean_label(name: str) -> str:
    return " ".join(name.strip().split())


def normalize(name: str) -> str:
    return clean_label(name).lower()


def resolve_target_isco_codes() -> dict[str, list[str]]:
    isco_groups = read_csv(ESCO_DIR / "ISCOGroups_en.csv")
    known_codes = {row["code"] for row in isco_groups}

    for dept, codes in DEPARTMENT_ISCO_CODES.items():
        for code in codes:
            if code not in known_codes:
                sys.exit(f"ISCO code {code!r} for department {dept!r} does not exist in ISCOGroups_en.csv")
    return DEPARTMENT_ISCO_CODES


def occupations_by_department(target_codes: dict[str, list[str]]) -> dict[str, set[str]]:
    occupations = read_csv(ESCO_DIR / "occupations_en.csv")
    result: dict[str, set[str]] = {}
    for occ in occupations:
        group = occ["iscoGroup"]
        for dept, codes in target_codes.items():
            if any(group.startswith(code) for code in codes):
                result.setdefault(occ["conceptUri"], set()).add(dept)
    return result


def skills_from_occupations(occupation_departments: dict[str, set[str]]) -> dict[str, set[str]]:
    relations = read_csv(ESCO_DIR / "occupationSkillRelations_en.csv")
    skill_departments: dict[str, set[str]] = {}
    included_relations = 0
    for rel in relations:
        depts = occupation_departments.get(rel["occupationUri"])
        if depts is None:
            continue
        included_relations += 1
        skill_departments.setdefault(rel["skillUri"], set()).update(depts)
    print(f"  {included_relations} occupation-skill relations matched (essential + optional)")
    return skill_departments


def parse_manual_supplement(filename: str) -> list[str]:
    text = (MIGRATION_DIR / filename).read_text(encoding="utf-8")
    names = re.findall(r"\('([^']*)',\s*NOW\(\),\s*NOW\(\)\)", text)
    if not names:
        sys.exit(f"parsed zero skill names out of {filename} - check the regex against the file's actual format")
    return names


def main() -> None:
    if not ESCO_DIR.exists():
        sys.exit(f"{ESCO_DIR} not found - place the ESCO v1.2.1 English CSV bundle there")

    print("Resolving locked departments to verified ISCO-08 codes...")
    target_codes = resolve_target_isco_codes()

    print("Filtering occupations_en.csv by department (prefix match)...")
    occ_departments = occupations_by_department(target_codes)
    print(f"  {len(occ_departments)} occupations matched across {len(target_codes)} departments")

    print("Joining occupationSkillRelations_en.csv...")
    skill_departments = skills_from_occupations(occ_departments)
    print(f"  {len(skill_departments)} distinct skill URIs referenced")

    print("Resolving skill URIs to labels via skills_en.csv...")
    skills = read_csv(ESCO_DIR / "skills_en.csv")
    skills_by_uri = {row["conceptUri"]: row for row in skills}

    keep: dict[str, list] = {}  # normalized name -> [display name, {sources}]
    unresolved = 0
    for uri, depts in skill_departments.items():
        row = skills_by_uri.get(uri)
        if row is None:
            unresolved += 1
            continue
        name = clean_label(row["preferredLabel"])
        key = normalize(name)
        keep.setdefault(key, [name, set()])[1].update(depts)
    if unresolved:
        print(f"  WARNING: {unresolved} skill URIs from occupationSkillRelations had no match in skills_en.csv")
    print(f"  {len(keep)} distinct skills derived from occupation joins")

    print("Adding transversal skills unconditionally...")
    before = len(keep)
    for row in skills:
        if TRANSVERSAL_SCHEME in row["inScheme"]:
            name = clean_label(row["preferredLabel"])
            key = normalize(name)
            keep.setdefault(key, [name, set()])[1].add("transversal")
    print(f"  {len(keep) - before} additional skills added from the transversal scheme")

    print("Adding manual supplements unconditionally...")
    before = len(keep)
    for filename in MANUAL_SUPPLEMENT_MIGRATIONS:
        names = parse_manual_supplement(filename)
        print(f"  {filename}: {len(names)} rows")
        for raw_name in names:
            name = clean_label(raw_name)
            key = normalize(name)
            keep.setdefault(key, [name, set()])[1].add(filename)
    print(f"  {len(keep) - before} additional skills added from manual supplements")

    rows = sorted(keep.values(), key=lambda pair: pair[0].lower())
    with OUTPUT_PATH.open("w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["name", "sources"])
        for name, sources in rows:
            writer.writerow([name, "|".join(sorted(sources))])
    print(f"\nTotal keep-list size: {len(rows)} skills -> {OUTPUT_PATH}")

    print("\nSpot check:")
    present_keys = set(keep.keys())
    for name in SPOT_CHECK_EXPECT_PRESENT:
        status = "OK - present" if normalize(name) in present_keys else "MISSING (unexpected)"
        print(f"  expect present: {name!r:45s} {status}")
    for name in SPOT_CHECK_EXPECT_ABSENT:
        status = "OK - absent" if normalize(name) not in present_keys else "STILL PRESENT (unexpected)"
        print(f"  expect absent:  {name!r:45s} {status}")


if __name__ == "__main__":
    main()
