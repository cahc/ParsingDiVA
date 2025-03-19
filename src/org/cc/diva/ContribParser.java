package org.cc.diva;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ContribParser {

    public static String newAuthorField(String fromContributorField) {

        StringBuilder stringBuilder = new StringBuilder();

        // 1) Split on semicolons.
        String[] authorSegments = fromContributorField.split("\\s*;\\s*");

        // 2) Regex to capture “author part” and (optionally) “roles part”.
        Pattern pattern = Pattern.compile(
                "^(?<author>.*?)"            // Group 'author': all text up to...
                        + "\\s*(?:Roles:\\{(?<roles>[^}]*)\\})?$" // optional “Roles:{...}” part
        );

        for (String segment : authorSegments) {
            Matcher matcher = pattern.matcher(segment.trim());
            if (matcher.matches()) {
                String authorPart = matcher.group("author").trim();
                String rolesPart  = matcher.group("roles");  // might be null

                // 3) Check if edt is present
                boolean isEdt = rolesPart != null && rolesPart.contains("[edt]");

                if (isEdt) {
                if(stringBuilder.length() == 0) { stringBuilder.append(authorPart); } else { stringBuilder.append(";").append(authorPart); }
                }
            }
                //System.out.println("Author/Org: " + authorPart);
                //System.out.println("Is Editor? : " + isEdt);
                //System.out.println("----");
            }


        return stringBuilder.toString();

    }


    public static void main(String[] args) {
       // String input = "Moberger, Victor [vimo0031] [0000-0001-7842-0900] (Umeå universitet [715], Humanistiska fakulteten [803], Institutionen för idé- och samhällsstudier [820]) Roles:{Editor [edt],Author of introduction, etc. [aui]};"
      //          + "Olson, Jonas (Stockholm University, Stockholm, Sweden) Roles:{Editor [edt],Author of introduction, etc. [aui]};"
      //          + "Observatoire Francophone de la Sclérose en Plaques (OFSEP),  Roles:{Contributor [ctb]};"
      //          + "Davallou, Aida Roles:{Editor [edt]}";


        String input = "Brorsson, Camilla [brca0001] [0000-0002-2113-8098] (Umeå universitet [715], Medicinska fakulteten [716], Institutionen för kirurgisk och perioperativ vetenskap [792], Anestesiologi och intensivvård [794]) Roles:{Contributor [ctb]};Koskinen, Lars-Owe D. [lako0002] [0000-0003-3528-8502] (Umeå universitet [715], Medicinska fakulteten [716], Institutionen för farmakologi och klinisk neurovetenskap [717], Klinisk neurovetenskap [10950]) Roles:{Contributor [ctb]};Sundström, Nina [nian0004] (Umeå universitet [715], Medicinska fakulteten [716], Institutionen för strålningsvetenskaper [776]) Roles:{Contributor [ctb]}";

        // 1) Split on semicolons.
        String[] authorSegments = input.split("\\s*;\\s*");

        // 2) Regex to capture “author part” and (optionally) “roles part”.
        Pattern pattern = Pattern.compile(
                "^(?<author>.*?)"            // Group 'author': all text up to...
                        + "\\s*(?:Roles:\\{(?<roles>[^}]*)\\})?$" // optional “Roles:{...}” part
        );

        for (String segment : authorSegments) {
            Matcher matcher = pattern.matcher(segment.trim());
            if (matcher.matches()) {
                String authorPart = matcher.group("author").trim();
                String rolesPart  = matcher.group("roles");  // might be null

                // 3) Check if edt is present
                boolean isEdt = rolesPart != null && rolesPart.contains("[edt]");

                System.out.println("Author/Org: " + authorPart);
                System.out.println("Is Editor? : " + isEdt);
                System.out.println("----");
            }
        }
    }
}