// https://searchcode.com/api/result/16713576/

package com.cloudant.indexers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.cloudant.couchdbjavaserver.*;
import com.cloudant.index.CouchIndexUtils;
import com.cloudant.index.IndexType;
import com.cloudant.index.IndexUtilities;
import com.cloudant.index.SingleDocumentIndex;
import com.stinkyteddy.utils.StDateUtils;

import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.util.*;

public class MyCustomSearch implements SearchView {
	

	private Map<String,FieldOption> fieldsToStore = null;
	private Map<Pattern, String> regexps = null;
	private Map<String, FieldOption> matches = null;
	private HashSet<String> misses = null;
	private Analyzer analyzer = null;
	private List<SimpleDateFormat> dateFormats = null;
		
	public Analyzer getAnalyzer() {
		if (analyzer == null) {
			return new StandardAnalyzer(Version.LUCENE_30);
		} else {
			return analyzer;
		}
	}
	
	public void setAnalyzer(Analyzer in) {
		analyzer = in;
	}
	
	public void Log(String message) {

	}

	public JSONArray MapDoc(JSONObject doc) {
		JSONArray out = new JSONArray();
		try {
			Analyzer analyzer = getAnalyzer();
			SingleDocumentIndex index = new SingleDocumentIndex();
			// user specified field set

			
			Map<String, List<Object>> mapped = CouchIndexUtils.MapJSONObject2Object(doc, null);
			if (mapped == null) {
//				Log("can't map json doc");
				return out.put(new JSONArray());
			}
			for (String field : mapped.keySet()) {
				try {
					FieldOption fo = findField(field);
					if (fo == null) continue;
					List<Object> l = mapped.get(field);
					Iterator<Object> iter = l.iterator();
					while (iter.hasNext()) {
						Object o = iter.next();
						if (o == null) continue;
						final IndexType type = fo.getType();
						final float boost = (float)fo.getBoost();
						String luceneName = fo.getFieldName();
						if (luceneName == null) luceneName = field;
						switch (type) {
						case STRING: index.addField(luceneName, IndexUtilities.ObjectToString(o), analyzer, boost);break;
						case KEYWORD: index.addField(luceneName, IndexUtilities.ObjectToString(o), boost);break;
						case BOOLEAN: index.addField(luceneName, o, boost);break;
						// JSON doesn't distinguish between int and long, always returns long
						case INT: index.addField(luceneName, (Long)o, boost);break;
						case LONG: index.addField(luceneName, (Long)o, boost);break;
						case FLOAT: index.addField(luceneName, (Float)o, boost);break;
						case DOUBLE: index.addField(luceneName, (Double)o, boost);break;
						case DATE: {
							Date d = null;
							d = parseDate((String)o);
							if (d != null) {
								index.addField(luceneName, d.getTime(), boost);break;
							} else {
								index.addField(luceneName, IndexUtilities.ObjectToString(o), boost);break;								
							}
						}
						case JSONOBJECT: index.addField(luceneName, o, boost);break;
						case NULL: index.addField(luceneName, (Object)JSONObject.NULL, boost);break;
						}
					}
				} catch (Exception e) {
					/* probably a casting exception */
				}
			}
			out = index.jsonMap();
		} catch (Exception e) {
			out.put(new JSONArray());
			if (e != null ) {
				Log("Map Exception: " + e.toString());
			}
		}
		return out;
	}

	public FieldOption findField(String field) {
		if (fieldsToStore == null || field == null) return null;
		if (misses.contains(field)) return null;
		if (fieldsToStore.containsKey(field)) return fieldsToStore.get(field);
		if (matches.containsKey(field)) return matches.get(field);
		if (regexps == null) return null;
		for (Pattern p: regexps.keySet()) {
			Matcher m = p.matcher(field);
			if (m != null && m.matches()) {
				final FieldOption fo = fieldsToStore.get(regexps.get(p));
				matches.put(field, fo);
				return fo;
			}
		}
		misses.add(field);
		return null;
	}

	/**
	 * This configures the list of fields to index.
	 * 
	 * @param A
	 *            
	 *            json object of the form {"fields":[{"name":"field_name","lucenename":"name_in_index","boost":1.0,"type":"index_type",regexp:true/false,"dateformat":"java_simple_date_fromat"}, ...],"analyzer":"name_of_analyzer"} where
	 *            "name" specifies the json field to be indexed (can be regular expression, see below).
	 *            "lucenename" is the name of the field in the Cloudant "Lucene" index
	 *            the boosts are the weights applied to each field. Boosts are
	 *            optional (default is 1.0) 
	 *            for a list of index types, see com.javaviews.IndexType.java. "string","date","long","float","object" etc.. 
	 *            string types are run through the specified analyzer, where they are tokenized accordingly.
	 *            object types are index as json objects with no modification. This is used for numbers.
	 *            date types are converted to unix time when indexed and stored as a number
	 *            "regexp" tells whether to parese the "name" as a regular expression.
	 */
	public void Configure(String config) {
		matches = new HashMap<String, FieldOption>();
		misses = new HashSet<String>();
		if (config == null || config.length() == 0) {
			fieldsToStore = new HashMap<String, FieldOption>();
			fieldsToStore.put(".*",new FieldOption(null,IndexType.STRING,1.0,true));
			return;
		}
		try {
	 		JSONObject configString = new JSONObject(config);
			fieldsToStore = new HashMap<String, FieldOption>();
			JSONArray fields = configString.optJSONArray("fields");
			if (fields == null) {
				// in this case, we want to map everything as a string using the json names
				fieldsToStore.put(".*",new FieldOption(null,IndexType.STRING,1.0,true));
			} else {
				for (int i = 0; i < fields.length(); i++) {
					JSONObject thisField = fields.getJSONObject(i);
					String name = thisField.getString("name");
					String luceneName = thisField.optString("lucenename",name);
					if (luceneName == null) name = null;
					double boost = thisField.optDouble("boost", 1.0);
					boolean regexp = thisField.optBoolean("regexp", false);
					String type = thisField.optString("type","string");
					IndexType t = IndexType.getIndexTypeFromString(type);
					if (t == IndexType.DATE) {
						try {
							String f = thisField.getString("dateformat");
							if (f != null) {
								SimpleDateFormat d = new SimpleDateFormat(f);
								dateFormats.add(d);
							}
						} catch (IllegalArgumentException ie) {
							/* invalid date format */
						} catch (JSONException je) {
							/* no specified */
						}
					}
					fieldsToStore.put(name, new FieldOption(luceneName, t,boost,regexp));
					if (regexp == true) {
						if (regexps == null) regexps = new HashMap<Pattern, String>();
						regexps.put(Pattern.compile(name), name);
					}
				}
			}
		} catch (JSONException je) {
			Log("CustomSearch.Configure error parsing configuration string: " + config);
		}
		return;		
	}

	
	public class FieldOption {
		private IndexType type;
		private double boost;
		private String fieldName;
		private boolean isRegexp;
		public FieldOption(String fieldName, IndexType type, float boost) {
			this.setType(type);
			this.setBoost(boost);
			this.setFieldName(fieldName);
			this.setRegexp(false);
		}
		public FieldOption(String fieldName, IndexType type, double boost, boolean regexp) {
			this.setType(type);
			this.setBoost(boost);
			this.setFieldName(fieldName);
			this.setRegexp(regexp);
		}
		public void setType(IndexType type) {
			this.type = type;
		}
		public IndexType getType() {
			return type;
		}
		public void setBoost(double boost) {
			this.boost = boost;
		}
		public double getBoost() {
			return boost;
		}
		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}
		public String getFieldName() {
			return fieldName;
		}
		public void setRegexp(boolean isRegexp) {
			this.isRegexp = isRegexp;
		}
		public boolean isRegexp() {
			return isRegexp;
		}
		
	}

	@Override
	public JSONArray ReReduce(JSONArray reduceResults) {
		// rereduce not implemented
		return new JSONArray();
	}

	@Override
	public JSONArray Reduce(JSONArray mapResults) {
		// reduce not implemented
		return new JSONArray();
	}
	
	public Date parseDate(String input) {
		Date d = null;
		if (dateFormats != null) {
			for (SimpleDateFormat f : dateFormats) {
				try {
					d = f.parse(input);
				} catch (ParseException e) {
					/* wrong format */
				}
			}
		}
		// now try some standard formats
		if (d == null) d = StDateUtils.parseDateString(input);
		return d;
	}
}

