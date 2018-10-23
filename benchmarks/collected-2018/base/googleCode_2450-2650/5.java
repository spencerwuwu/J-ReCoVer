// https://searchcode.com/api/result/11920241/

/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/Fmt.java#14 $
*/

package ariba.util.core;

import ariba.util.io.FormattingSerializer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
    Formatted output.  Like printf, except the only directives are
    <p>
    %%  -- print a single %
    <p>
    %/  -- print a java.io.File.separator
    <p>
    %[-][0][nn][.mm]s -- toString and print the next argument If the
    argument if fewer than NN characters, it is padded to NN
    characters. Padding is on the left if the directive starts with
    '-', otherwise it is on the right. Fields are padded with spaces,
    unless the optional '0' (zero) character is given, in which case
    the string is padded with zeros. There is no way to specify
    another pad character. If the argument has more than MM
    characters, it is truncated to MM characters. If NN is not given,
    there is no padding. If MM is not given, there is no truncation.

    @aribaapi documented
*/
public class Fmt
{
    /**
        @aribaapi private
    */
    public static final Object Null = new NoArgument();

    /**
        @aribaapi private
    */
    public static final Object[] NoMore = new Object[0];


    /**
        Don't want a hidden constructor.
    */
    private Fmt ()
    {
    }


    private static final int StartST             = 1;
    private static final int GetPadCharST        = 2;
    private static final int GetPreNumberST      = 3;
    private static final int DonePreST           = 4;
    private static final int GetPostNumberST     = 5;
    private static final int DonePostST          = 6;
    private static final int CollectNumberST     = 7;
    private static final int ConsumeDotST        = 8;
    private static final int GetFinalCharacterST = 9;
    private static final int UnexpectedEndST     = 10;
    private static final int PrintNextObjectST   = 11;

    /*
        Keep around a buffer to reduce stoage allocation

        JP:  Netscape 3 doesn't support locking TheBuffer.  Locking array
        objects appear to cause class verifier errors with Netscape 3.  I
        reverted the code to use BufLock instead.
    */

    private static final BufferAllocator bufferAllocator = getBufferAllocator();

    private static BufferAllocator getBufferAllocator ()
    {
        return new ThreadLocalBufferAllocator();
    }
    /**
        @aribaapi private
    */
    public static FormatBuffer getBuffer ()
    {
        return bufferAllocator.getBuffer();
    }

    /**
        @aribaapi private

    */
    public static void freeBuffer (FormatBuffer buf)
    {
        bufferAllocator.freeBuffer(buf);
    }

        // ----------------------------------------------------------------
        // Format into a string and return it



    /**
        Format a message into a String.

        @param control a String defining the format of the output

        @return the formatted String
        @aribaapi documented
    */
    public static String S (String control)
    {
        return S(control, 0, Null, Null, Null, Null, Null, Null, NoMore);
    }

    /**
        Format a message into a String.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>

        @return the formatted String
        @aribaapi documented
    */
    public static String S (String control, Object a1)
    {
        return S(control, 0, a1, Null, Null, Null, Null, Null, NoMore);
    }

    /**
        Format a message into a String.

        @param control a String defining the format of the output
        @param i1 an integer argument to the format string
        <b>control</b>

        @return the formatted String
        @aribaapi documented
    */
    public static String S (String control, int i1)
    {
        return S(control, 0, Constants.getInteger(i1), Null, Null, Null,
                 Null, Null, NoMore);
    }

    /**
        Format a message into a String.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>

        @return the formatted String
        @aribaapi documented
    */
    public static String S (String control, Object a1, Object a2)
    {
            // Optimize a common, simple case, even though people should know
            // to call concat for this.
        if (control == "%s%s" &&
            a1 instanceof String &&
            a2 instanceof String)
        {
            return ((String)a1).concat((String)a2);
        }
        return S(control, 0, a1, a2, Null, Null, Null, Null, NoMore);
    }

    /**
        Format a message into a String.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>

        @return the formatted String
        @aribaapi documented
    */
    public static String S (String control, Object a1, Object a2, Object a3)
    {
        return S(control, 0, a1, a2, a3, Null, Null, Null, NoMore);
    }

    /**
        Format a message into a String.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>

        @return the formatted String
        @aribaapi documented
    */
    public static String S (
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4)
    {
        return S(control, 0, a1, a2, a3, a4, Null, Null, NoMore);
    }

    /**
        Format a message into a String.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>

        @return the formatted String
        @aribaapi documented
    */
    public static String S (
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5)
    {
        return S(control, 0, a1, a2, a3, a4, a5, Null, NoMore);
    }

    /**
        Format a message into a String.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>
        @param a6 the sixth argument to the format string
        <b>control</b>

        @return the formatted String
        @aribaapi documented
    */
    public static String S (
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6)
    {
        return S(control, 0, a1, a2, a3, a4, a5, a6, NoMore);
    }

    /**
        Format a message into a String.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>
        @param a6 the sixth argument to the format string
        <b>control</b>
        @param more an array containing all subsequent arguments to
        the format string <b>control</b> after the sixth one.

        @return the formatted String
        @deprecated use S(String, Object[])
        @aribaapi documented
    */
    public static String S (
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6,
        Object[] more)
    {
        return S(control, 0, a1, a2, a3, a4, a5, a6, more);
    }

    /**
        Format a message into a String.

        @param control a String defining the format of the output
        @param args an array containing all arguments to the format
        string <b>control</b>

        @return the formatted String
        @aribaapi documented
    */
    public static String S (String control, Object[] args)
    {
        return S(control, 6, Null, Null, Null, Null, Null, Null, args);
    }

    private static String S (
        String control,
        int firstArg,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6,
        Object[] more)
    {
        FormatBuffer buf = getBuffer();
        B(buf, control, firstArg, a1, a2, a3, a4, a5, a6, more);
        String result = buf.toString();
        freeBuffer(buf);
        return result;
    }

        // ----------------------------------------------------------------
        // Format into an writer


    /**
        Format a message into a PrintWriter.

        @param out a PrintWriter to format the output into
        @param control a String defining the format of the output
        @aribaapi documented
    */
    public static void F (PrintWriter out, String control)
    {
        F(out, control, 0, Null, Null, Null, Null, Null, Null, NoMore);
    }

    /**
        Format a message into a PrintWriter.

        @param out a PrintWriter to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @aribaapi documented
    */
    public static void F (PrintWriter out, String control, Object a1)
    {
        F(out, control, 0, a1, Null, Null, Null, Null, Null, NoMore);
    }

    /**
        Format a message into a PrintWriter.

        @param out a PrintWriter to format the output into
        @param control a String defining the format of the output
        @param i1 an integer argument to the format string
        <b>control</b>
        @aribaapi documented
    */
    public static void F (PrintWriter out, String control, int i1)
    {
        F(out, control, 0, Constants.getInteger(i1),
          Null, Null, Null, Null, Null, NoMore);
    }

    /**
        Format a message into a PrintWriter.

        @param out a PrintWriter to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @aribaapi documented
    */
    public static void F (
        PrintWriter out,
        String control,
        Object a1,
        Object a2)
    {
        F(out, control, 0, a1, a2, Null, Null, Null, Null, NoMore);
    }

    /**
        Format a message into a PrintWriter.

        @param out a PrintWriter to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @aribaapi documented
    */
    public static void F (
        PrintWriter out,
        String control,
        Object a1,
        Object a2,
        Object a3)
    {
        F(out, control, 0, a1, a2, a3, Null, Null, Null, NoMore);
    }

    /**
        Format a message into a PrintWriter.

        @param out a PrintWriter to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @aribaapi documented
    */
    public static void F (
        PrintWriter out,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4)
    {
        F(out, control, 0, a1, a2, a3, a4, Null, Null, NoMore);
    }

    /**
        Format a message into a PrintWriter.

        @param out a PrintWriter to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>
        @aribaapi documented
    */
    public static void F (
        PrintWriter out,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5)
    {
        F(out, control, 0, a1, a2, a3, a4, a5, Null, NoMore);
    }

    /**
        Format a message into a PrintWriter.

        @param out a PrintWriter to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>
        @param a6 the sixth argument to the format string
        <b>control</b>
        @aribaapi documented
    */
    public static void F (
        PrintWriter out,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6)
    {
        F(out, control, 0, a1, a2, a3, a4, a5, a6, NoMore);
    }

    /**
        Format a message into a PrintWriter.

        @param out a PrintWriter to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>
        @param a6 the sixth argument to the format string
        <b>control</b>
        @param more an array containing all subsequent arguments to
        the format string <b>control</b> after the sixth one.
        @deprecated use F(PrintWriter, String, Object[])
        @aribaapi documented
    */
    public static void F (
        PrintWriter out,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6,
        Object[] more)
    {
        F(out, control, 0, a1, a2, a3, a4, a5, a6, more);
    }

    /**
        Format a message into a PrintWriter.

        @param out a PrintWriter to format the output into
        @param control a String defining the format of the output
        @param args an array containing all arguments to the format
        string <b>control</b>
        @aribaapi documented
    */
    public static void F (PrintWriter out, String control, Object[] args)
    {
        F(out, control, 6, Null, Null, Null, Null, Null, Null, args);
    }

    private static void F (
        PrintWriter out,
        String control,
        int firstArg,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6,
        Object[] more)
    {
        FormatBuffer buf = getBuffer();
        B(buf, control, firstArg, a1, a2, a3, a4, a5, a6, more);
        buf.print(out);
        freeBuffer(buf);
    }

        // ----------------------------------------------------------------
        // Format into an OutputStream


    /**
        Format a message into an OutputStream.

        @param out an OutputStream to format the output into
        @param control a String defining the format of the output

        @exception IOException if there is a problem writing to the
        OutputStream.
        @aribaapi documented
    */
    public static void O (OutputStream out, String control)
      throws IOException
    {
        O(out, control, 0, Null, Null, Null, Null, Null, Null, NoMore);
    }

    /**
        Format a message into an OutputStream.

        @param out an OutputStream to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>

        @exception IOException if there is a problem writing to the
        OutputStream.
        @aribaapi documented
    */
    public static void O (OutputStream out, String control, Object a1)
      throws IOException
    {
        O(out, control, 0, a1, Null, Null, Null, Null, Null, NoMore);
    }

    /**
        Format a message into an OutputStream.

        @param out an OutputStream to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>

        @exception IOException if there is a problem writing to the
        OutputStream.
        @aribaapi documented
    */
    public static void O (
        OutputStream out,
        String control,
        Object a1,
        Object a2)
      throws IOException
    {
        O(out, control, 0, a1, a2, Null, Null, Null, Null, NoMore);
    }

    /**
        Format a message into an OutputStream.

        @param out an OutputStream to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>

        @exception IOException if there is a problem writing to the
        OutputStream.
        @aribaapi documented
    */
    public static void O (
        OutputStream out,
        String control,
        Object a1,
        Object a2,
        Object a3)
      throws IOException
    {
        O(out, control, 0, a1, a2, a3, Null, Null, Null, NoMore);
    }

    /**
        Format a message into an OutputStream.

        @param out an OutputStream to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>

        @exception IOException if there is a problem writing to the
        OutputStream.
        @aribaapi documented
    */
    public static void O (
        OutputStream out,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4)
      throws IOException
    {
        O(out, control, 0, a1, a2, a3, a4, Null, Null, NoMore);
    }

    /**
        Format a message into an OutputStream.

        @param out an OutputStream to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>

        @exception IOException if there is a problem writing to the
        OutputStream.
        @aribaapi documented
    */
    public static void O (
        OutputStream out,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5)
      throws IOException
    {
        O(out, control, 0, a1, a2, a3, a4, a5, Null, NoMore);
    }

    /**
        Format a message into an OutputStream.

        @param out an OutputStream to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>
        @param a6 the sixth argument to the format string
        <b>control</b>

        @exception IOException if there is a problem writing to the
        OutputStream.
        @aribaapi documented
    */
    public static void O (
        OutputStream out,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6)
      throws IOException
    {
        O(out, control, 0, a1, a2, a3, a4, a5, a6, NoMore);
    }

    /**
        Format a message into an OutputStream.

        @param out an OutputStream to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>
        @param a6 the sixth argument to the format string
        <b>control</b>
        @param more an array containing all subsequent arguments to
        the format string <b>control</b> after the sixth one.

        @exception IOException if there is a problem writing to the
        OutputStream.
        @deprecated use O(OutputStream, String, Object[])
        @aribaapi documented
    */
    public static void O (
        OutputStream out,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6,
        Object[] more)
      throws IOException
    {
        O(out, control, 0, a1, a2, a3, a4, a5, a6, more);
    }

    /**
        Format a message into an OutputStream.

        @param out an OutputStream to format the output into
        @param control a String defining the format of the output
        @param args an array containing all arguments to
        the format string <b>control</b>

        @exception IOException if there is a problem writing to the
        OutputStream.
        @aribaapi documented
    */
    public static void O (OutputStream out, String control, Object[] args)
      throws IOException
    {
        O(out, control, 6, Null, Null, Null, Null, Null, Null, args);
    }

    private static void O (
        OutputStream out,
        String control,
        int firstArg,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6,
        Object[] more)
      throws IOException
    {
        FormatBuffer buf = getBuffer();
        B(buf, control, firstArg, a1, a2, a3, a4, a5, a6, more);
        buf.print(out);
        freeBuffer(buf);
    }

    // ----------------------------------------------------------------
    // Format into a Writer

    /**
        Formats a message onto a <code>Writer</code>.

        @param out a Writer to format the output into
        @param control a String defining the format of the output

        @exception IOException if there is a problem writing to the
        Writer.
        @aribaapi documented
    */
    public static void W (Writer out, String control)
      throws IOException
    {
        W(out, control, 0, Null, Null, Null, Null, Null, Null, NoMore);
    }

    /**
        Formats a message onto a <code>Writer</code>.

        @param out a Writer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>

        @exception IOException if there is a problem writing to the
        Writer.
        @aribaapi documented
    */
    public static void W (Writer out, String control, Object a1)
      throws IOException
    {
        W(out, control, 0, a1, Null, Null, Null, Null, Null, NoMore);
    }

    /**
        Formats a message onto a <code>Writer</code>.

        @param out a Writer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>

        @exception IOException if there is a problem writing to the
        Writer.
        @aribaapi documented
    */
    public static void W (
        Writer out,
        String control,
        Object a1,
        Object a2)
      throws IOException
    {
        W(out, control, 0, a1, a2, Null, Null, Null, Null, NoMore);
    }

    /**
        Formats a message onto a <code>Writer</code>.

        @param out a Writer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>

        @exception IOException if there is a problem writing to the
        Writer.
        @aribaapi documented
    */
    public static void W (
        Writer out,
        String control,
        Object a1,
        Object a2,
        Object a3)
      throws IOException
    {
        W(out, control, 0, a1, a2, a3, Null, Null, Null, NoMore);
    }

    /**
        Formats a message onto a <code>Writer</code>.

        @param out a Writer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>

        @exception IOException if there is a problem writing to the
        Writer.
        @aribaapi documented
    */
    public static void W (
        Writer out,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4)
      throws IOException
    {
        W(out, control, 0, a1, a2, a3, a4, Null, Null, NoMore);
    }

    /**
        Formats a message onto a <code>Writer</code>.

        @param out a Writer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>

        @exception IOException if there is a problem writing to the
        Writer.
        @aribaapi documented
    */
    public static void W (
        Writer out,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5)
      throws IOException
    {
        W(out, control, 0, a1, a2, a3, a4, a5, Null, NoMore);
    }

    /**
        Formats a message onto a <code>Writer</code>.

        @param out a Writer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>
        @param a6 the sixth argument to the format string
        <b>control</b>

        @exception IOException if there is a problem writing to the
        Writer.
        @aribaapi documented
    */
    public static void W (
        Writer out,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6)
      throws IOException
    {
        W(out, control, 0, a1, a2, a3, a4, a5, a6, NoMore);
    }

    /**
        Formats a message onto a <code>Writer</code>.

        @param out a Writer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>
        @param a6 the sixth argument to the format string
        <b>control</b>
        @param more an array containing all subsequent arguments to
        the format string <b>control</b> after the sixth one.

        @exception IOException if there is a problem writing to the
        Writer.
        @deprecated use W(Writer, String, Object[])
        @aribaapi documented
    */
    public static void W (
        Writer out,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6,
        Object[] more)
      throws IOException
    {
        W(out, control, 0, a1, a2, a3, a4, a5, a6, more);
    }

    /**
        Formats a message onto a <code>Writer</code>.

        @param out a Writer to format the output into
        @param control a String defining the format of the output
        @param args an array containing all arguments to
        the format string <b>control</b>

        @exception IOException if there is a problem writing to the
        Writer.
        @aribaapi documented
    */
    public static void W (Writer out, String control, Object[] args)
      throws IOException
    {
        W(out, control, 6, Null, Null, Null, Null, Null, Null, args);
    }

    private static void W (
        Writer out,
        String control,
        int firstArg,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6,
        Object[] more)
      throws IOException
    {
        FormatBuffer buf = getBuffer();
        B(buf, control, firstArg, a1, a2, a3, a4, a5, a6, more);
        buf.print(out);
        freeBuffer(buf);
    }

        // ----------------------------------------------------------------
        // Format into a given buffer

    /**
        Format a message into a FormatBuffer.

        @param buf a FormatBuffer to format the output into
        @param control a String defining the format of the output
        @aribaapi documented
    */
    public static void B (FormatBuffer buf, String control)
    {
        B(buf, control, 0, Null, Null, Null, Null, Null, Null, NoMore);
    }

    /**
        Format a message into a FormatBuffer.

        @param buf a FormatBuffer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @aribaapi documented
    */
    public static void B (FormatBuffer buf, String control, Object a1)
    {
        B(buf, control, 0, a1, Null, Null, Null, Null, Null, NoMore);
    }

    /**
        Format a message into a FormatBuffer.

        @param buf a FormatBuffer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @aribaapi documented
    */
    public static void B (
        FormatBuffer buf,
        String control,
        Object a1,
        Object a2)
    {
        B(buf, control, 0, a1, a2, Null, Null, Null, Null, NoMore);
    }

    /**
        Format a message into a FormatBuffer.

        @param buf a FormatBuffer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @aribaapi documented
    */
    public static void B (
        FormatBuffer buf,
        String control,
        Object a1,
        Object a2,
        Object a3)
    {
        B(buf, control, 0, a1, a2, a3, Null, Null, Null, NoMore);
    }

    /**
        Format a message into a FormatBuffer.

        @param buf a FormatBuffer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @aribaapi documented
    */
    public static void B (
        FormatBuffer buf,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4)
    {
        B(buf, control, 0, a1, a2, a3, a4, Null, Null, NoMore);
    }

    /**
        Format a message into a FormatBuffer.

        @param buf a FormatBuffer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>
        @aribaapi documented
    */
    public static void B (
        FormatBuffer buf,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5)
    {
        B(buf, control, 0, a1, a2, a3, a4, a5, Null, NoMore);
    }

    /**
        Format a message into a FormatBuffer.

        @param buf a FormatBuffer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>
        @param a6 the sixth argument to the format string
        <b>control</b>
        @aribaapi documented
    */
    public static void B (
        FormatBuffer buf,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6)
    {
        B(buf, control, 0, a1, a2, a3, a4, a5, a6, NoMore);
    }

    /**
        Format a message into a FormatBuffer.

        @param buf a FormatBuffer to format the output into
        @param control a String defining the format of the output
        @param a1 the first argument to the format string
        <b>control</b>
        @param a2 the second argument to the format string
        <b>control</b>
        @param a3 the third argument to the format string
        <b>control</b>
        @param a4 the fourth argument to the format string
        <b>control</b>
        @param a5 the fifth argument to the format string
        <b>control</b>
        @param a6 the sixth argument to the format string
        <b>control</b>
        @param more an array containing all subsequent arguments to
        the format string <b>control</b> after the sixth one.
        @deprecated use B(FormatBuffer, control, Object[])
        @aribaapi documented
    */
    public static void B (
        FormatBuffer buf,
        String control,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6,
        Object[] more)
    {
        B(buf, control, 0, a1, a2, a3, a4, a5, a6, more);
    }

    /**
        Format a message into a FormatBuffer.

        @param buf a FormatBuffer to format the output into
        @param control a String defining the format of the output
        @param args an array containing all arguments to the format
        string <b>control</b> after the sixth one
        @aribaapi documented
    */
    public static void B (FormatBuffer buf, String control, Object[] args)
    {
        B(buf, control, 6, Null, Null, Null, Null, Null, Null, args);
    }

        // ----------------------------------------------------------------
        // The master formatter.

    private static void B (FormatBuffer buf, String control, int whichArg,
                           Object a1, Object a2, Object a3, Object a4,
                           Object a5, Object a6,
                           Object[] more)
    {
        int controlLength = control.length();
        int controlPos = 0;

        while (controlPos < controlLength) {
            char c = control.charAt(controlPos++);

                // Copy simple character in control string to output
            if (c != '%') {
                buf.append(c);
            }

                // Parse a printing directive of the form "%[-][min][.[max]]s"
            else {
                int state = StartST;
                int nextStateAfterCollectNumber = -1;

                    // Set to true if the field spec starts with '-'
                boolean isLeftJustified = false;

                    // Set to true if the control string has a zero
                    // directly following the '-' or '+' in the field spec.
                boolean padWithZeros = false;

                    // The minimum field width.
                int pre = -1;

                    // The maximum number of characters to print.
                int post = -1;

                    // Used by the state machine to collect
                    // the numbers in the field spec.
                int accumulator = 0;

            DoOneControlLoop:
                while (true) {

                        // Get the next character
                    if (controlPos < controlLength) {
                        c = control.charAt(controlPos);
                    }
                    else {
                        state = UnexpectedEndST;
                    }

                    switch (state) {
                      case StartST:
                        if (c == 's') {
                            state = PrintNextObjectST;
                        }
                        else if (c == '-') {
                            isLeftJustified = true;
                            state = GetPadCharST;
                            controlPos++;
                        }
                        else if (c == '+') {
                            isLeftJustified = false;
                            state = GetPadCharST;
                            controlPos++;
                        }
                        else if (c == '0') {
                            state = GetPadCharST;
                        }
                        else if (c == '%') {
                            controlPos++;
                            buf.append(c);
                            break DoOneControlLoop;
                        }
                        else if (c == '/') {
                            controlPos++;
                            buf.append(File.separator);
                            break DoOneControlLoop;
                        }
                        else {
                            state = GetPreNumberST;
                        }
                        break;

                      case GetPadCharST:
                        if (c == '0') {
                            padWithZeros = true;
                            controlPos++;
                        }
                        state = GetPreNumberST;
                        break;

                      case GetPreNumberST:
                        if (c >= '0' && c <= '9') {
                            accumulator = c - '0';
                            state = CollectNumberST;
                            nextStateAfterCollectNumber = DonePreST;
                            controlPos++;
                        }
                        else {
                            state = ConsumeDotST;
                        }
                        break;

                      case DonePreST:
                        pre = accumulator;
                        state = ConsumeDotST;
                        break;

                      case GetPostNumberST:
                        if (c >= '0' && c <= '9') {
                            accumulator = c - '0';
                            state = CollectNumberST;
                            nextStateAfterCollectNumber = DonePostST;
                            controlPos++;
                        }
                        else {
                            state = GetFinalCharacterST;
                        }
                        break;

                      case DonePostST:
                        state = GetFinalCharacterST;
                        post = accumulator;
                        break;

                      case CollectNumberST:
                        if (c >= '0' && c <= '9') {
                            accumulator = (accumulator * 10) + (c - '0');
                            controlPos++;
                        }
                        else {
                            state = nextStateAfterCollectNumber;
                        }
                        break;

                      case ConsumeDotST:
                        if (c == '.') {
                            state = GetPostNumberST;
                            controlPos++;
                        }
                        else {
                            state = GetFinalCharacterST;
                        }
                        break;

                      case GetFinalCharacterST:
                        controlPos++;

                            // print object using column controls
                        if (c == 's') {
                            Object arg;
                            switch (whichArg++) {
                              case 0: arg = a1; break;
                              case 1: arg = a2; break;
                              case 2: arg = a3; break;
                              case 3: arg = a4; break;
                              case 4: arg = a5; break;
                              case 5: arg = a6; break;
                              default:
                                int i = whichArg - 7;
                                if (i >= more.length) {
                                    arg = "(too many controls)";
                                }
                                else {
                                    arg = more[i];
                                }
                            }

                            String argAsString = String.valueOf(arg);
                            int len = argAsString.length();

                                // Set min and max to sensible values.
                            if (post == -1) {
                                post = Math.max(len, pre);
                            }
                            if (pre == -1) {
                                pre = 0;
                            }

                                // Determine ammount of pad
                            int pad = 0;
                            if (len < post && len < pre) {
                                pad = pre - len;
                            }

                                // Write left side padding.
                            if (! isLeftJustified) {
                                for (int i = 0; i < pad; i++) {
                                    buf.append(padWithZeros ? '0' : ' ');
                                }
                            }

                                // Write subject, truncating if necessary.
                            if (len <= post) {
                                buf.append(argAsString);
                            }
                            else {
                                int startPosition = isLeftJustified ? 0 : len - post;
                                for (int j = 0; j < post; j++) {
                                    char subject = argAsString.charAt(startPosition + j);
                                    buf.append(subject);
                                }
                            }

                                // Write right-side padding.
                            if (isLeftJustified) {
                                for (int i = 0; i < pad; i++) {
                                    buf.append(padWithZeros ? '0' : ' ');
                                }
                            }
                        }

                            // Not a recognized printing control code.
                        else {
                            buf.append("(Fmt: unknown control character)");
                        }

                        break DoOneControlLoop;

                            // Print the next argument
                      case PrintNextObjectST:
                        controlPos++;
                        Object arg;
                        switch (whichArg++) {
                          case 0: arg = a1; break;
                          case 1: arg = a2; break;
                          case 2: arg = a3; break;
                          case 3: arg = a4; break;
                          case 4: arg = a5; break;
                          case 5: arg = a6; break;
                          default:
                            int i = whichArg - 7;
                            if (i >= more.length) {
                                arg = "(too many controls)";
                            }
                            else {
                                arg = more[i];
                            }
                        }

                            // Handle char arrays so they print their contents.
                        if (arg instanceof char[]) {
                            buf.append((char[])arg);
                        }
                        else if (arg instanceof Map || arg instanceof List) {
                            buf.append(FormattingSerializer.serializeObject(arg));
                        }
                        else {
                            buf.append(arg == null? "null" : arg.toString());
                        }

                        break DoOneControlLoop;

                            // Reached end of control string too soon.
                      case UnexpectedEndST:
                        buf.append("(Fmt: unexpected end of control sequence)");
                        break DoOneControlLoop;

                    }
                }
            }
        }
    }

    /*-----------------------------------------------------------------------
        I18N Capable Methods
      -----------------------------------------------------------------------*/

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table and does a
        java.text.MessageFormat style position independent formatting.
        <p>
        Since using MessageFormat without any substitute arguments is
        not useful, this method is deprecated and 
        ResourceService.getString(String, String) should be used instead.
        <p>       
        Because there are no substitution values in the string, translators
        may not be aware that they should use MessageFormat conventions and
        they may or may not do so.  This could cause lose of single-quotes
        because a single-quote is an escape character to MessageFormat.   
        This method tries to detect strings that are in MessageFormat 
        conventions.  If a "''" (two single-quotes) or a "{" are present,
        the string is format with MessageFormat.   Otherwise, the string
        is returned without formatting.

        @param stringTable resource string table
        @param key resource key
        @return pattern with formatted objects.
        @deprecated use ariba.util.core.ResourceService.getString(String, String)
        @aribaapi documented
    */
    public static String Sil (String stringTable, String key)
    {
        // This method is moduled after Sil (String stringTable, String key, Object[] args)
        // with some changes because of the special checks for "''" and "{'
        String control = ResourceService.getString(stringTable, key);

            // This is for the case that the string could not be printed
            // into a locale.
        if (control.equals(key)) {
            return notFoundError(control, new Object[0]);
        }
        
        if (control.indexOf("''") > -1 ||
            control.indexOf("{") > -1) {
                // String is in MessageFormat style, do formatting.
                return Si(control, new Object[0]);
            }
        // String is not in MessageFormat style, so return resource string.
        return control;
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table and does a
        java.text.MessageFormat style position independent formatting.

        @param stringTable resource string table
        @param key resource key
        @param a1 object to be substituted
        @return pattern with formatted objects.
        @aribaapi documented
    */
    public static String Sil (String stringTable, String key, Object a1)
    {
        return Sil(stringTable, key, a1, null, null, null, null, null, null);
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table and does a
        java.text.MessageFormat style position independent formatting.

        @param stringTable resource string table
        @param key resource key
        @param a1 object to be substituted
        @param a2 object to be substituted
        @return pattern with formatted objects.
        @aribaapi documented
    */
    public static String Sil (
        String stringTable,
        String key,
        Object a1,
        Object a2)
    {
        return Sil(stringTable, key, a1, a2, null, null, null, null, null);
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table and does a
        java.text.MessageFormat style position independent formatting.

        @param stringTable resource string table
        @param key resource key
        @param a1 object to be substituted
        @param a2 object to be substituted
        @param a3 object to be substituted
        @return pattern with formatted objects.
        @aribaapi documented
    */
    public static String Sil (
        String stringTable,
        String key,
        Object a1,
        Object a2,
        Object a3)
    {
        return Sil(stringTable, key, a1, a2, a3, null, null, null, null);
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table and does a
        java.text.MessageFormat style position independent formatting.

        @param stringTable resource string table
        @param key resource key
        @param a1 object to be substituted
        @param a2 object to be substituted
        @param a3 object to be substituted
        @param a4 object to be substituted
        @return pattern with formatted objects.
        @aribaapi documented
    */
    public static String Sil (
        String stringTable,
        String key,
        Object a1,
        Object a2,
        Object a3,
        Object a4)
    {
        return Sil(stringTable, key, a1, a2, a3, a4, null, null, null);
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table and does a
        java.text.MessageFormat style position independent formatting.

        @param stringTable resource string table
        @param key resource key
        @param a1 object to be substituted
        @param a2 object to be substituted
        @param a3 object to be substituted
        @param a4 object to be substituted
        @param a5 object to be substituted
        @return pattern with formatted objects.
        @aribaapi documented
    */
    public static String Sil (
        String stringTable,
        String key,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5)
    {
        return Sil(stringTable, key, a1, a2, a3, a4, a5, null, null);
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table and does a
        java.text.MessageFormat style position independent formatting.

        @param stringTable resource string table
        @param key resource key
        @param a1 object to be substituted
        @param a2 object to be substituted
        @param a3 object to be substituted
        @param a4 object to be substituted
        @param a5 object to be substituted
        @param a6 object to be substituted
        @return pattern with formatted objects.
        @aribaapi documented
    */
    public static String Sil (
        String stringTable,
        String key,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6)
    {
        return Sil(stringTable, key, a1, a2, a3, a4, a5, a6, null);
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table and does a
        java.text.MessageFormat style position independent formatting.

        @param stringTable resource string table
        @param key resource key
        @param a1 object to be substituted
        @param a2 object to be substituted
        @param a3 object to be substituted
        @param a4 object to be substituted
        @param a5 object to be substituted
        @param a6 object to be substituted
        @param more object array for more arguments
        @return pattern with formatted objects.
        @deprecated use Sil(String, String, Object[])
        @aribaapi documented
    */
    public static String Sil (
        String stringTable,
        String key,
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6,
        Object[] more)
    {
        Object[] args = argsArray(a1, a2, a3, a4, a5, a6, more);
        return Sil(stringTable, key, args);
    }

    private static Object[] argsArray (
        Object a1,
        Object a2,
        Object a3,
        Object a4,
        Object a5,
        Object a6,
        Object[] more)
    {
        Object[] args;
            //number of other args = 6
        if (more != null && more.length > 0) {
            args = new Object[6 + more.length];
            System.arraycopy(more, 0, args, 6, more.length);
        }
        else {
            args = new Object[6];
        }
        args[0] = a1;
        args[1] = a2;
        args[2] = a3;
        args[3] = a4;
        args[4] = a5;
        args[5] = a6;
        return args;
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table and does a
        java.text.MessageFormat style position independent formatting.

        @param stringTable resource string table
        @param key resource key
        @param vectorArgs object arguments to be substituted
        @return pattern with formatted objects.
        @aribaapi documented
    */
    public static String Sil (String stringTable, String key, List vectorArgs)
    {
        Object [] args = vectorArgs.toArray();
        return Sil(stringTable, key, args);
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table and does a
        java.text.MessageFormat style position independent formatting.

        @param stringTable resource string table
        @param key resource key, cannot be <b>null</b>
        @param args object arguments to be substituted
        @return pattern with formatted objects.
        @see #Si(String, Object[])
        @aribaapi documented
    */
    public static String Sil (String stringTable, String key, Object[] args)
    {
        String control = ResourceService.getString(stringTable, key);

            // This is for the case that the string could not be printed
            // into a locale.
        if (control.equals(key)) {
            return notFoundError(control, args);
        }

        return Si(control, args);
    }
    
    private static String notFoundError (String control, Object[] args)
    {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("Format string for message id ");
        buf.append(control);
        buf.append(" not found.");
        for (int i = 0; i < args.length; i++) {
            if (i == 0) {
                buf.append("  Arguments to be formatted are");
            }
            else {
                buf.append(",");
            }
            buf.append(" ");
            buf.append(args[i]);
        }
        return buf.toString();
    }

    /*-----------------------------------------------------------------------

        I18N Capable Methods that fetch the resource string for a
        specific locale
      -----------------------------------------------------------------------*/

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table for the specified
        locale and does a java.text.MessageFormat style position
        independent formatting.

        @param locale locale to be used
        @param stringTable resource string table
        @param key resource key
        @return pattern with formatted objects.
        @aribaapi documented
    */
    public static String Sil (
        Locale locale,
        String stringTable,
        String key)
    {
        return Sil(locale, stringTable, key, null, null, null, null, null, null, null);
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table for the specified
        locale and does a java.text.MessageFormat style position
        independent formatting.

        @param locale locale to be used
        @param stringTable resource string table
        @param key resource key
        @param a1 object to be substituted
        @return pattern with formatted objects.
        @aribaapi documented
    */
    public static String Sil (
        Locale locale,
        String stringTable,
        String key,
        Object a1)
    {
        return Sil(locale, stringTable, key, a1, null, null, null, null, null, null);
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table for the specified
        locale and does a java.text.MessageFormat style position
        independent formatting.

        @param locale locale to be used
        @param stringTable resource string table
        @param key resource key
        @param a1 object to be substituted
        @param a2 object to be substituted
        @return pattern with formatted objects.
        @aribaapi documented
    */
    public static String Sil (
        Locale locale,
        String stringTable,
        String key,
        Object a1,
        Object a2)
    {
        return Sil(locale, stringTable, key, a1, a2, null, null, null, null, null);
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table for the specified
        locale and does a java.text.MessageFormat style position
        independent formatting.

        @param locale locale to be used
        @param stringTable resource string table
        @param key resource key
        @param a1 object to be substituted
        @param a2 object to be substituted
        @param a3 object to be substituted
        @return pattern with formatted objects.
        @aribaapi documented
    */
    public static String Sil (
        Locale locale,
        String stringTable,
        String key,
        Object a1,
        Object a2,
        Object a3)
    {
        return Sil(locale, stringTable, key, a1, a2, a3, null, null, null, null);
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table for the specified
        locale and does a java.text.MessageFormat style position
        independent formatting.

        @param locale locale to be used
        @param stringTable resource string table
        @param key resource key
        @param a1 object to be substituted
        @param a2 object to be substituted
        @param a3 object to be substituted
        @param a4 object to be substituted
        @return pattern with formatted objects.
        @aribaapi documented
    */
    public static String Sil (
        Locale locale,
        String stringTable,
        String key,
        Object a1,
        Object a2,
        Object a3,
        Object a4)
    {
        return Sil(locale, stringTable, key, a1, a2, a3, a4, null, null, null);
    }

    /**
        An internationalized version of Fmt.S that gets the control
        string from the specified string table for the specified
        locale and does a java.text.MessageFormat style position
        independent formatting.

        @param locale locale to be used
        @param stringTable resource string table
        @param key resource key
        @param a1 object to be
