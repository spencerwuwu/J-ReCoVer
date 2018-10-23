// https://searchcode.com/api/result/11638251/

package net.hyperadapt.pxweave.integration.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.URI;
import java.util.ArrayList;

import javax.servlet.http.HttpSession;

import net.hyperadapt.pxweave.Environment;
import net.hyperadapt.pxweave.IEnvironment;
import net.hyperadapt.pxweave.XMLWeaverException;
import net.hyperadapt.pxweave.aspects.AspectWeaver;
import net.hyperadapt.pxweave.aspects.PipeStageJoinpoint;
import net.hyperadapt.pxweave.config.ast.WeaverConfiguration;
import net.hyperadapt.pxweave.context.IWeavingContext;
import net.hyperadapt.pxweave.context.ast.StringParameter;
import net.hyperadapt.pxweave.integration.common.IntegrationConstraints;
import net.hyperadapt.pxweave.interpreter.IInterpreterArgument;
import net.hyperadapt.pxweave.interpreter.InterpreterArgument;

/**
 * The class represent the interface between the integration solutions and
 * PX-Weave. After preparation of the transfered request, PX-Weave is invoked.
 * In the process, caching mechanisms reduce the adaptation overhead.
 * 
 * @author Martin Lehmann
 * 
 */
public class PXWeaveHelper {

	private static URI baseURI;
	private static URI contextURI;
	private static URI matrixURI;

	/**
	 * The weaver configuration, the aspect conflict matrix and the context
	 * model are determined.
	 */
	static {
		File base = new File(PropertyHelper.getResourcePath(
				IntegrationConstraints.APPLICATION_CONTEXTPATH,
				IntegrationConstraints.PXWEAVE_DEFAULT_FOLDERNAME));
		baseURI = base.toURI();
		File matrix = new File(PropertyHelper.getResourcePath(
				IntegrationConstraints.APPLICATION_CONTEXTPATH, "")
				+ IntegrationConstraints.PXWEAVE_DEFAULT_PATTERNCONFLICTS);
		matrixURI = matrix.toURI();
		contextURI = URI.create(IntegrationConstraints.PXWEAVE_DEFAULT_CONTEXT);
	}

	/**
	 * For caching purposes, the context is stored in the current session of the
	 * request.
	 * 
	 * @param session
	 *            - current session of the request
	 * @param context
	 *            - context model with the evaluated context parameter
	 */
	private static void saveContext(HttpSession session, IWeavingContext context) {
		session.setAttribute(IntegrationConstraints.SESSION_PXWEAVE_CONTEXT,
				context);
	}

	/**
	 * Initialization of the environment of PX-Weave with the help of the
	 * context model, which is stored in the session of the current request.
	 * 
	 * @param cache
	 *            - to reduce the adaptation overhead
	 * @return IEnvironment - the environment of PX-Weave
	 * @throws XMLWeaverException
	 *             - by an exception during the initialization
	 */
	private static IEnvironment getEnvironment(WeavingCache cache)
			throws XMLWeaverException {

		return Environment
				.create(baseURI,
						URI.create(IntegrationConstraints.PXWEAVE_DEFAULT_WEAVERCONFIG),
						cache.getContext(), matrixURI);
	}

	/**
	 * From the session of the current request the context is loaded and on this
	 * basis the weaving cache is initialized. If there isn't a context object
	 * in the session, the hole environment of PX-Weave will be initialized.
	 * 
	 * @param session
	 *            - current session from the request
	 * @return WeavingCache - cache to reduce the adaptation overhead
	 * @throws XMLWeaverException
	 *             - by an exception during the initialization
	 */
	@SuppressWarnings("unused")
	private static WeavingCache getWeavingCache(HttpSession session)
			throws XMLWeaverException {
		IWeavingContext context = (IWeavingContext) session
				.getAttribute(IntegrationConstraints.SESSION_PXWEAVE_CONTEXT);

		/*
		 * If there isn't a context in the session, the hole environment will be
		 * initialized and the current session id is stored in the context model
		 * for aspect evaluation purposes.
		 */
		if (context == null) {
			context = Environment.createWeavingContext(contextURI, baseURI);
			StringParameter param = new StringParameter();
			param.setName(IntegrationConstraints.SESSION_SESSIONID);
			param.setValue(session.getId());
			context.addParameter(param);
		}

		/* The weaver configuration as well as the weaving cache is initialized. */
		if (context != null) {
			WeaverConfiguration config = Environment
					.getWeaverConfigurationFromURL(
							URI.create(IntegrationConstraints.PXWEAVE_DEFAULT_WEAVERCONFIG),
							baseURI);
			return new WeavingCache(Environment.getAspectsFromConfig(config,
					baseURI), context, session.getId());
		}
		return null;
	}

	/**
	 * This method create a PX-Weave call for the PreProcessing mechanism. The
	 * weaving cache is initialized and on this base its possible that the
	 * process is aborted (because of the usage of already adapted files).
	 * 
	 * @param requestUrl
	 *            - absolute path of the allocated resource
	 * @param absolutContextPath
	 *            - absolute path of the application context
	 * @param site
	 *            - file name of the allocated resource
	 * @param coreId
	 *            - key of the programmatic joinpoint (must be equal to the key
	 *            in the used aspects)
	 * @param session
	 *            - current session of the request
	 * @return String - relative path of the adapted file
	 */
	public static String createCall(File requestUrl, String absolutContextPath,
			String site, String coreId, HttpSession session) {
		PXWeaveOutputFile outputFile = null;
		try {
			WeavingCache cache = getWeavingCache(session);

			/*
			 * Create an output file to generate an unique file reference based
			 * on the evaluation of the used aspects.
			 */
			outputFile = new PXWeaveOutputFile(absolutContextPath, site, cache);

			/*
			 * If the cache doesn't exist or if the context parameters, which
			 * are used during the aspect evaluation, aren't available in the
			 * context model, the adaptation process of PX-Weave will be go one.
			 */
			if (cache == null || !cache.isCaching()
					|| !new File(outputFile.getAbsolutePath()).isFile()) {
				IEnvironment environment = getEnvironment(cache);
				environment.getExecutionState().setCurrentJoinpoint(
						new PipeStageJoinpoint(coreId));

				/*
				 * The output file is commited to adaptation process and is used
				 * for creating an unique file reference after the evaluation
				 * process of PX-Weave.
				 */
				final ArrayList<IInterpreterArgument> interpreterArgs = new ArrayList<IInterpreterArgument>();
				final IInterpreterArgument fileArg = new InterpreterArgument(
						requestUrl, outputFile, "core");
				interpreterArgs.add(fileArg);

				/* The weaver starts the weaving process. */
				AspectWeaver.weave(environment, interpreterArgs);

				/*
				 * The determined context parameter are stored in the session of
				 * the current request.
				 */
				saveContext(session, environment.getExecutionState()
						.getContextModel());
			}
		} catch (final XMLWeaverException e) {
			e.printStackTrace();
		}

		if (outputFile == null) {
			return "";
		}

		return outputFile.getAbsolutePath().replace(absolutContextPath, "");
	}

	/**
	 * This method create a PX-Weave call for the PostProcessing mechanism and
	 * the programmatic advice.
	 * 
	 * @param inputStream
	 *            - stream from the interrupted webframework
	 * @param coreId
	 *            - key of the programmatic joinpoint (must be equal to the key
	 *            in the used aspects)
	 * @param session
	 *            - current session of the request
	 * @return OutputStream - adapted stream from PX-Weave
	 */
	public static OutputStream createCall(InputStream inputStream,
			String coreId, HttpSession session) {
		try {

			/* In order to handle incorrect stream, this method check for bom. */
			// inputStream = PXWeaveHelper.checkForUtf8BOM(inputStream);

			/*
			 * Because of the possibility of same method invocations over and
			 * over again, its necessary to check for already adapted streams.
			 */
			byte[] firstBytes = new byte[10];
			PushbackInputStream reader = new PushbackInputStream(inputStream,
					10);
			reader.read(firstBytes);
			String fileStart = new String(firstBytes);
			reader.unread(firstBytes);

			/*
			 * If the stream isn't already adapted, PX-Weave can processes the
			 * stream.
			 */
			if (fileStart.startsWith(IntegrationConstraints.FILE_PREFIX_XML)
					|| fileStart
							.startsWith(IntegrationConstraints.FILE_PREFIX_DOCTYPE)) {
				WeavingCache cache = getWeavingCache(session);
				IEnvironment environment = getEnvironment(cache);

				environment.getExecutionState().setCurrentJoinpoint(
						new PipeStageJoinpoint(coreId));

				ArrayList<IInterpreterArgument> interpreterArgs = new ArrayList<IInterpreterArgument>();
				IInterpreterArgument fileArg = new InterpreterArgument(reader,
						"core");
				interpreterArgs.add(fileArg);

				/* The weaver starts the weaving process. */
				AspectWeaver.weave(environment, interpreterArgs);

				/*
				 * The determined context parameter are stored in the session of
				 * the current request.
				 */
				saveContext(session, environment.getExecutionState()
						.getContextModel());

				return fileArg.getOutputStream();
			}
		} catch (final XMLWeaverException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Checks and remove empty signs at the beginning of the stream.
	 * 
	 * @param inputStream
	 *            - stream from the interrupted webframework
	 * @return PushbackInputStream - to receive the possibility to read and
	 *         unread an stream
	 * @throws IOException
	 *             - by an exception during stream inspection
	 */
	@SuppressWarnings("unused")
	private static PushbackInputStream checkForUtf8BOM(InputStream inputStream)
			throws IOException {
		PushbackInputStream pushbackInputStream = new PushbackInputStream(
				new BufferedInputStream(inputStream), 3);
		byte[] bom = new byte[3];
		if (pushbackInputStream.read(bom) != -1) {
			if (!(bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF)) {
				pushbackInputStream.unread(bom);
			}
		}
		return pushbackInputStream;
	}
}

