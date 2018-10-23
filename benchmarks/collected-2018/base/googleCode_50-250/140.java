// https://searchcode.com/api/result/3386524/

/**
 * Copyright 2010 Philippe Beaudoin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.puzzlebazar.client.util;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

/**
 * A class that monitor changes to a specific object.
 * This object does not participate in dependancy injection, but you
 * should call {@link #release()} whenever you're done using it.
 * This class is usually best used through {@link ChangeMonitorImpl}.
 *
 * @author Philippe Beaudoin
 */
public class ChangeMonitorUnit
implements ValueChangeHandler<Object>, KeyDownHandler, ChangeHandler {

  private class MyTimer extends Timer {

    private Object previousValue;

    public void start() {
      previousValue = null;
      this.schedule(50);
    }

    @Override
    public void run() {
      final Object value = getWidgetValue();
      if (previousValue == null || !previousValue.equals(value)) {
        previousValue = value;
        checkChanges(value);
        this.schedule(1500);
      } else {
        assert handlerRegistration == null;
        handlerRegistration = ((HasAllKeyHandlers) widget).addKeyDownHandler(ChangeMonitorUnit.this);
      }
    }
  }

  private final Object widget;
  private final MonitorHandler handler;
  private final MyTimer timer = new MyTimer();
  private HandlerRegistration handlerRegistration;
  private Object originalValue;
  private boolean changed;

  /**
   * Creates an object to monitor change within an object.
   *
   * @param widget The object to monitor. The object will be tested
   *               for a number of supported types and the change
   *               monitor will adapt to the type.
   * @param handler The {@link MonitorHandler} to notify when change are detected or reverted.
   */
  @SuppressWarnings("unchecked")
  public ChangeMonitorUnit(
      final Object widget,
      final MonitorHandler handler) {
    this.widget = widget;
    this.handler = handler;
    reset();

    if (widget instanceof TextBox) {
      handlerRegistration = ((HasAllKeyHandlers) widget).addKeyDownHandler(this);
    } else if (widget instanceof HasValueChangeHandlers<?>) {
      handlerRegistration = ((HasValueChangeHandlers<Object>) widget).addValueChangeHandler(this);
    } else if (widget instanceof HasChangeHandlers) {
      handlerRegistration = ((HasChangeHandlers) widget).addChangeHandler(this);
    } else {
      handlerRegistration = null;
    }
  }

  /**
   * Call this method when you're done using the monitor.
   */
  public void release() {
    if (handlerRegistration != null) {
      handlerRegistration.removeHandler();
    }
    timer.cancel();
  }

  /**
   * Checks if the monitored object has changed.
   *
   * @return <code>true</code> if the object has changed, <code>false</code> otherwise.
   */
  public boolean hasChanged() {
    return changed;
  }

  /**
   * Reset the original value the one currently contained in the widget.
   */
  public void reset() {
    originalValue = getWidgetValue();
    changed = false;
  }

  @Override
  public void onValueChange(ValueChangeEvent<Object> event) {
    checkChanges(event.getValue());
  }

  @Override
  public void onChange(ChangeEvent event) {
    checkChanges(getWidgetValue());
  }

  @Override
  public void onKeyDown(KeyDownEvent event) {
    scheduleCheckChanges();
  }

  /**
   * Gets the value currently contained in the widget. The method used
   * depends on the widget type.
   *
   * @return The value contained in the widget
   */
  private Object getWidgetValue() {
    if (widget instanceof HasText) {
      return ((HasText) widget).getText();
    }
    if (widget instanceof ListBox) {
      return ((ListBox) widget).getSelectedIndex();
    }
    assert false : "Unsupported widget class: " + widget.getClass();

    return null;
  }

  /**
   * Schedule a check changes after a key down a short way into
   * the future, to reduce UI stress. Only call from
   * onKeyDown, because it assumes the monitored object implements
   * HasAllKeyHandlers.
   */
  private void scheduleCheckChanges() {
    handlerRegistration.removeHandler();
    handlerRegistration = null;
    timer.start();
  }

  private void checkChanges(Object value) {
    boolean newChanged = !value.equals(originalValue);
    if (changed == newChanged) {
      return;
    }
    changed = newChanged;
    if (changed) {
      handler.changeDetected();
    } else {
      handler.changeReverted();
    }
  }

}

