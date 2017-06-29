package org.cc.wos.misc;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by crco0001 on 8/26/2016.
 */
public class PostDownload {

    private String q;


    PostDownload(String SID, String qid, String rurl, String from, String to, String query, boolean refs) throws UnsupportedEncodingException {

        q = URLEncoder.encode("viewType","UTF-8") + "=" + URLEncoder.encode("summary","UTF-8");

        q += "&" + URLEncoder.encode("selectedIds", "UTF-8") + "=" + URLEncoder.encode("","UTF-8");

        q += "&" + URLEncoder.encode("displayCitedRefs","UTF-8") + "=" + URLEncoder.encode("true","UTF-8");

        q += "&" + URLEncoder.encode("displayUsageInfo","UTF-8") + "=" + URLEncoder.encode("true","UTF-8");

        q += "&" + URLEncoder.encode("displayTimesCited","UTF-8") + "=" + URLEncoder.encode("true","UTF-8");

        q += "&" + URLEncoder.encode("viewType","UTF-8") + "=" + URLEncoder.encode("summary","UTF-8");

        q += "&" + URLEncoder.encode("product","UTF-8") + "=" + URLEncoder.encode("WOS","UTF-8");

        q += "&" + URLEncoder.encode("rurl","UTF-8") + "=" + URLEncoder.encode(rurl,"UTF-8");

        q += "&" + URLEncoder.encode("mark_id","UTF-8") + "=" + URLEncoder.encode("WOS","UTF-8");

        q += "&" + URLEncoder.encode("colName","UTF-8") + "=" + URLEncoder.encode("WOS","UTF-8");

        q += "&" + URLEncoder.encode("search_mode","UTF-8") + "=" + URLEncoder.encode("AdvancedSearch","UTF-8");

        q += "&" + URLEncoder.encode("locale","UTF-8") + "=" + URLEncoder.encode("en_US","UTF-8");

        q += "&" + URLEncoder.encode("view_name","UTF-8") + "=" + URLEncoder.encode("WOS-summary","UTF-8");

        q += "&" + URLEncoder.encode("sortBy","UTF-8") + "=" + URLEncoder.encode("PY.D;LD.D;SO.A;VL.D;PG.A;AU.A","UTF-8");

        q += "&" + URLEncoder.encode("mode","UTF-8") + "=" + URLEncoder.encode("outputService","UTF-8");

        //this is how it is done today.. will probably redirect to EST
        // q += "&" + URLEncoder.encode("mode","UTF-8") + "=" + URLEncoder.encode("OpenOutputService","UTF-8");

        q += "&" + URLEncoder.encode("qid","UTF-8") + "=" + URLEncoder.encode(qid,"UTF-8");

        q += "&" + URLEncoder.encode("SID","UTF-8") + "=" + URLEncoder.encode(SID,"UTF-8");

        q += "&" + URLEncoder.encode("format","UTF-8") + "=" + URLEncoder.encode("saveToFile","UTF-8");

        if(refs) {
            q += "&" + URLEncoder.encode("filters", "UTF-8") + "=" + URLEncoder.encode("PMID USAGEIND AUTHORSIDENTIFIERS ACCESSION_NUM FUNDING SUBJECT_CATEGORY JCR_CATEGORY LANG IDS PAGEC SABBR CITREFC ISSN PUBINFO KEYWORDS CITTIMES ADDRS CONFERENCE_SPONSORS DOCTYPE CITREF ABSTRACT CONFERENCE_INFO SOURCE TITLE AUTHORS  ", "UTF-8");

        } else {

            q += "&" + URLEncoder.encode("filters", "UTF-8") + "=" + URLEncoder.encode("PMID USAGEIND AUTHORSIDENTIFIERS ACCESSION_NUM FUNDING SUBJECT_CATEGORY JCR_CATEGORY LANG IDS PAGEC SABBR CITREFC ISSN PUBINFO KEYWORDS CITTIMES ADDRS CONFERENCE_SPONSORS DOCTYPE ABSTRACT CONFERENCE_INFO SOURCE TITLE AUTHORS  ", "UTF-8");

        }

        q += "&" + URLEncoder.encode("mark_to","UTF-8") + "=" + URLEncoder.encode(to,"UTF-8");

        q += "&" + URLEncoder.encode("mark_from","UTF-8") + "=" + URLEncoder.encode(from,"UTF-8");

        q += "&" + URLEncoder.encode("queryNatural","UTF-8") + "=" + URLEncoder.encode(query,"UTF-8");

        q += "&" + URLEncoder.encode("count_new_items_marked","UTF-8") + "=" + URLEncoder.encode("0","UTF-8");

        q += "&" + URLEncoder.encode("IncitesEntitled","UTF-8") + "=" + URLEncoder.encode("no","UTF-8");

        q += "&" + URLEncoder.encode("value(record_select_type)","UTF-8") + "=" + URLEncoder.encode("range","UTF-8");

        q += "&" + URLEncoder.encode("markFrom","UTF-8") + "=" + URLEncoder.encode(from,"UTF-8");

        q += "&" + URLEncoder.encode("markTo","UTF-8") + "=" + URLEncoder.encode(to,"UTF-8");

        if(refs) {
            q += "&" + URLEncoder.encode("fields_selection", "UTF-8") + "=" + URLEncoder.encode("PMID USAGEIND AUTHORSIDENTIFIERS ACCESSION_NUM FUNDING SUBJECT_CATEGORY JCR_CATEGORY LANG IDS PAGEC SABBR CITREFC ISSN PUBINFO KEYWORDS CITTIMES ADDRS CONFERENCE_SPONSORS DOCTYPE CITREF ABSTRACT CONFERENCE_INFO SOURCE TITLE AUTHORS  ", "UTF-8");

        } else {

            q += "&" + URLEncoder.encode("fields_selection", "UTF-8") + "=" + URLEncoder.encode("PMID USAGEIND AUTHORSIDENTIFIERS ACCESSION_NUM FUNDING SUBJECT_CATEGORY JCR_CATEGORY LANG IDS PAGEC SABBR CITREFC ISSN PUBINFO KEYWORDS CITTIMES ADDRS CONFERENCE_SPONSORS DOCTYPE ABSTRACT CONFERENCE_INFO SOURCE TITLE AUTHORS  ", "UTF-8");

        }
        q += "&" + URLEncoder.encode("save_options","UTF-8") + "=" + URLEncoder.encode("tabWinUTF8","UTF-8");




    }

    public String get() {

        return q;
    }


}
