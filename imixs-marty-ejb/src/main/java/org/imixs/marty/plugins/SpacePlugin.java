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

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;

import org.imixs.marty.ejb.TeamCache;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
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
	public static String SPACE_NAME_ERROR = "SPACE_NAME_ERROR";

	private static Logger logger = Logger.getLogger(SpacePlugin.class.getName());
	private ItemCollection space = null;

	@EJB
	TeamCache teamCache;

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
			List<ItemCollection> subspaces = findAllSubSpaces(documentContext.getUniqueID(), "space", "spacearchive");
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
			List<ItemCollection> subspaces = findAllSubSpaces(documentContext.getUniqueID(), "space");
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
			validateUniqueSpaceName(space);
			
			updateSubSpaces(space);
		}

		// if a process or a space was processed, then we need to reset the TeamCache
		if (type.startsWith("space") || type.startsWith("process")) {
			logger.finest(".......trigger teamCache reset....");
			teamCache.resetCache();
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

				String sName = space.getItemValueString("txtSpaceName");
				String sParentName = parentProject.getItemValueString("txtName");

				space.replaceItemValue("txtName", sParentName + "." + sName);
				space.replaceItemValue("txtParentName", sParentName);

				// update parent team lists
				space.replaceItemValue("namParentTeam", parentProject.getItemValue("namTeam"));
				space.replaceItemValue("namParentManager", parentProject.getItemValue("namManager"));
				space.replaceItemValue("namParentAssist", parentProject.getItemValue("namAssist"));
			} else {
				throw new PluginException(SpacePlugin.class.getName(), SPACE_ARCHIVE_ERROR,
						"Space object can not be updated, because parent space object is archived!");
			}
		} else {
			// root project - update txtName
			space.replaceItemValue("txtName", space.getItemValueString("txtSpaceName"));

		}
	}

	/**
	 * This method updates the parentName and the parent team properties for all
	 * sub-spaces. This is only necessary if sub-spaces are found.
	 * 
	 * @param space
	 */
	private void updateSubSpaces(ItemCollection parentSpace) {
		logger.finest("......updating sub-space data for '" + parentSpace.getItemValueString("$Uniqueid") + "'");
		List<ItemCollection> subSpaceList = findAllSubSpaces(parentSpace.getItemValueString("$Uniqueid"), "space",
				"spacearchive");
		String sParentSpaceName = parentSpace.getItemValueString("txtName");
		for (ItemCollection aSubSpace : subSpaceList) {

			aSubSpace.replaceItemValue("txtparentname", sParentSpaceName);
			aSubSpace.replaceItemValue("txtname",
					sParentSpaceName + "." + aSubSpace.getItemValueString("txtSpacename"));

			// update parent team lists
			aSubSpace.replaceItemValue("namParentTeam", parentSpace.getItemValue("namTeam"));
			aSubSpace.replaceItemValue("namParentManager", parentSpace.getItemValue("namManager"));
			aSubSpace.replaceItemValue("namParentAssist", parentSpace.getItemValue("namAssist"));

			aSubSpace = getWorkflowService().getDocumentService().save(aSubSpace);
			// call recursive....
			updateSubSpaces(aSubSpace);
		}
	}

	/**
	 * Helper method to find all sub-spaces for a given uniqueID.
	 * 
	 * @param sIDRef
	 * @return
	 */
	private List<ItemCollection> findAllSubSpaces(String sIDRef, String... types) {
		if (sIDRef == null) {
			return null;
		}
		String sQuery = "(";
		// query type...
		if (types != null && types.length > 0) {
			sQuery += "(";
			for (int i = 0; i < types.length; i++) {
				sQuery += " type:\"" + types[i] + "\"";
				if ((i + 1) < types.length) {
					sQuery += " OR ";
				}
			}
			sQuery += ") ";
		}
		sQuery += " AND $uniqueidref:\"" + sIDRef + "\")";

		List<ItemCollection> subSpaceList;
		try {
			subSpaceList = getWorkflowService().getDocumentService().find(sQuery, 9999, 0);
		} catch (QueryException e) {
			throw new InvalidAccessException(InvalidAccessException.INVALID_ID, e.getMessage(), e);
		}

		// sort by txtname
		Collections.sort(subSpaceList, new ItemCollectionComparator("txtname", true));
		return subSpaceList;
	}

	/**
	 * Helper method to find all sub-spaces for a given txtname.
	 * 
	 * @param name     - current space name
	 * @param unqiueid - current uniqueid
	 * 
	 * @throws PluginException if name is already taken
	 */
	private void validateUniqueSpaceName(ItemCollection space) throws PluginException {
		String name=space.getItemValueString("txtname");
		String unqiueid=space.getUniqueID();
		
		String sQuery = "((type:\"space\" OR type:\"spacearchive\") AND txtname:\"" + name + "\")";

		List<ItemCollection> spaceList;
		try {
			spaceList = getWorkflowService().getDocumentService().find(sQuery, 9999, 0);

			for (ItemCollection aspace : spaceList) {
				if (!aspace.getUniqueID().equals(unqiueid)) {
					throw new PluginException(SpacePlugin.class.getName(), SPACE_NAME_ERROR,
							"Space object with this name already exists!");
				}
			}

		} catch (QueryException e) {
			throw new InvalidAccessException(InvalidAccessException.INVALID_ID, e.getMessage(), e);
		}

	}
}
