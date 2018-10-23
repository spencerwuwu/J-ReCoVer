// https://searchcode.com/api/result/122050414/

/**
 *     This file is part of the Squashtest platform.
 *     Copyright (C) 2010 - 2016 Henix, henix.fr
 *
 *     See the NOTICE file distributed with this work for additional
 *     information regarding copyright ownership.
 *
 *     This is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     this software is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this software.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.squashtest.tm.service.internal.testcase;

import static org.squashtest.tm.service.security.Authorizations.OR_HAS_ROLE_ADMIN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.squashtest.tm.core.foundation.collection.PagedCollectionHolder;
import org.squashtest.tm.core.foundation.collection.Paging;
import org.squashtest.tm.core.foundation.collection.PagingAndSorting;
import org.squashtest.tm.core.foundation.collection.PagingBackedPagedCollectionHolder;
import org.squashtest.tm.core.foundation.lang.Couple;
import org.squashtest.tm.core.foundation.lang.PathUtils;
import org.squashtest.tm.domain.campaign.IterationTestPlanItem;
import org.squashtest.tm.domain.customfield.BoundEntity;
import org.squashtest.tm.domain.customfield.CustomFieldValue;
import org.squashtest.tm.domain.customfield.RawValue;
import org.squashtest.tm.domain.infolist.InfoListItem;
import org.squashtest.tm.domain.milestone.Milestone;
import org.squashtest.tm.domain.milestone.MilestoneStatus;
import org.squashtest.tm.domain.project.GenericProject;
import org.squashtest.tm.domain.project.Project;
import org.squashtest.tm.domain.testautomation.AutomatedTest;
import org.squashtest.tm.domain.testautomation.TestAutomationProject;
import org.squashtest.tm.domain.testcase.ActionTestStep;
import org.squashtest.tm.domain.testcase.CallTestStep;
import org.squashtest.tm.domain.testcase.TestCase;
import org.squashtest.tm.domain.testcase.TestCaseFolder;
import org.squashtest.tm.domain.testcase.TestCaseImportance;
import org.squashtest.tm.domain.testcase.TestCaseLibrary;
import org.squashtest.tm.domain.testcase.TestCaseLibraryNode;
import org.squashtest.tm.domain.testcase.TestStep;
import org.squashtest.tm.domain.testcase.TestStepVisitor;
import org.squashtest.tm.exception.DuplicateNameException;
import org.squashtest.tm.exception.InconsistentInfoListItemException;
import org.squashtest.tm.exception.UnallowedTestAssociationException;
import org.squashtest.tm.exception.testautomation.MalformedScriptPathException;
import org.squashtest.tm.service.advancedsearch.IndexationService;
import org.squashtest.tm.service.annotation.Id;
import org.squashtest.tm.service.annotation.PreventConcurrent;
import org.squashtest.tm.service.campaign.IterationTestPlanFinder;
import org.squashtest.tm.service.infolist.InfoListItemFinderService;
import org.squashtest.tm.service.internal.customfield.PrivateCustomFieldValueService;
import org.squashtest.tm.service.internal.library.NodeManagementService;
import org.squashtest.tm.service.internal.repository.ActionTestStepDao;
import org.squashtest.tm.service.internal.repository.LibraryNodeDao;
import org.squashtest.tm.service.internal.repository.TestCaseDao;
import org.squashtest.tm.service.internal.repository.TestStepDao;
import org.squashtest.tm.service.internal.testautomation.UnsecuredAutomatedTestManagerService;
import org.squashtest.tm.service.milestone.ActiveMilestoneHolder;
import org.squashtest.tm.service.milestone.MilestoneMembershipManager;
import org.squashtest.tm.service.testautomation.model.TestAutomationProjectContent;
import org.squashtest.tm.service.testcase.CustomTestCaseModificationService;
import org.squashtest.tm.service.testcase.ParameterModificationService;
import org.squashtest.tm.service.testcase.TestCaseImportanceManagerService;
import org.squashtest.tm.service.testcase.TestCaseLibraryNavigationService;

import com.google.common.base.Optional;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Gregory Fouquet
 *
 */
@Service("CustomTestCaseModificationService")
@Transactional
public class CustomTestCaseModificationServiceImpl implements CustomTestCaseModificationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomTestCaseModificationServiceImpl.class);
	private static final String WRITE_TC_OR_ROLE_ADMIN = "hasPermission(#testCaseId, 'org.squashtest.tm.domain.testcase.TestCase' , 'WRITE')" + OR_HAS_ROLE_ADMIN;
	private static final String READ_TC_OR_ROLE_ADMIN = "hasPermission(#testCaseId, 'org.squashtest.tm.domain.testcase.TestCase' , 'READ')" + OR_HAS_ROLE_ADMIN;

	@Inject
	private TestCaseDao testCaseDao;

	@Inject
	@Qualifier("squashtest.tm.repository.TestCaseLibraryNodeDao")
	private LibraryNodeDao<TestCaseLibraryNode> testCaseLibraryNodeDao;

	@Inject
	private ActionTestStepDao actionStepDao;

	@Inject
	private TestCaseImportanceManagerService testCaseImportanceManagerService;

	@Inject
	private TestStepDao testStepDao;

	@Inject
	@Named("squashtest.tm.service.internal.TestCaseManagementService")
	private NodeManagementService<TestCase, TestCaseLibraryNode, TestCaseFolder> testCaseManagementService;

	@Inject
	private TestCaseNodeDeletionHandler deletionHandler;

	@Inject
	private UnsecuredAutomatedTestManagerService taService;

	@Inject
	protected PrivateCustomFieldValueService customFieldValuesService;

	@Inject
	private ParameterModificationService parameterModificationService;

	@Inject
	private InfoListItemFinderService infoListItemService;

	@Inject
	private MilestoneMembershipManager milestoneService;

	@Inject
	private TestCaseLibraryNavigationService libraryService;

	@Inject
	private ActiveMilestoneHolder activeMilestoneHolder;

	@Inject
	private IterationTestPlanFinder iterationTestPlanFinder;

	@Inject
	private IndexationService indexationService;


	/* *************** TestCase section ***************************** */

	@Override
	@PreAuthorize("hasPermission(#testCaseId, 'org.squashtest.tm.domain.testcase.TestCase' , 'WRITE')" + OR_HAS_ROLE_ADMIN)
	public void rename(long testCaseId, String newName) throws DuplicateNameException {
            TestCase testCase = testCaseDao.findById(testCaseId);
            testCaseManagementService.renameNode(testCaseId, newName);
            // [Issue 6337] sorry ma, they forced me to
            reindexItpisReferencingTestCase(testCase);
	}

	@Override
	@PreAuthorize("hasPermission(#testCaseId, 'org.squashtest.tm.domain.testcase.TestCase' , 'WRITE')" + OR_HAS_ROLE_ADMIN)
	public void changeReference(long testCaseId, String reference) {
            TestCase testCase = testCaseDao.findById(testCaseId);
            testCase.setReference(reference);
            // [Issue 6337] sorry ma, they forced me to
            reindexItpisReferencingTestCase(testCase);
	}
        
        @Override
	@PreAuthorize("hasPermission(#testCaseId, 'org.squashtest.tm.domain.testcase.TestCase' , 'WRITE')" + OR_HAS_ROLE_ADMIN)
	public void changeImportance(long testCaseId, TestCaseImportance importance){
            TestCase testCase = testCaseDao.findById(testCaseId);
            testCase.setImportance(importance);
            // [Issue 6337] sorry ma, they forced me to
            reindexItpisReferencingTestCase(testCase);          
        }

	private void reindexItpisReferencingTestCase(TestCase testCase) {
		List<IterationTestPlanItem> itpis = iterationTestPlanFinder.findByReferencedTestCase(testCase);
		List<Long> itpiIds = new ArrayList();
		for (IterationTestPlanItem itpi : itpis) {
			itpiIds.add(itpi.getId());
		}
		indexationService.batchReindexItpi(itpiIds);
	}

	@Override
	@PreAuthorize("hasPermission(#testCaseId, 'org.squashtest.tm.domain.testcase.TestCase' , 'READ')" + OR_HAS_ROLE_ADMIN)
	@Transactional(readOnly = true)
	public List<TestStep> findStepsByTestCaseId(long testCaseId) {

		return testCaseDao.findTestSteps(testCaseId);

	}

	/* *************** TestStep section ***************************** */

	@Override
	@PreAuthorize("hasPermission(#parentTestCaseId, 'org.squashtest.tm.domain.testcase.TestCase' , 'WRITE')" + OR_HAS_ROLE_ADMIN)
	@PreventConcurrent(entityType=TestCase.class)
	public ActionTestStep addActionTestStep(@Id long parentTestCaseId, ActionTestStep newTestStep) {
		TestCase parentTestCase = testCaseDao.findById(parentTestCaseId);

		testStepDao.persist(newTestStep);
		// will throw a nasty NullPointerException if the parent test case can't
		// be found
		parentTestCase.addStep(newTestStep);
		customFieldValuesService.createAllCustomFieldValues(newTestStep, newTestStep.getProject());
		parameterModificationService.createParamsForStep(newTestStep.getId());
		return newTestStep;
	}

	@Override
	@PreAuthorize("hasPermission(#parentTestCaseId, 'org.squashtest.tm.domain.testcase.TestCase' , 'WRITE')" + OR_HAS_ROLE_ADMIN)
	@PreventConcurrent(entityType=TestCase.class)
	public ActionTestStep addActionTestStep(@Id long parentTestCaseId, ActionTestStep newTestStep, int index) {
		TestCase parentTestCase = testCaseDao.findById(parentTestCaseId);

		testStepDao.persist(newTestStep);
		// will throw a nasty NullPointerException if the parent test case can't
		// be found
		parentTestCase.addStep(index,newTestStep);
		customFieldValuesService.createAllCustomFieldValues(newTestStep, newTestStep.getProject());
		parameterModificationService.createParamsForStep(newTestStep.getId());
		return newTestStep;
	}

	@Override
	@PreAuthorize("hasPermission(#parentTestCaseId, 'org.squashtest.tm.domain.testcase.TestCase' , 'WRITE')" + OR_HAS_ROLE_ADMIN)
	@PreventConcurrent(entityType=TestCase.class)
	public ActionTestStep addActionTestStep(@Id long parentTestCaseId, ActionTestStep newTestStep,
			Map<Long, RawValue> customFieldValues) {

		ActionTestStep step = addActionTestStep(parentTestCaseId, newTestStep);
		initCustomFieldValues(step, customFieldValues);
		parameterModificationService.createParamsForStep(step.getId());
		return step;
	}

	@Override
	@PreAuthorize("hasPermission(#parentTestCaseId, 'org.squashtest.tm.domain.testcase.TestCase' , 'WRITE')" + OR_HAS_ROLE_ADMIN)
	@PreventConcurrent(entityType=TestCase.class)
	public ActionTestStep addActionTestStep(@Id long parentTestCaseId, ActionTestStep newTestStep,
			Map<Long, RawValue> customFieldValues, int index) {

		ActionTestStep step = addActionTestStep(parentTestCaseId, newTestStep,index);
		initCustomFieldValues(step, customFieldValues);
		parameterModificationService.createParamsForStep(step.getId());
		return step;
	}

	@Override
	@PreAuthorize("hasPermission(#testStepId, 'org.squashtest.tm.domain.testcase.TestStep' , 'WRITE')" + OR_HAS_ROLE_ADMIN)
	public void updateTestStepAction(long testStepId, String newAction) {
		ActionTestStep testStep = actionStepDao.findById(testStepId);
		testStep.setAction(newAction);
		parameterModificationService.createParamsForStep(testStepId);
	}

	@Override
	@PreAuthorize("hasPermission(#testStepId, 'org.squashtest.tm.domain.testcase.TestStep' , 'WRITE')" + OR_HAS_ROLE_ADMIN)
	public void updateTestStepExpectedResult(long testStepId, String newExpectedResult) {
		ActionTestStep testStep = actionStepDao.findById(testStepId);
		testStep.setExpectedResult(newExpectedResult);
		parameterModificationService.createParamsForStep(testStepId);
	}

	@Override
	@Deprecated
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	public void changeTestStepPosition(long testCaseId, long testStepId, int newStepPosition) {
		TestCase testCase = testCaseDao.findById(testCaseId);
		int index = findTestStepInTestCase(testCase, testStepId);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("**************** change step order : old index = " + index + ",new index : "
					+ newStepPosition);
		}

		testCase.moveStep(index, newStepPosition);
	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	@PreventConcurrent(entityType=TestCase.class)
	public void changeTestStepsPosition(@Id long testCaseId, int newPosition, List<Long> stepIds) {

		TestCase testCase = testCaseDao.findById(testCaseId);
		List<TestStep> steps = testStepDao.findListById(stepIds);

		testCase.moveSteps(newPosition, steps);

	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	@PreventConcurrent(entityType=TestCase.class)
	public void removeStepFromTestCase(@Id long testCaseId, long testStepId) {
		TestCase testCase = testCaseDao.findById(testCaseId);
		TestStep testStep = testStepDao.findById(testStepId);
		deletionHandler.deleteStep(testCase, testStep);
	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	@PreventConcurrent(entityType=TestCase.class)
	public void removeStepFromTestCaseByIndex(@Id long testCaseId, int index) {
		TestCase testCase = testCaseDao.findById(testCaseId);
		TestStep testStep = testCase.getSteps().get(index);
		deletionHandler.deleteStep(testCase, testStep);
	}

	/*
	 * given a TestCase, will search for a TestStep in the steps list (identified with its testStepId)
	 *
	 * returns : the index if found, -1 if not found or if the provided TestCase is null
	 */
	private int findTestStepInTestCase(TestCase testCase, long testStepId) {
		return testCase.getPositionOfStep(testStepId);
	}

	@Override
	@PostAuthorize("hasPermission(returnObject, 'READ')" + OR_HAS_ROLE_ADMIN)
	@Transactional(readOnly = true)
	public TestCase findTestCaseWithSteps(long testCaseId) {
		return testCaseDao.findAndInit(testCaseId);
	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	@PreventConcurrent(entityType=TestCase.class)
	public List<TestStep> removeListOfSteps(@Id long testCaseId, List<Long> testStepIds) {
		TestCase testCase = testCaseDao.findById(testCaseId);

		for (Long id : testStepIds) {
			TestStep step = testStepDao.findById(id);
			deletionHandler.deleteStep(testCase, step);
		}
		return testCase.getSteps();
	}

	@Override
	@PreAuthorize("hasPermission(#testCaseId, 'org.squashtest.tm.domain.testcase.TestCase' , 'READ')" + OR_HAS_ROLE_ADMIN)
	@Transactional(readOnly = true)
	public PagedCollectionHolder<List<TestStep>> findStepsByTestCaseIdFiltered(long testCaseId, Paging paging) {
		List<TestStep> list = testCaseDao.findAllStepsByIdFiltered(testCaseId, paging);
		long count = findStepsByTestCaseId(testCaseId).size();
		return new PagingBackedPagedCollectionHolder<>(paging, count, list);
	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	@PreventConcurrent(entityType=TestCase.class)
	public boolean pasteCopiedTestStep(@Id long testCaseId, long idInsertion, long copiedTestStepId) {
		Integer position = testStepDao.findPositionOfStep(idInsertion) + 1;
		return pasteTestStepAtPosition(testCaseId, Arrays.asList(copiedTestStepId), position);
	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	@PreventConcurrent(entityType=TestCase.class)
	public boolean pasteCopiedTestSteps(@Id long testCaseId, long idInsertion, List<Long> copiedTestStepIds) {
		Integer position = testStepDao.findPositionOfStep(idInsertion) + 1;
		return pasteTestStepAtPosition(testCaseId, copiedTestStepIds, position);
	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	@PreventConcurrent(entityType=TestCase.class)
	public boolean pasteCopiedTestStepToLastIndex(@Id long testCaseId, long copiedTestStepId) {
		return pasteTestStepAtPosition(testCaseId, Arrays.asList(copiedTestStepId), null);
	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	@PreventConcurrent(entityType=TestCase.class)
	public boolean pasteCopiedTestStepToLastIndex(@Id long testCaseId, List<Long> copiedTestStepIds) {
		return pasteTestStepAtPosition(testCaseId, copiedTestStepIds, null);
	}


	// FIXME : check for potential cycle with call steps. For now it's being checked
	// on the controller but it is obviously less safe.
	// FIXME : Refactor the method for null and not null position... it shouldn't be in the same method.
	/**
	 *
	 * @param testCaseId
	 * @param copiedStepIds
	 * @param position
	 * @return true if copied step is instance of CallTestStep
	 */
	private boolean pasteTestStepAtPosition(long testCaseId, List<Long> copiedStepIds, Integer position) {

		boolean hasCallstep = false;

		List<TestStep> originals = testStepDao.findByIdOrderedByIndex(copiedStepIds);

		// Issue 6146
		// If position is null we add at the end of list, so the index is correct
		// If position is not null we add several time at the same index. The list push
		// the content to the right, so we need to invert the order...
		if (position!=null) {
			Collections.reverse(originals);
		}

		for (TestStep original : originals){

			// first, create the step
			TestStep copyStep = original.createCopy();
			testStepDao.persist(copyStep);

			// attach it to a test case
			TestCase testCase = testCaseDao.findById(testCaseId);

			if (position != null && position < testCase.getSteps().size()) {
				testCase.addStep(position, copyStep);
			} else {
				testCase.addStep(copyStep);
			}

			// now special treatment if the steps are from another source
			if (!testCase.getSteps().contains(original)) {
				updateImportanceIfCallStep(testCase, copyStep);
				parameterModificationService.createParamsForStep(copyStep);
			}

			copyStep.accept(new TestStepCustomFieldCopier(original));

			// last, update that weird variable
			hasCallstep = hasCallstep || copyStep instanceof CallTestStep;
		}

		return hasCallstep;
	}

	private void updateImportanceIfCallStep(TestCase parentTestCase, TestStep copyStep) {
		if (copyStep instanceof CallTestStep) {
			TestCase called = ((CallTestStep) copyStep).getCalledTestCase();
			testCaseImportanceManagerService.changeImportanceIfCallStepAddedToTestCases(called, parentTestCase);
		}
	}


	@Override
	@Transactional(readOnly = true)
	public PagedCollectionHolder<List<TestCase>> findCallingTestCases(long testCaseId, PagingAndSorting sorting) {

		List<TestCase> callers = testCaseDao.findAllCallingTestCases(testCaseId, sorting);
		Long countCallers = testCaseDao.countCallingTestSteps(testCaseId);
		return new PagingBackedPagedCollectionHolder<>(sorting, countCallers, callers);

	}

	@Override
	public PagedCollectionHolder<List<CallTestStep>> findCallingTestSteps(long testCaseId, PagingAndSorting sorting) {
		List<CallTestStep> callers = testCaseDao.findAllCallingTestSteps(testCaseId, sorting);
		Long countCallers = testCaseDao.countCallingTestSteps(testCaseId);
		return new PagingBackedPagedCollectionHolder<>(sorting, countCallers, callers);
	}

	@Override
	public List<CallTestStep> findAllCallingTestSteps(long testCaseId) {
		return testCaseDao.findAllCallingTestSteps(testCaseId);
	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	public void changeImportanceAuto(long testCaseId, boolean auto) {
		TestCase testCase = testCaseDao.findById(testCaseId);
		testCase.setImportanceAuto(auto);
		testCaseImportanceManagerService.changeImportanceIfIsAuto(testCaseId);
	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	@Transactional(readOnly = true)
	public Collection<TestAutomationProjectContent> findAssignableAutomationTests(long testCaseId) {

		TestCase testCase = testCaseDao.findById(testCaseId);

		return taService.listTestsInProjects(testCase.getProject().getTestAutomationProjects());
	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	public AutomatedTest bindAutomatedTest(Long testCaseId, Long taProjectId, String testName) {

		TestAutomationProject project = taService.findProjectById(taProjectId);

		AutomatedTest newTest = new AutomatedTest(testName, project);

		AutomatedTest persisted = taService.persistOrAttach(newTest);

		TestCase testCase = testCaseDao.findById(testCaseId);

		AutomatedTest previousTest = testCase.getAutomatedTest();

		testCase.setAutomatedTest(persisted);

		taService.removeIfUnused(previousTest);

		return newTest;
	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	public AutomatedTest bindAutomatedTest(Long testCaseId, String testPath) {

		if (StringUtils.isBlank(testPath)) {
			removeAutomation(testCaseId);
			return null;
		} else {

			Couple<Long, String> projectAndTestname = extractAutomatedProjectAndTestName(testCaseId, testPath);

			// once it's okay we commit the test association
			return bindAutomatedTest(testCaseId, projectAndTestname.getA1(), projectAndTestname.getA2());
		}

	}

	@Override
	public void removeAutomation(long testCaseId) {
		TestCase testCase = testCaseDao.findById(testCaseId);
		AutomatedTest previousTest = testCase.getAutomatedTest();
		testCase.removeAutomatedScript();
		taService.removeIfUnused(previousTest);
	}

	/**
	 * initialCustomFieldValues maps the id of a CustomField to the value of the corresponding CustomFieldValues for
	 * that BoundEntity. read it again until it makes sense. it assumes that the CustomFieldValues instances already
	 * exists.
	 *
	 * @param entity
	 * @param initialCustomFieldValues
	 */
	protected void initCustomFieldValues(BoundEntity entity, Map<Long, RawValue> initialCustomFieldValues) {

		List<CustomFieldValue> persistentValues = customFieldValuesService.findAllCustomFieldValues(entity);

		for (CustomFieldValue value : persistentValues) {
			Long customFieldId = value.getCustomField().getId();

			if (initialCustomFieldValues.containsKey(customFieldId)) {
				RawValue newValue = initialCustomFieldValues.get(customFieldId);
				newValue.setValueFor(value);
			}

		}
	}

	/**
	 * @see org.squashtest.tm.service.testcase.CustomTestCaseFinder#findAllByAncestorIds(java.util.List)
	 */
	@Override
	@PostFilter("hasPermission(filterObject , 'READ')" + OR_HAS_ROLE_ADMIN)
	public List<TestCase> findAllByAncestorIds(Collection<Long> folderIds) {
		List<TestCaseLibraryNode> nodes = testCaseLibraryNodeDao.findAllByIds(folderIds);
		return new TestCaseNodeWalker().walk(nodes);
	}

	/**
	 * @see org.squashtest.tm.service.testcase.CustomTestCaseFinder#findAllCallingTestCases(long)
	 */
	@Override
	@PostFilter("hasPermission(filterObject , 'READ')" + OR_HAS_ROLE_ADMIN)
	public List<TestCase> findAllCallingTestCases(long calleeId) {
		return testCaseDao.findAllCallingTestCases(calleeId);
	}

	@Override
	public TestCase findTestCaseFromStep(long testStepId) {
		return testCaseDao.findTestCaseByTestStepId(testStepId);
	}

	/**
	 * @see org.squashtest.tm.service.testcase.CustomTestCaseFinder#findImpTCWithImpAuto(Collection)
	 */
	@Override
	public Map<Long, TestCaseImportance> findImpTCWithImpAuto(Collection<Long> testCaseIds) {
		return testCaseDao.findAllTestCaseImportanceWithImportanceAuto(testCaseIds);
	}

	/**
	 * @see org.squashtest.tm.service.testcase.CustomTestCaseFinder#findCallingTCids(long, Collection)
	 */
	@Override
	public Set<Long> findCallingTCids(long updatedId, Collection<Long> callingCandidates) {
		List<Long> callingCandidatesClone = new ArrayList<>(callingCandidates);
		List<Long> callingLayer = testCaseDao
				.findAllTestCasesIdsCallingTestCases(Arrays.asList(updatedId));
		Set<Long> callingTCToUpdate = new HashSet<>();
		while (!callingLayer.isEmpty() && !callingCandidatesClone.isEmpty()) {
			// filter found calling test cases
			callingLayer.retainAll(callingCandidatesClone);
			// save
			callingTCToUpdate.addAll(callingLayer);
			// reduce test case of interest
			callingCandidatesClone.removeAll(callingLayer);
			// go next layer
			callingLayer = testCaseDao.findAllTestCasesIdsCallingTestCases(callingLayer);
		}
		return callingTCToUpdate;
	}




	@Override
	// TODO : secure this
	public TestCase addNewTestCaseVersion(long originalTcId, TestCase newVersionData) {

		List<Long> milestoneIds =  new ArrayList<>();

		Optional<Milestone> activeMilestone = activeMilestoneHolder.getActiveMilestone();
		if (activeMilestone.isPresent()) {
			milestoneIds.add(activeMilestone.get().getId());
		}

                // copy the core attributes
		TestCase orig = testCaseDao.findById(originalTcId);
		TestCase newTC = orig.createCopy();

		newTC.setName(newVersionData.getName());
		newTC.setReference(newVersionData.getReference());
		newTC.setDescription(newVersionData.getDescription());
		newTC.clearMilestones();

		// now we must inster that at the correct location
		TestCaseLibrary library = libraryService.findLibraryOfRootNodeIfExist(orig);
		if (library != null){
			libraryService.addTestCaseToLibrary(library.getId(), newTC, null);
		}
		else{
			TestCaseFolder folder = libraryService.findParentIfExists(orig);
			libraryService.addTestCaseToFolder(folder.getId(), newTC, null);
		}

                // copy custom fields
		customFieldValuesService.copyCustomFieldValuesContent(orig, newTC);
                Queue<ActionTestStep> origSteps = new LinkedList<>(orig.getActionSteps());
                Queue<ActionTestStep> newSteps = new LinkedList<>(newTC.getActionSteps());
                while(! origSteps.isEmpty()){
                    ActionTestStep oStep = origSteps.remove();
                    ActionTestStep nStep = newSteps.remove();
                    customFieldValuesService.copyCustomFieldValuesContent(oStep, nStep);
                }
                
                // manage the milestones
		milestoneService.bindTestCaseToMilestones(newTC.getId(), milestoneIds);
		milestoneService.unbindTestCaseFromMilestones(originalTcId, milestoneIds);

		return newTC;
	}


	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	public void changeNature(long testCaseId, String natureCode) {
		TestCase testCase = testCaseDao.findById(testCaseId);
		InfoListItem nature = infoListItemService.findByCode(natureCode);

		if (infoListItemService.isNatureConsistent(testCase.getProject().getId(), natureCode)) {
			testCase.setNature(nature);
		} else {
			throw new InconsistentInfoListItemException("nature", natureCode);
		}

	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	public void changeType(long testCaseId, String typeCode) {
		TestCase testCase = testCaseDao.findById(testCaseId);
		InfoListItem type = infoListItemService.findByCode(typeCode);

		if (infoListItemService.isTypeConsistent(testCase.getProject().getId(), typeCode)) {
			testCase.setType(type);
		} else {
			throw new InconsistentInfoListItemException("type", typeCode);
		}
	}

	/* ********************************************************************************
	 *
	 * Milestones section
	 *
	 * *******************************************************************************
	 */

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	public void bindMilestones(long testCaseId, Collection<Long> milestoneIds) {
		milestoneService.bindTestCaseToMilestones(testCaseId, milestoneIds);
	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	public void unbindMilestones(long testCaseId, Collection<Long> milestoneIds) {
		milestoneService.unbindTestCaseFromMilestones(testCaseId, milestoneIds);
	}

	@Override
	@PreAuthorize(READ_TC_OR_ROLE_ADMIN)
	public Collection<Milestone> findAllMilestones(long testCaseId) {
		return milestoneService.findAllMilestonesForTestCase(testCaseId);
	}

	@Override
	@PreAuthorize(WRITE_TC_OR_ROLE_ADMIN)
	public Collection<Milestone> findAssociableMilestones(long testCaseId) {
		return milestoneService.findAssociableMilestonesToTestCase(testCaseId);
	}

	@Override
	public Collection<Milestone> findAssociableMilestonesForMassModif(List<Long> testCaseIds) {

		Collection<Milestone> milestones = null;

		for (Long testCaseId : testCaseIds){
			List<Milestone> mil = testCaseDao.findById(testCaseId).getProject().getMilestones();
			if (milestones != null){
				//keep only milestone that in ALL selected tc
				milestones.retainAll(mil);
			} else {
				//populate the collection for the first time
				milestones = new ArrayList<>(mil);
			}
		}
		filterLockedAndPlannedStatus(milestones);
		return milestones;
	}


	private void filterLockedAndPlannedStatus(Collection<Milestone> milestones){
		CollectionUtils.filter(milestones, new Predicate() {
			@Override
			public boolean evaluate(Object milestone) {

				return ((Milestone) milestone).getStatus() != MilestoneStatus.LOCKED
						&& ((Milestone) milestone).getStatus() != MilestoneStatus.PLANNED;
			}
		});
	}


	@Override
	public Collection<Long> findBindedMilestonesIdForMassModif(List<Long> testCaseIds) {

		Collection<Milestone> milestones = null;

		for (Long testCaseId : testCaseIds){
			Set<Milestone> mil = testCaseDao.findById(testCaseId).getMilestones();
			if (milestones != null){
				//keep only milestone that in ALL selected tc
				milestones.retainAll(mil);
			} else {
				//populate the collection for the first time
				milestones = new ArrayList<>(mil);
			}
		}
		filterLockedAndPlannedStatus(milestones);

		return 	CollectionUtils.collect(milestones, new Transformer() {

			@Override
			public Object transform(Object milestone) {

				return ((Milestone) milestone).getId();
			}
		});
	}



	@Override
	public boolean haveSamePerimeter(List<Long> testCaseIds) {
		if (testCaseIds.size() != 1) {

			Long first = testCaseIds.remove(0);
			List<Milestone> toCompare = testCaseDao.findById(first).getProject().getMilestones();

			for (Long testCaseId : testCaseIds) {
				List<Milestone> mil = testCaseDao.findById(testCaseId).getProject().getMilestones();

				if (mil.size() != toCompare.size() || !mil.containsAll(toCompare)) {
					return false;
				}
			}
		}

		return true;
	}


	/* *******************************************************
		private stuffs and shit etc
	**********************************************************/

	// returns a tuple-2 with first element : project ID, second element : test name
	private Couple<Long, String> extractAutomatedProjectAndTestName(Long testCaseId, String testPath) {

		// first we reject the operation if the script name is malformed
		if (!PathUtils.isPathWellFormed(testPath)) {
			throw new MalformedScriptPathException();
		}

		// now it's clear to go, let's find which TA project it is. The first slash must be removed because it doesn't
		// count.
		String path = testPath.replaceFirst("^/", "");
		int idxSlash = path.indexOf('/');

		String projectLabel = path.substring(0, idxSlash);
		String testName = path.substring(idxSlash + 1);

		TestCase tc = testCaseDao.findById(testCaseId);
		GenericProject tmproject = tc.getProject();

		TestAutomationProject tap = (TestAutomationProject) CollectionUtils.find(tmproject.getTestAutomationProjects(),
				new HasSuchLabel(projectLabel));

		// if the project couldn't be found we must also reject the operation
		if (tap == null) {
			throw new UnallowedTestAssociationException();
		}

		return new Couple<>(tap.getId(), testName);
	}


	private static final class HasSuchLabel implements Predicate {
		private String label;

		HasSuchLabel(String label) {
			this.label = label;
		}

		@Override
		public boolean evaluate(Object object) {
			TestAutomationProject tap = (TestAutomationProject) object;
			return tap.getLabel().equals(label);
		}
	}



	private final class TestStepCustomFieldCopier implements TestStepVisitor {
		TestStep original;

		private TestStepCustomFieldCopier(TestStep original) {
			this.original = original;
		}

		@Override
		public void visit(ActionTestStep visited) {
			customFieldValuesService.copyCustomFieldValues((ActionTestStep) original, visited);
			Project origProject = original.getTestCase().getProject();
			Project newProject = visited.getTestCase().getProject();

			if (!origProject.equals(newProject)) {
				customFieldValuesService.migrateCustomFieldValues(visited);
			}
		}

		@Override
		public void visit(CallTestStep visited) {
			// NOPE
		}

	}




}

