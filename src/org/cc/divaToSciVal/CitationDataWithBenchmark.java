package org.cc.divaToSciVal;

import cc.FilePathConstants;
import cc.analysis.scival.SciValParser;
import cc.analysis.scival.SciValSampleBenchmarkRecords;
import org.roaringbitmap.RoaringBitmap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

import static cc.analysis.TopicsAndTopicClusters.RecordToTopicsAndCitationIndicators.getSciValExcelFiles;
import static cc.analysis.scival.SciValParser.getSciValRecords;
import static org.cc.divaToSciVal.MatchDiVAToSciVal.validateAfids;

/**
 * Constructs publication-specific Swedish reference sets for focal publications
 * from Umeå University (UMU).
 *
 * <p>The restriction to Sweden is deliberate. This class does not replace SciVal's
 * global field normalization or reconstruct which publications belong to the Top 10%.
 * The publication-level SciVal Top-10 indicator is already normalized by subject and
 * publication year with respect to the worldwide database. The purpose here is instead
 * to create an alternative Swedish baseline for that already normalized binary outcome.</p>
 *
 * <p>Publications associated with UMU are removed from the Swedish benchmark pool.
 * For each focal publication {@code i}, subject-similar benchmark publications are
 * identified by the inclusive union of:</p>
 *
 * <ol>
 *     <li>the Rons-style ASJC partition cell: publications whose journals have exactly
 *     the same complete combination of ASJC codes as the focal publication; and</li>
 *     <li>the SciVal topic cluster to which the focal publication belongs.</li>
 * </ol>
 *
 * <p>A publication satisfying both criteria occurs only once in the resulting reference
 * set because the sets are combined using a bitmap union. Conceptually, the
 * reference-value calculation will estimate, for every focal publication:</p>
 *
 * <pre>
 * p_i = P(SciVal Top 10% = 1 |
 *         Swedish publication, non-UMU,
 *         exact ASJC partition of i OR topic cluster of i)
 * </pre>
 *
 * <p>Small reference sets are treated as too sparse for stable publication-specific
 * expectations. The Swedish fallback is therefore used when the citation reference
 * set contains fewer than 25 publications, or when the +/- one-year
 * internationalization reference set contains fewer than 20 publications.</p>
 *
 * We also introduce a notion of Top 50%.
 *
 *
 * Additionally, we are interested in the share of papers with international
 * collaboration, defined as publications involving at least two countries. This must
 * be treated somewhat differently because it is not year normalized and generally
 * changes over time. We therefore use the same notion of subject-similar records, but
 * restrict the expected internationalization rate to a centered +/- one-year window
 * around the focal publication year.
 *
 *
 *
 * <p>For an ad-hoc group of {@code N} focal publications, the observed value
 * {@code sum(Y_i) / N}, where {@code Y_i} is the focal publication's SciVal Top-10
 * indicator, can then be compared with the Swedish reference-set value
 * {@code sum(p_i) / N}. The latter is the mean of the publication-specific reference-set
 * proportions, not the proportion calculated from one pooled set of unique reference
 * publications.</p>
 *
 * <p>The class constructs the reference-set memberships and calculates the observed
 * and expected values for each focal publication. The per-publication output can then
 * be aggregated for arbitrary groups of UMU publications.</p>
 */
public class CitationDataWithBenchmark {

    private static final int MIN_CITATION_REFERENCE_SET_SIZE = 25;
    private static final int MIN_INTERNATIONAL_REFERENCE_SET_SIZE = 20;


    private static List<Integer> asjcPartitionKey(Set<Integer> codes) {
        List<Integer> key = new ArrayList<>(codes);
        Collections.sort(key);
        return Collections.unmodifiableList(key);
    }


    public static void main(String[] args) throws SQLException, IOException {


        /*


        Identify focal publications, i.e., publications from UMU

         */

        Connection conn = DriverManager.getConnection("jdbc:duckdb:");

        PreparedStatement ps = conn.prepareStatement("SELECT * FROM read_parquet('divaToScival.parquet')");

        ResultSet executedQuery = ps.executeQuery();

        Set<String> UMU_EIDs = new HashSet<>();
        while (executedQuery.next()) {
            Integer id = executedQuery.getInt("PID");
            String EID = executedQuery.getString("EID");
            String STATUS = executedQuery.getString("STATUS");
            if("NO_MATCH".equals(STATUS) || EID == null || EID.isBlank() ) continue; //but we keep AMBIGUOUS ETC.
            System.out.println(id + " " + EID + " " +  STATUS);
            UMU_EIDs.add(EID);
        }

        executedQuery.close();
        ps.close();
        conn.close();



        /*

        Read in all publications from Sweden

         */


        Set<String> ignoreDocTypes = new HashSet<>();
        // Editorial, Note, Letter, and Erratum are useful candidates, especially for
        // generic DiVA titles. Only types outside the intended publication corpus are
        // omitted before identifier matching and retrieval.
        Collections.addAll(ignoreDocTypes, "Retracted", "Abstract Report");

        File[] files = getSciValExcelFiles(FilePathConstants.SCIVAL_RAW_XLSX_LATEST);

        List<SciValParser.SciValRecord> potentialBenchMarkRecords = new ArrayList<>(10000);
        for (File file : files) {
            List<SciValParser.SciValRecord> parsed = getSciValRecords(file.getAbsolutePath(), ignoreDocTypes);
            potentialBenchMarkRecords.addAll(parsed);
            validateAfids(parsed);
        }

        int initialTotalRecords = potentialBenchMarkRecords.size();
        System.out.println("Unique UMU EIDs: " + UMU_EIDs.size());


        /*

        Separate UMU from the REST

         */

        Map<String,SciValParser.SciValRecord> umuRecords = new HashMap<>(10_000);
        Map<String, SciValParser.SciValRecord> ignored_umuRecords = new HashMap<>();
        ListIterator<SciValParser.SciValRecord> sciValRecordListIterator = potentialBenchMarkRecords.listIterator();
        while(sciValRecordListIterator.hasNext()) {

            SciValParser.SciValRecord record = sciValRecordListIterator.next();
            if(UMU_EIDs.contains(record.getEID()) ) {

                umuRecords.put(record.getEID(),record);
                sciValRecordListIterator.remove();
            } else if(record.getInstitutions().contains("Umeå University")) {
                ignored_umuRecords.put(record.getEID(),record);
                sciValRecordListIterator.remove();
            }

        }

        if((umuRecords.size() + ignored_umuRecords.size() + potentialBenchMarkRecords.size() ) !=  initialTotalRecords) {
            System.out.println("Record missmatch count!"); throw  new RuntimeException("Record missmatch count!");
        }


        if(umuRecords.size() != UMU_EIDs.size()) {

            System.out.println("Record missmatch count, sources from different timestamps? UMU RECORDS=" + umuRecords.size() + " INITIAL UMU RECORDS=" + UMU_EIDs.size()); throw  new RuntimeException("Record missmatch count!");
        }

        System.out.println("UMU records " + umuRecords.size() + " potential benchmark records: " + potentialBenchMarkRecords.size());

        /*



        Lets now for each focal record identify a comparison group based on subject similarity.
        Subject similarity is based on two notions:

         (1)
         Journal based partition-based normalization, i.e., reference set from intersection of journal categories
         see: https://www.sciencedirect.com/science/article/pii/S175115771100085X

         For a highly specialized research record, the appropriate reference domain and expected citation rate for publications in an intersection may be more accurately determined when based only on the journals in that intersection. This can be implemented in a straightforward way in practice, by calculating expected citation rates not per original subject category, but per cell of the partition formed by the fixed subject categories and their intersections. The set X of all publication sources is divided in a set of non-empty subsets of X such that every publication source in X is in exactly one of these subsets, being the subset that contains all publication sources classified in exactly the same combination of subject categories. These non-overlapping and non-empty subsets will in this paper be called “cells” of the partition of X
         So: A journal classified in a combination of multiple subject categories --> All journals in the cell containing all journals classified in exactly the same combination of subject categories.
         And: A journal classified in one subject category only --> All journals in the cell containing all journals classified in that subject category only.


        (2)

        Algorithmically constructed subject classification. Each record in the database is part of one and only one 'topic cluster', the partition is based on clustering a graph where the nodes are the documentes and the edges are direct citations.
        see: https://www.cwts.nl/pdf/CWTS-WP-2012-006.pdf


         */




        /*

        Index Topic Clusters and exact ASJC journal-code combinations for non-UMU records.

         */

        HashMap<Integer, RoaringBitmap> topicClusterToBitmapMap = new HashMap<>();
        HashMap<List<Integer>, RoaringBitmap> asjcPartitionToBitmapMap = new HashMap<>();

        for(int i=0; i<potentialBenchMarkRecords.size(); i++) {

            SciValParser.SciValRecord record = potentialBenchMarkRecords.get(i); //this is a non UMU RECORD
            if(UMU_EIDs.contains(record.getEID())) continue; //should never be true, see above.

            Integer TopicCluster = record.getTopicCluster();
            Set<Integer> ASJC = record.getASJC();

            if(TopicCluster != null && TopicCluster != -99) {

                RoaringBitmap bs = topicClusterToBitmapMap.computeIfAbsent(TopicCluster, k -> new RoaringBitmap());
                bs.add(i);

            }


            // A partition cell consists of records with exactly the same complete
            // combination of ASJC codes, not merely records sharing all focal codes.
            if(ASJC != null && !ASJC.isEmpty() && !ASJC.contains(-99)) {
                List<Integer> partitionKey = asjcPartitionKey(ASJC);
                RoaringBitmap bs = asjcPartitionToBitmapMap.computeIfAbsent(
                        partitionKey, k -> new RoaringBitmap());
                bs.add(i);
            }


        } //indexing of TC and ASJC completed.


        /*

        Define reference sets for each UMU record

         */

        HashMap<String, SciValSampleBenchmarkRecords.ReferenceSet> umuEIDsToReferenceSets = new HashMap<>();
        for(SciValParser.SciValRecord record : umuRecords.values()) {

            RoaringBitmap referenceSet = new RoaringBitmap();


            Integer TopicCluster = record.getTopicCluster();
            RoaringBitmap roaringBitmapTopicCluster = topicClusterToBitmapMap.getOrDefault(TopicCluster, new RoaringBitmap());
            referenceSet.or(roaringBitmapTopicCluster);
            int topicClusterSetSize = roaringBitmapTopicCluster.getCardinality();



            int asjcSetSize = 0;
            HashSet<Integer> asjc = record.getASJC();
            if(asjc != null && !asjc.isEmpty() && !asjc.contains(-99)) {

                List<Integer> partitionKey = asjcPartitionKey(asjc);
                RoaringBitmap asjcPartition = asjcPartitionToBitmapMap.getOrDefault(
                        partitionKey, new RoaringBitmap());
                referenceSet.or(asjcPartition);
                asjcSetSize = asjcPartition.getCardinality();




            }

            /*

            Now we have combine (union) every record identified as subject similar to the focal record

             */

            SciValSampleBenchmarkRecords.ReferenceSet rs = new SciValSampleBenchmarkRecords.ReferenceSet( record.getEID() );

            for(int docId : referenceSet) {

                rs.addReferenceEID( potentialBenchMarkRecords.get(docId).getEID() );
            }


            umuEIDsToReferenceSets.put(record.getEID(), rs);

        } //for each UMU record


        /*
        Construct Swedish fallback values for focal publications whose subject-similar
        reference sets are too small. Internationalization fallbacks are specific to a
        centered +/- one-year publication window.
         */


        if(potentialBenchMarkRecords.isEmpty()) {
            throw new IllegalStateException("Cannot calculate benchmarks without Swedish non-UMU records");
        }

        double fallbackTop10 = 0;
        double fallbackTop50 = 0;
        int fallbackInternationalAllYearsCount = 0;
        TreeMap<Integer,Integer> yearToInternationalPublications = new TreeMap<>();
        TreeMap<Integer,Integer> yearToAllPublications = new TreeMap<>();
        for(SciValParser.SciValRecord record : potentialBenchMarkRecords) {

            fallbackTop10 += record.getTopPercentile() <= 10 ? 1 :0;
            fallbackTop50 += record.getTopPercentile() <= 50 ? 1 :0;

            int year = record.getYear();
            yearToAllPublications.merge(year, 1, Integer::sum);
            if(record.getCountries().size() > 1) {
                yearToInternationalPublications.merge(year, 1, Integer::sum);
                fallbackInternationalAllYearsCount++;
            }
        }

        fallbackTop10 /= potentialBenchMarkRecords.size();
        fallbackTop50 /= potentialBenchMarkRecords.size();
        double fallbackInternationalAllYears =
                (double) fallbackInternationalAllYearsCount / potentialBenchMarkRecords.size();

        TreeMap<Integer,Double> yearToInternationalizationShare = new TreeMap<>();
        for(Integer year : yearToAllPublications.keySet()) {
            int internationalPublications = 0;
            int allPublications = 0;

            for(int comparisonYear = year - 1; comparisonYear <= year + 1; comparisonYear++) {
                internationalPublications += yearToInternationalPublications.getOrDefault(comparisonYear, 0);
                allPublications += yearToAllPublications.getOrDefault(comparisonYear, 0);
            }

            if(allPublications > 0) {
                yearToInternationalizationShare.put(
                        year, (double) internationalPublications / allPublications);
            }
        }

        Set<Integer> years = yearToInternationalizationShare.keySet();



        System.out.println("Fallback top 10%: " + fallbackTop10 + " fallback top 50%: " + fallbackTop50 );
        System.out.println("Fallback internationalization rates:");
        for(Integer year : years) {

            System.out.println(year + " " +yearToInternationalizationShare.get(year));
        }




        /*

        Example look at the reference set for a UMU record, e.g., 2-s2.0-85117788535

         */

        HashMap<String, SciValParser.SciValRecord> sciValRecordMap = new HashMap<>();
        for(SciValParser.SciValRecord sciValRecord : potentialBenchMarkRecords) {
            sciValRecordMap.put(sciValRecord.getEID(), sciValRecord);
        }

        String focal = "2-s2.0-85193354598";
        SciValSampleBenchmarkRecords.ReferenceSet referenceSet = umuEIDsToReferenceSets.get(focal);
        SciValParser.SciValRecord focalRecord = umuRecords.get(focal);
        if(focalRecord != null && referenceSet != null) {
            System.out.println("Focal publication: " + focalRecord.getTitle() + " "
                    + focalRecord.getASJC() + " " + focalRecord.getTopicCluster());
            System.out.println("Reference set size: " + referenceSet.referenceCount());
            System.out.println("Reference titles:");
            for(String EID: referenceSet.getReferenceEIDs() ) {
                SciValParser.SciValRecord benchmarkRecord = sciValRecordMap.get(EID);
                if(benchmarkRecord != null) {
                    System.out.println(benchmarkRecord.getTitle() + "\t"
                            + benchmarkRecord.getASJC() + "\t"
                            + benchmarkRecord.getTopicCluster());
                }
            }
        }



        /*
        Calculate observed and expected values for every focal publication. Citation
        percentile expectations use the full subject reference set. Internationalization
        expectations use only subject references published within +/- one year.
         */

        PrintWriter pw = new PrintWriter( new File("citationDataTemporary.txt"),StandardCharsets.UTF_8);


        pw.println(
                "FOCAL_EID\tOBSERVED_TOP10\tOBSERVED_TOP50\tOBSERVED_IS_INTERNATIONAL"
                        + "\tEXPECTED_TOP10\tEXPECTED_TOP50\tEXPECTED_INTERNATIONAL"
                        + "\tREFERENCE_SET_SIZE\tINTERNATIONAL_REFERENCE_SET_SIZE"
                        + "\tUSED_CITATION_FALLBACK\tUSED_INTERNATIONAL_FALLBACK");

        List<String> focalEIDs = new ArrayList<>(umuRecords.keySet());
        Collections.sort(focalEIDs);

        for(String focalEID : focalEIDs) {
            SciValParser.SciValRecord record = umuRecords.get(focalEID);
            SciValSampleBenchmarkRecords.ReferenceSet publicationReferenceSet =
                    umuEIDsToReferenceSets.get(focalEID);

            int observedTop10 = record.getTopPercentile() <= 10 ? 1 : 0;
            int observedTop50 = record.getTopPercentile() <= 50 ? 1 : 0;
            int observedInternational = record.getCountries().size() > 1 ? 1 : 0;

            int validReferenceRecords = 0;
            int top10ReferenceRecords = 0;
            int top50ReferenceRecords = 0;
            int internationalWindowReferenceRecords = 0;
            int internationalReferenceRecords = 0;

            if(publicationReferenceSet != null) {
                for(String referenceEID : publicationReferenceSet.getReferenceEIDs()) {
                    SciValParser.SciValRecord benchmarkRecord = sciValRecordMap.get(referenceEID);
                    if(benchmarkRecord == null) continue;

                    validReferenceRecords++;
                    if(benchmarkRecord.getTopPercentile() <= 10) top10ReferenceRecords++;
                    if(benchmarkRecord.getTopPercentile() <= 50) top50ReferenceRecords++;

                    Integer focalYear = record.getYear();
                    Integer benchmarkYear = benchmarkRecord.getYear();
                    if(focalYear != null && benchmarkYear != null
                            && Math.abs(benchmarkYear - focalYear) <= 1) {
                        internationalWindowReferenceRecords++;
                        if(benchmarkRecord.getCountries().size() > 1) {
                            internationalReferenceRecords++;
                        }
                    }
                }
            }

            boolean usedCitationFallback =
                    validReferenceRecords < MIN_CITATION_REFERENCE_SET_SIZE;
            double expectedTop10 = usedCitationFallback
                    ? fallbackTop10
                    : (double) top10ReferenceRecords / validReferenceRecords;
            double expectedTop50 = usedCitationFallback
                    ? fallbackTop50
                    : (double) top50ReferenceRecords / validReferenceRecords;

            boolean usedInternationalFallback =
                    internationalWindowReferenceRecords < MIN_INTERNATIONAL_REFERENCE_SET_SIZE;
            double expectedInternational;
            if(!usedInternationalFallback) {
                expectedInternational =
                        (double) internationalReferenceRecords / internationalWindowReferenceRecords;
            } else {
                Integer focalYear = record.getYear();
                Double yearSpecificFallback = focalYear == null
                        ? null
                        : yearToInternationalizationShare.get(focalYear);

                if(yearSpecificFallback != null) {
                    expectedInternational = yearSpecificFallback;
                } else if(focalYear != null) {
                    int internationalPublications = 0;
                    int allPublications = 0;
                    for(int comparisonYear = focalYear - 1;
                        comparisonYear <= focalYear + 1;
                        comparisonYear++) {
                        internationalPublications +=
                                yearToInternationalPublications.getOrDefault(comparisonYear, 0);
                        allPublications +=
                                yearToAllPublications.getOrDefault(comparisonYear, 0);
                    }
                    expectedInternational = allPublications == 0
                            ? fallbackInternationalAllYears
                            : (double) internationalPublications / allPublications;
                } else {
                    expectedInternational = fallbackInternationalAllYears;
                }
            }

            pw.printf(Locale.ROOT,
                    "%s\t%d\t%d\t%d\t%.6f\t%.6f\t%.6f\t%d\t%d\t%b\t%b%n",
                    focalEID,
                    observedTop10,
                    observedTop50,
                    observedInternational,
                    expectedTop10,
                    expectedTop50,
                    expectedInternational,
                    validReferenceRecords,
                    internationalWindowReferenceRecords,
                    usedCitationFallback,
                    usedInternationalFallback);
        }

        pw.flush();
        pw.close();





    } //main ends


}
