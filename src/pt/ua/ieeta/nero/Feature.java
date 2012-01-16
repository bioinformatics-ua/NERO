/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero;

/**
 *
 * @author Paulo
 */
public class Feature
{
    private String name;
    private double B, I, O;
    
    public Feature(String name, double B, double I, double O)
    {
        assert name != null;
        assert ((B >= 0) && (B <= 1));
        assert ((I >= 0) && (I <= 1));
        assert ((O >= 0) && (O <= 1));
        
        this.name = name;
        this.B = B;
        this.I = I;
        this.O = O;
    }
    
    public Feature()
    {
        name = null;
        B = I = O = 0;
    }

    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        assert name != null;
        
        this.name = name;
    }
    
    /**
     * @return the B
     */
    public double getB()
    {
        return B;
    }

    /**
     * @param B the B to set
     */
    public void setB(double B)
    {
        this.B = B;
    }

    /**
     * @return the I
     */
    public double getI()
    {
        return I;
    }

    /**
     * @param I the I to set
     */
    public void setI(double I)
    {
        this.I = I;
    }

    /**
     * @return the O
     */
    public double getO()
    {
        return O;
    }

    /**
     * @param O the O to set
     */
    public void setO(double O)
    {
        this.O = O;
    }
    
    
}
