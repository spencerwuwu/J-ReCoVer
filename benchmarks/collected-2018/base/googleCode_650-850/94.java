// https://searchcode.com/api/result/5310215/

/*******************************************************************************
 * Copyright 2011 AKRA GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package de.akra.idocit.common.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import de.akra.idocit.common.structure.Documentation;
import de.akra.idocit.common.structure.Interface;
import de.akra.idocit.common.structure.InterfaceArtifact;
import de.akra.idocit.common.structure.Operation;
import de.akra.idocit.common.structure.Parameter;
import de.akra.idocit.common.structure.Parameters;
import de.akra.idocit.common.structure.RoleScope;
import de.akra.idocit.common.structure.RolesRecommendations;
import de.akra.idocit.common.structure.SignatureElement;
import de.akra.idocit.common.structure.ThematicGrid;
import de.akra.idocit.common.structure.ThematicRole;
import de.akra.idocit.common.structure.ThematicRoleContext;
import de.akra.idocit.common.utils.DescribedItemNameComparator;
import de.akra.idocit.common.utils.Preconditions;
import de.akra.idocit.common.utils.SignatureElementUtils;
import de.akra.idocit.common.utils.StringUtils;
import de.akra.idocit.common.utils.ThematicRoleUtils;

/**
 * <table name="idocit" border="1" cellspacing="0">
 * <tr>
 * <td>Role:</td>
 * <td>AGENT</td>
 * </tr>
 * <tr>
 * <td><b>Developer</b>:</td>
 * <td>Service to execute role-based and grid-based rules. Rules are used to reduce the
 * grid to the minimum set of roles required to be documented.</td>
 * </tr>
 * </table>
 * 
 * @author Jan Christian Krause
 * @author Florian Stumpf
 * 
 */
public final class RuleService
{
	private static final Logger LOG = Logger.getLogger(RuleService.class.getName());

	/**
	 * <table name="idocit" border="1" cellspacing="0">
	 * <tr>
	 * <td>Role:</td>
	 * <td>ACTION</td>
	 * </tr>
	 * <tr>
	 * <td><b>Developer</b>:</td>
	 * <td>Executes all grid-based roles on the given thematic grid. It removes all
	 * thematic roles which lead to a "false"-evaluation of a rule.</td>
	 * </tr>
	 * </table>
	 * 
	 * @param gridToReduce
	 * <br />
	 *            <table name="idocit" border="1" cellspacing="0">
	 *            <tr>
	 *            <td>Element:</td>
	 *            <td>gridToReduce:de.akra.idocit.common.structure.ThematicGrid</td>
	 *            </tr>
	 *            <tr>
	 *            <td>Role:</td>
	 *            <td>SOURCE</td>
	 *            </tr>
	 *            </table>
	 * @param selectedSignatureElement
	 * <br />
	 *            <table name="idocit" border="1" cellspacing="0">
	 *            <tr>
	 *            <td>Element:</td>
	 *            <td>
	 *            selectedSignatureElement:de.akra.idocit.common.structure.
	 *            SignatureElement</td>
	 *            </tr>
	 *            <tr>
	 *            <td>Role:</td>
	 *            <td>SOURCE</td>
	 *            </tr>
	 *            <tr>
	 *            <td><b>Developer</b>:</td>
	 *            <td>Represents the execution context of the grid-based roles.</td>
	 *            </tr>
	 *            </table>
	 * @return <table name="idocit" border="1" cellspacing="0">
	 *         <tr>
	 *         <td>Element:</td>
	 *         <td>
	 *         de.akra.idocit.common.structure.ThematicGrid:de.akra.idocit. common.
	 *         structure .ThematicGrid</td>
	 *         </tr>
	 *         <tr>
	 *         <td>Role:</td>
	 *         <td>OBJECT</td>
	 *         </tr>
	 *         <tr>
	 *         <td><b>Developer</b>:</td>
	 *         <td>The reduced grid</td>
	 *         </tr>
	 *         </table>
	 * @thematicgrid None
	 */
	public static ThematicGrid reduceGrid(final ThematicGrid gridToReduce,
			final SignatureElement selectedSignatureElement)
	{
		ThematicGrid reducedGrid = (ThematicGrid) gridToReduce.clone();
		Map<ThematicRole, Boolean> reducedRoles = new HashMap<ThematicRole, Boolean>();
		Map<String, String> reducedRules = new HashMap<String, String>();

		if (gridToReduce.getRoles() != null)
		{
			for (Entry<ThematicRole, Boolean> entry : gridToReduce.getRoles().entrySet())
			{
				Map<String, String> gridBasedRules = gridToReduce.getGridBasedRules();
				ThematicRole role = entry.getKey();
				String gridRule = gridBasedRules.get(role.getName());

				if (LOG.isLoggable(Level.INFO))
				{
					LOG.log(Level.INFO,
							"Evaluating rule for thematic role " + role.getName());
				}

				if (evaluateRule(gridRule, selectedSignatureElement))
				{
					reducedRoles.put(role, entry.getValue());
					reducedRules.put(role.getName(), gridRule);
				}
			}
		}

		reducedGrid.setGridBasedRules(reducedRules);
		reducedGrid.setRoles(reducedRoles);

		return reducedGrid;
	}

	/**
	 * <table name="idocit" border="1" cellspacing="0">
	 * <tr>
	 * <td>Role:</td>
	 * <td>ACTION</td>
	 * </tr>
	 * <tr>
	 * <td><b>Developer</b>:</td>
	 * <td>Splits the defined thematic roles into two groups:1st level: these roles are
	 * recommended to document 2nd level: these roles could be documented, but they do not
	 * need to</td>
	 * </tr>
	 * </table>
	 * <br />
	 * <table name="idocit" border="1" cellspacing="0">
	 * <tr>
	 * <td>Role:</td>
	 * <td>ALGORITHM</td>
	 * </tr>
	 * <tr>
	 * <td><b>Developer</b>:</td>
	 * <td>The derivation algorithm in pseudo code:
	 * 
	 * <pre>
	 * For each defined thematic role
	 * {
	 * 	Execute role-based rule
	 * 	if(role-rule == true)
	 * 	{
	 * 		add role to 1st level list
	 * 	}
	 * 	else
	 * 	{
	 * 		add role to 2nd level list
	 * 	}
	 * }
	 * Get the Operation which the given signature element belongs to.
	 * If a reference grid exists or the given collection contains only 1 grid
	 * {
	 * 	for each role in grid
	 * 	{
	 * 		if role exists in 1st level list
	 * 		{
	 * 			Execute grid-based rule
	 * 			if(grid-rule == false)
	 * 			{
	 * 				Remove rule from first level list
	 * 			}
	 * 		}
	 * 	}
	 * }
	 * </pre>
	 * 
	 * </td>
	 * </tr>
	 * </table>
	 * <br />
	 * <table name="idocit" border="1" cellspacing="0">
	 * <tr>
	 * <td>Role:</td>
	 * <td>SOURCE</td>
	 * </tr>
	 * <tr>
	 * <td><b>Developer</b>:</td>
	 * <td>The recommendation bases on all locally defined roles which are retrieved from
	 * the {@link PersistenceService}.</td>
	 * </tr>
	 * </table>
	 * 
	 * @param matchingGrids
	 * <br />
	 *            <table name="idocit" border="1" cellspacing="0">
	 *            <tr>
	 *            <td>Element:</td>
	 *            <td>matchingGrids:java.util.Collection</td>
	 *            </tr>
	 *            <tr>
	 *            <td>Role:</td>
	 *            <td>SOURCE</td>
	 *            </tr>
	 *            <tr>
	 *            <td><b>Developer</b>:</td>
	 *            <td>After executing the role-based rules the two recommendation lists
	 *            will have an intermediate state. Afterwards all roles of the first list
	 *            list will be checked against the roles of these grids (are they allowed
	 *            due to the grid-rules).</td>
	 *            </tr>
	 *            </table>
	 * @param selectedSignatureElement
	 * <br />
	 *            <table name="idocit" border="1" cellspacing="0">
	 *            <tr>
	 *            <td>Element:</td>
	 *            <td>
	 *            selectedSignatureElement:de.akra.idocit.common.structure.
	 *            SignatureElement</td>
	 *            </tr>
	 *            <tr>
	 *            <td>Role:</td>
	 *            <td>OWNER</td>
	 *            </tr>
	 *            <tr>
	 *            <td><b>Developer</b>:</td>
	 *            <td>If this signature element is an operation, the recommendation holds
	 *            for it. If it is a parameter, the recommendation holds for the operation
	 *            the parameter belongs to.</td>
	 *            </tr>
	 *            </table>
	 * @return <table name="idocit" border="1" cellspacing="0">
	 *         <tr>
	 *         <td>Element:</td>
	 *         <td>
	 *         de.akra.idocit.common.structure.RolesRecommendations:de.akra. idocit.common
	 *         .structure.RolesRecommendations</td>
	 *         </tr>
	 *         <tr>
	 *         <td>Role:</td>
	 *         <td>OBJECT</td>
	 *         </tr>
	 *         </table>
	 * @thematicgrid Creating Operations
	 */
	public static RolesRecommendations deriveRolesRecommendation(
			final Collection<ThematicGrid> matchingGrids,
			final List<ThematicRole> definedRoles,
			final SignatureElement selectedSignatureElement)
	{
		// At the beginning every role is recommended. In the following steps we identify
		// those roles, which do not need to be on first level recommendations and remove
		// them from this list.
		final Set<ThematicRole> firstLevel = new HashSet<ThematicRole>();
		final Set<ThematicRole> secondLevel = new HashSet<ThematicRole>();

		evaluateRoleBasedRules(definedRoles, selectedSignatureElement, firstLevel,
				secondLevel);

		if ((matchingGrids != null) && (!matchingGrids.isEmpty()))
		{
			evaluateGridBasedRules(matchingGrids, selectedSignatureElement, firstLevel,
					secondLevel);
		}

		Set<ThematicRole> associatedThematicRoles = new HashSet<ThematicRole>();
		SignatureElementUtils.collectAssociatedThematicRoles(associatedThematicRoles,
				selectedSignatureElement);

		firstLevel.removeAll(associatedThematicRoles);
		secondLevel.addAll(associatedThematicRoles);

		for (ThematicRole role : definedRoles)
		{
			if (!firstLevel.contains(role))
			{
				secondLevel.add(role);
			}
		}

		return new RolesRecommendations(sortByName(firstLevel), sortByName(secondLevel));
	}

	private static boolean isInterfaceLevel(SignatureElement sigElem)
	{
		return (sigElem instanceof Interface) || (sigElem instanceof InterfaceArtifact);
	}

	private static void evaluateRoleBasedRules(final List<ThematicRole> roles,
			final SignatureElement selectedSignatureElement,
			final Set<ThematicRole> firstLevel, final Set<ThematicRole> secondLevel)
	{
		if (roles != null)
		{
			for (final ThematicRole role : roles)
			{
				if (RoleScope.BOTH.equals(role.getRoleScope())
						|| ((RoleScope.INTERFACE_LEVEL.equals(role.getRoleScope()) && (isInterfaceLevel(selectedSignatureElement))))
						|| (RoleScope.OPERATION_LEVEL.equals(role.getRoleScope()) && (!isInterfaceLevel(selectedSignatureElement))))
				{
					firstLevel.add(role);
				}
				else
				{
					firstLevel.remove(role);
					secondLevel.add(role);
				}
			}
		}
	}

	private static void removeNonGridRoles(ThematicGrid grid, Set<ThematicRole> roles)
	{
		Set<ThematicRole> gridRoles = new HashSet<ThematicRole>(roles.size());
		gridRoles.addAll(roles);
		roles.clear();

		for (ThematicRole role : gridRoles)
		{
			if (grid.getRoles().containsKey(role))
			{
				roles.add(role);
			}
		}
	}

	/**
	 * Important: The Sets must not be null!
	 * 
	 * @param matchingGrids
	 * @param selectedSignatureElement
	 * @param firstLevel
	 * @param secondLevel
	 */
	private static void evaluateGridBasedRules(
			final Collection<ThematicGrid> matchingGrids,
			final SignatureElement selectedSignatureElement,
			final Set<ThematicRole> firstLevel, final Set<ThematicRole> secondLevel)
	{
		final SignatureElement sigElemOp = SignatureElementUtils
				.findOperationForParameter(selectedSignatureElement);
		if (!SignatureElement.EMPTY_SIGNATURE_ELEMENT.equals(sigElemOp))
		{
			final Operation op = (Operation) sigElemOp;
			final ThematicGrid theOne = getUnambiguousGrid(matchingGrids, op);

			if (theOne != null)
			{
				removeNonGridRoles(theOne, firstLevel);

				if (theOne.getGridBasedRules() != null)
				{
					// Check for each role defined in the reference grid if its rule is
					// fulfilled.
					for (final Entry<String, String> entry : theOne.getGridBasedRules()
							.entrySet())
					{
						ThematicRole role = ThematicRoleUtils.findRoleByName(
								entry.getKey(), theOne.getRoles().keySet());

						if (role != null)
						{
							if (!evaluateRule(entry.getValue(), selectedSignatureElement))
							{
								// Remove role if the grid-based-rule does not apply:
								firstLevel.remove(role);
								secondLevel.add(role);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Creates a list of {@link ThematicRoleContext}s all {@link ThematicRoles} documented
	 * or inherited for / by the given {@link Operation}.
	 * 
	 * @param operation
	 *            The {@link Operation} to create the contexts for. Must not be
	 *            <code>null</code>.
	 * 
	 * @return The list of created contexts
	 */
	private static List<ThematicRoleContext> createThematicRolesContextsForOperation(
			Operation operation)
	{
		Preconditions.checkNotNull(operation, "The operation must not be null.");

		final List<ThematicRoleContext> thematicRoleContexts = new ArrayList<ThematicRoleContext>();

		// Create the context for ...
		// ... the operation itsself.
		final String verb = ThematicGridService.extractVerb(operation.getIdentifier());
		final ThematicRoleContext operationContext = new ThematicRoleContext(new ThematicRole("NONE"),
				operation.getNumerus(), operation.hasPublicAccessibleAttributes(), false,
				verb);

		thematicRoleContexts.add(operationContext);
		createThematicRoleContexts(thematicRoleContexts, operation);

		// ... the inputs.
		if (operation.getInputParameters() != null)
		{
			createThematicRoleContexts(thematicRoleContexts, operation
					.getInputParameters().getParameters());
		}

		// ... the outputs.
		if (operation.getOutputParameters() != null)
		{
			createThematicRoleContexts(thematicRoleContexts, operation
					.getOutputParameters().getParameters());
		}

		// ... the exceptions.
		if (operation.getExceptions() != null)
		{
			// TODO clarify why exceptions are declared as Lists of Parameters and
			// refactor this declaration if necessary.
			for (Parameters parameters : operation.getExceptions())
			{
				createThematicRoleContexts(thematicRoleContexts, parameters);
			}
		}

		// ... and the parent interfaces.
		SignatureElement parent = operation.getParent();
		while (!SignatureElement.EMPTY_SIGNATURE_ELEMENT.equals(parent)
				&& (parent != null))
		{
			createThematicRoleContexts(thematicRoleContexts, parent);
			parent = parent.getParent();
		}

		return thematicRoleContexts;
	}

	/**
	 * Convenience-Method to use {@link this#createThematicRoleContexts(List,
	 * SignatureElement)} with lists.
	 * 
	 * @param thematicRoleContexts
	 *            All created ThematicRoleContexts will be added to this list. It must not
	 *            be <code>null</code>.
	 * @param parameters
	 *            The list of {@link Parameter}s to create contexts for
	 * 
	 * @see {@link this#createThematicRoleContexts(List, SignatureElement)}
	 */
	private static void createThematicRoleContexts(
			List<ThematicRoleContext> thematicRoleContexts, List<Parameter> parameters)
	{
		Preconditions.checkNotNull(thematicRoleContexts,
				"The list for the contexts to add must not be null");

		if (parameters != null)
		{
			for (Parameter parameter : parameters)
			{
				createThematicRoleContexts(thematicRoleContexts, parameter);
				createThematicRoleContexts(thematicRoleContexts,
						parameter.getComplexType());
			}
		}
	}

	/**
	 * Creats {@link ThematicRoleContext}s for the {@link Documentation}s of the given
	 * {@link SignatureElement} and adds them to the given list. A context is created for
	 * each documented {@link ThematicRole}.
	 * 
	 * @param thematicRoleContexts
	 *            All created ThematicRoleContexts will be added to this list. It must not
	 *            be <code>null</code>.
	 * @param signatureElement
	 *            The SignatureElement to create the contexts for.
	 */
	private static void createThematicRoleContexts(
			List<ThematicRoleContext> thematicRoleContexts,
			SignatureElement signatureElement)
	{
		Preconditions.checkNotNull(thematicRoleContexts,
				"The list for the contexts to add must not be null");

		if (signatureElement != null)
		{
			final List<Documentation> documentations = signatureElement
					.getDocumentations();

			if (documentations != null)
			{
				for (Documentation documentation : documentations)
				{
					if (documentation.getThematicRole() != null)
					{
						final ThematicRole role = documentation.getThematicRole();
						final ThematicRoleContext context = new ThematicRoleContext(role,
								signatureElement.getNumerus(),
								signatureElement.hasPublicAccessibleAttributes(), false,
								null);

						thematicRoleContexts.add(context);
					}
				}
			}
		}
	}

	/**
	 * Evaluates the given rule for the given {@link SignatureElement}. The rule can
	 * either be role- or grid-based.
	 * 
	 * @param rule
	 *            The rule to evaluate. (OBJECT). Must not be <code>null</code>.
	 * @param sigElem
	 *            The {@link SignatureElement} to apply the rule to. Must not be
	 *            <code>null</code>.
	 * @return The result of the rule-evaluation, thus either <code>true</code> or
	 *         <code>false</code>
	 * 
	 * @throws IllegalArgumentException
	 *             If one of the parameters is <code>null</code>
	 */
	public static boolean evaluateRule(final String rule, final SignatureElement sigElem)
	{
		Preconditions.checkNotNull(rule, "The rule must not be null.");
		Preconditions.checkNotNull(sigElem, "The SignatureElement must not be null.");

		final ScriptEngine engine = getScriptEngine();
		engine.put("EMPTY_SIGNATURE_ELEMENT", SignatureElement.EMPTY_SIGNATURE_ELEMENT);
		engine.put(
				"interfaceLevel",
				Boolean.valueOf((sigElem instanceof Interface)
						|| (sigElem instanceof InterfaceArtifact)));

		final SignatureElement operationElement = SignatureElementUtils
				.findOperationForParameter(sigElem);

		if (!SignatureElement.EMPTY_SIGNATURE_ELEMENT.equals(operationElement))
		// In this case we have a signature element on operation level or below.
		{

			final Operation operation = (Operation) SignatureElementUtils
					.findOperationForParameter(sigElem);

			engine.put("thematicRoleContexts",
					createThematicRolesContextsForOperation(operation));
		}

		Boolean result;
		try
		{
			result = (Boolean) engine.eval(rule);
		}
		catch (ScriptException e)
		{
			LOG.log(Level.SEVERE, "Error evaluating rule \"" + rule + "\".", e);
			throw new IllegalArgumentException("Error evaluating rule \"" + rule + "\".");
		}

		return result.booleanValue();
	}

	/**
	 * Filters the given collection of ThematicGrids to return either the reference-grid,
	 * or - in case the collection contains only one grid - the only available grid.
	 * 
	 * If the collection is empty or no reference grid is chosen, this method will return
	 * <code>null</code>
	 * 
	 * @param grids
	 *            A collection of ThematicGrids.
	 * @param operation
	 *            The current {@link Operation} to get the reference-grid's name from.
	 * @return Either the only available grid, the reference grid or <code>null</code>
	 */
	private static ThematicGrid getUnambiguousGrid(final Collection<ThematicGrid> grids,
			final Operation operation)
	{
		ThematicGrid result = null;

		if (grids != null && !grids.isEmpty())
		{
			if (grids.size() == 1)
			{
				// There's only one grid. No big choice, using this one.
				result = grids.iterator().next();
			}
			else if (operation.getThematicGridName() != null)
			{
				// Get the thematic grid matching the reference name from the
				// operation:
				for (final ThematicGrid grid : grids)
				{
					if (grid.getName().equals(operation.getThematicGridName()))
					{
						result = grid;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Sorts the given {@link Set} of {@link ThematicRole}s by name.
	 * 
	 * @param roles
	 *            Unsorted (well, it's a Set...) roles.
	 * @return A List of the given roles, sorted by name.
	 */
	private static List<ThematicRole> sortByName(final Set<ThematicRole> roles)
	{
		final List<ThematicRole> result = new ArrayList<ThematicRole>(roles);
		Collections.sort(result, DescribedItemNameComparator.getInstance());

		return result;
	}

	/**
	 * <table name="idocit" border="1" cellspacing="0">
	 * <tr>
	 * <td>Role:</td>
	 * <td>ACTION</td>
	 * </tr>
	 * <tr>
	 * <td><b>Developer</b>:</td>
	 * <td>Tests whether the given rule expression has a valid syntax or not.</td>
	 * </tr>
	 * </table>
	 * <br />
	 * <table name="idocit" border="1" cellspacing="0">
	 * <tr>
	 * <td>Role:</td>
	 * <td>COMPARISON</td>
	 * </tr>
	 * <tr>
	 * <td><b>Developer</b>:</td>
	 * <td>The rule expression is tested against the grammar of the iDocIt! Rule Language.
	 * </td>
	 * </tr>
	 * </table>
	 * 
	 * @param ruleExpression
	 * <br />
	 *            <table name="idocit" border="1" cellspacing="0">
	 *            <tr>
	 *            <td>Element:</td>
	 *            <td>ruleExpression:java.lang.String</td>
	 *            </tr>
	 *            <tr>
	 *            <td>Role:</td>
	 *            <td>OBJECT</td>
	 *            </tr>
	 *            </table>
	 * @return <table name="idocit" border="1" cellspacing="0">
	 *         <tr>
	 *         <td>Element:</td>
	 *         <td>boolean:boolean</td>
	 *         </tr>
	 *         <tr>
	 *         <td>Role:</td>
	 *         <td>REPORT</td>
	 *         </tr>
	 *         </table>
	 * @thematicgrid Checking Operations
	 */
	public static boolean isRuleValid(String ruleExpression)
	{
		final ScriptEngine engine = getScriptEngine();
		boolean valid = false;

		try
		{
			((Compilable) engine).compile(ruleExpression);
			valid = true;
		}
		catch (final ScriptException e)
		{
			LOG.log(Level.INFO, "Cannot compile rule \"" + ruleExpression + "\".", e);
		}

		return valid;
	}

	/**
	 * Prepares and returns a {@link ScriptEngine}. With this ScriptEngine the rules
	 * (either role- or grid-based) can be evaluated.
	 * 
	 * Currently the ScriptEngine used is meant for JavaScript. Maybe at some time it
	 * might be necessary to implement a specific ScriptEngine to support only elements
	 * used for the rule-syntax.
	 * 
	 * @return A ScriptEngine for evaluating the rules.
	 */
	private static ScriptEngine getScriptEngine()
	{
		// final String predicates =
		// StringUtils.toString(RuleService.class.getResourceAsStream("basicRules.js"));
		String predicates = StringUtils.toString(RuleService.class
				.getResourceAsStream("basicRules.js"));

		final ScriptEngine engine = new ScriptEngineManager()
				.getEngineByName("JavaScript");

		try
		{
			engine.eval(predicates);
		}
		catch (final ScriptException e)
		{
			LOG.log(Level.SEVERE, "Error loading basic predicates.", e);
		}

		return engine;
	}
}

