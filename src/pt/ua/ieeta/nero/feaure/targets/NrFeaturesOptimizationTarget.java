/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.feaure.targets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Paulo
 */
public class NrFeaturesOptimizationTarget extends IOptimizationTarget
{
    private static boolean DEBUG = true;
    private static Logger logger = LoggerFactory.getLogger(GEWeightOptimizationTarget.class);

    /* Constraints*/
    private static double MIN = 1;
    private static double MAX = 1000;
    
    /* Dispersion factor. Used together with the initial dispersion from the SA. */
    private double internalDispersionFactor = 200;
    
    public NrFeaturesOptimizationTarget(int maxNumFeatures)
    {
        assert maxNumFeatures >= 1;
        
        MAX = maxNumFeatures;
        numFeatures = maxNumFeatures;
        internalDispersionFactor = 0.6 * maxNumFeatures;
    }
    
    /** Default constructor. Uses 1000 as the max number of features. */
    public NrFeaturesOptimizationTarget() {}
    
    private int numFeatures;

    public int getNumFeatures()
    {
        return numFeatures;
    }

    private void setNumFeatures(int numFeatures)
    {
        assert numFeatures >= 1;
        
        this.numFeatures = numFeatures;
    }

    @Override
    public void performMutation(int k, int kmax, double dispersionFactor)
    {
        assert k >= 0;
        assert kmax > 0;
        assert kmax >= k;
        assert dispersionFactor > 0;
        
        /* Control the dispersion factor. */
        dispersionFactor = dispersionFactor > 1 ? 1 : dispersionFactor;
        
        /* Calculate the mutation factor. */
        double mutationFactor = (Math.random() - 0.5) * internalDispersionFactor * ((kmax-k)/(double)kmax) * dispersionFactor;
        
        /* Apply mutation. */
        int value = getNumFeatures();
        value = (int) Math.min(MAX, Math.max(MIN, value + mutationFactor));
        
        if (DEBUG) logger.info("Num features was " + getNumFeatures() + " and now is " + value + " (mutationFactor="+mutationFactor+")");
        
        /* Save value. */
        setNumFeatures(value);
    }
}
