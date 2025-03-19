package org.cc.misc;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.cc.diva.Author;
import org.cc.diva.CreateDivaTable;
import org.cc.diva.Post;
import org.cc.diva.ReducedDiVAColumnIndices;

import java.io.File;

import java.io.IOException;
import java.util.*;

/**
 * Created by crco0001 on 7/4/2016.
 */
public class CAStoTime {

    public static final String CAS_COLUMN_HEADER = "cas";
    public static final String FORSKNINGSTID_BY_COLUMN_HEADER = "%forskningstid";
    private File inputfile;
    private HashMap<String, Double> casForskningstid = new HashMap<>();
    private String[] casArray;
    private HashSet<String> seenCas = new HashSet<>();


    public CAStoTime(File file) throws IOException {
        DataFormatter formatter = new DataFormatter();
        if (!file.exists()) {
            System.out.println("casFile don't exist!");
            System.exit(1);
        }


        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(file);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();
            System.out.println("Filen med CAS och forskningstid måste vara ett Micosoft Excel-dolument av typen XLSX");
            System.exit(0);
        }

        XSSFSheet cas = workbook.getSheet("CAS");

        if(cas == null) {System.out.println("Excel-filen måste innehålla en flik med namn CAS"); workbook.close(); System.exit(0); }

        Iterator<Row> rowIterator = cas.iterator();

        Row row = rowIterator.next();

        if(row.getPhysicalNumberOfCells() != 2) {System.out.println("CAS-filen måste innehålla exakt två kolumner: CAS respektive %FORSKNINGSTID"); workbook.close(); System.exit(0); }

        if (!row.getCell(0).toString().toLowerCase().equals(CAS_COLUMN_HEADER) || !row.getCell(1).toString().toLowerCase().equals(FORSKNINGSTID_BY_COLUMN_HEADER)) {
            System.out.println("CAS-filen måste innehålla kolumnnamnen: CAS respektive %FORSKNINGSTID");
            System.out.println("Nu existerar:");
            System.out.println( row.getCell(0).toString() );
            System.out.println(row.getCell(1).toString() );
            System.exit(1);
        }


        //System.out.println("# CAS i fil: " + cas.getLastRowNum()); // zero indexed

        while (rowIterator.hasNext()) {

            row = rowIterator.next();
            if(row.getPhysicalNumberOfCells() != 2) {System.out.println("cas-filen måste innehåller kolumner: CAS respektive %FORSKNINGSTID"); workbook.close(); System.exit(0); }


            Cell casColumn = row.getCell(0);
            Cell tidColumn = row.getCell(1);

            String CAS = null;
            Double TID = null;
            try {

                CAS =  formatter.formatCellValue(casColumn);
                TID = Double.valueOf( tidColumn.getNumericCellValue() );
                //TID = Double.valueOf( formatter.formatCellValue(tidColumn) );

                if(TID.compareTo(100.0) > 0  || TID.compareTo(0.0) < 0) throw new NumberFormatException("Forskningstid måste uttryckas med ett mellan 0.0 och 100.0");

            } catch (NumberFormatException e) {

                System.out.println("Forskningstid måste uttryckas med ett heltal mellan 0 och 100");
                System.exit(0);

            }


            if(casForskningstid.containsKey(CAS.toLowerCase().trim())) {System.out.println("Dubbletter i filen med CAS/Forskningstid!"); workbook.close(); System.exit(0); }
            casForskningstid.put( CAS.toLowerCase().trim(), TID  );


        }


        casArray = casForskningstid.keySet().toArray( new String[casForskningstid.keySet().size()] );

       workbook.close();

    }

    public void markAuthorsForInclusion(Post p) {

        List<Author> authors = p.getAuthorList();

        for(Author a : authors) {

            Double forskningstid = this.casForskningstid.get(a.getCas());

            if(forskningstid != null) {

                a.setPrintAuthor(    true    );
                a.setForskningsTidProcent(forskningstid);
            }


        }

    }

    public void markAllAuthorsForInclusionByDefault(Post p) {


        List<Author> authors = p.getAuthorList();

        for(Author a : authors) {

            a.setPrintAuthor(    true   );
            a.setForskningsTidProcent(100.0);
        }


    } //dummy



    public boolean ContributorFieldContainsCASDEBUGFUNCTION(CreateDivaTable divaTable, int row) {

        String contributorField =  divaTable.getRowInTable(row)[ReducedDiVAColumnIndices.ContributorString.getValue()].toLowerCase();

        if(contributorField.length() < 2) return false;


        for(int i=0; i<casArray.length; i++) {


            if( contributorField.contains(casArray[i]) ) {  System.out.println("WARNING, CAS: " + casArray[i] +" found in contrib. field för PID: " +divaTable.getRowInTable(row)[ReducedDiVAColumnIndices.PID.getValue()].toLowerCase() ); return true; }


        }

        return false;
    }


    public boolean includeRawPostBasedOnCas(CreateDivaTable divaTable, int row ) {

        boolean include = false;
        String nameField =  divaTable.getRowInTable(row)[ReducedDiVAColumnIndices.Name.getValue()].toLowerCase();

        for(int i=0; i<casArray.length; i++) {


            if( nameField.contains(casArray[i]) ) { seenCas.add(casArray[i]); include = true; } //TODO bugg in implementation 2020, seenCas is used for other stuff later must add all considered cas


        }


        return include;

    }

    public boolean includeRawPostBasedOnCas(String[] rowInTable ) {

        boolean include = false;
        String nameField =  rowInTable[ReducedDiVAColumnIndices.Name.getValue()].toLowerCase();

        for(int i=0; i<casArray.length; i++) {


            if( nameField.contains(casArray[i]) ) { seenCas.add(casArray[i]); include = true; } //TODO bugg in implementation 2020, seenCas is used for other stuff later must add all considered cas


        }


        return include;

    }

    public boolean includeRawPostByDefault(CreateDivaTable divaTable, int row) {

        return true;
    } //dummy


    public int antalCas() {

        return casForskningstid.size();
    }

    public Double getForskningsTid(String cas) {

        return this.casForskningstid.get(cas);

    }
    public String[] getAllCas() {return this.casArray; }

    public ArrayList<String> getCasWithNoPublications() {

        ArrayList<String> noPubs = new ArrayList<>();

        for(int i=0; i<this.casArray.length; i++) {

            if( !seenCas.contains(this.casArray[i])) noPubs.add(this.casArray[i]);

        }

        return noPubs;
    }

}
