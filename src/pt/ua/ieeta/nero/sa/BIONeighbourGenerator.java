
package pt.ua.ieeta.nero.sa;

import pt.ua.ieeta.nero.feaure.targets.BIOFeature;

/**
 *
 * @author Paulo Gaspar
 */
public class BIONeighbourGenerator implements INeighbourGenerator
{
    @Override
    public EvolvingSolution getNeighbour(EvolvingSolution solution, int k, int kmax, double dispersionFactor, double mutationAffectedPercent)
    {
        /* Create a neighbour solution object. */
        EvolvingSolution neighbour = new EvolvingSolution(solution);
        
        for (int i = 0; i < mutationAffectedPercent * neighbour.getFeatureList().size(); i++)
        {
            /* Calculate a random position. */
            int randomPos = (int) Math.round(Math.random() * (solution.getFeatureList().size()-1));

            /* Get feature to be mutated. */
            BIOFeature selectedFeature = (BIOFeature) neighbour.getFeatureList().get(randomPos);
            
            /* Mutate selected feature, considering the current iteration and dispersion factor. */
            selectedFeature.performMutation(k, kmax, dispersionFactor);
        }
        
        return neighbour;
    }
    
}
