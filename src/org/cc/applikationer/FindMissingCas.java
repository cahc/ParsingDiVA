package org.cc.applikationer;

import info.debatty.java.stringsimilarity.Levenshtein;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import org.cc.diva.Author;
import org.cc.diva.CreateDivaTable;
import org.cc.diva.Post;
import org.cc.misc.CAStoTime;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by crco0001 on 6/12/2017.
 */
public class FindMissingCas {

    public static void main(String[] arg) throws IOException {


        if(arg.length != 2) { System.out.println("CAS-file & divaDump"); System.exit(0); }

        File casFil =  new File( arg[0] );
        File divaDumpFil = new File( arg[1] );


        CreateDivaTable divaTable = new CreateDivaTable(divaDumpFil);
        divaTable.parse();
        System.out.println("Antal poster i rådata: " + divaTable.nrRows() );


        CAStoTime listaMedCasForskningstid = new CAStoTime(casFil);
        System.out.println("Antal inlästa CAS med forskningstid: " +  listaMedCasForskningstid.antalCas() );



        List<Post> postList = new ArrayList<>(100);

        for(int i = 0; i< divaTable.nrRows(); i++) {

            Post post = new Post(divaTable.getRowInTable(i));

            postList.add(post);

        }



        String[] evaluatedCasFromList = listaMedCasForskningstid.getAllCas();

        HashMap<String,Set<String>> casToNameVariants = new HashMap<>();
        HashMap<String,Set<Integer>> casToAffilIDs = new HashMap<>();
        HashMap<String,Set<Integer>> casToPIDS = new HashMap<>();

        for(String s : evaluatedCasFromList) {HashSet<String> names = new HashSet<>(); casToNameVariants.put(s,names); }
        for(String s : evaluatedCasFromList) {HashSet<Integer> ids = new HashSet<>(); casToAffilIDs.put(s,ids); }
        for(String s : evaluatedCasFromList) {HashSet<Integer> pids = new HashSet<>(); casToPIDS.put(s,pids); }


        System.out.println("Pass one, find name variants and affiliations connected to a specific CAS..");

        for(Post p : postList) {

            List<Author> a_list = p.getAuthorList();

            for(Author a : a_list) {

               String otherCAS = a.getCas();

                List<Integer> divaAdressNr = a.getLowestDivaAddressNumber();

                    for(String thisCAS : evaluatedCasFromList) {

                        if( thisCAS.equals(otherCAS) ) {

                            String name = a.getAuthorName();
                            int pid = a.getEnclosingPost().getPID();
                            Set<Integer> pids = casToPIDS.get(thisCAS);
                            Set<String> names = casToNameVariants.get(thisCAS);
                            Set<Integer> ids = casToAffilIDs.get(thisCAS);

                            ids.addAll(divaAdressNr);
                            names.add(name);
                            pids.add(pid);

                            break;

                        }
                    }


            }


        }



        for(Map.Entry<String,Set<String>> entry : casToNameVariants.entrySet()) {


            System.out.println(entry.getKey() + " " + entry.getValue()  +" " + casToAffilIDs.get(entry.getKey()) +" " + casToPIDS.get(entry.getKey()) );


        }

        System.out.println("Pass two: match name variants + affiliations against diva data and identify posts missed if only using CAS..");

        HashMap<String,Set<Integer>> casToPidsV2 = new HashMap<>();
        for(String s : evaluatedCasFromList) {HashSet<Integer> pids = new HashSet<>(); casToPidsV2.put(s,pids); }


        NormalizedLevenshtein lvSim = new NormalizedLevenshtein();

        for(Post p : postList) {


            List<Author> a_list = p.getAuthorList();




                       for(Author author : a_list) {


                           List<Integer> affils = author.getLowestDivaAddressNumber();
                           if(affils.size() <= 0) continue;

                           //ok an UmU author lets check..


                              String OtherName = author.getAuthorName();
                              jump:
                              for(int i=0; i<casToNameVariants.size(); i++) {

                                  Set<String> nameVariants = casToNameVariants.get(evaluatedCasFromList[i]);
                                  Iterator<String> iter = nameVariants.iterator();


                                  while(iter.hasNext()) {
                                      Double sim = lvSim.similarity(OtherName, iter.next());

                                      if(sim >= 0.95) {

                                          //NAME HIT, CHECK AFFILS
                                          boolean matchOnAffil = false;

                                          List<Integer> OtherDivaIDs = author.getLowestDivaAddressNumber();

                                          for(Integer id : casToAffilIDs.get(evaluatedCasFromList[i]) ) { if(OtherDivaIDs.contains(id)) { matchOnAffil = true; }  }


                                          if(matchOnAffil) {

                                              //OK!

                                              Set<Integer> pids = casToPidsV2.get( evaluatedCasFromList[i] );
                                              pids.add(  p.getPID()  );
                                              break jump;


                                          }

                                      }


                                  } // iterator ends



                              }

                       }






        } // for each post





        for(Map.Entry<String,Set<Integer>> entry : casToPidsV2.entrySet()) {

            //difference
            Set<Integer> check = new TreeSet<Integer>( entry.getValue() );
            check.removeAll(  casToPIDS.get( entry.getKey() )   );
            //System.out.println(e);



            System.out.println(entry.getKey() + " " + casToNameVariants.get(entry.getKey()) + " found in pass 2: " + entry.getValue().size() + " found in pass one: "  + casToPIDS.get(entry.getKey()).size() +" check these: " + check);


        }



        //INTERSECTION LOGIC..
        //Set<String> s1;
        //Set<String> s2;
        //s1.retainAll(s2); // s1 now contains only elements in both sets
        //If you want to preserve the sets, create a new set to hold the intersection:

        //Set<String> intersection = new HashSet<String>(s1); // use the copy constructor
        //intersection.retainAll(s2);

       //Set<Integer> a = new TreeSet<Integer>(Arrays.asList(new Integer[]{0,2,4,5,6,8,10}));
       // Set<Integer> b = new TreeSet<Integer>(Arrays.asList(new Integer[]{5,6,7,8,9,10}));

        //union
        //Set<Integer> c = new TreeSet<Integer>(a);
        //c.addAll(b);
        //System.out.println(c);

        //intersection
        //Set<Integer> d = new TreeSet<Integer>(a);
        //d.retainAll(b);
        //System.out.println(d);

        //difference
        //Set<Integer> e = new TreeSet<Integer>(a);
        //e.removeAll(b);
        //System.out.println(e);







    }



}
