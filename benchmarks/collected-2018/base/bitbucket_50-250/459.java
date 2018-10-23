// https://searchcode.com/api/result/96991761/

package de.threemonkeys.domain;

import de.threemonkeys.communication.model.outgoing.OutgoingMessage;
import de.threemonkeys.communication.model.outgoing.Ping;
import de.threemonkeys.communication.model.outgoing.SwitchDirection;
import de.threemonkeys.communication.model.outgoing.SwitchLane;
import de.threemonkeys.communication.model.outgoing.Throttle;
import de.threemonkeys.communication.model.outgoing.Turbo;

import java.util.LinkedList;
import java.util.List;

public class QueuedCarCommands implements CarCommands {
    private final List<OutgoingMessage> outgoingCache = new LinkedList<>();
    private static final Ping EMPTY_MESSAGE = new Ping();

    private static final double SPARE_CACHE_THROTTLE_DELTA = 0.01;
    private double throttle;

    @Override
    public OutgoingMessage next() {
        if (outgoingCache.isEmpty()) {
            return EMPTY_MESSAGE;
        } else {
            return outgoingCache.remove(0);
        }
    }

    /*
     * As we can only answer with one message per tick, we have to reduce the
     * number of messages send. Switching the lane AND changing the throttle
     * means two ticks. So we skip throttle messages to keep in sync if the
     * delta is small enough
     */
    @Override
    public void throttle(final double newThrottle) {
        if (outgoingCache.isEmpty() || Math.abs(newThrottle - throttle) >= SPARE_CACHE_THROTTLE_DELTA) {
            throttle = newThrottle;
            add(new Throttle(newThrottle));
        }
    }

    @Override
    public void switchLane(final SwitchDirection direction) {
        addWithPriority(new SwitchLane(direction));
    }

    @Override
    public void turbo() {
        addWithPriority(new Turbo("Wheeee"));
    }

    private void add(final Throttle tmp) {
        outgoingCache.add(tmp);
    }

    private void addWithPriority(final OutgoingMessage command) {
        outgoingCache.add(0, command);
    }

    @Override
    public void clear() {
        outgoingCache.clear();
    }
}

