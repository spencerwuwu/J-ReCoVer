// https://searchcode.com/api/result/12374672/

package org.eclipse.jface.text;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.jface.text.AbstractLineTracker.DelimiterInfo;


/**
 * Abstract implementation of <code>ILineTracker</code>. It lets the definition of line
 * delimiters to subclasses. Assuming that '\n' is the only line delimiter, this abstract
 * implementation defines the following line scheme:
 * <ul>
 * <li> "" -> [0,0]
 * <li> "a" -> [0,1]
 * <li> "\n" -> [0,1], [1,0]
 * <li> "a\n" -> [0,2], [2,0]
 * <li> "a\nb" -> [0,2], [2,1]
 * <li> "a\nbc\n" -> [0,2], [2,3], [5,0]
 * </ul>
 * <p>
 * This class must be subclassed.
 * </p>
 * 
 * @since 3.2
 */
abstract class TreeLineTracker implements ILineTracker {
	/*
	 * Differential Balanced Binary Tree
	 * 
	 * Assumption: lines cannot overlap => there exists a total ordering of the lines by their offset,
	 * which is the same as the ordering by line number
	 * 
	 * Base idea: store lines in a binary search tree
	 *   - the key is the line number / line offset
	 *     -> lookup_line is O(log n)
	 *     -> lookup_offset is O(log n)
	 *   - a change in a line somewhere will change any succeeding line numbers / line offsets 
	 *     -> replace is O(n)
	 *      
	 * Differential tree: instead of storing the key (line number, line offset) directly, every node
	 * stores the difference between its key and its parent's key
	 *   - the sort key is still the line number / line offset, but it remains "virtual"
	 *   - inserting a node (a line) really increases the virtual key of all succeeding nodes (lines), but this
	 *     fact will not be realized in the key information encoded in the nodes. 
	 *     -> any change only affects the nodes in the node's parent chain, although more bookkeeping
	 *         has to be done when changing a node or balancing the tree 
	 *        -> replace is O(log n)
	 *     -> line offsets and line numbers have to be computed when walking the tree from the root / 
	 *         from a node
	 *        -> still O(log n)
	 * 
	 * The balancing algorithm chosen does not depend on the differential tree property. An AVL tree
	 * implementation has been chosen for simplicity.
	 */
	
	/*
	 * Turns assertions on/off. Don't make this a a debug option for performance reasons - this way
	 * the compiler can optimize the asserts away.
	 */
	private static final boolean ASSERT= false;
	
	/**
	 * The empty delimiter of the last line. The last line and only the last line must have this
	 * zero-length delimiter.
	 */
	private static final String NO_DELIM= ""; //$NON-NLS-1$
	
	/**
	 * A node represents one line. Its character and line offsets are 0-based and relative to the
	 * subtree covered by the node. All nodes under the left subtree represent lines before, all
	 * nodes under the right subtree lines after the current node.
	 */
	private static final class Node {
		Node(int length, String delimiter) {
			this.length= length;
			this.delimiter= delimiter;
		}
		/**
		 * The line index in this node's line tree, or equivalently, the number of lines in the left
		 * subtree.
		 */
		int line;
		/**
		 * The line offset in this node's line tree, or equivalently, the number of characters in
		 * the left subtree.
		 */
		int offset;
		/** The number of characters in this line. */
		int length;
		/** The line delimiter of this line, needed to answer the delimiter query. */
		String delimiter;
		/** The parent node, <code>null</code> if this is the root node. */
		Node parent;
		/** The left subtree, possibly <code>null</code>. */
		Node left;
		/** The right subtree, possibly <code>null</code>. */
		Node right;
		/** The balance factor. */
		byte balance;
		
		/*
		 * @see java.lang.Object#toString()
		 */
		public final String toString() {
			String bal;
			switch (balance) {
				case 0:
					bal= "="; //$NON-NLS-1$
					break;
				case 1:
					bal= "+"; //$NON-NLS-1$
					break;
				case 2:
					bal= "++"; //$NON-NLS-1$
					break;
				case -1:
					bal= "-"; //$NON-NLS-1$
					break;
				case -2:
					bal= "--"; //$NON-NLS-1$
					break;
				default:
					bal= Byte.toString(balance);
			}
			return "[" + offset + "+" + pureLength() + "+" + delimiter.length() + "|" + line + "|" + bal + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		}

		/**
		 * Returns the pure (without the line delimiter) length of this line.
		 * 
		 * @return the pure line length
		 */
		int pureLength() {
			return length - delimiter.length();
		}
	}

	/**
	 * The root node of the tree, never <code>null</code>.
	 */
	private Node fRoot= new Node(0, NO_DELIM);

	/*
	 * Hints storing a line's offset / line index. Replace with a thread-local cache for multi
	 * threading.
	 */
	/**
	 * The line offset of the line last queried with {@link #nodeByOffset(int, int[])}.
	 */
//	private int lineHint;
	/**
	 * The character offset of the line last queried with  {@link #nodeByOffset(int, int[])} or
	 * {@link #nodeByLine(int, int[])}.
	 */
//	private int offsetHint;
	
	/**
	 * Creates a new line tracker.
	 */
	protected TreeLineTracker() {
	}
	
	/**
	 * Package visible constructor for creating a tree tracker from a list tracker.
	 * 
     * @param tracker
     */
    TreeLineTracker(ListLineTracker tracker) {
    	final List lines= tracker.getLines();
    	final int n= lines.size();
    	if (n == 0)
    		return;
    	
    	Line line= (Line) lines.get(0);
    	String delim= line.delimiter;
    	if (delim == null)
    		delim= NO_DELIM;
    	int length= line.length;
    	fRoot= new Node(length, delim);
    	Node node= fRoot;
    	
		for (int i= 1; i < n; i++) {
	        line= (Line) lines.get(i);
	        delim= line.delimiter;
	        if (delim == null)
	        	delim= NO_DELIM;
	        length= line.length;
			node= insertAfter(node, length, delim);
        }
		
		if (node.delimiter != NO_DELIM)
			insertAfter(node, 0, NO_DELIM);
		
		if (ASSERT) checkTree();
    }

	/**
	 * Returns the node (line) including a certain offset. <code>hints[0]</code> is set to the offset
	 * and <code>hints[1]</code> to the line number of the returned line. If the offset is between two
	 * lines, the line starting at <code>offset</code> is returned.
	 * <p>
	 * This means that for offsets smaller than the length, the following holds:
	 * </p>
	 * <p>
	 * <code>line.offset <= offset < line.offset + offset.length</code>.
	 * </p>
	 * <p>
	 * If <code>offset</code> is the document length, then this is true:
	 * </p>
	 * <p>
	 * <code>offset= line.offset + line.length</code>.
	 * </p>
	 * 
	 * @param offset a document offset
	 * @param hints the hint array with offset and line number
	 * @return the line starting at or containing <code>offset</code>
	 * @throws BadLocationException if the offset is invalid
	 */
	private Node nodeByOffset(final int offset, int[] hints) throws BadLocationException {
		/*
		 * Works for any binary search tree.
		 */
		int remaining= offset;
		Node node= fRoot;
		int line= 0;
		
		while (true) {
			if (node == null)
				fail(offset);
			
			if (remaining < node.offset) {
				node= node.left;
			} else {
				remaining -= node.offset;
				line+= node.line;
				if (remaining < node.length
						|| remaining == node.length && node.right == null) { // last line
					hints[0]= offset - remaining;
					hints[1]= line;
					return node;
				}
				remaining -= node.length;
				line ++;
				node= node.right;
			}
		}
	}
	/**
	 * Returns the line number for the given offset. If the offset is between
	 * two lines, the line starting at <code>offset</code> is returned.
	 * <p>
	 * This means that for offsets smaller than the length, the following holds:
	 * </p>
	 * <p>
	 * <code>line.offset <= offset < line.offset + offset.length</code>.
	 * </p>
	 * <p>
	 * If <code>offset</code> is the document length, then this is true:
	 * </p>
	 * <p>
	 * <code>offset= line.offset + line.length</code>.
	 * </p>
	 * 
	 * @param offset a document offset
	 * @return the line number starting at or containing <code>offset</code>
	 * @throws BadLocationException if the offset is invalid
	 */
	private int lineOfOffset(final int offset) throws BadLocationException {
		/*
		 * Works for any binary search tree.
		 */
		int remaining= offset;
		Node node= fRoot;
		int line= 0;
		
		while (true) {
			if (node == null)
				fail(offset);
			
			if (remaining < node.offset) {
				node= node.left;
			} else {
				remaining -= node.offset;
				line+= node.line;
				if (remaining < node.length	|| remaining == node.length && node.right == null) // last line
					return  line;

				remaining -= node.length;
				line ++;
				node= node.right;
			}
		}
	}
	
	/**
	 * Returns the node (line) with the given line number. <code>hints[0]</code> is set to the offset
	 * of the returned line. Note that the last line is always incomplete, i.e. has the
	 * {@link #NO_DELIM} delimiter.
	 * 
	 * @param line a line number
	 * @param offsetHint the offset hint array
	 * @return the line with the given line number
	 * @throws BadLocationException if the line is invalid
	 */
	private Node nodeByLine(final int line, int[] offsetHint) throws BadLocationException {
		/*
		 * Works for any binary search tree.
		 */
		int remaining= line;
		int offset= 0;
		Node node= fRoot;
		
		while (true) {
			if (node == null)
				fail(line);
			
			if (remaining == node.line) {
				if (offsetHint != null)
					offsetHint[0]= offset + node.offset;
				return node;
			}
			if (remaining < node.line) {
				node= node.left;
			} else {
				remaining -= node.line + 1;
				offset += node.offset + node.length;
				node= node.right;
			}
		}
	}
	
	/**
	 * Returns the offset for the given line number. Note that the
	 * last line is always incomplete, i.e. has the {@link #NO_DELIM} delimiter.
	 * 
	 * @param line a line number
	 * @return the line offset with the given line number
	 * @throws BadLocationException if the line is invalid
	 */
	private int offsetByLine(final int line) throws BadLocationException {
		/*
		 * Works for any binary search tree.
		 */
		int remaining= line;
		int offset= 0;
		Node node= fRoot;
		
		while (true) {
			if (node == null)
				fail(line);
			
			if (remaining == node.line)
				return offset + node.offset;

			if (remaining < node.line) {
				node= node.left;
			} else {
				remaining -= node.line + 1;
				offset += node.offset + node.length;
				node= node.right;
			}
		}
	}
	
	/**
	 * Left rotation - the given node is rotated down, its right child is rotated up, taking the
	 * previous structural position of <code>node</code>.
	 * 
	 * @param node the node to rotate around
	 */
	private void rotateLeft(Node node) {
		Node child= node.right;

		boolean leftChild= node.parent == null || node == node.parent.left;
		
		// restructure
		setChild(node.parent, child, leftChild);
		
		setChild(node, child.left, false);
		setChild(child, node, true);
		
		// update relative info
		// child becomes the new parent, its line and offset counts increase as the former parent
		// moves under child's left subtree
		child.line += node.line + 1;
		child.offset += node.offset + node.length;
	}

	/**
	 * Right rotation - the given node is rotated down, its left child is rotated up, taking the
	 * previous structural position of <code>node</code>.
	 * 
	 * @param node the node to rotate around
	 */
	private void rotateRight(Node node) {
		Node child= node.left;

		boolean leftChild= node.parent == null || node == node.parent.left;
		
		setChild(node.parent, child, leftChild);
		
		setChild(node, child.right, true);
		setChild(child, node, false);
		
		// update relative info
		// node loses its left subtree, except for what it keeps in its new subtree
		// this is exactly the amount in child
		node.line -= child.line + 1;
		node.offset -= child.offset + child.length;
	}

	/**
	 * Helper method for moving a child, ensuring that parent pointers are set correctly.
	 * 
	 * @param parent the new parent of <code>child</code>, <code>null</code> to replace the
	 *        root node
	 * @param child the new child of <code>parent</code>, may be <code>null</code>
	 * @param isLeftChild <code>true</code> if <code>child</code> shall become
	 *        <code>parent</code>'s left child, <code>false</code> if it shall become
	 *        <code>parent</code>'s right child
	 */
	private void setChild(Node parent, Node child, boolean isLeftChild) {
		if (parent == null) {
			if (child == null)
				fRoot= new Node(0, NO_DELIM);
			else
				fRoot= child;
		} else {
			if (isLeftChild)
				parent.left= child;
			else
				parent.right= child;
		}
		if (child != null)
			child.parent= parent;
	}
	
	/**
	 * A left rotation around <code>parent</code>, whose structural position is replaced by
	 * <code>node</code>.
	 * 
	 * @param node the node moving up and left
	 * @param parent the node moving left and down
	 */
	private void singleLeftRotation(Node node, Node parent) {
		rotateLeft(parent);
		node.balance= 0;
		parent.balance= 0;
	}

	/**
	 * A right rotation around <code>parent</code>, whose structural position is replaced by
	 * <code>node</code>.
	 * 
	 * @param node the node moving up and right
	 * @param parent the node moving right and down
	 */
	private void singleRightRotation(Node node, Node parent) {
		rotateRight(parent);
		node.balance= 0;
		parent.balance= 0;
	}

	/**
	 * A double left rotation, first rotating right around <code>node</code>, then left around
	 * <code>parent</code>.
	 * 
	 * @param node the node that will be rotated right
	 * @param parent the node moving left and down
	 */
	private void rightLeftRotation(Node node, Node parent) {
		Node child= node.left;
		rotateRight(node);
		rotateLeft(parent);
		if (child.balance == 1) {
			node.balance= 0;
			parent.balance= -1;
			child.balance= 0;
		} else if (child.balance == 0) {
			node.balance= 0;
			parent.balance= 0;
		} else if (child.balance == -1) {
			node.balance= 1;
			parent.balance= 0;
			child.balance= 0;
		}
	}

	/**
	 * A double right rotation, first rotating left around <code>node</code>, then right around
	 * <code>parent</code>.
	 * 
	 * @param node the node that will be rotated left
	 * @param parent the node moving right and down
	 */
	private void leftRightRotation(Node node, Node parent) {
		Node child= node.right;
		rotateLeft(node);
		rotateRight(parent);
		if (child.balance == -1) {
			node.balance= 0;
			parent.balance= 1;
			child.balance= 0;
		} else if (child.balance == 0) {
			node.balance= 0;
			parent.balance= 0;
		} else if (child.balance == 1) {
			node.balance= -1;
			parent.balance= 0;
			child.balance= 0;
		}
	}

	/**
	 * Inserts a line with the given length and delimiter after <code>node</code>.
	 * 
	 * @param node the predecessor of the inserted node
	 * @param length the line length of the inserted node
	 * @param delimiter the delimiter of the inserted node
	 * @return the inserted node
	 */
	private Node insertAfter(Node node, int length, String delimiter) {
		/*
		 * An insertion really shifts the key of all succeeding nodes. Hence we insert the added node 
		 * between node and the successor of node. The added node becomes either the right child
		 * of the predecessor node, or the left child of the successor node.
		 */
		Node added= new Node(length, delimiter);
		
		if (node.right == null)
			setChild(node, added, false);
		else
			setChild(successorDown(node.right), added, true);
		
		// parent chain update
		updateParentChain(added, length, 1);
		updateParentBalanceAfterInsertion(added);
		
		return added;
	}
	
	/**
	 * Updates the balance information in the parent chain of node until it reaches the root or
	 * finds a node whose balance violates the AVL constraint, which is the re-balanced.
	 * 
	 * @param node the child of the first node that needs balance updating
	 */
	private void updateParentBalanceAfterInsertion(Node node) {
		Node parent= node.parent;
		while (parent != null) {
			if (node == parent.left)
				parent.balance--;
			else
				parent.balance++;

			switch (parent.balance) {
				case 1:
				case -1:
					node= parent;
					parent= node.parent;
					continue;
				case -2:
					rebalanceAfterInsertionLeft(node);
					break;
				case 2:
					rebalanceAfterInsertionRight(node);
					break;
				case 0:
					break;
				default:

			}
			return;
		}
	}
	
	/**
	 * Re-balances a node whose parent has a double positive balance.
	 * 
	 * @param node the node to re-balance
	 */
	private void rebalanceAfterInsertionRight(Node node) {
		Node parent= node.parent;
		if (node.balance == 1) {
			singleLeftRotation(node, parent);
		} else if (node.balance == -1) {
			rightLeftRotation(node, parent);
		} else if (ASSERT) {

		}
	}

	/**
	 * Re-balances a node whose parent has a double negative balance.
	 * 
	 * @param node the node to re-balance
	 */
	private void rebalanceAfterInsertionLeft(Node node) {
		Node parent= node.parent;
		if (node.balance == -1) {
			singleRightRotation(node, parent);
		} else if (node.balance == 1) {
			leftRightRotation(node, parent);
		} else if (ASSERT) {

		}
	}

	/*
	 * @see org.eclipse.jface.text.ILineTracker#replace(int, int, java.lang.String)
	 */
	public final void replace(int offset, int length, String text) throws BadLocationException {
		if (ASSERT) checkTree();
		
		int[] hints= new int[2];
		Node first= nodeByOffset(offset, hints);

		int firstNodeOffset= hints[0];
		
		Node last;
		if (offset + length < firstNodeOffset + first.length)
			last= first;
		else
			last= nodeByOffset(offset + length, hints);
		
		int firstLineDelta= firstNodeOffset + first.length - offset;
		if (first == last)
			replaceInternal(first, text, length, firstLineDelta);
		else
			replaceFromTo(first, last, text, length, firstLineDelta);

		if (ASSERT) checkTree();
	}

	/**
	 * Replace happening inside a single line.
	 * 
	 * @param node the affected node
	 * @param text the added text
	 * @param length the replace length, &lt; <code>firstLineDelta</code>
	 * @param firstLineDelta the number of characters from the replacement offset to the end of
	 *        <code>node</code> &gt; <code>length</code>
	 */
	private void replaceInternal(Node node, String text, int length, int firstLineDelta) {
		// 1) modification on a single line
		
		DelimiterInfo info= text == null ? null : nextDelimiterInfo(text, 0);

		if (info == null || info.delimiter == null) {
			// a) trivial case: insert into a single node, no line mangling
			int added= text == null ? 0 : text.length();
			updateLength(node, added - length);
		} else {
			// b) more lines to add between two chunks of the first node
			// remember what we split off the first line
			int remainder= firstLineDelta - length;
			String remDelim= node.delimiter;
			
			// join the first line with the first added
			int firstLength= info.delimiterIndex + info.delimiterLength;
			int delta= firstLength - firstLineDelta;
			updateLength(node, delta);
			node.delimiter= info.delimiter;

			int[] consumed= new int[1];
			node= addLines(node, text, firstLength, consumed);

			// add remaining chunk merged with last (incomplete) additional line
			insertAfter(node, remainder + text.length() - consumed[0], remDelim);
		}
	}
	
	/**
	 * Replace spanning from one node to another.
	 * 
	 * @param node the first affected node
	 * @param last the last affected node
	 * @param text the added text
	 * @param length the replace length, &gt;= <code>firstLineDelta</code>
	 * @param firstLineDelta the number of characters removed from the replacement offset to the end
	 *        of <code>node</code>, &lt;= <code>length</code>
	 */
	private void replaceFromTo(Node node, Node last, String text, int length, int firstLineDelta) {
		// 2) modification covers several lines
		
		// delete intermediate nodes
		// TODO could be further optimized: replace intermediate lines with intermediate added lines
		// to reduce re-balancing
		Node successor= successor(node);
		while (successor != last) {
			length -= successor.length;
			Node toDelete= successor;
			successor= successor(successor);
			updateLength(toDelete, -toDelete.length);
		}

		DelimiterInfo info= text == null ? null : nextDelimiterInfo(text, 0);

		if (info == null || info.delimiter == null) {
			int added= text == null ? 0 : text.length();

			// join the two lines if there are no lines added
			join(node, last, added - length);
			
		} else {
			
			// join the first line with the first added
			int firstLength= info.delimiterIndex + info.delimiterLength;
			updateLength(node, firstLength - firstLineDelta);
			node.delimiter= info.delimiter;
			length -= firstLineDelta;

			int[] consumed= new int[1];
			addLines(node, text, firstLength, consumed);
			
			// merge last (incomplete) line with with last node
			updateLength(last, text.length() - consumed[0] - length);
		}
	}

	/**
	 * Inserts the intermediate lines in added.lengths, starting at 1, up to but not including the last.
	 * 
	 * @param node the node to insert after
	 * @param text the added text
	 * @param offset the (relative) offset into text that has been consumed
	 * @param consumed out parameter holding the amount of characters consumed from <code>text</code>
	 * @return the last inserted node, the originally passed node if no line was added
	 */
	private Node addLines(Node node, String text, int offset, int[] consumed) {
		DelimiterInfo info= nextDelimiterInfo(text, offset);
		while (info != null) {
			int length= info.delimiterIndex - offset + info.delimiterLength;
			node= insertAfter(node, length, info.delimiter);
			offset += length;
			info= nextDelimiterInfo(text, offset);
		}
		consumed[0]= offset;
		return node;
	}
	
	/**
	 * Joins two consecutive node lines, additionally adjusting the resulting length of the combined
	 * line by <code>delta</code>. The first node gets deleted.
	 * 
	 * @param one the first node to join
	 * @param two the second node to join
	 * @param delta the delta to apply to the remaining single node
	 */
	private void join(Node one, Node two, int delta) {
		int oneLength= one.length;
		updateLength(one, -oneLength);
		updateLength(two, oneLength + delta);
	}
	
	/**
	 * Adjusts the length of a node by <code>delta</code>, also adjusting the parent chain of
	 * <code>node</code>. If the node's length becomes zero and is not the last (incomplete)
	 * node, it is deleted after the update.
	 * 
	 * @param node the node to adjust
	 * @param delta the character delta to add to the node's length
	 */
	private void updateLength(Node node, int delta) {
		
		// update the node itself
		node.length += delta;
		
		// check deletion
		final int lineDelta;
		boolean delete= node.length == 0 && node.delimiter != NO_DELIM;
		if (delete)
			lineDelta= -1;
		else
			lineDelta= 0;
		
		// update parent chain
		if (delta != 0 || lineDelta != 0)
			updateParentChain(node, delta, lineDelta);
		
		if (delete)
			delete(node);
	}

	/**
	 * Updates the differential indices following the parent chain. All nodes from
	 * <code>from.parent</code> to the root are updated.
	 * 
	 * @param node the child of the first node to update
	 * @param deltaLength the character delta 
	 * @param deltaLines the line delta
	 */
	private void updateParentChain(Node node, int deltaLength, int deltaLines) {
		updateParentChain(node, null, deltaLength, deltaLines);
	}
	
	/**
	 * Updates the differential indices following the parent chain. All nodes from
	 * <code>from.parent</code> to <code>to</code> (exclusive) are updated.
	 * 
	 * @param from the child of the first node to update
	 * @param to the first node not to update
	 * @param deltaLength the character delta 
	 * @param deltaLines the line delta
	 */
	private void updateParentChain(Node from, Node to, int deltaLength, int deltaLines) {
		Node parent= from.parent;
		while (parent != to) {
			// only update node if update comes from left subtree
			if (from == parent.left) {
				parent.offset += deltaLength;
				parent.line += deltaLines;
			}
			from= parent;
			parent= from.parent;
		}
	}
	
	/**
	 * Deletes a node from the tree, re-balancing it if necessary. The differential indices in the
	 * node's parent chain have to be updated in advance to calling this method. Generally, don't
	 * call <code>delete</code> directly, but call
	 * {@link #updateLength(Node, int) update_length(node, -node.length)} to properly remove a
	 * node.
	 * 
	 * @param node the node to delete.
	 */
	private void delete(Node node) {
		
		Node parent= node.parent;
		Node toUpdate; // the parent of the node that lost a child
		boolean lostLeftChild;
		boolean isLeftChild= parent == null || node == parent.left;
		
		if (node.left == null || node.right == null) {
			// 1) node has one child at max - replace parent's pointer with the only child
			// also handles the trivial case of no children
			Node replacement= node.left == null ? node.right : node.left;
			setChild(parent, replacement, isLeftChild);
			toUpdate= parent;
			lostLeftChild= isLeftChild;
			// no updates to do - subtrees stay as they are
		} else if (node.right.left == null) {
			// 2a) node's right child has no left child - replace node with right child, giving node's
			// left subtree to the right child
			Node replacement= node.right;
			setChild(parent, replacement, isLeftChild);
			setChild(replacement, node.left, true);
			replacement.line= node.line;
			replacement.offset= node.offset;
			replacement.balance= node.balance;
			toUpdate= replacement;
			lostLeftChild= false;
//		} else if (node.left.right == null) {
//			// 2b) symmetric case
//			Node replacement= node.left;
//			set_child(parent, replacement, isLeftChild);
//			set_child(replacement, node.right, false);
//			replacement.balance= node.balance;
//			toUpdate= replacement;
//			lostLeftChild= true;
		} else {
			// 3) hard case - replace node with its successor
			Node successor= successor(node);

			toUpdate= successor.parent;
			lostLeftChild= true;

			// update relative indices
			updateParentChain(successor, node, -successor.length, -1);
			
			// delete successor from its current place - like 1)
			setChild(toUpdate, successor.right, true);

			// move node's subtrees to its successor
			setChild(successor, node.right, false);
			setChild(successor, node.left, true);
			
			// replace node by successor in its parent
			setChild(parent, successor, isLeftChild);
			
			// update the successor
			successor.line= node.line;
			successor.offset= node.offset;
			successor.balance= node.balance;
		}
		
		updateParentBalanceAfterDeletion(toUpdate, lostLeftChild);
	}
	
	/**
	 * Updates the balance information in the parent chain of node.
	 * 
	 * @param node the first node that needs balance updating
	 * @param wasLeftChild <code>true</code> if the deletion happened on <code>node</code>'s
	 *        left subtree, <code>false</code> if it occurred on <code>node</code>'s right
	 *        subtree
	 */
	private void updateParentBalanceAfterDeletion(Node node, boolean wasLeftChild) {
		while (node != null) {
			if (wasLeftChild)
				node.balance++;
			else
				node.balance--;
			
			Node parent= node.parent;
			if (parent != null)
				wasLeftChild= node == parent.left;

			switch (node.balance) {
				case 1:
				case -1:
					return; // done, no tree change
				case -2:
					if (rebalanceAfterDeletionRight(node.left))
						return;
					break; // propagate up
				case 2:
					if (rebalanceAfterDeletionLeft(node.right))
						return;
					break; // propagate up
				case 0:
					break; // propagate up
				default:
					break;
			}
			
			node= parent;
		}
	}
	
	/**
	 * Re-balances a node whose parent has a double positive balance.
	 * 
	 * @param node the node to re-balance
	 * @return <code>true</code> if the re-balancement leaves the height at
	 *         <code>node.parent</code> constant, <code>false</code> if the height changed
	 */
	private boolean rebalanceAfterDeletionLeft(Node node) {
		Node parent= node.parent;
		if (node.balance == 1) {
			singleLeftRotation(node, parent);
			return false;
		} else if (node.balance == -1) {
			rightLeftRotation(node, parent);
			return false;
		} else if (node.balance == 0) {
			rotateLeft(parent);
			node.balance= -1;
			parent.balance= 1;
			return true;
		} else {
			return true;
		}
	}

	/**
	 * Re-balances a node whose parent has a double negative balance.
	 * 
	 * @param node the node to re-balance
	 * @return <code>true</code> if the re-balancement leaves the height at
	 *         <code>node.parent</code> constant, <code>false</code> if the height changed
	 */
	private boolean rebalanceAfterDeletionRight(Node node) {
		Node parent= node.parent;
		if (node.balance == -1) {
			singleRightRotation(node, parent);
			return false;
		} else if (node.balance == 1) {
			leftRightRotation(node, parent);
			return false;
		} else if (node.balance == 0) {
			rotateRight(parent);
			node.balance= 1;
			parent.balance= -1;
			return true;
		} else {
			return true;
		}
	}

	/**
	 * Returns the successor of a node, <code>null</code> if node is the last node.
	 * 
	 * @param node a node
	 * @return the successor of <code>node</code>, <code>null</code> if there is none
	 */
	private Node successor(Node node) {
		if (node.right != null)
			return successorDown(node.right);
		
		return successorUp(node);
	}
	
	/**
	 * Searches the successor of <code>node</code> in its parent chain.
	 * 
	 * @param node a node
	 * @return the first node in <code>node</code>'s parent chain that is reached from its left
	 *         subtree, <code>null</code> if there is none
	 */
	private Node successorUp(final Node node) {
		Node child= node;
		Node parent= child.parent;
		while (parent != null) {
			if (child == parent.left)
				return parent;
			child= parent;
			parent= child.parent;
		}
		return null;
	}

	/**
	 * Searches the left-most node in a given subtree.
	 * 
	 * @param node a node
	 * @return the left-most node in the given subtree
	 */
	private Node successorDown(Node node) {
		Node child= node.left;
		while (child != null) {
			node= child;
			child= node.left;
		}
		return node;
	}

	/* miscellaneous */

	/**
	 * Throws an exception.
	 * 
	 * @param offset the illegal character or line offset that caused the exception
	 * @throws BadLocationException always
	 */
	private void fail(int offset) throws BadLocationException {
		throw new BadLocationException();
	}
	
	/**
	 * Returns the information about the first delimiter found in the given
	 * text starting at the given offset.
	 *
	 * @param text the text to be searched
	 * @param offset the offset in the given text
	 * @return the information of the first found delimiter or <code>null</code>
	 */
	protected abstract DelimiterInfo nextDelimiterInfo(String text, int offset);
	
	/*
	 * @see org.eclipse.jface.text.ILineTracker#getLineDelimiter(int)
	 */
	public final String getLineDelimiter(int line) throws BadLocationException {
		Node node= nodeByLine(line, null);
		return node.delimiter == NO_DELIM ? null : node.delimiter;
	}

	/*
	 * @see org.eclipse.jface.text.ILineTracker#computeNumberOfLines(java.lang.String)
	 */
	public final int computeNumberOfLines(String text) {
		int count= 0;
		int start= 0;
		DelimiterInfo delimiterInfo= nextDelimiterInfo(text, start);
		while (delimiterInfo != null && delimiterInfo.delimiterIndex > -1) {
			++count;
			start= delimiterInfo.delimiterIndex + delimiterInfo.delimiterLength;
			delimiterInfo= nextDelimiterInfo(text, start);
		}
		return count;
	}

	/*
	 * @see org.eclipse.jface.text.ILineTracker#getNumberOfLines()
	 */
	public final int getNumberOfLines() {
		// TODO track separately?
		Node node= fRoot;
		int lines= 0;
		while (node != null) {
			lines += node.line + 1;
			node= node.right;
		}
		return lines;
	}

	/*
	 * @see org.eclipse.jface.text.ILineTracker#getNumberOfLines(int, int)
	 */
	public final int getNumberOfLines(int offset, int length) throws BadLocationException {
		if (length == 0)
			return 1;

		int startLine= lineOfOffset(offset);
		int endLine= lineOfOffset(offset + length);
		
		return endLine - startLine + 1;
	}

	/*
	 * @see org.eclipse.jface.text.ILineTracker#getLineOffset(int)
	 */
	public final int getLineOffset(int line) throws BadLocationException {
		return offsetByLine(line);
	}

	/*
	 * @see org.eclipse.jface.text.ILineTracker#getLineLength(int)
	 */
	public final int getLineLength(int line) throws BadLocationException {
		Node node= nodeByLine(line, null);
		return node.length;
	}

	/*
	 * @see org.eclipse.jface.text.ILineTracker#getLineNumberOfOffset(int)
	 */
	public final int getLineNumberOfOffset(int offset) throws BadLocationException {
		return lineOfOffset(offset);
	}

	/*
	 * @see org.eclipse.jface.text.ILineTracker#getLineInformationOfOffset(int)
	 */
	public final IRegion getLineInformationOfOffset(int offset) throws BadLocationException {
		int[] hints= new int[2];
		Node node= nodeByOffset(offset, hints);
		return new Region(hints[0], node.pureLength());
	}

	/*
	 * @see org.eclipse.jface.text.ILineTracker#getLineInformation(int)
	 */
	public final IRegion getLineInformation(int line) throws BadLocationException {
		int[] offsetHint= new int[1];
		try {
			Node node= nodeByLine(line, offsetHint);
			return new Region(offsetHint[0], node.pureLength());
		} catch (BadLocationException x) {
			/*
			 * FIXME: this really strange behavior is mandated by the previous line tracker
			 * implementation and included here for compatibility. See
			 * LineTrackerTest3#testFunnyLastLineCompatibility().
			 */
			if (line > 0 && line == getNumberOfLines()) {
				Node last= nodeByLine(line - 1, offsetHint);
				if (last.length > 0)
					return new Region(offsetHint[0] + last.length, 0);
			}
			throw x;
		}
	}

	/*
	 * @see org.eclipse.jface.text.ILineTracker#set(java.lang.String)
	 */
	public final void set(String text) {
		fRoot= new Node(0, NO_DELIM);
		try {
			replace(0, 0, text);
		} catch (BadLocationException x) {
			throw new InternalError();
		}
	}
	
	/*
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		int depth= computeDepth(fRoot);
		int WIDTH= 30;
		int leaves= (int) Math.pow(2, depth - 1);
		int width= WIDTH * leaves;
		String empty= "."; //$NON-NLS-1$
		
		List roots= new LinkedList();
		roots.add(fRoot);
		StringBuffer buf= new StringBuffer((width + 1) * depth);
		int nodes= 1;
		int indents= leaves;
		char[] space= new char[leaves * WIDTH / 2];
		Arrays.fill(space, ' ');
		for(int d= 0; d < depth; d++) {
			// compute indent
			indents /= 2;
			int spaces= Math.max(0, indents * WIDTH - WIDTH / 2);
			// print nodes
			for (ListIterator it= roots.listIterator(); it.hasNext();) {
				// pad before
				buf.append(space, 0, spaces);
				
				Node node= (Node) it.next();
				String box;
				// replace the node with its children
				if (node == null) {
					it.add(null);
					box= empty;
				} else {
					it.set(node.left);
					it.add(node.right);
					box= node.toString();
				}
				
				// draw the node, pad to WIDTH
				int pad_left= (WIDTH - box.length() + 1) / 2;
				int pad_right= WIDTH - box.length() - pad_left;
				buf.append(space, 0, pad_left);
				buf.append(box);
				buf.append(space, 0, pad_right);
				
				// pad after
				buf.append(space, 0, spaces);
			}
			
			buf.append('\n');
			nodes *= 2; 
		}
		
		return buf.toString();
	}

	/**
	 * Recursively computes the depth of the tree. Only used by {@link #toString()}.
	 * 
	 * @param root the subtree to compute the depth of, may be <code>null</code>
	 * @return the depth of the given tree, 0 if it is <code>null</code>
	 */
	private byte computeDepth(Node root) {
		if (root == null)
			return 0;
		
		return (byte) (Math.max(computeDepth(root.left), computeDepth(root.right)) + 1);
	}
	
	/**
	 * Debug-only method that checks the tree structure and the differential offsets.
	 */
	private void checkTree() {
		checkTreeStructure(fRoot);
		
		try {
			checkTreeOffsets(nodeByOffset(0, new int[2]), new int[] {0, 0}, null);
		} catch (BadLocationException x) {
			throw new AssertionError();
		}
	}
	
	/**
	 * Debug-only method that validates the tree structure below <code>node</code>. I.e. it
	 * checks whether all parent/child pointers are consistent and whether the AVL balance
	 * information is correct.
	 * 
	 * @param node the node to validate
	 * @return the depth of the tree under <code>node</code>
	 */
	private byte checkTreeStructure(Node node) {
		if (node == null)
			return 0;
		
		byte leftDepth= checkTreeStructure(node.left);
		byte rightDepth= checkTreeStructure(node.right);
		
		return (byte) (Math.max(rightDepth, leftDepth) + 1);
	}
	
	/**
	 * Debug-only method that checks the differential offsets of the tree, starting at
	 * <code>node</code> and continuing until <code>last</code>.
	 * 
	 * @param node the first <code>Node</code> to check, may be <code>null</code>
	 * @param offLen an array of length 2, with <code>offLen[0]</code> the expected offset of
	 *        <code>node</code> and <code>offLen[1]</code> the expected line of
	 *        <code>node</code>
	 * @param last the last <code>Node</code> to check, may be <code>null</code>
	 * @return an <code>int[]</code> of length 2, with the first element being the character
	 *         length of <code>node</code>'s subtree, and the second element the number of lines
	 *         in <code>node</code>'s subtree
	 */
	private int[] checkTreeOffsets(Node node, int[] offLen, Node last) {
		if (node == last)
			return offLen;
		
		if (node.right != null) {
			int[] result= checkTreeOffsets(successorDown(node.right), new int[2], node);
			offLen[0] += result[0];
			offLen[1] += result[1];
		}
		
		offLen[0] += node.length;
		offLen[1]++;
		return checkTreeOffsets(node.parent, offLen, last);
	}
}
