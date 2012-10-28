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
import org.imixs.workflow.plugins.jee.AbstractPlugin;

/**
 * This plug-in supports additional business logic for project entities. The
 * plug-in updates the properties txtName and txtProjectName. It also compute
 * the parent team members and the team members of subprojects.
 * 
 * @author rsoika
 * 
 */
public class ProjectPlugin extends AbstractPlugin {

	private EntityService entityService = null;
	private static Logger logger = Logger.getLogger("org.imixs.marty");
	private ItemCollection project = null;

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);

		// check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			WorkflowService ws = (WorkflowService) actx;
			entityService = ws.getEntityService();
		}

	}

	/**
	 * The Plugin verifies if the workitem is from the type 'project'. Only in
	 * this case the plugin will update project Information and updates parent
	 * and subProjects.
	 * 
	 * The plugin also updates the name and access fields of a project.
	 * 
	 * The current user has to have sufficient access for these operations!
	 **/
	@SuppressWarnings("unchecked")
	@Override
	public int run(ItemCollection aworkItem, ItemCollection documentActivity)
			throws PluginException {
		project = null;

		// verify workitem type
		if (!"project".equals(aworkItem.getItemValueString("type")))
			return Plugin.PLUGIN_OK;

		project = aworkItem;

		updateProject();
		
		// Verify if namTeam or namOwner is empty!
		List<String> vTeam = project.getItemValue("namTeam");
		List<String> vOwner = project.getItemValue("namOwner");
		if (vTeam.size() == 0) {
			vTeam.add(getUserName());
			project.replaceItemValue("namTeam", vTeam);
		}
		if (vOwner.size() == 0) {
			vOwner.add(getUserName());
			project.replaceItemValue("namOwner", vOwner);
		}

		// set namFields for access handling (txtProjectReaders
		// txtProjectAuthors)
		Vector<String> vProjectReaders = new Vector<String>();
		Vector<String> vProjectAuthors = new Vector<String>();

		vProjectAuthors.addAll(project.getItemValue("namOwner"));
		vProjectAuthors.addAll(project.getItemValue("namParentOwner"));
		// if now owner is set - namcreator is owner per default
		if (vProjectAuthors.size() == 0)
			vProjectAuthors.add(project.getItemValueString("namCreator"));

		// compute ReadAccess
		vProjectReaders.addAll(project.getItemValue("namteam"));
		vProjectReaders.addAll(project.getItemValue("namParentTeam"));
		vProjectReaders.addAll(project.getItemValue("namOwner"));
		vProjectReaders.addAll(project.getItemValue("namParentOwner"));

		vProjectReaders.addAll(project.getItemValue("namManager"));
		vProjectReaders.addAll(project.getItemValue("namAssist"));
		// test if at lease one reader is defined....
		if (vProjectReaders.size() == 0)
			vProjectReaders.add(project.getItemValueString("namCreator"));

		project.replaceItemValue("namProjectReaders", vProjectReaders);
		project.replaceItemValue("namProjectAuthors", vProjectAuthors);
		project.replaceItemValue("type", "project");

		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int arg0) throws PluginException {
		if (project != null && arg0 != Plugin.PLUGIN_ERROR)
			updateSubProjectInformations();

	}

	/**
	 * This method updates the Project Name ('txtName') which is inherited from
	 * a parentProject aneme. A ParenProject is referenced by the $UniqueIDRef.
	 * 
	 * If the project has a parentProject, then the method adds also the
	 * properties
	 * 
	 * namParentTeam namParentOwner
	 * 
	 */
	private void updateProject() {
		ItemCollection parentProject =null;
		// test if the project has a subproject..
		String sParentProjectID = project.getItemValueString("$uniqueidRef");
		if (!sParentProjectID.isEmpty())
			parentProject = entityService.load(sParentProjectID);
		
		
		if (parentProject != null) {
			logger.fine("Updating Parent Project Informations for '"
					+ sParentProjectID + "'");

			String sName = project.getItemValueString("txtProjectName");
			String sParentName = parentProject.getItemValueString("txtName");

			project.replaceItemValue("txtName", sParentName + "." + sName);
			project.replaceItemValue("txtParentName", sParentName );
		
			project.replaceItemValue("namParentTeam",
					parentProject.getItemValue("namTeam"));
			project.replaceItemValue("namParentOwner",
					parentProject.getItemValue("namOwner"));
		}
		else {
			// root project - update txtName
			project.replaceItemValue("txtName",
					project.getItemValueString("txtProjectName"));

		}
	}

	/**
	 * This method updates the parentName property for all subprojects if the
	 * parent project name has changed
	 * 
	 * This is only necessary if subprojects are found.
	 * 
	 * @param project
	 */
	private void updateSubProjectInformations() {

		logger.fine("Updating Sub Project Informations for '"
				+ project.getItemValueString("$Uniqueid") + "'");

		List<ItemCollection> childProjectList = findAllSubProjects(project
				.getItemValueString("$Uniqueid"));
		String sProjectName = project.getItemValueString("txtName");
		for (ItemCollection aSubProject : childProjectList) {
			if (!sProjectName.equals(aSubProject
					.getItemValueString("txtparentname"))) {
				String sSubProjectName = aSubProject
						.getItemValueString("txtprojectname");
				aSubProject.replaceItemValue("txtparentname", sProjectName);
				aSubProject.replaceItemValue("txtname", sProjectName + "."
						+ sSubProjectName);
				try {
					this.entityService.save(aSubProject);
				} catch (Exception sube) {
					logger.severe("Subproject name could not be updated: "
							+ sube);
				}
			}
		}
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
}
