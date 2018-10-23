// https://searchcode.com/api/result/11487983/

package net.rubyeye.xmemcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ReconnectRequest;
import net.rubyeye.xmemcached.networking.Connector;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.Protocol;

/**
 * The memcached client's interface
 * 
 * @author dennis
 * 
 */
public interface MemcachedClient {

	/**
	 * Default thread number for reading nio's receive buffer and dispatch
	 * commands.Recommend users to set it equal or less to the memcached
	 * server's number on linux platform,keep default on windows.Default is 0.
	 */
	public static final int DEFAULT_READ_THREAD_COUNT = 0;

	/**
	 * Default TCP keeplive option,which is true
	 */
	public static final boolean DEFAULT_TCP_KEEPLIVE = true;
	/**
	 * Default connect timeout,1 minutes
	 */
	public static final int DEFAULT_CONNECT_TIMEOUT = 60000;
	/**
	 * Default socket's send buffer size,8k
	 */
	public static final int DEFAULT_TCP_SEND_BUFF_SIZE = 32 * 1024;
	/**
	 * Disable Nagle algorithm by default
	 */
	public static final boolean DEFAULT_TCP_NO_DELAY = true;
	/**
	 * Default session read buffer size,16k
	 */
	public static final int DEFAULT_SESSION_READ_BUFF_SIZE = 128 * 1024;
	/**
	 * Default socket's receive buffer size,16k
	 */
	public static final int DEFAULT_TCP_RECV_BUFF_SIZE = 64 * 1024;
	/**
	 * Default operation timeout,if the operation is not returned in 1
	 * second,throw TimeoutException
	 */
	public static final long DEFAULT_OP_TIMEOUT = 1000L;
	/**
	 * With java nio,there is only one connection to a memcached.In a high
	 * concurrent enviroment,you may want to pool memcached clients.But a
	 * xmemcached client has to start a reactor thread and some thread pools,if
	 * you create too many clients,the cost is very large. Xmemcached supports
	 * connection pool instreadof client pool.you can create more connections to
	 * one or more memcached servers,and these connections share the same
	 * reactor and thread pools,it will reduce the cost of system.Default pool
	 * size is 1.
	 */
	public static final int DEFAULT_CONNECTION_POOL_SIZE = 1;

	/**
	 * Default session idle timeout,if session is idle,xmemcached will do a
	 * heartbeat action to check if connection is alive.
	 */
	public static final int DEFAULT_SESSION_IDLE_TIMEOUT = 5000;

	/**
	 * Set the merge factor,this factor determins how many 'get' commands would
	 * be merge to one multi-get command.default is 150
	 * 
	 * @param mergeFactor
	 */
	public abstract void setMergeFactor(final int mergeFactor);

	/**
	 * Get the connect timeout
	 * 
	 * @param connectTimeout
	 */
	public abstract long getConnectTimeout();

	/**
	 * Set the connect timeout,default is 1 minutes
	 * 
	 * @param connectTimeout
	 */
	public abstract void setConnectTimeout(long connectTimeout);

	/**
	 * return the session manager
	 * 
	 * @return
	 */
	public abstract Connector getConnector();

	/**
	 * Enable/Disable merge many get commands to one multi-get command.true is
	 * to enable it,false is to disable it.Default is true.Recommend users to
	 * enable it.
	 * 
	 * @param optimizeGet
	 */
	public abstract void setOptimizeGet(final boolean optimizeGet);

	/**
	 * Enable/Disable merge many command's buffers to one big buffer fit
	 * socket's send buffer size.Default is true.Recommend true.
	 * 
	 * @param optimizeMergeBuffer
	 */
	public abstract void setOptimizeMergeBuffer(
			final boolean optimizeMergeBuffer);

	/**
	 * @return
	 */
	public abstract boolean isShutdown();

	/**
	 * Aadd a memcached server,the thread call this method will be blocked until
	 * the connecting operations completed(success or fail)
	 * 
	 * @param server
	 *            host string
	 * @param port
	 *            port number
	 */
	public abstract void addServer(final String server, final int port)
			throws IOException;

	/**
	 * Add a memcached server,the thread call this method will be blocked until
	 * the connecting operations completed(success or fail)
	 * 
	 * @param inetSocketAddress
	 *            memcached server's socket address
	 */
	public abstract void addServer(final InetSocketAddress inetSocketAddress)
			throws IOException;

	/**
	 * Add many memcached servers.You can call this method through JMX or
	 * program
	 * 
	 * @param host
	 *            String like [host1]:[port1] [host2]:[port2] ...
	 */
	public abstract void addServer(String hostList) throws IOException;

	/**
	 * Get current server list.You can call this method through JMX or program
	 */
	public abstract List<String> getServersDescription();

	/**
	 * Remove many memcached server
	 * 
	 * @param host
	 *            String like [host1]:[port1] [host2]:[port2] ...
	 */
	public abstract void removeServer(String hostList);

	/**
	 * Set the nio's ByteBuffer Allocator,use SimpleBufferAllocator by default.
	 * 
	 * 
	 * @param bufferAllocator
	 * @return
	 */
	@Deprecated
	public abstract void setBufferAllocator(
			final BufferAllocator bufferAllocator);

	/**
	 * Get value by key
	 * 
	 * @param <T>
	 * @param key
	 *            Key
	 * @param timeout
	 *            Operation timeout,if the method is not returned in this
	 *            time,throw TimeoutException
	 * @param transcoder
	 *            The value's transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> T get(final String key, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> T get(final String key, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> T get(final String key, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> T get(final String key) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Just like get,But it return a GetsResponse,include cas value for cas
	 * update.
	 * 
	 * @param <T>
	 * @param key
	 *            key
	 * @param timeout
	 *            operation timeout
	 * @param transcoder
	 * 
	 * @return GetsResponse
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> GetsResponse<T> gets(final String key,
			final long timeout, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> GetsResponse<T> gets(final String key)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> GetsResponse<T> gets(final String key,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	@SuppressWarnings("unchecked")
	public abstract <T> GetsResponse<T> gets(final String key,
			final Transcoder transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * memcached?getMulti?????????key??????
	 * 
	 * @param <T>
	 * @param keyCollections
	 *            ?????
	 * @param timeout
	 *            ??????
	 * @param transcoder
	 *            ???????????
	 * @return map???map??????????????????????map???
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> Map<String, T> get(
			final Collection<String> keyCollections, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> Map<String, T> get(
			final Collection<String> keyCollections,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> Map<String, T> get(
			final Collection<String> keyCollections) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> Map<String, T> get(
			final Collection<String> keyCollections, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * ??getMulti?????????cas?????map?value????GetsResponse??
	 * 
	 * @param <T>
	 * @param keyCollections
	 * @param timeout
	 * @param transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections, final long timeout,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> Map<String, GetsResponse<T>> gets(
			final Collection<String> keyCollections,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Store key-value item to memcached
	 * 
	 * @param <T>
	 * @param key
	 *            stored key
	 * @param exp
	 *            expire time
	 * @param value
	 *            stored data
	 * @param transcoder
	 *            transocder
	 * @param timeout
	 *            operation timeout,in milliseconds
	 * @return boolean result
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> boolean set(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract boolean set(final String key, final int exp,
			final Object value) throws TimeoutException, InterruptedException,
			MemcachedException;

	public abstract boolean set(final String key, final int exp,
			final Object value, final long timeout) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> boolean set(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Store key-value item to memcached,doesn't wait for reply
	 * 
	 * @param <T>
	 * @param key
	 *            stored key
	 * @param exp
	 *            expire time
	 * @param value
	 *            stored data
	 * @param transcoder
	 *            transocder
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract void setWithNoReply(final String key, final int exp,
			final Object value) throws InterruptedException, MemcachedException;

	public abstract <T> void setWithNoReply(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws InterruptedException, MemcachedException;

	/**
	 * Add key-value item to memcached, success only when the key is not exists
	 * in memcached.
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param value
	 * @param transcoder
	 * @param timeout
	 * @return boolean result
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> boolean add(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract boolean add(final String key, final int exp,
			final Object value) throws TimeoutException, InterruptedException,
			MemcachedException;

	public abstract boolean add(final String key, final int exp,
			final Object value, final long timeout) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> boolean add(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Add key-value item to memcached, success only when the key is not exists
	 * in memcached.This method doesn't wait for reply.
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param value
	 * @param transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */

	public abstract void addWithNoReply(final String key, final int exp,
			final Object value) throws InterruptedException, MemcachedException;

	public abstract <T> void addWithNoReply(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws InterruptedException, MemcachedException;

	/**
	 * Replace the key's data item in memcached,success only when the key's data
	 * item is exists in memcached.This method will wait for reply from server.
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param value
	 * @param transcoder
	 * @param timeout
	 * @return boolean result
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> boolean replace(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract boolean replace(final String key, final int exp,
			final Object value) throws TimeoutException, InterruptedException,
			MemcachedException;

	public abstract boolean replace(final String key, final int exp,
			final Object value, final long timeout) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> boolean replace(final String key, final int exp,
			final T value, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Replace the key's data item in memcached,success only when the key's data
	 * item is exists in memcached.This method doesn't wait for reply from
	 * server.
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param value
	 * @param transcoder
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract void replaceWithNoReply(final String key, final int exp,
			final Object value) throws InterruptedException, MemcachedException;

	public abstract <T> void replaceWithNoReply(final String key,
			final int exp, final T value, final Transcoder<T> transcoder)
			throws InterruptedException, MemcachedException;

	public abstract boolean append(final String key, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Append value to key's data item,this method will wait for reply
	 * 
	 * @param key
	 * @param value
	 * @param timeout
	 * @return boolean result
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract boolean append(final String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	/**
	 * Append value to key's data item,this method doesn't wait for reply.
	 * 
	 * @param key
	 * @param value
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract void appendWithNoReply(final String key, final Object value)
			throws InterruptedException, MemcachedException;

	public abstract boolean prepend(final String key, final Object value)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Prepend value to key's data item in memcached.This method doesn't wait
	 * for reply.
	 * 
	 * @param key
	 * @param value
	 * @return boolean result
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract boolean prepend(final String key, final Object value,
			final long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	/**
	 * Prepend value to key's data item in memcached.This method doesn't wait
	 * for reply.
	 * 
	 * @param key
	 * @param value
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract void prependWithNoReply(final String key, final Object value)
			throws InterruptedException, MemcachedException;

	public abstract boolean cas(final String key, final int exp,
			final Object value, final long cas) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 *Cas is a check and set operation which means "store this data but only if
	 * no one else has updated since I last fetched it."
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 * @param value
	 * @param transcoder
	 * @param timeout
	 * @param cas
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> boolean cas(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long timeout,
			final long cas) throws TimeoutException, InterruptedException,
			MemcachedException;

	public abstract boolean cas(final String key, final int exp,
			final Object value, final long timeout, final long cas)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> boolean cas(final String key, final int exp,
			final T value, final Transcoder<T> transcoder, final long cas)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 *Cas is a check and set operation which means "store this data but only if
	 * no one else has updated since I last fetched it."
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 *            ??????????
	 * @param operation
	 *            CASOperation?????cas??
	 * @param transcoder
	 *            ?????
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> boolean cas(final String key, final int exp,
			final CASOperation<T> operation, final Transcoder<T> transcoder)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 *cas is a check and set operation which means "store this data but only if
	 * no one else has updated since I last fetched it."
	 * 
	 * @param <T>
	 * @param key
	 * @param exp
	 *            data item expire time
	 * @param getsReponse
	 *            gets method's result
	 * @param operation
	 *            CASOperation
	 * @param transcoder
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> boolean cas(final String key, final int exp,
			GetsResponse<T> getsReponse, final CASOperation<T> operation,
			final Transcoder<T> transcoder) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> boolean cas(final String key, final int exp,
			GetsResponse<T> getsReponse, final CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> boolean cas(final String key,
			GetsResponse<T> getsResponse, final CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> boolean cas(final String key, final int exp,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> boolean cas(final String key,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * 
	 * @param <T>
	 * @param key
	 * @param getsResponse
	 * @param operation
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract <T> void casWithNoReply(final String key,
			GetsResponse<T> getsResponse, final CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> void casWithNoReply(final String key, final int exp,
			GetsResponse<T> getsReponse, final CASOperation<T> operation)
			throws TimeoutException, InterruptedException, MemcachedException;

	public abstract <T> void casWithNoReply(final String key, final int exp,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract <T> void casWithNoReply(final String key,
			final CASOperation<T> operation) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Delete key's data item from memcached.It it is not exists,return
	 * false.</br> time is the amount of time in seconds (or Unix time
	 * until</br> which) the client wishes the server to refuse "add" and
	 * "replace"</br> commands with this key. For this amount of item, the item
	 * is put into a</br> delete queue, which means that it won't possible to
	 * retrieve it by the</br> "get" command, but "add" and "replace" command
	 * with this key will also</br> fail (the "set" command will succeed,
	 * however). After the time passes,</br> the item is finally deleted from
	 * server memory. </br><strong>Note: This method is deprecated,because
	 * memcached 1.4.0 remove the optional argument "time".You can still use
	 * this method on old version,but is not recommended.</strong>
	 * 
	 * @param key
	 * @param time
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	@Deprecated
	public abstract boolean delete(final String key, final int time)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Get all connected memcached servers's versions.
	 * 
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public Map<InetSocketAddress, String> getVersions()
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * "incr" are used to change data for some item in-place, incrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for incr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @return the new value of the item's data, after the increment operation
	 *         was carried out.
	 * @param key
	 * @param num
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract long incr(final String key, final long delta)
			throws TimeoutException, InterruptedException, MemcachedException;

	public long incr(final String key, final long delta, final long initValue)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * "incr" are used to change data for some item in-place, incrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for incr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @param key
	 *            key
	 * @param num
	 *            increment
	 * @param initValue
	 *            initValue if the data is not exists.
	 * @param timeout
	 *            operation timeout
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public long incr(final String key, final long delta, final long initValue,
			long timeout) throws TimeoutException, InterruptedException,
			MemcachedException;

	/**
	 * "decr" are used to change data for some item in-place, decrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for decr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @return the new value of the item's data, after the decrement operation
	 *         was carried out.
	 * @param key
	 * @param num
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract long decr(final String key, final long delta)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * @see decr
	 * @param key
	 * @param num
	 * @param initValue
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract long decr(final String key, final long delta, long initValue)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * "decr" are used to change data for some item in-place, decrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for decr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @param key
	 *            The key
	 * @param num
	 *            The increment
	 * @param initValue
	 *            The initial value if the data is not exists.
	 * @param timeout
	 *            Operation timeout
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract long decr(final String key, final long delta,
			long initValue, long timeout) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Make All connected memcached's data item invalid
	 * 
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract void flushAll() throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract void flushAllWithNoReply() throws InterruptedException,
			MemcachedException;

	/**
	 * Make All connected memcached's data item invalid
	 * 
	 * @param timeout
	 *            operation timeout
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract void flushAll(long timeout) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * Invalidate all existing items immediately
	 * 
	 * @param address
	 *            Target memcached server
	 * @param timeout
	 *            operation timeout
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public abstract void flushAll(InetSocketAddress address)
			throws MemcachedException, InterruptedException, TimeoutException;

	public abstract void flushAllWithNoReply(InetSocketAddress address)
			throws MemcachedException, InterruptedException;

	public abstract void flushAll(InetSocketAddress address, long timeout)
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * This method is deprecated,please use flushAll(InetSocketAddress) instead.
	 * 
	 * @deprecated
	 * @param host
	 * 
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	@Deprecated
	public abstract void flushAll(String host) throws TimeoutException,
			InterruptedException, MemcachedException;

	public abstract Map<String, String> stats(InetSocketAddress address)
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * ???????memcached server????
	 * 
	 * @param address
	 *            ????
	 * @param timeout
	 *            ????
	 * @return
	 * @throws MemcachedException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public abstract Map<String, String> stats(InetSocketAddress address,
			long timeout) throws MemcachedException, InterruptedException,
			TimeoutException;

	/**
	 * Get stats from all memcached servers
	 * 
	 * @param timeout
	 * @return server->item->value map
	 * @throws MemcachedException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public abstract Map<InetSocketAddress, Map<String, String>> getStats(
			long timeout) throws MemcachedException, InterruptedException,
			TimeoutException;

	public abstract Map<InetSocketAddress, Map<String, String>> getStats()
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * Get special item stats. "stats items" for example
	 * 
	 * @param item
	 * @return
	 */
	public Map<InetSocketAddress, Map<String, String>> getStatsByItem(
			String itemName) throws MemcachedException, InterruptedException,
			TimeoutException;;

	public abstract void shutdown() throws IOException;

	public abstract boolean delete(final String key) throws TimeoutException,
			InterruptedException, MemcachedException;

	/**
	 * return default transcoder,default is SerializingTranscoder
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public abstract Transcoder getTranscoder();

	/**
	 * set transcoder
	 * 
	 * @param transcoder
	 */
	@SuppressWarnings("unchecked")
	public abstract void setTranscoder(final Transcoder transcoder);

	public Map<InetSocketAddress, Map<String, String>> getStatsByItem(
			String itemName, long timeout) throws MemcachedException,
			InterruptedException, TimeoutException;

	/**
	 * get operation timeout setting
	 * 
	 * @return
	 */
	public long getOpTimeout();

	/**
	 * set operation timeout,default is one second.
	 * 
	 * @param opTimeout
	 */
	public void setOpTimeout(long opTimeout);

	public Map<InetSocketAddress, String> getVersions(long timeout)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * get avaliable memcached servers's socket address.
	 * 
	 * @return
	 */
	public Collection<InetSocketAddress> getAvaliableServers();

	/**
	 * add a memcached server to MemcachedClient
	 * 
	 * @param server
	 * @param port
	 * @param weight
	 * @throws IOException
	 */
	public void addServer(final String server, final int port, int weight)
			throws IOException;

	public void addServer(final InetSocketAddress inetSocketAddress, int weight)
			throws IOException;

	/**
	 * Delete key's data item from memcached.This method doesn't wait for reply.
	 * This method does not work on memcached 1.3 or later version.See <a href=
	 * 'http://code.google.com/p/memcached/issues/detail?id=3&q=delete%20noreply
	 * ' > i s s u e 3</a> </br><strong>Note: This method is deprecated,because
	 * memcached 1.4.0 remove the optional argument "time".You can still use
	 * this method on old version,but is not recommended.</strong>
	 * 
	 * @param key
	 * @param time
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	@Deprecated
	public void deleteWithNoReply(final String key, final int time)
			throws InterruptedException, MemcachedException;

	public void deleteWithNoReply(final String key)
			throws InterruptedException, MemcachedException;

	/**
	 * "incr" are used to change data for some item in-place, incrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for incr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @param key
	 * @param num
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void incrWithNoReply(final String key, final long delta)
			throws InterruptedException, MemcachedException;

	/**
	 * "decr" are used to change data for some item in-place, decrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for decr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @param key
	 * @param num
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void decrWithNoReply(final String key, final long delta)
			throws InterruptedException, MemcachedException;

	/**
	 * Set the verbosity level of the memcached's logging output.This method
	 * will wait for reply.
	 * 
	 * @param address
	 * @param level
	 *            logging level
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void setLoggingLevelVerbosity(InetSocketAddress address, int level)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Set the verbosity level of the memcached's logging output.This method
	 * doesn't wait for reply from server
	 * 
	 * @param address
	 *            memcached server address
	 * @param level
	 *            logging level
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	public void setLoggingLevelVerbosityWithNoReply(InetSocketAddress address,
			int level) throws InterruptedException, MemcachedException;

	/**
	 * Add a memcached client listener
	 * 
	 * @param listener
	 */
	public void addStateListener(MemcachedClientStateListener listener);

	/**
	 * Remove a memcached client listener
	 * 
	 * @param listener
	 */
	public void removeStateListener(MemcachedClientStateListener listener);

	/**
	 * Get all current state listeners
	 * 
	 * @return
	 */
	public Collection<MemcachedClientStateListener> getStateListeners();

	public void flushAllWithNoReply(int exptime) throws InterruptedException,
			MemcachedException;

	public void flushAll(int exptime, long timeout) throws TimeoutException,
			InterruptedException, MemcachedException;

	public void flushAllWithNoReply(InetSocketAddress address, int exptime)
			throws MemcachedException, InterruptedException;

	public void flushAll(InetSocketAddress address, long timeout, int exptime)
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * If the memcached dump or network error cause connection closed,xmemcached
	 * would try to heal the connection.The interval between reconnections is 2
	 * seconds by default. You can change that value by this method.
	 * 
	 * @param healConnectionInterval
	 *            MILLISECONDS
	 */
	public void setHealSessionInterval(long healConnectionInterval);

	/**
	 * Return the default heal session interval in milliseconds
	 * 
	 * @return
	 */
	public long getHealSessionInterval();

	public Protocol getProtocol();

	/**
	 * Store all primitive type as string,defualt is false.
	 */
	public void setPrimitiveAsString(boolean primitiveAsString);

	/**
	 * In a high concurrent enviroment,you may want to pool memcached
	 * clients.But a xmemcached client has to start a reactor thread and some
	 * thread pools,if you create too many clients,the cost is very large.
	 * Xmemcached supports connection pool instreadof client pool.you can create
	 * more connections to one or more memcached servers,and these connections
	 * share the same reactor and thread pools,it will reduce the cost of
	 * system.
	 * 
	 * @param poolSize
	 *            pool size,default is one,every memcached has only one
	 *            connection.
	 */
	public void setConnectionPoolSize(int poolSize);

	/**
	 * Whether to enable heart beat
	 * 
	 * @param enableHeartBeat
	 *            if true,then enable heartbeat,true by default
	 */
	public void setEnableHeartBeat(boolean enableHeartBeat);

	/**
	 * Enables/disables sanitizing keys by URLEncoding.
	 * 
	 * @param sanitizeKey
	 *            if true, then URLEncode all keys
	 */
	public void setSanitizeKeys(boolean sanitizeKey);

	public boolean isSanitizeKeys();

	/**
	 * Get counter for key,and if the key's value is not set,then set it with 0.
	 * 
	 * @param key
	 * @return
	 */
	public Counter getCounter(String key);

	/**
	 * Get counter for key,and if the key's value is not set,then set it with
	 * initial value.
	 * 
	 * @param key
	 * @param initialValue
	 * @return
	 */
	public Counter getCounter(String key, long initialValue);

	/**
	 * Get key iterator for special memcached server.You must known that the
	 * iterator is a snapshot for memcached all keys,it is not real-time.
	 * 
	 * @param address
	 * @return
	 */
	public KeyIterator getKeyIterator(InetSocketAddress address)
			throws MemcachedException, InterruptedException, TimeoutException;

	/**
	 * Configure auth info
	 * 
	 * @param map
	 *            Auth info map,key is memcached server address,and value is the
	 *            auth info for the key.
	 */
	public void setAuthInfoMap(Map<InetSocketAddress, AuthInfo> map);

	/**
	 * return current all auth info
	 * 
	 * @return Auth info map,key is memcached server address,and value is the
	 *         auth info for the key.
	 */
	public Map<InetSocketAddress, AuthInfo> getAuthInfoMap();

	/**
	 * "incr" are used to change data for some item in-place, incrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for incr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @param key
	 * @param delta
	 * @param initValue
	 *            the initial value to be added when value is not found
	 * @param timeout
	 * @param exp
	 *            the initial vlaue expire time
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	long decr(String key, long delta, long initValue, long timeout, int exp)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * "incr" are used to change data for some item in-place, incrementing it.
	 * The data for the item is treated as decimal representation of a 64-bit
	 * unsigned integer. If the current data value does not conform to such a
	 * representation, the commands behave as if the value were 0. Also, the
	 * item must already exist for incr to work; these commands won't pretend
	 * that a non-existent key exists with value 0; instead, it will fail.This
	 * method doesn't wait for reply.
	 * 
	 * @param key
	 *            key
	 * @param delta
	 *            increment delta
	 * @param initValue
	 *            the initial value to be added when value is not found
	 * @param timeout
	 *            operation timeout
	 * @param exp
	 *            the initial vlaue expire time
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @throws MemcachedException
	 */
	long incr(String key, long delta, long initValue, long timeout, int exp)
			throws TimeoutException, InterruptedException, MemcachedException;

	/**
	 * Return the cache instance name
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * Set cache instance name
	 * 
	 * @param name
	 */
	public void setName(String name);

	/**
	 * Returns reconnecting task queue,the result is thread-safe,but maybe you
	 * should not modify it at all
	 * 
	 * @return The reconnecting task queue,if the client has not been
	 *         started,returns null.
	 */
	public Queue<ReconnectRequest> getReconnectRequestQueue();

	/**
	 * Configure wheather to set client in failure mode.If set it to true,that
	 * means you want to configure client in failure mode. Failure mode is that
	 * when a memcached server is down,it would not taken from the server list
	 * but marked as unavailable,and then further requests to this server will
	 * be transformed to standby node if configured or throw an exception until
	 * it comes back up.
	 * 
	 * @param failureMode
	 *            true is to configure client in failure mode.
	 */
	public void setFailureMode(boolean failureMode);

	/**
	 * Returns if client is in failure mode.
	 * 
	 * @return
	 */
	public boolean isFailureMode();

}
