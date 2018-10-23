// https://searchcode.com/api/result/126461457/

package fri.patterns.interpreter.parsergenerator.parsertables;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import fri.patterns.interpreter.parsergenerator.ParserTables;
import fri.patterns.interpreter.parsergenerator.Token;
import fri.patterns.interpreter.parsergenerator.syntax.*;

/**
	Base class for all parser tables. Default implementations.
	<p>
	Source generation method: <i>toSourceFile()</i>. Needed by <i>CompiledTables</i>,
	used for buffering compiled table classes at runtime.
	<p>
	Dump methods to print human readable table contents. Dumping does not work on
	buffered table objects (needs scratch build by constructor).

	@author (c) 2000, 2001, 2002 Fritz Ritzberger
*/

public abstract class AbstractParserTables implements
	ParserTables,
	Serializable
{
	/** The raw syntax to apply on parsing. */
	protected Syntax syntax;

	/** The GOTO-table with all possible follow states. As much lines as states and columns as terminals and nonterminals are present. */
	protected List gotoTable;

	/** The PARSE-ACTION table with SHIFT and REDUCE entries. As much lines as states and as much columns as terminals are present. */
	protected List parseTable;

	/** Non-terminals and terminals, without EPSILON, with START-symbol: column header of GOTO-table. */
	protected transient List symbols = new ArrayList();

	/** Terminals, with EPSILON: column header of PARSE-ACTION table. */
	protected transient List terminals = new ArrayList();

	/** Terminals, without EPSILON: tokens for the Lexer. */
	protected List terminalsWithoutEpsilon = new ArrayList();

	/** Non-terminals, with START-symbol. */
	protected transient List nonterminals = new ArrayList();

	/** Helper for the cell width of printed tables. Can be set manually if automatic result does not fit. */
	public transient static int CELLWIDTH = 0;


	/** Serializable constructor. */
	protected AbstractParserTables()	{
	}
	
	/**
		Implements ParserTables: returns the next state from GOTO-table.
		@param currentState curent state
		@param symbol current terminal or nonterminal
		@return new state
	*/
	public Integer getGotoState(Integer currentState, String symbol)	{
		Integer state = null;
		Map map = (Map) gotoTable.get(currentState.intValue());
		if (map != null)
			state = (Integer) map.get(symbol);
		return state == null ? ParserTables.ERROR : state;
	}

	/**
		Implements ParserTables: returns the next action from PARSE-ACTION-table.
		@param currentState curent state
		@param symbol current terminal
		@return new action
	*/
	public Integer getParseAction(Integer currentState, String terminal)	{
		Integer action = null;
		Map map = (Map) parseTable.get(currentState.intValue());
		if (map != null)
			action = (Integer) map.get(terminal);
		return action == null ? ParserTables.ERROR : action;
	}

	/** Implements ParserTables: returns String List of terminals, without EPSILON. */
	public List getTerminals()	{
		return terminalsWithoutEpsilon;
	}
	
	/** Implements ParserTables: returns Syntax. */
	public Syntax getSyntax()	{
		return syntax;
	}

	/** Implements ParserTables: Returns a Map of the expected tokens for current state, contained in keySet iterator. */
	public Map getExpected(Integer state)	{
		return (Map) parseTable.get(state.intValue());
	}

	

	/**
		Factory to build precompiled parser tables, or construct them from scratch.
		Used by <i>CompiledTables</i> and <i>SerializedTables</i>.
		@param parserType LALRParserTables, SLRParserTables, LRParserTables class.
		@param syntax can be null for precompiled tables, else the syntax to build.
	*/
	public static AbstractParserTables construct(Class parserType, Syntax syntax)
		throws Exception
	{
		Constructor constr;
		if (syntax == null)	{	// load compiled table
			constr = parserType.getConstructor(new Class [0]);
			return (AbstractParserTables)constr.newInstance(new Object [0]);
		}
		else	{	// build tables from scratch
			try	{	// if this fails
				constr = parserType.getConstructor(new Class [] { syntax.getClass() });
			}
			catch (Exception e)	{	// try Object as parameter for Constructor
				constr = parserType.getConstructor(new Class [] { Object.class });
			}
			return (AbstractParserTables)constr.newInstance(new Object [] { syntax });
		}
	}

	


	// dump methods, display rules and tables	


	/** Implements ParserTables: prints rules, FIRST/FOLLOW sets, syntax nodes (states), GOTO-table, PARSE-ACTION-table. */
	public void dump(PrintStream out)	{
		dumpSyntax(out);
		dumpTables(out);
	}
	
	public void dumpTables(PrintStream out)	{
		dumpGoto(out);
		dumpParseAction(out);
	}
	
	public void dumpSyntax(PrintStream out)	{
		for (int i = 0; i < syntax.size(); i++)
			out.println(dumpRule(syntax.getRule(i), i));
		out.println();
	}

	protected String dumpRule(Rule rule, int i)	{
		StringBuffer sb = new StringBuffer("(Rule "+i+")  "+rule.getNonterminal()+" : ");
		for (int j = 0; j < rule.rightSize(); j++)
			sb.append(rule.getRightSymbol(j)+" ");
		return sb.toString();
	}
	
	protected void dumpGoto(PrintStream out)	{
		if (symbols.size() > 0)
			dumpTable("GOTO TABLE", symbols, gotoTable, out);
	}
	
	protected void dumpParseAction(PrintStream out)	{
		if (terminals.size() > 0)
			dumpTable("PARSE-ACTION TABLE", terminals, parseTable, out);
	}
	
	
	protected void dumpTable(String title, List head, List table, PrintStream out)	{
		out.println(title);
		out.println(dummy(title.length(), "="));
		
		// if no CELLWIDTH is set, estimate maximum cell width
		if (CELLWIDTH <= 0)	{
			for (Iterator it = head.iterator(); it.hasNext(); )	{
				String s = (String) it.next();
				if (s.length() > CELLWIDTH && it.hasNext())	// not last
					CELLWIDTH = s.length() + 1;
			}
		}
		
		// print table header
		dumpCell(" | ", CELLWIDTH, false, out);			
		for (Iterator it = head.iterator(); it.hasNext(); )	{
			String s = (String) it.next();
			if (Token.isEpsilon(s))
				s = "<EOF>";
			dumpCell(s, CELLWIDTH, ! it.hasNext(), out);			
		}
		
		out.println();
		out.println(dummy(CELLWIDTH * (head.size() + 1), "_"));
		
		int i = 0;
		for (Iterator it = table.iterator(); it.hasNext(); i++)	{
			Map h = (Map) it.next();

			dumpCell(Integer.toString(i)+" | ", CELLWIDTH, false, out);			
			for (Iterator it2 = head.iterator(); it2.hasNext(); )	{
				String symbol = (String) it2.next();

				Integer intg = h != null ? (Integer) h.get(symbol) : null;
				String digit = intg == null ? "-" : intg.equals(ParserTables.SHIFT) ? "SH" : intg.equals(ParserTables.ACCEPT) ? "AC" : intg.toString();

				dumpCell(digit, CELLWIDTH, ! it2.hasNext(), out);
			}
			out.println();
		}
		
		out.println();
	}
	
	private void dumpCell(String digit, int width, boolean isLast, PrintStream out)	{
		StringBuffer tab = new StringBuffer(" ");
		for (int i = 0; i < width - 1 - digit.length(); i++)
			tab.append(' ');
			
		String s = tab+digit;
		if (!isLast && s.length() > width && width > 2)
			s = s.substring(0, width);
			
		out.print(s);
	}

	private String dummy(int width, String ch)	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < width; i++)
			sb.append(ch);
		return sb.toString();
	}


	/** Reports count of rules, ternminals and nonterminals. */
	public void report(PrintStream out)	{
		out.println("rules: "+syntax.size());
		out.println("terminals: "+terminals.size());
		out.println("nonterminals: "+nonterminals.size());
	}



	
	// Java source generation methods

	
	private transient Writer f = null;

	private void fwrite(String line)
		throws IOException
	{
		f.write(line, 0, line.length());
	}
	
	/**
		Generates a compilable source file "parserClassName.java".
		If the class name contains package path, the file will be generated
		within the corresponding directory, else within working directory.
		@param parserClassName name of class to generate:
				"Java" will generate "Java.java" in current directory,
				"my.pkg.PT" will generate "my/pkg/PT.java" in current directory.
		@return fileName if File could be written, else null.
	*/
	public String toSourceFile(String parserClassName)
		throws IOException
	{
		if (parserClassName.endsWith("ParserTables") == false)
			parserClassName = parserClassName+"ParserTables";
			
		String fileName = parserClassName.replace('.', File.separatorChar)+".java";
		System.err.println("Writing Java Source to "+fileName);

		File file = new File(fileName);
		String path = file.getParent();
		if (path != null && new File(path).exists() == false)
			new File(path).mkdirs();

		// begin file writing
		f = new BufferedWriter(new FileWriter(file));
		
		// header of Java source
		if (path != null)	{
			if (path.endsWith(File.separator))
				path = path.substring(0, path.length() - 1);

			fwrite("package "+path.replace(File.separatorChar, '.')+";\n\n");
			
			parserClassName = parserClassName.substring(parserClassName.lastIndexOf(".") + 1);
		}

		fwrite("import java.util.*;\n");
		fwrite("import fri.patterns.interpreter.parsergenerator.syntax.*;\n");
		fwrite("import fri.patterns.interpreter.parsergenerator.parsertables.AbstractParserTables;\n\n");
		fwrite("/**\n");
		fwrite(" * DO NOT EDIT - ParserTables generated\n");
		fwrite(" * at "+new Date()+"\n");
		fwrite(" * by fri.patterns.interpreter.parsergenerator.parsertables.AbstractParserTables.\n");
		fwrite(" */\n\n");
		fwrite("public final class "+parserClassName+" extends AbstractParserTables\n");
		fwrite("{\n");

		// begin constructor
		fwrite("	public "+parserClassName+"()	{\n");

		fwrite("		syntax = new Syntax("+syntax.size()+");\n");
		fwrite("		Rule s;\n");
		fwrite("\n");
		for (int i = 0; i < syntax.size(); i++)	{
			Rule s = syntax.getRule(i);
			fwrite("		syntax.addRule(s = new Rule(\""+s.getNonterminal()+"\", "+s.rightSize()+"));	// rule "+i+"\n");
			for (int j = 0; j < s.rightSize(); j++)
				fwrite("		s.addRightSymbol(\""+sub(s.getRightSymbol(j))+"\");\n");
			fwrite("\n");
		}
		fwrite("\n");

		// call methods to build tables
		fwrite("		loadGotoTable();\n");
		fwrite("		loadParseActionTable();\n");
		fwrite("\n");

		// load terminal list that gets passed to Lexer
		fwrite("		terminalsWithoutEpsilon = new ArrayList("+terminalsWithoutEpsilon.size()+");\n");
		for (int i = 0; i < terminalsWithoutEpsilon.size(); i++)
			fwrite("		terminalsWithoutEpsilon.add(\""+sub(terminalsWithoutEpsilon.get(i))+"\");\n");

		fwrite("	}\n");
		// end constructor


		// method to load Goto table
		fwrite("	private void loadGotoTable()	{\n");
		fwrite("		gotoTable = new ArrayList("+gotoTable.size()+");\n");
		fwrite("\n");
		for (int i = 0; i < gotoTable.size(); i++)	{
			Map g = (Map) gotoTable.get(i);
			if (g == null)
				fwrite("		gotoTable.add(null);  // state "+i);
			else
				fwrite("		loadGoto_"+i+"();");
			fwrite("\n");
		}
		fwrite("	}\n");

		// every goto state is a method as Java can not load methods bigger than 65563 Bytes
		for (int i = 0; i < gotoTable.size(); i++)	{
			Map g = (Map) gotoTable.get(i);
			if (g != null)	{
				fwrite("	private void loadGoto_"+i+"()	{\n");
				fwrite("		Hashtable g = new Hashtable("+g.size()+", 1);\n");
				fwrite("		gotoTable.add(g);\n");
				for (Iterator it = g.keySet().iterator(); it.hasNext(); )	{
					String key = (String) it.next();
					Object value = g.get(key);
					fwrite("		g.put(\""+sub(key)+"\", new Integer("+value+"));\n");
				}
				fwrite("	}\n");
			}
		}

		// method to load Parse-Action table
		fwrite("	private void loadParseActionTable()	{\n");
		fwrite("		parseTable = new ArrayList("+parseTable.size()+");\n");
		fwrite("\n");
		for (int i = 0; i < parseTable.size(); i++)	{
			Map p = (Map) parseTable.get(i);
			if (p == null)
				fwrite("		parseTable.add(null);  // state "+i);
			else
				fwrite("		loadParseAction_"+i+"();");
			fwrite("\n");
		}
		fwrite("	}\n");

		// every action state is a method as Java can not load methods bigger than 65563 Bytes
		for (int i = 0; i < parseTable.size(); i++)	{
			Map p = (Map) parseTable.get(i);
			if (p != null)	{
				fwrite("	private void loadParseAction_"+i+"()	{\n");
				fwrite("		Hashtable p = new Hashtable("+p.size()+", 1);\n");
				fwrite("		parseTable.add(p);\n");
				for (Iterator it = p.keySet().iterator(); it.hasNext(); )	{
					String key = (String) it.next();
					Object value = p.get(key);
					fwrite("		p.put(\""+sub(key)+"\", new Integer("+value+"));\n");
				}
				fwrite("	}\n");
			}
		}

		fwrite("}");

		// end file writing
		f.flush();
		f.close();
		f = null;
		
		return fileName;
	}


	private String sub(Object o)	{
		return SyntaxUtil.maskQuoteAndBackslash((String) o);
	}
	
}
