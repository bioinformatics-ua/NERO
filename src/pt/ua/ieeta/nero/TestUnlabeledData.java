/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.ieeta.nero.crf.CRFModel;
import pt.ua.ieeta.nero.feature.pipe.PipeBuilder;
import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.util.FileUtil;
import pt.ua.tm.gimli.util.UnclosableBufferedInputStream;

/**
 *
 * @author david
 */
public class TestUnlabeledData {
    private static Logger logger = LoggerFactory.getLogger(TestUnlabeledData.class);
    public static void main(String[] args) throws FileNotFoundException, GimliException{
        String configFile = "config/bc_semi.config";
        String unlabeledFile = "resources/corpus/silver/random_10k.gz";
        UnclosableBufferedInputStream unlabeledStream = new UnclosableBufferedInputStream(FileUtil.getFile(new FileInputStream(unlabeledFile)));
        
        ModelConfig config = new ModelConfig(configFile);
        PipeBuilder pb = new PipeBuilder(config);
        pb.initialise();
        pb.finalise(false);
        Pipe p = pb.getPipe();
        
        InstanceList unlabeledIL = CRFModel.loadUnlabeledData(unlabeledStream, p);
        
        for (Instance i:unlabeledIL){
            logger.info("{}", i.getSource());
            logger.info("{}", i.getData());
        }
    }
}
