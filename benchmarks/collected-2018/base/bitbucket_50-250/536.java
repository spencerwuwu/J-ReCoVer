// https://searchcode.com/api/result/53741187/

/*
 *  Lindenmayer
 *  see AUTHORS for a list of contributors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package lindenmayer.grammar;

import java.awt.Color;

/**
 * Represents a state of the turtle that runs through our
 * canvas and draws lines.
 * @author Christian Lins
 * @author Kai Ritterbusch
 */
public class TurtleState implements Cloneable {

	public static final float DEFAULT_LINE_LENGTH = 55;
	public static final float DEFAULT_SCALE_FACTOR = 0.90f;
	public static final float DEFAULT_STROKE_WIDTH = 6;
	private Color color = Color.BLACK;
	private float x = 0;
	private float y = 0;
	private float angle = 0;
	private float lineLength = DEFAULT_LINE_LENGTH;
	private float strokeWidth = DEFAULT_STROKE_WIDTH;

	public TurtleState() {
	}

	public TurtleState(float x, float y, float angle, Color color,
			float lineLength, float strokeWidth) {
		this.x = x;
		this.y = y;
		this.angle = angle;
		this.color = color;
		this.lineLength = lineLength;
		this.strokeWidth = strokeWidth;
	}

	@Override
	public Object clone() {
		try {
			TurtleState newTurtle = (TurtleState)super.clone();
			return newTurtle;
		} catch(CloneNotSupportedException ex) {
			throw new InternalError();
		}
	}

	public TurtleState scaleDown() {
		// Reduce stroke width and line length
		this.lineLength *= DEFAULT_SCALE_FACTOR;
		this.strokeWidth *= DEFAULT_SCALE_FACTOR;

		// Lighten the color
		this.color = color.brighter();
		return this;
	}

	public float getAngle() {
		return angle;
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public void rotateAngle(float angle) {
		this.angle += angle;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	/**
	 * @return the lineLength
	 */
	public float getLineLength() {
		return lineLength;
	}

	/**
	 * @param lineLength the lineLength to set
	 */
	public void setLineLength(float lineLength) {
		this.lineLength = lineLength;
	}

	/**
	 * @return the strokWidth
	 */
	public float getStrokeWidth() {
		return strokeWidth;
	}

	/**
	 * @param strokWidth the strokWidth to set
	 */
	public void setStrokeWidth(float strokeWidth) {
		this.strokeWidth = strokeWidth;
	}
}

