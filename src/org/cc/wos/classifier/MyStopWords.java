package org.cc.wos.classifier;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import java.io.*;
import java.util.HashSet;

/**
 * Created by crco0001 on 9/9/2016.
 */
public class MyStopWords extends Pipe implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int CURRENT_SERIAL_VERSION = 0;

    HashSet<String> stoplist = new HashSet<String>();
    String[] customStopWords;
    File f;
    String encoding;
    static final String[] EnglishStopwords = new String[]{"a", "able", "about", "above", "according", "accordingly", "across", "actually", "after", "afterwards", "again", "against", "all", "allow", "allows", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "appear", "appreciate", "appropriate", "are", "around", "as", "aside", "ask", "asking", "associated", "at", "available", "away", "awfully", "b", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between", "beyond", "both", "brief", "but", "by", "c", "came", "can", "cannot", "cant", "cause", "causes", "certain", "certainly", "changes", "clearly", "co", "com", "come", "comes", "concerning", "consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could", "course", "currently", "d", "definitely", "described", "despite", "did", "different", "do", "does", "doing", "done", "down", "downwards", "during", "e", "each", "edu", "eg", "eight", "either", "else", "elsewhere", "enough", "entirely", "especially", "et", "etc", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "f", "far", "few", "fifth", "first", "five", "followed", "following", "follows", "for", "former", "formerly", "forth", "four", "from", "further", "furthermore", "g", "get", "gets", "getting", "given", "gives", "go", "goes", "going", "gone", "got", "gotten", "greetings", "h", "had", "happens", "hardly", "has", "have", "having", "he", "hello", "help", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "hi", "him", "himself", "his", "hither", "hopefully", "how", "howbeit", "however", "i", "ie", "if", "ignored", "immediate", "in", "inasmuch", "inc", "indeed", "indicate", "indicated", "indicates", "inner", "insofar", "instead", "into", "inward", "is", "it", "its", "itself", "j", "just", "k", "keep", "keeps", "kept", "know", "knows", "known", "l", "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "like", "liked", "likely", "little", "look", "looking", "looks", "ltd", "m", "mainly", "many", "may", "maybe", "me", "mean", "meanwhile", "merely", "might", "more", "moreover", "most", "mostly", "much", "must", "my", "myself", "n", "name", "namely", "nd", "near", "nearly", "necessary", "need", "needs", "neither", "never", "nevertheless", "new", "next", "nine", "no", "nobody", "non", "none", "noone", "nor", "normally", "not", "nothing", "novel", "now", "nowhere", "o", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "on", "once", "one", "ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over", "overall", "own", "p", "particular", "particularly", "per", "perhaps", "placed", "please", "plus", "possible", "presumably", "probably", "provides", "q", "que", "quite", "qv", "r", "rather", "rd", "re", "really", "reasonably", "regarding", "regardless", "regards", "relatively", "respectively", "right", "s", "said", "same", "saw", "say", "saying", "says", "second", "secondly", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious", "seriously", "seven", "several", "shall", "she", "should", "since", "six", "so", "some", "somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specified", "specify", "specifying", "still", "sub", "such", "sup", "sure", "t", "take", "taken", "tell", "tends", "th", "than", "thank", "thanks", "thanx", "that", "thats", "the", "their", "theirs", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "theres", "thereupon", "these", "they", "think", "third", "this", "thorough", "thoroughly", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "took", "toward", "towards", "tried", "tries", "truly", "try", "trying", "twice", "two", "u", "un", "under", "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon", "us", "use", "used", "useful", "uses", "using", "usually", "uucp", "v", "value", "various", "very", "via", "viz", "vs", "w", "want", "wants", "was", "way", "we", "welcome", "well", "went", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "willing", "wish", "with", "within", "without", "wonder", "would", "would", "x", "y", "yes", "yet", "you", "your", "yours", "yourself", "yourselves", "z", "zero","report","paper","article"};
    boolean addStandardEnglish;


    public MyStopWords(File input, boolean addStandardEnglish, String encoding) {

        f = input;
        this.encoding = encoding;
        this.addStandardEnglish = addStandardEnglish;

        try {
            BufferedReader e = null;
            if(encoding == null) {
                e = new BufferedReader(new FileReader(f));
            } else {
                e = new BufferedReader(new InputStreamReader(new FileInputStream(f), this.encoding));
            }

            String line;
            while((line = e.readLine()) != null) {

                stoplist.add(line);

            }
        } catch (IOException var8) {
            throw new IllegalArgumentException("Trouble reading file " + f);
        }

        if(addStandardEnglish) {

            for(int i=0; i<EnglishStopwords.length; i++) {

                stoplist.add(EnglishStopwords[i]);
            }


        }



        System.out.println(stoplist.size()  + " unique words will be ignored based on stoplist");

    }


    public Instance pipe(Instance carrier) {
        TokenSequence oldSeq = (TokenSequence)carrier.getData();
        TokenSequence newSeq = new TokenSequence();

        for(int i = 0; i < oldSeq.size(); ++i) {
            Token t = (Token)oldSeq.get(i);
            if(!this.stoplist.contains( t.getText() ) ) {
                newSeq.add(t);
            }

        }

        carrier.setData(newSeq);
        return carrier;
    }



}