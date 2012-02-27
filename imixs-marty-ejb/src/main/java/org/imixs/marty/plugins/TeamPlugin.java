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

import javax.naming.InitialContext;

import org.imixs.marty.ejb.ProjectService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
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

	private ProjectService projectService = null;
	private EntityService entityService = null;

	@Override
	public void init(WorkflowContext actx) throws Exception {
		super.init(actx);
		// check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			WorkflowService ws = (WorkflowService) actx;
			// get latest model version....
			entityService = ws.getEntityService();
		}

		// lookup profile service EJB
		// String jndiName = "ejb/ProjectServiceBean";
		// InitialContext ictx = new InitialContext();

		// FooBean foo = (FooBean) new
		// InitialContext().lookup("java:module/FooBean");

		// Context ctx = (Context) ictx.lookup("java:comp/env");
		// projectService = (ProjectService) ctx.lookup(jndiName);
		projectService = (ProjectService) new InitialContext()
				.lookup("java:module/ProjectService");

	}

	public ProjectService getProjectService() {
		return projectService;
	}

	public EntityService getEntityService() {
		return entityService;
	}

	/**
	 * Each Workitem holds a reference to a Project in the attriubte
	 * '$uniqueidref'.
	 * <p>
	 * The Method updates the corresponding Project informations:
	 * <ul>
	 * <li>namProjectTeam
	 * <li>namProjectOwner
	 * <li>namProjectManager
	 * <li>namProjectAssist
	 * <li>namProjectName
	 * <li>namParentProjectTeam
	 * <li>namParentProjectOwner
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
	@SuppressWarnings("unchecked")
	@Override
	public int run(ItemCollection workItem, ItemCollection documentActivity)
			throws Exception {

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

			ItemCollection evalItemCollection=new ItemCollection();
			ResultPlugin.evaluate(sResult, evalItemCollection);
			
			
			// check for old pattern 'project=....' (deprecated!)
			/*
			if (sResult.indexOf("project=") > -1) {
				sResult = replaceDynamicValues(sResult, workItem);
				sProjectName = sResult
						.substring(sResult.indexOf("project=") + 8);
				// cut next newLine
				if (sProjectName.indexOf("\n") > -1)
					sProjectName = sProjectName.substring(0,
							sProjectName.indexOf("\n"));

			} else {
			*/
				// new Pattern
				sProjectName = evalItemCollection.getItemValueString("project");
			//}

			if (!"".equals(sProjectName)) {
				System.out
						.println("[WorkitemService] Updating Project reference: "
								+ sProjectName);
				// try to update project reference
				ItemCollection parent = this.projectService
						.findProjectByName(sProjectName);
				if (parent != null) {

					// assign project name and reference
					workItem.replaceItemValue("$uniqueidRef",
							parent.getItemValueString("$uniqueid"));
					workItem.replaceItemValue("txtProjectName",
							parent.getItemValueString("txtname"));

				}
			}

		} catch (Exception upr) {
			System.out
					.println("[WorkitemServiceBean] WARNING - Unable to update Project ("
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
			Vector vTeam = new Vector();
			Vector vOwner = new Vector();
			Vector vManager = new Vector();
			Vector vAssist = new Vector();
			Vector vParentTeam = new Vector();
			Vector vParentOwner = new Vector();
			Vector vParentManager = new Vector();
			Vector vParentAssist = new Vector();
			Vector vSubTeams = new Vector();
			Vector vSubOwner = new Vector();
			Vector vSubAssist = new Vector();
			Vector vSubManager = new Vector();
			 sProjectName = "";

			String parentType = itemColProject.getItemValueString("type");
			if ("project".equals(parentType)) {
				vTeam = itemColProject.getItemValue("namTeam");
				vOwner = itemColProject.getItemValue("namOwner");
				vManager = itemColProject.getItemValue("namManager");
				vAssist = itemColProject.getItemValue("namAssist");
				vParentTeam = itemColProject.getItemValue("namParentTeam");
				vParentOwner = itemColProject.getItemValue("namParentOwner");
				// now add subproject teams and owners to additional attributes
				// - if
				// available
				List<ItemCollection> subProjects = projectService
						.findAllSubProjects(sParentID, 0, -1);
				for (ItemCollection aSubProject : subProjects) {
					vSubTeams.addAll(aSubProject.getItemValue("namTeam"));
					vSubOwner.addAll(aSubProject.getItemValue("namOwner"));
					vSubManager.addAll(aSubProject.getItemValue("namManager"));
					vSubAssist.addAll(aSubProject.getItemValue("namAssist"));
				}

				sProjectName = itemColProject.getItemValueString("txtname");
			}
			if ("workitem".equals(parentType)) {
				vTeam = itemColProject.getItemValue("namProjectTeam");
				vOwner = itemColProject.getItemValue("namProjectOwner");
				vAssist = itemColProject.getItemValue("namProjectAssist");
				vManager = itemColProject.getItemValue("namProjectManager");
				vParentTeam = itemColProject
						.getItemValue("namParentProjectTeam");
				vParentOwner = itemColProject
						.getItemValue("namParentProjectOwner");
				vParentAssist = itemColProject
						.getItemValue("namParentProjectAssist");
				vParentManager = itemColProject
						.getItemValue("namParentProjectManager");
				vSubTeams = itemColProject.getItemValue("namSubProjectTeam");
				vSubOwner = itemColProject.getItemValue("namSubProjectOwner");
				vSubAssist = itemColProject.getItemValue("namSubProjectAssist");
				vSubManager = itemColProject
						.getItemValue("namSubProjectManager");

				sProjectName = itemColProject
						.getItemValueString("txtProjectName");
			}

			// update team lists
			workItem.replaceItemValue("namProjectTeam", vTeam);
			workItem.replaceItemValue("namProjectOwner", vOwner);
			workItem.replaceItemValue("namParentProjectTeam", vParentTeam);
			workItem.replaceItemValue("namParentProjectOwner", vParentOwner);
			workItem.replaceItemValue("namSubProjectTeam", vSubTeams);
			workItem.replaceItemValue("namSubProjectOwner", vSubOwner);

			workItem.replaceItemValue("namProjectAssist", vAssist);
			workItem.replaceItemValue("namProjectManager", vManager);
			workItem.replaceItemValue("namParentProjectAssist", vParentAssist);
			workItem.replaceItemValue("namParentProjectManager", vParentManager);
			workItem.replaceItemValue("namSubProjectAssist", vSubAssist);
			workItem.replaceItemValue("namSubProjectManager", vSubManager);

			// update project name
			workItem.replaceItemValue("txtProjectName", sProjectName);

		}
		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int arg0) throws Exception {
		// no op

	}

}