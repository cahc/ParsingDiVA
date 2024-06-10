package org.cc.NorskaModellen;

import org.cc.diva.Author;
import org.cc.diva.DeduplicatePostsPerAuthor;

import java.io.UnsupportedEncodingException;
import java.time.Year;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by crco0001 on 7/11/2016.
 */
public class PublicationPointPerAuthor {


    /**
     *
     * WHAT A STUPID IMPLEMENTATION! :-(
     *
     *
     * And duplicate detection is here, rather than on the distinct Posts as a duplicate for one Author object might not translat to a duplicate for all!
     *
     *
     */


    TreeMap<String, AggregatedAuthorInformation> aggregatedAuthorInformationTreeMap = new TreeMap<>();


    public void calculateAggregateAuthorStatistics(List<Author> authors, double collaborationWeight, boolean minAuthorFracOneTenth) throws UnsupportedEncodingException {

        for (Author a : authors) {


            String cas = a.getCas();
            Double publicationWeight = a.getEnclosingPost().getNorskNivå().getVikt();
            Integer year = a.getEnclosingPost().getYear();

            double fraction = minAuthorFracOneTenth ?  a.getFractionIgnoreMultipleUmUAffilsMin01() : a.getFractionConsiderMultipleUmUAffils();
            //double fraction = a.getFractionIgnoreMultipleUmUAffils();

            boolean nrAuthorsMoreThanOne = (a.getEnclosingPost().getNrAuthors() > 1);

            AggregatedAuthorInformation authorStatistics = aggregatedAuthorInformationTreeMap.get(cas);


            if (authorStatistics == null) {

                AggregatedAuthorInformation newAuthorStatistics = new AggregatedAuthorInformation();


                newAuthorStatistics.incrementRawSumOfFractions(fraction);
                newAuthorStatistics.incrementRawSumOfPublications();
                newAuthorStatistics.incrementRawSumOfWeights(publicationWeight);

                newAuthorStatistics.incrementFractionalizedPubSum(  nrAuthorsMoreThanOne ? (fraction*publicationWeight*collaborationWeight) : publicationWeight   );


                //sätt forskningstid en gång
                newAuthorStatistics.setForskningstidProcent(a.getForskningsTidProcent());


                //om det är en post av typ övrigt, uppdatera antalet för det givna året
                if (a.getEnclosingPost().getNorskNivå().getModelSpecificInfo().equals("Övrigt")) {

                    newAuthorStatistics.övrigaPublicationerPerÅr.put(year, 1);
                }

                newAuthorStatistics.incrementRestrictedSumOfWeights(publicationWeight);

                aggregatedAuthorInformationTreeMap.put(cas, newAuthorStatistics);

            } else {

                authorStatistics.incrementRawSumOfFractions(fraction);
                authorStatistics.incrementRawSumOfPublications();
                authorStatistics.incrementRawSumOfWeights(publicationWeight);
                authorStatistics.incrementFractionalizedPubSum(  nrAuthorsMoreThanOne ? (fraction*publicationWeight*collaborationWeight) : publicationWeight   );

                //Om posten är av typ övrigt, kontrollera att det är färre än 5 för det givna året
                if (a.getEnclosingPost().getNorskNivå().getModelSpecificInfo().equals("Övrigt")) {

                    Integer nrFörAktuelltÅr = authorStatistics.övrigaPublicationerPerÅr.get(year);

                    if (nrFörAktuelltÅr == null) {

                        authorStatistics.incrementRestrictedSumOfWeights(publicationWeight);
                        authorStatistics.övrigaPublicationerPerÅr.put(year, 1);

                    } else {

                        if (nrFörAktuelltÅr.compareTo(5) == 0) { /* do nothing */ } else {

                            authorStatistics.incrementRestrictedSumOfWeights(publicationWeight);
                            authorStatistics.övrigaPublicationerPerÅr.put(year, nrFörAktuelltÅr + 1);


                        }


                    }


                } else {
                    authorStatistics.incrementRestrictedSumOfWeights(publicationWeight);

                }

            }

        } //for author in author

    }

    public void addAuthorsWithZeroPub(String cas, Double forskningstid) {

        AggregatedAuthorInformation newAuthorStatistics = new AggregatedAuthorInformation();
        newAuthorStatistics.incrementRawSumOfFractions(0);
        newAuthorStatistics.incrementRawSumOfWeights(0);
        newAuthorStatistics.incrementFractionalizedPubSum(  0  );
        newAuthorStatistics.setForskningstidProcent( forskningstid );
        newAuthorStatistics.incrementRestrictedSumOfWeights(0);
        this.aggregatedAuthorInformationTreeMap.put(cas, newAuthorStatistics);


    }
    public TreeMap<String,AggregatedAuthorInformation> getAuthorsAggregatedStatistics() {

        return this.aggregatedAuthorInformationTreeMap;
    }


    public int nrUniqueAuthors() {

        return aggregatedAuthorInformationTreeMap.size();
    }
}
