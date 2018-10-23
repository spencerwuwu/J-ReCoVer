// https://searchcode.com/api/result/73792517/

package other;

/* FCpacket.java
 Version 1.0
 Praktikum Rechnernetze HAW Hamburg
 Autor: M. Hubner
 */

public class FCpacket implements Comparable<FCpacket> {
	/*
	 * Data structure for the representation of one data packet with seq num in
	 * the send/rec buffer (seq num and data will be sent in one UDP packet).
	 */
	private byte[] data;
	private int dataLen; // length of data array
	private long seqNumber; // sequence number as long
	private byte[] seqNumberBytes = new byte[8]; // sequence number as byte
	private FC_Timer timer;
	private boolean validACK = false;
	private long timestamp = -1; // Can be used to store the send time

	/**
	 * Constructor for sending FCpackets. The first <packetLen> bytes of the
	 * packetData byte array are copied and a new data byte array is generated.
	 */
	public FCpacket(long seqNum, byte[] packetData, int packetLen) {
		data = new byte[packetLen];
		System.arraycopy(packetData, 0, data, 0, packetLen);
		dataLen = packetLen;
		seqNumber = seqNum;
		writeBytes(seqNum, seqNumberBytes, 0, 8);
	}

	/**
	 * Constructor for received FCpackets. The first 8 bytes of the packetData
	 * are treated as the sequence number. The other <packetLen-8> bytes of the
	 * packetData byte array are copied and a new data byte array is generated.
	 */
	public FCpacket(byte[] packetData, int packetLen) {
		seqNumberBytes = reduce(packetData, 0, 8);
		data = reduce(packetData, 8, packetLen - 8);
		dataLen = packetLen - 8;
		seqNumber = makeLong(seqNumberBytes, 0, 8);
	}

	/**
	 * Save a timestamp for the FCpacket
	 */
	public void setTimestamp(long time) {
		timestamp = time;
	}

	/**
	 * Save a reference to a timer thread for the FCpacket
	 */
	public void setTimer(FC_Timer t) {
		timer = t;
	}

	/**
	 * Returns the data of the FCpacket.
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Returns the length of the data byte array of the FCpacket
	 */
	public int getLen() {
		return dataLen;
	}

	/**
	 * Returns the sequence number of the FCpacket as a long value
	 */
	public long getSeqNum() {
		return seqNumber;
	}

	/**
	 * Returns the sequence number of the FCpacket as a byte array
	 */
	public byte[] getSeqNumBytes() {
		return seqNumberBytes;
	}

	/**
	 * Returns the sequence number of the FCpacket as a byte[8] array
	 * concatenated with the data byte array (length = dataLen + 8)
	 */
	public byte[] getSeqNumBytesAndData() {
		return concatenate(seqNumberBytes, data);
	}

	/**
	 * Returns the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Returns the timer thread as FC_Timer object
	 */
	public FC_Timer getTimer() {
		return timer;
	}

	/**
	 * Returns the state of acknowledgement for the FCpacket
	 */
	public boolean isValidACK() {
		return validACK;
	}

	/**
	 * Save the state of acknowledgement for the FCpacket
	 */
	public void setValidACK(boolean validACK) {
		this.validACK = validACK;
	}

	// -------------------- standard methods ----------------------------------

	@Override
	public int compareTo(FCpacket partner) {
		// Use seq num for comparison (sort)
		return (int) (this.seqNumber - partner.seqNumber);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (seqNumber ^ (seqNumber >>> 32));
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FCpacket other = (FCpacket) obj;
		if (seqNumber != other.seqNumber) {
			return false;
		}
		return true;
	}

	// -------------------- auxiliary methods ---------------------------------
	/**
	 * Reduce the given byte array to the given length starting at position
	 * offset
	 */
	private byte[] reduce(byte[] ba, int offset, int len) {
		byte[] result = new byte[len];

		System.arraycopy(ba, offset, result, 0, len);

		return result;
	}

	/**
	 * Concatenate two byte arrays
	 */
	private byte[] concatenate(byte[] ba1, byte[] ba2) {
		int len1 = ba1.length;
		int len2 = ba2.length;
		byte[] result = new byte[len1 + len2];

		// Fill with first array
		System.arraycopy(ba1, 0, result, 0, len1);
		// Fill with second array
		System.arraycopy(ba2, 0, result, len1, len2);

		return result;
	}

	/**
	 * Convert a byte array to a long. Bits are collected from
	 * <code>buf[i..i+length-1]</code>.
	 */
	private long makeLong(byte[] buf, int i, int length) {
		long r = 0;
		length += i;

		for (int j = i; j < length; j++)
			r = (r << 8) | (buf[j] & 0xffL);

		return r;
	}

	/**
	 * Write a long to a byte array. Bits from <code>source</code> are written
	 * to <code>dest[i..i+length-1]</code>.
	 */
	private void writeBytes(long source, byte[] dest, int i, int length) {
		for (int j = (i + length) - 1; j >= i; j--) {
			dest[j] = (byte) source;
			source = source >>> 8;
		}
	}

}

