package org.cc.PersonalData;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by crco0001 on 12/18/2017.
 */
public class PersonNumberToCas {


    private List<Person> allPersonObjects = new ArrayList<>();



    public PersonNumberToCas(File pathToXml) throws ParseException, XMLStreamException, IOException

    {

        DataLoader personalData = new DataLoader(pathToXml);

        this.allPersonObjects = personalData.getPersonData();
    }

    public List<Person> getAllPersonObjects() {

        return allPersonObjects;
    }










    public static void main(String[] arg) throws ParseException, XMLStreamException, IOException {

        PersonNumberToCas personNumberToCas = new PersonNumberToCas(new File("F:\\pdata.xml"));

        System.out.println(personNumberToCas.getAllPersonObjects().size());

        for(Person p : personNumberToCas.getAllPersonObjects()) {

         //   System.out.println(p.getUID() +"\t" + p.getNIN() + "\t" + DataLoader.yearAndGender( p.getNIN() ));

        }


        Collections.sort(personNumberToCas.allPersonObjects, new Comparator<Person>() {
            @Override
            public int compare(Person o1, Person o2) {

                return o1.getNIN().compareTo(o2.getNIN());

            }
        });


        Person searchObj = new Person();
        searchObj.setNIN("198011233536");

        int idx =  Collections.binarySearch(personNumberToCas.allPersonObjects, searchObj, new Comparator<Person>() {
            @Override
            public int compare(Person o1, Person o2) {

                return o1.getNIN().compareTo(o2.getNIN());
            }
        });


        System.out.println( personNumberToCas.allPersonObjects.get(idx) );



    }

}
