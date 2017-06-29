package org.cc.wos.misc;

import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by crco0001 on 8/26/2016.
 */
public class Downloader {

    public static void writeLog(String fileName, String query, String from, String to, String language,String indices,String type) {



        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter("logfile.txt", true)));
            out.println(fileName + "\t" + "\"" + query + "\"" + "\t" + from + ":" + to + "\t" + language +" " + indices +" " + type +" "  + "OK!");

        } catch (IOException e) {
            System.err.println(e);
            System.exit(0);
        } finally {
            if (out != null) {
                out.flush();
                out.close();

            }
        }

    }

    public static void main(String[] args) throws MalformedURLException, UnsupportedEncodingException, InterruptedException {



        //For Anti-hammer
        Random rn = new Random();

        //Keep Alpha versions temporary

        Calendar now = Calendar.getInstance();
        Calendar expire = Calendar.getInstance();

        expire.set(2017,04,31);
        now.setTime(new Date());

        if ( now.after(expire) ) {System.out.println("The Alpha version has expired. Talk to cc for an updated version"); System.exit(1); }

        //Remove from final code

        if(args.length > 0) {
            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", "8888");
        }

        //For naming files based on download date/time

        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");

        //For automatic cookie control
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

        CookieHandler.setDefault(cookieManager);


        System.out.println("Web of Science Core Collection - Search and Retrieve (Beta 0.2 2016-08-xx)");
        System.out.println("Downloading TAB-separated (UTF-8) files in batch mode");
        System.out.println("Only for use within UmUB");

        URL url1 = new URL("http://www.webofknowledge.com/?DestApp=WOS&Error=Client.NullSessionID");

        HttpURLConnection conn = null;
        try {

            conn = (HttpURLConnection) url1.openConnection();

            conn.setInstanceFollowRedirects(true);
            conn.setFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/8.0.2 Safari/600.2.5");

        } catch (IOException e) { System.out.println("openConnection failed."); System.exit(1); }


        System.out.println("Trying to contact the server..");

        try {
            conn.connect();
            System.out.println("Messages: " + conn.getResponseMessage());
            System.out.println("Response code: " + conn.getResponseCode());

        } catch (IOException e) {

            System.out.println("Could not connect. Check internet connection?");
            System.exit(1);
        }


        //Check for cookies
        CookieStore cookies = cookieManager.getCookieStore();
        List<HttpCookie> c = cookies.getCookies();

        String SID = "null";

        for (int i = 0; i < c.size(); i++) {

            String name = c.get(i).getName();
            if(name.equals("SID")) SID = c.get(i).getValue();
            System.out.print(c.get(i).getName());

            System.out.print(" ");
            System.out.println(c.get(i).getValue());
        }

        if(SID.equals("null")) {System.out.println("Did not get the SID-cookie for some reason. Aborting."); System.exit(1); }

        //Now goto the advanced search

        url1 = new URL("http://apps.webofknowledge.com/WOS_AdvancedSearch_input.do?SID="+ SID +"&product=WOS&search_mode=AdvancedSearch");

        conn = null;
        try {

            conn = (HttpURLConnection) url1.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/8.0.2 Safari/600.2.5");

        } catch (IOException e) { System.out.println("openConnection failed."); System.exit(1); }


        System.out.println("Accessing advanced search mode");

        try {
            conn.connect();
            System.out.println("Messages: " + conn.getResponseMessage());
            System.out.println("Response code: " + conn.getResponseCode());

        } catch (IOException e) {

            System.out.println("Could not access advance search for some reason. Aborting.");
            System.exit(1);
        }


        /////////////////////////////////////////SETUP COMPLETE///////////////////////////////////////////


        ////////////////////////////////////////QUERY AND RESULTS//////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////////


        String resultsCount;
        String qid;
        String rurl;
        String summaryLink;
        String errorMessage;
        String noHits;
        BufferedReader reader;
        String line;
        String query;
        boolean allTypes;
        boolean allIndices;
        boolean allLanguages;
        boolean citedRefs;
        String indiceInfoLog;
        String typeInfoLog;
        String langInfoLog;

        Scanner sc = new Scanner(System.in);
        do {

            System.out.print("Input valid (\"advance-style\") WoS query:");

            query = sc.nextLine();

            System.out.print("Indices: SCI-EXPANDED and SSCI and A&HCI (1) also include CPCI-S and CPCI-SSH (2):" );

            allIndices = sc.nextLine().contains("2");

            if(allIndices) { indiceInfoLog = "SCI#SSCI#AHCI#CPCI-S#CPCI-SSH"; }  else  { indiceInfoLog = "SCI#SSCI#AHCI"; }

            System.out.print("Language: English (1) or all languages (2):");

            allLanguages = sc.nextLine().contains("2");

            if(allLanguages) { langInfoLog = "Language:ALL"; }  else  { langInfoLog = "Language:English"; }

            System.out.print("Type: Article, Review, Proceedings paper, Letter, Note (1) or all types (2):");

            allTypes = sc.nextLine().contains("2");

            if(allTypes) { typeInfoLog = "DocType:ALL"; }  else  { typeInfoLog = "DocType:Article#Letter#Note#Proceedings Paper#Review"; }


            System.out.print("Include cited references? NO (1) YES (2):");

            citedRefs = sc.nextLine().contains("2");

            PostQuery postFormQuery = new PostQuery(SID, query,allTypes,allLanguages,allIndices);

            url1 = new URL("http://apps.webofknowledge.com/WOS_AdvancedSearch.do");

            conn = null;
            try {

                conn = (HttpURLConnection) url1.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/8.0.2 Safari/600.2.5");
                conn.setRequestProperty("Connection", "keep-alive");
                conn.setRequestProperty("Origin", "http://apps.webofknowledge.com");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("charset", "utf-8");
                conn.setRequestProperty("Content-Length", "" + Integer.toString(postFormQuery.get().getBytes().length));
                conn.connect();
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(postFormQuery.get());
                wr.flush();
                wr.close();
            } catch (IOException e) {
                System.out.println("Error when submitting query");
                System.exit(1);
            }


            //Read response

            summaryLink = "null";
            errorMessage = "null";
            noHits = "null";

            try {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                //no lets try to find the summary.do-link
                //don't be greedy here!
                String regEx = "summary\\.do\\?product=WOS.*?back2search_link_param=yes"; // takes the first on page!!! this can be problematic if say there is several queries
                Pattern p = Pattern.compile(regEx);

                String regError = "<div class=\"errorText\" id=\"client_error_input_message\">Search Error:.*?</div>";
                Pattern p2 = Pattern.compile(regError);

                String regNoHits = "<div class=\"newErrorHead\">Your search found no records.*?</div>";
                Pattern p3 = Pattern.compile(regNoHits);

                Matcher m;
                Matcher m2;
                Matcher m3;

                while ((line = reader.readLine()) != null) {

                    m = p.matcher(line);
                    m2 = p2.matcher(line);
                    m3 = p3.matcher(line);

                    if(summaryLink.equals("null")) {
                        if (m.find()) {
                            summaryLink = m.group();
                            //reader.close();
                            //break;
                        }
                    }

                    if(errorMessage.equals("null")) {
                        if (m2.find()) {
                            errorMessage = m2.group();
                            //reader.close();
                            //break;
                        }
                    }


                    if(noHits.equals("null")) {
                        if (m3.find()) {
                            noHits = m3.group();
                            //reader.close();
                            //break;
                        }
                    }


                }

                reader.close();
            } catch (IOException e) {
                System.out.println("Error in reading response");
                System.exit(1);
            }


            if(!errorMessage.equals("null")) {

                System.out.println("Not a valid query! Consult WoS syntax.");
                System.out.println(errorMessage);
                System.exit(1);
            }

            if(!noHits.equals("null")) {

                System.out.println("No record matches query! Consult WoS syntax.");
                System.out.println(noHits);
                System.exit(1);
            }

            if (summaryLink.equals("null")) {
                System.out.println("No summary link found, but no IOException/Malformed query/or no hits occurred. Bug or server problem?");
                System.exit(1);
            }

            // System.out.println("Debug information:");
            //  System.out.println(summaryLink);


            url1 = new URL("http://apps.webofknowledge.com/" + summaryLink);


            //Find these values from the return of the summary.do-link so we can post a form for downloading//
            resultsCount = "-1";
            qid = "-1";
            rurl = "null";
            //////////////////////////////////////////////////////////////////////////////////////////////////

            try {
                conn = (HttpURLConnection) url1.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/8.0.2 Safari/600.2.5");
                conn.connect();
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                //Regex for the three

                String regExCount = "(?<=FINAL_DISPLAY_RESULTS_COUNT = )\\d+(?=;)";
                Pattern pCount = Pattern.compile(regExCount);
                String regExQid = "(?<=&qid=)\\d+(?=&)";
                Pattern pQid = Pattern.compile(regExQid);
                String regExrurl = "(?<=<input type=\"hidden\" id=\"rurl\" name=\"rurl\" value=\").*?(?=\" />)";
                Pattern pRurl = Pattern.compile(regExrurl);
                Matcher m;

                boolean BCount = false;
                boolean BQid = false;
                boolean Brurl = false;

                while ((line = reader.readLine()) != null) {

                    if (!BCount) {
                        m = pCount.matcher(line);
                        if (m.find()) {
                            resultsCount = m.group();
                            BCount = true;
                        }
                    }

                    if (!BQid) {
                        m = pQid.matcher(line);
                        if (m.find()) {
                            qid = m.group();
                            BQid = true;
                        }
                    }

                    if (!Brurl) {
                        m = pRurl.matcher(line);
                        if (m.find()) {
                            rurl = m.group();
                            Brurl = true;
                        }
                    }


                    //System.out.println(line);

                }
                reader.close();

            } catch (IOException e) {
                System.out.print("could not open connection");
                System.exit(0);
            }


            System.out.println("# document satisfying the query: " + resultsCount);
            System.out.println("(debug info) qid: " + qid);
            //System.out.println("rurl: " + rurl);

            if (resultsCount.equals("-1") | qid.equals("-1") | rurl.equals("null")) {
                System.out.println("count or qid or rurl missing");
                System.exit(0);
            }

            rurl = rurl.trim();

            System.out.print("Continue (1) or reformulate query (2):");

        } while(sc.nextLine().contains("2"));



        /////////////////////////////////////////////////////////////////////////////
        //NOW WE ARE SET UP FOR A LOOP OVER THE RESULTS AND DOWNLOAD THE WHOLE THING/
        /////////////////////////////////////////////////////////////////////////////

        url1 = new URL("http://apps.webofknowledge.com/OutboundService.do?action=go&&");

        String from;
        String to;

        while(true) {
            try {
                System.out.print("Start download from docID (e.g., 1):");
                from = sc.nextLine();
                System.out.print("Last docID to download (e.g., max returned if max<= 100,000):");
                to = sc.nextLine();

                int f = Integer.parseInt(from);
                int t = Integer.parseInt(to);
                int docMax = Integer.parseInt(resultsCount);

                if ((f > t) | (t > 100000) | (t>docMax)) {
                    System.out.println("From/To don't make sense..try again.");
                    throw new NumberFormatException();
                }

                break; //all ok

            } catch (NumberFormatException e) {

                System.out.println("From/To don't make sense..try again.");
            }

        }


        ///////////////////////////////LOOP FROM - TO/////////////////////////////////////////////////////////

        int start = Integer.parseInt(from);
        int stop  = Integer.parseInt(to);
        int total = stop-start+1;

        System.out.println("I will now download " + total + " records");

        int currenFrom = start;
        int currentTo;
        int batch = 1;





        while( currenFrom < stop   ) {



            //For the inner while loop
            int count = 0;
            int maxTries = 4;
            String fileName = dateFormat.format(new Date()) + ".txt";


            currentTo = Math.min(currenFrom-1+500,stop);



            while (true) {

                PostDownload download = new PostDownload(SID, qid, rurl, Integer.toString( currenFrom ),Integer.toString( currentTo ), query, citedRefs);

                PrintWriter writer = null;
                try {

                    conn = (HttpURLConnection) url1.openConnection();
                    conn.setInstanceFollowRedirects(false); //FOR DEBUGGING change to true
                    conn.setFollowRedirects(false); //FOR DEBUGGING change to true

                    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                    conn.setRequestProperty("Accept-Language", "en-us");
                    conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setRequestProperty("Origin", "http://apps.webofknowledge.com");
                    conn.setRequestProperty("Connection", "keep-alive");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/8.0.2 Safari/600.2.5");
                    conn.setRequestProperty("Referer", "http://apps.webofknowledge.com/" + summaryLink);
                    conn.setConnectTimeout(10000);


                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    //conn.setRequestProperty("Expect","100-continue");

                    conn.setFixedLengthStreamingMode(download.get().getBytes().length);

                    // conn.setRequestProperty("charset", "utf-8");
                    //conn.setRequestProperty("Content-Length", "" + Integer.toString(download.get().getBytes().length));
                    conn.connect();

                    OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
                    out.write(download.get());
                    out.flush();
                    out.close();


                    //Here we should get a ETS/ets.do link yes if mode in PostDownload is changed
                    //Well we get it if we do the selection..?
                    //String newUrl = conn.getHeaderField("Location");
                    //System.out.println(newUrl);


                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));


                    writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName, false)));

                    boolean fail = true;


                    while ((line = reader.readLine()) != null) {


                        if (fail) {
                            if (line.contains("AU\tBA")) fail = false; //simple check for headers

                        }


                        writer.println(line);

                    }
                    writer.flush();
                    writer.close();

                    if (fail) {

                        System.out.println("Unexpected data returned from server");

                        throw new IOException();
                    }


                    break; // break from while loop on success

                } catch (IOException e) {

                    if (++count == maxTries) {
                        e.printStackTrace(System.out);
                        System.out.println("Giving up.. Check log file and resume from last success at a later time.");
                        System.exit(1);
                    }

                    System.out.println("No valid response from server. I will sleep for some seconds and retry.");
                    Thread.sleep(6000);
                } finally {

                    if (writer != null) writer.close();
                }


            } // inner while loop ends

            writeLog(fileName, query, Integer.toString(currenFrom), Integer.toString(currentTo), langInfoLog, indiceInfoLog, typeInfoLog);

            System.out.println("Batch "+ batch + " complete. Taking a random sleep before batch " + (batch+1));
            Thread.sleep(rn.nextInt(15000));
            batch++;
            //update from to here
            currenFrom = currentTo+1;


        } //end of outer loop

        System.out.println("All done!");
    } //end of main

}//end of class
