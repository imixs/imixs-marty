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

import java.util.logging.Logger;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.imixs.marty.ejb.ProfileService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;

public class MailPlugin extends org.imixs.workflow.plugins.jee.MailPlugin {

	private ProfileService profileService = null;
	private boolean hasMailSession = false;
	private static Logger logger = Logger.getLogger("org.imixs.marty");

	@Override
	public void init(WorkflowContext actx) throws PluginException {

		super.init(actx);
		hasMailSession = true;
		// lookup profile service EJB
		String jndiName = "ejb/ProfileService";
		InitialContext ictx;
		try {
			ictx = new InitialContext();

			Context ctx = (Context) ictx.lookup("java:comp/env");
			profileService = (ProfileService) ctx.lookup(jndiName);
		} catch (NamingException e) {
			throw new PluginException(
					"[MailPlugin] unable to lookup ProfileService", e);

		}
	}

	@Override
	public void close(int arg0) throws PluginException {

		if (hasMailSession)
			super.close(arg0);
	}

	@Override
	public int run(ItemCollection arg0, ItemCollection arg1)
			throws PluginException {

		if (hasMailSession)
			return super.run(arg0, arg1);
		else
			return Plugin.PLUGIN_OK;
	}

	/**
	 * this helper method creates an internet address from a string if the
	 * string has illegal characters like whitespace the string will be
	 * surrounded with "". If you subclass this MailPlugin Class you can
	 * overwrite this method to return a different mail-address name or lookup a
	 * mail attribute in a directory like a ldap directory.
	 * 
	 * @param aAddr
	 * @return
	 * @throws AddressException
	 */
	public InternetAddress getInternetAddress(String aAddr)
			throws AddressException {

		// is smtp address skip profile lookup?
		if (aAddr.indexOf('@') > -1)
			return super.getInternetAddress(aAddr);

		// try to get email from syw profile
		try {
			aAddr = fetchEmail(aAddr);
			if (aAddr.indexOf('@') == -1) {
				System.out.println("[MartyMailPlugin] smtp mail address for '"
						+ aAddr + "' could not be resolved!");
				return null;
			}
		} catch (NamingException e) {
			// no valid email was found!
			System.out.println("[MartyMailPlugin] mail for '" + aAddr
					+ "' could not be resolved!");
			// e.printStackTrace();
			// avoid sending mail to this address!
			return null;
		}
		return super.getInternetAddress(aAddr);
	}

	/**
	 * This method lookups the emailadress for a given openid account through
	 * the ProfileService. If no profile is found or email is not valid the
	 * method throws a NamingException. This will lead into a situation where
	 * the super class tries to surround account with "" (hopefully)
	 * 
	 * 
	 * @param aOpenID
	 * @return
	 * @throws NamingException
	 */
	private String fetchEmail(String aOpenID) throws NamingException {

		ItemCollection itemColProfile = profileService
				.findProfileByName(aOpenID);

		if (itemColProfile == null)
			throw new NamingException(
					"[MartyMailPlugin] No Profile found for: " + aOpenID);

		String sEmail = itemColProfile.getItemValueString("txtEmail");

		// System.out.println("***** DEBUG ***** ProfileService - EmailLookup ="
		// + sEmail);

		if (sEmail != null && !"".equals(sEmail)) {
			if (sEmail.indexOf("http") > -1 || sEmail.indexOf("//") > -1)
				throw new NamingException(
						"[MartyMailPlugin] Invalid Email: ID=" + aOpenID
								+ " Email=" + sEmail);
			return sEmail;
		}

		// test if account contains protokoll information - this
		if (aOpenID.indexOf("http") > -1 || aOpenID.indexOf("//") > -1)
			throw new NamingException("[MartyMailPlugin] Invalid Email: ID="
					+ aOpenID);

		return aOpenID;
	}

}
