package org.cc.PersonalData;

/**
 * Created by crco0001 on 8/8/2017.
 */
public class FacultyPositionYear  {


/*

        This is an object specifying at which faculty and what position the person (employee) had at the time(year)
        Used for searching/filtering with CQengine
     */

    private String faculty; //med, hum, teknat, sam, other
    private Integer year;
    private String position;
    private Integer organisationNumber = -99;
    private String unit;

    public FacultyPositionYear(String faculty, Integer year, String position, Integer orgnumber, String unit) {

        this.faculty = faculty;
        this.year = year;
        this.position = position;
        this.organisationNumber = orgnumber;
        this.unit = unit;
    }


    public FacultyPositionYear() {

        this("unknown faculty",-99,"unknown position",-99,"unknown unit");
    }


    public FacultyPositionYear makeCopy() {


        return new FacultyPositionYear(this.faculty,this.year,this.position,this.organisationNumber, this.unit); // All immutable so OK
    }


    @Override
    public String toString() {
        return "FacultyPositionYear{" +
                "faculty='" + faculty + '\'' +
                ", year=" + year +
                ", position='" + position + '\'' +
                ", organisationNumber=" + organisationNumber +
                ", unit='" + unit + '\'' +
                '}';
    }

    public Integer getOrganisationNumber() {
        return organisationNumber;
    }

    public void setOrganisationNumber(Integer organisationNumber) {
        this.organisationNumber = organisationNumber;
    }

    public String getFaculty() {
        return faculty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FacultyPositionYear that = (FacultyPositionYear) o;

        if (!faculty.equals(that.faculty)) return false;
        if (!year.equals(that.year)) return false;
        if (!position.equals(that.position)) return false;
        if (!organisationNumber.equals(that.organisationNumber)) return false;
        return unit.equals(that.unit);

    }

    @Override
    public int hashCode() {
        int result = faculty != null ? faculty.hashCode() : 0;
        result = 31 * result + (year != null ? year.hashCode() : 0);
        result = 31 * result + (position != null ? position.hashCode() : 0);
        result = 31 * result + (organisationNumber != null ? organisationNumber.hashCode() : 0);
        return result;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
