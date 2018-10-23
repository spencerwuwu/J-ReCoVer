// https://searchcode.com/api/result/66310026/

// File: IntArrayBag.java from the package edu.colorado.collections
// Additional javadoc documentation is available from the IntArrayBag link in:
//   http://www.cs.colorado.edu/~main/docs

package edu.colorado.collections;

/*******************************************************************************
* An IntArrayBag is a collection of int numbers.
* The same number may appear multiple times in a bag...

* kgig
* @note
*   (1) The capacity of one of these bags can change after it's created, but
*   the maximum capacity is limited by the amount of free memory on the 
*   machine. The constructor, addItem, clone, 
*   and union will result in an OutOfMemoryError
*   when free memory is exhausted.
*   <p>
*   (2) A bag's capacity cannot exceed the maximum integer 2,147,483,647
*   (Integer.MAX_VALUE). Any attempt to create a larger capacity
*   results in a failure due to an arithmetic overflow. 
*   <p>
*   (3) Because of the slow linear algorithms of this
*   class, large bags will have poor performance.
*
* @see
*   <A HREF="../../../../edu/colorado/collections/IntArrayBag.java">
*   Java Source Code for this class
*   (www.cs.colorado.edu/~main/edu/colorado/collections/IntArrayBag.java)
*   </A>
*
* @author Michael Main 
*   <A HREF="mailto:main@colorado.edu"> (main@colorado.edu) </A>
*
* @version
*   Jul 5, 2005
*
* @see ArrayBag
* @see IntLinkedBag
******************************************************************************/
public class IntArrayBag implements Cloneable
{
   // Invariant of the IntArrayBag class:
   //   1. The number of elements in the bag is in the instance variable 
   //      manyItems, which is no more than data.length.
   //   2. For an empty bag, we do not care what is stored in any of data;
   //      for a non-empty bag, the elements in the bag are stored in data[0]
   //      through data[manyItems-1], and we dont care whats in the
   //      rest of data.
   private int[ ] data;
   private int manyItems; 
   
   /**
   * Initialize an empty bag with an initial capacity of 10.  Note that the
   * addItem method works efficiently (without needing more
   * memory) until this capacity is reached.
   * @param - none
   * @postcondition
   *   This bag is empty and has an initial capacity of 10.
   * @exception OutOfMemoryError
   *   Indicates insufficient memory for: 
   *   new int[10].
   **/   
   public IntArrayBag( )
   {
      final int INITIAL_CAPACITY = 10;
      manyItems = 0;
      data = new int[INITIAL_CAPACITY];
   }
     

   /**
   * Initialize an empty bag with a specified initial capacity. Note that the
   * addItem method works efficiently (without needing more
   * memory) until this capacity is reached.
   * @param initialCapacity
   *   the initial capacity of this bag
   * @precondition
   *   initialCapacity is non-negative.
   * @postcondition
   *   This bag is empty and has the given initial capacity.
   * @exception IllegalArgumentException
   *   Indicates that initialCapacity is negative.
   * @exception OutOfMemoryError
   *   Indicates insufficient memory for: new int[initialCapacity].
   **/   
   public IntArrayBag(int initialCapacity)
   {
      if (initialCapacity < 0)
         throw new IllegalArgumentException
         ("The initialCapacity is negative: " + initialCapacity);
      data = new int[initialCapacity];
      manyItems = 0;
   }
        
 
   /**
   * Add a new element to this bag. If the new element would take this
   * bag beyond its current capacity, then the capacity is increased
   * before adding the new element.
   * @param element
   *   the new element that is being inserted
   * @postcondition
   *   A new copy of the element has been added to this bag.
   * @exception OutOfMemoryError
   *   Indicates insufficient memory for increasing the bag's capacity.
   * @note
   *   An attempt to increase the capacity beyond
   *   Integer.MAX_VALUE will cause the bag to fail with an
   *   arithmetic overflow.
   **/
   public void add(int element)
   {
      if (manyItems == data.length)
      {  // Ensure twice as much space as we need.
         ensureCapacity((manyItems + 1)*2);
      }

      data[manyItems] = element;
      manyItems++;
   }


   /***
   * Add new elements to this bag. If the new elements would take this
   * bag beyond its current capacity, then the capacity is increased
   * before adding the new elements.
   * @param elements
   *   (a variable-arity argument)
   *   one or more new elements that are being inserted
   * @postcondition
   *   A new copy of the element has been added to this bag.
   * @exception OutOfMemoryError
   *   Indicates insufficient memory for increasing the bag's capacity.
   * @note
   *   An attempt to increase the capacity beyond
   *   Integer.MAX_VALUE will cause the bag to fail with an
   *   arithmetic overflow.
   **/
   public void addMany(int... elements)
   {
      if (manyItems + elements.length > data.length)
      {  // Ensure twice as much space as we need.
         ensureCapacity((manyItems + elements.length)*2);
      }

      System.arraycopy(elements, 0, data, manyItems, elements.length);
      manyItems += elements.length;
   }


   /**
   * Add the contents of another bag to this bag.
   * @param addend
   *   a bag whose contents will be added to this bag
   * @precondition
   *   The parameter, addend, is not null. 
   * @postcondition
   *   The elements from addend have been added to this bag.
   * @exception NullPointerException
   *   Indicates that addend is null. 
   * @exception OutOfMemoryError
   *   Indicates insufficient memory to increase the size of the bag.
   * @note
   *   An attempt to increase the capacity beyond
   *   Integer.MAX_VALUE will cause an arithmetic overflow
   *   that will cause the bag to fail. Such large collections should use
   *   a different bag implementation.
   **/
   public void addAll(IntArrayBag addend)
   {
      // If addend is null, then a NullPointerException is thrown.
      // In the case that the total number of items is beyond
      // Integer.MAX_VALUE, there will be an arithmetic overflow and
      // the bag will fail.
      ensureCapacity(manyItems + addend.manyItems);
         
      System.arraycopy(addend.data, 0, data, manyItems, addend.manyItems);
      manyItems += addend.manyItems;
   }   
   
   
   /**
   * Generate a copy of this bag.
   * @param - none
   * @return
   *   The return value is a copy of this bag. Subsequent changes to the
   *   copy will not affect the original, nor vice versa.
   * @exception OutOfMemoryError
   *   Indicates insufficient memory for creating the clone.
   **/ 
   public IntArrayBag clone( )
   {  // Clone an IntArrayBag object.
      IntArrayBag answer;
      
      try
      {
         answer = (IntArrayBag) super.clone( );
      }
      catch (CloneNotSupportedException e)
      {  // This exception should not occur. But if it does, it would probably
         // indicate a programming error that made super.clone unavailable.
         // The most common error would be forgetting the "Implements Cloneable"
         // clause at the start of this class.
         throw new RuntimeException
         ("This class does not implement Cloneable");
      }
      
      answer.data = data.clone( );
      
      return answer;
   }
   

   /**
   * Accessor method to count the number of occurrences of a particular element
   * in this bag.
   * @param target
   *   the element that needs to be counted
   * @return
   *   the number of times that target occurs in this bag
   **/
   public int countOccurrences(int target)
   {
      int answer;
      int index;
      
      answer = 0;
      for (index = 0; index < manyItems; index++)
         if (target == data[index])
            answer++;
      return answer;
   }


   /**
   * Change the current capacity of this bag.
   * @param minimumCapacity
   *   the new capacity for this bag
   * @postcondition
   *   This bag's capacity has been changed to at least minimumCapacity.
   *   If the capacity was already at or greater than minimumCapacity,
   *   then the capacity is left unchanged.
   * @exception OutOfMemoryError
   *   Indicates insufficient memory for: new int[minimumCapacity].
   **/
   public void ensureCapacity(int minimumCapacity)
   {
      int[ ] biggerArray;
      
      if (data.length < minimumCapacity)
      {
         biggerArray = new int[minimumCapacity];
         System.arraycopy(data, 0, biggerArray, 0, manyItems);
         data = biggerArray;
      }
   }

   
   /**
   * Accessor method to get the current capacity of this bag. 
   * The add method works efficiently (without needing
   * more memory) until this capacity is reached.
   * @param - none
   * @return
   *   the current capacity of this bag
   **/
   public int getCapacity( )
   {
      return data.length;
   }

              
   /**
   * Remove one copy of a specified element from this bag.
   * @param target
   *   the element to remove from the bag
   * @postcondition
   *   If target was found in the bag, then one copy of
   *   target has been removed and the method returns true. 
   *   Otherwise the bag remains unchanged and the method returns false. 
   **/
   public boolean remove(int target)
   {
      int index; // The location of target in the data array.
       
      // First, set index to the location of target in the data array,
      // which could be as small as 0 or as large as manyItems-1; If target
      // is not in the array, then index will be set equal to manyItems;
      for (index = 0; (index < manyItems) && (target != data[index]); index++)
         // No work is needed in the body of this for-loop.
         ;
         
      if (index == manyItems)
         // The target was not found, so nothing is removed.
         return false;
      else
      {  // The target was found at data[index].
         // So reduce manyItems by 1 and copy the last element onto data[index].
         manyItems--;
         data[index] = data[manyItems];
         return true;
      }
   }
                 
   
   /**
   * Determine the number of elements in this bag.
   * @param - none
   * @return
   *   the number of elements in this bag
   **/ 
   public int size( )
   {
      return manyItems;
   }
   
   
   /**
   * Reduce the current capacity of this bag to its actual size (i.e., the
   * number of elements it contains).
   * @param - none
   * @postcondition
   *   This bag's capacity has been changed to its current size.
   * @exception OutOfMemoryError
   *   Indicates insufficient memory for altering the capacity. 
   **/
   public void trimToSize( )
   {
      int[ ] trimmedArray;
      
      if (data.length != manyItems)
      {
         trimmedArray = new int[manyItems];
         System.arraycopy(data, 0, trimmedArray, 0, manyItems);
         data = trimmedArray;
      }
   }
      

   /**
   * Create a new bag that contains all the elements from two other bags.
   * @param b1
   *   the first of two bags
   * @param b2
   *   the second of two bags
   * @precondition
   *   Neither b1 nor b2 is null, and
   *   b1.getCapacity( ) + b2.getCapacity( ) &lt;= Integer.MAX_VALUE.
   * @return
   *   the union of b1 and b2
   * @exception NullPointerException.
   *   Indicates that one of the arguments is null.
   * @exception OutOfMemoryError
   *   Indicates insufficient memory for the new bag.
   * @note
   *   An attempt to create a bag with a capacity beyond
   *   Integer.MAX_VALUE will cause an arithmetic overflow
   *   that will cause the bag to fail. Such large collections should use
   *   a different bag implementation.
   **/   
   public static IntArrayBag union(IntArrayBag b1, IntArrayBag b2)
   {
      // If either b1 or b2 is null, then a NullPointerException is thrown. 
      // In the case that the total number of items is beyond
      // Integer.MAX_VALUE, there will be an arithmetic overflow and
      // the bag will fail.   
      IntArrayBag answer = new IntArrayBag(b1.getCapacity( ) + b2.getCapacity( ));
      
      System.arraycopy(b1.data, 0, answer.data, 0, b1.manyItems);
      System.arraycopy(b2.data, 0, answer.data, b1.manyItems, b2.manyItems);
      answer.manyItems = b1.manyItems + b2.manyItems;
      
      return answer;
   }
      
}
           

