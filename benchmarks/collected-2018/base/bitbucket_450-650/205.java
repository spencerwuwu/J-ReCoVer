// https://searchcode.com/api/result/122051374/

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
package org.squashtest.tm.domain.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Store;
import org.hibernate.validator.constraints.NotBlank;
import org.squashtest.csp.core.bugtracker.domain.BugTracker;
import org.squashtest.tm.domain.Identified;
import org.squashtest.tm.domain.attachment.AttachmentHolder;
import org.squashtest.tm.domain.attachment.AttachmentList;
import org.squashtest.tm.domain.audit.Auditable;
import org.squashtest.tm.domain.bugtracker.BugTrackerBinding;
import org.squashtest.tm.domain.campaign.CampaignLibrary;
import org.squashtest.tm.domain.customreport.CustomReportLibrary;
import org.squashtest.tm.domain.infolist.InfoList;
import org.squashtest.tm.domain.milestone.Milestone;
import org.squashtest.tm.domain.requirement.RequirementLibrary;
import org.squashtest.tm.domain.testautomation.TestAutomationProject;
import org.squashtest.tm.domain.testautomation.TestAutomationServer;
import org.squashtest.tm.domain.testcase.TestCaseLibrary;
import org.squashtest.tm.exception.NoBugTrackerBindingException;

/**
 * GenericProject is the superclass of Project and ProjectTemplate. Even though there is no other structural difference
 * between an project and a template, choosing a specialization through inheritance (instead of a specialization through
 * composition) lets the app rely on polymorphism and reduce the impact upon project templates introduction.
 *
 * @author Gregory Fouquet
 *
 */
@Auditable
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "PROJECT_TYPE", discriminatorType = DiscriminatorType.STRING)
@Entity
@Table(name = "PROJECT")
public abstract class GenericProject implements Identified, AttachmentHolder {

	@Id
	@Column(name = "PROJECT_ID")
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "project_project_id_seq")
	@SequenceGenerator(name = "project_project_id_seq", sequenceName = "project_project_id_seq")
	private Long id;

	@Lob
	@Type(type = "org.hibernate.type.StringClobType")
	private String description;

	@Size(min = 0, max = 255)
	private String label;

	@NotBlank
	@Size(min = 0, max = 255)
	@Field(analyze = Analyze.NO, store = Store.YES)
	private String name;

	private boolean active = true;

	@OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
	@JoinColumn(name = "TCL_ID")
	private TestCaseLibrary testCaseLibrary;

	@OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
	@JoinColumn(name = "RL_ID")
	private RequirementLibrary requirementLibrary;

	@OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
	@JoinColumn(name = "CL_ID")
	private CampaignLibrary campaignLibrary;

	@OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
	@JoinColumn(name = "CRL_ID")
	private CustomReportLibrary customReportLibrary;

	@OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "project")
	private BugTrackerBinding bugtrackerBinding;

	@OneToMany(cascade = { CascadeType.ALL }, mappedBy = "tmProject")
	private Set<TestAutomationProject> testAutomationProjects = new HashSet<>();

	@JoinColumn(name = "TA_SERVER_ID")
	@ManyToOne(fetch = FetchType.LAZY)
	private TestAutomationServer testAutomationServer;

	@OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ATTACHMENT_LIST_ID", updatable = false)
	private final AttachmentList attachmentList = new AttachmentList();


	// the so-called information lists
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="REQ_CATEGORIES_LIST")
	private InfoList requirementCategories;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="TC_NATURES_LIST")
	private InfoList testCaseNatures;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="TC_TYPES_LIST")
	private InfoList testCaseTypes;


	@ManyToMany(mappedBy = "projects")
	private Set<Milestone> milestones = new HashSet<>();

	private boolean allowTcModifDuringExec = false;

	public List<Milestone> getMilestones() {
		return new ArrayList<>(milestones);
	}



	public GenericProject() {
		super();
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public Long getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@NotBlank
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name.trim();
	}

	public void setActive(boolean isActive) {
		this.active = isActive;
	}

	public boolean isActive() {
		return this.active;
	}

	public boolean isBugtrackerConnected() {
		return bugtrackerBinding != null;
	}

	public TestCaseLibrary getTestCaseLibrary() {
		return testCaseLibrary;
	}

	public void setTestCaseLibrary(TestCaseLibrary testCaseLibrary) {
		this.testCaseLibrary = testCaseLibrary;
		notifyLibraryAssociation(testCaseLibrary);
	}

	public RequirementLibrary getRequirementLibrary() {
		return requirementLibrary;
	}

	public void setRequirementLibrary(RequirementLibrary requirementLibrary) {
		this.requirementLibrary = requirementLibrary;
		notifyLibraryAssociation(requirementLibrary);
	}

	public CampaignLibrary getCampaignLibrary() {
		return campaignLibrary;
	}

	public void setCampaignLibrary(CampaignLibrary campaignLibrary) {
		this.campaignLibrary = campaignLibrary;
		notifyLibraryAssociation(campaignLibrary);
	}


	public CustomReportLibrary getCustomReportLibrary() {
		return customReportLibrary;
	}


	public void setCustomReportLibrary(CustomReportLibrary customReportLibrary) {
		this.customReportLibrary = customReportLibrary;
	}


	public BugTrackerBinding getBugtrackerBinding() {
		return bugtrackerBinding;
	}

	public void setBugtrackerBinding(BugTrackerBinding bugtrackerBinding) {
		this.bugtrackerBinding = bugtrackerBinding;
	}

	/**
	 * Notifies a library it was associated with this project.
	 *
	 * @param library
	 */
	private void notifyLibraryAssociation(GenericLibrary<?> library) {
		if (library != null) {
			library.notifyAssociatedWithProject(this);
		}
	}

	@Override
	public AttachmentList getAttachmentList() {
		return attachmentList;
	}

	/**
	 * will add a TestAutomationProject if it wasn't added already, or won't do anything if it was already bound to
	 * this.
	 *
	 * @param project
	 */
	public void bindTestAutomationProject(TestAutomationProject project) {
		for (TestAutomationProject proj : testAutomationProjects) {
			if (proj.getId().equals(project.getId())) {
				return;
			}
		}
		testAutomationProjects.add(project);
	}

	public void unbindTestAutomationProject(TestAutomationProject project) {
		Iterator<TestAutomationProject> iter = testAutomationProjects.iterator();
		while (iter.hasNext()) {
			TestAutomationProject proj = iter.next();
			if (proj.getId().equals(project.getId())) {
				iter.remove();
				break;
			}
		}
	}

	public void unbindTestAutomationProject(long taProjectId) {
		Iterator<TestAutomationProject> iter = testAutomationProjects.iterator();
		while (iter.hasNext()) {
			TestAutomationProject proj = iter.next();
			if (proj.getId().equals(taProjectId)) {
				iter.remove();
				break;
			}
		}
	}

	public boolean isTestAutomationEnabled() {
		return testAutomationServer != null;
	}

	public TestAutomationServer getTestAutomationServer() {
		return testAutomationServer;
	}

	public void setTestAutomationServer(TestAutomationServer server) {
		this.testAutomationServer = server;
	}

	public boolean hasTestAutomationProjects() {
		return !testAutomationProjects.isEmpty();
	}

	public Collection<TestAutomationProject> getTestAutomationProjects() {
		return testAutomationProjects;
	}

	/**
	 * returns true if the given TA project is indeed bound to the TM project
	 *
	 * @param p
	 * @return
	 */
	public boolean isBoundToTestAutomationProject(TestAutomationProject p) {
		return testAutomationProjects.contains(p);
	}

	/**
	 * returns a TestAutomationProject, bound to this TM project, that references the same job than the argument.
	 *
	 * @param p
	 * @return a TestAutomationProject if an equivalent was found or null if not
	 */
	public TestAutomationProject findTestAutomationProjectByJob(TestAutomationProject p) {
		for (TestAutomationProject mine : testAutomationProjects) {
			if (mine.referencesSameJob(p)) {
				return mine;
			}
		}
		return null;
	}

	public void removeBugTrackerBinding() {
		this.bugtrackerBinding = null;
	}

	/**
	 *
	 * @return the BugTracker the Project is bound to
	 * @throws NoBugTrackerBindingException
	 *             if the project is not BugtrackerConnected
	 */
	public BugTracker findBugTracker() {
		if (isBugtrackerConnected()) {
			return getBugtrackerBinding().getBugtracker();
		} else {
			throw new NoBugTrackerBindingException();
		}
	}



	public InfoList getRequirementCategories() {
		return requirementCategories;
	}

	public void setRequirementCategories(InfoList requirementCategories) {
		this.requirementCategories = requirementCategories;
	}

	public InfoList getTestCaseNatures() {
		return testCaseNatures;
	}

	public void setTestCaseNatures(InfoList testCaseNatures) {
		this.testCaseNatures = testCaseNatures;
	}


	public InfoList getTestCaseTypes() {
		return testCaseTypes;
	}

	public void setTestCaseTypes(InfoList testCaseTypes) {
		this.testCaseTypes = testCaseTypes;
	}

	public void setTestAutomationProjects(Set<TestAutomationProject> testAutomationProjects) {
		this.testAutomationProjects = testAutomationProjects;
	}

	public abstract void accept(ProjectVisitor visitor);

	public void unbindMilestone(Milestone milestone) {
		removeMilestone(milestone);
		milestone.removeProject(this);
	}

	/**
	 * CONSIDER THIS PRIVATE ! It should only be called by project.unbindMilestone or milestone.unbindProject
	 * TODO find a better design
	 * @param milestone
	 */
	public void removeMilestone(Milestone milestone) {
		Iterator<Milestone> iter = milestones.iterator();
		while (iter.hasNext()) {
			Milestone mil = iter.next();
			if (mil.getId().equals(milestone.getId())) {
				iter.remove();
				break;
			}
		}
	}

	public void unbindMilestones(List<Milestone> milestones) {
		// TODO arg could be Collection instead of List
		for (Milestone milestone : milestones) {
			unbindMilestone(milestone);
		}

	}

	public void addMilestone(Milestone milestone) {
		milestones.add(milestone);
	}

	public void bindMilestone(Milestone milestone) {
		milestones.add(milestone);
		milestone.addProject(this);
	}

	public void bindMilestones(List<Milestone> milestones) {
		for (Milestone milestone : milestones){
			bindMilestone(milestone);
		}

	}

	public boolean isBoundToMilestone(Milestone milestone) {
		return milestones.contains(milestone);
	}

	public void setAllowTcModifDuringExec(boolean allowTcModifDuringExec) {
		this.allowTcModifDuringExec = allowTcModifDuringExec;
	}

	public boolean allowTcModifDuringExec() {
		return this.allowTcModifDuringExec;
	}

}

