package org.cc.PersonalData;

/**
 * Created by crco0001 on 8/8/2017.
 */

import info.debatty.java.stringsimilarity.Cosine;

import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.*;

import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.*;

import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.StringProfile;
import org.cc.diva.Author;


/**
 * Created by crco0001 on 11/1/2016.
 */
public class Person {

    public static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal.getTime();
    }


    public void normalizeAndSortName() {

        StringJoiner stringJoiner = new StringJoiner(", ");
        stringJoiner.add(this.GivenName.toLowerCase());
        stringJoiner.add(this.SurName.toLowerCase());
        this.NameForSearching = Author.normalizeAndSortNames(stringJoiner.toString());

    }

    public String getNameForSearching() {

        return this.NameForSearching;
    }


    public static String normalizeText(String s) {

        //based on this: https://blog.mafr.de/2015/10/10/normalizing-text-in-java/
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        temp = temp.replaceAll("[^\\p{ASCII}]", "");
        return temp.toLowerCase();
    }

    final static DecimalFormat formatter = new DecimalFormat("#0.000");

    public void mergeEmploymentPeriods() {

        //remove any duplicates if present (why is there duplicates sometimes?)

        for(ListIterator<Period> iterator = employmentList.listIterator(); iterator.hasNext();) {
            Period period = iterator.next();
            if(Collections.frequency(employmentList, period) > 1) {
                iterator.remove();
            }
        }


        //sort by organisation id and enddate and merge

        Collections.sort(this.employmentList, Comparator.reverseOrder());
        List<Period> mergedPeriodList = new ArrayList<Period>();
        int n = this.employmentList.size();

        for(int i=0; i<n;i++) {

            Period p1 = this.employmentList.get(i); // potentially update this with new start date
            i++;


            while(i<n) {

                Period p2 = this.employmentList.get(i);

                if(p1.getOrganisationNumber().equals(p2.getOrganisationNumber()) && p1.getPosition().equals(p2.getPosition()) && addDays(p2.getEndDate(),1).equals(p1.getStartDate())   ) {

                    p1.setStartDate(p2.getStartDate() );
                    i++;
                } else {

                    i--;
                    break;
                }

            }

            mergedPeriodList.add(p1);
        }



        // sort again after enddate only

        Collections.sort(mergedPeriodList, new Comparator<Period>() {
            @Override
            public int compare(Period o1, Period o2) {

                if(o1.getEndDate() == null) return -1;
                if(o2.getEndDate() == null) return 1;

                if(o1.getEndDate().after(o2.getEndDate())) { return -1;}
                if(o1.getEndDate().before(o2.getEndDate())) { return 1;}
                return 0;
            }
        });


        this.employmentList = mergedPeriodList;




    }

    public void mergeAffiliationPeriods() {

        //remove any duplicates if present (why is there duplicates sometimes?)

        for(ListIterator<Period>iterator = affiliationList.listIterator(); iterator.hasNext();) {
            Period period = iterator.next();
            if(Collections.frequency(affiliationList, period) > 1) {
                iterator.remove();
            }
        }


        //sort by organisation id and enddate and merge

        Collections.sort(this.affiliationList, Comparator.reverseOrder());
        List<Period> mergedAffiliationList = new ArrayList<Period>();
        int n = this.affiliationList.size();

        for(int i=0; i<n;i++) {

            Period p1 = this.affiliationList.get(i); // potentially update this with new start date
            i++;


            while(i<n) {

                Period p2 = this.affiliationList.get(i);

                if(p1.getOrganisationNumber().equals(p2.getOrganisationNumber()) && p1.getPosition().equals(p2.getPosition()) && addDays(p2.getEndDate(),1).equals(p1.getStartDate())   ) {

                    p1.setStartDate(p2.getStartDate() );
                    i++;
                } else {

                    i--;
                    break;
                }

            }

            mergedAffiliationList.add(p1);
        }



        // sort again after enddate only

        Collections.sort(mergedAffiliationList, new Comparator<Period>() {
            @Override
            public int compare(Period o1, Period o2) {

                if(o1.getEndDate() == null) return -1;
                if(o2.getEndDate() == null) return 1;

                if(o1.getEndDate().after(o2.getEndDate())) { return -1;}
                if(o1.getEndDate().before(o2.getEndDate())) { return 1;}
                return 0;
            }
        });


        this.affiliationList = mergedAffiliationList;

    }

    String NameForSearching = "";
    String GivenName = "";
    String SurName = "";
    String NormalizedGivenName = "";
    String NormalizedSurName = "";
    String DisplayName = "";
    String NormalizedDisplayName = "";
    String UID = "";
    String Mail = "";

    String NIN ="";
    String Gender ="";

    Map<String,Integer> stringProfile; // for Shingle based similarity matching
    Set<Integer> employmentYears = new TreeSet<>(); // collection of years that the person has been *employed*
    Set<FacultyPositionYear> facultyPositionYearEmployments = new HashSet<>(); // collection of triplets: faculty, year, position..
    Set<FacultyPositionYear> facultyPositionYearsAffiliations = new HashSet<>(); // collection of triplets: faculty, year, position..
    List<Period> employmentList;
    List<Period> affiliationList;


    Set<String> fakulties = new HashSet<>();

    public void setUniqueFaculties() {

        for(FacultyPositionYear facultyPositionYear : this.facultyPositionYearEmployments) {

            fakulties.add( facultyPositionYear.getFaculty() );

        }

        for(FacultyPositionYear facultyPositionYear : this.facultyPositionYearsAffiliations) {

            fakulties.add( facultyPositionYear.getFaculty() );

        }


    }

    public Set<String> getUniqueFaculties() {

        return this.fakulties;
    }

    public Set<FacultyPositionYear> getFacultyPositionYearsAffiliations() {

        return this.facultyPositionYearsAffiliations;

    }

    public Set<FacultyPositionYear> getFacultyPositionYearEmployments() {

        return this.facultyPositionYearEmployments;
    }

    public boolean[] employmentYearIndicatorArray() {

        //indicate employment per year beteween 2008 - 2016

        boolean[] indicator = new boolean[9];


        if(employmentYears.contains(2008)) indicator[0] =true;
        if(employmentYears.contains(2009)) indicator[1] =true;
        if(employmentYears.contains(2010)) indicator[2] =true;
        if(employmentYears.contains(2011)) indicator[3] =true;
        if(employmentYears.contains(2012)) indicator[4] =true;
        if(employmentYears.contains(2013)) indicator[5] =true;
        if(employmentYears.contains(2014)) indicator[6] =true;
        if(employmentYears.contains(2015)) indicator[7] =true;
        if(employmentYears.contains(2016)) indicator[8] =true;


        return indicator;
    }


    public String employmentYearsIndicatorToString() {

        boolean[] temp = employmentYearIndicatorArray();

        String sep = "\t";
        StringBuilder stringBuilder = new StringBuilder(20);

        for(int i=0; i<temp.length; i++) {

            if(i<temp.length+1) stringBuilder.append(temp[i]).append(sep);
            if(i==temp.length+1) stringBuilder.append(temp[i]);
        }

        return stringBuilder.toString();
    }


    public String getGivenName() {
        return GivenName;
    }

    public void setGivenName(String givenName) {
        GivenName = givenName;
        NormalizedGivenName = normalizeText(givenName);
    }


    public String getNIN() {
        return NIN;
    }

    public void setNIN(String NIN) {
        this.NIN = NIN;
    }

    public String getGender() {
        return Gender;
    }

    public void setGender(String gender) {
        Gender = gender;
    }

    public String getSurName() {
        return SurName;
    }

    public String getNormalizedSurName() {return  NormalizedSurName;}

    public String getNormalizedGivenName() {return NormalizedGivenName;}

    public String getNormalizedDisplayName() {return NormalizedDisplayName; }

    public void setSurName(String surName) {
        SurName = surName;
        NormalizedSurName = normalizeText(surName);
    }

    public String getDisplayName() {
        return DisplayName;
    }

    public void setDisplayName(String displayName) {

        // String  newDisplayName = displayName.replace("Dödsbo","").trim();

        DisplayName = displayName;

    }

    public void setNormalizedDisplayName(String displayName) {

        NormalizedDisplayName = normalizeText(displayName);

    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public String getMail() {
        return Mail;
    }

    public void setMail(String mail) {
        Mail = mail;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Person person = (Person) o;

        if (!GivenName.equals(person.GivenName)) return false;
        if (!SurName.equals(person.SurName)) return false;
        if (!DisplayName.equals(person.DisplayName)) return false;
        if (!UID.equals(person.UID)) return false;
        return Mail.equals(person.Mail);

    }

    @Override
    public int hashCode() {
        int result = GivenName.hashCode();
        result = 31 * result + SurName.hashCode();
        result = 31 * result + DisplayName.hashCode();
        result = 31 * result + UID.hashCode();
        result = 31 * result + Mail.hashCode();
        return result;
    }

    @Override
    public String toString() {


        return this.getDisplayName() + "\t" +this.getUID();


    }


}
