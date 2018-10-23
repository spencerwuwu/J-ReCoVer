// https://searchcode.com/api/result/7102458/

package dovetaildb.dbservice;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


import dovetaildb.api.ApiException;
import dovetaildb.bagindex.BagIndex;
import dovetaildb.bagindex.EditRec;
import dovetaildb.bagindex.Range;
import dovetaildb.bytes.ArrayBytes;
import dovetaildb.bytes.Bytes;
import dovetaildb.bytes.CompoundBytes;
import dovetaildb.querynode.AndNotQueryNode;
import dovetaildb.querynode.AndQueryNode;
import dovetaildb.querynode.OrderedOrQueryNode;
import dovetaildb.querynode.QueryNode;
import dovetaildb.querynode.QueryNodeTemplate;
import dovetaildb.util.Util;

public class DbServiceUtil {


	/**
	 * Handles "map", "filter", "reduce", "offset", and "limit"
	 * @param i
	 * @param options
	 * @return
	 */
	
	public static final String OP_AND = "&";
	public static final String OP_OR  = "|";
	public static final String OP_NOT = "!";
	public static final String OP_ANY = "*";
	public static final String OP_AS  = "$";
	public static final String OP_LT  = "<";
	public static final String OP_GT  = ">";
	public static final String OP_LE  = "<=";
	public static final String OP_GE  = ">=";
	public static final String OP_RG_EE = "()";
	public static final String OP_RG_EI = "(]";
	public static final String OP_RG_IE = "[)";
	public static final String OP_RG_II = "[]";
	
	/*
	 * Got the hash code literals from jython like so:
	 * >>> import java
	 * >>> for s in ['&','|','!','*','$','<','>','<=','>=','()','(]','[)','[]']: print java.lang.String(s).hashCode()
	 * ...   
	 */
	public static final int OP_HASH_AND = 38;
	public static final int OP_HASH_OR  = 124;
	public static final int OP_HASH_NOT = 33;
	public static final int OP_HASH_ANY = 42;
	public static final int OP_HASH_AS  = 36;
	public static final int OP_HASH_LT  = 60;
	public static final int OP_HASH_GT  = 62;
	public static final int OP_HASH_LE  = 1921;
	public static final int OP_HASH_GE  = 1983;
	public static final int OP_HASH_BETWEEN_EE = 1281;
	public static final int OP_HASH_BETWEEN_EI = 1333;
	public static final int OP_HASH_BETWEEN_IE = 2862;
	public static final int OP_HASH_BETWEEN_II = 2914;

	public static final HashSet<Integer> SYMBOLS = new HashSet<Integer>();
	static {
		SYMBOLS.add(OP_HASH_AND);
		SYMBOLS.add(OP_HASH_OR);
		SYMBOLS.add(OP_HASH_NOT);
		SYMBOLS.add(OP_HASH_ANY);
		SYMBOLS.add(OP_HASH_AS);
		SYMBOLS.add(OP_HASH_LT);
		SYMBOLS.add(OP_HASH_GT);
		SYMBOLS.add(OP_HASH_LE);
		SYMBOLS.add(OP_HASH_GE);
		SYMBOLS.add(OP_HASH_BETWEEN_EE);
		SYMBOLS.add(OP_HASH_BETWEEN_EI);
		SYMBOLS.add(OP_HASH_BETWEEN_IE);
		SYMBOLS.add(OP_HASH_BETWEEN_II);
	}
	
	public static final ArrayBytes HEADER_BYTE_S = new ArrayBytes(new byte[]{'s'});
	public static final ArrayBytes HEADER_BYTE_L = new ArrayBytes(new byte[]{'l'});
	public static final ArrayBytes HEADER_BYTE_T = new ArrayBytes(new byte[]{'t'});
	public static final ArrayBytes HEADER_BYTE_F = new ArrayBytes(new byte[]{'f'});
	public static final ArrayBytes HEADER_BYTE_COLON = new ArrayBytes(new byte[]{':'});
	public static final ArrayBytes HEADER_BYTE_LISTOPEN = new ArrayBytes(new byte[]{'['});
	public static final ArrayBytes HEADER_BYTE_MAPOPEN  = new ArrayBytes(new byte[]{'{'});

	public static final char TYPE_CHAR_NULL   = 'l';
	public static final char TYPE_CHAR_FALSE  = 'f';
	public static final char TYPE_CHAR_TRUE   = 't';
	public static final char TYPE_CHAR_NUMBER = 'n';
	public static final char TYPE_CHAR_STRING = 's';
	public static final char TYPE_CHAR_LIST   = '[';
	public static final char TYPE_CHAR_MAP    = '{';
	public static final char CHAR_ENTRY_SEP   = ':';
	
	public static Bytes sencodeMapKey(String key) {
		try {
			return new ArrayBytes(((String)key).getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String sdecodeMapKey(Bytes bytes, int startPos) {
		int end = bytes.getLength();
		int i = startPos;
		while(bytes.get(i) != ':') {
			i++;
			if (i >= end) {
				throw new RuntimeException("Cannot find map key starting at position "+startPos+" in this term: "+bytes);
			}
		}
		try {
			return new String(bytes.getBytes(startPos, i-startPos), "utf-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static void sencodeMulti(Bytes prefix, Bytes suffix, Object val, ArrayList<EditRec> buffer, long docId, boolean idDel) {
		if (val instanceof Map) {
			Map map = (Map)val;
			prefix = CompoundBytes.make(prefix, HEADER_BYTE_MAPOPEN);
			if (true) {
				Bytes headerBytes = CompoundBytes.make(prefix, suffix);
				buffer.add(new EditRec(docId, headerBytes.flatten(), idDel));
			}
			for(Object entryObj : map.entrySet()) {
				Map.Entry entry = (Map.Entry) entryObj;
				String key = (String)entry.getKey();
				Bytes sub = new CompoundBytes(prefix, sencodeMapKey(key));
				sub = new CompoundBytes(sub, HEADER_BYTE_COLON);
				sencodeMulti(sub, suffix, entry.getValue(), buffer, docId, idDel);
			}
		} else if (val instanceof List) {
			List list = (List)val;
			prefix = CompoundBytes.make(prefix, HEADER_BYTE_LISTOPEN);
			if (true) {
				Bytes headerBytes = CompoundBytes.make(prefix, suffix);
				buffer.add(new EditRec(docId, headerBytes.flatten(), idDel));
			}
			for(int index=list.size()-1; index>=0; index--) {
				Object subVal = list.get(index);
				Bytes newSuffix = CompoundBytes.make(DbServiceUtil.sencodeListIndex(index), suffix);
				sencodeMulti(prefix, newSuffix, subVal, buffer, docId, idDel);
			}
		} else {
			Bytes bytes = CompoundBytes.make(CompoundBytes.make(prefix,sencode(val)),suffix);
			buffer.add(new EditRec(docId, bytes.flatten(), false));
		}
	}
	
	public static char typeOfObject(Object val) {
		if (val instanceof Number) {
			return TYPE_CHAR_NUMBER;
		} else if (val instanceof String) {
			return TYPE_CHAR_STRING;
		} else if (val == null) {
			return TYPE_CHAR_NULL;
		} else if (val instanceof Map) {
			return TYPE_CHAR_MAP;
		} else if (val instanceof List) {
			return TYPE_CHAR_LIST;
		} else if (val instanceof Boolean) {
			if (((Boolean)val).booleanValue()) return TYPE_CHAR_TRUE;
			else return TYPE_CHAR_FALSE;
		} else {
			throw new ApiException("UnencodableValue","Result of type \""+val.getClass().getName()+"\" cannot be encoded in JSON (must be a String, Number, Boolean, HashMap, or ArrayList)");
		}
	}
	public static Bytes sencode(Object val) {
		if (val instanceof Number) {
			double doubleVal = ((Number)val).doubleValue();
			long bits = Double.doubleToLongBits(doubleVal);
			// Invert the negation flag itself to put positives above negatives:
			if ((bits & 0x8000000000000000L) != 0) {
				// if it's a negative, invert the other bits so that a bytewise 
				// lexiographic sort puts big negatives below small negatives
				bits ^= 0xFFFFFFFFFFFFFFFFL;
			} else {
				bits ^= 0x8000000000000000L;
			}
			return new ArrayBytes(new byte[] {
					'n',
					(byte)((bits >>> 8 * 7) & 0xFF),
					(byte)((bits >>> 8 * 6) & 0xFF),
					(byte)((bits >>> 8 * 5) & 0xFF),
					(byte)((bits >>> 8 * 4) & 0xFF),
					(byte)((bits >>> 8 * 3) & 0xFF),
					(byte)((bits >>> 8 * 2) & 0xFF),
					(byte)((bits >>> 8 * 1) & 0xFF),
					(byte)((bits) & 0xFF)});
		} else if (val instanceof String) {
			try {
				Bytes valBytes = new ArrayBytes(((String)val).getBytes("utf-8"));
				return new CompoundBytes(HEADER_BYTE_S, valBytes);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		} else if (val == null) {
			return HEADER_BYTE_L;
		} else if (val instanceof Boolean) {
			if (((Boolean)val).booleanValue()) return HEADER_BYTE_T;
			else return HEADER_BYTE_F;
		} else {
			throw new ApiException("UnencodableValue","Result of type \""+val.getClass().getName()+"\" cannot be encoded in JSON (must be a String, Number, Boolean, HashMap, or ArrayList)");
		}
	}

	public static abstract class PatternVisitor {
		public void apply(Object pattern) {
			apply(ArrayBytes.EMPTY_BYTES, ArrayBytes.EMPTY_BYTES, pattern);
		}
		public void apply(Bytes prefix, Bytes suffix, Object pattern) {
			if (pattern instanceof Map) {
				Map<String,Object> map = (Map<String,Object>)pattern;
				handleMap(prefix, suffix, map);
				prefix = new CompoundBytes(prefix, HEADER_BYTE_MAPOPEN);
				for(Object entryObj : map.entrySet()) {
					Map.Entry entry = (Map.Entry) entryObj;
					String key = (String)entry.getKey();
					Object value = entry.getValue();
					Bytes curPrefix = new CompoundBytes(prefix, sencodeMapKey(key));
					curPrefix = new CompoundBytes(curPrefix, HEADER_BYTE_COLON);
					handleMapEntry(prefix, suffix, key, value);
					apply(curPrefix, suffix, value);
				}
			} else if (pattern instanceof List) {
				List<Object> list = (List<Object>)pattern;
				if (list.size() > 0) {
					Object first = list.get(0);
					if (first != null && first instanceof String && isOperationLead((String)first)) {
						handleOperation(prefix, suffix, (String)first, list);
					}
				} else if (list.size() > 1) {
					throw new ApiException("QueryFormatError", "malformed list structure; only 1 element is allowed here: "+pattern);
				} else {
					prefix = new CompoundBytes(prefix, HEADER_BYTE_LISTOPEN);
					handleList(prefix, suffix, list);
				}
			} else {
				handleAtomic(prefix, suffix, pattern);
			}
		}
		protected boolean isOperationLead(String op) {
			int opHash = op.hashCode();
			return SYMBOLS.contains(opHash);
		}
		// normally, you override one of these
		public void handleAtomic(Bytes prefix, Bytes suffix, Object val) {}
		public void handleMap(Bytes prefix, Bytes suffix, Map<String,Object> val) {}
		public void handleMapEntry(Bytes prefix, Bytes suffix, String key, Object val) {}
		public void handleList(Bytes prefix, Bytes suffix, List<Object> val) {}
		public void handleOperation(Bytes prefix, Bytes suffix, String operaion, List<Object> list) {}
	}
	
	public static QueryNodeTemplate applyPatternToBagIndex(Object pattern, BagIndex index, long revNum) {
		QueryNodeTemplate templ = applyPatternToBagIndex(ArrayBytes.EMPTY_BYTES, pattern, index, revNum);
		if (templ.varMappings.isEmpty()) {
			templ.varMappings.put("", index.getRange(Range.OPEN_RANGE, revNum));
		}
		return templ;
	}
	public static QueryNodeTemplate applyPatternToBagIndex(Bytes prefix, Object pattern, BagIndex index, long revNum) {
		Map<String, QueryNode> vars = new HashMap<String, QueryNode>();
		QueryNode queryNode;
		if (pattern instanceof Map) {
			Map map = (Map)pattern;
			prefix = new CompoundBytes(prefix, HEADER_BYTE_MAPOPEN);
			ArrayList<QueryNode> nodes = new ArrayList<QueryNode>();
			for(Object entryObj : map.entrySet()) {
				Map.Entry entry = (Map.Entry) entryObj;
				String key = (String)entry.getKey();
				Object value = entry.getValue();
				Bytes curPrefix = new CompoundBytes(prefix, sencodeMapKey(key));
				curPrefix = new CompoundBytes(curPrefix, HEADER_BYTE_COLON);
				QueryNodeTemplate templ = applyPatternToBagIndex(curPrefix, value, index, revNum);
				nodes.add(templ.queryNode);
				vars.putAll(templ.varMappings);
			}
			if (map.isEmpty()) {
				queryNode = index.getRange(new Range(prefix, null, null, true, true), revNum);
			} else {
				queryNode = AndQueryNode.make(nodes);
			}
		} else if (pattern instanceof List) {
			List list = (List)pattern;
			if (list.size() > 0 && list.get(0) != null && 
					list.get(0) instanceof String && 
					SYMBOLS.contains(list.get(0).hashCode())) {
				return applyQueryToBagIndex(prefix, list, index, revNum);
			} else if (list.size() > 1) {
				throw new ApiException("QueryFormatError", "malformed list structure in query: "+pattern);
			} else {
				prefix = new CompoundBytes(prefix, HEADER_BYTE_LISTOPEN);
				if (list.isEmpty()) {
					queryNode = index.getTerm(prefix, revNum);
				} else {
					QueryNodeTemplate templ = applyPatternToBagIndex(prefix, list.get(0), index, revNum);
					queryNode = templ.queryNode;
					vars.putAll(templ.varMappings);
				}
			}
		} else {
			Bytes matchingBytes = new CompoundBytes(prefix,sencode(pattern));
			queryNode = index.getTerm(matchingBytes, revNum);
//			System.out.println(matchingBytes.toString()+" -> "+queryNode+" in "+revNum);
		}
		return new QueryNodeTemplate(queryNode, vars);
	}
	public static final class RangeExtractor extends PatternVisitor {
		ArrayList<Range> ranges;
		@Override
		public void handleList(Bytes prefix, Bytes suffix, List<Object> list) {
			if (list.isEmpty()) {
				ranges.add(new Range(prefix, null, null, true, true));
			}
		}
		@Override
		public void handleMap(Bytes prefix, Bytes suffix, Map<String,Object> map) {
			if (map.isEmpty()) {
				ranges.add(new Range(prefix, null, null, true, true));
			}
		}
		@Override
		public void handleAtomic(Bytes prefix, Bytes suffix, Object value) {
			Bytes term = new CompoundBytes(prefix,sencode(value));
			ranges.add(new Range(term, ArrayBytes.EMPTY_BYTES, ArrayBytes.EMPTY_BYTES, true, true));
		}
		@Override
		public void handleOperation(Bytes prefix, Bytes suffix, String op, List<Object> operation) {
			int opHash = op.hashCode();
			switch(opHash) {
			case DbServiceUtil.OP_HASH_AS:
			case DbServiceUtil.OP_HASH_OR:
			case DbServiceUtil.OP_HASH_AND:
			case DbServiceUtil.OP_HASH_NOT:
				throw new ApiException("QueryFormatError", "Invalid narrowing operator: "+operation.get(0));
			default:
				ranges.add(parseRange(prefix, opHash, operation));
			}
		}
		public ArrayList<Range> getRanges(Object query) {
			ranges = new ArrayList<Range>();
			apply(query);
			return ranges;
		}
	}
	public static Range parseRange(Bytes prefix, int opHash, List<Object> query) {
		Bytes term1 = null;
		Bytes term2 = null;
		boolean isExclusive1 = false;
		boolean isExclusive2 = false;
		switch(opHash) {
		case DbServiceUtil.OP_HASH_AS:
		case DbServiceUtil.OP_HASH_ANY:
			break;
		case DbServiceUtil.OP_HASH_GT:
			isExclusive1 = true;
		case DbServiceUtil.OP_HASH_GE:
			term1 = sencode(query.get(1));
			break;
		case DbServiceUtil.OP_HASH_LT:
			isExclusive2 = true;
		case DbServiceUtil.OP_HASH_LE:
			term2 = sencode(query.get(1));
			break;
		case DbServiceUtil.OP_HASH_BETWEEN_EE:
			isExclusive2 = true;
		case DbServiceUtil.OP_HASH_BETWEEN_EI:
			isExclusive1 = true;
		case DbServiceUtil.OP_HASH_BETWEEN_II:
			term1 = sencode(query.get(1));
			term2 = sencode(query.get(2));
			break;
		case DbServiceUtil.OP_HASH_BETWEEN_IE:
			isExclusive2 = true;
			term1 = sencode(query.get(1));
			term2 = sencode(query.get(2));
			break;
		default:
			throw new ApiException("QueryFormatError", "Unknown query operator: \""+query.get(0)+"\"");
		}
		return new Range(prefix, term1, term2, !isExclusive1, !isExclusive2);
	}
	public static void extractRangesFrom(Bytes prefix, Object pattern, ArrayList<Range> ranges) {
		if (pattern instanceof Map) {
			Map map = (Map)pattern;
			prefix = new CompoundBytes(prefix, HEADER_BYTE_MAPOPEN);
			if (map.isEmpty()) {
				ranges.add(new Range(prefix, null, null, true, true));
			} else {
				for(Object entryObj : map.entrySet()) {
					Map.Entry entry = (Map.Entry) entryObj;
					String key = (String)entry.getKey();
					Object value = entry.getValue();
					Bytes curPrefix = new CompoundBytes(prefix, sencodeMapKey(key));
					curPrefix = new CompoundBytes(curPrefix, HEADER_BYTE_COLON);
					extractRangesFrom(curPrefix, value, ranges);
				}
			}
		} else if (pattern instanceof List) {
			List<Object> list = (List<Object>)pattern;
			if (list.size() > 0 && SYMBOLS.contains(list.get(0).hashCode())) {
				int opHash = list.get(0).hashCode();
				switch(opHash) {
				case DbServiceUtil.OP_HASH_AS:
				case DbServiceUtil.OP_HASH_OR:
				case DbServiceUtil.OP_HASH_AND:
				case DbServiceUtil.OP_HASH_NOT:
					throw new ApiException("QueryFormatError", "Invalid narrowing operator: "+list.get(0));
				default:
					ranges.add(parseRange(prefix, opHash, list));
				}
			} else if (list.size() > 1) {
				throw new RuntimeException("malformed list structure in query: "+pattern);
			} else {
				prefix = new CompoundBytes(prefix, HEADER_BYTE_LISTOPEN);
				if (list.isEmpty()) {
					ranges.add(new Range(prefix, null, null, true, true));
				} else {
					extractRangesFrom(prefix, list.get(0), ranges);
				}
			}
		} else {
			Bytes term = new CompoundBytes(prefix,sencode(pattern));
			ranges.add(new Range(term, ArrayBytes.EMPTY_BYTES, ArrayBytes.EMPTY_BYTES, true, true));
		}
	}
	
	public static QueryNodeTemplate applyQueryToBagIndex(Bytes prefix, List query, BagIndex index, long revNum) {
		Map<String, QueryNode> vars = new HashMap<String, QueryNode>();
		QueryNode queryNode;
		ArrayList<QueryNode> clauses;
		if (query == null) {
			queryNode = index.getRange(Range.OPEN_RANGE, revNum);
		} else {
			int opHash = query.get(0).hashCode();
			int numArgs = query.size();
			switch(opHash) {
			case DbServiceUtil.OP_HASH_AS:
				queryNode = index.getRange(new Range(prefix, null, null, true, true), revNum);
				if (query.size() > 2) {
					throw new RuntimeException("Not yet supported");
//					QueryNodeTemplate subNode=applyQueryToBagIndex(prefix, (List)query.get(2), index, revNum);
//					vars.put((String)query.get(1), ExternalTermQueryNode.make(subNode.queryNode, queryNode));
//					queryNode = subNode.queryNode;
//					vars = subNode.varMappings;
				} else {
					vars.put((String)query.get(1), queryNode);
				}
				break;
			case DbServiceUtil.OP_HASH_OR:
			case DbServiceUtil.OP_HASH_AND:
				Object subQueryObject = query.get(1);
				if (!(subQueryObject instanceof List)) {
					throw new ApiException("QueryFormatError", "\""+query.get(0)+"\" operator must have a list in the first position, instead found: "+subQueryObject);
				}
				List<Object> subQueries = (List<Object>)query.get(1);
				int numQueries = subQueries.size();
				clauses = new ArrayList<QueryNode>(numQueries);
				for(int i=0; i<numQueries; i++) {
					QueryNodeTemplate node=applyPatternToBagIndex(prefix, subQueries.get(i), index, revNum);
					if (node.queryNode != null) clauses.add(node.queryNode);
					vars.putAll(node.varMappings);
				}
				if (opHash == DbServiceUtil.OP_HASH_OR) {
					queryNode = OrderedOrQueryNode.make(clauses);
				} else {
					queryNode = AndQueryNode.make(clauses);
				}
				break;
			case DbServiceUtil.OP_HASH_NOT:
				QueryNode matchesSoFar = index.getRange(new Range(prefix, null, null, true, true), revNum);
				clauses = new ArrayList<QueryNode>(numArgs-1);
				for(int i=1; i<numArgs; i++) {
					QueryNodeTemplate node=applyPatternToBagIndex(prefix, query.get(i), index, revNum);
					if (node.queryNode != null) clauses.add(node.queryNode);
				}
				QueryNode negativeMatches = OrderedOrQueryNode.make(clauses);
				queryNode = AndNotQueryNode.make(matchesSoFar, negativeMatches);
				break;
			default:
				queryNode = index.getRange(parseRange(prefix, opHash, query), revNum);
			}
		}
		return new QueryNodeTemplate(queryNode, vars);
	}

	public static Bytes sencodeListIndex(int index) {
		byte hiIdxByte = (byte)(index >> 8);
		byte loIdxByte = (byte)(index & 0xff);
		ArrayBytes idxBytes = new ArrayBytes(new byte[]{hiIdxByte,loIdxByte});
		return idxBytes;
	}

	public static int sdecodeListIndex(Bytes bytes, int indexPos) {
		return bytes.get(indexPos) << 8 | bytes.get(indexPos+1);
	}

	public static Object deepCopyResult(Object result) {
		if (result instanceof DbResultMapView) {
			return ((DbResultMapView)result).getDbResult().deepCopy();
		} else {
			return Util.jsonDecode(Util.jsonEncode(result));
		}
	}
	
	/*
	public static boolean applyPatternToObject(Object pattern, Object obj) {
		if (pattern instanceof Map) {
			if (!(obj instanceof Map)) return false;
			Map patternMap = (Map)pattern;
			Map objMap = (Map)obj;
			if (! objMap.keySet().containsAll(patternMap.keySet())) return false;
			for(Object patternEntryObj : patternMap.entrySet()) {
				Map.Entry entry = (Map.Entry) patternEntryObj;
				String key = (String)entry.getKey();
				Object subPattern = entry.getValue();
				if (! applyPatternToObject(subPattern, objMap.get(key))) return false;
			}
			return true;
		} else if (pattern instanceof List) {
			List list = (List)pattern;
			if (list.size() > 0 && SYMBOLS.contains(list.get(0).hashCode())) {
				return applyQueryToObject(list, obj);
			} else if (list.size() > 1) {
				throw new RuntimeException("malformed list structure in query: "+pattern);
			} else {
				if (!(obj instanceof List)) return false;
				if (list.isEmpty()) return true;
				Object subPattern = list.get(0);
				List listObj = (List)obj;
				boolean matches = false;
				for(Object subObj : listObj) {
					if (applyPatternToObject(subPattern, subObj)) {
						matches = true;
						break;
					}
				}
				return matches;
			}
		} else {
//			queryNode = index.getTerm(new CompoundBytes(prefix,sencode(pattern)), revNum);
		}
	}
*/

	// last parameter is scoring (and is optional)
	// default atomic constraint score->1, "AND"/"OR"->sum (even distribution)
	// bounds propagation:
	//  (bound)->min->(bound to every clause)
	//  (bound)->sum->(bound to every clause according to bound-(sum of other clause weights))
	//  (bound)->max->(no additional bounding)
	// Note that you can use bounds not only to drop entire posting lists but to find occurance combinations which are not productive
	//   or to think of it differently, the logic for traversing an "AND"/"OR" clause could take the score under consideration and artifically bump the doc_id before checking every clause
	// grouping is done elsewhere
	
	// primitives:
	// RANGE X Y X_EXCL Y_EXCL
	// EXCLUSIVE_MIN X
	// EXCLUSIVE_MAX X
	// MIN X
	// MAX X
	// NOT C

	// Sorting is done first with double values, then with an optional user-specified comparator, then by ID
	// These 3 values also constitute a bookmark
	
	// ["AND", [
	//          ["HAS_OTHER_THAN", "name", ["phil"]],   (default score is one) 
	//          ["MIN", "age", 14, ["SCORE_LINEAR_INTERPOLATE",[14,30,70],[0.0,0.8,1.0]]]
	//         ], ["SCORE_SUM", 3, 4] ]

	// Overreliance on JSON?  Really, it's just a store that groups a bunch of term strings together to make an object:
	//   (has age/)
	//   (hasany type/person, type/place, type/org)
	//   (min age/ 12)
	//   (range <prefix> <min> <max>)
	// or can i get away with finding some way to do a mapping?
	// A value indicated by path (numeric keys indicate a list, string keys a dict)
	// How do I indicate an empty list or dict? (I can't!)
	// but then i can't range search multivalue fields, right? type/*/
	
	// DDB core: dump sets of byte strings as documents into bags;
	// query using range(fieldname, prefix, minSuffix(optional), maxSuffix(optional))
	// complex query using some query syntax:
	//   ["AND", ["RANGE_MIN", "age", to_bin(14)], ["NOT", ["IN", "name", ["phil"]]]]

	// fulltext handling: term/rockies
	// What's the API?  
	//  - ddb.insert(["",..]), ddb.update(<conjunctive term list>,["",..]), ddb.remove([<op>,<arg>,..]), ddb.query([<op>,<arg>,..])
	//  - low level: commit([docid1,..], [["",..],..])
	//    fetchRange(revNum, t1, t2, excl1, excl2, [k1,k2],[0.1,0.5])  During linear interpolation, all keys must be of the same length  
	// 
	// How does this integrate with scripting?  Are there commit hooks?  
	// Scoring/sorting? reduce?
	
	
	
}

