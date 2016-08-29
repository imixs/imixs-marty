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

package org.imixs.marty.ejb.security;

import java.util.Collection;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Plugin updates the userId and password for a user profile. The Update
 * requires the UserGroupService EJB.
 * 
 * The Plugin runs only if the UserGroupService EJB is deployed and the BASIC
 * configuration property 'keyEnableUserDB' is 'true'.
 * 
 * @see UserGroupService
 * @author rsoika
 * @version 1.0
 * 
 */
public class UserGroupPlugin extends AbstractPlugin {
	public static final String INVALID_CONTEXT = "INVALID_CONTEXT";
	static final int EVENT_PROFILE_LOCK = 90;
	static final int TASK_PPROFILE_ACTIVE = 210;
	DocumentService documentService = null;
	UserGroupService userGroupService = null;;

	int sequenceNumber = -1;
	ItemCollection workitem = null;
	private static Logger logger = Logger.getLogger(UserGroupPlugin.class.getName());

	/**
	 * Try to lookup the UserGroupService. If not availalbe the plugin will not
	 * run.
	 */
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);

		// check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			WorkflowService ws = (WorkflowService) actx;
			// get latest model version....
			documentService = ws.getDocumentService();
		}

		// lookup profile service EJB
		String jndiName = "ejb/UserGroupService";
		InitialContext ictx;
		try {
			ictx = new InitialContext();
			Context ctx = (Context) ictx.lookup("java:comp/env");
			userGroupService = (UserGroupService) ctx.lookup(jndiName);
		} catch (NamingException e) {
			logger.warning(
					"[UserGroupPlugin] unable to lookup UserGroupService - check deployment or system model configuration!");
			userGroupService = null;
		}
	}

	/**
	 * This method updates the user object and the group relation ships
	 * 
	 * @return
	 * @throws PluginException
	 */
	@Override
	public int run(ItemCollection documentContext, ItemCollection documentActivity) throws PluginException {

		// skip if no userGroupService found
		if (userGroupService == null)
			return Plugin.PLUGIN_OK;

		workitem = documentContext;

		// check entity type....
		String sType = workitem.getItemValueString("Type");
		if (!("profile".equals(sType)))
			return Plugin.PLUGIN_OK;

		// skip if userDB support is not enabled
		if (!isUserDBEnabled())
			return Plugin.PLUGIN_OK;

		// if processid=210 and activity=20 - delete all groups
		int iProcessID = workitem.getItemValueInteger("$ProcessID");
		int iActivityID = documentActivity.getItemValueInteger("numActivityID");
		if (iProcessID >= TASK_PPROFILE_ACTIVE && iActivityID == EVENT_PROFILE_LOCK) {
			logger.info("Lock profile '" + workitem.getItemValueString("txtname") + "'");
			workitem.replaceItemValue("txtGroups", "");
		}

		logger.fine("[UserGroupPlugin] update profile '" + workitem.getItemValueString("txtname") + "'....");
		userGroupService.updateUser(workitem);

		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int status) throws PluginException {

	}

	/**
	 * Returns true if the flag keyEnableUserDB is set to true.
	 * 
	 * @return
	 */
	private boolean isUserDBEnabled() {
		String searchterm = "(type:\"configuration\" AND txtname:\"BASIC\")";
		Collection<ItemCollection> col = documentService.find(searchterm, 0, 1);

		if (col.size() > 0) {
			ItemCollection config = col.iterator().next();
			return config.getItemValueBoolean("keyEnableUserDB");
		}

		return false;
	}

}
