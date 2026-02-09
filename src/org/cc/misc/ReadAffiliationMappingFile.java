package org.cc.misc;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.cc.NorskaModellen.NorskSerie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Created by crco0001 on 8/7/2017.
 */
public class ReadAffiliationMappingFile {
    //columns
    private final int DIVA_ID = 0;
    private final int STANDARD_SWE = 1;
    private final int STANDARD_ENG = 2;
    private final int AKTIV = 3;
    private final int TYP = 4;
    private final int FAKULTET = 5;
    private final int INSTITUTION_MOTSVARANDE = 6;
    private final int ENHET_MOTSVARANDE_ALT1 = 7;
    private final int ENHET_MOTSVARANDE_ALT2 = 8;
    private final int INFO = 9;
    private final int ANVÄNDVIDFRAKTIONERING = 10;
    private final int ALTERNATIV = 11;
    //private final int INST = 12;
    //private final int FAKULTET = 13;
    private final int KOD = 14;




    public Map<Integer,DivaIDtoNames> parseAffiliationMappingFile(File file) throws IOException {
        DataFormatter formatter = new DataFormatter();
        if (!file.exists()) {
            System.out.println("divaID to AffilObject don't exist. Check file name / path.");
            System.exit(1);
        }


        //Get the workbook instance for XLS file
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(file);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();

        }

        XSSFSheet mappings = workbook.getSheet("mappings");
        if(mappings == null) {System.out.println("Excel-filen måste innehålla en flik med namn \"mappings\""); System.exit(0); }
        System.out.println("# mappings from divaID to AffilObject : " + mappings.getLastRowNum()); // zero indexed

        Iterator<Row> rowIterator = mappings.rowIterator();


        if(! "DIVA.ID".equals( rowIterator.next().getCell(this.DIVA_ID).toString() ) ) {System.out.println("Wrong header in mapping file"); System.exit(0); }


        Map<Integer,DivaIDtoNames> map = new TreeMap<>();

        while(rowIterator.hasNext()) {

            Row row = rowIterator.next();

            DivaIDtoNames divaIDtoNames = new DivaIDtoNames();

            divaIDtoNames.setDivaID( (int)(row.getCell(this.DIVA_ID).getNumericCellValue() ) );
            divaIDtoNames.setStandard_SWE( row.getCell(this.STANDARD_SWE).toString() );
            divaIDtoNames.setStandard_ENG( row.getCell(this.STANDARD_ENG).toString() );
            divaIDtoNames.setAKTIV(  row.getCell(this.AKTIV).toString() );
            divaIDtoNames.setTYP(row.getCell(this.TYP).toString() );
            divaIDtoNames.setFAKULTET(row.getCell(this.FAKULTET).toString() );
            divaIDtoNames.setINSTITUTION(row.getCell(this.INSTITUTION_MOTSVARANDE).toString() );
            divaIDtoNames.setENHET(row.getCell(this.ENHET_MOTSVARANDE_ALT1).toString() );
            divaIDtoNames.setENHET_ALT2(row.getCell(this.ENHET_MOTSVARANDE_ALT2).toString() );
            divaIDtoNames.setINFO(row.getCell(this.INFO).toString() );
            divaIDtoNames.setCONSIDER_WHEN_FRACTIONALISING( "Y".equals(row.getCell(this.ANVÄNDVIDFRAKTIONERING).toString()) ? true : false);
            divaIDtoNames.setALTERNATIVE(  row.getCell(this.ALTERNATIV).toString() );
            divaIDtoNames.setKOD( row.getCell(this.KOD).toString()  );

            map.put( divaIDtoNames.getDivaID(), divaIDtoNames );


        }


        workbook.close();
        return map;

    }

    public Map<Integer,DivaIDtoNames> parseAffiliationMappingFile2026Version(File file) throws IOException {

        //this version expect the new format, with centrumLike units, and cleaned mappingfile, see 20260203
        //this experimental, should replace the original down the road

        DataFormatter formatter = new DataFormatter();
        if (!file.exists()) {
            System.out.println("divaID to AffilObject don't exist. Check file name / path.");
            System.exit(1);
        }


        //Get the workbook instance for XLS file
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(file);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();

        }

        XSSFSheet mappings = workbook.getSheet("mappings");
        if(mappings == null) {System.out.println("Excel-filen måste innehålla en flik med namn \"mappings\""); System.exit(0); }
        System.out.println("# mappings from divaID to AffilObject : " + mappings.getLastRowNum()); // zero indexed

        Iterator<Row> rowIterator = mappings.rowIterator();


        if(! "DIVA.ID".equals( rowIterator.next().getCell(this.DIVA_ID).toString() ) ) {System.out.println("Wrong header in mapping file"); System.exit(0); }


        Map<Integer,DivaIDtoNames> map = new TreeMap<>();

        while(rowIterator.hasNext()) {

            Row row = rowIterator.next();

            DivaIDtoNames divaIDtoNames = new DivaIDtoNames();

            divaIDtoNames.setDivaID( (int)(row.getCell(this.DIVA_ID).getNumericCellValue() ) );
            divaIDtoNames.setStandard_SWE( row.getCell(this.STANDARD_SWE).toString() );
            divaIDtoNames.setStandard_ENG( row.getCell(this.STANDARD_ENG).toString() );
            divaIDtoNames.setAKTIV(  row.getCell(this.AKTIV).toString() );
            divaIDtoNames.setTYP(row.getCell(this.TYP).toString() );
            divaIDtoNames.setFAKULTET(row.getCell(this.FAKULTET).toString() );
            divaIDtoNames.setINSTITUTION(row.getCell(this.INSTITUTION_MOTSVARANDE).toString() );
            divaIDtoNames.setENHET(row.getCell(this.ENHET_MOTSVARANDE_ALT1).toString() );
            divaIDtoNames.setENHET_ALT2(row.getCell(this.ENHET_MOTSVARANDE_ALT2).toString() );
            divaIDtoNames.setINFO(row.getCell(this.INFO).toString() );
            divaIDtoNames.setCONSIDER_WHEN_FRACTIONALISING( "Y".equals(row.getCell(this.ANVÄNDVIDFRAKTIONERING).toString()) ? true : false);
            divaIDtoNames.setALTERNATIVE(  "UNUSED" );
            divaIDtoNames.setKOD( row.getCell(11).toString()  );

            map.put( divaIDtoNames.getDivaID(), divaIDtoNames );


        }


        workbook.close();
        return map;

    }



    public static void main(String[] args) throws IOException {

        ReadAffiliationMappingFile readAffiliationMappingFile = new ReadAffiliationMappingFile();
        Map<Integer,DivaIDtoNames> original = readAffiliationMappingFile.parseAffiliationMappingFile( new File("E:\\2025\\MEDFAK GENOMLYSING\\Mappningsfil20251215.xlsx"));


        Map<Integer,DivaIDtoNames> newFile = readAffiliationMappingFile.parseAffiliationMappingFile2026Version( new File("E:\\2025\\MEDFAK GENOMLYSING\\Mappningsfil20260203.xlsx"));

        System.out.println( newFile.get(739).CONSIDER_WHEN_FRACTIONALISING );
    }

}
