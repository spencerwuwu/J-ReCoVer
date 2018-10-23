// https://searchcode.com/api/result/123690939/

/*
bhpViewer - a 2D seismic viewer
This program module Copyright (C) Interactive Network Technologies 2006

The following is important information about the license which accompanies this copy of
the bhpViewer in either source code or executable versions (hereinafter the "Software").
The Software, which is owned by Interactive Network Technologies, Inc. ("INT")
is distributed pursuant to the terms of the GNU GPL v 2.0 ("GPL"), which can be found at
http://www.gnu.org/licenses/gpl.txt.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
PURPOSE.  See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program;
if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
For a license to use the Software under conditions other than in a way compliant with the
GPL or the additional restrictions set forth in this license agreement or to purchase
support for the Software, please contact: Interactive Network Technologies, Inc.,
2901 Wilcrest, Suite 100, Houston, Texas 77042 <sales@int.com>

All licensees should note the following:

*        In order to compile and/or modify the source code versions of the Software,
a user may require one or more of INT's proprietary toolkits or libraries.
Although all modifications or derivative works based on the source code are governed by the
GPL, such toolkits are proprietary to INT, and a library license is required to make use of
such toolkits when making modifications or derivatives of the Software.
More information about obtaining such a license can be obtained by contacting sales@int.com.

*        Under agreement with INT, BHP Billiton Petroleum (Americas) Inc. or its designee
serves as the custodian ("Custodian") of the Software.  Licensees are encouraged to
submit modified versions of the Software to the Custodian at www.qiWorkbench.org.
This will allow the Custodian to consider integrating such revised versions into the
version of the Software it distributes. Doing so will foster future innovation
and overall development of the bhpViewer platform.  Notwithstanding the foregoing,
the Custodian is under no obligation to include modifications or revisions in future
distributions.

This program module may have been modified by BHP Billiton Petroleum,
G&W Systems Consulting Corp or other third parties, and such portions are licensed
under the GPL.
To contact BHP Billiton about this software you can e-mail info@qiworkbench.org or
visit http://qiworkbench.org to learn more.
*/

package com.bhpBilliton.viewer2d.dataAdapter.bhpsu;

import java.util.*;

import com.bhpBilliton.viewer2d.Bhp2DviewerConstants;
import com.bhpBilliton.viewer2d.util.ErrorDialog;
import com.bhpb.qiworkbench.compAPI.QIWConstants;
import com.gwsys.seismic.io.SeismicMetaData;
import java.math.BigDecimal;

/**
 * Title:        BHP Viewer <br><br>
 * Description:  This class retrieves the trace data with bhpio and put it in a
 *               <code>{@link BhpSeismicDataCache}</code>.
 *               It also has a method named saveData, which is used to
 *               save the event data.
 *               Two subclass of this class are for application and applet
 *               respectively. In the run method of the thread, it
 *               firstly runs the bhpread command to generate the temporary
 *               data file and summrize the data. If this succeed, it
 *               continues to open that temporary file and gets prepared for
 *               reading. If both succeed, it starts reading. If there is
 *               a request in the BhpSeismicDataCache, indicating the reader
 *               needs a specific trace, this loader will read a segment of
 *               the file which has the data for that trace. Otherwise, it
 *               will try to get the traces sequentially, until all the traces
 *               are read. Each time it reads
 *               the file, it will read a segment of number of traces, that
 *               number is equal to or less than the value of _chunkSize.
 *               <br><br>
 *
 * Copyright:    Copyright (c) 2001 <br>
 * Company:      BHP INT <br>
 * @author Synthia Kong
 * @version 1.0
 */

public abstract class BhpSeismicDataLoader extends Thread {

    protected BhpSeismicDataCache _cache;
    // url
    protected String _parameter1;
    // bhpread query string "bhpread pathlist=asdf filename=asdf [pathlist=asdf] key=..."
    protected String _parameter2;
    protected String _outputFile;
    protected int _chunkSize;
    protected boolean[] _loaded;

    protected int _traceSize;

    private int _firstToLoad;
    protected boolean _mustDie;
    private boolean _finished;

    protected SeismicMetaData _metaData;

    /**
     * Constructs a new loader.
     * @param cache the place where trace data is stored.
     * @param parameter1 the bhpio command with path, or servlet URL
     * @param parameter2 bhpio query string.
     * @param outputFile name of the temporary file used to store the bhpread output.
     * @param chunkSize the maximum number of traces that will be read at one time.
     */
    public BhpSeismicDataLoader(BhpSeismicDataCache cache,
                                String parameter1, String parameter2,
                                String outputFile, int chunkSize) {
        _cache = cache;
        _parameter1 = parameter1;
        _parameter2 = parameter2;
        _chunkSize = chunkSize;

        _outputFile = outputFile; // at creation, it is the tmp directory location
                                  // in getDataAttributes, it changes to the temporary file name
        _firstToLoad = 0;
        _mustDie = false;
        _finished = false;

        _metaData = new SeismicMetaData();
        _metaData.setDataStatistics(Float.MIN_VALUE, Float.MAX_VALUE, 0.0, 0.2);

    }

    /**
     * Informs the thread to stop. <br>
     * This method is called when the action is cancelled.
     */
    public void setMustDie() {
        synchronized(_cache.getCacheForSync()) {
            _mustDie = true;
            _cache.loaderBeingStopped();
        }
    }

    /**
     * Save the data back. <br>
     * Currently, the only implementation of this method is to
     * save the event data back via bhpio. This saving is a two
     * step process. Firstly, the data is written back to the
     * temporary file that generated when running bhpread. Then
     * bhpwrite is called to update the disk data with the
     * temporary file as the input. This method is called from
     * <code>{@link BhpEventSegyReader}</code>.saveEvent(...).
     * @param name name of the temporary file.
     * @param values the data that will be saved.
     *        The length of the array should be the same as the
     *        total number of traces.
     * @param index the vertical sample index of the data.
     *        For event data, since there is only one event in
     *        the temporary file, it is always 0.
     */
    public abstract void saveData(String name, double[] values, int index) throws Exception;

    /**
     * The run method of the tread. <br>
     * The method will <br>
     * 1. run bhpread to generated the temporary data file and summary,
     *    calling getDataAttributes(). <br>
     * 2. open the temporary file and prepare for the reading,
     *    calling initiateLoading(). <br>
     * 3. read the file and put trace data into
     *    <code>{@link BhpSeismicDataCache}</code>. <br>
     * 4. when all the traces are read, or the flag is set
     *    for the thread to stop, quit reading and call finishLoading
     *    for clean up. <br>
     */
    public void run() {
        boolean iniResult = getDataAttributes();
        if (!iniResult ) {
			//try a 2nd time
			iniResult = getDataAttributes();
			if (!iniResult ) {
				_cache.setMetaInfoError(_outputFile);

				// Need to handle this thing better
				ErrorDialog.showErrorDialog(_cache.getBhpViewerBase(), QIWConstants.ERROR_DIALOG,Thread.currentThread().getStackTrace(),
                                        "Error reading data for viewer",
                                        new String[] {"Reading a large number of traces", "Possible permissions problem"},
                                        new String[] {"Read fewer traces by limiting the range(s)", "Fix any permissions errors",
                                                    "If permissions are OK, contact workbench support"});
			}
        }

        boolean iniResult2 = initiateLoading();
        if (iniResult && !iniResult2 ) {
            _cache.setMetaInfoError(_outputFile);

            ErrorDialog.showErrorDialog(_cache.getBhpViewerBase(), QIWConstants.ERROR_DIALOG,Thread.currentThread().getStackTrace(),
                                        "Error loading data for viewer",
                                        new String[] {"Possible permissions problem"},
                                        new String[] {"Fix any permissions errors",
                                                    "If permissions are OK, contact workbench support"});
        }

        if (iniResult && iniResult2)
        {
            if(Bhp2DviewerConstants.DEBUG_PRINT > 0)
                System.out.println("BhpSeismicDataLoader : setMetaInfo "+_cache);
            _cache.setMetaInfo(_metaData, _outputFile);
        }

        Integer key;
        int numberOfTraces = _metaData.getNumberOfTraces();
        while(!_finished && !_mustDie && !isInterrupted()) {
            try {
                key = _cache.getRequiredKey();
                if (key == null) {
                   while(_firstToLoad < numberOfTraces && _loaded[_firstToLoad] == true) {
                        _firstToLoad++;
                    }
                    if (_firstToLoad >= numberOfTraces) {
                        break;
                    }
                    int numToLoad = getNumberOfTraceToLoad(_firstToLoad);
                    if (numToLoad > 0)
                        getSeismicData(_firstToLoad, numToLoad);
                    else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ie) {
                            System.out.println("While waiting for numToLoad != 0, caught: " + ie.getMessage());
                        }
                    }
                } else {
                    int numToLoad = getNumberOfTraceToLoad(key.intValue());
                    if (numToLoad > 0)
                        getSeismicData(key.intValue(), numToLoad);
                    else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ie) {
                            System.out.println("While waiting for numToLoad != 0, caught: " + ie.getMessage());
                        }
                    }
                }
                if (_firstToLoad >= numberOfTraces) {
                    _finished = true;
                }
            } catch(OutOfMemoryError ofmError) {
                ErrorDialog.showErrorDialog(_cache.getBhpViewerBase(), QIWConstants.ERROR_DIALOG,Thread.currentThread().getStackTrace(),
                                            "Viewer has run out of memory",
                                            new String[] {"Too many open programs"},
                                            new String[] {"Reduce number of open programs",
                                                          "Contact workbench support for help"});
                _cache.outOfMemoryError();
                _finished = true;
            }
        }

        finishLoading();
        if(Bhp2DviewerConstants.DEBUG_PRINT > 0)
            System.out.println("BhpSeismicDataLoader : thread finish loading!!!");
    }

    /**
     * Implementation of this method should get the meta data initiated
     * and temporary file generated.
     * @retrun true if the action is successful, false otherwise.
     */
    protected abstract boolean getDataAttributes();
    /**
     * Implementation of this method should open the file
     * and prepare for the reading.
     * @retrun true if the action is successful, false otherwise.
     */
    protected abstract boolean initiateLoading();
    /**
     * Implementation of this method should read the file
     * and put the trace data into the <code>{@link BhpSeismicDataCache}</code>.
     * @param start the start position for reading, in trace number.
     * @param num the number of traces to be read.
     */
    protected abstract boolean getSeismicData(int start, int num);
    /**
     * This method is called when the reading is finished. <br>
     * Implementation of the method should do the necessary clean
     * up job, such as clost the file.
     * The temporary file is deleted when the corresponding layer is deleted.
     */
    protected abstract void finishLoading();

    /**
     * Adds the data to <code>{@link BhpSeismicDataCache}</code>.
     * This method will convert the binary data of the sample values
     * to a float array and put it into the cache.
     * @param start the start trace number.
     * @param num the number of traces that will be added.
     * @param data binary data for the traces.
     * @return true if valid data is put into the cache. False otherwise.
     */
    protected boolean addToCache(int start, int num, byte[] data) {
        synchronized(_cache.getCacheForSync()) {
            if (data == null) {
                for (int i=0; i<num; i++) {
                    _cache.put(new Integer(start+i), new BhpTraceObject(_cache.getBhpViewerBase(), null, null));
                    _loaded[start+i] = true;
                }
                return false;
            }
            int readOffset = 0;
            int samplesPerTrace = _metaData.getSamplesPerTrace();
            for (int i=0; i<num; i++) {
                readOffset = i * _traceSize;
                byte[] bheader = new byte[BhpSegyReader.TRACE_HEADER_SIZE];
                System.arraycopy(data, readOffset, bheader, 0, bheader.length);
                readOffset = readOffset + BhpSegyReader.TRACE_HEADER_SIZE;
                float[] dataArray = new float[samplesPerTrace];
                byteConvertToFloat(data, dataArray, readOffset, samplesPerTrace);
                
                //_cache.put(new Integer(start+i), new BhpTraceObject(dataArray, meta));
                _cache.put(new Integer(start+i), new BhpTraceObject(_cache.getBhpViewerBase(), dataArray, bheader));
                _loaded[start+i] = true;
                
            }
            return true;
        }
    }

    /**
     * Calculates the seismic meta data from the output of bhpread summary. <br>
     * In the summary, there are 9 numbers separated by space.
     * The format of the output is like this: <br>
     * mininum-amplitude(float) maximum-amplitude(float) avg(double) rms(double)
     * samples-per-trace(int ) total-number-of-traces(int) sample-unit(int)
     * sample-rate(double) sample-start(double)
     */
    protected void calculateStatistics(String content) {

        if (content==null || content.length()==0) {
            return;
        }
        if(Bhp2DviewerConstants.DEBUG_PRINT > 0)
            System.out.println("BhpSeismicDataLoader summary content **" + content + "**");
        StringTokenizer stk = new StringTokenizer(content);
        int count = 0;
        String token;
        double min = _metaData.getMinimumAmplitude();
        double max = _metaData.getMaximumAmplitude();
        double avg = _metaData.getAverage();
        double rms = _metaData.getRMS();
        int numberOfTraces = _metaData.getNumberOfTraces();
        int samplesPerTrace = _metaData.getSamplesPerTrace();
        double sampleRate = _metaData.getSampleRate();
        int sampleUnit = _metaData.getSampleUnits();
        double sampleStart = _metaData.getSampleStart();
        while(stk.hasMoreTokens()) {
            token = stk.nextToken();
            //System.out.println(count + "tt  " + token);
            if (count == 0) {
                try { min = Float.parseFloat(token); }
                catch (Exception ex) {
                    System.out.println("LocalBhpSeismicDataLoader.calculateStatistics Exception:");
                    System.out.println("    on _min, use default value -1.0.");
                    min = -1.0f;
                }
            }
            else if (count == 1) {
                try { max = Float.parseFloat(token); }
                catch (Exception ex) {
                    System.out.println("LocalBhpSeismicDataLoader.calculateStatistics Exception:");
                    System.out.println("    on _max, use default value 1.0.");
                    max = 1.0f;
                }
            }
            else if (count == 2) {
                try { avg = Double.parseDouble(token); }
                catch (Exception ex) {
                    System.out.println("LocalBhpSeismicDataLoader.calculateStatistics Exception:");
                    System.out.println("    on _avg, use default value 0.0.");
                    avg = 0.0;
                }
            }
            else if (count == 3) {
                try { rms = Double.parseDouble(token); }
                catch (Exception ex) {
                    System.out.println("LocalBhpSeismicDataLoader.calculateStatistics Exception:");
                    System.out.println("    on _rms, use default value 0.8.");
                    rms = 0.8;
                }
            }
            else if (count == 4) {
                try { samplesPerTrace = Integer.parseInt(token); }
                catch (Exception ex) {
                    System.out.println("LocalBhpSeismicDataLoader.calculateStatistics Exception:");
                    System.out.println("    on _samplesPerTrace, use default value 2001.");
                    samplesPerTrace = 2001;
                }
            }
            else if (count == 5) {
                try { numberOfTraces = Integer.parseInt(token); }
                catch (Exception ex) {
                    System.out.println("LocalBhpSeismicDataLoader.calculateStatistics Exception:");
                    System.out.println("    on _numberOfTraces, use default value 0.");
                    numberOfTraces = 0;
                }
            }
            else if (count == 6) {
                try { sampleUnit = Integer.parseInt(token); }
                catch (Exception ex) {
                    System.out.println("LocalBhpSeismicDataLoader.calculateStatistics Exception:");
                    System.out.println("    on _sampleUnit, use default value 0.");
                    sampleUnit = 0;
                }
            }
            else if (count == 7) {
                try { sampleRate = Double.parseDouble(token); }
                catch (Exception ex) {
                    System.out.println("LocalBhpSeismicDataLoader.calculateStatistics Exception:");
                    System.out.println("    on _sampleRate, use default value 0.004.");
                    sampleRate = 0.004;
                }
            }
            else if (count == 8) {
                try { sampleStart = Double.parseDouble(token); }
                catch (Exception ex) {
                    System.out.println("LocalBhpSeismicDataLoader.calculateStatistics Exception:");
                    System.out.println("    on _sampleStart, use default value 0.0.");
                    sampleStart = 0.0;
                }
            }
            else {
                break;
            }
            count++;
        }

        _traceSize = BhpSegyReader.TRACE_HEADER_SIZE + samplesPerTrace * 4;
        if (samplesPerTrace==1) {
            _chunkSize = numberOfTraces;
        }
        _metaData.setDataStatistics(min, max, avg, rms);
        _metaData.setNumberOfTraces(numberOfTraces);
        _metaData.setSamplesPerTrace(samplesPerTrace);
        _metaData.setSampleUnits(sampleUnit);
        _metaData.setSampleRate(sampleRate);
        _metaData.setSampleStart(sampleStart);
    }

    // Determin amount of traces to download
    /*
    private int getNumberOfTraceToLoad(int start) {
        int numToLoad = 0;
        int numberOfTraces = _metaData.getNumberOfTraces();
        for (int i=start; i<numberOfTraces; i++) {
            if (_loaded[i]) {
                break;
            }
            if (i-start >= _chunkSize) {
                break;
            }
            numToLoad++;
        }
        return numToLoad;
    }*/
        // Determin amount of traces to download
    private int getNumberOfTraceToLoad(int start) {
        /*
        int numberOfTraces = _metaData.getNumberOfTraces();
        double MAX_CHUNK_PERCENT = 0.15;
        int MAX_CHUNK_SIZE = 20;
        
        //new heuristic - default chunk size is MAX_CHUNK_PERCENT of total trace set
        //some data sets have very few traces, attempt to read at least 20 if < 100 traces total
        int potentialChunkSize=20;

        if (numberOfTraces >= 300)
            //try to load 15% of the total number of traces if > 100 traces total
            potentialChunkSize = new BigDecimal(Math.round(numberOfTraces * MAX_CHUNK_PERCENT)).intValue();

        potentialChunkSize = Math.min(potentialChunkSize, numberOfTraces - start);

        int numToLoad=0;
        for (int i=start; i<potentialChunkSize; i++) {
            if (_loaded[i]) {
                break;
            }
            if (i-start >= MAX_CHUNK_SIZE) {
                break;
            }
            numToLoad++;
        }
        System.out.println("Calculated chunk size: " + numToLoad);
        return numToLoad;
        */
        int numToLoad = 0;
        int numberOfTraces = _metaData.getNumberOfTraces();
        for (int i=start; i<numberOfTraces; i++) {
            if (_loaded[i]) {
                break;
            }
            if (i-start >= _chunkSize) {
                break;
            }
            numToLoad++;
        }
        return numToLoad;
    }

    private void byteConvertToFloat( byte[] source, float[] dest, int startOffset, int numElements) {
        int i, j, input_value;

        for ( i = 0, j = startOffset; i < numElements; i++, j += 4) {
            // Assembles the input value.
            input_value = (( (source[j]     & 255) << 24) |
                            ((source[j + 1] & 255) << 16) |
                            ((source[j + 2] & 255) << 8) |
                            ( source[j + 3] & 255));

            dest[i] = Float.intBitsToFloat(input_value);
        }
    }
}

