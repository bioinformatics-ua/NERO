package pt.ua.ieeta.nero;

import pt.ua.ieeta.nero.sa.IFitnessAssessor;
import pt.ua.ieeta.nero.sa.EvolvingSolution;
import pt.ua.ieeta.nero.sa.SimulatedAnnealing;
import pt.ua.ieeta.nero.crf.CRFFitnessAssessor;
import java.io.*;
import pt.ua.ieeta.nero.feature.Feature;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.ieeta.nero.feature.FeatureCollector;
import pt.ua.ieeta.nero.feature.FeatureOccurrences;
import pt.ua.ieeta.nero.feature.metrics.InfoGainUtil;
import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.util.FileUtil;
import pt.ua.tm.gimli.util.UnclosableBufferedInputStream;

/**
 *
 * @author Paulo
 */
public class Nero {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(Nero.class);

    public Nero() {
    }

    /**
     * Main thread entry point.
     */
    
    private static String processFileName(String fileName){
        return fileName.replaceAll("/", File.separator);
    }
    
    private static final int NUM_FEATURES_TRAINING = 1000;
    private static final int NUM_FEATURES_OPTMISE = 200;
    
    public static void main(String[] args) {
        String trainFile = processFileName("resources/corpus/bc2gm/train/1k.gz");
        String devFile = processFileName("resources/corpus/bc2gm/dev/500.gz");
        String geneFile = processFileName("resources/corpus/bc2gm/dev/500");
        //String testFile = "resources" + File.separator + "corpus" + File.separator + "test_500.gz";
        String unlabeledFile = processFileName("resources/corpus/silver/random_1k.gz");
        String configFile = processFileName("config/bc_semi.config");

        /*try {
            System.setErr(new PrintStream(new File("tmp")));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }*/

        System.out.println("Starting the NERO experiment...");

        // Initialise
        ModelConfig config = new ModelConfig(configFile);
        InputStream trainStream = null;
        InputStream devStream = null;
        //InputStream testStream = null;
        InputStream unlabeledStream = null;
        try {
            trainStream = FileUtil.getFile(new FileInputStream(trainFile));
            devStream = FileUtil.getFile(new FileInputStream(devFile));
            //testStream = FileUtil.getFile(new FileInputStream(testFile));
            unlabeledStream = FileUtil.getFile(new FileInputStream(unlabeledFile));
        } catch (FileNotFoundException ex) {
            logger.error("There was a problem accessing the input data.", ex);
            return;
        }

        // Get InfoGain Features
        logger.info("Performing Information Gain to get the best features...");
        
        UnclosableBufferedInputStream trainUnclosableStream = new UnclosableBufferedInputStream(trainStream);
        InfoGainUtil igu = new InfoGainUtil(config, trainUnclosableStream);
        List<String> features = igu.getFeatures(NUM_FEATURES_TRAINING);

        // Get features to be optimised
        List<String> optimisedFeatures = features.subList(0, NUM_FEATURES_OPTMISE);
        
        // Create base evolving solution
        try {
            trainUnclosableStream.reset();
        } catch (IOException ex) {
            logger.error("It was not possible to reset the stream.", ex);
            return;
        }
        EvolvingSolution seed = createBaseSolution(trainUnclosableStream, config, optimisedFeatures);
        IFitnessAssessor fitnessClass = null;
        try {
            fitnessClass = new CRFFitnessAssessor(config, trainUnclosableStream, devStream, unlabeledStream, features, geneFile); // new BogusFitnessAssessor(); //
            //fitnessClass = new BogusFitnessAssessor();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        /*
         * Instantiate the simulated annealing algorithm.
         */
        SimulatedAnnealing sa = new SimulatedAnnealing(fitnessClass, seed, 100, 0.8, 0.2, 0.3, 0.2);

        try {
            Thread saThread = new Thread(sa);
            saThread.start();
            saThread.join();
        } catch (InterruptedException ex) {
            System.out.println("An exception occured while starting or running the simulated annealing thread: " + ex.getMessage());
            return;
        }

        System.out.println("Ended the NERO experiment.");
    }

    /**
     * *******************************************************************
     */
    /*
     * Get or Create a base solution to the experiment.
     */
    public static EvolvingSolution createBaseSolution(InputStream input, ModelConfig config, List<String> features) {
        //String trainFile = "resources" + File.separator + "corpus" + File.separator + "train_5k.gz";
        /*String trainFile = "../gimli/resources/corpus/gold/bc2gm/train/corpus.gz";

        ModelConfig mc = new ModelConfig("config/bc_semi.config");
        FeatureCollector fc = new FeatureCollector(mc);
        try {
            fc.load(FileUtil.getFile(new FileInputStream(trainFile)));
            //fc.load(FileUtil.getFile(new FileInputStream("../gimli/resources/corpus/gold/bc2gm/train/corpus.gz")));
        } catch (IOException ex) {
            throw new RuntimeException("There was a problem loading the corpus.", ex);
        }

        List<Feature> features = new ArrayList<Feature>();

        if (mc.isCapitalization()) {
            features.add(fc.get("InitCap"));
            features.add(fc.get("EndCap"));
            features.add(fc.get("AllCaps"));
            features.add(fc.get("Lowercase"));
            features.add(fc.get("MixCase"));
        }

        if (mc.isCounting()) {
            features.add(fc.get("SingleCap"));
            features.add(fc.get("TwoCap"));
            features.add(fc.get("ThreeCap"));
            features.add(fc.get("MoreCap"));
            features.add(fc.get("SingleDigit"));
            features.add(fc.get("TwoDigit"));
            features.add(fc.get("ThreeDigit"));
            features.add(fc.get("MoreDigit"));
            features.add(fc.get("LENGTH=1"));
            features.add(fc.get("LENGTH=2"));
            features.add(fc.get("LENGTH=3-5"));
            features.add(fc.get("LENGTH=6+"));
        }

        if (mc.isToken()) {
            features.addAll(fc.getTop("WORD", 25));
        }

        if (mc.isNgrams()) {
            features.addAll(fc.getTop("CHARNGRAM", 25));
        }

        if (mc.isPrefix()) {
            features.addAll(fc.getTop("3PREFIX", 25));
        }

        if (mc.isSuffix()) {
            features.addAll(fc.getTop("3SUFFIX", 25));
        }

        if (mc.isMorphology()) {
            features.addAll(fc.getTop("WordShapeII", 10));
            features.addAll(fc.getTop("WordShapeIII", 25));
        }

        for (Feature f : features) {
            logger.info("{}", f.toString());
        }

        return new EvolvingSolution(features);*/
        
        FeatureCollector fc = new FeatureCollector(config);
        fc.load(input);
        List<FeatureOccurrences> lfo = fc.getFeatures(features);
        
        List<Feature> lf = new ArrayList<Feature>();
        lf.addAll(lfo);
        
        return new EvolvingSolution(lf);
    }

    /**
     * A ficticious class to implement the fitness assessor. The fitness method * returns a sum of all features.
     */
    private static class BogusFitnessAssessor implements IFitnessAssessor {

        @Override
        public double getFitness(EvolvingSolution solution) {
            double sum = 0;
            for (Feature f : solution.getFeatureList()) {
//                double old = sum;
                sum += (f.getB() + f.getI() - f.getO());
//                System.out.println("sum = " + (sum-old));
            }
            return sum;
        }
    }
}
