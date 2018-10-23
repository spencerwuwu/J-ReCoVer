// https://searchcode.com/api/result/5310282/

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

import de.akra.idocit.common.structure.Addressee;
import de.akra.idocit.common.utils.DescribedItemNameComparator;
import de.akra.idocit.ui.constants.DialogConstants;
import de.akra.idocit.ui.utils.DescribedItemUtils;

/**
 * {@link Composite} to manage {@link Addressee}s.
 * 
 * @author Dirk Meier-Eickhoff
 * @since 0.0.1
 * @version 0.0.1
 */
public class ManageAddresseeComposite
		extends
		AbsComposite<EmptyActionConfiguration, EmptyResourceConfiguration, ManageAddresseeCompositeSelection>
{
	// Widgets
	private EditAddresseeListComposite editAddresseeListComposite;

	private EditAddresseeComposite editAddresseeComposite;

	private Label errorLabel;

	// Listeners
	private ISelectionListener<EditAddresseeCompositeSelection> editAddresseeCompositeSelectionListener;

	private ISelectionListener<EditAddresseeListCompositeSelection> editAddresseeListCompositeSelectionListener;

	// globals
	private Color RED;

	/**
	 * Constructor.
	 * 
	 * @param parent
	 *            The parent Composite.
	 */
	public ManageAddresseeComposite(Composite parent)
	{
		super(parent, SWT.NONE, EmptyActionConfiguration.getInstance(),
				EmptyResourceConfiguration.getInstance());
	}

	@Override
	protected void addAllListener()
	{
		editAddresseeComposite
				.addSelectionListener(editAddresseeCompositeSelectionListener);
		editAddresseeListComposite
				.addSelectionListener(editAddresseeListCompositeSelectionListener);
	}

	@Override
	protected void doCleanUp()
	{
		RED.dispose();
	}

	@Override
	protected void doSetSelection(ManageAddresseeCompositeSelection oldSelection,
			ManageAddresseeCompositeSelection newSelection, Object sourceControl)
	{
		errorLabel.setVisible(newSelection.isNameExists());

		// Update the EditAddresseeListComposite.
		EditAddresseeListCompositeSelection editItemListSelection = new EditAddresseeListCompositeSelection();
		editItemListSelection.setAddressees(newSelection.getAddressees());
		editItemListSelection.setMinNumberOfItems(1);

		Addressee activeAddressee = newSelection.getActiveAddressee();
		List<Addressee> activeAddressees = new ArrayList<Addressee>();

		if (activeAddressee != null)
		{
			activeAddressees.add(activeAddressee);
		}

		editItemListSelection.setActiveAddressees(activeAddressees);

		editAddresseeListComposite.setSelection(editItemListSelection);

		// Update the EditDescribedItemComposite.
		EditAddresseeCompositeSelection editAddresseeCompositeSelection = new EditAddresseeCompositeSelection();
		editAddresseeCompositeSelection.setAddressee(activeAddressee);

		if (newSelection.isNameExists())
		{
			editAddresseeCompositeSelection.setModifiedAddressee(newSelection
					.getModifiedAddressee());
		}
		else
		{
			if (activeAddressee != null)
			{
				Addressee modifiedItem = DescribedItemUtils.copy(activeAddressee);
				editAddresseeCompositeSelection.setModifiedAddressee(modifiedItem);
			}
			else
			{
				editAddresseeCompositeSelection.setModifiedAddressee(null);
			}
		}

		editAddresseeCompositeSelection.setLastCurserPosition(newSelection
				.getLastCurserPosition());
		editAddresseeComposite.setSelection(editAddresseeCompositeSelection);
	}

	@Override
	protected void initGUI(Composite parent) throws CompositeInitializationException
	{
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).margins(5, 5)
				.applyTo(this);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(this);

		RED = this.getDisplay().getSystemColor(SWT.COLOR_RED);

		// TODO reduce the used space in UI if the label is not displayed (if possible)
		errorLabel = new Label(this, SWT.NONE);
		errorLabel.setVisible(false);
		errorLabel.setText(DialogConstants.ERR_MSG_NAME_CONFLICT);
		errorLabel.setForeground(RED);
		errorLabel.setAlignment(SWT.CENTER);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(errorLabel);

		Group grpEditList = new Group(this, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(grpEditList);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(grpEditList);
		grpEditList.setText("Defined Addressees:");

		editAddresseeListComposite = new EditAddresseeListComposite(grpEditList);

		Group grpEditDescribedItem = new Group(this, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5)
				.applyTo(grpEditDescribedItem);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grpEditDescribedItem);
		grpEditDescribedItem.setText("Edit selected Addressee:");

		editAddresseeComposite = new EditAddresseeComposite(grpEditDescribedItem);
	}

	@Override
	protected void initListener() throws CompositeInitializationException
	{
		editAddresseeCompositeSelectionListener = new ISelectionListener<EditAddresseeCompositeSelection>() {
			@Override
			public void selectionChanged(EditAddresseeCompositeSelection selection,
					PocUIComposite<EditAddresseeCompositeSelection> changedComposite,
					Object sourceControl)
			{
				// If this composite fires a change event, the active Addressee
				// has been modified. We have to update this composite.

				ManageAddresseeCompositeSelection editSelection = getSelection();

				boolean foundSameName = (!selection.getAddressee().getName()
						.equals(selection.getModifiedAddressee().getName()) && DescribedItemUtils
						.containsName(selection.getModifiedAddressee(),
								editSelection.getAddressees()));

				editSelection.setNameExists(foundSameName);
				editSelection.setLastCurserPosition(selection.getLastCurserPosition());

				if (foundSameName)
				{
					editSelection.setModifiedAddressee(selection.getModifiedAddressee());
				}
				else
				{
					// Get the addressee to change.
					Addressee activeAddressee = editSelection.getActiveAddressee();
					List<Addressee> addressees = editSelection.getAddressees();

					// Remove the old addressee.
					int addresseeIndex = addressees.indexOf(activeAddressee);
					addressees.remove(addresseeIndex);

					// Add the changed Addressee at the same position as the old one.
					Addressee changedAddressee = selection.getModifiedAddressee();
					addressees.add(addresseeIndex, changedAddressee);

					Collections.sort(editSelection.getAddressees(),
							DescribedItemNameComparator.getInstance());

					editSelection.setActiveAddressee(changedAddressee);

					// Update the selection.
					editSelection.setAddressees(addressees);
				}

				setSelection(editSelection);
			}
		};

		editAddresseeListCompositeSelectionListener = new ISelectionListener<EditAddresseeListCompositeSelection>() {
			@Override
			public void selectionChanged(EditAddresseeListCompositeSelection selection,
					PocUIComposite<EditAddresseeListCompositeSelection> changedComposite,
					Object sourceControl)
			{
				// If this composite fires a change event, a new active addressee has been
				// chosen.
				ManageAddresseeCompositeSelection editSelection = getSelection();
				editSelection.setNameExists(false);
				editSelection.setModifiedAddressee(null);

				if (selection.getActiveAddressees() != null
						&& !selection.getActiveAddressees().isEmpty())
				{
					editSelection.setActiveAddressee(selection.getActiveAddressees().get(
							0));
				}
				else
				{
					editSelection.setActiveAddressee(DescribedItemUtils
							.createNewAddressee());
				}

				setSelection(editSelection);
			}
		};
	}

	@Override
	protected void removeAllListener()
	{
		editAddresseeComposite
				.removeSelectionListener(editAddresseeCompositeSelectionListener);
		editAddresseeListComposite
				.removeSelectionListener(editAddresseeListCompositeSelectionListener);
	}
}

