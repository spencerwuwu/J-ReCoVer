// https://searchcode.com/api/result/46079139/

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
package org.nuclos.client.explorer;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.commons.lang.ObjectUtils;
import org.nuclos.client.command.CommonClientWorkerAdapter;
import org.nuclos.client.common.ClientParameterProvider;
import org.nuclos.client.common.MetaProvider;
import org.nuclos.client.common.NuclosCollectController;
import org.nuclos.client.common.NuclosCollectControllerFactory;
import org.nuclos.client.datasource.DatasourceDelegate;
import org.nuclos.client.entityobject.EntityObjectDelegate;
import org.nuclos.client.explorer.ExplorerSettings.FolderNodeAction;
import org.nuclos.client.explorer.ExplorerSettings.ObjectNodeAction;
import org.nuclos.client.genericobject.GenerationController;
import org.nuclos.client.main.Main;
import org.nuclos.client.main.MainController;
import org.nuclos.client.main.mainframe.MainFrame;
import org.nuclos.client.main.mainframe.MainFrameTab;
import org.nuclos.client.main.mainframe.MainFrameTabbedPane;
import org.nuclos.client.ui.CommonMultiThreader;
import org.nuclos.client.ui.Errors;
import org.nuclos.client.ui.UIUtils;
import org.nuclos.client.ui.tree.ChainedTreeNodeAction;
import org.nuclos.client.ui.tree.TreeNodeAction;
import org.nuclos.common.E;
import org.nuclos.common.EntityMeta;
import org.nuclos.common.ParameterProvider;
import org.nuclos.common.SpringApplicationContextHolder;
import org.nuclos.common.UID;
import org.nuclos.common.UsageCriteria;
import org.nuclos.common.collection.CollectionUtils;
import org.nuclos.common.collection.multimap.MultiListHashMap;
import org.nuclos.common.collection.multimap.MultiListMap;
import org.nuclos.common.dal.vo.EntityObjectVO;
import org.nuclos.common.preferences.IExplorerTreeNodeState;
import org.nuclos.common.report.valueobject.DynamicEntityVO;
import org.nuclos.common2.CommonRunnable;
import org.nuclos.common2.IOUtils;
import org.nuclos.common2.IdUtils;
import org.nuclos.common2.LangUtils;
import org.nuclos.common2.SpringLocaleDelegate;
import org.nuclos.common2.StringUtils;
import org.nuclos.common2.SystemUtils;
import org.nuclos.common2.exception.CommonBusinessException;
import org.nuclos.common2.exception.CommonFinderException;
import org.nuclos.common2.exception.CommonIOException;
import org.nuclos.common2.exception.CommonPermissionException;
import org.nuclos.server.genericobject.valueobject.GeneratorActionVO;
import org.nuclos.server.navigation.treenode.AbstractTreeNodeConfigured;
import org.nuclos.server.navigation.treenode.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A node in the explorer tree.
 * <br>
 * <br>Created by Novabit Informationssysteme GmbH
 * <br>Please visit <a href="http://www.novabit.de">www.novabit.de</a>
 *
 * @author	<a href="mailto:Christoph.Radig@novabit.de">Christoph.Radig</a>
 * @version 01.00.00
 */
public class ExplorerNode<TN extends TreeNode> extends DefaultMutableTreeNode {

	public static final Logger LOG = LoggerFactory.getLogger(ExplorerNode.class);

	public static final String ACTIONCOMMAND_SHOW_DETAILS = "SHOW DETAILS";
	public static final String ACTIONCOMMAND_SHOW_DETAILS_IN_NEW_TAB = "SHOW DETAILS IN NEW TAB";
	public static final String ACTIONCOMMAND_COPY = "COPY";
	public static final String ACTIONCOMMAND_PASTE = "PASTE";
	public static final String ACTIONCOMMAND_REMOVE = "REMOVE";
	public static final String ACTIONCOMMAND_SHOW_IN_OWN_TAB = "SHOW_IN_OWN_TAB";
	public static final String ACTIONCOMMAND_REFRESH = "REFRESH";
	public static final String ACTIONCOMMAND_EXPAND = "EXPAND";
	public static final String ACTIONCOMMAND_COLLAPSE = "COLLAPSE";
	public static final String ACTIONCOMMAND_SHOW_IN_LIST = "SHOW_IN_LIST";

	/**
	 * action: generate genericobject
	 */
	private static final String ACTIONCOMMAND_GENERATE_GENERICOBJECT = "GENERATE GENERICOBJECT";

	/**
	 * Have the children of this node already been loaded?
	 */
	private boolean bChildrenHaveBeenLoaded;
	
	private MainController mainController;
	
	// former Spring injection
	
	private transient SpringLocaleDelegate localeDelegate;
	
	// end of former Spring injection

	/**
	 * Use <code>ExplorerNodeFactory.newExplorerNode()</code> to create <code>ExplorerNode</code>s
	 * postcondition !this.getChildrenHaveBeenLoaded()
	 * @param treenode
	 */
	protected ExplorerNode(TreeNode treenode) {
		super(treenode, treenode instanceof AbstractTreeNodeConfigured ? ((AbstractTreeNodeConfigured<?>)treenode).allowsChildren() : true);
		
		setSpringLocaleDelegate(SpringApplicationContextHolder.getBean(SpringLocaleDelegate.class));
	}
	
	/*
	 * Maven don't like this.
	 * {@link org.springframework.beans.factory.aspectj.AbstractInterfaceDrivenDependencyInjectionAspect}.
	public Object readResolve() throws ObjectStreamException {
		setSpringLocaleDelegate(SpringLocaleDelegate.getInstance());
		return this;
	}
	 */
	
	protected MainController getMainController() {
		if (mainController == null) {
			mainController = Main.getInstance().getMainController();
		}
		return mainController;
	}
	
	final void setSpringLocaleDelegate(SpringLocaleDelegate cld) {
		this.localeDelegate = cld;
	}
	
	protected final SpringLocaleDelegate getSpringLocaleDelegate() {
		return localeDelegate;
	}

	/**
	 * @return the <code>TreeNode</code> object contained (as user object) in this node.
	 */
	public TN getTreeNode() {
		return (TN) this.getUserObject();
	}

	/**
	 * precondition treenode != null
	 */
	private void setTreeNode(TN treenode) {
		this.setUserObject(treenode);
	}

	/**
	 * postcondition result != null
	 * 
	 * @return the <code>ExplorerController</code> that controls this node.
	 */
	protected ExplorerController getExplorerController() {
		return getMainController().getExplorerController();
	}

	@Override
	public boolean isLeaf() {
		final Boolean bHasSubNodes = this.getTreeNode().hasSubNodes();
		return bHasSubNodes == null ? false : !bHasSubNodes.booleanValue();
	}
	
	@Override
	public int getChildCount() {
		return super.getChildCount();
	}

	/**
	 * Note that this method is (and should be!) private.
	 * @return Have the children of this node already been loaded?
	 */
	private boolean getChildrenHaveBeenLoaded() {
		return this.bChildrenHaveBeenLoaded || (this.getChildCount() > 0);
	}

	/**
	 * loads the children of this node, if they haven't been loaded yet.
	 * 
	 * postcondition this.getChildrenHaveBeenLoaded()
	 * 
	 * @param bLoadAllAvailableSubNodes Load all available subnodes (recursively)?
	 */
	public void loadChildren(boolean bLoadAllAvailableSubNodes) {
		if (!this.getChildrenHaveBeenLoaded()) {
			this.bChildrenHaveBeenLoaded = true;
			
			List<? extends TreeNode> lstSubNodes = this.getTreeNode().getSubNodes();
			if (lstSubNodes == null )
				return;
			
			LOG.debug("START loadChildren");
			for (TreeNode treenodeChild : lstSubNodes) {
				final ExplorerNode<TreeNode> explorernodeChild = (ExplorerNode<TreeNode>) ExplorerNodeFactory.getInstance().newExplorerNode(treenodeChild, false);
				this.add(explorernodeChild);
				if (bLoadAllAvailableSubNodes && (treenodeChild.hasSubNodes() != null)) {
					explorernodeChild.loadChildren(bLoadAllAvailableSubNodes);
				}
			}
			LOG.debug("FINISHED loadChildren");
		}
		assert this.getChildrenHaveBeenLoaded();
	}

	/**
	 * unloads the children of this node, if they have been loaded before.
	 * postcondition !this.getChildrenHaveBeenLoaded()
	 */
	public void unloadChildren() {
		if (this.getChildrenHaveBeenLoaded()) {
			this.removeAllChildren();
			this.bChildrenHaveBeenLoaded = false;
		}
		assert !this.getChildrenHaveBeenLoaded();
	}

	/**
	 * refreshes the current node (and its children) and notifies the given tree model
	 * 
	 * @param tree the DefaultTreeModel to notify. Must contain this node.
	 * @throws CommonFinderException if the object presented by this node no longer exists.
	 */
	public void refresh(final JTree tree) throws CommonFinderException {
		refresh(tree, false);
	}

	/**
	 * refreshes the current node (and its children) and notifies the given tree model
	 * 
	 * @param tree the DefaultTreeModel to notify. Must contain this node.
	 * @throws CommonFinderException if the object presented by this node no longer exists.
	 */
	public void refresh(final JTree tree, boolean fullRefreshCurrent) throws CommonFinderException {
		List<String> lstExpandedPathsResult = new ArrayList<String>();
		ExplorerNode.createExpandendPathsForTree(new TreePath(tree.getModel().getRoot()), tree, lstExpandedPathsResult);

		final TreePath[] lstSelected = tree.getSelectionPaths();
		DefaultTreeModel dtm = (DefaultTreeModel) tree.getModel();
		unloadChildren();

		final TN treenode = getTreeNode();
		if (treenode.implementsNewRefreshMethod()) {
			TN refreshed = null;
			if(fullRefreshCurrent && !this.isRoot()){
				TreeNode parentTreeNode = ((ExplorerNode<TN>)this.getParent()).getTreeNode();
				parentTreeNode.removeSubNodes();
				List<? extends TreeNode> parentSubNodes = parentTreeNode.getSubNodes();
				for(TreeNode parentSubNode : parentSubNodes){
					if(ObjectUtils.equals(parentSubNode.getId(), treenode.getId()))
						refreshed = (TN) parentSubNode;
				}
				if(refreshed == null){
					this.removeFromParent();
					dtm.nodeStructureChanged(this);
					return;
				}
			} else {
				refreshed = (TN) treenode.refreshed();
			}
			setTreeNode(refreshed);
		}
		else {
			treenode.refresh();
		}
		treenode.removeSubNodes();
		loadChildren(true);

		assert getChildrenHaveBeenLoaded();

		dtm.nodeStructureChanged(this);

		ExplorerNode.expandTreeAsync(lstExpandedPathsResult, tree);

		if (lstSelected != null) 
		{
			List<List<Object>> lstPathEssence = new ArrayList<List<Object>>();
			for (TreePath selected : lstSelected) {
				List<Object> pathEssence = CollectionUtils.asList(selected.getPath());
				Collections.reverse(pathEssence);
				if(pathEssence.size() > 1) {
					lstPathEssence.add(pathEssence.subList(0, pathEssence.size() - 1));
				}
			}
			
			boolean first = true;
			for (List<Object> pathEssence : lstPathEssence) {
				new Thread(new TreeExpander(tree, pathEssence, first)).start();		
				first = false;
				try {
					Thread.sleep(25L);					
				} catch (InterruptedException ie) {
					LOG.warn(ie.getMessage(), ie);
				}
			}
		}
	}

	private class TreeExpander implements TreeExpansionListener, Runnable {
		private JTree        tree;
		private List<Object> toExpand;
		private TreePath     expanded;
	    private boolean      listener;
	    private boolean first;

	    private TreeExpander(JTree tree, List<Object> toExpand, boolean first) {
	    	this.tree = tree;
	    	this.toExpand = toExpand;
	    	this.expanded = new TreePath(tree.getModel().getRoot());
	    }

	    @Override
        public void run() {
	    	if(toExpand.isEmpty())
	    		return;

	    	if(tree.isExpanded(expanded)) {
	    		SwingUtilities.invokeLater(new Runnable() {
	    			@Override
	    			public void run() {
	    				treeExpanded(new TreeExpansionEvent(tree, expanded));
	    			}});
	    	}
	    	else {
	    		tree.addTreeExpansionListener(TreeExpander.this);
	    		listener = true;
	    		tree.expandPath(expanded);
	    	}
	    }

		@Override
		public void treeExpanded(TreeExpansionEvent event) {
			TreePath eventPath = event.getPath();
			if(eventPath.equals(expanded)) {
				if(listener)
					tree.removeTreeExpansionListener(TreeExpander.this);

				ExplorerNode<?> last   = (ExplorerNode<?>) expanded.getLastPathComponent();
				ExplorerNode<?> expand = (ExplorerNode<?>) toExpand.get(toExpand.size() - 1);
				ExplorerNode<?> child  = null;

				for(int i=0; i<last.getChildCount(); i++) {
					ExplorerNode<?> childNode = (ExplorerNode<?>) last.getChildAt(i);
					if(childNode.getTreeNode().equals(expand.getTreeNode())) {
						child = childNode;
						break;
					}
				}

				if(child != null) {
					expanded = expanded.pathByAddingChild(child);
					toExpand = toExpand.subList(0, toExpand.size() - 1);
					if(toExpand.isEmpty()) {
						if (first) {
							tree.setSelectionPath(expanded);							
						} else {
							tree.addSelectionPath(expanded);
						}
						tree.scrollPathToVisible(expanded);
						//tree.expandPath(expanded);
					}
					else
						new Thread(TreeExpander.this).start();
				}
			}
		}

		@Override
        public void treeCollapsed(TreeExpansionEvent event) {}
	}

	public void handleKeyEvent(JTree tree, KeyEvent ev) {}

	/**
	 * @param tree the tree where the action is about to take place.
	 * @return the list of possible <code>TreeNodeAction</code>s for this node.
	 * These may be shown in the node's context menu.
	 * Separators are shown in the menus for <code>null</code> entries.
	 */
	public List<TreeNodeAction> getTreeNodeActions(JTree tree) {
		final List<TreeNodeAction> result = new LinkedList<TreeNodeAction>();

		result.add(new RefreshAction(tree));
		final ShowInOwnTabAction actShowInOwnTab = new ShowInOwnTabAction(tree);
		actShowInOwnTab.setEnabled(!this.getTreeNode().needsParent());
		result.add(actShowInOwnTab);

		result.addAll(getExpandCollapseActions(tree));

		final TreeNodeAction exploreractCopy = new ChainedTreeNodeAction(ACTIONCOMMAND_COPY, 
				getSpringLocaleDelegate().getMessage("ExplorerNode.4","Kopieren"),
				TransferHandler.getCopyAction(), tree);
		result.add(exploreractCopy);

		final TreeNodeAction exploreractPaste = new ChainedTreeNodeAction(ACTIONCOMMAND_PASTE, 
				getSpringLocaleDelegate().getMessage("ExplorerNode.2","Einf\u00fcgen"),
				TransferHandler.getPasteAction(), tree);
		result.add(exploreractPaste);

		// enable "copy" action according to the tree's TransferHandler:
		final boolean bCopyEnabled = (tree.getTransferHandler().getSourceActions(tree) & TransferHandler.COPY) != 0;
		exploreractCopy.setEnabled(bCopyEnabled);

		// enable "paste" action according to the tree's TransferHandler:
		// This is hard because TransferHandler.canImport is not called every time the selection changes.
		// Workaround: call canImport anyway to reduce bad paste actions:
		final DataFlavor[] aflavors = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this).getTransferDataFlavors();
		final boolean bPasteEnabled = tree.getTransferHandler().canImport(tree, aflavors);
		exploreractPaste.setEnabled(bPasteEnabled);

		return result;
	}

	/**
	 *
	 * @param jTree
	 * @return additional tool bar components
	 */
	public List<JComponent> getToolBarComponents(JTree jTree) {
		return null;
	}

	/**
	 * get actions for expand and collapse the children of this node.
	 * if this node is a leaf return an empty list
	 * @param tree
	 * @return
	 */
	protected List<TreeNodeAction> getExpandCollapseActions(JTree tree) {
		final List<TreeNodeAction> result;
//		if(this.getChildCount() > 0) {
		result = new ArrayList<TreeNodeAction>();
		result.add(TreeNodeAction.newSeparatorAction());
		result.add(new ExpandAction(tree));
		result.add(new CollapseAction(tree));
		result.add(TreeNodeAction.newSeparatorAction());
//		} else {
//			result = Collections.emptyList();
//		}
		return result;
	}

	/**
	 * There is no default action by default. Subclasses may specify the default action here.
	 * @param tree
	 * @return the action command of the default <code>TreeNodeAction</code> for this node, if any.
	 */
	public String getDefaultTreeNodeActionCommand(JTree tree) {
		return null;
	}

	/**
	 * Action that is performed when the user makes a simple mouse click on a tree element
	 * This method can be not overridden by subclasses, but its typically not used in most of the TreeViews.
	 * @param tree
	 * @return null
	 */
	public Action getTreeNodeActionOnMouseClick(JTree tree) {
		return null;
	}
	
	/**
	 * finds the default action for this node by searching the tree node actions
	 * (as specified in <code>getTreeNodeActions()</code>) for an enabled tree node action with the default tree node action
	 * command (as specified in <code>getDefaultTreeNodeActionCommand()</code>). Note that this method
	 * doesn't have to be overridden in subclasses.
	 * 
	 * postcondition result != null --&gt; result.isEnabled()
	 * 
	 * @param tree the tree where the action is about to take place.
	 * @return the default action for this node, if any.
	 */
	public TreeNodeAction getDefaultTreeNodeAction(JTree tree) {
		TreeNodeAction result = null;

		final String sDefaultTreeActionCommmand = this.getDefaultTreeNodeActionCommand(tree);
		if (sDefaultTreeActionCommmand != null) {
			for (TreeNodeAction action : this.getTreeNodeActions(tree)) {
				if (action.isEnabled() && LangUtils.equal(sDefaultTreeActionCommmand, action.getValue(Action.ACTION_COMMAND_KEY)))
				{
					result = action;
					break;
				}
			}
		}
		assert result == null || result.isEnabled();
		return result;
	}

	protected TreeNodeAction newShowDetailsAction(JTree tree, boolean newTab) {
		String command = newTab ? ACTIONCOMMAND_SHOW_DETAILS_IN_NEW_TAB : ACTIONCOMMAND_SHOW_DETAILS;
		String resource = newTab ? "ExplorerSettings.ObjectNodeAction.ShowDetailsInNewTab" : "RuleExplorerNode.1";
		final TreeNodeAction result = new ShowDetailsAction(command, resource, tree, newTab);
		return result;
	}

	protected TreeNodeAction newShowListAction(JTree tree) {
		final TreeNodeAction result = new ShowListAction(tree);
		/*boolean enabled = CollectionUtils.applyFilter(getTreeNode().getSubNodes(), new Predicate<TreeNode>() {
			@Override
			public boolean evaluate(TreeNode t) {
				return !StringUtils.isNullOrEmpty(t.getEntityName()) && t.getId() != null;
			}
		}).size() > 0;*/
		// performance issue. if subnodes are not loaded yet. deaktivate this action.
		boolean enabled = getTreeNode().hasSubNodes() == null ? false : getTreeNode().hasSubNodes();
		result.setEnabled(enabled);
		return result;
	}

	/**
	 * @return the label to display for this node
	 */
	public String getLabel() {
		return StringUtils.emptyIfNull(this.getTreeNode().getLabel());
	}

	/**
	 * @return the label to display if this node is a root (if it is displayed in a new tab)
	 */
	public String getLabelForRoot() {
		return this.getLabel();
	}

	/**
	 * @return the text to display for this node
	 */
	@Override
	public String toString() {
		return this.isRoot() ? this.getLabelForRoot() : this.getLabel();
	}

	/**
	 * @return the icon to display for the node, or <code>null</code> for the default icon,
	 * specified by the look&amp;feel.
	 */
	public Icon getIcon() {
		// use default icon by default ;-)
		
		return null;
	}

	/**
	 * source actions for drag&amp;drop.
	 * @see DnDConstants
	 * @return (default: DnDConstants.ACTION_NONE)
	 */
	public int getDataTransferSourceActions() {
		return DnDConstants.ACTION_NONE;
	}

	/**
	 * @return a <code>Transferable</code> for data transfer (default: null)
	 */
	public Transferable createTransferable(JTree tree) {
		return null;
	}

	/**
	 * imports data from a drop or paste operation. The default implementation throws an
	 * UnsupportedFlavorException to indicate that drag&amp;drop is not supported by default.
	 * 
	 * @see javax.swing.TransferHandler#importData
	 * @param transferable
	 * @param tree
	 * @return Was the data imported? <code>false</code> may be returned if the drop action requires
	 * additional user input, and the user cancels the operation.
	 * @throws UnsupportedFlavorException
	 */
	public boolean importTransferData(Component parent, Transferable transferable, JTree tree) throws IOException,
			UnsupportedFlavorException {
		throw new UnsupportedFlavorException(null);
	}

	/**
	 * expand all children of this node
	 * @param tree
	 */
	public void expandAllChildren(final JTree tree) {
		for (int i = getChildCount() - 1; i >= 0; i--) {
			final TreePath treePath = new TreePath((((DefaultMutableTreeNode) ExplorerNode.this.getChildAt(i))).getPath());
			if (!tree.isExpanded(treePath)) {
				tree.expandPath(treePath);
			}
		}
	}

	/**
	 * collapse all children of this node
	 * @param tree
	 */
	public void collapseAllChildren(JTree tree) {
		for (int i = getChildCount() - 1; i >= 0; i--) {
			tree.collapsePath(new TreePath(((DefaultMutableTreeNode) this.getChildAt(i)).getPath()));
		}
	}

	/**
	 * Action: Show the current node in its own tab.
	 */
	protected class ShowInOwnTabAction extends TreeNodeAction {

		public ShowInOwnTabAction(JTree tree) {
			super(ACTIONCOMMAND_SHOW_IN_OWN_TAB, 
					getSpringLocaleDelegate().getMessage("ExplorerNode.3","In eigenem Reiter anzeigen"), tree);
		}

		@Override
        public void actionPerformed(ActionEvent ev) {
			cmdShowInOwnTabAction();
		}
	}	// inner class ShowInOwnTabAction

	protected void cmdShowInOwnTabAction() {
		getExplorerController().cmdShowInOwnTab(ExplorerNode.this.getTreeNode());
	}

	/**
	 * Action: Refresh the descendants of the current node
	 */
	protected class RefreshAction extends TreeNodeAction {

		public RefreshAction(JTree tree) {
			super(ACTIONCOMMAND_REFRESH, 
					getSpringLocaleDelegate().getInstance().getMessage("ExplorerNode.1","Aktualisieren"), tree);
		}

		@Override
        public void actionPerformed(ActionEvent ev) {
			UIUtils.runCommandForTabbedPane(getExplorerController().getTabbedPane(), new CommonRunnable() {
				@Override
                public void run() throws CommonFinderException {
					ExplorerNode.this.refresh(RefreshAction.this.getJTree());

					// To be sure to correlate the label of the tab also when changed...
					if (ExplorerNode.this.isRoot()) {
						ExplorerNode.this.getExplorerController().refreshNode(ExplorerNode.this.getTreeNode());
					}
				}
			});
		}
	}	// inner class RefreshAction

	/**
	 * Action: Expand all Subnodes
	 */
	protected class ExpandAction extends TreeNodeAction {

		public ExpandAction(JTree tree) {
			super(ACTIONCOMMAND_EXPAND, 
					getSpringLocaleDelegate().getInstance().getMessage("ExplorerNode.5","Unterelemente aufklappen"), tree);
		}

		@Override
        public void actionPerformed(ActionEvent ev) {
			UIUtils.runCommandForTabbedPane(getExplorerController().getTabbedPane(), new Runnable() {
				@Override
                public void run() {
					final TreePath treePath = new TreePath(ExplorerNode.this);
					if (!ExpandAction.this.getJTree().isExpanded(treePath)) {
						ExpandAction.this.getJTree().expandPath(treePath);
					}

					ExplorerNode.this.expandAllChildren(ExpandAction.this.getJTree());
				}
			});
		}
	}	// inner class ExpandAction

	/**
	 * Action: Collapse all Subnodes
	 */
	protected class CollapseAction extends TreeNodeAction {

		public CollapseAction(JTree tree) {
			super(ACTIONCOMMAND_COLLAPSE, 
					getSpringLocaleDelegate().getMessage("ExplorerNode.6","Unterelemente zuklappen"), tree);
		}

		@Override
        public void actionPerformed(ActionEvent ev) {
			UIUtils.runCommandForTabbedPane(getExplorerController().getTabbedPane(), new Runnable() {
				@Override
                public void run() {
					ExplorerNode.this.collapseAllChildren(CollapseAction.this.getJTree());
				}
			});
		}
	}	// inner class CollapseAction

	public String getIdentifierPath() {
		StringBuilder sb = new StringBuilder();
		for (javax.swing.tree.TreeNode tn : getPath()) {
			sb.append('/');
			sb.append(((ExplorerNode<?>) tn).getTreeNode().getIdentifier());
		}
		return sb.toString().substring(1);
	}

	public static TreePath findDescendant(DefaultTreeModel model, String idPath) {
		String[] idComponent = idPath.split("/");
		ExplorerNode<?> root = (ExplorerNode<?>) model.getRoot();
		if (idComponent.length == 0 || !idComponent[0].equals(root.getTreeNode().getIdentifier()))
			return null;

		ExplorerNode<?>[] path = new ExplorerNode<?>[idComponent.length];
		path[0] = root;
		for (int i = 1; i < path.length; i++) {
			ExplorerNode<?> child = path[i-1].findChildNodeWithIdentifier(idComponent[i]);
			if (child == null)
				return null;
			path[i] = child;
		}

		return new TreePath(path);
	}

	public ExplorerNode<?> findChildNodeWithIdentifier(String id) {
		this.loadChildren(false);
		int childCount = this.getChildCount();
		for (int i = 0; i < childCount; i++) {
			ExplorerNode<?> childNode = (ExplorerNode<?>) this.getChildAt(i);
			if (id.equals(childNode.getTreeNode().getIdentifier()))
				return childNode;
		}
		return null;
	}


	// --- expanded paths ---> collect paths (e.g. for storing in preferences) ; expand stored paths asynchronous


	static void createExpandendPathsForTree(TreePath path, JTree tree, List<String> lstExpandedPathsResult) {
		final ExplorerNode<?> explorernode = (ExplorerNode<?>) path.getLastPathComponent();
		boolean isExpanded = tree.isExpanded(path);
		if (isExpanded) {
			lstExpandedPathsResult.add(explorernode.getIdentifierPath());

			for (int i = 0; i < explorernode.getChildCount(); i++) {
				final ExplorerNode<?> explorernodeChild = (ExplorerNode<?>) explorernode.getChildAt(i);
				createExpandendPathsForTree(path.pathByAddingChild(explorernodeChild), tree, lstExpandedPathsResult);
			}
		}
	}

	static void expandAndSelectTreeAsync(IExplorerTreeNodeState state, final JTree tree) {
		expandTreeAsync(state.getLstExpandedPaths(), tree);
		selectTreeNodeAsync(state.getSelectedRows(), tree);
	}

	static void expandTreeAsync(List<String> lstExpandedPaths, final JTree tree) {
		for (final String idPath : lstExpandedPaths) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
					TreePath path = ExplorerNode.findDescendant(model, idPath);
					if (path != null)
						tree.expandPath(path);
				}
			});
		}
	}
	
	static void selectTreeNodeAsync(final int[] selectedRow, final JTree tree) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				tree.setSelectionRows(selectedRow);
			}
		});			
	}

	protected String getDefaultObjectNodeAction() {
		ExplorerSettings settings = ExplorerSettings.getInstance();
		if (ObjectNodeAction.SHOW_LIST == settings.getObjectNodeAction()) {
			return ACTIONCOMMAND_SHOW_IN_LIST;
		}
		else if (ObjectNodeAction.SHOW_DETAILS == settings.getObjectNodeAction()) {
			return ACTIONCOMMAND_SHOW_DETAILS;
		}
		else if (ObjectNodeAction.SHOW_DETAILS_IN_NEW_TAB == settings.getObjectNodeAction()) {
			return ACTIONCOMMAND_SHOW_DETAILS_IN_NEW_TAB;
		}
		else {
			// Fallback
			return ACTIONCOMMAND_SHOW_DETAILS;
		}
	}

	protected String getDefaultFolderNodeAction() {
		ExplorerSettings settings = ExplorerSettings.getInstance();
		if (FolderNodeAction.SHOW_LIST == settings.getFolderNodeAction()) {
			return ACTIONCOMMAND_SHOW_IN_LIST;
		}
		else if (FolderNodeAction.EXPAND_SUBNODES == settings.getFolderNodeAction()) {
			return ACTIONCOMMAND_EXPAND;
		}
		else {
			// Fallback
			return ACTIONCOMMAND_EXPAND;
		}
	}

	/**
	 * Action: Shows the details for a object.
	 */
	protected class ShowDetailsAction extends TreeNodeAction {

		private final boolean newTab;

		protected ShowDetailsAction(String command, String labelResourceId, JTree tree, boolean newTab) {
			super(command, 
					getSpringLocaleDelegate().getText(labelResourceId), tree);
			this.newTab = newTab;
		}

		@Override
		public boolean isEnabled() {
			try {
				if (!super.isEnabled()) {
					return false;
				}
				UID uidEntity = getTreeNode().getEntityUID();
				EntityMeta<?> metaEntity = MetaProvider.getInstance().getEntity(uidEntity);
				if (metaEntity.isDynamic()) {
					final DynamicEntityVO dynEntity = DatasourceDelegate.getInstance().getDynamicEntity(metaEntity.getDataSource());
					uidEntity = dynEntity.getEntityUID();
					return getExplorerController().isDisplayable(uidEntity);
				} else {
					if (getTreeNode() != null && uidEntity != null && getTreeNode().getId() != null) {
						return getExplorerController().isDisplayable(uidEntity);
					}
					else {
						return false;
					}
				}
			} catch (CommonPermissionException e) {
				return false;
			}
		}

		@Override
		public void actionPerformed(ActionEvent ev) {
			UIUtils.runCommand(this.getParent(), new CommonRunnable() {
				@Override
				public void run() throws CommonBusinessException {
					UID uidEntity = getTreeNode().getEntityUID();
					if(E.GENERALSEARCHDOCUMENT.getUID().equals(uidEntity)) {
						handleShowDetailOfDocument(uidEntity);
					} else {
						EntityMeta<?> metaEntity = MetaProvider.getInstance().getEntity(uidEntity);
						if (metaEntity.isDynamic()) {
							final DynamicEntityVO dynEntity = DatasourceDelegate.getInstance().getDynamicEntity(metaEntity.getDataSource());
							uidEntity = dynEntity.getEntityUID();
						
						}
						getMainController().showDetails(uidEntity, getTreeNode().getId(), newTab, null);
						

					}
				}
			});
		}
		
		public void handleShowDetailOfDocument(final UID uidEntity) throws CommonIOException, CommonPermissionException {
			final EntityObjectVO eoVO = EntityObjectDelegate.getInstance().get(uidEntity, IdUtils.toLongId(getTreeNode().getId()));
			try {	
				final org.nuclos.common2.File file1 = (org.nuclos.common2.File) eoVO.getFieldValue(E.GENERALSEARCHDOCUMENT.documentfile.getUID());
				if (null != file1) {
					final byte[] abContents = file1.getContents();
					if (abContents == null) {
						throw new IOException(SpringLocaleDelegate.getInstance().getMessage(
								"CollectableDocumentFileChooserBase.3","Die Datei ist leer."));
					}
					String sFileName = file1.getFilename();
					sFileName = sFileName.replaceAll("\\s", "");
					final java.io.File file = File.createTempFile("tmp", sFileName);
					LOG.debug("Schreibe Dokument in tempor\u00e4re Datei " + file.getAbsolutePath() + ".");
					IOUtils.writeToBinaryFile(file, abContents);
					LOG.debug("Schreiben der tempor\u00e4ren Datei war erfolgreich.");
					file.deleteOnExit();
					SystemUtils.open(file);
				}
			} catch (IOException ex) {
				throw new CommonIOException(ex);
			}
		}
	}	// inner class ShowDetailsAction

	/**
	 * Action: Show subnodes in lists
	 */
	protected class ShowListAction extends TreeNodeAction {

		public ShowListAction(JTree tree) {
			super(ACTIONCOMMAND_SHOW_IN_LIST, 
					getSpringLocaleDelegate().getMessage("ExplorerNode.ShowList", "Unterelemente in Liste anzeigen"), tree);
		}

		@Override
        public void actionPerformed(ActionEvent ev) {
			UIUtils.runCommandForTabbedPane(getExplorerController().getTabbedPane(), new Runnable() {
				@Override
                public void run() {
					MultiListMap<UID, Object> toOpen = new MultiListHashMap<UID, Object>();
					for (TreeNode node : ExplorerNode.this.getTreeNode().getSubNodes()) {
						if (node.getId() != null && node.getEntityUID() != null) {
							toOpen.addValue(node.getEntityUID(), node.getId());
						}
					}

					for (UID entity : toOpen.keySet()) {
						try {
							NuclosCollectController controller = 
									NuclosCollectControllerFactory.getInstance().newCollectController(entity, null, ClientParameterProvider.getInstance().getValue(ParameterProvider.KEY_LAYOUT_CUSTOM_KEY));
							controller.runViewResults(toOpen.getValues(entity));
						}
						catch (CommonBusinessException ex) {
							Errors.getInstance().showExceptionDialog(getExplorerController().getTabbedPane().getComponentPanel(), "StartTabPanel.error.open.list", ex);
						}
					}
				}
			});
		}
	}	// inner class CollapseAction

	/**
	 * inner class GeneratorAction. Removes the relation between this node and its parent.
	 */
	protected class GeneratorAction extends TreeNodeAction {

		private final GeneratorActionVO generatoractionvo;
		private final UsageCriteria usagecriteria;

		public GeneratorAction(JTree tree, GeneratorActionVO generatoractionvo, UsageCriteria critera) {
			super(ACTIONCOMMAND_GENERATE_GENERICOBJECT, generatoractionvo.getLabel() + "...", tree);
			this.generatoractionvo = generatoractionvo;
			this.usagecriteria = critera;
		}

		@Override
		public void actionPerformed(ActionEvent ev) {
			final JTree tree = this.getJTree();

			CommonClientWorkerAdapter searchWorker = 
					new CommonClientWorkerAdapter(null) {
				@Override
				public void init() {
					UIUtils.setWaitCursor(tree);
				}

				@Override
				public void work() throws CommonBusinessException {
					cmdGenerateGenericObject();
				}

				@Override
				public void paint() {
					tree.setCursor(null);
				}

				@Override
				public void handleError(Exception ex) {
					Errors.getInstance().showExceptionDialog(null, 
							getSpringLocaleDelegate().getMessage("GenericObjectExplorerNode.3", "Fehler beim entfernen der Beziehungen"), ex);
				}
			};

			CommonMultiThreader.getInstance().execute(searchWorker);
		}

		/**
		 * removes the relation between this node and its parent.
		 * @param tree
		 * @param goexplorernode
		 */
		private void cmdGenerateGenericObject() {
			try {
				Map<Long, UsageCriteria> sources = new HashMap<Long, UsageCriteria>();
				sources.put(IdUtils.toLongId(getTreeNode().getId()), usagecriteria);

				UID targetEntity = MetaProvider.getInstance().getEntity(generatoractionvo.getTargetModule()).getUID();
				
				MainFrameTabbedPane pane = MainFrame.getHomePane();
				if (MainFrame.isPredefinedEntityOpenLocationSet(targetEntity)) {
					pane = MainFrame.getPredefinedEntityOpenLocation(targetEntity);
				}
				GenerationController controller = new GenerationController(sources, generatoractionvo, null, MainFrameTab.getMainFrameTabForComponent(getJTree()));
				controller.generateGenericObject(null);
			}
			catch (Exception ex) {
				Errors.getInstance().showExceptionDialog(MainFrame.getHomePane().getComponentPanel(), ex);
			}
		}
	}	// inner class GeneratorAction
}	// class ExplorerNode

