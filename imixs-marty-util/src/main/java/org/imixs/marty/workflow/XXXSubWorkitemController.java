package org.imixs.marty.workflow;
/*******************************************************************************
 *  Imixs Workflow Technology
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The SubWorkitemController provides methods to select and process linked
 * workitems, called 'subworkitems'. Those workitems can be either form the type
 * 'workitem' or 'childworkitem'. The workitem type is controlled by the model.
 * 
 * * A sub workitem is linked to the current workitem controlled by the
 * WorkflowController and is assigned to the same model version.
 * 
 * 
 * @author rsoika
 * @version 1.0
 */

@Named
@RequestScoped
public class XXXSubWorkitemController implements Serializable {

	public static Logger logger = Logger.getLogger(XXXSubWorkitemController.class.getName());

	private static final long serialVersionUID = 1L;

	@Inject
	protected WorkflowController workflowController;

	@EJB
	protected DocumentService documentService;

	@EJB
	protected WorkflowService workflowService;

	private List<ItemCollection> subWorkitemList = null;
	private ItemCollection workitem = null;

	/**
	 * this method returns a list of all child workitems for the current
	 * workitem. The workitem list is cached. Subclasses can overwrite the
	 * method loadWorkitems() to return a custom result set.
	 * 
	 * @return - list of file meta data objects
	 */
	public List<ItemCollection> getWorkitems() {
		if (subWorkitemList == null) {
			subWorkitemList = loadWorkitems();
		}
		return subWorkitemList;

	}

	public ItemCollection getWorkitem() {
		return workitem;
	}

	public void setWorkitem(ItemCollection workitem) {

		logger.info("worktiem id=" + workitem.getUniqueID());

		this.workitem = workitem;
	}

	public String process() throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {

		workitem = workflowService.processWorkItem(workitem);

		// test if the property 'action' is provided
		String action = workitem.getItemValueString("action");
		if ("".equals(action)) {
			// get default workflowResult message
			action = workitem.getItemValueString("txtworkflowresultmessage");
		}

		subWorkitemList = null;
		workitem = null;

		return ("".equals(action) ? null : action);
	}

	/**
	 * This method returns all linked sub workitems. The list is sorted by
	 * creation date.
	 * 
	 * @return - list of subworkitems
	 */
	public List<ItemCollection> loadWorkitems() {
		logger.fine("load minute list...");
		subWorkitemList = new ArrayList<ItemCollection>();

		if (workflowController.getWorkitem() == null || workflowController.getWorkitem().getUniqueID().isEmpty()) {
			// link found
			return subWorkitemList;
		}

		String uniqueIdRef = workflowController.getWorkitem().getUniqueID();
		if (!uniqueIdRef.isEmpty()) {
			String sQuery = null;

			sQuery = "( (type:\"workitem\" OR type:\"childworkitem\" OR type:\"workitemarchive\" OR type:\"childworkitemarchive\") ";
			sQuery += " AND ($uniqueidref:\"" + uniqueIdRef + "\")) ";

			try {
				subWorkitemList = documentService.find(sQuery, 99, 0);
				// sort by numsequencenumber
				Collections.sort(subWorkitemList, new ItemCollectionComparator("$created", false));
			} catch (QueryException e) {
				logger.warning("loadWorkitems - invalid query: " + e.getMessage());
			}

		}

		return subWorkitemList;

	}

	/**
	 * Returns the list of events of a subworkitem
	 * 
	 * @param uniqueid
	 * @return
	 */
	public List<ItemCollection> getEvents(String uniqueid) {
		ItemCollection subworkitem = workflowService.getWorkItem(uniqueid);
		List<ItemCollection> events;
		try {
			events = this.workflowService.getEvents(subworkitem);
		} catch (ModelException e) {
			// no events found
			events = new ArrayList<ItemCollection>();
		}

		return events;

	}
}
