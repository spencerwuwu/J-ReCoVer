// https://searchcode.com/api/result/126461458/

package fri.patterns.interpreter.parsergenerator.parsertables;

import java.util.*;
import fri.patterns.interpreter.parsergenerator.Token;
import fri.patterns.interpreter.parsergenerator.syntax.*;

/**
	LR bottom-up parser syntax node. This node type contains lookahead-sets
	within its items, calculated from FIRST sets and nullability.

	@see fri.patterns.interpreter.parsergenerator.parsertables.SLRSyntaxNode
	@author (c) 2000, Fritz Ritzberger
*/

class LRSyntaxNode extends SLRSyntaxNode
{
	/** LR node must know about nullability. */
	protected Nullable nullable;
	
	/** LR node must know FIRST sets. */
	protected FirstSets firstSets;
	

	/** Construction of node with FIRST-sets and nullability of all nonterminals in syntax. */
	public LRSyntaxNode(Nullable nullable, FirstSets firstSets)	{
		this.nullable = nullable;
		this.firstSets = firstSets;
	}
	
	
	/** Factory-method that constructs a LRSyntaxNode. */
	protected SLRSyntaxNode createSyntaxNode()	{
		return new LRSyntaxNode(nullable, firstSets);
	}
	
	/**
		Factory-method that constructs a LRRuleStateItem.
		A start-lookahead gets appended to the item when it is the start node.
	*/
	protected RuleStateItem createRuleStateItem(int ruleIndex, Rule rule)	{
		LRRuleStateItem item = new LRRuleStateItem(ruleIndex, rule);
		addStartLookahead(item, ruleIndex);
		return item;
	}
	
	/** When start node (ruleIndex == 0), add EPSILON lookahead. */
	protected void addStartLookahead(LRRuleStateItem item, int ruleIndex)	{
		if (ruleIndex == 0)	{
			List list = new ArrayList();
			list.add(Token.EPSILON);
			item.addLookahead(list.iterator());
		}
	}

	/**
		Method called from closure, adopt all rules that derive the pending nonterminal.
		Default lookaheads are calculated here.
	*/
	protected void addRulesDerivingPendingNonTerminal(RuleStateItem itm, String nonterm, Syntax syntax, List newItems)	{
		// make the closure for one item:
		// if pointer before a nonterminal, add all rules that derive it
		
		LRRuleStateItem item = (LRRuleStateItem)itm;
		List lookahead = null;

		for (int i = 0; i < syntax.size(); i++)	{
			Rule rule = syntax.getRule(i);
			
			if (rule.getNonterminal().equals(nonterm))	{
				LRRuleStateItem rsi = (LRRuleStateItem) createRuleStateItem(i, rule);
				
				if (lookahead == null)	// calculate lookahead, all new items get the same lookahead
					item.calculateLookahead(
							lookahead = new ArrayList(),
							nullable,
							firstSets);
				
				rsi.addLookahead(lookahead.iterator());	// merge lookaheads
				
				// look if new item is already contained, add when not
				if (entries.containsKey(rsi) == false)	{
					entries.put(rsi, rsi);
					newItems.add(rsi);
				}
			}
		}
	}


	/**
		Returns all symbols for which SHIFT must be put into PARSE-ACTION table for a nonterminal.
		For LR and LALR this returns null.
	*/
	protected List getNontermShiftSymbols(FirstSets firstSets, String nonterm)	{
		return null;
	}

	/**
		Returns all symbols for which REDUCE must be put into PARSE-ACTION table.
		For LR and LALR this returns the lookahead of the passed item.
	*/
	protected Iterator getReduceSymbols(FollowSets followSets, RuleStateItem item)	{
		return ((LRRuleStateItem) item).lookahead.keySet().iterator();
	}




	/**
		Rule state entry item class, contained within LR syntax node.
		Adds calculation and adoption of lookaheads functionality to super class.
	*/
	protected class LRRuleStateItem extends RuleStateItem
	{
		Hashtable lookahead = new Hashtable();
		
		public LRRuleStateItem(int ruleIndex, Rule rule)	{
			super(ruleIndex, rule);
		}
		
		/** Internal clone constructor, lookahead gets copied by cloning. */
		protected LRRuleStateItem(RuleStateItem orig)	{
			super(orig);
			lookahead = (Hashtable) ((LRRuleStateItem) orig).lookahead.clone();
		}

		/** Factory-method to create LRRuleStateItem. */
		protected RuleStateItem createRuleStateItem(RuleStateItem orig)	{
			return new LRRuleStateItem(orig);
		}
		
		/**
			Add (new) lookaheads when not already contained.
			@return true when change occured (needed in LALR).
		*/
		boolean addLookahead(Iterator propagation)	{
			// merge lookaheads
			boolean ret = false;	// assume no changes
				
			while (propagation.hasNext())	{
				Object la = propagation.next();
				
				if (lookahead.get(la) == null)	{
					lookahead.put(la, la);
					ret = true;	// there were changes
				}
			}
			return ret;
		}
		
		/**
			Calculate the lookahead for a rule state. The result is returned in
			newLookahead argument. These go to lookahead: FIRST set of second symbol after
			the dot (terminal is taken itself), and all FIRST sets of following
			symbols as long as they are nullable (terminal is not nullable).
			When symbols all were nullable, the lookahead of this item also goes to lookahead.
			@return true when all symbols after the dot in the rule were nullable.
					In LALR the originator item then propagates its lookaheads to this one.
		*/
		boolean calculateLookahead(List newLookahead, Nullable nullable, FirstSets firstSets)	{
			// consider all nullable symbols after the one past the dot
			// and add their first symbols to lookahead set.
			
			for (int i = pointerPosition; i < rule.rightSize(); i++)	{	// when pointer at start, it has value 1
				String symbol = rule.getRightSymbol(i);
				
				if (Token.isTerminal(symbol))	{
					newLookahead.add(symbol);
					return false;	// originator lookahead not visible
				}
				else	{
					List firstSet = (List) firstSets.get(symbol);
					
					for (int j = 0; j < firstSet.size(); j++)	{
						String la = (String) firstSet.get(j);
						newLookahead.add(la);
					}
					
					if (nullable.isNullable(symbol) == false)
						return false;
				}
			}
			
			// if we get here everything was nullable, add all lookaheads of this item
			for (Enumeration e = lookahead.keys(); e.hasMoreElements(); )
				newLookahead.add((String)e.nextElement());
			
			return true;	// originator lookahead is visible
		}

		/** Rule index, dot position and lookahead must be equal. */
		public boolean equals(Object o)	{
			if (super.equals(o) == false)
				return false;
				
			LRRuleStateItem item = (LRRuleStateItem)o;
			if (item.lookahead.equals(lookahead) == false)
				return false;

			return true;
		}

		/** All lookahead hashcodes are associated by ^, plus rule index * 13 + dot position. */
		public int hashCode()	{
			if (hashCache == null)	{
				int result = 0;
				for (Enumeration e = lookahead.keys(); e.hasMoreElements(); )
					result ^= e.nextElement().hashCode();
				hashCache = new Integer(ruleIndex * 13 + pointerPosition + result);
			}
			return hashCache.intValue();
		}

		/** String representation from super, lookahead appended. */
		public String toString()	{
			String s = super.toString();
			int i = s.lastIndexOf("->");
			if (i > 0)
				s = s.substring(0, i) + "LOOKAHEAD" + hashToStr(lookahead) + "	" + s.substring(i);
			else
				s = s + "	" + hashToStr(lookahead);
			return s;
		}
		
		// output of hashtable keys, separated by ", ".
		private String hashToStr(Hashtable l)	{
			StringBuffer sb = new StringBuffer("[");
			for (Enumeration e = l.keys(); e.hasMoreElements(); )	{
				String s = (String)e.nextElement();
				sb.append(s);
				if (e.hasMoreElements())
					sb.append(", ");
				else
				if (l.size() == 1 && s.length() <= 0)	// only Epsilon
					sb.append(" ");
			}
			sb.append("]");
			return sb.toString();
		}

	}	// end class RuleStateItem

}
