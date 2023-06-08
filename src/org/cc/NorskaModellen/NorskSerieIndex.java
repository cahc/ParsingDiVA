package org.cc.NorskaModellen;

/**
 * Created by crco0001 on 5/19/2016.
 */
public enum NorskSerieIndex {

    tidsskrift_id(0),
    Original_tittel(1),
    Internasjonal_tittel(2),

    Nedlagt(39),

    //Nivå_idag_2018(4),
    Print_ISSN(3),
    Online_ISSN(4),
    Open_Access(5),
    NPI_Fagfelt(8),
    Vitenskapsdisipliner(7),
    //Webadresse(10),
    //Tidsskrift_type(11),
    Forlag(31),
    Utgiver(32),
    Utgiverland(33),
    Språk(34),
    Nivå_2024(9),
    Nivå_2023(10),
    Nivå_2022(11),
    Nivå_2021(12),
    Nivå_2020(13),
    Nivå_2019(14),
    Nivå_2018(15),
    Nivå_2017(16),
    Nivå_2016(17),
    Nivå_2015(18),
    Nivå_2014(19),
    Nivå_2013(20),
    Nivå_2012(21),
    Nivå_2011(22),
    Nivå_2010(23),
    Nivå_2009(24),
    Nivå_2008(25),
    Nivå_2007(26),
    Nivå_2006(27),
    Nivå_2005(28),
    Nivå_2004(29);


    private int index;

    NorskSerieIndex(int ind) {
        index = ind;
    }

    public int getValue() {
        return index;
    }



    }
