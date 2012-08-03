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

package org.imixs.marty.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIInput;
import javax.faces.component.UIParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.config.SetupController;
import org.imixs.marty.ejb.ProfileService;
import org.imixs.marty.ejb.ProjectService;
import org.imixs.marty.profile.UserController;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.ModelService;
import org.imixs.workflow.jee.faces.workitem.DataController;


@Named("projectController") 
@SessionScoped
public class ProjectController extends org.imixs.workflow.jee.faces.workitem.WorkflowController implements Serializable {
 
	private static final long serialVersionUID = 1L;

	public final static int START_PROJECT_PROCESS_ID = 100;

	/* Project Service */
	@EJB
	ProjectService projectService;
	/* Profile Service */
	@EJB
	ProfileService profileService;
	
	@EJB
	EntityService entityService;

	@EJB
	ModelService modelService;
 

	@Inject
	private UserController userController = null;
	
	
	
	@Inject
	private SetupController setupController = null;
	
	
	  
	
	 
	private ArrayList<ItemCollection> team = null;
	private ArrayList<ItemCollection> projectSiblingList = null;

	
	private Collection<ProjectListener> projectListeners = new ArrayList<ProjectListener>();
	List<ItemCollection> projects;
	private static Logger logger = Logger.getLogger("org.imixs.marty");

	private StartProcessCache startProcessList;
	private SubProcessCache subProcessList;
	private boolean selectMainProjects = false;
	private ArrayList<ItemCollection> startProjects = null;

	private ArrayList<SelectItem> myProjectSelection = null;

	public ProjectController() {
		super();
	
	}
 

	@PostConstruct
	public void init() {
		projectListeners = new ArrayList<ProjectListener>();
		
		startProcessList = new StartProcessCache();
		subProcessList = new SubProcessCache();

		this.setMaxSearchResult(this.setupController.getWorkitem().getItemValueInteger(
				"MaxviewEntriesPerPage"));
	
		
	}


	public UserController getUserController() {
		return userController;
	}


	public void setUserController(UserController userController) {
		this.userController = userController;
	}


	public SetupController getSetupsetupController() {
		return setupController;
	}

	public void setSetupsetupController(SetupController setupMB) {
		this.setupController = setupMB;
	}

	


	public synchronized void addProjectistener(ProjectListener l) {
		// Test if the current listener was allreaded added to avoid that a
		// listener register itself more than once!
		if (!projectListeners.contains(l))
			projectListeners.add(l);
	}

	public synchronized void removeProjectListener(ProjectListener l) {
		projectListeners.remove(l);
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireProjectCreatedEvent() {
		for (Iterator<ProjectListener> i = projectListeners.iterator(); i
				.hasNext();) {
			ProjectListener l = i.next();
			l.onProjectCreated(getWorkitem());
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireProjectChangedEvent() {
		for (Iterator<ProjectListener> i = projectListeners.iterator(); i
				.hasNext();) {
			ProjectListener l = i.next();
			l.onProjectChanged(getWorkitem());
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireProjectProcessEvent() {
		for (Iterator<ProjectListener> i = projectListeners.iterator(); i
				.hasNext();) {
			ProjectListener l = i.next();
			l.onProjectProcess(getWorkitem());
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireProjectProcessCompletedEvent() {
		for (Iterator<ProjectListener> i = projectListeners.iterator(); i
				.hasNext();) {
			ProjectListener l = i.next();
			l.onProjectProcessCompleted(getWorkitem());
		}
	}

	/**
	 * Informs WorkitemListeners about a deletion of a workitem
	 */
	private void fireProjectDeleteEvent() {
		for (Iterator<ProjectListener> i = projectListeners.iterator(); i
				.hasNext();) {
			ProjectListener l = i.next();
			l.onProjectDelete(getWorkitem());
		}
	}
	



	public ProjectService getProjectService() {
		return projectService;
	}

	public ProfileService getProfileService() {
		return profileService;
	}

	/**
	 * setWorkitem overwritten to migrate new name concept
	 * 
	 */
	public void setWorkitem(ItemCollection aworkitem) {
		// clear inivtations and team
		
		team = null;
	
		// clear sibling list
		projectSiblingList = null;

		super.setWorkitem(aworkitem);

		try {
			// reset worklist
			
			System.out.println(" Hier fehlt der FireEvent mechanismus um die worklist zu resetten");
			
			/*
			worklistMB.doReset(null);

			// reset view filters
			worklistMB.setProcessFilter(0);
			worklistMB.setWorkflowGroupFilter(null);
			worklistMB.setProjectFilter(
					getWorkitem().getItemValueString("$uniqueid"));
*/
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		// inform listeners
		fireProjectChangedEvent();
	}

	
	
	
	
	
	
	
	/**
	 * Returns true if current user is member of team, owner, parentteam or
	 * parentowner list
	 * 
	 * @return
	 */
	public boolean isMember() {
		return isMember(getWorkitem());
	}

	/**
	 * Returns true if current user is member of team, owner, parentteam or
	 * parentowner list
	 **/
	@SuppressWarnings("unchecked")
	public boolean isMember(ItemCollection aProject) {
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		String remoteUser = externalContext.getRemoteUser();

		List<String> vOwner = aProject.getItemValue("namOwner");
		List<String> vTeam = aProject.getItemValue("namTeam");
		List<String> vAssist = aProject.getItemValue("namAssist");
		List<String> vManager = aProject.getItemValue("namManager");
		List<String> vPOwner = aProject.getItemValue("namParentOwner");
		List<String> vPTeam = aProject.getItemValue("namParentTeam");
		List<String> vPAssist = aProject.getItemValue("namParentAssist");
		List<String> vPManager = aProject.getItemValue("namParentManager");

		return (vTeam.indexOf(remoteUser) > -1
				|| vOwner.indexOf(remoteUser) > -1
				|| vAssist.indexOf(remoteUser) > -1
				|| vManager.indexOf(remoteUser) > -1
				|| vPOwner.indexOf(remoteUser) > -1
				|| vPManager.indexOf(remoteUser) > -1
				|| vPAssist.indexOf(remoteUser) > -1 || vPTeam
				.indexOf(remoteUser) > -1);

	}

	/**
	 * Returns true if current user is member of the current project team list
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean isProjectTeam() {
		ExternalContext externalContext = FacesContext.getCurrentInstance()
				.getExternalContext();
		String remoteUser = externalContext.getRemoteUser();
		List<String> vTeam = getWorkitem().getItemValue("namTeam");
		return (vTeam.indexOf(remoteUser) > -1);
	}

	/**
	 * Returns true if current user is member of the current project owner list
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean isProjectOwner() {
		ExternalContext externalContext = FacesContext.getCurrentInstance()
				.getExternalContext();
		String remoteUser = externalContext.getRemoteUser();
		List<String> vTeam = getWorkitem().getItemValue("namOwner");
		return (vTeam.indexOf(remoteUser) > -1);
	}

	/**
	 * Returns true if current user is member of the current project manager
	 * list
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean isProjectManager() {
		ExternalContext externalContext = FacesContext.getCurrentInstance()
				.getExternalContext();
		String remoteUser = externalContext.getRemoteUser();
		List<String> vTeam = getWorkitem()
				.getItemValue("namManager");
		return (vTeam.indexOf(remoteUser) > -1);
	}

	/**
	 * Returns true if current user is member of the current project assist list
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean isProjectAssist() {
		ExternalContext externalContext = FacesContext.getCurrentInstance()
				.getExternalContext();
		String remoteUser = externalContext.getRemoteUser();
		List<String> vTeam = getWorkitem().getItemValue("namAssist");
		return (vTeam.indexOf(remoteUser) > -1);
	}

	@Override
	public void doReset(ActionEvent event) {
		startProjects = null;
		startProcessList = new StartProcessCache();
		subProcessList = new SubProcessCache();
		super.doReset(event);
	}


	/**
	 * returns a unique list with all member names
	 * 
	 * @return
	 */
	public List<String> getMemberList() {
		return getMemberList(getWorkitem());
	}

	public List<String> getMemberList(ItemCollection aProject) {
		List<String> vTeam = aProject.getItemValue("namTeam");
		List<String> vOwner = aProject.getItemValue("namOwner");
		List<String> vAssist = aProject.getItemValue("namAssist");
		List<String> vManager = aProject.getItemValue("namManager");
		List<String> vPOwner = aProject.getItemValue("namParentOwner");
		List<String> vPTeam = aProject.getItemValue("namParentTeam");
		List<String> vPAssist = aProject.getItemValue("namParentAssist");
		List<String> vPManager = aProject.getItemValue("namParentManager");

		// make one vector...
		for (String aitem : vOwner) {
			if (vTeam.indexOf(aitem) == -1)
				vTeam.add(aitem);
		}
		for (String aitem : vAssist) {
			if (vTeam.indexOf(aitem) == -1)
				vTeam.add(aitem);
		}
		for (String aitem : vManager) {
			if (vTeam.indexOf(aitem) == -1)
				vTeam.add(aitem);
		}
		for (String aitem : vPOwner) {
			if (vTeam.indexOf(aitem) == -1)
				vTeam.add(aitem);
		}
		for (String aitem : vPTeam) {
			if (vTeam.indexOf(aitem) == -1)
				vTeam.add(aitem);
		}
		for (String aitem : vPAssist) {
			if (vTeam.indexOf(aitem) == -1)
				vTeam.add(aitem);
		}
		for (String aitem : vPManager) {
			if (vTeam.indexOf(aitem) == -1)
				vTeam.add(aitem);
		}
		return vTeam;
	}

	
	
	/**
	 * This method creates an empty project instance. The method sets the
	 * modelversion to the current user language selection
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doCreate(ActionEvent event) throws Exception {

		ItemCollection project = projectService
				.createProject(START_PROJECT_PROCESS_ID);

		// determine user language and set Model version depending on the
		// selected user locale
		Locale userLocale = FacesContext.getCurrentInstance().getViewRoot()
				.getLocale();
		String sUserLanguage = userLocale.getLanguage();

		// Set System Model Version for this Project to user Language
		String sModelVersion = userController.getModelVersionHandler()
				.getLatestSystemVersion(sUserLanguage);

		if (sModelVersion == null)
			throw new Exception(
					"Warning - no system model found for language '"
							+ sUserLanguage + "'");

		project.replaceItemValue("$modelversion", sModelVersion);

		// We do no longer check if the user lanaguage is supported by one of
		// the
		// model files - as a user can always chouse any model version/language
		// for a project
		// he like.
		// =====================
		// Now determine if the User Language is available in the users
		// ModelDomain!

		// String sModelDomain = getProfileBean().getUserModelDomain();
		String sModelLanguage = sUserLanguage;

		// Collection<String> colLangs =
		// getProfileBean().getModelVersionHandler()
		// .getLanguageSupport(sModelDomain);

		/*
		 * try { if (!colLangs.contains(sUserLanguage)) sModelLanguage =
		 * colLangs.iterator().next(); } catch (Exception cd) { throw new
		 * Exception( "No language support (" + sUserLanguage +
		 * ") for prefered user model domain '" + sModelDomain +
		 * "' found! Check user profile"); }
		 */

		project.replaceItemValue("txtModelLanguage",
				sModelLanguage);

		// add current user to team and owner lists
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		String remoteUser = externalContext.getRemoteUser();
		project.replaceItemValue("namTeam", remoteUser);
		project.replaceItemValue("namOwner", remoteUser);
		project.replaceItemValue("namAssist", new Vector());
		project.replaceItemValue("namManager", new Vector());

		// add a default process
		project.replaceItemValue(
				"txtprocesslist",
				this.setupController.getWorkitem()
						.getItemValue("defaultprojectprocesslist"));
		
		
		setWorkitem(project);
		// inform Listeners...
					fireProjectCreatedEvent();


	}

	/**
	 * This method acts as an actionListener to creates a new Project dataobject
	 * based on the current selected Project. A method caller must care about
	 * the right setting of the ProjectMB. It needs to be set to the parent
	 * project!
	 * 
	 * A Subproject begins with the name of the parent project followed by a '.'
	 * 
	 * The method set a reference using the $uniqueidRef attriubte which points
	 * to the parent project
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doCreateSubproject(ActionEvent event) throws Exception {

		// save project name from current project
		String sParentProjectName = getWorkitem()
				.getItemValueString("txtName");

		String sParentProjektID = getWorkitem()
				.getItemValueString("$UniqueID");

		doCreate(event);

		// set Parent Name and Reference
		getWorkitem().replaceItemValue("txtParentname",
				sParentProjectName);
		getWorkitem().replaceItemValue("$UniqueIDRef",
				sParentProjektID);

	}

	

	


	/**
	 * refreshes the current workitem list. so the list will be loaded again.
	 */
	public void doRefresh(ActionEvent event) {
		// projectSelection = null;
		//workitemItemCollection = null;
	}

	/**
	 * This method is for saving and processing a single project. The method
	 * generates the attribute 'txtprocesslist' containing a list of
	 * ModelVersions+ProcessIDs. This list will be used to start a new Process
	 * by a Workitem according to this project.
	 * 
	 * Attributes inherited by the ParentProject are updated through the
	 * ProjectServiceBean. So no additional Business logic is needed here. The
	 * Reference to a ParentProject is stored in the attribute $UniqueIDRef
	 * 
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
					activityID = (Integer) currentParam.getValue();
					break;
				}
			}
		}

		ItemCollection project=getWorkitem();
		// remove a4j: attributes generated inside the viewentries by the UI
		
		// set max History & log length
		project.replaceItemValue(
				"numworkflowHistoryLength",
				setupController.getWorkitem().getItemValueInteger(
						"MaxProjectHistoryLength"));
		project.replaceItemValue(
				"numworkflowLogLength",
				setupController.getWorkitem().getItemValueInteger(
						"MaxProjectHistoryLength"));

	

	

		// Process project via processService EJB
		project.replaceItemValue("$activityid", activityID);
		

		// inform Listeners...
		fireProjectProcessEvent();

		project = projectService
				.processProject(project);
		// inform Listeners...
		fireProjectProcessCompletedEvent();


		ItemCollection saveItem = project;

		
		
		this.setWorkitem(saveItem);

	}

	

	/**
	 * This Method adds an user entry to the owner or team list. The method is
	 * triggered by an ajax command from the project.xhtml
	 * 
	 * If the username was not found a validation error message will be send
	 * 
	 * If the username is empty ('') the method returns.
	 * 
	 * @see doAddUserImidiate
	 * @param event
	 * @throws Exception
	 */
	public void doAddUser(ActionEvent event) throws Exception {
		String sUserField;
		String sNewValue = "";
		List<String> vEntries;
		String sErrorMessage = "";

		// determine field (owner or team)....
		String sID = event.getComponent().getId();
		UIInput input = null;
		if (sID.toLowerCase().indexOf("owner") > -1) {
			sUserField = "namOwner";
			input = (UIInput) event.getComponent().findComponent(
					"new_owner_input_id");
		} else if (sID.toLowerCase().indexOf("manager") > -1) {
			sUserField = "namManager";
			input = (UIInput) event.getComponent().findComponent(
					"new_manager_input_id");
		} else if (sID.toLowerCase().indexOf("assist") > -1) {
			sUserField = "namAssist";
			input = (UIInput) event.getComponent().findComponent(
					"new_assist_input_id");
		} else {
			sUserField = "namTeam";
			input = (UIInput) event.getComponent().findComponent(
					"new_team_input_id");
		}

		// get value to add....
		sNewValue = (input.getValue().toString());
		sNewValue = sNewValue.trim();

		// if value is empty - return...
		if ("".equals(sNewValue))
			return;

		FacesContext context = FacesContext.getCurrentInstance();
		// verify value and translate into accountname
		String sAccount = null;
		// we can not use nameLookupBean here because this bean did not verify a
		// name object! So we need to force a ejb lookup here

		// test if the newValue is an Email address - if so than lookup the
		// email name
		ItemCollection profile = null;
		if (sNewValue.contains("@"))
			profile = profileService.findProfileByEmail(sNewValue);
		if (profile == null)
			profile = profileService.findProfileByUserName(sNewValue);

		// profile found?
		if (profile != null)
			sAccount = profile.getItemValueString("txtName");

		if (sAccount != null) {
			// clear Input field
			input.setValue("");
			// add Value to NameList
			vEntries = getWorkitem().getItemValue(sUserField);
			if (vEntries.indexOf(sAccount) == -1) {
				vEntries.add(sAccount);
				getWorkitem().replaceItemValue(sUserField, vEntries);
			}
		} else {
			try {
				// Generate Error message
				UIViewRoot viewRoot = FacesContext.getCurrentInstance()
						.getViewRoot();
				Locale locale = viewRoot.getLocale();
				ResourceBundle rb = null;
				if (locale != null)
					rb = ResourceBundle.getBundle("bundle.project", locale);
				else
					rb = ResourceBundle.getBundle("bundle.project");

				sErrorMessage = rb.getString("adduser_error");

			} catch (Exception e) {
				sErrorMessage = "Username '[username]' not found";
			}

			sErrorMessage = sErrorMessage.replace("[username]", sNewValue);
			FacesMessage message = new FacesMessage("* ", sErrorMessage);
			context.addMessage("project_form_id:" + input.getId(), message);
		}

	}

	/**
	 * This Method adds an user entry to the owner or team list. The method is
	 * triggered by an ajax command from the project.xhtml
	 * 
	 * In different to the method doAddUser() this method did not validate the
	 * userinput
	 * 
	 * If the username is empty ('') the method returns.
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doAddUserImmediate(ActionEvent event) throws Exception {
		String sUserField;
		String sNewValue = "";
		List<String> vEntries;
		String sErrorMessage = "";

		// determine field (owner or team)....
		String sID = event.getComponent().getId();
		UIInput input = null;
		if (sID.toLowerCase().indexOf("owner") > -1) {
			sUserField = "namOwner";
			input = (UIInput) event.getComponent().findComponent(
					"new_owner_input_id");
		} else if (sID.toLowerCase().indexOf("manager") > -1) {
			sUserField = "namManager";
			input = (UIInput) event.getComponent().findComponent(
					"new_manager_input_id");
		} else if (sID.toLowerCase().indexOf("assist") > -1) {
			sUserField = "namAssist";
			input = (UIInput) event.getComponent().findComponent(
					"new_assist_input_id");
		} else {
			sUserField = "namTeam";
			input = (UIInput) event.getComponent().findComponent(
					"new_team_input_id");
		}

		// get value to add....
		sNewValue = (input.getValue().toString());
		sNewValue = sNewValue.trim();

		// if value is empty - return...
		if ("".equals(sNewValue))
			return;

		FacesContext context = FacesContext.getCurrentInstance();
		// verify value and translate into accountname
		String sAccount = null;
		// we can not use nameLookupBean here because this bean did not verify a
		// name object! So we need to force a ejb lookup here

		// test if the newValue is an Email address - if so than lookup the
		// email name
		ItemCollection profile = null;
		if (sNewValue.contains("@"))
			profile = profileService.findProfileByEmail(sNewValue);
		if (profile == null)
			profile = profileService.findProfileByUserName(sNewValue);

		// profile found?
		if (profile != null)
			sAccount = profile.getItemValueString("txtName");
		else
			// simply take the users input - no validation !
			sAccount = sNewValue;

		if (sAccount != null) {
			// clear Input field
			input.setValue("");
			// add Value to NameList
			vEntries = getWorkitem().getItemValue(sUserField);
			if (vEntries.indexOf(sAccount) == -1) {
				vEntries.add(sAccount);
				getWorkitem().replaceItemValue(sUserField, vEntries);
			}
		}
	}

	/**
	 * This Method removes an user entry of the owner or team list.
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doRemoveUser(ActionEvent event) throws Exception {
		String currentSelection = null;
		String sUserField = null;
		String sID = null;

		// suche selektierte Zeile....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// Zeile gefunden
			currentSelection = (String) ((UIData) parent).getRowData();
			break;

		}
		if (currentSelection != null) {
			sID = event.getComponent().getId();
			// determine member property
			if (sID.toLowerCase().indexOf("owner") > -1) {
				sUserField = "namOwner";
			} else if (sID.toLowerCase().indexOf("manager") > -1) {
				sUserField = "namManager";
			} else if (sID.toLowerCase().indexOf("assist") > -1) {
				sUserField = "namAssist";
			} else {
				sUserField = "namTeam";
			}
			// remove entry
			List vEntries = getWorkitem().getItemValue(sUserField);
			vEntries.remove(currentSelection);
			getWorkitem().replaceItemValue(sUserField, vEntries);
		}

	}

	/**
	 * This Method Selects the Parent project and refreshes the Worklist Bean so
	 * wokitems of these project will be displayed after show_worklist
	 * 
	 * @return
	 */
	public void doSwitchToMainProject(ActionEvent event) {

		ItemCollection currentSelection = null;

		String sIDRef = this.getWorkitem().getItemValueString("$uniqueIDRef");
		ItemCollection parentProject = this.getProjectService().findProject(
				sIDRef);

		this.setWorkitem(parentProject);

	
	}

	/**
	 * This method deletes a project and all containing workitems
	 * 
	 * This method can only be called by role
	 * org.imixs.ACCESSLEVEL.MANAGERACCESS or by an Owner of the project
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doDelete(ActionEvent event) throws Exception {
		ExternalContext ectx = FacesContext.getCurrentInstance()
				.getExternalContext();
		String remoteUser = ectx.getRemoteUser();
		List ownerList = this.getWorkitem().getItemValue("namOwner");

		if (ectx.isUserInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS")
				|| ownerList.indexOf(remoteUser) > -1) {
			
			// inform Listeners...
			fireProjectDeleteEvent();
			projectService.deleteProject(this.getWorkitem());
			
		}
	}

	/**
	 * deletes a Project and its subprojects and workitems by changing the
	 * attribute type' into 'workitemdeleted'
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doSoftDelete(ActionEvent event) throws Exception {
		
			projectService.moveIntoDeletions(getWorkitem());

		
	}

	/**
	 * This method collects informations about all workitmes connected to the
	 * current project
	 * 
	 * 
	 * This method can only be called by role
	 * org.imixs.ACCESSLEVEL.MANAGERACCESS!
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doAnalyse(ActionEvent event) throws Exception {

		ExternalContext ectx = FacesContext.getCurrentInstance()
				.getExternalContext();
		if (ectx.isUserInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS")) {
			projectService.analyseProject(this.getWorkitem());
		}
	}

	/**
	 * returns the list of ItemCollectionAdapter Objects for each Team member
	 * 
	 * @return
	 */
	public ArrayList<ItemCollection> getTeam() throws Exception {
		if (team != null)
			return team;
		team = new ArrayList<ItemCollection>();
		List<String> vTeam = this.getWorkitem().getItemValue("namTeam");
		for (String sName : vTeam) {
			ItemCollection profile = profileService
					.findProfileByUserName(sName);
			if (profile != null)
				team.add((profile));
		}
		return team;
	}

	/**
	 * returns the list of Team Select Items to be displayed in the Project
	 * form. Each Item contains the accountname (txtName) and also the
	 * displayname (txtUsername)
	 * 
	 * @return
	 */
	public ArrayList<SelectItem> getTeamSelection() throws Exception {
		List<String> vOwners = this.getWorkitem().getItemValue("namTeam");
		ArrayList<SelectItem> nameSelection = new ArrayList<SelectItem>();
		for (String sName : vOwners)
			nameSelection.add(new SelectItem(sName, userController
					.getUserName(sName)));
		return nameSelection;
	}

	/**
	 * returns the list of Team Select Items to be displayed in the Project
	 * form. Each Item contains the accountname (txtName) and also the
	 * displayname (txtUsername)
	 * 
	 * @return
	 */
	public ArrayList<SelectItem> getManagerSelection() throws Exception {
		List<String> vOwners = this.getWorkitem().getItemValue("namManager");
		ArrayList<SelectItem> nameSelection = new ArrayList<SelectItem>();
		for (String sName : vOwners)
			nameSelection.add(new SelectItem(sName, userController
					.getUserName(sName)));
		return nameSelection;
	}

	/**
	 * returns the list of Team Select Items to be displayed in the Project
	 * form. Each Item contains the accountname (txtName) and also the
	 * displayname (txtUsername)
	 * 
	 * @return
	 */
	public ArrayList<SelectItem> getAssistSelection() throws Exception {
		List<String> vOwners = this.getWorkitem().getItemValue("namAssist");
		ArrayList<SelectItem> nameSelection = new ArrayList<SelectItem>();
		for (String sName : vOwners)
			nameSelection.add(new SelectItem(sName, userController
					.getUserName(sName)));
		return nameSelection;
	}

	/**
	 * returns the list of Owner Select Items to be displayed in the Project
	 * form. Each Item contains the accountname (txtName) and also the
	 * displayname (txtUsername)
	 * 
	 * @return
	 */
	public ArrayList<SelectItem> getOwnerSelection() throws Exception {
		List<String> vOwners = this.getWorkitem().getItemValue("namowner");
		ArrayList<SelectItem> nameSelection = new ArrayList<SelectItem>();
		for (String sName : vOwners)
			nameSelection.add(new SelectItem(sName, userController
					.getUserName(sName)));
		return nameSelection;
	}

	



	/**
	 * Returns a list of project siblings to the current project. if the current
	 * project is a main project, all main projects will be returned if the
	 * current project is a subproject, all subproject for the corresponding
	 * main project will be returned.
	 * 
	 * @return
	 */
	public ArrayList<ItemCollection> getProjectSiblings() {

		if (projectSiblingList != null)
			return projectSiblingList;

		projectSiblingList = new ArrayList<ItemCollection>();
		List<ItemCollection> col = null;

		String sIDRef = this.getWorkitem().getItemValueString("$uniqueIDRef");

		// is it a main Project?

		if ("".equals(sIDRef))
			col = projectService.findAllMainProjects(0, -1);
		else
			col = projectService.findAllSubProjects(sIDRef, 0, -1);

		for (ItemCollection aworkitem : col) {
			projectSiblingList.add((aworkitem));
		}

		return projectSiblingList;

	}

	/**
	 * Helper class to convert the provided vector into a List Object. The
	 * itemListArray method from the ItemCollectionAdapter did not work for
	 * RichFaces PickList. I was unable to find out the reason for this strange
	 * behavor. So the following getter and settern methods did convert the
	 * internal Vector representation into a List<Object>
	 * 
	 * So in the project.xhtml page the process selectMany object is bound as
	 * this:
	 * 
	 * <code>
	 *  <rich:pickList value="#{projectMB.processList}" >
			<f:selectItems value="#{modelMB.startProcessList}"/>
	    </rich:pickList>
	 * </code>
	 * 
	 * normally this would be much easier with the
	 * ItemCollectionAdapter.getItemListArray:
	 * 
	 * <code>
	 * <h:selectManyCheckbox value="#{projectMB.itemListArray['txtprocessList']}">
			<f:selectItems value="#{modelMB.startProcessList}"/>
		</h:selectManyCheckbox>
	 * </code>
	 * 
	 * but this will not work for richfaces Picklist
	 * 
	 * @return
	 */
	public List<Object> getProcessList() {
		List<Object> aList = new ArrayList<Object>();
		
			List v = getWorkitem().getItemValue("txtprocesslist");
			for (Object aEntryValue : v) {
				aList.add(aEntryValue);
			}
		
		return aList;

	}

	public void setProcessList(List<Object> aList) {
		if (aList != null) {
			// convert List<Object> into Vector
			List v = new Vector();
			for (Object aEntryValue : aList)
				v.add(aEntryValue);

			try {
				getWorkitem().replaceItemValue("txtprocesslist", v);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	

	public List<ItemCollection> getProjects() {
		selectMainProjects = false;
		if (projects == null)
			loadProjectList();
		return projects;

	}

	/**
	 * This method returns a list of all Projects with ProcessIDs defined and
	 * where the current User is a Member of. So these Projects represent the
	 * collection of Projects where the User can start a new task.
	 * 
	 * @return
	 */
	public List<ItemCollection> getStartProjects() {
		if (startProjects == null) {
			startProjects = new ArrayList<ItemCollection>();
			try {
				List<ItemCollection> col = null;
				long l = System.currentTimeMillis();
				col = projectService.findAllProjects(0, -1);

				logger.fine("  loadStartProjectList ("
						+ (System.currentTimeMillis() - l) + " ms) ");

				setEndOfList( col.size() <  getMaxSearchResult());
				
				for (ItemCollection aworkitem : col) {
					// test if Project contains a ProcessList
					List<String> vprojectList = aworkitem
							.getItemValue("txtprocesslist");
					if (vprojectList.size() > 0
							&& this.isMember(aworkitem))
						startProjects.add((aworkitem));
				}
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}
		return startProjects;

	}


	
	/**
	 * returns a project list where the current user is owner
	 * 
	 * @return
	 */
	public List<ItemCollection> getMyProjects() {
		if (projects == null)
			loadMyProjectList();
		return projects;
	}

	/**
	 * returns a project list where the current user is member
	 * 
	 * @return
	 */
	public List<ItemCollection> getMemberProjects() {
		if (projects == null)
			loadMemberProjectList();
		return projects;
	}
	public List<ItemCollection> getMainProjects() {
		selectMainProjects = true;
		if (projects == null)
			loadProjectList();
		return projects;
	}

	/**
	 * returns a project list where the type is 'projectdeleted'
	 * 
	 * @return
	 */
	public List<ItemCollection> getDeletedProjects() {
		
		String query = "SELECT project FROM Entity AS project "
				+ " WHERE project.type IN ('projectdeleted' ) "
				+ " ORDER BY project.modified DESC";
		
		super.setSearchQuery(query);
		return super.getWorkitems();
		
		
	

	}

	/**
	 * returns the full list of Porjects available to the current user
	 * 
	 * @return
	 */
	public ArrayList<SelectItem> getMyProjectSelection() throws Exception {

		if (myProjectSelection != null)
			return myProjectSelection;

		// load Project list only once...

		myProjectSelection = new ArrayList<SelectItem>();
		long l = System.currentTimeMillis();

		List<ItemCollection> col = projectService.findAllProjects(0, -1);

		logger.fine(" -------------- loadMyProjectList : "
				+ (System.currentTimeMillis() - l) + " ----------------- ");
		for (ItemCollection aworkitem : col) {

			String sID = aworkitem.getItemValueString("$uniqueID");
			String sName = aworkitem.getItemValueString("txtName");

			myProjectSelection.add(new SelectItem(sID, sName));

		}
		return myProjectSelection;
	}
	

	
	/**
	 * Loads the project list
	 * 
	 * boolean selectMainProjects indicates if only main projects should be
	 * loaded or all projects need to be loaded. The user frontend typically
	 * shows only the main projects
	 * 
	 * @return
	 */
	private List<ItemCollection> loadProjectList() {
		projects = new ArrayList<ItemCollection>();
		try {
			List<ItemCollection> col = null;
			long l = System.currentTimeMillis();
			if (selectMainProjects)
				col = projectService.findAllMainProjects(getRow(), getMaxSearchResult());
			else
				col = projectService.findAllProjects(getRow(), getMaxSearchResult());

			logger.fine("  loadProjectList ("
					+ (System.currentTimeMillis() - l) + " ms) ");

			setEndOfList( col.size() <  getMaxSearchResult());
			for (ItemCollection aworkitem : col) {
				projects.add((aworkitem));
			}
		} catch (Exception ee) {
			projects = null;
			ee.printStackTrace();
		}
		return projects;
	}

	/**
	 * Loads the project list where the current user is owner
	 * 
	 * @return
	 */
	private List<ItemCollection> loadMyProjectList() {
		 projects = new ArrayList<ItemCollection>();
		try {
			List<ItemCollection> col = null;
			
			col = projectService.findAllProjectsByOwner(getRow(), getMaxSearchResult());

			setEndOfList( col.size() <  getMaxSearchResult());
			
			for (ItemCollection aworkitem : col) {
				projects.add((aworkitem));
			}
		} catch (Exception ee) {
			projects = null;
			ee.printStackTrace();
		}
		return projects;
	}

	/**
	 * Loads the project list where the current user is member of without public
	 * projects
	 * 
	 * @return
	 */
	private List<ItemCollection> loadMemberProjectList() {
	 projects = new ArrayList<ItemCollection>();
		try {
			List<ItemCollection> col = null;
			long l = System.currentTimeMillis();
			col = projectService.findAllProjectsByMember(getRow(),getMaxSearchResult());

		
			setEndOfList( col.size() <  getMaxSearchResult());
			for (ItemCollection aworkitem : col) {
				projects.add((aworkitem));
			}
		} catch (Exception ee) {
			projects = null;
			ee.printStackTrace();
		}
		return projects;
	}
	
	


	/**
	 * This class implements an internal Cache for the StartProcess Lists
	 * assigned to a project. The Class overwrites the get() Method and
	 * implements an lazy loading mechanism to load a startprocess List for
	 * project the first time the list was forced. After the first load the list
	 * is cached internal so further get() calls are very fast.
	 * 
	 * The key value expected of the get() method is a string with the $uniqueID
	 * of the corresponding project. The class uses the projectService EJB to
	 * load the informations of a project.
	 * 
	 * The Cache size is shrinked to a maximum of 30 projects to be cached one
	 * time. This mechanism can be optimized later...
	 * 
	 * @author rsoika
	 * 
	 */
	class StartProcessCache extends HashMap {
		HashMap processEntityCache;
		final int MAX_SIZE = 30;

		/**
		 * returns a single value out of the ItemCollection if the key dos not
		 * exist the method will create a value automatical
		 */
		@SuppressWarnings("unchecked")
		public Object get(Object key) {
			List<ItemCollection> startProcessList;

			// check if a key is a String....
			if (!(key instanceof String))
				return null;

			// 1.) try to get list out from cache..
			startProcessList = (List<ItemCollection>) super.get(key);
			if (startProcessList != null)
				// list already known and loaded into the cache!....
				return startProcessList;

			logger.fine(" -------------- loadProcessList for Project " + key
					+ "----------------- ");

			if (processEntityCache == null)
				processEntityCache = new HashMap();

			startProcessList = new ArrayList<ItemCollection>();
			// first load Project
			ItemCollection aProject = projectService
					.findProject(key.toString());

			if (aProject == null)
				return startProcessList;

			List<String> vprojectList = aProject.getItemValue("txtprocesslist");

			// load ModelVersion
			// String sProcessModelVersion = aProject
			// .getItemValueString("txtProcessModelVersion");
			// sProcessModelVersion = "public-de-general-0.0.1";
			// get StartProcessList first time and store result into cache...

			for (String aProcessIdentifier : vprojectList) {
				// try to get ProcessEntity form ProcessEntity cache
				ItemCollection itemColProcessEntity = (ItemCollection) processEntityCache
						.get(aProcessIdentifier);

				if (itemColProcessEntity == null) {
					// not yet cached...
					try {
						// now try to separate modelversion from process id ...
						if (aProcessIdentifier.contains("|")) {
							logger.fine(" -------------- loadProcessEntity into cache ----------------- ");

							String sProcessModelVersion = aProcessIdentifier
									.substring(0,
											aProcessIdentifier.indexOf('|'));
							String sProcessID = aProcessIdentifier
									.substring(aProcessIdentifier.indexOf('|') + 1);

							logger.fine(" -------------- Modelversion:"
									+ sProcessModelVersion
									+ " ----------------- ");
							logger.fine(" -------------- ProcessID:"
									+ sProcessID + " ----------------- ");

							itemColProcessEntity = modelService
									.getProcessEntityByVersion(
											Integer.parseInt(sProcessID),
											sProcessModelVersion);

						}
					} catch (NumberFormatException e) {
						e.printStackTrace();
						itemColProcessEntity = null;
					} catch (Exception e) {
						e.printStackTrace();
						itemColProcessEntity = null;
					}
					// put processEntity into cache
					if (itemColProcessEntity != null)
						processEntityCache.put(aProcessIdentifier,
								itemColProcessEntity);
				}
				if (itemColProcessEntity != null)
					startProcessList.add((itemColProcessEntity));
			}

			// now put startProcessList first time into the cache

			// if size > MAX_SIZE than remove first entry
			if (this.keySet().size() > MAX_SIZE) {
				Object oldesKey = this.keySet().iterator().next();

				System.out
						.println(" -------------- maximum CacheSize exeeded remove : "
								+ oldesKey);

				this.remove(oldesKey);
			}

			this.put(key, startProcessList);

			return startProcessList;
		}

	}

	/**
	 * This class implements an internal Cache for the SubProcess Lists assigned
	 * to a project. A Subprocess is indeicated by the '~' character in its
	 * group name. The SubProcessCache is similar to the StartProcessCache class
	 * 
	 * The key value expected of the get() method is a string with the $unqiueid
	 * of a workitem. From this worktiem the modelversion and a specific
	 * txtWorkflowGroup name of the the main process will be taken
	 * <p>
	 * e.g. public-standard-de-0.0.1 Ticketservice
	 * <p>
	 * The Method searches all start processIDs with txtWorkflowGroup names
	 * started with the given Groupname + '~'
	 * 
	 * 
	 * 
	 * @author rsoika
	 * 
	 */
	class SubProcessCache extends HashMap {
		// HashMap processEntityCache;
		final int MAX_SIZE = 30;

		/**
		 * returns a single value out of the ItemCollection if the key dos not
		 * exist the method will create a value automatical
		 */
		@SuppressWarnings("unchecked")
		public Object get(Object key) {
			List<ItemCollection> startProcessList;

			// check if a key is a String....
			if (!(key instanceof String))
				return null;

			// find modelversion and group name from workitem proivded by key
			// ($uniqueid)
			// if $uniueid is equals to current workitem than no lookup is
			// neede.

			// find workitem
			ItemCollection workitem= entityService.load(key.toString());
			

			if (workitem == null)
				return null;
			// get modelversio and group name from given workitem
			String sModelVersion = workitem.getItemValueString("$modelVersion");
			String sGroupName = workitem.getItemValueString("txtWorkflowGroup");

			// now update the key
			key = sModelVersion + "|" + sGroupName;

			// 1.) try to get list out from cache..
			startProcessList = (List<ItemCollection>) super.get(key);
			if (startProcessList != null)
				// list already known and loaded into the cache!....
				return startProcessList;

			startProcessList = new ArrayList<ItemCollection>();

			System.out
					.println(" -------------- loadSubProcessList for ModelVersion "
							+ sModelVersion
							+ " and ProcessGroup "
							+ sGroupName
							+ "----------------- ");

			List<ItemCollection> aProcessList = modelService
					.getAllStartProcessEntitiesByVersion(sModelVersion);

			Iterator<ItemCollection> iter = aProcessList.iterator();
			while (iter.hasNext()) {
				ItemCollection processEntity = iter.next();
				String sSubGroupName = processEntity
						.getItemValueString("txtWorkflowGroup");

				// the process will not be added if it is a SubprocessGroup
				// Indicated by a '~' char
				if (!sSubGroupName.startsWith(sGroupName + "~"))
					continue;
				// subprocess maches!
				// add txtWorkflowSubGroup property
				try {
					processEntity
							.replaceItemValue("txtWorkflowSubGroup",
									sSubGroupName.substring(sSubGroupName
											.indexOf("~") + 1));
				} catch (Exception e) {

					e.printStackTrace();
				}

				startProcessList.add((processEntity));

			}

			// now put startProcessList first time into the cache

			// if size > MAX_SIZE than remove first entry
			if (this.keySet().size() > MAX_SIZE) {
				Object oldesKey = this.keySet().iterator().next();

				System.out
						.println(" -------------- maximum CacheSize exeeded remove : "
								+ oldesKey);

				this.remove(oldesKey);
			}

			this.put(key, startProcessList);

			return startProcessList;
		}

	}

}
