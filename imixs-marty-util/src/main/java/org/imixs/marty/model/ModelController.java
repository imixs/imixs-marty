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

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.ParserConfigurationException;

import org.imixs.marty.ejb.SetupService;
import org.imixs.marty.plugins.ApplicationPlugin;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.ModelService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.jee.faces.fileupload.FileData;
import org.imixs.workflow.jee.faces.fileupload.FileUploadController;
import org.xml.sax.SAXException;

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

	@Inject
	protected FileUploadController fileUploadController;

	@EJB
	protected EntityService entityService;

	@EJB
	protected ModelService modelService;

	@EJB
	protected WorkflowService workflowService;

	@EJB
	protected SetupService setupService;

	private static Logger logger = Logger.getLogger(ModelController.class.getName());

	/**
	 * returns a list of all ProcessEntities which are the first one in each
	 * ProcessGroup.
	 * 
	 * @return
	 */
	public List<ItemCollection> getAllStartProcessEntities(String version) {
		try {
			return modelService.getModel(version).findInitialTasks();
		} catch (ModelException e) {
			logger.warning("Unable to get Start Process list: " + e.getMessage());
			return new ArrayList<ItemCollection>();
		}
	}

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
	 * @throws ModelException
	 * 
	 * 
	 **/
	@PostConstruct
	public void reset() throws ModelException {
		systemModelVersion = null;
		workflowGroups = null;
		subWorkflowGroups = null;

		// now compute all workflow groups..
		workflowGroups = new HashMap<String, String>();
		subWorkflowGroups = new HashMap<String, String>();

		List<String> col;

		col = modelService.getAllModelVersions();

		for (String aModelVersion : col) {

			// Check if modelVersion is a System Model - latest version will
			// be stored
			if (aModelVersion.startsWith("system-")) {
				// test version
				if (systemModelVersion == null || systemModelVersion.compareTo(aModelVersion) < 0) {
					// update latest System model version
					systemModelVersion = aModelVersion;
				}
				continue;
			}

			List<String> groupList = modelService.getModel(aModelVersion).getGroups();
			for (String sGroupName : groupList) {
				if (sGroupName.contains("~")) {
					// test if the current version is higer than the last stored
					// version for this group
					String lastVersion = subWorkflowGroups.get(sGroupName);
					if (lastVersion == null || (lastVersion.compareTo(aModelVersion) < 0)) {
						subWorkflowGroups.put(sGroupName, aModelVersion);
					}
				} else {
					// test if the current version is higer than the last stored
					// version for this group
					String lastVersion = workflowGroups.get(sGroupName);
					if (lastVersion == null || (lastVersion.compareTo(aModelVersion) < 0)) {
						workflowGroups.put(sGroupName, aModelVersion);
					}
				}
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
			if (aGroupName.startsWith(parentWorkflowGroup + "~")) {
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
	 * @throws ModelException
	 */
	public List<ItemCollection> getAllProcessEntitiesByGroup(String sGroup, String sVersion) throws ModelException {

		setWorkflowGroup(sGroup);
		setModelVersion(sVersion);

		return modelService.getModel(sVersion).findTasksByGroup(sGroup);
	}

	

	
	
	/**
	 * This method adds all uploaded model files. The method tests the model
	 * type (.bmpm, .ixm). BPMN Model will be handled by the ImixsBPMNParser. A
	 * .ixm file will be imported using the default import mechanism.
	 * 
	 * @param event
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws ParseException
	 * @throws ModelException
	 * 
	 */
	public void doUploadModel(ActionEvent event)
			throws ModelException, ParseException, ParserConfigurationException, SAXException, IOException {
		List<FileData> fileList = fileUploadController.getUploades();
		for (FileData file : fileList) {
			logger.info("Model Upload started: " + file.getName());

			// test if bpmn model?
			if (file.getName().endsWith(".bpmn")) {
				BPMNModel model = BPMNParser.parseModel(file.getData(), "UTF-8");
				modelService.saveModelEntity(model);
				continue;
			}

			if (file.getName().endsWith(".ixm")) {
				setupService.importXmlEntityData(file.getData());
				continue;
			}

			// model type not supported!
			logger.warning("Invalid Model Type. Model can't be imported!");

		}
		fileUploadController.reset();
		// reinitialize models
		reset();
	}

	/**
	 * This Method deletes the given model
	 * 
	 * @throws AccessDeniedException
	 * @throws ModelException
	 */
	public void deleteModel(String modelversion) throws AccessDeniedException, ModelException {
		modelService.removeModel(modelversion);
		// reset model info
		reset();
	}

	
	/**
	 * This method returns a process entity for a given ModelVersion. 
	 * 
	 * 
	 * @param modelVersion
	 *            - version for the model to search the process entity
	 * @param processid
	 *            - id of the process entity
	 * @return an instance of the matching process entity
	 * @throws ModelException 
	 */
	public ItemCollection getProcessEntity(int processid, String modelversion) throws ModelException {
		return modelService.getModel(modelversion).getTask(processid);
	}

	/**
	 * This method return the 'rtfdescription' of a processentity and applies
	 * the dynamic Text replacement function from the jee plugin
	 * 
	 * @param processid
	 * @param modelversion
	 * @return
	 * @throws ModelException 
	 */
	public String getProcessDescription(int processid, String modelversion, ItemCollection documentContext) throws ModelException {
		ItemCollection pe = modelService.getModel(modelversion).getTask(processid);
		if (pe == null) {
			return "";
		}
		String desc = pe.getItemValueString("rtfdescription");
		ApplicationPlugin pp = new ApplicationPlugin();
		try {
			pp.init(workflowService);
			desc = pp.replaceDynamicValues(desc, documentContext);
		} catch (PluginException e) {
			logger.warning("Unable to update processDescription: " + e.getMessage());
		}

		return desc;

	}
}
