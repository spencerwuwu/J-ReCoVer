// https://searchcode.com/api/result/69652777/

package ncsa.d2k.modules.core.datatype.table.basic;

import ncsa.d2k.modules.core.datatype.table.*;

import java.io.*;
import java.util.*;
import ncsa.d2k.modules.core.datatype.table.util.ByteUtils;

/**
 * <code>BooleanColumn</code> is an implementation of <code>Column</code> which
 * holds boolean values. These values are kept in an array of
 * <code>boolean</code>s.
 */
public class BooleanColumn extends MissingValuesColumn {

	//static final long serialVersionUID = -6442288971928484351L;

	static final long serialVersionUID = -7963304594161482882L;

    /** holds BooleanColumn's internal data rep */
    private boolean[] internal = null;
	private boolean[] empty = null;

    /**
     * Creates a new, empty <code>BooleanColumn</code>.
     */
    public BooleanColumn () {
        this(0);
    }

    /**
     * Creates a new <code>BooleanColumn</code> with the specified capacity.
     *
     * @param capacity       the initial capacity
     */
    public BooleanColumn (int capacity) {
        internal = new boolean[capacity];
        setIsNominal(true);
        type = ColumnTypes.BOOLEAN;

		missing = new boolean[internal.length];
		empty = new boolean[internal.length];
		for(int i = 0; i < internal.length; i++) {
			missing[i] = false;
			empty[i] = false;
		}
    }

    /**
     * Creates a new <code>BooleanColumn</code> with the specified values.
     *
     * @param vals           the initial values
     */
    public BooleanColumn (boolean[] vals) {
        internal = vals;
        setIsNominal(true);
        type = ColumnTypes.BOOLEAN;

		missing = new boolean[internal.length];
		empty = new boolean[internal.length];
		for(int i = 0; i < internal.length; i++) {
			missing[i] = false;
			empty[i] = false;
		}
    }

    private BooleanColumn(boolean[] vals, boolean[] miss, boolean[] emp,
                          String lbl, String comm) {
        setIsNominal(true);
        type = ColumnTypes.BOOLEAN;
        internal = vals;
        this.setMissingValues(miss);
        empty = emp;
        setLabel(lbl);
        setComment(comm);
    }

    /**
     * Returns an exact copy of this <code>Column</code>. A deep copy is
     * attempted, but if it fails, a new <code>BooleanColumn</code> will be
     * created, initialized with the same data as this
     * <code>BooleanColumn</code>.
     *
     * @return               a new <code>BooleanColumn</code> with a copy of
     *                       the contents of this column.
     */
    public Column copy () {
        BooleanColumn newCol;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            byte buf[] = baos.toByteArray();
            oos.close();
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            ObjectInputStream ois = new ObjectInputStream(bais);
            newCol = (BooleanColumn)ois.readObject();
            ois.close();
            return  newCol;
        } catch (Exception e) {
            boolean[] newVals = new boolean[getNumRows()];
            for (int i = 0; i < getNumRows(); i++)
                newVals[i] = getBoolean(i);
			boolean[] miss = new boolean[internal.length];
			boolean[] em = new boolean[internal.length];
			for(int i = 0; i < internal.length; i++) {
				miss[i] = missing[i];
				em[i] = empty[i];

			}
		 	//newCol.missing = miss;
			//newCol.empty = em;
            newCol = new BooleanColumn(newVals, miss, em, getLabel(), getComment());
            return  newCol;
        }
    }

    //////////////////////////////////////
    //// Accessing Metadata

	/**
	 * Returns the internal representation of the data.
	 * @return the internal representation of the data.
	 */
	public Object getInternal () {
		return internal;
	}

    /**
     * Gets the number of rows in this <code>Column</code>.  Same as
     * <code>getCapacity()</code>.
     *
     * @return               the number of rows in this <code>Column</code>
     */
    public int getNumRows () {
        return internal.length;
    }

    /**
     * Gets the number of entries this <code>Column</code> holds.  This is the
     * number of non-null entries in the <code>Column</code>.
     *
     * @return               this <code>Column</code>'s number of entries
     */
    public int getNumEntries() {
        //return internal.length;
		int ctr = 0;
		for(int i = 0; i < internal.length; i++) {
			if(!isValueMissing(i) && !isValueEmpty(i))
				ctr++;
		}
		return ctr;
    }

    /**
     * Sets a new capacity for this <code>BooleanColumn</code>. The capacity is
     * its potential maximum number of entries. If <code>numEntries</code> is
     * greater than <code>newCapacity</code>, the <code>Column</code> will be
     * truncated.
     *
     * @param newCapacity    the new capacity
     */
    public void setNumRows(int newCapacity) {
        if (internal != null) {
            boolean[] newInternal = new boolean[newCapacity];
			boolean[] newMissing = new boolean[newCapacity];
			boolean[] newEmpty = new boolean[newCapacity];
            if (newCapacity > internal.length)
                newCapacity = internal.length;
            System.arraycopy(internal, 0, newInternal, 0, newCapacity);
			System.arraycopy(missing, 0, newMissing, 0, missing.length);
			System.arraycopy(empty, 0, newEmpty, 0, empty.length);
            internal = newInternal;
			missing = newMissing;
			empty = newEmpty;
        }
        else {
            internal = new boolean[newCapacity];
			missing = new boolean[newCapacity];
			empty = new boolean[newCapacity];
		}
    }

    /**
     * Gets the entry at <code>pos</code> as a <code>String</code>.
     *
     * @param pos            the position from which to get a
     *                       <code>String</code>
     * @return               a <code>String</code> representation of the entry
     *                       at that position
     */
    public String getString (int pos) {
        return new Boolean(internal[pos]).toString();
    }

    /**
     * Set the entry at <code>pos</code> to be <code>newEntry</code>. Set to
     * <code>true</code> if and only if <code>newEntry</code> is equal to
     * "true" (ignoring case). Otherwise, set to <code>false</code>.
     *
     * @param newEntry       the new entry
     * @param pos            the position in the column
     */
    public void setString (String newEntry, int pos) {
        internal[pos] = new Boolean(newEntry).booleanValue();
    }

    /**
     * Returns 1 if the value at <code>pos</code> is <code>true</code>, 0
     * otherwise.
     *
     * @param pos            the position in the column
     * @return               1 if the value at pos is <code>true</code>,
     *                       0 otherwise
     */
    public int getInt (int pos) {
        if (internal[pos])
            return 1;
        return 0;
    }

    /**
     * Set the entry at <code>pos</code> to <code>false</code> if
     * <code>newEntry</code> is equal to 0. Set to <code>true</code> otherwise.
     *
     * @param newEntry       the new entry
     * @param pos            the position in the column
     */
    public void setInt (int newEntry, int pos) {
        if (newEntry == 0)
            internal[pos] = false;
        else
            internal[pos] = true;
    }

    /**
     * Returns 1 if the value at <code>pos</code> is <code>true</code>, 0
     * otherwise.
     *
     * @param pos            the position in the column
     * @return               1 if the value at <code>pos</code> is
     *                       <code>true</code>, 0 otherwise
     */
    public short getShort (int pos) {
        if (internal[pos])
            return  (short)1;
        return  (short)0;
    }

    /**
     * Set the entry at <code>pos</code> to <code>false</code> if
     * <code>newEntry</code> is equal to 0. Set to <code>true</code> otherwise.
     *
     * @param newEntry       the new entry
     * @param pos            the position in the column
     */
    public void setShort (short newEntry, int pos) {
        if (newEntry == 0)
            internal[pos] = false;
        else
            internal[pos] = true;
    }

    /**
     * Returns 1 if the value at <code>pos</code> is <code>true</code>, 0
     * otherwise.
     *
     * @param pos            the position in the column
     * @return               1 if the value at <code>pos</code> is
     *                       <code>true</code>, 0 otherwise
     */
    public long getLong (int pos) {
        if (internal[pos])
            return  (long)1;
        return  (long)0;
    }

    /**
     * Set the entry at <code>pos</code> to <code>false</code> if
     * <code>newEntry</code> is equal to 0. Set to <code>true</code> otherwise.
     *
     * @param newEntry       the new entry
     * @param pos            the position in the column
     */
    public void setLong (long newEntry, int pos) {
        if (newEntry == 0)
            internal[pos] = false;
        else
            internal[pos] = true;
    }

    /**
     * Returns 1 if the value at <code>pos</code> is <code>true</code>, 0
     * otherwise.
     *
     * @param pos            the position in the column
     * @return               1 if the value at <code>pos</code> is
     *                       <code>true</code>, 0 otherwise
     */
    public double getDouble (int pos) {
        if (internal[pos])
            return  (double)1;
        return  (double)0;
    }

    /**
     * Set the entry at <code>pos</code> to <code>false</code> if
     * <code>newEntry</code> is equal to 0. Set to <code>true</code> otherwise.
     *
     * @param newEntry       the new entry
     * @param pos            the position in the column
     */
    public void setDouble (double newEntry, int pos) {
        if (newEntry == 0)
            internal[pos] = false;
        else
            internal[pos] = true;
    }

    /**
     * Returns 1 if the value at <code>pos</code> is <code>true</code>, 0
     * otherwise.
     *
     * @param pos            the position in the column
     * @return               1 if the value at <code>pos</code> is
     *                       <code>true</code>, 0 otherwise
     */
    public float getFloat (int pos) {
        if (internal[pos])
            return  (float)1;
        return  (float)0;
    }

    /**
     * Set the entry at <code>pos</code> to <code>false</code> if
     * <code>newEntry</code> is equal to 0. Set to <code>true</code> otherwise.
     *
     * @param newEntry       the new entry
     * @param pos            the position in the column
     */
    public void setFloat (float newEntry, int pos) {
        if (newEntry == 0)
            internal[pos] = false;
        else
            internal[pos] = true;
    }

    /**
     * Returns the byte representation of the entry at <code>pos</code> using
     * <code>ByteUtils.writeBoolean()</code>.
     *
     * @param pos            the position of interest
     * @return               the entry at <code>pos</code> as an array of bytes
     */
    public byte[] getBytes (int pos) {
        return ByteUtils.writeBoolean(internal[pos]);
    }

    /**
     * Converts <code>newEntry</code> to a <code>boolean</code> using
     * <code>ByteUtils.toBoolean()</code> and sets the value at <code>pos</code>
     * to this <code>boolean</code>.
     *
     * @param newEntry       the new item
     * @param pos            the position
     */
    public void setBytes (byte[] newEntry, int pos) {
        internal[pos] = ByteUtils.toBoolean(newEntry);
    }

    /**
     * Returns 1 if the value at <code>pos</code> is <code>true</code>, 0
     * otherwise.
     *
     * @param pos            the position in the column
     * @return               1 if the value at <code>pos</code> is
     *                       <code>true</code>, 0 otherwise
     */
    public byte getByte (int pos) {
        if (internal[pos])
            return  1;
        return  0;
    }

    /**
     * Set the entry at <code>pos</code> to <code>false</code> if
     * <code>newEntry</code> is equal to 0. Set to <code>true</code> otherwise.
     *
     * @param newEntry       the new entry
     * @param pos            the position in the column
     */
    public void setByte (byte newEntry, int pos) {
        if (newEntry == 0)
            internal[pos] = false;
        else
            internal[pos] = true;
    }

    /**
     * Converts the value at <code>pos</code> to a <code>String</code> and
     * returns the <code>String</code> as a char array.
     *
     * @param pos            the position
     * @return               the <code>String</code> representation of the item
     *                       at <code>pos</code> as an array of
     *                       <code>char</code>s
     */
    public char[] getChars (int pos) {
        return getString(pos).toCharArray();
    }

    /**
     * Set the entry at <code>pos</code> to be <code>newEntry</code>. Set to
     * <code>true</code> if and only if <code>newEntry</code> is equal to
     * "true" (ignoring case). Otherwise, set to <code>false</code>.
     *
     * @param newEntry       the new entry
     * @param pos            the position in the column
     */
    public void setChars (char[] newEntry, int pos) {
        setString(new String(newEntry), pos);
    }

    /**
     * Returns the character <code>'t'</code> or <code>'f'</code> corresponding
     * to a <code>true</code> or <code>false</code> value at this position,
     * respectively.
     *
     * @param pos            the position
     * @return               the appropriate <code>char</code>
     */
    //ANCA changed to returning t and f instead of T and F
    public char getChar (int pos) {
        if (internal[pos])
            return 't';
        else
            return 'f';
    }

    /**
     * Set the entry at <code>pos</code> to correspond to <code>newEntry</code>.
     * Set to <code>true</code> if and only if <code>newEntry</code> is equal
     * to <code>'T'</code> or <code>'t'</code>. Otherwise set to
     * <code>false</code>.
     *
     * @param newEntry       the new entry
     * @param pos            the position in the column
     */
    public void setChar (char newEntry, int pos) {
        if (newEntry == 'T' || newEntry == 't')
            internal[pos] = true;
        else
            internal[pos] = false;
    }

    /**
     * Return the <code>boolean</code> value at this position.
     *
     * @param pos            the position
     * @return               the value at pos
     */
    public boolean getBoolean (int pos) {
        return this.internal[pos];
    }

    /**
     * Set the <code>boolean</code> value at this position.
     *
     * @param newEntry       the new entry
     * @param pos            the position
     */
    public void setBoolean (boolean newEntry, int pos) {
        this.internal[pos] = newEntry;
    }

    /**
     * Return the value at <code>pos</code> as a <code>Boolean</code>.
     *
     * @param pos            the position
     * @return               a <code>Boolean</code> object with the value
     *                       contained at <code>pos</code>
     */
    public Object getObject (int pos) {
        return new Boolean(internal[pos]);
    }

    /**
     * Sets the value at <code>pos</code> to correspond to
     * <code>newEntry</code>. If <code>newEntry</code> is a
     * <code>Boolean</code>, then it sets the value to be the same as
     * <code>newEntry</code>. If <code>newEntry</code> is not a
     * <code>Boolean</code>, the <code>setString()</code> method is called
     * with <code>newEntry</code>.
     *
     * @param newEntry       the new entry
     * @param pos            the position
     */
    public void setObject (Object newEntry, int pos) {
        if (newEntry instanceof Boolean)
            this.internal[pos] = ((Boolean)newEntry).booleanValue();
        else
            setString(newEntry.toString(), pos);
    }

    //////////////////////////////////////
    //// ACCESSING FIELD ELEMENTS
    /**
      Gets a reference to the internal representation of this Column.
      (boolean[]).  Changes made to this object will be reflected in the Column.
      @return the internal representation of this Column.
     /
    public Object getInternal () {
        return  this.internal;
    }*/

    /**
     * Gets a subset of this <code>Column</code>, given a start position and
     * length. The primitive values are copied, so they have no destructive
     * abilities as far as the <code>Column</code> is concerned.
     *
     * @param pos            the start position for the subset
     * @param len            the length of the subset
     * @return               a subset of this <code>Column</code>
     */
    public Column getSubset (int pos, int len) {
        if ((pos + len) > internal.length)
            throw  new ArrayIndexOutOfBoundsException();
        boolean[] subset = new boolean[len];
		boolean[] newMissing = new boolean[len];
		boolean[] newEmpty = new boolean[len];
        System.arraycopy(internal, pos, subset, 0, len);
		System.arraycopy(missing, pos, newMissing, 0, len);
		System.arraycopy(empty, pos, newEmpty, 0, len);
        BooleanColumn bc = new BooleanColumn(subset, newMissing, newEmpty, getLabel(), getComment());
        int t = 0;
        for (int i = pos ; i < len ; i++)
        	if (missing[i]) t++;
        bc.numMissingValues = t;
        return  bc;
    }

    /**
     * Gets a subset of this <code>Column</code>, given a start position and
     * length. The primitive values are copied, so they have no destructive
     * abilities as far as the <code>Column</code> is concerned.
     *
     * @param pos            the start position for the subset
     * @param len            the length of the subset
     * @return               a subset of this <code>Column</code>
     */
    public Column getSubset (int[] rows) {
        boolean[] subset = new boolean[rows.length];
        boolean[] newMissing = new boolean[rows.length];
        boolean[] newEmpty = new boolean[rows.length];
        int t = 0;
        for(int i = 0; i < rows.length; i++) {
          subset[i] = internal[rows[i]];
          newMissing[i] = missing[rows[i]];
          if (missing[rows[i]]) t++;
          newEmpty[i] = empty[rows[i]];
        }

//                System.arraycopy(missing, pos, newMissing, 0, len);
//                System.arraycopy(empty, pos, newEmpty, 0, len);
        /*BooleanColumn bc = new BooleanColumn(subset);
                bc.missing = newMissing;
                bc.empty = newEmpty;
        bc.setLabel(getLabel());
        bc.setComment(getComment());
        */
        BooleanColumn bc = new BooleanColumn(subset, newMissing, newEmpty, getLabel(), getComment());
        bc.numMissingValues = t;
        return  bc;
    }


    //////////////////////////////////////
    /**
      Sets the reference to the internal representation of this Column.
      @param newInternal a new internal representation for this Column
     /
    public void setInternal (Object newInternal) {
        if (newInternal instanceof boolean[])
            this.internal = (boolean[])newInternal;
    }*/

    /**
     * Gets a <code>Boolean</code> representation of the entry at the indicated
     * position.
     *
     * @param pos            the position
     * @return               the entry at pos
     */
    public Object getRow (int pos) {
        return new Boolean(internal[pos]);
    }

    /**
     * Sets the entry at the given position to <code>newEntry</code>.
     * <code>newEntry</code> should be a <code>Boolean</code>.
     *
     * @param newEntry       a new entry, a <code>Boolean</code>
     * @param pos            the position to set
     */
    public void setRow (Object newEntry, int pos) {
        internal[pos] = ((Boolean)newEntry).booleanValue();
    }

    /**
     * Appends the new entry to the end of the <code>Column<code>.
     *
     * @param newEntry       a new entry
     */
    public void addRow (Object newEntry) {
        int last = internal.length;
        boolean[] newInternal = new boolean[internal.length + 1];
		boolean[] newMissing = new boolean[internal.length + 1];
		boolean[] newEmpty = new boolean[internal.length + 1];
        System.arraycopy(internal, 0, newInternal, 0, internal.length);
		System.arraycopy(missing, 0, newMissing, 0, missing.length);
		System.arraycopy(empty, 0, newEmpty, 0, empty.length);
        newInternal[last] = ((Boolean)newEntry).booleanValue();

        internal = newInternal;
		missing = newMissing;
		empty = newEmpty;
    }

	/**
	 * Add the specified number of blank rows.
	 * @param number number of rows to add.
	 */
	public void addRows (int number) {
		int last = internal.length;
		boolean[] newInternal = new boolean[last + number];
		boolean[] newMissing = new boolean[last + number];
		boolean[] newEmpty = new boolean[last + number];

		System.arraycopy(internal, 0, newInternal, 0, last);
		System.arraycopy(missing, 0, newMissing, 0, missing.length);
		System.arraycopy(empty, 0, newEmpty, 0, empty.length);
		internal = newInternal;
		missing = newMissing;
		empty = newEmpty;
	}

    /**
     * Removes an entry from the <code>Column</code>, at <code>pos</code>. All
     * entries from <code>pos</code>+1 will be moved back 1 position.
     *
     * @param pos            the position to remove
     * @return               a <code>Boolean</code> representation of the
     *                       removed <code>boolean</code>
     */
    public Object removeRow (int pos) {
        boolean removed = internal[pos];
        // copy all the items after the item to be removed one position up
        System.arraycopy(internal, pos + 1, internal, pos, internal.length -
                (pos + 1));

		// If the row is missing reduce our count.
		if (missing[pos]) 
			this.numMissingValues--;
        System.arraycopy(missing, pos + 1, missing, pos, internal.length -
                (pos + 1));

        System.arraycopy(empty, pos + 1, empty, pos, internal.length -
                (pos + 1));

        // copy the items into a new array
        boolean newInternal[] = new boolean[internal.length - 1];
		boolean newMissing[] = new boolean[internal.length-1];
		boolean newEmpty[] = new boolean[internal.length-1];
        System.arraycopy(internal, 0, newInternal, 0, internal.length - 1);
        System.arraycopy(missing, 0, newMissing, 0, internal.length - 1);
        System.arraycopy(empty, 0, newEmpty, 0, internal.length - 1);

        internal = newInternal;
		missing = newMissing;
		empty = newEmpty;
        return  new Boolean(removed);
    }

    /**
     * Inserts a new entry in the <code>Column</code> at position
     * <code>pos</code>. All elements from <code>pos</code> to capacity
     * will be moved up one. If <code>pos</code> is greater than the capacity
     * of this <code>Column</code>, the new entry will be appended to the end
     * of the <code>Column</code> by calling <code>addRow</code>.
     *
     * @param newEntry       a <code>Boolean</code>-wrapped <code>boolean</code>
     *                       as the new entry to insert
     * @param pos            the position to insert at
     */
    public void insertRow (Object newEntry, int pos) {
        if (pos > getNumRows()) {
            addRow(newEntry);
            return;
        }
        boolean[] newInternal = new boolean[internal.length + 1];
		boolean[] newMissing = new boolean[internal.length + 1];
		boolean[] newEmpty = new boolean[internal.length + 1];
        if (pos == 0) {
            System.arraycopy(internal, 0, newInternal, 1, getNumRows());
            System.arraycopy(missing, 0, newMissing, 1, getNumRows());
            System.arraycopy(empty, 0, newEmpty, 1, getNumRows());
		}
		/*
         newInternal[0] = internal[0];
         System.arraycopy(internal, 1, newInternal, 2, getCapacity()-2);
         }*/
        else {
            System.arraycopy(internal, 0, newInternal, 0, pos);
            System.arraycopy(internal, pos, newInternal, pos + 1, internal.length
                    - pos);

            System.arraycopy(missing, 0, newMissing, 0, pos);
            System.arraycopy(missing, pos, newMissing, pos + 1, internal.length
                    - pos);

            System.arraycopy(empty, 0, newEmpty, 0, pos);
            System.arraycopy(empty, pos, newEmpty, pos + 1, internal.length
                    - pos);
        }
        newInternal[pos] = ((Boolean)newEntry).booleanValue();
        internal = newInternal;
		missing = newMissing;
		empty = newEmpty;
    }

    /**
     * Swaps two entries in the column.
     *
     * @param pos1           the position of the 1st entry to swap
     * @param pos2           the position of the 2nd entry to swap
     */
    public void swapRows (int pos1, int pos2) {
        boolean d1 = internal[pos1];
		boolean miss = missing[pos1];
		boolean emp = empty[pos1];
        internal[pos1] = internal[pos2];
        internal[pos2] = d1;

		missing[pos1] = missing[pos2];
		missing[pos2] = miss;

		empty[pos1] = empty[pos2];
		empty[pos2] = emp;
    }

    /**
     * Gets a copy of this <code>Column</code>, reordered, based on the input
     * array of indices. Does not overwrite this <code>Column</code>.
     *
     * @param newOrder       an array of indices indicating a new order
     * @return               a copy of this column, re-ordered
     */
    public Column reorderRows (int[] newOrder) {
        boolean[] newInternal = null;
		boolean[] newMissing = null;
		boolean[] newEmpty = null;
        if (newOrder.length == internal.length) {
            newInternal = new boolean[internal.length];
			newMissing = new boolean[internal.length];
			newEmpty = new boolean[internal.length];
            for (int i = 0; i < internal.length; i++) {
                newInternal[i] = internal[newOrder[i]];
				newMissing[i] = missing[newOrder[i]];
				newEmpty[i] = empty[newOrder[i]];
			}
        }
        else
            throw  new ArrayIndexOutOfBoundsException();
        /*BooleanColumn bc = new BooleanColumn(newInternal);
		bc.missing = newMissing;
		bc.empty = newEmpty;
        bc.setLabel(getLabel());
        bc.setComment(getComment());
        */
        BooleanColumn bc = new BooleanColumn(newInternal, newMissing, newEmpty, getLabel(), getComment());
        return  bc;
    }

    /**
     * Given an array of <code>int</code>s, removes the positions in the
     * <code>Column</code> which are indicated by the <code>int</code>s in the
     * array.
     *
     * @param indices        the <code>int</code> array of remove indices
     */
    public void removeRowsByIndex (int[] indices) {
        HashSet toRemove = new HashSet(indices.length);
        for (int i = 0; i < indices.length; i++) {
            Integer id = new Integer(indices[i]);
            toRemove.add(id);
        }
        boolean newInternal[] = new boolean[internal.length - indices.length];
        boolean newMissing[] = new boolean[internal.length - indices.length];
        boolean newEmpty[] = new boolean[internal.length - indices.length];

        int newIntIdx = 0;
        for (int i = 0; i < getNumRows(); i++) {
            // check if this row is in the list of rows to remove
            //Integer x = (Integer)toRemove.get(new Integer(i));
            // if this row is not in the list, copy it into the new internal
            //if (x == null) {
         if(!toRemove.contains(new Integer(i))) {
                newInternal[newIntIdx] = internal[i];
				newMissing[newIntIdx] = missing[i];
				newEmpty[newIntIdx] = empty[i];
                newIntIdx++;
         } else 				
         	if (missing[i])
		 		this.numMissingValues--;
        }
        internal = newInternal;
		missing = newMissing;
		empty = newEmpty;
    }

    //////////////////////////////////////

    /**
     * Sort the items in this column.
     */
    public void sort () {
        sort(null);
    }

    /**
     * Sort the items in this <code>Column</code>, and also sort the rows
     * in the corresponding <code>Table</code> accordingly.
     *
     * @param t              the <code>Table</code> for which to swap rows
     */
    public void sort (MutableTable t) {
        internal = doSort(internal, 0, internal.length - 1, t);
    }

    /**
     * Sort the elements in this <code>Column</code> starting with row
     * <code>begin</code> up to row <code>end</code>, and also swap the
     * rows in the <code>Table</code> we are a part of.
     *
     * @param t              the <code>Table</code> for which to swap rows
     * @param begin          the row number which marks the beginning of the
     *                       column segment to be sorted
     * @param end            the row number which marks the end of the column
     *                       segment to be sorted
     */
    public void sort(MutableTable t,int begin, int end) {
        if (end > internal.length -1) {
            System.err.println(" end index was out of bounds");
            end = internal.length -1;
        }
        internal = doSort(internal, begin, end, t);
    }

    /**
     * Compare the values of <code>element</code> and the value at
     * <code>row</code>. Return 0 if they are the same or greater than 0 if
     * <code>element </code> is true  and row element is false and 
     * less than 0 if <code> element </code> is false and row element is true.
     *
     * @param element        the object to be passed in should be a subclass of
     *                       <code>Boolean</code>
     * @param row            the position of the element in this
     *                       <code>Column</code> to be compared with
     *
     * @return               a value representing the relationship:
     *                       <code>&gt;</code>, <code>&lt;</code>, or
     *                       <code>==</code> 0
     */
    public int compareRows (Object element, int row) {
        /*boolean b = ((Boolean)element).booleanValue();
        if (b == getBoolean(row))
            return  0;
        else
            return  1;*/
				int b1 = ((Boolean)element).booleanValue() ? 1:0;
				int b2 = getInt(row);
				return b1-b2; 
            
    }

    /**
     * Compare the values of the elements at <code>r1</code> and
     * <code>r2</code>. Return 0 if they are the same, less than 0 if 
     * r1 element is false and r2 element is true, and greater than 0 if r1 element
     * is true and r2 element is false.
     *
     * @param r1             the first row to compare
     * @param r2             the second row to compare
     * @return               a value representing the relationship:
     *                       <code>&gt;</code>, <code>&lt;</code>, or
     *                       <code>==</code> 0
     */
    public int compareRows (int r1, int r2) {
    	/*
        boolean b = getBoolean(r1);
        if (b == getBoolean(r2))
            return  0;
        else
            return  1; */
           int b1 = getInt(r1);
           int b2 = getInt(r2);
           return b1-b2; 
            
    }

    //////////////////////////////////////

    private boolean[] doSort (boolean[] A, int p, int r, MutableTable t) {
        if (p < r) {
            int q = partition(A, p, r, t);
            doSort(A, p, q, t);
            doSort(A, q + 1, r, t);
        }
        return  A;
    }

    private int partition (boolean[] A, int p, int r, MutableTable t) {
		boolean x = A[p];
		boolean xMissing = this.isValueMissing(p);
		int i = p - 1;
		int j = r + 1;
		while (true) {
			if (xMissing) {
				j--;
				do {
					i++;
				} while (!this.isValueMissing(i));
			} else {
				do {
					j--;
				} while (this.isValueMissing(j) || (A[j] && !x));
				do {
					i++;
				} while (!this.isValueMissing(i) && (!A[i] && x));
			}
			if (i < j) {
				if (t == null)
					this.swapRows(i, j);
				else
					t.swapRows(i, j);
			}
			else
				return  j;
		}
    }

	public void setValueToEmpty(boolean b, int row) {
		empty[row] = b;
	}

	public boolean isValueEmpty(int row) {
		return empty[row];
	}

}
/* BooleanColumn */

