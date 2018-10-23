// https://searchcode.com/api/result/60419562/

package com.ankurdave.boggle;

/**
 * Worker thread called by {@link GeneticClient} in order to perform the heavy lifting -- the {@link Board} optimization -- using a genetic algorithm. Reports results back to {@link GeneticClient}.
 */
public class GeneticClientThread extends Thread {
	private Population bp;
	private GeneticClient manager;
	
	
	public GeneticClientThread(GeneticClient manager) {
		this.manager = manager;
		bp = new Population();
	}
	
	public void setDictionary(Dictionary dict) {
		bp.setDictionary(dict);
	}
	
	public void setSideLength(int sideLength) {
		bp.setSideLength(sideLength);
	}
	
	public void setStartingPopulation(int startingPopulation) {
		bp.setStartingPopulation(startingPopulation);
	}
	
	public void setChildrenPerCouple(int childrenPerCouple) {
		bp.setChildrenPerCouple(childrenPerCouple);
	}
	
	public void setPopCap(int popCap) {
		bp.setPopCap(popCap);
	}
	
	@Override
	public void run() {
		while (!Thread.interrupted()) {
			try {
				// Evolve the population
				bp.evolve();
				
				// Print debugging information
				System.out.println(bp);
				// for (GeneticBoard b : bp.getCurrentGeneration()) {
				// System.err.println(b);
				// }
				// System.out.println();
				
				// Send every board as a potential highest board for consideration in the score charts
				// TODO: do some preliminary weeding to reduce network load
				for (GeneticBoard b : bp.getCurrentGeneration()) {
					manager.sendPotentialHighest(b);
				}
				
				// Occasionally, send a migrant (about once every 5 generations, or 20% of the time)
				if (Math.random() > 0.20) {
					manager.migrate(Util.weightedRandomFromList(bp.getCurrentGeneration()));
				}
			} catch (GenerationEmptyException e) {
				System.err.println(e);
				break;
			}
		}
	}
	
	public synchronized void addBoard(GeneticBoard board) {
		bp.add(board);
	}
}

