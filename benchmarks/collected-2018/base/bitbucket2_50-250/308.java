// https://searchcode.com/api/result/46078212/

//Copyright (C) 2010  Novabit Informationssysteme GmbH
//
//This file is part of Nuclos.
//
//Nuclos is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Nuclos is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Nuclos.  If not, see <http://www.gnu.org/licenses/>.
package org.nuclos.client.ui.tree;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.tree.TreeNode;

import org.nuclos.client.ui.UIUtils;
import org.nuclos.common2.SpringLocaleDelegate;

/**
 * An action in a <code>JTree</code> node.
 * <br>
 * <br>Created by Novabit Informationssysteme GmbH
 * <br>Please visit <a href="http://www.novabit.de">www.novabit.de</a>
 *
 * @author	<a href="mailto:Christoph.Radig@novabit.de">Christoph.Radig</a>
 * @version	01.00.00
 */
public abstract class TreeNodeAction extends AbstractAction {

	public static final String ACTIONCOMMAND_CUT = "CUT";
	public static final String ACTIONCOMMAND_COPY = "COPY";
	public static final String ACTIONCOMMAND_PASTE = "PASTE";
	protected static final String ACTIONCOMMAND_REMOVE = "REMOVE";

	private final JTree tree;

	/**
	 * @param sActionCommand May be <code>null</code>.
	 * @param sName May be <code>null</code>.
	 * @param tree May be <code>null</code>.
	 */
	public TreeNodeAction(String sActionCommand, String sName, JTree tree) {
		super(sName);
		this.tree = tree;
		this.putValue(Action.ACTION_COMMAND_KEY, sActionCommand);
	}

	public JTree getJTree() {
		return tree;
	}

	/**
	 * @return the parent to be used to display dialogs
	 */
	public Component getParent() {
		return this.getJTree().getParent();
	}

	/**
	 * adds this action to the given popup menu.
	 * @param menu
	 * @param bDefault Is this action supposed to be the default action? The font style (BOLD/PLAIN)
	 * will be set accordingly.
	 */
	public void addToMenu(JPopupMenu menu, boolean bDefault) {
		final JMenuItem mi = menu.add(this);

		this.customizeComponent(mi, bDefault);
	}

	/**
	 * adds this action to the given menu.
	 * @param menu
	 * @param bDefault Is this action supposed to be the default action? The font style (BOLD/PLAIN)
	 * will be set accordingly.
	 */
	public void addToMenu(JMenu menu, boolean bDefault) {
		final JMenuItem mi = menu.add(this);

		this.customizeComponent(mi, bDefault);
	}

	/**
	 * sets bold font for default action, plain font for regular actions.
	 * @param comp
	 * @param bDefault
	 */
	protected void customizeComponent(final JComponent comp, boolean bDefault) {
		UIUtils.setFontStyleBold(comp, bDefault);
	}

	/**
	 * creates a <code>TreeNodeAction</code> that acts as a separator in a menu.
	 * @return
	 */
	public static TreeNodeAction newSeparatorAction() {
		return new SeparatorAction();
	}

	public static TreeNodeAction newCutAction(JTree tree) {
		final TreeNodeAction result = new ChainedTreeNodeAction(ACTIONCOMMAND_CUT, 
				SpringLocaleDelegate.getInstance().getMessage("TreeNodeAction.1","Ausschneiden"),
				TransferHandler.getCutAction(), tree);

		// enable "cut" action according to the tree's TransferHandler:
		result.setEnabled((tree.getTransferHandler().getSourceActions(tree) & TransferHandler.MOVE) != 0);

		return result;
	}

	public static TreeNodeAction newCopyAction(JTree tree) {
		final TreeNodeAction result = new ChainedTreeNodeAction(ACTIONCOMMAND_COPY, 
				SpringLocaleDelegate.getInstance().getMessage("TreeNodeAction.2","Kopieren"),
				TransferHandler.getCopyAction(), tree);

		// enable "copy" action according to the tree's TransferHandler:
		result.setEnabled((tree.getTransferHandler().getSourceActions(tree) & TransferHandler.COPY) != 0);

		return result;
	}

	public static ChainedTreeNodeAction newPasteAction(JTree tree, TreeNode node) {
		final ChainedTreeNodeAction result = new ChainedTreeNodeAction(ACTIONCOMMAND_PASTE, 
				SpringLocaleDelegate.getInstance().getMessage("TreeNodeAction.3","Einf\u00fcgen"),
				TransferHandler.getPasteAction(), tree);

		// enable "paste" action according to the tree's TransferHandler:
		// This is hard because TransferHandler.canImport is not called every time the selection changes.
		// Workaround: call canImport anyway to reduce bad paste actions:
		final DataFlavor[] aflavors = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(node).getTransferDataFlavors();
		final boolean bPasteEnabled = tree.getTransferHandler().canImport(tree, aflavors);
		result.setEnabled(bPasteEnabled);

		return result;
	}

	static class NoAction extends TreeNodeAction {

		public NoAction() {
			this(null);
		}

		public NoAction(String sName) {
			super(null, sName, null);
		}

		@Override
		public void actionPerformed(ActionEvent ev) {
			// do nothing
		}
	}

	private static class SeparatorAction extends NoAction {

		@Override
		public void addToMenu(JPopupMenu menu, boolean bDefault) {
			menu.addSeparator();
		}

		@Override
		public void addToMenu(JMenu menu, boolean bDefault) {
			menu.addSeparator();
		}
	}

}  // class TreeNodeAction

