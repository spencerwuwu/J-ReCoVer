// https://searchcode.com/api/result/53215947/

package org.cocuyo.dsl.grammar.ast;

import java.util.ArrayList;

import org.cocuyo.parsing.ASTBuilder;
import org.cocuyo.parsing.IToken;

//close-imports
public class CocuyoGrammarASTBuilder extends ASTBuilder
//open-inheritance//close-inheritance
{
	public CocuyoGrammarASTBuilder() {
		super();
	}

	@Override
	public void reduce(int ruleIndex) {
		Object ast = null;

		switch (ruleIndex) {
		//(0) <CompileUnit> ::= <PackageDef> <UnitElementList>
		case 0: {
			Object[] _alt = pop(2);
			ast = new CompileUnit((PackageDef) _alt[0],
					buildList_Of_UnitElement(_alt[1]));
			pushAST(ast);
			break;
		}
			//(1) <PackageDef> ::= "package" <Name>
		case 1: {
			Object[] _alt = pop(2);
			ast = new PackageDef((Name) _alt[1]);
			pushAST(ast);
			break;
		}
			//(2) <PackageDef> ::= 
		case 2: {
			Object[] _alt = pop(0);
			ast = new PackageDef();
			pushAST(ast);
			break;
		}
			//(3) <UnitElementList> ::= <UnitElement> <UnitElementList>
		case 3: {
			Object[] _alt = pop(2);
			ast = buildRightList_Of_UnitElement((UnitElement) _alt[0],
					buildList_Of_UnitElement(_alt[1]));
			pushAST(ast);
			break;
		}
			//(4) <UnitElementList> ::= <UnitElement>
		case 4: {
			Object[] _alt = pop(1);
			ast = buildRightList_Of_UnitElement((UnitElement) _alt[0]);
			pushAST(ast);
			break;
		}
			//(5) <UnitElement> ::= <Grammar>
		case 5: {
			Object[] _alt = pop(1);
			ast = /*Nothing*/(Grammar) _alt[0];
			pushAST(ast);
			break;
		}
			//(6) <UnitElement> ::= <Import>
		case 6: {
			Object[] _alt = pop(1);
			ast = /*Nothing*/(Import) _alt[0];
			pushAST(ast);
			break;
		}
			//(7) <Import> ::= "import" <Name>
		case 7: {
			Object[] _alt = pop(2);
			ast = new Import((Name) _alt[1]);
			pushAST(ast);
			break;
		}
			//(8) <Name> ::= <NameIdList>
		case 8: {
			Object[] _alt = pop(1);
			ast = new Name(buildList_Of_Id(_alt[0]));
			pushAST(ast);
			break;
		}
			//(9) <NameIdList> ::= <NameIdList> "." <Id>
		case 9: {
			Object[] _alt = pop(3);
			ast = buildLeftList_Of_Id(buildList_Of_Id(_alt[0]), (Id) _alt[2]);
			pushAST(ast);
			break;
		}
			//(10) <NameIdList> ::= <Id>
		case 10: {
			Object[] _alt = pop(1);
			ast = buildLeftList_Of_Id((Id) _alt[0]);
			pushAST(ast);
			break;
		}
			//(11) <Decoration> ::= <DecoratorList>
		case 11: {
			Object[] _alt = pop(1);
			ast = new Decoration(buildList_Of_Decorator(_alt[0]));
			pushAST(ast);
			break;
		}
			//(12) <DecoratorList> ::= <Decorator> <DecoratorList>
		case 12: {
			Object[] _alt = pop(2);
			ast = buildRightList_Of_Decorator((Decorator) _alt[0],
					buildList_Of_Decorator(_alt[1]));
			pushAST(ast);
			break;
		}
			//(13) <DecoratorList> ::= <Decorator>
		case 13: {
			Object[] _alt = pop(1);
			ast = buildRightList_Of_Decorator((Decorator) _alt[0]);
			pushAST(ast);
			break;
		}
			//(14) <Decorator> ::= <ThisDecorator ~ "@" "(" <PropertyList> ")">
		case 14: {
			Object[] _alt = pop(4);
			ast = /*Nothing*/new ThisDecorator(buildList_Of_Property(_alt[2]));
			pushAST(ast);
			break;
		}
			//(15) <Decorator> ::= <ExternalDecorator ~ "@" <Name> "(" <PropertyList> ")">
		case 15: {
			Object[] _alt = pop(5);
			ast = /*Nothing*/new ExternalDecorator((Name) _alt[1],
					buildList_Of_Property(_alt[3]));
			pushAST(ast);
			break;
		}
			//(16) <Decorator> ::= <ExternalDecorator ~ "@" <Name>>
		case 16: {
			Object[] _alt = pop(2);
			ast = /*Nothing*/new ExternalDecorator((Name) _alt[1]);
			pushAST(ast);
			break;
		}
			//(17) <PropertyList> ::= <Property> <PropertyList>
		case 17: {
			Object[] _alt = pop(2);
			ast = buildRightList_Of_Property((Property) _alt[0],
					buildList_Of_Property(_alt[1]));
			pushAST(ast);
			break;
		}
			//(18) <PropertyList> ::= 
		case 18: {
			Object[] _alt = pop(0);
			ast = buildRightList_Of_Property();
			pushAST(ast);
			break;
		}
			//(19) <Property> ::= <Name> "=" <Value>
		case 19: {
			Object[] _alt = pop(3);
			ast = new Property((Name) _alt[0], (Value) _alt[2]);
			pushAST(ast);
			break;
		}
			//(20) <Property> ::= <Name> "{" <PropertyList> "}"
		case 20: {
			Object[] _alt = pop(4);
			ast = new Property((Name) _alt[0], buildList_Of_Property(_alt[2]));
			pushAST(ast);
			break;
		}
			//(21) <Property> ::= <Name>
		case 21: {
			Object[] _alt = pop(1);
			ast = new Property((Name) _alt[0]);
			pushAST(ast);
			break;
		}
			//(22) <Property> ::= "!" <Name>
		case 22: {
			Object[] _alt = pop(2);
			ast = new Property(build_IToken(_alt[0]), (Name) _alt[1]);
			pushAST(ast);
			break;
		}
			//(23) <Value> ::= <NameValue ~ <Name>>
		case 23: {
			Object[] _alt = pop(1);
			ast = /*Nothing*/new NameValue((Name) _alt[0]);
			pushAST(ast);
			break;
		}
			//(24) <Value> ::= <NameValue ~ <Name> "(" <PropertyList> ")">
		case 24: {
			Object[] _alt = pop(4);
			ast = /*Nothing*/new NameValue((Name) _alt[0],
					buildList_Of_Property(_alt[2]));
			pushAST(ast);
			break;
		}
			//(25) <Value> ::= <LiteralValue ~ number_literal>
		case 25: {
			Object[] _alt = pop(1);
			ast = /*Nothing*/new LiteralValue(build_NumberToken(_alt[0]));
			pushAST(ast);
			break;
		}
			//(26) <Value> ::= <LiteralValue ~ string_literal>
		case 26: {
			Object[] _alt = pop(1);
			ast = /*Nothing*/new LiteralValue(build_StringToken(_alt[0]));
			pushAST(ast);
			break;
		}
			//(27) <Value> ::= <ListValue ~ "[" <ValueList> "]">
		case 27: {
			Object[] _alt = pop(3);
			ast = /*Nothing*/new ListValue(buildList_Of_Value(_alt[1]));
			pushAST(ast);
			break;
		}
			//(28) <Value> ::= <DictionaryValue ~ "{" <EntryList> "}">
		case 28: {
			Object[] _alt = pop(3);
			ast = /*Nothing*/new DictionaryValue(buildList_Of_Entry(_alt[1]));
			pushAST(ast);
			break;
		}
			//(29) <EntryList> ::= <Entry> <EntryList>
		case 29: {
			Object[] _alt = pop(2);
			ast = buildRightList_Of_Entry((Entry) _alt[0],
					buildList_Of_Entry(_alt[1]));
			pushAST(ast);
			break;
		}
			//(30) <EntryList> ::= 
		case 30: {
			Object[] _alt = pop(0);
			ast = buildRightList_Of_Entry();
			pushAST(ast);
			break;
		}
			//(31) <Entry> ::= <Value> ":" <Value>
		case 31: {
			Object[] _alt = pop(3);
			ast = new Entry((Value) _alt[0], (Value) _alt[2]);
			pushAST(ast);
			break;
		}
			//(32) <ValueList> ::= <Value> <ValueList>
		case 32: {
			Object[] _alt = pop(2);
			ast = buildRightList_Of_Value((Value) _alt[0],
					buildList_Of_Value(_alt[1]));
			pushAST(ast);
			break;
		}
			//(33) <ValueList> ::= 
		case 33: {
			Object[] _alt = pop(0);
			ast = buildRightList_Of_Value();
			pushAST(ast);
			break;
		}
			//(34) <Grammar> ::= "grammar" <Id> <RuleList> "end"
		case 34: {
			Object[] _alt = pop(4);
			ast = new Grammar((Id) _alt[1], buildList_Of_Rule(_alt[2]));
			pushAST(ast);
			break;
		}
			//(35) <Grammar> ::= <Decoration> "grammar" <Id> <RuleList> "end"
		case 35: {
			Object[] _alt = pop(5);
			ast = new Grammar((Decoration) _alt[0], (Id) _alt[2],
					buildList_Of_Rule(_alt[3]));
			pushAST(ast);
			break;
		}
			//(36) <RuleSet> ::= <Decoration> "{" <RuleList> "}"
		case 36: {
			Object[] _alt = pop(4);
			ast = new RuleSet((Decoration) _alt[0], buildList_Of_Rule(_alt[2]));
			pushAST(ast);
			break;
		}
			//(37) <RuleSet> ::= "{" <RuleList> "}"
		case 37: {
			Object[] _alt = pop(3);
			ast = new RuleSet(buildList_Of_Rule(_alt[1]));
			pushAST(ast);
			break;
		}
			//(38) <RuleList> ::= <Rule> <RuleList>
		case 38: {
			Object[] _alt = pop(2);
			ast = buildRightList_Of_Rule((Rule) _alt[0],
					buildList_Of_Rule(_alt[1]));
			pushAST(ast);
			break;
		}
			//(39) <RuleList> ::= 
		case 39: {
			Object[] _alt = pop(0);
			ast = buildRightList_Of_Rule();
			pushAST(ast);
			break;
		}
			//(40) <Rule> ::= <SimpleRule ~ <Decoration> <RuleLeftSide> "->" <SubRule> ";">
		case 40: {
			Object[] _alt = pop(5);
			ast = /*Nothing*/new SimpleRule((Decoration) _alt[0],
					(RuleLeftSide) _alt[1], (SubRule) _alt[3]);
			pushAST(ast);
			break;
		}
			//(41) <Rule> ::= <SimpleRule ~ <RuleLeftSide> "->" <SubRule> ";">
		case 41: {
			Object[] _alt = pop(4);
			ast = /*Nothing*/new SimpleRule((RuleLeftSide) _alt[0],
					(SubRule) _alt[2]);
			pushAST(ast);
			break;
		}
			//(42) <Rule> ::= <RuleSet>
		case 42: {
			Object[] _alt = pop(1);
			ast = /*Nothing*/(RuleSet) _alt[0];
			pushAST(ast);
			break;
		}
			//(43) <RuleLeftSide> ::= <Id>
		case 43: {
			Object[] _alt = pop(1);
			ast = new RuleLeftSide((Id) _alt[0]);
			pushAST(ast);
			break;
		}
			//(44) <SubRule> ::= <AlternativeList>
		case 44: {
			Object[] _alt = pop(1);
			ast = new SubRule(buildList_Of_Alternative(_alt[0]));
			pushAST(ast);
			break;
		}
			//(45) <AlternativeList> ::= <Alternative> "|" <AlternativeList>
		case 45: {
			Object[] _alt = pop(3);
			ast = buildRightList_Of_Alternative((Alternative) _alt[0],
					buildList_Of_Alternative(_alt[2]));
			pushAST(ast);
			break;
		}
			//(46) <AlternativeList> ::= <Alternative>
		case 46: {
			Object[] _alt = pop(1);
			ast = buildRightList_Of_Alternative((Alternative) _alt[0]);
			pushAST(ast);
			break;
		}
			//(47) <Alternative> ::= <AltId> <AltExprList>
		case 47: {
			Object[] _alt = pop(2);
			ast = new Alternative((AltId) _alt[0],
					buildList_Of_AltExpr(_alt[1]));
			pushAST(ast);
			break;
		}
			//(48) <Alternative> ::= <AltId> <AltExprList> "=>" <Symbol>
		case 48: {
			Object[] _alt = pop(4);
			ast = new Alternative((AltId) _alt[0],
					buildList_Of_AltExpr(_alt[1]), (Symbol) _alt[3]);
			pushAST(ast);
			break;
		}
			//(49) <Alternative> ::= <AltId>
		case 49: {
			Object[] _alt = pop(1);
			ast = new Alternative((AltId) _alt[0]);
			pushAST(ast);
			break;
		}
			//(50) <AltId> ::= "{" <Id> "}"
		case 50: {
			Object[] _alt = pop(3);
			ast = new AltId((Id) _alt[1]);
			pushAST(ast);
			break;
		}
			//(51) <AltId> ::= "{" "}"
		case 51: {
			Object[] _alt = pop(2);
			ast = new AltId();
			pushAST(ast);
			break;
		}
			//(52) <AltId> ::= "{" <Decoration> <Id> "}"
		case 52: {
			Object[] _alt = pop(4);
			ast = new AltId((Decoration) _alt[1], (Id) _alt[2]);
			pushAST(ast);
			break;
		}
			//(53) <AltId> ::= "{" <Decoration> "}"
		case 53: {
			Object[] _alt = pop(3);
			ast = new AltId((Decoration) _alt[1]);
			pushAST(ast);
			break;
		}
			//(54) <AltId> ::= 
		case 54: {
			Object[] _alt = pop(0);
			ast = new AltId();
			pushAST(ast);
			break;
		}
			//(55) <AltExprList> ::= <AltExpr> <AltExprList>
		case 55: {
			Object[] _alt = pop(2);
			ast = buildRightList_Of_AltExpr((AltExpr) _alt[0],
					buildList_Of_AltExpr(_alt[1]));
			pushAST(ast);
			break;
		}
			//(56) <AltExprList> ::= <AltExpr>
		case 56: {
			Object[] _alt = pop(1);
			ast = buildRightList_Of_AltExpr((AltExpr) _alt[0]);
			pushAST(ast);
			break;
		}
			//(57) <AltExpr> ::= <AltElement>
		case 57: {
			Object[] _alt = pop(1);
			ast = new AltExpr((AltElement) _alt[0]);
			pushAST(ast);
			break;
		}
			//(58) <AltExpr> ::= <AltExpr> <Cuantifier ~ "+">
		case 58: {
			Object[] _alt = pop(2);
			ast = new AltExpr((AltExpr) _alt[0], new Cuantifier(
					build_IToken(_alt[1])));
			pushAST(ast);
			break;
		}
			//(59) <AltExpr> ::= <AltExpr> <Cuantifier ~ "*">
		case 59: {
			Object[] _alt = pop(2);
			ast = new AltExpr((AltExpr) _alt[0], new Cuantifier(
					build_IToken(_alt[1])));
			pushAST(ast);
			break;
		}
			//(60) <AltExpr> ::= <AltExpr> <Cuantifier ~ "?">
		case 60: {
			Object[] _alt = pop(2);
			ast = new AltExpr((AltExpr) _alt[0], new Cuantifier(
					build_IToken(_alt[1])));
			pushAST(ast);
			break;
		}
			//(61) <AltElement> ::= <Symbol>
		case 61: {
			Object[] _alt = pop(1);
			ast = /*Nothing*/(Symbol) _alt[0];
			pushAST(ast);
			break;
		}
			//(62) <AltElement> ::= <SubRuleElement ~ "(" <SubRule> ")">
		case 62: {
			Object[] _alt = pop(3);
			ast = /*Nothing*/new SubRuleElement((SubRule) _alt[1]);
			pushAST(ast);
			break;
		}
			//(63) <Symbol> ::= <Node>
		case 63: {
			Object[] _alt = pop(1);
			ast = new Symbol((Node) _alt[0]);
			pushAST(ast);
			break;
		}
			//(64) <Symbol> ::= <Node> ":" <Id>
		case 64: {
			Object[] _alt = pop(3);
			ast = new Symbol((Node) _alt[0], (Id) _alt[2]);
			pushAST(ast);
			break;
		}
			//(65) <Symbol> ::= <Node> ":" <Id> "=" <Id>
		case 65: {
			Object[] _alt = pop(5);
			ast = new Symbol((Node) _alt[0], (Id) _alt[2], (Id) _alt[4]);
			pushAST(ast);
			break;
		}
			//(66) <Symbol> ::= <Node> "=" <Id>
		case 66: {
			Object[] _alt = pop(3);
			ast = new Symbol((Node) _alt[0], (Id) _alt[2]);
			pushAST(ast);
			break;
		}
			//(67) <Symbol> ::= <Id> "-" <Node>
		case 67: {
			Object[] _alt = pop(3);
			ast = new Symbol((Id) _alt[0], (Node) _alt[2]);
			pushAST(ast);
			break;
		}
			//(68) <Symbol> ::= <Id> "-" <Node> ":" <Id>
		case 68: {
			Object[] _alt = pop(5);
			ast = new Symbol((Id) _alt[0], (Node) _alt[2], (Id) _alt[4]);
			pushAST(ast);
			break;
		}
			//(69) <Symbol> ::= <Id> "-" <Node> ":" <Id> "=" <Id>
		case 69: {
			Object[] _alt = pop(7);
			ast = new Symbol((Id) _alt[0], (Node) _alt[2], (Id) _alt[4],
					(Id) _alt[6]);
			pushAST(ast);
			break;
		}
			//(70) <Symbol> ::= <Id> "-" <Node> "=" <Id>
		case 70: {
			Object[] _alt = pop(5);
			ast = new Symbol((Id) _alt[0], (Node) _alt[2], (Id) _alt[4]);
			pushAST(ast);
			break;
		}
			//(71) <Node> ::= <IdNode ~ <Decoration> <Id>>
		case 71: {
			Object[] _alt = pop(2);
			ast = /*Nothing*/new IdNode((Decoration) _alt[0], (Id) _alt[1]);
			pushAST(ast);
			break;
		}
			//(72) <Node> ::= <IdNode ~ <Id>>
		case 72: {
			Object[] _alt = pop(1);
			ast = /*Nothing*/new IdNode((Id) _alt[0]);
			pushAST(ast);
			break;
		}
			//(73) <Node> ::= <LiteralNode ~ <Decoration> string_literal>
		case 73: {
			Object[] _alt = pop(2);
			ast = /*Nothing*/new LiteralNode((Decoration) _alt[0],
					build_IToken(_alt[1]));
			pushAST(ast);
			break;
		}
			//(74) <Node> ::= <LiteralNode ~ string_literal>
		case 74: {
			Object[] _alt = pop(1);
			ast = /*Nothing*/new LiteralNode(build_IToken(_alt[0]));
			pushAST(ast);
			break;
		}
			//(75) <Id> ::= id
		case 75: {
			Object[] _alt = pop(1);
			ast = new Id(build_IToken(_alt[0]));
			pushAST(ast);
			break;
		}
		}

		//open-reduce//close-reduce
	}

	protected IToken build_IToken(Object token) {
		//open-build-token-IToken
		return (IToken) token;
		//close-build-token-IToken
	}

	protected NumberToken build_NumberToken(Object token) {
		//open-build-token-NumberToken
		return (NumberToken) token;
		//close-build-token-NumberToken
	}

	protected StringToken build_StringToken(Object token) {
		//open-build-token-StringToken
		return new StringToken((IToken) token);
		//close-build-token-StringToken
	}

	protected ArrayList<Value> buildList_Of_Value(Object ast) {
		return (ArrayList<Value>) ast;
	}

	protected ArrayList<Entry> buildList_Of_Entry(Object ast) {
		return (ArrayList<Entry>) ast;
	}

	protected ArrayList<Decorator> buildList_Of_Decorator(Object ast) {
		return (ArrayList<Decorator>) ast;
	}

	protected ArrayList<UnitElement> buildList_Of_UnitElement(Object ast) {
		return (ArrayList<UnitElement>) ast;
	}

	protected ArrayList<Id> buildList_Of_Id(Object ast) {
		return (ArrayList<Id>) ast;
	}

	protected ArrayList<Alternative> buildList_Of_Alternative(Object ast) {
		return (ArrayList<Alternative>) ast;
	}

	protected ArrayList<Rule> buildList_Of_Rule(Object ast) {
		return (ArrayList<Rule>) ast;
	}

	protected ArrayList<Property> buildList_Of_Property(Object ast) {
		return (ArrayList<Property>) ast;
	}

	protected ArrayList<AltExpr> buildList_Of_AltExpr(Object ast) {
		return (ArrayList<AltExpr>) ast;
	}

	protected ArrayList<UnitElement> buildRightList_Of_UnitElement(
			UnitElement elem, ArrayList<UnitElement> list) {
		list.add(0, elem);
		return list;
	}

	protected ArrayList<UnitElement> buildRightList_Of_UnitElement(
			UnitElement elem) {
		ArrayList<UnitElement> list = new ArrayList<UnitElement>();
		list.add(elem);
		return list;
	}

	protected ArrayList<UnitElement> buildRightList_Of_UnitElement() {
		ArrayList<UnitElement> list = new ArrayList<UnitElement>();
		return list;
	}

	protected ArrayList<Rule> buildRightList_Of_Rule(Rule elem,
			ArrayList<Rule> list) {
		list.add(0, elem);
		return list;
	}

	protected ArrayList<Rule> buildRightList_Of_Rule(Rule elem) {
		ArrayList<Rule> list = new ArrayList<Rule>();
		list.add(elem);
		return list;
	}

	protected ArrayList<Rule> buildRightList_Of_Rule() {
		ArrayList<Rule> list = new ArrayList<Rule>();
		return list;
	}

	protected ArrayList<Alternative> buildRightList_Of_Alternative(
			Alternative elem, ArrayList<Alternative> list) {
		list.add(0, elem);
		return list;
	}

	protected ArrayList<Alternative> buildRightList_Of_Alternative(
			Alternative elem) {
		ArrayList<Alternative> list = new ArrayList<Alternative>();
		list.add(elem);
		return list;
	}

	protected ArrayList<Alternative> buildRightList_Of_Alternative() {
		ArrayList<Alternative> list = new ArrayList<Alternative>();
		return list;
	}

	protected ArrayList<Decorator> buildRightList_Of_Decorator(Decorator elem,
			ArrayList<Decorator> list) {
		list.add(0, elem);
		return list;
	}

	protected ArrayList<Decorator> buildRightList_Of_Decorator(Decorator elem) {
		ArrayList<Decorator> list = new ArrayList<Decorator>();
		list.add(elem);
		return list;
	}

	protected ArrayList<Decorator> buildRightList_Of_Decorator() {
		ArrayList<Decorator> list = new ArrayList<Decorator>();
		return list;
	}

	protected ArrayList<Entry> buildRightList_Of_Entry(Entry elem,
			ArrayList<Entry> list) {
		list.add(0, elem);
		return list;
	}

	protected ArrayList<Entry> buildRightList_Of_Entry(Entry elem) {
		ArrayList<Entry> list = new ArrayList<Entry>();
		list.add(elem);
		return list;
	}

	protected ArrayList<Entry> buildRightList_Of_Entry() {
		ArrayList<Entry> list = new ArrayList<Entry>();
		return list;
	}

	protected ArrayList<Value> buildRightList_Of_Value(Value elem,
			ArrayList<Value> list) {
		list.add(0, elem);
		return list;
	}

	protected ArrayList<Value> buildRightList_Of_Value(Value elem) {
		ArrayList<Value> list = new ArrayList<Value>();
		list.add(elem);
		return list;
	}

	protected ArrayList<Value> buildRightList_Of_Value() {
		ArrayList<Value> list = new ArrayList<Value>();
		return list;
	}

	protected ArrayList<AltExpr> buildRightList_Of_AltExpr(AltExpr elem,
			ArrayList<AltExpr> list) {
		list.add(0, elem);
		return list;
	}

	protected ArrayList<AltExpr> buildRightList_Of_AltExpr(AltExpr elem) {
		ArrayList<AltExpr> list = new ArrayList<AltExpr>();
		list.add(elem);
		return list;
	}

	protected ArrayList<AltExpr> buildRightList_Of_AltExpr() {
		ArrayList<AltExpr> list = new ArrayList<AltExpr>();
		return list;
	}

	protected ArrayList<Property> buildRightList_Of_Property(Property elem,
			ArrayList<Property> list) {
		list.add(0, elem);
		return list;
	}

	protected ArrayList<Property> buildRightList_Of_Property(Property elem) {
		ArrayList<Property> list = new ArrayList<Property>();
		list.add(elem);
		return list;
	}

	protected ArrayList<Property> buildRightList_Of_Property() {
		ArrayList<Property> list = new ArrayList<Property>();
		return list;
	}

	protected ArrayList<Id> buildLeftList_Of_Id(ArrayList<Id> list, Id elem) {
		list.add(elem);
		return list;
	}

	protected ArrayList<Id> buildLeftList_Of_Id(Id elem) {
		ArrayList<Id> list = new ArrayList<Id>();
		list.add(elem);
		return list;
	}

	protected ArrayList<Id> buildLeftList_Of_Id() {
		ArrayList<Id> list = new ArrayList<Id>();
		return list;
	}
}
