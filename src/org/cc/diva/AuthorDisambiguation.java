package org.cc.diva;

import com.googlecode.cqengine.query.simple.In;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import org.cc.PersonalData.DataLoader;
import org.cc.PersonalData.Person;
import org.cc.misc.DivaIDtoNames;
import org.cc.misc.ReadAffiliationMappingFile;
import org.cc.misc.SimilarAuthorSet;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.*;

/**
 * Created by crco0001 on 8/7/2017.
 */
public class AuthorDisambiguation {


    Map<Integer,DivaIDtoNames> mappings; //DivaID to AffiliationObject

    List<Person> allPersonObjects; // PersonObject from delta.ub.umu.se


    public AuthorDisambiguation(File affiliationMappingFileExcel, File personalDataXML) throws IOException, ParseException, XMLStreamException {


        ReadAffiliationMappingFile readAffiliationMappingFile = new ReadAffiliationMappingFile();
        this.mappings = readAffiliationMappingFile.parseAffiliationMappingFile(affiliationMappingFileExcel );

        DataLoader personalData = new DataLoader( personalDataXML );

        this.allPersonObjects = personalData.getPersonData();


    }


    public AuthorDisambiguation() throws ParseException, XMLStreamException, IOException {



        ReadAffiliationMappingFile readAffiliationMappingFile = new ReadAffiliationMappingFile();
        this.mappings = readAffiliationMappingFile.parseAffiliationMappingFile(new java.io.File("E:\\STARKA_MILJÖER_UTVÄRDERING\\V3_KOMPLETTERINGSRAPPORT\\Mappningsfil20170807.xlsx")  );

        DataLoader personalData = new DataLoader( new File("E:\\STARKA_MILJÖER_UTVÄRDERING\\V3_KOMPLETTERINGSRAPPORT\\PersonalData_201708071322409.xml"));

        this.allPersonObjects = personalData.getPersonData();

    }




    public String getFaculty(String institution) {

        for(Map.Entry<Integer,DivaIDtoNames> entry : this.mappings.entrySet()) {

            DivaIDtoNames divaIDtoNames = entry.getValue();

            if(institution.equals( divaIDtoNames.getINSTITUTION()  ) ) return  divaIDtoNames.getFAKULTET();


        }


        return null;
    }


    public boolean validInstitution(String institution) {

        for (Map.Entry<Integer, DivaIDtoNames> entry : this.mappings.entrySet()) {

            DivaIDtoNames divaIDtoNames = entry.getValue();

            if (institution.equals(divaIDtoNames.getINSTITUTION())) return true;


        }

        return false;
    }


    public boolean removeNonConsideredUmUaffiliationIfPossible(Author a) {

        if(a.getLowestDivaAddressNumber().size() == 0) return false;
        HashSet<Integer> notConsidered = new HashSet<>();
        List<Integer> lowestID = a.getLowestDivaAddressNumber();
        for(Integer i : lowestID) {

            DivaIDtoNames divaIDtoNames = mappings.get(i);
            if( !divaIDtoNames.isCONSIDER_WHEN_FRACTIONALISING() ) {

                notConsidered.add( i );

            }

        }

        if(notConsidered.size() == 0) return false;

        if(notConsidered.size() == a.getLowestDivaAddressNumber().size() ) {
            System.out.println("Warning only \"non considered\" affiliation(s): "+ a.getLowestDivaAddressNumber() );
            return false;

        }

        if(notConsidered.size() < a.getLowestDivaAddressNumber().size())  {

            System.out.println("I will remove non considered: " + notConsidered);
            return true;
        }

        if(notConsidered.size() > a.getLowestDivaAddressNumber().size() )  {

            new Exception("Non considered affils larger than total affils. Not  possible!");
        }

        return false;
    }


    public void mapAffiliationsAndDisanbigueAuthors(List<Post> postList) {


        //FIRST map umu-affiliations to DivaIDtoNames

        for(Post p : postList ) {

            for(Author a : p.getAuthorList()) {

                a.mappAffiliations(mappings);
            }

        }



        //setp 1, map cas to SimilarAuthorSet & identify UmU-authors without cas

        //System.out.println("Phase 1 in author disambiguation: map unique cas to SimilarAuthorSet (name variations and set of institutions");
        //System.out.println("Also identify UmU-authors that don't have a any CAS (list x)");

        HashMap<String,SimilarAuthorSet> casToAuthorSets = new HashMap<>();
        List<Author> UmuAuthorsWithourCas = new ArrayList<>();

        for(Post p : postList) {

            List<Author> authorList = p.getAuthorList();

            for(Author a : authorList) {

                int umuAddresses = a.getNrUmUaddresses();

                if( umuAddresses == 0) { continue; }

                String cas = a.getCas();

                if("not available".equals(cas)) { UmuAuthorsWithourCas.add(a); continue; }

                SimilarAuthorSet similarAuthorSet = casToAuthorSets.get(cas);

                if(similarAuthorSet == null) {

                    //create new

                    similarAuthorSet = new SimilarAuthorSet();
                    similarAuthorSet.addNormalizedNames( Author.normalizeAndSortNames(  a.getAuthorName() ) );
                    for( DivaIDtoNames divaIDtoNames : a.getAffilMappingsObjects() ) { similarAuthorSet.addInstitutions(  divaIDtoNames.getINSTITUTION()  ); }
                    similarAuthorSet.addAuthor(a);

                    casToAuthorSets.put(cas,similarAuthorSet);
                } else {

                    //update

                    similarAuthorSet.addNormalizedNames( Author.normalizeAndSortNames(  a.getAuthorName() ) );
                    for( DivaIDtoNames divaIDtoNames : a.getAffilMappingsObjects() ) { similarAuthorSet.addInstitutions(  divaIDtoNames.getINSTITUTION()  ); }
                    similarAuthorSet.addAuthor(a);

                }



            } //for-each author

        } //for-each post



        //System.out.println("# umu-author objects without cas: " + UmuAuthorsWithourCas.size());
        //System.out.println("# unique cas to SimilarAuthorSets: " + casToAuthorSets.size());
        //System.out.println("dimu0001 before: " + casToAuthorSets.get("dimu0001").getTotalAddedAuthorObjects());
        //try to merge UmU-authors without cas to a cas - SimilarAuthorSet-mapping
        //System.out.println("Phase 2 in author disambiguation: try to match authors in list x against the CAS-->SimilarAuthorSet based on name and affiliation(s)");

        NormalizedLevenshtein normalizedLevenshtein = new NormalizedLevenshtein();

        Iterator<Author> unmatchedAuthorIterator = UmuAuthorsWithourCas.iterator();

        while(unmatchedAuthorIterator.hasNext()) {

            Author tryToMatch = unmatchedAuthorIterator.next();

            break_out:
            for(Map.Entry<String,SimilarAuthorSet> entry : casToAuthorSets.entrySet()) {

                String cas_key = entry.getKey();
                SimilarAuthorSet similarAuthorSet = entry.getValue();
                Set<String> institutions = similarAuthorSet.getUniqueInstitutions();
                boolean shares_institution = false;

                for(DivaIDtoNames divaIDtoNames : tryToMatch.getAffilMappingsObjects()) {

                    if( institutions.contains( divaIDtoNames.getINSTITUTION() ) ) {shares_institution = true; break;}

                }

                //potentially now check names

                if(shares_institution) {

                    String normalizedAndSortedName = Author.normalizeAndSortNames( tryToMatch.getAuthorName() );

                    Set<String> matchAgainst = similarAuthorSet.getUniqueAndNormalizedNames();

                    for(String s : matchAgainst) {


                        double sim = normalizedLevenshtein.similarity(s,normalizedAndSortedName);

                        if(sim >= 0.95) {

                            //MATCH!

                            // System.out.println(tryToMatch.getAuthorName() +" matched to: " +  matchAgainst + " " + cas_key );
                            unmatchedAuthorIterator.remove();

                            //add

                            similarAuthorSet.addNormalizedNames( normalizedAndSortedName  );
                            for( DivaIDtoNames divaIDtoNames : tryToMatch.getAffilMappingsObjects() ) { similarAuthorSet.addInstitutions(  divaIDtoNames.getINSTITUTION()  ); }

                            //add identified cas

                            tryToMatch.addAutomaticAddedCas(cas_key);

                            similarAuthorSet.addAuthor(tryToMatch);
                            break break_out;

                        }

                    }

                }



            }



        } //author iterator





        //System.out.println("Phase 3 in author disambiguation: unmatched authors from list x are now matched against personal data from delta.ub.umu.se");

        unmatchedAuthorIterator = UmuAuthorsWithourCas.iterator();


        while(unmatchedAuthorIterator.hasNext()) {

            Author tryToMatch = unmatchedAuthorIterator.next();
            String normalizedAndSortedName = Author.normalizeAndSortNames( tryToMatch.getAuthorName() );
            break_out:
            for(Person p : allPersonObjects) {

                boolean shares_faculties = false;

                Set<String> faculties = p.getUniqueFaculties();

                for(DivaIDtoNames s : tryToMatch.getAffilMappingsObjects()) {  if(  faculties.contains( s.getFAKULTET() ) ) { shares_faculties = true; break; } }

                if(shares_faculties) {

                    //now test name similarity

                    double sim = normalizedLevenshtein.similarity(normalizedAndSortedName, p.getNameForSearching());

                    if(sim >= 0.95) {


                        //todo get cas and check cas-->SetOfNames, otherwise create a new one!

                        String cas = p.getUID();

                        SimilarAuthorSet similarAuthorSet = casToAuthorSets.get(cas);

                        if(similarAuthorSet == null) {

                            similarAuthorSet = new SimilarAuthorSet();

                            //add automatic cas

                            tryToMatch.addAutomaticAddedCas( cas );

                            similarAuthorSet.addAuthor( tryToMatch );
                            similarAuthorSet.addNormalizedNames(  Author.normalizeAndSortNames( tryToMatch.getAuthorName() ) );
                            for( DivaIDtoNames divaIDtoNames : tryToMatch.getAffilMappingsObjects() ) { similarAuthorSet.addInstitutions(  divaIDtoNames.getINSTITUTION()  ); }


                            casToAuthorSets.put(cas, similarAuthorSet);

                        } else {

                            //add automatic cas
                            tryToMatch.addAutomaticAddedCas( cas );

                            similarAuthorSet.addAuthor( tryToMatch );
                            similarAuthorSet.addNormalizedNames(  Author.normalizeAndSortNames( tryToMatch.getAuthorName() ) );
                            for( DivaIDtoNames divaIDtoNames : tryToMatch.getAffilMappingsObjects() ) { similarAuthorSet.addInstitutions(  divaIDtoNames.getINSTITUTION()  ); }


                        }


                        unmatchedAuthorIterator.remove();
                        break break_out;

                    }

                }

            }

        }





       // System.out.println("# umu-author objects without cas after reduction 2: " + UmuAuthorsWithourCas.size());

       // System.out.println("Phase 4 in author disambiguation: try to merge authors in list x based on name and faculty");
        // create fake cas like sO: unknown1, unknown2,etc

        Map<String,SimilarAuthorSet> fakeCasToSimilarAuthorSet = new HashMap<>();

        unmatchedAuthorIterator = UmuAuthorsWithourCas.iterator();
        int fakeCasCounter = 1;
        while(unmatchedAuthorIterator.hasNext()) {

            Author tryToMerge = unmatchedAuthorIterator.next();
            String fixedName = Author.normalizeAndSortNames( tryToMerge.getAuthorName() );

            boolean success = false;

            out:
            for(Map.Entry<String,SimilarAuthorSet> entry : fakeCasToSimilarAuthorSet.entrySet()) { //empty at round one!


                String fakeCas = entry.getKey();
                SimilarAuthorSet similarAuthorSet = entry.getValue();

                Set<String> otherInstitutions = new HashSet<>( similarAuthorSet.getUniqueInstitutions() ); // make a copy
                Set<String> thisInstitution = new HashSet<>();
                for(DivaIDtoNames divaIDtoNames : tryToMerge.getAffilMappingsObjects() ) {thisInstitution.add( divaIDtoNames.getINSTITUTION() ); }

                otherInstitutions.retainAll(thisInstitution); //intersection
                if(otherInstitutions.size() <= 0) continue; //don't proceed with levenstien

                for(String name :  similarAuthorSet.getUniqueAndNormalizedNames() ) {
                    double sim = normalizedLevenshtein.similarity(fixedName, name );

                    if(sim >= 0.95) {

                        //HIT! //todo

                        similarAuthorSet.addAuthor( tryToMerge );
                        similarAuthorSet.addNormalizedNames(  fixedName );
                        for(String inst : thisInstitution ) similarAuthorSet.addInstitutions( inst );

                        unmatchedAuthorIterator.remove();
                        success = true;
                        break out;
                    }

                }




            } // for fakeCas

            if(!success) {

                String unknownCas = "unknownCas" + fakeCasCounter;
                fakeCasCounter++;

                SimilarAuthorSet similarAuthorSet = new SimilarAuthorSet();
                similarAuthorSet.addAuthor(tryToMerge);
                similarAuthorSet.addNormalizedNames( fixedName );
                Set<String> thisInstitution = new HashSet<>();
                for(DivaIDtoNames divaIDtoNames : tryToMerge.getAffilMappingsObjects() ) {thisInstitution.add( divaIDtoNames.getINSTITUTION() ); }
                for(String inst : thisInstitution ) similarAuthorSet.addInstitutions( inst );
                unmatchedAuthorIterator.remove();

                fakeCasToSimilarAuthorSet.put(unknownCas,similarAuthorSet);

            }

            //NO HIT CREATE A NEW FAKE CAS-->



        }



       // System.out.println("Final phase, setting disambiguateID to author objects");


        int id = 1;
        for(Map.Entry<String,SimilarAuthorSet> entry : casToAuthorSets.entrySet() ) {


            SimilarAuthorSet similarAuthorSet = entry.getValue();

            for(Author a : similarAuthorSet.getAuthors()) {

                a.setDisambiguateID( "A"+id);

            }

            id++;
        }




        int id2 = 1;
        for(Map.Entry<String,SimilarAuthorSet> entry : fakeCasToSimilarAuthorSet.entrySet() ) {


            SimilarAuthorSet similarAuthorSet = entry.getValue();

            for(Author a : similarAuthorSet.getAuthors()) {

                a.setDisambiguateID("UA"+id2);

            }

            id2++;
        }


    }




    public static void main(String[] arg) throws FileNotFoundException {



    }


}
