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
    Nivå2022(5),
    Nivå2021(6),
    Nivå2020(7),
    Nivå2019(8),
    Nivå2018(9),
    Nivå2017(10),
    Nivå2016(11),
    Nivå2015(12),
    Nivå2014(13),
    Nivå2013(14),
    Nivå2012(15),
    Nivå2011(16),
    Nivå2010(17),
    Nivå2009(18),
    Nivå2008(19),
    Nivå2007(20),
    Nivå2006(21),
    Nivå2005(22),
    Nivå2004(23);

    private int index;
    NorskFörlagIndex(int ind) {
        index = ind;
    }

    public int getValue() {return index;}




    }
