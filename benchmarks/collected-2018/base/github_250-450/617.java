// https://searchcode.com/api/result/73573238/

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
package ch.tatool.core.element.handler.score;

import java.util.ArrayList;
import java.util.List;

import ch.tatool.core.data.DoubleProperty;
import ch.tatool.core.data.IntegerProperty;
import ch.tatool.core.data.Misc;
import ch.tatool.core.data.Points;
import ch.tatool.core.element.CompoundElement;
import ch.tatool.core.element.CompoundSelector;
import ch.tatool.core.element.handler.timeout.DefaultAdaptiveTimeoutHandler;
import ch.tatool.data.Module;
import ch.tatool.data.Trial;
import ch.tatool.element.Executable;
import ch.tatool.element.Node;
import ch.tatool.exec.ExecutionContext;
import ch.tatool.exec.ExecutionOutcome;

/**
 * Adaptive Score and Level Algorithm
 * 
 * The score and level algorithm adapts itself to the performance of the user.
 * After a given interval of trials the algorithm sets a benchmark according to
 * the performance of the user. This benchmark will then be used to compare to
 * the actual performance after the next interval of trials. Given the user
 * beats his own benchmark, a level-up will be triggered and a new benchmark
 * will be set. If the user can't beat his own benchmark, he has to continue
 * trying.
 * 
 * @author Andre Locher
 */
public class AdaptiveScoreAndLevelHandler extends AbstractPointsAndLevelHandler {

	/** checkpoint interval **/
	private int benchmarkSampleSize = 20;

	/** minimum benchmark value in percent **/
	private double minBenchmark = 0.5;

	/** maximum benchmark value in percent **/
	private double maxBenchmark = 1.0;

	/** performance gain in percent **/
	private double benchmarkRaise = 0;

	/** number of retries for re-adaptation of timer */
	private int numRetriesTimer = -1;

	/** number of retries for re-adaptation of benchmark */
	private int numRetriesBenchmark = -1;

	/** benchmarking state **/
	private boolean doBenchmark = true;

	/** benchmark value **/
	private double benchmark = 0;
	
	private double benchmarkTotalPoints = 0;
	
	private double benchmarkMaxPoints = 0;

	/** counter for checkpointInterval **/
	private int counter = 0;

	/** number of retries for timer adaptation */
	private int retryTimer = 0;

	/** number of retries for benchmark adaptation */
	private int retryBenchmark = 0;

	public static final String PROPERTY_BENCHMARK = "benchmark";
	public static final String PROPERTY_BENCHMARK_COUNTER = "benchmarkCounter";
	public static final String PROPERTY_PERFORMANCE = "performance";
	public static final String PROPERTY_TOTALPOINTS = "benchmarkTotalPoints";
	public static final String PROPERTY_MAXPOINTS = "benchmarkMaxPoints";

	private static DoubleProperty benchmarkProperty = new DoubleProperty(
			PROPERTY_BENCHMARK);
	private static IntegerProperty counterProperty = new IntegerProperty(
			PROPERTY_BENCHMARK_COUNTER);
	private static DoubleProperty performanceProperty = new DoubleProperty(
			PROPERTY_PERFORMANCE);
	private static DoubleProperty benchmarkTotalPointsProperty = new DoubleProperty(
			PROPERTY_TOTALPOINTS);
	private static DoubleProperty benchmarkMaxPointsProperty = new DoubleProperty(
			PROPERTY_MAXPOINTS);

	public static final int RESET = 2;
	public static final int INCREASE = 1;
	public static final int REDUCE = -1;
	public static final int ADAPT = 0;

	private double totalScore = 0;
	private double maxScore = 0;

	private List<DefaultAdaptiveTimeoutHandler> timeoutHandlers = new ArrayList<DefaultAdaptiveTimeoutHandler>();

	public AdaptiveScoreAndLevelHandler() {
		super("adaptive-score-and-level-handler");
	}

	/**
	 * Initializes the algorithm with the values of the DB at session start
	 */
	protected void initializeHandler(ExecutionContext context) {
		Module module = context.getExecutionData().getModule();
		benchmarkTotalPoints = benchmarkTotalPointsProperty.getValue(module, this, 0.0);
		benchmarkMaxPoints = benchmarkMaxPointsProperty.getValue(module, this, 0.0);
		
		totalScore = benchmarkTotalPoints;
		maxScore = benchmarkMaxPoints;
	}
	
	/**
	 * Initializes the algorithm with the values of the DB.
	 */
	public void initializeAlgorithm(ExecutionContext event) {
		Module module = event.getExecutionData().getModule();
		counter = counterProperty.getValue(module, this, 0);
		benchmark = benchmarkProperty.getValue(module, this, 0.0);

		if (benchmark > 0) {
			doBenchmark = false; // we already have a benchmark
		} else {
			doBenchmark = true; // new benchmark needed
		}
	}

	@Override
	protected int checkLevelChange(ExecutionContext context, int currentLevel) {
		List<Trial> trials = context.getExecutionData().getTrials();
		int oldLevel = currentLevel;
		int newLevel = oldLevel;

		Executable executable = context.getActiveExecutable();

		// loop through all trials
		for (int i = 0; i < trials.size(); i++) {
			Trial trial = trials.get(i);

			totalScore += Points.getPointsProperty().getValue(trial, trial.getParentId(), 0);
			maxScore += Points.getMaxPointsProperty().getValue(trial, trial.getParentId(), 0);

			if (trials.isEmpty()) return currentLevel;

			String trialOutcome = Misc.getOutcomeProperty().getValue(trial, executable);

			// only do calculation if trial is finished and trial is complete
			if (trialOutcome.equals(ExecutionOutcome.FINISHED) && isCompoundDone(context, trial)) {

				initializeAlgorithm(context);

				// increase benchmarkCounter
				counter++;

				// set the benchmark properties to the trial
				benchmarkProperty.setValue(trial, this, benchmark);
				counterProperty.setValue(trial, this, counter);

				if (counter == benchmarkSampleSize) {
					// do benchmark
					if (doBenchmark) {
						double performance = (totalScore / maxScore)
								+ benchmarkRaise;
	
						if (performance >= minBenchmark) {
							setBenchmark(context);
						} else {
							boolean reset = retryBenchmark(context);
							if (reset && oldLevel > 1) {
								newLevel = changeLevel(context, oldLevel, -1);
							}
						}

						// compare benchmark
					} else {
						double performance = totalScore / maxScore;

						if (performance >= benchmark) {
							newLevel = changeLevel(context, oldLevel, 1);
						} else {
							boolean reset = retryBenchmark(context);
							if (reset && oldLevel > 1) {
								newLevel = changeLevel(context, oldLevel, -1);
							}
						}
						performanceProperty.setValue(trial, this, performance);
					}
				}

				// save the counter to the DB
				counterProperty.setValue(
						context.getExecutionData().getModule(), this, counter);

			}
			// save the current points to the DB
			benchmarkTotalPointsProperty.setValue(context.getExecutionData().getModule(), this, totalScore);
			benchmarkMaxPointsProperty.setValue(context.getExecutionData().getModule(), this, maxScore);
		}

		return newLevel;
	}

	/**
	 * Sets a new individual benchmark according to the performance.
	 */
	private void setBenchmark(ExecutionContext context) {
		// set a new benchmark
		benchmark = (totalScore / maxScore) + benchmarkRaise;

		// make sure the benchmark isn't bigger than maxBenchmark
		if (benchmark > maxBenchmark) {
			benchmark = maxBenchmark;
		}
		benchmarkProperty.setValue(context.getExecutionData().getModule(),
				this, benchmark);
		doBenchmark = false; // don't make another benchmark
		adaptTimer(context, ADAPT);

		// reset algorithm parameters
		counter = 0;
		retryTimer = 0;
		retryBenchmark = 0;
		totalScore = 0;
		maxScore = 0;
	}

	/**
	 * Handles the retries at trying to beat the benchmark.
	 * 
	 * @param context
	 * @return whether the retry has lead to a reset
	 */
	private boolean retryBenchmark(ExecutionContext context) {
		boolean retryReset = false;
		retryTimer++;
		retryBenchmark++;
		// re-adapt the timer if retry count set
		if (retryTimer >= numRetriesTimer && numRetriesTimer > 0) {
			adaptTimer(context, INCREASE);
			retryTimer = 0;
		}

		// re-adapt the benchmark if retry count set
		if (retryBenchmark >= numRetriesBenchmark && numRetriesBenchmark > 0) {
			benchmark = 0;
			doBenchmark = true;
			retryBenchmark = 0;
			benchmarkProperty.setValue(context.getExecutionData().getModule(),
					this, 0.0);
			retryReset = true;
		}

		// reset algorithm parameters
		counter = 0;
		totalScore = 0;
		maxScore = 0;

		return retryReset;
	}

	/**
	 * Changes level and adapts all parameters of the adaptive algorithm.
	 * 
	 * @return the new level
	 */
	private int changeLevel(ExecutionContext event, int oldLevel, int addition) {
		int newLevel;

		// level up and reset benchmark
		newLevel = oldLevel + addition;

		// reset algorithm parameters
		benchmarkProperty.setValue(event.getExecutionData().getModule(), this, 0.0);
		benchmark = 0;
		counter = 0;
		retryTimer = 0;
		retryBenchmark = 0;
		doBenchmark = true;
		totalScore = 0;
		maxScore = 0;

		// reset the timer
		adaptTimer(event, RESET);

		return newLevel;
	}

	/**
	 * Checks whether the current trial is complete. The algorithm only gets
	 * triggered if a compound element is finished
	 * 
	 * @return whether the trial is complete
	 */
	private boolean isCompoundDone(ExecutionContext context, Trial trial) {

        Node currElement = context.getActiveElement();
        
        boolean isDone = true;
        while (this.getParent() != null
                && this.getParent() != currElement) {

            // CompoundElement
            if (currElement instanceof CompoundElement) {
                CompoundElement comp = (CompoundElement) currElement;

                for (Object handler : comp.getHandlers()) {
                    if (handler instanceof CompoundSelector) {
                        CompoundSelector selector = (CompoundSelector) handler;
                        isDone = selector.isDone();
                    }
                }

            }
            if (currElement.getParent() != null) {
                currElement = currElement.getParent();
            } else {
                return true;
            }

        }

        return isDone;
    }

	/**
	 * Get the Adaptive Timeout Handlers of a element.
	 */
	/*
	 * private void getTimeoutHandlers(Node unit) { if (unit instanceof
	 * AspectableUnit) { for (Node aspect : ((AspectableUnit)
	 * unit).getAspects()) { if (aspect instanceof
	 * DefaultAdaptiveTimeoutHandler) {
	 * timeoutHandlers.add((DefaultAdaptiveTimeoutHandler) aspect); } } }
	 * collectedHandlers = true; }
	 */

	/**
	 * Controls the adaptive timer according to the performance of the user.
	 */
	private void adaptTimer(ExecutionContext event, int mode) {
		for (int i = 0; i < timeoutHandlers.size(); i++) {
			DefaultAdaptiveTimeoutHandler timeoutHandler = timeoutHandlers
					.get(i);
			switch (mode) {
			case INCREASE:
				timeoutHandler.increaseTimeoutDuration(event);
				break;
			case REDUCE:
				timeoutHandler.decreaseTimeoutDuration(event);
				break;
			case ADAPT:
				timeoutHandler.adaptTimeoutDuration(event);
				break;
			case RESET:
				if (timeoutHandler.isResetTimerDuration()) {
					timeoutHandler.resetTimeoutDuration(event);
				}
				break;
			default:
				break;
			}
		}
	}

	public int getBenchmarkSampleSize() {
		return benchmarkSampleSize;
	}

	public void setBenchmarkSampleSize(int benchmarkSampleSize) {
		this.benchmarkSampleSize = benchmarkSampleSize;
	}

	public double getBenchmarkRaise() {
		return benchmarkRaise;
	}

	public void setBenchmarkRaise(double benchmarkRaise) {
		this.benchmarkRaise = benchmarkRaise;
	}

	public double getMinBenchmark() {
		return minBenchmark;
	}

	public void setMinBenchmark(double minBenchmark) {
		this.minBenchmark = minBenchmark;
	}

	public double getMaxBenchmark() {
		return maxBenchmark;
	}

	public void setMaxBenchmark(double maxBenchmark) {
		this.maxBenchmark = maxBenchmark;
	}

	public int getNumRetriesTimer() {
		return numRetriesTimer;
	}

	public void setNumRetriesTimer(int numRetriesTimer) {
		this.numRetriesTimer = numRetriesTimer;
	}

	public int getNumRetriesBenchmark() {
		return numRetriesBenchmark;
	}

	public void setNumRetriesBenchmark(int numRetriesBenchmark) {
		this.numRetriesBenchmark = numRetriesBenchmark;
	}

}

