package org.cc.misc;

/**
 * Created by crco0001 on 8/7/2017.
 */
public class DivaIDtoNames implements Comparable<DivaIDtoNames> {


    int divaID;
    String standard_SWE;
    String standard_ENG;
    String AKTIV;
    String TYP;
    String FAKULTET;
    String INSTITUTION;
    String ENHET;
    String ENHET_ALT2;
    String INFO;
    boolean CONSIDER_WHEN_FRACTIONALISING;
    String ALTERNATIVE;
    String KOD;


    public int getDivaID() {
        return divaID;
    }

    public void setDivaID(int divaID) {
        this.divaID = divaID;
    }

    public String getStandard_SWE() {
        return standard_SWE;
    }

    public void setStandard_SWE(String standard_SWE) {
        this.standard_SWE = standard_SWE;
    }

    public String getStandard_ENG() {
        return standard_ENG;
    }

    public void setStandard_ENG(String standard_ENG) {
        this.standard_ENG = standard_ENG;
    }

    public String getAKTIV() {
        return AKTIV;
    }

    public void setAKTIV(String AKTIV) {
        this.AKTIV = AKTIV;
    }

    public String getTYP() {
        return TYP;
    }

    public void setTYP(String TYP) {
        this.TYP = TYP;
    }

    public String getFAKULTET() {
        return FAKULTET;
    }

    public void setFAKULTET(String FAKULTET) {
        this.FAKULTET = FAKULTET;
    }

    public String getINSTITUTION() {
        return INSTITUTION;
    }

    public void setINSTITUTION(String INSTITUTION) {
        this.INSTITUTION = INSTITUTION;
    }

    public String getENHET() {
        return ENHET;
    }

    public void setENHET(String ENHET) {
        this.ENHET = ENHET;
    }

    public String getENHET_ALT2() {
        return ENHET_ALT2;
    }

    public void setENHET_ALT2(String ENHET_ALT2) {
        this.ENHET_ALT2 = ENHET_ALT2;
    }

    public String getINFO() {
        return INFO;
    }

    public void setINFO(String INFO) {
        this.INFO = INFO;
    }

    public boolean isCONSIDER_WHEN_FRACTIONALISING() {
        return CONSIDER_WHEN_FRACTIONALISING;
    }

    public void setCONSIDER_WHEN_FRACTIONALISING(boolean CONSIDER_WHEN_FRACTIONALISING) {
        this.CONSIDER_WHEN_FRACTIONALISING = CONSIDER_WHEN_FRACTIONALISING;
    }

    public String getALTERNATIVE() {
        return ALTERNATIVE;
    }

    public void setALTERNATIVE(String ALTERNATIVE) {
        this.ALTERNATIVE = ALTERNATIVE;
    }

    public String getKOD() {
        return KOD;
    }

    public void setKOD(String KOD) {
        this.KOD = KOD;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DivaIDtoNames that = (DivaIDtoNames) o;

        if (divaID != that.divaID) return false;
        if (CONSIDER_WHEN_FRACTIONALISING != that.CONSIDER_WHEN_FRACTIONALISING) return false;
        if (!standard_SWE.equals(that.standard_SWE)) return false;
        if (!standard_ENG.equals(that.standard_ENG)) return false;
        if (!AKTIV.equals(that.AKTIV)) return false;
        if (!TYP.equals(that.TYP)) return false;
        if (!FAKULTET.equals(that.FAKULTET)) return false;
        if (!INSTITUTION.equals(that.INSTITUTION)) return false;
        if (!ENHET.equals(that.ENHET)) return false;
        if (!ENHET_ALT2.equals(that.ENHET_ALT2)) return false;
        if (!INFO.equals(that.INFO)) return false;
        if (!ALTERNATIVE.equals(that.ALTERNATIVE)) return false;
        return KOD.equals(that.KOD);
    }

    @Override
    public int hashCode() {
        int result = divaID;
        result = 31 * result + standard_SWE.hashCode();
        result = 31 * result + standard_ENG.hashCode();
        result = 31 * result + AKTIV.hashCode();
        result = 31 * result + TYP.hashCode();
        result = 31 * result + FAKULTET.hashCode();
        result = 31 * result + INSTITUTION.hashCode();
        result = 31 * result + ENHET.hashCode();
        result = 31 * result + ENHET_ALT2.hashCode();
        result = 31 * result + INFO.hashCode();
        result = 31 * result + (CONSIDER_WHEN_FRACTIONALISING ? 1 : 0);
        result = 31 * result + ALTERNATIVE.hashCode();
        result = 31 * result + KOD.hashCode();
        return result;
    }


    @Override
    public int compareTo(DivaIDtoNames other) {

        if (this.divaID > other.divaID) return 1;
        if(this.divaID < other.divaID) return -1;

        return 0;

    }


    @Override
    public String toString() {
        return "DivaIDtoNames{" +
                "divaID=" + divaID +
                ", standard_SWE='" + standard_SWE + '\'' +
                ", standard_ENG='" + standard_ENG + '\'' +
                ", AKTIV='" + AKTIV + '\'' +
                ", TYP='" + TYP + '\'' +
                ", FAKULTET='" + FAKULTET + '\'' +
                ", INSTITUTION='" + INSTITUTION + '\'' +
                ", ENHET='" + ENHET + '\'' +
                ", ENHET_ALT2='" + ENHET_ALT2 + '\'' +
                ", INFO='" + INFO + '\'' +
                ", CONSIDER_WHEN_FRACTIONALISING=" + CONSIDER_WHEN_FRACTIONALISING +
                ", ALTERNATIVE='" + ALTERNATIVE + '\'' +
                ", KOD='" + KOD + '\'' +
                '}';
    }
}
