package org.cc.taxonomy;

import cc.FilePathConstants;
import cc.database.BerkeleyStorage;
import cc.database.ScopusRecord;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;

import java.util.Iterator;

public class Playground {


    public static void main(String[] args) {










        /*


        Local DB with UMU records from Scopus

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
