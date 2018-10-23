// https://searchcode.com/api/result/53215980/

package org.cocuyo.dsl.entity.ast;

import org.cocuyo.parsing.*;
//open-imports
//close-imports

public class EntityASTBuilder extends ASTBuilder
{
	public void reduce(int aIndex)
	{
		Object _ast = null;
		switch (aIndex)
		{
			/*
			EntityCompileUnit -> ("package" Name => PackageDef) ("import" Name => Import) ModelList
			*/
			case 0 : 
			{
				Object[] _alt = pop(5);
				_ast = new ASTEntityCompileUnit(new ASTPackageDef((ASTName)_alt[1]), new ASTImport((ASTName)_alt[3]), (ASTModelList)_alt[4]);
				pushAST(_ast);
				break;
			}
			/*
			EntityCompileUnit -> ("package" Name => PackageDef) ModelList
			*/
			case 1 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTEntityCompileUnit(new ASTPackageDef((ASTName)_alt[1]), (ASTModelList)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			EntityCompileUnit -> ("import" Name => Import) ModelList
			*/
			case 2 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTEntityCompileUnit(new ASTImport((ASTName)_alt[1]), (ASTModelList)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			EntityCompileUnit -> ModelList
			*/
			case 3 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTEntityCompileUnit((ASTModelList)_alt[0]);
				pushAST(_ast);
				break;
			}
			/*
			Model -> "model" Id IncludeList ModelElementList "end"
			*/
			case 4 : 
			{
				Object[] _alt = pop(5);
				_ast = new ASTModel((ASTId)_alt[1], (ASTIncludeList)_alt[2], (ASTModelElementList)_alt[3]);
				pushAST(_ast);
				break;
			}
			/*
			ModelElement -> Entity
			*/
			/*case 5 reduce an abtract AST*//*
			ModelElement -> Type
			*/
			/*case 6 reduce an abtract AST*//*
			Entity -> "entity" Id "is" Name EntityElementList "end"
			*/
			case 7 : 
			{
				Object[] _alt = pop(6);
				_ast = new ASTEntity((ASTId)_alt[1], (ASTName)_alt[3], (ASTEntityElementList)_alt[4]);
				pushAST(_ast);
				break;
			}
			/*
			Entity -> "entity" Id EntityElementList "end"
			*/
			case 8 : 
			{
				Object[] _alt = pop(4);
				_ast = new ASTEntity((ASTId)_alt[1], (ASTEntityElementList)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			EntityElement -> (("*" => Modifier) Id ":" FieldType => Field)
			*/
			case 9 : 
			{
				Object[] _alt = pop(4);
				_ast = new ASTField(new ASTModifier(), (ASTId)_alt[1], (ASTFieldType)_alt[3]);
				pushAST(_ast);
				break;
			}
			/*
			EntityElement -> (Id ":" FieldType => Field)
			*/
			case 10 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTField((ASTId)_alt[0], (ASTFieldType)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			FieldType -> Name Cardinality "(" TypeOptionList ")"
			*/
			case 11 : 
			{
				Object[] _alt = pop(5);
				_ast = new ASTFieldType((ASTName)_alt[0], (ASTCardinality)_alt[1], (ASTTypeOptionList)_alt[3]);
				pushAST(_ast);
				break;
			}
			/*
			FieldType -> Name Cardinality
			*/
			case 12 : 
			{
				Object[] _alt = pop(2);
				_ast = new ASTFieldType((ASTName)_alt[0], (ASTCardinality)_alt[1]);
				pushAST(_ast);
				break;
			}
			/*
			FieldType -> Name "(" TypeOptionList ")"
			*/
			case 13 : 
			{
				Object[] _alt = pop(4);
				_ast = new ASTFieldType((ASTName)_alt[0], (ASTTypeOptionList)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			FieldType -> Name
			*/
			case 14 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTFieldType((ASTName)_alt[0]);
				pushAST(_ast);
				break;
			}
			/*
			Cardinality -> "[]"
			*/
			case 15 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTCardinality();
				pushAST(_ast);
				break;
			}
			/*
			TypeOptionList -> Option "," TypeOptionList
			*/
			case 16 : 
			{
				Object[] _alt = pop(3);
				_ast = buildList_ASTTypeOptionList((ASTOption)_alt[0], (ASTTypeOptionList)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			TypeOptionList -> Option
			*/
			case 17 : 
			{
				Object[] _alt = pop(1);
				_ast = buildList_ASTTypeOptionList((ASTOption)_alt[0]);
				pushAST(_ast);
				break;
			}
			/*
			Option -> Id "=" Literal
			*/
			case 18 : 
			{
				Object[] _alt = pop(3);
				_ast = new ASTOption((ASTId)_alt[0], (ASTLiteral)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			Literal -> (string => StringLiteral)
			*/
			case 19 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTStringLiteral(build_IToken(_alt[0]));
				pushAST(_ast);
				break;
			}
			/*
			Literal -> (number => NumberLiteral)
			*/
			case 20 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTNumberLiteral(build_IToken(_alt[0]));
				pushAST(_ast);
				break;
			}
			/*
			Literal -> (symbol => SymbolLiteral)
			*/
			case 21 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTSymbolLiteral(build_IToken(_alt[0]));
				pushAST(_ast);
				break;
			}
			/*
			Type -> "type" Id "=" FieldType
			*/
			case 22 : 
			{
				Object[] _alt = pop(4);
				_ast = new ASTType((ASTId)_alt[1], (ASTFieldType)_alt[3]);
				pushAST(_ast);
				break;
			}
			/*
			Type -> "type" Id "=" (string => StringLiteral)
			*/
			case 23 : 
			{
				Object[] _alt = pop(4);
				_ast = new ASTType((ASTId)_alt[1], new ASTStringLiteral(build_IToken(_alt[3])));
				pushAST(_ast);
				break;
			}
			/*
			Name -> Id "." Name
			*/
			case 24 : 
			{
				Object[] _alt = pop(3);
				_ast = buildList_ASTName((ASTId)_alt[0], (ASTName)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			Name -> Id
			*/
			case 25 : 
			{
				Object[] _alt = pop(1);
				_ast = buildList_ASTName((ASTId)_alt[0]);
				pushAST(_ast);
				break;
			}
			/*
			Id -> id
			*/
			case 26 : 
			{
				Object[] _alt = pop(1);
				_ast = new ASTId(build_IToken(_alt[0]));
				pushAST(_ast);
				break;
			}
			/*
			ModelList -> Model ModelList
			*/
			case 27 : 
			{
				Object[] _alt = pop(2);
				_ast = buildList_ASTModelList((ASTModel)_alt[0], (ASTModelList)_alt[1]);
				pushAST(_ast);
				break;
			}
			/*
			ModelList -> 
			*/
			case 28 : 
			{
				Object[] _alt = pop(0);
				_ast = buildList_ASTModelList();
				pushAST(_ast);
				break;
			}
			/*
			IncludeList -> ("include" Name => Include) IncludeList
			*/
			case 29 : 
			{
				Object[] _alt = pop(3);
				_ast = buildList_ASTIncludeList(new ASTInclude((ASTName)_alt[1]), (ASTIncludeList)_alt[2]);
				pushAST(_ast);
				break;
			}
			/*
			IncludeList -> 
			*/
			case 30 : 
			{
				Object[] _alt = pop(0);
				_ast = buildList_ASTIncludeList();
				pushAST(_ast);
				break;
			}
			/*
			ModelElementList -> ModelElement ModelElementList
			*/
			case 31 : 
			{
				Object[] _alt = pop(2);
				_ast = buildList_ASTModelElementList((ASTModelElement)_alt[0], (ASTModelElementList)_alt[1]);
				pushAST(_ast);
				break;
			}
			/*
			ModelElementList -> 
			*/
			case 32 : 
			{
				Object[] _alt = pop(0);
				_ast = buildList_ASTModelElementList();
				pushAST(_ast);
				break;
			}
			/*
			EntityElementList -> EntityElement EntityElementList
			*/
			case 33 : 
			{
				Object[] _alt = pop(2);
				_ast = buildList_ASTEntityElementList((ASTEntityElement)_alt[0], (ASTEntityElementList)_alt[1]);
				pushAST(_ast);
				break;
			}
			/*
			EntityElementList -> 
			*/
			case 34 : 
			{
				Object[] _alt = pop(0);
				_ast = buildList_ASTEntityElementList();
				pushAST(_ast);
				break;
			}
			
		}
		
	}
	public ASTTypeOptionList buildList_ASTTypeOptionList(ASTOption aElem, ASTTypeOptionList aList)
	{
		aList.add(0, aElem);
		return aList;
	}
	public ASTTypeOptionList buildList_ASTTypeOptionList(ASTOption aElem)
	{
		ASTTypeOptionList _list = new ASTTypeOptionList();
		_list.add(aElem);
		return _list;
	}
	public ASTTypeOptionList buildList_ASTTypeOptionList()
	{
		return new ASTTypeOptionList();
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
	public ASTModelList buildList_ASTModelList(ASTModel aElem, ASTModelList aList)
	{
		aList.add(0, aElem);
		return aList;
	}
	public ASTModelList buildList_ASTModelList(ASTModel aElem)
	{
		ASTModelList _list = new ASTModelList();
		_list.add(aElem);
		return _list;
	}
	public ASTModelList buildList_ASTModelList()
	{
		return new ASTModelList();
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
	public ASTModelElementList buildList_ASTModelElementList(ASTModelElement aElem, ASTModelElementList aList)
	{
		aList.add(0, aElem);
		return aList;
	}
	public ASTModelElementList buildList_ASTModelElementList(ASTModelElement aElem)
	{
		ASTModelElementList _list = new ASTModelElementList();
		_list.add(aElem);
		return _list;
	}
	public ASTModelElementList buildList_ASTModelElementList()
	{
		return new ASTModelElementList();
	}
	public ASTEntityElementList buildList_ASTEntityElementList(ASTEntityElement aElem, ASTEntityElementList aList)
	{
		aList.add(0, aElem);
		return aList;
	}
	public ASTEntityElementList buildList_ASTEntityElementList(ASTEntityElement aElem)
	{
		ASTEntityElementList _list = new ASTEntityElementList();
		_list.add(aElem);
		return _list;
	}
	public ASTEntityElementList buildList_ASTEntityElementList()
	{
		return new ASTEntityElementList();
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
