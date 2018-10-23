// https://searchcode.com/api/result/100763012/

package com.tcmj.pm.spread.prima;

import com.tcmj.pm.spread.SpreadCalculator;
import com.tcmj.pm.spread.impl.SpreadPeriod;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;


public class SpreadCalculatorSaprimaFinal implements SpreadCalculator {

    private static final boolean CALCULATION_WITH_BIGDECIMAL = false;
    // TODO clarify if RoundingMode.HALF_EVEN is correct, or if 
    // RoundingMode.HALF_UP should be preferred instead

    /** MathContext.DECIMAL32 */
    private static final MathContext BIGDECIMAL_MATHCONTEXT = new MathContext(10, RoundingMode.HALF_EVEN);


    /**
     * Checks if all values refer to the complete timespan
     * 
     * @param pTotalValues
     *                an array of time related values to be spreaded
     * @param timespan
     *                the object representing start+finish date of the time span
     *                to be considered for the exclusions
     * @return true, if all total values refer to the complete timespan, false,
     *         if at least one value refers to another (i.e., a value specific)
     *         timespan
     */
    public static boolean allValuesReferToCompleteTimespan(
            final TimeRelatedValue[] pTotalValues, final Period timespan) {

        for (final TimeRelatedValue timeRelatedValue : pTotalValues) {

            if (timeRelatedValue.getCalcStartDate() != timespan.getCalcStartDate()
                    || timeRelatedValue.getCalcFinishDate() != timespan.getCalcFinishDate()) {

                //                System.out.println("************************************************************");
                //                System.out.println("timeRelatedValue.getCalcStartDate() = " + timeRelatedValue.getCalcStartDate());
                //                System.out.println("timespan.getCalcStartDate() = " + timespan.getCalcStartDate());
                //                System.out.println("timeRelatedValue.getCalcFinishDate() = " + timeRelatedValue.getCalcFinishDate());
                //                System.out.println("timespan.getCalcFinishDate() = " + timespan.getCalcFinishDate());
                //                System.out.println("************************************************************");

                return false;
            }
        }

        return true;

    }


    /**
     * @param pPeriod
     *                the period to intersect
     * @param pPeriodToIntersectWith
     *                the period used for interecting the pPeriod
     */
    protected static void calculateIntersectionOfPeriods(final Period pPeriod,
            final Period pPeriodToIntersectWith) {
        // Period is completely inside timespan (start - finish)
        if (pPeriod.getFinishDate() <= pPeriodToIntersectWith.getFinishDate()
                && pPeriod.getStartDate() >= pPeriodToIntersectWith.getStartDate()) {
            pPeriod.setInsideTimespan(true);
        } else { // Period is partly outside (before) timespan

            if (pPeriod.getStartDate() < pPeriodToIntersectWith.getStartDate()) {
                if (pPeriod.getFinishDate() > pPeriodToIntersectWith.getStartDate()) {
                    pPeriod.setCalcStartDate(pPeriodToIntersectWith.getStartDate());
                    pPeriod.setInsideTimespan(true);
                }
            } else if (pPeriod.getStartDate() < pPeriodToIntersectWith.getFinishDate()) {
                // Period is partly outside (after) timespan
                pPeriod.setCalcFinishDate(pPeriodToIntersectWith.getFinishDate());
                pPeriod.setInsideTimespan(true);
            }

            // Case "Period is completely outside (before or after) timespan"
            // is not considered, because default field values of period are
            // correct then (periodValues = null; insideTimespan = false) ;

        }
    }


    private static long calculateOverlapDuration(final long start1,
            final long finish1, final long start2, final long finish2) {

        // Period is completely outside (before or after) interval
        if (start1 >= finish2 || finish1 <= start2) {
            return 0;
        }

        // Start and finish of the 2 periods are put into a list and sorted.
        // Then, the distance of the 2 values in the middle of the list
        // equals the overlap.
        final long[] thelist = {finish2, start2, start1, finish1};
        Arrays.sort(thelist, 0, 4);
        return thelist[2] - thelist[1];
    }


    /**
     * Checks parameters for coherence, i.e., the data must be logically
     * coherent to each other.
     * 
     * @param pTotalValues
     *                the array of totalValues to be spread
     * @param pTimespan
     *                the total timespan valid for the complete curve
     * @return an error message, or null if coherence check is passed
     */
    public static String checkDataCoherence(
            final TimeRelatedValue[] pTotalValues, final Period pTimespan) {

        String result = null;
        // Check if all sub-timespans refering to pTotalValues are inside the pTimespan
        for (final TimeRelatedValue value : pTotalValues) {
            if (value.getCalcStartDate() < pTimespan.getCalcStartDate()
                    || value.getCalcFinishDate() > pTimespan.getCalcFinishDate()) {
                if (null == result) {
                    result = "";
                }
                result += "\nERROR:\n" + "TimeRelatedValue " + value.getValue()
                        + " with sub-timespan " + value.getCalcStartDate()
                        + " - " + value.getCalcFinishDate()
                        + " exceeds timespan of curve ("
                        + pTimespan.getCalcStartDate() + " - "
                        + pTimespan.getCalcFinishDate() + ")";
            }
        }
        return result;
    }


    private static void cleanIrrelevantPeriodParts(final Period[] pPeriods, final Period pTimespan) {
        for (final Period period : pPeriods) {
            SpreadCalculatorSaprimaFinal.calculateIntersectionOfPeriods(period, pTimespan);
        }
    }

    /**
     * saprima Spread Calculator. Spreads an array of values into given time
     * periods according to a spread curve. <br>
     * This method assumes that each value is to be spread over the complete
     * timespan (pTimespanStart,pTimespanFinish). For spreading each value over
     * an individual timespan, use same method with other signature
     * (TimeRelatedValue[] pTotalValues instead of double[] pTotalValues). <br>
     * Values are written into Periods. Therefore, the return value is void.
     * <br>
     * Values in Periods are initialized with NaN. Therefore, you can check for
     * NaN if you want to know, whether or not a Period is completely outside
     * the timespan (i.e., outside the total timespan (valid for the complete
     * curve), or outside the timespan defined in the TimeRelatedValue,
     * respectively) used for the spread.
     * <p>
     * @param pTotalValues an array of values to be spreaded
     * @param pTimespanStart the start date of the time span to be considered for all periods
     * @param pTimespanFinish the finish date of the time span to be considered for all periods
     * @param pPeriods the time periods used as spread targets
     * @param pCurve an array of values representing the curve that determines the spread logic
     * @param pExclusionPeriods the time periods to be excluded from the spread (e.g.,
     * weekends) - please note that exclusion periods may not overlap and must be sorted
     */
    public static void computeSpread(final double[] pTotalValues,
            final long pTimespanStart, final long pTimespanFinish,
            final Period[] pPeriods, final double[] pCurve,
            final Period[] pExclusionPeriods) {

        final int valueNumber = pTotalValues.length;
        final TimeRelatedValue[] timeRelatedTotalValues = new TimeRelatedValue[valueNumber];
        for (int i = 0; i < valueNumber; i++) {
            timeRelatedTotalValues[i] = new TimeRelatedValue(pTotalValues[i],
                    pTimespanStart, pTimespanFinish, i);
        }

        SpreadCalculatorSaprimaFinal.computeSpread(timeRelatedTotalValues, pTimespanStart,
                pTimespanFinish, pPeriods, pCurve, pExclusionPeriods);
    }


    /**
     * saprima Spread Calculator. Spreads an array of values into given time
     * periods according to a spread curve. <br>
     * Values are written into Periods. Therefore, the return value is void.
     * <br>
     * Values in Periods are initialized with NaN. Therefore, you can check for
     * NaN if you want to know, whether or not a Period is completely outside
     * the timespan (i.e., outside the total timespan (valid for the complete
     * curve), or outside the timespan defined in the TimeRelatedValue,
     * respectively) used for the spread.
     * 
     * @param pTotalValues
     *                an array of time related values to be spreaded
     * @param pTimespanStart
     *                the start date of the time span to be considered for the
     *                exclusions
     * @param pTimespanFinish
     *                the finish date of the time span to be considered for the
     *                exclusions
     * @param pPeriods
     *                the time periods used as spread targets
     * @param pCurve
     *                an array of values representing the curve that determines
     *                the spread logic
     * @param pExclusionPeriods
     *                the time periods to be excluded from the spread (e.g.,
     *                weekends) - please note that exclusion periods may not
     *                overlap and must be sorted
     */
    public static void computeSpread(final TimeRelatedValue[] pTotalValues,
            final long pTimespanStart, final long pTimespanFinish,
            final Period[] pPeriods, final double[] pCurve,
            final Period[] pExclusionPeriods) {

        final Period timespan = new Period(pTimespanStart, pTimespanFinish);

        // Various checks if data is coherent (i.e., does not include
        // logical errors).
        final String errorMessage = SpreadCalculatorSaprimaFinal.checkDataCoherence(pTotalValues, timespan);
        if (null != errorMessage) {
            throw new RuntimeException(errorMessage);
        }

        SpreadCalculatorSaprimaFinal.cleanIrrelevantPeriodParts(pPeriods, timespan);

        SpreadCalculatorSaprimaFinal.resetSingleValues(pPeriods, pTotalValues.length);

        SpreadCalculatorSaprimaFinal.segregateExclusions(pPeriods, timespan, pTotalValues, pExclusionPeriods);

        if (SpreadCalculatorSaprimaFinal.allValuesReferToCompleteTimespan(pTotalValues, timespan)) {
            SpreadCalculatorSaprimaFinal.computeSpreadForUniversalTimespan(pCurve, timespan, pPeriods, pTotalValues);
        } else {
            SpreadCalculatorSaprimaFinal.computeSpreadForIndividualTimespans(pCurve, timespan, pPeriods, pTotalValues);
        }

    }


    /**
     * <br>
     * <br>
     * Computes the spread for the case that there are different individual
     * (sub-)timespans defined for each value, i.e. the values are NOT being
     * spread over the same (complete) timespan.
     * 
     * @param pTotalValues
     *                an array of time related values to be spreaded
     * @param pTimespan
     *                the object representing start+finish date of the time span
     *                to be considered for the exclusions
     * @param pPeriods
     *                the time periods used as spread targets
     * @param pCurve
     *                an array of values representing the curve that determines
     *                the spread logic
     */
    private static void computeSpreadForIndividualTimespans(
            final double[] pCurve, final Period pTimespan,
            final Period[] pPeriods, final TimeRelatedValue[] pTotalValues) {

        // Step 1: Calculate curve intervals

        final int intervalCount = pCurve.length;
        final Interval[] intervals = new Interval[intervalCount];
        double intervalLength;
        if (SpreadCalculatorSaprimaFinal.CALCULATION_WITH_BIGDECIMAL) {
            final BigDecimal bdCalcDuration = BigDecimal.valueOf(pTimespan.getCalcDuration());
            final BigDecimal bdIntervalCount = BigDecimal.valueOf(intervalCount);
            final BigDecimal quotient = bdCalcDuration.divide(bdIntervalCount, SpreadCalculatorSaprimaFinal.BIGDECIMAL_MATHCONTEXT);
            intervalLength = quotient.doubleValue();
        } else {
            intervalLength = pTimespan.getCalcDuration() / intervalCount;
        }
        // Create intervals (sub-timespans) for each fragment of the curve
        for (int a = 0; a < intervalCount; a++) {
            //TODO Rundung reparieren - wird momentan durch cast abgeschnitten
            final long intervalStartDate = (long) (pTimespan.getCalcStartDate() + a * intervalLength);
            final long intervalFinishDate = (long) (intervalStartDate + intervalLength);

            final Interval interval = new Interval(intervalStartDate,
                    intervalFinishDate, pCurve[a]);
            intervals[a] = interval;

        }

        // Iterate through the totalValues

        for (final TimeRelatedValue totalValue : pTotalValues) {

            // Step 2: Calculate sub-intervals valid for the spread of this value

            final Interval[] subIntervals = SpreadCalculatorSaprimaFinal.createSubIntervals(intervals, totalValue);

            final double subCurveValueSum = SpreadCalculatorSaprimaFinal.getIntervalValueSum(subIntervals);

            // Step 3: Interate through the newly build intervals and spread the values

            for (final Interval interval : subIntervals) {

                double percentageInterval;
                if (0.0 == subCurveValueSum) {
                    percentageInterval = 0.0;
                } else {
                    if (SpreadCalculatorSaprimaFinal.CALCULATION_WITH_BIGDECIMAL) {
                        final BigDecimal bdInterval = BigDecimal.valueOf(interval.getValue());
                        final BigDecimal bdCurveValueSum = BigDecimal.valueOf(subCurveValueSum);
                        final BigDecimal quotient = bdInterval.divide(bdCurveValueSum, SpreadCalculatorSaprimaFinal.BIGDECIMAL_MATHCONTEXT);
                        percentageInterval = quotient.doubleValue();
                    } else {
                        percentageInterval = interval.getValue() / subCurveValueSum;
                    }
                }

                //int periodct = 0; //Debug
                for (final Period period : pPeriods) {

                    if (period.isInsideTimespan()) {

                        // Periods with duration == 0 (most probably, these are periods
                        // eliminated by exclusions) get value 0, if they lie
                        // inside the TimeRelatedValue, otherwise NaN
                        if (0 == period.getCalcDuration()) {

                            double[] values = period.getPeriodValues();
                            double[] singleValues = period.getPeriodSingleValues();

                            if (values == null) {
                                values = new double[pTotalValues.length];
                                Arrays.fill(values, Double.NaN);
                                period.setPeriodValues(values);
                            }
                            if (singleValues == null) {
                                singleValues = new double[pTotalValues.length];
                                Arrays.fill(singleValues, Double.NaN);
                                period.setPeriodSingleValues(singleValues);
                            }

                            // "Trick" for checking, if period is inside interval:
                            // 1 must be added to finishDate of period, as otherwise
                            // for periods with duration == 0, the return value
                            // of this method would always be 0
                            final long periodInsideIntervalIndicator = SpreadCalculatorSaprimaFinal.calculateOverlapDuration(interval.getStartDate(), interval.getFinishDate(), period.getCalcStartDate(), period.getCalcFinishDate() + 1);
                            if (1 == periodInsideIntervalIndicator) {
                                period.setPeriodValue(totalValue.getIndex(), 0.0);
                                period.setPeriodSingleValue(totalValue.getIndex(), 0.0);
                            } else if (0 != periodInsideIntervalIndicator) {
                                // Should never happen
                                throw new RuntimeException("ERROR:\nError in method computeSpreadForIndividualTimespans, when checking if 0 period is inside TimeRelatedValue.");
                            }

                            continue;
                        }

                        // TODO
                        // This calculation is done multiple times for the same period and sub-timespan
                        // Therefore, a speed-up is possible if a mechanism is implemented
                        // that enables checks like period.isInsideTimeRelatedValue (= isInsideSubTimespan)
                        final long durationInsideInterval = SpreadCalculatorSaprimaFinal.calculateOverlapDuration(interval.getStartDate(), interval.getFinishDate(), period.getCalcStartDate(), period.getCalcFinishDate());

                        // Periods completely outside (before or after) interval
                        // are being ignored
                        if (0 == durationInsideInterval) {
                            continue;
                        }

                        final double percentageTime;
                        final double percentage;
                        if (SpreadCalculatorSaprimaFinal.CALCULATION_WITH_BIGDECIMAL) {
                            final BigDecimal dividend = BigDecimal.valueOf(durationInsideInterval);
                            final BigDecimal divisor = BigDecimal.valueOf(interval.getDuration());
                            final BigDecimal quotient = dividend.divide(divisor, SpreadCalculatorSaprimaFinal.BIGDECIMAL_MATHCONTEXT);
                            percentageTime = quotient.doubleValue();
                            final BigDecimal secondFactor = BigDecimal.valueOf(percentageInterval);
                            final BigDecimal product = quotient.multiply(secondFactor);
                            percentage = product.doubleValue();

                        } else {
                            percentageTime = durationInsideInterval / interval.getDuration();
                            percentage = percentageInterval * percentageTime;
                        }

                        // Lazy initialisation of values and singleValues
                        double[] values = period.getPeriodValues();
                        double[] singleValues = period.getPeriodSingleValues();

                        if (values == null) {
                            values = new double[pTotalValues.length];
                            Arrays.fill(values, Double.NaN);
                            period.setPeriodValues(values);
                        }
                        if (singleValues == null) {
                            singleValues = new double[pTotalValues.length];
                            Arrays.fill(singleValues, Double.NaN);
                            period.setPeriodSingleValues(singleValues);
                        }

                        if (Double.isNaN(values[totalValue.getIndex()])) {
                            values[totalValue.getIndex()] = 0.0;
                        }
                        if (Double.isNaN(singleValues[totalValue.getIndex()])) {
                            singleValues[totalValue.getIndex()] = 0.0;
                        }

                        final double theValue;

                        if (SpreadCalculatorSaprimaFinal.CALCULATION_WITH_BIGDECIMAL) {

                            final BigDecimal firstFactor = BigDecimal.valueOf(totalValue.getValue());
                            final BigDecimal secondFactor = BigDecimal.valueOf(percentage);
                            final BigDecimal product = firstFactor.multiply(secondFactor);
                            theValue = values[totalValue.getIndex()] + product.doubleValue();

                        } else {
                            theValue = values[totalValue.getIndex()] + totalValue.getValue() * percentage;
                        }

                        period.setPeriodValue(totalValue.getIndex(), theValue);
                        period.setPeriodSingleValue(totalValue.getIndex(), theValue);

                    }
                }
            }
        }
    }


    /**
     * Computes the spread for the case that all total values are to be spread
     * over the complete timespan (i.e., there are no different individual
     * (sub-)timespans defined for each value).
     * 
     * @param pTotalValues
     *                an array of time related values to be spreaded
     * @param pTimespan
     *                the object representing start+finish date of the time span
     *                to be considered for the exclusions
     * @param pPeriods
     *                the time periods used as spread targets
     * @param pCurve
     *                an array of values representing the curve that determines
     *                the spread logic
     */
    private static void computeSpreadForUniversalTimespan(
            final double[] pCurve, final Period pTimespan,
            final Period[] pPeriods, final TimeRelatedValue[] pTotalValues) {

        final double curveValueSum = SpreadCalculatorSaprimaFinal.getCurveValueSum(pCurve);
        final int intervalCount = pCurve.length;
        final Interval[] intervals = new Interval[intervalCount];
        double intervalLength;
        if (SpreadCalculatorSaprimaFinal.CALCULATION_WITH_BIGDECIMAL) {
            final BigDecimal bdCalcDuration = BigDecimal.valueOf(pTimespan.getCalcDuration());
            final BigDecimal bdIntervalCount = BigDecimal.valueOf(intervalCount);
            final BigDecimal quotient = bdCalcDuration.divide(bdIntervalCount, SpreadCalculatorSaprimaFinal.BIGDECIMAL_MATHCONTEXT);
            intervalLength = quotient.doubleValue();
        } else {
            intervalLength = pTimespan.getCalcDuration() / intervalCount;
        }
        // Create intervals (sub-timespans) for each fragment of the curve and iterate through them
        for (int a = 0; a < intervalCount; a++) {
            //TODO Rundung reparieren - wird momentan durch cast abgeschnitten
            final long intervalStartDate = (long) (pTimespan.getCalcStartDate() + a * intervalLength);
            final long intervalFinishDate = (long) (intervalStartDate + intervalLength);

            final Interval interval = new Interval(intervalStartDate,
                    intervalFinishDate, pCurve[a]);
            intervals[a] = interval;

            double percentageInterval;
            if (SpreadCalculatorSaprimaFinal.CALCULATION_WITH_BIGDECIMAL) {
                final BigDecimal bdInterval = BigDecimal.valueOf(interval.getValue());
                final BigDecimal bdCurveValueSum = BigDecimal.valueOf(curveValueSum);
                final BigDecimal quotient = bdInterval.divide(bdCurveValueSum, SpreadCalculatorSaprimaFinal.BIGDECIMAL_MATHCONTEXT);
                percentageInterval = quotient.doubleValue();
            } else {
                percentageInterval = interval.getValue() / curveValueSum;
            }

            //int perodct = 0; //Debug
            for (final Period period : pPeriods) {

                if (period.isInsideTimespan()) {

                    final long durationInsideInterval = SpreadCalculatorSaprimaFinal.calculateOverlapDuration(interval.getStartDate(),
                            interval.getFinishDate(), period.getCalcStartDate(), period.getCalcFinishDate());

                    /*
                     * Debug: perodct++ System.out.println(" periode: " +
                     * perodct); System.out.println(" Duration Inside Intervall = " +
                     * durationInsideInterval);
                     */
                    final double percentageTime;
                    final double percentage;
                    if (SpreadCalculatorSaprimaFinal.CALCULATION_WITH_BIGDECIMAL) {
                        final BigDecimal dividend = BigDecimal.valueOf(durationInsideInterval);
                        final BigDecimal divisor = BigDecimal.valueOf(interval.getDuration());
                        final BigDecimal quotient = dividend.divide(divisor, SpreadCalculatorSaprimaFinal.BIGDECIMAL_MATHCONTEXT);
                        percentageTime = quotient.doubleValue();
                        final BigDecimal secondFactor = BigDecimal.valueOf(percentageInterval);
                        final BigDecimal product = quotient.multiply(secondFactor);
                        percentage = product.doubleValue();

                    } else {
                        percentageTime = durationInsideInterval / interval.getDuration();
                        percentage = percentageInterval * percentageTime;
                    }

                    double[] values = period.getPeriodValues();
                    double[] singleValues = period.getPeriodSingleValues();

                    if (values == null) {
                        values = new double[pTotalValues.length];
                        Arrays.fill(values, Double.NaN);
                        period.setPeriodValues(values);
                    }
                    if (singleValues == null) {
                        singleValues = new double[pTotalValues.length];
                        Arrays.fill(singleValues, Double.NaN);
                        period.setPeriodSingleValues(singleValues);
                    }

                    for (int j = 0; j < pTotalValues.length; j++) {
                        if (Double.isNaN(values[j])) {
                            values[j] = 0.0;
                        }
                        if (Double.isNaN(singleValues[j])) {
                            singleValues[j] = 0.0;
                        }

                        final double theValue;

                        if (SpreadCalculatorSaprimaFinal.CALCULATION_WITH_BIGDECIMAL) {

                            final BigDecimal firstFactor = BigDecimal.valueOf(pTotalValues[j].getValue());
                            final BigDecimal secondFactor = BigDecimal.valueOf(percentage);
                            final BigDecimal product = firstFactor.multiply(secondFactor);
                            theValue = values[pTotalValues[j].getIndex()] + product.doubleValue();
                        } else {
                            theValue = values[pTotalValues[j].getIndex()]
                                    + pTotalValues[j].getValue() * percentage;
                        }

                        /*
                         * Debug: System.out.println(" new value = " +
                         * (values[j] + pTotalValues[j] * percentage) + "\t(" +
                         * values[j] + " + " + pTotalValues[j] + " * " +
                         * percentage + "), Percentage = " + percentageInterval + "
                         * x " + percentageTime + ")");
                         */

                        period.setPeriodValue(j, theValue);
                        period.setPeriodSingleValue(j, theValue);

                    }
                }

            }
        }
    }


    /**
     * Creates sub-intervals, i.e. the intervals "cut" out of the original
     * intervals using the timespan of the TimeRelatedValue
     * 
     * @param pIntervals
     *                the intervals of the curve determined by the number of the
     *                curve values
     * @param subTimespan
     *                the sub-timespan valid for spreading the value
     * @return the created sub-intervals
     */
    public static Interval[] createSubIntervals(final Interval[] pIntervals,
            final TimeRelatedValue subTimespan) {

        // As the number of resulting sub-intervals is not known from the beginning,
        // they are first added into a list that is later transformed into an array
        final ArrayList<Interval> subIntervalList = new ArrayList<Interval>();

        for (final Interval interval : pIntervals) {
            // Intervals preceding or following the sub-timespan are being ignored
            if (interval.getFinishDate() <= subTimespan.getCalcStartDate()
                    || interval.getStartDate() >= subTimespan.getCalcFinishDate()) {
                continue;
            }

            // TODO a speed-up could be possible, if the original intervals are used
            // but this could be dangerous, as the original intervals might be modified

            // Intervals inside the sub-timespan are added completely 
            if (interval.getStartDate() >= subTimespan.getCalcStartDate() && interval.getFinishDate() <= subTimespan.getCalcFinishDate()) {
                subIntervalList.add(new Interval(interval.getStartDate(), interval.getFinishDate(), interval.getValue()));
                continue;
            }

            // Intervals overlapping the start or finish of the sub-timespan are
            // added partly, with value reduced according to percentage of original size
            // TODO check, if this is correct

            // TODO a speed-up could be possible, if the original intervals are used
            // but this could be dangerous, as the original intervals are modified

            long newStartDate = 0L;
            long newFinishDate = 0L;
            boolean startDateSet = false;
            if (interval.getStartDate() < subTimespan.getCalcStartDate()) {
                newStartDate = subTimespan.getCalcStartDate();
                startDateSet = true;
                newFinishDate = interval.getFinishDate();
            }
            // no else clause, because an interval could cover the complete sub-timespan
            // in this case, it must be shortened on both ends
            if (interval.getFinishDate() > subTimespan.getCalcFinishDate()) {
                // This is to consider intervals covering complete sub-timespan
                if (!startDateSet) {
                    newStartDate = interval.getStartDate();
                }
                newFinishDate = subTimespan.getCalcFinishDate();
            }
            final Interval newInterval = new Interval(newStartDate, newFinishDate, 0);

            // The value of the shortened interval is reduced according to
            // percentage of its original size

            double value;

            if (SpreadCalculatorSaprimaFinal.CALCULATION_WITH_BIGDECIMAL) {
                final BigDecimal firstFactor = BigDecimal.valueOf(interval.getValue());
                final BigDecimal secondFactor = BigDecimal.valueOf(newInterval.getDuration());
                final BigDecimal product = firstFactor.multiply(secondFactor, SpreadCalculatorSaprimaFinal.BIGDECIMAL_MATHCONTEXT);
                final BigDecimal divisor = BigDecimal.valueOf(interval.getDuration());
                final BigDecimal quotient = product.divide(divisor, SpreadCalculatorSaprimaFinal.BIGDECIMAL_MATHCONTEXT);
                value = quotient.doubleValue();

            } else {
                value = interval.getValue() * newInterval.getDuration()
                        / interval.getDuration();
            }

            newInterval.setValue(value);
            subIntervalList.add(newInterval);

        }

        // Transformation of the list into an array
        final Interval[] subIntervals = new Interval[subIntervalList.size()];
        for (int i = 0; i < subIntervals.length; i++) {
            subIntervals[i] = subIntervalList.get(i);
        }

        // TODO check why this does not work:
        // subIntervals = (Interval[]) subIntervalList.toArray();

        return subIntervals;

    }


    /**
     * Sums up all values of the curve.
     * 
     * @param pCurve
     *                the curve
     * @return the sum of all curve values
     */
    private static double getCurveValueSum(final double[] pCurve) {
        double curveValueSum = 0.0;
        for (final double curveValue : pCurve) {
            curveValueSum += curveValue;
        }
        return curveValueSum;
    }


    /**
     * Sums up the values of all intervals.
     * 
     * @param pIntervals
     *                the intervals containing the values to sum up
     * @return the sum of all interval values
     */
    private static double getIntervalValueSum(final Interval[] pIntervals) {
        double intervalValueSum = 0.0;
        for (final Interval interval : pIntervals) {
            intervalValueSum += interval.getValue();
        }
        return intervalValueSum;
    }


    /**
     * Resets all single value fields to zero. If the array is null it will be
     * initialized.
     * 
     * @param pPeriods
     *                periods
     * @param pInitSize
     *                initialize size (only used if the array is null!)
     */
    static void resetSingleValues(final Period[] pPeriods, final int pInitSize) {

        for (final Period period : pPeriods) {
            //reset the single value field
            double[] singleValues = period.getPeriodSingleValues();
            if (singleValues == null) {
                singleValues = new double[pInitSize];
                Arrays.fill(singleValues, Double.NaN);
                period.setPeriodSingleValues(singleValues);
            } else {
                for (int i = 0; i < singleValues.length; i++) {
                    singleValues[i] = Double.NaN;
                }
            }
        }
    }


    /**
     * Segregates the exclusions, i.e., periods, timespan, and start/finish of
     * total values are shortened. "To shorten" here can mean both a reduction
     * of their duration as well as to move them backward in time. <b>Note: Only
     * calcStartDate/calcFinishDate are changed. The original dates remain
     * unchanged in startDate/finishDate.
     * 
     * @param pTotalValues
     *                an array of time related values to be spreaded
     * @param pTimespan
     *                the object representing start+finish date of the time span
     *                to be considered for the exclusions
     * @param pPeriods
     *                the time periods used as spread targets
     * @param pCurve
     *                an array of values representing the curve that determines
     *                the spread logic
     * @param pExclusionPeriods
     *                the time periods to be excluded from the spread (e.g.,
     *                weekends) - please note that exclusion periods may not
     *                overlap and must be sorted
     */
    static void segregateExclusions(final Period[] pPeriods,
            final Period pTimespan, final TimeRelatedValue[] pTotalValues,
            final Period[] pExclusionPeriods) {

        if (pExclusionPeriods != null && pExclusionPeriods.length > 0) {

            SpreadCalculatorSaprimaFinal.cleanIrrelevantPeriodParts(pExclusionPeriods, pTimespan);

            //TODO ueberschneidende/ueberlappende exclusions mergen (feature wird derzeit nicht implementiert, siehe javadoc)

            // Sum of durations of all exclusions already handled
            long totalExclusionDuration = 0;

            for (final Period exclusion : pExclusionPeriods) {

                if (exclusion.isInsideTimespan()) {

                    // Reduce finish date of timespan by duration of exclusion
                    pTimespan.setCalcFinishDate(pTimespan.getCalcFinishDate() - exclusion.getCalcDuration());

                    // Exclusions are being moved backward by sum of durations
                    // of all previous exclusions
                    exclusion.setCalcStartDate(exclusion.getCalcStartDate() - totalExclusionDuration);
                    exclusion.setCalcFinishDate(exclusion.getCalcFinishDate() - totalExclusionDuration);
                    // Sum up durations of all exclusions already handled
                    totalExclusionDuration += exclusion.getCalcDuration();

                    for (final Period period : pPeriods) {

                        final long overlapDuration = SpreadCalculatorSaprimaFinal.calculateOverlapDuration(exclusion.getCalcStartDate(), exclusion.getCalcFinishDate(), period.getCalcStartDate(), period.getCalcFinishDate());

                        // Periods overlapped by exclusions are shortened by overlap duration
                        if (overlapDuration > 0) {
                            final long periodDurationNew = period.getCalcDuration() - overlapDuration;
                            if (exclusion.getCalcStartDate() < period.getCalcStartDate()) {
                                period.setCalcStartDate(exclusion.getCalcStartDate());
                            }
                            period.setCalcFinishDate(period.getCalcStartDate() + periodDurationNew);

                        } else {
                            // Periods not overlapped by exclusions are moved backward by exclusion duration,
                            // if they lie after the exclusion
                            if (period.getCalcStartDate() >= exclusion.getCalcFinishDate()) {
                                period.setCalcStartDate(period.getCalcStartDate() - exclusion.getCalcDuration());
                                period.setCalcFinishDate(period.getCalcFinishDate() - exclusion.getCalcDuration());
                            }
                            // Periods not overlapped by exclusions are ignored,
                            // if they lie before the exclusion
                        }
                    }

                    for (final TimeRelatedValue value : pTotalValues) {

                        final long overlapDuration = SpreadCalculatorSaprimaFinal.calculateOverlapDuration(exclusion.getCalcStartDate(), exclusion.getCalcFinishDate(), value.getCalcStartDate(), value.getCalcFinishDate());

                        // Timespans of time related values overlapped by exclusions are shortened by overlap duration
                        if (overlapDuration > 0) {
                            final long timeRelatedValueDurationNew = value.getCalcDuration() - overlapDuration;
                            if (exclusion.getCalcStartDate() < value.getCalcStartDate()) {
                                value.setCalcStartDate(exclusion.getCalcStartDate());
                            }
                            value.setCalcFinishDate(value.getCalcStartDate() + timeRelatedValueDurationNew);

                        } else {
                            // Timespans of time related values not overlapped by exclusions are moved backward by exclusion duration,
                            // if they lie after the exclusion
                            if (value.getCalcStartDate() >= exclusion.getCalcFinishDate()) {
                                value.setCalcStartDate(value.getCalcStartDate()
                                        - exclusion.getCalcDuration());
                                value.setCalcFinishDate(value.getCalcFinishDate() - exclusion.getCalcDuration());
                            }
                            // Timespans of time related values not overlapped by exclusions are ignored,
                            // if they lie before the exclusion
                        }

                    }

                }
            }
        }
    }


    @Override
    public void computeSpread(double[] values, long start, long finish, SpreadPeriod[] periods, double[] curve, SpreadPeriod[] exclusions) {
        Period[] pPeriods = toSpreadPeriods(periods);

        Period[] pExclusionPeriods = toSpreadPeriods(exclusions);

        SpreadCalculatorSaprimaFinal.computeSpread(values, start, finish, pPeriods, curve, pExclusionPeriods);

    }


    private Period[] toSpreadPeriods(SpreadPeriod[] periods) {
        if (periods == null || periods.length == 0) {
            return null;
        }
        Period[] temp = new Period[periods.length];
        for (int i = 0; i < periods.length; i++) {
            SpreadPeriod spreadPeriod = periods[i];
            Period period = new Period(spreadPeriod.getStartMillis(), spreadPeriod.getEndMillis());
            temp[i] = period;
        }
        return temp;
    }
}

