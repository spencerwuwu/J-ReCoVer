// https://searchcode.com/api/result/126461454/

package fri.patterns.interpreter.parsergenerator.parsertables;

import java.util.*;
import java.io.PrintStream;
import fri.patterns.interpreter.parsergenerator.Token;
import fri.patterns.interpreter.parsergenerator.syntax.*;

/**
	A table generator, building SLR bottom-up parser tables from a syntax.
	<p>
	An artifical START-node gets inserted. Lists of nonterminals and terminals are created.
	Syntax nodes are created, from which the parser GOTO and PARSE-ACTION tables get built.
	<p>
	This class contains dump methods for syntax nodes and FIRST/FOLLOW sets.
	
	@author (c) 2000, Fritz Ritzberger
*/

public class SLRParserTables extends AbstractParserTables
{
	protected transient List syntaxNodes = new ArrayList();
	protected transient FirstSets firstSets;
	protected transient FollowSets followSets;


	/**
		Appends START symbol and retrieves all symbols. Calls init then.
		All parser table types run through this constructor.
	*/
	public SLRParserTables(Syntax syntax)
		throws ParserBuildException
	{
		this.syntax = addStartSymbol(syntax);	// add START symbol to begin
		getAllSymbols();
		init();
	}
	

	/** Builds SLR bottom-up parser tables. */
	protected void init()
		throws ParserBuildException
	{
		syntaxNodes = new SLRSyntaxNode().build(syntax, syntaxNodes, new Hashtable());
		
		gotoTable = generateGoto(syntaxNodes);

		Nullable nullAble = new Nullable(syntax, nonterminals);
		firstSets  = new FirstSets(syntax, nullAble, nonterminals);
		followSets = new FollowSets(syntax, nullAble, firstSets);
		
		parseTable = generateParseAction(syntaxNodes);
	}


	// Looks for top level rules and inserts a START rule pointing to it.
	// Inserts a default rule pointing to the first rule when none found.
	private Syntax addStartSymbol(Syntax syntax)
		throws ParserBuildException
	{
		String startSym = null;
		List startRules = syntax.findStartRules();
		
		if (startRules.size() <= 0)	{	// no toplevel rule
			//throw new ParserBuildException("Grammatik hat keine Start-Regel!");
			Rule rule = syntax.getRule(0);
			System.err.println("WARNING: Grammar has no top level rule, taking first rule >"+rule+"<");
			startSym = rule.getNonterminal();
		}
		else
		if (startRules.size() > 1)	{	// more than one toplevel rules
			for (int i = 0; i < startRules.size(); i++)	{	// check if all start rules have the same nonterminal on left side
				Rule r = (Rule) startRules.get(i);
				String nt = r.getNonterminal();

				if (startSym == null)
					startSym = nt;
				else
				if (startSym.equals(nt) == false)
					throw new ParserBuildException("Grammar has more than one toplevel rules: "+startRules);
			}
		}
		else	{	// exactly one toplevel rule found
			startSym = ((Rule) startRules.get(0)).getNonterminal();
		}
		
		Rule start = new Rule("<START>", 1);
		start.addRightSymbol(startSym);
		syntax.insertRule(0, start);
		
		return syntax;
	}


	/** Returns all symbols (terminals and nonterminals). Builds lists. Called only once on construction. */
	protected List getAllSymbols()
		throws ParserBuildException
	{
		// collect nonterminals unique
		for (int i = 0; i < syntax.size(); i++)	{
			Rule rule = syntax.getRule(i);
			String nonterm = rule.getNonterminal();	// left side of derivation
				
			if (Nullable.isNull(nonterm))
				throw new ParserBuildException("ERROR: Empty nonterminal: >"+nonterm+"<");

			if (nonterminals.indexOf(nonterm) < 0)	// insert unique
				nonterminals.add(nonterm);
		}

		// collect terminals unique
		for (int j = 0; j < syntax.size(); j++)	{
			Rule rule = syntax.getRule(j);

			for (int i = 0; i < rule.rightSize(); i++)	{
				String symbol = rule.getRightSymbol(i);
				
				if (Nullable.isNull(symbol))
					throw new ParserBuildException("ERROR: Empty terminal: >"+symbol+"<");

				if (Token.isTerminal(symbol))	{
					if (terminals.indexOf(symbol) < 0)	{
						terminals.add(symbol);
						terminalsWithoutEpsilon.add(symbol);
					}
				}
				else	// throw error if nonterminal is not present
				if (nonterminals.indexOf(symbol) < 0)
					throw new ParserBuildException("ERROR: Every nonterminal must have a rule. symbol: >"+symbol+"<, rule: "+rule);
			}
		}

		// check for sanity
		if (terminals.size() <= 0)
			throw new ParserBuildException("ERROR: No terminal found: "+syntax);

		if (nonterminals.size() <= 0)
			throw new ParserBuildException("ERROR: No nonterminal found: "+syntax);

		// add nonterminals to symbols
		for (int i = 0; i < nonterminals.size(); i++)
			symbols.add(nonterminals.get(i));

		// add terminals without EpSiLoN to symbols
		for (int i = 0; i < terminals.size(); i++)
			symbols.add(terminals.get(i));

		// add "Epsilon" Symbol to Terminals
		terminals.add(Token.EPSILON);

		return symbols;
	}
	
	
	/** Creates GOTO table of follow states. */
	protected List generateGoto(List syntaxNodes)	{
		//System.err.println("got "+syntaxNodes.size()+" states");
		this.gotoTable = new ArrayList(syntaxNodes.size());
		
		Map hash = new Hashtable(syntaxNodes.size());

		for (int i = 0; i < syntaxNodes.size(); i++)	{
			SLRSyntaxNode node = (SLRSyntaxNode) syntaxNodes.get(i);

			Map h = node.fillGotoLine(i);
			
			if (h.size() <= 0)
				gotoTable.add(null);
			else
				insertTableLine(i, h, gotoTable, hash);
		}

		return gotoTable;
	}
	
	
	/**
		Erzeugen der Parse-Action-Tabelle fuer shift/reduce Aktionen.
	*/
	protected List generateParseAction(List syntaxNodes)	{
		this.parseTable = new ArrayList(syntaxNodes.size());

		Map hash = new Hashtable(syntaxNodes.size());
		
		for (int i = 0; i < syntaxNodes.size(); i++)	{
			SLRSyntaxNode node = (SLRSyntaxNode) syntaxNodes.get(i);

			Map h = node.fillParseActionLine(i, firstSets, followSets);

			if (h.size() <= 0)
				parseTable.add(null);
			else
				insertTableLine(i, h, parseTable, hash);
		}

		return parseTable;
	}

	
	/**
		Compression of tables: Look for an identical table line.
		@return identical line Zeile, or passed line when not found.
	*/
	protected void insertTableLine(
		int i,
		Map line,
		List table,
		Map hash)
	{
		Integer itg = (Integer) hash.get(line);
		if (itg == null)	{
			table.add(line);
			hash.put(line, new Integer(i));
		}
		else	{
			table.add(table.get(itg.intValue()));
		}
	}



	/** Enable garbage collection of builder variables. CAUTION: dump methods work reduced after this call. */
	public void freeSyntaxNodes()	{
		syntaxNodes = null;
		symbols = null;
		terminals = null;
	}
	
	

	// dump methods to print parser information

	/** Overridden to report better. */
	public void report(PrintStream out)	{
		System.err.println("Parser Generator is "+getClass());
		super.report(out);
		out.println("states: "+(syntaxNodes != null ? syntaxNodes.size() : -1));
	}


	/** Implements ParserTables: output of rules, FIRST/FOLLOW sets, syntax nodes, GOTO-table, PARSE-ACTON-table. */
	public void dump(PrintStream out)	{
		dumpSyntax(out);
		dumpFirstSet(out);
		dumpFollowSet(out);
		dumpSyntaxNodes(out);
		dumpGoto(out);
		dumpParseAction(out);
	}
	
	public void dumpSyntaxNodes(PrintStream out)	{
		if (syntaxNodes != null)	{
			for (int i = 0; i < syntaxNodes.size(); i++)
				dumpSyntaxNode(i, (SLRSyntaxNode) syntaxNodes.get(i), out);
			out.println();
		}
	}
	
	public void dumpSyntaxNode(int i, SLRSyntaxNode node, PrintStream out)	{
		out.println("State "+i);
		out.println(node.toString());
	}
	
	public void dumpFirstSet(PrintStream out)	{
		if (firstSets != null)
			dumpSet("FIRST", firstSets, out);
	}
	
	public void dumpFollowSet(PrintStream out)	{
		if (followSets != null)
			dumpSet("FOLLOW", followSets, out);
	}
	
	public void dumpSet(String header, Map set, PrintStream out)	{
		for (Iterator it = set.keySet().iterator(); it.hasNext(); )	{
			String nonterm = (String) it.next();
			out.println(header+"("+nonterm+") = "+set.get(nonterm));
		}
		out.println();
	}
	
	
	
	/** Test main dumping arithmetic expression tables. */
	public static void main(String [] args)	{
		String [][] syntax = {
			{ "EXPR", "TERM" },
			{ "EXPR", "EXPR", "'+'", "TERM" },
			{ "EXPR", "EXPR", "'-'", "TERM" },
			{ "TERM", "FAKT", },
			{ "TERM", "TERM", "'*'", "FAKT" },
			{ "TERM", "TERM", "'/'", "FAKT" },
			{ "FAKT", "`number`", },
			{ "FAKT", "'('", "EXPR", "')'" },
		};
		
		try	{
			SLRParserTables p = new SLRParserTables(new Syntax(syntax));
			p.dump(System.err);
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
	}
	
}

/*
(Rule 0)  <START> : EXPR 
(Rule 1)  EXPR : TERM 
(Rule 2)  EXPR : EXPR '+' TERM 
(Rule 3)  EXPR : EXPR '-' TERM 
(Rule 4)  TERM : FAKT 
(Rule 5)  TERM : TERM '*' FAKT 
(Rule 6)  TERM : TERM '/' FAKT 
(Rule 7)  FAKT : "[0-9]+" 
(Rule 8)  FAKT : '(' EXPR ')' 

FIRST(EXPR) = ["[0-9]+", '(']
FIRST(FAKT) = ["[0-9]+", '(']
FIRST(<START>) = ["[0-9]+", '(']
FIRST(TERM) = ["[0-9]+", '(']

FOLLOW(FAKT) = [Epsilon, '+', '-', ')', '*', '/']
FOLLOW(EXPR) = [Epsilon, '+', '-', ')']
FOLLOW(<START>) = [Epsilon]
FOLLOW(TERM) = [Epsilon, '+', '-', ')', '*', '/']

State 0
  (Rule 0) <START> : .EXPR  -> State 2
  (Rule 1) EXPR : .TERM  -> State 1
  (Rule 2) EXPR : .EXPR '+' TERM  -> State 2
  (Rule 3) EXPR : .EXPR '-' TERM  -> State 2
  (Rule 4) TERM : .FAKT  -> State 4
  (Rule 5) TERM : .TERM '*' FAKT  -> State 1
  (Rule 6) TERM : .TERM '/' FAKT  -> State 1
  (Rule 7) FAKT : ."[0-9]+"  -> State 5
  (Rule 8) FAKT : .'(' EXPR ')'  -> State 3

State 1
  (Rule 1) EXPR : TERM .
  (Rule 5) TERM : TERM .'*' FAKT  -> State 7
  (Rule 6) TERM : TERM .'/' FAKT  -> State 6

State 2
  (Rule 0) <START> : EXPR .
  (Rule 2) EXPR : EXPR .'+' TERM  -> State 9
  (Rule 3) EXPR : EXPR .'-' TERM  -> State 8

State 3
  (Rule 1) EXPR : .TERM  -> State 1
  (Rule 2) EXPR : .EXPR '+' TERM  -> State 10
  (Rule 3) EXPR : .EXPR '-' TERM  -> State 10
  (Rule 4) TERM : .FAKT  -> State 4
  (Rule 5) TERM : .TERM '*' FAKT  -> State 1
  (Rule 6) TERM : .TERM '/' FAKT  -> State 1
  (Rule 7) FAKT : ."[0-9]+"  -> State 5
  (Rule 8) FAKT : '(' .EXPR ')'  -> State 10
  (Rule 8) FAKT : .'(' EXPR ')'  -> State 3

State 4
  (Rule 4) TERM : FAKT .

State 5
  (Rule 7) FAKT : "[0-9]+" .

State 6
  (Rule 6) TERM : TERM '/' .FAKT  -> State 11
  (Rule 7) FAKT : ."[0-9]+"  -> State 5
  (Rule 8) FAKT : .'(' EXPR ')'  -> State 3

State 7
  (Rule 5) TERM : TERM '*' .FAKT  -> State 12
  (Rule 7) FAKT : ."[0-9]+"  -> State 5
  (Rule 8) FAKT : .'(' EXPR ')'  -> State 3

State 8
  (Rule 3) EXPR : EXPR '-' .TERM  -> State 13
  (Rule 4) TERM : .FAKT  -> State 4
  (Rule 5) TERM : .TERM '*' FAKT  -> State 13
  (Rule 6) TERM : .TERM '/' FAKT  -> State 13
  (Rule 7) FAKT : ."[0-9]+"  -> State 5
  (Rule 8) FAKT : .'(' EXPR ')'  -> State 3

State 9
  (Rule 2) EXPR : EXPR '+' .TERM  -> State 14
  (Rule 4) TERM : .FAKT  -> State 4
  (Rule 5) TERM : .TERM '*' FAKT  -> State 14
  (Rule 6) TERM : .TERM '/' FAKT  -> State 14
  (Rule 7) FAKT : ."[0-9]+"  -> State 5
  (Rule 8) FAKT : .'(' EXPR ')'  -> State 3

State 10
  (Rule 2) EXPR : EXPR .'+' TERM  -> State 9
  (Rule 3) EXPR : EXPR .'-' TERM  -> State 8
  (Rule 8) FAKT : '(' EXPR .')'  -> State 15

State 11
  (Rule 6) TERM : TERM '/' FAKT .

State 12
  (Rule 5) TERM : TERM '*' FAKT .

State 13
  (Rule 3) EXPR : EXPR '-' TERM .
  (Rule 5) TERM : TERM .'*' FAKT  -> State 7
  (Rule 6) TERM : TERM .'/' FAKT  -> State 6

State 14
  (Rule 2) EXPR : EXPR '+' TERM .
  (Rule 5) TERM : TERM .'*' FAKT  -> State 7
  (Rule 6) TERM : TERM .'/' FAKT  -> State 6

State 15
  (Rule 8) FAKT : '(' EXPR ')' .


GOTO TABLE
==========
      |  <START>    EXPR    TERM    FAKT     '+'     '-'     '*'     '/' "[0-9]+"    '('     ')'
________________________________________________________________________________________________
    0 |        -       2       1       4       -       -       -       -       5       3       -
    1 |        -       -       -       -       -       -       7       6       -       -       -
    2 |        -       -       -       -       9       8       -       -       -       -       -
    3 |        -      10       1       4       -       -       -       -       5       3       -
    4 |        -       -       -       -       -       -       -       -       -       -       -
    5 |        -       -       -       -       -       -       -       -       -       -       -
    6 |        -       -       -      11       -       -       -       -       5       3       -
    7 |        -       -       -      12       -       -       -       -       5       3       -
    8 |        -       -      13       4       -       -       -       -       5       3       -
    9 |        -       -      14       4       -       -       -       -       5       3       -
   10 |        -       -       -       -       9       8       -       -       -       -      15
   11 |        -       -       -       -       -       -       -       -       -       -       -
   12 |        -       -       -       -       -       -       -       -       -       -       -
   13 |        -       -       -       -       -       -       7       6       -       -       -
   14 |        -       -       -       -       -       -       7       6       -       -       -
   15 |        -       -       -       -       -       -       -       -       -       -       -

PARSE-ACTION TABLE
==================
      |      '+'     '-'     '*'     '/' "[0-9]+"    '('     ')'   <EOF>
________________________________________________________________________
    0 |        -       -       -       -      SH      SH       -       -
    1 |        1       1      SH      SH       -       -       1       1
    2 |       SH      SH       -       -       -       -       -      AC
    3 |        -       -       -       -      SH      SH       -       -
    4 |        4       4       4       4       -       -       4       4
    5 |        7       7       7       7       -       -       7       7
    6 |        -       -       -       -      SH      SH       -       -
    7 |        -       -       -       -      SH      SH       -       -
    8 |        -       -       -       -      SH      SH       -       -
    9 |        -       -       -       -      SH      SH       -       -
   10 |       SH      SH       -       -       -       -      SH       -
   11 |        6       6       6       6       -       -       6       6
   12 |        5       5       5       5       -       -       5       5
   13 |        3       3      SH      SH       -       -       3       3
   14 |        2       2      SH      SH       -       -       2       2
   15 |        8       8       8       8       -       -       8       8

*/
