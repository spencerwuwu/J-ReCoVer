// https://searchcode.com/api/result/5310294/

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.pocui.core.actions.EmptyActionConfiguration;
import org.pocui.core.composites.CompositeInitializationException;
import org.pocui.core.composites.ISelectionListener;
import org.pocui.core.composites.PocUIComposite;
import org.pocui.core.resources.EmptyResourceConfiguration;
import org.pocui.swt.composites.AbsComposite;

import de.akra.idocit.common.structure.ThematicRole;
import de.akra.idocit.common.utils.DescribedItemNameComparator;
import de.akra.idocit.core.services.impl.ServiceManager;
import de.akra.idocit.ui.constants.DialogConstants;
import de.akra.idocit.ui.utils.DescribedItemUtils;

/**
 * {@link Composite} to manage {@link ThematicRole}s.
 * 
 * @author Dirk Meier-Eickhoff
 * @since 0.0.1
 * @version 0.0.1
 */
public class ManageThematicRoleComposite
		extends
		AbsComposite<EmptyActionConfiguration, EmptyResourceConfiguration, ManageThematicRoleCompositeSelection>
{
	// Widgets
	private EditThematicRoleListComposite editThematicRoleListComposite;

	private EditThematicRoleComposite editThematicRoleComposite;

	private Label errorLabel;

	// Listeners
	private ISelectionListener<EditThematicRoleCompositeSelection> editThematicRoleCompositeSelectionListener;

	private ISelectionListener<EditThematicRoleListCompositeSelection> editThematicRoleListCompositeSelectionListener;

	// globals
	private Color RED;

	/**
	 * Constructor.
	 * 
	 * @param parent
	 *            The parent Composite.
	 */
	public ManageThematicRoleComposite(Composite parent)
	{
		super(parent, SWT.NONE, EmptyActionConfiguration.getInstance(),
				EmptyResourceConfiguration.getInstance());
	}

	@Override
	protected void addAllListener()
	{
		editThematicRoleComposite
				.addSelectionListener(editThematicRoleCompositeSelectionListener);
		editThematicRoleListComposite
				.addSelectionListener(editThematicRoleListCompositeSelectionListener);
	}

	@Override
	protected void doCleanUp()
	{
		RED.dispose();
	}

	@Override
	protected void doSetSelection(ManageThematicRoleCompositeSelection oldSelection,
			ManageThematicRoleCompositeSelection newSelection, Object sourceControl)
	{
		updateRoleList(newSelection);

		errorLabel.setVisible(newSelection.isNameExists());

		// Update the EditItemListComposite.
		EditThematicRoleListCompositeSelection editItemListSelection = new EditThematicRoleListCompositeSelection();
		editItemListSelection.setItems(newSelection.getThematicRoles());
		editItemListSelection.setMinNumberOfItems(1);

		ThematicRole activeRole = newSelection.getActiveThematicRole();
		List<ThematicRole> activeRoles = new ArrayList<ThematicRole>();

		if (activeRole != null)
		{
			activeRoles.add(activeRole);
		}

		editItemListSelection.setActiveItems(activeRoles);

		editThematicRoleListComposite.setSelection(editItemListSelection);

		// Update the EditDescribedItemComposite.
		EditThematicRoleCompositeSelection editThematicRoleCompositeSelection = new EditThematicRoleCompositeSelection();
		editThematicRoleCompositeSelection.setItem(activeRole);

		if (newSelection.isNameExists())
		{
			editThematicRoleCompositeSelection.setModifiedItem(newSelection
					.getModifiedThematicRole());
		}
		else
		{
			if (activeRole != null)
			{
				ThematicRole modifiedItem = DescribedItemUtils.copy(activeRole);
				editThematicRoleCompositeSelection.setModifiedItem(modifiedItem);
			}
			else
			{
				editThematicRoleCompositeSelection.setModifiedItem(null);
			}
		}
		editThematicRoleCompositeSelection.setLastCurserPosition(newSelection
				.getLastCurserPosition());
		editThematicRoleComposite.setSelection(editThematicRoleCompositeSelection);
	}

	/**
	 * If the global {@link ThematicRole} list is changed, load them again.
	 * 
	 * @param newSelection
	 *            The new selection object to update.
	 */
	private void updateRoleList(ManageThematicRoleCompositeSelection newSelection)
	{
		if (newSelection.getLastSaveTimeThematicRoles() < ServiceManager.getInstance()
				.getPersistenceService().getLastSaveTimeOfThematicRoles())
		{
			newSelection.setLastSaveTimeThematicRoles(ServiceManager.getInstance()
					.getPersistenceService().getLastSaveTimeOfThematicRoles());
			newSelection.setThematicRoles(ServiceManager.getInstance()
					.getPersistenceService().loadThematicRoles());
		}
	}

	@Override
	protected void initGUI(Composite parent) throws CompositeInitializationException
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
		grpEditList.setText("Defined Thematic Roles:");

		editThematicRoleListComposite = new EditThematicRoleListComposite(grpEditList);

		Group grpEditDescribedItem = new Group(this, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5)
				.applyTo(grpEditDescribedItem);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grpEditDescribedItem);
		grpEditDescribedItem.setText("Edit selected Thematic Role:");

		editThematicRoleComposite = new EditThematicRoleComposite(grpEditDescribedItem);
	}

	@Override
	protected void initListener() throws CompositeInitializationException
	{
		editThematicRoleCompositeSelectionListener = new ISelectionListener<EditThematicRoleCompositeSelection>() {
			@Override
			public void selectionChanged(EditThematicRoleCompositeSelection selection,
					PocUIComposite<EditThematicRoleCompositeSelection> changedComposite,
					Object sourceControl)
			{
				// If this composite fires a change event, the active
				// ThematicRole
				// has been modified. We have to update this composite.

				ManageThematicRoleCompositeSelection editSelection = getSelection();

				boolean foundSameName = (!selection.getItem().getName()
						.equals(selection.getModifiedItem().getName()) && DescribedItemUtils
						.containsName(selection.getModifiedItem(),
								editSelection.getThematicRoles()));

				editSelection.setNameExists(foundSameName);
				editSelection.setLastCurserPosition(selection.getLastCurserPosition());

				if (foundSameName)
				{
					editSelection.setModifiedThematicRole(selection.getModifiedItem());
				}
				else
				{
					// Get the role to change.
					ThematicRole activeThematicRole = editSelection
							.getActiveThematicRole();
					List<ThematicRole> thematicRoles = editSelection.getThematicRoles();

					// Remove the old ThematicRole.
					int roleIndex = thematicRoles.indexOf(activeThematicRole);
					thematicRoles.remove(roleIndex);

					// Add the changed ThematicRole at the same position as the
					// old one.
					ThematicRole changedRole = selection.getModifiedItem();
					thematicRoles.add(roleIndex, changedRole);

					Collections.sort(editSelection.getThematicRoles(),
							DescribedItemNameComparator.getInstance());
					editSelection.setActiveThematicRole(changedRole);

					// Update the selection.
					editSelection.setThematicRoles(thematicRoles);
				}

				setSelection(editSelection);
				fireChangeEvent(null);
			}
		};

		editThematicRoleListCompositeSelectionListener = new ISelectionListener<EditThematicRoleListCompositeSelection>() {
			@Override
			public void selectionChanged(
					EditThematicRoleListCompositeSelection selection,
					PocUIComposite<EditThematicRoleListCompositeSelection> changedComposite,
					Object sourceControl)
			{
				// If this composite fires a change event, a new active role has
				// been
				// chosen.
				ManageThematicRoleCompositeSelection mySelection = getSelection();
				mySelection.setNameExists(false);
				mySelection.setModifiedThematicRole(null);

				if (selection.getActiveItems() != null
						&& !selection.getActiveItems().isEmpty())
				{
					mySelection.setActiveThematicRole(selection.getActiveItems().get(0));
				}
				else
				{
					mySelection.setActiveThematicRole(DescribedItemUtils
							.createNewThematicRole());
				}

				setSelection(mySelection);
				fireChangeEvent(null);
			}
		};
	}

	@Override
	protected void removeAllListener()
	{
		editThematicRoleComposite
				.removeSelectionListener(editThematicRoleCompositeSelectionListener);
		editThematicRoleListComposite
				.removeSelectionListener(editThematicRoleListCompositeSelectionListener);
	}
}

