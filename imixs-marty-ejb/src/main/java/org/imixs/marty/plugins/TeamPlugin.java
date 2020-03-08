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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The Marty TeamPlugin organizes the hierarchical order of a workitem between
 * processes, spaces and workitems. A WorkItem is typically assigned to a
 * process and a optional to one ore more space entities. These references are
 * stored in the $UniqueIDRef property of the WorkItem. In addition to the
 * $UniqueIDRef property the TeamPlugin manages the properties txtProcessRef and
 * txtSpaceRef which containing only uniqueIDs of the corresponding entity type.
 * The properties txtProcessRef and txtSpaceRef can be modified by an
 * application to reassign the workitem.
 * 
 * This plug-in supports also additional workflow properties for further
 * processing. The method computes the team members and the name of the assigned
 * process and space.
 * 
 * <p>
 * The TeamPlugin updates the following properties:
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
 * The name properties are used in security and mail plug-ins.
 * 
 * The properties 'txtProcessRef' and 'txtSpaceRef' are optional and can provide
 * the current $uniqueIDs for referenced space or process entities. The Plug-in
 * updates the $UniqueIDRef property if these properties are filled.
 * 
 * If the workItem is a child to another workItem (ChildWorkitem) the
 * information is fetched from the parent workItem.
 * 
 * If the workflowresultmessage of the ActivityEntity contains a space or
 * process reference the plug-in will update the reference in the property
 * $uniqueIdRef.
 * 
 * Example:
 * 
 * <code>
			<item name="space">...</item>
			<item name="process">...</item>
   </code>
 * 
 * The Plug-in should run before Access-, Application- and Mail-Plug-in.
 * 
 * 
 * Model: default
 * 
 * @author rsoika
 * @version 2.0
 */
public class TeamPlugin extends AbstractPlugin {

	public static final String INVALID_REFERENCE_ASSIGNED_BY_MODEL = "INVALID_REFERENCE_ASSIGNED_BY_MODEL";
	public static final String NO_PROCESS_ASSIGNED = "NO_PROCESS_ASSIGNED";

	private ItemCollection documentContext;
	private static Logger logger = Logger.getLogger(TeamPlugin.class.getName());

	private Map<String, ItemCollection> entityCache = null;

	/**
	 * Fetch workflowService and entityService from WorkflowContext
	 */
	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
		// init cache
		entityCache = new HashMap<String, ItemCollection>();
	}

	/**
	 * The method updates information from the Process and Space entiy (optional)
	 * stored in the attribute '$uniqueIdref'
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
	public ItemCollection run(ItemCollection workItem, ItemCollection documentActivity) throws PluginException {

		documentContext = workItem;

		List<String> oldUnqiueIdRefList = workItem.getItemValue("$UniqueIdRef");
		List<String> newUnqiueIDRefList = null;
		List<String> processRefList = null;
		List<String> spaceRefList = null;

		// 1.1) if txtProcessRef don't exists then search for process ids in
		// $UnqiueIDRef
		if (!workItem.hasItem("txtProcessRef") && !oldUnqiueIdRefList.isEmpty()) {
			processRefList = workItem.getItemValue("txtProcessRef");
			for (String aUniqueID : oldUnqiueIdRefList) {
				ItemCollection entity = findEntity(aUniqueID);
				if (entity != null && "process".equals(entity.getItemValueString("type"))) {
					// update txtProcessRef
					processRefList.add(entity.getItemValueString(WorkflowKernel.UNIQUEID));
				}
			}
			// update txtProcessRef
			workItem.replaceItemValue("txtProcessRef", processRefList);
		} else {
			// 1.1.1) validate content of txtProcessRef
			if (workItem.hasItem("txtProcessRef")) {
				processRefList = workItem.getItemValue("txtProcessRef");
				List<String> verifiedRefList = new ArrayList<String>();
				for (String aUniqueID : processRefList) {
					ItemCollection entity = findEntity(aUniqueID);
					// if the entity was not found by id we test if we can catch
					// it up by its name...
					if (entity == null) {
						entity = findRefByName(aUniqueID, "process");
						if (entity != null) {
							String aID = entity.getItemValueString(WorkflowKernel.UNIQUEID);
							logger.info(
									"[TeamPlugin] processRefName '" + aUniqueID + "' translated into '" + aID + "'");
							// verified
							aUniqueID = aID;
						}
					}

					if (entity != null && "process".equals(entity.getItemValueString("type"))) {
						// verified
						verifiedRefList.add(aUniqueID);
					}
				}
				// update txtProcessRef
				workItem.replaceItemValue("txtProcessRef", verifiedRefList);
			}
		}

		// 1.2) if txtSpaceRef don't exists then search for space ids in
		// $UnqiueIDRef
		if (!workItem.hasItem("txtSpaceRef") && !oldUnqiueIdRefList.isEmpty()) {
			spaceRefList = workItem.getItemValue("txtSpaceRef");
			for (String aUniqueID : oldUnqiueIdRefList) {
				ItemCollection entity = findEntity(aUniqueID);
				if (entity != null && ("space".equals(entity.getType()) || "spacearchive".equals(entity.getType()))) {
					// update txtProcessRef
					spaceRefList.add(entity.getItemValueString(WorkflowKernel.UNIQUEID));
				}
			}
			// update txtProcessRef
			workItem.replaceItemValue("txtSpaceRef", spaceRefList);
		} else {
			// 1.2.1) validate content of txtSpaceRef
			if (workItem.hasItem("txtSpaceRef")) {
				processRefList = workItem.getItemValue("txtSpaceRef");
				List<String> verifiedRefList = new ArrayList<String>();
				for (String aUniqueID : processRefList) {
					ItemCollection entity = findEntity(aUniqueID);
					// if the entity was not found by id we test if we can catch
					// it up by its name...
					if (entity == null) {

						entity = findRefByName(aUniqueID, "space");
						if (entity != null) {
							String aID = entity.getItemValueString(WorkflowKernel.UNIQUEID);
							logger.info("[TeamPlugin] spaceRefName '" + aUniqueID + "' translated into '" + aID + "'");
							// verified
							aUniqueID = aID;
						}
					}

					if (entity != null && ("space".equals(entity.getType()) || "spacearchive".equals(entity.getType()))) {
						// verified
						verifiedRefList.add(aUniqueID);
					}

				}
				// update txtProcessRef
				workItem.replaceItemValue("txtSpaceRef", verifiedRefList);
			}
		}

		/*
		 * 2.) Check the txtActivityResult for a new Project reference.
		 * 
		 * Pattern:
		 * 
		 * '<item name="process">...</item>' '<item name="space">...</item>'
		 */
		ItemCollection evalItemCollection = this.getWorkflowService().evalWorkflowResult(documentActivity, workItem);
		if (evalItemCollection != null) {
			String sRef = fetchRefFromActivity("process", evalItemCollection);
			if (sRef != null && !sRef.isEmpty()) {
				logger.fine("[TeamPlugin] Updating process reference based on model information: " + sRef);
				workItem.replaceItemValue("txtProcessRef", sRef);
			}
			sRef = fetchRefFromActivity("space", evalItemCollection);
			if (sRef != null && !sRef.isEmpty()) {
				logger.fine("[TeamPlugin] Updating space reference based on model information: " + sRef);
				workItem.replaceItemValue("txtSpaceRef", sRef);
			}
		}

		// 3.) now synchronize txtProcessRef/txtSpaceRef with $UnqiueIDref
		processRefList = workItem.getItemValue("txtProcessRef");
		spaceRefList = workItem.getItemValue("txtSpaceRef");
		newUnqiueIDRefList = new ArrayList<String>();
		newUnqiueIDRefList.addAll(processRefList);
		newUnqiueIDRefList.addAll(spaceRefList);

		for (String aUniqueID : oldUnqiueIdRefList) {

			ItemCollection entity = findEntity(aUniqueID);
			// check if this is a deprecated process/space ref
			if (entity != null && "process".equals(entity.getItemValueString("type"))
					&& !processRefList.contains(aUniqueID)) {
				logger.fine("remove deprecated processRef " + aUniqueID);
			} else {
				if (entity != null && ("space".equals(entity.getType()) || "spacearchive".equals(entity.getType()))
						&& !spaceRefList.contains(aUniqueID)) {
					logger.fine("remove deprecated spaceRef " + aUniqueID);
				} else {
					// all other types of entities will still be contained...
					if (!newUnqiueIDRefList.contains(aUniqueID))
						newUnqiueIDRefList.add(aUniqueID);
				}
			}
		}

		// 4.) finally we can now update the $UniqueIDRef property
		workItem.replaceItemValue("$UniqueIdRef", newUnqiueIDRefList);
		logger.fine("Updated $UniqueIdRef: " + newUnqiueIDRefList);

		// and now $UnqiueIDref, txtProcessRef and txtSpaceRef are synchronized
		// and verified!

		// 6.) Now the team lists will be updated depending of the current
		// $uniqueidref
		List vSpaceTeam = new ArrayList<String>();
		List vSpaceManager = new ArrayList<String>();
		List vSpaceAssist = new ArrayList<String>();
		List vProcessTeam = new ArrayList<String>();
		List vProcessManager = new ArrayList<String>();
		List vProcessAssist = new ArrayList<String>();
		List<String> spaceNames = new ArrayList<String>();
        List<String> processNames = new ArrayList<String>();
		//String sSpaceNameNode = "";

		// interate over all refs if defined
		for (String aUnqiueID : newUnqiueIDRefList) {

			ItemCollection entity = findEntity(aUnqiueID);
			if (entity != null) {
				String parentType = entity.getItemValueString("type");

				// Test type property....
				if ("process".equals(parentType)) {
					vProcessTeam.addAll(entity.getItemValue("process.team"));
					vProcessManager.addAll(entity.getItemValue("process.manager"));
					vProcessAssist.addAll(entity.getItemValue("process.assist"));
					processNames.add(entity.getItemValueString("process.name"));

				}
				if ("space".equals(parentType) || "spacearchive".equals(parentType)) {
					vSpaceTeam.addAll(entity.getItemValue("space.team"));
					vSpaceManager.addAll(entity.getItemValue("space.manager"));
					vSpaceAssist.addAll(entity.getItemValue("space.assist"));
					spaceNames.add(entity.getItemValueString("space.name"));
				}

			}
		}

		// update properties
        workItem.replaceItemValue("space.name", spaceNames);
		workItem.replaceItemValue("space.team", vSpaceTeam);
		workItem.replaceItemValue("space.manager", vSpaceManager);
		workItem.replaceItemValue("space.assist", vSpaceAssist);

        workItem.replaceItemValue("process.name", processNames);
        workItem.replaceItemValue("process.team", vProcessTeam);
		workItem.replaceItemValue("process.manager", vProcessManager);
		workItem.replaceItemValue("process.assist", vProcessAssist);

		// removed duplicates...
		uniqueElements(workItem, "$UniqueIdRef");
		uniqueElements(workItem, "txtProcessRef");
		uniqueElements(workItem, "txtSpaceRef");

		uniqueElements(workItem, "space.team");
		uniqueElements(workItem, "space.manager");
		uniqueElements(workItem, "space.assist");
		uniqueElements(workItem, "process.team");
		uniqueElements(workItem, "process.manager");
		uniqueElements(workItem, "process.assist");
		
		// support deprecated item names... Issue #325
        workItem.replaceItemValue("namSpaceTeam", workItem.getItemValue("space.team"));
        workItem.replaceItemValue("namSpaceManager", workItem.getItemValue("space.manager"));
        workItem.replaceItemValue("namSpaceAssist", workItem.getItemValue("space.assist"));
        workItem.replaceItemValue("namProcessTeam", workItem.getItemValue("process.team"));
        workItem.replaceItemValue("namProcessManager", workItem.getItemValue("process.manager"));
        workItem.replaceItemValue("namProcessAssist", workItem.getItemValue("process.assist"));
		workItem.replaceItemValue("txtProcessName", workItem.getItemValue("process.name"));
		workItem.replaceItemValue("txtSpaceName", workItem.getItemValue("space.name"));
		workItem.replaceItemValue("txtSpaceNameNode", workItem.getItemValue("space.name"));
		
		
		

		documentContext.removeItem("space");
		documentContext.removeItem("process");

		return documentContext;
	}

	/**
	 * Helper method to lookup an entity in internal cache or load it from database
	 * 
	 * @param id
	 * @return entity or null if not exits
	 */
	public ItemCollection findEntity(String id) {

		ItemCollection entity = entityCache.get(id);
		if (entity == null) {
			// load entity
			entity = this.getWorkflowService().getDocumentService().load(id);

			if (entity == null) {
				// add a dummy entry....
				entity = new ItemCollection();

			}
			// cache entity
			entityCache.put(id, entity);

		}

		// if entity is dummy return null
		if (entity.getItemValueString(WorkflowKernel.UNIQUEID).isEmpty())
			return null;
		else
			return entity;

	}

	private String fetchRefFromActivity(String type, ItemCollection evalItemCollection) throws PluginException {

		String sRef = null;
		// Read workflow result directly from the activity definition
		String aActivityRefName = evalItemCollection.getItemValueString(type);

		// 1.) check if a new space reference is defined in the current
		// activity. This will overwrite the current value!!
		if (!"".equals(aActivityRefName)) {

			ItemCollection entity = this.getWorkflowService().getDocumentService().load(aActivityRefName);
			if (entity != null && !type.equals(entity.getItemValueString("type"))) {
				entity = null;
			}
			if (entity == null) {
				// load space entity
				entity = findRefByName(aActivityRefName, type);
			}
			if (entity != null) {
				sRef = entity.getItemValueString(WorkflowKernel.UNIQUEID);
				logger.fine("[TeamPlugin] found ref from Activity for: " + aActivityRefName);
			} else {
				// throw a PLuginException
				throw new PluginException(TeamPlugin.class.getSimpleName(), INVALID_REFERENCE_ASSIGNED_BY_MODEL,
						type + " '" + aActivityRefName + "' defined by the current model can not be found!");
			}
		}
		return sRef;
	}

	/**
	 * This method returns a Process or Space entity for a specified name. Returns
	 * null if no entity with the provided name was found
	 * 
	 * Because of the fact that spaces can be ordered in a hirachical order we need
	 * to be a little more tricky if we seach for spaces....
	 */
	private ItemCollection findRefByName(String aName, String type) {

		if (type == null || aName == null) {
			return null;
		}
		//String sQuery = "(type:\"" + type + "\" AND txtname:\"" + aName + "\")";
		String sQuery = "((type:\"" + type + "\" OR type:\"" + type + "archive\") AND txtname:\"" + aName + "\")";
		
		// because of the fact that spaces can be ordered in a hirachical order
		// we need to be a little more tricky if we seach for spaces....
		// Important: to find ambigous space names we search for maxount=2!
		List<ItemCollection> col;
		try {
			col = this.getWorkflowService().getDocumentService().find(sQuery, 2, 0);

			if (col.size() == 0) {
				logger.warning("findRefByName '" + aName + "' not found!");
			} else {
				if (col.size() > 1) {
					logger.warning("findRefByName '" + aName + "' is ambiguous!");
				} else {
					// we found one!
					ItemCollection entity = col.iterator().next();
					// update cache
					entityCache.put(entity.getItemValueString(WorkflowKernel.UNIQUEID), entity);
					return entity;
				}

			}
		} catch (QueryException e) {
			logger.warning("findRefByName - invalid query: " + e.getMessage());
		}

		// no match
		return null;
	}

	/**
	 * This method will remove empty or duplicate values from a list
	 * 
	 * @param target
	 * @param source
	 * @return
	 */
	private void uniqueElements(ItemCollection entity, String field) {
		List<String> target = new ArrayList<String>();
		@SuppressWarnings("unchecked")
		List<String> source = entity.getItemValue(field);

		for (String entry : source) {
			if (entry != null && !entry.isEmpty() && !target.contains(entry))
				target.add(entry);
		}
		entity.replaceItemValue(field, target);
	}

}
