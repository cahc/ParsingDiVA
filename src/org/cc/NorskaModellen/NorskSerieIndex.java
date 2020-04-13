package org.cc.NorskaModellen;

/**
 * Created by crco0001 on 5/19/2016.
 */
public enum NorskSerieIndex {

    tidsskrift_id(0),
    Original_tittel(1),
    Internasjonal_tittel(2),

    Nedlagt(34),

    //Nivå_idag_2018(4),
    Print_ISSN(3),
    Online_ISSN(4),
    Open_Access(5),
    NPI_Fagfelt(6),
    Vitenskapsdisipliner(7),
    //Webadresse(10),
    //Tidsskrift_type(11),
    Forlag(28),
    Utgiver(29),
    Utgiverland(30),
    Språk(31),
    Nivå_2021(8),
    Nivå_2020(9),
    Nivå_2019(10),
    Nivå_2018(11),
    Nivå_2017(12),
    Nivå_2016(13),
    Nivå_2015(14),
    Nivå_2014(15),
    Nivå_2013(16),
    Nivå_2012(17),
    Nivå_2011(18),
    Nivå_2010(19),
    Nivå_2009(20),
    Nivå_2008(21),
    Nivå_2007(22),
    Nivå_2006(23),
    Nivå_2005(24),
    Nivå_2004(25);


    private int index;

    NorskSerieIndex(int ind) {
        index = ind;
    }

    public int getValue() {
        return index;
    }



    }
