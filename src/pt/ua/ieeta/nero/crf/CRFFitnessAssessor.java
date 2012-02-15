/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.crf;

import cc.mallet.fst.CRF;
import cc.mallet.fst.Transducer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.ieeta.nero.external.evaluator.BC2Evaluator;
import pt.ua.ieeta.nero.external.evaluator.Performance;
import pt.ua.ieeta.nero.feature.pipe.PipeBuilder;
import pt.ua.ieeta.nero.sa.EvolvingSolution;
import pt.ua.ieeta.nero.sa.IFitnessAssessor;
import pt.ua.tm.gimli.annotator.Annotator;
import pt.ua.tm.gimli.config.Constants;

import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.processing.Abbreviation;
import pt.ua.tm.gimli.processing.Parentheses;
import pt.ua.tm.gimli.writer.BCWriter;

/**
 *
 * @author david
 */
public class CRFFitnessAssessor implements IFitnessAssessor {

    // Maximum number of iterations for CRF
    private final static int MAX_ITERATIONS = Integer.MAX_VALUE;
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
    private Corpus testCorpus;
    private InstanceList unlabeled;
    private CRFModel crfModel;
    private String geneFile;

    public CRFFitnessAssessor(ModelConfig config, final InputStream trainFile, final InputStream testFile, final InputStream unlabeledFile, final List<String> features, final String geneFile) throws GimliException {
        assert (config != null);
        assert (trainFile != null);
        assert (testFile != null);
        assert (unlabeledFile != null);
        assert (features != null);
        assert (geneFile != null);

        this.geneFile = geneFile;
        
        // Load model feature configuration
        this.config = config;
        CRFModel model = new CRFModel(config, Constants.Parsing.FW);
        
        // Load pipe
        PipeBuilder pb = new PipeBuilder(config);
        pb.initialise();
        pb.addFeatureSelector(features);
        pb.finalise(false);
        Pipe p = pb.getPipe();
        
        //Pipe p = model.getFeaturePipe();

        // Load train data
        logger.info("Loading train data...");
        Corpus c = new Corpus(LABEL_FORMAT, ENTITY_TYPE, trainFile);
        this.train = c.toModelFormatTrain(p);

        // Load test data
        logger.info("Loading test data...");
        testCorpus = new Corpus(LABEL_FORMAT, ENTITY_TYPE, testFile);
        this.test = testCorpus.toModelFormatTrain(p);

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

        try {
            // Train CRF Model
            crfModel.train(train, unlabeled, solution, MAX_ITERATIONS);
        } catch (Exception ex) {
            logger.error("Problem training CRF: ", ex);
        }

        // Annotate corpus
        Annotator an = new Annotator(testCorpus);
        an.annotate(crfModel);
        
        // Post-process corpus
        Parentheses.processRemoving(testCorpus);
        Abbreviation.process(testCorpus);
        
        // Generate annotation file
        File tmp = null;
        try {
            tmp = File.createTempFile("annotations", ".txt");
        } catch (IOException ex) {
            throw new RuntimeException("There was a problem creating the temporary file.", ex);
        }
        tmp.deleteOnExit();
        
        BCWriter writer = new BCWriter();
        try {
            writer.write(testCorpus, new FileOutputStream(tmp));
        } catch (Exception ex) {
            throw new RuntimeException("There was a problem writing the annotations file.", ex);
        }
        
        // Get Performance
        BC2Evaluator eval = new BC2Evaluator(geneFile, geneFile, tmp.getAbsolutePath());
        Performance p = eval.getPerformance();
        
        // Return F-measure
        return p.getF1();
    }
}
