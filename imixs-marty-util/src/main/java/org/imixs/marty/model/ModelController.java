/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.marty.model;

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.profile.UserController;
import org.imixs.marty.util.WorkitemComparator;
import org.imixs.marty.util.WorkitemHelper;
import org.imixs.marty.workflow.WorkflowEvent;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.ModelService;

/**
 * The ModelController provides informations about workflow models as also the
 * process and project structure.
 * 
 * A ModelVersion is always expected in the format
 * 
 * 'DOMAIN-LANGUAGE-VERSIONNUMBER'
 * 
 * e.g. office-de-0.1, support-en-2.0, system-de-0.0.1
 * 
 * 
 * The ModelController observes WorkflowEvents and rests the internal cache if a
 * project was updated.
 * 
 * @author rsoika
 * 
 */
@Named("modelController")
@SessionScoped
public class ModelController implements Serializable {

	private static final long serialVersionUID = 1L;

	private String systemModelVersion = null;

	private List<ItemCollection> processList = null;
	private List<ItemCollection> projectList = null;
	// private List<ItemCollection> processList = null;

	private String workflowGroup = null;
	private String modelVersion = null;

	private HashMap<String, String> modelVersionCache = null;

	@Inject
	private UserController userController = null;

	@EJB
	EntityService entityService;

	@EJB
	ModelService modelService;

	private static Logger logger = Logger.getLogger("org.imixs.marty");

	/**
	 * The init method is used load all model versions and store the latest
	 * version of a model domain into a list.
	 * <p>
	 * The model Version is either expected in the format:
	 * <p>
	 * DOMAIN-LANGUAGE-VERSIONNUMBER
	 * <p>
	 * e.g. office-de-0.0.1
	 * <p>
	 * 
	 * <p>
	 * The modelCache uses the first part (without the version) as a key to find
	 * the latest version of a domain model. So the system can deal with
	 * multiple versions of the same domain.
	 * <p>
	 * The method getStartProcessList reads the cached model versions. This
	 * method can also compare the modelversion to the userprofile settings. In
	 * this case the first part (domain) and the second token (language) are
	 * relevant.
	 * <p>
	 * if a Modelversion did not contains at least 3 tokens an warning will be
	 * thrown.
	 * 
	 * 
	 **/
	@PostConstruct
	public void init() {

		modelVersionCache = new HashMap<String, String>();

		List<String> col;

		col = modelService.getAllModelVersions();

		for (String aModelVersion : col) {

			// Test if the ModelVersion has the expected format
			// DOMAIN-LANG-VERSION
			StringTokenizer stFormat = new StringTokenizer(aModelVersion, "-");
			if (stFormat.countTokens() < 2) {
				// skip invalid format
				logger.warning("[WARNING] Invalid ModelFormat : "
						+ aModelVersion + " can not be used!");

				continue;
			}

			// now we parse the model version (e.g. office-de-0.0.1)
			String sDomain = stFormat.nextToken();
			String sLanguage = stFormat.nextToken();
			String sVersion = stFormat.nextToken();

			// Check if modelVersion is a System Model - latest version will
			// be stored
			if ("system".equals(sDomain)) {
				// test language and version
				if (sLanguage.equals(userController.getLocale())
						&& (systemModelVersion == null || systemModelVersion
								.compareTo(aModelVersion) < 0)) {
					// update latest System model version
					systemModelVersion = aModelVersion;
				}
				continue;
			}

			/*
			 * Now we have a general Model. So we test here if the current model
			 * version is higher then the last stored version of this domain
			 */
			String lastModel = modelVersionCache.get(sDomain);
			if (lastModel == null || lastModel.compareTo(aModelVersion) < 0) {
				modelVersionCache.put(sDomain, aModelVersion);

			}

		}

	}

	public void reset() {
		processList = null;
		projectList = null;
	}

	public String getWorkflowGroup() {
		if (workflowGroup == null)
			workflowGroup = "";
		return workflowGroup;
	}

	public void setWorkflowGroup(String workflowGroup) {
		this.workflowGroup = workflowGroup;
	}

	public String getModelVersion() {
		if (modelVersion == null)
			modelVersion = "";
		return modelVersion;
	}

	public void setModelVersion(String modelVersion) {
		this.modelVersion = modelVersion;
	}

	public String getSystemModelVersion() {
		return systemModelVersion;
	}

	/**
	 * Returns a list of ItemCollection representing the first ProcessEntity for
	 * each available WorkflowGroup. Each ItemCollection provides at least the
	 * properties
	 * <ul>
	 * <li>txtmodelVersion (model version)
	 * <li>numprocessID (first process of a group)
	 * <li>txtWorklfowGroup (name of group)
	 * 
	 * A workflowGroup with a '~' in its name will be skipped. This indicates a
	 * child process.
	 * 
	 * The worflowGroup list is used to display the process navigation bar and
	 * the filter options for the workList view.
	 * 
	 * @return
	 */
	public List<ItemCollection> getProcessList() {

		if (processList == null) {
			// build new groupSelection
			processList = new ArrayList<ItemCollection>();

			Collection<String> models = modelVersionCache.values();
			for (String modelVersion : models) {

				List<ItemCollection> startProcessList = modelService
						.getAllStartProcessEntitiesByVersion(modelVersion);

				for (ItemCollection process : startProcessList) {
					String sGroupName = process
							.getItemValueString("txtWorkflowGroup");
					if (sGroupName.contains("~"))
						continue; // childProcess is skipped

					processList.add(process);
				}

			}

			Collections.sort(processList, new WorkitemComparator(
					"txtWorkflowGroup", true));
		}

		return processList;

	}

	/**
	 * Returns a list of ItemCollection representing the first ProcessEntity for
	 * each available WorkflowGroup defined for a specific project entity. Each
	 * ItemCollection provides at least the properties
	 * <ul>
	 * <li>txtmodelVersion (model version)
	 * <li>numprocessID (first process of a group)
	 * <li>txtWorklfowGroup (name of group)
	 * 
	 * A workflowGroup with a '~' in its name will be skipped. This indicates a
	 * child process.
	 * 
	 * The worflowGroup list is used to display the start process list for a
	 * project
	 * 
	 * @param uniqueid
	 *            - $UniqueId of a project
	 * @return - a collection of ProcessEntities
	 */
	public List<ItemCollection> getProcessListByProject(String uniqueid) {

		List<ItemCollection> result = new ArrayList<ItemCollection>();
		ItemCollection project = getProjectByID(uniqueid);
		if (project == null)
			return result;

		List<String> aprocessList = null;

		aprocessList = project.getItemValue("txtprocesslist");

		// if no processList was defined return
		if (aprocessList == null || aprocessList.isEmpty())
			return result;

		// now add all matching workflowGroups
		List<ItemCollection> processEntityList = getProcessList();
		for (ItemCollection aProcessEntity : processEntityList) {
			// test if the $modelVersion matches....
			if (isProcessEntityInList(aProcessEntity, aprocessList))
				result.add(aProcessEntity);

		}

		for (ItemCollection atest : result) {
			String s = atest.getItemValueString("txtname");
			logger.info(s);
		}

		return result;

	}

	/**
	 * This method returns all project entities for the current user. This list
	 * can be used to display project informations inside a form. The returned
	 * project list is optimized and provides additional the following
	 * attributes
	 * <p>
	 * isMember, isTeam, isOwner, isManager, isAssist
	 * 
	 * @return
	 */
	public List<ItemCollection> getProjectList() {
		if (projectList == null) {
			projectList = new ArrayList<ItemCollection>();

			String sUserID = userController.getUserPrincipal();

			String sQuery = "SELECT projct FROM Entity AS projct "
					+ " JOIN projct.textItems AS t2"
					+ " WHERE projct.type = 'project'"
					+ " AND t2.itemName = 'txtname'"
					+ " ORDER BY t2.itemValue asc";
			Collection<ItemCollection> col = entityService.findAllEntities(
					sQuery, 0, -1);

			// create optimized list
			for (ItemCollection project : col) {

				ItemCollection clone = WorkitemHelper.clone(project);

				clone.replaceItemValue("isTeam", project
						.getItemValue("namTeam").indexOf(sUserID) > -1);
				clone.replaceItemValue(
						"isManager",
						project.getItemValue("namManager").indexOf(sUserID) > -1);

				boolean bMember = false;
				if (clone.getItemValueBoolean("isTeam")
						|| project.getItemValueBoolean("namManager"))
					bMember = true;
				clone.replaceItemValue("isMember", bMember);

				// add custom fields into clone...
				clone.replaceItemValue("txtProcessList",
						project.getItemValue("txtProcessList"));
				clone.replaceItemValue("txtdescription",
						project.getItemValue("txtdescription"));

				projectList.add(clone);

			}

		}

		return projectList;
	}

	/**
	 * this method finds a project by its UniqueID. The projectlist is read from
	 * the project cache
	 * 
	 * @param uniqueid
	 * @return
	 */
	public ItemCollection getProjectByID(String uniqueid) {
		if (uniqueid == null || uniqueid.isEmpty())
			return null;

		List<ItemCollection> projectList = getProjectList();
		for (ItemCollection aProject : projectList) {
			if (uniqueid.equals(aProject
					.getItemValueString(EntityService.UNIQUEID)))
				return aProject;
		}
		return null;

	}

	/**
	 * Returns a list of all projects which are siblings to a given project
	 * unqiueid.
	 * 
	 * @param uniqueIdRef
	 * @return
	 */
	public List<ItemCollection> getProjectsByRef(String uniqueIdRef) {

		List<ItemCollection> result = new ArrayList<ItemCollection>();

		if (uniqueIdRef != null) {
			// iterate over all projects and compare the $UniqueIDRef
			List<ItemCollection> list = getProjectList();
			for (ItemCollection project : list) {
				if (uniqueIdRef.equals(project
						.getItemValueString("$UniqueIDRef"))) {
					result.add(project);
				}

			}
		}
		return result;
	}

	/**
	 * Returns a List with all availabarg0le model versions
	 * 
	 * @return
	 */
	public List<String> getModelVersions() {

		Collection<String> col = modelVersionCache.values();

		List<String> versions = new ArrayList<String>();

		for (String aversion : col) {
			versions.add(aversion);
		}

		Collections.sort(versions);
		return versions;

	}

	/**
	 * This method returns all process entities for modelVersion|workflowGroup
	 * 
	 * This list can be used to display state/flow informations inside a form
	 * 
	 * @return
	 */
	public List<ItemCollection> getProcessEntities(String sGroup,
			String sVersion) {

		if (sGroup != null && sVersion != null) {

			setWorkflowGroup(sGroup);
			setModelVersion(sVersion);
			logger.info("ModelControler: AChtung - diese Methode sollte einen Caching mechanismus unterstützen !!! getAllProcessEntitiesByGroupByVersion");
			List<ItemCollection> result = modelService
					.getAllProcessEntitiesByGroupByVersion(getWorkflowGroup(),
							getModelVersion());
			return result;
		}
		return null;
	}

	/**
	 * This method computes the modelVersion and than returns the list
	 * 
	 * @param sGroup
	 * @return
	 */
	public List<ItemCollection> getProcessEntities(String sGroup) {
		if (sGroup == null || sGroup.isEmpty())
			return null;

		// find Modelversion..
		List<ItemCollection> aprocessList = getProcessList();
		for (ItemCollection aprocess : aprocessList) {
			if (sGroup.equals(aprocess.getItemValueString("txtWorkflowGroup"))) {
				return getProcessEntities(sGroup,
						aprocess.getItemValueString("$modelVersion"));
			}

		}

		return null;
	}

	/**
	 * Returns true if current user is manager of a given project.
	 * Therefore the method checks the cloned field 'isManager'
	 * 
	 * @return
	 */
	public boolean isProjectManager(String aProjectUniqueID) {
		// find project
		ItemCollection project = getProjectByID(aProjectUniqueID);
		if (project != null)
			return project.getItemValueBoolean("isManager");
		else
			return false;
	}

	/**
	 * Returns true if current user is member of the teamList of a given project
	 * Therefore the method checks the cloned field 'isTeam'
	 *
	 * @return
	 */
	public boolean isProjectTeam(String aProjectUniqueID) {
		// find project
		ItemCollection project = getProjectByID(aProjectUniqueID);
		if (project != null)
			return project.getItemValueBoolean("isTeam");
		else
			return false;

	}

	/**
	 * Returns true if current user is teamMember or manager of a given project
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean isProjectMember(String aProjectUniqueID) {

		// find project
		ItemCollection project = getProjectByID(aProjectUniqueID);
		if (project != null) {
			String remoteUser = userController.getUserPrincipal();
			List<String> vTeam = project.getItemValue("namTeam");
			List<String> vManager = project.getItemValue("namManager");

			if (vTeam.indexOf(remoteUser) > -1
					|| vManager.indexOf(remoteUser) > -1)
				return true;
		}

		return false;

	}

	/**
	 * WorkflowEvent listener
	 * 
	 * If a project WorkItem was processed the modellController will be reseted.
	 * 
	 * @param workflowEvent
	 */
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent) {
		if (workflowEvent == null)
			return;

		if (WorkflowEvent.WORKITEM_AFTER_PROCESS == workflowEvent
				.getEventType()) {
			// test if project was processed
			if ("project".equals(workflowEvent.getWorkitem()
					.getItemValueString("type"))) {

				reset();
				logger.info("ModelController:WorkflowEvent="
						+ workflowEvent.getEventType());

			}
		}

	}

	/**
	 * This method compares a list of $modelversion|numprocessid with a given
	 * startProcesEntity. If modelversion an processID matches the method
	 * returns true.
	 * 
	 * @param ProcessEntity
	 *            - itemCollection of start process
	 * @param processlist
	 *            - $modelversion|numprocessid
	 * @return
	 */
	private boolean isProcessEntityInList(ItemCollection startProcessEntity,
			List<String> aprocesslist) {

		String startGroupVersion = startProcessEntity
				.getItemValueString("$ModelVersion");
		String startProcessID = ""
				+ startProcessEntity.getItemValueInteger("numProcessID");
		for (String processid : aprocesslist) {
			String sModelVersion = null;
			String sProcessID = null;
			if (processid.indexOf('|') > -1) {
				sModelVersion = processid.substring(0, processid.indexOf('|'));
				sProcessID = processid.substring(processid.indexOf('|') + 1);
				if (startGroupVersion.equals(sModelVersion)
						&& startProcessID.equals(sProcessID))
					return true;
			}
		}
		return false;

	}

}
