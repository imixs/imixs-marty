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

import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.QueryException;

import jakarta.inject.Inject;

/**
 * This system model plug-in supports additional business logic for space and
 * process entities. The plugin verifies the uniqueness of a object name.
 * 
 * The case of a 'space' the plug-in updates also the properties
 * 'space.name' and 'space.parent.name'.
 * 
 * In case a space has sub spaces the method also tries to process the subspaces
 * with the same event.
 * 
 * Model: system
 * 
 * @author rsoika
 * 
 */
public class SpacePlugin extends AbstractPlugin {

    public static String SPACE_DELETE_ERROR = "SPACE_DELETE_ERROR";
    public static String SPACE_ARCHIVE_ERROR = "SPACE_ARCHIVE_ERROR";
    public static String ORGUNIT_NAME_ERROR = "ORGUNIT_NAME_ERROR";

    private static Logger logger = Logger.getLogger(SpacePlugin.class.getName());

    @Inject
    SpaceService spaceService;

    @Inject
    WorkflowService workflowService;

    /**
     * If a 'space' is processed, the method verifies if the space Information need
     * to be updated to the parent and subSpaces.
     *
     * The method also tries to update the workflow status of all subspaces
     **/
    @Override
    public ItemCollection run(ItemCollection documentContext, ItemCollection event) throws PluginException {
        String type = documentContext.getType();

        // verify space and sub spaces...
        if (type.startsWith("space")) {

            if (documentContext.getItemValueBoolean("$$ignoreNameUpdate")) {
                documentContext.removeItem("$$ignoreNameUpdate");
            } else {
                inheritParentSpaceProperties(documentContext);
                // spaceService.updateSubSpaces(documentContext);
                // verify name if still unique....
                validateUniqueOrgunitName(documentContext, "space");
            }

            // try to process all sub spaces....
            List<ItemCollection> subSpaces = spaceService.findAllSubSpaces(documentContext.getUniqueID(), type);
            // now we can trigger the same workflow event for all subspaces
            for (ItemCollection subSpace : subSpaces) {
                try {
                    // Update parent name
                    String sParentSpaceName = documentContext.getItemValueString("name");
                    subSpace.replaceItemValue("space.parent.name", sParentSpaceName);
                    subSpace.replaceItemValue("name",
                            sParentSpaceName + "." + subSpace.getItemValueString("space.name"));
                    // deprecated item name
                    subSpace.replaceItemValue("txtparentname", sParentSpaceName);
                    // Process subspace
                    subSpace.event(documentContext.getEventID());
                    subSpace.setItemValue("$$ignoreNameUpdate", true);
                    workflowService.processWorkItem(subSpace);
                } catch (PluginException | AccessDeniedException | ProcessingErrorException | ModelException e) {
                    logger.warning("Unable to process subspace '" +
                            subSpace.getItemValueString("name") + "' : "
                            + e.getMessage());
                }
            }

        }

        // verify unique process name ...
        if ("process".equals(type)) {
            // verify txtname if still unique....
            validateUniqueOrgunitName(documentContext, "process");
        }

        return documentContext;
    }

    /**
     * This method inherits the Space Name ('txtName') and team lists from a parent
     * Space. A parent space is referenced by the $UniqueIDRef.
     * 
     * If the parent is archived or deleted, the method throws a pluginExcepiton
     * 
     * @throws PluginException
     * 
     */
    private void inheritParentSpaceProperties(ItemCollection space) throws PluginException {
        // test if the Space has a parent..
        ItemCollection parentSpace = spaceService.loadParentSpace(space);
        if (parentSpace != null) {
            logger.fine("Updating Parent Space Information for '" + space.getUniqueID() + "'");
            String sParentName = parentSpace.getItemValueString("name");

            space.replaceItemValue("name", sParentName + "." + space.getItemValueString("space.name"));
            space.replaceItemValue("space.parent.name", sParentName);
        } else {
            // root space - update name
            space.replaceItemValue("name", space.getItemValueString("space.name"));
        }

    }

    /**
     * This method validates the uniqueness of the item txtname of an orgunit.
     * 
     * @param orgunit
     * @param type    - space or process
     * 
     * @throws PluginException if name is already taken
     */
    private void validateUniqueOrgunitName(ItemCollection orgunit, String type) throws PluginException {
        String name = orgunit.getItemValueString("name");
        String unqiueid = orgunit.getUniqueID();

        // support deprecated item name 'txtname
        String sQuery = "((type:\"" + type + "\" OR type:\"" + type + "archive\") AND (txtname:\"" + name
                + "\" OR name:\"" + name + "\"))";

        List<ItemCollection> spaceList;
        try {
            spaceList = getWorkflowService().getDocumentService().find(sQuery, 9999, 0);

            for (ItemCollection aspace : spaceList) {
                if (!aspace.getUniqueID().equals(unqiueid)) {
                    throw new PluginException(SpacePlugin.class.getName(), ORGUNIT_NAME_ERROR,
                            type + " with this name already exists!");
                }
            }

        } catch (QueryException e) {
            throw new InvalidAccessException(InvalidAccessException.INVALID_ID, e.getMessage(), e);
        }

    }
}
