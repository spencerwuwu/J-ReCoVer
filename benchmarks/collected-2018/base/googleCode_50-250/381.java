// https://searchcode.com/api/result/4898530/

package org.timepedia.chronoscope.client.render.domain;

import org.timepedia.chronoscope.client.canvas.Layer;
import org.timepedia.chronoscope.client.gss.GssProperties;
import org.timepedia.chronoscope.client.util.ArgChecker;
import org.timepedia.chronoscope.client.util.Interval;

/**
 * Provides functionality for rendering the domain axis ticks in a
 * context-sensitive way depending on the current domain interval.
 *
 * @author Ray Cromwell &lt;ray@timepedia.org&gt;
 * @author Chad Takahashi &lt;chad@timepedia.org&gt;
 */
public abstract class TickFormatter<T> {

  /**
   * Stores the possible "tick steps" that are relevant for a given formatter.
   * For example, when dealing with minutes, people typically expect tick steps
   * of 1, 2, 5, 10, 15, 30.  Whereas for months, people would expect tick steps
   * of 1, 2 (bimonthly), 3 (quarters), and 6 (semiannual).
   */
  protected int[] possibleTickSteps;

  /**
   * A pointer to the next formatter to use when the domain interval to format
   * is too small for this formatter to handle.
   */
  protected TickFormatter<T> subFormatter;

  /**
   * A pointer to this formatter's parent.
   */
  protected TickFormatter<T> superFormatter;

  private final String longestPossibleLabel;

  private double maxLabelWidth = -1;

  private double cachedDomainWidth = Double.NEGATIVE_INFINITY;

  private int cachedMaxTicksForScreen = -1;

  private int cachedIdealTickStep;

  /**
   * Constructs a new formatter.
   *
   * @param longestPossibleLabel Represents the the longest possible label that
   *                             could occur, given the set of all labels for
   *                             this formatter.  For example, if this formatter
   *                             formatted days of the week, then "Saturday"
   *                             should be used, since it is the longest name of
   *                             the 7 days.
   */
  protected TickFormatter(String longestPossibleLabel) {
    ArgChecker.isNotNull(longestPossibleLabel, "longestPossibleLabel");
    this.longestPossibleLabel = longestPossibleLabel;
  }

  /**
   * Attempt to find the optimal tick step (taking into account screen width,
   * tick label width, and domain-context-dependent quantized tick steps).
   */
  public final int calcIdealTickStep(double domainWidth,
      int maxTicksForScreen) {
    boolean isAnswerCached = domainWidth == this.cachedDomainWidth
        && maxTicksForScreen == this.cachedMaxTicksForScreen;

    if (isAnswerCached) {
      return this.cachedIdealTickStep;
    }

    int[] tickSteps = this.possibleTickSteps;
    final double tickDomainInterval = getTickInterval();

    // This is the smallest domain interval possible before the tick labels will
    // start running into each other
    final double minDomainInterval = domainWidth / (double) maxTicksForScreen;

    int idealTickStep = -1;
    for (int i = 0; i < tickSteps.length; i++) {
      int candidateTickStep = tickSteps[i];
      if (((double) candidateTickStep * tickDomainInterval)
          >= minDomainInterval) {
        idealTickStep = candidateTickStep;
        break;
      }
    }
    // TODO: find sensible fallback value if none of the quantized intervals will work
    if (idealTickStep == -1) {
      //throw new RuntimeException("Unable to find suitable tick interval");
      idealTickStep = tickSteps[tickSteps.length - 1];
    }

    cachedDomainWidth = domainWidth;
    cachedMaxTicksForScreen = maxTicksForScreen;
    cachedIdealTickStep = idealTickStep;
    return idealTickStep;
  }

  /**
   * Formats the current tick as a String to be displayed on the domain axis
   * panel.
   */
  public String formatTick() {
    return format();
  }
  
  public String formatCrosshair(T tick){
    return getSubFormatter() == null ? format(tick) : getSubFormatter().format(tick);
  }
  
  /**
   * Formats the parameter tick as a String.
   */
  public abstract String format(T tick);
  
  /**
   * Formats the current tick as a String.
   */
  public abstract String format();

  /**
   * Returns the domain value of the current tick.
   */
  public abstract double getTickDomainValue();

  /**
   * Return the screen width of the largest possible tick label for this
   * formatter.
   */
  public double getMaxTickLabelWidth(Layer layer,
      GssProperties axisProperties) {
    if (maxLabelWidth == -1) {
      maxLabelWidth = layer
          .stringWidth(longestPossibleLabel, axisProperties.fontFamily,
              axisProperties.fontWeight, axisProperties.fontSize);
    }
    return maxLabelWidth;
  }

  /**
   * Returns a suitable sub-tick step size for the given 'primaryTickStep' (i.e.
   * the tick spacing to be used for the labeled ticks).
   */
  public int getSubTickStep(int primaryTickStep) {
    // Subclasses may want to reduce this value to some smaller multiple to 
    // avoid too many subticks.
    return primaryTickStep;
  }

  /**
   * Returns a positive value corresponding to a single tick for this formatter.
   * For example, if this is a day-of-month formatter, then this method would
   * return {@link org.timepedia.chronoscope.client.util.TimeUnit#ms()}.
   */
  public abstract double getTickInterval();

  /**
   * Increments <tt>currTick</tt> by the specified number of tick steps.
   * Subclasses may sometimes need to override this method to modify the actual
   * number of tick steps in order to ensure that the associated tick labels are
   * stable when scrolling.
   *
   * @return the number of tick steps that were *actually* incremented;
   *         typically, this value will be the same as the <tt>numTickSteps</tt>
   *         input parameter, but in the aforementioned subclass override case,
   *         a different value could get returned (the typical case for this is
   *         a date near the end of a month).
   */
  public abstract int incrementTick(int numTickSteps);

  /**
   * Returns true if this formatter is capable of rendering the specified domain
   * width (e.g. '2 years').
   *
   * @param domainWidth The domain width, specified in milliseconds.
   */
  public boolean inInterval(double domainWidth) {
    if (isRootFormatter()) {
      return domainWidth > getTickInterval();
    } else {
      double myTickWidth = getTickInterval();
      double parentTickWidth = superFormatter.getTickInterval();
      return domainWidth > myTickWidth && domainWidth <= parentTickWidth;
    }
  }

  /**
   * Returns true only if this formatter has no subformatter.
   */
  public final boolean isLeafFormatter() {
    return this.subFormatter == null;
  }

  /**
   * Returns true only if this formatter has no superformatter.
   */
  public final boolean isRootFormatter() {
    return this.superFormatter == null;
  }

  /**
   * Quantizes <tt>domainX</tt> to the nearest <tt>tickStep</tt> and sets this
   * quantized value as the current tick.  For example, suppose that: <ul> <li>
   * this is a MonthTickFormatter, <li> <tt>timeStamp = JUN-19-1985:22hrs:36min...</tt>
   * <li> <tt>tickStep = 3</tt> (in this context, '3' refers to 3 months).
   * </ul>
   *
   * This method would then return <tt>APR-1-1985:0hrs:0min, ...</tt>.
   *
   * @param domainX  -The domain value to be quantized
   * @param tickStep - The tick step to which <tt>domainX</tt> will be
   *                 quantized
   */
  public abstract void resetToQuantizedTick(double domainX, int tickStep);

  public TickFormatter<T> getSubFormatter() {
    return subFormatter;
  }

  public TickFormatter<T> getSuperFormatter() {
    return superFormatter;
  }
  
  public boolean isBoundary(int tickStep) {
    return false;
  }
  
  public abstract String getRangeLabel(Interval interval);
  
  public abstract String getRangeLabelCompact(Interval interval);
}

