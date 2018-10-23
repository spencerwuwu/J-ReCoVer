// https://searchcode.com/api/result/126461451/

package fri.patterns.interpreter.parsergenerator.parsertables;

import java.util.*;
import fri.patterns.interpreter.parsergenerator.Token;
import fri.patterns.interpreter.parsergenerator.ParserTables;
import fri.patterns.interpreter.parsergenerator.syntax.*;

/**
	SLR bottom-up parser syntax node.
	The build() method of an no-arg-constructed node is used to fill a
	state node list. The empty list passed to build() will be filled
	with all state nodes after.
	<p>
	Every syntax node provides a fill() method to populate the corresponding
	line (node-index) within PARSE-ACTION and GOTO tables.
	<p>
	After construction the syntax node has state entries represented by
	RuleStateItem instances.
	<p>
	The state of a rule is represented by a dot "." at start, between symbols,
	or at end. To shift an item means to move the dot to left.
	
	@author (c) 2000, Fritz Ritzberger
*/

class SLRSyntaxNode
{
	/** Contains all rule state entries. */
	protected Hashtable entries = new Hashtable();

	private int kernels = 0;
	private Integer hashCache = null;


	/** Do-nothing-constructor, used to call the build() method. */
	public SLRSyntaxNode()	{
	}
	
	/** Factory-method to create a SLRSyntaxNode, to be overridden by other node types. */
	protected SLRSyntaxNode createSyntaxNode()	{
		return new SLRSyntaxNode();
	}
	
	/** Factory-method to create a RuleStateItem, to be overridden by other node types. */
	protected RuleStateItem createRuleStateItem(int ruleIndex, Rule rule)	{
		return new RuleStateItem(ruleIndex, rule);
	}		


	/**
		Construction of start node and all further state nodes.
		After this call the syntaxNodes list is filled with all state-bound nodes.
		@param syntax the grammar, including START rule.
		@param syntaxNodes ampty list that holds all syntax nodes after this call.
		@param kernels empty hashtable for internal buffering.
		@return the syntaxNodes list, now filled.
	*/
	public List build(Syntax syntax, List syntaxNodes, Hashtable kernels)	{
		// insert first rule as state item
		RuleStateItem item = createRuleStateItem(0, syntax.getRule(0));
		entries.put(item, item);
		closure(syntax);	// calculate followers
		
		syntaxNodes.add(this);	// now add startnode to list of syntax nodes

		generateSyntaxNodes(syntaxNodes, syntax, kernels);	// generate all other nodes

		//System.err.println("Built "+syntaxNodes.size()+" states.");
		return syntaxNodes;
	}

	
	/**
		Generates syntax nodes into passed list, whereby every node represents one possible state.
		Every generated node gets appended to list and can generate new nodes by itself.
		@param syntaxNodes list of nodes
		@param syntax the grammar rules
	*/
	protected void generateSyntaxNodes(List syntaxNodes, Syntax syntax, Hashtable kernels)	{
		// newly appended nodes will be found and processed
		for (int i = 0; i < syntaxNodes.size(); i++)	{
			SLRSyntaxNode node = (SLRSyntaxNode)syntaxNodes.get(i);
			node.generateSyntaxNodesFromItems(syntaxNodes, syntax, kernels);
		}
	}
	
	/**
		Generates follower nodes from rule state items into list. The followers are referenced
		by their originators (GOTO-references).
		@param syntaxNodes list of nodes
		@param syntax the grammar rules
	*/
	protected void generateSyntaxNodesFromItems(List syntaxNodes, Syntax syntax, Hashtable kernels)	{
		for (Enumeration e = entries.elements(); e.hasMoreElements(); )	{
			RuleStateItem item = (RuleStateItem)e.nextElement();
			String pending = item.getPendingSymbol();

			if (item.followNodeIndex < 0 && pending != null)	{	// further states are possible
				// create a kernel item
				SLRSyntaxNode node = createSyntaxNode();
				List tmp = node.addShiftedItems(pending, entries);	// get entries that have been taken

				// look if it is already in list
				Integer kernelIndex = (Integer)kernels.get(node);
				int index = kernelIndex != null ? kernelIndex.intValue() : -1;

				// if not in list, add it, compute closure
				if (index < 0)	{
					index = syntaxNodes.size();
					kernels.put(node, new Integer(index));
					syntaxNodes.add(node);
					node.closure(syntax);
				}

				// link originator entries to new or found node
				for (int i = 0; i < tmp.size(); i++)	{
					Tuple t = (Tuple)tmp.get(i);
					linkParentItemToChild(t.o1, index, syntaxNodes, t.o2);
				}
			}
		}
	}
	

	/**
		Adopt all rule-states from originator node, which have the passed symbol
		as pending to the right of dot. This is done when a new node was generated,
		which happens when the dot is moved to the right.
		All adopted items together form the kernel of the syntax node.
		The number of kernel items is calculated, which is important when searching
		existing nodes.
		@param symbol the symbol that must be the pending symbol right of the dot within rule state item
		@param originatorEntries map containing all rule state items of the originator node (as value).
		@return a Tuple list containing pairs of original and new item, the new item is the shifted one.
	*/
	protected List addShiftedItems(String symbol, Hashtable originatorEntries)	{
		List list = new ArrayList();
		for (Enumeration e = originatorEntries.elements(); e.hasMoreElements(); )	{
			RuleStateItem item = (RuleStateItem) e.nextElement();
			String pending = item.getPendingSymbol();
			
			if (pending != null && symbol.equals(pending))	{
				RuleStateItem newitem = item.shift();
				this.entries.put(newitem, newitem);
				list.add(new Tuple(item, newitem));	// return all derived originator items
			}
		}
		
		kernels = list.size();	// remember count of kernel items
		
		return list;	// return list of entries that were shifted
	}


	/**
		Store the follow state (node index) within rule state item.
		This is a sparate protected method as LALR nodes do further work here.
	*/
	protected void linkParentItemToChild(RuleStateItem parent, int newIndex, List syntaxNodes, RuleStateItem child)	{
		parent.followNodeIndex = newIndex;
	}



	/**
		Closure - do for all rule states:
		Adopt all rules from grammar that derive one of the pending nonterminals
		(right of dot) within entries list. This is done recursively, appending
		new rules at end, so that they can adopt further rules.
		The closure method calls <i>addRulesDerivingPendingNonTerminal()</i>.
	*/
	protected void closure(Syntax syntax)	{
		// put Hashtable to List for sequential work
		List todo = new ArrayList(entries.size() * 2);
		for (Enumeration e = entries.elements(); e.hasMoreElements(); )
			todo.add(e.nextElement());

		// loop todo list and find every added new item
		for (int i = 0; i < todo.size(); i++)	{
			RuleStateItem rsi = (RuleStateItem)todo.get(i);
			String nonterm = rsi.getPendingNonTerminal();
			if (nonterm != null)
				addRulesDerivingPendingNonTerminal(rsi, nonterm, syntax, todo);
		}
	}
	
	/**
		Closure call for one rule state item. All rules in grammar that have passed
		nonterm on left side get appended to todo list and put into item entries when not already in entries.
	*/
	protected void addRulesDerivingPendingNonTerminal(RuleStateItem item, String nonterm, Syntax syntax, List todo)	{
		// make the closure for one item:
		// if pointer before a nonterminal, add all rules that derive it
		for (int i = 0; i < syntax.size(); i++)	{
			Rule rule = syntax.getRule(i);
			
			if (rule.getNonterminal().equals(nonterm))	{
				RuleStateItem rsi = createRuleStateItem(i, rule);

				if (entries.containsKey(rsi) == false)	{
					entries.put(rsi, rsi);	// real entry list
					todo.add(rsi);	// work list
				}
			}
		}
	}



	/**
		Fill the line of GOTO table this state corresponds to.
		@param state the index of this state within list
		@return Hashtable with all terminals/nonterminals to handle, and their follower states.
	*/
	public Hashtable fillGotoLine(int state)	{
		Hashtable h = new Hashtable(entries.size() * 3 / 2);	// load factor 0.75
		
		// fill one row of GOTO-table
		for (Enumeration e = entries.elements(); e.hasMoreElements(); )	{
			// store temporary
			RuleStateItem item = (RuleStateItem)e.nextElement();
			String symbol = item.getPendingSymbol();
			
			if (symbol != null)	{	// if pointer not at end of rule
				//System.err.println("Regel-Zustand:	"+item);
				setTableLine("GOTO", state, h, item, new Integer(item.followNodeIndex), symbol);
			}
		}
		return h;
	}


	/**
		Fill the line of PARSE-ACTION table this state corresponds to.
		@param state the index of this state within list
		@param firstSets all FIRST-sets of all nonterminals
		@param followSets all FOLLOW-sets of all nonterminals
		@return Hashtable with all terminals to handle, and their actions.
	*/
	public Hashtable fillParseActionLine(int state, FirstSets firstSets, FollowSets followSets)	{
		// fill one row of PARSE-ACTION-table
		Hashtable h = new Hashtable(entries.size() * 10);
		
		for (Enumeration e = entries.elements(); e.hasMoreElements(); )	{
			// store temporary
			RuleStateItem item = (RuleStateItem)e.nextElement();
			String symbol = item.getPendingSymbol();
			
			if (symbol != null)	{	// pointer not at end of rule, SHIFT
				if (Token.isTerminal(symbol))	{	// enter SHIFT at terminal symbol
					// first-set of terminal is terminal
					setParseTableLine(state, h, item, ParserTables.SHIFT, symbol);
				}
				else	{	// put SHIFT at all terminals of FIRST-set
					List firstSet = getNontermShiftSymbols(firstSets, item.getNonterminal());
					
					if (firstSet != null)	{	// LALR will return null, SLR not null
						for (int i = 0; i < firstSet.size(); i++)	{
							String terminal = (String) firstSet.get(i);
							setParseTableLine(state, h, item, ParserTables.SHIFT, terminal);
						}
					}
				}
			}
			else	{	// pointer at end, REDUCE to rule number
				for (Iterator reduceSymbols = getReduceSymbols(followSets, item); reduceSymbols.hasNext(); )	{
					String terminal = (String) reduceSymbols.next();
					
					if (item.ruleIndex == 0)	// is startnode
						setParseTableLine(state, h, item, ParserTables.ACCEPT, terminal);
					else	// ruleIndex > 0 means REDUCE
						setParseTableLine(state, h, item, new Integer(item.ruleIndex), terminal);
				}
			}
		}
		return h;
	}

	/**
		Returns all symbols for which SHIFT must be put into PARSE-ACTION table for a nonterminal.
		For SLR this is the FIRST set of the nonterminal.
	*/
	protected List getNontermShiftSymbols(FirstSets firstSets, String nonterm)	{
		return (List) firstSets.get(nonterm);
	}

	/**
		Returns all symbols for which REDUCE must be put into PARSE-ACTION table.
		For SLR this is the FOLLOW set of the nonterminal.
	*/
	protected Iterator getReduceSymbols(FollowSets followSets, RuleStateItem item)	{
		return ((List) followSets.get(item.getNonterminal())).iterator();
	}




	/**
		Set a position in PARSE-ACTION table.
		This is the place where SHIFT/REDUCE and REDUCE/REDUCE conflicts are solved.
	*/
	protected void setParseTableLine(int state, Hashtable line, RuleStateItem item, Integer action, String terminal)	{
		// set one action into a parse-table row and resolve conflicts
		
		if (setTableLine("PARSE-ACTION", state, line, item, action, terminal) == false)	{
			// shift/reduce or reduce/reduce conflict
			Object o = line.get(terminal);
			
			if (action.equals(ParserTables.SHIFT) || o.equals(ParserTables.SHIFT))	{
				// prefer SHIFT operation
				line.put(terminal, ParserTables.SHIFT);
				System.err.println("WARNING: shift/reduce conflict, SHIFT is preferred.");
			}
			else	{
				System.err.println("WARNING: reduce/reduce conflict, rule with smaller index is preferred.");
				// prefer rule with smaller index
				if (((Integer)o).intValue() > action.intValue())
					line.put(terminal, action);
			}
		}
	}
	
	
	/**
		Set a position in one of the tables.
		Here SHIFT/SHIFT, SHIFT/REDUCE and REDUCE/REDUCE conflicts are detected.
		@return true when no conflict was detected
	*/
	protected boolean setTableLine(String table, int state, Hashtable line, RuleStateItem item, Integer action, String terminal)	{
		// set one action into a table row and detect conflicts
		Object o = line.get(terminal);
		if (o == null)	{	// no conflict
			line.put(terminal, action);
		}
		else	{	// conflict?
			if (o.equals(action) == false)	{	// conflict!
				System.err.println("========================================================");
				System.err.println("WARNING: "+table+" state "+state+", terminal "+
						terminal+" is "+
						displayAction(o)+" and was overwritten by action "+
						displayAction(action));
				System.err.println("... from rule state: "+item);
				System.err.println("... current state:\n"+this);
				System.err.println("========================================================");
				return false;
			}
		}
		return true;
	}
	
	private String displayAction(Object action)	{
		if (action.equals(ParserTables.SHIFT))
			return "SHIFT";
		return "REDUCE("+action.toString()+")";
	}
	

	/**
		The count of kernel items must be equal. All kernel items must exist in passed node.
		@param o new node that contains only kernel items (which do not have dot at start).
	*/
	public boolean equals(Object o)	{
		//System.err.println("SLRSyntaxNode.equals: \n"+this+"\n with \n"+o);
		SLRSyntaxNode node = (SLRSyntaxNode)o;
		
		if (node.kernels != kernels)
			return false;
		
		// look if all entries are in the other node
		for (Enumeration e = entries.elements(); e.hasMoreElements(); )	{
			RuleStateItem item = (RuleStateItem)e.nextElement();
			// kernel items have pointer not at start
			if (item.pointerPosition > 1 && node.entries.containsKey(item) == false)
				return false;
		}
		return true;
	}

	/** Returns the hashcodes of all rule state items, associated by ^. The result gets buffered on first call. */
	public int hashCode()	{
		if (hashCache == null)	{
			int result = 0;
			for (Enumeration e = entries.elements(); e.hasMoreElements(); )
				result ^= e.nextElement().hashCode();
			hashCache = new Integer(result);
		}
		return hashCache.intValue();
	}


	/** Outputs this syntax node with all its rule state entries sorted. */
	public String toString()	{
		StringBuffer sb = new StringBuffer();
		// we want a sorted output of items, order by ruleIndex
		List list = new ArrayList(entries.size());
		for (Enumeration e = entries.elements(); e.hasMoreElements(); )	{
			RuleStateItem rsi = (RuleStateItem)e.nextElement();
			int index = -1;
			for (int i = 0; index == -1 && i < list.size(); i++)	{
				RuleStateItem rsi1 = (RuleStateItem) list.get(i);
				if (rsi1.ruleIndex > rsi.ruleIndex || rsi1.ruleIndex == rsi.ruleIndex && rsi.pointerPosition > 1)
					index = i;
			}
			if (index < 0)
				list.add(rsi);
			else
				list.add(index, rsi);
		}
		for (int i = 0; i < list.size(); i++)	{
			sb.append("  ");
			sb.append(list.get(i).toString());
			sb.append("\n");
		}
		return sb.toString();
	}



	// Helper that hold two RuleStateItems
	private class Tuple
	{
		RuleStateItem o1, o2;
		
		Tuple(RuleStateItem o1, RuleStateItem o2)	{
			this.o1 = o1;
			this.o2 = o2;
		}
	}



	/**
		Rule state entry item class, contained within SLR syntax node.
	*/
	protected class RuleStateItem
	{
		Rule rule;
		int pointerPosition = 1;
		int ruleIndex;
		int followNodeIndex = -1;
		protected Integer hashCache = null;
		
		
		/** Constructor with syntax rule index and rule. */
		public RuleStateItem(int ruleIndex, Rule rule)	{
			this.rule = rule;
			this.ruleIndex = ruleIndex;
		}
		
		/** Internal construction of shifted rule states. */
		protected RuleStateItem(RuleStateItem orig)	{
			this.rule = orig.rule;
			this.pointerPosition = orig.pointerPosition;
			this.ruleIndex = orig.ruleIndex;
		}

		
		/** Factory-method, to be overridden by subclasses. */
		protected RuleStateItem createRuleStateItem(RuleStateItem orig)	{
			return new RuleStateItem(orig);
		}
		
		/** Returns the nonterminal on the left side of the rule. */
		String getNonterminal()	{
			return rule.getNonterminal();
		}
		
		/** Returns a new shifted rule state item (dot has moved one position right). */
		RuleStateItem shift()	{
			RuleStateItem clone = createRuleStateItem(this);
			clone.pointerPosition++;
			return clone;
		}
		
		/** Returns null when no nonterminal is after dot, else the nonterminal to the right of the dot. */
		String getPendingNonTerminal()	{
			if (pointerPosition > rule.rightSize())
				return null;
				
			String symbol = getPendingSymbol();
			if (Token.isTerminal(symbol))
				return null;	// is a terminal
					
			return symbol;
		}
		
		/** Return pending symbol if pointer position is not at end, else null. */
		String getPendingSymbol()	{
			if (pointerPosition > rule.rightSize())
				return null;

			return rule.getRightSymbol(pointerPosition - 1);
		}

		/** The rule number and dot position must match for equality. */
		public boolean equals(Object o)	{
			RuleStateItem item = (RuleStateItem)o;
			return ruleIndex == item.ruleIndex && pointerPosition == item.pointerPosition;
		}
	
		/** Returns rule index * 13 + position of dot. */
		public int hashCode()	{
			if (hashCache == null)
				hashCache = new Integer(ruleIndex * 13 + pointerPosition);
			return hashCache.intValue();
		}
	
		/** String representation of rule state, showing index, rule and dot position. */
		public String toString()	{
			StringBuffer sb = new StringBuffer("(Rule "+ruleIndex+") "+getNonterminal()+" : ");
			int i = 0;
			for (; i < rule.rightSize(); i++)	{
				if (i == pointerPosition - 1)
					sb.append(".");
				sb.append(rule.getRightSymbol(i));
				sb.append(" ");
			}
			if (i == pointerPosition - 1)
				sb.append(".");
			if (followNodeIndex >= 0)
				sb.append(" -> State "+followNodeIndex);
			return sb.toString();
		}
	
	}

}
