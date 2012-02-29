/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.feature.metrics;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.ieeta.nero.feature.pipe.PipeBuilder;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.exception.GimliException;

/**
 *
 * @author david
 */
public class InfoGainUtil {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(InfoGainUtil.class);
    
    private InfoGain infoGain;
    private InstanceList data;

    public InfoGainUtil(InstanceList data) {
        assert (data != null);
        this.data = data;
        this.infoGain = new InfoGain(data);
    }

    public InfoGainUtil(ModelConfig config, InputStream input) {
        try {
            // Get corpus
            Corpus c = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein, input);
            // Get pipe
            PipeBuilder pb = new PipeBuilder(config);
            pb.initialise();
            pb.finalise(true);
            Pipe p = pb.getPipe();
            // Get instance data
            this.data = c.toModelFormatTrain(p);
            
            
            
            this.infoGain = new InfoGain(data);
        } catch (GimliException ex) {
            throw new RuntimeException("There was a problem loading the corpus.", ex);
        }
    }

    public List<String> getFeatures(){
        return getFeatures(data.getDataAlphabet().size());
    }
    
    public List<String> getFeatures(int numFeatures) {
        
        logger.info("TOTAL SIZE OF ALPHABET: {}", data.getDataAlphabet().size());
        
        /*for (Instance inst : data) {
            logger.info("{}", inst.getData());
        }*/
        
        // Get features for the specified rank
        ArrayList<String> features = new ArrayList<String>();
        for (int rank = 0; rank < numFeatures; rank++) {
            Integer index = infoGain.getIndexAtRank(rank);
            //double weight = infoGain.getIndexAtRank(rank);
            String name = (String) data.getDataAlphabet().lookupObject(index);

            features.add(name);
        }
        return features;
    }
}
