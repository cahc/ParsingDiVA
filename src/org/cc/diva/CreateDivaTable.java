package org.cc.diva;
import com.opencsv.CSVParser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by crco0001 on 5/11/2016.
 */

/*

  create a reduced table from DiVA csv all metadata version 2 for (1) saving to excel or (2) as a basic data container for further processing

 */

public class CreateDivaTable {

    private List<String[]> rowsInTable = new ArrayList<>();
    private File csvAllMetadataVer2;

    public CreateDivaTable(File file) {

        this.csvAllMetadataVer2 = file;

        if (!csvAllMetadataVer2.exists()) {
            System.out.println("divaDump file don't exist!");
            System.exit(1);
        }

    }


    public void parse() throws IOException {

       // BufferedReader reader = new BufferedReader(new FileReader(csvAllMetadataVer2));
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvAllMetadataVer2), StandardCharsets.UTF_8));
        CSVParser parser = new CSVParser();

        String raw;
        String[] line = null;
        boolean firstRow = true;
        int lineNr = 1;
        while ((raw = reader.readLine()) != null) {

            //clean broken DiVA csv

            raw = raw.replaceAll("\\\"\"","");

            try {
                line = parser.parseLine(raw);
            } catch (IOException e) {

                System.out.println("This line could not be parsed..");
                System.out.println();
                System.out.println(raw);
                System.exit(1);

            }

            //check if the file is a csvAllMetadataVer2
            if(firstRow) {


                //remove BOM - Byte Order Mark if present
                String PID = line[FullDivAColumnIndices.PID.getValue()];
                if(PID.charAt(0)=='\uFEFF') PID = PID.substring(1);


                if(line.length != 68 || !("PID".equals( PID )) || !("Contributor".equals( line[FullDivAColumnIndices.Contributor.getValue()]))  ) {

                    System.out.println("Not a valid csvAllMetadataVer2 file! Aborting"); {

                        System.out.println("Header contains " + line.length + " variables");
                        System.out.println("First variable is " + PID);
                        System.out.println("Last variable is " + line[FullDivAColumnIndices.Contributor.getValue()]);

                        System.exit(1);
                    }
                }

                firstRow = false;
                continue;
            }

            String[] reducedColumns = new String[ ReducedDiVAColumnIndices.values().length];

            if(line.length < 66 || line.length > 68) {System.out.println("Error in file on line " + lineNr + " Aborting!"); System.exit(1); }

            reducedColumns[ReducedDiVAColumnIndices.PID.getValue()] = line[FullDivAColumnIndices.PID.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.Name.getValue()] = line[FullDivAColumnIndices.Name.getValue()];
            //clean clean title
            reducedColumns[ReducedDiVAColumnIndices.Title.getValue()] = DivaHelpFunctions.stripHtmlTags( line[FullDivAColumnIndices.Title.getValue()] );
            reducedColumns[ReducedDiVAColumnIndices.PublicationType.getValue()] = line[FullDivAColumnIndices.PublicationType.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.ContentType.getValue()] = line[FullDivAColumnIndices.ContentType.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.Language.getValue()] = line[FullDivAColumnIndices.Language.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.Journal.getValue()] = line[FullDivAColumnIndices.Journal.getValue()];

            reducedColumns[ReducedDiVAColumnIndices.JournalISSN.getValue()] = line[FullDivAColumnIndices.JournalISSN.getValue()];

            String eissn1 =  line[FullDivAColumnIndices.JournalEISSN.getValue()];

            if(eissn1.length() > 1) {

             String issn =  reducedColumns[ReducedDiVAColumnIndices.JournalISSN.getValue()];

                if(issn.length() > 1) {  reducedColumns[ReducedDiVAColumnIndices.JournalISSN.getValue()] = issn.concat(";").concat(eissn1); } else {


                    reducedColumns[ReducedDiVAColumnIndices.JournalISSN.getValue()] = eissn1;
                }
            }


            reducedColumns[ReducedDiVAColumnIndices.Status.getValue()] = line[FullDivAColumnIndices.Status.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.Volume.getValue()] = line[FullDivAColumnIndices.Volume.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.Issue.getValue()] = line[FullDivAColumnIndices.Issue.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.HostPublication.getValue()] = line[FullDivAColumnIndices.HostPublication.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.Year.getValue()] = line[FullDivAColumnIndices.Year.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.Edition.getValue()] = line[FullDivAColumnIndices.Edition.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.Pages.getValue()] = line[FullDivAColumnIndices.Pages.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.Publisher.getValue()] = line[FullDivAColumnIndices.Publisher.getValue()];


            reducedColumns[ReducedDiVAColumnIndices.Series.getValue()] = line[FullDivAColumnIndices.Series.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.SeriesISSN.getValue()] = line[FullDivAColumnIndices.SeriesISSN.getValue()];

            String eissn2 = line[FullDivAColumnIndices.SeriesEISSN.getValue()];

            if(eissn2.length() > 1) {

                String issn =  reducedColumns[ReducedDiVAColumnIndices.SeriesISSN.getValue()];

                if(issn.length() > 1) {  reducedColumns[ReducedDiVAColumnIndices.SeriesISSN.getValue()] = issn.concat(";").concat(eissn2); } else {


                    reducedColumns[ReducedDiVAColumnIndices.SeriesISSN.getValue()] = eissn2;
                }
            }




            reducedColumns[ReducedDiVAColumnIndices.ISBN.getValue()] = line[FullDivAColumnIndices.ISBN.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.DOI.getValue()] = line[FullDivAColumnIndices.DOI.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.ISI.getValue()] = line[FullDivAColumnIndices.ISI.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.PMID.getValue()] = line[FullDivAColumnIndices.PMID.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.ScopusId.getValue()] = line[FullDivAColumnIndices.ScopusId.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.NBN.getValue()] = line[FullDivAColumnIndices.NBN.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.Keywords.getValue()] = line[FullDivAColumnIndices.Keywords.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.Categories.getValue()] = line[FullDivAColumnIndices.Categories.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.Notes.getValue()] = line[FullDivAColumnIndices.Notes.getValue()];
            //clean abstract
            reducedColumns[ReducedDiVAColumnIndices.Abstract.getValue()] =  DivaHelpFunctions.stripHtmlTags(line[FullDivAColumnIndices.Abstract.getValue()] );
            reducedColumns[ReducedDiVAColumnIndices.CreatedDate.getValue()] = line[FullDivAColumnIndices.CreatedDate.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.PublicationDate.getValue()] = line[FullDivAColumnIndices.PublicationDate.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.LastUpdated.getValue()] = line[FullDivAColumnIndices.LastUpdated.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.NumberOfAuthors.getValue()] = line[FullDivAColumnIndices.NumberOfAuthors.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.PartOfThesis.getValue()] = line[FullDivAColumnIndices.PartOfThesis.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.PublicationSubtype.getValue()] = line[FullDivAColumnIndices.PublicationSubtype.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.ArticleId.getValue()] = line[FullDivAColumnIndices.ArticleId.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.Reviewed.getValue()] = line[FullDivAColumnIndices.Reviewed.getValue()];
            reducedColumns[ReducedDiVAColumnIndices.FreeFulltext.getValue()] = line[FullDivAColumnIndices.FreeFulltext.getValue()];

           if(line.length >= 68) { reducedColumns[ReducedDiVAColumnIndices.ContributorString.getValue()] = line[FullDivAColumnIndices.Contributor.getValue()]; } else {

               reducedColumns[ReducedDiVAColumnIndices.ContributorString.getValue()] = "";
           }

            this.rowsInTable.add(reducedColumns);

            lineNr++;
        }

        reader.close();

    }

    public String[] getRowInTable(int ind) {


        return this.rowsInTable.get(ind);

    }

    public int nrRows() {
        //i.e number of documents non-zero based
        return this.rowsInTable.size();
    }

    public void saveToExcel(String fileName) {

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Grunddata");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;
      //  int columnIndices = 0;



      //  boolean header = true;

        Row row = sheet.createRow(rowIndices);
        for( ReducedDiVAColumnIndices indices : ReducedDiVAColumnIndices.values()) {

            Cell cell = row.createCell(cellIndices);
            cell.setCellValue(indices.toString());
            cell.setCellStyle(style);
            cellIndices++;

       //     if(header) { System.out.print(indices); header =false; continue; }
       //     System.out.print("\t" + indices);

        }
      //  System.out.println();

        for(String[] s : this.rowsInTable) {
            row = sheet.createRow(++rowIndices);
            cellIndices = -1;
            for(int i=0; i<s.length; i++) {
                Cell cell = row.createCell(++cellIndices);
                cell.setCellValue(s[i]);

                //if(i==0) System.out.print(s[i]);
                //if(i>0)  System.out.print("\t" + s[i]);

            }

          //  System.out.println();



        }


        try (FileOutputStream outputStream = new FileOutputStream(fileName + ".xlsx")) {
            workbook.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
