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
import java.util.logging.Logger;

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
 * 
 * @author rsoika
 * 
 */
public class ProfilePlugin extends AbstractPlugin {

	private EntityService entityService = null;
	private static Logger logger = Logger.getLogger("org.imixs.marty");
	private ItemCollection profile = null;

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);

		// check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			WorkflowService ws = (WorkflowService) actx;
			entityService = ws.getEntityService();
		}

	}

	/**
	 * The Plug-in verifies if the workitem is from the type 'profile'. The
	 * plug-in tests if the usernam or email is unique
	 **/
	@Override
	public int run(ItemCollection aworkItem, ItemCollection documentActivity)
			throws PluginException {

		// verify workitem type
		if (!"profile".equals(aworkItem.getItemValueString("type")))
			return Plugin.PLUGIN_OK;

		profile = aworkItem;

		String sUsername = profile.getItemValueString("txtName");

		// update the txtname if not already set
		if ("".equals(sUsername)) {
			logger.fine("initialize profile with username: " + this.getUserName());
			profile.replaceItemValue("txtName", this.getUserName());
		}
		if (!isValidUserName())
			throw new PluginException(
					"Username is already taken - verifiy txtname and txtusername");

		if (!isValidEmail())
			throw new PluginException(
					"Email is already taken - verifiy txtemail");

		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int arg0) throws PluginException {

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
	private boolean isValidUserName() {

		String sName = profile.getItemValueString("txtName");
		String sUserName = profile.getItemValueString("txtUserName");
		String sID = profile.getItemValueString("$uniqueid");

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
	private boolean isValidEmail() {

		String sEmail = profile.getItemValueString("txtEmail");
		String sID = profile.getItemValueString("$uniqueid");

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

}
