// https://searchcode.com/api/result/92665029/

/*
 * Created on 12 fevr. 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.opensat.data.simple;

import java.io.PrintWriter;
import java.util.Iterator;

import org.opensat.data.ContradictionFoundException;
import org.opensat.data.IClause;
import org.opensat.data.ILiteral;

/**
 * @author leberre
 */
public class UnitClause implements IClause {

	private final ILiteral literal;
	private final ILiteral [] literals;
	private boolean registered;
	private boolean learned;
	
    /**
     * 
     */
    public UnitClause(ILiteral l) {
        this(l,false);
    }
    
    public UnitClause(ILiteral l, boolean learned) {
        literal = l;
        literals = new ILiteral[] {literal};
        registered= false;
        this.learned = learned;
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#isNull()
     */
    public boolean isNull() {
 		return literal.isFalsified();
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#isSatisfied()
     */
    public boolean isSatisfied() {
	return literal.isSatisfied();
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#isUnit()
     */
    public boolean isUnit() {
        return literal.isUnassigned();
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#satisfy(org.opensat.data.ILiteral)
     */
    public void satisfy(ILiteral l) {
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#reduce(org.opensat.data.ILiteral, java.util.Iterator)
     */
    public void reduce(ILiteral l, Iterator it) {
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#unassign(org.opensat.data.ILiteral)
     */
    public void unassign(ILiteral l) {
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#restore(org.opensat.data.ILiteral)
     */
    public void restore(ILiteral l) {
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#isBinary()
     */
    public boolean isBinary() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#contains(org.opensat.data.ILiteral)
     */
    public boolean contains(ILiteral l) {
        return literal == l;
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#size()
     */
    public int size() {
        return literal.isFalsified()?0:1;
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#satisfiedLiterals()
     */
    public int satisfiedLiterals() {
        return literal.isSatisfied()?1:0;
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#falsifiedLiterals()
     */
    public int falsifiedLiterals() {
        return literal.isFalsified()?1:0;
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#firstUnassignedLiteral()
     */
    public ILiteral firstUnassignedLiteral() {
        assert literal.isUnassigned();
        return literal;
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#isSatisfiedByOnlyOneLiteral()
     */
    public boolean isSatisfiedByOnlyOneLiteral() {
        return isSatisfied();
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#isFalsified()
     */
    public boolean isFalsified() {
        return literal.isFalsified();
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#literalIterator()
     */
    public Iterator literalIterator() {
		throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#resolveWith(org.opensat.data.IClause)
     */
    public IClause resolveWith(IClause cl) throws ContradictionFoundException {
       throw new UnsupportedOperationException();

    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#display(java.io.PrintWriter)
     */
    public void display(PrintWriter out) {
        out.print(literal+" 0");
        out.flush();
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#setRelevance(boolean)
     */
    public void setRelevance(boolean b) {
        literal.setRelevance(b);
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#getTautology()
     */
    public IClause getTautology() {
		throw new UnsupportedOperationException();	
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#registerToLiterals()
     */
    public void registerToLiterals() {
        literal.register(this);
        literal.watch(this);
        registered = true;
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#isRegistered()
     */
    public boolean isRegistered() {
        return registered;
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#getLiteral(int)
     */
    public ILiteral getLiteral(int i) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#getLiterals()
     */
    public ILiteral[] getLiterals() {
        return literals;
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#getLiteralsSize()
     */
    public int getLiteralsSize() {
        return 1;
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#originalSize()
     */
    public int originalSize() {
        return 1;
    }

    /* (non-Javadoc)
     * @see org.opensat.data.IClause#isLearned()
     */
    public boolean isLearned() {
        return learned;
    }

}

