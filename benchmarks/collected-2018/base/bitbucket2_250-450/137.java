// https://searchcode.com/api/result/131020595/

/*
 * Copyright 2012-2015 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.notes.client.swing.internal;

import java.util.ArrayList;
import java.util.List;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.notes.common.Note;
import com.redhat.thermostat.notes.common.NoteDAO;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.Ref;

public abstract class NotesController<R extends Ref, N extends Note, D extends NoteDAO<R, N>> implements InformationServiceController<R> {

    protected static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    protected Clock clock;
    protected ApplicationService appSvc;
    protected R ref;
    protected D dao;
    protected NotesView view;

    private List<N> models;

    public NotesController(Clock clock, final ApplicationService appSvc, R ref, D dao, NotesView view) {
        this.clock = clock;
        this.appSvc = appSvc;
        this.ref = ref;
        this.dao = dao;
        this.view = view;

        models = new ArrayList<>();

        this.view.addNoteActionListener(new ActionListener<NotesView.NoteAction>() {
            @Override
            public void actionPerformed(ActionEvent<NotesView.NoteAction> actionEvent) {
                switch (actionEvent.getActionId()) {
                    /* remote-storage operations */
                    case REMOTE_REFRESH: {
                        remoteGetNotesFromStorage();
                        break;
                    }
                    /* local operations */
                    case LOCAL_ADD: {
                        remoteSaveNotes();
                        localAddNewNote();
                        break;
                    }
                    case LOCAL_SAVE: {
                        String noteId = /* tag = */ (String) actionEvent.getPayload();
                        localSaveNote(noteId);
                        break;
                    }
                    case LOCAL_DELETE: {
                        String noteId = /* tag = */ (String) actionEvent.getPayload();
                        localDeleteNote(noteId);
                        remoteSaveNotes();
                        break;
                    }
                }
            }
        });

        view.addActionListener(new ActionListener<BasicView.Action>() {
            @Override
            public void actionPerformed(ActionEvent<BasicView.Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case HIDDEN:
                        appSvc.getApplicationExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                remoteSaveNotes();
                            }
                        });
                        break;
                    case VISIBLE:
                        appSvc.getApplicationExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                remoteGetNotesFromStorage();
                            }
                        });
                        break;
                    default:
                        throw new AssertionError("Unknown action event: " + actionEvent);
                }
            }
        });
    }

    @Override
    public UIComponent getView() {
        return view;
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translator.localize(LocaleResources.TAB_NAME);
    }

    protected void remoteGetNotesFromStorage() {
        Utils.assertNotInEdt();

        view.setBusy(true);

        models = dao.getFor(ref);
        localUpdateNotesInView();

        view.setBusy(false);
    }

    protected void remoteSaveNotes() {
        view.setBusy(true);

        List<N> remoteModels = dao.getFor(ref);

        List<String> seen = new ArrayList<>();
        for (N remoteModel : remoteModels) {
            N localModel = findById(models, remoteModel.getId());
            if (localModel == null) {
                // deleted
                dao.remove(remoteModel);
            } else {
                if (localModel.getTimeStamp() != remoteModel.getTimeStamp()) {
                    // notes differ
                    dao.update(localModel);
                }
                seen.add(localModel.getId());
            }
        }

        for (N note : models) {
            if (seen.contains(note.getId())) {
                continue;
            }
            dao.add(note);
        }

        view.setBusy(false);
    }

    /** Add a new note */
    protected void localAddNewNote() {
        long timeStamp = clock.getRealTimeMillis();
        String content = "";

        N note = createNewNote(timeStamp, content);
        models.add(note);

        localUpdateNotesInView();
    }

    protected abstract N createNewNote(long timeStamp, String content);

    /** Update the view to match what's in the local cache */
    protected void localUpdateNotesInView() {
        List<N> vmNotes = models;

        // TODO only apply diff of notes to reduce UI glitches/changes
        view.clearAll();

        for (N vmNote : vmNotes) {
            view.add(vmNote);
        }
    }

    /** Update the local cache of notes to match what's in the view */
    protected void localSaveNote(String noteId) {
        long timeStamp = clock.getRealTimeMillis();

        for (N note : models) {
            if (noteId.equals(note.getId())) {
                String oldContent = note.getContent();
                String newContent = view.getContent(noteId);
                if (oldContent.equals(newContent)) {
                    continue;
                }

                view.setTimeStamp(noteId, timeStamp);

                note.setTimeStamp(timeStamp);
                note.setContent(newContent);
            }
        }
    }

    /** Delete a note */
    protected void localDeleteNote(String noteId) {
        N note = findById(models, noteId);
        if (note == null) {
            throw new AssertionError("Unable to find note to delete");
        }

        boolean removed = models.remove(note);
        if (!removed) {
            throw new AssertionError("Deleting a note failed");
        }

        localUpdateNotesInView();
    }

    private static<N extends Note> N findById(List<N> notes, String id) {
        for (N note : notes) {
            if (note.getId().equals(id)) {
                return note;
            }
        }
        return null;
    }
}

