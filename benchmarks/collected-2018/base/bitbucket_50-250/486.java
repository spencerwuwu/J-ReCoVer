// https://searchcode.com/api/result/92003055/

package edu.haw.ttvp2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.data.URL;

public class Enemy implements Comparable<Enemy> {
	ID id;
	BigInteger start, end, step;
	Map<BigInteger, Integer> hitMap;
	int ships, hits, hitsMulti, maxHits;
	double hitChance = 0.0;
	URL url;
	
	public Enemy(ID id, URL url, int ships, int maxHits) {
		this.id = id;
		this.url = url;
		this.ships = ships;
		this.end = id.toBigInteger();
		this.hitMap = new HashMap<BigInteger, Integer>();
		this.maxHits = maxHits;
		this.hitChance = (1.0 * ships) / maxHits;
	}

	public ID getNextTarget() {
		// randomize
		List<BigInteger> intervals = new ArrayList<BigInteger>(hitMap.keySet());
		Collections.shuffle(intervals);		
		// find an untouched interval
		for (BigInteger i : intervals)
			if (hitMap.get(i) == 0) 
				// return the middle of the interval
				return ID.valueOf(i.add(step.divide(new BigInteger("2"))));
		return null;
	}
	
	public void registerHit(ID target, boolean hit) {
		// save to hitmap
		for (BigInteger st : hitMap.keySet()) {
			ID s = ID.valueOf(st.subtract(BigInteger.ONE));
			ID e = ID.valueOf(st.add(step));
			
//			if (target.toBigInteger().compareTo(s) >= 0 && target.toBigInteger().compareTo(s.add(step)) < 0) {
			if (target.isInInterval(s, e)) {
				if (hitMap.get(st) != 0) hitsMulti++;
				else hitMap.put(st, hit ? 1 : -1);
				break;
			}
		}
		
		hits++;
		
		// reduce ships
		if (hit) {
			ships--;
			System.out.println("Enemy "+id+" has only "+ships+" ships left!");
		}
		
		hitChance = (1.0 * ships) / (maxHits - hits);
	}
	
	@Override
	public int compareTo(Enemy o) {
		return Double.compare(o.hitChance, this.hitChance);
	}
}

