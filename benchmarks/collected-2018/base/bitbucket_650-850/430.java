// https://searchcode.com/api/result/127674454/

/*
 * Main.java
 *
 * Created on 3. october 2007, 20:16

 *Written by Rune Dahl Jrrgensen @ rune_dahl@hotmail.com
 *
 */

package webreader;

import com.sun.net.ssl.internal.ssl.Debug;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;


/**
 *
 * @author Rune
 */
public class Main {
    
    /** Creates a new instance of Main */
    public Main() {
    }
    
    /**
     * @param args the command line arguments
     */
    
    

    public static void main(String[] args) throws Exception {
    
        //create debug log
        File errorFile = new File("error.log");
        PrintStream PSerr = new PrintStream(errorFile);
        System.setErr(PSerr);
        //System.out.println("Note that all errors are written to error.log");
        
        
        
        //default settings
        String version = "0.7";
        boolean debugEnable = false;
        long sleepTime = 200;
        int daysToRead = 2;
        String outputFile = "tvguide.xml";
        String channelFile = "channels-tv.xml";
        String guessFileStr = "guess.txt";
        File guessFile = null;
        String tZone = "+0100";
        boolean icons = true;
        boolean useCategories = false;
        boolean useCategoryGuess = true;
        
                
        /*
        if(test){
            GetGenrer genreList = new GetGenrer(daysToRead);
            genreList.LoadCategory();
            return;
        }
         */
        
        for(int i = 0; i < args.length ; i++){
            if(args[i].equalsIgnoreCase("--days")){
                daysToRead = Integer.parseInt(args[i+1]);
                if(daysToRead>8){
                    System.out.println("Too many days. Resetting to 8.");
                    daysToRead = 8;
                }
            }
            if(args[i].equalsIgnoreCase("--debug"))
                debugEnable = true;
            if(args[i].equalsIgnoreCase("--output"))
                outputFile = args[i+1];
            if(args[i].equalsIgnoreCase("--channelfile"))
                channelFile = args[i+1];
            if(args[i].equalsIgnoreCase("--TimeZone"))
                tZone = args[i+1];
            if(args[i].equalsIgnoreCase("--noIcons"))
                icons = false;
            if(args[i].equalsIgnoreCase("--sleeptime"))
                sleepTime = Integer.parseInt(args[i+1]);
            if(args[i].equalsIgnoreCase("--DRcategory"))
                useCategories = true;
            if(args[i].equalsIgnoreCase("--noGuess"))
                useCategoryGuess = false;
            if(args[i].equalsIgnoreCase("--help")){
                System.out.println("Syntax is: --days (number) --output (file) --TimeZone (zone) --noicons --channelfile (file)\n" +
                        "--days number : Number of days to grab. Max 8. \n\tExample: --days 5\n" +
                        "--output (file) : File where tvguide is stored. \n\tExample: --output tvguide.xml\n" +
                        "--TimeZone (zone) : Time zone offset in xml-file. \n\tExample: --TimeZone +0100\n" +
                        "--noicons : Remove icons from xml-file. \n\tExample: --noicons\n" +
                        "--channelfile : Select which file the channels should be read from. \n\tExample: --channelfile channels.xml\n" + 
                        "--sleeptime (ms) : The application will wait between every data fetched from dr.dk." +
                        "\n\tThis is done to relax load on dr-servers.\n" +
                        "--DRcategory : Enable reading of categories from dr.dk. Won't work on radio-guide\n" + 
                        "--noGuess : Disable guessing of categories. Reading guess.txt\n" + 
                        "example: webreader.jar --days 5 --output myguide.xml --TimeZone +0100 --noicons\n" +
                        "default is: days=2, output=tvguide.xml and TimeZone=+0100 channelfile=channels-tv.xml sleeptime=200 if no arguments are used");
                return;
            }
        }//end input argument parsing
        
                              
        File outFile = new File(outputFile);
        if(useCategoryGuess)
            guessFile = new File(guessFileStr);
        
        //get current date
        Calendar rightNow = new GregorianCalendar();
         
        //create list for channel data
        LinkedList<channel> channelList = null;
        try {
            //Read channels.txt list of channel ID's and channel names
            channelList = getChannelsXML(new File(channelFile));
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
            return;
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        LinkedList<tvInfo> dataList = new LinkedList<tvInfo>();
        int charCnt=0;
        int channelCount = 0;
        int day = 0;
        while(day < daysToRead)
        {
            System.out.print("Guide: Reading day " + (day+1) + " of " + daysToRead + " ...");
            while(channelCount < channelList.size() ) 
            {
                String channelID = ((channel)channelList.get(channelCount)).ID;
                String channelName = ((channel)channelList.get(channelCount)).Name;
                URL drGuide = null;
                try {
                    drGuide = new URL("http://www.dr.dk/tjenester/programoversigten/w3c/inc/channel.aframe?channel=" + channelID + "&seldate=" + day + "&seltime=0");
                } catch (MalformedURLException ex) {
                    ex.printStackTrace();
                    System.err.println("Error in URL");
                    return;
                }
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new InputStreamReader(drGuide.openStream()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.err.println("Error opening DR input stream" +
                            "\nDR is probably down");
                }
                //Thread sleep to make DR happy
                Thread.sleep(sleepTime);
            

                //Correct date when using this application between midnight and 05:00
                if(rightNow.get(Calendar.HOUR_OF_DAY)<5)
                    rightNow.add(Calendar.DAY_OF_MONTH,-1);

                String Date = Calendar2String(rightNow);//get current date -but not hour/min
                rightNow.add(Calendar.DAY_OF_MONTH,1);
                //Store a string with a date one higher. Used for timestamping when past midnight
                String DatePlus1 = Calendar2String(rightNow);
                rightNow.add(Calendar.DAY_OF_MONTH,-1);
                //System.out.println("D: " + Date + "  D1: " + DatePlus1);
                tvInfo tvData = new tvInfo();

                int cnt = 1;
                String inputLine;
                while ((inputLine = in.readLine()) != null){
                    charCnt += inputLine.length();
                    int classIndex = inputLine.indexOf("class=");

                    if(classIndex>0)
                    {
                        inputLine = inputLine.replaceAll("&nbsp;", " ");
                        inputLine = RemoveDivTag(inputLine, classIndex);
                        int programmerIndex = inputLine.indexOf("programmer", classIndex);
                        if((programmerIndex > 0) && ((programmerIndex-classIndex) < 10))
                        {
                            if(tvData.Program!=null)
                            {
                                dataList.add(tvData);
                                tvData = new tvInfo();//create new data in memory
                            }

                            tvData.Channel = channelID;
                
                            //Find start of time
                            int timeIndex = inputLine.indexOf(">", classIndex) + 1;
                            //Read time
                            tvData.Time = inputLine.substring(timeIndex, timeIndex+2) + inputLine.substring(timeIndex+3, timeIndex+5);
                            if(Integer.parseInt(tvData.Time)<500)
                                tvData.Time = DatePlus1 + tvData.Time;
                            else
                                tvData.Time = Date + tvData.Time;
                            //System.out.println(Time);

                            //detect repeats
                            if(inputLine.indexOf("Genudsendelse")>-1)
                                tvData.repeat = 1;
                            else
                                tvData.repeat =-1;

                            int endProgramIndex = inputLine.indexOf("<", timeIndex) + 1;
                            tvData.Program = htmlConvert(inputLine.substring(timeIndex+5, endProgramIndex-1)).trim();

                            int EpisodeIndexStart = inputLine.indexOf('(', timeIndex);
                            int EpisodeIndexEnd = inputLine.indexOf(')', EpisodeIndexStart);
                            if( (EpisodeIndexStart>0) && (EpisodeIndexEnd>0) )
                            {
                                //System.out.println(EpisodeIndexStart + " " + EpisodeIndexEnd + " : " );
                                tvData.Episode = inputLine.substring(EpisodeIndexStart+1, EpisodeIndexEnd);
                                  try{
                                    Integer.parseInt(tvData.Episode); //see if it is a number
                                }catch( NumberFormatException ex){
                                    //System.err.println("Not an error: Tried to parse: (" + tvData.Episode + ") as episode number. Channel:" + channelID + " Date:" + tvData.Time + " Line:" + cnt);
                                    tvData.Episode = null;
                                }
                                if(tvData.Episode != null)//remove episode number from tvdata.program
                                {
                                    int episodeStart = tvData.Program.indexOf( "(" + tvData.Episode + ")" );
                                    tvData.Program = tvData.Program.substring(0, episodeStart);
                                    tvData.Program = tvData.Program.trim();
                                    //System.out.println("|" + tvData.Program + "|");
                                }
                            }


                 //           System.out.println(tvData.Time + " - " + tvData.Program + " " + tvData.Episode + "  : " + cnt);
                        }

                        int p_textIndex = inputLine.indexOf("\"p_text\"", classIndex);
                        if((p_textIndex > 0) && ((p_textIndex-classIndex) < 10))
                        {
                            int textStartIndex = inputLine.indexOf(">", p_textIndex) + 1;
                            int textEndIndex = inputLine.indexOf("<", textStartIndex);
                            tvData.ProgramInfo = htmlConvert(inputLine.substring(textStartIndex, textEndIndex));
                  //          System.out.println(tvData.ProgramInfo + "   : " + cnt);
                        }

                        int p_text_borderIndex = inputLine.indexOf("p_text_border", classIndex);
                        if((p_text_borderIndex > 0) && ((p_text_borderIndex-classIndex) < 10))
                        {
                            int textStartIndex = inputLine.indexOf(">", p_text_borderIndex) + 1;
                            int textEndIndex = inputLine.indexOf("<", textStartIndex);
                            tvData.ProgramInfoExt = htmlConvert(inputLine.substring(textStartIndex, textEndIndex));
                //            System.out.println(tvData.ProgramInfoExt + "   : " + cnt);
                        }

                    }//end classindex

                    //System.out.println("classIndex: " + classIndex);
                    //System.out.println("programmerIndex: " + programmerIndex);

                    cnt++;
                }
                //System.out.println("lines: " + cnt);
                if(tvData.Program != null)
                    dataList.add(tvData);//add last element
                in.close();//Close input buffer dr

                channelCount++;

            }//END : channelCount < (channelList.size())
            System.out.print(" done\n");
            rightNow.add(Calendar.DAY_OF_MONTH,1);
            day++;
            channelCount = 0;
        }//END: day<daysToRead
        System.out.print("Guide: Read " + addDot(charCnt*2) + " bytes from dr.dk\n");
        //categories and genre is used as the same term...
        if(useCategories){
            GetGenrer genreList = new GetGenrer(daysToRead, sleepTime);//Create object
            if(genreList.LoadCategory()!=0){//read Categories
                System.err.println("Error reading categories. Exiting");
                return;
            }
            System.out.print("\nCategory: Read " + addDot(genreList.getSize()) + " bytes from dr.dk");
            AddCategory(dataList, genreList.getList());
            
        }
                
        writeXMLFile(outFile, dataList, channelList, tZone, icons, version, guessFile);
        
        System.out.print("\nFile written!");
    }

    /** 
     * Function converting a Calendar to string
     *
     */
    public static String Calendar2String(Calendar rightNow)
    {
        String currentTime = Integer.toString(rightNow.get(Calendar.YEAR));
        if(rightNow.get(Calendar.MONTH)+1 > 9)
            currentTime += Integer.toString(rightNow.get(Calendar.MONTH)+1);
        else
            currentTime += "0" + Integer.toString(rightNow.get(Calendar.MONTH)+1);
        
        if(rightNow.get(Calendar.DAY_OF_MONTH) > 9)
            currentTime += Integer.toString(rightNow.get(Calendar.DAY_OF_MONTH));
        else
            currentTime += "0" + Integer.toString(rightNow.get(Calendar.DAY_OF_MONTH));
        
        return currentTime;
        
    }
   
    
    /**
     *Function removing html-tags from a String.
     */
    public static String RemoveDivTag(String in, int startIndex)
    {
        int startTagIndex = in.indexOf("<", startIndex);
        int endtableIndex = in.indexOf("</td", startIndex);
        while( (startTagIndex > 0) && (startTagIndex < endtableIndex) ){
            int endTagIndex = in.indexOf(">", startTagIndex);
            if( startIndex < endTagIndex){    
                //System.out.println("index:" + index);
                in = in.substring(0, startTagIndex) + in.substring(endTagIndex+1); 
                startTagIndex = in.indexOf("<");
                //System.out.println(in);
            }
        }
        
        return in;
    }
    
    /** Returns a LinkedList of channel ID's and channel names read from an xml-file
     */
    public static LinkedList<channel> getChannelsXML(File inputFile) throws FileNotFoundException, IOException
    {
        FileInputStream FIS = new FileInputStream(inputFile);
        BufferedReader BFIS = new BufferedReader(new InputStreamReader(FIS));
        LinkedList<channel> list = new LinkedList<channel>();
        
        String inputLine;
        int lineCnt = 0;
        channel chan = null;
        while( (inputLine = BFIS.readLine()) != null){
            lineCnt++;
            //System.out.println("gC:" + inputLine);
            if( (inputLine.indexOf('#') == -1) || (inputLine.indexOf('#') > 4)){
                //read channel id
                int channelIDIndex = inputLine.indexOf("<channel id");
                int quotIndex= inputLine.indexOf('"', channelIDIndex);
                int quotIndex2= inputLine.indexOf('"', quotIndex+1);
                if( (channelIDIndex != -1) && (quotIndex != -1) && (quotIndex2 != -1) ){
                    chan = new channel();
                    chan.ID = inputLine.substring(quotIndex+1, quotIndex2);
                }

                //read channel name
                int channelNameIndex = inputLine.indexOf("<display-name>");
                int channelNameIndexEnd = inputLine.indexOf("</display-name>");
                if( (channelNameIndex != -1) && (channelNameIndexEnd != -1) ){
                    chan.Name = inputLine.substring(channelNameIndex+14, channelNameIndexEnd);

                    //convert to html tag. Thx to Michael Steffensen for correcting this.
                    chan.Name = htmlConvert(chan.Name);

                    //System.out.println("c:" + chan.ID + "  ID:" + chan.Name);
                }

                //read icon source
                int iconIndex = inputLine.indexOf("<icon>");
                int iconIndexEnd = inputLine.indexOf("</icon>");
                if( (iconIndex != -1) && (iconIndexEnd != -1) ){
                    chan.Icon = inputLine.substring(iconIndex+6, iconIndexEnd);
                }

                //store channel in linked list
                if(inputLine.indexOf("</channel>") != -1){
                    list.add(chan);
                    //System.out.println("c:" + chan.ID + "  ID:" + chan.Name);
                }
            }             
        }
        
        return list;
    }
    
    /** Returns a LinkedList of channel ID's and channel names
     */
    public static LinkedList<channel> getChannels(File inputFile) throws FileNotFoundException, IOException{
        FileInputStream FIS = new FileInputStream(inputFile);
        BufferedReader BFIS = new BufferedReader(new InputStreamReader(FIS));
        LinkedList<channel> list = new LinkedList<channel>();
        
        String inputLine;
        
        while( (inputLine = BFIS.readLine()) != null){
            //System.out.println("gC:" + inputLine);
            if( inputLine.indexOf('#') == -1 ){
                int spaceIndex= inputLine.indexOf(' ');
                int Index= inputLine.indexOf('"', spaceIndex);
                int Index2= inputLine.indexOf('"', Index+1);
                if( (spaceIndex != -1) && (Index != -1) && (Index2 != -1) ){
                    channel chan = new channel();
                    chan.ID = inputLine.substring(0, spaceIndex);
                    chan.Name = inputLine.substring(Index+1, Index2);
                    
                    //convert to html tag. Thx to Michael Steffensen for correcting this.
                    chan.Name = htmlConvert(chan.Name);
                    list.add(chan);
                    //System.out.println("c:" + chan.ID + "  ID:" + chan.Name);
                }
            }
        }
        System.err.println("Read " + list.size() + " channel(s) from " + inputFile.getName() + " file.");
        
        return list;
    }
    /**
     * Function converting & to &amp; and so on. 
     * Calling this function multiple times on the same string will probably cause problems.
     * This is due to & being an escape code, and the conversion isn't inteligent
     */
    public static String htmlConvert(String str){
        int ampIndex = -1;
        do{
            ampIndex = str.indexOf('&', ampIndex+1);
            if(ampIndex != -1){
                int semiIndex = str.indexOf(';', ampIndex);
                //System.out.println(str + " amp:" + ampIndex + " ; " + semiIndex);
                if( (semiIndex-ampIndex > 0) && (semiIndex-ampIndex < 8)){
                    
                }
                else{
                    str = str.substring(0, ampIndex) + "&amp;" + str.substring(ampIndex+1);
                }
                //System.out.println(str + " amp:" + ampIndex + " ; " + semiIndex);
            }
        }while(ampIndex!=-1);
            
        str = str.replaceAll("'"," ");
        str = str.replaceAll("'","`");
        //str = str.replaceAll("&","&amp;");
    /*
        str = str.replaceAll("<","&lt;");
        str = str.replaceAll(">","&gt;");
        str = str.replaceAll("c","&aelig;");
        str = str.replaceAll("C","&Aelig;");
        str = str.replaceAll("r","&oslash;");
        str = str.replaceAll("R","&Oslash;");
        str = str.replaceAll("l","&aring;");
        str = str.replaceAll("L","&Aring;");
        str = str.replaceAll(" ","&rsquo;");
     */
        str = str.replaceAll("&nbsp;"," ");
        
        return str;
    }
    
    /**
     *Function which writes an xml-file
     */
    public static int writeXMLFile(File outputFile, LinkedList<tvInfo> programList, LinkedList<channel> channelList, String tZone, boolean icons, String version, File guessFile) throws IOException{
        FileWriter outFile;
        try{
            outFile = new FileWriter(outputFile);
        }catch(IOException ex){
            System.out.println(ex.getMessage());
            System.out.println("Error opening output file");
            return -1;
        }
        boolean guessEnable=false;
        GuessCategory guessObj=null;
        if(guessFile!=null){
            guessEnable=true;
            guessObj = new GuessCategory();
            try {
                int guessCnt = guessObj.ReadList(guessFile);
                System.out.println("Read " + guessCnt + " category guesses from file.");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        //write header
        outFile.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
        outFile.write("<!DOCTYPE tv SYSTEM \"xmltv.dtd\">\n\n");
        outFile.write("<tv generator-info-name=\"rdj_tv_grab_dr.dk_ver_" + version + "\">\n");
        
        //write channel identifiers
        channel chan;
        while(channelList.size()>0){
            chan = channelList.removeFirst();
            outFile.write("  <channel id=\"" + chan.ID + "\">\n");
            outFile.write("    <display-name>" + chan.Name + "</display-name>\n");
            if( icons ){
                outFile.write("    <icon src=\"" + chan.Icon + "\" />\n");
            }
            
            outFile.write("  </channel>\n");
        }
        
                
        tvInfo data;
        //data = (tvInfo) list.removeFirst();
        System.out.println("\nTotal number of programs: " + programList.size());
        while(programList.size()>0){
            data = programList.pop();
            if(data.Program!=null){
                outFile.write("  <programme channel=\"" + data.Channel + "\" start=\"" + data.Time + "00 " + tZone + "\">\n");
                outFile.write("    <title lang=\"dk\">" + data.Program + "</title>\n");
                
                if(data.ProgramInfo!=null){
                    //outFile.write("    <title>" + data.ProgramInfo + "</title>\n");
                    //Removed because mediaportal doesn't seem to understand sub-title
                    outFile.write("    <sub-title lang=\"dk\">" + data.ProgramInfo + "</sub-title>\n");
                
                }
                if(data.ProgramInfoExt!=null){
                    outFile.write("    <desc lang=\"dk\">" + data.ProgramInfoExt + "</desc>\n");
                
                }
                if(data.Category!=-1){
                    outFile.write("    <category lang=\"dk\">" + resolveCategory(data.Category) + "</category>\n");
                }
                else if(guessEnable){
                    if(data.ProgramInfo!=null){
                        String cat = guessObj.Match(data.ProgramInfo);
                        if(cat!=null)
                            outFile.write("    <category lang=\"dk\">" + cat + "</category>\n");
                    }
                }
                if(data.Episode!=null){
                    outFile.write("    <episode-num system=\"xmltv_ns\"> . " + data.Episode + " . </episode-num>\n" );
                }
                if(data.repeat==1){
                    outFile.write("    <previously-shown />\n");
                }
                
                outFile.write("  </programme>\n");
            }
        }
        
        //Write data to File and Close file
        outFile.write("</tv>\n");
        outFile.close();
        
        return 0;
        /* 
         <tv generator-info-name="tv_grab_uk">
              <channel id="bbc2.bbc.co.uk">
                 <display-name lang="en">BBC2</display-name>
              </channel>
              <channel id="channel4.com">
                 <display-name lang="en">Channel 4</display-name>
              </channel>
         
         
          <programme channel="channel4.com" start="20010829095500 +0100">
             <title lang="en">King of the Hill</title>
             <sub-title lang="en">Meet the Propaniacs</sub-title>
             <desc lang="en">
                 Bobby tours with a comedy troupe who specialize in
                 propane-related mirth.
             </desc>
             <credits>
                <actor>Mike Judge</actor>
                <actor>Lane Smith</actor>
             </credits>
             <category lang="en">animation</category>
          </programme>
         
         <programme start="200006031633" channel="3sat.de">
   33     <title lang="de">blah</title>
   34     <title lang="en">blah</title>
   35     <desc lang="de">
   36        Blah Blah Blah.
   37     </desc>
   38     <credits>
   39       <director>blah</director>
   40       <actor>a</actor>
   41       <actor>b</actor>
   42     </credits>
   43     <date>19901011</date>
   44     <country>ES</country>
   45     <episode-num system="xmltv_ns">2 . 9 . 0/1</episode-num>
   46     <video>
   47       <aspect>16:9</aspect>
   48     </video>
   49     <rating system="MPAA">
   50       <value>PG</value>
   51       <icon src="pg_symbol.png" />
   52     </rating>
   53     <star-rating>
   54       <value>3/3</value>
   55     </star-rating>
   56   </programme>
         
                  */
    }
    
    public static void AddCategory(LinkedList<tvInfo> programList, LinkedList<genre> genreList){
        System.out.print("\nMatching categories with tv-guide ... ");
        int matchcnt =0;
        for(tvInfo Info : programList){
            for(genre Element : genreList){
                if(Element.channel.equals(Info.Channel)){
                    if(Element.time.equals(Info.Time)){
                        //System.out.println("Match: " + Element.channel + " " + Element.time);
                        Info.Category = Element.category;
                        matchcnt++;
                    }
                }
            }
            try {
                Thread.sleep(0);//Reduce the amount of processing power used
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        System.out.print("done.");
        //System.out.print("done. Matched " + matchcnt + " of " + genreList.size());
        
    }
    
    
    /*Categories
        <a href="#" onclick="NormalLink('search.asp?genre=7','TV'); return false;">Brrn</a><br>
        <a href="#" onclick="NormalLink('search.asp?genre=1','TV'); return false;">Film</a><br>

        <a href="#" onclick="NormalLink('search.asp?genre=4','TV'); return false;">Dokumentar</a><br>
        <a href="#" onclick="NormalLink('search.asp?genre=5','TV'); return false;">Natur</a><br>
        <a href="#" onclick="NormalLink('search.asp?genre=9','TV'); return false;">Nyheder</a><br>
        <a href="#" onclick="NormalLink('search.asp?genre=3','TV'); return false;">Sport</a><br>
        <a href="#" onclick="NormalLink('search.asp?genre=2','TV'); return false;">Serier</a><br>
        <a href="#" onclick="NormalLink('search.asp?genre=6;genre=8','TV'); return false;">Underholdning</a></div>*/

    public static String resolveCategory(int catID){
        switch(catID){
            case 1:
                return "Film";
            case 2:
                return "Serier";
            case 3:
                return "Sport";
            case 4:
                return "Dokumentar";
            case 5:
                return "Natur";
            case 6:
                return "Underholdning";
            case 7:
                return "Brrn";
            case 9:
                return "Nyheder";
            default:
                return "Unknown";//Should not be used
        }
    }
    public static String addDot(int no){
        char[] charArr = Integer.toString(no).toCharArray();
        String str="";
        for(int i=0 ; i<charArr.length ; i++){
            if(((i%3)==0) && (i>0))
                str = "." + str;
            str = String.valueOf(charArr[charArr.length-1-i]) + str;
            
        }
        return str;
    }

   
}

