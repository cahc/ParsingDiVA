package org.cc.applikationer;

import java.util.HashMap;
import java.util.TreeSet;

public class UMUID_YEARS_AT_UNITS {

    public final Integer minYears;
    public final Integer maxYears;

    //UMUID -- > UNIT(S) --> YEARS AT
    public final HashMap<String, HashMap<String, TreeSet<Integer>>> UMUID_TO_YEARS_AT_UNITS;

    public UMUID_YEARS_AT_UNITS(Integer minYears, Integer maxYears, HashMap<String,HashMap<String, TreeSet<Integer>>> UMUID_TO_YEARS_AT_UNITS) {
        this.minYears = minYears;
        this.maxYears = maxYears;
        this.UMUID_TO_YEARS_AT_UNITS = UMUID_TO_YEARS_AT_UNITS;
    }


}