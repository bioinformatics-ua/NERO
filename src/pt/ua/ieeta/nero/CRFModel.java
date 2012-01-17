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
package pt.ua.ieeta.nero;

import cc.mallet.fst.*;
import cc.mallet.fst.semi_supervised.CRFOptimizableByGE;
import cc.mallet.fst.semi_supervised.StateLabelMap;
import cc.mallet.fst.semi_supervised.constraints.GEConstraint;
import cc.mallet.fst.semi_supervised.constraints.OneLabelKLGEConstraints;
import cc.mallet.optimize.Optimizable;
import pt.ua.tm.gimli.config.ModelConfig;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.tsf.RegexMatches;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.Maths;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.annotator.Annotator;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.features.Input2TokenSequence;
import pt.ua.tm.gimli.features.MixCase;
import pt.ua.tm.gimli.features.NumberOfCap;
import pt.ua.tm.gimli.features.NumberOfDigit;
import pt.ua.tm.gimli.features.WordLength;
import pt.ua.tm.gimli.config.Constants.Parsing;
import pt.ua.tm.gimli.model.CRFBase;
import pt.ua.tm.gimli.writer.BCWriter;
import pt.ua.tm.gimli.writer.JNLPBAWriter;

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
    /**
     * Regular expression to identify uppercase letters.
     */
    private static String CAPS = "[A-Z]";
    /**
     * Regular expression to identify lowercase letters.
     */
    private static String LOW = "[a-z]";
    /**
     * Regular expression to identify Greek letters.
     */
    private static String GREEK = "(alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|omicron|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)";

    /**
     * Constructor.
     *
     * @param config Model configuration.
     * @param parsing Parsing direction.
     */
    public CRFModel(final ModelConfig config, final Parsing parsing) {
        super(config, parsing);
    }

    /**
     * Constructor that loads the model from an input file.
     *
     * @param config Model configuration.
     * @param parsing Parsing direction.
     * @param file File that contains the model.
     * @throws GimliException Problem reading the model from file.
     */
    public CRFModel(final ModelConfig config, final Parsing parsing, final String file) throws GimliException {
        super(config, parsing, file);
    }

    /**
     * Setup the features to be used by the model.
     *
     * @return The {@link Pipe} that contains the description of the features to be extracted.
     * @throws GimliException Problem specifying the features.
     */
    public static Pipe setupPipe(final ModelConfig config) throws GimliException {
        ArrayList<Pipe> pipe = new ArrayList<Pipe>();

        try {
            pipe.add(new Input2TokenSequence(config));

            if (config.isCapitalization()) {
                pipe.add(new RegexMatches("InitCap", Pattern.compile(CAPS + ".*")));
                pipe.add(new RegexMatches("EndCap", Pattern.compile(".*" + CAPS)));
                pipe.add(new RegexMatches("AllCaps", Pattern.compile(CAPS + "+")));
                pipe.add(new RegexMatches("Lowercase", Pattern.compile(LOW + "+")));
                pipe.add(new MixCase());
                pipe.add(new RegexMatches("DigitsLettersAndSymbol", Pattern.compile("[0-9a-zA-z]+[-%/\\[\\]:;()'\"*=+][0-9a-zA-z]+")));
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

            if (config.isGreek()) {
                pipe.add(new RegexMatches("GREEK", Pattern.compile(GREEK, Pattern.CASE_INSENSITIVE)));
            }

            if (config.isRoman()) {
                pipe.add(new RegexMatches("ROMAN", Pattern.compile("((?=[MDCLXVI])((M{0,3})((C[DM])|(D?C{0,3}))?((X[LC])|(L?XX{0,2})|L)?((I[VX])|(V?(II{0,2}))|V)?))")));
            }

            pipe.add(new TokenSequence2FeatureVectorSequence(true, true));

        } catch (Exception ex) {
            throw new GimliException("There was a problem initializing the features.", ex);
        }
        return new SerialPipes(pipe);
    }

    private HashMap<Integer, double[][]> loadGEConstraints(EvolvingSolution solution, InstanceList data) {

        HashMap<Integer, double[][]> constraints = new HashMap<Integer, double[][]>();

        List<Feature> features = solution.getFeatureList();
        for (Feature f : features) {

            // Get feature index
            int featureIndex = data.getDataAlphabet().lookupIndex(f.getName(), false);
            if (featureIndex == -1) {
                throw new RuntimeException("Feature " + f.getName() + " not found in the alphabet!");
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

    public double getF1(InstanceList test){
        String[] allowedTags = new String[]{Constants.LabelTag.B.toString(), Constants.LabelTag.I.toString()};
        
        // Define Evaluator
        MyMultiSegmentationEvaluator evaluator = new MyMultiSegmentationEvaluator(
                new InstanceList[]{test},
                new String[]{"test"}, allowedTags, allowedTags) {
        };

        // Evaluate
        NoopTransducerTrainer crfTrainer = new NoopTransducerTrainer(getCRF());
        evaluator.evaluateInstanceList(crfTrainer, test, "test");
        
        return evaluator.getOverallF1();
    }
    
    public void train(final InstanceList train, final InstanceList unlabeled, final EvolvingSolution features, final int iterations) {
        CRF crf = getCRF();

        // Semi-supervised
        int numThreads = 8;

        // Load constraints
        HashMap<Integer, double[][]> constraints = loadGEConstraints(features, train);
        ArrayList constraintsList = new ArrayList();

        // Set OneLabelKL constraints
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

        // Set label map
        StateLabelMap map = new StateLabelMap(train.getTargetAlphabet(), true);

        // Optimazable gradients
        Optimizable.ByGradientValue[] opts = new Optimizable.ByGradientValue[]{
            new CRFOptimizableByGE(crf, constraintsList, unlabeled, map, numThreads, 1.0),
            new CRFOptimizableByLabelLikelihood(getCRF(), train)
        };

        // Train
        CRFTrainerByValueGradients crfTrainer = new CRFTrainerByValueGradients(crf, opts);
        crfTrainer.train(train, iterations);

        // Set CRF
        setCRF(crf);
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

    private CRF loadModelFromFile(String file) throws Exception {
        ObjectInputStream ois = null;
        CRF crf = null;
        ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
        crf = (CRF) ois.readObject();
        ois.close();
        return crf;
    }

    public static InstanceList loadUnlabeledData(String file, Pipe p) throws GimliException {

        InstanceList instances = new InstanceList(p);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
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
            Corpus c = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein, "/Users/david/Downloads/gimli_corpora_new_tokens/bc2gm/train_5k.gz");

            ModelConfig mc = new ModelConfig("config/bc_semi.config");

            CRFModel crf = new CRFModel(mc, Parsing.FW);

            crf.train(c);
            crf.writeToFile("/Users/david/Downloads/bc2gm_o1_fw_semi.gz");

            Corpus testBC = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein, "/Users/david/Downloads/gimli_corpora_new_tokens/bc2gm/test.gz");
            Corpus testJNLPBA = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein, "/Users/david/Downloads/gimli_corpora_new_tokens/jnlpba/test.gz");

            Annotator a = new Annotator(testBC);
            a.annotate(crf);
            BCWriter bc = new BCWriter();
            bc.write(testBC, "/Users/david/Downloads/bc2gm_o1_fw_semi");

            a = new Annotator(testJNLPBA);
            a.annotate(crf);
            JNLPBAWriter jnlpba = new JNLPBAWriter();
            jnlpba.write(testJNLPBA, "/Users/david/Downloads/jnlpba_protein_o1_fw_semi");

            System.out.println("DONE!");
            System.exit(0);

        } catch (GimliException ex) {
            ex.printStackTrace();
        }

    }
}
