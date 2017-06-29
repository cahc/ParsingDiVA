package org.cc.NorskaModellen;

/**
 * Created by crco0001 on 6/9/2016.
 */
public class NorwegianMatchInfo {

    boolean inNorewgianList = false;
    String type = null; // {serie, förlag, ingen träff}
    String how = null;  // {issn,serie/tidskrift_namn,förlag_namn,ISBN_prefix}
    int norsk_id = -99;
    Integer nivå; // can be null
    Integer max_nivå; //can be null
    String norsk_namn; //can be null
    private double vikt = -99; // set with Viktning.function
    String modelSpecificInfo = "not available";


    public void setModelSpecificInfo(String s) {

        this.modelSpecificInfo = s;
    }

    public String getModelSpecificInfo() {

        return this.modelSpecificInfo;
    }
    public void setVikt(double v) {

        this.vikt =v;
    }

    public double getVikt() {

        return this.vikt;
    }

    public void setInNorewgianList(boolean isInNorwegianList) {

        this.inNorewgianList = isInNorwegianList;
    }

    public boolean isInNorewgianList() {

        return this.inNorewgianList;
    }

    public String getNorsk_namn() {
        return norsk_namn;
    }

    public void setNorsk_namn(String norsk_namn) {
        this.norsk_namn = norsk_namn;
    }

    public NorwegianMatchInfo() {}


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHow() {
        return how;
    }

    public void setHow(String how) {
        this.how = how;
    }


    public int getNorsk_id() {
        return norsk_id;
    }

    public void setNorsk_id(int serie_id) {
        this.norsk_id = serie_id;
    }

    public Integer getNivå() {
        return nivå;
    }

    public void setNivå(Integer nivå) {
        this.nivå = nivå;
    }

    public Integer getMax_nivå() {
        return max_nivå;
    }

    public void setMax_nivå(Integer max_nivå) {
        this.max_nivå = max_nivå;
    }

    @Override
    public String toString() {

        String separator = "\t";
        StringBuilder s = new StringBuilder(50);
        s.append(type).append(separator);
        s.append(how).append(separator);
        s.append(norsk_id).append(separator);
        s.append(norsk_namn).append(separator);
        s.append(nivå).append(separator);
        s.append(max_nivå).append(separator);
        s.append(modelSpecificInfo).append(separator);
        s.append(vikt);

        return s.toString();
    }

}
