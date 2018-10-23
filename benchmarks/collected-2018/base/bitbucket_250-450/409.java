// https://searchcode.com/api/result/54727741/

/*
 * Created on Nov 30, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package javazoom.audio;

import java.util.ArrayList;

import javax.sound.sampled.SourceDataLine;


/**
 * @author Kris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class KJDigitalSignalProcessingAudioDataConsumer implements KJAudioDataConsumer {

	private static final int DEFAULT_SAMPLE_SIZE = 2048;
	private static final int DEFAULT_FPS = 70;
	
	private Object readWriteLock = new Object();
	
	private SourceDataLine sourceDataLine;

	private int  sampleSize;
	
	private long fpsAsNS;
	private long desiredFpsAsNS;
	
	private byte[]  audioDataBuffer;

	private float[] left;
	private float[] right;
	
	private int     position;
	private long    offset;
	
	private SignalProcessor signalProcessor;
	
	private ArrayList dsps = new ArrayList();
	
	/**
	 * Default constructor creates a DSPAC with DEFAULT_SAMPLE_SIZE and DEFAULT_FPS as
	 * parameters.
	 */
	public KJDigitalSignalProcessingAudioDataConsumer() {
		this( DEFAULT_SAMPLE_SIZE, DEFAULT_FPS );
	}
	
	/**
	 * @param pSampleSize The sample size to extract from audio data sent to the SourceDataLine.
	 * @param pFramePerSecond The desired refresh rate per second of registered DSP's.  
	 */
	public KJDigitalSignalProcessingAudioDataConsumer( int pSampleSize, int pFramesPerSecond ) { 
	
		sampleSize = pSampleSize;
		desiredFpsAsNS = 1000000000L / (long)pFramesPerSecond;
		fpsAsNS        = desiredFpsAsNS;	
		
	}
	
	/**
	 * Adds a DSP to the DSPAC and forwards any audio data to it at the specific
	 * frame rate.
	 * 
	 * @param A class implementing the KJDigitalSignalProcessor interface. 
	 */
	@SuppressWarnings("unchecked")
	public void add( KJDigitalSignalProcessor pSignalProcessor ) {
		dsps.add( pSignalProcessor );
	}

	/**
	 * Removes the specified DSP from this DSPAC if it exists.
	 * 
	 * @param A class implementing the KJDigitalSignalProcessor interface. 
	 */
	public void remove( KJDigitalSignalProcessor pSignalProcessor ) {
		dsps.remove( pSignalProcessor );
	}
	
	/**
	 * Start monitoring the specified SourceDataLine.
	 * 
	 * @param pSdl A SourceDataLine.
	 */
	public synchronized void start( SourceDataLine pSdl ) {
		
		// -- Stop processing previous source data line.
		if ( signalProcessor != null ) {
			stop();
		}
		
		if ( signalProcessor == null ) {
		
//			System.out.println( "ADBS: " + pSdl.getBufferSize() );
			
			sourceDataLine = pSdl;
			
			// -- Allocate double the memory than the SDL to prevent
			//    buffer overlapping.
			audioDataBuffer = new byte[ pSdl.getBufferSize() << 1 ];
			
			left  = new float[ sampleSize ];			
			right = new float[ sampleSize ];			

			position = 0;
			offset = 0;
			
			signalProcessor = new SignalProcessor();
			
			new Thread( signalProcessor ).start();
			
		}
		
	}
	
	/**
	 * Stop monitoring the currect SourceDataLine.
	 */
	public synchronized void stop() {
		
		if ( signalProcessor != null ) {
			
			signalProcessor.stop();
			signalProcessor = null;
			
			audioDataBuffer = null;
			sourceDataLine = null;
			
		}
		
	}
	
	private void storeAudioData( byte[] pAudioData, int pOffset, int pLength ) {
		
		synchronized( readWriteLock ) {
		
			if ( audioDataBuffer == null ) {
				return;
			}
			
			int wOverrun = 0;
			
			if ( position + pLength > audioDataBuffer.length - 1 ) {
				
				wOverrun = ( position + pLength ) - audioDataBuffer.length;
				pLength = audioDataBuffer.length - position;
				
			}
			
			System.arraycopy( pAudioData, pOffset, audioDataBuffer, position, pLength );
			
			if ( wOverrun > 0 ) {
				
				System.arraycopy( pAudioData, pOffset + pLength, audioDataBuffer, 0, wOverrun );
				position = wOverrun;
				
			} else {
				position += pLength;
			}
			
		}
		
	}
	
	/* (non-Javadoc)
	 * @see kj.audio.KJAudioDataConsumer#writeAudioData(byte[])
	 */
	public void writeAudioData( byte[] pAudioData ) {
		storeAudioData( pAudioData, 0, pAudioData.length );
	}

	/* (non-Javadoc)
	 * @see kj.audio.KJAudioDataConsumer#writeAudioData(byte[], int, int)
	 */
	public void writeAudioData( byte[] pAudioData, int pOffset, int pLength ) {
		storeAudioData( pAudioData, pOffset, pLength );
	}
	
	private class SignalProcessor implements Runnable {
		
		boolean process = true;
		
		long lfp = 0;
		
		int frameSize;
		
		public SignalProcessor() {
			frameSize = sourceDataLine.getFormat().getFrameSize();
		}
		
		private int calculateSamplePosition() {
			
			synchronized( readWriteLock ) {
				
				long wFp = sourceDataLine.getLongFramePosition();
				long wNfp = lfp;
				
				lfp = wFp;
				
				int wSdp = (int)( (long)( wNfp * frameSize ) - (long)( audioDataBuffer.length * offset ) );
				
				return wSdp;
				
			}
			
		}
		
		public void run() {
			
			while( process ) {
				
				long wStn = System.nanoTime();
				
				int wSdp = calculateSamplePosition();
				
				if ( wSdp > 0 ) {
				
					int c = wSdp;
					
					for( int a = 0; a < sampleSize; a++, c += 4 ) { 
					
						if ( c >= audioDataBuffer.length ) {
							offset++; 
							c = ( c - audioDataBuffer.length );
						}
						
						// -- Separate channels and normalize ( 16-bit stereo ).
                        int length = audioDataBuffer.length;
                        if (c + 1 >= length) {
                            c -= 5;
                        } else if (c + 2 >= length) {
                            c -= 4;
                        } else if (c + 3 >= length) {
                            c -= 3;
                        } else if (c + 4 >= length) {
                            c -= 2;
                        }
						left[ a ]  = (float)( ( (int)audioDataBuffer[ c + 1 ] << 8 ) + audioDataBuffer[ c ] ) / 32767.0f;
						right[ a ] = (float)( ( (int)audioDataBuffer[ c + 3 ] << 8 ) + audioDataBuffer[ c + 2 ] ) / 32767.0f; 
						
					}
					
				} 
				
				// -- Dispatch sample data to digtal signal processors.
				for( int a = 0; a < dsps.size(); a++ ) {
					
					// -- Calculate the frame rate ratio hint. This value can be used by 
					//    animated DSP's to fast forward animation frames to make up for
					//    inconsistencies with the frame rate.
					float wFrr = (float)fpsAsNS / (float)desiredFpsAsNS; 
					
					try {
						( (KJDigitalSignalProcessor)dsps.get( a ) ).process( left, right, wFrr );
					} catch( Exception pEx ) {
						System.err.println( "-- DSP Exception: " );
						pEx.printStackTrace();
					}
				}
				
//				KJJukeBox.getDSPDialog().setDSPInformation( 
//					String.valueOf( 1000.0f / ( (float)( wEtn - wStn ) / 1000000.0f ) ) ); 
				
//				System.out.println( 1000.0f / ( (float)( wEtn - wStn ) / 1000000.0f ) );
				
				long wDelay = fpsAsNS - ( System.nanoTime() - wStn );
				
				// -- No DSP registered? Put the the DSP thread to sleep. 
				if ( dsps.isEmpty() ) {
					wDelay = 1000000000; // -- 1 second.
				}  
					
				if ( wDelay > 0 ) {
				
					try {
						Thread.sleep( wDelay / 1000000, (int)wDelay % 1000000 );
					} catch ( InterruptedException pEx ) {
						// TODO Auto-generated catch block
					}

					// -- Adjust FPS until we meet the "desired FPS".
					if ( fpsAsNS > desiredFpsAsNS ) {
						fpsAsNS -= wDelay;
					} else {
						fpsAsNS = desiredFpsAsNS;
					}
					
				} else {
					
					// -- Reduce FPS because we cannot keep up with the "desired FPS".
					fpsAsNS += -wDelay;
					
					// -- Keep thread from hogging CPU.
					try {
						Thread.sleep( 10 );
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
					}
					
				}
				
			}
			
		}
		
		public void stop() {
			process = false;
		}
		
	}
	

}

