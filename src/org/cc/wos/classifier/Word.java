package org.cc.wos.classifier;

/**
 * Created by crco0001 on 9/9/2016.
 */
import java.io.Serializable;

/**
 * Created by crco0001 on 9/7/2016.
 */
public class Word implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int CURRENT_SERIAL_VERSION = 0;

    String word;
    double ruleno;

    public Word(String word, double ruleno) {
        this.word = word;
        this.ruleno = ruleno;
    }


}
