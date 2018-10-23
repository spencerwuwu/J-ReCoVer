// https://searchcode.com/api/result/76496957/

package howto.database;


import howto.Utilities;

public class Relation {
	private String name = null;						//the name of the relation
	private String[] attributes = null;				//the set of attributes of the relation
	private Key[] relationKeys = null;				//a set of keys for the relation
	
	private boolean isIDB = false;					//is the relation an IDB or EDB
	
	private int[] unsafeIndexes = null;				//the indexes of unsafe variables
	
	private String selectionPredicate = null;	//selection predicates (where clause) that are used for partitioning the data.
	private String partitioningPredicate = null;	//predicates used for partitioning
	private int[] partitionIndexes = null;
	
	private boolean semiJoin = false;
	private String semiJoinQuery = null;
	
	private int status = 0;			//0 for regular relations, 1 for the replacement HDB, -1 for the original relation
	
	/**
	 * Basic constructor
	 */
	public Relation(){
		
	}
	
	/**
	 * Creates a new relation with the given name
	 * @param name		the name of the relation
	 */
	public Relation(String name){
		this.name = name;
	}
	
	/**
	 * Creates a relation with the given name and attributes
	 * @param name			the name of the relation
	 * @param attributes	the attributes of the relation
	 */
	public Relation(String name, String[] attributes){
		this(name);
		this.attributes = new String[attributes.length];
		System.arraycopy(attributes, 0, this.attributes, 0, attributes.length);
	}
	
	/**
	 * Creates a relation with the given name and attributes
	 * @param name			the name of the relation
	 * @param attributes	the attributes of the relation
	 */
	public Relation(String name, String[] attributes, int status){
		this(name, attributes);
		this.status = status;
	}
	
	/**
	 * Gets the relation name
	 * @return	the relation name
	 */
	public String getName(){
		return this.name;
	}
	
	/**
	 * Gets the list of all attributes of the relation
	 * @return	a list of attributes
	 */
	public String[] getAttributes(){
		return this.attributes;
	}
	
	/**
	 * Checks if a relation contains an attribute of the given name
	 * @param name		the name of the attribute
	 * @return			true if the attribute exists in the relation, false if not
	 */
	public boolean hasAttribute(String name){
		if(Utilities.findStringInArray(attributes, name) >= 0)
			return true;
		else
			return false;
	}
	
	/**
	 * Returns the ith attribute of the relation
	 * @param i		the index of the wanted attribute
	 * @return		the attribute at index i
	 */
	public String getAttribute(int i){
		if(i < 0 || i > this.getNumOfAttributes() - 1)
			return null;
		
		return this.attributes[i];
	}
	
	/**
	 * Returns the index of the attribute with the given name
	 * @param name		the name of the attribute
	 * @return			the index of the attribute, -1 if it's not there.
	 */
	public int getAttributeIndex(String name){
		if(this.attributes == null)
			return -1;
		
		for(int i = 0; i < this.attributes.length; i++){
			String currentAttr = this.attributes[i];
			if(Utilities.isArithmeticExpression(currentAttr)){
				String[] simpleAttr = Utilities.getArithmeticElements(currentAttr);
				
				for(int j = 0; j < simpleAttr.length; j++){
					if(simpleAttr[j].equals(name) && !Utilities.isNumeric(simpleAttr[j]))
						return i;		//if any attribute in the set matches, return it.
				}
				
			}
			else{
				if(currentAttr.equals(name))
					return i;
			}
		}
		
		return Utilities.findStringInArray(this.attributes, name);
	}
	
	/**
	 * Gets the total number of attributes of the relation
	 * @return	the number of attributes in the relation
	 */
	public int getNumOfAttributes(){
		if(this.attributes == null)
			return 0;
		
		return this.attributes.length;
	}
	
	/**
	 * Get the total number of keys for this relation
	 * @return	the number of keys
	 */
	public int getNumOfKeys(){
		if(this.relationKeys == null)
			return 0;
		
		return this.relationKeys.length;
	}
	
	/**
	 * Adds the specified key to the relation. Ignores the addition, if the same key already exists.
	 * @param key	the key to add
	 */
	public void addKey(Key key){
		if(this.hasKey(key))
			return;					//key already there, do nothing
		
		int numOfKeys = this.getNumOfKeys();
		Key[] newKeys = new Key[numOfKeys + 1];
		
		if(numOfKeys > 0){
			System.arraycopy(this.relationKeys, 0, newKeys, 0, numOfKeys);
		}
		
		newKeys[numOfKeys] = key;
		
		this.relationKeys = new Key[numOfKeys + 1];
		System.arraycopy(newKeys, 0, this.relationKeys, 0, numOfKeys + 1);
	}
	
	/**
	 * Adds a new key to the set of keys. Ignores the addition, if the same key already exists.
	 * @param attr	the set of attributes comprising the new key
	 */
	public void addKey(String[] attr){
		Key key = new Key(attr);
		this.addKey(key);
	}
	
	/**
	 * Adds a new key on the attributes corresponding to the given indexes. Ignores the addition, 
	 * if the same key already exists.
	 * @param attrIndexes	the indexes of relation attributes to form a key
	 */
	public void addKey(int[] attrIndexes){
		String[] attr = new String[attrIndexes.length];
		
		for(int i = 0; i< attrIndexes.length; i++){
			if(attrIndexes[i] < 0 || attrIndexes[i] > this.getNumOfAttributes() - 1)
				return;
			
			attr[i] = this.attributes[attrIndexes[i]];
		}
		
		this.addKey(attr);
	}
	
	/**
	 * Checks if the relation already has a certain key stored
	 * @param key	the key to look for
	 * @return		true if the key already exists, false if not
	 */
	public boolean hasKey(Key key){
		for(int i = 0; i < this.getNumOfKeys(); i++){
			if(this.relationKeys[i].equals(key))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Sanitizes the name of the relation. If the relation contains any unsafe variables,
	 * the name returned is preceded by "CORE_", otherwise the actual relation name is returned
	 * @param unsafe	a list of unsafe attributes
	 * @return			the name of the relation, preceded by "CORE_" if the relation contains
	 * 					unsafe attributes
	 */
	public String getSafeName(){
		if(this.isIDB)
			return "CORE_" + this.name;
//		if(this.hasUnsafe())
//			return "CORE_" + this.name;
		else
			return this.name;
	}
	
	/**
	 * Checks whether a relation contains attributes from the given list
	 * @param attr	a list of attributes to compare against the relation's
	 * @return		true if the relation contains any of the attributes, false otherwise
	 */
	public boolean containsAttributes(String[] attr){
		String[] myUnsafe = Utilities.intersection(this.attributes, attr);
		
		if(myUnsafe == null || myUnsafe.length < 1)
			return false;
		else
			return true;
	}
	
	public String toString(){
		if(this.name == null)
			return null;
		
		if(this.attributes == null)
			return this.name;
		
		String s = this.name + "(" + this.attributes[0];
		for(int i = 1; i < this.getNumOfAttributes(); i++){
			s = s + ", " + this.attributes[i];
		}
		s = s + ")";
		
		
		for(int i = 0; i < this.getNumOfKeys(); i++){
			s = s + " : ";
			s = s + this.relationKeys[i].toString() ;
		}
		
		return s;
	}
	
	/**
	 * Declares a set of attribute names unsafe
	 * @param s
	 */
	public void declareUnsafe(String[] s){
		if(s == null)
			return;
		
		for(int i = 0; i < s.length; i++){
			this.declareUnsafe(this.getAttributeIndex(s[i]));
		}
	}
	
	
	/**
	 * Declares an attribute as unsafe based on position.
	 * @param unsafe		the index of the unsafe variable
	 */
	public void declareUnsafe(int unsafe){
		if(unsafe >= this.getNumOfAttributes() || unsafe < 0)				//the attribute index exceeds the attribute array
			return;
		
		if(this.unsafeIndexes == null){
			this.unsafeIndexes = new int[1];
			this.unsafeIndexes[0] = unsafe;
			return;
		}
		
		for(int i = 0; i < this.unsafeIndexes.length; i++){
			if(this.unsafeIndexes[i] == unsafe)
				return;
		}
		
		this.unsafeIndexes = Utilities.addToArray(this.unsafeIndexes, unsafe);
	}
	
	public int[] getUnsafe(){
		return this.unsafeIndexes;
	}
	
	/**
	 * Checks if the relation contains unsafe attributes (as they have been declared)
	 * Unsafe attributes need to be explicitly declared for the relation first. The method 
	 * only checks the stored unsafe array
	 * @return		true if there is at least one unsafe attribute, false otherwise.
	 */
	public boolean hasUnsafe(){
		if(this.unsafeIndexes == null || this.unsafeIndexes.length < 1)
			return false;
		else
			return true;
	}
	
	/**
	 * Retrieves the list of safe attribute names
	 * @return
	 */
	public String[] getSafeAttributes(){
		String[] unsafe = Utilities.getElements(this.attributes, this.getUnsafe());
		return Utilities.difference(this.attributes, unsafe);
	}
	
	/**
	 * Retrieves the list of unsafe attribute names
	 * @return
	 */
	public String[] getUnsafeAttributes(){
		String[] unsafe = Utilities.getElements(this.attributes, this.getUnsafe());
		return unsafe;
	}
	
	public Key getKey(int i){
		if(i >= 0 && i < this.getNumOfKeys())
			return this.relationKeys[i];
		
		return null;
	}
	
	public void isIDB(){
		this.isIDB = true;
	}
	
	public String getPartitioningPredicate(){
		return this.partitioningPredicate;
	}
	
	public void setPartitioningPredicate(String s){
		this.partitioningPredicate = s;
	}
	
	public String getSelectionPredicate(){
		return this.selectionPredicate;
	}
	
	public void setSelectionPredicate(String s){
		this.selectionPredicate = s;
	}
	
	public int[] getPartitionAttributes(){
		return this.partitionIndexes;
	}
	
	/**
	 * Updates the partition variables of the relation, to the intersection of the given set, and 
	 * the one already stored.
	 * @param indexes	the new set
	 * @return			0 if the update is valid and does not reduce the one stored. 
	 * 					-1 if the update is invalid (impossible partitioning). 
	 * 					1 if the set is updated successfully but its size is reduced.
	 * 					2 if the set is updated for the first time
	 */
	public int updatePartitionAttributes(int[] indexes){
		
		if(Utilities.findInArray(indexes, -1) >= 0){
			this.partitionIndexes = null;
			return -1;
		}
		
		if(this.partitionIndexes == null){
			this.partitionIndexes = indexes;
			return 2;
		}
		
		int setSize = this.partitionIndexes.length;
		
		this.partitionIndexes = Utilities.intersection(this.partitionIndexes, indexes);
		
		if(this.partitionIndexes == null || this.partitionIndexes.length < 1){
			return -1;
		}
		else if (this.partitionIndexes.length < setSize){ //set has been reduced
			return 1;
		}
		else{
			return 0;
		}
	}
	
	/**
	 * Resets the values of the partitioning data to NULL
	 */
	public void resetPartitioning(){
		this.partitionIndexes = null;
		this.partitioningPredicate = null;
		this.semiJoin = false;
		this.semiJoinQuery = null;
	}
	
	/**
	 * updates the partition predicate based on the given values. It creates equality predicates.
	 * @param values
	 */
	public void updatePartitionPredicate(String[] values){
		if(this.partitionIndexes == null || this.partitionIndexes.length < 1 || this.partitionIndexes[0] < 0)
			return;
		
		this.partitioningPredicate = this.getSafeName() + "." + this.attributes[this.partitionIndexes[0]] 
		                                  + " = " +  values[0];
		
		for(int i = 1; i < this.partitionIndexes.length; i++){
			this.partitioningPredicate = this.partitioningPredicate + " AND " + 
										this.getSafeName() + "." + this.attributes[this.partitionIndexes[i]] 
			 		                    + " = " + values[i];
		}
	}
	
	/**
	 * Updates the partition predicate given lower bound and upper bound values. It creates inequality predicates
	 * @param lowerBound
	 * @param upperBound
	 */
	public void updatePartitionPredicate(String[] lowerBound, String[] upperBound){
		if(this.partitionIndexes == null || this.partitionIndexes.length < 1 || this.partitionIndexes[0] < 0)
			return;
		
		this.partitioningPredicate = this.getSafeName() + "." + this.attributes[this.partitionIndexes[0]] 
		                                  + " >= " +  lowerBound[0];
		
		if(upperBound != null){
			this.partitioningPredicate = this.partitioningPredicate + " AND " + this.getSafeName() + "."
			                          	+ this.attributes[this.partitionIndexes[0]] + " <= " +  upperBound[0];
		}
		
		for(int i = 1; i < this.partitionIndexes.length; i++){
			this.partitioningPredicate = this.partitioningPredicate + " AND " + 
										 this.getSafeName() + "." + this.attributes[this.partitionIndexes[i]] 
			 		                                  + " >= " +  lowerBound[i] ;
			
			if(upperBound != null){
				this.partitioningPredicate = this.partitioningPredicate + " AND " + this.getSafeName() + "."
				                          	+ this.attributes[this.partitionIndexes[i]] + " <= " +  upperBound[i];
			}
		}
	}
	
	public String getPredicate(){
		String s1;
		
		if(this.selectionPredicate == null){
			s1 = "";
		}
		else{
			s1 = this.selectionPredicate;
		}
		
		String s2;
		
		if(this.partitioningPredicate == null){
			s2 = "";
		}
		else{
			s2 = this.partitioningPredicate;
		}
		
		if(s1.equals(""))
			return s2;
		
		if(s2.equals(""))
			return s1;
		
		return s1 + " AND " + s2;
	}
	
	public void setAttribute(String s, int i){
		if(i < 0 || this.getNumOfAttributes() < 1 || i >= this.getNumOfAttributes())
			return;
		
		this.attributes[i] = s;
	}
	
	/**
	 * Retrieves a semijoin statements (reduces the size of the relation to only the relevant tuples)
	 * @param st
	 * @param s
	 * @return
	 */
	public String getSemiJoinStatement(DatalogStatement st, Schema s){
		String statement = "";
		
		//get the selest statement (attributes of relation)
//		String select = this.name + "." + this.getAttribute(0);
//		for(int i = 1; i < this.getNumOfAttributes(); i++){
//			select = select + ", " + this.name + "." + this.getAttribute(i);
//		}
//		
//		select = select + ", " + this.name + ".tid";
		
		//actually we only need the tid!
		String select = this.name + ".tid";
		
		//get the from statement (list of statement relations in the tail
		Atomic myTail = (Atomic) st.getTail();
		String from  =  myTail.getRelation(0).getSafeName();
		
		for(int i = 1; i < myTail.getNumOfRelations(); i++){
			from = from + ", " + myTail.getRelation(i).getSafeName();
		}
		
		String where = st.getWhereClauseWithPartitioning(s);
		
		statement = "SELECT " + select + " FROM " + from + where;
		
		return statement;
	}
	
	public void setSemiJoin(){
		this.semiJoin = true;
	}
	
	public boolean hasSemiJoin(){
		return this.semiJoin;
	}
	
	public void setSemiJoinQuery(String s){
		this.semiJoinQuery = s;
	}
	
	public String getSemiJoinQuery(){
		return this.semiJoinQuery;
	}
	
	/**
	 * Determines whether the relation is a replacement for another
	 * @return
	 */
	public int getStatus(){
		return this.status;
	}
}

