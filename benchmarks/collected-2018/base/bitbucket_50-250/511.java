// https://searchcode.com/api/result/118212621/

import java.util.List;

/** Class SOM adalah representasi dari algoritma Self Organizing Map
*	sementara radius masih tetap
* @author	M Iqbal Tawakal (mitbal)
*/
public class SOM {
	
	public static enum Topology {
		LINEAR,
		RECTANGULAR,
	}
	
	private final Topology topology;
	private int numFeature;
	private int numNeuron;
	private Neuron[] neurons;
	
	private int numNeuronVert;
	private int numNeuronHori;
	
	private double initLearningRate = 0.1;
	private double learningRate;
	
	// Default constructor, for testing purpose only
	public SOM() {topology = Topology.LINEAR;}
	
	public SOM(Topology t, int numNeuron, int feature) {
		topology = t;
		neurons = new Neuron[numNeuron];
		numFeature = feature;
		initializeNeuron();
	}
	
	public SOM(Topology t, int numNeuronVert, int numNeuronHori, int feature) {
		topology = t;
		this.numNeuronVert = numNeuronVert;
		this.numNeuronHori = numNeuronHori;
		numFeature = feature;
		numNeuron = numNeuronVert * numNeuronHori;
		neurons = new Neuron[numNeuron];
		initializeNeuron();
	}
	
	// Inisialisasi neuron sebanyak numNeuron, tiap neuron mempunya weights sebanyak numFeature
	private void initializeNeuron() {
		if(topology == Topology.LINEAR) {
			for(int i=0; i<neurons.length; i++) {
				neurons[i] = new Neuron(i, i, 0, 0, numFeature);
			}
		} else if(topology == Topology.RECTANGULAR) {
			int index = 0;
			for(int i=0; i<numNeuronVert; i++) {
				for(int j=0; j<numNeuronHori; j++) {
					neurons[index] = new Neuron(i, i, j, j, numFeature);
					index++;
				}
			}
		}
	}
	
	// return true jika sukses atau training pernah dilakukan sebelumnya
	public boolean epoch(List<List<Double>> data) {
		
		learningRate = initLearningRate;
		// Cek panjang vektor input dengan panjang weight di neuron
		if(data.get(0).size() != numFeature) {
			return false;
		}
		
		int numIterations = data.size();
		int iterationCount = 0;
		
		// Start training, berbeda dengan yang ditunjukkan di ai-junkie
		// di sini dipakai seperti nn backprop, yaitu pakai epoch. satu epoch 
		// sama dengan satu training set.
		for(List<Double> vector : data) {
			Neuron winner = findBestMatchingNeuron(vector);
			
			// Calculate radius of this time timestep
			double radius = 0;
			
			// Iterasi semua node cari yang jaraknya masuk dalam radius
			for(Neuron n : neurons) {
				//System.out.println("n : "+n);
				//System.out.println("winner : "+winner);
				double distToNeuronSq = Math.pow(n.getX()-winner.getX(), 2) + Math.pow(n.getY()-winner.getY(), 2);
				double radiusSq = radius*radius;
				if(distToNeuronSq <= radiusSq) {
					// Besarnya perubahan terhadap weights tergantung dari jarahknya neuron ke neuron pemenang
					double influence = calcInfluence(distToNeuronSq, radiusSq);
					n.adjustWeight(vector, learningRate, influence);
				}
			}
			// Reduce the learning rate
			reduceLearningRate(iterationCount, numIterations);
			iterationCount++;
			
			// Laporan
			//System.out.println("Iterasi ke : "+iterationCount);
			//System.out.println("Input : "+vector.get(0));
			//System.out.println("Vektor pemenang : "+winner.getX());
			//System.out.println("Learning Rate : "+learningRate);
		}
		return true;
	}
	
	// Efek dari learning seharusnya proporsional terhadap jarak sebuah neuron terhadap vektor pemenang
	public double calcInfluence(double dist, double rad) {
		return Math.exp(-dist / 2*rad);
	}
	
	// Tiap iterasi, learning rate berkurang hingga nanti di akhir-akhir iterasi mendekati nol
	// method yang dipakai bisa beragam, disini antara linear dengan exponential decay
	private void reduceLearningRate(int iterationCount, int numIterations) {
		learningRate = expDecay(initLearningRate, iterationCount, numIterations);
	}
	
	// Fungsi exponential decay
	public double expDecay(double init, int inc, int max) {
		return init * Math.exp(-(double)inc/max);
	}
	
	// Fungsi decay secara linear
	public double linearDecay(double init, int inc, int max) {
		return init - ((double)inc/max)*init;
	}
	
	// Mencari neuron yang jarak euclidean nya paling dekat dengan input vektor sekarang
	private Neuron findBestMatchingNeuron(List<Double> vec) {
		Neuron candidate = null;
		double minDistance = Integer.MAX_VALUE;
		double dist = 0;
		for(int i=0; i<neurons.length; i++) {
			Neuron n = neurons[i];
			dist = n.getDistance(vec);
			//System.out.println("distance : "+dist);
			if(dist < minDistance) {
				minDistance = dist;
				candidate = n;
			}
		}
		return candidate;
	}
	
	// Untuk melihat weight dari neuron
	public void printNeuronWeights() {
		for(int i=0; i<neurons.length; i++) {
			System.out.println("Neuron ke : "+i);
			List<Double> weights = neurons[i].getWeights();
			for(int j=0; j<weights.size(); j++) {
				System.out.print(weights.get(j)+" ");
			}
			System.out.println();
		}
	}
	
	public String classify(List<Double> vec) {
		Neuron n = findBestMatchingNeuron(vec);
		return n.getClassTarget();
	}
	
	public double testing(List<List<Double>> input, List<String> classTarget) {
		int total = input.size();
		int rightCount = 0;
		double percentage = 0;
		for(int i=0; i<total; i++) {
			List<Double> vector = input.get(i);
			String ans = classify(vector);
			if(ans.equals(classTarget.get(i))) {
				rightCount++;
			}
			System.out.println("Jawabnya :"+ans+" Targetnya : "+classTarget.get(i));
		}
		percentage = ((double)rightCount/total)*100;
		System.out.println("Akurasinya : "+((double)rightCount/total)*100);
		return percentage;
	}
	
	public void assignClass(int neu, String classTarget) {
		neurons[neu].setClassTarget(classTarget);
	}
	
	public Neuron[] getNeurons() {
		return neurons;
	}
}

