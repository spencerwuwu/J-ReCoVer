// https://searchcode.com/api/result/9452163/

// $Id: Variable.java,v 1.1 2007/03/26 13:07:24 dmedv Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2;

import ucar.ma2.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;

/**
 * A read-only netcdf variable, implementing ucar.ma2.IOArray.
 * A variable is name, a collection of dimensions and attributes. data access is handled
 * through the IOArray.
 *
 * The only way to create a Variable is in NetcdfFileWriteable.
 * @see NetcdfFileWriteable.addVariable
 * @see ucar.ma2.IOArray
 * @author caron
 * @version $Revision: 1.1 $ $Date: 2007/03/26 13:07:24 $
 */

public class Variable implements ucar.ma2.IOArray {
  private String name;
  protected int[] shape;
  protected Class elementType;
  protected ArrayList dimensions = new ArrayList();
  protected ArrayList attributes = new ArrayList();
  protected boolean isCoordinateVariable = false;

  /**
   * Returns the name of this Variable.
   * @return String which names this Variable.
   */
  public String	getName() { return name; }

  /**
   * Returns an ArrayList containing the dimensions
   * used by this variable. The most slowly varying (leftmost
   * for java and C programmers) dimension is first.
   * For scalar variables, the set has no elements and the iteration
   * is empty.
   * @return ArrayList with objects of type Dimension
   */
  public ArrayList getDimensions() { return dimensions; }

  /** get the ith dimension
   * @param i : which dimension
   * @return ArrayList with objects of type Dimension
   */
  public Dimension getDimension(int i) { return (Dimension) dimensions.get(i); }

  /**
   * Returns the set of attributes associated with this variable,
   * @return Iterator.
   */
  public Iterator getAttributeIterator() {
    return attributes.iterator();
  }
  protected ArrayList getAttributes() { return attributes; }

  /**
   * Convenience function; look up Attribute by name.
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findAttribute(String name) {
    for (int i=0; i<attributes.size(); i++) {
      Attribute a = (Attribute) attributes.get(i);
      if (name.equals(a.getName()))
        return a;
    }
    return null;
  }

  /**
   * Convenience function; look up Attribute by name.
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findAttributeIgnoreCase(String name) {
    for (int i=0; i<attributes.size(); i++) {
      Attribute a = (Attribute) attributes.get(i);
      if (name.equalsIgnoreCase(a.getName()))
        return a;
    }
    return null;
  }

  /**
   * Returns <code>true</code> if and only if this variable can grow.
   * This is equivalent to saying at least one of its dimensions is unlimited.
   * @return boolean <code>true</code> iff this variable can grow
   */
  public boolean isUnlimited() {
    for (int i=0; i<dimensions.size(); i++) {
      Dimension d = (Dimension) dimensions.get(i);
      if (d.isUnlimited()) return true;
    }
   return false;
  }

  /**
   * Returns <code>true</code> if this is a coordinate variable.
   */
  public boolean isCoordinateVariable() { return isCoordinateVariable; }

  //// IOArray methods

  /**
   * Get the type of the underlying data store.
   * @return the Class object of the underlying data store
   */
  public Class getElementType() { return elementType; }

  /**
   * Get the number of dimensions of the array.
   * @return int number of dimensions of the array
   */
  public int getRank() { return shape.length; }

  /**
   * Get the total number of elements in the array.
   * @return int total number of elements in the array
   */
  public long getSize() {
    int[] shape = getShape();
    long size = 1;
    for (int i=0; i<shape.length; i++)
      size *= shape[i];
    return size;
  }

  /**
    * Get the shape: length of array in each dimension.
    *
    * @return int array whose length is the rank of this
    * Array and whose elements represent the
    * length of each of its dimensions.
    */
  public int [] getShape() { return shape; }

  /**
   * Read data from the netcdf file and return a memory resident Array.
   * This Array has the same element type as the IOArray, and the requested shape.
   * Note that this does not do rank reduction, so the returned Array has the same rank
   *  as the Variable. Use Array.reduce() for rank reduction.
   * <p>
   * <code>assert(origin[ii] + shape[ii] <= Variable.shape[ii]); </code>
   * <p>
   * @param origin int array specifying the starting index.
   * @param shape  int array specifying the extents in each
   *	dimension. This becomes the shape of the returned Array.
   * @return the requested data in a memory-resident Array
   */
  public Array read(int [] origin, int [] shape) throws IOException, InvalidRangeException  {
     ucar.multiarray.MultiArray ma = ncvar.copyout(origin, shape);
     Object storage = ma.toArray();
     ArrayAbstract aa = ArrayAbstract.factory( ma.getComponentType(), ma.getLengths(), storage);
     return aa;
  }

  /**
   * Read all the data from the netcdf file for this Variable and return a memory resident Array.
   * This Array has the same element type and shape as the Variable.
   * <p>
   * @return the requested data in a memory-resident Array.
   */
  public Array read() throws IOException {
     Object storage = ncvar.toArray();
     ArrayAbstract aa = ArrayAbstract.factory( ncvar.getComponentType(), ncvar.getLengths(), storage);
     return aa;
  }

  /**
   * Create a new MultiArray that is a slice of this variable; for implementing ucar.ma2.IOArray.
   * @param which_dim: dimension to slice
   * @param index_value: index to fix
   * @return a MultiArray that represents the slice.
   */
  public MultiArray sliceMA(int which_dim, int index_value) {
    return new MultiArrayAdapter(this, which_dim, index_value);
  }

  // dump out this layer of info
  private void getFullName(StringBuffer buf) {
    buf.append(getElementType());
    buf.append(" ");
    buf.append(getName());
    if (getRank() > 0) buf.append("(");
    for (int i=0; i<dimensions.size(); i++) {
      Dimension myd = (Dimension) dimensions.get(i);
      if (i!=0)
        buf.append(", ");
      buf.append( myd.getName() );
    }
    if (getRank() > 0) buf.append(")");
    buf.append(";");
  }

  /*******************************************/

  /** nicely formatted string representation */
  public String toString() {
    buf.setLength(0);
    buf.append("     ");
    getFullName( buf);
    buf.append("\n");

    Iterator iter = getAttributeIterator();
    while (iter.hasNext()) {
      buf.append( "        :");
      Attribute att = (Attribute) iter.next();
      buf.append(att.getName());
      if (att.isString())
        buf.append(" = \""+att.getStringValue()+"\"");
      else if (att.isArray()) {
        buf.append(" = ");
        for (int i=0; i<att.getLength(); i++) {
          if (i > 0) buf.append(", ");
          buf.append(att.getNumericValue(i).toString());
        }
      } else
        buf.append(" = "+att.getNumericValue()); //+" "+att.getNumericValue().getClass().getName());

      buf.append(";");
      if (att.getValueType() != String.class) buf.append(" // "+att.getValueType().getName());
      buf.append("\n");

    }
    return buf.toString();
  }

  /**
   * Instances which have same name are equal.
   */
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if ( !(oo instanceof Variable))
      return false;

    Variable d = (Variable) oo;
    return getName().equals(d.getName());
  }
  /**
   * Override Object.hashCode() to be consistent with equals.
   */
  public int hashCode() {
    return getName().hashCode();
  }

  /////////////////////////////////////////////////////////////////////////////
  //// for subclasses
  protected Variable(String name) {
    this.name = name;
  }

  protected void setIfCoordinateVariable() {

    // is this a coordinate variable ?
    int n = getRank();
    if (n == 1) {
      Dimension firstd = (Dimension) dimensions.get(0);
      if (firstd.getName().equals( getName())) { //  : names match
        firstd.setCoordinateVariable( this);
        this.isCoordinateVariable = true;
      }
    }
    if (n == 2) {    // two dimensional
      Dimension firstd = (Dimension) dimensions.get(0);
      if ((firstd.getName().equals(getName())) &&  // names match
          (getElementType() == char.class)) {         // must be string valued
        firstd.setCoordinateVariable( this);
        this.isCoordinateVariable = true;
      }
    }
  }

  /** should not be public */
  public void addAttribute( ucar.nc2.Attribute att) { attributes.add( att); }
  public void removeAttribute( ucar.nc2.Attribute att) { attributes.remove( att); }
  public void rename( String rename) { this.name = rename; }

  //////////////////////////////////////////////////////////////////////
  // package private - for ucar.netcdf adapter
  private ucar.netcdf.Variable ncvar;
  Variable( ucar.netcdf.Variable ncvar, ArrayList myDims) {
    this.ncvar = ncvar;
    this.name = ncvar.getName();
    this.shape = ncvar.getLengths();
    this.elementType = ncvar.getComponentType();

      // construct the dimensions
    ucar.netcdf.DimensionIterator iter = ncvar.getDimensionIterator();
    while (iter.hasNext()) {
      ucar.netcdf.Dimension d = iter.next();
      String name = d.getName();

        // find it in the myDims array
      for (int i=0; i<myDims.size(); i++) {
        Dimension myd = (Dimension) myDims.get(i);
        if (name.equals(myd.getName())) {
          dimensions.add(myd);
          break;
        }
      }
    }

    // double check got them all
    if (ncvar.getRank() != dimensions.size())
      throw new IllegalStateException("Netcdf file inconsistent");

    // is this a coordinate variable ?
    setIfCoordinateVariable();

    // get attributes
    ucar.netcdf.AttributeIterator aiter = ncvar.getAttributes().iterator();
    while (aiter.hasNext())
      attributes.add(new Attribute(aiter.next()));
  }

  // for NetcdfFileWriteable
  ucar.netcdf.Variable getNetcdfVariable() { return ncvar; }

  // for debugging: dump out directly the underlying netcdf info
  protected StringBuffer buf = new StringBuffer(200);
  private void getFullNameN(StringBuffer buf) {
    buf.append(ncvar.getComponentType());
    buf.append(" ");
    buf.append(ncvar.getName());
    buf.append("(");
    ucar.netcdf.DimensionIterator iter=ncvar.getDimensionIterator();
    while( iter.hasNext()) {
      buf.append( iter.next().getName() );
      if(!iter.hasNext())
        break;
      buf.append(",");
    }
    buf.append(")");
  }

  /** debugging: nicely formatted string representation of underlying ucar.netcdf.Variable. */
  public String toStringN() {
    buf.setLength(0);
    buf.append("    ");
    getFullNameN( buf);

    ucar.netcdf.AttributeIterator iter = ncvar.getAttributes().iterator();
    while (iter.hasNext()) {
      buf.append( "\n                  ");
      iter.next().toCdl(buf);
    }
    buf.append("\n");
    return buf.toString();
  }

}

/* Change History:
   $Log: Variable.java,v $
   Revision 1.1  2007/03/26 13:07:24  dmedv
   no message

   Revision 1.13  2002/02/14 23:41:27  caron
   Instances which have same name are equal

   Revision 1.12  2001/08/10 21:18:29  caron
   add close()

 */

