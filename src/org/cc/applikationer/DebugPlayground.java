package org.cc.applikationer;

import org.cc.NorskaModellen.NorskFörlag;
import org.cc.NorskaModellen.NorskSerie;
import org.cc.NorskaModellen.ReadNorwegianLists;

import java.io.File;
import java.util.List;

public class DebugPlayground {

    public static void main(String[] args) throws Exception {


        List<NorskSerie> listaMedSerier = ReadNorwegianLists.parseSeries(new File("C:\\opt\\divaexporter_support_files\\norskalistan20260827.xlsx"));
        List<NorskFörlag> listaMedFörlag = ReadNorwegianLists.parseFörlag(new File("C:\\opt\\divaexporter_support_files\\norskalistan20260827.xlsx"));
        System.out.println("Antal serier i auktoritetsregister: " + listaMedSerier.size());
        System.out.println("Antal förlag i auktoritetsregister: " + listaMedFörlag.size());

    }
}
