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
    Nivå2023(5),
    Nivå2022(6),
    Nivå2021(7),
    Nivå2020(8),
    Nivå2019(9),
    Nivå2018(10),
    Nivå2017(11),
    Nivå2016(12),
    Nivå2015(13),
    Nivå2014(14),
    Nivå2013(15),
    Nivå2012(16),
    Nivå2011(17),
    Nivå2010(18),
    Nivå2009(19),
    Nivå2008(20),
    Nivå2007(21),
    Nivå2006(22),
    Nivå2005(23),
    Nivå2004(24);

    private int index;
    NorskFörlagIndex(int ind) {
        index = ind;
    }

    public int getValue() {return index;}




    }
