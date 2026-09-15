# Label audit: agreement between the original labels and a blind re-grade

Written by `audit_sample.py --score`, from the worksheet emitted by `--emit` and graded
by hand in between.

Sample: 60 pairs, drawn with seed 35.  
Graded from candidate and listing text alone. No original grade, no score and no algorithm
output appeared on the worksheet.

The sample is stratified rather than uniform. It favours seniority-mismatch pairs, which
are the part of the ground truth most exposed to the algorithm design (rubric.md 8.2), and
education-capped pairs, where the rubric disagrees with the M4 hard gate on purpose. A
cross-area quota keeps the easy end of the scale represented. Agreement measured this way
is conservative: a uniform 10% draw would be about 83% cross-area zeroes and would score
higher without showing much.

| Statistic | Value |
|---|---|
| Cohen's kappa (quadratic weights) | 0.854 |
| Spearman rho | 0.769 |
| Exact agreement | 73.3% |
| Agreement within 1 grade | 98.3% |

## Reading

A quadratic-weighted kappa of 0.854 counts as almost perfect agreement on the
usual Landis & Koch bands. Kappa corrects for the agreement chance alone would produce, and
the quadratic weighting means a one-grade disagreement counts for much less than a
three-grade one, which suits an ordinal 0-3 scale. Spearman rho (0.769) says the same
thing about ordering rather than about exact values.

This does not make the benchmark objective. The sample was graded by the same person who
designed the fixtures, only without sight of the original grades. What it does is bound how
much of the result could be an artefact of one particular grading pass.

