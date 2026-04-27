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
    Utgiver(35),
    Utgiverland(36),
    Språk(37),

    Nivå_2027(9),
    Nivå_2026(10),
    Nivå_2025(11),
    Nivå_2024(12),
    Nivå_2023(13),
    Nivå_2022(14),
    Nivå_2021(15),
    Nivå_2020(16),
    Nivå_2019(17),
    Nivå_2018(18),
    Nivå_2017(19),
    Nivå_2016(20),
    Nivå_2015(21),
    Nivå_2014(22),
    Nivå_2013(23),
    Nivå_2012(24),
    Nivå_2011(25),
    Nivå_2010(26),
    Nivå_2009(27),
    Nivå_2008(28),
    Nivå_2007(29),
    Nivå_2006(30),
    Nivå_2005(31),
    Nivå_2004(32);


    private int index;

    NorskSerieIndex(int ind) {
        index = ind;
    }

    public int getValue() {
        return index;
    }



    }
