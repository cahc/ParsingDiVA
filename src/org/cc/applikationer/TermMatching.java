package org.cc.applikationer;

import org.cc.diva.CreateDivaTable;
import org.cc.diva.Post;
import org.cc.diva.ReducedDiVAColumnIndices;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by crco0001 on 8/15/2016.
 */
public class TermMatching {


    public static void main(String[] arg) throws IOException {


        if(arg.length != 2) {System.out.println("input: TermList divaDump"); System.exit(1); }

        File terms = new File(arg[0]);
        File dump = new File(arg[1]);

        if(!terms.exists()) {System.out.println("TermList not found"); System.exit(1); }
        if(!dump.exists()) {System.out.println("divaDump not found"); System.exit(1); }

        System.out.println("Creating a hash table with terms - class");

        HashMap<String, String> termToClass = new HashMap<>(400,90);

        BufferedReader reader = new BufferedReader( new FileReader(terms));

        String string;
        String[] termClass;
        int iter = 0;
        while(  (string = reader.readLine() ) != null    ) {

            termClass = string.split("\t");
            if(termClass.length != 2) {System.out.println("TermList must be a two column file"); System.exit(1); }

            if(iter == 0) {

                if(!termClass[0].equals("TERM")) { System.out.println("TermList must be a two column file with headers TERM and CLASS"); System.exit(1); }
                if(!termClass[1].equals("CLASS")) { System.out.println("TermList must be a two column file with headers TERM and CLASS"); System.exit(1); }
                iter++;
                continue;
            }

            termToClass.put( termClass[0].trim().toLowerCase(), termClass[1] );

            iter++;
        }

        System.out.println("Unika Nyckelord: " + termToClass.size());

        /*
        Iterator <Map.Entry<String,String>> it  = termToClass.entrySet().iterator();

        while(it.hasNext()) {

           Map.Entry<String,String>  entry =  it.next();
            System.out.println( entry.getKey() + " --> " +entry.getValue()  );
        }

        */

        CreateDivaTable divaTable = new CreateDivaTable(dump);
        divaTable.parse();
        System.out.println("Antal poster i divaDump: " + divaTable.nrRows() );


        ArrayList<Post> postArrayList = new ArrayList<>(100);
        for(int i=0; i < divaTable.nrRows(); i++ ) {

            Post post = new Post(divaTable.getRowInTable(i));
            postArrayList.add(post);

        }

        ArrayList<String> textData = new ArrayList<>(100);
        for(Post p : postArrayList) {

            String[] rawString = p.getRawDataRow();
            StringBuilder stringBuilder = new StringBuilder();

            String PID = rawString[ReducedDiVAColumnIndices.PID.getValue()];
            String title = rawString[ReducedDiVAColumnIndices.Title.getValue()];
            String abstracts = rawString[ReducedDiVAColumnIndices.Abstract.getValue()];
            String keywords = rawString[ReducedDiVAColumnIndices.Keywords.getValue()];

            stringBuilder.append(PID).append("\t").append(title).append("\t").append(abstracts).append("\t").append(keywords);
            textData.add( stringBuilder.toString().toLowerCase() );

        }


        //MATCHA NU MOT TERMLIST (BRUTE FORCE, STÖRRE DATAMÄNDGDER KRÄVER INVERTERAT INDEX ELLER DYLIKT)

        int counter = 0;
        for(String s : textData) {

            StringBuilder termHits = new StringBuilder();
            StringBuilder classInfo = new StringBuilder();

            boolean anyHit = false;


            for (Map.Entry<String, String> entry : termToClass.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String searchKey;

                //högertrunkera om nyckelordet är 4 tecken eller längre..

                if(key.length() >= 4) { searchKey = ".*(?<![a-zåäö])"+ key +".*"; } else {

                    searchKey = ".*(?<![a-zåäö])"+ key +"(?![a-zåäö]).*";
                }


                    if(s.matches( searchKey ) ) {
                //if( s.contains(key) ) {

                    if(anyHit) { termHits.append(" || ").append(key); } else termHits.append(key);
                    if(anyHit) { classInfo.append(" || ").append(value); } else classInfo.append(value);

                    anyHit = true;
                }

            }


            if(!anyHit) {

                termHits.append("no match!");
                classInfo.append("no match!");
            }


            System.out.println( postArrayList.get(counter).getPID() +"\t"  + termHits.toString() +"\t" + classInfo.toString() );

            counter++;



        }

    }



}
