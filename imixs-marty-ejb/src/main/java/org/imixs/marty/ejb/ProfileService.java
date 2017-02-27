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
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.PropertyService;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The Marty ProfileService is a sigelton EJB providing user attributes like the
 * username and user email. The service is used to cache names application wide.
 * 
 * 
 * @author rsoika
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Singleton
public class ProfileService {

	final int DEFAULT_CACHE_SIZE = 100;

	final int MAX_SEARCH_COUNT = 1;
	private Cache cache;

	private static Logger logger = Logger.getLogger(ProfileService.class.getName());

	@EJB
	private DocumentService documentService;

	@EJB
	private PropertyService propertyService;

	@Resource
	private SessionContext ctx;

	/**
	 * PostContruct event - loads the imixs.properties.
	 */
	@PostConstruct
	void init() {
		// initialize cache...
		reset();
	}

	/**
	 * resets the profile cache..
	 */
	public void reset() {
		// try to lookup cache size...
		int iCacheSize = DEFAULT_CACHE_SIZE;

		// early initialization did not work in Wildfly because of security
		// problem
		// see issue-#59
		// try {
		// String sCacheSize = propertyService.getProperties().getProperty(
		// "profileservice.cachesize", DEFAULT_CACHE_SIZE + "");
		//
		//
		//
		// iCacheSize = Integer.parseInt(sCacheSize);
		// } catch (NumberFormatException nfe) {
		// logger.warning("ProfileService unable to determine property:
		// profileservice.cachesize - check imixs.properties!");
		// iCacheSize = DEFAULT_CACHE_SIZE;
		// }
		// initialize cache
		cache = new Cache(iCacheSize);
	}

	/**
	 * This method returns the property 'profile.lowerCaseUserID'. The default
	 * value is 'true'
	 * 
	 * @return
	 */
	public boolean useLowerCaseUserID() {
		String value = propertyService.getProperties().getProperty("profile.lowerCaseUserID", "true");
		if ("false".equals(value))
			return false;
		else
			return true;
	}

	/**
	 * This method returns a profile by its id. The method uses an internal
	 * cache. The method returns null if no Profile for this name was found
	 * 
	 * The returned workitem is a cloned version of the profile entity and can
	 * not be processed or updated. Use lookupProfile to get the full entity of
	 * a profile.
	 * 
	 * @param userid
	 *            - the profile id
	 * @return cloned workitem
	 */
	public ItemCollection findProfileById(String userid) {
		return findProfileById(userid, false);

	}

	/**
	 * This method returns a profile by its id. The method uses an internal
	 * cache. The method returns null if no Profile for this name was found.
	 * 
	 * The returned workitem is a cloned version of the profile entity and can
	 * not be processed or updated. Use lookupProfile to get the full entity of
	 * a profile.
	 * 
	 * If the boolean 'refresh' is true the method lookup the user in any case
	 * with a JQPL statement and updates the cache.
	 * 
	 * The userId is always lower case!
	 * 
	 * @param userid
	 *            - the profile id
	 * @param refresh
	 *            - boolean indicates if the internal cache should be used
	 * @return cloned workitem
	 */
	public ItemCollection findProfileById(String userid, boolean refresh) {

		if (userid == null || userid.isEmpty()) {
			return null;
		}

		// lower case userId
		if (useLowerCaseUserID())
			userid = userid.toLowerCase();

		// try to get name out from cache
		ItemCollection userProfile = null;

		// use cache?
		if (!refresh)
			userProfile = (ItemCollection) cache.get(userid);
		// not yet cached?
		if (userProfile == null && !cache.containsKey(userid)) {
			userProfile = lookupProfileById(userid);
			if (userProfile != null) {
				// put a clone workitem into the cahe
				userProfile = cloneWorkitem(userProfile);
				// cache profile
				cache.put(userid, userProfile);
				logger.fine("[ProfileService] profile '" + userid + "' cached");
			} else {
				logger.fine("[ProfileService] profile '" + userid + "' not found");
				// put null entry into cache to avoid next lookup!
				cache.put(userid, null);
			}
		} else {
			logger.fine("[ProfileService] get profile '" + userid + "' from cache");
		}
		return userProfile;

	}

	/**
	 * This method returns a profile by its id. In different to the
	 * findProfileById method this method lookups the profile and returns the
	 * full entity. The returned workItem can be processed.
	 * 
	 * Use findProfileById to work with the internal cache if there is no need
	 * to update the profile.
	 * 
	 * The userId is always lower case!
	 * 
	 * @param userid
	 *            - the profile id
	 * @return profile workitem
	 */
	public ItemCollection lookupProfileById(String userid) {

		if (userid == null || userid.isEmpty()) {
			logger.warning("[ProfileService] lookupProfileById - no id provided!");
			return null;
		}

		// lower case userId
		if (useLowerCaseUserID()) {
			userid = userid.toLowerCase();
		}
		// try to get name out from cache
		ItemCollection userProfile = null;

		logger.fine("[ProfileService] lookupProfileById '" + userid + "'");
		// lookup user profile....
//		String sQuery = "SELECT DISTINCT profile FROM Entity as profile " + " JOIN profile.textItems AS t2"
//				+ " WHERE  profile.type= 'profile' " + " AND t2.itemName = 'txtname' " + " AND t2.itemValue = '"
//				+ userid + "' ";


		String sQuery="(type:\"profile\" AND txtname:\""+userid + "\")";
		logger.finest("searchprofile: " + sQuery);
		
		Collection<ItemCollection> col;
		try {
			col = documentService.find(sQuery, 1, 0);
		} catch (QueryException e) {
			throw new InvalidAccessException(InvalidAccessException.INVALID_ID,e.getMessage(),e);
		}

		if (col.size() > 0) {
			userProfile = col.iterator().next();
		} else {
			logger.fine("[ProfileService] lookup profile '" + userid + "' failed");
		}
		return userProfile;
	}

	

	/**
	 * This method removes a profile from the cache.
	 * 
	 * @param userid
	 */
	public void discardCache(String userid) {
		cache.remove(userid);
	}

	/**
	 * This method closes a profile entity and computes the attributes
	 * txtUsername and txtInitials
	 * 
	 * @param aWorkitem
	 * @return
	 */
	public static ItemCollection cloneWorkitem(ItemCollection aWorkitem) {
		ItemCollection clone = new ItemCollection();

		// clone the standard WorkItem properties
		clone.replaceItemValue("Type", aWorkitem.getItemValue("Type"));
		clone.replaceItemValue("$UniqueID", aWorkitem.getItemValue("$UniqueID"));
		clone.replaceItemValue("$ModelVersion", aWorkitem.getItemValue("$ModelVersion"));
		clone.replaceItemValue("$ProcessID", aWorkitem.getItemValue("$ProcessID"));
		clone.replaceItemValue("$Created", aWorkitem.getItemValue("$Created"));
		clone.replaceItemValue("$Modified", aWorkitem.getItemValue("$Modified"));
		clone.replaceItemValue("$isAuthor", aWorkitem.getItemValue("$isAuthor"));

		clone.replaceItemValue("txtWorkflowStatus", aWorkitem.getItemValue("txtWorkflowStatus"));
		clone.replaceItemValue("txtWorkflowSummary", aWorkitem.getItemValue("txtWorkflowSummary"));
		clone.replaceItemValue("txtWorkflowAbstract", aWorkitem.getItemValue("txtWorkflowAbstract"));
		clone.replaceItemValue("txtEmail", aWorkitem.getItemValue("txtEmail"));
		clone.replaceItemValue("namdeputy", aWorkitem.getItemValue("namdeputy"));
		clone.replaceItemValue("txtusericon", aWorkitem.getItemValue("txtusericon"));

		// get accountName
		String sAccountName = aWorkitem.getItemValueString("txtName");
		clone.replaceItemValue("txtName", sAccountName);

		// test txtuserName
		String sUserName = aWorkitem.getItemValueString("txtUserName");
		if (!sUserName.isEmpty()) {
			clone.replaceItemValue("txtUserName", sUserName);
		} else {
			// use account name
			clone.replaceItemValue("txtUserName", sAccountName);
		}

		// construct initials (2 digits)
		String sInitials = aWorkitem.getItemValueString("txtinitials");
		if (sInitials.isEmpty()) {
			// default
			sInitials = "NO";
			if (!sUserName.isEmpty() && sUserName.length() > 2) {
				int iPos = sUserName.indexOf(' ');
				if (iPos > -1) {
					sInitials = sUserName.substring(0, 1);
					sInitials = sInitials + sUserName.substring(iPos + 1, iPos + 2);
				} else {
					sInitials = sUserName.substring(0, 2);
				}
			} else {
				sInitials = sAccountName;
			}
			clone.replaceItemValue("txtinitials", sInitials);
		}
		return clone;
	}

	/**
	 * Cache implementation to hold config entities
	 * 
	 * @author rsoika
	 * 
	 */
	class Cache extends LinkedHashMap<String, ItemCollection> implements Serializable {
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
