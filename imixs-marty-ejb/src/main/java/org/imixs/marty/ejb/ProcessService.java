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
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.imixs.marty.util.WorkitemHelper;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.engine.DocumentService;

/**
 * The Marty ProcessService provides access to the mary process and space
 * entities.
 * 
 * @author rsoika
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class ProcessService {

	int DEFAULT_CACHE_SIZE = 30;

	final int MAX_SEARCH_COUNT = 1;

	private static Logger logger = Logger.getLogger(ProcessService.class
			.getName());

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
	 * This method returns all project entities for the current user. This list
	 * can be used to display project informations inside a form. The returned
	 * project list is optimized and provides additional the following
	 * attributes
	 * <p>
	 * isMember, isTeam, isOwner, isManager, isAssist
	 * 
	 * @return
	 */
	public List<ItemCollection> getProcessList() {

		logger.fine("[ProcessService] getProcessList");
		List<ItemCollection> processList = new ArrayList<ItemCollection>();

//		String sQuery = "SELECT process FROM Entity AS process "
//				+ " JOIN process.textItems AS t2"
//				+ " WHERE process.type = 'process'"
//				+ " AND t2.itemName = 'txtname'" + " ORDER BY t2.itemValue asc";
//		
		
		Collection<ItemCollection> col = documentService.getDocumentsByType("process");

		// create optimized list
		for (ItemCollection process : col) {
			ItemCollection clone =cloneOrgItemCollection(process);
			processList.add(clone);
		}

		// sort by txtname
		Collections.sort(processList, new ItemCollectionComparator("txtname", true));
	
		return processList;
	}

	public List<ItemCollection> getSpaces() {

		logger.fine("[ProcessService] getSpaces");
		List<ItemCollection> spaces = new ArrayList<ItemCollection>();

//		String sQuery = "SELECT space FROM Entity AS space "
//				+ " JOIN space.textItems AS t2" + " WHERE space.type = 'space'"
//				+ " AND t2.itemName = 'txtname'" + " ORDER BY t2.itemValue asc";
//		Collection<ItemCollection> col = documentService.findAllEntities(sQuery,
//				0, -1);
		
		Collection<ItemCollection> col = documentService.getDocumentsByType("process");


		// create optimized list
		for (ItemCollection space : col) {
			ItemCollection clone =cloneOrgItemCollection(space);
			spaces.add(clone);

		}
		// sort by txtname
		Collections.sort(spaces, new ItemCollectionComparator("txtname", true));
		return spaces; 
	}

	
	/**
	 * This method clones a given process or space ItemCollection.
	 * The method also verifiies if the current user is manager, teammember or assit
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ItemCollection cloneOrgItemCollection(ItemCollection orgunit) {
		ItemCollection clone = WorkitemHelper.clone(orgunit);
		clone.replaceItemValue("isTeam", false);
		clone.replaceItemValue("isManager", false);

		// check the isTeam status for the current user
		List<String> userNameList = documentService.getUserNameList();
		Vector<String> vNameList = (Vector<String>) orgunit
				.getItemValue("namTeam");
		// check if one entry matches....
		for (String username : userNameList) {
			if (vNameList.indexOf(username) > -1) {
				clone.replaceItemValue("isTeam", true);
				break;
			}
		}
		// check the isManager status for the current user
		vNameList = (Vector<String>) orgunit.getItemValue("namManager");
		// check if one entry matches....
		for (String username : userNameList) {
			if (vNameList.indexOf(username) > -1) {
				clone.replaceItemValue("isManager", true);
				break;
			}
		}

		// check the isAssist status for the current user
		vNameList = (Vector<String>) orgunit.getItemValue("namAssist");
		// check if one entry matches....
		for (String username : userNameList) {
			if (vNameList.indexOf(username) > -1) {
				clone.replaceItemValue("isAssist", true);
				break;
			}
		}

		// check if user is member of team or manager list
		boolean bMember = false;
		if (clone.getItemValueBoolean("isTeam")
				|| clone.getItemValueBoolean("isManager")
				|| clone.getItemValueBoolean("isAssist"))
			bMember = true;
		clone.replaceItemValue("isMember", bMember);

		// add custom fields into clone...
		clone.replaceItemValue("txtWorkflowList",
				orgunit.getItemValue("txtWorkflowList"));
		clone.replaceItemValue("txtReportList",
				orgunit.getItemValue("txtReportList"));
		clone.replaceItemValue("txtdescription",
				orgunit.getItemValue("txtdescription"));
		clone.replaceItemValue("namTeam", orgunit.getItemValue("namTeam"));
		clone.replaceItemValue("namManager",
				orgunit.getItemValue("namManager"));
		clone.replaceItemValue("namAssist",
				orgunit.getItemValue("namAssist"));

		return clone;
	}
}
