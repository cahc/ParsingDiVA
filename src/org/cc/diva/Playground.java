package org.cc.diva;

import java.util.List;

public class Playground {

    public static void main(String[] arg) {

        //String[] authorsAndAddresses = DivaHelpFunctions.splitAuthorInformation("Oldgren, Jonas (Uppsala Clinical Research Center (J.O.; Department of Medical Sciences, Uppsala University, S.Å);Åsberg, Signild (Department of Medical Sciences, Uppsala University, S.Å);Hijazi, Ziad (Uppsala Clinical Research Center (J.O.; Department of Medical Sciences, Uppsala University, S.Å);Wester, Per [pewe0002] [0000-0001-8522-1707] (Umeå universitet [715], Medicinska fakulteten [716], Institutionen för folkhälsa och klinisk medicin [723]) (Department of Clinical Science, Karolinska Institutet Danderyds sjukhus, Stockholm, Sweden);Bertilsson, Maria (Uppsala Clinical Research Center (J.O.);Norrving, Bo (Department of Clinical Sciences' Section of Neurology' Lund University' Skåne University Hospital, Sweden)");

        String[] authorsAndAddresses = DivaHelpFunctions.splitAuthorInformation("Thulesius, Hans (Department of Clinical Sciences Malmö, Family Medicine, Lund University, Lund, Sweden;Research and Development Centre, Region Kronoberg, Växjö, Sweden;Department of Medicine and Optometry, Linnaeus University, Kalmar, Sweden;Department of Design Sciences, Faculty of Engineering, Lund University, Lund, Sweden);Sandén, Ulrika (Department of Design Sciences, Faculty of Engineering, Lund University, Lund, Sweden);Petek, Davorina (Department of Family Medicine, Faculty of medicine, University of Ljubljana, Ljubljana, Slovenia);Hoffman, Robert (Departments of Family Medicine & Medical Education, Sackler Medical School, Tel Aviv University, Tel Aviv, Israel);Koskela, Tuomas (Department of General Practice, School of Medicine, University of Tampere, Tampere, Finland);Oliva-Fanlo, Bernardino (Department of Primary Health Care, Palma, Spain);Neves, Ana Luísa (Patient Safety Translational Research Centre, Institute of Global Health Innovation, Imperial College London, London, UK;Department of Community Medicine, Health Information and Decision, Faculty of Medicine, University of Porto, Porto, Portugal);Hajdarevic, Senada [seahai01] [0000-0003-0661-8269] (Umeå universitet [715], Medicinska fakulteten [716], Institutionen för omvårdnad [769]);Harrysson, Lars (Department of Design Sciences, Faculty of Engineering, Lund University, Lund, Sweden;School of Social Work, Faculty of Social Sciences, Lund University, Lund, Sweden);Toftegaard, Berit Skjodeborg (Department of Emergency Medicine, Horsens Hospital, Horsens, Denmark);Vedsted, Peter (Department of Clinical Medicine, Research Unit for General Practice, The Research Centre for Cancer Diagnosis in Primary Care, Aarhus University, Aarhus, Denmark);Harris, Michael (College of Medicine & Health, University of Exeter, Exeter, UK;Institute of Primary Health Care (BIHAM), University of Bern, Bern, Switzerland)");

        for(String s : authorsAndAddresses) {

            System.out.println(s);
        }


        System.out.println();

        //String[] authAdd = DivaHelpFunctions.splitAuthInformationNonRegexExperimental("Oldgren, Jonas (Uppsala Clinical Research Center (J.O.; Department of Medical Sciences, Uppsala University, S.Å);Åsberg, Signild (Department of Medical Sciences, Uppsala University, S.Å);Hijazi, Ziad (Uppsala Clinical Research Center (J.O.; Department of Medical Sciences, Uppsala University, S.Å);Wester, Per [pewe0002] [0000-0001-8522-1707] (Umeå universitet [715], Medicinska fakulteten [716], Institutionen för folkhälsa och klinisk medicin [723]) (Department of Clinical Science, Karolinska Institutet Danderyds sjukhus, Stockholm, Sweden);Bertilsson, Maria (Uppsala Clinical Research Center (J.O.);Norrving, Bo (Department of Clinical Sciences' Section of Neurology' Lund University' Skåne University Hospital, Sweden)");
        String[] authAdd = DivaHelpFunctions.splitAuthInformationNonRegexExperimental("Thulesius, Hans (Department of Clinical Sciences Malmö, Family Medicine, Lund University, Lund, Sweden;Research and Development Centre, Region Kronoberg, Växjö, Sweden;Department of Medicine and Optometry, Linnaeus University, Kalmar, Sweden;Department of Design Sciences, Faculty of Engineering, Lund University, Lund, Sweden);Sandén, Ulrika (Department of Design Sciences, Faculty of Engineering, Lund University, Lund, Sweden);Petek, Davorina (Department of Family Medicine, Faculty of medicine, University of Ljubljana, Ljubljana, Slovenia);Hoffman, Robert (Departments of Family Medicine & Medical Education, Sackler Medical School, Tel Aviv University, Tel Aviv, Israel);Koskela, Tuomas (Department of General Practice, School of Medicine, University of Tampere, Tampere, Finland);Oliva-Fanlo, Bernardino (Department of Primary Health Care, Palma, Spain);Neves, Ana Luísa (Patient Safety Translational Research Centre, Institute of Global Health Innovation, Imperial College London, London, UK;Department of Community Medicine, Health Information and Decision, Faculty of Medicine, University of Porto, Porto, Portugal);Hajdarevic, Senada [seahai01] [0000-0003-0661-8269] (Umeå universitet [715], Medicinska fakulteten [716], Institutionen för omvårdnad [769]);Harrysson, Lars (Department of Design Sciences, Faculty of Engineering, Lund University, Lund, Sweden;School of Social Work, Faculty of Social Sciences, Lund University, Lund, Sweden);Toftegaard, Berit Skjodeborg (Department of Emergency Medicine, Horsens Hospital, Horsens, Denmark);Vedsted, Peter (Department of Clinical Medicine, Research Unit for General Practice, The Research Centre for Cancer Diagnosis in Primary Care, Aarhus University, Aarhus, Denmark);Harris, Michael (College of Medicine & Health, University of Exeter, Exeter, UK;Institute of Primary Health Care (BIHAM), University of Bern, Bern, Switzerland)");

        for(String s : authAdd) {

            System.out.println(s);
        }


    }
}
