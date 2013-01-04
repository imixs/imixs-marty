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

import org.imixs.marty.util.WorkitemHelper;
import org.imixs.marty.workflow.WorkflowEvent;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.faces.util.LoginController;
 
/**
 * The ProcessController provides informations about the process and project
 * structure. The controller is session scoped and holds information depending
 * on the current user grants. The ProcessController interacts with the
 * application scoped ModelController which holds information about the workflow
 * models. *
 * 
 * @author rsoika
 * 
 */
@Named("processController")
@SessionScoped
public class ProcessController implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<ItemCollection> projectList = null;

	@Inject
	private LoginController loginController = null;

	
	@EJB
	EntityService entityService;

	
	private static Logger logger = Logger.getLogger(ProcessController.class
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

	}

	public void reset() {
		// initialProcessEntityList = null;
		projectList = null;
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
	public List<ItemCollection> getProjectList() {
		if (projectList == null) {
			projectList = new ArrayList<ItemCollection>();

			String sQuery = "SELECT projct FROM Entity AS projct "
					+ " JOIN projct.textItems AS t2"
					+ " WHERE projct.type = 'project'"
					+ " AND t2.itemName = 'txtname'"
					+ " ORDER BY t2.itemValue asc";
			Collection<ItemCollection> col = entityService.findAllEntities(
					sQuery, 0, -1);

			// create optimized list
			for (ItemCollection project : col) {

				ItemCollection clone = WorkitemHelper.clone(project);
				clone.replaceItemValue("isTeam", false);
				clone.replaceItemValue("isManager", false);

				// check the isTeam status for the current user
				List<String> userNameList = entityService.getUserNameList();
				Vector<String> vProjectNameList = (Vector<String>) project
						.getItemValue("namTeam");
				// check if one entry matches....
				for (String username : userNameList) {
					if (vProjectNameList.indexOf(username) > -1) {
						clone.replaceItemValue("isTeam", true);
						break;
					}
				}
				// check the isManager status for the current user
				vProjectNameList = (Vector<String>) project
						.getItemValue("namManager");
				// check if one entry matches....
				for (String username : userNameList) {
					if (vProjectNameList.indexOf(username) > -1) {
						clone.replaceItemValue("isManager", true);
						break;
					}
				}
				// check if user is member of team or manager list
				boolean bMember = false;
				if (clone.getItemValueBoolean("isTeam")
						|| clone.getItemValueBoolean("isManager"))
					bMember = true;
				clone.replaceItemValue("isMember", bMember);

				// add custom fields into clone...
				clone.replaceItemValue("txtProcessList",
						project.getItemValue("txtProcessList"));
				clone.replaceItemValue("txtdescription",
						project.getItemValue("txtdescription"));

				projectList.add(clone);

			}

		}

		return projectList;
	}

	/**
	 * this method finds a project by its UniqueID. The projectList is read from
	 * the project cache
	 * 
	 * @param uniqueid
	 * @return
	 */
	public ItemCollection getProjectByID(String uniqueid) {
		if (uniqueid == null || uniqueid.isEmpty())
			return null;

		// get the project list form local cache
		List<ItemCollection> projectList = getProjectList();
		for (ItemCollection aProject : projectList) {
			if (uniqueid.equals(aProject
					.getItemValueString(EntityService.UNIQUEID)))
				return aProject;
		}
		return null;

	}

	/**
	 * Returns a list of all projects which are siblings to a given project
	 * unqiueid.
	 * 
	 * @param uniqueIdRef
	 * @return
	 */
	public List<ItemCollection> getProjectsByRef(String uniqueIdRef) {

		List<ItemCollection> result = new ArrayList<ItemCollection>();

		if (uniqueIdRef != null) {
			// iterate over all projects and compare the $UniqueIDRef
			List<ItemCollection> list = getProjectList();
			for (ItemCollection project : list) {
				if (uniqueIdRef.equals(project
						.getItemValueString("$UniqueIDRef"))) {
					result.add(project);
				}

			}
		}
		return result;
	}

	

	/**
	 * Returns true if current user is manager of a given project. Therefore the
	 * method checks the cloned field 'isManager'
	 * 
	 * @return
	 */
	public boolean isProjectManager(String aProjectUniqueID) {
		// find project
		ItemCollection project = getProjectByID(aProjectUniqueID);
		if (project != null)
			return project.getItemValueBoolean("isManager");
		else
			return false;
	}

	/**
	 * Returns true if current user is member of the teamList of a given project
	 * Therefore the method checks the cloned field 'isTeam'
	 * 
	 * @return
	 */
	public boolean isProjectTeam(String aProjectUniqueID) {
		// find project
		ItemCollection project = getProjectByID(aProjectUniqueID);
		if (project != null)
			return project.getItemValueBoolean("isTeam");
		else
			return false;

	}

	/**
	 * Returns true if current user is teamMember or manager of a given project
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean isProjectMember(String aProjectUniqueID) {

		// find project
		ItemCollection project = getProjectByID(aProjectUniqueID);
		if (project != null) {
			String remoteUser = loginController.getUserPrincipal();
			List<String> vTeam = project.getItemValue("namTeam");
			List<String> vManager = project.getItemValue("namManager");

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
	 * @param workflowEvent
	 */
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent) {
		if (workflowEvent == null)
			return;

		if (WorkflowEvent.WORKITEM_AFTER_PROCESS == workflowEvent
				.getEventType()) {
			// test if project was processed
			if ("project".equals(workflowEvent.getWorkitem()
					.getItemValueString("type"))) {

				reset();
				logger.info("ModelController:WorkflowEvent="
						+ workflowEvent.getEventType());

			}
		}

	}

	
}
