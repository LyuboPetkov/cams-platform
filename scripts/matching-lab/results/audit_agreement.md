# Label audit - agreement between by-construction and blind human grading

Sample: 60 pairs, drawn with seed 35.  
Graded from candidate and listing text alone: no by-construction grade, no scores and no
algorithm output appeared on the worksheet.

**The sample is stratified, not uniform.** It over-weights seniority-mismatch pairs - the
part of the ground truth contaminated by the algorithm design, see rubric.md 8.2 -
and education-capped pairs, where the rubric deliberately disagrees with the M4+ hard gate,
while keeping a cross-area quota so the easy end of the scale is still represented.
Agreement measured this way is **conservative**: a uniform 10% draw would be roughly 83%
cross-area zeroes and would score substantially higher without telling you anything.

| Statistic | Value |
|---|---|
| Cohen's kappa (quadratic weights) | 0.854 |
| Spearman rho | 0.769 |
| Exact agreement | 73.3% |
| Agreement within 1 grade | 98.3% |

## Reading

A quadratic-weighted kappa of 0.854 is **almost perfect** agreement on the
conventional Landis & Koch bands. Kappa corrects for agreement that chance alone would
produce; the quadratic weighting means a one-grade disagreement counts far less than a
three-grade one, which is the right treatment for an ordinal 0-3 scale. Spearman rho
(0.769) says the same thing about ordering rather than about exact values.

This figure is the honest answer to "how do you know your labels are right". It does not
make the benchmark objective - the sample was graded by the same person who designed the
fixtures, just without sight of the original grades - but it does bound how much of the
result could be an artefact of one particular grading pass.

