// https://searchcode.com/api/result/105578044/

package fluent;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Experimenting with "fluent" java programming
 * @author srydberg
 *
 * @param <T>
 */
public class Fluent<T>
{
	/**
	 * Emulate ruby :)
	 * @param args
	 */
	public static void main(String[] args)
	{
		String s = list(1,2,3,4,5).map(multiply(2)).reverse().join(", ");
		System.out.println(s);
		
		Integer max = list(1,3,67,300,3,4,67).map(multiply(2)).reduce(max());
		System.out.println(max);
		
		Integer sum = list(1,3,67,300,3,4,67).reduce(sum());
		System.out.println(sum);
	}
	
	/**
	 * Interface for an iterator with collection methods
	 * @author srydberg
	 *
	 * @param <T> the type to iterate over
	 */
	public interface FluentIterator<T> extends Iterator<T>
	{
		<R> FluentIterator<R> map(Mapper<R,T> function);
		
		T reduce(Reducer<T> function,T initial);
		T reduce(Reducer<T> function);
		
		String join(String inSepartor);
		FluentIterator<T> reverse();
	}
	
	/**
	 * Interface for applying an operation on an item
	 * @author srydberg
	 *
	 * @param <T>
	 */
	public interface Mapper<R,T>
	{
		R map(T item);
	}
	
	/**
	 * Interface for applying an operation on a collection
	 * @author srydberg
	 *
	 * @param <T>
	 */
	public interface Reducer<T>
	{
		T reduce(T memo, T item);
	}
	
	/**
	 * Factory method for creating a fluent iterator
	 * @param <T>
	 * @param items
	 * @return
	 */
	public static <T> FluentIterator<T> list(T... items)
	{
		Iterator<T> iterator = Arrays.asList(items).iterator();
		FluentItr<T> itrl = new FluentItr<T>(iterator);
		return itrl;
	}
	
	/**
	 * Implementation of a FluentIterator
	 * @author srydberg
	 *
	 * @param <T>
	 */
	private static class FluentItr<T> implements FluentIterator<T>
	{
		Iterator<T> m_iterator;
		
		public FluentItr(Iterator<T> inIterator)
		{
			m_iterator = inIterator;
		}
		
		public boolean hasNext() {
			return m_iterator.hasNext();
		}

		public T next()
		{
			return m_iterator.next();
		}

		public void remove()
		{
			m_iterator.remove();
		}
		
		public <R> FluentIterator<R> map(Mapper<R,T> function)
		{
			List<R> result = new LinkedList<R>();
			while(m_iterator.hasNext())
			{
				result.add(function.map(m_iterator.next()));
			}

			return new FluentItr<R>(result.iterator());
		}
		
		public T reduce(Reducer<T> function, T initial)
		{
			T result = initial;
			
			while(m_iterator.hasNext())
			{
				result = function.reduce(result,m_iterator.next());
			}
			
			return result;
		}
		
		public T reduce(Reducer<T> function)
		{
			if(m_iterator.hasNext())
			{
				return reduce(function, m_iterator.next());
			}
			
			return null;
		}
		
		public String join(String inSeparator)
		{
			StringBuffer sb = new StringBuffer();
			while(m_iterator.hasNext())
			{
				sb.append(m_iterator.next()+inSeparator);
			}

			sb.setLength(sb.length()-2);
			
			return sb.toString();
		}
		
		public FluentIterator<T> reverse()
		{
			LinkedList<T> result = new LinkedList<T>();
			while(m_iterator.hasNext())
			{
				result.offerFirst(m_iterator.next());
			}

			m_iterator = result.iterator();

			return this;
		}
	}
	
	public static Mapper<Integer,Integer> multiply(final Integer factor)
	{
		return new Mapper<Integer,Integer>()
		{
			public Integer map(Integer item) {
				return item * factor;
			}
		};
	}
	
	public static Mapper<String,String> upper()
	{
		return new Mapper<String,String>()
		{
			public String map(String item) {
				return item.toUpperCase();
			}
		};
	}
	
	public static Reducer<Integer> max()
	{
		return new Reducer<Integer>()
		{
			public Integer reduce(Integer memo, Integer item)
			{
				if(item > memo)
					return item;
				else
					return memo;
			}
		};
	}
	
	public static Reducer<Integer> sum()
	{
		return new Reducer<Integer>()
		{
			public Integer reduce(Integer memo, Integer item)
			{
				return memo + item;
			}
		};
	}
}

