// https://searchcode.com/api/result/141712/

/*
 * QuizWindow.java
 *
 * Copyright (C) 2011 Thomas Everingham
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * A copy of the GNU General Public License can be found in the file
 * LICENSE.txt provided with the source distribution of this program (see
 * the META-INF directory in the source jar). This license can also be
 * found on the GNU website at http://www.gnu.org/licenses/gpl.html.
 *
 * If you did not receive a copy of the GNU General Public License along
 * with this program, contact the lead developer, or write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * If you find this program useful, please tell me about it! I would be delighted
 * to hear from you at tom.ingatan@gmail.com.
 */
package org.ingatan.component.quiztime;

import org.ingatan.ThemeConstants;
import org.ingatan.component.answerfield.AnsFieldMultiChoice;
import org.ingatan.component.answerfield.AnsFieldSimpleText;
import org.ingatan.component.answerfield.IAnswerField;
import org.ingatan.component.librarymanager.FlexiQuestionContainer;
import org.ingatan.component.text.RichTextArea;
import org.ingatan.data.FlexiQuestion;
import org.ingatan.data.IQuestion;
import org.ingatan.data.TableQuestion;
import org.ingatan.io.IOManager;
import org.ingatan.io.ParserWriter;
import org.ingatan.io.QuizManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * The quiz window shows one question at a time, provides a 'next question' button,
 * and a panel showing statistics for the current quiz, such as the number of questions
 * answered, remaining, the number of points awarded so far, the average grade of answers, etc.
 *
 * @author Thomas Everingham
 * @version 1.0
 */
public class QuizWindow extends JFrame implements WindowListener {

    /**
     * Score bonus given for attempting a question, regardless of whether or not
     * the answer was correct.
     */
    private static final int BASE_SCORE_BONUS_PER_QUESTION = 2;
    /**
     * How 'badly' the user has to do with a question before it starts subtacting from their score. The
     * subtraction only occurs if the score is less than what it has been historically (i.e. not an improvement).
     */
    private static final float PENALTY_THRESHOLD = 0.85f;
//    /**
//     * When a question template is used, the following style is applied to the 'question' text, where the rest
//     * of the template is left as plain text. These are the opening tags.
//     */
//    private static final String QUESTION_TEMPLATE_STYLE_OPEN = "[" + RichTextArea.TAG_FONT_SIZE + "]15[!" + RichTextArea.TAG_FONT_SIZE + "][b]";
//    /**
//     * The closing tags for the question template 'question' formatting. See QUESTION_TEMPLATE_STYLE_OPEN.
//     */
//    private static final String QUESTION_TEMPLATE_STYLE_CLOSE = "[" + RichTextArea.TAG_FONT_SIZE + "]11[!" + RichTextArea.TAG_FONT_SIZE + "][b]";
    /**
     * Displays the question text.
     */
    private RichTextArea questionArea = new RichTextArea();
    /**
     * Displays the answer text.
     */
    private RichTextArea answerArea = new RichTextArea();
    /**
     * Used to grade the question and show the post data text, or show the next question
     */
    private JButton btnContinue = new JButton(new ContinueAction());
    /**
     * Used to skip the current question and load the next question.
     */
    private JButton btnSkip = new JButton(new SkipAction());
    /**
     * This window is set visible before the QuizWindow is disposed.
     */
    private JFrame returnToOnClose;
    /**
     * Content pane for the quiz window.
     */
    private JPanel contentPane = new JPanel();
    /**
     * Label that displays where the currently displayed question is taken from.
     */
    private JLabel lblQuestionFrom;
    /**
     * The quiz manager taking care of question loading, sorting, and saving, etc.
     */
    private QuizManager quizManager;
    /**
     * JPanel with a painted border, used as the side panel to display statistics and
     * the user's current score.
     */
    private JPanel sideBar = new JPanel();
    /**
     * The currently displayed question.
     */
    private IQuestion currentQuestion;
    /**
     * Whether or not the question is being displayed (<code>true</code> value) or
     * the post-answer text is being displayed.
     */
    private boolean isShowingQuestion = true;
    /**
     * Keeps track of how many questions have been asked since the quiz started.
     */
    private int totalQuestionsAnswered = 0;
    /**
     * Keeps track of how many questions have been skipped since the quiz started.
     */
    private int totalQuestionsSkipped = 0;
    /**
     * Keeps track of how many marks have been available for the user to win.
     */
    private int totalMarksAvailable = 0;
    /**
     * Keeps track of the total number of marks that have been awarded to the user.
     */
    private int totalMarksAwarded = 0;
    /**
     * Keeps track of the user's score so far.
     */
    private int totalScore = 0;
    /**
     * Is incremeneted until the user gets less than 0.95 for a question and it is
     * reset to 0. When scoring, if the combo counter is > 3, a bonus is added.
     */
    private int comboCounter = 0;
    /**
     * Counts how many times individual combos have occurred.
     */
    private int comboIncidenceCounter = 0;
    /**
     * This is set when the end screen is shown. The continue button checks this flag
     * and will dispose the quiz window upon click event if this flag is set to <code>true</code>.
     */
    private boolean quizHasEnded = false;
    /**
     * Keeps track of how may times the user improved on the historic correctness of a question.
     */
    private int improvementCount = 0;
    /**
     * The number of answer fields that currently exist in the answer text area. This is set when
     * a new question is displayed.
     */
    private ArrayList<IAnswerField> currentAnswerFields = new ArrayList<IAnswerField>();
    /**
     * Used in the displayNextQuestion method. This is a workaround for resizing the question and
     * answer areas properly, until a better solution can be found. The areas only truly resize
     * nicely when the quiz window is resized. This variable is multiplied by -1 each time it is used,
     * and each time a question is asked, the quiz window is resized by adding this value (1 or -1). This
     * keeps the window at the same size, but also means the text fields are resized appropriately.
     */
    private int sizeStep = -1;

    /**
     * Creates a new instance of QuizWindow with the specified owner. The owner is the
     * JFrame that is set visible when the quiz window is closed.
     *
     * @param q the QuizManager for this quiz. Every quiz must have a manager to provide the questions, etc.
     * @param owner this window is set visible upon the closing of this QuizWindow.
     */
    public QuizWindow(QuizManager q, JFrame owner) {

        //if the user has constructed an empty quiz, then end it straight away with a message
        if (q.hasMoreQuestions() == false) {
            this.setVisible(false);
            owner.setVisible(true);
            JOptionPane.showMessageDialog(owner, "There are no questions in the selected libraries to be\nasked!", "No Questions", JOptionPane.INFORMATION_MESSAGE);
            this.setVisible(false);
            this.dispose();
            return;
        }

        this.setLocation(450, 100);

        this.returnToOnClose = owner;
        this.quizManager = q;
        this.addWindowListener(this);
        this.setContentPane(contentPane);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setTitle("Quiz Time");
        this.setIconImage(IOManager.windowIcon);
        setUpGUI();
        setUpInputMap();
        displayNextQuestion();
    }

    /**
     * Woot.
     */
    private void setUpGUI() {
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        //create a heading
        JLabel heading = new JLabel("Quiz Time");
        heading.setFont(new Font(contentPane.getFont().getFamily(), Font.PLAIN, 26));
        heading.setHorizontalAlignment(SwingConstants.LEFT);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        heading.setForeground(new Color(70, 70, 70));
        lblQuestionFrom = new JLabel("Question from library __ in group __.");
        lblQuestionFrom.setFont(new Font(contentPane.getFont().getFamily(), Font.PLAIN, 10));
        lblQuestionFrom.setHorizontalAlignment(SwingConstants.LEFT);
        lblQuestionFrom.setAlignmentX(LEFT_ALIGNMENT);
        lblQuestionFrom.setForeground(new Color(70, 70, 70));

        questionArea.getScroller().setAlignmentX(LEFT_ALIGNMENT);
        questionArea.setOpaque(false);
        questionArea.getScroller().setOpaque(false);
        questionArea.setToolbarVisible(false);
        questionArea.setEditable(false);
        questionArea.getScroller().setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, ThemeConstants.borderUnselected));

        answerArea.getScroller().setAlignmentX(LEFT_ALIGNMENT);
        answerArea.setOpaque(false);
        answerArea.getScroller().setOpaque(false);
        answerArea.getScroller().setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, ThemeConstants.borderUnselected));
        answerArea.setToolbarVisible(false);
        answerArea.setEditable(false);
        btnContinue.setAlignmentX(LEFT_ALIGNMENT);
        btnContinue.setToolTipText("Hotkey: Shift+Enter or Ctrl+Enter");
        btnSkip.setToolTipText("Hotkey: Ctrl+S");

        sideBar.setMaximumSize(new Dimension(300, 1300));
        sideBar.setMinimumSize(new Dimension(200, 500));
        sideBar.setPreferredSize(new Dimension(200, 500));
        sideBar.setBorder(BorderFactory.createLineBorder(ThemeConstants.borderUnselected));

        contentPane.add(heading);
        contentPane.add(Box.createVerticalStrut(30));
        contentPane.add(lblQuestionFrom);
        contentPane.add(Box.createVerticalStrut(5));
        contentPane.add(questionArea.getScroller());
        contentPane.add(Box.createVerticalStrut(25));
        contentPane.add(answerArea.getScroller());
        contentPane.add(Box.createVerticalStrut(25));

        Box horiz = Box.createHorizontalBox();
        horiz.add(btnContinue);
        horiz.add(Box.createHorizontalStrut(15));
        horiz.add(btnSkip);
        horiz.setAlignmentX(LEFT_ALIGNMENT);
        horiz.setMaximumSize(new Dimension(2000, 40));
        contentPane.add(horiz);

        this.setPreferredSize(new Dimension(500, 600));
        this.pack();

    }

    private void setUpInputMap() {
        InputMap inMap = this.getRootPane().getInputMap(JRootPane.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap aMap = this.getRootPane().getActionMap();

        inMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "ContinueAction");
        inMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "ContinueAction");
        inMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "SkipAction");

        aMap.put("ContinueAction", new ContinueAction());
        aMap.put("SkipAction", new SkipAction());


        this.getRootPane().setInputMap(JRootPane.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inMap);
        this.getRootPane().setActionMap(aMap);
    }

    /**
     * Asks the QuizManager for the next question to display, and sets the data
     * to the question area and answer areas.
     */
    private void displayNextQuestion() {
        //get the next question from the quiz manager
        currentQuestion = quizManager.getNextQuestion();

        //build the string that states what library the question is from and what groups the library is contained by.
        lblQuestionFrom.setText("Question from library '" + IOManager.getLibraryName(currentQuestion.getParentLibrary()) + "' in group");
        String[] groups = IOManager.getGroupsThatContain(currentQuestion.getParentLibrary());
        if (groups.length > 1) {
            String txtAppend = "s ";
            for (int i = 0; i < groups.length; i++) {
                if (i == 0) {
                    txtAppend = txtAppend + " " + groups[i];
                } else if (i < groups.length - 1) {
                    txtAppend = txtAppend + ", " + groups[i];
                } else {
                    txtAppend = txtAppend + " and " + groups[i];
                }
            }
            lblQuestionFrom.setText(lblQuestionFrom.getText() + txtAppend);
        } else if (groups.length == 1) {
            lblQuestionFrom.setText(lblQuestionFrom.getText() + " " + groups[0]);
        } else {
            lblQuestionFrom.setText("Question from library '" + IOManager.getLibraryName(currentQuestion.getParentLibrary()) + "' from the default 'all libraries' group.");
        }

        //the label is set invisible when showing post text or answer.
        lblQuestionFrom.setVisible(true);

        //the question is either a flexi question or Table-Question-Unit. Table
        //questions themselves are not added to the quiz manager.
        if (currentQuestion instanceof FlexiQuestion) {
            questionArea.setRichText(((FlexiQuestion) currentQuestion).getQuestionText());
            answerArea.setRichText(((FlexiQuestion) currentQuestion).getAnswerText());
            answerArea.setCaretPosition(0);
            //contextualisation of answer fields: the answer and question areas are traversed, and any answer field found is told
            //that it is in quiz mode through the setContext method that exists in the IAnswerField interface.
            contextualiseAnswerFields(answerArea);
            contextualiseAnswerFields(questionArea);
        } else if (currentQuestion instanceof TableQuestionUnit) {
            TableQuestionUnit ques = (TableQuestionUnit) currentQuestion;
            String questionText = "";
            String questionTemplate = "";
            //if asking in reverse is allowed, it will occur randomly. This flag sets whether
            //we will be asking in reverse for this particular question (flag is set below).
            //this is needed so that further down in this method we still know whether or not to take
            //data from the question or answer column.
            boolean askInReverse = false;

            //check whether it is okay to ask in reverse - doesn't necessarily mean ask in reverse, must be randomised
            if (ques.getParentTableQuestion().isAskInReverse()) {
                //if more than 5, then ask in forward direction
                if (IOManager.random.nextInt(11) > 5) {
                    questionText = chooseRandomEntry(ques.getQuestionColumnData());
                    questionTemplate = ques.getParentTableQuestion().getQuestionTemplateFwd();
                } //otherwise as in backward direction
                else {
                    askInReverse = true;
                    questionText = chooseRandomEntry(ques.getAnswerColumnData());
                    questionTemplate = ques.getParentTableQuestion().getQuestionTemplateBwd();
                }
            } else { //it is not okay to ask in reverse, only ask forward
                questionText = chooseRandomEntry(ques.getQuestionColumnData());
                questionTemplate = ques.getParentTableQuestion().getQuestionTemplateFwd();
            }
            
            //question text will be loaded into a RichTextArea, so ensure that the square brackets are expressed as RichTextArea markup charcodes
            questionText = questionText.replace("[", RichTextArea.CHARCODE_OPENING_SQUARE_BRACKET).replace("]", RichTextArea.CHARCODE_CLOSING_SQUARE_BRACKET);

            //set question text to include the question template if we can.
            if ((questionTemplate.trim().isEmpty() == false) && (questionTemplate.contains("[q]"))) {
                //so we can restore the font for the rest of the template.
                String fontName = questionArea.getFont().getFamily();
                int fontSize = questionArea.getFont().getSize();

                //tags used to make the question phrase a larger font than the question template text
                String insertText = "[" + RichTextArea.TAG_FONT_FAMILY + "]" + ques.getParentTableQuestion().getFontFamilyName() + "[!" + RichTextArea.TAG_FONT_FAMILY + "]"
                        + "[" + RichTextArea.TAG_FONT_SIZE + "]" + ques.getParentTableQuestion().getFontSize() + "[!" + RichTextArea.TAG_FONT_SIZE + "]"
                        + questionText
                        + "[" + RichTextArea.TAG_FONT_FAMILY + "]" + fontName + "[!" + RichTextArea.TAG_FONT_FAMILY + "]"
                        + "[" + RichTextArea.TAG_FONT_SIZE + "]" + fontSize + "[!" + RichTextArea.TAG_FONT_SIZE + "]";

                questionText = questionTemplate.replace("[q]", insertText) + "[" + RichTextArea.TAG_DOCUMENT_END + "]";
            } else {
                questionText = "[" + RichTextArea.TAG_FONT_FAMILY + "]" + ques.getParentTableQuestion().getFontFamilyName() + "[!" + RichTextArea.TAG_FONT_FAMILY + "]"
                        + "[" + RichTextArea.TAG_FONT_SIZE + "]" + ques.getParentTableQuestion().getFontSize() + "[!" + RichTextArea.TAG_FONT_SIZE + "]"
                        + questionText
                        + "[" + RichTextArea.TAG_DOCUMENT_END + "]";
            }

            //now create an appropriate answer field and set questionArea and answerArea text
            questionArea.setRichText(questionText);
            answerArea.setText("");
            //check for the type of answer required (MultiChoice, Written, or Both)
            if (ques.getParentTableQuestion().getQuizMethod() == TableQuestion.WRITTEN) {
                //insert a simple text field answer field
                answerArea.insertComponent(generateSimpleTextField(ques, askInReverse));
            } else if (ques.getParentTableQuestion().getQuizMethod() == TableQuestion.MULTI_CHOICE) {
                //insert a multi choice answer field
                answerArea.insertComponent(generateMultiChoiceField(ques, askInReverse));
            } else if (ques.getParentTableQuestion().getQuizMethod() == TableQuestion.RANDOM) {
                //    -if both, then do the random number between 1 and 10, >5 is multichoice
                //Add the appropriate answer field
                if (IOManager.random.nextInt(11) > 5) {
                    //insert a simple text field answer field
                    answerArea.insertComponent(generateSimpleTextField(ques, askInReverse));
                } else {
                    //insert a multi choice answer field.
                    answerArea.insertComponent(generateMultiChoiceField(ques, askInReverse));
                }
            }

        }

        //clear the currentAnswerFields array, this is repopulated in the setAnswerFieldListener method.
        currentAnswerFields = new ArrayList<IAnswerField>();
        /*
         * Sets answer field listeners to all answer fields in the answer text area. It used to be such that the listener
         * was only added if there was only one answer field in the answer text area. Now, when a listener is fired, if there
         * is only 1 answer field in the area, then the ContinueAction is called, otherwise the next field is focussed.
         *
         * This is also where the currentAnswerFields array list is populated.
         */
        setAnswerFieldListener();
        //request focus for the top-most answer field in the answer field area.
        focusFirstAnswerField();

        answerArea.setCaretPosition(0);
        questionArea.setCaretPosition(0);

        resetSize(answerArea);
        resetSize(questionArea);

        //resize the quiz window to ensure answer and question areas are a nice size. This
        //is a workaround until a better solution is found. See stepSize variable's javadoc comment
        //for more info.
        sizeStep = sizeStep * (-1);
        this.setSize(this.getWidth() + sizeStep, this.getHeight() + sizeStep);
    }

    /**
     * Generates a simple text field for a table unit question. Accepts any of the possible correct answers provided
     * by the TableQuestionUnit. The 'correct answer' values are taken from the column corresponding to <code>askInReverse</code>. If
     * this flag is <code>true</code> then the answers are taken from the question column, otherwise they are taken from the answer column.
     * If any of the correct answers are specified by the user, then the value given by <code>ques.getParentTableQuestion().getMarksPerCorrectAnswer()</code> is awarded.
     * @param ques The table question from which this simple text answer field should be generated.
     * @param askInReverse <code>true</code> if the field should be created using reverse values (question column for the answers).
     * @return a simple text answer field which has all the specified data set to it, and is ready to show to the user in quiz time.
     */
    private AnsFieldSimpleText generateSimpleTextField(TableQuestionUnit ques, boolean askInReverse) {
        String[] answers;
        //the source of the answers array depends on whether or not we are asking in reverse this time
        if (askInReverse) {
            answers = ques.getQuestionColumnData();
        } else {
            answers = ques.getAnswerColumnData();
        }

        AnsFieldSimpleText retField = new AnsFieldSimpleText();
        retField.setValues(answers, ques.getParentTableQuestion().getMarksPerCorrectAnswer());
        retField.setContext(false);
        return retField;
    }

    /**
     * Generates a multiple choice field for a table question unit. Choses random values from possible questions/answers for this particular
     * table unit (if there are multiple entered). Asks the parent <code>TableQuestion</code> for the complete list
     * of column data and depending on whether or not askInReverse is true, takes three possible values which are not
     * the correct answer, randomly. If there are not enough options to generate 4 options, then the defualt values used
     * are, in this order, Maisy, Louella, Hugh. Because it is unlikely that there will be less than 4 entries in a <code>TableQuestion</code>,
     * this was seen as a fun way to do it.
     * @param ques the <code>TableQuestionUnit</code> from which to generate this question.
     * @param askInReverse whether or not the question should be asked in reverse has already been determined, so if <code>true</code> this parameter tells this method
     * to definitely ask in reverse. Otherwise (if <code>false</code>) this parameter tells this method to definitely ask in the forward direction.
     * @return a multiple choice answer field which has had all required data set to it. All options receive 0 marks if chosen except for the correct answer option which
     * receives whatever the TableUnitQuestion's parent says is the value for "marks per correct answer".
     */
    private AnsFieldMultiChoice generateMultiChoiceField(TableQuestionUnit ques, boolean askInReverse) {
        //first, get a random correct answer from the possible correct answers in the list
        String answerOption;
        ArrayList<String> incorrectOptions;
        ArrayList<String> mcOptions = new ArrayList<String>();
        //the source of the answers array depends on whether or not we are asking in reverse this time
        if (askInReverse) {
            answerOption = chooseRandomEntry(ques.getQuestionColumnData());
        } else {
            answerOption = chooseRandomEntry(ques.getAnswerColumnData());
        }
        //now we need three options from elsewhere in the table
        if (askInReverse) {
            incorrectOptions = ques.getParentTableQuestion().getCol1DataArrayList();
        } else {
            incorrectOptions = ques.getParentTableQuestion().getCol2DataArrayList();
        }
        //get rid of the correct answer
        incorrectOptions.remove(ques.getIndexInTableQuestion());
        //ensure we have enough values to add three incorrect options
        String[] names = new String[]{"Maisy", "Louella", "Hugh"};
        int i = 0;
        while (incorrectOptions.size() < 3) {
            incorrectOptions.add(names[i++]);
        }
        //shuffle the incorrect options and take the first three... seems the easiest way
        Collections.shuffle(incorrectOptions, IOManager.random);
        mcOptions.add(answerOption);
        mcOptions.add(chooseRandomEntry(incorrectOptions.get(0).split(",,")));
        mcOptions.add(chooseRandomEntry(incorrectOptions.get(1).split(",,")));
        mcOptions.add(chooseRandomEntry(incorrectOptions.get(2).split(",,")));

        //shuffle our final multiple choice options
        Collections.shuffle(mcOptions, IOManager.random);

        float[] pctMarks = new float[4];
        Arrays.fill(pctMarks, 0.0f);
        pctMarks[mcOptions.indexOf(answerOption)] = 1.0f;

        //create the answer field
        AnsFieldMultiChoice retField = new AnsFieldMultiChoice();
        Arrays.toString(mcOptions.toArray(new String[mcOptions.size()]));
        retField.setSimpleOptions(mcOptions.toArray(new String[mcOptions.size()]), pctMarks, ques.getParentTableQuestion().getMarksPerCorrectAnswer());
        retField.setParentLibraryID(ques.getParentLibrary());

        return retField;
    }

    /**
     * Traverses the answer area and finds all answer fields. Adds the answer field listener to each
     * answer field found.
     */
    public void setAnswerFieldListener() {
        //traverse the answer text area until we find an answer field
        Element curEl;
        AttributeSet curAttr;
        int runCount;

        for (int i = 0; i < answerArea.getDocument().getDefaultRootElement().getElementCount(); i++) {
            //each paragraph has 'runCount' runs
            runCount = answerArea.getDocument().getDefaultRootElement().getElement(i).getElementCount();
            for (int j = 0; j < runCount; j++) {
                curEl = answerArea.getDocument().getDefaultRootElement().getElement(i).getElement(j);
                curAttr = curEl.getAttributes();

                if (curEl.getName().equals(StyleConstants.ComponentElementName)) {
                    //this run is a component. May be an answer field, picture or math text component.
                    Component o = (Component) curAttr.getAttribute(StyleConstants.ComponentAttribute);
                    if (o instanceof IAnswerField) {
                        //add the ContinueActionListener
                        ((IAnswerField) o).setQuizContinueListener(new ContinueActionListener());
                        currentAnswerFields.add((IAnswerField) o);
                    }
                }
            }
        }

    }

    /**
     * This find the first (top most) answer field in the answer text area and
     * calls its .requestFocus() method. If an answer field wants to handle this
     * event in a special way (for example pass focus to the correct sub-component) then
     * this can be done by overriding the <code>requestFocus</code> method.
     */
    public void focusFirstAnswerField() {
        //traverse the answer text area until we find an answer field
        Element curEl;
        AttributeSet curAttr;
        int runCount;

        for (int i = 0; i < answerArea.getDocument().getDefaultRootElement().getElementCount(); i++) {
            //each paragraph has 'runCount' runs
            runCount = answerArea.getDocument().getDefaultRootElement().getElement(i).getElementCount();
            for (int j = 0; j < runCount; j++) {
                curEl = answerArea.getDocument().getDefaultRootElement().getElement(i).getElement(j);
                curAttr = curEl.getAttributes();

                if (curEl.getName().equals(StyleConstants.ComponentElementName)) {
                    //this run is a component. May be an answer field, picture or math text component.
                    Component o = (Component) curAttr.getAttribute(StyleConstants.ComponentAttribute);
                    if (o instanceof IAnswerField) {
                        //set focus to the answer field
                        o.requestFocus();
                        return;
                    }
                }
            }
        }
    }

    /**
     * Chooses an entry within the specified array at random using the IOManager's
     * <code>Random</code> instance to randomly choose an index between 0 (inclusive) and
     * the array's length (exclusive).
     * @param array the array to choose an entry form.
     * @return the randomly chosen entry from <code>array</code>.
     */
    private String chooseRandomEntry(String[] array) {
        return array[IOManager.random.nextInt(array.length)];
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        //quiz has ended, so exit is cool
        if (quizHasEnded) {
            returnToOnClose.setVisible(true);
            //save the quiz history entries to the library (these have been accumulated by the quiz manager throughout the quiz).
            quizManager.commitLibraryHistoryEntries();
            //repack libraries and flush temp directory.
            try {
                IOManager.cleanUpAndRepackage();
            } catch (IOException ex) {
                Logger.getLogger(QuizWindow.class.getName()).log(Level.SEVERE, "Quiz window closing event. Trying to cleanup+repack.", ex);
            }
            QuizWindow.this.dispose();
            return;
        }

        int resp;
        if (totalQuestionsAnswered == 0) {
            //otherwise, make sure that the user wants to finish up - if so, we'll first show the final screen
            resp = JOptionPane.showConfirmDialog(QuizWindow.this, "Are you sure you wish to end the quiz?\n\n"
                    + "Note: no questions have been answered, so no record will be made.", "End Quiz?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (resp == JOptionPane.YES_OPTION) {
                returnToOnClose.setVisible(true);
                QuizWindow.this.dispose();
            }
            return;
        } else {
            resp = JOptionPane.showConfirmDialog(QuizWindow.this, "Are you sure you wish to end the quiz?", "End Quiz?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        }


        if (resp == JOptionPane.YES_OPTION) {
            quizHasEnded = true;
            totalQuestionsSkipped += quizManager.getCurrentQuestionCount();

            //if currently showing a question, then this question will not be answered, but has
            //already been taken from the quizmanager, so wasn't included in the getCurrentQuestionCount method.
            if (isShowingQuestion) {
                totalQuestionsSkipped++;
            }

            showEndScreen();
        }
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    /**
     * When the user presses the Skip button. Usually skips the
     * question without showing feedback, but a special case occurs where there
     * are no more questions to ask, and in this case the isShowingQuestion flag
     * is set false and the continueAction event is fired. This takes care of showing
     * the end screen for the quiz, etc.
     */
    private class SkipAction extends AbstractAction {

        public SkipAction() {
            super("Skip");
        }

        public void actionPerformed(ActionEvent e) {
            if (isShowingQuestion == false) {
                return;
            }

            totalQuestionsSkipped++;
            if (quizManager.hasMoreQuestions()) {
                displayNextQuestion();
            } else { //in this case, no questions remain and the continue menu will show the final screen for us
                isShowingQuestion = false;
                new ContinueAction().actionPerformed(null);
            }
        }
    }

    /**
     * Traverses the elements of the answerArea <code>RichTextArea</code> and tells
     * all IAnswerField components found that they exist in the quizTime context.
     */
    private void contextualiseAnswerFields(RichTextArea textArea) {
        int runCount;
        int paragraphCount = textArea.getDocument().getDefaultRootElement().getElementCount();
        Element curEl = null;
        AttributeSet curAttr = null;
        AttributeSet prevAttr = null;

        for (int i = 0; i < paragraphCount; i++) {
            //each paragraph has 'runCount' runs
            runCount = textArea.getDocument().getDefaultRootElement().getElement(i).getElementCount();
            for (int j = 0; j < runCount; j++) {
                curEl = textArea.getDocument().getDefaultRootElement().getElement(i).getElement(j);
                curAttr = curEl.getAttributes();

                if (curEl.getName().equals(StyleConstants.ComponentElementName)) //this is a component
                {
                    //this run is a component. May be an answer field, picture or math text component.
                    Component o = (Component) curAttr.getAttribute(StyleConstants.ComponentAttribute);
                    if (o instanceof IAnswerField) {
                        ((IAnswerField) o).setContext(false);
                    }
                }
            }
        }
    }

    /**
     * Resizes the specified text area to a size that makes sense based on the content of the field.
     * @param txtArea
     */
    public void resetSize(RichTextArea txtArea) {
        try {
            //get the height of the 'document'
            javax.swing.text.Document doc = txtArea.getDocument();
            Dimension d = txtArea.getPreferredSize();
            Rectangle r = txtArea.modelToView(doc.getLength());

            //create a new height
            d.height = r.y + r.height + txtArea.getToolbar().getHeight();

            //ensure that we are not setting a smaller height than the scroller's minimum height
            if (d.getHeight() < txtArea.getScroller().getMaximumSize().getHeight()) {
                if (d.getHeight() < txtArea.getScroller().getMinimumSize().getHeight()) {
                    d.height = (int) txtArea.getScroller().getMinimumSize().getHeight();
                }
                txtArea.getScroller().setPreferredSize(d);
            } else { //and ensure not setting a larger size than the maximum scroller size.
                txtArea.getScroller().setPreferredSize(txtArea.getScroller().getMaximumSize());
            }

            txtArea.getScroller().validate();
            QuizWindow.this.validate();

        } catch (Exception e2) {
            Logger.getLogger(FlexiQuestionContainer.class.getName()).log(Level.WARNING, "Could not resize the text area to fit content.", e2);
        }
    }

    /**
     * Listens to answer fields, and triggers the ContinueAction action if an answer field
     * fires this listener. This will occur when the answer field has received a logical input that
     * this should occur, as determined by the answer field programmer, but only if there is only 1 answer
     * field currently in the answer text area. If there are others, the user will still need to see to those
     * answer fields, and so if this listener is fired all that occurs is that focus is shifted to the next listener
     * in the answer area.
     */
    private class ContinueActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (currentAnswerFields.size() == 1) {
                //continue action
                new ContinueAction().actionPerformed(null);
            } else if (currentAnswerFields.size() > 1) {
                //focus change
                int index = currentAnswerFields.indexOf(e.getSource());
                if (index < currentAnswerFields.size() - 1) {
                    ((JComponent) currentAnswerFields.get(index + 1)).requestFocus();
                } else if (index == currentAnswerFields.size() - 1) {
                    ((JComponent) currentAnswerFields.get(0)).requestFocus();
                }
            }
        }
    }

    /**
     * Action performed when the 'Continue' button is pressed. Will grade the
     * question and then either load the next question or show the post answer
     * text.
     */
    private class ContinueAction extends AbstractAction {

        public ContinueAction() {
            super("Continue");
        }

        public void actionPerformed(ActionEvent e) {
            QuizWindow.this.getRootPane().requestFocus();

            //first, ensure quiz is still going
            if (quizHasEnded) {
                //quiz has ended, everything has been saved since this flag has been set
                //so exit
                returnToOnClose.setVisible(true);
                QuizWindow.this.setVisible(false);
                //commit any history entries to the corresponding library before repacking
                quizManager.commitLibraryHistoryEntries();
                //repack libraries and flush temp directory.
                try {
                    IOManager.cleanUpAndRepackage();
                } catch (IOException ex) {
                    Logger.getLogger(QuizWindow.class.getName()).log(Level.SEVERE, "User pressed Continue button at end of quiz. Trying to cleanup+repack.", ex);
                }
                QuizWindow.this.dispose();
                return;
            }

            //tally just for this particular question. If it is a flexi question,
            //these values will be saved to it, otherwise, the
            int marksAwarded = 0;
            int marksAvailable = 0;
            float previousCorrectness = 0;
            int addToScore = 0;
            boolean isCombo = false;

            //if the question is currently being displayed, we must now mark the answer
            //and display post text
            if (isShowingQuestion) {
                btnSkip.setEnabled(false);
                isShowingQuestion = false;
                lblQuestionFrom.setVisible(false);

                //------mark the question---------
                //      -traverse the answer text area and look for answer fields
                Element curEl;
                AttributeSet curAttr;
                int runCount;

                for (int i = 0; i < answerArea.getDocument().getDefaultRootElement().getElementCount(); i++) {
                    //each paragraph has 'runCount' runs
                    runCount = answerArea.getDocument().getDefaultRootElement().getElement(i).getElementCount();
                    for (int j = 0; j < runCount; j++) {
                        curEl = answerArea.getDocument().getDefaultRootElement().getElement(i).getElement(j);
                        curAttr = curEl.getAttributes();

                        if (curEl.getName().equals(StyleConstants.ComponentElementName)) {
                            //this run is a component. May be an answer field, picture or math text component.
                            Component o = (Component) curAttr.getAttribute(StyleConstants.ComponentAttribute);
                            if (o instanceof IAnswerField) {
                                IAnswerField ansField = (IAnswerField) o;
                                //      -have a running total of what those answer fields say their max and acheived marks are
                                marksAwarded += ansField.getMarksAwarded();
                                marksAvailable += ansField.getMaxMarks();
                                //      -tell each answer field to show its answers as they are traversed
                                ansField.displayCorrectAnswer();
                            }
                        }
                    }
                }

                float correctness;
                if (marksAvailable == 0) {
                    correctness = 0;
                } else {
                    correctness = ((float) marksAwarded / (float) marksAvailable);
                }

                //is this a combo?
                if (correctness >= 0.95) {
                    comboCounter += 1;
                    if (comboCounter >= 4) {
                        isCombo = true;
                    }
                } else {
                    comboCounter = 0;
                }


                //      -update the score
                addToScore += BASE_SCORE_BONUS_PER_QUESTION;
                addToScore += marksAwarded;
                float improvementValue = 0;
                if (currentQuestion instanceof FlexiQuestion) {
                    previousCorrectness = ((FlexiQuestion) currentQuestion).getCorrectness();
                } else if (currentQuestion instanceof TableQuestionUnit) {
                    previousCorrectness = ((TableQuestionUnit) currentQuestion).getHistoricCorrectness();
                }
                //if this value is positive, then the user has improved on the last time this question has been asked.
                //if it is negative, then the user has not improved. If the correctness for the latest answer is =< 0.85 (85%), then
                //the decrease is deducted from the score (multiple of the BASE_SCORE_BONUS_PER_QUESTION. Similarly, a multiple of the
                //base score is added if the user has improved.
                improvementValue = correctness - previousCorrectness;
                if (improvementValue > 0) {
                    addToScore += BASE_SCORE_BONUS_PER_QUESTION * (improvementValue);
                    improvementCount++;
                } else if (correctness <= PENALTY_THRESHOLD) {
                    //reduce score: improvement value already negative, so add rather than subtract
                    addToScore += BASE_SCORE_BONUS_PER_QUESTION * (improvementValue);
                }

                if (isCombo) {
                    comboIncidenceCounter++;
                    addToScore += (int) (comboCounter / 2);
                }


                //set the timesAsked, marksAwarded, and marksAvailable values in the question before saving
                if (currentQuestion instanceof FlexiQuestion) {
                    FlexiQuestion q = (FlexiQuestion) currentQuestion;
                    q.setMarksAvailable(q.getMarksAvailable() + marksAvailable);
                    q.setMarksAwarded(q.getMarksAwarded() + marksAwarded);
                    q.setTimesAsked(q.getTimesAsked() + 1);
                } else if (currentQuestion instanceof TableQuestionUnit) {
                    TableQuestionUnit q = (TableQuestionUnit) currentQuestion;
                    q.getParentTableQuestion().setMarksAvailable(q.getIndexInTableQuestion(), q.getParentTableQuestion().getMarksAvailable()[q.getIndexInTableQuestion()] + marksAvailable);
                    q.getParentTableQuestion().setMarksAwarded(q.getIndexInTableQuestion(), q.getParentTableQuestion().getMarksAwarded()[q.getIndexInTableQuestion()] + marksAwarded);
                    q.getParentTableQuestion().setTimesAsked(q.getIndexInTableQuestion(), q.getParentTableQuestion().getTimesAsked()[q.getIndexInTableQuestion()] + 1);
                }

                //------save the question---------
                try {
                    quizManager.updateLibrary(currentQuestion, marksAwarded, marksAvailable, improvementValue);
                } catch (IOException ex) {
                    Logger.getLogger(QuizWindow.class.getName()).log(Level.SEVERE, "While saving the current question with updates for times asked, score, etc. (quiz window)", ex);
                }

                //update class variables (these are the tallies shown to the user during the quiz and at the end)
                totalMarksAvailable += marksAvailable;
                totalMarksAwarded += marksAwarded;
                totalQuestionsAnswered++;
                totalScore += addToScore;

                //------display post answer screen
                //      -check if we need to display post answer text, if so, display it in the question area
                if (currentQuestion instanceof FlexiQuestion) {
                    FlexiQuestion q = (FlexiQuestion) currentQuestion;
                    if (q.isUsingPostAnswerText()) {
                        questionArea.setRichText(q.getPostAnswerText());
                        contextualiseAnswerFields(questionArea);
                    }
                }

                //      -display marks for that question out of possible
                //the offset of text insertion from location 0 in the StyledDocument
                int offset = 0;
                String insertionText;
                try {
                    SimpleAttributeSet attributes = new SimpleAttributeSet();
                    StyleConstants.setFontFamily(attributes, contentPane.getFont().getFamily());
                    StyleConstants.setFontSize(attributes, 18);
                    StyleConstants.setForeground(attributes, ThemeConstants.textColour);
                    insertionText = marksAwarded + "/" + marksAvailable + " - ";
                    answerArea.getStyledDocument().insertString(offset, insertionText, attributes);
                    offset += insertionText.length();

                    if (correctness >= 0.5) {
                        StyleConstants.setForeground(attributes, ThemeConstants.quizPassGreen);
                    } else {
                        StyleConstants.setForeground(attributes, ThemeConstants.quizFailRed);
                    }

                    if (correctness < 0.5) {
                        insertionText = "maybe next time.\n";
                    } else if ((correctness >= 0.5) && (correctness < 0.65)) {
                        insertionText = "not bad.\n";
                    } else if ((correctness >= 0.65) && (correctness < 0.75)) {
                        insertionText = "good work.\n";
                    } else if ((correctness >= 0.75) && (correctness < 0.85)) {
                        insertionText = "very nice.\n";
                    } else if ((correctness >= 0.85) && (correctness < 0.95)) {
                        insertionText = "excellent!\n";
                    } else if (correctness >= 0.95) {
                        insertionText = "perfect :-D\n";
                    }
                    answerArea.getStyledDocument().insertString(offset, insertionText, attributes);
                    offset += (insertionText).length();

                    //NOW PRINT OVERALL QUIZ SUMMARY SO FAR
                    StyleConstants.setFontSize(attributes, 10);
                    StyleConstants.setForeground(attributes, ThemeConstants.textColour);
                    StyleConstants.setBold(attributes, true);

                    insertionText = "\n";
                    answerArea.getStyledDocument().insertString(offset, insertionText, attributes);
                    offset += insertionText.length();

                    insertionText = "Progress: ";
                    answerArea.getStyledDocument().insertString(offset, insertionText, attributes);
                    offset += insertionText.length();

                    StyleConstants.setBold(attributes, false);

                    int totalCorrectness;
                    if (totalMarksAvailable == 0) {
                        totalCorrectness = 0;
                    } else {
                        totalCorrectness = (int) (100 * ((float) totalMarksAwarded / (float) totalMarksAvailable));
                    }

                    insertionText = "you have answered " + totalQuestionsAnswered + " question(s), with an overall score of " + totalMarksAwarded + "/" + totalMarksAvailable
                            + " = " + totalCorrectness + "%\n" + quizManager.getCurrentQuestionCount() + " question(s) remain.\n";
                    answerArea.getStyledDocument().insertString(offset, insertionText, attributes);
                    offset += insertionText.length();

                    StyleConstants.setBold(attributes, true);
                    insertionText = "Score: ";
                    answerArea.getStyledDocument().insertString(offset, insertionText, attributes);
                    offset += insertionText.length();


                    StyleConstants.setBold(attributes, false);

                    insertionText = totalScore + "  [+" + addToScore + "]";
                    answerArea.getStyledDocument().insertString(offset, insertionText, attributes);
                    offset += insertionText.length();

                    //if there is a combo, display it
                    if (isCombo) {
                        insertionText = "  [";
                        answerArea.getStyledDocument().insertString(offset, insertionText, attributes);
                        offset += insertionText.length();

                        StyleConstants.setForeground(attributes, ThemeConstants.comboColour);
                        StyleConstants.setBold(attributes, true);
                        StyleConstants.setFontSize(attributes, 13);

                        insertionText = "COMBO!";
                        answerArea.getStyledDocument().insertString(offset, insertionText, attributes);
                        offset += insertionText.length();

                        StyleConstants.setForeground(attributes, ThemeConstants.textColour);
                        StyleConstants.setBold(attributes, false);
                        StyleConstants.setFontSize(attributes, 10);

                        insertionText = " " + comboCounter + " >95% in a row: +" + ((int) (comboCounter / 2) + "]\n\n");
                        answerArea.getStyledDocument().insertString(offset, insertionText, attributes);
                        offset += insertionText.length();
                    }

                    insertionText = "\n\n";
                    answerArea.getStyledDocument().insertString(offset, insertionText, attributes);
                    offset += insertionText.length();

                } catch (BadLocationException ex) {
                    Logger.getLogger(QuizWindow.class.getName()).log(Level.SEVERE, "occurred while attempting to insert styled text into answer area StyledDocument at offset=" + offset, ex);
                }
            } else if (isShowingQuestion == false) { //********************************************************************************************************88eight
                //this may mean that the next question must be loaded, or that the quiz has ended and the end screen must
                //be displayed
                btnSkip.setEnabled(true);
                //show the next question
                if (quizManager.hasMoreQuestions()) {
                    isShowingQuestion = true;
                    displayNextQuestion();
                } //quiz is over, show the end screen (hide the answer area and put summary in the question area
                else {
                    showEndScreen();
                }


            }


            resetSize(answerArea);
            answerArea.setCaretPosition(0);
            resetSize(questionArea);
            questionArea.setCaretPosition(0);
        }
    }

    public void showEndScreen() {
        quizHasEnded = true;
        lblQuestionFrom.setVisible(false);
        answerArea.getScroller().setVisible(false);
        btnSkip.setVisible(false);
        questionArea.setText("");
        QuizWindow.this.validate();

        //display final screen info
        String insertionText = "";
        int offset = 0;
        try {
            SimpleAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setFontFamily(attributes, contentPane.getFont().getFamily());
            StyleConstants.setFontSize(attributes, 18);
            StyleConstants.setForeground(attributes, ThemeConstants.textColour);

            insertionText = "Quiz Completed - ";
            questionArea.getStyledDocument().insertString(offset, insertionText, attributes);
            offset += (insertionText).length();

            float overallCorrectness;
            if (totalMarksAvailable == 0) {
                overallCorrectness = 0;
            } else {
                overallCorrectness = (float) totalMarksAwarded / (float) totalMarksAvailable;
            }

            if (overallCorrectness < 0.5) {
                insertionText = "maybe do another round..\n";
            } else if ((overallCorrectness >= 0.5) && (overallCorrectness < 0.65)) {
                insertionText = "not a bad result :)\n";
            } else if ((overallCorrectness >= 0.65) && (overallCorrectness < 0.75)) {
                insertionText = "very good work.\n";
            } else if ((overallCorrectness >= 0.75) && (overallCorrectness < 0.85)) {
                insertionText = "lovely outcome!\n";
            } else if ((overallCorrectness >= 0.85) && (overallCorrectness < 0.95)) {
                insertionText = "rather excellent!\n";
            } else if (overallCorrectness >= 0.95) {
                insertionText = "flawless :-D\n";
            }

            if (overallCorrectness >= 0.5) {
                StyleConstants.setForeground(attributes, ThemeConstants.quizPassGreen);
            } else {
                StyleConstants.setForeground(attributes, ThemeConstants.quizFailRed);
            }
            questionArea.getStyledDocument().insertString(offset, insertionText, attributes);
            offset += (insertionText).length();

            StyleConstants.setFontSize(attributes, 12);
            StyleConstants.setForeground(attributes, ThemeConstants.textColour);
            StyleConstants.setItalic(attributes, true);
            insertionText = "Questions answered: ";
            questionArea.getStyledDocument().insertString(offset, insertionText, attributes);
            offset += insertionText.length();

            StyleConstants.setItalic(attributes, false);
            insertionText = String.valueOf(totalQuestionsAnswered);
            questionArea.getStyledDocument().insertString(offset, insertionText, attributes);
            offset += insertionText.length();

            StyleConstants.setItalic(attributes, true);
            insertionText = "\nQuestions skipped: ";
            questionArea.getStyledDocument().insertString(offset, insertionText, attributes);
            offset += insertionText.length();

            StyleConstants.setItalic(attributes, false);
            insertionText = String.valueOf(totalQuestionsSkipped);
            questionArea.getStyledDocument().insertString(offset, insertionText, attributes);
            offset += insertionText.length();


            StyleConstants.setItalic(attributes, true);
            insertionText = "\n\nGrade: ";
            questionArea.getStyledDocument().insertString(offset, insertionText, attributes);
            offset += insertionText.length();

            StyleConstants.setItalic(attributes, false);
            insertionText = totalMarksAwarded + "/" + totalMarksAvailable + " = " + ((int) (overallCorrectness * 100)) + "%";
            questionArea.getStyledDocument().insertString(offset, insertionText, attributes);
            offset += insertionText.length();

            StyleConstants.setItalic(attributes, true);
            StyleConstants.setBold(attributes, true);
            insertionText = "\n\nScore: ";
            questionArea.getStyledDocument().insertString(offset, insertionText, attributes);
            offset += insertionText.length();

            StyleConstants.setItalic(attributes, false);
            insertionText = String.valueOf(totalScore);
            questionArea.getStyledDocument().insertString(offset, insertionText, attributes);
            offset += insertionText.length();
            StyleConstants.setBold(attributes, false);

            insertionText = "\n\nOf the " + totalQuestionsAnswered + " question(s) answered, you improved on " + improvementCount + " of them, compared "
                    + "with previous values. You acheived " + comboIncidenceCounter + " combo(s).";
            questionArea.getStyledDocument().insertString(offset, insertionText, attributes);
            offset += insertionText.length();


            int totalCorrectness;
            if (totalMarksAvailable == 0) {
                totalCorrectness = 0;
            } else {
                totalCorrectness = (int) (100 * ((float) totalMarksAwarded / (float) totalMarksAvailable));
            }

            //***********************CREATE A QUIZ HISTORY ENTRY**********************************************
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

            IOManager.getQuizHistoryFile().addNewEntry(dateFormat.format(calendar.getTime()), totalCorrectness, totalQuestionsAnswered, totalQuestionsSkipped, totalScore, quizManager.getLibrariesUsed());
            //***********************UPDATE THE SAVED SCORE***************************************************
            IOManager.getQuizHistoryFile().addToTotalScore(totalScore);
            ParserWriter.writeQuizHistoryFile(IOManager.getQuizHistoryFile());



        } catch (BadLocationException ex) {
            Logger.getLogger(QuizWindow.class.getName()).log(Level.SEVERE, "occurred while attempting to insert styled text into answer area StyledDocument at offset=" + offset, ex);
        }
    }
}

