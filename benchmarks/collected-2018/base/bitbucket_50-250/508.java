// https://searchcode.com/api/result/116594593/

/*
 * AdroitLogic UltraESB Enterprise Service Bus
 *
 * Copyright (c) 2010-2015 AdroitLogic Private Ltd. (http://adroitlogic.org). All Rights Reserved.
 *
 * GNU Affero General Public License Usage
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program (See LICENSE-AGPL.TXT).
 * If not, see http://www.gnu.org/licenses/agpl-3.0.html
 *
 * Commercial Usage
 *
 * Licensees holding valid UltraESB Commercial licenses may use this file in accordance with the UltraESB Commercial
 * License Agreement provided with the Software or, alternatively, in accordance with the terms contained in a written
 * agreement between you and AdroitLogic.
 *
 * If you are unsure which license is appropriate for your use, or have questions regarding the use of this file,
 * please contact AdroitLogic at info@adroitlogic.com
 */

/**
 * PercentileIterator.java
 * Written by Gil Tene of Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * @author Gil Tene
 * @version 1.1.2
 */

package org.adroitlogic.metrics.histogram;

import java.util.Iterator;

/**
 * Used for iterating through histogram values according to percentile levels. The iteration is
 * performed in steps that start at 0% and reduce their distance to 100% according to the
 * <i>percentileTicksPerHalfDistance</i> parameter, ultimately reaching 100% when all recorded histogram
 * values are exhausted.
*/
public class PercentileIterator extends AbstractHistogramIterator implements Iterator<HistogramIterationValue> {
    int percentileTicksPerHalfDistance;
    double percentileLevelToIterateTo;
    double percentileLevelToIterateFrom;
    boolean reachedLastRecordedValue;

    /**
     * Reset iterator for re-use in a fresh iteration over the same histogram data set.
     *
     * @param percentileTicksPerHalfDistance The number of iteration steps per half-distance to 100%.
     */
    public void reset(int percentileTicksPerHalfDistance) {
        reset(histogram, percentileTicksPerHalfDistance);
    }

    private void reset(final AbstractHistogram histogram, final int percentileTicksPerHalfDistance) {
        super.resetIterator(histogram);
        this.percentileTicksPerHalfDistance = percentileTicksPerHalfDistance;
        this.percentileLevelToIterateTo = 0.0;
        this.percentileLevelToIterateFrom = 0.0;
        this.reachedLastRecordedValue = false;
    }

    PercentileIterator(final AbstractHistogram histogram, final int percentileTicksPerHalf) {
        reset(histogram, percentileTicksPerHalf);
    }

    @Override
    public boolean hasNext() {
        if (super.hasNext())
            return true;
        // We want one additional last step to 100%
        if (!reachedLastRecordedValue && (arrayTotalCount > 0)) {
            percentileLevelToIterateTo = 100.0;
            reachedLastRecordedValue = true;
            return true;
        }
        return false;
    }

    void incrementIterationLevel() {
        percentileLevelToIterateFrom = percentileLevelToIterateTo;
        long percentileReportingTicks =
                percentileTicksPerHalfDistance *
                        (long) Math.pow(2,
                                (long) (Math.log(100.0 / (100.0 - (percentileLevelToIterateTo))) / Math.log(2)) + 1);
        percentileLevelToIterateTo += 100.0 / percentileReportingTicks;
    }

    boolean reachedIterationLevel() {
        if (countAtThisValue == 0)
            return false;
        double currentPercentile = (100.0 * (double) totalCountToCurrentIndex) / arrayTotalCount;
        return (currentPercentile >= percentileLevelToIterateTo);
    }

    @Override
    double getPercentileIteratedTo() {
        return percentileLevelToIterateTo;
    }

    @Override
    double getPercentileIteratedFrom() {
        return percentileLevelToIterateFrom;
    }
}

