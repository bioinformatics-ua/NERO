
package pt.ua.ieeta.nero;


/**
 *
 * @author Paulo Gaspar
 */
public class SimulatedAnnealing implements Runnable
{
    /* Final results: solution and its score. */
    private EvolvingSolution resultingSolution;
    private double score;
    
    /* Simmulated annealing parameters */
    private double coolingSchedule;
    private double convergenceFraction;
    private int kmax;
    private IFitnessAssessor fitnessCalculator;
    private EvolvingSolution seed;

    /* DEBUG flag. */
    private boolean DEBUG = true;
    
    /** Class constructor. Receives some parameters for the simmulated annealing algorithm.
     ** @param fitnessCalculator The class that implements the IFitnessAssessor interface. Responsible for calculating the fitness. 
     ** @param seed The initial object to start from. */
    public SimulatedAnnealing(IFitnessAssessor fitnessCalculator, EvolvingSolution seed)
    {
        assert fitnessCalculator != null;
        assert seed != null;
        assert !seed.getFeatureList().isEmpty();
        
        this.kmax = 10000;
        this.coolingSchedule = 0.9;
        this.convergenceFraction = 0.1;
        this.fitnessCalculator = fitnessCalculator;
        this.seed = seed;
    }
    
    /** Class constructor. Receives some parameters for the simmulated annealing algorithm.
     ** @param fitnessCalculator The class that implements the IFitnessAssessor interface. Responsible for calculating the fitness. 
     ** @param seed The initial object to start from. 
     ** @param kmax The maximum number of iterations. Each iteration corresponds to a single call to the fitnessCalculator object. 
     ** @param coolingSchedule Controls the simmulated annealing temperature decrease: 
     **                        smaller values make the temperature decrease faster, and the algorithm terminate faster as well.
     **                        Larger values (closer to 1) generally return better solutions. 
     ** @param convergenceFraction The percentage of kmax that is considered to be the maximum number of consecutive iterations 
     *                             without evolution. */
    public SimulatedAnnealing(IFitnessAssessor fitnessCalculator, EvolvingSolution seed, int kmax, double coolingSchedule, double convergenceFraction)
    {
        assert fitnessCalculator != null;
        assert seed != null;
        assert !seed.getFeatureList().isEmpty();
        assert kmax > 0;
        assert ((coolingSchedule > 0) && (coolingSchedule < 1));
        assert convergenceFraction > 0;
        
        this.kmax = kmax;
        this.coolingSchedule = coolingSchedule;
        this.fitnessCalculator = fitnessCalculator;
        this.seed = seed;
        this.convergenceFraction = convergenceFraction;
    }
    
    /** Runs the simulated annealing algorithm. Evolves the seed to find the maximum 
     ** fitness according to the fitness assessor.
     ** @return The solution that was found. */
    public synchronized EvolvingSolution runSimulatedAnnealing()
    {        
        assert fitnessCalculator != null;
        assert seed != null;
        
        /* Control maximum number of iterations. */
        if (kmax <= 0)
        {
            System.out.println("Invalid maximum number of interations! Must be a positive integer number. The input was: " + kmax);
            
            score = 0;
            resultingSolution = null;
            
            return null;
        }
        
        /* The evolving object. Initially takes the seed value. */
        EvolvingSolution s = seed;
        
        /* sbest is the best evolution so far. 
         * snew is the object to support a new evolution.*/
        EvolvingSolution sbest, snew;
        
        /* Fitness values: current, new and best. */
        double e, enew = 0, ebest;
        
        /* Counter to deal with convergency: if no evolution occurs through 10% 
         * of the maximum cicles, give up! */
        int convergenceCounter = 0;

        /* Calculate fitness for the initial seed. */
        e = calculateEnergy(fitnessCalculator, s);

        /* Initial values. */
        sbest  = s; ebest = e;
        int k = 0; //k is the iteration counter 
        double emax = 99.999f;
        double pacceptance;

        /* Main loop. 
         * Don't stop while:
         *      - the counter doesn't reach the max, 
         *      - AND while the max energy isn't achieved 
         *      - AND while there isn't putative convergence. */
        while ((k < kmax) && (e < emax) && (convergenceCounter < convergenceFraction*kmax))
        {
              if (DEBUG)
                System.out.println("Simulated Annealing, Iteration " + k + ". Best: " + ebest + "   Last: " + enew);
            
              /* Obtain random neighbour. */
              snew = getNeighbour(s);
             
              /* Calculate energy. */
              enew = calculateEnergy(fitnessCalculator, snew);

              /* Calculate acceptance probability. */
              pacceptance = calculateAcceptanceProbability(e, enew, k);
              if ((enew > e) || (pacceptance > Math.random()))
              {
                  s = snew;
                  e = enew;
              }
              
              //TODO: make a restart strategy! If (ebest-e) is very large, it means the 
              //current solution it too far apart from the best solution found so far

              /* Count number of repeated consecutive best scores to find convergence. */
              if (enew <= ebest)
                  convergenceCounter++;
              else
                  convergenceCounter = 0;

              /* Found a better solution? Record it. */
              if (enew > ebest)
              {
                  sbest = snew;
                  ebest = enew;
              }

              /* Increment iteration counter. */
              k = k + 1;
        }
        
        if (DEBUG)
        {
            String reason;
            if (k >= kmax)
                reason = "the end of iterations was reached.";
            else if (e >= emax) 
                reason = "maximum fitness reached.";
            else if(convergenceCounter >= 0.1*kmax)
                reason = "convergence reached.";
            else
                reason = "UNKNOWN.";
            
            System.out.println("Terminated because " + reason);
        }

        score = ebest;
        resultingSolution = sbest;

        return sbest;
    }
    
    /** Calculates the probability of acceptance of a solution. Takes into consideration
     ** the current solution's fitness, the new solution's fitness, and the iteration number. */
    private double calculateAcceptanceProbability(double e, double enew, int k)
    {
        assert k >= 0;
        
        return Math.exp(-(e - enew)/(kmax*Math.pow(coolingSchedule, k)));
    }
            

    /** Calculate fitness score for an object using a Fitness Assessor. 
     ** @param fitnesCalculator The class that implements the IFitnessAssessor interface. Responsible for calculating the fitness. 
     ** @param solution The input object to be evaluated by the fitness evaluator. 
     ** @return Fitness score for the object. */
    private double calculateEnergy(IFitnessAssessor fitnessCalculator, EvolvingSolution solution)
    {
        assert fitnessCalculator != null;
        assert solution != null;
        assert !solution.getFeatureList().isEmpty(); //perhaps an "if" instead?
        
        try 
        {
            double fitness = fitnessCalculator.getFitness(solution);
            //assert ((fitness >= 0) && (fitness <= 1)); //fitness constrains ?
            
            return fitness;
        }
        catch(Exception ex)
        {
            System.out.println("An exception occured while calculating the fitness score: " + ex.getMessage());
            return 0;
        }
    }
    
    /** Gets a neighbour object from an input object. */
    private EvolvingSolution getNeighbour(EvolvingSolution solution)
    {
        assert solution != null;
        assert !solution.getFeatureList().isEmpty();
                        
        /* Create a neighbour solution object. */
        EvolvingSolution neighbour = new EvolvingSolution(solution);
        
        /* Calculate a random position. */
        int randomPos = (int) Math.round(Math.random() * (solution.getFeatureList().size()-1));
        
        /* Get feature to be mutated. */
        Feature selectedFeature = neighbour.getFeatureList().get(randomPos);
        
        /* Mutate feature.
         * TODO: all parameters are being changed. Select one randomly?
         * TODO: changes are always completely random, shouldn't they be related to the current annealing temperature? */
        
        double rand1 = Math.random();
        double rand2 = Math.random();
        double max = Math.max(rand1, rand2);
        double min = Math.min(rand1, rand2);
        
        selectedFeature.setB(min);
        selectedFeature.setI(max - min);
        selectedFeature.setO(1 - max);
                
        return neighbour;
    }

    /** Returns the score of the found solution. */
    public synchronized double getScore()
    {
        return score;
    }
    
    /** Returns the found solution. */
    public synchronized EvolvingSolution getSolution()
    {
        return resultingSolution;
    }

    /* The runnable interface. Allows to run this class as a thread. */
    @Override
    public void run()
    {
        System.out.println("Started the Simulated Annealing algorithm.");
        runSimulatedAnnealing();
        System.out.println("Ended the Simulated Annealing algorithm.");
    }
}
