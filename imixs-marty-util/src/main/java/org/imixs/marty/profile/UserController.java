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
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIParameter;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.imixs.marty.config.SetupController;
import org.imixs.marty.util.Cache;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;

/**
 * This backing beans handles the Profile of the current user. The user is
 * identified by its remote user name. This name is mapped to the attribute
 * txtname.
 * 
 * This bean creates automatically a new profile for the current user if no
 * profile yet exists!
 * 
 * The Bean also sets the properties 'skin' and 'locale' to the skinMB which is
 * used in JSF Pages to display pages using the current user settings.
 * 
 * Additional the Bean provides an ModelVersionHandler. This Class can be used
 * to dertermin model version spcific to the userprofile. therefor the
 * modelversion number is devided into a Domain, a Language and a Style as also
 * an internal Versionnummber.
 * 
 * e.g.: public-de-office-0.0.5
 * 
 * Where 'public' is the domain (associated to the user) 'de' is the user
 * language and 'office' is the name for the model. The ModelVersion handler is
 * used for ShareYourWorks. For example to provide a Model Tree Select Widget.
 * 
 * Imixs Office Workflow makes no use of the ModelVersionHandler
 * 
 * 
 * @see org.imixs.sywapps.web.profile.SkinMB
 * @author rsoika
 * 
 */
@Named("userController")
@SessionScoped
public class UserController  implements
		Serializable {

	private static final long serialVersionUID = 1L;
	private final String COOKIE_LOCALE = "imixs.workflow.locale";
	private ItemCollection workitem = null;

	final int MAX_CACHE_SIZE = 20;
	private Cache cache;
	
	
	
	@EJB
	private org.imixs.workflow.jee.ejb.ModelService modelService;

	@EJB
	private EntityService entityService;

	@EJB
	private WorkflowService workflowService;
	
	//@EJB
	//private org.imixs.marty.ejb.WorkitemService workitemService;

	public final static int MAX_PRIMARY_ENTRIES = 5;
	public final static int START_PROFILE_PROCESS_ID = 200;
	public final static int CREATE_PROFILE_ACTIVITY_ID = 5;
	public final static int UPDATE_PROJECT_ACTIVITY_ID = 10;

	private boolean profileLoaded = false;

	private String locale;

	
	@Inject
	private SetupController setupController = null;

	
	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	
	
	 
	public UserController() {
		super();
		cache = new Cache(MAX_CACHE_SIZE);
		
	}

	/**
	 * The init method is used to load a user profile or automatically create a
	 * new one if no profile for the user is available. A new Profile will be
	 * filled with default values. Additional the method creates a default
	 * project if the user is first time loged in.
	 * 
	 * 
	 * After the user values are read out from the database the bean updates the
	 * Bean skinMB which stores the values in a cookie. JSF Pages use the skinMB
	 * to determine the user settings skin and locale.
	 * 
	 * The Method also updates the Profile Workitem to indicate the last
	 * Successful Login of the current user
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

			// if SystemSetup is not yet completed - start System Setup now
			if (!setupController.isSetupOk())
				setupController.doSetup(null);

			// determine user language and set Modelversion depending on
			// the selected user locale
			String sModelVersion = "system-"+getLocale();
			// terminate excecution if no system model ist defined
			if (sModelVersion == null) {
				throw new RuntimeException(" No System Model found!");
			}

			try {
				// try to load the profile for the current user
				ItemCollection profile = findProfileByName(null);
				if (profile == null) {

					// create new Profile for current user
					profile = new ItemCollection();
					profile.replaceItemValue("type","profile");
					profile.replaceItemValue("$processID",START_PROFILE_PROCESS_ID);

					profile.replaceItemValue("$modelversion", sModelVersion);

					// now set default values for locale
					profile.replaceItemValue("txtLocale", getLocale());

		
				
					// process new profile...
					profile.replaceItemValue("$ActivityID",
							CREATE_PROFILE_ACTIVITY_ID);
					profile = workflowService.processWorkItem(profile);

				} else {
					// Update Workitem to store last Login Time and logincount
					Calendar cal = Calendar.getInstance();

					Date datpenultimateLogin = profile
							.getItemValueDate("datLastLogin");
					if (datpenultimateLogin == null)
						datpenultimateLogin = cal.getTime();

					profile.replaceItemValue("datpenultimateLogin",
							datpenultimateLogin);

					profile.replaceItemValue("datLastLogin", cal.getTime());
					int logins = profile.getItemValueInteger("numLoginCount");
					logins++;
					profile.replaceItemValue("numLoginCount", logins);

					// workitemItemCollection = getEntityService().save(
					// workitemItemCollection);

					// process profile to trigger ProfilePlugin (Invitations)...
					profile.replaceItemValue("$ActivityID",
							UPDATE_PROJECT_ACTIVITY_ID);
					profile = workflowService.processWorkItem(profile);

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

	public SetupController getSetupController() {
		return setupController;
	}

	public void setSetupController(SetupController setupMB) {
		this.setupController = setupMB;
	}

	
	/**
	 * returns the userPrincipal Name 
	 * @return
	 */
	public String getUserPrincipal() {
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		return externalContext.getUserPrincipal() != null ? externalContext
				.getUserPrincipal().toString() : null;
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
	 * This method returns the current user locale. If the user is not logged in the method 
	 * try to get the locale out from the cookie. If no cockie is set the method defaults to "en"
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
			String cookiePath = null;

			Cookie cookie[] = ((HttpServletRequest) facesContext
					.getExternalContext().getRequest()).getCookies();
			if (cookie != null && cookie.length > 0) {
				for (int i = 0; i < cookie.length; i++) {
					cookieName = cookie[i].getName();
					cookiePath = cookie[i].getPath();

					if (cookieName.equals(COOKIE_LOCALE)

					) {
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

			locale = verifyLocale(locale);
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
	 * This Method returns the users Model Domain. e.g. 'public'
	 * 
	 * @return
	 */
	public String getUserModelDomain() {
		// select Domain
		String sDomain = this.getWorkitem()
				.getItemValueString("txtModelDomain");
		if ("".equals(sDomain))
			sDomain = "public";
		return sDomain;
	}

	/**
	 * This method is for saving and processing a profile using the
	 * profileService EJB
	 * 
	 * The method changes the workflow step form 10 to 20 if: $processID=200 &&
	 * keyagb="true"
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doProcess(ActionEvent event) throws Exception {
		// Activity ID raussuchen und in activityID speichern
		List children = event.getComponent().getChildren();
		int activityID = -1;
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) instanceof UIParameter) {
				UIParameter currentParam = (UIParameter) children.get(i);
				if (currentParam.getName().equals("id")
						&& currentParam.getValue() != null) {
					// activityID = (Integer) currentParam.getValue();

					Object o = currentParam.getValue();
					activityID = Integer.parseInt(o.toString());
					break;
				}
			}
		}

		
		
		// Now reset current Skin
		updateLocale();
	}

	

	/**
	 * This method returns the username (displayname) for a useraccount
	 * 
	 * @param aName
	 * @return
	 */
	public String getUserName(String aAccount) {
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
	public String getEmail(String aAccount) {
		String[] userArray;
		// try to get name out from cache
		userArray = (String[]) cache.get(aAccount);
		if (userArray == null)
			userArray = lookupUser(aAccount);

		return userArray[1];
	}


	/*
	 * HELPER METHODS
	 */

	/**
	 * This method updates the current skin and location setting through the
	 * skinMB Therefore the method checks if skin and locale are still set in
	 * userprofile. If not than the method updates these attributes for the
	 * userprofile.
	 * 
	 * The mehtod always updates the cookies through the methods setLocale and
	 * setSkin form the Skin MB
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

	/**
	 * This method verifies a locale against the current skin configuration
	 * file: /configuration/skins.properties
	 * 
	 * if the locale is not found the method will default to the frist locale
	 * found in property file So a valid locale will be returned!
	 * 
	 * @param aSkin
	 * @return
	 */
	private String verifyLocale(String aLocale) {

		String sBestLocale = null;

		/* Test if current skin is available in the skin configuration */
		ResourceBundle r = ResourceBundle.getBundle("configuration.locale");

		Enumeration<String> enkeys = r.getKeys();
		while (enkeys.hasMoreElements()) {
			String sKey = enkeys.nextElement();

			// save first skin
			if (sBestLocale == null)
				sBestLocale = sKey;

			// test if current skin match...
			if (sKey.equals(aLocale))
				// yes! aSkin is valid!
				return aLocale;
		}

		// aSkin did not match anny of the available skinn in the skin
		// configuration
		// so return the first skin found in the configuration
		return sBestLocale;

	}

	
	/**
	 * this class performes a EJB Lookup for the corresponding userprofile. The
	 * method stores the username and his email into a string array. So either
	 * the username or the email address will be cached in a single object.
	 * 
	 * @param aName
	 * @return array of username and string
	 */
	private String[] lookupUser(String aName) {
		String[] array = new String[2];

		// String sUserName = null;
		ItemCollection profile = findProfileByName(aName);
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
		ItemCollection profile = findProfileByUserName(aUserName);
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
	 * This method returns a profile ItemCollection for a specified account
	 * name. if no name is supported the remote user name will by used to find
	 * the profile The method returns null if no Profile for this name was found
	 * 
	 * @param aname
	 * @return
	 */
	private ItemCollection findProfileByName(String aname) {

		if (aname == null)
			aname = getUserPrincipal();

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
	private ItemCollection findProfileByUserName(String aname) {
		if (aname == null)
			aname = getUserPrincipal();

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
	

}