
package pt.ua.ieeta.nero;

import java.util.ArrayList;

/**
 *
 * @author Paulo
 */
public class Nero
{
    public Nero()
    {
    
    }    

    /** Main thread entry point. */
    public static void main(String[] args)
    {
        System.out.println("Starting the NERO experiment.");
        
        /* Create seed and fitness function. */
        EvolvingSolution seed = createBaseSolution();
        IFitnessAssessor fitnessClass = new BogusFitnessAssessor();
        
        /* Instantiate the simulated annealing algorithm. */
        SimulatedAnnealing sa = new SimulatedAnnealing(fitnessClass, seed, 50000, 0.97, 0.8);
        
        try
        {
            Thread saThread = new Thread(sa);
            saThread.start();
            saThread.join();
        } 
        catch (InterruptedException ex)
        {
            System.out.println("An exception occured while starting or running the simulated annealing thread: " + ex.getMessage());
        }
        
        System.out.println("Ended the NERO experiment.");
    }

    /* Get or Create a base solution to the experiment. */
    private static EvolvingSolution createBaseSolution()
    {
        /* New feature list. */
        ArrayList<Feature> featureList = new ArrayList<Feature>();
        
        /* Generate several ficticious features. They are randomly generated. */
        for (int i=0; i<20; i++)
            featureList.add(new Feature("Bogus feature " + i, Math.random(), Math.random(), Math.random()));
        
        assert featureList.size() == 20;
        
        return new EvolvingSolution(featureList);
    }
    
    /** A ficticious class to implement the fitness assessor. The fitness method
     ** returns a sum of all features. */
    private static class BogusFitnessAssessor implements IFitnessAssessor
    {
        @Override
        public double getFitness(EvolvingSolution solution)
        {
            double sum = 0;
            for (Feature f : solution.getFeatureList())
                sum += (f.getB() + f.getI() + f.getO());
            
            return sum;
        }
    }
}
