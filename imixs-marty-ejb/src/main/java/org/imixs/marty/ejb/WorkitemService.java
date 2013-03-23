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

import java.util.Collection;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.plugins.jee.extended.LucenePlugin;

/**
 * The WorkitemService provides methods to create, process and update a workItem
 * 
 * @author rsoika
 * 
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

	ItemCollection workItem = null;

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
	 * The Method creates an new property 'txtProjectName' which holds the
	 * property 'txtName' from the parent Workitem. This is normally the project
	 * name where the workItem is assigned to. But in cases where the WorkItem
	 * is created as a subprocess to another workItem the property can be mapped
	 * to an individual value provided by the Parent workItem.
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

		// lookup ProcessEntiy from the model
		ItemCollection processEntity = modelService.getProcessEntityByVersion(
				aProcessID, sProcessModelVersion);
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
		workItem = new ItemCollection();
		workItem.replaceItemValue("type", "workitem");
		workItem.replaceItemValue("$processID", processID);
		
		// set default writeAccess
		workItem.replaceItemValue("$writeAccess", workflowService.getUserName());

		// assign project name and reference
		workItem.replaceItemValue("$uniqueidRef",
				parent.getItemValueString("$uniqueid"));
		workItem.replaceItemValue("txtProjectName",
				parent.getItemValueString("txtname"));

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
	 * 
	 */
	public ItemCollection processWorkItem(ItemCollection aworkitem)
			throws AccessDeniedException, ProcessingErrorException {

		// Process workitem...
		workItem = workflowService.processWorkItem(aworkitem);

		return workItem;
	}

	/**
	 * This method moves a workitem into the archive by changing the type
	 * property
	 * 
	 * @param workitem
	 * @return
	 * @throws Exception
	 */
	public ItemCollection moveIntoArchive(ItemCollection workitem)
			throws Exception {
		if ("workitem".equals(workitem.getItemValueString("type"))) {

			String id = workitem.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = workflowService
					.getWorkListByRef(id);
			for (ItemCollection achildworkitem : col) {
				// recursive method call
				moveIntoArchive(achildworkitem);
			}
			workitem.replaceItemValue("type",
					workitem.getItemValueString("type") + "archive");

			workitem = entityService.save(workitem);

			// update search index
			try {
				LucenePlugin.updateWorkitem(workitem);
			} catch (Exception e) {
				// no op
			}
		}

		return workitem;
	}

	/**
	 * THie method restores a workitem from the archive by changing the type
	 * property
	 * 
	 * @param workitem
	 * @return
	 * @throws Exception
	 */
	public ItemCollection restoreFromArchive(ItemCollection workitem)
			throws Exception {

		if ("workitemarchive".equals(workitem.getItemValueString("type"))
				|| "childworkitemarchive".equals(workitem
						.getItemValueString("type"))) {

			String id = workitem.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = workflowService
					.getWorkListByRef(id);
			for (ItemCollection achildworkitem : col) {
				// recursive method call
				restoreFromArchive(achildworkitem);
			}
			if ("workitemarchive".equals(workitem.getItemValueString("type")))
				workitem.replaceItemValue("type", "workitem");
			if ("childworkitemarchive".equals(workitem
					.getItemValueString("type")))
				workitem.replaceItemValue("type", "childworkitem");
			workitem = entityService.save(workitem);

			// update search index
			try {
				LucenePlugin.updateWorkitem(workitem);
			} catch (Exception e) {
				// no op
			}
		}
		return workitem;
	}

	/**
	 * performs a soft delete by changing the type property
	 * 
	 */
	public ItemCollection moveIntoDeletions(ItemCollection workitem)
			throws Exception {
		if ("workitem".equals(workitem.getItemValueString("type"))
				|| "childworkitem".equals(workitem.getItemValueString("type"))) {

			String id = workitem.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = workflowService
					.getWorkListByRef(id);
			for (ItemCollection achildworkitem : col) {
				// recursive method call
				moveIntoDeletions(achildworkitem);
			}
			workitem.replaceItemValue("type",
					workitem.getItemValueString("type") + "deleted");
			workitem = entityService.save(workitem);

			// update search index
			try {
				LucenePlugin.updateWorkitem(workitem);
			} catch (Exception e) {
				// no op
			}
		}

		return workitem;
	}

	/**
	 * This method restores a delted workitem by changing the type property
	 * 
	 * @param workitem
	 * @return
	 * @throws Exception
	 */
	public ItemCollection restoreFromDeletions(ItemCollection workitem)
			throws Exception {

		if ("workitemdeleted".equals(workitem.getItemValueString("type"))
				|| "childworkitemdeleted".equals(workitem
						.getItemValueString("type"))) {

			String id = workitem.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = workflowService
					.getWorkListByRef(id);
			for (ItemCollection achildworkitem : col) {
				// recursive method call
				restoreFromDeletions(achildworkitem);
			}
			if ("workitemdeleted".equals(workitem.getItemValueString("type")))
				workitem.replaceItemValue("type", "workitem");
			if ("childworkitemdeleted".equals(workitem
					.getItemValueString("type")))
				workitem.replaceItemValue("type", "childworkitem");
			workitem = entityService.save(workitem);

			// update search index
			try {
				LucenePlugin.updateWorkitem(workitem);
			} catch (Exception e) {
				// no op

			}
		}

		return workitem;
	}

	/**
	 * This method delete a workitem. The method checks for child processes and
	 * runs a recursive deletion ...
	 */
	public void deleteWorkItem(ItemCollection workitem) throws Exception {
		String id = workitem.getItemValueString("$uniqueid");
		Collection<ItemCollection> col = workflowService.getWorkListByRef(id);
		for (ItemCollection achildworkitem : col) {
			// recursive method call
			deleteWorkItem(achildworkitem);
		}
		entityService.remove(workitem);
	}

}
