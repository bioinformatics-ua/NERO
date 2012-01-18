package pt.ua.ieeta.nero;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Paulo
 */
public class Nero {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(Nero.class);

    public Nero() {
    }

    /**
     * Main thread entry point.
     */
    public static void main(String[] args) 
    {
        String trainFile = "resources" + File.separator + "corpus" + File.separator + "train_1k.gz";
        String testFile = "resources" + File.separator + "corpus" + File.separator + "test_500.gz";
        String unlabeledFile = "resources" + File.separator + "corpus" + File.separator + "unlabeled_1k.gz";
        String configFile = "config" + File.separator + "bc_semi.config";
        
        try {
            System.setErr(new PrintStream(new File("tmp")));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        
        System.out.println("Starting the NERO experiment.");

        /*
         * Create seed and fitness function.
         */
        EvolvingSolution seed = createBaseSolution();
        IFitnessAssessor fitnessClass = null;
        try {
            fitnessClass = new CRFFitnessAssessor(configFile, trainFile, testFile, unlabeledFile); // new BogusFitnessAssessor(); //
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        /*
            * Instantiate the simulated annealing algorithm.
            */
        SimulatedAnnealing sa = new SimulatedAnnealing(fitnessClass, seed, 100, 0.9, 0.15, 0.25);

        try {
            Thread saThread = new Thread(sa);
            saThread.start();
            saThread.join();
        } catch (InterruptedException ex) {
            System.out.println("An exception occured while starting or running the simulated annealing thread: " + ex.getMessage());
        }

        System.out.println("Ended the NERO experiment.");
    }

    /**
     * *******************************************************************
     */
    /*
     * Get or Create a base solution to the experiment.
     */
    private static EvolvingSolution createBaseSolution() 
    {
        /*
         * New feature list.
         */
        ArrayList<Feature> featureList = new ArrayList<Feature>();

        featureList.add(new Feature("InitCap", 0.2379794528049769, 0.0829554664738998, 0.6790650807211233));
        featureList.add(new Feature("AllCaps", 0.2557663587587809, 0.12893412246252559, 0.6152995187786935));
        featureList.add(new Feature("EndCap", 0.28641242292760904, 0.1423612696962777, 0.5712263073761132));
        featureList.add(new Feature("Lowercase", 0.01838204823604166, 0.045936127303367685, 0.9356818244605907));
        featureList.add(new Feature("SingleCap", 0.14242852708900922, 0.07449046453081354, 0.7830810083801772));
        featureList.add(new Feature("TwoCap", 0.4085733422638982, 0.12659075686537175, 0.46483590087073007));
        featureList.add(new Feature("ThreeCap", 0.3803059273422562, 0.13288718929254303, 0.4868068833652008));
        featureList.add(new Feature("MoreCap", 0.3703907539900936, 0.05888827738029719, 0.5707209686296092));
        featureList.add(new Feature("SingleDigit", 0.22188284802823594, 0.1869951808864454, 0.5911219710853187));
        featureList.add(new Feature("TwoDigit", 0.16693591814754982, 0.05236941303177167, 0.7806946688206785));
        featureList.add(new Feature("ThreeDigit", 0.10176492677431469, 0.04919263987983477, 0.8490424333458505));
        featureList.add(new Feature("MoreDigit", 0.047979797979797977, 0.02904040404040404, 0.922979797979798));
        featureList.add(new Feature("LENGTH=1", 0.00943056026051513, 0.10208608801127733, 0.8884833517282076));
        featureList.add(new Feature("LENGTH=2", 0.02512904102146156, 0.0200013583265417, 0.9548696006519968));
        featureList.add(new Feature("LENGTH=3-5", 0.08749876799672476, 0.04899203178189372, 0.8635092002213816));
        featureList.add(new Feature("LENGTH=6+", 0.03034191888010988, 0.06631658538954056, 0.9033414957303495));
        featureList.add(new Feature("MixCase", 0.18628553698173214, 0.05810029864639491, 0.7556141643718729));
        
        /*
         * Generate several ficticious features. They are randomly generated.
         */
        /*
         * for (int i = 0; i < 20; i++) { featureList.add(new Feature("Bogus feature " + i, Math.random(),
         * Math.random(), Math.random())); }
         *
         * assert featureList.size() == 20;
         */

        return new EvolvingSolution(featureList);
    }

    /**
     * A ficticious class to implement the fitness assessor. The fitness method * returns a sum of all features.
     */
    private static class BogusFitnessAssessor implements IFitnessAssessor 
    {
        @Override
        public double getFitness(EvolvingSolution solution) 
        {
            double sum = 0;
            for (Feature f : solution.getFeatureList()) 
            {
//                double old = sum;
                sum += (f.getB() + f.getI() - f.getO());
//                System.out.println("sum = " + (sum-old));
            }
            return sum;
        }
    }
}
