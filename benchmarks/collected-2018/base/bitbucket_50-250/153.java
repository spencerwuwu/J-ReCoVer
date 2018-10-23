// https://searchcode.com/api/result/53216027/

package org.cocuyo.dsl._native.ast;

import org.cocuyo.parsing.ASTBuilder;
import org.cocuyo.parsing.IToken;

public class NativeASTBuilder extends ASTBuilder
{
	public void reduce(int aIndex)
	{
		Object _ast = null;
		switch (aIndex)
		{
			/*
			CompileUnit -> ("package" Name => PackageDef) "native" Id "is" text
			*/
			case 0 : 
			{
				Object[] _alt = pop(6);
				_ast = new ASTCompileUnit(new ASTPackageDef((ASTName)_alt[1]), (ASTId)_alt[3], build_IToken(_alt[5]));
				pushAST(_ast);
				break;
			}
			/*
			CompileUnit -> ("package" Name => PackageDef)
			*/
			case 1 : 
			{
				Object[] _alt = pop(2);
				_ast = new ASTCompileUnit(new ASTPackageDef((ASTName)_alt[1]));
				pushAST(_ast);
				break;
			}
			/*
			CompileUnit -> "native" Id "is" text
			*/
			case 2 : 
			{
				Object[] _alt = pop(4);
				_ast = new ASTCompileUnit((ASTId)_alt[1], build_IToken(_alt[3]));
				pushAST(_ast);
				break;
			}
			/*
			CompileUnit -> 
			*/
			case 3 : 
			{
				Object[] _alt = pop(0);
				_ast = new ASTCompileUnit();
				pushAST(_ast);
				break;
			}
			/*
			Name -> Id "." Name
			*/
			case 4 : 
			{
				Object[] _alt = pop(3);
				_ast = buildList_ASTName((ASTId)_alt[0], (ASTName)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			Name -> Id
			*/
			case 5 : 
			{
				Object[] _alt = pop(1);
				_ast = buildList_ASTName((ASTId)_alt[0]);
				pushAST(_ast);
				break;
			}
			/*
			Id -> id
			*/
			case 6 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTId(build_IToken(_alt[0]));
				pushAST(_ast);
				break;
			}
			
		}
		
	}
	public ASTName buildList_ASTName(ASTId aElem, ASTName aList)
	{
		aList.add(0, aElem);
		return aList;
	}
	public ASTName buildList_ASTName(ASTId aElem)
	{
		ASTName _list = new ASTName();
		_list.add(aElem);
		return _list;
	}
	public ASTName buildList_ASTName()
	{
		return new ASTName();
	}
	public IToken build_IToken(Object aToken)
	{
		//open-build-IToken
		return (IToken) aToken;
		//close-build-IToken
	}
	//open-members
	//close-members
}
