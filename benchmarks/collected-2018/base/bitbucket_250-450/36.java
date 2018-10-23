// https://searchcode.com/api/result/123311833/

package com.chine.invertedindex.mapreduce.search.highlight;

import java.nio.charset.CharacterCodingException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Reducer;

import com.chine.invertedindex.analysis.ChineseTokenizer;

public class HighlightSearchReducer extends Reducer<Text, Text, Text, Text> {
	
	public static final String HIGHLIGHT_SIZE_KEY = "search.snippet";
	public static final String HIGHLIGHT_LEFT_KEY = "highlight.left";
	public static final String HIGHLIGHT_RIGHT_KEY = "highlight.right";
	
	@Override
	public void reduce(Text key, Iterable<Text> values, Context context) 
		throws IOException, InterruptedException, CharacterCodingException {
		
		Configuration conf = context.getConfiguration();
		String hll = conf.get(HIGHLIGHT_LEFT_KEY);
		String hlr = conf.get(HIGHLIGHT_RIGHT_KEY);
		int snippet = Integer.valueOf(context.getConfiguration().get(HIGHLIGHT_SIZE_KEY));
		
		List<Text> texts = new ArrayList<Text>();
		for(Text val: values) {
			texts.add(new Text(val.toString()));
		}
		
		Collections.sort(texts, new Comparator<Text>() {
			public int compare(Text o1, Text o2) {
				try {
					return parsePos(o1).compareTo(parsePos(o2));
				} catch (CharacterCodingException e) {
					return 0;
				} 
			}
		});
		
		String highlight = "";
		List<Span> spans = getSpans(texts, snippet);
		
		if(spans.size() > 0) {
			boolean startPrintdot = false;
			boolean firstRun = true;
			for(Text cTxt: texts) {
				if (inSpans(parsePos(cTxt), spans)) {
					int startIdx = cTxt.find("(");
					int endIdx = cTxt.find(",");
					String token = Text.decode(cTxt.getBytes(), startIdx+1, endIdx-startIdx-1);
					if(cTxt.charAt(0) == '!') {
						highlight += sweetify(token);
					} else {
						highlight += hll + token + hlr + (isChn(token)?"":" ");
					}
					startPrintdot = false;
					if(firstRun) firstRun = false;
				}  else {
					startPrintdot = true;
				}
				
				if(!firstRun && startPrintdot) {
					highlight += "...";
					firstRun = true;
				}
			}
			highlight = highlight.substring(0, highlight.length() - 3);
			
			context.write(key, new Text(highlight));
		}
	}
	
	private boolean isChn(String token) {
		return ChineseTokenizer.chnPat.matcher(token).matches();
	}
	
	private String sweetify(String token) {
		if(isChn(token)) {
			return token;
		}
		return token + " ";
	}
	
	private Integer parsePos(Text val) throws CharacterCodingException {
		int endIndex = val.find(")");
		int startIndex = val.find(",");
		String retVal = Text
			.decode(val.getBytes(), startIndex+1, endIndex-startIndex-1)
			.trim();
		return Integer.valueOf(retVal);
	}
	
	private List<Integer> parsePositions(List<Text> values) 
		throws CharacterCodingException {
		
		List<Integer> positions = new ArrayList<Integer>();
		
		for(Text val: values) {
			if(!val.toString().trim().startsWith("!")) {
				positions.add(parsePos(val));
			}
		}
		
		Collections.sort(positions);
		
		return positions;
	}
	
	private int getAllLength(List<Text> values) {
		return values.size();
	}
	
	private List<Span> initSpans(List<Integer> values) throws CharacterCodingException {
		List<Span> res = new ArrayList<Span>();
		
		for(Integer i: values)
			res.add(new Span(i, i));
		
		return res;
	}
	
	private List<Span> aggregationSpans(List<Span> spans) {
		List<Span> res = new ArrayList<Span> ();
		for(Span s: spans) {
			res.add(s);
		}
		Collections.sort(res, new Comparator<Span>() {
			@Override
			public int compare(Span arg0, Span arg1) {
				return Integer.valueOf(arg0.start).compareTo(Integer.valueOf(arg1.start));
			}
		});
		
		int i = 0;
		while(i < res.size() - 1) {
			Span spani = res.get(i);
			Span spaniplus1 = res.get(i+1);
			
			if(spani.end > spaniplus1.end) {
				res.remove(i+1);
			} else if(spani.end >= spaniplus1.start - 1) {
				Span newSpan = new Span(spani.start, spaniplus1.end);
				res.remove(i+1);
				res.remove(i);
				res.add(i, newSpan);
			} else {
				i++;
			}
		}
		
		return res;
	}
	
	private int countSpansTokenSize(List<Span> spans) {
		int res = 0;
		for(Span s: spans) {
			res += s.end - s.start +1;
		}
		return res;
	}
	
	private int getPartitionSize (List<Span> spans) {
		return spans.size() * 2;
	}
	
	private boolean inSpans(int idx, List<Span> spans) {
		for(Span s: spans) {
			if(idx >= s.start && idx <= s.end) {
				return true;
			}
		}
		return false;
	}
	
	private List<Span> getSpans (List<Text> values, int snippet) 
		throws CharacterCodingException {
		
		//doctoken
		int length = getAllLength(values);
		//dochittoken
		List<Integer> positions = parsePositions(values);
		Collections.sort(positions);
		//dochittoken
		int posSize = positions.size();
		
		//Spantoken
		List<Span> spans = new ArrayList<Span>();
		
		if(posSize == 0) return spans;
		
		if(snippet >= length || snippet < posSize) {
			spans.add(new Span(1, length));
			return spans;
		}
		
		spans = initSpans(positions);
		int partitionCount = getPartitionSize(spans);
		int partitionTokenCount = (snippet - spans.size()) / partitionCount;
		while(true) {
			for(Span s: spans) {
				s.addLeft(partitionTokenCount);
				s.addRight(partitionTokenCount, length);
			}
			spans = aggregationSpans(spans);
			partitionCount = getPartitionSize(spans);
			partitionTokenCount = (snippet - countSpansTokenSize(spans)) / partitionCount;
			
			if (partitionTokenCount == 0)
				break;
		}
		
		int rest = snippet - countSpansTokenSize(spans);
		int i = 0;
		while(i < spans.size() - 1) {
			if (rest == 0) break;
			
			Span spani = spans.get(i);
			Span spaniplus1 = spans.get(i+1);
			
			if (spaniplus1.start - spani.end - 1 < rest) {
				spans.add(i+1, new Span(spani.end+1, spaniplus1.start-1));
				spans = aggregationSpans(spans);
				rest -= spaniplus1.start - spani.end - 1;
			} else {
				spans.add(i+1, new Span(spani.end+1, spani.end+rest));
				spans = aggregationSpans(spans);
				rest = 0;
			}
		}
		
		return spans;
	}
	
	public class Span {
		private int start;
		private int end;
		
		public Span(int start, int end) {
			this.start = start;
			this.end = end;
		}
		
		public int getStart() { return this.start; }
		public int getEnd() { return this.end; }
		
		public void addLeft(int size) {
			if(start - size >= 1) {
				this.start = start - size;
			} else {
				this.start = 1;
			}
		}
		
		public void addRight(int size, int fullLength) {
			if(end + size <= fullLength) {
				this.end = end + size;
			} else {
				this.end = fullLength;
			}
		}
	}
	
	public static void main(String[] args) throws CharacterCodingException {
		final HighlightSearchReducer rd = new HighlightSearchReducer();
		
		String key = "doc1";
		List<Text> texts = new ArrayList<Text>();
		for(int i = 3000; i >0; --i) {
			String isResult = i % 400 == 0 ? "" : "!";
			texts.add(new Text(isResult+"(token"+i+","+i+")"));
		}
		
		Collections.sort(texts, new Comparator<Text>() {
			public int compare(Text o1, Text o2) {
				try {
					return rd.parsePos(o1).compareTo(rd.parsePos(o2));
				} catch (CharacterCodingException e) {
					return 0;
				} 
			}
		});
		
		String hll = "[";
		String hlr = "]";
		int snippet = 50;
		
		String highlight = "";
		List<Span> spans = rd.getSpans(texts, snippet);
		int idx = 0;
		for(Span sp: spans) {
			//System.out.println("span:"+sp.start+","+sp.end);
			for(int i = sp.start-1; i<sp.end; i++) {
				Text cTxt = texts.get(i);
				int startIdx = cTxt.find("(");
				int endIdx = cTxt.find(",");
				String token = Text.decode(cTxt.getBytes(), startIdx+1, endIdx-startIdx-1);
				if(cTxt.charAt(0) == '!') {
					highlight += rd.sweetify(token);
				} else {
					highlight += hll + token + hlr + (rd.isChn(token)?"":" ");
				}
			}
			if(idx != spans.size() - 1)
				highlight += "...";
			idx++;
		}
		
		System.out.println(key + "\t" + highlight);
	}
}

