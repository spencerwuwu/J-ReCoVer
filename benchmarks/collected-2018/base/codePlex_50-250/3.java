// https://searchcode.com/api/result/2384673/

//#preprocessor

//---------------------------------------------------------------------------------
//
//  Little Color Management System
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the Software
// is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
//---------------------------------------------------------------------------------
//
//@Author Vinnie Simonetti
package littlecms.internal.helper;

//#ifdef CMS_USE_IO_BIT_CONVERTER
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
//#endif

import littlecms.internal.LCMSResource;

/**
 * Data type converter, byte order is Little Endian.
 */
public final class BitConverter
{
	public static byte[] getBytes(short value)
	{
//#ifdef CMS_USE_IO_BIT_CONVERTER
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream(2);
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeShort(value);
			return baos.toByteArray();
		}
		catch (IOException e)
		{
		}
		//Fallback on error
//#endif
		return new byte[]{(byte)((value >> 8) & 0xFF), (byte)(value & 0xFF)};
	}
	
	public static byte[] getBytes(int value)
	{
//#ifdef CMS_USE_IO_BIT_CONVERTER
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeInt(value);
			return baos.toByteArray();
		}
		catch (IOException e)
		{
		}
		//Fallback on error
//#endif
		return new byte[]{(byte)((value >> 24) & 0xFF), (byte)((value >> 16) & 0xFF), (byte)((value >> 8) & 0xFF), (byte)(value & 0xFF)};
	}
	
	public static byte[] getBytes(long value)
	{
//#ifdef CMS_USE_IO_BIT_CONVERTER
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream(8);
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeLong(value);
			return baos.toByteArray();
		}
		catch (IOException e)
		{
		}
		//Fallback on error
//#endif
		return new byte[]{(byte)((value >> 56) & 0xFF), (byte)((value >> 48) & 0xFF), (byte)((value >> 40) & 0xFF), (byte)((value >> 32) & 0xFF),
				(byte)((value >> 24) & 0xFF), (byte)((value >> 16) & 0xFF), (byte)((value >> 8) & 0xFF), (byte)(value & 0xFF)};
	}
	
	public static byte[] getBytes(char value)
	{
		return getBytes((short)value);
	}
	
	public static byte[] getBytes(float value)
	{
		return getBytes(Float.floatToIntBits(value));
	}
	
	public static byte[] getBytes(double value)
	{
		return getBytes(Double.doubleToLongBits(value));
	}
	
	//Helper function to reduce code
	private static void argumentCheck(byte[] value, int startIndex, int size)
	{
		if (value == null)
	    {
			throw new NullPointerException("value");
	    }
	    if ((startIndex & 0xFFFFFFFFL) >= value.length)
	    {
	    	throw new IndexOutOfBoundsException("startIndex");
	    }
	    if (startIndex > (value.length - size))
	    {
	    	throw new IndexOutOfBoundsException(Utility.LCMS_Resources.getString(LCMSResource.BITCONVERTER_VALUE_TOO_SMALL));
	    }
	}
	
	public static short toInt16(byte[] value, int startIndex)
	{
		argumentCheck(value, startIndex, 2);
//#ifdef CMS_USE_IO_BIT_CONVERTER
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(value, startIndex, 2);
			DataInputStream dis = new DataInputStream(bais);
			return dis.readShort();
		}
		catch (IOException e)
		{
		}
		//Fallback on error
//#endif
		return (short)((value[startIndex + 1] & 0xFF) | ((value[startIndex] & 0xFF) << 8));
	}
	
	public static int toInt32(byte[] value, int startIndex)
	{
		argumentCheck(value, startIndex, 4);
//#ifdef CMS_USE_IO_BIT_CONVERTER
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(value, startIndex, 4);
			DataInputStream dis = new DataInputStream(bais);
			return dis.readInt();
		}
		catch (IOException e)
		{
		}
		//Fallback on error
//#endif
		return ((value[startIndex + 3] & 0xFF) | ((value[startIndex + 2] & 0xFF) << 8) | ((value[startIndex + 1] & 0xFF) << 16) | ((value[startIndex] & 0xFF) << 24));
	}
	
	public static long toInt64(byte[] value, int startIndex)
	{
		argumentCheck(value, startIndex, 8);
//#ifdef CMS_USE_IO_BIT_CONVERTER
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(value, startIndex, 8);
			DataInputStream dis = new DataInputStream(bais);
			return dis.readLong();
		}
		catch (IOException e)
		{
		}
		//Fallback on error
//#endif
		int low = ((value[startIndex + 7] & 0xFF) | ((value[startIndex + 6] & 0xFF) << 8) | ((value[startIndex + 5] & 0xFF) << 16) | ((value[startIndex + 4] & 0xFF) << 24));
		int high = ((value[startIndex + 3] & 0xFF) | ((value[startIndex + 2] & 0xFF) << 8) | ((value[startIndex + 1] & 0xFF) << 16) | ((value[startIndex] & 0xFF) << 24));
		return (low & 0xFFFFFFFFL) | ((high & 0xFFFFFFFFL) << 32);
	}
	
	public static char toChar(byte[] value, int startIndex)
	{
		return (char)toInt16(value, startIndex);
	}
	
	public static float toFloat(byte[] value, int startIndex)
	{
		return Float.intBitsToFloat(toInt32(value, startIndex));
	}
	
	public static double toDouble(byte[] value, int startIndex)
	{
		return Double.longBitsToDouble(toInt64(value, startIndex));
	}
}

