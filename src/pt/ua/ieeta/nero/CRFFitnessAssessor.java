/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero;

import cc.mallet.fst.CRF;
import cc.mallet.fst.Transducer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.Constants;

import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.exception.GimliException;

/**
 *
 * @author david
 */
public class CRFFitnessAssessor implements IFitnessAssessor {

    // Maximum number of iterations for CRF
    private final static int MAX_ITERATIONS = 50;
    private final static Constants.LabelFormat LABEL_FORMAT = Constants.LabelFormat.BIO;
    private final static Constants.EntityType ENTITY_TYPE = Constants.EntityType.protein;
    private final Pattern forbiddenPattern = Pattern.compile(Constants.LabelTag.O + "," + Constants.LabelTag.I);
    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(CRFFitnessAssessor.class);
    private ModelConfig config;
    private InstanceList train;
    private InstanceList test;
    private InstanceList unlabeled;
    private CRFModel crfModel;

    public CRFFitnessAssessor(String configFile, final String trainFile, final String testFile, final String unlabeledFile) throws GimliException {
        assert (configFile != null);
        assert (trainFile != null);
        assert (testFile != null);
        assert (unlabeledFile != null);

        // Load model feature configuration
        this.config = new ModelConfig(configFile);

        // Load pipe
        Pipe p = CRFModel.setupPipe(config);

        // Load train data
        logger.info("Loading train data...");
        Corpus c = new Corpus(LABEL_FORMAT, ENTITY_TYPE, trainFile);
        this.train = c.toModelFormat(p);

        // Load test data
        logger.info("Loading test data...");
        c = new Corpus(LABEL_FORMAT, ENTITY_TYPE, testFile);
        this.test = c.toModelFormat(p);

        // Load unlabeled data
        logger.info("Loading unlabeled data...");
        this.unlabeled = CRFModel.loadUnlabeledData(unlabeledFile, p);
    }

    private CRF getCRF() {
        int order = config.getOrder() + 1;
        int[] orders = new int[order];
        for (int i = 0; i < order; i++) {
            orders[i] = i;
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

        for (int i = 0; i < crf.numStates(); i++) {
            crf.getState(i).setInitialWeight(Transducer.IMPOSSIBLE_WEIGHT);
        }
        crf.getState(startStateName).setInitialWeight(0.0);
        crf.setWeightsDimensionAsIn(train, false);

        return crf;
    }

    @Override
    public double getFitness(EvolvingSolution solution) {
        assert (solution != null);

        // Create CRFModel
        crfModel = new CRFModel(config, Constants.Parsing.FW);

        // Set CRF
        crfModel.setCRF(getCRF());

        // Train CRF Model
        crfModel.train(train, unlabeled, solution, MAX_ITERATIONS);

        // Get F-measure performance
        return crfModel.getF1(test);
    }
}
