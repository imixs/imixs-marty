/*******************************************************************************
 *  Imixs IX Workflow Technology
 *  Copyright (C) 2001, 2008 Imixs Software Solutions GmbH,  
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
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika
 *******************************************************************************/
package org.imixs.marty.ejb.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * user Group service to provide method for managing user and groups settings.
 * 
 * @author rsoika
 * 
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@LocalBean
public class UserGroupService {

	@PersistenceContext(unitName = "org.imixs.workflow.jee.jpa")
	private EntityManager manager;

	@Resource
	SessionContext ctx;

	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	private static Logger logger = Logger.getLogger(UserGroupService.class
			.getName());

	/**
	 * This method verifies the profile data and creates or update the
	 * corresponding user entries in the user tables.
	 * 
	 * NOTE: this method did not change a userid. To do this use the method
	 * changeUser!
	 * 
	 * @param profile
	 */
	@SuppressWarnings("unchecked")
	public void updateUser(ItemCollection profile) {
		boolean bNewEntity = false;

		String sType = profile.getItemValueString("Type");
		if (!("profile".equals(sType)))
			return;

		String sID = profile.getItemValueString("txtName");
		String sPassword = profile.getItemValueString("txtPassword");
		Collection<String> groups = profile.getItemValue("txtGroups");

		UserId user = null;
		user = manager.find(UserId.class, sID);
		if (user == null) {
			user = new UserId(sID);
			bNewEntity = true;
		}

		// encrypt and update password
		if (sPassword != null && !"".equals(sPassword)) {
			String sEncryptedPasswort = Crypter.crypt(sPassword);
			user.setPassword(sEncryptedPasswort);
			// remove password....
			profile.removeItem("txtPassword");
		}

		// find group relation ships
		Set<UserGroup> groupList = new HashSet<UserGroup>();
		for (String aGroup : groups) {
			UserGroup group = manager.find(UserGroup.class, aGroup);
			// if group dos not exist - create it...
			if (group == null) {
				group = new UserGroup(aGroup);
				manager.persist(user);
			}

			groupList.add(group);

		}

		// update groups
		user.setUserGroups(groupList);

		if (bNewEntity)
			manager.persist(user);
		else
			manager.merge(user);
	}

	/**
	 * This method changes the userID of an existing user entry and updates the
	 * userGroup table entries.
	 * 
	 * @param oldID
	 *            - the existing userEntry
	 * @param newID
	 *            - the name of the new id
	 */
	public void changeUserId(String oldID, String newID) {
		UserId user = null;

		// test if new userid still exits
		user = manager.find(UserId.class, newID);
		if (user != null) {
			logger.warning("[UserGroupService] changeUser - new userId '"
					+ newID + "'is still in Use!");
			return;
		}

		// find old user entry....
		user = manager.find(UserId.class, oldID);
		if (user == null) {
			logger.warning("[UserGroupService] changeUser - UserID '" + oldID
					+ "' not found!");
			return;
		}

		// change id
		UserId newUser = new UserId(newID);
		newUser.setPassword(user.getPassword());
		newUser.setUserGroups(user.getUserGroups());
		manager.persist(newUser);

		// remove old
		manager.remove(user);
	}

	/**
	 * This method deletes the userID of an existing user entry and also the
	 * userGroup table entries.
	 * 
	 * @param userID
	 *            - the existing userEntry
	 */
	public void removeUserId(String userID) {
		UserId user = null;

		// test if userid exits
		user = manager.find(UserId.class, userID);
		if (user == null) {
			logger.warning("[UserGroupService] removeUserId - userId '"
					+ userID + "' did not exist!");
			return;
		}

		// remove old
		manager.remove(user);

	}

	/**
	 * This method verifies if a default user id already exists. If no userID
	 * exists the method generates a default account 'admin' with password
	 * 'adminadmin'
	 * 
	 * @throws AccessDeniedException
	 */
	@SuppressWarnings("unchecked")
	public void initUserIDs() {

		String sQuery = "SELECT user FROM UserId AS user ";

		Query q = manager.createQuery(sQuery);
		q.setFirstResult(0);
		q.setMaxResults(1);

		Collection<UserId> entityList = q.getResultList();

		if (entityList == null || entityList.size() == 0) {
			logger.info("Create default admin account");
			// create a default account
			ItemCollection profile = new ItemCollection();
			profile.replaceItemValue("type", "profile");
			profile.replaceItemValue("txtName", "admin");
			profile.replaceItemValue("txtPassword", "adminadmin");
			profile.replaceItemValue("txtGroups", "IMIXS-WORKFLOW-Manager");

			// hard coded version nummer!
			profile.replaceItemValue("$modelversion", "system-en-0.0.1");
			profile.replaceItemValue("$processid", 210);

			try {
				entityService.save(profile);
			} catch (AccessDeniedException e) {
				logger.warning("UserGroupService - unable to initialize default admin account");
				logger.severe(e.getMessage());
				// throw new RuntimeException(e);
				return;
			}
			this.updateUser(profile);

		}

	}

}
