package org.cc.diva;

import org.cc.NorskaModellen.StatusInModel;
import org.cc.NorskaModellen.NorwegianMatchInfo;
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

    private boolean hasUMUAuthors;


    public boolean isAheadOfPrint() {
        String status = this.rawData[ReducedDiVAColumnIndices.Status.getValue() ];

        return ("aheadofprint".equals( status ));

    }


    public boolean isHasUMUAuthors() {
        return hasUMUAuthors;
    }

    public void setHasUMUAuthors(boolean hasUMUAuthors) {
        this.hasUMUAuthors = hasUMUAuthors;
    }

    public void setDuplicate(boolean bool, int otherPID) {

        isDuplicate = bool;
        duplicateOfPID = otherPID;
    }

    public void setNrAuthors(int nrAuthors) {
        this.nrAuthors = nrAuthors;
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



        //TODO evaluate this change in splitting from 2023-01-18, using non regex if balanced, regex if not
        //String[] authorsAndAddresses = DivaHelpFunctions.splitAuthorInformation(nameFiled);
        String[] authorsAndAddresses = DivaHelpFunctions.splitAuthInformationNonRegexExperimental(nameFiled);

        for (int i = 0; i < authorsAndAddresses.length; i++) {

            Author author = new Author(authorsAndAddresses[i]);
            //author.calculateAndSetFraction( this.nrAuthors ); use this later when we have fixed IDtoDiVAnames mapping in main()
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

    public String getEID() {

        return this.rawData[ReducedDiVAColumnIndices.ScopusId.getValue()];
    }

    public int getPID() {
        return  this.PID;
    }


    public int getNrAuthors() {
        return nrAuthors;
    }


    public String getDivaLanguage() { return  this.rawData[ReducedDiVAColumnIndices.Language.getValue()]; }
    public String getDivaPublicationYear() { return this.rawData[ReducedDiVAColumnIndices.Year.getValue()]; }

    public String getDOI() { return this.rawData[ReducedDiVAColumnIndices.DOI.getValue()]; }
    public String getPMID() { return this.rawData[ReducedDiVAColumnIndices.PMID.getValue()]; }


    public String getDivaPublicationType() {
        return this.rawData[ReducedDiVAColumnIndices.PublicationType.getValue()];
    }

    public String getDivaContentType() {
        return this.rawData[ReducedDiVAColumnIndices.ContentType.getValue()];
    }

    public String getDivaPublicationSubtype() {
        return this.rawData[ReducedDiVAColumnIndices.PublicationSubtype.getValue()];
    }


    public String getHost() {

        return this.rawData[ReducedDiVAColumnIndices.HostPublication.getValue()];
    }

    public String getDivaStatus() {
        return this.rawData[ReducedDiVAColumnIndices.Status.getValue()];
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


