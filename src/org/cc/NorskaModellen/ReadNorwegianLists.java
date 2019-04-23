package org.cc.NorskaModellen;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.cc.NorskaModellen.NorskFörlag;
import org.cc.NorskaModellen.NorskFörlagIndex;
import org.cc.NorskaModellen.NorskSerie;
import org.cc.NorskaModellen.NorskSerieIndex;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by crco0001 on 5/18/2016.
 */
public class ReadNorwegianLists {


    public static List<NorskSerie> parseSeries(File file) throws FileNotFoundException {
        DataFormatter formatter = new DataFormatter();
        if (!file.exists()) {
            System.out.println("norwegianList don't exist. Check file name / path.");
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

        XSSFSheet tidskrifter = workbook.getSheet("Alle tidsskrift");
        if(tidskrifter == null) {System.out.println("Excel-filen måste innehålla en flik med namn \"Alle tidsskrift\""); System.exit(0); }
       // System.out.println("# tidskrifter/serier: " + tidskrifter.getLastRowNum()); // zero indexed

        Iterator<Row> rowIterator = tidskrifter.iterator();

        Row row = rowIterator.next();

        if (!row.getCell(NorskSerieIndex.tidsskrift_id.getValue()).toString().equals("NSD tidsskrift_id") || !row.getCell(NorskSerieIndex.Nivå_2004.getValue()).toString().equals("Nivå 2004")) {
            System.out.println("Not a valid header in norwegian authority file! (serier)");
            System.exit(1);
        }


        // now iterate over the rows and create Series objects
        List<NorskSerie> norskSerieList = new ArrayList<>(32300);

        while (rowIterator.hasNext()) {

            row = rowIterator.next();
            NorskSerie norskSerie = new NorskSerie();


            norskSerie.setTidskriftsID( Integer.valueOf( formatter.formatCellValue( row.getCell(NorskSerieIndex.tidsskrift_id.getValue()) ) ) );
            norskSerie.setOriginalTitel(row.getCell(NorskSerieIndex.Original_tittel.getValue()).toString());
            norskSerie.setInternationellTitel(row.getCell(NorskSerieIndex.Internasjonal_tittel.getValue()).toString());
            //check if active today 2018
            Cell nedlaggd = row.getCell(NorskSerieIndex.Nedlagt.getValue());
            if (nedlaggd == null || nedlaggd.getCellType() != Cell.CELL_TYPE_BLANK ) { /*do something */} else { norskSerie.setNedlaggd(true); }

            //check print issn can be null
            Cell cell = row.getCell(NorskSerieIndex.Print_ISSN.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setIssnPrint(cell.toString());

            //check online issn can be null
            cell = row.getCell(NorskSerieIndex.Online_ISSN.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setIssnOnline(cell.toString());

            //is open access
            cell = row.getCell(NorskSerieIndex.Open_Access.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setDOAJ(true);

            norskSerie.setNpuField(row.getCell(NorskSerieIndex.NPI_Fagfelt.getValue()).toString());

            //check vetenskapsdisciplin
            cell = row.getCell(NorskSerieIndex.Vitenskapsdisipliner.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setDiscipliner(cell.toString());

            //check serietyp
            //cell = row.getCell(NorskSerieIndex.Tidsskrift_type.getValue());
            //if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setSerieType(cell.toString());


            //check Förlag
            cell = row.getCell(NorskSerieIndex.Forlag.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setFörlag(cell.toString());

            //check utgivare
            cell = row.getCell(NorskSerieIndex.Utgiver.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setUtgivare(cell.toString());

            //check språk
            cell = row.getCell(NorskSerieIndex.Språk.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setSpråk(cell.toString());


            //Nivå 2019

            cell = row.getCell(NorskSerieIndex.Nivå_2019.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2019(Integer.valueOf(formatter.formatCellValue(cell)));


            //Nivå 2018

            cell = row.getCell(NorskSerieIndex.Nivå_2018.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2018(Integer.valueOf(formatter.formatCellValue(cell)));


            //Nivå 2017

            cell = row.getCell(NorskSerieIndex.Nivå_2017.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2017(Integer.valueOf(formatter.formatCellValue(cell)));


            //Nivå 2016
            cell = row.getCell(NorskSerieIndex.Nivå_2016.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2016(Integer.valueOf(formatter.formatCellValue(cell)));


            //Nivå 2015
            cell = row.getCell(NorskSerieIndex.Nivå_2015.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2015(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2014
            cell = row.getCell(NorskSerieIndex.Nivå_2014.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2014(Integer.valueOf(formatter.formatCellValue(cell)));


            //Nivå 2013
            cell = row.getCell(NorskSerieIndex.Nivå_2013.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2013(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2012
            cell = row.getCell(NorskSerieIndex.Nivå_2012.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2012(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2011
            cell = row.getCell(NorskSerieIndex.Nivå_2011.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2011(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2010
            cell = row.getCell(NorskSerieIndex.Nivå_2010.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2010(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2009
            cell = row.getCell(NorskSerieIndex.Nivå_2009.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2009(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2008
            cell = row.getCell(NorskSerieIndex.Nivå_2008.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2008(Integer.valueOf(formatter.formatCellValue(cell)));


            //Nivå 2007
            cell = row.getCell(NorskSerieIndex.Nivå_2007.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2007(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2006
            cell = row.getCell(NorskSerieIndex.Nivå_2006.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2006(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2005
            cell = row.getCell(NorskSerieIndex.Nivå_2005.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2005(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2004
            cell = row.getCell(NorskSerieIndex.Nivå_2004.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskSerie.setNivå2004(Integer.valueOf(formatter.formatCellValue(cell)));

            norskSerieList.add(norskSerie);
        }


        return norskSerieList;

    }


    public static List<NorskFörlag> parseFörlag(File file) throws FileNotFoundException {

        DataFormatter formatter = new DataFormatter();
        if (!file.exists()) {
            System.out.println("File don't exist!");
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

        XSSFSheet förlag = workbook.getSheet("Alle forlag");
        if(förlag == null) {System.out.println("Excel-filen måste innehålla en flik med namn\"Alle förlag\""); System.exit(0); }
       // System.out.println("# förlag: " + förlag.getLastRowNum()); // zero indexed


        Iterator<Row> rowIterator = förlag.iterator();

        Row row = rowIterator.next();

        if (!row.getCell(NorskFörlagIndex.forlag_id.getValue()).toString().equals("NSD forlag_id") || !row.getCell(NorskFörlagIndex.Nivå2004.getValue()).toString().equals("Nivå 2004")) {
            System.out.println("Not a valid header in norwegian authority file! (förlag)");
            System.exit(1);
        }

        // now iterate over the rows and create Series objects
        List<NorskFörlag> norskFörlagList = new ArrayList<>(2999);

        while (rowIterator.hasNext()) {

            row = rowIterator.next();
            NorskFörlag norskFörlag = new NorskFörlag();

            norskFörlag.setForlag_id(  Integer.valueOf(formatter.formatCellValue(row.getCell(NorskFörlagIndex.forlag_id.getValue()))) );
            norskFörlag.setOriginaltittel(row.getCell(NorskFörlagIndex.Originaltittel.getValue()).toString());
            norskFörlag.setInternasjonaltittel(row.getCell(NorskFörlagIndex.InternasjonalTittel.getValue()).toString());

            //check if active today 2018
            //Cell nedlaggd = row.getCell(NorskFörlagIndex.Nedlagt.getValue());
            //if (nedlaggd == null || (nedlaggd.getCellType() == Cell.CELL_TYPE_BLANK)) {/*do something */} else { norskFörlag.setNedlagt(true); }

            //check ISBN prefix, 0 or multiple is possible
            Cell isbnPrefixes = row.getCell(NorskFörlagIndex.ISBNprefix.getValue());
            if (isbnPrefixes == null || isbnPrefixes.getCellType() == Cell.CELL_TYPE_BLANK) {
                norskFörlag.setISBNprefix(null);
            } else {

                String[] tempIsbn = isbnPrefixes.toString().split(",");

               //TODO remove useless ISBN prefixes must have must have publisher group, e.g., 978-3-527 (527), that is at least two (2) hypens
                List<String> usableIsbn = new ArrayList<>();

                for(String s : tempIsbn) {

                    int count = s.length() - s.replace("-", "").length();
                    if(count >= 2) usableIsbn.add(s);
                }


                if(usableIsbn.size() > 0) {

                    List<String> isbn = new ArrayList<>(1);
                    for (String s : usableIsbn) {

                        isbn.add(s.trim());

                    }

                    norskFörlag.setISBNprefix(isbn);

                }

            }

            //check land
            Cell cell = row.getCell(NorskFörlagIndex.Land.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setLand(  cell.toString() );



            //Nivå 2019
            cell = row.getCell(NorskFörlagIndex.Nivå2019.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2019(Integer.valueOf(formatter.formatCellValue(cell)));



            //Nivå 2018
            cell = row.getCell(NorskFörlagIndex.Nivå2018.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2018(Integer.valueOf(formatter.formatCellValue(cell)));


            //Nivå 2017
            cell = row.getCell(NorskFörlagIndex.Nivå2017.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2017(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2016
            cell = row.getCell(NorskFörlagIndex.Nivå2016.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2016(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2015
            cell = row.getCell(NorskFörlagIndex.Nivå2015.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2015(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2014
            cell = row.getCell(NorskFörlagIndex.Nivå2014.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2014(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2013
            cell = row.getCell(NorskFörlagIndex.Nivå2013.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2013(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2012
            cell = row.getCell(NorskFörlagIndex.Nivå2012.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2012(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2011
            cell = row.getCell(NorskFörlagIndex.Nivå2011.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2011(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2010
            cell = row.getCell(NorskFörlagIndex.Nivå2010.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2010(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2009
            cell = row.getCell(NorskFörlagIndex.Nivå2009.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2009(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2008
            cell = row.getCell(NorskFörlagIndex.Nivå2008.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2008(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2007
            cell = row.getCell(NorskFörlagIndex.Nivå2007.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2007(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2006
            cell = row.getCell(NorskFörlagIndex.Nivå2006.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2006(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2005
            cell = row.getCell(NorskFörlagIndex.Nivå2005.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2005(Integer.valueOf(formatter.formatCellValue(cell)));

            //Nivå 2004
            cell = row.getCell(NorskFörlagIndex.Nivå2004.getValue());
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) norskFörlag.setNivå2004(Integer.valueOf(formatter.formatCellValue(cell)));


            //finally add object to list

            norskFörlagList.add(norskFörlag);

        }

        return norskFörlagList;
    }

}
