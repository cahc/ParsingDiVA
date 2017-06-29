package org.cc.wos;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Labeling;
import org.cc.diva.DivaHelpFunctions;
import org.cc.diva.DivaPublicationTypes;
import org.cc.wos.classifier.SerializeClassifier;
import org.cc.wos.misc.TabReader;
import org.cc.wos.misc.TabRecord;

import java.io.*;
import java.util.*;

/**
 * Created by crco0001 on 8/26/2016.
 */




public class Parser {

    public static Instance getUnPipedInstance(WosRecord r) {


        String data = r.getTitle() + " " + r.getAB();
        String keywords = r.getKeywords().toString();

        Instance instance = new Instance(data, null, null, keywords);

        return instance;

    }

    //the downloaded TAB-format always contains these column names even if no data is available!
    private final static String[] validTags = {"PT","AU","BA","BE","GP","AF","BF","CA","TI","SO","SE","BS","LA","DT","CT","CY","CL","SP","HO","DE","ID","AB","C1","RP","EM","RI","OI","FU","FX","CR","NR","TC","Z9","U1","U2","PU","PI","PA","SN","EI","BN","J9","JI","PD","PY","VL","IS","PN","SU","SI","MA","BP","EP","AR","DI","D2","PG","WC","SC","GA","UT","PM"};
    private String directory = System.getProperty("user.dir");
    private File[] files;
    private FilenameFilter filenameFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.matches("(?i:.*?.txt)");
        }
    };


    Parser() {



        File dir = new File(directory);
        if(!dir.exists()) {System.out.println("No such directory exists! Aborting."); System.exit(1); }

        this.files = dir.listFiles(filenameFilter);
        System.out.println("Files in directory:" + files.length);
        for(File f : files) {
            System.out.println(f.toString());
        }
    }

    Parser(String directory) {


          this.directory = directory;
          File dir = new File(this.directory);
          if(!dir.exists()) {System.out.println("No such directory exists! Aborting."); System.exit(1); }

          this.files = dir.listFiles(filenameFilter);
          System.out.println("Files in directory:" + files.length);
          for(File f : files) {
              System.out.println(f.toString());
          }

        System.out.println();
    }

    public void validate() throws IOException {

        ArrayList<File> validatedFiles = new ArrayList<>();
        int records = 0;
        if(files.length == 0) {System.out.println("No *.txt files in directory"); }

        BufferedReader reader;

        String[] splittedLine;
        String line;
        for(File f : files) {

            reader = new BufferedReader( new InputStreamReader( new FileInputStream(f),"UTF-8"));
            BOMSkipper.skip(reader);
            splittedLine = reader.readLine().split("\t");

            if(!Arrays.equals(splittedLine,validTags) ) {System.out.println(f.toString() + " contains invalid column headers!"); continue; }

            while( (line = reader.readLine()) != null ) {

                if( line.split("\t").length < 61) {System.out.println(f.toString() + " contains invalid rows! Found a row with " + line.split("\t").length +" columns" ); System.exit(1); }
                records++;
            }

            validatedFiles.add(f);
        }

        System.out.println(validatedFiles.size() + " WOS files passed validation");
        System.out.println(records + " records passed validation.");

        this.files = validatedFiles.toArray( new File[ validatedFiles.size() ] );
    }

    public ArrayList<WosRecord> parse() throws IOException, ClassNotFoundException {


        ArrayList<WosRecord> wosRecords = new ArrayList<>();

        BufferedReader reader;

        String[] splittedLine;
        String line;
        for(File f : files) {

            reader = new BufferedReader( new InputStreamReader( new FileInputStream(f),"UTF-8"));
            BOMSkipper.skip(reader);
            splittedLine = reader.readLine().split("\t");

            if(!Arrays.equals(splittedLine,validTags) ) {System.out.println(f.toString() + " contains invalid column headers! Aborting."); System.exit(1); }

            while( (line = reader.readLine()) != null ) {
                splittedLine = line.split("\t");
                WosRecord record = new WosRecord();

                record.setPT( splittedLine[ Tags.PT.getInt() ]   );
                record.setTitle( splittedLine[ Tags.TI.getInt() ] );
                record.setUT( splittedLine[Tags.UT.getInt()] );
                record.setPY( splittedLine[Tags.PY.getInt()] );
                if(splittedLine.length == 61) { record.setPMID(""); } else { record.setPMID( splittedLine[Tags.PM.getInt()] ); } // records with no PMID has 61 columns
                record.setDOI( splittedLine[Tags.DI.getInt()] );
                record.setSO(  splittedLine[Tags.SO.getInt()]  );
                record.setWC(  splittedLine[Tags.WC.getInt()] );
                record.setDT(  splittedLine[Tags.DT.getInt()]  );
                record.setMACRO9( WosHelperFunctions.SUBJECT_TO_MACRO9 );
                record.setAB( splittedLine[Tags.AB.getInt()] );
                record.setAuthors( splittedLine[Tags.AF.getInt()]  );
                record.setAdresses( splittedLine[Tags.C1.getInt()] );
                record.setKeywords(  splittedLine[Tags.DE.getInt()]  ); //Author keywords
                record.setKeywords( splittedLine[Tags.ID.getInt()] );  //Keywordplus

                wosRecords.add(record);
            }

        }

        return wosRecords;
    }


    public static void main(String[] arg) throws IOException, ClassNotFoundException {


    Parser parser = new Parser(); //uses current directory
    //Parser parser = new Parser("C:\\", false);

    parser.validate();
    ArrayList<WosRecord> wosRecords = parser.parse();
    System.out.println(wosRecords.size() + " wos records parsed.");


        //LOAD CLASSIFIER
        System.out.println("Loading classifier model");
        Classifier classifier = SerializeClassifier.loadClassifier("E:\\Classifier.bin");
        //put the an instance trough the same pipe
        Pipe pipe = classifier.getInstancePipe();

        
        // (A) IF A WOS RECORD CONTAINS MORE THAN ONE WC AND ONE OF THEM IS MULTISISCIPLINARY - REMOVE MULTIDISCIPLINARE
        // (B) IF A WOS RECORD CONTAINS ONE (1) WC AND THIS WC IS MULTIDISCIPLINARY - RECLASSIFY

        for(WosRecord r: wosRecords) {

            String[] WCs = r.getWC();

            if (WCs.length == 1) {

                if (WCs[0].equals("multidisciplinary sciences")) {

                    System.out.println("RECLASSIFICATION! --> " + r.getUT() );


                    Instance toClassify = getUnPipedInstance(r);

                    pipe.instanceFrom(toClassify);
                    Classification classification = classifier.classify(toClassify);
                    Labeling labeling = classification.getLabeling();

                    //IF BEST LABEL < 50% THEN USE TWO LABELS FOR CLASSIFICATION

                        r.addReclassifiedWCs( labeling.getLabelAtRank(0).toString()  );
                        if( labeling.getValueAtRank(0) < 0.5  ) r.addReclassifiedWCs( labeling.getLabelAtRank(1).toString()  );




                } else { r.addReclassifiedWCs( WCs[0] ); }
            }
            else {

                    for (int i = 0; i < WCs.length; i++) {
                        if ( !WCs[i].equals("multidisciplinary sciences") ) {
                           r.addReclassifiedWCs( WCs[i] );

                        }

                    }


                }


                 r.setReclassifiedMACRO9( WosHelperFunctions.SUBJECT_TO_MACRO9 );
            }




    //int j=0;
    for(WosRecord r : wosRecords) {


        System.out.println(r);

       //List<String> classes = Arrays.asList(r.getWC());
        //Collections.shuffle(classes);
        //for(int i=0; i<classes.size(); i++) {

         //  if(classes.get(i).equals("multidisciplinary sciences")) { continue; } else {

         //     System.out.println(classes.get(i) + "\t" + r.getTitle() + " " + r.getAB() +"\t" + r.getKeywords() +"\t" + j);
         //      j++;
              // break; // create multiple instances when dual classed?
         //  }
        }


    }



    }





