package org.cc.applikationer;

import java.util.ArrayList;
import java.util.HashSet;

public class IdentifyPrefaceEtc {

    private final ArrayList<String> signalerarAvsaknadAvPeerRewview;
    public IdentifyPrefaceEtc() {

        signalerarAvsaknadAvPeerRewview = new ArrayList<>();
        signalerarAvsaknadAvPeerRewview.add( "Preface".toLowerCase() );
        signalerarAvsaknadAvPeerRewview.add( "Förord".toLowerCase() );
        signalerarAvsaknadAvPeerRewview.add( "Foreword".toLowerCase()  );

        signalerarAvsaknadAvPeerRewview.add( "Invited editorial".toLowerCase() );
        signalerarAvsaknadAvPeerRewview.add( "Guest editorial".toLowerCase() );
        signalerarAvsaknadAvPeerRewview.add( "Editorial".toLowerCase() );

        signalerarAvsaknadAvPeerRewview.add( "Inledning".toLowerCase() );
        signalerarAvsaknadAvPeerRewview.add( "Introduktion".toLowerCase() );
        signalerarAvsaknadAvPeerRewview.add( "Introduction".toLowerCase() );
        signalerarAvsaknadAvPeerRewview.add( "Efterord".toLowerCase()  );
        signalerarAvsaknadAvPeerRewview.add( "Afterword".toLowerCase() );

    }

    public String checkForPrefaceIntroEtcAndConsiderShareOfTotalTitleLength(String title) {

        String loweCasedTitle = title.toLowerCase();

        for(String s : this.signalerarAvsaknadAvPeerRewview) {

            if( loweCasedTitle.contains( s ) ) {

                int titleLength = loweCasedTitle.length();
                int signalLength = s.length();

                if(titleLength <= (signalLength * 2)) return title;

            }
        }

        return null;
    }



    public static void main(String[] arg) {

        IdentifyPrefaceEtc identifyPrefaceEtc = new IdentifyPrefaceEtc();
        System.out.println( identifyPrefaceEtc.checkForPrefaceIntroEtcAndConsiderShareOfTotalTitleLength("A cool INTRODUCTION" ) );


    }

}
