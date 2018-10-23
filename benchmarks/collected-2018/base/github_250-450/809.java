// https://searchcode.com/api/result/74459560/

package uk.ac.susx.mlcl.lib.eval;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import uk.ac.susx.mlcl.lib.Functions2;
import uk.ac.susx.mlcl.lib.reduce.Reducer;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ConfusionMatrix is an abstract superclass to various implementations of a contingency table between actual and
 * predicted result sets. It provides generic functionality for measuring predictive performance based on a methods that
 * must be implemented by subclasses.
 *
 * @author Hamish Morgan
 */
@Nonnull
@Immutable
@CheckReturnValue
public abstract class ConfusionMatrix<T> {

    private static final String INFO_FORMAT = "%1$s: %2$d instances in %3$d classes%n";
    private static final String RATE_FORMAT = "%1$s: %2$d / %3$d = %4$.2f%%%n";
    private static final String LABEL_RATE_FORMAT = "%1$s %2$s: %3$d / %4$d = %5$.2f%%%n";
    private static final String SCORE_FORMAT = "%1$s %2$s: %3$.2f%%%n";
    private static final String ELEMENT_FORMAT = "%d ";

    public static <T> ConfusionMatrixBuilder<T> builder() {
        return new ConfusionMatrixBuilder<T>();
    }

    public abstract List<T> getLabels();

    @Nonnegative
    public abstract long getCount(T actual, T predicted);

    protected abstract String formatLabel(T label);

    @Nonnegative
    public int size() {
        return getLabels().size();
    }

    @Nonnegative
    public long getPredictedCountFor(final T predictedLabel) {
        double sum = 0;
        for (T actualLabel : getLabels())
            sum += getCount(actualLabel, predictedLabel);
        return (long) sum;
    }

    @Nonnegative
    public long getActualCountFor(final T actualLabel) {
        double sum = 0;
        for (T predictedLabel : getLabels())
            sum += getCount(actualLabel, predictedLabel);
        return (long) sum;
    }

    @Nonnegative
    public long getTrueCount() {
        double sum = 0;
        for (T label : getLabels())
            sum += getTrueCountFor(label);
        return (long) sum;
    }

    @Nonnegative
    public long getFalseCount() {
        return getGrandTotal() - getTrueCount();
    }

    @Nonnegative
    public long getTrueCountFor(final T label) {
        return getCount(label, label);
    }

    @Nonnegative
    public long getFalseCountFor(final T label) {
        return getPredictedCountFor(label) - getTrueCountFor(label);
    }

    @Nonnegative
    public long getGrandTotal() {
        double sum = 0;
        for (T actual : getLabels())
            for (T predicted : getLabels())
                sum += getCount(actual, predicted);
        return (long) sum;
    }

    @Nonnegative
    public double getAccuracy() {
        final long trueCount = getTrueCount();
        if (trueCount == 0)
            return 0;
        final long total = getGrandTotal();
        assert total >= trueCount;
        return getTrueCount() / (double) getGrandTotal();
    }

    /**
     * Get the proportion of instances with the given label that where correctly identified.
     * <p/>
     * <p/>
     * In the case of the positive label in a binary confusion matrix, this statistic is often known as "recall". For
     * the negative label, this statistic is sometimes called "specificity".
     * <p/>
     * The probability an instance was predicted correctly given the ground truth label.
     *
     * @param label
     * @return
     */
    @Nonnegative
    public double getTrueRateFor(final T label) {
        return getTrueCountFor(label) == 0 ? 0 :
                getTrueCountFor(label) / (double) getActualCountFor(label);
    }

    /**
     * Get the proportion of instances with the given label that where incorrectly identified.
     *
     * @param label
     * @return
     */
    @Nonnegative
    public double getFalseRateFor(final T label) {
        return getFalseCountFor(label) == 0 ? 0 :
                getFalseCountFor(label) / (double) getActualCountFor(label);
    }

    /**
     * Get the proportion of instances, predicted as having the label, which where correct.
     * <p/>
     * In the case of the positive label in a binary confusion matrix, this statistic is commonly known as "precision".
     *
     * @param label
     * @return
     */
    @Nonnegative
    public double getPredictiveValueFor(final T label) {
        return getTrueCountFor(label) == 0 ? 0 :
                getTrueCountFor(label) / (double) getPredictedCountFor(label);
    }

    /**
     * Get the proportion of instances, predicted as having the label, which where incorrect.
     *
     * @param label
     * @return
     */
    @Nonnegative
    public double getFalseDiscoveryRate(final T label) {
        return getFalseCountFor(label) == 0 ? 0 :
                getFalseCountFor(label) / (double) getPredictedCountFor(label);
    }

    /**
     * Calculate the F-Beta score, measuring the precision and recall of the given label vs all other labels.
     *
     * @param label
     * @param beta
     * @return
     */
    @Nonnegative
    public double getFScoreFor(final T label, double beta) {
        checkArgument(beta >= 0, "beta < 0");
        if (beta == Double.POSITIVE_INFINITY) {
            return getTrueRateFor(label);
        } else if (beta == 0) {
            return getPredictiveValueFor(label);
        } else {
            final double precision = getPredictiveValueFor(label);
            final double recall = getTrueRateFor(label);

            if (precision == 0 && recall == 0) {
                return 0;
            } else {
                return (1.0 + beta * beta) * precision * recall / (beta * beta * precision + recall);
            }
        }
    }

    public BinaryConfusionMatrix<String> mapAllVersusOne(final T srcPositiveLabel, final Reducer<Double, Double> reducer) {


        final String targetPositiveLabel = formatLabel(srcPositiveLabel);
        final String targetNegativeLabl = "Other";
        final Function<String, String> targetLabelFormatter = Functions.identity();

        final Function<T, String> labelMapping =
                Functions2.forPredicate(Predicates.equalTo(srcPositiveLabel), targetPositiveLabel, targetNegativeLabl);
        final Comparator<String> labelOrder = new BinaryMatrixLabelOrder<String>(targetPositiveLabel);
        return (BinaryConfusionMatrix<String>) mapLabels(labelMapping, reducer, labelOrder, targetLabelFormatter);
    }

    public <D> BinaryConfusionMatrix<D> mapAllVersus(
            final Predicate<T> positive, final D positiveLabel, final D negativeLabel,
            final Function<D, String> labelFormatter, final Reducer<Double, Double> reducer) {
        final Function<T, D> labelMapping = Functions2.forPredicate(positive, positiveLabel, negativeLabel);
        final Comparator<D> labelOrder = new BinaryMatrixLabelOrder<D>(positiveLabel);
        return (BinaryConfusionMatrix<D>) mapLabels(labelMapping, reducer, labelOrder, labelFormatter);
    }

    public BinaryConfusionMatrix<String> mapAllVersus(
            final Predicate<T> positive, final String positiveLabel, final String negativeLabel,
            final Reducer<Double, Double> reducer) {
        return mapAllVersus(positive, positiveLabel, negativeLabel, Functions.<String>identity(), reducer);
    }

    /**
     * Remap the matrix labels to produce a new confusion matrix. If source labels are merged (i.e two or more
     * source labels map to one destination label) the counts are summed.
     *
     * @param mapping
     * @return
     */
    public <D> ConfusionMatrix<D> mapLabels(
            final Function<T, D> mapping,
            final Reducer<Double, Double> reducer,
            final Comparator<D> dstLabelOrder,
            final Function<D, String> targetLabelFormatter) {

        ConfusionMatrixBuilder<D> builder = ConfusionMatrix.builder();
        builder.setLabelOrder(dstLabelOrder);
        builder.setLabelFormat(targetLabelFormatter);
        builder.addAllLabels(Sets.newHashSet(Lists.transform(Lists.newArrayList(getLabels()), mapping)));
        for (T predicted : getLabels())
            for (T actual : getLabels()) {
                builder.addResults(
                        mapping.apply(actual),
                        mapping.apply(predicted),
                        (int) getCount(actual, predicted));
            }
        return builder.build();
//
//
//
//
//
//        final List<D> dstLabels = ImmutableList.copyOf(Sets.newHashSet(Lists.transform(Lists.newArrayList(getLabels()), mapping)));
//
//
//        final BiMap<D, Integer> dstLabelIndexMap = ImmutableBiMap.copyOf(EvalUtil.indexMap(dstLabels, dstLabelOrder));
//        final SimpleMatrix targetMat = new SimpleMatrix(dstLabelIndexMap.size(), dstLabelIndexMap.size());
//
//        for (T srcXLabel : getLabels())
//            for (T srcYLabel : getLabels()) {
//                final int dstIndex = targetMat.getIndex(
//                        dstLabelIndexMap.get(mapping.apply(srcYLabel)),
//                        dstLabelIndexMap.get(mapping.apply(srcXLabel)));
//                targetMat.set(dstIndex, reducer.foldIn(targetMat.get(dstIndex), (double) getCount(srcYLabel, srcXLabel)));
//            }
//        return new ConcreteEJMLConfusionMatrix<D>(dstLabelIndexMap, targetMat, dstLabelOrder, targetLabelFormatter);
    }

    public final String getStatsString() {
        return getStatsString(Locale.getDefault());
    }

    public final String getStatsStringFor(final T label) {
        return getStatsStringFor(label, Locale.getDefault());
    }

    public final String getTableString() {
        return getTableString(Locale.getDefault());
    }

    public final String getStatsString(final Locale locale) {
        final StringBuilder builder = new StringBuilder();
        appendStats(builder, locale);
        return builder.toString();
    }

    public final String getStatsStringFor(final T label, final Locale locale) {
        final StringBuilder builder = new StringBuilder();
        appendStatsFor(label, builder, locale);
        return builder.toString();
    }

    public final String getTableString(final Locale locale) {
        final StringBuilder builder = new StringBuilder();
        appendTable(builder, locale);
        return builder.toString();
    }

    public void appendStats(Appendable dst, Locale locale) {
        new Formatter(dst, locale)
                .format(INFO_FORMAT, this.getClass().getSimpleName(), getGrandTotal(), size())
                .format(RATE_FORMAT, "Accuracy", getTrueCount(), getGrandTotal(), 100d * getAccuracy());
    }

    public void appendStatsFor(T label, Appendable dst, Locale locale) {
        new Formatter(dst, locale)
                .format(LABEL_RATE_FORMAT, formatLabel(label), "Precision",
                        getTrueCountFor(label), getPredictedCountFor(label), 100d * getPredictiveValueFor(label))
                .format(LABEL_RATE_FORMAT, formatLabel(label), "Recall",
                        getTrueCountFor(label), getActualCountFor(label), 100d * getTrueRateFor(label))
                .format(SCORE_FORMAT, formatLabel(label), "F1-Score", 100d * getFScoreFor(label, 1.0));
    }

    public void appendTable(Appendable dst, Locale locale) {

        final String[][] cells = new String[size() + 1][size() + 1];
        cells[0][0] = "";

        final List<T> labels = getLabels();
        final int size = labels.size();

        for (int x = 0; x < size; x++)
            cells[0][x + 1] = formatLabel(labels.get(x));
        for (int y = 0; y < size; y++)
            cells[y + 1][0] = formatLabel(labels.get(y));

        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                cells[y + 1][x + 1] = String.format(ELEMENT_FORMAT, getCount(labels.get(y), labels.get(x)));

        // Calculate the max widths for each column
        final int[] widths = new int[cells[0].length];
        for (int y = 0; y < cells.length; y++)
            for (int x = 0; x < cells[0].length; x++)
                widths[x] = Math.max(widths[x], cells[y][x].length());

        // Build a formatter for the whole row
        final Formatter rowFormatter = new Formatter(new StringBuilder(), locale);
        for (int x = 0; x < cells[0].length; x++)
            rowFormatter.format("%%%d$%ds", x + 1, widths[x] + 1);
        rowFormatter.format("%n");

        final String rowFormat = rowFormatter.toString();

        // Write each row
        final Formatter tableFormatter = new Formatter(dst, locale);
        for (int y = 0; y < cells.length; y++)
            tableFormatter.format(rowFormat, cells[y]);
    }

    private static class BinaryMatrixLabelOrder<D> implements Comparator<D> {
        private final D positiveLabel;

        public BinaryMatrixLabelOrder(D positiveLabel) {
            this.positiveLabel = checkNotNull(positiveLabel, "positiveLabel");
        }

        @Override
        public int compare(final D o1, final D o2) {
            return Integer.compare(
                    o1.equals(positiveLabel) ? 0 : 1,
                    o2.equals(positiveLabel) ? 0 : 1);
        }
    }


}

