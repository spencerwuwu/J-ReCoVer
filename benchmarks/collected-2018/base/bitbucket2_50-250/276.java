// https://searchcode.com/api/result/52976441/

package com.sun.lwuit.util;

import com.sun.lwuit.Form;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;


/**
 * Pluggable logging framework that allows a developer to log into storage
 * using the file connector API. It is highly recommended to use this 
 * class coupled with Netbeans preprocessing tags to reduce its overhead
 * completely in runtime.
 *
 * @author Shai Almog
 */
public class Log {
    /**
     * Constant indicating the logging level Debug is the default and the lowest level
     * followed by info, warning and error
     */
    public static final int DEBUG = 1;

    /**
     * Constant indicating the logging level Debug is the default and the lowest level
     * followed by info, warning and error
     */
    public static final int INFO = 2;

    /**
     * Constant indicating the logging level Debug is the default and the lowest level
     * followed by info, warning and error
     */
    public static final int WARNING = 3;

    /**
     * Constant indicating the logging level Debug is the default and the lowest level
     * followed by info, warning and error
     */
    public static final int ERROR = 4;
    
    
    private int level = DEBUG;
    private static Log instance = new Log();
    private long zeroTime = System.currentTimeMillis();
    private Writer output;
    private boolean fileWriteEnabled = false;
    
    /**
     * Installs a log subclass that can replace the logging destination/behavior
     * 
     * @param newInstance the new instance for the Log object
     */
    public static void install(Log newInstance) {
        instance = newInstance;
    }
    
    /**
     * Default println method invokes the print instance method, uses DEBUG level
     * 
     * @param text the text to print
     */
    public static void p(String text) {
        p(text, DEBUG);
    }
    
    /**
     * Default println method invokes the print instance method, uses given level
     * 
     * @param text the text to print
     * @param level one of DEBUG, INFO, WARNING, ERROR
     */
    public static void p(String text, int level) {
        instance.print(text, level);
    }
    
    /**
     * Default log implementation prints to the console and the file connector
     * if applicable. Also prepends the thread information and time before 
     * 
     * @param text the text to print
     * @param level one of DEBUG, INFO, WARNING, ERROR
     */
    protected void print(String text, int level) {
        if(this.level > level) {
            return;
        }
        text = getThreadAndTimeStamp() + " - " + text;
        System.out.println(text);
    }
    
    /**
     * Default method for creating the output writer into which we write, this method
     * creates a simple log file using the file connector
     */
    protected Writer createWriter() throws IOException {
        // currently return a "dummy" writer so we won't fail on device
        return new OutputStreamWriter(new ByteArrayOutputStream());
    }
    
    private Writer getWriter() throws IOException {
        if(output == null) {
            output = createWriter();
        }
        return output;
    }

    /**
     * Returns a simple string containing a timestamp and thread name.
     */
    protected String getThreadAndTimeStamp() {
        long time = System.currentTimeMillis() - zeroTime;
        long milli = time % 1000;
        time /= 1000;
        long sec = time % 60;
        time /= 60;
        long min = time % 60; 
        time /= 60;
        long hour = time % 60; 
        
        return "[" + Thread.currentThread().getName() + "] " + hour  + ":" + min + ":" + sec + "," + milli;
    }
    
    /**
     * Sets the logging level for printing log details, the lower the value 
     * the more verbose would the printouts be
     * 
     * @param level one of DEBUG, INFO, WARNING, ERROR
     */
    public static void setLevel(int level) {
        instance.level = level;
    }

    /**
     * Returns the logging level for printing log details, the lower the value 
     * the more verbose would the printouts be
     * 
     * @return one of DEBUG, INFO, WARNING, ERROR
     */
    public static int getLevel() {
        return instance.level;
    }
    
    /**
     * Places a form with the log as a TextArea on the screen, this method can
     * be attached to appear at a given time or using a fixed global key. Using
     * this method might cause a problem with further log output
     */
    public static void showLog() {
    }

    /**
     * Returns the singleton instance of the log
     *
     * @return the singleton instance of the log
     */
    public static Log getInstance() {
        return instance;
    }

    /**
     * Indicates whether GCF's file writing should be used to generate the log file
     *
     * @return the fileWriteEnabled
     */
    public boolean isFileWriteEnabled() {
        return false;
    }

    /**
     * Indicates whether GCF's file writing should be used to generate the log file
     *
     * @param fileWriteEnabled the fileWriteEnabled to set
     */
    public void setFileWriteEnabled(boolean fileWriteEnabled) {
    }

    /**
     * Indicates the URL where the log file is saved
     *
     * @return the fileURL
     */
    public String getFileURL() {
        return "";
    }

    /**
     * Indicates the URL where the log file is saved
     *
     * @param fileURL the fileURL to set
     */
    public void setFileURL(String fileURL) {
    }
}

