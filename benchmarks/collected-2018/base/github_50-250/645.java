// https://searchcode.com/api/result/66398878/

/*
Copyright (c) 2010, Hammurabi Mendes
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package mapreduce.programs.counting;

import java.util.Collections;
import java.util.Comparator;

import java.util.Map;
import java.util.Map.Entry;

import java.util.List;
import java.util.ArrayList;

import mapreduce.communication.MRRecord;

import mapreduce.programs.Reducer;

public class CountingReducer<K> extends Reducer<K,Long> {
	private static final long serialVersionUID = 1L;

	private CountingCombiner<K> combiner;

	public CountingReducer() {
		this.combiner = new CountingCombiner<K>();
	}

	public void reduce(K key, Long value) {
		combiner.addTuple(key, value);
	}

	public void flushReduce() {
		List<Map.Entry<K,Long>> tuples = new ArrayList<Map.Entry<K,Long>>(combiner.getTuples());

		Collections.sort(tuples, new CountingEntryComparator<K>());

		for (Map.Entry<K,Long> tuple: tuples) {
			K key = tuple.getKey();
			Long value = tuple.getValue();

			writeArbitraryChannel(new MRRecord<K,Long>(key, value));
		}
	}
}

class CountingEntryComparator<K> implements Comparator<Map.Entry<K,Long>> {
	public int compare(Entry<K, Long> first, Entry<K, Long> second) {
		return first.getValue().compareTo(second.getValue());
	}
}

