// https://searchcode.com/api/result/92665055/

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
package org.opensat.data.colt;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.opensat.data.ContradictionFoundException;
import org.opensat.data.ICNF;
import org.opensat.data.IClause;
import org.opensat.data.ILiteral;
import org.opensat.data.IVocabulary;

import cern.colt.list.ObjectArrayList;
import cern.colt.list.adapter.ObjectListAdapter;

/**
 * Class responsability.
 * 
 * @author leberre
 *
 */
public class CNFSimpleImpl implements ICNF {

    public CNFSimpleImpl() {
        this(new VocabularySimpleImpl());
    }

    public CNFSimpleImpl(IVocabulary voc) {
        this.voc = voc;
        clauses = new ObjectArrayList();
        unitclauses = new ObjectArrayList();
        clausesadapter = new ObjectListAdapter(clauses);
        nullclauses = new ObjectArrayList();
        nulladapter = new ObjectListAdapter(nullclauses);
        nextUnitClause = null;
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
            if (!cl.isSatisfied()) {
                if (cl.isUnit()) {
                    assert !unitclauses.contains(cl, false);
                    unitclauses.add(cl);
                } else if (cl.isNull()) {
                    assert !nullclauses.contains(cl, false);
                    nullclauses.add(cl);
                }
            }
        }
        return nullclauses.isEmpty();
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
            if (!cl.isSatisfied()) {
                if (cl.isUnit()) {
                    assert nullclauses.contains(cl, false);
                    int index = nullclauses.indexOf(cl, false);
                    assert index != -1;
                    nullclauses.remove(index);
                    // assert !unitclauses.contains(cl);
                    unitclauses.add(cl);
                }
            }
        }
    }

    /**
    * @see org.opensat.ICNF#isSatisfied()
    */
    public boolean isSatisfied() {
        Object[] cls = clauses.elements();
        for (int i = 0; i < clauses.size(); i++) {
            IClause cl = (IClause) cls[i];
            if (!cl.isSatisfied()) {
                return false;
            }
        }
        return true;
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
        // create the clause
        IClause clause = createClause(literals);
        clauses.add(clause);
        clause.registerToLiterals();
        if (clause.isNull()) {
            nullclauses.add(clause);
        } else if (clause.isUnit()) {
            unitclauses.add(clause);
        }
    }

    /**
     * Factory method to create new clauses.
     * 
     * @param literals a list of ILiterals.
     * @return IClause
     */
    public IClause createClause(List literals) {
        return new ClauseSimpleImpl(literals);
        // return new ClauseSimpleImplWL(literals);
    }

    /**
    * @see org.opensat.ICNF#beginLoadFormula()
    */
    public void beginLoadFormula() {
        voc.clear();
        clauses.clear();
        unitclauses.clear();
        nullclauses.clear();
    }

    /**
    * @see org.opensat.ICNF#endLoadFormula()
    */
    public void endLoadFormula() {
        // put active clauses in a list
        voc.freeze(clausesadapter.iterator());
    }

    /**
     * @see org.opensat.ICNF#hasNullClause()
     */
    public boolean hasNullClause() {
        return !nullclauses.isEmpty();
    }

    public String toString() {
        StringBuffer stb = new StringBuffer();
        stb.append("c output cnf generated by opensat\n");
        stb.append(
            "p cnf " + voc.getNumberOfVariables() + " " + activeclauses + "\n");
        Iterator it = activeClauseIterator();
        while (it.hasNext()) {
            IClause cl = (IClause) it.next();
            stb.append(cl);
            stb.append("\n");
        }
        return stb.toString();
    }

    protected final ObjectArrayList clauses;
    private int activeclauses;
    private final ObjectArrayList unitclauses;
    private final ObjectListAdapter clausesadapter;
    private final ObjectArrayList nullclauses;
    private final ObjectListAdapter nulladapter;
    private IVocabulary voc;
    private IClause nextUnitClause;

    /**
     * @see org.opensat.ICNF#size()
     */
    public int size() {
        return activeclauses;
    }

    /**
     * @see org.opensat.ICNF#getVocabulary()
     */
    public IVocabulary getVocabulary() {
        return voc;
    }

    /**
     * @see org.opensat.ICNF#hasUnitClause()
     */
    public boolean hasUnitClause() throws ContradictionFoundException {
        assert nextUnitClause == null : "Call nextUnitClause()";
        Object[] cls = unitclauses.elements();
        for (int i = 0; i < unitclauses.size();) {
            IClause cl = (IClause) cls[i];
            if (cl.isNull()) {
                nullclauses.add(cl);
                throw new ContradictionFoundException();
            }
            if (!cl.isUnit()) {
                unitclauses.remove(i);
                continue;
            }
            assert cl.firstUnassignedLiteral() != null;
            nextUnitClause = cl;
            break;
        }
        return nextUnitClause != null;
    }

    /**
     * @see org.opensat.ICNF#nextUnitClause()
     */
    public IClause nextUnitClause() {
        assert nextUnitClause != null : "Call hasUnitClause() before!";
        assert unitclauses.indexOf(nextUnitClause, false) == 0;
        unitclauses.remove(0);
        IClause clause = nextUnitClause;
        nextUnitClause = null;
        return clause;
    }
    /**
     * @see org.opensat.ICNF#activeClauseIterator()
     */
    public Iterator activeClauseIterator() {
        //FIXME
        throw new UnsupportedOperationException("FIXME");
        // return activeclauses.iterator();
    }
    /**
     * @see org.opensat.ICNF#display(java.io.PrintWriter)
     */
    public void display(PrintWriter out) {
        out.print("p cnf ");
        out.print(getVocabulary().getMaxVariableId() + " ");
        out.println(size());

        Iterator iter = activeClauseIterator();

        while (iter.hasNext())
             ((IClause) iter.next()).display(out);

        out.flush();
    }

    /**
     * @see org.opensat.ICNF#setUniverse(int, int)
     */
    public void setUniverse(int nbvars, int nbclauses) {
        voc.setUniverse(nbvars);
        clauses.ensureCapacity(nbclauses);
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
        return nulladapter.iterator();
    }

    /**
     * @see org.opensat.ICNF#learn(IClause)
     */
    public void learn(IClause cl) {
        // throw new UnsupportedOperationException();
        clauses.add(cl);
        ((ClauseSimpleImpl) cl).registerToLiterals();
        if (!cl.isSatisfied()) {
            activeclauses++;
        }
        if (cl.isUnit()) {
            unitclauses.add(cl);
        } else if (cl.isNull()) {
            nullclauses.add(cl);
        }
    }

    /**
     * @see org.opensat.data.ICNF#flush()
     */
    public void flush() {
        unitclauses.clear();
        assert nullclauses.isEmpty();
        assert activeclauses == 0;
    }

    /**
     * @see org.opensat.data.ICNF#maxLengthClause()
     */
    public int maxLengthClause() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.opensat.data.ICNF#createClause(org.opensat.data.ILiteral[], int)
     */
    public IClause createClause(ILiteral[] lits, int size) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }
    
    /*added by Jinchuan Chen 17, June, 2014
	 *To get the full list of clauses 
	 * */
    public Iterator fullClauseIterator(){
    	//TODO
    	return null;
    }

}

