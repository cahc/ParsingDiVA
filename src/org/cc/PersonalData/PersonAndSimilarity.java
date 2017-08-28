package org.cc.PersonalData;

/**
 * Created by crco0001 on 8/8/2017.
 */
public class PersonAndSimilarity implements Comparable<PersonAndSimilarity> {

    Person p;
    Double similarity;

    public PersonAndSimilarity(Person p, Double similarity) {

        this.p = p;
        this.similarity = similarity;

    }



    @Override
    public int compareTo(PersonAndSimilarity other) {

        if(this.similarity < other.similarity) return -1;
        if(this.similarity > other.similarity) return 1;
        return 0;


    }

    public Person getP() {
        return p;
    }

    public void setP(Person p) {
        this.p = p;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    @Override
    public String toString() {
        return this.p +"\t" +this.similarity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PersonAndSimilarity that = (PersonAndSimilarity) o;

        if (!p.equals(that.p)) return false;
        return similarity.equals(that.similarity);

    }

    @Override
    public int hashCode() {
        int result = p.hashCode();
        result = 31 * result + similarity.hashCode();
        return result;
    }
}
