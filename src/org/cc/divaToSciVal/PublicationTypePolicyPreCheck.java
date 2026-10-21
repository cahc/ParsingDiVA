package org.cc.divaToSciVal;

import cc.FilePathConstants;
import cc.analysis.scival.SciValParser;
import org.cc.divaToSciVal.SciValDocumentTypePolicy.Group;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static cc.analysis.TopicsAndTopicClusters.RecordToTopicsAndCitationIndicators.getSciValExcelFiles;
import static cc.analysis.scival.SciValParser.getSciValRecords;

/**
 * Compares the production document-type policy with historical mixed-type references.
 * Production matching and indicator outputs are never invoked or overwritten.
 * Expectations and summaries are weighted once per unique accepted EID.
 */
public final class PublicationTypePolicyPreCheck {
    private static final String INSTITUTION = "Umeå University";
    private static final Set<String> ACCEPTED = Set.of("EXACT", "EXACT_SUSPECT", "TEXT_AUTO_MATCH");

    record TrialReference(ReferenceIndicators indicators, String citationScope,
                          String internationalScope, int citationFallbackPoolSize,
                          int internationalFallbackPoolSize) { }

    record Comparison(String eid, String documentType, Group group, String disposition,
                      ReferenceIndicators current, ReferenceIndicators exclusionsOnly,
                      TrialReference proposed) { }

    /** Runs with defaults, or: match TSV/parquet, SciVal directory, new output directory. */
    public static void main(String[] args) throws IOException, SQLException {
        if (args.length > 3 || (args.length == 1 && "--help".equals(args[0]))) {
            System.out.println("Usage: PublicationTypePolicyPreCheck [match.tsv|match.parquet]"
                    + " [SciVal-directory] [new-output-directory]");
            return;
        }
        Path matchFile = Path.of(args.length > 0 ? args[0] : "DIVA_PID_TO_SCIVAL_EID.txt");
        String sciValDirectory = args.length > 1 ? args[1] : FilePathConstants.SCIVAL_RAW_XLSX_LATEST;
        Path output = Path.of(args.length > 2 ? args[2] : "publication-type-policy-precheck");
        if (Files.exists(output)) {
            throw new IOException("Pre-check output already exists; choose a new directory: " + output);
        }

        Map<String, Set<String>> pidsByEid = readAcceptedMatches(matchFile);
        if (pidsByEid.isEmpty()) throw new IllegalArgumentException("No accepted EIDs in " + matchFile);
        File[] files = getSciValExcelFiles(sciValDirectory);
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No SciVal XLSX exports in " + sciValDirectory);
        }
        Arrays.sort(files, Comparator.comparing(File::getAbsolutePath));
        List<SciValParser.SciValRecord> records = new ArrayList<>();
        for (File file : files) {
            // Load all types for the inventory; exclusions are applied separately per scenario.
            List<SciValParser.SciValRecord> parsed = getSciValRecords(
                    file.getAbsolutePath(), Collections.emptySet());
            MatchDiVAToSciVal.validateAfids(parsed);
            records.addAll(parsed);
        }
        List<Comparison> comparisons = evaluate(records, pidsByEid.keySet());
        Files.createDirectories(output);
        writeDetails(output.resolve("focal-publications.tsv"), comparisons, pidsByEid);
        writeSummary(output.resolve("summary.tsv"), comparisons, pidsByEid);
        writeInventory(output.resolve("document-types.tsv"), records, pidsByEid);
        writeReadme(output.resolve("README.txt"), matchFile, sciValDirectory, records, comparisons);
        System.out.println("Pre-check complete: " + comparisons.size() + " unique accepted EIDs; "
                + pidsByEid.values().stream().mapToInt(Set::size).sum() + " DiVA PID links");
        System.out.println("Reports: " + output.toAbsolutePath());
        for (Group group : Group.values()) {
            List<Comparison> members = comparisons.stream().filter(c -> c.group() == group).toList();
            long citationFallback = members.stream().filter(c -> c.proposed() != null
                    && c.proposed().indicators().usedCitationFallback()).count();
            long internationalFallback = members.stream().filter(c -> c.proposed() != null
                    && c.proposed().indicators().usedInternationalFallback()).count();
            System.out.printf(Locale.ROOT, "%s: focal=%d, citation fallback=%d, international fallback=%d%n",
                    group, members.size(), citationFallback, internationalFallback);
        }
    }

    static Map<String, Set<String>> readAcceptedMatches(Path file) throws IOException, SQLException {
        Map<String, Set<String>> result = new TreeMap<>();
        if (file.toString().toLowerCase(Locale.ROOT).endsWith(".parquet")) {
            try (Connection connection = DriverManager.getConnection("jdbc:duckdb:");
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT CAST(PID AS VARCHAR), EID, STATUS FROM read_parquet(?)")) {
                statement.setString(1, file.toAbsolutePath().toString());
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) addMatch(result, rows.getString(1), rows.getString(2), rows.getString(3));
                }
            }
        } else {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String header = reader.readLine();
                if (header == null) throw new IOException("Empty match file: " + file);
                List<String> names = Arrays.asList(header.replace("\uFEFF", "").split("\t", -1));
                int pid = names.indexOf("PID"), eid = names.indexOf("EID"), status = names.indexOf("STATUS");
                if (pid < 0 || eid < 0 || status < 0) throw new IOException("Expected PID, EID, STATUS columns");
                int required = Math.max(pid, Math.max(eid, status));
                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank()) continue;
                    String[] cells = line.split("\t", -1);
                    if (cells.length <= required) throw new IOException("Incomplete match row " + lineNumber);
                    addMatch(result, cells[pid], cells[eid], cells[status]);
                }
            }
        }
        return result;
    }

    private static void addMatch(Map<String, Set<String>> result, String pid, String eid, String status) {
        String normalized = SwedishReferenceIndicatorCalculator.normalizeEid(eid);
        if (ACCEPTED.contains(status) && !normalized.isEmpty()) {
            result.computeIfAbsent(normalized, ignored -> new TreeSet<>()).add(pid == null ? "" : pid);
        }
    }

    static List<Comparison> evaluate(Collection<SciValParser.SciValRecord> records, Set<String> focalEids) {
        Map<String, SciValParser.SciValRecord> unique = uniqueRecords(records);
        Set<String> normalizedFocalEids = new TreeSet<>();
        focalEids.forEach(eid -> normalizedFocalEids.add(SwedishReferenceIndicatorCalculator.normalizeEid(eid)));
        normalizedFocalEids.remove("");
        List<SciValParser.SciValRecord> currentRecords = unique.values().stream()
                .filter(r -> !SciValDocumentTypePolicy.legacyExcluded(r.getScivalDocType())).toList();
        List<SciValParser.SciValRecord> exclusionsOnlyRecords = unique.values().stream()
                .filter(r -> !SciValDocumentTypePolicy.excluded(r.getScivalDocType())).toList();
        SwedishReferenceIndicatorCalculator current = calculatorOrNull(
                currentRecords, normalizedFocalEids, SwedishReferenceIndicatorCalculator.ReferencePolicy.LEGACY_MIXED);
        SwedishReferenceIndicatorCalculator exclusionsOnly = calculatorOrNull(
                exclusionsOnlyRecords, normalizedFocalEids, SwedishReferenceIndicatorCalculator.ReferencePolicy.EXCLUSIONS_ONLY);
        SwedishReferenceIndicatorCalculator grouped = calculatorOrNull(
                exclusionsOnlyRecords, normalizedFocalEids, SwedishReferenceIndicatorCalculator.ReferencePolicy.GROUPED);

        List<Comparison> result = new ArrayList<>();
        for (String eid : normalizedFocalEids) {
            SciValParser.SciValRecord focal = unique.get(eid);
            if (focal == null) {
                result.add(new Comparison(eid, "", null, "MISSING_SCIVAL_RECORD", null, null, null));
                continue;
            }
            String type = focal.getScivalDocType();
            Group group = SciValDocumentTypePolicy.group(type);
            String disposition = SciValDocumentTypePolicy.disposition(type);
            ReferenceIndicators old = SciValDocumentTypePolicy.legacyExcluded(type)
                    ? null : current == null ? null : current.calculate(focal);
            ReferenceIndicators filtered = SciValDocumentTypePolicy.excluded(type)
                    ? null : exclusionsOnly == null ? null : exclusionsOnly.calculate(focal);
            TrialReference trial = null;
            if (!SciValDocumentTypePolicy.excluded(type)) {
                if (grouped == null) throw new IllegalArgumentException("No eligible external Swedish benchmark remains");
                ReferenceIndicators calculated = grouped.calculate(focal);
                trial = new TrialReference(calculated, calculated.citationReferenceScope().name(),
                        calculated.internationalReferenceScope().name(), grouped.groupPopulationSize(group),
                        grouped.groupInternationalPopulationSize(group, focal.getYear()));
            }
            result.add(new Comparison(eid, type, group, disposition, old, filtered, trial));
        }
        return result;
    }

    private static Map<String, SciValParser.SciValRecord> uniqueRecords(
            Collection<SciValParser.SciValRecord> records) {
        Map<String, SciValParser.SciValRecord> unique = new TreeMap<>();
        for (SciValParser.SciValRecord record : records) {
            if (record == null) continue;
            String eid = SwedishReferenceIndicatorCalculator.normalizeEid(record.getEID());
            if (!eid.isEmpty()) unique.putIfAbsent(eid, record);
        }
        return unique;
    }

    private static SwedishReferenceIndicatorCalculator calculatorOrNull(
            List<SciValParser.SciValRecord> records, Set<String> focalEids,
            SwedishReferenceIndicatorCalculator.ReferencePolicy policy) {
        boolean hasBenchmark = records.stream().anyMatch(r ->
                !focalEids.contains(SwedishReferenceIndicatorCalculator.normalizeEid(r.getEID()))
                        && (r.getInstitutions() == null || !r.getInstitutions().contains(INSTITUTION)));
        return hasBenchmark ? new SwedishReferenceIndicatorCalculator(records, focalEids, INSTITUTION, policy) : null;
    }

    private static void writeDetails(Path file, List<Comparison> comparisons,
                                     Map<String, Set<String>> pidsByEid) throws IOException {
        try (PrintWriter out = writer(file)) {
            List<Object> header = new ArrayList<>(List.of("EID", "DIVA_PIDS", "DIVA_PID_COUNT", "SCIVAL_DOCUMENT_TYPE",
                    "NORMALIZATION_ARTIFACT_GROUP", "PROPOSED_DISPOSITION"));
            for (String scenario : List.of("CURRENT", "EXCLUSIONS_ONLY", "GROUPED")) {
                for (String field : List.of("REFERENCE_SET_SIZE", "INTERNATIONAL_REFERENCE_SET_SIZE",
                        "USED_CITATION_FALLBACK", "USED_INTERNATIONAL_FALLBACK", "EXPECTED_TOP10",
                        "EXPECTED_TOP50", "EXPECTED_INTERNATIONAL")) header.add(scenario + "_" + field);
            }
            header.addAll(List.of("GROUPED_CITATION_SCOPE", "GROUPED_INTERNATIONAL_SCOPE",
                    "GROUP_CITATION_FALLBACK_POOL_SIZE", "GROUP_INTERNATIONAL_FALLBACK_POOL_SIZE",
                    "DELTA_TOP10_PP", "DELTA_TOP50_PP", "DELTA_INTERNATIONAL_PP",
                    "CITATION_REFERENCE_POPULATION_SIZE", "INTERNATIONAL_REFERENCE_POPULATION_SIZE"));
            row(out, header);
            for (Comparison c : comparisons) {
                Set<String> pids = pidsByEid.get(c.eid());
                List<Object> values = new ArrayList<>(Arrays.asList(c.eid(), String.join(";", pids), pids.size(),
                        c.documentType(), c.group(), c.disposition()));
                appendIndicators(values, c.current());
                appendIndicators(values, c.exclusionsOnly());
                ReferenceIndicators proposed = c.proposed() == null ? null : c.proposed().indicators();
                appendIndicators(values, proposed);
                values.addAll(Arrays.asList(c.proposed() == null ? c.disposition() : c.proposed().citationScope(),
                        c.proposed() == null ? c.disposition() : c.proposed().internationalScope(),
                        c.proposed() == null ? null : c.proposed().citationFallbackPoolSize(),
                        c.proposed() == null ? null : c.proposed().internationalFallbackPoolSize()));
                for (int indicator = 0; indicator < 3; indicator++) {
                    values.add(100 * (expectation(proposed, indicator) - expectation(c.current(), indicator)));
                }
                values.add(proposed == null ? null : proposed.citationReferencePopulationSize());
                values.add(proposed == null ? null : proposed.internationalReferencePopulationSize());
                row(out, values);
            }
        }
    }

    private static void appendIndicators(List<Object> values, ReferenceIndicators r) {
        if (r == null) { for (int i = 0; i < 7; i++) values.add(null); return; }
        values.addAll(List.of(r.referenceSetSize(), r.internationalReferenceSetSize(), r.usedCitationFallback(),
                r.usedInternationalFallback(), r.expectedTop10(), r.expectedTop50(), r.expectedInternational()));
    }

    private static void writeSummary(Path file, List<Comparison> comparisons,
                                     Map<String, Set<String>> pidsByEid) throws IOException {
        Map<String, List<Comparison>> cohorts = new TreeMap<>();
        for (Comparison c : comparisons) {
            String group = c.group() == null ? c.disposition() : c.group().name();
            cohorts.computeIfAbsent("GROUP\t" + group, ignored -> new ArrayList<>()).add(c);
            cohorts.computeIfAbsent("DOCUMENT_TYPE\t" + clean(c.documentType()), ignored -> new ArrayList<>()).add(c);
        }
        cohorts.put("ALL\tALL_ACCEPTED", comparisons);
        cohorts.put("ALL\tPROPOSED_INCLUDED", comparisons.stream().filter(c -> c.proposed() != null).toList());
        try (PrintWriter out = writer(file)) {
            row(out, List.of("LEVEL", "COHORT", "SCENARIO", "INDICATOR", "FOCAL_EIDS", "DIVA_PID_LINKS",
                    "AVAILABLE_EXPECTATIONS", "UNAVAILABLE_EXPECTATIONS", "REFERENCE_SIZE_MIN", "REFERENCE_SIZE_P10",
                    "REFERENCE_SIZE_MEDIAN", "REFERENCE_SIZE_P90", "REFERENCE_SIZE_MAX", "FALLBACK_REQUIRED_COUNT",
                    "FALLBACK_REQUIRED_RATE", "MEAN_EXPECTED", "PAIRED_EIDS", "PAIRED_CURRENT_MEAN",
                    "PAIRED_SCENARIO_MEAN", "MEAN_DELTA_PP", "MEDIAN_DELTA_PP", "P90_ABSOLUTE_DELTA_PP", "UNRESTRICTED_FALLBACK_COUNT"));
            for (Map.Entry<String, List<Comparison>> cohort : cohorts.entrySet()) {
                String[] label = cohort.getKey().split("\t", -1);
                List<Comparison> members = cohort.getValue();
                for (String scenario : List.of("CURRENT", "EXCLUSIONS_ONLY", "GROUPED")) {
                    for (int indicator = 0; indicator < 3; indicator++) {
                        List<Double> sizes = new ArrayList<>(), expected = new ArrayList<>();
                        List<Double> oldPaired = new ArrayList<>(), newPaired = new ArrayList<>(), deltas = new ArrayList<>();
                        int fallback = 0, unrestricted = 0;
                        for (Comparison c : members) {
                            ReferenceIndicators r = scenarioIndicators(c, scenario);
                            if (r != null) {
                                sizes.add((double) (indicator == 2 ? r.internationalReferenceSetSize() : r.referenceSetSize()));
                                if (indicator == 2 ? r.usedInternationalFallback() : r.usedCitationFallback()) fallback++;
                                ReferenceScope scope = indicator == 2 ? r.internationalReferenceScope() : r.citationReferenceScope();
                                if (scope == ReferenceScope.SWEDISH_FALLBACK || scope == ReferenceScope.SWEDISH_YEAR_FALLBACK
                                        || scope == ReferenceScope.SWEDISH_ALL_YEARS_FALLBACK) unrestricted++;
                            }
                            double value = expectation(r, indicator), old = expectation(c.current(), indicator);
                            if (Double.isFinite(value)) expected.add(value);
                            if (Double.isFinite(value) && Double.isFinite(old)) {
                                oldPaired.add(old); newPaired.add(value); deltas.add(100 * (value - old));
                            }
                        }
                        row(out, Arrays.asList(label[0], label[1], scenario,
                                List.of("TOP10", "TOP50", "INTERNATIONAL").get(indicator), members.size(),
                                members.stream().mapToInt(c -> pidsByEid.get(c.eid()).size()).sum(), expected.size(),
                                members.size() - expected.size(), quantile(sizes, 0), quantile(sizes, .1),
                                quantile(sizes, .5), quantile(sizes, .9), quantile(sizes, 1), fallback,
                                sizes.isEmpty() ? Double.NaN : (double) fallback / sizes.size(), mean(expected),
                                deltas.size(), mean(oldPaired), mean(newPaired), mean(deltas), quantile(deltas, .5),
                                quantile(deltas.stream().map(Math::abs).toList(), .9), unrestricted));
                    }
                }
            }
        }
    }

    private static ReferenceIndicators scenarioIndicators(Comparison c, String scenario) {
        return switch (scenario) {
            case "CURRENT" -> c.current();
            case "EXCLUSIONS_ONLY" -> c.exclusionsOnly();
            case "GROUPED" -> c.proposed() == null ? null : c.proposed().indicators();
            default -> throw new IllegalArgumentException(scenario);
        };
    }

    private static double expectation(ReferenceIndicators r, int indicator) {
        if (r == null) return Double.NaN;
        return switch (indicator) {
            case 0 -> r.expectedTop10();
            case 1 -> r.expectedTop50();
            case 2 -> r.expectedInternational();
            default -> throw new IllegalArgumentException("Unknown indicator");
        };
    }

    static double quantile(List<Double> values, double probability) {
        if (values.isEmpty()) return Double.NaN;
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compare);
        double position = probability * (sorted.size() - 1);
        int lower = (int) Math.floor(position), upper = (int) Math.ceil(position);
        return sorted.get(lower) + (position - lower) * (sorted.get(upper) - sorted.get(lower));
    }

    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    private static void writeInventory(Path file, List<SciValParser.SciValRecord> records,
                                       Map<String, Set<String>> pidsByEid) throws IOException {
        Map<String, long[]> counts = new TreeMap<>();
        Map<String, SciValParser.SciValRecord> unique = uniqueRecords(records);
        for (SciValParser.SciValRecord r : records) {
            if (r != null) counts.computeIfAbsent(SciValDocumentTypePolicy.normalize(r.getScivalDocType()),
                    ignored -> new long[4])[0]++;
        }
        for (Map.Entry<String, SciValParser.SciValRecord> entry : unique.entrySet()) {
            SciValParser.SciValRecord r = entry.getValue();
            long[] count = counts.get(SciValDocumentTypePolicy.normalize(r.getScivalDocType()));
            count[1]++;
            if (pidsByEid.containsKey(entry.getKey())) count[2]++;
            if (!pidsByEid.containsKey(entry.getKey())
                    && (r.getInstitutions() == null || !r.getInstitutions().contains(INSTITUTION))) count[3]++;
        }
        try (PrintWriter out = writer(file)) {
            row(out, List.of("DOCUMENT_TYPE", "CURRENT_EXCLUDED", "PROPOSED_DISPOSITION", "GROUP",
                    "EXPORT_ROWS", "UNIQUE_EIDS", "ACCEPTED_FOCAL_EIDS", "EXTERNAL_NON_FOCAL_EIDS"));
            for (Map.Entry<String, long[]> entry : counts.entrySet()) {
                String type = entry.getKey(); long[] n = entry.getValue();
                row(out, Arrays.asList(type, SciValDocumentTypePolicy.legacyExcluded(type),
                        SciValDocumentTypePolicy.disposition(type), SciValDocumentTypePolicy.group(type),
                        n[0], n[1], n[2], n[3]));
            }
        }
    }

    private static void writeReadme(Path file, Path matches, String directory,
                                    List<SciValParser.SciValRecord> records, List<Comparison> comparisons) throws IOException {
        try (PrintWriter out = writer(file)) {
            out.println("Document-type policy comparison; CURRENT preserves the historical mixed-type policy.");
            out.println("Match input: " + matches.toAbsolutePath());
            out.println("SciVal directory: " + Path.of(directory).toAbsolutePath());
            out.println("Export rows: " + records.size() + "; unique non-empty EIDs: " + uniqueRecords(records).size());
            out.println("Accepted focal EIDs: " + comparisons.size());
            out.println("Missing focal records: " + comparisons.stream()
                    .filter(c -> "MISSING_SCIVAL_RECORD".equals(c.disposition())).count());
            out.println("CURRENT excludes Retracted and Abstract Report, with mixed-type references and existing fallbacks.");
            out.println("EXCLUSIONS_ONLY additionally excludes Erratum, Editorial and Conference Review; references remain mixed.");
            out.println("GROUPED uses the production policy: groups, group-specific fallbacks, then unrestricted Swedish fallbacks.");
            out.println("Unknown/blank types receive generic Swedish expectations; Chapter and Book Chapter are aliases.");
            out.println("All scenarios exclude Umeå University and all accepted focal EIDs from benchmarks, deduplicated by EID.");
            out.println("First export row wins for duplicate EIDs; input files are sorted by absolute path.");
            out.println("Subject references use (same exact ASJC combination OR same topic cluster), restricted to group for GROUPED.");
            out.println("Citation references use all imported years; international references use focal SciVal year +/-1.");
            out.println("Subject/group populations require 25 citation or 20 international records; smaller groups broaden to Swedish benchmarks.");
            out.println("Generic international fallback uses year +/-1 if at least 20 records, otherwise all years; final non-empty benchmarks always supply values.");
            out.println("Blank numeric expectations/deltas mean unavailable; valid zeros are written as 0.000000.");
            out.println("summary.tsv reports one observation per unique EID, with GROUP and DOCUMENT_TYPE breakdowns.");
            out.println("Fallback rates use EIDs with calculated subject sets; unrestricted Swedish fallback counts are reported separately.");
            out.println("Expected means use available expectations; paired comparisons use identical EIDs on both sides.");
            out.println("Deltas are scenario minus CURRENT in percentage points; quantiles use linear interpolation.");
            out.println("focal-publications.tsv lists every accepted EID and all linked DiVA PIDs, including proposed exclusions.");
            out.println("document-types.tsv inventories all parsed types, including excluded and unknown types.");
            out.println("This is a fixed-match sensitivity analysis: it does not rerun matching or estimate lost/reassigned text matches.");
            out.println("Expectations are recomputed from this export snapshot; they need not equal historical match-file values.");
        }
    }

    private static PrintWriter writer(Path file) throws IOException {
        return new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8));
    }

    private static void row(PrintWriter out, List<?> values) {
        out.println(values.stream().map(value -> {
            if (value == null) return "";
            if (value instanceof Double number) {
                return Double.isFinite(number) ? String.format(Locale.ROOT, "%.6f", number) : "";
            }
            return clean(value.toString());
        }).collect(java.util.stream.Collectors.joining("\t")));
        if (out.checkError()) throw new java.io.UncheckedIOException(new IOException("Failed writing pre-check report"));
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
