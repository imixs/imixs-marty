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

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

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
 * 
 */

@DeclareRoles( { "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed( { "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class ProfileService  {

	@Resource
	SessionContext ctx;

	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	// Workflow Manager
	@EJB
	org.imixs.workflow.jee.ejb.WorkflowService wm;
	ItemCollection workItem = null;


	/**
	 * This method create an empty profile object The current username will be
	 * Automatically assigned to the field txtname
	 * 
	 */
	public ItemCollection createProfile() throws Exception {
		workItem = new ItemCollection();
		workItem.replaceItemValue("type", "profile");

		String aname = ctx.getCallerPrincipal().getName();
		workItem.replaceItemValue("txtName", aname);
		return workItem;
	}

	/**
	 * This method create an empty Profile object with the defined initial
	 * $processID The current username will be automatically assigned to the
	 * field txtname
	 */
	public ItemCollection createProfile(int initialProcessID) throws Exception {
		workItem = createProfile();
		workItem.replaceItemValue("$processID", new Integer(initialProcessID));
		return workItem;
	}

	/**
	 * This method create an empty profile object for a specific user name with
	 * the defined initial $processID The username will be automatically
	 * assigned to the field txtname
	 */
	public ItemCollection createProfile(int initialProcessID, String aName)
			throws Exception {
		workItem = createProfile(initialProcessID);
		workItem.replaceItemValue("txtName", aName);
		return workItem;
	}

	/**
	 * Deletes a team from the Team list
	 */
	public void deleteProfile(ItemCollection aproject) throws Exception {
		entityService.remove(aproject);
	}

	/**
	 * This method returns a profile ItemCollection for a specified id The
	 * method returns null if no Profile for this name was found
	 * 
	 * @param id
	 * @return
	 */
	public ItemCollection findProfile(String id) {
		return entityService.load(id);
	}

	/**
	 * This method returns a profile ItemCollection for a specified account name.
	 * if no name is supported the remote user name will by used to find the
	 * profile The method returns null if no Profile for this name was found
	 * 
	 * @param aname
	 * @return
	 */
	public ItemCollection findProfileByName(String aname) {

		if (aname == null)
			aname = ctx.getCallerPrincipal().getName();

		String sQuery = "SELECT DISTINCT profile FROM Entity as profile "
				+ " JOIN profile.textItems AS t2"
				+ " WHERE  profile.type= 'profile' "
				+ " AND t2.itemName = 'txtname' " + " AND t2.itemValue = '"
				+ aname + "' ";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);

		if (col.size() > 0) {
			ItemCollection aworkitem = col.iterator().next();
			return aworkitem;
		}
		return null;

	}

	/**
	 * This method returns a profile ItemCollection for a specified username.
	 * The username is mapped to a technical name inside a profile. The method
	 * returns null if no Profile for this name was found
	 * 
	 * @param aname
	 * @return
	 */
	public ItemCollection findProfileByUserName(String aname) {
		if (aname == null)
			aname = ctx.getCallerPrincipal().getName();

		String sQuery = "SELECT DISTINCT profile FROM Entity as profile "
				+ " JOIN profile.textItems AS t2"
				+ " WHERE  profile.type= 'profile' "
				+ " AND t2.itemName = 'txtusername' " + " AND t2.itemValue = '"
				+ aname.trim() + "' ";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);

		if (col.size() > 0) {
			ItemCollection aworkitem = col.iterator().next();
			return aworkitem;
		}
		return null;

	}
	
	/**
	 * This method returns a profile ItemCollection for a specified email address.
	 * The email address is mapped to a technical name inside a profile. The method
	 * returns null if no Profile for this email address was found
	 * 
	 * @param email
	 * @return
	 */
	public ItemCollection findProfileByEmail(String email) {
		if (email == null || "".equals(email))
			return null;
		
		String sQuery = "SELECT DISTINCT profile FROM Entity as profile "
				+ " JOIN profile.textItems AS t2"
				+ " WHERE  profile.type= 'profile' "
				+ " AND t2.itemName = 'txtemail' " + " AND t2.itemValue = '"
				+ email.toLowerCase().trim() + "' ";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);

		if (col.size() > 0) {
			ItemCollection aworkitem = col.iterator().next();
			return aworkitem;
		}
		return null;

	}


	/**
	 * Updates a Profile Entity
	 */
	public ItemCollection saveProfile(ItemCollection aeditProfile) throws Exception {
		if (!isValidProfile(aeditProfile))
			throw new Exception(
					"ProfileServiceBean - invalid profile object! Attribute 'txtname' not found");
		aeditProfile.replaceItemValue("type", "profile");
		return entityService.save(aeditProfile);

	}

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
	public ItemCollection processProfile(ItemCollection aeditProfile) throws Exception {

		if (!isValidUserName(aeditProfile))
			throw new Exception(
					"Username is already taken - verifiy txtname and txtusername");

		if (!isValidEmail(aeditProfile))
			throw new Exception(
					"Email is already taken - verifiy txtemail");

		
		if (!isValidProfile(aeditProfile))
			throw new Exception(
					"ProfileServiceBean - invalid profile object! Attribute 'txtname' not found");

		workItem = aeditProfile;

		workItem.replaceItemValue("type", "profile");

		// Process workitem...
		workItem = wm.processWorkItem(workItem);
	
		return workItem;

	}

	public List<ItemCollection> findAllProfiles(int row, int count) {
		ArrayList<ItemCollection> teamList = new ArrayList<ItemCollection>();
		String sQuery = "SELECT DISTINCT profile FROM Entity AS profile "
				+ " JOIN profile.textItems AS t2"
				+ " WHERE profile.type= 'profile' "
				+ " AND t2.itemName = 'txtname' "
				+ " ORDER BY t2.itemValue ASC";
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		for (ItemCollection aworkitem : col) {
			teamList.add(aworkitem);
		}
		return teamList;
	}

	/**
	 * This method validates if the attributes supported to a map are
	 * corresponding to the team structure
	 */
	private boolean isValidProfile(ItemCollection aproject) throws Exception {
		boolean bvalid = true;
		if (!aproject.hasItem("txtname"))
			bvalid = false;

		return bvalid;
	}

	

	/**
	 * verifies if the txtName and txtUsername is available. Attribute
	 * txtUsername is optional and will be only verified if provided.
	 * 
	 * returns true if name isn't still taken by another object.
	 * 
	 * @param aprofile
	 * @return
	 */
	private boolean isValidUserName(ItemCollection aprofile) {

		String sName = aprofile.getItemValueString("txtName");
		String sUserName = aprofile.getItemValueString("txtUserName");
		String sID = aprofile.getItemValueString("$uniqueid");

		String sQuery;

		// username provided?
		if (sUserName != null && !"".equals(sUserName))
			sQuery = "SELECT DISTINCT profile FROM Entity as profile "
					+ " JOIN profile.textItems AS n"
					+ " JOIN profile.textItems AS u" 
					+ " WHERE  profile.type = 'profile' "
					+ " AND ((n.itemName = 'txtname' " + " AND n.itemValue = '"
					+ sName + "') OR  (u.itemName = 'txtusername' "
					+ " AND u.itemValue = '" + sUserName + "'))"
					+ " AND profile.id<>'"+ sID + "' ";
		else
			// query only txtName
			sQuery = "SELECT DISTINCT profile FROM Entity as profile "
					+ " JOIN profile.textItems AS n" + " WHERE profile.id<>'"
					+ sID + "' AND  profile.type = 'profile' "
					+ " AND n.itemName = 'txtname' " + " AND n.itemValue = '"
					+ sName + "'";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);

		return (col.size() == 0);

	}
	
	
	
	
	/**
	 * verifies if the txtemail is available.
	 * returns true if address isn't still taken by another object.
	 * 
	 * @param aprofile
	 * @return
	 */
	private boolean isValidEmail(ItemCollection aprofile) {

		String sEmail = aprofile.getItemValueString("txtEmail");
		String sID = aprofile.getItemValueString("$uniqueid");

		String sQuery;

		// username provided?
		if (!"".equals(sEmail)) 
			sQuery = "SELECT DISTINCT profile FROM Entity as profile "
					+ " JOIN profile.textItems AS n"
					+ " WHERE  profile.type = 'profile' "
					+ " AND (n.itemName = 'txtemail' " + " AND n.itemValue = '"
					+ sEmail + "') "
					+ " AND profile.id<>'"+ sID + "' ";
		else
			return true;

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);

		return (col.size() == 0);

	}

}
