// https://searchcode.com/api/result/123690914/

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

package com.bhpBilliton.viewer2d.dataAdapter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.StringTokenizer;

import com.bhpBilliton.viewer2d.Bhp2DviewerConstants;
import com.bhpBilliton.viewer2d.BhpPropertyManager;
import com.bhpBilliton.viewer2d.util.ErrorDialog;
import com.bhpBilliton.viewer2d.util.TextBuffer;

import com.gwsys.seismic.io.SeismicMetaData;
import com.gwsys.seismic.util.TraceSampleFactory;

import com.bhpb.qiworkbench.QiWorkbenchMsg;
import com.bhpb.qiworkbench.api.IMessagingManager;
import com.bhpb.qiworkbench.api.IQiWorkbenchMsg;
import com.bhpb.qiworkbench.compAPI.MsgUtils;
import com.bhpb.qiworkbench.compAPI.QIWConstants;

/**
 * Title:        BHP Viewer <br><br>
 * Description:  This class retrieves the remote trace data with bhpio and put
 *               it in a <code>{@link SeismicDataCache}</code>.
 *               It also has a method named saveData, which is used to
 *               save the event data.
 *               In the run method of the thread, it first reads the
 *               seismic file's metadata. If this succeeds, it then
 *               reads the seismic traces, If there is
 *               a request in the SeismicDataCache, indicating the reader
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
 * @author Gil Hansen
 * @version 1.1
 */

public class SeismicDataLoader extends Thread {
    private static final Logger logger = Logger.getLogger(SeismicDataLoader.class.getName());
    public static final int TRACE_HEADER_SIZE = 240;

    private SeismicDataCache _cache;
    private String _urlString;
    private String _queryString;
    private String _formatFileName;
    private String _sampleKey;

    private int _chunkSize;
    private boolean[] _loaded;

    private int _traceSize;

    private int _firstToLoad;
    private boolean _mustDie;
    private boolean _finished;

    private SeismicMetaData _metaData;
    private double _timeStart;
    private double _timeEnd;
    private int _startSample;

    private BhpPropertyManager _pmanager;

    /**
     * Constructs a new loader.
     * @param cache the place where trace data is stored.
     */
    public SeismicDataLoader(SeismicDataCache cache, String url, String par,
                             String formatfn, String sampleKey, int chunkSize, BhpPropertyManager pmanager) {
        _cache = cache;
        _chunkSize = chunkSize;
        _urlString = url;
        _queryString = par;
        _formatFileName = formatfn;
        _sampleKey = sampleKey;

        _firstToLoad = 0;
        _mustDie = false;
        _finished = false;

        _timeStart = 0;
        _timeEnd = 0;
        _metaData = new SeismicMetaData();
        _metaData.setDataStatistics(Float.MIN_VALUE, Float.MAX_VALUE, 0.0, 0.2);

        _pmanager = pmanager;
    }

    /**
     * Informs the thread to stop. <br>
     * This method is called when the action is cancelled.
     */
    public void setMustDie() {
        _mustDie = true;
        _cache.loaderBeingStopped();
    }


    /**
     * The run method of the thread.
     */
    public void run() {

        boolean iniResult = getDataAttributes();
        boolean iniResult2 = initiateLoading();

        if (iniResult == false) {
            // Need to handle this thing better
            ErrorDialog.showErrorDialog(_pmanager.getViewer(), QIWConstants.ERROR_DIALOG,Thread.currentThread().getStackTrace(), "Error reading data for viewer", new String[] {"Possible permissions problem"}, new String[] {"Fix any permissions errors", "If permissions are OK, contact workbench support"});
            _cache.setMetaInfoError();
        } else if (iniResult2 == false) {
            ErrorDialog.showErrorDialog(_pmanager.getViewer(), QIWConstants.ERROR_DIALOG,Thread.currentThread().getStackTrace(), "Error loading data for viewer", new String[] {"Possible permissions problem"}, new String[] {"Fix any permissions errors", "If permissions are OK, contact workbench support"});
            _cache.setMetaInfoError();
        } else {
            _cache.setMetaInfo(_metaData, _timeStart, _timeEnd);
        }

        Integer key;
        int numberOfTraces = _metaData.getNumberOfTraces();

        while(!_finished && !_mustDie && !isInterrupted()) {
            try {
                key = _cache.getRequiredKey();

                if (key == null) {

                    while( _firstToLoad < numberOfTraces && _loaded[ _firstToLoad ] == true) {
                        _firstToLoad++;
                    }

                    if (_firstToLoad >= numberOfTraces) {
                        _cache.notifyDataReady();
                        break;
                    }

                    int numToLoad = getNumberOfTraceToLoad(_firstToLoad);

                    getSeismicData( _firstToLoad, numToLoad );
                    _cache.notifyDataReady();
                } else {
                    int numToLoad = getNumberOfTraceToLoad(key.intValue());
                    getSeismicData( key.intValue(), numToLoad );
                    _cache.notifyDataReady();
                }

                if (_firstToLoad >= numberOfTraces) _finished = true;
            } catch (OutOfMemoryError oome) {
                ErrorDialog.showErrorDialog(_pmanager.getViewer(), QIWConstants.ERROR_DIALOG,Thread.currentThread().getStackTrace(), "Viewer has run out of memory", new String[] {"Too many open qiComponents"}, new String[] {"Reduce the number of open qiComponents or increase Java's available memory.", "Contact workbench support for help"});
                _cache.outOfMemoryError();
                _finished = true;
            }
        }

        finishLoading();
    }

    IMessagingManager messagingMgr;

    /**
     * Gets the remote seismic file's metadata.
     * @return true if the action is successful; otherwise, false.
     */
    private boolean getDataAttributes() {
        messagingMgr = _pmanager.getViewer().getViewerAgent().getMessagingManager();

        try {
            IQiWorkbenchMsg response = null;

            // form parameters for remote SEGY read command
            ArrayList<String> params = new ArrayList<String>();
            // [0] IO preference
            params.add(QIWConstants.REMOTE_PREF);
            // [1] read request
            params.add("readReq=startRead");
            // [2] query string
            params.add("query="+_queryString);
            // [3] format filename
            params.add("formatFilename="+_formatFileName);
            // [4] sample key
            params.add("sampleKey="+_sampleKey);

            String msgID = messagingMgr.sendRequest(QIWConstants.CMD_MSG, QIWConstants.READ_SEGY_DATA_CMD,
                     QIWConstants.ARRAYLIST_TYPE, params, true);
            // wait for the response.
            int k = 0;
            while (response == null) {
                response = messagingMgr.getMatchingResponseWait(msgID, Bhp2DviewerConstants.MINUTE_WAIT_TIME);
                k++;
                if (k >= 4) break;
            }

            try {
                if (MsgUtils.isResponseAbnormal(response)) {
                  //TODO: Warning dialog: notify user could not read SEGY data
                  logger.finest("SEGY read data error:"+(String)MsgUtils.getMsgContent(response));
                  return false;
                }
            } catch (NullPointerException npe) {
                StackTraceElement[] stack = npe.getStackTrace();
                StringBuffer sbuf = new StringBuffer();
                for (int i=0; i<stack.length; i++)
                    sbuf.append(stack[i].toString() + "\n");
                logger.severe("SYSTEM ERROR: Timed out waiting for response to read SEGY data. stack trace: \n"+sbuf.toString());
                return false;
            }

            TextBuffer txtbuf = new TextBuffer((ByteBuffer)response.getContent());

            // first line is the remote file name, last line, the useful summary data
            String line = txtbuf.readLine();
            //if (line == null || line.length() == 0) {
            if (line == null) {
                logger.finest("SeismicDataLoader error : first line is null");
                return false;
            } else if (line.indexOf("BHPVIEWERERROR") != -1) {
                if (Bhp2DviewerConstants.DEBUG_PRINT > 0)
                    logger.finest("SeismicDataLoader : " + line);
                while (line != null) {
                    line = txtbuf.readLine();
                    logger.finest("    " + line);
                }
                return false;
            } else {
                String content = null;
                while (line != null) {
                    content = line;
                    if (Bhp2DviewerConstants.DEBUG_PRINT > 0)
                        logger.finest("SeismicDataLoader file : " + content);
                    line = txtbuf.readLine();
                }

                if (_mustDie) {
                    finishLoading();
                    return false;
                }

                // last line is the summary of the data: _min, _max, ...
                calculateStatistics(content);
                return true;
            }
        } catch (Exception ex) {
            logger.finest("SeismicDataLoader.getDataAttributes Exception: "+ex.getMessage());
            return false;
        }
    }

    /**
     * Implementation of this method should open the file
     * and prepare for the reading.
     * @return true if the action is successful, false otherwise.
     */
    private boolean initiateLoading() {
        int numberOfTraces = _metaData.getNumberOfTraces();
        _loaded = new boolean[numberOfTraces];
        for (int i=0; i<numberOfTraces; i++)
            _loaded[i] = false;
        return true;
    }

    /**
     * Read the SEGY file
     * and put the trace data into the <code>{@link BhpSeismicDataCache}</code>.
     * @param start the start position for reading, in trace number.
     * @param num the number of traces to be read.
     * @return true if the action is successful, false otherwise.
     */
    private boolean getSeismicData(int start, int num) {
        if (num == 0) return true;
        byte[] data = _getSeismicData(start, num);
        return addToCache(start, num, data);
    }

    /**
     * Cleanup when reading the remote sesismic file is finished. <br>
     */
    private void finishLoading() {
    }

    /**
     * Adds the data to <code>{@link SeismicDataCache}</code>.
     * This method will convert the binary data of the sample values
     * to a float array and put it into the cache.
     * @param start the start trace number.
     * @param num the number of traces that will be added.
     * @param data binary data for the traces.
     * @return true if valid data is put into the cache. False otherwise.
     */
    private boolean addToCache(int start, int num, byte[] data) {
        return  addToCache( start, num, data, 0 );
    }

    /**
     * Adds the data to <code>{@link SeismicDataCache}</code>.
     * This method will convert the binary data of the sample values
     * to a float array and put it into the cache.
     * @param start the start trace number.
     * @param num the number of traces that will be added.
     * @param data binary data for the traces.
     * @param startSample the start sample for every trace
     * @return true if valid data is put into the cache. False otherwise.
     */
    private boolean addToCache(int start, int num, byte[] data, int startSample ) {
        if (data == null) {

            for (int i=0; i<num; i++) {
                _cache.put( new Integer(start+i), new TraceObject(_pmanager.getViewer(), null, null) );
                _loaded[start+i] = true;
            }
            return false;
        }
        int readOffset = 0;
        int samplesPerTrace = _metaData.getSamplesPerTrace();

        for (int i=0; i<num; i++) {

            readOffset = i * _traceSize;

            byte[] bheader = new byte[ TRACE_HEADER_SIZE ];
            byte[] bsample = new byte[ _traceSize - TRACE_HEADER_SIZE ];

            System.arraycopy(data, readOffset, bheader, 0, bheader.length);
            readOffset = readOffset + TRACE_HEADER_SIZE;
            System.arraycopy(data, readOffset, bsample, 0, bsample.length);

            float[] dataArray = new float[ samplesPerTrace ];
            TraceSampleFactory.toFloatArray(
                    _metaData.getDataFormat(),bsample, dataArray, samplesPerTrace);

            _cache.put( new Integer( start+i ), new TraceObject(_pmanager.getViewer(), dataArray, bheader));

            _loaded[ start+i ] = true;

        }
        return true;
    }

    private byte[] _getSeismicData(int start, int num) {
        messagingMgr = _pmanager.getViewer().getViewerAgent().getMessagingManager();

        byte[] data = null;
        try {
            int traceSize = TRACE_HEADER_SIZE + _metaData.getSamplesPerTrace() * 4;

            IQiWorkbenchMsg response = null;

            // form parameters for remote SEGY read command
            ArrayList<String> params = new ArrayList<String>();
            // [0] IO preference
            params.add(QIWConstants.REMOTE_PREF);
            // [1] read request
            params.add("readReq=readTrace");
            // [2] sample key
            params.add("sampleKey="+_sampleKey);
            // [3] query string
            params.add("query="+_queryString);
            // [4] start
            params.add("start="+start);
            // [5] num
            params.add("num="+num);
            // [6] start sample
            params.add("startSample="+_startSample);
            // [7] samples per trace
            params.add("samplesPerTrace="+_metaData.getSamplesPerTrace());

            String msgID = messagingMgr.sendRequest(QIWConstants.CMD_MSG, QIWConstants.READ_SEGY_DATA_CMD,
                     QIWConstants.ARRAYLIST_TYPE, params, true);
            // wait for the response.
            int k = 0;
            while (response == null) {
                response = messagingMgr.getMatchingResponseWait(msgID, Bhp2DviewerConstants.MINUTE_WAIT_TIME);
                k++;
                if (k >=4) break;
            }

            try {
                if (MsgUtils.isResponseAbnormal(response)) {
                  //TODO: Warning dialog: notify user could not read SEGY data
                  logger.finest("SEGY read data error:"+(String)MsgUtils.getMsgContent(response));
                  return data;
                }
            } catch (NullPointerException npe) {
                StackTraceElement[] stack = npe.getStackTrace();
                StringBuffer sbuf = new StringBuffer();
                for (int i=0; i<stack.length; i++)
                    sbuf.append(stack[i].toString() + "\n");
                logger.severe("SYSTEM ERROR: Timed out waiting for response to read SEGY data. stack trace: \n"+sbuf.toString());
                return data;
            }

            ByteBuffer bytebuf = (ByteBuffer)response.getContent();

//            data = new byte[num * traceSize];
            data = new byte[bytebuf.position()];
            //get the bytes behind the ByteBuffer
            bytebuf.flip();
            bytebuf.get(data);
        } catch (Exception ex) {
            ErrorDialog.showErrorDialog(_pmanager.getViewer(), QIWConstants.ERROR_DIALOG,Thread.currentThread().getStackTrace(), "Error reading SEGY data for viewer", new String[] {"Possible permissions problem"}, new String[] {"Fix any permissions errors", "If permissions are OK, contact workbench support"});
        }

        return data;
    }

    /**
     * Calculate the seismic metadata. <br>
     * In the summary, there are 9 numbers separated by space.
     * The format of the output is like this: <br>
     * mininum-amplitude(float) maximum-amplitude(float) avg(double) rms(double)
     * samples-per-trace(int ) total-number-of-traces(int) sample-unit(int)
     * sample-rate(double) sample-start(double)
     */
    private void calculateStatistics(String content) {
        if (content==null || content.length()==0) return;
        if(Bhp2DviewerConstants.DEBUG_PRINT > 0)
            logger.finest("int DataLoader summary content **" + content + "**");
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

            if (count == 0) {
                try { min = Float.parseFloat(token); }
                catch (Exception ex) {
                    logger.finest("SeismicDataLoader.calculateStatistics Exception:");
                    logger.finest("    on _min, use default value -1.0.");
                    min = -1.0f;
                }
            }
            else if (count == 1) {
                try { max = Float.parseFloat(token); }
                catch (Exception ex) {
                    logger.finest("SeismicDataLoader.calculateStatistics Exception:");
                    logger.finest("    on _max, use default value 1.0.");
                    max = 1.0f;
                }
            }
            else if (count == 2) {
                try { avg = Double.parseDouble(token); }
                catch (Exception ex) {
                    logger.finest("SeismicDataLoader.calculateStatistics Exception:");
                    logger.finest("    on _avg, use default value 0.0.");
                    avg = 0.0;
                }
            }
            else if (count == 3) {
                try { rms = Double.parseDouble(token); }
                catch (Exception ex) {
                    logger.finest("SeismicDataLoader.calculateStatistics Exception:");
                    logger.finest("    on _rms, use default value 0.8.");
                    rms = 0.8;
                }
            }
            else if (count == 4) {
                try { samplesPerTrace = Integer.parseInt(token); }
                catch (Exception ex) {
                    logger.finest("SeismicDataLoader.calculateStatistics Exception:");
                    logger.finest("    on _samplesPerTrace, use default value 2001.");
                    samplesPerTrace = 2001;
                }
            }
            else if (count == 5) {
                try { numberOfTraces = Integer.parseInt(token); }
                catch (Exception ex) {
                    logger.finest("SeismicDataLoader.calculateStatistics Exception:");
                    logger.finest("    on _numberOfTraces, use default value 0.");
                    numberOfTraces = 0;
                }
            }
            else if (count == 6) {
                try { sampleUnit = Integer.parseInt(token); }
                catch (Exception ex) {
                    logger.finest("SeismicDataLoader.calculateStatistics Exception:");
                    logger.finest("    on _sampleUnit, use default value 0.");
                    sampleUnit = 0;
                }
            }
            else if (count == 7) {
                try { sampleRate = Double.parseDouble(token); }
                catch (Exception ex) {
                    logger.finest("SeismicDataLoader.calculateStatistics Exception:");
                    logger.finest("    on _sampleRate, use default value 0.004.");
                    sampleRate = 0.004;
                }
            }
            else if (count == 8) {
                try { sampleStart = Double.parseDouble(token); }
                catch (Exception ex) {
                    logger.finest("SeismicDataLoader.calculateStatistics Exception:");
                    logger.finest("    on _sampleStart, use default value 0.0.");
                    sampleStart = 0.0;
                }
            }
            else if (count == 9) {
                try { _timeStart = Double.parseDouble(token); }
                catch (Exception ex) {
                    logger.finest("SeismicDataLoader.calculateStatistics Exception:");
                    logger.finest("    on time start, use default value 0.0.");
                    _timeStart = sampleStart;
                }
            }
            else if (count == 10) {
                try { _timeEnd = Double.parseDouble(token); }
                catch (Exception ex) {
                    logger.finest("SeismicDataLoader.calculateStatistics Exception:");
                    logger.finest("    on time end, use default value 0.0.");
                    _timeEnd = _timeStart + sampleRate*samplesPerTrace;
                }
            }
            else break;
            count++;
        }

        _traceSize = TRACE_HEADER_SIZE + samplesPerTrace * 4;
        if (samplesPerTrace==1) _chunkSize = numberOfTraces;
        _metaData.setDataStatistics(min, max, avg, rms);
        _metaData.setNumberOfTraces(numberOfTraces);
        _metaData.setSamplesPerTrace(samplesPerTrace);
        _metaData.setSampleUnits(sampleUnit);
        _metaData.setSampleRate(sampleRate);
        _metaData.setSampleStart(sampleStart);
/*
        int sampleRangeMax = _metaData.getSamplesPerTrace()-1;
        int sampleRangeMin = 0;

        double num_samples = ( _timeEnd > _timeStart ) ?
            (_timeEnd -_timeStart) / _metaData.getSampleRate()
            : _metaData.getSamplesPerTrace();
        _startSample = (int)( _timeStart / _metaData.getSampleRate());
        if( _startSample < 0 ) _startSample = 0;
        if( _startSample+num_samples > _metaData.getSamplesPerTrace() ) {
            num_samples = _metaData.getSamplesPerTrace()-_startSample;
            if( num_samples < 0 ) num_samples = 0;
        }
        int realmax = sampleRangeMax +_startSample;
        if(realmax > _metaData.getSamplesPerTrace())
            realmax = _metaData.getSamplesPerTrace()-1;
        int realmin = sampleRangeMin + _startSample;
        if( min > max ) min = max;

        _metaData.setSamplesPerTrace(samplesPerTrace);
        _metaData.setSampleStart(_timeStart);
*/
    }

    // Determin amount of traces to download
    private int getNumberOfTraceToLoad(int start) {
        int numToLoad = 0;
        int numberOfTraces = _metaData.getNumberOfTraces();
        for (int i=start; i<numberOfTraces; i++) {
            if (_loaded[i]) break;
            if (i-start >= _chunkSize) break;
            numToLoad++;
        }
        return numToLoad;
    }

    private void setTraceSize( int traceSize ) {
        _traceSize = traceSize;
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

