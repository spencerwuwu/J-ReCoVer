// https://searchcode.com/api/result/73573135/

/*******************************************************************************
 * Copyright (c) 2011 Michael Ruflin, Andre Locher, Claudia von Bastian.
 * 
 * This file is part of Tatool.
 * 
 * Tatool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published 
 * by the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * Tatool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Tatool. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package ch.tatool.core.element.handler.timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.tatool.data.Module;
import ch.tatool.data.Trial;
import ch.tatool.exec.ExecutionContext;

/**
 * Timeout handler that individually adapts itself to the user by reducing and
 * increasing the timeout duration. The interval of adaptation is given by the
 * minDuration and maxDuration values. A level change resets the timeout
 * duration.
 * 
 * @author Andre Locher
 */
public class DefaultAdaptiveTimeoutHandler extends DefaultVisualTimeoutHandler
		implements AdaptiveTimeoutHandler {

	Logger logger = LoggerFactory
			.getLogger(DefaultAdaptiveTimeoutHandler.class);

	/** Number of samples used to calculate the new timeout. */
	private int timerSampleSize = 5;

	/** Minimum timer duration in milliseconds */
	private long minTimerDuration = 200;

	/** Maximum timer duration in milliseconds */
	private long maxTimerDuration = 3000;

	/** Percentile which will be used to calculate the new timer duration. */
	private double percentile = 0.75;

	/** Factor by which to reduce/increase the average timeout. */
	private float factor = 0.9f;

	/** Reset the duration at level-change */
	private boolean resetTimerDuration = true;

	public DefaultAdaptiveTimeoutHandler() {
		super();
	}
	
    public void processExecutionPhase(ExecutionContext event) {
    	super.processExecutionPhase(event);
    	switch (event.getPhase()) {
    	case SESSION_START:
    		initialize(event);
    		break;
    	}
    }

	/** Initializes this aspect. */
	private void initialize(ExecutionContext event) {
		Module module = event.getExecutionData().getModule();
		// read the last timeout data from the module aspect properties, use
		// default value if not provided
		long timeoutDuration = getDefaultTimerDuration();
		timeoutDuration = durationProperty.getValue(module, this, timeoutDuration);
		setDefaultTimerDuration(timeoutDuration);
	}

	/**
	 * Individually adapts the timer.
	 */
	public void adaptTimeoutDuration(ExecutionContext event) {
		long newDuration = 0;
		newDuration = getSamplesDuration(event);
		if (newDuration > maxTimerDuration) {
			increaseTimeoutDuration(event);
		} else if (newDuration < minTimerDuration) {
			decreaseTimeoutDuration(event);
		} else {
			durationProperty.setValue(event.getExecutionData().getModule(), this, newDuration);
			setDefaultTimerDuration(newDuration);
		}
	}

	/**
	 * Gets the reaction time of the n last trials (n=maxSamples) and calculates
	 * the 3rd quartile
	 * 
	 * @return the 3rd quartile of the n last trials reaction times
	 */
	public long getSamplesDuration(ExecutionContext event) {

		// find the trials that contain data about this handler
		List<Trial> trials = event.getDataService().getTrials(event.getExecutionData().getModule(), null,
				getParent(), timerSampleSize);

		// find the n-percentile reaction time - use the current duration by
		// default
		long newDuration = getDefaultTimerDuration();
		List<Long> timeList = new ArrayList<Long>();
		long reactionTimeSum = 0;

		if (trials.size() > 0) {
			int samples = 0;

			// set the samples
			if (trials.size() < timerSampleSize) {
				samples = trials.size();
			} else {
				samples = timerSampleSize;
			}

			for (int i = 0; i < samples - 1; i++) {
				Trial t = trials.get(trials.size() - 1 - i);
				Long duration = durationProperty.getValue(t, this);
				if (duration != null) {
					// use reactionTime or timeout if no reactionTime is present
					//Long duration = durationProperty.getValue(t, this);
					Long reactionTime = reactionTimeProperty.getValue(t, this);
					if (reactionTime == null) {
						reactionTime = duration;
					}
					if (reactionTime <= 0) {
						reactionTime = duration;
					}
					reactionTimeSum += reactionTime;
					timeList.add(reactionTime);
				}
			}
		}

		// add the reaction time of the current trial
		long timeout = getDefaultTimerDuration();
		long reactionTime = getReactionTime();
		if (reactionTime <= 0) {
			reactionTime = timeout;
		}
		reactionTimeSum += Long.valueOf(reactionTime);
		timeList.add(Long.valueOf(reactionTime));

		// calculate the n-percentile reaction time
		Collections.sort(timeList);
		Long[] list = new Long[timeList.size()];
		newDuration = (long) getInterpolatedValue(timeList.toArray(list),
				percentile);

		return newDuration;
	}

	/**
	 * Sets the duration of the timer to maxDuration
	 */
	public void resetTimeoutDuration(ExecutionContext event) {
		durationProperty.setValue(event.getExecutionData().getModule(), this, maxTimerDuration);
		setDefaultTimerDuration(maxTimerDuration);
	}

	/**
	 * Reduces the timer by the factor f with a minimum of minDuration
	 */
	public void decreaseTimeoutDuration(ExecutionContext event) {
		Module module = event.getExecutionData().getModule();
		long newDuration = 0;
		newDuration = (long) (((float) getDefaultTimerDuration()) * factor);
		if (newDuration < minTimerDuration) {
			// set the minimal duration
			durationProperty.setValue(module, this, minTimerDuration);
			setDefaultTimerDuration(minTimerDuration);
		} else {
			// reduce the duration
			durationProperty.setValue(module, this, newDuration);
			setDefaultTimerDuration(newDuration);
		}
	}

	/**
	 * Increases the timer by the factor f with a maximum of maxDuration
	 */
	public void increaseTimeoutDuration(ExecutionContext event) {
		Module module = event.getExecutionData().getModule();
		long newDuration = 0;
		newDuration = (long) (((float) getDefaultTimerDuration()) / factor);
		if (newDuration > maxTimerDuration) {
			// set the maximal duration
			durationProperty.setValue(module, this, maxTimerDuration);
			setDefaultTimerDuration(maxTimerDuration);
		} else {
			// increase the duration
			durationProperty.setValue(module, this, newDuration);
			setDefaultTimerDuration(newDuration);
		}
	}

	/**
	 * Calculates the percentile of an array of longs.
	 * 
	 * @param m
	 *            has to be an ordered list
	 * @return the value
	 */
	public double getInterpolatedValue(Long[] m, double percentile) {
		double value = 0;
		if (m.length > 1) {
			if (percentile >= 1) {
				percentile = 0.99;
			} else if (percentile <= 0) {
				percentile = 0.01;
			}
			// get the indices that are used for this percentile
			double index = (double) m.length * percentile;
			int lowerIndex = (int) Math.floor((m.length * percentile));
			int upperIndex = (int) Math.ceil((m.length * percentile));
			double fraction = index - (double) lowerIndex;

			// linear interpolation
			value = m[lowerIndex - 1]
					+ (fraction * (m[upperIndex - 1] - m[lowerIndex - 1]));

		} else if (m.length == 1) {
			value = m[0];
		} else {
			value = 0;
		}
		return value;
	}

	/**
	 * TODO: NOT USED ANYMORE?!
	 */
	public double getPercentile(Long[] m, double percentile) {
		if (m.length == 1) {
			return m[0]; // return the only element
		} else if (m.length == 2) {
			return ((m[0] + m[1]) / 2.0); // return the average
		} else if (m.length == 3) {
			return m[1]; // return the middle element
		} else {
			// get the subscript of the percentile element
			int middle = (int) Math.floor((m.length * percentile));
			if (m.length % 2 == 1) {
				// Odd number -- return the middle one.
				return m[middle - 1];
			} else {
				// Even number of elements -- return average of middle two
				// Must cast the numbers to double before dividing.
				return ((m[middle - 1] + m[middle]) / 2.0);
			}
		}
	}

	public int getTimerSampleSize() {
		return timerSampleSize;
	}

	public void setTimerSampleSize(int timerSampleSize) {
		this.timerSampleSize = timerSampleSize;
	}

	public double getPercentile() {
		return percentile;
	}

	public void setPercentile(double percentile) {
		this.percentile = percentile;
	}

	public float getFactor() {
		return factor;
	}

	public void setFactor(float factor) {
		this.factor = factor;
	}

	public long getMinTimerDuration() {
		return minTimerDuration;
	}

	public void setMinTimerDuration(long minTimerDuration) {
		this.minTimerDuration = minTimerDuration;
	}

	public long getMaxTimerDuration() {
		return maxTimerDuration;
	}

	public void setMaxTimerDuration(long maxTimerDuration) {
		this.maxTimerDuration = maxTimerDuration;
	}

	public boolean isResetTimerDuration() {
		return resetTimerDuration;
	}

	public void setResetTimerDuration(boolean resetTimerDuration) {
		this.resetTimerDuration = resetTimerDuration;
	}
}

