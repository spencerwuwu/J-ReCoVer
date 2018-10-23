// https://searchcode.com/api/result/2188437/

package uk.ac.lkl.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.lkl.common.util.BoundingBox;
import uk.ac.lkl.common.util.Dimension;
import uk.ac.lkl.common.util.Location;
import uk.ac.lkl.common.util.config.MiGenConfiguration;
import uk.ac.lkl.common.util.expression.Expression;
import uk.ac.lkl.common.util.expression.LocatedExpression;
import uk.ac.lkl.common.util.expression.ModifiableOperation;
import uk.ac.lkl.common.util.expression.ValueExpression;
import uk.ac.lkl.common.util.value.Number;
import uk.ac.lkl.common.util.xml.XMLException;
import uk.ac.lkl.migen.system.expresser.model.AllocatedColor;
import uk.ac.lkl.migen.system.expresser.model.ExpresserModel;
import uk.ac.lkl.migen.system.expresser.model.ExpresserModelImpl;
import uk.ac.lkl.migen.system.expresser.model.shape.block.BlockShape;
import uk.ac.lkl.migen.system.expresser.model.tiednumber.TiedNumberExpression;
import uk.ac.lkl.migen.system.expresser.ui.uievent.UIEventManager;

import  uk.ac.lkl.com.allen_sauer.gwt.dnd.client.DragEndEvent;
import  uk.ac.lkl.com.allen_sauer.gwt.dnd.client.DragHandler;
import  uk.ac.lkl.com.allen_sauer.gwt.dnd.client.DragHandlerAdapter;
import  uk.ac.lkl.com.allen_sauer.gwt.dnd.client.DragStartEvent;
import  uk.ac.lkl.com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TouchSplitLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Node;

public class ExpresserCanvas extends FocusPanel {
    
    protected ExpresserCanvasPanel expresserCanvasPanel;
    private RectangleSelection rectangleSelection;
    private int startX;
    private int startY;
//    private int relativeXTouch;
//    private int relativeYTouch;
    private PopupPanel popUpMenu;
    protected List<ShapeView> shapeViewsBeingDragged = new ArrayList<ShapeView>();
    protected ExpressionPanel expressionPanelBeingDragged;
//    private TotalTilesRulePanel totalTilesRulePanel;
    private boolean modelInvalidWhileDragInProgress;
    private Fader fader;
    private AnimationPanel animationPanel;
    private HashMap<Location, TileViewSnapShot> currentTileSnapshots = 
	new HashMap<Location, TileViewSnapShot>();
    protected FocusWidget restoreFocusToWidget;
    private ModelRulesPanel modelRulesPanel;
    private CanvasToolBar toolBar;
    // canvas height shorter than the available area
    // to leave room for the menu bar, tool bar, and rule area
//    private int canvasHeightMargins = 96;
    private ArrayList<CanvasUpdatedListener> canvasUpdatedListeners = new ArrayList<CanvasUpdatedListener>();
       
    protected HashMap<TiedNumberExpression<Number>, TiedNumber> previouslyMappedTiedNumbers = 
	       new HashMap<TiedNumberExpression<Number>, TiedNumber>();
    protected ScrollPanel canvasScrollPanel;
    // following support negative coordinates
    // TODO: determine if this support for negative coordinates is obsolete
    private int xOffset = 0;
    private int yOffset = 0;
//    private int width;
//    private int height;
    private ThumbnailAnimationButton thumbnailAnimationButton;
    private boolean initialModelLoaded;
    private AnimationButton animationButton;
    // used to avoid redundant computation
    private boolean refreshCommandScheduled = false;
    
    private ScheduledCommand refreshCommand = 
	    new ScheduledCommandWithExceptionHandling("Error occurred in refresh command of ExpresserCanvas.") {
	
	@Override
	public void executeWithExceptionHandling() {
	    refreshCommandScheduled = false;
	    // following depends upon geometry of other panels so defer until they are 'ready'
	    BoundingBox boundingBox = getModel().getBoundingBox();
	    Dimension preferredSize = getPreferredSize();
	    // don't use getWidth and getHeight since need to show everything from 0,0
	    int modelWidth = boundingBox == null ? 1 : boundingBox.getMaxX()+1;
	    int modelHeight = boundingBox == null ? 1 : boundingBox.getMaxY()+1;
	    Integer gridSize = getGridSize();
	    int newWidth = modelWidth*gridSize;
	    int newHeight = modelHeight*gridSize;
	    if (preferredSize != null && expresserCanvasPanel != null) {
		newWidth = Math.max(newWidth, preferredSize.width);
		newHeight = Math.max(newHeight, preferredSize.height);
		if (modelInvalidWhileDragInProgress &&
			(newWidth < getOffsetWidth() || newHeight < getOffsetHeight())) {
		    // drag shouldn't shrink the canvas
		    return;
		}
		expresserCanvasPanel.setPixelSize(newWidth, newHeight);
		// extra room for scroll bars
		// -8 seems to be necessary for Chrome -- otherwise have extra scroll bars
		// IE9 and FireFox didn't need this adjustment but can't hurt
		preferredSize.width += Expresser.VERTICAL_SCROLL_BAR_WIDTH-8;
		preferredSize.height += Expresser.HORIZONTAL_SCROLL_BAR_HEIGHT;
	    }
	    if (preferredSize != null && canvasScrollPanel != null) {
		canvasScrollPanel.setPixelSize(preferredSize.width, preferredSize.height);
	    }
	    ComputersModelPanel computersModelPanel = Expresser.getComputersModelPanel();
	    if (computersModelPanel != null) {
		// computer's panel has the same height
		ExpresserCanvasSlave slaveCanvas = computersModelPanel.getCanvas();
		if (preferredSize != null) {
		    int maximumHeight = computersModelPanel.getMaximumHeight();
		    slaveCanvas.setFixedHeight(Math.min(preferredSize.height, maximumHeight));
		}
		slaveCanvas.updateSize();
	    }
	}
	
    };
       
    public ExpresserCanvas(int gridSize, int width, int height, ExpresserModel model, boolean enableDragAndDrop, boolean enableScrolling) {
	super();
	expresserCanvasPanel = new ExpresserCanvasPanel(gridSize, width, height, this, model, enableDragAndDrop);
	if (enableScrolling) {
	    canvasScrollPanel = new ScrollPanel(expresserCanvasPanel);
	    setWidget(canvasScrollPanel);
	    canvasScrollPanel.setPixelSize(width, height);
	} else {
	    setWidget(expresserCanvasPanel);
	    expresserCanvasPanel.setPixelSize(width, height);
	}
	enableRectangleSelection();
	if (!MiGenConfiguration.isTiedNumbersControlPanelEnabled() || isReadOnly()) {
	    // keep animation panels on activity doc and computer's model
	    animationPanel = new AnimationPanel(this);
	}
    }
    
    public ExpresserCanvas(int gridSize, int width, int height, ExpresserModel model) {
	this(gridSize, width, height, model, true, true);
    }
    
    protected void enableRectangleSelection() {
	MouseDownHandler mouseDownHandler = new MouseDownHandler() {

	    @Override
	    public void onMouseDown(MouseDownEvent event) {
		if (widgetContainingPoint(event.getClientX(), event.getClientY()) != null) {
		    // don't start selection rectangle if mouse is over a shape, expression, or property list
		    return;
		}
		Element element = expresserCanvasPanel.getElement();
		startX = event.getRelativeX(element);
		startY = event.getRelativeY(element);
//		relativeXTouch = startX;
//		relativeYTouch = startY;
//		Utilities.warn("mouse down start rect: " + startX + "," + startY);
		startRectangleSelection();
		if (popUpMenu != null && popUpMenu.isShowing()) {
		    popUpMenu.hide();
		}
	    }
	    
	};
	addMouseDownHandler(mouseDownHandler);
	MouseUpHandler mouseUpHandler = new MouseUpHandler() {

	    @Override
	    public void onMouseUp(MouseUpEvent event) {
		if (!stopRectangleSelection(event.getClientX(), event.getClientY())) {
		    getGridConstrainedDragController().clearSelection();
//		    Utilities.warn("mouse up end rect clear selection: " + event.getClientX() + "," + event.getClientY());
//		} else {
//		    Utilities.warn("mouse up end rect: " + event.getClientX() + "," + event.getClientY());
		}
	    }
	    
	};
	addMouseUpHandler(mouseUpHandler);
	MouseMoveHandler mouseMoveHandler = new MouseMoveHandler() {

	    @Override
	    public void onMouseMove(MouseMoveEvent event) {
		Element element = expresserCanvasPanel.getElement();
		updateRectangleSelection(event.getRelativeX(element), event.getRelativeY(element));
//		Utilities.warn("mouse move: " + event.getClientX() + "," + event.getClientY());
	    }
	    
	};
	addMouseMoveHandler(mouseMoveHandler);
	MouseOutHandler mouseOutHandler = new MouseOutHandler() {

	    @Override
	    public void onMouseOut(MouseOutEvent event) {
		stopRectangleSelection(event.getClientX(), event.getClientY());		
	    }
	    
	};
	addMouseOutHandler(mouseOutHandler);
	TouchStartHandler touchStartHandler = new TouchStartHandler() {

	    @Override
	    public void onTouchStart(TouchStartEvent event) {
		JsArray<Touch> touches = event.getTouches();
		int touchCount = touches.length();
		Utilities.warn("Start touches: " + touchCount);
		if (touchCount >= URLParameters.getMinimumTouchesToSelect()) {
		    Element element = expresserCanvasPanel.getElement();
		    int left = Integer.MAX_VALUE;
		    int top = Integer.MAX_VALUE;
		    int right = Integer.MIN_VALUE;
		    int bottom = Integer.MIN_VALUE;
		    for (int i = 0; i < touchCount; i++) {
			Touch touch = touches.get(i);
			int relativeX = touch.getRelativeX(element);
			int relativeY = touch.getRelativeY(element);
			left = Math.min(left, relativeX);
			top = Math.min(top, relativeY);
			right = Math.max(right, relativeX);
			bottom = Math.max(bottom, relativeY);
		    }
		    int selectionWidth = right-left;
		    int selectionHeight = bottom-top;
		    Utilities.warn(touchCount + " touches"
		                   + "; left=" + left
		                   + "; right=" + right
		                   + "; top=" + top
		                   + "; bottom=" + bottom
		                   + "; width=" + selectionWidth
		                   + "; height=" + selectionHeight);
		    expresserCanvasPanel.clearDragSelection();
		    expresserCanvasPanel.selectWidgetsInRectangle(left, top, selectionWidth, selectionHeight);
		    // at top of region centered horizontally
		    popupMenu(left+selectionWidth/2, top);
		    
		}
//		event.preventDefault(); // http://stackoverflow.com/questions/2987706/touchend-event-doesnt-work-on-android
//		JsArray<Touch> touches = event.getTouches();
//		if (touches.length() == 1) {
//		    Touch touch = touches.get(0);
//		    if (containsShapeAt(touch.getPageX(), touch.getPageY())) {
//			// don't start selection rectangle if touch is over a shape
//			return;
//		    }
//		    Element element = expresserCanvasPanel.getElement();
//		    startX = touch.getRelativeX(element);
//		    startY = touch.getRelativeY(element);
//		    Utilities.warn("touch start rect: " + startX + "," + startY);
//		    startRectangleSelection();
//		    if (popUpMenu != null && popUpMenu.isShowing()) {
//			popUpMenu.hide();
//		    }
//		} else {
//		    Utilities.warn("touch more than one: " + event.getTouches().length());
//		}
	    }
	    
	};
	addTouchStartHandler(touchStartHandler);
//	TouchEndHandler touchEndHandler = new TouchEndHandler() {
//
//	    @Override
//	    public void onTouchEnd(TouchEndEvent event) {
//		Utilities.warn("touch end " + relativeXTouch + "," + relativeYTouch);
//		if (!stopRectangleSelection(relativeXTouch, relativeYTouch)) {
//		    getGridConstrainedDragController().clearSelection();
//		}
//	    }
//	    
//	};
//	addTouchEndHandler(touchEndHandler);
//	TouchMoveHandler touchMoveHandler = new TouchMoveHandler() {
//
//	    @Override
//	    public void onTouchMove(TouchMoveEvent event) {
////		event.preventDefault(); // http://stackoverflow.com/questions/2987706/touchend-event-doesnt-work-on-android
//		JsArray<Touch> touches = event.getTouches();
//		int length = touches.length();
//		if (length == 1) {
//		    // one touch -- like a mouse
//		    Touch touch = touches.get(0);
//		    Element element = expresserCanvasPanel.getElement();
//		    relativeXTouch = touch.getRelativeX(element);
//		    relativeYTouch = touch.getRelativeY(element);
//		    updateRectangleSelection(relativeXTouch, relativeYTouch);
//		    Utilities.warn("single touch move: " + relativeXTouch + "," + relativeYTouch);
//		    // even with this test was not very usable
////		    if (!degenerateRectangleSelection()) {
////			// don't want scrolling while dragging out a rectangle
////			// but if the "rectangle" is nearly a line then allow dragging
////			event.preventDefault(); 
////		    }
////		} else {
////		    stopRectangleSelection();
//		} else {
//		    Utilities.warn("single touch move, more than one: " + length);
//		}
//	    }
//	};
//	addTouchMoveHandler(touchMoveHandler);
    }
    
    // probably the following was a bad idea
    // interferes with gestures to zoom in and out, for example.
//		} else {
//		    int rectangleLeft = Integer.MAX_VALUE;
//		    int rectangleTop = Integer.MIN_VALUE;
//		    int rectangleRight = Integer.MIN_VALUE;
//		    int rectangleBottom = Integer.MAX_VALUE;
//		    for (int i = 0; i < length; i++) {
//			Touch touch = touches.get(i);
//			int x = touch.getClientX()-expresserCanvasPanel.getAbsoluteLeft();
//			int y = touch.getClientY()-expresserCanvasPanel.getAbsoluteTop();
//			rectangleLeft = Math.min(rectangleLeft, x);
//			rectangleRight = Math.max(rectangleRight, x);
//			rectangleTop = Math.max(rectangleTop, y);
//			rectangleBottom = Math.min(rectangleBottom, y);
//		    }
//		    updateRectangleSelection(rectangleLeft, rectangleTop, rectangleRight-rectangleLeft, rectangleTop-rectangleBottom);    


//    protected boolean degenerateRectangleSelection() {
//	if (rectangleSelection == null || !rectangleSelection.isAttached()) {
//	    return true;
//	}
//	int width = rectangleSelection.getOffsetWidth();
//	int height = rectangleSelection.getOffsetHeight();
//	int gridSize = expresserCanvasPanel.getGridSize();
//	if (width < gridSize || height < gridSize) {
//	    return true;
//	}
//	double ratio = Math.abs((double) width / (double) height);
//	return ratio < .5 || ratio > 2.0;
//    }

    public PickupDragControllerEnhanced getGridConstrainedDragController() {
	return expresserCanvasPanel.getGridConstrainedDragController();
    }
    
    public PickupDragControllerEnhanced getUnconstrainedDragController() {
	return expresserCanvasPanel.getUnconstrainedDragController();
    }

    protected boolean stopRectangleSelection(int x, int y) {
	if (rectangleSelectionActive()) {
	    rectangleSelection.removeFromParent();
	    if ((popUpMenu == null || !popUpMenu.isShowing()) &&
		rectangleSelection.okToPopupMenu(getGridSize())) {
		popupMenu(x, y);
		rectangleSelection.reset();
	    }
	    return true;
	}
	return false;
    }

    public boolean rectangleSelectionActive() {
	return rectangleSelection != null && rectangleSelection.isAttached();
    }

    public void popupMenu(int x, int y) {
	ArrayList<ShapeView> selectedShapes = expresserCanvasPanel.getSelectedShapes();
	if (!selectedShapes.isEmpty()) {
	    popUpMenu = selectedShapes.get(0).createPopUpMenu(x, y);
	}
    }

    protected void updateRectangleSelection(int eventX, int eventY) {
	if (rectangleSelectionActive()) {
	    int rectangleLeft, rectangleTop, rectangleWidth, rectangleHeight;
	    if (eventX < startX) {
		rectangleWidth = startX-eventX;
		rectangleLeft = eventX;
	    } else {
		rectangleWidth = eventX-startX;
		rectangleLeft = startX;
	    }
	    if (eventY < startY) {
		rectangleHeight = startY-eventY;
		rectangleTop = eventY;
	    } else {
		rectangleHeight = eventY-startY;
		rectangleTop = startY;
	    }
	    updateRectangleSelection(rectangleLeft, rectangleTop, rectangleWidth, rectangleHeight);
	}
    }

    protected void updateRectangleSelection(int left, int top, int selectionWidth, int selectionHeight) {
	rectangleSelection.setLeft(left);
	rectangleSelection.setTop(top);
	rectangleSelection.setPixelSize(selectionWidth, selectionHeight);
	expresserCanvasPanel.setWidgetPosition(rectangleSelection, left, top);
	expresserCanvasPanel.clearDragSelection();
	expresserCanvasPanel.selectWidgetsInRectangle(left, top, selectionWidth, selectionHeight);
    }

    protected void startRectangleSelection() {
//	if (!expresserCanvasPanel.containsShapeAt(startX, startY)) {
//	if (expresserCanvasPanel.widgetContainingPoint(startX, startY) == null) {
	    if (rectangleSelection == null) {
		rectangleSelection = new RectangleSelection();
	    }
	    expresserCanvasPanel.add(rectangleSelection, startX, startY);
//	}
    }

    public void createDropEvents() {
	PickupDragController gridConstrainedDragController = expresserCanvasPanel.getGridConstrainedDragController();
	DragHandler gridConstrainedDragHandler = new ShapeViewsDragHandlerAdapter(this);
	gridConstrainedDragController.addDragHandler(gridConstrainedDragHandler);
	PickupDragController unonstrainedDragController = expresserCanvasPanel.getUnconstrainedDragController();
	DragHandler unconstrainedDragHandler = new DragHandlerAdapter() {
	    
	    @Override
	    public void onDragStart(DragStartEvent event) {
		// remove the shape from the model to update the tile colouring
		Widget widgetBeingDragged = event.getContext().draggable;
		if (widgetBeingDragged instanceof ExpressionPanel) {
		    expressionPanelBeingDragged = (ExpressionPanel) widgetBeingDragged;
		}
	    }
	    
	    @Override
	    public void onDragEnd(DragEndEvent event) {	
		if (expressionPanelBeingDragged != null) {
		    if (!expressionPanelBeingDragged.isConsumed()) {
			// hasn't been added to a rule or property list
			moveIfOnAPropertyList(expressionPanelBeingDragged);
			EventManager eventManager = expresserCanvasPanel.getEventManager();
			boolean droppedOnSomething = expressionPanelBeingDragged.getDroppedOn() != null;
			LocatedExpression<Number> locatedExpression = addExpressionPanelToModel(expressionPanelBeingDragged);
			if (expressionPanelBeingDragged instanceof TiedNumberPanel) {
			    TiedNumber tiedNumber = expressionPanelBeingDragged.getTiedNumber();
			    eventManager.updateTiedNumber(tiedNumber, false);
			    UIEventManager.processEvent(new ExpressionDroppedOnCanvas(expressionPanelBeingDragged.getId(), true));
			} else {
			    UIEventManager.processEvent(new ExpressionDroppedOnCanvas(expressionPanelBeingDragged.getId(), false));
			}
			eventManager.expressionCreatedOrUpdated(expressionPanelBeingDragged, !droppedOnSomething);
			eventManager.associateExpressionWithId(locatedExpression, expressionPanelBeingDragged.getId());
			
		    }
		    expressionPanelBeingDragged = null;
		} else if (event.getSource() instanceof PropertyList) {
		    PropertyList propertyList = (PropertyList) event.getSource();
		    // ensure it is on top by adding it again
		    int left = propertyList.getLeftInsideCanvas(expresserCanvasPanel);	               
		    int top = propertyList.getTopInsideCanvas(expresserCanvasPanel);
		    propertyList.setPreferredLeft(left);
		    propertyList.setPreferredTop(top);
		    expresserCanvasPanel.add(propertyList, left, top);
		} else if (event.getSource() instanceof ScrollPanel) {
		    ScrollPanel scrollPanel = (ScrollPanel) event.getSource();
		    PropertyList propertyList = (PropertyList) scrollPanel.getWidget();
		    // ensure it is on top by adding it again
		    int left = propertyList.getLeftInsideCanvas(expresserCanvasPanel);	               
		    int top = propertyList.getTopInsideCanvas(expresserCanvasPanel);
		    expresserCanvasPanel.add(scrollPanel, left, top);
		}
	    }
	};
	unonstrainedDragController.addDragHandler(unconstrainedDragHandler);
	PickupDragControllerEnhanced expressionDragController = Expresser.instance().getExpressionDragController();
	if (expressionDragController != unonstrainedDragController) {
	    expressionDragController.addDragHandler(unconstrainedDragHandler);
	}
    }

    protected void moveIfOnAPropertyList(ExpressionPanel expressionPanel) {
	expresserCanvasPanel.moveIfOnAPropertyList(expressionPanel);
    }

    public void add(Widget widget, int x, int y) {
	expresserCanvasPanel.add(widget, x, y);	
    }
    
    @Override
    public boolean remove(Widget widget) {
	return expresserCanvasPanel.remove(widget);
    }
    
    public Integer getGridSize() {
	if (expresserCanvasPanel == null) {
	    // can happen during initialisation/instance creation
	    return null;
	}
	return expresserCanvasPanel.getGridSize();
    }
    
    public void setGridSize(int gridSize) {
	expresserCanvasPanel.setGridSize(gridSize);
    }

    protected boolean containsShapeAt(int x, int y) {
	return expresserCanvasPanel.containsShapeAt(x, y);
    }
    
    protected Widget widgetContainingPoint(int x, int y) {
	return expresserCanvasPanel.widgetContainingPoint(x, y);
    }

    public void updateTilesDisplayMode(Map<Location, ArrayList<AllocatedColor>> map) {
	expresserCanvasPanel.updateTilesDisplayMode(map);
    }
    
    public void reportMovedByGlueLocationsToDataStore() {
	expresserCanvasPanel.reportMovedByGlueLocationsToDataStore();
    }
    
    protected void recomputePixelSize() {
	expresserCanvasPanel.recomputePixelSize();
    }
    
//    @Override
//    public void setPixelSize(int width, int height) {
//	if (this.width == width && this.height == height) {
//	    return;
//	}
//	this.width = width;
//	this.height = height;
//	super.setPixelSize(width, height);
////	expresserCanvasPanel.setPixelSize(width, height);
//	canvasScrollPanel.setPixelSize(width, height);
//	// extra room for scroll bars
//	// TODO: determine if height also need this
////	canvasScrollPanel.setPixelSize(width+Expresser.VERTICAL_SCROLL_BAR_WIDTH, height);
//    }

    public ExpresserModel getModel() {
        return expresserCanvasPanel.getModel();
    }
    
    /**
     * @param canvas
     * Updates the tiles with the display mode appropriate for the overlaps and color rules
     */
    public void updateTiles() {
	if (!URLParameters.isApplyColoringRules()) {
	    return;
	}	
	refresh();
	ExpresserModel model = getModel();
	model.updateColorGrid();
	Map<Location, ArrayList<AllocatedColor>> colorGridMap = model.getColorGridMap();
	updateTilesDisplayMode(colorGridMap);
	if (fader != null) {
	    fader.stop();
	}
	if (isFaderEnabled()) {
	    List<TileView> allTileViews = expresserCanvasPanel.getAllTileViews();
	    if (!currentTileSnapshots.isEmpty()) {
		fader = new Fader(this, allTileViews, currentTileSnapshots);
	    }
	    updateSnapshotOfTiles(allTileViews);
	}
    }

    /**
     * @return true if fading between successive frames is desired
     */
    protected boolean isFaderEnabled() {
	return animationIsPlaying() && URLParameters.isAnimationFaded();
    }

    public boolean animationIsPlaying() {
	if (animationPanel != null) {
	    return animationPanel.isPlaying();
	} else if (animationButton != null) {
	    return animationButton.isPlaying();
	} else {
	    return false;
	}
    }
    
    public void createSnapshotOfTiles() {
	if (fader != null) {
	    fader.stop();
	}
	updateSnapshotOfTiles(expresserCanvasPanel.getAllTileViews());
    }

    /**
     * Create snapshots of tileViews and their locations
     * 
     * @param tileViews 
     */
    public void updateSnapshotOfTiles(List<TileView> tileViews) {
	// recompute currentTileViews
	// Fader will remove these snapshots from the canvas
	currentTileSnapshots.clear();
	// capture current tile snapshots to become previous tiles
	int canvasLeft = expresserCanvasPanel.getAbsoluteLeft();
	int canvasTop = expresserCanvasPanel.getAbsoluteTop();
	for (TileView tileView : tileViews) {
	    TileViewSnapShot snapshot = tileView.getSnapShot();
	    int left = tileView.getAbsoluteLeft()-canvasLeft;
	    int top = tileView.getAbsoluteTop()-canvasTop;
	    currentTileSnapshots.put(new Location(left, top), snapshot);
	}
    }
    
    public void stopFading() {
	if (fader != null) {
	    fader.stop();
	}
	// in case any left behind
	expresserCanvasPanel.removeAllSnaphots();
    }

    public List<ShapeView> getShapeViewsBeingDragged() {
        return shapeViewsBeingDragged;
    }
    
    public void addShapeViewBeingDragged(ShapeView shapeView) {
	shapeViewsBeingDragged.add(shapeView);
    }
    
    public void clearShapeViewBeingDragged() {
	// ensure the z-ordering is correct
//	int canvasAbsoluteLeft = getAbsoluteLeft();
//	int canvasAbsoluteTop = getAbsoluteTop();
//	for (ShapeView shapeView : shapeViewsBeingDragged) {
//	    add(shapeView, 
//		shapeView.getAbsoluteLeft()-canvasAbsoluteLeft, 
//		shapeView.getAbsoluteTop()-canvasAbsoluteTop);
//	}
	shapeViewsBeingDragged.clear();
	getGridConstrainedDragController().clearSelection();
    }

    public void setModelRulesPanel(ModelRulesPanel theModelRulesPanel) {
	this.modelRulesPanel = theModelRulesPanel;
    }

    public void provideRuleFeedback() {
	if (modelRulesPanel != null && 
            !isModelInvalidWhileDragInProgress()) {
	    modelRulesPanel.provideFeedback();
	}	    
    }

    public EventManager getEventManager() {
	return expresserCanvasPanel.getEventManager();
    }

    public boolean isModelInvalidWhileDragInProgress() {
        return modelInvalidWhileDragInProgress;
    }

    public AnimationPanel getAnimationPanel() {
        return animationPanel;
    }

    public ExpresserCanvasPanel getExpresserCanvasPanel() {
        return expresserCanvasPanel;
    }

    public Fader getFader() {
        return fader;
    }

    public void setFader(Fader fader) {
        this.fader = fader;
    }

    public void scheduleUpdateTilesDisplayMode() {
	new Timer() {

	    @Override
	    public void run() {
		if (!Expresser.instance().isAnyUserEvents()) {
		    // don't report indicators or provide feedback before the user has done anything
		    // e.g. those triggered by refresh or loading initial model
		    return;
		}
		final ExpresserModel model = getModel();
		if (model.isDirtyModel() && Expresser.instance().getShapeViewsBeingDragged().isEmpty()) {
		    // defer this until any update of the DOM is finished so that dimensions of widgets
		    // are up-to-date
		    Scheduler.get().scheduleDeferred(
			    new ScheduledCommandWithExceptionHandling("Error occurred updating a canvas.") {
			
				@Override
				public void executeWithExceptionHandling() {
				    updateTiles();
				    provideRuleFeedback();
				    canvasUpdated();
				    if (!model.isAnyTiedNumberAnimating()) {
					reportMovedByGlueLocationsToDataStore();
				    }
				    model.setDirtyModel(false);
				    if (restoreFocusToWidget != null) {
					restoreFocusToWidget.setFocus(true);
					// in IE9 the above moves the cursor to the beginning of the number
					// so set it to the end (which is how it behaves in other browsers)
					if (restoreFocusToWidget instanceof TextBox) {
					    TextBox textBox = (TextBox) restoreFocusToWidget;
					    textBox.setCursorPos(textBox.getText().length());
					}	
					restoreFocusToWidget = null;
				    }
				}});
		}
	    }

	}.scheduleRepeating(10);
    }
    
    public void addCanvasUpdatedListener(CanvasUpdatedListener listener) {
	canvasUpdatedListeners.add(listener);
    }
    
    public void removeCanvasUpdatedListener(CanvasUpdatedListener listener) {
	canvasUpdatedListeners.remove(listener);
    }
    
    protected void canvasUpdated() {
	for (CanvasUpdatedListener listener : canvasUpdatedListeners) {
	    listener.canvasUpdated();
	}
	if (MiGenConfiguration.isEnableAutoVelcro()) {
	    getModel().maintainGlue();
	}
    }

    protected void loadModel(String modelXML, boolean addToDataStore, boolean substituteIds, String initialModelXML ) {
	if (modelXML == null) {
	    return;
	}
	try {
	    Node contents = XMLUtilities.parseXML(modelXML);
	    String tag = contents.getNodeName();
	    if (tag.equals("ExpresserModel")) {
		EventManager eventManagerForAddingToDataStore = addToDataStore ? getEventManager() : null;
		// perhaps I should define subclass of com.google.gwt.xml.client.Element -- e.g. xmlElement
		ExpresserModel model = 
			XMLConverter.xmlToExpresserModel((com.google.gwt.xml.client.Element) contents, eventManagerForAddingToDataStore, substituteIds);
		BoundingBox boundingBox = null;
		try {
		    boundingBox = model.getBoundingBox();
		    if (boundingBox != null) {
			Number deltaX = Number.ZERO;
			Number deltaY = Number.ZERO;
			if (boundingBox.getMinX() < 0) {
			    deltaX = new Number(-boundingBox.getMinX());
			}
			if (boundingBox.getMinY() < 0) {
			    deltaY = new Number(-boundingBox.getMinY());
			}
			if (!deltaX.isZero() || !deltaY.isZero()) {
			    for (BlockShape shape : model.getShapes())  {
				shape.moveBy(deltaX, deltaY);
			    }
			}
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		    // ignore it since not important and can happen if data store isn't consistent
		    // due to a shape without a bounding box
		}
		if (URLParameters.isShapesThumbnail() && boundingBox != null) {
		    // only shapes thumb nails don't need to have the model loaded into the canvas
		    // for the thumb nail move the model to the upper left
		    int minX = boundingBox.getMinX();
		    int minY = boundingBox.getMinY();
		    Number deltaX = new Number(-minX);
		    Number deltaY = new Number(-minY);
		    for (BlockShape shape : model.getShapes()) {
			shape.moveBy(deltaX, deltaY, true);
		    }
		    addModel(model, eventManagerForAddingToDataStore);
		}
		if (URLParameters.isThumbnail()) {
		    List<ShapeView> shapeViews;
		    String focusId = URLParameters.getFocusId();
		    if (focusId == null) {
			shapeViews = getShapeViews();
		    } else {
			Widget widgetWithId = getWidgetWithId(focusId);
			if (widgetWithId == null) {
			    Utilities.warn("Unable to find something to focus on. Id=" + focusId);
			    shapeViews = getShapeViews();
			} else if (widgetWithId instanceof ShapeView) {
			    shapeViews = new ArrayList<ShapeView>();
			    ShapeView shapeViewCopy = ((ShapeView) widgetWithId).copy();
			    shapeViews.add(shapeViewCopy);
			    boundingBox = shapeViewCopy.getBlockShape().getBoundingBox();
			} else if (widgetWithId instanceof ExpressionPanel) {
			    replaceCanvasWithExpressionThumbnail((ExpressionPanel) widgetWithId);
			    return;
			} else {
			    Utilities.warn("Currently unable to find focus on non-shape and non-expression element. Id=" + focusId);
			    shapeViews = getShapeViews();
			}
		    }
		    if (shapeViews.isEmpty() && !URLParameters.isRulesThumbnail()) {
			Utilities.warn("Thumbnail requested but there are no shapes.");
		    }
		    if (URLParameters.isRulesThumbnail() && URLParameters.isShapesThumbnail()) {
			replaceCanvasWithShapesAndRulesThumbnail(shapeViews, model, boundingBox);
		    } else if (URLParameters.isRulesThumbnail()) {
			replaceCanvasWithRulesThumbnail(model);
		    } else {
			replaceCanvasWithShapesThumbnail(shapeViews, boundingBox, model);
		    }
		} else {
		    if (initialModelXML == null || !model.hasNoShapesRulesOrExpressions()) {
			addModel(model, eventManagerForAddingToDataStore);
			if (!initialModelLoaded) {
			    Expresser.instance().updateActivityDocumentWidgets();
			}
		    } else {
			initialModelLoaded = true;
			loadModel(initialModelXML, true, true, null);
		    }
		    updateTiles();
		}
	    } else {
		Utilities.severe("Expected model tag to be ExpresserModel. Not " + tag);
	    }
	} catch (XMLException e) {
	    e.printStackTrace();
	    Utilities.logException(e, "Unable to parse the following as an ExpresserModel:\n" + modelXML); 
	}
    }
    
    public List<ShapeView> getShapeViews() {
	return expresserCanvasPanel.getShapeViews();
    }
    
    public void initialize(ExpresserModel model) {
	expresserCanvasPanel.removeAllTileViews(true);
	expresserCanvasPanel.removeAllLocatedExpressions(true);
	expresserCanvasPanel.removePropertyLists();
	if (modelRulesPanel != null) {
	    modelRulesPanel.clearColorSpecificModelRules();
	    modelRulesPanel.updateModelRulesPanel(model);
	}
    }
    
    public void replaceModel(final String timeStamp, boolean substituteIds, Command continuation) {
	Expresser.instance().fetchAndLoadModel(this, timeStamp, substituteIds, continuation, null);
    }
    
    public void addModel(ExpresserModel newModel, EventManager eventManagerForAddingToDataStore) {
	clearPreviouslyMappedTiedNumbers();
	initialize(newModel);
	// TODO: deal with the palette
	List<BlockShape> shapes = newModel.getShapes();
	EventManager eventManager = getEventManager();
	if (eventManager != null) {
	    eventManager.clearIdToExpressionMap();
	}
	ExpresserModel model = getModel();
	addModelShapes(shapes, model, eventManagerForAddingToDataStore);
	List<LocatedExpression<Number>> locatedExpressions = newModel.getLocatedExpressions();
	for (LocatedExpression<Number> locatedExpression : locatedExpressions) {
	    ExpressionPanel expressionPanel = addLocatedExpression(locatedExpression);
	    String id = expressionPanel.getId();
	    if (eventManager != null) {
		eventManager.associateExpressionWithId(locatedExpression, id);
	    }
	    if (eventManagerForAddingToDataStore != null) {
		// imported with ids that aren't in the data store
		// so force the generation of new ones
		if (id != null && !id.equals(URLParameters.getFocusId())) {
		    expressionPanel.setId(null);
		}
		eventManagerForAddingToDataStore.expressionCreatedOrUpdated(expressionPanel, false);
	    }
	    model.addLocatedExpression(locatedExpression);
	}
	model.setTotalAllocationExpression(newModel.getTotalAllocationExpression());
	model.transferColorSpecificRules(newModel);
	if (modelRulesPanel != null) {
	    modelRulesPanel.updateModelRulesPanel(model);
	    if (eventManager != null) {
		modelRulesPanel.sendStateToEventManager(eventManager);
	    }
	}
	model.setAnimationSettings(newModel.getAnimationSettings());
	// initialize above does the following -- not good to do it twice
//	if (modelRulesPanel != null) {
//	    modelRulesPanel.updateModelRulesPanel(newModel);
//	    // TODO: inform eventManager
//	}
	// in case contents are too big and scrolling is needed
	recomputePixelSize(); 
	model.setDirtyModel(true); // to be sure its repainted
	// TODO: color specific rules too
//	setGridSize(getGridSize());
	final String focusId = URLParameters.getFocusId();
	if (focusId != null && !focusId.isEmpty()) {
	    try {
		// highlight for 5 seconds
		Expresser.instance().setHighlighted(focusId, true);
		new Timer() {

		    @Override
		    public void run() {
			Expresser.instance().setHighlighted(focusId, false);
		    }
		    
		}.schedule(5000);
	    } catch (UnsupportedOperationException e) {
		Utilities.warn("No widget with id found to give focus highlighting: " + focusId);
	    }
	}
	if (MiGenConfiguration.isTiedNumbersControlPanelEnabled() && !URLParameters.isThumbnail()) { 
//	    addUnlockedTiedNumbersControlPanel();
	    Expresser.instance().addUnlockedTiedNumbersControlPanel();
	}
    }

//    private void addUnlockedTiedNumbersControlPanel() {
//	expresserCanvasPanel.positionUnlockedTiedNumbersControlPanel();
//    }

    /**
     * @param shapes
     * @param model
     * @param eventManagerForAddingToDataStore
     */
    protected void addModelShapes(List<BlockShape> shapes, ExpresserModel model,  EventManager eventManagerForAddingToDataStore) {
	boolean anyNegativePatterns = false;
	for (BlockShape shape: shapes) {
	    ShapeView shapeView = addShape(shape, false, model, eventManagerForAddingToDataStore);
	    if (!shapeView.isPositive()) {
		anyNegativePatterns = true;
	    }
	}
	if (anyNegativePatterns) {
	    // tiles are positive by default -- this recomputes as needed
	    updateTiles();
	}
	expresserCanvasPanel.ensureGridPanelIsUnderneath();
    }

    protected ShapeView addShape(BlockShape shape, boolean copying, ExpresserModel model, EventManager eventManagerForAddingToDataStore) {
	ShapeView shapeView = addShapeView(shape, copying, eventManagerForAddingToDataStore);
	model.addObject(shape);	
	if (!isComputersModel()) {
	    shapeView.listenToChanges(shape, this);
	}
	if (!URLParameters.isThumbnail()) {
	    PickupDragControllerEnhanced gridConstrainedDragController = getExpresserCanvasPanel().getGridConstrainedDragController();
	    if (gridConstrainedDragController != null) {
		gridConstrainedDragController.makeDraggable(shapeView);
	    }
	}
	return shapeView;
    }
    
    protected ShapeView addShapeView(BlockShape shape, boolean copying, EventManager eventManager) {
	ShapeView shapeView = ShapeView.toShapeView(shape, copying, this, eventManager);
	if (shapeView == null) {
	    return null;
	}
	// following needed to resolve Issue 1856
	// since when deleted it tries to delete the Tile or GroupShape rather than the PatternShape
	shapeView.setBlockShape(shape);
//	int gridSize = getGridSize();
//	BoundingBox boundingBox = shape.getBoundingBox();
//	int x = boundingBox.getMinX()*gridSize+xOffset;
//	int y = boundingBox.getMinY()*gridSize+yOffset;
	int x = shapeView.getLeft()+xOffset;
	int y = shapeView.getTop()+yOffset;
	int deltaX = 0;
	int deltaY = 0;
	if (x < 0) {
	    deltaX = -x;
	    xOffset += deltaX;
	    x = 0;
	}
	if (y < 0) {
	    deltaY = -y;
	    yOffset += deltaY;
	    y = 0;
	}
//	if (shape.treatAsBuildingBlock() || shape.buildingBlockHasNonZeroDistanceToOrigin()) {
//	    Distance2D distanceToOrigin = shape.distanceToOrigin();
//	    x += distanceToOrigin.getHorizontal().intValue()*gridSize;
//	    y += distanceToOrigin.getVertical().intValue()*gridSize;
//	}
	add(shapeView, x, y);
	if (deltaX != 0 || deltaY != 0) {
	    // offsets changed so move all the views
	    expresserCanvasPanel.moveAllBy(deltaX, deltaY);
	}
	shapeView.setLeft(x+deltaX);
	shapeView.setTop(y+deltaY);
	if (!URLParameters.isThumbnail()) {
	    shapeView.setDraggable(getGridConstrainedDragController());
	}
	return shapeView;
    }

    protected ExpressionPanel addLocatedExpression(LocatedExpression<Number> locatedExpression) {
	int x = locatedExpression.getX();
	int y = locatedExpression.getY();
	ExpressionInterface expressionInterface = 
	    toExpressionInterface(locatedExpression.getExpression());
	ExpressionPanel panel = expressionInterface.getPanel(Expresser.instance().getExpressionDragController());
	panel.setId(locatedExpression.getUniqueId());
	add(panel, x, y);
	return panel;
    }

    protected VerticalPanel replaceCanvasWithShapesThumbnail(List<ShapeView> shapeViews, BoundingBox boundingBox, ExpresserModel model) {
	RootPanel rootPanel = RootPanel.get("expresserinterface");
	// add canvas temporally so geometry works correctly
	rootPanel.add(this);
	int thumbnailWidth = URLParameters.getThumbnailWidth();
	int thumbnailHeight = URLParameters.getThumbnailHeight();
	ExpresserModel thumbnailModel = new ExpresserModelImpl();
	AbsolutePanel thumbnailCanvas = new AbsolutePanel();
	// Ensure the document BODY has height in standards mode
	rootPanel.setPixelSize(thumbnailWidth, thumbnailHeight);
	thumbnailCanvas.setPixelSize(thumbnailWidth, thumbnailHeight);
	// add this to a vertical panel so this code is shared with the code 
	// for showing model and rules both
	VerticalPanel verticalPanel = new VerticalPanel();
	if (shapeViews.isEmpty()) { 
	    // not clear what to do if there are no shapes
	    rootPanel.remove(this);
	    rootPanel.add(verticalPanel);
	    return verticalPanel;
	}
	FocusPanel focusPanel = new FocusPanel(thumbnailCanvas);
	verticalPanel.add(focusPanel);
	rootPanel.add(verticalPanel);
	double horizontalScale = ((double) thumbnailWidth) / (boundingBox.getWidth()*getGridSize());
	double verticalScale = ((double) thumbnailHeight) / (boundingBox.getHeight()*getGridSize());
	double scale = Math.min(horizontalScale, verticalScale);
	int newGridSize = (int) (scale*getGridSize());
	for (ShapeView shapeView : shapeViews) {
	    BlockShape blockShape = shapeView.getBlockShape();
	    thumbnailModel.addObject(blockShape);
	    thumbnailCanvas.add(shapeView, blockShape.getX()*newGridSize,  blockShape.getY()*newGridSize);
	    shapeView.setGridSize(newGridSize);
	    shapeView.updateDisplay(this);
	}
	ArrayList<TiedNumberExpression<Number>> containedTiedNumbers = 
		thumbnailModel.getContainedTiedNumbers(true);
	thumbnailAnimationButton = 
		new ThumbnailAnimationButton(containedTiedNumbers, shapeViews, thumbnailModel, this);
	if (!containedTiedNumbers.isEmpty()) {
	    thumbnailCanvas.add(thumbnailAnimationButton);
	    // thumbnailAnimationButton.getParent().getOffsetWidth() returns the full width of the panel
	    // so workaround for now is to use constant size of button
	    int buttonWidth = 44;
	    int buttonHeight = 44; 
	    int left = (thumbnailWidth-buttonWidth)/2;
	    int top = (thumbnailHeight-buttonHeight)/2;
	    thumbnailCanvas.setWidgetPosition(thumbnailAnimationButton, left, top); 
	    ClickHandler clickHandler = new ClickHandler() {

		@Override
		public void onClick(ClickEvent event) {
		    thumbnailAnimationButton.toggle();
		}
		
	    };
	    focusPanel.addClickHandler(clickHandler);
	    focusPanel.setTitle(Expresser.messagesBundle.ThumbnailAnimationTitle());
	    focusPanel.addStyleName("expresser-thumbnail-animation");
	    if (URLParameters.isAnimateThumbnail()) {
		// start off animating
		thumbnailAnimationButton.toggle();
	    } else {
		Scheduler.get().scheduleDeferred(new ScheduledCommandWithExceptionHandling("Error occurred in thumbnail update shapes command of ExpresserCanvas.") {
			
			@Override
			public void executeWithExceptionHandling() {
			    thumbnailAnimationButton.updateAllShapes();
		    }
		});
	    }
	} else {
	    Scheduler.get().scheduleDeferred(new ScheduledCommandWithExceptionHandling("Error occurred in thumbnail update shapes command of ExpresserCanvas.") {
		
		@Override
		public void executeWithExceptionHandling() {
		    thumbnailAnimationButton.updateAllShapes();
		}
	    });
	}
	if (URLParameters.isUnlockedNumbersThumbnail()) {
	    verticalPanel.add(new ComputersModelTiedNumberValues(model));
	}
	rootPanel.remove(this);
	return verticalPanel;
    }
    
    protected void replaceCanvasWithShapesAndRulesThumbnail(List<ShapeView> shapeViews, ExpresserModel model, BoundingBox boundingBox) {
	VerticalPanel verticalPanel = replaceCanvasWithShapesThumbnail(shapeViews, boundingBox, model);
	// following can cause buggy tile views (???s)
//	verticalPanel.setSpacing(6);
	VerticalPanel rulesDisplay = getRulesDisplay(model, false);
	SimplePanel rulesDisplayPanel = new SimplePanel(rulesDisplay);
	verticalPanel.add(rulesDisplayPanel);
	if (thumbnailAnimationButton != null) {
	    thumbnailAnimationButton.setCanvas(this);
	    thumbnailAnimationButton.setRulesModel(model);
	    thumbnailAnimationButton.setRulesDisplay(rulesDisplayPanel);
	}
	int offsetWidth = verticalPanel.getOffsetWidth();
	int offsetHeight = verticalPanel.getOffsetHeight();
	// 5/3 original width looks good with the default 150x90 model thumbnail
	RootPanel.get().setPixelSize(5*offsetWidth/3, offsetHeight*2);
    }
    
    protected void replaceCanvasWithRulesThumbnail(ExpresserModel model) {
	VerticalPanel rulesDisplay = getRulesDisplay(model, false);
	RootPanel.get().add(rulesDisplay);
    }
    
    protected void replaceCanvasWithExpressionThumbnail(ExpressionPanel expressionPanel) {
	Expression<Number> expression = expressionPanel.getExpressionInterface().getExpression();
	RootPanel.get().add(new HTML("<font size='5' style='bold'>" + expression.toHTMLString() + "</font>"));
    }

    public VerticalPanel getRulesDisplay(ExpresserModel model, Boolean showNamesAndValues) {
	expresserCanvasPanel.setModel(model);
	MyModelRulesPanel myModelRulesPanel = new MyModelRulesPanel(this);
	myModelRulesPanel.updateModelRulesPanel(model);
	VerticalPanel rulesDisplay = new VerticalPanel();
	List<RulePanel> rules = myModelRulesPanel.getRules();
	for (RulePanel rulePanel : rules) {
	    Widget modelRuleDisplay = rulePanel.getModelRuleDisplay(model, showNamesAndValues);
	    rulesDisplay.add(modelRuleDisplay);
	}
	return rulesDisplay;
    }

    public FocusWidget getRestoreFocusToWidget() {
        return restoreFocusToWidget;
    }

    public void setRestoreFocusToWidget(FocusWidget restoreFocusToWidget) {
        this.restoreFocusToWidget = restoreFocusToWidget;
    }

    public void addImportModelPanel() {
	int thirdWidth = getOffsetWidth()/3;
	int thirdHeight = getOffsetHeight()/3;
	ImportXMLPanel importXMLPanel = new ImportXMLPanel(this);
	add(importXMLPanel, thirdWidth/2, thirdHeight/2);
	importXMLPanel.setPixelSize(2*thirdWidth, 2*thirdHeight);
    }

    public ExpressionInterface toExpressionInterface(Expression<Number> expression) {
        // convert from stand-alone version's representation of expressions to web version which uses
        // sub-classes of those used in the stand-alone version
	if (expression instanceof ExpressionInterface) {
	    return (ExpressionInterface) expression;
	} else if (expression instanceof ModifiableOperation<?, ?>) {
            @SuppressWarnings("unchecked")
            ModifiableOperation<Number, Number> operation = 
        	(ModifiableOperation<Number, Number>) expression;
            return new CompoundExpression(operation.getOperator(), 
        	                          toExpressionInterface(operation.getOperand(0)),
        	                          toExpressionInterface(operation.getOperand(1)),
        	                          operation.getIdString());
        } else if (expression instanceof TiedNumberExpression<?>) {
            if (expression.isSpecified()) {
        	TiedNumberExpression<Number> tiedNumberExpression = 
        	    (TiedNumberExpression<Number>) expression;
        	TiedNumber tiedNumber = previouslyMappedTiedNumbers.get(tiedNumberExpression);
        	if (tiedNumber != null) {
        	    return tiedNumber;
        	}
        	tiedNumber = new TiedNumber(tiedNumberExpression.evaluate());
        	previouslyMappedTiedNumbers.put(tiedNumberExpression, tiedNumber);
        	tiedNumber.setDisplayMode(tiedNumberExpression.getDisplayMode());
        	tiedNumber.setName(tiedNumberExpression.getName());
        	tiedNumber.setNamed(tiedNumberExpression.isNamed());
        	tiedNumber.setLocked(tiedNumberExpression.isLocked());
        	tiedNumber.setKeyAvailable(tiedNumberExpression.isKeyAvailable());
        	tiedNumber.setIdString(tiedNumberExpression.getIdString());
        	return tiedNumber;
            } else {
        	return new UnspecifiedTiedNumber(expression.evaluate());
            }
        } else if (expression instanceof ValueExpression<?>) {
            return new TiedNumber(expression.evaluate());
        } else {
            Utilities.severe("Expression is neither a ModifiableOperation or TiedNumberExpression.");
        }
        return null;
    }

    public void refresh() {
	// defer so that the geometry has settled before recomputing the geometry of my model
	if (refreshCommandScheduled) {
	    return;
	}
	refreshCommandScheduled = true;
	Scheduler.get().scheduleDeferred(refreshCommand);
    }
    
    public Dimension getPreferredSize() {
	if (URLParameters.isThumbnail()) {
	    return null;
	}
	TouchSplitLayoutPanel splitPanel = Expresser.getSplitPanel();
	Integer gridSize = getGridSize();
	if (gridSize == null) {
	    return null;
	}
	int preferredWidth = Window.getClientWidth()-Expresser.VERTICAL_SCROLL_BAR_WIDTH*2;
	if (MiGenConfiguration.isTiedNumbersControlPanelEnabled() && !MiGenConfiguration.isTiedNumbersControlPanelOnTop()) {
	    ExpresserVerticalPanel toolsOnTheSide = Expresser.instance().getToolsOnTheSide();
	    int toolsWidth = toolsOnTheSide == null ? 0 : toolsOnTheSide.getOffsetWidth();
	    preferredWidth -= Math.max(235, toolsWidth)+30;
	}
	ExpresserMenuBar menuBar = Expresser.getMenuBar();
	int menuBarHeight = menuBar == null ? 0 : menuBar.getOffsetHeight();
	int toolBarHeight = toolBar == null ? 0 : toolBar.getOffsetHeight();
	// need room for possibly two horizontal scroll bars (one for entire panel and one for the canvas)
	int preferredHeight = Window.getClientHeight()-(menuBarHeight+toolBarHeight+Expresser.HORIZONTAL_SCROLL_BAR_HEIGHT*2);
	preferredHeight -= Expresser.instance().getTranslationBarHeight();
	if (splitPanel != null) {
	    int absoluteLeft = Expresser.getMyModelPanel().getAbsoluteLeft();
	    if (absoluteLeft > 0) {
		preferredWidth -= absoluteLeft;
	    }
	}
	MyModelCanvasToolBar myModelToolBar = Expresser.instance().getMyModelToolBar();
	int canvasToolBarWidth = myModelToolBar == null ? 0 : myModelToolBar.getOffsetWidth();
	if (preferredWidth < canvasToolBarWidth) {
	    preferredWidth = canvasToolBarWidth;
	    // above will add a horizontal scroll bar so reduce height accordingly
	    preferredHeight -= Expresser.HORIZONTAL_SCROLL_BAR_HEIGHT;
	}
	if (modelRulesPanel != null) {
	    preferredHeight -= Expresser.instance().getRulesPanelHeight(); 
	}
	if (preferredWidth <= gridSize || preferredHeight <= gridSize) { // too small
	    return null;
	}
	return new Dimension(preferredWidth, preferredHeight);
    }
    
//    protected Panel getOtherPanelIfSplit() {
//	return Expresser.getComputersModelPanel();
//    }

    public boolean isComputersModel() {
	return false;
    }

    public boolean isActivityDocumentCanvas() {
	return false;
    }

    public ModelRulesPanel getModelRulesPanel() {
        return modelRulesPanel;
    }

    public CanvasToolBar getToolBar() {
        return toolBar;
    }

    public void setToolBar(CanvasToolBar toolBar) {
        this.toolBar = toolBar;
    }

    public void setModelInvalidWhileDragInProgress(
    	boolean modelInvalidWhileDragInProgress) {
        this.modelInvalidWhileDragInProgress = modelInvalidWhileDragInProgress;
    }

    public int getXOffset() {
        return xOffset;
    }

    public void setXOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public void setYOffset(int yOffset) {
        this.yOffset = yOffset;
    }

    public int getLeft() {
	if (canvasScrollPanel != null) {
	    int horizontalScrollPosition = canvasScrollPanel.getHorizontalScrollPosition();
	    return getAbsoluteLeft()-horizontalScrollPosition;
	} else {
	    return getAbsoluteLeft(); // -xOffset;
	}
    }
    
    public int getTop() {
	if (canvasScrollPanel != null) {
	    int verticallScrollPosition = canvasScrollPanel.getVerticalScrollPosition();
	    return getAbsoluteTop()-verticallScrollPosition;
	} else {
	    return getAbsoluteTop(); //-yOffset;
	}
    }
    
    public int getAbsoluteYOffset() {
	return getAbsoluteTop()-yOffset;
    }
    
    public boolean isReadOnly() {
	return false;
    }

    public int modelXToLeft(int x) {
	return x*getGridSize()+xOffset;
    }

    public int modelYToTop(int y) {
	return y*getGridSize()+yOffset;
    }

    public int getWidgetLeft(Widget widget) {
	return expresserCanvasPanel.getWidgetLeft(widget);
    }
    
    public int getWidgetTop(Widget widget) {
	return expresserCanvasPanel.getWidgetTop(widget);
    }

    public String getCurrentTimeStamp(boolean warnIfNotUpToDate) {
	EventManager eventManager = getEventManager();
	if (eventManager == null) {
	    return null;
	} else {
	    return eventManager.getCurrentTimeStamp(warnIfNotUpToDate);
	}
    }
    
    public String getPreviousTimeStamp() {
	EventManager eventManager = getEventManager();
	if (eventManager == null) {
	    return null;
	} else {
	    return eventManager.getPreviousTimeStamp();
	}
    }
    
    public String getFirstTimeStamp() {
	EventManager eventManager = getEventManager();
	if (eventManager == null) {
	    return null;
	} else {
	    return eventManager.getFirstTimeStamp();
	}
    }
    
    public String getNextTimeStamp() {
	EventManager eventManager = getEventManager();
	if (eventManager == null) {
	    return null;
	} else {
	    return eventManager.getNextTimeStamp();
	}
    }
    
    public String getLastReportedTimeStamp() {
	EventManager eventManager = getEventManager();
	if (eventManager == null) {
	    return null;
	} else {
	    return eventManager.getLastReportedTimeStamp();
	}
    }

    public Widget getWidgetWithId(String objectId) {
	return expresserCanvasPanel.getWidgetWithId(objectId);
    }

    public void clearPreviouslyMappedTiedNumbers() {
        previouslyMappedTiedNumbers.clear();	
    }

    public List<PatternPropertyList> getOpenPropertyLists() {
	return expresserCanvasPanel.getOpenPropertyLists();
    }

    public void selectAllShapes() {
	List<ShapeView> allShapes = expresserCanvasPanel.selectAllShapes();
	int xTotal = 0;
	int yTotal = 0;
	int shapeCount = 0;
	for (ShapeView shapeView : allShapes) {
	    xTotal += shapeView.getAbsoluteLeft();
	    yTotal += shapeView.getAbsoluteTop();
	    shapeCount++;
	}
	if (shapeCount > 0) {
	    popupMenu(xTotal/shapeCount, yTotal/shapeCount);
	}
    }

    public AnimationButton createAnimationButton() {
	if (animationButton == null) {
	    animationButton = new AnimationButton(this);
	}
	return animationButton;
    }

    protected LocatedExpression<Number> addExpressionPanelToModel(ExpressionPanel expressionPanel) {
	int modelX = expressionPanel.getModelX(ExpresserCanvas.this);
	int modelY = expressionPanel.getModelY(ExpresserCanvas.this);
	LocatedExpression<Number> locatedExpression = 
		new LocatedExpression<Number>(
			expressionPanel.getExpressionInterface().getExpression(), modelX, modelY);
	getModel().addLocatedExpression(locatedExpression);
	return locatedExpression;
    }

    public PropertyList addPropertyList(ShapeView shapeView, int left, int top) {
	return expresserCanvasPanel.addPropertyList(shapeView, left, top);
    }

}

