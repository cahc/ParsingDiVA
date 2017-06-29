package org.cc.wos.misc;

/**
 * Created by crco0001 on 8/29/2016.
 */
public class TabRecord {

    String PID;
    String TI;
    String JOURNAL;
    String DOI;
    String PUBMED;
    String UT;
    String YEAR;

    public TabRecord() {}


    @Override
    public String toString() {
        return "TabRecord{" +
                "PID='" + PID + '\'' +
                ", TI='" + TI + '\'' +
                ", JOURNAL='" + JOURNAL + '\'' +
                ", DOI='" + DOI + '\'' +
                ", PUBMED='" + PUBMED + '\'' +
                ", UT='" + UT + '\'' +
                ", YEAR='" + YEAR + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TabRecord tabRecord = (TabRecord) o;

        if (!PID.equals(tabRecord.PID)) return false;
        if (!TI.equals(tabRecord.TI)) return false;
        if (!JOURNAL.equals(tabRecord.JOURNAL)) return false;
        if (!DOI.equals(tabRecord.DOI)) return false;
        if (!PUBMED.equals(tabRecord.PUBMED)) return false;
        return UT.equals(tabRecord.UT);

    }

    @Override
    public int hashCode() {
        int result = PID.hashCode();
        result = 31 * result + TI.hashCode();
        result = 31 * result + JOURNAL.hashCode();
        result = 31 * result + DOI.hashCode();
        result = 31 * result + PUBMED.hashCode();
        result = 31 * result + UT.hashCode();
        return result;
    }

    public void setYEAR(String YEAR) {

        this.YEAR = YEAR;
    }

    public String getYEAR() {

        return YEAR;
    }

    public String getPID() {

        return PID;
    }

    public void setPID(String PID) {
        this.PID = PID;
    }

    public String getTI() {
        return TI;
    }

    public void setTI(String TI) {
        this.TI = TI;
    }

    public String getJOURNAL() {
        return JOURNAL;
    }

    public void setJOURNAL(String JOURNAL) {
        this.JOURNAL = JOURNAL;
    }

    public String getDOI() {
        return DOI;
    }

    public void setDOI(String DOI) {
        this.DOI = DOI;
    }

    public String getPUBMED() {
        return PUBMED;
    }

    public void setPUBMED(String PUBMED) {
        this.PUBMED = PUBMED;
    }

    public String getUT() {
        return UT;
    }

    public void setUT(String UT) {
        this.UT = UT;
    }
}
