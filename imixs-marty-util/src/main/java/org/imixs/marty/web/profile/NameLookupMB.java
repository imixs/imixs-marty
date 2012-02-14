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

package org.imixs.marty.web.profile;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;

import org.imixs.marty.ejb.ProfileService;
import org.imixs.marty.util.Cache;
import org.imixs.workflow.ItemCollection;

/**
 * This backingBean supports namelookups to translate a technical user account
 * into a Displayname to be used in the Frontend. The Bean uses the
 * profileService EJB to lookup a name and holds a Cache to prevent repeatable
 * lookups.
 * 
 * @author rsoika
 * 
 */
public class NameLookupMB {

	final int MAX_CACHE_SIZE = 20;
	private Cache cache;
	private UserNameAdapter userNameAdapter = null;
	private EmailAdapter emailAdapter = null;

	/* Profile Service */
	@EJB
	ProfileService profileService;

	public NameLookupMB() {
		cache = new Cache(MAX_CACHE_SIZE);
		userNameAdapter = new UserNameAdapter();
		emailAdapter = new EmailAdapter();
	}

	/**
	 * This method uses the Map Interface as a return value to allow the
	 * parameterized access to a username.
	 * 
	 * 
	 * in a jsf page using a expression language like this:
	 * 
	 * #{nameLookupMB.userName[accountname]}
	 * 
	 * @see The inner class UserNameAdapter
	 * 
	 * @return
	 */
	public Map getUserName() {
		return userNameAdapter;

	}

	/**
	 * This method uses the Map Interface as a return value to allow the
	 * parameterized access to a email address of a user.
	 * 
	 * 
	 * in a jsf page using a expression language like this:
	 * 
	 * #{nameLookupMB.email[accountname]}
	 * 
	 * @see The inner class UserNameAdapter
	 * 
	 * @return
	 */
	public Map getEmail() {
		return emailAdapter;
	}

	/**
	 * This method returns the username (displayname) for a useraccount
	 * 
	 * @param aName
	 * @return
	 */
	public String findUserName(String aAccount) {
		String[] userArray;
		// try to get name out from cache
		userArray = (String[]) cache.get(aAccount);
		if (userArray == null)
			userArray = lookupUser(aAccount);

		return userArray[0];
	}

	/**
	 * This method returns the email for a useraccount
	 * 
	 * @param aName
	 * @return
	 */
	public String findEmail(String aAccount) {
		String[] userArray;
		// try to get name out from cache
		userArray = (String[]) cache.get(aAccount);
		if (userArray == null)
			userArray = lookupUser(aAccount);

		return userArray[1];
	}

	/**
	 * This method returns the useraccount name for a given username
	 * (displayname)
	 * 
	 * @param aName
	 * @return
	 */
	public String findAccountName(String aUserName) {
		// see if account is cached
		Set set = cache.entrySet();
		Iterator i = set.iterator();
		while (i.hasNext()) {
			Map.Entry me = (Map.Entry) i.next();
			if (aUserName.equals(me.getValue()))
				return me.getKey().toString();
		}

		// lookup object....
		return lookupAccount(aUserName);
	}

	/**
	 * this class performes a EJB Lookup for the corresponding userprofile The
	 * method stores the username and his email into a string array. So either
	 * the username or the email address will be cached in a single object.
	 * 
	 * @param aName
	 * @return array of username and string
	 */
	private String[] lookupUser(String aName) {
		String[] array = new String[2];

		// String sUserName = null;
		ItemCollection profile = profileService.findProfileByName(aName);
		// if profile null cache current name object
		if (profile == null) {
			array[0] = aName;
			array[1] = aName;
		} else {
			array[0] = profile.getItemValueString("txtuserName");
			array[1] = profile.getItemValueString("txtemail");
		}

		if ("".equals(array[0]))
			array[0] = aName;

		if ("".equals(array[1]))
			array[1] = array[0];

		// put name into cache
		cache.put(aName, array);

		return array;
	}

	/**
	 * this class performs a EJB Lookup for the corresponding userprofile
	 * 
	 * @param aName
	 * @return
	 */
	private String lookupAccount(String aUserName) {
		String sAccount = null;
		ItemCollection profile = profileService
				.findProfileByUserName(aUserName);
		// if profile null cache current name object
		if (profile == null)
			sAccount = aUserName;
		else
			sAccount = profile.getItemValueString("txtName");

		if ("".equals(sAccount))
			sAccount = aUserName;

		// put name into cache
		cache.put(aUserName, sAccount);

		return sAccount;
	}

	/**
	 * This class helps to addapt the behavior of a usernamLookup to a MapObject
	 * The Class overwrites the get Method and makes a coll to findUserName()
	 * 
	 * in a jsf page using a expression language like this:
	 * 
	 * #{mybean.userName['txtMyItem']}
	 * 
	 * 
	 * @author rsoika
	 * 
	 */
	class UserNameAdapter implements Map {

		public UserNameAdapter() {

		}

		/**
		 * returns a single value out of the ItemCollection if the key dos not
		 * exist the method will create a value automatical
		 */
		@SuppressWarnings("unchecked")
		public Object get(Object key) {
			return findUserName(key.toString());
		}

		public Object put(Object key, Object value) {
			return null;
		}

		/* ############### Default methods ################# */

		public void clear() {
		}

		public boolean containsKey(Object key) {
			return false;
		}

		public boolean containsValue(Object value) {
			return false;
		}

		public Set entrySet() {
			return null;
		}

		public boolean isEmpty() {
			return false;
		}

		public Set keySet() {
			return null;
		}

		public void putAll(Map m) {
		}

		public Object remove(Object key) {
			return null;
		}

		public int size() {
			return 0;
		}

		public Collection values() {
			return null;
		}

	}

	class EmailAdapter extends UserNameAdapter {

		public EmailAdapter() {

		}

		/**
		 * returns a single value out of the ItemCollection if the key dos not
		 * exist the method will create a value automatical
		 */
		@SuppressWarnings("unchecked")
		public Object get(Object key) {
			return findEmail(key.toString());
		}

	}
}
