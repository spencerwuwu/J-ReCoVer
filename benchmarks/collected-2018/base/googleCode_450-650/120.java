// https://searchcode.com/api/result/13551649/

/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.ws.commons.tcpmon.swing;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicButtonListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.apache.ws.commons.tcpmon.TCPMonBundle;
import org.apache.ws.commons.tcpmon.core.engine.Interceptor;
import org.apache.ws.commons.tcpmon.core.engine.InterceptorConfiguration;
import org.apache.ws.commons.tcpmon.core.engine.InterceptorConfigurationBuilder;
import org.apache.ws.commons.tcpmon.core.engine.RequestResponseListener;
import org.apache.ws.commons.tcpmon.core.ui.AbstractListener;
import org.apache.ws.commons.tcpmon.core.ui.Configuration;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Vector;

/**
 * this is one of the tabbed panels that acts as the actual proxy
 */
class Listener extends AbstractListener {
    
    private final JPanel panel;

    /**
     * Field portField
     */
    private JTextField portField = null;

    /**
     * Field hostField
     */
    private JTextField hostField = null;

    /**
     * Field tPortField
     */
    private JTextField tPortField = null;

    /**
     * Field isProxyBox
     */
    private JCheckBox isProxyBox = null;

    /**
     * Field startButton
     */
    private JToggleButton startButton = null;

    /**
     * Field removeButton
     */
    private final JButton removeButton;

    /**
     * Field removeAllButton
     */
    private final JButton removeAllButton;

    /**
     * Field xmlFormatBox
     */
    private JToggleButton xmlFormatBox = null;

    /**
     * Field saveButton
     */
    private final JButton saveButton;

    /**
     * Field resendButton
     */
    private final JButton resendButton;

    private final JButton switchButton;

    /**
     * Field connectionTable
     */
    private final JTable connectionTable;

    /**
     * Field tableModel
     */
    public DefaultTableModel tableModel = null;

    /**
     * Field outPane
     */
    private final JSplitPane outPane;

    private Interceptor interceptor = null;

    /**
     * Field leftPanel
     */
    private JPanel leftPanel = null;

    /**
     * Field rightPanel
     */
    private JPanel rightPanel = null;

    /**
     * Field notebook
     */
    private JTabbedPane notebook = null;

    private final InterceptorConfiguration baseConfiguration;

    /**
     * Field connections
     */
    public final Vector requestResponses = new Vector();

    /**
     * create a listener
     * 
     * @param _notebook
     * @param name
     * @param listenPort
     * @param host
     * @param targetPort
     * @param isProxy
     * @param slowLink
     *            optional reference to a slow connection
     */
    public Listener(JTabbedPane _notebook, String name, InterceptorConfiguration config) {
        
        notebook = _notebook;
        if (name == null) {
            name = TCPMonBundle.getMessage("port01", "Port") + " " + config.getListenPort();
        }

        baseConfiguration = config;
        panel = new JPanel(new BorderLayout());

        // 1st component is just a row of labels and 1-line entry fields
        // ///////////////////////////////////////////////////////////////////
        JToolBar top = new JToolBar();
        top.add(startButton = new JToggleButton(Icons.START));
        startButton.setToolTipText(TCPMonBundle.getMessage("start00", "Start") + " / "
                + TCPMonBundle.getMessage("stop00", "Stop"));
        top.addSeparator();
        top.add(new JLabel(TCPMonBundle.getMessage("listenPort01", "Listen Port:") + " ", SwingConstants.RIGHT));
        top.add(portField = new JTextField("" + config.getListenPort(), 4));
        top.add(new JLabel("  " + TCPMonBundle.getMessage("host00", "Host:") + " ", SwingConstants.RIGHT));
        top.add(hostField = new JTextField(config.getTargetHost(), 15));
        top.add(new JLabel("  " + TCPMonBundle.getMessage("port02", "Port:") + " ", SwingConstants.RIGHT));
        top.add(tPortField = new JTextField("" + config.getTargetPort(), 4));
        top.add(isProxyBox = new JCheckBox(TCPMonBundle.getMessage("proxy00", "Proxy")));
        isProxyBox.addChangeListener(new BasicButtonListener(isProxyBox) {
            public void stateChanged(ChangeEvent event) {
                JCheckBox box = (JCheckBox) event.getSource();
                boolean state = box.isSelected();
                tPortField.setEnabled(!state);
                hostField.setEnabled(!state);
            }
        });
        isProxyBox.setSelected(config.isProxy());
        portField.setEditable(false);
        portField.setMaximumSize(portField.getPreferredSize());
        hostField.setEditable(false);
        hostField.setMaximumSize(hostField.getPreferredSize());
        tPortField.setEditable(false);
        tPortField.setMaximumSize(tPortField.getPreferredSize());
        startButton.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (startButton.isSelected()) {
                    start();
                } else {
                    stop();
                }
            }
        });
        panel.add(top, BorderLayout.NORTH);

        // 2nd component is a split pane with a table on the top
        // and the request/response text areas on the bottom
        // ///////////////////////////////////////////////////////////////////
        tableModel = new DefaultTableModel(new String[] { TCPMonBundle.getMessage("state00", "State"),
                TCPMonBundle.getMessage("time00", "Time"), TCPMonBundle.getMessage("requestHost00", "Request Host"),
                TCPMonBundle.getMessage("targetHost", "Target Host"), TCPMonBundle.getMessage("request00", "Request..."),
                TCPMonBundle.getMessage("elapsed00", "Elapsed Time") }, 0);
        tableModel.addRow(new Object[] { "---", TCPMonBundle.getMessage("mostRecent00", "Most Recent"), "---", "---", "---",
                "---" });
        connectionTable = new JTable(1, 2);
        connectionTable.setModel(tableModel);
        connectionTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Reduce the STATE column and increase the REQ column
        TableColumn col;
        col = connectionTable.getColumnModel().getColumn(TCPMon.STATE_COLUMN);
        col.setMaxWidth(col.getPreferredWidth() / 2);
        col = connectionTable.getColumnModel().getColumn(TCPMon.REQ_COLUMN);
        col.setPreferredWidth(col.getPreferredWidth() * 2);
        ListSelectionModel sel = connectionTable.getSelectionModel();
        sel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    handleSelection();
                }
            }
        });

        // Add Response Section
        // ///////////////////////////////////////////////////////////////////
        JPanel pane2 = new JPanel();
        pane2.setLayout(new BorderLayout());
        leftPanel = new JPanel();
        leftPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(new JLabel("  " + TCPMonBundle.getMessage("request01", "Request")));
        leftPanel.add(new JLabel(" " + TCPMonBundle.getMessage("wait01", "Waiting for connection")));
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.add(new JLabel("  " + TCPMonBundle.getMessage("response00", "Response")));
        rightPanel.add(new JLabel(""));
        outPane = new JSplitPane(0, leftPanel, rightPanel);
        outPane.setDividerSize(4);
        pane2.add(outPane, BorderLayout.CENTER);
        top.addSeparator();
        top.add(xmlFormatBox = new JToggleButton(Icons.XML_FORMAT));
        xmlFormatBox.setToolTipText(TCPMonBundle.getMessage("xmlFormat00", "XML Format"));

        top.addSeparator();

        top.add(saveButton = new JButton(Icons.SAVE));
        saveButton.setToolTipText(TCPMonBundle.getMessage("save00", "Save"));
        top.add(resendButton = new JButton(Icons.RESEND));
        resendButton.setToolTipText(TCPMonBundle.getMessage("resend00", "Resend"));
        top.add(removeButton = new JButton(Icons.REMOVE));
        removeButton.setToolTipText(TCPMonBundle.getMessage("removeSelected00", "Remove Selected"));
        top.add(removeAllButton = new JButton(Icons.REMOVE_ALL));
        removeAllButton.setToolTipText(TCPMonBundle.getMessage("removeAll00", "Remove All"));
        removeButton.setEnabled(false);
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                remove();
            }
        });
        removeAllButton.setEnabled(false);
        removeAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                removeAll();
            }
        });

        top.addSeparator();

        switchButton = new JButton();
        switchButton.setToolTipText(TCPMonBundle.getMessage("switch00", "Switch Layout"));
        updateSwitchButton();
        top.add(switchButton);
        top.add(Box.createHorizontalGlue());
        JButton closeButton = new JButton(Icons.CLOSE);
        closeButton.setToolTipText(TCPMonBundle.getMessage("close00", "Close"));
        top.add(closeButton);
        saveButton.setEnabled(false);
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                save();
            }
        });
        resendButton.setEnabled(false);
        resendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                resend();
            }
        });
        switchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int v = outPane.getOrientation();
                if (v == 0) {

                    // top/bottom
                    outPane.setOrientation(1);
                } else {

                    // left/right
                    outPane.setOrientation(0);
                }
                outPane.setDividerLocation(0.5);
                updateSwitchButton();
            }
        });
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                close();
            }
        });
        JSplitPane pane1 = new JSplitPane(0);
        pane1.setDividerSize(4);
        pane1.setTopComponent(new JScrollPane(connectionTable));
        pane1.setBottomComponent(pane2);
        pane1.setDividerLocation(150);
        panel.add(pane1, BorderLayout.CENTER);

        // 
        // //////////////////////////////////////////////////////////////////
        sel.setSelectionInterval(0, 0);
        outPane.setDividerLocation(150);
        notebook.addTab(name, panel);
        start();
    }

    private void updateSwitchButton() {
        switchButton.setIcon(outPane.getOrientation() == 0 ? Icons.LAYOUT_VERTICAL : Icons.LAYOUT_HORIZONTAL);
    }

    public void handleSelection() {
        ListSelectionModel m = connectionTable.getSelectionModel();
        int divLoc = outPane.getDividerLocation();
        RequestResponse requestResponse;
        if (m.isSelectionEmpty()) {
            requestResponse = null;
            removeButton.setEnabled(false);
        } else {
            int row = m.getLeadSelectionIndex();
            if (row == 0) {
                if (requestResponses.size() == 0) {
                    requestResponse = null;
                } else {
                    requestResponse = (RequestResponse) requestResponses.lastElement();
                }
                removeButton.setEnabled(false);
            } else {
                requestResponse = (RequestResponse) requestResponses.get(row - 1);
                removeButton.setEnabled(true);
            }
        }
        if (requestResponse == null) {
            setLeft(new JLabel(" " + TCPMonBundle.getMessage("wait00", "Waiting for Connection...")));
            setRight(new JLabel(""));
        } else {
            setLeft(requestResponse.inputScroll);
            setRight(requestResponse.outputScroll);
        }
        saveButton.setEnabled(requestResponse != null);
        resendButton.setEnabled(requestResponse != null);
        removeAllButton.setEnabled(!requestResponses.isEmpty());
        outPane.setDividerLocation(divLoc);
    }

    /**
     * Method setLeft
     * 
     * @param left
     */
    private void setLeft(Component left) {
        leftPanel.removeAll();
        leftPanel.add(left);
    }

    /**
     * Method setRight
     * 
     * @param right
     */
    private void setRight(Component right) {
        rightPanel.removeAll();
        rightPanel.add(right);
    }

    /**
     * Method start
     */
    public void start() {
        if (interceptor == null) {
            InterceptorConfiguration config = getConfiguration().getInterceptorConfiguration();
            int port = config.getListenPort();
            portField.setText("" + port);
            int i = notebook.indexOfComponent(panel);
            notebook.setTitleAt(i, TCPMonBundle.getMessage("port01", "Port") + " " + port);
            tPortField.setText("" + config.getTargetPort());
            interceptor = new Interceptor(config, this);
            startButton.setSelected(true);
            portField.setEditable(false);
            hostField.setEditable(false);
            tPortField.setEditable(false);
            isProxyBox.setEnabled(false);
        }
    }

    /**
     * Method stop
     */
    public void stop() {
        if (interceptor != null) {
            try {
                interceptor.halt();
                interceptor = null;
                startButton.setSelected(false);
                portField.setEditable(true);
                hostField.setEditable(true);
                tPortField.setEditable(true);
                isProxyBox.setEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method remove
     */
    public void remove() {
        ListSelectionModel lsm = connectionTable.getSelectionModel();
        int bot = lsm.getMinSelectionIndex();
        int top = lsm.getMaxSelectionIndex();
        for (int i = top; i >= bot; i--) {
            ((RequestResponse) requestResponses.get(i - 1)).remove();
        }
        if (bot > requestResponses.size()) {
            bot = requestResponses.size();
        }
        lsm.setSelectionInterval(bot, bot);
    }

    /**
     * Method removeAll
     */
    public void removeAll() {
        ListSelectionModel lsm = connectionTable.getSelectionModel();
        lsm.clearSelection();
        while (requestResponses.size() > 0) {
            ((RequestResponse) requestResponses.get(0)).remove();
        }
        lsm.setSelectionInterval(0, 0);
    }

    /**
     * Method close
     */
    public void close() {
        stop();
        notebook.remove(panel);
    }

    /**
     * Method save
     */
    public void save() {
        JFileChooser dialog = new JFileChooser(".");
        int rc = dialog.showSaveDialog(panel);
        if (rc == JFileChooser.APPROVE_OPTION) {
            try {
                File file = dialog.getSelectedFile();
                FileOutputStream out = new FileOutputStream(file);
                ListSelectionModel lsm = connectionTable.getSelectionModel();
                rc = lsm.getLeadSelectionIndex();
                int n = 0;
                for (Iterator i = requestResponses.iterator(); i.hasNext(); n++) {
                    RequestResponse requestResponse = (RequestResponse) i.next();
                    if (lsm.isSelectedIndex(n + 1) || (!(i.hasNext()) && (lsm.getLeadSelectionIndex() == 0))) {
                        rc = Integer.parseInt(portField.getText());
                        out.write("\n==============\n".getBytes());
                        out.write(((TCPMonBundle.getMessage("listenPort01", "Listen Port:") + " " + rc + "\n")).getBytes());
                        out.write((TCPMonBundle.getMessage("targetHost01", "Target Host:") + " " + hostField.getText() + "\n")
                                .getBytes());
                        rc = Integer.parseInt(tPortField.getText());
                        out.write(((TCPMonBundle.getMessage("targetPort01", "Target Port:") + " " + rc + "\n")).getBytes());
                        out.write((("==== " + TCPMonBundle.getMessage("request01", "Request") + " ====\n")).getBytes());
                        out.write(requestResponse.getRequestAsString().getBytes());
                        out.write((("==== " + TCPMonBundle.getMessage("response00", "Response") + " ====\n")).getBytes());
                        out.write(requestResponse.getResponseAsString().getBytes());
                        out.write("\n==============\n".getBytes());
                    }
                }
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method resend
     */
    public void resend() {
        int rc;
        ListSelectionModel lsm = connectionTable.getSelectionModel();
        rc = lsm.getLeadSelectionIndex();
        if (rc == 0) {
            rc = requestResponses.size();
        }
        RequestResponse requestResponse = (RequestResponse) requestResponses.get(rc - 1);
        if (rc > 0) {
            lsm.clearSelection();
            lsm.setSelectionInterval(0, 0);
        }
        resend(requestResponse);
    }

    public Configuration getConfiguration() {
        
        InterceptorConfigurationBuilder configBuilder = new InterceptorConfigurationBuilder(baseConfiguration);
        Configuration config = new Configuration();
        configBuilder.setListenPort(Integer.parseInt(portField.getText()));
        configBuilder.setTargetHost(hostField.getText());
        configBuilder.setTargetPort(Integer.parseInt(tPortField.getText()));
        configBuilder.setProxy(isProxyBox.isSelected());
        config.setInterceptorConfiguration(configBuilder.build());
        config.setXmlFormat(xmlFormatBox.isSelected());
        
        return config;
    }

    public void onServerSocketStart() {
        setLeft(new JLabel(TCPMonBundle.getMessage("wait00", " Waiting for Connection...")));
        panel.repaint();
    }

    public void onServerSocketError(Throwable ex) {
        JLabel tmp = new JLabel(ex.toString());
        tmp.setForeground(Color.red);
        setLeft(tmp);
        setRight(new JLabel(""));
        stop();
    }

    public RequestResponseListener createRequestResponseListener(String fromHost) {
        return new RequestResponse(this, fromHost);
    }
    
} // End of the Class //

