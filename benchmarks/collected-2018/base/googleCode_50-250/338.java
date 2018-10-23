// https://searchcode.com/api/result/2918279/

/**
 * Copyright [2010] [Stefan Lenselink, Ali Mesbah] Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
package com.crawljax.plugins.sfgexporter.transformer.dot;

/**
 * This class denotes all the configuration options the DOT command supports. Only set methods are
 * present to set the configuration options. the buildCommandline call creats the command and
 * parameters used.
 * 
 * @author Stefan Lenselink <S.R.Lenselink@student.tudelft.nl>
 * @version $Id: DOTConfig.java 6576 2010-01-12 15:38:57Z stefan $
 */
public class DOTConfig {

	/**
	 * This enum enumerates the different output formats dot supports. Default png is selected.
	 */
	public enum OutputFormat {
		canon, cmap, cmapx, cmapx_np, dia, dot, fig, gtk, hpgl, imap, imap_np, ismap, mif, mp,
		pcl, pdf, pic, plain, png, ps, ps2, svg, svgz, vml, vmlz, vtx, xdot, xlib
	}

	/**
	 * This enum enumerates the different LayoutEngines fot supports. Default dot is selected.
	 */
	public enum LayoutEngine {
		circo, dot, fdp, neato, nop, nop1, nop2, twopi
	}

	// -Tv - Set output format to 'v'
	// -Kv - Set layout engine to 'v' (overrides default based on command name)
	// -lv - Use external library 'v'
	// -q[l] - Set level of message suppression (=1)
	// -s[v] - Scale input by 'v' (=72)
	// -y - Invert y coordinate in output
	// -n[v] - No layout mode 'v' (=1)
	// -x - Reduce graph
	// -Lg - Don't use grid
	// -LO - Use old attractive force
	// -Ln<i> - Set number of iterations to i
	// -LU<i> - Set unscaled factor to i
	// -LC<v> - Set overlap expansion factor to v
	// -LT[*]<v> - Set temperature (temperature factor) to v

	private OutputFormat outputFormat = DOTConfig.OutputFormat.png;
	private LayoutEngine layoutEngine = DOTConfig.LayoutEngine.dot;
	private String externalLibrary;
	private int levelOfMessageSuppression = -1;
	private int scaleInput = -1;
	private boolean invertYcoordinate = false;
	private String noLayoutMode;
	private boolean reduceGraph = false;
	private boolean useGrid = true;
	private boolean useOldAttriactiveForce = false;
	private int iterations = -1;
	private int unscaledFactor = -1;
	private int overlapExpansionFactor = -1;
	private int temperatureFactor = -1;
	private String dotExecutable = "dot";

	/**
	 * Build the commandline to execute.
	 * 
	 * @param dotFile
	 *            the dot file used as input
	 * @return the commandline to execute
	 */
	public String buildCommandline(String dotFile) {
		String line = this.dotExecutable + " -O";
		if (this.outputFormat != null) {
			line += " -T" + outputFormat;
		}
		if (this.layoutEngine != null) {
			line += " -K" + layoutEngine;
		}
		if (this.externalLibrary != null) {
			line += " -l" + externalLibrary;
		}
		if (this.levelOfMessageSuppression != -1) {
			line += " -q" + levelOfMessageSuppression;
		}
		if (this.scaleInput != -1) {
			line += " -s" + scaleInput;
		}
		if (this.invertYcoordinate) {
			line += " -y";
		}
		if (this.noLayoutMode != null) {
			line += " -n" + noLayoutMode;
		}
		if (this.reduceGraph) {
			line += " -x";
		}
		if (!this.useGrid) {
			line += " -Lg";
		}
		if (this.useOldAttriactiveForce) {
			line += " -LO";
		}
		if (this.iterations != -1) {
			line += " -Ln" + iterations;
		}
		if (this.unscaledFactor != -1) {
			line += " -LU" + unscaledFactor;
		}
		if (this.overlapExpansionFactor != -1) {
			line += " -LC" + overlapExpansionFactor;
		}
		if (this.temperatureFactor != -1) {
			line += " -LT" + temperatureFactor;
		}
		return line + " " + dotFile;
	}

	/**
	 * @param outputFormat
	 *            the outputFormat to set
	 */
	public final void setOutputFormat(OutputFormat outputFormat) {
		this.outputFormat = outputFormat;
	}

	/**
	 * @param layoutEngine
	 *            the layoutEngine to set
	 */
	public final void setLayoutEngine(LayoutEngine layoutEngine) {
		this.layoutEngine = layoutEngine;
	}

	/**
	 * @param externalLibrary
	 *            the externalLibrary to set
	 */
	public final void setExternalLibrary(String externalLibrary) {
		this.externalLibrary = externalLibrary;
	}

	/**
	 * @param levelOfMessageSuppression
	 *            the levelOfMessageSuppression to set
	 */
	public final void setLevelOfMessageSuppression(int levelOfMessageSuppression) {
		this.levelOfMessageSuppression = levelOfMessageSuppression;
	}

	/**
	 * @param scaleInput
	 *            the scaleInput to set
	 */
	public final void setScaleInput(int scaleInput) {
		this.scaleInput = scaleInput;
	}

	/**
	 * @param invertYcoordinate
	 *            the invertYcoordinate to set
	 */
	public final void setInvertYcoordinate(boolean invertYcoordinate) {
		this.invertYcoordinate = invertYcoordinate;
	}

	/**
	 * @param noLayoutMode
	 *            the noLayoutMode to set
	 */
	public final void setNoLayoutMode(String noLayoutMode) {
		this.noLayoutMode = noLayoutMode;
	}

	/**
	 * @param reduceGraph
	 *            the reduceGraph to set
	 */
	public final void setReduceGraph(boolean reduceGraph) {
		this.reduceGraph = reduceGraph;
	}

	/**
	 * @param useGrid
	 *            the useGrid to set
	 */
	public final void setUseGrid(boolean useGrid) {
		this.useGrid = useGrid;
	}

	/**
	 * @param useOldAttriactiveForce
	 *            the useOldAttriactiveForce to set
	 */
	public final void setUseOldAttriactiveForce(boolean useOldAttriactiveForce) {
		this.useOldAttriactiveForce = useOldAttriactiveForce;
	}

	/**
	 * @param iterations
	 *            the iterations to set
	 */
	public final void setIterations(int iterations) {
		this.iterations = iterations;
	}

	/**
	 * @param unscaledFactor
	 *            the unscaledFactor to set
	 */
	public final void setUnscaledFactor(int unscaledFactor) {
		this.unscaledFactor = unscaledFactor;
	}

	/**
	 * @param overlapExpansionFactor
	 *            the overlapExpansionFactor to set
	 */
	public final void setOverlapExpansionFactor(int overlapExpansionFactor) {
		this.overlapExpansionFactor = overlapExpansionFactor;
	}

	/**
	 * @param temperatureFactor
	 *            the temperatureFactor to set
	 */
	public final void setTemperatureFactor(int temperatureFactor) {
		this.temperatureFactor = temperatureFactor;
	}

	/**
	 * @param dotExecutable
	 *            the dotExecutable to set
	 */
	public final void setDotExecutable(String dotExecutable) {
		this.dotExecutable = dotExecutable;
	}
}
