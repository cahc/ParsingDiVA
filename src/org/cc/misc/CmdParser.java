package org.cc.misc;

import org.apache.commons.cli.*;

/**
 * Created by crco0001 on 7/5/2016.
 */
public class CmdParser {

    Options options;
    String divaFileName;
    String norskListaFileName;
    String casFileName;
    String thesaurusFileName;
    String modelType;


    public void setModelType(String modelType) {

        this.modelType = modelType;

    }

    public String getModelType() {

        return this.modelType;
    }

    public String getDivaFileName() {
        return divaFileName;
    }

    public void setDivaFileName(String divaFileName) {
        this.divaFileName = divaFileName;
    }

    public String getNorskListaFileName() {
        return norskListaFileName;
    }

    public void setNorskListaFileName(String norskListaFileName) {
        this.norskListaFileName = norskListaFileName;
    }

    public String getCasFileName() {
        return casFileName;
    }

    public void setCasFileName(String casFileName) {
        this.casFileName = casFileName;
    }

    public String getThesaurusFileName() {
        return thesaurusFileName;
    }

    public void setThesaurusFileName(String thesaurusFileName) {
        this.thesaurusFileName = thesaurusFileName;
    }

    public static void printHelpAndExit(Options options) {

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(" java -jar ProgramName", options );
        System.exit(0);

    }

    public CmdParser(String[] args) {

    // create Options object
    this.options = new Options();

    Option help = new Option( "help", "Print this message" ); //boolean

    Option programVersion = new Option( "version", "print the version information and exit" ); //boolean

    Option divaFile   = Option.builder("divaDump")
            .hasArg()
            .desc(  "CSV file that contains a dump from DiVA" )
            .build();

     Option model = Option.builder("model")
             .hasArg()
             .desc("Specify bibliometric model, e.g., HF or LH")
             .build();

    Option norwegianFile   = Option.builder("norwegianList")
            .hasArg()
            .desc(  "Excel (.xlsx) file that contains the Norwegian list (both series and publishers)" )
            .build();

    Option casFile   = Option.builder("casFile")
            .hasArg()
            .desc(  "Excel (.xlsx) file that contains CAS with corresponding research time" )
            .build();

    Option thesaurusFile = Option.builder("thesaurus")
            .hasArg()
            .desc(  "Excel (.xlsx) file that contains standardisations w.r.t. series and publisher names" )
            .build();

    options.addOption(help);
    options.addOption(divaFile);
    options.addOption(norwegianFile);
    options.addOption(casFile);
    options.addOption(thesaurusFile);
    options.addOption(programVersion);
    options.addOption(model);
        CommandLineParser parser = new DefaultParser();

    if(args.length == 0) {

        printHelpAndExit(options);
    }


    CommandLine cmd = null;
    try { cmd = parser.parse( options, args); } catch (ParseException e) { System.out.println(e.getMessage()); System.exit(0); }

    if( cmd.hasOption("help") ) {

        printHelpAndExit(options);

    }

    if( cmd.hasOption("version") ) {

        System.out.println("Beta v0.8 2017-09-xx (release for new SamFak model)");
        System.exit(0);
    }


    if( cmd.hasOption( "divaDump" ) ) {

      setDivaFileName(cmd.getOptionValue( "divaDump" ) );
    } else { printHelpAndExit(options); }


    if( cmd.hasOption( "casFile" ) ) {

      setCasFileName(cmd.getOptionValue( "casFile" ) );
    } else { System.out.println("Missing mandatory arguments!"); System.out.println("Missing mandatory arguments!"); printHelpAndExit(options); }

    if( cmd.hasOption( "norwegianList" ) ) {

        setNorskListaFileName(cmd.getOptionValue( "norwegianList" ) );
    } else { System.out.println("Missing mandatory arguments!"); printHelpAndExit(options); }

    if( cmd.hasOption( "thesaurus" ) ) {

    setThesaurusFileName( cmd.getOptionValue( "thesaurus" ) );
    } else { System.out.println("Missing mandatory arguments!"); printHelpAndExit(options); }

   if(cmd.hasOption("model")) {

       String typ = cmd.getOptionValue("model").toLowerCase();

       if( !(typ.equals("hf") || typ.equals("lh"))  ) {System.out.println("Not a valid model argument.."); printHelpAndExit(options); }

       setModelType(typ.toUpperCase());

   }

}

}
