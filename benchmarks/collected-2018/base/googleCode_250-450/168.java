// https://searchcode.com/api/result/14182605/

package timing;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import network.Address;
import network.Data;
import network.IConnectionAdaptor;
import network.IConnectionMedium;
import network.IData;
import network.INode;
import network.IPacket;
import network.NodeSimulatableListener;
import network.Packet;

import cellTests.Cell;

import routing.IAddress;
import simulation.AbstractSimulatable;
import simulation.ISimulatable;
import simulation.ISimulatableEvent;
import simulation.ISimulatableListener;
import simulation.ISimulator;
import simulation.ISimulatorEvent;
import simulation.Simulator;

/**
 * Scales scalable objects to perform, at maximum, one operation per tick and
 * ensures that everyone managed follows this principle.
 * @author Alex Maskovyak
 *
 */
public class OperationTimeScaler implements IOperationTimeScaler {

	/** time scalables to scale. */
	protected List<ITimeScalable> _timeScalables;
	/** holds the fractional amounts for computing the new flattened scale value. */
	protected List<BigFraction> _timeFractions;
	
	/**
	 * Default constructor.
	 */
	public OperationTimeScaler() {
		init();
	}
	
	/**
	 * Initialize member variables.
	 */
	protected void init() {
		_timeScalables = new ArrayList<ITimeScalable>();
		_timeFractions = new ArrayList<BigFraction>();
	}
	
	/*
	 * (non-Javadoc)
	 * @see timing.IOperationTimeScaler#register(timing.ITimeScalable)
	 */
	@Override
	public void register(ITimeScalable timeScalable) {
		_timeScalables.add(timeScalable);
	}

	/*
	 * (non-Javadoc)
	 * @see timing.IOperationTimeScaler#unregister(timing.ITimeScalable)
	 */
	@Override
	public void unregister(ITimeScalable timeScalable) {
		_timeScalables.remove(timeScalable);
	}

	/*
	 * (non-Javadoc)
	 * @see timing.IOperationTimeScaler#rescale()
	 */
	@Override
	public void rescale() {
		// store the primitive fractions
		_timeFractions.clear();
		
		// get fastest performer
		int fastestPerformance = 0;
		for( ITimeScalable ts : _timeScalables ) {
			int currentPerformance = ts.getBaselinePerformance();
			fastestPerformance = (currentPerformance > fastestPerformance) ? currentPerformance : fastestPerformance; 
			_timeFractions.add(new BigFraction(1, currentPerformance));
		}
		
		// multiply all fractions by the fastestperformer
		// reduce the fraction
		// collect the denominators
		Set<BigInteger> denominators = new HashSet<BigInteger>();
		for( int i = 0; i < _timeFractions.size(); ++i ) {
			BigFraction fraction = _timeFractions.get(i);
			BigFraction newFract = fraction.multiply(fastestPerformance);
			_timeFractions.set(i, newFract);
			denominators.add(newFract.denominator());
		}
		
		// remove the denominators by multiplying there value against all fractions present
		for( BigInteger denominator : denominators ) {
			for( int i = 0; i < _timeFractions.size(); ++i ) {
				BigFraction fraction = _timeFractions.get(i);
				BigFraction newFract = fraction.multiply(denominator);
				_timeFractions.set(i, newFract );
			}
		}
		
		// set everyone's new tick response rate
		for( int i = 0; i < _timeFractions.size(); ++i ) {
			ITimeScalable ts = _timeScalables.get(i);
			BigFraction fract = _timeFractions.get(i);
			ts.scaleTickResponse(fract.numerator().intValue());
		}
	}

	/**
	 * Test the operation scaler.
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String... args) throws InterruptedException {
		ITimeScalable t1 = new TimeScalableSimulatable(2000);
		ITimeScalable t2 = new TimeScalableSimulatable(1000);
		ITimeScalable t3 = new TimeScalableSimulatable(800);
		ITimeScalable t4 = new TimeScalableSimulatable(600);
		ITimeScalable t5 = new TimeScalableSimulatable(500);
		ITimeScalable t6 = new TimeScalableSimulatable(400);
		ITimeScalable t7 = new TimeScalableSimulatable(550);

		IOperationTimeScaler scaler = new OperationTimeScaler();
		scaler.register(t1);
		scaler.register(t2);
		scaler.register(t3);
		scaler.register(t6);
		scaler.register(t7);
		scaler.register(t4);
		scaler.register(t5);
		scaler.rescale();
		
		System.out.println(t1.getScaledPerformance());
		System.out.println(t2.getScaledPerformance());
		System.out.println(t3.getScaledPerformance());
		System.out.println(t4.getScaledPerformance());
		System.out.println(t5.getScaledPerformance());
		System.out.println(t6.getScaledPerformance());
		System.out.println(t7.getScaledPerformance());
		
		ISimulator sim = new Simulator();
		scaler = new OperationTimeScaler();
		ISimulatable s1 = new TestTimeScalableSimulatable(1000, 1);
		ISimulatable s2 = new TestTimeScalableSimulatable(500, 2);
		scaler.register((ITimeScalable)s1);
		scaler.register((ITimeScalable)s2);
		scaler.rescale();
		sim.registerSimulatable(s1);
		sim.registerSimulatable(s2);
		System.out.println("blah");
		sim.start();
		sim.simulate(10);
		System.out.println("blah");
		
		/*Thread t = new Thread((Runnable)sim);
		t.start();
		t.join();	
		//sim.unregisterSimulatable((ISimulatable)n);
		sim.start();
		sim.simulate(5);
*/
	}
}

class TestTimeScalableSimulatable 
		extends TimeScalableSimulatable 
		implements INode, ITimeScalable, ISimulatable {

	protected int _baselinePerformance;
	protected int _scaledPerformance;
	
	protected int _ticksReceived;
	
	protected IAddress _id;
	
	public TestTimeScalableSimulatable(int baselinePerformance, int address) {
		super( baselinePerformance );
		_id = new Address(address);
		this.addListener( new NodeSimulatableListener(System.out) );
	}

	@Override
	public IAddress getAddress() {
		return _id;
	}

	@Override
	public void receive(IPacket packet) {
		// TODO Auto-generated method stub
		
	}

	public void registerConnection(IConnectionMedium connection) {
		// TODO Auto-generated method stub
		
	}

	public void send(IPacket packet) {
		// TODO Auto-generated method stub
		
	}

	public void unregisterConnection(IConnectionMedium connection) {
		// TODO Auto-generated method stub
		
	}

	public void send(Data data, IAddress address) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int compareTo(INode o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public INode createNew(IAddress address) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addAdaptor(IConnectionAdaptor adaptor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<IConnectionAdaptor> getAdaptors() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeAdaptor(IConnectionAdaptor adaptor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send(Object data, IAddress address) {
		// TODO Auto-generated method stub
		
	}
}

