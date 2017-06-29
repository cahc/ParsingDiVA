package org.cc.wos;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by crco0001 on 8/26/2016.
 */
public class BOMSkipper {

    public static void skip(Reader reader) throws IOException
    {
        reader.mark(1);
        char[] possibleBOM = new char[1];
        reader.read(possibleBOM);

        if (possibleBOM[0] != '\ufeff')
        {
            reader.reset();
        }
    }
}
