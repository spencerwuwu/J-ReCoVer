// https://searchcode.com/api/result/1973467/

/**
 * Copyright (c) 2006-2008 MiniMe. Code released under The MIT/X Window System
 * License. Full license text can be found in license.txt
 */

package minime.core;

/**
 * A utility class that holds width and height information of a Drawable
 * Note the constructor and set methods do NOT prevent you from setting negative values.
 * 
 * @author Liang
 */
public class Dimension {
    
	/**
	 * This information is retrieved frequently make it public to reduce
	 * overhead
	 */
	public int width;
	public int height;

    /**
     * Creates an instance of Dimension with a width of zero and a height of zero.
     */
    public Dimension() {
    	this(0, 0);
    }

    /**
     * Creates an instance of Dimension whose width and height are the same as for the specified dimension.
     * 
     * @param d the specified dimension for the width and height values
     */
    public Dimension(Dimension d) {
    	this(d.width, d.height);
    }

    /**
     * Creates a new instance of Dimension with specified width and height
     * 
     * @param width the specified width
     * @param height the specified height
     */
    public Dimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Set the width of the dimension
     * 
     * @param width the dimension width
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Set the height of the dimension
     * 
     * @param height the dimension height
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Returns the width of the dimension
     * 
     * @return width of the dimension
     */
    public int getWidth() {
        return width;
    }

   /**
    * Return the height of the dimension
    * 
    * @return height of the dimension
    */
    public int getHeight() {
        return height;
    }
    
    /**
     * Gets the size of this Dimension object.
     *
     * @return   the size of this dimension, a new instance of 
     *           Dimension with the same width and height
     */
    public Dimension getSize() 
    {
    	return new Dimension(width, height);
    }	

    /**
     * Sets the size of this Dimension object to the specified size.
     * 
     * @param d  the new size for this Dimension object
     */
    public void setSize(Dimension d) {
    	setSize(d.width, d.height);
    }	    
    /**
     * Sets the size of this Dimension object 
     * to the specified width and height.
     *
     * @param width   the new width
     * @param height  the new height
     */
    public void setSize(int width, int height) {
    	this.width = width;
    	this.height = height;
    }

    /**
     * Returns a string representation 
     */
    public String toString() {
        return getClass().getName() + "[width=" + width + ",height=" + height + "]";
    }

    /**
     * Returns the hash code for this Dimension.
     */
    public int hashCode() {
        int sum = width + height;
        return sum * (sum + 1)/2 + width;
    }

    /**
     * Checks whether two dimension objects have equal values.
     */
    public boolean equals(Object obj) {
    	if (obj == null)
    		return false;
    	
    	if (obj instanceof Dimension) {
    	    Dimension d = (Dimension)obj;
    	    return (width == d.width) && (height == d.height);
    	}
    	return false;
    }
    
    
}
