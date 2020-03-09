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
import java.util.logging.Logger;

import javax.ejb.EJB;

import org.imixs.marty.ejb.SpaceService;
import org.imixs.marty.ejb.TeamCache;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * This system model plug-in supports additional business logic for space and
 * process entities. The case of a 'space' the plug-in updates the properties
 * txtName and txtSpaceName. It also compute the parent team members and the
 * team members of subspaces.
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
    private ItemCollection space = null;

    @EJB
    TeamCache teamCache;

    @EJB
    SpaceService spaceService;

    /**
     * If a 'space' is processed, the method verifies if the space Information need
     * to be updated to the parent and subSpaces.
     * 
     * If a 'spacedeleted' is processed, the method verifies if a deletion is
     * allowed. This is not the case if subspaces exist!
     * 
     **/
    @Override
    public ItemCollection run(ItemCollection documentContext, ItemCollection event) throws PluginException {
        space = null;
        String type = documentContext.getType();

        // verify type 'spacedeleted'
        // in case of a deletion we test if subspaces still exist! In this case
        // deletion is not allowed
        if ("spacedeleted".equals(type)) {
            List<ItemCollection> subspaces = spaceService.findAllSubSpaces(documentContext.getUniqueID(), "space",
                    "spacearchive");
            // if a parentSpace exist - stop deletion!
            if (subspaces != null && subspaces.size() > 0) {
                throw new PluginException(SpacePlugin.class.getName(), SPACE_DELETE_ERROR,
                        "Space object can not be deleted, because descendant space object(s) exist!");
            }
            return documentContext;
        }

        // verify type 'spacearchive'
        // in this case we test if subspaces still exist! In this case
        // archive is not allowed
        if ("spacearchive".equals(type)) {
            List<ItemCollection> subspaces = spaceService.findAllSubSpaces(documentContext.getUniqueID(), "space");
            // if a parentSpace exist - stop deletion!
            if (subspaces != null && subspaces.size() > 0) {
                throw new PluginException(SpacePlugin.class.getName(), SPACE_ARCHIVE_ERROR,
                        "Space object can not be archived, because active descendant space object(s) exist!");
            }
        }

        // verify if the space name and sub-spaces need to be updated...
        if ("space".equals(type) || "spacearchive".equals(type)) {
            space = documentContext;
            inheritParentSpaceProperties();
            // verify txtname if still unique....
            validateUniqueOrgunitName(space, "space");
            spaceService.updateSubSpaces(space);
        }

        // verify if the space name and sub-spaces need to be updated...
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
    private void inheritParentSpaceProperties() throws PluginException {
        ItemCollection parentProject = null;
        // test if the project has a subproject..
        String sParentProjectID = space.getItemValueString("$uniqueidRef");

        if (!sParentProjectID.isEmpty())
            parentProject = getWorkflowService().getDocumentService().load(sParentProjectID);

        if (parentProject != null) {
            if ("space".equals(parentProject.getType())) {
                logger.fine("Updating Parent Project Informations for '" + sParentProjectID + "'");

                String sName = space.getItemValueString("space.name");
                String sParentName = parentProject.getItemValueString("name");

                space.replaceItemValue("name", sParentName + "." + sName);
                space.replaceItemValue("space.parent.name", sParentName);

            } else {
                throw new PluginException(SpacePlugin.class.getName(), SPACE_ARCHIVE_ERROR,
                        "Space object can not be updated, because parent space object is archived!");
            }
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
