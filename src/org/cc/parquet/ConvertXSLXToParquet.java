package org.cc.parquet;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConvertXSLXToParquet {


    public static void DiVAMatchingXLSXToParquet(String inputXLSX, String outputParquet) throws SQLException {


        System.out.println("Converting divaToScival to paraquet");
        try (Connection conn =
                     DriverManager.getConnection("jdbc:duckdb:")) {

            try (Statement stmt = conn.createStatement()) {

                stmt.execute("INSTALL excel");
                stmt.execute("LOAD excel");

                stmt.execute("""
                    
                    
                    COPY (
                        SELECT *
                        FROM read_xlsx(
                            '%s',
                            header = true
                        )
                    )
                    TO '%s'
                    (
                        FORMAT PARQUET,
                        COMPRESSION ZSTD
                    )
                    """.formatted(
                        inputXLSX.replace("\\", "/"),
                        outputParquet.replace("\\", "/")
                ));
            }
        }


    }

    public static void NorwegianXLSXToParquet(String inputXLSX, String outputParquet) throws SQLException {


        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement stmt = conn.createStatement()) {

            stmt.execute("INSTALL rusty_sheet FROM community");
            stmt.execute("LOAD rusty_sheet");

            stmt.execute("""
        COPY (
            SELECT
                * EXCLUDE (NORWEGIAN_ID),
                CASE
                    WHEN TRIM(NORWEGIAN_ID) = 'not available' THEN -99
                    ELSE CAST(NORWEGIAN_ID AS INTEGER)
                END AS NORWEGIAN_ID
            FROM read_sheet(
                '%s',
                sheet = 'FÖRFATTARFRAKTIONER',
                header = true,
                columns = {'NORWEGIAN_ID': 'VARCHAR'}
            )
        )
        TO '%s'
        (
            FORMAT PARQUET,
            COMPRESSION ZSTD
        )
        """.formatted(
                    inputXLSX.replace("\\", "/"),
                    outputParquet.replace("\\", "/")
            ));
        }


    }



    public static void main(String[] args) throws Exception {

        String xlsxDivaMappingToSciVal = "DIVA_PID_TO_SCIVAL_EID.XLSX";
        String parquetDivaMappingToSciVal = "divaToScival.parquet";

        DiVAMatchingXLSXToParquet(xlsxDivaMappingToSciVal, parquetDivaMappingToSciVal);


        String xlsxDivaNorwegian = "C:\\opt\\bibliometric_gui_static_data_files\\Norwegian model (including external authors)_2026-09-30 10-25.xlsx";
        String parquetDivaNorwegian = "divaNorwegian.parquet";

        NorwegianXLSXToParquet(xlsxDivaNorwegian, parquetDivaNorwegian);



    }


}
