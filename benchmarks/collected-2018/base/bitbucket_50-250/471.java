// https://searchcode.com/api/result/97090587/

package noobbot;

import noobbot.domain.CarPosition;
import noobbot.message.Clock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by vtajzich
 */
public class Tachometer implements GameTickAware {

    int pieceIndex;
    int startTime;
    double lastPosition;
    double startSpeed;
    double speed;
    double acceleration;
    double lastKnownDeceleration;
    double distancePerTick;

    List<Double> lastDecelerations = new ArrayList<>(50);

    final Clock clock;

    public Tachometer(Clock clock) {
        this.clock = clock;
    }

    void start() {
        startTime = clock.getTime();
        pieceIndex = -1;
    }

    public void onTick(CarPosition carPosition) {

        int clockTime = clock.getTime();

        int deltaTime = clockTime - startTime;

        if (clockTime == 0 || deltaTime == 0) {
            return;
        }

        startTime = clockTime;

        distancePerTick = carPosition.getPiecePosition().getInPieceDistance() - lastPosition;

        lastPosition = carPosition.getPiecePosition().getInPieceDistance();

        int currentPieceIndex = carPosition.getPiecePosition().getPieceIndex();

        if (pieceIndex == -1) {
            pieceIndex = currentPieceIndex;
        } else if (currentPieceIndex != pieceIndex) {
            pieceIndex = carPosition.getPiecePosition().getPieceIndex();
            return;
        }

        double speedInTick = distancePerTick / deltaTime;

        acceleration = (speedInTick - speed) / deltaTime;

        if (acceleration < 0 && Math.abs(acceleration) > 0.1) {

            double currentDeceleration = acceleration * -1;

            if (lastDecelerations.size() == 50) {
                lastKnownDeceleration = calculateDecelerationMedian();
                lastDecelerations.clear();
            }

            lastDecelerations.add(currentDeceleration);
        }

        speed = startSpeed + (acceleration * deltaTime);

        startSpeed = speed;

        System.out.println("Speed: " + speed + ", acceleration: " + acceleration + ", bp: " + getKnownDecelerationPower());
    }

    public double getKnownDecelerationPower() {

        if (lastKnownDeceleration == 0d) {
            return calculateDecelerationMedian();
        }

        return lastKnownDeceleration;
    }

    private Double calculateDecelerationMedian() {

        List<Double> medians = lastDecelerations.stream()
                .collect(Collectors.groupingBy(d -> Math.round(d)))
                .values().stream()
                .reduce((l, r) -> {
                    if (l.size() < r.size()) {
                        return r;
                    } else {
                        return l;
                    }
                }).orElseGet(() -> Collections.emptyList());


        if (!medians.isEmpty()) {
            return medians.get(0);
        } else {
            return 0.05;
        }
    }


    public double getAcceleration() {
        return acceleration;
    }

    public double getSpeed() {
        return speed;
    }

    public double getDistancePerTick() {
        return distancePerTick;
    }
}

