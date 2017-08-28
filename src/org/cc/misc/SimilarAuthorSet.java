package org.cc.misc;

import com.koloboke.collect.set.hash.HashByteSet;
import org.cc.diva.Author;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by crco0001 on 8/8/2017.
 */
public class SimilarAuthorSet {

    Set normalizedNames = new HashSet<String>();
    Set institutions = new HashSet<String>();
    List<Author> authorList = new ArrayList<>();


    public Set<String> getUniqueAndNormalizedNames() {

        return normalizedNames;
    }


    public Set<String> getUniqueInstitutions() {

        return institutions;
    }


    public List<Author> getAuthors() {

        return authorList;
    }


    public int getTotalAddedAuthorObjects() {

        return authorList.size();
    }


    public void addNormalizedNames(String name) {
        this.normalizedNames.add(name);
    }

    public void addInstitutions(String inst) {
        this.institutions.add(inst);
    }

    public void addAuthor(Author author) {
        this.authorList.add(author);
    }
}

