package org.cc.isbn;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


 public class IsbnPrefix {

        private static se.kb.libris.utils.isbn.IsbnParser isbnParser = new se.kb.libris.utils.isbn.IsbnParser();

        //simplistic, just for getting potential ISBN from noisy strings
        static final Pattern regex = Pattern.compile("[0-9-]{10,17}X?", Pattern.CASE_INSENSITIVE);

        public List<String> getPotentialIsbnFromNoisyDiva(String s) {

            ArrayList<String> isbnList = new ArrayList<>();
            Matcher matcher = regex.matcher(s);

            while( matcher.find() ) {

                isbnList.add( matcher.group(0) );

            }

            return isbnList;

        }

        public IsbnPrefix() {}

        public List<String> getPrefixes(String s)  {


            //we are returning a empty arraylist insted of Collections.emptylist() for reason that has do do with rJava

            List<String> potentialIsbn = getPotentialIsbnFromNoisyDiva(s);
            if(potentialIsbn.size() == 0) return new ArrayList<>(0);
            TreeSet<String> prefixes = new TreeSet<>();


            for(String i : potentialIsbn) {

                se.kb.libris.utils.isbn.Isbn parsedIsbn = null;

                try {
                    parsedIsbn = this.isbnParser.parse(i).convert(se.kb.libris.utils.isbn.Isbn.ISBN13);

                } catch (se.kb.libris.utils.isbn.ConvertException | se.kb.libris.utils.isbn.IsbnException | NullPointerException e) {

                    continue;
                }

                if(parsedIsbn != null) {

                    prefixes.add( parsedIsbn.getCountry().substring(0,3) + '-' + parsedIsbn.getCountry().substring(3) + '-' + parsedIsbn.getPublisher() );
                }


            }

            List<String> prefixes2 =  new ArrayList<String>(prefixes);

            return prefixes2.size() != 0 ? prefixes2 : new ArrayList<String>(0);
        }

        public String getFull(String s) throws se.kb.libris.utils.isbn.IsbnException, se.kb.libris.utils.isbn.ConvertException {

            se.kb.libris.utils.isbn.Isbn parsedIsbn = this.isbnParser.parse(s).convert(se.kb.libris.utils.isbn.Isbn.ISBN13);
            return parsedIsbn.toString(true);

        }

        public static void main(String[] arg) throws se.kb.libris.utils.isbn.IsbnException, se.kb.libris.utils.isbn.ConvertException {

            IsbnPrefix isbnPrefix = new IsbnPrefix();

            //isbn in diva (csv) can look in different ways:
            //9781788742894;9781788742900;9781788742917;9781788742924
            //978-91-44-13898-5

            //978-93-5547-458-2
            List<String> publisherPrefix = isbnPrefix.getPrefixes("1788742894;9781788742900 ;9781788742917;9781788742924 978-93-5547-458-2,11111111111;;9355474582.\\t978-9527076-50-7;9798886978155\"");
            System.out.println(publisherPrefix); //978-93-5547

            String publisherPrefix2 = isbnPrefix.getFull("9798886978155");
            System.out.println(publisherPrefix2); //978-1-78874-292-4
        }

}
