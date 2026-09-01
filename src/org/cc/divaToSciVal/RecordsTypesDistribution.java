package org.cc.divaToSciVal;

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


        String csvFile = "E:\\2026\\divaToSciVal\\Raw_DiVA_export_20260819_115425.csv";
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


        File[] files = getSciValExcelFiles("C:\\opt\\SCIVALEXPORT\\SWEDEN20260217");
        List<SciValParser.SciValRecord> sciValRecords = new ArrayList<>(10000);
        for (File file : files) {
            List<SciValParser.SciValRecord> parsed = getSciValRecords(file.getAbsolutePath(), Collections.emptySet());
            sciValRecords.addAll(parsed);
        }
        System.out.println("Total SciVal records parsed: " + sciValRecords.size());

        HashSet<String> scivalTypes = new HashSet<>(10000);
        HashSet<String> scivalSourceTypes = new HashSet<>(10000);
        for(SciValParser.SciValRecord record : sciValRecords) {

            scivalTypes.add(record.getScivalDocType());
            scivalSourceTypes.add( record.getSciValSourceType() );
        }

        System.out.println("Total SciVal types parsed: " + scivalTypes.size());
        for(String s: scivalTypes) System.out.println(s);

        System.out.println();
        System.out.println("SciVal Source types parsed: " + scivalSourceTypes.size());
        for(String s: scivalSourceTypes) System.out.println(s);

    }

}
