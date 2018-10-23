// https://searchcode.com/api/result/1618320/

/*
 * Copyright 2011 Sikirulai Braheem <sbraheem at bramosystems.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bramosystems.oss.player.core.client;

import com.bramosystems.oss.player.util.client.RegExp;
import com.bramosystems.oss.player.util.client.RegExp.RegexException;

/**
 * Utility class with methods to manipulate playback time.
 *
 * @author Sikirulai Braheem <sbraheem at bramosystems.com>
 * @since 1.3
 */
public final class PlayTime implements Comparable<PlayTime> {

    private static RegExp regexp;
    private double ms;

    /**
     * Creates a PlayTime object
     */
    public PlayTime() {
    }

    /**
     * Creates a PlayTime object set to the specified time
     * 
     * @param hour the hour
     * @param minute the minute
     * @param second the seconds
     * @param fract the hundredths of a second
     */
    public PlayTime(int hour, int minute, int second, int fract) {
        ms = fract + (second * 1000) + (minute * 60000) + (hour * 3600000);
    }

    /**
     * Creates a PlayTime object set to the specified milliseconds
     * 
     * @param milliseconds time in milliseconds
     */
    public PlayTime(double milliseconds) {
        ms = milliseconds;
    }

    /**
     * Creates a PlayTime object set to the specified time.
     * 
     * <p>{@code time} should be in the format {@code hour:min:sec.fract}. {@code hour}
     * and {@code fract} may be omitted unless required.
     * 
     * 
     * @param time the time
     */
    public PlayTime(String time) {
        try {
            regexp = RegExp.getRegExp("((\\d+):)?(\\d\\d):(\\d\\d)(\\.(\\d+))?", "");
            RegExp.RegexResult m = regexp.exec(time);
            for (int i = 1; i <= 6; i++) {
                String val = m.getMatch(i);
                if (val != null) {
                    switch (i) {
                        case 2:
                            setHour(Integer.parseInt(val));
                            break;
                        case 3:
                            setMinute(Integer.parseInt(val));
                            break;
                        case 4:
                            setSecond(Integer.parseInt(val));
                            break;
                        case 6:
                            setFract(Integer.parseInt(val));
                            break;
                    }
                }
            }
        } catch (RegexException ex) {
        }
    }

    /**
     * Adds {@code millseconds} to the current play time
     * 
     * @param milliseconds the milliseconds to add
     * @return the updated play time
     */
    public PlayTime add(int milliseconds) {
        ms += milliseconds;
        return this;
    }

    /**
     * Reduces the current play time by {@code millseconds}
     *  
     * @param milliseconds the milliseconds to reduce
     * @return the updated play time
     */
    public PlayTime reduce(int milliseconds) {
        ms -= milliseconds;
        return this;
    }

    /**
     * Returns the current PlayTime in milliseconds
     * 
     * @return play time in milliseconds
     */
    public double getTime() {
        return ms;
    }

    /**
     * Returns the hour represented by this PlayTime object
     * 
     * @return the hour
     */
    public int getHour() {
        return (int) ((ms / 3600000) % 60);
    }

    /**
     * Sets the hour of this PlayTime object
     * 
     * @param hour the hour
     */
    public void setHour(int hour) {
        ms = getFract() + (getSecond() * 1000) + (getMinute() * 60000) + (hour * 3600000);
    }

    /**
     * Returns the minute represented by this PlayTime object
     * 
     * @return the minute
     */
    public int getMinute() {
        return (int) ((ms / 60000) % 60);
    }

    /**
     * Sets the minute of this PlayTime object
     * 
     * @param minute the minute
     */
    public void setMinute(int minute) {
        ms = getFract() + (getSecond() * 1000) + (minute * 60000) + (getHour() * 3600000);
    }

    /**
     * Returns the seconds represented by this PlayTime object
     * 
     * @return the seconds
     */
    public int getSecond() {
        return (int) ((ms / 1000) % 60);
    }

    /**
     * Sets the seconds of this PlayTime object
     * 
     * @param second the seconds
     */
    public void setSecond(int second) {
        ms = getFract() + (second * 1000) + (getMinute() * 60000) + (getHour() * 3600000);
    }

    /**
     * Returns the hundredths of a second represented by this PlayTime object
     * 
     * @return the hundredths of a second
     */
    public int getFract() {
        return (int) (ms % 1000);
    }

    /**
     * Sets the hundredths of a second of this PlayTime object
     * 
     * @param fract the hundredths of a second
     */
    public void setFract(int fract) {
        ms = fract + (getSecond() * 1000) + (getMinute() * 60000) + (getHour() * 3600000);
    }

    /**
     * Returns the string representation of this object in full-format
     * 
     * @return the string representation of the time
     * @see #toString(boolean)
     */
    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * Returns the string representation of this object
     * 
     * <p>The string representation is either in its full format (i.e. {@code 00:00:00.000}) or in the
     * short form.  The short form excludes the hour (if hour is less than 1) and hundredths parts. 
     * 
     * @param fullFormat {@code true} to represent this object in the full format, {@code false} otherwise
     * 
     * @return the string representation
     */
    public String toString(boolean fullFormat) {
        int hr = getHour();
        if (fullFormat) {
            return pad(hr) + ":" + pad(getMinute()) + ":" + pad(getSecond()) + "." + getFract();
        } else {
            return (hr > 0 ? hr + ":" : "") + pad(getMinute()) + ":" + pad(getSecond());
        }
    }

    private String pad(int value) {
        if (value < 10) {
            return "0" + value;
        }
        return String.valueOf(value);
    }

    @Override
    public int compareTo(PlayTime o) {
        return Double.compare(ms, o.ms);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlayTime other = (PlayTime) obj;
        if (this.ms != other.ms) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 41 * 7 + (int) (this.ms * 32);
    }
}

