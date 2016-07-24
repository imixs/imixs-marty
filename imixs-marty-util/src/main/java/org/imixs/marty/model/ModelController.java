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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
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
@Named()
@RequestScoped
public class ModelController implements Serializable {

	private static final long serialVersionUID = 1L;

	private Map<String, String> subWorkflowGroups = null;

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
	 * Returns a String list of all WorkflowGroup names.
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

		Set<String> set = new HashSet<String>();
		List<String> versions = modelService.getVersions();
		for (String version : versions) {
			try {
				set.addAll(modelService.getModel(version).getGroups());
			} catch (ModelException e) {
				e.printStackTrace();
			}
		}
		List<String> result = new ArrayList<>();

		result.addAll(set);
		Collections.sort(result);
		return result;

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
	 * This method returns the first task in a workflow group. The method computes the
	 * latest model version for the group.
	 * 
	 * @param sGroup
	 *            - name of a workflow group
	 * @return task
	 * @throws ModelException
	 */
	public ItemCollection getInitialTaskByGroup(String sGroup) {
		List<ItemCollection> tasks = null;
		try {
			List<String> versions = modelService.findVersionsByGroup(sGroup);
			if (!versions.isEmpty()) {
				String version = versions.get(0);
				tasks = modelService.getModel(version).findTasksByGroup(sGroup);
				if (tasks != null && tasks.size() > 0) {
					return tasks.get(0);
				} 
			}
		} catch (ModelException e) {
			logger.fine(e.getMessage());
		}
		logger.warning("no matching model version found for group '" + sGroup + "'");
		return null;
	}
	
	/**
	 * returns all model versions
	 * @return
	 */
	public List<String> getVersions() {
		return modelService.getVersions();
	}

	public ItemCollection getModelEntity(String version) {
		return modelService.loadModelEntity(version);
	}
	
	/**
	 * return sall groups for a version
	 * @param version
	 * @return
	 */
	public List<String> getGroups(String version) {
		try {
			return modelService.getModel(version).getGroups();
		} catch (ModelException e) {
			logger.warning("Unable to load groups:" + e.getMessage());
		}
		// return empty result
		return new ArrayList<String>();
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
	}

	/**
	 * This Method deletes the given model
	 * 
	 * @throws AccessDeniedException
	 * @throws ModelException
	 */
	public void deleteModel(String modelversion) throws AccessDeniedException, ModelException {
		modelService.removeModel(modelversion);
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
	public String getProcessDescription(int processid, String modelversion, ItemCollection documentContext)
			throws ModelException {
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
