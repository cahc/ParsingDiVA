package org.cc.applikationer;

import com.cc.PersonLevelAffil.DataLoader;
import com.cc.PersonLevelAffil.Person;
import com.cc.PersonLevelAffil.PersonLatestInfo;
import org.cc.NorskaModellen.*;
import org.cc.diva.*;
import org.cc.misc.SaveToExcel;
import org.cc.misc.Thesaurus;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class StandardImplementationWithPersonnelData {

    public static class UMUID_YEARS_AT_UNITS {

        public final Integer minYears;
        public final Integer maxYears;

        //UMUID -- > UNIT(S) --> YEARS AT
        public final HashMap<String,HashMap<String,TreeSet<Integer>>> UMUID_TO_YEARS_AT_UNITS;

        public UMUID_YEARS_AT_UNITS(Integer minYears, Integer maxYears, HashMap<String,HashMap<String, TreeSet<Integer>>> UMUID_TO_YEARS_AT_UNITS) {
            this.minYears = minYears;
            this.maxYears = maxYears;
            this.UMUID_TO_YEARS_AT_UNITS = UMUID_TO_YEARS_AT_UNITS;
        }



    }

    public static HashMap<String, String> readColumnData(String filePath, String keyColumn, String valueColumn, String sep) throws IOException {
        HashMap<String, String> map = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("The file is empty.");
            }

            // Split the header by tabs and find the indexes of the desired columns
            String[] headers = headerLine.split(sep);
            int keyIndex = -1;
            int valueIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                if (headers[i].equals(keyColumn)) {
                    keyIndex = i;
                }
                if (headers[i].equals(valueColumn)) {
                    valueIndex = i;
                }
            }

            // Validate the indexes
            if (keyIndex == -1 || valueIndex == -1) {
                throw new IllegalArgumentException("Column names not found in the file header.");
            }

            // Read the rest of the file and populate the HashMap
            String line;
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(sep);

                // Ensure there are enough columns in this row
                if (columns.length > keyIndex && columns.length > valueIndex) {
                    String key = columns[keyIndex].trim();
                    String value = columns[valueIndex].trim();
                    map.put(key, value);
                }
            }
        }

        return map;
    }

    public static void main(String[] args) throws IOException, XMLStreamException, ParseException {


        DataLoader dataLoader = new DataLoader();

        CreateDivaTable divaTable = new CreateDivaTable(new File("E:\\2025\\MEDFAK GENOMLYSING\\2016-2024.csv")); //2016-2024.csv for full
        divaTable.parse();
        System.out.println("Antal rader i CSV: " + divaTable.nrRows());

        HashMap<String,String> IDtoContrib = readColumnData("E:\\2025\\MEDFAK GENOMLYSING\\PIDTOCONTRIB_EXTRA.txt","PID","NoOfContributors",";");
        System.out.println("Records with different number of contributors w r t registered authors: " + IDtoContrib.size());
        HashMap<Integer,Integer> PIDtoFullAuthorCount = new HashMap<>();
        for(Map.Entry<String,String> entry : IDtoContrib.entrySet()){

            PIDtoFullAuthorCount.put(Integer.valueOf(entry.getKey()),Integer.parseInt(entry.getValue()));

        }

        List<Post> postList = new ArrayList<>(1024);

        for (int i = 0; i < divaTable.nrRows(); i++) {
            Post post = new Post(divaTable.getRowInTable(i));
            postList.add(post);

        }

        System.out.println("Poster skapade: " + postList.size());


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


        AuthorDisambiguation authorDisambiguation = new AuthorDisambiguation(new File("E:\\2025\\MEDFAK GENOMLYSING\\Mappningsfil20241111.xlsx"),dataLoader);

        authorDisambiguation.mapAffiliationsAndDisanbigueAuthors(postList,true);


        /*


        When is the

         */


        Set<Integer> useStraightFractionalizationForThese = PIDtoFullAuthorCount.keySet();
        TreeSet<Integer> yearsInCurrentData = new TreeSet<>(); //meta data for later creating UMUID@UMU ..
        HashSet<String> uniqueAndValidUMUID = new HashSet<>(); // meta data for later creating UMUID@UMU ..

        for (Post post : postList) {

            yearsInCurrentData.add(post.getYear());

            boolean orderInformationAvailiable = true;

            List<Author> authorList = post.getAuthorList();
            int authorCount = authorList.size();
            if(useStraightFractionalizationForThese.contains(post.getPID())) {


                int trueCount = PIDtoFullAuthorCount.get(post.getPID());
                if(trueCount != authorCount) {

                    System.out.println("PID: " +  post.getPID() + "\t" + authorCount + "\t" + trueCount );
                    orderInformationAvailiable = false;
                    authorCount = trueCount; //update authorCount

                }

            }


            post.setNrAuthors(authorCount); //update with corrected info



            //fractionalize and set befattning

            boolean anyUMU = false;
            for(int i=0; i<authorList.size(); i++) {

                Author author = authorList.get(i);
                author.calculateAndBylineAwareFraction(authorCount,(i+1),orderInformationAvailiable);

                if(author.getHasControlledAddresses() ) anyUMU = true; //If it is a author with registered UMU organization

                if(author.getHasControlledAddresses() ) {

                    //lets find the persons position

                   String umuid = author.getAutomaticAddedCass();

                   if(umuid.equals("not available")) {

                       author.setBefattningAtPublicationDate("missing umuid");
                       author.setLastKnownBeffattning("missing umuid");

                   } else {

                       Person deltaPerson = dataLoader.getPersonByCas(umuid);

                       if (deltaPerson != null) {
                           uniqueAndValidUMUID.add(umuid);
                           PersonLatestInfo latestInfo = new PersonLatestInfo(deltaPerson);
                           Set<String> befattningarAtPubTime = deltaPerson.getBestGuessBefattningAtPublicationYearOrClosestPossible(post.getYear()); //liberal

                           //TODO remap to categories and pick the highest..
                           author.setBefattningAtPublicationDate(befattningarAtPubTime.toString());
                           author.setLastKnownBeffattning(latestInfo.getLatestPosition());
                       } else {


                           author.setBefattningAtPublicationDate("umuid unknown, possible a student");
                           author.setLastKnownBeffattning("umuid unknown, possible a student");

                       }


                   }

                }

            }

            post.setHasUMUAuthors(anyUMU);

        } //for each post


        ConsideredPublications publicationsToInclude = new DefaultPubIncludingAheadOfPrint();

        System.out.println("Matchar mot norska listan..");


        /**
         *
         *Läs in norska listan
         *
         **/

        File lista = new File("E:\\2025\\MEDFAK GENOMLYSING\\norska listan nov 2025.xlsx");
        File thesaurusFil = new File("E:\\2025\\MEDFAK GENOMLYSING\\Thesaurus 2025-06-03.xlsx");
        List<NorskSerie> listaMedSerier = ReadNorwegianLists.parseSeries(lista);
        List<NorskFörlag> listaMedFörlag = ReadNorwegianLists.parseFörlag(lista);
        System.out.println("Antal serier i auktoritetsregister: " + listaMedSerier.size());
        System.out.println("Antal förlag i auktoritetsregister: " + listaMedFörlag.size());

        Thesaurus standardiseringsListor = new Thesaurus(thesaurusFil);



        for(Post p : postList) {


            //OBS hög tröskel, 0.94

            p.setStatusInModel( publicationsToInclude.consideredPub(p) ); // ska publikationen matchas mot norska listan, se StatusInMdelConstants
            NorwegianMatchInfo matchInfo = NorskNivå.getNorwegianLevel(listaMedSerier,listaMedFörlag,p,0.94,standardiseringsListor,true); //matcha eventuellt mot norska listan samt uppdatera StatusInModel
            p.setNorskNivå( matchInfo ); // uppdatera posten med information om matchning mot norska listan


            //check if it is a non journal that has series match, if match but level NULL, try to do a match again restricted to publisher..
            //this is a fix introduced 2021-06-22
            if(matchInfo.getNivå() == null && "serie".equals( matchInfo.getType() ) && !p.getDivaPublicationType().equals(DivaPublicationTypes.tidskrift) && !p.getDivaPublicationType().equals( DivaPublicationTypes.review ) ) {

                System.out.print(p.getPID() + " is matched against series, is not a journal AND the series level for the pubyear is NULL. Try again with publisher restricted matching.. ");

                NorwegianMatchInfo matchInfo2 = NorskNivå.getNorwegianLevelRestrictedToMatchOnPublisherOnlyPreMatched(listaMedFörlag,p,0.94,standardiseringsListor);

                //now check if if matchInfo2 is the same *object* as before (i.e., no new publisher match were made..)

                if(matchInfo2 == matchInfo) {System.out.println("no new match on publisher was made");} else {System.out.println("the matchInfo changed from NULL-series to publisher!"); }

                p.setNorskNivå(matchInfo2);

            }


            //considers epubahead of print
            Viktning.DefaultWeighting(p);



        }



        //calculate for each unique UMUID years@UMU, years@MED, years@SAM , years@HUM
        //which years are we considering?

        Integer minYearInSet = yearsInCurrentData.stream().min(Comparator.naturalOrder()).orElse(-1);
        Integer maxYearInSet = yearsInCurrentData.stream().max(Comparator.naturalOrder()).orElse(-1);
        if(minYearInSet.equals(-1) || maxYearInSet.equals(-1)) {System.out.println("Somthing is wrong with the publications years..aborting"); System.exit(0);}
        System.out.println("Years in current data: " + yearsInCurrentData + " min " + minYearInSet + " max " + maxYearInSet );
        System.out.println("Valid unique UMUID in data: " + uniqueAndValidUMUID.size());

        HashMap<String,HashMap<String,TreeSet<Integer>>> collectStat = new HashMap<>();

        for(String umuid : uniqueAndValidUMUID) {

            Person p = dataLoader.getPersonByCas(umuid);
            HashMap<String,TreeSet<Integer>> yearsAtDifferentUnits = p.yearsAtUMUAndFacultiesInTimeInterval(minYearInSet,maxYearInSet, false);
            collectStat.put(umuid, yearsAtDifferentUnits);
        }

        UMUID_YEARS_AT_UNITS umuidYearsAtUnits = new UMUID_YEARS_AT_UNITS(minYearInSet,maxYearInSet,collectStat);

        System.out.println( umuidYearsAtUnits.UMUID_TO_YEARS_AT_UNITS.get("crco0001") );

        //System.exit(0);
        /*


        SAVE TO FILES


         */


        System.out.println("Saving to files..");

        SaveToExcel saveToExcel = new SaveToExcel();
        saveToExcel.saveNorwegianMatchingInfo(postList);

        saveToExcel = new SaveToExcel();
        saveToExcel.saveDisambiguatedAuthorFractionsBylineAware2025(postList,false,umuidYearsAtUnits);

        saveToExcel = new SaveToExcel();
        saveToExcel.saveDisambiguatedAuthorFractionsBylineAware2025(postList,true,umuidYearsAtUnits);

        System.out.println("Resultat sparat i tre Excel-filer: Fractions och NorskMatchning..");


    }
}


