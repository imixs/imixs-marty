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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.imixs.marty.ejb.ProfileService;
import org.imixs.marty.util.ErrorHandler;
import org.imixs.marty.workflow.WorkflowEvent;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.jee.faces.util.LoginController;
import org.imixs.workflow.jee.util.PropertyService;

/**
 * This backing beans handles the Profile entity for the current user and
 * provides a application scoped access to all other profiles through the
 * ProfileService EJB.
 * 
 * A new user profile will be created automatically if no profile yet exists!
 * The user is identified by its principal user name. This name is mapped to the
 * attribute txtname.
 * 
 * The UserController provides the user 'locale' and 'language' which is used in
 * JSF Pages to display pages using the current user settings.
 * 
 * With the methods mark() and unmark() workitems can be added into the users
 * profile favorite list.
 * 
 * @author rsoika
 * 
 */
@Named("userController")
@SessionScoped
public class UserController implements Serializable {

	public final static int MAX_PRIMARY_ENTRIES = 5;
	public final static int START_PROFILE_PROCESS_ID = 200;
	public final static int CREATE_PROFILE_ACTIVITY_ID = 5;
	public final static int UPDATE_PROJECT_ACTIVITY_ID = 10;
	public final static String DEFAULT_LOCALE = "de_DE";
	public final static String COOKIE_LOCALE = "imixs.workflow.locale";

	@EJB
	protected ProfileService profileService;

	@EJB
	protected PropertyService propertyService;

	@EJB
	protected EntityService entityService;

	@EJB
	protected WorkflowService workflowService;

	@Inject
	protected LoginController loginController;

	private static final long serialVersionUID = 1L;
	private ItemCollection workitem = null;
	private boolean profileLoaded = false;
	private Locale locale;

	private static Logger logger = Logger.getLogger(UserController.class
			.getName());

	public UserController() {
		super();
	}

	/**
	 * The init method is used to load a user profile or automatically create a
	 * new one if no profile for the user is available. A new Profile will be
	 * filled with default values.
	 * 
	 * This method did not use the internal cache of the ProfileService to
	 * lookup the user profile, to make sure that the entity is uptodate when a
	 * user logs in.
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 */
	@PostConstruct
	public void init() throws AccessDeniedException, ProcessingErrorException {

		// test user is logged-in and automatically create profile if no profile
		// exists yet
		if (this.loginController.isAuthenticated() && !profileLoaded) {

			// determine user language and set Modelversion depending on
			// the selected user locale
			String sModelVersion = "system-" + getLocale().getLanguage();
			// try to load the profile for the current user
			ItemCollection profile = profileService
					.lookupProfileById(loginController.getUserPrincipal());
			if (profile == null) {
				// create new Profile for current user
				profile = new ItemCollection();
				profile.replaceItemValue("type", "profile");
				profile.replaceItemValue("$processID", START_PROFILE_PROCESS_ID);
				profile.replaceItemValue("$modelversion", sModelVersion);
				profile.replaceItemValue("txtLocale", getLocale());
				// set default group
				profile.replaceItemValue("txtgroups", "IMIXS-WORKFLOW-Author");
				// process new profile...
				profile.replaceItemValue("$ActivityID",
						CREATE_PROFILE_ACTIVITY_ID);
				try {
					profile = workflowService.processWorkItem(profile);
				} catch (PluginException e) {
					logger.severe("[UserController] unable to process new profile entity!");
					throw new ProcessingErrorException(
							UserController.class.getName(),
							ProcessingErrorException.INVALID_WORKITEM,
							" unable to process new profile entity!", e);
				}
				logger.info("New Profile created ");

			} else {
				// check if profile.autoProcessOnLogin is defined

				String sAutoProcessID = propertyService.getProperties()
						.getProperty("profile.autoProcessOnLogin");

				logger.fine("[UserController] profile.autoProcessOnLogin="
						+ sAutoProcessID);

				try {
					if (sAutoProcessID != null) {
						int iActiviyID = Integer.valueOf(sAutoProcessID);
						profile.replaceItemValue("$ActivityID", iActiviyID);
						logger.fine("[UserController] autoprocess profile with autoProcessOnLogin="
								+ iActiviyID);
						try {
							profile = workflowService.processWorkItem(profile);
						} catch (PluginException e) {
							logger.severe("[UserController] unable to process new profile entity!");
							throw new ProcessingErrorException(
									UserController.class.getName(),
									ProcessingErrorException.INVALID_WORKITEM,
									" unable to process new profile entity!", e);
						}

					}
				} catch (NumberFormatException nfe) {
					logger.warning("[UserController] unable to autoprocess profile with autoProcessOnLogin="
							+ sAutoProcessID);
				}

			}

			this.setWorkitem(profile);

			profileLoaded = true;

			// Now reset current locale based on the profile information
			updateLocaleFromProfile();

		}

	}

	/**
	 * This method returns the current users userprofile entity. The method
	 * verifies if the profile was yet loaded. if not the method tries to
	 * initiate the profile - see method init();
	 * 
	 * @return
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 */
	public ItemCollection getWorkitem() throws AccessDeniedException,
			ProcessingErrorException {
		// test if current users profile was loaded
		if (!profileLoaded)
			init();
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
	 * the method defaults to "de_DE"
	 * 
	 * @return - ISO Locale format
	 */
	public Locale getLocale() {
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
						String sLocale = cookie[i].getValue();
						if (sLocale != null && !"".equals(sLocale)) {

							// split locale
							StringTokenizer stLocale = new StringTokenizer(
									sLocale, "_");
							if (stLocale.countTokens() == 1) {
								// only language variant
								String sLang = stLocale.nextToken();
								String sCount = sLang.toUpperCase();
								locale = new Locale(sLang, sCount);
							} else {
								// language and country
								String sLang = stLocale.nextToken();
								String sCount = stLocale.nextToken();
								locale = new Locale(sLang, sCount);
							}

						}
						break;
					}

				}
			}

			// still no value found? - default to "en"
			if (locale == null || "".equals(locale.getLanguage())) {
				Locale ldefault = request.getLocale();
				if (ldefault != null) {
					locale = ldefault;
				} else {
					locale = new Locale(DEFAULT_LOCALE);
				}
			}

		}
		return locale;
	}

	public void setLocale(Locale alocale) {
		if (alocale == null || "".equals(alocale))
			locale = new Locale(DEFAULT_LOCALE);
		else
			this.locale = alocale;

		// update cookie
		HttpServletResponse response = (HttpServletResponse) FacesContext
				.getCurrentInstance().getExternalContext().getResponse();
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		Cookie cookieLocale = new Cookie(COOKIE_LOCALE, locale.toString());
		cookieLocale.setPath(request.getContextPath());

		// 30 days
		cookieLocale.setMaxAge(2592000);
		response.addCookie(cookieLocale);

	}

	/**
	 * returns the user language
	 * 
	 * @return
	 */
	public String getLanguage() {
		return getLocale().getLanguage();
	}

	/**
	 * This method returns a cached cloned version of a user profile for a given
	 * useraccount
	 * 
	 * @param aName
	 * @return
	 */
	public ItemCollection getProfile(String aAccount) {
		return profileService.findProfileById(aAccount);
	}

	/**
	 * This method returns the username (displayname) for a useraccount.
	 * If no Username is set in the profile then we return the useraccount.
	 * 
	 * @param aName
	 * @return
	 */
	public String getUserName(String aAccount) {

		ItemCollection userProfile = profileService.findProfileById(aAccount);
		if (userProfile != null) {
			String value = userProfile.getItemValueString("txtuserName");
			if (!value.isEmpty())
				return value;
			else
				return aAccount;

		} else {
			return aAccount;
		}
	}

	/**
	 * This method returns the email for a useraccount
	 * 
	 * @param aName
	 * @return
	 */
	public String getEmail(String aAccount) {
		ItemCollection userProfile = profileService.findProfileById(aAccount);
		if (userProfile != null)
			return userProfile.getItemValueString("txtemail");
		else
			return null;
	}

	/**
	 * This method processes the current userprofile and returns an action
	 * result. The method expects that the current workItem provides a valid
	 * $ActiviytID.
	 * 
	 * The method evaluates the default property 'txtworkflowResultmessage' from
	 * the model as an action result.
	 * 
	 * @return action result
	 * @throws AccessDeniedException
	 * @throws
	 */
	public String process() throws AccessDeniedException,
			ProcessingErrorException {

		// process workItem now...
		try {
			workitem = this.workflowService.processWorkItem(workitem);
		} catch (PluginException e) {
			// add a new FacesMessage into the FacesContext
			ErrorHandler.handlePluginException(e);
		}

		// get default workflowResult message
		String action = workitem.getItemValueString("txtworkflowresultmessage");
		return ("".equals(action) ? null : action);

	}

	/**
	 * WorkflowEvent listener listens to WORKITEM events to reset the current
	 * username workitem if processed. The method also updates the user Locale
	 * 
	 * @param workflowEvent
	 * 
	 **/
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent) {
		if (workflowEvent == null || workflowEvent.getWorkitem() == null) {
			return;
		}

		// skip if not a profile...
		if (!workflowEvent.getWorkitem().getItemValueString("type")
				.equals("profile"))
			return;

		// discared cached user profile and update locale
		if (WorkflowEvent.WORKITEM_AFTER_PROCESS == workflowEvent
				.getEventType()) {

			// check if current user profile was processed....
			String sName = workflowEvent.getWorkitem().getItemValueString(
					"txtName");
			if (sName.equals(this.getWorkitem().getItemValueString("txtName"))) {
				logger.info("[UserController] reload current user profile");
				setWorkitem(workflowEvent.getWorkitem());

				// update locale
				updateLocaleFromProfile();
			}

		}

	}

	/**
	 * Returns true if the uniqueid is stored in the profile favorites
	 * 
	 * @param id
	 * @return
	 */
	public boolean isFavorite(String id) {
		return getFavoriteIds().contains(id);
	}

	/**
	 * Returns a list with all uniqueids stored in the profile favorites
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getFavoriteIds() {
		if (getWorkitem() == null)
			return new ArrayList<String>();
		return getWorkitem().getItemValue("txtWorkitemRef");
	}

	public void addFavorite(String id) {
		if (getWorkitem() == null)
			return;

		List<String> list = getFavoriteIds();
		// we expect that the id is in the list-..
		if (!list.contains(id)) {
			logger.fine("[UserController] add WorkitemRef:" + id);
			list.add(id);
			workitem.replaceItemValue("txtWorkitemRef", list);
			workitem = entityService.save(workitem);
		}
	}

	public void removeFavorite(String id) {
		if (getWorkitem() == null)
			return;

		List<String> list = getFavoriteIds();
		// we expect that the id is in the list-..
		if (list.contains(id)) {
			logger.fine("[UserController] remove WorkitemRef:" + id);
			list.remove(id);
			workitem.replaceItemValue("txtWorkitemRef", list);
			workitem = entityService.save(workitem);
		}
	}

	/**
	 * This method returns all workitems sotred in the users profile
	 * txtWorkitemRef
	 * 
	 * @param refId - a UnqiueIdRef as optional filter criterium
	 * @return
	 */
	public List<ItemCollection> getFavorites(String refId) {

		List<String> favorites = getFavoriteIds();
		if (favorites.size() <= 0)
			return new ArrayList<ItemCollection>();

		// verify optional refId param
		if (refId!=null && ("-".equals(refId) || refId.isEmpty()) ) {
			refId=null;
		}

		// create a JPQL statement....
		
		
		// create IN list
		String inStatement = "";
		for (String aID : favorites) {
			inStatement = inStatement + "'" + aID + "',";
		}
		// cut last ,
		inStatement = inStatement.substring(0, inStatement.length() - 1);

		String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";
		
		if (refId!=null) {
			sQuery+= "	 JOIN wi.textItems as ref ";
		}
		
		sQuery += " WHERE wi.type IN ('workitem','workitemarchive')";
		

		if (refId!=null) {
			sQuery+= "	AND ref.itemName = '$uniqueidref' AND ref.itemValue = '"
						+ refId + "'";
		}
		
		
		sQuery += " AND wi.id IN (" + inStatement + ")";
		sQuery += " ORDER BY wi.modified DESC";

		return workflowService.getEntityService()
				.findAllEntities(sQuery, 0, -1);

	}

	public List<ItemCollection> getFavorites() {
		return getFavorites(null);
		
	}
	/*
	 * HELPER METHODS
	 */

	/**
	 * This method updates user locale stored in the user profile entity to the
	 * faces context.
	 * 
	 * @throws ProcessingErrorException
	 * @throws AccessDeniedException
	 * 
	 */
	private void updateLocaleFromProfile() throws AccessDeniedException,
			ProcessingErrorException {

		Locale profileLocale = null;

		// Verify if Locale is available in profile
		String sLocale = getWorkitem().getItemValueString("txtLocale");
		if ("".equals(sLocale)) {
			// get default value
			profileLocale = getLocale();
			getWorkitem().replaceItemValue("txtLocale",
					profileLocale.toString());
		} else {

			if (sLocale.indexOf('_') > -1) {
				String language = sLocale.substring(0, sLocale.indexOf('_'));
				String country = sLocale.substring(sLocale.indexOf('_') + 1);
				profileLocale = new Locale(language, country);
			} else {
				profileLocale = new Locale(sLocale);
			}
		}

		logger.info("[UserController] update user locale: " + profileLocale);
		// reset locale to update cookie
		setLocale(profileLocale);
		// set locale for context
		FacesContext.getCurrentInstance().getViewRoot()
				.setLocale(profileLocale);

	}

}
