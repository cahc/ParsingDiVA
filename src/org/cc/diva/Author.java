package org.cc.diva;

import org.cc.misc.DivaIDtoNames;
import org.cc.wos.DiVAtoWoS;

import java.text.Normalizer;
import java.util.*;

/**
 * Created by crco0001 on 5/12/2016.
 */
public class Author implements Comparable<Author>{
    private boolean printAuthor = false;
    private boolean hasUmuDivaAddress = false;
    private int nrUmUaddresses = 0;
    private String cas = "not available";
    private String authorName = "not available" ;
    private Double forskningstid = -99.0;

    private double fractionConsiderMultipleUmUAffils;
    private double fractionConsiderMultipleUmUAffilsMin01;

    private double fractionIgnoreMultipleUmUAffils;
    private double fractionIgnoreMultipleUmUAffilsMin01;

    //no frac use getNoFrac that just returns 1

    private List<String> umuDivaAddresses; // can be null
    private List<Integer> lowestDivaAddressNumber; //can be null
    private List<DivaIDtoNames> affilMappingsObjects; // can be null
    private int PID;
    private Post post; // reference to the post in which the specific author object is located

    String disambiguateID = "XA0";

    String automaticAddedCas;

    public void addAutomaticAddedCas(String cas2) {

        automaticAddedCas = cas2;
    }


    public String getAutomaticAddedCass() {

        if(automaticAddedCas == null) return cas;

        return(automaticAddedCas);
    }

    public void setDisambiguateID(String i) {
        this.disambiguateID = i;
    }

    public String getDisambiguateID() {

        return disambiguateID;
    }
    public void mappAffiliations(Map<Integer,DivaIDtoNames> mappings) {

        if(this.nrUmUaddresses == 0) { affilMappingsObjects = Collections.emptyList(); return; }

        affilMappingsObjects = new ArrayList<>(nrUmUaddresses);

        for(int i=0; i<nrUmUaddresses; i++) {

           DivaIDtoNames mapp = mappings.get( getLowestDivaAddressNumber().get(i) );

           if(mapp == null) {System.out.println( getLowestDivaAddressNumber().get(i) + " diva affill id has no mappings!" ); System.exit(0); }

           affilMappingsObjects.add(mapp);

        }

    }


    public List<DivaIDtoNames> getAffilMappingsObjects() {

        return this.affilMappingsObjects;
    }

    public Double getForskningsTidProcent() {

        return forskningstid;
    }

    public void setForskningsTidProcent(Double tid) {

        this.forskningstid = tid;
    }

    public void setEnclosingPost(Post p) {

        this.post = p;
    }

    public Post getEnclosingPost() {

        return this.post;
    }
    public Author(String name) {

        String getCas = DivaHelpFunctions.extractCas(name);
        if(getCas != null) this.cas = getCas;

        String getName = DivaHelpFunctions.extractName(name);
        if(getName != null) this.authorName = getName.toLowerCase().trim();

        List<String> extractedAddresses = DivaHelpFunctions.extractUmUadress(name);
        if(extractedAddresses != null) { this.umuDivaAddresses = extractedAddresses; this.hasUmuDivaAddress = true; }

        if(this.hasUmuDivaAddress) {

           lowestDivaAddressNumber = new ArrayList<>(2);
           for(String s : this.umuDivaAddresses) { lowestDivaAddressNumber.add(DivaHelpFunctions.extractUmUAddressNumber(s)); }

            nrUmUaddresses = lowestDivaAddressNumber.size();
        }


    }


    public void setCas(String cas) {
        this.cas = cas;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Double getForskningstid() {
        return forskningstid;
    }

    public void setForskningstid(Double forskningstid) {
        this.forskningstid = forskningstid;
    }

    public void setFractionConsiderMultipleUmUAffils(double fractionConsiderMultipleUmUAffils) {
        this.fractionConsiderMultipleUmUAffils = fractionConsiderMultipleUmUAffils;
    }

    public void setFractionConsiderMultipleUmUAffilsMin01(double fractionConsiderMultipleUmUAffilsMin01) {
        this.fractionConsiderMultipleUmUAffilsMin01 = fractionConsiderMultipleUmUAffilsMin01;
    }

    public void setFractionIgnoreMultipleUmUAffils(double fractionIgnoreMultipleUmUAffils) {
        this.fractionIgnoreMultipleUmUAffils = fractionIgnoreMultipleUmUAffils;
    }

    public void setFractionIgnoreMultipleUmUAffilsMin01(double fractionIgnoreMultipleUmUAffilsMin01) {
        this.fractionIgnoreMultipleUmUAffilsMin01 = fractionIgnoreMultipleUmUAffilsMin01;
    }

    public Author() {


    }


    public String getAuthorName() {

        return authorName;
    }

    public int getNoFrac() {

        return 1;
    }

    public double getFractionIgnoreMultipleUmUAffils() {

        return this.fractionIgnoreMultipleUmUAffils;
    }

    public double getFractionConsiderMultipleUmUAffils() {

        return this.fractionConsiderMultipleUmUAffils;
    }

    public double getFractionConsiderMultipleUmUAffilsMin01() {

        return this.fractionConsiderMultipleUmUAffilsMin01;
    }

    public double getFractionIgnoreMultipleUmUAffilsMin01() {

        return this.fractionIgnoreMultipleUmUAffilsMin01;
    }


    public void setPrintAuthor(boolean includeAuthorInPrint) {

        this.printAuthor = includeAuthorInPrint;
    }

    public boolean getPrintAuthor() {

        return this.printAuthor;

    }


    public boolean hasUmuDivaAddress() {


        return this.hasUmuDivaAddress;
    }

    public List<Integer> getLowestDivaAddressNumber() {

        if(this.lowestDivaAddressNumber == null) return Collections.emptyList();

        return this.lowestDivaAddressNumber;

    }

    public void calculateAndSetFraction(int nrAuthors) {

        /*

         normal author fractionConsiderMultipleUmUAffils, split if within UmU if multiple UmU affiliations are present

         */

      if(!this.hasUmuDivaAddress) this.fractionConsiderMultipleUmUAffils = (1.0/ nrAuthors);

      if(this.hasUmuDivaAddress) this.fractionConsiderMultipleUmUAffils = (1.0/nrAuthors) / this.umuDivaAddresses.size();

        /*

        normal fractionConsiderMultipleUmUAffils that ignores multiple UmU-affiliations

         */
      this.fractionIgnoreMultipleUmUAffils = (1.0/ nrAuthors);

        /*
        fractionConsiderMultipleUmUAffils that don't go below 0.1 (if multiple UmU affiliations that sum to at least 0.1)
         */

        if(!this.hasUmuDivaAddress) {

            this.fractionConsiderMultipleUmUAffilsMin01 = ( 1.0/nrAuthors < 0.1) ? 0.1 : 1.0/nrAuthors;
        }

        if(this.hasUmuDivaAddress) {

            double frac = 1.0/nrAuthors;
            if(frac < 0.1) frac = 0.1;

            this.fractionConsiderMultipleUmUAffilsMin01 = (frac / this.umuDivaAddresses.size());

        }


        this.fractionIgnoreMultipleUmUAffilsMin01 = ( 1.0/nrAuthors < 0.1) ? 0.1 : 1.0/nrAuthors;
}



    public int getNrUmUaddresses() {

        return this.nrUmUaddresses;
    }

    public void setPID(int pid) {

    this.PID = pid;

}


    public String getCas() {

        return cas;

    }

    public String getAffiliations() {

        StringBuilder stringBuilder = new StringBuilder(20);

        if(this.hasUmuDivaAddress) {

            for(int i=0; i<umuDivaAddresses.size(); i++) {

                if(i==0)stringBuilder.append( umuDivaAddresses.get(i) );
                if(i>0) stringBuilder.append(" || ").append( umuDivaAddresses.get(i));

            }

        } else {

            stringBuilder.append("no UmU address");

        }

        return stringBuilder.toString();

    }

    public String printSeveralRowPerAuthorFracMethod1(int frac) {

        StringBuilder stringBuilder = new StringBuilder();

        if (this.hasUmuDivaAddress) {


            stringBuilder.append(this.PID);
            stringBuilder.append("\t");
            stringBuilder.append(this.authorName);
            stringBuilder.append("\t");
            stringBuilder.append(this.cas);
            stringBuilder.append("\t");
            stringBuilder.append(fractionConsiderMultipleUmUAffils);
            stringBuilder.append("\t");
            stringBuilder.append(fractionConsiderMultipleUmUAffilsMin01);
            stringBuilder.append("\t");
            stringBuilder.append(umuDivaAddresses.get(frac));
            stringBuilder.append("\t");
            stringBuilder.append(lowestDivaAddressNumber.get(frac));


        } else {
            stringBuilder.append(this.PID);
            stringBuilder.append("\t");
            stringBuilder.append(this.authorName);
            stringBuilder.append("\t");
            stringBuilder.append(this.cas);
            stringBuilder.append("\t");
            stringBuilder.append(this.fractionConsiderMultipleUmUAffils);
            stringBuilder.append("\t");
            stringBuilder.append(fractionConsiderMultipleUmUAffilsMin01);
            stringBuilder.append("\t");
            stringBuilder.append("no UmU address");
            stringBuilder.append("\t");
            stringBuilder.append("no UmU affiliation code");

        }

        return stringBuilder.toString();
    }

    public String printOneRowPerAuthorFracMethod2() {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(this.PID);
        stringBuilder.append("\t");
        stringBuilder.append(this.authorName);
        stringBuilder.append("\t");
        stringBuilder.append(this.cas);
        stringBuilder.append("\t");
        stringBuilder.append(getFractionIgnoreMultipleUmUAffils());
        stringBuilder.append("\t");


        if(this.hasUmuDivaAddress) {

            for(int i=0; i<umuDivaAddresses.size(); i++) {

                if(i==0)stringBuilder.append( umuDivaAddresses.get(i) );
                if(i>0) stringBuilder.append(" || ").append( umuDivaAddresses.get(i));

            }

        } else {

            stringBuilder.append("no UmU address");

        }


        return stringBuilder.toString();
    }


    @Override
    public int compareTo(Author other) {

       return this.cas.compareTo(other.cas);
    }


    public static String normalizeAndSortNames(String s) {

        //based on this: https://blog.mafr.de/2015/10/10/normalizing-text-in-java/
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        temp = temp.replaceAll("[^\\p{ASCII}]", "");
        temp = temp.replaceAll("-", " ");
        temp = temp.replaceAll("\\.","");
        // temp = temp.toLowerCase();

        String[] splitted = temp.split(",");

        for(int i=0; i<splitted.length; i++ ) {

            splitted[i] = splitted[i].trim();

        }

        Arrays.sort(splitted);
        return String.join(" ",splitted);
    }



    public String printMappedAuthorWithFractionsAndNorwegianModel() {


        String seperator = "\t";
        StringBuilder stringBuilder = new StringBuilder();

        int nrUmUAdresses = this.getAffilMappingsObjects().size();

        Post p = this.getEnclosingPost();

        if(nrUmUAdresses != 0) {

            List<DivaIDtoNames> divaIDtoNamesList = getAffilMappingsObjects();

            for(int i=0; i<divaIDtoNamesList.size(); i++) {
                //UmU author

                if(i>0) stringBuilder.append("\n"); //new line for multi affiled

                   stringBuilder.append(p.getPID()).append(seperator);

                    stringBuilder.append(getDisambiguateID()).append(seperator);

                    stringBuilder.append( getAuthorName() ).append(seperator);

                    stringBuilder.append(getCas()).append(seperator);

                    stringBuilder.append(getAutomaticAddedCass()).append(seperator);

                    stringBuilder.append( fractionConsiderMultipleUmUAffils ).append(seperator);

                    stringBuilder.append(divaIDtoNamesList.get(i).getFAKULTET() ).append(seperator);

                    stringBuilder.append(divaIDtoNamesList.get(i).getINSTITUTION() ).append(seperator);

            }

        } else {

            //non-UmU author

            stringBuilder.append(p.getPID()).append(seperator);

            stringBuilder.append(getDisambiguateID()).append(seperator);

            stringBuilder.append( getAuthorName() ).append(seperator);

            stringBuilder.append(getCas()).append(seperator);

            stringBuilder.append(getAutomaticAddedCass()).append(seperator);

            stringBuilder.append( fractionConsiderMultipleUmUAffils ).append(seperator);

            stringBuilder.append( "external" ).append(seperator); //FACULTY

            stringBuilder.append( "external" ).append(seperator); //INSTITUUTION



        }

       // System.out.println( p.getPID() + "\t" + a.getDisambiguateID() + "\t" + a.getAuthorName() +"\t" + a.getCas() +"\t" + a.getAutomaticAddedCass() +"\t" + a.getFractionIgnoreMultipleUmUAffils() + "\t" + a.getFractionConsiderMultipleUmUAffils() +"\t" +a.getNrUmUaddresses() + "\t" +a.getAffiliations() +"\t" +a.getAffilMappingsObjects() );



        return stringBuilder.toString();

    }

}
