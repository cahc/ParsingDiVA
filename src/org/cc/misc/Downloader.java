package org.cc.misc;

import org.apache.commons.cli.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by crco0001 on 7/12/2016.
 */
public class Downloader {


    public static void printHelpAndExit(Options options) {

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "run with: ", options );
        System.exit(0);

    }


    public static void main(String[] args) throws URISyntaxException, IOException {

        Options options = new Options();

        Option help = new Option("help", "print this help message");
        Option min = Option.builder("min")
                .hasArg()
                .desc("earliest publication year (inclusive). e.g., 2011")
                .required()
                .build();
        Option max = Option.builder("max")
                .hasArg()
                .desc("latest publication year (inclusive). e.g., 2015")
                .required()
                .build();
        Option set = Option.builder("set")
                .hasArg()
                .required()
                .desc("subset to download: 1 (everything), 2 (UmU). e.g., 1")
                .build();

        options.addOption(help);
        options.addOption(min);
        options.addOption(max);
        options.addOption(set);

        CommandLineParser parser = new DefaultParser();

        if(args.length == 0) {
            System.out.println("Missing arguments..");
            printHelpAndExit(options);
        }


        CommandLine cmd = null;
        try { cmd = parser.parse( options, args); } catch (ParseException e) { System.out.println(e.getMessage()); System.exit(0); }

        if(cmd.hasOption("help") ) { printHelpAndExit(options); }


        if(cmd.getOptions().length != 3) { printHelpAndExit(options); }


        Integer minimum = null,maximum = null, downloadSet = null;
        try {
            minimum = Integer.valueOf(cmd.getOptionValue("min"));

            maximum = Integer.valueOf(cmd.getOptionValue("max"));

            downloadSet = Integer.valueOf(cmd.getOptionValue("set"));

        } catch (NumberFormatException e) {

            System.out.println("Felaktigt argument..");
            System.out.println(e.getMessage());
            printHelpAndExit(options);
        }


        if(maximum.compareTo(minimum) < 0 ) { System.out.println("max year < min year ... aborting."); printHelpAndExit(options); }

        if( !( downloadSet.equals(1) || downloadSet.equals(2) ) ) {System.out.println("set argument must be 1 or 2"); printHelpAndExit(options); }


        // HttpHost proxy = new HttpHost("localhost", 8888, "http");
        final RequestConfig params = RequestConfig.custom().setConnectTimeout(20000).setSocketTimeout(200000).build();

        String query = null;
        if(downloadSet.equals(1)) { query =  "[[{\"dateIssued\":{\"from\":\"" + minimum.toString() +"\",\"to\":\""+ maximum.toString() +"\"}},{\"publicationTypeCode\":[\"bookReview\",\"comprehensiveDoctoralThesis\",\"review\",\"monographDoctoralThesis\",\"article\",\"comprehensiveLicentiateThesis\",\"book\",\"monographLicentiateThesis\",\"chapter\",\"manuscript\",\"collection\",\"other\",\"conferencePaper\",\"patent\",\"conferenceProceedings\",\"report\",\"dataset\",\"dissertation\"]}]]"; }
        if(downloadSet.equals(2)) { query = "[[{\"dateIssued\":{\"from\":\""+ minimum.toString() +"\",\"to\":\""+ maximum.toString() +"\"}},{\"organisationId\":\"715\",\"organisationId-Xtra\":true},{\"publicationTypeCode\":[\"bookReview\",\"comprehensiveDoctoralThesis\",\"review\",\"monographDoctoralThesis\",\"article\",\"comprehensiveLicentiateThesis\",\"book\",\"monographLicentiateThesis\",\"chapter\",\"manuscript\",\"collection\",\"other\",\"conferencePaper\",\"patent\",\"conferenceProceedings\",\"report\",\"dataset\",\"dissertation\"]}]]"; }

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost("umu.diva-portal.org")
                .setPort(80)
                .setPath("/smash/export.jsf")
                .addParameter("format", "csvall2")
                .addParameter("aq", "[[]]")
                .addParameter("aqe", "[]")
                .addParameter("aq2",query)
                .addParameter("onlyFullText", "false")
                .addParameter("noOfRows", "99999")
                .addParameter("sortOrder", "title_sort_asc")
                .build();


        HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(params).build();

        HttpGet request = new HttpGet(uri);

        // add request header
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1");
        System.out.println("Sending request to sever. Please note that it can take several minutes for the server to start sending data..");

        HttpResponse response = client.execute(request);
        System.out.println("Response Code : "
                + response.getStatusLine().getStatusCode());

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line;

        String OutputWithtimeStamp = "divaExport("+minimum+"-"+maximum+")." + new SimpleDateFormat("yyyyMMdd_HHmm").format(Calendar.getInstance().getTime()) +"SET" + downloadSet.toString() + ".csv";

        System.out.println("Now starting to download bibliographic posts to: " + OutputWithtimeStamp + " (be patient)");

        int rows = 0;

        BufferedWriter writer = new BufferedWriter( new FileWriter(new File(OutputWithtimeStamp) )  );

        while ((line = rd.readLine()) != null) {

            writer.write(line);
            writer.newLine();

            rows++;
        }

        writer.flush();
        writer.close();
        rd.close();


        System.out.println(rows-1 + " bibliographic posts downloaded.");

    }



}

