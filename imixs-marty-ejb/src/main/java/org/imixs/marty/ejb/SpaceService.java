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

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The SpaceService provides a business logic to update sub spaces. For this feature the 
 * service runs with Manager access to ensure spaces are in a consistent status 
 * even if the caller has no write access for a subspace.
 * 
 * See issue #290.
 * <p>
 *
 * @author rsoika
 *  
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Singleton
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
public class SpaceService {

	private static Logger logger = Logger.getLogger(SpaceService.class.getName());

	
	@EJB
	DocumentService documentService;
	
	/**
	 * This method updates the parentName and the parent team properties for all
	 * sub-spaces. This is only necessary if sub-spaces are found.
	 * 
	 * @param space
	 */
	public void updateSubSpaces(ItemCollection parentSpace) {
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

			aSubSpace =documentService.save(aSubSpace);
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
	public List<ItemCollection> findAllSubSpaces(String sIDRef, String... types) {
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
			subSpaceList = documentService.find(sQuery, 9999, 0);
		} catch (QueryException e) {
			throw new InvalidAccessException(InvalidAccessException.INVALID_ID, e.getMessage(), e);
		}

		// sort by txtname
		Collections.sort(subSpaceList, new ItemCollectionComparator("txtname", true));
		return subSpaceList;
	}
}
