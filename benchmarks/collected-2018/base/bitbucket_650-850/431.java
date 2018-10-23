// https://searchcode.com/api/result/127674511/

/*
 * Main.java
 *
 * Created on 3. october 2007, 20:16

 * Written by Rune Dahl Jrrgensen @ rune_dahl@hotmail.com
 * Contributors:
 * Nino Saturnino Martinez Vazquez Wael
 * Rene Jensen
 * Morten Piil
 */

package webreader;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.LinkedList;

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
    static Log log = new Log();
       
   
    public static void main(String[] args) throws Exception {
        //For testing only
        if(false){
            DRtalk drt = new DRtalk();
            //System.out.print(drt.query("getChannels", "\"type\":tv"));
            //System.out.println(drt.getChannels("tv"));
            String tst = drt.getSchedule("dr.dk/mas/whatson/channel/DR1", "2009-11-02T00:00:00.0000000+01:00");
            System.out.println(tst);
            //System.out.println(tst);
            //JSON t0 = new JSON("HEAD");
            //t0.list = new LinkedList<JSON>();
            //t0.name = "HEAD";
            //drt.createList(tst, t0);
            //System.out.println("");
            //System.out.println(drt.getSchedule("dr.dk/mas/whatson/channel/DR1", "2009-08-17T00:00:00.0000000+02:00").substring(1000));
            //drt.parseSchedule(drt.getSchedule("dr.dk/mas/whatson/channel/DR1", "2009-08-25T00:00:00.0000000+02:00"));
            //System.out.println(drt.getAvailableBroadcastDates());
            //System.out.println(drt.getParsedCurrentBroadcastDate());
            //drt.getParsedAvailableBroadcastDates();
            //LinkedList<String> channels = drt.getParsedChannels();
            //LinkedList<String> usefulDates = drt.getUsefulBroadcastDates();
            //LinkedList<tvInfo> tvInfoList = new LinkedList<tvInfo>();
            //for(String date : usefulDates){
            //    drt.fillScheduleList(tvInfoList, date, "dr.dk/mas/whatson/channel/DR1");
            //}
            
            //for(String chan : channels)
            //    System.out.println(chan);
            //drt.printChannels();    
           
            return;
        }
        
        
        //create debug log
        File errorFile = new File("error.log");
        PrintStream PSerr = new PrintStream(errorFile);
        System.setErr(PSerr);
        //System.out.println("Note that all errors are written to error.log");
        
        //default settings
        String version = "0.98.0";
        boolean debugEnable = false;
        long sleepTime = 50;
        String outputFile = "tvguide.xml";
        String fileEncoding = "UTF-8";
        String channelFile = "channels-tv.xml";
        String guessFileStr = "guess.txt";
        String genreFile = "genre.txt";
        File guessFile = null;
        String tZone = "+0100";
        boolean icons = true;
        boolean useCategoryGuess = true;
        boolean useSeries = false;
        boolean copyEpisode = false;
        boolean copyRepeat = false;
        boolean copyProgramInfo = false;
        boolean copyCredits = false;
        
        System.out.println("DR EPG Ripper - Version " + version + "\n" +
                "By Rune Dahl Joergensen. Report bugs to Rune_dahl@hotmail.com");
        
        for(int i = 0; i < args.length ; i++){
            //System.out.println("Parsing arg" + i + ":" + args[i]);
            if(args[i].equalsIgnoreCase("--debug"))
                debugEnable = true;
            if(args[i].equalsIgnoreCase("--output"))
                if(i!=args.length-1)
                    outputFile = args[i+1];
                else{
                    System.out.println("Missing " + args[i] + " argument.");
                    return;
                }
            if(args[i].equalsIgnoreCase("--encoding"))
                if(i!=args.length-1)
                    fileEncoding = args[i+1];
                else{
                    System.out.println("Missing " + args[i] + " argument.");
                    return;
                }
            if(args[i].equalsIgnoreCase("--channelfile"))
                if(i!=args.length-1)
                    channelFile = args[i+1];
                else{
                    System.out.println("Missing " + args[i] + " argument.");
                    return;
                }
            if(args[i].equalsIgnoreCase("--genrefile"))
                if(i!=args.length-1)
                    genreFile = args[i+1];
                else{
                    System.out.println("Missing " + args[i] + " argument.");
                    return;
                }
            if(args[i].equalsIgnoreCase("--TimeZone"))
                if(i!=args.length-1)
                    tZone = args[i+1];
                else{
                    System.out.println("Missing " + args[i] + " argument.");                    
                    return;
                }
            if(args[i].equalsIgnoreCase("--noIcons"))
                icons = false;
            if(args[i].equalsIgnoreCase("--sleeptime"))
                if(i!=args.length-1)
                    sleepTime = Integer.parseInt(args[i+1]);
                else{
                    System.out.println("Missing " + args[i] + " argument.");
                    return;
                }
            if(args[i].equalsIgnoreCase("--noGuess"))
                useCategoryGuess = false;
            if(args[i].equalsIgnoreCase("--useSeries"))
                useSeries = true;
            if(args[i].equalsIgnoreCase("--copyEpisode"))
                copyEpisode = true;
            if(args[i].equalsIgnoreCase("--copyRepeat"))
                copyRepeat = true;
            if(args[i].equalsIgnoreCase("--copyProgramInfo"))
                copyProgramInfo = true;
            if(args[i].equalsIgnoreCase("--copyCredits"))
                copyCredits = true;
            if(args[i].equalsIgnoreCase("--setup")){
               GUI gui = new GUI();
               gui.startGUI("tv");
               return;
            }
            if(args[i].equalsIgnoreCase("--setup-radio")){
               GUI gui = new GUI();
               gui.startGUI("radio");
               return;
            }
            if(args[i].equalsIgnoreCase("--help")){
                System.out.println("Syntax is: --days (number) --output (file) --TimeZone (zone) --noicons --channelfile (file)\n" +
                        "--output (file) : File where tvguide is stored. \n\tExample: --output tvguide.xml\n" +
                        "--encoding (type) : File encoding. Default: UTF-8\n\tExample: --encoding UTF-8\n" +
                        "--genrefile (file) : File with categories. \n\tExample: --genrefile genre.txt\n" +
                        "--TimeZone (zone) : Time zone offset in xml-file. \n\tExample: --TimeZone +0100\n" +
                        "--noicons : Remove icons from xml-file. \n\tExample: --noicons\n" +
                        "--channelfile : Select which file the channels should be read from. \n\tExample: --channelfile channels.xml\n" + 
                        "--sleeptime (ms) : The application will wait between every data fetched from dr.dk." +
                        "\n\tThis is done to relax load on dr-servers.\n" +
                        "--noGuess : Disable guessing of categories. Reading guess.txt\n" +
                        "--useSeries : Changes the title of a program if it starts with one of the lines in the series.txt file\n" +
                        "--copyEpisode : Copies the episode info to the program info\n"+
                        "--copyRepeat : Copies the repeat info into the program info\n" +
                        "--copyProgramInfo : Copies the sub title into the program info\n" +
                        "--copyCredits : Copies the credits into the program info\n" +
                        "--setup : Setup which tv channels to read\n" +
                        "--setup-radio : Setup which radio channels to read\n" +
                        "example: webreader.jar --output myguide.xml --TimeZone +0100 --noicons\n" +
                        "default is: output=tvguide.xml and TimeZone=+0100 channelfile=channels-tv.xml sleeptime=50 if no arguments are used");
                return;
            }
        }//end input argument parsing
        if(debugEnable)
            System.out.println("Done Parsing inputs.");
        log.enable(debugEnable);
        
        File outFile = new File(outputFile);
        if(useCategoryGuess)
            guessFile = new File(guessFileStr);
        
        //create list for channel data
        LinkedList<channel> channelList = null;
        try {
            //Read channels.txt list of channel ID's and channel names
            channelList = getChannelsXML(new File(channelFile));
        } catch (FileNotFoundException ex) {
            System.out.println("Unable to open file: " + channelFile);
            System.err.println(ex.getMessage());
            GUI gui = new GUI();
            gui.startGUI("tv");
            return;
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        LinkedList<tvInfo> dataList = new LinkedList<tvInfo>();


        LinkedList<GenreCode> genreList = null;
        try {
            genreList = getGenreCodes(new File(genreFile));
        } catch (FileNotFoundException e) {
            System.out.println("Missing genre file:" + genreFile);
            genreList = new LinkedList<GenreCode>();
        }
        // NEW PART /////////////////////////////////////////////
        DRtalk drt = new DRtalk();
        LinkedList<String> usefulDates = drt.getUsefulBroadcastDates();
        for(channel chan : channelList){
            System.out.println("Reading channel:" + chan.Name);
            for(String date : usefulDates){                
                //drt.fillScheduleList(dataList, date, "dr.dk/mas/whatson/channel/DR1");
                drt.fillScheduleList(dataList, date, chan.ID);
                Thread.sleep(sleepTime);
            }
        }
        
        if(dataList.size()>0){
            //Get categories from file
            GuessCategory guessObj=null;
            if(guessFile!=null){
                guessObj = new GuessCategory();
                try {
                    int guessCnt = guessObj.ReadList(guessFile);
                    //System.out.println("Read " + guessCnt + " category guesses from file.");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            ModifyProgramInfoExt(dataList, useSeries, copyRepeat, copyEpisode, copyProgramInfo, copyCredits);

            writeXMLFile(outFile, fileEncoding, dataList, channelList, tZone, icons, version, guessObj, genreList);
            System.out.print("Guess-file had " + guessObj.Matches + " matches.");
            System.out.print("\n" + outputFile + " written!\n");
        }
    }

    /**
     * Modify ProgramInfoExt
     * @param data
     * @throws IOException
     */
    private static void ModifyProgramInfoExt(LinkedList<tvInfo> dataList, boolean useSeries, boolean copyRepeat, boolean copyEpisode, boolean copyProgramInfo, boolean copyCredits) throws IOException
    {
        //Read list of series
        LinkedList<String> SeriesList = new LinkedList<String>();
        if(useSeries){
            //System.out.println("\nReading series.txt file");
            FileInputStream FIS = null;
            try {
                FIS = new FileInputStream("series.txt");
            } catch (FileNotFoundException ex) {
                System.out.println("No series.txt file found.");
                System.err.println("No series.txt file found.");
                //ex.printStackTrace();
            }
            if(FIS!=null){
                BufferedReader BFIS = new BufferedReader(new InputStreamReader(FIS));

                String inputLine;
                while( (inputLine = BFIS.readLine()) != null)
                {
                    if(inputLine.length()>2)
                        SeriesList.add(inputLine.trim());
                }
            }
        }
        
        for(tvInfo data : dataList){
            //If the ProgramInfoExt is null, then we make it just empty
            if( (data.ProgramInfoExt==null) && (copyRepeat || copyEpisode || copyProgramInfo || copyCredits))
                data.ProgramInfoExt = "";

            if (data.repeat && copyRepeat) {
                data.ProgramInfoExt = "Genudsendelse\r\n" + data.ProgramInfoExt;
            }
            if ( (data.EpisodeNo >= 0) && copyEpisode ) {
                if(data.EpisodeMax >= 0 )
                    data.ProgramInfoExt = "Episode (" + data.EpisodeNo + ":" + data.EpisodeMax + ")\r\n" + data.ProgramInfoExt;
                else
                    data.ProgramInfoExt = "Episode (" + data.EpisodeNo + ")\r\n" + data.ProgramInfoExt;
            }
            if ( (data.ProgramInfo != null) && copyProgramInfo ) {
                data.ProgramInfoExt = data.ProgramInfo + "\r\n" + data.ProgramInfoExt;
            }
            if ( (data.Credits != null) && copyCredits ) {
                if ( data.Credits.Director.size() > 0 )  {
                    data.ProgramInfoExt = data.ProgramInfoExt + "\r\nInstruktion: ";
                    for(int directorIndex=0; directorIndex<data.Credits.Director.size(); directorIndex++){
                        data.ProgramInfoExt = data.ProgramInfoExt + data.Credits.Director.get(directorIndex);
                        if(directorIndex<data.Credits.Director.size()-1)
                            data.ProgramInfoExt = data.ProgramInfoExt + ", ";
                    }
                }
                if ( data.Credits.Actor.size() > 0 )  {
                    data.ProgramInfoExt = data.ProgramInfoExt + "\r\nMedvirkende: ";
                    for(int actorIndex=0; actorIndex<data.Credits.Actor.size(); actorIndex++){
                        actor actorObj = data.Credits.Actor.get(actorIndex);
                        if(actorObj.role != null){
                            data.ProgramInfoExt = data.ProgramInfoExt +
                                data.Credits.Actor.get(actorIndex).role + ": " + data.Credits.Actor.get(actorIndex).actorName;
                        }
                        else{
                            data.ProgramInfoExt = data.ProgramInfoExt +
                                data.Credits.Actor.get(actorIndex).actorName;
                        }
                        if(actorIndex<data.Credits.Actor.size()-1)
                            data.ProgramInfoExt = data.ProgramInfoExt + ", ";
                    }
                }
            }
            for(String FixIt : SeriesList)
            {
                if (data.Program.startsWith(FixIt)) {
                    //System.out.println("Found program: " + data.Program);
                    if(data.ProgramInfoExt==null)
                        data.ProgramInfoExt = data.Program;
                    else
                        data.ProgramInfoExt = data.Program + "\r\n" + data.ProgramInfoExt;
                    data.Program = FixIt;
                }
            }
        }
    }

    /** Returns a LinkedList of channel ID's and channel names read from an xml-file
     */
    public static LinkedList<GenreCode> getGenreCodes(File inputFile) throws FileNotFoundException, IOException
    {
        FileInputStream FIS = new FileInputStream(inputFile);
        BufferedReader BFIS = new BufferedReader(new InputStreamReader(FIS));
        LinkedList<GenreCode> list = new LinkedList<GenreCode>();

        String inputLine;
        int lineCnt = 0;
        GenreCode gen = null;
        while( (inputLine = BFIS.readLine()) != null){
            lineCnt++;
            //System.out.println("gC:" + inputLine);
            if( (inputLine.indexOf('#') == -1) || (inputLine.indexOf('#') > 4)){
                String[] split = inputLine.split(":");
                if(split.length == 3){
                    gen = new GenreCode();
                    gen.mainCode = Integer.parseInt(split[0]);
                    gen.subCode = Integer.parseInt(split[1]);
                    gen.description = split[2].trim();
                    list.add(gen);
                }
            }
        }
        return list;
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
                if(chan != null){
                    //read DR ID
                    int channelDrIDIndex = inputLine.indexOf("<drid>");
                    int channelDrIDIndexEnd = inputLine.indexOf("</drid>");
                    if( (channelDrIDIndex != -1) && (channelDrIDIndexEnd != -1) ){
                        chan.DrID = inputLine.substring(channelDrIDIndex+6, channelDrIDIndexEnd);
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

                    //read timezone
                    int timezoneIndex = inputLine.indexOf("<timezone>");
                    int timezoneIndexEnd = inputLine.indexOf("</timezone>");
                    if( (timezoneIndex != -1) && (timezoneIndexEnd != -1) ){
                        chan.TimeZone= inputLine.substring(timezoneIndex+10, timezoneIndexEnd);
                    }

                    //store channel in linked list
                    if(inputLine.indexOf("</channel>") != -1){
                        list.add(chan);
                        //System.out.println("c:" + chan.ID + "  ID:" + chan.Name + "  TimeZone:" + chan.TimeZone);
                        chan = null;//invalidate object
                    }
                }
            }             
        }
        
        return list;
    }
   
    /**
     * Function converting & to &amp; and so on. 
     * Calling this function multiple times on the same string will probably cause problems.
     * This is due to & being an escape code, and the conversion isn't inteligent
     */
    public static String htmlConvert(String str){
        //Replace ampersand(&) with &amp; but make sure that it isn't already done.
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
          
        str = str.replace("?", "'");
        str = str.replace("`", "'");
        //str = str.replaceAll("&","&amp;");
    
        str = str.replace("<","&lt;");
        str = str.replace(">","&gt;");
//        str = str.replace("?","&rsquo;");
        for(int i=0;i<str.length();i++){
            int ch = str.codePointAt(i);
            if(!Character.isLetterOrDigit(ch) && !((ch>0x1F)&&(ch<0x7F))){
//                System.out.println("Not a letter @ " + i + " : |" + ch + "|" + str.charAt(i));
//                System.out.println(str);
                if(ch==160)//Replace strange space character with ordinary space character
                    str = str.replace(str.substring(i,i+1), " ");
                else
                    str = str.replace(str.substring(i,i+1), "");
                i--;
            }
        }
        str = str.replaceAll(" +", " ");//remove double spaces
        str = str.replace("&nbsp;"," ");
        
        return str;
    }
    
    /**
     *Function which writes an xml-file
     */
    public static int writeXMLFile(File outputFile, String fileEncoding, LinkedList<tvInfo> programList, LinkedList<channel> channelList, String tZone, boolean icons, String version, GuessCategory guessObj, LinkedList<GenreCode> genreList) throws IOException{
        OutputStreamWriter outFile;
        try{
            outFile = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(outputFile)), fileEncoding);
        }catch(IOException ex){
            System.out.println(ex.getMessage());
            System.out.println("Error opening output file");
            return -1;
        }
        boolean guessEnable=false;
        if(guessObj!=null)
            guessEnable=true;
        
        //write header
        //outFile.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
        outFile.write("<?xml version=\"1.0\" encoding=\"" + fileEncoding + "\"?>\n");

        outFile.write("<!DOCTYPE tv SYSTEM \"xmltv.dtd\">\n\n");
        outFile.write("<tv generator-info-name=\"rdj_tv_grab_dr.dk_ver_" + version + "\">\n");
        
        //write channel identifiers
        for(channel chan : channelList){
            outFile.write("  <channel id=\"" + chan.ID + "\">\n");
            outFile.write("    <display-name>" + chan.Name + "</display-name>\n");
            if( icons && (chan.Icon != null))
                outFile.write("    <icon src=\"" + chan.Icon + "\" />\n");
                        
            outFile.write("  </channel>\n");
        }
        
                
        tvInfo data;
        //data = (tvInfo) list.removeFirst();
        System.out.println("\nTotal number of programs: " + programList.size());
        while(programList.size()>0){
            data = programList.removeFirst();
            if(data.Program!=null){
                //Find channel TimeZone - Used for modifying the timezone on the individual channels
                String TimeZone = tZone;
                for(channel chan: channelList){//
                    if(chan.ID.equals(data.Channel)){
                        if(chan.TimeZone != null) {
                            TimeZone = chan.TimeZone;
                        }
                        break;
                    }
                }
                outFile.write("  <programme channel=\"" + data.Channel + "\" start=\"" + data.StartTime + " ");
                outFile.write(TimeZone + "\"");
                
                if(data.StopTime!=null){
                    outFile.write(" stop=\"" + data.StopTime + " ");
                    outFile.write(TimeZone + "\"");
                }
                outFile.write(">\n");

                outFile.write("    <title lang=\"dk\">" + data.Program + "</title>\n");
                
                if( (data.ProgramInfo!=null) && (data.ProgramInfo.length()>0) ){
                    //outFile.write("    <title>" + data.ProgramInfo + "</title>\n");
                    //Removed because mediaportal doesn't seem to understand sub-title
                    outFile.write("    <sub-title lang=\"dk\">" + data.ProgramInfo + "</sub-title>\n");
                
                }
                if( (data.ProgramInfoExt!=null) && (data.ProgramInfoExt.length()>0) ){
                    outFile.write("    <desc lang=\"dk\">" + data.ProgramInfoExt + "</desc>\n");
                
                }
                //Write credits
                if(data.Credits!=null){
                    outFile.write("    <credits>\n");
                    for(String name : data.Credits.Director){
                        outFile.write("      <director>" + name + "</director>\n");
                    }
                    for(actor actor : data.Credits.Actor){
                        if(actor.role==null)
                            outFile.write("      <actor>" + actor.actorName + "</actor>\n");
                        else
                            outFile.write("      <actor role=\"" + actor.role + "\">" + actor.actorName + "</actor>\n");
                    }
                    outFile.write("    </credits>\n");
                }

                String category = null;
                if( (data.GenreMain!=-1) && (data.GenreSub != -1) ){
                    String genreMainDescription = null;
                    for(GenreCode gen : genreList){
                        if(gen.mainCode == data.GenreMain){
                            if(gen.subCode == data.GenreSub){
                                category = gen.description;
                            }
                            //store the main genre for use if no sub genre is found.
                            if(gen.subCode == 0){
                                genreMainDescription = gen.description;
                            }
                        }
                    }
                    if( (category == null) && (genreMainDescription != null) ){
                        category = genreMainDescription;
                    }
                }
                if(guessEnable && (category == null)){
                    if(data.ProgramInfo!=null)
                        category = guessObj.Match(data.ProgramInfo);
                    if(category==null)
                        category = guessObj.Match(data.Program);
                    
                }
                if(category!=null)
                    outFile.write("    <category lang=\"dk\">" + htmlConvert(category) + "</category>\n");

                if(data.ProductionYear!=null){
                    outFile.write("    <date>" + data.ProductionYear + "</date>\n");
                }
                if(data.ProductionCountry!=null){
                    outFile.write("    <country lang=\"dk\">" + data.ProductionCountry + "</country>\n");
                }
                if(data.EpisodeNo >= 1){
                    if(data.EpisodeMax >= 0)
                        outFile.write("    <episode-num system=\"xmltv_ns\"> . " + (data.EpisodeNo-1) + "/" + data.EpisodeMax + " . </episode-num>\n" );
                    else
                        outFile.write("    <episode-num system=\"xmltv_ns\"> . " + (data.EpisodeNo-1) + " . </episode-num>\n" );
                }
                if (data.Video!=null) {
                    outFile.write("    <video>\n");
                    outFile.write("      <aspect>" + data.Video + "</aspect>\n");
                    outFile.write("    </video>\n");
                }
                if (data.Audio!=null) {
                    outFile.write("    <audio>\n");
                    outFile.write("      <stereo>" + data.Audio + "</stereo>\n");
                    outFile.write("    </audio>\n");
                }

                if(data.repeat){
                    outFile.write("    <previously-shown />\n");
                }
                if(data.Subtitles!=null){
                    outFile.write("    <subtitles type=\"" + data.Subtitles + "\"/>\n");
                }
                if(data.PromoUrl!=null){
                    outFile.write("    <url>" + data.PromoUrl + "</url>\n");
                }
                if(data.Rating!=null){
                    outFile.write("    <rating system=\"DRDK\">\n");
                    outFile.write("      <value>" + data.Rating + "</value>\n");
                    outFile.write("    </rating>\n");
                }

                outFile.write("  </programme>\n");
            }
        }
        
        //Write data to File and Close file
        outFile.write("</tv>\n");
        outFile.close();
        
        return 0;
    }
    
    /* Add the categories read from dr.dk to the EPG
     */
    /*
    public static void AddCategory(LinkedList<tvInfo> programList, LinkedList<genre> genreList){
        System.out.print("\nMatching categories with tv-guide ... ");
        int matchcnt =0;
        for(tvInfo Info : programList){
            for(genre Element : genreList){
                if(Element.channel.equals(Info.Channel)){
                    if(Element.time.equals(Info.StartTime)){
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
        log.std("done. Matched " + matchcnt + " of " + genreList.size());
        
    }
    */
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

