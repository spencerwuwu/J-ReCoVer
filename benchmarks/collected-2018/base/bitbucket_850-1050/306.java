// https://searchcode.com/api/result/76392127/

package org.apache.zookeeper.inspector.ui.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.zookeeper.inspector.logger.LoggerFactory;

public class FileUtils {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger();

    /**
     * Several arrays of illegal characters on various operating systems. Used
     * by convertFileName
     */
    private static final char[] ILLEGAL_CHARS_ANY_OS = { '/', '\n', '\r', '\t', '\0', '\f' };

    private static final char[] ILLEGAL_CHARS_UNIX = { '`' };

    private static final char[] ILLEGAL_CHARS_WINDOWS = { '?', '*', '\\', '<', '>', '|', '\"', ':' };

    private static final char[] ILLEGAL_CHARS_MACOS = { ':' };

    public static String getCanonicalPath(File f) throws IOException {
        try {
            return f.getCanonicalPath();
        } catch (IOException ioe) {
            String msg = ioe.getMessage();
            // windows bugs out :(
            if (OSUtils.isWindows() && msg != null && msg.indexOf("There are no more files") != -1)
                return f.getAbsolutePath();
            else
                throw ioe;
        }
    }

    /**
     * Returns the filename without an extension.
     */
    public static String getFilenameNoExtension(String fullname) {
        int i = fullname.lastIndexOf(".");
        if (i < 0) {
            return fullname;
        } else {
            return fullname.substring(0, i);
        }
    }

    /**
     * Utility method that returns the file extension of the given file.
     * 
     * @param f
     *            the <tt>File</tt> instance from which the extension should be
     *            extracted
     * @return the file extension string, or <tt>empty string</tt> if the
     *         extension could not be extracted
     */
    public static String getFileExtension(File f) {
        String name = f.getName();
        return getFileExtension(name);
    }

    /**
     * Utility method that returns the file extension of the given file.
     * 
     * @param name
     *            the file name <tt>String</tt> from which the extension should
     *            be extracted
     * @return the file extension string, or <tt>empty string</tt> if the
     *         extension could not be extracted
     */
    public static String getFileExtension(String name) {
        int index = name.lastIndexOf(".");
        if (index == -1)
            return "";

        // the file must have a name other than the extension
        if (index == 0)
            return "";

        // if the last character of the string is the ".", then there's
        // no extension
        if (index == (name.length() - 1))
            return "";

        return name.substring(index + 1).intern();
    }

//    /**
//     * Forcibly renames a file.
//     * 
//     * @return true if the rename succeeded
//     */
//    public static boolean forceRename(File src, File dst) {
//        // First attempt to rename it.
//        boolean success = src.renameTo(dst);
//
//        // If that didn't work, try copying the file.
//        if (!success) {
//            success = copy(src, dst);
//            // if copying succeeded, get rid of the original
//            // at this point any active uploads will have been killed
//            if (success)
//                src.delete();
//        }
//
//        return success;
//    }

    /**
     * Forcibly deletes a file
     * 
     * @return true if the deletion succeeded
     */
    public static boolean forceDelete(File file) {
        // First attempt to rename it.
        boolean success = file.delete();

//        if (LOG.isDebugEnabled()) {
//            LOG.debugf("success= {0}, file.exists()? {1}", success, file.exists());
//        }
        return !file.exists();
    }

    /**
     * Reads a file, filling a byte array.
     */
    public static byte[] readFileFully(File source) {
        DataInputStream raf = null;
        int length = (int) source.length();
        if (length <= 0)
            return null;

        byte[] data = new byte[length];
        try {
            raf = new DataInputStream(new BufferedInputStream(new FileInputStream(source)));
            raf.readFully(data);
        } catch (IOException ioe) {
            return null;
        } finally {
        	try {
        		if(raf != null) {
        			raf.close();
        		}
        	} catch(IOException e) {
        	}
        }

        return data;
    }

    /**
     * @param directory
     *            gets all files under this directory RECURSIVELY.
     * @param filter
     *            if null, then returns all files. Else, only returns files
     *            extensions in the filter array.
     * @return an array of Files recursively obtained from the directory,
     *         according to the filter.
     * 
     */
    public static File[] getFilesRecursive(File directory, String... filter) {
        List<File> dirs = new ArrayList<File>();
        // the return array of files...
        List<File> retFileArray = new ArrayList<File>();
        File[] retArray = new File[0];

        // bootstrap the process
        if (directory.exists() && directory.isDirectory())
            dirs.add(directory);

        // while i have dirs to process
        while (dirs.size() > 0) {
            File currDir = dirs.remove(0);
            String[] listedFiles = currDir.list();
            for (int i = 0; (listedFiles != null) && (i < listedFiles.length); i++) {
                File currFile = new File(currDir, listedFiles[i]);
                if (currFile.isDirectory()) // to be dealt with later
                    dirs.add(currFile);
                else if (currFile.isFile()) { // we have a 'file'....
                    boolean shouldAdd = false;
                    if (filter == null || filter.length == 0)
                        shouldAdd = true;
                    else {
                        String ext = FileUtils.getFileExtension(currFile);
                        for (int j = 0; (j < filter.length) && (!ext.isEmpty()); j++) {
                            if (ext.equalsIgnoreCase(filter[j])) {
                                shouldAdd = true;

                                // don't keep looping through all filters --
                                // one match is good enough
                                break;
                            }
                        }
                    }
                    if (shouldAdd)
                        retFileArray.add(currFile);
                }
            }
        }

        if (!retFileArray.isEmpty()) {
            retArray = new File[retFileArray.size()];
            for (int i = 0; i < retArray.length; i++)
                retArray[i] = retFileArray.get(i);
        }

        return retArray;
    }

//    /**
//     * Deletes the given file or directory, moving it to the trash can or
//     * recycle bin if the platform has one and <code>moveToTrash</code> is true.
//     * 
//     * @param file
//     *            the file or directory to trash or delete
//     * @param moveToTrash
//     *            whether the file should be moved to the trash bin or
//     *            permanently deleted
//     * @return true on success
//     * 
//     * @throws IllegalArgumentException
//     *             if the OS does not support moving files to a trash bin, check
//     *             with {@link OSUtils#supportsTrash()}.
//     */
//    public static boolean delete(File file, boolean moveToTrash) {
//        if (!file.exists()) {
//            return false;
//        }
//        if (moveToTrash) {
//            if (OSUtils.isMacOSX()) {
//                return moveToTrashOSX(file);
//            } else if (OSUtils.isWindows()) {
//                return SystemUtils.recycle(file);
//            } else {
//                throw new IllegalArgumentException("OS does not support trash");
//            }
//        } else {
//            return deleteRecursive(file);
//        }
//    }

//    /**
//     * Moves the given file or directory to Trash.
//     * 
//     * @param file
//     *            the file or directory to move to Trash
//     * @throws IOException
//     *             if the canonical path cannot be resolved or if the move
//     *             process is interrupted
//     * @return true on success
//     */
//    private static boolean moveToTrashOSX(File file) {
//        try {
//            String[] command = moveToTrashCommand(file);
//            ProcessBuilder builder = new ProcessBuilder(command);
//            builder.redirectErrorStream();
//            Process process = builder.start();
//            ProcessUtils.consumeAllInput(process);
//            process.waitFor();
//        } catch (InterruptedException err) {
//            LOG.error("InterruptedException", err);
//        } catch (IOException err) {
//            LOG.error("IOException", err);
//        }
//        return !file.exists();
//    }

    /**
     * Creates and returns the the <code>osascript</code> command to move a file
     * or directory to the Trash
     * 
     * @param file
     *            the file or directory to move to Trash
     * @throws IOException
     *             if the canonical path cannot be resolved
     * @return OSAScript command
     */
    private static String[] moveToTrashCommand(File file) {
        String path = null;
        try {
            path = file.getCanonicalPath();
        } catch (IOException err) {
            LOG.error("IOException", err);
            path = file.getAbsolutePath();
        }

        String fileOrFolder = (file.isFile() ? "file" : "folder");

        String[] command = new String[] { "osascript", "-e", "set unixPath to \"" + path + "\"", "-e",
                "set hfsPath to POSIX file unixPath", "-e", "tell application \"Finder\"", "-e",
                "if " + fileOrFolder + " hfsPath exists then", "-e", "move " + fileOrFolder + " hfsPath to trash",
                "-e", "end if", "-e", "end tell" };

        return command;
    }

    /**
     * Deletes all files in 'directory'. Returns true if this successfully
     * deleted every file recursively, including itself.
     * 
     * @return
     */
    public static boolean deleteRecursive(File directory) {
        // make sure we only delete canonical children of the parent file we
        // wish to delete. I have a hunch this might be an issue on OSX and
        // Linux under certain circumstances.
        // If anyone can test whether this really happens (possibly related to
        // symlinks), I would much appreciate it.
        String canonicalParent;
        try {
            canonicalParent = getCanonicalPath(directory);
        } catch (IOException ioe) {
            return false;
        }

        if (!directory.isDirectory())
            return directory.delete();

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    if (!getCanonicalPath(file).startsWith(canonicalParent))
                        continue;
                } catch (IOException ioe) {
                    return false;
                }

                if (!deleteRecursive(file))
                    return false;
            }
        }

        return directory.delete();
    }

    /**
     * A utility method to flush Flushable objects (Readers, Writers, Input- and
     * OutputStreams and RandomAccessFiles).
     */
    public static void flush(Flushable flushable) {
        if (flushable != null) {
            try {
                flushable.flush();
            } catch (IOException ignored) {
            }
        }
    }

//    /**
//     * Attempts to copy the first 'amount' bytes of file 'src' to 'dst',
//     * returning the number of bytes actually copied. If 'dst' already exists,
//     * the copy may or may not succeed.
//     * 
//     * @param src
//     *            the source file to copy
//     * @param amount
//     *            the amount of src to copy, in bytes
//     * @param dst
//     *            the place to copy the file
//     * @return the number of bytes actually copied. Returns 'amount' if the
//     *         entire requested range was copied.
//     */
//    public static long copy(File src, long amount, File dst) {
//        final int BUFFER_SIZE = 1024;
//        long amountToRead = amount;
//        InputStream in = null;
//        OutputStream out = null;
//        try {
//            // I'm not sure whether buffering is needed here. It can't hurt.
//            in = new BufferedInputStream(new FileInputStream(src));
//            out = new BufferedOutputStream(new FileOutputStream(dst));
//            byte[] buf = new byte[BUFFER_SIZE];
//            while (amountToRead > 0) {
//                int read = in.read(buf, 0, (int) Math.min(BUFFER_SIZE, amountToRead));
//                if (read == -1)
//                    break;
//                amountToRead -= read;
//                out.write(buf, 0, read);
//            }
//        } catch (IOException ignore) {
//            LOG.error(ignore.getMessage(), ignore);
//        } finally {
//            IOUtils.close(in);
//            flush(out);
//            IOUtils.close(out);
//        }
//        return amount - amountToRead;
//    }
//
//    /**
//     * Copies the file 'src' to 'dst', returning true iff the copy succeeded. If
//     * 'dst' already exists, the copy may or may not succeed. May also fail for
//     * VERY large source files.
//     */
//    public static boolean copy(File src, File dst) {
//        long length = src.length();
//        return copy(src, length, dst) == length;
//    }

    /**
     * Creates a temporary file using
     * {@link File#createTempFile(String, String, File)}, trying a few times.
     * This is a workaround for Sun Bug: 6325169: createTempFile occasionally
     * fails (throwing an IOException).
     */
    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        IOException iox = null;

        for (int i = 0; i < 10; i++) {
            try {
                return File.createTempFile(prefix, suffix, directory);
            } catch (IOException x) {
                iox = x;
            }
        }

        throw iox;
    }

    /**
     * Creates a temporary file using
     * {@link File#createTempFile(String, String)}, trying a few times. This is
     * a workaround for Sun Bug: 6325169: createTempFile occasionally fails
     * (throwing an IOException).
     */
    public static File createTempFile(String prefix, String suffix) throws IOException {
        IOException iox = null;

        for (int i = 0; i < 10; i++) {
            try {
                return File.createTempFile(prefix, suffix);
            } catch (IOException x) {
                iox = x;
            }
        }

        throw iox;
    }

    /**
     * Utility method to generate an MD5 hash from a target file.
     */
    public static String getMD5(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest m = MessageDigest.getInstance("MD5");
        ByteBuffer byteBuffer = ByteBuffer.allocate(16 * 1024);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            FileChannel fileChannel = fileInputStream.getChannel();
            while (fileChannel.read(byteBuffer) != -1) {
                byteBuffer.flip();
                m.update(byteBuffer);
                byteBuffer.clear();
            }

        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
        byte[] digest = m.digest();
        String md5 = new BigInteger(1, digest).toString(16);
        return md5;
    }

    /**
     * Utility method to generate an MD5 hash from an InputStream.
     */
    public static String getMD5(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest m = MessageDigest.getInstance("MD5");
        byte[] bytes = new byte[16 * 1024];
        int length = inputStream.read(bytes);
        while (length != -1) {
            m.update(bytes, 0, length);
            length = inputStream.read(bytes);
        }

        byte[] digest = m.digest();
        String md5 = new BigInteger(1, digest).toString(16);
        return md5;
    }

    /**
     * Tries to create a symbolic link from the source to the target
     * destinations. Returning the exit code of the command run.
     * 
     * @throws UnsupportedOperationException
     *             if this method is run on an os other than linux.
     */
    public static int createSymbolicLink(File source, File target) throws IOException, InterruptedException {
        if (!OSUtils.isLinux()) {
            throw new UnsupportedOperationException("Creating Symbolic links is only supported on linux.");
        }
        String[] command = { "ln", "-s", source.getAbsolutePath(), target.getAbsolutePath() };
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        return process.exitValue();
    }

    /**
     * Deletes all files in 'directory'. Returns true if this successfully
     * deleted every file recursively, including itself.
     * 
     * This takes deletion 1 step further than the standard deleteRecursive in
     * that it uses forceDelete on each file to clean up any locks on files that
     * exist.
     */
    public static boolean forceDeleteRecursive(File directory) {
        // make sure we only delete canonical children of the parent file we
        // wish to delete. I have a hunch this might be an issue on OSX and
        // Linux under certain circumstances.
        // If anyone can test whether this really happens (possibly related to
        // symlinks), I would much appreciate it.
        String canonicalParent;
        try {
            canonicalParent = getCanonicalPath(directory);
        } catch (IOException ioe) {
            return false;
        }

        if (!directory.isDirectory()) {
            return forceDelete(directory);
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    if (!getCanonicalPath(file).startsWith(canonicalParent))
                        continue;
                } catch (IOException ioe) {
                    return false;
                }

                if (!forceDeleteRecursive(file))
                    return false;
            }
        }

        return forceDelete(directory);
    }

    /**
     * Saves all data from the stream into the destination file. This will
     * always overwrite the file.
     */
    public static void saveStream(InputStream inStream, File newFile) throws IOException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            // buffer the streams to improve I/O performance
            final int bufferSize = 2048;
            bis = new BufferedInputStream(inStream, bufferSize);
            bos = new BufferedOutputStream(new FileOutputStream(newFile), bufferSize);
            byte[] buffer = new byte[bufferSize];
            int c = 0;

            do { // read and write in chunks of buffer size until EOF reached
                c = bis.read(buffer, 0, bufferSize);
                if (c > 0)
                    bos.write(buffer, 0, c);
            } while (c == bufferSize); // (# of bytes read)c will = bufferSize
            // until EOF

            bos.flush();
        } catch (IOException e) {
            // if there is any error, delete any portion of file that did write
            newFile.delete();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ignored) {
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Recursively deletes any empty directories. Returns true if the given
     * directory was empty or only had empty subdirectories, all of which were
     * deleted.
     */
    public static boolean deleteEmptyDirectories(File directory) {
        if (directory.isDirectory()) {
            boolean empty = true;
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    empty &= file.isDirectory() && deleteEmptyDirectories(file);
                }
            }
            if (empty) {
                directory.delete();
            }
            return empty;
        }
        return false;
    }

    public static File createTempDirectory(String prefix) {
        File directory = new File(new File(System.getProperty("java.io.tmpdir")), (prefix + UUID.randomUUID()
                .toString()));
        directory.mkdirs();
        // TODO check and throw errors if directory cannot be made
        return directory;
    }

    /**
     * Sanitizes a folder name. Folder names can contain illegal characters that
     * are valid within a filename.
     * http://msdn.microsoft.com/en-us/library/aa365247(VS.85).aspx
     * 
     * @param name
     *            String to sanitize
     * @return sanitized String
     * @throws IOException
     * @throws IOException
     *             if no valid characters remain within this file name.
     */
    public static String sanitizeFolderName(String name) throws IOException {
        String result = santizeFileName(name).trim();
        int index = result.length();

        while (index > 0 && result.charAt(index - 1) == '.') {
            index -= 1;
        }

        if (index <= 0) {
            throw new IOException("folder does not contain valid characters");
        }

        if (index == result.length()) {
            return result;
        } else {
            return result.substring(0, index);
        }
    }

    /**
     * Sanitizes a String for use in a directory and file name and removes any
     * illegal characters from it.
     * 
     * @param name
     *            String to check
     * @return sanitized String
     */
    public static String santizeFileName(String name) {
        for (char aILLEGAL_CHARS_ANY_OS : ILLEGAL_CHARS_ANY_OS) {
            name = name.replace(aILLEGAL_CHARS_ANY_OS, '_');
        }

        if (OSUtils.isWindows() || OSUtils.isOS2()) {
            for (char aILLEGAL_CHARS_WINDOWS : ILLEGAL_CHARS_WINDOWS) {
                name = name.replace(aILLEGAL_CHARS_WINDOWS, '_');
            }
        } else if (OSUtils.isLinux() || OSUtils.isSolaris()) {
            for (char aILLEGAL_CHARS_UNIX : ILLEGAL_CHARS_UNIX) {
                name = name.replace(aILLEGAL_CHARS_UNIX, '_');
            }
        } else if (OSUtils.isMacOSX()) {
            for (char aILLEGAL_CHARS_MACOS : ILLEGAL_CHARS_MACOS) {
                name = name.replace(aILLEGAL_CHARS_MACOS, '_');
            }
        }
        return name;
    }

    public static File getTempFile(File saveFile) {
        return new File(saveFile.getAbsolutePath() + ".tmp");
    }
    
//    /**
//     * Returns a normalized and shortened valid file name taking the length of
//     * the path of the parent directory into account.
//     * <p>
//     * The name is cleared from illegal file system characters and it is ensured
//     * that the maximum path system on the system is not exceeded unless the
//     * parent directory path has already the maximum path length.
//     * 
//     * @throws IOException if the parent directory's path takes up
//     *         {@link OSUtils#getMaxPathLength()}.
//     */
//    public static String convertFileName(File parentDir, String name) throws IOException {
//        int parentLength = getPathBytes(parentDir);
//        if (parentLength >= OSUtils.getMaxPathLength() - 1 /*
//                                                            * for the separator
//                                                            * char
//                                                            */) {
//            throw new IOException("Path too long");
//        }
//        return convertFileName(name, Math.min(OSUtils.getMaxPathLength() - parentLength - 1, 180));
//    }
    
    /**
     * @return the number of bytes in the directory path
     */
    public static int getPathBytes(File dir) throws IOException{
        return FileUtils.getCanonicalFile(dir).getAbsolutePath().getBytes(Charset.defaultCharset().name()).length;
    }
    

    /** Same as f.getCanonicalFile() in JDK1.3. */
    public static File getCanonicalFile(File f) throws IOException {
        try {
            return f.getCanonicalFile();
        } catch (IOException ioe) {
            String msg = ioe.getMessage();
            // windows bugs out :(
            if (OSUtils.isWindows() && msg != null && msg.indexOf("There are no more files") != -1)
                return f.getAbsoluteFile();
            else
                throw ioe;
        }
    }

//    /**
//     * Cleans up the filename and truncates it to length of 180 bytes by calling
//     * {@link #convertFileName(String, int) convertFileName(String, 180)}.
//     */
//    public static String convertFileName(String name) {
//        return convertFileName(name, 180);
//    }
//
//    /**
//     * Cleans up the filename from illegal characters and truncates it to the
//     * length of bytes specified.
//     * 
//     * @param name the filename to clean up
//     * @param maxBytes the maximum number of bytes the cleaned up file name can
//     *        take up
//     * @return the cleaned up file name
//     */
//    public static String convertFileName(String name, int maxBytes) {
//        // use default encoding which is also used for files judging from the
//        // property name "file.encoding"
//        try {
//            return convertFileName(name, maxBytes, Charset.defaultCharset());
//        } catch (CharacterCodingException cce) {
//            try {
//                // UTF-8 should always be available
//                return convertFileName(name, maxBytes, Charset.forName("UTF-8"));
//            } catch (CharacterCodingException e) {
//                // should not happen, UTF-8 can encode unicode and gives us a
//                // good length estimate
//                throw new RuntimeException("UTF-8 should have encoded: " + name, e);
//            }
//        }
//    }

//    /**
//     * Replaces OS specific illegal characters from any filename with '_',
//     * including ( / \n \r \t ) on all operating systems, ( ? * \ < > | " ) on
//     * Windows, ( ` ) on Unix.
//     * 
//     * @param name the filename to check for illegal characters
//     * @param maxBytes the maximum number of bytes for the resulting file name,
//     *        must be > 0
//     * @return String containing the cleaned filename
//     * 
//     * @throws CharacterCodingException if the charset could not encode the
//     *         characters in <code>name</code>
//     * @throws IllegalArgumentException if maxBytes <= 0
//     */
//    public static String convertFileName(String name, int maxBytes, Charset charSet)
//            throws CharacterCodingException {
//
//        if (maxBytes <= 0) {
//            throw new IllegalArgumentException("maxBytes must be > 0");
//        }
//
//        // ensure that block-characters aren't in the filename.
//        name = I18NConvert.instance().compose(name);
//
//        // if the name is too long, reduce it. We don't go all the way
//        // up to 255 because we don't know how long the directory name is;
//        // We want to keep the extension, though.
//        if (name.length() > maxBytes || name.getBytes().length > maxBytes) {
//            int extStart = name.lastIndexOf('.');
//            if (extStart == -1) { // no extension, weird, but possible
//                name = getPrefixWithMaxBytes(name, maxBytes, charSet);
//            } else {
//                // if extension is greater than 11, we truncate it.
//                // ( 11 = '.' + 10 extension bytes )
//                int extLength = name.length() - extStart;
//                int extEnd = extLength > 11 ? extStart + 11 : name.length();
//                byte[] extension = getMaxBytes(name.substring(extStart, extEnd), 16, charSet);
//                try {
//                    // disregard extension if we lose too much of the name
//                    // since the name is also used for searching
//                    if (extension.length >= maxBytes - 10) {
//                        name = getPrefixWithMaxBytes(name, maxBytes, charSet);
//                    } else {
//                        name = getPrefixWithMaxBytes(name, maxBytes - extension.length, charSet)
//                                + new String(extension, charSet.name());
//                    }
//                } catch (UnsupportedEncodingException uee) {
//                    throw new RuntimeException("Could not handle string", uee);
//                }
//            }
//        }
//        for (char aILLEGAL_CHARS_ANY_OS : ILLEGAL_CHARS_ANY_OS) {
//            name = name.replace(aILLEGAL_CHARS_ANY_OS, '_');
//        }
//
//        if (OSUtils.isWindows() || OSUtils.isOS2()) {
//            for (char aILLEGAL_CHARS_WINDOWS : ILLEGAL_CHARS_WINDOWS) {
//                name = name.replace(aILLEGAL_CHARS_WINDOWS, '_');
//            }
//        } else if (OSUtils.isLinux() || OSUtils.isSolaris()) {
//            for (char aILLEGAL_CHARS_UNIX : ILLEGAL_CHARS_UNIX) {
//                name = name.replace(aILLEGAL_CHARS_UNIX, '_');
//            }
//        } else if (OSUtils.isMacOSX()) {
//            for (char aILLEGAL_CHARS_MACOS : ILLEGAL_CHARS_MACOS) {
//                name = name.replace(aILLEGAL_CHARS_MACOS, '_');
//            }
//        }
//
//        return name;
//    }
    
    /**
     * Returns the prefix of <code>string</code> which takes up a maximum of
     * <code>maxBytes</code>.
     * 
     * @throws CharacterCodingException
     */
    static String getPrefixWithMaxBytes(String string, int maxBytes, Charset charSet)
            throws CharacterCodingException {
        try {
            return new String(getMaxBytes(string, maxBytes, charSet), charSet.name());
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Could not recreate string", uee);
        }
    }

    /**
     * Returns the first <code>maxBytes</code> of <code>string</code> encoded
     * using the encoder of <code>charSet</code>
     * 
     * @param string whose prefix bytes to return
     * @param maxBytes the maximum number of bytes to return
     * @param charSet the char set used for encoding the characters into bytes
     * @return the array of bytes of length <= maxBytes
     * @throws CharacterCodingException if the char set's encoder could not
     *         handle the characters in the string
     */
    static byte[] getMaxBytes(String string, int maxBytes, Charset charSet)
            throws CharacterCodingException {
        byte[] bytes = new byte[maxBytes];
        ByteBuffer out = ByteBuffer.wrap(bytes);
        CharBuffer in = CharBuffer.wrap(string.toCharArray());
        CharsetEncoder encoder = charSet.newEncoder();
        CoderResult cr = encoder.encode(in, out, true);
        encoder.flush(out);
        if (cr.isError()) {
            cr.throwException();
        }
        byte[] result = new byte[out.position()];
        System.arraycopy(bytes, 0, result, 0, result.length);
        return result;
    }
}

