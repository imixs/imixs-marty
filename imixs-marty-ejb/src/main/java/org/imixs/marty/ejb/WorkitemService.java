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
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.lucene.LuceneUpdateService;

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

	@EJB 
	LuceneUpdateService luceneUpdateService;

	
	ItemCollection workItem = null;

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

		// lookup ProcessEntiy from the model
		ItemCollection processEntity = modelService.getProcessEntity(
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
		workItem = workflowService.processWorkItem(aworkitem);

		return workItem;
	}

	/**
	 * This method performs a soft delete by changing the type property. The
	 * method appends the sufix 'deleted' to the current type value.
	 * 
	 * The method can be called on all types of workitems. Also process or space
	 * entities can be deleted. The param 'recursive' indicates if also
	 * references to this workitem should be deleted recursively.
	 * 
	 * The method also updates the attribute namcurrenteditor with the current
	 * user name.
	 * 
	 * The method did not change a workitem if the type property still ends with
	 * the sufix 'deleted'
	 * 
	 * @param workitem
	 *            - the workitem to be soft deleted
	 * @param recursive
	 *            - if true also references to the current workitem will be soft
	 *            deleted
	 * @return deleted workitem
	 * @throws AccessDeniedException
	 *             - if user is not allowed to update the workitem
	 */
	public ItemCollection softDeleteWorkitem(ItemCollection workitem,
			boolean recursive) throws AccessDeniedException {
		if (workitem == null)
			return null;
		logger.fine("[WorkitemService] softDeleteWorkitem: '"
				+ workitem.getItemValueString(EntityService.UNIQUEID)
				+ "' recursive=" + recursive);

		String sType = workitem.getItemValueString("type");

		if (!sType.endsWith("deleted")) {
			// recursive soft delete?
			if (recursive) {
				String id = workitem.getItemValueString("$uniqueid");
				Collection<ItemCollection> col = workflowService
						.getWorkListByRef(id);
				for (ItemCollection achildworkitem : col) {
					// recursive method call
					softDeleteWorkitem(achildworkitem, recursive);
				}
			}
			workitem.replaceItemValue("type",
					workitem.getItemValueString("type") + "deleted");
			workitem.replaceItemValue("namcurrenteditor",
					workflowService.getUserName());
			workitem = entityService.save(workitem);

			// update search index
			try {
				luceneUpdateService.updateWorkitem(workitem);
			} catch (Exception e) {
				// no op
				logger.finest("[WorkitemService] moveIntoDeletesions: unable to update lucene index");
			}
		}

		return workitem;
	}

	/**
	 * This method archives a workitem by changing the property 'type'. The
	 * method appends the sufix 'archive' to the current type value.
	 * 
	 * The method can be called on all types of workitems. Also process or space
	 * entities can be deleted. The param 'recursive' indicates if also
	 * references to this workitem should be deleted recursively.
	 * 
	 * The method also updates the attribute namcurrenteditor with the current
	 * user name.
	 * 
	 * The method did not change a workitem if the type property still ends with
	 * the sufix 'archive'
	 * 
	 * @param workitem
	 *            - the workitem to be archived
	 * @param recursive
	 *            - if true also references to the current workitem will be
	 *            archived
	 * @return archived workitem
	 * @throws AccessDeniedException
	 *             - if user is not allowed to update the workite
	 */
	public ItemCollection archiveWorkitem(ItemCollection workitem,
			boolean recursive) throws AccessDeniedException {
		if (workitem == null)
			return null;

		logger.fine("[WorkitemService] archiveWorkitem: '"
				+ workitem.getItemValueString(EntityService.UNIQUEID)
				+ "' recursive=" + recursive);

		String sType = workitem.getItemValueString("type");
		if (!sType.endsWith("archive")) {
			if (recursive) {
				String id = workitem.getItemValueString("$uniqueid");
				Collection<ItemCollection> col = workflowService
						.getWorkListByRef(id);
				for (ItemCollection achildworkitem : col) {
					// recursive method call
					archiveWorkitem(achildworkitem, recursive);
				}
			}
			workitem.replaceItemValue("namcurrenteditor",
					workflowService.getUserName());
			workitem.replaceItemValue("type",
					workitem.getItemValueString("type") + "archive");

			workitem = entityService.save(workitem);

			// update search index
			try {
				luceneUpdateService.updateWorkitem(workitem);
			} catch (Exception e) {
				// no op
			}
		}

		return workitem;
	}

	/**
	 * THie method restores a workitem from the archive by changing the type
	 * property. Also references to this workitem will be updated recursively.
	 * 
	 * The method also updates the attribute namcurrenteditor with the current
	 * user name.
	 * 
	 * @param workitem
	 *            - to be restored from the archive
	 * @return - restored workitem
	 * @throws AccessDeniedException
	 *             - if user is not allowed to update the workitem
	 */
	public ItemCollection restoreFromArchive(ItemCollection workitem)
			throws AccessDeniedException {
		if (workitem == null)
			return null;
		logger.fine("[WorkitemService] restoreFromDeletions: '"
				+ workitem.getItemValueString(EntityService.UNIQUEID) + "'");

		String sType = workitem.getItemValueString("type");
		if ((sType.startsWith("workitem") || sType.startsWith("childworkitem"))
				&& sType.endsWith("archive")) {

			String id = workitem.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = workflowService
					.getWorkListByRef(id);
			for (ItemCollection achildworkitem : col) {
				// recursive method call
				restoreFromArchive(achildworkitem);
			}
			// cut the 'deleted' part from the type property
			if (sType.endsWith("archive")) {
				String sTypeNew = sType.substring(0, sType.indexOf("archive"));
				workitem.replaceItemValue("type", sTypeNew);
				workitem.replaceItemValue("namcurrenteditor",
						workflowService.getUserName());
			}

			workitem = entityService.save(workitem);

			// update search index
			try {
				luceneUpdateService.updateWorkitem(workitem);
			} catch (Exception e) {
				// no op
			}
		}
		return workitem;
	}

	/**
	 * This method restores a soft deleted workitem by changing the type
	 * property. Also references to this workitem will be updated recursively.
	 * 
	 * The method also updates the attribute namcurrenteditor with the current
	 * user name.
	 * 
	 * @param workitem
	 *            to be deleted
	 * @return restored workitem
	 * @throws AccessDeniedException
	 */
	public ItemCollection restoreFromDeletions(ItemCollection workitem)
			throws AccessDeniedException {
		if (workitem == null)
			return null;
		String sType = workitem.getItemValueString("type");

		logger.fine("[WorkitemService] restoreFromDeletions: '"
				+ workitem.getItemValueString(EntityService.UNIQUEID) + "'");

		if (sType.endsWith("deleted")) {

			String id = workitem.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = workflowService
					.getWorkListByRef(id);
			for (ItemCollection achildworkitem : col) {
				// recursive method call
				restoreFromDeletions(achildworkitem);
			}

			// cut the 'deleted' part from the type property
			if (sType.endsWith("deleted")) {
				String sTypeNew = sType.substring(0, sType.indexOf("deleted"));
				workitem.replaceItemValue("type", sTypeNew);
				workitem.replaceItemValue("namcurrenteditor",
						workflowService.getUserName());
			}

			workitem = entityService.save(workitem);

			// update search index
			try {
				luceneUpdateService.updateWorkitem(workitem);
			} catch (Exception e) {
				// no op

			}
		}

		return workitem;
	}

	/**
	 * This method delete a workitem. The method can be called on all types of
	 * workitems. Also process or space entities can be deleted. The param
	 * 'recursive' indicates if also references to this workitem should be
	 * deleted recursively.
	 * 
	 * @param workitem
	 *            - the workitem to be soft deleted
	 * @param recursive
	 *            - if true also references to the current workitem will be soft
	 *            deleted
	 * @throws AccessDeniedException
	 *             - if user is not allowed to delete the workitem
	 */
	public void deleteWorkItem(ItemCollection workitem, boolean recursive)
			throws AccessDeniedException {
		if (workitem == null)
			return;

		logger.fine("[WorkitemService] deleteWorkItem: '"
				+ workitem.getItemValueString(EntityService.UNIQUEID)
				+ "' recursive=" + recursive);

		if (recursive) {
			String id = workitem.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = workflowService
					.getWorkListByRef(id);
			for (ItemCollection achildworkitem : col) {
				// recursive method call
				deleteWorkItem(achildworkitem, recursive);
			}
		}
		entityService.remove(workitem);

	}

}
