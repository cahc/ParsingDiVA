package org.cc.NorskaModellen;

/**
 * Created by crco0001 on 5/19/2016.
 */
public enum NorskSerieIndex {

    tidsskrift_id(0),
    Original_tittel(1),
    Internasjonal_tittel(2),
    Nedlagt(3),
    Nivå_idag_2017(4),
    Print_ISSN(5),
    Online_ISSN(6),
    Open_Access(7),
    NPI_Fagfelt(8),
    Vitenskapsdisipliner(9),
    Webadresse(10),
    Tidsskrift_type(11),
    Forlag(12),
    Utgiver(13),
    Utgiverland(14),
    Språk(15),
    Nivå_2018(16),
    Nivå_2017(17),
    Nivå_2016(18),
    Nivå_2015(19),
    Nivå_2014(20),
    Nivå_2013(21),
    Nivå_2012(22),
    Nivå_2011(23),
    Nivå_2010(24),
    Nivå_2009(25),
    Nivå_2008(26),
    Nivå_2007(27),
    Nivå_2006(28),
    Nivå_2005(29),
    Nivå_2004(30);


    private int index;

    NorskSerieIndex(int ind) {
        index = ind;
    }

    public int getValue() {
        return index;
    }



    }
