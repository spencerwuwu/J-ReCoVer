// https://searchcode.com/api/result/53215670/

package org.cocuyo.parsing.cup;

import org.cocuyo.parsing.ASTBuilder;
import org.cocuyo.parsing.IParserListener;
import org.cocuyo.parsing.IToken;

public class CupParserToASTBuilderEventsMapper implements IParserListener
{
    private ASTBuilder fBuilder;
    private boolean fDebug;
    private boolean fFindEOF;

    public CupParserToASTBuilderEventsMapper(ASTBuilder aBuilder)
    {
	fBuilder = aBuilder;
	fDebug = false;
	fFindEOF = false;

    }

    public CupParserToASTBuilderEventsMapper(ASTBuilder aBuilder, boolean aDebug)
    {
	this(aBuilder);
	fDebug = aDebug;
    }

    @Override
    public void listenEOF()
    {

    }

    @Override
    public void listenReduce(int aRuleIndex)
    {
	aRuleIndex = aRuleIndex == 0 ? 0 : aRuleIndex - 1;

	if (isDebug())
	    System.out.println((fFindEOF ? "IGNORED " : "") + "REDUCE "
		    + aRuleIndex);

	if (!fFindEOF)
	{
	    fBuilder.reduce(aRuleIndex);
	}

	if (isDebug())
	    fBuilder.printStack();

    }

    @Override
    public void listenShift(Object aToken)
    {
	if (((IToken) aToken).isEOF())
	{
	    fFindEOF = true;
	}
	else
	{
	    fBuilder.shift(aToken);
	}

	if (isDebug())
	{
	    System.out.println("SHIFT '" + aToken + "'");
	    fBuilder.printStack();
	}
    }

    @Override
    public void listenStart()
    {
	fBuilder.reset();
    }

    public ASTBuilder getBuilder()
    {
	return fBuilder;
    }

    public void setBuilder(ASTBuilder aBuilder)
    {
	fBuilder = aBuilder;
    }

    public Object getAST()
    {
	return fBuilder.getLastAST();
    }

    public boolean isDebug()
    {
	return fDebug;
    }

    public void setDebug(boolean aDebug)
    {
	fDebug = aDebug;
    }

}

