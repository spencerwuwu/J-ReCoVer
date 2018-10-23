// https://searchcode.com/api/result/133647277/

/*
  * Copyright (c) 2011, Tulip Development Team
 * 
 * This file is part of Tulip.
 * 
 * Tulip is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * Tulip is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Tulip. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package de.unistuttgart.iste.se.tulip.view.attributemodel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.unistuttgart.iste.se.tulip.Messages;
import de.unistuttgart.iste.se.tulip.controller.action.CloseDialogAction;
import de.unistuttgart.iste.se.tulip.controller.observer.PatternObserver;
import de.unistuttgart.iste.se.tulip.model.AnnotatedEntity;
import de.unistuttgart.iste.se.tulip.model.vo.AnnotationVO;
import de.unistuttgart.iste.se.tulip.model.vo.ExtensionScenarioVO;
import de.unistuttgart.iste.se.tulip.model.vo.LocalStepVO;
import de.unistuttgart.iste.se.tulip.model.vo.MainScenarioVO;
import de.unistuttgart.iste.se.tulip.model.vo.ScenarioVO;
import de.unistuttgart.iste.se.tulip.model.vo.StepVO;
import de.unistuttgart.iste.se.tulip.utils.CurrentProject;
import de.unistuttgart.iste.se.tulip.utils.IconSize;
import de.unistuttgart.iste.se.tulip.utils.Icons;
import de.unistuttgart.iste.se.tulip.utils.TulipFonts;
import de.unistuttgart.iste.se.tulip.utils.Utils;
import de.unistuttgart.iste.se.tulip.view.internal.components.InfoButton;
import de.unistuttgart.iste.se.tulip.view.internal.components.TransparentPanel;
import de.unistuttgart.iste.se.tulip.view.internal.components.TulipPanel;
import de.unistuttgart.iste.se.tulip.view.internal.interfaces.Annotated;
import de.unistuttgart.iste.se.up.attributes.gen.Attribute;
import de.unistuttgart.iste.se.up.attributes.gen.Value;
import de.unistuttgart.iste.se.up.attributes.model.AttributeModelUtils;
import de.unistuttgart.iste.se.up.browser.ui.components.RichTextDisplay;
import de.unistuttgart.iste.se.up.model.UsabilityPattern;
import de.unistuttgart.iste.se.up.model.UseCaseElementType;

/**
 * Dialog to edit the annotations of a step or a sequence using the semantic interaction model.
 * 
 * @author Tulip Development Team
 */
public class EditAnnotationsDialog extends JDialog {
	
	private static final long serialVersionUID = 1666529845302467655L;
	
	protected AnnotatedEntity originalEntity;
	protected AnnotatedEntity entity;
	
	protected Annotated panel;
	
	private JPanel contentPanel = new TulipPanel();
	private JPanel buttonPanel = new TulipPanel();
	
	
	/**
	 * Creates the dialog.
	 **/
	public EditAnnotationsDialog(AnnotatedEntity entity, Annotated panel) {
		super(CurrentProject.getInstance().getMainWindow(), Messages.getString("EDIT_ANNOTATIONS"), true);
		
		this.originalEntity = entity;
		this.entity = entity.clone();
		this.panel = panel;
		
		// Update of automatic attribute assignment
		if (this.entity instanceof LocalStepVO) {
			AttributeModelUtils.updateAutomaticAttributesStep(this.entity.getAttributes(), ((LocalStepVO) this.entity)
					.getActor().isHuman());
		}
		if (this.entity instanceof ScenarioVO) {
			List<Map<Attribute, Value>> steps = new ArrayList<Map<Attribute, Value>>();
			boolean hasHumanActor = false;
			for (StepVO step : ((ScenarioVO) this.entity).getSteps()) {
				if (step instanceof LocalStepVO) {
					steps.add(((LocalStepVO) step).getAttributes());
					if (((LocalStepVO) step).getActor().isHuman()) {
						hasHumanActor = true;
					}
				}
			}
			AttributeModelUtils.updateAutomaticAttributesSequence(this.entity.getAttributes(), steps, hasHumanActor);
		}
		
		// Options
		setResizable(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		// Size & position
		Dimension windowDim = new Dimension(800, 800);
		Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
		
		setPreferredSize(windowDim);
		setLocation((int) ((screenDim.getWidth() - windowDim.getWidth()) * .5),
				(int) ((screenDim.getHeight() - windowDim.getHeight()) * .5));
		
		// Panels
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new JScrollPane(this.contentPanel), BorderLayout.CENTER);
		getContentPane().add(this.buttonPanel, BorderLayout.SOUTH);
		
		// Content
		GridBagLayout layout = new GridBagLayout();
		
		GridBagConstraints cFull = new GridBagConstraints(0, 0, 3, 1, 1, 0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0);
		GridBagConstraints cLeft = new GridBagConstraints(0, 0, 1, 1, 0.5, 0.5, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(10, 10, 0, 10), 0, 0);
		GridBagConstraints cRight = new GridBagConstraints(1, 0, 1, 1, 0.5, 0.5, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(10, 10, 0, 10), 0, 0);
		JComponent l;
		int i = 0;
		
		this.contentPanel.setLayout(layout);
		
		// Buttons
		this.buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		JButton save = new JButton(Messages.getString("SAVE"));
		save.addActionListener(new AbstractAction() {
			
			private static final long serialVersionUID = 8227602649920339484L;
			
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				addNewPatterns();
				
				EditAnnotationsDialog.this.originalEntity.setAnnotations(EditAnnotationsDialog.this.entity
						.getAnnotations());
				EditAnnotationsDialog.this.originalEntity.setAttributes(EditAnnotationsDialog.this.entity
						.getAttributes());
				EditAnnotationsDialog.this.panel.buildAnnotations();
				dispose();
			}
		});
		this.buttonPanel.add(save);
		
		JButton cancel = new JButton(Messages.getString("CANCEL"));
		cancel.addActionListener(new CloseDialogAction(this));
		this.buttonPanel.add(cancel);
		
		// Title 1
		TransparentPanel titlePanel = new TransparentPanel();
		titlePanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
		String title = Messages.getString("CHOOSE_ANNOTATIONS") + ": ";
		if (this.entity instanceof LocalStepVO) {
			title += Messages.getString("STEP") + " " + ((LocalStepVO) this.entity).getStepNumber();
		}
		if (this.entity instanceof MainScenarioVO) {
			title += Messages.getString("MAIN_SEQUENCE");
		}
		if (this.entity instanceof ExtensionScenarioVO) {
			title += Messages.getString("EXTENSION_SCENARIO") + " " + ((ExtensionScenarioVO) this.entity).getName();
		}
		l = new JLabel(title, Icons.ANNOTATION_XL, SwingConstants.LEADING);
		((JLabel) l).setIconTextGap(8);
		l.setFont(TulipFonts.TITLE_FONT);
		titlePanel.add(l);
		String dialogInfo = "";
		if (this.entity instanceof StepVO) {
			dialogInfo = Messages.getString("EDIT_ANNOTATIONS_INFO", Messages.getString("STEP"));
		}
		if (this.entity instanceof ScenarioVO) {
			dialogInfo = Messages.getString("EDIT_ANNOTATIONS_INFO", Messages.getString("SEQUENCE"));
		}
		titlePanel.add(new InfoButton(dialogInfo));
		cFull.gridy = i;
		layout.setConstraints(titlePanel, cFull);
		this.contentPanel.add(titlePanel);
		i++;
		
		// Patterns
		GroupPanel patternsPanel = new GroupPanel(Messages.getString("PATTERN_SUGGESTIONS"));
		final PatternChooser patternChooser;
		patternChooser = new PatternChooser(this.entity);
		patternsPanel.add(patternChooser);
		patternChooser.getParent().setBackground(Color.WHITE);
		cLeft.gridy = i;
		layout.setConstraints(patternsPanel, cLeft);
		this.contentPanel.add(patternsPanel);
		
		GroupPanel patternInfoPanel = new GroupPanel(Messages.getString("DESCRIPTION"));
		final RichTextDisplay patternInfo = new RichTextDisplay();
		patternChooser.addSelectionListener(new PatternChooserListener(patternInfo, patternChooser, patternInfoPanel));
		patternInfoPanel.add(patternInfo);
		cRight.gridy = i;
		layout.setConstraints(patternInfoPanel, cRight);
		this.contentPanel.add(patternInfoPanel);
		i++;
		
		// Annotations
		GroupPanel annotationsPanel = new GroupPanel(Messages.getString("AVAILABLE_ANNOTATIONS"));
		final AnnotationChooser annotationChooser = new AnnotationChooser();
		// Pattern chooser updates annotation chooser
		patternChooser.addSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				annotationChooser.update(EditAnnotationsDialog.this.entity, patternChooser.getSelectedPattern());
			}
		});
		// Annotation chooser updates pattern chooser
		annotationChooser.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				int selection = patternChooser.getSelectedRow();
				patternChooser.getModel().fireTableDataChanged();
				patternChooser.changeSelection(selection, 0, false, false);
			}
		});
		annotationsPanel.add(annotationChooser);
		cFull.gridy = i;
		cFull.fill = GridBagConstraints.BOTH;
		cFull.weighty = 0.3;
		layout.setConstraints(annotationsPanel, cFull);
		cFull.fill = GridBagConstraints.HORIZONTAL;
		cFull.weighty = 0;
		this.contentPanel.add(annotationsPanel);
		i++;
		/*
		if (entity instanceof ScenarioVO) {
			pack();
			setVisible(true);
			return;
		}
		*/
		// Title 2
		l = new JLabel(Messages.getString("OPTIMIZE_OPTIONS"), Icons.ATTRIBUTES.getIcon(IconSize.XL),
				SwingConstants.LEADING);
		((JLabel) l).setIconTextGap(8);
		l.setFont(TulipFonts.TITLE_FONT);
		cFull.gridy = i;
		cFull.insets = new Insets(30, 10, 0, 10);
		layout.setConstraints(l, cFull);
		cFull.insets = new Insets(10, 10, 0, 10);
		this.contentPanel.add(l);
		i++;
		
		// Attributes
		String lString = Messages.getString("STEP_ATTRIBUTES");
		if (this.entity instanceof ScenarioVO) {
			lString = Messages.getString("SCENARIO_ATTRIBUTES");
		}
		GroupPanel attributesPanel = new GroupPanel(lString);
		final AttributeChooser attributeChooser;
		attributeChooser = new AttributeChooser(this.entity);
		attributeChooser.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				int selection = patternChooser.getSelectedRow();
				patternChooser.getModel().fireTableDataChanged();
				patternChooser.changeSelection(selection, 0, false, false);
			}
		});
		attributesPanel.add(attributeChooser);
		attributeChooser.getParent().setBackground(Color.WHITE);
		cLeft.gridy = i;
		cLeft.gridheight = 3;
		layout.setConstraints(attributesPanel, cLeft);
		cLeft.gridheight = 1;
		this.contentPanel.add(attributesPanel);
		
		GroupPanel attributeInfoPanel = new GroupPanel(Messages.getString("DESCRIPTION"));
		final RichTextDisplay attributeInfo = new RichTextDisplay();
		attributeInfoPanel.add(attributeInfo);
		cRight.gridy = i;
		layout.setConstraints(attributeInfoPanel, cRight);
		this.contentPanel.add(attributeInfoPanel);
		i++;
		
		GroupPanel attributeValuePanel = new GroupPanel(Messages.getString("VALUE"));
		AttributeValueChooser attributeValueChooser = new AttributeValueChooser();
		attributeValueChooser.addChangeListener(new AttributeValueChangeListener(patternChooser, attributeChooser,
				attributeValueChooser));
		attributeChooser.addChangeListener(new AttributeChangeListener(attributeChooser, attributeValueChooser));
		attributeValuePanel.add(attributeValueChooser);
		cRight.gridy = i;
		layout.setConstraints(attributeValuePanel, cRight);
		this.contentPanel.add(attributeValuePanel);
		i++;
		
		GroupPanel attributeEffectsPanel = new GroupPanel(Messages.getString("EFFECT"));
		EffectsList effectsList = null;
		if (entity instanceof StepVO) {
			effectsList = new EffectsList(UseCaseElementType.Step);
		}
		if (entity instanceof ScenarioVO) {
			effectsList = new EffectsList(UseCaseElementType.Sequence);
		}
		attributeValueChooser.addChangeListener(new AttributeValueChangeListener(attributeValueChooser, effectsList));
		attributeChooser.addChangeListener(new AttributeChangeListener(attributeValueChooser, effectsList));
		
		attributeEffectsPanel.add(effectsList);
		cRight.gridy = i;
		cRight.insets = new Insets(10, 10, 0, 10);
		layout.setConstraints(attributeEffectsPanel, cRight);
		this.contentPanel.add(attributeEffectsPanel);
		i++;
		
		attributeChooser.addSelectionListener(new AttributeSelectionListener(attributeChooser, attributeValueChooser,
				attributeInfo, attributeInfoPanel, effectsList));
		
		if (patternChooser.getRowCount() > 0) {
			patternChooser.changeSelection(0, 0, false, false);
		}
		attributeChooser.changeSelection(0, 0, false, false);
		
		pack();
		setVisible(true);
	}
	
	
	/**
	 * Adds the patterns to the project that are required to add all annotations to the step/sequence.
	 **/
	protected void addNewPatterns() {
		HashSet<UsabilityPattern> newPatterns = new HashSet<UsabilityPattern>();
		
		// Find new patterns
		for (AnnotationVO annotation : this.entity.getAnnotations()) {
			UsabilityPattern pattern = Utils.findPatternInCatalog(annotation.getAnnotationTemplate());
			if (!CurrentProject.getInstance().isPatternInProject(pattern)) {
				newPatterns.add(pattern);
			}
		}
		
		// Add patterns to the project
		PatternObserver observer = new PatternObserver();
		for (UsabilityPattern pattern : newPatterns) {
			observer.applicationChanged(pattern, true);
		}
	}
	
	
	/**
	 * @author Tulip Development Team
	 * 
	 */
	private class AttributeValueChangeListener implements ChangeListener {
		
		protected AttributeChooser attributeChooser;
		private final AttributeValueChooser attributeValueChooser;
		private PatternChooser patternChooser;
		private EffectsList effectsList;
		
		
		/**
		 * @param attributeChooser
		 */
		private AttributeValueChangeListener(PatternChooser patternChooser, AttributeChooser attributeChooser,
				AttributeValueChooser attributeValueChooser) {
			this.patternChooser = patternChooser;
			this.attributeChooser = attributeChooser;
			this.attributeValueChooser = attributeValueChooser;
		}
		
		
		/**
		 * @param attributeValueChooser
		 * @param effectsList
		 */
		public AttributeValueChangeListener(AttributeValueChooser attributeValueChooser, EffectsList effectsList) {
			this.attributeValueChooser = attributeValueChooser;
			this.effectsList = effectsList;
		}
		
		
		@Override
		public void stateChanged(ChangeEvent e) {
			if (this.attributeChooser != null) {
				int selection = this.attributeChooser.getSelectedRow();
				EditAnnotationsDialog.this.entity.getAttributes().put(this.attributeChooser.getSelectedAttribute(),
						this.attributeValueChooser.getSelectedValue());
				this.attributeChooser.getModel().fireTableDataChanged();
				this.attributeChooser.changeSelection(selection, 0, false, false);
			}
			
			if (this.patternChooser != null) {
				int selection = this.patternChooser.getSelectedRow();
				this.patternChooser.getModel().fireTableDataChanged();
				this.patternChooser.changeSelection(selection, 0, false, false);
			}
			
			if (this.effectsList != null) {
				this.effectsList.update(this.attributeValueChooser.getSelectedValue());
			}
		}
	}
	
	
	private static class PatternChooserListener implements ListSelectionListener {
		
		private final RichTextDisplay patternInfo;
		private final PatternChooser patternChooser;
		protected GroupPanel patternInfoPanel;
		
		
		/**
		 * @param patternInfo
		 * @param patternChooser
		 */
		private PatternChooserListener(RichTextDisplay patternInfo, PatternChooser patternChooser,
				GroupPanel patternInfoPanel) {
			this.patternInfo = patternInfo;
			this.patternChooser = patternChooser;
			this.patternInfoPanel = patternInfoPanel;
		}
		
		
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (this.patternChooser.getSelectedPattern() != null) {
				this.patternInfo.setText(this.patternChooser.getSelectedPattern().getSolution());
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						PatternChooserListener.this.patternInfoPanel.getScrollPane().getViewport()
								.setViewPosition(new Point(0, 0));
					}
				});
			}
		}
	}
	
	
	private class AttributeSelectionListener implements ListSelectionListener {
		
		private RichTextDisplay attributeInfo;
		private AttributeChooser attributeChooser;
		private AttributeValueChooser attributeValueChooser;
		private EffectsList effectsList;
		private Attribute lastSelection;
		protected GroupPanel attributeInfoPanel;
		
		
		/**
		 * @param attributeChooser2
		 * @param attributeValueChooser2
		 * @param attributeInfo2
		 * @param attributeInfoPanel2
		 * @param effectsList2
		 */
		public AttributeSelectionListener(AttributeChooser attributeChooser,
				AttributeValueChooser attributeValueChooser, RichTextDisplay attributeInfo,
				GroupPanel attributeInfoPanel, EffectsList effectsList) {
			this.attributeChooser = attributeChooser;
			this.attributeValueChooser = attributeValueChooser;
			this.attributeInfo = attributeInfo;
			this.attributeInfoPanel = attributeInfoPanel;
			this.effectsList = effectsList;
		}
		
		
		@Override
		public void valueChanged(ListSelectionEvent e) {
			// Reduce unnessecary calls
			if (this.attributeChooser != null) {
				Attribute selection = this.attributeChooser.getSelectedAttribute();
				if (selection == null || selection.equals(this.lastSelection)) {
					return;
				}
				this.lastSelection = this.attributeChooser.getSelectedAttribute();
			}
			
			if (this.attributeInfo != null) {
				if (this.attributeChooser.getSelectedAttribute() != null) {
					this.attributeInfo.setText(this.attributeChooser.getSelectedAttribute().getDescription());
					SwingUtilities.invokeLater(new Runnable() {
						
						@Override
						public void run() {
							AttributeSelectionListener.this.attributeInfoPanel.getScrollPane().getViewport()
									.setViewPosition(new Point(0, 0));
						}
					});
				}
			}
			
			if (this.attributeValueChooser != null && this.attributeChooser != null) {
				if (this.attributeChooser.getSelectedAttribute() != null) {
					HashMap<Attribute, Value> attributes = EditAnnotationsDialog.this.entity.getAttributes();
					Attribute selection = this.attributeChooser.getSelectedAttribute();
					this.attributeValueChooser.update(selection, attributes.get(selection), attributes,
							EditAnnotationsDialog.this.entity);
				}
			}
			
			if (this.effectsList != null) {
				this.effectsList.update(this.attributeValueChooser.getSelectedValue());
			}
		}
	}
	
	
	/**
	 * @author Tulip Development Team
	 * 
	 */
	private class AttributeChangeListener implements ChangeListener {
		
		private AttributeChooser attributeChooser;
		private AttributeValueChooser attributeValueChooser;
		private EffectsList effectsList;
		
		
		public AttributeChangeListener(AttributeChooser attributeChooser, AttributeValueChooser attributeValueChooser) {
			this.attributeChooser = attributeChooser;
			this.attributeValueChooser = attributeValueChooser;
		}
		
		
		/**
		 * @param attributeValueChooser
		 * @param effectsList
		 */
		public AttributeChangeListener(AttributeValueChooser attributeValueChooser, EffectsList effectsList) {
			this.attributeValueChooser = attributeValueChooser;
			this.effectsList = effectsList;
		}
		
		
		/* (non-Javadoc)
		 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
		 */
		@Override
		public void stateChanged(ChangeEvent e) {
			if (this.attributeChooser != null) {
				if (this.attributeChooser.getSelectedAttribute() != null) {
					HashMap<Attribute, Value> attributes = EditAnnotationsDialog.this.entity.getAttributes();
					Attribute selection = this.attributeChooser.getSelectedAttribute();
					this.attributeValueChooser.update(selection, attributes.get(selection), attributes,
							EditAnnotationsDialog.this.entity);
				}
			}
			
			if (this.effectsList != null) {
				this.effectsList.update(this.attributeValueChooser.getSelectedValue());
			}
		}
		
	}
}

