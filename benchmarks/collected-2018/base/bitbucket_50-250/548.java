// https://searchcode.com/api/result/58136326/

/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.waveprotocol.wave.client.wavepanel.impl.edit;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.common.util.EventWrapper;
import org.waveprotocol.wave.client.common.util.KeyCombo;
import org.waveprotocol.wave.client.common.util.KeySignalListener;
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.util.ClientFlags;
import org.waveprotocol.wave.client.wave.ContentDocumentSinkFactory;
import org.waveprotocol.wave.client.wavepanel.impl.WavePanelImpl;
import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFramePresenter;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicBlipMetaView.MenuOption;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.widget.common.LogicalPanel;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;

/**
 * Inteprets focus-frame movement as reading actions, and also provides an
 * ordering for focus frame movement, based on unread content.
 *
 */
public final class EditSession
    implements FocusFramePresenter.Listener, WavePanelImpl.LifecycleListener, KeySignalListener {

  public interface Listener {
    void onSessionStart(Editor e);
    void onSessionEnd(Editor e);
  }

  private static final EditorSettings EDITOR_SETTINGS = new EditorSettings()
      .setHasDebugDialog(LogLevel.showErrors() || ClientFlags.get().enableEditorDebugging())
      .setUndoEnabled(ClientFlags.get().enableUndo())
      .setUseFancyCursorBias(ClientFlags.get().useFancyCursorBias())
      .setUseSemanticCopyPaste(ClientFlags.get().useSemanticCopyPaste())
      .setUseWhitelistInEditor(ClientFlags.get().useWhitelistInEditor())
      .setUseWebkitCompositionEvents(ClientFlags.get().useWebkitCompositionEvents())
      .setCloseSuggestionsMenuDelayMs(ClientFlags.get().closeSuggestionsMenuDelayMs());

  private static final KeyBindingRegistry KEY_BINDINGS = new KeyBindingRegistry();

  private final ModelAsViewProvider views;
  private final ContentDocumentSinkFactory documents;
  private final LogicalPanel container;
  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  private BlipView editing;
  private Editor editor;

  public EditSession(
      ModelAsViewProvider views, ContentDocumentSinkFactory documents, LogicalPanel container) {
    this.views = views;
    this.documents = documents;
    this.container = container;
  }

  /**
   * Warms up the editor code (e.g., internal statics) by creating and throwing
   * away an editor, in order to reduce the latency of starting the first edit
   * session.
   */
  void warmUp() {
    Editors.create();
  }

  @Override
  public void onInit() {
  }

  @Override
  public void onReset() {
    endSession();
  }

  /**
   * Starts an edit session on a blip. If there is already an edit session on
   * another blip, that session will be moved to the new blip.
   *
   * @param blipUi blip to edit
   */
  public void startEditing(BlipView blipUi) {
    Preconditions.checkArgument(blipUi != null);
    endSession();
    startNewSession(blipUi);
  }

  /**
   * Ends the current edit session, if there is one.
   */
  public void stopEditing() {
    endSession();
  }

  /**
   * Starts a new document-edit session on a blip.
   *
   * @param blipUi blip to edit.
   */
  private void startNewSession(BlipView blipUi) {
    assert editing == null && blipUi != null;

    // Find the document.
    ConversationBlip blip = views.getBlip(blipUi);
    ContentDocument document = documents.get(blip).getDocument();
    blipUi.getMeta().select(MenuOption.EDIT);

    // Create or re-use and editor for it.
    editor = Editors.attachTo(document);
    container.doAdopt(editor.getWidget());
    editor.init(null, KEY_BINDINGS, EDITOR_SETTINGS);
    editor.addKeySignalListener(this);
    editor.setEditing(true);
    editor.focus(false);
    editing = blipUi;
    fireOnSessionStart(editor);
  }

  /**
   * Stops editing if there is currently an edit session.
   *
   * @return true if there was an edit session, false otherwise.
   */
  private boolean endSession() {
    if (editing != null) {
      container.doOrphan(editor.getWidget());
      editor.blur();
      editor.setEditing(false);
      editor.reset();
      // TODO(user): this does not work if the view has been deleted and detached.
      editing.getMeta().deselect(MenuOption.EDIT);
      Editor oldEditor = editor;
      editor = null;
      editing = null;
      fireOnSessionEnd(oldEditor);
      return true;
    } else {
      return false;
    }
  }

  //
  // Events.
  //

  @Override
  public void onFocusMoved(BlipView oldUi, BlipView newUi) {
    boolean wasEditing = endSession();
    if (wasEditing && newUi != null) {
      startEditing(newUi);
    }
  }

  @Override
  public boolean onKeySignal(Widget sender, SignalEvent signal) {
    KeyCombo key = EventWrapper.getKeyCombo(signal);
    switch (key) {
      case SHIFT_ENTER:
        endSession();
        return true;
      case ESC:
        // TODO: undo.
        endSession();
        return true;
      default:
        return false;
    }
  }

  //
  // Listeners.
  //

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void fireOnSessionStart(Editor editor) {
    for (Listener listener : listeners) {
      listener.onSessionStart(editor);
    }
  }

  private void fireOnSessionEnd(Editor editor) {
    for (Listener listener : listeners) {
      listener.onSessionEnd(editor);
    }
  }
}

