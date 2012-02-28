package pt.ua.ieeta.nero.feaure.targets;

/**
 *
 * @author Paulo
 */
public class BIOFeature extends IOptimizationTarget
{
    protected double B, I, O;

    public BIOFeature(String name, double B, double I, double O) {
        assert name != null;
        assert ((B >= 0) && (B <= 1));
        assert ((I >= 0) && (I <= 1));
        assert ((O >= 0) && (O <= 1));

        this.name = name;
        this.B = B;
        this.I = I;
        this.O = O;
    }

    public BIOFeature() {
        name = null;
        B = I = O = 0;
    }

    /**
     * @return the B
     */
    public double getB() {
        return B;
    }

    /**
     * @param B the B to set
     */
    public void setB(double B) {
        this.B = B;
    }

    /**
     * @return the I
     */
    public double getI() {
        return I;
    }

    /**
     * @param I the I to set
     */
    public void setI(double I) {
        this.I = I;
    }

    /**
     * @return the O
     */
    public double getO() {
        return O;
    }

    /**
     * @param O the O to set
     */
    public void setO(double O) {
        this.O = O;
    }

    @Override
    public String toString() {
        return name + "\t" + "B: " + B + "\t" + "I: " + I + "\t" + "O: " + O;
    }

    @Override
    public void performMutation(int k, int kmax, double dispersionFactor)
    {
        assert k >= 0;
        assert kmax > 0;
        assert kmax >= k;
        assert dispersionFactor > 0;
        
        /*   0----------BI-----------IO--------1    */
        double BIpos = getB();
        double IOpos = getB() + getI();

        /* Mutate B, I and O values by adding or subtracting a random value that initially varies
        * between -dispersionFactor and +dispersionFactor. That interval shrinks with passing iterations. */
        double factor1 = (Math.random() - 0.5) * 2 * ((kmax-k)/(double)kmax) * dispersionFactor;
        double factor2 = (Math.random() - 0.5) * 2 * ((kmax-k)/(double)kmax) * dispersionFactor;
        double newBIpos = Math.min(1, Math.max(0 , BIpos + factor1));
        double newIOpos = Math.min(1, Math.max(0 , IOpos + factor2));

        setB(newBIpos);
        setI(newBIpos>newIOpos? 0 : newIOpos-newBIpos);
        setO(1 - (newBIpos>newIOpos? newBIpos : newIOpos));

        assert ((getB() >= 0.0) && (getB() <= 1.0));
        assert ((getI() >= 0.0) && (getI() <= 1.0));
        assert ((getO() >= 0.0) && (getO() <= 1.0));
        assert getB() + getI() + getO() <= 1.0;
    }
}
