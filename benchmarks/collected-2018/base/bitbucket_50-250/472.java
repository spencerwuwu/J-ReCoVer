// https://searchcode.com/api/result/102112732/

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.apache.commons.httpclient.methods.TraceMethod;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import eu.mosaic_cloud.benchmarks.prototype.Unit;
import eu.mosaic_cloud.benchmarks.prototype.Unit.StatisticsReducer;
import eu.mosaic_cloud.benchmarks.store.Store;
import eu.mosaic_cloud.checkout.dao.MQConfigObject;
import eu.mosaic_cloud.checkout.dao.MQConnection;
import eu.mosaic_cloud.checkout.dao.MQConsumer;
import eu.mosaic_cloud.checkout.dao.MQPublisher;
import eu.mosaic_cloud.checkout.lib.Agent;
import eu.mosaic_cloud.checkout.lib.Constants;
import eu.mosaic_cloud.checkout.obj.CheckoutTransaction;
import eu.mosaic_cloud.checkout.obj.ConfigObj;
import eu.mosaic_cloud.external.lib.CreditCardGenerator;

public class feedTransactions2 implements Runnable, Agent {

	/**
	 * 
	 * just for testing; it feeds Billing agent directly!
	 * 
	 * @param args
	 */

	private MQPublisher<CheckoutTransaction> mqPublisher;
	private MQConsumer<String> mqConsumer;
	private long startTime;
	private boolean pub = true;
	private Semaphore semaphore;
	private int slots;
	private int iterations;
	private ConcurrentHashMap<String, Unit> units;
	private StatisticsReducer reducer;
	
	public feedTransactions2(Semaphore semaphore, boolean publish, int slots, int iterations, ConcurrentHashMap<String, Unit> units, StatisticsReducer reducer) {
		this.pub = publish;
		System.out.println();
		if (pub) {
			this.mqPublisher = new MQPublisher<CheckoutTransaction>(
					Constants.CHECKOUT_TRANS_REQ_QUEUE,
					Constants.CHECKOUT_TRANS_REQ_RK, CheckoutTransaction.class, true);
		} else {
			this.mqConsumer = new MQConsumer<String>("replier", "replierRK", String.class);
		}
		this.semaphore = semaphore;
		startTime = System.currentTimeMillis();
		this.iterations = iterations;
		this.slots = slots;
		this.units = units;
		this.reducer = reducer;
	}

	public void stop() {
			mqPublisher.destroy();
			MQConnection.destroyConnection();
	}
	
	public synchronized boolean consume(int slots, int iter) {
		try {
			String header = "[feeder-"+Thread.currentThread().getName()+"]";
			System.out.println(header + " Consumer thread");
			float transactionNo = 0;
			System.out.println(slots+":"+iter+":"+(slots*iter));
			while (transactionNo < ((slots*iter))) {
				String corrId = mqConsumer.getNextMessage();
				mqConsumer.acknowledge();
				Unit unit = this.units.remove(corrId);
				unit.completed();
				this.reducer.reduce(unit);
				semaphore.release();
				transactionNo++;
			}
			System.out.println(header + "[DONE] Consumed: " + transactionNo);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.mqConsumer.destroy();
			MQConnection.destroyConnection();
		}
		return true;
	}
	public synchronized boolean produce(int slots, int iter) {
		try {
			String header = "[feeder-"+Thread.currentThread().getName()+"]";
			System.out.println(header + " Publisher thread");
			CreditCardGenerator ccgen = new CreditCardGenerator();
			final SecureRandom rand = SecureRandom.getInstance("SHA1PRNG", "SUN");
			final SecureRandom rand2 = SecureRandom.getInstance("SHA1PRNG", "SUN");
			List<String> prods = new ArrayList<String>();
			int transactionNo = 0;
			while (transactionNo < (slots*iter)) {
				while (semaphore.tryAcquire()) {
					transactionNo++;
					CheckoutTransaction ccprods = new CheckoutTransaction();
					ccprods.setTransactionID(UUID.randomUUID().toString());
					ccprods.setCreditCardNumber(ccgen.generateMasterCardNumber());
					prods.clear();
					prods.add(Integer.toString(rand.nextInt(100000)));
					for (int j = 0; j < rand2.nextInt(10); j++)
						prods.add(Integer.toString(rand.nextInt(100000)));
					ccprods.setProductList(prods);
					//System.out.print("\r" + header + "[NEW]  Feeding: " + transactionNo);
					String corrId = UUID.randomUUID().toString();
					ccprods.setCorrelationID(corrId);
					Unit unit = new Unit(null);
					unit.created();
					this.units.put(corrId, unit);
					mqPublisher.publishMessage(ccprods, corrId, "replier");
				}
			}
			System.out.println("\n" + header + "[DONE] Feeds: "+transactionNo);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			//this.mqPublisher.destroy();
		}
		
		return true;
	}

	public void run() {
		if (pub) 
			produce(this.slots, this.iterations);
		else
			consume(this.slots, this.iterations);
	}

	public static void main(String[] args) throws Exception {

		if (args.length < 3) {
			System.out.println("\nUsage: feedTransactions SLOTS ITERATION EXPERIMENT_NAME\n");
			System.exit(1);
		}

		ConfigObj conf = new ConfigObj();
		MQConfigObject mqConfig = MQConfigObject.getConfiguration();
		mqConfig.setHostname(conf.getMqAddress());
		mqConfig.setPort(conf.getMqPort());

		System.out.println("Feeding MQ using 2 concurent threads:");
		System.out.println();
		final Semaphore semaphore = new Semaphore(Integer.parseInt(args[0]));
		Thread[] workers = new Thread[2];
		feedTransactions2[] agents = new feedTransactions2[2];
		ConcurrentHashMap<String, Unit> units = new ConcurrentHashMap<String, Unit>();
		StatisticsReducer reducer = new StatisticsReducer();
		
		long start = System.currentTimeMillis();
		
		for (int i = 0; i < 2; i++) {
			agents[i] = new feedTransactions2(
					semaphore,
					i == 1 ? true : false,
					Integer.parseInt(args[0]),
					Integer.parseInt(args[1]),
					units, reducer);
			workers[i] = new Thread(agents[i]);
			workers[i].setDaemon(true);
			workers[i].start();
		}
		ShutdownHook<feedTransactions2> shutdownHook = new ShutdownHook<feedTransactions2>(
				workers, agents);
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		for (int i = 0; i < 2; i++)
			workers[i].join();
		
		long stop = System.currentTimeMillis();
		
		StatisticsReducer.Outcome outcome = reducer.outcome();
		outcome.elapsed = (stop - start) / 1000.0;
		outcome.throughput = outcome.count / outcome.elapsed;
		
		Store store = new Store (conf.getCdbAddress(), conf.getCdbPort(), conf.getCdbDnName(), conf.getCdbUsername(), conf.getCdbPassword());
		JsonObject report = new JsonObject();
		{
			JsonObject config = new JsonObject();
			config.addProperty("buffer", Integer.parseInt(args[0]));
			config.addProperty("billing_agents", conf.getThreadNo());
			config.addProperty("product_agents", conf.getSrvThreadNo());
			config.addProperty("charge_agents", conf.getSrvThreadNo());
			
			report.addProperty("timestamp", System.currentTimeMillis());
			report.add("configuration", config);
			report.addProperty("experiment", args[2]);
		}
		report.add("outcome", new Gson().toJsonTree(outcome));
		store.store(null, "benchmark-outcome:sync", report);
	}
}

