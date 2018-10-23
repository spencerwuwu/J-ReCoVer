// https://searchcode.com/api/result/92665000/

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
 * Created on 24 nov. 2002
 * 
 */
package org.opensat.data;

import java.io.PrintWriter;
import java.util.Iterator;

/**
 * That interface represents basic services needed to manipulate
 * clauses. One assumption in the design is that once created, a 
 * clause will not be extended: all the literals contained in the clause
 * are known when constructing the clause.
 * 
 * @author leberre
 *
 */
public interface IClause {

	/**
	 * To detect a null clause.
	 * 
	 * @return true if the clauses does not contain any literal or only falsified literals.
	 */
	boolean isNull();

	/**
	 * A clause is satisfied iff one of its literals is satisfied.
	 * 
	 * @return boolean
	 */
	boolean isSatisfied();

	/**
	 * An unassigned clause with only one unassigned literal.
	 * 
	 * @return boolean
	 */
	boolean isUnit();

	/**
	 * Notify satisfaction occuring thanks to the literal l.
	 * 
	 * @param l a satisfied literal occuring in the clause.
	 * @see #unassign
	 */
	void satisfy(ILiteral l);

	/**
	 * Reduce the clause by removing falsified literal l.
	 * @param l a falsified literal occuring in the clause.
	 * @param it the iterator over the clauses used to call that method (useful
	 * for removing clauses in watched literals).
	 * @see #restore
	 */
	void reduce(ILiteral l, Iterator it);

	/**
	 * Unassign a clause.
	 * Satisfy dual method.
	 *  
	 * @param l
	 * @see #satisfy
	 */
	void unassign(ILiteral l);

	/**
	 * reduce dual method.
	 * @param l
	 * @see #reduce
	 */
	void restore(ILiteral l);

	/**
	 * An unassigned clause containing exactly two unassigned literals.
	 * @return boolean
	 */
	boolean isBinary();

	/**
	 * To know if the clause contains the literal l.
	 * 
	 * @param l
	 * @return true iff tje literal is contained in the clause
	 */
	boolean contains(ILiteral l);

	/**
	 * Returns the number of unassigned literals in the clause.
	 * (satisfied literals are not counted here).
	 * 
	 * @return int
	 */
	int size();

	/**
	 * Returns the number of satisfied literals in the clause.
	 * @return int
	 */
	int satisfiedLiterals();

	/**
	 * Returns the number of falsified literals in the clause.
	 * @return int
	 */
	int falsifiedLiterals();

	/**
	 * Returns the first unassigned literal if any, else null.
	 * @return ILiteral
	 */
	ILiteral firstUnassignedLiteral();

	/**
	 * Returns true iff the clause is satisfied by exactly one literal.
	 * @return boolean
	 */
	boolean isSatisfiedByOnlyOneLiteral();

	/**
	 * Method isFalsified.
	 * @return boolean
	 */
	boolean isFalsified();

	/**
	 * Return an iterator over the literals in this clause.
	 * 
	 * <p>Unless otherwise specified, the iterator will include every literal 
	 * in this clause.
	 * 
	 * <p>For implementation of IClause which store the literals in an array,
	 * it will be much faster to use getLiterals() to iterate directly over 
	 * the array.
	 * 
	 * @return an iterator over the literals in this clause
	 * @see #getLiterals()
	 */
	Iterator literalIterator();

	/**
	 * Create a new clause which is the resolvant from cl and this.
	 * @param cl
	 * @return ICLause
	 */
	IClause resolveWith(IClause cl) throws ContradictionFoundException;

	/**
	 * Display this clause in Dimacs format.
	 * @param out output stream
	 */
	void display(PrintWriter out);

	/**
	 * Set the relevance of all the literals contained in the clause.
	 * NOTE: the clause may not be satisfied when calling that method.
	 * @param b
	 */
	void setRelevance(boolean b);

	/**
	 * Return the tautology clause
	 * 
	 */
	IClause getTautology();

	/**
	 * Register that clause to the literals it contains.
	 * This allows to manipulate clauses that are not directly part of the
	 * formula to solve (e.g. reasons for backjumping, etc).
	 */
	void registerToLiterals();
	
	boolean isRegistered();
	
	
	/**
	 * Get a literal from the clause.
	 * 
	 * @param i index of the literal we want to get. Must be in [0..getLiteralsSize()-1] 
	 * @return the literal at position i in the clause
	 * @see #getLiteralsSize()
	 */
	ILiteral getLiteral(int i);

	/**
	 * Get the literals in the clause.
	 * 
	 * 
	 * <p>This is the most efficient way to iterate over the literals of the clause
	 * provided the implementation is based on an array and we have direct access 
	 * to this array
	 * 
	 * <p>It should be used in this way :
	 * 
	 * <pre>
	 * ILiteral [] literals=theClause.getLiterals();
	 * int nbLiterals=theClause.getLiteralSize();
	 * 
	 * for(int i=0;i&lt;nbLiterals;i++)
	 *   foo(literals[i]);
	 * </pre>
	 * 
	 * <p>Note that one cannot assume that literals contains literals.length
	 * valid literals. getLiteralSize() <b>must</b> be used to get the limit
	 * of the loop
	 * 
	 * @return the literals in the clause
	 * @see #getLiteralsSize()
	 */
	ILiteral [] getLiterals();
	
	/**
	 * Return the number of literals present in the array returned by getLiterals.
	 * 
	 * <p>One cannot assume that this function returns getLiterals().length or originalSize() 
	 * or size() or any other size.
	 * 
	 * <p>Implementation must guarantee that getLiterals()[0..getLiteralsSize()-1] contains
	 * literals of the clause
	 * 
	 * @return The number of literals over which we can iterate in the array returned by getLiterals().
	 * Depending on the implementation, this may be different from getLiterals().length.
	 * @see #getLiterals()
	 */
	int getLiteralsSize();
	
	int originalSize();
	
	boolean isLearned();
}


