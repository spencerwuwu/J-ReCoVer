// https://searchcode.com/api/result/64602282/

package org.vaadin.peter.multibutton;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.vaadin.peter.multibutton.client.ui.MultiButtonState;

import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;

/**
 * MultiButton is a component that allows grouping multiple buttons as sub
 * buttons. This component can be used for example to reduce the amount of
 * visible buttons in the UI by grouping them as sub buttons according to usage.
 * 
 * @author Peter Lehto / Vaadin Ltd
 */

public class MultiButton extends AbstractComponentContainer {
	private static final long serialVersionUID = -4986172666522104060L;

	private static final int INITIAL_POPUP_BUTTON_WIDTH = 30;
	private final Button mainButton;
	private final Button popupButton;

	private final List<Button> buttons;

	/**
	 * Constructs new empty multibutton
	 */
	public MultiButton() {
		setStyleName("v-multibutton");
		mainButton = new Button();
		mainButton.setWidth(100, Unit.PERCENTAGE);
		mainButton.setStyleName("v-multibutton-mainbutton");

		popupButton = new Button();
		popupButton.setStyleName("v-multibutton-popupbutton");

		super.addComponent(mainButton);
		super.addComponent(popupButton);

		buttons = new ArrayList<Button>();

		setPopupButtonPixelWidth(INITIAL_POPUP_BUTTON_WIDTH);
	}

	/**
	 * Constructs new MultiButton with given caption
	 * 
	 * @param caption
	 */
	public MultiButton(String caption) {
		this();
		setCaption(caption);
	}

	/**
	 * Constructs new empty multibutton with given caption and click listener
	 * attached to the main button
	 * 
	 * @param caption
	 * @param clickListener
	 */
	public MultiButton(String caption, Button.ClickListener clickListener) {
		this();
		addClickListener(clickListener);
		setCaption(caption);
	}

	/**
	 * Constructs new multibutton with given icon
	 * 
	 * @param icon
	 */
	public MultiButton(Resource icon) {
		this();
		setIcon(icon);
	}

	@Override
	public void beforeClientResponse(boolean initial) {
		super.beforeClientResponse(initial);

		getState().setMainButtonConnectorId(mainButton.getConnectorId());
		getState().setPopupButtonConnectorId(popupButton.getConnectorId());
	}

	/**
	 * Sets the width of the right hand side popup button
	 * 
	 * @param pixels
	 *            width in pixels
	 */
	public void setPopupButtonPixelWidth(int pixels) {
		getState().setPopupButtonPixelWidth(pixels);
		popupButton.setWidth(pixels, Unit.PIXELS);
	}

	/**
	 * Sets the caption of the main button
	 * 
	 * @param caption
	 */
	@Override
	public void setCaption(String caption) {
		mainButton.setCaption(caption);
	}

	/**
	 * Sets the icon of the main button
	 * 
	 * @param icon
	 */
	@Override
	public void setIcon(Resource icon) {
		mainButton.setIcon(icon);
	}

	/**
	 * Enables or disables the right hand side popup button
	 * 
	 * @param enabled
	 */
	public void setPopupButtonEnabled(boolean enabled) {
		popupButton.setEnabled(enabled);
	}

	/**
	 * @return true if popup button is enabled, false otherwise
	 */
	public boolean isPopupButtonEnabled() {
		return popupButton.isEnabled();
	}

	/**
	 * Adds given button to this multibutton. Given button must be unattached.
	 * 
	 * @param button
	 * 
	 * @return given button
	 */
	public Button addButton(Button button) {
		if (button.getParent() != null) {
			throw new IllegalArgumentException(
					"Given button already has a parent component");
		}

		super.addComponent(button);
		buttons.add(button);
		button.setWidth(100, Unit.PERCENTAGE);

		return button;
	}

	/**
	 * Adds new sub button with given caption
	 * 
	 * @param caption
	 * 
	 * @return added button instance
	 */
	public Button addButton(String caption) {
		Button button = new Button(caption);
		addButton(button);

		return button;
	}

	/**
	 * Adds new sub button with given caption and icon
	 * 
	 * @param caption
	 * @param icon
	 * 
	 * @return added button instance
	 */
	public Button addButton(String caption, Resource icon) {
		Button button = addButton(caption);
		button.setIcon(icon);

		return button;
	}

	/**
	 * Adds new sub button with given caption and click listener
	 * 
	 * @param caption
	 * @param clickListener
	 * 
	 * @return added button instance
	 */
	public Button addButton(String caption, Button.ClickListener clickListener) {
		Button button = addButton(caption);
		button.addClickListener(clickListener);

		return button;
	}

	/**
	 * Adds new sub button with given caption, icon and click listener
	 * 
	 * @param caption
	 * @param icon
	 * @param clickListener
	 * @return added button instance
	 */
	public Button addButton(String caption, Resource icon,
			Button.ClickListener clickListener) {
		Button button = addButton(caption, icon);
		button.addClickListener(clickListener);

		return button;
	}

	@Override
	public MultiButtonState getState() {
		return (MultiButtonState) super.getState();
	}

	/**
	 * Removes given sub button from this multibutton
	 * 
	 * @param button
	 *            to remove
	 */
	public void removeButton(Button button) {
		if (buttons.contains(button)) {
			super.removeComponent(button);
			buttons.remove(button);
		}
	}

	/**
	 * Removes all the buttons added to this multibutton
	 */
	public void removeAllButtons() {
		for (Button button : buttons) {
			removeComponent(button);
		}

		buttons.clear();
	}

	/**
	 * Removes all buttons added to this multibutton.
	 */
	@Override
	public void removeAllComponents() {
		removeAllButtons();
	}

	/**
	 * Replacing components in multibutton is currently not supported
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void replaceComponent(Component oldComponent, Component newComponent) {
		throw new UnsupportedOperationException("Currently not supported");
	}

	@Override
	public int getComponentCount() {
		return buttons.size() + 2;
	}

	@Override
	public Iterator<Component> iterator() {
		List<Component> children = new LinkedList<Component>();
		children.add(mainButton);
		children.add(popupButton);
		children.addAll(buttons);
		return children.iterator();
	}

	/**
	 * Adds given click listener to the main button
	 * 
	 * @param clickListener
	 */
	public void addClickListener(Button.ClickListener clickListener) {
		mainButton.addClickListener(clickListener);
	}

	/**
	 * Removes given click listener from the main button
	 * 
	 * @param clickListener
	 */
	public void removeClickListener(Button.ClickListener clickListener) {
		mainButton.removeClickListener(clickListener);
	}

	/**
	 * Adds given click listener to the popup button on the right hand side
	 * 
	 * @param clickListener
	 */
	public void addPopupButtonClickListener(Button.ClickListener clickListener) {
		popupButton.addClickListener(clickListener);
	}

	/**
	 * Removes given click listener from the popup button on the right hand side
	 * 
	 * @param clickListener
	 */
	public void removePopupButtonClickListener(
			Button.ClickListener clickListener) {
		popupButton.removeClickListener(clickListener);
	}
}
