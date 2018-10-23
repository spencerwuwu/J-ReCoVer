// https://searchcode.com/api/result/92105628/

package hw3;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.DeathEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

public class HW3 extends AdvancedRobot implements LUTInterface {

	static File savetest = new File("d:\\savetest.txt");
	static File convergeCheck = new File("d:\\convergeCheck.txt");
	static File rewardCheck = new File("d:\\rewardCheck.txt");
	static String weightsFile = new String ("d:\\weightsFile.txt");
	static String weightsFileHW3 = new String ("d:\\weightsFileHW3.txt");
	static int battleCount = 0;
	static int winCount = 0;

	public static double reward = 0;
	public static double totalReward = 0;
	// define state constant
	public static final int numHeading = 4;
	public static final int numEnemyBearing = 4;
	//public static final int numEnemyDistance = 20;
	// distance in 20 is too many, reduce to 2
	public static final int numEnemyDistance = 2;
	public static final int numMyEnergy = 10;
	public static final int numEnemyEnergy = 10;
	public static int stateIndex[][][] = new int[numHeading][numEnemyBearing][numEnemyDistance];

	private static final int numState = numHeading * numEnemyBearing
			* numEnemyDistance;
	private static final int numAction = 6;
	// static double[][] qtable = new double[numState][numAction];

	// action parameters
	static int[] actionIndex = new int[6];

	// make actions also discrete, in accordance to discrete states
	// so that state-action pair is set up
	public static final int goAhead = 0;
	public static final int goBack = 1;
	public static final int goAheadTurnLeft = 2;
	public static final int goAheadTurnRight = 3;
	public static final int goBackTurnLeft = 4;
	public static final int goBackTurnRight = 5;
	public static final int numActions = 6;

	// pre-define the robot action parameters for all above actions
	public static final double RobotMoveDistance = 300.0;
	public static final double RobotTurnDegree = 45.0;

	// learning parameters
	private boolean first = true;
	public static final double LearningRate = 0.1;
	public static final double DiscountRate = 0.9;
	public static final double ExplorationRate = 1;
	public static final double ExploitationRate = 0.6;
	private double[] lastState = new double[3];
	private int lastAction;

	// enemy parameters
	String nameEnemy;
	public double bearingEnemy;
	public double headingEnemy;
	public double spotTimeEnemy;
	public double speedEnemy;
	public double XPositionEnemy;
	public double YPositionEnemy;
	public double distanceEnemy = 10000;
	public double constantHeadingEnemy;
	public double energyEnemy;
	// fire system
	private double firePower = 1;
	private double direction = 500;
	
	

	public void run() {
//		try {
//			// to test loading and saving function
//			load(savetest);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		
		// HW1NNforHW3 nn = new HW1NNforHW3();
		// !!!!!! if load existing q-table, then do NOT initialize, that will erase all the pre-trained q-value !!!!! *************
//		try {
//			// load weights from training LUT
//			loadWeights(weightsFile);
//			
//			// load weights from hw3 training
//			//loadWeights(weightsFileHW3);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		// turn off pre-trained weights
		//zeroWeights();
		initializeWeights();
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		while (true) {
			move();
			firePower = 400 / distanceEnemy;
			radarMove();
			gunMove();
			if (getGunHeat() == 0) {
				setFire(firePower);
			}
			execute();
		}
	}

	private void gunMove() {
		long time;
		long nextTime;
		Point2D.Double p;
		p = new Point2D.Double(XPositionEnemy, YPositionEnemy);
		for (int i = 0; i < 20; i++) {
			nextTime = (int) Math
					.round((getrange(getX(), getY(), p.x, p.y) / (20 - (3 * firePower))));
			time = getTime() + nextTime - 10;
			p = futurePosition(time);
		}
		// offsets the gun by the angle to the next shot based on linear
		// targeting provided by the enemy class
		double gunOffset = getGunHeadingRadians()
				- (Math.PI / 2 - Math.atan2(p.y - getY(), p.x - getX()));
		setTurnGunLeftRadians(nomalizeDegree(gunOffset));
	}

	public Point2D.Double futurePosition(long futureTime) {
		double duration = futureTime - spotTimeEnemy;
		double futureX;
		double futureY;
		if (Math.abs(constantHeadingEnemy) > 0.000001) {
			double radius = speedEnemy / constantHeadingEnemy;
			double tothead = duration * constantHeadingEnemy;
			futureY = YPositionEnemy
					+ (Math.sin(headingEnemy + tothead) * radius)
					- (Math.sin(headingEnemy) * radius);
			futureX = XPositionEnemy + (Math.cos(headingEnemy) * radius)
					- (Math.cos(headingEnemy + tothead) * radius);
		} else {
			futureY = YPositionEnemy + Math.cos(headingEnemy) * speedEnemy
					* duration;
			futureX = XPositionEnemy + Math.sin(headingEnemy) * speedEnemy
					* duration;
		}
		return new Point2D.Double(futureX, futureY);
	}

	private double getrange(double x1, double y1, double x2, double y2) {
		double xo = x2 - x1;
		double yo = y2 - y1;
		double h = Math.sqrt(xo * xo + yo * yo);
		return h;
	}

	private void radarMove() {
		double turnDegree;
		if (getTime() - spotTimeEnemy > 4) {
			turnDegree = Math.PI * 4;
		} else {
			turnDegree = getRadarHeadingRadians()
					- (Math.PI / 2 - Math.atan2(YPositionEnemy - getY(),
							XPositionEnemy - getX()));

			turnDegree = nomalizeDegree(turnDegree);
			if (turnDegree < 0)
				turnDegree -= Math.PI / 10;
			else
				turnDegree += Math.PI / 10;
		}

		setTurnRadarLeftRadians(turnDegree);
	}

	private double nomalizeDegree(double ang) {
		if (ang > Math.PI)
			ang -= 2 * Math.PI;
		if (ang < -Math.PI)
			ang += 2 * Math.PI;
		return ang;
	}

	private void move() {

		// normolize heading
		double tmpHeading = getHeading();
		tmpHeading += 45;
		if (tmpHeading > 360)
			tmpHeading -= 360;
		double heading = tmpHeading / 90;

		// normalize enemy bearing
		if (bearingEnemy < 0)
			bearingEnemy += 360;
		double tmpBearingEnemy = bearingEnemy + 45;
		if (tmpBearingEnemy > 360)
			tmpBearingEnemy -= 360;
		double enemyBearing = tmpBearingEnemy / 90;

		// int enemyDistance = (int) (distanceEnemy / 30);
		
		// enemyDistance number is reduced to 2, so used below
		double enemyDistance = distanceEnemy / 1000;

		double[] currentState = { heading, enemyBearing, enemyDistance };
		double crrentAction = train(currentState, reward);
		int intAction = (int) crrentAction;

		reward = 0;
		executeAction(intAction);
	}

	// RL for movement selection
	private void executeAction(int action) {
		switch (action) {
		case goAhead:
			setAhead(RobotMoveDistance);
			break;

		case goBack:
			setBack(RobotMoveDistance);
			break;

		case goAheadTurnLeft:
			setAhead(RobotMoveDistance);
			setTurnLeft(goAhead);
			break;

		case goAheadTurnRight:
			setAhead(RobotMoveDistance);
			setTurnRight(RobotTurnDegree);
			break;

		case goBackTurnLeft:
			setBack(RobotMoveDistance);
			setTurnRight(RobotTurnDegree);
			break;

		case goBackTurnRight:
			setBack(RobotMoveDistance);
			setTurnRight(RobotTurnDegree);
			break;
		}
	}

	@Override
	public double outputFor(double[] X) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	// return the actionIndex to take for state X
	public double train(double[] X, double reward) {
		double maxQ = maxQValueFor(X);
		// resized the value from sigmoid activation
		maxQ = 8 * maxQ;

		// select action policy
		int actionNow = selectAction(X);

		// maxQ and actionNow above, can be combined and replaced by NN Q-value approximation
		// int action = actionMaxQValue(stateIndexNow);
		double diffQValue = 0;
		double newQValue = 0;
		
		// for Q-Learning
		double oldQValue = QValueNN(lastState, lastAction);
		// resized the value from sigmoid activation
		oldQValue = 8 * oldQValue;

		System.out.println("Reward: " + reward);
		if (first)
			first = false;
		else {
			// for sarsa learning
			// double sarsaQValue = qtable[stateIndexNow][actionNow];
			// double oldQValue = qtable[lastStateIndex][lastAction];
			// double newQValue = (1 - LearningRate) * oldQValue + LearningRate
			// * (reward + DiscountRate * sarsaQValue);

			
			newQValue = (1 - LearningRate) * oldQValue + LearningRate
					* (reward + DiscountRate * maxQ);
			
			diffQValue = newQValue - oldQValue;
			// back up needed...
			updateWeightNewQvalue(X, actionNow, newQValue);
		}
		// back up state and action pair
		System.arraycopy(X, 0, lastState, 0, X.length);
		lastAction = actionNow;

		// measure learning and converging
		//if (X[0] < 1.01 && X[0] > 0.99 && X[1] < 1.01 && X[1] > 0.99 && X[2] < 0.2 && actionNow == 5)
		if (X[0] < 1 && X[1] < 1 && X[2] < 0.1 && actionNow == 5)
			// saveDiffQValue(stateIndexNow, actionNow, diffQValue, convergeCheck);
			saveDiffQValue(X[0], X[1], X[2], actionNow, diffQValue, convergeCheck);

		// ??? exploratory move here

		return actionNow;
	}

	private void saveDiffQValue(double X0, double X1, double X2, int actionNow,
			double diffQValue, File convergeCheck2) {
		BufferedWriter writer = null;
		try {
			// ture means append, not clear all
			writer = new BufferedWriter(new FileWriter(convergeCheck, true));
			writer.write(X0 + "\t" + X1 + "\t" + X2 + "\t"  + actionNow
					+ "\t" + diffQValue + "\n");

		} catch (IOException e) {
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
			}
		}		
	}

	private int selectAction(double[] X) {
		
		int action = actionMaxQValue(X);
		// perform exploration in else statement
		if (Math.random() > ExplorationRate) {
			return action;
		} else {
			// ramdom action
			int tmpAction = (int)(Math.random() * 10) % 6;
			return tmpAction;
		}
	}



	private void saveWinRate(int winCount, int totalBattle, File winRate) {
		BufferedWriter writer = null;
		try {
			// ture means append, not clear all
			writer = new BufferedWriter(new FileWriter(winRate, true));
			writer.write(((double)winCount)/totalBattle + "\t" + totalBattle + "\n");

		} catch (IOException e) {
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
			}
		}
	}

	// purely using the NN to produce Qvalue
//	@Override
	public void saveHW3Weights(File argFile) {
		BufferedWriter writer = null;
		try {
			// ture means append, not clear all
			writer = new BufferedWriter(new FileWriter(weightsFileHW3));
			
			for (int i = 0; i < numHidden; i++) {
				for (int j = 0; j < numInputs; j++) {
					writer.write(weightInputToHidden[i][j] + "\n");
				}
				// Save bias weight for this hidden neuron too
				writer.write(weightInputToHidden[i][numInputs] + "\n");
			}

			// Now save the weights from the hidden to the output neuron
			for (int i = 0; i < numHidden; i++) {
				writer.write(weightHiddenToOutput[i] + "\n");
			}

		} catch (IOException e) {
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
			}
		}

	}

	// also no need to load input, just using NN for real-time input values
//	@Override
//	public void load(File inputFile) throws IOException {
//		try {
//			FileInputStream fileInputStream = new FileInputStream(inputFile);
//			Scanner inputScanner = new Scanner(fileInputStream);
//
//			for (int i = 0; i < numState; i++)
//				for (int j = 0; j < numAction; j++) {
//					double statee = inputScanner.nextDouble();
//					double actionn = inputScanner.nextDouble();
//					if ((int) statee == i && (int) actionn == j)
//						qtable[i][j] = inputScanner.nextDouble();
//				}
//
//		} catch (IOException e) {
//			System.out.print(e.getMessage());
//		}
//	}

//	@Override
//	public void initialiseLUT() {
//		// initiate state-stateIndex table
//		int count = 0;
//		for (int a = 0; a < numHeading; a++)
//			for (int b = 0; b < numEnemyBearing; b++)
//				for (int c = 0; c < numEnemyDistance; c++)
//					stateIndex[a][b][c] = count++;
//
//		// initiate stateIndex-action table for QValue
//		for (int x = 0; x < numState; x++)
//			for (int y = 0; y < numAction; y++)
//				// qtable[x][y] = 0;
//
//				// multiplied by 10, since the random is too small, reward is
//				// much larger
//				qtable[x][y] = 0;
//		// qtable[x][y] = Math.random();
//	}

	@Override
	// get stateIndex of given state vector X
	public int indexFor(double[] X) {
		int index = ((int) X[0] + 1) * 4 + ((int) X[1] + 1) * 4 + ((int) X[2] + 1) * 2 -1;
		return index;
		
		// this mapping is not accurate
//		int index = stateIndex[(int) X[0]][(int) X[1]][(int) X[2]];
//		return index;
	}

	// return max QValue
	public double maxQValueFor(double[] X) {
		
		double max = -1000000; 
		for (int i = 0; i < numAction; i++) {
			double tmp = QValueNN(X, i);
			if (tmp > max)
				max = tmp;
		}
		return max;
	}
	
	// return action number with max Qvalue
	private int actionMaxQValue(double[] X) {
		double max = 0;
		int action = 0;
		for (int i = 0; i < numAction; i++) {
			double tmp = QValueNN(X, i);
			if (tmp > max) {
				max = tmp;
				action = i;
			}
				
		}
		return action;
	}

	// return action with max QValue
	public double QValueNN(double[] X, int actionNow) {
		// HW1NNforHW3 nn = new HW1NNforHW3();
		
		/*
		 * following is using neuron network
		 */
					
//		double oldQValue = qtable[lastStateIndex][lastAction];
//		double newQValue = (1 - LearningRate) * oldQValue + LearningRate
//				* (reward + DiscountRate * maxQ);
		

			double saveHeading = X[0];
			double saveBearing = X[1];
			double saveDistance = X[2];
			double[] sVector = new double[]{saveHeading, saveBearing, saveDistance, actionNow};
			double tmpMaxQ = getQValue(sVector);
			
			return tmpMaxQ;

		
//		/*
//		 * following is using look up table
//		 */
//		for (int i = 0; i < numAction; i++) {
//			if (qtable[stateIndexNow][i] > max) {
//				max = qtable[stateIndexNow][i];
//				action = i;
//			}
//
//		}
//		return action;
	}

	public void onBulletHit(BulletHitEvent e) {
		double change = e.getBullet().getPower() * 9;
		reward += change;
		totalReward += change;
	}

	public void onBulletHitBullet(BulletHitBulletEvent e) {
		//
	}

	public void onHitByBullet(HitByBulletEvent e) {

		double change = -5 * e.getBullet().getPower();
		reward += change;
		totalReward += change;
	}

	public void onBulletMissed(BulletMissedEvent e) {
		double change = -e.getBullet().getPower();
		reward += change;
		totalReward += change;
	}

	public void onHitRobot(HitRobotEvent e) {

		double change = -6.0;
		reward += change;
		totalReward += change;
	}

	public void onHitWall(HitWallEvent e) {

		double change = -(Math.abs(getVelocity()) * 0.5 - 1);
		reward += change;
		totalReward += change;
		// need more here
	}

	public void onScannedRobot(ScannedRobotEvent e) {

		// the next line gets the absolute bearing to the point where the bot is
		double absbearing_rad = (getHeadingRadians() + e.getBearingRadians())
				% (2 * Math.PI);
		// this section sets all the information about our enemy
		nameEnemy = e.getName();
		double h = nomalizeDegree(e.getHeadingRadians() - headingEnemy);
		h = h / (getTime() - spotTimeEnemy);
		constantHeadingEnemy = h;
		XPositionEnemy = getX() + Math.sin(absbearing_rad) * e.getDistance(); 
		YPositionEnemy = getY() + Math.cos(absbearing_rad) * e.getDistance(); 
		bearingEnemy = e.getBearingRadians();
		headingEnemy = e.getHeadingRadians();
		spotTimeEnemy = getTime(); // game time at which this scan was produced
		speedEnemy = e.getVelocity();
		distanceEnemy = e.getDistance();
		energyEnemy = e.getEnergy();

	}

	public void onWin(WinEvent event) {
		//saveHW3Weights(savetest);
		// saveReward();
		battleCount++;
		winCount++;
		totalReward = 0;
		saveWinRate(winCount, battleCount, winRate);
	}

	public void onDeath(DeathEvent event) {
		//saveHW3Weights(savetest);
		// saveReward();
		battleCount++;
		totalReward = 0;
		saveWinRate(winCount, battleCount, winRate);
	}

	private void saveReward() {
		BufferedWriter writer = null;
		try {
			// ture means append, not clear all
			writer = new BufferedWriter(new FileWriter(rewardCheck, true));
			writer.write(totalReward + "\n");

		} catch (IOException e) {
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void save(File argFile) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void load(File argFileName) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initialiseLUT() {
		// TODO Auto-generated method stub
		
	}
	
	public double updateWeightNewQvalue(double[] X, int actionNow, double newQValue) {
		// HW1NNforHW3 updateWeights = new HW1NNforHW3();
		
		/*
		 * following is using neuron network
		 */
					
//		double oldQValue = qtable[lastStateIndex][lastAction];
//		double newQValue = (1 - LearningRate) * oldQValue + LearningRate
//				* (reward + DiscountRate * maxQ);
		

			double saveDistance = X[0];
			double saveBearing = X[1];
			double saveHeading = X[2];
			double[] sVector = new double[]{saveHeading, saveBearing, saveDistance, actionNow};
			
			// update weights
			double tmpMaxQ = trainNN(sVector, newQValue);
			
			return tmpMaxQ;

		
//		/*
//		 * following is using look up table
//		 */
//		for (int i = 0; i < numAction; i++) {
//			if (qtable[stateIndexNow][i] > max) {
//				max = qtable[stateIndexNow][i];
//				action = i;
//			}
//
//		}
//		return action;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	public double trainNN(double[] X, double target) {
		// feed forward
		calcHiddenValue(X);
		calcOutputValue();
		
		// backPrapogation
		calcOutputError(target);
		calcHiddenError();
		
		// update weights
		updateWeightInputToHidden(X);
		updateWeightHiddenToOutput();
		return tmpOut;
	}

	private void calcHiddenValue(double[] X) {
		for (int i = 0; i < numHidden; i++) {
			hiddenValues[i] = 0;
			for (int j = 0; j < numInputs; j++) {
				hiddenValues[i] += X[j] * weightInputToHidden[i][j];
			}
			hiddenValues[i] += weightInputToHidden[i][numInputs] * 1; // bias

			// processed by activation function
			hiddenValues[i] = sigmoid(hiddenValues[i]);
		}
	}

	private void calcOutputValue() {
		tmpOut = 0;
		for (int i = 0; i < numHidden; i++) {
			tmpOut += hiddenValues[i] * weightHiddenToOutput[i];
		}
		tmpOut += weightHiddenToOutput[numHidden] * 1; // bias
		tmpOut = sigmoid(tmpOut); // activation function
	}

	private void calcOutputError(double target) {
		// for bipolar input
		outputError = 0.5 * (1 +tmpOut) * (1 - tmpOut) * (target - tmpOut);
		
		// for binary input
		//outputError = tmpOut * (1 - tmpOut) * (target - tmpOut);
		//System.out.println("output error:" + outputError + "\ttmpOut: " + tmpOut + "\ttarget: " + target);
	}

	private void calcHiddenError() {
		// for bipolar input
		for (int i = 0; i < numHidden; i++) {
		hiddenError[i] = 0.5 * (1 +hiddenValues[i]) * (1 - hiddenValues[i])
				* outputError * weightHiddenToOutput[i];
		
		// for binary input
//		for (int i = 0; i < numHidden; i++) {
//			hiddenError[i] = hiddenValues[i] * (1 - hiddenValues[i])
//					* outputError * weightHiddenToOutput[i];
			//System.out.println(i + " hidden error" + hiddenError[i]);
		}
	}

	private void updateWeightInputToHidden(double[] X) {
		// // backup current weight
		for (int i = 0; i < numHidden; i++) {
		System.arraycopy(weightInputToHidden[i], 0, tempWeightInputToHidden[i], 0,
				weightInputToHidden[i].length);
		}

		// update weightInputToHidden
		for (int i = 0; i < numHidden; i++) {
			for (int j = 0; j < numInputs; j++) {
				weightInputToHidden[i][j] += learningRate
						* hiddenError[i] * X[j] + momentumTerm
						* getPreviousToHiddenDeltaWeight(i, j);
				//System.out.println("numHidden: " + i + "  Inputs: " + X[j] + "  weight: " +weightInputToHidden[i][j]);
			}
			// for bias term below
			weightInputToHidden[i][numInputs] += learningRate
					* hiddenError[i] + momentumTerm
					* getPreviousToHiddenDeltaWeight(i, numInputs);
			//System.out.println("numHidden: " + i + "  Inputs: " +  X[numInputs] + "  weight: " +weightInputToHidden[i][numInputs]);
		}

		// update previous weight
		for (int i = 0; i < numHidden; i++) {
		System.arraycopy(tempWeightInputToHidden[i], 0, previousWeightInputToHidden[i], 0,
				weightInputToHidden[i].length);
		}

	}

	private void updateWeightHiddenToOutput() {
		// backup current weight
		System.arraycopy(weightHiddenToOutput, 0, tempWeightHiddenToOutput, 0,
				weightHiddenToOutput.length);
		
		for (int i = 0; i < numHidden; i++) {
			weightHiddenToOutput[i] += learningRate * outputError
					* hiddenValues[i] + momentumTerm
					* getPreviousToOutDeltaWeight(i);
			//System.out.println("numHidden: " + i + "  weight: " +weightHiddenToOutput[i]);
		}		
		weightHiddenToOutput[numHidden] += learningRate * outputError * 1
				+ momentumTerm * getPreviousToOutDeltaWeight(numHidden);
		// update previous weight
		System.arraycopy(tempWeightHiddenToOutput, 0,
				previousWeightHiddenToOutput, 0,
				tempWeightHiddenToOutput.length);
	}
	
	private double getPreviousToOutDeltaWeight(int i) {
		if (previousWeightHiddenToOutput[i] != 0)
			return weightHiddenToOutput[i] - previousWeightHiddenToOutput[i];
		else
			return 0;
	}

	private double getPreviousToHiddenDeltaWeight(int i, int j) {
		if (previousWeightInputToHidden[i][j] != 0)
			return weightInputToHidden[i][j]
					- previousWeightInputToHidden[i][j];
		else
			return 0;
	}
	
	public double sigmoid(double x) {
		// for part-3, use converging sigmoid
		//return 1 / (1 + Math.pow(Math.E, -x)); // binary input
		return 2 / (1 + Math.pow(Math.E, -x)) - 1; // bipolar input
	}

	public double customSigmoid(double x) {
		return (B - A) / (1 + Math.pow(Math.E, -x)) - A;
	}

	public void initializeWeights() {
		// Initialize the weight InputToHidden to random values
		for (int i = 0; i < numHidden; i++) {
			for (int j = 0; j < numInputs + 1; j++) {
				// System.out.println(i + ", " + j);
				weightInputToHidden[i][j] = Math.random() - 0.5;
				
			}
		}

		// Initialize the weight HiddenToOutput to random values
		for (int j = 0; j < numHidden + 1; j++) { 
			weightHiddenToOutput[j] = Math.random() - 0.5;
		}
	}

	public void zeroWeights() {
		// Initialize the weight InputToHidden to random values
		for (int i = 0; i < numHidden; i++) {
			for (int j = 0; j < numInputs + 1; j++) {
				weightInputToHidden[i][j] = 0;
			}
		}

		// Initialize the weight HiddenToOutput to random values
		for (int j = 0; j < numHidden + 1; j++) { 
			weightHiddenToOutput[j] = 0;
		}
	}


	// load pre-trained weights from NN
	public void loadWeights(String inputFile) throws IOException {
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			Scanner inputScanner = new Scanner(fileInputStream);

			for (int i = 0; i < numHidden; i++) {
				for (int j = 0; j < numInputs; j++) {
					weightInputToHidden[i][j] = inputScanner.nextDouble();
				}
				// Save bias weight for this hidden neuron too
				weightInputToHidden[i][numInputs] = inputScanner.nextDouble();
			}

			// Now save the weights from the hidden to the output neuron
			for (int i = 0; i < numHidden; i++) {
				weightHiddenToOutput[i] = inputScanner.nextDouble();
			}
			
		} catch (IOException e) {
			System.out.print(e.getMessage());
		}
	}
	
	static int epoch = 0;
	static int numInputs = 4; // element number of input vector
	static public int numHidden = 37; // number of hidden nodes
	static int numOutputs = 1; // element number of output vector
	/*
	 * numVector change according to LUT changes
	 */
	static int numVectors = 42;
	static double learningRate = 0.05;
	static double momentumTerm = 0.5;
	static double bias = 1.0;
	double A;
	double B;
	static double totalError; 
	static double acceptableError = 0.05;
	
	static double[][] weightInputToHidden = new double[numHidden][numInputs + 1];
	static double[][] previousWeightInputToHidden = new double[numHidden][numInputs + 1];

	// double[][] weightHiddenToOutput = new double[numOutputs][numHidden + 1];
	// above is for multiple output

	static double[] weightHiddenToOutput = new double[numHidden + 1];
	static double[] previousWeightHiddenToOutput = new double[numHidden + 1];

	// temp store current weight, used in momentum
	static double[][] tempWeightInputToHidden = new double[numHidden][numInputs + 1];
	static double[] tempWeightHiddenToOutput = new double[numHidden + 1];

	static double[] hiddenValues = new double[numHidden];
	static double[][] inputValues = new double[numVectors][numInputs + 1];
	static double[] targetValues = new double[numVectors];
	static double tmpOut = 0;
	static double outputError = 0;
	static double[] hiddenError = new double[numHidden];

	File paraFile = new File("d:\\parameters.txt");
	//static File inputFile = new File("d:\\XOR.txt");
	static File ErrorEpochFile = new File("d:\\error_epoch.txt");
	static String inputFile = new String("d:\\XOR.txt");
	static String LUT_hw2 = new String ("d:\\LUT_hw2.txt");
	static File winRate = new File ("d:\\winRate.txt");

	public double getQValue(double[] sVector) {
		// feed forward
		calcHiddenValue(sVector);
		calcOutputValue();
		
		return tmpOut;
	}	
	
}

