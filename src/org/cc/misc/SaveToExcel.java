package org.cc.misc;

import SwePub.Record;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.cc.NorskaModellen.AggregatedAuthorInformation;
import org.cc.NorskaModellen.NorskNivå;
import org.cc.NorskaModellen.NorwegianMatchInfo;
import org.cc.NorskaModellen.PublicationPointPerAuthor;
import org.cc.diva.Author;
import org.cc.diva.Post;
import org.cc.diva.ReducedDiVAColumnIndices;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by crco0001 on 7/12/2016.
 */
public class SaveToExcel {

    XSSFWorkbook workbook = new XSSFWorkbook();


    public SaveToExcel() {}


    public void saveDesanbiguedAuthorFractions(List<Post> recordList) {

        XSSFSheet sheet = workbook.createSheet("AuthorFracs");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        //SKAPA EN LÅST HEADER-RAD
        Row row = sheet.createRow(rowIndices);


        Cell cell = row.createCell(0);
        cell.setCellValue("PID" );
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("AUTHOR_ID" );
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("NAME" );
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("CAS1" );
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("CAS2" );
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("FRACTIONS" );
        cell.setCellStyle(style);

        cell = row.createCell(6);
        cell.setCellValue("FACULTY" );
        cell.setCellStyle(style);

        cell = row.createCell(7);
        cell.setCellValue("INSTITUTION" );
        cell.setCellStyle(style);



        for(Post p : recordList) {

            for(Author author : p.getAuthorList() ) {


                List<DivaIDtoNames> divaIDtoNames = author.getAffilMappingsObjects();
                boolean isUmuAuthor = divaIDtoNames.size() > 0;

                int indice = 0;
                do {
                    row = sheet.createRow(++rowIndices);
                    cellIndices = -1;

                    //PID
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getPID());

                    //AUTHOR_ID"

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getDisambiguateID());


                    //AUTHOR_NAME"

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getAuthorName() );

                    //CAS 1"

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getCas() );

                    //CAS 2"

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getAutomaticAddedCass() );

                    //CAS FRAC"

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getFractionConsiderMultipleUmUAffils() );



                    //FACULTY
                    String faculty = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getFAKULTET() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(faculty);

                    //INST

                    String inst = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getINSTITUTION() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(inst);


                    indice++;
                } while(indice < divaIDtoNames.size() );



            }


        }


        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm") ;


        try (FileOutputStream outputStream = new FileOutputStream("Fractions" + dateFormat.format(date)  +".xlsx")) {
            workbook.setActiveSheet(0);
            workbook.setSelectedTab(0);
            workbook.write(outputStream);
            workbook.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }



    public void saveNorwegianMatchingInfo(List<Post> recordList) {


        XSSFSheet sheet = workbook.createSheet("MatchningsInfo");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        //SKAPA EN LÅST HEADER-RAD
        Row row = sheet.createRow(rowIndices);


        Cell cell = row.createCell(0);
        cell.setCellValue("PID" );
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("DIVA_TYP" );
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("ÅR" );
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("DIVA_INNEHÅLL" );
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("DIVA_SUBTYP" );
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("DIVA_STATUS" );
        cell.setCellStyle(style);

        cell = row.createCell(6);
        cell.setCellValue("DIVA_NAMN_AFFILS" );
        cell.setCellStyle(style);




        cell = row.createCell(7);
        cell.setCellValue("DIVA_NBN" );
        cell.setCellStyle(style);

        cell = row.createCell(8);
        cell.setCellValue("NORSK_ID" );
        cell.setCellStyle(style);


        cell = row.createCell(9);
        cell.setCellValue("NORSK_TYP" );
        cell.setCellStyle(style);

        cell = row.createCell(10);
        cell.setCellValue("MATCHNINGSINFO" );
        cell.setCellStyle(style);

        cell = row.createCell(11);
        cell.setCellValue("NORSK_NAMN" );
        cell.setCellStyle(style);

        cell = row.createCell(12);
        cell.setCellValue("NORSK NIVÅ" );
        cell.setCellStyle(style);

        cell = row.createCell(13);
        cell.setCellValue("NORSK NIVÅ (HISTORISKT MAXIMUM)" );
        cell.setCellStyle(style);


        cell = row.createCell(14);
        cell.setCellValue("MONDELLINFO" );
        cell.setCellStyle(style);

        cell = row.createCell(15);
        cell.setCellValue("DIVA_KANALER" );
        cell.setCellStyle(style);



        cell = row.createCell(16);
        cell.setCellValue("TI" );
        cell.setCellStyle(style);

        cell = row.createCell(17);
        cell.setCellValue("ABSTRACT" );
        cell.setCellStyle(style);

        cell = row.createCell(18);
        cell.setCellValue("TIDSKRIFT" );
        cell.setCellStyle(style);

        cell = row.createCell(19);
        cell.setCellValue("TIDSKRIFT_ISSN" );
        cell.setCellStyle(style);


        cell = row.createCell(20);
        cell.setCellValue("SERIE" );
        cell.setCellStyle(style);

        cell = row.createCell(21);
        cell.setCellValue("SERIE_ISSN" );
        cell.setCellStyle(style);


        cell = row.createCell(22);
        cell.setCellValue("FÖRLAG" );
        cell.setCellStyle(style);


        cell = row.createCell(23);
        cell.setCellValue("FÖRLAG_ISBN" );
        cell.setCellStyle(style);

        cell = row.createCell(24);
        cell.setCellValue("SPRÅK" );
        cell.setCellStyle(style);


        cell = row.createCell(25);
        cell.setCellValue("DIVA_SENAST_ÄNDRAD" );
        cell.setCellStyle(style);

        cell = row.createCell(26);
        cell.setCellValue("NORSK_POÄNG" );
        cell.setCellStyle(style);

        cell = row.createCell(27);
        cell.setCellValue("NR_FÖRFATTARE" );
        cell.setCellStyle(style);

        cell = row.createCell(28);
        cell.setCellValue("DUBBLETT" );
        cell.setCellStyle(style);

        cell = row.createCell(29);
        cell.setCellValue("DUBLETT_PID" );
        cell.setCellStyle(style);


        for(Post p : recordList) {


            row = sheet.createRow(++rowIndices);
            cellIndices = -1;

            //PID
            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getPID());

            //PUBTYPE
            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getDivaPublicationType() );

            //PUBYEAR
            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Year.getValue() ] );

            //CONTENT
            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.ContentType.getValue() ] );

            //CONTENT
            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.PublicationSubtype.getValue() ] );

            //CONTENT
            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Status.getValue() ] );


            cell = row.createCell(++cellIndices);

            String name = p.getRawDataRow()[ ReducedDiVAColumnIndices.Name.getValue() ];

            if(name.length() > 32700 ) { name = name.substring(0,32000); name = name.concat("[TRUNCATED!!]");       }
            cell.setCellValue( name );


            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.NBN.getValue() ] );


            NorwegianMatchInfo norskNivå = p.getNorskNivå();

            cell = row.createCell(++cellIndices);
            cell.setCellValue(norskNivå.getNorsk_id());

            cell = row.createCell(++cellIndices);
            String type = norskNivå.getType();
            cell.setCellValue((type == null) ? "null" : type);

            cell = row.createCell(++cellIndices);
            String how = norskNivå.getHow();
            cell.setCellValue((how == null) ? "null" : how);

            cell = row.createCell(++cellIndices);
            String namn = norskNivå.getNorsk_namn();
            cell.setCellValue((namn == null) ? "null" : namn);

            cell = row.createCell(++cellIndices);
            Integer nivåStandard = norskNivå.getNivå();
            cell.setCellValue((nivåStandard == null) ? "null" : nivåStandard.toString());

            cell = row.createCell(++cellIndices);
            Integer nivåMax = norskNivå.getMax_nivå();
            cell.setCellValue((nivåMax == null) ? "null" : nivåMax.toString());


            cell = row.createCell(++cellIndices);
            cell.setCellValue(     p.getStatusInModel().getStatusInModel()  );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(          p.getDivaChannels() );


            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Title.getValue() ] );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Abstract.getValue() ] );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Journal.getValue() ] );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.JournalISSN.getValue() ] );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Series.getValue() ] );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.SeriesISSN.getValue() ] );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Publisher.getValue() ] );


            p.getISBN();

            cell = row.createCell(++cellIndices);
            cell.setCellValue(  p.getISBN()  );


            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Language.getValue() ] );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.LastUpdated.getValue() ] );


            cell = row.createCell(++cellIndices);

            cell.setCellValue(  norskNivå.getVikt() );


            cell = row.createCell(++cellIndices);

            cell.setCellValue(  p.getNrAuthors() );

            cell = row.createCell(++cellIndices);

            cell.setCellValue(  p.isDuplicate() );

            cell = row.createCell(++cellIndices);

            cell.setCellValue(  p.getDuplicateOfPID() );

        }




        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm") ;


        try (FileOutputStream outputStream = new FileOutputStream("NorskMatchning" + dateFormat.format(date)  +".xlsx")) {
            workbook.setActiveSheet(0);
            workbook.setSelectedTab(0);
            workbook.write(outputStream);
            workbook.close();

        } catch (IOException e) {
            e.printStackTrace();
        }



    }




    public void addFullPublicationData(List<Post> postList) {

        XSSFSheet sheet = workbook.createSheet("Grunddata");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        //SKAPA EN LÅST HEADER-RAD
        Row row = sheet.createRow(rowIndices);
        for( ReducedDiVAColumnIndices indices : ReducedDiVAColumnIndices.values()) {

            Cell cell = row.createCell(cellIndices);
            cell.setCellValue(indices.toString());
            cell.setCellStyle(style);
            cellIndices++;

        }


        for(Post p : postList) {

            row = sheet.createRow(++rowIndices);
            cellIndices = -1;

            String[] rawRow =  p.getRawDataRow();

            for(int i=0; i<rawRow.length; i++) {
                Cell cell = row.createCell(++cellIndices);
                cell.setCellValue(rawRow[i]);

            }

        }


    }

    public void addBibliometricInformation(List<Author> consideredAuthorList) {

        XSSFSheet sheet = workbook.createSheet("PoängPerPublikation");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        ///////////////////SKAPA EN LÅST HEADER-RAD////////////////////////////////
        Row row = sheet.createRow(rowIndices);

        Cell cell = row.createCell(0);
        cell.setCellValue("PID" );
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("CAS" );
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("NAMN" );
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("AFFILIERING" );
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("TITEL" );
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("FÖRFATTARFRAKTION" );
        cell.setCellStyle(style);

        cell = row.createCell(6);
        cell.setCellValue("DIVA_TYP" );
        cell.setCellStyle(style);

        cell = row.createCell(7);
        cell.setCellValue("DIVA_INNEHÅLL" );
        cell.setCellStyle(style);

        cell = row.createCell(8);
        cell.setCellValue("NORSK_TYP" );
        cell.setCellStyle(style);

        cell = row.createCell(9);
        cell.setCellValue("MATCHNINGSINFO" );
        cell.setCellStyle(style);

        cell = row.createCell(10);
        cell.setCellValue("NORSK_ID" );
        cell.setCellStyle(style);

        cell = row.createCell(11);
        cell.setCellValue("NORSK_KANAL" );
        cell.setCellStyle(style);

        cell = row.createCell(12);
        cell.setCellValue("NORSK_NIVÅ(STANDARD)" );
        cell.setCellStyle(style);

        cell = row.createCell(13);
        cell.setCellValue("NORSK_NIVÅ(HISTORISKT MAXIMUM)" );
        cell.setCellStyle(style);


        cell = row.createCell(14);
        cell.setCellValue("PUBLIKATIONSINFO" );
        cell.setCellStyle(style);

        cell = row.createCell(15);
        cell.setCellValue("MODELLINFO" );
        cell.setCellStyle(style);

        cell = row.createCell(16);
        cell.setCellValue("POÄNG_I_MODELL" );
        cell.setCellStyle(style);

        cell = row.createCell(17);
        cell.setCellValue("PUBLICERINGSÅR" );
        cell.setCellStyle(style);

        cell = row.createCell(18);
        cell.setCellValue("DIVA_SUBTYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(19);
        cell.setCellValue("NBN" );
        cell.setCellStyle(style);

        cell = row.createCell(20);
        cell.setCellValue("PART OF THESIS" );
        cell.setCellStyle(style);

        cell = row.createCell(21);
        cell.setCellValue("DIVA CHANNEL INFO" );
        cell.setCellStyle(style);


        for(Author a : consideredAuthorList) {

            boolean publishingAuthor = true;
            Post p = a.getEnclosingPost();
            if(p == null) publishingAuthor = false;

            if(publishingAuthor) {
                NorwegianMatchInfo norskNivå = p.getNorskNivå();

                //set

                row = sheet.createRow(++rowIndices);
                cellIndices = -1;

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getPID());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(a.getCas());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(a.getAuthorName());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(a.getAffiliations());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getTitle());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(a.getFractionIgnoreMultipleUmUAffils());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getDivaPublicationType());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getDivaContentType());

                cell = row.createCell(++cellIndices);
                String type = norskNivå.getType();
                cell.setCellValue((type == null) ? "null" : type);

                cell = row.createCell(++cellIndices);
                String how = norskNivå.getHow();
                cell.setCellValue((how == null) ? "null" : how);

                cell = row.createCell(++cellIndices);
                cell.setCellValue(norskNivå.getNorsk_id());

                cell = row.createCell(++cellIndices);
                String namn = norskNivå.getNorsk_namn();
                cell.setCellValue((namn == null) ? "null" : namn);

                cell = row.createCell(++cellIndices);
                Integer nivåStandard = norskNivå.getNivå();
                cell.setCellValue((nivåStandard == null) ? "null" : nivåStandard.toString());

                cell = row.createCell(++cellIndices);
                Integer nivåMax = norskNivå.getMax_nivå();
                cell.setCellValue((nivåMax == null) ? "null" : nivåMax.toString());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getStatusInModel().getStatusInModel());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(norskNivå.getModelSpecificInfo());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(norskNivå.getVikt());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getYear());

                //todo new
                cell = row.createCell(++cellIndices);

                String subtype = p.getDivaPublicationSubtype();
                if(subtype.length() < 2) subtype = "Standard";

                cell.setCellValue( subtype );
                //cell.setCellValue(p.getDivaPublicationSubtype() );

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getNBN());

                cell = row.createCell(++cellIndices);

                String partOfThesis = p.getPartOfThesis();
                if(partOfThesis.length() <= 2) partOfThesis = "NO";
                cell.setCellValue( partOfThesis );

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getDivaChannels());


            } else {

                //non publishing author

                //set dummies

                row = sheet.createRow(++rowIndices);
                cellIndices = -1;

                cell = row.createCell(++cellIndices);
                cell.setCellValue( -99 );

                cell = row.createCell(++cellIndices);
                cell.setCellValue(a.getCas());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(a.getAuthorName());

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue(0);

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);

                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue(-99);

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);

                cell.setCellValue("null");

                cell = row.createCell(++cellIndices);
                cell.setCellValue("null");

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue(0);

                cell = row.createCell(++cellIndices);
                cell.setCellValue(-99);






            }


        }


        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
       // sheet.autoSizeColumn(3);
       // sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);
        sheet.autoSizeColumn(8);
        sheet.autoSizeColumn(9);
        sheet.autoSizeColumn(10);
        sheet.autoSizeColumn(11);
        sheet.autoSizeColumn(12);
        sheet.autoSizeColumn(13);
        sheet.autoSizeColumn(14);
        sheet.autoSizeColumn(15);
        sheet.autoSizeColumn(16);
        sheet.autoSizeColumn(17);

    }


    public void addAggregatedAuthorStatisticsHumantistiskaFakulteten(PublicationPointPerAuthor sumAuthorWeights) {
        XSSFCellStyle decimalStyle = workbook.createCellStyle();
        decimalStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0"));

        XSSFSheet sheet = workbook.createSheet("PoängPerSökande");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        //SKAPA EN LÅST HEADER-RAD
        Row row = sheet.createRow(rowIndices);

        Cell cell = row.createCell(0);
        cell.setCellValue("SÖKANDE" );
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("ANTAL POSTER" );
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("FÖRFATTARFRAKTIONER" );
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("POÄNG TOTALT (A)" );
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("POÄNG TOTALT, MAX 5 ÖVRIGT PER ÅR (B)" );
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("FORSKNINGSTID (C) " );
        cell.setCellStyle(style);

        cell = row.createCell(6);
        cell.setCellValue("FORSKNINGSTIDSVIKT (D)" );
        cell.setCellStyle(style);

        cell = row.createCell(7);
        cell.setCellValue("INDIKATOR (B * D)" );
        cell.setCellStyle(style);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        TreeMap<String,AggregatedAuthorInformation> result = sumAuthorWeights.getAuthorsAggregatedStatistics();



        for(Map.Entry<String,AggregatedAuthorInformation> entry : result.entrySet()) {
            row = sheet.createRow(++rowIndices);
            cellIndices = -1;

            AggregatedAuthorInformation aggregatedAuthorInformation = entry.getValue();


            cell = row.createCell(++cellIndices);
            cell.setCellValue( entry.getKey() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRawSumOfPublications() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRawSumOfFractions() );
            cell.setCellType(Cell.CELL_TYPE_NUMERIC);
            cell.setCellStyle(decimalStyle);


            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRawSumOfWeights() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRestrictedSumOfWeights() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getForskningstidProcent() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getForskningstidsVikt() );

            Double indikator =     ( aggregatedAuthorInformation.getRestrictedSumOfWeights() * aggregatedAuthorInformation.getForskningstidsVikt() );
            cell = row.createCell(++cellIndices);
            cell.setCellValue( indikator );


        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);

    }

    public void addAggregatedAuthorStatisticsLärarHögskolan(PublicationPointPerAuthor sumAuthorWeights) {

        XSSFCellStyle decimalStyle = workbook.createCellStyle();
        decimalStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0"));

        XSSFSheet sheet = workbook.createSheet("PoängPerSökande");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        //SKAPA EN LÅST HEADER-RAD
        Row row = sheet.createRow(rowIndices);

        Cell cell = row.createCell(0);
        cell.setCellValue("SÖKANDE" );
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("ANTAL POSTER" );
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("FÖRFATTARFRAKTIONER" );
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("OVIKTAD POÄNG (A)" );
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("FRAKTIONS- OCH SAMMARBETSVIKTAD POÄNG (B)" );
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("FORSKNINGSTID (C) " );
        cell.setCellStyle(style);

        cell = row.createCell(6);
        cell.setCellValue("FORSKNINGSTIDSVIKT (D)" );
        cell.setCellStyle(style);

        cell = row.createCell(7);
        cell.setCellValue("INDIKATOR (B * D)" );
        cell.setCellStyle(style);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        TreeMap<String,AggregatedAuthorInformation> result = sumAuthorWeights.getAuthorsAggregatedStatistics();



        for(Map.Entry<String,AggregatedAuthorInformation> entry : result.entrySet()) {
            row = sheet.createRow(++rowIndices);
            cellIndices = -1;

            AggregatedAuthorInformation aggregatedAuthorInformation = entry.getValue();


            cell = row.createCell(++cellIndices);
            cell.setCellValue( entry.getKey() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRawSumOfPublications() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRawSumOfFractions() );
            cell.setCellType(Cell.CELL_TYPE_NUMERIC);
            cell.setCellStyle(decimalStyle);


            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRawSumOfWeights() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getFractionalizedPubSum() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getForskningstidProcent() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getForskningstidsVikt() );

            Double indikator =     ( aggregatedAuthorInformation.getFractionalizedPubSum() * aggregatedAuthorInformation.getForskningstidsVikt() );
            cell = row.createCell(++cellIndices);
            cell.setCellValue( indikator );


        }



        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);

    }




    public void saveToFile(String m) {

        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm") ;


        try (FileOutputStream outputStream = new FileOutputStream("Resultat" + dateFormat.format(date) + "-" + m +".xlsx")) {
            workbook.setSheetOrder("PoängPerSökande",0);
            workbook.setSheetOrder("PoängPerPublikation",1);
            workbook.setSheetOrder("Grunddata",2);
            workbook.setActiveSheet(0);
            workbook.setSelectedTab(0);
            workbook.write(outputStream);
            workbook.close();

        } catch (IOException e) {
            e.printStackTrace();
        }



    }




}
