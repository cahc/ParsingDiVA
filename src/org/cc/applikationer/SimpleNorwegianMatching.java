package org.cc.applikationer;

import org.cc.NorskaModellen.*;
import org.cc.diva.CreateDivaTable;
import org.cc.diva.Post;
import org.cc.misc.SaveToExcel;
import org.cc.misc.Thesaurus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by crco0001 on 6/20/2017.
 */
public class SimpleNorwegianMatching {


    public static void main(String[] arg) throws IOException {


        if(arg.length != 3) { System.out.println(" java -cp .. norwegianList.xlsx thesaurusFile.xlsx divaDump.csv"); System.exit(0); }


       File auktoritetsRegisterFil = new File( arg[0] );
       File thesaurusFil = new File(arg[1]);
       File divaDumpFil = new File(arg[2]);


        /**
         *
         *Läs in norska listan
         *
         **/

        List<NorskSerie> listaMedSerier = ReadNorwegianLists.parseSeries(auktoritetsRegisterFil);
        List<NorskFörlag> listaMedFörlag = ReadNorwegianLists.parseFörlag(auktoritetsRegisterFil);
        System.out.println("Antal serier i auktoritetsregister: " + listaMedSerier.size());
        System.out.println("Antal förlag i auktoritetsregister: " + listaMedFörlag.size());

        Thesaurus standardiseringsListor = new Thesaurus(thesaurusFil);

        /**
         *
         *Läs in CSVall2 dump från DiVA
         *
         **/

        CreateDivaTable divaTable = new CreateDivaTable(divaDumpFil);
        divaTable.parse();
        System.out.println("Antal poster i rådata: " + divaTable.nrRows() );


        /**


         Beaktade pubtyper


         **/

        ConsideredPublications defaultPubInclusion = new DefaultPubInclusion();

        List<Post> postList = new ArrayList<>(100);

        for(int i = 0; i< divaTable.nrRows(); i++) {


                //if(listaMedCasForskningstid.includeRawPostByDefault(divaTable, i)) {
                Post post = new Post(divaTable.getRowInTable(i));
                postList.add(post);

            }



        System.out.println("Object skapade: " + postList.size());

        System.out.println("Matchar mot norska listan..");

        for(Post p : postList) {



            p.setStatusInModel( defaultPubInclusion.consideredPub(p) ); // ska publikationen matchas mot norska listan, se StatusInMdelConstants
            NorwegianMatchInfo matchInfo = NorskNivå.getNorwegianLevel(listaMedSerier,listaMedFörlag,p,0.9,standardiseringsListor); //matcha eventuellt mot norska listan samt uppdatera StatusInModel
            p.setNorskNivå( matchInfo ); // uppdatera posten med information om matchning mot norska listan


            }

        System.out.println("OK.. now saving to Excel..");

        SaveToExcel saveToExcel = new SaveToExcel();
        saveToExcel.saveNorwegianMatchingInfo(postList);


        }




    }
