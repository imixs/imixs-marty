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
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.ejb.ProcessService;
import org.imixs.marty.util.WorkitemHelper;
import org.imixs.marty.workflow.WorkflowEvent;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.faces.util.LoginController;

/**
 * The ProcessController provides informations about the process and space
 * entities. A Workitem can be assigned to a process and one or more spaces. The
 * controller is session scoped and holds information depending on the current
 * user grants.
 * 
 * The ProcessController interacts with the application scoped ModelController
 * which holds information about the workflow models.
 * 
 * @author rsoika
 * 
 */
@Named("processController")
@SessionScoped
public class ProcessController implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<ItemCollection> spaces = null;
	private List<ItemCollection> processList = null;

	@Inject
	protected LoginController loginController = null;

	@Inject
	protected ModelController modelController = null;

	@EJB
	protected EntityService entityService;
	
	@EJB
	protected ProcessService processService;

	private static Logger logger = Logger.getLogger(ProcessController.class
			.getName());

	/**
	 * Reset the internal cache
	 */
	@PostConstruct
	public void reset() {
		spaces = null;
		processList = null;
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
	public List<ItemCollection> getProcessList() {
		if (processList == null) {
			processList = processService.getProcessList();

		}

		return processList;
	}

	/**
	 * This method returns all space entities for the current user. This list
	 * can be used to display space informations inside a form. The returned
	 * space list is optimized and provides additional the following attributes
	 * <p>
	 * isMember, isTeam, isOwner, isManager, isAssist
	 * 
	 * @return
	 */
	public List<ItemCollection> getSpaces() {
		if (spaces == null) {
			spaces = new ArrayList<ItemCollection>();

			String sQuery = "SELECT space FROM Entity AS space "
					+ " JOIN space.textItems AS t2"
					+ " WHERE space.type = 'space'"
					+ " AND t2.itemName = 'txtname'"
					+ " ORDER BY t2.itemValue asc";
			Collection<ItemCollection> col = entityService.findAllEntities(
					sQuery, 0, -1);

			// create optimized list
			for (ItemCollection space : col) {

				ItemCollection clone = WorkitemHelper.clone(space);
				clone.replaceItemValue("isTeam", false);
				clone.replaceItemValue("isManager", false);

				// check the isTeam status for the current user
				List<String> userNameList = entityService.getUserNameList();
				Vector<String> vNameList = (Vector<String>) space
						.getItemValue("namTeam");
				// check if one entry matches....
				for (String username : userNameList) {
					if (vNameList.indexOf(username) > -1) {
						clone.replaceItemValue("isTeam", true);
						break;
					}
				}
				// check the isManager status for the current user
				vNameList = (Vector<String>) space.getItemValue("namManager");
				// check if one entry matches....
				for (String username : userNameList) {
					if (vNameList.indexOf(username) > -1) {
						clone.replaceItemValue("isManager", true);
						break;
					}
				}

				// check the isAssist status for the current user
				vNameList = (Vector<String>) space.getItemValue("namAssist");
				// check if one entry matches....
				for (String username : userNameList) {
					if (vNameList.indexOf(username) > -1) {
						clone.replaceItemValue("isAssist", true);
						break;
					}
				}

				// check if user is member of team or manager list
				boolean bMember = false;
				if (clone.getItemValueBoolean("isTeam")
						|| clone.getItemValueBoolean("isManager")
						|| clone.getItemValueBoolean("isAssist"))
					bMember = true;
				clone.replaceItemValue("isMember", bMember);

				// add custom fields into clone...
				clone.replaceItemValue("txtdescription",
						space.getItemValue("txtdescription"));

				spaces.add(clone);

			}

		}

		return spaces;
	}

	/**
	 * This method returns a space or process entity by its UniqueID. The space
	 * and process entities are read from the internal cache.
	 * 
	 * @param uniqueid
	 * @return
	 */
	public ItemCollection getEntityById(String uniqueid) {
		if (uniqueid == null || uniqueid.isEmpty())
			return null;

		// get the process list form local cache
		List<ItemCollection> alist = getProcessList();
		for (ItemCollection aProcess : alist) {
			if (uniqueid.equals(aProcess
					.getItemValueString(EntityService.UNIQUEID)))
				return aProcess;
		}

		// get the space list form local cache
		alist = getSpaces();
		for (ItemCollection aSpace : alist) {
			if (uniqueid.equals(aSpace
					.getItemValueString(EntityService.UNIQUEID)))
				return aSpace;
		}
		return null;

	}

	/**
	 * Returns the process for a given uniqueID.
	 * 
	 * @param uniqueId
	 * @return itemCollection of process or null if not process with the
	 *         specified id exists
	 */
	public ItemCollection getProcessById(String uniqueId) {

		if (uniqueId != null && !uniqueId.isEmpty()) {
			// iterate over all spaces and compare the $UniqueIDRef
			List<ItemCollection> list = getProcessList();
			for (ItemCollection process : list) {
				if (uniqueId.equals(process
						.getItemValueString(EntityService.UNIQUEID))) {
					return process;
				}
			}
		}
		return null;
	}

	/**
	 * Returns a Space for a given uniqueID.
	 * 
	 * @param uniqueId
	 * @return itemCollection of process or null if not process with the
	 *         specified id exists
	 */
	public ItemCollection getSpaceById(String uniqueId) {

		if (uniqueId != null && !uniqueId.isEmpty()) {
			// iterate over all spaces and compare the $UniqueIDRef
			List<ItemCollection> list = getProcessList();
			for (ItemCollection process : list) {
				if (uniqueId.equals(process
						.getItemValueString(EntityService.UNIQUEID))) {
					return process;
				}
			}
		}
		return null;
	}


	/**
	 * Returns a process by its name
	 * 
	 * @param name
	 * @return itemCollection of process or null if not process with the
	 *         specified id exists
	 */
	public ItemCollection getProcessByName(String name) {

		if (name != null && !name.isEmpty()) {
			// iterate over all processes and compare the txtname
			List<ItemCollection> list = getProcessList();
			for (ItemCollection process : list) {
				if (name.equals(process
						.getItemValueString("txtName"))) {
					return process;
				}
			}
		}
		return null;
	}

	/**
	 * Returns a space by its name
	 * 
	 * @param name
	 * @return itemCollection of process or null if not process with the
	 *         specified id exists
	 */
	public ItemCollection getSpaceByName(String name) {

		if (name != null && !name.isEmpty()) {
			// iterate over all processes and compare the txtname
			List<ItemCollection> list = getSpaces();
			for (ItemCollection process : list) {
				if (name.equals(process
						.getItemValueString("txtName"))) {
					return process;
				}
			}
		}
		return null;
	}

	
	
	
	
	
	/**
	 * Returns a list of all spaces which are assigned to a given process
	 * entity.
	 * 
	 * @param uniqueId
	 *            of a processEntity
	 * @return
	 */
	public List<ItemCollection> getSpacesByProcessId(String uniqueId) {
		List<ItemCollection> result = new ArrayList<ItemCollection>();
		if (uniqueId != null && !uniqueId.isEmpty()) {
			// find project
			ItemCollection process = getProcessById(uniqueId);
			result = getSpacesByProcess(process);
		}
		return result;
	}

	/**
	 * Returns a list of all spaces which are assigned to a given process
	 * entity.
	 * 
	 * @param uniqueId
	 *            of a processEntity
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<ItemCollection> getSpacesByProcess(ItemCollection process) {
		List<ItemCollection> result = new ArrayList<ItemCollection>();
		if (process != null) {
			Vector<String> refs = (Vector<String>) process
					.getItemValue("$UniqueIDRef");
			if (refs != null && !refs.isEmpty()) {
				// iterate over all spaces and compare the $UniqueIDRef
				List<ItemCollection> list = getSpaces();
				for (ItemCollection space : list) {

					if (refs.contains(space
							.getItemValueString(EntityService.UNIQUEID))) {
						result.add(space);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns a list of all spaces which are siblings to a given UniqueID.
	 * 
	 * @param uniqueId
	 * @return
	 */
	public List<ItemCollection> getSpacesByRef(String uniqueId) {

		List<ItemCollection> result = new ArrayList<ItemCollection>();

		if (uniqueId != null && !uniqueId.isEmpty()) {
			// iterate over all spaces and compare the $UniqueIDRef
			List<ItemCollection> list = getSpaces();
			for (ItemCollection space : list) {
				if (uniqueId.equals(space.getItemValueString("$UnqiueIDRef"))) {
					result.add(space);
				}
			}
		}
		return result;
	}

	/**
	 * Returns true if current user is manager of a given space or process
	 * entity. Therefore the method checks the cloned field 'isManager'
	 * 
	 * @return
	 */
	public boolean isManagerOf(String aUniqueID) {
		// find Space entity
		ItemCollection entity = getEntityById(aUniqueID);
		if (entity != null)
			return entity.getItemValueBoolean("isManager");
		else
			return false;
	}

	/**
	 * Returns true if current user is member of the teamList of a given project
	 * Therefore the method checks the cloned field 'isTeam'
	 * 
	 * @return
	 */
	public boolean isTeamMemberOf(String aUniqueID) {
		// find project
		ItemCollection entity = getEntityById(aUniqueID);
		if (entity != null)
			return entity.getItemValueBoolean("isTeam");
		else
			return false;

	}

	/**
	 * Returns true if current user is teamMember or manager of a given space or
	 * process
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean isMemberOf(String aUniqueID) {

		// find project
		ItemCollection entity = getEntityById(aUniqueID);
		if (entity != null) {
			String remoteUser = loginController.getUserPrincipal();
			List<String> vTeam = entity.getItemValue("namTeam");
			List<String> vManager = entity.getItemValue("namManager");

			if (vTeam.indexOf(remoteUser) > -1
					|| vManager.indexOf(remoteUser) > -1)
				return true;
		}

		return false;

	}

	/**
	 * Returns a list of ItemCollection representing the first Start Process
	 * defined for a specific space entity. Each ItemCollection provides at
	 * least the properties
	 * <ul>
	 * <li>txtmodelVersion (model version)
	 * <li>numprocessID (first process of a group)
	 * <li>txtWorklfowGroup (name of group)
	 * 
	 * 
	 * The worflowGroup list is used to display the start process list for a
	 * space selection
	 * 
	 * @param uniqueid
	 *            - $UniqueId of a project
	 * @return - a collection of ProcessEntities or an empty arrayList if not
	 *         processes are defined
	 */
	// public List<ItemCollection> getProcessEntitiesByCoreProcess(String
	// uniqueid) {
	//
	// List<ItemCollection> result = new ArrayList<ItemCollection>();
	// ItemCollection coreProcess = getCoreProcessByID(uniqueid);
	//
	// if (coreProcess == null)
	// return result;
	//
	// List<String> aprocessList = null;
	// aprocessList = coreProcess.getItemValue("txtprocesslist");
	//
	// // if no processList was defined return an empty array
	// if (aprocessList == null || aprocessList.isEmpty())
	// return result;
	//
	// // now add all matching Process Entities
	// List<ItemCollection> processEntityList = modelController
	// .getInitialProcessEntities();
	// for (ItemCollection aProcessEntity : processEntityList) {
	// // test if the $modelVersion matches....
	// if (isProcessEntityInList(aProcessEntity, aprocessList))
	// result.add(aProcessEntity);
	// }
	//
	// return result;
	//
	// }

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
			// test if a space or process entity was processed
			String sType = workflowEvent.getWorkitem().getItemValueString(
					"type");
			if ("space".equals(sType) || "process".equals(sType)) {

				reset();
				logger.fine("ModelController:WorkflowEvent="
						+ workflowEvent.getEventType());

			}
		}

	}

}
