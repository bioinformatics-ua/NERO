
package pt.ua.ieeta.nero.sa;

import pt.ua.ieeta.nero.external.evaluator.Performance;

/**
 *
 * @author Paulo
 */
public interface IFitnessAssessor
{
    public Performance getFitness(EvolvingSolution solution);
}
