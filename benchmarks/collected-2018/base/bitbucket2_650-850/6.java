// https://searchcode.com/api/result/53215776/

package org.cocuyo.dsl.textgenerator.ast;
import java.util.ArrayList;

import org.cocuyo.parsing.ASTBuilder;
import org.cocuyo.parsing.IToken;

public class TextGeneratorGrammarASTBuilder extends ASTBuilder
//open-inheritance//close-inheritance
{
	public TextGeneratorGrammarASTBuilder()
	{
		super();
	}
	@Override
	public void reduce(int ruleIndex)
	{
		Object ast = null;
		
		switch(ruleIndex)
		{
			//(0) <CompileUnit> ::= <PackageDef> <UnitElementList>
			case 0 :
			{
				Object[] _alt = pop(2);
				ast = new CompileUnit((PackageDef) _alt[0], buildList_Of_UnitElement(_alt[1]));
				pushAST(ast);
				break;
			}
			//(1) <PackageDef> ::= "package" <Name>
			case 1 :
			{
				Object[] _alt = pop(2);
				ast = new PackageDef((Name) _alt[1]);
				pushAST(ast);
				break;
			}
			//(2) <PackageDef> ::= 
			case 2 :
			{
				Object[] _alt = pop(0);
				ast = new PackageDef();
				pushAST(ast);
				break;
			}
			//(3) <UnitElementList> ::= <UnitElement> <UnitElementList>
			case 3 :
			{
				Object[] _alt = pop(2);
				ast = buildRightList_Of_UnitElement((UnitElement) _alt[0], buildList_Of_UnitElement(_alt[1]));
				pushAST(ast);
				break;
			}
			//(4) <UnitElementList> ::= <UnitElement>
			case 4 :
			{
				Object[] _alt = pop(1);
				ast = buildRightList_Of_UnitElement((UnitElement) _alt[0]);
				pushAST(ast);
				break;
			}
			//(5) <UnitElement> ::= <Generator>
			case 5 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/(Generator) _alt[0];
				pushAST(ast);
				break;
			}
			//(6) <UnitElement> ::= <Import>
			case 6 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/(Import) _alt[0];
				pushAST(ast);
				break;
			}
			//(7) <Import> ::= "import" <Name>
			case 7 :
			{
				Object[] _alt = pop(2);
				ast = new Import((Name) _alt[1]);
				pushAST(ast);
				break;
			}
			//(8) <Generator> ::= "gen" <Id> "is" <NameList> <GeneratorMemberList> "end"
			case 8 :
			{
				Object[] _alt = pop(6);
				ast = new Generator((Id) _alt[1], buildList_Of_Name(_alt[3]), buildList_Of_GeneratorMember(_alt[4]));
				pushAST(ast);
				break;
			}
			//(9) <Generator> ::= "gen" <Id> <GeneratorMemberList> "end"
			case 9 :
			{
				Object[] _alt = pop(4);
				ast = new Generator((Id) _alt[1], buildList_Of_GeneratorMember(_alt[2]));
				pushAST(ast);
				break;
			}
			//(10) <GeneratorMemberList> ::= <GeneratorMember> <GeneratorMemberList>
			case 10 :
			{
				Object[] _alt = pop(2);
				ast = buildRightList_Of_GeneratorMember((GeneratorMember) _alt[0], buildList_Of_GeneratorMember(_alt[1]));
				pushAST(ast);
				break;
			}
			//(11) <GeneratorMemberList> ::= 
			case 11 :
			{
				Object[] _alt = pop(0);
				ast = buildRightList_Of_GeneratorMember();
				pushAST(ast);
				break;
			}
			//(12) <GeneratorMember> ::= <AliasDecl>
			case 12 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/(AliasDecl) _alt[0];
				pushAST(ast);
				break;
			}
			//(13) <GeneratorMember> ::= <FuncDecl>
			case 13 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/(GeneratorMember) _alt[0];
				pushAST(ast);
				break;
			}
			//(14) <GeneratorMember> ::= <AspectDecl>
			case 14 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/(AspectDecl) _alt[0];
				pushAST(ast);
				break;
			}
			//(15) <AliasDecl> ::= <Id> "." <Id> "as" <Id>
			case 15 :
			{
				Object[] _alt = pop(5);
				ast = new AliasDecl((Id) _alt[0], (Id) _alt[2], (Id) _alt[4]);
				pushAST(ast);
				break;
			}
			//(16) <AspectDecl> ::= "on" <Name> "do" <Id>
			case 16 :
			{
				Object[] _alt = pop(4);
				ast = new AspectDecl((Name) _alt[1], (Id) _alt[3]);
				pushAST(ast);
				break;
			}
			//(17) <FuncDecl> ::= <StandardFuncDecl ~ "fun" <Id> "(" <FuncFormalArgList> ")" <CodeList> "end">
			case 17 :
			{
				Object[] _alt = pop(7);
				ast = /*Nothing*/new StandardFuncDecl((Id) _alt[1], buildList_Of_FormalArg(_alt[3]), buildList_Of_Code(_alt[5]));
				pushAST(ast);
				break;
			}
			//(18) <FuncDecl> ::= <BuiltinFuncDecl ~ "fun" <Id> "(" <FuncFormalArgList> ")" "return" <Name>>
			case 18 :
			{
				Object[] _alt = pop(7);
				ast = /*Nothing*/new BuiltinFuncDecl((Id) _alt[1], buildList_Of_FormalArg(_alt[3]), (Name) _alt[6]);
				pushAST(ast);
				break;
			}
			//(19) <FuncDecl> ::= <PropertyDecl ~ <Id> "=" <Code>>
			case 19 :
			{
				Object[] _alt = pop(3);
				ast = /*Nothing*/new PropertyDecl((Id) _alt[0], (Code) _alt[2]);
				pushAST(ast);
				break;
			}
			//(20) <FuncFormalArgList> ::= <FormalArg> "," <FuncFormalArgList>
			case 20 :
			{
				Object[] _alt = pop(3);
				ast = buildRightList_Of_FormalArg((FormalArg) _alt[0], buildList_Of_FormalArg(_alt[2]));
				pushAST(ast);
				break;
			}
			//(21) <FuncFormalArgList> ::= <FormalArg>
			case 21 :
			{
				Object[] _alt = pop(1);
				ast = buildRightList_Of_FormalArg((FormalArg) _alt[0]);
				pushAST(ast);
				break;
			}
			//(22) <FuncFormalArgList> ::= 
			case 22 :
			{
				Object[] _alt = pop(0);
				ast = buildRightList_Of_FormalArg();
				pushAST(ast);
				break;
			}
			//(23) <FormalArg> ::= <Id>
			case 23 :
			{
				Object[] _alt = pop(1);
				ast = new FormalArg((Id) _alt[0]);
				pushAST(ast);
				break;
			}
			//(24) <FormalArg> ::= <Id> "=" <Code>
			case 24 :
			{
				Object[] _alt = pop(3);
				ast = new FormalArg((Id) _alt[0], (Code) _alt[2]);
				pushAST(ast);
				break;
			}
			//(25) <Code> ::= <CodeBlock ~ "{" <CodeList> "}">
			case 25 :
			{
				Object[] _alt = pop(3);
				ast = /*Nothing*/new CodeBlock(buildList_Of_Code(_alt[1]));
				pushAST(ast);
				break;
			}
			//(26) <Code> ::= <ForLoop>
			case 26 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/(ForLoop) _alt[0];
				pushAST(ast);
				break;
			}
			//(27) <Code> ::= <NewLineCode ~ "$">
			case 27 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/new NewLineCode();
				pushAST(ast);
				break;
			}
			//(28) <Code> ::= <TabCode ~ "-">
			case 28 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/new TabCode();
				pushAST(ast);
				break;
			}
			//(29) <Code> ::= <IndentCode ~ "indent" <CodeList> "end">
			case 29 :
			{
				Object[] _alt = pop(3);
				ast = /*Nothing*/new IndentCode(buildList_Of_Code(_alt[1]));
				pushAST(ast);
				break;
			}
			//(30) <Code> ::= <FuncCall ~ <Code> "." <Id> <FuncCallArgs>>
			case 30 :
			{
				Object[] _alt = pop(4);
				ast = /*Nothing*/new FuncCall((Code) _alt[0], (Id) _alt[2], (FuncCallArgs) _alt[3]);
				pushAST(ast);
				break;
			}
			//(31) <Code> ::= <FuncCall ~ <Id> <FuncCallArgs>>
			case 31 :
			{
				Object[] _alt = pop(2);
				ast = /*Nothing*/new FuncCall((Id) _alt[0], (FuncCallArgs) _alt[1]);
				pushAST(ast);
				break;
			}
			//(32) <Code> ::= <FuncCall ~ "super" <FuncCallArgs>>
			case 32 :
			{
				Object[] _alt = pop(2);
				ast = /*Nothing*/new FuncCall((FuncCallArgs) _alt[1]);
				pushAST(ast);
				break;
			}
			//(33) <Code> ::= <ObjectPropertyAccess ~ <Code> "." <Id>>
			case 33 :
			{
				Object[] _alt = pop(3);
				ast = /*Nothing*/new ObjectPropertyAccess((Code) _alt[0], (Id) _alt[2]);
				pushAST(ast);
				break;
			}
			//(34) <Code> ::= <ObjectPropertyExistAndAccess ~ <Code> "." <Id> "?">
			case 34 :
			{
				Object[] _alt = pop(4);
				ast = /*Nothing*/new ObjectPropertyExistAndAccess((Code) _alt[0], (Id) _alt[2]);
				pushAST(ast);
				break;
			}
			//(35) <Code> ::= <ObjectId ~ <Id>>
			case 35 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/new ObjectId((Id) _alt[0]);
				pushAST(ast);
				break;
			}
			//(36) <Code> ::= <ObjectId ~ "super">
			case 36 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/new ObjectId();
				pushAST(ast);
				break;
			}
			//(37) <Code> ::= <StringLiteralCode ~ string_literal>
			case 37 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/new StringLiteralCode(build_StringToken(_alt[0]));
				pushAST(ast);
				break;
			}
			//(38) <Code> ::= <NumberLiteralCode ~ number_literal>
			case 38 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/new NumberLiteralCode(build_IToken(_alt[0]));
				pushAST(ast);
				break;
			}
			//(39) <Code> ::= <BooleanLiteralCode ~ "true">
			case 39 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/new BooleanLiteralCode(build_IToken(_alt[0]));
				pushAST(ast);
				break;
			}
			//(40) <Code> ::= <BooleanLiteralCode ~ "false">
			case 40 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/new BooleanLiteralCode(build_IToken(_alt[0]));
				pushAST(ast);
				break;
			}
			//(41) <Code> ::= <CodeAlias ~ <Id> "=" <Code>>
			case 41 :
			{
				Object[] _alt = pop(3);
				ast = /*Nothing*/new CodeAlias((Id) _alt[0], (Code) _alt[2]);
				pushAST(ast);
				break;
			}
			//(42) <Code> ::= <Conditional>
			case 42 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/(Conditional) _alt[0];
				pushAST(ast);
				break;
			}
			//(43) <Code> ::= <GetCode>
			case 43 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/(GetCode) _alt[0];
				pushAST(ast);
				break;
			}
			//(44) <Code> ::= <PathSeparator ~ "/">
			case 44 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/new PathSeparator();
				pushAST(ast);
				break;
			}
			//(45) <Code> ::= <UnitCode ~ "in" <CodeList> "write" <CodeList> "end">
			case 45 :
			{
				Object[] _alt = pop(5);
				ast = /*Nothing*/new UnitCode(buildList_Of_Code(_alt[1]), buildList_Of_Code(_alt[3]));
				pushAST(ast);
				break;
			}
			//(46) <Code> ::= <AspectListeners ~ "aspects" <NameList> "listen" <CodeList> "end">
			case 46 :
			{
				Object[] _alt = pop(5);
				ast = /*Nothing*/new AspectListeners(buildList_Of_Name(_alt[1]), buildList_Of_Code(_alt[3]));
				pushAST(ast);
				break;
			}
			//(47) <GetCode> ::= "get" <CodeList> ".." <CodeList> "or" <CodeList> "end"
			case 47 :
			{
				Object[] _alt = pop(7);
				ast = new GetCode(buildList_Of_Code(_alt[1]), buildList_Of_Code(_alt[3]), buildList_Of_Code(_alt[5]));
				pushAST(ast);
				break;
			}
			//(48) <GetCode> ::= "get" <CodeList> ".." <CodeList> "end"
			case 48 :
			{
				Object[] _alt = pop(5);
				ast = new GetCode(buildList_Of_Code(_alt[1]), buildList_Of_Code(_alt[3]));
				pushAST(ast);
				break;
			}
			//(49) <Conditional> ::= "if" <ConditionalPart> "end"
			case 49 :
			{
				Object[] _alt = pop(3);
				ast = new Conditional((ConditionalPart) _alt[1]);
				pushAST(ast);
				break;
			}
			//(50) <ConditionalPart> ::= <Condition> "do" <CodeList> "else" <CodeList>
			case 50 :
			{
				Object[] _alt = pop(5);
				ast = new ConditionalPart((Condition) _alt[0], buildList_Of_Code(_alt[2]), buildList_Of_Code(_alt[4]));
				pushAST(ast);
				break;
			}
			//(51) <ConditionalPart> ::= <Condition> "do" <CodeList> "elif" <ConditionalPart>
			case 51 :
			{
				Object[] _alt = pop(5);
				ast = new ConditionalPart((Condition) _alt[0], buildList_Of_Code(_alt[2]), (ConditionalPart) _alt[4]);
				pushAST(ast);
				break;
			}
			//(52) <ConditionalPart> ::= <Condition> "do" <CodeList>
			case 52 :
			{
				Object[] _alt = pop(3);
				ast = new ConditionalPart((Condition) _alt[0], buildList_Of_Code(_alt[2]));
				pushAST(ast);
				break;
			}
			//(53) <Condition> ::= <TrueCondition ~ <Code>>
			case 53 :
			{
				Object[] _alt = pop(1);
				ast = /*Nothing*/new TrueCondition((Code) _alt[0]);
				pushAST(ast);
				break;
			}
			//(54) <Condition> ::= <FalseCondition ~ "not" <Code>>
			case 54 :
			{
				Object[] _alt = pop(2);
				ast = /*Nothing*/new FalseCondition((Code) _alt[1]);
				pushAST(ast);
				break;
			}
			//(55) <ForLoop> ::= "for" <Id> "in" <CodeList> "do" <CodeList> "end"
			case 55 :
			{
				Object[] _alt = pop(7);
				ast = new ForLoop((Id) _alt[1], buildList_Of_Code(_alt[3]), buildList_Of_Code(_alt[5]));
				pushAST(ast);
				break;
			}
			//(56) <ForLoop> ::= "for" <Id> "in" <CodeList> "sep" <CodeList> "do" <CodeList> "end"
			case 56 :
			{
				Object[] _alt = pop(9);
				ast = new ForLoop((Id) _alt[1], buildList_Of_Code(_alt[3]), buildList_Of_Code(_alt[5]), buildList_Of_Code(_alt[7]));
				pushAST(ast);
				break;
			}
			//(57) <FuncCallArgs> ::= "(" <FuncCallArguments> ")" ":" <CodeList> "end"
			case 57 :
			{
				Object[] _alt = pop(6);
				ast = new FuncCallArgs((FuncCallArguments) _alt[1], buildList_Of_Code(_alt[4]));
				pushAST(ast);
				break;
			}
			//(58) <FuncCallArgs> ::= "(" <FuncCallArguments> ")"
			case 58 :
			{
				Object[] _alt = pop(3);
				ast = new FuncCallArgs((FuncCallArguments) _alt[1]);
				pushAST(ast);
				break;
			}
			//(59) <FuncCallArgs> ::= ":" <CodeList> "end"
			case 59 :
			{
				Object[] _alt = pop(3);
				ast = new FuncCallArgs(buildList_Of_Code(_alt[1]));
				pushAST(ast);
				break;
			}
			//(60) <FuncCallArguments> ::= <FuncCallArgList>
			case 60 :
			{
				Object[] _alt = pop(1);
				ast = new FuncCallArguments(buildList_Of_FuncCallArg(_alt[0]));
				pushAST(ast);
				break;
			}
			//(61) <FuncCallArgList> ::= <FuncCallArgList> "," <FuncCallArg>
			case 61 :
			{
				Object[] _alt = pop(3);
				ast = buildLeftList_Of_FuncCallArg(buildList_Of_FuncCallArg(_alt[0]), (FuncCallArg) _alt[2]);
				pushAST(ast);
				break;
			}
			//(62) <FuncCallArgList> ::= <FuncCallArg>
			case 62 :
			{
				Object[] _alt = pop(1);
				ast = buildLeftList_Of_FuncCallArg((FuncCallArg) _alt[0]);
				pushAST(ast);
				break;
			}
			//(63) <FuncCallArg> ::= <CodeList>
			case 63 :
			{
				Object[] _alt = pop(1);
				ast = new FuncCallArg(buildList_Of_Code(_alt[0]));
				pushAST(ast);
				break;
			}
			//(64) <CodeList> ::= <Code> <CodeList>
			case 64 :
			{
				Object[] _alt = pop(2);
				ast = buildRightList_Of_Code((Code) _alt[0], buildList_Of_Code(_alt[1]));
				pushAST(ast);
				break;
			}
			//(65) <CodeList> ::= 
			case 65 :
			{
				Object[] _alt = pop(0);
				ast = buildRightList_Of_Code();
				pushAST(ast);
				break;
			}
			//(66) <NameList> ::= <Name> "," <NameList>
			case 66 :
			{
				Object[] _alt = pop(3);
				ast = buildRightList_Of_Name((Name) _alt[0], buildList_Of_Name(_alt[2]));
				pushAST(ast);
				break;
			}
			//(67) <NameList> ::= <Name>
			case 67 :
			{
				Object[] _alt = pop(1);
				ast = buildRightList_Of_Name((Name) _alt[0]);
				pushAST(ast);
				break;
			}
			//(68) <Name> ::= <NameIdList>
			case 68 :
			{
				Object[] _alt = pop(1);
				ast = new Name(buildList_Of_Id(_alt[0]));
				pushAST(ast);
				break;
			}
			//(69) <NameIdList> ::= <NameIdList> "." <Id>
			case 69 :
			{
				Object[] _alt = pop(3);
				ast = buildLeftList_Of_Id(buildList_Of_Id(_alt[0]), (Id) _alt[2]);
				pushAST(ast);
				break;
			}
			//(70) <NameIdList> ::= <Id>
			case 70 :
			{
				Object[] _alt = pop(1);
				ast = buildLeftList_Of_Id((Id) _alt[0]);
				pushAST(ast);
				break;
			}
			//(71) <Id> ::= id
			case 71 :
			{
				Object[] _alt = pop(1);
				ast = new Id(build_IToken(_alt[0]));
				pushAST(ast);
				break;
			}
		}
		
		//open-reduce//close-reduce
	}
	protected StringToken build_StringToken(Object token)
	{
		//open-build-token-StringToken
	return new StringToken((IToken) token);
	//close-build-token-StringToken
	}
	protected IToken build_IToken(Object token)
	{
		//open-build-token-IToken
	return (IToken) token;
	//close-build-token-IToken
	}
	protected ArrayList<Name> buildList_Of_Name(Object ast)
	{
		return (ArrayList<Name>) ast;
	}
	protected ArrayList<UnitElement> buildList_Of_UnitElement(Object ast)
	{
		return (ArrayList<UnitElement>) ast;
	}
	protected ArrayList<FuncCallArg> buildList_Of_FuncCallArg(Object ast)
	{
		return (ArrayList<FuncCallArg>) ast;
	}
	protected ArrayList<Id> buildList_Of_Id(Object ast)
	{
		return (ArrayList<Id>) ast;
	}
	protected ArrayList<GeneratorMember> buildList_Of_GeneratorMember(Object ast)
	{
		return (ArrayList<GeneratorMember>) ast;
	}
	protected ArrayList<Code> buildList_Of_Code(Object ast)
	{
		return (ArrayList<Code>) ast;
	}
	protected ArrayList<FormalArg> buildList_Of_FormalArg(Object ast)
	{
		return (ArrayList<FormalArg>) ast;
	}
	protected ArrayList<UnitElement> buildRightList_Of_UnitElement(UnitElement elem, ArrayList<UnitElement> list)
	{
		list.add(0, elem);
		return list;
	}
	protected ArrayList<UnitElement> buildRightList_Of_UnitElement(UnitElement elem)
	{
		ArrayList<UnitElement> list = new ArrayList<UnitElement>();
		list.add(elem);
		return list;
	}protected ArrayList<UnitElement> buildRightList_Of_UnitElement()
	{
		ArrayList<UnitElement> list = new ArrayList<UnitElement>();
		return list;
	}
	protected ArrayList<Name> buildRightList_Of_Name(Name elem, ArrayList<Name> list)
	{
		list.add(0, elem);
		return list;
	}
	protected ArrayList<Name> buildRightList_Of_Name(Name elem)
	{
		ArrayList<Name> list = new ArrayList<Name>();
		list.add(elem);
		return list;
	}protected ArrayList<Name> buildRightList_Of_Name()
	{
		ArrayList<Name> list = new ArrayList<Name>();
		return list;
	}
	protected ArrayList<FormalArg> buildRightList_Of_FormalArg(FormalArg elem, ArrayList<FormalArg> list)
	{
		list.add(0, elem);
		return list;
	}
	protected ArrayList<FormalArg> buildRightList_Of_FormalArg(FormalArg elem)
	{
		ArrayList<FormalArg> list = new ArrayList<FormalArg>();
		list.add(elem);
		return list;
	}protected ArrayList<FormalArg> buildRightList_Of_FormalArg()
	{
		ArrayList<FormalArg> list = new ArrayList<FormalArg>();
		return list;
	}
	protected ArrayList<GeneratorMember> buildRightList_Of_GeneratorMember(GeneratorMember elem, ArrayList<GeneratorMember> list)
	{
		list.add(0, elem);
		return list;
	}
	protected ArrayList<GeneratorMember> buildRightList_Of_GeneratorMember(GeneratorMember elem)
	{
		ArrayList<GeneratorMember> list = new ArrayList<GeneratorMember>();
		list.add(elem);
		return list;
	}protected ArrayList<GeneratorMember> buildRightList_Of_GeneratorMember()
	{
		ArrayList<GeneratorMember> list = new ArrayList<GeneratorMember>();
		return list;
	}
	protected ArrayList<FuncCallArg> buildLeftList_Of_FuncCallArg(ArrayList<FuncCallArg> list, FuncCallArg elem)
	{
		list.add(elem);
		return list;
	}
	protected ArrayList<FuncCallArg> buildLeftList_Of_FuncCallArg(FuncCallArg elem)
	{
		ArrayList<FuncCallArg> list = new ArrayList<FuncCallArg>();
		list.add(elem);
		return list;
	}protected ArrayList<FuncCallArg> buildLeftList_Of_FuncCallArg()
	{
		ArrayList<FuncCallArg> list = new ArrayList<FuncCallArg>();
		return list;
	}
	protected ArrayList<Code> buildRightList_Of_Code(Code elem, ArrayList<Code> list)
	{
		list.add(0, elem);
		return list;
	}
	protected ArrayList<Code> buildRightList_Of_Code(Code elem)
	{
		ArrayList<Code> list = new ArrayList<Code>();
		list.add(elem);
		return list;
	}protected ArrayList<Code> buildRightList_Of_Code()
	{
		ArrayList<Code> list = new ArrayList<Code>();
		return list;
	}
	protected ArrayList<Id> buildLeftList_Of_Id(ArrayList<Id> list, Id elem)
	{
		list.add(elem);
		return list;
	}
	protected ArrayList<Id> buildLeftList_Of_Id(Id elem)
	{
		ArrayList<Id> list = new ArrayList<Id>();
		list.add(elem);
		return list;
	}protected ArrayList<Id> buildLeftList_Of_Id()
	{
		ArrayList<Id> list = new ArrayList<Id>();
		return list;
	}
}
