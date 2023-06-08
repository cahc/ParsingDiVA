package org.cc.NorskaModellen;

import org.cc.diva.Post;

/**
 * Created by crco0001 on 7/11/2016.
 */
public class LHPubInclusion implements ConsideredPublications {


    public StatusInModel consideredPub(Post p) {



        /**
         Generellt gäller att:

         För att matchas mot norska listans autktoritetsregister måste en publikation vara av förlande typer och uppfylla vissa krav på vetenskapligt innehåll:

         {"Artikel i tidskrift","Artikel, forskningsöversikt","Bok","Kapitel i bok, del av antologi","Konferensbidrag"};

         Publikationen måste också vara av innehållstyp:

         {"Refereegranskat","Övrigt vetenskapligt"}

         Beroende på publikationstyp så ställs också krav på att följande underkategorier *EJ* är uppfyllda:

         1.Artikel i tidskift != {"editorialMaterial","meetingAbstract","newsItem",}
         2.Konferensbidrag != {"abstracts","poster","presentation"}

         Med publiceringsår avses tryckår. Publikationen måste vara publicerad med fullständiga bibliografiska uppgifter.
         Detta påverkar framförallt {"Artikel i tidskrift","Artikel, forskningsöversikt"} där det gäller att:

         1. Status = {Published}  (m a o *EJ* {"accepted","aheadofprint","inPress","submitted"} )

         */


        StatusInModel statusInModel = new StatusInModel();


        if("Artikel i tidskrift".equals(p.getDivaPublicationType()) || "Artikel, forskningsöversikt".equals(p.getDivaPublicationType())) {


            if(p.getDivaContentType().equals("Refereegranskat") || p.getDivaContentType().equals("Övrigt vetenskapligt")) {

                //now check subtype

                if(p.getDivaPublicationSubtype().equals("editorialMaterial") | p.getDivaPublicationSubtype().equals("meetingAbstract") | p.getDivaPublicationSubtype().equals("newsItem") ) {


                    statusInModel.setIgnorerad(true);
                    statusInModel.setStatusInModel( StatusInModelConstants.IGNORERAD_EDITORIAL_ABSTRACT_OR_NEWS_ITEM);

                } else {

                    //finally check if published

                    if(p.getDivaStatus().equals("published") || p.getDivaStatus().equals("aheadofprint") ) {
                        statusInModel.setIgnorerad(false); statusInModel.setStatusInModel(StatusInModelConstants.BEAKTAD_ÄNNU_EJ_MATCHAD_MOT_NORSKA_LISTAN);} else { statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_EJ_PUBLICERAD); statusInModel.setIgnorerad(true); }
                }


            } else { statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_EJ_VETENSKAPLIGT); statusInModel.setIgnorerad(true);}



        } else

        if("Bok".equals(p.getDivaPublicationType()) || "Kapitel i bok, del av antologi".equals(p.getDivaPublicationType()) || "Samlingsverk (redaktörskap)".equals(p.getDivaPublicationType()) ) {

            if(p.getDivaContentType().equals("Refereegranskat") || p.getDivaContentType().equals("Övrigt vetenskapligt")) { statusInModel.setStatusInModel(StatusInModelConstants.BEAKTAD_ÄNNU_EJ_MATCHAD_MOT_NORSKA_LISTAN); statusInModel.setIgnorerad(false); } else { statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_EJ_VETENSKAPLIGT); statusInModel.setIgnorerad(true);}


        } else

        if("Konferensbidrag".equals(p.getDivaPublicationType())) {

            if(p.getDivaContentType().equals("Refereegranskat") | p.getDivaContentType().equals("Övrigt vetenskapligt")) {

                //check subtype

                if(p.getDivaPublicationSubtype().equals("abstracts") | p.getDivaPublicationSubtype().equals("poster") | p.getDivaPublicationSubtype().equals("presentation") ) {

                    statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_ABSTRACT_POSTER_ELLER_PRESENTATION);
                    statusInModel.setIgnorerad(true);

                } else {
                    statusInModel.setStatusInModel(StatusInModelConstants.BEAKTAD_ÄNNU_EJ_MATCHAD_MOT_NORSKA_LISTAN); statusInModel.setIgnorerad(false); }

            } else { statusInModel.setIgnorerad(true); statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_EJ_VETENSKAPLIGT);}




        } else

        if("Doktorsavhandling, monografi".equals( p.getDivaPublicationType() ) || "Doktorsavhandling, sammanläggning".equals(p.getDivaPublicationType()) ) {


            statusInModel.setIgnorerad(true);
            statusInModel.setStatusInModel(StatusInModelConstants.BEAKTAD_PUBLIKATION_SPECIALLFALL_EJ_NIVÅBESTÄMNING);

        } else

        {

            statusInModel.setStatusInModel(StatusInModelConstants.IGNORERAD_EJ_BEAKTAD_PUBLIKATIONSTYP);
            statusInModel.setIgnorerad(true);

        }


        return statusInModel;










    }
}
