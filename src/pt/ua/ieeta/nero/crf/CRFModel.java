/*
 * Gimli - High-performance and multi-corpus recognition of biomedical
 * entity names
 *
 * Copyright (C) 2011 David Campos, Universidade de Aveiro, Instituto de
 * Engenharia Electrónica e Telemática de Aveiro
 *
 * Gimli is licensed under the Creative Commons
 * Attribution-NonCommercial-ShareAlike 3.0 Unported License. To view a copy of
 * this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/.
 *
 * Gimli is a free software, you are free to copy, distribute, change and
 * transmit it. However, you may not use Gimli for commercial purposes.
 *
 * Gimli is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 *
 */
package pt.ua.ieeta.nero.crf;

import pt.ua.ieeta.nero.feaure.targets.BIOFeature;
import cc.mallet.fst.*;
import cc.mallet.fst.semi_supervised.StateLabelMap;
import cc.mallet.fst.semi_supervised.constraints.OneLabelKLGEConstraints;
import cc.mallet.pipe.*;
import pt.ua.tm.gimli.config.ModelConfig;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.Maths;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.ieeta.nero.FSOptimisation;
import pt.ua.ieeta.nero.external.evaluator.Performance;
import pt.ua.ieeta.nero.feature.metrics.InfoGainUtil;
import pt.ua.ieeta.nero.sa.EvolvingSolution;
import pt.ua.ieeta.nero.feature.pipe.PipeBuilder;
import pt.ua.ieeta.nero.feaure.targets.IOptimizationTarget;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.config.Constants.Parsing;
import pt.ua.tm.gimli.model.CRFBase;
import pt.ua.tm.gimli.util.FileUtil;

/**
 * The CRF model used by Gimli, providing features to train and test the models.
 *
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class CRFModel extends CRFBase {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(CRFModel.class);
    private CRF supervisedCRF;
    private static final Pattern forbiddenPattern = Pattern.compile(Constants.LabelTag.O + "," + Constants.LabelTag.I);

    /**
     * Constructor.
     *
     * @param config Model configuration.
     * @param parsing Parsing direction.
     */
    public CRFModel(final ModelConfig config, final Parsing parsing) {
        super(config, parsing);
        supervisedCRF = null;
    }

    /**
     * Constructor that loads the model from an input file.
     *
     * @param config Model configuration.
     * @param parsing Parsing direction.
     * @param file File that contains the model.
     * @throws GimliException Problem reading the model from file.
     */
    public CRFModel(final ModelConfig config, final Parsing parsing, final InputStream input) throws GimliException {
        super(config, parsing, input);
        supervisedCRF = null;
    }

    public CRF getSupervisedCRF() {
        return supervisedCRF;
    }

    public void setSupervisedCRF(CRF supervisedCRF) {
        this.supervisedCRF = supervisedCRF;
    }

    private HashMap<Integer, double[][]> loadGEConstraints(EvolvingSolution solution, InstanceList data) {

        HashMap<Integer, double[][]> constraints = new HashMap<Integer, double[][]>();

        List<IOptimizationTarget> features = solution.getFeatureList();
        for (IOptimizationTarget bioFeature : features) 
        {
            BIOFeature f = (BIOFeature) bioFeature;
            
            // Get feature index
            int featureIndex = data.getDataAlphabet().lookupIndex(f.getName(), false);
            if (featureIndex == -1) {
                logger.error("Feature {} not found in the alphabet!", f.getName());
                continue;
                //throw new RuntimeException("BIOFeature " + f.getName() + " not found in the alphabet!");
            }

            // Initiate probabilities
            double[][] probs = new double[data.getTargetAlphabet().size()][2];
            for (int i = 0; i < probs.length; i++) {
                Arrays.fill(probs[i], Double.NEGATIVE_INFINITY);
            }

            // Get B
            int li = data.getTargetAlphabet().lookupIndex("B", false);
            assert (li != -1) : "B";
            probs[li][0] = probs[li][1] = f.getB();

            //Get I
            li = data.getTargetAlphabet().lookupIndex("I", false);
            assert (li != -1) : "I";
            probs[li][0] = probs[li][1] = f.getI();

            //Get O
            li = data.getTargetAlphabet().lookupIndex("O", false);
            assert (li != -1) : "O";
            probs[li][0] = probs[li][1] = f.getO();

            // Add constraint
            constraints.put(featureIndex, probs);
        }
        return constraints;
    }

    public double getF1(InstanceList test) {
        String[] allowedTags = new String[]{Constants.LabelTag.B.toString(), Constants.LabelTag.I.toString()};

        // Define Evaluator
        F1MultiSegmentationEvaluator evaluator = new F1MultiSegmentationEvaluator(
                new InstanceList[]{test},
                new String[]{"test"}, allowedTags, allowedTags) {
        };

        // Evaluate
        NoopTransducerTrainer crfTrainer = new NoopTransducerTrainer(getCRF());
        evaluator.evaluateInstanceList(crfTrainer, test, "test");

        return evaluator.getOverallF1();
    }

    public int train(final InstanceList train, final InstanceList unlabeled, final EvolvingSolution features, final int iterations) {
        //CRF crf = getCRF();

        // Semi-supervised
        int numThreads = 4;

        // Load constraints

        ArrayList constraintsList = new ArrayList();
        if (unlabeled != null && features != null) {
            HashMap<Integer, double[][]> constraints = loadGEConstraints(features, train);

            // Set OneLabelKL constraints OneLabelKLGEConstraints 
            OneLabelKLGEConstraints geConstraints = new OneLabelKLGEConstraints();
            for (int fi : constraints.keySet()) {
                double[][] dist = constraints.get(fi);

                boolean allSame = true;
                double sum = 0;

                double[] prob = new double[dist.length];
                for (int li = 0; li < dist.length; li++) {
                    prob[li] = dist[li][0];
                    if (!Maths.almostEquals(dist[li][0], dist[li][1])) {
                        allSame = false;
                        break;
                    } else if (Double.isInfinite(prob[li])) {
                        prob[li] = 0;
                    }
                    sum += prob[li];
                }

                if (!allSame) {
                    throw new RuntimeException("A KL divergence penalty cannot be used with target ranges!");
                }
                if (!Maths.almostEquals(sum, 1)) {
                    throw new RuntimeException("Targets must sum to 1 when using a KL divergence penalty!");
                }

                geConstraints.addConstraint(fi, prob, 1);
            }
            constraintsList.add(geConstraints);
        }


        // Initialise CRF
        int order = getConfig().getOrder() + 1;
        int[] orders = new int[order];
        for (int i = 0; i < order; i++) {
            orders[i] = i;
        }

        // Set label map
        StateLabelMap map = new StateLabelMap(train.getTargetAlphabet(), true);

        CRF crf = getCRF();

        // Optimazable gradients
        /*
         * Optimizable.ByGradientValue[] opts; if (unlabeled != null && features != null) { opts = new
         * Optimizable.ByGradientValue[]{ new CRFOptimizableByGE(crf, constraintsList, unlabeled, map, 8), new
         * CRFOptimizableByLabelLikelihood(crf, train),}; } else { opts = new Optimizable.ByGradientValue[]{ //new
         * CRFOptimizableByGE(crf, constraintsList, unlabeled, map, numThreads, 1.0), new
         * CRFOptimizableByLabelLikelihood(crf, train) }; }
         */

//        logger.info("TOTAL SIZE OF ALPHABET AFTER FEATURE SELECTION): {}", train.getDataAlphabet().size());

        // Train

        if (unlabeled != null && features != null) {
            //CRFTrainerByValueGradients crfTrainer = new CRFTrainerByValueGradients(crf, opts);
            //crfTrainer.train(train, iterations);

            MyCRFTrainerByLikelihoodAndGE2 crfTrainer = new MyCRFTrainerByLikelihoodAndGE2(crf, constraintsList, map);
            crfTrainer.setNumThreads(8);
            crfTrainer.setGEWeight(1.0);
            crfTrainer.setGaussianPriorVariance(1.0);
            crfTrainer.setInitSupervised(true);
            //crfTrainer.setSupervisedCRF(supervisedCRF);
            crfTrainer.train(train, unlabeled, iterations);

            //this.supervisedCRF = crfTrainer.getSupervisedCRF();

            // Set CRF
            setCRF(crf);
            return crfTrainer.getIteration() - 1;


        } else {
            CRFTrainerByThreadedLabelLikelihood crfTrainer = new CRFTrainerByThreadedLabelLikelihood(crf, 8);
            crfTrainer.train(train, iterations);
            crfTrainer.shutdown();

            // Set CRF
            setCRF(crf);
            return crfTrainer.getIteration() - 1;
        }






    }

    /**
     * Train the CRF model.
     *
     * @throws GimliException Problem training the model.
     */
    @Override
    public void train(final Corpus corpus) throws GimliException {
        throw new NotImplementedException("Use the other train method");
    }

    public static InstanceList loadUnlabeledData(InputStream input, Pipe p) throws GimliException {

        InstanceList instances = new InstanceList(p);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(input));
            String line;
            String text = "";
            int i = 0;
            while ((line = br.readLine()) != null) {
                if (line.equals("") || line.equals("\n")) {
                    instances.addThruPipe(new Instance(text, null, i++, null));
                    text = "";
                } else {
                    text += line;
                    text += "\n";
                }
            }
            br.close();
        } catch (IOException ex) {
            throw new GimliException("Problem reading unlabeled data.", ex);
        }

        return instances;
    }

    public static void main(String[] args) {
        try {
            String trainFile = "resources/corpus/bc2gm/train/corpus.gz";
            String devFile = "resources/corpus/bc2gm/dev/corpus.gz";
            String geneFile = "resources/corpus/bc2gm/dev/annotations";
            String testFile = "resources/corpus/bc2gm/test/corpus.gz";
            String unlabeledFile = "resources" + File.separator + "corpus" + File.separator + "unlabeled_1k.gz";
            String configFile = "config" + File.separator + "bc_semi.config";

            ModelConfig mc = new ModelConfig(configFile);
            mc.print();


            Corpus train = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein, FileUtil.getFile(new FileInputStream(trainFile)));



            // Get test data
            //PipeBuilder pb = new PipeBuilder(mc);
            //pb.initialise();
            //pb.finalise(false);
            //Pipe p = pb.getPipe();
            Corpus dev = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein, FileUtil.getFile(new FileInputStream(devFile)));
            //Corpus test = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein, FileUtil.getFile(new FileInputStream(testFile)));
            //InstanceList devInstances = dev.toModelFormatTrain(p);
            //InstanceList unlabeled = loadUnlabeledData(FileUtil.getFile(new FileInputStream(unlabeledFile)), p);


            // Get top features
            PipeBuilder pb = new PipeBuilder(mc);
            pb.initialise();
            pb.finalise(true);
            Pipe p = pb.getPipe();
            InstanceList trainInstances = train.toModelFormatTrain(p);
            int totalFeatures = trainInstances.getDataAlphabet().size();
            InfoGainUtil igf = new InfoGainUtil(trainInstances);

            //double[] sizes = {0.01, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
            //double[] sizes = {0.8, 0.9, 1.0};
            double[] sizes = {0.6, 0.7, 0.8, 0.9};
            Performance[] results = new Performance[sizes.length];
            double[] numFeatures = new double[sizes.length];

            for (int j = 0; j < sizes.length; j++) {


                int feat = (int) (sizes[j] * totalFeatures) - 5;
                List<String> features = igf.getFeatures(feat);

                numFeatures[j] = feat;

                /*
                 * if (j == 0) { for (String f : features) { logger.info("{}", f); } }
                 */

                // Get data
                pb = new PipeBuilder(mc);
                pb.initialise();
                pb.addFeatureSelector(features);
                pb.finalise(false);
                p = pb.getPipe();

                Performance per = new Performance();
                CRF crf = FSOptimisation.getSupervisedCRF(train.toModelFormatTrain(p), mc, dev, geneFile, per);

                results[j] = per;






                /*
                 * InstanceList newInstList = train.toModelFormatTrain(p);
                 *
                 * // Set CRF int order = mc.getOrder() + 1; int[] orders = new int[order]; for (int i = 0; i < order;
                 * i++) { orders[i] = i; }
                 *
                 * CRF crf = new CRF(newInstList.getPipe(), (Pipe) null); String startStateName = crf.addOrderNStates(
                 * newInstList, orders, null, // "defaults" parameter; see mallet javadoc "O", forbiddenPattern, null,
                 * true); // true for a fully connected CRF
                 *
                 * for (int i = 0; i < crf.numStates(); i++) {
                 * crf.getState(i).setInitialWeight(Transducer.IMPOSSIBLE_WEIGHT); }
                 * crf.getState(startStateName).setInitialWeight(0.0); crf.setWeightsDimensionAsIn(newInstList, false);
                 * CRFModel model = new CRFModel(mc, Parsing.FW); model.setCRF(crf);
                 *
                 * //model.train(trainInstances, unlabeled, Nero.createBaseSolution(), Integer.MAX_VALUE); int
                 * numIterations = model.train(newInstList, null, null, Integer.MAX_VALUE, null); model.write(new
                 * GZIPOutputStream(new FileOutputStream("resources/model/bc2gm_o1_fw_" + sizes[j] + ".gz")));
                 *
                 * iterations[j] = numIterations;
                 *
                 * //InstanceList devInstances = dev.toModelFormatTrain(p); //results[j] = model.getF1(devInstances);
                 *
                 * Annotator an = new Annotator(dev); an.annotate(model);
                 *
                 * // Pre-process annotated corpus Parentheses.processRemoving(dev); Abbreviation.process(dev);
                 *
                 * String annotations = "resources/silver/bc2gm_dev_o1_fw_" + sizes[j]; BCWriter bw = new BCWriter();
                 * bw.write(dev, new FileOutputStream(annotations));
                 *
                 * BC2Evaluator eval = new BC2Evaluator(geneFile, geneFile, annotations); results[j] =
                 * eval.getPerformance();
                 */
                /*
                 * an = new Annotator(test); an.annotate(model); bw = new BCWriter(); bw.write(test, new
                 * FileOutputStream("resources/silver/bc2gm_test_o1_fw"));
                 */

                //InstanceList devInstances = dev.toModelFormatTrain(p);
                //results[j] = model.getF1(devInstances);

                logger.info("{} ({}) - {} in {} - {} iterations", new Object[]{sizes[j], numFeatures[j], results[j], results[j].getTime(), results[j].getNumIterations()});
            }



            logger.info("");
            logger.info("");
            for (int j = 0; j < sizes.length; j++) {
                logger.info("{} ({}) - {} in {} - {} iterations", new Object[]{sizes[j], numFeatures[j], results[j], results[j].getTime(), results[j].getNumIterations()});
            }

            System.out.println("DONE!");
            System.exit(0);

        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(CRFModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GimliException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public Pipe getFeaturePipe() throws GimliException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
