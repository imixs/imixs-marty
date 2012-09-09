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
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.profile.UserController;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.ModelService;

/**
 * This backing beans provides informations about the Models provided in the
 * current workflow Instance.
 * 
 * There are two types of models handled. The System Model (used for project
 * management or Profile Settings) and the general Models used in a typical
 * workflow
 * 
 * A latest System Model depends on the current user locale. The general models
 * are language independent.
 * 
 * A ModelVersion is always expected in the format
 * 
 * 'domain-lang-version'
 * 
 * e.g. office-de-0.1, support-en-2.0, system-de-0.0.1
 * 
 * 
 * The Model Format handled by the ModelVersionHanler which is used by the
 * MyProfileMB is different from this format and was used in earlier versions of
 * ShareYourWork. We hope that we can drop the ModelVersionHandler sometimes....
 * 
 * @see org.imixs.marty.profile.UserController
 * @author rsoika
 * 
 */
@Named("modelController")
@SessionScoped
public class ModelController implements Serializable {

	private static final long serialVersionUID = 1L;

	private String latestSystemModelVersion = null;

	private List<ItemCollection> workflowGroups = null;
	private ArrayList<ItemCollection> projectList = null;
	private List<ItemCollection> processList = null;

	private String workflowGroup = null;
	private String modelVersion = null;

	private HashMap<String, String> modelVersionCache = null;

	@Inject
	private UserController userController = null;

	@EJB
	EntityService entityService;

	@EJB
	ModelService modelService;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

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
						&& (latestSystemModelVersion == null || latestSystemModelVersion
								.compareTo(aModelVersion) < 0)) {
					// update latest System model version
					latestSystemModelVersion = aModelVersion;
				}
				continue;
			}

			/*
			 * Now we have a general Model. So we test here if the current model
			 * version is higher then the last stored version of this domain
			 */
			String lastModel = (String) modelVersionCache.get(sDomain);
			if (lastModel == null || lastModel.compareTo(aModelVersion) < 0) {
				modelVersionCache.put(sDomain, aModelVersion);

			}

		}

	}

	public UserController getUserController() {
		return userController;
	}

	public void setUserController(UserController userController) {
		this.userController = userController;
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
	 * the filter options for the worklist view.
	 * 
	 * @return
	 */
	public List<ItemCollection> getWorkflowGroups() {

		if (workflowGroups == null) {
			// build new groupSelection
			workflowGroups = new ArrayList<ItemCollection>();

			Collection<String> models = modelVersionCache.values();
			for (String modelVersion : models) {

				List<ItemCollection> startProcessList = modelService
						.getAllStartProcessEntitiesByVersion(modelVersion);

				for (ItemCollection process : startProcessList) {
					String sGroupName = process
							.getItemValueString("txtWorkflowGroup");
					if (sGroupName.contains("~"))
						continue; // childProcess is skipped

					workflowGroups.add(process);
				}

			}

		}

		Collections.sort(workflowGroups, new WorkflowGroupComparator(
				FacesContext.getCurrentInstance().getViewRoot().getLocale(),
				true));

		return workflowGroups;

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
	 * @return
	 */
	public List<ItemCollection> getWorkflowGroupsByProject(
			ItemCollection project) {

		List<ItemCollection> result = new ArrayList<ItemCollection>();

		if (project == null)
			return result;

		List<String> processList = null;

		processList = project.getItemValue("txtprocesslist");
		
		// if no processList was defined return
		if (processList == null || processList.isEmpty())
			return result;

		// now add all matching workflowGroups
		List<ItemCollection> processEntityList = getWorkflowGroups();
		for (ItemCollection aProcessEntity : processEntityList) {
			// test if the $modelVersion matches....
			if (isProcessEntityInList(aProcessEntity, processList))
				result.add(aProcessEntity);
 
		}

		return result;

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
			List<String> processlist) {

		String startGroupVersion = startProcessEntity
				.getItemValueString("$ModelVersion");
		String startProcessID = ""
				+ startProcessEntity.getItemValueInteger("numProcessID");
		for (String processid : processlist) {
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
	 * This method returns all project entities for the current user. This list
	 * can be used to display project informations inside a form. The returned
	 * project list is optimized and provides additional the following attributes
	 * <p>
	 * isMember, isTeam, isOwner, isManager, isAssist
	 * 
	 * @return
	 */
	public List<ItemCollection> getProjectList() {
		if (projectList == null) {
			projectList = new ArrayList<ItemCollection>();

			String sUserID = getUserController().getUserPrincipal();

			String sQuery = "SELECT projct FROM Entity AS projct "
					+ " JOIN projct.textItems AS t2"
					+ " WHERE projct.type = 'project'"
					+ " AND t2.itemName = 'txtname'"
					+ " ORDER BY t2.itemValue asc";
			Collection<ItemCollection> col = entityService.findAllEntities(
					sQuery, 0, -1);

			// create optimized list
			for (ItemCollection project : col) {
				project.replaceItemValue(
						"isOwner",
						project.getItemValue("namOwner").indexOf(sUserID) > -1);
				project.replaceItemValue("isTeam",
						project.getItemValue("namTeam").indexOf(sUserID) > -1);
				project.replaceItemValue(
						"isAssist",
						project.getItemValue("namAssist").indexOf(sUserID) > -1);
				project.replaceItemValue(
						"isManager",
						project.getItemValue("namManager").indexOf(sUserID) > -1);

				boolean bMember = false;
				if (project.getItemValueBoolean("isTeam")
						|| project.getItemValueBoolean("isAssist"))
					bMember = true;
				project.replaceItemValue("isMember", bMember);

				projectList.add(project);

			}

		}

		return projectList;
	}

	/**
	 * This method returns all process entities for the current selected
	 * workflowGroup and modelVersion
	 * 
	 * This list can be used to display state/flow informations inside a form
	 * 
	 * @return
	 */
	public List<ItemCollection> getProcessList(String sGroup, String sVersion) {

		if (processList == null || !getWorkflowGroup().equals(sGroup)
				|| !getModelVersion().equals(sVersion)) {

			setWorkflowGroup(sGroup);
			setModelVersion(sVersion);
			logger.info("ModelControler: getAllProcessEntitiesByGroupByVersion");
			processList = modelService.getAllProcessEntitiesByGroupByVersion(
					getWorkflowGroup(), getModelVersion());

		}
		return processList;
	}

	private class WorkflowGroupComparator implements Comparator<ItemCollection> {
		private final Collator collator;

		private final boolean ascending;

		public WorkflowGroupComparator(Locale locale, boolean ascending) {
			this.collator = Collator.getInstance(locale);
			this.ascending = ascending;
		}

		public int compare(ItemCollection a, ItemCollection b) {
			int result = this.collator.compare(
					a.getItemValueString("txtWorkflowGroup"),
					b.getItemValueString("txtWorkflowGroup"));
			if (!this.ascending) {
				result = -result;
			}
			return result;
		}

	}

}
