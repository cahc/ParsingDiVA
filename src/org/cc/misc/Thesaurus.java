package org.cc.misc;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by crco0001 on 7/4/2016.
 */
public class Thesaurus {

    public static final String LABEL_COLUMN_HEADER = "label";
    public static final String REPLACE_BY_COLUMN_HEADER = "replace by";
    private File inputfile;
    private HashMap<String, String> förlagThesaurus = new HashMap<>();
    private HashMap<String, String> serieThesaurus = new HashMap<>();


    public Thesaurus(File file) throws IOException {


        if (!file.exists()) {
            System.out.println("thesaurus file don't exist!");
            System.exit(1);
        }


        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(file);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();
            System.out.println("Thesaurus-filen måste vara ett Micosoft Excel-dolument av typen XLSX");
            System.exit(0);
        }


        XSSFSheet förlag = workbook.getSheet("FÖRLAG");
        XSSFSheet serie = workbook.getSheet("SERIE");

        if (förlag == null || serie == null) {

            System.out.println("Thesuarus-filen måste innehålla två flikar namngivna FÖRLAG respektive SERIE");
            workbook.close();
            System.exit(0);
        }


        // läs in förlag thesaurus
        Iterator<Row> rowIterator = förlag.iterator();

        Row row = rowIterator.next();

        if(row.getPhysicalNumberOfCells() != 2) {System.out.println("Filen måste innehåller två kolumner: LABEL respektive REPLACE BY"); workbook.close(); System.exit(0); }

        if (!row.getCell(0).toString().toLowerCase().equals(LABEL_COLUMN_HEADER) || !row.getCell(1).toString().toLowerCase().equals(REPLACE_BY_COLUMN_HEADER)) {
            System.out.println("Filen måste innehåller kolumnnamnen: LABEL respektive REPLACE BY");
            System.exit(0);
        }

        while (rowIterator.hasNext()) {

            row = rowIterator.next();
            if (row.getPhysicalNumberOfCells() != 2) {
                System.out.println("Filen måste innehåller två kolumner: LABEL respektive REPLACE BY");
                workbook.close();
                System.exit(0);
            }

            String  labelColumn = row.getCell(0).toString().toLowerCase().trim();
            String replaceColumn = row.getCell(1).toString().toLowerCase().toLowerCase();

            förlagThesaurus.put(labelColumn,replaceColumn);

        }


        //läs in serier

        rowIterator = serie.iterator();

        row = rowIterator.next();

        if(row.getPhysicalNumberOfCells() != 2) {System.out.println("Filen måste innehåller två kolumner: LABEL respektive REPLACE BY"); workbook.close(); System.exit(0); }

        if (!row.getCell(0).toString().toLowerCase().equals(LABEL_COLUMN_HEADER) || !row.getCell(1).toString().toLowerCase().equals(REPLACE_BY_COLUMN_HEADER)) {
            System.out.println("Filen måste innehåller kolumnnamnen: LABEL respektive REPLACE BY");
            System.exit(0);
        }


        while (rowIterator.hasNext()) {

            row = rowIterator.next();
            if (row.getPhysicalNumberOfCells() != 2) {
                System.out.println("Filen måste innehåller två kolumner: LABEL respektive REPLACE BY");
                workbook.close();
                System.exit(0);
            }

            String  labelColumn = row.getCell(0).toString().toLowerCase().trim();
            String replaceColumn = row.getCell(1).toString().toLowerCase().toLowerCase();

            serieThesaurus.put(labelColumn,replaceColumn);

        }


        workbook.close();
    }

    public int antalFörlagsMappningar() {

        return  förlagThesaurus.size();
    }

    public int antalSerieMappningar() {

        return serieThesaurus.size();
    }

    public String replaceSerieBy(String label) {

        if(label == null) return null;
        if(label.equals("")) return label;

        String replaceBy = this.serieThesaurus.get(label.toLowerCase());

        return replaceBy == null ? label : replaceBy;

    }

    public String replaceFörlagBy(String label) {

        if(label == null) return null;
        if(label.equals("")) return label;

        String replaceBy = this.förlagThesaurus.get(label.toLowerCase());

        return replaceBy == null ? label : replaceBy;

    }


}




