// https://searchcode.com/api/result/9452141/

// $Id: Array.java,v 1.1 2007/03/26 13:07:24 dmedv Exp $
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
package ucar.ma2;
import java.io.IOException;

/**
 * Abstraction for memory-resident multidimensional arrays of primitive values.
 * <p>
 * The type and shape and backing storage of an Array are immutable.
 * The data itself can be read or written using an Index or an IndexIterator.
 * Generally this makes use of Arrays thread-safe, except for the possibility of
 * non-atomic read/write on long/doubles. If this is the case, you should probably
 * synchronize your calls. Presumably 64-bit CPUs will make those operations atomic also.
 * </p>
 * @see Index
 * @see IndexIterator
 * @see MAMath
 *
 * @author caron
 * @version $Revision: 1.1 $ $Date: 2007/03/26 13:07:24 $
 */
public interface Array extends MultiArray {
    /**
     * Get the type of the underlying data store.
     * @return the Class object of the underlying data store
     */
    public Class getElementType();

    /**
     * Get the number of dimensions of the array.
     * @return int number of dimensions of the array
     */
    public int getRank();

    /**
     * Get the total number of elements in the array.
     * @return int total number of elements in the array
     */
    public long getSize();

     /**
     * Get the shape: length of array in each dimension.
     *
     * @return int array whose length is the rank of this
     * Array and whose elements represent the
     * length of each of its dimensions.
     */
    public int [] getShape();

    /** Get an Index object used for indexed access of this Array.
     * Use this for set() convenience methods or for
     * multiple independent array traversals.
     * @see Index
     */
    public Index getIndex();

    /** Get an index iterator for traversing the array in canonical order.
     * @see IndexIterator
     */
    public IndexIterator getIndexIterator();

    /** Get an index iterator for traversing a section of the array in canonical order.
     * This is equivalent to Array.section(ranges).getIterator();
     * @param ranges array of Ranges that specify the array subset.
     *   Must be same rank as original Array.
     *   A particular Range: 1) may be a subset, or 2) may be null, meaning use entire Range.
     *
     * @return an IndexIterator over the named range.
     */
    public IndexIterator getRangeIterator(Range[] ranges) throws InvalidRangeException;

    /** Get a fast index iterator for traversing the array in arbitrary order.
     * @see IndexIterator
     */
    public IndexIterator getIndexIteratorFast();

   /**
    * Create a new Array, with the same shape as the original Array, and a copy of the backing
    * data store.  When copying the data the physical order becomes the same as the
    * logical order (this is transparent except for efficency).
    * @return the new Array
    */
    public Array copy();

   /**
    * Create a new Array as a subsection of this Array, with rank reduction.
    * No data is moved, so the new Array references the same backing store as the original.
    * @param ranges array of Ranges that specify the array subset.
    *   Must be same rank as original Array.
    *   A particular Range: 1) may be a subset, or 2) may be null, meaning use entire Range.
    *   If Range[dim].length == 1, then the rank of the resulting Array is reduced at that dimension.
    * @return the new Array
    */
    public Array section( Range[] ranges) throws InvalidRangeException;

    /*
    * Create a new Array as a subsection of this Array, making a copy of the data.
    * The data is placed into "canonical order" (last index varies fastest) in the new Array.
    * @param ranges array of Ranges that specify the array subset.
    *   Must be same rank as original Array.
    *   A particular Range: 1) may be a subset, or 2) may be null, meaning use entire Range.
    *   If Range[dim].length == 1, then the rank of the resulting Array is reduced at that dimension.
    * @return the new Array
    *
    public Array sectionCopy( Range[] ranges) throws InvalidRangeException; */

   /**
    * Create a new Array as a subsection of this Array, without rank reduction.
    * No data is moved, so the new Array references the same backing store as the original.
    * @param ranges array of Ranges that specify the array subset.
    *   Must be same rank as original Array.
    *   A particular Range: 1) may be a subset, or 2) may be null, meaning use entire Range.
    * @return the new Array
    */
    public Array sectionNoReduce( Range[] ranges) throws InvalidRangeException;

    /**
     * Create a new Array using same backing store as this Array, by
     * fixing the specified dimension at the specified index value. reduces rank by 1.
     * @param which_dim which dimension to fix
     * @param index_value at whay index value
     * @return the new Array
     */
    public Array slice(int which_dim, int index_value);

    /**
     * Create a new Array using same backing store as this Array, by
     * flipping the given dimension so that it runs from shape[dim]-1 to 0.
     * @param dim dimension to flip
     * @return the new Array
     * @exception IllegalArgumentException dim not valid
     */
    public Array flip( int dim);

    /**
     * Create a new Array using same backing store as this Array, by
     * transposing two of the indices.
     * @param dim1, dim2 transpose these two indices
     * @return the new Array
     * @exception IllegalArgumentException dim1 or dim2 not valid
     */
    public Array transpose( int dim1, int dim2);

    /**
     * Create a new Array using same backing store as this Array, by
     * permuting the indices.
     * @param dims: the old index dims[k] becomes the new kth index.
     * @return the new Array
     * @exception IllegalArgumentException: wrong rank or dim[k] not valid
     */
    public Array permute( int[] dims);

    /**
     * Create a new Array by copying this Array to a new one with given shape.
     * @param shape the new shape
     * @return the new Array, will be in canonical order
     * @exception IllegalArgumentException shape not conformable to this array.
     */
    public Array reshape( int [] shape);

    /**
     * Create a new Array using same backing store as this Array, by
     * eliminating any dimensions with length one.
     * @return the new Array
     */
    public Array reduce();

    /**
     * Create a new Array using same backing store as this Array, by
     * eliminating the specified dimension; must have length one, else IllgalArgumentException.
     * @return the new Array
     */
    public Array reduce(int dim);


    ///////////////////////////////////////////////////
    /* these are the type-specific element accessors */
    ///////////////////////////////////////////////////

    /** Get the array element at the current element of ima, as a double.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to double if necessary.
     */
    public double getDouble(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param double value the new value; cast to underlying data type if necessary.
     */
    public void setDouble(Index ima, double value);

    /** Get the array element at the current element of ima, as a float.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to float if necessary.
     */
    public float getFloat(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param float value the new value; cast to underlying data type if necessary.
     */
    public void setFloat(Index ima, float value);

    /** Get the array element at the current element of ima, as a long.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to long if necessary.
     */
    public long getLong(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param float value the new value; cast to underlying data type if necessary.
     */
    public void setLong(Index ima, long value);

    /** Get the array element at the current element of ima, as a int.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to int if necessary.
     */
    public int getInt(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param float value the new value; cast to underlying data type if necessary.
     */
    public void setInt(Index ima, int value);

    /** Get the array element at the current element of ima, as a short.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to short if necessary.
     */
    public short getShort(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param float value the new value; cast to underlying data type if necessary.
     */
    public void setShort(Index ima, short value);

    /** Get the array element at the current element of ima, as a byte.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to float if necessary.
     */
    public byte getByte(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param byte value the new value; cast to underlying data type if necessary.
     */
    public void setByte(Index ima, byte value);

    /** Get the array element at the current element of ima, as a char.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to char if necessary.
     */
    public char getChar(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param float value the new value; cast to underlying data type if necessary.
     */
    public void setChar(Index ima, char value);

    /** Get the array element at the current element of ima, as a boolean.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to boolean if necessary.
     * @exception runtime ForbiddenConversionException if underlying array not boolean
     */
    public boolean getBoolean(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param float value the new value; cast to underlying data type if necessary.
     * @exception runtime ForbiddenConversionException if underlying array not boolean
     */
    public void setBoolean(Index ima, boolean value);

    /**
     * Get the array element at index as an Object.
     * The returned value is wrapped in an object, eg Double for double
     * @param index element index
     * @return Object value at <code>index</code>
     * @exception ArrayIndexOutOfBoundsException if index incorrect rank or out of bounds
     */
    public Object getObject(Index ima);

    /**
     * Set the array element at index to the specified value.
     * the value must be passed wrapped in the appropriate Object (eg Double for double)
     * @param ima Index with current element set
     * @param value the new value.
     * @exception ArrayIndexOutOfBoundsException if index incorrect rank or out of bounds
     * @exception ClassCastException if Object is incorrect type
     */
    public void setObject(Index ima, Object value);

   /**
    * Copy this array to a 1D Java primitive array of type getElementType(), with the physical order
    * of the result the same as logical order.
    * @return a Java 1-dimensional array of type getElementType().
    */
    public Object copyTo1DJavaArray();

   /**
    * Copy this array to a n-Dimensional Java primitive array of type getElementType()
    * and rank getRank(). Makes a copy of the data.
    * @return a Java N-dimensional array of type getElementType().
    */
    public Object copyToNDJavaArray();


    /** This is present so that Array is-a MultiArray:
     *  equivalent to sectionNoReduce().
     */
    public Array read(int [] origin, int [] shape) throws InvalidRangeException;

    /** This is present so that Array is-a MultiArray: just returns itself. */
    public Array read();

      // this is irritating that the corresponding method that returns Array cant be used.
    public MultiArray sectionMA( Range[] ranges) throws InvalidRangeException;
    public MultiArray flipMA( int dim);
    public MultiArray reduceMA();
    public MultiArray sliceMA(int which_dim, int index_value);
    public MultiArray transposeMA( int dim1, int dim2);
}

