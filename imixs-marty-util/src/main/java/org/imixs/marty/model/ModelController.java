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

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.imixs.marty.util.WorkitemComparator;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.ModelService;
import org.imixs.workflow.jee.faces.fileupload.FileData;
import org.imixs.workflow.jee.faces.fileupload.FileUploadController;
import org.imixs.workflow.xml.EntityCollection;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

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

	// private List<ItemCollection> initialProcessEntityList = null;
	private Map<String, String> workflowGroups = null;

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
	public void init() {
		// initialProcessEntityList = null;
		workflowGroups = null;
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
		for (String aModelVersion : modelVersionCache) {
			List<String> groupList = modelService
					.getAllWorkflowGroupsByVersion(aModelVersion);
			for (String sGroupName : groupList) {
				if (sGroupName.contains("~"))
					continue; // childProcess is skipped
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
	 * Returns a List with all available model versions
	 * 
	 * @return list of model versions
	 */
	public List<String> getModelVersions() {
		return modelVersionCache;
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
			importXmlEntityData(file.getData());

		}

		fileUploadController.doClear(null);
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
		init();
	}

	/**
	 * this method imports an xml entity data stream. This is used to provide
	 * model uploads during the system setup. The method can also import general
	 * entity data like configuration data.
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void importXmlEntityData(byte[] filestream) {
		XMLItemCollection entity;
		ItemCollection itemCollection;
		String sModelVersion = null;

		if (filestream == null)
			return;
		try {
			EntityCollection ecol = null;
			logger.info("[ModelController] importXmlEntityData - verifing file content....");
			JAXBContext context = JAXBContext
					.newInstance(EntityCollection.class);
			Unmarshaller m = context.createUnmarshaller();

			ByteArrayInputStream input = new ByteArrayInputStream(filestream);
			Object jaxbObject = m.unmarshal(input);
			if (jaxbObject == null) {
				throw new RuntimeException(
						"[ModelController] error - wrong xml file format - unable to import file!");
			}

			ecol = (EntityCollection) jaxbObject;

			// import entities....
			if (ecol.getEntity().length > 0) {

				Vector<String> vModelVersions = new Vector<String>();
				// first iterrate over all enttity and find if model entries are
				// included
				for (XMLItemCollection aentity : ecol.getEntity()) {
					itemCollection = XMLItemCollectionAdapter
							.getItemCollection(aentity);
					// test if this is a model entry
					// (type=WorkflowEnvironmentEntity)
					if ("WorkflowEnvironmentEntity".equals(itemCollection
							.getItemValueString("type"))
							&& "environment.profile".equals(itemCollection
									.getItemValueString("txtName"))) {

						sModelVersion = itemCollection
								.getItemValueString("$ModelVersion");
						if (vModelVersions.indexOf(sModelVersion) == -1)
							vModelVersions.add(sModelVersion);
					}
				}
				// now remove old model entries....
				for (String aModelVersion : vModelVersions) {
					logger.info("[ModelController] removing existing configuration for model version '"
							+ aModelVersion + "'");
					modelService.removeModelVersion(aModelVersion);
				}
				// save new entities into database and update modelversion.....
				for (int i = 0; i < ecol.getEntity().length; i++) {
					entity = ecol.getEntity()[i];
					itemCollection = XMLItemCollectionAdapter
							.getItemCollection(entity);
					// save entity
					entityService.save(itemCollection);
				}

				logger.info("[ModelController] " + ecol.getEntity().length
						+ " entries sucessfull imported");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// reinitialize models
		init();

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
	 * Returns a list of all uploaded model profile entities.
	 * 
	 * @return list of ItemCollections
	 */
	@Deprecated
	public List<ItemCollection> xxxgetModelProfileList() {
		List<ItemCollection> result = new ArrayList<ItemCollection>();
		Collection<ItemCollection> colEntities = modelService
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
