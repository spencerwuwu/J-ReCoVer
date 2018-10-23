// https://searchcode.com/api/result/2601584/

/**
 * 
 */
package lib.algo;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author <a href="www.sureinterview.com">SureInterview</a>
 */
public class FloodfillQ1<T extends Comparable<T>> extends FloodfillAbs<T> {
	/**
	 * http://en.wikipedia.org/wiki/Flood_fill#Alternative_implementations
	 */
	@Override
	protected void foolfill(int x, int y) {
		if (x < 0 || x >= width)
			return;
		if (y < 0 || y >= height)
			return;
		T curr = space[x][y];
		if (curr.compareTo(target) != 0)
			return;

		Queue<Position> queue = new LinkedList<Position>();
		queue.add(new Position(x, y));
		while (!queue.isEmpty()) {
			Position pos = queue.poll();
			if (space[pos.x][pos.y].compareTo(target) != 0) {
				continue;
			}

			int i = pos.x;
			int posy = pos.y;

			do {
				i--;
			} while (i >= 0 && space[i][posy].compareTo(target) == 0);
			i++;

			int j = pos.x;
			do {
				j++;
			} while (j < width && space[j][posy].compareTo(target) == 0);
			j--;

			for (int k = i; k <= j; k++) {
				space[k][posy] = replace;
				if (posy > 0 && space[k][posy - 1].compareTo(target) == 0) {
					// can reduce the queue size by only adding the first
					// element
					queue.offer(new Position(k, posy - 1));
				}
				if (posy + 1 < height
						&& space[k][posy + 1].compareTo(target) == 0) {
					queue.offer(new Position(k, posy + 1));
				}
			}
		}
	}
}

