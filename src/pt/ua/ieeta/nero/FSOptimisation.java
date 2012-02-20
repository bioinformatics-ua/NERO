/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByThreadedLabelLikelihood;
import cc.mallet.fst.Transducer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.ieeta.nero.crf.CRFModel;
import pt.ua.ieeta.nero.external.evaluator.BC2Evaluator;
import pt.ua.ieeta.nero.external.evaluator.Performance;
import pt.ua.ieeta.nero.feature.metrics.InfoGainUtil;
import pt.ua.ieeta.nero.feature.pipe.PipeBuilder;
import pt.ua.ieeta.nero.sa.EvolvingSolution;
import pt.ua.tm.gimli.annotator.Annotator;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.processing.Abbreviation;
import pt.ua.tm.gimli.processing.Parentheses;
import pt.ua.tm.gimli.util.FileUtil;
import pt.ua.tm.gimli.util.UnclosableBufferedInputStream;
import pt.ua.tm.gimli.writer.BCWriter;

/**
 *
 * @author david
 */
public class FSOptimisation {

    private static Logger logger = LoggerFactory.getLogger(FSOptimisation.class);
    private static final Pattern forbiddenPattern = Pattern.compile(Constants.LabelTag.O + "," + Constants.LabelTag.I);
    private static double FEATURES_PERCENTAGE = 1.0;

    public static void main(String[] args) throws GimliException, FileNotFoundException {
        String trainFile = "resources/corpus/bc2gm/train/corpus.gz";
        String devFile = "resources/corpus/bc2gm/dev/corpus.gz";
        String geneFile = "resources/corpus/bc2gm/dev/annotations";
        String unlabeledFile = "resources/corpus/silver/random_10k.gz";
        String configFile = "config/bc_semi.config";

        // Initialise
        ModelConfig config = new ModelConfig(configFile);
        UnclosableBufferedInputStream trainStream = new UnclosableBufferedInputStream(FileUtil.getFile(new FileInputStream(trainFile)));
        UnclosableBufferedInputStream devStream = new UnclosableBufferedInputStream(FileUtil.getFile(new FileInputStream(devFile)));
        UnclosableBufferedInputStream unlabeledStream = new UnclosableBufferedInputStream(FileUtil.getFile(new FileInputStream(unlabeledFile)));


        //Get total number of features
        //PipeBuilder pb = new PipeBuilder(config);
        //pb.initialise();
        //pb.finalise(true);
        //Pipe p = pb.getPipe();

        //Corpus train = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein, trainStream);
        //InstanceList t = train.toModelFormatTrain(p);
        int totalFeatures = 46066;
        logger.info("Total features: {}", totalFeatures);

        InfoGainUtil igu = new InfoGainUtil(config, trainStream);
        int numPercentageFeatures = (int) (FEATURES_PERCENTAGE * totalFeatures);
        logger.info("{}% of total features: {}", FEATURES_PERCENTAGE * 100, numPercentageFeatures);
        List<String> features = igu.getFeatures(numPercentageFeatures);


        // Train supervised-CRF
        PipeBuilder pb = new PipeBuilder(config);
        pb.initialise();
        pb.addFeatureSelector(features);
        pb.finalise(false);
        Pipe p = pb.getPipe();
        Corpus train = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein, trainStream);
        Corpus dev = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein, devStream);
        Performance per = new Performance();
        CRF supervisedCRF = getSupervisedCRF(train.toModelFormatTrain(p), config, dev, geneFile, per);
        logger.info("SUPERVISED CRF: {}", per);

        //double[] sizes = {0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08, 0.09, 0.1};
        double[] sizes = {100, 250, 500, 1000, 1500, 2000};
        //double[] sizes = {100, 100, 100, 100};

        /*
         * List<Integer> sizesList = new ArrayList<Integer>(); for (int i=100; i<=1800; i+=200){ sizesList.add(i); }
         */

        //Integer[] sizes = sizesList.toArray(new Integer[]{});

        Performance[] performance = new Performance[sizes.length];
        StopWatch[] time = new StopWatch[sizes.length];
        int[] numFeatures = new int[sizes.length];
        CRF[] crfs = new CRF[sizes.length];

        for (int i = 0; i < crfs.length; i++) {
            crfs[i] = new CRF(supervisedCRF);
        }

        //CRF supervisedCRF = null;

        for (int i = 0; i < sizes.length; i++) {

            // Get unlabeled and dev data
            /*
             * PipeBuilder pb = new PipeBuilder(config); pb.initialise(); //pb.addFeatureSelector(features);
             * pb.finalise(false); Pipe p = pb.getPipe();
             */

            p = crfs[i].getInputPipe();

            InstanceList unlabeledIL = CRFModel.loadUnlabeledData(unlabeledStream, p);

            InstanceList trainIL = train.toModelFormatTrain(p);



            int numFeaturesToOptimise = (int) sizes[i];
            logger.info("features to optimise: {}", numFeaturesToOptimise);
            List<String> featuresOptimise = features.subList(0, numFeaturesToOptimise);

            EvolvingSolution seed = Nero.createBaseSolution(trainStream, config, featuresOptimise);

            StopWatch sw = new StopWatch();
            sw.start();

            // Set CRF
            /*
             * int order = config.getOrder() + 1; int[] orders = new int[order]; for (int j = 0; j < order; j++) {
             * orders[j] = j; }
             *
             * CRF crf = new CRF(trainIL.getPipe(), (Pipe) null); String startStateName = crf.addOrderNStates( trainIL,
             * orders, null, // "defaults" parameter; see mallet javadoc "O", forbiddenPattern, null, true); // true for
             * a fully connected CRF
             *
             * for (int j = 0; j < crf.numStates(); j++) {
             * crf.getState(j).setInitialWeight(Transducer.IMPOSSIBLE_WEIGHT); }
             * crf.getState(startStateName).setInitialWeight(0.0); crf.setWeightsDimensionAsIn(trainIL, false);
             *
             * //CRFModel m = new CRFModel(config, Constants.Parsing.FW, FileUtil.getFile(new
             * FileInputStream("resources/model/bc2gm_o1_fw_1.0.gz")));
             */

            CRFModel model = new CRFModel(config, Constants.Parsing.FW);
            model.setCRF(crfs[i]);


            int numIterations = model.train(trainIL, unlabeledIL, seed, Integer.MAX_VALUE);
            sw.stop();


            Annotator an = new Annotator(dev);
            an.annotate(model);

            // Pre-process annotated corpus
            Parentheses.processRemoving(dev);
            Abbreviation.process(dev);

            String annotations = "resources/silver/bc2gm_dev_o1_fw_" + sizes[i];
            BCWriter bw = new BCWriter();
            bw.write(dev, new FileOutputStream(annotations));

            BC2Evaluator eval = new BC2Evaluator(geneFile, geneFile, annotations);
            performance[i] = eval.getPerformance();
            performance[i].setNumIterations(numIterations);
            time[i] = sw;
            numFeatures[i] = numFeaturesToOptimise;

            logger.info("{} ({}) - {} in {} - {} iterations", new Object[]{sizes[i], numFeatures[i], performance[i], time[i].toString(), performance[i].getNumIterations()});
        }

        for (int i = 0; i < sizes.length; i++) {
            logger.info("{} ({}) - {} in {} - {} iterations", new Object[]{sizes[i], numFeatures[i], performance[i], time[i].toString(), performance[i].getNumIterations()});
        }
    }

    public static CRF getSupervisedCRF(InstanceList train, ModelConfig config, Corpus dev, String geneFile, Performance per) throws GimliException, FileNotFoundException {
        //InstanceList il = train.toModelFormatTrain(p);

        // Set CRF
        int order = config.getOrder() + 1;
        int[] orders = new int[order];
        for (int j = 0; j < order; j++) {
            orders[j] = j;
        }

        CRF crf = new CRF(train.getPipe(), (Pipe) null);
        String startStateName = crf.addOrderNStates(
                train,
                orders,
                null, // "defaults" parameter; see mallet javadoc
                "O",
                forbiddenPattern,
                null,
                true); // true for a fully connected CRF

        for (int j = 0; j < crf.numStates(); j++) {
            crf.getState(j).setInitialWeight(Transducer.IMPOSSIBLE_WEIGHT);
        }
        crf.getState(startStateName).setInitialWeight(0.0);
        crf.setWeightsDimensionAsIn(train, false);




        CRFTrainerByThreadedLabelLikelihood trainer = new CRFTrainerByThreadedLabelLikelihood(crf, 8);
        trainer.setAddNoFactors(true);
        trainer.setGaussianPriorVariance(1.0);
        StopWatch time = new StopWatch();
        time.start();
        trainer.train(train, Integer.MAX_VALUE);
        time.stop();
        trainer.shutdown();

        CRFModel model = new CRFModel(config, Constants.Parsing.FW);
        model.setCRF(trainer.getCRF());

        Annotator an = new Annotator(dev);
        an.annotate(model);

        // Pre-process annotated corpus
        Parentheses.processRemoving(dev);
        Abbreviation.process(dev);

        String annotations = "resources/silver/bc2gm_dev_o1_fw";
        BCWriter bw = new BCWriter();
        bw.write(dev, new FileOutputStream(annotations));

        BC2Evaluator eval = new BC2Evaluator(geneFile, geneFile, annotations);

        Performance pe = eval.getPerformance();

        per.setPrecision(pe.getPrecision());
        per.setRecall(pe.getRecall());
        per.setF1(pe.getF1());
        per.setNumIterations(trainer.getIteration() - 1);
        per.setTime(time);

        return trainer.getCRF();
    }

    public static Performance getPerformance(ModelConfig config, CRF crf, Corpus dev, String geneFile) {
        CRFModel model = new CRFModel(config, Constants.Parsing.FW);
        model.setCRF(crf);

        Annotator an = new Annotator(dev);
        an.annotate(model);

        // Pre-process annotated corpus
        Parentheses.processRemoving(dev);
        Abbreviation.process(dev);

        String annotations = "resources/silver/bc2gm_dev_o1_fw";
        BCWriter bw = new BCWriter();
        try {
            bw.write(dev, new FileOutputStream(annotations));
        } catch (Exception ex) {
            throw new RuntimeException("There was a problem writing the annotations file.", ex);
        }

        BC2Evaluator eval = new BC2Evaluator(geneFile, geneFile, annotations);

        return eval.getPerformance();
    }
}
