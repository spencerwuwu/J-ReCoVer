// https://searchcode.com/api/result/123317374/

/** 
    Copyright (c) 2011 University of Washington. All rights reserved. 
*/

package edu.uw.harwood.pipeline;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.Container;

public class PipeControl {
    private Map<Integer, Container> views;
    private List<String> argHeader; //loaded from config, given via helper
    private static PipeView currentView;
    private PipeArgHelper helper;
    private Thread pipelineThread;
//  private Process pipelineProcess;
//  private ProcessBuilder pipelineProcessBuilder; //used for each job run, depending on time
    private Pipeline pipeRun;
    private File saveFile;
    private File END_FILE;
    private static BlockingQueue<File> runQueue; //this isn't doing anything very special...
    //private List<Map<String, String>> runList; //list of snapshots of args
    private internalRunList runList; //list of snapshots of args
    private int runNumber;
    private boolean nameReuseAsked;
    
    public PipeControl(InputStream configFile, InputStream settingsFile) throws IOException, IllegalArgumentException {
        views = new HashMap<Integer, Container>();
        runQueue = new LinkedBlockingQueue<File>();
        helper = new PipeArgHelper(configFile, settingsFile);  
        runList = new internalRunList();
        END_FILE = new File("END_FILE");
        END_FILE.deleteOnExit();
        runNumber = 0;
        nameReuseAsked = false;
        //pipelineProcessBuilder = getProcessBuilder(); //called when settings change
    }
    
    public void registerView(int hash, Container view){
        views.put(hash, view);
    }

    //set one main window as current/only, others to pop up when needed, as this's children
    public void showView(int hash){
        if (views.containsKey(hash)) {
            for (int key : views.keySet()) {
                if (key == hash) {
                    Container c = views.get(key);
                    c.setVisible(true);
                    currentView = (PipeView)c;
                }
            }
        }
    }
    
    // makes visibility opposite of given window's current visibility
    public void popView(int hash) {
        if (views.containsKey(hash)){
            Container t = views.get(hash);
            t.setVisible(!t.isVisible());
        }
    }
 
    public PipeView getCurrentView() {
        return currentView;
    }
        
    // handles all PipeArg initilisation, loading, saving, and arg passing
    public PipeArgHelper getHelper() {
        return helper;
    }
       
    public void updateViewTitle(String title) {
        currentView.updateTitle(title);
    }


    // File Operations
    
    public boolean isSaved() {
        return isSaved(true);
    }
    
    // check to keep any changes that have been made
    public boolean isSaved(boolean showDialog) {
        boolean saved = helper.isSaved();
        if (!showDialog)
        return saved;
        if (!saved) {
            Object[] options = { "Save", "Discard", "Cancel" };
            int selection = JOptionPane.showOptionDialog(currentView, "Save configuration changes?", "Save changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (selection == JOptionPane.NO_OPTION) {
                return true; //user chooses not to save, allow new/load/etc
            } else if ( selection == JOptionPane.YES_OPTION) {
                return requestSave(); //consider carrying saved boolean into this... reduce calls on helper.isSaved()
            } // else == cancel
            return false;
        }
        return true; //saved => true
    }
   
    // handler for making a new save file with makeNew
    // gets a blanked set of PipeArgs and a file to save to
    public boolean requestNew() {
        if (isSaved()) { // makes it past the save option, will be returned true if user chooses not to save, false if cancel
            return makeNew();  //also allows these other methods to get written, and let this one implement as design changes
        }
        return false;
    }
    
    // handler for saving a file with doSave
    public boolean requestSave() {
        if (!helper.isSaved() && helper.getSaveFile() == null) {
            return doSave(true); //true for saveFileIsNull
        } //helper is not saved && savefile != null
        return helper.saveArgs(); //with current file
    }
    
    // handler for saving args to another file using doSave or requestSave
    public boolean requestSaveAs() {
        if (helper.getSaveFile() == null) {
            return requestSave();
        } return doSave(false);
    }
    
    // for saving anonymous files
    // currently used by details buttong in details window for queued sample
    public void requestSaveAs(Map<String, String> valMap) {
        File cF = PipeTools.chooseFile(false, "Save configuration", currentView, null);
        if (cF != null) {
            helper.writeArgsToFile(cF, valMap);
        }
    }
    
    // handler for loading a file
    public boolean requestLoad() {
        if (isSaved()) {
            File file = PipeTools.chooseFile(true, null,"Open configuration", currentView, null);
            if (file != null) {
                try{
                    Map<String, String> errorMap = helper.loadArgs(file);
                    //return helper.loadArgs(file).size() == 0;
                    if (errorMap.size() == 0)  {
                        updateViewTitle(helper.getSaveFile().getAbsolutePath());
                        return true;
                    } // else
                    JTextArea errorArea = new JTextArea();
                    for (Map.Entry<String, String> e: errorMap.entrySet()) {
                        errorArea.append(helper.getPipeArg((e.getKey())).getName() + " ==> " + e.getValue() + "\n");
                    }
                    //JScrollPane errorWindow = new JScrollPane(errorArea);
                    Object [] params = {"Some values were invalid or empty.\nDefault values have been loaded in their place.", new JScrollPane(errorArea)};
                    JOptionPane.showMessageDialog(currentView, params, "Invalid file values", JOptionPane.ERROR_MESSAGE);
                    
                    updateViewTitle(helper.getSaveFile().getAbsolutePath());
                    return true;
                } catch (IOException e) { // else
                    e.printStackTrace();
                    // falls through to 'fail' window and false returned
                } catch (java.text.ParseException p) {

                    Object [] params = {"Failed to load file.\nUnrecognized argument at line " + p.getErrorOffset()+"."};
                    JOptionPane.showMessageDialog(currentView, params, "Parsing Error", JOptionPane.ERROR_MESSAGE);

                    return false;
                    
                }
                JOptionPane.showMessageDialog(currentView, "Failed to load file", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } // not isSaved() meaning save attempt was attempted but unsuccessful or cancelled
        return false;
    } //end requestLoad
    
    // handler for closing main window
    // checks for running pipeline first, then saved file
    public void requestClose() {
        if (isSaved(false)) { 
            doClose();
        } else {
            Object[] options = { "Save", "Discard", "Cancel" };
            int selection = JOptionPane.showOptionDialog(currentView, "Save configuration changes before exiting?", "Exit", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            switch (selection) {
                case 0:
                    if (!requestSave()) {
                        break;  //file not chosen/saved
                    } // else do save, fall through to 1
                case 1:
                    //do exit, fall to 2
                    doClose();
                    break;
                case 2:
                case JOptionPane.CLOSED_OPTION:
                    //do nothing
                default:
                break;
            }
        } 
    } //end requestClose
    
    // handler to add sample to the queue
    // queue whether thread running or not...
    // checks for all needed and validity of arguments
    public boolean requestEnqueue() {
        //competition is not fierce..why still using blocking queue? it waits nicely...
        if (helper.isReady()) { 
            try {
                runQueue.put(helper.getOptionFile()); // this, or multi-command/flag... 
                //runQueue.put(helper.getArgumentsAsString()); // future --long-arg=val implementation
                runList.addMap(helper.getSnapshot());
                if (runQueue.size() < 2 && !currentView.getRunListView().isVisible()) {
                    popView(currentView.getRunListView().hashCode());
                }
                System.err.printf("queue: %d\n", runQueue.size());
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.err.printf("queue: %d\n", runQueue.size());
        return false;
    } // end requestEnqueue

    // if main pipeline Thread is null, request starting new thread
    // if main thread is not null, request stopping the thread
    public boolean toggleRun() {
        System.err.print("pipelineThread ==> null: ");
        System.err.println(pipelineThread == null);
        if (pipelineThread != null) {
            System.err.print("pipelineThread ==> interrupted: ");
            System.err.println(pipelineThread.isInterrupted());
            return requestStop(); // will stop P, set pipelineProcessnull
        } 
        return requestStart();
    }
    

    public boolean requestStart() {
        if (pipelineThread == null ) {
            if (runQueue.size() > 0 || requestEnqueue()) {
                getPipelineThread().start();
                if (!currentView.getLogView().isVisible()) {
                    popView(currentView.getLogView().hashCode());
                }
                return true;
            }
        }
        return false; //what is false signifying? right now, enqueue failed, or thread already running
    }

    // handler to immediately halt pipeline..
    public boolean requestStop() {
        if (pipelineThread != null) {
            int selection = JOptionPane.showConfirmDialog(currentView, "Are you sure you want to terminate the running process?", "Terminate", JOptionPane.YES_NO_OPTION);
            if (selection == JOptionPane.YES_OPTION) {
                if (pipeRun != null && !pipeRun.isProcessNull()) {
                    runList.updateStatus(runNumber, "Stopping");
                    pipeRun.stopRunning(); 
                    doSystemTerminate(); 
                } else  {
                    pipelineThread.interrupt();
                }
                pipelineThread = null;
                return true;
            } 
            return false;
        } 
        return true;
    }
    
    // pops a bung in the pipeline, exit when END_FILE is reached....
    // this is not used, as when queue is empty, pipeline ceases and is destroyed
    public void requestEnd() { //graceful stop, will cause run to end, shutdown...
        try {
            runQueue.put(END_FILE); // end-of-all-jobs stop
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // handles displaying queued samples in runList window 
    // shows current status and general information, also can be queried for 
    // certain row/sample information
    public class internalRunList {
        private List<Map <String, String> > runMapList;
        private List<String> header_str;
        private List<String> header_var;
        private List<String> status;
        
        private Map<String, String> statusMap;
        
        public internalRunList() {
            runMapList = new ArrayList<Map <String, String>>();
            header_str = new ArrayList<String>();
            header_var = new ArrayList<String>();
            header_str.add(helper.getPipeArg("--sample_name").getName());
            header_var.add("--sample_name");
            header_str.add(helper.getPipeArg("--sample_barcode").getName());
            header_var.add("--sample_barcode");
            header_str.add("Status");
            header_var.add("status");
            statusMap = new HashMap<String, String>();
            statusMap.put("DONE", "Complete");
            statusMap.put("ERROR",   "Error");
            statusMap.put("QUEUED", "Queued");
            statusMap.put("RUN",   "Running");
            statusMap.put("STOP",  "Stopped");
        }
        
        public int size() {
            return runMapList.size();
        }
        
        public void addMap(Map<String, String> argMap) {
            runMapList.add(argMap);
            runMapList.get(runMapList.size()-1).put("status","Queued");
            ((AbstractTableModel)currentView.getRunTable().getModel()).fireTableStructureChanged();
        }
        
        //place in list, status update
        public void updateStatus(int index, String stat) {
            runMapList.get(index).put("status", stat);
            ((AbstractTableModel)currentView.getRunTable().getModel()).fireTableDataChanged();
            //runMapList.get(index)["status"] = status;
        }
        
        public String getError(){ return statusMap.get("ERROR");  }
        public String getDone(){  return statusMap.get("DONE");   }
        public String getStop(){  return statusMap.get("STOP");   }
        public String getRun(){  return  statusMap.get("RUN");    }
        public String getQueued(){ return statusMap.get("QUEUED");}
        
        public List<String> getHeader() {
            return header_str;
        }
        
        public List<Map<String, String>> getRunList() {
            return runMapList;
        }
        
        public String getName(int indexRow) {
            return runMapList.get(indexRow).get("--sample_name");
        }
        
        public String getBarcode(int indexRow) {
            return runMapList.get(indexRow).get("--sample_barcode");
        }
        
        public String getStatus(int indexRow) {
            return runMapList.get(indexRow).get("status");
        }
        
        public String get(int indexRow, int indexCol) {
            return runMapList.get(indexRow).get(header_var.get(indexCol));
        }
        
        public Map<String, String> getMap(int indexRow) {
            return runMapList.get(indexRow);
        }
        
        public Map<String, String> getPrintMap(int indexRow) {
            Map<String, String> print = new HashMap<String, String>();
            Map<String, String> map = runMapList.get(indexRow);
            helper.getArgList();
            for (String s : helper.getArgList()) {
                print.put(helper.getPipeArg(s).getName(), map.get(s));
            }
            return print;
        }
        
        public List<String> getPrintList(int indexRow) {
            List<String> print = new ArrayList<String>();
            Map<String, String> map = runMapList.get(indexRow);
            helper.getArgList();
            for (String s : helper.getArgList()) {
                print.add(helper.getPipeArg(s).getName());
                print.add(map.get(s));
            }
            return print;
        }
    } //end internalRunList

    // view row of runList, all values of a queued sample
    public void viewRow(int row) {
        //currentView.argView(runList.getPrintMap(row));
        Map<String, String> saveMap = new HashMap<String, String>(runList.getMap(row));
        saveMap.remove("status");
        currentView.argView(runList.getPrintList(row), saveMap);
    }
    
    public internalRunList getRunList() {
        return runList;
    }
       
    public int getRunQueueSize() {
        return runQueue.size();
    }
    
    private Pipeline getPipeline() {
        if (pipeRun == null) {
            pipeRun = new Pipeline();
        }
        return pipeRun;
    }

    private Thread getPipelineThread() {
        if (pipelineThread == null) {
            pipelineThread = new Thread(getPipeline(), "pipelineRunner"); //params?
        }
        return pipelineThread;
    } 

    private boolean doSystemTerminate() {
        return doSystemTerminate(true);
    }

    private boolean doSystemTerminate(boolean waitForTerm) {
        List<String> killCommand = new ArrayList<String>();
        killCommand.add("env");
        killCommand.add("python2");
        killCommand.add(helper.getProcessTerminator());
        try {
            ProcessBuilder kill = getProcessBuilder(killCommand); // correct working directory
            Process pkill = kill.start();
            if (waitForTerm) {
                pkill.waitFor();
            }
            return true; 
        } catch (IOException ignore) {
            System.err.printf("ioerror, maybe kill script not found.\n");
            return false; 
        } catch (InterruptedException e) {
            System.err.printf("interrupted, maybe not all processes were killed.\n");
            return false; 
        }
    }

    // main exit function, called on menu 'exit' and window manager close button alike
    private void doClose() {
        // # running process cleanup should go here
        if (requestStop() ) {
            helper.doOnClose();  // save user preferences
            currentView.dispose();
            System.err.printf("Xpression exiting...\n");
            System.exit(0);
        }
    }
    
    private ProcessBuilder getProcessBuilder() {
        return getProcessBuilder("/bin/sh");
    }
    
    private ProcessBuilder getProcessBuilder(List<String> commandAndArgs) {
        ProcessBuilder p = new ProcessBuilder(commandAndArgs);
        p.redirectErrorStream(true);
        p.directory(helper.getTempDir());
        // System.err.printf("working dir: %s\n", pipelineProcessBuilder.directory().getAbsolutePath());
        return p;
    }
    
    private ProcessBuilder getProcessBuilder(String command) {
        return getProcessBuilder(Arrays.asList(command.split(" ")));
    }

    // creates new save file
    // PipeArgs are initialised
    // internal savefile is now new file
    private boolean makeNew() {
        File file = PipeTools.chooseFile(false, null,"New configuration file", currentView, null); //warning!! overwrite possible
        if (file != null) {
            helper.setSaveFile(file);
            helper.initArgs();
            boolean ret = helper.saveArgs();
            if (ret) {
                updateViewTitle(helper.getSaveFile().getAbsolutePath());
            }
            return ret;
        }
        return false;
    }

    // if no defined save file, create a new one
    // write values from PipeArgs to save file
    private boolean doSave(boolean saveFileIsNull) {
        File parent = (saveFileIsNull) ? new File(helper.getPipeArg("--output_dir").getValue()) : helper.getSaveFile().getParentFile();
        File cF = PipeTools.chooseFile(false, "Save configuration", currentView, parent);
        if (cF != null) {
            boolean ret = helper.saveArgs(cF);
            if (ret) {
                updateViewTitle(helper.getSaveFile().getAbsolutePath());
            }
            return ret;
        } return false;
    }

    // handled as a singleton from other PipeControl methods...
    private class Pipeline implements Runnable {
        
        private Process pipelineProcess;
        private volatile boolean keepRunning;
        
        public void run() {
            int i; 
            String output; // used for both arg input and resulting output
            File optionFile; //reference to option file supplied as argument
            // stream naming is in context of java process object to undelying process, so output is actually system process input
            BufferedReader br = null; //read from shell
            List<String> command = new ArrayList<String>();
            try {
                currentView.setRunning(true);
                keepRunning = true;
                while (keepRunning && END_FILE != (optionFile = runQueue.take())) { // assignment in conditional!
                    System.err.printf("runNumber: %d starting\n", runNumber);
                    i = -999999; // reset
                    command.clear(); 
                    command.add("env");
                    command.add("python2");
                    command.add(String.format(helper.getRunScript().getAbsolutePath()));
                    command.add("-p");
                    command.add(String.format(optionFile.getAbsolutePath()));
                    pipelineProcess = getProcess(command); // /bin/sh ready to print 
                    System.err.printf("%s %s %s %s %s\n", command.get(0), command.get(1), command.get(2), command.get(3), command.get(4));
                    runList.updateStatus(runNumber, runList.getRun()); 

                    // reads from running shell
                    br = new BufferedReader(new InputStreamReader(new BufferedInputStream(pipelineProcess.getInputStream())));
                    // for unix systems, this is simpler just to read from the same thread   
                    // implementing separate stream consumer did not work for me
                    while (keepRunning && (output = br.readLine()) != null) { // assignment in conditional!
                        // # logging should go here at least!
                        currentView.update("log", output);
                    }
                    br.close(); // close stream input, allows release
                    i = pipelineProcess.waitFor(); // should  happen very soon, stream was just null
                    System.err.printf("process exit: %d\n", i);

                    if (i != 0) { 
                        stopRunning();
                        if ("Stopping".equals(runList.getStatus(runNumber))) {
                            runList.updateStatus(runNumber, runList.getStop()); //set to Stopped
                        } else {
                            runList.updateStatus(runNumber, runList.getError());
                        }
                    } else { 
                        runList.updateStatus(runNumber, runList.getDone());
                    }

                    if (runQueue.size() < 1) { // this is why blocking queue is not needed
                        stopRunning();
                    }

                    // process has exited by now, 
                    pipelineProcess.destroy();
                    pipelineProcess = null;
                    runNumber += 1; 
                } //end runQueue.take() loop

            System.err.println("pipeRun exiting run()...");

            } catch (InterruptedException e) {
                e.printStackTrace();
                stopRunning();
                //doSystemTerminate();
                if (!runList.getDone().equals(runList.getStatus(runNumber)) || 
                    !runList.getError().equals(runList.getStatus(runNumber))) {
                    runList.updateStatus(runNumber, runList.getStop()); //set to Stopped
                }
                // go to next on list
                // not satisfied with this way of doing it, maybe isn't clear to user
                runNumber += 1;
                //pipelineProcess.destroy();
            } catch (IOException x) {
                x.printStackTrace();
            } finally { // clear variables for next time pipeline starts
                System.err.println("pipeRun exiting via 'finally'...");
                currentView.setRunning(false);
                try {
                    if (br != null) { br.close(); }
                    if (pipelineProcess != null) { pipelineProcess.destroy(); } //probably done already
                    pipelineProcess = null;
                    pipelineThread = null; 
                } catch (Exception ignore) {ignore.printStackTrace();}
            }
        } //end run()
        
        public boolean isProcessNull() {
            return pipelineProcess == null;
        }
    
        private Process getProcess(List<String> shellCommand) {
            if (pipelineProcess == null) {
                try {
                    pipelineProcess = getProcessBuilder(shellCommand).start();
                } catch (IOException e) {e.printStackTrace();}
            }
            return pipelineProcess;
        }
    
        public void stopRunning() {
            keepRunning = false;
        }

    } //end class Pipeline runnable

} //end PipeControl class

