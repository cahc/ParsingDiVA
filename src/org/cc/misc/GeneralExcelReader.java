package org.cc.misc;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by crco0001 on 12/19/2017.
 */
public class GeneralExcelReader {

   private  XSSFWorkbook workbook;

    GeneralExcelReader(String fileName) {


        //Get the workbook instance for XLS file

        try {
            workbook = new XSSFWorkbook(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public int getNrColumns(int cheet) {


        return this.workbook.getSheetAt(cheet).getRow(0).getLastCellNum();

    }


    public List getColumnData(int cheet,int col, boolean ignoreHeader) {

        List<String> columndata = new ArrayList<>();

        XSSFSheet blad = this.workbook.getSheetAt(cheet);

        Iterator<Row> rowIterator = blad.iterator();

        if(ignoreHeader) rowIterator.next();

        while(rowIterator.hasNext()) {

           Row row = rowIterator.next();

            Cell cell = row.getCell(col);
           columndata.add(   cell == null ? "NULL" : cell.toString()   );

        }




        return columndata;

    }



    public static void main(String[] arg) {


        GeneralExcelReader generalExcelReader = new GeneralExcelReader("C:\\Users\\crco0001\\Desktop\\TEKNAT_PRLIM\\Teknat forskare.xlsx");

        System.out.println( generalExcelReader.getNrColumns(0) );

        System.out.println( generalExcelReader.getColumnData(0,2,true));

    }










}
