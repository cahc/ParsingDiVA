package org.cc.applikationer;

import jsat.utils.IntList;
import org.apache.http.client.methods.HttpPost;
import org.cc.diva.Author;
import org.cc.diva.AuthorDisambiguation;
import org.cc.diva.CreateDivaTable;
import org.cc.diva.Post;
import org.cc.misc.DivaIDtoNames;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by crco0001 on 9/17/2018.
 */
public class CreateAuthorImportFile {

    /*

    Create an importFile with authors to be used in SciVal

    #Author,Name variants,Affiliation,EIDs,DOIs,PMIDs,Title,ISSN/Volume/Issue/Pages,Scopus Author ID,ORCID,Level 1,Level 2,Level 3,Level 4,Level 5,Level 6,Level 7,Level 8,Level 9,Level 10

     */


    /*

    http://umu.diva-portal.org/smash/export.jsf?format=csvall2&addFilename=true&aq=[[]]&aqe=[]&aq2=[[{"dateIssued":{"from":"2000","to":"2018"}},{"organisationId":"715","organisationId-Xtra":true},{"publicationTypeCode":["review","article","conferencePaper"]},{"contentTypeCode":["refereed","science"]}]]&onlyFullText=false&noOfRows=99999&sortOrder=title_sort_asc&sortOrder2=title_sort_asc


     */

    //Author	Name variants	Affiliation	EIDs	DOIs	PMIDs	Title	ISSN/Volume/Issue/Pages	Scopus Author ID	ORCID	Level 1	Level 2  //todo the other levels	Level 3	Level 4	Level 5	Level 6	Level 7	Level 8	Level 9	Level 10

    public static void printAlternatives() {
        System.out.println("1\t(T) Institutionen för naturvetenskapernas och matematikens didaktik");
        System.out.println("2\t(T) Institutionen för ekologi, miljö och geovetenskap");
        System.out.println("3\t(T) Institutionen för datavetenskap");
        System.out.println("4\t(T) Designhögskolan vid Umeå universitet");
        System.out.println("5\t(T) Institutionen för fysik");
        System.out.println("6\t(T) Institutionen för fysiologisk botanik");
        System.out.println("7\t(T) Kemiska institutionen");
        System.out.println("8\t(T) Institutionen för matematik och matematisk statistik");
        System.out.println("9\t(T) Institutionen för tillämpad fysik och elektronik");
        System.out.println("10\t(T) Institutionen för molekylärbiologi (Teknisk-naturvetenskaplig fakultet)");
        System.out.println("11\t(T) Arkitekthögskolan vid Umeå universitet");
        System.out.println("12\t(S) Enheten för undervisning och lärande");
        System.out.println("13\t(S) Institutionen för tillämpad utbildningsvetenskap");
        System.out.println("14\t(S) Institutionen för psykologi");
        System.out.println("15\t(S) Demografiska databasen");
        System.out.println("16\t(S) Institutionen för geografi och ekonomisk historia");
        System.out.println("17\t(S) Handelshögskolan vid Umeå universitet");
        System.out.println("18\t(S) Institutionen för informatik");
        System.out.println("19\t(S) Juridiska institutionen");
        System.out.println("20\t(S) Institutionen för kostvetenskap");
        System.out.println("21\t(S) Umeå centrum för genusstudier (UCGS)");
        System.out.println("22\t(S) Pedagogiska institutionen");
        System.out.println("23\t(S) Enheten för polisutbildning vid Umeå universitet");
        System.out.println("24\t(S) Institutionen för socialt arbete");
        System.out.println("25\t(S) Sociologiska institutionen");
        System.out.println("26\t(S) Statsvetenskapliga institutionen");
        System.out.println("27\t(S) Enheten för restauranghögskolan");
        System.out.println("28\t(S) Juridiskt forum");
        System.out.println("29\t(M) Institutionen för farmakologi och klinisk neurovetenskap");
        System.out.println("30\t(M) Institutionen för folkhälsa och klinisk medicin");
        System.out.println("31\t(M) Institutionen för integrativ medicinsk biologi (IMB)");
        System.out.println("32\t(M) Institutionen för klinisk vetenskap");
        System.out.println("33\t(M) Institutionen för medicinsk biovetenskap");
        System.out.println("34\t(M) Institutionen för odontologi");
        System.out.println("35\t(M) Institutionen för omvårdnad");
        System.out.println("36\t(M) Institutionen för samhällsmedicin och rehabilitering");
        System.out.println("37\t(M) Institutionen för strålningsvetenskaper");
        System.out.println("38\t(M) Institutionen för klinisk mikrobiologi");
        System.out.println("39\t(M) Institutionen för medicinsk kemi och biofysik");
        System.out.println("40\t(M) Institutionen för kirurgisk och perioperativ vetenskap");
        System.out.println("41\t(M) Institutionen för molekylärbiologi (Medicinska fakulteten)");
        System.out.println("42\t(M) Enheten för biobanksforskning");
        System.out.println("43\t(M&T) Molekylärbiologi (Teknat- och Medfak, nedlaggd)");
        System.out.println("44\t(H) Institutionen för idé- och samhällsstudier");
        System.out.println("45\t(H) Institutionen Konsthögskolan");
        System.out.println("46\t(H) Institutionen för kultur- och medievetenskaper");
        System.out.println("47\t(H) Institutionen för språkstudier");
        System.out.println("48\t(H) Institutionen för estetiska ämnen i lärarutbildningen");


    }





    public static String fixName(String input) {

        if(input.length() == 0) return  input;

        IntList spaces = new IntList();
        IntList dashes = new IntList();
        IntList spike = new IntList();
        int N = input.length();

        for(int i=0; i<N; i++){

            if( input.charAt(i) == ' ') spaces.add(i);
            if( input.charAt(i) == '-') dashes.add(i);
            if( input.charAt(i) == '|') spike.add(i);

        }

        StringBuilder stringBuilder = new StringBuilder(input);

        stringBuilder.setCharAt(0, Character.toUpperCase(input.charAt(0))); //Always capitalize start

        for(int i : spaces) {

            stringBuilder.setCharAt(i+1, Character.toUpperCase(input.charAt(i+1)));
        }

        for(int i : dashes) {

            stringBuilder.setCharAt(i+1, Character.toUpperCase(input.charAt(i+1)));
        }

        for(int i : spike) {

            stringBuilder.setCharAt(i+1, Character.toUpperCase(input.charAt(i+1)));
        }



        return stringBuilder.toString();
    }

    private static StringBuilder header = new StringBuilder();
       static {
           header.append("Author");
           header.append(",");
           header.append("Name variants");
           header.append(",");
           header.append("Affiliation");
           header.append(",");
           header.append("EIDs");
           header.append(",");
           header.append("DOIs");
           header.append(",");
           header.append("PMIDs");
           header.append(",");
           header.append("Title");
           header.append(",");
           header.append("ISSN/Volume/Issue/Pages");
           header.append(",");
           header.append("Scopus Author ID");
           header.append(",");
           header.append("ORCID");
           header.append(",");
           header.append("Level 1");
           header.append(",");
           header.append("Level2");
       }


       private static HashMap<Integer,String> mapNrToInst = new HashMap();
       static {

           mapNrToInst.put(1,"Institutionen för naturvetenskapernas och matematikens didaktik");
           mapNrToInst.put(2,"Institutionen för ekologi, miljö och geovetenskap");
           mapNrToInst.put(3,"Institutionen för datavetenskap");
           mapNrToInst.put(4,"Designhögskolan vid Umeå universitet");
           mapNrToInst.put(5,"Institutionen för fysik");
           mapNrToInst.put(6,"Institutionen för fysiologisk botanik");
           mapNrToInst.put(7,"Kemiska institutionen");
           mapNrToInst.put(8,"Institutionen för matematik och matematisk statistik");
           mapNrToInst.put(9,"Institutionen för tillämpad fysik och elektronik");
           mapNrToInst.put(10,"Institutionen för molekylärbiologi (Teknisk-naturvetenskaplig fakultet)");
           mapNrToInst.put(11,"Arkitekthögskolan vid Umeå universitet");
           mapNrToInst.put(12,"Enheten för undervisning och lärande");
           mapNrToInst.put(13,"Institutionen för tillämpad utbildningsvetenskap");
           mapNrToInst.put(14,"Institutionen för psykologi");
           mapNrToInst.put(15,"Demografiska databasen");
           mapNrToInst.put(16,"Institutionen för geografi och ekonomisk historia");
           mapNrToInst.put(17,"Handelshögskolan vid Umeå universitet");
           mapNrToInst.put(18,"Institutionen för informatik");
           mapNrToInst.put(19,"Juridiska institutionen");
           mapNrToInst.put(20,"Institutionen för kostvetenskap");
           mapNrToInst.put(21,"Umeå centrum för genusstudier (UCGS)");
           mapNrToInst.put(22,"Pedagogiska institutionen");
           mapNrToInst.put(23,"Enheten för polisutbildning vid Umeå universitet");
           mapNrToInst.put(24,"Institutionen för socialt arbete");
           mapNrToInst.put(25,"Sociologiska institutionen");
           mapNrToInst.put(26,"Statsvetenskapliga institutionen");
           mapNrToInst.put(27,"Enheten för restauranghögskolan");
           mapNrToInst.put(28,"Juridiskt forum");
           mapNrToInst.put(29,"Institutionen för farmakologi och klinisk neurovetenskap");
           mapNrToInst.put(30,"Institutionen för folkhälsa och klinisk medicin");
           mapNrToInst.put(31,"Institutionen för integrativ medicinsk biologi (IMB)");
           mapNrToInst.put(32,"Institutionen för klinisk vetenskap");
           mapNrToInst.put(33,"Institutionen för medicinsk biovetenskap");
           mapNrToInst.put(34,"Institutionen för odontologi");
           mapNrToInst.put(35,"Institutionen för omvårdnad");
           mapNrToInst.put(36,"Institutionen för samhällsmedicin och rehabilitering");
           mapNrToInst.put(37,"Institutionen för strålningsvetenskaper");
           mapNrToInst.put(38,"Institutionen för klinisk mikrobiologi");
           mapNrToInst.put(39,"Institutionen för medicinsk kemi och biofysik");
           mapNrToInst.put(40,"Institutionen för kirurgisk och perioperativ vetenskap");
           mapNrToInst.put(41,"Institutionen för molekylärbiologi (Medicinska fakulteten)");
           mapNrToInst.put(42,"Enheten för biobanksforskning");
           mapNrToInst.put(43,"Molekylärbiologi (Teknat- och Medfak, nedlaggd)");
           mapNrToInst.put(44,"Institutionen för idé- och samhällsstudier");
           mapNrToInst.put(45,"Institutionen Konsthögskolan");
           mapNrToInst.put(46,"Institutionen för kultur- och medievetenskaper");
           mapNrToInst.put(47,"Institutionen för språkstudier");
           mapNrToInst.put(48,"Institutionen för estetiska ämnen i lärarutbildningen");



       }

    public static String generateImportLine(List<Author> authorObjects,String affiliation, String faculty, String department) {

        int maxDoiAndIssnAndPmid = 15;

        HashSet<String> nameVariants = new HashSet<>();
        HashSet<String> DOIs = new HashSet<>();
        HashSet<String> titles = new HashSet<>();
        HashSet<String> PMIDs = new HashSet<>();
        HashSet<String> ISSNs = new HashSet<>();

        for(Author a : authorObjects) {

            Post p = a.getEnclosingPost();
            String name = a.getAuthorName();
            String language = p.getDivaLanguage();
            String DOI = p.getDOI();
            String title = p.getTitle();
            String PMID = p.getPMID();
            String ISSN = p.getJournalISSN(); //OBS if multiple then ;-delimited
            ISSN = ISSN.replaceAll(";","|");

            if(name.length() > 3) nameVariants.add(name);
            if(DOI.length() > 4) DOIs.add(DOI);
            if(title.length() > 4 && ("eng".equals(language)  || "".equals(language)) ) titles.add(title); //take only english or unknown title info
            if(PMID.length() > 4) PMIDs.add(PMID);
            if(ISSN.length() >4) ISSNs.add(ISSN);



        }


     //if comma in field, must be enclosed with " "


      StringBuilder nameBuilder = new StringBuilder();
      StringBuilder nameVariantBuilder = new StringBuilder();


      boolean first = true;
      boolean firstVariant = true;
      for(String n : nameVariants) {

          if(first) {

              first = false;
              nameBuilder.append(n);


          } else {


              if(firstVariant) {

                  nameVariantBuilder.append(n);
                  firstVariant = false;
              } else {

                  nameVariantBuilder.append("|");
                  nameVariantBuilder.append(n);
              }



          }

      }


      String fixedName = fixName(nameBuilder.toString());
      String fixedVariants = fixName(nameVariantBuilder.toString());


        StringBuilder doiBuilder = new StringBuilder();

        boolean firstDOI = true;
        int dnr = 1;
        for(String doi : DOIs) {


            if(firstDOI) {
                firstDOI = false;
                doiBuilder.append(doi);

            } else {

                doiBuilder.append("|");
                doiBuilder.append(doi);

            }

            if(dnr == maxDoiAndIssnAndPmid) break;
            dnr++;
        }



        StringBuilder ISSNBuilder = new StringBuilder();

        boolean firstISSN=true;
        int inr = 1;
        for(String issn : ISSNs) {

            if(firstISSN) {

                firstISSN=false;
                ISSNBuilder.append(issn);

            } else {

                ISSNBuilder.append("|");
                ISSNBuilder.append(issn);

            }

            if(inr == maxDoiAndIssnAndPmid) break;
            inr++;
        }


        StringBuilder PMIDBuilder= new StringBuilder();

        boolean firstPMID = true;
        int pnr = 1;
        for(String pmid : PMIDs) {

            if(firstPMID) {

                firstPMID = false;
                PMIDBuilder.append(pmid);

            } else {

                PMIDBuilder.append("|");
                PMIDBuilder.append(pmid);

            }

            if(pnr == maxDoiAndIssnAndPmid) break;
            pnr++;
        }


        StringBuilder titleBuilder= new StringBuilder();

        boolean firstTitle = true;
        for(String title : titles) {

            if(firstTitle) {

                firstTitle = false;

                String newTitle = title.replaceAll("|", "");
                newTitle = newTitle.replaceAll(";", "");

                titleBuilder.append(newTitle);

            } else {

                titleBuilder.append("|");
                String newTitle = title.replaceAll("|", "");
                newTitle = newTitle.replaceAll(";", "");
                titleBuilder.append(newTitle);

            }

        }


        //Author	Name variants	Affiliation	EIDs	DOIs	PMIDs	Title	ISSN/Volume/Issue/Pages	Scopus Author ID	ORCID	Level 1	Level 2  //todo the other levels	Level 3	Level 4	Level 5	Level 6	Level 7	Level 8	Level 9	Level 10


        StringBuilder row = new StringBuilder();

        row.append("\"").append( fixedName ).append("\""); // first nameVariant
        row.append(",");
        if(nameVariantBuilder.length() > 0) {

            row.append("\"").append( fixedVariants ).append("\""); // variants

        }
        row.append(",");
        row.append(affiliation);
        row.append(",");
        //todo EIDs
        row.append(",");

        if(doiBuilder.length() > 0) {

            row.append(doiBuilder.toString());
        }
        row.append(",");

        if(PMIDBuilder.length() >0) {

            row.append(PMIDBuilder.toString());
        }
        row.append(",");
        if(titleBuilder.length() > 0) {

            //TODO enable titles if it is good
          // row.append("\"");
           // row.append(titleBuilder.toString());
          //  row.append("\"");
        }
        row.append(",");

        if(ISSNBuilder.length() > 0) {

            row.append(ISSNBuilder.toString());
        }
        row.append(",");
        //todo scopus author ID
        row.append(",");
        //todo ORCID
        row.append(",");

        row.append(faculty);
        row.append(",");
        row.append(department);






        return row.toString();
    }


    public static void main(String[] arg) throws IOException, ParseException, XMLStreamException {


        if(arg.length != 3) {

            System.out.println("parameters expected: divaDump (CSV) DiVAAffilMapping (xlsx) personelData (XML)");
            System.out.println("ex: java -cp DivaTools.jar org.cc.applikationer.CreateAuthorImportFile 2000-2018UMU.csv Mappningsfil20180917.xlsx PersonalData_201809170003188.xml");
            System.exit(0);
        }
        System.out.println("********************************************************************************************************");
        System.out.println("********************************************************************************************************");
        System.out.println("**Rutin för att identifierar författare från angiven institution och generera en importfil till SciVal**");
        System.out.println("********************************************************************************************************");
        System.out.println("********************************************************************************************************");
        System.out.println();
        System.out.println("Alfaversion 2018-09-19");
        System.out.println();
        printAlternatives();


        Scanner scanner = new Scanner(System.in);
        String thisDepartment = "null";

            while (true) {
                System.out.print("Välj institution (nr): ");
                String integerString = scanner.nextLine();
                int integer = -1;

                try {
                    integer = Integer.valueOf(integerString);
                } catch (NumberFormatException e) {
                }

                thisDepartment = mapNrToInst.get(integer);

                if (thisDepartment != null) break;

                System.out.println("Fel! Ange ett nummer mellan 1 och N..");
            }




        File csvInput = new  File(arg[0]); //File("C:\\Users\\crco0001\\Desktop\\SciVal_utvärderingSeptember\\DiVA\\2000-2018UMU.csv");
        File affilMapping = new  File(arg[1]);//File("C:\\Users\\crco0001\\Desktop\\SciVal_utvärderingSeptember\\DiVA\\Mappningsfil20180917.xlsx");
        File pdataMapping = new  File(arg[2]); //File("C:\\Users\\crco0001\\Desktop\\SciVal_utvärderingSeptember\\DiVA\\PersonalData_201809170003188.xml");



        //String thisDepartment = arg[3]; //"Sociologiska institutionen";
        //check that it is valid
        AuthorDisambiguation authorDisambiguation = new AuthorDisambiguation(affilMapping, pdataMapping);
        if(!authorDisambiguation.validInstitution(thisDepartment) ) {System.out.println("NO A VALID INSTITUTION! ABORTING!"); System.exit(0); }




        CreateDivaTable divaTable = new CreateDivaTable(csvInput);
        divaTable.parse();

        System.out.println("Records in DiVA dump: " + divaTable.nrRows() );

        List<Post> postList = new ArrayList<>(100);

        for(int i = 0; i< divaTable.nrRows(); i++) {


            //if(listaMedCasForskningstid.includeRawPostByDefault(divaTable, i)) {
            Post post = new Post(divaTable.getRowInTable(i));
            postList.add(post);

        }


        System.out.println("# objects created: " + postList.size());


        /**
         *
         * Author disambiguate
         *
         */




        System.out.println("Running author disambiguation..wait");
        authorDisambiguation.mapAffiliationsAndDisanbigueAuthors(postList,false);


        /*

        Get authors from a specific institution

         */





        HashMap<String, List<Author>> uniqueIDtoAuthorList =  new HashMap<>();



        for(Post p : postList) {


           List<Author> authorsList = p.getAuthorList();

           for(Author author : authorsList) {

               if(!author.hasUmuDivaAddress()) continue;

                      List<DivaIDtoNames> umuAffilInfo = author.getAffilMappingsObjects();

                      for(DivaIDtoNames divaIDtoNames : umuAffilInfo) {

                         if(thisDepartment.equals(divaIDtoNames.getINSTITUTION())) {

                             String uniqueId = author.getDisambiguateID();

                             List<Author> authorObjects = uniqueIDtoAuthorList.get(uniqueId); //author objects are unique to the article they belong to

                             if(authorObjects == null) {

                                 List<Author> newAuthorObjects = new ArrayList<>(5);
                                 newAuthorObjects.add(author);
                                 uniqueIDtoAuthorList.put(uniqueId,newAuthorObjects);

                             } else {

                                 authorObjects.add(author);

                             }

                             // System.out.println( author.getAuthorName() +" " + author.hasUmuDivaAddress() + " " + author.getAutomaticAddedCass() +" " + author.getCas() + " " + author.getDisambiguateID() +  " " + author.getAffilMappingsObjects() );

                          }


                      } //loop over affils

           } //loop over aothors


        } //loop over posts


        System.out.println("# unique authors identified:" + uniqueIDtoAuthorList.size() );

      //  List<Author> collianders = uniqueIDtoAuthorList.get("A4134");

     //   for(Author a : collianders) {

    //        System.out.println(a.getEnclosingPost().getTitle() + "  "+ a.getEnclosingPost().getDOI() + " " + a.getEnclosingPost().getDivaLanguage());
   //     }


   //     System.out.println("###");

   //     System.out.println(generateImportLine(collianders,"Umeå university",authorDisambiguation.getFaculty(thisDepartment),thisDepartment) );


        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm") ;


        String filename ="Import-"+thisDepartment +"-"+ dateFormat.format(date) + ".csv" ;



        BufferedWriter outputFile = new BufferedWriter( new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8) );

        outputFile.write(header.toString());
        outputFile.newLine();

        for(Map.Entry<String,List<Author>> entry : uniqueIDtoAuthorList.entrySet()) {


            outputFile.write(  generateImportLine(entry.getValue(),"Umeå university",authorDisambiguation.getFaculty(thisDepartment),thisDepartment) );
            outputFile.newLine();

        }


        outputFile.flush();
        outputFile.close();

        System.out.println("Klar, se: " + filename);

    }

}
