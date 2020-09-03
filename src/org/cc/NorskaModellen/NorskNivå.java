package org.cc.NorskaModellen;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import org.cc.diva.*;
import org.cc.misc.Thesaurus;

import java.util.List;

import static org.cc.diva.DivaHelpFunctions.extractDivaISBN;
import static org.cc.diva.DivaHelpFunctions.extractISSN;
import static org.cc.diva.DivaHelpFunctions.extractSeriesName;

/**
 * Created by crco0001 on 6/21/2016.
 */
public class NorskNivå {


    private static String problematicSerie1 = DivaHelpFunctions.simplifyString("Studies in Educational Leadership");

    public static NorwegianMatchInfo getNorwegianLevel(List<NorskSerie> serieLista, List<NorskFörlag> förlagList, Post p, double threshold, Thesaurus thesaurus) {

        NorwegianMatchInfo matchInfo = new NorwegianMatchInfo();

        if(p.getStatusInModel().isIgnorerad()) {

            return matchInfo;
        }

        String divaPublikationsTyp = p.getDivaPublicationType();

        //Artikel i i tidskrift kan matachas mot norska listan med ISSN/EISSN eller Tidskriftsnamn

        //konferensbidrag och antologi kan matchas mot norska listan enligt:
        // A) serieISSN eller serie namn --> behandlas som serie
        // B) utgivare eller ISBN --> förlag

        //Bok och redaktörskap matchas mot norska listan med utgivare eller ISBN-prefix ELLER serie, serie har företräde..!!



        if(divaPublikationsTyp.equals(DivaPublicationTypes.bok) || divaPublikationsTyp.equals(DivaPublicationTypes.tidskrift) || divaPublikationsTyp.equals(DivaPublicationTypes.review) || divaPublikationsTyp.equals(DivaPublicationTypes.antologi) || divaPublikationsTyp.equals(DivaPublicationTypes.konferens) || divaPublikationsTyp.equals(DivaPublicationTypes.redaktörskapSamlingsverk) || divaPublikationsTyp.equals(DivaPublicationTypes.redaktörskapProceeding) ) {

            List<String> divaIssn = null;
            if(divaPublikationsTyp.equals(DivaPublicationTypes.tidskrift) || divaPublikationsTyp.equals(DivaPublicationTypes.review)) divaIssn = extractISSN( p.getJournalISSN() );
            if(divaPublikationsTyp.equals(DivaPublicationTypes.bok) || divaPublikationsTyp.equals(DivaPublicationTypes.konferens) || divaPublikationsTyp.equals(DivaPublicationTypes.antologi) || divaPublikationsTyp.equals(DivaPublicationTypes.redaktörskapSamlingsverk) || divaPublikationsTyp.equals(DivaPublicationTypes.redaktörskapProceeding)  ) divaIssn = extractISSN( p.getSeriesISSN() );

            if(divaIssn != null) {

                for(String s : divaIssn) {

                    for(int i=0; i<serieLista.size(); i++) {

                        if(serieLista.get(i).getIssnPrint() != null && serieLista.get(i).getIssnPrint().equals(s) ) {


                            matchInfo.setInNorewgianList(true);
                            p.getStatusInModel().setStatusInModel( StatusInModelConstants.BEAKTAD_PUBLIKATION_I_NORSKA_LISTAN );
                            matchInfo.setType("serie");
                            matchInfo.setHow("issn");
                            matchInfo.setNorsk_id(  serieLista.get(i).getTidskriftsID()  );
                            //get level
                            matchInfo.setNivå( serieLista.get(i).getLevel(  p.getYear() )  );
                            matchInfo.setMax_nivå(  serieLista.get(i).getHistoricalMaxLevel()   );
                            matchInfo.setNorsk_namn( serieLista.get(i).getInternationellTitel() );

                            return matchInfo;



                        }

                        if(serieLista.get(i).getIssnOnline() != null && serieLista.get(i).getIssnOnline().equals(s) ) {
                            matchInfo.setInNorewgianList(true);
                            p.getStatusInModel().setStatusInModel( StatusInModelConstants.BEAKTAD_PUBLIKATION_I_NORSKA_LISTAN );
                            matchInfo.setType("serie");
                            matchInfo.setHow("eissn");
                            matchInfo.setNorsk_id(  serieLista.get(i).getTidskriftsID()  );
                            //get level
                            matchInfo.setNivå( serieLista.get(i).getLevel(  p.getYear() )  );
                            matchInfo.setMax_nivå(  serieLista.get(i).getHistoricalMaxLevel()   );
                            matchInfo.setNorsk_namn( serieLista.get(i).getInternationellTitel() );

                            return matchInfo;

                        }


                    }

                }

            }

            //no hit on ISSN; continue with journal name or with series name

            String simplifiedSeriesName = "";

            if(divaPublikationsTyp.equals(DivaPublicationTypes.review) || divaPublikationsTyp.equals(DivaPublicationTypes.tidskrift)) simplifiedSeriesName = DivaHelpFunctions.simplifyString( thesaurus.replaceSerieBy( p.getJournal() ) );

            if(divaPublikationsTyp.equals(DivaPublicationTypes.bok) || divaPublikationsTyp.equals(DivaPublicationTypes.antologi) || divaPublikationsTyp.equals(DivaPublicationTypes.konferens) || divaPublikationsTyp.equals(DivaPublicationTypes.redaktörskapSamlingsverk) || divaPublikationsTyp.equals(DivaPublicationTypes.redaktörskapProceeding) ) {

                //TODO the order of functions was not right here, missed ; in thesaurus-lookup
               // String serieName = extractSeriesName( thesaurus.replaceSerieBy( p.getSeriesName() ));

                String serieName = thesaurus.replaceSerieBy( extractSeriesName( p.getSeriesName() ) );

                if(serieName != null) simplifiedSeriesName = DivaHelpFunctions.simplifyString( serieName );

            }


            boolean troublesome_series = !problematicSerie1.equals(simplifiedSeriesName);

            if(simplifiedSeriesName.length() > 1 && troublesome_series) {

                NormalizedLevenshtein levenshtein = new NormalizedLevenshtein();

                double max = -1;
                int indice = -1;


                for(int i=0; i<serieLista.size(); i++) {
                    double sim = levenshtein.similarity(simplifiedSeriesName, serieLista.get(i).getSimplifyedTitle() );
                    if(sim > max) {max = sim; indice = i;}
                }

                if(max >= threshold) {
                    matchInfo.setInNorewgianList(true);
                    p.getStatusInModel().setStatusInModel( StatusInModelConstants.BEAKTAD_PUBLIKATION_I_NORSKA_LISTAN );
                    matchInfo.setType("serie");
                    matchInfo.setHow("serie_name");
                    matchInfo.setNorsk_id(  serieLista.get(indice).getTidskriftsID()  );
                    //get level
                    matchInfo.setNivå( serieLista.get(indice).getLevel(  p.getYear() )  );
                    matchInfo.setMax_nivå(  serieLista.get(indice).getHistoricalMaxLevel()   );
                    matchInfo.setNorsk_namn( serieLista.get(indice).getInternationellTitel() );

                    return matchInfo;

                }

            }
        } // if artikel/review/konferens/antologi/Redaktörskap..

        // Still no hit. Try with publisher and ISBN prefix if anthology/conferens or if it is a book

        if(divaPublikationsTyp.equals(DivaPublicationTypes.konferens) || divaPublikationsTyp.equals(DivaPublicationTypes.antologi) || divaPublikationsTyp.equals(DivaPublicationTypes.bok) || divaPublikationsTyp.equals(DivaPublicationTypes.redaktörskapSamlingsverk) || divaPublikationsTyp.equals(DivaPublicationTypes.redaktörskapProceeding)  ) {

            String simplifiedDivaPublisher =  DivaHelpFunctions.simplifyString(  thesaurus.replaceFörlagBy( p.getPublisher() ) );

            if(simplifiedDivaPublisher.length() != 0) {

                NormalizedLevenshtein normalizedLevenshtein = new NormalizedLevenshtein();

                double max = -1;
                int indice = -1;
                for(int i=0; i<förlagList.size(); i++) {
                    double sim = normalizedLevenshtein.similarity(simplifiedDivaPublisher, förlagList.get(i).getSimplyfiedTitle() );
                    if(sim > max) {max = sim; indice = i;}
                }

                if(max >= threshold) {
                    matchInfo.setInNorewgianList(true);
                    p.getStatusInModel().setStatusInModel( StatusInModelConstants.BEAKTAD_PUBLIKATION_I_NORSKA_LISTAN );
                    matchInfo.setType("förlag");
                    matchInfo.setHow("förlag_name");
                    matchInfo.setNorsk_id( förlagList.get(indice).getForlag_id()  );
                    matchInfo.setNivå(  förlagList.get(indice).getLevel( p.getYear() ) );
                    matchInfo.setMax_nivå( förlagList.get(indice).getHistoricalMaxLevel() );
                    matchInfo.setNorsk_namn( förlagList.get(indice).getInternasjonaltittel() );
                    return matchInfo;

                }

            }

            //still no hit, lets try ISBN prefix..


            List<String> divaISBN = extractDivaISBN(  p.getISBN()  ); // convert to ISBN-13 and hyphenate!

            //create a prefix

            if(divaISBN != null) {


                for(int i=0; i< divaISBN.size(); i++) {


                    divaISBN.set(i,   DivaHelpFunctions.createISBNprefix( divaISBN.get(i)  )    );

                }


                for (int i=0; i<förlagList.size(); i++) {

                    List<String> norskaFörlagsISBN = förlagList.get(i).getISBNprefix();
                    if (norskaFörlagsISBN == null) continue;

                    for (String ISBN : norskaFörlagsISBN) {

                        for (String divaisbn : divaISBN) {

                            if (divaisbn.equals(ISBN)) {
                                matchInfo.setInNorewgianList(true);
                                p.getStatusInModel().setStatusInModel( StatusInModelConstants.BEAKTAD_PUBLIKATION_I_NORSKA_LISTAN );
                                matchInfo.setType("förlag");
                                matchInfo.setHow("ISBN_prefix");
                                matchInfo.setNorsk_id(  förlagList.get(i).getForlag_id() );
                                matchInfo.setNivå(  förlagList.get(i).getLevel( p.getYear() ) );
                                matchInfo.setMax_nivå( förlagList.get(i).getHistoricalMaxLevel() );
                                matchInfo.setNorsk_namn( förlagList.get(i).getInternasjonaltittel() );
                                return matchInfo;
                            }

                        }

                    }

                }

            }


        }



        //Beaktad publikation men ej i Norska listan
        p.getStatusInModel().setStatusInModel( StatusInModelConstants.BEAKTAD_PUBLIKATION_EJ_I_NORSKA_LISTAN );
        matchInfo.setInNorewgianList(false);

        return matchInfo;
    }




}
