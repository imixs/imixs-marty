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

package org.imixs.marty.ejb;

import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;

/**
 * The WorkitemService provides methods to create, process, update and remove a
 * workItem. The service can be used to all types of workitems (e.g. workitem,
 * process, space)
 * 
 * @author rsoika
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class WorkitemService {

	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	@EJB
	org.imixs.workflow.jee.ejb.WorkflowService workflowService;

	@EJB
	org.imixs.workflow.jee.ejb.ModelService modelService;
	
	private static Logger logger = Logger.getLogger(WorkitemService.class
			.getName());

	/**
	 * This method creates a new workItem. The workItem becomes a response
	 * object to the provided parent ItemCollection. The workItem will also be
	 * assigned to the ProcessEntity specified by the provided modelVersion and
	 * ProcessID. The method throws an exception if the ProcessEntity did not
	 * exist in the model.
	 * 
	 * The Method set the property '$WriteAccess' to the default value of the
	 * current Principal name. This allows initial updates of BlobWorkitems
	 * 
	 * The Attributes txtWorkflowEditor, numProcessID, $modelVersion and
	 * txtWrofklwoGroup will be set to the corresponding values of processEntity
	 * 
	 * @param parent
	 *            ItemCollection representing the parent where the workItem is
	 *            assigned to. This is typical a project entity but can also be
	 *            an other workItem
	 * @param processEntity
	 *            ItemCollection representing the ProcessEntity where the
	 *            workItem is assigned to
	 */
	public ItemCollection createWorkItem(ItemCollection parent,
			String sProcessModelVersion, int aProcessID) throws Exception {
		logger.fine("create workitem...");
		// lookup ProcessEntiy from the model
		ItemCollection processEntity = modelService.getModel(sProcessModelVersion).getTask(
				aProcessID);
		if (processEntity == null)
			throw new Exception(
					"error createWorkItem: Process Entity can not be found ("
							+ sProcessModelVersion + "|" + aProcessID + ")");

		String sEditorID = processEntity.getItemValueString("txteditorid");
		if ("".equals(sEditorID))
			sEditorID = "default";
		int processID = processEntity.getItemValueInteger("numProcessID");
		String sModelVersion = processEntity
				.getItemValueString("$modelversion");
		String sWorkflowGroup = processEntity
				.getItemValueString("txtworkflowgroup");

		// create empty workitem
		ItemCollection workItem = new ItemCollection();
		workItem.replaceItemValue("type", "workitem");
		workItem.replaceItemValue("$processID", processID);

		// set default writeAccess
		workItem.replaceItemValue("$writeAccess", workflowService.getUserName());

		// assign project name and reference
		workItem.replaceItemValue("$uniqueidRef",
				parent.getItemValueString("$uniqueid"));

		// assign ModelVersion, group and editor
		workItem.replaceItemValue("$modelversion", sModelVersion);
		workItem.replaceItemValue("txtworkflowgroup", sWorkflowGroup);
		workItem.replaceItemValue("txtworkfloweditorid", sEditorID);

		return workItem;

	}

	/**
	 * Processes a WorkItem.
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * @throws PluginException
	 * 
	 */
	public ItemCollection processWorkItem(ItemCollection aworkitem)
			throws AccessDeniedException, ProcessingErrorException,
			PluginException {
		// Process workitem...
		return workflowService.processWorkItem(aworkitem);
	}

}
