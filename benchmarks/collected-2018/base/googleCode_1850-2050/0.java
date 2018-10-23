// https://searchcode.com/api/result/12093007/

/*
 * BitVector.java
 * 
 * last update: 15.01.2010 by Stefan Saru
 * 
 * author:	Alec(panovici@elcom.pub.ro)
 *
 * Obs:  This is is the most ... part of the whole stuff.
 * It's a wonder that it really works (by the way, does it ?)
 */

/*
  BitVector.java
  16.01.99
 */

package engine;

import java.util.*;
import java.math.*;
/*
        MSB:--+                                      LSB--+
              |                                           |
              v                                           v
            +-------+-----------------------------------+-------+
     b0,b1: |b0[n-1]|               ...                 | b0[0] | 
            +-------+-----------------------------------+-------+
             7     0                                     7     0
 */
/**
 *  The implementation of a 4-state (0,1,X,Z) bit array.
 */

public class BitVector extends DataHolder{

	public static final int X = 2, Z = 3;

	/**
	 * each "elecrical" bit is represented as a 2 - bit value (see table) below
	 */
	private byte b0[], b1[];

	/*
      value:     |  b1   b0
      -----------+---------
          0      |   0    0
          1      |   0    1
          x      |   1    0
          z      |   1    1
	 */

	/**
	 * indexes of the MSB & the LSB
	 */
	int msb, lsb; 

	/**
	 * Used in a <code>for(.. ; .. ; .. += increment)</code> to traverse this BitVector from 
	 *  LSB to MSB ( i guess.. yeah, I was right !)
	 */
	int increment;

	/**
	 * the size ( |msb - lsb| + 1)
	 */
	int n; 

	/**
	 * true if this BitVector is signed
	 * (this is NOT the sign bit !!)
	 */
	boolean signed;


	//truth tables for the usual logical operations

	public static final int 
	ands[] = {  0, 0, 0, 0,
		0, 1, X, X,
		0, X, X, X,
		0, X, X, X },

		ors[] = { 0, 1, X, X,
		1, 1, 1, 1,
		X, 1, X, X,
		X, 1, X, X },

		xors[] = {  0, 1, X, X,
		1, 0, X, X,
		X, X, X, X,
		X, X, X, X },

		nands[] = {  1, 1, 1, 1,
		1, 0, X, X,
		1, X, X, X,
		1, X, X, X },
		nors[] = { 1, 0, X, X,
		0, 0, 0, 0,
		X, 0, X, X,
		X, 0, X, X },
		xnors[] = { 1, 0, X, X,
		0, 1, X, X,
		X, X, X, X,
		X, X, X, X },

		nots[] = { 1, 0, X, X};

	//3 - bit expansions for octal digits:

	public static final char vtable8 [][] = { 
		{0, 0, 0},  //0
		{1, 0, 0},  //1
		{0, 1, 0},  //2
		{1, 1, 0},  //3
		{0, 0, 1},  //4
		{1, 0, 1},  //5
		{0, 1, 1},  //6
		{1, 1, 1}   //7
	};

	/**
	 *4-bit expansions for hexa digits:
	 */

	static final char vtable16 [][] = { 
		{0, 0, 0, 0}, //0
		{1, 0, 0, 0}, //1
		{0, 1, 0, 0}, //2
		{1, 1, 0, 0}, //3
		{0, 0, 1, 0}, //4
		{1, 0, 1, 0}, //5
		{0, 1, 1, 0}, //6
		{1, 1, 1, 0}, //7
		{0, 0, 0, 1}, //8
		{1, 0, 0, 1}, //9
		{0, 1, 0, 1}, //A
		{1, 1, 0, 1}, //B
		{0, 0, 1, 1}, //C
		{1, 0, 1, 1}, //D
		{0, 1, 1, 1}, //E
		{1, 1, 1, 1}, //F
		{2, 2, 2, 2}, //Z
		{3, 3, 3, 3}  //X
	};

	/*
	 * the transition is b0 -> b1
	 */
	static final int posedge[][] = {
		// \b1
		//b0\      0  1  x  z
		//    \ --------------
		/*  0 |*/ {0, 1, 1, 1},
		/*   1|*/ {0, 0, 0, 0},
		/*   x|*/ {0, 1, 0, 0},
		/*   z|*/ {0, 1, 0, 0}
	};

	static final int negedge[][] = {
		//        \b1
		//      b0 \     0  1  x  z
		//          \--------------
		/*        0 |*/ {0, 0, 0, 0},
		/*        1 |*/ {1, 0, 1, 1},
		/*        x |*/ {1, 0, 0, 0},
		/*        z |*/ {1, 0, 0, 0}
	};


	public static final int and(int a, int b){
		return ands[ (( a & 0x3 ) << 2 ) | (b & 0x3) ];
	}

	public static final int or(int a, int b){
		return ors[ (( a & 0x3 ) << 2 ) | (b & 0x3) ];
	}

	public static final int xor(int a, int b){
		return xors[ (( a & 0x3 ) << 2 ) | (b & 0x3) ];
	}

	public static final int not(int x){
		return nots[x & 0x3];
	}

	/**
	 * returns a new une - bit BitVector whose value is X
	 */
	public static final BitVector bX(){
		BitVector b = new BitVector(0, 0);
		b.reduce(X);
		return b;
	}


	/**
	 * returns a new one - bit BitVector whose value is Z
	 */
	public static final BitVector bZ(){
		BitVector b = new BitVector(0, 0);
		b.reduce(Z);
		return b;
	}

	/**
	 * returns a new one - bit BitVector whose value is 1
	 */
	public static final BitVector b1(){
		BitVector b = new BitVector(0, 0);
		b.reduce(1);
		return b;
	}

	/**
	 * returns a new one - bit BitVector whose value is 0
	 */
	public static final BitVector b0(){
		BitVector b = new BitVector(0, 0);
		b.reduce(0);
		return b;
	}

	public BitVector(long l, boolean signed){
		msb = 63;
		n = 64;
		ensureCapacity(8);
		lsb = 0;
		increment = 1;
		this.signed = signed;   
		for(int i = 0; (i < n) && (l != 0 ) ; i++){
			setb(i, (int)(l & 1));
			l >>= 1;
		}
	}

	public BitVector(BigInteger bi, boolean signed){
		byte data[] = bi.toByteArray();
		int len = data.length;
		ensureCapacity(len);
		for(int i = 0 , j = len-1; i < len ; i++, j--){
			b0[i] = data[j];
			b1[i] = 0;
		}
		this.signed = signed;
		msb = len * 8 - 1;
		lsb = 0;
		increment = 1;
	}

	/**
	 * Creates a new unsigned BitVector
	 */
	public BitVector(int msb, int lsb){
		this(msb, lsb, false);
	}

	public BitVector(int msb, int lsb, boolean signed){
		//    //xConsole.debug("new bitvector ["+msb+", "+lsb+"]");
		this.msb = msb;
		this.lsb = lsb;
		this.signed = signed;
		increment = msb >= lsb ? 1 : -1; //the increment
		n = Math.abs(lsb - msb) +1;
		ensureBitCapacity(n);
		for(int i = 0; i< n ; i++)setb1(i, 1); //set all bits to X
	}

	/**
	 * Creates a new Bitvector filled with the 'bit' value
	 */
	public BitVector(byte bit, int n) {
		this.n = n;
		msb = n-1;
		lsb = 0;
		signed = false;
		increment = msb >= lsb ? 1 : -1;
		ensureBitCapacity(n);
		for(int i = 0; i < n; i++)
			setb(i, bit);
	}

	protected synchronized Object clone(){
		BitVector b = new BitVector(this);

		return b;
	}

	/**
	 * copy constructor
	 */
	public BitVector(BitVector b){
		synchronized(b){
			n = b.n;
			msb = b.msb;
			lsb= b.lsb;
			signed = b.signed;
			increment = b.increment;
			//this may not work !
			b0 = (byte []) b.b0.clone();
			b1 = (byte []) b.b1.clone();

		}
	}

	/**
	 *  this <- b [start : end];
	 * @exception InterpretTimeException if MSB & LSB are reversed
	 */
	public BitVector (BitVector b, int start, int end)
	throws InterpretTimeException
	{
		synchronized(b){
			int s = Math.abs(start - end) +1, i;
			ensureBitCapacity(s);
			signed = false;   //since this is a BitSelect, it must be unsigned
			////xConsole.debug("BitVector.BitVector(BitVector, int, int) : signed assumed false !");
			n = s;
			msb = s - 1;
			lsb = 0;
			increment = 1;
			attrib(msb, lsb, b, start, end);

		}
	}

	/**
	 * creates an unsigned BitVector
	 *
	 */
	public BitVector(String image, int base, int size){
		this(image, base, size, false, false);
	}

	/**
	 *  parses image, according to base & size
	 */
	public BitVector(String image, int base, int size,
			boolean signed, boolean sign) {
		int bitcount = 0;
		if(image.equals(""))image = "0";
		synchronized(image){
			switch(base){
			case 2:
			{
				int ch;
				if(size == -1)
					size = image.length();
				n = size;
				//xConsole.debug("image: \"" + image + "\" size: " + n);
				ensureBitCapacity(n);
				for(bitcount = 0, ch = image.length()-1 ;
				(bitcount < size) && (ch >=0) ; 
				bitcount ++, ch -- )
					switch(image.charAt(ch)){
					case '0': 
					{
						break;
					}
					case '1': 
					{
						setb0(bitcount, 1);
						break;
					}
					case 'X':
					case 'x': 
					{
						setb1(bitcount, 1);
						break;
					}
					case '?':
					case 'Z':
					case 'z': 
					{
						setb(bitcount, Z);
					}      
					} //switch(image.charAt...
				//xConsole.debug("bitc: " + bitcount + " ch: " + ch);
				int msbBit = getb(bitcount-1);
				if(msbBit > 1) //the msb was X or Z ?
					while(bitcount < size)setb(bitcount++, msbBit);  //set the remaining to X
				break;
			}//case 2
			case 10:
			{
				byte[] number;
				try{
					number = (new BigInteger(image)).toByteArray();
				}catch(NumberFormatException ex){
					//normally, it should never get here...
					throw new Error("BitVector.<init> : NumberfoFmatException allowed by grammar ?? " + image);
				}
				if(size == -1){
					size = Integer.MAX_VALUE;
					n = -1;  //set this to rememeber later to set it correctly
				}else n = size;

				ensureCapacity(number.length);
				for(int i = number.length-1 , j = 0; i >= 0; i--, j++)
					b0[i] = number[j];

				//xConsole.debug("BitVector::init (String): length = " + number.length);

				if(n == -1)
					n = number.length * 8;
				break;

			}//case 10

			case 8:
			{

				if(size == -1){
					size = Integer.MAX_VALUE;
					n = -1;  //set this to rememeber later to set it correctly
					ensureBitCapacity(image.length() * 3); 
				}else{
					n = size;
					ensureBitCapacity(n);
				}

				for(int ch = image.length()-1 ; ch >= 0 ; ch--){
					int n = (int)image.charAt(ch) - (int)'0';
					if((n >= 0) && (n < 8)){
						for(int bit = 0; (bit < 3) && (bitcount < size) ; bit++, bitcount++){
							setb0(bitcount, vtable8[n][bit]);
						}
					}else{
						if((image.charAt(ch) == 'x') || (image.charAt(ch) == 'X')){
							for(int bit = 0; (bit < 3) && (bitcount < size) ; bit++, bitcount++){
								setb1(bitcount, 1);
							}
						}else{ //here '?' is included too !
							for(int bit = 0; (bit < 3) && (bitcount < size) ; bit++, bitcount++){
								setb(bitcount, Z);
							}
						}
					}
				}

				if(n == -1) n = bitcount;
				else {
					int msbBit = getb(bitcount-1);
					if(msbBit > 1)
						while(bitcount < n)setb(bitcount++, msbBit);  //set the remaining to X
				}
				break;
			}//case 8
			case 16:
			{
				if(size == -1){
					size = Integer.MAX_VALUE;
					n = -1;  //set this to rememeber later to set it correctly
					ensureBitCapacity(image.length() * 4);
				}else{
					n = size;
					ensureBitCapacity(n);
				}

				for(int ch = image.length()-1 ; ch >= 0 ; ch--){
					int n;
					switch(image.charAt(ch)){
					case '0':
						n = 0;
						break;
					case '1':
						n = 1;
						break;
					case '2':
						n = 2;
						break;
					case '3':
						n = 3;
						break;
					case '4':
						n = 4;
						break;
					case '5':
						n = 5;
						break;
					case '6':
						n = 6;
						break;
					case '7':
						n = 7;
						break;
					case '8':
						n = 8;
						break;
					case '9':
						n = 9;
						break;
					case 'a':
					case 'A':
						n = 10;
						break;
					case 'b':
					case 'B':
						n = 11;
						break;
					case 'c':
					case 'C':
						n = 12;
						break;
					case 'd':
					case 'D':
						n = 13;
						break;
					case 'e':
					case 'E':
						n = 14;
						break;
					case 'f':
					case 'F':
						n = 15;
						break;
						/*this can be optimized by adding into
						 * vtable16 the X and Z corresp. values"
						 */
					default: n=16;     
					}

					if((n >= 0) && (n < 16)){
						for(int bit = 0; (bit < 4) && (bitcount < size) ; bit++, bitcount++){
							setb0(bitcount, vtable16[n][bit]);
						}
					}else{
						if((image.charAt(ch) == 'x') || (image.charAt(ch) == 'X')){
							for(int bit = 0; (bit < 4) && (bitcount < size) ; bit++, bitcount++){
								setb1(bitcount, 1);
							}
						}else{
							//this must be Z
							for(int bit = 0; (bit < 4) && (bitcount < size) ; bit++, bitcount++){
								setb(bitcount, Z);
							}
						}
					}
				}

				if(n == -1) n = bitcount;
				else{
					int msbBit = getb(bitcount - 1);
					if(msbBit > 1)
						while(bitcount < n)setb(bitcount++, msbBit);  //set the remaining to X
				}
				break;
			}//case 16
			case 256:
			{
				if(size == -1){
					n = size = image.length()*8;
					ensureCapacity(image.length());
				}else{
					n = size;
					ensureBitCapacity(n);
				}

				bitcount = 0;
				for(int i = image.length() -1; i>= 0; i--){
					b0[i] = (byte)image.charAt(i);
				}
			}//case 256
			}
			lsb = 0;
			msb = n - 1;
			increment = 1;

			this.signed = signed; //signed is true only if we get somethin negative, like -7368742834
			if(sign){
				neg();  //keep the 2's complement
				//xConsole.debug("negative: " +image); 
			}

		}
	}

	/**
	 * Check two BitVectors to be identical.
	 * They must have the same length & data.
	 */
	public synchronized boolean equals(BitVector b){
		if(b == null) {

			return false;
		}
		synchronized(b){
			if(n != b.n) {


				return false;
			}

			int len = b0.length - 1;
			byte mask = (byte)((byte)0xffff >> (8 - (n & 7))); //the mask for the last byte of data

			for (int i = 0 ; i < len ; i++)
				if ((b0[i] != b.b0[i]) || (b1[i] != b.b1[i])) {


					return false;
				}

			if (( (b0[len] & mask) != (b.b0[len] & mask) ) ||
					( (b1[len] & mask) != (b.b1[len] & mask) )) {


				return false;
			}
		}
		return true;
	}

	/**
	 * this <- b
	 */
	public synchronized void attrib(BitVector b)
	throws InterpretTimeException{
		synchronized(b){
			if(n!=b.n)
				throw new Error("error: size does not match in assignement");

			b0 = (byte []) b.b0.clone();
			b1 = (byte[]) b.b1.clone();
			notifyMonitors();


		}
	}

	/**
	 * This[start:end] <- b
	 */
	public synchronized void attrib (BitVector b, int start, int end)
	throws InterpretTimeException{
		synchronized(b){
			if(b.n != Math.abs(start-end) +1)
				throw new Error("error: size does not match in assignement");
			attrib(start, end, b, b.msb, b.lsb);
			notifyMonitors();


		}
	}

	/**
	 * This[from:to] <- b[start:end];
	 */
	synchronized void attrib (int from, int to, BitVector b, int start, int end)
	throws InterpretTimeException
	{
		synchronized(b){
			if( (from != to) && ((from < to ? -1 : 1) != increment) ||
					(start != end) && ((start < end ? -1 : 1) != b.increment) )
				throw new InterpretTimeException("MSB and LSB reversed in selection ");
			//      //xConsole.debug("[" + from + ":" + to + "] <- [" +
			//             start + ":" + end +"]");
			int i = from, j = start;
			while(true){
				setBit(i, b.getBit(j));
				if(i == to)break;
				i -= increment;
				j -= b.increment;
			}
			notifyMonitors();


		}
	}

	/**
	 *  this <- valPetru
	 *  val can be < 0 !
	 */
	synchronized void attrib(long val)
	throws InterpretTimeException
	{
		n = 0;
		while(val != 0){
			setb(n, (int)val&1);
			n++;
			val >>= 1; 
		}
		lsb = 0;
		msb = n-1;
		notifyMonitors();

	}

	/**
	 * checks that ths BitVector can hold enough bytes
	 *
	 * @param newLength the number of bytes that this BitVector must hold
	 */
	final protected synchronized void ensureCapacity(int newLength){
		if((b0 != null) && (b1 != null)){
			if(b0.length < newLength){
				int arrayLength = newLength;
				byte newB0[] = new byte [arrayLength],
				newB1[] = new byte [arrayLength];
				System.arraycopy(b0, 0, newB0, 0, b0.length);
				System.arraycopy(b1, 0, newB1, 0, b1.length);
				b0 = newB0;
				b1 = newB1;
			}
		}else{
			b0 = new byte[newLength];
			b1 = new byte[newLength];
		}

	}

	/**
	 * checks that ths BitVector can hold enough bits
	 *
	 * @param newLength the number of bits that this BitVector must hold
	 */
	final protected void ensureBitCapacity(int newLength){
		ensureCapacity((newLength >> 3) +1);
	}

	/**
	 *  Returns the i-th bit from b0.
	 *
	 * @param i the index of requested bit
	 * @exception IndexOutOfBoundsException
	 */
	final protected int getb0(int i){
		byte bitPos = (byte) (i & 0x0007);
		return (b0[i >> 3] & ((byte)0x0001 << bitPos)) >> bitPos;
	}

	/**
	 *  sets the i-th bit from b0
	 */
	final protected void setb0(int i, int v){
		byte mask = (byte) (1 << (i & 0x7));
		if(v != 0)
			b0[i >> 3] |=  mask;
		else
			b0[i >> 3] &=  ~mask;
	}

	/**
	 *  returns the i-th bit from b1
	 */
	final protected int getb1(int i){
		byte bitPos = (byte) (i & 0x0007);
		return (b1[i >> 3] & ((byte)0x0001 << bitPos)) >> bitPos;
	}

	/**
	 *  sets the i-th bit from b1
	 */
	final protected void setb1(int i, int v){
		byte mask = (byte) (1 << (i & 0x7));
		if(v != 0)
			b1[i >> 3] |=  mask;
		else
			b1[i >> 3] &=  ~mask;
	}

	/**
	 *  sets both b0 and b1
	 */
	final protected void setb(int i, int v){
		//    if( (i >= 0) && (i < n)){
		setb0(i, v & 1);
		setb1(i, v & 0x0002);
		//}else //xConsole.debug("Warning: data bounds crossed in BitVector.setBit");
	}

	/**
	 *  returns both b[1:0]
	 */
	final protected int getb(int i){
		//if( (i >= 0) && (i < n))
		return ( getb1(i) << 1 ) | getb0(i);
		/*    else{
      //xConsole.debug("Warning: data bounds crossed in BitVector.getBit");
      return X;
      }*/
	}

	/**
	 *  sets the i'th bit to value (i is in the range lsb..msb !)
	 */
	final public synchronized void setBit(int i, int value){
		setb(increment == 1 ? i - lsb : lsb - i, value);

	}

	/**
	 *  gets the i'th bit value (i is in the range lsb..msb !)
	 */
	final public synchronized int getBit(int i){
		int res =  getb(increment == 1 ? i - lsb : lsb - i);

		return res;
	}

	/**
	 * reduces the BitVector to a single bit vector
	 * @param value the value assigned to the bit
	 */
	final public synchronized void reduce(int value){
		lsb = msb = 0;
		n = 1;
		b0 = new byte [1];
		b1 = new byte [1];
		setb(0, value);

	}

	public synchronized void bAnd(Result r)throws InterpretTimeException{
		try{
			synchronized(r){
				BitVector b = (BitVector)r;
				ensureBitCapacity(b.n);
				for(int i = 0 ; i < n ; i++)
					setb(i, and(getb(i) , b.getb(i)) );

			}
		}catch(ClassCastException ex){
			throw new InterpretTimeException("bitwise operator not allowed on real");
		}

	}

	public synchronized void bOr(Result r)throws InterpretTimeException{
		try{
			synchronized(r){
				BitVector b = (BitVector)r;
				ensureBitCapacity(b.n);
				for(int i = 0 ; i < n ; i++)
					setb(i, or( getb(i) , b.getb(i)) );

			}
		}catch(ClassCastException ex){ //r must be Real
			throw new InterpretTimeException("bitwise operator not allowed on real");
		}

	}

	public synchronized void bXor(Result r)throws InterpretTimeException{
		try{
			synchronized(r){
				BitVector b = (BitVector)r;
				ensureBitCapacity(b.n);
				for(int i = 0 ; i < n ; i++)
					setb(i, xor( getb(i) , b.getb(i)) );

			}
		}catch(ClassCastException ex){
			throw new InterpretTimeException("bitwise operator not allowed on real");
		}

	}

	public synchronized void bNAnd(Result r)throws InterpretTimeException{
		try{
			synchronized(r){
				BitVector b = (BitVector)r;
				ensureBitCapacity(b.n);
				for(int i = 0 ; i < n ; i++)
					setb(i, nots[ and( getb(i) , b.getb(i))]);

			}
		}catch(ClassCastException ex){
			throw new InterpretTimeException("bitwise operator not allowed on real");
		}

	}

	public synchronized void bNOr(Result r)throws InterpretTimeException{
		try{
			synchronized(r) {
				BitVector b = (BitVector)r;
				ensureBitCapacity(b.n);
				for(int i = 0 ; i < n ; i++)
					setb(i, nots[ or( getb(i) , b.getb(i))]);

			}
		}catch(ClassCastException ex){
			throw new InterpretTimeException("bitwise operator not allowed on real");
		}

	}

	public synchronized void bNXor(Result r)throws InterpretTimeException{
		try{
			synchronized(r){
				BitVector b = (BitVector)r;
				ensureBitCapacity(b.n);
				for(int i = 0 ; i < n ; i++)
					setb(i, nots[ xor( getb(i) , b.getb(i))]);

			}
		}catch(ClassCastException ex){
			throw new InterpretTimeException("bitwise operator not allowed on real");
		}

	}

	public synchronized void bNot()throws InterpretTimeException{
		for(int i = 0 ; i < n ; i++)
			setb(i, nots[ (getb1(i) << 1) | getb0(i) ]);

	}

	/**
	 * Reduces this BitVector to a single-bit one wich 
	 * contains its logical truth value (0, 1, or X). 
	 */
	public synchronized void lNot(){
		try{
			bOrR();
			reduce(getb(0));
			bNot();
		}catch(InterpretTimeException ex){
			//this should never happend
			xConsole.dumpStack(ex);
			throw new Error(ex.toString());
		}

	}

	/**
	 * This = -this (2's complement).
	 */
	public synchronized void neg(){
		int i = 0;
		while(i < n)
			if(getb0(i++) != 0)break;
		for(; i < n ; i++)
			setb(i, nots[getb(i)]);

	}

	public synchronized void shl(Result count)throws InterpretTimeException{
		int i;
		synchronized(count){
			try{
				i = (int)count.getInt().value();
			}catch(UndefinedValueException ex){
				reduce(X);
				return;
			}
			shl(i);

		}

	}
	/**
	 * Shift this BitVector with i positions.
	 * If i > 0, the shift is done to the left. In this case, if the BitVector is a reg, its size remains the same. 
	 * Else ( i.e it is an integer) its size will grow in order to accomodate the result of the operation.
	 * Remember that OVI says : shift is always logical (no sign expansion).
	 */

	public synchronized void shl(int i){
		if(i == 0) return;
		int k;
		if(i > 0){ //shift left
			if(signed){
				//xConsole.debug("BitVector.shl(int) : signed BitVector: " + toString(10));
				expand0(n+i); //integers will grow to the necessary capacity
				//xConsole.debug("BitVector.shl(int); after ex[pansion: " + toString(10));
			}else
				if(i > n){   //if i > capacity, just set it to 0 and return
					for(int len = b0.length, j = 0;  j < len ; j++)
						b0[j] = b1[i] = 0;
					return;
				}
			for(k = n-1; k >= i ; k--)
				setb(k, getb(k-i));
			while(k >= 0){
				setb(k, 0);
				k--;
			}
		}else{ //shift right
			i = -i;
			if(i > n){
				for(int len = b0.length, j = 0;  j < len ; j++)
					b0[j] = b1[i] = 0;
				return;
			}
			for(k = 0; k < n-i ; k++)
				setb(k, getb(k+i));
			while(k < n){
				setb(k, 0);
				k++;
			}
		}

	}

	public synchronized void shr(Result count)throws InterpretTimeException{
		int i;
		synchronized(count){
			try{
				i = (int)count.getInt().value();
			}catch(UndefinedValueException ex){
				reduce(X);

				return;
			}

		}
		i = -i;
		shl(i);

	}

	public synchronized boolean isDefined(){
		int len = b1.length-1;
		for(int i = 0 ; i < len ; i++)
			if(b1[i] != 0){
				return false;
			}
		if ( (b1[len] & ((byte)0xffff >> (8 - (n & 7)))) != 0 ) {
			return false;
		}

		return true;
	}

	/**
	 * Tests if this bivector is 0
	 */
	public synchronized boolean isNull(){
		int len = b0.length, i;
		for (i = 0; i < b0.length - 1; i++)
			if ((b0[i] != 0) || (b1[i] != 0)) {
				return false;
			}
		int mask = 0xff >> (8 - (n & 7));
		if ((b0[i] & mask) != 0 || (b1[i] & mask) != 0) {
			return false;
		}

		return true;
	}

	public synchronized boolean isZ(){
		int len = b0.length, i;
		if(len > 1){
			for(i = 1; i < b0.length - 1; i++)
				if((b0[i] != 0) || (b1[i] != 0)) {
					return false;
				}
			int mask = 0xff >> (8 - (n & 7));
			if((b0[i] & mask) != 0 || (b1[i] & mask) != 0) {
				return false;
			}
		}
		if(b0[0] == (Z & 1) && b1[0] == (Z & 2) >> 1) {
			return true;
		}

		return false;
	}

	public synchronized boolean isX(){
		int len = b0.length, i;
		if(len > 1){
			for(i = 1; i < b0.length - 1; i++)
				if((b0[i] != 0) || (b1[i] != 0)) {
					return false;
				}
			int mask = 0xff >> (8 - (n & 7));
			if((b0[i] & mask) != 0 || (b1[i] & mask) != 0) {
				return false;
			}
		}
		if(b0[0] == (X & 1) && b1[0] == (X & 2) >> 1) {
			return true;
		}

		return false;
	}

	public synchronized Result lEq(Result r){
		synchronized(r){
			if(!isDefined()) {

				return BitVector.bX();
			}
			try{
				BitVector b = (BitVector)r;
				int len = b0.length - 1;
				int mask = (byte)0xffff >> (8 - (n & 7));

		if(!b.isDefined()) {

			return BitVector.bX();
		}
		for(int i = 0 ; i < len ; i++)
			if(b0[i] != b.b0[i]) {


				return BitVector.b0();
			}
		if((b0[len] & mask) != (b.b0[len] & mask)) {

			return BitVector.b0();
		}

		return BitVector.b1();
			}catch(ClassCastException ex){

				return getReal().lEq(r); 
			}
		}
	}

	public synchronized Result lNEq(Result r){
		synchronized(r){
			Result res = lEq(r);
			res.lNot();

			return res;
		}
	}

	public synchronized Result cEq(Result r)throws InterpretTimeException{
		synchronized(r){
			try{
				BitVector b = (BitVector)r;
				byte longerB0[], longerB1[];
				int nShort, nLong;
				byte maskShort, maskLong;

				if(n < b.n){
					nShort = b0.length -1;
					nLong = b.b0.length - 1;
					longerB0 = b.b0;
					longerB1 = b.b1;
					maskShort = (byte) ((byte)0xffff >> (8 - (n & 7))); 
					maskLong = (byte) ((byte)0xffff >> (8 - (b.n & 7)));
				}else{
					nShort = b.b0.length -1;
					nLong = b0.length - 1;
					longerB0 = b0;
					longerB1 = b1;
					maskShort = (byte) ((byte)0xffff >> (8 - (b.n & 7))); 
					maskLong = (byte) ((byte)0xffff >> (8 - (n & 7)));
				}
				int i;

				//first check the equality for the common bytes:
					for( i = 0 ; i < nShort ; i++)
						if((b0[i] != b.b0[i]) || (b1[i] != b.b1[i])) {

							return BitVector.b0();
						}

				//check the last byte from the shorter bitVector:
					if(((b0[i] & maskShort) != (b.b0[i] & maskShort)) ||
							((b1[i] & maskShort) != (b.b1[i] & maskShort))) {

						return BitVector.b0();
					}
					if(((longerB0[i] & ~maskShort) != 0) ||
							((longerB1[i] & ~maskShort) != 0)) {

						return BitVector.b0();
					}
					//now check the remaining bytes from the longer one
					for( i++ ; i < nLong ; i++)
						if((longerB0[i] != 0) || (longerB1[i] != 0)) {

							return BitVector.b0();
						}
					if(((longerB0[i] & maskLong) != 0) ||
							((longerB1[i] & maskLong) != 0)) {//the last byte

						return BitVector.b0();
					}

					return BitVector.b1(); //ok here we know they are equal
			}catch(ClassCastException ex){

				throw new InterpretTimeException("bitwise operator not allowed on real");
			}
		}
	}

	public synchronized Result cNEq(Result r)throws InterpretTimeException{
		synchronized(r){
			Result res = cEq(r);
			res.lNot();

			return res;
		}
	}

	public synchronized Result lt(Result r){
		synchronized(r){
			try{
				BitVector b = (BitVector)r;
				try{
					if(value() < b.value()) {


						return BitVector.b1();
					}

					return BitVector.b0();
				}catch(UndefinedValueException ex){

					return BitVector.bX();
				}
			}catch(ClassCastException ex){

				return getReal().lt(r);
			}
		}
	}

	public synchronized Result ge(Result r){
		synchronized(r){
			Result res = lt(r);
			res.lNot();

			return res;
		}
	}

	public synchronized Result gt(Result r){
		synchronized(r){
			try{
				BitVector b = (BitVector)r;
				try{
					if(value() > b.value()) {

						return BitVector.b1();
					}

					return BitVector.b0();
				}catch(UndefinedValueException ex){

					return BitVector.bX();
				}
			}catch(ClassCastException ex){

				return getReal().gt(r);
			}
		}
	}

	public synchronized Result le(Result r){
		synchronized(r){
			Result res = gt(r);
			res.lNot();


			return res;
		}
	}

	public synchronized void bAndR()throws InterpretTimeException{
		//and reduction
		int r=getb(0);
		for(int i = 1 ; i < n ; i++)
			r = ands[ (r << 2) | (getb1(i) << 1) | getb0(i)];
		reduce(r);
	}

	public synchronized void bOrR()throws InterpretTimeException{
		//or reduction
		int r=getb(0);
		for(int i = 1 ; i < n ; i++)
			r = ors[ (r << 2) | (getb1(i) << 1) | getb0(i)];
		reduce(r);
	}

	public synchronized void bXOrR()throws InterpretTimeException{
		//xor reduction
		int r=getb(0);
		for(int i = 1 ; i < n ; i++)
			r = xors[ (r << 2) | (getb1(i) << 1) | getb0(i)];
		reduce(r);
	}

	public synchronized void bNAndR()throws InterpretTimeException{
		//nand reduction
		int r=getb(0);
		for(int i = 1 ; i < n ; i++)
			r = ands[ (r << 2) | (getb1(i) << 1) | getb0(i) ];
		r = nots[r];
		reduce(r);
	}

	public synchronized void bNOrR()throws InterpretTimeException{
		//nor reduction
		int r=getb(0);
		for(int i = 1 ; i < n ; i++)
			r = ors[ (r << 2) | (getb1(i) << 1) | getb0(i) ];
		r = nots[r];
		reduce(r);
	}

	public synchronized void bNXOrR()throws InterpretTimeException{
		//nxor reduction
		int r=getb(0);
		for(int i = 1 ; i < n ; i++)
			r = xors[ (r << 2) | (getb1(i) << 1) | getb0(i)] ;
		r = nots[r];
		reduce(r);
	}

	/**
	 * Reduces the BitVector according to the given
	 * given truth table
	 */
	public synchronized void genericReduction(int [] table)
	throws InterpretTimeException
	{
		int r=getb(0);
		for(int i = 1 ; i < n ; i++) {
			int tmp = (r << 2) | (getb1(i) << 1) | getb0(i);
			r = table[tmp] ;
			xConsole.debug("r: " + r + " tmp: " + tmp);
		}
		reduce(r);
	}

	/**
	 * Same as genericReduction, but the
	 * direction is reversed, because I don't want
	 * to transpose the truth tables from the Standard.
	 */
	public synchronized void genericReduction1(int [] table)
	throws InterpretTimeException
	{
		int r=getb(n-1);
		for(int i = n-2 ; i >= 0 ; i--) {
			int tmp = (r << 2) | (getb1(i) << 1) | getb0(i);
			r = table[tmp] ;
			xConsole.debug("r: " + r + " tmp: " + tmp);
		}
		reduce(r);
	}

	/**
	 * performs arithmetic (Signed) expansion if signed is set
	 * (for Luke : i.e. with sign extension)
	 * the trick is that it preserves the sign bit only for integers,
	 * so when assigning an integer to a register, 
	 * the int always appeares to be "longer" than it, from the register's point of view
	 * and thus, the integer's size need not to be fixed, nor limited
	 */
	synchronized void expandS(int newN){
		if(signed){
			if(newN < n)
				throw new Error("in function BitVector.expandS : newN < n");
			int s = getb( n -1);

			int i = n;
			msb += msb >= 0 ? newN - n : n - newN;
			n = newN;  //this is needed before for to avoid setb's annoying message
			ensureBitCapacity(n);
			for(; i < newN ; i++)
				setb(i, s);
		}else{
			expand0(newN);
		}

	}

	public synchronized int getSign(){
		if(signed && (getb0(n-1) != 0)) {

			return 1;
		}

		return 0;
	}

	/**
	 * this = this + b
	 * does NOT guarantees the integrity of b !
	 */
	public synchronized Result add(Result r){
		synchronized(r){
			if(!isDefined()) {

				return BitVector.bX();
			}
			try{
				BitVector b = (BitVector)r;
				if(!b.isDefined())return BitVector.bX();
				boolean needsCorrection = getSign() != b.getSign();
				boolean newSigned = needsCorrection ? true : getSign() != 0 ;
				int nn = n >= b.n ? n : b.n; 
				expandS(nn+1);    
				b.expandS(nn+1);
				//xConsole.debug("BitVector.add: before addition: this: " + this + " b = " + b);
				int carry = 0;
				int i;
				for(i= 0; i < nn; i++){
					carry = carry + getb0(i) + b.getb0(i);
					setb0(i, carry & 1);
					carry >>= 1;
				}
				if(!needsCorrection)setb0(i, carry); //tries to preserve the carry in case of 
				//absolute addition
				else setb0(i, getb0(i-1));
				signed = newSigned; //finally set the signed
				//xConsole.debug("BitVector.add: after addition: this: " + this + "
				//      length: " + b0.length);


				return this;
			}catch(ClassCastException ex){
				return getReal().add(r);
			}
		}
	}

	/**
	 * this = this - r
	 * does NOT guarantee the integrity of parameter r
	 * implementation is:
	 * r = -r
	 * this = this+r
	 */
	public synchronized Result sub(Result r){
		synchronized(r){
			try{
				BitVector b  = (BitVector)r;
				if(!b.signed){
					b.expand0(b.n+1);  //convet b to integer
					b.signed = true;
				}
				b.neg();
				return add(b);
			}catch(ClassCastException ex){
				return getReal().sub(r);
			}
		}
	}

	/**
	 * Whata' ...
	 */
	synchronized byte [] reverse(byte[] original){
		byte result[] = new byte [original.length];
		for(int i = 0, j = original.length-1 ; j >= 0 ; j--, i++)
			result[i] = original[j];

		return result;
	}

	/**
	 * Multiplies this BitVector and r.
	 * The result is stored into this.
	 */
	public synchronized Result mul(Result r){
		synchronized(r){
			try{
				BitVector b = (BitVector)r;
				if((!isDefined()) || (!b.isDefined()))
					return BitVector.bX();

				//fill in the high byte with:
				expandS(b0.length * 8);
				b.expandS(b.b0.length * 8);

				BigInteger me = new BigInteger(reverse(b0)),
				him = new BigInteger(reverse(b.b0));
				me = me.multiply(him);

				b0 = me.toByteArray();
				b1 = new byte[b0.length];

				int newLength = b0.length * 8;
				msb += msb >= 0 ? newLength - n : n - newLength;
				n = newLength;


				return this;
			}catch(ClassCastException ex){
				return getReal().mul(r);
			}
		}
	}

	/**
	 * 
	 * The result is stored into this.
	 */
	public synchronized Result mod(Result r)throws InterpretTimeException{
		synchronized(r){
			try{
				BitVector b = (BitVector)r;
				if((!isDefined()) || (!b.isDefined()))
					return BitVector.bX();

				//fill in the high byte with:
				expandS(b0.length * 8);
				b.expandS(b.b0.length * 8);

				BigInteger me = new BigInteger(reverse(b0)),
				him = new BigInteger(reverse(b.b0));
				me = me.mod(him);

				b0 = me.toByteArray();
				b1 = new byte[b0.length];

				int newLength = b0.length * 8;
				msb += msb >= 0 ? newLength - n : n - newLength;
				n = newLength;


				return this;
			}catch(ClassCastException ex){
				throw new InterpretTimeException("modulus not allowed on Real ");
			}
		}
	}

	/**
	 * todo: implementation
	 *
	 */
	public synchronized Result div(Result r){
		synchronized(r){
			try{
				BitVector b = (BitVector)r;
				if((!isDefined()) || (!b.isDefined()))
					return BitVector.bX();

				//fill in the high byte with:
				expandS(b0.length * 8);
				b.expandS(b.b0.length * 8);

				BigInteger me = new BigInteger(reverse(b0)),
				him = new BigInteger(reverse(b.b0));
				me = me.divide(him);

				b0 = me.toByteArray();
				b1 = new byte[b0.length];

				int newLength = b0.length * 8;
				msb += msb >= 0 ? newLength - n : n - newLength;
				n = newLength;


				return this;
			}catch(ClassCastException ex){
				return getReal().div(r);
			}
		}
	}

	/**
	 *  converts this BitVector to its binary representation into a long
	 */
	public synchronized long bitsToLong()throws UndefinedValueException{
		//TODO: optimizare
		long v=0;
		for(int i = n-1 ; i >= 0 ; i--){
			v |= (long)getb0(i) << i;
			if(getb1(i) != 0) {

				throw new UndefinedValueException();
			}
		}

		return v;
	}

	/**
	 * computes the value represented by this BitVector
	 * @return the value represented by this BitVector. If some bits are X or Z, the result is undefined.
	 * @exception UndefinedValueException
	 */
	public synchronized long value()throws UndefinedValueException{
		long v=0;
		for(int i = n-1 ; i >= 0 ; i--){
			v |= (long)getb0(i) << i;
			if(getb1(i) != 0) {

				throw new UndefinedValueException();
			}
		}
		if(signed && (getb0(n-1) != 0)) //extind semnul ?
			v |= 0xffffffffffffffffl << n;

		return v;
	}

	public synchronized String toString(){
		String result = n + "\'b" + toString(2);
		try{
			if(signed)result = result +" ('d" + value() +")";
		}catch(UndefinedValueException ex){
			result += "('d??)";
		}

		return result;
	}

	public synchronized String toString(int base){
		String result = "";
		switch(base){
		case 2:
			for(int i = n-1 ; i >= 0 ; i--){
				switch((getb1(i) << 1) | getb0(i)){
				case 0: 
					result += "0";
					break;
				case 1:
					result += "1";
					break;
				case 2:
					result += "X";
					break;
				case 3:
					result += "Z";
				}
				if((i % 4 == 0) && (i != 0)) result += "_";
			}
			break;
		case 8:
			for(int i = n-1, n = 0; i>= 0 ; i--){
				if(getb(i) == 1)n++;
				else n = 10;  //this nibble will be indefined
				if((i % 3) == 0){
					result += n < 10 ? "X" : Integer.toString(n);
					n = 0;
				}
			}
			break;
		case 10:{

			int length = ((n-1) >> 3) + 1; // this is the part of the BitVector that is really significant
			byte [] bb = new byte[length];

			//set the X bits to 0:
			for(int i = 1, j = length - 2; j >= 0 ; i++, j--){
				bb[i] = (byte)(b0[j] & (byte)~b1[j]);
				////xConsole.debug("BitVector.toString(10): b0[" + j + "] = " + Integer.toHexString(b0[j]));
			}

			bb[0] = (byte) ((b0[length - 1] & ~b1[length - 1]) | ( signed && (b0[length - 1] & ( 1 << ((n & 7) - 1))) != 0 ? (0xffff << (n & 7)) : 0));
			////xConsole.debug("negative : " + (b0[length - 1] & ( 1 << ((n & 7) - 1))) + "sign bit is :  " + ((n & 7) - 1));
			//if signed and negative, extend the sign bit

			////xConsole.debug("BitVector.toString(10): b0[" + (length-1) + "] = " + Integer.toHexString(b0[length - 1]));

			BigInteger big = new BigInteger(bb);
			result = big.toString();
			break;
		}
		case 16:{
			byte [] bb = new byte[b0.length];

			//set the X bits to 0:
			for(int j = b0.length -1; j >= 0 ; j--)
				bb[j] = (byte) ((byte)b0[j] & ~(byte)b1[j]);

			for(int i = bb.length - 1 ; i >= 0 ; i--){
				result += Character.forDigit(bb[i] >> 4, 16);
				result += Character.forDigit(bb[i] & 0xf, 16);
			}
		}
		case 256:
			byte [] chars = new byte[b0.length];
			//set the X bits to 0:
			for(int i = 0, j = b0.length - 1; j >= 0 ; j--,i++)
				chars[j] = (byte) ((byte)b0[i] & ~(byte)b1[i]); 

			for(int i = chars.length - 1 ; i >= 0 ; i--)
				result += "" + new Character((char)(chars[i]));
			break;
		default:
			throw new Error("BitVector: toString(int base): illegal base " + base);
		}

		return result;
	}

	/**
	 * expands the bitvector to the new size, padding the new space with 0
	 * the msb is adjusted accordingly
	 */
	synchronized void expand0(int newLength){
		if(newLength < n)  
			throw new Error("newLength < n in BitVector.expand0(..)");

		ensureBitCapacity(newLength);
		msb += msb >= 0 ? newLength - n : n - newLength;
		int i;
		for( i = n , n = newLength ; i < n ; i++)
			setb(i, 0);

	}

	/**
	 * cuts off the most significants bits, so the length remains newL...
	 */
	synchronized void trunc(int newLength){
		if(newLength > n)
			throw new Error("in function BitVector.cut : newLength > n");
		msb -= msb >= 0 ? n - newLength : newLength - n;
		n = newLength;

	}

	/**
	 * Modifies the length of this Bitvector, expanding or shrinking it as necessary.
	 */
	synchronized void setLength(int length){
		if(n > length)trunc(length);
		else if(n < length)expandS(length);
	}

	/**
	 * Looks for a value change between this and b (used for edge detesction).
	 */
	synchronized int compare(BitVector b){
		if(b == null){


			return ValueChangeMonitor.EVT_CHANGE; //see comment below
		}
		synchronized(b){
			//xConsole.debug("" + b);
			//xConsole.debug("" + this);
			int result = 0;
			if(!equals(b)) result |= ValueChangeMonitor.EVT_CHANGE;
			int ln = (b.getb1(0)<<1) | b.getb0(0),
			col = (getb1(0)<<1) | getb0(0);
			result |= (posedge[ln][col] * ValueChangeMonitor.EVT_POSEDGE);
			result |= (negedge[ln][col] * ValueChangeMonitor.EVT_NEGEDGE);


			return result;
		}//if b == null, assume no change (stupid ain't it ??)
	}

	/**
	 * Computes the acutal value for each bit, considering 
	 * the BitSelects assigned to it. It's suitable for reg continuous assign statement(i guess..).
	 * @return the value obtained from arbitration of all the values driving this BitVector.
	 */
	synchronized Object[] compute()throws InterpretTimeException{
		BitSet assigned = new BitSet(n);
		BitVector b = new BitVector(this);
		for(Enumeration e = data.elements() ; e.hasMoreElements() ; ){
			ContBitSelect ck = (ContBitSelect)e.nextElement();
			int i = ck.start, inc = ck.start <= ck.end ? 1 : -1;
			for(;i <= ck.end ; i += inc){
				//is this the first assignement?
				if(assigned.get(increment == 1 ? i - lsb : lsb - i)){
					b.setBit(i, lookupTableWire[ck.b.getBit(i)][getBit(i)]);  //i guess this is the implicit table
				}else{
					assigned.set(increment == 1 ? i - lsb : lsb - i);
					b.setBit(i, ck.data.getBit(i));
				}
			}
		}
		Object [] res = {b};

		return res;
	}

	/**
	 * used to combine the alternatives in the ambiguity case of ?: operator
	 */
	protected final static int abgCond[][] = {
		{0, X, X, X},
		{X, 1, X, X},
		{X, X, X, X},
		{X, X, X, X}
	};

	/**
	 * combines this and b for the ambiguous case of ?:
	 */
	public final synchronized void combineFAbgCond(BitVector b){
		synchronized(b){
			if(n < b.n)expand0(b.n);
			for(int i = 0; i < n; i++)
				setb(i, abgCond[getb(i)][b.getb(i)]);

		}

	}

	//Result implementation ->

	/**
	 *  a combination of the $bitstoreal and
	 *  the $itor system functions
	 *  @see Result
	 */
	//TODO: check up the behaviour with VeriWell
	//TODO: a non-limited version
	public synchronized Real getReal(){
		try{
			if(signed){
				long l = value();

				return new Real(l);
			}else {
				return new Real(Double.longBitsToDouble(bitsToLong()));
			}
		}catch(UndefinedValueException ex){

			return new Real(0);
		}
	}

	/**
	 *  @see Result
	 */
	public synchronized BitVector getBits(){
		BitVector b = new BitVector(this);
		b.signed = false;

		return b;
	}

	/**
	 *  @see Result
	 */
	public synchronized BitVector getInt(){
		BitVector b = new BitVector(this);
		//xConsole.debug(b+"");

		return b;
	}

	/**
	 *  @see Result
	 */
	public synchronized int getBool(){
		//TODO: optimized version
		int result = 0;
		for(int i = 0; i < n ; i++)
			result |= getb(i);

		return result;
	}

	public synchronized long getLong(){
		try{
			long l = value();

			return l;
		}catch(UndefinedValueException ex){

			return 0;  //e corect ?
		}
	}

	/**
	 *  @see Result
	 */
	public synchronized Result duplicate(){
		BitVector b = new BitVector(this);
		return b;
	}

	public synchronized boolean isTrue(){
		boolean b = getBool() == 1;
		return b;
	}

	public int getType(){
		if(signed)return Symbol.intType;
		return Symbol.regType;
	}

	public int length() {
		return n;
	}

	// <- Result implementation

}

