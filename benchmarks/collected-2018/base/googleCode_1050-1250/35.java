// https://searchcode.com/api/result/13849458/

package com.systemsplanet.maven.plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.google.api.translate.Language;
import com.google.api.translate.Translate;
import com.systemsplanet.util.OrderedProperties;

/**
 * generate Language files
 *
 * @goal localize
 *
 * @phase generate-sources
 */
public class LocalizeMojo extends AbstractMojo {

   /**
    * List of source translation files.
    * If this property is configured in maven, the following properties are ignored, and automatically deducted for each translation file:
    * <ul>
    * <li>sourceTranslationFile</li>
    * <li>sourceTranslationPath</li>
    * <li>languageFilePattern</li>
    * <li>destinationPath</li>
    *
    * The smartSyncChangeFile is appended with the name of each sourceTranslationFile.
    * This allowes for batch processing multiple translation files.
    *
    * @parameter
    *
    * @author E.Blanksma
    * @since 1.4
    *
    */
   private String[] sourceTranslationFiles;

   /**
    * Maximum number of calls to google before pauzing.
    * Google will block the translation service if too many calls are made from on source within a short period.
    * To prevent this, the plugin will pause for "pauzeSeconds" after " maxCalls".
    *
    * @parameter expression="1500"
    *
    * @author E.Blanksma
    * @since 1.4
    */
   private int maxCalls;

   /**
    * Pause in seconds after maxCalls have been made
    *
    * @parameter expression="180"
    * @author E.Blanksma
    * @since 1.4
    */
   private int pauseSeconds;

    /**
     * Debug mode
     *
     * @parameter expression="false"
     *
     */
    private Boolean debug;

    /**
     * Progress percentage console output
     *
     * @parameter default-value="false"
     *
     */
    private Boolean showProgress;

    /**
     * English translation source path
     *
     * @parameter expression="${basedir}/src/main/resources/"
     *
     * @author Robert Savage
     * @since  1.3
     *
     */
    private File sourceTranslationPath;

    /**
     * (Optional)
     * English Translation source file
     * <p>
     *  This property is used to explicitly override
     *  the default translation file determined using
     *  the 'languageFilePattern' and 'sourceLanguage'
     *  properties.  This is useful if for example
     *  you want to read the base language neutral
     *  properties file.
     *
     * @parameter
     *
     * @author Robert Savage
     * @since  1.3
     *
     */
    private String sourceTranslationFile;

    /**
     * English translation source file pattern
     *
     * @parameter expression="Language_{0}.properties"
     *
     */
    private String languageFilePattern;

    /**
     * English translation source file patterns
     *
     * @parameter
     *
     */
    private String[] languageFilePatterns;

    /**
     * English translation source language
     *
     * @parameter expression="en"
     *
     */
    private String sourceLanguage;

    /**
     * Destination directory for output files
     *
     * @parameter expression="${project.build.directory}/classes/"
     */
    private File destinationPath;

    /**
     * Destination directory for output files
     *
     * @parameter
     */
    private String[] destinationPaths;

    /*
     * ARABIC = "ar"; CHINESE = "zh"; CHINESE_SIMPLIFIED = "zh-CN";
     * CHINESE_TRADITIONAL = "zh-TW"; DUTCH = "nl"; FRENCH = "fr"; GERMAN =
     * "de"; GREEK = "el"; ITALIAN = "it"; JAPANESE = "ja"; KOREAN = "ko";
     * PORTUGESE = "pt"; RUSSIAN = "ru"; SPANISH = "es";
     */

    /**
     * list of languages
     *
     * @parameter expression="es,fr,nl,de,it"
     */
    private String targetLanguages;

    /**
     * (Optional)
     * Encoding of source file(s).
     * Defaults to destinationFileEncoding.
     *
     * @parameter
     */
    private String sourceFileEncoding;

    /**
     * (Optional)
     * Target generated property file encoding
     * <p>
     * Please note that the default Java Properties
     * reader/writer only supports '8859_1' encoding
     * by default.  There are work-arounds and other
     * external tools such as the Spring Framework
     * that can read properties files encoded in
     * alternate formats such as UTF-8 and UTF-16.
     * <p>
     * Note: as of Java 1.6 there are overloaded
     * methods for load() and store() that accept
     * a Reader/Writer argument.  Using this you
     * can create a FileInputStream and
     * InputStreamReader using an alternate encoding.
     * <p>
     * Supports: ASCII
     *           Cp1252
     *           ISO8859_1, ISO-8859_1
     *           UTF8, UTF-8
     *           UTF-16
     *           UTF-16BE, UnicodeBigUnmarked
     *           UTF-16LE, UnicodeLittleUnmarked
     *           UnicodeBig
     *           UnicodeLittle
     *
     * @see http://java.sun.com/j2se/1.3/docs/guide/intl/encoding.doc.html
     * @parameter expression="ISO8859_1"
     *
     * @author Robert Savage
     * @since  1.3
     *
     */
    private String destinationFileEncoding;

    /**
     * (Optional)
     * Target generated property file encoding byte-order-marker
     * <p>
     * If specified, this option will forcefully include the
     * BOM (byte-order-market) bytes on the generated property
     * files.
     * <p>
     * ( this is really only useful when needing to
     *   include the BOM bytes for UTF-8 encoding )
     * <p>
     * Supports: UTF8, UTF-8
     *           UTF-16BE, UnicodeBigUnmarked
     *           UTF-16LE, UnicodeLittleUnmarked
     *
     * @parameter default-value="false"
     *
     * @author Robert Savage
     * @since  1.3
     *
     */
    private Boolean destinationFileEncodingIncludeBOM;

    /**
     * (Optional)
     * Array of properties to exclude from translation
     * <p>
     * This parameter defines a list of resource
     * property names in the source file that you wish
     * to exclude from the generated property files.
     * Regular expressions can be used.
     * <p>
     * FORMAT:
     *   <excludeProperties>
     *      <param>Property.Name.One</param>
     *      <param>Property.Name.Two</param>
     *      <param>Property.Name.Three</param>
     *      <param>Property.Name.*</param>
     *      <param>.*\.Name.Four.*</param>
     *    </excludeProperties>
     *
     * @parameter
     *
     * @author Robert Savage, Erik-Jan Blanksma
     * @since  1.3
     *
     */
    private String[] excludeProperties;

    /**
     * (Optional)
     * Array of properties that will not be translated.
     * <p>
     * This parameter defines a list of resource
     * property names in the source file that you wish
     * to include in the generated property files but do
     * not wish to translate.  The untranslated source
     * property value will be written to each generated
     * output file.
     * Regular expressions can be used.
     * <p>
     * FORMAT:
     *   <excludeProperties>
     *      <param>Property.Name.Five</param>
     *      <param>Property.Name.Six</param>
     *      <param>Property.Name\..*</param>
     *    </excludeProperties>
     *
     * @parameter
     *
     * @author Robert Savage, Erik-Jan Blanksma
     * @since  1.3
     *
     */
    private String[] passThruProperties;


    /**
     * (Optional)
     * Include a data header in the generated files (destination, sync)
     * <p>
     * Setting this to 'false' ensures your generated files are unchanged, if nothing functionally changes.
     *
     * @parameter default-value="true"
     *
     * @author Robert Savage
     * @since  1.3
     *
     */
    private boolean includeDateHeader;

    /**
     * (Optional)
     * Translation target destination file header comment
     * <p>
     * This comment is prepended to each generated resource
     * properties file.
     *
     * @parameter default-value="auto-generated using 'google-api-translate-java-maven-plugin'"
     *
     * @author Robert Savage
     * @since  1.3
     *
     */
    private String commentHeader;


    /**
     * (Optional)
     * If 'smartSync' is enabled then only detected changes
     * in the source file will be processed for each
     * destination file.  This option can significantly
     * reduce the amount of processing time since only detected
     * changes are translated.  If 'smartSync' is disabled,
     * then all destination property values are re-translated
     * each time this tool is executed.
     * <p>
     * If an existing destination files exists, then it
     * will be analyzed to determine which if any property
     * values are missing or have changed.  Only these
     * missing and changed property values are translated,
     * existing unchanged property values will remain
     * untouched and any source properties that have been
     * removed will also be removed from the destination files.
     * <p>
     * NOTE:  This option is used in conjunction with the
     *        'smartSyncChangeFile' configuration parameter.
     *        If the 'smartSyncChangeFile' parameter is not
     *        defined, then only property values added to and
     *        removed from the source file will be propagated
     *        to the destination files.  The change file defined
     *        by 'smartSyncChangeFile' is used to detect value
     *        changes for all existing source properties.
     * <p>
     *
     * @parameter default-value="false"
     *
     * @author Robert Savage
     * @since  1.3
     *
     */
    private Boolean smartSync;


    /**
     * (Optional)
     * Source change file.  If this parameter is defined,
     * a source change file will be created in the
     * destination path.  This file can be used for
     * comparison of source properties to determine which
     * source properties have been modified in subsequent
     * executions of the translation generation.
     * <p>
     * This comment is prepended to each generated resource
     * properties file.
     *
     * @parameter
     *
     * @author Robert Savage
     * @since  1.3
     *
     */
    private String smartSyncChangeFile;

    /**
     * (Optional)
     * If this option is enabled, then this mojo will create
     * the empty shell resource files for each translation
     * language defined.  These shell files will include the 
     * property keys but will contain no translated values,
     * all the value will be empty.  This feature can be used
     * when you are ready to generate empty shell files to
     * distribute to a translation service/company.    
     * <p>
     *
     * @parameter
     *
     * @author Robert Savage
     * @since  1.6
     *
     */
    private boolean createEmptyFiles;

    

    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException, MojoFailureException {
       EventProcessor.maxCalls= maxCalls;
       EventProcessor.pauseMs = 1000* pauseSeconds;
       OrderedProperties.setIncludeDateHeader(includeDateHeader);
       if (sourceFileEncoding==null || sourceFileEncoding.trim().length()==0){
          sourceFileEncoding=destinationFileEncoding;
       }

       if (sourceTranslationFiles==null || sourceTranslationFiles.length==0){
          executeFileTranslation(sourceTranslationFile, sourceTranslationPath, languageFilePattern, destinationPath, smartSyncChangeFile);
       }else{
          if (debug) {
             System.out.println("processing files: "+sourceTranslationFiles);
          }
          for (int i=0; i<sourceTranslationFiles.length;i++) {
             String sourceFile=sourceTranslationFiles[i];
             File file= new File(sourceFile);
            if (file.exists() && file.isFile()){
               //establish smartsync file name
               String smartSyncChangeFileName = getSmartSyncFileName(file);

               //establish language file pattern
               String destinationFilePattern = getDestinationFilePattern(i, file);

               //establish destination path
               File destinationPathFile = getDestinationFilePath(i, file);

               executeFileTranslation(file.getName(), file.getParentFile(), destinationFilePattern, destinationPathFile, smartSyncChangeFileName);
            }
         }
       }

    }

   private File getDestinationFilePath(int i, File file) {
      File destinationPathFile = null;
      if(destinationPaths!=null && i < destinationPaths.length){
         String destinationPathString=destinationPaths[i];
         destinationPathFile = new File(destinationPathString);
      }else if (destinationPath!=null){
         destinationPathFile = destinationPath;
      }else{
         destinationPathFile = file.getParentFile();
      }
      return destinationPathFile;
   }

   private String getDestinationFilePattern(int i, File file) {
      String destinationFilePattern=null;

      if(languageFilePatterns!=null && i < languageFilePatterns.length){
         destinationFilePattern=languageFilePatterns[i];
      }else if (languageFilePattern!=null && !languageFilePattern.isEmpty()){
         destinationFilePattern=languageFilePattern;
      }else{
         if (file.getName().indexOf("_")>-1){
            destinationFilePattern=file.getName().substring(0,file.getName().indexOf("_"))+"_{0}.properties";
         }else{
            destinationFilePattern=file.getName().replaceFirst(".properties", "_{0}.properties");
         }
      }
      return destinationFilePattern;
   }

   private String getSmartSyncFileName(File file) {
      String smartSyncChangeFileName=null;
      if (smartSyncChangeFile!=null && !smartSyncChangeFile.isEmpty()) {
         smartSyncChangeFileName = smartSyncChangeFile + "_"
               + file.getName();
      }
      if (debug) {
         System.out.println("smart-sync file: "+smartSyncChangeFileName);
      }
      return smartSyncChangeFileName;
   }

    @SuppressWarnings("unchecked")
    public void executeFileTranslation(String sourceTranslationFileString, File sourceTranslationPath, String languageFilePattern, File destinationPath, String smartSyncChangeFile) throws MojoExecutionException, MojoFailureException {
       String sourceTranslationFileName;
        Object[] arguments = { sourceLanguage };

        // <REV 1.3; RSavage> added support for overriding parameter to
        //                    specify explicit target file name
        if(sourceTranslationFileString == null || sourceTranslationFileString.isEmpty())
        {
            // construct the target filename from the file pattern and source language
            sourceTranslationFileName = MessageFormat.format(
                    sourceTranslationPath + File.separator + languageFilePattern,
                    arguments);
        }
        else
        {
            // use explicit target filename dictated by plugin parameter
            sourceTranslationFileName = MessageFormat.format(
                    sourceTranslationPath + File.separator + sourceTranslationFileString,
                    arguments);
        }

        File sourceTranslationFile = new File(sourceTranslationFileName);

        if (debug) {
            System.out.println("sourceTranslationPath: ["
                    + sourceTranslationPath + "]");
            System.out.println("languageFilePattern: [" + languageFilePattern
                    + "]");
            System.out.println("sourceLanguage: [" + sourceLanguage + "]");
            System.out.println("sourceTranslationFileName: ["
                    + sourceTranslationFileName + "]");
            System.out.println("sourceTranslationFile ["
                    + sourceTranslationFile.getAbsolutePath() + "]");
            System.out.println("destinationPath ["
                    + destinationPath.getAbsolutePath() + "]");
        }

        // ensure the destination path exists
        if (!destinationPath.exists())
            destinationPath.mkdirs();


        Reader sourceReader = null;
        InputStream fis = null;
        BufferedWriter bw = null;
        try {
            String[] languages = targetLanguages.split(",");

            // <REV 1.3; RSavage> changed to use 'OrderedProperties' class to
            //                    read source property file.  This class will
            //                    maintain the order of the properties as they
            //                    are defined in the file.
            OrderedProperties sourceProperties = new OrderedProperties();
            OrderedProperties sourceChangeProperties = new OrderedProperties();

            sourceReader = new InputStreamReader(new FileInputStream(sourceTranslationFile.getAbsolutePath()),sourceFileEncoding);
            sourceProperties.load(sourceReader);
            sourceReader.close();
            sourceReader = null;

            // <REV 1.3; RSavage> added support for 'smart sync' feature using
            //                    a source change file for comparison to
            //                    determine if any source properties values
            //                    have changed since the last execution cycle.
            //                    Detected changes will be marked for translation.
            if(smartSync)
            {
                if (debug) {
                    System.out.println("SMART-SYNC is enabled");
                }

                // attempt to load smart-sync change file
                if(smartSyncChangeFile != null && !smartSyncChangeFile.isEmpty())
                {
                    // copy current source file to the source change file
                    String sourceChangeFileName = destinationPath + File.separator + smartSyncChangeFile;
                    File sourceChangeFile = new File(sourceChangeFileName);

                    // only load if the file exists
                    if(sourceChangeFile.exists())
                    {
                        fis = new FileInputStream(sourceChangeFile.getAbsolutePath());
                        sourceChangeProperties.load(fis);
                        fis.close();
                        fis = null;

                        if (debug) {
                            System.out.println("SMART-SYNC CHANGE FILE LOADED: " + sourceChangeFileName);
                        }

                        // now loop over all source properties
                        Set<String> keys = sourceProperties.propertyNames();
                        for(String key : keys) {

                            // if there is no change between the current source
                            // file and the source change file then remove it
                            // from the change collection.
                            if(sourceProperties.getProperty(key).equals(sourceChangeProperties.getProperty(key)))
                            {
                                sourceChangeProperties.remove(key);

                                if (debug) {
                                    System.out.println("SMART-SYNC EXCLUDING UNCHANGED PROPERTY: " + key);
                                }
                            }
                        }
                    }else{
                       if (debug) {
                          System.out.println("CREATING SMART-SYNC file: " + sourceChangeFileName);
                       }
                       sourceChangeFile.getParentFile().mkdirs();
                       sourceChangeFile.createNewFile();
                    }
                }
            }

            int languageIndex = 0;
            int propertyIndex = 0;


            for (String destLanguage : languages) {

                // increment language counter
                languageIndex++;
                
                boolean emptyLanguageFile = (destLanguage.startsWith("{") && destLanguage.endsWith("}"));

                Object[] args = { destLanguage };
                String destTranslationFileName = MessageFormat.format(
                        destinationPath + File.separator + languageFilePattern,
                        args);
                //System.out.print(destLanguage + " ");
                if (debug) {
                    System.out.println("destTranslationFileName:"
                            + destTranslationFileName);
                }

                // <REV 1.3; RSavage> added support for 'smart sync' only generating missing
                //                    and changed properties values for each destination file.
                //                    This means we need to load the existing destination file
                //                    to determine which if any properties are missing or are
                //                    no longer included in the source properties file.
                OrderedProperties outputPropertiesLoaded = null;
                if(smartSync)
                {
                    File outputFile = new File(destTranslationFileName);
                    if(outputFile.exists())
                    {
                        outputPropertiesLoaded = new OrderedProperties();
                        FileInputStream fisOutputFile = new FileInputStream(destTranslationFileName);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(fisOutputFile, destinationFileEncoding));
                        outputPropertiesLoaded.load(reader);

                        // close the file
                        reader.close();
                        fisOutputFile.close();
                        reader = null;
                        fisOutputFile = null;

                        if (debug) {
                            System.out.println("SMART-SYNC DESTINATION FILE LOADED: " + destTranslationFileName);
                        }
                    }
                }


                // create file stream
                FileOutputStream fos = new FileOutputStream(destTranslationFileName);

                // <REV 1.3; RSavage> added support for plugin parameter defining
                //                    a specific file encoding for generated
                //                    property files.
                if(destinationFileEncoding == null || destinationFileEncoding.isEmpty())
                {
                    // create file writer;
                    // do not include specified encoding - use default JVM encoding
                    bw = new BufferedWriter(new OutputStreamWriter(fos));
                }
                else
                {
                    // <REV 1.3; RSavage> added support for plugin parameter defining
                    //                    a file encoding BOM inclusion for generated
                    //                    property files.
                    if(destinationFileEncodingIncludeBOM)
                    {
                        // lookup BOM byte signature based on the file encoding
                        byte[] bom = getEncodingBOM(destinationFileEncoding);
                        if(bom.length > 0)
                        {
                            // write BOM bytes
                            fos.write(bom);
                        }
                    }

                    // create file writer;
                    // include plugin parameter specified encoding
                    bw = new BufferedWriter(new OutputStreamWriter(fos, destinationFileEncoding));
                }

                // create new output properties object
                OrderedProperties outputProperties = new OrderedProperties();

                // get a collection of all the source property names
                Set<String> keys = sourceProperties.propertyNames();

                // <REV 1.3; RSavage> calculate and display status &
                //                    percentage complete
                int propertyCount = keys.size();
                int languageCount = languages.length;
                int propertyTranslationsCount = propertyCount * languageCount;

                try {
                    for(String msgKey : keys) {

                        // increment property counter
                        propertyIndex++;

                        // <REV 1.3; RSavage> calculate and display status &
                        //                    percentage complete
                        int percentComplete = (propertyIndex * 100) / propertyTranslationsCount;

                        String msgVal = sourceProperties.getProperty(msgKey);
                        if (debug) {
                            System.out.println(msgKey + "=[" + sourceLanguage
                                    + "]" + msgVal);
                        } else {

                            if(showProgress){
                                System.out.print("\r[ " + percentComplete + "% ] ("+sourceTranslationFile.getName()+", language " + languageIndex +
                                        " of " + languageCount + ") : (translation " + propertyIndex +
                                        " of " + propertyTranslationsCount + ") [" +
                                        destLanguage + "]    ");

                            }
                            else
                            {
                                System.out.print(".");
                            }
                        }

                        if (debug){

                            if(showProgress){
                                System.out.println("[ " + percentComplete + "% ] (language " + languageIndex +
                                                   " of " + languageCount + ") : (translation " + propertyIndex +
                                                   " of " + propertyTranslationsCount + ") [" +
                                                   destLanguage + "]    ");
                            }

                            System.out.print(msgKey + "=[" + destLanguage + "]");
                        }

                        // <REV 1.3; RSavage> added support for excluding specific
                        //                    named properties from the generated
                        //                    property files.
                        if(isExcludedProperty(msgKey))
                        {
                            if(debug){
                                System.out.println(" ** EXCLUDED **");
                            }
                        }


                        // <REV 1.3; RSavage> added support for pass-thru named properties
                        //                    that are included in the the generated output
                        //                    property files but are not translated.
                        else if(isPassThruProperty(msgKey))
                        {
                            outputProperties.setProperty(msgKey, sourceProperties.getProperty(msgKey));
                        }

                        // <REV 1.6; RSavage> added support for creating empty shell files
                        //                    that include the resource keys but no values
                        else if(createEmptyFiles || emptyLanguageFile)
                        {
                            outputProperties.setProperty(msgKey, "");
                        }

                        // <REV 1.3; RSavage> added support for 'smart sync' feature only
                        //                    performing translations for missing and changed
                        //                    property values.
                        else if(smartSync &&
                                outputPropertiesLoaded != null &&
                                outputPropertiesLoaded.containsKey(msgKey) &&
                                (!sourceChangeProperties.containsKey(msgKey)))
                        {
                            outputProperties.setProperty(msgKey, outputPropertiesLoaded.getProperty(msgKey));
                            if(debug){
                                System.out.println(" ** TRANSLATED VALUE ALREADY EXISTS **");
                            }
                        }
                        else
                        {
                            // perform translation
                            EventProcessor e = new EventProcessor(debug, sourceLanguage, destLanguage, msgKey, outputProperties);
                            parse(new ByteArrayInputStream(msgVal.getBytes()), e);
                        }
                    }

                    // <REV 1.3; RSavage> write output properties file;
                    //                    using an instance of the 'OrderedProperties'
                    //                    class to write out the properties in the
                    //                    same order as they were defined in the
                    //                    source property file.  Also using the
                    //                    store method of the 'OrderedProperties'
                    //                    class will ensure any necessary character
                    //                    escaping is enforced.
                    //
                    //                    added support for a comment header in the
                    //                    generated property files.
                    //
                    outputProperties.store(bw,commentHeader);

                    // close the writer
                    bw.close();
                    bw = null;
                    //System.out.println();


                    // <REV 1.3; RSavage> added support for 'smart sync' feature.
                    //                    when the translation generation has been
                    //                    completed, we need to save a change file
                    //                    with the last known source property values.
                    if(smartSyncChangeFile != null && !smartSyncChangeFile.isEmpty())
                    {
                        // copy current source file to the source change file
                        String destChangeFileName = destinationPath + File.separator + smartSyncChangeFile;
                        File destChangeFile = new File(destChangeFileName);
                        sourceProperties.store(new FileWriter(destChangeFile,false),"SOURCE CHANGE FILE");
                    }

                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    throw new MojoExecutionException(e.getMessage(), e);
                } finally {
                    if (bw != null) {
                        bw.close();
                    }
                }
            }
        } catch (Exception e) {

            System.out.println(e.getMessage());

            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e1) {
                }
            }
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e1) {
                }
            }

            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Parse message, translating the text not inside html
     *
     * @param is
     * @param e
     * @throws IOException
     */
    void parse(InputStream is, EventProcessor e) throws IOException {
        StringBuffer sb = new StringBuffer();
        int ch = 0;
        while (ch != -1) {
            if (isMarkup(ch) == false) {
                ch = is.read();
            }
            if (ch == -1) {
                break;
            }
            // System.out.println("CH=" + (char) ch + " = " + ch);
            if ((char) ch == '$') {
                notifyContent(sb, e);
                ch = skipMarkup('$', ']', is, e);
                continue;
            }
            if ((char) ch == '<') {
               notifyContent(sb, e);
               ch = skipMarkup('<', '>', is, e);
               continue;
            }
            if ((char) ch == '{') {
                notifyContent(sb, e);
                ch = skipMarkup('{', '}', is, e);
                continue;
            }
            if (isContent(ch)) {
                sb.append((char) ch);
            } else {
                notifyContent(sb, e);
                ch = skipToContent(ch, is, e);
                if (ch != -1 && isMarkup(ch) == false) {
                    sb.append((char) ch);
                }
            }
        }
        notifyContent(sb, e);
    }

    void notifyContent(StringBuffer sb, EventProcessor e) throws IOException {
        if (sb.length() > 0) {
            e.foundContent(sb.toString());
            sb.setLength(0);
        }
    }

    int skipToContent(int ch, InputStream is, EventProcessor e)
            throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append((char) ch);
        while (true) {
            ch = is.read();
            if (ch == -1) {
                break;
            }
            if (isContent(ch) || isMarkup(ch)) {
                break;
            } else {
                sb.append((char) ch);
            }
        }
        e.foundMarkup(sb.toString());
        return ch;
    }

    int skipMarkup(int start, int end, InputStream is, EventProcessor e)
            throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append((char) start);
        int ch = 0;
        while (true) {
            ch = is.read();
            if (ch == -1) {
                break;
            }
            sb.append((char) ch);
            if (ch == end) {
                break;
            }
        }
        e.foundMarkup(sb.toString());
        return ch;
    }

    /**
     *  Also allow diacritics
     *  @author E.Blanksma
     */
    boolean isContent(int c) {
      if (Character.isLetterOrDigit(c))
            return true;
        if (c == ' ' || c == '\t'|| c == '\''|| c == ','|| c == '.'|| c == '?'|| c == '!')
            return true;

        return false;
   }

    boolean isMarkup(int c) {
        if (c == '{' || c == '<' || c=='$' )
            return true;
        return false;
    }

    /**
     * Get the BOM (byte-order-marking) byte signature
     * based on the provided file encoding type.
     *
     * Supports: UTF8, UTF-8
     *           UTF-16BE, UnicodeBigUnmarked
     *           UTF-16LE, UnicodeLittleUnmarked
     *
     * @param encoding
     * @return byte array of BOM signature
     * @see http://unicode.org/faq/utf_bom.html#bom1
     *
     * @author Robert Savage
     * @since  1.3
     *
     */
    byte[] getEncodingBOM(String encoding)
    {
        // modified code to
        final byte[] utf32BE_bom = { (byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF };
        final byte[] utf32LE_bom = { (byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00 };
        final byte[] utf16BE_bom = { (byte) 0xFE, (byte) 0xFF };
        final byte[] utf16LE_bom = { (byte) 0xFF, (byte) 0xFE };
        final byte[] utf8_bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        final byte[] no_bom = { };

        if(encoding.equalsIgnoreCase("utf-32be"))
        {
            return utf32BE_bom;
        }
        else if(encoding.equalsIgnoreCase("utf-32le"))
        {
            return utf32LE_bom;
        }
        else if(encoding.equalsIgnoreCase("utf-16be") || encoding.equalsIgnoreCase("unicodebigunmarked"))
        {
            return utf16BE_bom;
        }
        else if(encoding.equalsIgnoreCase("utf-16le") || encoding.equalsIgnoreCase("unicodelittleunmarked"))
        {
            return utf16LE_bom;
        }
        else if(encoding.equalsIgnoreCase("utf-8") || encoding.equalsIgnoreCase("utf8"))
        {
            return utf8_bom;
        }
        else
        {
            // other encodings either do not include a BOM
            // or the Java writer automatically includes a BOM
            // such as the case with UTF-16 and UTF-32
            return no_bom;
        }
    }


    /**
     * Determines if the property key/name provided
     * is marked as an excluded property that should
     * not be translated and not included in the
     * generated property files.
     *
     * @param property name
     * @return 'true' if the property should be excluded
     *
     * @author Robert Savage, Erik-Jan Blanksma
     * @since  1.3
     *
     */
    boolean isExcludedProperty(String key)
    {
        if(excludeProperties == null)
            return false;

        for(int index = 0; index < excludeProperties.length; index++)
        {
            if(excludeProperties[index].toString().trim().equals(key.trim()))
                return true;
            if(key.trim().matches(excludeProperties[index].toString().trim()))
               return true;

        }
        return false;
    }


    /**
     * Determines if the property key/name provided
     * is marked as a pass-thru property that should
     * not be translated but should be included in the
     * generated property files.
     *
     * @param property name
     * @return 'true' if the property should pass thru
     *
     * @author Robert Savage
     * @since  1.3
     *
     */
    boolean isPassThruProperty(String key)
    {
        if(passThruProperties == null)
            return false;

        for(int index = 0; index < passThruProperties.length; index++)
        {
            if(passThruProperties[index].toString().trim().equals(key.trim()))
                return true;
            if(key.trim().matches(passThruProperties[index].toString().trim()))
               return true;

        }
        return false;
    }
}


class EventProcessor {
    static int callsToGoogle=0;
    static int maxCalls=100;
    static int pauseMs=5000;
    Boolean debug;
    String sourceLanguage;
    String destLanguage;
    String key;
    OrderedProperties properties;

    // <REV 1.3; RSavage> modified constructor to accept property
    //                    key name string and 'OrderedProperties'
    //                    parameters instead of 'writer'
    public EventProcessor(Boolean debug, String sourceLanguage,
            String destLanguage, String key, OrderedProperties properties) {
        //Set referrer
        Translate.setHttpReferrer("http://localhost");
        this.debug = debug;
        this.sourceLanguage = sourceLanguage;
        this.destLanguage = destLanguage;
        this.key = key;
        this.properties = properties;
    }

    public void foundMarkup(String markup) throws IOException {
        // if (debug) {
        // System.out.println("markup0:[" + markup + "]");
        // }

        // <REV 1.3; RSavage> we no longer need to escape the line feed,
        //                    as the 'OrderedProperties' class should take
        //                    case of all character escaping
        //markup = markup.replaceAll("\n", "\\\\n");
        //writer.write(markup);

        // !! POSSIBLE BUG !! - now that the code has been converted over
        //                      to using the 'OrderedProperties' to write
        //                      to translated property files, we no longer
        //                      have control over managing multi-line
        //                      property values, the current implementation
        //                      just writes the entire property value to a
        //                      single line.
        //if (markup.equalsIgnoreCase("<br>")) {
        //    writer.write("\\\r\n");

        // set named property value;
        // append to any existing value in the property
        String currentValue = properties.getProperty(key,"");
        properties.setProperty(key, currentValue + markup);
    }

    public void foundContent(String content) throws IOException {
        if (debug) {
            System.out.println("\n[" + content + "]BEFORE");
        }
        String translatedText = content;
        try {
            if (content.trim().length() > 0) {
                // extract whitespace from start
                String prefix = stripFront(content);
                // extract whitespace from end
                String suffix = stripEnd(content);
                content = content.trim();
                if (++callsToGoogle % maxCalls==0){
                   System.out.print("\r pauzing "+pauseMs/1000+" seconds every "+maxCalls+" calls to google");
                   Thread.sleep(pauseMs);
                   System.out.print("\r resuming...");
                }
                translatedText = Translate.execute(content, Language.fromString(sourceLanguage), Language.fromString(destLanguage));
                translatedText = (new StringBuilder()).append(prefix).append(translatedText).append(suffix).toString();
            }
        } catch (Exception e) {
            System.out.println("ERROR src:[" + content + "] srcLang:"
                    + sourceLanguage + " dstLang:" + destLanguage + " err:"
                    + e.getMessage());
        }
        if(debug.booleanValue()) {
        	System.out.println((new StringBuilder()).append("[").append(translatedText).append("]").toString());
        }

        // <REV 1.3; RSavage> no longer are writing directly to the file
        //                    instead using the 'OrderedProperties' class
        //                    to write out the property file.
        //writer.write(translatedText);
        String currentValue = properties.getProperty(key,"");
        properties.setProperty(key, currentValue + translatedText);
    }

    // Return the whitespace from the front of a string
    private String stripFront(String s) {
        int len = s.length();
        int st = 0;
        char[] val = s.toCharArray();
        while ((st < len) && (val[st] <= ' ')) {
            st++;
        }
        return (st > 0) ? s.substring(0, st) : "";
    }

    // Return the whitespace from the end of a string
    private String stripEnd(String s) {
        int len = s.length();
        int c = 0;
        char[] val = s.toCharArray();
        while (len > 0 && val[len - 1] <= ' ') {
            len--;
            c++;
        }
        return (c > 0) ? s.substring(s.length() - c) : "";
    }
}
