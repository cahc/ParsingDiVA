package org.cc.wos.classifier;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;

import java.io.Serializable;

/**
 * Created by crco0001 on 9/9/2016.
 */
public class RemoveCopyRightStatement extends Pipe implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int CURRENT_SERIAL_VERSION = 0;

    PatternMatcher patternMatcher;
    public RemoveCopyRightStatement(PatternMatcher patternMatcher) {

        this.patternMatcher = patternMatcher;
    }
    @Override
    public Instance pipe(Instance carrier) {
        String string = (String)carrier.getData();

        string = this.patternMatcher.cleanStringFromCopyrightStatements(string);

        carrier.setData(string);
        return carrier;
    }




}