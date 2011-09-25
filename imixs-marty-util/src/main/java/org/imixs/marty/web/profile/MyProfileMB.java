/*******************************************************************************
d *  Imixs IX Workflow Technology
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
package org.imixs.marty.web.profile;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;

import org.imixs.marty.business.ProfileService;
import org.imixs.marty.business.ProjectService;
import org.imixs.marty.model.ModelVersionHandler;
import org.imixs.marty.util.LoginMB;
import org.imixs.marty.web.project.ProjectMB;
import org.imixs.marty.web.util.SetupMB;

import org.imixs.marty.web.workitem.WorkitemMB;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.faces.AbstractWorkflowController;
import org.richfaces.event.DropEvent;

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
public class MyProfileMB extends AbstractWorkflowController {

	/* Profile Service */
	@EJB
	ProfileService profileService;

	/* Project Service */
	@EJB
	ProjectService projectService;

	/* WorkItem Services */
	@EJB
	private org.imixs.marty.business.WorkitemService workitemService;

	public final static int MAX_PRIMARY_ENTRIES = 5;
	public final static int START_PROFILE_PROCESS_ID = 200;
	public final static int CREATE_PROFILE_ACTIVITY_ID = 5;
	public final static int UPDATE_PROJECT_ACTIVITY_ID = 10;

	private boolean profileLoaded = false;

	private ArrayList<ItemCollection> invitations = null;

	private String lastDropType; // stores the last drop type event

	private ModelVersionHandler modelVersionHandler = null;

	private LoginMB loginMB = null;
	private ProjectMB projectMB = null;
	private WorkitemMB workitemMB = null;
	private SetupMB setupMB = null;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	
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

			SetupMB systemSetupMB = (SetupMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "setupMB");
			if (systemSetupMB != null && !systemSetupMB.isSetupOk())
				systemSetupMB.doSetup(null);
			
			
			// determine user language and set Modelversion depending on
			// the selected user locale
			String sModelVersion = this.getModelVersionHandler()
					.getLatestSystemVersion(getLoginBean().getLocale());
			// terminate excecution if no system model ist defined
			if (sModelVersion==null) {
				throw new RuntimeException(" No System Model found!");
			}
			

			try {
				// try to load the profile for the current user
				workitemItemCollection = profileService.findProfileByName(null);
				if (workitemItemCollection == null) {

					// create new Profile for current user
					workitemItemCollection = profileService
							.createProfile(START_PROFILE_PROCESS_ID);

					workitemItemCollection.replaceItemValue("$modelversion",
							sModelVersion);

					// now set default values for locale
					workitemItemCollection.replaceItemValue("txtLocale",
							getLoginBean().getLocale());

					// set default launch page
					workitemItemCollection.replaceItemValue(
							"keyStartpage",
							getConfigBean().getWorkitem().getItemValueString(
									"DefaultPage"));

					Vector defaultProcessList = getConfigBean().getWorkitem()
							.getItemValue("defaultprojectprocesslist");

					// create a default project
					try {
						if (getConfigBean().getWorkitem().getItemValueBoolean(
								"CreateDefaultProject") == true)
							createUserDefaultProject(defaultProcessList);
					} catch (Exception ecp) {
						logger.warning("WARNING: Can not create default User Project");
						ecp.printStackTrace();
					}
					// process new profile...
					workitemItemCollection.replaceItemValue("$ActivityID",
							CREATE_PROFILE_ACTIVITY_ID);
					workitemItemCollection = profileService
							.processProfile(workitemItemCollection);

				} else {
					// Update Workitem to store last Login Time and logincount
					Calendar cal = Calendar.getInstance();

					Date datpenultimateLogin = workitemItemCollection
							.getItemValueDate("datLastLogin");
					if (datpenultimateLogin == null)
						datpenultimateLogin = cal.getTime();

					workitemItemCollection.replaceItemValue(
							"datpenultimateLogin", datpenultimateLogin);

					workitemItemCollection.replaceItemValue("datLastLogin",
							cal.getTime());
					int logins = workitemItemCollection
							.getItemValueInteger("numLoginCount");
					logins++;
					workitemItemCollection.replaceItemValue("numLoginCount",
							logins);
					workitemItemCollection = getEntityService().save(
							workitemItemCollection);

				}
				// set max History & log length
				workitemItemCollection.replaceItemValue(
						"numworkflowHistoryLength",
						getConfigBean().getWorkitem().getItemValueInteger(
								"MaxProfileHistoryLength"));
				workitemItemCollection.replaceItemValue(
						"numworkflowLogLength",
						getConfigBean().getWorkitem().getItemValueInteger(
								"MaxProfileHistoryLength"));

				this.setWorkitem(workitemItemCollection);

				profileLoaded = true;

				// Now reset current lokale
				updateLocale();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

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
	private void createUserDefaultProject(Vector<String> defaultProcectList)
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
			String sUserLanguage = workitemItemCollection
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

			// update primary project List
			workitemItemCollection.replaceItemValue("txtprimaryprojectlist",
					itemColProject.getItemValueString("$uniqueid"));
		}

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

			List<String> col = getModelService().getAllModelVersions();

			Vector modelDomains = workitemItemCollection
					.getItemValue("txtModelDomain");
			// add default model domain if empty or first entry is '' (could be
			// happen :-/)
			if (modelDomains.size() == 0
					|| (modelDomains.size() == 1 && "".equals(modelDomains
							.firstElement().toString())))
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
				logger.fine("===> modelVersionHandler adding:"
						+ latestversion);
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
	 * This Method acts as a ActionListener from the PanelMenuItem and Selects
	 * the current project and refreshes the Worklist Bean so wokitems of these
	 * project will be displayed after show_worklist
	 * 
	 * The method expects the param uniqueid which holds the $uniqueID from the
	 * selected Project.
	 * 
	 * @return
	 */
	public void doSwitchToPrimaryProject(ActionEvent event) {
		// search selected item..
		UIComponent component = event.getComponent();

		if (component instanceof org.richfaces.component.html.HtmlPanelMenuItem) {

			FacesContext context = FacesContext.getCurrentInstance();
			String sID = (String) context.getExternalContext()
					.getRequestParameterMap().get("uniqueid");
			logger.fine("------------ doSwitchToPrimaryProject $uniqueid:"
							+ sID);

			if (sID == null)
				return;

			// get current selected Project from primaryProjects menue
			/*
			 * for (ItemCollectionAdapter aProject : primaryProjects) { if
			 * (sID.equals(aProject.getItemCollection().getItemValueString(
			 * "$uniqueid"))) { // get WorklistMB instance // and reset worklist
			 * WorklistMB worklist = (WorklistMB) FacesContext
			 * .getCurrentInstance() .getApplication() .getELResolver()
			 * .getValue( FacesContext.getCurrentInstance() .getELContext(),
			 * null, "worklistMB");
			 * 
			 * worklist.doReset(event);
			 * 
			 * getProjectBean().setWorkitem(aProject.getItemCollection());
			 * break; } }
			 */
		}

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

		// provess workitem and verify txtname and txtusername
		try {
			// lowercase email to allow unique lookups
			String sEmail = workitemItemCollection
					.getItemValueString("txtEmail");
			sEmail = sEmail.toLowerCase();
			workitemItemCollection.replaceItemValue("txtEmail", sEmail);

			// Now update the Model version to the current User Setting
			// determine user language and set Modelversion depending on
			// the selected user locale
			String sModelVersion = this.getModelVersionHandler()
					.getLatestSystemVersion(
							workitemItemCollection
									.getItemValueString("txtLocale"));
			workitemItemCollection.replaceItemValue("$modelversion",
					sModelVersion);

			workitemItemCollection.replaceItemValue("$ActivityID", activityID);
			workitemItemCollection = profileService
					.processProfile(workitemItemCollection);

			setWorkitem(workitemItemCollection);

			// clear primary lists and invitations
			clearCache();

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

			String sMessage = rb.getString("displayname_error");
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

	
	 /* Comparator for ProjectNames
	 */
	class ProjectComparator implements Comparator<ItemCollection> {
		private final Collator collator;

		private final boolean ascending;

		public ProjectComparator(Locale locale, boolean ascending) {
			this.collator = Collator.getInstance(locale);
			this.ascending = ascending;
		}

		public int compare(ItemCollection a, ItemCollection b) {
			String nameA = a.getItemValueString("txtName");
			String nameB = b.getItemValueString("txtName");
			int result = this.collator.compare(nameA, nameB);
			if (!this.ascending) {
				result = -result;
			}
			return result;
		}

	}

	
	public void doEditWorkitem(ActionEvent event) {
		// get selection
		UIComponent component = event.getComponent();
		if (component instanceof org.richfaces.component.html.HtmlPanelMenuItem) {

			FacesContext context = FacesContext.getCurrentInstance();
			String sID = (String) context.getExternalContext()
					.getRequestParameterMap().get("uniqueid");

			if (sID == null)
				return;

			// load workitem
			try {
				ItemCollection aWorkitem = this.getWorkflowService()
						.getWorkItem(sID);
				// Now load corresponding Project
				ItemCollection aProject = projectService.findProject(aWorkitem
						.getItemValueString("$uniqueIDRef"));
				getProjectBean().setWorkitem(aProject);
				getProjectBean().setWorkitem(aProject);
				aWorkitem.removeItem("a4j:showhistory");
				aWorkitem.removeItem("a4j:showdetails");
				getWorkitemBean().setWorkitem(aWorkitem);

			} catch (Exception e) {
				// unable to load workitem!
				logger.warning("Unable to load workitem form TopIssue List!");
				e.printStackTrace();
			}

		}

	}

	/**
	 * This method handles DropEvents generated by the sidbar.xhtml
	 * 
	 * The method checks which kind of event (project, profile, workitem) was
	 * dragged and adds the $uniqueID to the corresponding field
	 * (txtPrimaryProjectList, txtPrimaryProfileList, txtPrimaryWorkitemList)
	 * 
	 * Maximum of MAX_PRIMARY_ENTRIES(10) Entries per Field are allowed.
	 */
	public void processDropEventAdd(DropEvent event) {
		String sUniqueID;
		String sDragType;
		String sAttributeName;

		try {

			// get $uniqueID of DragValue
			sUniqueID = event.getDragValue().toString();
			sDragType = event.getDragType();

			sAttributeName = "txtPrimary" + sDragType + "List";

			// get current Primary Project list
			Vector<String> vPrimaryProjectList = workitemItemCollection
					.getItemValue(sAttributeName);

			if (vPrimaryProjectList.indexOf(sUniqueID) > -1)
				return; // entry allready added to list

			// only 10 entryies are allowed
			if (vPrimaryProjectList.size() >= MAX_PRIMARY_ENTRIES)
				vPrimaryProjectList.add(MAX_PRIMARY_ENTRIES - 1, sUniqueID);
			else
				vPrimaryProjectList.add(sUniqueID);

			workitemItemCollection.replaceItemValue(sAttributeName,
					vPrimaryProjectList);

			workitemItemCollection = profileService
					.saveProfile(workitemItemCollection);
			this.setWorkitem(workitemItemCollection);

			// clear primary lists
			clearCache();

			lastDropType = sDragType;

			logger.fine("--- Added " + sDragType + " ID: " + sUniqueID);

		} catch (Exception e) {
			logger.warning("Unable to process Drop Event");
			e.printStackTrace();

		}
	}

	/**
	 * This method clears the cached project, workitems and invations lists The
	 * method is called after a doProcess and after DropEvents
	 */
	public void clearCache() {
		// primaryProjects = null;
		// primaryWorkitems = null;
		invitations = null;
	}

	/**
	 * This method handles DropEvents generated by the sidbar.xhtml
	 * 
	 * The Remove Method removes a item
	 * 
	 * The method checks which kind of event (project, profile, workitem) was
	 * draged and removes the $uniqueID from the corresponding field
	 * (txtPrimaryProjectList, txtPrimaryProfileList, txtPrimaryWorkitemList)
	 * 
	 * Maximum of MAX_PRIMARY_ENTRIES(10) Entries per Field are allowed.
	 */
	public void processDropEventRemove(DropEvent event) {
		String sUniqueID;
		String sDragType;
		String sAttributeName;

		try {

			// get $uniqueID of DragValue
			sUniqueID = event.getDragValue().toString();
			sDragType = event.getDragType();

			sDragType = sDragType.substring("remove_".length());

			sAttributeName = "txtPrimary" + sDragType + "List";

			// get current Primary Project list
			Vector<String> vPrimaryProjectList = workitemItemCollection
					.getItemValue(sAttributeName);

			if (vPrimaryProjectList.indexOf(sUniqueID) == -1)
				return; // entry did not exist

			vPrimaryProjectList.remove(sUniqueID);

			workitemItemCollection.replaceItemValue(sAttributeName,
					vPrimaryProjectList);
			workitemItemCollection = profileService
					.saveProfile(workitemItemCollection);
			this.setWorkitem(workitemItemCollection);

			// clear primary lists
			clearCache();
			lastDropType = sDragType;

			logger.fine("--- Added " + sDragType + " ID: " + sUniqueID);

		} catch (Exception e) {
			logger.warning("Unable to process Drop Event");
			e.printStackTrace();

		}

	}

	/**
	 * This method is for saving and processing a profile using the
	 * profileService EJB
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doProcessInvitation(ActionEvent event) throws Exception {
		// Activity ID raussuchen und in activityID speichern
		List children = event.getComponent().getChildren();
		String sActivityID = "";
		int activityID = 0;
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) instanceof UIParameter) {
				UIParameter currentParam = (UIParameter) children.get(i);
				if (currentParam.getName().equals("id")
						&& currentParam.getValue() != null) {
					sActivityID = currentParam.getValue().toString();
					activityID = Integer.parseInt(sActivityID);
					break;
				}
			}
		}

		ItemCollection currentSelection = null;
		// suche selektierte Zeile....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// Zeile gefunden
			currentSelection = (ItemCollection) ((UIData) parent)
					.getRowData();

			ItemCollection invitation = currentSelection;

			// if inivtation accepted add current username!
			int iProcessID = invitation.getItemValueInteger("$ProcessID");
			if (iProcessID == ProjectMB.PROJECT_INVITATION_PENDING
					&& activityID == ProjectMB.ACCEPT_PROJECT_INVITATION) {
				FacesContext context = FacesContext.getCurrentInstance();
				ExternalContext externalContext = context.getExternalContext();
				String remoteUser = externalContext.getRemoteUser();

				invitation.replaceItemValue("namAcceptedBy", remoteUser);

			}

			invitation.replaceItemValue("$ActivityID", activityID);
			getWorkflowService().processWorkItem(invitation);

			invitations = null;
			break;

		}

	}

	public List<ItemCollection> getInvitations() {
		if (invitations == null)
			loadInvitations();
		return invitations;

	}

	private void loadInvitations() {
		invitations = new ArrayList<ItemCollection>();
		try {
			logger.fine("----- Reload Inventations... ");
			String sEmail = workitemItemCollection
					.getItemValueString("txtEmail");
			String sQuery = "";
			sQuery = "SELECT wi from Entity as wi "
					+ " JOIN wi.textItems as r "
					+ " JOIN wi.integerItems as p "
					+ " WHERE wi.type = 'invitation'"
					+ " AND p.itemName = '$processid' AND p.itemValue = 310"
					+ " AND r.itemName = 'txtemail' and r.itemValue = '"
					+ sEmail + "' " + " order by wi.created desc";

			Collection<ItemCollection> col = getEntityService()
					.findAllEntities(sQuery, 0, -1);
			// endOfList = col.size() < count;
			for (ItemCollection aworkitem : col) {
				invitations.add(aworkitem);
			}
		} catch (Exception ee) {
			ee.printStackTrace();
		}

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
		String sLocale = workitemItemCollection.getItemValueString("txtLocale");
		if ("".equals(sLocale)) {
			sLocale = getLoginBean().getLocale();
			workitemItemCollection.replaceItemValue("txtLocale", sLocale);

		}
		// reset locale to update cookie
		getLoginBean().setLocale(sLocale);
		// set locale for context
		FacesContext.getCurrentInstance().getViewRoot()
				.setLocale(new Locale(sLocale));

	}

	private LoginMB getLoginBean() {
		if (loginMB == null)
			loginMB = (LoginMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "loginMB");
		return loginMB;
	}

	private SetupMB getConfigBean() {
		if (setupMB == null)
			setupMB = (SetupMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "setupMB");
		return setupMB;
	}

	public WorkitemMB getWorkitemBean() {

		if (workitemMB == null)
			workitemMB = (WorkitemMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "workitemMB");
		return workitemMB;
	}

	public ProjectMB getProjectBean() {
		if (projectMB == null)
			projectMB = (ProjectMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "projectMB");
		return projectMB;
	}

}
