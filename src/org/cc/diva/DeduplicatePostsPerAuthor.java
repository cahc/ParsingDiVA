package org.cc.diva;

import org.cc.NorskaModellen.DefaultPubInclusion;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

/**
 * Created by crco0001 on 11/10/2016.
 */
public class DeduplicatePostsPerAuthor {


    private MessageDigest digest;
    private HashSet<ByteBuffer> set;


    public DeduplicatePostsPerAuthor() {

        set = new HashSet<ByteBuffer>();

        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    public byte[] digester(String s) throws UnsupportedEncodingException {

        this.digest.reset();
        this.digest.update(s.getBytes("UTF-8"));
        byte[] encoded = digest.digest();

        return encoded;


    }

    public boolean isInSet(Author a) throws UnsupportedEncodingException {

        Post p = a.getEnclosingPost();

        byte[] cas = a.getCas().getBytes("UTF-8");
        byte[] title = DivaHelpFunctions.simplifyString(p.getTitle()).getBytes("UTF-8");
        byte[] year  = BigInteger.valueOf(p.getYear()).toByteArray();
        byte[] norsk_id =  BigInteger.valueOf(p.getNorskNivå().getNorsk_id()).toByteArray();
        byte[] diva_type = p.getDivaPublicationType().getBytes("UTF-8");

        this.digest.reset();
        this.digest.update(cas);
        this.digest.update(title);
        this.digest.update(year);
        this.digest.update(diva_type);
        this.digest.update(norsk_id);

        ByteBuffer encoded = ByteBuffer.wrap(digest.digest()).asReadOnlyBuffer();

        return set.add(encoded);


    }





}
