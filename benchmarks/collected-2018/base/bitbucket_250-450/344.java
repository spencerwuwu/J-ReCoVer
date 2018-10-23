// https://searchcode.com/api/result/97090594/

package noobbot;

import noobbot.domain.CarPosition;
import noobbot.domain.internal.TrackElement;
import noobbot.message.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by vtajzich
 */
public class TrackControlUnit implements GameTickAware {

    static enum Trend {
        NONE, INCREESING, DECREESING
    }

    Map<ThrottleTeKey, CarBot.ThrottleSpeed> throttleForTrackElements = new HashMap<ThrottleTeKey, CarBot.ThrottleSpeed>();

    Tachometer tachometer;
    RaceTrack raceTrack;
    Clock clock;

    CarBot.ThrottleSpeed throttle;
    CarBot.ThrottleSpeed wantedThrottleSpeed;
    double angleAbs;
    double lastAngle;
    double lastAngleIncrement;
    boolean slowing;
    boolean forceBreak;
    boolean gameStarted;

    Trend angleTrend = Trend.NONE;

    List<Double> angleIncrements = new ArrayList<>();
    private boolean lastMessageWasThrottle = false;
    private SwitchDirection lastSwitch = SwitchDirection.None;
    private CarPosition carPosition;

    public TrackControlUnit() {
    }

    public TrackControlUnit(Tachometer tachometer, RaceTrack raceTrack, Clock clock) {
        this.tachometer = tachometer;
        this.raceTrack = raceTrack;
        this.clock = clock;

        classifyTrack();
    }

    void classifyTrack() {

        AtomicInteger index = new AtomicInteger();

        RaceTrack.Type type = raceTrack.getType();

        raceTrack.getElements().stream().forEach(te -> classifyTrackElement(index.getAndIncrement(), te, type.getMinSpeed(), type.getHighSpeed()));
    }

    private void classifyTrackElement(int index, TrackElement te, double lowSpeed, double highSpeed) {

        TrackElement tePlus1 = getNext(index);
        TrackElement tePlus2 = getNext(index + 1);

        double classifiedHighSpeed = TrackElementClassifier.classify(te, tePlus1);

        if (classifiedHighSpeed > highSpeed) {
            highSpeed = classifiedHighSpeed;
        }

        boolean isNextCurve = te.getLength() < 100 && (tePlus1.isCurve() && tePlus1.getOriginalAngleAbs() > 30);
        boolean dangerIsNear = tePlus2.isCurve() && tePlus2.getRadius() <= 70 && tePlus2.getOriginalAngleAbs() > 40;

        if (te.getOriginalAngleAbs() < 30 && !tePlus1.isCurve() && tePlus1.getLength() >= 170) {
            setSpeedFor(te, highSpeed);
        } else if ((te.isCurve() && te.getOriginalAngleAbs() > 30) || isNextCurve || dangerIsNear) {
            setSpeedFor(te, lowSpeed);
        } else {
            setSpeedFor(te, highSpeed);
        }
    }

    private TrackElement getNext(int currIndex) {

        int nextIndex = currIndex + 1;

        if (nextIndex < 0 || nextIndex > raceTrack.getElements().size() - 1) {
            nextIndex = 0;
        }

        return raceTrack.getElements().get(nextIndex);
    }

    private void setSpeedFor(TrackElement te, double speed) {
        te.getPieces().stream().forEach(p -> throttleForTrackElements.put(new ThrottleTeKey(te, p), new CarBot.ThrottleSpeed(0.5, speed)));
    }

    public void onTick(CarPosition carPosition) {

        this.carPosition = carPosition;

        lastAngle = angleAbs;
        angleAbs = carPosition.getAngleAbs();
        throttle = throttleForTrackElements.get(raceTrack.getThrottleTeKey());
        Curve curve = raceTrack.getCurrentElement().getCurve();

        if (angleAbs <= 1d) {
            angleTrend = Trend.NONE;
        } else if (angleAbs > lastAngle) {
            angleTrend = Trend.INCREESING;
        } else {
            angleTrend = Trend.DECREESING;
        }

        lastAngleIncrement = angleAbs - lastAngle;

        if (angleTrend == Trend.INCREESING && ((angleAbs > lastAngle && forceBreak(curve, Math.abs(lastAngleIncrement))) || lastAngleIncrement > 2d && angleAbs >= 40d)) {
            forceBreak = true;
            angleIncrements.clear();
        } else {
            angleIncrements.add(lastAngleIncrement);
        }

        //TODO: sledovat angleIncrement a podle trendu zrychlovat

        wantedThrottleSpeed = throttleForTrackElements.get(raceTrack.getNextThrottleTeKey());
    }

    List<AbstractMessage> control() {

        TrackElement currentTrackElement = raceTrack.getCurrentElement();
        TrackElement nexTrackElement = raceTrack.getNextElement();

        if (!gameStarted) {
            currentTrackElement.setCilovaRovinka(true);
            throttleForTrackElements.entrySet().stream()
                    .filter(entry -> entry.getKey().getTrackElement().equals(currentTrackElement))
                    .forEach(entry -> entry.getValue().setSpeed(8d));
            gameStarted = true;
        }

        List<AbstractMessage> messages = new ArrayList<>();

        if (tachometer.getSpeed() < 2d) {
            messages.add(new ThrottleMessage(1));
            return messages;
        }

        controlSwitchLane(messages);

        double lengthLeft = raceTrack.getLengthTillNextCurve();

        double wantedSpeed = wantedThrottleSpeed.getSpeed();

        int timeLeft = new DecelerationUnit(tachometer.getSpeed(), wantedSpeed, tachometer.getKnownDecelerationPower(), lengthLeft, tachometer.getDistancePerTick()).timeLeft();

        System.out.println("params: " + tachometer.getSpeed() + ", " + wantedSpeed + ", " + tachometer.getKnownDecelerationPower() + ", " + lengthLeft + ", " + tachometer.getDistancePerTick());
        System.out.println("Time left before curve: " + timeLeft);

        if (mustBreak(currentTrackElement)) {
            forceBreak = false;
            System.out.println("FORCE BREAK");
            messages.add(new ThrottleMessage(0.0));
            return updateMessages(messages);
        }

        if (noTimeLeft(timeLeft)) {

            slowing = true;
            messages.add(new ThrottleMessage(0.0));
            return updateMessages(messages);

        } else if (justStraightAhead(currentTrackElement, nexTrackElement)) {
            messages.add(new ThrottleMessage(1));
            return updateMessages(messages);
        } else if (slowEnough()) {
            slowing = false;
        }

        if (slowing) {
            messages.add(new ThrottleMessage(0.0));
            return updateMessages(messages);
        }

        if (!isDecreesing()) {
            if (lastAngleIncrement > 2d && angleAbs > 20 && angleAbs < 35d) {
                throttle.throttle = 0.5;
            } else if (lastAngleIncrement <= 1.1d && angleAbs > 40d) {
                throttle.throttle = 0.7;
            } else if (angleAbs > 50d) {
                throttle.throttle = 0.0d;
            }
        }

        if (raceTrack.isCurve()) {
            throttle.throttle += 0.05;
        } else {
            throttle.throttle += 0.1;
        }

        if (angleAbs < 45d) {
            throttle.addSpeed(tachometer.getSpeed());
        }

        throttle.addAngle(angleAbs);
        throttleForTrackElements.put(raceTrack.getThrottleTeKey(), throttle);

        lastMessageWasThrottle = true;
        messages.add(new ThrottleMessage(throttle.getThrottle()));
        return updateMessages(messages);
    }

    private boolean slowEnough() {
        return (slowing && tachometer.getSpeed() <= wantedThrottleSpeed.getSpeed()) || angleTrend == Trend.DECREESING;
    }

    private boolean justStraightAhead(TrackElement currentTrackElement, TrackElement nexTrackElement) {
        return !currentTrackElement.isCurve() && !nexTrackElement.isCurve();
    }

    private boolean noTimeLeft(int timeLeft) {
        return timeLeft <= 0 || (tachometer.getSpeed() > (wantedThrottleSpeed.getSpeed() * 1.5) && wantedThrottleSpeed.getMaxAngle() > 30d);
    }

    private boolean mustBreak(TrackElement currentTrackElement) {
        return forceBreak || (tachometer.getSpeed() > (throttle.getSpeed() * 1.3) && currentTrackElement.curveFactor() > 1.8);
    }

    private void controlSwitchLane(List<AbstractMessage> messages) {
        if (raceTrack.shouldSwitch() != SwitchDirection.None && raceTrack.shouldSwitch() != lastSwitch && lastMessageWasThrottle) {
            lastMessageWasThrottle = false;
            lastSwitch = SwitchDirection.None;
            messages.add(0, new SwitchLane(raceTrack.shouldSwitch()));
        }
    }

    boolean isDecreesing() {
        return angleTrend == Trend.DECREESING || angleTrend == Trend.NONE;
    }

    private List<AbstractMessage> updateMessages(List<AbstractMessage> messages) {
        updateTick(messages, clock.getTime());
        return leaveOnlyLowestThrottleMessage(messages);
    }

    protected List<AbstractMessage> leaveOnlyLowestThrottleMessage(List<AbstractMessage> messages) {

        ThrottleMessage lowestThrottle = (ThrottleMessage) messages.stream().filter(m -> m instanceof ThrottleMessage).reduce((l, r) -> {

            ThrottleMessage left = (ThrottleMessage) l;
            ThrottleMessage right = (ThrottleMessage) r;

            if (left.getValue() < right.getValue()) {
                return left;
            } else {
                return right;
            }

        }).get();

        if (raceTrack != null) {
            throttleForTrackElements.get(raceTrack.getThrottleTeKey()).setThrottle(lowestThrottle.getValue());
        }

        List<AbstractMessage> result = messages.stream().filter(m -> m.getClass() != ThrottleMessage.class).collect(Collectors.toList());
        result.add(lowestThrottle);

        return result;
    }

    private void updateTick(List<AbstractMessage> messages, int time) {
        messages.stream().forEach(m -> m.setGameTick(time));
    }

    boolean forceBreak(Curve curve, double angleIncrement) {

        if (angleIncrement > 3.5d) {
            System.out.println("Increment large than 3.5, BREAKING!");
            return true;
        }

        switch (curve) {

            case Kurva_Ostra:
                return angleIncrement > 1.5d;
            case Hodne_Ostra:
                return angleIncrement > 2d;
            case Ostra:
                return angleIncrement > 3d;
            case Mirna:
            case Zadna:
            default:
                return false;
        }
    }

    public void reClassifyTrack() {

        ArrayList<Integer> solvedIndexes = new ArrayList<>();

        List<TrackElement> elements = raceTrack.getElements();

        AtomicInteger entryIndex = new AtomicInteger();

        throttleForTrackElements.entrySet().stream().forEach(entry -> {

            TrackElement trackElement = entry.getKey().getTrackElement();
            int index = elements.indexOf(trackElement);
            int nextIndex = IndexUtil.getNextIndex(elements, index);

            if (solvedIndexes.contains(index)) {
                return;
            } else {
                solvedIndexes.add(index);
            }

            TrackElement nextTrackElement = elements.get(nextIndex);

            CarBot.ThrottleSpeed throttleSpeed = entry.getValue();

            double currentSpeed = throttleSpeed.getSpeed();

            Map.Entry<ThrottleTeKey, CarBot.ThrottleSpeed> nextEntry = (Map.Entry<ThrottleTeKey, CarBot.ThrottleSpeed>) throttleForTrackElements.entrySet().toArray()[entryIndex.getAndIncrement()];

            if (nextEntry.getValue().getMaxAngle() > 56d) {
                classifyTrackElement(index, trackElement, currentSpeed * 0.4, currentSpeed * 0.6);
                return;
            } else if (nextEntry.getValue().getMaxAngle() > 52d) {
                classifyTrackElement(index, trackElement, currentSpeed * 0.5, currentSpeed * 0.7);
                return;
            }

            if (trackElement.curveFactor() > 2.5 || nextTrackElement.curveFactor() > 2.5) {
                return;
            }

            switch (trackElement.getCurve()) {

                case Kurva_Ostra:
                    doClassify(throttleSpeed, index, 1.15, 1.1, 1.05, 0.7, 0.5);
                    break;
                case Hodne_Ostra:
                    doClassify(throttleSpeed, index, 1.2, 1.1, 1.05, 1, 0.5);
                    break;
                case Ostra:
                    doClassify(throttleSpeed, index, 1.3, 1.2, 1.1, 1, 0.5);
                    break;
                case Mirna:
                case Zadna:
                default:
                    doClassify(throttleSpeed, index, 1.6, 1.5, 1.3, 1.1, 0.5);
                    break;
            }
        });
    }

    private void doClassify(CarBot.ThrottleSpeed throttleSpeed, int index, double tenFactor, double twentyFactor, double thirtyFactor, double fourtyFactor, double fiftyFactor) {

        double currentSpeed = throttleSpeed.getSpeed();
        double newLowSpeed = currentSpeed;
        double newHighSpeed = 0;

        List<TrackElement> elements = raceTrack.getElements();

        if (throttleSpeed.getMaxAngle() < 10) {
            newHighSpeed = currentSpeed * tenFactor;
        } else if (throttleSpeed.getMaxAngle() < 20) {
            newHighSpeed = currentSpeed * twentyFactor;
        } else if (throttleSpeed.getMaxAngle() < 30) {
            newHighSpeed = currentSpeed * thirtyFactor;
        } else if (throttleSpeed.getMaxAngle() < 40) {
            newHighSpeed = currentSpeed * fourtyFactor;
        } else if (throttleSpeed.getMaxAngle() > 50) {

            //FIXME: neco podobneho.... :-)
            int prevIndex = IndexUtil.getPrevIndex(elements, index);

            classifyTrackElement(prevIndex, elements.get(prevIndex), newLowSpeed * 0.5, newHighSpeed * 0.6);

            newHighSpeed = currentSpeed * fiftyFactor;
        }

        classifyTrackElement(elements.indexOf(index), elements.get(index), newLowSpeed, newHighSpeed);
    }
}

