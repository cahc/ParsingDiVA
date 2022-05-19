package org.cc.applikationer;

import org.cc.NorskaModellen.*;
import org.cc.diva.Author;
import org.cc.diva.CreateDivaTable;
import org.cc.diva.DivaHelpFunctions;
import org.cc.diva.Post;
import org.cc.misc.SaveToExcel;
import org.cc.misc.Thesaurus;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DivaPortalTest {

    public static void main(String[] arg) throws IOException {

        if(arg.length != 3) {System.out.println("Supply data from diva portal.csv, norwegian list.xlsx, thesaurus.xlsx"); System.exit(0); }

        List<NorskSerie> listaMedSerier = ReadNorwegianLists.parseSeries(new File(arg[1]));
        List<NorskFörlag> listaMedFörlag = ReadNorwegianLists.parseFörlag(new File(arg[1]));
        System.out.println("Antal serier i auktoritetsregister: " + listaMedSerier.size());
        System.out.println("Antal förlag i auktoritetsregister: " + listaMedFörlag.size());

        Thesaurus standardiseringsListor = new Thesaurus(new File(arg[2]));




        CreateDivaTable divaTable = new CreateDivaTable(new File(arg[0]));
        divaTable.parse();
        System.out.println("Antal poster i rådata: " + divaTable.nrRows() );

        //för att filtrera på beaktade publikationstyper/innehållstyp och status
        ConsideredPublications sfPubInclusion = new SFPubInclusion();

        List<Post> postList = new ArrayList<>(10000);

        for(int i = 0; i< divaTable.nrRows(); i++) {

            Post post = new Post(divaTable.getRowInTable(i));
            postList.add(post);

        }

        System.out.println("Poster skapade: " + postList.size());

        //TODO howto handle mappings from controlled affiliations in diva portal..

        BufferedWriter writer = new BufferedWriter(  new OutputStreamWriter(  new FileOutputStream( new File("orgz.txt")), StandardCharsets.UTF_8) );


        for(int i=0; i<postList.size(); i++) {
            Post post = postList.get(i);

            List<Author> authorList = post.getAuthorList();

            for (Author a : authorList) {

                if(!a.getHasControlledAddresses()) {

                   // System.out.println(post.getPID() +" : " + a.getAuthorName() + "  EXTERNAL TO PROVIDER" );

                } else  {


                    for(String rawAffil : a.getControlledAddresses()) {


                        writer.write( post.getPID() +"\t" + DivaHelpFunctions.orgNamesHeightLevel(rawAffil) + "\t" + DivaHelpFunctions.orgNumberHighestLevel(rawAffil) );
                        writer.newLine();
                    }


                //    System.out.println(post.getPID() +" : " + a.getAuthorName() + " " + a.getControlledAddresses() );

                }

            }

        //System.out.println();

        }

        writer.flush();
        writer.close();


        //norwegian stuff below
        //NORSK MODELL




        System.out.println("Matchar mot norska listan..");

        for(Post p : postList) {


            //OBS hög tröskel, 0.94

            p.setStatusInModel( sfPubInclusion.consideredPub(p) ); // ska publikationen matchas mot norska listan, se StatusInMdelConstants
            NorwegianMatchInfo matchInfo = NorskNivå.getNorwegianLevel(listaMedSerier,listaMedFörlag,p,0.94,standardiseringsListor,true); //matcha eventuellt mot norska listan samt uppdatera StatusInModel
            p.setNorskNivå( matchInfo ); // uppdatera posten med information om matchning mot norska listan

            //Viktning.DefaultWeightning(p);

            Viktning.SFWeighting(p);


        }


        SaveToExcel saveToExcel = new SaveToExcel();
        saveToExcel.saveNorwegianMatchingInfo(postList);



    }

}
