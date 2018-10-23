// https://searchcode.com/api/result/100762977/

package com.tcmj.pm.spread.impl;

import com.tcmj.pm.spread.SpreadCalculator;
import com.tcmj.pm.spread.impl.SpreadCalculatorPrimaImpl.CurveInterval;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.janino.CompileException;
import org.codehaus.janino.ExpressionEvaluator;
import org.codehaus.janino.Parser.ParseException;
import org.codehaus.janino.Scanner.ScanException;

/**
 *
 */
public class SpreadJaninoImpl implements SpreadCalculator {

    private static final boolean CALCULATION_WITH_JANINO = false;

    private static ExpressionEvaluator ee;


    private SpreadPeriod[] periods;

    static {

        System.out.println(new BigDecimal(2.3));
        System.out.println(BigDecimal.valueOf(2.3));
        System.out.println(new BigDecimal("2.3"));

        try {
            fff();
        } catch (Exception ex) {
            System.err.println("Errror:::: " + ex);
        }
    }


    private static void fff() throws CompileException, ParseException, ScanException {

        // Compile the expression once; relatively slow.
        ee = new ExpressionEvaluator(
                "a / b", // expression
                double.class, // expressionType
                new String[]{"a", "b"}, // parameterNames
                new Class[]{double.class, double.class} // parameterTypes
                );



    }


    /**
     * Spreads one or more values over periods using a curve.
     * The values will be directly set into the given periods for performance reasons.
     * @param values one or more values to be spreaded
     * @param start curve start date (timeframe to be considered)
     * @param finish curve finish date (timeframe to be considered)
     * @param periods time periods
     * @param curve an array of values representing the curve used to spread the values
     * @param exclusions the time periods to be excluded from the spread (e.g. weekends)
     *        <b>please note that exclusion periods may not overlap and must be sorted</b>
     */
    @Override
    public void computeSpread(double[] values, long start, long finish,
            SpreadPeriod[] periods, double[] curve, SpreadPeriod[] exclusions) {

        final SpreadPeriod timeframe = new SpreadPeriod(start, finish);

        double curveValueSum = getCurveValueSum(curve);

        cleanIrrelevantPeriodParts(periods, timeframe);

        resetSingleValues(periods, values.length);
        // TODO Funktion einbauen, die Ausschlussperioden abzieht und dabei Gesamt-Zeitraum und Perioden Start/Ende neu berechnet
        subtractExclusions(periods, timeframe, exclusions);


        int intervalCount = curve.length;
        CurveInterval[] intervals = new CurveInterval[intervalCount];
        double intervalLength;
        if (CALCULATION_WITH_JANINO) {
            try {
                // Evaluate it with varying parameter values; very fast.
                intervalLength = (Double) ee.evaluate(new Object[]{timeframe.getCalcDuration(), intervalCount});


            } catch (InvocationTargetException ex) {
                Logger.getLogger(SpreadJaninoImpl.class.getName()).log(Level.SEVERE, null, ex);
                intervalLength = 1;
            }

        } else {
            intervalLength = timeframe.getCalcDuration() / intervalCount;
        }
        // Create sub-timeframes for each interval of the curve
        for (int a = 0; a < intervalCount; a++) {
            //TODO Rundung reparieren - wird momentan durch cast abgeschnitten
            long intervalStartDate = (long) (timeframe.getCalcStartDate() + a * intervalLength);
            long intervalFinishDate = (long) (intervalStartDate + intervalLength);

            CurveInterval interval = new CurveInterval(intervalStartDate, intervalFinishDate, curve[a]);
            intervals[a] = interval;

            // TODO evtl. auslagern in Intervall- oder Kurvenklasse
            double percentageInterval;
            if (CALCULATION_WITH_JANINO) {
                try {
                    percentageInterval = (Double) ee.evaluate(new Object[]{interval.getValue(), curveValueSum});
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(SpreadJaninoImpl.class.getName()).log(Level.SEVERE, null, ex);
                    percentageInterval = 1;
                }



            } else {
                percentageInterval = interval.getValue() / curveValueSum;
            }



            for (SpreadPeriod period : periods) {

                if (period.isInsideTimespan()) {

                    long durationInsideInterval = calculateOverlapDuration(interval.getStartDate(), interval.getFinishDate(), period.getCalcStartDate(), period.getCalcFinishDate());

                    /* Debug:
                    perodct++
                    System.out.println("   periode: " + perodct);
                    System.out.println("        Duration Inside Intervall = " + durationInsideInterval);
                     */
                    double percentageTime;
                    if (CALCULATION_WITH_JANINO) {
                        try {
                            percentageTime = (Double) ee.evaluate(new Object[]{durationInsideInterval, interval.getDuration()});
                        } catch (InvocationTargetException ex) {
                            Logger.getLogger(SpreadJaninoImpl.class.getName()).log(Level.SEVERE, null, ex);
                            percentageTime = 1;
                        }


                    } else {
                        percentageTime = durationInsideInterval / interval.getDuration();
                    }
                    double percentage = percentageInterval * percentageTime;

                    double[] periodvalues = period.getPeriodValues();
                    if (periodvalues == null) {
                        periodvalues = new double[values.length];
                        period.setPeriodValues(periodvalues);
                    }

                    for (int j = 0; j < values.length; j++) {
                        final double theValue = periodvalues[j] + values[j] * percentage;

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
     * @param period the period to intersect
     * @param periodToIntersectWith the period used for interecting the period
     */
//    @Override
    public void intersectPeriod(SpreadPeriod period, SpreadPeriod periodToIntersectWith) {
        // SpreadPeriod is completely inside timeframe (start - finish)
        if (period.getEndMillis() <= periodToIntersectWith.getEndMillis()
                && period.getStartMillis() >= periodToIntersectWith.getStartMillis()) {
            period.setInsideTimespan(true);
        } else {  // SpreadPeriod is partly outside (before) timeframe

            if (period.getStartMillis() < periodToIntersectWith.getStartMillis()) {
                if (period.getEndMillis() > periodToIntersectWith.getStartMillis()) {
                    period.setCalcStartDate(periodToIntersectWith.getStartMillis());
                    period.setInsideTimespan(true);
                }
            } else if (period.getStartMillis() < periodToIntersectWith.getEndMillis()) {
                // SpreadPeriod is partly outside (after) timeframe
                period.setCalcFinishDate(periodToIntersectWith.getEndMillis());
                period.setInsideTimespan(true);
            }

            // Case "SpreadPeriod is completely outside (before or after) timeframe"
            // is not considered, because default field values of period are
            // correct then (periodValues = null; insideTimespan = false) ;

        }
    }


//    @Override
    public void subtractExclusions(SpreadPeriod[] periods, SpreadPeriod timeframe, SpreadPeriod[] exclusions) {

        if (exclusions != null && exclusions.length > 0) {

            cleanIrrelevantPeriodParts(exclusions, timeframe);

            //TODO ueberschneidende/ueberlappende exclusions mergen (feature wird derzeit nicht implementiert, siehe javadoc)

            // Sum of durations of all exclusions already handled
            long totalExclusionDuration = 0;

            for (SpreadPeriod exclusion : exclusions) {

                if (exclusion.isInsideTimespan()) {

                    // Reduce finish date of timeframe by duration of exclusion
                    timeframe.setCalcFinishDate(timeframe.getCalcFinishDate() - exclusion.getCalcDuration());

                    // Exclusions are being moved backward by sum of durations
                    // of all previous exclusions
                    exclusion.setCalcStartDate(exclusion.getCalcStartDate() - totalExclusionDuration);
                    exclusion.setCalcFinishDate(exclusion.getCalcFinishDate() - totalExclusionDuration);
                    // Sum up durations of all exclusions already handled
                    totalExclusionDuration += exclusion.getCalcDuration();

                    for (SpreadPeriod period : periods) {

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


    private void cleanIrrelevantPeriodParts(SpreadPeriod[] periods, SpreadPeriod timeframe) {

        for (SpreadPeriod period : periods) {
            intersectPeriod(period, timeframe);
        }
    }


    /**
     * Sums up all values of the curve.
     * @param curve the curve
     * @return the sum of all curve values
     */
    private static double getCurveValueSum(double[] curve) {
        double curveValueSum = 0d;
        for (double curveValue : curve) {
            curveValueSum += curveValue;
        }
        return curveValueSum;
    }


    /**
     * @return the periods
     */
    public SpreadPeriod[] getPeriods() {
        return periods;
    }
}

