package org.cc.applikationer;

import org.cc.NorskaModellen.*;
import org.cc.diva.*;
import org.cc.misc.*;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * Created by crco0001 on 6/20/2017.
 */
public class SamFakModel {

    /*

    Samhällsvetenskapliga fakultetens model

    Se dokumentation: "Norska modellen vid samFak, Datum 2017-09-01

     */

    public static void main(String[] arg) throws IOException, XMLStreamException, ParseException {


        if(arg.length != 5) { System.out.println(" java -cp ... norwegianList.xlsx thesaurusFile.xlsx divaDump.csv AffiliationMappingFile.xlsx PersonalData.xml"); System.exit(0); }


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

        //ConsideredPublications defaultPubInclusion = new DefaultPubInclusion();

        ConsideredPublications sfPubInclusion = new SFPubInclusion();

        List<Post> postList = new ArrayList<>(100);

        for(int i = 0; i< divaTable.nrRows(); i++) {


                //if(listaMedCasForskningstid.includeRawPostByDefault(divaTable, i)) {
                Post post = new Post(divaTable.getRowInTable(i));
                postList.add(post);

            }


        System.out.println("Object skapade: " + postList.size());


        /**
         *
         * Identifierar dubbletter
         *
         */




        System.out.println(DuplicationIdentifier.deDuplicate(postList) +" duplicated identified");


        /**
         *
         * Author disambiguate
         *
         */


      //  AuthorDisambiguation authorDisambiguation = new AuthorDisambiguation(); //constructor with hardcoded paths to personalData.xml and Mappings.excel

        AuthorDisambiguation authorDisambiguation = new AuthorDisambiguation(new java.io.File(arg[3]), new File(arg[4]));

        authorDisambiguation.mapAffiliationsAndDisanbigueAuthors(postList);



        System.out.println("Matchar mot norska listan..");

        for(Post p : postList) {



            p.setStatusInModel( sfPubInclusion.consideredPub(p) ); // ska publikationen matchas mot norska listan, se StatusInMdelConstants
            NorwegianMatchInfo matchInfo = NorskNivå.getNorwegianLevel(listaMedSerier,listaMedFörlag,p,0.9,standardiseringsListor); //matcha eventuellt mot norska listan samt uppdatera StatusInModel
            p.setNorskNivå( matchInfo ); // uppdatera posten med information om matchning mot norska listan


            //Viktning.DefaultWeightning(p);

            Viktning.SFWeighting(p);


            }



        SaveToExcel saveToExcel = new SaveToExcel();
        saveToExcel.saveNorwegianMatchingInfo(postList);

        saveToExcel = new SaveToExcel();
        saveToExcel.saveDisambiguatedAuthorFractions(postList,false);

       saveToExcel = new SaveToExcel();
        saveToExcel.saveDisambiguatedAuthorFractions(postList,true);

        System.out.println("Resultat sparat i tre Excel-filer: Fractions och NorskMatchning..");

        }




    }
