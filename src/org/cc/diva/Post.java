package org.cc.diva;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.cc.NorskaModellen.StatusInModel;
import org.cc.NorskaModellen.NorwegianMatchInfo;
import org.cc.NorskaModellen.Viktning;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by crco0001 on 5/10/2016.
 */
public class Post implements Comparable<Post>{


   boolean isDuplicate = false;
   int duplicateOfPID;

    private NorwegianMatchInfo norskNivå;
    private StatusInModel infoRegardingInclusionInModel;
    private int nrAuthors;
    private String[] rawData;
    private List<Author> authorList = new ArrayList<>();
    private int PID;

    public void setDuplicate(boolean bool, int otherPID) {

        isDuplicate = bool;
        duplicateOfPID = otherPID;
    }

    public int getDuplicateOfPID() {

        if(isDuplicate) return duplicateOfPID;

        return -1;
    }

    public boolean isDuplicate() {

        return isDuplicate;
    }

    public Post(String[] row) {

        this.rawData = row;

        try {
            this.nrAuthors = Integer.valueOf(rawData[ReducedDiVAColumnIndices.NumberOfAuthors.getValue()]);

            if(this.nrAuthors == 0) {

                System.out.println("Warning! number of authors for PID " + rawData[ReducedDiVAColumnIndices.PID.getValue()] +" is set to zero. Will assume 1 author.. ");

                this.nrAuthors = 1;
            }

        } catch (NumberFormatException e) {

            System.out.println("Warning! number of authors for PID " + rawData[ReducedDiVAColumnIndices.PID.getValue()] +" is missing. Will assume 1 author.. ");

            this.nrAuthors = 1; //In rare cases there are no author information available (consortium etc). Set author to 1 and proceed..
        }




        String nameFiled = this.rawData[ReducedDiVAColumnIndices.Name.getValue()];

        String[] authorsAndAddresses = DivaHelpFunctions.splitAuthorInformation(nameFiled);

        for (int i = 0; i < authorsAndAddresses.length; i++) {

            Author author = new Author(authorsAndAddresses[i]);
            author.calculateAndSetFraction( this.nrAuthors );
            author.setPID( Integer.valueOf(rawData[ReducedDiVAColumnIndices.PID.getValue()]) );
            author.setEnclosingPost(this);
            authorList.add(author);

        }


        this.PID = Integer.valueOf(rawData[ReducedDiVAColumnIndices.PID.getValue()]);
    }


    public Post() {

    }

    public String[] getRawDataRow() {

        return this.rawData;
    }

    public void setNorskNivå(NorwegianMatchInfo i) {

        this.norskNivå = i;
    }

    public NorwegianMatchInfo getNorskNivå() {

        return this.norskNivå;
    }


    public void setStatusInModel(StatusInModel i) {

        this.infoRegardingInclusionInModel = i;
    }

    public StatusInModel getStatusInModel() {

        return this.infoRegardingInclusionInModel;
    }

    public List<Author> getAuthorList() {

        return this.authorList;
    }

    public Integer getYear() {

        String year = this.rawData[ReducedDiVAColumnIndices.Year.getValue()  ];

        if(DivaHelpFunctions.isInteger(year)) return Integer.valueOf(this.rawData[ReducedDiVAColumnIndices.Year.getValue()] );


        return -99; //MISSING YEAR INFO..!
    }

    public String getISBN() {

        return this.rawData[ReducedDiVAColumnIndices.ISBN.getValue()];
    }

    public String getSeriesISSN() {

        return this.rawData[ReducedDiVAColumnIndices.SeriesISSN.getValue()];
    }


    public String getDivaChannels() {

        //mostly for debugging

        StringBuilder stringBuilder = new StringBuilder();

        String journal = getJournal();

        if(journal.length() >= 3) stringBuilder.append("j:").append( journal );

        String series = getSeriesName();

        if(series.length() >= 3) {

           if(stringBuilder.length() >= 3) { stringBuilder.append(" | ").append("s:").append( getSeriesName() );  } else {   stringBuilder.append("s:").append( getSeriesName() );   }

        }

        String publisher = getPublisher();


        if(publisher.length() >= 3) {

            if(stringBuilder.length() >= 3) { stringBuilder.append(" | ").append("f:").append( publisher );  } else {   stringBuilder.append("f:").append( publisher );   }

        }


        return stringBuilder.toString();

    }


    public String getSeriesName() {

        return this.rawData[ReducedDiVAColumnIndices.Series.getValue()];
    }

    public String getPublisher() {


        return this.rawData[ReducedDiVAColumnIndices.Publisher.getValue()];
    }

    public String getNBN() {

        return this.rawData[ReducedDiVAColumnIndices.NBN.getValue()];
    }

    public String getPartOfThesis() {

        return this.rawData[ReducedDiVAColumnIndices.PartOfThesis.getValue()];
    }


    public String getJournalISSN() {

        return this.rawData[ReducedDiVAColumnIndices.JournalISSN.getValue()];

    }

    public String getTitle() {

        return this.rawData[ReducedDiVAColumnIndices.Title.getValue()];
    }

    public String getJournal() {

        return this.rawData[ReducedDiVAColumnIndices.Journal.getValue()];

    }

    public int getPID() {
        return  this.PID;
    }


    public int getNrAuthors() {
        return nrAuthors;
    }


    public String getDivaPublicationType() {
        return this.rawData[ReducedDiVAColumnIndices.PublicationType.getValue()];
    }

    public String getDivaContentType() {
        return this.rawData[ReducedDiVAColumnIndices.ContentType.getValue()];
    }

    public String getDivaPublicationSubtype() {
        return this.rawData[ReducedDiVAColumnIndices.PublicationSubtype.getValue()];
    }

    public String getDivaStatus() {
        return this.rawData[ReducedDiVAColumnIndices.Status.getValue()];
    }


    public String printMultipleAuthorPerRow() {

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < authorList.size(); i++){

            Author author = authorList.get(i);


            if(!author.getPrintAuthor()) continue; //don't print non considered author


            int nrUmUaddresses = author.getNrUmUaddresses();
            if(nrUmUaddresses == 0) {

                stringBuilder.append(author.printSeveralRowPerAuthorFracMethod1(0)).append("\t").append(getTitle()).append("\t").append(getDivaPublicationType()).append("\n");
            } else {

                for (int j = 0; j < nrUmUaddresses; j++) {

                    stringBuilder.append(author.printSeveralRowPerAuthorFracMethod1(j)).append("\t").append(getTitle()).append("\t").append(getDivaPublicationType()).append("\n");
                }

            }
        }

        return stringBuilder.toString();
    }

    public String printOneAuthorPerRow() {

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < authorList.size(); i++){
            Author author = authorList.get(i);
            if(author.getPrintAuthor()) stringBuilder.append( author.printOneRowPerAuthorFracMethod2() ).append("\t").append(getTitle()).append("\t").append(getDivaPublicationType()).append("\t").append(getStatusInModel().getStatusInModel()).append("\t").append(getNorskNivå()).append("\n");

        }


        return stringBuilder.toString();
    }

    public static void savePostsToExcel(List<Post> postList) {

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Publikationsdata");
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


        try (FileOutputStream outputStream = new FileOutputStream("Result" + ".xlsx")) {
            workbook.write(outputStream);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }




    @Override
    public int compareTo(Post other) {

        if(this.PID > other.PID) return 1;
        if(this.PID < other.PID) return -1;

        return 0; // equal
    }


    @Override
    public String toString() {

        return String.valueOf(this.PID );
    }
}


