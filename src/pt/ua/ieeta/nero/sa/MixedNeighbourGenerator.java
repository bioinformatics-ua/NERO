/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.sa;

import pt.ua.ieeta.nero.feaure.targets.BIOFeature;
import pt.ua.ieeta.nero.feaure.targets.IOptimizationTarget;

/**
 *
 * @author Paulo
 */
public class MixedNeighbourGenerator implements INeighbourGenerator
{
    private int numWeights = 3;
    
    @Override
    public EvolvingSolution getNeighbour(EvolvingSolution solution, int k, int kmax, double dispersionFactor, double mutationAffectedPercent)
    {
        assert solution != null;
        assert solution.getFeatureList() != null;
        assert k >= 0;
        assert kmax > 0;
        assert k <= kmax;        
        
        /* Create a neighbour solution object. */
        EvolvingSolution neighbour = new EvolvingSolution(solution);
        
        /* Calculate a random position, to select which parameter wil be changed. */
        int randomPos = (int) Math.round(Math.random() * (numWeights-1));
        
        /* Get parameters to optimize. */
        IOptimizationTarget targetToMutate = neighbour.getFeatureList().get(randomPos);
        
        /* Mutate selected parameter, considering the current iteration and dispersion factor. */
        targetToMutate.performMutation(k, kmax, dispersionFactor);
        
        for (int i = 0; i < mutationAffectedPercent * neighbour.getFeatureList().size(); i++)
        {
            /* Calculate a random position. */
            randomPos = numWeights + (int) Math.round(Math.random() * (solution.getFeatureList().size()-numWeights-1));

            /* Get feature to be mutated. */
            BIOFeature selectedFeature = (BIOFeature) neighbour.getFeatureList().get(randomPos);
            
            /* Mutate selected feature, considering the current iteration and dispersion factor. */
            selectedFeature.performMutation(k, kmax, dispersionFactor);
        }
        
        return neighbour;
    }
}
