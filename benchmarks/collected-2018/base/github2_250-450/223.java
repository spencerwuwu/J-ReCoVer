// https://searchcode.com/api/result/92665033/

package org.opensat.data.simple;

import java.util.Iterator;
import java.util.List;

import org.opensat.data.IClause;
import org.opensat.data.ILiteral;

/**
 * @author ines
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ClauseSimpleImplWL extends ClauseSimpleImplArray {

	// two watched literals id
	private int wl1;
	private int wl2;

  
    /**
     * @param lits
     * @param size
     */
    public ClauseSimpleImplWL(ILiteral[] lits, int size) {
        super(lits, size);
        assert literals.length >= 2;
        wl1=0;
        wl2=1;
    }

	/**
	 * Constructor for ClauseSimpleImplWL.
	 * @param l
	 */
	public ClauseSimpleImplWL(List l) {
		this(l, false);
	}


	/**
	 * Constructor for ClauseSimpleImplWL.
	 * @param l
	 */
	public ClauseSimpleImplWL(List l, boolean learned) {
		super(
			l,
			(l.size() == 1) ? new ILiteral[2] : new ILiteral[l.size()],
			learned);
		assert literals.length > 1:literals.length;
		if (l.size() == 1) {
			assert literals[0] != null;
			assert literals[1] == null;
			literals[1] = FalsifiedLiteral.FALSIFIED_LITERAL;
		}
		assert literals.length >= 2;
		wl1 = 0;
		wl2 = 1;
	}
    
    
	/**
	 * @see org.opensat.data.AbstractClause#createClause(List)
	 */
	protected IClause createClause(List l) {
		return new ClauseSimpleImplWL(l, true);
	}

    
	/**
	 * @see org.opensat.IClause#firstUnassignedLiteral()
	 */
	public ILiteral firstUnassignedLiteral() {
		assert !isSatisfied();
		return literals[wl1].isUnassigned() ? literals[wl1] : literals[wl2];
	}

	/**
	 * @see org.opensat.IClause#isBinary()
	 */
	public boolean isBinary() {
		// throw new UnsupportedOperationException();
		return !isSatisfied() && countUnassignedLiterals() == 2;
	}

	/**
	 * @see org.opensat.IClause#isFalsified()
	 */
	public boolean isFalsified() {
		boolean result =
			literals[wl1].isFalsified() && literals[wl2].isFalsified();
		assert result
			== ((countUnassignedLiterals() == 0) && !isSatisfied()) : "result"
				+ result
				+ " counted"
				+ countUnassignedLiterals();
		return result;
	}

	/**
	 * @see org.opensat.IClause#isNull()
	 */
	public boolean isNull() {
		return isFalsified();
	}

	/**
	 * @see org.opensat.IClause#isSatisfied()
	 */
	public boolean isSatisfied() {
		// Beware here: it is incomplete. Satisfied literals may not be watched.
		// return literals[wl1].isSatisfied()||literals[wl2].isSatisfied();
		for (int i = 0; i < literals.length; i++) {
			if (literals[i].isSatisfied()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @see org.opensat.IClause#isSatisfiedByOnlyOneLiteral()
	 */
	public boolean isSatisfiedByOnlyOneLiteral() {
		return countSatisfiedLiterals() == 1;
	}

	/**
	 * @see org.opensat.IClause#isUnit()
	 */
	public boolean isUnit() {
//		if (isSatisfied())
//			return false;
		boolean result =
			literals[wl1].isFalsified()
				&& literals[wl2].isUnassigned()
				|| literals[wl1].isUnassigned()
				&& literals[wl2].isFalsified();
		assert((countUnassignedLiterals() == 1) && !isSatisfied())
			== result : " res" + result + "#count" + countUnassignedLiterals();
		return result;
	}

	private int lastFalsfiedLiteral() {
		int max = -1;
		int maxi = -1;
		for (int i = 0; i < literals.length; i++) {
			if (max < literals[i].getDecisionLevel()) {
				max = literals[i].getDecisionLevel();
				maxi = i;
			}
		}
		assert maxi != -1;
		return maxi;
	}

	private int nextNonFalsifiedLiteral(int from, int avoid) {
		int index = from + 1;
		if (index == literals.length) {
			index = 0;
		}
		while ((literals[index].isFalsified() || (index == avoid))
			&& (index != from)) {
			index++;
			if (index == literals.length) {
				index = 0;
			}
		}
		assert !literals[index].isFalsified() || (from == index);
		assert index != avoid;
		return index;
	}

	/**
	 * @see org.opensat.IClause#reduce(org.opensat.ILiteral)
	 */
	public void reduce(ILiteral l, Iterator it) {
		assert l.isFalsified();
		// find out new unassigned literal.
		// 1) a satsified literal is met, watch it and stop

		// is l wl1 or wl2?
		// FIXME
		if (literals[wl1].equals(l)) {
			assert literals[wl1].isFalsified();
			int index = nextNonFalsifiedLiteral(wl1, wl2);
			if (index != wl1) {
				it.remove();
				literals[index].watch(this);
				wl1 = index;
			}			
		} else {
			assert literals[wl2].equals(l);
			assert literals[wl2].isFalsified();
			int index = nextNonFalsifiedLiteral(wl2, wl1);
			if (index != wl2) {
				it.remove();
				literals[index].watch(this);
				wl2 = index;
			}
		}
		assert wl1 != wl2;
	}

	/**
	 * @see org.opensat.IClause#registerToLiterals()
	 */
	public void registerToLiterals() {		
		for(int i=0;i<literals.length;i++) {
			literals[i].register(this);
			if (literals[i].isSatisfied()) {
				satisfied++;
				unassigned--;
			} else if (literals[i].isFalsified()) {
				unassigned--;
			}
		}
		assert wl1==0:"wl1:"+wl1;
        assert wl2 == 1 : "wl2:" + wl2;
		if (originalSize() > 2) {
			if (literals[wl1].isFalsified()) {
				wl1 = nextNonFalsifiedLiteral(wl1, wl2);
				if (literals[wl1].isFalsified()) {
					wl1 = lastFalsfiedLiteral();
				}
			}
			if (literals[wl2].isFalsified()) {
				wl2 = nextNonFalsifiedLiteral(wl2, wl1);
				if (literals[wl2].isFalsified()) {
					wl2 = lastFalsfiedLiteral();
				}
			}
		}
		assert wl1 != wl2;
		literals[wl1].watch(this);
		literals[wl2].watch(this);
		registered = true;
	}

	/**
	 * @see org.opensat.IClause#restore(org.opensat.ILiteral)
	 */
	public void restore(ILiteral l) {
	}

	/**
	 * @see org.opensat.IClause#satisfy(org.opensat.ILiteral)
	 */
	public void satisfy(ILiteral l) {
	}

	/**
	 * @see org.opensat.IClause#size()
	 */
	public int size() {
		//FIXME
		return countUnassignedLiterals();
	}

	/**
	 * @see org.opensat.IClause#unassign(org.opensat.ILiteral)
	 */
	public void unassign(ILiteral l) {
	}

}

