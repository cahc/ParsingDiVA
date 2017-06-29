package org.cc.wos;

import org.cc.diva.DivaHelpFunctions;
import org.cc.wos.misc.TabReader;
import org.cc.wos.misc.TabRecord;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by crco0001 on 8/29/2016.
 */
public class DiVAtoWoS {


    public static boolean matchOnUT(ArrayList<WosRecord> wosRecords, TabRecord tabRecord) {

        for(WosRecord wosRecord : wosRecords) {


            if(tabRecord.getUT().equals( wosRecord.getUT() )) {System.out.println("UT HIT ON: " + tabRecord.getPID()); return true; }

        }

        return false;
    }

    public static boolean matchOnPMID(ArrayList<WosRecord> wosRecords, TabRecord tabRecord) {

        for(WosRecord wosRecord : wosRecords) {


            if(tabRecord.getPUBMED().equals( wosRecord.getPMID() )) {System.out.println("PMID HIT ON: " + tabRecord.getPID()); return true; }

        }

        return false;

    }

    public static boolean matchOnDOI(ArrayList<WosRecord> wosRecords, TabRecord tabRecord ) {


        for(WosRecord wosRecord : wosRecords) {


            if(tabRecord.getDOI().equals( wosRecord.getDOI() )) {System.out.println("DOI HIT ON: " + tabRecord.getPID()); return true; }

        }

        return false;

    }

    public static boolean matchOnTitle(ArrayList<WosRecord> wosRecords, ArrayList<String> simplifyedWoS, TabRecord tabRecord ) {

        for(String simpleWoS : simplifyedWoS) {

            if( DivaHelpFunctions.simplifyString(tabRecord.getTI()).equals( simpleWoS )) {System.out.println("TITLE HIT ON: " + tabRecord.getPID()); return true; }

        }

        return false;
    }


    public static void main(String[] arg) throws IOException, ClassNotFoundException {


        Parser parser = new Parser(); //uses current directory
        //Parser parser = new Parser("C:\\");
        parser.validate();
        ArrayList<WosRecord> wosRecords = parser.parse();
        System.out.println(wosRecords.size() + " wos records parsed.");

        ArrayList<String> simplyfiedWoSTitles = new ArrayList<>();
        for(WosRecord w : wosRecords) {

          simplyfiedWoSTitles.add( DivaHelpFunctions.simplifyString( w.getTitle() ) );

        }


        TabReader tabReader = new TabReader("C:\\Testing\\matcha.txt");
        ArrayList<TabRecord> tabRecords = tabReader.getRecords();
        System.out.println(tabReader.getRecords().size() + " tab-records parsed.");

        boolean match = false;
        for(TabRecord tabRecord : tabRecords) {

            match = matchOnUT(wosRecords,tabRecord);
            if(match) continue;
            match = matchOnPMID(wosRecords,tabRecord);
            if(match) continue;
            match = matchOnDOI(wosRecords, tabRecord);
            if(match) continue;
            match = matchOnTitle(wosRecords,simplyfiedWoSTitles,tabRecord);

            if(!match) System.out.println("NO HIT ON: " + tabRecord.getPID());
        }


    }



}



