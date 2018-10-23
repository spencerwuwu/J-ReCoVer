// https://searchcode.com/api/result/100762973/

package com.tcmj.pm.spread.impl;

import com.tcmj.pm.spread.SpreadCalculator;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;

/** */
public class SpreadCalculatorPrimaImpl implements SpreadCalculator {

    private static final boolean CALCULATION_WITH_BIGDECIMAL = false;

    /** MathContext.DECIMAL32 */
    private static final MathContext BIGDECIMAL_MATHCONTEXT = new MathContext(5, RoundingMode.HALF_UP);

    SpreadPeriod[] periods;


    /**
     * saprima Spread Calculator.
     * Spreads an array of values into given time periods according to a spread curve.
     * @param pTotalValues an array of values to be spreaded
     * @param pTimespanStart the start date of the time span to be considered
     * @param pTimespanFinish the finish date of the time span to be considered
     * @param pPeriods the time periods used as spread targets
     * @param pCurve an array of values representing the curve that determines the spread logic
     * @param pExclusionPeriods the time periods to be excluded from the spread (e.g., weekends) -
     *        please note that exclusion periods may not overlap and must be sorted
     */
    public static void computeSpreadStatic(
            double[] pTotalValues,
            long pTimespanStart, long pTimespanFinish,
            SpreadPeriod[] pPeriods,
            double[] pCurve,
            SpreadPeriod[] pExclusionPeriods) {

        final SpreadPeriod timespan = new SpreadPeriod(pTimespanStart, pTimespanFinish);

        double curveValueSum = getCurveValueSum(pCurve);

        cleanIrrelevantPeriodParts(pPeriods, timespan);

        resetSingleValues(pPeriods, pTotalValues.length);

        reducePeriodsByExclusions(pPeriods, timespan, pExclusionPeriods);


        int intervalCount = pCurve.length;
        CurveInterval[] intervals = new CurveInterval[intervalCount];
        double intervalLength;
        if (CALCULATION_WITH_BIGDECIMAL) {
            BigDecimal bdCalcDuration = BigDecimal.valueOf(timespan.getCalcDuration());
            BigDecimal bdIntervalCount = BigDecimal.valueOf(intervalCount);
            BigDecimal divide = bdCalcDuration.divide(bdIntervalCount, BIGDECIMAL_MATHCONTEXT);
            intervalLength = divide.doubleValue();
        } else {
            intervalLength = timespan.getCalcDuration() / intervalCount;
        }
        // Create sub-timespans for each interval of the curve
        for (int a = 0; a < intervalCount; a++) {
            //TODO Rundung reparieren - wird momentan durch cast abgeschnitten
            long intervalStartDate = (long) (timespan.getCalcStartDate() + a * intervalLength);
            long intervalFinishDate = (long) (intervalStartDate + intervalLength);

            CurveInterval interval = new CurveInterval(intervalStartDate, intervalFinishDate, pCurve[a]);
            intervals[a] = interval;

            double percentageInterval;
            if (CALCULATION_WITH_BIGDECIMAL) {
                BigDecimal bdInterval = BigDecimal.valueOf(interval.getValue());
                BigDecimal bdCurveValueSum = BigDecimal.valueOf(curveValueSum);
                BigDecimal divide = bdInterval.divide(bdCurveValueSum, BIGDECIMAL_MATHCONTEXT);
                percentageInterval = divide.doubleValue();
            } else {
                percentageInterval = interval.getValue() / curveValueSum;
            }

            //int perodct = 0; //Debug
            for (SpreadPeriod period : pPeriods) {


                if (period.isInsideTimespan()) {

                    long durationInsideInterval = calculateOverlapDuration(interval.getStartDate(), interval.getFinishDate(), period.getCalcStartDate(), period.getCalcFinishDate());

                    /* Debug:
                    perodct++
                    System.out.println("   periode: " + perodct);
                    System.out.println("        Duration Inside Intervall = " + durationInsideInterval);
                     */
                    double percentageTime;
                    if (CALCULATION_WITH_BIGDECIMAL) {
                        BigDecimal bdDurationInsideInterval = BigDecimal.valueOf(durationInsideInterval);
                        BigDecimal bdIntervalDuration = BigDecimal.valueOf(interval.getDuration());
                        BigDecimal divide = bdDurationInsideInterval.divide(bdIntervalDuration, BIGDECIMAL_MATHCONTEXT);
                        percentageTime = divide.doubleValue();
                    } else {
                        percentageTime = durationInsideInterval / interval.getDuration();
                    }
                    double percentage = percentageInterval * percentageTime;

                    double[] values = period.getPeriodValues();
                    if (values == null) {
                        values = new double[pTotalValues.length];
                        period.setPeriodValues(values);
                    }


                    for (int j = 0; j < pTotalValues.length; j++) {
                        final double theValue = values[j] + pTotalValues[j] * percentage;

                        /* Debug:
                        System.out.println("         new value = " + (values[j] + pTotalValues[j] * percentage) + "\t(" + values[j] + " + " + pTotalValues[j] + " * " + percentage + "), Percentage = "
                        + percentageInterval + " x " + percentageTime + ")");
                         */

                        period.setPeriodValue(j, theValue);
                        period.setPeriodSingleValue(j, theValue);

                    }
                }
            }
        }
    }


    private static long calculateOverlapDuration(long start1, long finish1, long start2, long finish2) {

        // SpreadPeriod is completely outside (before or after) interval
        if (start1 >= finish2 || finish1 <= start2) {
            return 0;
        }

        long[] thelist = {finish2, start2, start1, finish1};
        Arrays.sort(thelist, 0, 4);
        return thelist[2] - thelist[1];


//        // SpreadPeriod is completely inside interval
//        if (start1 >= start2
//                && finish1 <= finish2) {
//            return pRange.getCalcDuration();
//        }
//
//        // SpreadPeriod is partly outside (before) interval
//        if (start1 < start2) {
//            return finish1 - start2;
//        } // SpreadPeriod is partly outside (after) interval
//        else {
//            return finish2 - start1;
//        }

    }


    /**
     * Resets all single value fields to zero.
     * If the array is null it will be initialized.
     * @param pPeriods periods
     * @param pInitSize initialize size (only used if the array is null!)
     */
    static void resetSingleValues(SpreadPeriod[] pPeriods, int pInitSize) {
        //   (TODO move to a separate method)
        for (SpreadPeriod period : pPeriods) {
            //reset the single value field
            double[] periodSingleValues = period.getPeriodSingleValues();
            if (periodSingleValues == null) {
                period.setPeriodSingleValues(new double[pInitSize]);
            } else {
                for (int i = 0; i < periodSingleValues.length; i++) {
                    periodSingleValues[i] = 0d;
                }
            }
        }
    }


    /**
     *
     * @param pPeriod the period to intersect
     * @param pPeriodToIntersectWith the period used for interecting the pPeriod
     */
    protected static void calculateIntersectionOfPeriods(SpreadPeriod pPeriod, SpreadPeriod pPeriodToIntersectWith) {
        // SpreadPeriod is completely inside timespan (start - finish)
        if (pPeriod.getEndMillis() <= pPeriodToIntersectWith.getEndMillis()
                && pPeriod.getStartMillis() >= pPeriodToIntersectWith.getStartMillis()) {
            pPeriod.setInsideTimespan(true);
        } else {  // SpreadPeriod is partly outside (before) timespan

            if (pPeriod.getStartMillis() < pPeriodToIntersectWith.getStartMillis()) {
                if (pPeriod.getEndMillis() > pPeriodToIntersectWith.getStartMillis()) {
                    pPeriod.setCalcStartDate(pPeriodToIntersectWith.getStartMillis());
                    pPeriod.setInsideTimespan(true);
                }
            } else if (pPeriod.getStartMillis() < pPeriodToIntersectWith.getEndMillis()) {
                // SpreadPeriod is partly outside (after) timespan
                pPeriod.setCalcFinishDate(pPeriodToIntersectWith.getEndMillis());
                pPeriod.setInsideTimespan(true);
            }

            // Case "SpreadPeriod is completely outside (before or after) timespan"
            // is not considered, because default field values of period are
            // correct then (periodValues = null; insideTimespan = false) ;

        }
    }


    static void reducePeriodsByExclusions(SpreadPeriod[] pPeriods, SpreadPeriod pTimespan, SpreadPeriod[] pExclusions) {

        if (pExclusions != null && pExclusions.length > 0) {

            cleanIrrelevantPeriodParts(pExclusions, pTimespan);

            //TODO ueberschneidende/ueberlappende exclusions mergen (feature wird derzeit nicht implementiert, siehe javadoc)

            // Sum of durations of all exclusions already handled
            long totalExclusionDuration = 0;

            for (SpreadPeriod exclusion : pExclusions) {

                if (exclusion.isInsideTimespan()) {

                    // Reduce finish date of timespan by duration of exclusion
                    pTimespan.setCalcFinishDate(pTimespan.getCalcFinishDate() - exclusion.getCalcDuration());

                    // Exclusions are being moved backward by sum of durations
                    // of all previous exclusions
                    exclusion.setCalcStartDate(exclusion.getCalcStartDate() - totalExclusionDuration);
                    exclusion.setCalcFinishDate(exclusion.getCalcFinishDate() - totalExclusionDuration);
                    // Sum up durations of all exclusions already handled
                    totalExclusionDuration += exclusion.getCalcDuration();

                    for (SpreadPeriod period : pPeriods) {

                        long overlapDuration = calculateOverlapDuration(exclusion.getCalcStartDate(), exclusion.getCalcFinishDate(), period.getCalcStartDate(), period.getCalcFinishDate());

                        // Periods overlapped by exclusions are shortened by overlap duration
                        if (overlapDuration > 0) {
                            long periodDurationNew = period.getCalcDuration() - overlapDuration;
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
                }
            }
        }
    }


    private static void cleanIrrelevantPeriodParts(SpreadPeriod[] pPeriods, SpreadPeriod pTimespan) {

        for (SpreadPeriod period : pPeriods) {

            calculateIntersectionOfPeriods(period, pTimespan);

        }
    }


    /**
     * Sums up all values of the curve.
     * @param pCurve the curve
     * @return the sum of all curve values
     */
    private static double getCurveValueSum(double[] pCurve) {
        double curveValueSum = 0.0;
        for (double curveValue : pCurve) {
            curveValueSum += curveValue;
        }
        return curveValueSum;
    }


//    @Override
    public void intersectPeriod(SpreadPeriod period, SpreadPeriod periodToIntersectWith) {
        calculateIntersectionOfPeriods(period, periodToIntersectWith);
    }


//    @Override
    public void subtractExclusions(SpreadPeriod[] periods, SpreadPeriod timeframe, SpreadPeriod[] exclusions) {
        reducePeriodsByExclusions(periods, timeframe, exclusions);
    }


    @Override
    public void computeSpread(double[] values, long start, long finish, SpreadPeriod[] periods, double[] curve, SpreadPeriod[] exclusions) {
        computeSpreadStatic(values, start, finish, periods, curve, exclusions);
    }


//    @Override
    public SpreadPeriod[] getPeriods() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    static class CurveInterval {

        /** Original start date. */
        private final long mStartDate;

        /** Original finish date. */
        private final long mFinishDate;

        /** Calculated value of this interval. */
        private final double mDuration;

        /** Calculated value of this interval. */
        private double mValue;


        /**
         * Constructor.
         * @param pStartDate start date
         * @param pFinishDate finish date
         * @param pValue the value of the interval
         */
        public CurveInterval(long pStartDate, long pFinishDate, double pValue) {
            if (pStartDate > pFinishDate) {
                throw new RuntimeException("StartDate may not after FinishDate!");
            }
            this.mStartDate = pStartDate;
            this.mFinishDate = pFinishDate;
            this.mDuration = pFinishDate - pStartDate;
            this.mValue = pValue;


            int x = 0;

            x++;

        }


        public long getFinishDate() {
            return mFinishDate;
        }


        public long getStartDate() {
            return mStartDate;
        }


        /**
         * @return the value
         */
        public double getValue() {
            return mValue;
        }


        /**
         * @param value the value to set
         */
        void setPeriodValue(double value) {
            this.mValue = value;
        }


        /**
         * @return the mDuration
         */
        public double getDuration() {
            return mDuration;
        }
    }
}

