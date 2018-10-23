// https://searchcode.com/api/result/52806705/

package tokyotyrant;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import tokyotyrant.networking.NodeAddress;
import tokyotyrant.protocol.Adddouble;
import tokyotyrant.protocol.Addint;
import tokyotyrant.protocol.Command;
import tokyotyrant.protocol.Copy;
import tokyotyrant.protocol.Ext;
import tokyotyrant.protocol.Fwmkeys;
import tokyotyrant.protocol.Get;
import tokyotyrant.protocol.Iterinit;
import tokyotyrant.protocol.Iternext;
import tokyotyrant.protocol.Mget;
import tokyotyrant.protocol.Misc;
import tokyotyrant.protocol.Optimize;
import tokyotyrant.protocol.Out;
import tokyotyrant.protocol.Put;
import tokyotyrant.protocol.Putcat;
import tokyotyrant.protocol.Putkeep;
import tokyotyrant.protocol.Putnr;
import tokyotyrant.protocol.Putshl;
import tokyotyrant.protocol.Restore;
import tokyotyrant.protocol.Rnum;
import tokyotyrant.protocol.Setmst;
import tokyotyrant.protocol.Size;
import tokyotyrant.protocol.Stat;
import tokyotyrant.protocol.Sync;
import tokyotyrant.protocol.Vanish;
import tokyotyrant.protocol.Vsiz;
import tokyotyrant.transcoder.StringTranscoder;
import tokyotyrant.transcoder.Transcoder;

/**
 * Official Tokyo Tyrant API(C/Perl/Ruby) like interface.
 */
public class RDB {
	/**
	 * scripting extension option: record locking
	 */
	public static final int XOLCKREC = 1 << 0;
	/**
	 * scripting extension option: global locking 
	 */
	public static final int XOLCKGLB = 1 << 1;
	
	/**
	 * Restore options: consistency checking 
	 */
	public static final int ROCHKCON = 1 << 0;
	
	/**
	 * Versatile function option: omission of the update log
	 */
	public static final int MONOULOG = 1 << 0;
	
	private Transcoder keyTranscoder = new StringTranscoder();
	private Transcoder valueTranscoder = new StringTranscoder();

	private SocketAddress address;
	private int timeout;
	Connection connection;

	/**
	 * Open a remote database connection.
	 * 
	 * @param address specifies the uri of the server.
	 */
	public void open(NodeAddress address) {
		open(address.socketAddress(), address.timeout());
	}

	/**
	 * Open a remote database connection.
	 * 
	 * @param address specifies the address of the server.
	 */
	public void open(SocketAddress address) {
		open(address, 0);
	}

	/**
	 * Open a remote database connection.
	 * 
	 * @param address specifies the address of the server.
	 * @param timeout specified the socket timeout.
	 */
	public void open(SocketAddress address, int timeout) {
		this.address = address;
		this.timeout = timeout;
		this.connection = null;
	}

	/**
	 * Close the database connection.
	 */
	public void close() {
		connection.close();
		connection = null;
	}
	
	/**
	 * Set the {@link Transcoder} to be used to transcode keys.
	 * Not required: @{link StringTranscoder} is the default.
	 */
	public void setKeyTranscoder(Transcoder transcoder) {
		this.keyTranscoder = transcoder;
	}
	
	/**
	 * Set the {@link Transcoder} to be used to transcode values.
	 * Not required: @{link StringTranscoder} is the default.
	 */
	public void setValueTranscoder(Transcoder transcoder) {
		this.valueTranscoder = transcoder;
	}
	
	/**
	 * Get the {@link Transcoder} to be used to transcode keys.
	 */
	public Transcoder getKeyTranscoder() {
		return keyTranscoder;
	}
	
	/**
	 * Get the {@link Transcoder} to be used to transcode values.
	 */
	public Transcoder getValueTranscoder() {
		return valueTranscoder;
	}

	/**
	 * Store a record.
	 * If a record with the same key exists in the database, it is overwritten.
	 *  
	 * @param key specifies the key.
	 * @param value specifies the value.
	 * @return If successful, the return value is true, else, it is false.
	 */
	public boolean put(Object key, Object value) {
		return execute(new Put(keyTranscoder, valueTranscoder, key, value));
	}

	public boolean put(Object key, Object value, Transcoder valueTranscoder) {
		return execute(new Put(keyTranscoder, valueTranscoder, key, value));
	}

	/**
	 * Store a new record.
	 * If a record with the same key exists in the database, this method has no effect.
	 * 
	 * @param key specifies the key.
	 * @param value specifies the value.
	 * @return If successful, the return value is true, else, it is false.
	 */
	public boolean putkeep(Object key, Object value) {
		return execute(new Putkeep(keyTranscoder, valueTranscoder, key, value));
	}

	public boolean putkeep(Object key, Object value, Transcoder valueTranscoder) {
		return execute(new Putkeep(keyTranscoder, valueTranscoder, key, value));
	}

	/**
	 * Concatenate a value at the end of the existing record.
	 * If there is no corresponding record, a new record is created.
	 * 
	 * @param key specifies the key.
	 * @param value specifies the value.
	 * @return If successful, the return value is true, else, it is false.
	 */
	public boolean putcat(Object key, Object value) {
		return execute(new Putcat(keyTranscoder, valueTranscoder, key, value));
	}

	public boolean putcat(Object key, Object value, Transcoder valueTranscoder) {
		return execute(new Putcat(keyTranscoder, valueTranscoder, key, value));
	}

	/**
	 * Concatenate a value at the end of the existing record and shift it to the left.
	 * If there is no corresponding record, a new record is created.
	 *  
	 * @param key specifies the key.
	 * @param value specifies the value.
	 * @param width specifies the width of the record.
	 * @return If successful, the return value is true, else, it is false.
	 */
	public boolean putshl(Object key, Object value, int width) {
		return execute(new Putshl(keyTranscoder, valueTranscoder, key, value, width));
	}

	public boolean putshl(Object key, Object value, int width, Transcoder valueTranscoder) {
		return execute(new Putshl(keyTranscoder, valueTranscoder, key, value, width));
	}

	/**
	 * Store a record without response from the server
	 * If a record with the same key exists in the database, it is overwritten.
	 * 
	 * @param key specifies the key.
	 * @param value specifies the value.
	 */
	public void putnr(Object key, Object value) {
		execute(new Putnr(keyTranscoder, valueTranscoder, key, value));
	}

	public void putnr(Object key, Object value, Transcoder valueTranscoder) {
		execute(new Putnr(keyTranscoder, valueTranscoder, key, value));
	}

	/**
	 * Remove a record.
	 * 
	 * @param key specifies the key.
	 * @return If successful, the return value is true, else, it is false.
	 */
	public boolean out(Object key) {
		return execute(new Out(keyTranscoder, valueTranscoder, key));
	}
	
	/**
	 * Retrieve a record.
	 * 
	 * @param key specifies the key.
	 * @return If successful, the return value is the value of the corresponding record. {@code null} is returned if no record corresponds.
	 */
	public Object get(Object key) {
		return execute(new Get(keyTranscoder, valueTranscoder, key));
	}

	public Object get(Object key, Transcoder valueTranscoder) {
		return execute(new Get(keyTranscoder, valueTranscoder, key));
	}

	/**
	 * Retrieve records.
	 * 
	 * @param keys specifies an array containing the retrieval keys.
	 * @return If successful, the return value is the map contains corresponding values, else, it is {@code null}. As a result of this method, keys existing in the database have the corresponding values and keys not existing in the database are removed.
	 */
	public Map<Object, Object> mget(Object[] keys) {
		return execute(new Mget(keyTranscoder, valueTranscoder, keys));
	}
	
	public Map<Object, Object> mget(Object[] keys, Transcoder valueTranscoder) {
		return execute(new Mget(keyTranscoder, valueTranscoder, keys));
	}

	/**
	 * Get the size of the value of a record.
	 * 
	 * @param key specifies the key.
	 * @return If successful, the return value is the size of the value of the corresponding record, else, it is -1.
	 */
	public int vsiz(Object key) {
		return execute(new Vsiz(keyTranscoder, valueTranscoder, key));
	}

	/**
	 * Initialize the iterator.
	 * The iterator is used in order to access the key of every record stored in a database.
	 *  
	 * @return If successful, the return value is true, else, it is false.
	 */
	public boolean iterinit() {
		return execute(new Iterinit());
	}
	
	/**
	 * Get the next key of the iterator.
	 * It is possible to access every record by iteration of calling this method. It is allowed to update or remove records whose keys are fetched while the iteration. However, it is not assured if updating the database is occurred while the iteration. Besides, the order of this traversal access method is arbitrary, so it is not assured that the order of storing matches the one of the traversal access.
	 * 
	 * @return If successful, the return value is the next key, else, it is {@code null}. {@code null} is returned when no record is to be get out of the iterator.
	 */
	public Object iternext() {
		return execute(new Iternext(keyTranscoder, valueTranscoder));
	}

	/**
	 * Get forward matching keys.
	 * Note that this method may be very slow because every key in the database is scanned.
	 * 
	 * @param prefix specifies the prefix of the corresponding keys.
	 * @param max specifies the maximum number of keys to be fetched. If it is not defined or negative, no limit is specified.
	 * @return The return value is an array of the keys of the corresponding records. This method does never fail and return an empty list even if no record corresponds.
	 */
	public Object[] fwmkeys(Object prefix, int max) {
		return execute(new Fwmkeys(keyTranscoder, valueTranscoder, prefix, max));
	}

	/**
	 * Add an integer to a record.
	 * If the corresponding record exists, the value is treated as an integer and is added to. If no record corresponds, a new record of the additional value is stored.
	 * 
	 * @param key specifies the key.
	 * @param num specifies the additional value. If it is not defined, 0 is specified.
	 * @return If successful, the return value is the summation value, else, it is {@link Integer#MIN_VALUE}.
	 */
	public int addint(Object key, int num) {
		return execute(new Addint(keyTranscoder, valueTranscoder, key, num));
	}

	/**
	 * Add a real number to a record.
	 * If the corresponding record exists, the value is treated as a real number and is added to. If no record corresponds, a new record of the additional value is stored.
	 * 
	 * @param key specifies the key.
	 * @param num specifies the additional value. If it is not defined, 0 is specified.
	 * @return If successful, the return value is the summation value, else, it is {@link Double#NaN}.
	 */
	public double adddouble(Object key, double num) {
		return execute(new Adddouble(keyTranscoder, valueTranscoder, key, num));
	}

	/**
	 * Call a function of the script language extension.
	 * 
	 * @param name specifies the function name.
	 * @param key specifies the key. If it is not defined, an empty string is specified.
	 * @param value specifies the value. If it is not defined, an empty string is specified.
	 * @param opts specifies options by bitwise or: {@link RDB#XOLCKREC} for record locking, {@link RDB#XOLCKGLB} for global locking. If it is {@code 0}, no option is specified.
	 * @return If successful, the return value is the value of the response or {@code null} on failure.
	 */
	public Object ext(String name, Object key, Object value, int opts) {
		return execute(new Ext(keyTranscoder, valueTranscoder, name, key, value, opts));
	}

	public Object ext(String name, Object key, Object value, int opts, Transcoder valueTranscoder) {
		return execute(new Ext(keyTranscoder, valueTranscoder, name, key, value, opts));
	}

	/**
	 * Synchronize updated contents with the file and the device.
	 * 
	 * @return If successful, the return value is true, else, it is false.
	 */
	public boolean sync() {
		return execute(new Sync());
	}

	/**
	 * Optimize the storage.
	 * 
	 * @param params specifies the string of the tuning parameters. If it is not defined, it is not used.
	 * @return If successful, the return value is true, else, it is false.
	 */
	public boolean optimize(String params) {
		return execute(new Optimize(params));
	}

	/**
	 * Remove all records.
	 * 
	 * @return If successful, the return value is true, else, it is false.
	 */
	public boolean vanish() {
		return execute(new Vanish());
	}

	/**
	 * Copy the database file.
	 * The database file is assured to be kept synchronized and not modified while the copying or executing operation is in progress. So, this method is useful to create a backup file of the database file.
	 * 
	 * @param path specifies the path of the destination file. If it begins with {@code @}, the trailing substring is executed as a command line.
	 * @return If successful, the return value is true, else, it is false. False is returned if the executed command returns non-zero code.
	 */
	public boolean copy(String path) {
		return execute(new Copy(path));
	}

	/**
	 * Restore the database file from the update log.
	 * 
	 * @param path specifies the path of the update log directory.
	 * @param ts specifies the beginning time stamp in microseconds.
	 * @param opts specifies options by bitwise-or: {@link RDB#ROCHKCON} for consistency checking.
	 * @return If successful, the return value is true, else, it is false.
	 */
	public boolean restore(String path, long ts, int opts) {
		return execute(new Restore(path, ts, opts));
	}
	
	/**
	 * Set the replication master. 
	 * 
	 * @param host specifies the name or the address of the server. If it is {@code null}, replication of the database is disabled.
	 * @param port specifies the port number.
	 * @param ts specifies the beginning timestamp in microseconds.
	 * @param opts specifies options by bitwise-or: {@link RDB#ROCHKCON} for consistency checking.
	 * @return If successful, the return value is true, else, it is false.
	 */
	public boolean setmst(String host, int port, long ts, int opts) {
		return execute(new Setmst(host, port, ts, 0));
	}

	/**
	 * Get the number of records.
	 * 
	 * @return The return value is the number of records or 0 if the object does not connect to any database server.
	 */
	public long rnum() {
		return execute(new Rnum());
	}

	/**
	 * Get the size of the database.
	 *
	 * @return The return value is the size of the database or 0 if the object does not connect to any database server.
	 */
	public long size() {
		return execute(new Size());
	}

	/**
	 * Get the status string of the database server.
	 *  
	 * @return The return value is the status items of the database.
	 */
	public Map<String, String> stat() {
		return execute(new Stat());
	}

	/**
	 * Call a versatile function for miscellaneous operations.
	 * 
	 * @param name
	 *            specifies the name of the function. All databases support
	 *            "putlist", "outlist", and "getlist". "putlist" is to store
	 *            records. It receives keys and values one after the other, and
	 *            returns an empty list. "outlist" is to remove records. It
	 *            receives keys, and returns an empty array. "getlist" is to
	 *            retrieve records. It receives keys, and returns keys and
	 *            values of corresponding records one after the other. Table
	 *            database supports "setindex", "search", and "genuid".
	 * @param args
	 *            specifies an array containing arguments. If it is not defined,
	 *            no argument is specified.
	 * @param opts
	 *            specifies options by bitwise-or: {@link RDB#MONOULOG} for omission
	 *            of the update log. If it is not defined, no option is
	 *            specified.
	 * @return If successful, the return value is an array of the result.
	 *         {@code null} is returned on failure.
	 */
	public List<byte[]> misc(String name, List<byte[]> args, int opts) {
		return execute(new Misc(name, args, opts));
	}

	/**
	 * Execute the command.
	 * Use the default value transcoder.
	 * 
	 * @param <T> the type of the return value of the command.
	 * @param command the command to execute.
	 * @return the return value of the command.
	 */
	protected <T> T execute(Command<T> command) {
		if (connection == null) {
			connection = new SocketConnection(address, timeout);
		}
		try {
			sendRequest(command);
			receiveResponse(command);
			return command.getReturnValue();
		} catch (IOException e) {
			connection.close();
			connection = null;
			throw new RuntimeException("Error while executing the command " + command, e);
		}
	}

	/**
	 * Send request.
	 * 
	 * @param command the command to send request.
	 */
	protected void sendRequest(Command<?> command) throws IOException {
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		command.encode(buffer);
		connection.write(buffer);
	}

	/**
	 * Receive response.
	 * 
	 * @param command the command to receive response.
	 */
	protected void receiveResponse(Command<?> command) throws IOException {
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		while (true) {
            //try to decode before read for commands like putnr.
            buffer.markReaderIndex();
            if (command.decode(buffer)) {
                break;
            }
            buffer.resetReaderIndex();

            //fill buffer
            connection.read(buffer);
		}
	}
	
	static interface Connection {
		void close();
		void write(ChannelBuffer buffer) throws IOException;
		void read(ChannelBuffer buffer) throws IOException;
	}
	
	static class SocketConnection implements Connection {
		private final Socket socket;
		private final InputStream inputStream;
		private final OutputStream outputStream;

		public SocketConnection(SocketAddress address, int timeout) {
			try {
				socket = new Socket();
				socket.setTcpNoDelay(true);
				socket.setKeepAlive(true);
				socket.setSoTimeout(timeout);
				socket.connect(address, timeout);
				inputStream = socket.getInputStream();
				outputStream = socket.getOutputStream();
			} catch (IOException e) {
				throw new RuntimeException("Cannot connect to the server " + address, e);
			}
		}

		public void close() {
			try {
				socket.close();
			} catch (IOException ignore) {
			}
		}

		public void write(ChannelBuffer buffer) throws IOException {
            byte[] b = new byte[buffer.readableBytes()];
            buffer.readBytes(b);
            //In blocking-mode, a write operation will return only after writing all of the requested bytes.
			outputStream.write(b);
		}

		/**
		 * Read as much as possible to reduce decoding of incomplete responses.
		 */
		public void read(ChannelBuffer buffer) throws IOException {
            byte[] chunk = new byte[4 * 1024];
            do {
                int n = inputStream.read(chunk);
                if (n == 0) {
                    break;
                }
                if (n == -1) {
                    throw new IOException("Connection closed unexpectedly");
                }
                buffer.writeBytes(chunk, 0, n);
            } while (inputStream.available() > 0);
        }
	}
}

