// https://searchcode.com/api/result/9452138/

// $Id: IndexImpl.java,v 1.1 2007/03/26 13:07:24 dmedv Exp $
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

/**
 * Superclass for our implementations of IndexArray.
 *
 * @see Index
 * @author caron
 * @version $Revision: 1.1 $ $Date: 2007/03/26 13:07:24 $
 */
public class IndexImpl implements Index, Cloneable {

  /** array shape */
  protected int [] shape;
  /** array rank */
  protected int rank;
  /** total number of elements */
  protected long size;
  /** array stride */
  protected int [] stride;
  /** element = offset + stride[0]*current[0] + ... */
  protected int offset;

  /** current element's index */
  protected int [] current;

  /** Iterator implementation */
  protected boolean fastIterator = true;

  /** constructor for subclasses only */
  protected IndexImpl(int rank) {
    shape = new int[rank];
    this.rank = rank;
    current = new int[rank];
    stride = new int[rank];
  }

  /** constructor for subclasses only. */
  protected IndexImpl( int[] shape) {
    this.shape = (int []) shape.clone();
    rank = shape.length;
    current = new int[rank];
    stride = new int[rank];
    size = computeStrides( shape, stride);
    offset = 0;
  }

    /** subclass specialization/optimization calculations */
  protected void precalc(){}

  /** create a new IndexImpl based on current one, except
    * flip the index so that it runs from shape[index]-1 to 0.
    * @param index dimension to flip
    */
  IndexImpl flip( int index) {
    if ((index <0) || (index >= rank))
      throw new IllegalArgumentException();

    IndexImpl i = (IndexImpl) this.clone();
    i.offset += stride[index] * (shape[index]-1);
    i.stride[index] = -stride[index];

    i.fastIterator = false;
    i.precalc(); // any subclass-specific optimizations
    return i;
  }

  /** create a new IndexImpl based on a subsection of this one, with rank reduction if
   * dimension length == 1.
   * @param ranges array of Ranges that specify the array subset.
   *   Must be same rank as original Array.
   *   A particular Range: 1) may be a subset; 2) may be null, meaning use entire Range.
   * @return new IndexImpl, with same or smaller rank as original.
   */
  IndexImpl section( Range[] ranges) throws InvalidRangeException {

    // check ranges are valid
    if (ranges.length != rank)
      throw new InvalidRangeException("Bad ranges [] length");
    for (int ii=0; ii<rank; ii++) {
      if (ranges[ii] == null)
        continue;
      if ((ranges[ii].first() < 0) || (ranges[ii].first() >= shape[ii]))
        throw new InvalidRangeException("Bad range starting value at index "+ii+" == "+ranges[ii].first());
      if ((ranges[ii].last() < 0) || (ranges[ii].last() >= shape[ii]))
        throw new InvalidRangeException("Bad range ending value at index "+ii+" == "+ranges[ii].last());
    }

    int reducedRank = rank;
    for (int ii=0; ii< ranges.length; ii++) {
      if ((ranges[ii] != null) && (ranges[ii].length() == 1))
        reducedRank--;
    }
    IndexImpl newindex = IndexImpl.factory( reducedRank);
    newindex.offset = offset;

    // calc shape, size, and index transformations
    // calc strides into original (backing) store
    int newDim = 0;
    for (int ii=0; ii<rank; ii++) {
      if (ranges[ii] == null) {          // null range means use the whole original dimension
        newindex.shape[newDim] = shape[ii];
        newindex.stride[newDim] = stride[ii];
        newDim++;
      } else if (ranges[ii].length() != 1) {
        newindex.shape[newDim] = ranges[ii].length();
        newindex.stride[newDim] = stride[ii] * ranges[ii].step();
        newindex.offset += stride[ii] * ranges[ii].first();
        newDim++;
      } else {
        newindex.offset += stride[ii] * ranges[ii].first();   // constant due to rank reduction
      }
    }
    newindex.size = computeSize( newindex.shape);
    newindex.fastIterator = false;
    newindex.precalc(); // any subclass-specific optimizations
    return newindex;
  }

  /** create a new IndexImpl based on a subsection of this one, without rank reduction.
   * @param ranges array of Ranges that specify the array subset.
   *   Must be same rank as original Array.
   *   A particular Range: 1) may be a subset; 2) may be null, meaning use entire Range.
   * @return new IndexImpl, with same rank as original.
   */
  IndexImpl sectionNoReduce( Range[] ranges) throws InvalidRangeException {

    // check ranges are valid
    if (ranges.length != rank)
      throw new InvalidRangeException("Bad ranges [] length");
    for (int ii=0; ii<rank; ii++) {
      if (ranges[ii] == null)
        continue;
      if ((ranges[ii].first() < 0) || (ranges[ii].first() >= shape[ii]))
        throw new InvalidRangeException("Bad range starting value at index "+ii+" == "+ranges[ii].first());
      if ((ranges[ii].last() < 0) || (ranges[ii].last() >= shape[ii]))
        throw new InvalidRangeException("Bad range ending value at index "+ii+" == "+ranges[ii].last());
    }

      // allocate
    IndexImpl newindex = IndexImpl.factory( rank);
    newindex.offset = offset;

    // calc shape, size, and index transformations
    // calc strides into original (backing) store
    for (int ii=0; ii<rank; ii++) {
      if (ranges[ii] == null) {          // null range means use the whole original dimension
        newindex.shape[ii] = shape[ii];
        newindex.stride[ii] = stride[ii];
      } else {
        newindex.shape[ii] = ranges[ii].length();
        newindex.stride[ii] = stride[ii] * ranges[ii].step();
        newindex.offset += stride[ii] * ranges[ii].first();
      }
    }
    newindex.size = computeSize( newindex.shape);
    newindex.fastIterator = false;
    newindex.precalc(); // any subclass-specific optimizations
    return newindex;
  }

    /**
     * Create a new IndexImpl based on current one by
     * eliminating any dimensions with length one.
     * @return the new IndexImpl
     */
   IndexImpl reduce() {
     IndexImpl c = this;
     for (int ii=0; ii< rank; ii++)
       if (shape[ii] == 1) {  // do this on the first one you find
         IndexImpl newc = c.reduce(ii);
         return newc.reduce();  // any more to do?
       }
     return c;
   }

   /**
     * Create a new IndexImpl based on current one by
     * eliminating the specified dimension;
     * @param dim: dimension to eliminate: must be of length one, else IllegalArgumentException
     * @return the new IndexImpl
     */
   IndexImpl reduce(int dim) {
     if ((dim < 0) || (dim >= rank))
       throw new IllegalArgumentException("illegal reduce dim "+dim);
     if (shape[dim] != 1)
       throw new IllegalArgumentException("illegal reduce dim "+dim+ " : length != 1");

     IndexImpl newindex = IndexImpl.factory( rank-1);
     newindex.offset = offset;
     int count = 0;
     for (int ii=0; ii< rank; ii++) {
       if (ii != dim) {
         newindex.shape[count] = shape[ii];
         newindex.stride[count] = stride[ii];
         count++;
       }
     }
     newindex.size = computeSize( newindex.shape);
     newindex.fastIterator = fastIterator;
     newindex.precalc();         // any subclass-specific optimizations
     return newindex;
  }

  /** create a new IndexImpl based on current one, except
    * transpose two of the indices.
    * @param index1, index2 transpose these two indices
    */
  IndexImpl transpose( int index1, int index2) {
    if ((index1 <0) || (index1 >= rank))
      throw new IllegalArgumentException();
    if ((index2 <0) || (index2 >= rank))
      throw new IllegalArgumentException();

    IndexImpl newIndex = (IndexImpl) this.clone();
    newIndex.stride[index1] = stride[index2];
    newIndex.stride[index2] = stride[index1];
    newIndex.shape[index1] = shape[index2];
    newIndex.shape[index2] = shape[index1];

    newIndex.fastIterator = false;
    newIndex.precalc(); // any subclass-specific optimizations
    return newIndex;
  }

  /** create a new IndexImpl based on a permutation of the current indices
    * @param dims: the old index dim[k] becomes the new kth index.
    */
  IndexImpl permute( int[] dims) {
    if (dims.length != shape.length)
      throw new IllegalArgumentException();
    for (int i=0; i<dims.length; i++)
      if ((dims[i] < 0) || (dims[i] >= rank))
        throw new IllegalArgumentException();

    IndexImpl newIndex = (IndexImpl) this.clone();
    for (int i=0; i<dims.length; i++) {
      newIndex.stride[i] = stride[dims[i]];
      newIndex.shape[i] = shape[dims[i]];
    }

    newIndex.fastIterator = false;
    newIndex.precalc(); // any subclass-specific optimizations
    return newIndex;
  }


  /** Get the number of dimensions in the array. */
  public int getRank() { return rank; }

  /** Get the shape: length of array in each dimension. */
  public int [] getShape() { return (int []) shape.clone(); }

  /** Get the current element's index as an int []
  public int [] getCurrentIndex() { return (int []) current.clone(); } */

  /** Get an index iterator for traversing the array in canonical order.
   * @see IndexIterator
   */
  IndexIterator getIndexIterator(ArrayAbstract maa) {
     if (fastIterator)
      return new IteratorFast(maa);
    else
      return new IteratorImpl(maa);
  }

  IteratorFast getIndexIteratorFast(ArrayAbstract maa) {
    return new IteratorFast(maa);
  }

  /** Get the total number of elements in the array. */
  public long getSize() { return size; }

  /** Get the current element's index into the 1D backing array. */
  public int currentElement() {
    int value = offset;                 // NB: dont have to check each index again
    for(int ii = 0; ii < rank; ii++)    // general rank
      value += current[ii] * stride[ii];
    return value;
  }

  /** Use index[] to calculate the index into the 1D backing array.
   * Does not set the current element.
   *
  public int element(int [] index) {
    int value = offset;
    for(int ii = 0; ii < rank; ii++) {
      final int thisIndex = index[ii];
      if( thisIndex < 0 || thisIndex >= shape[ii])  // check each index
        throw new ArrayIndexOutOfBoundsException();
      value += thisIndex * stride[ii];
    }
    return value;
  } */

  /** Increment the current element by 1. Used by IndexIterator.
   * General rank, with subclass specialization.
   */
  protected int incr() {
    int digit = rank-1;
    while (digit >= 0) {
      current[digit]++;
      if (current[digit] < shape[digit])
        break;                        // normal exit
      current[digit] = 0;               // else, carry
      digit--;
    }
    return currentElement();
  }

  /** Set the current element's index. General-rank case.
   * @return this, so you can use A.get(i.set(i))
   * @exception runtime ArrayIndexOutOfBoundsException if index.length != rank.
   */
  public Index set(int[] index){
    if (index.length != rank)
      throw new ArrayIndexOutOfBoundsException();

    for(int ii = 0; ii < rank; ii++)
      current[ii] = index[ii];
    return this;
  }


  /** set current element at dimension dim to v */
  public void setDim(int dim, int value) {
    if (value < 0 || value >= shape[dim])  // check index here
      throw new ArrayIndexOutOfBoundsException();
    current[dim] = value;
  }

  /** set current element at dimension 0 to v
    @return this, so you can use A.get(i.set(i)) */
  public Index set0(int v) {
    setDim(0, v);
    return this;
  }
  /** set current element at dimension 1 to v
    @return this, so you can use A.get(i.set(i)) */
  public Index set1(int v) {
    setDim(1, v);
    return this;
  }
  /** set current element at dimension 2 to v
    @return this, so you can use A.get(i.set(i)) */
  public Index set2(int v) {
    setDim(2, v);
    return this;
  }
  /** set current element at dimension 3 to v
    @return this, so you can use A.get(i.set(i)) */
  public Index set3(int v) {
    setDim(3, v);
    return this;
  }
  /** set current element at dimension 4 to v
    @return this, so you can use A.get(i.set(i)) */
  public Index set4(int v) {
    setDim(4, v);
    return this;
  }
  /** set current element at dimension 5 to v
    @return this, so you can use A.get(i.set(i)) */
  public Index set5(int v) {
    setDim(5, v);
    return this;
  }
  /** set current element at dimension 6 to v
    @return this, so you can use A.get(i.set(i)) */
  public Index set6(int v) {
    setDim(6, v);
    return this;
  }

  /** set current element at dimension 0 to v0
    @return this, so you can use A.get(i.set(i)) */
  public Index set(int v0) {
    setDim(0, v0);
    return this;
  }

  /** set current element at dimension 0,1 to v0,v1
    @return this, so you can use A.get(i.set(i,j)) */
  public Index set(int v0, int v1) {
    setDim(0, v0);
    setDim(1, v1);
    return this;
  }

  /** set current element at dimension 0,1,2 to v0,v1,v2
    @return this, so you can use A.get(i.set(i,j,k)) */
  public Index set(int v0, int v1, int v2) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    return this;
  }

  /** set current element at dimension 0,1,2,3 to v0,v1,v2,v3
    @return this, so you can use A.get(i.set(i,j,k,l)) */
  public Index set(int v0, int v1, int v2, int v3) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    setDim(3, v3);
    return this;
  }

  /** set current element at dimension 0,1,2,3,4 to v0,v1,v2,v3,v4
    @return this, so you can use A.get(i.set(i,j,k,l,m)) */
  public Index set(int v0, int v1, int v2, int v3, int v4) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    setDim(3, v3);
    setDim(4, v4);
    return this;
  }

  /** set current element at dimension 0,1,2,3,4,5 to v0,v1,v2,v3,v4,v5
    @return this, so you can use A.get(i.set(i,j,k,l,m,n)) */
  public Index set(int v0, int v1, int v2, int v3, int v4, int v5) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    setDim(3, v3);
    setDim(4, v4);
    setDim(5, v5);
    return this;
  }

  /** set current element at dimension 0,1,2,3,4,5,6 to v0,v1,v2,v3,v4,v5,v6
    @return this, so you can use A.get(i.set(i,j,k,l,m,n,p)) */
  public Index set(int v0, int v1, int v2, int v3, int v4, int v5, int v6) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    setDim(3, v3);
    setDim(4, v4);
    setDim(5, v5);
    setDim(6, v6);
    return this;
  }

  private StringBuffer sbuff = new StringBuffer(100);
  /** String representation */
  public String toString() {
    sbuff.setLength(0);

    sbuff.append(" shape= ");
    for(int ii = 0; ii < rank; ii++) {
      sbuff.append(shape[ii]);
      sbuff.append(" ");
    }

    sbuff.append(" stride= ");
    for(int ii = 0; ii < rank; ii++) {
      sbuff.append(stride[ii]);
      sbuff.append(" ");
    }

    sbuff.append(" offset= "+ offset);
    sbuff.append(" rank= "+ rank);
    sbuff.append(" size= "+ size);

    sbuff.append(" current= ");
    for(int ii = 0; ii < rank; ii++) {
      sbuff.append(current[ii]);
      sbuff.append(" ");
    }

    return sbuff.toString();
  }

  public Object clone() {
    IndexImpl i;
    try {
      i = (IndexImpl) super.clone();
    } catch(CloneNotSupportedException e) {
      return null;
    }
    i.stride = (int []) stride.clone();
    i.shape = (int []) shape.clone();
    i.current = new int[rank];  // want zeros

    return (Object) i;
  }

  ////////////////////// inner class ///////////////////////////

  /* the idea is IteratorFast can do the iteration without an IndexImpl */
  public class IteratorFast implements IndexIterator {

    private int currElement = -1;
    private final ArrayAbstract maa;

    private IteratorFast(ArrayAbstract maa) {
      this.maa = maa;
      //System.out.println("IteratorFast");
    }

    public boolean hasNext() {
      return currElement < size-1;
    }

    public boolean hasMore(int howMany) {
      return currElement < size-howMany;
    }

    public double getDoubleCurrent() { return maa.getDouble(currElement); }
    public double getDoubleNext() { return maa.getDouble(++currElement); }
    public void setDoubleCurrent(double val) { maa.setDouble(currElement, val); }
    public void setDoubleNext(double val) { maa.setDouble(++currElement, val); }

    public float getFloatCurrent() { return maa.getFloat(currElement); }
    public float getFloatNext() { return maa.getFloat(++currElement); }
    public void setFloatCurrent(float val) { maa.setFloat(currElement, val); }
    public void setFloatNext(float val) { maa.setFloat(++currElement, val); }

    public long getLongCurrent() { return maa.getLong(currElement); }
    public long getLongNext() { return maa.getLong(++currElement); }
    public void setLongCurrent(long val) { maa.setLong(currElement, val); }
    public void setLongNext(long val) { maa.setLong(++currElement, val); }

    public int getIntCurrent() { return maa.getInt(currElement); }
    public int getIntNext() { return maa.getInt(++currElement); }
    public void setIntCurrent(int val) { maa.setInt(currElement, val); }
    public void setIntNext(int val) { maa.setInt(++currElement, val); }

    public short getShortCurrent() { return maa.getShort(currElement); }
    public short getShortNext() { return maa.getShort(++currElement); }
    public void setShortCurrent(short val) { maa.setShort(currElement, val); }
    public void setShortNext(short val) { maa.setShort(++currElement, val); }

    public byte getByteCurrent() { return maa.getByte(currElement); }
    public byte getByteNext() { return maa.getByte(++currElement); }
    public void setByteCurrent(byte val) { maa.setByte(currElement, val); }
    public void setByteNext(byte val) { maa.setByte(++currElement, val); }

    public char getCharCurrent() { return maa.getChar(currElement); }
    public char getCharNext() { return maa.getChar(++currElement); }
    public void setCharCurrent(char val) { maa.setChar(currElement, val); }
    public void setCharNext(char val) { maa.setChar(++currElement, val); }

    public boolean getBooleanCurrent() { return maa.getBoolean(currElement); }
    public boolean getBooleanNext() { return maa.getBoolean(++currElement); }
    public void setBooleanCurrent(boolean val) { maa.setBoolean(currElement, val); }
    public void setBooleanNext(boolean val) { maa.setBoolean(++currElement, val); }
  }

  private class IteratorImpl implements IndexIterator {
    private int count=0;
    private int currElement = 0;
    private IndexImpl counter;
    private ArrayAbstract maa;

    private IteratorImpl(ArrayAbstract maa) {
      this.maa = maa;
      counter = (IndexImpl) IndexImpl.this.clone();  // could be subtype of IndexImpl
      counter.current[rank-1] = -1;                  // avoid "if first" on every incr.
      counter.precalc();
      //System.out.println("IteratorSlow");
    }

    public boolean hasNext() {
      return count < size;
    }

    public String toString() {
      return counter.toString() + count;
    }

    public double getDoubleCurrent() { return maa.getDouble(currElement); }
    public double getDoubleNext() {
      count++;
      currElement = counter.incr();
      return maa.getDouble(currElement);
    }
    public void setDoubleCurrent(double val) { maa.setDouble(currElement, val); }
    public void setDoubleNext(double val) {
      count++;
      currElement = counter.incr();
      maa.setDouble(currElement, val);
    }

    public float getFloatCurrent() { return maa.getFloat(currElement); }
    public float getFloatNext() {
      count++;
      currElement = counter.incr();
      return maa.getFloat(currElement);
    }
    public void setFloatCurrent(float val) { maa.setFloat(currElement, val); }
    public void setFloatNext(float val) {
      count++;
      currElement = counter.incr();
      maa.setFloat(currElement, val);
    }

    public long getLongCurrent() { return maa.getLong(currElement); }
    public long getLongNext() {
      count++;
      currElement = counter.incr();
      return maa.getLong(currElement);
    }
    public void setLongCurrent(long val) { maa.setLong(currElement, val); }
    public void setLongNext(long val) {
      count++;
      currElement = counter.incr();
      maa.setLong(currElement, val);
    }

    public int getIntCurrent() { return maa.getInt(currElement); }
    public int getIntNext() {
      count++;
      currElement = counter.incr();
      return maa.getInt(currElement);
    }
    public void setIntCurrent(int val) { maa.setInt(currElement, val); }
    public void setIntNext(int val) {
      count++;
      currElement = counter.incr();
      maa.setInt(currElement, val);
    }

    public short getShortCurrent() { return maa.getShort(currElement); }
    public short getShortNext() {
      count++;
      currElement = counter.incr();
      return maa.getShort(currElement);
    }
    public void setShortCurrent(short val) { maa.setShort(currElement, val); }
    public void setShortNext(short val) {
      count++;
      currElement = counter.incr();
      maa.setShort(currElement, val);
    }

    public byte getByteCurrent() { return maa.getByte(currElement); }
    public byte getByteNext() {
      count++;
      currElement = counter.incr();
      return maa.getByte(currElement);
    }
    public void setByteCurrent(byte val) { maa.setByte(currElement, val); }
    public void setByteNext(byte val) {
      count++;
      currElement = counter.incr();
      maa.setByte(currElement, val);
    }

    public char getCharCurrent() { return maa.getChar(currElement); }
    public char getCharNext() {
      count++;
      currElement = counter.incr();
      return maa.getChar(currElement);
    }
    public void setCharCurrent(char val) { maa.setChar(currElement, val); }
    public void setCharNext(char val) {
      count++;
      currElement = counter.incr();
      maa.setChar(currElement, val);
    }

    public boolean getBooleanCurrent() { return maa.getBoolean(currElement); }
    public boolean getBooleanNext() {
      count++;
      currElement = counter.incr();
      return maa.getBoolean(currElement);
    }
    public void setBooleanCurrent(boolean val) { maa.setBoolean(currElement, val); }
    public void setBooleanNext(boolean val) {
      count++;
      currElement = counter.incr();
      maa.setBoolean(currElement, val);
    }

  }

  ////////////////////// static /////////////////////////////////

  /** Generate a subclass of IndexImpl optimized for this array's rank */
  static IndexImpl factory( int [] shape) {
    int rank = shape.length;
    switch (rank) {
      case 0: return new Index0D(shape);
      case 1: return new Index1D(shape);
      case 2: return new Index2D(shape);
      case 3: return new Index3D(shape);
      case 4: return new Index4D(shape);
      case 5: return new Index5D(shape);
      case 6: return new Index6D(shape);
      case 7: return new Index7D(shape);
      default: return new IndexImpl(shape);
    }
  }
  private static IndexImpl factory(int rank) {
    switch (rank) {
      case 0: return new Index0D();
      case 1: return new Index1D();
      case 2: return new Index2D();
      case 3: return new Index3D();
      case 4: return new Index4D();
      case 5: return new Index5D();
      case 6: return new Index6D();
      case 7: return new Index7D();
      default: return new IndexImpl(rank);
    }
  }

   /** Compute total number of elements in the array.
   * @param shape length of array in each dimension.
   */
  static long computeSize(int [] shape) {
    long product = 1;
    for(int ii = shape.length-1; ii >= 0; ii--)
      product *= shape[ii];
    return product;
  }

  /** Compute standard strides based on array's shape.
   * @param shape length of array in each dimension.
   * @param stride put result here
   */
  private static long computeStrides(int [] shape, int [] stride) {
    long product = 1;
    for(int ii = shape.length-1; ii >= 0; ii--) {
      final int thisDim = shape[ii];
      if(thisDim < 0)
        throw new NegativeArraySizeException();
      stride[ii] = (int) product;
      product *= thisDim;
    }
    return product;
  }

}

