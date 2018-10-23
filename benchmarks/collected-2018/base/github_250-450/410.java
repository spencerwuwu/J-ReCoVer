// https://searchcode.com/api/result/100494933/

package pipe.gui;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import pipe.constants.GUIConstants;
import pipe.controllers.application.PipeApplicationController;
import pipe.gui.plugin.GuiModule;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The ModuleManager class contains methods to create swing components to allow
 * the user to load modules and execute methods within them. To use, instantiate
 * a ModuleManager object and use the methods to return the required components.
 *
 * @author Camilla Clifford
 * @author David Patterson -- minor changes 24 Nov 2006
 * @author Matthew Worthington -- changed the ModuleManger to dynamically load
 *         all class files in the module directory without the need to update the cfg
 *         files and provide path properties. Modules can now be dropped into the module
 *         folder and automatically loaded on all subsequent executions of pipe.
 *         Also refactored to reduce number of methods loaded with reflection into the
 *         Jtree which were subsequently never used. Now only loading the run method of
 *         each of the modules. (Jan,2007)
 * @author Pere Bonet - JAR May 2007
 */
public class ModuleManager {

    /**
     * Load text
     */
    private static final String LOAD_NODE_STRING = "Find IModule";

    /**
     * Class logger
     */
    private static final Logger LOGGER = Logger.getLogger(ModuleManager.class.getName());

    /**
     * PIPE Gui concrete models package
     */
    public static final String PIPE_GUI_PLUGIN_CONCRETE_PACKAGE = "pipe.gui.plugin.concrete";

    /**
     * All modules that have been found in the PIPE_GUI_PLUGIN_CONCRETE_PACKAGE
     */
    private final Set<Class<?>> installedModules;

    /**
     * Main PIPE application controller
     */
    private final PipeApplicationController controller;

    /**
     * Parent of the module loader
     */
    private final Component parent;

    /**
     * Module tree
     */
    private JTree moduleTree;

    /**
     * Tree model
     */
    private DefaultTreeModel treeModel;

    /**
     * Loaded modules
     */
    private DefaultMutableTreeNode loadModules;


    /**
     * Constructor
     * @param view view on which the modules should be displayed
     * @param controller main PIPE appliaction controller
     */
    public ModuleManager(Component view, PipeApplicationController controller) {
        this.controller = controller;

        parent = view;
        installedModules = new HashSet<>();
    }

    public JTree getModuleTree() {

        Collection<Class<? extends GuiModule>> classes = new ArrayList<>();

        // get the names of all the classes that are confirmed to be modules
        Collection<Class<? extends GuiModule>> names = getModuleClasses();
        classes.addAll(names);

        // create the root node
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Analysis Module Manager");

        // create root children
        loadModules = new DefaultMutableTreeNode("Available Modules");

        MutableTreeNode add_modules = new DefaultMutableTreeNode(LOAD_NODE_STRING);

        // iterate over the class names and create a node for each
        for (Class<? extends GuiModule> clazz : classes) {
            // create each ModuleClass node using an instantiation of the
            // ModuleClass
            addClassToTree(clazz);
        }

        root.add(loadModules);
        root.add(add_modules);

        treeModel = new DefaultTreeModel(root);

        moduleTree = new JTree(treeModel);
        moduleTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        moduleTree.addMouseListener(new TreeHandler());

        moduleTree.setFocusable(false);

        // expand the modules path
        moduleTree.expandPath(moduleTree.getPathForRow(1));
        return moduleTree;
    }

    /**
     * Finds all the fully qualified (ie: full package names) module classnames
     * by recursively searching the rootDirectories
     *
     * @return
     */
    //only load attempt to add .class files
    private Collection<Class<? extends GuiModule>> getModuleClasses() {
        Collection<Class<? extends GuiModule>> results = new ArrayList<>();
        try {
            ClassPath classPath = ClassPath.from(this.getClass().getClassLoader());
            ImmutableSet<ClassPath.ClassInfo> set = classPath.getTopLevelClasses(PIPE_GUI_PLUGIN_CONCRETE_PACKAGE);
            for (ClassPath.ClassInfo classInfo : set) {
                Class<?> clazz = classInfo.load();
                if (GuiModule.class.isAssignableFrom(clazz)) {
                    results.add((Class<? extends GuiModule>) clazz);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
        return results;
    }

    /**
     * Method creates and returns a IModule management tree.
     * This consists of two nodes, one resposible for listing all the available
     * modules from the module directory, and another for admin options such as
     * list refreshing.
     * Each node of the tree has it's own user object, for class nodes this will
     * be ModuleClass, for method nodes ModuleMethod, and another one yet to be
     * implemented for other options.
     * When the user clicks on a method node the method is invoked.
     * <p/>
     * Matthew - modified to reduce unnecessary reflection, now only loading
     * the run method of each module class into the tree
     *
     * @param moduleClass
     */
    private void addClassToTree(Class<? extends GuiModule> moduleClass) {
        if (installedModules.add(moduleClass)) {
            DefaultMutableTreeNode modNode = new DefaultMutableTreeNode(new ModuleClassContainer(moduleClass));

            try {
                Method tempMethod = moduleClass.getMethod("start", PetriNet.class);
                ModuleMethod m = new ModuleMethod(moduleClass, tempMethod);
                m.setName(modNode.getUserObject().toString());
                modNode.add(new DefaultMutableTreeNode(m));
            } catch (SecurityException | NoSuchMethodException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }

            if (modNode.getChildCount() == 1) {
                Object m = ((DefaultMutableTreeNode) modNode.getFirstChild()).
                        getUserObject();
                loadModules.add(new DefaultMutableTreeNode(m));
            } else {
                loadModules.add(modNode);
            }
        }
    }

    /**
     * Removes a node from the IModule subtree
     *
     * @param newNode The node to be removed.
     */
    private void removeModuleFromTree(MutableTreeNode newNode) {
        treeModel.removeNodeFromParent(newNode);
        treeModel.reload();
    }


    /**
     * Action object that can be used to remove a module from the ModuleTree
     */
    class RemoveModuleAction extends AbstractAction {
        private final DefaultMutableTreeNode removeNode;

        RemoveModuleAction(TreePath path) {
            removeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object o = removeNode.getUserObject();

            if (o instanceof ModuleMethod) {
                installedModules.remove(((ModuleMethod) o).getModClass());
            } else if (o instanceof ModuleClassContainer) {
                installedModules.remove(((ModuleClassContainer) o).returnClass());
            } else {
                LOGGER.log(Level.INFO, "Don't know how to delete class for " + o.getClass());
            }
            removeModuleFromTree(removeNode);
            moduleTree.expandPath(moduleTree.getPathForRow(1));
        }
    }


    // now add in the action listener to enable module method loading.
    public class TreeHandler extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            int selRow = moduleTree.getRowForLocation(e.getX(), e.getY());
            TreePath selPath = moduleTree.getPathForLocation(e.getX(), e.getY());

            if (selRow != -1) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                Object nodeObj = node.getUserObject();

                if (e.getClickCount() == 2) {
                    if (nodeObj instanceof ModuleMethod) {

                        PetriNet petriNet = controller.getActivePetriNetController().getPetriNet();
                        ((ModuleMethod) nodeObj).execute(petriNet);
                    } else if (nodeObj.equals(LOAD_NODE_STRING)) {

                        //Create a file chooser
                        JFileChooser fc = new JFileChooser();
                        fc.setFileFilter(new ExtensionFilter(GUIConstants.PROPERTY_FILE_EXTENSION,
                                        GUIConstants.PROPERTY_FILE_DESC)
                        );
                        //In response to a button click:
                        int returnVal = fc.showOpenDialog(parent);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            File moduleProp = fc.getSelectedFile();
                            Class<?> newModuleClass = ModuleLoader.importModule(moduleProp);

                            if (newModuleClass != null) {
                                //TODO
                                //                                addClassToTree(newModuleClass);
                                treeModel.reload();
                                moduleTree.expandPath(moduleTree.getPathForRow(1));
                            } else {
                                JOptionPane.showMessageDialog(parent, "Invalid file selected.\n Please ensure the "
                                                + "class implements the IModule interface and is"
                                                + " on the CLASSPATH.", "File Selection Error",
                                        JOptionPane.ERROR_MESSAGE
                                );
                            }
                        }
                    }
                }
            }
        }
        /**
         * Show the menu popup to run the module
         * @param e
         */
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopupMenu(e);
            }
        }

        /**
         * Show the menu popup to run the module
         * @param e
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopupMenu(e);
            }
        }

        /**
         * Show the menu for the modules
         * @param e
         */
        private void showPopupMenu(MouseEvent e) {
            TreePath selPath = moduleTree.getPathForLocation(e.getX(), e.getY());

            if (selPath != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                Object nodeObj = node.getUserObject();

                if ((nodeObj instanceof ModuleClassContainer) || (nodeObj instanceof ModuleMethod)) {
                    JPopupMenu popup = new JPopupMenu();
                    TreePath removePath = moduleTree.getPathForLocation(e.getX(), e.getY());
                    JMenuItem menuItem = new JMenuItem(new RemoveModuleAction(removePath));
                    menuItem.setText("Remove Module");
                    popup.add(menuItem);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
    }

}

