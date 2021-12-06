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

package org.imixs.marty.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * This Plugin updates the userId and password for a user profile. The Update
 * requires the UserGroupService EJB.
 * 
 * The Plugin runs only if the UserGroupService EJB is deployed and the BASIC
 * configuration property 'keyEnableUserDB' is 'true'.
 * 
 * @see UserGroupService
 * @author rsoika
 * @version 1.0
 * 
 */
public class UserGroupPlugin extends AbstractPlugin {
    public static final String INVALID_CONTEXT = "INVALID_CONTEXT";
    static final int EVENT_PROFILE_LOCK = 90;
    static final int TASK_PPROFILE_ACTIVE = 210;

    @EJB
    DocumentService documentService;

    @EJB
    UserGroupService userGroupService = null;;

    int sequenceNumber = -1;
    ItemCollection workitem = null;
    private static Logger logger = Logger.getLogger(UserGroupPlugin.class.getName());

    /**
     * This method updates the user object and the group relation ships
     * 
     * @return
     * @throws PluginException
     */
    @SuppressWarnings("unchecked")
    @Override
    public ItemCollection run(ItemCollection documentContext, ItemCollection documentActivity) throws PluginException {

        // skip if no userGroupService found
        if (userGroupService == null)
            return documentContext;

        workitem = documentContext;

        // check entity type....
        String sType = workitem.getItemValueString("Type");
        if (!("profile".equals(sType)))
            return documentContext;

        // skip if userDB support is not enabled
        if (!isUserDBEnabled()) {
            return documentContext;
        }

        // if processid=210 and activity=20 - delete all groups
        int iProcessID = workitem.getItemValueInteger("$ProcessID");
        int iActivityID = documentActivity.getItemValueInteger("numActivityID");

        // we do not clear roles for 'admin' profile!
        if (!"admin".equalsIgnoreCase(workitem.getItemValueString("txtname"))) {
            if (iProcessID >= TASK_PPROFILE_ACTIVE && iActivityID == EVENT_PROFILE_LOCK) {
                logger.info("Lock profile '" + workitem.getItemValueString("txtname") + "'");
                workitem.replaceItemValue("txtGroups", UserGroupService.ACCESSLEVEL_NOACCESS);
            }
        }

        logger.fine("......update profile '" + workitem.getItemValueString("txtname") + "'....");

        // check if we have deprecated roles
        // issue #373
        List<String> clonedGoupNames = new ArrayList<String>();// clone list
        clonedGoupNames.addAll(workitem.getItemValue("txtGroups"));
        List<String> deprecatedCoreGrouplist = Arrays.asList(UserGroupService.DEPRECATED_CORE_GROUPS);
        for (String aGroup : clonedGoupNames) {
            if (deprecatedCoreGrouplist.contains(aGroup)
                    && !clonedGoupNames.contains(userGroupService.getCoreGroupName(aGroup))) {
                String newGroup = userGroupService.getCoreGroupName(aGroup);
                logger.warning(
                        "...Your Application provides deprecated userroles! This should not happen - check your application!!");
                logger.warning(
                        "..." + workitem.getItemValueString("txtname") + " contains depreacted userrole " + aGroup);
                logger.warning("... Group will be automatically migrated to " + newGroup);
                workitem.appendItemValueUnique("txtGroups", newGroup);
            }
        }
        userGroupService.updateUser(workitem);

        return documentContext;
    }

    /**
     * Returns true if the flag keyEnableUserDB is set to true.
     * 
     * @return
     */
    private boolean isUserDBEnabled() {
        String searchterm = "(type:\"configuration\" AND txtname:\"BASIC\")";
        Collection<ItemCollection> col;
        try {
            col = documentService.find(searchterm, 1, 0);
        } catch (QueryException e) {
            throw new InvalidAccessException(InvalidAccessException.INVALID_ID, e.getMessage(), e);
        }

        if (col.size() > 0) {
            ItemCollection config = col.iterator().next();
            return config.getItemValueBoolean("keyEnableUserDB");
        }

        return false;
    }

}
