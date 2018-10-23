// https://searchcode.com/api/result/53215844/

package org.cocuyo.dsl.lexer.ast;

import org.cocuyo.dsl.lexer.syntax.LexerSymbol;
import org.cocuyo.parsing.ASTBuilder;
import org.cocuyo.parsing.IToken;
import org.cocuyo.parsing.cup.CupToken;

//close-imports

public class LexerASTBuilder extends ASTBuilder
{
	public void reduce(int aIndex)
	{
		Object _ast = null;
		switch (aIndex)
		{
			/*
			Unit -> ("package" Name => PackageDef) LexerDefList
			*/
			case 0 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTUnit(new ASTPackageDef((ASTName)_alt[1]), (ASTLexerDefList)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			Unit -> LexerDefList
			*/
			case 1 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTUnit((ASTLexerDefList)_alt[0]);
				pushAST(_ast);
				break;
			}
			/*
			LexerDef -> "lexer" id IncludeList RegexList TransitionSetList "end"
			*/
			case 2 : 
			{
				Object[] _alt = pop(6);
				_ast = new ASTLexerDef(build_IToken(_alt[1]), (ASTIncludeList)_alt[2], (ASTRegexList)_alt[3], (ASTTransitionSetList)_alt[4]);
				pushAST(_ast);
				break;
			}
			/*
			Include -> "include" id "from" Name
			*/
			case 3 : 
			{
				Object[] _alt = pop(4);
				_ast = new ASTInclude(build_IToken(_alt[1]), (ASTName)_alt[3]);
				pushAST(_ast);
				break;
			}
			/*
			Include -> "include" Name
			*/
			case 4 : 
			{
				Object[] _alt = pop(2);
				_ast = new ASTInclude((ASTName)_alt[1]);
				pushAST(_ast);
				break;
			}
			/*
			Regex -> id "?=" line
			*/
			case 5 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTRegex(build_IToken(_alt[0]), build_IToken(_alt[2]));
				pushAST(_ast);
				break;
			}
			/*
			TransitionSet -> "on" id "yield" id "->" id TransitionList
			*/
			case 6 : 
			{
				Object[] _alt = pop(7);
				_ast = new ASTTransitionSet(build_IToken(_alt[1]), build_IToken(_alt[3]), build_IToken(_alt[5]), (ASTTransitionList)_alt[6]);
				pushAST(_ast);
				break;
			}
			/*
			TransitionSet -> "on" id "yield" id TransitionList
			*/
			case 7 : 
			{
				Object[] _alt = pop(5);
				_ast = new ASTTransitionSet(build_IToken(_alt[1]), build_IToken(_alt[3]), (ASTTransitionList)_alt[4]);
				pushAST(_ast);
				break;
			}
			/*
			TransitionSet -> "on" id "->" id TransitionList
			*/
			case 8 : 
			{
				Object[] _alt = pop(5);
				_ast = new ASTTransitionSet(build_IToken(_alt[1]), build_IToken(_alt[3]), (ASTTransitionList)_alt[4]);
				pushAST(_ast);
				break;
			}
			/*
			TransitionSet -> "on" id TransitionList
			*/
			case 9 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTTransitionSet(build_IToken(_alt[1]), (ASTTransitionList)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			Transition -> Pattern "yield" id "->" id
			*/
			case 10 : 
			{
				Object[] _alt = pop(5);
				_ast = new ASTTransition((ASTPattern)_alt[0], build_IToken(_alt[2]), build_IToken(_alt[4]));
				pushAST(_ast);
				break;
			}
			/*
			Transition -> Pattern "yield" id
			*/
			case 11 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTTransition((ASTPattern)_alt[0], build_IToken(_alt[2]));
				pushAST(_ast);
				break;
			}
			/*
			Transition -> Pattern "->" id
			*/
			case 12 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTTransition((ASTPattern)_alt[0], build_IToken(_alt[2]));
				pushAST(_ast);
				break;
			}
			/*
			Transition -> Pattern
			*/
			case 13 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTTransition((ASTPattern)_alt[0]);
				pushAST(_ast);
				break;
			}
			/*
			Pattern -> id
			*/
			case 14 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTPattern(build_IToken(_alt[0]));
				pushAST(_ast);
				break;
			}
			/*
			Pattern -> string_literal
			*/
			case 15 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTPattern(build_IToken(_alt[0]));
				pushAST(_ast);
				break;
			}
			/*
			Name -> id "," Name
			*/
			case 16 : 
			{
				Object[] _alt = pop(3);
				_ast = buildList_ASTName(build_IToken(_alt[0]), (ASTName)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			Name -> id
			*/
			case 17 : 
			{
				Object[] _alt = pop(1);
				_ast = buildList_ASTName(build_IToken(_alt[0]));
				pushAST(_ast);
				break;
			}
			/*
			LexerDefList -> LexerDef LexerDefList
			*/
			case 18 : 
			{
				Object[] _alt = pop(2);
				_ast = buildList_ASTLexerDefList((ASTLexerDef)_alt[0], (ASTLexerDefList)_alt[1]);
				pushAST(_ast);
				break;
			}
			/*
			LexerDefList -> 
			*/
			case 19 : 
			{
				Object[] _alt = pop(0);
				_ast = buildList_ASTLexerDefList();
				pushAST(_ast);
				break;
			}
			/*
			IncludeList -> Include IncludeList
			*/
			case 20 : 
			{
				Object[] _alt = pop(2);
				_ast = buildList_ASTIncludeList((ASTInclude)_alt[0], (ASTIncludeList)_alt[1]);
				pushAST(_ast);
				break;
			}
			/*
			IncludeList -> 
			*/
			case 21 : 
			{
				Object[] _alt = pop(0);
				_ast = buildList_ASTIncludeList();
				pushAST(_ast);
				break;
			}
			/*
			RegexList -> Regex RegexList
			*/
			case 22 : 
			{
				Object[] _alt = pop(2);
				_ast = buildList_ASTRegexList((ASTRegex)_alt[0], (ASTRegexList)_alt[1]);
				pushAST(_ast);
				break;
			}
			/*
			RegexList -> 
			*/
			case 23 : 
			{
				Object[] _alt = pop(0);
				_ast = buildList_ASTRegexList();
				pushAST(_ast);
				break;
			}
			/*
			TransitionSetList -> TransitionSet TransitionSetList
			*/
			case 24 : 
			{
				Object[] _alt = pop(2);
				_ast = buildList_ASTTransitionSetList((ASTTransitionSet)_alt[0], (ASTTransitionSetList)_alt[1]);
				pushAST(_ast);
				break;
			}
			/*
			TransitionSetList -> 
			*/
			case 25 : 
			{
				Object[] _alt = pop(0);
				_ast = buildList_ASTTransitionSetList();
				pushAST(_ast);
				break;
			}
			/*
			TransitionList -> Transition TransitionList
			*/
			case 26 : 
			{
				Object[] _alt = pop(2);
				_ast = buildList_ASTTransitionList((ASTTransition)_alt[0], (ASTTransitionList)_alt[1]);
				pushAST(_ast);
				break;
			}
			/*
			TransitionList -> Transition
			*/
			case 27 : 
			{
				Object[] _alt = pop(1);
				_ast = buildList_ASTTransitionList((ASTTransition)_alt[0]);
				pushAST(_ast);
				break;
			}
			
		}
		
	}
	public ASTName buildList_ASTName(IToken aElem, ASTName aList)
	{
		aList.add(0, aElem);
		return aList;
	}
	public ASTName buildList_ASTName(IToken aElem)
	{
		ASTName _list = new ASTName();
		_list.add(aElem);
		return _list;
	}
	public ASTName buildList_ASTName()
	{
		return new ASTName();
	}
	public ASTLexerDefList buildList_ASTLexerDefList(ASTLexerDef aElem, ASTLexerDefList aList)
	{
		aList.add(0, aElem);
		return aList;
	}
	public ASTLexerDefList buildList_ASTLexerDefList(ASTLexerDef aElem)
	{
		ASTLexerDefList _list = new ASTLexerDefList();
		_list.add(aElem);
		return _list;
	}
	public ASTLexerDefList buildList_ASTLexerDefList()
	{
		return new ASTLexerDefList();
	}
	public ASTIncludeList buildList_ASTIncludeList(ASTInclude aElem, ASTIncludeList aList)
	{
		aList.add(0, aElem);
		return aList;
	}
	public ASTIncludeList buildList_ASTIncludeList(ASTInclude aElem)
	{
		ASTIncludeList _list = new ASTIncludeList();
		_list.add(aElem);
		return _list;
	}
	public ASTIncludeList buildList_ASTIncludeList()
	{
		return new ASTIncludeList();
	}
	public ASTRegexList buildList_ASTRegexList(ASTRegex aElem, ASTRegexList aList)
	{
		aList.add(0, aElem);
		return aList;
	}
	public ASTRegexList buildList_ASTRegexList(ASTRegex aElem)
	{
		ASTRegexList _list = new ASTRegexList();
		_list.add(aElem);
		return _list;
	}
	public ASTRegexList buildList_ASTRegexList()
	{
		return new ASTRegexList();
	}
	public ASTTransitionSetList buildList_ASTTransitionSetList(ASTTransitionSet aElem, ASTTransitionSetList aList)
	{
		aList.add(0, aElem);
		return aList;
	}
	public ASTTransitionSetList buildList_ASTTransitionSetList(ASTTransitionSet aElem)
	{
		ASTTransitionSetList _list = new ASTTransitionSetList();
		_list.add(aElem);
		return _list;
	}
	public ASTTransitionSetList buildList_ASTTransitionSetList()
	{
		return new ASTTransitionSetList();
	}
	public ASTTransitionList buildList_ASTTransitionList(ASTTransition aElem, ASTTransitionList aList)
	{
		aList.add(0, aElem);
		return aList;
	}
	public ASTTransitionList buildList_ASTTransitionList(ASTTransition aElem)
	{
		ASTTransitionList _list = new ASTTransitionList();
		_list.add(aElem);
		return _list;
	}
	public ASTTransitionList buildList_ASTTransitionList()
	{
		return new ASTTransitionList();
	}
	public IToken build_IToken(Object aToken)
	{
		//open-build-IToken
		CupToken _t = (CupToken) aToken;
		if (_t.getID() == LexerSymbol.STRING_LITERAL)
			_t.setText(_t.getText().substring(1, _t.getText().length() - 1));
		return _t;
		//close-build-IToken
	}
	//open-members
	//close-members
}
