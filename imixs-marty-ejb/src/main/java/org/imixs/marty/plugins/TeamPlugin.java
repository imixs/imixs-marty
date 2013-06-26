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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * The Marty TeamPlugin organizes the hierarchical order of a workitem between
 * processes, spaces and worktiems. A WorkItem is typically assigend to a
 * process and a optional to one ore more space entities. These references are
 * stored in the $UniqueIDRef property of the WorkItem. In addition to the
 * $UniqueIDRef poroperty the TeamPlugin manages the properties txtProcessRef
 * and txtSpaceRef which containing only uniqueIDs of the corresponding entity
 * type. The properties txtProcessRef and txtSpaceRef can be modiefied by an
 * application to reassign the workitem.
 * 
 * This plugin supports also additional workflow properties for further
 * processing. The method computes the team members and the name of the assigned
 * process and space.
 * 
 * <p>
 * The TeamPlugin updates the follwoing properties:
 * <ul>
 * <li>namSpaceTeam
 * <li>namSpaceManager
 * <li>namSpaceAssist
 * <li>namSpaceName
 * <li>txtSpaceRef
 * <li>namProcessTeam
 * <li>namProcessManager
 * <li>namProcessAssist
 * <li>txtProcessName
 * <li>txtProcessRef
 * 
 * The name properties are used in security and mail plugins.
 * 
 * The properties 'txtProcessRef' and 'txtSpaceRef' are optional and can provide
 * the current $uniqueIDs for referenced space or process entities. The Plugin
 * updates the $UniqueIDRef property if these properties are filled.
 * 
 * If the workItem is a child to another workItem (ChildWorkitem) the
 * information is fetched from the parent workItem.
 * 
 * If the workflowresultmessage of the ActivityEntity contains a space or
 * process reference the plugin will update the reference in the property
 * $uniqueIdRef.
 * 
 * Example:
 * 
 * <code>
			<item name="space">...</item>
			<item name="process">...</item>
   </code>
 * 
 * The Plugin should run before Access-, Application- and Mail-Plguin.
 * 
 * 
 * Model: default
 * 
 * @author rsoika
 * @version 2.0
 */
public class TeamPlugin extends AbstractPlugin {

	public static final String INVALID_SPACE_ASSIGNED_BY_MODEL = "INVALID_SPACE_ASSIGNED_BY_MODEL";
	public static final String INVALID_PROCESS_ASSIGNED_BY_MODEL = "INVALID_PROCESS_ASSIGNED_BY_MODEL";

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
	 * (optional) stored in the attribute '$uniqueIdref'
	 * <ul>
	 * <li>txtProcessRef
	 * <li>txtSpaceRef
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

		Map<String, ItemCollection> cacheRef = new HashMap<String, ItemCollection>();

		List<String> newUnqiueIDRefList = new Vector<String>();

		/*
		 * Check the txtActivityResult for a new Project reference.
		 * 
		 * Pattern:
		 * 
		 * '<item name="project">...</item>'
		 */
		String sResult = "";
		// try {
		// Read workflow result directly from the activity definition
		sResult = documentActivity.getItemValueString("txtActivityResult");

		ItemCollection evalItemCollection = new ItemCollection();
		ResultPlugin.evaluate(sResult, evalItemCollection);
		String aActivitySpaceName = evalItemCollection
				.getItemValueString("space");
		String aActivityProcessName = evalItemCollection
				.getItemValueString("process");

		// 1.) check if a new space reference is defined in the current
		// activity. This will overwrite the current value!!
		if (!"".equals(aActivitySpaceName)) {
			logger.fine("[TeamPlugin] Updating Space reference based on model information: "
					+ aActivitySpaceName);
			// load space entity
			ItemCollection parent = findRefByName(aActivitySpaceName, "space");
			if (parent != null) {
				workItem.replaceItemValue("txtSpaceRef",
						parent.getItemValueString(EntityService.UNIQUEID));
				newUnqiueIDRefList.add(parent
						.getItemValueString(EntityService.UNIQUEID));

				cacheRef.put(parent.getItemValueString(EntityService.UNIQUEID),
						parent);
			} else {
				// throw a PLuginException
				throw new PluginException(
						TeamPlugin.class.getSimpleName(),
						INVALID_SPACE_ASSIGNED_BY_MODEL,
						"Space '"
								+ aActivitySpaceName
								+ "' defined by the current model can not be found!");
			}
		} else {
			// 1.1.) verify if the current space assignment is still valid!
			List<String> oldSpaceRefList = workItem.getItemValue("txtSpaceRef");
			List<String> newSpaceRefList = new Vector<String>();
			for (String aUniqueID : oldSpaceRefList) {
				if (!aUniqueID.isEmpty()) {
					ItemCollection entity = entityService.load(aUniqueID);
					if (entity != null) {
						newSpaceRefList.add(aUniqueID);
						newUnqiueIDRefList.add(aUniqueID);
						cacheRef.put(entity
								.getItemValueString(EntityService.UNIQUEID),
								entity);
					} else {
						logger.warning("[TeamPlugin] space ref '" + aUniqueID
								+ "' is no longer valid and will be removed!");
					}
				}
			}
			// update spaceRef
			workItem.replaceItemValue("txtSpaceRef", newSpaceRefList);

		}

		// 2.) check if a new process reference is defined in the current
		// activity. This will overwrite the current value!!
		if (!"".equals(aActivityProcessName)) {
			logger.fine("[TeamPlugin] Updating Process reference based on model information: "
					+ aActivityProcessName);
			// load space entity
			ItemCollection parent = findRefByName(aActivityProcessName,
					"process");
			if (parent != null) {
				workItem.replaceItemValue("txtProcessRef",
						parent.getItemValueString(EntityService.UNIQUEID));
				newUnqiueIDRefList.add(parent
						.getItemValueString(EntityService.UNIQUEID));
				cacheRef.put(parent.getItemValueString(EntityService.UNIQUEID),
						parent);
			} else {
				throw new PluginException(
						TeamPlugin.class.getSimpleName(),
						INVALID_PROCESS_ASSIGNED_BY_MODEL,
						"Process '"
								+ aActivitySpaceName
								+ "' defined by the current model can not be found!");

			}
		} else {
			// 2.1.) verify if the current process assignement is still valid!
			List<String> oldProcessRefList = workItem
					.getItemValue("txtProcessRef");
			List<String> newProcessRefList = new Vector<String>();
			for (String aUniqueID : oldProcessRefList) {
				if (!aUniqueID.isEmpty()) {
					ItemCollection entity = entityService.load(aUniqueID);
					if (entity != null) {
						newProcessRefList.add(aUniqueID);
						newUnqiueIDRefList.add(aUniqueID);
						cacheRef.put(entity
								.getItemValueString(EntityService.UNIQUEID),
								entity);
					} else {
						logger.warning("[TeamPlugin] process ref '" + aUniqueID
								+ "' is no longer valid and will be removed!");
					}
				}
			}
			// update spaceRef
			workItem.replaceItemValue("txtProcessRef", newProcessRefList);

		}

		// 3.) Now if both lists (txtSpaceRef and txtProcessRef) were empty!
		// then we will check the content of $UniqueIDRef and verify if the IDs
		// are still valid! Also the txtSpaceRef and txtProcessRef will be
		// updated
		if (newUnqiueIDRefList.isEmpty()) {
			List<String> oldUnqiueIdRefList = workItem
					.getItemValue("$UniqueIdRef");
			for (String aUniqueID : oldUnqiueIdRefList) {
				if (!aUniqueID.isEmpty()) {
					ItemCollection entity = entityService.load(aUniqueID);
					if (entity != null) {
						// valid reference!
						newUnqiueIDRefList.add(aUniqueID);
						// check type
						String sType = entity.getItemValueString("type");
						// update txtProcessRef
						if ("process".equals(sType)) {
							List<String> old = workItem
									.getItemValue("txtProcessRef");
							old.add(aUniqueID);
							workItem.replaceItemValue("txtProcessRef", old);
						}
						// update txtSpaceRef
						if ("space".equals(sType)) {
							List<String> old = workItem
									.getItemValue("txtSpaceRef");
							old.add(aUniqueID);
							workItem.replaceItemValue("txtSpaceRef", old);
						}

						cacheRef.put(entity
								.getItemValueString(EntityService.UNIQUEID),
								entity);

					} else {
						logger.warning("[TeamPlugin] UniqueIdRef '" + aUniqueID
								+ "' is no longer valid and will be removed!");
					}
				}
			}

		}

		// 4.) finally we can now update the $UniqueIDRef property
		workItem.replaceItemValue("$UniqueIdRef", newUnqiueIDRefList);
		logger.fine("[TeamPlugin] Updated $UniqueIdRef: " + newUnqiueIDRefList);

		// and now $UnqiueIDref, txtProcessRef and txtSpaceRef are synchronized
		// and verified!

		// 5.) update txtSpaceName and txtProcesName
		String sNewSpaceName = null;
		String sNewProcessName = null;
		Collection<ItemCollection> parents = cacheRef.values();
		for (ItemCollection entity : parents) {
			if (sNewSpaceName == null
					&& "space".equals(entity.getItemValueString("type")))
				sNewSpaceName = entity.getItemValueString("txtName");
			if (sNewProcessName == null
					&& "process".equals(entity.getItemValueString("type")))
				sNewProcessName = entity.getItemValueString("txtName");

			if (sNewSpaceName != null && sNewProcessName != null)
				break;

		}
		workItem.replaceItemValue("txtSpaceName", sNewSpaceName);
		workItem.replaceItemValue("txtProcessName", sNewProcessName);
		logger.fine("[TeamPlugin] new ProcessName= " + sNewProcessName);
		logger.fine("[TeamPlugin] new SpaceName= " + sNewSpaceName);

		
		// 6.) Now the team lists will be updated depending of the current
		// $uniqueidref
		List vSpaceTeam = new Vector();
		List vSpaceManager = new Vector();
		List vSpaceAssist = new Vector();
		List vProcessTeam = new Vector();
		List vProcessManager = new Vector();
		List vProcessAssist = new Vector();
		String sSpaceName = "";
		String sProcessName = "";

		parents = cacheRef.values();
		for (ItemCollection itemColProject : parents) {
			// the fetched information depends on the type of the reference!

			String parentType = itemColProject.getItemValueString("type");

			// Test type property....

			if ("process".equals(parentType)) {
				vProcessTeam.addAll(itemColProject.getItemValue("namTeam"));
				vProcessManager.addAll(itemColProject
						.getItemValue("namManager"));
				vProcessAssist.addAll(itemColProject.getItemValue("namAssist"));
				sProcessName = itemColProject.getItemValueString("txtname");
			}
			if ("space".equals(parentType)) {
				vSpaceTeam.addAll(itemColProject.getItemValue("namTeam"));
				vSpaceManager.addAll(itemColProject.getItemValue("namManager"));
				vSpaceAssist.addAll(itemColProject.getItemValue("namAssist"));
				sSpaceName = itemColProject.getItemValueString("txtname");
			}
			if ("workitem".equals(parentType)) {
				vSpaceTeam = itemColProject.getItemValue("namSpaceTeam");
				vSpaceManager = itemColProject.getItemValue("namSpaceManager");
				vSpaceAssist = itemColProject.getItemValue("namSpaceAssist");
				vProcessTeam = itemColProject.getItemValue("namProcessTeam");
				vProcessManager = itemColProject
						.getItemValue("namProcessManager");
				vProcessAssist = itemColProject.getItemValue("namAssist");
				sSpaceName = itemColProject.getItemValueString("txtSpaceName");
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
