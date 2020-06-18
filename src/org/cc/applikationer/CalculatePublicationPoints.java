package org.cc.applikationer;

import org.cc.NorskaModellen.*;
import org.cc.diva.*;
import org.cc.misc.CmdParser;
import org.cc.misc.CAStoTime;
import org.cc.misc.SaveToExcel;
import org.cc.misc.Thesaurus;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * Created by crco0001 on 7/5/2016.
 */
public class CalculatePublicationPoints {

    public static void main(String[] args) throws IOException, ParseException, XMLStreamException {

        CmdParser cmdParser = new CmdParser(args);

        /**
         *
         *Filer innehållandes 1) Norska listan, 2) CAS-Forskningstid 3) diva dump 4) thesaurus för standardisering
         *
         **/

        File auktoritetsRegisterFil = new File(cmdParser.getNorskListaFileName());
        File divaDumpFil = new File(cmdParser.getDivaFileName());
        File casFil =  new File(cmdParser.getCasFileName());
        File thesaurusFil = new File(cmdParser.getThesaurusFileName());

        String model = cmdParser.getModelType();

        if( !(model.equals("HF") || model.equals("LH"))  ) {System.out.println("The specified model is not implemented"); System.exit(1); }


        /**
         *
         *Läs in norska listan
         *
         **/

        List<NorskSerie> listaMedSerier = ReadNorwegianLists.parseSeries(auktoritetsRegisterFil);
        List<NorskFörlag> listaMedFörlag = ReadNorwegianLists.parseFörlag(auktoritetsRegisterFil);
        System.out.println("Antal serier i auktoritetsregister: " + listaMedSerier.size());
        System.out.println("Antal förlag i auktoritetsregister: " + listaMedFörlag.size());

        /**
         *
         *Läs in tab-avgränsad fil med CAS och forskningstid
         *
         **/

        CAStoTime listaMedCasForskningstid = new CAStoTime(casFil);
        System.out.println("Antal inlästa CAS med forskningstid: " +  listaMedCasForskningstid.antalCas() );

        /**
         *
         *Läs in CSVall2 dump från DiVA
         *
         **/

        CreateDivaTable divaTable = new CreateDivaTable(divaDumpFil);
        divaTable.parse();
        System.out.println("Antal poster i rådata: " + divaTable.nrRows() );


        /**
         *
         *Skapa thesaurus
         *
         **/

        Thesaurus standardiseringsListor = new Thesaurus(thesaurusFil);

        System.out.println("Antal Serie-mappningar i thesaurus " + standardiseringsListor.antalSerieMappningar());
        System.out.println("Antal förlags-mappningar i thesaurus " + standardiseringsListor.antalFörlagsMappningar());


        /**
         *
         *Skapa potentiellt ett Post-object från varje rad i DivaTable och spara i postList
         *
         **/

        List<Post> reducedPostList = new ArrayList<>(100);

        for(int i = 0; i< divaTable.nrRows(); i++) {


            //TODO remove
            //check contributor field
            listaMedCasForskningstid.ContributorFieldContainsCASDEBUGFUNCTION(divaTable,i);


            //Innehåller posten minst ett CAS som specificerats i casForskningstid? om Nej, skapa ej posten.
            if( listaMedCasForskningstid.includeRawPostBasedOnCas(divaTable,i) ) {

            //if(listaMedCasForskningstid.includeRawPostByDefault(divaTable, i)) {
                Post post = new Post(divaTable.getRowInTable(i));

                //Markera den eller de författare i posten som specificerats i casForskningstid
                listaMedCasForskningstid.markAuthorsForInclusion(post);

                //Markera alla författare
                //listaMedCasForskningstid.markAllAuthorsForInclusionByDefault(post);
                reducedPostList.add(post);

            }
        }

        System.out.println("Antal poster efter filterering på CAS: " + reducedPostList.size());
        System.out.println("Antal CAS utan koppling till någon publikation: " + listaMedCasForskningstid.getCasWithNoPublications().size() + " " + listaMedCasForskningstid.getCasWithNoPublications()) ;

        /**
         *
         * Filtrera på beaktad publikationstyp samt matcha mot norska auktoritetslistan
         *
         **/


        //DEFAULT
        // ConsideredPublications defaultPubInclusion = new DefaultPubInclusion(); // Anger vilka publikationer ska beaktas beroende på vilken variant av norska modellen som avsees

        ConsideredPublications defaultPubInclusion = null;
        if(model.equals("HF")) {
            //Humanistiska fakulteten
              defaultPubInclusion = new HFPubInclusion();
        }

        //Lärarhögskolan

        if(model.equals("LH")) {
            //Lärarhögskolan
            defaultPubInclusion = new LHPubInclusion();

        }

        System.out.println();
        System.out.println("Matchar poster mot norska listan. Notera att fuzzy-strängmatchning används ibland vilket är tidskrävande.." );

        for(Post p : reducedPostList) {


            //Threshold increased!

            p.setStatusInModel( defaultPubInclusion.consideredPub(p) ); // ska publikationen matchas mot norska listan, se StatusInMdelConstants
            NorwegianMatchInfo matchInfo = NorskNivå.getNorwegianLevel(listaMedSerier,listaMedFörlag,p,0.94,standardiseringsListor); //matcha eventuellt mot norska listan samt uppdatera StatusInModel
            p.setNorskNivå( matchInfo ); // uppdatera posten med information om matchning mot norska listan


            if(model.equals("LH")) {

                //viktning Lärarhögskolan
                Viktning.LHWeighting(p);
            }

            if(model.equals("HF")) {

                //viktning humanistiska fakulteten
                Viktning.HFWeighting(p);

            }

            //Default
            //Viktning.DefaultWeightning(p);

        }


        System.out.println("...klar!");


        //FRACTIONALIZATION - TODO WHY WAS THIS REMOVE IN ERLIER VERSION, DOSENT MAKE ANY SENSE!!


        AuthorDisambiguation authorDisambiguation = new AuthorDisambiguation(new java.io.File("Mappningsfil20200103.xlsx"), new File("PersonalData_202006080006164.xml"));
        authorDisambiguation.mapAffiliationsAndDisanbigueAuthors(reducedPostList);


        System.out.println("Fraktionaliserar..");
        for(Post p : reducedPostList) {

            for(Author a : p.getAuthorList()) {
                a.calculateAndSetFraction( p.getNrAuthors() );
            }

        }




        /**
         *
         * samla alla författar object med PrintAuthor = true + sorterar
         *
         */



        List<Author> consideredAuthorsPostPairs = new ArrayList<>();
        DeduplicatePostsPerAuthor deduplicatePostsPerAuthor = new DeduplicatePostsPerAuthor();

        for(Post p : reducedPostList) {

            List<Author> allAuthors = p.getAuthorList();

            for(Author a: allAuthors ) {

                if(a.getPrintAuthor()) {

                    //CHECK SO WE HAVE'T SEEN THE COMBINATION OF AUTHOR AND POST BEFORE, I.E., DUPLICATION CONTROL
                    if(!deduplicatePostsPerAuthor.isInSet(a)) {

                        System.out.print("A COMBINATION OF CAS AND POST DUPLICATION DETECTED! PID:" + " " + a.getEnclosingPost().getPID());
                        System.out.println(" ... Ignored in the summary statistics");

                    } else {  consideredAuthorsPostPairs.add(a); }



                    }

            }

        }


        Collections.sort(consideredAuthorsPostPairs);


        System.out.println("Beräknar publikationspoäng per CAS..enligt modell: " + model);

        PublicationPointPerAuthor publicationPointPerAuthor = new PublicationPointPerAuthor();

        double collaborationWeight = 1.2;
        publicationPointPerAuthor.calculateAggregateAuthorStatistics(consideredAuthorsPostPairs,collaborationWeight);
        System.out.println("Using collaboration weight: " + collaborationWeight );

        //ADD CAS THAT HAVE NO PUB TO THE SUMMARY STATISTIC (i.e., 0,0,0)

        if(listaMedCasForskningstid.getCasWithNoPublications().size() > 0) {

            for(String s : listaMedCasForskningstid.getCasWithNoPublications()) {


                publicationPointPerAuthor.addAuthorsWithZeroPub(s,listaMedCasForskningstid.getForskningsTid(s));

            }

        }



        System.out.println("..klar!");

        /**
         *
         * Sriv till timeStamped Excel-fil
         *
         */


        System.out.println("Sparar information till fil..");

        SaveToExcel saveToExcel = new SaveToExcel();

        saveToExcel.addFullPublicationData(reducedPostList);



        //TODO OBS USING LF for new HF WHAT THE HELL IS THIS!!!
        if(model.equals("HF")) saveToExcel.addAggregatedAuthorStatisticsLärarHögskolan(publicationPointPerAuthor);

        if(model.equals("LH")) saveToExcel.addAggregatedAuthorStatisticsLärarHögskolan(publicationPointPerAuthor);

        //add zero publishing CAS to reducedPostList with a dummy row

        if(listaMedCasForskningstid.getCasWithNoPublications().size() > 0) {

            for(String s : listaMedCasForskningstid.getCasWithNoPublications()) {

                Author a = new Author();
                a.setPID(-1);
                a.setAuthorName("nonPublishingAuthor");
                a.setCas(s);
                a.setFractionConsiderMultipleUmUAffils(0);
                a.setFractionIgnoreMultipleUmUAffils(0);
                a.setFractionIgnoreMultipleUmUAffilsMin01(0);

                Post p = null; // this is handled in write to excel
                a.setEnclosingPost(p);

               consideredAuthorsPostPairs.add(a);

            }

        }
        saveToExcel.addBibliometricInformation(consideredAuthorsPostPairs);
        saveToExcel.saveToFile(model);

        System.out.println("..klar! Se filen Resultat[...].xlsx");



        // for(Post p : reducedPostList) {

         //   System.out.println(p.getPID() + "\t" + p.getDivaPublicationType() +"\t" + p.getDivaContentType() + "\t"  + p.getNorskNivå() + "\t" + p.getStatusInModel().getStatusInModel());
      //  }




    /*



        for(Author a : consideredAuthors) {

            Post p = a.getEnclosingPost();
            NorwegianMatchInfo norskNivå = p.getNorskNivå();

            System.out.println(p.getPID() + "\t" + a.getCas() + "\t" + a.getAuthorName() + "\t" + a.getAffiliations() + "\t" + p.getTitle() +"\t" + a.getFractionIgnoreMultipleUmUAffils() + "\t" + p.getDivaPublicationType() + "\t" + p.getDivaContentType() +"\t" + norskNivå.getType() +"\t" +norskNivå.getHow() + "\t" + norskNivå.getNorsk_id() +"\t" + norskNivå.getNorsk_namn() + "\t" + norskNivå.getNivå() + "\t" + norskNivå.getMax_nivå() + "\t" + p.getStatusInModel().getStatusInModel() + "\t" + norskNivå.getModelSpecificInfo() +"\t" + norskNivå.getVikt() );

        }


*/


        /*
        *
        *Sum weights, fractionalize (LH) , weight w.r.t forskningstid, set max övrigt (HF)
        *



        PublicationPointPerAuthor sumAuthorWeights = new PublicationPointPerAuthor();

        sumAuthorWeights.calculateAggregateAuthorStatistics(consideredAuthors);

        TreeMap<String,AggregatedAuthorInformation> result = sumAuthorWeights.getAuthorsAggregatedStatistics();

        System.out.println("#########AUTHOR SUMMARY##############");

        System.out.println("SÖKANDE" +"\t" + "ANTAL POSTER" +"\t" + "FÖRFATTARFRAKTIONER" + "\t" + "POÄNG TOTALT (A)" +"\t" + "POÄNG TOTALT, MAX 5 ÖVRIGA PER ÅR (B)" +"\t" + "FORSKNINGSTID (C)" + "\t" + "FORSKNINGSTIDSVIKT (D)" +"\t" + "INDIKATOR (D*B)" );
        for(Map.Entry<String,AggregatedAuthorInformation> entry : result.entrySet()) {

            AggregatedAuthorInformation aggregatedAuthorInformation = entry.getValue();

            System.out.println(entry.getKey() + "\t" + aggregatedAuthorInformation.getRawSumOfPublications() +"\t" +aggregatedAuthorInformation.getRawSumOfFractions() +"\t" + aggregatedAuthorInformation.getRawSumOfWeights() +"\t" + aggregatedAuthorInformation.getRestrictedSumOfWeights() +"\t" + aggregatedAuthorInformation.getForskningstidProcent() +"\t" + aggregatedAuthorInformation.getForskningstidsVikt() + "\t" + ( aggregatedAuthorInformation.getRestrictedSumOfWeights() * aggregatedAuthorInformation.getForskningstidsVikt() )    );
        }

       */

    }


}
