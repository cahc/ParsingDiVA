package org.cc;

import org.cc.diva.DivaHelpFunctions;
import org.cc.misc.Thesaurus;

import java.io.File;
import java.io.IOException;

public class Playground {


    public static void main(String[] arg) throws IOException {

        Thesaurus standardiseringsListor = new Thesaurus( new File("C:\\Users\\crco0001\\OneDrive - Umeå universitet\\Desktop\\SAMFAK_2021\\FINAL\\Thesaurus20200901.xlsx"));

        String a = DivaHelpFunctions.extractSeriesName("Centrum för idrottsforskning");

        System.out.println(a);
        System.out.println( standardiseringsListor.replaceSerieBy(a) );





    }
}
