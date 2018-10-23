// https://searchcode.com/api/result/8285700/

package name.nanek.gdwprototype.client.view;

import name.nanek.gdwprototype.client.controller.support.SoundPlayer;
import name.nanek.gdwprototype.shared.model.DefaultMarkers;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Dialog explaining how to play.
 * 
 * @author Lance Nanek
 *
 */
public class TerrainDialog {
	//TODO these help screens are getting hideously long and we were told the best help is just in time help in class
	//maybe detect clicks as well as drags, clicks would show short piece specific information and help
	//then all the user would have to be told at the start is to click on a piece to learn about it

	public final DialogBox dialogBox = new DialogBox();

	public final Button closeButton = new Button("Close");

	public TerrainDialog(SoundPlayer player) {
		dialogBox.setText("How to Play : Terrain");
		//dialogBox.setAnimationEnabled(true);

		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		FlexTable table = new FlexTable();
		
		//XXX CSS seems to be overriding this. Make CSS more specific.
		table.setCellPadding(10);
		
		int row = 0;
		addHeading("Carrot", table, row++);
		table.setWidget(row, 0, new Image("images/" + DefaultMarkers.CARROT.source, 0, 0, 
				DefaultMarkers.MARKER_WIDTH_PX, DefaultMarkers.MARKER_HEIGHT_PX));
		table.setWidget(row++, 1, new HTML(
				"Eat carrots! Land on a carrot to get a new random bunny at your home.<br />" + 
				"There must be an open space next to your home, however, or it is wasted."));

		addHeading("Hill", table, row++);
		table.setWidget(row, 0, new Image("images/" + DefaultMarkers.HILL.source, 0, 0, 
				DefaultMarkers.MARKER_WIDTH_PX, DefaultMarkers.MARKER_HEIGHT_PX));
		table.setWidget(row++, 1, new HTML(
				"Land on hills to see farther."));
		
		addHeading("Tree", table, row++);
		table.setWidget(row, 0, new Image("images/" + DefaultMarkers.TREE.source, 0, 0, 
				DefaultMarkers.MARKER_WIDTH_PX, DefaultMarkers.MARKER_HEIGHT_PX));
		table.setWidget(row++, 1, new HTML(
				"Avoid trees. They reduce how far you can see."));
		
		addHeading("Grass", table, row++);
		table.setWidget(row, 0, new Image("images/" + DefaultMarkers.GRASS.source, 0, 0, 
				DefaultMarkers.MARKER_WIDTH_PX, DefaultMarkers.MARKER_HEIGHT_PX));
		table.setWidget(row++, 1, new HTML(
				"Guard grass. Carrots grow on it every once in a while."));
		
		dialogVPanel.add(table);
		
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});
		dialogVPanel.add(closeButton);
		dialogBox.setWidget(dialogVPanel);
		
		player.addMenuClick(closeButton);
	}

	private void addHeading(String text, FlexTable table, int row) {
		Label label = new Label(text);
		label.addStyleName("heavy");
		table.setWidget(row, 0, label);
		table.getFlexCellFormatter().setColSpan(row, 0, 3);
	}

	public void show() {
		dialogBox.center();
		closeButton.setFocus(true);
	}

}

