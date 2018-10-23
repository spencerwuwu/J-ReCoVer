// https://searchcode.com/api/result/17061019/


/* A Bison parser, made by GNU Bison 2.4.  */

/* Skeleton implementation for Bison LALR(1) parsers in Java
   
      Copyright (C) 2007, 2008 Free Software Foundation, Inc.
   
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.  */

/* As a special exception, you may create a larger work that contains
   part or all of the Bison parser skeleton and distribute that work
   under terms of your choice, so long as that work isn't itself a
   parser generator using the skeleton or a modified version thereof
   as a parser skeleton.  Alternatively, if you modify or redistribute
   the parser skeleton itself, you may (at your option) remove this
   special exception, which will cause the skeleton and the resulting
   Bison output files to be licensed under the GNU General Public
   License without this special exception.
   
   This special exception was added by the Free Software Foundation in
   version 2.2 of Bison.  */

package com.blogspot.miguelinlas3.bison;
/* First part of user declarations.  */

/* Line 32 of lalr1.java  */
/* Line 1 of "RBisonParser.y"  */
	
	
		// lang imports
	import java.lang.Integer;
	import java.lang.Double;
	
	// Collections imports
	import java.util.ArrayList;
	import java.util.Iterator;
	import java.util.List;
	
	// AST NODES imports
	import org.eclipse.dltk.ast.ASTNode;
	import org.eclipse.dltk.ast.ASTListNode;
	import org.eclipse.dltk.ast.expressions.CallArgumentsList;
	import org.eclipse.dltk.ast.declarations.ModuleDeclaration;
	import org.eclipse.dltk.ast.statements.Statement;
	import org.eclipse.dltk.ast.declarations.Declaration;
	import org.eclipse.dltk.ast.declarations.Argument;
	import org.eclipse.dltk.ast.declarations.Decorator;
	import org.eclipse.dltk.ast.declarations.FieldDeclaration;
	import org.eclipse.dltk.ast.declarations.MethodDeclaration;
	import org.eclipse.dltk.ast.declarations.TypeDeclaration;
	import org.eclipse.dltk.ast.expressions.Expression;
	import org.eclipse.dltk.ast.statements.Block;
	import org.eclipse.dltk.ast.expressions.CallExpression;
	import org.eclipse.dltk.ast.expressions.MethodCallExpression;
	import org.eclipse.dltk.ast.expressions.Literal;
	import org.eclipse.dltk.ast.expressions.BigNumericLiteral;
	import org.eclipse.dltk.ast.expressions.BooleanLiteral;
	import org.eclipse.dltk.ast.expressions.FloatNumericLiteral;
	import org.eclipse.dltk.ast.expressions.NilLiteral;
	import org.eclipse.dltk.ast.expressions.NumericLiteral;
	import org.eclipse.dltk.ast.expressions.StringLiteral;
	import org.eclipse.dltk.ast.references.Reference;
	import org.eclipse.dltk.ast.references.SimpleReference;
	import org.eclipse.dltk.codeassist.complete.CompletionOnKeyword;
	import org.eclipse.dltk.ast.references.ConstantReference;
	import org.eclipse.dltk.ast.references.TypeReference;
	import org.eclipse.dltk.ast.references.VariableReference;
  
  	// own AST nodes import  	
	import com.blogspot.miguelinlas3.r.eclipse.language.antlr.ast.*;
	import com.blogspot.miguelinlas3.r.eclipse.language.antlr.ast.statements.*;
	
	// tokens type
	import com.blogspot.miguelinlas3.bison.ScannerToken;



/**
 * A Bison parser, automatically generated from <tt>RBisonParser.y</tt>.
 *
 * @author LALR (1) parser skeleton written by Paolo Bonzini.
 */
public class RBisonParser
{
    /** Version number for the Bison executable that generated this parser.  */
  public static final String bisonVersion = "2.4";

  /** Name of the skeleton that generated this parser.  */
  public static final String bisonSkeleton = "lalr1.java";


  /** True if verbose error messages are enabled.  */
  public boolean errorVerbose = false;


  /**
   * A class defining a pair of positions.  Positions, defined by the
   * <code>Position</code> class, denote a point in the input.
   * Locations represent a part of the input through the beginning
   * and ending positions.  */
  public class Location {
    /** The first, inclusive, position in the range.  */
    public Position begin;

    /** The first position beyond the range.  */
    public Position end;

    /**
     * Create a <code>Location</code> denoting an empty range located at
     * a given point.
     * @param loc The position at which the range is anchored.  */
    public Location (Position loc) {
      this.begin = this.end = loc;
    }

    /**
     * Create a <code>Location</code> from the endpoints of the range.
     * @param begin The first position included in the range.
     * @param end   The first position beyond the range.  */
    public Location (Position begin, Position end) {
      this.begin = begin;
      this.end = end;
    }

    /**
     * Print a representation of the location.  For this to be correct,
     * <code>Position</code> should override the <code>equals</code>
     * method.  */
	public String toString() {
		if (this.begin != null && this.end != null) {
			if (begin.equals(end))
				return begin.toString();
			else
				return begin.toString() + "-" + end.toString();
		}
		return "";
	}
  }



  /** Token returned by the scanner to signal the end of its input.  */
  public static final int EOF = 0;

/* Tokens.  */
  /** Token number, to be returned by the scanner.  */
  public static final int END_OF_INPUT = 258;
  /** Token number, to be returned by the scanner.  */
  public static final int ERROR = 259;
  /** Token number, to be returned by the scanner.  */
  public static final int STR_CONST = 260;
  /** Token number, to be returned by the scanner.  */
  public static final int NUM_CONST = 261;
  /** Token number, to be returned by the scanner.  */
  public static final int NULL_CONST = 262;
  /** Token number, to be returned by the scanner.  */
  public static final int SYMBOL = 263;
  /** Token number, to be returned by the scanner.  */
  public static final int FUNCTION = 264;
  /** Token number, to be returned by the scanner.  */
  public static final int COMMENT = 265;
  /** Token number, to be returned by the scanner.  */
  public static final int LEFT_ASSIGN = 266;
  /** Token number, to be returned by the scanner.  */
  public static final int EQ_ASSIGN = 267;
  /** Token number, to be returned by the scanner.  */
  public static final int RIGHT_ASSIGN = 268;
  /** Token number, to be returned by the scanner.  */
  public static final int LBB = 269;
  /** Token number, to be returned by the scanner.  */
  public static final int FOR = 270;
  /** Token number, to be returned by the scanner.  */
  public static final int IN = 271;
  /** Token number, to be returned by the scanner.  */
  public static final int IF = 272;
  /** Token number, to be returned by the scanner.  */
  public static final int ELSE = 273;
  /** Token number, to be returned by the scanner.  */
  public static final int WHILE = 274;
  /** Token number, to be returned by the scanner.  */
  public static final int NEXT = 275;
  /** Token number, to be returned by the scanner.  */
  public static final int BREAK = 276;
  /** Token number, to be returned by the scanner.  */
  public static final int REPEAT = 277;
  /** Token number, to be returned by the scanner.  */
  public static final int GT = 278;
  /** Token number, to be returned by the scanner.  */
  public static final int GE = 279;
  /** Token number, to be returned by the scanner.  */
  public static final int LT = 280;
  /** Token number, to be returned by the scanner.  */
  public static final int LE = 281;
  /** Token number, to be returned by the scanner.  */
  public static final int EQ = 282;
  /** Token number, to be returned by the scanner.  */
  public static final int NE = 283;
  /** Token number, to be returned by the scanner.  */
  public static final int AND = 284;
  /** Token number, to be returned by the scanner.  */
  public static final int OR = 285;
  /** Token number, to be returned by the scanner.  */
  public static final int AND2 = 286;
  /** Token number, to be returned by the scanner.  */
  public static final int OR2 = 287;
  /** Token number, to be returned by the scanner.  */
  public static final int NS_GET = 288;
  /** Token number, to be returned by the scanner.  */
  public static final int NS_GET_INT = 289;
  /** Token number, to be returned by the scanner.  */
  public static final int LIBRARY_IMPORT = 290;
  /** Token number, to be returned by the scanner.  */
  public static final int LOW = 291;
  /** Token number, to be returned by the scanner.  */
  public static final int TILDE = 292;
  /** Token number, to be returned by the scanner.  */
  public static final int NOT = 293;
  /** Token number, to be returned by the scanner.  */
  public static final int UNOT = 294;
  /** Token number, to be returned by the scanner.  */
  public static final int SPECIAL = 295;
  /** Token number, to be returned by the scanner.  */
  public static final int UPLUS = 296;
  /** Token number, to be returned by the scanner.  */
  public static final int UMINUS = 297;



  
  private Location yylloc (YYStack rhs, int n)
  {
    if (n > 0)
      return new Location (rhs.locationAt (1).begin, rhs.locationAt (n).end);
    else
      return new Location (rhs.locationAt (0).end);
  }

  /**
   * Communication interface between the scanner and the Bison-generated
   * parser <tt>RBisonParser</tt>.
   */
  public interface Lexer {
    /**
     * Method to retrieve the beginning position of the last scanned token.
     * @return the position at which the last scanned token starts.  */
    Position getStartPos ();

    /**
     * Method to retrieve the ending position of the last scanned token.
     * @return the first position beyond the last scanned token.  */
    Position getEndPos ();

    /**
     * Method to retrieve the semantic value of the last scanned token.
     * @return the semantic value of the last scanned token.  */
    org.eclipse.dltk.ast.ASTNode getLVal ();

    /**
     * Entry point for the scanner.  Returns the token identifier corresponding
     * to the next token and prepares to return the semantic value
     * and beginning/ending positions of the token. 
     * @return the token identifier corresponding to the next token. */
    int yylex () throws java.io.IOException;

    /**
     * Entry point for error reporting.  Emits an error
     * referring to the given location in a user-defined way.
     *
     * @param loc The location of the element to which the
     *                error message is related
     * @param s The string for the error message.  */
     void yyerror (Location loc, String s);
  }

  /** The object doing lexical analysis for us.  */
  private Lexer yylexer;
  
  



  /**
   * Instantiates the Bison-generated parser.
   * @param yylexer The scanner that will supply tokens to the parser.
   */
  public RBisonParser (Lexer yylexer) {
    this.yylexer = yylexer;
    
  }

  private java.io.PrintStream yyDebugStream = System.err;

  /**
   * Return the <tt>PrintStream</tt> on which the debugging output is
   * printed.
   */
  public final java.io.PrintStream getDebugStream () { return yyDebugStream; }

  /**
   * Set the <tt>PrintStream</tt> on which the debug output is printed.
   * @param s The stream that is used for debugging output.
   */
  public final void setDebugStream(java.io.PrintStream s) { yyDebugStream = s; }

  private int yydebug = 0;

  /**
   * Answer the verbosity of the debugging output; 0 means that all kinds of
   * output from the parser are suppressed.
   */
  public final int getDebugLevel() { return yydebug; }

  /**
   * Set the verbosity of the debugging output; 0 means that all kinds of
   * output from the parser are suppressed.
   * @param level The verbosity level for debugging output.
   */
  public final void setDebugLevel(int level) { yydebug = level; }

  private final int yylex () throws java.io.IOException {
    return yylexer.yylex ();
  }
  protected final void yyerror (Location loc, String s) {
    yylexer.yyerror (loc, s);
  }

  
  protected final void yyerror (String s) {
    yylexer.yyerror ((Location)null, s);
  }
  protected final void yyerror (Position loc, String s) {
    yylexer.yyerror (new Location (loc), s);
  }

  protected final void yycdebug (String s) {
    if (yydebug > 0)
      yyDebugStream.println (s);
  }

  private final class YYStack {
    private int[] stateStack = new int[16];
    private Location[] locStack = new Location[16];
    private org.eclipse.dltk.ast.ASTNode[] valueStack = new org.eclipse.dltk.ast.ASTNode[16];

    public int size = 16;
    public int height = -1;
    
    public final void push (int state, org.eclipse.dltk.ast.ASTNode value    	   	      	    , Location loc) {
      height++;
      if (size == height) 
        {
	  int[] newStateStack = new int[size * 2];
	  System.arraycopy (stateStack, 0, newStateStack, 0, height);
	  stateStack = newStateStack;
	  
	  Location[] newLocStack = new Location[size * 2];
	  System.arraycopy (locStack, 0, newLocStack, 0, height);
	  locStack = newLocStack;
	  
	  org.eclipse.dltk.ast.ASTNode[] newValueStack = new org.eclipse.dltk.ast.ASTNode[size * 2];
	  System.arraycopy (valueStack, 0, newValueStack, 0, height);
	  valueStack = newValueStack;

	  size *= 2;
	}

      stateStack[height] = state;
      locStack[height] = loc;
      valueStack[height] = value;
    }

    public final void pop () {
      height--;
    }

    public final void pop (int num) {
      // Avoid memory leaks... garbage collection is a white lie!
      if (num > 0) {
	java.util.Arrays.fill (valueStack, height - num + 1, height, null);
        java.util.Arrays.fill (locStack, height - num + 1, height, null);
      }
      height -= num;
    }

    public final int stateAt (int i) {
      return stateStack[height - i];
    }

    public final Location locationAt (int i) {
      return locStack[height - i];
    }

    public final org.eclipse.dltk.ast.ASTNode valueAt (int i) {
      return valueStack[height - i];
    }

    // Print the state stack on the debug stream.
    public void print (java.io.PrintStream out)
    {
      out.print ("Stack now");
      
      for (int i = 0; i < height; i++)
        {
	  out.print (' ');
	  out.print (stateStack[i]);
        }
      out.println ();
    }
  }

  /**
   * Returned by a Bison action in order to stop the parsing process and
   * return success (<tt>true</tt>).  */
  public static final int YYACCEPT = 0;

  /**
   * Returned by a Bison action in order to stop the parsing process and
   * return failure (<tt>false</tt>).  */
  public static final int YYABORT = 1;

  /**
   * Returned by a Bison action in order to start error recovery without
   * printing an error message.  */
  public static final int YYERROR = 2;

  /**
   * Returned by a Bison action in order to print an error message and start
   * error recovery.  */
  public static final int YYFAIL = 3;

  private static final int YYNEWSTATE = 4;
  private static final int YYDEFAULT = 5;
  private static final int YYREDUCE = 6;
  private static final int YYERRLAB1 = 7;
  private static final int YYRETURN = 8;

  private int yyerrstatus_ = 0;

  /**
   * Return whether error recovery is being done.  In this state, the parser
   * reads token until it reaches a known state, and then restarts normal
   * operation.  */
  public final boolean recovering ()
  {
    return yyerrstatus_ == 0;
  }

  private int yyaction (int yyn, YYStack yystack, int yylen) 
  {
    org.eclipse.dltk.ast.ASTNode yyval;
    Location yyloc = yylloc (yystack, yylen);

    /* If YYLEN is nonzero, implement the default value of the action:
       `$$ = $1'.  Otherwise, use the top of the stack.
    
       Otherwise, the following line sets YYVAL to garbage.
       This behavior is undocumented and Bison
       users should not rely upon it.  */
    if (yylen > 0)
      yyval = yystack.valueAt (yylen - 1);
    else
      yyval = yystack.valueAt (0);
    
    yy_reduce_print (yyn, yystack);

    switch (yyn)
      {
	  case 2:
  if (yyn == 2)
    
/* Line 353 of lalr1.java  */
/* Line 141 of "RBisonParser.y"  */
    {};
  break;
    

  case 3:
  if (yyn == 3)
    
/* Line 353 of lalr1.java  */
/* Line 143 of "RBisonParser.y"  */
    {};
  break;
    

  case 4:
  if (yyn == 4)
    
/* Line 353 of lalr1.java  */
/* Line 145 of "RBisonParser.y"  */
    {};
  break;
    

  case 5:
  if (yyn == 5)
    
/* Line 353 of lalr1.java  */
/* Line 147 of "RBisonParser.y"  */
    { root.addStatement(((yystack.valueAt (1-(1)))));	};
  break;
    

  case 6:
  if (yyn == 6)
    
/* Line 353 of lalr1.java  */
/* Line 151 of "RBisonParser.y"  */
    {
										this.root.addStatement(((yystack.valueAt (3-(3)))));
									};
  break;
    

  case 7:
  if (yyn == 7)
    
/* Line 353 of lalr1.java  */
/* Line 155 of "RBisonParser.y"  */
    {
										yyval = ((yystack.valueAt (2-(1))));
									};
  break;
    

  case 8:
  if (yyn == 8)
    
/* Line 353 of lalr1.java  */
/* Line 159 of "RBisonParser.y"  */
    {
										this.root.addStatement(((yystack.valueAt (3-(3)))));
									};
  break;
    

  case 9:
  if (yyn == 9)
    
/* Line 353 of lalr1.java  */
/* Line 163 of "RBisonParser.y"  */
    {
										yyval = ((yystack.valueAt (2-(1))));
									};
  break;
    

  case 10:
  if (yyn == 10)
    
/* Line 353 of lalr1.java  */
/* Line 167 of "RBisonParser.y"  */
    {
										System.out.println("ERROR");
									};
  break;
    

  case 11:
  if (yyn == 11)
    
/* Line 353 of lalr1.java  */
/* Line 173 of "RBisonParser.y"  */
    {yyval = ((yystack.valueAt (1-(1))));};
  break;
    

  case 12:
  if (yyn == 12)
    
/* Line 353 of lalr1.java  */
/* Line 176 of "RBisonParser.y"  */
    {yyval = ((yystack.valueAt (1-(1))));};
  break;
    

  case 13:
  if (yyn == 13)
    
/* Line 353 of lalr1.java  */
/* Line 177 of "RBisonParser.y"  */
    {yyval = ((yystack.valueAt (1-(1))));};
  break;
    

  case 14:
  if (yyn == 14)
    
/* Line 353 of lalr1.java  */
/* Line 180 of "RBisonParser.y"  */
    {yyval = new BinaryExpression(RExpressionConstansExtension.E_ASSIGN,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));};
  break;
    

  case 15:
  if (yyn == 15)
    
/* Line 353 of lalr1.java  */
/* Line 185 of "RBisonParser.y"  */
    {yyval = ((yystack.valueAt (3-(1))));};
  break;
    

  case 16:
  if (yyn == 16)
    
/* Line 353 of lalr1.java  */
/* Line 188 of "RBisonParser.y"  */
    {
							ScannerToken token = (ScannerToken)((yystack.valueAt (1-(1))));
							Double value;
							String tokenText = token.getText();
							value = tokenText != null? Double.parseDouble(tokenText):0;
							yyval = new FloatNumericLiteral(token.getOffset(),token.getOffset() + token.getLength(),value);
						};
  break;
    

  case 17:
  if (yyn == 17)
    
/* Line 353 of lalr1.java  */
/* Line 196 of "RBisonParser.y"  */
    {
							ScannerToken token = (ScannerToken)((yystack.valueAt (1-(1))));
							yyval = new StringLiteral(token.getOffset(),token.getOffset() + token.getLength(),token.getText());
						};
  break;
    

  case 18:
  if (yyn == 18)
    
/* Line 353 of lalr1.java  */
/* Line 200 of "RBisonParser.y"  */
    {
							ScannerToken token = (ScannerToken)((yystack.valueAt (1-(1))));
							yyval = new NilLiteral(token.getOffset(),token.getOffset() +token.getLength());
						};
  break;
    

  case 19:
  if (yyn == 19)
    
/* Line 353 of lalr1.java  */
/* Line 204 of "RBisonParser.y"  */
    {										
							ScannerToken token = (ScannerToken)((yystack.valueAt (1-(1))));
							yyval = new VariableReference(token.getOffset(),token.getOffset() + token.getLength() ,token.getText());
						};
  break;
    

  case 20:
  if (yyn == 20)
    
/* Line 353 of lalr1.java  */
/* Line 210 of "RBisonParser.y"  */
    {							
							RBisonParser.SKIP_NEW_LINE = false;													
							((RBlock)((yystack.valueAt (3-(2))))).setLeftBracePos(yystack.locationAt (3-(1)).begin.getFirstColumn());
							((RBlock)((yystack.valueAt (3-(2))))).setRightBracePos(yystack.locationAt (3-(3)).begin.getLastColumn());
							((yystack.valueAt (3-(2)))).setStart(((yystack.valueAt (3-(2)))).sourceStart());
							((yystack.valueAt (3-(2)))).setEnd(((yystack.valueAt (3-(2)))).sourceEnd());
							yyval = ((yystack.valueAt (3-(2))));
						};
  break;
    

  case 21:
  if (yyn == 21)
    
/* Line 353 of lalr1.java  */
/* Line 219 of "RBisonParser.y"  */
    {
							((yystack.valueAt (3-(2)))).setStart(yystack.locationAt (3-(1)).begin.getFirstColumn());
							((yystack.valueAt (3-(2)))).setEnd(yystack.locationAt (3-(3)).begin.getLastColumn());
							yyval = ((yystack.valueAt (3-(2))));
						};
  break;
    

  case 22:
  if (yyn == 22)
    
/* Line 353 of lalr1.java  */
/* Line 225 of "RBisonParser.y"  */
    {								
								yyval = new UnaryExpression(RExpressionConstansExtension.E_MINUS,yystack.locationAt (2-(1)).begin.getFirstColumn(),((yystack.valueAt (2-(2)))));
							};
  break;
    

  case 23:
  if (yyn == 23)
    
/* Line 353 of lalr1.java  */
/* Line 229 of "RBisonParser.y"  */
    {								
								yyval = new UnaryExpression(RExpressionConstansExtension.E_PLUS,yystack.locationAt (2-(1)).begin.getFirstColumn(),((yystack.valueAt (2-(2)))));
							};
  break;
    

  case 24:
  if (yyn == 24)
    
/* Line 353 of lalr1.java  */
/* Line 233 of "RBisonParser.y"  */
    {								
								yyval = new UnaryExpression(RExpressionConstansExtension.E_LNOT,yystack.locationAt (2-(1)).begin.getFirstColumn(),((yystack.valueAt (2-(2)))));
							};
  break;
    

  case 25:
  if (yyn == 25)
    
/* Line 353 of lalr1.java  */
/* Line 237 of "RBisonParser.y"  */
    {								
								yyval = new UnaryExpression(RExpressionConstansExtension.E_TILDE,yystack.locationAt (2-(1)).begin.getFirstColumn(),((yystack.valueAt (2-(2)))));							
							};
  break;
    

  case 26:
  if (yyn == 26)
    
/* Line 353 of lalr1.java  */
/* Line 241 of "RBisonParser.y"  */
    {
								ScannerToken token = (ScannerToken)((yystack.valueAt (2-(2))));
								yyval = new UnaryExpression(RExpressionConstansExtension.E_QUESTION,yystack.locationAt (2-(1)).begin.getFirstColumn(),((yystack.valueAt (2-(2)))));
							};
  break;
    

  case 27:
  if (yyn == 27)
    
/* Line 353 of lalr1.java  */
/* Line 247 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_SEMI_COLON,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 28:
  if (yyn == 28)
    
/* Line 353 of lalr1.java  */
/* Line 251 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_PLUS,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 29:
  if (yyn == 29)
    
/* Line 353 of lalr1.java  */
/* Line 255 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_MINUS,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 30:
  if (yyn == 30)
    
/* Line 353 of lalr1.java  */
/* Line 259 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_MULT,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 31:
  if (yyn == 31)
    
/* Line 353 of lalr1.java  */
/* Line 263 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_DIV,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 32:
  if (yyn == 32)
    
/* Line 353 of lalr1.java  */
/* Line 267 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_POWER,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 33:
  if (yyn == 33)
    
/* Line 353 of lalr1.java  */
/* Line 273 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_MOD,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 34:
  if (yyn == 34)
    
/* Line 353 of lalr1.java  */
/* Line 277 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_TILDE,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 35:
  if (yyn == 35)
    
/* Line 353 of lalr1.java  */
/* Line 281 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_QUESTION,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 36:
  if (yyn == 36)
    
/* Line 353 of lalr1.java  */
/* Line 285 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_LT,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 37:
  if (yyn == 37)
    
/* Line 353 of lalr1.java  */
/* Line 289 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_LE,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 38:
  if (yyn == 38)
    
/* Line 353 of lalr1.java  */
/* Line 293 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_EQUAL,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 39:
  if (yyn == 39)
    
/* Line 353 of lalr1.java  */
/* Line 297 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_NOT_EQUAL,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 40:
  if (yyn == 40)
    
/* Line 353 of lalr1.java  */
/* Line 301 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_GE,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 41:
  if (yyn == 41)
    
/* Line 353 of lalr1.java  */
/* Line 305 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_GT,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 42:
  if (yyn == 42)
    
/* Line 353 of lalr1.java  */
/* Line 309 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_AND_SINGLE,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 43:
  if (yyn == 43)
    
/* Line 353 of lalr1.java  */
/* Line 313 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_OR_SINGLE,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 44:
  if (yyn == 44)
    
/* Line 353 of lalr1.java  */
/* Line 317 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_LAND,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 45:
  if (yyn == 45)
    
/* Line 353 of lalr1.java  */
/* Line 321 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_LOR,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 46:
  if (yyn == 46)
    
/* Line 353 of lalr1.java  */
/* Line 325 of "RBisonParser.y"  */
    {
							// meter un nodo AST para asignaciones o dejar este mismo??
							yyval = new BinaryExpression(RExpressionConstansExtension.E_LEFT_ASSIGN,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 47:
  if (yyn == 47)
    
/* Line 353 of lalr1.java  */
/* Line 330 of "RBisonParser.y"  */
    { 
							yyval = new BinaryExpression(RExpressionConstansExtension.E_RIGHT_ASSIGN,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),((yystack.valueAt (3-(3)))));
						};
  break;
    

  case 48:
  if (yyn == 48)
    
/* Line 353 of lalr1.java  */
/* Line 334 of "RBisonParser.y"  */
    {
							
							RMethodDeclaration method = new RMethodDeclaration("", yystack.locationAt (6-(1)).begin.getFirstColumn(), yystack.locationAt (6-(1)).begin.getLastColumn(),((yystack.valueAt (6-(6)))).sourceStart(),((yystack.valueAt (6-(6)))).sourceEnd());
							RArgumentList argumentList = (RArgumentList)((yystack.valueAt (6-(3))));
							List args = argumentList.getChilds();
							for(int i=0;i<args.size();++i){
								method.addArgument((RArgument)(args.get(i)));
							}
							method.getStatements().add(((yystack.valueAt (6-(6)))));							
							method.setArgumentsList(argumentList);
							method.setLeftParentPos(yystack.locationAt (6-(2)).begin.getFirstColumn());
							method.setRightParentPos(yystack.locationAt (6-(4)).begin.getFirstColumn());
							yyval = method;							
						};
  break;
    

  case 49:
  if (yyn == 49)
    
/* Line 353 of lalr1.java  */
/* Line 350 of "RBisonParser.y"  */
    {				
							RArgumentList args = new RArgumentList();
							ScannerToken token = (ScannerToken)((yystack.valueAt (4-(3))));
							StringLiteral literal = new StringLiteral(token.getOffset(),token.getOffset() +token.getLength(),token.getText());							
							literal.setStart(yystack.locationAt (4-(3)).begin.getFirstColumn());
							literal.setEnd(yystack.locationAt (4-(3)).begin.getLastColumn());
							args.addNode(literal);
							LoadLibraryExpression call = new LoadLibraryExpression(yystack.locationAt (4-(1)).begin.getFirstColumn(),yystack.locationAt (4-(4)).begin.getLastColumn(), args);							
							call.setLeftParentPos(yystack.locationAt (4-(2)).begin.getFirstColumn());
							call.setRightParentPos(yystack.locationAt (4-(4)).begin.getFirstColumn());							
							yyval = call;
							
						};
  break;
    

  case 50:
  if (yyn == 50)
    
/* Line 353 of lalr1.java  */
/* Line 365 of "RBisonParser.y"  */
    {
							// inicialmente el nombre de la funcion estara vacio
							String funcName = "";
							if(((yystack.valueAt (4-(1)))) instanceof VariableReference){
								funcName = ((VariableReference)((yystack.valueAt (4-(1))))).getName();
							}							
							RMethodCallExpression call = new RMethodCallExpression(((yystack.valueAt (4-(1)))).sourceStart(),yystack.locationAt (4-(4)).begin.getLastColumn(), null,funcName, (RArgumentList)((yystack.valueAt (4-(3)))));							
							call.setLeftParentPos(yystack.locationAt (4-(2)).begin.getFirstColumn());
							call.setRightParentPos(yystack.locationAt (4-(4)).begin.getFirstColumn());
							yyval = call;
						};
  break;
    

  case 51:
  if (yyn == 51)
    
/* Line 353 of lalr1.java  */
/* Line 377 of "RBisonParser.y"  */
    {
							yyval = new IfStatement((ParenthesisCondition)((yystack.valueAt (3-(2)))), (Statement)((yystack.valueAt (3-(3)))),null);
							yyval.setStart(yystack.locationAt (3-(1)).begin.getFirstColumn());
							yyval.setEnd(yystack.locationAt (3-(3)).begin.getLastColumn());
						};
  break;
    

  case 52:
  if (yyn == 52)
    
/* Line 353 of lalr1.java  */
/* Line 383 of "RBisonParser.y"  */
    {
							IfStatement ifStatement = new IfStatement((ParenthesisCondition)((yystack.valueAt (5-(2)))), (Statement)((yystack.valueAt (5-(3)))),(Statement)((yystack.valueAt (5-(5)))));
							ifStatement.setStart(yystack.locationAt (5-(1)).begin.getFirstColumn());
							ifStatement.setEnd(yystack.locationAt (5-(3)).begin.getLastColumn());
							ifStatement.setElseKeywordStartPosition(yystack.locationAt (5-(4)).begin.getFirstColumn());
							yyval = ifStatement;
						};
  break;
    

  case 53:
  if (yyn == 53)
    
/* Line 353 of lalr1.java  */
/* Line 391 of "RBisonParser.y"  */
    {
							yyval = new ForStatement((ForCondition)((yystack.valueAt (3-(2)))),(Statement)((yystack.valueAt (3-(3)))));
							yyval.setStart(yystack.locationAt (3-(1)).begin.getFirstColumn());
							yyval.setEnd(yystack.locationAt (3-(3)).begin.getLastColumn());
						};
  break;
    

  case 54:
  if (yyn == 54)
    
/* Line 353 of lalr1.java  */
/* Line 397 of "RBisonParser.y"  */
    {
							WhileStatement whileStatement = new WhileStatement((ParenthesisCondition)((yystack.valueAt (3-(2)))), (Statement)((yystack.valueAt (3-(3)))));
							whileStatement.setStart(yystack.locationAt (3-(1)).begin.getFirstColumn());
							whileStatement.setEnd(yystack.locationAt (3-(3)).begin.getLastColumn());
							yyval = whileStatement;
						};
  break;
    

  case 55:
  if (yyn == 55)
    
/* Line 353 of lalr1.java  */
/* Line 404 of "RBisonParser.y"  */
    {
							yyval = new RepeatStatement((Statement)((yystack.valueAt (2-(2)))));
						};
  break;
    

  case 56:
  if (yyn == 56)
    
/* Line 353 of lalr1.java  */
/* Line 409 of "RBisonParser.y"  */
    {
							yyval = new BinaryExpression(RExpressionConstansExtension.E_SCRIPT_DOUBLE_OPERATOR,yystack.locationAt (5-(2)).begin.getFirstColumn(),((yystack.valueAt (5-(1)))),((yystack.valueAt (5-(3)))));
						};
  break;
    

  case 57:
  if (yyn == 57)
    
/* Line 353 of lalr1.java  */
/* Line 414 of "RBisonParser.y"  */
    {
							// $$ = new BinaryExpression(RExpressionConstansExtension.E_SCRIPT_SINGLE_OPERATOR,@2.begin.getFirstColumn(),$1,$3);
							yyval = new IndexAccessBinaryExpression(RExpressionConstansExtension.E_SCRIPT_SINGLE_OPERATOR,((yystack.valueAt (4-(1)))),((yystack.valueAt (4-(3)))),yystack.locationAt (4-(2)).begin.getFirstColumn(),yystack.locationAt (4-(4)).begin.getFirstColumn());
							yyval.setStart(((yystack.valueAt (4-(1)))).sourceStart());
							yyval.setEnd(yystack.locationAt (4-(4)).begin.getFirstColumn());
						};
  break;
    

  case 58:
  if (yyn == 58)
    
/* Line 353 of lalr1.java  */
/* Line 422 of "RBisonParser.y"  */
    {
							ScannerToken token1 = (ScannerToken)((yystack.valueAt (3-(1))));
							VariableReference reference1 = new VariableReference(token1.getOffset(),token1.getOffset() + token1.getLength() ,token1.getText());
							
							ScannerToken token2 = (ScannerToken)((yystack.valueAt (3-(3))));
							VariableReference reference2 = new VariableReference(token2.getOffset(),token2.getOffset() + token2.getLength() ,token2.getText());
							yyval = new BinaryExpression(RExpressionConstansExtension.E_PACKAGE_OPERATOR,yystack.locationAt (3-(2)).begin.getFirstColumn(),reference1,reference2);
						};
  break;
    

  case 59:
  if (yyn == 59)
    
/* Line 353 of lalr1.java  */
/* Line 431 of "RBisonParser.y"  */
    {
							ScannerToken token1 = (ScannerToken)((yystack.valueAt (3-(1))));
							VariableReference reference1 = new VariableReference(token1.getOffset(),token1.getOffset() + token1.getLength() ,token1.getText());
							
							ScannerToken token2 = (ScannerToken)((yystack.valueAt (3-(3))));
							StringLiteral reference2 = new StringLiteral(token2.getOffset(),token2.getOffset() + token2.getLength() ,token2.getText());
													
							yyval = new BinaryExpression(RExpressionConstansExtension.E_PACKAGE_OPERATOR,yystack.locationAt (3-(2)).begin.getFirstColumn(),reference1,reference2);
						};
  break;
    

  case 60:
  if (yyn == 60)
    
/* Line 353 of lalr1.java  */
/* Line 441 of "RBisonParser.y"  */
    {
							ScannerToken token1 = (ScannerToken)((yystack.valueAt (3-(3))));
							VariableReference reference1 = new VariableReference(token1.getOffset(),token1.getOffset() + token1.getLength() ,token1.getText());
							
							ScannerToken token2 = (ScannerToken)((yystack.valueAt (3-(1))));
							StringLiteral reference2 = new StringLiteral(token2.getOffset(),token2.getOffset() + token2.getLength() ,token2.getText());
						
							yyval = new BinaryExpression(RExpressionConstansExtension.E_PACKAGE_OPERATOR,yystack.locationAt (3-(2)).begin.getFirstColumn(),reference2,reference1);
						};
  break;
    

  case 61:
  if (yyn == 61)
    
/* Line 353 of lalr1.java  */
/* Line 451 of "RBisonParser.y"  */
    {
							ScannerToken token1 = (ScannerToken)((yystack.valueAt (3-(1))));
							StringLiteral reference1 = new StringLiteral(token1.getOffset(),token1.getOffset() + token1.getLength() ,token1.getText());
							
							ScannerToken token2 = (ScannerToken)((yystack.valueAt (3-(3))));
							StringLiteral reference2 = new StringLiteral(token2.getOffset(),token2.getOffset() + token2.getLength() ,token2.getText());
						
							yyval = new BinaryExpression(RExpressionConstansExtension.E_PACKAGE_OPERATOR,yystack.locationAt (3-(2)).begin.getFirstColumn(),reference1,reference2);
						};
  break;
    

  case 62:
  if (yyn == 62)
    
/* Line 353 of lalr1.java  */
/* Line 461 of "RBisonParser.y"  */
    {
							ScannerToken token1 = (ScannerToken)((yystack.valueAt (3-(1))));
							VariableReference reference1 = new VariableReference(token1.getOffset(),token1.getOffset() + token1.getLength() ,token1.getText());
							
							ScannerToken token2 = (ScannerToken)((yystack.valueAt (3-(3))));
							VariableReference reference2 = new VariableReference(token2.getOffset(),token2.getOffset() + token2.getLength() ,token2.getText());
						
							yyval = new BinaryExpression(RExpressionConstansExtension.E_PACKAGE_OPERATOR_HIDDEN,yystack.locationAt (3-(2)).begin.getFirstColumn(),reference1,reference2);
						};
  break;
    

  case 63:
  if (yyn == 63)
    
/* Line 353 of lalr1.java  */
/* Line 471 of "RBisonParser.y"  */
    {
							ScannerToken token1 = (ScannerToken)((yystack.valueAt (3-(1))));
							VariableReference reference1 = new VariableReference(token1.getOffset(),token1.getOffset() + token1.getLength() ,token1.getText());
							
							ScannerToken token2 = (ScannerToken)((yystack.valueAt (3-(3))));
							StringLiteral reference2 = new StringLiteral(token2.getOffset(),token2.getOffset() + token2.getLength() ,token2.getText());
													
							yyval = new BinaryExpression(RExpressionConstansExtension.E_PACKAGE_OPERATOR_HIDDEN,yystack.locationAt (3-(2)).begin.getFirstColumn(),reference1,reference2);
						};
  break;
    

  case 64:
  if (yyn == 64)
    
/* Line 353 of lalr1.java  */
/* Line 481 of "RBisonParser.y"  */
    {
							ScannerToken token1 = (ScannerToken)((yystack.valueAt (3-(3))));
							VariableReference reference1 = new VariableReference(token1.getOffset(),token1.getOffset() + token1.getLength() ,token1.getText());
							
							ScannerToken token2 = (ScannerToken)((yystack.valueAt (3-(1))));
							StringLiteral reference2 = new StringLiteral(token2.getOffset(),token2.getOffset() + token2.getLength() ,token2.getText());
							yyval = new BinaryExpression(RExpressionConstansExtension.E_PACKAGE_OPERATOR_HIDDEN,yystack.locationAt (3-(2)).begin.getFirstColumn(),reference2,reference1);
						};
  break;
    

  case 65:
  if (yyn == 65)
    
/* Line 353 of lalr1.java  */
/* Line 490 of "RBisonParser.y"  */
    {
							ScannerToken token1 = (ScannerToken)((yystack.valueAt (3-(1))));
							StringLiteral reference1 = new StringLiteral(token1.getOffset(),token1.getOffset() + token1.getLength() ,token1.getText());
							
							ScannerToken token2 = (ScannerToken)((yystack.valueAt (3-(3))));
							StringLiteral reference2 = new StringLiteral(token2.getOffset(),token2.getOffset() + token2.getLength() ,token2.getText());						
							yyval = new BinaryExpression(RExpressionConstansExtension.E_PACKAGE_OPERATOR_HIDDEN,yystack.locationAt (3-(2)).begin.getFirstColumn(),reference1,reference2);
						};
  break;
    

  case 66:
  if (yyn == 66)
    
/* Line 353 of lalr1.java  */
/* Line 499 of "RBisonParser.y"  */
    {
						    ScannerToken token = (ScannerToken)((yystack.valueAt (3-(3))));
							VariableReference reference = new VariableReference(token.getOffset(),token.getOffset() + token.getLength() ,token.getText());
							yyval = new BinaryExpression(RExpressionConstansExtension.E_DOLLAR,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),reference);
						};
  break;
    

  case 67:
  if (yyn == 67)
    
/* Line 353 of lalr1.java  */
/* Line 505 of "RBisonParser.y"  */
    {
							ScannerToken token = (ScannerToken)((yystack.valueAt (3-(3))));
							StringLiteral literal = new StringLiteral(token.getOffset(),token.getOffset() +token.getLength(),token.getText());
							yyval = new BinaryExpression(RExpressionConstansExtension.E_DOLLAR,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),literal);
						};
  break;
    

  case 68:
  if (yyn == 68)
    
/* Line 353 of lalr1.java  */
/* Line 511 of "RBisonParser.y"  */
    {
							ScannerToken token = (ScannerToken)((yystack.valueAt (3-(3))));
							VariableReference reference = new VariableReference(token.getOffset(),token.getOffset() + token.getLength() ,token.getText());						
							yyval = new BinaryExpression(RExpressionConstansExtension.E_AT,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),reference);
						};
  break;
    

  case 69:
  if (yyn == 69)
    
/* Line 353 of lalr1.java  */
/* Line 517 of "RBisonParser.y"  */
    {
							ScannerToken token = (ScannerToken)((yystack.valueAt (3-(3))));
							StringLiteral literal = new StringLiteral(token.getOffset(),token.getOffset() +token.getLength(),token.getText());
							yyval = new BinaryExpression(RExpressionConstansExtension.E_AT,yystack.locationAt (3-(2)).begin.getFirstColumn(),((yystack.valueAt (3-(1)))),literal);
						};
  break;
    

  case 70:
  if (yyn == 70)
    
/* Line 353 of lalr1.java  */
/* Line 523 of "RBisonParser.y"  */
    {
							ScannerToken token = (ScannerToken)((yystack.valueAt (1-(1))));
							yyval = new ContinueStatement(token.getOffset(),token.getOffset() +token.getLength());
						};
  break;
    

  case 71:
  if (yyn == 71)
    
/* Line 353 of lalr1.java  */
/* Line 527 of "RBisonParser.y"  */
    {
							ScannerToken token = (ScannerToken)((yystack.valueAt (1-(1))));
							yyval = new BreakStatement(token.getOffset(),token.getOffset() +token.getLength());
						};
  break;
    

  case 72:
  if (yyn == 72)
    
/* Line 353 of lalr1.java  */
/* Line 532 of "RBisonParser.y"  */
    {
							ScannerToken token = (ScannerToken)((yystack.valueAt (1-(1))));
							yyval = new SingleLineCommentNode(token.getText());
							yyval.setStart(token.getOffset());
							yyval.setEnd(token.getOffset() +token.getLength());
						};
  break;
    

  case 73:
  if (yyn == 73)
    
/* Line 353 of lalr1.java  */
/* Line 539 of "RBisonParser.y"  */
    {
							yyval = ((yystack.valueAt (2-(1))));
						};
  break;
    

  case 74:
  if (yyn == 74)
    
/* Line 353 of lalr1.java  */
/* Line 545 of "RBisonParser.y"  */
    {
							RBisonParser.SKIP_NEW_LINE = true;
							ParenthesisCondition cond = new ParenthesisCondition((Statement)((yystack.valueAt (3-(2)))));
							cond.setLeftParenthesisPos(yystack.locationAt (3-(1)).begin.getFirstColumn());
							cond.setRightParenthesisPos(yystack.locationAt (3-(3)).begin.getLastColumn()); 
							yyval = cond;
						};
  break;
    

  case 75:
  if (yyn == 75)
    
/* Line 353 of lalr1.java  */
/* Line 555 of "RBisonParser.y"  */
    {
							RBisonParser.SKIP_NEW_LINE = true;
							ParenthesisCondition cond = new ParenthesisCondition((Statement)((yystack.valueAt (3-(2)))));
							cond.setLeftParenthesisPos(yystack.locationAt (3-(1)).begin.getFirstColumn());
							cond.setRightParenthesisPos(yystack.locationAt (3-(3)).begin.getLastColumn()); 
							yyval = cond;
						};
  break;
    

  case 76:
  if (yyn == 76)
    
/* Line 353 of lalr1.java  */
/* Line 565 of "RBisonParser.y"  */
    {
							RBisonParser.SKIP_NEW_LINE = true;
							ScannerToken token = (ScannerToken)((yystack.valueAt (5-(2))));
							VariableReference var = new VariableReference(token.getOffset(),token.getOffset() +token.getLength() ,token.getText());
							ForCondition forCond= new ForCondition(var,(Statement)((yystack.valueAt (5-(4)))));
							forCond.setLeftParenthesisPos(yystack.locationAt (5-(1)).begin.getFirstColumn());
							forCond.setRightParenthesisPos(yystack.locationAt (5-(5)).begin.getLastColumn());
							yyval = forCond;
						};
  break;
    

  case 77:
  if (yyn == 77)
    
/* Line 353 of lalr1.java  */
/* Line 577 of "RBisonParser.y"  */
    {										
										yyval = new RBlock((yyloc).begin.getFirstColumn(),(yyloc).begin.getLastColumn());
									};
  break;
    

  case 78:
  if (yyn == 78)
    
/* Line 353 of lalr1.java  */
/* Line 580 of "RBisonParser.y"  */
    { 										
										RBlock block = new RBlock(yyval.sourceStart(),yyval.sourceEnd());
										block.addStatement(yyval);
										yyval = block;
									};
  break;
    

  case 79:
  if (yyn == 79)
    
/* Line 353 of lalr1.java  */
/* Line 585 of "RBisonParser.y"  */
    {										
										((RBlock)yyval).addStatement(((yystack.valueAt (3-(3)))));
										yyval.setEnd(((yystack.valueAt (3-(3)))).sourceEnd());
									};
  break;
    

  case 80:
  if (yyn == 80)
    
/* Line 353 of lalr1.java  */
/* Line 589 of "RBisonParser.y"  */
    {										
										yyval = ((yystack.valueAt (2-(1))));
									};
  break;
    

  case 81:
  if (yyn == 81)
    
/* Line 353 of lalr1.java  */
/* Line 593 of "RBisonParser.y"  */
    {										
										((RBlock)yyval).addStatement(((yystack.valueAt (3-(3)))));
										yyval.setEnd(((yystack.valueAt (3-(3)))).sourceEnd());
									};
  break;
    

  case 82:
  if (yyn == 82)
    
/* Line 353 of lalr1.java  */
/* Line 598 of "RBisonParser.y"  */
    {									
										yyval = ((yystack.valueAt (2-(1))));
									};
  break;
    

  case 83:
  if (yyn == 83)
    
/* Line 353 of lalr1.java  */
/* Line 604 of "RBisonParser.y"  */
    {
										RArgumentList argumentList = new RArgumentList();
										if(((yystack.valueAt (1-(1)))) != null){
											argumentList.setStart(((yystack.valueAt (1-(1)))).sourceStart());
											argumentList.setEnd(((yystack.valueAt (1-(1)))).sourceEnd());
											argumentList.addNode(((yystack.valueAt (1-(1)))));
										}else{
											argumentList.setStart((yyloc).begin.getFirstColumn());
											argumentList.setEnd((yyloc).begin.getLastColumn());											
										}
										yyval = argumentList;									
										
									};
  break;
    

  case 84:
  if (yyn == 84)
    
/* Line 353 of lalr1.java  */
/* Line 618 of "RBisonParser.y"  */
    {
										if(((yystack.valueAt (4-(4)))) != null){
											yyval.setEnd(((yystack.valueAt (4-(4)))).sourceEnd());
											((RArgumentList)yyval).addNode(((yystack.valueAt (4-(4)))));
										}
										((RArgumentList)yyval).addNewCommaPosition(yystack.locationAt (4-(3)).begin.getFirstColumn());
									};
  break;
    

  case 85:
  if (yyn == 85)
    
/* Line 353 of lalr1.java  */
/* Line 628 of "RBisonParser.y"  */
    {
										yyval = null;
									};
  break;
    

  case 86:
  if (yyn == 86)
    
/* Line 353 of lalr1.java  */
/* Line 632 of "RBisonParser.y"  */
    {										
										yyval = ((yystack.valueAt (1-(1)))); 
									};
  break;
    

  case 87:
  if (yyn == 87)
    
/* Line 353 of lalr1.java  */
/* Line 636 of "RBisonParser.y"  */
    {
										ScannerToken token = (ScannerToken)((yystack.valueAt (2-(1))));
										yyval = new BinaryExpression(RExpressionConstansExtension.E_ASSIGN,yystack.locationAt (2-(2)).begin.getFirstColumn(),new VariableReference(token.getOffset(),token.getOffset() +token.getLength() ,token.getText()),null);		
									};
  break;
    

  case 88:
  if (yyn == 88)
    
/* Line 353 of lalr1.java  */
/* Line 641 of "RBisonParser.y"  */
    {
										ScannerToken token = (ScannerToken)((yystack.valueAt (3-(1))));
										yyval = new BinaryExpression(RExpressionConstansExtension.E_ASSIGN,yystack.locationAt (3-(2)).begin.getFirstColumn(),new VariableReference(token.getOffset(),token.getOffset() +token.getLength() ,token.getText()),((yystack.valueAt (3-(3)))));												 
									};
  break;
    

  case 89:
  if (yyn == 89)
    
/* Line 353 of lalr1.java  */
/* Line 646 of "RBisonParser.y"  */
    {
										ScannerToken token = (ScannerToken)((yystack.valueAt (2-(1))));
										StringLiteral literal = new StringLiteral(token.getOffset(),token.getOffset() +token.getLength(),token.getText());
										yyval = new BinaryExpression(RExpressionConstansExtension.E_ASSIGN,literal,null);		
										
									};
  break;
    

  case 90:
  if (yyn == 90)
    
/* Line 353 of lalr1.java  */
/* Line 653 of "RBisonParser.y"  */
    {
										ScannerToken token = (ScannerToken)((yystack.valueAt (3-(1))));
										StringLiteral literal = new StringLiteral(token.getOffset(),token.getOffset() +token.getLength(),token.getText());
										yyval = new BinaryExpression(RExpressionConstansExtension.E_ASSIGN,yystack.locationAt (3-(2)).begin.getFirstColumn(),literal,((yystack.valueAt (3-(3)))));		
									
									};
  break;
    

  case 91:
  if (yyn == 91)
    
/* Line 353 of lalr1.java  */
/* Line 660 of "RBisonParser.y"  */
    {
											ScannerToken token = (ScannerToken)((yystack.valueAt (2-(1))));
											NilLiteral literal = new NilLiteral(token.getOffset(),token.getOffset() +token.getLength());
											yyval = new BinaryExpression(RExpressionConstansExtension.E_ASSIGN,yystack.locationAt (2-(2)).begin.getFirstColumn(),literal,null);
									};
  break;
    

  case 92:
  if (yyn == 92)
    
/* Line 353 of lalr1.java  */
/* Line 666 of "RBisonParser.y"  */
    {
											ScannerToken token = (ScannerToken)((yystack.valueAt (3-(1))));
											NilLiteral literal = new NilLiteral(token.getOffset(),token.getOffset() +token.getLength());
											yyval = new BinaryExpression(RExpressionConstansExtension.E_ASSIGN,yystack.locationAt (3-(2)).begin.getFirstColumn(),literal,((yystack.valueAt (3-(3)))));
									};
  break;
    

  case 93:
  if (yyn == 93)
    
/* Line 353 of lalr1.java  */
/* Line 674 of "RBisonParser.y"  */
    {													
												yyval = new RArgumentList((yyloc).begin.getFirstColumn(),(yyloc).begin.getLastColumn());												
											};
  break;
    

  case 94:
  if (yyn == 94)
    
/* Line 353 of lalr1.java  */
/* Line 678 of "RBisonParser.y"  */
    { 											
												yyval = new RArgumentList(yystack.locationAt (1-(1)).begin.getFirstColumn(),yystack.locationAt (1-(1)).begin.getLastColumn());
												ScannerToken token = (ScannerToken)((yystack.valueAt (1-(1))));
												SimpleReference var = new VariableReference(token.getOffset(),token.getOffset() +token.getLength() ,token.getText());
												ASTNode arg = new RArgument(var,0);
												((RArgumentList)yyval).addNode(arg);
											};
  break;
    

  case 95:
  if (yyn == 95)
    
/* Line 353 of lalr1.java  */
/* Line 686 of "RBisonParser.y"  */
    {												
												yyval = new RArgumentList(yystack.locationAt (3-(1)).begin.getFirstColumn(),yystack.locationAt (3-(2)).begin.getLastColumn());
												ScannerToken token = (ScannerToken)((yystack.valueAt (3-(1))));
												SimpleReference var = new VariableReference(token.getOffset(),token.getOffset() +token.getLength() ,token.getText());												
												ASTNode arg = new RArgument(var,0,((yystack.valueAt (3-(3)))));
												((RArgumentList)yyval).addNode(arg);												
											};
  break;
    

  case 96:
  if (yyn == 96)
    
/* Line 353 of lalr1.java  */
/* Line 694 of "RBisonParser.y"  */
    {
												ScannerToken token = (ScannerToken)((yystack.valueAt (3-(3))));
												SimpleReference var = new VariableReference(token.getOffset(),token.getOffset() +token.getLength() ,token.getText());
												((RArgumentList)yyval).addNode(new RArgument(var,0));
												((RArgumentList)yyval).addNewCommaPosition(yystack.locationAt (3-(2)).begin.getFirstColumn());
												yyval.setEnd(yystack.locationAt (3-(3)).begin.getLastColumn());
											};
  break;
    

  case 97:
  if (yyn == 97)
    
/* Line 353 of lalr1.java  */
/* Line 702 of "RBisonParser.y"  */
    { 
												ScannerToken token = (ScannerToken)((yystack.valueAt (5-(3))));
												SimpleReference var = new VariableReference(token.getOffset(),token.getOffset() +token.getLength() ,token.getText());	
												((RArgumentList)yyval).addNode(new RArgument(var,0,((yystack.valueAt (5-(5))))));
												((RArgumentList)yyval).addNewCommaPosition(yystack.locationAt (5-(2)).begin.getFirstColumn());
												yyval.setEnd(yystack.locationAt (5-(5)).begin.getLastColumn());
											};
  break;
    

  case 98:
  if (yyn == 98)
    
/* Line 353 of lalr1.java  */
/* Line 713 of "RBisonParser.y"  */
    {
			RBisonParser.SKIP_NEW_LINE = true;
			};
  break;
    



/* Line 353 of lalr1.java  */
/* Line 1706 of "RBisonParser.java"  */
	default: break;
      }

    yy_symbol_print ("-> $$ =", yyr1_[yyn], yyval, yyloc);

    yystack.pop (yylen);
    yylen = 0;

    /* Shift the result of the reduction.  */
    yyn = yyr1_[yyn];
    int yystate = yypgoto_[yyn - yyntokens_] + yystack.stateAt (0);
    if (0 <= yystate && yystate <= yylast_
	&& yycheck_[yystate] == yystack.stateAt (0))
      yystate = yytable_[yystate];
    else
      yystate = yydefgoto_[yyn - yyntokens_];

    yystack.push (yystate, yyval, yyloc);
    return YYNEWSTATE;
  }

  /* Return YYSTR after stripping away unnecessary quotes and
     backslashes, so that it's suitable for yyerror.  The heuristic is
     that double-quoting is unnecessary unless the string contains an
     apostrophe, a comma, or backslash (other than backslash-backslash).
     YYSTR is taken from yytname.  */
  private final String yytnamerr_ (String yystr)
  {
    if (yystr.charAt (0) == '"')
      {
        StringBuffer yyr = new StringBuffer ();
        strip_quotes: for (int i = 1; i < yystr.length (); i++)
          switch (yystr.charAt (i))
            {
            case '\'':
            case ',':
              break strip_quotes;

            case '\\':
	      if (yystr.charAt(++i) != '\\')
                break strip_quotes;
              /* Fall through.  */
            default:
              yyr.append (yystr.charAt (i));
              break;

            case '"':
              return yyr.toString ();
            }
      }
    else if (yystr.equals ("$end"))
      return "end of input";

    return yystr;
  }

  /*--------------------------------.
  | Print this symbol on YYOUTPUT.  |
  `--------------------------------*/

  private void yy_symbol_print (String s, int yytype,
			         org.eclipse.dltk.ast.ASTNode yyvaluep				 , Object yylocationp)
  {
    if (yydebug > 0)
    yycdebug (s + (yytype < yyntokens_ ? " token " : " nterm ")
	      + yytname_[yytype] + " ("
	      + yylocationp + ": "
	      + (yyvaluep == null ? "(null)" : yyvaluep.toString ()) + ")");
  }

  /**
   * Parse input from the scanner that was specified at object construction
   * time.  Return whether the end of the input was reached successfully.
   *
   * @return <tt>true</tt> if the parsing succeeds.  Note that this does not
   *          imply that there were no syntax errors.
   */
  public boolean parse () throws java.io.IOException
  {
    /// Lookahead and lookahead in internal form.
    int yychar = yyempty_;
    int yytoken = 0;

    /* State.  */
    int yyn = 0;
    int yylen = 0;
    int yystate = 0;

    YYStack yystack = new YYStack ();

    /* Error handling.  */
    int yynerrs_ = 0;
    /// The location where the error started.
    Location yyerrloc = null;

    /// Location of the lookahead.
    Location yylloc = new Location (null, null);

    /// @$.
    Location yyloc;

    /// Semantic value of the lookahead.
    org.eclipse.dltk.ast.ASTNode yylval = null;

    int yyresult;

    yycdebug ("Starting parse\n");
    yyerrstatus_ = 0;


    /* Initialize the stack.  */
    yystack.push (yystate, yylval, yylloc);

    int label = YYNEWSTATE;
    for (;;)
      switch (label)
      {
        /* New state.  Unlike in the C/C++ skeletons, the state is already
	   pushed when we come here.  */
      case YYNEWSTATE:
        yycdebug ("Entering state " + yystate + "\n");
        if (yydebug > 0)
          yystack.print (yyDebugStream);
    
        /* Accept?  */
        if (yystate == yyfinal_)
          return true;
    
        /* Take a decision.  First try without lookahead.  */
        yyn = yypact_[yystate];
        if (yyn == yypact_ninf_)
          {
            label = YYDEFAULT;
	    break;
          }
    
        /* Read a lookahead token.  */
        if (yychar == yyempty_)
          {
	    yycdebug ("Reading a token: ");
	    yychar = yylex ();
            
	    yylloc = new Location(yylexer.getStartPos (),
	    		   	            yylexer.getEndPos ());
            yylval = yylexer.getLVal ();
          }
    
        /* Convert token to internal form.  */
        if (yychar <= EOF)
          {
	    yychar = yytoken = EOF;
	    yycdebug ("Now at end of input.\n");
          }
        else
          {
	    yytoken = yytranslate_ (yychar);
	    yy_symbol_print ("Next token is", yytoken,
	    		     yylval, yylloc);
          }
    
        /* If the proper action on seeing token YYTOKEN is to reduce or to
           detect an error, take that action.  */
        yyn += yytoken;
        if (yyn < 0 || yylast_ < yyn || yycheck_[yyn] != yytoken)
          label = YYDEFAULT;
    
        /* <= 0 means reduce or error.  */
        else if ((yyn = yytable_[yyn]) <= 0)
          {
	    if (yyn == 0 || yyn == yytable_ninf_)
	      label = YYFAIL;
	    else
	      {
	        yyn = -yyn;
	        label = YYREDUCE;
	      }
          }
    
        else
          {
            /* Shift the lookahead token.  */
	    yy_symbol_print ("Shifting", yytoken,
	    		     yylval, yylloc);
    
            /* Discard the token being shifted.  */
            yychar = yyempty_;
    
            /* Count tokens shifted since error; after three, turn off error
               status.  */
            if (yyerrstatus_ > 0)
              --yyerrstatus_;
    
            yystate = yyn;
            yystack.push (yystate, yylval, yylloc);
            label = YYNEWSTATE;
          }
        break;
    
      /*-----------------------------------------------------------.
      | yydefault -- do the default action for the current state.  |
      `-----------------------------------------------------------*/
      case YYDEFAULT:
        yyn = yydefact_[yystate];
        if (yyn == 0)
          label = YYFAIL;
        else
          label = YYREDUCE;
        break;
    
      /*-----------------------------.
      | yyreduce -- Do a reduction.  |
      `-----------------------------*/
      case YYREDUCE:
        yylen = yyr2_[yyn];
        label = yyaction (yyn, yystack, yylen);
	yystate = yystack.stateAt (0);
        break;
    
      /*------------------------------------.
      | yyerrlab -- here on detecting error |
      `------------------------------------*/
      case YYFAIL:
        /* If not already recovering from an error, report this error.  */
        if (yyerrstatus_ == 0)
          {
	    ++yynerrs_;
	    yyerror (yylloc, yysyntax_error (yystate, yytoken));
          }
    
        yyerrloc = yylloc;
        if (yyerrstatus_ == 3)
          {
	    /* If just tried and failed to reuse lookahead token after an
	     error, discard it.  */
    
	    if (yychar <= EOF)
	      {
	      /* Return failure if at end of input.  */
	      if (yychar == EOF)
	        return false;
	      }
	    else
	      yychar = yyempty_;
          }
    
        /* Else will try to reuse lookahead token after shifting the error
           token.  */
        label = YYERRLAB1;
        break;
    
      /*---------------------------------------------------.
      | errorlab -- error raised explicitly by YYERROR.  |
      `---------------------------------------------------*/
      case YYERROR:
    
        yyerrloc = yystack.locationAt (yylen - 1);
        /* Do not reclaim the symbols of the rule which action triggered
           this YYERROR.  */
        yystack.pop (yylen);
        yylen = 0;
        yystate = yystack.stateAt (0);
        label = YYERRLAB1;
        break;
    
      /*-------------------------------------------------------------.
      | yyerrlab1 -- common code for both syntax error and YYERROR.  |
      `-------------------------------------------------------------*/
      case YYERRLAB1:
        yyerrstatus_ = 3;	/* Each real token shifted decrements this.  */
    
        for (;;)
          {
	    yyn = yypact_[yystate];
	    if (yyn != yypact_ninf_)
	      {
	        yyn += yyterror_;
	        if (0 <= yyn && yyn <= yylast_ && yycheck_[yyn] == yyterror_)
	          {
	            yyn = yytable_[yyn];
	            if (0 < yyn)
		      break;
	          }
	      }
    
	    /* Pop the current state because it cannot handle the error token.  */
	    if (yystack.height == 1)
	      return false;
    
	    yyerrloc = yystack.locationAt (0);
	    yystack.pop ();
	    yystate = yystack.stateAt (0);
	    if (yydebug > 0)
	      yystack.print (yyDebugStream);
          }
    
	
	/* Muck with the stack to setup for yylloc.  */
	yystack.push (0, null, yylloc);
	yystack.push (0, null, yyerrloc);
        yyloc = yylloc (yystack, 2);
	yystack.pop (2);

        /* Shift the error token.  */
        yy_symbol_print ("Shifting", yystos_[yyn],
			 yylval, yyloc);
    
        yystate = yyn;
	yystack.push (yyn, yylval, yyloc);
        label = YYNEWSTATE;
        break;
    
        /* Accept.  */
      case YYACCEPT:
        return true;
    
        /* Abort.  */
      case YYABORT:
        return false;
      }
  }

  // Generate an error message.
  private String yysyntax_error (int yystate, int tok)
  {
    if (errorVerbose)
      {
        int yyn = yypact_[yystate];
        if (yypact_ninf_ < yyn && yyn <= yylast_)
          {
	    StringBuffer res;

	    /* Start YYX at -YYN if negative to avoid negative indexes in
	       YYCHECK.  */
	    int yyxbegin = yyn < 0 ? -yyn : 0;

	    /* Stay within bounds of both yycheck and yytname.  */
	    int yychecklim = yylast_ - yyn + 1;
	    int yyxend = yychecklim < yyntokens_ ? yychecklim : yyntokens_;
	    int count = 0;
	    for (int x = yyxbegin; x < yyxend; ++x)
	      if (yycheck_[x + yyn] == x && x != yyterror_)
	        ++count;

	    // FIXME: This method of building the message is not compatible
	    // with internationalization.
	    res = new StringBuffer ("syntax error, unexpected ");
	    res.append (yytnamerr_ (yytname_[tok]));
	    if (count < 5)
	      {
	        count = 0;
	        for (int x = yyxbegin; x < yyxend; ++x)
	          if (yycheck_[x + yyn] == x && x != yyterror_)
		    {
		      res.append (count++ == 0 ? ", expecting " : " or ");
		      res.append (yytnamerr_ (yytname_[x]));
		    }
	      }
	    return res.toString ();
          }
      }

    return "syntax error";
  }


  /* YYPACT[STATE-NUM] -- Index in YYTABLE of the portion describing
     STATE-NUM.  */
  private static final short yypact_ninf_ = -55;
  private static final short yypact_[] =
  {
      2372,   -55,   -55,   -10,   -55,   -55,     2,   -34,   -55,   -14,
     -12,    10,   -55,   -55,  2428,    14,  2428,  2428,  2428,  2428,
    2428,  2314,  2428,  2428,    16,   -55,   -55,   -55,   159,     7,
       9,    13,    24,     0,    69,  2428,  2428,  2428,  2428,  2428,
     -55,    75,   223,   287,   351,   415,    27,   -18,   -55,   -45,
     479,   -55,  2428,  2428,    29,   -55,  2428,  2428,  2428,  2484,
    2428,  2428,  2428,  2428,  2428,  2428,  2428,  2428,  2428,  2428,
    2428,  2428,  2428,  2428,  2428,  2428,  2428,  2428,    25,    26,
    2484,  2484,  2428,   -55,   -55,   -55,   -55,   -55,   -55,   -55,
     -55,    76,   -54,    73,   -55,  2149,    72,  2203,   -55,    42,
     -55,  2428,  2428,   -55,   -55,   -55,   -55,   543,   -55,   607,
      -8,    90,    -6,    68,    41,   -55,  1503,  1567,  1631,  1695,
    1759,  1823,   671,   735,   799,   863,   927,   991,  1055,  1119,
    1183,  1247,  1311,  1375,   -55,   -55,   -55,   -55,    49,    45,
    1439,  2428,   -55,   101,  2428,   -55,  2428,   -55,   -55,   -55,
     -55,  2428,  2428,  2428,    52,    53,   -55,   -55,  2041,  2428,
     105,  2257,   -55,  1879,  1933,  1987,   -55,  2484,   -55,  2428,
     -55,   -55,  2095
  };

  /* YYDEFACT[S] -- default rule to reduce with in state S when YYTABLE
     doesn't specify something else to do.  Zero means the default is an
     error.  */
  private static final byte yydefact_[] =
  {
         0,    10,     2,    17,    16,    18,    19,     0,    72,     0,
       0,     0,    70,    71,     0,     0,     0,     0,     0,     0,
       0,     0,    77,     0,     0,     5,    11,    13,     0,     0,
       0,     0,     0,    93,     0,     0,     0,     0,     0,     0,
      55,     0,     0,     0,     0,     0,     0,     4,    78,     0,
       0,     1,     9,     7,     0,    73,     0,     0,     0,    85,
       0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
       0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
      85,    85,     0,    61,    60,    65,    64,    59,    58,    63,
      62,    94,     0,     0,    53,     0,    51,     0,    54,     0,
      21,    82,    80,    20,     8,     6,    15,     0,    14,     0,
      17,    18,    19,     0,    98,    83,     0,     0,     0,     0,
       0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
       0,     0,     0,     0,    67,    66,    69,    68,    98,    98,
       0,     0,    98,     0,     0,    75,     0,    74,    49,    81,
      79,    89,    91,    87,     0,     0,    50,    57,     0,     0,
      96,     0,    52,     0,     0,     0,    56,    85,    48,     0,
      76,    84,     0
  };

  /* YYPGOTO[NTERM-NUM].  */
  private static final byte yypgoto_[] =
  {
       -55,   102,    21,    48,   -55,   -16,   -55,   -55,   -55,   -55,
      -5,   -43,   -55,    -9
  };

  /* YYDEFGOTO[NTERM-NUM].  */
  private static final short
  yydefgoto_[] =
  {
        -1,    24,    25,    26,    27,    28,    39,    37,    35,    49,
     114,   115,    92,   155
  };

  /* YYTABLE[YYPACT[STATE-NUM]].  What to do in state STATE-NUM.  If
     positive, shift that token.  If negative, reduce the rule which
     number is the opposite.  If zero, do what YYDEFACT says.  */
  private static final short yytable_ninf_ = -98;
  private static final short
  yytable_[] =
  {
        42,    43,    44,    45,   151,   142,   153,    50,    91,   143,
     101,   102,    83,   103,    85,    84,    51,    86,    87,    33,
      95,    88,    97,    29,    30,    29,    30,    31,    32,    89,
     134,   136,    90,   135,   137,    31,    32,    52,    53,    34,
     107,    36,   109,   113,   116,   117,   118,   119,   120,   121,
     122,   123,   124,   125,   126,   127,   128,   129,   130,   131,
     132,   133,    40,    38,   113,   113,   140,    41,    46,    54,
      48,    52,    53,   104,   105,   138,   139,    93,    55,    56,
      99,    58,    59,    94,   106,    96,   100,    98,   141,   144,
     146,    60,    61,    62,    63,    64,    65,    66,    67,    68,
      69,   148,   152,   154,    70,   108,    71,   157,   156,   160,
      72,    73,    74,    75,   166,    76,   167,   169,    77,    78,
      79,    80,    81,    47,   171,   158,     0,   -86,   161,    82,
     -86,   -86,     0,   159,     0,   163,   164,   165,     0,     0,
       0,     0,     0,     0,     0,     0,     0,     0,     0,   149,
     150,   113,     0,   172,     0,     0,     0,     0,     0,   -12,
      54,     0,     0,     0,     0,     0,     0,     0,     0,    55,
      56,    57,    58,    59,     0,     0,     0,   -12,     0,     0,
       0,     0,    60,    61,    62,    63,    64,    65,    66,    67,
      68,    69,     0,     0,   162,    70,     0,    71,     0,     0,
       0,    72,    73,    74,    75,     0,    76,   168,     0,    77,
      78,    79,    80,    81,   -12,   -12,     0,   -12,   -12,     0,
      82,   -12,   -12,   -26,    54,     0,     0,     0,     0,     0,
       0,     0,     0,    55,    56,   -26,    58,    59,     0,     0,
       0,   -26,     0,     0,     0,     0,    60,    61,    62,    63,
      64,    65,    66,    67,    68,    69,     0,     0,     0,   -26,
       0,    71,     0,     0,     0,    72,    73,    74,    75,     0,
      76,     0,     0,    77,    78,    79,    80,    81,   -26,   -26,
       0,   -26,   -26,     0,    82,   -26,   -26,   -25,    54,     0,
       0,     0,     0,     0,     0,     0,     0,    55,   -25,   -25,
     -25,    59,     0,     0,     0,   -25,     0,     0,     0,     0,
      60,    61,    62,    63,    64,    65,    66,    67,    68,    69,
       0,     0,     0,   -25,     0,   -25,     0,     0,     0,    72,
      73,    74,    75,     0,    76,     0,     0,    77,    78,    79,
      80,    81,   -25,   -25,     0,   -25,   -25,     0,    82,   -25,
     -25,   -23,    54,     0,     0,     0,     0,     0,     0,     0,
       0,    55,   -23,   -23,   -23,    59,     0,     0,     0,   -23,
       0,     0,     0,     0,   -23,   -23,   -23,   -23,   -23,   -23,
     -23,   -23,   -23,   -23,     0,     0,     0,   -23,     0,   -23,
       0,     0,     0,   -23,   -23,   -23,   -23,     0,   -23,     0,
       0,    77,    78,    79,    80,    81,   -23,   -23,     0,   -23,
     -23,     0,    82,   -23,   -23,   -22,    54,     0,     0,     0,
       0,     0,     0,     0,     0,    55,   -22,   -22,   -22,    59,
       0,     0,     0,   -22,     0,     0,     0,     0,   -22,   -22,
     -22,   -22,   -22,   -22,   -22,   -22,   -22,   -22,     0,     0,
       0,   -22,     0,   -22,     0,     0,     0,   -22,   -22,   -22,
     -22,     0,   -22,     0,     0,    77,    78,    79,    80,    81,
     -22,   -22,     0,   -22,   -22,     0,    82,   -22,   -22,   -24,
      54,     0,     0,     0,     0,     0,     0,     0,     0,    55,
     -24,   -24,   -24,    59,     0,     0,     0,   -24,     0,     0,
       0,     0,    60,    61,    62,    63,    64,    65,   -24,   -24,
     -24,   -24,     0,     0,     0,   -24,     0,   -24,     0,     0,
       0,    72,    73,    74,    75,     0,    76,     0,     0,    77,
      78,    79,    80,    81,   -24,   -24,     0,   -24,   -24,     0,
      82,   -24,   -24,   -46,    54,     0,     0,     0,     0,     0,
       0,     0,     0,    55,    56,   -46,    58,    59,     0,     0,
       0,   -46,     0,     0,     0,     0,    60,    61,    62,    63,
      64,    65,    66,    67,    68,    69,     0,     0,     0,   -46,
       0,    71,     0,     0,     0,    72,    73,    74,    75,     0,
      76,     0,     0,    77,    78,    79,    80,    81,   -46,   -46,
       0,   -46,   -46,     0,    82,   -46,   -46,   -47,    54,     0,
       0,     0,     0,     0,     0,     0,     0,    55,   -47,   -47,
     -47,    59,     0,     0,     0,   -47,     0,     0,     0,     0,
      60,    61,    62,    63,    64,    65,    66,    67,    68,    69,
       0,     0,     0,   -47,     0,    71,     0,     0,     0,    72,
      73,    74,    75,     0,    76,     0,     0,    77,    78,    79,
      80,    81,   -47,   -47,     0,   -47,   -47,     0,    82,   -47,
     -47,   -42,    54,     0,     0,     0,     0,     0,     0,     0,
       0,    55,   -42,   -42,   -42,    59,     0,     0,     0,   -42,
       0,     0,     0,     0,    60,    61,    62,    63,    64,    65,
     -42,   -42,   -42,   -42,     0,     0,     0,   -42,     0,   -42,
       0,     0,     0,    72,    73,    74,    75,     0,    76,     0,
       0,    77,    78,    79,    80,    81,   -42,   -42,     0,   -42,
     -42,     0,    82,   -42,   -42,   -43,    54,     0,     0,     0,
       0,     0,     0,     0,     0,    55,   -43,   -43,   -43,    59,
       0,     0,     0,   -43,     0,     0,     0,     0,    60,    61,
      62,    63,    64,    65,    66,   -43,    68,   -43,     0,     0,
       0,   -43,     0,   -43,     0,     0,     0,    72,    73,    74,
      75,     0,    76,     0,     0,    77,    78,    79,    80,    81,
     -43,   -43,     0,   -43,   -43,     0,    82,   -43,   -43,   -44,
      54,     0,     0,     0,     0,     0,     0,     0,     0,    55,
     -44,   -44,   -44,    59,     0,     0,     0,   -44,     0,     0,
       0,     0,    60,    61,    62,    63,    64,    65,   -44,   -44,
     -44,   -44,     0,     0,     0,   -44,     0,   -44,     0,     0,
       0,    72,    73,    74,    75,     0,    76,     0,     0,    77,
      78,    79,    80,    81,   -44,   -44,     0,   -44,   -44,     0,
      82,   -44,   -44,   -45,    54,     0,     0,     0,     0,     0,
       0,     0,     0,    55,   -45,   -45,   -45,    59,     0,     0,
       0,   -45,     0,     0,     0,     0,    60,    61,    62,    63,
      64,    65,    66,   -45,    68,   -45,     0,     0,     0,   -45,
       0,   -45,     0,     0,     0,    72,    73,    74,    75,     0,
      76,     0,     0,    77,    78,    79,    80,    81,   -45,   -45,
       0,   -45,   -45,     0,    82,   -45,   -45,   -35,    54,     0,
       0,     0,     0,     0,     0,     0,     0,    55,    56,   -35,
      58,    59,     0,     0,     0,   -35,     0,     0,     0,     0,
      60,    61,    62,    63,    64,    65,    66,    67,    68,    69,
       0,     0,     0,   -35,     0,    71,     0,     0,     0,    72,
      73,    74,    75,     0,    76,     0,     0,    77,    78,    79,
      80,    81,   -35,   -35,     0,   -35,   -35,     0,    82,   -35,
     -35,   -34,    54,     0,     0,     0,     0,     0,     0,     0,
       0,    55,   -34,   -34,   -34,    59,     0,     0,     0,   -34,
       0,     0,     0,     0,    60,    61,    62,    63,    64,    65,
      66,    67,    68,    69,     0,     0,     0,   -34,     0,   -34,
       0,     0,     0,    72,    73,    74,    75,     0,    76,     0,
       0,    77,    78,    79,    80,    81,   -34,   -34,     0,   -34,
     -34,     0,    82,   -34,   -34,   -28,    54,     0,     0,     0,
       0,     0,     0,     0,     0,    55,   -28,   -28,   -28,    59,
       0,     0,     0,   -28,     0,     0,     0,     0,   -28,   -28,
     -28,   -28,   -28,   -28,   -28,   -28,   -28,   -28,     0,     0,
       0,   -28,     0,   -28,     0,     0,     0,   -28,   -28,    74,
      75,     0,    76,     0,     0,    77,    78,    79,    80,    81,
     -28,   -28,     0,   -28,   -28,     0,    82,   -28,   -28,   -29,
      54,     0,     0,     0,     0,     0,     0,     0,     0,    55,
     -29,   -29,   -29,    59,     0,     0,     0,   -29,     0,     0,
       0,     0,   -29,   -29,   -29,   -29,   -29,   -29,   -29,   -29,
     -29,   -29,     0,     0,     0,   -29,     0,   -29,     0,     0,
       0,   -29,   -29,    74,    75,     0,    76,     0,     0,    77,
      78,    79,    80,    81,   -29,   -29,     0,   -29,   -29,     0,
      82,   -29,   -29,   -30,    54,     0,     0,     0,     0,     0,
       0,     0,     0,    55,   -30,   -30,   -30,    59,     0,     0,
       0,   -30,     0,     0,     0,     0,   -30,   -30,   -30,   -30,
     -30,   -30,   -30,   -30,   -30,   -30,     0,     0,     0,   -30,
       0,   -30,     0,     0,     0,   -30,   -30,   -30,   -30,     0,
      76,     0,     0,    77,    78,    79,    80,    81,   -30,   -30,
       0,   -30,   -30,     0,    82,   -30,   -30,   -31,    54,     0,
       0,     0,     0,     0,     0,     0,     0,    55,   -31,   -31,
     -31,    59,     0,     0,     0,   -31,     0,     0,     0,     0,
     -31,   -31,   -31,   -31,   -31,   -31,   -31,   -31,   -31,   -31,
       0,     0,     0,   -31,     0,   -31,     0,     0,     0,   -31,
     -31,   -31,   -31,     0,    76,     0,     0,    77,    78,    79,
      80,    81,   -31,   -31,     0,   -31,   -31,     0,    82,   -31,
     -31,   -27,    54,     0,     0,     0,     0,     0,     0,     0,
       0,    55,   -27,   -27,   -27,    59,     0,     0,     0,   -27,
       0,     0,     0,     0,   -27,   -27,   -27,   -27,   -27,   -27,
     -27,   -27,   -27,   -27,     0,     0,     0,   -27,     0,   -27,
       0,     0,     0,   -27,   -27,   -27,   -27,     0,   -27,     0,
       0,    77,    78,    79,    80,    81,   -27,   -27,     0,   -27,
     -27,     0,    82,   -27,   -27,   -32,    54,     0,     0,     0,
       0,     0,     0,     0,     0,    55,   -32,   -32,   -32,    59,
       0,     0,     0,   -32,     0,     0,     0,     0,   -32,   -32,
     -32,   -32,   -32,   -32,   -32,   -32,   -32,   -32,     0,     0,
       0,   -32,     0,   -32,     0,     0,     0,   -32,   -32,   -32,
     -32,     0,   -32,     0,     0,    77,    78,    79,    80,    81,
     -32,   -32,     0,   -32,   -32,     0,    82,   -32,   -32,   -33,
      54,     0,     0,     0,     0,     0,     0,     0,     0,    55,
      56,   -33,    58,    59,     0,     0,     0,   -33,     0,     0,
       0,     0,    60,    61,    62,    63,    64,    65,    66,    67,
      68,    69,     0,     0,     0,    70,     0,    71,     0,     0,
       0,    72,    73,    74,    75,     0,    76,     0,     0,    77,
      78,    79,    80,    81,   -33,   -33,     0,   -33,   -33,     0,
      82,   -33,   -33,   -41,    54,     0,     0,     0,     0,     0,
       0,     0,     0,    55,   -41,   -41,   -41,    59,     0,     0,
       0,   -41,     0,     0,     0,     0,     0,     0,     0,     0,
       0,     0,   -41,   -41,   -41,   -41,     0,     0,     0,   -41,
       0,   -41,     0,     0,     0,    72,    73,    74,    75,     0,
      76,     0,     0,    77,    78,    79,    80,    81,   -41,   -41,
       0,   -41,   -41,     0,    82,   -41,   -41,   -40,    54,     0,
       0,     0,     0,     0,     0,     0,     0,    55,   -40,   -40,
     -40,    59,     0,     0,     0,   -40,     0,     0,     0,     0,
       0,     0,     0,     0,     0,     0,   -40,   -40,   -40,   -40,
       0,     0,     0,   -40,     0,   -40,     0,     0,     0,    72,
      73,    74,    75,     0,    76,     0,     0,    77,    78,    79,
      80,    81,   -40,   -40,     0,   -40,   -40,     0,    82,   -40,
     -40,   -36,    54,     0,     0,     0,     0,     0,     0,     0,
       0,    55,   -36,   -36,   -36,    59,     0,     0,     0,   -36,
       0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     -36,   -36,   -36,   -36,     0,     0,     0,   -36,     0,   -36,
       0,     0,     0,    72,    73,    74,    75,     0,    76,     0,
       0,    77,    78,    79,    80,    81,   -36,   -36,     0,   -36,
     -36,     0,    82,   -36,   -36,   -37,    54,     0,     0,     0,
       0,     0,     0,     0,     0,    55,   -37,   -37,   -37,    59,
       0,     0,     0,   -37,     0,     0,     0,     0,     0,     0,
       0,     0,     0,     0,   -37,   -37,   -37,   -37,     0,     0,
       0,   -37,     0,   -37,     0,     0,     0,    72,    73,    74,
      75,     0,    76,     0,     0,    77,    78,    79,    80,    81,
     -37,   -37,     0,   -37,   -37,     0,    82,   -37,   -37,   -38,
      54,     0,     0,     0,     0,     0,     0,     0,     0,    55,
     -38,   -38,   -38,    59,     0,     0,     0,   -38,     0,     0,
       0,     0,     0,     0,     0,     0,     0,     0,   -38,   -38,
     -38,   -38,     0,     0,     0,   -38,     0,   -38,     0,     0,
       0,    72,    73,    74,    75,     0,    76,     0,     0,    77,
      78,    79,    80,    81,   -38,   -38,     0,   -38,   -38,     0,
      82,   -38,   -38,   -39,    54,     0,     0,     0,     0,     0,
       0,     0,     0,    55,   -39,   -39,   -39,    59,     0,     0,
       0,   -39,     0,     0,     0,     0,     0,     0,     0,     0,
       0,     0,   -39,   -39,   -39,   -39,     0,     0,     0,   -39,
       0,   -39,     0,     0,     0,    72,    73,    74,    75,     0,
      76,     0,     0,    77,    78,    79,    80,    81,   -39,   -39,
      54,   -39,   -39,     0,    82,   -39,   -39,     0,     0,    55,
      56,     0,    58,    59,     0,     0,     0,     0,     0,     0,
       0,     0,    60,    61,    62,    63,    64,    65,    66,    67,
      68,    69,     0,     0,     0,    70,     0,    71,     0,     0,
       0,    72,    73,    74,    75,     0,    76,     0,     0,    77,
      78,    79,    80,    81,    54,     0,     0,     0,   -90,     0,
      82,   -90,   -90,    55,    56,     0,    58,    59,     0,     0,
       0,     0,     0,     0,     0,     0,    60,    61,    62,    63,
      64,    65,    66,    67,    68,    69,     0,     0,     0,    70,
       0,    71,     0,     0,     0,    72,    73,    74,    75,     0,
      76,     0,     0,    77,    78,    79,    80,    81,    54,     0,
       0,     0,   -92,     0,    82,   -92,   -92,    55,    56,     0,
      58,    59,     0,     0,     0,     0,     0,     0,     0,     0,
      60,    61,    62,    63,    64,    65,    66,    67,    68,    69,
       0,     0,     0,    70,     0,    71,     0,     0,     0,    72,
      73,    74,    75,     0,    76,     0,     0,    77,    78,    79,
      80,    81,    54,     0,     0,     0,   -88,     0,    82,   -88,
     -88,    55,    56,     0,    58,    59,     0,     0,     0,     0,
       0,     0,     0,     0,    60,    61,    62,    63,    64,    65,
      66,    67,    68,    69,     0,     0,     0,    70,     0,    71,
       0,     0,     0,    72,    73,    74,    75,     0,    76,     0,
       0,    77,    78,    79,    80,    81,    54,     0,     0,     0,
     -95,     0,    82,     0,   -95,    55,    56,     0,    58,    59,
       0,     0,     0,     0,     0,     0,     0,     0,    60,    61,
      62,    63,    64,    65,    66,    67,    68,    69,     0,     0,
       0,    70,     0,    71,     0,     0,     0,    72,    73,    74,
      75,     0,    76,     0,     0,    77,    78,    79,    80,    81,
      54,     0,     0,     0,   -97,     0,    82,     0,   -97,    55,
      56,     0,    58,    59,     0,     0,     0,     0,     0,     0,
       0,     0,    60,    61,    62,    63,    64,    65,    66,    67,
      68,    69,     0,     0,     0,    70,     0,    71,     0,     0,
       0,    72,    73,    74,    75,     0,    76,     0,     0,    77,
      78,    79,    80,    81,    54,     0,     0,     0,   145,     0,
      82,     0,     0,    55,    56,     0,    58,    59,     0,     0,
       0,     0,     0,     0,     0,     0,    60,    61,    62,    63,
      64,    65,    66,    67,    68,    69,     0,     0,     0,    70,
       0,    71,     0,     0,     0,    72,    73,    74,    75,     0,
      76,     0,     0,    77,    78,    79,    80,    81,    54,     0,
       0,     0,   147,     0,    82,     0,     0,    55,    56,     0,
      58,    59,     0,     0,     0,     0,     0,     0,     0,     0,
      60,    61,    62,    63,    64,    65,    66,    67,    68,    69,
       0,     0,     0,    70,     0,    71,     0,     0,     0,    72,
      73,    74,    75,     0,    76,     0,     0,    77,    78,    79,
      80,    81,     0,     0,    -3,     1,   170,     2,    82,     3,
       4,     5,     6,     7,     8,     0,     0,     0,     0,     9,
       0,    10,     0,    11,    12,    13,    14,     0,     0,     0,
       0,     0,     0,     0,     0,     0,     0,     0,     0,    15,
      16,     0,    17,     0,     0,     0,    18,    19,     0,     0,
       0,     0,     0,     0,     0,     0,     0,    20,     0,    21,
      -3,    22,     0,     1,    23,     2,     0,     3,     4,     5,
       6,     7,     8,     0,     0,     0,     0,     9,     0,    10,
       0,    11,    12,    13,    14,     0,     0,     0,     0,     0,
       0,     0,     0,     0,     0,     0,     0,    15,    16,     0,
      17,     0,     0,     0,    18,    19,     0,     0,     0,     0,
       0,     0,     0,     0,     0,    20,     0,    21,     0,    22,
       0,     0,    23,     3,     4,     5,     6,     7,     8,     0,
       0,     0,     0,     9,     0,    10,     0,    11,    12,    13,
      14,     0,     0,     0,     0,     0,     0,     0,     0,     0,
       0,     0,     0,    15,    16,     0,    17,     0,     0,     0,
      18,    19,     0,     0,     0,     0,     0,     0,     0,     0,
       0,    20,     0,     0,     0,    22,     0,     0,    23,   110,
       4,   111,   112,     7,     8,     0,     0,     0,     0,     9,
       0,    10,     0,    11,    12,    13,    14,     0,     0,     0,
       0,     0,     0,     0,     0,     0,     0,     0,     0,    15,
      16,     0,    17,     0,     0,     0,    18,    19,     0,     0,
       0,     0,     0,     0,     0,     0,     0,    20,     0,     0,
       0,    22,     0,     0,    23
  };

  /* YYCHECK.  */
  private static final short
  yycheck_[] =
  {
        16,    17,    18,    19,    12,    59,    12,    23,     8,    63,
      55,    56,     5,    58,     5,     8,     0,     8,     5,    53,
      36,     8,    38,    33,    34,    33,    34,    33,    34,     5,
       5,     5,     8,     8,     8,    33,    34,    55,    56,    53,
      56,    53,    58,    59,    60,    61,    62,    63,    64,    65,
      66,    67,    68,    69,    70,    71,    72,    73,    74,    75,
      76,    77,    14,    53,    80,    81,    82,    53,    20,     1,
      22,    55,    56,    52,    53,    80,    81,     8,    10,    11,
       5,    13,    14,    35,    55,    37,    59,    39,    12,    16,
      18,    23,    24,    25,    26,    27,    28,    29,    30,    31,
      32,    59,    12,    62,    36,    57,    38,    62,    59,     8,
      42,    43,    44,    45,    62,    47,    63,    12,    50,    51,
      52,    53,    54,    21,   167,   141,    -1,    59,   144,    61,
      62,    63,    -1,   142,    -1,   151,   152,   153,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,   101,
     102,   167,    -1,   169,    -1,    -1,    -1,    -1,    -1,     0,
       1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    10,
      11,    12,    13,    14,    -1,    -1,    -1,    18,    -1,    -1,
      -1,    -1,    23,    24,    25,    26,    27,    28,    29,    30,
      31,    32,    -1,    -1,   146,    36,    -1,    38,    -1,    -1,
      -1,    42,    43,    44,    45,    -1,    47,   159,    -1,    50,
      51,    52,    53,    54,    55,    56,    -1,    58,    59,    -1,
      61,    62,    63,     0,     1,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    10,    11,    12,    13,    14,    -1,    -1,
      -1,    18,    -1,    -1,    -1,    -1,    23,    24,    25,    26,
      27,    28,    29,    30,    31,    32,    -1,    -1,    -1,    36,
      -1,    38,    -1,    -1,    -1,    42,    43,    44,    45,    -1,
      47,    -1,    -1,    50,    51,    52,    53,    54,    55,    56,
      -1,    58,    59,    -1,    61,    62,    63,     0,     1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    10,    11,    12,
      13,    14,    -1,    -1,    -1,    18,    -1,    -1,    -1,    -1,
      23,    24,    25,    26,    27,    28,    29,    30,    31,    32,
      -1,    -1,    -1,    36,    -1,    38,    -1,    -1,    -1,    42,
      43,    44,    45,    -1,    47,    -1,    -1,    50,    51,    52,
      53,    54,    55,    56,    -1,    58,    59,    -1,    61,    62,
      63,     0,     1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,
      -1,    10,    11,    12,    13,    14,    -1,    -1,    -1,    18,
      -1,    -1,    -1,    -1,    23,    24,    25,    26,    27,    28,
      29,    30,    31,    32,    -1,    -1,    -1,    36,    -1,    38,
      -1,    -1,    -1,    42,    43,    44,    45,    -1,    47,    -1,
      -1,    50,    51,    52,    53,    54,    55,    56,    -1,    58,
      59,    -1,    61,    62,    63,     0,     1,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    10,    11,    12,    13,    14,
      -1,    -1,    -1,    18,    -1,    -1,    -1,    -1,    23,    24,
      25,    26,    27,    28,    29,    30,    31,    32,    -1,    -1,
      -1,    36,    -1,    38,    -1,    -1,    -1,    42,    43,    44,
      45,    -1,    47,    -1,    -1,    50,    51,    52,    53,    54,
      55,    56,    -1,    58,    59,    -1,    61,    62,    63,     0,
       1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    10,
      11,    12,    13,    14,    -1,    -1,    -1,    18,    -1,    -1,
      -1,    -1,    23,    24,    25,    26,    27,    28,    29,    30,
      31,    32,    -1,    -1,    -1,    36,    -1,    38,    -1,    -1,
      -1,    42,    43,    44,    45,    -1,    47,    -1,    -1,    50,
      51,    52,    53,    54,    55,    56,    -1,    58,    59,    -1,
      61,    62,    63,     0,     1,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    10,    11,    12,    13,    14,    -1,    -1,
      -1,    18,    -1,    -1,    -1,    -1,    23,    24,    25,    26,
      27,    28,    29,    30,    31,    32,    -1,    -1,    -1,    36,
      -1,    38,    -1,    -1,    -1,    42,    43,    44,    45,    -1,
      47,    -1,    -1,    50,    51,    52,    53,    54,    55,    56,
      -1,    58,    59,    -1,    61,    62,    63,     0,     1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    10,    11,    12,
      13,    14,    -1,    -1,    -1,    18,    -1,    -1,    -1,    -1,
      23,    24,    25,    26,    27,    28,    29,    30,    31,    32,
      -1,    -1,    -1,    36,    -1,    38,    -1,    -1,    -1,    42,
      43,    44,    45,    -1,    47,    -1,    -1,    50,    51,    52,
      53,    54,    55,    56,    -1,    58,    59,    -1,    61,    62,
      63,     0,     1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,
      -1,    10,    11,    12,    13,    14,    -1,    -1,    -1,    18,
      -1,    -1,    -1,    -1,    23,    24,    25,    26,    27,    28,
      29,    30,    31,    32,    -1,    -1,    -1,    36,    -1,    38,
      -1,    -1,    -1,    42,    43,    44,    45,    -1,    47,    -1,
      -1,    50,    51,    52,    53,    54,    55,    56,    -1,    58,
      59,    -1,    61,    62,    63,     0,     1,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    10,    11,    12,    13,    14,
      -1,    -1,    -1,    18,    -1,    -1,    -1,    -1,    23,    24,
      25,    26,    27,    28,    29,    30,    31,    32,    -1,    -1,
      -1,    36,    -1,    38,    -1,    -1,    -1,    42,    43,    44,
      45,    -1,    47,    -1,    -1,    50,    51,    52,    53,    54,
      55,    56,    -1,    58,    59,    -1,    61,    62,    63,     0,
       1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    10,
      11,    12,    13,    14,    -1,    -1,    -1,    18,    -1,    -1,
      -1,    -1,    23,    24,    25,    26,    27,    28,    29,    30,
      31,    32,    -1,    -1,    -1,    36,    -1,    38,    -1,    -1,
      -1,    42,    43,    44,    45,    -1,    47,    -1,    -1,    50,
      51,    52,    53,    54,    55,    56,    -1,    58,    59,    -1,
      61,    62,    63,     0,     1,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    10,    11,    12,    13,    14,    -1,    -1,
      -1,    18,    -1,    -1,    -1,    -1,    23,    24,    25,    26,
      27,    28,    29,    30,    31,    32,    -1,    -1,    -1,    36,
      -1,    38,    -1,    -1,    -1,    42,    43,    44,    45,    -1,
      47,    -1,    -1,    50,    51,    52,    53,    54,    55,    56,
      -1,    58,    59,    -1,    61,    62,    63,     0,     1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    10,    11,    12,
      13,    14,    -1,    -1,    -1,    18,    -1,    -1,    -1,    -1,
      23,    24,    25,    26,    27,    28,    29,    30,    31,    32,
      -1,    -1,    -1,    36,    -1,    38,    -1,    -1,    -1,    42,
      43,    44,    45,    -1,    47,    -1,    -1,    50,    51,    52,
      53,    54,    55,    56,    -1,    58,    59,    -1,    61,    62,
      63,     0,     1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,
      -1,    10,    11,    12,    13,    14,    -1,    -1,    -1,    18,
      -1,    -1,    -1,    -1,    23,    24,    25,    26,    27,    28,
      29,    30,    31,    32,    -1,    -1,    -1,    36,    -1,    38,
      -1,    -1,    -1,    42,    43,    44,    45,    -1,    47,    -1,
      -1,    50,    51,    52,    53,    54,    55,    56,    -1,    58,
      59,    -1,    61,    62,    63,     0,     1,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    10,    11,    12,    13,    14,
      -1,    -1,    -1,    18,    -1,    -1,    -1,    -1,    23,    24,
      25,    26,    27,    28,    29,    30,    31,    32,    -1,    -1,
      -1,    36,    -1,    38,    -1,    -1,    -1,    42,    43,    44,
      45,    -1,    47,    -1,    -1,    50,    51,    52,    53,    54,
      55,    56,    -1,    58,    59,    -1,    61,    62,    63,     0,
       1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    10,
      11,    12,    13,    14,    -1,    -1,    -1,    18,    -1,    -1,
      -1,    -1,    23,    24,    25,    26,    27,    28,    29,    30,
      31,    32,    -1,    -1,    -1,    36,    -1,    38,    -1,    -1,
      -1,    42,    43,    44,    45,    -1,    47,    -1,    -1,    50,
      51,    52,    53,    54,    55,    56,    -1,    58,    59,    -1,
      61,    62,    63,     0,     1,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    10,    11,    12,    13,    14,    -1,    -1,
      -1,    18,    -1,    -1,    -1,    -1,    23,    24,    25,    26,
      27,    28,    29,    30,    31,    32,    -1,    -1,    -1,    36,
      -1,    38,    -1,    -1,    -1,    42,    43,    44,    45,    -1,
      47,    -1,    -1,    50,    51,    52,    53,    54,    55,    56,
      -1,    58,    59,    -1,    61,    62,    63,     0,     1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    10,    11,    12,
      13,    14,    -1,    -1,    -1,    18,    -1,    -1,    -1,    -1,
      23,    24,    25,    26,    27,    28,    29,    30,    31,    32,
      -1,    -1,    -1,    36,    -1,    38,    -1,    -1,    -1,    42,
      43,    44,    45,    -1,    47,    -1,    -1,    50,    51,    52,
      53,    54,    55,    56,    -1,    58,    59,    -1,    61,    62,
      63,     0,     1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,
      -1,    10,    11,    12,    13,    14,    -1,    -1,    -1,    18,
      -1,    -1,    -1,    -1,    23,    24,    25,    26,    27,    28,
      29,    30,    31,    32,    -1,    -1,    -1,    36,    -1,    38,
      -1,    -1,    -1,    42,    43,    44,    45,    -1,    47,    -1,
      -1,    50,    51,    52,    53,    54,    55,    56,    -1,    58,
      59,    -1,    61,    62,    63,     0,     1,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    10,    11,    12,    13,    14,
      -1,    -1,    -1,    18,    -1,    -1,    -1,    -1,    23,    24,
      25,    26,    27,    28,    29,    30,    31,    32,    -1,    -1,
      -1,    36,    -1,    38,    -1,    -1,    -1,    42,    43,    44,
      45,    -1,    47,    -1,    -1,    50,    51,    52,    53,    54,
      55,    56,    -1,    58,    59,    -1,    61,    62,    63,     0,
       1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    10,
      11,    12,    13,    14,    -1,    -1,    -1,    18,    -1,    -1,
      -1,    -1,    23,    24,    25,    26,    27,    28,    29,    30,
      31,    32,    -1,    -1,    -1,    36,    -1,    38,    -1,    -1,
      -1,    42,    43,    44,    45,    -1,    47,    -1,    -1,    50,
      51,    52,    53,    54,    55,    56,    -1,    58,    59,    -1,
      61,    62,    63,     0,     1,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    10,    11,    12,    13,    14,    -1,    -1,
      -1,    18,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    29,    30,    31,    32,    -1,    -1,    -1,    36,
      -1,    38,    -1,    -1,    -1,    42,    43,    44,    45,    -1,
      47,    -1,    -1,    50,    51,    52,    53,    54,    55,    56,
      -1,    58,    59,    -1,    61,    62,    63,     0,     1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    10,    11,    12,
      13,    14,    -1,    -1,    -1,    18,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    29,    30,    31,    32,
      -1,    -1,    -1,    36,    -1,    38,    -1,    -1,    -1,    42,
      43,    44,    45,    -1,    47,    -1,    -1,    50,    51,    52,
      53,    54,    55,    56,    -1,    58,    59,    -1,    61,    62,
      63,     0,     1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,
      -1,    10,    11,    12,    13,    14,    -1,    -1,    -1,    18,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,
      29,    30,    31,    32,    -1,    -1,    -1,    36,    -1,    38,
      -1,    -1,    -1,    42,    43,    44,    45,    -1,    47,    -1,
      -1,    50,    51,    52,    53,    54,    55,    56,    -1,    58,
      59,    -1,    61,    62,    63,     0,     1,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    10,    11,    12,    13,    14,
      -1,    -1,    -1,    18,    -1,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    29,    30,    31,    32,    -1,    -1,
      -1,    36,    -1,    38,    -1,    -1,    -1,    42,    43,    44,
      45,    -1,    47,    -1,    -1,    50,    51,    52,    53,    54,
      55,    56,    -1,    58,    59,    -1,    61,    62,    63,     0,
       1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    10,
      11,    12,    13,    14,    -1,    -1,    -1,    18,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    29,    30,
      31,    32,    -1,    -1,    -1,    36,    -1,    38,    -1,    -1,
      -1,    42,    43,    44,    45,    -1,    47,    -1,    -1,    50,
      51,    52,    53,    54,    55,    56,    -1,    58,    59,    -1,
      61,    62,    63,     0,     1,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    10,    11,    12,    13,    14,    -1,    -1,
      -1,    18,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    29,    30,    31,    32,    -1,    -1,    -1,    36,
      -1,    38,    -1,    -1,    -1,    42,    43,    44,    45,    -1,
      47,    -1,    -1,    50,    51,    52,    53,    54,    55,    56,
       1,    58,    59,    -1,    61,    62,    63,    -1,    -1,    10,
      11,    -1,    13,    14,    -1,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    23,    24,    25,    26,    27,    28,    29,    30,
      31,    32,    -1,    -1,    -1,    36,    -1,    38,    -1,    -1,
      -1,    42,    43,    44,    45,    -1,    47,    -1,    -1,    50,
      51,    52,    53,    54,     1,    -1,    -1,    -1,    59,    -1,
      61,    62,    63,    10,    11,    -1,    13,    14,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    23,    24,    25,    26,
      27,    28,    29,    30,    31,    32,    -1,    -1,    -1,    36,
      -1,    38,    -1,    -1,    -1,    42,    43,    44,    45,    -1,
      47,    -1,    -1,    50,    51,    52,    53,    54,     1,    -1,
      -1,    -1,    59,    -1,    61,    62,    63,    10,    11,    -1,
      13,    14,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,
      23,    24,    25,    26,    27,    28,    29,    30,    31,    32,
      -1,    -1,    -1,    36,    -1,    38,    -1,    -1,    -1,    42,
      43,    44,    45,    -1,    47,    -1,    -1,    50,    51,    52,
      53,    54,     1,    -1,    -1,    -1,    59,    -1,    61,    62,
      63,    10,    11,    -1,    13,    14,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    23,    24,    25,    26,    27,    28,
      29,    30,    31,    32,    -1,    -1,    -1,    36,    -1,    38,
      -1,    -1,    -1,    42,    43,    44,    45,    -1,    47,    -1,
      -1,    50,    51,    52,    53,    54,     1,    -1,    -1,    -1,
      59,    -1,    61,    -1,    63,    10,    11,    -1,    13,    14,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    23,    24,
      25,    26,    27,    28,    29,    30,    31,    32,    -1,    -1,
      -1,    36,    -1,    38,    -1,    -1,    -1,    42,    43,    44,
      45,    -1,    47,    -1,    -1,    50,    51,    52,    53,    54,
       1,    -1,    -1,    -1,    59,    -1,    61,    -1,    63,    10,
      11,    -1,    13,    14,    -1,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    23,    24,    25,    26,    27,    28,    29,    30,
      31,    32,    -1,    -1,    -1,    36,    -1,    38,    -1,    -1,
      -1,    42,    43,    44,    45,    -1,    47,    -1,    -1,    50,
      51,    52,    53,    54,     1,    -1,    -1,    -1,    59,    -1,
      61,    -1,    -1,    10,    11,    -1,    13,    14,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    23,    24,    25,    26,
      27,    28,    29,    30,    31,    32,    -1,    -1,    -1,    36,
      -1,    38,    -1,    -1,    -1,    42,    43,    44,    45,    -1,
      47,    -1,    -1,    50,    51,    52,    53,    54,     1,    -1,
      -1,    -1,    59,    -1,    61,    -1,    -1,    10,    11,    -1,
      13,    14,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,
      23,    24,    25,    26,    27,    28,    29,    30,    31,    32,
      -1,    -1,    -1,    36,    -1,    38,    -1,    -1,    -1,    42,
      43,    44,    45,    -1,    47,    -1,    -1,    50,    51,    52,
      53,    54,    -1,    -1,     0,     1,    59,     3,    61,     5,
       6,     7,     8,     9,    10,    -1,    -1,    -1,    -1,    15,
      -1,    17,    -1,    19,    20,    21,    22,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    35,
      36,    -1,    38,    -1,    -1,    -1,    42,    43,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    53,    -1,    55,
      56,    57,    -1,     1,    60,     3,    -1,     5,     6,     7,
       8,     9,    10,    -1,    -1,    -1,    -1,    15,    -1,    17,
      -1,    19,    20,    21,    22,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    35,    36,    -1,
      38,    -1,    -1,    -1,    42,    43,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    53,    -1,    55,    -1,    57,
      -1,    -1,    60,     5,     6,     7,     8,     9,    10,    -1,
      -1,    -1,    -1,    15,    -1,    17,    -1,    19,    20,    21,
      22,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    35,    36,    -1,    38,    -1,    -1,    -1,
      42,    43,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,
      -1,    53,    -1,    -1,    -1,    57,    -1,    -1,    60,     5,
       6,     7,     8,     9,    10,    -1,    -1,    -1,    -1,    15,
      -1,    17,    -1,    19,    20,    21,    22,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    -1,    35,
      36,    -1,    38,    -1,    -1,    -1,    42,    43,    -1,    -1,
      -1,    -1,    -1,    -1,    -1,    -1,    -1,    53,    -1,    -1,
      -1,    57,    -1,    -1,    60
  };

  /* STOS_[STATE-NUM] -- The (internal number of the) accessing
     symbol of state STATE-NUM.  */
  private static final byte
  yystos_[] =
  {
         0,     1,     3,     5,     6,     7,     8,     9,    10,    15,
      17,    19,    20,    21,    22,    35,    36,    38,    42,    43,
      53,    55,    57,    60,    65,    66,    67,    68,    69,    33,
      34,    33,    34,    53,    53,    72,    53,    71,    53,    70,
      67,    53,    69,    69,    69,    69,    67,    65,    67,    73,
      69,     0,    55,    56,     1,    10,    11,    12,    13,    14,
      23,    24,    25,    26,    27,    28,    29,    30,    31,    32,
      36,    38,    42,    43,    44,    45,    47,    50,    51,    52,
      53,    54,    61,     5,     8,     5,     8,     5,     8,     5,
       8,     8,    76,     8,    67,    69,    67,    69,    67,     5,
      59,    55,    56,    58,    66,    66,    55,    69,    67,    69,
       5,     7,     8,    69,    74,    75,    69,    69,    69,    69,
      69,    69,    69,    69,    69,    69,    69,    69,    69,    69,
      69,    69,    69,    69,     5,     8,     5,     8,    74,    74,
      69,    12,    59,    63,    16,    59,    18,    59,    59,    67,
      67,    12,    12,    12,    62,    77,    59,    62,    69,    77,
       8,    69,    67,    69,    69,    69,    62,    63,    67,    12,
      59,    75,    69
  };

  /* TOKEN_NUMBER_[YYLEX-NUM] -- Internal symbol number corresponding
     to YYLEX-NUM.  */
  private static final short
  yytoken_number_[] =
  {
         0,   256,   257,   258,   259,   260,   261,   262,   263,   264,
     265,   266,   267,   268,   269,   270,   271,   272,   273,   274,
     275,   276,   277,   278,   279,   280,   281,   282,   283,   284,
     285,   286,   287,   288,   289,   290,    63,   291,   126,   292,
     293,   294,    43,    45,    42,    47,   295,    58,   296,   297,
      94,    36,    64,    40,    91,    10,    59,   123,   125,    41,
      33,    37,    93,    44
  };

  /* YYR1[YYN] -- Symbol number of symbol that rule YYN derives.  */
  private static final byte
  yyr1_[] =
  {
         0,    64,    65,    65,    65,    65,    65,    65,    65,    65,
      65,    66,    67,    67,    68,    69,    69,    69,    69,    69,
      69,    69,    69,    69,    69,    69,    69,    69,    69,    69,
      69,    69,    69,    69,    69,    69,    69,    69,    69,    69,
      69,    69,    69,    69,    69,    69,    69,    69,    69,    69,
      69,    69,    69,    69,    69,    69,    69,    69,    69,    69,
      69,    69,    69,    69,    69,    69,    69,    69,    69,    69,
      69,    69,    69,    69,    70,    71,    72,    73,    73,    73,
      73,    73,    73,    74,    74,    75,    75,    75,    75,    75,
      75,    75,    75,    76,    76,    76,    76,    76,    77
  };

  /* YYR2[YYN] -- Number of symbols composing right hand side of rule YYN.  */
  private static final byte
  yyr2_[] =
  {
         0,     2,     1,     1,     2,     1,     3,     2,     3,     2,
       1,     1,     1,     1,     3,     3,     1,     1,     1,     1,
       3,     3,     2,     2,     2,     2,     2,     3,     3,     3,
       3,     3,     3,     3,     3,     3,     3,     3,     3,     3,
       3,     3,     3,     3,     3,     3,     3,     3,     6,     4,
       4,     3,     5,     3,     3,     2,     5,     4,     3,     3,
       3,     3,     3,     3,     3,     3,     3,     3,     3,     3,
       1,     1,     1,     2,     3,     3,     5,     0,     1,     3,
       2,     3,     2,     1,     4,     0,     1,     2,     3,     2,
       3,     2,     3,     0,     1,     3,     3,     5,     0
  };

  /* YYTNAME[SYMBOL-NUM] -- String name of the symbol SYMBOL-NUM.
     First, the terminals, then, starting at \a yyntokens_, nonterminals.  */
  private static final String yytname_[] =
  {
    "$end", "error", "$undefined", "END_OF_INPUT", "ERROR", "STR_CONST",
  "NUM_CONST", "NULL_CONST", "SYMBOL", "FUNCTION", "COMMENT",
  "LEFT_ASSIGN", "EQ_ASSIGN", "RIGHT_ASSIGN", "LBB", "FOR", "IN", "IF",
  "ELSE", "WHILE", "NEXT", "BREAK", "REPEAT", "GT", "GE", "LT", "LE", "EQ",
  "NE", "AND", "OR", "AND2", "OR2", "NS_GET", "NS_GET_INT",
  "LIBRARY_IMPORT", "'?'", "LOW", "'~'", "TILDE", "NOT", "UNOT", "'+'",
  "'-'", "'*'", "'/'", "SPECIAL", "':'", "UPLUS", "UMINUS", "'^'", "'$'",
  "'@'", "'('", "'['", "'\\n'", "';'", "'{'", "'}'", "')'", "'!'", "'%'",
  "']'", "','", "$accept", "script", "prog", "expr_or_assign",
  "equal_assign", "expr", "cond", "ifcond", "forcond", "exprlist",
  "sublist", "sub", "formlist", "cr", null
  };

  /* YYRHS -- A `-1'-separated list of the rules' RHS.  */
  private static final byte yyrhs_[] =
  {
        65,     0,    -1,     3,    -1,    55,    -1,    55,    65,    -1,
      66,    -1,    65,    56,    66,    -1,    65,    56,    -1,    65,
      55,    66,    -1,    65,    55,    -1,     1,    -1,    67,    -1,
      69,    -1,    68,    -1,    69,    12,    67,    -1,    69,     1,
      55,    -1,     6,    -1,     5,    -1,     7,    -1,     8,    -1,
      57,    73,    58,    -1,    53,    67,    59,    -1,    43,    69,
      -1,    42,    69,    -1,    60,    69,    -1,    38,    69,    -1,
      36,    69,    -1,    69,    47,    69,    -1,    69,    42,    69,
      -1,    69,    43,    69,    -1,    69,    44,    69,    -1,    69,
      45,    69,    -1,    69,    50,    69,    -1,    69,    61,    69,
      -1,    69,    38,    69,    -1,    69,    36,    69,    -1,    69,
      25,    69,    -1,    69,    26,    69,    -1,    69,    27,    69,
      -1,    69,    28,    69,    -1,    69,    24,    69,    -1,    69,
      23,    69,    -1,    69,    29,    69,    -1,    69,    30,    69,
      -1,    69,    31,    69,    -1,    69,    32,    69,    -1,    69,
      11,    69,    -1,    69,    13,    69,    -1,     9,    53,    76,
      59,    77,    67,    -1,    35,    53,     5,    59,    -1,    69,
      53,    74,    59,    -1,    17,    71,    67,    -1,    17,    71,
      67,    18,    67,    -1,    15,    72,    67,    -1,    19,    70,
      67,    -1,    22,    67,    -1,    69,    14,    74,    62,    62,
      -1,    69,    54,    74,    62,    -1,     8,    33,     8,    -1,
       8,    33,     5,    -1,     5,    33,     8,    -1,     5,    33,
       5,    -1,     8,    34,     8,    -1,     8,    34,     5,    -1,
       5,    34,     8,    -1,     5,    34,     5,    -1,    69,    51,
       8,    -1,    69,    51,     5,    -1,    69,    52,     8,    -1,
      69,    52,     5,    -1,    20,    -1,    21,    -1,    10,    -1,
      69,    10,    -1,    53,    69,    59,    -1,    53,    69,    59,
      -1,    53,     8,    16,    69,    59,    -1,    -1,    67,    -1,
      73,    56,    67,    -1,    73,    56,    -1,    73,    55,    67,
      -1,    73,    55,    -1,    75,    -1,    74,    77,    63,    75,
      -1,    -1,    69,    -1,     8,    12,    -1,     8,    12,    69,
      -1,     5,    12,    -1,     5,    12,    69,    -1,     7,    12,
      -1,     7,    12,    69,    -1,    -1,     8,    -1,     8,    12,
      69,    -1,    76,    63,     8,    -1,    76,    63,     8,    12,
      69,    -1,    -1
  };

  /* YYPRHS[YYN] -- Index of the first RHS symbol of rule number YYN in
     YYRHS.  */
  private static final short yyprhs_[] =
  {
         0,     0,     3,     5,     7,    10,    12,    16,    19,    23,
      26,    28,    30,    32,    34,    38,    42,    44,    46,    48,
      50,    54,    58,    61,    64,    67,    70,    73,    77,    81,
      85,    89,    93,    97,   101,   105,   109,   113,   117,   121,
     125,   129,   133,   137,   141,   145,   149,   153,   157,   164,
     169,   174,   178,   184,   188,   192,   195,   201,   206,   210,
     214,   218,   222,   226,   230,   234,   238,   242,   246,   250,
     254,   256,   258,   260,   263,   267,   271,   277,   278,   280,
     284,   287,   291,   294,   296,   301,   302,   304,   307,   311,
     314,   318,   321,   325,   326,   328,   332,   336,   342
  };

  /* YYRLINE[YYN] -- Source line where rule number YYN was defined.  */
  private static final short yyrline_[] =
  {
         0,   141,   141,   143,   145,   147,   150,   154,   158,   162,
     166,   173,   176,   177,   180,   185,   187,   195,   200,   204,
     209,   218,   224,   228,   232,   236,   240,   246,   250,   254,
     258,   262,   266,   272,   276,   280,   284,   288,   292,   296,
     300,   304,   308,   312,   316,   320,   324,   329,   333,   349,
     364,   376,   382,   390,   396,   403,   408,   413,   421,   430,
     440,   450,   460,   470,   480,   489,   498,   504,   510,   516,
     522,   527,   531,   538,   544,   554,   564,   577,   580,   585,
     589,   592,   597,   603,   617,   628,   631,   635,   640,   645,
     652,   659,   665,   674,   677,   685,   693,   701,   713
  };

  // Report on the debug stream that the rule yyrule is going to be reduced.
  private void yy_reduce_print (int yyrule, YYStack yystack)
  {
    if (yydebug == 0)
      return;

    int yylno = yyrline_[yyrule];
    int yynrhs = yyr2_[yyrule];
    /* Print the symbols being reduced, and their result.  */
    yycdebug ("Reducing stack by rule " + (yyrule - 1)
	      + " (line " + yylno + "), ");

    /* The symbols being reduced.  */
    for (int yyi = 0; yyi < yynrhs; yyi++)
      yy_symbol_print ("   $" + (yyi + 1) + " =",
		       yyrhs_[yyprhs_[yyrule] + yyi],
		       ((yystack.valueAt (yynrhs-(yyi + 1)))), 
		       yystack.locationAt (yynrhs-(yyi + 1)));
  }

  /* YYTRANSLATE(YYLEX) -- Bison symbol number corresponding to YYLEX.  */
  private static final byte yytranslate_table_[] =
  {
         0,     2,     2,     2,     2,     2,     2,     2,     2,     2,
      55,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,    60,     2,     2,    51,    61,     2,     2,
      53,    59,    44,    42,    63,    43,     2,    45,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,    47,    56,
       2,     2,     2,    36,    52,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,    54,     2,    62,    50,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,    57,     2,    58,    38,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     1,     2,     3,     4,
       5,     6,     7,     8,     9,    10,    11,    12,    13,    14,
      15,    16,    17,    18,    19,    20,    21,    22,    23,    24,
      25,    26,    27,    28,    29,    30,    31,    32,    33,    34,
      35,    37,    39,    40,    41,    46,    48,    49
  };

  private static final byte yytranslate_ (int t)
  {
    if (t >= 0 && t <= yyuser_token_number_max_)
      return yytranslate_table_[t];
    else
      return yyundef_token_;
  }

  private static final int yylast_ = 2544;
  private static final int yynnts_ = 14;
  private static final int yyempty_ = -2;
  private static final int yyfinal_ = 51;
  private static final int yyterror_ = 1;
  private static final int yyerrcode_ = 256;
  private static final int yyntokens_ = 64;

  private static final int yyuser_token_number_max_ = 297;
  private static final int yyundef_token_ = 2;

/* User implementation code.  */
/* Unqualified %code blocks.  */

/* Line 875 of lalr1.java  */
/* Line 56 of "RBisonParser.y"  */

	/* Variables de estado en el lexer y el parser*/
	public static int	EatLines = 0;
	public static int	GenerateCode = 0;
	public static int	EndOfFile = 0;
	public static int	xxcharcount, xxcharsave;
	public static int	xxlineno, xxbyteno, xxcolno,  xxlinesave, xxbytesave, xxcolsave;
	
	public static final char LBRACE	= '{';
	public static final char RBRACE	= '}';
	
	public static final int CONTEXTSTACK_SIZE = 50;
	public static int savedToken, contextpIndex = 0, contextStackIndex = 0;
	//public static SEXP	savedLval;
	public static char	contextstack[]= new char[CONTEXTSTACK_SIZE], contextp[];
	public static final int PUSHBACK_BUFSIZE = 16;
	public static int[] pushback = new int[PUSHBACK_BUFSIZE];
	public static int npush = 0;
	public static int prevpos = 0;
	public static int[] prevlines = new int[PUSHBACK_BUFSIZE];
	public static int[] prevcols = new int[PUSHBACK_BUFSIZE];
	public static int[] prevbytes = new int[PUSHBACK_BUFSIZE];
	
	public static final int MAXFUNSIZE = 131072;
	public static final int MAXNEST = 265;	
	static char [] functionSource = new char[MAXFUNSIZE];
	static char[] FunctionStart = new char[MAXNEST];
	static char[] sourcePtr;
	static int sourcePtrIndex;
	static int functionLevel = 0;
	static int keepSource;


/* Line 875 of lalr1.java  */
/* Line 89 of "RBisonParser.y"  */


	/**
	* Nodo raiz del AST construida durante el reconocimiento sintactico
	*/
	protected RModuleDeclaration root = new RModuleDeclaration(0);
	
	public RModuleDeclaration getASTRoot(){
		return this.root;
	}
	
	/**
	* Determina si el analizador lexico debe ignorar los saltos de linea
	*/
	public static boolean SKIP_NEW_LINE = false;
	



/* Line 875 of lalr1.java  */
/* Line 2955 of "RBisonParser.java"  */

}


/* Line 879 of lalr1.java  */
/* Line 717 of "RBisonParser.y"  */


