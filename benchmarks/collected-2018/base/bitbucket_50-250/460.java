// https://searchcode.com/api/result/97090557/

package noobbot;

import noobbot.domain.CarPosition;
import noobbot.domain.Race;
import noobbot.message.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by vtajzich
 */
public class CarBot extends RacingCar {

    Clock clock;
    Tachometer tachometer;
    TrackControlUnit trackControlUnit;
    TurboUnit turboUnit;

    Race race;
    RaceTrack raceTrack;

    public CarBot(String color, String name, Logger logger) {
        super(color, name, logger);

        clock = new Clock();
        tachometer = new Tachometer(clock);
    }

    public List<AbstractMessage> onMessage(AbstractMessage message) {

        logger.fromServer(message);

        List<AbstractMessage> response = new ArrayList<AbstractMessage>();

        if (message == null || message.getMsgType() == null) {
            response.add(new PingMessage());
            return response;
        }

        switch (message.getMsgType()) {

            case gameInit:
                processGameInit((GameInitMessage) message);
                break;
            case gameStart:
                response.add(new ThrottleMessage(1));
                break;
            case turboAvailable:
                System.out.println("turbo available");
                turboUnit.onTick(((TurboAvailableMessage) message).getData());
                break;
            case carPositions:
                response = updateOnCarPosition((CarPositionsMessage) message);
                break;
            case lapFinished:
                trackControlUnit.reClassifyTrack();
                break;
            case gameEnd:
                break;
            case spawn:
                response.add(new ThrottleMessage(1));
                break;
            case crash:
                System.out.println("Crash!");
            case UNKNOWN:
            default:
                response.add(new PingMessage());
        }

        response.stream().forEach(m -> logger.toServer(m));

        return response;
    }

    void processGameInit(GameInitMessage gameInitMessage) {

        race = gameInitMessage.getData().getRace();

        initTrack();

        trackControlUnit = new TrackControlUnit(tachometer, raceTrack, clock);
        turboUnit = new TurboUnit(raceTrack);

        System.out.println("race: " + race);
        System.out.println("race track: " + raceTrack);
    }

    List<AbstractMessage> updateOnCarPosition(CarPositionsMessage message) {

        CarPosition myPosition = message.getData().stream().filter((CarPosition position) -> position.getId().equals(getId())).findFirst().get();

        clock.onTick(message);
        tachometer.onTick(myPosition);
        raceTrack.onTick(myPosition);
        trackControlUnit.onTick(myPosition);

        List<AbstractMessage> messages = trackControlUnit.control();

        return turboUnit.onCarPosition(messages, myPosition);
    }

    private void initTrack() {

        raceTrack = new RaceTrack(race.getTrack().getPieces(), race.getTrack().getLanes(), race.getRaceSession().getLaps());
    }

    public static class ThrottleSpeed {

        List<Double> speeds = new ArrayList<>();
        List<Double> angles = new ArrayList<>();

        double throttle;
        private double speed;

        ThrottleSpeed(double throttle, double speed) {
            this.throttle = throttle;
            this.speed = speed;
        }

        void addSpeed(Double speed) {
            speeds.add(speed);
        }

        void addAngle(Double angle) {
            angles.add(angle);
        }

        Double getMaxAngle() {

            if (angles.isEmpty()) {
                return 0d;
            }

            return angles.stream().mapToDouble(angle -> angle).max().getAsDouble();
        }

        public void setThrottle(double throttle) {
            this.throttle = throttle;
        }

        void setSpeed(Double speed) {
            speeds.clear();
            this.speed = speed;
        }

        public double getSpeed() {

            if (!speeds.isEmpty()) {

                return speeds.stream()
                        .collect(Collectors.groupingBy(d -> Math.round(d)))
                        .values().stream()
                        .reduce((l, r) -> {
                            if (l.size() < r.size()) {
                                return r;
                            } else {
                                return l;
                            }
                        }).get().get(0);

            }

            return speed;
        }

        public double getThrottle() {

            if (throttle > 1) {
                throttle = 1;
            } else if (throttle < 0) {
                throttle = 0;
            }

            return throttle;
        }

        @Override
        public String toString() {
            return "Speed: " + getSpeed() + ", throttle: " + throttle;
        }
    }
}

