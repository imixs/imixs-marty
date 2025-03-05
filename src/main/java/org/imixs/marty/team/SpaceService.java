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

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.QueryException;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;

/**
 * The SpaceService provides a business logic to update sub spaces. For this
 * feature the
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
	 * Load the parent space for a given space.
	 * If the space is a root space, the method returns null
	 * 
	 * @param space
	 * @return parent space or null if the given space is a root space
	 */
	public ItemCollection loadParentSpace(ItemCollection space) {
		String ref = space.getItemValueString(WorkflowService.UNIQUEIDREF);
		if (!ref.isEmpty()) {
			// lookup parent...
			return documentService.load(ref);
		}
		return null;
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
