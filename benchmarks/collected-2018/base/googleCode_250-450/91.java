// https://searchcode.com/api/result/4344551/

package hu.cubussapiens.zestlayouts.simulatedcooling;

import hu.cubussapiens.debugvisualisation.layouts.IContinuableLayoutAlgorithm;

import org.eclipse.zest.layouts.LayoutEntity;
import org.eclipse.zest.layouts.LayoutRelationship;
import org.eclipse.zest.layouts.algorithms.AbstractLayoutAlgorithm;
import org.eclipse.zest.layouts.dataStructures.InternalNode;
import org.eclipse.zest.layouts.dataStructures.InternalRelationship;

/**
 * Layout graph using simulated cooling algorithm
 * 
 */
public class SimulatedCooling extends AbstractLayoutAlgorithm implements
		IContinuableLayoutAlgorithm {

	private double coolingFactor = 0.65;
	private double beginTemp = 1000;
	private double stopDifference = 0.001;
	private ICriteria[] crits;
	private volatile boolean fNeedsRecall = true;
	private volatile boolean fCancel;

	/**
	 * Map getLayoutEntity() for an array of {@link InternalNodes}. This conversion is needed
	 * as the InternalNodes do not contain the layout information
	 * explicitly thats needed in the criteria calculations.
	 * @param nodes {@link LayoutEntity}
	 * @return
	 */
	private LayoutEntity[] convert(InternalNode[] nodes) {
		LayoutEntity[] result = new LayoutEntity[nodes.length];
		for (int i = 0; i < nodes.length; i++)
			result[i] = nodes[i].getLayoutEntity();
		return result;
	}

	/**
	 * This method generates the {@link LayoutRelationship} elements from an
	 * array of {@link InternalRelationship} elements. This conversion is needed
	 * as the InternalRelationships do not contain the layout information
	 * explicitly thats needed in the criteria calculations.
	 * @param relations
	 * @return
	 */
	private LayoutRelationship[] convert(InternalRelationship[] relations) {
		LayoutRelationship[] result = new LayoutRelationship[relations.length];
		for (int i = 0; i < relations.length; i++)
			result[i] = relations[i].getLayoutRelationship();
		return result;
	}

	/**
	 * Creates a simulated cooling algorithm.
	 * @param styles
	 * @param criterias
	 * @param stopDifference 
	 */
	public SimulatedCooling(int styles, ICriteria[] criterias, double stopDifference) {
		super(styles);
		crits = criterias.clone();
		this.stopDifference = stopDifference;
	}

	/**
	 * Get criteria values for current graph configuration
	 * @param entitiesToLayout
	 * @param relationshipsToConsider
	 * @param boundsX
	 * @param boundsY
	 * @param boundsWidth
	 * @param boundsHeight
	 * @return
	 */
	private double getCriteria(InternalNode[] entitiesToLayout,
			InternalRelationship[] relationshipsToConsider, double boundsX,
			double boundsY, double boundsWidth, double boundsHeight) {

		double result = 0;

		for (ICriteria crit : crits) {
			try {
				result += crit.apply(convert(entitiesToLayout),
						convert(relationshipsToConsider), boundsX, boundsY,
						boundsWidth, boundsHeight);
				if (fCancel) break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	private double temp;

	private LayoutEntity movedentity = null;

	private double[] oldpos = null;

	/**
	 * Apply a random move on the configuration, and save it in private fields.
	 * @param entity
	 */
	private void applyRandomMove(LayoutEntity entity) {
		movedentity = entity;
		oldpos = new double[] { entity.getXInLayout(), entity.getYInLayout() };

		double angle = Math.PI * 2 * Math.random();
		double d = Math.random() * temp;

		double newx = oldpos[0] + Math.sin(angle) * d;
		double newy = oldpos[1] + Math.cos(angle) * d;

		entity.setLocationInLayout(newx, newy);
	}

	/**
	 * Undo last move (private fields can store only one move)
	 */
	private void undomove() {
		if (movedentity != null)
			movedentity.setLocationInLayout(oldpos[0], oldpos[1]);

		movedentity = null;
		oldpos = null;
	}

	public synchronized boolean needsRecall() {
		return fNeedsRecall;
	}

	/**
	 * @see org.eclipse.zest.layouts.algorithms.AbstractLayoutAlgorithm#applyLayoutInternal(org.eclipse.zest.layouts.dataStructures.InternalNode[],
	 *      org.eclipse.zest.layouts.dataStructures.InternalRelationship[],
	 *      double, double, double, double)
	 */
	@Override
	protected synchronized void applyLayoutInternal(
			InternalNode[] entitiesToLayout,
			InternalRelationship[] relationshipsToConsider, double boundsX,
			double boundsY, double boundsWidth, double boundsHeight) {
		fireProgressStarted(1);

		fNeedsRecall = true;
		double valuedelta = 0;

		// move outbounded nodes inbound
		for (LayoutEntity e : convert(entitiesToLayout)) {
			if ((e.getXInLayout() < boundsX)
					|| (e.getXInLayout() + e.getWidthInLayout() > boundsWidth)
					|| (e.getYInLayout() < boundsY)
					|| (e.getYInLayout() + e.getHeightInLayout() > boundsHeight)) {
				e.setLocationInLayout((boundsX + boundsWidth) / 2,
						(boundsY + boundsHeight) / 2);
			}
		}

		temp = beginTemp;

		int step = 0;

		while (temp > 1 && !fCancel) {

			// get criteria value for current configuration:
			double value = getCriteria(entitiesToLayout,
					relationshipsToConsider, boundsX, boundsY, boundsWidth,
					boundsHeight);
			// select a node
			LayoutEntity entity = entitiesToLayout[step
					% entitiesToLayout.length].getLayoutEntity();
			// random move for entity
			applyRandomMove(entity);
			// recalculate value for new configuration
			double newvalue = getCriteria(entitiesToLayout,
					relationshipsToConsider, boundsX, boundsY, boundsWidth,
					boundsHeight);
			// the smaller the better.
			if (newvalue <= value) {
				// reduce temperature
				temp = temp * coolingFactor;
				valuedelta += value - newvalue;
			} else {
				// undo the applied move.
				undomove();
			}

			step++;
		}
		if (valuedelta < stopDifference) fNeedsRecall = false;
		fireProgressEnded(1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int getCurrentLayoutStep() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int getTotalNumberOfLayoutSteps() {
		return 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isValidConfiguration(boolean asynchronous,
			boolean continuous) {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void postLayoutAlgorithm(InternalNode[] entitiesToLayout,
			InternalRelationship[] relationshipsToConsider) {
		//

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void preLayoutAlgorithm(InternalNode[] entitiesToLayout,
			InternalRelationship[] relationshipsToConsider, double x, double y,
			double width, double height) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLayoutArea(double x, double y, double width, double height) {
		//

	}

	public void startLayouting() {
		fNeedsRecall = true;
		fCancel = false;
		beginTemp = 1000;
		coolingFactor = 0.65;
	}

	public void cancel() {
		fCancel = true;
		finishLayouting();
	}

	public void finishLayouting() {
		fNeedsRecall = false;
		
	}

}

