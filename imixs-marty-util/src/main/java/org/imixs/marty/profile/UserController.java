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

package org.imixs.marty.profile;

import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.imixs.marty.config.SetupController;
import org.imixs.marty.ejb.ProfileService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.jee.faces.util.LoginController;

/**
 * This backing beans handles the Profile for the current user and provides a
 * session based cache for usernames and email addresses.
 * 
 * The Users name and email address is stored in the user profile entity. The
 * user is identified by its principal user name. This name is mapped to the
 * attribute txtname.
 * 
 * This bean creates automatically a new profile for the current user if no
 * profile yet exists!
 * 
 * The Bean also provides the user 'locale' which is used in JSF Pages to
 * display pages using the current user settings.
 * 
 * @author rsoika
 * 
 */
@Named("userController")
@SessionScoped
public class UserController implements Serializable {

	private static final long serialVersionUID = 1L;
	private final String COOKIE_LOCALE = "imixs.workflow.locale";
	private ItemCollection workitem = null;



	@EJB
	private ProfileService profileService;
 
	@EJB
	private EntityService entityService;

	@EJB
	private WorkflowService workflowService;

	public final static int MAX_PRIMARY_ENTRIES = 5;
	public final static int START_PROFILE_PROCESS_ID = 200;
	public final static int CREATE_PROFILE_ACTIVITY_ID = 5;
	public final static int UPDATE_PROJECT_ACTIVITY_ID = 10;

	private boolean profileLoaded = false;

	private String locale;

	@Inject
	private SetupController setupController;

	@Inject
	private LoginController loginController;

	private static Logger logger = Logger.getLogger(UserController.class.getName());

	public UserController() {
		super();
	

	}

	/**
	 * The init method is used to load a user profile or automatically create a
	 * new one if no profile for the user is available. A new Profile will be
	 * filled with default values.
	 * 
	 * 
	 * The method also tests if the System Setup is completed. This if for
	 * situations where the method is called the very first time after the
	 * system was deployed. The SystemSetup can load a default model if a
	 * configuration in model.properties was defined.
	 * 
	 * @throws Exception
	 */
	@PostConstruct
	public void init() throws Exception {
		// Automatically create profile if no profile exists yet

		if (!profileLoaded) {
			// determine user language and set Modelversion depending on
			// the selected user locale
			String sModelVersion = "system-" + getLocale();

			try {
				// try to load the profile for the current user
				ItemCollection profile = profileService.findProfileById(null);
				if (profile == null) {

					// create new Profile for current user
					profile = new ItemCollection();
					profile.replaceItemValue("type", "profile");
					profile.replaceItemValue("$processID",
							START_PROFILE_PROCESS_ID);

					profile.replaceItemValue("$modelversion", sModelVersion);

					// now set default values for locale
					profile.replaceItemValue("txtLocale", getLocale());

					// process new profile...
					profile.replaceItemValue("$ActivityID",
							CREATE_PROFILE_ACTIVITY_ID);
					profile = workflowService.processWorkItem(profile);
					logger.info("New Profile created ");

				} else {
					// no op
					// in earlier versions teh datlastLogin and numLoginCoutn
					// property was set

				}
				// set max History & log length
				profile.replaceItemValue(
						"numworkflowHistoryLength",
						setupController.getWorkitem().getItemValueInteger(
								"MaxProfileHistoryLength"));
				profile.replaceItemValue(
						"numworkflowLogLength",
						setupController.getWorkitem().getItemValueInteger(
								"MaxProfileHistoryLength"));

				this.setWorkitem(profile);

				profileLoaded = true;

				// Now reset current lokale
				updateLocale();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public ItemCollection getWorkitem() {
		if (workitem == null)
			workitem = new ItemCollection();
		return workitem;
	}

	public void setWorkitem(ItemCollection aworkitem) {
		this.workitem = aworkitem;
	}

	/**
	 * This method returns the current user locale. If the user is not logged in
	 * the method try to get the locale out from the cookie. If no cockie is set
	 * the method defaults to "en"
	 * 
	 * @return - ISO Locale format
	 */
	public String getLocale() {
		// if no locale is set try to get it from cookie or set default
		if (locale == null) {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			HttpServletRequest request = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();

			String cookieName = null;

			Cookie cookie[] = ((HttpServletRequest) facesContext
					.getExternalContext().getRequest()).getCookies();
			if (cookie != null && cookie.length > 0) {
				for (int i = 0; i < cookie.length; i++) {
					cookieName = cookie[i].getName();
					if (cookieName.equals(COOKIE_LOCALE)) {
						locale = cookie[i].getValue();
						break;
					}

				}
			}

			// still no value found? - default to "en"
			if (locale == null || "".equals(locale) || "null".equals(locale)) {
				Locale ldefault = request.getLocale();
				if (ldefault != null)
					locale = ldefault.toString();
				else
					locale = "en";
			}

		}
		return locale;
	}

	public void setLocale(String locale) {
		if (locale == null)
			locale = "en";
		this.locale = locale;

		// update cookie
		HttpServletResponse response = (HttpServletResponse) FacesContext
				.getCurrentInstance().getExternalContext().getResponse();
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		Cookie cookieLocale = new Cookie(COOKIE_LOCALE, locale);
		cookieLocale.setPath(request.getContextPath());

		// 30 days
		cookieLocale.setMaxAge(2592000);
		response.addCookie(cookieLocale);

	}

	/**
	 * This method returns the username (displayname) for a useraccount
	 * 
	 * @param aName
	 * @return
	 */
	public String getUserName(String aAccount) {
		
		ItemCollection userProfile=profileService.findProfileById(aAccount);
		if (userProfile!=null)
			return userProfile.getItemValueString("txtuserName");
		else
			return null;
	}

	/**
	 * This method returns the email for a useraccount
	 * 
	 * @param aName
	 * @return
	 */
	public String getEmail(String aAccount) {
		ItemCollection userProfile=profileService.findProfileById(aAccount);
		if (userProfile!=null)
			return userProfile.getItemValueString("txtemail");
		else
			return null;
	}

	/*
	 * HELPER METHODS
	 */

	
	/**
	 * This method updates user locale to the faces context.
	 * 
	 */
	private void updateLocale() throws Exception {

		// Verify if Locale is available in profile
		String sLocale = getWorkitem().getItemValueString("txtLocale");
		if ("".equals(sLocale)) {
			sLocale = getLocale();
			getWorkitem().replaceItemValue("txtLocale", sLocale);

		}
		// reset locale to update cookie
		setLocale(sLocale);
		// set locale for context
		FacesContext.getCurrentInstance().getViewRoot()
				.setLocale(new Locale(sLocale));

	}


	


}
