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

package org.imixs.marty.plugins;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.plugins.ResultPlugin;
import org.imixs.workflow.plugins.jee.AbstractPlugin;

/**
 * This plugin supports additional workflow properties for further processing.
 * The method computes the team members of a corresponding parent project so
 * these teams can be used in security and mail plugins.
 * <p>
 * The Plugin should run first
 * 
 * @author rsoika
 * 
 */
public class TeamPlugin extends AbstractPlugin {

	//private ProjectService projectService = null;
	private EntityService entityService = null;
	private static Logger logger = Logger.getLogger("org.imixs.marty");

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
		// check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			WorkflowService ws = (WorkflowService) actx;
			// get latest model version....
			entityService = ws.getEntityService();
		}

	

	}



	/**
	 * Each Workitem holds a reference to a Project in the attriubte
	 * '$uniqueidref'.
	 * <p>
	 * The Method updates the corresponding Project informations:
	 * <ul>
	 * <li>namProjectTeam
	 * <li>namProjectManager
	 * <li>namProjectName
	 * <li>namParentProjectTeam
	 * <li>namParentProjectManager
	 * 
	 * Additional the Method also computes the values namSubProjectTeam
	 * namSubProjectOwner which holds all members of all subprojects
	 * 
	 * If the workitem is not a child to a project but to a other workitem
	 * (Child Process) then these informations are fetched from the parent
	 * workitem.
	 * 
	 * Finally the Method updates the field "txtProjectname" with the name of
	 * the parent project.
	 * 
	 **/
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public int run(ItemCollection workItem, ItemCollection documentActivity)
			throws PluginException {

		String sProjectName = null;
		String sResult = null;

		/*
		 * the following code updates a project reference if the
		 * workflowresultmessage contains a project= ref test if a new project
		 * reference need to be set now! we support both - old pattern
		 * 'project=....' and new pattern
		 * 
		 * '<item name="_project">...</item>'
		 */

		try {
			// Read workflow result directly from the activity definition
			sResult = documentActivity.getItemValueString("txtActivityResult");

			ItemCollection evalItemCollection = new ItemCollection();
			ResultPlugin.evaluate(sResult, evalItemCollection);

			sProjectName = evalItemCollection.getItemValueString("project");
		
			if (!"".equals(sProjectName)) {
				logger.fine("[WorkitemService] Updating Project reference: "
								+ sProjectName);
				// try to update project reference
				ItemCollection parent =findProjectByName(sProjectName);
				if (parent != null) {

					// assign project name and reference
					workItem.replaceItemValue("$uniqueidRef",
							parent.getItemValueString("$uniqueid"));
					workItem.replaceItemValue("txtProjectName",
							parent.getItemValueString("txtname"));

				}
			}

		} catch (Exception upr) {
			logger.warning("[WorkitemServiceBean] WARNING - Unable to update Project ("
							+ sResult + ") reference: " + upr);
		}

		/*
		 * Now the team lists will be updated depending of the current
		 * $uniqueidref
		 */

		String sParentID = workItem.getItemValueString("$uniqueidref");

		// now add project Team and Owners to the current Workitem

		// there are two different situations.
		// if the parent is a project the method fetches team and owner form
		// project
		// if the parent is a workitem the method fetches the team and owner
		// from the workitem

		ItemCollection itemColProject = entityService.load(sParentID);
		if (itemColProject != null) {
			List vTeam = new Vector();
			List vManager = new Vector();
			List vParentTeam = new Vector();
			List vParentManager = new Vector();
			List vSubTeams = new Vector();
			List vSubManager = new Vector();
			sProjectName = "";

			String parentType = itemColProject.getItemValueString("type");
			if ("project".equals(parentType)) {
				vTeam = itemColProject.getItemValue("namTeam");
				vManager = itemColProject.getItemValue("namManager");
				vParentTeam = itemColProject.getItemValue("namParentTeam");
				// now add subproject teams and owners to additional attributes
				// - if
				// available 
				List<ItemCollection> subProjects = findAllSubProjects(sParentID);
				for (ItemCollection aSubProject : subProjects) {
					vSubTeams.addAll(aSubProject.getItemValue("namTeam"));
					vSubManager.addAll(aSubProject.getItemValue("namManager"));
				}

				sProjectName = itemColProject.getItemValueString("txtname");
			}
			if ("workitem".equals(parentType)) {
				vTeam = itemColProject.getItemValue("namProjectTeam");
				vManager = itemColProject.getItemValue("namProjectManager");
				vParentTeam = itemColProject
						.getItemValue("namParentProjectTeam");
				vParentManager = itemColProject
						.getItemValue("namParentProjectManager");
				vSubTeams = itemColProject.getItemValue("namSubProjectTeam");
				vSubManager = itemColProject
						.getItemValue("namSubProjectManager");

				sProjectName = itemColProject
						.getItemValueString("txtProjectName");
			}

			// update team lists
			workItem.replaceItemValue("namProjectTeam", vTeam);
			workItem.replaceItemValue("namParentProjectTeam", vParentTeam);
			workItem.replaceItemValue("namSubProjectTeam", vSubTeams);
			workItem.replaceItemValue("namProjectManager", vManager);
			workItem.replaceItemValue("namParentProjectManager", vParentManager);
			workItem.replaceItemValue("namSubProjectManager", vSubManager);

			// update project name
			workItem.replaceItemValue("txtProjectName", sProjectName);

		}
		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int arg0) throws PluginException {
		// no op

	}

	

	private List<ItemCollection> findAllSubProjects(String sIDRef) {

		String sQuery = "SELECT project FROM Entity AS project "
				+ " JOIN project.textItems AS r"
				+ " JOIN project.textItems AS n"
				+ " WHERE project.type = 'project'"
				+ " AND n.itemName = 'txtname' "
				+ " AND r.itemName='$uniqueidref'" + " AND r.itemValue = '"
				+ sIDRef + "' " + " ORDER BY n.itemValue asc";

		return entityService.findAllEntities(sQuery, 0, -1);
	}
	
	
	/**
	 * This method returns a project ItemCollection for a specified name.
	 * Returns null if no project with the provided name was found
	 * 
	 */
	public ItemCollection findProjectByName(String aName) {
		String sQuery = "SELECT project FROM Entity AS project "
				+ " JOIN project.textItems AS t2"
				+ " WHERE  project.type = 'project' "
				+ " AND t2.itemName = 'txtname' " + " AND t2.itemValue = '"
				+ aName + "'";

		List<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);
		if (col.size() > 0)
			return col.iterator().next();
		else
			return null;
	}
}
