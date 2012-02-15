/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.feature;

import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.tsf.RegexMatches;
import cc.mallet.pipe.tsf.TokenTextCharNGrams;
import cc.mallet.pipe.tsf.TokenTextCharPrefix;
import cc.mallet.pipe.tsf.TokenTextCharSuffix;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.TokenSequence;
import cc.mallet.util.PropertyList;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.ieeta.nero.feature.pipe.PipeBuilder;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.dictionary.Dictionary;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.features.*;
import pt.ua.tm.gimli.util.FileUtil;

/**
 *
 * @author david
 */
public class FeatureCollector {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(FeatureCollector.class);
    private InstanceList instances;
    private ModelConfig config;
    private Pattern stopwords;

    public FeatureCollector(ModelConfig config) {
        assert (config != null);
        this.config = config;
    }

    public void load(InputStream input) {
        assert (input != null);
        try {
            stopwords = Dictionary.loadStopwords(new FileInputStream("resources/lexicon/umlsstop.txt"));

            Corpus c = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein, input);
            
            PipeBuilder pb = new PipeBuilder(config);
            pb.initialise();
            Pipe p = pb.getPipe();
            
            instances = c.toModelFormatTrain(p);
        } catch (FileNotFoundException ex) {
            logger.error("There was a problem loading the stopwords.");
        } catch (GimliException ex) {
            logger.error("There was a problem loading the input data.", ex);
        }
    }

    public List<FeatureOccurrences> getFeatures(List<String> features) {
        List<FeatureOccurrences> foList = new ArrayList<FeatureOccurrences>();

        for (Instance i : instances) {
            TokenSequence data = (TokenSequence) i.getData();
            LabelSequence target = (LabelSequence) i.getTarget();


            for (int j = 0; j < data.size(); j++) {
                cc.mallet.types.Token t = data.get(j);
                String label = target.get(j).toString();

                PropertyList pl = t.getFeatures();
                PropertyList.Iterator it = pl.numericIterator();
                while (it.hasNext()) {
                    it.nextProperty();
                    String key = it.getKey();

                    if (features.contains(key)) {
                        // Get FO
                        FeatureOccurrences fo = new FeatureOccurrences(key);
                        int index = foList.indexOf(fo);
                        if (index != -1) {
                            fo = foList.get(index);
                        }
                        
                        // Get occurrences
                        if (label.equals("B")) {
                            fo.incrementOccurrencesB();
                        } else if (label.equals("I")) {
                            fo.incrementOccurrencesI();
                        } else {
                            fo.incrementOccurrencesO();
                        }
                        
                        // Set FO
                        if (index != -1) {
                            foList.set(index, fo);
                        } else {
                            foList.add(fo);
                        }
                    }
                }
            }
        }

        for (FeatureOccurrences fo : foList) {
            fo.setProbabilities();
        }

        return foList;
    }

    public FeatureOccurrences get(String feature) {
        FeatureOccurrences fo = new FeatureOccurrences(feature);

        for (Instance i : instances) {
            TokenSequence data = (TokenSequence) i.getData();
            LabelSequence target = (LabelSequence) i.getTarget();


            for (int j = 0; j < data.size(); j++) {
                cc.mallet.types.Token t = data.get(j);
                String label = target.get(j).toString();

                PropertyList pl = t.getFeatures();
                PropertyList.Iterator it = pl.numericIterator();
                while (it.hasNext()) {
                    it.nextProperty();
                    String key = it.getKey();
                    if (key.equals(feature)) {
                        if (label.equals("B")) {
                            fo.incrementOccurrencesB();
                        } else if (label.equals("I")) {
                            fo.incrementOccurrencesI();
                        } else {
                            fo.incrementOccurrencesO();
                        }
                    }
                }
            }
        }

        fo.setProbabilities();
        return fo;
    }

    public List<FeatureOccurrences> getTop(String prefix, int n) {
        assert (prefix != null);
        assert (n > 0);

        //Pattern p = Pattern.compile(prefix + "=.*");
        prefix = prefix + "=";
        List<FeatureOccurrences> features = new ArrayList<FeatureOccurrences>();

        for (Instance i : instances) {
            TokenSequence data = (TokenSequence) i.getData();
            LabelSequence target = (LabelSequence) i.getTarget();


            for (int j = 0; j < data.size(); j++) {
                cc.mallet.types.Token t = data.get(j);
                String label = target.get(j).toString();

                PropertyList pl = t.getFeatures();
                PropertyList.Iterator it = pl.numericIterator();
                while (it.hasNext()) {
                    it.nextProperty();
                    String key = it.getKey();
                    //Matcher m = p.matcher(key);
                    if (key.contains(prefix)) {

                        String feature = key.substring(key.lastIndexOf("=") + 1);
                        if (feature.length() < 3) {
                            continue;
                        }

                        Matcher m = stopwords.matcher(feature);
                        if (m.matches()) {
                            continue;
                        }

                        FeatureOccurrences fo = new FeatureOccurrences(key);

                        int index = features.indexOf(fo);
                        if (index != -1) {
                            fo = features.get(index);
                        }

                        if (label.equals("B")) {
                            fo.incrementOccurrencesB();
                        } else if (label.equals("I")) {
                            fo.incrementOccurrencesI();
                        } else {
                            fo.incrementOccurrencesO();
                        }

                        if (index != -1) {
                            features.set(index, fo);
                        } else {
                            features.add(fo);
                        }
                    }
                }
            }
        }

        int totalOccurrences = 0;
        for (FeatureOccurrences fo : features) {
            fo.setProbabilities();
            totalOccurrences += fo.getOccurrencesB() + fo.getOccurrencesI() + fo.getOccurrencesO();
        }

        //Collections.sort(features, new FeatureOccurrencesWeightedComparator(totalOccurrences));
        Collections.sort(features, new FeatureOccurrencesComparator());

        if (features.size() >= n) {
            features = features.subList(0, n);
        }

        return features;
    }

    public static void main(String[] args) {
        try {
            FeatureCollector fc = new FeatureCollector(new ModelConfig("config/bc_semi.config"));
            fc.load(FileUtil.getFile(new FileInputStream("../gimli/resources/corpus/gold/bc2gm/train/corpus.gz")));

            FeatureOccurrences fo = fc.get("InitCap");
            logger.info("{}", fo.toString());

            fo = fc.get("EndCap");
            logger.info("{}", fo.toString());

            fo = fc.get("AllCaps");
            logger.info("{}", fo.toString());

            fo = fc.get("Lowercase");
            logger.info("{}", fo.toString());

            fo = fc.get("MixCase");
            logger.info("{}", fo.toString());

            fo = fc.get("SingleCap");
            logger.info("{}", fo.toString());

            fo = fc.get("TwoCap");
            logger.info("{}", fo.toString());

            fo = fc.get("ThreeCap");
            logger.info("{}", fo.toString());

            fo = fc.get("MoreCap");
            logger.info("{}", fo.toString());

            fo = fc.get("SingleDigit");
            logger.info("{}", fo.toString());

            fo = fc.get("TwoDigit");
            logger.info("{}", fo.toString());

            fo = fc.get("ThreeDigit");
            logger.info("{}", fo.toString());

            fo = fc.get("MoreDigit");
            logger.info("{}", fo.toString());

            fo = fc.get("LENGTH=1");
            logger.info("{}", fo.toString());

            fo = fc.get("LENGTH=2");
            logger.info("{}", fo.toString());

            fo = fc.get("LENGTH=3-5");
            logger.info("{}", fo.toString());

            fo = fc.get("LENGTH=6+");
            logger.info("{}", fo.toString());


            List<FeatureOccurrences> features = fc.getTop("WORD", 50);
            print(features);
            logger.info("\n\n");

            features = fc.getTop("3SUFFIX", 50);
            print(features);
            logger.info("\n\n");

            features = fc.getTop("3PREFIX", 50);
            print(features);
            logger.info("\n\n");

            /*
             * features = fc.getTop("WordShapeI", 50); print(features); logger.info("\n\n");
             */

            features = fc.getTop("WordShapeII", 10);
            print(features);
            logger.info("\n\n");

            features = fc.getTop("WordShapeIII", 50);
            print(features);
            logger.info("\n\n");

        } catch (FileNotFoundException ex) {
            logger.error("There was a problem loading the corpus.", ex);
        }
    }

    public static void print(List<FeatureOccurrences> features) {
        for (int i = 0; i < features.size(); i++) {
            FeatureOccurrences fo = features.get(i);
            logger.info("{} - {}", (i + 1), fo.toString());
        }
    }
}
