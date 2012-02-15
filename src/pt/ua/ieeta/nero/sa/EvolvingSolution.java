/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.sa;

import pt.ua.ieeta.nero.feature.Feature;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Paulo
 */
public class EvolvingSolution
{
    /* The content of this solution. */
    private List<Feature> featureList;

    /* Constructor. Receives a list of features. */
    public EvolvingSolution(List<Feature> featureList)
    {
        assert featureList != null;
        assert !featureList.isEmpty();
        
        this.featureList = featureList;
    }
    
    /* Copy constructor. Receives an EvolvingSolution, and copies its features. */
    public EvolvingSolution(EvolvingSolution solution)
    {
        assert solution != null;
        assert solution.getFeatureList() != null;
        assert !solution.getFeatureList().isEmpty();
        
        /* Create a new array list. */
        this.featureList = new ArrayList<Feature>();
        
        /* Copy all features from the input solution. */
        for (Feature f : solution.getFeatureList())
                this.featureList.add(f.clone());
    }

    /**
     * Get the list of features in this solution.
     * @return the solution
     */
    public List<Feature> getFeatureList()
    {
        return featureList;
    }

    /**
     * Set the list of features for this solution.
     * @param solution the solution to set
     */
    public void setSolution(List<Feature> solution)
    {
        assert solution != null;
        
        this.featureList = solution;
    }
    
    
}
