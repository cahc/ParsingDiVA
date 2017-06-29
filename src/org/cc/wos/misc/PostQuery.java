package org.cc.wos.misc;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;

/**
 * Created by crco0001 on 8/26/2016.
 */
public class PostQuery {

    private String q;

    //OBS!! PROBABLY WRONG SORT --> "yes"


    PostQuery(String SID, String queryString, boolean allTypes,boolean allLanguages, boolean allIndices) throws UnsupportedEncodingException {  //Constructor


        q = URLEncoder.encode("product", "UTF-8") + "=" + URLEncoder.encode("WOS","UTF-8");
        q += "&" + URLEncoder.encode("search_mode","UTF-8") + "=" + URLEncoder.encode("AdvancedSearch","UTF-8");

        q += "&" + URLEncoder.encode("SID","UTF-8") + "=" + URLEncoder.encode(SID,"UTF-8");

        q += "&" + URLEncoder.encode("input_invalid_notice","UTF-8") + "=" + URLEncoder.encode("Search Error: Please enter a search term.","UTF-8");

        q += "&" + URLEncoder.encode("input_invalid_notice_limits","UTF-8") + "=" + URLEncoder.encode("<br/>Note: Fields displayed in scrolling boxes must be combined with at least one other search field.","UTF-8");

        q += "&" + URLEncoder.encode("action","UTF-8") + "=" + URLEncoder.encode("search","UTF-8");

        q += "&" + URLEncoder.encode("replaceSetId","UTF-8") + "=" + URLEncoder.encode("","UTF-8");

        q += "&" + URLEncoder.encode("goToPageLoc","UTF-8") + "=" + URLEncoder.encode("SearchHistoryTableBanner","UTF-8");

        q += "&" + URLEncoder.encode("value(input1)","UTF-8") + "=" + URLEncoder.encode(queryString,"UTF-8");

        q += "&" + URLEncoder.encode("value(searchOp)","UTF-8") + "=" + URLEncoder.encode("search","UTF-8");

        q += "&" + URLEncoder.encode("x","UTF-8") + "=" + URLEncoder.encode("107","UTF-8");

        q += "&" + URLEncoder.encode("y","UTF-8") + "=" + URLEncoder.encode("436","UTF-8");

        q += "&" + URLEncoder.encode("value(select2)","UTF-8") + "=" + URLEncoder.encode("LA","UTF-8");

        //Choose language

        if(allLanguages) {

            q += "&" + URLEncoder.encode("value(input2)", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8");

        } else

        {     q += "&" + URLEncoder.encode("value(input2)", "UTF-8") + "=" + URLEncoder.encode("English", "UTF-8");           }

        q += "&" + URLEncoder.encode("value(select3)","UTF-8") + "=" + URLEncoder.encode("DT","UTF-8");

        //Choose document type

        if(allTypes) { q += "&" + URLEncoder.encode("value(input3)","UTF-8") + "=" + URLEncoder.encode("","UTF-8"); } else

        { q += "&" + URLEncoder.encode("value(input3)","UTF-8") + "=" + URLEncoder.encode("Article### Letter### Note### Proceedings Paper### Review","UTF-8"); }



        q += "&" + URLEncoder.encode("value(limitCount)","UTF-8") + "=" + URLEncoder.encode("14","UTF-8");

        q += "&" + URLEncoder.encode("limitStatus","UTF-8") + "=" + URLEncoder.encode("collapsed","UTF-8");

        q += "&" + URLEncoder.encode("ss_lemmatization","UTF-8") + "=" + URLEncoder.encode("On","UTF-8");

        q += "&" + URLEncoder.encode("ss_spellchecking","UTF-8") + "=" + URLEncoder.encode("Suggest","UTF-8");

        q += "&" + URLEncoder.encode("SinceLastVisit_UTC","UTF-8") + "=" + URLEncoder.encode("","UTF-8"); //THE FUCK?

        q += "&" + URLEncoder.encode("SinceLastVisit_DATE","UTF-8") + "=" + URLEncoder.encode("","UTF-8");

        q += "&" + URLEncoder.encode("period","UTF-8") + "=" + URLEncoder.encode("Range Selection","UTF-8");

        q += "&" + URLEncoder.encode("range","UTF-8") + "=" + URLEncoder.encode("ALL","UTF-8");

        q += "&" + URLEncoder.encode("startYear","UTF-8") + "=" + URLEncoder.encode("1945","UTF-8");

        int year = Calendar.getInstance().get(Calendar.YEAR);

        q += "&" + URLEncoder.encode("endYear","UTF-8") + "=" + URLEncoder.encode(String.valueOf(year),"UTF-8"); //OBS!

        //Choose indices

        if(allIndices) {

            q += "&" + URLEncoder.encode("editions", "UTF-8") + "=" + URLEncoder.encode("SCI", "UTF-8");

            q += "&" + URLEncoder.encode("editions", "UTF-8") + "=" + URLEncoder.encode("SSCI", "UTF-8");

            q += "&" + URLEncoder.encode("editions", "UTF-8") + "=" + URLEncoder.encode("AHCI", "UTF-8");

            q += "&" + URLEncoder.encode("editions", "UTF-8") + "=" + URLEncoder.encode("ISTP", "UTF-8");

            q += "&" + URLEncoder.encode("editions", "UTF-8") + "=" + URLEncoder.encode("ISSHP", "UTF-8");

        } else {

            q += "&" + URLEncoder.encode("editions", "UTF-8") + "=" + URLEncoder.encode("SCI", "UTF-8");

            q += "&" + URLEncoder.encode("editions", "UTF-8") + "=" + URLEncoder.encode("SSCI", "UTF-8");

            q += "&" + URLEncoder.encode("editions", "UTF-8") + "=" + URLEncoder.encode("AHCI", "UTF-8");


        }

        q += "&" + URLEncoder.encode("update_back2search_link_param","UTF-8") + "=" + URLEncoder.encode("yes","UTF-8");

        q += "&" + URLEncoder.encode("ss_query_language","UTF-8") + "=" + URLEncoder.encode("","UTF-8");

        q += "&" + URLEncoder.encode("rs_sort_by","UTF-8") + "=" + URLEncoder.encode("PY.D;LD.D;SO.A;VL.D;PG.A;AU.A","UTF-8");

    }


    public String get() {

        return q;
    }


}
