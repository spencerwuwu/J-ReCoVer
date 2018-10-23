// https://searchcode.com/api/result/4898644/

package org.timepedia.chronoscope.client.browser.flashcanvas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.google.gwt.core.client.JsArrayMixed;
import org.timepedia.chronoscope.client.Chart;
import org.timepedia.chronoscope.client.Cursor;
import org.timepedia.chronoscope.client.browser.BrowserCanvasImage;
import org.timepedia.chronoscope.client.browser.BrowserCanvasImage;
import org.timepedia.chronoscope.client.browser.Chronoscope;
import org.timepedia.chronoscope.client.browser.GwtView;
import org.timepedia.chronoscope.client.canvas.Bounds;
import org.timepedia.chronoscope.client.canvas.Canvas;
import org.timepedia.chronoscope.client.canvas.CanvasImage;
import org.timepedia.chronoscope.client.canvas.CanvasPattern;
import org.timepedia.chronoscope.client.canvas.CanvasReadyCallback;
import org.timepedia.chronoscope.client.canvas.Color;
import org.timepedia.chronoscope.client.canvas.DisplayList;
import org.timepedia.chronoscope.client.canvas.Layer;
import org.timepedia.chronoscope.client.canvas.PaintStyle;
import org.timepedia.chronoscope.client.canvas.RadialGradient;
import org.timepedia.chronoscope.client.canvas.View;
import org.timepedia.chronoscope.client.render.LinearGradient;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;

public class FlashCanvas extends Canvas {
  // In order to reduce bandwidth across the flash-browser barrier,
  // numbers like 37.9439483984y72394 will be cut to SIGDIG precision
  // eg 37.9440
  private static int SIGDIG = 6;

  public static String FLASH_ALTERNATIVES = "\n"; // <!-- chart not visible in browser without canvas element -->\n";   // TODO - make this configurable.
          // "<p>Modern browsers such as Chrome, Safari, Firefox, or Internet Explorer 9 use javascript and HTML (rather than Flash) for a faster charting experience.</p>\n";

  public static String FLASH_ADVICE =
        "<p>If you're using Internet Explorer 6, 7, or 8 you need to enable or install Flash Player to experience these charts.</p>\n" +
        "<p><a href=\"http://www.adobe.com/go/getflashplayer\"><img src=\"http://www.adobe.com/images/shared/download_buttons/get_flash_player.gif\" alt=\"Get Adobe Flash player\" /></a></p>\n" +
                FLASH_ALTERNATIVES;

  public static final String CMDSEP = "%";


  private static native void createFlashLayer(String canvasId, String layerId,
      double x, double y, double width, double height) /*-{
        var flashCanvas = $wnd.navigator.appName.indexOf("Microsoft") != -1 ? $wnd[canvasId] : $doc[canvasId];
        flashCanvas && flashCanvas.createCanvas && flashCanvas.createCanvas(layerId, x, y, width, height);
    }-*/;

  private static native void drawFlashCanvas(String canvasId, String cmds) /*-{
        var flashCanvas = $wnd.navigator.appName.indexOf("Microsoft") != -1 ? $wnd[canvasId] : $doc[canvasId];
        flashCanvas && flashCanvas.drawframe && flashCanvas.drawframe(cmds);
    }-*/;

  private static native void flashDisposeLayer(String canvasId, String layerId) /*-{
        var flashCanvas = $wnd.navigator.appName.indexOf("Microsoft") != -1 ? $wnd[canvasId] : $doc[canvasId];
        flashCanvas && flashCanvas.disposeCanvas && flashCanvas.disposeCanvas(layerId);
    }-*/;

  private static native void flashSetLayerBounds(String canvasId, String layerId,
    double x, double y, double width, double height) /*-{
      var flashCanvas = $wnd.navigator.appName.indexOf("Microsoft") != -1 ? $wnd[canvasId] : $doc[canvasId];
      flashCanvas && flashCanvas.setBoundss && flashCanvas.setBounds(layerId, x, y, width, height);
  }-*/;

  private ArrayList<String> selectedLayers = new ArrayList<String>();

  private String selectedLayerId = "";

  // private FlashLayer rootLayer;   // FIXME - get rid of rootLayer

  private final HashMap<String,FlashLayer> id2Layer = new HashMap<String,FlashLayer>();

  private final int width;
  private final int height;

  private Element canvasDivElement;
  private String canvasDivElementId;

  private SwfObject swfobj;
  private Element swfObjectElement;
  private String swfObjectElementId;

  private String readyFn = "";

  private JavaScriptObject ctx = null;

  public FlashCanvas(FlashView view, int width, int height) {
    super(view);
    this.width = width;
    this.height = height;
    log("view: "+view.getViewId() + " ctor w:"+width+" h:"+height);

    canvasDivElementId = getView().getViewId() + "fc";
    if (null == canvasDivElement) {
      log("creating canvasDivElement");
      canvasDivElement = DOM.createElement("div");
    }
    DOM.setElementAttribute(canvasDivElement, "id", canvasDivElementId);

    Element viewElement = view.getElement();
    if ((viewElement != null) && (canvasDivElement !=null)) {
      DOM.appendChild(viewElement, canvasDivElement);
    }

    ctx = makectx();

    swfobj = GWT.create(SwfObject.class);
    swfObjectElementId = canvasDivElementId+"so";
    log("soid: " + swfObjectElementId);
    readyFn = "canvasReadyFn" + swfObjectElementId;
    log("readyFn: "+readyFn);
  }

  public void disposeLayer(String layerId) {
    log("disposeLayer "+layerId);
    Layer layer = getLayer(layerId);
    if (layer != null) {
      flashDisposeLayer(swfObjectElementId, layerId);
      layer.dispose();
    }
  }

  public void remove(String layerId) {
    log("remove "+layerId);
    id2Layer.remove(layerId);
    if (selectedLayers.contains(layerId)) {
      selectedLayers.remove(layerId);
    }
  }

  public void dispose() {
    HashSet<String> layers = new HashSet<String>(id2Layer.keySet());
    for (String layerId: layers) {
      disposeLayer(layerId);
    }
    if (!id2Layer.isEmpty()) {
      id2Layer.clear();
    }
    if ((null != selectedLayers) && (!selectedLayers.isEmpty())) {
      selectedLayers.clear();
    }

    // remove from the DOM
    if (null != swfObjectElement ) {
      while (swfObjectElement.hasChildNodes()) {
        DOM.removeChild(swfObjectElement, DOM.getFirstChild(swfObjectElement));
      }
      if (swfObjectElement.hasParentElement()) {
        swfObjectElement.removeFromParent();
      }
    }
    if (null != canvasDivElement) {
      if (canvasDivElement.hasParentElement()) {
        canvasDivElement.removeFromParent();
      }
      canvasDivElement = null;
    }
  }

//  public void arc(double x, double y, double radius, double startAngle,
//      double endAngle, int clockwise) {
//    rootLayer.arc(x, y, radius, startAngle, endAngle, clockwise);
//  }

  @Override
  public void attach(final View view, final CanvasReadyCallback canvasReadyCallback) {
    log("attach readyFn:"+readyFn);
    exportReadyFn(readyFn, view, new CanvasReadyCallback() {
      boolean initialized = false;
      public void onCanvasReady(Canvas canvas) {
        if (!initialized) {
          initialized = true;
          canvasReadyCallback.onCanvasReady(canvas);
        } else {
          resyncLayers();
        }
      }
    });

    // String codeBasePref = GWT.getHostPageBaseURL().startsWith("https") ? "https" : "http";

    String swfUrl = Chronoscope.getURL(GWT.getModuleBaseURL() + "flcanvas.swf");
    Element altContent = DOM.createElement("p");
    altContent.setInnerText("Install Flash plugin to view chart.");
    swfObjectElement = swfobj.create(swfObjectElementId, swfUrl, readyFn, altContent);

    Bounds initialBounds = new Bounds(0,0,view.getWidth(), view.getHeight());
    GwtView.initDivElement(canvasDivElement, initialBounds);
    GwtView.positionDivElement(canvasDivElement, initialBounds);

    // log("create rootLayer");
    // rootLayer = createLayer(Layer.BACKGROUND, initialBounds);

    if ((canvasDivElement != null) && (swfObjectElement != null)) {
      DOM.appendChild(canvasDivElement, swfObjectElement);
    }


//    super.attach(view, canvasReadyCallback);
  }

  public void beginFrame() {
    super.beginFrame();
    // selectedLayerId = getLayerId();
    selectedLayers.clear();
//        Iterator it = id2Layer.values().iterator();
//        while(it.hasNext()) {
//            FlashLayer fl=(FlashLayer)it.next();
//            fl.beginFrame();
//        }
    clearFlashDisplayList();
  }

//  public void beginPath() {
//    rootLayer.beginPath();
//  }

  public void canvasSetupDone() {
    flashCanvasSetupDone(swfObjectElementId);
  }

//  public void clearRect(double x, double y, double width, double height) {
//    rootLayer.clearRect(x, y, width, height);
//  }

//  public void clearTextLayer(String layerName) {
//    rootLayer.clearTextLayer(layerName);
//  }

//  public void clip(double x, double y, double width, double height) {
//    rootLayer.clip(x, y, width, height);
//  }

//  public void closePath() {
//    rootLayer.closePath();
//  }

//  public final native void clearctx() /*-{
//            this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx = [];
//  }-*/;

  public final native void cmd(String cmd, String arg) /*-{
             this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(cmd, 1, arg);
      }-*/;

  public final native void cmd(String cmd, float arg) /*-{
             this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(cmd, 1, arg.toPrecision(5));
      }-*/;

  public final native void cmd(String cmd, double arg) /*-{
             this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(cmd, 1, arg.toPrecision(5));
      }-*/;

  public final native void cmd(String cmd, double arg1, double arg2) /*-{
             this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(cmd, 2,
                     arg1.toFixed(0), arg2.toFixed(0));
      }-*/;

  public final native void cmd(String cmd, double arg1, double arg2, double arg3, double arg4) /*-{
             this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(cmd, 4,
                     arg1.toFixed(0), arg2.toFixed(0), arg3.toFixed(0), arg4.toFixed(0));
         }-*/;

  public final native void cmd(String cmd, double arg1, double arg2,
      double arg3, double arg4, double arg5, double arg6) /*-{
                this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(cmd, 6,
                        arg1.toFixed(0), arg2.toFixed(0), arg3.toPrecision(5),
                        arg4.toPrecision(5), arg5.toPrecision(5), arg6.toFixed(0));
      }-*/;

  public final native void cmd(String cmd) /*-{
                this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(cmd, 0);
      }-*/;

  public final native void cmd(String cmd, String arg1, int arg2) /*-{
             this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(cmd, 2, arg1, arg2);
         }-*/;

  public final native void cmd(String cmd, double x, double y, String label,
      String fontFamily, String fontWeight, String fontSize, String layerName) /*-{
           this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(cmd, 7,
                   x.toFixed(0), y.toFixed(0), label, fontFamily, fontWeight, fontSize, layerName);
    }-*/;

  public final native void cmd(String cmd, String layerName, double x, double y, double width, double height) /*-{
           this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(cmd, 5,
                   layerName, x.toFixed(0), y.toFixed(0), width.toFixed(0), height.toFixed(0));
    }-*/;

  public final native void cmd(String cmd, double x, double y, double a,
      String label, String fontFamily, String fontWeight, String fontSize,
      String layerName) /*-{
           this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(cmd, 8,
                   x.toFixed(0), y.toFixed(0), a.toPrecision(5), label, fontFamily, fontWeight, fontSize, layerName);
    }-*/;

  public final native void cmd(String cmd, double x, double y,
      String label, String fontFamily, String fontWeight, String fontSize,
      String layerName, String cursorStyle) /*-{
           this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(cmd, 8,
                   x.toFixed(0), y.toFixed(0), label, fontFamily, fontWeight, fontSize, layerName, cursorStyle);
    }-*/;


//  public DisplayList createDisplayList(String id) {
//    return rootLayer.createDisplayList(id);
//  }

  public FlashLayer createLayer(String layerId, Bounds b) {
    log("createLayer "+layerId+" bounds: "+b);
    FlashLayer layer = getLayer(layerId);
    if (b == null) { log ("createLayer null bounds"); b = new Bounds(); }
    if (layer == null) {
      createFlashLayer(swfObjectElementId, layerId, b.x, b.y, b.width, b.height);
      layer = new FlashLayer(this, layerId, b);
      id2Layer.put(layerId, layer);
    } else {
      log("reusing "+layerId);
      layer.save();
      layer.clear();
      layer.setBounds(b);
      layer.restore();
    }
    return layer;
  }

  public void setLayerBounds(String layerId, Bounds b) {
      log("setLayerBounds "+layerId+" bounds: "+b);

      FlashLayer layer = id2Layer.get(layerId);
      if (null != layer) {
        layer.dispose();
      }
      createLayer(layerId, b);

//
//
//    if (b == null) { log ("setLayerBounds null bounds"); return; }
//    flashSetLayerBounds(swfObjectElementId, layerId, b.x, b.y, b.width, b.height);
  }

//  public LinearGradient createLinearGradient(double x, double y, double w, double h) {
//    return rootLayer.createLinearGradient(x, y, w, h);
//  }

//  public PaintStyle createPattern(String imageUri) {
//    return rootLayer.createPattern(imageUri);
//  }

//  public RadialGradient createRadialGradient(double x0, double y0, double r0,
//      double x1, double y1, double r1) {
//    return rootLayer.createRadialGradient(x0, y0, r0, x1, y1, r1);
//  }

//  public void drawImage(Layer layer, double x, double y, double width, double height) {
//    rootLayer.drawImage(layer, x, y, width, height);
//  }

//  public void drawImage(Layer layer, double sx, double sy, double swidth,
//      double sheight, double dx, double dy, double dwidth, double dheight) {
//    rootLayer.drawImage(layer, sx, sy, swidth, sheight, dx, dy, dwidth, dheight);
//  }

//  public void drawRotatedText(double x, double y, double angle, String label,
//      String fontFamily, String fontWeight, String fontSize, String layerName,
//      Chart chart) {
//    rootLayer.drawRotatedText(x, y, angle, label, fontFamily, fontWeight, fontSize,
//            layerName, chart);
//  }

//  public void drawText(double x, double y, String label, String fontFamily,
//      String fontWeight, String fontSize, String layerName) {
//    rootLayer.drawText(x, y, label, fontFamily, fontWeight, fontSize, layerName, Cursor.DEFAULT);
//  }

  public void endFrame() {
    super.endFrame();
    log("endFrame drawFlashCanvas "+swfObjectElementId);
    log(getFlashDisplayList());
    drawFlashCanvas(swfObjectElementId, getFlashDisplayList());
  }

//  public void fill() {
//    rootLayer.fill();
//  }

//  public void fillRect(double x, double y, double w, double h) {
//    rootLayer.fillRect(Math.floor(x), Math.floor(y), Math.ceil(w), Math.floor(h));
//  }

//  public void fillRect() {
//    rootLayer.fillRect();
//  }

  public native void flashCanvasSetupDone(String canvasId) /*-{
      var flashCanvas = $wnd.navigator.appName.indexOf("Microsoft") != -1 ? $wnd[canvasId] : $doc[canvasId];
      flashCanvas && flashCanvas.viewInitialized && 
        flashCanvas.viewInitialized();
  }-*/;

  public Bounds getBounds() {
    return new Bounds(0,0,width,height);
  }

  public Element getElement() {
    return DOM.getElementById(canvasDivElementId);
  }

  public native String getFlashDisplayList() /*-{
           return this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.join(@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::CMDSEP);
    }-*/;

  public double getHeight() {
    return height;
  }

  public FlashLayer getLayer(String layerId) {
    return id2Layer.get(layerId);
  }

//  public float getLayerAlpha() {
//    return rootLayer.getLayerAlpha();
//  }

//  public String getLayerId() {
//    return rootLayer.getLayerId();
//  }

//  public int getLayerOrder() {
//    return rootLayer.getLayerOrder();
//  }

  public Layer getRootLayer() {
    return createLayer(Layer.BACKGROUND, new Bounds(0, 0, width, height));
  }

  public CanvasImage createImage(String url) {
    return new BrowserCanvasImage(url);
  }

//  public int getScrollLeft() {
//    return rootLayer.getScrollLeft();
//  }

//  public String getStrokeColor() {
//    return rootLayer.getStrokeColor();
//  }

//  public String getTransparency() {
//    return rootLayer.getTransparency();
//  }

//  public double getWidth() {
//    return rootLayer.getWidth();
//  }

//  public boolean isVisible() {
//    return rootLayer.isVisible();
//  }

//  public void lineTo(double x, double y) {
//    rootLayer.lineTo(x, y);
//  }

//  public void moveTo(double x, double y) {
//    rootLayer.moveTo(x, y);
//  }

  public final void popSelection() {
    if (null == selectedLayers || 0 == selectedLayers.size()) {
      return;
    } else {
      selectedLayerId = (String) selectedLayers.remove(selectedLayers.size() - 1);
    }
    if (null == selectedLayerId || selectedLayerId.isEmpty()) {
      log("ERROR popSelection empty "+selectedLayerId);
    }
    cmd("L", selectedLayerId);
  }

  public final native void push(String cmd) /*-{
          this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(cmd);
      }-*/;

  public final native void push(float f) /*-{
          this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(f);

      }-*/;

  public final native void push(double f) /*-{
          this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::ctx.push(f);
      }-*/;

  public final void pushNCmd(String cmd, int n) {
    push(cmd);
    push(n);
  }

  public final void pushSelection(String selectedLayerId) {
    log("pushSelection: "+selectedLayerId);
    selectedLayers.add(selectedLayerId);
    this.selectedLayerId = selectedLayerId;
    if (null == selectedLayerId || selectedLayerId.isEmpty()) {
      log("ERROR pushSelection empty "+selectedLayerId);
    } else {
      cmd("L", selectedLayerId);
    }
  }

//  public void rect(double x, double y, double width, double height) {
//    rootLayer.rect(x, y, width, height);
//  }
//
//  public void restore() {
//    rootLayer.restore();
//  }

    // TODO - switch the angles to type Radians or Degrees, too much ambiguity
//  public int rotatedStringHeight(String str, double rotationAngle,
//      String fontFamily, String fontWeight, String fontSize) {
//    return rootLayer
//        .rotatedStringHeight(str, rotationAngle, fontFamily, fontWeight, fontSize);
//  }

//  public int rotatedStringWidth(String str, double rotationAngle,
//      String fontFamily, String fontWeight, String fontSize) {
//    return rootLayer
//        .rotatedStringWidth(str, rotationAngle, fontFamily, fontWeight, fontSize);
//  }

//  public void save() {
//    rootLayer.save();
//  }

//  public void scale(double sx, double sy) {
//    rootLayer.scale(sx, sy);
//  }

//  public void setCanvasPattern(CanvasPattern canvasPattern) {
//    rootLayer.setCanvasPattern(canvasPattern);
//  }

//  public void setComposite(int mode) {
//    rootLayer.setComposite(mode);
//  }

//  public void setFillColor(Color color) {
//    rootLayer.setFillColor(color);
//  }

//  public void setFillColor(PaintStyle p) {
//    rootLayer.setFillColor(p);
//  }

//  public void setLayerAlpha(float alpha) {
//    // rootLayer.setLayerAlpha(alpha);
//  }

//  public void setLayerOrder(int zorder) {
//    // rootLayer.setLayerOrder(zorder);
//  }

//  public void setLinearGradient(LinearGradient lingrad) {
//    rootLayer.setLinearGradient(lingrad);
//  }

//  public void setLineWidth(double width) {
//    rootLayer.setLineWidth(width);
//  }

//  public void setRadialGradient(RadialGradient radialGradient) {
//    rootLayer.setRadialGradient(radialGradient);
//  }

//  public void setScrollLeft(int i) {
//    rootLayer.setScrollLeft(i);
//  }

//  public void setShadowBlur(double width) {
//    rootLayer.setShadowBlur(width);
//  }

//  public void setShadowColor(String color) {
//    rootLayer.setShadowColor(color);
//  }

//  public void setShadowColor(Color shadowColor) {
//    rootLayer.setShadowColor(shadowColor);
//  }

//  public void setShadowOffsetX(double x) {
//    rootLayer.setShadowOffsetX(x);
//  }

//  public void setShadowOffsetY(double y) {
//    rootLayer.setShadowOffsetY(y);
//  }

//  public void setStrokeColor(Color color) {
//    rootLayer.setStrokeColor(color);
//  }

//  public void setStrokeColor(PaintStyle p) {
//    rootLayer.setStrokeColor(p);
//  }

//  public void setTransparency(float value) {
//    rootLayer.setTransparency(value);
//  }

  public void setVisibility(boolean visibility) {
    DOM.setStyleAttribute(canvasDivElement, "visibility", visibility ? "visible" : "hidden");
  }

//  public native int stringHeight(String canvasId, String string, String font, String bold, String size, float angle) /*-{
//        var flashCanvas = $wnd.navigator.appName.indexOf("Microsoft") != -1 ? $wnd[canvasId] : $doc[canvasId];
//        if(flashCanvas && flashCanvas.stringHeight) {
//          return flashCanvas.stringHeight(string, font, bold, size, angle);
//        } else {
//          return 10;
//        }
//    }-*/;

  public static int stringHeight(String string, String font, String bold, String size, float angle) {
    //  return stringHeight(swfObjectElementId, string, font, bold, size, angle);
    log("stringHeight "+string + " "+font + " "+ size);

    return parseInt(size);
  }

//  public native int stringWidth(String canvasId, String string, String font, String bold, String size, float angle) /*-{
//        var flashCanvas = $wnd.navigator.appName.indexOf("Microsoft") != -1 ? $wnd[canvasId] : $doc[canvasId];
//        if(flashCanvas && flashCanvas.stringWidth) {
//          return flashCanvas.stringWidth(string, font, bold, size, angle);
//        } else {
//          return 8 * string.length;
//        }
//    }-*/;

  public static int stringWidth(String string, String font, String bold, String size, float angle) {
    // return stringWidth(swfObjectElementId, string, font, bold, size, angle);
    log("stringWidth "+string + " "+font + " "+ size);

    return (int) Math.ceil(0.75d * parseInt(size)*string.length()); // NOTE - very approximate
  }

//  public void stroke() {
//    rootLayer.stroke();
//  }

//  public void translate(double x, double y) {
//    rootLayer.translate(x, y);
//  }

  void clearFlashDisplayList() {
    ctx = makectx();
  }

  private native void exportReadyFn(String readyFn, View view,
      CanvasReadyCallback canvasReadyCallback) /*-{
        var _this=this;
        $wnd[readyFn] = function() {
            _this.@org.timepedia.chronoscope.client.browser.flashcanvas.FlashCanvas::flashCanvasReady(Lorg/timepedia/chronoscope/client/canvas/View;Lorg/timepedia/chronoscope/client/canvas/CanvasReadyCallback;)(view, canvasReadyCallback);
        }
    }-*/;

  private void flashCanvasReady(final View view, final CanvasReadyCallback canvasReadyCallback) {
    Timer t = new Timer() {
      public void run() {
        FlashCanvas.super.attach(view, canvasReadyCallback);
      }
    };
    t.schedule(750);
  }

  private native JsArrayMixed makectx() /*-{
          return [];
      }-*/;

  private void resyncLayers() {
    log("resync");
    for (FlashLayer layer: id2Layer.values()) {
      Bounds bounds = layer.getBounds();
      log("resync "+layer.getLayerId()+ " "+bounds);
      createFlashLayer(swfObjectElementId, layer.getLayerId(),
              bounds.x, bounds.y, bounds.width, bounds.height);
    }
    getView().getChart().reloadStyles();
  }

  /**
   * @param intWithTrailingText
   * @return leading int portion of String, eg "12px" or "12pt" returns 12
   */
   private static int parseInt(String intWithTrailingText) {
     return Integer.valueOf("0" + intWithTrailingText.replaceAll("(\\d*).*", "$1"));
   }

  private static void log(String msg) {
    System.out.println("FlashCanvas> "+msg);
  }
}
