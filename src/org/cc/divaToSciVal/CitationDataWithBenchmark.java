package org.cc.divaToSciVal;

import cc.FilePathConstants;
import cc.analysis.scival.SciValParser;
import cc.analysis.scival.SciValSampleBenchmarkRecords;
import org.roaringbitmap.RoaringBitmap;
import java.io.File;
import java.io.IOException;
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
 * set because the sets are combined using a bitmap union. Conceptually, the later
 * reference-value calculation will estimate, for every focal publication:</p>
 *
 * <pre>
 * p_i = P(SciVal Top 10% = 1 |
 *         Swedish publication, non-UMU,
 *         exact ASJC partition of i OR topic cluster of i)
 * </pre>
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
 * <p>The present class only constructs the reference-set memberships. Calculation and
 * storage of the actual {@code p_i} reference values will be added in a later step.</p>
 */
public class CitationDataWithBenchmark {


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

        System.out.println("UMU records" + umuRecords.size() + " potential benchmark records: " + potentialBenchMarkRecords.size());

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

        TODO
        We must construct some fallback expected values for a focal publication that have an empty reference set (for example, rare cases when the focal publication dont have either ASJC codes or is in a Topic Cluster (or is the onlu such record in the swedish subset of SciVal)

        For internationalization, calculate one value per focal year using a centered
        +/- one-year publication window.

         */


        double fallbackTop10 = 0;
        double fallbackTop50 = 0;
        TreeMap<Integer,Integer> yearToInternationalPublications = new TreeMap<>();
        TreeMap<Integer,Integer> yearToAllPublications = new TreeMap<>();
        for(SciValParser.SciValRecord record : potentialBenchMarkRecords) {

            fallbackTop10 += record.getTopPercentile() <= 10 ? 1 :0;
            fallbackTop50 += record.getTopPercentile() <= 50 ? 1 :0;

            int year = record.getYear();
            yearToAllPublications.merge(year, 1, Integer::sum);
            if(record.getCountries().size() > 1) {
                yearToInternationalPublications.merge(year, 1, Integer::sum);
            }
        }

        fallbackTop10 /= potentialBenchMarkRecords.size();
        fallbackTop50 /= potentialBenchMarkRecords.size();

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
        System.out.println("Focal publication: " + umuRecords.get(focal).getTitle() + " " + umuRecords.get(focal).getASJC() + " " + umuRecords.get(focal).getTopicCluster());
        System.out.println("Reference set size: " + referenceSet.referenceCount());
        System.out.println("Reference titles:");
        for(String EID: referenceSet.getReferenceEIDs() ) {

            System.out.println( sciValRecordMap.get(EID).getTitle() + "\t" + sciValRecordMap.get(EID).getASJC() + "\t" + sciValRecordMap.get(EID).getTopicCluster() );
        }



        /*

        TODO:

        Now we are ready to calculate expected top 10% and expected top 50%, and also expected internationalization for each "UMU" record

        We shall print someting like:

        FOCAL_EID   OBSERVED_TOP10  OBSERVED_TOP50 OBSERVED_IS_INTERNATIONAL    EXPECTED_TOP10  EXPECTED_TOP50  EXPECTED_INTERNATIONAL
        ...         binary          binary          binary                      double (p_i)    double (p_i)    double_pi

         SUCH DATA CAN THEN BE READILY USED TO AVERAGE OVER DOWNSTREAM TO GET INDICATORS AND EXPECTED VALUES FOR THE INDICATORS
         */


    } //main ends


}
