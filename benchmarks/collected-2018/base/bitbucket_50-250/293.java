// https://searchcode.com/api/result/53215579/

package tiger.syntax;
import java.util.ArrayList;
import org.cocuyo.parsing.*;
import org.cocuyo.parsing.cup.*;
import java.io.IOException;
import java_cup.runtime.*;
//open-import
//close-import

public class TigerParser extends TigerCupParser implements IParser
{
	private ArrayList<IParserListener> fListeners;
	private ICupLexer fLexer;
	private RecognitionErrorCollection fErrors;		 			
	
	
	public TigerParser(ICupLexer aLexer)
	{
		super(aLexer);
		fListeners = new ArrayList<IParserListener>();
		fErrors = new RecognitionErrorCollection();
		fLexer = aLexer;
	}
	/**
	 * Create a  TigerParser with a TigerLexer
	 */
	public TigerParser()
	{
		this(new TigerLexer());
	}
	
	@Override
	public void debug_reduce(int prodIndex, int nonTerminalIndex, int rhsSize)
	{
		//open-before-reduce
		//close-before-reduce
		for(IParserListener listener : fListeners)
		{
			listener.listenReduce(prodIndex);
		}
		//open-after-reduce
		//close-after-reduce
	}
	
	@Override
	public void debug_shift(java_cup.runtime.Symbol symbol)
	{
		//open-before-shift
		//close-before-shift
		for(IParserListener listener : fListeners)
		{
			listener.listenShift(symbol);
		}
		//open-after-shift
		//close-after-shift
	}
	
	@Override
	public void debug_message(String aMsg)
	{
		//open-before-message
		//close-before-message
		//open-after-message
		//close-after-message
	}
	
	@Override
	public java_cup.runtime.Symbol parse()  throws Exception
	{
		for(IParserListener listener : fListeners)
		{
			listener.listenStart();
		}
		
		return debug_parse();
	}
	
	@Override
	public void report_error(String aMsg, Object aInfo)
	{
		
	}
	
	@Override
	public void report_fatal_error(String aMsg, Object aInfo)
	{
		
	}
	
	@Override
	public void syntax_error(Symbol aToken)
	{
		fErrors.addUnexpectedTokenError((IToken) aToken);
	}
	
	@Override
	public void parseSource(String aSource)
	{
		fLexer.setInputSource(aSource);		
		try
		{
			parse();
		}
		catch(RecognitionException _e)
		{
			fErrors.addErrors(_e.getErrors());
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void parseFile(String aFilePath) throws IOException
	{
		fLexer.setInputFile(aFilePath);
		try
		{
			parse();
		}
		catch(RecognitionException _e)
		{
			fErrors.addErrors(_e.getErrors());
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void addListener(IParserListener aListener)
	{
		fListeners.add(aListener);
	}
	
	@Override
	public RecognitionErrorCollection getErrors()
	{
		return fErrors;
	}
	
	@Override
	public boolean hasErrors()
	{
		return getErrors().hasErrors();
	}
}
