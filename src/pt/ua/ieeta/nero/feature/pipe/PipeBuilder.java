/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.feature.pipe;

import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.tsf.*;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.config.Resources;
import pt.ua.tm.gimli.features.*;

/**
 *
 * @author david
 */
public class PipeBuilder {

    private ModelConfig config;
    private List<Pipe> pipe;

    public PipeBuilder(ModelConfig config) {
        this.config = config;
        this.pipe = new ArrayList<Pipe>();
    }

    public void initialise() {
        try {
            pipe.add(new Input2TokenSequence(config));

            if (config.isPrge()) {
                pipe.add(new TrieLexiconMembership("PRGE", new InputStreamReader(Resources.getResource("prge")), true));
            }

            if (config.isVerbs()) {
                pipe.add(new TrieLexiconMembership("VERB", new InputStreamReader(Resources.getResource("verbs")), true));
            }

            if (config.isConcepts()) {
                pipe.add(new TrieLexiconMembership("CONCEPT", new InputStreamReader(Resources.getResource("aminoacid")), true));
                pipe.add(new TrieLexiconMembership("CONCEPT", new InputStreamReader(Resources.getResource("nucleicacid")), true));
                pipe.add(new TrieLexiconMembership("CONCEPT", new InputStreamReader(Resources.getResource("nucleobase")), true));
                pipe.add(new TrieLexiconMembership("CONCEPT", new InputStreamReader(Resources.getResource("nucleoside")), true));
                pipe.add(new TrieLexiconMembership("CONCEPT", new InputStreamReader(Resources.getResource("nucleotide")), true));
            }

            if (config.isCapitalization()) {
                pipe.add(new RegexMatches("InitCap", Pattern.compile(Constants.CAPS + ".*")));
                pipe.add(new RegexMatches("EndCap", Pattern.compile(".*" + Constants.CAPS)));
                pipe.add(new RegexMatches("AllCaps", Pattern.compile(Constants.CAPS + "+")));
                pipe.add(new RegexMatches("Lowercase", Pattern.compile(Constants.LOW + "+")));
                pipe.add(new MixCase());
                //pipe.add(new RegexMatches("DigitsLettersAndSymbol", Pattern.compile("[0-9a-zA-z]+[-%/\\[\\]:;()'\"*=+][0-9a-zA-z]+")));
            }

            if (config.isCounting()) {
                pipe.add(new NumberOfCap());
                pipe.add(new NumberOfDigit());
                pipe.add(new WordLength());
            }

            if (config.isSymbols()) {
                pipe.add(new RegexMatches("Hyphen", Pattern.compile(".*[-].*")));
                pipe.add(new RegexMatches("BackSlash", Pattern.compile(".*[/].*")));
                pipe.add(new RegexMatches("OpenSquare", Pattern.compile(".*[\\[].*")));
                pipe.add(new RegexMatches("CloseSquare", Pattern.compile(".*[\\]].*")));
                pipe.add(new RegexMatches("Colon", Pattern.compile(".*[:].*")));
                pipe.add(new RegexMatches("SemiColon", Pattern.compile(".*[;].*")));
                pipe.add(new RegexMatches("Percent", Pattern.compile(".*[%].*")));
                pipe.add(new RegexMatches("OpenParen", Pattern.compile(".*[(].*")));
                pipe.add(new RegexMatches("CloseParen", Pattern.compile(".*[)].*")));
                pipe.add(new RegexMatches("Comma", Pattern.compile(".*[,].*")));
                pipe.add(new RegexMatches("Dot", Pattern.compile(".*[\\.].*")));
                pipe.add(new RegexMatches("Apostrophe", Pattern.compile(".*['].*")));
                pipe.add(new RegexMatches("QuotationMark", Pattern.compile(".*[\"].*")));
                pipe.add(new RegexMatches("Star", Pattern.compile(".*[*].*")));
                pipe.add(new RegexMatches("Equal", Pattern.compile(".*[=].*")));
                pipe.add(new RegexMatches("Plus", Pattern.compile(".*[+].*")));
            }

            if (config.isNgrams()) {
                //pipe.add(new TokenTextCharNGrams("CHARNGRAM=", new int[]{2, 3, 4}));
                pipe.add(new TokenTextCharNGrams("CHARNGRAM=", new int[]{2, 3}));
            }

            if (config.isSuffix()) {
                //pipe.add(new TokenTextCharSuffix("2SUFFIX=", 2));
                pipe.add(new TokenTextCharSuffix("3SUFFIX=", 3));
                //pipe.add(new TokenTextCharSuffix("4SUFFIX=", 4));
            }

            if (config.isPrefix()) {
                //pipe.add(new TokenTextCharPrefix("2PREFIX=", 2));
                pipe.add(new TokenTextCharPrefix("3PREFIX=", 3));
                //pipe.add(new TokenTextCharPrefix("4PREFIX=", 4));
            }

            if (config.isMorphology()) {
                pipe.add(new WordShape());
            }

            if (config.isGreek()) {
                pipe.add(new RegexMatches("GREEK", Pattern.compile(Constants.GREEK, Pattern.CASE_INSENSITIVE)));
            }

            if (config.isRoman()) {
                pipe.add(new RegexMatches("ROMAN", Pattern.compile("((?=[MDCLXVI])((M{0,3})((C[DM])|(D?C{0,3}))?((X[LC])|(L?XX{0,2})|L)?((I[VX])|(V?(II{0,2}))|V)?))")));
            }

            if (config.isConjunctions()) {
                pipe.add(new OffsetConjunctions(true, Pattern.compile("LEMMA=.*"), new int[][]{{-1, 0}, {-2, -1}, {0, 1}, {-1, 1}, {-3, -1}}));
                pipe.add(new OffsetConjunctions(true, Pattern.compile("POS=.*"), new int[][]{{-1, 0}, {-2, -1}, {0, 1}, {-1, 1}, {-3, -1}}));
            }

            if (config.isWindow()) {
                //pipe.add(new FeaturesInWindow("WINDOW_LEMMA=", -3, 3, Pattern.compile("LEMMA=.*"), true));
                //pipe.add(new FeaturesInWindow("WINDOW_WORD=", -3, 3, Pattern.compile("WORD=.*"), true));
                //pipe.add(new FeaturesInWindow("WINDOW_LEXICON=", -3, 3, Pattern.compile("LEXICON=.*"), true));
                //pipe.add(new FeaturesInWindow("WINDOW_SPECIAL=", -3, 3, Pattern.compile("SPECIAL=.*"), true));
                //pipe.add(new FeaturesInWindow("WINDOW_FEATURES=", -1, 1));
                pipe.add(new FeaturesAtWindow(-3, 3));
            }

        } catch (Exception ex) {
            throw new RuntimeException("There was a problem initializing the features.", ex);
        }
    }

    public void addFeatureSelector(List<String> features) {
        pipe.add(new FeatureSelector(features));
    }

    public void finalise(boolean convertLabel) {
        if (convertLabel) {
            pipe.add(new Target2Label());
        }
        pipe.add(new TokenSequence2FeatureVectorSequence(true, true));
    }

    public Pipe getPipe() {
        return new SerialPipes(pipe);
    }
}
