// https://searchcode.com/api/result/74205389/

/*******************************************************************************
 * Copyright (c) 2011 SunGard CSA LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SunGard CSA LLC - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.stardust.engine.core.compatibility.gui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Component;
import java.util.Vector;

import javax.swing.*;

import org.eclipse.stardust.common.error.InternalException;


/**
 * Class to arrange one or more fields consisting of label and inputfield <P>
 * (see main method for usage demo - especially panel1 & 2)
 */
public class LabeledComponentsPanel extends JPanel
{
   JPanel composedPanel;

   Vector labels;
   Vector fields;

   private boolean readOnly = false;

   /**
    * Position of current JLabel/JComponent-pair
    */
   Vector positions;

   /**
    * By default assume 2 columns
    */
   static Integer integers[] = {new Integer(0), new Integer(1)};

   /**
    * Label alignment as given by SwingConstants
    */
   int horizontalLabelAlignment = SwingConstants.LEFT;
   /**
    * Field alignment as given by SwingConstants
    */
   int horizontalFieldAlignment = SwingConstants.LEFT;
   /**
    * gridBagConstraints
    */
   java.awt.GridBagConstraints constraints = new java.awt.GridBagConstraints();

   /**
    * size that the JComponents must have
    */
   private final int MINIMAL_SIZE = 8;

   /*
    * true enlarges the space between label & field.
    */
   protected boolean isEnlargingSpace = false;
   /**
    * true formats all rows.
    */
   protected boolean isFormattingAllColumns = false;
   protected Dimension maxLabelSize;
   /**
    * pos where the standard alignment starts at, must be >0 !!!
    */
   final int STARTING_POSITION = 1;

   public final int STDPOS = SwingConstants.CENTER;
   public final int LEFT = SwingConstants.LEFT;
   public final int RIGHT = SwingConstants.RIGHT;

   /**
    *
    */
   public LabeledComponentsPanel()
   {
      super();

      labels = new Vector();
      fields = new Vector();
      positions = new Vector();

      setLayout(new java.awt.GridBagLayout());
   }

   /**
    *
    */
   public void setMaxLabelSize(Dimension maxLabelSize)
   {
      this.maxLabelSize = maxLabelSize;
   }

   /**
    * Add component with a label.
    */
   public void add(JComponent field, String text)
   {
      Entry _entry = null;

      field = wrap(field);

      JLabel label = new JLabel(text);

      if (maxLabelSize != null)
      {
         label.setPreferredSize(maxLabelSize);
      }

      labels.add(label);
      if (field instanceof Entry)
      {
         _entry = ((Entry) field);
         _entry.setReadonly(_entry.isReadonly() || this.isReadonly());
      }
      fields.add(field);

      // position == null <=> no more elements in this row -> go to next line

      positions.add(null);
   }

   /**
    * Add component with label and mnemonic.
    */
   public void add(JComponent field, String text, int mnemonic)
   {
      add(fields.size(), field, text, mnemonic);
   }

   public void removeAt(int index)
   {
      JComponent object = (JComponent) fields.elementAt(index);
      JComponent label = (JComponent) labels.elementAt(index);
      super.remove(object);
      super.remove(label);
      fields.remove(index);
      labels.remove(index);
   }

   public void remove(JComponent field)
   {
      int index = fields.indexOf(field);
      if (!(index < 0))
      {
        removeAt(index);
      }
      invalidate();
   }

   /**
    * Add component with label and mnemonic.
    */
   public void add(int index, JComponent field, String text, int mnemonic)
   {
      Entry _entry = null;

      field = wrap(field);

      JLabel label = new JLabel(text);

      if (maxLabelSize != null)
      {
         label.setPreferredSize(maxLabelSize);
      }

      label.setDisplayedMnemonic(mnemonic);
      labels.add(index, label);

      if (field instanceof Entry)
      {
         label.setLabelFor(field);
         _entry = ((Entry) field);
         _entry.setReadonly(_entry.isReadonly() || this.isReadonly());
      }

      fields.add(index, field);

      positions.add(null);
   }

   /**
    * Add component with label and mnemonic.
    */
   public void add(JComponent field, String text, int mnemonic, String unit)
   {
      Entry _entry = null;

      field = wrap(field);

      // todo put at the end of the field

      JLabel label = null;

      if (text.endsWith(":"))
      {
         label = new JLabel(text.substring(0, text.length() - 1) + " (" + unit + "):");
      }
      else
      {
         label = new JLabel(text + " (" + unit + ")");
      }

      if (maxLabelSize != null)
      {
         label.setPreferredSize(maxLabelSize);
      }

      label.setDisplayedMnemonic(mnemonic);
      labels.add(label);

      if (field instanceof Entry)
      {
         label.setLabelFor(field);
         _entry = ((Entry) field);
         _entry.setReadonly(_entry.isReadonly() || this.isReadonly());
      }

      fields.add(field);

      positions.add(null);
   }

   /**
    * Add components together with their labels in the same row.
    */
   public void add(JComponent fields[], String texts[])
   {
      add(fields, texts, null);
   }

   /** Add components with their labels in the same row and
    *  set the position where the column alignment will start at.
    */
   public void add(JComponent fields[], String texts[], int alignPos)
   {
      add(fields, texts, null, alignPos);
   }

   /**
    * Add components with their labels and mnemonics in the same row.
    */
   public void add(JComponent fields[], String texts[], int mnemonics[])
   {
      add(fields, texts, mnemonics, 0);
   }

   /**
    * Add components with their labels and mnemonics in the same row and
    * set the position where the column alignment will start at.
    */
   public void add(JComponent jcfields[], String texts[], int mnemonics[],
                   int alignPos)
   {
      if (jcfields == null || jcfields.length < 1)
      {
         return;
      }

      Entry _entry = null;

      // Wrap components with mandatory padding

      for (int n = 0; n < jcfields.length; ++n)
      {
         jcfields[n] = wrap(jcfields[n]);

         if (jcfields[n] instanceof Entry)
         {
            _entry = ((Entry) jcfields[n]);
            _entry.setReadonly(_entry.isReadonly() || this.isReadonly());
         }
      }

      // test parameters

      if (jcfields.length != texts.length
            || (mnemonics != null && jcfields.length != mnemonics.length))
      {
         throw new InternalException("LabeledComponentsPanel: Lengths do not match:"
               + " fields = " + jcfields.length
               + "; texts = " + texts.length
               + "; mnemonics = " + mnemonics.length);
      }

      // Check alignment

      if (alignPos < 0)
      {
         alignPos = 0;
      }
      else if (alignPos >= jcfields.length)
      {
         alignPos = jcfields.length - 1;
      }

      // declare label variables

      JLabel label;

      // Check if all columns are to be formatted

      if (!isFormattingAllColumns)
      {
         if (alignPos > 0)
         {
            fields.add(composePanel(jcfields, texts, mnemonics, 0, alignPos - 1,
                  true, true));
            labels.add(null); // just put 'null' in here to identify the leader
         }

         // Add first label & all other fields

         label = new JLabel(texts[alignPos]);

         if (maxLabelSize != null)
         {
            label.setPreferredSize(maxLabelSize);
         }

         if (mnemonics != null)
         {
            label.setDisplayedMnemonic(mnemonics[alignPos]);
         }

         labels.add(label);
         fields.add(composePanel(jcfields, texts, mnemonics,
               alignPos, jcfields.length - 1, false, false));
         positions.add(null);
      }
      else
      {
         int max = jcfields.length - 1;
         for (int i = 0; i < max; i++)
         {
            label = new JLabel(texts[i]);

            if (maxLabelSize != null)
            {
               label.setPreferredSize(maxLabelSize);
            }

            if (mnemonics != null)
            {
               label.setDisplayedMnemonic(mnemonics[i]);
            }

            labels.add(label);
            fields.add(jcfields[i]);
            positions.add(getInteger(i + alignPos));
         }

         // Add last one (different position only)

         label = new JLabel(texts[max]);

         if (maxLabelSize != null)
         {
            label.setPreferredSize(maxLabelSize);
         }

         if (mnemonics != null)
         {
            label.setDisplayedMnemonic(mnemonics[max]);
         }

         labels.add(label);
         fields.add(jcfields[max]);
         positions.add(null);
      }
   }

   /** get back the corresponding Integer for gaining some speed (reduce usage of
    new()-operator */
   public Integer getInteger(int pos)
   {
      // check if Integer corresponding to pos is already prepared

      if (pos >= integers.length)
      {
         // Per default add as much Integers as needed + an additional one

         final int NEW_NUMBER = pos - integers.length + 2;
         Integer newIntegers[] = new Integer[pos + NEW_NUMBER];

         // Copy old ones

         System.arraycopy(integers, 0, newIntegers, 0, integers.length);

         // Add new ones

         for (int i = integers.length; i < integers.length + NEW_NUMBER; i++)
         {
            newIntegers[i] = new Integer(i);
         }

         integers = newIntegers;
      }

      return integers[pos];
   }

   /** compose a standard panel for given labels & fields <p>
    All values taken in interval [start,end], i.e. start,...,end<br> */
   public JPanel composePanel(JComponent fields[],
                              String texts[], int mnemonics[],
                              int start, int end,
                              boolean isAddingFirstLabel,
                              boolean isAddingAnotherDistance)
   {
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

      for (int i = start; i <= end; i++)
      {
         if (i != start || isAddingFirstLabel)
         {
            JLabel label = new JLabel(texts[i]);
            if (mnemonics != null)
            {
               label.setDisplayedMnemonic(mnemonics[i]);
            }
            panel.add(label);
            panel.add(Box.createHorizontalStrut(GUI.HorizontalLabelDistance));
         }

         panel.add(fields[i]);

         if (i != end || isAddingAnotherDistance)
         {
            panel.add(Box.createHorizontalStrut(GUI.HorizontalWidgetDistance));
         }
      }

      return panel;
   }

   /** set alignment for labels */
   public void setHorizontalLabelAlignment(int horizontalAlignment)
   {
      switch (horizontalAlignment)
      {
         case SwingConstants.LEFT:
            constraints.anchor = java.awt.GridBagConstraints.WEST;
            break;
         case SwingConstants.CENTER:
            constraints.anchor = java.awt.GridBagConstraints.CENTER;
            break;
         default:
            constraints.anchor = java.awt.GridBagConstraints.EAST;
            break;
      }

      this.horizontalLabelAlignment = horizontalAlignment;
   }

   /** get alignment of labels */
   public int getHorizontalLabelAlignment()
   {
      return horizontalLabelAlignment;
   }

   /** set alignment for fields */
   public void setHorizontalFieldAlignment(int horizontalAlignment)
   {
      switch (horizontalAlignment)
      {
         case SwingConstants.LEFT:
            constraints.anchor = java.awt.GridBagConstraints.WEST;
            break;
         case SwingConstants.CENTER:
            constraints.anchor = java.awt.GridBagConstraints.CENTER;
            break;
         default:
            constraints.anchor = java.awt.GridBagConstraints.EAST;
            break;
      }

      this.horizontalFieldAlignment = horizontalAlignment;
   }

   /** get alignment of fields */
   public int getHorizontalFieldAlignment()
   {
      return horizontalFieldAlignment;
   }

   /** translate gridbagconstants <= swingconstants */
   public int getGridBagHorizontalAlignment(int horizontalAlignment)
   {
      switch (horizontalAlignment)
      {
         case SwingConstants.LEFT:
            return java.awt.GridBagConstraints.WEST;
         case SwingConstants.CENTER:
            return java.awt.GridBagConstraints.CENTER;
         default:
            return java.awt.GridBagConstraints.EAST;
      }
   }

   /** translate gridbagconstants <=> swingconstants */
   public int getSwingHorizontalAlignment(int horizontalAlignment)
   {
      switch (horizontalAlignment)
      {
         case java.awt.GridBagConstraints.WEST:
            return SwingConstants.LEFT;
         case java.awt.GridBagConstraints.CENTER:
            return SwingConstants.CENTER;
         default:
            return SwingConstants.RIGHT;
      }
   }

   /** set behaviour in case of more available space than needed for displaying
    */
   public void setEnlargingSpace(boolean isEnlargingSpace)
   {
      this.isEnlargingSpace = isEnlargingSpace;
   }

   /** returns true if free space is enlarged in case of more available space. <p>
    Default is false */
   public boolean isEnlargingSpace()
   {
      return isEnlargingSpace;
   }

   /** set behaviour in case of more available space than needed for displaying
    */
   public void setFormattingAllColumns(boolean isFormattingAllColumns)
   {
      this.isFormattingAllColumns = isFormattingAllColumns;
   }

   /** returns true LabeledComponentsPanel allows multiple columns. <p>
    Default is false */
   public boolean isFormattingAllColumns()
   {
      return isFormattingAllColumns;
   }

   /** get number of elements that were put in */
   public int getCount()
   {
      return labels.size();
   }

   /** get label at specified pos  */
   public JLabel getLabelAt(int i)
   {
      return (JLabel) labels.elementAt(i);
   }

   /** get entry field at specified pos  */
   public JComponent getFieldAt(int i)
   {
      return (JComponent) fields.elementAt(i);
   }

   // methods & vars to pack the whole thing

   int labelAlignment;
   int fieldAlignment;

   // set set-and-forget-constraints (after adding first line)

   final Insets DefaultInsets = new Insets(GUI.VerticalWidgetDistance, 0, 0, 0);

   // var to get max width of JComponents (in case of pure unsized JTextfields)
   int maxWidth;

   /** Decide which pack method is to be used. */
   public void pack()
   {
      // Get the alignments

      labelAlignment = getGridBagHorizontalAlignment(horizontalLabelAlignment);
      fieldAlignment = getGridBagHorizontalAlignment(horizontalFieldAlignment);

      // Insets for first line are a bit different

      constraints.insets = new Insets(0, 0, 0, 0);
      constraints.gridwidth = 1;
      constraints.gridheight = 1;
      maxWidth = 0;

      // Analyze the components to ensure that width of components allows input

      int max = fields.size();

      for (int i = 0; i < max; i++)
      {
         // Only check sizes at standard position (identifyed by label != null)

         if (labels.elementAt(i) == null)
         {
            continue;
         }

         JComponent component = (JComponent) fields.elementAt(i);

         // Check minSize

         int componentWidth = component.getPreferredSize().width;
         if (maxWidth < componentWidth)
         {
            maxWidth = componentWidth;
         }
      }

      // Choose the right packing method

      if (isFormattingAllColumns)
      {
         packAll();
      }
      else
      {
         packSingle();
      }

      // We grant arbitrary horizontal stretching but disallow vertical stretching; vertical
      // size is the size of the layouted components

      setMaximumSize(new Dimension(getMaximumSize().width,
            getPreferredSize().height));
   }

   public void packAllAlignMax()
   {
      // Get the alignments

      labelAlignment = getGridBagHorizontalAlignment(horizontalLabelAlignment);
      fieldAlignment = getGridBagHorizontalAlignment(horizontalFieldAlignment);

      // Insets for first line are a bit different

      constraints.insets = new Insets(0, 0, 0, 0);
      constraints.gridwidth = 1;
      constraints.gridheight = 1;
      maxWidth = 0;

      // Analyze the components to ensure that width of components allows input

      int max = fields.size();

      for (int i = 0; i < max; i++)
      {
         // Only check sizes at standard position (identifyed by label != null)

         if (labels.elementAt(i) == null)
         {
            continue;
         }

         JComponent component = (JComponent) fields.elementAt(i);

         // Check minSize

         int componentWidth = component.getPreferredSize().width;
         if (maxWidth < componentWidth)
         {
            maxWidth = componentWidth;
         }
      }

      for (int i = 0; i < max; i++)
      {
         JComponent component = (JComponent) fields.elementAt(i);

         Dimension dimension = component.getPreferredSize();
         dimension.setSize(maxWidth, dimension.height);
         component.setPreferredSize(dimension);
      }
      // Choose the right packing method

      if (isFormattingAllColumns)
      {
         packAll();
      }
      else
      {
         packSingle();
      }

      // We grant arbitrary horizontal stretching but disallow vertical stretching; vertical
      // size is the size of the layouted components

      setMaximumSize(new Dimension(getMaximumSize().width,
            getPreferredSize().height));
   }

   /** pack to get the real panel */
   public void packAll()
   {
      Integer position = null;
      int maxX = 0;

      // starting pos

      constraints.gridx = STARTING_POSITION;
      int ypos = 0;

      // go for all elements in vectors

      int max = fields.size();

      for (int i = 0; i < max; i++)
      {
         // different insets for first line

         if (constraints.insets != DefaultInsets && ypos != 0)
         {
            constraints.insets = DefaultInsets;
         }

         // constraints for label

         constraints.gridy = ypos;
         constraints.weightx = 0.0;
         constraints.weighty = 0.0;
         constraints.anchor = labelAlignment;
         constraints.fill = java.awt.GridBagConstraints.NONE;

         position = (Integer) positions.elementAt(i);
         if (position != null)
         {
            constraints.gridx = 4 * position.intValue() + STARTING_POSITION;
         }

         JLabel label = (JLabel) labels.elementAt(i);
         if (label != null)// only add it if needed
         {
            super.add(label, constraints);
         }

         // add space between label & jcomponent

         constraints.gridx++;
         if (isEnlargingSpace && (maxWidth > MINIMAL_SIZE))
         {
            constraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            constraints.weightx = 1.0;
         }
         else
         {
            constraints.fill = java.awt.GridBagConstraints.NONE;
         }
         super.add(Box.createHorizontalStrut(GUI.HorizontalLabelDistance), constraints);

         // constraints for JComponent

         constraints.gridx++;
         constraints.anchor = fieldAlignment;

         // get the JComponent

         JComponent component = (JComponent) fields.elementAt(i);
         if (component != null)// only add it if needed
         {
            // check if size is <= MINIMAL_SIZE <=> only border is drawn

            if (component.getPreferredSize().width > MINIMAL_SIZE)
            {
               constraints.fill = java.awt.GridBagConstraints.NONE;
            }
            else
            {
               constraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            }

            if (maxWidth > MINIMAL_SIZE)
            {
               constraints.weightx = 0.0;
            }
            else
            {
               constraints.weightx = 1.0;
            }

            super.add(component, constraints);
         }

         if (constraints.gridx > maxX)
         {
            maxX = constraints.gridx;
            maxX++;
         }

         // finally increase y position if no special position is given

         if (position == null)
         {
            ypos++;
            constraints.gridx = STARTING_POSITION;
         }
         else
         {
            constraints.weightx = 0.0;
            constraints.gridx++;
            super.add(Box.createHorizontalStrut(GUI.HorizontalWidgetDistance),
                  constraints);

            constraints.gridx++;
         }
      }

      // add a simple glue to make this panel be left aligned if !isEnlargingSpace

      if (!isEnlargingSpace && max > 0)
      {
         constraints.gridx = ++maxX;
         constraints.gridy = 0;
         constraints.weightx = 1.0;
         constraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         super.add(Box.createHorizontalGlue(), constraints);
      }
   }

   /**
    * Pack to get the real panel.
    */
   private void packSingle()
   {
      int ypos = 0;
      int max = fields.size();

      for (int i = 0; i < max; i++)
      {
         // different insets for first line

         if (constraints.insets != DefaultInsets && ypos != 0)
         {
            constraints.insets = DefaultInsets;
         }

         // constraints for main label & left panel

         constraints.gridy = ypos;
         constraints.weightx = 0.0;
         constraints.weighty = 0.0;
         constraints.anchor = labelAlignment;
         constraints.fill = java.awt.GridBagConstraints.NONE;

         // left panel ?

         JLabel label = (JLabel) labels.elementAt(i);

         if (label == null)
         {
            //constraints.anchor = fieldAlignment;

            constraints.gridx = STARTING_POSITION - 1;
            super.add((JComponent) fields.elementAt(i), constraints);

            continue;
         }

         // main label

         constraints.gridx = STARTING_POSITION;
         super.add(label, constraints);

         // main space

         constraints.gridx++;

         if (isEnlargingSpace && (maxWidth > MINIMAL_SIZE))
         {
            constraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            constraints.weightx = 1.0;
         }
         else
         {
            constraints.fill = java.awt.GridBagConstraints.NONE;
         }
         super.add(Box.createHorizontalStrut(GUI.HorizontalLabelDistance), constraints);

         // constraints for main JComponent

         constraints.gridx++;
         constraints.anchor = fieldAlignment;

         // get the JComponent

         JComponent component = (JComponent) fields.elementAt(i);

         // check if size is <= MINIMAL_SIZE <=> only border is drawn

         if (component.getPreferredSize().width > MINIMAL_SIZE)
         {
            constraints.fill = java.awt.GridBagConstraints.NONE;
         }
         else
         {
            constraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         }

         if (maxWidth > MINIMAL_SIZE)
         {
            constraints.weightx = 0.0;
         }
         else
         {
            constraints.weightx = 1.0;
         }

         super.add(component, constraints);

         ypos++;
      }

      if (!isEnlargingSpace && max > 0)
      {
         constraints.gridx = STARTING_POSITION + 3;
         constraints.gridy = 1;
         constraints.weightx = 1.0;
         constraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
         super.add(Box.createHorizontalGlue(), constraints);
      }
   }

   /**
    * Wrap component for mandatory padding.
    */
   private JComponent wrap(JComponent component)
   {
      return component instanceof Entry || component instanceof MandatoryWrapper ?
            component : new MandatoryWrapper(component);
   }

   /**
    * set the "readonly"-flag for all contained component
    */
   public void setReadonly(boolean isReadonly)
   {
      if (readOnly != isReadonly)
      {
         readOnly = isReadonly;
         for (int i = 0; i < fields.size(); i++)
         {
            Component c = (Component) fields.get(i);
            if (c instanceof Entry)
            {
               ((Entry) c).setReadonly(isReadonly);
            }
         }
      }
   }

   public boolean isReadonly()
   {
      return readOnly;
   }
}
