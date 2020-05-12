package org.cc.NorskaModellen;

import org.cc.diva.Post;

/**
 * Created by crco0001 on 6/21/2016.
 */
public class HFPubInclusion implements ConsideredPublications {



    public StatusInModel consideredPub(Post p) {

        /*

        Generellt gäller att:

        För att matchas mot norska listans autktoritetsregister måste en publikation vara av följande typer och uppfylla vissa krav på vetenskapligt innehåll:

        {"Artikel i tidskrift","Artikel, forskningsöversikt","Bok","Kapitel i bok, del av antologi", Redaktör för antologi/proceeding};

        Publikationen måste också vara av innehållstyp:

        {"Refereegranskat","Övrigt vetenskapligt"}

        Beroende på publikationstyp så ställs också krav på att följande underkategorier *EJ* är uppfyllda:

        1.Artikel i tidskift != {"editorialMaterial","meetingAbstract","newsItem",}
        2.Konferensbidrag != {"abstracts","poster","presentation"}

        Med publiceringsår avses tryckår. Publikationen måste vara publicerad med fullständiga bibliografiska uppgifter.
        Detta påverkar framförallt {"Artikel i tidskrift","Artikel, forskningsöversikt"} där det gäller att:

         1. Status = {Published}  (m a o *EJ* {"accepted","aheadofprint","inPress","submitted"} )



        SPECIALL: Avhandlingar beaktas för urvalsgruppen nydisputerade!
                  Hanteras då som en bok nivå 1

        */


        StatusInModel statusInModel = new StatusInModel();

        if(p.getDivaContentType().equals("Övrig (populärvetenskap, debatt, mm)")) {

            statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_EJ_VETENSKAPLIGT);
            statusInModel.setIgnorerad(true);


        } else if("Artikel i tidskrift".equals(p.getDivaPublicationType()) | "Artikel, forskningsöversikt".equals(p.getDivaPublicationType())) {


            if(p.getDivaContentType().equals("Refereegranskat") | p.getDivaContentType().equals("Övrigt vetenskapligt")) {

                //now check subtype

                //p.getDivaPublicationSubtype().equals("editorialMaterial") is allowed!!
                if(p.getDivaPublicationSubtype().equals("meetingAbstract") | p.getDivaPublicationSubtype().equals("newsItem") ) {


                    statusInModel.setIgnorerad(true);
                    statusInModel.setStatusInModel( StatusInModelConstants.IGNORERAD_EDITORIAL_ABSTRACT_OR_NEWS_ITEM);

                } else {

                    //finally check if published

                    if(p.getDivaStatus().equals("published") || p.getDivaStatus().equals("aheadofprint")) {
                        statusInModel.setIgnorerad(false); statusInModel.setStatusInModel(StatusInModelConstants.BEAKTAD_ÄNNU_EJ_MATCHAD_MOT_NORSKA_LISTAN);} else { statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_EJ_PUBLICERAD); statusInModel.setIgnorerad(true); }
                }


            } else { statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_EJ_VETENSKAPLIGT); statusInModel.setIgnorerad(true);}



        } else if("Bok".equals(p.getDivaPublicationType()) | "Kapitel i bok, del av antologi".equals(p.getDivaPublicationType())) {

            if(p.getDivaContentType().equals("Refereegranskat") || p.getDivaContentType().equals("Övrigt vetenskapligt")) { statusInModel.setStatusInModel(StatusInModelConstants.BEAKTAD_ÄNNU_EJ_MATCHAD_MOT_NORSKA_LISTAN); statusInModel.setIgnorerad(false); } else { statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_EJ_VETENSKAPLIGT); statusInModel.setIgnorerad(true);}


        } else if("Doktorsavhandling, monografi".equals( p.getDivaPublicationType() ) || "Doktorsavhandling, sammanläggning".equals(p.getDivaPublicationType()) ) {


            statusInModel.setIgnorerad(false);
            statusInModel.setStatusInModel( StatusInModelConstants.BEAKTAD_PUBLIKATION_SPECIALLFALL_EJ_NIVÅBESTÄMNING);
            return statusInModel;

        } else if("Proceedings (redaktörskap)".equals(p.getDivaPublicationType()) || "Samlingsverk (redaktörskap)".equals(p.getDivaPublicationType())) {

            if(p.getDivaContentType().equals("Refereegranskat") || p.getDivaContentType().equals("Övrigt vetenskapligt")) { statusInModel.setStatusInModel(StatusInModelConstants.BEAKTAD_ÄNNU_EJ_MATCHAD_MOT_NORSKA_LISTAN); statusInModel.setIgnorerad(false); } else { statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_EJ_VETENSKAPLIGT); statusInModel.setIgnorerad(true);}


        } else if("Manuskript (preprint)".equals(p.getDivaPublicationType())) {

            statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_EJ_PUBLICERAD); statusInModel.setIgnorerad(true);
        } else {

            //ej beaktad publikationstyp - kan bli klassad som övrigt med notera att ett, säg, Konferensbidrag, av underkategorin abstracts, poster, presentation ska sättas som IGNORERAD_EJ_VETENSKAPLIG


            if(p.getDivaPublicationSubtype().equals("abstracts") | p.getDivaPublicationSubtype().equals("meetingAbstract") | p.getDivaPublicationSubtype().equals("poster") | p.getDivaPublicationSubtype().equals("presentation")  | p.getDivaPublicationSubtype().equals("newsItem") | p.getDivaPublicationSubtype().equals("editorialMaterial")  ) {

                statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_EDITORIAL_ABSTRACT_OR_NEWS_ITEM);
                statusInModel.setIgnorerad(true);

            } else {


                statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_EJ_BEAKTAD_PUBLIKATIONSTYP);
                statusInModel.setIgnorerad(true);

            }
        }


        return statusInModel;
    }




}
