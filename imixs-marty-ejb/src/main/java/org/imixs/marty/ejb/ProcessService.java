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
	private org.imixs.workflow.jee.ejb.EntityService entityService;

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
	@SuppressWarnings("unchecked")
	public List<ItemCollection> getProcessList() {

		logger.fine("[ProcessService] getProcessList");
		List<ItemCollection> processList = new ArrayList<ItemCollection>();

		String sQuery = "SELECT process FROM Entity AS process "
				+ " JOIN process.textItems AS t2"
				+ " WHERE process.type = 'process'"
				+ " AND t2.itemName = 'txtname'" + " ORDER BY t2.itemValue asc";
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, -1);

		// create optimized list
		for (ItemCollection process : col) {

			ItemCollection clone = WorkitemHelper.clone(process);
			clone.replaceItemValue("isTeam", false);
			clone.replaceItemValue("isManager", false);

			// check the isTeam status for the current user
			List<String> userNameList = entityService.getUserNameList();
			Vector<String> vNameList = (Vector<String>) process
					.getItemValue("namTeam");
			// check if one entry matches....
			for (String username : userNameList) {
				if (vNameList.indexOf(username) > -1) {
					clone.replaceItemValue("isTeam", true);
					break;
				}
			}
			// check the isManager status for the current user
			vNameList = (Vector<String>) process.getItemValue("namManager");
			// check if one entry matches....
			for (String username : userNameList) {
				if (vNameList.indexOf(username) > -1) {
					clone.replaceItemValue("isManager", true);
					break;
				}
			}

			// check the isAssist status for the current user
			vNameList = (Vector<String>) process.getItemValue("namAssist");
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
					process.getItemValue("txtWorkflowList"));
			clone.replaceItemValue("txtdescription",
					process.getItemValue("txtdescription"));

			processList.add(clone);

		}

		return processList;
	}

	@SuppressWarnings("unchecked")
	public List<ItemCollection> getSpaces() {

		logger.fine("[ProcessService] getSpaces");
		List<ItemCollection> spaces = new ArrayList<ItemCollection>();

		String sQuery = "SELECT space FROM Entity AS space "
				+ " JOIN space.textItems AS t2" + " WHERE space.type = 'space'"
				+ " AND t2.itemName = 'txtname'" + " ORDER BY t2.itemValue asc";
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, -1);

		// create optimized list
		for (ItemCollection space : col) {

			ItemCollection clone = WorkitemHelper.clone(space);
			clone.replaceItemValue("isTeam", false);
			clone.replaceItemValue("isManager", false);

			// check the isTeam status for the current user
			List<String> userNameList = entityService.getUserNameList();
			Vector<String> vNameList = (Vector<String>) space
					.getItemValue("namTeam");
			// check if one entry matches....
			for (String username : userNameList) {
				if (vNameList.indexOf(username) > -1) {
					clone.replaceItemValue("isTeam", true);
					break;
				}
			}
			// check the isManager status for the current user
			vNameList = (Vector<String>) space.getItemValue("namManager");
			// check if one entry matches....
			for (String username : userNameList) {
				if (vNameList.indexOf(username) > -1) {
					clone.replaceItemValue("isManager", true);
					break;
				}
			}

			// check the isAssist status for the current user
			vNameList = (Vector<String>) space.getItemValue("namAssist");
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
			clone.replaceItemValue("txtdescription",
					space.getItemValue("txtdescription"));

			spaces.add(clone);

		}
		return spaces;
	}

}
