package org.cc.divaToSciVal;

import cc.FilePathConstants;
import cc.analysis.scival.SciValParser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static cc.analysis.TopicsAndTopicClusters.RecordToTopicsAndCitationIndicators.getSciValExcelFiles;
import static cc.analysis.scival.SciValParser.getSciValRecords;
import static org.cc.divaToSciVal.MatchDiVAToSciVal.validateAfids;

/**
 * Compatibility runner for calculating reference indicators from an existing
 * DiVA-to-SciVal match file.
 *
 * <p>The primary pipeline is now {@link MatchDiVAToSciVal}, which performs matching
 * and indicator enrichment in one run. This class remains useful for checking an
 * already materialized {@code divaToScival.parquet} file. The methodology itself is
 * implemented only by {@link SwedishReferenceIndicatorCalculator}.</p>
 */
public class RunCitationIndicatorPipelineOnAlreadyMaterializedParquetFile {

    private static final Set<String> ACCEPTED_MATCH_STATUSES = Set.of(
            "EXACT", "EXACT_SUSPECT", "TEXT_AUTO_MATCH");

    public static void main(String[] args) throws SQLException, IOException {
        Set<String> focalEids = readAcceptedFocalEids("divaToScival.parquet");
        List<SciValParser.SciValRecord> rawRecords =
                readSciValRecords(FilePathConstants.SCIVAL_RAW_XLSX_LATEST);

        Map<String, SciValParser.SciValRecord> recordsByEid = new HashMap<>();
        for(SciValParser.SciValRecord record : rawRecords) {
            String normalizedEid =
                    SwedishReferenceIndicatorCalculator.normalizeEid(record.getEID());
            if(!normalizedEid.isEmpty()) recordsByEid.putIfAbsent(normalizedEid, record);
        }

        Map<String, SciValParser.SciValRecord> focalRecordsByEid = new HashMap<>();
        int excludedFocalRecords = 0;
        for(String focalEid : focalEids) {
            SciValParser.SciValRecord record = recordsByEid.get(focalEid);
            if(record == null) {
                throw new IllegalStateException(
                        "Matched focal EID is absent from the SciVal export: " + focalEid);
            }
            if (SciValDocumentTypePolicy.excluded(record.getScivalDocType())) {
                excludedFocalRecords++;
            } else {
                focalRecordsByEid.put(focalEid, record);
            }
        }

        System.out.println("Previously accepted EIDs excluded by document-type policy: " + excludedFocalRecords);
        List<SciValParser.SciValRecord> swedishRecords = rawRecords.stream()
                .filter(record -> !SciValDocumentTypePolicy.excluded(record.getScivalDocType())).toList();
        if (focalRecordsByEid.isEmpty()) {
            writeIndicators(Map.of(), new File("citationDataTemporary.txt"));
            return;
        }
        SwedishReferenceIndicatorCalculator calculator =
                new SwedishReferenceIndicatorCalculator(
                        swedishRecords, focalRecordsByEid.keySet(), "Umeå University");
        Map<String, ReferenceIndicators> indicatorsByEid =
                calculator.calculateAll(focalRecordsByEid.values());

        writeIndicators(indicatorsByEid, new File("citationDataTemporary.txt"));
    }

    private static Set<String> readAcceptedFocalEids(String parquetFile) throws SQLException {
        Set<String> focalEids = new HashSet<>();
        try(Connection connection = DriverManager.getConnection("jdbc:duckdb:");
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT EID, STATUS FROM read_parquet(?)")) {
            statement.setString(1, parquetFile);
            try(ResultSet rows = statement.executeQuery()) {
                while(rows.next()) {
                    String status = rows.getString("STATUS");
                    String normalizedEid = SwedishReferenceIndicatorCalculator.normalizeEid(
                            rows.getString("EID"));
                    if(ACCEPTED_MATCH_STATUSES.contains(status) && !normalizedEid.isEmpty()) {
                        focalEids.add(normalizedEid);
                    }
                }
            }
        }
        return focalEids;
    }

    private static List<SciValParser.SciValRecord> readSciValRecords(String directory)
            throws IOException {
        List<SciValParser.SciValRecord> records = new ArrayList<>(10_000);
        for(File file : getSciValExcelFiles(directory)) {
            List<SciValParser.SciValRecord> parsed =
                    getSciValRecords(file.getAbsolutePath(), java.util.Collections.emptySet());
            validateAfids(parsed.stream().filter(record -> !SciValDocumentTypePolicy.excluded(record.getScivalDocType())).toList());
            records.addAll(parsed);
        }
        return records;
    }

    private static void writeIndicators(
            Map<String, ReferenceIndicators> indicatorsByEid, File outputFile)
            throws IOException {
        try(PrintWriter output = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            output.println(
                    "FOCAL_EID\tOBSERVED_TOP10\tOBSERVED_TOP50"
                            + "\tOBSERVED_IS_INTERNATIONAL\tEXPECTED_TOP10"
                            + "\tEXPECTED_TOP50\tEXPECTED_INTERNATIONAL"
                            + "\tREFERENCE_SET_SIZE\tINTERNATIONAL_REFERENCE_SET_SIZE"
                            + "\tUSED_CITATION_FALLBACK\tUSED_INTERNATIONAL_FALLBACK"
                            + "\tNORMALIZATION_ARTIFACT_GROUP\tCITATION_REFERENCE_SCOPE"
                            + "\tINTERNATIONAL_REFERENCE_SCOPE\tCITATION_REFERENCE_POPULATION_SIZE"
                            + "\tINTERNATIONAL_REFERENCE_POPULATION_SIZE");

            for(Map.Entry<String, ReferenceIndicators> entry : indicatorsByEid.entrySet()) {
                ReferenceIndicators indicators = entry.getValue();
                output.printf(Locale.ROOT,
                        "%s\t%d\t%d\t%d\t%.6f\t%.6f\t%.6f\t%d\t%d\t%b\t%b\t%s\t%s\t%s\t%d\t%d%n",
                        entry.getKey(),
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
        }
    }
}
