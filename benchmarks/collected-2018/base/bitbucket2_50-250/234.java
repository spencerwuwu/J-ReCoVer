// https://searchcode.com/api/result/53215661/

package org.cocuyo.parsing;

import static java.lang.System.out;

import java.util.Stack;

public abstract class ASTBuilder
{
    private Stack<Object> fASTStack;
    private Object fLastAST;

    public ASTBuilder()
    {
	reset();
    }

    public abstract void reduce(int aRuleIndex);

    public void pushAST(Object aAST)
    {
	fLastAST = fASTStack.push(aAST);
    }

    public Object getLastAST()
    {
	return fLastAST;
    }

    public void shift(Object aToken)
    {
	pushAST(aToken);
    }

    public void reset()
    {
	fASTStack = new Stack<Object>();
    }

    public void printStack()
    {
	out.print("{");
	for (Object _obj : fASTStack)
	{
	    out.print((_obj instanceof IToken ? "'" + _obj + "'" : _obj
		    .getClass().getSimpleName())
		    + " ");
	}
	out.println("}");
    }

    protected Object[] pop(int aSize)
    {
	Object[] _result = new Object[aSize];

	for (int _i = 0; _i < aSize; _i++)
	{
	    _result[aSize - _i - 1] = fASTStack.pop();
	}
	return _result;
    }

}

