/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.feature;

/**
 *
 * @author david
 */
public class FeatureOccurrences extends Feature {

    private int occurrencesB, occurrencesI, occurrencesO;

    public FeatureOccurrences(String name) {
        super(name, 0.0, 0.0, 0.0);
        this.occurrencesB = 0;
        this.occurrencesI = 0;
        this.occurrencesO = 0;
    }

    public void incrementOccurrencesB() {
        this.occurrencesB++;
    }

    public void incrementOccurrencesI() {
        this.occurrencesI++;
    }

    public void incrementOccurrencesO() {
        this.occurrencesO++;
    }

    public int getOccurrencesB() {
        return occurrencesB;
    }

    public int getOccurrencesI() {
        return occurrencesI;
    }

    public int getOccurrencesO() {
        return occurrencesO;
    }

    public void setProbabilities() {
        int total = occurrencesB + occurrencesI + occurrencesO;

        B = (double) occurrencesB / (double) total;
        I = (double) occurrencesI / (double) total;
        O = (double) occurrencesO / (double) total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("\t");
        sb.append("B:");
        sb.append(getB());
        sb.append("\t");
        sb.append("I:");
        sb.append(getI());
        sb.append("\t");
        sb.append("O:");
        sb.append(getO());
        sb.append("\t");
        sb.append("TOTAL:");
        sb.append(getOccurrencesB() + getOccurrencesI() + getOccurrencesO());

        return sb.toString();
    }
}
