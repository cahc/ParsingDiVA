package org.cc.wos.classifier;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.TokenSequence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by crco0001 on 9/9/2016.
 */
public class KeywordFromSourceToTokens extends Pipe implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int CURRENT_SERIAL_VERSION = 0;
    private static Pattern pattern = Pattern.compile("[\\p{L}\\p{N}- ]+");

    @Override
    public Instance pipe(Instance carrier) {
        String keywordString = (String)carrier.getSource();
        Matcher matcher = pattern.matcher(keywordString);
        ArrayList<String> arrayList = new ArrayList<>();

        while(matcher.find()) {

            String match = matcher.group().trim().toLowerCase();
            arrayList.add(match);
        }

        TokenSequence sequence = (TokenSequence)carrier.getData();
        for(String s : arrayList) {

            sequence.add(s);
        }

        return carrier;
    }





}
