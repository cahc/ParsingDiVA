package org.cc.divaToSciVal;

import cc.FilePathConstants;
import cc.analysis.scival.SciValParser;
import org.cc.diva.CreateDivaTable;
import org.cc.diva.Post;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static cc.analysis.TopicsAndTopicClusters.RecordToTopicsAndCitationIndicators.getSciValExcelFiles;
import static cc.analysis.scival.SciValParser.getSciValRecords;


public class RecordsTypesDistribution {

    public static void main(String[] args) throws IOException {


        String csvFile = "C:\\opt\\bibliometric_gui_static_data_files\\Raw_DiVA_export_20260930_102140.csv";
        CreateDivaTable divaTable = new CreateDivaTable(new File(csvFile));
        divaTable.parse();
        List<Post> posts = new ArrayList<>(divaTable.nrRows());
        for (int i = 0; i < divaTable.nrRows(); i++) {
            posts.add(new Post(divaTable.getRowInTable(i)));
        }
        System.out.println("Total DiVA records parsed: " + posts.size());

        HashSet<String> divaTypes = new HashSet<>();

        for(Post post : posts) {

            divaTypes.add(post.getDivaPublicationType());

        }


        System.out.println("Total DIVA types parsed: " + divaTypes.size());
        for(String s: divaTypes) System.out.println(s);


        //The diva types (post.getDivaPublicationType())

        //Artikel i tidskrift
        //Artikel, forskningsöversikt

        //can only match against SciVal type (record.getScivalDocType())

       // "Article in Press"
       // "Article"
       // "Letter"
       // "Note"
       // "Data Paper"
       // "Review"
       // "Short Survey"
       // "Editorial"


        File[] files = getSciValExcelFiles(FilePathConstants.SCIVAL_RAW_XLSX_LATEST);
        List<SciValParser.SciValRecord> sciValRecords = new ArrayList<>(10000);
        for (File file : files) {
            List<SciValParser.SciValRecord> parsed = getSciValRecords(file.getAbsolutePath(), Collections.emptySet());
            sciValRecords.addAll(parsed);
        }
        System.out.println("Total SciVal records parsed: " + sciValRecords.size());


        HashMap<String,HashMap<String,Integer>> scivalSourceTypesAndDocTypes = new HashMap<>(10000);

        for(SciValParser.SciValRecord record : sciValRecords) {

            String sourcetype = record.getSciValSourceType();
            String recordType = record.getScivalDocType();

            HashMap<String,Integer> map =  scivalSourceTypesAndDocTypes.get(sourcetype);
            if(map == null) {

                map = new HashMap<>();
                map.put(recordType, 1);
                scivalSourceTypesAndDocTypes.put(sourcetype, map);
            } else {

                map.merge(recordType, 1, Integer::sum);


            }


        }
        System.out.println();
        HashSet<String> allTypes = new HashSet<>();
        scivalSourceTypesAndDocTypes.forEach((k,v)->{

            allTypes.addAll( v.keySet() );

        });

        System.out.println("Total SciVal types parsed: " + allTypes.size());
        for(String s: allTypes) System.out.println(s);

        System.out.println();
        System.out.println("SciVal Source types parsed: " + scivalSourceTypesAndDocTypes.size());
        for(String s: scivalSourceTypesAndDocTypes.keySet()) System.out.println(s);


        for(Map.Entry<String,HashMap<String,Integer>> entry: scivalSourceTypesAndDocTypes.entrySet()) {

            System.out.println("## " + entry.getKey());
                for(Map.Entry<String,Integer> entry2: entry.getValue().entrySet()) {
                    System.out.println(entry2.getKey() + "\t" + entry2.getValue());
                }


        }

    }

}
