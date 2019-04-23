package org.cc.misc;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import jsat.utils.IntList;
import org.cc.PersonalData.DataLoader;
import org.cc.PersonalData.Person;
import org.cc.diva.CreateDivaTable;
import org.cc.diva.DeduplicatePostsPerAuthor;
import org.cc.diva.DivaHelpFunctions;
import org.cc.wos.WosHelperFunctions;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Arrays;

/**
 * Created by crco0001 on 11/10/2016.
 */
public class Playground {


    public static String fixName(String input) {

        IntList spaces = new IntList();
        IntList dashes = new IntList();
        IntList spike = new IntList();
        int N = input.length();

        for(int i=0; i<N; i++){

            if( input.charAt(i) == ' ') spaces.add(i);
            if( input.charAt(i) == '-') dashes.add(i);
            if( input.charAt(i) == '|') spike.add(i);

        }

        StringBuilder stringBuilder = new StringBuilder(input);

        stringBuilder.setCharAt(0, Character.toUpperCase(input.charAt(0))); //Always capitalize start

        for(int i : spaces) {

            stringBuilder.setCharAt(i+1, Character.toUpperCase(input.charAt(i+1)));
        }

        for(int i : dashes) {

            stringBuilder.setCharAt(i+1, Character.toUpperCase(input.charAt(i+1)));
        }

        for(int i : spike) {

            stringBuilder.setCharAt(i+1, Character.toUpperCase(input.charAt(i+1)));
        }



        return stringBuilder.toString();
    }

    public static void main(String[] arg) throws IOException, XMLStreamException, ParseException {


        DataLoader personalData = new DataLoader( new File("PersonalData_201903180103182.xml") );
        System.out.println(personalData.size());
        Person p = personalData.getPersonByCas("crco0001");
        System.out.println(p.getFacultyPositionYearEmployments());


      //  String test = "edin, kerstin e";

       // System.out.println(fixName(test));


      //  NormalizedLevenshtein lvSim = new NormalizedLevenshtein();

      //  String s1 = DivaHelpFunctions.simplifyString( "International Journal of Telerehabilitation" );
      //  String s2 =   DivaHelpFunctions.simplifyString("International Journal of Neurorehabilitation" );

     //   String s3 =  DivaHelpFunctions.simplifyString("International Journal of Engineering and Technology");
     //   String s4 =   DivaHelpFunctions.simplifyString("International Journal of Web Engineering and Technology");

     //   String s5 =  DivaHelpFunctions.simplifyString("The journal of crap");
    //    String s6 =   DivaHelpFunctions.simplifyString("thejournal of crapp");

    //    System.out.println("likhet mellan s1 och s2: " + lvSim.similarity(s1,s2));

    //    System.out.println("likhet mellan s3 och s4: " + lvSim.similarity(s3,s4));

    //    System.out.println("likhet mellan s5 och s6: " + lvSim.similarity(s5,s6));

        //if(arg.length != 1) {System.out.println("supply divaDumpFile"); System.exit(0); }

       // File file = new File(arg[0]);

       // CreateDivaTable divaTable = new CreateDivaTable(file);
       // divaTable.parse();
      //  System.out.println("Antal poster i rådata: " + divaTable.nrRows() );

     //   divaTable.saveToExcel("DumpToExcel");

        //DeduplicatePostsPerAuthor deduplicatePostsPerAuthor = new DeduplicatePostsPerAuthor();
        //System.out.println(Arrays.toString(deduplicatePostsPerAuthor.digester("hello as!")) );
        //System.out.println(Arrays.toString(deduplicatePostsPerAuthor.digester("hellO as!")) );
        //System.out.println(DivaHelpFunctions.simplifyString("hello as whole!:åäö"));
    }

}
