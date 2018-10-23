// https://searchcode.com/api/result/53215583/

package tiger.ast;

import org.cocuyo.parsing.*;
//open-imports
//close-imports

public class TigerASTBuilder extends ASTBuilder
{
	public void reduce(int aIndex)
	{
		Object _ast = null;
		switch (aIndex)
		{
			/*
			Expr -> (OrExpr "=" OrExpr => AsignExpr)
			*/
			case 0 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTAsignExpr((ASTExpr)_alt[0], (ASTExpr)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			Expr -> OrExpr
			*/
			/*case 1 reduce an abtract AST*//*
			OrExpr -> (OrExpr "|" AndExpr => BinaryExpr)
			*/
			case 2 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTBinaryExpr((ASTExpr)_alt[0], (ASTExpr)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			OrExpr -> AndExpr
			*/
			/*case 3 reduce an abtract AST*//*
			AndExpr -> (AndExpr "&" AddExpr => BinaryExpr)
			*/
			case 4 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTBinaryExpr((ASTExpr)_alt[0], (ASTExpr)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			AndExpr -> AddExpr
			*/
			/*case 5 reduce an abtract AST*//*
			AddExpr -> (AddExpr "+" MulExpr => BinaryExpr)
			*/
			case 6 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTBinaryExpr((ASTExpr)_alt[0], (ASTExpr)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			AddExpr -> (AddExpr "-" MulExpr => BinaryExpr)
			*/
			case 7 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTBinaryExpr((ASTExpr)_alt[0], (ASTExpr)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			AddExpr -> MulExpr
			*/
			/*case 8 reduce an abtract AST*//*
			MulExpr -> (MulExpr "*" AccesExpr => BinaryExpr)
			*/
			case 9 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTBinaryExpr((ASTExpr)_alt[0], (ASTExpr)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			MulExpr -> (MulExpr "/" AccesExpr => BinaryExpr)
			*/
			case 10 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTBinaryExpr((ASTExpr)_alt[0], (ASTExpr)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			MulExpr -> AccesExpr
			*/
			/*case 11 reduce an abtract AST*//*
			AccesExpr -> (AccesExpr "." Id => FieldAcces)
			*/
			case 12 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTFieldAcces((ASTExpr)_alt[0], (ASTId)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			AccesExpr -> (AccesExpr "[" Expr "]" => ArrayAcces)
			*/
			case 13 : 
			{
				Object[] _alt = pop(4);
				_ast = new ASTArrayAcces((ASTExpr)_alt[0], (ASTExpr)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			AccesExpr -> Value
			*/
			/*case 14 reduce an abtract AST*//*
			Value -> (number => Number)
			*/
			case 15 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTNumber();
				pushAST(_ast);
				break;
			}
			/*
			Value -> Id
			*/
			/*case 16 reduce an abtract AST*//*
			Value -> ("(" ExprList ")" => Secuence)
			*/
			case 17 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTSecuence((ASTExprList)_alt[1]);
				pushAST(_ast);
				break;
			}
			/*
			ExprList -> Expr ";" ExprList
			*/
			case 18 : 
			{
				Object[] _alt = pop(3);
				_ast = buildList_ASTExprList((ASTExpr)_alt[0], (ASTExprList)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			ExprList -> Expr
			*/
			case 19 : 
			{
				Object[] _alt = pop(1);
				_ast = buildList_ASTExprList((ASTExpr)_alt[0]);
				pushAST(_ast);
				break;
			}
			/*
			Id -> ident
			*/
			case 20 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTId();
				pushAST(_ast);
				break;
			}
			
		}
		
	}
	public ASTExprList buildList_ASTExprList(ASTExpr aElem, ASTExprList aList)
	{
		aList.add(0, aElem);
		return aList;
	}
	public ASTExprList buildList_ASTExprList(ASTExpr aElem)
	{
		ASTExprList _list = new ASTExprList();
		_list.add(aElem);
		return _list;
	}
	public ASTExprList buildList_ASTExprList()
	{
		return new ASTExprList();
	}
	//open-members
	//close-members
}
