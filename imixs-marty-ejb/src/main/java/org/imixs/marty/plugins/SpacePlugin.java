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

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * This system model plug-in supports additional business logic for space
 * entities. The plug-in updates the properties txtName and txtSpaceName. It
 * also compute the parent team members and the team members of subspaces.
 * 
 * Model: system
 * 
 * @author rsoika
 * 
 */
public class SpacePlugin extends AbstractPlugin {

	public static String SPACE_DELETE_ERROR = "SPACE_DELETE_ERROR";

	private static Logger logger = Logger.getLogger(SpacePlugin.class.getName());
	private ItemCollection space = null;

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

		// verify type 'spacedeleted'
		// in case of a deletion we test if parent nodes still exist! In this case
		// deletion is not allowed
		if ("spacedeleted".equals(documentContext.getItemValueString("type"))) {

			List<ItemCollection> subspaces = findAllSubSpaces(documentContext.getUniqueID());
			// if a parentSpace exist - stop deletion!
			if (subspaces != null && subspaces.size() > 0) {
				throw new PluginException(SpacePlugin.class.getName(), SPACE_DELETE_ERROR,
						"Space object can not be deleted, because descendant space object(s) exist!");
			}

		}

		// verify if sub spaces need to be renamed...
		if ("space".equals(documentContext.getItemValueString("type"))) {
			space = documentContext;
			updateParentSpaceProperties();
			updateSubSpaces();
		}

		return documentContext;
	}

	/**
	 * This method updates the Space Name ('txtName') and team lists inherited from
	 * a parent Space. A parent space is referenced by the $UniqueIDRef.
	 * 
	 */
	private void updateParentSpaceProperties() {
		ItemCollection parentProject = null;
		// test if the project has a subproject..
		String sParentProjectID = space.getItemValueString("$uniqueidRef");
		if (!sParentProjectID.isEmpty())
			parentProject = getWorkflowService().getDocumentService().load(sParentProjectID);

		if (parentProject != null && "space".equals(parentProject.getType())) {
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
			// root project - update txtName
			space.replaceItemValue("txtName", space.getItemValueString("txtSpaceName"));

		}
	}

	/**
	 * This method updates the parentName properties for all sub-spaces.
	 * 
	 * This is only necessary if sub-spaces are found.
	 * 
	 * @param space
	 */
	private void updateSubSpaces() {

		logger.fine("Updating Sub Space Informations for '" + space.getItemValueString("$Uniqueid") + "'");

		List<ItemCollection> subSpaceList = findAllSubSpaces(space.getItemValueString("$Uniqueid"));
		String sParentSpaceName = space.getItemValueString("txtName");
		for (ItemCollection aSubSpace : subSpaceList) {

			aSubSpace.replaceItemValue("txtparentname", sParentSpaceName);
			aSubSpace.replaceItemValue("txtname",
					sParentSpaceName + "." + aSubSpace.getItemValueString("txtSpacename"));

			// update parent team lists
			aSubSpace.replaceItemValue("namParentTeam", space.getItemValue("namTeam"));
			aSubSpace.replaceItemValue("namParentManager", space.getItemValue("namManager"));
			aSubSpace.replaceItemValue("namParentAssist", space.getItemValue("namAssist"));

			getWorkflowService().getDocumentService().save(aSubSpace);
		}
	}

	private List<ItemCollection> findAllSubSpaces(String sIDRef) {

		if (sIDRef == null) {
			return null;
		}
		String sQuery = "(type:\"space\" AND $uniqueidref:\"" + sIDRef + "\")";

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
}
