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
    Webadresse(23),
    Land(4),
    Nivå2020(5),
    Nivå2019(6),
    Nivå2018(7),
    Nivå2017(8),
    Nivå2016(9),
    Nivå2015(10),
    Nivå2014(11),
    Nivå2013(12),
    Nivå2012(13),
    Nivå2011(14),
    Nivå2010(15),
    Nivå2009(16),
    Nivå2008(17),
    Nivå2007(18),
    Nivå2006(19),
    Nivå2005(20),
    Nivå2004(21);

    private int index;
    NorskFörlagIndex(int ind) {
        index = ind;
    }

    public int getValue() {return index;}




    }
