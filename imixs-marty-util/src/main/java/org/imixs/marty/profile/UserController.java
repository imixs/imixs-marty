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
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.imixs.marty.config.SetupMB;
import org.imixs.marty.ejb.ProfileService;
import org.imixs.marty.ejb.ProjectService;
import org.imixs.marty.model.ModelVersionHandler;
import org.imixs.marty.project.ProjectMB;
import org.imixs.workflow.ItemCollection;

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

	@EJB
	private org.imixs.workflow.jee.ejb.ModelService modelService;

	
	/* Profile Service */
	@EJB
	private ProfileService profileService;

	/* Project Service */
	@EJB
	private ProjectService projectService;

	/* WorkItem Services */
	@EJB
	private org.imixs.marty.ejb.WorkitemService workitemService;

	public final static int MAX_PRIMARY_ENTRIES = 5;
	public final static int START_PROFILE_PROCESS_ID = 200;
	public final static int CREATE_PROFILE_ACTIVITY_ID = 5;
	public final static int UPDATE_PROJECT_ACTIVITY_ID = 10;

	private boolean profileLoaded = false;

	private String locale;

	private ModelVersionHandler modelVersionHandler = null;

	@Inject
	private SetupMB setupMB = null;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	
	
	
	public UserController() {
		super();
		// empty constructor!
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
			if (!setupMB.isSetupOk())
				setupMB.doSetup(null);

			// determine user language and set Modelversion depending on
			// the selected user locale
			String sModelVersion = this.getModelVersionHandler()
					.getLatestSystemVersion(getLocale());
			// terminate excecution if no system model ist defined
			if (sModelVersion == null) {
				throw new RuntimeException(" No System Model found!");
			}

			try {
				// try to load the profile for the current user
				ItemCollection profile = profileService.findProfileByName(null);
				if (profile == null) {

					// create new Profile for current user
					profile = profileService
							.createProfile(START_PROFILE_PROCESS_ID);

					profile.replaceItemValue("$modelversion", sModelVersion);

					// now set default values for locale
					profile.replaceItemValue("txtLocale", getLocale());

					// set default launch page
					profile.replaceItemValue("keyStartpage", setupMB
							.getWorkitem().getItemValueString("DefaultPage"));

					List defaultProcessList = setupMB.getWorkitem()
							.getItemValue("defaultprojectprocesslist");

					// create a default project
					try {
						if (setupMB.getWorkitem().getItemValueBoolean(
								"CreateDefaultProject") == true)
							createUserDefaultProject(defaultProcessList);
					} catch (Exception ecp) {
						logger.warning("WARNING: Can not create default User Project");
						ecp.printStackTrace();
					}
					// process new profile...
					profile.replaceItemValue("$ActivityID",
							CREATE_PROFILE_ACTIVITY_ID);
					profile = profileService.processProfile(profile);

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
					profile = profileService.processProfile(profile);

				}
				// set max History & log length
				profile.replaceItemValue(
						"numworkflowHistoryLength",
						setupMB.getWorkitem().getItemValueInteger(
								"MaxProfileHistoryLength"));
				profile.replaceItemValue(
						"numworkflowLogLength",
						setupMB.getWorkitem().getItemValueInteger(
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

	public SetupMB getSetupMB() {
		return setupMB;
	}

	public void setSetupMB(SetupMB setupMB) {
		this.setupMB = setupMB;
	}

	/**
	 * This method creates a UserDefault Project. The Method is called only if a
	 * user logged in first time and a new profile was created successfully
	 * Note: This method may not call getProjectMB() because a circular
	 * exception will be thrown!!!
	 * 
	 * <p>
	 * The default process model versions are now scanned for the string
	 * '-LOCALE-'. If this string is found it will be replaced with the current
	 * user locale. So it is possible to provide default process list
	 * indenpendent from a fixed locale
	 * 
	 * public-LOCALE-standard-0.0.2|1000
	 * 
	 * 
	 * @throws Exception
	 * 
	 */
	private void createUserDefaultProject(List<String> defaultProcectList)
			throws Exception {
		// test if project list is empty...
		List<ItemCollection> projectList = projectService
				.findAllProjectsByOwner(0, 1);
		if (projectList.size() == 0) {
			// create default project
			ItemCollection itemColProject = projectService
					.createProject(ProjectMB.START_PROJECT_PROCESS_ID);

			// determine user language and set Modelversion depending on the
			// selected user locale
			Locale userLocale = FacesContext.getCurrentInstance().getViewRoot()
					.getLocale();
			String sUserLanguage = getWorkitem()
					.getItemValueString("txtLocale");

			// Set System Model Version for this Project to user Language
			String sModelVersion = getModelVersionHandler()
					.getLatestSystemVersion(sUserLanguage);
			itemColProject.replaceItemValue("$modelversion", sModelVersion);
			itemColProject.replaceItemValue("txtModelLanguage", userLocale);

			// add current user to team and owner lists
			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext externalContext = context.getExternalContext();
			String remoteUser = externalContext.getRemoteUser();
			itemColProject.replaceItemValue("namTeam", remoteUser);
			itemColProject.replaceItemValue("namOwner", remoteUser);

			// update project name and process infos
			String sDefaultProjectName = "My Project";
			String sDefaultProjectDesc = "My first ShareYourWork project";
			try {
				ResourceBundle rb = null;
				if (userLocale != null)
					rb = ResourceBundle.getBundle("bundle.profile", userLocale);
				else
					rb = ResourceBundle.getBundle("bundle.profile");

				sDefaultProjectName = rb.getString("default_project_name");
				sDefaultProjectDesc = rb
						.getString("default_project_description");
			} catch (Exception e) {
				logger.warning("Warning: can not load ressource bundle profile");
				e.printStackTrace();
			}

			itemColProject.replaceItemValue("txtProjectName",
					sDefaultProjectName);
			itemColProject.replaceItemValue("txtdescription",
					sDefaultProjectDesc);

			itemColProject.replaceItemValue("$ActivityID",
					UPDATE_PROJECT_ACTIVITY_ID);

			// the default model versions are now scanned for the string
			// '-LOCALE-'. If this string is found it will be replaced
			// with the current user locale.
			Vector vnewProcessList = new Vector();
			for (String sversion : defaultProcectList) {
				sversion = sversion.replace("-LOCALE-",
						"-" + userLocale.getLanguage() + "-");
				vnewProcessList.add(sversion);
			}
			itemColProject.replaceItemValue("txtprocesslist", vnewProcessList);

			itemColProject = projectService.processProject(itemColProject);

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
	 * This getter method trys to get the locale out from the cookie if
	 * available. Otherwise it will default to "en"
	 * 
	 * @return
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
	 * Returns a new modelVersionHandler. The modelVersionHandler supports all
	 * process Types and Languages available for the current user.
	 * 
	 * The Methods verifies the internal Version number of each model and adds
	 * only the latest version in a domain/language.
	 * 
	 * The domain is read from the user Profile. If no txtModelDomain is set the
	 * domain defaults to 'public'.
	 * 
	 * ======= sywapps 2.0 ======= A model version can also be added if no
	 * domain-lang-style schema is provided. In this case the
	 * ModelVersionHandler provides the latest model version (excluding the
	 * system model version) in the property latestDefaultModel() The latest
	 * Default model is used by the imixs office workflwo where no user domain
	 * needs to be configured.
	 * 
	 * 
	 * 
	 * @return
	 * @throws Exception
	 */
	public ModelVersionHandler getModelVersionHandler() throws Exception {
		HashMap<String, String> latestModelVersions = new HashMap<String, String>();

		// initialize modelVersionHandler the first time...
		if (modelVersionHandler == null) {
			modelVersionHandler = new ModelVersionHandler();

			// add available Models...

			List<String> col = modelService.getAllModelVersions();

			List modelDomains = getWorkitem().getItemValue("txtModelDomain");
			// add default model domain if empty or first entry is '' (could be
			// happen :-/)
			if (modelDomains.size() == 0
					|| (modelDomains.size() == 1 && "".equals(modelDomains.get(
							0).toString())))
				modelDomains.add("public");

			for (String sversion : col) {
				/*
				 * add ModelVersion only if the modelDomain is corresponding to
				 * the user ProfileSelection (txtModelDomain) or if it is a
				 * system model
				 */
				String sModelDomain = sversion.substring(0,
						sversion.indexOf("-"));

				if (modelDomains.indexOf(sModelDomain) > -1
						|| "system".equals(sModelDomain)) {

					// test if this modeltype is still stored in a older version
					// before..
					String sModelType = sversion.substring(0,
							sversion.lastIndexOf("-"));
					String lastVersion = (String) latestModelVersions
							.get(sModelType);
					if (lastVersion == null
							|| lastVersion.compareTo(sversion) < 0)
						latestModelVersions.put(sModelType, sversion);

				}
			}

			// Now add the latest ModelVersion to the modelVersionHandler
			for (String latestversion : latestModelVersions.values()) {
				logger.fine("===> modelVersionHandler adding:" + latestversion);
				modelVersionHandler.addVersion(latestversion);
			}
		}

		return modelVersionHandler;

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

		// validate workitem and verify txtname and txtusername
		try {
			ItemCollection profile = getWorkitem();
			// lowercase email to allow unique lookups
			String sEmail = profile.getItemValueString("txtEmail");
			sEmail = sEmail.toLowerCase();
			profile.replaceItemValue("txtEmail", sEmail);

			// Now update the Model version to the current User Setting
			// determine user language and set Modelversion depending on
			// the selected user locale
			String sModelVersion = this.getModelVersionHandler()
					.getLatestSystemVersion(
							profile.getItemValueString("txtLocale"));
			profile.replaceItemValue("$modelversion", sModelVersion);

			profile.replaceItemValue("$ActivityID", activityID);
			profile = profileService.processProfile(profile);

			setWorkitem(profile);

			
		} catch (Exception ee) {

			// Generate Error message
			FacesContext context = FacesContext.getCurrentInstance();
			UIViewRoot viewRoot = FacesContext.getCurrentInstance()
					.getViewRoot();
			Locale locale = viewRoot.getLocale();
			ResourceBundle rb = null;
			if (locale != null)
				rb = ResourceBundle.getBundle("bundle.profile", locale);
			else
				rb = ResourceBundle.getBundle("bundle.profile");

			// depending on the messeage string we fetch the message
			String sMessage = "";
			if (ee.getMessage().contains("txtemail"))
				sMessage = rb.getString("duplicateemail_error");
			else
				sMessage = rb.getString("displayname_error");
			FacesMessage message = new FacesMessage("* ", sMessage);
			// add two messages to support the standard profile_form and also
			// the verify Profile form
			// should be optimized some times....
			context.addMessage("myprofile_form_id:displayname_id", message);
			context.addMessage(
					"verify_profile_id:verify_profile_form:displayname_id",
					message);

			throw new ValidatorException(message);
		}
		// Now reset current Skin
		updateLocale();
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

}
