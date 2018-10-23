// https://searchcode.com/api/result/61138481/

package utils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to convert a map from an image to a matrix of chars.
 * 
 * @author Jonathan Lovelace
 * 
 */
public class MapReaderDriver implements Serializable {
	/**
	 * Version UID for serialization.
	 */
	private static final long serialVersionUID = -1956548986752307179L;
	/**
	 * A logger to replace printStackTrace() calls.
	 */
	private static final Logger LOGGER = Logger.getLogger(MapReaderDriver.class
			.getName());
	/**
	 * The image we're converting.
	 */
	private final Picture pict;
	/**
	 * Moved out of run() to appease static analysis plugins.
	 */
	private final Map<Pixel, String> pixMap;
	/**
	 * To avoid instantiating in a loop.
	 */
	private static final Character A_CHARACTER = new Character('a');

	/**
	 * @param args Command-line arguments
	 */
	public static void main(final String[] args) {
		try {
			new MapReaderDriver(args[0]).run(args[1]);
		} catch (FileNotFoundException e) {
			LOGGER.log(Level.SEVERE, "File not found", e);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "I/O error", e);
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param filename
	 *            The filename of the image
	 * @throws FileNotFoundException
	 *             if the file doesn't exist
	 */
	public MapReaderDriver(final String filename) throws FileNotFoundException {
		pict = new Picture(filename);
		pixMap = new HashMap<Pixel, String>();
	}

	/**
	 * 
	 * @param filename
	 *            The filename to write the matrix to
	 * @throws IOException
	 *             on input error
	 */
	public void run(final String filename) throws IOException {
		pixMap.clear();
		final BufferedWriter writer = new BufferedWriter(new FileWriter(
				filename));
		final Incrementor currentTerrain = new Incrementor(); // NOPMD
		for (List<Pixel> list : reduce()) {
			for (Pixel pix : list) {
				if (!pixMap.containsKey(pix)) {
					pixMap.put(pix,
							(pix.equals(Pixel.BLACK_PIXEL) ? "Separator"
									: "Terrain" + currentTerrain.getNextValue()
											+ " "));
				}
				writer.append(pixMap.get(pix));
			}
			writer.newLine();
		}
		writer.close();
	}

	/**
	 * @return a matrix of characters representing the map.
	 */
	public List<List<Character>> createMap() {
		final List<List<Character>> retval = new ArrayList<List<Character>>();
		final Map<Pixel, Character> map = new HashMap<Pixel, Character>();
		int currentTerrain = 0;
		for (List<Pixel> list : reduce()) {
			final List<Character> row = newList();
			for (Pixel pix : list) {
				if (!map.containsKey(pix)) {
					if (pix.equals(Pixel.BLACK_PIXEL)) {
						map.put(pix, A_CHARACTER);
					} else {
						map
								.put(pix, Integer.toString(currentTerrain)
										.charAt(0));
						++currentTerrain;
					}
				}
				if (!map.get(pix).equals(A_CHARACTER)) {
					row.add(map.get(pix));
				}
			}
			if (!row.isEmpty()) {
				retval.add(row);
			}
		}
		return retval;
	}

	/**
	 * @return a new List of Characters
	 */
	private static List<Character> newList() {
		return new ArrayList<Character>();
	}

	/**
	 * @return a reduced (all consecutive duplicate rows removed, and all
	 *         consecutive duplicate pixels in each row removed) matrix of
	 *         pixels.
	 */
	private List<List<Pixel>> reduce() {
		final List<List<Pixel>> pixels = new ArrayList<List<Pixel>>(); // NOPMD
		for (int i = 0; i < pict.getWidth(); i++) {
			final List<Pixel> list = newPixelList(); // NOPMD
			for (int j = 0; j < pict.getHeight(); j++) {
				list.add(pict.getPixel(i, j));
			}
			pixels.add(list);
		}
		for (int i = 0; i < pixels.size(); i++) {
			while (i + 1 < pixels.size()
					&& pixels.get(i).equals(pixels.get(i + 1))) {
				pixels.remove(i + 1);
			}
		}
		for (List<Pixel> list : pixels) {
			removeDuplicatePixels(list);
		}
		return pixels;
	}

	/**
	 * Remove duplicate pixels from a list of pixels.
	 * 
	 * @param list
	 *            The list to reduce
	 */
	private static void removeDuplicatePixels(final List<Pixel> list) {
		for (int i = 0; i < list.size(); i++) {
			while (i + 1 < list.size() && list.get(i).equals(list.get(i + 1))) {
				list.remove(i + 1);
			}
		}
	}

	/**
	 * @return a new List of Pixels
	 */
	private static List<Pixel> newPixelList() {
		return new ArrayList<Pixel>();
	}
}

