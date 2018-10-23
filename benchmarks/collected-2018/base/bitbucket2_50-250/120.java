// https://searchcode.com/api/result/61669174/

package org.discoproject.worker;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.Channels;
import java.util.Arrays;
import java.util.List;

import org.discoproject.DiscoMapFunction;
import org.discoproject.DiscoUtils;

/**
 * @author Luke Hoersten <lhoersten@allstontrading.com>
 * 
 */
public class DiscoWorkerMain {

	public static void main(final String[] args) throws IOException {
		final DiscoWorker discoWorker = new DiscoWorker(Channels.newChannel(System.in), Channels.newChannel(System.err));

		System.setOut(new PrintStream(new DiscoMsgOutputStream(discoWorker)));
		System.setErr(new PrintStream(new DiscoErrOutputStream(discoWorker)));

		final String functionName = args[0];
		final String reduceFunctionName = args[1];
		final String[] slicedArgs = Arrays.copyOfRange(args, 2, args.length);

		try {
			discoWorker.requestTask();

			if (discoWorker.hasMapTask()) {
				final DiscoMapFunction mapFunction = DiscoWorkerMain.<DiscoMapFunction> instantiateFunction(functionName);
				final List<File> outputFiles = mapFunction.map(discoWorker.getMapInput(), discoWorker.getJobName(),
				        discoWorker.getWorkingDir(), slicedArgs);
				discoWorker.reportOutputs(outputFiles);
			}

			// TODO: reduce phase is not supported because the dir:// URL scheme is unsupported in the input fetcher.
			// if (discoWorker.hasReduceTask()) {
			// final DiscoReduceFunction reduceFunction = DiscoWorkerMain.<DiscoReduceFunction> instantiateFunction(reduceFunctionName);
			// final List<File> outputFile = reduceFunction.reduce(discoWorker.getReduceInputs(), discoWorker.getJobName(),
			// discoWorker.getWorkingDir(), slicedArgs);
			// discoWorker.reportOutputs(outputFile);
			// }

			discoWorker.doneReportingOutput();
		}
		catch (final Exception e) {
			discoWorker.reportFatalError(DiscoUtils.stacktraceToString(e));
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T instantiateFunction(final String clazz) throws IllegalArgumentException, SecurityException,
	        InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
		return (T) Class.forName(clazz).getConstructor().newInstance();
	}

	private static abstract class DiscoOutputStream extends OutputStream {
		protected final StringBuilder stringBuilder;
		protected final DiscoWorker discoWorker;

		public DiscoOutputStream(final DiscoWorker discoWorker) {
			super();
			this.discoWorker = discoWorker;
			this.stringBuilder = new StringBuilder();
		}

		@Override
		public void write(final byte[] b, final int off, final int len) {
			stringBuilder.append(new String(b, off, len));
		}

		@Override
		public void write(final int b) {
			stringBuilder.append((byte) b);
		}

		protected void clear() {
			stringBuilder.setLength(0); // Clear
			stringBuilder.append("[pid:");
			stringBuilder.append(DiscoUtils.getPid());
			stringBuilder.append("] ");
		}
	}

	private static class DiscoMsgOutputStream extends DiscoOutputStream {
		public DiscoMsgOutputStream(final DiscoWorker discoWorker) {
			super(discoWorker);
		}

		@Override
		public void flush() throws IOException {
			discoWorker.reportMessage(stringBuilder.toString());
			clear();
		}
	}

	private static class DiscoErrOutputStream extends DiscoOutputStream {
		public DiscoErrOutputStream(final DiscoWorker discoWorker) {
			super(discoWorker);
		}

		@Override
		public void flush() throws IOException {
			discoWorker.reportError(stringBuilder.toString());
			clear();
		}
	}

}

