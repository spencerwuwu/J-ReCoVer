// https://searchcode.com/api/result/67372114/

package it.netgrid.gwt.couchdb.options;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayBoolean;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsArrayString;

public class ViewQueryOptions extends JavaScriptObject {
	protected ViewQueryOptions() {}
	
	public final native String getKey()/*-{
		return this.key;
	}-*/;
	
	public final native void setKey(String key) /*-{
		this.key = key;
	}-*/;
	
	public final native void setKey(JsArrayMixed key) /*-{
		this.key = key;
	}-*/;
	
	public final native void setKey(JsArrayString key) /*-{
		this.key = key;
	}-*/;

	public final native JavaScriptObject getKeyObject() /*-{
		return this.key;
	}-*/;
	
	public final native void setKeyObject(JavaScriptObject object) /*-{
		this.key = object;
	}-*/;
	
	public final native JsArrayString getKeys() /*-{
		return this.keys;
	}-*/;
	
	public final native void setKeys(JsArrayString keys) /*-{
		this.keys = keys;
	}-*/;
	
	public final native void setStartKey(JsArrayInteger startKey) /*-{
		this.startkey = startKey;
	}-*/;
	
	public final native void setStartKey(JsArrayBoolean startKey) /*-{
		this.startkey = startKey;
	}-*/;
	
	public final native void setStartKey(JsArrayNumber startKey) /*-{
		this.startkey = startKey;
	}-*/;
	
	public final native void setStartKey(JsArrayMixed startKey) /*-{
		this.startkey = startKey;
	}-*/;
	
	public final native void setStartKey(JsArrayString startKey) /*-{
		this.startkey = startKey;
	}-*/;
	
	public final native String getStartkey()/*-{
		return this.startkey;
	}-*/;
	
	public final native void setStartkey(String startkey) /*-{
		this.startkey = startkey;
	}-*/;
	
	public final native void setStartkeyDocId(String key) /*-{
		this.startkey_docid = key;
	}-*/;
	
	public final native String getStartkeyDocId()/*-{
		return this.startkey_docid;
	}-*/;
	
	public final native String getEndkey() /*-{
		return this.endkey;
	}-*/;
	
	public final native void setEndkey(String key) /*-{
		this.endkey = key;
	}-*/;
	
	public final native void setEndKey(JsArrayString endKey) /*-{
		this.endkey = endKey;
	}-*/;

	public final native void setEndKey(JsArrayInteger endKey) /*-{
		this.endkey = endKey;
	}-*/;
	
	public final native void setEndKey(JsArrayBoolean endKey) /*-{
		this.endkey = endKey;
	}-*/;
	
	public final native void setEndKey(JsArrayNumber endKey) /*-{
		this.endkey = endKey;
	}-*/;
	
	public final native void setEndKey(JsArrayMixed endKey) /*-{
		this.endkey = endKey;
	}-*/;
	
	public final native String getEndkeyDocId() /*-{
		return this.endkey_docid;
	}-*/;
	
	public final native void setEndkeyDocId(String key) /*-{
		this.endkey_docid = key;
	}-*/;
	
	public final native int getLimit()/*-{
		return this.limit;
	}-*/;
	
	public final native void setLimit(int limit)/*-{
		this.limit = limit;
	}-*/;
	
	public final native String getStale()/*-{
		return this.stale;
	}-*/;
	
	public final native void setStale(String stale) /*-{
		this.stale = stale;
	}-*/;
	
	public final native boolean isDescending() /*-{
		return this.descending;
	}-*/;
	
	public final native void setDescending(boolean descending) /*-{
		this.descending = descending;
	}-*/;
	
	public final native int getSkip() /*-{
		return this.skip;
	}-*/;
	
	public final native void setSkip(int skip) /*-{
		this.skip = skip;
	}-*/;
	
	public final native boolean isGrouping() /*-{
		return this.group;
	}-*/;
	
	public final native void setGrouping(boolean enabled) /*-{
		this.group = enabled;
	}-*/;
	
	public final native int getGroupLevel() /*-{
		return this.group_level;
	}-*/;
	
	public final native void setGroupLevel(int level) /*-{
		this.group_level = level;
	}-*/;
	
	public final native boolean isReducing()/*-{
		return this.reduce;
	}-*/;
	
	public final native void setReducing(boolean enable) /*-{
		this.reduce = enable;
	}-*/;
	
	public final native String getReduceFunction() /*-{
		return this.reduce;
	}-*/;
	
	public final native void setReduceFunction(String reduce) /*-{
		this.reduce = reduce;
	}-*/;
	
	public final native boolean isIncludeDocs() /*-{
		return this.include_docs;
	}-*/;
	
	public final native void setIncludeDocs(boolean enabled) /*-{
		this.include_docs = enabled;
	}-*/;
	
	public final native boolean isInclusiveEnd()/*-{
		return this.inclusive_end;
	}-*/;
	
	public final native void setInclusiveEnd(boolean enabled) /*-{
		this.inclusive_end = enabled;
	}-*/;
	
	public final native boolean isUpdateSeq() /*-{
		return this.update_seq;
	}-*/;
	
	public final native void setUpdateSeq(boolean enabled) /*-{
		this.update_seq = enabled;
	}-*/;

}

