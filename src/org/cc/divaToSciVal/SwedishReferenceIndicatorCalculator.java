package org.cc.divaToSciVal;

import cc.analysis.scival.SciValParser;
import org.roaringbitmap.RoaringBitmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Calculates publication-specific observed values and subject-matched Swedish
 * expectations. The reference set is the inclusive union of the exact ASJC
 * partition and the SciVal topic cluster.
 */
public final class SwedishReferenceIndicatorCalculator {

    public static final int MIN_CITATION_REFERENCE_SET_SIZE = 25;
    public static final int MIN_INTERNATIONAL_REFERENCE_SET_SIZE = 20;
    public static final int INTERNATIONAL_YEAR_RADIUS = 1;

    private final List<SciValParser.SciValRecord> benchmarkRecords;
    private final Map<Integer, RoaringBitmap> topicClusterIndex = new HashMap<>();
    private final Map<List<Integer>, RoaringBitmap> asjcPartitionIndex = new HashMap<>();
    private final Map<Integer, Integer> internationalPublicationsByYear = new TreeMap<>();
    private final Map<Integer, Integer> allPublicationsByYear = new TreeMap<>();
    private final double fallbackTop10;
    private final double fallbackTop50;
    private final double fallbackInternationalAllYears;

    /**
     * @param allSwedishRecords all SciVal records in the Swedish comparison universe
     * @param focalEids EIDs selected as focal publications; these can never benchmark themselves
     * @param focalInstitutionName institution name excluded from the Swedish benchmark population
     */
    public SwedishReferenceIndicatorCalculator(
            Collection<SciValParser.SciValRecord> allSwedishRecords,
            Collection<String> focalEids,
            String focalInstitutionName) {

        if(allSwedishRecords == null || allSwedishRecords.isEmpty()) {
            throw new IllegalArgumentException("Swedish SciVal records must not be empty");
        }

        Set<String> normalizedFocalEids = new HashSet<>();
        if(focalEids != null) {
            for(String eid : focalEids) {
                String normalized = normalizeEid(eid);
                if(!normalized.isEmpty()) normalizedFocalEids.add(normalized);
            }
        }

        // Deduplicate by EID so repeated export rows cannot weight the reference values.
        Map<String, SciValParser.SciValRecord> uniqueBenchmarkRecords = new LinkedHashMap<>();
        for(SciValParser.SciValRecord record : allSwedishRecords) {
            if(record == null) continue;
            String normalizedEid = normalizeEid(record.getEID());
            if(normalizedEid.isEmpty() || normalizedFocalEids.contains(normalizedEid)) continue;
            if(hasInstitution(record, focalInstitutionName)) continue;
            uniqueBenchmarkRecords.putIfAbsent(normalizedEid, record);
        }

        benchmarkRecords = new ArrayList<>(uniqueBenchmarkRecords.values());
        if(benchmarkRecords.isEmpty()) {
            throw new IllegalArgumentException(
                    "No Swedish non-focal-organization benchmark records remain");
        }

        int top10Count = 0;
        int top50Count = 0;
        int internationalCount = 0;

        for(int index = 0; index < benchmarkRecords.size(); index++) {
            SciValParser.SciValRecord record = benchmarkRecords.get(index);

            Integer topicCluster = record.getTopicCluster();
            if(topicCluster != null && topicCluster != -99) {
                topicClusterIndex.computeIfAbsent(
                        topicCluster, ignored -> new RoaringBitmap()).add(index);
            }

            Set<Integer> asjc = record.getASJC();
            if(hasValidAsjc(asjc)) {
                asjcPartitionIndex.computeIfAbsent(
                        asjcPartitionKey(asjc), ignored -> new RoaringBitmap()).add(index);
            }

            if(isTop(record, 10)) top10Count++;
            if(isTop(record, 50)) top50Count++;

            Integer year = record.getYear();
            if(year != null) {
                allPublicationsByYear.merge(year, 1, Integer::sum);
                if(isInternational(record)) {
                    internationalPublicationsByYear.merge(year, 1, Integer::sum);
                }
            }
            if(isInternational(record)) internationalCount++;
        }

        fallbackTop10 = (double) top10Count / benchmarkRecords.size();
        fallbackTop50 = (double) top50Count / benchmarkRecords.size();
        fallbackInternationalAllYears = (double) internationalCount / benchmarkRecords.size();
    }

    public Map<String, ReferenceIndicators> calculateAll(
            Collection<SciValParser.SciValRecord> focalRecords) {

        List<SciValParser.SciValRecord> sortedFocalRecords = new ArrayList<>();
        if(focalRecords != null) {
            for(SciValParser.SciValRecord record : focalRecords) {
                if(record != null && !normalizeEid(record.getEID()).isEmpty()) {
                    sortedFocalRecords.add(record);
                }
            }
        }
        sortedFocalRecords.sort(Comparator.comparing(
                record -> normalizeEid(record.getEID())));

        Map<String, ReferenceIndicators> result = new LinkedHashMap<>();
        for(SciValParser.SciValRecord focalRecord : sortedFocalRecords) {
            String normalizedEid = normalizeEid(focalRecord.getEID());
            result.putIfAbsent(normalizedEid, calculate(focalRecord));
        }
        return result;
    }

    public ReferenceIndicators calculate(SciValParser.SciValRecord focalRecord) {
        if(focalRecord == null) throw new IllegalArgumentException("Focal record must not be null");

        RoaringBitmap referenceSet = new RoaringBitmap();

        Integer topicCluster = focalRecord.getTopicCluster();
        if(topicCluster != null && topicCluster != -99) {
            referenceSet.or(topicClusterIndex.getOrDefault(topicCluster, new RoaringBitmap()));
        }

        Set<Integer> asjc = focalRecord.getASJC();
        if(hasValidAsjc(asjc)) {
            referenceSet.or(asjcPartitionIndex.getOrDefault(
                    asjcPartitionKey(asjc), new RoaringBitmap()));
        }

        int top10ReferenceRecords = 0;
        int top50ReferenceRecords = 0;
        int internationalWindowReferenceRecords = 0;
        int internationalReferenceRecords = 0;
        Integer focalYear = focalRecord.getYear();

        for(int index : referenceSet) {
            SciValParser.SciValRecord benchmarkRecord = benchmarkRecords.get(index);
            if(isTop(benchmarkRecord, 10)) top10ReferenceRecords++;
            if(isTop(benchmarkRecord, 50)) top50ReferenceRecords++;

            Integer benchmarkYear = benchmarkRecord.getYear();
            if(focalYear != null && benchmarkYear != null
                    && Math.abs(benchmarkYear - focalYear) <= INTERNATIONAL_YEAR_RADIUS) {
                internationalWindowReferenceRecords++;
                if(isInternational(benchmarkRecord)) internationalReferenceRecords++;
            }
        }

        int referenceSetSize = referenceSet.getCardinality();
        boolean usedCitationFallback =
                referenceSetSize < MIN_CITATION_REFERENCE_SET_SIZE;
        double expectedTop10 = usedCitationFallback
                ? fallbackTop10
                : (double) top10ReferenceRecords / referenceSetSize;
        double expectedTop50 = usedCitationFallback
                ? fallbackTop50
                : (double) top50ReferenceRecords / referenceSetSize;

        boolean usedInternationalFallback =
                internationalWindowReferenceRecords < MIN_INTERNATIONAL_REFERENCE_SET_SIZE;
        double expectedInternational = usedInternationalFallback
                ? fallbackInternationalForYear(focalYear)
                : (double) internationalReferenceRecords / internationalWindowReferenceRecords;

        return new ReferenceIndicators(
                isTop(focalRecord, 10) ? 1 : 0,
                isTop(focalRecord, 50) ? 1 : 0,
                isInternational(focalRecord) ? 1 : 0,
                expectedTop10,
                expectedTop50,
                expectedInternational,
                referenceSetSize,
                internationalWindowReferenceRecords,
                usedCitationFallback,
                usedInternationalFallback);
    }

    private double fallbackInternationalForYear(Integer focalYear) {
        if(focalYear == null) return fallbackInternationalAllYears;

        int internationalPublications = 0;
        int allPublications = 0;
        for(int year = focalYear - INTERNATIONAL_YEAR_RADIUS;
            year <= focalYear + INTERNATIONAL_YEAR_RADIUS;
            year++) {
            internationalPublications += internationalPublicationsByYear.getOrDefault(year, 0);
            allPublications += allPublicationsByYear.getOrDefault(year, 0);
        }
        return allPublications == 0
                ? fallbackInternationalAllYears
                : (double) internationalPublications / allPublications;
    }

    private static boolean hasValidAsjc(Set<Integer> asjc) {
        return asjc != null && !asjc.isEmpty() && !asjc.contains(-99);
    }

    private static List<Integer> asjcPartitionKey(Set<Integer> codes) {
        List<Integer> key = new ArrayList<>(codes);
        Collections.sort(key);
        return List.copyOf(key);
    }

    private static boolean isTop(SciValParser.SciValRecord record, int percentile) {
        Integer topPercentile = record.getTopPercentile();
        return topPercentile != null && topPercentile <= percentile;
    }

    private static boolean isInternational(SciValParser.SciValRecord record) {
        return record.getCountries() != null && record.getCountries().size() > 1;
    }

    private static boolean hasInstitution(
            SciValParser.SciValRecord record, String institutionName) {
        if(institutionName == null || institutionName.isBlank()
                || record.getInstitutions() == null) {
            return false;
        }
        for(String institution : record.getInstitutions()) {
            if(institutionName.equals(institution)) return true;
        }
        return false;
    }

    static String normalizeEid(String eid) {
        return eid == null
                ? ""
                : eid.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}
