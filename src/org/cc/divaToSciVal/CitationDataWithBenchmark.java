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
import static cc.analysis.scival.SciValSampleBenchmarkRecords.intersectAllBitSets;
import static org.cc.divaToSciVal.MatchDiVAToSciVal.validateAfids;

public class CitationDataWithBenchmark {


    /*

    For focal publication i, we are essentially constructing a comparison set conditional on two different notions of subject similarity: citation-cluster membership and the exact combination of journal classifications. Then you remove publications belonging to the focal organizational unit. So

    This should then be used as a comparison to the observed Top 10% citation indicator for ad-hoc groups of UMU records, i.e.,

    p_i = P(Top10 = 1 | subject context of i, excluding external organizations )

    Then we can have PP_top10 = (sum_i Y_i) / N  and compare with sum_i(p_i) /N = E(PP_top10)

    Y_i is a binary variable that is 1 if the record is in top 10%, 0 otherwise

    The goul is to have a descriptive indicator, that can be described like like this:

     “The reference-set Top-10 value is the mean Top-10 proportion among the subject-specific reference sets corresponding to the focal publications.”

     Then the audience can simply see, e.g.,:
     Focal publications: 12.2%
     Comparable reference publications: 13.5%

     */


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

        ListIterator<SciValParser.SciValRecord> sciValRecordListIterator = potentialBenchMarkRecords.listIterator();
        while(sciValRecordListIterator.hasNext()) {

            SciValParser.SciValRecord record = sciValRecordListIterator.next();
            if(UMU_EIDs.contains(record.getEID())) {

                umuRecords.put(record.getEID(),record);
                sciValRecordListIterator.remove();
            }

        }

        if((umuRecords.size() + potentialBenchMarkRecords.size()) !=  initialTotalRecords) {
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

        Index Topic Clusters and AJSC journal codes for non-UMU records.

         */

        HashMap<Integer, RoaringBitmap> topicClusterToBitmapMap = new HashMap<>();
        HashMap<Integer, RoaringBitmap> ASJCToBitmapMap = new HashMap<>();
        HashMap<Integer, RoaringBitmap> singletonASJCToBitmapMap = new HashMap<>();

        for(int i=0; i<potentialBenchMarkRecords.size(); i++) {

            SciValParser.SciValRecord record = potentialBenchMarkRecords.get(i); //this is a non UMU RECORD
            if(UMU_EIDs.contains(record.getEID())) continue; //should never be true, see above.

            Integer TopicCluster = record.getTopicCluster();
            Set<Integer> ASJC = record.getASJC();

            if(TopicCluster != null && TopicCluster != -99) {

                RoaringBitmap bs = topicClusterToBitmapMap.computeIfAbsent(TopicCluster, k -> new RoaringBitmap());
                bs.add(i);

            }


            //this is needed for partition-based category reference sets
            if(ASJC != null && !ASJC.isEmpty() && !ASJC.contains(-99)) {

                if(ASJC.size() == 1) {

                    RoaringBitmap bs = singletonASJCToBitmapMap.computeIfAbsent(ASJC.iterator().next(), k -> new RoaringBitmap());
                    bs.add(i);

                } else {


                    for (Integer ASJCID : ASJC) {
                        RoaringBitmap bs = ASJCToBitmapMap.computeIfAbsent(ASJCID, k -> new RoaringBitmap());
                        bs.add(i);
                    }

                }

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

                if(asjc.size() == 1) {

                    RoaringBitmap bs = singletonASJCToBitmapMap.getOrDefault(asjc.iterator().next(), new RoaringBitmap());
                    referenceSet.or(bs);
                    asjcSetSize = bs.getCardinality();
                } else {


                    ArrayList<RoaringBitmap> asjcToIntersect = new ArrayList<>();
                    for(Integer code : record.getASJC() ) {

                        asjcToIntersect.add( ASJCToBitmapMap.getOrDefault(code, new RoaringBitmap()) );

                    }

                    RoaringBitmap intersected = intersectAllBitSets(asjcToIntersect);
                    referenceSet.or(intersected);
                    asjcSetSize = intersected.getCardinality();
                }


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

        Example look at the reference set for a UMU record, e.g., 2-s2.0-85117788535

         */

        HashMap<String, SciValParser.SciValRecord> sciValRecordMap = new HashMap<>();
        for(SciValParser.SciValRecord sciValRecord : potentialBenchMarkRecords) {
            sciValRecordMap.put(sciValRecord.getEID(), sciValRecord);
        }

        String focal = "2-s2.0-85117788535";
        SciValSampleBenchmarkRecords.ReferenceSet referenceSet = umuEIDsToReferenceSets.get(focal);
        System.out.println("Focal publication: " + umuRecords.get(focal).getTitle() + " " + umuRecords.get(focal).getASJC() + " " + umuRecords.get(focal).getTopicCluster());
        System.out.println("Reference set size: " + referenceSet.referenceCount());
        System.out.println("Reference titles:");
        for(String EID: referenceSet.getReferenceEIDs() ) {

            System.out.println( sciValRecordMap.get(EID).getTitle() + "\t" + sciValRecordMap.get(EID).getASJC() + "\t" + sciValRecordMap.get(EID).getTopicCluster() );
        }



    } //main ends


}
