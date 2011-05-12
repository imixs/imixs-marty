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
package org.imixs.sywapps.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.imixs.workflow.ItemCollection;

/**
 * Provides Methods to create, manage and find a user profile.
 * 
 * @author rsoika
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
public class ProfileServiceBean implements ProfileService {

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
	 * automatical assigend to the field txtname
	 */
	public ItemCollection createProfile() throws Exception {
		workItem = new ItemCollection();
		workItem.replaceItemValue("type", "profile");

		String aname = ctx.getCallerPrincipal().getName();
		workItem.replaceItemValue("txtName", aname);
		return workItem;
	}

	/**
	 * This method create an empty project object with the defined initial
	 * $processID The current username will be automatical assigend to the field
	 * txtname
	 */
	public ItemCollection createProfile(int initialProcessID) throws Exception {
		workItem = createProfile();
		workItem.replaceItemValue("$processID", new Integer(initialProcessID));
		return workItem;
	}

	/**
	 * This method create an empty project object for a specific user name with
	 * the defined initial $processID The username will be automatical assigend
	 * to the field txtname
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
	 * Processes a Workitem expects an ItemCollection with the Datastructure for
	 * a profile. The Method verifies if txtname and txtusername is unique if
	 * provided. If username is already in use by a different object the method
	 * throws an exception.
	 * 
	 */
	public ItemCollection processProfile(ItemCollection aeditProfile) throws Exception {

		if (!isValidUserName(aeditProfile))
			throw new Exception(
					"Username is already taken - verifiy txtname and txtusername");

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

}
