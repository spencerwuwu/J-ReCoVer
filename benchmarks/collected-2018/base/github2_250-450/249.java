// https://searchcode.com/api/result/92665048/

/*
 * The OpenSAT project
 * Copyright (c) 2002, Joao Marques-Silva and Daniel Le Berre
 * 
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * Created on 26 nov. 2002
 *  
 */
package org.opensat.data.simple;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opensat.data.ActiveClauseIterator;
import org.opensat.data.ContradictionFoundException;
import org.opensat.data.ICNF;
import org.opensat.data.IClause;
import org.opensat.data.DequeOfIClause;
import org.opensat.data.ILiteral;
import org.opensat.data.IVocabulary;

/**
 * Class responsability.
 * 
 * @author leberre
 *
 */
public class CNFSimpleImplAlt implements ICNF {

	protected final ArrayList clauses;
	private int maxLengthClause;
	protected final List nullclauses;
	protected DequeOfIClause unitclauses;
	private IVocabulary voc;

	public CNFSimpleImplAlt() {
		this(new VocabularySimpleImpl());
	}

	public CNFSimpleImplAlt(IVocabulary voc) {
		this.voc = voc;
		clauses = new ArrayList();
		unitclauses = null;
		nullclauses = new ArrayList();
	}

	/**
	 * @see org.opensat.ICNF#activeClauseIterator()
	 */
	public Iterator activeClauseIterator() {
		return new ActiveClauseIterator(clauses.iterator());
	}

	/**
	 * @see org.opensat.ICNF#addClause(List)
	 */
	public void addClause(List literals) {
		// detect opposite literal
		ILiteral[] lits = new ILiteral[literals.size()];
		literals.toArray(lits);
		for (int i = 0; i < lits.length; i++) {
			for (int j = i + 1; j < lits.length; j++) {
				if (lits[i].opposite().equals(lits[j])) {
					return;
				}
			}
		}
		// if detected, return.
		// remove duplicate literals
		for (int i = 0; i < lits.length; i++) {
			for (int j = i + 1; j < lits.length;j++) {
				if (lits[i].equals(lits[j])) {
					literals.remove(lits[j]);
				}
			}
		}

		// create the clause
		IClause clause = createClause(literals);
		clauses.add(clause);
		clause.registerToLiterals();
		if (clause.isNull()) {
			nullclauses.add(clause);
		} else if (clause.isUnit()) {
			addUnitClause(clause);
		}
		if (maxLengthClause<clause.size()) {
			maxLengthClause = clause.size();
		}
	}

	/**
	* @see org.opensat.ICNF#beginLoadFormula()
	*/
	public void beginLoadFormula() {
		voc.clear();
		clauses.clear();
		unitclauses=null;
		nullclauses.clear();
		maxLengthClause=-1;
	}

   /**
     * Factory method to create new clauses.
     * 
     * @param literals a list of ILiterals.
     * @return IClause
     */
    public IClause createClause(ILiteral [] lits, int size) {
        return new ClauseSimpleImpl(lits,size);
    }

	/**
	 * Factory method to create new clauses.
	 * 
	 * @param literals a list of ILiterals.
	 * @return IClause
	 */
	public IClause createClause(List literals) {
		return new ClauseSimpleImpl(literals);
	}

	/**
	 * @see org.opensat.ICNF#display(java.io.PrintWriter)
	 */
	public void display(PrintWriter out) {
		out.print("p cnf ");
		out.print(getVocabulary().getMaxVariableId());
		out.print(" ");
		out.println(size());

		Iterator iter = activeClauseIterator();

		while (iter.hasNext())
			 ((IClause) iter.next()).display(out);

		out.flush();
	}

	/**
	* @see org.opensat.ICNF#endLoadFormula()
	*/
	public void endLoadFormula() {
		// put active clauses in a list
		voc.freeze(clauses.iterator());
	}

	/**
	 * @see org.opensat.data.ICNF#flush()
	 */
	public void flush() {
		unitclauses.clear();
		assert nullclauses.isEmpty();
	}

	/**
	 * @see org.opensat.ICNF#getLastNullClause(Iliteral l)
	 */

	public IClause getLastNullClause(ILiteral lit) {

		// I m looking for the first clause null containing lit
		//return (IClause) nullclauses.get(nullclauses.size() - 1);

		Iterator itn = getNullClauses();

		while (itn.hasNext()) {
			IClause cl = (IClause) itn.next();
			if (cl.contains(lit)) {
				//System.out.println("lit = " + lit + " cl = " +cl);
				return cl;
			}
		}
		throw new RuntimeException("BIG Problem");
	}

	/**
	 * @see org.opensat.ICNF#getNullClauses()
	 */
	public Iterator getNullClauses() {
		return nullclauses.iterator();
	}

	/**
	 * @see org.opensat.ICNF#getVocabulary()
	 */
	public IVocabulary getVocabulary() {
		return voc;
	}

	/**
	 * @see org.opensat.ICNF#hasNullClause()
	 */
	public boolean hasNullClause() {
		if (!nullclauses.isEmpty()) {
			unitclauses.clear();
			return true;
		}
		return false;
		// return !nullclauses.isEmpty();
	}

	/**
	 * @see org.opensat.ICNF#hasUnitClause()
	 */
	public boolean hasUnitClause() throws ContradictionFoundException {
		while (!unitclauses.isEmpty()) {
			IClause cl = unitclauses.uncheckedBack();
			if (cl.isNull()) {
				nullclauses.add(cl);
				unitclauses.clear();
				throw new ContradictionFoundException();
			}
			if (!cl.isUnit()) {
				unitclauses.uncheckedPopBack();
				continue;
			}
			assert cl.firstUnassignedLiteral() != null;
			break;
		}
		return !unitclauses.isEmpty();
	}

	/**
	* @see org.opensat.ICNF#isSatisfied()
	*/
	public boolean isSatisfied() {
		Iterator it = clauses.iterator();
		while (it.hasNext()) {
			IClause cl = (IClause) it.next();
			if (!cl.isSatisfied()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @see org.opensat.ICNF#learn(IClause)
	 */
	public void learn(IClause cl) {
		// throw new UnsupportedOperationException();
		assert !cl.isSatisfied();
		clauses.add(cl);
		cl.registerToLiterals();
		assert cl.isUnit();
		if (cl.isUnit()) {
			addUnitClause(cl);
		} else if (cl.isNull()) {
			nullclauses.add(cl);
		}
		if (maxLengthClause<cl.size()) {
			maxLengthClause = cl.size();
		}

	}

	/**
	 * @see org.opensat.data.ICNF#maxLengthClause()
	 */
	public int maxLengthClause() {
		return maxLengthClause;
	}

	/**
	 * @see org.opensat.ICNF#nextUnitClause()
	 */
	public IClause nextUnitClause() {
		return unitclauses.uncheckedPopBack();
	}

	/**
	 * @see org.opensat.ICNF#propagateAssignment(ILiteral)
	 */
	public boolean propagateAssignment(ILiteral l) {
		assert l.isUnassigned();
		Iterator it = l.clauseIterator();
		while (it.hasNext()) {
			IClause cl = (IClause) it.next();
			// to update the clause state.
			cl.satisfy(l);
		}

		l.satisfy();

		it = l.opposite().clauseIterator();
		while (it.hasNext()) {
			IClause cl = (IClause) it.next();
			cl.reduce(l.opposite(), it);
			// if (!cl.isSatisfied()) {
				if (cl.isUnit()) {
					// assert !Arrays.search(unitclauses,unitlevel):cl.isLearned();
					addUnitClause(cl);
				} else if (cl.isNull()) {
					assert !nullclauses.contains(cl);
					nullclauses.add(cl);
				}
			// }
		}
		return nullclauses.isEmpty();
	}

	/**
	 * @see org.opensat.ICNF#setUniverse(int, int)
	 */
	public void setUniverse(int nbvars, int nbclauses) {
		voc.setUniverse(nbvars);
		clauses.ensureCapacity(nbclauses);
		unitclauses = new DequeOfIClause(nbclauses);
	}

	/**
	 * @see org.opensat.ICNF#size()
	 */
	public int size() {
		return clauses.size();
	}

	
	public String toString() {
		StringBuffer stb = new StringBuffer();
		stb.append("c output cnf generated by opensat\n");
		stb.append(
			"p cnf "
				+ voc.getNumberOfVariables()
				+ " "
				+ clauses.size()
				+ "\n");

		Iterator it = this.clauses.iterator();//activeClauseIterator();
		while (it.hasNext()) {
			IClause cl = (IClause) it.next();
			stb.append(cl);
			stb.append("\n");
		}
		return stb.toString();
	}

	/**
	* @see org.opensat.ICNF#unpropagateAssignment(org.opensat.ILiteral)
	*/
	public void unpropagateAssignment(ILiteral l) {
		assert l.isSatisfied();
		Iterator it = l.clauseIterator();
		while (it.hasNext()) {
			IClause cl = (IClause) it.next();
			cl.unassign(l);
		}

		// need to unassign before looking for unit clauses
		l.unassign();

		it = l.opposite().clauseIterator();
		while (it.hasNext()) {
			IClause cl = (IClause) it.next();
			cl.restore(l.opposite());
			// if (!cl.isSatisfied()) {
				if (cl.isUnit()) {
					assert nullclauses.contains(cl);
					nullclauses.remove(cl);
					// assert !unitclauses.contains(cl);
					addUnitClause(cl);
				}
			//}
		}
	}
	
	/**
     * Add a unit clause to the stack/queue of current unit clauses.
     * 
     * <p> This method let us choose easily between a stack and a queue
     * 
     * @param c unit clause to add
     */
    protected void addUnitClause(IClause c) {
		assert c.isUnit() : "Clause is not unit !";
		
	    unitclauses.uncheckedPushBack(c); // for a stack of unitclauses
	    //unitclauses.unchecked_push_front(c); // for a queue of unitclauses
	}
    
    /*added by Jinchuan Chen 17, June, 2014
	 *To get the full list of clauses 
	 * */
    public Iterator fullClauseIterator(){
    	return clauses.iterator();
    }

}

