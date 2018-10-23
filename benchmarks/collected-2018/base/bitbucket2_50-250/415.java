// https://searchcode.com/api/result/64138154/

package com.gmail.jafelds.ppedits;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author wolfhome
 *
 */
public class Measure
{
	protected final int MAX_BEAT = 192;
	private TreeMap<Integer, Beat> beats;
	private int columns;
	
	
	public Measure()
	{
		this(5);
	}
	
	public Measure(int c)
	{
		columns = c;
		beats = new TreeMap<Integer, Beat>();
		beats.put(0, new Beat(c));
	}
	
	public int getColumns()
	{
		return columns;
	}
	
	public void delBeat(int i)
	{
		beats.remove(i);
	}
	
	public TreeMap<Integer, Beat> getBeats()
	{
		return beats;
	}
	
	public void addBeat(int i)
	{
		beats.put(i, new Beat(getColumns()));
	}
	
	public void addBeat(int i, Beat b)
	{
		beats.put(i, b);
	}
	
	public Beat getBeat(int i)
	{
		Beat b = beats.get(i);
		if (b == null)
		{
			int c = getColumns();
			b = new Beat(c);
			addBeat(i, b);
		}
		return beats.get(i);
	}
	
	public void copyBeat(int o, int n)
	{
		addBeat(n, getBeat(o));
	}
	
	public void moveBeat(int o, int n)
	{
		addBeat(n, getBeat(o));
		delBeat(o);
	}
	
	protected String measureString()
	{
		Set<Integer> keys = beats.keySet();
		StringBuilder s = new StringBuilder();
		String nl = "\r\n";
		
		/*
		 * The goal here: reduce the amount of measures required for inserting.
		 * the magic number is 192: everything divides that.
		 */
		
		ArrayList<Integer> splits = new ArrayList<Integer>();
		splits.add(1);
		splits.add(2);
		//splits.add(3);
		splits.add(4);
		splits.add(6);
		splits.add(8);
		splits.add(12);
		splits.add(16);
		splits.add(24);
		splits.add(32);
		splits.add(48);
		splits.add(64);
		splits.add(96);
		splits.add(192);
		MULTIPLIER:
		for (int i : splits)
		{
			// KEYLOOP:
			for (int j : keys)
			{
				if (j % (MAX_BEAT / i) > 0)
				{
					continue MULTIPLIER;
				}
			}
			// At this point, we've hit a minimum multiplier.
			// LINELOOP:
			for (int n = 0; n < i; n++)
			{
				Beat b = getBeat(MAX_BEAT * n / i);
				if (b == null)
				{
					s.append(new Beat(getColumns())); // blank beat
				}
				else
				{
					s.append(b);
				}
				s.append(nl);
			}
			break MULTIPLIER;
		}
		
		return s.toString();
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		for (Map.Entry<Integer,Beat> b : beats.entrySet())
		{
			s.append("Beat " + b.getKey() + ": ");
			s.append(b.getValue().toString() + "\r\n");
		}
		return s.toString();
	}
}

