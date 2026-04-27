package org.cc.NorskaModellen;

/**
 * Created by crco0001 on 5/24/2016.
 */
public enum NorskFörlagIndex {


    //2019-03-25 NSDs register over vitenskapelige publiseringskanaler - serier,tidskrifter,förlag

    forlag_id(0),
    Originaltittel(1),
    InternasjonalTittel(2),
    //Nedlagt(3),
    //Nivåidag2018(4),
    ISBNprefix(3),
    Webadresse(28),
    Land(4),

    Nivå2027(5),
    Nivå2026(6),
    Nivå2025(7),
    Nivå2024(8),
    Nivå2023(9),
    Nivå2022(10),
    Nivå2021(11),
    Nivå2020(12),
    Nivå2019(13),
    Nivå2018(14),
    Nivå2017(15),
    Nivå2016(16),
    Nivå2015(17),
    Nivå2014(18),
    Nivå2013(19),
    Nivå2012(20),
    Nivå2011(21),
    Nivå2010(22),
    Nivå2009(23),
    Nivå2008(24),
    Nivå2007(25),
    Nivå2006(26),
    Nivå2005(27),
    Nivå2004(28);

    private int index;
    NorskFörlagIndex(int ind) {
        index = ind;
    }

    public int getValue() {return index;}




    }
