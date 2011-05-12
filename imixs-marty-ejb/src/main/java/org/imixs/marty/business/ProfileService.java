/*******************************************************************************
 *  Imixs IX Workflow Technology
 *  Copyright (C) 2003, 2008 Imixs Software Solutions GmbH,  
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
 *  
 *******************************************************************************/
package org.imixs.marty.business;

import java.util.List;

import org.imixs.workflow.ItemCollection;

/**
 * 
 * This Service Facade encapsulates the management of a User Profile. The
 * service manages User profiles with unique name and username.
 * 
 * A Profile is represented by a ItemCollection with the following Attributes:
 * 
 * txtName - Name of the user - CallerPrincipal - a technical username
 * txtUserName - (optinal) a unique username used to display a non technical
 * name
 * 
 * The Services provides a process method to process a userprofile with a
 * workflow step. This method checks the uniqueness of username and name.
 * 
 * @version 0.0.2
 * @author rsoika
 * 
 */

public interface ProfileService {

	/**
	 * This method create an empty profile object The current username will be
	 * Automatically assigned to the field txtname
	 * 
	 */
	public ItemCollection createProfile() throws Exception;

	/**
	 * This method create an empty Profile object with the defined initial
	 * $processID The current username will be automatically assigned to the
	 * field txtname
	 */
	public ItemCollection createProfile(int initialProcessID) throws Exception;

	/**
	 * This method create an empty profile object for a specific user name with
	 * the defined initial $processID The username will be automatically
	 * assigned to the field txtname
	 */
	public ItemCollection createProfile(int initialProcessID, String aUser)
			throws Exception;

	/**
	 * This method process a profile object using the IX WorkflowManager. The
	 * method also verifies if the attributes txtname and txtusername are
	 * unique. The method throws an exception if username or name already
	 * exists.
	 * 
	 * @param aProfile
	 *            ItemCollection representing the user profile
	 * @param activityID
	 *            activity ID the issue should be processed
	 */
	public ItemCollection processProfile(ItemCollection aProfile)
			throws Exception;

	/**
	 * This method deletes an existing profile.
	 * 
	 * @param aTeamName
	 */
	public void deleteProfile(ItemCollection aProfile) throws Exception;

	/**
	 * This method returns a profile ItemCollection for a specified id The
	 * method returns null if no Profile for this name was found
	 * 
	 * @param id
	 * @return
	 */
	public ItemCollection findProfile(String id);

	/**
	 * This method returns a profile ItemCollection for a specified technical
	 * name. if no name is supported the remote user name will by used to find
	 * the profile The method returns null if no Profile for this name was found
	 * 
	 * @param aname
	 * @return
	 */
	public ItemCollection findProfileByName(String aname);

	/**
	 * This method returns a profile ItemCollection for a specified user name.
	 * The UserName is mapped to the technical account name. Also the UserName
	 * is a unique name.
	 * 
	 * @param aname
	 * @return
	 */
	public ItemCollection findProfileByUserName(String aname);

	
	/**
	 * This method returns a profile ItemCollection for a specified email address.
	 * The email address is mapped to the technical account name. Also the email address
	 * is a unique name.
	 * 
	 * @param aname
	 * @return
	 */
	public ItemCollection findProfileByEmail(String aname);

	
	
	
	/**
	 * Updates a Profile Entity without verification of txtname or txtusername.
	 */
	public ItemCollection saveProfile(ItemCollection aproject) throws Exception;

	/**
	 * Returns a list of projects
	 * 
	 * @param row
	 * @param count
	 * @return
	 */
	public List<ItemCollection> findAllProfiles(int row, int count);

}
