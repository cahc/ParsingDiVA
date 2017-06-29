package org.cc.wos;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by crco0001 on 8/26/2016.
 */
public class WosRecord {

    //Static matcher objects for regular expressions
    private final static Pattern UT_IDENTIFYER = Pattern.compile("(?<=WOS:)[0-9a-z]+",Pattern.CASE_INSENSITIVE);



    //instance variables
   private String PT;
   private String Title;
   private String UT;
   private String PMID;
   private String DOI;
   private String SO;
   private String[] WC;
   private ArrayList<String> Keywords = new ArrayList<>(2); //Both Author keywords (DE) and Keyword PLUS (ID)
   private HashMap<String,Double> MACRO9;
   private String DT;
   private String AB;
   private String[] Authors;
   private String Adresses;
   private  int PY;

   //FOR RECLASSIFICATION
  private ArrayList<String> reclassifiedWCs = new ArrayList<>(2);
  private HashMap<String,Double> reclassifiedMACRO9;

  //METHODS

   public void addReclassifiedWCs(String newWC) {

       reclassifiedWCs.add(newWC);

   }

    public void setKeywords(String keywords) {

        String[] splitted = keywords.split(";");
        for(int i=0; i < splitted.length; i++) {
            splitted[i] = splitted[i].trim().toLowerCase();
            if(splitted[i].length() == 0) continue;
            this.Keywords.add(splitted[i]);
        }

    }

    public ArrayList<String> getKeywords() {

        return this.Keywords;
    }


    public String getAdresses() {
        return Adresses;
    }

    public void setAdresses(String adresses) {
        Adresses = adresses;
    }

    public String[] getAuthors() {
        return Authors;
    }

    public void setAuthors(String authors) {
        this.Authors = authors.split(";");

        for(int i=0; i<Authors.length; i++) Authors[i] = Authors[i].trim();

    }

    public int nrAuthors() {

        return this.Authors.length;
    }

    public void setAB(String AB) {

        this.AB = AB;
    }

    public String getAB() {

        return AB;
    }
    public void setMACRO9(Map<String,String> mapper) {

        //FRACTION OF A PUBLICATION THAT BE LONGS TO A GIVEN MACRO9 FIELD, SUMS TO 1

        MACRO9 =  new HashMap<>();
        ArrayList<String> tempCollectionOfMacro9Subjects= new ArrayList<>();

        for(int i=0; i<this.WC.length; i++) {

            tempCollectionOfMacro9Subjects.add(mapper.get( this.WC[i] ) );
        }

        Double totalMacro9Subjects = Double.valueOf(tempCollectionOfMacro9Subjects.size());
        for(String s: tempCollectionOfMacro9Subjects) {

            int freq = Collections.frequency(tempCollectionOfMacro9Subjects, s);

            MACRO9.put(s, freq/totalMacro9Subjects); //overwrites if necessary, slightly inefficient
        }

    }

    public void setReclassifiedMACRO9(Map<String,String> mapper) {

        //FRACTION OF A PUBLICATION THAT BE LONGS TO A GIVEN MACRO9 FIELD, SUMS TO 1

        this.reclassifiedMACRO9 =  new HashMap<>();
        ArrayList<String> tempCollectionOfMacro9Subjects= new ArrayList<>();

        for(int i=0; i<this.reclassifiedWCs.size(); i++) {

            tempCollectionOfMacro9Subjects.add(mapper.get( this.reclassifiedWCs.get(i) ) );
        }

        Double totalMacro9Subjects = Double.valueOf(tempCollectionOfMacro9Subjects.size());
        for(String s: tempCollectionOfMacro9Subjects) {

            int freq = Collections.frequency(tempCollectionOfMacro9Subjects, s);

            reclassifiedMACRO9.put(s, freq/totalMacro9Subjects); //overwrites if necessary, slightly inefficient
        }

    }



    public HashMap<String, Double> getMACRO9() {

        return this.MACRO9;
    }

    public void setDT(String DT) {

        this.DT = DT;
    }

    public String getDT() {

        return this.DT;
    }

    public void setWC(String wcString) {

        String[] WC = wcString.split(";");
        for(int i=0; i<WC.length; i++) WC[i] = WC[i].trim().toLowerCase();

        this.WC = WC;
    }

    public String[] getWC() {

        return this.WC;
    }

    @Override
    public String toString() {

        StringBuilder stringBuilder = new StringBuilder();
        String seperator = "\t";
        stringBuilder.append(PT).append(seperator);
        stringBuilder.append(DT).append(seperator);
        stringBuilder.append(PY).append(seperator);
        stringBuilder.append(UT).append(seperator);
        stringBuilder.append(DOI).append(seperator);
        stringBuilder.append(Keywords).append(seperator);
        stringBuilder.append(SO).append(seperator);
        stringBuilder.append(Title).append(seperator);
        stringBuilder.append(Arrays.toString(WC)).append(seperator);
        stringBuilder.append(MACRO9).append(seperator);
        stringBuilder.append(reclassifiedWCs).append(seperator);
        stringBuilder.append(reclassifiedMACRO9).append(seperator);
        stringBuilder.append(PMID).append(seperator);
        stringBuilder.append(Arrays.asList(Authors)).append(seperator);
        stringBuilder.append(nrAuthors()).append(seperator);
        stringBuilder.append(Adresses);

        return stringBuilder.toString();
    }
    public String getDOI() {
        return DOI;
    }


    public void setDOI(String DOI) {
        this.DOI = DOI;
    }

    public String getSO() {
        return SO;
    }

    public void setSO(String SO) {
        this.SO = SO;
    }

    public String getPMID() {

        return PMID;
    }

    public void setPMID(String PMID) {

        this.PMID = PMID;
    }

    public String getPT() {
        return PT;
    }

    public void setPT(String PT) {
        this.PT = PT;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getUT() {


        return UT;
    }

    public void setUT(String UT) {

        Matcher m = UT_IDENTIFYER.matcher(UT);
        m.find();
        this.UT =  m.group(0);

    }

    public int getPY() {
        return PY;
    }

    public void setPY(String PY) {
        this.PY = Integer.valueOf(PY);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WosRecord wosRecord = (WosRecord) o;

        return UT.equals(wosRecord.UT);

    }

    @Override
    public int hashCode() {
        return UT.hashCode();
    }


}
