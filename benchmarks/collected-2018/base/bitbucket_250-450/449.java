// https://searchcode.com/api/result/63358545/

/*******************************************************************************
 * Copyright (c) 2013 Luca Nenni.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package it.unibo.alchemist.boundary.monitors;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import it.unibo.alchemist.boundary.interfaces.OutputMonitor;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IEnvironment2DWithObstacles;
import it.unibo.alchemist.model.interfaces.ILsaMolecule;
import it.unibo.alchemist.model.interfaces.IMolecule;
import it.unibo.alchemist.model.interfaces.INeighborhood;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IObstacle2D;
import it.unibo.alchemist.model.interfaces.IPosition;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.utils.L;

/**
 * The OutputMonitor for Alchemist2Blender
 * Based upon Danilo Pianini's SAPEREContinuous2DDisplay
 * 
 * @author Luca Nenni
 * @version 20130227
 * 
 */

public class Alchemist2BlenderOutputMonitor implements OutputMonitor<Double, Double, List<? extends ILsaMolecule>> {

	private static final String SLASH = System.getProperty("file.separator");
	private static final long serialVersionUID = 314159265359L;
	private static final int FRAME_RATE = 25;
	private static int FRAME_STEP_RATE = 25;
	private final Map<INode<List<? extends ILsaMolecule>>, IPosition<Double, Double>> positions = new ConcurrentHashMap<>();
	private final Map<INode<List<? extends ILsaMolecule>>, INeighborhood<Double, List<? extends ILsaMolecule>>> neighbors = new ConcurrentHashMap<>();
	private List<? extends IObstacle2D> obstacles = null;
	private boolean firstTime = true, realTime = false;
	private int st;
	private double lasttime;
	private final double timestep = 1d / FRAME_RATE;
	private long lastRefresh = System.currentTimeMillis();
	private final Semaphore mutex = new Semaphore(1);
	private String pos, cont= "";
	private String filePath = System.getProperty("user.home"), fileName = "simulation";

	/**
	 * Builds an OutputMonitor
	 */
	public Alchemist2BlenderOutputMonitor() {
		this(1);
	}
	
	public Alchemist2BlenderOutputMonitor(final int step) {
		st = step;
	}

	/**
	 * Some 'set' functions
	 */
	public void setStep(final int step) {
		st = step;
	}

	public void setRealTime(final boolean rt) {
		realTime = rt;
	}
	
	public void setFilePath(final String fp) {
		filePath = fp;
	}
	
	public void setFileName(final String fn) {
		fileName = fn;
	}

	/**
	 * The core of the OutputMonitor
	 */
	@Override
	public void stepDone(final IEnvironment<Double, Double, List<? extends ILsaMolecule>> env, final IReaction<List<? extends ILsaMolecule>> r, final ITime time, final long step) {
		if (firstTime) {
			mutex.acquireUninterruptibly();
			initAll(env, r, time, step);
			lasttime = -timestep;
			firstTime = false;
			if (env instanceof IEnvironment2DWithObstacles<?,?>) {
				try{
					FileWriter fstream = new FileWriter((filePath + SLASH + fileName + ".wal"), false);
					BufferedWriter out = new BufferedWriter(fstream);
					// Reset the nodes file
					FileWriter fstreamr = new FileWriter((filePath + SLASH + fileName + ".nod"), false);
					BufferedWriter outr = new BufferedWriter(fstreamr);
					// Reset the gradients file
					FileWriter fstreamg = new FileWriter((filePath + SLASH + fileName + ".gra"), false);
					BufferedWriter outg = new BufferedWriter(fstreamg);
					// Create file 
					StringBuffer sb = new StringBuffer();
					for (Object obs : ((IEnvironment2DWithObstacles<?,?>) env).getObstacles()) {
						sb.append(((IObstacle2D)obs).toString().replace("(", "").replace(")", "").replace(" ", "")+";");
					}

					out.write(sb.toString());

					//Close the output stream
					out.close();
					fstream.close();
					outr.close();
					fstreamr.close();
					outg.close();
					fstreamg.close();
					}catch (Exception e){ //Catch exception if any
						L.error(e);
					}
				}				
			mutex.release();
		} else if (st == 0 || step % st == 0) {
			if (realTime) {
				if (lasttime + timestep > time.toDouble()) {
					return;
				}
				long timePassed = System.currentTimeMillis() - lastRefresh;
				long timeSimulated = (long) ((time.toDouble() - lasttime) * 1000);
				if (timeSimulated > timePassed) {
					try {
						Thread.sleep(Math.min(timeSimulated - timePassed, 1000 / FRAME_RATE));
					} catch (InterruptedException e) {
						L.error("Damn spurious wakeups.");
					}
				}
			}
			mutex.acquireUninterruptibly();
			update(env, time);
			if (step % FRAME_STEP_RATE == 0) {
				try{
					FileWriter fstreamn = new FileWriter((filePath + SLASH + fileName + ".nod"), true);
					BufferedWriter outn = new BufferedWriter(fstreamn);
					FileWriter fstreamn2 = new FileWriter((filePath + SLASH + fileName + ".gra"), true);
					BufferedWriter outn2 = new BufferedWriter(fstreamn2);
					// Create file 
					StringBuffer sbn = new StringBuffer();
					StringBuffer sbn2 = new StringBuffer();
					sbn.append(String.format("%.5f", time.toDouble()) + ";" + String.format("%05d", step) + ";");
					sbn2.append(String.format("%.5f", time.toDouble()) + ";" + String.format("%05d", step) + ";");
					for (INode<List<? extends ILsaMolecule>> n : env.getNodes()) {
						sbn.append(Integer.toString(n.getId()));
						sbn.append(",");
						pos = env.getPosition(n).toString();
						sbn.append(pos);
						sbn.append(",");
						cont = (n.getContents().keySet()).toString();
						sbn.append(cont.replace(";", ":").replace(",", ":"));
						sbn.append(";");
						if (!n.getContents().keySet().isEmpty()) {
							IMolecule mol = n.getContents().keySet().iterator().next();
							if (mol != null && n.getConcentration(mol).toString().contains("grad, target, ")) {
								sbn2.append(Integer.toString(n.getId()));
								sbn2.append(",");
								sbn2.append(pos);
								sbn2.append(",");
								sbn2.append(n.getConcentration(mol).toString().replace("grad, target, ", ""));
								sbn2.append(";");
							}
						}
					}
					
					sbn.append("\r\n");
					sbn2.append("\r\n");
						outn.write(sbn.toString().replace("[", "").replace("]", "").replace("<", "").replace(">", ""));
						outn2.write(sbn2.toString().replace("[", "").replace("]", "").replace("<", "").replace(">", ""));
					//Close the output stream
					outn.close();
					fstreamn.close();
					outn2.close();
					fstreamn2.close();
					}catch (Exception e){ //Catch exception if any
						L.error(e);
					}
				}
			}
			mutex.release();
		}


	/**
	 * Updates the simulation data
	 */
	private void update(final IEnvironment<Double, Double, List<? extends ILsaMolecule>> env, final ITime time) {
		if (envHasMobileObstacles(env)) {
			loadObstacles(env);
		}
		lastRefresh = System.currentTimeMillis();
		lasttime = time.toDouble();
		computeNodes(env);
	}

	/**
	 * Initializes the nodes and obstacles
	 */
	private void initAll(final IEnvironment<Double, Double, List<? extends ILsaMolecule>> env, final IReaction<List<? extends ILsaMolecule>> r, final ITime time, final long step) {
		computeNodes(env);
		if (env instanceof IEnvironment2DWithObstacles) {
			loadObstacles(env);
		} else {
			obstacles = null;
		}
	}

	/**
	 * Calculates the nodes positions
	 */
	private void computeNodes(final IEnvironment<Double, Double, List<? extends ILsaMolecule>> env) {
		positions.clear();
		neighbors.clear();
		for (final INode<List<? extends ILsaMolecule>> n : env) {
			positions.put(n, env.getPosition(n));
			neighbors.put(n, env.getNeighborhood(n));
		}
	}

	/**
	 * Loads the obstacles
	 */
	private void loadObstacles(final IEnvironment<Double, Double, List<? extends ILsaMolecule>> env) {
		obstacles = ((IEnvironment2DWithObstacles<?, ?>) env).getObstacles();
	}

	/**
	 * Tells if there are mobile obstacles in the environment
	 */
	private static boolean envHasMobileObstacles(final IEnvironment<Double, Double, List<? extends ILsaMolecule>> env) {
		return (env instanceof IEnvironment2DWithObstacles) && ((IEnvironment2DWithObstacles<?, ?>) env).hasMobileObstacles();
	}

	/**
	 * May be needed to reduce the output rows
	 */
	public void setFrameStepRate (int fsr) {
		FRAME_STEP_RATE = fsr;
	}

	/**
	 * Returns the step number
	 */
	public int getStep() {
		return st;
	}

	/**
	 * Call this method if you want this monitor to be bound to a new
	 * environment
	 */
	public void reset() {
		firstTime = true;
	}

}

