package pt.ua.ieeta.nero;

import cc.mallet.fst.CRF;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;
import pt.ua.ieeta.nero.crf.CRFFitnessAssessor;
import pt.ua.ieeta.nero.crf.CRFModel;
import pt.ua.ieeta.nero.external.evaluator.Performance;
import pt.ua.ieeta.nero.feature.FeatureCollector;
import pt.ua.ieeta.nero.feature.FeatureOccurrences;
import pt.ua.ieeta.nero.feature.metrics.InfoGainUtil;
import pt.ua.ieeta.nero.feature.pipe.PipeBuilder;
import pt.ua.ieeta.nero.feaure.targets.*;
import pt.ua.ieeta.nero.sa.EvolvingSolution;
import pt.ua.ieeta.nero.sa.IFitnessAssessor;
import pt.ua.ieeta.nero.sa.SimulatedAnnealing;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.corpus.Corpus;
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
    private static String processFileName(String fileName) {
        return fileName.replace("/", File.separator);
    }
    private static final int NUM_FEATURES_TRAINING = 9574; //46066;
    private static final int NUM_FEATURES_OPTMISE = 50; //1000;

    public static void main(String[] args) {
        String trainFile = processFileName("resources/corpus/bc2gm/train/500.gz"); //corpus.gz
        String devFile = processFileName("resources/corpus/bc2gm/dev/500.gz"); //corpus.gz
        String geneFile = processFileName("resources/corpus/bc2gm/dev/500"); //annotations
        //String testFile = "resources" + File.separator + "corpus" + File.separator + "test_500.gz";
        String unlabeledFile = processFileName("resources/corpus/silver/random_1k.gz");
        String configFile = processFileName("config/bc_semi.config");


        try {
            System.setErr(new PrintStream(new File("tmp")));
            System.setOut(new PrintStream(new File("tmp")));
        } catch (FileNotFoundException ex) {
            return;
        }


        System.out.println("Starting the NERO experiment...");

        // Initialise
        ModelConfig config = new ModelConfig(configFile);
        UnclosableBufferedInputStream trainStream;
        UnclosableBufferedInputStream devStream;
        //InputStream testStream = null;
        UnclosableBufferedInputStream unlabeledStream;
        try {
            trainStream = new UnclosableBufferedInputStream(FileUtil.getFile(new FileInputStream(trainFile)));
            devStream = new UnclosableBufferedInputStream(FileUtil.getFile(new FileInputStream(devFile)));
            //testStream = FileUtil.getFile(new FileInputStream(testFile));
            unlabeledStream = new UnclosableBufferedInputStream(FileUtil.getFile(new FileInputStream(unlabeledFile)));
        } catch (FileNotFoundException ex) {
            logger.error("There was a problem accessing the input data.", ex);
            return;
        }

        // Get InfoGain Features
        logger.info("Performing Information Gain to get the best features...");
        InfoGainUtil igu = new InfoGainUtil(config, trainStream);
        List<String> features = igu.getFeatures();

        // Get supervised CRF
        PipeBuilder pb = new PipeBuilder(config);
        pb.initialise();
        pb.addFeatureSelector(features);
        pb.finalise(false);
        Pipe p = pb.getPipe();

        CRF supervisedCRF = null;
        InstanceList trainIL = null;
        InstanceList devIL = null;
        InstanceList unlabeledIL = null;
        Corpus dev = null;
        try {
            Corpus train = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein, trainStream);
            trainIL = train.toModelFormatTrain(p);

            dev = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein, devStream);
            devIL = dev.toModelFormatTrain(p);


            Performance performance = new Performance();
            supervisedCRF = FSOptimisation.getSupervisedCRF(trainIL, config, dev, geneFile, performance);
            logger.info("SUPERVISED CRF: {}", performance);

            unlabeledIL = CRFModel.loadUnlabeledData(unlabeledStream, p);
        } catch (Exception ex) {
            throw new RuntimeException("There was a problem training the supervised CRF.", ex);
        }

        // Get Feature BIO Probabilities
        FeatureCollector fc = new FeatureCollector(config);
        fc.load(trainStream);
        List<FeatureOccurrences> featuresBIOOccurrences = fc.getFeatures(features);

        // Convert to FeatureBIO
        List<BIOFeature> featuresBIO = new ArrayList<BIOFeature>();
        featuresBIO.addAll(featuresBIOOccurrences);

        logger.info("MAX NUM. FEATURES: {}", featuresBIO.size());
        
        // Create base evolving solution
        EvolvingSolution seed = createBaseSolution(featuresBIO.size());
        IFitnessAssessor fitnessClass = null;
        try {
            fitnessClass = new CRFFitnessAssessor(config, trainIL, devIL, unlabeledIL, dev, featuresBIO, geneFile, supervisedCRF); // new BogusFitnessAssessor(); //
            //fitnessClass = new BogusFitnessAssessor();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        /*
         * Instantiate the simulated annealing algorithm.
         */
        SimulatedAnnealing sa = new SimulatedAnnealing(fitnessClass, seed, 100, 0.1, 0.2, 0.2, 0.2);

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
    public static EvolvingSolution createBaseSolution(int numMaxFeatures) {
        List<IOptimizationTarget> optimisations = new ArrayList<IOptimizationTarget>();
        optimisations.add(new GEWeightOptimizationTarget(1.0));
        optimisations.add(new GPVOptimizationTarget(1.0));
        optimisations.add(new NrFeaturesOptimizationTarget(numMaxFeatures));

        return new EvolvingSolution(optimisations);
    }

    /**
     * A ficticious class to implement the fitness assessor. The fitness method
     * * returns a sum of all features.
     */
    private static class BogusFitnessAssessor implements IFitnessAssessor {

        @Override
        public Performance getFitness(EvolvingSolution solution) {
            double sum = 0;
            for (IOptimizationTarget f : solution.getFeatureList()) {
                sum += (((BIOFeature) f).getB() + ((BIOFeature) f).getI() - ((BIOFeature) f).getO()); //                System.out.println("sum = " + (sum-old));
            }
            return new Performance();
        }
    }
}
