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

package org.imixs.marty.web.workitem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.imixs.marty.ejb.ProjectService;
import org.imixs.marty.ejb.WorkitemService;
import org.imixs.marty.web.profile.NameLookupMB;
import org.imixs.marty.web.project.ProjectMB;
import org.imixs.marty.web.util.SetupMB;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.faces.workitem.AbstractWorkflowController;

/**
 * This class provides methods to access a single workitem controlled by the
 * Imixs Workflow engine. This is the most used BackingBean class in forms. The
 * different pages from the marty-web module providing all the functionality to
 * create and manage workitems. You can implement additional sub_forms for your
 * individual business process. In this case you can access the WorkitemMB to
 * bind input fields to specific properties of a workitem
 * 
 * @author rsoika
 * 
 */
@ManagedBean
@SessionScoped
public class WorkitemMB extends AbstractWorkflowController {

	public final static String DEFAULT_EDITOR_ID = "default";
	private int sortby = -1;
	private int sortorder = -1;
	/* Workflow Model & Caching objects */
	private HashMap processCache;

	/* Project Backing Bean */
	@ManagedProperty(value = "#{projectMB}")
	private ProjectMB projectMB = null;

	@ManagedProperty(value = "#{setupMB}")
	private SetupMB setupMB = null;

	@ManagedProperty(value = "#{nameLookupMB}")
	private NameLookupMB nameLookupMB = null;

	/* Child Process */
	protected ItemCollection childWorkitemItemCollection = null;

	private ArrayList<ItemCollection> childs = null;
	private ArrayList<ItemCollection> versions = null;
	private ArrayList<ItemCollection> processList = null;

	private Collection<WorkitemListener> workitemListeners = new ArrayList<WorkitemListener>();

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	/* WorkItem Services */
	@EJB
	private org.imixs.marty.ejb.WorkitemService workitemService;

	/* Porject Service */
	@EJB
	ProjectService projectService;

	@PostConstruct
	public void init() {
		processCache = new HashMap();
		workitemListeners = new ArrayList<WorkitemListener>();

	
		// read configuration for the sort order
		if (sortby == -1)
			sortby = setupMB.getWorkitem().getItemValueInteger("Sortby");
		if (sortorder == -1)
			sortorder = setupMB.getWorkitem().getItemValueInteger("Sortorder");
	}

	public ProjectMB getProjectMB() {
		return projectMB;
	}

	public void setProjectMB(ProjectMB projectMB) {
		this.projectMB = projectMB;
	}

	public SetupMB getSetupMB() {
		return setupMB;
	}

	public void setSetupMB(SetupMB setupMB) {
		this.setupMB = setupMB;
	}

	public NameLookupMB getNameLookupMB() {
		return nameLookupMB;
	}

	public void setNameLookupMB(NameLookupMB nameLookupMB) {
		this.nameLookupMB = nameLookupMB;
	}

	/**
	 * returns the workflowEditorID for the current workItem. If no attribute
	 * with the name "txtWorkflowEditorid" is available then the method return
	 * the DEFAULT_EDITOR_ID.
	 * 
	 * Additional the method tests if the txtWorkflowEditorid contains the
	 * character '#'. This character indicates additional form-section
	 * informations. The Method cuts this information and provides an Array of
	 * EditoSection Objects by the property EditorSections
	 * 
	 * @see getEditorSections
	 * 
	 * 
	 * @return
	 */
	public String getEditor() {
		// if (isAvailable()) {

		String sEditor = getWorkitem()
				.getItemValueString("txtWorkflowEditorid");
		if (!"".equals(sEditor)) {
			// test if # is provides to indicate optinal section
			// informations
			if (sEditor.indexOf('#') > -1)
				sEditor = sEditor.substring(0, sEditor.indexOf('#'));
			return sEditor;
		} else
			return DEFAULT_EDITOR_ID;

	}

	/**
	 * This method provides a HashMap with EditorSections. The Method is used to
	 * test if a specific Section is defined within the current Process Entity.
	 * 
	 * EditorSections are provided by the workItem property
	 * 'txtWorkflowEditorid' marked with the '#' character and separated with
	 * charater '|'. Editors can evaluate this additional information to change
	 * the behaivor of a form. The map provides booleans (true) per each section
	 * or false if the section is not included.
	 * 
	 * <code>
	 *   ....<h:outputPanel 
				rendered="#{! empty workitemMB.editorSection['team']}">
	 * </code>
	 * 
	 * @return
	 */
	public Map getEditorSection() {
		// create dynamic hashmap
		return new EditorSectionMap();
	}

	/**
	 * returns an array list with EditorSection Objects. Each EditorSection
	 * object contains the url and the name of one section. EditorSections can
	 * be provided by the workitem property 'txtWorkflowEditorid' marked with
	 * the '#' character and separated with charater '|'.
	 * 
	 * e.g.: form_tab#basic_project|sub_timesheet[owner,manager]
	 * 
	 * This example provides the editor sections 'basic_project' and
	 * 'sub_timesheet'. The optional marker after the second section in []
	 * defines the user membership to access this action. In this example the
	 * second section is only visible if the current user is member of the
	 * project owner or manager list.
	 * 
	 * The following example illustrates how to iterate over the section array
	 * from a JSF fragment:
	 * 
	 * <code>
	 * <ui:repeat value="#{workitemMB.editorSections}" var="section">
	 *   ....
	 *      <ui:include src="/pages/workitems/forms/#{section.url}.xhtml" />
	 * </code>
	 * 
	 * 
	 * The array of EditorSections also contains information about the name for
	 * a section. This name is read from the resouce bundle 'bundle.forms'. The
	 * '/' character will be replaced with '_'. So for example the section url
	 * myforms/sub_timesheet will result in resoure bundle lookup for the name
	 * 'myforms_sub_timersheet'
	 * 
	 * @return
	 */
	public ArrayList<EditorSection> getEditorSections() {
		ArrayList<EditorSection> sections = new ArrayList<EditorSection>();

		UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
		Locale locale = viewRoot.getLocale();

		String sEditor = getWorkitem()
				.getItemValueString("txtWorkflowEditorid");
		if (sEditor.indexOf('#') > -1) {
			String liste = sEditor.substring(sEditor.indexOf('#') + 1);

			StringTokenizer st = new StringTokenizer(liste, "|");
			while (st.hasMoreTokens()) {
				try {
					String sURL = st.nextToken();

					// if the URL contains a [] section test the defined
					// user
					// permissions
					if (sURL.indexOf('[') > -1 || sURL.indexOf(']') > -1) {
						boolean bPermissionGranted = false;
						// yes - cut the permissions
						String sPermissions = sURL.substring(
								sURL.indexOf('[') + 1, sURL.indexOf(']'));

						// cut the permissions from the URL
						sURL = sURL.substring(0, sURL.indexOf('['));
						StringTokenizer stPermission = new StringTokenizer(
								sPermissions, ",");
						while (stPermission.hasMoreTokens()) {
							String aPermission = stPermission.nextToken();
							// test for user role
							ExternalContext ectx = FacesContext
									.getCurrentInstance().getExternalContext();
							if (ectx.isUserInRole(aPermission)) {
								bPermissionGranted = true;
								break;
							}
							// test if user is project member
							if ("owner".equalsIgnoreCase(aPermission)
									&& this.projectMB.isProjectOwner()) {
								bPermissionGranted = true;
								break;
							}
							if ("manager".equalsIgnoreCase(aPermission)
									&& this.projectMB.isProjectManager()) {
								bPermissionGranted = true;
								break;
							}
							if ("team".equalsIgnoreCase(aPermission)
									&& this.projectMB.isProjectTeam()) {
								bPermissionGranted = true;
								break;
							}
							if ("assist".equalsIgnoreCase(aPermission)
									&& this.projectMB.isProjectAssist()) {
								bPermissionGranted = true;
								break;
							}

						}

						// if not permission is granted - skip this section
						if (!bPermissionGranted)
							continue;

					}

					String sName = null;
					// compute name from ressource Bundle....
					try {
						ResourceBundle rb = null;
						if (locale != null)
							rb = ResourceBundle.getBundle("bundle.forms",
									locale);
						else
							rb = ResourceBundle.getBundle("bundle.forms");

						String sResouceURL = sURL.replace('/', '_');
						sName = rb.getString(sResouceURL);
					} catch (java.util.MissingResourceException eb) {
						sName = "";
						System.out.println(eb.getMessage());
					}

					EditorSection aSection = new EditorSection(sURL, sName);
					sections.add(aSection);

				} catch (Exception est) {
					logger.severe("[WorkitemMB] can not parse EditorSections : '"
							+ sEditor + "'");
					logger.severe(est.getMessage());
				}
			}
		}

		return sections;

	}

	/**
	 * This method returns all process entities for the current workflow model.
	 * This list can be used to display state/flow informations inside a form
	 * 
	 * @return
	 */
	public List<ItemCollection> getProcessList() {
		if (processList == null) {
			processList = new ArrayList<ItemCollection>();

			String sGroup = this.getWorkitem().getItemValueString(
					"txtWorkflowGroup");
			String sVersion = this.getWorkitem().getItemValueString(
					"$modelVersion");

			List<ItemCollection> col = this.getModelService()
					.getAllProcessEntitiesByGroupByVersion(sGroup, sVersion);

			for (ItemCollection aworkitem : col)
				processList.add((aworkitem));
		}

		return processList;
	}

	@Override
	public void setWorkitem(ItemCollection aworkitem) {

		super.setWorkitem(aworkitem);

		// reset Versions
		versions = null;
		// reset Childs
		childs = null;
		this.setChildWorkitem(null);

		// reset processlist
		processList = null;

		// inform listeners
		fireWorkitemChangedEvent();
	}

	/**
	 * This method is called by the page myProjects.xhtml form the startProcess
	 * section.
	 * 
	 * This method creates an empty WorkItem assigned to the currentProject and
	 * ProcessEntity. So the WorktIem will become an reference to the current
	 * project by setting the field "$unqiueidref". If no project was selected
	 * before the method throws an exception.
	 * 
	 * The ProcessID, Modelversion and Group will be set to the attributes of
	 * the corresponding ProcessEntity provided by param ID provided by the
	 * ActionEvent
	 * 
	 * The Method expects an ID which identifies the ModelVersion and Process ID
	 * the new Workitem should be started. The ID is a String value with the
	 * following format:
	 * 
	 * modelversion|processID
	 * 
	 * The first Part is the modelversion for the corresponding workflowmodel
	 * the second part will be vast to an integer which corresponds to the start
	 * process id in the model
	 * 
	 * 
	 * Optional the param 'project' is evaluated. If this param is provided the
	 * method selects the refered project.
	 * 
	 * @see WorkitemService
	 * 
	 * @param event
	 * @return
	 */
	public void doCreateWorkitem(ActionEvent event) throws Exception {
		// get Process ID out from the ActionEvent Object....
		List children = event.getComponent().getChildren();
		String processEntityIdentifier = "";
		String projectIdentifier = "";

		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) instanceof UIParameter) {
				UIParameter currentParam = (UIParameter) children.get(i);
				if (currentParam.getName().equals("id")
						&& currentParam.getValue() != null) {
					processEntityIdentifier = (String) currentParam.getValue();
				}
				if (currentParam.getName().equals("project")
						&& currentParam.getValue() != null) {
					projectIdentifier = (String) currentParam.getValue();
				}
				if (!"".equals(processEntityIdentifier)
						&& !"".equals(projectIdentifier))
					break;
			}
		}
		doCreateWorkitem(processEntityIdentifier, projectIdentifier);

	}

	/**
	 * @see doCreateWorkitem(ActionEvent event)
	 * @param processEntityIdentifier
	 * @param projectIdentifier
	 * @throws Exception
	 */
	public void doCreateWorkitem(String processEntityIdentifier,
			String projectIdentifier) throws Exception {
		// this.getWorkitemBlobBean().clear();
		if (processEntityIdentifier != null
				&& !"".equals(processEntityIdentifier)) {

			// if a project was refred switch to this project
			if (projectIdentifier != null && !"".equals(projectIdentifier)) {
				ItemCollection currentProject = this.projectMB
						.getEntityService().load(projectIdentifier);
				projectMB.setWorkitem(currentProject);

			}

			// find ProcessEntity the Worktiem should be started at
			String sProcessModelVersion = processEntityIdentifier.substring(0,
					processEntityIdentifier.indexOf('|'));
			String sProcessID = processEntityIdentifier
					.substring(processEntityIdentifier.indexOf('|') + 1);

			ItemCollection workitem = workitemService.createWorkItem(
					projectMB.getWorkitem(), sProcessModelVersion,
					Integer.parseInt(sProcessID));

			this.setWorkitem(workitem);

			// inform Listeners...
			fireWorkitemCreatedEvent();

		}
	}

	/**
	 * processes the current workitem. The method expects an parameter "id" with
	 * the activity ID which should be processed
	 * 
	 * Attributes inherited by the Project are updated through the
	 * WorkItemServiceBean. So no additional Business logic is needed here.
	 * 
	 * The method adds additional display name fields to be used in history
	 * plugin
	 * 
	 * The Process method from the workflowService will be called. This method
	 * can change the current project reference. See WorkflowService
	 * implementation for details.
	 * 
	 * After all the Method refreshes the Worklist which is typical shown after
	 * a process action. To Change the behavior of the worklist displayed after
	 * this method a bean should register as a workitemListener and implement
	 * the method onWorkitemProcessCompleted
	 * 
	 * 
	 * @param event
	 * @return
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
					activityID = Integer.parseInt(currentParam.getValue()
							.toString());
					break;
				}
			}
		}

		ItemCollection workitem = getWorkitem();
		// remove last workflow result properties......
		workitem.removeItem("action");
		workitem.removeItem("project");

		// set max History & log length
		workitem.replaceItemValue("numworkflowHistoryLength", setupMB
				.getWorkitem().getItemValueInteger("MaxWorkitemHistoryLength"));
		workitem.replaceItemValue("numworkflowLogLength", setupMB.getWorkitem()
				.getItemValueInteger("MaxWorkitemHistoryLength"));

		workitem.replaceItemValue("$ActivityID", activityID);

		updateDisplayNameFields(workitem);

		// inform Listeners...
		fireWorkitemProcessEvent();

		workitem = workitemService.processWorkItem(workitem);

		// inform Listeners...
		fireWorkitemProcessCompletedEvent();
		// update workitemcollection and reset childs
		this.setWorkitem(workitem);

	}

	/**
	 * deletes the current workitem from the database.
	 * 
	 * After all the Method refreshes the Worklist which is typical shown after
	 * a process action. To Change the behavior a bean should register as a
	 * workitemListener and implement the method onWorkitemDeleteCompleted
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doDelete(ActionEvent event) throws Exception {

		// inform Listeners...
		fireWorkitemDeleteEvent();

		workitemService.deleteWorkItem(getWorkitem());
		this.setWorkitem(null);
		// getFileUploadMB().reset();

		// inform Listeners...
		fireWorkitemDeleteCompletedEvent();

	}

	/**
	 * moves a workitem into the archive by changing the attribute type to
	 * 'workitemdeleted'
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doSoftDelete(ActionEvent event) throws Exception {

		// inform Listeners...
		fireWorkitemSoftDeleteEvent();

		workitemService.moveIntoDeletions(getWorkitem());

		// inform Listeners...
		fireWorkitemSoftDeleteCompletedEvent();

	}

	/**
	 * This method is similar to the createWorkitem method but creates a child
	 * process to the current workitem.
	 * 
	 * @see WorkitemService
	 * 
	 * @param event
	 * @return
	 */
	public void doCreateChildWorkitem(ActionEvent event) throws Exception {

		// get Process ID out from the ActionEvent Object....
		List children = event.getComponent().getChildren();
		String processEntityIdentifier = "";

		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) instanceof UIParameter) {
				UIParameter currentParam = (UIParameter) children.get(i);
				if (currentParam.getName().equals("id")
						&& currentParam.getValue() != null) {
					processEntityIdentifier = (String) currentParam.getValue();
					break;
				}
			}
		}
		if (processEntityIdentifier != null
				&& !"".equals(processEntityIdentifier)) {
			// find ProcessEntity the Worktiem should be started at
			String sProcessModelVersion = processEntityIdentifier.substring(0,
					processEntityIdentifier.indexOf('|'));
			String sProcessID = processEntityIdentifier
					.substring(processEntityIdentifier.indexOf('|') + 1);

			childWorkitemItemCollection = workitemService.createWorkItem(
					getWorkitem(), sProcessModelVersion,
					Integer.parseInt(sProcessID));

			this.setChildWorkitem(childWorkitemItemCollection);

			// inform Listeners...
			fireChildCreatedEvent();

		}
	}

	/**
	 * this method is called by datatables to select an workitem
	 * 
	 * @return
	 */
	public void doEditChild(ActionEvent event) {
		ItemCollection currentSelection = null;
		// suche selektierte Zeile....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// Zeile gefunden
			currentSelection = (ItemCollection) ((UIData) parent).getRowData();
			setChildWorkitem(currentSelection);
			break;

		}
	}

	/**
	 * processes the current child workitem. The method expects an parameter
	 * "id" with the activity ID which should be processed
	 * 
	 * The method adds additional display name fields to be used in history
	 * plugin
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doProcessChild(ActionEvent event) throws Exception {
		// Activity ID raussuchen und in activityID speichern
		List children = event.getComponent().getChildren();
		int activityID = -1;

		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) instanceof UIParameter) {
				UIParameter currentParam = (UIParameter) children.get(i);
				if (currentParam.getName().equals("id")
						&& currentParam.getValue() != null) {
					activityID = Integer.parseInt(currentParam.getValue()
							.toString());
					break;
				}
			}
		}

		// set max History & log length
		childWorkitemItemCollection.replaceItemValue(
				"numworkflowHistoryLength", setupMB.getWorkitem()
						.getItemValueInteger("MaxWorkitemHistoryLength"));
		childWorkitemItemCollection.replaceItemValue(
				"numworkflowLogLength",
				setupMB.getWorkitem().getItemValueInteger(
						"MaxWorkitemHistoryLength"));

		childWorkitemItemCollection.replaceItemValue("$ActivityID", activityID);

		updateDisplayNameFields(childWorkitemItemCollection);

		// inform Listeners...
		fireChildProcessEvent();

		childWorkitemItemCollection = workitemService
				.processWorkItem(childWorkitemItemCollection);

		this.setChildWorkitem(childWorkitemItemCollection);

		// inform Listeners...
		fireChildProcessCompletedEvent();
		childs = null;
	}

	/**
	 * wrapper method to delete a child process.
	 * 
	 * @see doMoveIntoDeletions
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doDeleteChild(ActionEvent event) throws Exception {
		ItemCollection currentSelection = null;
		// suche selektierte Zeile....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// Zeile gefunden
			currentSelection = (ItemCollection) ((UIData) parent).getRowData();
			childWorkitemItemCollection = currentSelection;

			break;
		}

		if (childWorkitemItemCollection != null) {

			// inform Listeners...
			fireChildDeleteEvent();

			if ("childworkitem".equals(childWorkitemItemCollection
					.getItemValueString("type")))
				workitemService.deleteWorkItem(childWorkitemItemCollection);

			doResetChildWorkitems(event);

			// inform Listeners...
			fireChildDeleteCompletedEvent();
		}

	}

	/**
	 * moves a child into the archive by changing the attribute type to
	 * 'workitemdeleted'
	 * 
	 * @see doMoveIntoDeletions
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doSoftDeleteChild(ActionEvent event) throws Exception {
		ItemCollection currentSelection = null;
		// suche selektierte Zeile....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// Zeile gefunden
			currentSelection = (ItemCollection) ((UIData) parent).getRowData();
			childWorkitemItemCollection = currentSelection;

			break;
		}

		if (childWorkitemItemCollection != null) {
			// inform Listeners...
			fireChildSoftDeleteEvent();

			childWorkitemItemCollection = workitemService
					.moveIntoDeletions(childWorkitemItemCollection);

			// inform Listeners...
			fireChildSoftDeleteCompletedEvent();

			doResetChildWorkitems(event);
		}
	}

	/**
	 * This method resets the current Child Selection and set the ChildWorkitem
	 * to null. Also the childs selection will be reset to null
	 * 
	 * @param event
	 * @return
	 */
	public void doResetChildWorkitems(ActionEvent event) throws Exception {
		childWorkitemItemCollection = null;
		// reset Childs
		childs = null;
		this.setChildWorkitem(null);
	}

	/**
	 * This method updates the current $uniqueidref of the acutal workitem. But
	 * did not (!) save the workitem!
	 * 
	 * The method updates the projectname attribute so the workitem can be
	 * displayed with the new settings (Navigation rule: show_workitem)
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doMoveToProject(ActionEvent event) throws Exception {

		// get current project
		ItemCollection itemColProject = projectService
				.findProject(getWorkitem().getItemValueString("$uniqueidRef"));
		projectMB.setWorkitem(itemColProject);

	}

	/**
	 * moves a workitem into the archive by changing the attribute type to
	 * 'workitemarchive'
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doMoveIntoArchive(ActionEvent event) throws Exception {
		ItemCollection workitem = workitemService
				.moveIntoArchive(getWorkitem());
		setWorkitem(workitem);

	}

	/**
	 * moves a workitem into the archive by changing the attribute type to
	 * 'workitemdeleted'
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doMoveIntoDeletions(ActionEvent event) throws Exception {

		ItemCollection workitem = workitemService
				.moveIntoDeletions(getWorkitem());
		setWorkitem(workitem);

	}

	/**
	 * restores a workitem from the archive by changing the attribute type to
	 * 'workitemarchive'
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doRestoreFromArchive(ActionEvent event) throws Exception {
		ItemCollection workitem = workitemService
				.restoreFromArchive(getWorkitem());

		setWorkitem(workitem);

	}

	/**
	 * restores a workitem from the archive by changing the attribute type to
	 * 'workitemarchive'
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doRestoreFromDeletions(ActionEvent event) throws Exception {
		ItemCollection workitem = workitemService
				.restoreFromDeletions(getWorkitem());
		setWorkitem(workitem);

	}

	/**
	 * This method changes the current $ProcessID of the actual workitem. But
	 * did not (!) save the workitem!
	 * 
	 * The method calls the workitemService EJB which also updates the
	 * $ModelVersion, txtWorkflowGroup attribute and the Attribute
	 * txtworkfloweditorid. So the workitem can be displayed with the new editor
	 * (Navigation rule: show_workitem)
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doChangeProcessID(ActionEvent event) throws Exception {

		String aProcessIdentifier = getWorkitem().getItemValueString(
				"txtNewStratProcessEntity");

		/*
		 * System.out
		 * .println(" -------------- doChangeProcessID ----------------- ");
		 */
		String sProcessModelVersion = aProcessIdentifier.substring(0,
				aProcessIdentifier.indexOf('|'));
		String sProcessID = aProcessIdentifier.substring(aProcessIdentifier
				.indexOf('|') + 1);

		ItemCollection itemColProcessEntity = getModelService()
				.getProcessEntityByVersion(Integer.parseInt(sProcessID),
						sProcessModelVersion);

		if (itemColProcessEntity != null) {
			ItemCollection workitem = workitemService.changeProcess(
					getWorkitem(), itemColProcessEntity);
			this.setWorkitem(workitem);

		}

	}

	/**
	 * returns the last workflow result to control the navigation flow if no
	 * result is found open_workitem will be returned.
	 * 
	 * The property 'action' is computed by teh result plugin
	 * 
	 * @return
	 */
	public String getAction() {

		String sResult = getWorkitem().getItemValueString("action");
		if ("".equals(sResult))
			return "open_workitem";
		else
			return sResult;

	}

	/**
	 * returns the process description text from the current processEntity
	 * 
	 * @return a help text
	 */
	public String getProcessDescription() {

		String sModelVersion = getWorkitem()
				.getItemValueString("$modelversion");
		int iPID = getWorkitem().getItemValueInteger("$ProcessID");

		ItemCollection processEntity = this.getModelService()
				.getProcessEntityByVersion(iPID, sModelVersion);
		if (processEntity != null)
			return processEntity.getItemValueString("rtfdescription");

		return "";

	}

	/**
	 * returns a report name provided by teh WorkflowProperty
	 * 'txtworkflowresultmessage'. The report must be defined by the präfix the
	 * tag:
	 * 
	 * <ul>
	 * <li>report=
	 * </ul>
	 * 
	 * If this präfix is available the string followed by 'report=' will be
	 * returned
	 * 
	 * @return
	 */
	public String getWorkflowReport() {

		String sResult = getWorkitem().getItemValueString(
				"txtworkflowresultmessage");
		if (sResult != null && !"".equals(sResult)) {
			// test if result contains "report="
			if (sResult.indexOf("report=") > -1) {
				sResult = sResult.substring(sResult.indexOf("report=") + 7);
				// cut next newLine
				if (sResult.indexOf("\n") > -1)
					sResult = sResult.substring(0, sResult.indexOf("\n"));
			}
			return sResult;
		} else
			return "";
	}

	/***
	 * This method finds the corresponding Project of the curreent workitem and
	 * updates the ProjectMB with this project
	 */
	public void updateProjectMB() {

		String projectID = getWorkitem().getItemValueString("$uniqueidRef");

		if (projectMB.getWorkitem() == null
				|| !projectID.equals(projectMB.getWorkitem()
						.getItemValueString("$uniqueid"))) {
			// update projectMB
			System.out.println("Updating ProjectMB....");
			ItemCollection itemColProject = projectService
					.findProject(projectID);
			projectMB.setWorkitem(itemColProject);
		}
	}

	/**************************************************************************
	 * ModelService Helper Methods
	 *************************************************************************/

	/**
	 * loads a processEntity using a internal caching meachnism used by the
	 * methods loadProcessList and doCreateWorkitem
	 * 
	 * @param processID
	 * @return
	 */
	public ItemCollection loadProcessEntity(String processEntityUnqiueID) {
		ItemCollection itemColProcessEntity = (ItemCollection) processCache
				.get(processEntityUnqiueID);
		if (itemColProcessEntity == null) {
			// process entity not yet in cache
			itemColProcessEntity = getEntityService().load(
					processEntityUnqiueID);
			// .findProcessEntity(processID);
			// put processEntity into cache
			processCache.put(processEntityUnqiueID, itemColProcessEntity);
		}

		return itemColProcessEntity;
	}

	/*************************************************************************
	 * Child Process management
	 *************************************************************************/

	/**
	 * updates all attributes by the supported map into the child ItemCollection
	 * 
	 * Diese Mehtode wird beim Klick auf einen Datensatz aufgerufen
	 * 
	 * @param ateam
	 */
	public void setChildWorkitem(ItemCollection aworkitem) {

		if (aworkitem != null)
			childWorkitemItemCollection = aworkitem;
		else
			childWorkitemItemCollection = new ItemCollection();

	}

	public ItemCollection getChildWorkitem() {
		return childWorkitemItemCollection;
	}

	public ItemCollection getChild() {

		return getChildWorkitem();
	}

	/**
	 * retuns a List with all Child Workitems
	 * 
	 * @return
	 */
	public List<ItemCollection> getChilds() {
		if (childs == null)
			loadChildWorkItemList();
		return childs;
	}

	/**
	 * this method loads the child workitems to the current workitem
	 * 
	 * @see org.imixs.WorkitemService.business.WorkitemServiceBean
	 */
	private void loadChildWorkItemList() {
		childs = new ArrayList<ItemCollection>();
		if (this.isNewWorkitem())
			return;
		Collection<ItemCollection> col = null;
		try {
			String sRefUniqueID = getWorkitem().getItemValueString("$uniqueid");

			col = workitemService.findAllWorkitems(sRefUniqueID, null, null, 0,
					0, -1, getSortby(), getSortorder());
			for (ItemCollection aworkitem : col) {
				childs.add((aworkitem));
			}
		} catch (Exception ee) {
			childs = null;
			ee.printStackTrace();
		}

	}

	/**
	 * returns a arrayList of Activities to the corresponidng processiD of the
	 * current Worktiem. The Method returns the activities corresponding to the
	 * worktiems modelVersionID
	 * 
	 * @return
	 */
	public ArrayList<ItemCollection> getChildActivities() {
		ArrayList<ItemCollection> activityChildList = new ArrayList<ItemCollection>();

		if (childWorkitemItemCollection == null)
			return activityChildList;

		int processId = childWorkitemItemCollection
				.getItemValueInteger("$processid");

		if (processId <= 0)
			return activityChildList;

		String sversion = childWorkitemItemCollection
				.getItemValueString("$modelversion");

		// get Workflow-Activities by version if provided by the workitem
		List<ItemCollection> col;
		if (sversion != null && !"".equals(sversion))
			col = getModelService().getPublicActivitiesByVersion(processId,
					sversion);
		else
			// return activities by defined modelversion
			col = getModelService().getPublicActivitiesByVersion(processId,
					getModelVersion());
		for (ItemCollection aworkitem : col) {
			activityChildList.add((aworkitem));
		}
		return activityChildList;
	}

	/**
	 * returns a List with all Versions of the current Workitem
	 * 
	 * @return
	 */
	public List<ItemCollection> getVersions() {
		if (versions == null)
			loadVersionWorkItemList();
		return versions;
	}

	/**
	 * this method loads all versions to the current workitem
	 * 
	 * @see org.imixs.WorkitemService.business.WorkitemServiceBean
	 */
	private void loadVersionWorkItemList() {
		versions = new ArrayList<ItemCollection>();
		if (this.isNewWorkitem())
			return;
		Collection<ItemCollection> col = null;
		try {
			String sRefID = getWorkitem().getItemValueString("$workitemId");
			String refQuery = "SELECT entity FROM Entity entity "
					+ " JOIN entity.textItems AS t"
					+ "  WHERE entity.type='workitem'"
					+ "  AND t.itemName = '$workitemid'"
					+ "  AND t.itemValue = '" + sRefID + "' "
					+ " ORDER BY entity.created ASC";

			col = this.getEntityService().findAllEntities(refQuery, 0, -1);
			for (ItemCollection aworkitem : col) {
				versions.add(aworkitem);
			}
		} catch (Exception ee) {
			versions = null;
			ee.printStackTrace();
		}

	}

	/*** Action Events ***/

	public synchronized void addWorkitemListener(WorkitemListener l) {
		// Test if the current listener was allreaded added to avoid that a
		// listener register itself more than once!
		if (!workitemListeners.contains(l))
			workitemListeners.add(l);
	}

	public synchronized void removeWorkitemListener(WorkitemListener l) {
		workitemListeners.remove(l);
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireWorkitemCreatedEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onWorkitemCreated(getWorkitem());
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireWorkitemChangedEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onWorkitemChanged(getWorkitem());
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireWorkitemProcessEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onWorkitemProcess(getWorkitem());
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireWorkitemProcessCompletedEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onWorkitemProcessCompleted(getWorkitem());
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireChildProcessEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onChildProcess(childWorkitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireChildProcessCompletedEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onChildProcessCompleted(childWorkitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a deletion of a workitem
	 */
	private void fireWorkitemSoftDeleteEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onWorkitemSoftDelete(getWorkitem());
		}
	}

	/**
	 * Informs WorkitemListeners about a deletion of a workitem
	 */
	private void fireWorkitemSoftDeleteCompletedEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onWorkitemSoftDeleteCompleted(getWorkitem());
		}
	}

	/**
	 * Informs WorkitemListeners about a deletion of a workitem
	 */
	private void fireWorkitemDeleteEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onWorkitemDelete(getWorkitem());
		}
	}

	/**
	 * Informs WorkitemListeners about a deletion of a workitem
	 */
	private void fireWorkitemDeleteCompletedEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onWorkitemDeleteCompleted();
		}
	}

	/**
	 * Informs WorkitemListeners about a deletion of a child process
	 */
	private void fireChildDeleteEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onChildDelete(childWorkitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a deletion of a child process
	 */
	private void fireChildDeleteCompletedEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onChildDeleteCompleted();
		}
	}

	/**
	 * Informs WorkitemListeners about a deletion of a child process
	 */
	private void fireChildSoftDeleteEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onChildSoftDelete(childWorkitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a deletion of a child process
	 */
	private void fireChildSoftDeleteCompletedEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onChildSoftDeleteCompleted(childWorkitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireChildCreatedEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onChildCreated(childWorkitemItemCollection);
		}
	}

	/**
	 * This method adds the displayname of the current user to the following
	 * fields which can be used by history or mail plugin configuration
	 * 
	 * namcreator, namcurrenteditor
	 * 
	 * display names are translated using the NameLookupBean which has an
	 * additional caching feature
	 * 
	 * @param acol
	 */
	private void updateDisplayNameFields(ItemCollection acol) {
		try {
			// get display remote user name by the nameLookupMB
			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext externalContext = context.getExternalContext();
			String remoteUser = externalContext.getRemoteUser();

			// update current editor / remote user
			acol.replaceItemValue("dspnamcurrenteditor",
					this.nameLookupMB.findUserName(remoteUser));

			// test if creator was still right translated
			String sNamCreator = acol.getItemValueString("namCreator");
			String sDspNamCreator = acol.getItemValueString("dspnamCreator");
			if ("".equals(sDspNamCreator)
					|| !this.nameLookupMB.findUserName(sNamCreator).equals(
							sDspNamCreator)) {
				// update dsp name for creator
				acol.replaceItemValue("dspnamCreator",
						this.nameLookupMB.findUserName(sNamCreator));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public int getSortby() {
		return sortby;
	}

	public void setSortby(int sortby) {
		this.sortby = sortby;
	}

	public int getSortorder() {
		return sortorder;
	}

	public void setSortorder(int sortorder) {
		this.sortorder = sortorder;
	}

	/**
	 * Helper class returns a Map containing all EditorSection Objects. The
	 * Class is used by the method EditorSectionIn() to test if a specific
	 * Section is defined
	 * 
	 * <code>
	 *      #{! empty workitemMB.editorSectionIn['prototyp/files']}
	 *         ....
	 * </code>
	 * 
	 * @author rsoika
	 * 
	 */
	class EditorSectionMap extends HashMap {
		ArrayList<EditorSection> sections = getEditorSections();

		public Object get(Object key) {
			EditorSection section = null;
			for (EditorSection aSection : sections) {
				if (aSection.getUrl().equals(key)) {
					section = aSection;
					break;
				}
			}
			return section;
		}

	}
}
