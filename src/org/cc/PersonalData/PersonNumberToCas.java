package org.cc.PersonalData;

import org.cc.misc.GeneralExcelReader;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

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

        PersonNumberToCas personNumberToCas = new PersonNumberToCas(new File("C:\\Users\\crco0001\\Desktop\\TEKNAT_PRLIM\\PersonalData.xml"));

        GeneralExcelReader generalExcelReader = new GeneralExcelReader("C:\\Users\\crco0001\\Desktop\\TEKNAT_PRLIM\\Teknat forskare.xlsx");

        List<String> personnummer = generalExcelReader.getColumnData(0,0,true);



     for(int i=0; i<personnummer.size(); i++) {

         String s = personnummer.get(i);
         personnummer.set(i, "19" + s.replaceAll("-",""));

     }


       //System.out.println(personNumberToCas.getAllPersonObjects().size());

        for(Person p : personNumberToCas.getAllPersonObjects()) {

       //  System.out.println(p.getUID() +"\t" + p.getNIN() + "\t" + DataLoader.yearAndGender( p.getNIN() ));

        }


        Collections.sort(personNumberToCas.allPersonObjects, new Comparator<Person>() {
            @Override
            public int compare(Person o1, Person o2) {

                return o1.getNIN().compareTo(o2.getNIN());

            }
        });




     for(String s : personnummer) {

         Person searchObj = new Person();


         searchObj.setNIN( s );

         int idx = Collections.binarySearch(personNumberToCas.allPersonObjects, searchObj, new Comparator<Person>() {
             @Override
             public int compare(Person o1, Person o2) {

                 return o1.getNIN().compareTo(o2.getNIN());
             }
         });


         System.out.println(idx > 0 ? personNumberToCas.allPersonObjects.get(idx) : "unknown\tunknown");

     }

    }

}
