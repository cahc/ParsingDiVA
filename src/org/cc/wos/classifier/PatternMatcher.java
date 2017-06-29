package org.cc.wos.classifier;

/**
 * Created by crco0001 on 9/9/2016.
 */
import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Created by crco0001 on 9/7/2016.
 */
public class PatternMatcher implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int CURRENT_SERIAL_VERSION = 0;

    //Replacing with a sub-part of the matched portion
    static PatternMatcher[] CLEAN_PATTERN_DOCUMENT = new PatternMatcher[] { new PatternMatcher("^(.*?)((?i)copyright(?-i).{0,3})?(\\xA9.{0,5}[0-9][0-9][0-9][0-9])(.{0,400})$", "$1"), new PatternMatcher("^(.*?)((?i)copyright(?-i).{0,3})?(\\xA9)([^a-z].{0,3}[A-Z].{0,300})$", "$1"), new PatternMatcher("^(.*?)((?i)copyright(?-i).{0,3})?(\\xA9)(.{0,200})$", "$1"), new PatternMatcher("^(.*?)((?i)copyright(?-i).{0,3}[(].{0,1}[Cc].{0,1}[)])(.{0,300})$", "$1"), new PatternMatcher("^(.*?)((?i)copyright(?-i).{0,3})?([(][Cc][)])([^=]{0,6})((?i)[0-9][0-9][0-9][0-9]|crown|author|elsevier|wiley(?-i))(.{0,200})$", "$1"), new PatternMatcher("^(.*?)((?i)copyright(?-i).{0,3})?([(][Cc][)])(.{0,200})((?i)\\b[12][09][0-9][0-9]\\b|\\breserved\\b|\\bltd\\b|\\binc\\b|paris|basel(?-i))(.{0,3})$", "$1"), new PatternMatcher("^(.*?)((?i)copyright(?-i).{0,3})?([(][Cc][)])((?i).{0,7}author|.{0,7}published|.{0,200}open[ ]access|.{0,200}creative[ ]commons(?-i))(.{0,500})$", "$1"), new PatternMatcher("^(.*?)((?i)copyright(?-i))(.{0,100})((?i)[(]c[)]|[12][09][0-9][0-9]|\\breserved\\b|\\bdistributed\\b|\\bltd\\b|\\binc\\b|\\bpublished\\b|\\bpress\\b|american|institute|society|association|administration|elsevier|wiley(?-i))(.{0,200})$", "$1"), new PatternMatcher("^(.*?)((?i)copy;[ ]|[(].{0,1}c.{0,1}[)].{0,2}|\\bthe\\b[ ]\\bauthor.{0,5}|\\bpublished\\b[ ]\\bby\\b.{0,100}(?-i))?([12][09][0-9][0-9])(.{0,200})((?i)\\breserved\\b|\\bltd\\b|\\binc\\b(?-i))(.{0,3})$", "$1"), new PatternMatcher("^(.*?)([.][ ]+\\b[Pp]ublished\\b[ ]\\bby\\b.{0,300}|[ ]+\\bPublished\\b[ ]\\bby\\b[ ]+[A-Z].{0,300}|[ ]+\\bpublished\\b[ ]\\bby\\b[ ]+[A-Z].{0,100})$", "$1"), new PatternMatcher("^(.*?)([.][ ]+[(][C][)].{0,100}|[ ]+[(][C][)][ ]+[A-Z].{0,100}|[ ]+[(][c][)][ ]+[A-Z].{0,50})$", "$1"), new PatternMatcher("^(.*?)([(].{0,1}[Cc].{0,1}[)])((?i).{0,3}[0-9][0-9][0-9][0-9]|.{0,80}society|.{0,80}association|.{0,80}elsevier|.{0,80}wiley|.{0,80}wilkins|.{0,80}blackwell|.{0,80}ksbb|.{0,80}rsna(?-i))(.{0,80})$", "$1"), new PatternMatcher("^(.*?)(Copyright.{0,100})$", "$1") };

    private Pattern pattern;
    private String replacement;

    public PatternMatcher() {}

    public PatternMatcher(final String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public PatternMatcher(final String pattern, final String replacement) {
        this.pattern = Pattern.compile(pattern);
        this.replacement = replacement;
    }

    public boolean isMatch(final String text) {
        return this.pattern.matcher(text).find();
    }

    public String replace(final String text) {
        return (this.replacement != null) ? this.pattern.matcher(text).replaceAll(this.replacement) : null;
    }

    public String[] split(final String text) {
        return this.pattern.split(text);
    }

    public String cleanStringFromCopyrightStatements(String input) {

        int i=0;
        boolean cleaned = false;

        for(cleaned = false; i <CLEAN_PATTERN_DOCUMENT.length && !cleaned; i++) {

            if(CLEAN_PATTERN_DOCUMENT[i].isMatch(input)) {

                input = CLEAN_PATTERN_DOCUMENT[i].replace(input);
                cleaned = true;

            }


        }

        return input; // output is cleaned if copyright statemens where found, otherwise identical to input

    }



}