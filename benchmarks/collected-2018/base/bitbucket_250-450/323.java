// https://searchcode.com/api/result/97360060/

package noobbot;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by rukshan on 4/20/14.
 */
public class TrackKnowledge {

    private double force = 0.0;

    private double mass;
    private double dragConstant;

    public static double DefaultDecreasePerSec = 0.1;
    public static double DefaultMaxSpeed = 10;
    public final double MAX_SLIP = 55;
    public final double DEFAULT_FORCE_FACTOR = 1; // default force (in other words, use svm value)
    public final double MAX_INCREASE = 1.0;  // this is the maximum target slip for any piece
    private final double FORCE_INCREASE = 1.07; // target slip is increased by this factor
    public final double FORCE_REDUCE_FACTOR = 0.75; // target slip is decreased by this
    private final double FORCE_REDUCE_FACTOR_OTHERS = 0.85; // target slip decrease for other pieces
    private final double SLIP_BAR = 45; // target slip will be increased for slip angles below this bar
    public static double DANGER_SLIP = 40;


    private int crashCount = 0 ;
    private  int lapCount = 0 ;

    private HashMap<Integer,Double> bendForceFactors = new HashMap<>();
    private HashMap<Integer, Double> dangerSlips = new HashMap<>();

    private ArrayList<Integer> crashedTracks = new ArrayList<>(); // crashed piece indices
    private HashMap<Integer, Double> maxSlips = new HashMap<>(); // max slip by piece
    private HashMap<Integer, Double> crashedForces = new HashMap<>();

    private ArrayList<String> othersInTurbo = new ArrayList<>();

    public void addOtherCarTurbo(String name){
        if(othersInTurbo != null && !othersInTurbo.contains(name))
            othersInTurbo.add(name);
    }

    public void removeOtherCarTurbo(String name){
        if(othersInTurbo != null && !othersInTurbo.contains(name))
            othersInTurbo.remove(name);
    }

    public boolean isOtherCarInTurbo(String name){
        return othersInTurbo != null && othersInTurbo.contains(name);
    }


    private static TrackKnowledge instance;

    private TrackKnowledge(){

    }

    public static TrackKnowledge getInstance(){
        if(instance == null)
            instance = new TrackKnowledge();

        return instance;
    }

    public  void addNewBend(int bendIndex){
        if(!bendForceFactors.containsKey(bendIndex)){
            bendForceFactors.put(bendIndex, 1.0);
        }
    }

    public void notifyCrash(int trackIndex, double speed, int lane){

        System.out.println(Main.tick+": Crashed at: " + trackIndex);

        crashCount++;

        if(!crashedTracks.contains(trackIndex))
            crashedTracks.add(trackIndex);

        //add previous index to
        int prev = TrackHelper.getInstance().getPreviousTrackPiece(trackIndex).getIndex();

        if(!crashedTracks.contains(prev))
            crashedTracks.add(prev);

        double existingDangerSlip = getDangerSlip(trackIndex);
        dangerSlips.put(trackIndex, existingDangerSlip * FORCE_REDUCE_FACTOR);



        //if(lapCount <=2){
        if(true){
            //if we are still on the first lap reduce all
            for(int i : bendForceFactors.keySet()){
                double existingVal = bendForceFactors.get(i);

                if(existingVal > 1)
                    existingVal = 1;

                if(i == trackIndex){
                    bendForceFactors.put(i, existingVal * FORCE_REDUCE_FACTOR);

                    double f = getForce();
                    if(crashedForces.containsKey(i))
                        f = crashedForces.get(i);

                    crashedForces.put(i,f);
                }
                else
                    bendForceFactors.put(i, existingVal * FORCE_REDUCE_FACTOR_OTHERS);
            }
        }


        DANGER_SLIP = DANGER_SLIP * FORCE_REDUCE_FACTOR_OTHERS;

        force = force * 0.9 ;
    }

    public void notifySlip(int trackIndex, double slip, int lane, double speed, double throttle, int lap){



        slip = Math.abs(slip);

        if(slip < SLIP_BAR)
            updateForce(trackIndex, lane, speed,  throttle, lap);

        if(!maxSlips.containsKey(trackIndex))
            maxSlips.put(trackIndex,slip);
        else{
            double existingMax = maxSlips.get(trackIndex);
            if(existingMax < slip){
                maxSlips.put(trackIndex, slip);
            }
        }
    }

    private void updateForce(int index, int lane, double speed, double throttle, int lap){

        if(crashCount >= 3)
            return;

        if(throttle <= 0.5)
            return;

        TrackHelper trackHelper = TrackHelper.getInstance();
        
        double totalLaps = trackHelper.getTotalLaps();
        
        if(totalLaps > 0.0 && lap > totalLaps * 0.75){
        	return;
        }
        
        TrackPiece track = trackHelper.getTrackPiece(index);

        if(track.getType() == TrackPiece.PieceType.BEND){
            double radius = track.getRadius(trackHelper.getLane(lane));

            if(radius > 0){
                double f = speed * speed / radius ;
                double currentForce = getForce();
                if(currentForce < f &&  currentForce * FORCE_INCREASE >= f){
                    System.out.println("current force: " + getForce() + " calculated : " + f + " lane:" + lane + " t->" + throttle);
                    force = f;
                }

            }

        }

    }

    public double getForceFactor(int bendIndex){
        if(bendForceFactors.containsKey(bendIndex))
            return bendForceFactors.get(bendIndex);
        else
            return DEFAULT_FORCE_FACTOR;
    }

    public void recalculateAtEndOfLap(){

        lapCount++;

        for(int i = 0 ; i < TrackHelper.getInstance().pieces.size(); i++){

            if(!bendForceFactors.containsKey(i))
                continue;

            if(crashedTracks.contains(i)){
                continue;
            }

            double maxSlip = 0 ;
            if(maxSlips.containsKey(i)){
                //get the max slip on this track index
                maxSlip = Math.abs(maxSlips.get(i));
            }



            if(maxSlip < SLIP_BAR){
                //it hasn't had a big slip yet
                double factor = bendForceFactors.get(i);
                factor = factor * FORCE_INCREASE;
                if(factor > MAX_INCREASE)
                    factor = MAX_INCREASE;

                bendForceFactors.put(i, factor);
            }
            else{

                try{
                    double existingVal = bendForceFactors.get(i);

                    if(existingVal >= 1){
                        existingVal = 1;
                        bendForceFactors.put(i, existingVal * FORCE_REDUCE_FACTOR_OTHERS);
                    }
                }
                catch (Exception ex){

                }
            }


        }

        maxSlips.clear();
    }

    public boolean hasCrashedBefore(int trackIndex){
        return crashedTracks.contains(trackIndex);
    }

    public double getDangerSlip(int index){
        if(dangerSlips.containsKey(index))
            return dangerSlips.get(index);
        else
            return TrackKnowledge.DANGER_SLIP;
    }


    public double getMass() {
        if(Double.isNaN(mass) || Double.isInfinite(mass) || mass == 0)
            return 4.949;

        return mass;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public double getDragConstant() {
        if(Double.isNaN(dragConstant) || Double.isInfinite(dragConstant) || dragConstant == 0)
            return 0.09999;

        return dragConstant;
    }

    public void setDragConstant(double dragConstant) {
        this.dragConstant = dragConstant;
    }



    public int ticksRequiredToGetToGivenSpeed(double currentSpeed, double targetSpeed, double throttle, double turboFactor)
    {

        //How many ticks do you need to get to a given speed v:
        double m = getMass();
        double k = getDragConstant();
        
        if(turboFactor > 1.0)
        	throttle = throttle * turboFactor;
        
        double t = ( Math.log  ((targetSpeed - ( throttle/k ) )/(currentSpeed - ( throttle/k ) ) ) * m ) / ( -1*k );
        return (int) Math.ceil(t);
    }

    //  Distance traveled in t ticks:
    public double distanceTravelledInTicks(int ticks, double currentSpeed, double throttle, double turboFactor){

        double m = getMass();
        double k = getDragConstant();

        if(turboFactor > 1.0)
        	throttle = throttle * turboFactor;
        
        return  ( m/k ) * ( currentSpeed - ( throttle/k ) ) * ( 1.0 - Math.exp ( ( -k*ticks ) / m ) ) + ( throttle/k ) * ticks ;

    }


    public double distanceTravelledInTicksWithTurbo(int ticks, double currentSpeed, double throttle, double turboFactor){

        double m = getMass();
        double k = getDragConstant();
        
        if(turboFactor > 1.0)
        	throttle = throttle * turboFactor;
        
        return  ( m/k ) * ( currentSpeed - ( throttle/k ) ) * ( 1.0 - Math.exp ( ( -k*ticks ) / m ) ) + ( throttle/k ) * ticks ;

    }

    //Calculate your speed in a given amount of ticks t:
    //v(t) = (v(0) - (h/k) ) * e^ ( ( - k * t ) / m ) + ( h/k )
    public double speedInGivenAmountOfTicks(int ticks, double currentSpeed, double throttle, double turboFactor){
        double m = getMass();
        double k = getDragConstant();
        
        if(turboFactor > 1.0)
        	throttle = throttle * turboFactor;
        
        return (currentSpeed - (throttle/k) ) * Math.exp((-k * ticks) / m) + ( throttle/k );
    }

    public  double maxPossibleSpeed(int index, double radius){


        if(force <= 0.0){
            try {
                force = DataProcessor.getInstance().getForceUsingSvm(radius,getMass(),getDragConstant());
            }
            catch (Exception ex){
                ex.printStackTrace();
                force = 4.5;
            }
        }

        double factor = getForceFactor(index);
        double f = getForce();

        if(crashedForces.containsKey(index)){
            f = crashedForces.get(index);
        }
        else{
            if(factor < 1.0)
                f = f * factor;
        }
        double val =    Math.sqrt(f * radius);

        return  val;
    }

   /* public double getStartSpeed(double endSpeed, double throttle, int ticks){
            double k = getDragConstant();
            double m = getMass();
            double val = (endSpeed - throttle / k ) / Math.exp( -k*ticks/m) + throttle/k ;
            System.out.println("start speed: " + val);
            return val;
    } */

    public double getThrottle(double startSpeed, double endSpeed, int ticks, double turboFactor){
        
    	double k = getDragConstant();
        double m = getMass();

        double e = Math.exp(-k*ticks/m);

        double h = k * (endSpeed - startSpeed * e) / ( 1 - e);
        
        if(turboFactor > 1)
        	h = h /turboFactor ;

        if(h < 0.0)
            return 0.0 ;

        if(h > 1.0)
            return 1.0;

        return h;
    }

    public double getForce() {
        return force;
    }
}

