"""
Rebuild the seeded skill vocabulary from the Flyway migrations.

Every fixture skill name has to exist in the real seeded vocabulary. A name that
is not there scores zero structural coverage, which looks like an algorithm
result rather than the typo it is. The lab runs without Postgres, so the
vocabulary is reconstructed from the two seed migrations into
fixtures/vocabulary.txt and committed with the fixtures.

    V30  ESCO office-relevant skill and knowledge concepts (generated)
    V31  hand-curated non-ESCO supplement (tech products, soft skills)

Run it when those migrations change:

    python build_vocabulary.py

It reads SQL files and writes one text file. run_experiments.py checks against
the generated file, not against this script.
"""

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATIONS = REPO_ROOT / "src" / "main" / "resources" / "db" / "migration"
OUTPUT = Path(__file__).parent / "fixtures" / "vocabulary.txt"

# Migrations that INSERT INTO skills, in application order.
INSERT_MIGRATIONS = [
    "V30__seed_office_skill_vocabulary.sql",
    "V31__add_manual_supplemental_skills.sql",
]

# First single-quoted SQL string on a line, with '' as an escaped quote.
QUOTED = re.compile(r"'((?:[^']|'')*)'")


def unquote(raw: str) -> str:
    return raw.replace("''", "'")


def read_inserted_names(path: Path) -> list:
    """Every skill name from an `INSERT INTO skills (name, ...) VALUES` block."""
    names = []
    in_values = False
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if stripped.startswith("--"):
            continue
        if "INSERT INTO skills" in stripped:
            in_values = True
            continue
        if not in_values:
            continue
        match = QUOTED.search(stripped)
        if match:
            names.append(unquote(match.group(1)))
    return names


def main() -> None:
    if not MIGRATIONS.is_dir():
        print(f"ERROR: migrations not found at {MIGRATIONS}", file=sys.stderr)
        sys.exit(1)

    seeded = []
    for name in INSERT_MIGRATIONS:
        rows = read_inserted_names(MIGRATIONS / name)
        seeded.extend(rows)
        print(f"{name:<48} +{len(rows)}")

    # Deduplicate case-insensitively, the same rule the table's unique index
    # uses, keeping the first-seeded casing as the display form.
    final = {}
    for name in seeded:
        final.setdefault(name.lower(), name)

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text("\n".join(sorted(final.values(), key=str.lower)) + "\n", encoding="utf-8")
    print(f"\nWrote {OUTPUT.relative_to(REPO_ROOT)} - {len(final)} skills")


if __name__ == "__main__":
    main()
