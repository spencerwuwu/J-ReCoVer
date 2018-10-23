// https://searchcode.com/api/result/117894073/

/**
 * Copyright (c) 2009,2010 Washington University
 */
package org.nrg.upload.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.json.JSONException;
import org.netbeans.spi.wizard.WizardPage;
import org.nrg.net.RestServer;
import org.nrg.upload.data.Project;
import org.nrg.upload.data.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class SelectSubjectPage extends WizardPage {
	private static final long serialVersionUID = 1L;

	public static final String PRODUCT_NAME = "subject";
	private static final String STEP_DESCRIPTION = "Select subject";
	private static final String LONG_DESCRIPTION = "Select the subject for the session to be uploaded";

	private final Logger logger = LoggerFactory.getLogger(SelectSubjectPage.class);
	private final JList list;
	private final DefaultListModel listModel = new DefaultListModel();
	private final Dimension dimension;
	private final RestServer xnat;
        public String filterExpr = null; //UIOWA customization: variable for a filter

	public static final String getDescription() {
		return STEP_DESCRIPTION;
	}

	public SelectSubjectPage(final RestServer xnat, final Dimension dimension) {
		this.xnat = xnat;
		this.dimension = dimension;
		list = new JList(listModel);
		list.setName(PRODUCT_NAME);
		setLongDescription(LONG_DESCRIPTION);
	}

	/*
	 * (non-Javadoc)
	 * @see org.netbeans.spi.wizard.WizardPage#recycle()
	 */
	protected void recycle() {
		removeAll();
		refreshSubjectList();
		validate();
                filterExpr = null; //UIOWA customization: initialize to null
	}

	/*
	 * (non-Javadoc)
	 * @see org.netbeans.spi.wizard.WizardPage#renderingPage()
	 */
	protected void renderingPage() {
		setBusy(true);
		final Project project = (Project) getWizardData(SelectProjectPage.PRODUCT_NAME);
		final JScrollPane subject_list = new javax.swing.JScrollPane(this.list);
		if (null != dimension) {
			subject_list.setPreferredSize(dimension);
		}
		add(subject_list);
		if (null != project) {
			refreshSubjectList();
			final JButton newSubject = new JButton("Create new subject");

			final JDialog newSubjectDialog = new NewSubjectDialog(this, xnat, project);
			newSubject.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					newSubjectDialog.setVisible(true);
				}
			});
			newSubject.setEnabled(true);
			//add(newSubject); //UIOWA customization: Don't like that users can create subjects from applet to reduce typo problems

                        //UIOWA customization: add a filter that list subjects that match.

                        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridBagLayout());
                        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
                        final javax.swing.JLabel filterLabel = new javax.swing.JLabel("Search:");
                        c.gridx=0;c.gridy=0;
                        panel.add(filterLabel, c);

                        final javax.swing.JTextField filter = new javax.swing.JTextField(7);
                        filter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
                             public void changedUpdate(javax.swing.event.DocumentEvent documentEvent) {
                                 doFilter(documentEvent);
                             }
                             public void insertUpdate(javax.swing.event.DocumentEvent documentEvent) {
                                 doFilter(documentEvent);
                             }
                             public void removeUpdate(javax.swing.event.DocumentEvent documentEvent) {
                                 doFilter(documentEvent);
                             }
                             private void doFilter(javax.swing.event.DocumentEvent documentEvent) {
                                 javax.swing.text.Document source = documentEvent.getDocument();
                                 try {
                                    String text = source.getText(0, source.getLength());
                                    if(text != null && text.trim().length() != 0)filterExpr = text.trim();
                                    else filterExpr = null;
                                    listModel.removeAllElements();
                                    for (Subject subject:project.getSubjects()){
                                        if(filterExpr==null || filterExpr.length()==0 || (subject.getLabel() != null && subject.getLabel().contains(filterExpr)))listModel.addElement(subject);
                                    }
                                 } catch (Exception e){}
                             }
                        });
                        c.gridx=1;c.gridy=0;
                        panel.add(filter, c);

                        add(panel);
                        //End of UIOWA customation
		}
		setBusy(false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.netbeans.spi.wizard.WizardPage#validateContents(java.awt.Component, java.lang.Object)
	 */
	protected String validateContents(final Component component, final Object o) {
		return null == getWizardData(PRODUCT_NAME) ? "" : null;
	}

	/**
	 * Refreshes the list of subjects in the current project.
	 * @param selection item in the subjects list to be selected after refresh
	 */
	void refreshSubjectList(final Object selection) {
		final Project project = (Project) getWizardData(SelectProjectPage.PRODUCT_NAME);
		putWizardData(PRODUCT_NAME, null);
		final Callable<Object> doRefresh = new Callable<Object>() {
			public Object call() throws IOException, JSONException {
				SUBJECT_LABELS: for (;;) {
					listModel.removeAllElements();
					try {
						for (final Subject subject : project.getSubjects()) {
                                                        if(filterExpr==null || filterExpr.length()==0 || (subject.getLabel() != null && subject.getLabel().contains(filterExpr)))listModel.addElement(subject); //UIOWA customization: replacing statement listModel.addElement(subject);
						}
						break SUBJECT_LABELS;
					} catch (InterruptedException retry) {
						logger.info("subject retrieval interrupted, retrying", retry);
					} catch (ExecutionException e) {
						final Throwable cause = e.getCause();
						if (cause instanceof IOException) {
							final Object[] options = { "Retry", "Cancel" };
							final int n = JOptionPane.showOptionDialog(SelectSubjectPage.this,
									"Unable to contact " + xnat,
									"Network error",
									JOptionPane.YES_NO_OPTION,
									JOptionPane.ERROR_MESSAGE,
									null,
									options,
									options[0]);
							if (JOptionPane.NO_OPTION == n) {
								throw (IOException)cause;
							} else {
								logger.error("error getting subject list; retrying at user request", cause);
							}
						} else if (cause instanceof JSONException) {
							final Object[] options = { "Retry", "Cancel" };
							final int n = JOptionPane.showOptionDialog(SelectSubjectPage.this,
									"Received invalid response from " + xnat,
									"Server error",
									JOptionPane.YES_NO_OPTION,
									JOptionPane.ERROR_MESSAGE,
									null,
									options,
									options[0]);
							if (JOptionPane.NO_OPTION == n) {
								throw (IOException)cause;
							} else {
								logger.error("error getting subject list; retrying at user request", cause);
							}
						} else {
							logger.error("error getting subject list for " + project, cause);
							logger.info("will retry in 1000 ms");
							try {
								Thread.sleep(1000);
							} catch (InterruptedException ignore) {}
						}
					}
				}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() { selectSubject(selection); }
			});
			return selection;
			}
		};
		project.submit(doRefresh);
	}

	/**
	 * Refreshes the list of subjects in the current project, reselecting
	 * the current selection after refresh is complete.
	 */
	void refreshSubjectList() {
		refreshSubjectList(getWizardData(PRODUCT_NAME));
	}

	private void selectSubject(Object selection) {
		if (listModel.contains(selection)) {
			list.setSelectedValue(selection, true);
		}
	}
}

