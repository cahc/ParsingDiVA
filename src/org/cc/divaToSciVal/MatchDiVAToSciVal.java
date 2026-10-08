package org.cc.divaToSciVal;

import cc.FilePathConstants;
import cc.analysis.scival.SciValParser;
import info.debatty.java.stringsimilarity.Levenshtein;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.cc.NorskaModellen.ConsideredPublications;
import org.cc.NorskaModellen.DefaultPubIncludingAheadOfPrint;
import org.cc.diva.Author;
import org.cc.diva.CreateDivaTable;
import org.cc.diva.Post;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;

import static cc.analysis.TopicsAndTopicClusters.RecordToTopicsAndCitationIndicators.getSciValExcelFiles;

/**
 * Links bibliographic records from DiVA to records exported from SciVal. Candidate
 * retrieval is deliberately permissive; automatic acceptance is a separate,
 * conservative decision because related publications often share authors, source,
 * year, and substantial title vocabulary.
 *
 * <h2>Pipeline</h2>
 * <ol>
 *     <li><b>DiVA eligibility.</b> Every DiVA post is classified by
 *     {@link DefaultPubIncludingAheadOfPrint}. Posts marked ignored are emitted as
 *     {@code IGNORED} for audit purposes but never enter identifier resolution,
 *     calibration, candidate retrieval, reranking, or collision handling.</li>
 *     <li><b>Identifier resolution.</b> DOI, PMID, and EID values are normalized and
 *     looked up in indexes that retain every mapped EID. Agreeing identifiers produce
 *     {@code EXACT}; an ambiguous value or disagreement between identifiers produces
 *     {@code IDENTIFIER_CONFLICT} instead of silently choosing one record.</li>
 *     <li><b>Exact-match diagnostics.</b> Identifier matches with a large year
 *     disagreement or an extremely low non-generic title similarity remain linked as
 *     {@code EXACT_SUSPECT}. They are excluded from calibration, since translations,
 *     bad identifiers, and erroneous metadata should not teach the text matcher.</li>
 *     <li><b>Candidate retrieval.</b> SciVal title, source, year, and normalized author
 *     surnames are indexed in Lucene. Records without a usable identifier retrieve at
 *     most 25 candidates from year +/- 1. Ordinary titles require title and author
 *     terms, with source terms as a boost. Generic titles such as "Editorial" or
 *     "Introduction" instead require both author and source terms.</li>
 *     <li><b>Interpretable reranking.</b> Candidates are scored from subtitle-aware
 *     title similarity, Dice overlap of author surnames, source similarity, year
 *     proximity, and a small document-type compatibility term. Missing fields are
 *     omitted from the weighted denominator. The subtitle-containment boost requires
 *     high token containment and a reasonable shorter/longer title-length ratio.
 *     Ordinary automatic matches require
 *     substantially stronger title and corroborating author/source evidence than the
 *     review queue. Generic titles require very strong non-title evidence. Response,
 *     correction, and derivative-title markers must agree on both sides so that a
 *     reply containing an original title is not linked to the original article. A clear
 *     journal/conference conflict can be reviewed but can never be accepted
 *     automatically; unknown or internally mixed type metadata remains neutral.</li>
 *     <li><b>Calibration with hard negatives.</b> Trusted identifier matches are run
 *     through retrieval without identifiers. Each retrieved known match is a positive
 *     example; removing its known EID creates a hard no-match example from the most
 *     plausible related records. PID modulo 5 keeps both examples from one DiVA record
 *     together in an 80/20 calibration/validation split. A small grid search selects
 *     score and runner-up-margin thresholds that meet the target precision, but fixed
 *     safety floors prevent calibration from accepting low scores or ties. A small
 *     training precision buffer reduces threshold-selection optimism; automatic text
 *     matching is disabled if the independent validation split misses the stated
 *     precision target.</li>
 *     <li><b>Collision checks.</b> A text candidate already claimed by an identifier
 *     match, or proposed for several DiVA records, is accepted automatically only for
 *     a non-generic title with exceptionally strong title agreement. Otherwise it is
 *     sent to {@code AMBIGUOUS}. This is not a strict one-to-one constraint: genuinely
 *     duplicated DiVA registrations may still link to the same EID when their titles
 *     provide near-identity evidence.</li>
 *     <li><b>Reference indicators.</b> Accepted SciVal matches are enriched with
 *     observed citation/collaboration indicators and publication-specific expected
 *     values from a Swedish, non-UMU subject reference population.</li>
 * </ol>
 *
 * <p>Final statuses are {@code IGNORED}, {@code EXACT}, {@code EXACT_SUSPECT},
 * {@code TEXT_AUTO_MATCH}, {@code AMBIGUOUS}, {@code NO_MATCH}, and
 * {@code IDENTIFIER_CONFLICT}. Identical audit columns are written to a UTF-8
 * tab-separated file and a streaming XLSX workbook. They contain the chosen and
 * runner-up EIDs, total/margin/field scores, type compatibility, collision reason, and
 * the original DiVA and SciVal titles and type labels. Calibration precision is an
 * internal validation diagnostic, not an estimate of real-world precision when the
 * unmatched population or source coverage differs from the calibration examples.</p>
 */
public class MatchDiVAToSciVal {

    private static final Levenshtein similarity = new Levenshtein();
    private static final int CANDIDATE_LIMIT = 25;
    private static final double TARGET_CALIBRATION_PRECISION = 0.99;
    private static final double CALIBRATION_PRECISION_BUFFER = 0.005;
    private static final double SAFETY_MINIMUM_SCORE = 0.65;
    private static final double SAFETY_MINIMUM_MARGIN = 0.05;
    private static final double MINIMUM_AUTOMATIC_TITLE = 0.65;
    private static final double DUPLICATE_TARGET_MINIMUM_TITLE = 0.90;
    private static final double WEAK_TITLE_MINIMUM_MARGIN = 0.15;
    private static final double TITLE_CONTAINMENT_THRESHOLD = 0.85;
    private static final double TITLE_CONTAINMENT_LENGTH_RATIO = 0.50;
    private static final double MINIMUM_REVIEW_SCORE = 0.60;
    private static final double TITLE_WEIGHT = 0.55;
    private static final double AUTHORS_WEIGHT = 0.25;
    private static final double SOURCE_WEIGHT = 0.15;
    private static final double YEAR_WEIGHT = 0.05;
    private static final double DOCUMENT_TYPE_WEIGHT = 0.10;
    private static final int EXCEL_ROW_WINDOW = 100;
    private static final int EXCEL_MAX_CELL_TEXT_LENGTH = 32767;
    private static final String[] OUTPUT_HEADERS = {
            "PID", "STATUS", "EID", "SCORE", "MARGIN", "TITLE_SCORE", "AUTHORS_SCORE",
            "SOURCE_SCORE", "YEAR_SCORE", "TYPE_COMPATIBILITY", "RELATED_WORK_CONFLICT",
            "RUNNER_UP_EID", "TARGET_COLLISION", "DIVA_TITLE", "SCIVAL_TITLE", "DIVA_SOURCE",
            "SCIVAL_SOURCE", "DIVA_PUBLICATION_TYPE", "SCIVAL_DOCUMENT_TYPE",
            "SCIVAL_SOURCE_TYPE", "NOTE",
            "OBSERVED_TOP10", "OBSERVED_TOP50", "OBSERVED_IS_INTERNATIONAL",
            "EXPECTED_TOP10", "EXPECTED_TOP50", "EXPECTED_INTERNATIONAL",
            "REFERENCE_SET_SIZE", "INTERNATIONAL_REFERENCE_SET_SIZE",
            "USED_CITATION_FALLBACK", "USED_INTERNATIONAL_FALLBACK",
            "NORMALIZATION_ARTIFACT_GROUP", "CITATION_REFERENCE_SCOPE", "INTERNATIONAL_REFERENCE_SCOPE",
            "CITATION_REFERENCE_POPULATION_SIZE", "INTERNATIONAL_REFERENCE_POPULATION_SIZE"
    };

    private static final Set<String> SCIVAL_ARTICLE_DOCUMENT_TYPES = new HashSet<>(Arrays.asList(
            "editorial", "article in press", "article", "letter", "note",
            "data paper", "review", "short survey"));

    final static HashSet<String> weakTitles;

    static {

        weakTitles = new HashSet<>();
        weakTitles.add("[Not Available]".toLowerCase());
        weakTitles.add("Afterword".toLowerCase());
        weakTitles.add("Aktuellt".toLowerCase());
        weakTitles.add("Avslutning".toLowerCase());
        weakTitles.add("Book review".toLowerCase());
        weakTitles.add("Commentary".toLowerCase());
        weakTitles.add("Conclusion".toLowerCase());
        weakTitles.add("Conclusions".toLowerCase());
        weakTitles.add("Correction".toLowerCase());
        weakTitles.add("Corrigendum".toLowerCase());
        weakTitles.add("Debatt".toLowerCase());
        weakTitles.add("Discussion".toLowerCase());
        weakTitles.add("Editorial".toLowerCase());
        weakTitles.add("Editorial".toLowerCase());
        weakTitles.add("Editorial Introduction".toLowerCase());
        weakTitles.add("Efterord".toLowerCase());
        weakTitles.add("Epilog".toLowerCase());
        weakTitles.add("Epilogue".toLowerCase());
        weakTitles.add("Erratum".toLowerCase());
        weakTitles.add("Foreword".toLowerCase());
        weakTitles.add("Foreword".toLowerCase());
        weakTitles.add("Från redaktionen".toLowerCase());
        weakTitles.add("Förord".toLowerCase());
        weakTitles.add("Förord".toLowerCase());
        weakTitles.add("Guest editorial".toLowerCase());
        weakTitles.add("In memoriam".toLowerCase());
        weakTitles.add("Indledning".toLowerCase());
        weakTitles.add("Inledning".toLowerCase());
        weakTitles.add("Introduction".toLowerCase());
        weakTitles.add("Introduktion".toLowerCase());
        weakTitles.add("Introduktion".toLowerCase());
        weakTitles.add("Invited".toLowerCase());
        weakTitles.add("Kommentar".toLowerCase());
        weakTitles.add("Letter".toLowerCase());
        weakTitles.add("Letter to the Editor".toLowerCase());
        weakTitles.add("Note".toLowerCase());
        weakTitles.add("Preface".toLowerCase());
        weakTitles.add("Re".toLowerCase());
        weakTitles.add("Recension".toLowerCase());
        weakTitles.add("Recension av".toLowerCase());
        weakTitles.add("Recensioner".toLowerCase());
        weakTitles.add("Reply".toLowerCase());
        weakTitles.add("Response".toLowerCase());
        weakTitles.add("Review".toLowerCase());
        weakTitles.add("Slutord".toLowerCase());
        weakTitles.add("Untitled".toLowerCase());
        weakTitles.add("Vorwort".toLowerCase());

        Set<String> normalizedWeakTitles = new HashSet<>();
        for (String title : weakTitles) normalizedWeakTitles.add(reduceString(title));
        weakTitles.clear();
        weakTitles.addAll(normalizedWeakTitles);


    }


    public static boolean isWeakTitle(String s) {
        if (s == null || s.trim().length() <= 3) return true;
        return weakTitles.contains(reduceString(s));
    }

    public static String getSurname(String raw) {

        if (raw == null) return "";
        String[] splitted = raw.split(",", 2);
        return splitted[0];

    }

    public static String simplifyString(String s) {

        if (s == null) return null;

        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        temp = temp.replaceAll("\\p{M}+", "");
        temp = temp.replaceAll("[^\\p{L}\\p{Nd}]", "");
        return temp.toLowerCase(Locale.ROOT);

    }

    public static String reduceString(String s) {

        if (s == null) return null;
        return s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\d]", "");

    }

    public static double similarityThresholded(String s, String t, double similarityThreshold) {
        if (similarityThreshold < 0 || similarityThreshold > 1) {
            throw new IllegalArgumentException("Similarity threshold must be between 0 and 1");
        }
        if (s == null || t == null || s.isEmpty() || t.isEmpty()) return -1;
        int max = Math.max(s.length(), t.length());
        int allowedEdits = (int) Math.floor((1 - similarityThreshold) * max + 1.0e-12);
        double edits = similarity.distance(s, t, allowedEdits + 1);
        if (edits > allowedEdits) return -1;
        return 1 - (edits / max);

    }

    enum MatchStatus {
        IGNORED, EXACT, EXACT_SUSPECT, TEXT_AUTO_MATCH, AMBIGUOUS, NO_MATCH,
        IDENTIFIER_CONFLICT
    }

    enum DocumentTypeCompatibility {
        COMPATIBLE, UNKNOWN, CONFLICT
    }

    private static final class IdentifierIndexes {
        final Map<String, SciValParser.SciValRecord> recordsByEid = new HashMap<>();
        final Map<String, Set<String>> eidsByEid = new HashMap<>();
        final Map<String, Set<String>> eidsByDoi = new HashMap<>();
        final Map<String, Set<String>> eidsByPmid = new HashMap<>();

        IdentifierIndexes(List<SciValParser.SciValRecord> records) {
            for (SciValParser.SciValRecord record : records) {
                String eid = clean(record.getEID());
                if (eid.isEmpty()) continue;
                recordsByEid.put(eid, record);
                add(eidsByEid, normalizeEid(eid), eid);
                add(eidsByDoi, normalizeDoi(record.getDOI()), eid);
                add(eidsByPmid, normalizePmid(record.getPMID()), eid);
            }
        }

        private static void add(Map<String, Set<String>> index, String key, String eid) {
            if (key.isEmpty()) return;
            index.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(eid);
        }

        IdentifierMatch resolve(Post post) {
            List<Set<String>> mappings = new ArrayList<>(3);
            Set<String> evidence = new LinkedHashSet<>();
            addMapping(mappings, evidence, "EID", eidsByEid.get(normalizeEid(post.getEID())));
            addMapping(mappings, evidence, "DOI", eidsByDoi.get(normalizeDoi(post.getDOI())));
            addMapping(mappings, evidence, "PMID", eidsByPmid.get(normalizePmid(post.getPMID())));
            if (mappings.isEmpty()) return IdentifierMatch.none();

            Set<String> intersection = new LinkedHashSet<>(mappings.get(0));
            for (int i = 1; i < mappings.size(); i++) intersection.retainAll(mappings.get(i));
            if (intersection.size() == 1) {
                return IdentifierMatch.exact(recordsByEid.get(intersection.iterator().next()), evidence);
            }
            Set<String> union = new LinkedHashSet<>();
            for (Set<String> mapping : mappings) union.addAll(mapping);
            return IdentifierMatch.conflict(union, evidence);
        }

        private static void addMapping(List<Set<String>> mappings, Set<String> evidence,
                                       String type, Set<String> mappedEids) {
            if (mappedEids != null && !mappedEids.isEmpty()) {
                mappings.add(mappedEids);
                evidence.add(type);
            }
        }
    }

    private static final class IdentifierMatch {
        final boolean found;
        final boolean conflict;
        final SciValParser.SciValRecord record;
        final Set<String> candidateEids;
        final Set<String> evidence;

        private IdentifierMatch(boolean found, boolean conflict, SciValParser.SciValRecord record,
                                Set<String> candidateEids, Set<String> evidence) {
            this.found = found;
            this.conflict = conflict;
            this.record = record;
            this.candidateEids = candidateEids;
            this.evidence = evidence;
        }

        static IdentifierMatch none() {
            return new IdentifierMatch(false, false, null, Collections.<String>emptySet(),
                    Collections.<String>emptySet());
        }

        static IdentifierMatch exact(SciValParser.SciValRecord record, Set<String> evidence) {
            return new IdentifierMatch(true, false, record, Collections.<String>emptySet(), evidence);
        }

        static IdentifierMatch conflict(Set<String> candidates, Set<String> evidence) {
            return new IdentifierMatch(true, true, null, candidates, evidence);
        }
    }

    private static final class RetrievedCandidate {
        final SciValParser.SciValRecord record;
        final float luceneScore;

        RetrievedCandidate(SciValParser.SciValRecord record, float luceneScore) {
            this.record = record;
            this.luceneScore = luceneScore;
        }
    }

    private static final class CandidateFeatures {
        final double title;
        final double authors;
        final double source;
        final double year;
        final DocumentTypeCompatibility documentTypeCompatibility;
        final boolean relatedWorkConflict;
        final double total;
        final boolean sufficientEvidence;
        final boolean reviewableEvidence;

        CandidateFeatures(double title, double authors, double source, double year,
                          DocumentTypeCompatibility documentTypeCompatibility,
                          boolean relatedWorkConflict, double total,
                          boolean sufficientEvidence, boolean reviewableEvidence) {
            this.title = title;
            this.authors = authors;
            this.source = source;
            this.year = year;
            this.documentTypeCompatibility = documentTypeCompatibility;
            this.relatedWorkConflict = relatedWorkConflict;
            this.total = total;
            this.sufficientEvidence = sufficientEvidence;
            this.reviewableEvidence = reviewableEvidence;
        }
    }

    private static final class ScoredCandidate {
        final SciValParser.SciValRecord record;
        final CandidateFeatures features;
        final float luceneScore;

        ScoredCandidate(SciValParser.SciValRecord record, CandidateFeatures features,
                        float luceneScore) {
            this.record = record;
            this.features = features;
            this.luceneScore = luceneScore;
        }
    }

    private static final class CalibrationExample {
        final String knownEid;
        final List<ScoredCandidate> rankedCandidates;
        final boolean positive;

        private CalibrationExample(String knownEid, List<ScoredCandidate> rankedCandidates,
                                   boolean positive) {
            this.knownEid = knownEid;
            this.rankedCandidates = rankedCandidates;
            this.positive = positive;
        }

        static CalibrationExample positive(String knownEid,
                                           List<ScoredCandidate> rankedCandidates) {
            return new CalibrationExample(knownEid, rankedCandidates, true);
        }

        static CalibrationExample hardNegative(List<ScoredCandidate> rankedCandidates) {
            return new CalibrationExample("", rankedCandidates, false);
        }
    }

    private static final class Evaluation {
        final int examples;
        final int positiveExamples;
        final int hardNegativeExamples;
        final int accepted;
        final int correct;
        final int incorrect;
        final int acceptedHardNegatives;
        final double precision;
        final double coverage;
        final double hardNegativeAcceptanceRate;

        Evaluation(int examples, int positiveExamples, int hardNegativeExamples,
                   int accepted, int correct, int incorrect, int acceptedHardNegatives) {
            this.examples = examples;
            this.positiveExamples = positiveExamples;
            this.hardNegativeExamples = hardNegativeExamples;
            this.accepted = accepted;
            this.correct = correct;
            this.incorrect = incorrect;
            this.acceptedHardNegatives = acceptedHardNegatives;
            precision = accepted == 0 ? 0 : (double) correct / accepted;
            coverage = positiveExamples == 0 ? 0 : (double) correct / positiveExamples;
            hardNegativeAcceptanceRate = hardNegativeExamples == 0 ? 0
                    : (double) acceptedHardNegatives / hardNegativeExamples;
        }
    }

    private static final class CalibrationThresholds {
        final double minimumScore;
        final double minimumMargin;
        final Evaluation training;
        final boolean targetMet;

        CalibrationThresholds(double minimumScore, double minimumMargin, Evaluation training,
                              boolean targetMet) {
            this.minimumScore = minimumScore;
            this.minimumMargin = minimumMargin;
            this.training = training;
            this.targetMet = targetMet;
        }
    }

    private static final class MatchResult {
        final MatchStatus status;
        final String eid;
        final String runnerUpEid;
        final CandidateFeatures features;
        final double margin;
        final String note;
        final SciValParser.SciValRecord diagnosticRecord;
        final String targetCollision;

        MatchResult(MatchStatus status, String eid, String runnerUpEid,
                    CandidateFeatures features, double margin, String note,
                    SciValParser.SciValRecord diagnosticRecord, String targetCollision) {
            this.status = status;
            this.eid = eid;
            this.runnerUpEid = runnerUpEid;
            this.features = features;
            this.margin = margin;
            this.note = note;
            this.diagnosticRecord = diagnosticRecord;
            this.targetCollision = targetCollision;
        }

        SciValParser.SciValRecord acceptedRecord() {
            if(status == MatchStatus.EXACT
                    || status == MatchStatus.EXACT_SUSPECT
                    || status == MatchStatus.TEXT_AUTO_MATCH) {
                return diagnosticRecord;
            }
            return null;
        }

        MatchResult withStatus(MatchStatus replacementStatus, String extraNote,
                               String replacementCollision) {
            String combinedNote = note.isEmpty() ? extraNote : note + "; " + extraNote;
            return new MatchResult(replacementStatus, eid, runnerUpEid, features, margin,
                    combinedNote, diagnosticRecord, replacementCollision);
        }

        MatchResult withCollision(String replacementCollision) {
            return new MatchResult(status, eid, runnerUpEid, features, margin, note,
                    diagnosticRecord, replacementCollision);
        }
    }

    public static void main(String[] args) throws IOException {

        runMatchingPipeline("C:\\opt\\bibliometric_gui_static_data_files\\Raw_DiVA_export_20260930_102140.csv",FilePathConstants.SCIVAL_RAW_XLSX_LATEST,"DIVA_PID_TO_SCIVAL_EID.xlsx","DIVA_PID_TO_SCIVAL_EID.txt");
    }

    public static void runMatchingPipeline(String CSVFile, String PathToSciValData, String XLSX_OUTPUT_FILE, String TSV_OUTPUT_FILE) throws IOException {
        // Apply the shared analytical exclusions before matching and reference calculations.

        File[] files = getSciValExcelFiles(PathToSciValData);

        List<SciValParser.SciValRecord> sciValRecords = new ArrayList<>(10000);
        for (File file : files) {
            List<SciValParser.SciValRecord> parsed = SciValDocumentTypePolicy.readEligibleRecords(file.getAbsolutePath());
            sciValRecords.addAll(parsed);
            validateAfids(parsed);
        }
        System.out.println("Total SciVal records parsed: " + sciValRecords.size());

        String csvFile = CSVFile;
        CreateDivaTable divaTable = new CreateDivaTable(new File(csvFile));
        divaTable.parse();
        List<Post> posts = new ArrayList<>(divaTable.nrRows());
        for (int i = 0; i < divaTable.nrRows(); i++) {
            posts.add(new Post(divaTable.getRowInTable(i)));
        }
        System.out.println("Total DiVA records parsed: " + posts.size());

        ConsideredPublications publicationsToInclude = new DefaultPubIncludingAheadOfPrint();
        int ignoredCount = 0;
        for (Post post : posts) {
            post.setStatusInModel(publicationsToInclude.consideredPub(post));
            if (isIgnored(post)) ignoredCount++;
        }
        System.out.println("Eligible DiVA records: " + (posts.size() - ignoredCount));
        System.out.println("Ignored DiVA records: " + ignoredCount);

        IdentifierIndexes indexes = new IdentifierIndexes(sciValRecords);
        Map<Post, IdentifierMatch> identifierMatches = new HashMap<>();
        Set<String> identifierClaimedEids = new HashSet<>();
        int exactCount = 0;
        int conflictCount = 0;
        for (Post post : posts) {
            if (isIgnored(post)) {
                identifierMatches.put(post, IdentifierMatch.none());
                continue;
            }
            IdentifierMatch match = indexes.resolve(post);
            identifierMatches.put(post, match);
            if (match.conflict) conflictCount++;
            else if (match.found) {
                exactCount++;
                if (match.record != null) {
                    identifierClaimedEids.add(normalizeEid(match.record.getEID()));
                }
            }
        }
        System.out.println("Unique identifier matches: " + exactCount);
        System.out.println("Identifier conflicts/ambiguities: " + conflictCount);
        System.out.println("Unique DOI/PMID/EID keys: " + indexes.eidsByDoi.size() + "/"
                + indexes.eidsByPmid.size() + "/" + indexes.eidsByEid.size());

        StandardAnalyzer analyzer = new StandardAnalyzer(createStopWords());
        Path lucenePath = Paths.get("/tmp/temporaryLucene");
        Files.createDirectories(lucenePath);
        Directory luceneIndex = FSDirectory.open(lucenePath);
        buildSciValIndex(sciValRecords, analyzer, luceneIndex);
        IndexReader reader = DirectoryReader.open(luceneIndex);
        IndexSearcher searcher = new IndexSearcher(reader);
        BooleanQuery.setMaxClauseCount(3500);

        List<CalibrationExample> training = new ArrayList<>();
        List<CalibrationExample> validation = new ArrayList<>();
        Set<Post> suspectExactMatches = new HashSet<>();
        int trusted = 0;
        int retrievedKnown = 0;
        for (Post post : posts) {
            if (isIgnored(post)) continue;
            IdentifierMatch match = identifierMatches.get(post);
            if (!match.found || match.conflict || match.record == null) continue;
            if (hasBibliographicContradiction(post, match.record, analyzer)) {
                suspectExactMatches.add(post);
                continue;
            }
            trusted++;
            List<ScoredCandidate> ranked = scoreCandidates(post,
                    retrieveCandidates(post, analyzer, searcher, indexes.recordsByEid), analyzer);
            if (!containsEid(ranked, match.record.getEID())) continue;
            retrievedKnown++;
            List<CalibrationExample> split = Math.floorMod(post.getPID(), 5) == 0
                    ? validation : training;
            split.add(CalibrationExample.positive(match.record.getEID(), ranked));
            List<ScoredCandidate> hardNegatives = withoutEid(ranked, match.record.getEID());
            if (!hardNegatives.isEmpty()) {
                split.add(CalibrationExample.hardNegative(hardNegatives));
            }
        }

        double recall = trusted == 0 ? 0 : (double) retrievedKnown / trusted;
        System.out.printf(Locale.ROOT,
                "Calibration candidates: trusted=%d, retrieved=%d, recall@%d=%.4f%n",
                trusted, retrievedKnown, CANDIDATE_LIMIT, recall);
        CalibrationThresholds thresholds = calibrate(training);
        Evaluation heldOut = evaluate(validation, thresholds.minimumScore, thresholds.minimumMargin);
        printCalibration(thresholds, heldOut);
        if (thresholds.targetMet && (heldOut.accepted == 0
                || heldOut.precision + 1.0e-12 < TARGET_CALIBRATION_PRECISION)) {
            double disabledThreshold = 1.01;
            thresholds = new CalibrationThresholds(disabledThreshold, disabledThreshold,
                    thresholds.training, false);
        }

        List<MatchResult> results = new ArrayList<>(posts.size());
        for (Post post : posts) {
            IdentifierMatch exact = identifierMatches.get(post);
            MatchResult result;
            if (isIgnored(post)) {
                result = new MatchResult(MatchStatus.IGNORED, "", "", null, 0,
                        "publication inclusion filter="
                                + post.getStatusInModel().getStatusInModel(), null, "");
            } else if (exact.conflict) {
                result = new MatchResult(MatchStatus.IDENTIFIER_CONFLICT, "", "", null, 0,
                        "identifiers=" + exact.evidence + "; candidates=" + exact.candidateEids,
                        null, "");
            } else if (exact.found && exact.record != null) {
                CandidateFeatures features = scorePair(post, exact.record, analyzer);
                MatchStatus status = suspectExactMatches.contains(post)
                        ? MatchStatus.EXACT_SUSPECT : MatchStatus.EXACT;
                result = new MatchResult(status, exact.record.getEID(), "", features, 0,
                        "identifiers=" + exact.evidence, exact.record, "");
            } else {
                List<ScoredCandidate> ranked = scoreCandidates(post,
                        retrieveCandidates(post, analyzer, searcher, indexes.recordsByEid), analyzer);
                result = decideTextMatch(post, ranked, thresholds, identifierClaimedEids);
            }
            results.add(result);
        }

        markSharedTextTargets(posts, results);

        Map<String, SciValParser.SciValRecord> focalRecordsByEid = new LinkedHashMap<>();
        for(MatchResult result : results) {
            SciValParser.SciValRecord acceptedRecord = result.acceptedRecord();
            if(acceptedRecord == null) continue;
            String normalizedEid = normalizeEid(acceptedRecord.getEID());
            if(!normalizedEid.isEmpty()) {
                focalRecordsByEid.putIfAbsent(normalizedEid, acceptedRecord);
            }
        }

        Map<String, ReferenceIndicators> referenceIndicatorsByEid = Collections.emptyMap();
        if(!focalRecordsByEid.isEmpty()) {
            SwedishReferenceIndicatorCalculator indicatorCalculator =
                    new SwedishReferenceIndicatorCalculator(
                            sciValRecords, focalRecordsByEid.keySet(), "Umeå University");
            referenceIndicatorsByEid =
                    indicatorCalculator.calculateAll(focalRecordsByEid.values());
        }

        Map<MatchStatus, Integer> counts = new EnumMap<>(MatchStatus.class);
        for (MatchStatus status : MatchStatus.values()) counts.put(status, 0);
        try (PrintWriter output = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(TSV_OUTPUT_FILE), StandardCharsets.UTF_8), true)) {
            output.println(String.join("\t", OUTPUT_HEADERS));
            for (int i = 0; i < posts.size(); i++) {
                MatchResult result = results.get(i);
                counts.put(result.status, counts.get(result.status) + 1);
                saveMatchToFile(posts.get(i), result,
                        referenceIndicatorsFor(result, referenceIndicatorsByEid), output);
            }
        }
        saveMatchesToExcel(
                posts, results, referenceIndicatorsByEid, new File(XLSX_OUTPUT_FILE));

        System.out.println("Match summary:");
        for (MatchStatus status : MatchStatus.values()) {
            System.out.println(status + ": " + counts.get(status));
        }
        reader.close();
        luceneIndex.close();
        analyzer.close();
    }

    public static void validateAfids(List<SciValParser.SciValRecord> records) {
        for (SciValParser.SciValRecord record : records) {
            List<Integer> afids = record.getAfids();
            if (afids != null && new HashSet<>(afids).size() != afids.size()) {
                throw new IllegalStateException(record.getEID()
                        + " has unexpected duplicate AFIDs within record: " + afids);
            }
        }
    }

    private static boolean isIgnored(Post post) {
        return post.getStatusInModel() != null && post.getStatusInModel().isIgnorerad();
    }

    private static CharArraySet createStopWords() {
        return new CharArraySet(Arrays.asList(
                "a", "an", "and", "are", "as", "at", "be", "but", "by", "for",
                "if", "in", "into", "is", "it", "no", "not", "of", "on", "or",
                "such", "that", "the", "their", "then", "there", "these", "they",
                "this", "to", "was", "will", "with"), true);
    }

    private static void buildSciValIndex(List<SciValParser.SciValRecord> records,
                                         StandardAnalyzer analyzer, Directory index)
            throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(index, config);
        for (SciValParser.SciValRecord record : records) {
            String eid = clean(record.getEID());
            if (eid.isEmpty()) continue;
            Document document = new Document();
            document.add(new StoredField("EID", eid));
            document.add(new StoredField("record_type", clean(record.getScivalDocType())));
            document.add(new StringField("year", String.valueOf(validYear(record.getYear())),
                    Field.Store.YES));
            document.add(new TextField("title", clean(record.getTitle()), Field.Store.YES));
            document.add(new TextField("source", clean(record.getSourceTitle()), Field.Store.YES));
            for (String surname : normalizedSciValSurnames(record)) {
                document.add(new StringField("familyNames", surname, Field.Store.NO));
            }
            writer.addDocument(document);
        }
        writer.commit();
        System.out.println("SciVal documents indexed: " + writer.getDocStats().numDocs);
        writer.close();
    }

    private static List<RetrievedCandidate> retrieveCandidates(
            Post post, StandardAnalyzer analyzer, IndexSearcher searcher,
            Map<String, SciValParser.SciValRecord> recordsByEid) throws IOException {
        int year = validYear(post.getYear());
        if (year < 0) return Collections.emptyList();

        Query yearQuery = disjunction("year", Arrays.asList(String.valueOf(year - 1),
                String.valueOf(year), String.valueOf(year + 1)));
        Query titleQuery = disjunction("title", analyze(post.getTitle(), analyzer));
        Query authorQuery = disjunction("familyNames", normalizedDivaSurnames(post));
        Query sourceQuery = disjunction("source", analyze(sourceFor(post), analyzer));

        BooleanQuery.Builder query = new BooleanQuery.Builder();
        query.add(yearQuery, BooleanClause.Occur.MUST);
        if (isWeakTitle(post.getTitle())) {
            if (authorQuery == null || sourceQuery == null) return Collections.emptyList();
            query.add(authorQuery, BooleanClause.Occur.MUST);
            query.add(sourceQuery, BooleanClause.Occur.MUST);
        } else {
            if (titleQuery == null) return Collections.emptyList();
            query.add(titleQuery, BooleanClause.Occur.MUST);
            if (authorQuery != null) query.add(authorQuery, BooleanClause.Occur.MUST);
            if (sourceQuery != null) query.add(sourceQuery, BooleanClause.Occur.SHOULD);
        }

        TopDocs topDocs = searcher.search(query.build(), CANDIDATE_LIMIT);
        Map<String, RetrievedCandidate> unique = new LinkedHashMap<>();
        for (ScoreDoc hit : topDocs.scoreDocs) {
            String eid = searcher.doc(hit.doc).get("EID");
            SciValParser.SciValRecord record = recordsByEid.get(eid);
            if (record != null && !unique.containsKey(eid)) {
                unique.put(eid, new RetrievedCandidate(record, hit.score));
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static Query disjunction(String field, Iterable<String> terms) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        int count = 0;
        for (String term : terms) {
            if (term != null && !term.isEmpty()) {
                builder.add(new TermQuery(new Term(field, term)), BooleanClause.Occur.SHOULD);
                count++;
            }
        }
        return count == 0 ? null : builder.build();
    }

    private static List<ScoredCandidate> scoreCandidates(Post post,
                                                         List<RetrievedCandidate> candidates,
                                                         StandardAnalyzer analyzer)
            throws IOException {
        List<ScoredCandidate> scored = new ArrayList<>(candidates.size());
        for (RetrievedCandidate candidate : candidates) {
            scored.add(new ScoredCandidate(candidate.record,
                    scorePair(post, candidate.record, analyzer), candidate.luceneScore));
        }
        Collections.sort(scored, new Comparator<ScoredCandidate>() {
            @Override
            public int compare(ScoredCandidate left, ScoredCandidate right) {
                int byScore = Double.compare(right.features.total, left.features.total);
                return byScore != 0 ? byScore : Float.compare(right.luceneScore, left.luceneScore);
            }
        });
        return scored;
    }

    private static CandidateFeatures scorePair(Post post, SciValParser.SciValRecord record,
                                               StandardAnalyzer analyzer) throws IOException {
        boolean weakTitle = isWeakTitle(post.getTitle());
        double title = textSimilarity(post.getTitle(), record.getTitle(), analyzer, true);
        double authors = authorSimilarity(normalizedDivaSurnames(post),
                normalizedSciValSurnames(record));
        double source = textSimilarity(sourceFor(post), record.getSourceTitle(), analyzer, false);
        double year = yearSimilarity(post.getYear(), record.getYear());
        DocumentTypeCompatibility typeCompatibility = documentTypeCompatibility(post, record);
        boolean relatedWorkConflict = relatedWorkVariantConflict(post.getTitle(), record.getTitle());

        double sum = 0;
        double weight = 0;
        if (!weakTitle && title >= 0) {
            sum += TITLE_WEIGHT * title;
            weight += TITLE_WEIGHT;
        }
        if (authors >= 0) {
            sum += AUTHORS_WEIGHT * authors;
            weight += AUTHORS_WEIGHT;
        }
        if (source >= 0) {
            sum += SOURCE_WEIGHT * source;
            weight += SOURCE_WEIGHT;
        }
        if (year >= 0) {
            sum += YEAR_WEIGHT * year;
            weight += YEAR_WEIGHT;
        }
        if (typeCompatibility == DocumentTypeCompatibility.COMPATIBLE) {
            sum += DOCUMENT_TYPE_WEIGHT;
            weight += DOCUMENT_TYPE_WEIGHT;
        } else if (typeCompatibility == DocumentTypeCompatibility.CONFLICT) {
            weight += DOCUMENT_TYPE_WEIGHT;
        }
        double total = weight == 0 ? 0 : sum / weight;

        boolean enough;
        boolean reviewable;
        if (weakTitle) {
            // The title carries no identity information. Automatic matching is only
            // possible with very strong author/source agreement and a plausible year;
            // the runner-up margin is checked separately in decideTextMatch().
            enough = authors >= 0.80 && source >= 0.80 && year >= 0.50;
            reviewable = authors >= 0.20 && source >= 0.40;
        } else {
            enough = title >= MINIMUM_AUTOMATIC_TITLE
                    && (authors >= 0.20 || source >= 0.55);
            reviewable = title >= 0.30 && (authors >= 0.10 || source >= 0.40);
            if (authors < 0) {
                enough = title >= 0.85 && source >= 0.65;
                reviewable = title >= 0.65 && source >= 0.45;
            }
            if (source < 0) {
                enough = title >= 0.80 && authors >= 0.25;
                reviewable = title >= 0.60 && authors >= 0.10;
            }
        }
        if (typeCompatibility == DocumentTypeCompatibility.CONFLICT) enough = false;
        if (relatedWorkConflict) enough = false;
        return new CandidateFeatures(title, authors, source, year, typeCompatibility,
                relatedWorkConflict, total, enough, reviewable);
    }

    /**
     * A deliberately partial compatibility check. It only acts when both sides
     * provide unambiguous evidence for the journal-article/conference distinction.
     * Mixed metadata (for example Conference Paper in a Journal source) and all
     * unrecognised types remain UNKNOWN so future values are not rejected.
     */
    private static DocumentTypeCompatibility documentTypeCompatibility(
            Post post, SciValParser.SciValRecord record) {
        String divaType = normalizeTypeLabel(post.getDivaPublicationType());
        boolean divaArticle = "artikel i tidskrift".equals(divaType)
                || "artikel, forskningsöversikt".equals(divaType);
        boolean divaConference = "konferensbidrag".equals(divaType);
        if (!divaArticle && !divaConference) return DocumentTypeCompatibility.UNKNOWN;

        String sciValDocumentType = normalizeTypeLabel(record.getScivalDocType());
        String sciValSourceType = normalizeTypeLabel(record.getSciValSourceType());
        boolean sciValArticle = SCIVAL_ARTICLE_DOCUMENT_TYPES.contains(sciValDocumentType)
                || "journal".equals(sciValSourceType);
        boolean sciValConference = "conference paper".equals(sciValDocumentType)
                || "conference proceeding".equals(sciValSourceType);

        // Conflicting signals inside SciVal are too ambiguous for this partial gate.
        if (sciValArticle == sciValConference) return DocumentTypeCompatibility.UNKNOWN;
        if (divaArticle) {
            return sciValArticle ? DocumentTypeCompatibility.COMPATIBLE
                    : DocumentTypeCompatibility.CONFLICT;
        }
        return sciValConference ? DocumentTypeCompatibility.COMPATIBLE
                : DocumentTypeCompatibility.CONFLICT;
    }

    /**
     * Detects titles that describe a response, correction, or derivative item while
     * the other title describes the underlying publication. These records commonly
     * contain nearly the complete original title and would otherwise receive the
     * subtitle-containment boost even though they are distinct publications.
     */
    private static boolean relatedWorkVariantConflict(String left, String right) {
        String leftKind = relatedWorkKind(left);
        String rightKind = relatedWorkKind(right);
        return !leftKind.equals(rightKind) && (!leftKind.isEmpty() || !rightKind.isEmpty());
    }

    private static String relatedWorkKind(String title) {
        if (!hasText(title)) return "";
        String normalized = title.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");

        if (normalized.contains("visual abstract")) return "VISUAL_ABSTRACT";
        if (normalized.startsWith("correction") || normalized.startsWith("corrigendum")
                || normalized.startsWith("erratum") || normalized.startsWith("addendum")
                || normalized.startsWith("rättelse")) {
            return "CORRECTION";
        }
        if (normalized.startsWith("reply") || normalized.startsWith("response")
                || normalized.startsWith("author response")
                || normalized.startsWith("answer to the letter")
                || normalized.startsWith("comment on")
                || normalized.startsWith("commentary on")
                || normalized.startsWith("letter to")
                || normalized.startsWith("correspondence to")
                || normalized.startsWith("re: ") || normalized.startsWith("re. ")
                || normalized.startsWith("replik") || normalized.startsWith("svar på")
                || normalized.startsWith("kommentar till")
                || normalized.contains("letter to the editor")
                || normalized.contains("letter to editor")
                || normalized.contains("reply by authors")
                || normalized.contains("response to comment")
                || normalized.contains("response letter")
                || normalized.contains(": reply")
                || normalized.contains("[reply]")) {
            return "RESPONSE_OR_COMMENT";
        }
        if (normalized.equals("editorial") || normalized.startsWith("editorial:")
                || normalized.startsWith("editorial ")) {
            return "EDITORIAL";
        }
        return "";
    }

    private static double textSimilarity(String left, String right, StandardAnalyzer analyzer,
                                         boolean subtitleAware) throws IOException {
        if (!hasText(left) || !hasText(right)) return -1;
        double characters = similarityThresholded(simplifyString(left), simplifyString(right), 0.10);
        if (characters < 0) characters = 0;
        Set<String> leftTokens = analyze(left, analyzer);
        Set<String> rightTokens = analyze(right, analyzer);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return characters;

        int intersection = intersectionSize(leftTokens, rightTokens);
        double dice = (2.0 * intersection) / (leftTokens.size() + rightTokens.size());
        double base = 0.6 * dice + 0.4 * characters;
        if (!subtitleAware || Math.min(leftTokens.size(), rightTokens.size()) < 4) return base;
        double containment = (double) intersection / Math.min(leftTokens.size(), rightTokens.size());
        double lengthRatio = (double) Math.min(leftTokens.size(), rightTokens.size())
                / Math.max(leftTokens.size(), rightTokens.size());
        // Containment is intended for a complete shorter title plus a subtitle.
        // High topical overlap with a much longer, different title is not enough.
        if (!relatedWorkVariantConflict(left, right)
                && intersection >= 3
                && containment >= TITLE_CONTAINMENT_THRESHOLD
                && lengthRatio >= TITLE_CONTAINMENT_LENGTH_RATIO) {
            return Math.max(base, 0.95 * containment);
        }
        return base;
    }

    private static double authorSimilarity(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) return -1;
        int intersection = intersectionSize(left, right);
        return (2.0 * intersection) / (left.size() + right.size());
    }

    private static double yearSimilarity(Integer left, Integer right) {
        int leftYear = validYear(left);
        int rightYear = validYear(right);
        if (leftYear < 0 || rightYear < 0) return -1;
        int difference = Math.abs(leftYear - rightYear);
        if (difference == 0) return 1;
        if (difference == 1) return 0.5;
        return 0;
    }

    private static Set<String> analyze(String text, StandardAnalyzer analyzer) throws IOException {
        if (!hasText(text)) return Collections.emptySet();
        Set<String> terms = new LinkedHashSet<>();
        TokenStream stream = analyzer.tokenStream(null, new StringReader(text));
        CharTermAttribute attribute = stream.addAttribute(CharTermAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            if (!attribute.toString().isEmpty()) terms.add(attribute.toString());
        }
        stream.end();
        stream.close();
        return terms;
    }

    private static Set<String> normalizedDivaSurnames(Post post) {
        Set<String> surnames = new LinkedHashSet<>();
        if (post.getAuthorList() == null) return surnames;
        for (Author author : post.getAuthorList()) {
            String surname = simplifyString(getSurname(author.getAuthorName()));
            if (surname != null && surname.length() > 1) surnames.add(surname);
        }
        return surnames;
    }

    private static Set<String> normalizedSciValSurnames(SciValParser.SciValRecord record) {
        Set<String> surnames = new LinkedHashSet<>();
        if (record.getAuthors() == null) return surnames;
        for (String author : record.getAuthors()) {
            String surname = simplifyString(getSurname(author));
            if (surname != null && surname.length() > 1) surnames.add(surname);
        }
        return surnames;
    }

    private static int intersectionSize(Set<String> left, Set<String> right) {
        Set<String> smaller = left.size() <= right.size() ? left : right;
        Set<String> larger = left.size() <= right.size() ? right : left;
        int result = 0;
        for (String value : smaller) if (larger.contains(value)) result++;
        return result;
    }

    private static CalibrationThresholds calibrate(List<CalibrationExample> examples) {
        if (examples.isEmpty()) {
            double disabledThreshold = 1.01;
            return new CalibrationThresholds(disabledThreshold, disabledThreshold,
                    evaluate(examples, disabledThreshold, disabledThreshold), false);
        }
        CalibrationThresholds best = null;
        int firstScoreStep = (int) Math.ceil(SAFETY_MINIMUM_SCORE * 100);
        int firstMarginStep = (int) Math.ceil(SAFETY_MINIMUM_MARGIN * 100);
        for (int scoreStep = firstScoreStep; scoreStep <= 98; scoreStep++) {
            double minimumScore = scoreStep / 100.0;
            for (int marginStep = firstMarginStep; marginStep <= 30; marginStep++) {
                double minimumMargin = marginStep / 100.0;
                Evaluation evaluation = evaluate(examples, minimumScore, minimumMargin);
                if (evaluation.accepted == 0
                        || evaluation.precision + 1.0e-12
                        < TARGET_CALIBRATION_PRECISION + CALIBRATION_PRECISION_BUFFER) continue;
                if (best == null
                        || evaluation.correct > best.training.correct
                        || (evaluation.correct == best.training.correct
                        && evaluation.incorrect < best.training.incorrect)
                        || (evaluation.correct == best.training.correct
                        && evaluation.incorrect == best.training.incorrect
                        && minimumScore + minimumMargin
                        < best.minimumScore + best.minimumMargin)) {
                    best = new CalibrationThresholds(minimumScore, minimumMargin, evaluation, true);
                }
            }
        }
        if (best != null) return best;
        // Fail closed: if no tested pair reaches the precision target, no text
        // candidate is automatically accepted. Review and no-match decisions remain.
        double disabledThreshold = 1.01;
        return new CalibrationThresholds(disabledThreshold, disabledThreshold,
                evaluate(examples, disabledThreshold, disabledThreshold), false);
    }

    private static Evaluation evaluate(List<CalibrationExample> examples,
                                       double minimumScore, double minimumMargin) {
        int positiveExamples = 0;
        int hardNegativeExamples = 0;
        int accepted = 0;
        int correct = 0;
        int incorrect = 0;
        int acceptedHardNegatives = 0;
        for (CalibrationExample example : examples) {
            if (example.positive) positiveExamples++;
            else hardNegativeExamples++;
            if (example.rankedCandidates.isEmpty()) continue;
            ScoredCandidate best = example.rankedCandidates.get(0);
            double margin = candidateMargin(example.rankedCandidates);
            if (!best.features.sufficientEvidence || best.features.total < minimumScore
                    || margin < minimumMargin) continue;
            accepted++;
            if (example.positive && sameEid(best.record.getEID(), example.knownEid)) {
                correct++;
            } else {
                incorrect++;
                if (!example.positive) acceptedHardNegatives++;
            }
        }
        return new Evaluation(examples.size(), positiveExamples, hardNegativeExamples,
                accepted, correct, incorrect, acceptedHardNegatives);
    }

    private static List<ScoredCandidate> withoutEid(List<ScoredCandidate> ranked, String eid) {
        List<ScoredCandidate> result = new ArrayList<>(ranked.size());
        for (ScoredCandidate candidate : ranked) {
            if (!sameEid(candidate.record.getEID(), eid)) result.add(candidate);
        }
        return result;
    }

    private static MatchResult decideTextMatch(Post post, List<ScoredCandidate> ranked,
                                               CalibrationThresholds thresholds,
                                               Set<String> identifierClaimedEids) {
        if (ranked.isEmpty()) {
            return new MatchResult(MatchStatus.NO_MATCH, "", "", null, 0,
                    "no retrieval candidates", null, "");
        }
        ScoredCandidate best = ranked.get(0);
        String runnerUp = ranked.size() > 1 ? ranked.get(1).record.getEID() : "";
        double margin = candidateMargin(ranked);
        boolean weakTitle = isWeakTitle(post.getTitle());
        boolean identifierCollision = identifierClaimedEids.contains(
                normalizeEid(best.record.getEID()));
        boolean collisionAllowsAutomaticMatch = !identifierCollision
                || (!weakTitle && best.features.title >= DUPLICATE_TARGET_MINIMUM_TITLE);
        double minimumScore = Math.max(SAFETY_MINIMUM_SCORE, thresholds.minimumScore);
        double minimumMargin = Math.max(SAFETY_MINIMUM_MARGIN, thresholds.minimumMargin);
        if (weakTitle) minimumMargin = Math.max(minimumMargin, WEAK_TITLE_MINIMUM_MARGIN);
        String collision = identifierCollision ? "IDENTIFIER_LINK" : "";

        if (best.features.sufficientEvidence
                && best.features.total >= minimumScore
                && margin >= minimumMargin
                && collisionAllowsAutomaticMatch) {
            return new MatchResult(MatchStatus.TEXT_AUTO_MATCH, best.record.getEID(), runnerUp,
                    best.features, margin, "candidates=" + ranked.size(), best.record, collision);
        }
        if (best.features.reviewableEvidence && best.features.total >= MINIMUM_REVIEW_SCORE) {
            String reason = "candidates=" + ranked.size();
            if (identifierCollision && !collisionAllowsAutomaticMatch) {
                reason += "; identifier-linked target requires near-identical title";
            } else if (best.features.relatedWorkConflict) {
                reason += "; response/correction/derivative title differs from candidate kind";
            } else if (margin < minimumMargin) {
                reason += "; margin below automatic threshold";
            } else if (weakTitle && !best.features.sufficientEvidence) {
                reason += "; generic title lacks strong non-title evidence";
            }
            return new MatchResult(MatchStatus.AMBIGUOUS, best.record.getEID(), runnerUp,
                    best.features, margin, reason, best.record, collision);
        }
        if (!best.features.reviewableEvidence) {
            return new MatchResult(MatchStatus.NO_MATCH, "", runnerUp, best.features, margin,
                    "insufficient field evidence for review; bestEid=" + best.record.getEID(),
                    best.record, collision);
        }
        return new MatchResult(MatchStatus.NO_MATCH, "", runnerUp, best.features, margin,
                "best candidate below thresholds; bestEid=" + best.record.getEID(),
                best.record, collision);
    }

    private static void markSharedTextTargets(List<Post> posts, List<MatchResult> results) {
        Map<String, List<Integer>> proposalsByEid = new HashMap<>();
        for (int i = 0; i < results.size(); i++) {
            MatchResult result = results.get(i);
            if ((result.status == MatchStatus.TEXT_AUTO_MATCH
                    || result.status == MatchStatus.AMBIGUOUS) && hasText(result.eid)) {
                proposalsByEid.computeIfAbsent(normalizeEid(result.eid), ignored -> new ArrayList<>())
                        .add(i);
            }
        }

        for (List<Integer> proposals : proposalsByEid.values()) {
            if (proposals.size() < 2) continue;
            for (Integer index : proposals) {
                MatchResult result = results.get(index);
                String collision = addCollision(result.targetCollision, "TEXT_TARGET_SHARED");
                if (result.status == MatchStatus.TEXT_AUTO_MATCH
                        && (isWeakTitle(posts.get(index).getTitle())
                        || result.features.title < DUPLICATE_TARGET_MINIMUM_TITLE)) {
                    results.set(index, result.withStatus(MatchStatus.AMBIGUOUS,
                            "target proposed for " + proposals.size()
                                    + " DiVA records; near-identical title required",
                            collision));
                } else {
                    results.set(index, result.withCollision(collision));
                }
            }
        }
    }

    private static String addCollision(String existing, String addition) {
        if (!hasText(existing)) return addition;
        if (existing.contains(addition)) return existing;
        return existing + "+" + addition;
    }

    private static double candidateMargin(List<ScoredCandidate> ranked) {
        if (ranked.isEmpty()) return 0;
        if (ranked.size() == 1) return ranked.get(0).features.total;
        return ranked.get(0).features.total - ranked.get(1).features.total;
    }

    private static boolean hasBibliographicContradiction(Post post,
                                                         SciValParser.SciValRecord record,
                                                         StandardAnalyzer analyzer)
            throws IOException {
        int divaYear = validYear(post.getYear());
        int sciValYear = validYear(record.getYear());
        if (divaYear >= 0 && sciValYear >= 0 && Math.abs(divaYear - sciValYear) > 2) return true;
        return !isWeakTitle(post.getTitle()) && hasText(post.getTitle()) && hasText(record.getTitle())
                && textSimilarity(post.getTitle(), record.getTitle(), analyzer, true) < 0.15;
    }

    private static boolean containsEid(List<ScoredCandidate> candidates, String eid) {
        for (ScoredCandidate candidate : candidates) {
            if (sameEid(candidate.record.getEID(), eid)) return true;
        }
        return false;
    }

    private static void printCalibration(CalibrationThresholds thresholds, Evaluation validation) {
        Evaluation training = thresholds.training;
        System.out.printf(Locale.ROOT,
                "Calibrated thresholds: minimumScore=%.2f, minimumMargin=%.2f, "
                        + "safetyFloors=%.2f/%.2f, targetPrecision=%.2f, "
                        + "trainingSelectionPrecision=%.3f%n",
                thresholds.minimumScore, thresholds.minimumMargin, SAFETY_MINIMUM_SCORE,
                SAFETY_MINIMUM_MARGIN, TARGET_CALIBRATION_PRECISION,
                TARGET_CALIBRATION_PRECISION + CALIBRATION_PRECISION_BUFFER);
        System.out.printf(Locale.ROOT,
                "Training: positives=%d, hardNegatives=%d, accepted=%d, correct=%d, "
                        + "incorrect=%d, precision=%.4f, positiveCoverage=%.4f, "
                        + "hardNegativeAcceptance=%.4f%n",
                training.positiveExamples, training.hardNegativeExamples, training.accepted,
                training.correct, training.incorrect, training.precision, training.coverage,
                training.hardNegativeAcceptanceRate);
        System.out.printf(Locale.ROOT,
                "Held-out: positives=%d, hardNegatives=%d, accepted=%d, correct=%d, "
                        + "incorrect=%d, precision=%.4f, positiveCoverage=%.4f, "
                        + "hardNegativeAcceptance=%.4f%n",
                validation.positiveExamples, validation.hardNegativeExamples,
                validation.accepted, validation.correct, validation.incorrect,
                validation.precision, validation.coverage,
                validation.hardNegativeAcceptanceRate);
        if (!thresholds.targetMet) {
            System.out.println("WARNING: calibration did not reach target precision; "
                    + "automatic text matching is disabled for this run.");
        } else if (validation.accepted == 0
                || validation.precision + 1.0e-12 < TARGET_CALIBRATION_PRECISION) {
            System.out.println("WARNING: held-out precision did not reach the target; "
                    + "automatic text matching is disabled for this run.");
        }
    }

    private static void printMatch(Post post, MatchResult result) {
        CandidateFeatures f = result.features;
        System.out.printf(Locale.ROOT,
                "%d\t%s\t%s\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%s\t%s\t%s\t%s\t%s%n",
                post.getPID(), result.status, cleanForOutput(result.eid), f == null ? 0 : f.total,
                result.margin, f == null ? -1 : f.title, f == null ? -1 : f.authors,
                f == null ? -1 : f.source, f == null ? -1 : f.year,
                f == null ? DocumentTypeCompatibility.UNKNOWN : f.documentTypeCompatibility,
                f != null && f.relatedWorkConflict, cleanForOutput(result.runnerUpEid),
                cleanForOutput(result.targetCollision),
                cleanForOutput(result.note));
    }

    private static ReferenceIndicators referenceIndicatorsFor(
            MatchResult result, Map<String, ReferenceIndicators> indicatorsByEid) {
        SciValParser.SciValRecord acceptedRecord = result.acceptedRecord();
        if(acceptedRecord == null) return null;
        return indicatorsByEid.get(normalizeEid(acceptedRecord.getEID()));
    }

    private static void saveMatchToFile(Post post, MatchResult result,
                                        ReferenceIndicators indicators, PrintWriter writer) {
        CandidateFeatures f = result.features;
        SciValParser.SciValRecord record = result.diagnosticRecord;
        writer.printf(Locale.ROOT,
                "%d\t%s\t%s\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%s\t%s\t%s\t%s"
                        + "\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                post.getPID(), result.status, cleanForOutput(result.eid), f == null ? 0 : f.total,
                result.margin, f == null ? -1 : f.title, f == null ? -1 : f.authors,
                f == null ? -1 : f.source, f == null ? -1 : f.year,
                f == null ? DocumentTypeCompatibility.UNKNOWN : f.documentTypeCompatibility,
                f != null && f.relatedWorkConflict, cleanForOutput(result.runnerUpEid),
                cleanForOutput(result.targetCollision),
                cleanForOutput(post.getTitle()),
                cleanForOutput(record == null ? "" : record.getTitle()),
                cleanForOutput(sourceFor(post)),
                cleanForOutput(record == null ? "" : record.getSourceTitle()),
                cleanForOutput(post.getDivaPublicationType()),
                cleanForOutput(record == null ? "" : record.getScivalDocType()),
                cleanForOutput(record == null ? "" : record.getSciValSourceType()),
                cleanForOutput(result.note));
        writeReferenceIndicators(writer, indicators);
    }

    private static void writeReferenceIndicators(
            PrintWriter writer, ReferenceIndicators indicators) {
        if(indicators == null) {
            for(int column = 0; column < 15; column++) writer.print('\t');
            writer.println();
            return;
        }
        writer.printf(Locale.ROOT,
                "\t%d\t%d\t%d\t%.6f\t%.6f\t%.6f\t%d\t%d\t%b\t%b\t%s\t%s\t%s\t%d\t%d%n",
                indicators.observedTop10(),
                indicators.observedTop50(),
                indicators.observedInternational(),
                indicators.expectedTop10(),
                indicators.expectedTop50(),
                indicators.expectedInternational(),
                indicators.referenceSetSize(),
                indicators.internationalReferenceSetSize(),
                indicators.usedCitationFallback(),
                indicators.usedInternationalFallback(),
                indicators.normalizationArtifactGroup(),
                indicators.citationReferenceScope(),
                indicators.internationalReferenceScope(),
                indicators.citationReferencePopulationSize(),
                indicators.internationalReferencePopulationSize());
    }

    private static void saveMatchesToExcel(List<Post> posts, List<MatchResult> results,
                                            Map<String, ReferenceIndicators> indicatorsByEid,
                                            File outputFile) throws IOException {
        if (posts.size() != results.size()) {
            throw new IllegalArgumentException("Every DiVA post must have exactly one match result");
        }

        XSSFWorkbook templateWorkbook = new XSSFWorkbook();
        SXSSFWorkbook workbook = new SXSSFWorkbook(templateWorkbook, EXCEL_ROW_WINDOW,
                true, false);
        try {
            Sheet sheet = workbook.createSheet("Matches");
            sheet.createFreezePane(0, 1);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            CellStyle scoreStyle = workbook.createCellStyle();
            scoreStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0000"));

            Row header = sheet.createRow(0);
            for (int column = 0; column < OUTPUT_HEADERS.length; column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(OUTPUT_HEADERS[column]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < posts.size(); i++) {
                MatchResult result = results.get(i);
                writeExcelMatchRow(posts.get(i), result,
                        referenceIndicatorsFor(result, indicatorsByEid),
                        sheet.createRow(i + 1), scoreStyle);
            }

            sheet.setAutoFilter(new CellRangeAddress(0, posts.size(), 0,
                    OUTPUT_HEADERS.length - 1));
            try (FileOutputStream output = new FileOutputStream(outputFile)) {
                workbook.write(output);
            }
        } finally {
            workbook.dispose();
            workbook.close();
        }
    }

    private static void writeExcelMatchRow(Post post, MatchResult result,
                                           ReferenceIndicators indicators, Row row,
                                           CellStyle scoreStyle) {
        CandidateFeatures features = result.features;
        SciValParser.SciValRecord record = result.diagnosticRecord;
        int column = 0;

        row.createCell(column++).setCellValue(post.getPID());
        row.createCell(column++).setCellValue(result.status.name());
        row.createCell(column++).setCellValue(cleanForExcel(result.eid));
        setNumericCell(row, column++, features == null ? 0 : features.total, scoreStyle);
        setNumericCell(row, column++, result.margin, scoreStyle);
        setNumericCell(row, column++, features == null ? -1 : features.title, scoreStyle);
        setNumericCell(row, column++, features == null ? -1 : features.authors, scoreStyle);
        setNumericCell(row, column++, features == null ? -1 : features.source, scoreStyle);
        setNumericCell(row, column++, features == null ? -1 : features.year, scoreStyle);
        row.createCell(column++).setCellValue(features == null
                ? DocumentTypeCompatibility.UNKNOWN.name()
                : features.documentTypeCompatibility.name());
        row.createCell(column++).setCellValue(features != null && features.relatedWorkConflict);
        row.createCell(column++).setCellValue(cleanForExcel(result.runnerUpEid));
        row.createCell(column++).setCellValue(cleanForExcel(result.targetCollision));
        row.createCell(column++).setCellValue(cleanForExcel(post.getTitle()));
        row.createCell(column++).setCellValue(cleanForExcel(record == null ? "" : record.getTitle()));
        row.createCell(column++).setCellValue(cleanForExcel(sourceFor(post)));
        row.createCell(column++).setCellValue(cleanForExcel(
                record == null ? "" : record.getSourceTitle()));
        row.createCell(column++).setCellValue(cleanForExcel(post.getDivaPublicationType()));
        row.createCell(column++).setCellValue(cleanForExcel(
                record == null ? "" : record.getScivalDocType()));
        row.createCell(column++).setCellValue(cleanForExcel(
                record == null ? "" : record.getSciValSourceType()));
        row.createCell(column++).setCellValue(cleanForExcel(result.note));
        writeReferenceIndicatorCells(row, column, indicators, scoreStyle);
    }

    private static void writeReferenceIndicatorCells(
            Row row, int column, ReferenceIndicators indicators, CellStyle scoreStyle) {
        if(indicators == null) {
            for(int offset = 0; offset < 15; offset++) row.createCell(column + offset);
            return;
        }

        row.createCell(column++).setCellValue(indicators.observedTop10());
        row.createCell(column++).setCellValue(indicators.observedTop50());
        row.createCell(column++).setCellValue(indicators.observedInternational());
        setNumericCell(row, column++, indicators.expectedTop10(), scoreStyle);
        setNumericCell(row, column++, indicators.expectedTop50(), scoreStyle);
        setNumericCell(row, column++, indicators.expectedInternational(), scoreStyle);
        row.createCell(column++).setCellValue(indicators.referenceSetSize());
        row.createCell(column++).setCellValue(indicators.internationalReferenceSetSize());
        row.createCell(column++).setCellValue(indicators.usedCitationFallback());
        row.createCell(column++).setCellValue(indicators.usedInternationalFallback());
        row.createCell(column++).setCellValue(indicators.normalizationArtifactGroup());
        row.createCell(column++).setCellValue(indicators.citationReferenceScope().name());
        row.createCell(column++).setCellValue(indicators.internationalReferenceScope().name());
        row.createCell(column++).setCellValue(indicators.citationReferencePopulationSize());
        row.createCell(column).setCellValue(indicators.internationalReferencePopulationSize());
    }

    private static void setNumericCell(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static String sourceFor(Post post) {
        if (hasText(post.getJournal()) && post.getJournal().trim().length() > 3) {
            return post.getJournal();
        }
        if (hasText(post.getHost()) && post.getHost().trim().length() > 3) return post.getHost();
        if (hasText(post.getSeriesName()) && post.getSeriesName().trim().length() > 3) {
            return post.getSeriesName();
        }
        return "";
    }

    private static int validYear(Integer year) {
        return year != null && year > 0 ? year : -1;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeTypeLabel(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    private static String cleanForOutput(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private static String cleanForExcel(String value) {
        String cleaned = cleanForOutput(value);
        if (cleaned.length() <= EXCEL_MAX_CELL_TEXT_LENGTH) return cleaned;
        String marker = " [TRUNCATED]";
        return cleaned.substring(0, EXCEL_MAX_CELL_TEXT_LENGTH - marker.length()) + marker;
    }

    private static String normalizeDoi(String value) {
        String normalized = clean(value).toLowerCase(Locale.ROOT);
        normalized = normalized.replaceFirst("^https?://(dx\\.)?doi\\.org/", "");
        normalized = normalized.replaceFirst("^doi\\s*:\\s*", "");
        normalized = normalized.replaceAll("\\s+", "");
        normalized = normalized.replaceAll("[.,;]+$", "");
        return normalized.length() > 5 ? normalized : "";
    }

    private static String normalizePmid(String value) {
        String normalized = clean(value).replaceAll("\\D", "");
        return normalized.length() > 3 ? normalized : "";
    }

    private static String normalizeEid(String value) {
        String normalized = clean(value).toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        return normalized.length() > 5 ? normalized : "";
    }

    private static boolean sameEid(String left, String right) {
        String normalizedLeft = normalizeEid(left);
        return !normalizedLeft.isEmpty() && normalizedLeft.equals(normalizeEid(right));
    }

}
