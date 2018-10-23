package reduce_test;

import java.util.ArrayList;


public class Context<T1, T2> {
	ArrayList<T1> keyList;
	ArrayList<String> valueList;
	
	
	public Context(){
		 keyList = new ArrayList<T1>();
		 valueList = new ArrayList<String>();
	}

	public void write(T1 key, T2 value){
		keyList.add(key);
		if (value != null) valueList.add(value.toString());
        else valueList.add(null);
	}
	
	public ArrayList<T1> getKeyList() {
		return keyList;
	}
	public ArrayList<String> getValueList() {
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
