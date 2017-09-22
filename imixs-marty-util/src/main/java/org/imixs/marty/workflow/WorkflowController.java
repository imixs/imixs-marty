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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.ObserverException;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.imixs.marty.ejb.WorkitemService;
import org.imixs.marty.model.ProcessController;
import org.imixs.marty.util.ErrorHandler;
import org.imixs.marty.util.ValidationException;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.faces.util.LoginController;

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
public class WorkflowController extends org.imixs.workflow.faces.workitem.WorkflowController implements Serializable {

	public static final String DEFAULT_ACTION_RESULT = "/pages/workitems/workitem";

	@Inject
	private Conversation conversation;

	/* Services */
	@EJB
	protected WorkitemService workitemService;

	@Inject
	protected ProcessController processController;

	@Inject
	protected LoginController loginController = null;

	@Inject
	protected Event<WorkflowEvent> events;

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(WorkflowController.class.getName());

	private List<ItemCollection> versions = null;
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
	 * The method loads a new wokitem by a uniqueID. If no id is provided the method
	 * did not change the current workitem reference. If the uniqueId is invalid the
	 * workitem will be set to null (see setUnqiueId)
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
	 * The method sets the workitem and fires a WORKITEM_CHANGED event.
	 */
	@Override
	public void setWorkitem(ItemCollection newWorkitem) {
		// we may not call reset() here, because a conversation context can
		// still exist.
		events.fire(new WorkflowEvent(newWorkitem, WorkflowEvent.WORKITEM_CHANGED));
		super.setWorkitem(newWorkitem);
		versions = null;
		//editorSections = null;
	}

	/**
	 * Loads a workitem by its ID. The method starts a new conversation context.
	 */
	@Override
	public void load(String uniqueID) {
		super.load(uniqueID);
		if (conversation.isTransient()) {
			conversation.setTimeout(
					((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
							.getSession().getMaxInactiveInterval() * 1000);
			conversation.begin();
			logger.fine("start new conversation, id=" + conversation.getId());
		}
	}

	/**
	 * This ActionListener method creates a new empty workitem. An existing workitem
	 * and optional conversation context will be reset. The method starts a new
	 * conversation context. Finally the method fires the WorkfowEvent
	 * WORKITEM_CREATED.
	 */
	@Override
	public void create(ActionEvent event) {
		super.create();

		if (conversation.isTransient()) {
			conversation.setTimeout(
					((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
							.getSession().getMaxInactiveInterval() * 1000);
			conversation.begin();
			logger.fine("start new conversation, id=" + conversation.getId());
		}

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_CREATED));
	}

	/**
	 * This method creates a new empty workitem. An existing workitem and optional
	 * conversation context will be reset.
	 * 
	 * The method assigns the initial values '$ModelVersion', '$ProcessID' and
	 * '$UniqueIDRef' to the new workitem. The method creates the empty field
	 * '$workitemID' and the field 'namowner' which is assigned to the current user.
	 * This data can be used in case that a workitem is not processed but saved
	 * (e.g. by the dmsController).
	 * 
	 * The method starts a new conversation context. Finally the method fires the
	 * WorkfowEvent WORKITEM_CREATED.
	 * 
	 * @param modelVersion
	 *            - model version
	 * @param processID
	 *            - processID
	 * @param processRef
	 *            - uniqueid ref
	 */

	public void create(String modelVersion, int processID, String processRef) {
		super.create();
		// set model information..
		getWorkitem().replaceItemValue("$ModelVersion", modelVersion);
		getWorkitem().replaceItemValue("$ProcessID", processID);

		// set default owner
		getWorkitem().replaceItemValue("namowner", loginController.getUserPrincipal());

		// set empty $workitemid
		getWorkitem().replaceItemValue("$workitemid", "");

		// assign process..
		if (processRef != null) {
			getWorkitem().replaceItemValue("$UniqueIDRef", processRef);
			// find process
			ItemCollection process = processController.getProcessById(processRef);
			if (process != null) {
				getWorkitem().replaceItemValue("txtProcessName", process.getItemValueString("txtName"));
				getWorkitem().replaceItemValue("txtProcessRef", process.getItemValueString(WorkflowKernel.UNIQUEID));

			} else {
				logger.warning("[create] - unable to find process entity '" + processRef + "'!");
			}
		}

		// start now the new conversation
		if (conversation.isTransient()) {
			conversation.setTimeout(
					((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
							.getSession().getMaxInactiveInterval() * 1000);
			conversation.begin();
			logger.fine("start new conversation, id=" + conversation.getId());
		}
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_CREATED));
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
	 * The action method processes the current workItem and fires the WorkflowEvents
	 * WORKITEM_BEFORE_PROCESS and WORKITEM_AFTER_PROCESS. The Method also catches
	 * PluginExceptions and adds the corresponding Faces Error Message into the
	 * FacesContext. In case of an exception the WorkflowEvent
	 * WORKITEM_AFTER_PROCESS will not be fired. <br>
	 * The action result returned by the workflow engine may contain a $uniqueid to
	 * redirect the user and load that new workitem. If no action result is defined
	 * the method redirects to the default action with the current workitem id.
	 * 
	 * The method appends faces-redirect=true to the action result in case no
	 * faces-redirect is defined.
	 * 
	 * 
	 * <code>
	 *       /pages/workitems/workitem?id=23452345-2452435234&faces-redirect=true
	 * </code>
	 * 
	 * In case the processing was successful, the current conversation will be
	 * closed. In Case of an Exception (e.g PluginException) the conversation will
	 * not be closed, so that the current workitem data is still available.
	 * 
	 * @throws ModelException
	 * 
	 */
	@Override
	public String process() throws AccessDeniedException, ProcessingErrorException, ModelException {
		String actionResult = null;
		long lTotal = System.currentTimeMillis();

		// process workItem and catch exceptions
		try {
			// fire event
			long l1 = System.currentTimeMillis();
			events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_BEFORE_PROCESS));
			logger.finest(
					"[process] fire WORKITEM_BEFORE_PROCESS event: ' in " + (System.currentTimeMillis() - l1) + "ms");

			// process workitem
			actionResult = super.process();

			// reset versions and editor sections
			versions = null;
			// ! Do not call setWorkitem here because this fires a
			// WORKITEM_CHANGED event !

			// fire event
			long l2 = System.currentTimeMillis();
			events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_AFTER_PROCESS));
			logger.finest(
					"[process] fire WORKITEM_AFTER_PROCESS event: ' in " + (System.currentTimeMillis() - l2) + "ms");

			// if a action was set by the workflowController, then this
			// action will be the action result String
			if (action != null && !action.isEmpty()) {
				actionResult = action;
				// reset action
				setAction(null);
			}

			// compute the Action result...
			if (actionResult == null || actionResult.isEmpty()) {
				// construct default action result if no actionResult was
				// defined
				actionResult = getDefaultActionResult() + "?id=" + getWorkitem().getUniqueID() + "&faces-redirect=true";
			}

			// test if 'faces-redirect' is included in actionResult
			if (actionResult.contains("/") && !actionResult.contains("faces-redirect=")) {
				// append faces-redirect=true
				if (!actionResult.contains("?")) {
					actionResult = actionResult + "?faces-redirect=true";
				} else {
					actionResult = actionResult + "&faces-redirect=true";
				}
			}

			logger.fine("action result=" + actionResult);

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("[process] '" + getWorkitem().getItemValueString(WorkflowService.UNIQUEID)
						+ "' completed in " + (System.currentTimeMillis() - lTotal) + "ms");
			}

			// close conversation
			reset();

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
		} catch (ModelException me) {
			actionResult = null;
			// add a new FacesMessage into the FacesContext
			ErrorHandler.handleModelException(me);
		}

		return actionResult;
	}

	/**
	 * The method saves the current workItem and fires the WorkflowEvents
	 * WORKITEM_BEFORE_SAVE and WORKITEM_AFTER_SAVE.
	 * 
	 * NOTE: the super class changes the behavior of save(action) and process a
	 * workItem instead of saving. This may conflict in future cases. If so we
	 * should decide if we simply add a new method here called 'saveAsDraft()' which
	 * would more precisely describe the behavior of this method.
	 */
	@Override
	public void save() throws AccessDeniedException {
		logger.fine("save workitem...");
		// fire event
		events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_BEFORE_SAVE));

		super.save();

		// fire event
		events.fire(new WorkflowEvent(getWorkitem(), WorkflowEvent.WORKITEM_AFTER_SAVE));
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
	 * Loades a new wokitem by a uniqueID
	 * 
	 * @param aUniqueID
	 */
	public void setUniqueId(String uniqueID) {
		this.load(uniqueID);
	}

	/**
	 * Returns the current uniqueid of the workitem or null if no workitem is set.
	 * 
	 * @return uniqueid of current workitem or null
	 */
	public String getUniqueId() {
		if (this.getWorkitem() == null) {
			return null;
		} else {
			return this.getWorkitem().getItemValueString(WorkflowKernel.UNIQUEID);
		}
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
	 * this method loads all versions to the current workitem. Idependent from the
	 * type property! The method returns an empty list if no version exist (only the
	 * main version)
	 * 
	 * @see org.imixs.WorkitemService.business.WorkitemServiceBean
	 */
	private void loadVersionWorkItemList() {
		versions = new ArrayList<ItemCollection>();
		if (this.isNewWorkitem() || null == getWorkitem())
			return;
		List<ItemCollection> col = null;
		String sRefID = getWorkitem().getItemValueString("$workitemId");
		String refQuery = "( (type:\"workitem\" OR type:\"workitemarchive\" OR type:\"workitemversion\") AND $workitemid:\""
				+ sRefID + "\")";
		try {
			col = this.getWorkflowService().getDocumentService().find(refQuery, 999, 0);
			// sort by $modified
			Collections.sort(col, new ItemCollectionComparator("$modified", true));

			// Only return version list if more than one version was found!
			if (col.size() > 1) {
				for (ItemCollection aworkitem : col) {
					versions.add(aworkitem);
				}
			}

		} catch (QueryException e) {
			logger.warning("loadVersionWorkItemList - invalid query: " + e.getMessage());
		}
	}

}
