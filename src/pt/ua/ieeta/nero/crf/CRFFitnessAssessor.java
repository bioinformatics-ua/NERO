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
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.ieeta.nero.FSOptimisation;
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
    private String geneFile;
    private CRF supervisedCRF;

    public CRFFitnessAssessor(ModelConfig config, final InstanceList train, final InstanceList test, final InstanceList unlabeled, Corpus testCorpus, final List<String> features, final String geneFile, final CRF supervisedCRF) throws GimliException {
        assert (config != null);
        assert (train != null);
        assert (test != null);
        assert (unlabeled != null);
        assert (features != null);
        assert (geneFile != null);
        assert (supervisedCRF != null);
        assert (testCorpus != null);

        this.config = config;
        this.train = train;
        this.test = test;
        this.unlabeled = unlabeled;
        this.geneFile = geneFile;
        this.supervisedCRF = supervisedCRF;
        this.testCorpus = testCorpus;
        
        // Load unlabeled data
        //logger.info("Loading unlabeled data...");
        //this.unlabeled = CRFModel.loadUnlabeledData(unlabeledFile, supervisedCRF.getInputPipe());
    }
    
    
    @Override
    public double getFitness(EvolvingSolution solution) {
        assert (solution != null);

        CRF crf =new CRF(supervisedCRF);
        
        logger.info("SUPERVISED CRF: {}", FSOptimisation.getPerformance(config, crf, testCorpus, geneFile));
        
        // Create CRFModel
        CRFModel model = new CRFModel(config, Constants.Parsing.FW);

        // Set CRF
        model.setCRF(new CRF(supervisedCRF));

        StopWatch time = new StopWatch();
        time.start();
        int numIterations = model.train(train, unlabeled, solution, Integer.MAX_VALUE);
        time.stop();

        // Annotate corpus
        Annotator an = new Annotator(testCorpus);
        an.annotate(model);
        
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
        p.setNumIterations(numIterations);
        p.setTime(time);
        
        logger.info("PERFORMANCE: {}", p);
        
        // Return F-measure
        return p.getF1();
    }
    
    
    
    
    
    
    public Performance getFitnessPerformance(EvolvingSolution solution) {
        assert (solution != null);
        int numIterations = 0;
        
        // Create CRFModel
        CRFModel model = new CRFModel(config, Constants.Parsing.FW);

        // Set CRF
        model.setCRF(supervisedCRF);

        try {
            // Train CRF Model
            numIterations = model.train(train, unlabeled, solution, MAX_ITERATIONS);
        } catch (Exception ex) {
            logger.error("Problem training CRF: ", ex);
        }

        // Annotate corpus
        Annotator an = new Annotator(testCorpus);
        an.annotate(model);
        
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
        p.setNumIterations(numIterations);
        return p;
    }
}
