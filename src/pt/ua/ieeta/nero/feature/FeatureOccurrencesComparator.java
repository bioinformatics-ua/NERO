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
public class FeatureOccurrencesComparator implements Comparator<FeatureOccurrences> {

    @Override
    public int compare(FeatureOccurrences o1, FeatureOccurrences o2) {
        int total1 = o1.getOccurrencesB() + o1.getOccurrencesI() + o1.getOccurrencesO();
        int total2 = o2.getOccurrencesB() + o2.getOccurrencesI() + o2.getOccurrencesO();

        if (total1 > total2) {
            return -1;
        }

        if (total1 < total2) {
            return 1;
        }

        return 0;
    }
}
