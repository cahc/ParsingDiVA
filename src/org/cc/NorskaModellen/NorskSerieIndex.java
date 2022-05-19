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
    Forlag(30),
    Utgiver(31),
    Utgiverland(32),
    Språk(35),
    Nivå_2023(9),
    Nivå_2022(10),
    Nivå_2021(11),
    Nivå_2020(12),
    Nivå_2019(13),
    Nivå_2018(14),
    Nivå_2017(15),
    Nivå_2016(16),
    Nivå_2015(17),
    Nivå_2014(18),
    Nivå_2013(19),
    Nivå_2012(20),
    Nivå_2011(21),
    Nivå_2010(22),
    Nivå_2009(23),
    Nivå_2008(24),
    Nivå_2007(25),
    Nivå_2006(26),
    Nivå_2005(27),
    Nivå_2004(28);


    private int index;

    NorskSerieIndex(int ind) {
        index = ind;
    }

    public int getValue() {
        return index;
    }



    }
