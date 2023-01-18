package org.cc.misc;

import SwePub.Record;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
import java.util.*;

/**
 * Created by crco0001 on 7/12/2016.
 */
public class SaveToExcel {

    XSSFWorkbook workbook = new XSSFWorkbook();


    public SaveToExcel() {}


    public void saveDisambiguatedAuthorFractions(List<Post> recordList, boolean includeExternalAuthors) {

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
        cell.setCellValue("FRACTIONS (STANDARD)" );
        cell.setCellStyle(style);


        cell = row.createCell(6);
        cell.setCellValue("FRACTIONS (STANDARD + MIN0.1)" );
        cell.setCellStyle(style);

        cell = row.createCell(7);
        cell.setCellValue("FRACTIONS (NO INTERNAL FRACTIONS)" );
        cell.setCellStyle(style);


        cell = row.createCell(8);
        cell.setCellValue("FRACTIONS (NO INTERNAL FRACTIONS + MIN0.1)" );
        cell.setCellStyle(style);

        cell = row.createCell(9);
        cell.setCellValue("FACULTY FRACTION" );
        cell.setCellStyle(style);




        cell = row.createCell(10);
        cell.setCellValue("FACULTY" );
        cell.setCellStyle(style);

        cell = row.createCell(11);
        cell.setCellValue("INSTITUTION" );
        cell.setCellStyle(style);


        cell = row.createCell(12);
        cell.setCellValue("UNIT (IF AVAILABLE OTHERWISE INST.)" );
        cell.setCellStyle(style);

        //BIBLIOGRAPHIC INFO

        cell = row.createCell(13);
        cell.setCellValue("DIVA_TYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(14);
        cell.setCellValue("DIVA_YEAR" );
        cell.setCellStyle(style);

        cell = row.createCell(15);
        cell.setCellValue("DIVA_CONTENT" );
        cell.setCellStyle(style);

        cell = row.createCell(16);
        cell.setCellValue("DIVA_SUBTYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(17);
        cell.setCellValue("DIVA_STATUS" );
        cell.setCellStyle(style);

        cell = row.createCell(18);
        cell.setCellValue("DIVA_AFFILIATIONS" );
        cell.setCellStyle(style);

        cell = row.createCell(19);
        cell.setCellValue("DIVA_URN" );
        cell.setCellStyle(style);


        cell = row.createCell(20);
        cell.setCellValue("DIVA_POTENTIAL_CHANNELS" );
        cell.setCellStyle(style);

        cell = row.createCell(21);
        cell.setCellValue("DIVA_TITLE" );
        cell.setCellStyle(style);

        cell = row.createCell(22);
        cell.setCellValue("DIVA_ABSTRACT" );
        cell.setCellStyle(style);

        cell = row.createCell(23);
        cell.setCellValue("DIVA_JOURNAL" );
        cell.setCellStyle(style);

        cell = row.createCell(24);
        cell.setCellValue("DIVA_JOURNAL_ISSN" );
        cell.setCellStyle(style);


        cell = row.createCell(25);
        cell.setCellValue("DIVA_SERIES" );
        cell.setCellStyle(style);


        cell = row.createCell(26);
        cell.setCellValue("DIVA_SERIES_ISSN" );
        cell.setCellStyle(style);

        cell = row.createCell(27);
        cell.setCellValue("DIVA_PUBLISHER" );
        cell.setCellStyle(style);

        cell = row.createCell(28);
        cell.setCellValue("DIVA_ISBN" );
        cell.setCellStyle(style);


        cell = row.createCell(29);
        cell.setCellValue("DIVA_LANGUAGE" );
        cell.setCellStyle(style);

        cell = row.createCell(30);
        cell.setCellValue("DIVA_LAST_CHANGED" );
        cell.setCellStyle(style);

        cell = row.createCell(31);
        cell.setCellValue("DIVA_NR_AUTHORS" );
        cell.setCellStyle(style);

        cell = row.createCell(32);
        cell.setCellValue("DIVA_IS_DUPLICATED" );
        cell.setCellStyle(style);

        cell = row.createCell(33);
        cell.setCellValue("DIVA_DUPLICATED_PID" );
        cell.setCellStyle(style);

        //NORSK STUFF


        cell = row.createCell(34);
        cell.setCellValue("NORWEGIAN_ID" );
        cell.setCellStyle(style);


        cell = row.createCell(35);
        cell.setCellValue("NORWEGIAN_TYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(36);
        cell.setCellValue("NORWEGIAN_MATCH" );
        cell.setCellStyle(style);


        cell = row.createCell(37);
        cell.setCellValue("NORWEGIAN_NAME" );
        cell.setCellStyle(style);

        cell = row.createCell(38);
        cell.setCellValue("NORWEGIAN_LEVEL" );
        cell.setCellStyle(style);

        cell = row.createCell(39);
        cell.setCellValue("NORWEGIAN_LEVEL (HISTORICAL MAXIMUM)" );
        cell.setCellStyle(style);

        cell = row.createCell(40);
        cell.setCellValue("NORWEGIAN_MODEL_INFO" );
        cell.setCellStyle(style);

        cell = row.createCell(41);
        cell.setCellValue("NORWEGIAN_POINTS" );
        cell.setCellStyle(style);

        //add localID, use to manually remap institutionen för geografi och ekonomisk historia

        cell = row.createCell(42);
        cell.setCellValue("LOCAL_ID");
        cell.setCellStyle(style);

        //add info if the affiliation has been altered, i.e., non considered removed (0=no, 1=yes, 2=count not remove, only non considered

        cell = row.createCell(43);
        cell.setCellValue("AFFIL_MAPPING_INFO");
        cell.setCellStyle(style);


        for(Post p : recordList) {

            for(Author author : p.getAuthorList() ) {


                List<DivaIDtoNames> divaIDtoNames = author.getAffilMappingsObjects();
                boolean isUmuAuthor = divaIDtoNames.size() > 0;

                if(!includeExternalAuthors && !isUmuAuthor) continue;


                NorwegianMatchInfo norskNivå = p.getNorskNivå();
                
                
                //
                //Count number of FACULTIES an author is associated with. This is needed for fractionalization that dont d´split an author fraction over faculties but split on inst/unit on a lower level
                //
                
                Map<String, Integer> facultyToOccurance = null;
                boolean multipleUmUaffils = (divaIDtoNames.size() > 1);
                
                if(multipleUmUaffils) {
                    
                    facultyToOccurance = new HashMap<>();
                    
                    for(DivaIDtoNames id : divaIDtoNames) {
                        
                        String facultyName = id.getFAKULTET();
                        
                        Integer occurrence = facultyToOccurance.get(facultyName);
                        if(occurrence == null) {
                            
                            facultyToOccurance.put(facultyName,1);
                        } else {
                            
                            facultyToOccurance.put(facultyName, (occurrence+1) );
                            
                        }
                        
                        
                        
                    }//for ends
                    
                    
                }
                
                
                
                ////End special treatment for non frac over Faculties
                

                int indice = 0;
                do {
                    row = sheet.createRow(++rowIndices);
                    cellIndices = -1;

                    //PID 1
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getPID());

                    //AUTHOR_ID" 2

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getDisambiguateID());


                    //AUTHOR_NAME" 3

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getAuthorName() );

                    //CAS 1" 4

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getCas() );

                    //CAS 2" 5

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getAutomaticAddedCass() );

                    //CAS FRAC STANDARD 6

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getFractionConsiderMultipleUmUAffils() );

                    // NEW NEW 7
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getFractionConsiderMultipleUmUAffilsMin01() );

                    //NEW 2022-11-16, fractions ignoring internal, and no min01

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getFractionIgnoreMultipleUmUAffils() );


                    //CAS FRAC Min.01 AND Ignore multiple UmU-affils 8

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getFractionIgnoreMultipleUmUAffilsMin01() );


                    //ignore multiple UmU-FACULTIES but fractionalize within!

                    double faculty_special_frac = -1;
                    if(isUmuAuthor && multipleUmUaffils) {

                      faculty_special_frac =  author.getFractionIgnoreMultipleUmUAffils();
                      int occurances = facultyToOccurance.get( divaIDtoNames.get(indice).getFAKULTET() );
                      faculty_special_frac =  faculty_special_frac/occurances;


                    } else {

                        faculty_special_frac = author.getFractionIgnoreMultipleUmUAffils();
                    }

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( faculty_special_frac );





                    //FACULTY 8
                    String faculty = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getFAKULTET() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(faculty);

                    //INST 9

                    String inst = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getINSTITUTION() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(inst);


                    //INST 10 UNIT

                    String unit = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getENHET() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(unit);

                    //SET DIVA BIBLIOGRAPHIC STUFF

                    //DIVA TYPE 11
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaPublicationType()  );


                    //DIVA YEAR 11
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getYear()  );

                    //DIVA CONTENT 12
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaContentType()  );

                    //DIVA SUBTYPE 13
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaPublicationSubtype()  );

                    //DIVA status 14
                    cell = row.createCell(++cellIndices);

                    String status = p.getDivaStatus();

                    if("".equals(status)) {status = "published";}

                    cell.setCellValue( status );

                    //DIVA AFFILIATIONS 15
                    cell = row.createCell(++cellIndices);

                    String name = p.getRawDataRow()[ ReducedDiVAColumnIndices.Name.getValue() ];
                    if(name.length() > 32700 ) { name = name.substring(0,32000); name = name.concat("[TRUNCATED!!]");       }
                    cell.setCellValue( name );

                    //DIVA URN 16

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getNBN()  );

                    //DIVA CHANEL 17

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaChannels()  );

                    //DIVA TITLE 18

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getTitle()  );

                    //DIVA ABSTRACT 19

                    cell = row.createCell(++cellIndices);

                    String summary = p.getRawDataRow()[ ReducedDiVAColumnIndices.Abstract.getValue() ];
                    if(summary.length() > 32700 ) { summary = summary.substring(0,32000); summary = summary.concat("[TRUNCATED!!]");       }
                    cell.setCellValue( summary );

                    //DIVA JOURNAL 20

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getJournal()  );

                    //DIVA JOURNAL ISSN 21

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getJournalISSN()  );


                    //DIVA SERIES 22

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getSeriesName()  );


                    //DIVA SERIES ISSN 23

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getSeriesISSN()  );

                    //DIVA PUBLISHER 24

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getPublisher()  );

                    //DIVA PUBLISHER 24

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getISBN()  );



                    //DIVA LANGUAGE 25

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Language.getValue() ] );

                    //DIVA LAST UPDATED  26

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.LastUpdated.getValue() ] );

                    //DIVA NR AUTHORS 27
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getNrAuthors());


                    //DIVA IS DUPE 28
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(  p.isDuplicate() );


                    //DIVA WHICH DUPE 29
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(  p.getDuplicateOfPID() );


                    //SET NORWEGIAN STUFF

                    //NORWEGIAN ID 30

                    cell = row.createCell(++cellIndices);

                    Integer nid = norskNivå.getNorsk_id();
                    if(nid.equals(-99)) { cell.setCellValue( "not available" ); } else {cell.setCellValue( nid ); }


                    //NORWEGIAN TYPE 31

                    cell = row.createCell(++cellIndices);
                    String type = norskNivå.getType();
                    cell.setCellValue((type == null) ? "not available" : type);

                    //NOWRWEGIAN MATCH 32

                    cell = row.createCell(++cellIndices);
                    String how = norskNivå.getHow();
                    cell.setCellValue((how == null) ? "not available" : how);

                    //NOWRWEGIAN NAME 33

                    cell = row.createCell(++cellIndices);
                    String namn = norskNivå.getNorsk_namn();
                    cell.setCellValue((namn == null) ? "not available" : namn);

                    //NORWEGIAN LEVEL 34

                    cell = row.createCell(++cellIndices);
                    Integer nivåStandard = norskNivå.getNivå();
                    cell.setCellValue((nivåStandard == null) ? "not available" : nivåStandard.toString());

                    //NORWEGIAN LEVEL 35

                    cell = row.createCell(++cellIndices);
                    Integer nivåMax = norskNivå.getMax_nivå();
                    cell.setCellValue((nivåMax == null) ? "not available" : nivåMax.toString());


                    //cell = row.createCell(++cellIndices);
                  //  cell.setCellValue(     p.getStatusInModel().getStatusInModel()  );

                    //NORWEGIAN MODEL SPECIFIC INFO 36
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(     norskNivå.getModelSpecificInfo() );


                    //NORWEGIAN POINTS 37

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(      norskNivå.getVikt()     );


                    //add localID

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.LocalId.getValue() ] );

                    //affil mapping info
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.removedNonConsidered );


                    indice++;
                } while(indice < divaIDtoNames.size() );



            }


        }


        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm") ;


        String fileName;

        if(includeExternalAuthors) {fileName = "FractionsIncludedExternal"; } else {fileName = "FractionsExcludedExternal";}


        try (FileOutputStream outputStream = new FileOutputStream(fileName + dateFormat.format(date)  +".xlsx")) {
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


        //model specific info
        cell = row.createCell(15);
        cell.setCellValue("MODELLINFO2" );
        cell.setCellStyle(style);


        cell = row.createCell(16);
        cell.setCellValue("DIVA_KANALER" );
        cell.setCellStyle(style);



        cell = row.createCell(17);
        cell.setCellValue("TI" );
        cell.setCellStyle(style);

        cell = row.createCell(18);
        cell.setCellValue("ABSTRACT" );
        cell.setCellStyle(style);

        cell = row.createCell(19);
        cell.setCellValue("TIDSKRIFT" );
        cell.setCellStyle(style);

        cell = row.createCell(20);
        cell.setCellValue("TIDSKRIFT_ISSN" );
        cell.setCellStyle(style);


        cell = row.createCell(21);
        cell.setCellValue("SERIE" );
        cell.setCellStyle(style);

        cell = row.createCell(22);
        cell.setCellValue("SERIE_ISSN" );
        cell.setCellStyle(style);


        cell = row.createCell(23);
        cell.setCellValue("FÖRLAG" );
        cell.setCellStyle(style);


        cell = row.createCell(24);
        cell.setCellValue("FÖRLAG_ISBN" );
        cell.setCellStyle(style);

        cell = row.createCell(25);
        cell.setCellValue("SPRÅK" );
        cell.setCellStyle(style);


        cell = row.createCell(26);
        cell.setCellValue("DIVA_SENAST_ÄNDRAD" );
        cell.setCellStyle(style);

        cell = row.createCell(27);
        cell.setCellValue("NORSK_POÄNG" );
        cell.setCellStyle(style);

        cell = row.createCell(28);
        cell.setCellValue("NR_FÖRFATTARE" );
        cell.setCellStyle(style);

        cell = row.createCell(29);
        cell.setCellValue("DUBBLETT" );
        cell.setCellStyle(style);

        cell = row.createCell(30);
        cell.setCellValue("DUBLETT_PID" );
        cell.setCellStyle(style);

        cell = row.createCell(31);
        cell.setCellValue("NBN" );
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
            cell.setCellValue(     p.getNorskNivå().getModelSpecificInfo() );


            cell = row.createCell(++cellIndices);
            cell.setCellValue(          p.getDivaChannels() );


            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Title.getValue() ] );

            cell = row.createCell(++cellIndices);

            String abs = p.getRawDataRow()[ ReducedDiVAColumnIndices.Abstract.getValue() ];
            if(abs.length() > 32700 ) { abs = abs.substring(0,32000); abs = abs.concat("[TRUNCATED!!]");       }
            cell.setCellValue( abs );

           // cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Abstract.getValue() ] );

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

            cell = row.createCell(++cellIndices);

            cell.setCellValue(  p.getNBN() );


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
            cell.setCellType(CellType.NUMERIC);
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
            cell.setCellType(CellType.NUMERIC);
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



    public void saveDumpOnly(String path) {

        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm") ;


        try (FileOutputStream outputStream = new FileOutputStream(path +"\\dump.xlsx")) {
            workbook.setSheetOrder("Grunddata",0);
            workbook.setActiveSheet(0);
            workbook.setSelectedTab(0);
            workbook.write(outputStream);
            workbook.close();

        } catch (IOException e) {
            e.printStackTrace();
        }



    }




}
