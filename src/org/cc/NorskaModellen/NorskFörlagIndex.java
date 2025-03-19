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

    Nivå2026(5),
    Nivå2025(6),
    Nivå2024(7),
    Nivå2023(8),
    Nivå2022(9),
    Nivå2021(10),
    Nivå2020(11),
    Nivå2019(12),
    Nivå2018(13),
    Nivå2017(14),
    Nivå2016(15),
    Nivå2015(16),
    Nivå2014(17),
    Nivå2013(18),
    Nivå2012(19),
    Nivå2011(20),
    Nivå2010(21),
    Nivå2009(22),
    Nivå2008(23),
    Nivå2007(24),
    Nivå2006(25),
    Nivå2005(26),
    Nivå2004(27);

    private int index;
    NorskFörlagIndex(int ind) {
        index = ind;
    }

    public int getValue() {return index;}




    }
