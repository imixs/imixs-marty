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

package org.imixs.marty.ejb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;

import org.imixs.marty.util.WorkitemHelper;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.engine.DocumentEvent;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The Marty TeamService provides access to the mary process and space entities.
 * 
 * @author rsoika
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
        "org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
        "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class TeamService {

    int DEFAULT_CACHE_SIZE = 30;

    final int MAX_SEARCH_COUNT = 1;

    private static Logger logger = Logger.getLogger(TeamService.class.getName());

    @EJB
    private DocumentService documentService;

    @Resource
    private SessionContext ctx;

    /**
     * PostContruct event
     */
    @PostConstruct
    void init() {

    }

    /**
     * This method returns all process entities for the current user. This list can
     * be used to display process information. The returned process list is
     * optimized and provides additional the following attributes
     * <p>
     * isMember, isTeam, isOwner, isManager, isAssist
     * 
     * @return
     */
    public List<ItemCollection> getProcessList() {
        List<ItemCollection> processList = new ArrayList<ItemCollection>();
        Collection<ItemCollection> col = documentService.getDocumentsByType("process");
        // create optimized list
        for (ItemCollection process : col) {
            ItemCollection clone = cloneOrgItemCollection(process);
            processList.add(clone);
        }
        // sort by txtname
        Collections.sort(processList, new ItemCollectionComparator("txtname", true));

        return processList;
    }

    /**
     * This method returns all space entities for the current user. This list can be
     * used to display space information. The returned space list is optimized and
     * provides additional the following attributes
     * <p>
     * isMember, isTeam, isOwner, isManager, isAssist
     * 
     * @return
     */
    public List<ItemCollection> getSpaces() {
        List<ItemCollection> spaces = new ArrayList<ItemCollection>();
        Collection<ItemCollection> col = documentService.getDocumentsByType("space");

        // create optimized list
        for (ItemCollection space : col) {
            ItemCollection clone = cloneOrgItemCollection(space);
            spaces.add(clone);
        }
        // sort by txtname
        Collections.sort(spaces, new ItemCollectionComparator("txtname", true));
        return spaces;
    }

    /**
     * Returns a space by its name
     * 
     * @param name
     * @return itemCollection of process or null if not process with the specified
     *         id exists
     */
    public ItemCollection getSpaceByName(String name) {
        String query = "type:\"space\" AND txtname:\"" + name + "\"";
        List<ItemCollection> spaces;
        try {
            spaces = documentService.find(query, 1, 0);
        } catch (QueryException e) {
            logger.warning("Failed to lookup space name '" + name + "'!");
            return null;
        }
        if (spaces.size() >= 1) {
            return spaces.get(0);
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
    public ItemCollection getProcessByName(String name) {
        String query = "type:\"process\" AND txtname:\"" + name + "\"";
        List<ItemCollection> spaces;
        try {
            spaces = documentService.find(query, 1, 0);
        } catch (QueryException e) {
            logger.warning("Failed to lookup space name '" + name + "'!");
            return null;
        }
        if (spaces.size() >= 1) {
            return spaces.get(0);
        }
        return null;
    }

    /**
     * Reacts on ON_DOCUMENT_SAVE and updates the space.name | process.name fields
     * 
     * @see DocumentEvent
     * @param documentEvent
     */
    public void onDocumentEvent(@Observes DocumentEvent documentEvent) {

        String type = documentEvent.getDocument().getType();
        if ((type.startsWith("space") || type.startsWith("process"))
                && documentEvent.getEventType() == DocumentEvent.ON_DOCUMENT_SAVE) {
            if (type.startsWith("space")) {
                documentEvent.getDocument().setItemValue("space.name",
                        documentEvent.getDocument().getItemValue("name"));
            }
            if (type.startsWith("process")) {
                documentEvent.getDocument().setItemValue("process.name",
                        documentEvent.getDocument().getItemValue("name"));
            }
        }

    }

    /**
     * This method clones a given process or space ItemCollection. The method also
     * verifies if the current user is manager, teamMember, assist or general
     * membership within this orgunit. THe membership is computed based on the
     * username-list for the current user.
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    private ItemCollection cloneOrgItemCollection(ItemCollection orgunit) {
        ItemCollection clone = WorkitemHelper.clone(orgunit);
        String type= "";
        if (orgunit.getType().startsWith("space")) {
            type="space";
        }
        if (orgunit.getType().startsWith("process")) {
            type="process";
        }

        clone.replaceItemValue("isTeam", false);
        clone.replaceItemValue("isManager", false);
        clone.replaceItemValue("isAssist", false);

        // check the isTeam status for the current user
        List<String> vNameList = orgunit.getItemValue(type + ".team");
        if (documentService.isUserContained(vNameList)) {
            clone.replaceItemValue("isTeam", true);
        }

        // check the isManager status for the current user
        vNameList = orgunit.getItemValue(type + ".manager");
        if (documentService.isUserContained(vNameList)) {
            clone.replaceItemValue("isManager", true);
        }

        // check the isAssist status for the current user
        vNameList = orgunit.getItemValue(type + ".assist");
        if (documentService.isUserContained(vNameList)) {
            clone.replaceItemValue("isAssist", true);
        }

        // check if user is member of team or manager list
        boolean bMember = false;
        if (clone.getItemValueBoolean("isTeam") || clone.getItemValueBoolean("isManager")
                || clone.getItemValueBoolean("isAssist"))
            bMember = true;
        clone.replaceItemValue("isMember", bMember);

        // add custom fields into clone...
        clone.replaceItemValue("txtWorkflowList", orgunit.getItemValue("txtWorkflowList"));
        clone.replaceItemValue("txtReportList", orgunit.getItemValue("txtReportList"));
        clone.replaceItemValue("txtdescription", orgunit.getItemValue("txtdescription"));

        return clone;
    }
}
