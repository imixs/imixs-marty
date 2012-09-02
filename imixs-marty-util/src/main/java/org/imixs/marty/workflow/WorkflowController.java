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

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;

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

	private static final long serialVersionUID = 1L;

	public final static String DEFAULT_EDITOR_ID = "default";



	private static Logger logger = Logger.getLogger("org.imixs.marty");

	/* Services */
	@EJB
	private org.imixs.marty.ejb.WorkitemService workitemService;

	
	

	public WorkflowController() {
		super();

	}

	@PostConstruct
	public void init() {

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
							/*
							 * if ("owner".equalsIgnoreCase(aPermission) &&
							 * this.projectMB.isProjectOwner()) {
							 * bPermissionGranted = true; break; } if
							 * ("manager".equalsIgnoreCase(aPermission) &&
							 * this.projectMB.isProjectManager()) {
							 * bPermissionGranted = true; break; } if
							 * ("team".equalsIgnoreCase(aPermission) &&
							 * this.projectMB.isProjectTeam()) {
							 * bPermissionGranted = true; break; } if
							 * ("assist".equalsIgnoreCase(aPermission) &&
							 * this.projectMB.isProjectAssist()) {
							 * bPermissionGranted = true; break; }
							 */

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



	

}
