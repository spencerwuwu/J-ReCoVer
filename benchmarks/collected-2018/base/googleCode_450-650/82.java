// https://searchcode.com/api/result/5310290/

/*******************************************************************************
 * Copyright 2011 AKRA GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package de.akra.idocit.ui.composites;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.pocui.core.actions.EmptyActionConfiguration;
import org.pocui.core.composites.CompositeInitializationException;
import org.pocui.core.composites.ISelectionListener;
import org.pocui.core.composites.PocUIComposite;
import org.pocui.core.resources.EmptyResourceConfiguration;
import org.pocui.swt.composites.AbsComposite;

import de.akra.idocit.common.services.ThematicGridService;
import de.akra.idocit.common.structure.ThematicGrid;
import de.akra.idocit.common.structure.ThematicRole;
import de.akra.idocit.common.utils.DescribedItemNameComparator;
import de.akra.idocit.core.services.impl.ServiceManager;
import de.akra.idocit.ui.Activator;
import de.akra.idocit.ui.constants.DialogConstants;
import de.akra.idocit.ui.utils.DescribedItemUtils;
import de.akra.idocit.ui.utils.MessageBoxUtils;

/**
 * The composite to manage {@link ThematicGrid}s in the preference page.
 * 
 * @author Dirk Meier-Eickhoff
 * @since 0.0.1
 * @version 0.0.1
 */
public class ManageThematicGridsComposite
		extends
		AbsComposite<EmptyActionConfiguration, EmptyResourceConfiguration, ManageThematicGridsCompositeSelection>
{
	/**
	 * Logger.
	 */
	private static Logger log = Logger.getLogger(ManageThematicGridsComposite.class
			.getName());

	// Widgets
	private EditThematicGridListComposite editThematicGridListComposite;

	private EditThematicGridComposite editThematicGridComposite;

	private Button btnImportGrids;

	private Button btnExportGridsXml;

	private Button btnExportGridsHtml;

	private Label errorLabel;

	// Listeners
	private ISelectionListener<EditThematicGridCompositeSelection> editThematicGridSelectionListener;

	private ISelectionListener<EditThematicGridListCompositeSelection> editThematicGridListSelectionListener;

	private SelectionListener btnImportListener;

	private SelectionListener btnExportListenerXml;

	private SelectionListener btnExportListenerHtml;

	// globals
	private Color RED;

	/**
	 * Constructor.
	 * 
	 * @param parent
	 *            The parent Composite.
	 * @param actionConf
	 *            The action configuration {@link EmptyActionConfiguration}.
	 */
	public ManageThematicGridsComposite(Composite parent,
			EmptyActionConfiguration actionConf)
	{
		super(parent, SWT.NONE, actionConf, EmptyResourceConfiguration.getInstance());
	}

	@Override
	protected void addAllListener()
	{
		editThematicGridComposite.addSelectionListener(editThematicGridSelectionListener);
		editThematicGridListComposite
				.addSelectionListener(editThematicGridListSelectionListener);

		btnImportGrids.addSelectionListener(btnImportListener);
		btnExportGridsXml.addSelectionListener(btnExportListenerXml);
		btnExportGridsHtml.addSelectionListener(btnExportListenerHtml);
	}

	@Override
	protected void doCleanUp()
	{
		RED.dispose();
	}

	@Override
	protected void doSetSelection(ManageThematicGridsCompositeSelection oldSelection,
			ManageThematicGridsCompositeSelection newSelection, Object sourceControl)
	{
		updateRoleList(newSelection);

		errorLabel.setVisible(newSelection.isNameExists());

		/**********************************************************************/

		EditThematicGridListCompositeSelection editItemListCompositeSelection = new EditThematicGridListCompositeSelection();
		editItemListCompositeSelection.setMinNumberOfItems(1);

		editItemListCompositeSelection.setItems(newSelection.getThematicGrids());

		List<ThematicGrid> activeItems = new ArrayList<ThematicGrid>();
		activeItems.add(newSelection.getActiveThematicGrid());
		editItemListCompositeSelection.setActiveItems(activeItems);

		editThematicGridListComposite.setSelection(editItemListCompositeSelection);

		/**********************************************************************/

		EditThematicGridCompositeSelection editThematicGridCompositeSelection = new EditThematicGridCompositeSelection();
		editThematicGridCompositeSelection = editThematicGridCompositeSelection
				.setRoles(newSelection.getRoles());
		editThematicGridCompositeSelection = editThematicGridCompositeSelection
				.setActiveThematicGrid(newSelection.getActiveThematicGrid());
		editThematicGridCompositeSelection = editThematicGridCompositeSelection
				.setActiveRole(newSelection.getActiveRole());

		editThematicGridComposite.setSelection(editThematicGridCompositeSelection);
	}

	/**
	 * If the global {@link ThematicRole} list is changed, load them again.
	 * 
	 * @param selection
	 *            The selection object to update.
	 */
	private void updateRoleList(ManageThematicGridsCompositeSelection selection)
	{
		if (selection.getLastSaveTimeThematicRoles() < ServiceManager.getInstance()
				.getPersistenceService().getLastSaveTimeOfThematicRoles())
		{
			selection.setLastSaveTimeThematicRoles(ServiceManager.getInstance()
					.getPersistenceService().getLastSaveTimeOfThematicRoles());
			selection.setRoles(ServiceManager.getInstance().getPersistenceService()
					.loadThematicRoles());
		}
	}

	@Override
	protected void initGUI(Composite arg0) throws CompositeInitializationException
	{
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(this);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(this);

		RED = this.getDisplay().getSystemColor(SWT.COLOR_RED);

		// TODO reduce the used space in UI if the label is not displayed (if
		// possible)
		errorLabel = new Label(this, SWT.NONE);
		errorLabel.setVisible(false);
		errorLabel.setText(DialogConstants.ERR_MSG_NAME_CONFLICT);
		errorLabel.setForeground(RED);
		errorLabel.setAlignment(SWT.CENTER);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(errorLabel);

		Group grpEditList = new Group(this, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(grpEditList);

		GridDataFactory.fillDefaults().grab(false, true).applyTo(grpEditList);
		grpEditList.setText("Defined Thematic Grids:");

		editThematicGridListComposite = new EditThematicGridListComposite(grpEditList);

		Group grpEditDescribedItem = new Group(this, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5)
				.applyTo(grpEditDescribedItem);
		GridDataFactory.fillDefaults().span(1, 2).grab(true, true)
				.applyTo(grpEditDescribedItem);
		grpEditDescribedItem.setText("Edit selected Thematic Grid:");

		editThematicGridComposite = new EditThematicGridComposite(grpEditDescribedItem);

		Composite btnComposite = new Composite(this, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).equalWidth(true)
				.applyTo(btnComposite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(btnComposite);

		btnImportGrids = new Button(btnComposite, SWT.PUSH);
		btnImportGrids.setText("Import Thematic Grids");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(btnImportGrids);

		btnExportGridsXml = new Button(btnComposite, SWT.PUSH);
		btnExportGridsXml.setText("Export Thematic Grids as XML");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(btnExportGridsXml);

		new Label(btnComposite, SWT.NONE);
		btnExportGridsHtml = new Button(btnComposite, SWT.PUSH);
		btnExportGridsHtml.setText("Export Thematic Grids as HTML");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(btnExportGridsHtml);
	}

	@Override
	protected void initListener() throws CompositeInitializationException
	{
		editThematicGridSelectionListener = new ISelectionListener<EditThematicGridCompositeSelection>() {
			@Override
			public void selectionChanged(EditThematicGridCompositeSelection selection,
					PocUIComposite<EditThematicGridCompositeSelection> composite,
					Object sourceControl)
			{
				ManageThematicGridsCompositeSelection mySelection = getSelection();

				List<ThematicGrid> grids = mySelection.getThematicGrids();
				boolean foundSameName = DescribedItemUtils
						.containsName(selection.getActiveThematicGrid(),
								mySelection.getThematicGrids())
						&& mySelection.getIndexOfActiveThematicGrid() != DescribedItemUtils
								.indexOfName(selection.getActiveThematicGrid().getName(),
										grids);

				mySelection.setNameExists(foundSameName);

				ThematicGrid activeGrid = selection.getActiveThematicGrid();
				ThematicGrid copiedGrid = activeGrid.clone();

				if (activeGrid != null)
				{
					int gridIndex = mySelection.getIndexOfActiveThematicGrid();
					grids.remove(gridIndex);
					grids.add(copiedGrid);
					Collections.sort(grids, DescribedItemNameComparator.getInstance());

					mySelection.setIndexOfActiveThematicGrid(grids.indexOf(copiedGrid));

					// keep the copy of the selected grid
					mySelection.setActiveThematicGrid(copiedGrid);
				}
				
				mySelection.setActiveRole(selection.getActiveRole());

				setSelection(mySelection);
			}
		};

		editThematicGridListSelectionListener = new ISelectionListener<EditThematicGridListCompositeSelection>() {
			@Override
			public void selectionChanged(
					EditThematicGridListCompositeSelection selection,
					PocUIComposite<EditThematicGridListCompositeSelection> composite,
					Object sourceControls)
			{
				ManageThematicGridsCompositeSelection mySelection = getSelection();
				mySelection.setNameExists(false);

				// Create a copied list to prevent side effects.
				List<ThematicGrid> grids = new ArrayList<ThematicGrid>(
						selection.getItems());
				mySelection.setThematicGrids(grids);

				List<ThematicGrid> selectedItems = selection.getActiveItems();
				if ((selectedItems != null) && !selectedItems.isEmpty())
				{
					// copy grid to notice changes
					ThematicGrid clonedActiveGrid = selectedItems.get(0).clone();
					mySelection.setActiveThematicGrid(clonedActiveGrid);

					// remember the index to replace the changed item in the
					// global list. the list must be ordered.
					mySelection.setIndexOfActiveThematicGrid(mySelection
							.getThematicGrids().indexOf(selectedItems.get(0)));
				}
				// Changes due to Issue #10
				else
				{
					mySelection.setActiveThematicGrid(null);
					mySelection.setIndexOfActiveThematicGrid(-1);
				}
				// End changes due to Issue #10

				setSelection(mySelection);
			}
		};

		btnImportListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
				fileDialog.setText("Import Thematic Grids");
				String[] filterExt = { "*.xml" };
				fileDialog.setFilterExtensions(filterExt);
				String selectedFileName = fileDialog.open();

				if (selectedFileName != null)
				{
					try
					{
						List<ThematicGrid> importedGrids = ServiceManager.getInstance()
								.getPersistenceService()
								.importThematicGrids(new File(selectedFileName));

						ManageThematicGridsCompositeSelection selection = getSelection();
						List<ThematicGrid> grids = selection.getThematicGrids();

						// Changes due to Issue #32
						if (grids == null)
						{
							grids = importedGrids;
						}
						else
						{
							grids = mergeGrids(grids, importedGrids);
						}
						selection.setThematicGrids(grids);
						// End changes due to Issue #32
						// Changes due to Issue #10
						selection.setActiveThematicGrid(grids.size() > 0 ? grids.get(0)
								: null);
						// End changes due to Issue #10

						List<ThematicRole> roles = ThematicGridService
								.collectThematicRoles(grids, selection.getRoles());
						selection.setRoles(roles);

						setSelection(selection);
					}
					catch (IOException ioEx)
					{
						log.log(Level.SEVERE,
								"An error occured while importing thematic grids from the selected resource.",
								ioEx);
						Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0,
								ioEx.getLocalizedMessage(), null);
						ErrorDialog
								.openError(
										Display.getCurrent().getActiveShell(),
										DialogConstants.DIALOG_TITLE,
										"An error occured while importing thematic grids from the selected resource.",
										status);
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{}
		};

		btnExportListenerXml = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				FileDialog fileDialog = new FileDialog(getShell(), SWT.SAVE);
				fileDialog.setText("Export Thematic Grids as XML-file");
				String[] filterExt = { "*.xml" };
				fileDialog.setFilterExtensions(filterExt);
				String selectedFileName = fileDialog.open();
				boolean stored = false;

				while ((selectedFileName != null) && !stored)
				{
					try
					{
						boolean exists = new File(selectedFileName).exists();
						boolean overwrite = exists
								&& MessageBoxUtils
										.openQuestionDialogBox(
												getShell(),
												"The file "
														+ selectedFileName
														+ " already exists. Do you want to overwrite it?");

						if (overwrite || !exists)
						{
							ManageThematicGridsCompositeSelection selection = getSelection();
							ServiceManager
									.getInstance()
									.getPersistenceService()
									.exportThematicGridsAsXml(new File(selectedFileName),
											selection.getThematicGrids());

							stored = true;
						}
						else
						{
							selectedFileName = fileDialog.open();
						}
					}
					catch (IOException ioEx)
					{
						log.log(Level.SEVERE,
								"An error occured while importing thematic grids from the selected resource.",
								ioEx);
						Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0,
								ioEx.getLocalizedMessage(), null);
						ErrorDialog
								.openError(
										Display.getCurrent().getActiveShell(),
										DialogConstants.DIALOG_TITLE,
										"An error occured while importing thematic grids from the selected resource.",
										status);
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{}
		};

		btnExportListenerHtml = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				FileDialog fileDialog = new FileDialog(getShell(), SWT.SAVE);
				fileDialog.setText("Export Thematic Grids as HTML-file");
				String[] filterExt = { "*.html" };
				fileDialog.setFilterExtensions(filterExt);
				String selectedFileName = fileDialog.open();
				boolean stored = false;

				while ((selectedFileName != null) && !stored)
				{
					try
					{
						boolean exists = new File(selectedFileName).exists();
						boolean overwrite = exists
								&& MessageBoxUtils
										.openQuestionDialogBox(
												getShell(),
												"The file "
														+ selectedFileName
														+ " already exists. Do you want to overwrite it?");

						if (overwrite || !exists)
						{
							ManageThematicGridsCompositeSelection selection = getSelection();
							ServiceManager
									.getInstance()
									.getPersistenceService()
									.exportThematicGridsAsHtml(
											new File(selectedFileName),
											selection.getThematicGrids());

							stored = true;
						}
						else
						{
							selectedFileName = fileDialog.open();
						}
					}
					catch (IOException ioEx)
					{
						log.log(Level.SEVERE,
								"An error occured while importing thematic grids from the selected resource.",
								ioEx);
						Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0,
								ioEx.getLocalizedMessage(), null);
						ErrorDialog
								.openError(
										Display.getCurrent().getActiveShell(),
										DialogConstants.DIALOG_TITLE,
										"An error occured while importing thematic grids from the selected resource.",
										status);
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{}
		};
	}

	/**
	 * New grids should be inserted into the list of the old grids. If the old grid list
	 * contains already grids with names that have also new grids, then the user is asked
	 * if he wants to overwrite the existing grids. The user is asked once for all equal
	 * grids.<br/>
	 * If old grids should not be overwritten, only new grids with other names are
	 * inserted. New grids with same names are skipped.
	 * 
	 * @param oldGrids
	 * @param newGrids
	 */
	private List<ThematicGrid> mergeGrids(List<ThematicGrid> oldGrids,
			List<ThematicGrid> newGrids)
	{
		TreeMap<String, ThematicGrid> oldGridMap = new TreeMap<String, ThematicGrid>();
		for (ThematicGrid oGrid : oldGrids)
		{
			oldGridMap.put(oGrid.getName(), oGrid);
		}

		boolean overwriteGrids = false;
		boolean asked = false;

		for (ThematicGrid newGrid : newGrids)
		{
			if (oldGridMap.containsKey(newGrid.getName()))
			{
				if (!asked)
				{
					asked = true;
					overwriteGrids = MessageBoxUtils
							.openQuestionDialogBox(
									getShell(),
									"There exists thematic grids with same names. New imported grids with same names will overwrite the existing grids.");
				}
				if (overwriteGrids)
				{
					oldGridMap.put(newGrid.getName(), newGrid);
				}
			}
			else
			{
				oldGridMap.put(newGrid.getName(), newGrid);
			}
		}

		return new ArrayList<ThematicGrid>(oldGridMap.values());
	}

	@Override
	protected void removeAllListener()
	{
		editThematicGridComposite
				.removeSelectionListener(editThematicGridSelectionListener);
		editThematicGridListComposite
				.removeSelectionListener(editThematicGridListSelectionListener);

		btnImportGrids.removeSelectionListener(btnImportListener);
		btnExportGridsXml.removeSelectionListener(btnExportListenerXml);
		btnExportGridsHtml.removeSelectionListener(btnExportListenerHtml);
	}
}

