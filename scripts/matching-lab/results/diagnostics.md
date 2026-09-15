# Diagnostic fixtures

Written by `run_experiments.py`.

The aggregate metrics say which approach scores best. These say why. Each fixture
below is a case built to make the approaches disagree, shown against the listings in
that candidate's own area.

## `career-changer`

Years in one area, most recent years in another. Does recent relevant work outweigh older irrelevant work? The labels vary across the three bands of the new area and run opposite to total tenure, so M4 (total_years) and M5 (relevant_years) cannot score it identically.

**dsg-career-changer-1** (area: Design)

| Approach | dsg-graphic-junior rank (score) | dsg-ux-mid rank (score) | dsg-lead-senior rank (score) |
|---|---|---|---|
| M0_rule_based_v1 | 5 (0.371) | 1 (0.775) | 3 (0.539) |
| M1_embedding_fused | 4 (0.435) | 1 (0.515) | 2 (0.507) |
| M2_embedding_fused_level | 4 (0.423) | 1 (0.544) | 2 (0.507) |
| M3_embedding_sections | 6 (0.376) | 1 (0.572) | 2 (0.470) |
| M4_hybrid_structural | 10 (0.038) | 1 (0.493) | 2 (0.461) |
| M5_hybrid_relevance_weighted | 4 (0.227) | 1 (0.515) | 6 (0.213) |
| M6_hybrid_level_adaptive | 4 (0.195) | 1 (0.530) | 6 (0.172) |

Intended grades: dsg-graphic-junior=1, dsg-ux-mid=2, dsg-lead-senior=0

## `cross-domain-tenure`

Five or more years in an unrelated field, applying in another. Does irrelevant tenure add noise instead of seniority signal?

**it-cross-domain-1** (area: IT)

| Approach | it-frontend-junior rank (score) | it-backend-mid rank (score) | it-devops-senior rank (score) |
|---|---|---|---|
| M0_rule_based_v1 | 12 (0.325) | 1 (0.508) | 3 (0.371) |
| M1_embedding_fused | 4 (0.371) | 1 (0.377) | 5 (0.342) |
| M2_embedding_fused_level | 3 (0.380) | 1 (0.437) | 8 (0.349) |
| M3_embedding_sections | 7 (0.289) | 1 (0.357) | 5 (0.304) |
| M4_hybrid_structural | 12 (0.069) | 1 (0.338) | 7 (0.250) |
| M5_hybrid_relevance_weighted | 1 (0.188) | 7 (0.087) | 13 (0.000) |
| M6_hybrid_level_adaptive | 1 (0.151) | 7 (0.087) | 13 (0.000) |

Intended grades: it-frontend-junior=0, it-backend-mid=1, it-devops-senior=0

## `education-gate-fail`

Holds less than the listing requires. Does the gate exclude, and is the exclusion visible rather than silent?

**mkt-education-gate-fail-1** (area: Marketing)

| Approach | mkt-content-junior rank (score) | mkt-digital-mid rank (score) | mkt-brand-senior rank (score) |
|---|---|---|---|
| M0_rule_based_v1 | 1 (0.911) | 2 (0.382) | 4 (0.347) |
| M1_embedding_fused | 1 (0.667) | 2 (0.499) | 6 (0.309) |
| M2_embedding_fused_level | 1 (0.654) | 2 (0.496) | 5 (0.338) |
| M3_embedding_sections | 1 (0.588) | 2 (0.481) | 3 (0.375) |
| M4_hybrid_structural | 15 (0.000) | 1 (0.378) | 14 (0.000) |
| M5_hybrid_relevance_weighted | 15 (0.000) | 2 (0.194) | 14 (0.000) |
| M6_hybrid_level_adaptive | 15 (0.000) | 1 (0.205) | 14 (0.000) |

Intended grades: mkt-content-junior=1, mkt-digital-mid=1, mkt-brand-senior=0

## `education-gate-pass`

Holds more than the listing requires. Does the ordinal gate pass on >= rather than ==?

**hr-education-gate-pass-1** (area: HR)

| Approach | hr-recruiter-junior rank (score) | hr-generalist-mid rank (score) | hr-head-senior rank (score) |
|---|---|---|---|
| M0_rule_based_v1 | 10 (0.358) | 1 (0.888) | 9 (0.361) |
| M1_embedding_fused | 10 (0.228) | 1 (0.461) | 2 (0.395) |
| M2_embedding_fused_level | 11 (0.214) | 1 (0.469) | 2 (0.388) |
| M3_embedding_sections | 4 (0.337) | 1 (0.563) | 2 (0.400) |
| M4_hybrid_structural | 9 (0.183) | 1 (0.629) | 13 (0.071) |
| M5_hybrid_relevance_weighted | 2 (0.191) | 1 (0.476) | 7 (0.022) |
| M6_hybrid_level_adaptive | 2 (0.154) | 1 (0.491) | 7 (0.019) |

Intended grades: hr-recruiter-junior=1, hr-generalist-mid=3, hr-head-senior=1

## `esco-gap`

A candidate under-claiming a skill that is in the seeded vocabulary. `Spring Boot` is there (the manual supplement added it) but this candidate never selected it, although their work history describes the work plainly. This is a real strength of the embedding approach and any replacement has to keep it.

**it-esco-gap-1** (area: IT)

| Approach | it-frontend-junior rank (score) | it-backend-mid rank (score) | it-devops-senior rank (score) |
|---|---|---|---|
| M0_rule_based_v1 | 2 (0.413) | 1 (0.486) | 3 (0.389) |
| M1_embedding_fused | 3 (0.383) | 1 (0.698) | 2 (0.404) |
| M2_embedding_fused_level | 3 (0.382) | 1 (0.713) | 2 (0.402) |
| M3_embedding_sections | 4 (0.315) | 1 (0.611) | 5 (0.311) |
| M4_hybrid_structural | 13 (0.079) | 1 (0.521) | 2 (0.276) |
| M5_hybrid_relevance_weighted | 2 (0.191) | 1 (0.529) | 7 (0.030) |
| M6_hybrid_level_adaptive | 2 (0.154) | 1 (0.556) | 7 (0.023) |

Intended grades: it-frontend-junior=1, it-backend-mid=3, it-devops-senior=1

## `ongoing-degree`

Degree in progress against a listing requiring it. Checks the 'ongoing counts at its stated level' decision.

**fin-ongoing-degree-1** (area: Finance & Accounting)

| Approach | fin-accountant-junior rank (score) | fin-analyst-mid rank (score) | fin-controller-senior rank (score) |
|---|---|---|---|
| M0_rule_based_v1 | 15 (0.000) | 2 (0.285) | 1 (0.752) |
| M1_embedding_fused | 15 (0.000) | 2 (0.420) | 1 (0.704) |
| M2_embedding_fused_level | 15 (0.000) | 2 (0.455) | 1 (0.755) |
| M3_embedding_sections | 15 (0.000) | 2 (0.441) | 1 (0.664) |
| M4_hybrid_structural | 15 (0.000) | 2 (0.341) | 1 (0.685) |
| M5_hybrid_relevance_weighted | 10 (0.000) | 5 (0.167) | 1 (0.474) |
| M6_hybrid_level_adaptive | 10 (0.000) | 2 (0.175) | 1 (0.400) |

Intended grades: fin-accountant-junior=0, fin-analyst-mid=1, fin-controller-senior=2

## `overqualified`

Far more tenure than the listing's band. Is over-qualification penalised gently?

**mkt-overqualified-1** (area: Marketing)

| Approach | mkt-content-junior rank (score) | mkt-digital-mid rank (score) | mkt-brand-senior rank (score) |
|---|---|---|---|
| M0_rule_based_v1 | 1 (0.908) | 2 (0.638) | 3 (0.521) |
| M1_embedding_fused | 2 (0.560) | 1 (0.587) | 3 (0.533) |
| M2_embedding_fused_level | 3 (0.551) | 1 (0.611) | 2 (0.582) |
| M3_embedding_sections | 1 (0.551) | 2 (0.526) | 3 (0.479) |
| M4_hybrid_structural | 1 (0.440) | 4 (0.285) | 2 (0.401) |
| M5_hybrid_relevance_weighted | 1 (0.690) | 2 (0.550) | 7 (0.167) |
| M6_hybrid_level_adaptive | 1 (0.652) | 2 (0.594) | 7 (0.140) |

Intended grades: mkt-content-junior=1, mkt-digital-mid=2, mkt-brand-senior=1

## `skill-superset`

Identical to a paired candidate except for extra unrelated skills. Does mean-pooling dilution push the superset candidate below its own twin?

**it-backend-mid-1-superset** (area: IT)

| Approach | it-frontend-junior rank (score) | it-backend-mid rank (score) | it-devops-senior rank (score) |
|---|---|---|---|
| M0_rule_based_v1 | 6 (0.411) | 1 (0.911) | 7 (0.391) |
| M1_embedding_fused | 3 (0.541) | 1 (0.831) | 2 (0.563) |
| M2_embedding_fused_level | 3 (0.533) | 1 (0.844) | 2 (0.551) |
| M3_embedding_sections | 5 (0.372) | 1 (0.715) | 2 (0.438) |
| M4_hybrid_structural | 14 (0.136) | 1 (0.849) | 3 (0.364) |
| M5_hybrid_relevance_weighted | 6 (0.260) | 1 (0.849) | 7 (0.122) |
| M6_hybrid_level_adaptive | 6 (0.232) | 1 (0.868) | 8 (0.092) |

Intended grades: it-frontend-junior=1, it-backend-mid=3, it-devops-senior=1

### Dilution measurement vs `it-backend-mid-1`

The two candidates are identical apart from the extra skills, so any non-zero
delta comes from dilution alone. Negative means the superset candidate scored lower.

| Approach | delta on it-frontend-junior | delta on it-backend-mid | delta on it-devops-senior |
|---|---|---|---|
| M0_rule_based_v1 | +0.0000 | +0.0000 | +0.0000 |
| M1_embedding_fused | +0.0056 | +0.0057 | +0.0124 |
| M2_embedding_fused_level | +0.0055 | +0.0108 | +0.0109 |
| M3_embedding_sections | -0.0191 | -0.0336 | -0.0205 |
| M4_hybrid_structural | +0.0000 | +0.0000 | +0.0000 |
| M5_hybrid_relevance_weighted | +0.0000 | +0.0000 | +0.0000 |
| M6_hybrid_level_adaptive | +0.0000 | +0.0000 | +0.0000 |

## `thin-profile`

Strong structured skills, almost no free text. How much does each approach depend on prose?

**it-thin-profile-1** (area: IT)

| Approach | it-frontend-junior rank (score) | it-backend-mid rank (score) | it-devops-senior rank (score) |
|---|---|---|---|
| M0_rule_based_v1 | 8 (0.370) | 4 (0.385) | 1 (0.962) |
| M1_embedding_fused | 3 (0.323) | 8 (0.287) | 1 (0.513) |
| M2_embedding_fused_level | 5 (0.333) | 3 (0.359) | 1 (0.502) |
| M3_embedding_sections | 4 (0.371) | 2 (0.375) | 1 (0.633) |
| M4_hybrid_structural | 10 (0.027) | 7 (0.105) | 1 (0.728) |
| M5_hybrid_relevance_weighted | 2 (0.267) | 13 (0.000) | 1 (0.478) |
| M6_hybrid_level_adaptive | 2 (0.222) | 13 (0.000) | 1 (0.396) |

Intended grades: it-frontend-junior=1, it-backend-mid=1, it-devops-senior=3

## `underqualified`

Far less tenure than the listing's band. Is under-qualification penalised harder than over-qualification?

**it-underqualified-1** (area: IT)

| Approach | it-frontend-junior rank (score) | it-backend-mid rank (score) | it-devops-senior rank (score) |
|---|---|---|---|
| M0_rule_based_v1 | 1 (0.753) | 2 (0.378) | 3 (0.370) |
| M1_embedding_fused | 1 (0.731) | 2 (0.511) | 4 (0.462) |
| M2_embedding_fused_level | 1 (0.729) | 2 (0.553) | 4 (0.465) |
| M3_embedding_sections | 1 (0.605) | 2 (0.401) | 4 (0.348) |
| M4_hybrid_structural | 1 (0.697) | 6 (0.158) | 12 (0.074) |
| M5_hybrid_relevance_weighted | 1 (0.669) | 7 (0.074) | 6 (0.081) |
| M6_hybrid_level_adaptive | 1 (0.651) | 6 (0.065) | 7 (0.062) |

Intended grades: it-frontend-junior=2, it-backend-mid=0, it-devops-senior=0

## `vocabulary-gap`

A skill the seeded vocabulary cannot express. The candidate's history is explicit about Scrum, but `Scrum` is in no seeded skill list, so there was no structured skill to select. The listing requires `Agile project management`, which is in the vocabulary. Structural coverage cannot reach this pair; only semantic evidence can.

**it-vocabulary-gap-1** (area: IT)

| Approach | it-frontend-junior rank (score) | it-backend-mid rank (score) | it-devops-senior rank (score) |
|---|---|---|---|
| M0_rule_based_v1 | 3 (0.388) | 1 (0.778) | 5 (0.371) |
| M1_embedding_fused | 3 (0.421) | 1 (0.617) | 2 (0.459) |
| M2_embedding_fused_level | 3 (0.422) | 1 (0.633) | 2 (0.463) |
| M3_embedding_sections | 3 (0.377) | 1 (0.614) | 2 (0.384) |
| M4_hybrid_structural | 10 (0.144) | 1 (0.633) | 2 (0.259) |
| M5_hybrid_relevance_weighted | 2 (0.229) | 1 (0.654) | 7 (0.068) |
| M6_hybrid_level_adaptive | 2 (0.191) | 1 (0.689) | 7 (0.051) |

Intended grades: it-frontend-junior=1, it-backend-mid=3, it-devops-senior=1

