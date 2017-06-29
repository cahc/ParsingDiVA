package org.cc.wos.classifier;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import java.io.Serializable;

/**
 * Created by crco0001 on 9/9/2016.
 */
public class TokenSequenceStemmer extends Pipe implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int CURRENT_SERIAL_VERSION = 0;
    UEALite ueaLite;

    public TokenSequenceStemmer(UEALite ueaLite) {

        this.ueaLite = ueaLite;


    }

    @Override
    public Instance pipe(Instance carrier) {
        TokenSequence sequence = (TokenSequence)carrier.getData();

        for(int i=0; i<sequence.size(); i++) {

            Token token = sequence.get(i);
            token.setText( ueaLite.stem(  token.getText()  ).word   );
        }


        return carrier; // do nothing
    }



}