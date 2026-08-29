"""
Shared scoring primitives for the matching-algorithm lab. Pure functions only -
no HTTP, file I/O or DB. The one non-function is the Context dataclass, a passive
parameter bag the runner fills in and hands to every scorer; it holds the
embed/cosine callables but never performs I/O itself.
"""

from dataclasses import dataclass, field
from datetime import date
from typing import Callable

# SECONDARY < BACHELORS < MASTERS < DOCTORATE. OTHER is 0 ("unclassified", not a
# level below SECONDARY), so it satisfies no explicit requirement - see meets_education.
EDUCATION_ORDINAL = {"SECONDARY": 1, "BACHELORS": 2, "MASTERS": 3, "DOCTORATE": 4, "OTHER": 0}

# Years-of-experience bands per JobLevel, deliberately overlapping at the edges -
# real listings have no hard cutoffs.
LEVEL_BANDS = {
    "INTERN": (0.0, 1.0),
    "JUNIOR": (0.5, 2.5),
    "MID": (2.0, 6.0),
    "SENIOR": (5.0, float("inf")),
}

# Semantic relevance floor. Unrelated MiniLM embeddings still cosine ~0.1-0.3,
# so rescale() maps [tau, 1.0] onto [0.0, 1.0]. A starting point, swept in sensitivity.py.
TAU = 0.35

# A semantically-inferred skill match is worth at most this fraction of an
# explicitly-claimed one.
BACKFILL_CAP = 0.7

# M4/M5 default weight triple. M6 overrides these per listing level.
DEFAULT_WEIGHTS = {"skills": 0.35, "semantic": 0.40, "seniority": 0.25}

# M6's level-adaptive profile: junior roles judged more on domain fit, senior
# roles more on tenure.
LEVEL_WEIGHTS = {
    "INTERN": {"skills": 0.30, "semantic": 0.55, "seniority": 0.15},
    "JUNIOR": {"skills": 0.35, "semantic": 0.45, "seniority": 0.20},
    "MID": {"skills": 0.35, "semantic": 0.35, "seniority": 0.30},
    "SENIOR": {"skills": 0.30, "semantic": 0.30, "seniority": 0.40},
}

DAYS_PER_YEAR = 365.25


@dataclass
class Context:
    """
    Everything a scorer needs from the outside world, passed in rather than
    imported, so sensitivity.py can re-run the same scorers with different
    constants and the scorers stay pure.
    """

    embed: Callable[[str], list]
    cosine: Callable[[list, list], float]
    today: date
    tau: float = TAU
    backfill_cap: float = BACKFILL_CAP
    weights: dict = field(default_factory=lambda: dict(DEFAULT_WEIGHTS))
    level_weights: dict = field(default_factory=lambda: {k: dict(v) for k, v in LEVEL_WEIGHTS.items()})

    def similarity(self, text_a: str, text_b: str) -> float:
        """Convenience: embed both sides and cosine them. Cached upstream."""
        if not text_a or not text_a.strip() or not text_b or not text_b.strip():
            return 0.0
        return self.cosine(self.embed(text_a), self.embed(text_b))


def parse_date(value) -> date | None:
    """Fixture dates are ISO strings ('2021-03-01') or null for ongoing."""
    if value is None or value == "":
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(value)


def years_between(start_date, end_date, today: date) -> float:
    """
    Tenure of a single experience entry in years.

    end_date None means ongoing, so it resolves to `today`. Never returns a
    negative number: a fixture with end_date before start_date is a data bug,
    and clamping to 0 keeps it from silently subtracting tenure elsewhere.
    """
    start = parse_date(start_date)
    if start is None:
        return 0.0
    end = parse_date(end_date) or today
    days = (end - start).days
    if days <= 0:
        return 0.0
    return days / DAYS_PER_YEAR


def total_years(experiences: list, today: date) -> float:
    """
    Sum of every entry's tenure.

    SIMPLIFICATION, stated rather than hidden: overlapping entries are summed
    as-is (two concurrent roles for a year count as two years). The fixtures
    contain no concurrent roles; revisit this line if any are ever added.
    """
    return sum(years_between(e.get("startDate"), e.get("endDate"), today) for e in experiences)


def seniority_score(years: float, level: str | None) -> float:
    """
    How well a candidate's total tenure fits a listing's level band.

    Asymmetric on purpose: under-qualification is a worse recruiting error
    than over-qualification, so the penalty below the band decays twice as
    fast (d/2.0) as the penalty above it (d/4.0).

    A listing with no level states no tenure constraint, so everyone scores
    1.0 - the same "missing data is not a mismatch" rule LocationEligibility
    uses in production.
    """
    if level is None:
        return 1.0
    band = LEVEL_BANDS.get(level)
    if band is None:
        return 1.0

    low, high = band
    if low <= years <= high:
        return 1.0
    if years < low:
        return max(0.0, 1.0 - (low - years) / 2.0)
    return max(0.0, 1.0 - (years - high) / 4.0)


def meets_education(educations: list, required_level: str | None) -> bool:
    """
    Ordinal education gate.

    - No requirement (None/absent/blank) passes everyone.
    - A candidate passes if ANY entry's level is >= the requirement.
    - Ongoing degrees (endDate null) count at their stated level. A Master's
      in progress is treated as satisfying a MASTERS requirement, which is a
      deliberate leniency: an employer filtering on MASTERS is describing the
      track, not demanding the diploma be already printed.
    - OTHER (ordinal 0) satisfies no explicit requirement. It is the enum's
      unclassified bucket, so it carries no comparable level - counting it as
      a pass would let an unlabelled entry clear a DOCTORATE requirement.
    """
    if not required_level:
        return True
    required = EDUCATION_ORDINAL.get(required_level, 0)
    if required == 0:
        return True
    return any(EDUCATION_ORDINAL.get(e.get("level"), 0) >= required for e in educations)


def normalise_skill(name: str) -> str:
    """
    Skill names are compared case-insensitively, matching the skills table's
    own idx_skills_name_lower unique index. The lab has no DB, so names are
    the join key here where production joins on skill id - noted as a stated
    deviation in the M0 module docstring.
    """
    return (name or "").strip().lower()


def normalise_skills(names) -> set:
    return {normalise_skill(n) for n in (names or []) if normalise_skill(n)}


def skill_coverage(candidate_skills: set, required_skills: set) -> float:
    """
    Recall over what the listing requires: |C & R| / |R|.

    Recall, not Jaccard or precision, precisely so extra candidate skills
    never penalise - that is weakness 3 in the strategy doc's audit, and the
    structural rungs exist to not reproduce it. An empty requirement set is
    1.0 (nothing was asked for, so nothing is missing).
    """
    if not required_skills:
        return 1.0
    return len(candidate_skills & required_skills) / len(required_skills)


def location_eligible(candidate_location, listing_location, listing_remote) -> bool:
    """
    Direct port of cams.util.LocationEligibility.isEligible, so the offline
    harness gates exactly as production does.

    Remote listings are open to everyone. Missing data on either side means
    "no constraint", not "mismatch" - we cannot judge a mismatch against data
    we do not have. Otherwise the candidate's city (text before the first
    comma, lowercased) must appear in the listing's location text.
    """
    if listing_remote is True:
        return True

    candidate_city = extract_city(candidate_location)
    if candidate_city is None or not listing_location or not listing_location.strip():
        return True

    return candidate_city in listing_location.lower()


def extract_city(location) -> str | None:
    """'Sofia, Bulgaria' -> 'sofia'. Blank/None -> None (no constraint)."""
    if not location or not location.strip():
        return None
    city = location.split(",", 1)[0].strip().lower()
    return city or None


def rescale(cos: float, tau: float = TAU) -> float:
    """
    Map a raw cosine onto a usable 0-1 weight: clamp((cos - tau)/(1 - tau)).

    Below tau -> 0.0 (unrelated), 1.0 -> 1.0 (identical). This is the single
    place the TAU floor is applied, so sweeping tau in sensitivity.py changes
    every rung that depends on it consistently.
    """
    if tau >= 1.0:
        return 0.0
    scaled = (cos - tau) / (1.0 - tau)
    return max(0.0, min(1.0, scaled))


# ---------------------------------------------------------------------------
# Production text reproduction
#
# Python mirrors of EmbeddingService's private text builders. M1 is only a valid
# "production as-is" control if these match the Java exactly.
# ---------------------------------------------------------------------------


def _is_blank(value) -> bool:
    """Java's String.isBlank(): null or whitespace-only."""
    return value is None or not str(value).strip()


def build_embedding_text(headline_or_title, description, skill_names: list, extra_parts: list) -> str:
    """
    Mirror of EmbeddingService.buildEmbeddingText.

    Java, for reference:
        List<String> parts = new ArrayList<>();
        if (headlineOrTitle != null && !headlineOrTitle.isBlank()) parts.add(headlineOrTitle);
        if (description != null && !description.isBlank()) parts.add(description);
        if (!skillNames.isEmpty()) parts.add("Skills: " + String.join(", ", skillNames));
        parts.addAll(extraParts);
        return String.join(". ", parts);

    Note what the Java does NOT do, and neither does this: extraParts are
    added unconditionally (no blank check), and the join is ". " with no
    trailing period.
    """
    parts = []
    if not _is_blank(headline_or_title):
        parts.append(headline_or_title)
    if not _is_blank(description):
        parts.append(description)
    if skill_names:
        parts.append("Skills: " + ", ".join(skill_names))
    parts.extend(extra_parts)
    return ". ".join(parts)


def describe_experience(experience: dict) -> str:
    """
    Mirror of EmbeddingService.describeExperience.

    roleTitle + ": " + description when a description is present, else
    roleTitle alone. startDate/endDate are deliberately excluded - that is the
    production behaviour the audit flagged as weakness 1/2, and M1 has to
    reproduce the flaw, not fix it.
    """
    if not _is_blank(experience.get("description")):
        return f"{experience.get('roleTitle')}: {experience.get('description')}"
    return experience.get("roleTitle") or ""


def describe_education(education: dict) -> str:
    """Mirror of EmbeddingService.describeEducation: LEVEL at INSTITUTION [in SPECIALTY]."""
    base = f"{education.get('level')} at {education.get('institutionName')}"
    if not _is_blank(education.get("specialty")):
        return f"{base} in {education.get('specialty')}"
    return base


def describe_course(course: dict) -> str:
    """Mirror of EmbeddingService.describeCourse: NAME (ISSUER)."""
    return f"{course.get('name')} ({course.get('issuingOrganization')})"


def candidate_embedding_text(candidate: dict) -> str:
    """
    The exact text production embeds for a candidate profile:
    headline, description, "Skills: ...", then every experience, education
    and course part, joined with ". ".

    Order matters and is taken from onCandidateProfileChanged: experiences,
    then educations, then courses.
    """
    extra_parts = []
    extra_parts.extend(describe_experience(e) for e in candidate.get("experiences", []))
    extra_parts.extend(describe_education(e) for e in candidate.get("educations", []))
    extra_parts.extend(describe_course(c) for c in candidate.get("courses", []))
    return build_embedding_text(
        candidate.get("headline"),
        candidate.get("description"),
        candidate.get("skills", []),
        extra_parts,
    )


def listing_embedding_text(listing: dict) -> str:
    """
    The exact text production embeds for a job listing: title, description,
    "Skills: ..." - and nothing else. onJobListingChanged passes
    Collections.emptyList() for extraParts, which is precisely why `level`
    has zero influence on the live score (strategy doc Section 2.2).
    """
    return build_embedding_text(
        listing.get("title"),
        listing.get("description"),
        listing.get("skills", []),
        [],
    )
