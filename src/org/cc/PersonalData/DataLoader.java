package org.cc.PersonalData;

/**
 * Created by crco0001 on 8/8/2017.
 */
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by crco0001 on 3/16/2017.
 */
public class DataLoader {


    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final Calendar calendar = Calendar.getInstance();
    private final HashMap<String, Person> casToPerson = new HashMap<>();

    private List<Person> personList = new ArrayList<>();

    public DataLoader(File file) throws XMLStreamException, IOException, ParseException {

        if(!file.exists()) {System.out.println("xml with personData don't exist.."); System.exit(0); }


        //http://stackoverflow.com/questions/18717038/adding-resources-in-intellij-for-java-project

        XMLInputFactory factory = XMLInputFactory.newInstance();
       // InputStream in = getClass().getResourceAsStream("PERSDATA201703.XML");

       InputStream in = new FileInputStream(file);
        XMLEventReader eventReader = factory.createXMLEventReader( in );

        while (eventReader.hasNext()) {

            XMLEvent event = eventReader.nextEvent();


            if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
                StartElement startElement = event.asStartElement();

                if ("Employee".equals(startElement.getName().getLocalPart())) {

                    //A new person object in XML
                    Person newPerson = new Person();


                    //Get the attributes
                    Attribute attribute = startElement.getAttributeByName(new QName("GivenName"));
                    newPerson.setGivenName(attribute.getValue());
                    attribute = startElement.getAttributeByName(new QName("SurName"));
                    newPerson.setSurName(attribute.getValue());
                    attribute = startElement.getAttributeByName(new QName("DisplayName"));
                    String dispName = attribute.getValue();

                    dispName = dispName.replace("Dödsbo", "").trim();
                    newPerson.setDisplayName(dispName);
                    newPerson.setNormalizedDisplayName(dispName);
                    attribute = startElement.getAttributeByName(new QName("UID"));
                    newPerson.setUID(attribute.getValue().toLowerCase().trim());
                    attribute = startElement.getAttributeByName(new QName("Mail"));
                    newPerson.setMail(attribute.getValue());

                    attribute = startElement.getAttributeByName(new QName("NIN"));
                    newPerson.setNIN(attribute.getValue());
                    attribute = startElement.getAttributeByName(new QName("Gender"));
                    newPerson.setGender(attribute.getValue());

                    newPerson.normalizeAndSortName();
                    //get the period data

                    List<Period> employments = new ArrayList<>();
                    List<Period> affiliations = new ArrayList<>();


                    while (true) { //loop over all elements for an employee

                        event = eventReader.nextEvent();

                        if (event.getEventType() == XMLStreamConstants.START_ELEMENT && event.asStartElement().getName().getLocalPart().equals("EmploymentPeriods")) {

                            while (true) {

                                event = eventReader.nextEvent();

                                if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("Period")) {


                                    FacultyPositionYear facultyPositionYear = new FacultyPositionYear();

                                    startElement = event.asStartElement();
                                    Period period = new Period();
                                    String organisationNummer = startElement.getAttributeByName(new QName("OrganisationNumber")).getValue();
                                    period.setOrganisationNumber(organisationNummer);
                                    facultyPositionYear.setOrganisationNumber(Integer.valueOf(organisationNummer));

                                    if (organisationNummer.length() > 2) {

                                        if (organisationNummer.startsWith("1")) {
                                            facultyPositionYear.setFaculty("Humanistiska fakulteten");
                                        } else if (organisationNummer.startsWith("2")) {
                                            facultyPositionYear.setFaculty("Samhällsvetenskapliga fakulteten");
                                        } else if (organisationNummer.startsWith("3")) {
                                            facultyPositionYear.setFaculty("Medicinska fakulteten");
                                        } else if (organisationNummer.startsWith("4")) {
                                            facultyPositionYear.setFaculty("Medicinska fakulteten");
                                        } else //tandlärkar-relaterat
                                            if (organisationNummer.startsWith("5")) {
                                                facultyPositionYear.setFaculty("Teknisk-naturvetenskapliga fakulteten");
                                            } else if (organisationNummer.startsWith("6")) {
                                                facultyPositionYear.setFaculty("lärarhögskolan");
                                            } else if (organisationNummer.startsWith("7")) {
                                                facultyPositionYear.setFaculty("förvaltning/service");
                                            } else

                                                //"tricky"
                                                if (organisationNummer.startsWith("8400")) {
                                                    facultyPositionYear.setFaculty("Umeå University (gemensam)");
                                                } else if (organisationNummer.startsWith("8401")) {
                                                    facultyPositionYear.setFaculty("Umeå University (gemensam)");
                                                } else if (organisationNummer.startsWith("8402")) {
                                                    facultyPositionYear.setFaculty("Medicinska fakulteten");
                                                } else if (organisationNummer.startsWith("8403")) {
                                                    facultyPositionYear.setFaculty("Umeå University (gemensam)");
                                                } else if (organisationNummer.startsWith("8540")) {
                                                    facultyPositionYear.setFaculty("Samhällsvetenskapliga fakulteten");
                                                } else if (organisationNummer.startsWith("8541")) {
                                                    facultyPositionYear.setFaculty("Samhällsvetenskapliga fakulteten");
                                                } else if (organisationNummer.startsWith("8560")) {
                                                    facultyPositionYear.setFaculty("Umeå University (gemensam)");
                                                } else if (organisationNummer.startsWith("8610")) {
                                                    facultyPositionYear.setFaculty("Medicinska fakulteten");
                                                } else if (organisationNummer.startsWith("8615")) {
                                                    facultyPositionYear.setFaculty("Medicinska fakulteten");
                                                } else if (organisationNummer.startsWith("8620")) {
                                                    facultyPositionYear.setFaculty("Medicinska fakulteten");
                                                } else if (organisationNummer.startsWith("8640")) {
                                                    facultyPositionYear.setFaculty("Umeå University (gemensam)");
                                                } else if (organisationNummer.startsWith("8650")) {
                                                    facultyPositionYear.setFaculty("Umeå University (gemensam)");
                                                } else if (organisationNummer.startsWith("8660")) {
                                                    facultyPositionYear.setFaculty("Humanistiska fakulteten");
                                                } else {
                                                    facultyPositionYear.setFaculty("unknown");
                                                }

                                    } else {

                                        facultyPositionYear.setFaculty("unknown");
                                    }


                                    String position = startElement.getAttributeByName(new QName("Position")).getValue();
                                    facultyPositionYear.setPosition(position);
                                    period.setPosition(position);


                                    String startDatum = startElement.getAttributeByName(new QName("StartDate")).getValue();
                                    String stopDatum = startElement.getAttributeByName(new QName("StopDate")).getValue();

                                    Date startDate = simpleDateFormat.parse(startDatum);
                                    period.setStartDate(startDate);

                                    calendar.setTime(startDate);
                                    int startYear = calendar.get(Calendar.YEAR);


                                    newPerson.employmentYears.add(calendar.get(Calendar.YEAR));
                                    facultyPositionYear.setYear(calendar.get(Calendar.YEAR));


                                    String organisationNamn = startElement.getAttributeByName(new QName("OrganisationName")).getValue();
                                    if (organisationNamn.equals("")) organisationNamn = "information saknas";

                                    period.setOrganisationName(organisationNamn);
                                    facultyPositionYear.setUnit(organisationNamn);

                                    newPerson.facultyPositionYearEmployments.add(facultyPositionYear);

                                    if (stopDatum.equals("")) {


                                        //do nothing for the Period object, keep Enddate as null

                                        //for facultyPositionYearEmployments and employmentYears and years from startYear+1 to Current year ("tillsvidare anställd").
                                        Calendar now = Calendar.getInstance();   // Gets the current date and time
                                        int year = now.get(Calendar.YEAR);      // The current year as an int

                                        for (int i = year; i > startYear; i--) {

                                            newPerson.employmentYears.add(i);
                                            FacultyPositionYear facultyPositionYear2 = facultyPositionYear.makeCopy();
                                            facultyPositionYear2.setYear(i);
                                            newPerson.facultyPositionYearEmployments.add(facultyPositionYear2);
                                        }


                                    } else {

                                        Date endDate = simpleDateFormat.parse(stopDatum);
                                        calendar.setTime(endDate);
                                        int endyear = calendar.get(Calendar.YEAR);

                                        for (int i = endyear; i > startYear; i--) {

                                            newPerson.employmentYears.add(i);
                                            FacultyPositionYear facultyPositionYear2 = facultyPositionYear.makeCopy();
                                            facultyPositionYear2.setYear(i);
                                            newPerson.facultyPositionYearEmployments.add(facultyPositionYear2);
                                        }


                                        period.setEndDate(endDate);
                                    }


                                    employments.add(period);
                                }

                                if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals("EmploymentPeriods"))
                                    break;

                            }


                        } //employment processing loop

                        if (event.getEventType() == XMLStreamConstants.START_ELEMENT && event.asStartElement().getName().getLocalPart().equals("AffiliationPeriods")) {

                            while (true) {


                                event = eventReader.nextEvent();

                                if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("Period")) {


                                    FacultyPositionYear facultyPositionYear = new FacultyPositionYear();

                                    startElement = event.asStartElement();
                                    Period period = new Period();
                                    String organisationNummer = startElement.getAttributeByName(new QName("OrganisationNumber")).getValue();
                                    period.setOrganisationNumber(organisationNummer);
                                    facultyPositionYear.setOrganisationNumber(Integer.valueOf(organisationNummer));

                                    if (organisationNummer.length() > 2) {

                                        if (organisationNummer.startsWith("1")) {
                                            facultyPositionYear.setFaculty("Humanistiska fakulteten");
                                        } else if (organisationNummer.startsWith("2")) {
                                            facultyPositionYear.setFaculty("Samhällsvetenskapliga fakulteten");
                                        } else if (organisationNummer.startsWith("3")) {
                                            facultyPositionYear.setFaculty("Medicinska fakulteten");
                                        } else if (organisationNummer.startsWith("4")) {
                                            facultyPositionYear.setFaculty("Medicinska fakulteten");
                                        } else //tandlärkar-relaterat
                                            if (organisationNummer.startsWith("5")) {
                                                facultyPositionYear.setFaculty("Teknisk-naturvetenskapliga fakulteten");
                                            } else if (organisationNummer.startsWith("6")) {
                                                facultyPositionYear.setFaculty("lärarhögskolan");
                                            } else if (organisationNummer.startsWith("7")) {
                                                facultyPositionYear.setFaculty("förvaltning/service");
                                            } else

                                                //"tricky"
                                                if (organisationNummer.startsWith("8400")) {
                                                    facultyPositionYear.setFaculty("Umeå University (gemensam)");
                                                } else if (organisationNummer.startsWith("8401")) {
                                                    facultyPositionYear.setFaculty("Umeå University (gemensam)");
                                                } else if (organisationNummer.startsWith("8402")) {
                                                    facultyPositionYear.setFaculty("Medicinska fakulteten");
                                                } else if (organisationNummer.startsWith("8403")) {
                                                    facultyPositionYear.setFaculty("Umeå University (gemensam)");
                                                } else if (organisationNummer.startsWith("8540")) {
                                                    facultyPositionYear.setFaculty("Samhällsvetenskapliga fakulteten");
                                                } else if (organisationNummer.startsWith("8541")) {
                                                    facultyPositionYear.setFaculty("Samhällsvetenskapliga fakulteten");
                                                } else if (organisationNummer.startsWith("8560")) {
                                                    facultyPositionYear.setFaculty("Umeå University (gemensam)");
                                                } else if (organisationNummer.startsWith("8610")) {
                                                    facultyPositionYear.setFaculty("Medicinska fakulteten");
                                                } else if (organisationNummer.startsWith("8615")) {
                                                    facultyPositionYear.setFaculty("Medicinska fakulteten");
                                                } else if (organisationNummer.startsWith("8620")) {
                                                    facultyPositionYear.setFaculty("Medicinska fakulteten");
                                                } else if (organisationNummer.startsWith("8640")) {
                                                    facultyPositionYear.setFaculty("Umeå University (gemensam)");
                                                } else if (organisationNummer.startsWith("8650")) {
                                                    facultyPositionYear.setFaculty("Umeå University (gemensam)");
                                                } else if (organisationNummer.startsWith("8660")) {
                                                    facultyPositionYear.setFaculty("Humanistiska fakulteten");
                                                } else {
                                                    facultyPositionYear.setFaculty("unknown");
                                                }

                                    } else {

                                        facultyPositionYear.setFaculty("unknown");
                                    }


                                    String position = startElement.getAttributeByName(new QName("Position")).getValue();
                                    facultyPositionYear.setPosition(position);
                                    period.setPosition(position);


                                    String startDatum = startElement.getAttributeByName(new QName("StartDate")).getValue();
                                    String stopDatum = startElement.getAttributeByName(new QName("StopDate")).getValue();

                                    Date startDate = simpleDateFormat.parse(startDatum);
                                    period.setStartDate(startDate);

                                    calendar.setTime(startDate);
                                    int startYear = calendar.get(Calendar.YEAR);


                                    newPerson.employmentYears.add(calendar.get(Calendar.YEAR));
                                    facultyPositionYear.setYear(calendar.get(Calendar.YEAR));


                                    String organisationNamn = startElement.getAttributeByName(new QName("OrganisationName")).getValue();
                                    if (organisationNamn.equals("")) organisationNamn = "information saknas";

                                    period.setOrganisationName(organisationNamn);
                                    facultyPositionYear.setUnit(organisationNamn);

                                    newPerson.facultyPositionYearsAffiliations.add(facultyPositionYear);

                                    if (stopDatum.equals("")) {


                                        //do nothing for the Period object, keep Enddate as null

                                        //for facultyPositionYearEmployments and employmentYears and years from startYear+1 to Current year ("tillsvidare anställd").
                                        Calendar now = Calendar.getInstance();   // Gets the current date and time
                                        int year = now.get(Calendar.YEAR);      // The current year as an int

                                        for (int i = year; i > startYear; i--) {

                                            //newPerson.employmentYears.add(i);
                                            FacultyPositionYear facultyPositionYear2 = facultyPositionYear.makeCopy();
                                            facultyPositionYear2.setYear(i);
                                            newPerson.facultyPositionYearsAffiliations.add(facultyPositionYear2);
                                        }


                                    } else {

                                        Date endDate = simpleDateFormat.parse(stopDatum);
                                        calendar.setTime(endDate);
                                        int endyear = calendar.get(Calendar.YEAR);

                                        for (int i = endyear; i > startYear; i--) {

                                            //newPerson.employmentYears.add(i);
                                            FacultyPositionYear facultyPositionYear2 = facultyPositionYear.makeCopy();
                                            facultyPositionYear2.setYear(i);
                                            newPerson.facultyPositionYearsAffiliations.add(facultyPositionYear2);
                                        }


                                        period.setEndDate(endDate);
                                    }


                                    affiliations.add(period);
                                }


                                if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals("AffiliationPeriods"))
                                    break;

                            }


                        } //affiliation processing loop


                        if (event.getEventType() == XMLStreamConstants.END_ELEMENT) {

                            if (event.asEndElement().getName().getLocalPart().equals("Employee")) {

                                //Collections.sort(employmentList,Comparator.reverseOrder());
                                newPerson.employmentList = employments;
                                newPerson.affiliationList = affiliations;
                                newPerson.mergeEmploymentPeriods(); //sort and merge!
                                newPerson.mergeAffiliationPeriods(); //sort and merge!

                                newPerson.setUniqueFaculties();
                                personList.add(newPerson);
                                break;
                            }
                        }


                    } //while true ends (after we encountered a Employee end tag)


                } //if Employee


            }


        }


        eventReader.close();
        in.close();

        //initialize hashmap
        for (Person p : personList) {

            String cas = p.getUID().toLowerCase().trim();

            casToPerson.put(cas, p);

        }


    }


    public List<Person> getPersonData() {

        return this.personList;
    }

    public Person getPersonByCas(String cas) {

        String fixedCas = cas.replace("[", "");
        fixedCas = fixedCas.replace("]", "");
        fixedCas = fixedCas.toLowerCase().trim();

        return this.casToPerson.get(fixedCas);
    }



    public int size() {

        return personList.size();
    }

    public static String normalizeDivaName(String name) {

        StringBuilder stringbuilder = new StringBuilder();

        String[] namePair = name.split(",");
        String efternamn = namePair[0].trim();
        String förnamn = "";
        if (namePair.length > 1) {
            förnamn = namePair[1];
        }

        förnamn = förnamn.replaceAll("\\.", "");
        String[] förnamnPair = förnamn.split(" ");

        for (int i = 0; i < förnamnPair.length; i++) {

            if (förnamnPair[i].length() > 1) {
                stringbuilder.append(förnamnPair[i]);
                stringbuilder.append(" ");
            }
        }
        stringbuilder.append(efternamn);
        String klar = stringbuilder.toString().trim();
        return Person.normalizeText(klar);
    }

    public static String yearAndGender(String personnummer) {

        String gender = "unknown";
        String year = "unknown";

        if(personnummer == null) return year +"\t" +gender;



        personnummer = personnummer.trim();

        int n= personnummer.length();

        if(n==12) {

            //we can work with this

            year = personnummer.substring(0,4);
            int genderCheck = Integer.valueOf(personnummer.charAt(10)).intValue();

            if( (genderCheck%2) == 0) { gender = "kvinna";} else {gender = "man"; }


            return year +"\t" +gender;
        }


        if(n>=4) {

            year = personnummer.substring(0,4);

            return year +"\t" +gender;
        }


        return year +"\t" +gender;

    }




    public static void main(String[] arg) throws IOException, XMLStreamException, ParseException {

        DataLoader personalData = new DataLoader( new File("E:\\STARKA_MILJÖER_UTVÄRDERING\\V3_KOMPLETTERINGSRAPPORT\\PersonalData_201708071322409.xml"));

        System.out.println("Person objects: " + personalData.size());

        List<Person> allPersonObjects = personalData.getPersonData();

         for(Person p : allPersonObjects) {

             System.out.println(p.getNameForSearching() +" " + p.getUniqueFaculties());
         }










    }

}



