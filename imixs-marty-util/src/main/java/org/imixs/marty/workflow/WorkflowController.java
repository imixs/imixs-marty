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

package org.imixs.marty.workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.model.ProcessController;
import org.imixs.marty.util.ErrorHandler;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;

/**
 * The marty WorkflowController extends the
 * org.imixs.workflow.jee.faces.workitem.WorkflowController and provides
 * additional functionality to manage workitems.
 * 
 * The String property 'project' is used to assign the workItem to a project
 * entity. The property contains the $UniqueId of the corresponding project. If
 * the 'project' property is not set a new workItem can not be processed or a
 * new workItem can not be created.
 * 
 * The marty WorkflowController provides an editor selector which allows to
 * split parts of a form in separate sections (see formpanel.xhmtl,
 * tabpanel.xhmtl)
 * 
 * @author rsoika
 * 
 */
@Named("workflowController")
@SessionScoped
public class WorkflowController extends
		org.imixs.workflow.jee.faces.workitem.WorkflowController implements
		Serializable {

	public static final String DEFAULT_EDITOR_ID = "form_panel_simple#basic";

	/* Services */
	@EJB
	protected org.imixs.marty.ejb.WorkitemService workitemService;

	@Inject
	protected ProcessController processController;

	@Inject
	protected Event<WorkflowEvent> events;

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(WorkflowController.class
			.getName());

	private List<ItemCollection> versions = null;
	private List<EditorSection> editorSections = null;

	public WorkflowController() {
		super();

	}

	/**
	 * ActionListener create a new empty workitem and fires a WorkfowEvent
	 */
	@Override
	public void create(ActionEvent event) {
		super.create(event);
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_CREATED));

	}

	/**
	 * Method to create a new workitem with inital values. The method fires a
	 * WorkfowEvent
	 * 
	 * @param modelVersion
	 *            - model version
	 * @param processID
	 *            - processID
	 * @param processRef
	 *            - uniqueid ref
	 */

	public void create(String modelVersion, int processID, String processRef) {
		super.create(null);

		getWorkitem().replaceItemValue("$ModelVersion", modelVersion);
		getWorkitem().replaceItemValue("$ProcessID", processID);
		getWorkitem().replaceItemValue("$UniqueIDRef", processRef);

		// find process
		ItemCollection process = processController.getProcessById(processRef);
		if (process != null) {
			String sNewProcessName = process.getItemValueString("txtName");
			getWorkitem().replaceItemValue("txtProcessName", sNewProcessName);

		} else {
			logger.warning("[WorkflowController] create - unable to find process entity '"
					+ processRef + "'!");
		}

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_CREATED));

	}

	/**
	 * This method overwrites the default init() and fires a WorkflowEvent.
	 * 
	 */
	@Override
	public String init(String action) {
		String actionResult = super.init(action);

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_INITIALIZED));

		return actionResult;
	}

	/**
	 * The method fires a WORKITEM_CHANGED event if the uniqueId of the workitem
	 * has changed. Additional the method resets the editoSection list and
	 * version list.
	 */
	@Override
	public void setWorkitem(ItemCollection newWorkitem) {

		events.fire(new WorkflowEvent(newWorkitem,
				WorkflowEvent.WORKITEM_CHANGED));

		super.setWorkitem(newWorkitem);

		versions = null;
		editorSections = null;

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

		String sEditor = DEFAULT_EDITOR_ID;

		if (getWorkitem() != null) {
			String currentEditor = getWorkitem().getItemValueString(
					"txtWorkflowEditorid");
			if (!currentEditor.isEmpty())
				sEditor = currentEditor;
		}

		// test if # is provides to indicate optional section
		// informations
		if (sEditor.indexOf('#') > -1)
			sEditor = sEditor.substring(0, sEditor.indexOf('#'));
		return sEditor;

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
	public List<EditorSection> getEditorSections() {

		if (editorSections == null) {
			// compute editorSections
			editorSections = new ArrayList<EditorSection>();

			UIViewRoot viewRoot = FacesContext.getCurrentInstance()
					.getViewRoot();
			Locale locale = viewRoot.getLocale();

			String sEditor = DEFAULT_EDITOR_ID;

			if (getWorkitem() != null) {
				String currentEditor = getWorkitem().getItemValueString(
						"txtWorkflowEditorid");
				if (!currentEditor.isEmpty())
					sEditor = currentEditor;
			}

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
										.getCurrentInstance()
										.getExternalContext();
								if (ectx.isUserInRole(aPermission)) {
									bPermissionGranted = true;
									break;
								}
								// test if user is project member
								String sProjectUniqueID = this.getWorkitem()
										.getItemValueString("$UniqueIDRef");

								if ("manager".equalsIgnoreCase(aPermission)
										&& processController
												.isManagerOf(sProjectUniqueID)) {
									bPermissionGranted = true;
									break;
								}
								if ("team".equalsIgnoreCase(aPermission)
										&& this.processController
												.isTeamMemberOf(sProjectUniqueID)) {
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
								rb = ResourceBundle.getBundle("bundle.app",
										locale);
							else
								rb = ResourceBundle.getBundle("bundle.app");

							String sResouceURL = sURL.replace('/', '_');
							sName = rb.getString(sResouceURL);
						} catch (java.util.MissingResourceException eb) {
							sName = "";
							logger.warning(eb.getMessage());
						}

						EditorSection aSection = new EditorSection(sURL, sName);
						editorSections.add(aSection);

					} catch (Exception est) {
						logger.severe("[WorkitemController] can not parse EditorSections : '"
								+ sEditor + "'");
						logger.severe(est.getMessage());
					}
				}
			}
		}

		return editorSections;

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
	 * The action method processes the current workItem and fires the
	 * WorkflowEvents WORKITEM_BEFORE_PROCESS and WORKITEM_AFTER_PROCESS.
	 * 
	 * The Method also catches a PluginException and adds the corresponding
	 * Faces Error Message into the FacesContext. If the PluginException was
	 * thrown from the RulePLugin then the method test this exception for
	 * ErrorParams with will generate separate Faces Error Messages.
	 */
	@Override
	public String process() throws AccessDeniedException,
			ProcessingErrorException {
		String actionResult = null;
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_BEFORE_PROCESS));

		// process workItem and catch exceptions
		try {
			actionResult = super.process();
		} catch (PluginException pe) {
			// add a new FacesMessage into the FacesContext
			ErrorHandler.handlePluginException(pe);
		}
		// reset versions and editor sections
		versions = null;
		editorSections = null;
		// ! Do not call setWorkitem here because this fires a WORKITEM_CHANGED
		// event !

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_AFTER_PROCESS));

		return actionResult;
	}

	/**
	 * This method moves a workitem into the archive by appending the sufix
	 * 'archive' to the attribute type. The Lucene search index will be
	 * automatically updated by the workitemService.
	 * 
	 * The param 'recursive' indicates if also references of the current
	 * workitem should be updated.
	 * 
	 * @param recursive
	 *            - if true also refrences will be updated
	 * @throws AccessDeniedException
	 */
	public void archiveWorkitem(boolean recursive) throws AccessDeniedException {

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_BEFORE_ARCHIVE));

		ItemCollection workitem = workitemService.archiveWorkitem(
				getWorkitem(), recursive);

		setWorkitem(workitem);
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_AFTER_ARCHIVE));

	}

	/**
	 * The method soft deletes a workitem by appending the sufix 'deleted' to
	 * the attribute type. The Lucene search index will be automatically updated
	 * by the workitemService.
	 * 
	 * The param 'recursive' indicates if also references of the current
	 * workitem should be updated.
	 * 
	 * @param recursive
	 *            - if true also refrences will be updated
	 * @throws AccessDeniedException
	 */
	public void softDeleteWorkitem(boolean recursive)
			throws AccessDeniedException {

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_BEFORE_SOFTDELETE));

		ItemCollection workitem = workitemService.softDeleteWorkitem(
				getWorkitem(), recursive);

		setWorkitem(workitem);

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_AFTER_SOFTDELETE));

	}

	/**
	 * This actionListener method moves a workitem into the archive by appending
	 * the sufix 'archive' to the attribute type. References to this workitem
	 * will be updated.
	 * 
	 * The Lucene search index will be automatically updated by the
	 * workitemService.
	 * 
	 * @param event
	 * @throws AccessDeniedException
	 */
	public void doArchiveWorkitem(ActionEvent event)
			throws AccessDeniedException {
		// call default behavior
		archiveWorkitem(true);
	}

	/**
	 * This actionListener method soft deletes a workitem by appending the sufix
	 * 'deleted' to the attribute type. References to this workitem will be
	 * updated.
	 * 
	 * The Lucene search index will be automatically updated by the
	 * workitemService.
	 * 
	 * @param event
	 * @throws AccessDeniedException
	 */
	public void doSoftDeleteWorkitem(ActionEvent event)
			throws AccessDeniedException {
		// call default behavior
		softDeleteWorkitem(true);
	}

	/**
	 * This method restores a workitem from the archive by changing the
	 * attribute type to 'workitemarchive'. The Lucene search index will be
	 * automatically updated by the workitemService.
	 * 
	 * @param event
	 * @throws AccessDeniedException
	 */
	public void doRestoreFromArchive(ActionEvent event)
			throws AccessDeniedException {
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_BEFORE_RESTOREFROMARCHIVE));

		ItemCollection workitem = workitemService
				.restoreFromArchive(getWorkitem());

		setWorkitem(workitem);
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_AFTER_RESTOREFROMARCHIVE));

	}

	/**
	 * This method restores a workitem from the archive by changing the
	 * attribute type to 'workitemarchive'. The Lucene search index will be
	 * automatically updated by the workitemService.
	 * 
	 * @param event
	 * @throws AccessDeniedException
	 */
	public void doRestoreFromDeletions(ActionEvent event)
			throws AccessDeniedException {

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_BEFORE_RESTOREFROMSOFTDELETE));

		ItemCollection workitem = workitemService
				.restoreFromDeletions(getWorkitem());

		setWorkitem(workitem);

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_AFTER_RESTOREFROMSOFTDELETE));

	}

	/**
	 * this method loads all versions to the current workitem. Idependent from
	 * the type property! The method returns an empty list if no version exist
	 * (only the main version)
	 * 
	 * @see org.imixs.WorkitemService.business.WorkitemServiceBean
	 */
	private void loadVersionWorkItemList() {
		versions = new ArrayList<ItemCollection>();
		if (this.isNewWorkitem() || null == getWorkitem())
			return;
		Collection<ItemCollection> col = null;
		String sRefID = getWorkitem().getItemValueString("$workitemId");
		String refQuery = "SELECT entity FROM Entity entity "
				+ " JOIN entity.textItems AS t"
				+ "  WHERE t.itemName = '$workitemid'"
				+ "  AND t.itemValue = '" + sRefID + "' "
				+ " ORDER BY entity.modified ASC";

		col = this.getEntityService().findAllEntities(refQuery, 0, -1);

		// Only return version list if more than one version was found!
		if (col.size() > 1) {
			for (ItemCollection aworkitem : col) {
				versions.add(aworkitem);
			}
		}
		
		

	}

}
