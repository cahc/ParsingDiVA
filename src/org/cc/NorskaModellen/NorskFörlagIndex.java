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
    Nivå2024(5),
    Nivå2023(6),
    Nivå2022(7),
    Nivå2021(8),
    Nivå2020(9),
    Nivå2019(10),
    Nivå2018(11),
    Nivå2017(12),
    Nivå2016(13),
    Nivå2015(14),
    Nivå2014(15),
    Nivå2013(16),
    Nivå2012(17),
    Nivå2011(18),
    Nivå2010(19),
    Nivå2009(20),
    Nivå2008(21),
    Nivå2007(22),
    Nivå2006(23),
    Nivå2005(24),
    Nivå2004(25);

    private int index;
    NorskFörlagIndex(int ind) {
        index = ind;
    }

    public int getValue() {return index;}




    }
