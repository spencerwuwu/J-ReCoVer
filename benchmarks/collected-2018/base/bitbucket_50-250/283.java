// https://searchcode.com/api/result/61669175/

package org.discoproject.worker;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;

import org.discoproject.DiscoUtils;
import org.discoproject.worker.protocol.DiscoIOChannel;
import org.discoproject.worker.protocol.decoder.DiscoWorkerDecoder;
import org.discoproject.worker.protocol.decoder.DiscoWorkerListener;
import org.discoproject.worker.protocol.decoder.types.DiscoInput;
import org.discoproject.worker.protocol.decoder.types.DiscoInputReplica;
import org.discoproject.worker.protocol.decoder.types.DiscoTaskMode;
import org.discoproject.worker.protocol.encoder.DoneEncoder;
import org.discoproject.worker.protocol.encoder.ErrorEncoder;
import org.discoproject.worker.protocol.encoder.FatalEncoder;
import org.discoproject.worker.protocol.encoder.MessageEncoder;
import org.discoproject.worker.protocol.encoder.OutputEncoder;
import org.discoproject.worker.protocol.encoder.RequestTaskEncoder;
import org.discoproject.worker.protocol.encoder.WorkerAnnounceEncoder;
import org.discoproject.worker.protocol.encoder.types.OutputType;
import org.discoproject.worker.task.DiscoMapTask;
import org.discoproject.worker.task.DiscoReduceTask;
import org.discoproject.worker.task.DiscoTask;

/**
 * @author Luke Hoersten <lhoersten@allstontrading.com>
 * 
 */
public class DiscoWorker implements DiscoWorkerListener {

	private static final String WORKER_PROTOCOL_VERSION = "1.0";

	private final WorkerAnnounceEncoder workerAnnounceEncoder;
	private final RequestTaskEncoder requestTaskEncoder;
	private final OutputEncoder outputEncoder;
	private final DoneEncoder doneEncoder;

	private final DiscoIOChannel discoIOChannel;
	private DiscoMapTask map;
	private DiscoReduceTask reduce;

	private final ErrorEncoder errorEncoder;
	private final FatalEncoder fatalEncoder;

	private final MessageEncoder messageEncoder;

	public DiscoWorker(final ReadableByteChannel readChannel, final WritableByteChannel writeChannel) {
		this.discoIOChannel = new DiscoIOChannel(readChannel, writeChannel, new DiscoWorkerDecoder().setListener(this));
		this.map = null;
		this.reduce = null;

		this.workerAnnounceEncoder = new WorkerAnnounceEncoder();
		this.requestTaskEncoder = new RequestTaskEncoder();
		this.outputEncoder = new OutputEncoder();
		this.doneEncoder = new DoneEncoder();
		this.errorEncoder = new ErrorEncoder();
		this.fatalEncoder = new FatalEncoder();
		this.messageEncoder = new MessageEncoder();
	}

	public void requestTask() throws IOException {
		if (!hasTask()) {
			discoIOChannel.write(workerAnnounceEncoder.set(WORKER_PROTOCOL_VERSION, DiscoUtils.getPid()));
			discoIOChannel.write(requestTaskEncoder);
		}
	}

	public ReadableByteChannel getMapInput() {
		return map.getMapInput();
	}

	public List<ReadableByteChannel> getReduceInputs() {
		return reduce.getReduceInputs();
	}

	public void reportOutputs(final List<File> outputs) throws IOException {
		for (final File output : outputs) {
			reportOutput(output, OutputType.disco);
		}
	}

	public void reportOutput(final File outputLocation, final OutputType outputType) throws IOException {
		discoIOChannel.write(outputEncoder.set(getWorkingDir(), outputLocation, outputType, ""));
	}

	public void doneReportingOutput() throws IOException {
		discoIOChannel.write(doneEncoder);
	}

	public void reportMessage(final String msg) throws IOException {
		discoIOChannel.write(messageEncoder.set(msg));
	}

	public void reportError(final String msg) throws IOException {
		discoIOChannel.write(errorEncoder.set(msg));
	}

	public void reportFatalError(final String msg) throws IOException {
		discoIOChannel.write(fatalEncoder.set(msg));
	}

	public File getWorkingDir() {
		return getTask().getWorkingDir();
	}

	public boolean hasMapTask() {
		return map != null;
	}

	public boolean hasReduceTask() {
		return reduce != null;
	}

	@Override
	public void task(final String taskHost, final String masterHost, final String jobName, final int taskId, final DiscoTaskMode taskMode,
	        final int discoPort, final int putPort, final File discoData, final File ddfsData, final File jobFile) {

		// TODO use all these other task arguments to optimize input fetching.
		switch (taskMode) {
			case map:
				map = new DiscoMapTask(discoIOChannel, jobName, taskId, discoPort);
				break;
			case reduce:
				reduce = new DiscoReduceTask(discoIOChannel, jobName, taskId, discoPort);
				break;
		}
	}

	@Override
	public void ok() {
		if (hasTask()) {
			getTask().ok();
		}
	}

	@Override
	public void input(final boolean isDone, final List<DiscoInput> inputs) {
		getTask().input(isDone, inputs);
	}

	@Override
	public void fail(final int inputId, final List<Integer> replicaIds) {
		getTask().fail(inputId, replicaIds);
	}

	@Override
	public void retry(final List<DiscoInputReplica> replicas) {
		getTask().retry(replicas);
	}

	@Override
	public void pause(final int seconds) {
		getTask().pause(seconds);
	}

	private DiscoTask getTask() {
		return hasMapTask() ? map : reduce;
	}

	private boolean hasTask() {
		return hasMapTask() || hasReduceTask();
	}

	public String getJobName() {
		return getTask().getJobName();
	}

}

