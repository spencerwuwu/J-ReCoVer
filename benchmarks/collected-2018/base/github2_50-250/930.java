// https://searchcode.com/api/result/112629513/

package reuo.resources;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An abstract class for loading resources by a unique identifier. 
 * <p>
 * <b>Resource Meta-Information</b><br/>
 * Each implementation has a nested class that must extend Entry; which describes
 * the meta-information about a resource. The meta information is loaded separately
 * from the resource to reduce memory usage and provide a way to inspect the resource before
 * it's loaded.
 * <p>
 * <b>Performance Guidelines</b><br/>
 * The ResourceLoader abstraction attempts to provide a solid framework for accessing
 * resources effecently from a data source. Any resources returned by the implemented
 * {@link #get(int)} method should remain valid, but may still access the data source.
 * To conserve memory implementations should allow the garbage collector to determine
 * when a loaded resouce is no longer used; and re-load resources dynamically when
 * requested.
 * <p>
 * <b>Iterability</b><br/>
 * An iterator is provided for getting all of the valid identifiers in the data source. An iterator
 * can be requested explicitly using the {@link #iterator()} method or implicitly using a foreach
 * loop. The ResourceLoader is not backed by the iterator and any attempts to invoke a remove
 * will throw an exception.
 * @author Kristopher Ives, Lucas Green
 * @param <E> the Entry type used for the index
 * @param <R> the Resource type
 */
public abstract class ResourceLoader<E extends ResourceLoader.Entry, R> implements Iterable<Integer>{
	private Map<Integer, SoftReference<E>> entries = new HashMap<Integer, SoftReference<E>>();
	FileChannel entrySource = null;
	ByteBuffer entryBuffer = null;
	
	/**
	 * Initializes a ResourceLoader that loads entries from the provided
	 * data source.
	 * @param entrySource the channel for the entry index
	 * @param maxEntrySize the maximum size of an entry (in bytes)
	 */
	protected ResourceLoader(FileChannel entrySource, int maxEntrySize){
		this.entrySource = entrySource;
		
		entryBuffer = ByteBuffer.allocate(maxEntrySize);
		entryBuffer.order(ByteOrder.LITTLE_ENDIAN);
	}
	
	/**
	 * Gets the position in the data source the resource entry is stored
	 * at.
	 * @param id the resource entry identifier
	 * @return the position as an offset from the beginning of the data source
	 * in bytes
	 */
	protected abstract long getEntryOffset(int id) throws IndexOutOfBoundsException, IOException;
	
	/**
	 * Gets the size of an entry as stored in the data source.
	 * @param id the resource entry identifier
	 * @return the size of the entry (in bytes)
	 */
	protected abstract int getEntrySize(int id) throws IndexOutOfBoundsException, IOException;
	
	/**
	 * Gets the meta-information about a resource from data
	 * @param buffer the data
	 * @return the entry
	 */
	protected abstract E getEntryFromBuffer(ByteBuffer buffer);
	
	/**
	 * Gets an Entry for a resource by the resource identifier. The Entry class
	 * provides methods for checking the integrity of the entry.
	 * @param id the resource identifier
	 * @return the entry
	 * @throws IOException
	 */
	public E getEntry(int id) throws IOException{
		SoftReference<E> ref = entries.get(id);
		E entry = (ref != null) ? ref.get() : null;
		
		/* The entry needs to be loaded because either because
		 * the garbage collector has free'd it (the SoftReference
		 * is no longer valid) or it has never been loaded */
		if(entry == null){
			/* Seek in the file and pull the data buffer in */
			//entrySource.position(id * entryBuffer.limit());
			entrySource.position(getEntryOffset(id));
			
			entryBuffer.rewind();
			entryBuffer.limit(getEntrySize(id));
			entrySource.read(entryBuffer);
			entryBuffer.clear();
			
			/* Construct an entry and place it into the cache */
			entry = getEntryFromBuffer(entryBuffer);
			ref = new SoftReference<E>(entry);
			entries.put(id, ref);
		}
		
		return(entry);
	}
	
	//protected IllegalArgumentException outOfBounds
	
	/**
	 * Gets the amount of identifiable entries. This contains entries that
	 * may not be valid until loaded.
	 * @return the amount of identifiable entries
	 */
	public int getCapacity(){
		try{
			return((int)(entrySource.size() / entryBuffer.capacity()));
		}catch (IOException e){
			return(0);
		}
	}
	
	/**
	 * Gets an iteration of all the valid entries in the data source.
	 */
	public Iterator<Integer> iterator(){
		return(new ValidIndexIterator());
	}
	
	/**
	 * Gets a resource by it's identifier.
	 * @param id the resource identifier
	 * @return the resource
	 * @throws IllegalArgumentException if the identifier cannot address a resource
	 * @throws IOException if any IO operations fail
	 */
	public abstract R get(int id) throws IllegalArgumentException, IOException;
	
	/**
	 * Describes the meta-information about a resource.
	 */
	public abstract class Entry{
		/**
		 * Checks if the Entry is valid and can be loaded.
		 * @return true if the entry is valid
		 */
		public abstract boolean isValid();
	}
	
	/**
	 * An iteration of all the valid identifiers.
	 */
	private class ValidIndexIterator implements Iterator<Integer>{
		int index = -1;
		
		public boolean hasNext(){
			E entry = null;
			
			try{
				do{
					index++;
					entry = getEntry(index);
					
					if(index > getCapacity()){
						return false;
					}
				}while((entry == null) || !entry.isValid());
			} catch(IOException e) {
				e.printStackTrace();
			}
			
			return(true);
		}

		public Integer next(){
			return(index);
		}

		public void remove(){
			throw(new UnsupportedOperationException("This iterator is not backed by the ResourceLoader"));
		}
		
	}
}
