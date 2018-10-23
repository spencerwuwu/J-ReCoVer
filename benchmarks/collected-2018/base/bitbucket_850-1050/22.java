// https://searchcode.com/api/result/97138823/

/*
 * Copyright 2014 Roberto Badaro (Team: Realize Games)
 * 
 * - Was just for fun ;)
 * 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package realizegames;

import java.util.List;

import realizegames.data.CarId;
import realizegames.data.CarInfo;
import realizegames.data.CarPosition;
import realizegames.data.Lane;
import realizegames.data.Piece;
import realizegames.msg.Msg;
import realizegames.msg.SwitchLaneMsg;
import realizegames.msg.ThrottleMsg;
import realizegames.msg.TurboOnMsg;

public class Bot {
    private static final String TAG = "BOTv2";

    private static final double SHIFT_UP_FACTOR = 0.70;
    private static final double SHIFT_DOWN_FACTOR = 0.50;

    public CarId id;
    public CarInfo mycar;
    public int lapcount;
    public boolean spawn;
    
    public double topSpeed;
    public double maxAc;
    public double maxDeac;
    public double maxAcAng;

    private Cockpit car = new Cockpit();
    
    private boolean switchDone;
    private int currPiece = -1;

    private Lane lane;

    public double prevPosition;

    private double carAngleThreshould;

    private boolean turboAvailable;
    private boolean turboOn;
    private int turboResidual = 0;
    private double turboFactor;
    private int turboDuration;
    private int turboDurationEnd;

    private boolean turboConvenient;
    private boolean saindoCurva;
    
    private boolean moveStarted;

    private int lastPieceIndex;

    public List<Integer> retao;
    
    private SlipAcSensor slipAcSensor;
    private DriftSensor driftSensor;
    
    private boolean pieceChanging = false;
    private boolean pieceChangingExt10 = false;
    

    public Bot(final CarId id) {
        this.id = id;
    }
    
    public void myCar(final CarInfo mycar) {
        this.mycar = mycar;
        slipAcSensor = new SlipAcSensor(1);
        driftSensor = new DriftSensor(carAngleThreshould);
    }

    public Msg prepareForStart() {
        car.reset();
        moveStarted = false;
        
        currPiece = -1;
        prevPosition = -1;
        lapcount = 0;

        switchDone = false;
        spawn = false;
        turboAvailable = false;
        turboOn = false;
        turboConvenient = false;
        saindoCurva = false;

        topSpeed = 0;
        maxAc = 0;
        maxDeac = 0;
        maxAcAng = 0;
        
        fullThrottle();
        return ThrottleMsg.getMsg(car.appForce, null);
    }
    public void crash() {
        car.reset();
        turboConvenient = false;
        turboResidual = 0;
        saindoCurva = false;
    }

    public void spawn() {
        turboConvenient = false;
        spawn = true;
    }
    public void turboAvailable(final double factor, final int duration) {
        turboFactor = factor;
        turboDuration = duration;
        turboAvailable = true;
    }

    private int keep = 0;
    public Msg nextCommand(final CarPosition position,
                           final CarPosition[] allCars,
                           final Piece[] pieces,
                           final Lane[] lanes,
                           final Integer gameTick) {

        lapcount = position.piecePosition.lap;
        
        checkTurbo(gameTick);
        
        final int pieceIndex = position.piecePosition.pieceIndex;
        final Piece piece = pieces[pieceIndex];
        final double currPos = position.piecePosition.inPieceDistance;
        lane = lanes[position.piecePosition.lane.endLaneIndex];
        lastPieceIndex = pieces.length -1;
        
        
        if (currPiece != pieceIndex) {
            currPiece = pieceIndex;
            if (!Logger.isDebugEnabled()) {
                Logger.info(TAG, 
                      "gt: " + gameTick +
                      "; lp: " + lapcount +
                      "; p: " + currPiece +
                      "; p-r: " + piece.radius + 
                      "; p-ang: " + piece.angle +
                      "; ln: " + position.piecePosition.lane.endLaneIndex +
                      "; pos: " + Doubles.truncate(currPos,3) +
                      "; T: " + car.appForce +
                      "; turbo: " + (turboOn ? 1 : 0) +
                      "; vel: " + Doubles.truncate(car.velocity, 3) +
                      "; ang: " + car.slip +
                      "; acAng: " + car.slipSpeed +
                      "; acl: " + Doubles.truncate(car.acceleration,3) +
                      "; t-res: " + turboResidual
                      );
            }
        }
        
        if (prevPosition == -1) {
            // gamestarted
            prevPosition = currPos;
        }
        
        if (currPos == prevPosition && !moveStarted) {
            prevPosition = currPos;
            fullThrottle();
            return ThrottleMsg.getMsg(car.appForce, gameTick);
        }

        double posRef = currPos;
        double adjustedPrevPos = prevPosition;
        if (currPos < prevPosition) {
            final Piece prev = pieces[pieceIndex > 0 ? pieceIndex -1 : lastPieceIndex];
            double plen = prev.length;
            if (prev.isBend()) {
                plen = plen + lane.distanceFromCenter;
            }
            
            if (plen < prevPosition) {
                // apareceu no log...
                adjustedPrevPos = plen - (prevPosition - plen);
            }

            posRef = adjustedPrevPos + (plen - adjustedPrevPos) + currPos;
        }
        
        final double v = posRef - adjustedPrevPos;
        final double angle = Doubles.truncate(position.angle, 3);
            
        if ((currPos > prevPosition) && !moveStarted) {
            moveStarted = true;
            car.acceleration = v;
            car.onMotionStart(gameTick);
        }
        car.update(v, angle, gameTick);
        
        if (car.velocity > topSpeed) {
            topSpeed = car.velocity;
        }
        if (car.acceleration > maxAc) {
            maxAc = car.acceleration;
        }
        if (car.acceleration < maxDeac) {
            maxDeac = car.acceleration;
        }
        if (car.slipAcceleration > maxAcAng) {
            maxAcAng = car.slipAcceleration;
        }
        
        
        prevPosition = currPos;

        final double nextInstantSpeed = car.velocity + car.acceleration;
        
        final Piece _nextPiece = nextPiece(pieceIndex, pieces);
        Piece nextPiece = _nextPiece;
        double nextPos = currPos + nextInstantSpeed;
        
        final double a = (2 * nextInstantSpeed) * (2 + turboResidual);
        final double nextPosExt10 = currPos + a;
        
        pieceChanging = false;
        pieceChangingExt10 = false;
        
        int pieceIndexRef = pieceIndex;
        Piece pieceRef = piece;
        
        if(nextPos > piece.length) {
            pieceChanging = true;
            // proxima posicao dentro da proxima peca
            final double dif = nextPos - piece.length;
            nextPos = dif;
            pieceRef = nextPiece;
            pieceIndexRef = pieceIndex +1;
            if (pieceIndexRef > lastPieceIndex) {
                pieceIndexRef = 0;
            }
            
            nextPiece = nextPiece(pieceIndexRef, pieces);            
        }
        
        if(nextPosExt10 > piece.length) {
            pieceChangingExt10 = true;
        }

        final double nextPieceRadius = TrackUtil.relativeRadius(nextPiece, lane);
        
        saindoCurva = pieceRef.isBend() && (!nextPiece.isBend() || nextPieceRadius > 100) && pieceChangingExt10;

        final double sugestedSpeed = maxSpeedUntilNextBend(currPos, pieceIndex, pieces);
        
        final double slip = Doubles.truncate(car.slip + car.slipSpeed * (piece == pieceRef ? 1 : 1),1);
        final boolean driftRisc = slipAcSensor.updateLimit(piece.slipSpeedThreshold).hasProblem(car.slipSpeed);
        final boolean angleRisc = driftSensor.updateLimit(piece.slipThreshold).hasProblem(slip);
        
        boolean alert = false;
        
        if (keep > 0) {
            if (driftRisc || angleRisc) {
                keep = 0;
            } else {
                keep --;
            }
        }

        final boolean bigProblem = false;// (3 * piece.slipSpeedThreshold) < Math.abs(car.slipSpeed);
        
        if (driftRisc && angleRisc || bigProblem) {
            alert = true;
            car.appForce = 0;
        } else if (driftRisc || angleRisc) {
            alert = true;
            car.appForce = 0;
//            stop();
        }
        
        //@formatter:off
        if (Logger.isDebugEnabled()) {
            Logger.debug(TAG, 
                  "gt: " + gameTick +
                  "; lp: " + lapcount +
                  "; p: " + currPiece +
                  "; p-r: " + piece.radius + 
                  "; p-ang: " + piece.angle +
                  "; ln: " + position.piecePosition.lane.endLaneIndex +
                  "; pos: " + Doubles.truncate(currPos,3) +
                  "; T: " + car.appForce +
                  "; turbo: " + (turboOn ? 1 : 0) +
                  "; vel: " + Doubles.truncate(car.velocity, 3) +
                  " / " + Doubles.truncate(sugestedSpeed,3) +
                  "; ang: " + car.slip +
                  "; acAng: " + car.slipSpeed +
                  "; acl: " + Doubles.truncate(car.acceleration,3) +
                  "; keep: " + keep +
                  "; t-res: " + turboResidual
                  );
        }
        //@formatter:on
        
        if (alert) {
            return ThrottleMsg.getMsg(car.appForce, gameTick);
        }

        if (spawn) {
            spawn = false;
            if (pieceRef.isBend() || nextPiece.isBend()) {
                car.throttle(0.7);
                return ThrottleMsg.getMsg(car.appForce, gameTick);
            } else {
                fullThrottle();
                if (goTurbo(gameTick)) {
                    return TurboOnMsg.getMsg(gameTick);
                }
            }
        }

        if (pieceChanging && pieceRef.isSwitch()) {
            if (!switchDone) {
                final int switchIndex = pieceIndexRef;
                final int nextBendIndex = nextPieceIndexByType(switchIndex, pieces, true, false);
                final Piece nextBend = pieces[nextBendIndex];
                if (nextBend.isBend()) {
                    final int nextSwitchIndex = nextPieceIndexByType(switchIndex, pieces, false, true);
                    final int bestSide =
                        bestLaneToSwitch(switchIndex, nextSwitchIndex, nextBend, nextBendIndex, lane, lanes,
                            allCars);
                    
                    SwitchLaneMsg switchMsg = null;
                    switch (bestSide) {
                    case -1:
                        switchMsg = SwitchLaneMsg.left(gameTick);
                        break;
                    case 1:
                        switchMsg = SwitchLaneMsg.right(gameTick);
                        break;
                    default:
                        // no switch
                        break;
                    }
                    if (switchMsg != null) {
                        switchDone = true;
                        Logger.info(TAG, "SwitchLaneMsg: %s; gameTick: %s", switchMsg.data, gameTick);
                        return switchMsg;
                    }
                }
            }
        } else {
            switchDone = false;
        }


        if (car.velocity <= 1.0) {
            fullThrottle();
        }
        
        if (keep == 0 && goTurbo(gameTick)) {
            fullThrottle();
            return TurboOnMsg.getMsg(gameTick);
        } else {
            return ThrottleMsg.getMsg(car.appForce, gameTick);
        }
    }

    private void checkTurbo(final Integer gameTick) {
        if (gameTick == null) {
            return;
        }
        if (!turboOn && turboResidual > 0) {
            turboResidual --;
            if (turboResidual < 0) {
                turboResidual = 0;
            }
        }
        if (turboOn && gameTick.intValue() > turboDurationEnd) {
            turboOn = false;
            Logger.debug(TAG, "\tTURBO GONE.");
        }
    }

    private boolean goTurbo(final Integer gameTick) {
        if (gameTick == null) {
            return false;
        }
        if (turboConvenient && turboAvailable) {
            turboAvailable = false;
            turboDurationEnd = 1 + gameTick.intValue() + turboDuration;
            turboOn = true;
            turboResidual = turboDuration;
            Logger.debug(TAG, "### GO TURBO!");
            return true;
        }
        return false;
    }

    private int nextPieceIndexByType(final int pieceIndex,
                                  final Piece[] pieces,
                                  final boolean bendType,
                                  final boolean switchType) {
        final int max = pieces.length;
        int next = pieceIndex + 1;
        for (int cnt = 0; cnt < max; cnt++) {
            if (next >= max) {
                next = 0;
            }
            final Piece p = pieces[next];
            if ((bendType && p.isBend()) || (switchType && p.isSwitch())) {
                return next;
            }
            next++;
        }
        Logger.warn(TAG, "## PISTA SEM CURVA ou switch? ##");
        return -1;
    }
    
    private static final Piece BEND_200 = new Piece();
    static {
        BEND_200.angle = 22.0;
        BEND_200.radius = 200;
        BEND_200.speedFactor = 1;
        BEND_200.slipSpeedThreshold = TrackUtil.FAST_BEND_SLIP_SPEED_THRESHOLD;
    }
    
    private double maxSpeedUntilNextBend(final double currPos, final int currPieceIndex, final Piece[] pieces) {

        final Piece currPiece = pieces[currPieceIndex];
        Piece nextBend = null;
        double distance = currPiece.length - currPos;
        final int max = pieces.length;
        int next = currPieceIndex + 1;
        for (int cnt = 0; cnt < max; cnt++) {
            if (next >= max) {
                next = 0;
            }
            final Piece p = pieces[next];
            if (p.isBend() && p.radius < 200.0) {
                nextBend = p;
                break;
            }
            distance += p.length;
            next++;
        }
        if (nextBend == null) {
            nextBend = BEND_200;
        }

        // qtos tiks ate la, na vel atual?
        final double tiks = distance / (car.velocity + car.acceleration); //  + car.acceleration
        // velocidade na curva
        final double velcurva = TrackUtil.bendSpeed(nextBend, lane, car.mass, car.coefFric);
        double velPrevistaAqui = velcurva;
        
        final Piece nextPiece = nextPiece(currPieceIndex, pieces);
        if (nextPiece != nextBend) {
            final double accelerationRef = (car.acceleration > 0 && turboResidual == 0 ? car.acceleration : car.freeAcceleration * 0.40);
//            accelerationRef = accelerationRef * (turboResidual == 0 ? 1.15 : 1/turboFactor); 2014-05-25
            velPrevistaAqui = Doubles.truncate(velcurva + accelerationRef * tiks +1, 3);
        }

        if (currPiece.isBend()) {
            if (nextPiece.isBend() && TrackUtil.relativeRadius(nextPiece, lane) < TrackUtil.relativeRadius(currPiece, lane)) {
                velPrevistaAqui = TrackUtil.bendSpeed(nextPiece, lane, car.mass, car.coefFric);
            } else {
                velPrevistaAqui = TrackUtil.bendSpeed(currPiece, lane, car.mass, car.coefFric);
                velPrevistaAqui = velPrevistaAqui * 1.1;
            }
        } else {
            final int posRetao = retao.indexOf(currPieceIndex);
            turboConvenient = (turboAvailable && (posRetao != -1 && posRetao <= Math.round(retao.size()/4)));//3 //2.5
        }
        
        if (car.velocity > velPrevistaAqui) {
            keep = 0;
            if (turboResidual > 0) {
                car.throttle(0);
            } else {
                stop();
//                car.throttle(car.appForce * 0.1);
            }
            
        } else if (car.velocity < velPrevistaAqui && keep == 0) {
            if (turboOn) {
                final double time = (currPiece.isBend() || nextPiece.isBend() ? 5 : 3);
                final double throttle = car.forceToReachVelocityInTime(car.velocity, velPrevistaAqui * 0.7, time); // *1 // 1.05, 2
                car.throttle(Doubles.truncate(throttle));
                // keep = (int) time;
            } else {
                fullThrottle();
            }
        }

//        if ((currPieceIndex > 0 || lapcount > 0) && nextPiece.isBend() && pieceChangingExt10 &&
//            (!currPiece.isBend() || currPiece.radius > nextPiece.radius)) {
//            // diminui pra entrar na curva
//            turboConvenient = false;
//            if (car.velocity < velPrevistaAqui) {
//                  car.throttle(car.appForce * (car.slip == 0 /*|| nextPiece.radius > 150*/ ? 1 : 0.5));
////                keep = 3;
//            } else {
////                final double reduce = 0.50; //velPrevistaAqui/car.velocity;
////                car.throttle(car.appForce * reduce);
//                if (nextPiece.radius < 150) {
//                    stop();
//                }
//            }
//        }
        
        return velPrevistaAqui;
    }

    private int bestLaneToSwitch(final int switchIndex,
                                 final int nextSwitchIndex,
                                 final Piece nextBend,
                                 final int nextBendIndex,
                                 final Lane lane,
                                 final Lane[] lanes,
                                 final CarPosition[] carPositions) {
        final int myLane = lane.index;
        final int laneLeft = myLane - 1;
        final int laneRight = myLane + 1;
    
        final int angleSide = (nextBend.angle > 0 ? 1 : -1);
        int side = angleSide;
        
        for (final CarPosition cp : carPositions) {
            final int cplane = cp.piecePosition.lane.endLaneIndex;
                final int pos = cp.piecePosition.pieceIndex;
                boolean dentro = false;
                if (nextSwitchIndex < switchIndex) {
                    if (pos < nextSwitchIndex && (pos >= 0 || pos >= switchIndex)) {
                        dentro = true;
                    }
                } else {
                    if (pos < nextSwitchIndex && pos >= switchIndex) {
                        dentro = true;
                    }
                }
                if (!dentro) {
                    continue;
                }
                
                if (cplane == laneLeft) {
                    side ++;
                } else if (cplane == laneRight) {
                    side --;
                } else if (cplane == myLane) {
                    side += angleSide;
                }
        }
        
        if (side == 0) {
            return 0;
        } else {
            int choosenOne = (side < 0 ? -1 : 1);
            final int bendIndex =
                (nextBendIndex > switchIndex ? nextBendIndex : lastPieceIndex + nextBendIndex);
            if (bendIndex == (switchIndex + 1) && nextBend.radius <= 50) {
                choosenOne *= -1;
            }
            
            int laneindex = myLane + choosenOne;
            if (laneindex < 0) {
                laneindex = 0;
            } else if (laneindex >= lanes.length) {
                laneindex = lanes.length -1;
            }
            
            final Lane target = lanes[laneindex];
            if (target.equals(lane)) {
                return 0;
            }
                
            return choosenOne;
        }
    }

    protected Piece nextPiece(final int indexRef, final Piece[] pieces) {
        final int next = indexRef + 1;
        final int dif = next - pieces.length;
        return (next < pieces.length ? pieces[next] : pieces[dif]);
    }

    protected void shiftUp() {
        double factor = SHIFT_UP_FACTOR;
        if (turboResidual > 0) {
            factor = Doubles.truncate(Cockpit.MAX_THROTTLE / turboFactor);
        }
        car.throttle(Doubles.truncate(car.appForce + factor));
    }

    protected void shiftDown() {
        double factor = SHIFT_DOWN_FACTOR;
        if (turboResidual > 0) {
            factor = 1.0;
        }
        car.throttle(Doubles.truncate(car.appForce - factor));
    }

    protected void fullThrottle() {
        car.throttle(Cockpit.MAX_THROTTLE);
    }

    public void stop() {
        if (turboOn || turboResidual > 0) {
            car.appForce = 0; // direto para evitar a checagem.
        } else {
            car.throttle(Cockpit.MIN_THROTTLE);
        }
    }

    
    //
    // Inner classes
    //
    
    public class Sensor {
        public String sensorType;
        public double min;
        public double max;
        public boolean activated;

        public Sensor() {
        }

        public Sensor(final String sensorType, final double min, final double max) {
            this.sensorType = sensorType;
            this.min = min;
            this.max = max;
        }

        public boolean hasProblem(final double currentValue) {
//            double vmin = (turboResidual == 0 ? min : min * (1 - (1 / turboFactor)));
//            double vmax = (turboResidual == 0 ? max : max * (1 - (1 / turboFactor)));
//            if (vmin >= 0) {
//                vmin = 0;
//            }
//            if (vmax <= 0) {
//                vmax = 0;
//            }
            
            activated = currentValue < min || currentValue > max;
            return activated;
        }
    }

    public class SlipAcSensor extends Sensor {
        public SlipAcSensor(final double limit) {
            super("SlipAc-Sensor", limit * -1, limit);
        }

        public SlipAcSensor updateLimit(final double limit) {
            final double vlimit = (turboOn || turboResidual > 0 ? Math.min(1, limit) : limit);
            min = vlimit * -1;
            max = vlimit;
            return this;
        }
    }

    public class DriftSensor extends Sensor {
        private double prev;
        public DriftSensor(final double limit) {
            super("DriftSensor", limit * -1, limit);
        }
        public DriftSensor updateLimit(final double limit) {
            min = limit * -1;
            max = limit;
            return this;
        }
        @Override
        public boolean hasProblem(final double currentValue) {
            super.hasProblem(currentValue);
            final double curr = Doubles.truncate(currentValue, 4);
            if (activated && prev == curr) {
                activated = false;
            }
            prev = curr;
            return activated;
        }
    }
    
 

    public static class Cockpit {

        private static String TAG = "Car";

        public static final double MIN_THROTTLE = 0.1;//0.0
        public static final double MAX_THROTTLE = 1.0;

        public double velocity;
        public double targetVelocity;
        public int timeToKeepVelocity;

        public double slip;
        public double slipSpeed;
        public double slipAcceleration;

        public double normForce;
        public double gravForce;
        public double fricForce;
        // throttle - comeca com 1 (0.0 - 1.0)
        public double appForce;
        public double coefFric;

        public double mass;
        public double acceleration;
        public double netForce;

        // a inicial, de Fr = 0;
        public double freeAcceleration;

        public void onMotionStart(final int ticksToMove) {
            fricForce = 0;
            coefFric = 0;
            // In our case, we know that (-)Fn + (Fg) = 0.
            netForce = Doubles.truncate(-fricForce + appForce, 4);

            mass = 5.0; // Valor se mostrou constante.  Doubles.truncate(netForce / acceleration, 4);
            gravForce = Doubles.truncate(mass * Physics.G, 4);
            normForce = gravForce;

            freeAcceleration = acceleration;

            Logger.info(TAG, "onMotionStart - mass: %s; acc: %s; gravForce: %s; coefFric: %s", mass,
                acceleration, gravForce, coefFric);
        }

        public double timeToReachVelocity(final double f, final double v0, final double v) {
            return Doubles.truncate((mass * Math.abs(v - v0)) / f, 4);
        }

        public double forceToReachVelocityInTime(final double v0, final double v, final double ticks) {
            return Doubles.truncate((mass * Math.abs(v - v0)) / ticks, 4);
        }

        public void reset() {
            velocity = 0.0;
            acceleration = 0.0;
            targetVelocity = 0.0;
            timeToKeepVelocity = 0;
            slip = 0.0;
            slipSpeed = 0.0;
            slipAcceleration = 0.0;
        }

        public double appliedAcceleration() {
            return netForce / mass;
        }

        public void update(final double deltaSpace, final double slipAngle, final Integer gameTick) {
            acceleration = Doubles.truncate(deltaSpace - velocity, 4);
            velocity = Doubles.truncate(deltaSpace, 4);

            slipSpeed = Doubles.truncate(slipAngle - slip, 2);
            slipAcceleration = Doubles.truncate(Math.abs(slipAngle - slipSpeed), 2);
            slip = Doubles.truncate(slipAngle, 2);

            fricForce = Doubles.truncate(freeAcceleration - acceleration, 4);

            coefFric = Doubles.truncate(-fricForce / normForce, 4);

            netForce = Doubles.truncate(-fricForce + appForce + -normForce + gravForce, 4);
        }

        public void throttle(final double nextF) {
            final double f = Doubles.truncate((nextF > 0.0 ? nextF : 0.0), 4);
            if (f < MIN_THROTTLE) {
                appForce = MIN_THROTTLE;
            } else if (f > MAX_THROTTLE) {
                appForce = MAX_THROTTLE;
            } else {
                appForce = f;
            }
        }
    }

    public static class Doubles {

        public static double truncate(final double valor) {
            return truncate(valor, 2);
        }

        public static double truncate(final double valor, final int decimais) {
            final int multiplicador = (int) Math.pow(10d, decimais);
            final int multiplied = (int) (valor * multiplicador);
            final double truncated = multiplied / (double) multiplicador;
            return truncated;
        }

    }

    public static abstract class Physics {

        public static final double G = 10.0; // 9.8;

        public static double bendSpeed(final double ms, final double radius) {
            final double speed = 3.6 * Math.pow(ms * radius, 0.5);
            return Doubles.truncate(speed, 4);
        }

        public static double maxBendSpeedWithNoSlip(final double ms, final double radius) {
            final double speed = Math.sqrt(ms * radius * Physics.G) / 10.0;
            return Doubles.truncate(speed, 4);
        }

        public static double centripedForce(final double mass, final double speed, final double radius) {
            final double fc = (mass * (Math.pow(speed, 2) / radius)); // / 10.0;
            return Doubles.truncate(fc, 4);
        }

        public static double centrifugalForce(final double mass, final double slipSpeed, final double radius) {
            final double fc = (mass * Math.pow(slipSpeed, 2) * radius); // / 10.0;
            return Doubles.truncate(fc, 4);
        }

    }

    public static class TrackUtil {

        public static final double STRAIGHT_SLIP_SPEED_THRESHOLD = 3.0; // 4.0;
        public static final double CLOSED_BEND_SLIP_SPEED_THRESHOLD = 1.0; // 0.5;// 1.0;
        public static final double EASY_BEND_SLIP_SPEED_THRESHOLD = 1.0; // 1.3; //1.2; //1.5; // 1.5
        public static final double FAST_BEND_SLIP_SPEED_THRESHOLD = 1.0; // 2.0; ////1.6; // 2.6

        public static final double DEFAULT_SLIP_THRESHOLD = 0.85; //0.80; // .75
        public static final double A_SLIP_THRESHOLD = 0.75;
        public static final double B_SLIP_THRESHOLD = 0.60;//0.50; //0.40;

        public static double relativeRadius(final Piece piece, final Lane lane) {
            if (piece.radius != 360.0) {
                return piece.radius - lane.distanceFromCenter;
            } else {
                return piece.radius;
            }
        }

        public static double bendSpeed(final Piece piece,
                                       final Lane lane,
                                       final double mass,
                                       final double carFricCoef) {
            double speedFactor = piece.speedFactor;
            final double radius = relativeRadius(piece, lane);
            final double dif = Doubles.truncate(Math.abs(piece.radius - radius) / 100.0);
            if (radius > piece.radius) {
//                speedFactor *= 1.1 + dif;
                speedFactor *= 1.0 + dif;
            } else if (radius < piece.radius) {
//                speedFactor *= 1.1 - dif;
                speedFactor *= 1.0 - dif;
            }
            speedFactor = 1; // 2014-5-25
            final double maxSpeed = Physics.maxBendSpeedWithNoSlip(mass, radius) * speedFactor;
            return maxSpeed;
        }

        public static void calculateThresholds(final Piece[] pieces, final CarInfo mycar) {

            final double maxAngle = (mycar.dimensions.length - mycar.dimensions.guideFlagPosition) * 2;
            final double defaultSlipThreshold = Doubles.truncate(DEFAULT_SLIP_THRESHOLD * maxAngle, 1);
            final double aSlipThreshold = Doubles.truncate(A_SLIP_THRESHOLD * maxAngle, 1);
            final double bSlipThreshold = Doubles.truncate(B_SLIP_THRESHOLD * maxAngle, 1);

            for (int i = 0; i < pieces.length; i++) {
                final Piece p = pieces[i];
                final Piece prev = previousPiece(i, pieces);
                final Piece np = nextPiece(i, pieces);

                if (p.isBend()) {
                    if (p.radius < 100.0) {
                        p.speedFactor = 1.05;

                        p.slipSpeedThreshold = CLOSED_BEND_SLIP_SPEED_THRESHOLD;
                        p.slipThreshold = bSlipThreshold;
                    } else if (p.radius < 200.0) {
                        p.speedFactor = 1.2; //1.15;

                        p.slipSpeedThreshold = EASY_BEND_SLIP_SPEED_THRESHOLD;
                        p.slipThreshold = aSlipThreshold;
                    } else {
                        p.speedFactor = 1.3; //1.2; // 1.15

                        p.slipSpeedThreshold = FAST_BEND_SLIP_SPEED_THRESHOLD;
                        p.slipThreshold = defaultSlipThreshold;
                        
                        double reduce = 1;
                        final Piece np1 = nextPiece(i +1, pieces);
                        if ((np.isBend() && np.radius < p.radius)) {
                            reduce = np.radius / p.radius;
                        } else if ((np1.isBend() && np1.radius < p.radius)) {
                            reduce = np1.radius / p.radius;
                        }
                        p.speedFactor *= reduce;
                        p.slipThreshold *= reduce;
                        p.slipSpeedThreshold *= reduce;
                    }
                } else {
                    p.speedFactor = 1.4;// 1.2;

                    p.slipSpeedThreshold = STRAIGHT_SLIP_SPEED_THRESHOLD;
                    p.slipThreshold = defaultSlipThreshold;
                    
                    if (prev.isBend() || np.isBend()) {
                        p.slipThreshold *= 0.8;// 0.7;
                        p.slipSpeedThreshold *= 0.8; //0.7;
                    }
                }
            }
        }

        private static Piece nextPiece(final int currPiece, final Piece[] pieces) {
            int next = currPiece + 1;
            if (next > pieces.length - 1) {
                next = 0;
            }
            return pieces[next];
        }
        
        private static Piece previousPiece(final int currPiece, final Piece[] pieces) {
            int next = currPiece - 1;
            if (next < 0) {
                next = pieces.length -1;
            }
            return pieces[next];
        }
    }

}
