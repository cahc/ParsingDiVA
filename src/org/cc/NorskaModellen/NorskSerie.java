package org.cc.NorskaModellen;

import org.cc.diva.DivaHelpFunctions;

/**
 * Created by crco0001 on 5/19/2016.
 */
public class NorskSerie {

    private Integer tidskriftsID; // not null
    private String originalTitel; // not null
    private String internationellTitel; //not null
    private String simplifyedTitle;
    private boolean nedlaggd;
    private String issnPrint; //can be null
    private String issnOnline; //can be mull
    private boolean DOAJ; //can be null
    private String npuField;
    private String discipliner; //can be null
    //private String serieType; //can be null
    private String förlag; //can be null
    private String utgivare; //can be null
    private String språk; //can be null

    private Integer nivå2021; //can be null
    private Integer nivå2020; //can be null
    private Integer nivå2019; //can be null
    private Integer nivå2018; //can be null
    private Integer nivå2017; //can be null
    private Integer nivå2016; //can be null
    private Integer nivå2015; //can be null
    private Integer nivå2014; //can be null
    private Integer nivå2013; //can be null
    private Integer nivå2012; //can be null
    private Integer nivå2011; //can be null
    private Integer nivå2010; //can be null
    private Integer nivå2009; //can be null
    private Integer nivå2008; //can be null
    private Integer nivå2007; //can be null
    private Integer nivå2006; //can be null
    private Integer nivå2005; //can be null
    private Integer nivå2004; //can be null

    public NorskSerie() {}


    public Integer getLevel(Integer year) { //can return null for two reasons, no level for the specified year or the specified year is null or outside of 2004-2016

            if(year == null) return null;

            if(year.equals(2004)) return getNivå2004();
            if(year.equals(2005)) return getNivå2005();
            if(year.equals(2006)) return getNivå2006();
            if(year.equals(2007)) return getNivå2007();
            if(year.equals(2008)) return getNivå2008();
            if(year.equals(2009)) return getNivå2009();
            if(year.equals(2010)) return getNivå2010();
            if(year.equals(2011)) return getNivå2011();
            if(year.equals(2012)) return getNivå2012();
            if(year.equals(2013)) return getNivå2013();
            if(year.equals(2014)) return getNivå2014();
            if(year.equals(2015)) return getNivå2015();
            if(year.equals(2016)) return getNivå2016();
            if(year.equals(2017)) return getNivå2017();
            if(year.equals(2018)) return getNivå2018();
            if(year.equals(2019)) return getNivå2019();
            if(year.equals(2020)) return getNivå2020();
            if(year.equals(2021)) return getNivå2021();
            return null; //obs!!

        }

    public Integer getHistoricalMaxLevel() {

        Integer max = 0;
        if(getNivå2021() != null && (max.compareTo( getNivå2021() ) < 0) ) max = getNivå2021();
        if(getNivå2020() != null && (max.compareTo( getNivå2020() ) < 0) ) max = getNivå2020();
        if(getNivå2019() != null && (max.compareTo( getNivå2019() ) < 0) ) max = getNivå2019();
        if(getNivå2018() != null && (max.compareTo( getNivå2018() ) < 0) ) max = getNivå2018();
        if(getNivå2017() != null && (max.compareTo( getNivå2017() ) < 0) ) max = getNivå2017();
        if(getNivå2016() != null && (max.compareTo( getNivå2016() ) < 0) ) max = getNivå2016();
        if(getNivå2015() != null && (max.compareTo( getNivå2015() ) < 0) ) max = getNivå2015();
        if(getNivå2014() != null && (max.compareTo( getNivå2014() ) < 0) ) max = getNivå2014();
        if(getNivå2013() != null && (max.compareTo( getNivå2013() ) < 0) ) max = getNivå2013();
        if(getNivå2012() != null && (max.compareTo( getNivå2012() ) < 0) ) max = getNivå2012();
        if(getNivå2011() != null && (max.compareTo( getNivå2011() ) < 0) ) max = getNivå2011();
        if(getNivå2010() != null && (max.compareTo( getNivå2010() ) < 0) ) max = getNivå2010();
        if(getNivå2009() != null && (max.compareTo( getNivå2009() ) < 0) ) max = getNivå2009();
        if(getNivå2008() != null && (max.compareTo( getNivå2008() ) < 0) ) max = getNivå2008();
        if(getNivå2007() != null && (max.compareTo( getNivå2007() ) < 0) ) max = getNivå2007();
        if(getNivå2006() != null && (max.compareTo( getNivå2006() ) < 0) ) max = getNivå2006();
        if(getNivå2005() != null && (max.compareTo( getNivå2005() ) < 0) ) max = getNivå2005();
        if(getNivå2004() != null && (max.compareTo( getNivå2004() ) < 0) ) max = getNivå2004();

        return max;
    }




    public String getOriginalTitel() {
        return originalTitel;
    }

    public void setOriginalTitel(String originalTitel) {
        this.originalTitel = originalTitel;
        this.simplifyedTitle = DivaHelpFunctions.simplifyString( this.originalTitel);
    }

    public String getSimplifyedTitle() {

        return this.simplifyedTitle;
    }
    public Integer getTidskriftsID() {
        return tidskriftsID;
    }

    public void setTidskriftsID(Integer tidskriftsID) {
        this.tidskriftsID = tidskriftsID;
    }

    public String getInternationellTitel() {
        return internationellTitel;
    }

    public void setInternationellTitel(String internationellTitel) {
        this.internationellTitel = internationellTitel;
    }

    public boolean isNedlaggd() {
        return nedlaggd;
    }

    public void setNedlaggd(boolean nedlaggd) {
        this.nedlaggd = nedlaggd;
    }

    public String getIssnPrint() {
        return issnPrint;
    }

    public void setIssnPrint(String issnPrint) {
        this.issnPrint = issnPrint;
    }

    public boolean isDOAJ() {
        return DOAJ;
    }

    public void setDOAJ(boolean DOAJ) {
        this.DOAJ = DOAJ;
    }

    public String getIssnOnline() {
        return issnOnline;
    }

    public void setIssnOnline(String issnOnline) {
        this.issnOnline = issnOnline;
    }

    public String getNpuField() {
        return npuField;
    }

    public void setNpuField(String npuField) {
        this.npuField = npuField;
    }

    public String getDiscipliner() {
        return discipliner;
    }

    public void setDiscipliner(String discipliner) {
        this.discipliner = discipliner;
    }

    /*
    public String getSerieType() {
        return serieType;
    }


    public void setSerieType(String serieType) {
        this.serieType = serieType;
    }

*/


    public String getFörlag() {
        return förlag;
    }



    public void setFörlag(String förlag) {
        this.förlag = förlag;
    }

    public String getUtgivare() {
        return utgivare;
    }

    public void setUtgivare(String utgivare) {
        this.utgivare = utgivare;
    }

    public String getSpråk() {
        return språk;
    }

    public void setSpråk(String språk) {
        this.språk = språk;
    }

    public Integer getNivå2015() {
        return nivå2015;
    }

    public void setNivå2015(Integer nivå2015) {
        this.nivå2015 = nivå2015;
    }

    public Integer getNivå2016() {
        return nivå2016;
    }

    public void setNivå2016(Integer nivå2016) {
        this.nivå2016 = nivå2016;
    }


    public void setNivå2021(Integer nivå2021) {this.nivå2021 = nivå2021; }

    public Integer getNivå2021() { return  nivå2021; }

    public void setNivå2020(Integer nivå2020) {this.nivå2020 = nivå2020; }
    public Integer getNivå2020() { return  nivå2020; }

    public Integer getNivå2019() { return  nivå2019; }

    public void setNivå2019(Integer nivå2019) { this.nivå2019 = nivå2019; }


    public Integer getNivå2018() { return  nivå2018; }

    public void setNivå2018(Integer nivå2018) { this.nivå2018 = nivå2018; }


    public Integer getNivå2017() { return  nivå2017; }

    public void setNivå2017(Integer nivå2017) { this.nivå2017 = nivå2017; }


    public Integer getNivå2014() {
        return nivå2014;
    }

    public void setNivå2014(Integer nivå2014) {
        this.nivå2014 = nivå2014;
    }

    public Integer getNivå2013() {
        return nivå2013;
    }

    public void setNivå2013(Integer nivå2013) {
        this.nivå2013 = nivå2013;
    }

    public Integer getNivå2012() {
        return nivå2012;
    }

    public void setNivå2012(Integer nivå2012) {
        this.nivå2012 = nivå2012;
    }

    public Integer getNivå2011() {
        return nivå2011;
    }

    public void setNivå2011(Integer nivå2011) {
        this.nivå2011 = nivå2011;
    }

    public Integer getNivå2010() {
        return nivå2010;
    }

    public void setNivå2010(Integer nivå2010) {
        this.nivå2010 = nivå2010;
    }

    public Integer getNivå2009() {
        return nivå2009;
    }

    public void setNivå2009(Integer nivå2009) {
        this.nivå2009 = nivå2009;
    }

    public Integer getNivå2008() {
        return nivå2008;
    }

    public void setNivå2008(Integer nivå2008) {
        this.nivå2008 = nivå2008;
    }

    public Integer getNivå2007() {
        return nivå2007;
    }

    public void setNivå2007(Integer nivå2007) {
        this.nivå2007 = nivå2007;
    }

    public Integer getNivå2006() {
        return nivå2006;
    }

    public void setNivå2006(Integer nivå2006) {
        this.nivå2006 = nivå2006;
    }

    public Integer getNivå2005() {
        return nivå2005;
    }

    public void setNivå2005(Integer nivå2005) {
        this.nivå2005 = nivå2005;
    }

    public Integer getNivå2004() {
        return nivå2004;
    }

    public void setNivå2004(Integer nivå2004) {
        this.nivå2004 = nivå2004;
    }
}
