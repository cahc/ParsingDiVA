package org.cc.taxonomy;

import cc.FilePathConstants;
import cc.TextUtils.CleanAndCompare;
import cc.database.BerkeleyStorage;
import cc.database.ScopusRecord;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.PrimaryIndex;


import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class Playground {

    /*

    ## Eligible DiVA publications

    An **eligible DiVA publication** is a distinct publication identifier (PID) with at least one qualifying row in Umeå University’s Norwegian-model export. The export contains author and affiliation rows, so one publication can appear several times. Eligibility is assessed on those rows first, then the surviving rows are counted once per PID. A publication with both Umeå University and external coauthors can therefore qualify through a single surviving row. [Norwegian-model methodology](/Users/cc/BibliometricGUI/src/main/webapp/norwegian-model-methodology.html:39) · [NorwegianModelRepository.java](/Users/cc/BibliometricGUI/src/main/java/se/crco/bibliometricgui/NorwegianModelRepository.java:19)

    The documented publication rules cover published scientific output. This includes journal articles and letters, research reviews, books and monographs, chapters and anthology contributions, and published conference papers. The methodology includes peer-reviewed and other scientific content and excludes popular-science and debate material. It also excludes unpublished records, ordinary reviews such as *Artikel, recension*, meeting abstracts, editorials and newspaper items, posters and oral presentations, theses, manuscripts, patents, reports, edited proceedings and collections, and the residual “other” category. A publication in a level 0 or unassessed channel can still qualify. Channel level determines its contribution to the Norwegian publication indicator. [Norwegian-model methodology](/Users/cc/BibliometricGUI/src/main/webapp/norwegian-model-methodology.html:39)

    In practice, the dashboard uses the classification already recorded in `NORWEGIAN_MODEL_INFO`. It excludes rows marked as ignored for abstracts, posters, news items and similar material, rows marked as ignored because the record is unpublished, and rows with a missing model classification. It also excludes rows whose faculty is `external`. In the all-university view, the centrum–institution double-counting flag must be explicitly `FALSE`. This prevents a parallel centre affiliation from adding a second copy of the same contribution. A missing faculty can pass that filter, while a missing double-counting flag cannot. For the scope we’ve been discussing, all years, faculties, and institutions are considered together. [NorwegianModelRepository.java](/Users/cc/BibliometricGUI/src/main/java/se/crco/bibliometricgui/NorwegianModelRepository.java:19) · [Scope and double-counting rules](/Users/cc/BibliometricGUI/src/main/java/se/crco/bibliometricgui/NorwegianModelRepository.java:124)

    ## Eligible publications with a SciVal match

    The **SciVal subset** starts with those same eligible DiVA PIDs. A publication enters the subset when its PID has at least one row in the DiVA-to-SciVal match export with status `EXACT`, `EXACT_SUSPECT`, or `TEXT_AUTO_MATCH`. The dashboard consumes the status assigned by the upstream matcher, including `EXACT_SUSPECT` as an accepted category. Ambiguous matches, identifier conflicts, ignored matches, and unmatched records fall outside the subset. [CitationMetricsRepository.java](/Users/cc/BibliometricGUI/src/main/java/se/crco/bibliometricgui/CitationMetricsRepository.java:19)

    The counting unit is still the **DiVA PID**. The SciVal EID identifies the linked record, and several PIDs can share one EID. If multiple accepted rows exist for a PID, the repository averages their indicator values within that PID before calculating publication-level averages. The PID-to-EID function from earlier adds a requirement specific to returning a map. It needs a nonblank EID and reports a conflict if one PID has multiple distinct accepted EIDs. [CitationMetricsRepository.java](/Users/cc/BibliometricGUI/src/main/java/se/crco/bibliometricgui/CitationMetricsRepository.java:23) · [SciVal reference methodology](/Users/cc/BibliometricGUI/src/main/webapp/bibliometric_reference_methodology.html:416)

    An accepted match establishes membership in the subset. To contribute to the **citation indicators**, a PID also needs all four observed and expected Top 10% and Top 50% values. International collaboration requires its own observed and expected pair. The dashboard’s displayed **citation coverage** divides the number of PIDs with complete citation indicators by the total number of eligible DiVA PIDs. [CitationMetricsRepository.java](/Users/cc/BibliometricGUI/src/main/java/se/crco/bibliometricgui/CitationMetricsRepository.java:41) · [Dashboard coverage calculation](/Users/cc/BibliometricGUI/src/main/webapp/assets/app.js:611)

    Upstream  SciValDocumentTypePolicy.readEligibleRecords() skips Retracted, Abstract Report, Erratum, Editorial, and Conference Review.



     */


    public static List<Long> getNorwegianEligiblePids(Path parquetPath) throws SQLException {
        String sql =
                "SELECT DISTINCT \"PID\" FROM read_parquet(?) "
                        + "WHERE \"NORWEGIAN_MODEL_INFO\" NOT IN ("
                        + "'IGNORERAD (ABSTARACT, POSTER, NEWS ITEM ETC.)', "
                        + "'IGNORERAD (STATUS EJ PUBLICERAD)') "
                        + "AND \"FACULTY\" IS DISTINCT FROM 'external' "
                        + "AND \"DOUBLE COUNTING INDUCED BY CENTRUM-INSTITUTION COMBINATION\" = FALSE "
                        + "AND \"PID\" IS NOT NULL "
                        + "ORDER BY \"PID\"";

        List<Long> pids = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection("jdbc:duckdb:");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parquetPath.toAbsolutePath().normalize().toString());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    pids.add(rows.getLong(1));
                }
            }
        }
        return pids;

    }

    public static Map<String, String> getEligibleSciValMatches(Path norwegianParquet, Path matchesParquet)
            throws SQLException {
        String sql =
                "WITH eligible_pids AS ("
                        + "SELECT DISTINCT PID FROM read_parquet(?) "
                        + "WHERE NORWEGIAN_MODEL_INFO NOT IN ("
                        + "'IGNORERAD (ABSTARACT, POSTER, NEWS ITEM ETC.)', "
                        + "'IGNORERAD (STATUS EJ PUBLICERAD)') "
                        + "AND FACULTY IS DISTINCT FROM 'external' "
                        + "AND \"DOUBLE COUNTING INDUCED BY CENTRUM-INSTITUTION COMBINATION\" = FALSE"
                        + ") "
                        + "SELECT DISTINCT eligible.PID, matches.EID "
                        + "FROM eligible_pids eligible "
                        + "JOIN read_parquet(?) matches ON eligible.PID = TRY_CAST(matches.PID AS BIGINT) "
                        + "WHERE matches.STATUS IN ('EXACT', 'EXACT_SUSPECT', 'TEXT_AUTO_MATCH') "
                        + "AND matches.EID IS NOT NULL AND TRIM(matches.EID) <> '' "
                        + "ORDER BY eligible.PID";

        Map<String, String> pidToEid = new LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection("jdbc:duckdb:");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, norwegianParquet.toAbsolutePath().normalize().toString());
            statement.setString(2, matchesParquet.toAbsolutePath().normalize().toString());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String pid = rows.getString(1);
                    String eid = rows.getString(2);
                    String previous = pidToEid.putIfAbsent(pid, eid);
                    if (previous != null) {
                        throw new SQLException("Multiple accepted SciVal EIDs for PID " + pid
                                + ": " + previous + " and " + eid);
                    }
                }
            }
        }
        return pidToEid;
    }

    public static void main(String[] args) throws SQLException {


        /*

        This is a sketch pad, our immediate aim is to produce text representations for all Eligible DiVA record/PIDs, it will be used as input to an embedding model, Qwen 3 Qwen/Qwen3-Embedding-4B

        I am thinkin what we should produce something like:

        PID\TEXT

        Where text is Title + CleanAndCompare.truncateText( CleanAndCompare.removeCopyrightStatement( abstract ), 300 ) + keywords + source(e.g. journal) + SCB classification

        We have a couple of data sources to our disposal:




         */

       List<Long> PIDs = getNorwegianEligiblePids(Path.of("C:\\opt\\bibliometric_gui_static_data_files\\Norwegian model (including external authors)_2026-11-26 01-05.parquet"));
       System.out.println("Eligible PIDs " + PIDs.size());


       Map<String,String> PIDtoMatchedScival = getEligibleSciValMatches(Path.of("C:\\opt\\bibliometric_gui_static_data_files\\Norwegian model (including external authors)_2026-11-26 01-05.parquet"),Path.of("C:\\opt\\bibliometric_gui_static_data_files\\DIVA_PID_TO_SCIVAL_EID_20261126_010148.parquet"));
       System.out.println("Eligible SciVal matched: " + PIDtoMatchedScival.size());
       System.out.println("SciVal coverage: " + (PIDtoMatchedScival.size()/(double)PIDs.size())*100.0 );
       for(Map.Entry<String,String> entry : PIDtoMatchedScival.entrySet()) {

           System.out.println(entry.getKey() + ": " + entry.getValue());
       }






        System.exit(0);

        /*


        Local DB with UMU records from Scopus, potenially use these for augmenting some PID/DiVA records that lacks, e.g., abstract

         */

        BerkeleyStorage<String, ScopusRecord> berkeleyStorage = new BerkeleyStorage<>(FilePathConstants.SCOPUS_DB, false, false, String.class, ScopusRecord.class, null);
        PrimaryIndex<String, ScopusRecord> primaryIndex = berkeleyStorage.getPrimaryIndex();
        EntityCursor<ScopusRecord> iterator = primaryIndex.entities();

        for(ScopusRecord scopusRecord : iterator) {

            System.out.println(scopusRecord.getAbstractsRetrievalResponse().getCoredata().getEid());
        }

        iterator.close();
        berkeleyStorage.close();










    }



}
