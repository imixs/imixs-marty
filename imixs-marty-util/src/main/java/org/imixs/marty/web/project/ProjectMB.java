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

package org.imixs.marty.web.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
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

import org.imixs.marty.ejb.ProfileService;
import org.imixs.marty.ejb.ProjectService;
import org.imixs.marty.model.ModelData;
import org.imixs.marty.web.profile.MyProfileMB;
import org.imixs.marty.web.profile.NameLookupMB;
import org.imixs.marty.web.util.SetupMB;
import org.imixs.marty.web.workitem.WorkitemMB;
import org.imixs.marty.web.workitem.WorklistMB;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.faces.AbstractWorkflowController;
import org.richfaces.event.DropEvent;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

public class ProjectMB extends AbstractWorkflowController {

	public final static int START_PROJECT_PROCESS_ID = 100;

	/* Project Service */
	@EJB
	ProjectService projectService;
	/* Profile Service */
	@EJB
	ProfileService profileService;

	private TreeNode processTreeSelection = null;
	private MyProfileMB myProfileMB = null;
	private SetupMB setupMB = null;
	private ProjectlistMB projectlist = null;
	private WorklistMB worklistMB = null;
	private WorkitemMB workitemMB = null;
	private NameLookupMB nameLookup = null;
	private ArrayList<ItemCollection> team = null;
	private ArrayList<ItemCollection> projectSiblingList = null;

	private TreeNodeImpl subProjectTree = null;

	private Collection<ProjectListener> projectListeners = new ArrayList<ProjectListener>();

	private static Logger logger = Logger.getLogger("org.imixs.marty");

	
	
	@PostConstruct
	public void init() {
		projectListeners = new ArrayList<ProjectListener>();
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
			l.onProjectCreated(workitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireProjectChangedEvent() {
		for (Iterator<ProjectListener> i = projectListeners.iterator(); i
				.hasNext();) {
			ProjectListener l = i.next();
			l.onProjectChanged(workitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireProjectProcessEvent() {
		for (Iterator<ProjectListener> i = projectListeners.iterator(); i
				.hasNext();) {
			ProjectListener l = i.next();
			l.onProjectProcess(workitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireProjectProcessCompletedEvent() {
		for (Iterator<ProjectListener> i = projectListeners.iterator(); i
				.hasNext();) {
			ProjectListener l = i.next();
			l.onProjectProcessCompleted(workitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a deletion of a workitem
	 */
	private void fireProjectDeleteEvent() {
		for (Iterator<ProjectListener> i = projectListeners.iterator(); i
				.hasNext();) {
			ProjectListener l = i.next();
			l.onProjectDelete(workitemItemCollection);
		}
	}
	
	/**
	 * Returns a instance of the MBProfileMB. This ManagedBean can not be find
	 * during the constructor because the referenece of this bean is queried
	 * form the MyProfielMB itself
	 * 
	 * @return
	 */
	public MyProfileMB getProfileBean() {
		if (myProfileMB == null)
			myProfileMB = (MyProfileMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "myProfileMB");

		return myProfileMB;

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

	public ProjectlistMB getProjectListMB() {
		// get WorklistMB instance
		if (projectlist == null)
			projectlist = (ProjectlistMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "projectlistMB");

		return projectlist;
	}

	public WorklistMB getworkListMB() {
		// get WorklistMB instance
		if (worklistMB == null)
			worklistMB = (WorklistMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "worklistMB");

		return worklistMB;
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

	public NameLookupMB getNameLookupBean() {
		// get WorklistMB instance
		if (nameLookup == null)
			nameLookup = (NameLookupMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "nameLookupMB");

		return nameLookup;
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
		// clear ProcessTree Selection
		processTreeSelection = null;
		// clear SubprojectTree Selection
		subProjectTree = null;
		// clear sibling list
		projectSiblingList = null;

		super.setWorkitem(aworkitem);

		try {
			// reset worklist
			this.getworkListMB().doReset(null);

			// reset view filters
			this.getworkListMB().setProcessFilter(0);
			this.getworkListMB().setWorkflowGroupFilter(null);
			this.getworkListMB().setProjectFilter(
					workitemItemCollection.getItemValueString("$uniqueid"));

			// disable toogle switch for searchfilter and process list
			workitemItemCollection.replaceItemValue("a4j:showSearchFilter",
					false);
			workitemItemCollection.replaceItemValue("a4j:showprocessList",
					false);
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
		return isMember(workitemItemCollection);
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

		Vector<String> vOwner = aProject.getItemValue("namOwner");
		Vector<String> vTeam = aProject.getItemValue("namTeam");
		Vector<String> vAssist = aProject.getItemValue("namAssist");
		Vector<String> vManager = aProject.getItemValue("namManager");
		Vector<String> vPOwner = aProject.getItemValue("namParentOwner");
		Vector<String> vPTeam = aProject.getItemValue("namParentTeam");
		Vector<String> vPAssist = aProject.getItemValue("namParentAssist");
		Vector<String> vPManager = aProject.getItemValue("namParentManager");

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
		Vector<String> vTeam = workitemItemCollection.getItemValue("namTeam");
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
		Vector<String> vTeam = workitemItemCollection.getItemValue("namOwner");
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
		Vector<String> vTeam = workitemItemCollection
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
		Vector<String> vTeam = workitemItemCollection.getItemValue("namAssist");
		return (vTeam.indexOf(remoteUser) > -1);
	}

	/**
	 * returns a unique list with all member names
	 * 
	 * @return
	 */
	public Vector<String> getMemberList() {
		return getMemberList(workitemItemCollection);
	}

	public Vector<String> getMemberList(ItemCollection aProject) {
		Vector<String> vTeam = aProject.getItemValue("namTeam");
		Vector<String> vOwner = aProject.getItemValue("namOwner");
		Vector<String> vAssist = aProject.getItemValue("namAssist");
		Vector<String> vManager = aProject.getItemValue("namManager");
		Vector<String> vPOwner = aProject.getItemValue("namParentOwner");
		Vector<String> vPTeam = aProject.getItemValue("namParentTeam");
		Vector<String> vPAssist = aProject.getItemValue("namParentAssist");
		Vector<String> vPManager = aProject.getItemValue("namParentManager");

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

		workitemItemCollection = projectService
				.createProject(START_PROJECT_PROCESS_ID);

		// determine user language and set Model version depending on the
		// selected user locale
		Locale userLocale = FacesContext.getCurrentInstance().getViewRoot()
				.getLocale();
		String sUserLanguage = userLocale.getLanguage();

		// Set System Model Version for this Project to user Language
		String sModelVersion = this.getProfileBean().getModelVersionHandler()
				.getLatestSystemVersion(sUserLanguage);

		if (sModelVersion == null)
			throw new Exception(
					"Warning - no system model found for language '"
							+ sUserLanguage + "'");

		workitemItemCollection.replaceItemValue("$modelversion", sModelVersion);

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

		workitemItemCollection.replaceItemValue("txtModelLanguage",
				sModelLanguage);

		// add current user to team and owner lists
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		String remoteUser = externalContext.getRemoteUser();
		workitemItemCollection.replaceItemValue("namTeam", remoteUser);
		workitemItemCollection.replaceItemValue("namOwner", remoteUser);
		workitemItemCollection.replaceItemValue("namAssist", new Vector());
		workitemItemCollection.replaceItemValue("namManager", new Vector());

		// add a default process
		workitemItemCollection.replaceItemValue(
				"txtprocesslist",
				this.getConfigBean().getWorkitem()
						.getItemValue("defaultprojectprocesslist"));
		
		
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
		String sParentProjectName = workitemItemCollection
				.getItemValueString("txtName");

		String sParentProjektID = workitemItemCollection
				.getItemValueString("$UniqueID");

		doCreate(event);

		// set Parent Name and Reference
		workitemItemCollection.replaceItemValue("txtParentname",
				sParentProjectName);
		workitemItemCollection.replaceItemValue("$UniqueIDRef",
				sParentProjektID);

	}

	/**
	 * this method toogles the ajax attribute a4j:showProcessList. This
	 * Attriubte is used to display the StartProcesList inside the Workitem
	 * View.
	 * 
	 * The list of StartProcesses was preloaded by the ProjectListMB during the
	 * Method doSwitchToProject
	 * 
	 **/
	public void doToggleProcessList(ActionEvent event) throws Exception {

		// now try to read current toogle state and switch state
		boolean bTogle = false;
		if (workitemItemCollection.hasItem("a4j:showProcessList")) {
			try {
				boolean bTogleCurrent = (Boolean) workitemItemCollection
						.getItemValue("a4j:showProcessList").firstElement();
				bTogle = !bTogleCurrent;
			} catch (Exception e) {
				bTogle = true;
			}
		} else
			// item did not exist yet....
			bTogle = true;
		workitemItemCollection.replaceItemValue("a4j:showprocessList", bTogle);

		// disable searchfilter
		workitemItemCollection.replaceItemValue("a4j:showSearchFilter", false);
	}

	/**
	 * this method toogles the ajax attribute a4j:showTeamList. This Attriubte
	 * is used to display the Team inside the Workitem View.
	 * 
	 * The list of StartProcesses was preloaded by the ProjectListMB during the
	 * Method doSwitchToProject
	 * 
	 **/
	public void doToggleTeamList(ActionEvent event) throws Exception {

		// now try to read current toogle state and switch state
		boolean bTogle = false;
		if (workitemItemCollection.hasItem("a4j:showTeamList")) {
			try {
				boolean bTogleCurrent = (Boolean) workitemItemCollection
						.getItemValue("a4j:showTeamList").firstElement();
				bTogle = !bTogleCurrent;
			} catch (Exception e) {
				bTogle = true;
			}
		} else
			// item did not exist yet....
			bTogle = true;
		workitemItemCollection.replaceItemValue("a4j:showTeamList", bTogle);

		// disable searchfilter
		workitemItemCollection.replaceItemValue("a4j:showSearchFilter", false);
	}

	/**
	 * this method toogles the ajax attribute a4j:showSearchFilter. This
	 * Attriubte is used to display the SearchFilter inside the Workitem View.
	 **/
	public void doToggleSearchFilter(ActionEvent event) throws Exception {

		// now try to read current toogle state and switch state
		boolean bTogle = false;
		if (workitemItemCollection.hasItem("a4j:showSearchFilter")) {
			try {
				boolean bTogleCurrent = (Boolean) workitemItemCollection
						.getItemValue("a4j:showSearchFilter").firstElement();
				bTogle = !bTogleCurrent;
			} catch (Exception e) {
				bTogle = true;
			}
		} else
			// item did not exist yet....
			bTogle = true;
		workitemItemCollection.replaceItemValue("a4j:showSearchFilter", bTogle);

		// disable processlist
		workitemItemCollection.replaceItemValue("a4j:showprocessList", false);

	}

	/**
	 * refreshes the current workitem list. so the list will be loaded again.
	 */
	public void doRefresh(ActionEvent event) {
		// projectSelection = null;
		workitemItemCollection = null;
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
	 * If the attriubte namTeam or namOwner is empty this method will fill in
	 * the current Username. This is necessary as long as a workflow model uses
	 * the projektTeam or projectOwners from a project to manage read/write
	 * access. If these fields are empty there are situations possible where the
	 * readaccess is empty and everybody can read a workitem
	 * 
	 * If the attribute 'txtLocale' is provided by a project the method updates the 
	 * $modelVersion to the current locale. With this feature it is possibel to change the 
	 * language for a project. 
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

		// remove a4j: attributes generated inside the viewentries by the UI
		workitemItemCollection.getAllItems().remove("a4j:showprocesslist");
		workitemItemCollection.getAllItems().remove("a4j:showteam");
		workitemItemCollection.getAllItems().remove("a4j:showteamlist");
		workitemItemCollection.getAllItems().remove("a4j:showsearchfilter");

		// set max History & log length
		workitemItemCollection.replaceItemValue(
				"numworkflowHistoryLength",
				getConfigBean().getWorkitem().getItemValueInteger(
						"MaxProjectHistoryLength"));
		workitemItemCollection.replaceItemValue(
				"numworkflowLogLength",
				getConfigBean().getWorkitem().getItemValueInteger(
						"MaxProjectHistoryLength"));

		/*
		 * now generate the ProcessList out from the processTreeSelection The
		 * processTreeSelection can be used in JSF Pages. Check if
		 * processTreeSelection is null. Object is null if no treeSelector is
		 * used by the JSF Page
		 */
		if (processTreeSelection != null) {
			Vector<String> vProcessList = new Vector<String>();
			addProcessTreeSelectionToVector(this.processTreeSelection,
					vProcessList);
			// System.out.println("doProcess list="+vProcessList);
			workitemItemCollection.replaceItemValue("txtprocesslist",
					vProcessList);
		}

		// Verify if namTeam or namOwner is empty!
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		String remoteUser = externalContext.getRemoteUser();
		Vector vTeam = workitemItemCollection.getItemValue("namTeam");
		Vector vOwner = workitemItemCollection.getItemValue("namOwner");
		if (vTeam.size() == 0) {
			vTeam.add(remoteUser);
			workitemItemCollection.replaceItemValue("namTeam", vTeam);
		}
		if (vOwner.size() == 0) {
			vOwner.add(remoteUser);
			workitemItemCollection.replaceItemValue("namOwner", vOwner);
		}

		// Process project via processService EJB
		workitemItemCollection.replaceItemValue("$activityid", activityID);
		
		
		// test if the project provides a txtLocale
		String sProjectLocale=workitemItemCollection.getItemValueString("txtLocale");
		if (!"".equals(sProjectLocale)) {
			try {
				String sModelVersion=workitemItemCollection.getItemValueString("$modelVersion");
				// replace the project locale
				int iStart=sModelVersion.indexOf('-');
				int iEnd=sModelVersion.indexOf('-', iStart);
				
				sModelVersion=sModelVersion.substring(0,iStart) + sProjectLocale + sModelVersion.substring(iEnd);
				workitemItemCollection.replaceItemValue("$ModelVersion", sModelVersion);
			} catch (Exception em) {
				logger.severe("[ProjectMB] unable to determine project $modelVersion!");
			}
			
		}
		
		// inform Listeners...
		fireProjectProcessEvent();

		workitemItemCollection = projectService
				.processProject(workitemItemCollection);
		// inform Listeners...
		fireProjectProcessCompletedEvent();


		ItemCollection saveItem = workitemItemCollection;

		this.getProjectListMB().doRefresh(event);
		this.getProjectListMB().resetProcessList();
		this.getProfileBean().clearCache();
		this.setWorkitem(saveItem);

	}

	/**
	 * This method iterates over the ProcessTreeSelection to get all selected
	 * ProcessEntity nodes. The Results are stored into the aVector object as
	 * String values in the format
	 * 
	 * MODELVERSION|ID
	 * 
	 * e.g. :
	 * 
	 * public-de-default-0.0.1|1500 ...
	 * 
	 * The method is called by the doProcess method to update the txtProcessList
	 * attribute of a project.
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void addProcessTreeSelectionToVector(TreeNode aNode,
			Vector<String> aVector) {
		java.util.Iterator<java.util.Map.Entry> iterChilds = aNode
				.getChildren();
		while (iterChilds.hasNext()) {
			Map.Entry aEntry = iterChilds.next();
			TreeNode childNode = (TreeNode) aEntry.getValue();
			if (!childNode.isLeaf())
				addProcessTreeSelectionToVector(childNode, aVector);
			else {
				ModelData childData = (ModelData) childNode.getData();
				aVector.add(childData.getVersion() + "|" + childData.getId());
			}
		}

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
		Vector<String> vEntries;
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
			vEntries = workitemItemCollection.getItemValue(sUserField);
			if (vEntries.indexOf(sAccount) == -1) {
				vEntries.add(sAccount);
				workitemItemCollection.replaceItemValue(sUserField, vEntries);
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
		Vector<String> vEntries;
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
			vEntries = workitemItemCollection.getItemValue(sUserField);
			if (vEntries.indexOf(sAccount) == -1) {
				vEntries.add(sAccount);
				workitemItemCollection.replaceItemValue(sUserField, vEntries);
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
			Vector vEntries = workitemItemCollection.getItemValue(sUserField);
			vEntries.remove(currentSelection);
			workitemItemCollection.replaceItemValue(sUserField, vEntries);
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

		// get WorklistMB instance
		WorklistMB worklist = (WorklistMB) FacesContext
				.getCurrentInstance()
				.getApplication()
				.getELResolver()
				.getValue(FacesContext.getCurrentInstance().getELContext(),
						null, "worklistMB");

		worklist.doReset(event);

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
		Vector ownerList = this.getWorkitem().getItemValue("namOwner");

		if (ectx.isUserInRole("org.imixs.ACCESSLEVEL.MANAGERACCESS")
				|| ownerList.indexOf(remoteUser) > -1) {
			
			// inform Listeners...
			fireProjectDeleteEvent();
			projectService.deleteProject(this.getWorkitem());
			this.getProjectListMB().doReset(event);
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
		if (workitemItemCollection != null) {
			projectService.moveIntoDeletions(workitemItemCollection);

			this.getProjectListMB().doReset(event);
		}
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
	 * This method generates a RichFaces TreeNode for the provided Model
	 * Versions. The Tree is structured by language and domains. The process
	 * Group is the leaf of each subtree. A Process group will only be included
	 * if the processgroup is no subgroup indicated by notation:
	 * maingroup.subgroup
	 * 
	 * @return
	 * @throws Exception
	 */
	public TreeNode getModelTree() throws Exception {
		return this.getProfileBean().getModelVersionHandler().getModelTree();
	}

	/**
	 * Property which holds a TreeNode Implementation of the current process
	 * selection. The method parses the attribute txtprocesslist to generate the
	 * current process tree selection if the method is called the first time
	 * 
	 * 
	 * @return
	 * @throws Exception
	 */
	public TreeNode getModelTreeSelection() throws Exception {
		if (processTreeSelection == null) {
			// Create Process Tree Selection from current txtprocesslist
			processTreeSelection = new TreeNodeImpl();

			Vector<String> vProcessList = workitemItemCollection
					.getItemValue("txtprocesslist");

			// processSelection = new Vector<String>();
			Iterator<String> iterat = vProcessList.iterator();
			while (iterat.hasNext()) {
				String sPID = (String) iterat.next();
				// PID should be in format 'modelversion|processid'
				if (sPID.indexOf("|") > -1) {
					String sModelVersion = sPID.substring(0, sPID.indexOf("|"))
							.trim();
					String sProcessID = sPID.substring(sPID.indexOf("|") + 1)
							.trim();

					// find corresponding start process entity...
					ItemCollection processEntity = getModelService()
							.getProcessEntityByVersion(
									Integer.parseInt(sProcessID), sModelVersion);
					// ...add ProcessTreeNode
					if (processEntity != null) {
						this.getProfileBean()
								.getModelVersionHandler()
								.addModelTreeNode(processTreeSelection,
										sModelVersion, processEntity);
					}
				}

			}
		}
		return processTreeSelection;
	}

	/**
	 * This method handles DropEvents generated by the process treeSelector. The
	 * expected drag object must be an instance of ModelData. The method adds
	 * the corresponding StartProcess Entity or the full tree of a TreeNode.
	 * 
	 * The method is used in both cases. either a process node was dropped to
	 * the process SelectionTree or the process node was dropped to the trash
	 * symbol. If a ProcessTree should be dropped on the trash the drop_type
	 * starts with 'delete_'
	 * 
	 */
	public void processDropProcessSelection(DropEvent event) {
		try {
			ModelData modelData = (ModelData) event.getDragValue();
			if (event.getDragType().startsWith("delete_")) {
				// delete process node from current process selection
				this.getProfileBean().getModelVersionHandler()
						.removeModelTreeNode(processTreeSelection, modelData);
			} else {

				// simply add the dropped version to the ProcessTreeSelection
				// First check if a StartProcessEntiy was dropped... ?
				if (modelData.getId() > -1) {
					// yes - so get ProcessEntity....
					ItemCollection processEntity = getModelService()
							.getProcessEntityByVersion(modelData.getId(),
									modelData.getVersion());
					// and add the ProcessEntity
					this.getProfileBean()
							.getModelVersionHandler()
							.addModelTreeNode(processTreeSelection,
									modelData.getVersion(), processEntity);
				} else {
					// no - a node form the model version was dropped.
					// so simply add the full tree...
					this.getProfileBean()
							.getModelVersionHandler()
							.addModelTreeNode(processTreeSelection,
									modelData.getVersion(), null);
				}
			}

		} catch (Exception e) {
			System.out
					.println("ProcectMB Unable to process Drop Event of ProcessTree Selection");
			e.printStackTrace();
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
		Vector<String> vTeam = this.getWorkitem().getItemValue("namTeam");
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
		Vector<String> vOwners = this.getWorkitem().getItemValue("namTeam");
		ArrayList<SelectItem> nameSelection = new ArrayList<SelectItem>();
		for (String sName : vOwners)
			nameSelection.add(new SelectItem(sName, getNameLookupBean()
					.findUserName(sName)));
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
		Vector<String> vOwners = this.getWorkitem().getItemValue("namManager");
		ArrayList<SelectItem> nameSelection = new ArrayList<SelectItem>();
		for (String sName : vOwners)
			nameSelection.add(new SelectItem(sName, getNameLookupBean()
					.findUserName(sName)));
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
		Vector<String> vOwners = this.getWorkitem().getItemValue("namAssist");
		ArrayList<SelectItem> nameSelection = new ArrayList<SelectItem>();
		for (String sName : vOwners)
			nameSelection.add(new SelectItem(sName, getNameLookupBean()
					.findUserName(sName)));
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
		Vector<String> vOwners = this.getWorkitem().getItemValue("namowner");
		ArrayList<SelectItem> nameSelection = new ArrayList<SelectItem>();
		for (String sName : vOwners)
			nameSelection.add(new SelectItem(sName, getNameLookupBean()
					.findUserName(sName)));
		return nameSelection;
	}

	

	/**
	 * returns a richFacess TreeNode implementation containing a tree structure
	 * of sub projects contained by the current project.
	 * 
	 * @return
	 */
	public TreeNodeImpl getSubProjectTree() {

		if (subProjectTree == null) {
			// create new TreeNode Instance....
			subProjectTree = new TreeNodeImpl();
			// add the root node

			SubProjectTreeNode nodeProcess = new SubProjectTreeNode(
					getWorkitem(), SubProjectTreeNode.ROOT_PROJECT);
			subProjectTree.addChild(getWorkitem().getItemValueString("$uniqueid"),
					nodeProcess);
		}

		return subProjectTree;

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
		if (workitemItemCollection != null) {
			Vector v = workitemItemCollection.getItemValue("txtprocesslist");
			for (Object aEntryValue : v) {
				aList.add(aEntryValue);
			}
		}
		return aList;

	}

	public void setProcessList(List<Object> aList) {
		if (aList != null) {
			// convert List<Object> into Vector
			Vector v = new Vector();
			for (Object aEntryValue : aList)
				v.add(aEntryValue);

			try {
				workitemItemCollection.replaceItemValue("txtprocesslist", v);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
