/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.external.evaluator;

/**
 *
 * @author david
 */
public class Performance {
    private double precision;
    private double recall;
    private double f1;

    public Performance(double precision, double recall, double f1) {
        this.precision = precision;
        this.recall = recall;
        this.f1 = f1;
    }

    public double getF1() {
        return f1;
    }

    public void setF1(double f1) {
        this.f1 = f1;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    @Override
    public String toString() {
        return "Performance{" + "Precision=" + precision + ", Recall=" + recall + ", F1=" + f1 + '}';
    }
    
}
