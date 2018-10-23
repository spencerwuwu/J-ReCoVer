// https://searchcode.com/api/result/92665004/

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
 * Created on 3 f?vr. 2003
 * 
 */
package org.opensat.data;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Contains all the basic operations for a clause based on counters.
 * The underlying data structure recording the literals belonging to the clause
 * needs to be implemented.
 * 
 * @author leberre
 *
 */
public abstract class AbstractClause implements IClause {
	private boolean learned;
    
	protected boolean registered;
	protected int satisfied;
	protected int unassigned;

	/**
	 * Constructor for AbstractClause.
	 * @param size the original clause size.
	 */
	protected AbstractClause(int size, boolean learned) {
		super();
		registered = false;
		satisfied = 0;
		unassigned = size;
		this.learned = learned;
	}

	protected int countSatisfiedLiterals() {
		int counter = 0;

		ILiteral [] literals=getLiterals();
		int nbLiterals=getLiteralsSize();
		
		for(int i=0;i<nbLiterals;i++) {
			if (literals[i].isSatisfied()) {
				counter++;
			}
		}
		
		return counter;
	}

	protected int countUnassignedLiterals() {
		int counter = 0;

		ILiteral [] literals=getLiterals();
		int nbLiterals=getLiteralsSize();
		
		for(int i=0;i<nbLiterals;i++) {
			if (literals[i].isUnassigned()) {
				counter++;
			}
		}
		
		return counter;
	}
	
    protected abstract IClause createClause(List l);

	/**
	 * @see org.opensat.IClause#display(java.io.PrintWriter)
	 */
	public void display(PrintWriter out) {
		ILiteral [] literals=getLiterals();
		int nbLiterals=getLiteralsSize();
		
		for(int i=0;i<nbLiterals;i++) {
			out.print(literals[i].getId());
			out.print(" ");
		}

		out.println("0");
		out.flush();
	}

	/**
	 * @see org.opensat.IClause#falsifiedLiterals()
	 */
	public int falsifiedLiterals() {
		return originalSize() - satisfied - unassigned;
	}

	/**
	 * @see org.opensat.IClause#firstUnassignedLiteral()
	 */
	public ILiteral firstUnassignedLiteral() {
		ILiteral [] literals=getLiterals();
		int nbLiterals=getLiteralsSize();
		
		for(int i=0;i<nbLiterals;i++) {
			if (literals[i].isUnassigned()) {
				return literals[i];
			}
		}
		return null;
	}

	/**
	 * @see org.opensat.data.IClause#getLiteral(int)
	 */
	public ILiteral getLiteral(int i) {
		return getLiterals()[i];
	}

	/**
	 * @see org.opensat.data.IClause#getLiterals()
	 */
	public ILiteral [] getLiterals() {
		return null;
	}


	/**
	 * @see org.opensat.data.IClause#getTautology()
	 */
	public IClause getTautology() {
		return null;
	}

	/**
	 * @see org.opensat.IClause#isBinary()
	 */
	public boolean isBinary() {
		assert unassigned >= 0;
		assert satisfied >= 0;

		return (satisfied == 0) && (unassigned == 2);
	}

	/**
	 * @see org.opensat.IClause#isFalsified()
	 */
	public boolean isFalsified() {
		return (satisfied == 0) && (unassigned == 0);
	}

	/**
	 * @see org.opensat.data.IClause#isLearned()
	 */
	public boolean isLearned() {
		return learned;
	}

	/**
	 * @see org.opensat.IClause#isNull()
	 */
	public boolean isNull() {
		assert unassigned >= 0;
		assert satisfied >= 0;
		// assert (unassigned == countUnassignedLiterals()) ;
		return (satisfied == 0) && (unassigned == 0);
	}

	/**
	 * @see org.opensat.data.IClause#isRegistered()
	 */
	public boolean isRegistered() {
		return registered;
	}

	/**
	 * @see org.opensat.IClause#isSatisfied()
	 */
	public boolean isSatisfied() {
		assert satisfied >= 0;
		assert satisfied <= originalSize();

		return satisfied > 0;
	}

	/**
	 * @see org.opensat.IClause#isSatisfiedByOnlyOneLiteral()
	 */
	public boolean isSatisfiedByOnlyOneLiteral() {
		return satisfied == 1;
	}

	/**
	 * @see org.opensat.IClause#isUnit()
	 */
	public boolean isUnit() {
		assert unassigned >= 0;
		assert unassigned <= originalSize();
		assert satisfied >= 0;
		assert satisfied <= originalSize();

		return (satisfied == 0) && (unassigned == 1);
	}

	/**
	 * @see org.opensat.IClause#reduce(ILiteral)
	 */
	public void reduce(ILiteral l,Iterator it) {
		assert contains(l);
		assert unassigned > 0;
		assert unassigned <= originalSize();

		unassigned--;
	}

	/**
	 * Attach this clause to its literals: the clause register itself to each of its literals.
	 */
	public void registerToLiterals() {
		ILiteral [] literals=getLiterals();
		int nbLiterals=getLiteralsSize();
		
		for(int i=0;i<nbLiterals;i++) {
			literals[i].register(this);
			if (literals[i].isSatisfied()) {
				satisfied++;
				unassigned--;
			} else if (literals[i].isFalsified()) {
				unassigned--;
			}
		}
		registered = true;
		assert originalSize() != 0;
		assert satisfied >= 0;
		assert satisfied <= originalSize();
		assert unassigned >= 0;
		assert unassigned <= originalSize();
		assert satisfied + unassigned <= originalSize();
	}

    /**
     * Create a resolvant from clauses this and cl.
     * cl MUST contain at least one literal whose opposite is in
     * this clause.
     * 
     * @param cl
     * @return null if a tautological clause is computed, else the resolvant of the
     * two clauses.
     * @throws ContradictionFoundException if the resolvant is the empty clause.
     */
    public IClause resolveWith(IClause cl) throws ContradictionFoundException {
    	List literals = new ArrayList(size()+cl.size());		
    	int oppositecounter = 0;
    	// Put first literals clause in tmp

		ILiteral [] thisLiterals=getLiterals();
		int thisNbLiterals=getLiteralsSize();
		
		for(int i=0;i<thisNbLiterals;i++) {
   			literals.add(thisLiterals[i]);
    	}
    	
    	// 

		ILiteral [] clLiterals=cl.getLiterals();
		int clNbLiterals=cl.getLiteralsSize();
		
		for(int i=0;i<clNbLiterals;i++) {
    		if (literals.contains(clLiterals[i].opposite())) {
    			literals.remove(clLiterals[i].opposite());
    			oppositecounter++;
    		} else {
    			if (!literals.contains(clLiterals[i])) {
    				literals.add(clLiterals[i]);
    			}
    		}
    	}
    	if (literals.size() == 0) {
    		throw new ContradictionFoundException();
    	}
    	
    	if ((oppositecounter == 0) /*&& (this.size()!=0) && (cl.size()!=0)*/) {
    		// If the reason is a tautology 
    		// oppositeCounter equal 0 ?
    			throw new IllegalArgumentException();
    	}
    	// if oppositecounter>1 then tautological clause.
    	if (oppositecounter > 1) {
    		return getTautology();
    	}
    	return createClause(literals);
    }

	/**
	 * @see org.opensat.IClause#restore(ILiteral)
	 */
	public void restore(ILiteral l) {
		assert contains(l);
		assert unassigned < originalSize();
		assert unassigned >= 0;

		unassigned++;
	}

	/**
	 * @see org.opensat.IClause#satisfiedLiterals()
	 */
	public int satisfiedLiterals() {
		return satisfied;
	}

	/**
	 * @see org.opensat.IClause#satisfy(ILiteral)
	 */
	public void satisfy(ILiteral l) {
		assert contains(l);
		assert satisfied >= 0;
		assert satisfied < originalSize();
		assert unassigned > 0;

		satisfied++;
		unassigned--;
	}

	/**
	 * @see org.opensat.IClause#setRelevance(boolean)
	 */
	public void setRelevance(boolean b) {
		ILiteral [] literals=getLiterals();
		int nbLiterals=getLiteralsSize();
		
		for(int i=0;i<nbLiterals;i++) {
			literals[i].setRelevance(b);
		}
	}

	/**
	* @see org.opensat.IClause#size()
	*/
	public int size() {
		assert unassigned >= 0;

		return unassigned;
	}

	public String toString() {
        if (isSatisfied())
            return "";
        StringBuffer stb = new StringBuffer();
		
		ILiteral [] literals=getLiterals();
		int nbLiterals=getLiteralsSize();
		
		for(int i=0;i<nbLiterals;i++) {
            stb.append(literals[i]);
            stb.append(" ");
        }
        
        stb.append("0");
        return stb.toString();
    }

	/**
	 * @see org.opensat.IClause#unassign(ILiteral)
	 */
	public void unassign(ILiteral l) {
		assert contains(l);
		assert satisfied > 0;
		assert unassigned >= 0;
		assert satisfied <= originalSize();
		assert unassigned < originalSize();

		satisfied--;
		unassigned++;
	}
}

