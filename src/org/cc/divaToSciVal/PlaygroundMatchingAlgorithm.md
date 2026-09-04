# DiVA-to-SciVal bibliographic matching algorithm

## 1. Purpose and source of truth

This document specifies the matching and indicator-enrichment pipeline implemented by
[MatchDiVAToSciVal.java](./MatchDiVAToSciVal.java). It is intended to be precise enough for an
independent implementation or audit.

The Java implementation remains the executable source of truth. This document
describes the current constants, order of operations, edge cases, and output
semantics. The algorithm is deliberately conservative: Lucene retrieval seeks
plausible candidates, while separate evidence and calibration gates decide whether
the highest-ranked candidate can be accepted automatically.

The algorithm links each eligible DiVA record \(d\) to zero or one selected SciVal
record \(s\). An ineligible DiVA record is retained in the audit output with status
IGNORED but is outside the matching population. The algorithm does not impose a
global one-to-one relationship: several eligible DiVA records
may link to the same SciVal EID when the collision rules allow it.

## 2. Pipeline summary

For each run, the program performs these stages in order:

1. Read the SciVal and DiVA data.
2. Apply the DiVA publication-inclusion policy. Ineligible posts are assigned
   IGNORED and leave the matching pipeline.
3. Construct multi-valued DOI, PMID, and EID indexes over SciVal.
4. Resolve exact identifier matches and identifier conflicts for eligible DiVA posts.
5. Build a Lucene index for text retrieval.
6. Identify trustworthy exact matches among eligible posts and use them to
   construct positive and hard-negative calibration examples.
7. Select a minimum reranker score and a minimum runner-up margin on the training
   split, then verify them on a held-out split.
8. Classify every eligible DiVA post, giving identifier results precedence over text
   matching, and retain IGNORED for every ineligible post.
9. Post-process SciVal EIDs proposed for more than one eligible DiVA record.
10. For accepted matches, calculate observed and subject-matched Swedish expected
    citation and international-collaboration indicators once per unique EID.
11. Write one row per parsed DiVA record, including ignored posts, to both the UTF-8
    TSV and streaming XLSX audit outputs.

## 3. Fixed parameters

| Parameter | Value | Meaning |
|---|---:|---|
| Candidate limit | 25 | Maximum Lucene hits before EID deduplication |
| Target held-out precision | 0.99 | Minimum validation precision required to enable automatic text matching |
| Training precision buffer | 0.005 | Added to the target during threshold selection |
| Required training precision | 0.995 | \(0.99 + 0.005\) |
| Absolute score floor | 0.65 | Calibration cannot select a lower automatic-match score |
| Absolute margin floor | 0.05 | Calibration cannot select a lower automatic-match margin |
| Ordinary-title automatic evidence floor | 0.65 | Minimum title feature in the ordinary full-field case |
| Identifier/shared-target title floor | 0.90 | Required title feature for automatic matching when the target is already claimed or shared |
| Weak-title margin floor | 0.15 | Additional margin floor for weak titles |
| Subtitle containment floor | 0.85 | Minimum token containment for the title containment boost |
| Subtitle length-ratio floor | 0.50 | Minimum shorter/longer token-set size ratio for the containment boost |
| Review score floor | 0.60 | Minimum total score for AMBIGUOUS |
| Title weight | 0.55 | Reranker weight |
| Author weight | 0.25 | Reranker weight |
| Source weight | 0.15 | Reranker weight |
| Year weight | 0.05 | Reranker weight |
| Document-type weight | 0.10 | Reranker weight |

The calibrated score and margin are data-dependent. The constants above define the
search space and hard safety constraints, not necessarily the thresholds selected
for a particular run.

## 4. Input scope and operational paths

The current executable has these hard-coded paths:

| Purpose | Path |
|---|---|
| SciVal input directory | `FilePathConstants.SCIVAL_RAW_XLSX_LATEST` |
| DiVA CSV | E:\2026\divaToSciVal\Raw_DiVA_export_20260819_115425.csv |
| Temporary Lucene index | C:\tmp\temporaryLucene |
| TSV output | DIVA_PID_TO_SCIVAL_EID.txt in the process working directory |
| XLSX output | DIVA_PID_TO_SCIVAL_EID.xlsx in the process working directory |

SciVal records whose document type is exactly Retracted or Abstract Report are
excluded by the parser before identifier indexing and text retrieval. Editorial,
Note, Letter, and Erratum records are retained.

Every parsed SciVal batch is checked for repeated AFIDs within a record. A repeated
AFID causes the run to terminate with an exception.

### 4.1 DiVA publication eligibility

Immediately after parsing, every DiVA post is passed to
`DefaultPubIncludingAheadOfPrint.consideredPub(post)`. The returned `StatusInModel`
object is stored on the post with `post.setStatusInModel(...)`. Its Boolean
`isIgnorerad()` value is authoritative:

- `false` means the post is eligible for matching;
- `true` means the post is ineligible and receives final matching status IGNORED.

The current `DefaultPubIncludingAheadOfPrint` policy uses exact, case-sensitive
string comparisons. Define the accepted DiVA content types as
`Refereegranskat` and `Övrigt vetenskapligt`. Its decisions are:

| DiVA publication type | Eligible exactly when |
|---|---|
| `Artikel i tidskrift` or `Artikel, forskningsöversikt` | content type is accepted; subtype is not `editorialMaterial`, `meetingAbstract`, or `newsItem`; and DiVA status is `published` or `aheadofprint` |
| `Bok` or `Kapitel i bok, del av antologi` | content type is accepted |
| `Konferensbidrag` | content type is accepted and subtype is not `abstracts`, `poster`, or `presentation` |
| Any other publication type | never eligible |

For books, chapters, and conference contributions, this policy does not inspect the
DiVA publication-status field. The precise ignored reason is retained in
`StatusInModel.getStatusInModel()` and written in the output NOTE field.

The five possible ignored reasons, written after the prefix
`publication inclusion filter=`, are:

- `Ignorerad (editorial/abstract or newsItem)`;
- `Ignorerad (ej publicerad)`;
- `Ignorerad (ej vetenskapligt)`;
- `Ignorerad (abstract, poster eller presentation)`;
- `Ignorerad (ej beaktad publikationstyp)`.

An ignored post is excluded before any post-specific matching work. In particular,
the program does not resolve its DOI, PMID, or EID; does not count or claim an exact
identifier target; does not use it as a trusted calibration record; does not execute
a Lucene query or reranker for it; and does not include it in shared-target collision
processing. It nevertheless receives one TSV audit row.

## 5. Normalization and field extraction

### 5.1 General cleaning

The helper clean operation converts null to the empty string and otherwise trims
leading and trailing whitespace.

A year is valid only when it is non-null and greater than zero. An invalid year is
represented internally by \(-1\).

### 5.2 Identifier normalization

#### DOI

DOIs are normalized in this order:

1. Trim and lowercase.
2. Remove a leading http://doi.org/, https://doi.org/, http://dx.doi.org/, or
   https://dx.doi.org/.
3. Remove a leading doi: with optional whitespace around the colon.
4. Remove all remaining whitespace.
5. Remove trailing periods, commas, and semicolons.
6. Treat the result as missing when its length is at most 5.

#### PMID

All non-digit characters are removed. The result is treated as missing when its
length is at most 3.

This means that a field containing several PMID values is not split: all its digits
would be concatenated by the current implementation.

#### EID

EIDs are trimmed, lowercased, and stripped of all whitespace. The result is treated
as missing when its length is at most 5.

### 5.3 Character normalization

The simplifyString operation used for character similarity and surnames:

1. Applies Unicode NFD decomposition.
2. Removes combining marks, thereby removing most diacritics.
3. Removes everything except Unicode letters and decimal digits.
4. Lowercases with Locale.ROOT.

The reduceString operation used to recognize weak titles lowercases and removes
everything except Unicode letters and digits. Unlike simplifyString, it does not
explicitly decompose or remove diacritics.

### 5.4 Lucene tokenization

Titles and sources are tokenized with Lucene StandardAnalyzer and converted to
sets: token frequency and token order are discarded for reranking.

The analyzer uses this case-insensitive stop-word set:

    a, an, and, are, as, at, be, but, by, for,
    if, in, into, is, it, no, not, of, on, or,
    such, that, the, their, then, there, these, they,
    this, to, was, will, with

### 5.5 Author surnames

For each author string, the text before the first comma is treated as the surname.
The surname is passed through simplifyString. Empty values and normalized surnames
of length 1 are discarded. Duplicate surnames are collapsed because the result is
a set.

### 5.6 DiVA source

The DiVA source string is the first available value, longer than three trimmed
characters, in this order:

1. Journal
2. Host publication
3. Series name

If none qualifies, the source is the empty string.

### 5.7 Weak titles

A DiVA title is weak when it is null, has at most three trimmed characters, or its
reduceString value equals the reduceString value of one of the following labels:

    [Not Available], Afterword, Aktuellt, Avslutning, Book review,
    Commentary, Conclusion, Conclusions, Correction, Corrigendum,
    Debatt, Discussion, Editorial, Editorial Introduction, Efterord,
    Epilog, Epilogue, Erratum, Foreword, Från redaktionen, Förord,
    Guest editorial, In memoriam, Indledning, Inledning, Introduction,
    Introduktion, Invited, Kommentar, Letter, Letter to the Editor,
    Note, Preface, Re, Recension, Recension av, Recensioner, Reply,
    Response, Review, Slutord, Untitled, Vorwort

Weakness is determined only from the DiVA title. A weak title is still assigned a
diagnostic title similarity, but that similarity is excluded from the total score.

## 6. Identifier matching

### 6.1 SciVal identifier indexes

For each SciVal record with a non-empty raw EID, the program stores:

- raw EID to SciVal record;
- normalized EID to the set of raw EIDs having that value;
- normalized DOI to the set of raw EIDs having that value;
- normalized PMID to the set of raw EIDs having that value.

The indexes are multi-valued. A duplicate DOI or PMID therefore remains visible as
an ambiguity rather than overwriting an earlier mapping.

### 6.2 Resolution for one DiVA record

This stage is executed only for eligible DiVA posts. Let \(M_1,\ldots,M_k\) be the
non-empty SciVal EID sets found from the DiVA EID,
DOI, and PMID, where \(0 \le k \le 3\).

- If \(k=0\), there is no identifier match.
- Otherwise compute

$$
I = \bigcap_{j=1}^{k} M_j.
$$

- If \(\lvert I\rvert=1\), the sole EID is an exact identifier match.
- If \(\lvert I\rvert=0\) or \(\lvert I\rvert>1\), the result is an identifier
  conflict. The diagnostic candidate set is

$$
U = \bigcup_{j=1}^{k} M_j.
$$

Thus, a multi-valued DOI can still resolve exactly when another identifier narrows
their intersection to one EID. A single multi-valued identifier without other
evidence is a conflict.

### 6.3 Exact-match suspicion

An exact identifier match is marked suspect when either condition holds:

1. both years are valid and their absolute difference is greater than 2; or
2. the DiVA title is not weak, both titles are present, and the title similarity
   defined in Section 9 is below 0.15.

An exact suspect remains linked to its identifier-selected EID. Document-type and
related-work conflicts do not override identifier matches. Suspect exact matches
are excluded from calibration.

All exact and exact-suspect EIDs are nevertheless considered identifier-claimed
targets during later collision checks.

## 7. Lucene candidate index

One Lucene document is added for every SciVal record with a non-empty EID.

| Lucene field | Field type | Contents |
|---|---|---|
| EID | StoredField | Raw EID |
| record_type | StoredField | SciVal document type; not used in retrieval |
| year | StringField, stored | Valid year or -1 |
| title | TextField, stored | SciVal title |
| source | TextField, stored | SciVal source title |
| familyNames | Repeated StringField | Normalized unique surnames |

The index is recreated on every run. No custom Lucene Similarity is installed; the
IndexSearcher uses the Lucene version's configured default.

## 8. Candidate retrieval

Retrieval is attempted only when the DiVA year is valid. Otherwise the candidate
list is empty.

A disjunction for a field is a BooleanQuery containing one SHOULD TermQuery per
non-empty term. It therefore means "at least one of these terms" when used as a
required subquery.

For DiVA year \(y\), every query has this mandatory clause:

$$
\operatorname{year}(s) \in \{y-1,y,y+1\}.
$$

### 8.1 Ordinary DiVA title

For a non-weak title:

- at least one analyzed title token is mandatory;
- if the DiVA author-surname set is non-empty, at least one surname is mandatory;
- if source tokens exist, their disjunction is optional and only boosts Lucene
  ranking.

If the title query has no terms, retrieval returns no candidates.

In Boolean-query notation:

$$
Q_{\text{ordinary}}
=
\operatorname{MUST}(Q_{\text{year}})
\land
\operatorname{MUST}(Q_{\text{title}})
\land
\begin{cases}
\operatorname{MUST}(Q_{\text{author}}), & Q_{\text{author}}\text{ exists},\\
\text{no author clause}, & \text{otherwise},
\end{cases}
$$

with \(Q_{\text{source}}\) added as SHOULD when it exists.

### 8.2 Weak DiVA title

For a weak title, both the author and source disjunctions must exist. If either is
missing, retrieval returns no candidates. The query is:

$$
Q_{\text{weak}}
=
\operatorname{MUST}(Q_{\text{year}})
\land
\operatorname{MUST}(Q_{\text{author}})
\land
\operatorname{MUST}(Q_{\text{source}}).
$$

The weak title itself is not included in the Lucene query.

### 8.3 Retrieval result

Lucene returns at most 25 hits. Hits are deduplicated by raw EID while preserving
their first-hit order. Records that cannot be found in the raw-EID record map are
dropped.

Lucene score is not part of the bibliographic reranker score. It is used only as
the tie-breaker when two candidates have exactly equal reranker scores.

## 9. Pairwise feature definitions

Feature value \(-1\) means unavailable. Otherwise all numeric similarities are in
\([0,1]\).

### 9.1 Thresholded normalized Levenshtein similarity

For non-empty normalized strings \(x\) and \(y\), let

$$
m = \max(\lvert x\rvert,\lvert y\rvert)
$$

and, for threshold \(\tau\),

$$
a_\tau = \left\lfloor (1-\tau)m + 10^{-12}\right\rfloor.
$$

Let \(\delta(x,y)\) be Levenshtein edit distance. The helper returns

$$
L_\tau(x,y)=
\begin{cases}
1-\dfrac{\delta(x,y)}{m}, & \delta(x,y)\le a_\tau,\\[6pt]
-1, & \delta(x,y)>a_\tau.
\end{cases}
$$

The implementation uses the library's bounded-distance call with
\(a_\tau+1\) as its limit. Missing or empty input also returns \(-1\).

Text similarity calls this helper with \(\tau=0.10\). For present text, a returned
\(-1\) is converted to character score 0. Thus the effective character feature is

$$
C(x,y)=
\begin{cases}
L_{0.10}(\operatorname{simplify}(x),\operatorname{simplify}(y)),
& L_{0.10}\ge 0,\\
0, & L_{0.10}<0.
\end{cases}
$$

### 9.2 Token Dice similarity

Let \(X\) and \(Y\) be the unique StandardAnalyzer token sets. When both are
non-empty:

$$
D(X,Y)=\frac{2\lvert X\cap Y\rvert}{\lvert X\rvert+\lvert Y\rvert}.
$$

The base text similarity is

$$
B(x,y)=0.6D(X,Y)+0.4C(x,y).
$$

If either token set is empty, text similarity is just \(C(x,y)\).

### 9.3 Title similarity and subtitle containment

For titles, define

$$
H(X,Y)=\frac{\lvert X\cap Y\rvert}{\min(\lvert X\rvert,\lvert Y\rvert)}
$$

and

$$
R(X,Y)=\frac{\min(\lvert X\rvert,\lvert Y\rvert)}
{\max(\lvert X\rvert,\lvert Y\rvert)}.
$$

The subtitle containment boost is eligible only when all conditions hold:

1. both token sets contain at least 4 tokens;
2. \(\lvert X\cap Y\rvert\ge 3\);
3. \(H(X,Y)\ge 0.85\);
4. \(R(X,Y)\ge 0.50\);
5. there is no related-work conflict as defined in Section 10.

The title feature is then

$$
T(x,y)=
\begin{cases}
\max\left(B(x,y),0.95H(X,Y)\right), & \text{if the boost is eligible},\\
B(x,y), & \text{otherwise}.
\end{cases}
$$

### 9.4 Source similarity

Source similarity uses the same base text formula but never applies the subtitle
containment boost:

$$
S_{\text{source}}(x,y)=B(x,y).
$$

### 9.5 Author similarity

For non-empty normalized surname sets \(A_d\) and \(A_s\):

$$
S_{\text{author}}(d,s)=
\frac{2\lvert A_d\cap A_s\rvert}{\lvert A_d\rvert+\lvert A_s\rvert}.
$$

If either set is empty, the feature is \(-1\).

### 9.6 Year similarity

For valid years \(y_d\) and \(y_s\):

$$
S_{\text{year}}(d,s)=
\begin{cases}
1, & \lvert y_d-y_s\rvert=0,\\
0.5, & \lvert y_d-y_s\rvert=1,\\
0, & \lvert y_d-y_s\rvert\ge 2.
\end{cases}
$$

If either year is invalid, the feature is \(-1\).

## 10. Related-work title conflict

The algorithm assigns each title one related-work kind. Whitespace is collapsed
and matching is case-insensitive. Rules are evaluated in this order:

1. VISUAL_ABSTRACT if the title contains visual abstract.
2. CORRECTION if the title starts with correction, corrigendum, erratum, addendum,
   or rättelse.
3. RESPONSE_OR_COMMENT if any of these tests is true:
   - starts with reply, response, author response, answer to the letter, comment on,
     commentary on, letter to, correspondence to, re: followed by a space, re.
     followed by a space, replik, svar på, or kommentar till;
   - contains letter to the editor, letter to editor, reply by authors, response to
     comment, response letter, : reply, or [reply].
4. EDITORIAL if the normalized title equals editorial or starts with editorial:
   or editorial followed by a space.
5. The empty kind otherwise.

For title kinds \(K_d\) and \(K_s\), related-work conflict is

$$
C_{\text{related}}
=
(K_d\ne K_s)
\land
(K_d\ne\varnothing \lor K_s\ne\varnothing).
$$

A conflict suppresses the subtitle-containment boost and forces automatic
sufficient evidence to false. It does not force reviewable evidence to false, so
the pair can still become AMBIGUOUS.

## 11. Partial document-type compatibility

Type labels are trimmed and lowercased.

The supported DiVA categories are:

- article: Artikel i tidskrift or Artikel, forskningsöversikt;
- conference: Konferensbidrag.

Every other DiVA publication type produces UNKNOWN compatibility.

A SciVal record is article-like when either:

- its document type is Editorial, Article in Press, Article, Letter, Note,
  Data Paper, Review, or Short Survey; or
- its source type is Journal.

A SciVal record is conference-like when either:

- its document type is Conference Paper; or
- its source type is Conference Proceeding.

If the SciVal record is both article-like and conference-like, or neither, its
signal is internally non-exclusive and compatibility is UNKNOWN.

Otherwise:

| DiVA type | Exclusive SciVal signal | Result |
|---|---|---|
| Article | Article-like | COMPATIBLE |
| Article | Conference-like | CONFLICT |
| Conference | Conference-like | COMPATIBLE |
| Conference | Article-like | CONFLICT |

Compatibility is only an automatic text-match gate. It never overrides an exact
identifier match.

## 12. Total reranker score

Let the available numeric features be:

- \(T\): title similarity;
- \(A\): author similarity;
- \(S\): source similarity;
- \(Y\): year similarity.

Define availability indicators:

$$
I_T=
\begin{cases}
1, & \text{DiVA title is not weak and }T\ge0,\\
0, & \text{otherwise},
\end{cases}
$$

$$
I_A=\mathbf{1}[A\ge0],\qquad
I_S=\mathbf{1}[S\ge0],\qquad
I_Y=\mathbf{1}[Y\ge0].
$$

For document type, define

$$
(I_D,D)=
\begin{cases}
(1,1), & \text{COMPATIBLE},\\
(1,0), & \text{CONFLICT},\\
(0,0), & \text{UNKNOWN}.
\end{cases}
$$

The total score is the available-weight normalized average

$$
\operatorname{score}(d,s)=
\frac{
0.55 I_TT+
0.25 I_AA+
0.15 I_SS+
0.05 I_YY+
0.10 I_DD
}{
0.55 I_T+
0.25 I_A+
0.15 I_S+
0.05 I_Y+
0.10 I_D
}.
$$

If the denominator is zero, the score is 0.

Consequences:

- A missing feature is omitted from both numerator and denominator.
- A weak DiVA title is omitted even though TITLE_SCORE is still calculated and
  written to the audit file.
- COMPATIBLE contributes value 1 with weight 0.10.
- CONFLICT contributes value 0 with weight 0.10 and therefore lowers the total.
- UNKNOWN contributes no weight.
- Related-work conflict does not directly subtract a numeric penalty. It suppresses
  the containment boost and blocks sufficient evidence.
- Lucene score is absent from this formula.

Candidates are sorted by descending total score. Exact total-score ties are sorted
by descending Lucene score.

## 13. Field-evidence gates

The score alone is never enough for automatic matching or review.

### 13.1 Weak DiVA title

For a weak title:

$$
\operatorname{sufficient}
=
(A\ge0.80)\land(S\ge0.80)\land(Y\ge0.50),
$$

$$
\operatorname{reviewable}
=
(A\ge0.20)\land(S\ge0.40).
$$

### 13.2 Ordinary DiVA title

When author and source features are both available:

$$
\operatorname{sufficient}
=
(T\ge0.65)\land\big((A\ge0.20)\lor(S\ge0.55)\big),
$$

$$
\operatorname{reviewable}
=
(T\ge0.30)\land\big((A\ge0.10)\lor(S\ge0.40)\big).
$$

When authors are unavailable but source is available:

$$
\operatorname{sufficient}=(T\ge0.85)\land(S\ge0.65),
$$

$$
\operatorname{reviewable}=(T\ge0.65)\land(S\ge0.45).
$$

When source is unavailable but authors are available:

$$
\operatorname{sufficient}=(T\ge0.80)\land(A\ge0.25),
$$

$$
\operatorname{reviewable}=(T\ge0.60)\land(A\ge0.10).
$$

When both authors and source are unavailable, the sequential implementation of
the two missing-field overrides makes both gates false.

Finally, sufficient is forced to false for either:

- document-type CONFLICT; or
- related-work conflict.

Reviewable is not forcibly cleared by these conflicts.

## 14. Calibration

### 14.1 Trusted identifier examples

An identifier match is trusted for calibration when it:

- belongs to an eligible DiVA post under Section 4.1;
- resolves to exactly one SciVal record;
- is not an identifier conflict; and
- is not an exact suspect under Section 6.3.

The DiVA record is retrieved and reranked without using its identifiers. If the
known EID is absent from the top-25 candidate list, the record contributes no
calibration example.

If the known EID is present, two examples are constructed:

1. Positive: the complete ranked candidate list and the known EID.
2. Hard negative: the same ranked list after removing every candidate whose EID
   equals the known EID. The hard negative is added only when this reduced list is
   non-empty.

The hard-negative margin is recomputed from the reduced list. It is not copied
from the positive example.

### 14.2 Split

Both examples derived from one DiVA record go to the same split:

$$
\operatorname{split}(d)=
\begin{cases}
\text{held-out}, & \operatorname{floorMod}(\operatorname{PID}(d),5)=0,\\
\text{training}, & \text{otherwise}.
\end{cases}
$$

This is approximately an 80/20 deterministic split.

Calibration retrieval recall at 25 is reported as

$$
\operatorname{recall@25}
=
\frac{\text{trusted records whose known EID was retrieved}}
{\text{trusted records}}.
$$

### 14.3 Candidate margin

For reranked candidate scores \(r_1\ge r_2\ge\cdots\):

$$
\operatorname{margin}=
\begin{cases}
0, & \text{no candidates},\\
r_1, & \text{one candidate},\\
r_1-r_2, & \text{at least two candidates}.
\end{cases}
$$

Treating a single candidate's score as its margin is an explicit implementation
choice.

### 14.4 Acceptance during calibration

For threshold pair \((\theta_s,\theta_m)\), the best candidate of an example is
accepted when

$$
\operatorname{sufficient}
\land
(r_1\ge\theta_s)
\land
(\operatorname{margin}\ge\theta_m).
$$

Calibration evaluation does not apply identifier-target collision checks,
shared-target post-processing, or the special 0.15 weak-title margin. Those later
rules can only make final matching more conservative.

An accepted example is counted as correct only when it is positive and its
top-ranked EID equals the known EID. The following are incorrect:

- an accepted positive whose top EID is not the known EID;
- every accepted hard negative.

Rejected examples are neither correct nor incorrect.

Let \(N_a\) be accepted examples, \(N_c\) correct accepted positives, \(N_i\)
incorrect accepted examples, \(N_p\) positive examples, \(N_h\) hard negatives,
and \(N_{ha}\) accepted hard negatives. Reported metrics are

$$
\operatorname{precision}=
\begin{cases}
\dfrac{N_c}{N_a}, & N_a>0,\\
0, & N_a=0,
\end{cases}
$$

$$
\operatorname{positiveCoverage}=
\begin{cases}
\dfrac{N_c}{N_p}, & N_p>0,\\
0, & N_p=0,
\end{cases}
$$

$$
\operatorname{hardNegativeAcceptance}=
\begin{cases}
\dfrac{N_{ha}}{N_h}, & N_h>0,\\
0, & N_h=0.
\end{cases}
$$

### 14.5 Grid search

The score grid is

$$
\theta_s\in\{0.65,0.66,\ldots,0.98\},
$$

and the margin grid is

$$
\theta_m\in\{0.05,0.06,\ldots,0.30\}.
$$

A pair is eligible only when it accepts at least one training example and

$$
\operatorname{precision}_{\text{training}}\ge0.995.
$$

Eligible pairs are selected lexicographically:

1. maximize the number of correct accepted positives;
2. if tied, minimize incorrect accepted examples;
3. if still tied, minimize \(\theta_s+\theta_m\).

If no pair is eligible, both thresholds are set to 1.01 and automatic text
matching is disabled.

### 14.6 Held-out fail-closed check

The selected pair is evaluated unchanged on the held-out split. If held-out
accepted count is zero or

$$
\operatorname{precision}_{\text{held-out}}<0.99,
$$

both operational thresholds are replaced by 1.01. Because total score and margin
cannot exceed 1, no text candidate can then become TEXT_AUTO_MATCH.

The selected thresholds and validation metrics are printed before this replacement,
along with a warning that automatic matching is disabled.

Calibration precision is an internal diagnostic over identifier-derived positives
and synthetic hard negatives. It is not a direct estimate of deployment precision
when source coverage, metadata quality, or the prevalence of true matches differs
for identifier-less DiVA records.

## 15. Initial classification

Eligibility has precedence over all match evidence, including identifiers:

1. An ineligible post becomes IGNORED without identifier resolution or text retrieval.
2. For an eligible post, an identifier conflict becomes IDENTIFIER_CONFLICT.
3. A unique identifier match becomes EXACT or EXACT_SUSPECT.
4. Only an eligible post with no identifier result enters text decision logic.

For an identifier-less DiVA record, let \(s_1\) be the highest-ranked candidate,
\(r_1\) its total score, and \(m\) its margin.

If there are no retrieval candidates, the result is NO_MATCH.

### 15.1 Identifier-target collision

The best candidate has an identifier collision when its normalized EID is already
the target of any unique identifier match, including an exact-suspect match.

Such a collision allows automatic matching only when:

$$
\neg\operatorname{weakTitle}
\land
(T\ge0.90).
$$

Without an identifier collision, this additional condition is vacuously true.

### 15.2 Effective thresholds

The effective score threshold is

$$
\theta_s^*=\max(0.65,\theta_s),
$$

and the normal effective margin threshold is

$$
\theta_m^*=\max(0.05,\theta_m).
$$

For a weak DiVA title:

$$
\theta_{m,\text{weak}}^*=\max(\theta_m^*,0.15).
$$

### 15.3 TEXT_AUTO_MATCH

The best candidate initially becomes TEXT_AUTO_MATCH exactly when:

$$
\operatorname{sufficient}
\land
(r_1\ge\theta_s^*)
\land
(m\ge\theta_m^*)
\land
\operatorname{collisionAllowsAutomaticMatch},
$$

using \(\theta_{m,\text{weak}}^*\) instead of \(\theta_m^*\) for weak titles.

### 15.4 AMBIGUOUS

If automatic acceptance fails, the best candidate becomes AMBIGUOUS exactly when:

$$
\operatorname{reviewable}
\land
(r_1\ge0.60).
$$

AMBIGUOUS does not require the automatic margin, type compatibility, absence of a
related-work conflict, or absence of a target collision.

### 15.5 NO_MATCH

Every remaining text case becomes NO_MATCH:

- no candidates;
- insufficient evidence even for review; or
- reviewable evidence whose total score is below 0.60.

For NO_MATCH, the public EID column is empty. If a best candidate existed, its
metadata and features are retained as diagnostics, and its EID is included in the
NOTE field.

## 16. Shared-target post-processing

After initial classification, proposals are grouped by normalized EID. A proposal
is a TEXT_AUTO_MATCH or AMBIGUOUS result with a non-empty EID. NO_MATCH records do
not participate. IGNORED records cannot be proposals because they never enter text
matching.

For every group of at least two DiVA records:

1. add TEXT_TARGET_SHARED to every proposal's collision diagnostic;
2. downgrade an automatic match to AMBIGUOUS when either:
   - its DiVA title is weak; or
   - its title feature is below 0.90.

A non-weak automatic match with title feature at least 0.90 remains automatic even
when its EID is shared. Consequently, the algorithm permits multiple DiVA
registrations of an apparently identical publication.

Collision labels are:

- empty: no detected target collision;
- IDENTIFIER_LINK: the proposed EID already has a unique identifier link;
- TEXT_TARGET_SHARED: several text proposals use the EID;
- IDENTIFIER_LINK+TEXT_TARGET_SHARED: both conditions hold.

## 17. Final statuses

| Status | Meaning |
|---|---|
| IGNORED | The DiVA publication-inclusion policy marked the post ineligible before identifier resolution, calibration, retrieval, and reranking |
| EXACT | A unique identifier intersection selected the EID and no severe title/year contradiction was detected |
| EXACT_SUSPECT | A unique identifier intersection selected the EID, but the exact-match suspicion rule fired |
| IDENTIFIER_CONFLICT | Identifier mappings existed but their intersection did not contain exactly one EID |
| TEXT_AUTO_MATCH | The best text candidate passed evidence, calibrated score, margin, and collision gates after shared-target post-processing |
| AMBIGUOUS | A reviewable best candidate scored at least 0.60 but was not safe for automatic acceptance |
| NO_MATCH | No identifier result and no candidate met the review rule |

IGNORED is an audit classification, not a failed match and not evidence that the
post is absent from SciVal. EXACT and EXACT_SUSPECT are links even when their pairwise document type or
related-work diagnostics conflict.

## 18. Audit outputs

The program writes a TSV and an XLSX file at the paths supplied to
`runMatchingPipeline`. Both contain the same 31 columns, in the same order, with one
header row and one data row per parsed DiVA
record. Embedded tabs, line feeds, and carriage returns in text values are replaced
with spaces in both formats.

### 18.1 TSV

The TSV is encoded as UTF-8. Matching scores are formatted with four digits and
expected indicator values with six digits after the decimal point. All values,
including Boolean and numeric values, are serialized as text by the tab-separated
format.

### 18.2 XLSX

The XLSX file contains one sheet named `Matches`. It is written with Apache POI
`SXSSFWorkbook`, using a 100-row in-memory window, compressed temporary files, and
inline rather than shared strings. Temporary SXSSF files are disposed after the
workbook is written. The first row is frozen and an auto-filter spans the complete
data range.

Excel cell types are preserved:

- PID, score fields, observed indicators, expected indicators, and reference-set
  sizes are numeric;
- RELATED_WORK_CONFLICT and fallback flags are Boolean;
- statuses, identifiers, compatibility labels, metadata, collision labels, and notes
  are strings.

Score cells retain their numeric precision and use the display format `0.0000`.
Excel limits a cell to 32,767 characters. A longer text value is shortened to fit and
ends with ` [TRUNCATED]`; the TSV value is not subject to this Excel-specific limit.

### 18.3 Common column schema

| Column | Meaning |
|---|---|
| PID | DiVA PID |
| STATUS | Final status from Section 17 |
| EID | Selected/proposed EID for exact, automatic, and ambiguous results; empty for IGNORED, NO_MATCH, and IDENTIFIER_CONFLICT |
| SCORE | Total reranker score; 0 when no candidate features exist |
| MARGIN | Best-minus-runner-up margin; 0 for identifier results and no candidates |
| TITLE_SCORE | Title feature, or -1 when unavailable |
| AUTHORS_SCORE | Author feature, or -1 when unavailable |
| SOURCE_SCORE | Source feature, or -1 when unavailable |
| YEAR_SCORE | Year feature, or -1 when unavailable |
| TYPE_COMPATIBILITY | COMPATIBLE, UNKNOWN, or CONFLICT |
| RELATED_WORK_CONFLICT | true when the title kinds conflict |
| RUNNER_UP_EID | Second-ranked EID; empty when unavailable or for identifier results |
| TARGET_COLLISION | Collision label or empty |
| DIVA_TITLE | Original DiVA title |
| SCIVAL_TITLE | Diagnostic SciVal title |
| DIVA_SOURCE | Source selected by the precedence rule in Section 5.6 |
| SCIVAL_SOURCE | Diagnostic SciVal source title |
| DIVA_PUBLICATION_TYPE | DiVA publication type |
| SCIVAL_DOCUMENT_TYPE | Diagnostic SciVal document type |
| SCIVAL_SOURCE_TYPE | Diagnostic SciVal source type |
| NOTE | Inclusion-filter reason, identifier evidence, candidate count, rejection reason, or diagnostic best EID |
| OBSERVED_TOP10 | 1 when the accepted SciVal publication is in Top 10%, otherwise 0 |
| OBSERVED_TOP50 | 1 when the accepted SciVal publication is in Top 50%, otherwise 0 |
| OBSERVED_IS_INTERNATIONAL | 1 when the accepted publication involves at least two countries, otherwise 0 |
| EXPECTED_TOP10 | Top-10 proportion in the publication-specific Swedish subject reference, or its fallback |
| EXPECTED_TOP50 | Top-50 proportion in the publication-specific Swedish subject reference, or its fallback |
| EXPECTED_INTERNATIONAL | International-collaboration proportion in the subject- and year-restricted Swedish reference, or its fallback |
| REFERENCE_SET_SIZE | Number of unique Swedish non-UMU publications in the inclusive ASJC/topic-cluster reference set |
| INTERNATIONAL_REFERENCE_SET_SIZE | Number of those reference publications within publication year ±1 |
| USED_CITATION_FALLBACK | true when REFERENCE_SET_SIZE is below 25 |
| USED_INTERNATIONAL_FALLBACK | true when INTERNATIONAL_REFERENCE_SET_SIZE is below 20 |

Indicator columns are populated only for `EXACT`, `EXACT_SUSPECT`, and
`TEXT_AUTO_MATCH`. They are blank for `IGNORED`, `IDENTIFIER_CONFLICT`,
`AMBIGUOUS`, and `NO_MATCH`; diagnostic candidates never receive indicators.

The diagnostic SciVal record is:

- the identifier-selected record for EXACT and EXACT_SUSPECT;
- the selected/proposed best record for TEXT_AUTO_MATCH and AMBIGUOUS;
- the rejected best record for NO_MATCH when one exists;
- absent for IGNORED, IDENTIFIER_CONFLICT, and NO_MATCH with no candidates.

## 19. Reference pseudocode

    load SciVal records, excluding Retracted and Abstract Report
    abort if a SciVal record repeats an AFID
    load DiVA records

    for each DiVA post:
        inclusion = DefaultPubIncludingAheadOfPrint.consideredPub(post)
        post.setStatusInModel(inclusion)
        if inclusion.isIgnorerad():
            mark post ineligible for every matching and calibration stage

    build normalized EID/DOI/PMID -> set<EID> indexes
    for each eligible DiVA record:
        resolve identifier mappings by set intersection
        record unique identifier targets for collision checks

    build Lucene index

    for each eligible, non-suspect unique identifier match:
        retrieve and rerank without identifiers
        if known EID is retrieved:
            add positive example
            remove known EID and, if candidates remain, add hard negative
            keep both examples in PID-modulo-5 split

    grid-search score and margin on training examples
    if no pair reaches 0.995 training precision:
        disable automatic text matching
    else evaluate selected pair on held-out examples
        if accepted == 0 or precision < 0.99:
            disable automatic text matching

    for each DiVA record:
        if ineligible:
            IGNORED
        else if identifier conflict:
            IDENTIFIER_CONFLICT
        else if unique identifier match:
            EXACT or EXACT_SUSPECT
        else:
            retrieve up to 25 candidates
            compute features and total score
            sort by total score, then Lucene score
            if automatic evidence, score, margin, and identifier-collision gates pass:
                TEXT_AUTO_MATCH
            else if reviewable evidence and score >= 0.60:
                AMBIGUOUS
            else:
                NO_MATCH

    group automatic and ambiguous proposals by normalized EID
    mark shared targets
    downgrade weak-title or title-score-below-0.90 automatic matches in shared groups

    collect unique EIDs from EXACT, EXACT_SUSPECT, and TEXT_AUTO_MATCH
    build the Swedish non-UMU subject-reference indexes once
    calculate observed and expected indicators once per accepted EID

    write the UTF-8 TSV
    write the streaming XLSX workbook with the same rows and columns

## 20. Important interpretation limits

- The reranker score is an engineered similarity score, not a calibrated posterior
  probability.
- Calibration is conditional on identifier-linked records whose known EID was
  retrieved. The hard negatives improve rejection testing but do not reproduce all
  properties or prevalence of genuinely unmatched DiVA records.
- Retrieval requires a valid DiVA year and is restricted to year \(\pm1\).
- Token features use sets, so word order and frequency do not affect Dice scores.
- Author comparison assumes that the text before the first comma is a surname.
- The document-type gate intentionally covers only the article/conference
  distinction. All other DiVA types and non-exclusive SciVal signals are UNKNOWN.
- Related-work recognition is a finite phrase rule, not a general semantic
  classifier.
- Exact identifiers take precedence even when bibliographic diagnostics disagree.
- Shared EIDs are permitted for near-identical non-weak titles; this is not a global
  assignment or deduplication algorithm.
