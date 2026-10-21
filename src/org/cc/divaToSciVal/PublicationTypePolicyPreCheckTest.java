package org.cc.divaToSciVal;

import cc.analysis.scival.SciValParser.SciValRecord;
import org.cc.divaToSciVal.SciValDocumentTypePolicy.Group;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Standalone regression checks; run main without a test-framework dependency. */
public final class PublicationTypePolicyPreCheckTest {
    public static void main(String[] args) throws Exception {
        if (args.length >= 1) {
            validateReports(Path.of(args[0]));
            if (args.length == 2) compareReports(Path.of(args[0]), Path.of(args[1]));
            return;
        }
        policyClassification();
        subjectUnionAndExclusions();
        sameGroupFallbacks();
        referenceSizeBoundaries();
        unavailableAndMissingRecords();
        matchInputDeduplication();
        unrestrictedFallbacks();
        missingTypeImport();
        outputColumns();
        check(PublicationTypePolicyPreCheck.quantile(List.of(0.0, 10.0), .5) == 5.0,
                "Quantiles must interpolate");
        System.out.println("All publication-type policy pre-check regression checks passed.");
    }

    private static void validateReports(Path directory) throws Exception {
        List<Map<String, String>> details = readReport(directory.resolve("focal-publications.tsv"));
        Set<String> eids = new HashSet<>();
        for (Map<String, String> row : details) {
            check(eids.add(row.get("EID")), "Duplicate focal EID");
            if (("INCLUDED".equals(row.get("PROPOSED_DISPOSITION"))
                    || "UNKNOWN_DOCUMENT_TYPE".equals(row.get("PROPOSED_DISPOSITION")))) {
                check(Integer.parseInt(row.get("GROUPED_REFERENCE_SET_SIZE"))
                        <= Integer.parseInt(row.get("EXCLUSIONS_ONLY_REFERENCE_SET_SIZE")), "Grouped reference containment");
                check(Integer.parseInt(row.get("EXCLUSIONS_ONLY_REFERENCE_SET_SIZE"))
                        <= Integer.parseInt(row.get("CURRENT_REFERENCE_SET_SIZE")), "Exclusions-only reference containment");
                for (String indicator : List.of("TOP10", "TOP50", "INTERNATIONAL")) {
                    String value = row.get("GROUPED_EXPECTED_" + indicator);
                    check(!value.isEmpty(), "Eligible focal must have every expectation");
                    if (!value.isEmpty()) {
                        double expected = Double.parseDouble(value);
                        check(Double.isFinite(expected) && expected >= 0 && expected <= 1, "Expectation range");
                        double delta = 100 * (expected - Double.parseDouble(row.get("CURRENT_EXPECTED_" + indicator)));
                        check(Math.abs(delta - Double.parseDouble(row.get("DELTA_" + indicator + "_PP"))) <= .00011,
                                "Rounded detail delta");
                    }
                }
                if (!row.get("GROUPED_EXPECTED_TOP10").isEmpty()) {
                    check(Double.parseDouble(row.get("GROUPED_EXPECTED_TOP10"))
                            <= Double.parseDouble(row.get("GROUPED_EXPECTED_TOP50")), "Nested citation expectations");
                }
            } else {
                check(row.get("GROUPED_EXPECTED_TOP10").isEmpty()
                        && row.get("GROUPED_EXPECTED_INTERNATIONAL").isEmpty(), "Excluded/missing grouped expectation");
            }
        }
        for (Map<String, String> row : readReport(directory.resolve("summary.tsv"))) {
            check(Integer.parseInt(row.get("AVAILABLE_EXPECTATIONS")) + Integer.parseInt(row.get("UNAVAILABLE_EXPECTATIONS"))
                    == Integer.parseInt(row.get("FOCAL_EIDS")), "Summary availability totals");
            if (!row.get("MEAN_DELTA_PP").isEmpty()) {
                double delta = 100 * (Double.parseDouble(row.get("PAIRED_SCENARIO_MEAN"))
                        - Double.parseDouble(row.get("PAIRED_CURRENT_MEAN")));
                check(Math.abs(delta - Double.parseDouble(row.get("MEAN_DELTA_PP"))) <= .00011, "Paired summary delta");
            }
        }
        readReport(directory.resolve("document-types.tsv"));
        System.out.println("Report validation passed for " + details.size()
                + " focal EIDs: population containment, expectations, deltas, summaries and TSV columns.");
    }

    private static List<Map<String, String>> readReport(Path file) throws Exception {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        check(!lines.isEmpty(), "Missing report header: " + file);
        String[] header = lines.get(0).split("\t", -1);
        check(new HashSet<>(Arrays.asList(header)).size() == header.length, "Duplicate report headers");
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] values = line.split("\t", -1);
            check(values.length == header.length, "Inconsistent TSV columns: " + file);
            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < header.length; i++) row.put(header[i], values[i]);
            rows.add(row);
        }
        return rows;
    }

    private static void policyClassification() {
        for (String type : List.of("Retracted", "Abstract Report", "Erratum", "Editorial", "Conference Review")) {
            check(SciValDocumentTypePolicy.excluded(type), type + " must be excluded");
            check(SciValDocumentTypePolicy.group(type) == null, type + " must have no group");
        }
        for (String type : List.of("Article in Press", "Letter", "Data Paper", "Short Survey", "Note", "Article", "Review")) {
            check(SciValDocumentTypePolicy.group(type) == Group.JOURNAL_ARTIFACT_TYPE, type);
        }
        check(SciValDocumentTypePolicy.group("Conference Paper") == Group.CONFERENCE_ARTIFACT_TYPE, "Conference group");
        for (String type : List.of("Chapter", "Book Chapter", "Book")) {
            check(SciValDocumentTypePolicy.group(type) == Group.BOOK_AND_CHAPTER_ARTIFACT_TYPE, type);
        }
        check(SciValDocumentTypePolicy.group("  ARTICLE   IN PRESS ") == Group.JOURNAL_ARTIFACT_TYPE, "Label normalization");
        check("UNKNOWN_DOCUMENT_TYPE".equals(SciValDocumentTypePolicy.disposition("Future Type")), "Unknown type");
        check(SciValDocumentTypePolicy.group(null) == null, "Missing type");
    }

    private static void subjectUnionAndExclusions() {
        List<SciValRecord> records = new ArrayList<>();
        records.add(record("focal", "Article", 2024, 1, Set.of(100, 200), 10, true, "Umeå University"));
        records.add(record("other-focal", "Article", 2024, 1, Set.of(100, 200), 10, true, "External"));
        for (int i = 0; i < 25; i++) {
            records.add(record("asjc-" + i, "Article", i < 20 ? 2024 : 2020, 99, Set.of(200, 100), 60, false, "External"));
        }
        for (int i = 0; i < 5; i++) {
            records.add(record("cluster-" + i, "Review", 2024, 1, Set.of(300), 10, true, "External"));
        }
        // Satisfies both subject branches: counted once, even with a duplicate export row.
        records.add(record("both", "Letter", 2024, 1, Set.of(100, 200), 10, true, "External"));
        records.add(record(" BOTH ", "Letter", 2024, 1, Set.of(100, 200), 10, true, "External"));
        for (int i = 0; i < 40; i++) records.add(record("conf-" + i, "Conference Paper", 2024, 1, Set.of(100, 200), 10, true, "External"));
        for (int i = 0; i < 10; i++) records.add(record("ed-" + i, "Editorial", 2024, 1, Set.of(100, 200), 10, true, "External"));
        for (int i = 0; i < 10; i++) records.add(record("umu-" + i, "Article", 2024, 1, Set.of(100, 200), 10, true, "Umeå University"));
        // Partial ASJC overlap is not an exact partition match.
        records.add(record("partial", "Article", 2024, 99, Set.of(100), 10, true, "External"));
        var c = find(PublicationTypePolicyPreCheck.evaluate(records, Set.of("focal", "other-focal")), "focal");
        check(c.current().referenceSetSize() == 81, "Current union size");
        check(c.exclusionsOnly().referenceSetSize() == 71, "Expanded exclusions effect");
        check(c.proposed().indicators().referenceSetSize() == 31, "Group restriction on both subject branches");
        check(c.proposed().indicators().internationalReferenceSetSize() == 26, "International year window");
        near(c.proposed().indicators().expectedTop10(), 6.0 / 31, "Citation group expectation");
        near(c.proposed().indicators().expectedInternational(), 6.0 / 26, "International group expectation");
        check("SUBJECT_REFERENCE".equals(c.proposed().citationScope()), "Subject scope");
        check(c.current().equals(new SwedishReferenceIndicatorCalculator(records, Set.of("focal", "other-focal"),
                "Umeå University", SwedishReferenceIndicatorCalculator.ReferencePolicy.LEGACY_MIXED)
                .calculate(records.get(0))), "CURRENT must preserve historical methodology");
        check(SciValDocumentTypePolicy.group(records.stream().filter(r -> r.getEID().equals("conf-0")).findFirst()
                .orElseThrow().getScivalDocType()) == Group.CONFERENCE_ARTIFACT_TYPE, "Journal source must not override document group");
    }

    private static void sameGroupFallbacks() {
        List<SciValRecord> records = new ArrayList<>();
        records.add(record("focal", "Article", 2024, 1, Set.of(100), 50, false, "Umeå University"));
        for (int i = 0; i < 30; i++) records.add(record("journal-" + i, "Article", i < 20 ? 2024 : 2020,
                99, Set.of(900), 10, i < 10, "External"));
        for (int i = 0; i < 100; i++) records.add(record("book-" + i, "Book", 2024, 99, Set.of(900), 90, false, "External"));
        var trial = find(PublicationTypePolicyPreCheck.evaluate(records, Set.of("focal")), "focal").proposed();
        check(trial.indicators().referenceSetSize() == 0, "Empty subject set");
        check("GROUP_FALLBACK".equals(trial.citationScope()), "Citation fallback group");
        check("GROUP_YEAR_FALLBACK".equals(trial.internationalScope()), "International fallback group/year");
        check(trial.citationFallbackPoolSize() == 30 && trial.internationalFallbackPoolSize() == 20, "Group pool sizes");
        near(trial.indicators().expectedTop10(), 1, "No cross-group citation fallback");
        near(trial.indicators().expectedInternational(), .5, "No cross-group international fallback");
    }

    private static void referenceSizeBoundaries() {
        List<SciValRecord> records = new ArrayList<>();
        records.add(record("focal", "Article", 2024, 1, Set.of(100), 50, false, "Umeå University"));
        for (int i = 0; i < 25; i++) records.add(record("r-" + i, "Article", i < 20 ? 2024 : 2020,
                1, Set.of(100), 50, false, "External"));
        var at = find(PublicationTypePolicyPreCheck.evaluate(records, Set.of("focal")), "focal").proposed();
        check(!at.indicators().usedCitationFallback() && !at.indicators().usedInternationalFallback(), "25/20 boundaries inclusive");
        records.remove(records.size() - 1);
        var below = find(PublicationTypePolicyPreCheck.evaluate(records, Set.of("focal")), "focal").proposed();
        check("SWEDISH_FALLBACK".equals(below.citationScope()), "Pool of 24 uses final generic citation fallback");
        check(below.indicators().citationReferencePopulationSize() == 24, "Final citation population size");
        near(below.indicators().expectedTop10(), 0, "Measured zero citation expectation");
        check("SUBJECT_REFERENCE".equals(below.internationalScope()), "Families evaluated independently");
        records.remove(1);
        var underWindow = find(PublicationTypePolicyPreCheck.evaluate(records, Set.of("focal")), "focal").proposed();
        check("SWEDISH_ALL_YEARS_FALLBACK".equals(underWindow.internationalScope()), "Window of 19 broadens to all years");
        check(underWindow.indicators().internationalReferencePopulationSize() == 23, "Final international population size");
    }

    private static void unavailableAndMissingRecords() {
        List<SciValRecord> records = new ArrayList<>();
        records.add(record("focal", "Book", 2024, -99, Set.of(-99), 10, false, "Umeå University"));
        records.add(record("unknown", "Future Type", 2024, 1, Set.of(100), 10, false, "Umeå University"));
        records.add(record("excluded", "Editorial", 2024, 1, Set.of(100), 10, false, "Umeå University"));
        records.add(record("abstract", "Abstract Report", 2024, 1, Set.of(100), 10, false, "Umeå University"));
        for (int i = 0; i < 25; i++) records.add(record("book-" + i, "Book", 2020, 1, Set.of(100), 10, false, "External"));
        var result = PublicationTypePolicyPreCheck.evaluate(records, Set.of("focal", "unknown", "excluded", "abstract", "missing"));
        var trial = find(result, "focal").proposed();
        check("GROUP_FALLBACK".equals(trial.citationScope()), "Missing subject metadata uses group fallback");
        check("SWEDISH_ALL_YEARS_FALLBACK".equals(trial.internationalScope()), "Empty year window uses all years");
        near(trial.indicators().expectedInternational(), 0, "All-years international expectation");
        check(find(result, "unknown").proposed() != null, "Unknown focal receives generic expectations");
        check(find(result, "excluded").current() != null && find(result, "excluded").exclusionsOnly() == null,
                "Excluded focal retained for current comparison only");
        check(find(result, "abstract").current() == null && find(result, "abstract").proposed() == null, "Abstract Report excluded in both policies");
        check("UNKNOWN_DOCUMENT_TYPE".equals(find(result, "unknown").disposition()), "Unknown focal explicitly flagged");
        check("MISSING_SCIVAL_RECORD".equals(find(result, "missing").disposition()), "Missing focal explicitly flagged");
        boolean failed = false;
        try { PublicationTypePolicyPreCheck.evaluate(List.of(records.get(0)), Set.of("focal")); }
        catch (IllegalArgumentException expected) { failed = true; }
        check(failed, "An entirely empty eligible external benchmark is a run-level error");
    }

    private static void matchInputDeduplication() throws Exception {
        Path file = Files.createTempFile("publication-type-precheck-test-", ".tsv");
        try {
            Files.writeString(file, "\uFEFFPID\tSTATUS\tEID\n1\tEXACT\t EID-A \n2\tEXACT_SUSPECT\teid-a\n"
                    + "2\tTEXT_AUTO_MATCH\teid-a\n3\tAMBIGUOUS\teid-b\n4\tIGNORED\teid-c\n5\tNO_MATCH\t\n",
                    StandardCharsets.UTF_8);
            Map<String, Set<String>> links = PublicationTypePolicyPreCheck.readAcceptedMatches(file);
            check(links.equals(Map.of("eid-a", Set.of("1", "2"))), "Only accepted EIDs; deduplicate PID links");
        } finally {
            Files.deleteIfExists(file);
        }
    }


    private static void unrestrictedFallbacks() {
        List<SciValRecord> records = new ArrayList<>();
        SciValRecord book = record("book", "Book", 2024, 1, Set.of(100), 10, true, "Umeå University");
        records.add(book);
        for (int i = 0; i < 3; i++) records.add(record("b-" + i, "Chapter", 2024, 1, Set.of(100), 90, false, "External"));
        for (int i = 0; i < 30; i++) records.add(record("j-" + i, "Article", 2024, 1, Set.of(100), 10, true, "External"));
        records.add(record("excluded", " Editorial ", 2024, 1, Set.of(100), 10, true, "External"));
        records.add(record("umu", "Article", 2024, 1, Set.of(100), 10, true, "Umeå University"));
        records.add(record("accepted", "Article", 2024, 1, Set.of(100), 10, true, "External"));
        var calculator = new SwedishReferenceIndicatorCalculator(records, Set.of("book", " ACCEPTED "), "Umeå University");
        ReferenceIndicators result = calculator.calculate(book);
        check(result.referenceSetSize() == 3, "Sparse group subject size retained");
        check(result.citationReferenceScope() == ReferenceScope.SWEDISH_FALLBACK, "Sparse group citation broadens");
        check(result.internationalReferenceScope() == ReferenceScope.SWEDISH_YEAR_FALLBACK, "Sparse group international broadens");
        check(result.citationReferencePopulationSize() == 33 && result.internationalReferencePopulationSize() == 33,
                "Generic fallbacks exclude explicit excluded, UMU and accepted EIDs");
        near(result.expectedTop10(), 30.0 / 33, "Generic citation proportion");
        near(result.expectedInternational(), 30.0 / 33, "Generic international proportion");
        for (String type : new String[] {"Future Type", "", null}) {
            SciValRecord unknown = record("unknown", type, 2024, 1, Set.of(100), 10, false, "Umeå University");
            ReferenceIndicators generic = calculator.calculate(unknown);
            check(generic.referenceSetSize() == 0, "Unknown group bypasses subject matching");
            check(generic.citationReferenceScope() == ReferenceScope.SWEDISH_FALLBACK, "Unknown generic citation");
            check(generic.internationalReferenceScope() == ReferenceScope.SWEDISH_YEAR_FALLBACK, "Unknown generic international");
            near(generic.expectedTop10(), 30.0 / 33, "Unknown generic value");
            unknown.setYear(null);
            generic = calculator.calculate(unknown);
            check(generic.internationalReferenceScope() == ReferenceScope.SWEDISH_ALL_YEARS_FALLBACK,
                    "Missing year uses all-years generic");
            check(calculator.calculateAll(List.of(unknown)).containsKey("unknown"), "Unknown focal must not be dropped");
        }
        SciValRecord one = record("one", "Article", 2020, -99, Set.of(-99), 10, true, "External");
        result = new SwedishReferenceIndicatorCalculator(List.of(one), Set.of("book"), "Umeå University").calculate(book);
        check(result.citationReferencePopulationSize() == 1 && result.internationalReferencePopulationSize() == 1,
                "Final fallback accepts every nonempty eligible benchmark");
        near(result.expectedTop10(), 1, "Single-record citation expectation");
        near(result.expectedInternational(), 1, "Single-record international expectation");
    }

    private static void missingTypeImport() throws Exception {
        Path file = Files.createTempFile("scival-missing-type-", ".xlsx");
        try {
            for (boolean missingHeader : List.of(false, true)) {
                try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                    var sheet = workbook.createSheet();
                    String[] required = {"DOI", "PubMed ID", "Language", "EID", "Publication type", "Source type",
                            "Field-Weighted Citation Impact", "Field-Weighted Outputs in Top Citation Percentiles, per percentile",
                            "Citations", "CiteScore (publication year)", "CiteScore percentile (publication year) *",
                            "Institutions", "Country/Region", "Authors", "Scopus Affiliation IDs",
                            "All Science Journal Classification (ASJC) code", "Topic Cluster number", "Topic number",
                            "Year", "Number of Institutions", "Title", "Scopus Source title", "Source ID",
                            "Publication link to Topic strength", "Number of Authors"};
                    Set<String> numeric = Set.of("Field-Weighted Citation Impact",
                            "Field-Weighted Outputs in Top Citation Percentiles, per percentile", "Citations",
                            "Year", "Number of Institutions", "Source ID", "Number of Authors");
                    var header = sheet.createRow(0);
                    for (int c = 0; c < 60; c++) header.createCell(c).setCellValue(c < required.length
                            ? (missingHeader && c == 4 ? "Unused type column" : required[c]) : "Unused " + c);
                    for (int r = 1; r <= 3; r++) {
                        var row = sheet.createRow(r);
                        for (int c = 0; c < 60; c++) {
                            var cell = row.createCell(c);
                            String name = c < required.length ? required[c] : "";
                            if (numeric.contains(name)) cell.setCellValue(name.equals("Year") ? 2024 : 1);
                            else cell.setCellValue(name.equals("EID") ? "test-" + r
                                    : name.equals("Institutions") ? "External"
                                    : name.equals("Country/Region") ? "Sweden" : "-");
                        }
                        if (r == 1) row.removeCell(row.getCell(4));
                        if (r == 2) row.getCell(4).setBlank();
                        if (r == 3) row.getCell(4).setCellValue("Future Type");
                    }
                    try (var out = Files.newOutputStream(file)) { workbook.write(out); }
                }
                var parsed = SciValDocumentTypePolicy.readEligibleRecords(file.toString());
                check(parsed.size() == 3, "Missing cells/header must not drop records");
                check(parsed.get(0).getScivalDocType().isEmpty() && parsed.get(1).getScivalDocType().isEmpty(),
                        "Missing/blank publication type becomes generic");
                for (SciValRecord focal : parsed) {
                    var calculator = new SwedishReferenceIndicatorCalculator(parsed, Set.of(focal.getEID()), "Umeå University");
                    check(Double.isFinite(calculator.calculate(focal).expectedTop10()), "Parsed unknown gets finite reference");
                }
            }
        } finally { Files.deleteIfExists(file); }
    }

    private static void outputColumns() throws Exception {
        SciValRecord record = record("test", "Article", 2024, -99, Set.of(-99), 10, true, "External");
        ReferenceIndicators value = new SwedishReferenceIndicatorCalculator(List.of(record), Set.of(), "Umeå University")
                .calculate(record);
        var tsv = MatchDiVAToSciVal.class.getDeclaredMethod("writeReferenceIndicators",
                java.io.PrintWriter.class, ReferenceIndicators.class);
        tsv.setAccessible(true);
        var xlsx = MatchDiVAToSciVal.class.getDeclaredMethod("writeReferenceIndicatorCells",
                org.apache.poi.ss.usermodel.Row.class, int.class, ReferenceIndicators.class,
                org.apache.poi.ss.usermodel.CellStyle.class);
        xlsx.setAccessible(true);
        for (ReferenceIndicators indicators : new ReferenceIndicators[] {value, null}) {
            var buffer = new java.io.StringWriter();
            try (var out = new java.io.PrintWriter(buffer)) { tsv.invoke(null, out, indicators); }
            check(buffer.toString().split("\\t", -1).length == 16, "TSV appends fifteen indicator fields");
            try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                var row = workbook.createSheet().createRow(0);
                xlsx.invoke(null, row, 21, indicators, workbook.createCellStyle());
                check(row.getLastCellNum() == 36, "XLSX preserves original columns and appends five");
            }
        }
    }

    private static void compareReports(Path actual, Path historical) throws Exception {
        Map<String, Map<String, String>> original = new HashMap<>();
        for (var row : readReport(historical.resolve("focal-publications.tsv"))) original.put(row.get("EID"), row);
        var current = readReport(actual.resolve("focal-publications.tsv"));
        check(current.size() == original.size(), "Same accepted EID cohort");
        for (var row : current) {
            var before = original.get(row.get("EID"));
            check(before != null, "Historical EID exists");
            for (String scenario : List.of("CURRENT", "EXCLUSIONS_ONLY", "GROUPED")) {
                for (String metric : List.of("EXPECTED_TOP10", "EXPECTED_TOP50", "EXPECTED_INTERNATIONAL",
                        "REFERENCE_SET_SIZE", "INTERNATIONAL_REFERENCE_SET_SIZE")) {
                    String column = scenario + "_" + metric;
                    check(row.get(column).equals(before.get(column)), "Real-data reproduction: " + row.get("EID") + " " + column);
                }
            }
        }
        System.out.println("Existing CURRENT, EXCLUSIONS_ONLY and GROUPED values reproduced for every focal EID.");
    }

    private static SciValRecord record(String eid, String type, int year, int cluster, Set<Integer> asjc,
                                       int percentile, boolean international, String institution) {
        return new SciValRecord("", eid, "", 1.0, List.of(institution), new HashSet<>(asjc), -99, cluster,
                year, 0, percentile, type, international ? List.of("Sweden", "Norway") : List.of("Sweden"),
                List.of(), international ? 1.0 : 0.0, "Title", "Source", 1, "Journal", "English", 0.0,
                0.0, 1L, "", 1, List.of("Author"));
    }

    private static PublicationTypePolicyPreCheck.Comparison find(
            List<PublicationTypePolicyPreCheck.Comparison> comparisons, String eid) {
        return comparisons.stream().filter(c -> c.eid().equals(eid)).findFirst().orElseThrow();
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1e-12, message + ": " + actual + " != " + expected);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
