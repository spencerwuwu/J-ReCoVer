// https://searchcode.com/api/result/38954892/

/* 
 * This was a class project where had to create and implement a double linkedlist data
 * structure.
 */
package com.kurtronaldmueller.doublelinkedlist;

import java.util.List;
import java.util.AbstractList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

@SuppressWarnings("rawtypes")
public class DoubleLinkedList<E> extends AbstractList implements List {

    private Node<E> head = null; // the first node in the list
    private Node<E> tail = null; // the last node in the list
    private int size = 0;        // the size of the list
    private DoubleLinkedListIterator iterator;

    /**
     * A node is the building block for this double-linked list. It contains
     * reference to a data item as well as to the next and previous nodes.
     * 
     * @param <E> A generic data type.
     */
    private static class Node<E> {

        private E data;              // the data item of the node
        private Node<E> next = null; // the link to the next node
        private Node<E> prev = null; // the link to the previous node

        /**
         * Construct a node with the given data value.
         * 
         * @param dataItem The data value.
         */
        private Node( E dataItem ) {
            this.data = dataItem;
        }

        /**
         * Construct a node with a given data value and references to the next
         * and previous nodes.
         * 
         * @param dataItem The data value to insert in the current node.
         * @param nextNode The link to the next node.
         * @param prevNode The link to the previous node.
         */
        private Node( E dataItem, Node<E> nextNode, Node<E> prevNode ) {
            this.data = dataItem; // set the data value of the current node
            this.next = nextNode; // set the link to the next node
            this.prev = prevNode; // set the link to the previous node
        }
    } // end class Node<E>

    /**
     * The iterator class is used to to traverse the double linked list.
     */
    @SuppressWarnings("unused")
    private class DoubleLinkedListIterator implements ListIterator<E> {

        private Node<E> nextItem;         // a reference to the next item
        private Node<E> lastItemReturned; // a reference to the node that was last returned by the class' next/previous method.
        private int index;                // the index of the current item

        /**
         * Constructs an iterator object that will reference the item in position
         * specified by the position parameter.
         * 
         * @param position The position in the list to start the List Iterator.
         */
        public DoubleLinkedListIterator( int position ) {

            // if the position passed in is less than 0 or greater than the linked list's
            // size, throw an index out of bounds exception
            if( position < 0 || position > size ){
                throw new IndexOutOfBoundsException( "Invalid Index: " + index );
            }

            // no item returned yet... when the iterator starts traversing through the
            // list, this will, as the variable name implies, be set to the value of the
            // last time returned
            this.lastItemReturned = null; 

            // special case for when the position passed in is equal to the current size
            // of the double linked list
            if( position == size ) {
                this.index    = size;
                this.nextItem = null;
            }
            // start at the beginning of the list and traverse through the list until
            // the index reaches the desired position
            else {
                // start at the first item in the linked list
                this.nextItem = head;

                // traverse through the list until immediately before the desired position
                for( index = 0; index < position; index++ ) {
                    this.nextItem = nextItem.next;
                    this.lastItemReturned = this.nextItem;
                }
            }
        } // end constructor DoubleLinkedListIterator( int position )

        /**
         * Indicates if the there is a next item in the list. If a node ahead of
         * the current node exists, this method returns true. Otherwise, if there
         * is no node ahead of the current one, it returns false.
         * 
         * @return True if node ahead of the current node exists.
         */
        public boolean hasNext() {

            // examines if the next item is either a node or does not exist && 
            // returns the result
            return this.nextItem != null;
        }

        /**
         * Move the iterator forward in the list and return the next item.
         * 
         * @return The next item in the list.
         * @throws NoSuchElementException if there is no such element. 
         */
        public E next() {

            // if there is not a node immediately ahead of the current node
            if( ! hasNext() ) {
                throw new NoSuchElementException();
            }

            // Once we've checked that a next node exists, we can than move forward
            // in the list. This next series of statements is simply traversing from
            // the current node to the next node.
            this.lastItemReturned = this.nextItem; // store the next node into current node
            this.nextItem = this.nextItem.next;    // the node ahead of the next node becomes the next node
            this.index++;                          // reflect the new position with the index variable
            return this.lastItemReturned.data;     // return the new node's data
        }

        /**
         * Indicate if the there a node before the current node exists. 
         * 
         * @return True if a node previous to the current one exists.
         */
        public boolean hasPrevious() {
            // a previous node exists if:
            // 1. the next item is null (i.e. no node exists) && the size is 
            // equal to or greater than 1 OR
            // 2. the next nodes pointer to the previous node is not null,
            return ( this.nextItem == null && size != 0 ) ||    
                    this.nextItem.prev != null; 
        }

        /**
         * Returns the previous item and moves the iterator backwards.
         * 
         * @return The previous item in the list.
         * @throws NoSuchElementException if there is no such object.
         */
        public E previous() {

            // if there is no previous element
            if( ! hasPrevious() ) {
                throw new NoSuchElementException();
            }

            // if the iterator is past the last element, set its position to the
            // tail of the list
            if( this.nextItem == null ) {
                this.nextItem = tail;
            }
            // go backward in the list - the new next item is set to the current item
            else {
                this.nextItem = this.nextItem.prev;
            }

            this.lastItemReturned = this.nextItem; // the last returned item becomes the node that was just iterated over
            this.index--; // reduce the list index by one

            return this.lastItemReturned.data; // return the data item that was just iterated over
        }

        /**
         * Inserts a new node before the node referenced by the class member 'nextItem'.
         * The are four possibilities when adding a new node: 1) add to an empty list,
         * 2) add to the head of the list, 3) add to the tail of the list, and 4) add
         * to the middle of the list.  
         * 
         * @param obj The object whose value will be stored in the new ndoe.
         */
        public void add( E obj ) {

            // if the list is empty, the newly created node will be referenced by
            // both the head and tail indicators
            if ( head == null ) {
                head = new Node<E>( obj );
                tail = head;
            }
            // if the next node is the head of the node, insert a node that will
            // become the new head of the list
            else if ( this.nextItem == head ) {
                Node<E> newNode    = new Node<E>( obj ); // create a new node whose data item is the object passed in
                newNode.next       = this.nextItem;      // link the new node to the next node
                this.nextItem.prev = newNode;            // link the next node's previous pointer to the new node
                head               = newNode;            // the new node becomes the head
            }
            // if the next node does not exist, the iterator is at the end of the list
            // and the new node will be inserted at the tail
            else if ( this.nextItem == null ) {
                Node<E> newNode = new Node<E>( obj ); // create the new node
                tail.next = newNode; // set the tail node to point to the new nodes
                newNode.prev = tail; // set the new node's previous pointer to the tail
                tail = newNode;      // the new node is now the tail of the list
            }
            // add the new node to the middle of the list
            else {
                Node<E> newNode = new Node<E>( obj );         // create a new node
                newNode.prev            = this.nextItem.prev; // the new node's previous pointer links to the next item's previous link
                this.nextItem.prev.next = newNode;            // the nextItem.prev.next simply means the previous node's next link and it points to the current node
                newNode.next            = this.nextItem;      // the new node's next link points to the next item
                this.nextItem.prev      = newNode;            // the next item's previous link points to the new node
            }

            size++;                  // increase the total size of the list
            index++;                 // increment the index position of the iterator
            lastItemReturned = null; // the last item returned is null since the iterator didn't traverse any nodes
        }

        /**
         * Set the current node's data with new data being passed in. 
         * 
         * @param obj The data that replaces the data of the last item returned.
         * @throws IllegalStateException when the set command has not been preceded
         * by a call to the next or previous method.
         */
        public void set( E obj ) {
            if( lastItemReturned == null ) {
                throw new IllegalStateException();
            }
            else {
                lastItemReturned.data = obj;
            }
        }

        /**
         * Removes the last item returned from a call to the 'next' or 'previous' methods.
         * 
         * @throws IllegalStateException when the call to 'remove' is not preceded by a
         * call to 'next' or 'previous'
         */
        public void remove() {
            if( lastItemReturned == null ) {
                throw new IllegalStateException();
            }
            // remove the node currently in the 'head' position
            else if( lastItemReturned == head ) {
                head = lastItemReturned.next;      // the new head is the returned last item's link to the next node
                lastItemReturned.next.prev = null; // the new head's previous link is set to null
                lastItemReturned.next = null;      // the item being removed has its last link removed
            }
            // remove the node currently in the 'tail' position
            else if( lastItemReturned == tail ) {
                tail = lastItemReturned.prev;      // the new tail is the returned last item's link to the previous node
                lastItemReturned.prev.next = null; // the new tail's 'next' node is set to null
                lastItemReturned.prev = null;      // the item being removed has its last link to the list removed
            }
            // remove a node in the middle of the list
            else {

                lastItemReturned.next.prev = lastItemReturned.prev; // set the next item's previous link to the current item's previous link
                lastItemReturned.prev.next = lastItemReturned.next; // set the previous item's next link to the curent item's next link
                lastItemReturned.next = null; // detach the current item's previous & next links
                lastItemReturned.prev = null;
            }
        }

        /**
         * Returns the index of the item that will be returned by the next call to previous.
         * 
         * @return The item index that will be returned by the next call to previous. -1 if
         * the iterator is at the beginning of the list.
         */
        public int previousIndex() {

            // if the index is greater than 0, iterate backward in the list by one
            if( index > 0 ) {
                this.previous(); // go backward
                return index;    // return the index
            }

            // if the index is equal to 0, -1 is returned to signify that the
            // iterator cannot go backwards any further
            return -1;
        }

        /**
         * Returns the index of the item that will be returned by the next call to 'next'.
         * If the iterator is at the end, the list size is returned.
         * 
         * @return The index of the item that will be returned by the next call to 'next'.
         * The list size is returned if the iterator is at the end of the linked list.
         */
        public int nextIndex() {

            // if the index is less than the linked list size, iterate to the next object
            // and return the index
            if( index < size ) {
                this.next();
                return index;
            }

            // otherwise, the iterator is at the end of the list
            return size;
        }
    } // end List Iterator class

    /**
     * Returns a DoubleLinkedListIterator that begins just before the first element.
     * 
     * @return Returns the Double Linked List Iterator starting just before the 1st element.
     */
    public DoubleLinkedListIterator listIterator() {
        return new DoubleLinkedListIterator( 0 );
    }

    /**
     * Returns a Double Linked List Iterator that begins just before the passed-in position.
     * 
     * @return The double linked list iterator that starts at the position.
     */
    public DoubleLinkedListIterator listIterator( int position ) {
        return new DoubleLinkedListIterator( position );
    } 

    /**
     * Inserts an object into the specified position in the DoubleLinkedList. 
     * 
     * @param position The position in the list to enter the object.
     * @param entry The object in which to add.
     */
    @SuppressWarnings("unchecked")
    public void add( int position, Object entry ) {
        listIterator( position ).add( (E) entry); // use the iterator to traverse to the position and insert the object
    }

    /**
     * Add the entry to the head of the list.
     * 
     * @param entry The entry to add into the Double Linked List.
     */
    public void addFirst( E entry ) {
        listIterator().add( entry ); // use the iterator to traverse to the position & insert the entry
    }

    /**
     * Add an entry to the tail of the list.
     * 
     * @param entry The entry to add to the end of the list.
     */
    public void addLast( E entry ) {
        listIterator( size ).add( entry ); // use the iterator to traverse to to the end of the list & insert the entry
    }

    /**
     * Sets the entry in the linked list at the specified position.
     * 
     * @param position The position in which to set the item.
     * @param entry The new entry that will replace the old entry.
     * @return The newly changed entry.
     */
    @SuppressWarnings("unchecked")
    public Object set( int position, Object entry ) {

        // start the list iterator at the specified position & set the entry
        listIterator( position ).set( (E) entry );

        // return the new entry
        return (E) entry;
    }

    /**
     * Gets the entry at the specified position. Returns the item at the specified 
     * position.
     * 
     * @param position The position of the entry in the Double Linked List.
     * @return The entry located at the position.
     */
    public E get( int position ) {
        return (E) listIterator( position ).next();
    }

    /**
     * Get the first entry in the double linked list.
     *  
     * @return The first entry in the double linked list.
     */
    public E getFirst() {

        // start the iterator at the beginning of the list & return the next entry
        return (E) listIterator( 0 ).next();
    }

    /**
     * Gets the last entry in the double linked list.
     * 
     * @return The last entry in the double linked list.
     */
    public E getLast() {

        // start the iterator at the end of hte list & return the previous entry
        return (E) listIterator( size ).previous();
    }

    /**
     * Removes the specified entry from the list.
     * 
     * @param obj The list entry to remove.
     * @return True if the removal was successful.  False if it was not.
     */
    @SuppressWarnings("unchecked")
    public boolean remove( Object obj ) {

        // reset the iterator
        iterator = listIterator();

        // while there are more elements in the list
        while( iterator.hasNext() ) {

            // check to see if the next node's entry is equal to the passed in
            // entry
            if( iterator.next().equals( (E) obj ) ) {
                iterator.remove(); // removes the last entry that was iterated over
                return true;       // a remove operation was performed, so return true
            }
        }

        // the iterator has gone through the list & has not found the passed
        // in object so return false
        return false;
    }

    /**
     * Removes the entry at the specified position in the double linked list.
     * 
     * @param position The position of the entry to be removed.
     * @return The entry at the specified position.
     */
    public E remove( int position ) {

        iterator = listIterator( position );
        E removed = iterator.next();
        iterator.remove();

        return removed;
    }

    /**
     * Returns the size of the linked list.
     * 
     * @return The size of the list.
     */
    public int size() {
        return this.size;
    }

    /**
     * Outputs the contents of the double linked list.
     * 
     * @return Returns a string that contains the contents of the list.
     */
    public String toString() {

        // reset the iterator to the first in the list
        iterator = listIterator( 0 );

        String s = ""; // stores the elements in the string
        int position = 0; // the current position in the list

        // iterate through the list 
        while( iterator.hasNext() ) {
            // store the position index & the element at that location
            s += position + ": " + iterator.next() + "\n";
            position++;
        }

        return s;
    }
}
