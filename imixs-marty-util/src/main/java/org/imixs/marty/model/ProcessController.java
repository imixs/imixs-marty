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
import java.util.Collections;
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
import org.imixs.marty.ejb.ProfileService;
import org.imixs.marty.workflow.WorkflowEvent;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.faces.util.LoginController;

/**
 * The ProcessController provides informations about the process and space
 * entities. A Workitem can be assigned to a process and one or more spaces. The
 * controller is session scoped and holds information depending on the current
 * user grants.
 * 
 * The ProcessController can load a process and provide agregated information
 * about the process like the Team or member lists
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
	private ItemCollection process = null;

	@Inject
	protected LoginController loginController = null;

	@EJB
	protected DocumentService documentService;

	@EJB
	protected ProcessService processService;

	@EJB
	protected ProfileService profileService;

	private static Logger logger = Logger.getLogger(ProcessController.class
			.getName());

	/**
	 * Reset the internal cache
	 */
	@PostConstruct
	public void reset() {
		spaces = null;
		processList = null;
		process = null;
	}

	/**
	 * Returns the current process entity
	 * 
	 * @return
	 */
	public ItemCollection getProcess() {
		return process;
	}

	/**
	 * Set the current process entity
	 * 
	 * @param process
	 */
	public void setProcess(ItemCollection process) {
		this.process = process;
	}

	/**
	 * Loads a process entity by its UniqueID from the internal cache and
	 * updates the current process entity.
	 * 
	 * @param uniqueid
	 *            - of process entity
	 * 
	 * @return current process entity
	 */
	public ItemCollection loadProcess(String uniqueid) {
		if (this.process == null
				|| !this.process.getItemValue(WorkflowService.UNIQUEID).equals(
						uniqueid)) {
			setProcess(this.getProcessById(uniqueid));
		}
		return getProcess();
	}

	/**
	 * Returns the process for a given uniqueID. The method uses the internal
	 * cache.
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
						.getItemValueString(WorkflowService.UNIQUEID))) {
					return process;
				}
			}
		}
		return null;
	}

	/**
	 * This method returns a chached list of process entities for the current user. This list
	 * can be used to display processs information. The returned
	 * process list is optimized and provides additional the following attributes
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
	 * This method returns a cached list of spaces for the current user. This list
	 * can be used to display space information. The returned
	 * space list is optimized and provides additional the following attributes
	 * <p>
	 * isMember, isTeam, isOwner, isManager, isAssist
	 * 
	 * @return
	 */
	public List<ItemCollection> getSpaces() {
		if (spaces == null) {
			spaces = processService.getSpaces(); new ArrayList<ItemCollection>();
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
					.getItemValueString(WorkflowService.UNIQUEID)))
				return aProcess;
		}

		// get the space list form local cache
		alist = getSpaces();
		for (ItemCollection aSpace : alist) {
			if (uniqueid.equals(aSpace
					.getItemValueString(WorkflowService.UNIQUEID)))
				return aSpace;
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
			List<ItemCollection> list = getSpaces();
			for (ItemCollection space : list) {
				if (uniqueId.equals(space
						.getItemValueString(WorkflowService.UNIQUEID))) {
					return space;
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
				if (name.equals(process.getItemValueString("txtName"))) {
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
				if (name.equals(process.getItemValueString("txtName"))) {
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
							.getItemValueString(WorkflowService.UNIQUEID))) {
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
				logger.fine("Spacename= " + space.getItemValueString("txtName")
						+ " uniquidref= "
						+ space.getItemValueString(WorkflowService.UNIQUEIDREF));
				if (uniqueId.equals(space
						.getItemValueString(WorkflowService.UNIQUEIDREF))) {
					result.add(space);
				}
			}
		}
		return result;
	}

	/**
	 * Returns a unique sorted list of managers for the current project. The
	 * returned list contains cloned user profile entities.
	 * 
	 *
	 * @return list of profile entities for the current team managers
	 */
	public List<ItemCollection> getManagers(String aUniqueID) {
		List<ItemCollection> resultList = getMemberListByRole(aUniqueID,
				"namManager");

		// sort by username..
		Collections.sort(resultList,
				new ItemCollectionComparator("txtUserName", true));

		return resultList;
	}

	/**
	 * Returns a unique sorted list of team members for the current project. The
	 * returned list contains cloned user profile entities.
	 * 
	 *
	 * @return list of profile entities for the current team members
	 */
	public List<ItemCollection> getTeam(String aUniqueID) {
		List<ItemCollection> resultList = getMemberListByRole(aUniqueID,
				"namTeam");

		// sort by username..
		Collections.sort(resultList,
				new ItemCollectionComparator("txtUserName", true));

		return resultList;
	}

	/**
	 * Returns a unique sorted list of assist members for the current project.
	 * The returned list contains cloned user profile entities.
	 * 
	 *
	 * @return list of profile entities for the current team members
	 */
	public List<ItemCollection> getAssist(String aUniqueID) {
		List<ItemCollection> resultList = getMemberListByRole(aUniqueID,
				"namAssist");

		// sort by username..
		Collections.sort(resultList,
				new ItemCollectionComparator("txtUserName", true));

		return resultList;
	}

	/**
	 * Returns a unique sorted list of all members (Managers, Team, Assist) for
	 * the current project. The returned list contains cloned user profile
	 * entities.
	 * 
	 *
	 * @return list of profile entities for the current team members
	 */
	public List<ItemCollection> getProcessMembers(String aUniqueID) {
		List<ItemCollection> resultList = new ArrayList<ItemCollection>();
		List<String> dupplicatedIds = new ArrayList<String>();

		List<ItemCollection> assistList = getMemberListByRole(aUniqueID,
				"namAssist");
		List<ItemCollection> teamList = getMemberListByRole(aUniqueID,
				"namTeam");
		List<ItemCollection> managerList = getMemberListByRole(aUniqueID,
				"namManager");

		for (ItemCollection profile : teamList) {
			// avoid duplicates..
			if (!dupplicatedIds.contains(profile
					.getItemValueString(WorkflowService.UNIQUEID))) {
				resultList.add(profile);
			}
			dupplicatedIds.add(profile
					.getItemValueString(WorkflowService.UNIQUEID));
		}
		for (ItemCollection profile : managerList) {
			// avoid duplicates..
			if (!dupplicatedIds.contains(profile
					.getItemValueString(WorkflowService.UNIQUEID))) {
				resultList.add(profile);
			}
			dupplicatedIds.add(profile
					.getItemValueString(WorkflowService.UNIQUEID));
		}
		for (ItemCollection profile : assistList) {
			// avoid duplicates..
			if (!dupplicatedIds.contains(profile
					.getItemValueString(WorkflowService.UNIQUEID))) {
				resultList.add(profile);
			}
			dupplicatedIds.add(profile
					.getItemValueString(WorkflowService.UNIQUEID));
		}

		// sort by username..
		Collections.sort(resultList,
				new ItemCollectionComparator("txtUserName", true));

		return resultList;
	}

	/**
	 * Returns true if current user is manager of a given space or process
	 * entity. Therefore the method checks the cloned field 'isManager'
	 * 
	 * @return
	 */
	public boolean isManagerOf(String aUniqueID) {
		// find Process/Space entity
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
	 * WorkflowEvent listener
	 * 
	 * If a project WorkItem was processed the modellController will be reseted.
	 * 
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

	/**
	 * Returns a unique sorted list of profile itemCollections for a team list
	 * in a project. The returned list contains cloned user profile entities.
	 * 
	 * @param listType
	 *            - the member field of the project (namTeam, namManager,
	 *            namAssist)
	 * @return list of team profiles
	 */
	@SuppressWarnings("unchecked")
	private List<ItemCollection> getMemberListByRole(String aUniqueID,
			String role) {
		List<ItemCollection> resultList = new ArrayList<ItemCollection>();
		List<String> dupplicatedIds = new ArrayList<String>();

		// find Process/Space entity
		ItemCollection entity = getEntityById(aUniqueID);
		if (entity == null)
			return resultList;

		List<String> members = entity.getItemValue(role);
		for (String member : members) {
			// avoid duplicates..
			if (!dupplicatedIds.contains(member)) {
				ItemCollection profile = profileService.findProfileById(member);
				if (profile != null) {
					resultList.add(profile);
				}
				dupplicatedIds.add(member);
			}
		}

		return resultList;
	}

}
