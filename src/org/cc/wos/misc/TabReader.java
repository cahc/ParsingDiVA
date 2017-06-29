package org.cc.wos.misc;

import org.cc.wos.BOMSkipper;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by crco0001 on 8/29/2016.
  * for matching Diva-records in WoS
 * requires the following columns: PID	TI	JOURNAL	DOI	PUBMED	UT


 */
public class TabReader {

    private ArrayList<TabRecord> records = new ArrayList<>();

    public TabReader(String file) throws IOException {

        String[] splittedLine;
        File input = new File(file);
        if(!input.exists()) {System.out.println("No such directory exists! Aborting."); System.exit(1); }

        BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream(input),"UTF-8"));
        BOMSkipper.skip(reader);

        String line;
        boolean firstRow = true;
        int PID_INDICE = -1;
        int TI_INDICE = -1;
        int JOURNAL_INDICE = -1;
        int DOI_INDICE = -1;
        int PUBMED_INDICE = -1;
        int UT_INDICE = -1;
        int YEAR_INDICE = -1;

        while(  (line = reader.readLine()) != null  ) {

            splittedLine =line.split("\t");

            if(firstRow) {

                for(int i=0; i<splittedLine.length; i++) {

                    if( splittedLine[i].equals("PID")) PID_INDICE = i;
                    if( splittedLine[i].equals("TI")) TI_INDICE = i;
                    if( splittedLine[i].equals("JOURNAL")) JOURNAL_INDICE = i;
                    if( splittedLine[i].equals("DOI")) DOI_INDICE = i;
                    if( splittedLine[i].equals("PUBMED")) PUBMED_INDICE = i;
                    if( splittedLine[i].equals("UT")) UT_INDICE = i;
                    if( splittedLine[i].equals("YEAR") ) YEAR_INDICE = i;

                }


                if(PID_INDICE == -1 || TI_INDICE == -1 || JOURNAL_INDICE == -1 || DOI_INDICE == -1 || PID_INDICE == -1 || UT_INDICE == -1 || YEAR_INDICE == -1) {

                    System.out.println("File containing DiVA data must be a tab separated file containing (at least) the following headers:PID TI JOURNAL DOI PUBMED UT YEAR");
                    System.exit(1);
                }

                firstRow = false;
                continue;
            }

            TabRecord record = new TabRecord();

            record.setDOI( splittedLine[DOI_INDICE] );
            record.setUT( splittedLine[UT_INDICE]  );
            record.setJOURNAL( splittedLine[JOURNAL_INDICE] );
            record.setPUBMED( splittedLine[PUBMED_INDICE] );
            record.setPID( splittedLine[PID_INDICE]  );
            record.setYEAR( splittedLine[YEAR_INDICE] );
            record.setTI( splittedLine[TI_INDICE] );

            this.records.add(record);

        }

        reader.close();

    }

    public ArrayList<TabRecord> getRecords() {

        return this.records;
    }


    public static void main(String[] arg) throws IOException {

        TabReader tabReader = new TabReader("C:\\Testing\\matcha.txt");

        System.out.println(tabReader.getRecords().size() + " tab-records read! ");

        for(TabRecord t : tabReader.getRecords()) {

            System.out.println(t);
        }


    }


}
