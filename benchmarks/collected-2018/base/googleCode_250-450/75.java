// https://searchcode.com/api/result/2187908/

package uk.ac.lkl.migen.system.ai.analysis.core.mask;

import java.util.*;

import uk.ac.lkl.common.util.event.*;
import uk.ac.lkl.common.util.expression.Expression;
import uk.ac.lkl.common.util.value.Number;
import uk.ac.lkl.migen.system.ai.analysis.*;
import uk.ac.lkl.migen.system.ai.analysis.trigger.*;
import uk.ac.lkl.migen.system.ai.um.DetectionStateIndicator;
import uk.ac.lkl.migen.system.ai.um.IndicatorClassOld;
import uk.ac.lkl.migen.system.expresser.model.Attribute;
import uk.ac.lkl.migen.system.expresser.model.AttributeHandle;
import uk.ac.lkl.migen.system.expresser.model.ExpresserModel;
import uk.ac.lkl.migen.system.expresser.model.Walker;
import uk.ac.lkl.migen.system.expresser.model.event.*;
import uk.ac.lkl.migen.system.expresser.model.shape.block.BlockShape;
import uk.ac.lkl.migen.system.expresser.model.shape.block.PatternShape;
import uk.ac.lkl.migen.system.expresser.model.tiednumber.TiedNumberExpression;
import uk.ac.lkl.migen.system.task.ConstructionExpressionTask;

/**
 * This class verifies whether the construction of the student 
 * can be messed up or not from the PoV of the structure (i.e. 
 * not the color allocation) and, if yes, return a shape to 
 * mess up the pattern with. 
 * 
 * The module returns a pattern to use to mess-up the model. 
 * It does so when it can find an instance of the model that,  
 * after some variation of its internal variables (i.e. unlocked
 * tied numbers) does not look like a solution.
 *  
 * The module returns Detector.NULL_SHAPE_DETECTED if the model 
 * does not have any variables. By definition, a model without 
 * variables cannot be messed-up.
 * 
 * Note that a shape that does not look like a solution in the 
 * first place does not need any change to be messed-up, so any
 * of its constituent patterns/shapes can be returned by this
 * module.
 * 
 * @author sergut
 *
 */
public class StructuralMessupDetector extends Detector {
    /**
     * An expected construction, to compare
     */
    private ExpresserModel solution = null;

    /**
     * A new MessupVerifier.
     * 
     * @param model the model on which the student is working.
     * @param task the task 
     * @param trigger the trigger that fires the evaluation (if any)
     */
    public StructuralMessupDetector(ExpresserModel model,ConstructionExpressionTask task, AnalysisUpdateTrigger trigger) {
	super(model, task, trigger);
	assignSolution(task.getExpectedConstructionList().get(0).getExpresserModel());
	if (trigger == null) 
	    attachListenersToModel();
	else
	    attachListenersToTrigger();
    }

    /**
     * A new StructuralMessupDetector. Used for testing.
     * 
     * @param solutionConstruction the solution
     * @param studentsConstruction the construction done by the student
     */
    public StructuralMessupDetector(ExpresserModel solutionConstruction, ExpresserModel studentsConstruction) {
	super(studentsConstruction, null, null);
	assignSolution(solutionConstruction); 
    }

    /**
     * Sets the internal representation of a solution for the current task.
     * 
     * The solution must have at least one variable (otherwise, it can be a 
     * valid solution for any generalisation problem).
     * 
     * @param solutionConstruction the solution
     * 
     * @throws IllegalArgumentException if the solution does not have any variables
     */
    private void assignSolution(ExpresserModel solutionConstruction) {
	int solutionVariableCount = solutionConstruction.getContainedTiedNumbers(true).size();
	if (solutionVariableCount < 1)
	    throw new IllegalArgumentException("Solution must have at least one variable.");
	
	this.solution = solutionConstruction;
    }

    protected void attachListenersToTrigger() {
	addUpdateListener(new AnalysisUpdateTriggerListener() {
	    public void updateAnalysis() {
		detect();
	    }});	
    }

    protected void attachListenersToModel() {
	getModel().addAttributeChangeListener(new AttributeChangeListener<BlockShape>() {
	    public void attributesChanged(AttributeChangeEvent<BlockShape> e) {
		detect();
	    }});
	getModel().addObjectListener(new ObjectListener() {
	    public void objectAdded(ObjectEvent e) {
		detect();
	    }
	    public void objectRemoved(ObjectEvent e) {
		detect();
	    }});
	getModel().addObjectUpdateListener(new UpdateListener<BlockShape>() {
	    public void objectUpdated(UpdateEvent<BlockShape> e) {
		detect();
	    }});
    }

    /**
     * Returns true if the student's construction can be messed up, false otherwise. 
     * 
     * @return true if the student's construction can be messed up, false otherwise.
     */
    public boolean constructionCanBeMessedUp() {
	if (this.solution == null || this.getModel() == null) {
	    return false;
	}

	// The work of this module involves changing the students' construction, so we need to make a copy
	ExpresserModel studentConstructionCopy = getModel().createCopyAndReplaceUnlockedTiedNumbersWithNewOnes();

	Set<TiedNumberExpression<Number>> studentVariableSet, solutionVariableSet;

	studentVariableSet = new HashSet<TiedNumberExpression<Number>>(getStructuralIntegerVariablesOf(studentConstructionCopy));
	if (studentVariableSet.size() == 0) {
	    return false;
	}

	solutionVariableSet = new HashSet<TiedNumberExpression<Number>>(getStructuralIntegerVariablesOf(solution));
	if (solutionVariableSet.size() != studentVariableSet.size()) {
	    return true;
	}

	// At the moment, we assume there is only one variable. TODO: this needs revisiting, but is low 
	// priority at the moment.
	if (solutionVariableSet.size() != 1)
	    throw new RuntimeException("Solution should have one and only one variable");

	//MM doesn't understand the four lines below
	Iterator<TiedNumberExpression<Number>> solutionItr = solutionVariableSet.iterator();
	Iterator<TiedNumberExpression<Number>> studentItr = studentVariableSet.iterator();
	//Does this return the first (and assumed only) variable of the model?
	TiedNumberExpression<Number> solutionVariable = solutionItr.next();
	TiedNumberExpression<Number> studentVariable = studentItr.next();

	// SG: "For a task with 'one' 'linear' variable, actually just two points would be enough"
	// MM therefore changed things from 10 values to only 2 hoping that it will reduce the memory problems
	int[] severalValues = {5,9};
	for (int i : severalValues) {   //  for (int i = 1; i < 10; i++) {
	    studentVariable.setValue(new Number(i));
	    ColorBlindMask studentMask = new ColorBlindMask(studentConstructionCopy);
	    if (!maskMatchesSolutionMask(studentMask, solutionVariable)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Returns all the integer variables from the given construction 
     * 
     * @param construction the construction
     * 
     * @return the integer variables from the given construction
     */
    @SuppressWarnings({"unused" })
    private List<TiedNumberExpression<Number>> getIntegerVariablesOf(ExpresserModel model) {
	List<TiedNumberExpression<Number>> result = new ArrayList<TiedNumberExpression<Number>>();
	List<TiedNumberExpression<Number>> variableList = model.getUnlockedNumbers();
	for (TiedNumberExpression<Number> variable : variableList) {
	    try {
		TiedNumberExpression<Number> integerVariable = (TiedNumberExpression<Number>) variable;
		result.add(integerVariable);
		BlockShape e;
	    } catch (ClassCastException e) {
		// Non integer variable
		// TODO: Variables should have some mechanism to know their type, so that we can just
		// call something like 'if (variable.isInteger()) {result.add(variable)}  
	    }
	}
	return result;
    }

    /**
     * Returns the integer variables from the given construction in the structural attributes of eXpresser (as of Dic'10, 
     * iterations, moveDown, and moveRight)
     * 
     * @param construction the construction
     * 
     * @return the integer variables from the given construction
     */
    private List<TiedNumberExpression<Number>> getStructuralIntegerVariablesOf(ExpresserModel model) {
	List<Expression<Number>> structuralExpressionList = new ArrayList<Expression<Number>>();
	List<TiedNumberExpression<Number>> result = new ArrayList<TiedNumberExpression<Number>>();
	for (BlockShape shape : model.getShapes()) {
	    if (!shape.isTruePattern()) {
		continue; 
	    }
	    PatternShape pattern = (PatternShape) shape;
	    
	    structuralExpressionList.add(getIterationsOf(pattern));
	    structuralExpressionList.add(getMoveRightOf(pattern));
	    structuralExpressionList.add(getMoveDownOf(pattern));
	}
	for (Expression<Number> expression : structuralExpressionList) {
	    List<TiedNumberExpression<Number>> variablesInExpression = expression.getContainedTiedNumbers(true);
	    for (TiedNumberExpression<Number> variable : variablesInExpression) {
		result.add(variable);
	    }
	}	
	return result;
    }

    // Convenience
    private Expression<Number> getIterationsOf(PatternShape pattern) {
	Attribute<Number> iterationsAtt = pattern.getAttribute(PatternShape.ITERATIONS);
	return iterationsAtt.getValueSource().getExpression();
    }
    
    // Convenience
    private Expression<Number> getMoveRightOf(PatternShape pattern) {
	return getDeltaExpressionOf(PatternShape.X, pattern);
    }
	
    // Convenience
    private Expression<Number> getMoveDownOf(PatternShape pattern) {
	return getDeltaExpressionOf(PatternShape.Y, pattern);
    }
	
    // Convenience
    private Expression<Number> getDeltaExpressionOf(AttributeHandle<Number> attributeHandle, PatternShape pattern) {
	return pattern.getExpression(PatternShape.getIncrementHandle(attributeHandle, false));
    }
	
    /**
     * Returns true if given mask matches the mask of the solution
     * for some value of given variable, false otherwise.
     * 
     * @param studentMask the mask 
     * @param solutionVariable the variable
     * 
     * @return true if given mask matches the mask of the solution
     * for some value of given variable, false otherwise.
     */
    private boolean maskMatchesSolutionMask(ColorBlindMask studentMask, TiedNumberExpression<Number> solutionVariable) {
	for (int i = 1; i < 10; i++) {
	    solutionVariable.setValue(new Number(i));
	    ColorBlindMask solutionMask = new ColorBlindMask(this.solution);
	    if (solutionMask.looksEqualTo(studentMask)) {
		return true;
	    }
	}
	return false;
    }


    @Override
    public BlockShape detect() {
	System.out.println("Structural Mess-up Detector will now detect...");
	if (constructionCanBeMessedUp()) {
	    //TODO: improve this with appropriate return
	    //      perhaps return list of unlocked numbers rather than shape - this needs discussion
	    BlockShape shapeDetected = getStudentShapeWithVariables();
	    setValue(shapeDetected);
	    return shapeDetected;
	} else {
	    setValue(Detector.NULL_SHAPE_DETECTED);
	    return Detector.NULL_SHAPE_DETECTED;
	}
    }

    // TODO: this code will be totally rewritten when KK implements BlockShape.getVariables()
    // or similar before 2010-04-29
    // FIXME: @Hack(who = "SG", why = "ISEvaluationApril10", issues = 0)
    private BlockShape getStudentShapeWithVariables() {
	final BlockShape[] result = {Detector.NULL_SHAPE_DETECTED}; // Ugly ugly ugly hack 
	Walker walker = new Walker() {
	    public boolean tiedNumberFound(TiedNumberExpression<?> tiedNumber, 
		    			BlockShape shape, 
		    			AttributeHandle<Number> handle,
		    			ExpresserModel expresserModel) {
		//checking only changeable (confusingly aka unlocked numbers and 
		//only the ones that are in iterations of shapes
		if (!tiedNumber.isLocked() && handle == PatternShape.ITERATIONS) {
		    result[0] = shape; // Ugly ugly ugly hack
		    return false;
		} else {
		    return true;	
		}
	    }};
	this.getModel().walkToTiedNumbers(walker);
	return result[0]; // Ugly ugly ugly hack
    }
    
    @Override @Deprecated
    public String getOutputName() {
	return "StructuralMessupDetection";
    }

    @Override
    public DetectionStateIndicator getOutputIndicator() {
	return IndicatorClassOld.PATTERN_STRUCTURE_GENERAL__NO_SHAPE_DETECTED_TO_MESSUP;
    }

}

