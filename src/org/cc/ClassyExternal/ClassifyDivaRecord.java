package org.cc.ClassyExternal;

import Database.FileHashDB;
import Database.ModsDivaFileParser;
import Database.ModsOnlineParser;
import Database.SwePubParser;
import SwePub.Record;
import org.cc.diva.CreateDivaTable;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by crco0001 on 6/13/2017.
 */
public class ClassifyDivaRecord {


    public static void main(String[] arg) throws IOException, XMLStreamException {


        ModsDivaFileParser modsDivaFileParser = new ModsDivaFileParser();


        List<Record> hej = modsDivaFileParser.parse("EXPORTMODSDIVA.xml");

        System.out.println(hej.size());
        BufferedWriter writer = new BufferedWriter( new FileWriter( new File("test.txt")));

        for(Record r : hej) {

            writer.write(r.toString());
            writer.newLine();

        }
        writer.flush();
        writer.close();


        /*

        //use mods instead
        File divaDumpFil = new File( "F:/divaExport(2010-2017).20170613_0834SET1.csv" );


        CreateDivaTable divaTable = new CreateDivaTable(divaDumpFil);
        divaTable.parse();
        System.out.println("Antal poster i rådata: " + divaTable.nrRows() );


        Record record = new Record(); // swepub record type

*/







    }



}
