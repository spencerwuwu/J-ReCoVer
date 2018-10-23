// https://searchcode.com/api/result/1115890/

package cc.creativecomputing.sound.audio;

import cc.creativecomputing.io.CCIOUtil;

import com.softsynth.jsyn.LineOut;
import com.softsynth.jsyn.Synth;
import com.softsynth.jsyn.SynthSample;
import com.softsynth.jsyn.SynthSampleAIFF;
import com.softsynth.jsyn.SynthSampleWAV;

public class CCSoundIO {
	
	public static float DEFAULT_FRAME_RATE = (float)Synth.DEFAULT_FRAME_RATE;
	
	
	
	////////////////////////////////////////////////////////////
	//
	// LOADING SAMPLES
	//
	////////////////////////////////////////////////////////////
	
	private static CCSamplerPool _mySamplerPool;
	
	public static CCSample loadSample(final String theFileName) {
		// Use the correct sample class depending on the file type, "*.aiff" or "*.wav".
		SynthSample mySample = null;

		switch (SynthSample.getFileType(theFileName)) {
		case SynthSample.AIFF:
			mySample = new SynthSampleAIFF();
			break;
		case SynthSample.WAV:
			mySample = new SynthSampleWAV();
			break;
		default:
			throw new CCSoundException("Unrecognized sample file suffix.");
		}

		/* Load sample from a file. */
		try {
			mySample.load(CCIOUtil.openStream(theFileName));
		} catch (Exception e) {
			throw new CCSoundException("Problems loading Sample file:"+theFileName,e);
		}
		
		CCSample myCCSample = new CCSample(mySample,_mySamplerPool);
		return myCCSample;
	}
	
	////////////////////////////////////////////////////////////
	//
	// START STOP ENGINE
	//
	////////////////////////////////////////////////////////////
	
	public static LineOut lineOut;
	
	/**
	 * Before making any other calls to CCSoundIO, you must initialize it by calling:
	 * <pre>CCSoundIO.start();</pre>
	 * The method start() is static so you do not have to create a CCSoundIO object.
	 * You can specify an optional frame rate as a second parameter.  By specifying a 
	 * low sample rate, ideally 1/2 or 1/4 of the default rate, you can reduce the number 
	 * of samples that must be calculated per second. This will reduce the amount of 
	 * computation that the CPU must perform. For example:
	 * <pre>CCSoundIO.startEngine( 0, CCSoundIO.DEFAULT_FRAME_RATE / 2.0 );</pre>
	 */
	public static void start() {
		Synth.startEngine(0);
		lineOut = new LineOut();
		lineOut.start();
		_mySamplerPool = new CCSamplerPool(100);
	}
	
	/**
	 * 
	 * @param theFrameRate theFrameRate CCSoundIO runs
	 */
	public static void start(final float theFrameRate) {
		Synth.startEngine(0, theFrameRate);
	}
	
	/**
	 * When your program finishes, you must terminate CCSoundIO by calling:
	 * CCSoundIO.stop();
	 */
	public static void stop() {
		lineOut.stop();
		Synth.stopEngine();
	}
}

