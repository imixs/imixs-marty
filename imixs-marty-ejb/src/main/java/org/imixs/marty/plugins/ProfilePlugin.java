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

package org.imixs.marty.plugins;

import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.imixs.marty.ejb.ProfileService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.plugins.jee.AbstractPlugin;

/**
 * This plug-in supports additional business logic for profile entities. This
 * Plugins is used by the System Workflow when a userProfile is processed
 * (typically when a User logged in).
 * 
 * In additon the Plugin provides a mechanism to translate elements of an
 * activityEntity to replace placeholders for a user id with the corresponding
 * user name. There for the plugin uses the profileService EJB
 * 
 * @author rsoika
 * 
 */
public class ProfilePlugin extends AbstractPlugin {

	private EntityService entityService = null;
	private ProfileService profileService = null;

	private static Logger logger = Logger.getLogger(ProfilePlugin.class
			.getName());

	// error codes
	public static String USERNAME_ALREADY_TAKEN = "USERNAME_ALREADY_TAKEN";
	public static String INVALID_USERNAME = "INVALID_USERNAME";
	public static String EMAIL_ALREADY_TAKEN = "EMAIL_ALREADY_TAKEN";
	public static String NO_PROFILE_SERVICE_FOUND = "NO_SEQUENCE_SERVICE_FOUND";

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);

		// check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			WorkflowService ws = (WorkflowService) actx;
			entityService = ws.getEntityService();
		}

		// lookup profile service EJB
		String jndiName = "ejb/ProfileService";
		InitialContext ictx;
		try {
			ictx = new InitialContext();

			Context ctx = (Context) ictx.lookup("java:comp/env");
			profileService = (ProfileService) ctx.lookup(jndiName);
		} catch (NamingException e) {
			throw new PluginException(ProfilePlugin.class.getSimpleName(),
					NO_PROFILE_SERVICE_FOUND,
					"[ProfilePlugin] unable to lookup profileService: ", e);
		}

	}

	/**
	 * The Plug-in verifies if the workitem is from the type 'profile'. The
	 * plug-in tests if the usernam or email is unique
	 **/
	@Override
	public int run(ItemCollection workItem, ItemCollection documentActivity)
			throws PluginException {

		// validate profile..
		if ("profile".equals(workItem.getItemValueString("type"))) {
			validateUserProfile(workItem);
			// discared cache for this name
			profileService.discardCache(workItem.getItemValueString("txtName"));
		}

		// translate dynamic activity values
		if ("workitem".equals(workItem.getItemValueString("type")))
			updateActivityEntity(workItem, documentActivity);

		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int arg0) throws PluginException {

	}

	/**
	 * replace the text phrases in the activity
	 * 
	 * @param workItem
	 * @param documentActivity
	 */
	void updateActivityEntity(ItemCollection workItem,
			ItemCollection documentActivity) {
		String sText;

		String[] fields = { "rtfresultlog", "txtworkflowabstract",
				"txtworkflowsummary", "txtMailSubject", "rtfMailBody" };

		for (String aField : fields) {
			sText = documentActivity.getItemValueString(aField);
			sText = replaceUsernames(sText, workItem);
			documentActivity.replaceItemValue(aField, sText);

		}

	}

	/**
	 * The method validates the userProfile entity. The txtName property will be
	 * initialized if a new profile is created The txtName property will always
	 * be lower case!
	 * 
	 * @param profile
	 * @throws PluginException
	 */
	void validateUserProfile(ItemCollection profile) throws PluginException {
		String sUsername = profile.getItemValueString("txtName");

		if (this.getUserName() == null || this.getUserName().isEmpty()) {
			throw new PluginException(ProfilePlugin.class.getSimpleName(),
					INVALID_USERNAME, "Invalid username - username is empty");
		}

		// update the txtname if not already set
		if ("".equals(sUsername)) {
			// trim and lower case username!
			sUsername = this.getUserName().toLowerCase().trim();
			logger.fine("initialize profile with username: " + sUsername);
			profile.replaceItemValue("txtName", sUsername);
		}
		if (!isValidUserName(profile))
			throw new PluginException(
					ProfilePlugin.class.getSimpleName(),
					USERNAME_ALREADY_TAKEN,
					"Username is already taken - verifiy txtname and txtusername",
					new Object[] { profile.getItemValueString("txtName") });

		if (!isValidEmail(profile))
			throw new PluginException(ProfilePlugin.class.getSimpleName(),
					EMAIL_ALREADY_TAKEN,
					"Email is already taken - verifiy txtemail",
					new Object[] { profile.getItemValueString("txtEmail") });

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
	boolean isValidUserName(ItemCollection profile) {

		String sName = profile.getItemValueString("txtName");
		String sUserName = profile.getItemValueString("txtUserName");
		String sID = profile.getItemValueString("$uniqueid");

		// Trim names....
		if (!sName.equals(sName.trim())) {
			sName = sName.trim();
			profile.replaceItemValue("txtName", sName);
		}
		// lower case userid?
		if ((profileService.useLowerCaseUserID())
				&& (!sName.equals(sName.toLowerCase()))) {
			sName = sName.toLowerCase();
			profile.replaceItemValue("txtName", sName);
		}

		if (!sUserName.equals(sUserName.trim())) {
			sUserName = sUserName.trim();
			profile.replaceItemValue("txtUserName", sUserName);
		}

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
					+ " AND profile.id<>'" + sID + "' ";
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
	 * verifies if the txtemail is available. returns true if address isn't
	 * still taken by another object.
	 * 
	 * @param aprofile
	 * @return
	 */
	boolean isValidEmail(ItemCollection profile) {

		String sEmail = profile.getItemValueString("txtEmail");
		String sID = profile.getItemValueString("$uniqueid");

		// Trim email....
		if (!sEmail.equals(sEmail.trim())) {
			sEmail = sEmail.trim();
			profile.replaceItemValue("txtEmail", sEmail);
		}

		String sQuery;

		// username provided?
		if (!"".equals(sEmail))
			sQuery = "SELECT DISTINCT profile FROM Entity as profile "
					+ " JOIN profile.textItems AS n"
					+ " WHERE  profile.type = 'profile' "
					+ " AND (n.itemName = 'txtemail' " + " AND n.itemValue = '"
					+ sEmail + "') " + " AND profile.id<>'" + sID + "' ";
		else
			return true;

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);

		return (col.size() == 0);

	}

	/**
	 * this method parses a string for xml tag <username>. Those tags will be
	 * replaced with the corresponding userProfile property 'txtUserName' <code>
	 *   
	 *   hello <username>namCreator</username>
	 *   
	 *   
	 * </code>
	 * 
	 * 
	 * If the itemValue is a multiValue object the single values can be
	 * spearated by a separator
	 * 
	 * <code>
	 *  
	 *  Team List: <username separator="<br />">txtTeam</username>
	 * 
	 * </code>
	 * 
	 * 
	 * 
	 */
	public String replaceUsernames(String aString,
			ItemCollection documentContext) {
		int iTagStartPos;
		int iTagEndPos;

		int iContentStartPos;
		int iContentEndPos;

		int iSeparatorStartPos;
		int iSeparatorEndPos;

		String sSeparator = " ";
		String sItemValue;

		if (aString == null)
			return "";

		// test if a <value> tag exists...
		while ((iTagStartPos = aString.toLowerCase().indexOf("<username")) != -1) {

			iTagEndPos = aString.toLowerCase().indexOf("</username>",
					iTagStartPos);

			// if no end tag found return string unchanged...
			if (iTagEndPos == -1)
				return aString;

			// reset pos vars
			iContentStartPos = 0;
			iContentEndPos = 0;

			iSeparatorStartPos = 0;
			iSeparatorEndPos = 0;
			sSeparator = " ";
			sItemValue = "";

			// so we now search the beginning of the tag content
			iContentEndPos = iTagEndPos;
			// start pos is the last > before the iContentEndPos
			String sTestString = aString.substring(0, iContentEndPos);
			iContentStartPos = sTestString.lastIndexOf('>') + 1;

			// if no end tag found return string unchanged...
			if (iContentStartPos >= iContentEndPos)
				return aString;

			iTagEndPos = iTagEndPos + "</username>".length();

			// now we have the start and end position of a tag and also the
			// start and end pos of the value

			// next we check if the start tag contains a 'separator'
			// attribute
			iSeparatorStartPos = aString.toLowerCase().indexOf("separator=",
					iTagStartPos);
			// extract format string if available
			if (iSeparatorStartPos > -1
					&& iSeparatorStartPos < iContentStartPos) {
				iSeparatorStartPos = aString.indexOf("\"", iSeparatorStartPos) + 1;
				iSeparatorEndPos = aString
						.indexOf("\"", iSeparatorStartPos + 1);
				sSeparator = aString.substring(iSeparatorStartPos,
						iSeparatorEndPos);
			}

			// extract Item Value
			sItemValue = aString.substring(iContentStartPos, iContentEndPos);

			List<String> tempList = documentContext.getItemValue(sItemValue);
			// clone List
			List<String> vUserIDs = new Vector(tempList);
			// get usernames ....
			for (int i = 0; i < vUserIDs.size(); i++) {
				ItemCollection profile = profileService
						.findProfileById(vUserIDs.get(i));
				if (profile != null) {
					vUserIDs.set(i, profile.getItemValueString("txtUserName"));
				}
			}

			// format field value
			String sResult = formatItemValues(vUserIDs, sSeparator, "");

			// now replace the tag with the result string
			aString = aString.substring(0, iTagStartPos) + sResult
					+ aString.substring(iTagEndPos);
		}

		return aString;

	}

}
