package pt.ua.ieeta.nero.feature;

/**
 *
 * @author Paulo
 */
public class Feature implements Cloneable {

    protected String name;
    protected double B, I, O;

    public Feature(String name, double B, double I, double O) {
        assert name != null;
        assert ((B >= 0) && (B <= 1));
        assert ((I >= 0) && (I <= 1));
        assert ((O >= 0) && (O <= 1));

        this.name = name;
        this.B = B;
        this.I = I;
        this.O = O;
    }

    @Override
    public Feature clone() {
        try {
            return (Feature) super.clone();
        } catch (CloneNotSupportedException ex) {
            System.out.println("An exception occured when trying to clone a Feature: " + ex.getMessage());
            return null;
        }
    }

    public Feature() {
        name = null;
        B = I = O = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        assert name != null;

        this.name = name;
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
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Feature other = (Feature) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}