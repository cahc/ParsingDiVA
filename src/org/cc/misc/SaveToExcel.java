package org.cc.misc;


import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.*;
import org.cc.NorskaModellen.AggregatedAuthorInformation;
import org.cc.NorskaModellen.NorwegianMatchInfo;
import org.cc.NorskaModellen.PublicationPointPerAuthor;
import org.cc.applikationer.StandardImplementationWithPersonnelData;
import org.cc.applikationer.UMUID_YEARS_AT_UNITS;
import org.cc.diva.Author;
import org.cc.diva.Post;
import org.cc.diva.ReducedDiVAColumnIndices;


import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by crco0001 on 7/12/2016.
 */
public class SaveToExcel {

    private static void fillDocumentationTextBox(XSSFTextBox textBox, String docs) {
        textBox.clearText();

        String[] lines = docs.split("\\R");

        List<String[]> rows = new ArrayList<>();
        int maxLabelLen = 0;

        // simpler: run regex on trimmed content
        Pattern pattern = Pattern.compile("^(.+?)\\s{2,}(.+)$");

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                rows.add(new String[]{null, null});
                continue;
            }

            // 1) comment/explanation lines – ALWAYS non-column
            if (trimmed.startsWith("//")) {
                rows.add(new String[]{null, trimmed});
                continue;
            }

            // 2) no “  ” anywhere -> non-column
            if (!trimmed.matches(".*\\s{2,}.*")) {
                rows.add(new String[]{null, trimmed});
                continue;
            }

            // 3) otherwise try to split as LABEL  DESCRIPTION
            Matcher m = pattern.matcher(trimmed);
            if (m.matches()) {
                String label = m.group(1).trim();
                String desc  = m.group(2).trim();
                rows.add(new String[]{label, desc});
                if (label.length() > maxLabelLen) {
                    maxLabelLen = label.length();
                }
            } else {
                // fallback: non-column
                rows.add(new String[]{null, trimmed});
            }
        }

        if (maxLabelLen < 10) {
            maxLabelLen = 10;
        }

        final double fontSize = 11.0;
        final String typeface = "Courier New";
        final byte charset = 0;
        final byte pictAndFamily = 3;
        final boolean isSymbol = false;

        // 2. Build paragraphs
        for (String[] row : rows) {
            String label = row[0];
            String desc  = row[1];

            XSSFTextParagraph para = textBox.addNewTextParagraph();

            // blank line
            if (label == null && desc == null) {
                continue;
            }

            // === CASE 1: non-column lines (headings, //-explanations, etc.) ===
            if (label == null && desc != null) {
                // indent a bit relative to first column, via a dummy invisible run
                int indentChars = Math.max(2, maxLabelLen / 6); // tweak 2,3,4,...

                StringBuilder filler = new StringBuilder(indentChars);
                for (int i = 0; i < indentChars; i++) {
                    filler.append('x'); // any char, will be “hidden”
                }

                XSSFTextRun fillerRun = para.addNewTextRun();
                fillerRun.setText(filler.toString());
                fillerRun.setFontSize(fontSize);
                fillerRun.setFontFamily(typeface, charset, pictAndFamily, isSymbol);
                // make it invisible by using white (sheet background) – if that
                // ends up visible for you, you’ll see the x’s and can adjust.
                fillerRun.setFontColor(java.awt.Color.WHITE);

                XSSFTextRun run = para.addNewTextRun();
                run.setText(desc);
                run.setFontSize(fontSize);
                run.setFontFamily(typeface, charset, pictAndFamily, isSymbol);

                continue;
            }

            // === CASE 2: normal LABEL  DESCRIPTION lines ===
            if (label != null) {
                String paddedLabel = String.format("%-" + maxLabelLen + "s  ", label);

                XSSFTextRun labelRun = para.addNewTextRun();
                labelRun.setText(paddedLabel);
                labelRun.setBold(true);
                labelRun.setFontSize(fontSize);
                labelRun.setFontFamily(typeface, charset, pictAndFamily, isSymbol);
            }

            if (desc != null && !desc.isEmpty()) {
                XSSFTextRun descRun = para.addNewTextRun();
                descRun.setText(desc);
                descRun.setFontSize(fontSize);
                descRun.setFontFamily(typeface, charset, pictAndFamily, isSymbol);
            }
        }

        textBox.setWordWrap(true);
    }



    private static final String documentationTextTemplate = new String("""
            Bibliografisk databas:                   https://umu.diva-portal.org
            Auktoritetsregister ("norsk modell"):    https://kanalregister.hkdir.no/ ; https://npi.hkdir.no/
            Personaldata:                            https://delta.ub.umu.se
            Exportdatum:                             %s
    
            **Variabelnamn**                        **Beskrivning**
    
            PID                                     DIVA ID
            AUTHOR_ID                               INTERNET DISAMBIGUERINGSID
            NAME                                    FÖRFATTARNAMN
            UMUID	                                UMUID
            UMUID (FIX MISSING IF POSSIBLE)         AUTOMATISKT KOMPLETTERAT, OM MÖJLIGT
            STRAIGHT FRACTIONS                      RAKA FÖRFATTARFRAKTIONER, 1/N
            STRAIGHT FRACTIONS (MINIMUM 0.1)	    MAX(0.1, STRAIGHT FRACTIONS)
            BYLINE WEIGHTED FRACTIONS (U-SHAPED)    FÖRSTA OCH SISTA FÖRFATTARE VIKTAS HÖGST, MEDIANPOSITIONEN LÄGST, "U-FORMAD":
    
                    // För en post med n författare ges en relativ vikt till författaren med den i:te positionen w(n,i) enligt:
                    // w(n,i) = (1 + |n + 1 - 2i|) / n
                    // Således gäller att 1/n <= w <= 1 och första samt sista författaren ges 1.0 och vikter till författare nära medianen rör sig mot 1/n
                    // Dessa relativa vikter normaliseras sedan så att summan över publikationen = 1.0.
                    // Se t.ex. "Author contributions and allocation of authorship credit: testing the validity of different counting methods in the field of chemical biology" https://link.springer.com/article/10.1007/s11192-023-04680-y
                    // I undantagsfall registreras endast en delmängd av författarna explicit och det totala antalet författare anges i stället separat som ett heltal. I dessa fall kan inte viktning som tar hänsyn till ordning utföras (rak fraktionering används då)
    
            BYLINE WEIGHTED FRACTIONS (LiU/UMU-MODEL)   VIKTER FÖR FÖRSTA OCH SISTA FÖRFATTARE > VIKTER FÖR NÄST FÖRSTA OCH NÄST SISTA FÖRFATTARE > RESTERANDE:
    
                    // För en post med n författare ges en relativ vikt till författarna enligt:
                    // a. En 4-2-1 regel: första och sista får 4, näst första och näst sista får 2, resterande 1. Dessa vikter normaliseras så att summan över publikationen = 1.0.
                    // b. Sedan kontrolleras att följande håller: första och sista får MINST 1/4, näst första och näst sista får MINST 1/8. Om inte justeras dessa fraktioner och "mellanförfattarna" får dela på det som blir över.
                    // Ursprunget till denna vikting är en implementering vid LiU (ingen formell beskrivning existerar)
                    // I undantagsfall registreras endast en delmängd av författarna explicit och det totala antalet författare anges i stället separat som ett heltal. I dessa fall kan inte viktning som tar hänsyn till ordning utföras (rak fraktionering används då)
    
            AUTHOR BYLINE CATEGORY	                FÖRFATTARENS POSITION, GRUPPERAD: FRIST, LAST, MIDDLE, UNDETERMINABLE. Den senare kategorin då inte samtliga författare är explicit registrerade (endast rak fraktionalisering möjlig)
            DOUBLE COUNTING INDUCED BY CENTRUM-INSTITUTION COMBINATION  {FALSE, TRUE} : ÄR INDIVIDEN REGISTRERAD MED BÅDE EN INSTITUTION OCH "CENTRUM-LIKA" ENHETER? OM JA SÅ *INTERNFRAKTIONALISERAR* VI INTE PÅ CENTRUM. FILTER = FALSE NÖDVÄNDIGT FÖR ANALYSER FÖRFATTARNIVÅ!
            CENTRUM LIKE UNIT                       {TRUE, FALSE} ÄR DET ETT CENTRUM ELLER CENTRUM-LIK ENHET? FÖRFATTARFRAKTIONER TILLSKRIVS I SIN HELTHET TILL DESSA, INGEN INTERNFRAKTIONALISERING. MÅSTE SÄRBEHANDLAS I SAMMANSTÄLLNINGAR!
            TOTAL NUMBER OF AUTHORS	                TOTALT ANTAL REGISTRERADE FÖRFATTARE
            POSITION CLOSEST TO PUBLICATION YEAR    BEFATTNINGSKATEGORI VID PUBLICERINGSÅRET, ELLER APPROXIMATION OM KATEGORI FÖR GIVET ÅR SAKNAS I PERSONALDATA (ANSTÄLLNINGI FÖRSTA HAND, ANNARS ANKNYTNING)
            LAST KNOWN POSITION @ UMU	            SENAST KÄNDA BEFATTNINGSKATEGORI, EJ KOPPLAT TILL PUBLIKATIONENS PUBLICERINGSÅR (ANSTÄLLNINGI FÖRSTA HAND, ANNARS ANKNYTNING)
            FACULTY	                                FAKULTET
            INSTITUTION	                            INSTITUTION (I FÖREKOMMANDE FALL MAPPAD TILL EFTERFÖLJANDE OM NEDLAGGD IDAG)
            UNIT (IF AVAILABLE OTHERWISE INST.)	    LÄGRE ORGANISATORISK ENHET OM SÅDAN FINNS REGISTRERAD (MAPPAD)
            DIVA_TYPE	                            PUBLIKATIONSTYP
            DIVA_YEAR	                            REGISTRERAT PUBLIKATIONSÅR
            DIVA_CONTENT	                        INNEHÅLLSKLASSIFICERING (KOLLEGIALGRANSKNINGSNIVÅ)
            DIVA_AFFILIATIONS	                    EJ BEARBETAD/MAPPAD REGISTRERAD DIVA-ENHET
            DIVA_JOURNAL	                        BIBLIOGRAFISK PUBLICERINGSKANALSINFORMATION
            DIVA_JOURNAL_ISSN	                    BIBLIOGRAFISK PUBLICERINGSKANALSINFORMATION
            DIVA_SERIES	                            BIBLIOGRAFISK PUBLICERINGSKANALSINFORMATION
            DIVA_SERIES_ISSN	                    BIBLIOGRAFISK PUBLICERINGSKANALSINFORMATION
            DIVA_PUBLISHER	                        BIBLIOGRAFISK PUBLICERINGSKANALSINFORMATION
            DIVA_ISBN                               BIBLIOGRAFISK PUBLICERINGSKANALSINFORMATION
            DIVA_LANGUAGE	                        SPRÅK
            DIVA_LAST_CHANGED	                    SENAST POSTEN JUSTERATS I DIVA
            DIVA_IS_DUPLICATED	                    OM DUBBLETT (ATOMATISKT FLAGGAD)
            DIVA_DUPLICATED_PID	                    I FÖREKOMMANDE FALL, VAD ANSES VARA DUBBLETT
            NORWEGIAN_ID	                        ID FRÅN DET NORSKA KANALREGISTRET
            NORWEGIAN_TYPE	                        SERIE ELLER FÖRLAG I DET NORSKA KANALREGISTRET
            NORWEGIAN_MATCH	                        HUR HAR MATCHNINGEN MELLAN DIVA OCH NORSKA REGISTRET GJORTS
            NORWEGIAN_NAME	                        KANALENS NAMN I NORSKA REGISTRER
            NORWEGIAN_LEVEL                         {0,1,2,"not available"} : NORSK NIVÅ, VID AKTUELLT PUBLICERINGSÅR (STANDARD)
            NORWEGIAN_LEVEL (OMKODAD)               {1,2, "0 (ink. ej bedömd kanal)"} : NORSK NIVÅ, VID AKTUELLT PUBLICERINGSÅR, NIVÅ 0 & ej bedömda kanaler sammanslagna.
            NORWEGIAN_LEVEL (HISTORICAL MAXIMUM)    NORSK NIVÅ, HISTORISKT MAXIMUM (ANVÄNDS I REGEL EJ)
            NORWEGIAN_MODEL_INFO	                INFORMATION RÖRANDE KANALEN NIVÅ, ELLER OM POSTEN ÄR UPPFYLLER KRAVEN FÖR ATT BEAKTAS I MODELLEN
            PUBLICATION_MODEL_INFO	                INFORMATION RÖRANDE KANALEN NIVÅ, ELLER OM POSTEN ÄR UPPFYLLER KRAVEN FÖR ATT BEAKTAS I MODELLEN
            NORWEGIAN_POINTS	                    STANDARD "POÄNG" PER NIVÅ OCH PUBLIKATIONSTYP
            DOI	                                    PUBLIKATIONSID, DOI
            EID	                                    PUBLIKATIONSID, SCOPUS
            WOS	                                    PUBLIKATIONSID, Web of Science
            PMID	                                PUBLIKATIONSID, PUBMED
            ANY_UMU_AUTHOR_ON_RECORD_LEVEL	        HAR DIVA-POSTEN MINST EN FÖRFATTARE REGISTRERAD MED EN UMU-ENHET
            NORWEGIAN_POINTS_TIMES_FRACTION (STRAIGHT)  NORWEGIAN_POINTS * STRAIGHT FRACTIONS
            NORWEGIAN_POINTS_TIMES_FRACTION (STRAIGHT, MIN 0.1)     NORWEGIAN_POINTS * STRAIGHT FRACTIONS (MINIMUM 0.1)
            NORWEGIAN_POINTS_TIMES_FRACTION (WEIGHTED, U-SHAPED)    NORWEGIAN_POINTS * BYLINE WEIGHTED FRACTIONS (U-SHAPED)
    
            FLIKEN: FÖRFATTARE_ÅR_VID_UMU            FÖR VARJE GILTIGT UMUID I MATERIALET REDOVISAS HUR MÅNGA ÅR DETTA UMUID ÄR KOPPLAT TILL EN ANSTÄLLNING OCH/ELLER ANKNYTNING TILL UMU, TOTALT SAMT PER FAKULTET INOM DET AKTUELLA PUBLICERINGSFÖNSTRET
            FLIKEN: ABSTRACTS                        I FÖREKOMMANDE FALL, KOPPLING MELLAN PID OCH ABSTRACT
    
            //Utfall i norska modellen ges genom att - för valda enheter - summera över NORWEGIAN_POINTS_TIMES_FRACTION (*) där * är vald fraktionalisering (MIN 0.1, i ursprunglig definition av norska modellen).
            //DOUBLE COUNTING INDUCED BY CENTRUM-INSTITUTION COMBINATION samt CENTRUM LIKE UNIT måste explicit hanteras utifrån syfte med analysen.
            //För ytterligare information se: Norska modellen - implementering 2026.pdf
    """);



    XSSFWorkbook workbook = new XSSFWorkbook();


    public SaveToExcel() {}


    public int addFractionForConsistency(XSSFSheet sheet, Post p, int rowIndices, double fractionToAdd) {

        NorwegianMatchInfo norskNivå = p.getNorskNivå();

        Row row = sheet.createRow(++rowIndices);
        int cellIndices = -1;

        //PID  0
        Cell cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getPID());

        //AUTHOR_ID" 1

        cell = row.createCell(++cellIndices);
        cell.setCellValue("XA0");


        //AUTHOR_NAME" 2

        cell = row.createCell(++cellIndices);
        cell.setCellValue("indeterminable");

        //CAS 1" 3

        cell = row.createCell(++cellIndices);
        cell.setCellValue("not available");

        //CAS 2" 4

        cell = row.createCell(++cellIndices);
        cell.setCellValue("not available");

        //CAS STRAIGHT FRACTIONS 5

        cell = row.createCell(++cellIndices);
        cell.setCellValue( fractionToAdd );

        // STRAIGHT FRACTIONS MIN 0.1 6
        cell = row.createCell(++cellIndices);
        cell.setCellValue( fractionToAdd );


        //BYLINE WEIGHTED A 7

        cell = row.createCell(++cellIndices);
        cell.setCellValue( fractionToAdd );


        //BYLINE WEIGHTED B 8

        cell = row.createCell(++cellIndices);
        cell.setCellValue( fractionToAdd );


        //BYLINE CATEGORY 9

        cell = row.createCell(++cellIndices);
        cell.setCellValue( "UNDETERMINABLE" );

        //IMPORTANT
        //DOUBLE COUNTING INDUCED BY CENTRUM-INSTITUTION COMBINATION
        cell = row.createCell(++cellIndices);
        cell.setCellValue(false);

        //IS CENTRUM LIKE
        cell = row.createCell(++cellIndices);
        cell.setCellValue(false);



        //NR AUTHORS 10

        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getNrAuthors() );

        //position @ pubyear 11

        cell = row.createCell(++cellIndices);
        cell.setCellValue( "" );

        //Last known position 12

        cell = row.createCell(++cellIndices);
        cell.setCellValue( "" );



        //FACULTY 13
        String faculty = "external"; //( isUmuAuthor ) ? divaIDtoNames.get(indice).getFAKULTET() : "external";
        cell = row.createCell(++cellIndices);
        cell.setCellValue(faculty);

        //INST 14

        String inst = "external"; // ( isUmuAuthor ) ? divaIDtoNames.get(indice).getINSTITUTION() : "external";
        cell = row.createCell(++cellIndices);
        cell.setCellValue(inst);


        //INST 15 UNIT

        String unit = "external"; //  ( isUmuAuthor ) ? divaIDtoNames.get(indice).getENHET() : "external";
        cell = row.createCell(++cellIndices);
        cell.setCellValue(unit);

        //SET DIVA BIBLIOGRAPHIC STUFF

        //DIVA TYPE 16
        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getDivaPublicationType()  );


        //DIVA YEAR 17
        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getYear()  );

        //DIVA CONTENT 18
        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getDivaContentType()  );

        //DIVA SUBTYPE 19
        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getDivaPublicationSubtype()  );

        //DIVA status 20
        cell = row.createCell(++cellIndices);

        String status = p.getDivaStatus();

        if("".equals(status)) {status = "published";}

        cell.setCellValue( status );

        //DIVA AFFILIATIONS 21
        cell = row.createCell(++cellIndices);

        String name = p.getRawDataRow()[ ReducedDiVAColumnIndices.Name.getValue() ];
        if(name.length() > 32700 ) { name = name.substring(0,32000); name = name.concat("[TRUNCATED!!]");       }
        cell.setCellValue( name );

        //DIVA URN 22

        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getNBN()  );

        //DIVA CHANEL 23

        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getDivaChannels()  );

        //DIVA TITLE 24

        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getTitle()  );


        //DIVA JOURNAL 26

        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getJournal()  );

        //DIVA JOURNAL ISSN 27

        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getJournalISSN()  );


        //DIVA SERIES 28

        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getSeriesName()  );


        //DIVA SERIES ISSN 29

        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getSeriesISSN()  );

        //DIVA PUBLISHER 30

        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getPublisher()  );

        //DIVA PUBLISHER 31

        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getISBN()  );



        //DIVA LANGUAGE 32

        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Language.getValue() ] );

        //DIVA LAST UPDATED  33

        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.LastUpdated.getValue() ] );

        //DIVA NR AUTHORS 34
        // cell = row.createCell(++cellIndices);
        // cell.setCellValue(p.getNrAuthors());


        //DIVA IS DUPE 35
        cell = row.createCell(++cellIndices);
        cell.setCellValue(  p.isDuplicate() );


        //DIVA WHICH DUPE 36
        cell = row.createCell(++cellIndices);
        cell.setCellValue(  p.getDuplicateOfPID() );


        //SET NORWEGIAN STUFF

        //NORWEGIAN ID 37

        cell = row.createCell(++cellIndices);

        Integer nid = norskNivå.getNorsk_id();
        if(nid.equals(-99)) { cell.setCellValue( "not available" ); } else {cell.setCellValue( nid ); }


        //NORWEGIAN TYPE 38

        cell = row.createCell(++cellIndices);
        String type = norskNivå.getType();
        cell.setCellValue((type == null) ? "not available" : type);

        //NOWRWEGIAN MATCH 39

        cell = row.createCell(++cellIndices);
        String how = norskNivå.getHow();
        cell.setCellValue((how == null) ? "not available" : how);

        //NOWRWEGIAN NAME 40

        cell = row.createCell(++cellIndices);
        String namn = norskNivå.getNorsk_namn();
        cell.setCellValue((namn == null) ? "not available" : namn);

        //NORWEGIAN LEVEL 41

        cell = row.createCell(++cellIndices);
        Integer nivåStandard = norskNivå.getNivå();
        cell.setCellValue((nivåStandard == null) ? "not available" : nivåStandard.toString());


        //OMKODAD LEVEL , 0 + "not available" =  0 (ink. ej bedömd kanal)


        cell = row.createCell(++cellIndices);
        cell.setCellValue( ((nivåStandard == null) || nivåStandard.equals(0)) ? "0 (ink. ej bedömd kanal)" : nivåStandard.toString());

        //NORWEGIAN LEVEL 42

        cell = row.createCell(++cellIndices);
        Integer nivåMax = norskNivå.getMax_nivå();
        cell.setCellValue((nivåMax == null) ? "not available" : nivåMax.toString());


        //NORWEGIAN MODEL SPECIFIC INFO 43
        cell = row.createCell(++cellIndices);
        cell.setCellValue(     norskNivå.getModelSpecificInfo() );

        //PUBLICATION MODEL SPECIFICATION 44
        cell = row.createCell(++cellIndices);
        cell.setCellValue(     p.getStatusInModel().getStatusInModel()  );


        //NORWEGIAN POINTS 45

        cell = row.createCell(++cellIndices);
        cell.setCellValue(      norskNivå.getVikt()     );


        //add localID 46

        cell = row.createCell(++cellIndices);
        cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.LocalId.getValue() ] );

        //affil mapping info 47, we dont remove any longer
        //cell = row.createCell(++cellIndices);
        //cell.setCellValue(author.removedNonConsidered );

        //ADD DOI 48

        cell = row.createCell(++cellIndices);
        cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.DOI.getValue() ] );

        //ADD EID 49

        cell = row.createCell(++cellIndices);
        cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.ScopusId.getValue() ] );

        //ADD WOS, 50

        cell = row.createCell(++cellIndices);
        cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.ISI.getValue() ] );

        //ADD PMID 51

        cell = row.createCell(++cellIndices);
        cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.PMID.getValue() ] );

        //any UMU author 52

        cell = row.createCell(++cellIndices);
        cell.setCellValue(p.isHasUMUAuthors());

        //norwegian points, four different versions based on author fraction type

        //STRAIGHT
        cell = row.createCell(++cellIndices);
        cell.setCellValue(fractionToAdd * p.getNorskNivå().getVikt() );

        //MIN 0.1
        cell = row.createCell(++cellIndices);
        cell.setCellValue( fractionToAdd * p.getNorskNivå().getVikt()  );

        //U-SHAPED
        cell = row.createCell(++cellIndices);
        cell.setCellValue(fractionToAdd * p.getNorskNivå().getVikt() );

        //LiU Style
        cell = row.createCell(++cellIndices);
        cell.setCellValue(fractionToAdd * p.getNorskNivå().getVikt() );



        return rowIndices;


    }

    public void saveDisambiguatedAuthorFractions(List<Post> recordList, boolean includeExternalAuthors) {



        XSSFSheet sheet = workbook.createSheet("AuthorFracs");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        //SKAPA EN LÅST HEADER-RAD
        Row row = sheet.createRow(rowIndices);


        Cell cell = row.createCell(0);
        cell.setCellValue("PID" );
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("AUTHOR_ID" );
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("NAME" );
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("CAS1" );
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("CAS2" );
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("FRACTIONS (STANDARD)" );
        cell.setCellStyle(style);


        cell = row.createCell(6);
        cell.setCellValue("FRACTIONS (STANDARD + MIN0.1)" );
        cell.setCellStyle(style);

        cell = row.createCell(7);
        cell.setCellValue("FRACTIONS (NO INTERNAL FRACTIONS)" );
        cell.setCellStyle(style);


        cell = row.createCell(8);
        cell.setCellValue("FRACTIONS (NO INTERNAL FRACTIONS + MIN0.1)" );
        cell.setCellStyle(style);

        cell = row.createCell(9);
        cell.setCellValue("FACULTY FRACTION" );
        cell.setCellStyle(style);




        cell = row.createCell(10);
        cell.setCellValue("FACULTY" );
        cell.setCellStyle(style);

        cell = row.createCell(11);
        cell.setCellValue("INSTITUTION" );
        cell.setCellStyle(style);


        cell = row.createCell(12);
        cell.setCellValue("UNIT (IF AVAILABLE OTHERWISE INST.)" );
        cell.setCellStyle(style);

        //BIBLIOGRAPHIC INFO

        cell = row.createCell(13);
        cell.setCellValue("DIVA_TYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(14);
        cell.setCellValue("DIVA_YEAR" );
        cell.setCellStyle(style);

        cell = row.createCell(15);
        cell.setCellValue("DIVA_CONTENT" );
        cell.setCellStyle(style);

        cell = row.createCell(16);
        cell.setCellValue("DIVA_SUBTYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(17);
        cell.setCellValue("DIVA_STATUS" );
        cell.setCellStyle(style);

        cell = row.createCell(18);
        cell.setCellValue("DIVA_AFFILIATIONS" );
        cell.setCellStyle(style);

        cell = row.createCell(19);
        cell.setCellValue("DIVA_URN" );
        cell.setCellStyle(style);


        cell = row.createCell(20);
        cell.setCellValue("DIVA_POTENTIAL_CHANNELS" );
        cell.setCellStyle(style);

        cell = row.createCell(21);
        cell.setCellValue("DIVA_TITLE" );
        cell.setCellStyle(style);

        cell = row.createCell(22);
        cell.setCellValue("DIVA_ABSTRACT" );
        cell.setCellStyle(style);

        cell = row.createCell(23);
        cell.setCellValue("DIVA_JOURNAL" );
        cell.setCellStyle(style);

        cell = row.createCell(24);
        cell.setCellValue("DIVA_JOURNAL_ISSN" );
        cell.setCellStyle(style);


        cell = row.createCell(25);
        cell.setCellValue("DIVA_SERIES" );
        cell.setCellStyle(style);


        cell = row.createCell(26);
        cell.setCellValue("DIVA_SERIES_ISSN" );
        cell.setCellStyle(style);

        cell = row.createCell(27);
        cell.setCellValue("DIVA_PUBLISHER" );
        cell.setCellStyle(style);

        cell = row.createCell(28);
        cell.setCellValue("DIVA_ISBN" );
        cell.setCellStyle(style);


        cell = row.createCell(29);
        cell.setCellValue("DIVA_LANGUAGE" );
        cell.setCellStyle(style);

        cell = row.createCell(30);
        cell.setCellValue("DIVA_LAST_CHANGED" );
        cell.setCellStyle(style);

        cell = row.createCell(31);
        cell.setCellValue("DIVA_NR_AUTHORS" );
        cell.setCellStyle(style);

        cell = row.createCell(32);
        cell.setCellValue("DIVA_IS_DUPLICATED" );
        cell.setCellStyle(style);

        cell = row.createCell(33);
        cell.setCellValue("DIVA_DUPLICATED_PID" );
        cell.setCellStyle(style);

        //NORSK STUFF


        cell = row.createCell(34);
        cell.setCellValue("NORWEGIAN_ID" );
        cell.setCellStyle(style);


        cell = row.createCell(35);
        cell.setCellValue("NORWEGIAN_TYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(36);
        cell.setCellValue("NORWEGIAN_MATCH" );
        cell.setCellStyle(style);


        cell = row.createCell(37);
        cell.setCellValue("NORWEGIAN_NAME" );
        cell.setCellStyle(style);

        cell = row.createCell(38);
        cell.setCellValue("NORWEGIAN_LEVEL" );
        cell.setCellStyle(style);

        cell = row.createCell(39);
        cell.setCellValue("NORWEGIAN_LEVEL (HISTORICAL MAXIMUM)" );
        cell.setCellStyle(style);

        cell = row.createCell(40);
        cell.setCellValue("NORWEGIAN_MODEL_INFO" );
        cell.setCellStyle(style);

        cell = row.createCell(41);
        cell.setCellValue("NORWEGIAN_POINTS" );
        cell.setCellStyle(style);

        //add localID, use to manually remap institutionen för geografi och ekonomisk historia

        cell = row.createCell(42);
        cell.setCellValue("LOCAL_ID");
        cell.setCellStyle(style);

        //add info if the affiliation has been altered, i.e., non considered removed (0=no, 1=yes, 2=count not remove, only non considered

        cell = row.createCell(43);
        cell.setCellValue("AFFIL_MAPPING_INFO");
        cell.setCellStyle(style);


        for(Post p : recordList) {

            for(Author author : p.getAuthorList() ) {


                List<DivaIDtoNames> divaIDtoNames = author.getAffilMappingsObjects();
                boolean isUmuAuthor = divaIDtoNames.size() > 0;

                if(!includeExternalAuthors && !isUmuAuthor) continue;


                NorwegianMatchInfo norskNivå = p.getNorskNivå();
                
                
                //
                //Count number of FACULTIES an author is associated with. This is needed for fractionalization that dont d´split an author fraction over faculties but split on inst/unit on a lower level
                //
                
                Map<String, Integer> facultyToOccurance = null;
                boolean multipleUmUaffils = (divaIDtoNames.size() > 1);
                
                if(multipleUmUaffils) {
                    
                    facultyToOccurance = new HashMap<>();
                    
                    for(DivaIDtoNames id : divaIDtoNames) {
                        
                        String facultyName = id.getFAKULTET();
                        
                        Integer occurrence = facultyToOccurance.get(facultyName);
                        if(occurrence == null) {
                            
                            facultyToOccurance.put(facultyName,1);
                        } else {
                            
                            facultyToOccurance.put(facultyName, (occurrence+1) );
                            
                        }
                        
                        
                        
                    }//for ends
                    
                    
                }
                
                
                
                ////End special treatment for non frac over Faculties
                

                int indice = 0;
                do {
                    row = sheet.createRow(++rowIndices);
                    cellIndices = -1;

                    //PID 1
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getPID());

                    //AUTHOR_ID" 2

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getDisambiguateID());


                    //AUTHOR_NAME" 3

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getAuthorName() );

                    //CAS 1" 4

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getCas() );

                    //CAS 2" 5

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getAutomaticAddedCass() );

                    //CAS FRAC STANDARD 6

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getFractionConsiderMultipleUmUAffils() );

                    // NEW NEW 7
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getFractionConsiderMultipleUmUAffilsMin01() );

                    //NEW 2022-11-16, fractions ignoring internal, and no min01

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getFractionIgnoreMultipleUmUAffils() );


                    //CAS FRAC Min.01 AND Ignore multiple UmU-affils 8

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getFractionIgnoreMultipleUmUAffilsMin01() );


                    //ignore multiple UmU-FACULTIES but fractionalize within!

                    double faculty_special_frac = -1;
                    if(isUmuAuthor && multipleUmUaffils) {

                      faculty_special_frac =  author.getFractionIgnoreMultipleUmUAffils();
                      int occurances = facultyToOccurance.get( divaIDtoNames.get(indice).getFAKULTET() );
                      faculty_special_frac =  faculty_special_frac/occurances;


                    } else {

                        faculty_special_frac = author.getFractionIgnoreMultipleUmUAffils();
                    }

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( faculty_special_frac );





                    //FACULTY 8
                    String faculty = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getFAKULTET() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(faculty);

                    //INST 9

                    String inst = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getINSTITUTION() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(inst);


                    //INST 10 UNIT

                    String unit = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getENHET() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(unit);

                    //SET DIVA BIBLIOGRAPHIC STUFF

                    //DIVA TYPE 11
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaPublicationType()  );


                    //DIVA YEAR 11
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getYear()  );

                    //DIVA CONTENT 12
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaContentType()  );

                    //DIVA SUBTYPE 13
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaPublicationSubtype()  );

                    //DIVA status 14
                    cell = row.createCell(++cellIndices);

                    String status = p.getDivaStatus();

                    if("".equals(status)) {status = "published";}

                    cell.setCellValue( status );

                    //DIVA AFFILIATIONS 15
                    cell = row.createCell(++cellIndices);

                    String name = p.getRawDataRow()[ ReducedDiVAColumnIndices.Name.getValue() ];
                    if(name.length() > 32700 ) { name = name.substring(0,32000); name = name.concat("[TRUNCATED!!]");       }
                    cell.setCellValue( name );

                    //DIVA URN 16

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getNBN()  );

                    //DIVA CHANEL 17

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaChannels()  );

                    //DIVA TITLE 18

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getTitle()  );

                    //DIVA ABSTRACT 19

                    cell = row.createCell(++cellIndices);

                    String summary = p.getRawDataRow()[ ReducedDiVAColumnIndices.Abstract.getValue() ];
                    if(summary.length() > 32700 ) { summary = summary.substring(0,32000); summary = summary.concat("[TRUNCATED!!]");       }
                    cell.setCellValue( summary );

                    //DIVA JOURNAL 20

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getJournal()  );

                    //DIVA JOURNAL ISSN 21

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getJournalISSN()  );


                    //DIVA SERIES 22

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getSeriesName()  );


                    //DIVA SERIES ISSN 23

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getSeriesISSN()  );

                    //DIVA PUBLISHER 24

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getPublisher()  );

                    //DIVA PUBLISHER 24

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getISBN()  );



                    //DIVA LANGUAGE 25

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Language.getValue() ] );

                    //DIVA LAST UPDATED  26

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.LastUpdated.getValue() ] );

                    //DIVA NR AUTHORS 27
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getNrAuthors());


                    //DIVA IS DUPE 28
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(  p.isDuplicate() );


                    //DIVA WHICH DUPE 29
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(  p.getDuplicateOfPID() );


                    //SET NORWEGIAN STUFF

                    //NORWEGIAN ID 30

                    cell = row.createCell(++cellIndices);

                    Integer nid = norskNivå.getNorsk_id();
                    if(nid.equals(-99)) { cell.setCellValue( "not available" ); } else {cell.setCellValue( nid ); }


                    //NORWEGIAN TYPE 31

                    cell = row.createCell(++cellIndices);
                    String type = norskNivå.getType();
                    cell.setCellValue((type == null) ? "not available" : type);

                    //NOWRWEGIAN MATCH 32

                    cell = row.createCell(++cellIndices);
                    String how = norskNivå.getHow();
                    cell.setCellValue((how == null) ? "not available" : how);

                    //NOWRWEGIAN NAME 33

                    cell = row.createCell(++cellIndices);
                    String namn = norskNivå.getNorsk_namn();
                    cell.setCellValue((namn == null) ? "not available" : namn);

                    //NORWEGIAN LEVEL 34

                    cell = row.createCell(++cellIndices);
                    Integer nivåStandard = norskNivå.getNivå();
                    cell.setCellValue((nivåStandard == null) ? "not available" : nivåStandard.toString());

                    //NORWEGIAN LEVEL 35

                    cell = row.createCell(++cellIndices);
                    Integer nivåMax = norskNivå.getMax_nivå();
                    cell.setCellValue((nivåMax == null) ? "not available" : nivåMax.toString());


                    //cell = row.createCell(++cellIndices);
                  //  cell.setCellValue(     p.getStatusInModel().getStatusInModel()  );

                    //NORWEGIAN MODEL SPECIFIC INFO 36
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(     norskNivå.getModelSpecificInfo() );


                    //NORWEGIAN POINTS 37

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(      norskNivå.getVikt()     );


                    //add localID

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.LocalId.getValue() ] );

                    //affil mapping info
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.removedNonConsidered );


                    indice++;
                } while(indice < divaIDtoNames.size() );



            }


        }


        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm") ;


        String fileName;

        if(includeExternalAuthors) {fileName = "FractionsIncludedExternal"; } else {fileName = "FractionsExcludedExternal";}


        try (FileOutputStream outputStream = new FileOutputStream(fileName + dateFormat.format(date)  +".xlsx")) {
            workbook.setActiveSheet(0);
            workbook.setSelectedTab(0);
            workbook.write(outputStream);
            workbook.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public void saveDisambiguatedAuthorFractions2024VERSION(List<Post> recordList, boolean includeExternalAuthors) {

        /*
        SHEET 1
         */
        XSSFSheet sheet = workbook.createSheet("FÖRFATTARFRAKTIONER");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        /*
        SHEET 2, ABSTRACTS
        */

        XSSFSheet sheet2 = workbook.createSheet("ABSTRACTS");
        XSSFFont font2 = workbook.createFont();
        XSSFCellStyle style2 = workbook.createCellStyle();
        font2.setBold(true);
        style2.setFont(font2);

        sheet2.createFreezePane(0,1);
        int cellIndices2 = 0;
        int rowIndices2 = 0;


        //SKAPA EN LÅST HEADER-RAD

        Row row2 = sheet2.createRow(rowIndices2);
        Cell cell2 = row2.createCell(0);
        cell2.setCellValue("PID");
        cell2.setCellStyle(style2);
        cell2 = row2.createCell(1);
        cell2.setCellValue("ABSTRACTS");
        cell2.setCellStyle(style2);


        //SKAPA EN LÅST HEADER-RAD
        Row row = sheet.createRow(rowIndices);


        Cell cell = row.createCell(0);
        cell.setCellValue("PID" );
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("AUTHOR_ID" );
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("NAME" );
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("UMUID" );
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("UMUID (FIX MISSING IF POSSIBLE)" );
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("FRACTIONS (STANDARD)" );
        cell.setCellStyle(style);


        cell = row.createCell(6);
        cell.setCellValue("FRACTIONS (STANDARD + MIN0.1)" );
        cell.setCellStyle(style);
    /*

        cell = row.createCell(7);
        cell.setCellValue("FRACTIONS (NO INTERNAL FRACTIONS)" );
        cell.setCellStyle(style);


        cell = row.createCell(8);
        cell.setCellValue("FRACTIONS (NO INTERNAL FRACTIONS + MIN0.1)" );
        cell.setCellStyle(style);

        cell = row.createCell(9);
        cell.setCellValue("FACULTY FRACTION" );
        cell.setCellStyle(style);

   */


        cell = row.createCell(7);
        cell.setCellValue("FACULTY" );
        cell.setCellStyle(style);



        cell = row.createCell(8);
        cell.setCellValue("INSTITUTION" );
        cell.setCellStyle(style);


        cell = row.createCell(9);
        cell.setCellValue("UNIT (IF AVAILABLE OTHERWISE INST.)" );
        cell.setCellStyle(style);

        //BIBLIOGRAPHIC INFO

        cell = row.createCell(10);
        cell.setCellValue("DIVA_TYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(11);
        cell.setCellValue("DIVA_YEAR" );
        cell.setCellStyle(style);

        cell = row.createCell(12);
        cell.setCellValue("DIVA_CONTENT" );
        cell.setCellStyle(style);

        cell = row.createCell(13);
        cell.setCellValue("DIVA_SUBTYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(14);
        cell.setCellValue("DIVA_STATUS" );
        cell.setCellStyle(style);

        cell = row.createCell(15);
        cell.setCellValue("DIVA_AFFILIATIONS" );
        cell.setCellStyle(style);

        cell = row.createCell(16);
        cell.setCellValue("DIVA_URN" );
        cell.setCellStyle(style);


        cell = row.createCell(17);
        cell.setCellValue("DIVA_POTENTIAL_CHANNELS" );
        cell.setCellStyle(style);

        cell = row.createCell(18);
        cell.setCellValue("DIVA_TITLE" );
        cell.setCellStyle(style);

        //cell = row.createCell(19);
        //cell.setCellValue("DIVA_ABSTRACT" );
        //cell.setCellStyle(style);

        cell = row.createCell(19);
        cell.setCellValue("DIVA_JOURNAL" );
        cell.setCellStyle(style);

        cell = row.createCell(20);
        cell.setCellValue("DIVA_JOURNAL_ISSN" );
        cell.setCellStyle(style);


        cell = row.createCell(21);
        cell.setCellValue("DIVA_SERIES" );
        cell.setCellStyle(style);


        cell = row.createCell(22);
        cell.setCellValue("DIVA_SERIES_ISSN" );
        cell.setCellStyle(style);

        cell = row.createCell(23);
        cell.setCellValue("DIVA_PUBLISHER" );
        cell.setCellStyle(style);

        cell = row.createCell(24);
        cell.setCellValue("DIVA_ISBN" );
        cell.setCellStyle(style);


        cell = row.createCell(25);
        cell.setCellValue("DIVA_LANGUAGE" );
        cell.setCellStyle(style);

        cell = row.createCell(26);
        cell.setCellValue("DIVA_LAST_CHANGED" );
        cell.setCellStyle(style);

        cell = row.createCell(27);
        cell.setCellValue("DIVA_NR_AUTHORS" );
        cell.setCellStyle(style);

        cell = row.createCell(28);
        cell.setCellValue("DIVA_IS_DUPLICATED" );
        cell.setCellStyle(style);

        cell = row.createCell(29);
        cell.setCellValue("DIVA_DUPLICATED_PID" );
        cell.setCellStyle(style);

        //NORSK STUFF


        cell = row.createCell(30);
        cell.setCellValue("NORWEGIAN_ID" );
        cell.setCellStyle(style);


        cell = row.createCell(31);
        cell.setCellValue("NORWEGIAN_TYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(32);
        cell.setCellValue("NORWEGIAN_MATCH" );
        cell.setCellStyle(style);


        cell = row.createCell(33);
        cell.setCellValue("NORWEGIAN_NAME" );
        cell.setCellStyle(style);

        cell = row.createCell(34);
        cell.setCellValue("NORWEGIAN_LEVEL" );
        cell.setCellStyle(style);

        cell = row.createCell(35);
        cell.setCellValue("NORWEGIAN_LEVEL (HISTORICAL MAXIMUM)" );
        cell.setCellStyle(style);

        cell = row.createCell(36);
        cell.setCellValue("NORWEGIAN_MODEL_INFO" );
        cell.setCellStyle(style);

        cell = row.createCell(37);
        cell.setCellValue("PUBLICATION_MODEL_INFO" );
        cell.setCellStyle(style);

        cell = row.createCell(38);
        cell.setCellValue("NORWEGIAN_POINTS" );
        cell.setCellStyle(style);

        //add localID, use to manually remap institutionen för geografi och ekonomisk historia

        cell = row.createCell(39);
        cell.setCellValue("LOCAL_ID");
        cell.setCellStyle(style);

        //add info if the affiliation has been altered, i.e., non considered removed (0=no, 1=yes, 2=count not remove, only non considered

        cell = row.createCell(40);
        cell.setCellValue("AFFIL_MAPPING_INFO");
        cell.setCellStyle(style);

        //2024 add IDs

        cell = row.createCell(41);
        cell.setCellValue("DOI");
        cell.setCellStyle(style);

        cell = row.createCell(42);
        cell.setCellValue("EID");
        cell.setCellStyle(style);

        cell = row.createCell(43);
        cell.setCellValue("WOS");
        cell.setCellStyle(style);

        cell = row.createCell(44);
        cell.setCellValue("PMID");
        cell.setCellStyle(style);

        for(Post p : recordList) {

                String summary2 = p.getRawDataRow()[ ReducedDiVAColumnIndices.Abstract.getValue() ];
                if(summary2.length() > 32700 ) { summary2 = summary2.substring(0,32000); summary2 = summary2.concat("[TRUNCATED!!]");       }

                row2 = sheet2.createRow(++rowIndices2);
                cellIndices2 = -1;

                //PID
                cell2 = row2.createCell(++cellIndices2);
                cell2.setCellValue(p.getPID());

                //ABSTRACTS

                cell2 = row2.createCell(++cellIndices2);
                cell2.setCellValue(summary2);





            for(Author author : p.getAuthorList() ) {


                List<DivaIDtoNames> divaIDtoNames = author.getAffilMappingsObjects();
                boolean isUmuAuthor = divaIDtoNames.size() > 0;

                if(!includeExternalAuthors && !isUmuAuthor) continue;


                NorwegianMatchInfo norskNivå = p.getNorskNivå();


                //
                //Count number of FACULTIES an author is associated with. This is needed for fractionalization that dont d´split an author fraction over faculties but split on inst/unit on a lower level
                //

                Map<String, Integer> facultyToOccurance = null;
                boolean multipleUmUaffils = (divaIDtoNames.size() > 1);

                if(multipleUmUaffils) {

                    facultyToOccurance = new HashMap<>();

                    for(DivaIDtoNames id : divaIDtoNames) {

                        String facultyName = id.getFAKULTET();

                        Integer occurrence = facultyToOccurance.get(facultyName);
                        if(occurrence == null) {

                            facultyToOccurance.put(facultyName,1);
                        } else {

                            facultyToOccurance.put(facultyName, (occurrence+1) );

                        }



                    }//for ends


                }



                ////End special treatment for non frac over Faculties


                int indice = 0;
                do {
                    row = sheet.createRow(++rowIndices);
                    cellIndices = -1;

                    //PID 1
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getPID());

                    //AUTHOR_ID" 2

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getDisambiguateID());


                    //AUTHOR_NAME" 3

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getAuthorName() );

                    //CAS 1" 4

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getCas() );

                    //CAS 2" 5

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getAutomaticAddedCass() );

                    //CAS FRAC STANDARD 6

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getFractionConsiderMultipleUmUAffils() );

                    // NEW NEW 7
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getFractionConsiderMultipleUmUAffilsMin01() );


                    //NEW 2022-11-16, fractions ignoring internal, and no min01 , 8

                   // cell = row.createCell(++cellIndices);
                   // cell.setCellValue(author.getFractionIgnoreMultipleUmUAffils() );


                    //CAS FRAC Min.01 AND Ignore multiple UmU-affils 9

                   // cell = row.createCell(++cellIndices);
                   // cell.setCellValue(author.getFractionIgnoreMultipleUmUAffilsMin01() );


                    //ignore multiple UmU-FACULTIES but fractionalize within!

                    double faculty_special_frac = -1;
                    if(isUmuAuthor && multipleUmUaffils) {

                        faculty_special_frac =  author.getFractionIgnoreMultipleUmUAffils();
                        int occurances = facultyToOccurance.get( divaIDtoNames.get(indice).getFAKULTET() );
                        faculty_special_frac =  faculty_special_frac/occurances;


                    } else {

                        faculty_special_frac = author.getFractionIgnoreMultipleUmUAffils();
                    }

                   // cell = row.createCell(++cellIndices);
                   // cell.setCellValue( faculty_special_frac );





                    //FACULTY 10
                    String faculty = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getFAKULTET() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(faculty);

                    //INST 11

                    String inst = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getINSTITUTION() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(inst);


                    //INST 12 UNIT

                    String unit = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getENHET() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(unit);

                    //SET DIVA BIBLIOGRAPHIC STUFF

                    //DIVA TYPE 13
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaPublicationType()  );


                    //DIVA YEAR 14
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getYear()  );

                    //DIVA CONTENT 15
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaContentType()  );

                    //DIVA SUBTYPE 16
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaPublicationSubtype()  );

                    //DIVA status 17
                    cell = row.createCell(++cellIndices);

                    String status = p.getDivaStatus();

                    if("".equals(status)) {status = "published";}

                    cell.setCellValue( status );

                    //DIVA AFFILIATIONS 18
                    cell = row.createCell(++cellIndices);

                    String name = p.getRawDataRow()[ ReducedDiVAColumnIndices.Name.getValue() ];
                    if(name.length() > 32700 ) { name = name.substring(0,32000); name = name.concat("[TRUNCATED!!]");       }
                    cell.setCellValue( name );

                    //DIVA URN 19

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getNBN()  );

                    //DIVA CHANEL 20

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaChannels()  );

                    //DIVA TITLE 21

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getTitle()  );

                    //DIVA ABSTRACT 22

                    //MOVED TO OWN SHEET 2024
                    //cell = row.createCell(++cellIndices);
                    //String summary = p.getRawDataRow()[ ReducedDiVAColumnIndices.Abstract.getValue() ];
                    //if(summary.length() > 32700 ) { summary = summary.substring(0,32000); summary = summary.concat("[TRUNCATED!!]");       }
                    //cell.setCellValue( summary );

                    //DIVA JOURNAL 23

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getJournal()  );

                    //DIVA JOURNAL ISSN 24

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getJournalISSN()  );


                    //DIVA SERIES 25

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getSeriesName()  );


                    //DIVA SERIES ISSN 26

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getSeriesISSN()  );

                    //DIVA PUBLISHER 27

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getPublisher()  );

                    //DIVA PUBLISHER 28

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getISBN()  );



                    //DIVA LANGUAGE 29

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Language.getValue() ] );

                    //DIVA LAST UPDATED  30

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.LastUpdated.getValue() ] );

                    //DIVA NR AUTHORS 31
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getNrAuthors());


                    //DIVA IS DUPE 32
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(  p.isDuplicate() );


                    //DIVA WHICH DUPE 33
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(  p.getDuplicateOfPID() );


                    //SET NORWEGIAN STUFF

                    //NORWEGIAN ID 34

                    cell = row.createCell(++cellIndices);

                    Integer nid = norskNivå.getNorsk_id();
                    if(nid.equals(-99)) { cell.setCellValue( "not available" ); } else {cell.setCellValue( nid ); }


                    //NORWEGIAN TYPE 35

                    cell = row.createCell(++cellIndices);
                    String type = norskNivå.getType();
                    cell.setCellValue((type == null) ? "not available" : type);

                    //NOWRWEGIAN MATCH 36

                    cell = row.createCell(++cellIndices);
                    String how = norskNivå.getHow();
                    cell.setCellValue((how == null) ? "not available" : how);

                    //NOWRWEGIAN NAME 37

                    cell = row.createCell(++cellIndices);
                    String namn = norskNivå.getNorsk_namn();
                    cell.setCellValue((namn == null) ? "not available" : namn);

                    //NORWEGIAN LEVEL 38

                    cell = row.createCell(++cellIndices);
                    Integer nivåStandard = norskNivå.getNivå();
                    cell.setCellValue((nivåStandard == null) ? "not available" : nivåStandard.toString());

                    //NORWEGIAN LEVEL 39

                    cell = row.createCell(++cellIndices);
                    Integer nivåMax = norskNivå.getMax_nivå();
                    cell.setCellValue((nivåMax == null) ? "not available" : nivåMax.toString());


                    //NORWEGIAN MODEL SPECIFIC INFO 40
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(     norskNivå.getModelSpecificInfo() );

                    //PUBLICATION MODEL SPECIFICATION 41
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(     p.getStatusInModel().getStatusInModel()  );


                    //NORWEGIAN POINTS 42

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(      norskNivå.getVikt()     );


                    //add localID 43

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.LocalId.getValue() ] );

                    //affil mapping info 44
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.removedNonConsidered );

                  //ADD DOI

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.DOI.getValue() ] );

                    //ADD EID

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.ScopusId.getValue() ] );

                    //ADD WOS

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.ISI.getValue() ] );

                    //ADD PMID

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.PMID.getValue() ] );



                    indice++;
                } while(indice < divaIDtoNames.size() );



            }


        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);
        sheet.autoSizeColumn(8);
        sheet.autoSizeColumn(9);
        sheet.autoSizeColumn(10);
        sheet.autoSizeColumn(11);
        sheet.autoSizeColumn(12);
        sheet.autoSizeColumn(13);
        sheet.autoSizeColumn(14);
        sheet.autoSizeColumn(41);
        sheet.autoSizeColumn(42);
        sheet.autoSizeColumn(43);
        sheet.autoSizeColumn(44);

        sheet2.autoSizeColumn(0); //abstract sheet

        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm") ;


        String fileName;

        if(includeExternalAuthors) {fileName = "FractionsIncludedExternal"; } else {fileName = "FractionsExcludedExternal";}


        try (FileOutputStream outputStream = new FileOutputStream(fileName + dateFormat.format(date)  +".xlsx")) {
            workbook.setActiveSheet(0);
            workbook.setSelectedTab(0);
            workbook.write(outputStream);
            workbook.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void saveDisambiguatedAuthorFractionsBylineAware2025(List<Post> recordList, boolean includeExternalAuthors, UMUID_YEARS_AT_UNITS yearsAtUnits) {

        // 1. Create a separate sheet for documentation
        XSSFSheet docSheet = workbook.createSheet("AUTO_DOKUMENTATION");
        docSheet.setDisplayGridlines(false); // optional, looks more like a “page”

        // 2. Create the drawing container for shapes
        XSSFDrawing drawing = docSheet.createDrawingPatriarch();

        // 3. Define where the textbox should appear (top-left & bottom-right cell)
        XSSFClientAnchor anchor = new XSSFClientAnchor();
        anchor.setCol1(1);   // column B
        anchor.setRow1(1);   // row 2
        anchor.setCol2(60);   // some column
        anchor.setRow2(59);  // row 26

        XSSFTextBox textBox = drawing.createTextbox(anchor);

        // Use helper that formats documentationText into bold/normal “columns”
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm");
        String formattedDate = dateFormat.format(date);
        String docsForThisRun = String.format(documentationTextTemplate, formattedDate);
        fillDocumentationTextBox(textBox, docsForThisRun);


        //NOW THE ACCTUAL DATA



        /*
        SHEET 1
         */
        XSSFSheet sheet = workbook.createSheet("FÖRFATTARFRAKTIONER");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        /*
        SHEET 2, ABSTRACTS
        */

        XSSFSheet sheet2 = workbook.createSheet("ABSTRACTS");
        XSSFFont font2 = workbook.createFont();
        XSSFCellStyle style2 = workbook.createCellStyle();
        font2.setBold(true);
        style2.setFont(font2);

        sheet2.createFreezePane(0,1);
        int cellIndices2 = 0;
        int rowIndices2 = 0;


        //SKAPA EN LÅST HEADER-RAD

        Row row2 = sheet2.createRow(rowIndices2);
        Cell cell2 = row2.createCell(0);
        cell2.setCellValue("PID");
        cell2.setCellStyle(style2);
        cell2 = row2.createCell(1);
        cell2.setCellValue("ABSTRACTS");
        cell2.setCellStyle(style2);


        //SKAPA EN LÅST HEADER-RAD
        Row row = sheet.createRow(rowIndices);


        Cell cell = row.createCell(0);
        cell.setCellValue("PID" );
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("AUTHOR_ID" );
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("NAME" );
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("UMUID" );
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("UMUID (FIX MISSING IF POSSIBLE)" );
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("STRAIGHT FRACTIONS" );
        cell.setCellStyle(style);


        cell = row.createCell(6);
        cell.setCellValue("STRAIGHT FRACTIONS (MINIMUM 0.1)" );
        cell.setCellStyle(style);

        cell = row.createCell(7);
        cell.setCellValue("BYLINE WEIGHTED FRACTIONS (U-SHAPED)" );
        cell.setCellStyle(style);

        cell = row.createCell(8);
        cell.setCellValue("BYLINE WEIGHTED FRACTIONS (LiU/UMU-MODEL)" );
        cell.setCellStyle(style);


        cell = row.createCell(9);
        cell.setCellValue("AUTHOR BYLINE CATEGORY" );
        cell.setCellStyle(style);

        cell = row.createCell(10);
        cell.setCellValue("TOTAL NUMBER OF AUTHORS" );
        cell.setCellStyle(style);

        cell = row.createCell(11);
        cell.setCellValue("POSITION CLOSEST TO PUBLICATION YEAR" );
        cell.setCellStyle(style);

        cell = row.createCell(12);
        cell.setCellValue("LAST KNOWN POSITION @ UMU" );
        cell.setCellStyle(style);

        cell = row.createCell(13);
        cell.setCellValue("FACULTY" );
        cell.setCellStyle(style);



        cell = row.createCell(14);
        cell.setCellValue("INSTITUTION" );
        cell.setCellStyle(style);


        cell = row.createCell(15);
        cell.setCellValue("UNIT (IF AVAILABLE OTHERWISE INST.)" );
        cell.setCellStyle(style);

        //BIBLIOGRAPHIC INFO

        cell = row.createCell(16);
        cell.setCellValue("DIVA_TYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(17);
        cell.setCellValue("DIVA_YEAR" );
        cell.setCellStyle(style);

        cell = row.createCell(18);
        cell.setCellValue("DIVA_CONTENT" );
        cell.setCellStyle(style);

        cell = row.createCell(19);
        cell.setCellValue("DIVA_SUBTYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(20);
        cell.setCellValue("DIVA_STATUS" );
        cell.setCellStyle(style);

        cell = row.createCell(21);
        cell.setCellValue("DIVA_AFFILIATIONS" );
        cell.setCellStyle(style);

        cell = row.createCell(22);
        cell.setCellValue("DIVA_URN" );
        cell.setCellStyle(style);


        cell = row.createCell(23);
        cell.setCellValue("DIVA_POTENTIAL_CHANNELS" );
        cell.setCellStyle(style);

        cell = row.createCell(24);
        cell.setCellValue("DIVA_TITLE" );
        cell.setCellStyle(style);


        cell = row.createCell(25);
        cell.setCellValue("DIVA_JOURNAL" );
        cell.setCellStyle(style);

        cell = row.createCell(26);
        cell.setCellValue("DIVA_JOURNAL_ISSN" );
        cell.setCellStyle(style);


        cell = row.createCell(27);
        cell.setCellValue("DIVA_SERIES" );
        cell.setCellStyle(style);


        cell = row.createCell(28);
        cell.setCellValue("DIVA_SERIES_ISSN" );
        cell.setCellStyle(style);

        cell = row.createCell(29);
        cell.setCellValue("DIVA_PUBLISHER" );
        cell.setCellStyle(style);

        cell = row.createCell(30);
        cell.setCellValue("DIVA_ISBN" );
        cell.setCellStyle(style);


        cell = row.createCell(31);
        cell.setCellValue("DIVA_LANGUAGE" );
        cell.setCellStyle(style);

        cell = row.createCell(32);
        cell.setCellValue("DIVA_LAST_CHANGED" );
        cell.setCellStyle(style);

        //cell = row.createCell(32);
        //cell.setCellValue("DIVA_NR_AUTHORS" );
        //cell.setCellStyle(style);

        cell = row.createCell(33);
        cell.setCellValue("DIVA_IS_DUPLICATED" );
        cell.setCellStyle(style);

        cell = row.createCell(34);
        cell.setCellValue("DIVA_DUPLICATED_PID" );
        cell.setCellStyle(style);

        //NORSK STUFF


        cell = row.createCell(35);
        cell.setCellValue("NORWEGIAN_ID" );
        cell.setCellStyle(style);


        cell = row.createCell(36);
        cell.setCellValue("NORWEGIAN_TYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(37);
        cell.setCellValue("NORWEGIAN_MATCH" );
        cell.setCellStyle(style);


        cell = row.createCell(38);
        cell.setCellValue("NORWEGIAN_NAME" );
        cell.setCellStyle(style);

        cell = row.createCell(39);
        cell.setCellValue("NORWEGIAN_LEVEL" );
        cell.setCellStyle(style);

        cell = row.createCell(40);
        cell.setCellValue("NORWEGIAN_LEVEL (HISTORICAL MAXIMUM)" );
        cell.setCellStyle(style);

        cell = row.createCell(41);
        cell.setCellValue("NORWEGIAN_MODEL_INFO" );
        cell.setCellStyle(style);

        cell = row.createCell(42);
        cell.setCellValue("PUBLICATION_MODEL_INFO" );
        cell.setCellStyle(style);

        cell = row.createCell(43);
        cell.setCellValue("NORWEGIAN_POINTS" );
        cell.setCellStyle(style);

        //add localID, use to manually remap institutionen för geografi och ekonomisk historia

        cell = row.createCell(44);
        cell.setCellValue("LOCAL_ID");
        cell.setCellStyle(style);

        //add info if the affiliation has been altered, i.e., non considered removed (0=no, 1=yes, 2=count not remove, only non considered

        cell = row.createCell(45);
        cell.setCellValue("AFFIL_MAPPING_INFO");
        cell.setCellStyle(style);

        //2024 add IDs

        cell = row.createCell(46);
        cell.setCellValue("DOI");
        cell.setCellStyle(style);

        cell = row.createCell(47);
        cell.setCellValue("EID");
        cell.setCellStyle(style);

        cell = row.createCell(48);
        cell.setCellValue("WOS");
        cell.setCellStyle(style);

        cell = row.createCell(49);
        cell.setCellValue("PMID");
        cell.setCellStyle(style);

        cell = row.createCell(50);
        cell.setCellValue("ANY_UMU_AUTHOR_ON_RECORD_LEVEL");
        cell.setCellStyle(style);

        //calculated points

        cell = row.createCell(51);
        cell.setCellValue("NORWEGIAN_POINTS_TIMES_FRACTION (STRAIGHT)");
        cell.setCellStyle(style);

        cell = row.createCell(52);
        cell.setCellValue("NORWEGIAN_POINTS_TIMES_FRACTION (STRAIGHT, MIN 0.1)");
        cell.setCellStyle(style);

        cell = row.createCell(53);
        cell.setCellValue("NORWEGIAN_POINTS_TIMES_FRACTION (WEIGHTED, U-SHAPED)");
        cell.setCellStyle(style);

        cell = row.createCell(54);
        cell.setCellValue("NORWEGIAN_POINTS_TIMES_FRACTION (WEIGHTED, LiU/UMU-MODEL)");
        cell.setCellStyle(style);




        // populate header cells...
        for (int c = 0; c <sheet.getRow(0).getLastCellNum(); c++) {
            sheet.autoSizeColumn(c);
        }


        for(Post p : recordList) {

            String summary2 = p.getRawDataRow()[ ReducedDiVAColumnIndices.Abstract.getValue() ];
            if(summary2.length() > 32700 ) { summary2 = summary2.substring(0,32000); summary2 = summary2.concat("[TRUNCATED!!]");       }

            row2 = sheet2.createRow(++rowIndices2);
            cellIndices2 = -1;

            //PID
            cell2 = row2.createCell(++cellIndices2);
            cell2.setCellValue(p.getPID());

            //ABSTRACTS

            cell2 = row2.createCell(++cellIndices2);
            cell2.setCellValue(summary2);





            for(Author author : p.getAuthorList() ) {


                List<DivaIDtoNames> divaIDtoNames = author.getAffilMappingsObjects();
                boolean isUmuAuthor = !divaIDtoNames.isEmpty();

                if(!includeExternalAuthors && !isUmuAuthor) continue;


                NorwegianMatchInfo norskNivå = p.getNorskNivå();


                //
                //Count number of FACULTIES an author is associated with. This is needed for fractionalization that dont d´split an author fraction over faculties but split on inst/unit on a lower level
                //

                Map<String, Integer> facultyToOccurance = null;
                boolean multipleUmUaffils = (divaIDtoNames.size() > 1);

                if(multipleUmUaffils) {

                    facultyToOccurance = new HashMap<>();

                    for(DivaIDtoNames id : divaIDtoNames) {

                        String facultyName = id.getFAKULTET();

                        Integer occurrence = facultyToOccurance.get(facultyName);
                        if(occurrence == null) {

                            facultyToOccurance.put(facultyName,1);
                        } else {

                            facultyToOccurance.put(facultyName, (occurrence+1) );

                        }



                    }//for ends


                }



                ////End special treatment for non frac over Faculties


                int indice = 0;
                do {
                    row = sheet.createRow(++rowIndices);
                    cellIndices = -1;

                    //PID  0
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getPID());

                    //AUTHOR_ID" 1

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getDisambiguateID());


                    //AUTHOR_NAME" 2

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getAuthorName() );

                    //CAS 1" 3

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getCas() );

                    //CAS 2" 4

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getAutomaticAddedCass() );

                    //CAS STRAIGHT FRACTIONS 5

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getStraightFractions() );

                    // STRAIGHT FRACTIONS MIN 0.1 6
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getStraightFractionsMin01() );


                    //BYLINE WEIGHTED A 7

                     cell = row.createCell(++cellIndices);
                     cell.setCellValue(author.getUshapedFractions() );


                    //BYLINE WEIGHTED B 8

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getLiuStyleFractions() );


                    //BYLINE CATEGORY 9

                     cell = row.createCell(++cellIndices);
                     cell.setCellValue(author.getAuthorBylineCategory() );

                    //NR AUTHORS 10

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getNrAuthors() );

                    //position @ pubyear 11

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( author.getBefattningAtPublicationDate() );

                    //Last known position 12

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( author.getLastKnownBeffattning() );


                    //ignore multiple UmU-FACULTIES but fractionalize within!

                    double faculty_special_frac = -1;
                    if(isUmuAuthor && multipleUmUaffils) {

                        faculty_special_frac =  author.getFractionIgnoreMultipleUmUAffils();
                        int occurances = facultyToOccurance.get( divaIDtoNames.get(indice).getFAKULTET() );
                        faculty_special_frac =  faculty_special_frac/occurances;


                    } else {

                        faculty_special_frac = author.getFractionIgnoreMultipleUmUAffils();
                    }

                    // cell = row.createCell(++cellIndices);
                    // cell.setCellValue( faculty_special_frac );





                    //FACULTY 13
                    String faculty = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getFAKULTET() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(faculty);

                    //INST 14

                    String inst = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getINSTITUTION() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(inst);


                    //INST 15 UNIT

                    String unit = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getENHET() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(unit);

                    //SET DIVA BIBLIOGRAPHIC STUFF

                    //DIVA TYPE 16
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaPublicationType()  );


                    //DIVA YEAR 17
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getYear()  );

                    //DIVA CONTENT 18
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaContentType()  );

                    //DIVA SUBTYPE 19
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaPublicationSubtype()  );

                    //DIVA status 20
                    cell = row.createCell(++cellIndices);

                    String status = p.getDivaStatus();

                    if("".equals(status)) {status = "published";}

                    cell.setCellValue( status );

                    //DIVA AFFILIATIONS 21
                    cell = row.createCell(++cellIndices);

                    String name = p.getRawDataRow()[ ReducedDiVAColumnIndices.Name.getValue() ];
                    if(name.length() > 32700 ) { name = name.substring(0,32000); name = name.concat("[TRUNCATED!!]");       }
                    cell.setCellValue( name );

                    //DIVA URN 22

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getNBN()  );

                    //DIVA CHANEL 23

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaChannels()  );

                    //DIVA TITLE 24

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getTitle()  );

                    //DIVA ABSTRACT 25

                    //MOVED TO OWN SHEET 2024
                    //cell = row.createCell(++cellIndices);
                    //String summary = p.getRawDataRow()[ ReducedDiVAColumnIndices.Abstract.getValue() ];
                    //if(summary.length() > 32700 ) { summary = summary.substring(0,32000); summary = summary.concat("[TRUNCATED!!]");       }
                    //cell.setCellValue( summary );

                    //DIVA JOURNAL 26

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getJournal()  );

                    //DIVA JOURNAL ISSN 27

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getJournalISSN()  );


                    //DIVA SERIES 28

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getSeriesName()  );


                    //DIVA SERIES ISSN 29

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getSeriesISSN()  );

                    //DIVA PUBLISHER 30

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getPublisher()  );

                    //DIVA PUBLISHER 31

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getISBN()  );



                    //DIVA LANGUAGE 32

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Language.getValue() ] );

                    //DIVA LAST UPDATED  33

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.LastUpdated.getValue() ] );

                    //DIVA NR AUTHORS 34
                   // cell = row.createCell(++cellIndices);
                   // cell.setCellValue(p.getNrAuthors());


                    //DIVA IS DUPE 35
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(  p.isDuplicate() );


                    //DIVA WHICH DUPE 36
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(  p.getDuplicateOfPID() );


                    //SET NORWEGIAN STUFF

                    //NORWEGIAN ID 37

                    cell = row.createCell(++cellIndices);

                    Integer nid = norskNivå.getNorsk_id();
                    if(nid.equals(-99)) { cell.setCellValue( "not available" ); } else {cell.setCellValue( nid ); }


                    //NORWEGIAN TYPE 38

                    cell = row.createCell(++cellIndices);
                    String type = norskNivå.getType();
                    cell.setCellValue((type == null) ? "not available" : type);

                    //NOWRWEGIAN MATCH 39

                    cell = row.createCell(++cellIndices);
                    String how = norskNivå.getHow();
                    cell.setCellValue((how == null) ? "not available" : how);

                    //NOWRWEGIAN NAME 40

                    cell = row.createCell(++cellIndices);
                    String namn = norskNivå.getNorsk_namn();
                    cell.setCellValue((namn == null) ? "not available" : namn);

                    //NORWEGIAN LEVEL 41

                    cell = row.createCell(++cellIndices);
                    Integer nivåStandard = norskNivå.getNivå();
                    cell.setCellValue((nivåStandard == null) ? "not available" : nivåStandard.toString());

                    //NORWEGIAN LEVEL 42

                    cell = row.createCell(++cellIndices);
                    Integer nivåMax = norskNivå.getMax_nivå();
                    cell.setCellValue((nivåMax == null) ? "not available" : nivåMax.toString());


                    //NORWEGIAN MODEL SPECIFIC INFO 43
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(     norskNivå.getModelSpecificInfo() );

                    //PUBLICATION MODEL SPECIFICATION 44
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(     p.getStatusInModel().getStatusInModel()  );


                    //NORWEGIAN POINTS 45

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(      norskNivå.getVikt()     );


                    //add localID 46

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.LocalId.getValue() ] );

                    //affil mapping info 47
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.removedNonConsidered );

                    //ADD DOI 48

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.DOI.getValue() ] );

                    //ADD EID 49

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.ScopusId.getValue() ] );

                    //ADD WOS, 50

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.ISI.getValue() ] );

                    //ADD PMID 51

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.PMID.getValue() ] );

                    //any UMU author 52

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.isHasUMUAuthors());

                    //norwegian points, four different versions based on author fraction type

                    //STRAIGHT
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getStraightFractions() * p.getNorskNivå().getVikt() );

                    //MIN 0.1
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( author.getStraightFractionsMin01() * p.getNorskNivå().getVikt()  );

                    //U-SHAPED
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getUshapedFractions() * p.getNorskNivå().getVikt() );

                    //LiU Style
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getLiuStyleFractions() * p.getNorskNivå().getVikt() );


                    indice++;
                } while(indice < divaIDtoNames.size() );



            }


        }



        //ADD YEARS EMPLYMENT IF AVAILIABLE

        if(yearsAtUnits != null) {

            //System.out.println( yearsAtUnits.UMUID_TO_YEARS_AT_UNITS.get("crco0001") );

            /*
            SHEET 3, ABSTRACTS
            */

            XSSFSheet sheet3 = workbook.createSheet("FÖRFATTARE_ÅR_VID_UMU");
            XSSFFont font3 = workbook.createFont();
            XSSFCellStyle style3 = workbook.createCellStyle();
            font3.setBold(true);
            style3.setFont(font3);

            sheet3.createFreezePane(0,1);
            int cellIndices3 = 0;
            int rowIndices3 = 0;


            //SKAPA EN LÅST HEADER-RAD

            Row row3 = sheet3.createRow(rowIndices3);
            Cell cell3 = row3.createCell(0);
            cell3.setCellValue("UMUID");
            cell3.setCellStyle(style3);
            cell3 = row3.createCell(1);
            cell3.setCellValue("YEARS @ UMU (IN INTERVAL " + yearsAtUnits.minYears + "-" + yearsAtUnits.maxYears + ")");
            cell3.setCellStyle(style3);

            cell3 = row3.createCell(2);
            cell3.setCellValue("YEARS @ MEDFAK (IN INTERVAL " + yearsAtUnits.minYears + "-" + yearsAtUnits.maxYears + ")");
            cell3.setCellStyle(style3);

            cell3 = row3.createCell(3);
            cell3.setCellValue("YEARS @ TEKNAT (IN INTERVAL " + yearsAtUnits.minYears + "-" + yearsAtUnits.maxYears + ")");
            cell3.setCellStyle(style3);

            cell3 = row3.createCell(4);
            cell3.setCellValue("YEARS @ SAMFAK (IN INTERVAL " + yearsAtUnits.minYears + "-" + yearsAtUnits.maxYears + ")");
            cell3.setCellStyle(style3);

            cell3 = row3.createCell(5);
            cell3.setCellValue("YEARS @ HUMFAK (IN INTERVAL " + yearsAtUnits.minYears + "-" + yearsAtUnits.maxYears + ")");
            cell3.setCellStyle(style3);


            // populate header cells...
            for (int c = 0; c <sheet3.getRow(0).getLastCellNum(); c++) {
                sheet3.autoSizeColumn(c);
            }

            String[] UNITS = new String[]{"UMU","MEDFAK","SAMFAK","TEKNAT","HUMFAK"};

            for(Map.Entry<String,HashMap<String,TreeSet<Integer>>> entry : yearsAtUnits.UMUID_TO_YEARS_AT_UNITS.entrySet()) {


                row3 = sheet3.createRow(++rowIndices3);
                cellIndices3 = -1;

                cell3 = row3.createCell(++cellIndices3);
                cell3.setCellValue(entry.getKey());

                for(String unit : UNITS) {

                    //
                    cell3 = row3.createCell(++cellIndices3);
                    cell3.setCellValue( entry.getValue().get(unit).size() );


                }
            }





        } //years and stuff compleated




        String fileName;

        if(includeExternalAuthors) {fileName = "ShowExternalAuthors"; } else {fileName = "HideExternalAuthors";}


        try (FileOutputStream outputStream = new FileOutputStream(fileName + "." + dateFormat.format(date)  +".xlsx")) {
            workbook.setActiveSheet(0);
            workbook.setSelectedTab(0);
            workbook.write(outputStream);
            workbook.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public void saveDisambiguatedAuthorFractionsBylineAwareWiP2026(List<Post> recordList, boolean includeExternalAuthors, UMUID_YEARS_AT_UNITS yearsAtUnits) throws Exception {

        /*

        Here we take author fractions, not split over internal units, and handel that here insted, no internal fraction on Centrumlika units, but att info w r t DOUBBLE_COUNT_ON_CENTRUM and CENTRUM_LIKE_UNIT

         */

        // 1. Create a separate sheet for documentation
        XSSFSheet docSheet = workbook.createSheet("VARIABELDEFINITIONER");
        docSheet.setDisplayGridlines(false); // optional, looks more like a “page”

        // 2. Create the drawing container for shapes
        XSSFDrawing drawing = docSheet.createDrawingPatriarch();

        // 3. Define where the textbox should appear (top-left & bottom-right cell)
        XSSFClientAnchor anchor = new XSSFClientAnchor();
        anchor.setCol1(1);   // column B
        anchor.setRow1(1);   // row 2
        anchor.setCol2(50);   // some column
        anchor.setRow2(70);  // row 26

        XSSFTextBox textBox = drawing.createTextbox(anchor);

        // Use helper that formats documentationText into bold/normal “columns”
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm");
        String formattedDate = dateFormat.format(date);
        String docsForThisRun = String.format(documentationTextTemplate, formattedDate);
        fillDocumentationTextBox(textBox, docsForThisRun);


        //NOW THE ACCTUAL DATA



        /*
        SHEET 1
         */
        XSSFSheet sheet = workbook.createSheet("FÖRFATTARFRAKTIONER");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        /*
        SHEET 2, ABSTRACTS
        */

        XSSFSheet sheet2 = workbook.createSheet("ABSTRACTS");
        XSSFFont font2 = workbook.createFont();
        XSSFCellStyle style2 = workbook.createCellStyle();
        font2.setBold(true);
        style2.setFont(font2);

        sheet2.createFreezePane(0,1);
        int cellIndices2 = -1;
        int rowIndices2 = 0;


        //SKAPA EN LÅST HEADER-RAD

        Row row2 = sheet2.createRow(rowIndices2);
        Cell cell2 = row2.createCell(0);
        cell2.setCellValue("PID");
        cell2.setCellStyle(style2);
        cell2 = row2.createCell(1);
        cell2.setCellValue("ABSTRACTS");
        cell2.setCellStyle(style2);


        //SKAPA EN LÅST HEADER-RAD
        Row row = sheet.createRow(rowIndices);


        Cell cell = row.createCell(++cellIndices2);
        cell.setCellValue("PID" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("AUTHOR_ID" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NAME" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("UMUID" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("UMUID (FIX MISSING IF POSSIBLE)" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("STRAIGHT FRACTIONS" );
        cell.setCellStyle(style);


        cell = row.createCell(++cellIndices2);
        cell.setCellValue("STRAIGHT FRACTIONS (MINIMUM 0.1)" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("BYLINE WEIGHTED FRACTIONS (U-SHAPED)" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("BYLINE WEIGHTED FRACTIONS (LiU/UMU-MODEL)" );
        cell.setCellStyle(style);


        cell = row.createCell(++cellIndices2);
        cell.setCellValue("AUTHOR BYLINE CATEGORY" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DOUBLE COUNTING INDUCED BY CENTRUM-INSTITUTION COMBINATION" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("CENTRUM LIKE UNIT" );
        cell.setCellStyle(style);


        cell = row.createCell(++cellIndices2);
        cell.setCellValue("TOTAL NUMBER OF AUTHORS" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("POSITION CLOSEST TO PUBLICATION YEAR" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("LAST KNOWN POSITION @ UMU" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("FACULTY" );
        cell.setCellStyle(style);



        cell = row.createCell(++cellIndices2);
        cell.setCellValue("INSTITUTION" );
        cell.setCellStyle(style);


        cell = row.createCell(++cellIndices2);
        cell.setCellValue("UNIT (IF AVAILABLE OTHERWISE INST.)" );
        cell.setCellStyle(style);

        //BIBLIOGRAPHIC INFO

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_TYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_YEAR" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_CONTENT" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_SUBTYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_STATUS" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_AFFILIATIONS" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_URN" );
        cell.setCellStyle(style);


        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_POTENTIAL_CHANNELS" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_TITLE" );
        cell.setCellStyle(style);


        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_JOURNAL" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_JOURNAL_ISSN" );
        cell.setCellStyle(style);


        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_SERIES" );
        cell.setCellStyle(style);


        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_SERIES_ISSN" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_PUBLISHER" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_ISBN" );
        cell.setCellStyle(style);


        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_LANGUAGE" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_LAST_CHANGED" );
        cell.setCellStyle(style);

        //cell = row.createCell(32);
        //cell.setCellValue("DIVA_NR_AUTHORS" );
        //cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_IS_DUPLICATED" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DIVA_DUPLICATED_PID" );
        cell.setCellStyle(style);

        //NORSK STUFF


        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NORWEGIAN_ID" );
        cell.setCellStyle(style);


        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NORWEGIAN_TYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NORWEGIAN_MATCH" );
        cell.setCellStyle(style);


        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NORWEGIAN_NAME" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NORWEGIAN_LEVEL" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NORWEGIAN_LEVEL (OMKODAD)" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NORWEGIAN_LEVEL (HISTORICAL MAXIMUM)" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NORWEGIAN_MODEL_INFO" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("PUBLICATION_MODEL_INFO" );
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NORWEGIAN_POINTS" );
        cell.setCellStyle(style);

        //add localID, use to manually remap institutionen för geografi och ekonomisk historia

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("LOCAL_ID");
        cell.setCellStyle(style);

        //add info if the affiliation has been altered, i.e., non considered removed (0=no, 1=yes, 2=count not remove, only non considered

        //cell = row.createCell(48);
        //cell.setCellValue("AFFIL_MAPPING_INFO");
        //cell.setCellStyle(style);

        //2024 add IDs

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("DOI");
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("EID");
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("WOS");
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("PMID");
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("ANY_UMU_AUTHOR_ON_RECORD_LEVEL");
        cell.setCellStyle(style);

        //calculated points

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NORWEGIAN_POINTS_TIMES_FRACTION (STRAIGHT)");
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NORWEGIAN_POINTS_TIMES_FRACTION (STRAIGHT, MIN 0.1)");
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NORWEGIAN_POINTS_TIMES_FRACTION (WEIGHTED, U-SHAPED)");
        cell.setCellStyle(style);

        cell = row.createCell(++cellIndices2);
        cell.setCellValue("NORWEGIAN_POINTS_TIMES_FRACTION (WEIGHTED, LiU/UMU-MODEL)");
        cell.setCellStyle(style);




        // populate header cells...
        for (int c = 0; c <sheet.getRow(0).getLastCellNum(); c++) {
            sheet.autoSizeColumn(c);
        }


        for(Post p : recordList) {

            String summary2 = p.getRawDataRow()[ ReducedDiVAColumnIndices.Abstract.getValue() ];
            if(summary2.length() > 32700 ) { summary2 = summary2.substring(0,32000); summary2 = summary2.concat("[TRUNCATED!!]");       }

            row2 = sheet2.createRow(++rowIndices2);
            cellIndices2 = -1;

            //PID
            cell2 = row2.createCell(++cellIndices2);
            cell2.setCellValue(p.getPID());

            //ABSTRACTS

            cell2 = row2.createCell(++cellIndices2);
            cell2.setCellValue(summary2);



            double checkSumOfStraightFractions = 0;
            for(Author a : p.getAuthorList()) {  checkSumOfStraightFractions += a.getStraightFractions(); }


            for(Author author : p.getAuthorList() ) {


                List<DivaIDtoNames> divaIDtoNames = author.getAffilMappingsObjects();
                boolean isUmuAuthor = !divaIDtoNames.isEmpty();

                if(!includeExternalAuthors && !isUmuAuthor) continue;


                //show we can split fractions correctly
                int totalUmUnits = divaIDtoNames.size();
                int nrCentrumLikeUnits = 0;
                for(DivaIDtoNames divaAdr : divaIDtoNames) {

                    if(!divaAdr.CONSIDER_WHEN_FRACTIONALISING) nrCentrumLikeUnits++;

                }

                int nrStandardUnits = totalUmUnits - nrCentrumLikeUnits;


                NorwegianMatchInfo norskNivå = p.getNorskNivå();



                int indice = 0;
                do { // while indice < divaIDtoNames.size()


                    boolean isNormalUmUUnit = (isUmuAuthor && divaIDtoNames.get(indice).isCONSIDER_WHEN_FRACTIONALISING());
                    boolean isUmUCentrum = (isUmuAuthor && !divaIDtoNames.get(indice).isCONSIDER_WHEN_FRACTIONALISING());
                    boolean isDualCounting = isUmUCentrum && (nrStandardUnits > 0);

                    double straightFraction, min01,uShaped,liuStyle;

                    if(isUmuAuthor) {

                        straightFraction = isNormalUmUUnit ? author.getStraightFractions() / nrStandardUnits : (author.getStraightFractions() /nrCentrumLikeUnits); //put the author fraction as a whole on the centrum and mark it
                        min01 = isNormalUmUUnit ? author.getStraightFractionsMin01() / nrStandardUnits : (author.getStraightFractionsMin01() /nrCentrumLikeUnits);
                        uShaped = isNormalUmUUnit ? author.getUshapedFractions() /nrStandardUnits : (author.getUshapedFractions() /nrCentrumLikeUnits);
                        liuStyle = isNormalUmUUnit ? author.getLiuStyleFractions() /nrStandardUnits : (author.getLiuStyleFractions() /nrCentrumLikeUnits);



                    } else {


                        straightFraction = author.getStraightFractions();
                        min01 = author.getStraightFractionsMin01();
                        uShaped = author.getUshapedFractions();
                        liuStyle = author.getLiuStyleFractions();

                    }





                    row = sheet.createRow(++rowIndices);
                    cellIndices = -1;

                    //PID  0
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getPID());

                    //AUTHOR_ID" 1

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getDisambiguateID());


                    //AUTHOR_NAME" 2

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getAuthorName() );

                    //CAS 1" 3

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getCas() );

                    //CAS 2" 4

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getAutomaticAddedCass() );

                    //CAS STRAIGHT FRACTIONS 5

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( straightFraction );

                    // STRAIGHT FRACTIONS MIN 0.1 6
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( min01 );


                    //BYLINE WEIGHTED A 7

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( uShaped );


                    //BYLINE WEIGHTED B 8

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( liuStyle );


                    //BYLINE CATEGORY 9

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(author.getAuthorBylineCategory() );

                    //IMPORTANT
                    //DOUBLE COUNTING INDUCED BY CENTRUM-INSTITUTION COMBINATION
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(isDualCounting);

                    //IS CENTRUM LIKE
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(isUmUCentrum);



                    //NR AUTHORS 10

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getNrAuthors() );

                    //position @ pubyear 11

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( author.getBefattningAtPublicationDate() );

                    //Last known position 12

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( author.getLastKnownBeffattning() );



                    //FACULTY 13
                    String faculty = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getFAKULTET() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(faculty);

                    //INST 14

                    String inst = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getINSTITUTION() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(inst);


                    //INST 15 UNIT

                    String unit = ( isUmuAuthor ) ? divaIDtoNames.get(indice).getENHET() : "external";
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(unit);

                    //SET DIVA BIBLIOGRAPHIC STUFF

                    //DIVA TYPE 16
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaPublicationType()  );


                    //DIVA YEAR 17
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getYear()  );

                    //DIVA CONTENT 18
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaContentType()  );

                    //DIVA SUBTYPE 19
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaPublicationSubtype()  );

                    //DIVA status 20
                    cell = row.createCell(++cellIndices);

                    String status = p.getDivaStatus();

                    if("".equals(status)) {status = "published";}

                    cell.setCellValue( status );

                    //DIVA AFFILIATIONS 21
                    cell = row.createCell(++cellIndices);

                    String name = p.getRawDataRow()[ ReducedDiVAColumnIndices.Name.getValue() ];
                    if(name.length() > 32700 ) { name = name.substring(0,32000); name = name.concat("[TRUNCATED!!]");       }
                    cell.setCellValue( name );

                    //DIVA URN 22

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getNBN()  );

                    //DIVA CHANEL 23

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getDivaChannels()  );

                    //DIVA TITLE 24

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getTitle()  );


                    //DIVA JOURNAL 26

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getJournal()  );

                    //DIVA JOURNAL ISSN 27

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getJournalISSN()  );


                    //DIVA SERIES 28

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getSeriesName()  );


                    //DIVA SERIES ISSN 29

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getSeriesISSN()  );

                    //DIVA PUBLISHER 30

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getPublisher()  );

                    //DIVA PUBLISHER 31

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getISBN()  );



                    //DIVA LANGUAGE 32

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Language.getValue() ] );

                    //DIVA LAST UPDATED  33

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.LastUpdated.getValue() ] );

                    //DIVA NR AUTHORS 34
                    // cell = row.createCell(++cellIndices);
                    // cell.setCellValue(p.getNrAuthors());


                    //DIVA IS DUPE 35
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(  p.isDuplicate() );


                    //DIVA WHICH DUPE 36
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(  p.getDuplicateOfPID() );


                    //SET NORWEGIAN STUFF

                    //NORWEGIAN ID 37

                    cell = row.createCell(++cellIndices);

                    Integer nid = norskNivå.getNorsk_id();
                    if(nid.equals(-99)) { cell.setCellValue( "not available" ); } else {cell.setCellValue( nid ); }


                    //NORWEGIAN TYPE 38

                    cell = row.createCell(++cellIndices);
                    String type = norskNivå.getType();
                    cell.setCellValue((type == null) ? "not available" : type);

                    //NOWRWEGIAN MATCH 39

                    cell = row.createCell(++cellIndices);
                    String how = norskNivå.getHow();
                    cell.setCellValue((how == null) ? "not available" : how);

                    //NOWRWEGIAN NAME 40

                    cell = row.createCell(++cellIndices);
                    String namn = norskNivå.getNorsk_namn();
                    cell.setCellValue((namn == null) ? "not available" : namn);

                    //NORWEGIAN LEVEL 41

                    cell = row.createCell(++cellIndices);
                    Integer nivåStandard = norskNivå.getNivå();
                    cell.setCellValue((nivåStandard == null) ? "not available" : nivåStandard.toString());


                    //OMKODAD LEVEL , 0 + "not available" =  0 (ink. ej bedömd kanal)


                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( ((nivåStandard == null) || nivåStandard.equals(0)) ? "0 (ink. ej bedömd kanal)" : nivåStandard.toString());

                    //NORWEGIAN LEVEL 42

                    cell = row.createCell(++cellIndices);
                    Integer nivåMax = norskNivå.getMax_nivå();
                    cell.setCellValue((nivåMax == null) ? "not available" : nivåMax.toString());


                    //NORWEGIAN MODEL SPECIFIC INFO 43
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(     norskNivå.getModelSpecificInfo() );

                    //PUBLICATION MODEL SPECIFICATION 44
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(     p.getStatusInModel().getStatusInModel()  );


                    //NORWEGIAN POINTS 45

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(      norskNivå.getVikt()     );


                    //add localID 46

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.LocalId.getValue() ] );

                    //affil mapping info 47, we dont remove any longer
                    //cell = row.createCell(++cellIndices);
                    //cell.setCellValue(author.removedNonConsidered );

                    //ADD DOI 48

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.DOI.getValue() ] );

                    //ADD EID 49

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.ScopusId.getValue() ] );

                    //ADD WOS, 50

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.ISI.getValue() ] );

                    //ADD PMID 51

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( p.getRawDataRow()[ ReducedDiVAColumnIndices.PMID.getValue() ] );

                    //any UMU author 52

                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(p.isHasUMUAuthors());

                    //norwegian points, four different versions based on author fraction type

                    //STRAIGHT
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(straightFraction * p.getNorskNivå().getVikt() );

                    //MIN 0.1
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue( min01 * p.getNorskNivå().getVikt()  );

                    //U-SHAPED
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(uShaped * p.getNorskNivå().getVikt() );

                    //LiU Style
                    cell = row.createCell(++cellIndices);
                    cell.setCellValue(liuStyle * p.getNorskNivå().getVikt() );


                    indice++;
                } while(indice < divaIDtoNames.size() );



            } //for each author


            //TODO if indeterminably, then the sum of (straight) fractions will not equal 1.0. Lets add a fake row to fix this
            //but we should just add it if we show external authors
            if( Math.abs( checkSumOfStraightFractions - 1.0) > 0.00001 && includeExternalAuthors) {

                System.out.println(p.getPID() + " added fake row here, as missing fractions induced by bad registration " + checkSumOfStraightFractions);

                rowIndices = addFractionForConsistency(sheet,p,rowIndices,(1.0-checkSumOfStraightFractions)); //add a row and update row indice

            }


        } //for each post



        //ADD YEARS EMPLYMENT IF AVAILIABLE

        if(yearsAtUnits != null) {

            //System.out.println( yearsAtUnits.UMUID_TO_YEARS_AT_UNITS.get("crco0001") );

            /*
            SHEET 3, ABSTRACTS
            */

            XSSFSheet sheet3 = workbook.createSheet("FÖRFATTARE_ÅR_VID_UMU");
            XSSFFont font3 = workbook.createFont();
            XSSFCellStyle style3 = workbook.createCellStyle();
            font3.setBold(true);
            style3.setFont(font3);

            sheet3.createFreezePane(0,1);
            int cellIndices3 = 0;
            int rowIndices3 = 0;


            //SKAPA EN LÅST HEADER-RAD

            Row row3 = sheet3.createRow(rowIndices3);
            Cell cell3 = row3.createCell(0);
            cell3.setCellValue("UMUID");
            cell3.setCellStyle(style3);
            cell3 = row3.createCell(1);
            cell3.setCellValue("YEARS @ UMU (IN INTERVAL " + yearsAtUnits.minYears + "-" + yearsAtUnits.maxYears + ")");
            cell3.setCellStyle(style3);

            cell3 = row3.createCell(2);
            cell3.setCellValue("YEARS @ MEDFAK (IN INTERVAL " + yearsAtUnits.minYears + "-" + yearsAtUnits.maxYears + ")");
            cell3.setCellStyle(style3);

            cell3 = row3.createCell(3);
            cell3.setCellValue("YEARS @ TEKNAT (IN INTERVAL " + yearsAtUnits.minYears + "-" + yearsAtUnits.maxYears + ")");
            cell3.setCellStyle(style3);

            cell3 = row3.createCell(4);
            cell3.setCellValue("YEARS @ SAMFAK (IN INTERVAL " + yearsAtUnits.minYears + "-" + yearsAtUnits.maxYears + ")");
            cell3.setCellStyle(style3);

            cell3 = row3.createCell(5);
            cell3.setCellValue("YEARS @ HUMFAK (IN INTERVAL " + yearsAtUnits.minYears + "-" + yearsAtUnits.maxYears + ")");
            cell3.setCellStyle(style3);


            // populate header cells...
            for (int c = 0; c <sheet3.getRow(0).getLastCellNum(); c++) {
                sheet3.autoSizeColumn(c);
            }

            String[] UNITS = new String[]{"UMU","MEDFAK","SAMFAK","TEKNAT","HUMFAK"};

            for(Map.Entry<String,HashMap<String,TreeSet<Integer>>> entry : yearsAtUnits.UMUID_TO_YEARS_AT_UNITS.entrySet()) {


                row3 = sheet3.createRow(++rowIndices3);
                cellIndices3 = -1;

                cell3 = row3.createCell(++cellIndices3);
                cell3.setCellValue(entry.getKey());

                for(String unit : UNITS) {

                    //
                    cell3 = row3.createCell(++cellIndices3);
                    cell3.setCellValue( entry.getValue().get(unit).size() );


                }
            }





        } //years and stuff compleated


        System.out.println("Record list: " + recordList.size());
        for(Post p : recordList) {

            if(p.getPID() == 1951580) System.out.println(p.getPID() + " SEEN IN RECORDLIST");

        }

        String fileName;

        if(includeExternalAuthors) {fileName = "ShowExternalAuthors"; } else {fileName = "HideExternalAuthors";}


        try (FileOutputStream outputStream = new FileOutputStream(fileName + "." + dateFormat.format(date)  +".xlsx")) {
            workbook.setActiveSheet(0);
            workbook.setSelectedTab(0);
            workbook.write(outputStream);
            workbook.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void saveNorwegianMatchingInfo(List<Post> recordList) {


        XSSFSheet sheet = workbook.createSheet("MatchningsInfo");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        //SKAPA EN LÅST HEADER-RAD
        Row row = sheet.createRow(rowIndices);


        Cell cell = row.createCell(0);
        cell.setCellValue("PID" );
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("DIVA_TYP" );
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("ÅR" );
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("DIVA_INNEHÅLL" );
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("DIVA_SUBTYP" );
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("DIVA_STATUS" );
        cell.setCellStyle(style);

        cell = row.createCell(6);
        cell.setCellValue("DIVA_NAMN_AFFILS" );
        cell.setCellStyle(style);




        cell = row.createCell(7);
        cell.setCellValue("DIVA_NBN" );
        cell.setCellStyle(style);

        cell = row.createCell(8);
        cell.setCellValue("NORSK_ID" );
        cell.setCellStyle(style);


        cell = row.createCell(9);
        cell.setCellValue("NORSK_TYP" );
        cell.setCellStyle(style);

        cell = row.createCell(10);
        cell.setCellValue("MATCHNINGSINFO" );
        cell.setCellStyle(style);

        cell = row.createCell(11);
        cell.setCellValue("NORSK_NAMN" );
        cell.setCellStyle(style);

        cell = row.createCell(12);
        cell.setCellValue("NORSK NIVÅ" );
        cell.setCellStyle(style);

        cell = row.createCell(13);
        cell.setCellValue("NORSK NIVÅ (HISTORISKT MAXIMUM)" );
        cell.setCellStyle(style);


        cell = row.createCell(14);
        cell.setCellValue("MONDELLINFO" );
        cell.setCellStyle(style);


        //model specific info
        cell = row.createCell(15);
        cell.setCellValue("MODELLINFO2" );
        cell.setCellStyle(style);


        cell = row.createCell(16);
        cell.setCellValue("DIVA_KANALER" );
        cell.setCellStyle(style);



        cell = row.createCell(17);
        cell.setCellValue("TI" );
        cell.setCellStyle(style);

        cell = row.createCell(18);
        cell.setCellValue("ABSTRACT" );
        cell.setCellStyle(style);

        cell = row.createCell(19);
        cell.setCellValue("TIDSKRIFT" );
        cell.setCellStyle(style);

        cell = row.createCell(20);
        cell.setCellValue("TIDSKRIFT_ISSN" );
        cell.setCellStyle(style);


        cell = row.createCell(21);
        cell.setCellValue("SERIE" );
        cell.setCellStyle(style);

        cell = row.createCell(22);
        cell.setCellValue("SERIE_ISSN" );
        cell.setCellStyle(style);


        cell = row.createCell(23);
        cell.setCellValue("FÖRLAG" );
        cell.setCellStyle(style);


        cell = row.createCell(24);
        cell.setCellValue("FÖRLAG_ISBN" );
        cell.setCellStyle(style);

        cell = row.createCell(25);
        cell.setCellValue("SPRÅK" );
        cell.setCellStyle(style);


        cell = row.createCell(26);
        cell.setCellValue("DIVA_SENAST_ÄNDRAD" );
        cell.setCellStyle(style);

        cell = row.createCell(27);
        cell.setCellValue("NORSK_POÄNG" );
        cell.setCellStyle(style);

        cell = row.createCell(28);
        cell.setCellValue("NR_FÖRFATTARE" );
        cell.setCellStyle(style);

        cell = row.createCell(29);
        cell.setCellValue("DUBBLETT" );
        cell.setCellStyle(style);

        cell = row.createCell(30);
        cell.setCellValue("DUBLETT_PID" );
        cell.setCellStyle(style);

        cell = row.createCell(31);
        cell.setCellValue("NBN" );
        cell.setCellStyle(style);


        for(Post p : recordList) {


            row = sheet.createRow(++rowIndices);
            cellIndices = -1;

            //PID
            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getPID());

            //PUBTYPE
            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getDivaPublicationType() );

            //PUBYEAR
            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Year.getValue() ] );

            //CONTENT
            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.ContentType.getValue() ] );

            //CONTENT
            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.PublicationSubtype.getValue() ] );

            //CONTENT
            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Status.getValue() ] );


            cell = row.createCell(++cellIndices);

            String name = p.getRawDataRow()[ ReducedDiVAColumnIndices.Name.getValue() ];

            if(name.length() > 32700 ) { name = name.substring(0,32000); name = name.concat("[TRUNCATED!!]");       }
            cell.setCellValue( name );


            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.NBN.getValue() ] );


            NorwegianMatchInfo norskNivå = p.getNorskNivå();

            cell = row.createCell(++cellIndices);
            cell.setCellValue(norskNivå.getNorsk_id());

            cell = row.createCell(++cellIndices);
            String type = norskNivå.getType();
            cell.setCellValue((type == null) ? "null" : type);

            cell = row.createCell(++cellIndices);
            String how = norskNivå.getHow();
            cell.setCellValue((how == null) ? "null" : how);

            cell = row.createCell(++cellIndices);
            String namn = norskNivå.getNorsk_namn();
            cell.setCellValue((namn == null) ? "null" : namn);

            cell = row.createCell(++cellIndices);
            Integer nivåStandard = norskNivå.getNivå();
            cell.setCellValue((nivåStandard == null) ? "null" : nivåStandard.toString());

            cell = row.createCell(++cellIndices);
            Integer nivåMax = norskNivå.getMax_nivå();
            cell.setCellValue((nivåMax == null) ? "null" : nivåMax.toString());


            cell = row.createCell(++cellIndices);
            cell.setCellValue(     p.getStatusInModel().getStatusInModel()  );


            cell = row.createCell(++cellIndices);
            cell.setCellValue(     p.getNorskNivå().getModelSpecificInfo() );


            cell = row.createCell(++cellIndices);
            cell.setCellValue(          p.getDivaChannels() );


            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Title.getValue() ] );

            cell = row.createCell(++cellIndices);

            String abs = p.getRawDataRow()[ ReducedDiVAColumnIndices.Abstract.getValue() ];
            if(abs.length() > 32700 ) { abs = abs.substring(0,32000); abs = abs.concat("[TRUNCATED!!]");       }
            cell.setCellValue( abs );

           // cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Abstract.getValue() ] );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Journal.getValue() ] );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.JournalISSN.getValue() ] );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Series.getValue() ] );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.SeriesISSN.getValue() ] );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Publisher.getValue() ] );


            p.getISBN();

            cell = row.createCell(++cellIndices);
            cell.setCellValue(  p.getISBN()  );


            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.Language.getValue() ] );

            cell = row.createCell(++cellIndices);
            cell.setCellValue(p.getRawDataRow()[ ReducedDiVAColumnIndices.LastUpdated.getValue() ] );


            cell = row.createCell(++cellIndices);

            cell.setCellValue(  norskNivå.getVikt() );


            cell = row.createCell(++cellIndices);

            cell.setCellValue(  p.getNrAuthors() );

            cell = row.createCell(++cellIndices);

            cell.setCellValue(  p.isDuplicate() );

            cell = row.createCell(++cellIndices);

            cell.setCellValue(  p.getDuplicateOfPID() );

            cell = row.createCell(++cellIndices);

            cell.setCellValue(  p.getNBN() );


        }




        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm") ;


        try (FileOutputStream outputStream = new FileOutputStream("NorskMatchning" + dateFormat.format(date)  +".xlsx")) {
            workbook.setActiveSheet(0);
            workbook.setSelectedTab(0);
            workbook.write(outputStream);
            workbook.close();

        } catch (IOException e) {
            e.printStackTrace();
        }



    }


    public void addFullPublicationData(List<Post> postList) {

        XSSFSheet sheet = workbook.createSheet("Grunddata");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        //SKAPA EN LÅST HEADER-RAD
        Row row = sheet.createRow(rowIndices);
        for( ReducedDiVAColumnIndices indices : ReducedDiVAColumnIndices.values()) {

            Cell cell = row.createCell(cellIndices);
            cell.setCellValue(indices.toString());
            cell.setCellStyle(style);
            cellIndices++;

        }


        for(Post p : postList) {

            row = sheet.createRow(++rowIndices);
            cellIndices = -1;

            String[] rawRow =  p.getRawDataRow();

            for(int i=0; i<rawRow.length; i++) {
                Cell cell = row.createCell(++cellIndices);
                cell.setCellValue(rawRow[i]);

            }

        }


    }

    public void addBibliometricInformation(List<Author> consideredAuthorList) {

        XSSFSheet sheet = workbook.createSheet("PoängPerPublikation");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        ///////////////////SKAPA EN LÅST HEADER-RAD////////////////////////////////
        Row row = sheet.createRow(rowIndices);

        Cell cell = row.createCell(0);
        cell.setCellValue("PID" );
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("CAS" );
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("NAMN" );
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("AFFILIERING" );
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("TITEL" );
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("FÖRFATTARFRAKTION" );
        cell.setCellStyle(style);

        cell = row.createCell(6);
        cell.setCellValue("DIVA_TYP" );
        cell.setCellStyle(style);

        cell = row.createCell(7);
        cell.setCellValue("DIVA_INNEHÅLL" );
        cell.setCellStyle(style);

        cell = row.createCell(8);
        cell.setCellValue("NORSK_TYP" );
        cell.setCellStyle(style);

        cell = row.createCell(9);
        cell.setCellValue("MATCHNINGSINFO" );
        cell.setCellStyle(style);

        cell = row.createCell(10);
        cell.setCellValue("NORSK_ID" );
        cell.setCellStyle(style);

        cell = row.createCell(11);
        cell.setCellValue("NORSK_KANAL" );
        cell.setCellStyle(style);

        cell = row.createCell(12);
        cell.setCellValue("NORSK_NIVÅ(STANDARD)" );
        cell.setCellStyle(style);

        cell = row.createCell(13);
        cell.setCellValue("NORSK_NIVÅ(HISTORISKT MAXIMUM)" );
        cell.setCellStyle(style);


        cell = row.createCell(14);
        cell.setCellValue("PUBLIKATIONSINFO" );
        cell.setCellStyle(style);

        cell = row.createCell(15);
        cell.setCellValue("MODELLINFO" );
        cell.setCellStyle(style);

        cell = row.createCell(16);
        cell.setCellValue("POÄNG_I_MODELL" );
        cell.setCellStyle(style);

        cell = row.createCell(17);
        cell.setCellValue("PUBLICERINGSÅR" );
        cell.setCellStyle(style);

        cell = row.createCell(18);
        cell.setCellValue("DIVA_SUBTYPE" );
        cell.setCellStyle(style);

        cell = row.createCell(19);
        cell.setCellValue("NBN" );
        cell.setCellStyle(style);

        cell = row.createCell(20);
        cell.setCellValue("PART OF THESIS" );
        cell.setCellStyle(style);

        cell = row.createCell(21);
        cell.setCellValue("DIVA CHANNEL INFO" );
        cell.setCellStyle(style);


        for(Author a : consideredAuthorList) {

            boolean publishingAuthor = true;
            Post p = a.getEnclosingPost();
            if(p == null) publishingAuthor = false;

            if(publishingAuthor) {
                NorwegianMatchInfo norskNivå = p.getNorskNivå();

                //set

                row = sheet.createRow(++rowIndices);
                cellIndices = -1;

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getPID());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(a.getCas());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(a.getAuthorName());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(a.getAffiliations());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getTitle());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(a.getFractionIgnoreMultipleUmUAffils());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getDivaPublicationType());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getDivaContentType());

                cell = row.createCell(++cellIndices);
                String type = norskNivå.getType();
                cell.setCellValue((type == null) ? "null" : type);

                cell = row.createCell(++cellIndices);
                String how = norskNivå.getHow();
                cell.setCellValue((how == null) ? "null" : how);

                cell = row.createCell(++cellIndices);
                cell.setCellValue(norskNivå.getNorsk_id());

                cell = row.createCell(++cellIndices);
                String namn = norskNivå.getNorsk_namn();
                cell.setCellValue((namn == null) ? "null" : namn);

                cell = row.createCell(++cellIndices);
                Integer nivåStandard = norskNivå.getNivå();
                cell.setCellValue((nivåStandard == null) ? "null" : nivåStandard.toString());

                cell = row.createCell(++cellIndices);
                Integer nivåMax = norskNivå.getMax_nivå();
                cell.setCellValue((nivåMax == null) ? "null" : nivåMax.toString());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getStatusInModel().getStatusInModel());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(norskNivå.getModelSpecificInfo());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(norskNivå.getVikt());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getYear());

                //todo new
                cell = row.createCell(++cellIndices);

                String subtype = p.getDivaPublicationSubtype();
                if(subtype.length() < 2) subtype = "Standard";

                cell.setCellValue( subtype );
                //cell.setCellValue(p.getDivaPublicationSubtype() );

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getNBN());

                cell = row.createCell(++cellIndices);

                String partOfThesis = p.getPartOfThesis();
                if(partOfThesis.length() <= 2) partOfThesis = "NO";
                cell.setCellValue( partOfThesis );

                cell = row.createCell(++cellIndices);
                cell.setCellValue(p.getDivaChannels());


            } else {

                //non publishing author

                //set dummies

                row = sheet.createRow(++rowIndices);
                cellIndices = -1;

                cell = row.createCell(++cellIndices);
                cell.setCellValue( -99 );

                cell = row.createCell(++cellIndices);
                cell.setCellValue(a.getCas());

                cell = row.createCell(++cellIndices);
                cell.setCellValue(a.getAuthorName());

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue(0);

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);

                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue(-99);

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);

                cell.setCellValue("null");

                cell = row.createCell(++cellIndices);
                cell.setCellValue("null");

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue("DUMMY ROW");

                cell = row.createCell(++cellIndices);
                cell.setCellValue(0);

                cell = row.createCell(++cellIndices);
                cell.setCellValue(-99);






            }


        }


        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
       // sheet.autoSizeColumn(3);
       // sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);
        sheet.autoSizeColumn(8);
        sheet.autoSizeColumn(9);
        sheet.autoSizeColumn(10);
        sheet.autoSizeColumn(11);
        sheet.autoSizeColumn(12);
        sheet.autoSizeColumn(13);
        sheet.autoSizeColumn(14);
        sheet.autoSizeColumn(15);
        sheet.autoSizeColumn(16);
        sheet.autoSizeColumn(17);

    }


    public void addAggregatedAuthorStatisticsHumantistiskaFakulteten(PublicationPointPerAuthor sumAuthorWeights) {
        XSSFCellStyle decimalStyle = workbook.createCellStyle();
        decimalStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0"));

        XSSFSheet sheet = workbook.createSheet("PoängPerSökande");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        //SKAPA EN LÅST HEADER-RAD
        Row row = sheet.createRow(rowIndices);

        Cell cell = row.createCell(0);
        cell.setCellValue("SÖKANDE" );
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("ANTAL POSTER" );
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("FÖRFATTARFRAKTIONER" );
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("POÄNG TOTALT (A)" );
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("POÄNG TOTALT, MAX 5 ÖVRIGT PER ÅR (B)" );
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("FORSKNINGSTID (C) " );
        cell.setCellStyle(style);

        cell = row.createCell(6);
        cell.setCellValue("FORSKNINGSTIDSVIKT (D)" );
        cell.setCellStyle(style);

        cell = row.createCell(7);
        cell.setCellValue("INDIKATOR (B * D)" );
        cell.setCellStyle(style);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        TreeMap<String,AggregatedAuthorInformation> result = sumAuthorWeights.getAuthorsAggregatedStatistics();



        for(Map.Entry<String,AggregatedAuthorInformation> entry : result.entrySet()) {
            row = sheet.createRow(++rowIndices);
            cellIndices = -1;

            AggregatedAuthorInformation aggregatedAuthorInformation = entry.getValue();


            cell = row.createCell(++cellIndices);
            cell.setCellValue( entry.getKey() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRawSumOfPublications() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRawSumOfFractions() );
            cell.setCellType(CellType.NUMERIC);
            cell.setCellStyle(decimalStyle);


            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRawSumOfWeights() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRestrictedSumOfWeights() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getForskningstidProcent() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getForskningstidsVikt() );

            Double indikator =     ( aggregatedAuthorInformation.getRestrictedSumOfWeights() * aggregatedAuthorInformation.getForskningstidsVikt() );
            cell = row.createCell(++cellIndices);
            cell.setCellValue( indikator );


        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);

    }

    public void addAggregatedAuthorStatisticsLärarHögskolan(PublicationPointPerAuthor sumAuthorWeights) {

        XSSFCellStyle decimalStyle = workbook.createCellStyle();
        decimalStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0"));

        XSSFSheet sheet = workbook.createSheet("PoängPerSökande");
        XSSFFont font = workbook.createFont();
        XSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        sheet.createFreezePane(0,1);
        int cellIndices = 0;
        int rowIndices = 0;

        //SKAPA EN LÅST HEADER-RAD
        Row row = sheet.createRow(rowIndices);

        Cell cell = row.createCell(0);
        cell.setCellValue("SÖKANDE" );
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("ANTAL POSTER" );
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("FÖRFATTARFRAKTIONER" );
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("OVIKTAD POÄNG (A)" );
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("FRAKTIONS- OCH SAMMARBETSVIKTAD POÄNG (B)" );
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("FORSKNINGSTID (C) " );
        cell.setCellStyle(style);

        cell = row.createCell(6);
        cell.setCellValue("FORSKNINGSTIDSVIKT (D)" );
        cell.setCellStyle(style);

        cell = row.createCell(7);
        cell.setCellValue("INDIKATOR (B * D)" );
        cell.setCellStyle(style);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        TreeMap<String,AggregatedAuthorInformation> result = sumAuthorWeights.getAuthorsAggregatedStatistics();



        for(Map.Entry<String,AggregatedAuthorInformation> entry : result.entrySet()) {
            row = sheet.createRow(++rowIndices);
            cellIndices = -1;

            AggregatedAuthorInformation aggregatedAuthorInformation = entry.getValue();


            cell = row.createCell(++cellIndices);
            cell.setCellValue( entry.getKey() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRawSumOfPublications() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRawSumOfFractions() );
            cell.setCellType(CellType.NUMERIC);
            cell.setCellStyle(decimalStyle);


            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getRawSumOfWeights() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getFractionalizedPubSum() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getForskningstidProcent() );

            cell = row.createCell(++cellIndices);
            cell.setCellValue( aggregatedAuthorInformation.getForskningstidsVikt() );

            Double indikator =     ( aggregatedAuthorInformation.getFractionalizedPubSum() * aggregatedAuthorInformation.getForskningstidsVikt() );
            cell = row.createCell(++cellIndices);
            cell.setCellValue( indikator );


        }



        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);

    }




    public void saveToFile(String m) {

        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm") ;


        try (FileOutputStream outputStream = new FileOutputStream("Resultat" + dateFormat.format(date) + "-" + m +".xlsx")) {
            workbook.setSheetOrder("PoängPerSökande",0);
            workbook.setSheetOrder("PoängPerPublikation",1);
            workbook.setSheetOrder("Grunddata",2);
            workbook.setActiveSheet(0);
            workbook.setSelectedTab(0);
            workbook.write(outputStream);
            workbook.close();

        } catch (IOException e) {
            e.printStackTrace();
        }



    }



    public void saveDumpOnly(String path) {

        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm") ;


        try (FileOutputStream outputStream = new FileOutputStream(path +"\\dump.xlsx")) {
            workbook.setSheetOrder("Grunddata",0);
            workbook.setActiveSheet(0);
            workbook.setSelectedTab(0);
            workbook.write(outputStream);
            workbook.close();

        } catch (IOException e) {
            e.printStackTrace();
        }



    }




}
