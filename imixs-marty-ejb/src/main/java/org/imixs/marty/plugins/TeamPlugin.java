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
 * The method computes the team members and additional information of the
 * assigned core process and project from a workItem. The references to a core
 * process and a project are stored in the attribute '$uniqueIdref'.
 * 
 * <p>
 * The plugin updates the corresponding CoreProcess and Project informations:
 * <ul>
 * <li>namProjectTeam
 * <li>namProjectManager
 * <li>namProjectName
 * <li>namCoreProcessManager
 * <li>txtProjectname
 * <li>txtCoreProcessName
 * 
 * The name properties are used in security and mail plugins.
 * 
 * If the workitem is a child to another workitem (ChildWorkitem) the
 * information is fetched from the parent workitem.
 * 
 * If the workflowresultmessage of the ActivityEntity contains a project
 * reference the plugin will update the reference in the property $uniqueIdRef.
 * 
 * Example:
 * 
 * <code>
			<item name="project">...</item>
   </code>
 * 
 * The Plugin should run before Access-, Application- and Mail-Plguin.
 * 
 * @author rsoika
 * @version 2.0
 */
public class TeamPlugin extends AbstractPlugin {

	private WorkflowService workflowService = null;
	private EntityService entityService = null;
	private static Logger logger = Logger.getLogger(TeamPlugin.class.getName());

	/**
	 * Fetch workflowService and entityService from WorkflowContext
	 */
	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
		// check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			workflowService = (WorkflowService) actx;
			entityService = workflowService.getEntityService();
		}
	}

	/**
	 * The method updates information from the Process and Space entiy
	 * (optional) stored in the attribute '$uniqueIdref':
	 * <ul>
	 * <li>namTeam
	 * <li>namManager
	 * <li>namProcessTeam
	 * <li>namProcessManager
	 * <li>txtSpaceName
	 * <li>txtCoreProcessName
	 * 
	 * If the workitem is a child to another workitem (ChildWorkitem) the
	 * information is fetched from the parent workitem.
	 * 
	 * If the workflowresultmessage contains a space entity reference the plugin
	 * will update the reference in the property $uniqueIdRef.
	 * 
	 * Example:
	 * 
	 * <code>
			<item name="space">...</item>
	   </code>
	 * 
	 **/
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public int run(ItemCollection workItem, ItemCollection documentActivity)
			throws PluginException {

		/*
		 * Check the txtActivityResult for a new Project reference.
		 * 
		 * Pattern:
		 * 
		 * '<item name="project">...</item>'
		 */
		String sResult = "";
		try {
			// Read workflow result directly from the activity definition
			sResult = documentActivity.getItemValueString("txtActivityResult");

			ItemCollection evalItemCollection = new ItemCollection();
			ResultPlugin.evaluate(sResult, evalItemCollection);
			String aSpaceName = evalItemCollection.getItemValueString("space");
			String aProcessName = evalItemCollection
					.getItemValueString("process");

			if (!"".equals(aSpaceName)) {
				logger.fine("[TeamPlugin] Updating Space reference: "
						+ aSpaceName);
				// load project reference
				ItemCollection parent = findRefByName(aSpaceName, "space");
				if (parent != null) {
					// first remove all older project references
					List<String> refList = workItem
							.getItemValue("$uniqueidRef");
					for (String aUniqueID : refList) {
						// test ref to type 'project'
						ItemCollection entity = entityService.load(aUniqueID);
						if (entity != null
								&& "space".equals(entity
										.getItemValueString("type"))) {
							refList.remove(aUniqueID);
						}
					}
					// add new ref
					refList.add(parent.getItemValueString("$uniqueid"));

					// assign project name and reference
					workItem.replaceItemValue("$uniqueidRef", refList);
					workItem.replaceItemValue("txtSpaceName",
							parent.getItemValueString("txtname"));

					logger.fine("[TeamPlugin] new $uniqueidRef= " + refList);
				}
			}

		} catch (Exception upr) {
			logger.warning("[TeamPlugin] WARNING - Unable to update Project ("
					+ sResult + ") reference: " + upr);
		}

		/*
		 * Now the team lists will be updated depending of the current
		 * $uniqueidref
		 */
		List vSpaceTeam = new Vector();
		List vSpaceManager = new Vector();
		List vSpaceAssist = new Vector();
		List vProcessTeam = new Vector();
		List vProcessManager = new Vector();
		List vProcessAssist = new Vector();
		List<String> parentRefs = workItem.getItemValue("$uniqueidref");
		String sSpaceName = "";
		String sProcessName = "";
		for (String sParentID : parentRefs) {

			// the fetched information depends on the type of the reference!
			ItemCollection itemColProject = entityService.load(sParentID);
			if (itemColProject != null) {

				String parentType = itemColProject.getItemValueString("type");

				// Test type property....

				if ("process".equals(parentType)) {
					vProcessTeam.addAll(itemColProject.getItemValue("namTeam"));
					vProcessManager.addAll(itemColProject
							.getItemValue("namManager"));
					vProcessAssist.addAll(itemColProject
							.getItemValue("namAssist"));
					sProcessName = itemColProject.getItemValueString("txtname");
				}
				if ("space".equals(parentType)) {
					vSpaceTeam.addAll(itemColProject.getItemValue("namTeam"));
					vSpaceManager.addAll(itemColProject
							.getItemValue("namManager"));
					vSpaceAssist.addAll(itemColProject
							.getItemValue("namAssist"));
					sSpaceName = itemColProject.getItemValueString("txtname");
				}
				if ("workitem".equals(parentType)) {
					vSpaceTeam = itemColProject.getItemValue("namSpaceTeam");
					vSpaceManager = itemColProject
							.getItemValue("namSpaceManager");
					vSpaceAssist = itemColProject
							.getItemValue("namSpaceAssist");
					vProcessTeam = itemColProject
							.getItemValue("namProcessTeam");
					vProcessManager = itemColProject
							.getItemValue("namProcessManager");
					vProcessAssist = itemColProject
							.getItemValue("namAssist");
					sSpaceName = itemColProject
							.getItemValueString("txtSpaceName");
					sProcessName = itemColProject
							.getItemValueString("txtProcessname");
				}
			}

			// update properties
			workItem.replaceItemValue("namSpaceTeam", vSpaceTeam);
			workItem.replaceItemValue("namSpaceManager", vSpaceManager);
			workItem.replaceItemValue("namSpaceAssist", vSpaceManager);
			workItem.replaceItemValue("namProcessTeam", vProcessTeam);
			workItem.replaceItemValue("namProcessManager", vProcessManager);
			workItem.replaceItemValue("namProcessAssist", vProcessManager);
			workItem.replaceItemValue("txtSpaceName", sSpaceName);
			workItem.replaceItemValue("txtProcessName", sProcessName);

		}
		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int arg0) throws PluginException {
		// no op

	}

	/**
	 * This method returns a project ItemCollection for a specified name.
	 * Returns null if no project with the provided name was found
	 * 
	 */
	public ItemCollection findRefByName(String aName, String type) {
		String sQuery = "SELECT project FROM Entity AS project "
				+ " JOIN project.textItems AS t2" + " WHERE  project.type = '"
				+ type + "' " + " AND t2.itemName = 'txtname' "
				+ " AND t2.itemValue = '" + aName + "'";

		List<ItemCollection> col = entityService.findAllEntities(sQuery, 0, 1);
		if (col.size() > 0)
			return col.iterator().next();
		else
			return null;
	}
}
