package org.cc.NorskaModellen;

import java.util.HashMap;

/**
 * Created by crco0001 on 7/11/2016.
 */
public class AggregatedAuthorInformation {


    private double rawSumOfWeights = 0;
    private double rawSumOfFractions = 0;
    private int    rawSumOfPublications = 0;
    private double    forskningstidProcent = 0;

    private double restrictedSumOfWeights = 0; //Humanistiska fakulteten
    private double fractionalizedPubSum = 0; // Lärarhögskolan

    HashMap<Integer,Integer> övrigaPublicationerPerÅr = new HashMap<>(); //FÖR HF-MODELLEN, beakta max 5 per år!


    public double getFractionalizedPubSum() {

        return fractionalizedPubSum;
    }

    public void incrementFractionalizedPubSum(double increment) {

        fractionalizedPubSum += increment;
    }

    public double getForskningstidProcent() {
        return forskningstidProcent;
    }

    public void setForskningstidProcent(double tid) {

        this.forskningstidProcent = tid;
    }

    public double getForskningstidsVikt() {

        return 2 - (forskningstidProcent/100.0);


    }


    public double getRestrictedSumOfWeights() {

        return restrictedSumOfWeights;
    }

    public void incrementRestrictedSumOfWeights(double incement) {

        restrictedSumOfWeights += incement;
    }
    public double getRawSumOfWeights() {
        return rawSumOfWeights;
    }

    public void incrementRawSumOfWeights(double increment) {
        this.rawSumOfWeights += increment;
    }

    public double getRawSumOfFractions() {
        return this.rawSumOfFractions;
    }

    public void incrementRawSumOfFractions(double increment) {
        this.rawSumOfFractions += increment;
    }

    public int getRawSumOfPublications() {
        return rawSumOfPublications;
    }

    public void incrementRawSumOfPublications() {
        this.rawSumOfPublications++;
    }
}
