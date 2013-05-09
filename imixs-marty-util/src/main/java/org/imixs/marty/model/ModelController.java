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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.ejb.SetupService;
import org.imixs.marty.util.WorkitemComparator;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.ModelService;
import org.imixs.workflow.jee.faces.fileupload.FileData;
import org.imixs.workflow.jee.faces.fileupload.FileUploadController;

/**
 * The ModelController provides informations about workflow models.
 * 
 * A ModelVersion is always expected in the format
 * 
 * 'DOMAIN-LANGUAGE-VERSIONNUMBER'
 * 
 * e.g. office-de-0.1, support-en-2.0, system-de-0.0.1
 * 
 * 
 * @author rsoika
 * 
 */
@Named("modelController")
@ApplicationScoped
public class ModelController implements Serializable {

	private static final long serialVersionUID = 1L;

	private String systemModelVersion = null;

	private Map<String, String> workflowGroups = null;
	private Map<String, String> subWorkflowGroups = null;

	private String workflowGroup = null;
	private String modelVersion = null;

	private List<String> modelVersionCache = null;
	private HashMap<String, List<ItemCollection>> processEntityCache = null;

	@Inject
	private FileUploadController fileUploadController;

	@EJB
	EntityService entityService;

	@EJB
	ModelService modelService;

	@EJB
	SetupService setupService;

	private static Logger logger = Logger.getLogger(ModelController.class
			.getName());

	/**
	 * The method loads all model versions and store the latest version of a
	 * model domain into a cache from type list.
	 * <p>
	 * The model Version is expected in the format:
	 * <p>
	 * DOMAIN-LANGUAGE-VERSIONNUMBER
	 * <p>
	 * e.g. office-de-0.0.1
	 * <p>
	 * If a Modelversion did not contains at least 3 tokens an warning will be
	 * thrown.
	 * 
	 * 
	 **/
	@PostConstruct
	public void reset() {
		systemModelVersion = null;
		workflowGroups = null;
		subWorkflowGroups = null;
		modelVersionCache = new ArrayList<String>();
		processEntityCache = new HashMap<String, List<ItemCollection>>();

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
				// test version
				if (systemModelVersion == null
						|| systemModelVersion.compareTo(aModelVersion) < 0) {
					// update latest System model version
					systemModelVersion = aModelVersion;
				}
				continue;
			}

			/*
			 * Now we have a general Model. So we test here if the current model
			 * version is higher then the last stored version with the same
			 * domain in the modelVersionCache....
			 */
			boolean bModelCached = false;
			for (String aStoredModel : modelVersionCache) {
				// has a stored model version the same domain and is it older?
				if ((aStoredModel.startsWith(sDomain))
						&& (aStoredModel.compareTo(aModelVersion) < 0)) {
					modelVersionCache.remove(aStoredModel);
					modelVersionCache.add(aModelVersion);
					bModelCached = true;
					// leave the block now
					break;
				}
			}
			if (!bModelCached)
				modelVersionCache.add(aModelVersion);

		}
		// sort model versions
		Collections.sort(modelVersionCache);

		// now compute all workflow groups..
		workflowGroups = new HashMap<String, String>();
		subWorkflowGroups = new HashMap<String, String>();

		for (String aModelVersion : modelVersionCache) {
			List<String> groupList = modelService
					.getAllWorkflowGroupsByVersion(aModelVersion);
			for (String sGroupName : groupList) {
				if (sGroupName.contains("~"))
					subWorkflowGroups.put(sGroupName, aModelVersion);
				else
					workflowGroups.put(sGroupName, aModelVersion);
			}
		}

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
	 * determine if a system model is available
	 * 
	 * @return true if a system model was found
	 */
	public boolean hasSystemModel() {
		return (systemModelVersion != null && !systemModelVersion.isEmpty());
	}

	/**
	 * Returns a String list of all WorkflowGroup names. The workflow group
	 * names are stored in a internal map with the corresponding model version.
	 * This map is used to get the current model version for a specified group
	 * name.
	 * 
	 * A workflowGroup with a '~' in its name will be skipped. This indicates a
	 * child process.
	 * 
	 * The worflowGroup list is used to assign a workflow Group to a core
	 * process.
	 * 
	 * @return list of workflow groups
	 */
	public List<String> getWorkflowGroups() {

		// return a sorted list of all workflow groups....
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<String> aList = new ArrayList(workflowGroups.keySet());
		Collections.sort(aList);

		return aList;

	}

	/**
	 * Returns a String list of all Sub-WorkflowGroup names for a specified
	 * WorkflowGroup.
	 * 
	 * 
	 * A SubWorkflowGroup contains a '~' in its name.
	 * 
	 * The SubWorflowGroup list is used to assign sub workflow Group to a
	 * workitem
	 * 
	 * @see getWorkflowGroups()
	 * 
	 * @param parentWorkflowGroup
	 *            - the parent workflow group name
	 * @return list of all sub workflow groups for the given parent group name
	 */
	public List<String> getSubWorkflowGroups(String parentWorkflowGroup) {

		List<String> aList = new ArrayList<String>();
		if (parentWorkflowGroup == null || parentWorkflowGroup.isEmpty())
			return aList;

		for (String aGroupName : subWorkflowGroups.keySet()) {
			if (aGroupName.startsWith(workflowGroup + "~")) {
				aList.add(aGroupName);
			}
		}

		Collections.sort(aList);
		return aList;
	}

	/**
	 * This method returns all process entities for a specific workflowGroup and
	 * modelVersion. This list can be used to display state/flow informations
	 * inside a form depending on the current workflow process information
	 * stored in a workItem.
	 * 
	 * The Method uses a local cache (processEntityCache) to cache a collection
	 * of process entities for the key 'workflowGroup|modelVersion'.
	 * 
	 * In most cases it is sufficient to use the method
	 * getAllProcessEntitiesByGroup(group) which returns the process entity list
	 * for the latest model version.
	 * 
	 * @param sGroup
	 *            - name of a workflow group
	 * @param sVersion
	 *            - model version
	 * @return list of process entities for the specified workflowGroup and
	 *         modelVersion (cached)
	 */
	public List<ItemCollection> getAllProcessEntitiesByGroup(String sGroup,
			String sVersion) {
		List<ItemCollection> result = null;

		if (sGroup != null && sVersion != null) {

			setWorkflowGroup(sGroup);
			setModelVersion(sVersion);

			// test if list is already cached?
			result = processEntityCache.get(sGroup + "|" + sVersion);
			if (result == null) {
				logger.fine("ModelControler getProcessEntities for '" + sGroup
						+ "|" + sVersion + "'");
				result = modelService.getAllProcessEntitiesByGroupByVersion(
						getWorkflowGroup(), getModelVersion());
				// cache result
				processEntityCache.put(sGroup + "|" + sVersion, result);
			}
		}
		return result;
	}

	/**
	 * Returns a list of all ProcessEntities for a specified workflow group. The
	 * list of ProcessEntities depends on the latest modelVersion where the
	 * groupName is listed.
	 * 
	 * The method uses a internal cache.
	 * 
	 * @see getAllProcessEntitiesByGroup(groupName,modelVersion)
	 * 
	 * @param groupName
	 *            - name of a workflow group
	 * @return list of ProcessEntities or an empty list if no process entities
	 *         for the specified group exists.
	 */
	public List<ItemCollection> getAllProcessEntitiesByGroup(String groupName) {
		// find the matching latest ModelVersion for this group
		String sModelVersion = workflowGroups.get(groupName);
		if (sModelVersion == null)
			// check sub workflow groups
			sModelVersion = subWorkflowGroups.get(groupName);
		if (sModelVersion == null)
			logger.warning("[ModelController] WorkflowGroup '" + groupName
					+ "' not defined in latest model version!");
		return getAllProcessEntitiesByGroup(groupName, sModelVersion);
	}

	/**
	 * Returns the first ProcessEntity in a specified workflow group
	 * 
	 * @param group
	 *            - name of group
	 * @return initial ProcessEntity or null if group not found
	 */
	public ItemCollection getInitialProcessEntityByGroup(String group) {
		List<ItemCollection> aProcessList = getAllProcessEntitiesByGroup(group);
		if (aProcessList.size() > 0)
			return aProcessList.get(0);
		else
			return null;
	}

	/**
	 * Returns a List with all available model versions. The list contains the
	 * latest version of each model group.
	 * 
	 * @return list of model versions
	 */
	public List<String> getModelVersions() {
		return modelVersionCache;
	}

	/**
	 * Returns a list of all uploaded model profile entities. This list is used
	 * to give an overview about all uploaded models. Different versions of the
	 * same model group will be returned.
	 * 
	 * @see modellist.xhtml
	 * @return list of ItemCollections
	 */
	public List<ItemCollection> getAllProfileEntities() {
		List<ItemCollection> result = new ArrayList<ItemCollection>();
		List<ItemCollection> colEntities = modelService
				.getEnvironmentEntityList();
		for (ItemCollection aworkitem : colEntities) {
			String sName = aworkitem.getItemValueString("txtName");
			if ("environment.profile".equals(sName)) {
				result.add(aworkitem);
			}
		}
		return result;
	}

	/**
	 * Returns the profile entity for a given model version.
	 * 
	 * @return ItemCollection
	 */
	public ItemCollection getProfileEntityByVersion(String sVersion) {
		List<ItemCollection> profiles = getAllProfileEntities();
		for (ItemCollection aworkitem : profiles) {
			if (sVersion.equals(sVersion))
				return aworkitem;

		}
		return null;
	}

	/**
	 * adds all uploaded files.
	 * 
	 * @param event
	 * 
	 */
	public void doUploadModel(ActionEvent event) {

		List<FileData> fileList = fileUploadController.getUploadedFiles();
		for (FileData file : fileList) {

			logger.info("ModelController - starting xml model file upload: "
					+ file.getName());
			setupService.importXmlEntityData(file.getData());

		}

		fileUploadController.doClear(null);

		// reinitialize models
		reset();
	}

	/**
	 * This Method deletes the given model
	 * 
	 * @throws AccessDeniedException
	 */
	public void deleteModel(String currentModelVersion)
			throws AccessDeniedException {

		String sQuery;
		sQuery = "SELECT process FROM Entity AS process "
				+ "	 JOIN process.textItems as t"
				+ "	 JOIN process.textItems as v"
				+ "	 WHERE t.itemName = 'type' AND t.itemValue IN('ProcessEntity', 'ActivityEntity', 'WorkflowEnvironmentEntity')"
				+ " 	 AND v.itemName = '$modelversion' AND v.itemValue = '"
				+ currentModelVersion + "'";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, -1);

		for (ItemCollection aworkitem : col) {
			entityService.remove(aworkitem);
		}

		// reset model info
		reset();
	}

	/**
	 * This method returns a process entity for a given ModelVersion
	 * 
	 * @param modelVersion
	 *            - version for the model to search the process entity
	 * @param processid
	 *            - id of the process entity
	 * @return an instance of the matching process entity
	 */
	@Deprecated
	public ItemCollection xxxgetProcessEntity(int processid, String modelversion) {
		return modelService.getProcessEntityByVersion(processid, modelversion);
	}

	/**
	 * Returns the processEntities for a given WorkflowGroup (txtWorkflowGorup)
	 * The method computes the modelVersion from the first matching process
	 * entity for the given group
	 * 
	 * @param sGroup
	 *            - name of a workflowGroup
	 * @return list of ProcessEntities for the given group
	 */
	@Deprecated
	public List<ItemCollection> xxxgetProcessEntitiesByGroup(String sGroup) {
		if (sGroup == null || sGroup.isEmpty())
			return null;

		// find Modelversion..
		// List<ItemCollection> aprocessList = getInitialProcessEntities();
		// for (ItemCollection aprocess : aprocessList) {
		// if (sGroup.equals(aprocess.getItemValueString("txtWorkflowGroup"))) {
		// return getProcessEntitiesByGroupVersion(sGroup,
		// aprocess.getItemValueString("$modelVersion"));
		// }
		//
		// }

		return null;
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
	@Deprecated
	public List<ItemCollection> xxxxgetInitialProcessEntities() {
		List initialProcessEntityList = null;
		if (initialProcessEntityList == null) {
			// build new groupSelection
			initialProcessEntityList = new ArrayList<ItemCollection>();
			for (String modelVersion : modelVersionCache) {

				List<ItemCollection> startProcessList = modelService
						.getAllStartProcessEntitiesByVersion(modelVersion);

				for (ItemCollection process : startProcessList) {
					String sGroupName = process
							.getItemValueString("txtWorkflowGroup");
					if (sGroupName.contains("~"))
						continue; // childProcess is skipped

					initialProcessEntityList.add(process);
				}

			}

			Collections.sort(initialProcessEntityList, new WorkitemComparator(
					"txtWorkflowGroup", true));
		}

		return initialProcessEntityList;

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
	@Deprecated
	private boolean xxxxisProcessEntityInList(
			ItemCollection startProcessEntity, List<String> aprocesslist) {

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
