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

import org.imixs.marty.profile.UserController;
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
@ApplicationScoped
public class ModelController implements Serializable {

	private static final long serialVersionUID = 1L;

	private String systemModelVersion = null;

	private List<ItemCollection> initialProcessEntityList = null;

	private String workflowGroup = null;
	private String modelVersion = null;

	private HashMap<String, String> modelVersionCache = null;
	private HashMap<String, List<ItemCollection>> processEntityCache = null;
	private HashMap<String, ItemCollection> prjectEntityCache = null;

	@Inject
	private UserController userController = null;

	@Inject
	private FileUploadController fileUploadController;

	@EJB
	EntityService entityService;

	@EJB
	ModelService modelService;

	private static Logger logger = Logger.getLogger(ModelController.class
			.getName());

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
		initialProcessEntityList = null;
		modelVersionCache = new HashMap<String, String>();
		processEntityCache = new HashMap<String, List<ItemCollection>>();
		prjectEntityCache = new HashMap<String, ItemCollection>();

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
	 * This method returns a process entity for a given modelversion
	 * 
	 * @param modelVersion
	 *            - version for the model to search the process entity
	 * @param processid
	 *            - id of the process entity
	 * @return an instance of the matching process entity
	 */
	public ItemCollection getProcessEntity(int processid, String modelversion) {
		return modelService.getProcessEntityByVersion(processid, modelversion);
	}

	/**
	 * This method returns all process entities for modelVersion|workflowGroup
	 * This list can be used to display state/flow informations inside a form
	 * 
	 * The Method uses a local cache (processEntityCache) to cache processEntity
	 * lists for a Group/Model key
	 * 
	 * @return
	 */
	public List<ItemCollection> getProcessEntitiesByGroupVersion(String sGroup,
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
	 * Returns the processEntities for a given WorkflowGroup (txtWorkflowGorup)
	 * The method computes the modelVersion from the first matching process
	 * entity for the given group
	 * 
	 * @param sGroup
	 *            - name of a workflowGroup
	 * @return list of ProcessEntities for the given group
	 */
	public List<ItemCollection> getProcessEntitiesByGroup(String sGroup) {
		if (sGroup == null || sGroup.isEmpty())
			return null;

		// find Modelversion..
		List<ItemCollection> aprocessList = getInitialProcessEntities();
		for (ItemCollection aprocess : aprocessList) {
			if (sGroup.equals(aprocess.getItemValueString("txtWorkflowGroup"))) {
				return getProcessEntitiesByGroupVersion(sGroup,
						aprocess.getItemValueString("$modelVersion"));
			}

		}

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
	public List<ItemCollection> getInitialProcessEntities() {

		if (initialProcessEntityList == null) {
			// build new groupSelection
			initialProcessEntityList = new ArrayList<ItemCollection>();

			Collection<String> models = modelVersionCache.values();
			for (String modelVersion : models) {

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
	 * @return - a collection of ProcessEntities or an empty arrayList if not
	 *         processes are defined
	 */
	public List<ItemCollection> getInitialProcessEntitiesByProject(
			String uniqueid) {

		List<ItemCollection> result = new ArrayList<ItemCollection>();
		ItemCollection project = (ItemCollection) prjectEntityCache
				.get(uniqueid);
		if (project == null) {
			project = entityService.load(uniqueid);
			if (project != null)
				prjectEntityCache.put(uniqueid, project);
		}

		if (project == null)
			return result;

		List<String> aprocessList = null;

		aprocessList = project.getItemValue("txtprocesslist");

		// if no processList was defined return an empty array
		if (aprocessList == null || aprocessList.isEmpty())
			return result;

		// now add all matching workflowGroups
		List<ItemCollection> processEntityList = getInitialProcessEntities();
		for (ItemCollection aProcessEntity : processEntityList) {
			// test if the $modelVersion matches....
			if (isProcessEntityInList(aProcessEntity, aprocessList))
				result.add(aProcessEntity);
		}

		return result;

	}

	/**
	 * Returns a List with all available model versions
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
	 * Returns a list of all uploaded model profile entities.
	 * 
	 * @return list of ItemCollections
	 */
	public List<ItemCollection> getModelProfileList() {
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
	 * entity data like project data.
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
			logger.info("[SetupController] verifing file content....");
			JAXBContext context = JAXBContext
					.newInstance(EntityCollection.class);
			Unmarshaller m = context.createUnmarshaller();

			ByteArrayInputStream input = new ByteArrayInputStream(filestream);
			Object jaxbObject = m.unmarshal(input);
			if (jaxbObject == null) {
				throw new RuntimeException(
						"[SetupController] error - wrong xml file format - unable to import file!");
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
					logger.info("[SetupController] removing existing configuration for model version '"
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

				logger.info("[SetupController] " + ecol.getEntity().length
						+ " entries sucessfull imported");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// reinitialize models
		init();

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
