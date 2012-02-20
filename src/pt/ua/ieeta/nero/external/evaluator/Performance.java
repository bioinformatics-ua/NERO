/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.external.evaluator;

import org.apache.commons.lang.time.StopWatch;

/**
 *
 * @author david
 */
public class Performance {
    private double precision;
    private double recall;
    private double f1;
    private int numIterations;
    private StopWatch time;

    public Performance(){
        this.precision = 0.0;
        this.recall = 0.0;
        this.f1 = 0.0;
        this.numIterations = 0;
        this.time = new StopWatch();
    }
    
    public Performance(double precision, double recall, double f1) {
        super();
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

    public int getNumIterations() {
        return numIterations;
    }

    public void setNumIterations(int numIterations) {
        this.numIterations = numIterations;
    }

    public StopWatch getTime() {
        return time;
    }

    public void setTime(StopWatch time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "Performance{" + "P=" + precision + ", R=" + recall + ", F1=" + f1 + ", Iterations=" + numIterations + ", Time=" + time + '}';
    }
    
    
    
}
