package org.cc.NorskaModellen;

/**
 * Created by crco0001 on 5/24/2016.
 */
public enum NorskFörlagIndex {

    forlag_id(0),
    Originaltittel(1),
    InternasjonalTittel(2),
    Nedlagt(3),
    Nivåidag2017(4),
    ISBNprefix(5),
    Webadresse(6),
    Land(7),
    Nivå2018(8),
    Nivå2017(9),
    Nivå2016(10),
    Nivå2015(11),
    Nivå2014(12),
    Nivå2013(13),
    Nivå2012(14),
    Nivå2011(15),
    Nivå2010(16),
    Nivå2009(17),
    Nivå2008(18),
    Nivå2007(19),
    Nivå2006(20),
    Nivå2005(21),
    Nivå2004(22);

    private int index;
    NorskFörlagIndex(int ind) {
        index = ind;
    }

    public int getValue() {return index;}




    }
