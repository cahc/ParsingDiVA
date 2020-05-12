package org.cc.NorskaModellen;

import com.sun.org.apache.xpath.internal.operations.Div;
import org.cc.diva.Post;
import org.cc.diva.DivaPublicationTypes;

/**
 * Created by crco0001 on 7/6/2016.
 */
public class Viktning {

    /**
     * implementerar olika viktningsscheman
     */

    public static void DefaultWeightning(Post p) {


        String publicationsStatus = p.getStatusInModel().getStatusInModel();

        if (StatusInModelConstants.BEAKTAD_PUBLIKATION_I_NORSKA_LISTAN.equals(publicationsStatus)) {


            NorwegianMatchInfo matchInfo = p.getNorskNivå();

            if ("serie".equals(matchInfo.getType())) {


                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(1);
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(3);
                }

            }


            if ("förlag".equals(matchInfo.getType()) && (p.getDivaPublicationType().equals(DivaPublicationTypes.antologi) || p.getDivaPublicationType().equals(DivaPublicationTypes.konferens))) {


                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(0.7);
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(1);
                }

            }


            if ("förlag".equals(matchInfo.getType()) && p.getDivaPublicationType().equals(DivaPublicationTypes.bok)) {

                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(5);
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(8);
                }

            }

        } else {
            //ej i norska listan
            NorwegianMatchInfo matchInfo = p.getNorskNivå();
            matchInfo.setVikt(0);
        }


    }


    public static void HFWeighting(Post p) {

        String publicationsStatus = p.getStatusInModel().getStatusInModel();
        NorwegianMatchInfo matchInfo = p.getNorskNivå();

        if(p.getPID() == 1068229) System.out.println( p.getPID() + " --->" + publicationsStatus + " --> " + p.getDivaPublicationType() );

        if (publicationsStatus.equals(StatusInModelConstants.BEAKTAD_PUBLIKATION_EJ_I_NORSKA_LISTAN) && (p.getDivaPublicationType().equals(DivaPublicationTypes.avhandlingMonografi) || p.getDivaPublicationType().equals(DivaPublicationTypes.avhandlingSammanläggning))) {


            matchInfo.setVikt(5);
            matchInfo.setModelSpecificInfo("Doktorsavhandling (endast för urvalsgrupp nydisp.)");

        } else

        if (publicationsStatus.equals(StatusInModelConstants.BEAKTAD_PUBLIKATION_I_NORSKA_LISTAN)) {

            if (p.getDivaPublicationType().equals(DivaPublicationTypes.bok)) {

                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Monografi ej nivåbestämd");
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Monografi nivå 0");
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(5);
                    matchInfo.setModelSpecificInfo("Monografi nivå 1");
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(8);
                    matchInfo.setModelSpecificInfo("Monografi nivå 2");
                }


            } else if (p.getDivaPublicationType().equals(DivaPublicationTypes.tidskrift) || p.getDivaPublicationType().equals(DivaPublicationTypes.review)) {

                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Artikel ej nivåbestämd");
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Artikel nivå 0");
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(1);
                    matchInfo.setModelSpecificInfo("Artikel nivå 1");
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(4);
                    matchInfo.setModelSpecificInfo("Artikel nivå 2");
                }

            } else if (p.getDivaPublicationType().equals(DivaPublicationTypes.antologi)) {

                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Bokkap. ej nivåbestämd");
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Bokkap. nivå 0");
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(1);
                    matchInfo.setModelSpecificInfo("Bokkap. nivå 1");
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(2);
                    matchInfo.setModelSpecificInfo("Bokkap. nivå 2");
                }


            } else if(p.getDivaPublicationType().equals(DivaPublicationTypes.redaktörskapSamlingsverk) || p.getDivaPublicationType().equals(DivaPublicationTypes.redaktörskapProceeding) ) {

                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Redaktörskap (Proceeding/Samlingsverk) ej nivåbestämd");
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Redaktörskap (Proceeding/Samlingsverk) nivå 0");
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(1);
                    matchInfo.setModelSpecificInfo("Redaktörskap (Proceeding/Samlingsverk) nivå 1");
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(2);
                    matchInfo.setModelSpecificInfo("Redaktörskap (Proceeding/Samlingsverk) nivå 2");
                }



            }

        }
        //FANNS EJ I NORSKA LISTAN
        else if (publicationsStatus.equals(StatusInModelConstants.BEAKTAD_PUBLIKATION_EJ_I_NORSKA_LISTAN)) {


            if (p.getDivaPublicationType().equals(DivaPublicationTypes.bok)) {

                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Monografi ej i norska listan");
                }
                if (p.getDivaPublicationType().equals(DivaPublicationTypes.tidskrift) || p.getDivaPublicationType().equals(DivaPublicationTypes.review)) {

                        matchInfo.setVikt(0);
                        matchInfo.setModelSpecificInfo("Artikel ej i norska listan");
                    }
                    if (p.getDivaPublicationType().equals(DivaPublicationTypes.antologi)) {

                            matchInfo.setVikt(0);
                            matchInfo.setModelSpecificInfo("Bokkap. ej i norska listan");
                        }


                         if (p.getDivaPublicationType().equals(DivaPublicationTypes.redaktörskapProceeding) || p.getDivaPublicationType().equals(DivaPublicationTypes.redaktörskapSamlingsverk )) {

                            matchInfo.setVikt(0);
                            matchInfo.setModelSpecificInfo("Redaktörskap (Proceeding/Samlingsverk) ej i norska listan");
                    }


        }

        //EJ BEAKTAD PUBLIKATIONSTYP
        else if (publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EJ_BEAKTAD_PUBLIKATIONSTYP)) {

                        matchInfo.setVikt(0);
                        matchInfo.setModelSpecificInfo("Övrigt");

                    } else

            if(publicationsStatus.equals(StatusInModelConstants.IGNORERAD_ABSTRACT_POSTER_ELLER_PRESENTATION) || publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EDITORIAL_ABSTRACT_OR_NEWS_ITEM) || publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EJ_PUBLICERAD) || publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EJ_VETENSKAPLIGT) ) {

                matchInfo.setVikt(0);
                String modelInfoString = "null";
                if( publicationsStatus.equals(StatusInModelConstants.IGNORERAD_ABSTRACT_POSTER_ELLER_PRESENTATION) ) modelInfoString = "EJ VETENSKAPLIG SUBTYP (ABSTRACT,POSTER,PRESENTATION)";

                if( publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EDITORIAL_ABSTRACT_OR_NEWS_ITEM) ) modelInfoString = "EJ VETENSKAPLIG SUBTYP (EDITORIAL,ABSTRACT,NEWS ITEM)";

                if( publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EJ_PUBLICERAD) ) modelInfoString = "EJ PUBLICERAD";


                if( publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EJ_VETENSKAPLIGT) ) modelInfoString = "EJ VETENSKAPLIG INNEHÅLLSTYP";

                matchInfo.setModelSpecificInfo(modelInfoString);

                } else {

                matchInfo.setVikt(-99);
                matchInfo.setModelSpecificInfo("ERROR IN WEIGHTING SCHEMA!!!");
            }

            }


    public static void LHWeighting(Post p) {



        String publicationsStatus = p.getStatusInModel().getStatusInModel();
        NorwegianMatchInfo matchInfo = p.getNorskNivå();

        if (publicationsStatus.equals(StatusInModelConstants.BEAKTAD_PUBLIKATION_SPECIALLFALL_EJ_NIVÅBESTÄMNING) && (p.getDivaPublicationType().equals(DivaPublicationTypes.avhandlingMonografi) || p.getDivaPublicationType().equals(DivaPublicationTypes.avhandlingSammanläggning))) {


            matchInfo.setVikt(3);
            matchInfo.setModelSpecificInfo("Doktorsavhandling");

        } else

        if (publicationsStatus.equals(StatusInModelConstants.BEAKTAD_PUBLIKATION_I_NORSKA_LISTAN)) {

            if (p.getDivaPublicationType().equals(DivaPublicationTypes.bok)) {

                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Monografi nivå 0");
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Monografi nivå 0");
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(5);
                    matchInfo.setModelSpecificInfo("Monografi nivå 1");
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(8);
                    matchInfo.setModelSpecificInfo("Monografi nivå 2");
                }


            } else if (p.getDivaPublicationType().equals(DivaPublicationTypes.tidskrift) || p.getDivaPublicationType().equals(DivaPublicationTypes.review)) {

                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Tidskrift nivå 0");
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Tidskrift nivå 0");
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(1);
                    matchInfo.setModelSpecificInfo("Tidskrift nivå 1");
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(3);
                    matchInfo.setModelSpecificInfo("Tidskrift nivå 2");
                }

            } else if (p.getDivaPublicationType().equals(DivaPublicationTypes.antologi)) {

                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Kapitel i antologi nivå 0");
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Kapitel i antologi nivå 0");
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(0.7);
                    matchInfo.setModelSpecificInfo("Kapitel i antologi nivå 1");
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(1);
                    matchInfo.setModelSpecificInfo("Kapitel i antologi nivå 2");
                }


            } else if (p.getDivaPublicationType().equals(DivaPublicationTypes.redaktörskapSamlingsverk)) {


                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Redaktör nivå 0");
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Redaktör nivå 0");
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(1);
                    matchInfo.setModelSpecificInfo("Redaktör nivå 1");
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(2);
                    matchInfo.setModelSpecificInfo("Redaktör nivå 2");
                }


            } else if(p.getDivaPublicationType().equals(DivaPublicationTypes.konferens)) {


                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Konferenspublikation nivå 0");
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("Konferenspublikation nivå 0");
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(0.5);
                    matchInfo.setModelSpecificInfo("Konferenspublikation nivå 1");
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(0.5);
                    matchInfo.setModelSpecificInfo("Konferenspublikation nivå 2");
                }



            }

        }
        //FANNS EJ I NORSKA LISTAN
        else if (publicationsStatus.equals(StatusInModelConstants.BEAKTAD_PUBLIKATION_EJ_I_NORSKA_LISTAN)) {

                matchInfo.setVikt(0);
                matchInfo.setModelSpecificInfo("Publikationskanalen ej återfunnen i norska listan");



        }

        //EJ BEAKTAD PUBLIKATIONSTYP
        else if (publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EJ_BEAKTAD_PUBLIKATIONSTYP)) {

            matchInfo.setVikt(0);
            matchInfo.setModelSpecificInfo("Ej beaktad publikationstyp");

        } else

        if(publicationsStatus.equals(StatusInModelConstants.IGNORERAD_ABSTRACT_POSTER_ELLER_PRESENTATION) || publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EDITORIAL_ABSTRACT_OR_NEWS_ITEM) || publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EJ_PUBLICERAD) || publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EJ_VETENSKAPLIGT) ) {

            matchInfo.setVikt(0);
            matchInfo.setModelSpecificInfo("Ej vetenskapligt innehåll alt. ej publicerad");

        } else {

            matchInfo.setVikt(-99);
            matchInfo.setModelSpecificInfo("ERROR IN WEIGHTING SCHEMA!!!");
        }






    }


    public static void SFWeighting(Post p) {


        String publicationsStatus = p.getStatusInModel().getStatusInModel();
        NorwegianMatchInfo matchInfo = p.getNorskNivå();

        if (StatusInModelConstants.BEAKTAD_PUBLIKATION_I_NORSKA_LISTAN.equals(publicationsStatus)) {




            if ("serie".equals(matchInfo.getType())) {


                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("SERIE NIVÅ 0");
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("SERIE NIVÅ 0");
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(1);
                    matchInfo.setModelSpecificInfo("SERIE NIVÅ 1");
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setModelSpecificInfo("SERIE NIVÅ 2");
                    matchInfo.setVikt(3);
                }

            }


            if ("förlag".equals(matchInfo.getType()) && (p.getDivaPublicationType().equals(DivaPublicationTypes.antologi) || p.getDivaPublicationType().equals(DivaPublicationTypes.konferens))) {


                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("FÖRLAG NIVÅ 0 (ARTIKEL)");
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("FÖRLAG NIVÅ 0 (ARTIKEL)");
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(0.7);
                    matchInfo.setModelSpecificInfo("FÖRLAG NIVÅ 1 (ARTIKEL)");
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(1);
                    matchInfo.setModelSpecificInfo("FÖRLAG NIVÅ 2 (ARTIKEL)");
                }

            }


            if ("förlag".equals(matchInfo.getType()) && p.getDivaPublicationType().equals(DivaPublicationTypes.bok)) {

                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("FÖRLAG NIVÅ 0 (BOK)");
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("FÖRLAG NIVÅ 0 (BOK)");
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(5);
                    matchInfo.setModelSpecificInfo("FÖRLAG NIVÅ 1 (BOK)");
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setModelSpecificInfo("FÖRLAG NIVÅ 2 (BOK)");
                    matchInfo.setVikt(8);
                }

            }


            //override series for diva book types!

            if ("serie".equals(matchInfo.getType()) && p.getDivaPublicationType().equals(DivaPublicationTypes.bok)) {


                if (matchInfo.getNivå() == null) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("SERIE NIVÅ 0 (BOKSERIE, BOK)");
                } else if (matchInfo.getNivå() == 0) {
                    matchInfo.setVikt(0);
                    matchInfo.setModelSpecificInfo("SERIE NIVÅ 0 (BOKSERIE, BOK)");
                } else if (matchInfo.getNivå() == 1) {
                    matchInfo.setVikt(5);
                    matchInfo.setModelSpecificInfo("SERIE NIVÅ 1 (BOKSERIE, BOK)");
                } else if (matchInfo.getNivå() == 2) {
                    matchInfo.setVikt(8);
                    matchInfo.setModelSpecificInfo("SERIE NIVÅ 2 (BOKSERIE, BOK)");
                }

            }

            //EJ i norska listan
        } else if (publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EJ_BEAKTAD_PUBLIKATIONSTYP)) {

                matchInfo.setVikt(0);
                matchInfo.setModelSpecificInfo("IGNORERAD (EJ BEAKTAD PUBLIKATIONSTYP)");

            } else if(publicationsStatus.equals(StatusInModelConstants.IGNORERAD_ABSTRACT_POSTER_ELLER_PRESENTATION) || publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EDITORIAL_ABSTRACT_OR_NEWS_ITEM)  ) {

                matchInfo.setVikt(0);
                matchInfo.setModelSpecificInfo("IGNORERAD (ABSTARACT, POSTER, NEWS ITEM ETC.)");

            } else if(publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EJ_PUBLICERAD) ) {

                matchInfo.setVikt(0);
                matchInfo.setModelSpecificInfo("IGNORERAD (STATUS EJ PUBLICERAD)");

            } else if(publicationsStatus.equals(StatusInModelConstants.IGNORERAD_EJ_VETENSKAPLIGT)) {

                matchInfo.setVikt(0);
                matchInfo.setModelSpecificInfo("IGNORERAD (EJ VETENSKAPLIG INNEHÅLLSTYP)");


            }  else if(publicationsStatus.equals(StatusInModelConstants.BEAKTAD_PUBLIKATION_EJ_I_NORSKA_LISTAN)) {

                matchInfo.setVikt(0);
                matchInfo.setModelSpecificInfo("KANALEN EJ I DET NORSKA AUKTORITETSREGISTRET");

            } else {

            System.out.println("Error undefined weight for this publikation type!");
            System.out.println("Publikation status: " + publicationsStatus);
            matchInfo.setVikt(-99);
            matchInfo.setModelSpecificInfo("ERROR IN MATCHING ROUTINE!");

        }




        } //weighting function ends







    }


