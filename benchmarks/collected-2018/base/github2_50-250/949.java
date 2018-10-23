// https://searchcode.com/api/result/67467894/

/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is "stereotyped" (http://code.google.com/p/stereotyped).
 * 
 * The Initial Developer of the Original Code is Richard Nichols.
 * Portions created by Richard Nichols are Copyright (C) 2010
 * Richard Nichols. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms
 * of the GNU Public License (the  "GPL"), in which case the
 * provisions of GPL License are applicable instead of those
 * above.  If you wish to allow use of your version of this file only
 * under the terms of the GPL and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting  the provisions above and replace  them with the notice and
 * other provisions required by the GPL.  If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the GPL License.
 */
package com.visural.stereotyped.core;

import com.visural.common.collection.readonly.ReadOnlyList;
import com.visural.stereotyped.core.resource.CopyIcon;
import com.visural.stereotyped.core.resource.CutIcon;
import com.visural.stereotyped.core.resource.PasteIcon;
import com.visural.stereotyped.ui.service.ClipboardService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * @version $Id$
 * @author Richard Nichols
 */
public class Slot implements Serializable, OperationPublisher, HasMetaData, HasUuid {

    private static final long serialVersionUID = 1L;
    private List<Component> content = new ArrayList<Component>();
    private final String name;
    protected final Component slottedComponent;

    public Slot(String name, Component slotted) {
        this.slottedComponent = slotted;
        this.name = name;
    }

    public final String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return name;
    }

    /**
     * Override this method to implement component filtering for slots, e.g.
     * only accept certain types of components.
     * 
     * @param component
     * @return
     */
    public boolean acceptComponent(Component component) {
        return component != null && acceptComponent(component.getClass());
    }

    /**
     * Override this method to implement component filtering for slots, e.g.
     * only accept certain types of components.
     * 
     * @param component
     * @return
     */
    public boolean acceptComponent(Class<? extends Component> component) {
        return true;
    }

    public Component getSlottedComponent() {
        return slottedComponent;
    }

    // TODO: check for recursive structures - i.e. navigate stereotype to see if
    //       this component is already referenced.
    public void addComponent(Component component) {
        if (component == null) {
            throw new IllegalArgumentException("Null component passed to addComponent()");
        }
        if (acceptComponent(component)) {
            component.setParentSlot(this);
            content.add(component);
        }
        // TODO: some sort of feedback where components have been filtered out
    }

    public void addComponent(int index, Component component) {
        if (component == null) {
            throw new IllegalArgumentException("Null component passed to addComponent()");
        }
        if (acceptComponent(component)) {
            component.setParentSlot(this);
            content.add(index, component);
        }
        // TODO: some sort of feedback where components have been filtered out
    }

    public void moveComponent(Component component, int listIndex) {
        if (content.contains(component)) {
            if (listIndex < 0 || listIndex > this.content.size()) {
                throw new IllegalArgumentException("Index " + listIndex + " is not within the bounds of the slot.");
            }
            int currentIndex = content.indexOf(component);
            if (currentIndex != listIndex) {
                content.remove(component);
                content.add(listIndex, component);
            }
        } else {
            throw new IllegalArgumentException("Component is not contained in this slot.");
        }
    }

    public void deleteComponent(Component component) {
        if (content.contains(component)) {
            content.remove(component);
        } else {
            throw new IllegalArgumentException("Component is not contained in this slot.");
        }
    }

    public ReadOnlyList<Component> getContent() {
        return new ReadOnlyList<Component>(content);
    }

    public List<OperationPointer> getOperations() {
        return OperationInspector.inspect(this);
    }

    public List<StereotypeMetaData> getMetaData() {
        return MetaDataInspector.inspect(this);
    }

    @Operation(prettyName = "Paste", iconOnly = true, iconReference = PasteIcon.class)
    public void paste() {
        Object clipObj = ServiceProvider.getServiceInstance(ClipboardService.class).paste();
        if (clipObj != null && Component.class.isAssignableFrom(clipObj.getClass()) && !Stereotype.class.isAssignableFrom(clipObj.getClass())) {
            this.addComponent(((Component) clipObj).duplicate());
        } else if (clipObj != null && Collection.class.isAssignableFrom(clipObj.getClass())) {
            Collection<Component> cs = duplicateContent((Collection) clipObj);
            for (Component c : cs) {
                this.addComponent(c);
            }
        }
    }

    private Collection<Component> duplicateContent(Collection c) {
        List<Component> result = new ArrayList<Component>();
            for (Object o : c) {
                if (o != null && Component.class.isAssignableFrom(o.getClass()) && !Stereotype.class.isAssignableFrom(o.getClass())) {
                Component nc = (Component) o;
                this.addComponent(nc.duplicate());
        }
    }
        return result;
        }

    @Operation(prettyName = "Cut All", iconOnly = true, iconReference = CutIcon.class)
    public void cutAll() {
        copyAll();
        deleteAll();
}

    @Operation(prettyName = "Copy All", iconOnly = true, iconReference = CopyIcon.class)
    public void copyAll() {
        ClipboardService cs = ServiceProvider.getServiceInstance(ClipboardService.class);
        List<Component> all = new ArrayList<Component>();
        for (Component c : content) {
            c.copy();
            all.add((Component) cs.paste());
        }
        cs.copy(all);
    }

    private void deleteAll() {
        List<Component> all = new ArrayList<Component>();
        all.addAll(content);
        for (Component c : all) {
            c.delete();
}
    }

    private UUID uuid = null;

    public UUID getUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        return uuid;
    }

    // TODO: reduce to protected once fix has been completed
    public void mutateUUID(boolean mutatePreviewStateIds) {
        UUID nu = UUID.randomUUID();
        if (mutatePreviewStateIds) {
            Stereotype s = getStereotype();
            if (s != null) {
                s.getDefaultState().mutateUUID(uuid, nu);
                for (PreviewState ps : s.getPreviewStates()) {
                    ps.mutateUUID(uuid, nu);
                }
            }
        }
        uuid = nu;
    }

    public Stereotype getStereotype() {
        return slottedComponent == null ? null : slottedComponent.getStereotype();
    }



}

