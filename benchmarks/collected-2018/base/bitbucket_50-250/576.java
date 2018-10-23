// https://searchcode.com/api/result/122818473/

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.tasklist.usertasks.actions;

import java.util.List;
import org.netbeans.modules.tasklist.usertasks.table.UTBasicTreeTableNode;
import org.netbeans.modules.tasklist.usertasks.table.UTTreeTableNode;
import java.awt.event.ActionEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.tree.TreePath;
import org.netbeans.modules.tasklist.usertasks.table.UTTreeTableNode;
import org.netbeans.modules.tasklist.usertasks.UserTaskView;
import org.netbeans.modules.tasklist.usertasks.model.UserTask;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;

/**
 * Delete all tasks marked completed
 *
 * @author Tor Norbye 
 * @author tl
 */
public class PurgeTasksAction extends UTViewAction {
    private static final long serialVersionUID = 2;

    /**
     * Constructor.
     * 
     * @param utv view associated with this action 
     */
    public PurgeTasksAction(UserTaskView utv) {
        super(utv, NbBundle.getMessage(PurgeTasksAction.class, 
                "LBL_PurgeTodo")); // NOI18N
    }
    
    public void actionPerformed(ActionEvent e) {
        NotifyDescriptor nd = new NotifyDescriptor.Confirmation(
            NbBundle.getMessage(PurgeTasksAction.class, "PurgeTasks"), // NOI18N
            NbBundle.getMessage(PurgeTasksAction.class, "PurgeTasksTitle"), // NOI18N
            NotifyDescriptor.OK_CANCEL_OPTION
        );
        if (DialogDisplayer.getDefault().notify(nd) == NotifyDescriptor.OK_OPTION) {
            List<UserTask> sel = getSelectedTasks();
            UserTask[] uts = sel.toArray(new UserTask[sel.size()]);
            uts = UserTask.reduce(uts);
            for (int i = 0; i < uts.length; i++) {
                uts[i].purgeCompleted();
                if (uts[i].isDone()) {
                    if (uts[i].getParent() != null)
                        uts[i].getParent().getSubtasks().remove(uts[i]);
                    else
                        uts[i].getList().getSubtasks().remove(uts[i]);
                }
            }
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        List<UserTask> sel = getSelectedTasks();
        setEnabled(sel.size() > 0);
    }
}

