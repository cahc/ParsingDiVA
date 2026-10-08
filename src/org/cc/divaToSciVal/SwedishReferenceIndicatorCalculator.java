package org.cc.divaToSciVal;

import cc.analysis.scival.SciValParser;
import org.cc.divaToSciVal.SciValDocumentTypePolicy.Group;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;

/**
 * Swedish non-UMU expectations from the union of exact ASJC partition and topic
 * cluster, restricted to document group. Small sets broaden to group and then
 * Swedish benchmarks; every eligible focal record receives finite expectations.
 */
public final class SwedishReferenceIndicatorCalculator {
    public static final int MIN_CITATION_REFERENCE_SET_SIZE = 25;
    public static final int MIN_INTERNATIONAL_REFERENCE_SET_SIZE = 20;
    public static final int INTERNATIONAL_YEAR_RADIUS = 1;

    // Explicit comparison modes keep the pre-check's historical CURRENT scenario stable.
    enum ReferencePolicy { LEGACY_MIXED, EXCLUSIONS_ONLY, GROUPED }

    private final ReferencePolicy policy;
    private final List<SciValParser.SciValRecord> benchmarkRecords;
    private final Map<Integer, RoaringBitmap> topicClusterIndex = new HashMap<>();
    private final Map<List<Integer>, RoaringBitmap> asjcPartitionIndex = new HashMap<>();
    private final Map<Group, RoaringBitmap> groupIndex = new EnumMap<>(Group.class);
    private final Map<Group, PopulationCounts> groupCounts = new EnumMap<>(Group.class);
    private final PopulationCounts swedishCounts = new PopulationCounts();

    public SwedishReferenceIndicatorCalculator(
            Collection<SciValParser.SciValRecord> allSwedishRecords,
            Collection<String> focalEids, String focalInstitutionName) {
        this(allSwedishRecords, focalEids, focalInstitutionName, ReferencePolicy.GROUPED);
    }

    SwedishReferenceIndicatorCalculator(
            Collection<SciValParser.SciValRecord> allSwedishRecords,
            Collection<String> focalEids, String focalInstitutionName, ReferencePolicy policy) {
        this.policy = Objects.requireNonNull(policy);
        if (allSwedishRecords == null || allSwedishRecords.isEmpty()) {
            throw new IllegalArgumentException("Swedish SciVal records must not be empty");
        }
        Set<String> normalizedFocalEids = new HashSet<>();
        if (focalEids != null) {
            for (String eid : focalEids) {
                String normalized = normalizeEid(eid);
                if (!normalized.isEmpty()) normalizedFocalEids.add(normalized);
            }
        }
        Map<String, SciValParser.SciValRecord> unique = new LinkedHashMap<>();
        for (SciValParser.SciValRecord record : allSwedishRecords) {
            if (record == null || excluded(record.getScivalDocType())) continue;
            String eid = normalizeEid(record.getEID());
            if (eid.isEmpty() || normalizedFocalEids.contains(eid)) continue;
            if (hasInstitution(record, focalInstitutionName)) continue;
            unique.putIfAbsent(eid, record);
        }
        benchmarkRecords = new ArrayList<>(unique.values());
        if (benchmarkRecords.isEmpty()) {
            throw new IllegalArgumentException("No eligible Swedish non-focal-organization benchmark records remain");
        }
        for (int index = 0; index < benchmarkRecords.size(); index++) {
            SciValParser.SciValRecord record = benchmarkRecords.get(index);
            Integer cluster = record.getTopicCluster();
            if (cluster != null && cluster != -99) {
                topicClusterIndex.computeIfAbsent(cluster, ignored -> new RoaringBitmap()).add(index);
            }
            if (hasValidAsjc(record.getASJC())) {
                asjcPartitionIndex.computeIfAbsent(asjcPartitionKey(record.getASJC()),
                        ignored -> new RoaringBitmap()).add(index);
            }
            swedishCounts.add(record);
            Group group = SciValDocumentTypePolicy.group(record.getScivalDocType());
            if (group != null) {
                groupIndex.computeIfAbsent(group, ignored -> new RoaringBitmap()).add(index);
                groupCounts.computeIfAbsent(group, ignored -> new PopulationCounts()).add(record);
            }
        }
    }

    public Map<String, ReferenceIndicators> calculateAll(Collection<SciValParser.SciValRecord> focalRecords) {
        List<SciValParser.SciValRecord> sorted = new ArrayList<>();
        if (focalRecords != null) {
            for (SciValParser.SciValRecord record : focalRecords) {
                if (record != null && !normalizeEid(record.getEID()).isEmpty()
                        && !excluded(record.getScivalDocType())) sorted.add(record);
            }
        }
        sorted.sort(Comparator.comparing(record -> normalizeEid(record.getEID())));
        Map<String, ReferenceIndicators> result = new LinkedHashMap<>();
        for (SciValParser.SciValRecord record : sorted) {
            result.putIfAbsent(normalizeEid(record.getEID()), calculate(record));
        }
        return result;
    }

    public ReferenceIndicators calculate(SciValParser.SciValRecord focalRecord) {
        if (focalRecord == null) throw new IllegalArgumentException("Focal record must not be null");
        if (excluded(focalRecord.getScivalDocType())) {
            throw new IllegalArgumentException("Excluded focal document type: " + focalRecord.getScivalDocType());
        }
        Group group = SciValDocumentTypePolicy.group(focalRecord.getScivalDocType());
        RoaringBitmap referenceSet = new RoaringBitmap();
        // Unknown groups use generic references directly, even when subject metadata exists.
        if (policy != ReferencePolicy.GROUPED || group != null) {
            Integer cluster = focalRecord.getTopicCluster();
            if (cluster != null && cluster != -99) {
                referenceSet.or(topicClusterIndex.getOrDefault(cluster, new RoaringBitmap()));
            }
            if (hasValidAsjc(focalRecord.getASJC())) {
                referenceSet.or(asjcPartitionIndex.getOrDefault(
                        asjcPartitionKey(focalRecord.getASJC()), new RoaringBitmap()));
            }
            if (policy == ReferencePolicy.GROUPED) {
                referenceSet.and(groupIndex.getOrDefault(group, new RoaringBitmap()));
            }
        }

        int top10 = 0, top50 = 0, windowSize = 0, international = 0;
        Integer focalYear = focalRecord.getYear();
        for (int index : referenceSet) {
            SciValParser.SciValRecord record = benchmarkRecords.get(index);
            if (isTop(record, 10)) top10++;
            if (isTop(record, 50)) top50++;
            if (inWindow(record.getYear(), focalYear)) {
                windowSize++;
                if (isInternational(record)) international++;
            }
        }
        int subjectSize = referenceSet.getCardinality();
        boolean citationFallback = subjectSize < MIN_CITATION_REFERENCE_SET_SIZE;
        boolean internationalFallback = windowSize < MIN_INTERNATIONAL_REFERENCE_SET_SIZE;

        int citationPopulationSize = subjectSize;
        ReferenceScope citationScope = ReferenceScope.SUBJECT_REFERENCE;
        if (citationFallback) {
            PopulationCounts chosen = swedishCounts;
            citationScope = ReferenceScope.SWEDISH_FALLBACK;
            PopulationCounts grouped = groupCounts.get(group);
            if (policy == ReferencePolicy.GROUPED && grouped != null
                    && grouped.size >= MIN_CITATION_REFERENCE_SET_SIZE) {
                chosen = grouped;
                citationScope = ReferenceScope.GROUP_FALLBACK;
            }
            top10 = chosen.top10;
            top50 = chosen.top50;
            citationPopulationSize = chosen.size;
        }

        WindowCounts internationalPopulation = new WindowCounts(windowSize, international);
        ReferenceScope internationalScope = ReferenceScope.SUBJECT_REFERENCE;
        if (internationalFallback) {
            WindowCounts grouped = groupWindow(group, focalYear);
            WindowCounts generic = swedishCounts.window(focalYear);
            if (policy == ReferencePolicy.GROUPED && grouped.size() >= MIN_INTERNATIONAL_REFERENCE_SET_SIZE) {
                internationalPopulation = grouped;
                internationalScope = ReferenceScope.GROUP_YEAR_FALLBACK;
            } else if (generic.size() >= (policy == ReferencePolicy.GROUPED
                    ? MIN_INTERNATIONAL_REFERENCE_SET_SIZE : 1)) {
                internationalPopulation = generic;
                internationalScope = ReferenceScope.SWEDISH_YEAR_FALLBACK;
            } else {
                internationalPopulation = new WindowCounts(swedishCounts.size, swedishCounts.international);
                internationalScope = ReferenceScope.SWEDISH_ALL_YEARS_FALLBACK;
            }
        }

        return new ReferenceIndicators(isTop(focalRecord, 10) ? 1 : 0,
                isTop(focalRecord, 50) ? 1 : 0, isInternational(focalRecord) ? 1 : 0,
                (double) top10 / citationPopulationSize, (double) top50 / citationPopulationSize,
                (double) internationalPopulation.international() / internationalPopulation.size(),
                subjectSize, windowSize, citationFallback, internationalFallback,
                group == null ? "UNKNOWN" : group.name(), citationScope, internationalScope,
                citationPopulationSize, internationalPopulation.size());
    }

    int groupPopulationSize(Group group) {
        PopulationCounts counts = groupCounts.get(group);
        return counts == null ? 0 : counts.size;
    }

    int groupInternationalPopulationSize(Group group, Integer year) {
        return groupWindow(group, year).size();
    }

    private WindowCounts groupWindow(Group group, Integer year) {
        PopulationCounts counts = groupCounts.get(group);
        return counts == null ? new WindowCounts(0, 0) : counts.window(year);
    }

    private boolean excluded(String type) {
        return policy == ReferencePolicy.LEGACY_MIXED
                ? SciValDocumentTypePolicy.legacyExcluded(type) : SciValDocumentTypePolicy.excluded(type);
    }

    private record WindowCounts(int size, int international) {
        WindowCounts plus(WindowCounts other) {
            return new WindowCounts(size + other.size, international + other.international);
        }
    }

    private static final class PopulationCounts {
        int size, top10, top50, international;
        final Map<Integer, WindowCounts> byYear = new HashMap<>();

        void add(SciValParser.SciValRecord record) {
            size++;
            if (isTop(record, 10)) top10++;
            if (isTop(record, 50)) top50++;
            int isInternational = isInternational(record) ? 1 : 0;
            international += isInternational;
            if (record.getYear() != null && record.getYear() > 0) {
                byYear.merge(record.getYear(), new WindowCounts(1, isInternational), WindowCounts::plus);
            }
        }

        WindowCounts window(Integer year) {
            WindowCounts counts = new WindowCounts(0, 0);
            if (year != null && year > 0) {
                for (long candidate = (long) year - INTERNATIONAL_YEAR_RADIUS;
                     candidate <= (long) year + INTERNATIONAL_YEAR_RADIUS; candidate++) {
                    if (candidate > 0 && candidate <= Integer.MAX_VALUE) {
                        counts = counts.plus(byYear.getOrDefault((int) candidate, new WindowCounts(0, 0)));
                    }
                }
            }
            return counts;
        }
    }

    private static boolean inWindow(Integer year, Integer focalYear) {
        return year != null && focalYear != null && year > 0 && focalYear > 0
                && Math.abs((long) year - focalYear) <= INTERNATIONAL_YEAR_RADIUS;
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
        Integer value = record.getTopPercentile();
        return value != null && value <= percentile;
    }

    private static boolean isInternational(SciValParser.SciValRecord record) {
        return record.getCountries() != null && record.getCountries().size() > 1;
    }

    private static boolean hasInstitution(SciValParser.SciValRecord record, String institution) {
        return institution != null && !institution.isBlank() && record.getInstitutions() != null
                && record.getInstitutions().contains(institution);
    }

    static String normalizeEid(String eid) {
        return eid == null ? "" : eid.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}
