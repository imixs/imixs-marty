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
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
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
import org.imixs.workflow.jee.faces.AbstractWorkflowController;

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
public class WorkitemMB extends AbstractWorkflowController {

	public final static String DEFAULT_EDITOR_ID = "default";

	/* Workflow Model & Caching objects */
	private HashMap processCache;

	/* Project Backing Bean */
	private ProjectMB projectBean = null;
	private WorklistMB worklistBean = null;
	private SetupMB setupMB = null;
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
	}

	/**
	 * returns an instance of the PorjectMB
	 * 
	 * @return
	 */
	public ProjectMB getProjectBean() {
		if (projectBean == null)
			projectBean = (ProjectMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "projectMB");
		return projectBean;
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

	/**
	 * returns an instance of the WorklitsMB
	 * 
	 * @return
	 */
	public WorklistMB getWorklistBean() {
		if (worklistBean == null)
			worklistBean = (WorklistMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "worklistMB");
		return worklistBean;
	}

	/**
	 * returns an instance of the NameLookupMB
	 * 
	 * @return
	 */
	public NameLookupMB getNameLookupBean() {
		if (nameLookupMB == null)
			nameLookupMB = (NameLookupMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "nameLookupMB");
		return nameLookupMB;
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
		if (workitemItemCollection != null) {
			String sEditor = workitemItemCollection
					.getItemValueString("txtWorkflowEditorid");
			if (!"".equals(sEditor)) {
				// test if # is provides to indicate optinal section
				// informations
				if (sEditor.indexOf('#') > -1)
					sEditor = sEditor.substring(0, sEditor.indexOf('#'));
				return sEditor;
			} else
				return DEFAULT_EDITOR_ID;
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

		if (workitemItemCollection != null) {

			UIViewRoot viewRoot = FacesContext.getCurrentInstance()
					.getViewRoot();
			Locale locale = viewRoot.getLocale();

			String sEditor = workitemItemCollection
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
							sURL = sURL.substring(0,sURL.indexOf('['));
							StringTokenizer stPermission = new StringTokenizer(
									sPermissions, ",");
							while (stPermission.hasMoreTokens()) {
								String aPermission = stPermission.nextToken();
								// test for user role
								ExternalContext ectx = FacesContext
										.getCurrentInstance()
										.getExternalContext();
								if (ectx.isUserInRole(aPermission)) {
									bPermissionGranted = true;
									break;
								}
								// test if user is project member
								if ("owner".equalsIgnoreCase(aPermission)
										&& this.getProjectBean()
												.isProjectOwner()) {
									bPermissionGranted = true;
									break;
								}
								if ("manager".equalsIgnoreCase(aPermission)
										&& this.getProjectBean()
												.isProjectManager()) {
									bPermissionGranted = true;
									break;
								}
								if ("team".equalsIgnoreCase(aPermission)
										&& this.getProjectBean()
												.isProjectTeam()) {
									bPermissionGranted = true;
									break;
								}
								if ("assist".equalsIgnoreCase(aPermission)
										&& this.getProjectBean()
												.isProjectAssist()) {
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
				ItemCollection currentProject = this.getProjectBean()
						.getEntityService().load(projectIdentifier);
				getProjectBean().setWorkitem(currentProject);
				// reset worklist
				getProjectBean().getworkListMB().doReset(null);
			}

			// find ProcessEntity the Worktiem should be started at
			String sProcessModelVersion = processEntityIdentifier.substring(0,
					processEntityIdentifier.indexOf('|'));
			String sProcessID = processEntityIdentifier
					.substring(processEntityIdentifier.indexOf('|') + 1);

			workitemItemCollection = workitemService.createWorkItem(
					getProjectBean().getWorkitem(), sProcessModelVersion,
					Integer.parseInt(sProcessID));

			this.setWorkitem(workitemItemCollection);

			// inform Listeners...
			fireWorkitemCreatedEvent();

			getWorklistBean().doReset(null);

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

		// remove a4j: attributes generated inside the viewentries by the UI
		workitemItemCollection.removeItem("a4j:showhistory");
		workitemItemCollection.removeItem("a4j:showdetails");

		// remove last workflow result properties......
		workitemItemCollection.removeItem("action");
		workitemItemCollection.removeItem("project");

		// set max History & log length
		workitemItemCollection.replaceItemValue(
				"numworkflowHistoryLength",
				getConfigBean().getWorkitem().getItemValueInteger(
						"MaxWorkitemHistoryLength"));
		workitemItemCollection.replaceItemValue(
				"numworkflowLogLength",
				getConfigBean().getWorkitem().getItemValueInteger(
						"MaxWorkitemHistoryLength"));

		workitemItemCollection.replaceItemValue("$ActivityID", activityID);

		updateDisplayNameFields(workitemItemCollection);

		// inform Listeners...
		fireWorkitemProcessEvent();

		workitemItemCollection = workitemService
				.processWorkItem(workitemItemCollection);

		// inform Listeners...
		fireWorkitemProcessCompletedEvent();
		// update workitemcollection and reset childs
		this.setWorkitem(workitemItemCollection);
		getWorklistBean().doRefresh(event);
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
		if (workitemItemCollection != null) {
			// inform Listeners...
			fireWorkitemDeleteEvent();

			workitemService.deleteWorkItem(workitemItemCollection);
			this.setWorkitem(null);
			// getFileUploadMB().reset();

			// inform Listeners...
			fireWorkitemDeleteCompletedEvent();

			getWorklistBean().doRefresh(event);
		}
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
		if (workitemItemCollection != null) {

			// inform Listeners...
			fireWorkitemSoftDeleteEvent();

			workitemItemCollection = workitemService
					.moveIntoDeletions(workitemItemCollection);

			// inform Listeners...
			fireWorkitemSoftDeleteCompletedEvent();

			getWorklistBean().doRefresh(event);
		}
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
					workitemItemCollection, sProcessModelVersion,
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
				"numworkflowHistoryLength", getConfigBean().getWorkitem()
						.getItemValueInteger("MaxWorkitemHistoryLength"));
		childWorkitemItemCollection.replaceItemValue(
				"numworkflowLogLength",
				getConfigBean().getWorkitem().getItemValueInteger(
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

			if ("childworkitem".equals(childWorkitemItemCollection.getItemValueString("type")))
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
				.findProject(workitemItemCollection
						.getItemValueString("$uniqueidRef"));
		getProjectBean().setWorkitem(itemColProject);

		getWorklistBean().doReset(event);
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

		if (workitemItemCollection != null) {

			// remove a4j: attributes generated inside the viewentries by the UI
			workitemItemCollection.getAllItems().remove("a4j:showhistory");
			workitemItemCollection.getAllItems().remove("a4j:showdetails");

			workitemItemCollection = workitemService
					.moveIntoArchive(workitemItemCollection);
			this.getWorklistBean().doRefresh(event);
		}
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
		if (workitemItemCollection != null) {

			// remove a4j: attributes generated inside the viewentries by the UI
			workitemItemCollection.getAllItems().remove("a4j:showhistory");
			workitemItemCollection.getAllItems().remove("a4j:showdetails");

			workitemItemCollection = workitemService
					.moveIntoDeletions(workitemItemCollection);
			this.getWorklistBean().doRefresh(event);

		}
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
		if (workitemItemCollection != null) {

			// remove a4j: attributes generated inside the viewentries by the UI
			workitemItemCollection.getAllItems().remove("a4j:showhistory");
			workitemItemCollection.getAllItems().remove("a4j:showdetails");

			workitemItemCollection = workitemService
					.restoreFromArchive(workitemItemCollection);

			this.getWorklistBean().doRefresh(event);
		}
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
		if (workitemItemCollection != null) {
			// remove a4j: attributes generated inside the viewentries by the UI
			workitemItemCollection.getAllItems().remove("a4j:showhistory");
			workitemItemCollection.getAllItems().remove("a4j:showdetails");
			workitemItemCollection = workitemService
					.restoreFromDeletions(workitemItemCollection);

			this.getWorklistBean().doRefresh(event);
		}
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

		String aProcessIdentifier = workitemItemCollection
				.getItemValueString("txtNewStratProcessEntity");

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
			workitemItemCollection = workitemService.changeProcess(
					workitemItemCollection, itemColProcessEntity);
			this.setWorkitem(workitemItemCollection);
			this.getWorklistBean().doRefresh(event);
		}

	}

	/**
	 * this method toogles the ajax attribute a4j:showHistory. This Attribute is
	 * used to display the History inside the Workitem Form.
	 * 
	 * 
	 **/
	public void doToggleHistory(ActionEvent event) throws Exception {

		// now try to read current toogle state and switch state
		boolean bTogle = false;
		if (workitemItemCollection.hasItem("a4j:showHistory")) {
			try {
				boolean bTogleCurrent = (Boolean) workitemItemCollection
						.getItemValue("a4j:showHistory").get(0);
				bTogle = !bTogleCurrent;
			} catch (Exception e) {
				bTogle = true;
			}
		} else
			// item did not exist yet....
			bTogle = true;
		workitemItemCollection.replaceItemValue("a4j:showHistory", bTogle);

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
		if (workitemItemCollection == null)
			return "open_workitem";

		String sResult = workitemItemCollection.getItemValueString("action");
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
		if (workitemItemCollection == null)
			return "";
		try {
			String sModelVersion = this.workitemItemCollection
					.getItemValueString("$modelversion");
			int iPID = this.workitemItemCollection
					.getItemValueInteger("$ProcessID");

			ItemCollection processEntity = this.getModelService()
					.getProcessEntityByVersion(iPID, sModelVersion);
			if (processEntity != null)
				return processEntity.getItemValueString("rtfdescription");
		} catch (Exception e) {

			e.printStackTrace();

		}
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
		if (workitemItemCollection == null)
			return "";

		String sResult = workitemItemCollection
				.getItemValueString("txtworkflowresultmessage");
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

	/**
	 * returns a project name provided by the WorklfowProperty
	 * 'txtworkflowresultmessage'. The project name must be defined by the
	 * präfix the tag:
	 * 
	 * <ul>
	 * <li>project=
	 * </ul>
	 * 
	 * If this präfix is available the string followed by 'project=' will be
	 * returned
	 * 
	 * @return
	 */
	/*
	 * <code> public String getWorkflowProject() { if (workitemItemCollection ==
	 * null) return "";
	 * 
	 * String sResult = workitemItemCollection
	 * .getItemValueString("txtworkflowresultmessage"); if (sResult != null &&
	 * !"".equals(sResult)) { // test if result contains "report=" if
	 * (sResult.indexOf("project=") > -1) { sResult =
	 * sResult.substring(sResult.indexOf("project=") + 8); // cut next newLine
	 * if (sResult.indexOf("\n") > -1) sResult = sResult.substring(0,
	 * sResult.indexOf("\n")); } return sResult; } else return ""; } </code>
	 */
	/***
	 * This method finds the corresponding Project of the curreent workitem and
	 * updates the ProjectMB with this project
	 */
	public void updateProjectMB() {
		if (workitemItemCollection == null)
			return;

		String projectID = workitemItemCollection
				.getItemValueString("$uniqueidRef");

		if (getProjectBean().getWorkitem() == null
				|| !projectID.equals(getProjectBean().getWorkitem()
						.getItemValueString("$uniqueid"))) {
			// update projectMB
			System.out.println("Updating ProjectMB....");
			ItemCollection itemColProject = projectService
					.findProject(projectID);
			getProjectBean().setWorkitem(itemColProject);
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
			String sRefUniqueID = workitemItemCollection
					.getItemValueString("$uniqueid");

			col = workitemService.findAllWorkitems(sRefUniqueID, null, null, 0,
					0, -1, this.getWorklistBean().getSortby(), this
							.getWorklistBean().getSortorder());
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
	 * this method loads all versions to the current workitem. Idependent from the type property!
	 * 
	 * @see org.imixs.WorkitemService.business.WorkitemServiceBean
	 */
	private void loadVersionWorkItemList() {
		versions = new ArrayList<ItemCollection>();
		if (this.isNewWorkitem())
			return;
		Collection<ItemCollection> col = null;
		try {
			String sRefID = workitemItemCollection
					.getItemValueString("$workitemId");
			String refQuery = "SELECT entity FROM Entity entity "
					+ " JOIN entity.textItems AS t"
					+ "  WHERE t.itemName = '$workitemid'"
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
			l.onWorkitemCreated(workitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireWorkitemChangedEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onWorkitemChanged(workitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireWorkitemProcessEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onWorkitemProcess(workitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a new or updated Workitem
	 */
	private void fireWorkitemProcessCompletedEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onWorkitemProcessCompleted(workitemItemCollection);
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
			l.onWorkitemSoftDelete(workitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a deletion of a workitem
	 */
	private void fireWorkitemSoftDeleteCompletedEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onWorkitemSoftDeleteCompleted(workitemItemCollection);
		}
	}

	/**
	 * Informs WorkitemListeners about a deletion of a workitem
	 */
	private void fireWorkitemDeleteEvent() {
		for (Iterator<WorkitemListener> i = workitemListeners.iterator(); i
				.hasNext();) {
			WorkitemListener l = i.next();
			l.onWorkitemDelete(workitemItemCollection);
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
			acol.replaceItemValue("dspnamcurrenteditor", this
					.getNameLookupBean().findUserName(remoteUser));

			// test if creator was still right translated
			String sNamCreator = acol.getItemValueString("namCreator");
			String sDspNamCreator = acol.getItemValueString("dspnamCreator");
			if ("".equals(sDspNamCreator)
					|| !this.getNameLookupBean().findUserName(sNamCreator)
							.equals(sDspNamCreator)) {
				// update dsp name for creator
				acol.replaceItemValue("dspnamCreator", this.getNameLookupBean()
						.findUserName(sNamCreator));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

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
