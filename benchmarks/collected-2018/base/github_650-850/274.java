// https://searchcode.com/api/result/96208555/

package gdsc.smlm.ij.plugins;

/*----------------------------------------------------------------------------- 
 * GDSC Plugins for ImageJ
 * 
 * Copyright (C) 2011 Alex Herbert
 * Genome Damage and Stability Centre
 * University of Sussex, UK
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *---------------------------------------------------------------------------*/

import gdsc.smlm.ij.plugins.ResultsManager.InputSource;
import gdsc.smlm.ij.utils.CoordinateProvider;
import gdsc.smlm.ij.utils.ImageROIPainter;
import gdsc.smlm.ij.utils.Utils;
import gdsc.smlm.results.FilePeakResults;
import gdsc.smlm.results.MemoryPeakResults;
import gdsc.smlm.results.PeakResult;
import gdsc.smlm.results.match.BasePoint;
import gdsc.smlm.results.match.Coordinate;
import gdsc.smlm.results.match.MatchCalculator;
import gdsc.smlm.results.match.MatchResult;
import gdsc.smlm.results.match.PointPair;
import ij.IJ;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.text.TextWindow;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares the coordinates in two sets of results and computes the match statistics.
 */
public class ResultsMatchCalculator implements PlugIn, CoordinateProvider
{
	private static String TITLE = "Results Match Calculator";

	private static String inputOption1 = "";
	private static String inputOption2 = "";
	private static double dThreshold = 0.5;
	private static int increments = 5;
	private static double delta = 0.1;
	private static double beta = 4;
	private static boolean showTable = true;
	private static boolean showPairs = false;
	private static boolean saveClassifications = false;
	private static String classificationsFile = "";
	private static boolean idAnalysis = false;

	private static boolean writeHeader = true;
	private static TextWindow resultsWindow = null;
	private static TextWindow pairsWindow = null;
	private static ImageROIPainter pairPainter = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	public void run(String arg)
	{
		if (MemoryPeakResults.countMemorySize() == 0)
		{
			IJ.error(TITLE, "No localisations in memory");
			return;
		}

		if (!showDialog())
			return;

		// Load the results
		MemoryPeakResults results1 = ResultsManager.loadInputResults(inputOption1, false);
		MemoryPeakResults results2 = ResultsManager.loadInputResults(inputOption2, false);
		if (results1 == null || results1.size() == 0 || results2 == null || results2.size() == 0)
		{
			IJ.error(TITLE, "No results could be loaded");
			IJ.showStatus("");
			return;
		}

		final long start = System.nanoTime();
		compareCoordinates(results1, results2, dThreshold, increments, delta);
		double seconds = (System.nanoTime() - start) / 1000000000.0;

		IJ.showStatus(String.format("%s = %ss", TITLE, Utils.rounded(seconds, 4)));
	}

	private boolean showDialog()
	{
		GenericDialog gd = new GenericDialog(TITLE);

		gd.addMessage("Compare the points in two results sets\nand compute the match statistics");
		ResultsManager.addInput(gd, "Results1", inputOption1, InputSource.MEMORY);
		ResultsManager.addInput(gd, "Results2", inputOption2, InputSource.MEMORY);
		gd.addNumericField("Distance", dThreshold, 2);

		gd.addSlider("Increments", 0, 10, increments);
		gd.addNumericField("Delta", delta, 2);
		gd.addNumericField("Beta", beta, 2);
		gd.addCheckbox("Show_table", showTable);
		gd.addCheckbox("Show_pairs", showPairs);
		gd.addCheckbox("Save_classifications", saveClassifications);
		gd.addCheckbox("Id_analysis", idAnalysis);

		gd.showDialog();

		if (gd.wasCanceled())
			return false;

		inputOption1 = gd.getNextChoice();
		inputOption2 = gd.getNextChoice();
		dThreshold = gd.getNextNumber();
		increments = (int) gd.getNextNumber();
		delta = gd.getNextNumber();
		beta = gd.getNextNumber();
		showTable = gd.getNextBoolean();
		showPairs = gd.getNextBoolean();
		saveClassifications = gd.getNextBoolean();
		idAnalysis = gd.getNextBoolean();

		if (!(showTable || showPairs || saveClassifications))
		{
			IJ.error(TITLE, "No outputs specified");
			return false;
		}

		// Check arguments
		try
		{
			Parameters.isAboveZero("Distance threshold", dThreshold);
			Parameters.isPositive("Increments", increments);
			Parameters.isAboveZero("Delta", delta);
			Parameters.isPositive("Beta", beta);
		}
		catch (IllegalArgumentException e)
		{
			IJ.error(TITLE, e.getMessage());
			return false;
		}

		return true;
	}

	private void compareCoordinates(MemoryPeakResults results1, MemoryPeakResults results2, double dThreshold,
			int increments, double delta)
	{
		int tp = 0, fp = 0, fn = 0;
		//double rmsd = 0;

		boolean requirePairs = showPairs || saveClassifications;

		FilePeakResults fileResults = createFilePeakResults(results2);

		List<PointPair> allMatches = new LinkedList<PointPair>();
		List<PointPair> pairs = (requirePairs) ? new LinkedList<PointPair>() : null;
		List<PeakResult> actualPoints = results1.getResults();
		List<PeakResult> predictedPoints = results2.getResults();

		double maxDistance = dThreshold + increments * delta;

		// Old implementation
		//// Process each time point
		//for (Integer t : getTimepoints(actualPoints, predictedPoints))
		//{
		//	Coordinate[] actual = getCoordinates(actualPoints, t);
		//	Coordinate[] predicted = getCoordinates(predictedPoints, t);

		// Divide the results into time points
		HashMap<Integer, ArrayList<Coordinate>> actualCoordinates = getCoordinates(actualPoints);
		HashMap<Integer, ArrayList<Coordinate>> predictedCoordinates = getCoordinates(predictedPoints);

		// Process each time point
		for (Integer t : getTimepoints(actualCoordinates, predictedCoordinates))
		{
			Coordinate[] actual = getCoordinates(actualCoordinates, t);
			Coordinate[] predicted = getCoordinates(predictedCoordinates, t);

			List<Coordinate> TP = null;
			List<Coordinate> FP = null;
			List<Coordinate> FN = null;
			List<PointPair> matches = new LinkedList<PointPair>();
			if (requirePairs)
			{
				FP = new LinkedList<Coordinate>();
				FN = new LinkedList<Coordinate>();
			}

			MatchResult result = MatchCalculator.analyseResults2D(actual, predicted, maxDistance, TP, FP, FN, matches);

			// Aggregate
			tp += result.getTruePositives();
			fp += result.getFalsePositives();
			fn += result.getFalseNegatives();
			//rmsd += (result.getRMSD() * result.getRMSD()) * result.getTruePositives();

			allMatches.addAll(matches);
			if (showPairs)
			{
				pairs.addAll(matches);
				for (Coordinate c : FN)
					pairs.add(new PointPair(c, null));
				for (Coordinate c : FP)
					pairs.add(new PointPair(null, c));
			}
			if (fileResults != null)
			{
				// Matches are marked in the original value with 1 for true, 0 for false 
				for (PointPair pair : matches)
				{
					PeakResult p = ((PeakResultPoint) pair.getPoint2()).peakResult;
					fileResults.add(p.peak, p.origX, p.origY, 1, p.error, p.noise, p.params, null);
				}
				for (Coordinate c : FP)
				{
					PeakResult p = ((PeakResultPoint) c).peakResult;
					fileResults.add(p.peak, p.origX, p.origY, 0, p.error, p.noise, p.params, null);
				}
			}
		}

		if (fileResults != null)
			fileResults.end();

		boolean doIdAnalysis1 = (idAnalysis) ? haveIds(results1) : false;
		boolean doIdAnalysis2 = (idAnalysis) ? haveIds(results2) : false;
		boolean doIdAnalysis = doIdAnalysis1 || doIdAnalysis2;

		// Create output
		if (!java.awt.GraphicsEnvironment.isHeadless())
		{
			String header = createResultsHeader(doIdAnalysis);
			Utils.refreshHeadings(resultsWindow, header, true);

			if (showTable && (resultsWindow == null || !resultsWindow.isShowing()))
			{
				resultsWindow = new TextWindow(TITLE + " Results", header, "", 900, 300);
			}
			if (showPairs)
			{
				if (pairsWindow == null || !pairsWindow.isShowing())
				{
					pairsWindow = new TextWindow(TITLE + " Pairs", createPairsHeader(pairs), "", 900, 300);
					if (resultsWindow != null)
					{
						Point p = resultsWindow.getLocation();
						p.y += resultsWindow.getHeight();
						pairsWindow.setLocation(p);
					}
					pairPainter = new ImageROIPainter(pairsWindow.getTextPanel(), "", this);
				}
				pairsWindow.getTextPanel().clear();
				String title = "Results 1";
				if (results1.getSource() != null && results1.getSource().getOriginal().getName().length() > 0)
					title = results1.getSource().getOriginal().getName();
				pairPainter.setTitle(title);
				for (PointPair pair : pairs)
					addPairResult(pair);
			}
		}
		else
		{
			if (writeHeader && showTable)
			{
				writeHeader = false;
				IJ.log(createResultsHeader(idAnalysis));
			}
		}

		if (!showTable)
			return;

		TreeSet<Integer> id1 = (doIdAnalysis1) ? getIds(results1) : null;
		TreeSet<Integer> id2 = (doIdAnalysis2) ? getIds(results2) : null;

		// We have the results for the largest distance.
		// Now reduce the distance threshold and recalculate the results
		double[] distanceThresholds = getDistances(dThreshold, increments, delta);
		double[] pairDistances = getPairDistances(allMatches);
		for (double distanceThreshold : distanceThresholds)
		{
			double rms = 0;
			int tp2 = 0;
			final double d2 = distanceThreshold * distanceThreshold;
			for (double d : pairDistances)
			{
				if (d <= d2)
				{
					rms += d;
					tp2++;
				}
			}
			// All non-true positives must be added to the false totals.
			int fp2 = fp + tp - tp2;
			int fn2 = fn + tp - tp2;

			MatchResult result = new MatchResult(tp2, fp2, fn2, (tp2 > 0) ? Math.sqrt(rms / tp2) : 0);

			MatchResult idResult1 = null, idResult2 = null;
			if (doIdAnalysis)
			{
				TreeSet<Integer> matchId1 = (doIdAnalysis1) ? new TreeSet<Integer>() : null;
				TreeSet<Integer> matchId2 = (doIdAnalysis2) ? new TreeSet<Integer>() : null;
				int i = 0;
				for (PointPair pair : allMatches)
				{
					if (pairDistances[i++] <= d2)
					{
						if (doIdAnalysis1)
							matchId1.add(((PeakResultPoint) pair.getPoint1()).peakResult.getId());
						if (doIdAnalysis2)
							matchId2.add(((PeakResultPoint) pair.getPoint2()).peakResult.getId());
					}
				}
				// Only the actual points are checked for Ids. For example these could be from the 
				// Create Data plugin with actual fluorophore Ids.
				// => Only the recall will be valid: tp / (tp + fn)
				if (doIdAnalysis1)
					idResult1 = new MatchResult(matchId1.size(), 0, id1.size() - matchId1.size(), 0);
				if (doIdAnalysis2)
					idResult2 = new MatchResult(matchId2.size(), 0, id2.size() - matchId2.size(), 0);
			}

			addResult(inputOption1, inputOption2, distanceThreshold, result, idResult1, idResult2);
		}
	}

	@SuppressWarnings("unused")
	private boolean haveIds(MemoryPeakResults results1, MemoryPeakResults results2)
	{
		return haveIds(results1) && haveIds(results2);
	}

	private boolean haveIds(MemoryPeakResults results)
	{
		final int id = results.getResults().get(0).getId();
		for (PeakResult r : results.getResults())
			if (id != r.getId())
				return true;
		return false;
	}

	private FilePeakResults createFilePeakResults(MemoryPeakResults results2)
	{
		if (!saveClassifications)
			return null;
		String[] path = Utils.decodePath(classificationsFile);
		OpenDialog chooser = new OpenDialog("Classifications_File", path[0], path[1]);
		if (chooser.getFileName() != null)
		{
			classificationsFile = chooser.getDirectory() + chooser.getFileName();
			FilePeakResults r = new FilePeakResults(classificationsFile, false, false);
			r.copySettings(results2);
			r.setPeakIdColumnName("Frame");
			r.begin();
			return r;
		}
		return null;
	}

	/**
	 * Build a map between the peak id (time point) and a list of coordinates
	 * 
	 * @param results
	 * @return
	 */
	public static HashMap<Integer, ArrayList<Coordinate>> getCoordinates(List<PeakResult> results)
	{
		return getCoordinates(results, false);
	}

	/**
	 * Build a map between the peak id (time point) and a list of coordinates
	 * 
	 * @param results
	 * @param integerCoordinates
	 *            True if the values should be rounded down to integers
	 * @return
	 */
	public static HashMap<Integer, ArrayList<Coordinate>> getCoordinates(List<PeakResult> results,
			boolean integerCoordinates)
	{
		HashMap<Integer, ArrayList<Coordinate>> coords = new HashMap<Integer, ArrayList<Coordinate>>();
		if (results.size() > 0)
		{
			ResultsMatchCalculator instance = new ResultsMatchCalculator();

			// Do not use HashMap directly to build the coords object since there 
			// will be many calls to getEntry(). Instead sort the results and use 
			// a new list for each time point
			Collections.sort(results);
			int minT = results.get(0).peak;
			int maxT = results.get(results.size() - 1).getEndFrame();

			// Create lists
			ArrayList<ArrayList<Coordinate>> tmpCoords = new ArrayList<ArrayList<Coordinate>>(maxT - minT + 1);
			for (int t = minT; t <= maxT; t++)
			{
				tmpCoords.add(new ArrayList<Coordinate>());
			}

			// Add the results to the lists
			for (PeakResult p : results)
			{
				final float x = (integerCoordinates) ? (int) p.getXPosition() : p.getXPosition();
				final float y = (integerCoordinates) ? (int) p.getYPosition() : p.getYPosition();
				for (int t = p.peak - minT, i = p.getEndFrame() - p.peak + 1; i-- > 0; t++)
					tmpCoords.get(t).add(instance.new PeakResultPoint(t + minT, x, y, p));
			}

			// Put in the map
			for (int t = minT, i = 0; t <= maxT; t++, i++)
			{
				coords.put(t, tmpCoords.get(i));
			}
		}
		return coords;
	}

	/**
	 * Merge the time points from each map into a single sorted list of unique time points
	 * 
	 * @param actualCoordinates
	 * @param predictedCoordinates
	 * @return a list of time points
	 */
	private int[] getTimepoints(HashMap<Integer, ArrayList<Coordinate>> actualCoordinates,
			HashMap<Integer, ArrayList<Coordinate>> predictedCoordinates)
	{
		Set<Integer> set = new HashSet<Integer>();
		for (Integer i : actualCoordinates.keySet())
			set.add(i);
		for (Integer i : predictedCoordinates.keySet())
			set.add(i);
		int[] t = new int[set.size()];
		int i = 0;
		for (Integer ii : set)
		{
			t[i++] = ii.intValue();
		}
		Arrays.sort(t);
		return t;
	}

	/**
	 * Return an array of coordinates for the given time point. Returns an empty array if there are no coordinates.
	 * 
	 * @param coords
	 * @param t
	 * @return
	 */
	public static Coordinate[] getCoordinates(HashMap<Integer, ArrayList<Coordinate>> coords, Integer t)
	{
		if (coords.containsKey(t))
		{
			return coords.get(t).toArray(new Coordinate[0]);
		}
		else
		{
			return new Coordinate[0];
		}
	}

	/**
	 * Merge the time points from the two sets into a single sorted list of unique time points
	 * 
	 * @param actualPoints
	 * @param predictedPoints
	 * @return
	 */
	@SuppressWarnings("unused")
	private int[] getTimepoints(List<PeakResult> actualPoints, List<PeakResult> predictedPoints)
	{
		Set<Integer> set = new HashSet<Integer>();
		for (PeakResult r : actualPoints)
			set.add(r.peak);
		for (PeakResult r : predictedPoints)
			set.add(r.peak);
		int[] t = new int[set.size()];
		int i = 0;
		for (Integer ii : set)
		{
			t[i++] = ii.intValue();
		}
		Arrays.sort(t);
		return t;
	}

	private String createResultsHeader(boolean idAnalysis)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Image 1\t");
		sb.append("Image 2\t");
		sb.append("Distance (px)\t");
		sb.append("N\t");
		sb.append("TP\t");
		sb.append("FP\t");
		sb.append("FN\t");
		sb.append("Jaccard\t");
		sb.append("RMSD\t");
		sb.append("Precision\t");
		sb.append("Recall\t");
		sb.append("F0.5\t");
		sb.append("F1\t");
		sb.append("F2\t");
		sb.append("F-beta");
		if (idAnalysis)
		{
			sb.append("\tId1-N");
			sb.append("\tId1-TP");
			sb.append("\tId1-Recall");
			sb.append("\tId2-N");
			sb.append("\tId2-TP");
			sb.append("\tId2-Recall");
		}
		return sb.toString();
	}

	private void addResult(String i1, String i2, double dThrehsold, MatchResult result, MatchResult idResult1,
			MatchResult idResult2)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(i1).append("\t");
		sb.append(i2).append("\t");
		sb.append(IJ.d2s(dThrehsold, 2)).append("\t");
		sb.append(result.getNumberPredicted()).append("\t");
		sb.append(result.getTruePositives()).append("\t");
		sb.append(result.getFalsePositives()).append("\t");
		sb.append(result.getFalseNegatives()).append("\t");
		sb.append(IJ.d2s(result.getJaccard(), 4)).append("\t");
		sb.append(IJ.d2s(result.getRMSD(), 4)).append("\t");
		sb.append(IJ.d2s(result.getPrecision(), 4)).append("\t");
		sb.append(IJ.d2s(result.getRecall(), 4)).append("\t");
		sb.append(IJ.d2s(result.getFScore(0.5), 4)).append("\t");
		sb.append(IJ.d2s(result.getFScore(1.0), 4)).append("\t");
		sb.append(IJ.d2s(result.getFScore(2.0), 4)).append("\t");
		sb.append(IJ.d2s(result.getFScore(beta), 4));
		if (idResult1 != null)
		{
			sb.append("\t").append(idResult1.getNumberPredicted());
			sb.append("\t").append(idResult1.getTruePositives());
			sb.append("\t").append(IJ.d2s(idResult1.getRecall(), 4));
		}
		else if (idResult2 != null)
		{
			sb.append("\t-\t-\t-");
		}
		if (idResult2 != null)
		{
			sb.append("\t").append(idResult2.getNumberPredicted());
			sb.append("\t").append(idResult2.getTruePositives());
			sb.append("\t").append(IJ.d2s(idResult2.getRecall(), 4));
		}
		else if (idResult1 != null)
		{
			sb.append("\t-\t-\t-");
		}

		if (java.awt.GraphicsEnvironment.isHeadless())
		{
			IJ.log(sb.toString());
		}
		else
		{
			resultsWindow.append(sb.toString());
		}
	}

	private String createPairsHeader(List<PointPair> pairs)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("T\t");
		sb.append("X1\t");
		sb.append("Y1\t");
		sb.append("Z1\t");
		sb.append("X2\t");
		sb.append("Y2\t");
		sb.append("Z2\t");
		sb.append("Distance\t");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gdsc.smlm.ij.utils.CoordinateProvider#getCoordinates(java.lang.String)
	 */
	public double[] getCoordinates(String line)
	{
		// Extract the startT and x,y coordinates from the first pulse in the line
		final int[] index = { 1, 4 };
		String[] fields = line.split("\t");
		int startT = Integer.valueOf(fields[0]);
		for (int i : index)
		{
			if (i < fields.length)
			{
				if (fields[i].equals("-"))
					continue;
				double x = Double.valueOf(fields[i]);
				double y = Double.valueOf(fields[i + 1]);
				return new double[] { startT, x, y };
			}
		}
		return null;
	}

	private void addPairResult(PointPair pair)
	{
		StringBuilder sb = new StringBuilder();
		PeakResultPoint p1 = (PeakResultPoint) pair.getPoint1();
		PeakResultPoint p2 = (PeakResultPoint) pair.getPoint2();
		int t = (p1 != null) ? p1.getTime() : p2.getTime();
		sb.append(t).append("\t");
		addPoint(sb, p1);
		addPoint(sb, p2);
		double d = pair.getXYDistance();
		if (d >= 0)
			sb.append(Utils.rounded(d, 4)).append("\t");
		else
			sb.append("-\t");
		pairsWindow.append(sb.toString());
	}

	private void addPoint(StringBuilder sb, PeakResultPoint p)
	{
		if (p == null)
		{
			sb.append("-\t-\t-\t");
		}
		else
		{
			sb.append(IJ.d2s(p.getX())).append("\t");
			sb.append(IJ.d2s(p.getY())).append("\t");
			sb.append(IJ.d2s(p.getZ())).append("\t");
		}
	}

	private TreeSet<Integer> getIds(MemoryPeakResults results)
	{
		TreeSet<Integer> ids = new TreeSet<Integer>();
		for (PeakResult p : results.getResults())
			ids.add(p.getId());
		return ids;
	}

	private double[] getDistances(double dThreshold, int increments, double delta)
	{
		double[] d = new double[increments + 1];
		for (int i = 0; i <= increments; i++)
			d[i] = dThreshold + i * delta;
		return d;
	}

	private double[] getPairDistances(List<PointPair> pairs)
	{
		double[] d = new double[pairs.size()];
		int i = 0;
		for (PointPair pair : pairs)
		{
			d[i++] = pair.getXYDistance2();
		}
		return d;
	}

	private class PeakResultPoint extends BasePoint
	{
		int t;
		PeakResult peakResult;

		public PeakResultPoint(int t, float x, float y, PeakResult peakResult)
		{
			super(x, y);
			this.t = t;
			this.peakResult = peakResult;
		}

		public int getTime()
		{
			return t;
		}
	}
}

