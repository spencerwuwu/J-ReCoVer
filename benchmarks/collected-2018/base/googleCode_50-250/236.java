// https://searchcode.com/api/result/13417881/

package uk.ac.ebi.pride.gui.component;

import uk.ac.ebi.pride.data.controller.DataAccessController;
import uk.ac.ebi.pride.data.controller.DataAccessMonitor;
import uk.ac.ebi.pride.gui.PrideViewerContext;
import uk.ac.ebi.pride.gui.prop.PropertyManager;
import uk.ac.ebi.pride.gui.utils.GUIUtilities;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.sql.Connection;
import java.util.Collection;
import java.util.LinkedList;

/**
 * DataSourceViewer should be monitor the DataAccessControllers in
 * DataAccessMonitor.
 * User: rwang
 * Date: 26-Feb-2010
 * Time: 10:42:08
 */
public class DataSourceViewer extends JPanel implements PropertyChangeListener {
    private Collection<ActionListener> actionListeners = null;
    private JTable sourceTable = null;
    private int selectedRow = 0;

    public DataSourceViewer() {
        this.setLayout(new BorderLayout());
        this.actionListeners = new LinkedList<ActionListener>();
        this.setMinimumSize(new Dimension(UIComponentConstants.DATA_SOURCE_VIEWER_WIDTH,
                                          UIComponentConstants.DATA_SOURCE_VIEWER_HEIGH));
        initialize();
    }

    private void initialize() {
        PrideViewerContext context = (PrideViewerContext)uk.ac.ebi.pride.gui.desktop.Desktop.getInstance().getDesktopContext();
        DataAccessMonitor monitor = context.getDataAccessMonitor();
        monitor.addPropertyChangeListener(this);
        DataAccessTableModel sourceTableModel = new DataAccessTableModel(monitor);
        TableCellRenderer sourceTableCellRenderer = new DataAccessTableCellRenderer(context);
        sourceTable = new JTable(sourceTableModel);
        sourceTable.setDefaultRenderer(String.class, sourceTableCellRenderer);
        sourceTable.setRowSelectionAllowed(true);
        sourceTable.setRowHeight(20);
        sourceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sourceTable.setFillsViewportHeight(true);
        sourceTable.getSelectionModel().addListSelectionListener(new DataAccessSelectionListener(monitor));
        sourceTable.setTableHeader(null);
        sourceTable.setGridColor(Color.white);
        //ToDo: reduce the size of the table

        // Scroll Pane
        JScrollPane scrollPane = new JScrollPane(sourceTable, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.add(scrollPane, BorderLayout.CENTER);

        // popup menu?
        // ToDo: popup menu ?

    }

    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        sourceTable.revalidate();
        sourceTable.repaint();
    }

    /**
     * DataAccessTableModel tracks data sources stored in DataAccessMonitor
     */
    private static class DataAccessTableModel extends AbstractTableModel {

        private DataAccessMonitor monitor = null;
        private static final String COLUMN_NAME = "Data source";

        public DataAccessTableModel(DataAccessMonitor monitor) {
            this.monitor = monitor;
        }

        public String getColumnName(int column) {
            return COLUMN_NAME;
        }

        public Class getColumnClass(int column) {
            return String.class;
        }

        @Override
        public int getRowCount() {
            java.util.List<DataAccessController> controllers = monitor.getControllers();
            return controllers.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            java.util.List<DataAccessController> controllers = monitor.getControllers();
            return controllers.get(rowIndex).getName();
        }
    }

    /**
     * DataAccessTableCellRender change the appearence of the cell
     * Only change the font at the moment
     */
    private static class DataAccessTableCellRenderer extends DefaultTableCellRenderer {
        private PrideViewerContext context;

        DataAccessTableCellRenderer(PrideViewerContext context) {
            this.context = context;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel cell = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            //cell.setFont(cell.getFont().deriveFont(Font.BOLD));
            PropertyManager propMgr = context.getPropertyManager();
            DataAccessMonitor monitor = context.getDataAccessMonitor();
            DataAccessController controller = monitor.getControllers().get(row);
            Icon icon = null;
            Object controllerSource = controller.getSource();
            if ( controllerSource instanceof File) {
                icon = GUIUtilities.loadIcon(propMgr.getProperty("file.source.small.icon"));
            } else if (controllerSource instanceof Connection) {
                icon = GUIUtilities.loadIcon(propMgr.getProperty("database.source.small.icon"));
            }
            cell.setIcon(icon);
            return cell;
        }
    }

    /**
     * DataAccessSelectionListener is triggered when a selection has been made on a data sourece
     */
    private class DataAccessSelectionListener implements ListSelectionListener {
        private DataAccessMonitor monitor;

        DataAccessSelectionListener(DataAccessMonitor monitor) {
            this.monitor = monitor;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int row = sourceTable.getSelectedRow();
            if (row != selectedRow) {
                selectedRow = row;
                DataAccessController foregroundController = monitor.getControllers().get(row);
                monitor.setForegroundDataAccessController(foregroundController);
            }
        }
    }
}

