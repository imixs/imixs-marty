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

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;

import org.imixs.workflow.ItemCollection;

/**
 * The Marty ProfileService is a sigelton EJB providing user attributes like the
 * username and user email. The service is used to cache names application wide.
 * 
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
@Singleton
public class ProfileService {

	int DEFAULT_CACHE_SIZE = 30;

	final int MAX_SEARCH_COUNT = 1;
	private Cache cache;

	private static Logger logger = Logger.getLogger(ProfileService.class
			.getName());

	@EJB
	private org.imixs.workflow.jee.ejb.EntityService entityService;

	@Resource
	private SessionContext ctx;

	/**
	 * PostContruct event - loads the imixs.properties.
	 */
	@PostConstruct
	void init() {
		// initialize cache
		cache = new Cache(DEFAULT_CACHE_SIZE);
	}

	/**
	 * This method returns a profile by its userid. The method uses an internal
	 * cache. If no name is provided the remote user name will by used to find
	 * the profile. The method returns null if no Profile for this name was
	 * found
	 * 
	 * @param userid
	 *            - the profile id
	 * @return cloned workitem
	 */
	public ItemCollection findProfileById(String userid) {

		if (userid == null || userid.isEmpty())
			userid = ctx.getCallerPrincipal().getName();

		// try to get name out from cache
		ItemCollection userProfile = (ItemCollection) cache.get(userid);
		if (userProfile == null) {
			logger.fine("[ProfileService] lookup profile '" + userid + "'");
			// lookup user profile....
			String sQuery = "SELECT DISTINCT profile FROM Entity as profile "
					+ " JOIN profile.textItems AS t2"
					+ " WHERE  profile.type= 'profile' "
					+ " AND t2.itemName = 'txtname' " + " AND t2.itemValue = '"
					+ userid + "' ";

			logger.finest("searchprofile: " + sQuery);

			Collection<ItemCollection> col = entityService.findAllEntities(
					sQuery, 0, MAX_SEARCH_COUNT);

			if (col.size() > 0) {
				userProfile = col.iterator().next();
				// clone workitem
				userProfile = cloneWorkitem(userProfile);

				// cache profile
				cache.put(userid, userProfile);
			} else{
				logger.fine("[ProfileService] profile '" + userid + "' not found");
			}
		}
		return userProfile;

	}

	public static ItemCollection cloneWorkitem(ItemCollection aWorkitem) {
		ItemCollection clone = new ItemCollection();

		// clone the standard WorkItem properties
		clone.replaceItemValue("$UniqueID", aWorkitem.getItemValue("$UniqueID"));
		clone.replaceItemValue("$ModelVersion",
				aWorkitem.getItemValue("$ModelVersion"));
		clone.replaceItemValue("$ProcessID",
				aWorkitem.getItemValue("$ProcessID"));
		clone.replaceItemValue("$Created", aWorkitem.getItemValue("$Created"));
		clone.replaceItemValue("$Modified", aWorkitem.getItemValue("$Modified"));
		clone.replaceItemValue("$isAuthor", aWorkitem.getItemValue("$isAuthor"));

		clone.replaceItemValue("txtWorkflowStatus",
				aWorkitem.getItemValue("txtWorkflowStatus"));

		clone.replaceItemValue("txtName", aWorkitem.getItemValue("txtName"));
		clone.replaceItemValue("txtUserName",
				aWorkitem.getItemValue("txtUserName"));
		clone.replaceItemValue("txtEmail", aWorkitem.getItemValue("txtEmail"));

		return clone;
	}

	/**
	 * Cache implementation to hold config entities
	 * 
	 * @author rsoika
	 * 
	 */
	class Cache extends LinkedHashMap<String, ItemCollection> implements
			Serializable {
		private static final long serialVersionUID = 1L;
		private final int capacity;

		public Cache(int capacity) {
			super(capacity + 1, 1.1f, true);
			this.capacity = capacity;
		}

		protected boolean removeEldestEntry(Entry<String, ItemCollection> eldest) {
			return size() > capacity;
		}
	}

}
