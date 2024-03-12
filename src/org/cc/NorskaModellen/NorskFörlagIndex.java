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
    Webadresse(24),
    Land(4),

    Nivå2025(5),
    Nivå2024(6),
    Nivå2023(7),
    Nivå2022(8),
    Nivå2021(9),
    Nivå2020(10),
    Nivå2019(11),
    Nivå2018(12),
    Nivå2017(13),
    Nivå2016(14),
    Nivå2015(15),
    Nivå2014(16),
    Nivå2013(17),
    Nivå2012(18),
    Nivå2011(19),
    Nivå2010(20),
    Nivå2009(21),
    Nivå2008(22),
    Nivå2007(23),
    Nivå2006(24),
    Nivå2005(25),
    Nivå2004(26);

    private int index;
    NorskFörlagIndex(int ind) {
        index = ind;
    }

    public int getValue() {return index;}




    }
