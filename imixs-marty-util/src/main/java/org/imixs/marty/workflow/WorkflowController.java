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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.ObserverException;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.model.ProcessController;
import org.imixs.marty.util.ErrorHandler;
import org.imixs.marty.util.ValidationException;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.jee.faces.util.LoginController;

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
 * @version 2.0
 */
@Named
@ConversationScoped
public class WorkflowController extends org.imixs.workflow.jee.faces.workitem.WorkflowController
		implements Serializable {

	public static final String DEFAULT_EDITOR_ID = "form_panel_simple#basic";
	public static final String DEFAULT_ACTION_RESULT = "/pages/workitems/workitem";

	@Inject
	private Conversation conversation;

	/* Services */
	@EJB
	protected org.imixs.marty.ejb.WorkitemService workitemService;

	@Inject
	protected ProcessController processController;

	@Inject
	protected LoginController loginController = null;

	@Inject
	protected Event<WorkflowEvent> events;

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(WorkflowController.class.getName());

	private List<ItemCollection> versions = null;
	private List<EditorSection> editorSections = null;
	private String action = null; // optional page result
	private String deepLinkId = null; // deep link UniqueId
	private String defaultActionResult = null;

	public WorkflowController() {
		super();
	}

	/**
	 * Defines an optinal Action Result used by the process method
	 */
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getDefaultActionResult() {
		if (defaultActionResult == null) {
			defaultActionResult = DEFAULT_ACTION_RESULT;
		}
		return defaultActionResult;
	}

	public void setDefaultActionResult(String defultActionResult) {
		this.defaultActionResult = defultActionResult;
	}

	/**
	 * Loads a workitem by its ID. The method starts a new conversation context.
	 */
	@Override
	public void load(String uniqueID) {

		if (conversation.isTransient()) {
			conversation.begin();
			logger.fine("start new conversation, id=" + conversation.getId());
		}
		super.load(uniqueID);

	}

	/**
	 * ActionListener create a new empty workitem and fires a WorkfowEvent. The
	 * method starts a new conversation context.
	 */
	@Override
	public void create(ActionEvent event) {
		if (conversation.isTransient()) {
			conversation.begin();
			logger.fine("start new conversation, id=" + conversation.getId());
		}
		super.create();
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_CREATED));

	}

	/**
	 * Method to create a new workitem with inital values. The method fires a
	 * WorkfowEvent. The method starts a new conversation context.
	 * 
	 * This method also set an empyt $workitemID field and the namowner field to
	 * the current user. This is used in case that a workitem is not processed
	 * but save (see dmsController save). In such a case the workitem is asigned
	 * to the current user.
	 * 
	 * @param modelVersion
	 *            - model version
	 * @param processID
	 *            - processID
	 * @param processRef
	 *            - uniqueid ref
	 */

	public void create(String modelVersion, int processID, String processRef) {
		if (conversation.isTransient()) {
			conversation.begin();
			logger.fine("start new conversation, id=" + conversation.getId());
		}
		super.create();

		getWorkitem().replaceItemValue("$ModelVersion", modelVersion);
		getWorkitem().replaceItemValue("$ProcessID", processID);

		// set default owner
		getWorkitem().replaceItemValue("namowner", loginController.getUserPrincipal());

		// set empty $workitemid
		getWorkitem().replaceItemValue("$workitemid", "");

		if (processRef != null) {
			getWorkitem().replaceItemValue("$UniqueIDRef", processRef);
			// find process
			ItemCollection process = processController.getProcessById(processRef);
			if (process != null) {
				getWorkitem().replaceItemValue("txtProcessName", process.getItemValueString("txtName"));
				getWorkitem().replaceItemValue("txtProcessRef", process.getItemValueString(EntityService.UNIQUEID));

			} else {
				logger.warning("[WorkflowController] create - unable to find process entity '" + processRef + "'!");
			}
		}
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_CREATED));

	}

	/**
	 * Reset the current workitem. The method closes the existing conversation
	 * context.
	 */
	@Override
	public void reset() {
		super.reset();
		if (!conversation.isTransient()) {
			logger.fine("close conversation, id=" + conversation.getId());
			conversation.end();
		}

	}

	/**
	 * This method reset the worktiem, closes the current conversation and
	 * navigation to the home page.
	 * 
	 * @return
	 */
	public String close() {
		this.reset();
		return "pages/home?faces-redirect=true";
	}

	/**
	 * This method overwrites the default init() and fires a WorkflowEvent.
	 * 
	 * @throws ModelException
	 * 
	 */
	@Override
	public String init(String action) throws ModelException {
		String actionResult = super.init(action);

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_INITIALIZED));

		return actionResult;
	}

	/**
	 * Loades a new wokitem by a uniqueID
	 * 
	 * @param aUniqueID
	 */
	public void setUniqueId(String uniqueID) {
		this.load(uniqueID);
	}

	/**
	 * Returns the current uniqueid of the workitem or null if no workitem is
	 * set.
	 * 
	 * @return uniqueid of current workitem or null
	 */
	public String getUniqueId() {
		if (this.getWorkitem() == null) {
			return null;
		} else {
			return this.getWorkitem().getItemValueString(EntityService.UNIQUEID);
		}
	}

	/**
	 * The method loads a new wokitem by a uniqueID. If no id is provided the
	 * method did not change the current workitem reference. If the uniqueId is
	 * invalid the workitem will be set to null (see setUnqiueId)
	 * 
	 * This method is used for the DeepLink Feature used by workitem.xhml.
	 * 
	 * 
	 * @param aUniqueID
	 */
	public void setDeepLinkId(String adeepLinkId) {
		this.deepLinkId = adeepLinkId;
		// if Id is provided try to load the corresponding workitem.
		if (deepLinkId != null && !deepLinkId.isEmpty()) {
			this.load(deepLinkId);
			// finally we destroy the deepLinkId to avoid a reload on the next
			// postback
			deepLinkId = null; // !
		}
	}

	public String getDeepLinkId() {
		return deepLinkId;
	}

	/**
	 * The method fires a WORKITEM_CHANGED event if the uniqueId of the workitem
	 * has changed. Additional the method resets the editoSection list and
	 * version list.
	 */
	@Override
	public void setWorkitem(ItemCollection newWorkitem) {

		events.fire(new WorkflowEvent(newWorkitem, WorkflowEvent.WORKITEM_CHANGED));

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
			String currentEditor = getWorkitem().getItemValueString("txtWorkflowEditorid");
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

			UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
			Locale locale = viewRoot.getLocale();

			String sEditor = DEFAULT_EDITOR_ID;

			if (getWorkitem() != null) {
				String currentEditor = getWorkitem().getItemValueString("txtWorkflowEditorid");
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
							String sPermissions = sURL.substring(sURL.indexOf('[') + 1, sURL.indexOf(']'));

							// cut the permissions from the URL
							sURL = sURL.substring(0, sURL.indexOf('['));
							StringTokenizer stPermission = new StringTokenizer(sPermissions, ",");
							while (stPermission.hasMoreTokens()) {
								String aPermission = stPermission.nextToken();
								// test for user role
								ExternalContext ectx = FacesContext.getCurrentInstance().getExternalContext();
								if (ectx.isUserInRole(aPermission)) {
									bPermissionGranted = true;
									break;
								}
								// test if user is project member
								String sProjectUniqueID = this.getWorkitem().getItemValueString("$UniqueIDRef");

								if ("manager".equalsIgnoreCase(aPermission)
										&& processController.isManagerOf(sProjectUniqueID)) {
									bPermissionGranted = true;
									break;
								}
								if ("team".equalsIgnoreCase(aPermission)
										&& this.processController.isTeamMemberOf(sProjectUniqueID)) {
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
						editorSections.add(aSection);

					} catch (Exception est) {
						logger.severe("[WorkitemController] can not parse EditorSections : '" + sEditor + "'");
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
	 * WorkflowEvents WORKITEM_BEFORE_PROCESS and WORKITEM_AFTER_PROCESS. The
	 * Method also catches PluginExceptions and adds the corresponding Faces
	 * Error Message into the FacesContext. In case of an exception the
	 * WorkflowEvent WORKITEM_AFTER_PROCESS will not be fired. <br>
	 * The action result returned by the workflow engine may contain a $uniqueid
	 * to redirect the user and load that new workitem. If no action result is
	 * defined the method redirects to the default action with the current
	 * workitem id.
	 * 
	 * The method appends faces-redirect=true to the action result in case no
	 * faces-redirect is defined.
	 * 
	 * <code>
	 *       /pages/workitems/workitem?id=23452345-2452435234&faces-redirect=true
	 * </code>
	 * 
	 */
	@Override
	public String process() throws AccessDeniedException, ProcessingErrorException {
		String actionResult = null;

		long lTotal = System.currentTimeMillis();

		// process workItem and catch exceptions
		try {

			// fire event
			long l1 = System.currentTimeMillis();
			events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_BEFORE_PROCESS));
			logger.finest("[WorkflowController] fire WORKITEM_BEFORE_PROCESS event: ' in "
					+ (System.currentTimeMillis() - l1) + "ms");

			// process workitem
			actionResult = super.process();

			// reset versions and editor sections
			versions = null;
			editorSections = null;
			// ! Do not call setWorkitem here because this fires a
			// WORKITEM_CHANGED event !

			// fire event
			long l2 = System.currentTimeMillis();
			events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_AFTER_PROCESS));
			logger.finest("[WorkflowController] fire WORKITEM_AFTER_PROCESS event: ' in "
					+ (System.currentTimeMillis() - l2) + "ms");

			// if a action was set by the workflowController, then this
			// action will be the action result String
			if (action != null && !action.isEmpty()) {
				actionResult = action;
				// reset action
				setAction(null);
			}

		} catch (ObserverException oe) {
			actionResult = null;
			// test if we can handle the exception...
			if (oe.getCause() instanceof PluginException) {
				// add error message into current form
				ErrorHandler.addErrorMessage((PluginException) oe.getCause());
			} else {
				if (oe.getCause() instanceof ValidationException) {
					// add error message into current form
					ErrorHandler.addErrorMessage((ValidationException) oe.getCause());
				} else {
					// throw unknown exception
					throw oe;
				}
			}
		} catch (PluginException pe) {
			actionResult = null;
			// add a new FacesMessage into the FacesContext
			ErrorHandler.handlePluginException(pe);
		}

		if (logger.isLoggable(Level.FINEST)) {
			String id = "";
			if (getWorkitem() != null)
				id = getWorkitem().getItemValueString(WorkflowService.UNIQUEID);
			logger.finest(
					"[WorkflowController] process: '" + id + "' in " + (System.currentTimeMillis() - lTotal) + "ms");
		}

		// test if no actionResult is defined
		if (actionResult == null || actionResult.isEmpty()) {
			// construct default action result
			actionResult = getDefaultActionResult() + "?id=" + getWorkitem().getUniqueID() + "&faces-redirect=true";
		}

		// test if faces-redirect is included in actionResult
		if (actionResult.contains("/") && !actionResult.contains("faces-redirect=")) {
			// append faces-redirect=true
			if (!actionResult.contains("?")) {
				actionResult = actionResult + "?";
			}
			actionResult = actionResult + "faces-redirect=true";
		}

		logger.fine("action result=" + actionResult);
		// close conversation
		reset();
		return actionResult;
	}

	/**
	 * The method saves the current workItem and fires the WorkflowEvents
	 * WORKITEM_BEFORE_SAVE and WORKITEM_AFTER_SAVE.
	 * 
	 * NOTE: the super class changes the behavior of save(action) and process a
	 * workItem instead of saving. This may conflict in future cases. If so we
	 * should decide if we simply add a new method here called 'saveAsDraft()'
	 * which would more precisely describe the behavior of this method.
	 */
	@Override
	public void save() throws AccessDeniedException {
		logger.fine("[WorkflowController] save");
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_BEFORE_SAVE));

		super.save();

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_AFTER_SAVE));
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
		String refQuery = "SELECT entity FROM Entity entity " + " JOIN entity.textItems AS t"
				+ "  WHERE entity.type IN ('workitem', 'workitemarchive', 'workitemversion') "
				+ "  AND t.itemName = '$workitemid'" + "  AND t.itemValue = '" + sRefID + "' "
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
