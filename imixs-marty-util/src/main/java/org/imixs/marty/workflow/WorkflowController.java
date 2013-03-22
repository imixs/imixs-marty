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
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.plugins.jee.extended.LucenePlugin;

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

	// private String lastUniqueID = null;

	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_EDITOR_ID = "form_panel_simple#basic";
	private static Logger logger = Logger.getLogger("org.imixs.marty");

	/* Services */
	@EJB
	private org.imixs.marty.ejb.WorkitemService workitemService;

	@Inject
	private ProcessController processController;

	@Inject
	private Event<WorkflowEvent> events;

	public WorkflowController() {
		super();

	}

	/**
	 * create method fires a WorkfowEvent
	 */
	@Override
	public void create(ActionEvent event) {
		super.create(event);

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_CREATED));

	}

	/**
	 * This method provides the additional business information concerning the
	 * assigned project and overrides the default behavior. Finally the method
	 * fires a WorkflowEvent.
	 * 
	 */
	@Override
	public String init(String action) {
		// fetch the assigned project
		String sProjectRef = getWorkitem().getItemValueString("$UniqueidRef");
		if (!"".equals(sProjectRef)) {
			ItemCollection currentProject = this.getEntityService().load(
					sProjectRef);
			if (currentProject != null)
				getWorkitem().replaceItemValue("txtprojectname",
						currentProject.getItemValue("txtprojectname"));
		}

		String actionResult = super.init(action);

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_INITIALIZED));

		return actionResult;
	}

	/**
	 * The action method processes the current workItem and fires a
	 * WorkflowEvent.
	 */
	@Override
	public String process() throws AccessDeniedException,
			ProcessingErrorException {

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_BEFORE_PROCESS));

		String actionResult = super.process();

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_AFTER_PROCESS));

		return actionResult;
	}

	/**
	 * Fires a WORKITEM_CHANGED event if the $uniqueID of the current WorkItem
	 * has changed
	 */
	@Override
	public void setWorkitem(ItemCollection aworkitem) {
		super.setWorkitem(aworkitem);
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_CHANGED));
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

		if ("".equals(sEditor))
			sEditor = DEFAULT_EDITOR_ID;

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
	public ArrayList<EditorSection> getEditorSections() {
		ArrayList<EditorSection> sections = new ArrayList<EditorSection>();

		UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
		Locale locale = viewRoot.getLocale();

		String sEditor = getWorkitem()
				.getItemValueString("txtWorkflowEditorid");
		if ("".equals(sEditor))
			sEditor = DEFAULT_EDITOR_ID;

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
							String sProjectUniqueID = this.getWorkitem()
									.getItemValueString("$UniqueIDRef");

							if ("manager".equalsIgnoreCase(aPermission)
									&& processController
											.isProjectManager(sProjectUniqueID)) {
								bPermissionGranted = true;
								break;
							}
							if ("team".equalsIgnoreCase(aPermission)
									&& this.processController
											.isProjectTeam(sProjectUniqueID)) {
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
							rb = ResourceBundle.getBundle("bundle.app", locale);
						else
							rb = ResourceBundle.getBundle("bundle.app");

						String sResouceURL = sURL.replace('/', '_');
						sName = rb.getString(sResouceURL);
					} catch (java.util.MissingResourceException eb) {
						sName = "";
						logger.warning(eb.getMessage());
					}

					EditorSection aSection = new EditorSection(sURL, sName);
					sections.add(aSection);

				} catch (Exception est) {
					logger.severe("[WorkitemController] can not parse EditorSections : '"
							+ sEditor + "'");
					logger.severe(est.getMessage());
				}
			}
		}

		return sections;

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

		workitemService.moveIntoDeletions(getWorkitem());

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

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_BEFORE_ARCHIVE));

		ItemCollection workitem = workitemService
				.moveIntoArchive(getWorkitem());
		
		// update lucene index
		LucenePlugin.addWorkitem(workitem);
				
		setWorkitem(workitem);
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_AFTER_ARCHIVE));

	}

	/**
	 * moves a workitem into the archive by changing the attribute type to
	 * 'workitemdeleted'.
	 * The workitem will be excluded from the Lucene search index.
	 * 
	 * 
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doMoveIntoDeletions(ActionEvent event) throws Exception {

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_BEFORE_SOFTDELETE));

		ItemCollection workitem = workitemService
				.moveIntoDeletions(getWorkitem());
				
		// update lucene index
		LucenePlugin.addWorkitem(workitem);		
		
		setWorkitem(workitem);

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_AFTER_SOFTDELETE));

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
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_BEFORE_RESTOREFROMARCHIVE));

		ItemCollection workitem = workitemService
				.restoreFromArchive(getWorkitem());

		// update lucene index
		LucenePlugin.addWorkitem(workitem);
				
		setWorkitem(workitem);
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_AFTER_RESTOREFROMARCHIVE));

	}

	/**
	 * restores a workitem from the archive by changing the attribute type to
	 * 'workitemarchive'
	 * 
	 * The workitem will be added into the Lucene search index.
	 * 
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doRestoreFromDeletions(ActionEvent event) throws Exception {

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_BEFORE_RESTOREFROMSOFTDELETE));

		ItemCollection workitem = workitemService
				.restoreFromDeletions(getWorkitem());
			
		// update lucene index
		LucenePlugin.addWorkitem(workitem);
		
		setWorkitem(workitem);		
		
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(),
				WorkflowEvent.WORKITEM_AFTER_RESTOREFROMSOFTDELETE));

	}

}
