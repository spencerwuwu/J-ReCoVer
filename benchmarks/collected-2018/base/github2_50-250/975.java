// https://searchcode.com/api/result/70689653/

package com.rogue.algorithms.sort;

import java.util.List;

/**
 * Sorts using the heap sort algorithm.
 * 
 * @author R. Matt McCann
 */
public abstract class HeapSort<T extends Comparable<T>> implements Sorter<T> {
    @Override
    public void sort(List<T> toBeSorted) {
        buildHeap(toBeSorted);
        
        int heapSize = toBeSorted.size();
        for (int nodeIter = toBeSorted.size(); nodeIter > 1; nodeIter--) {
            int rootPos = 0;
            int swapPos = nodeIter - 1;
            
            // Swap the root with the current sub-root
            T swapValue = toBeSorted.remove(swapPos);
            T rootValue = toBeSorted.remove(rootPos);
            toBeSorted.add(rootPos, swapValue);
            toBeSorted.add(swapPos, rootValue);
            
            // One more node at the end of the list has been properly sorted
            // so reduce the size of the heap to be sorted
            heapSize--;
            
            // Sift down the root node
            int root = 1;
            heapify(toBeSorted, root, heapSize);
        }
    }
    
    /**
     * Builds a heap by iteratively applying heapify.
     * 
     * @param toBeHeaped Collection to be sorted. Must not be null.
     */
    protected abstract void buildHeap(final List<T> toBeHeaped);
    
    /**
     * Sorts a given node into the correct position by recursively sifting it
     * down the heap.
     * 
     * @param toBeHeaped Collection to be sorted. Must not be null.
     * @param node Position of the node to be sorted down. Must be 0 <= node <= |toBeHeaped|
     */
    protected abstract void heapify(final List<T> toBeHeaped, 
                                    final int node, 
                                    final int heapSize);
}

