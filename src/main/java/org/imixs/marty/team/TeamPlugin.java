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

package org.imixs.marty.team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The Marty TeamPlugin organizes the hierarchical order of a workitem between
 * processes, spaces and workitems. A WorkItem is typically assigned to a
 * process and a optional to one ore more space entities. These references are
 * stored in the $UniqueIDRef property of the WorkItem. In addition to the
 * $UniqueIDRef property the TeamPlugin manages the properties process.ref and
 * space.ref which containing only uniqueIDs of the corresponding entity type.
 * The properties process.ref and space.ref can be modified by an application to
 * reassign the workitem.
 * <p>
 * This plug-in supports also additional workflow properties for further
 * processing. The method computes the team members and the name of the assigned
 * process and space.
 * <p>
 * The TeamPlugin updates the following properties:
 * <ul>
 * <li>space.team
 * <li>space.Manager
 * <li>space.Assist
 * <li>space.Name
 * <li>space.ref
 * <li>process.Team
 * <li>process.Manager
 * <li>process.Assist
 * <li>process.name
 * <li>process.ref
 * </ul>
 * <p>
 * The name properties are used in security and mail plug-ins.
 * <p>
 * The properties 'process.ref' and 'space.ref' are optional and can provide the
 * current $uniqueIDs for referenced space or process entities. The Plug-in
 * updates the $UniqueIDRef property if these properties are filled.
 * <p>
 * If the workItem is a child to another workItem (ChildWorkitem) the
 * information is fetched from the parent workItem.
 * <p>
 * If the workflowresultmessage of the ActivityEntity contains a space or
 * process reference the plug-in will update the reference in the property
 * $uniqueIdRef.
 * <p>
 * Example:
 * 
 * <code>
			<item name="space">...</item>
			<item name="process">...</item>
   </code>
 * 
 * The Plug-in should run before Access-, Application- and Mail-Plug-in.
 * 
 * Note: the deprecated item names are still supported:
 * <ul>
 * <li>namSpaceTeam
 * <li>namSpaceManager
 * <li>namSpaceAssist
 * <li>namSpaceName
 * <li>namProcessTeam
 * <li>namProcessManager
 * <li>namProcessAssist
 * <li>txtSpaceName
 * <li>txtProcessName
 * </ul>
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
     * <li>process.ref
     * <li>space.ref
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
    @SuppressWarnings({ "unchecked" })
    @Override
    public ItemCollection run(ItemCollection workItem, ItemCollection documentActivity) throws PluginException {

        documentContext = workItem;

        List<String> oldUnqiueIdRefList = workItem.getItemValue(WorkflowService.UNIQUEIDREF);
        List<String> newUnqiueIDRefList = null;
        List<String> processRefList = null;
        List<String> spaceRefList = null;

        // 0.) migrate deprecated field names!
        if (!workItem.hasItem("process.ref") && workItem.hasItem("txtprocessref")) {
            workItem.setItemValueUnique("process.ref", workItem.getItemValue("txtprocessref"));
        }
        if (!workItem.hasItem("space.ref") && workItem.hasItem("txtspaceref")) {
            workItem.setItemValueUnique("space.ref", workItem.getItemValue("txtspaceref"));
        }

        // 1.1) if process.ref don't exists then search for process ids in
        // $UnqiueIDRef
        if (!workItem.hasItem("process.ref") && !oldUnqiueIdRefList.isEmpty()) {
            processRefList = workItem.getItemValue("process.ref");
            for (String aUniqueID : oldUnqiueIdRefList) {
                if (aUniqueID == null || aUniqueID.isEmpty()) {
                    continue;
                }
                ItemCollection entity = findEntity(aUniqueID);
                if (entity != null && "process".equals(entity.getItemValueString("type"))) {
                    // update process.ref
                    processRefList.add(entity.getItemValueString(WorkflowKernel.UNIQUEID));
                }
            }
            // update process.ref
            workItem.setItemValueUnique("process.ref", processRefList);
        } else {
            // 1.1.1) validate content of process.ref
            if (workItem.hasItem("process.ref")) {
                processRefList = workItem.getItemValue("process.ref");
                List<String> verifiedRefList = new ArrayList<String>();
                for (String aUniqueID : processRefList) {
                    if (aUniqueID == null || aUniqueID.isEmpty()) {
                        continue;
                    }
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
                // update process.ref
                workItem.setItemValueUnique("process.ref", verifiedRefList);
            }
        }

        // 1.2) if space.ref don't exists then search for space ids in
        // $UnqiueIDRef
        if (!workItem.hasItem("space.ref") && !oldUnqiueIdRefList.isEmpty()) {
            spaceRefList = workItem.getItemValue("space.ref");
            for (String aUniqueID : oldUnqiueIdRefList) {
                ItemCollection entity = findEntity(aUniqueID);
                if (entity != null && ("space".equals(entity.getType()) || "spacearchive".equals(entity.getType()))) {
                    // update space.ref
                    spaceRefList.add(entity.getItemValueString(WorkflowKernel.UNIQUEID));
                }
            }
            // update space.ref
            workItem.setItemValueUnique("space.ref", spaceRefList);
        } else {
            // 1.2.1) validate content of space.ref
            if (workItem.hasItem("space.ref")) {
                processRefList = workItem.getItemValue("space.ref");
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

                    if (entity != null
                            && ("space".equals(entity.getType()) || "spacearchive".equals(entity.getType()))) {
                        // verified
                        verifiedRefList.add(aUniqueID);
                    }

                }
                // update space.ref
                workItem.setItemValueUnique("space.ref", verifiedRefList);
            }
        }

        /*
         * 2.) Check the txtActivityResult for a new Project reference.
         * 
         * Pattern:
         * 
         * '<item name="process">...</item>' '<item name="space">...</item>'
         */
        ItemCollection evalItemCollection = this.getWorkflowService().evalWorkflowResult(documentActivity, "item",
                workItem);
        if (evalItemCollection != null) {
            String sRef = fetchRefFromActivity("process", evalItemCollection);
            if (sRef != null && !sRef.isEmpty()) {
                logger.fine("[TeamPlugin] Updating process reference based on model information: " + sRef);
                workItem.setItemValueUnique("process.ref", sRef);
            }
            sRef = fetchRefFromActivity("space", evalItemCollection);
            if (sRef != null && !sRef.isEmpty()) {
                logger.fine("[TeamPlugin] Updating space reference based on model information: " + sRef);
                workItem.setItemValueUnique("space.ref", sRef);
            }
        }

        // 3.) now synchronize process.ref/space.ref with $UnqiueIDref
        processRefList = workItem.getItemValue("process.ref");
        spaceRefList = workItem.getItemValue("space.ref");
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

        // clean old values
        workItem.removeItem("space.team");
        workItem.removeItem("space.manager");
        workItem.removeItem("space.assist");
        workItem.removeItem("space.team.label");
        workItem.removeItem("space.manager.label");
        workItem.removeItem("space.assist.label");
        workItem.removeItem("process.team");
        workItem.removeItem("process.manager");
        workItem.removeItem("process.assist");
        workItem.removeItem("process.team.label");
        workItem.removeItem("process.manager.label");
        workItem.removeItem("process.assist.label");
        workItem.removeItem("process.name");
        workItem.removeItem("space.name");

        workItem.removeItem(WorkflowService.UNIQUEIDREF);

        // 4.) finally we can now update the $UniqueIDRef property
        workItem.setItemValueUnique(WorkflowService.UNIQUEIDREF, newUnqiueIDRefList);
        logger.fine("Updated $UniqueIdRef: " + newUnqiueIDRefList);

        // 6.) Now the team lists will be updated depending of the current
        // $uniqueidref - iterate over all refs if defined
        for (String aUnqiueID : newUnqiueIDRefList) {

            ItemCollection entity = findEntity(aUnqiueID);
            if (entity != null) {
                String parentType = entity.getItemValueString("type");

                // Test type property....
                if ("process".equals(parentType)) {
                    workItem.appendItemValueUnique("process.assist", entity.getItemValue("process.assist"));
                    workItem.appendItemValueUnique("process.team", entity.getItemValue("process.team"));
                    workItem.appendItemValueUnique("process.manager", entity.getItemValue("process.manager"));

                    workItem.appendItemValueUnique("process.assist.label", entity.getItemValue("process.assist.label"));
                    workItem.appendItemValueUnique("process.team.label", entity.getItemValue("process.team.label"));
                    workItem.appendItemValueUnique("process.manager.label",
                            entity.getItemValue("process.manager.label"));
                    workItem.appendItemValueUnique("process.name", entity.getItemValue("name"));
                }
                if ("space".equals(parentType) || "spacearchive".equals(parentType)) {
                    workItem.appendItemValueUnique("space.assist", entity.getItemValue("space.assist"));
                    workItem.appendItemValueUnique("space.team", entity.getItemValue("space.team"));
                    workItem.appendItemValueUnique("space.manager", entity.getItemValue("space.manager"));

                    workItem.appendItemValueUnique("space.assist.label", entity.getItemValue("space.assist.label"));
                    workItem.appendItemValueUnique("space.team.label", entity.getItemValue("space.team.label"));
                    workItem.appendItemValueUnique("space.manager.label", entity.getItemValue("space.manager.label"));

                    workItem.appendItemValueUnique("space.name", entity.getItemValue("space.name"));
                    workItem.appendItemValueUnique("space.parent.name", entity.getItemValue("space.parent.name"));

                }

            }
        }

        // update properties
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

        workItem.replaceItemValue("txtProcessRef", workItem.getItemValue("process.ref"));
        workItem.replaceItemValue("txtSpaceRef", workItem.getItemValue("space.ref"));

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
        // String sQuery = "(type:\"" + type + "\" AND txtname:\"" + aName + "\")";
        // String sQuery = "((type:\"" + type + "\" OR type:\"" + type + "archive\") AND
        // txtname:\"" + aName + "\")";
        // We need to ensure that the old deprecated item name 'txtname' is still
        // supported by the query.
        String sQuery = "(type:\"" + type + "\" OR type:\"" + type + "archive\") AND (name:\"" + aName
                + "\" OR txtname:\"" + aName + "\")";

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

}
