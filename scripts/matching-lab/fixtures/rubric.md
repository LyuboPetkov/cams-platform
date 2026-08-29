# Relevance grading rubric

*Written before `labels.json` existed. Nothing in `results/` had been produced from these
fixtures when this document was authored, and no algorithm output was consulted while grading.
See "Authoring conditions" at the end for the full statement.*

---

## 1. What is being graded

Every one of the 40 candidates is graded against every one of the 18 listings — 720 pairs — on a
0–3 relevance scale. The question each grade answers is deliberately narrow:

> **If this listing's recruiter opened this candidate's profile, how far would it get?**

Not "would they be hired", not "is this person good". Relevance to *this* listing, judged from the
candidate profile and the listing text alone, as those two documents actually read.

The grades are the ground truth the seven matching approaches are measured against. No scorer ever
reads them: `run_experiments.py` loads `labels.json` only in the metrics stage, after every score
has already been computed.

---

## 2. The 0–3 scale

| Grade | Name | Meaning |
|---|---|---|
| **3** | Excellent | Top of the shortlist. Right area *and* right sub-specialty, tenure inside the listing's band, all or nearly all required skills evidenced, no unmet formal requirement. The recruiter would contact this person. |
| **2** | Good | A genuine shortlist entry. Right area, seniority within one band, a majority of the required skills evidenced. Something is imperfect — an adjacent sub-specialty, a band off, a couple of missing skills — but the recruiter would read the whole profile. |
| **1** | Weak / partial | The recruiter would open it and close it. Right area, but a serious mismatch: two or more seniority bands out, thin skill coverage, or a formal requirement the candidate does not meet. Not worthless — a human can see why it surfaced — but not a shortlist entry. |
| **0** | Not relevant | Should not have been surfaced at all. No evidence of work in the listing's professional area, or a hard blocker (location) that makes the pairing impossible regardless of quality. |

The scale is **ordinal, not interval**. The distance from 0 to 1 (should this have been shown at
all?) is a different kind of distance from 2 to 3 (is this the best of a good set?). This is why
the label audit reports a *quadratic-weighted* kappa: a one-grade disagreement is a much smaller
error than a three-grade one, and an unweighted statistic would treat both as simply wrong.

---

## 3. The three signals, in precedence order

The signals are not weighted and summed. They are applied **in order**, each one able to modify
what the previous one established. This ordering *is* the rubric's substantive claim about
relevance, and it is stated here so that a reader can disagree with it explicitly rather than
having to infer it from the grades.

### 3.1 Domain relevance dominates — and domain is read from evidence

A candidate with **no evidence of work in the listing's professional area** is **grade 0**, without
exception, however strong they are elsewhere. A brilliant senior accountant is a 0 against every IT
listing. No amount of seniority fit or incidental skill overlap can lift such a pair off the floor.

This is the strongest statement in the rubric, and it is deliberate. It reflects how the platform
is actually used: a marketing manager browsing matches does not want a well-ranked warehouse role,
and a recruiter for a backend position does not want the accountant however senior. Domain is a
gate, not a factor.

**What "domain" is read from.** Domain is a property of the candidate's *evidence* — their
experience entries, their education, and what their prose actually describes. It is **not** read
from the `area` field. That field is bookkeeping: it records the area the candidate is *seeking*
work in, so that the runner can group listings for the diagnostic tables. It carries no grading
authority whatsoever, and there are candidates in this fixture set whose `area` tag and whose
evidence deliberately disagree.

Three consequences, all of which change grades:

- A candidate whose history **spans two areas** is graded on the merits in *both*. Their pairs in
  the second area are individually authored like any other same-area pair — they are never swept
  into the cross-area zero bucket because a tag says they belong elsewhere. `dsg-career-changer-1`
  is tagged `Design` and has six years of real marketing history; their Marketing pairs are graded,
  not zeroed.
- Conversely, a candidate **tagged** for an area they have never worked in gets no credit for the
  tag. `it-cross-domain-1` is tagged `IT` and has five years of electrical engineering; their IT
  pairs are graded on the near-beginner they actually are in software.
- **A structured skill name, on its own, is not evidence.** A claimed skill with no experience,
  education or prose in that area behind it does not open a domain. This is what makes the
  `skill-superset` fixture coherent: its six extra skill names are claims about other areas with
  nothing whatsoever supporting them, so they lift no cross-area pair off 0 — and, per 3.3, they
  lower nothing either.

Within an area, **sub-specialty** still matters and is a factor rather than a gate. A graphic
designer against a UX research listing is in the right area but the wrong sub-specialty: that pair
caps at **2**, and drops to **1** where the sub-specialty gap is wide enough that the candidate
would need to retrain rather than adapt.

### 3.2 Seniority fit modifies

Once the area is right, tenure against the listing's level band moves the grade. The bands are read
as an ordered ladder — INTERN < JUNIOR < MID < SENIOR — and the candidate is placed on it by the
years of *relevant* experience their profile actually evidences (see 3.5).

| Distance from the listing's band | Effect |
|---|---|
| Inside the band | no change |
| One band below | −1 |
| Two or more bands below | grade **0** |
| One band above | −1, but never below 1 |
| Two or more bands above | cap at **1** |

The asymmetry is intentional: **under-qualification is a worse recruiting error than
over-qualification.** Hiring someone with eight months of experience into a senior position fails;
hiring an over-qualified person merely risks them leaving. So the ladder bottoms out at 0 going
down but floors at 1 going up — an over-qualified candidate is always at least "I can see why this
surfaced", while a badly under-qualified one is noise.

*Stated openly:* this asymmetry is the same shape as the asymmetric penalty in the M4+
`seniority_score` component (`d/2.0` below the band versus `d/4.0` above). They share a rationale
rather than corroborating each other, and section 8 records that as a limitation.

### 3.3 Skill coverage refines

Last, and weakest. Coverage moves a grade by at most one step, and only within what domain and
seniority already allow.

- All or nearly all required skills evidenced → no change.
- Roughly half evidenced → −1 if the pair was heading for 3, otherwise no change.
- Clearly under half → −1.

**Extra skills never reduce a grade.** A candidate carrying six skills the listing never asked for
is exactly as relevant as one carrying none of them. This is a rubric position, not a scoring
convenience: a recruiter reading a CV does not think worse of an applicant for knowing things that
are beside the point. It is also, deliberately, the thing mean-pooled embeddings are expected to
get backwards (weakness 3) — so the `skill-superset` fixture is a direct test of a stated
rubric principle, not a test rigged for a preferred algorithm.

### 3.4 The floor rule

**A same-area pair with no hard blocker never falls below 1.** Grade 0 means "should not have been
surfaced at all", and that is a claim about the *area*, not about degree of fit. A junior content
writer against a mid-level SEO role is a bad match, but a recruiter can see why the system produced
it; calling that 0 would make it indistinguishable from an accountant applying for a backend role,
and would throw away the distinction the benchmark most needs to measure.

There is exactly **one exception**, and it is deliberate: the "two or more bands below" rule in 3.2
does reach 0. Someone with eight months of experience against a senior position is not a weak
match, it is a wrong one, and the rubric says so. This is the same asymmetry as before — the floor
protects over-qualification and thin coverage, but not gross under-qualification.

Without this rule the steps in section 5 compound: cap at 2 for a sub-specialty gap, −1 for a
seniority band, −1 for thin coverage, and an ordinary same-area near-miss arrives at 0 through
arithmetic rather than through judgement.

### 3.5 What counts as "evidenced"

**Free-text evidence counts as fully as a structured skill.** If the candidate's experience
description makes it plain they have worked with a required skill, that skill is covered, whether
or not it also appears in their structured skill list. Nothing in the rubric rewards the mere act
of ticking a box.

Likewise, **years count only where the work is relevant.** Five years of electrical engineering
does not make someone a MID-band software candidate; for the purpose of 3.2 those years place the
candidate where their *relevant* history places them, not where their total history does. Older
relevant experience still counts, but stale experience — several years since the candidate last
worked in the area — caps the pair at **2**.

---

## 4. Hard blockers

Two conditions are checked before the three signals and can override them.

**Location.** A listing that is not remote and is in a city the candidate is not in is **grade 0**,
regardless of fit. This mirrors what CAMS actually does — `LocationEligibility` is a pre-filter in
the production SQL, so such a pair is never shown to anyone — and all seven approaches in the lab
apply the same gate, so it advantages none of them. Remote listings never gate. Missing location
data on either side is treated as "no constraint", not as a mismatch, exactly as in production.

**Education.** A listing stating a `requiredEducationLevel` the candidate does not hold **caps the
pair at 1**, not at 0. The distinction matters: an unmet formal requirement is a real blocker, but
it is not a domain mismatch, and grading it 0 would make it indistinguishable from an accountant
applying for a backend role. "The recruiter would open it and close it" is the honest description,
and that is what 1 means. Requirements are read ordinally — SECONDARY < BACHELORS < MASTERS <
DOCTORATE — and a degree *in progress* counts at its stated level, on the reading that an employer
filtering on MASTERS is describing the track rather than demanding the diploma already be printed.

---

## 5. The procedure, as applied

For each pair, in this order:

1. **Location blocked?** → 0. Stop.
2. **No evidence in the listing's professional area?** → 0. Stop. Reason recorded as "No evidence
   in this professional area". Judged on experience, education and prose — never on the `area` tag
   (3.1). A candidate with evidence in the area continues to step 3 whatever their tag says.
3. **Start from 3.** Same area, right sub-specialty, no complications.
4. **Sub-specialty gap** → cap at 2, or 1 if wide.
5. **Seniority distance** (3.2 table) → apply.
6. **Skill coverage** (3.3) → apply, at most one step.
7. **Stale relevance** → cap at 2.
8. **Education requirement unmet** → cap at 1.
9. **Apply the floor rule** (3.4): same area, no hard blocker → never below 1, except where step 5
   found the candidate two or more bands *below* the listing.
10. Clamp to 0–3.

Steps 3–8 are judgement applied consistently, not arithmetic. Where a pair sat genuinely between
two grades, the rule used was **grade down**: a 2 that could be argued as a 3 was recorded as 2, on
the principle that a benchmark should not flatter the thing it measures. Every grade carries a
one-line reason in `labels.json` so that any individual call can be disputed on its merits.

Pairs with no evidence in the listing's area are written out explicitly as rows with the reason
"No evidence in this professional area" rather than left to the scoring code's absent-pair default,
so that `labels.json` is a complete, self-contained statement of the ground truth. That bucket is
determined by evidence, never by the `area` tag (3.1) — every candidate whose history spans areas
has those pairs individually authored instead.

---

## 6. How the diagnostic fixtures are intended to be graded

These ten fixtures exist to make the approaches *diverge*. Their grades are the sharpest claims in
the benchmark, so each is stated here in advance, with the reason.

| Tag | Intended grading | Why |
|---|---|---|
| `cross-domain-tenure` | **Low (0–1)** against IT listings | Five years of electrical engineering is not five years of software. Under 3.5 those years do not place the candidate on the software ladder at all, so the pair is graded as the near-beginner they are in this area. If an approach ranks this candidate highly, it is reading tenure without reading domain. |
| `skill-superset` | **Identical to its pair, on all 18 listings** | The two candidates differ in exactly one field: six extra unrelated skills. Under 3.3 extras never penalise, so the grades must be byte-identical. Any score difference an approach produces is therefore pure dilution, measurable without confounds. |
| `esco-gap` | **Graded as if Spring Boot were a structured skill** | Tests **under-claiming a skill that IS in the seeded vocabulary** — not vocabulary absence. `Spring Boot` is present in the vocabulary (added manually by V12/V22); this candidate simply never claimed it, listing only `Java (computer programming)` while their work history describes the Spring Boot and PostgreSQL work plainly. Under 3.5 prose evidence counts fully, so coverage is full and this is a 3. An approach that grades it down is punishing the candidate for how they filled in a form. This is the strength of the embedding approach that any replacement must not lose. |
| `vocabulary-gap` | **Graded as if Agile project management were structured** | Tests the case `esco-gap` does *not*: a skill the seeded vocabulary **cannot express**. The candidate's history is explicit about Scrum — sprint planning, retrospectives, a Scrum Master rotation — but `Scrum` is not in CAMS's vocabulary at all, so there is no structured skill they could have selected. The listing requires `Agile project management`, which is in the vocabulary. Under 3.5 the evidence covers the requirement and the pair is a 3. Structural coverage alone cannot reach it; only semantic evidence can. |
| `overqualified` | **1** against the JUNIOR listing | Twelve years against a JUNIOR band is two-plus bands above: capped at 1 by 3.2, never 0. The recruiter can see why it surfaced; they would not shortlist it. |
| `underqualified` | **0** against the SENIOR listing | Eight months against a SENIOR band is two-plus bands below, which floors at 0. This is the asymmetry in 3.2 doing its work: the same distance in the other direction was worth 1. |
| `career-changer` | **Varies by band in the new area: 2 / 1 / 0**, and **1** in the old | The most recent two years are the relevant ones, so the candidate sits in the JUNIOR band despite eight years of total tenure. Against Design that gives UX-MID **2** (own sub-specialty, one band below), graphic-JUNIOR **1** (sub-specialty gap, no coverage) and lead-SENIOR **0** (two-plus bands below). The grades must *differ across the three bands*: a flat grade would make M4's `total_years` ordering and M5's `relevant_years` ordering score identically and the fixture would measure nothing. Note the ordering is deliberately **inverted** against total tenure — 8.0 total years would make the SENIOR listing the best seniority fit, and the labels say it is the worst. Their six years of marketing are real but stale, and per 3.1 are graded rather than zeroed: **1** across all three Marketing listings. |
| `education-gate-pass` | **Unaffected by education** | MASTERS held against a BACHELORS requirement satisfies it. The requirement contributes nothing; the grade comes from the three signals alone. This fixture exists to confirm the gate passes on `>=` and not on `==`. |
| `education-gate-fail` | **Cap 1** | SECONDARY against a BACHELORS requirement. Per section 4, a formal blocker is a 1, not a 0. The interesting result here is *which* approaches see the requirement at all — M1 and M3 cannot, since it never enters their text. |
| `ongoing-degree` | **Unaffected by education** | A Master's with `endDate: null` counts at MASTERS against a MASTERS requirement, per section 4. |
| `thin-profile` | **Graded on the structured facts alone, with no discount for thinness** | This is the important one. The candidate has strong skills and a one-line description. The grade reflects the fit those facts describe, and is *not* reduced for the profile being sparse. If the grade were discounted, the fixture would test nothing — the whole point is to measure how far each approach falls when the prose it relies on is absent. |

---

## 7. Authoring conditions

Stated plainly, because the circularity objection is the obvious one at a defence:

- This rubric was written **before** `labels.json` existed and was not revised afterwards.
- Every grade was authored from the candidate profile and the listing text **only**. No score, no
  ranking, and no output of any of the seven approaches was consulted, and none had been produced
  from these fixtures at the time of grading.
- The fixtures were designed first, the rubric second, the labels third, and the experiments
  fourth, in that order.
- `labels.json` records `authoredBlindToAlgorithmOutput: true` as a machine-readable assertion of
  the above.

The independent check on all of this is `audit_sample.py`: a stratified sample of pairs is
re-graded from candidate and listing text alone, with no sight of the grades recorded here, and
`results/audit_agreement.md` reports the agreement. That figure — not this document's own
assurances — is the honest answer to "how do you know your labels are right".

---

## 8. Limitations of this rubric

Recorded here rather than left for a reader to find:

1. **The fixtures and the labels share an author.** Designing a candidate to be a good MID backend
   match and then grading them a 3 is not independent evidence. The blind audit bounds how much of
   the result this could account for; it does not eliminate it.
2. **The seniority asymmetry is contaminated, not convergent.** Stating this precisely, because
   the weaker version would be misleading: this rubric was authored with the **algorithm design
   already in hand**, including M4+'s asymmetric `seniority_score` (`d/2.0` below
   the band, `d/4.0` above). The rubric's matching asymmetry in 3.2 therefore has a **direct
   contamination path** from the algorithm design into the ground truth. It is not two independent
   parties arriving at the same conclusion, and it must not be presented as convergent validation.
   The bands themselves (`LEVEL_BANDS`) reached the labels by the same route.

   What survives the contamination: M1, M2 and M3 do not implement the asymmetry at all, so any
   M3 -> M4 gain is partly a measure of the labels agreeing with M4's design rather than of M4
   being better, and should be read that way. The M4 -> M5 -> M6 comparisons are unaffected, since
   all three share the identical seniority component and differ only in what feeds it and how it is
   weighted. The blind audit is the only check that bears on this at all, and it is a weak one: the
   auditor is also not blind to the algorithm.
3. **"Professional area" is coarser than real hiring.** Six areas is a simplification; real
   relevance has no such clean partition, and the hard 0 for cross-area pairs makes the benchmark
   easier than reality on exactly the axis embeddings are best at.
4. **The `area` field is bookkeeping and carries no grading authority** (3.1). It records the area
   a candidate is seeking work in, so the runner can group listings for the diagnostic tables. For
   `cross-domain-tenure` and `career-changer` the tag and the evidence differ on purpose. Every
   grade follows the evidence.
5. **Synthetic data.** These are authored profiles, not real ones. Real CVs are messier, less
   internally consistent, and worse written. The benchmark supports *relative* comparison between
   approaches on identical data; it is not a claim about absolute real-world matching quality.
