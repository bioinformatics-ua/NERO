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
public class GPVOptimizationTarget extends IOptimizationTarget
{
    private static boolean DEBUG = true;
    private static Logger logger = LoggerFactory.getLogger(GEWeightOptimizationTarget.class);
    
    /* GPV Weight value. */
    private double GPVWeight;

    /* Constraints*/
    private static double MIN = 0;
    private static double MAX = 1.5;
    
    public GPVOptimizationTarget(double GPVWeight) {
        assert (GPVWeight >= MIN && GPVWeight <= MAX);
        this.GPVWeight = GPVWeight;
    }
    
    public double getGPVWeight()
    {
        return GPVWeight;
    }

    private void setGPVWeight(double GPVWeight)
    {
        assert GPVWeight >= 0;
        
        this.GPVWeight = GPVWeight;
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
        double mutationFactor = (Math.random() - 0.5) * 2 * ((kmax-k)/(double)kmax) * dispersionFactor;
        
        /* Apply mutation. */
        double value = getGPVWeight();
        value = Math.min(MAX, Math.max(MIN, value + mutationFactor));
        
        if (DEBUG) logger.info("GPV Weight was " + getGPVWeight() + " and now is " + value + " (mutationFactor="+mutationFactor+")");
        
        /* Save value. */
         setGPVWeight(value);
    }
}
