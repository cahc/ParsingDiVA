package org.cc.NorskaModellen;

import org.cc.diva.DivaHelpFunctions;

import java.util.List;

/**
 * Created by crco0001 on 5/19/2016.
 */
public class NorskFörlag {

    private Integer forlag_id;
    private String Originaltittel;
    private String simplyfiedTitle;
    private String Internasjonaltittel;
    //private boolean Nedlagt;
    private List<String> ISBNprefix; // can be null
    private String Land; // can be null

    private Integer Nivå2023; //can be null
    private Integer Nivå2022; //can be null
    private Integer Nivå2021; //can be null
    private Integer Nivå2020; // can be null
    private Integer Nivå2019; // can be null
    private Integer Nivå2018; // can be null
    private Integer Nivå2017; // can be null
    private Integer Nivå2016; // can be null
    private Integer Nivå2015; // can be null
    private Integer Nivå2014; // can be null
    private Integer Nivå2013; // can be null
    private Integer Nivå2012; // can be null
    private Integer Nivå2011; // can be null
    private Integer Nivå2010; // can be null
    private Integer Nivå2009; // can be null
    private Integer Nivå2008; // can be null
    private Integer Nivå2007; // can be null
    private Integer Nivå2006; // can be null
    private Integer Nivå2005; // can be null
    private Integer Nivå2004; // can be null


    public NorskFörlag() {}


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
        if(year.equals(2022)) return getNivå2022();
        if(year.equals(2023)) return getNivå2023();

        return null; //obs!!

    }

    public Integer getHistoricalMaxLevel() {

        Integer max = 0;
        if(getNivå2023() != null && (max.compareTo( getNivå2023() ) < 0) ) max = getNivå2023();

        if(getNivå2022() != null && (max.compareTo( getNivå2022() ) < 0) ) max = getNivå2022();
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

    public Integer getForlag_id() {
        return forlag_id;
    }

    public void setForlag_id(Integer forlag_id) {
        this.forlag_id = forlag_id;
    }

    public String getOriginaltittel() {
        return Originaltittel;
    }

    public void setOriginaltittel(String originaltittel) {
        Originaltittel = originaltittel;
    }

    public String getInternasjonaltittel() {
        return Internasjonaltittel;
    }

    public String getSimplyfiedTitle() {

        return simplyfiedTitle;
    }

    public void setInternasjonaltittel(String internasjonaltittel) {
        Internasjonaltittel = internasjonaltittel;
        simplyfiedTitle = DivaHelpFunctions.simplifyString(internasjonaltittel);
    }

    /*
    public boolean isNedlagt() {
        return Nedlagt;
    }

    public void setNedlagt(boolean nedlagt) {
        this.Nedlagt = nedlagt;
    }

*/


    public List<String> getISBNprefix() {
        return ISBNprefix;
    }

    public void setISBNprefix(List<String> ISBNprefix) {
        this.ISBNprefix = ISBNprefix;
    }

    public String getLand() {
        return Land;
    }

    public void setLand(String land) {
        Land = land;
    }


    public Integer getNivå2023() { return Nivå2023;}
    public void setNivå2023(Integer nivå2023) {Nivå2023 = nivå2023;}

    public Integer getNivå2022() {
        return Nivå2022;
    }

    public void setNivå2022(Integer nivå2022) {
        Nivå2022 = nivå2022;
    }


    public Integer getNivå2021() {
        return Nivå2021;
    }

    public void setNivå2021(Integer nivå2021) {
        Nivå2021 = nivå2021;
    }


    public Integer getNivå2020() {
        return Nivå2020;
    }

    public void setNivå2020(Integer nivå2020) {
        Nivå2020 = nivå2020;
    }


    public Integer getNivå2019() {
        return Nivå2019;
    }

    public void setNivå2019(Integer nivå2019) {
        Nivå2019 = nivå2019;
    }

    public Integer getNivå2018() {
        return Nivå2018;
    }

    public void setNivå2018(Integer nivå2018) {
        Nivå2018 = nivå2018;
    }


    public Integer getNivå2017() {
        return Nivå2017;
    }

    public void setNivå2017(Integer nivå2017) {
        Nivå2017 = nivå2017;
    }

    public Integer getNivå2016() {
        return Nivå2016;
    }

    public void setNivå2016(Integer nivå2016) {
        Nivå2016 = nivå2016;
    }

    public Integer getNivå2015() {
        return Nivå2015;
    }

    public void setNivå2015(Integer nivå2015) {
        Nivå2015 = nivå2015;
    }

    public Integer getNivå2014() {
        return Nivå2014;
    }

    public void setNivå2014(Integer nivå2014) {
        Nivå2014 = nivå2014;
    }

    public Integer getNivå2013() {
        return Nivå2013;
    }

    public void setNivå2013(Integer nivå2013) {
        Nivå2013 = nivå2013;
    }

    public Integer getNivå2012() {
        return Nivå2012;
    }

    public void setNivå2012(Integer nivå2012) {
        Nivå2012 = nivå2012;
    }

    public Integer getNivå2011() {
        return Nivå2011;
    }

    public void setNivå2011(Integer nivå2011) {
        Nivå2011 = nivå2011;
    }

    public Integer getNivå2010() {
        return Nivå2010;
    }

    public void setNivå2010(Integer nivå2010) {
        Nivå2010 = nivå2010;
    }

    public Integer getNivå2009() {
        return Nivå2009;
    }

    public void setNivå2009(Integer nivå2009) {
        Nivå2009 = nivå2009;
    }

    public Integer getNivå2008() {
        return Nivå2008;
    }

    public void setNivå2008(Integer nivå2008) {
        Nivå2008 = nivå2008;
    }

    public Integer getNivå2007() {
        return Nivå2007;
    }

    public void setNivå2007(Integer nivå2007) {
        Nivå2007 = nivå2007;
    }

    public Integer getNivå2006() {
        return Nivå2006;
    }

    public void setNivå2006(Integer nivå2006) {
        Nivå2006 = nivå2006;
    }

    public Integer getNivå2005() {
        return Nivå2005;
    }

    public void setNivå2005(Integer nivå2005) {
        Nivå2005 = nivå2005;
    }

    public Integer getNivå2004() {
        return Nivå2004;
    }

    public void setNivå2004(Integer nivå2004) {
        Nivå2004 = nivå2004;
    }
}
