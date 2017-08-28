package org.cc.diva;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Created by crco0001 on 8/10/2017.
 */
public class DuplicationIdentifier {


    public static String simplifyTitle(String s) {

        //based on this: https://blog.mafr.de/2015/10/10/normalizing-text-in-java/
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        temp = temp.replaceAll("[^\\p{ASCII}]", "");
        temp = temp.toLowerCase();
        temp = temp.replaceAll("[^a-z0-9]", "");


        return temp;
    }


    public static int deDuplicate(List<Post> pList) {

        int dups = 0;

        List<String> reducedTitles = new ArrayList<>();

        for(Post p : pList) {


            reducedTitles.add( simplifyTitle( p.getTitle() ) );

        }

        //ignore these titles;
        HashSet<String> ignore = new HashSet<>();

        ignore.add( simplifyTitle("introduction") );
        ignore.add( simplifyTitle("introduktion") );
        ignore.add( simplifyTitle("förord") );
        ignore.add( simplifyTitle("editorial") );


        boolean[] availiableForCheck = new boolean[pList.size()];
        for(int i=0; i<availiableForCheck.length; i++) availiableForCheck[i] = true;

        NormalizedLevenshtein levenshtein = new NormalizedLevenshtein();
        for(int i=0; i<pList.size(); i++) {

            if( reducedTitles.get(i).length() <= 3 || ignore.contains( reducedTitles.get(i)) ) continue;

            Integer targetYear = pList.get(i).getYear();
            String targetType = pList.get(i).getDivaPublicationType();

            List<Integer> indicesOfDups = new ArrayList<>();
            indicesOfDups.add(i);

                    for(int j=(i+1); j<pList.size(); j++ ) {


                        Integer compareYear = pList.get(j).getYear();
                        String compareType = pList.get(j).getDivaPublicationType();


                        if(targetYear.equals(compareYear) && targetType.equals(compareType)) {

                            //calculate title similarity

                           // double sim = levenshtein.similarity( reducedTitles.get(i), reducedTitles.get(j) );

                           // if(sim >= 0.95) {

                           //  dups++;
                           // }


                            if( reducedTitles.get(i).equals(reducedTitles.get(j) )) {

                                dups++;
                                indicesOfDups.add(j);

                                pList.get(j).setDuplicate(true, pList.get(i).getPID() );

                            }


                        }


                    }


        }


        return dups;

    }





    public static void main(String[] arg) {


        System.out.println( simplifyTitle("apn OLa är död! fy2010-2011") );



    }



}
