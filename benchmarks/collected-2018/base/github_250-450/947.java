// https://searchcode.com/api/result/100762985/

package com.tcmj.pm.spread.impl;

import com.tcmj.pm.spread.SpreadCalculator;

/**
 * verbesserung: setCurve und dort alle berechnungen fur die kurve
 */
public class SpreadDoubleImpl implements SpreadCalculator {

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
    public strictfp void computeSpread(double[] values, long start, long finish,
            SpreadPeriod[] periods, double[] curve, SpreadPeriod[] exclusions) {

        final SpreadPeriod timeframe = new SpreadPeriod(start, finish);

        double curveValueSum = curveValueSum(curve);


        for (SpreadPeriod period : periods) {

            period.adjustIntersectionDate(timeframe);

            initValues(period, values.length);
        }


        // TODO Funktion einbauen, die Ausschlussperioden abzieht und dabei Gesamt-Zeitraum und Perioden Start/Ende neu berechnet
        subtractExclusions(periods, timeframe, exclusions);


        int intervalCount = curve.length;
//        CurveInterval[] intervals = new CurveInterval[intervalCount];
        double intervalLength = timeframe.getCalcDuration() / intervalCount;
        // Create sub-timeframes for each interval of the curve
        for (int a = 0; a < intervalCount; a++) {
            //TODO Rundung reparieren - wird momentan durch cast abgeschnitten
            long intervalStartDate = (long) (timeframe.getCalcStartDate() + a * intervalLength);
//            System.out.println("s "+intervalStartDate);
            long intervalFinishDate = (long) (intervalStartDate + intervalLength);
//            System.out.println("f "+intervalFinishDate);
            double intervalDuration = intervalFinishDate - intervalStartDate;  //wichtig dass es double ist wg. spterer berechnung

//            CurveInterval interval = new CurveInterval(intervalStartDate, intervalFinishDate, curve[a]);
//            intervals[a] = interval;

            // TODO evtl. auslagern in Intervall- oder Kurvenklasse
            double percentageInterval = curve[a] / curveValueSum;
            //percentageInterval = interval.getValue() / curveValueSum;
//            percentageInterval;



            for (SpreadPeriod period : periods) {

//        org.joda.time.CurveInterval interval1 = new org.joda.time.CurveInterval(period.getStartMillis(), period.getEndMillis());
//
//        org.joda.time.CurveInterval interval2 = new org.joda.time.CurveInterval(timeframe.getStartMillis(), timeframe.getEndMillis());
//        org.joda.time.CurveInterval overlap = interval1.overlap(interval2);
//        if (overlap != null) {
//            period.setInsideTimespan(true);
//        }


                if (period.isInsideTimespan() && period.getCalcDuration()>0) {


//                    period.setCalcStartDate(overlap.getStartMillis());
//            period.setCalcFinishDate(overlap.getEndMillis());

//            System.out.println("p "+period.getCalcStartDate() + " - "+period.getCalcFinishDate());
                    //long durationInsideInterval = calculateOverlapDuration(interval.getStartMillis(), interval.getEndMillis(), period.getCalcStartDate(), period.getCalcFinishDate());
//                    long durationInsideInterval = calculateOverlapDuration(intervalStartDate, intervalFinishDate, period.getCalcStartDate(), period.getCalcFinishDate());


            //wenn periode start und ende gleich (duration also 0) dann value auch 0
            //TODO
//            if (period.getCalcDuration()>0) {
//
//                    }
//

            long durationInsideInterval = period.calculatedOverlapDuration(intervalStartDate, intervalFinishDate);
//            long durationInsideInterval = new Range(period.getCalcStartDate(), period.getCalcFinishDate())
//                    .overlapDuration(new Range(intervalStartDate, intervalFinishDate));

                    /* Debug:
                    perodct++
                    System.out.println("   periode: " + perodct);
                    System.out.println("        Duration Inside Intervall = " + durationInsideInterval);
                     */
//                    percentageTime;
                    //percentageTime = durationInsideInterval / interval.getDuration();
                    double percentageTime = durationInsideInterval / intervalDuration;
                    double percentage = percentageInterval * percentageTime;

                    double[] periodvalues = period.getPeriodValues();
                    double[] periodSinglevalues = period.getPeriodSingleValues();
//                    if (periodvalues == null) {
//                        periodvalues = new double[values.length];
//                        period.setPeriodValues(periodvalues);
//                    }

                    for (int j = 0; j < values.length; j++) {
                        final double theValue = periodvalues[j] + values[j] * percentage;

                        /* Debug:
                        System.out.println("         new value = " + (values[j] + pTotalValues[j] * percentage) + "\t(" + values[j] + " + " + pTotalValues[j] + " * " + percentage + "), Percentage = "
                        + percentageInterval + " x " + percentageTime + ")");
                         */

                        period.setPeriodValue(j, theValue);
                        period.setPeriodSingleValue(j, periodSinglevalues[j] + values[j] * percentage);

                    }
                }
            }
        }
    }




    /**
     * Resets all single value fields to zero.
     * If the array is null it will be initialized.
     * @param periods periods
     * @param initSize initialize size (only used if the array is null!)
     */
    private static void initValues(SpreadPeriod period, int initSize) {

        if (period.isInsideTimespan() && period.getPeriodValues() == null) {
            period.setPeriodValues(new double[initSize]);
        }

        //reset the single value field
        double[] periodSingleValues = period.getPeriodSingleValues();
        if (periodSingleValues == null) {
            period.setPeriodSingleValues(new double[initSize]);
        } else {
            for (int i = 0; i < periodSingleValues.length; i++) {
                periodSingleValues[i] = 0d;
            }
        }
    }



//    @Override
    public void subtractExclusions(SpreadPeriod[] periods, SpreadPeriod timeframe, SpreadPeriod[] exclusions) {

        if (exclusions != null && exclusions.length > 0) {

//            cleanIrrelevantPeriodParts(exclusions, timeframe);

            //TODO ueberschneidende/ueberlappende exclusions mergen (feature wird derzeit nicht implementiert, siehe javadoc)

            // Sum of durations of all exclusions already handled
            long totalExclusionDuration = 0;

            for (int i = 0, cntExcl = exclusions.length; i < cntExcl; i++) {
                SpreadPeriod exclusion = exclusions[i];
                
                if (exclusion != null) {



                exclusion.adjustIntersectionDate(timeframe);


//                org.joda.time.CurveInterval interval1 = new org.joda.time.CurveInterval(exclusion.getStartMillis(), exclusion.getEndMillis());
//
//        org.joda.time.CurveInterval interval2 = new org.joda.time.CurveInterval(timeframe.getStartMillis(), timeframe.getEndMillis());
//        org.joda.time.CurveInterval overlap = interval1.overlap(interval2);
//        if (overlap != null) {
//
//            System.out.println("overlap: "+overlap);
//exclusion.setCalcStartDate(overlap.getStartMillis());
//                exclusion.setCalcFinishDate(overlap.getEndMillis());


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

//                        long overlapDuration = calculateOverlapDuration(exclusion.getCalcStartDate(), exclusion.getCalcFinishDate(), period.getCalcStartDate(), period.getCalcFinishDate());

                        long overlapDuration = period.overlapDuration(exclusion);


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
    }


    /**
     * Sums up all values of the curve.
     * @param curve the curve
     * @return the sum of all curve values
     */
    private static double curveValueSum(double[] curve) {
        double curveValueSum = 0d;
        for (double curveValue : curve) {
            curveValueSum += curveValue;
        }
        return curveValueSum;
    }
}

