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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.imixs.marty.profile.ProfileService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.faces.data.WorkflowEvent;
import org.imixs.workflow.faces.util.LoginController;
import org.imixs.workflow.faces.util.ResourceBundleHandler;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
@Named
@SessionScoped
public class TeamController implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ItemCollection> spaces = null;
    private List<ItemCollection> processList = null;
    private ItemCollection process = null;

    // Cache for spaces by reference
    private Map<String, List<ItemCollection>> spacesByRefCache = null;
    // Cache for root spaces
    private List<ItemCollection> rootSpacesCache = null;
    // Cache for spaces by process
    private Map<String, List<ItemCollection>> spacesByProcessCache = null;

    @Inject
    protected LoginController loginController = null;

    @Inject
    protected ResourceBundleHandler resourceBundleHandler = null;

    @EJB
    protected DocumentService documentService;

    @EJB
    protected TeamService teamService;

    @EJB
    protected ProfileService profileService;

    private static Logger logger = Logger.getLogger(TeamController.class.getName());

    /**
     * Reset the internal cache
     */
    @PostConstruct
    public void reset() {
        spaces = null;
        processList = null;
        process = null;
        spacesByRefCache = null;
        rootSpacesCache = null;
        spacesByProcessCache = null;
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
     * Loads a process entity by its UniqueID from the internal cache and updates
     * the current process entity.
     * 
     * @param uniqueid - of process entity
     * 
     * @return current process entity
     */
    public ItemCollection loadProcess(String uniqueid) {
        if (this.process == null || !this.process.getItemValue(WorkflowKernel.UNIQUEID).equals(uniqueid)) {
            setProcess(this.getProcessById(uniqueid));
        }
        return getProcess();
    }

    /**
     * Returns the process for a given uniqueID. The method uses the internal cache.
     * 
     * @param uniqueId
     * @return itemCollection of process or null if not process with the specified
     *         id exists
     */
    public ItemCollection getProcessById(String uniqueId) {

        if (uniqueId != null && !uniqueId.isEmpty()) {
            // iterate over all spaces and compare the $UniqueIDRef
            List<ItemCollection> list = getProcessList();
            for (ItemCollection process : list) {
                if (uniqueId.equals(process.getItemValueString(WorkflowKernel.UNIQUEID))) {
                    return process;
                }
            }
        }
        return null;
    }

    /**
     * This method returns a chached list of process entities for the current user.
     * This list can be used to display processs information. The returned process
     * list is optimized and provides additional the following attributes
     * <p>
     * isMember, isTeam, isOwner, isManager, isAssist
     * 
     * @return
     */
    public List<ItemCollection> getProcessList() {
        if (processList == null) {
            processList = teamService.getProcessList();
        }
        return processList;
    }

    /**
     * This method returns a cached list of spaces for the current user. This list
     * can be used to display space information. The returned space list is
     * optimized and provides additional the following attributes
     * <p>
     * isMember, isTeam, isOwner, isManager, isAssist
     * 
     * @return
     */
    public List<ItemCollection> getSpaces() {
        if (spaces == null) {
            spaces = teamService.getSpaces();
        }
        return spaces;
    }

    /**
     * This method returns a space or process entity by its UniqueID. The space and
     * process entities are read from the internal cache.
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
            if (uniqueid.equals(aProcess.getUniqueID()))
                return aProcess;
        }

        // get the space list form local cache
        alist = getSpaces();
        for (ItemCollection aSpace : alist) {
            if (uniqueid.equals(aSpace.getUniqueID()))
                return aSpace;
        }
        return null;

    }

    /**
     * Returns a Space for a given uniqueID.
     * 
     * @param uniqueId
     * @return itemCollection of process or null if not process with the specified
     *         id exists
     */
    public ItemCollection getSpaceById(String uniqueId) {

        if (uniqueId != null && !uniqueId.isEmpty()) {
            // iterate over all spaces and compare the $UniqueIDRef
            List<ItemCollection> list = getSpaces();
            for (ItemCollection space : list) {
                if (uniqueId.equals(space.getItemValueString(WorkflowKernel.UNIQUEID))) {
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
     * @return itemCollection of process or null if not process with the specified
     *         id exists
     */
    public ItemCollection getProcessByName(String name) {

        if (name != null && !name.isEmpty()) {
            // iterate over all processes and compare the txtname
            List<ItemCollection> list = getProcessList();
            for (ItemCollection process : list) {
                if (name.equals(process.getItemValueString("name"))
                        || name.equals(process.getItemValueString("txtName"))) {
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
     * @return itemCollection of process or null if not process with the specified
     *         id exists
     */
    public ItemCollection getSpaceByName(String name) {

        if (name != null && !name.isEmpty()) {
            // iterate over all processes and compare the txtname
            List<ItemCollection> list = getSpaces();
            for (ItemCollection space : list) {
                if (name.equals(space.getItemValueString("name")) || name.equals(space.getItemValueString("txtName"))) {
                    return space;
                }
            }
        }
        return null;
    }

    /**
     * Returns a list of all spaces which are assigned to a given process entity.
     * 
     * @param uniqueId of a processEntity
     * @return
     */
    public List<ItemCollection> getSpacesByProcessId(String uniqueId) {
        if (uniqueId == null || uniqueId.isEmpty()) {
            return new ArrayList<ItemCollection>();
        }

        // Initialize cache if null
        if (spacesByProcessCache == null) {
            spacesByProcessCache = new HashMap<String, List<ItemCollection>>();
        }

        // Check if result is already cached
        if (spacesByProcessCache.containsKey(uniqueId)) {
            return spacesByProcessCache.get(uniqueId);
        }

        // find project
        ItemCollection process = getProcessById(uniqueId);
        List<ItemCollection> result = getSpacesByProcess(process);

        // Cache the result
        spacesByProcessCache.put(uniqueId, result);

        return result;
    }

    /**
     * Returns a list of all spaces which are assigned to a given process entity.
     * 
     * @param uniqueId of a processEntity
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<ItemCollection> getSpacesByProcess(ItemCollection process) {
        List<ItemCollection> result = new ArrayList<ItemCollection>();
        if (process != null) {
            String processId = process.getItemValueString(WorkflowKernel.UNIQUEID);

            // Initialize cache if null
            if (spacesByProcessCache == null) {
                spacesByProcessCache = new HashMap<String, List<ItemCollection>>();
            }

            // Check if result is already cached
            if (!processId.isEmpty() && spacesByProcessCache.containsKey(processId)) {
                return spacesByProcessCache.get(processId);
            }

            List<String> refs = process.getItemValue(WorkflowService.UNIQUEIDREF);
            if (refs != null && !refs.isEmpty()) {
                // iterate over all spaces and compare the $UniqueIDRef
                List<ItemCollection> list = getSpaces();
                for (ItemCollection space : list) {
                    if (refs.contains(space.getItemValueString(WorkflowKernel.UNIQUEID))) {
                        result.add(space);
                    }
                }
            }

            // Cache the result if we have a valid process ID
            if (!processId.isEmpty()) {
                spacesByProcessCache.put(processId, result);
            }
        }
        return result;
    }

    /**
     * Returns a list of all spaces which are siblings to a given UniqueID.
     * Uses a cache to improve performance for repeated calls.
     * 
     * @param uniqueId
     * @return
     */
    public List<ItemCollection> getSpacesByRef(String uniqueId) {
        if (uniqueId == null || uniqueId.isEmpty()) {
            return new ArrayList<ItemCollection>();
        }

        // Initialize cache if null
        if (spacesByRefCache == null) {
            spacesByRefCache = new HashMap<String, List<ItemCollection>>();
        }

        // Check if result is already cached
        if (spacesByRefCache.containsKey(uniqueId)) {
            return spacesByRefCache.get(uniqueId);
        }

        List<ItemCollection> result = new ArrayList<ItemCollection>();

        // iterate over all spaces and compare the $UniqueIDRef
        List<ItemCollection> list = getSpaces();
        for (ItemCollection space : list) {
            logger.fine("getSpacesByRef Spacename= " + space.getItemValueString("Name") + " uniquidref= "
                    + space.getItemValueString(WorkflowService.UNIQUEIDREF));
            if (uniqueId.equals(space.getItemValueString(WorkflowService.UNIQUEIDREF))) {
                result.add(space);
            }
        }

        // Cache the result
        spacesByRefCache.put(uniqueId, result);

        return result;
    }

    /**
     * Returns a list of all spaces on the root level.
     * Uses a cache to improve performance for repeated calls.
     * 
     * @return
     */
    public List<ItemCollection> getRootSpaces() {
        // Check if result is already cached
        if (rootSpacesCache != null) {
            return rootSpacesCache;
        }

        List<ItemCollection> result = new ArrayList<ItemCollection>();

        // iterate over all spaces and select those without a $UniqueIDRef
        List<ItemCollection> list = getSpaces();
        for (ItemCollection space : list) {
            logger.fine("getRootSpaces Spacename= " + space.getItemValueString("txtName") + " uniquidref= "
                    + space.getItemValueString(WorkflowService.UNIQUEIDREF));
            if (space.getItemValueString(WorkflowService.UNIQUEIDREF).isEmpty()) {
                result.add(space);
            }
        }

        // Cache the result
        rootSpacesCache = result;

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
        List<ItemCollection> resultList = getMemberListByRole(aUniqueID, "manager");

        // sort by username..
        Collections.sort(resultList, new ItemCollectionComparator("txtUserName", true));

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
        List<ItemCollection> resultList = getMemberListByRole(aUniqueID, "team");

        // sort by username..
        Collections.sort(resultList, new ItemCollectionComparator("txtUserName", true));

        return resultList;
    }

    /**
     * Returns a unique sorted list of assist members for the current project. The
     * returned list contains cloned user profile entities.
     * 
     *
     * @return list of profile entities for the current team members
     */
    public List<ItemCollection> getAssist(String aUniqueID) {
        List<ItemCollection> resultList = getMemberListByRole(aUniqueID, "assist");

        // sort by username..
        Collections.sort(resultList, new ItemCollectionComparator("txtUserName", true));

        return resultList;
    }

    /**
     * Returns a unique sorted list of all members (Managers, Team, Assist) for the
     * current process. The returned list contains cloned user profile entities.
     * 
     *
     * @return list of profile entities for the current team members
     */
    public List<ItemCollection> getProcessMembers(String aUniqueID) {
        List<ItemCollection> resultList = new ArrayList<ItemCollection>();
        List<String> dupplicatedIds = new ArrayList<String>();

        List<ItemCollection> assistList = getMemberListByRole(aUniqueID, "assist");
        List<ItemCollection> teamList = getMemberListByRole(aUniqueID, "team");
        List<ItemCollection> managerList = getMemberListByRole(aUniqueID, "manager");

        for (ItemCollection profile : teamList) {
            // avoid duplicates..
            if (!dupplicatedIds.contains(profile.getItemValueString(WorkflowKernel.UNIQUEID))) {
                resultList.add(profile);
            }
            dupplicatedIds.add(profile.getItemValueString(WorkflowKernel.UNIQUEID));
        }
        for (ItemCollection profile : managerList) {
            // avoid duplicates..
            if (!dupplicatedIds.contains(profile.getItemValueString(WorkflowKernel.UNIQUEID))) {
                resultList.add(profile);
            }
            dupplicatedIds.add(profile.getItemValueString(WorkflowKernel.UNIQUEID));
        }
        for (ItemCollection profile : assistList) {
            // avoid duplicates..
            if (!dupplicatedIds.contains(profile.getItemValueString(WorkflowKernel.UNIQUEID))) {
                resultList.add(profile);
            }
            dupplicatedIds.add(profile.getItemValueString(WorkflowKernel.UNIQUEID));
        }

        // sort by username..
        Collections.sort(resultList, new ItemCollectionComparator("txtUserName", true));

        return resultList;
    }

    /**
     * Returns true if current user is manager of a given orgunit. Therefore the
     * method checks the cloned field 'isManager' computed by the ProcessService
     * 
     * @return
     */
    public boolean isManagerOf(String aUniqueID) {
        // find orgunit....
        ItemCollection entity = getEntityById(aUniqueID);
        if (entity != null) {
            return entity.getItemValueBoolean("isManager");
        } else {
            return false;
        }
    }

    /**
     * Returns true if current user is team member of a given orgunit. Therefore the
     * method checks the cloned field 'isTeam' computed by the ProcessService
     * 
     * @return
     */
    public boolean isTeamMemberOf(String aUniqueID) {
        // find orgunit...
        ItemCollection entity = getEntityById(aUniqueID);
        if (entity != null) {
            return entity.getItemValueBoolean("isTeam");
        } else {
            return false;
        }
    }

    /**
     * Returns true if current user is assist of a given orgunit. Therefore the
     * method checks the cloned field 'isTeam' computed by the ProcessService
     * 
     * @return
     */
    public boolean isAssistOf(String aUniqueID) {
        // find orgunit...
        ItemCollection entity = getEntityById(aUniqueID);
        if (entity != null) {
            return entity.getItemValueBoolean("isAssist");
        } else {
            return false;
        }
    }

    /**
     * Returns true if current user is teamMember, manager or assist of a given
     * orgunit. Therefore the method checks the cloned field 'isMember' computed by
     * the ProcessService
     * 
     * @return
     */
    public boolean isMemberOf(String aUniqueID) {
        // find orgunit..
        ItemCollection entity = getEntityById(aUniqueID);
        if (entity != null) {
            return entity.getItemValueBoolean("isMember");
        } else {
            return false;
        }
    }

    /**
     * WorkflowEvent listener
     * 
     * If a process or space was created/loaded/processed the modellController will
     * be reseted.
     * 
     * 
     * @param workflowEvent
     */
    public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent) {
        if (workflowEvent == null || workflowEvent.getWorkitem() == null) {
            return;
        }

        String type = workflowEvent.getWorkitem().getItemValueString("type");

        // WORKITEM
        if (type.startsWith("workitem")) {
            if (WorkflowEvent.WORKITEM_CREATED == workflowEvent.getEventType()) {
                String processRef = workflowEvent.getWorkitem().getItemValueString(WorkflowService.UNIQUEIDREF);
                ItemCollection process = getProcessById(processRef);
                if (process != null) {
                    workflowEvent.getWorkitem().replaceItemValue("process.Name", process.getItemValueString("name"));
                    workflowEvent.getWorkitem().replaceItemValue("process.Ref",
                            process.getItemValueString(WorkflowKernel.UNIQUEID));
                    // support deprecated fields
                    workflowEvent.getWorkitem().replaceItemValue("txtProcessName", process.getItemValueString("name"));
                    workflowEvent.getWorkitem().replaceItemValue("txtProcessRef",
                            process.getItemValueString(WorkflowKernel.UNIQUEID));
                } else {
                    logger.fine("...unable to find process entity '" + processRef + "'!");
                }
            }
        }

        // PROCESS || SPACE
        if (type.startsWith("space") || type.startsWith("process")) {
            // set default labels for team/manager/assist if empty
            if (WorkflowEvent.WORKITEM_CREATED == workflowEvent.getEventType()
                    || WorkflowEvent.WORKITEM_CHANGED == workflowEvent.getEventType()) {

                if (type.startsWith("space")) {
                    if (!workflowEvent.getWorkitem().hasItem("space.manager.label")) {
                        workflowEvent.getWorkitem().setItemValue("space.manager.label",
                                resourceBundleHandler.findMessage("space.manager"));
                    }

                    if (!workflowEvent.getWorkitem().hasItem("space.team.label")) {
                        workflowEvent.getWorkitem().setItemValue("space.team.label",
                                resourceBundleHandler.findMessage("space.team"));
                    }
                    if (!workflowEvent.getWorkitem().hasItem("space.assist.label")) {
                        workflowEvent.getWorkitem().setItemValue("space.assist.label",
                                resourceBundleHandler.findMessage("space.assist"));
                    }
                }
                if (type.startsWith("process")) {
                    if (!workflowEvent.getWorkitem().hasItem("process.manager.label")) {
                        workflowEvent.getWorkitem().setItemValue("process.manager.label",
                                resourceBundleHandler.findMessage("process.manager"));
                    }

                    if (!workflowEvent.getWorkitem().hasItem("process.team.label")) {
                        workflowEvent.getWorkitem().setItemValue("process.team.label",
                                resourceBundleHandler.findMessage("process.team"));
                    }
                    if (!workflowEvent.getWorkitem().hasItem("process.assist.label")) {
                        workflowEvent.getWorkitem().setItemValue("process.assist.label",
                                resourceBundleHandler.findMessage("process.assist"));
                    }
                }
            }

            // reset cache
            if (WorkflowEvent.WORKITEM_AFTER_PROCESS == workflowEvent.getEventType()) {
                reset();
                logger.fine("ModelController:WorkflowEvent=" + workflowEvent.getEventType());
            }

        }

    }

    /**
     * Returns a unique sorted list of profile itemCollections for a team list in a
     * project. The returned list contains cloned user profile entities.
     * 
     * @param listType - the member field of the project (namTeam, namManager,
     *                 namAssist)
     * @return list of team profiles
     */
    @SuppressWarnings("unchecked")
    private List<ItemCollection> getMemberListByRole(String aUniqueID, String role) {
        List<ItemCollection> resultList = new ArrayList<ItemCollection>();
        List<String> dupplicatedIds = new ArrayList<String>();

        // find Process/Space entity
        ItemCollection orgUnit = getEntityById(aUniqueID);
        if (orgUnit == null)
            return resultList;

        String type = orgUnit.getType();
        List<String> members = orgUnit.getItemValue(type + "." + role);
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

    /**
     * This method resolves the field 'process.default.ref'.
     * This fields holds the first space ID the current user is member of.
     * 
     */
    public String resolveDefaultProcessRef(ItemCollection workitem) {
        // iterate over all processes
        List<ItemCollection> _processList = getProcessList();
        if (_processList != null) {
            for (ItemCollection _process : _processList) {
                if (_process.getItemValueBoolean("isMember")) {
                    return _process.getUniqueID();
                }
            }
        }
        // no match !
        return null;

    }
}