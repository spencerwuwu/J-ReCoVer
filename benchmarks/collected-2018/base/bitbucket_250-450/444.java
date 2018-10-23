// https://searchcode.com/api/result/62473449/

/*******************************************************************************
 * Copyright (c) 2013 Luca Nenni.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package it.unibo.alchemist.boundary.monitors;

import java.io.BufferedWriter;
import java.io.FileWriter;
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
 * The OutputMonitor for Alchemist2Blender Based upon Danilo Pianini's
 * SAPEREContinuous2DDisplay.
 * 
 * @author Luca Nenni
 * @author Danilo Pianini
 * 
 */

public class Alchemist2BlenderOutputMonitor implements OutputMonitor<Double, Double, List<? extends ILsaMolecule>> {

	private static final String SLASH = System.getProperty("file.separator");
	private static final long serialVersionUID = 314159265359L;
	private static final int FRAME_RATE = 25;
	private static final int MILLIS_PER_SECOND = 1000;
	private static int stepRate = FRAME_RATE;
	private final Map<INode<List<? extends ILsaMolecule>>, IPosition<Double, Double>> positions = new ConcurrentHashMap<>();
	private final Map<INode<List<? extends ILsaMolecule>>, INeighborhood<Double, List<? extends ILsaMolecule>>> neighbors = new ConcurrentHashMap<>();
	private boolean firstTime = true, realTime = false;
	private int st;
	private double lasttime;
	private final double timestep = 1d / FRAME_RATE;
	private long lastRefresh = System.currentTimeMillis();
	private final Semaphore mutex = new Semaphore(1);
	private String pos, cont = "";
	private String filePath = System.getProperty("user.home"), fileName = "simulation";

	/**
	 * Builds an OutputMonitor.
	 */
	public Alchemist2BlenderOutputMonitor() {
		this(1);
	}

	/**
	 * Builds an OutputMonitor.
	 * @param step 
	 */
	public Alchemist2BlenderOutputMonitor(final int step) {
		st = step;
	}


	/**
	 * @param rt true if realtime
	 */
	public void setRealTime(final boolean rt) {
		realTime = rt;
	}

	/**
	 * @param fp the file path
	 */
	public void setFilePath(final String fp) {
		filePath = fp;
	}

	/**
	 * @param fn the file name
	 */
	public void setFileName(final String fn) {
		fileName = fn;
	}

	@Override
	public void stepDone(final IEnvironment<Double, Double, List<? extends ILsaMolecule>> env, final IReaction<List<? extends ILsaMolecule>> r, final ITime time, final long step) {
		if (firstTime) {
			mutex.acquireUninterruptibly();
			initAll(env);
			lasttime = -timestep;
			firstTime = false;
			if (env instanceof IEnvironment2DWithObstacles<?, ?>) {
				try {
					final FileWriter fstream = new FileWriter((filePath + SLASH + fileName + ".wal"), false);
					final BufferedWriter out = new BufferedWriter(fstream);
					// Reset the nodes file
					final FileWriter fstreamr = new FileWriter((filePath + SLASH + fileName + ".nod"), false);
					final BufferedWriter outr = new BufferedWriter(fstreamr);
					// Reset the gradients file
					final FileWriter fstreamg = new FileWriter((filePath + SLASH + fileName + ".gra"), false);
					final BufferedWriter outg = new BufferedWriter(fstreamg);
					// Create file
					final StringBuffer sb = new StringBuffer();
					for (final Object obs : ((IEnvironment2DWithObstacles<?, ?>) env).getObstacles()) {
						sb.append(((IObstacle2D) obs).toString().replace("(", "").replace(")", "").replace(" ", ""));
						sb.append(';');
					}

					out.write(sb.toString());

					// Close the output stream
					out.close();
					fstream.close();
					outr.close();
					fstreamr.close();
					outg.close();
					fstreamg.close();
				} catch (Exception e) { // Catch exception if any
					L.error(e);
				}
			}
			mutex.release();
		} else if (st == 0 || step % st == 0) {
			if (realTime) {
				if (lasttime + timestep > time.toDouble()) {
					return;
				}
				final long timePassed = System.currentTimeMillis() - lastRefresh;
				final long timeSimulated = (long) ((time.toDouble() - lasttime) * MILLIS_PER_SECOND);
				if (timeSimulated > timePassed) {
					try {
						Thread.sleep(Math.min(timeSimulated - timePassed, MILLIS_PER_SECOND / FRAME_RATE));
					} catch (InterruptedException e) {
						L.error("Damn spurious wakeups.");
					}
				}
			}
			mutex.acquireUninterruptibly();
			update(env, time);
			if (step % stepRate == 0) {
				try {
					final FileWriter fstreamn = new FileWriter((filePath + SLASH + fileName + ".nod"), true);
					final BufferedWriter outn = new BufferedWriter(fstreamn);
					final FileWriter fstreamn2 = new FileWriter((filePath + SLASH + fileName + ".gra"), true);
					final BufferedWriter outn2 = new BufferedWriter(fstreamn2);
					// Create file
					final StringBuffer sbn = new StringBuffer();
					final StringBuffer sbn2 = new StringBuffer();
					sbn.append(String.format("%.5f", time.toDouble()) + ";" + String.format("%05d", step) + ";");
					sbn2.append(String.format("%.5f", time.toDouble()) + ";" + String.format("%05d", step) + ";");
					for (final INode<List<? extends ILsaMolecule>> n : env.getNodes()) {
						sbn.append(Integer.toString(n.getId()));
						sbn.append(',');
						pos = env.getPosition(n).toString();
						sbn.append(pos);
						sbn.append(',');
						cont = (n.getContents().keySet()).toString();
						sbn.append(cont.replace(';', ':').replace(',', ':'));
						sbn.append(';');
						if (!n.getContents().keySet().isEmpty()) {
							final IMolecule mol = n.getContents().keySet().iterator().next();
							if (mol != null && n.getConcentration(mol).toString().contains("grad, target, ")) {
								sbn2.append(Integer.toString(n.getId()));
								sbn2.append(',');
								sbn2.append(pos);
								sbn2.append(',');
								sbn2.append(n.getConcentration(mol).toString().replace("grad, target, ", ""));
								sbn2.append(';');
							}
						}
					}

					sbn.append("\r\n");
					sbn2.append("\r\n");
					outn.write(sbn.toString().replace("[", "").replace("]", "").replace("<", "").replace(">", ""));
					outn2.write(sbn2.toString().replace("[", "").replace("]", "").replace("<", "").replace(">", ""));
					// Close the output stream
					outn.close();
					fstreamn.close();
					outn2.close();
					fstreamn2.close();
				} catch (Exception e) { // Catch exception if any
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
		lastRefresh = System.currentTimeMillis();
		lasttime = time.toDouble();
		computeNodes(env);
	}

	/**
	 * Initializes the nodes and obstacles
	 */
	private void initAll(final IEnvironment<Double, Double, List<? extends ILsaMolecule>> env) {
		computeNodes(env);
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
	 * May be needed to reduce the output rows.
	 * @param fsr frame step rate
	 */
	public void setFrameStepRate(final int fsr) {
		stepRate = fsr;
	}

	/**
	 * @return the step number
	 */
	public int getStep() {
		return st;
	}

	/**
	 * Call this method if you want this monitor to be bound to a new
	 * environment.
	 */
	public void reset() {
		firstTime = true;
	}

	@Override
	public void initialized(final IEnvironment<Double, Double, List<? extends ILsaMolecule>> env) {
	}

	@Override
	public void finished(final IEnvironment<Double, Double, List<? extends ILsaMolecule>> env, final ITime time, final long step) {
	}

}

