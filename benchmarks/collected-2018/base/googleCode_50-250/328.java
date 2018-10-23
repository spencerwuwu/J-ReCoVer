// https://searchcode.com/api/result/59098915/

package com.googlecode.pngtastic;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.pngtastic.core.PngImage;
import com.googlecode.pngtastic.core.PngOptimizer;

/**
 * Optimizes PNG images to reduce filesize
 *
 * @see <a href="http://www.w3.org/TR/PNG">PNG spec</a>
 * @see <a href="http://optipng.sourceforge.net/pngtech/">PNG related articles</a>
 * @see <a href="http://www.schaik.com/pngsuite/">PNG reference images</a>
 *
 * @author rayvanderborght
 */
public class PngtasticOptimizer {
	/** */
	private static final String HELP = "java -jar pngtastic-x.x.jar com.googlecode.pngtastic.PngtasticOptimizer [options] file1 [file2 ..]\n"
			+ "Options:\n"
			+ "  --toDir            the directory where optimized files go (will be created if it doesn't exist)\n"
			+ "  --fileSuffix       string appended to the optimized files (file.png can become file.png.optimized.png)\n"
			+ "  --removeGamma      remove gamma correction info if found\n"
			+ "  --compressionLevel the compression level; 0-9 allowed (default is to try them all by brute force)\n"
			+ "  --compressor       path to an alternate compressor (e.g. zopfli)\n"
			+ "  --logLevel         the level of logging output (none, debug, info, or error)\n";

	/** */
	public PngtasticOptimizer(String toDir, String[] fileNames, String fileSuffix, Boolean removeGamma,
			Integer compressionLevel, String compressor, String logLevel) {

		long start = System.currentTimeMillis();

		PngOptimizer optimizer = new PngOptimizer(logLevel);
		optimizer.setCompressor(compressor);

		for (String file : fileNames) {
			try {
				String outputPath = toDir + "/" + file;
				makeDirs(outputPath.substring(0, outputPath.lastIndexOf('/')));

				PngImage image = new PngImage(file);
				optimizer.optimize(image, outputPath + fileSuffix, removeGamma, compressionLevel);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println(String.format("Processed %d files in %d milliseconds, saving %d bytes", optimizer.getStats().size(), System.currentTimeMillis() - start, optimizer.getTotalSavings()));
	}

	/* */
	private String makeDirs(String path) throws IOException {
		File out = new File(path);
		if (!out.exists()) {
			if (!out.mkdirs()) {
				throw new IOException("Couldn't create path: " + path);
			}
		}
		return out.getCanonicalPath();
	}

	/** */
	public static void main(String[] args) {
		Map<String, String> options = new HashMap<String, String>();
		int last = 0;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("--")) {
				int next = i + 1;
				if (next < args.length) {
					options.put(arg, args[next]);
					last = next + 1;
				} else {
					options.put(arg, null);
					last = next;
				}
			}
		}
		String[] files = Arrays.copyOfRange(args, last, args.length);

		if (files.length == 0) {
			System.out.println("No files to process");
			System.out.println(HELP);
			return;
		}

		String toDir = (options.get("--toDir") == null) ? "." : options.get("--toDir");
		String fileSuffix = (options.get("--fileSuffix") == null) ? "" : options.get("--fileSuffix");
		Boolean removeGamma = Boolean.valueOf(options.get("--removeGamma"));
		Integer compressionLevel = safeInteger(options.get("--compressionLevel"));
		String logLevel = options.get("--logLevel");
		String compressor = options.get("--compressor");

		new PngtasticOptimizer(toDir, files, fileSuffix, removeGamma, compressionLevel, compressor, logLevel);
	}

	/* */
	private static Integer safeInteger(String input) {
		try {
			return Integer.valueOf(input);
		} catch (Exception e) {
			return null;
		}
	}
}

