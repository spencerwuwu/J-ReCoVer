package reduce_test;

import java.util.ArrayList;


public class Context<T1, T2> {
	ArrayList<T1> keyList;
	ArrayList<T2> valueList;
	
	
	public Context(){
		 keyList = new ArrayList<T1>();
		 valueList = new ArrayList<T2>();
	}

	public void write(T1 key, T2 value){
		keyList.add(key);
		valueList.add(value);
		System.out.print("<"+key+","+value+">");
	}
	
	public ArrayList<T1> getKeyList() {
		return keyList;
	}
	public ArrayList<T2> getValueList() {
		return valueList;
	}

	public boolean equals(Object o){
	  if(o instanceof Context){
	    Context other =(Context) o;
	    if(keyList.size()!=other.keyList.size()){
          return false;
	    }else if(keyList.size()==0){
	      return true;
	    }

	    for(int i=0;i<keyList.size();i++){
          if(keyList.get(i)!=null && other.keyList.get(i)==null){
              return false;
          }else if (keyList.get(i)==null && other.keyList.get(i)!=null){
              return false;
          }else if(keyList.get(i)!=null && other.keyList.get(i)!=null && 
              !keyList.get(i).equals(other.keyList.get(i)))
              return false;
          
          if(valueList.get(i)!=null && other.valueList.get(i)==null){
              return false;
          }else if (valueList.get(i)==null && other.valueList.get(i)!=null){
              return false;
          }else if(valueList.get(i)!=null && other.valueList.get(i)!=null 
              && !valueList.get(i).equals(other.valueList.get(i)))
              return false;	    
	    }
	    return true;
	  }else{
	    return false;
	  }
	}
}
