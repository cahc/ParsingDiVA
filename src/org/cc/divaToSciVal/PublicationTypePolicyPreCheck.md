# Publication-type policy pre-check

Run `org.cc.divaToSciVal.PublicationTypePolicyPreCheck.main()` in IntelliJ.
No arguments uses the current `DIVA_PID_TO_SCIVAL_EID.txt`,
`FilePathConstants.SCIVAL_RAW_XLSX_LATEST`, and a **new**
`publication-type-policy-precheck` directory in the working directory.

Optional positional arguments:

```text
"path/to/matches.tsv" "path/to/SciVal/export/directory" "path/to/new/report-directory"
```

The first argument can also be the existing `divaToScival.parquet`. TSV files
require `PID`, `EID`, and `STATUS` columns. Only `EXACT`, `EXACT_SUSPECT`, and
`TEXT_AUTO_MATCH` enter the focal population. Several accepted PIDs sharing an
EID contribute one observation; every PID is preserved in the detail report.
An existing output directory is rejected so previous reports cannot be overwritten.

## Scenarios

| Scenario | Exclusions | Reference selection |
|---|---|---|
| CURRENT | Retracted, Abstract Report | Existing mixed-type methodology |
| EXCLUSIONS_ONLY | Also Erratum, Editorial, Conference Review | Existing mixed-type methodology |
| GROUPED | Expanded exclusions; unmapped types use generic references | Same group and (exact ASJC partition OR topic cluster) |

The proposed groups are:

| Group | SciVal document types |
|---|---|
| JOURNAL_ARTIFACT_TYPE | Article in Press, Letter, Data Paper, Short Survey, Note, Article, Review |
| CONFERENCE_ARTIFACT_TYPE | Conference Paper |
| BOOK_AND_CHAPTER_ARTIFACT_TYPE | Chapter (also Book Chapter), Book |

Groups depend on document type, never source type. Type labels are normalized
for case and whitespace. Unknown or blank types are listed explicitly and receive generic Swedish references.
`SciValDocumentTypePolicy` supplies the shared production policy. The pre-check does
not invoke matching or overwrite production outputs.

All three scenarios use unique EIDs, exclude accepted focal EIDs and Umeå University
from the benchmark, and reuse `SwedishReferenceIndicatorCalculator`. Explicit calculator modes preserve the historical CURRENT scenario and compare it
with the production GROUPED policy.

Citation references require 25 records, with fallback to the whole external Swedish
group if that group also contains at least 25 records. International references
require 20 records within focal SciVal year ±1; fallback uses the external Swedish
group in that same window, also requiring 20 records. If the group is too small, citation expectations use the generic external Swedish
benchmark. International expectations next use the generic Swedish year window
if it contains at least 20 records, otherwise the generic benchmark across all years.
Missing focal years use the all-years generic international reference. Unknown types
skip group/subject selection and enter the generic fallbacks directly. The final
fallback accepts any nonempty benchmark; an entirely empty eligible benchmark raises
an error. CURRENT and EXCLUSIONS_ONLY retain historical mixed-type fallbacks.

## Reports

- `focal-publications.tsv`: one row per accepted EID, linked PIDs, proposed
  disposition/group, all three sets of expectations, reference sizes, fallback
  flags/scopes, group fallback pool sizes and effective reference population sizes. Grouped-minus-current changes are in
  percentage points. Excluded, unknown and missing focal records remain visible.
- `summary.tsv`: breakdowns by group and individual document type, plus all
  accepted and proposed-included cohorts. For each scenario and indicator:
  reference-size min/P10/median/P90/max, fallback requirement count/rate,
  unrestricted fallback counts, expectation availability, means, and paired changes against CURRENT.
- `document-types.tsv`: export-row and unique-EID counts, accepted focal counts,
  external non-focal counts, and proposed treatment for every parsed type.
- `README.txt`: input provenance, counting rules, units and limitations.

Fallback rates count eligible records requiring fallback. Their denominator is the
number with a calculated subject set, including unknown types (subject size zero);
excluded and missing SciVal records have no grouped set. Means use available
expectations. Paired changes always use the same EIDs on both sides. Blank numeric
values denote unavailable results; zero is a valid measured value. Quantiles use
linear interpolation. Duplicate export EIDs retain the first row from sorted input
files; export-row counts make duplication visible.

This is a **fixed-match sensitivity analysis**. It measures loss of already accepted
focal records and changes in their expectations. It does not rerun candidate retrieval,
calibration or matching, so it cannot predict reassignment of matches after exclusions.
Expectations are recalculated from the supplied export snapshot, not read from
historical match-file indicator columns. Inspect group coverage and fallback rates,
particularly for books/chapters and for unrestricted fallback use.

Run `PublicationTypePolicyPreCheckTest.main()` for standalone regression checks
covering the subject union, grouping, exclusions, deduplication, thresholds, fallback
availability, missing metadata and accepted-match input handling.

Pass a report directory as the test runner's first argument to validate generated
TSV schemas, unique EIDs, reference-set containment, expectation bounds and
consistency of detailed and paired-summary changes.

A second report-directory argument compares all three scenarios with an existing
pre-check. For the present export snapshot, all expectation values and subject sizes
should reproduce the original trial, since no unrestricted fallback is needed.

The SciVal parser in the neighboring `scopustools` project reads publication type
with `DataFormatter`, accepting absent cells or headers as blank. The existing
`C:/opt/ScopusTools.jar` parser class has been updated; its serialized record class
is unchanged. Preserve this parser fix when rebuilding that dependency.
