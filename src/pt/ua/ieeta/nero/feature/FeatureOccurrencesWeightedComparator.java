/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.feature;

import java.util.Comparator;

/**
 *
 * @author david
 */
public class FeatureOccurrencesWeightedComparator implements Comparator<FeatureOccurrences> {

    private int totalOccurrences;

    public FeatureOccurrencesWeightedComparator(int totalOccurrences) {
        this.totalOccurrences = totalOccurrences;
    }

    @Override
    public int compare(FeatureOccurrences o1, FeatureOccurrences o2) {

        int total1 = o1.getOccurrencesB() + o1.getOccurrencesI() + o1.getOccurrencesO();
        int total2 = o2.getOccurrencesB() + o2.getOccurrencesI() + o2.getOccurrencesO();

        double p1 = o1.getB() * o1.getI() * ((double) total1 / (double) totalOccurrences);
        double p2 = o2.getB() * o2.getI() * ((double) total2 / (double) totalOccurrences);

        if (p1 > p2) {
            return -1;
        }

        if (p1 < p2) {
            return 1;
        }



        if (total1 > total2) {
            return -1;
        }

        if (total1 < total2) {
            return 1;
        }

        return 0;
    }
}
