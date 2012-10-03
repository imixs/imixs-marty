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
package org.imixs.marty.workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;

/**
 * The HistoryController provides a history navigation over workItems the user
 * has selected. There for the controller listens to the events of the
 * WokflowController. The history workItems containing only the $uniqueid and a
 * minimum of attributes to display a summary.
 * 
 * @see historynav.xhtml
 * 
 * @author rsoika
 * 
 */

@Named("historyController")
@SessionScoped
public class HistoryController implements Serializable {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger("org.imixs.office");

	@Inject
	private WorkflowController workflowController;

	private List<ItemCollection> workitems = null;
	private String lastRemoveActionResult = null;
	private String lastRemovedWorkitem = null;

	private String currentId = null;

	public HistoryController() {
		super();

		workitems = new ArrayList<ItemCollection>();
	}

	public void setWorkflowController(WorkflowController workflowController) {
		this.workflowController = workflowController;
	}

	public String getCurrentId() {
		return currentId;
	}

	public void setCurrentId(String currentId) {
		this.currentId = currentId;
	}

	/**
	 * retuns a list of all workites curently visited
	 * 
	 * @return
	 */
	public List<ItemCollection> getWorkitems() {
		if (workitems == null)
			workitems = new ArrayList<ItemCollection>();
		return workitems;
	}

	/**
	 * this action method removes a workItem from the history list
	 * 
	 * @param id
	 *            - $UniqueID
	 * @param action
	 *            - default action
	 * @return action
	 * 
	 */
	public String removeWorkitem(String sID, String action) {
		// test if exits
		int iPos = findWorkItem(sID);
		if (iPos > -1) {
			// update workitem
			workitems.remove(iPos);
		}
		return action;

	}

	/**
	 * WorkflowEvent listener listens to WORKITEM_CHANGED events
	 * 
	 * @param workflowEvent
	 * 
	 **/
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent) {
		if (workflowEvent == null || workflowEvent.getWorkitem() == null) {
			currentId = null;
			return;
		}

		if (WorkflowEvent.WORKITEM_CHANGED == workflowEvent.getEventType()) {
			addWorkItem(workflowEvent.getWorkitem());
		}

		if (WorkflowEvent.WORKITEM_AFTER_PROCESS == workflowEvent
				.getEventType()) {
			addWorkItem(workflowEvent.getWorkitem());
		}
	}

	/**
	 * This method removes a workItem from the current historyList.
	 */
	private void removeWorkItem(ItemCollection aWorkitem) {
		if (aWorkitem == null)
			return;

		// test if exits
		int iPos = findWorkItem(aWorkitem.getItemValueString("$UniqueID"));
		if (iPos > -1) {
			// update workitem
			workitems.remove(iPos);
		}
	}

	/**
	 * This method adds a workItem into the current historyList. If the workItem
	 * is still contained it will be updated.
	 */
	private void addWorkItem(ItemCollection aWorkitem) {

		if (aWorkitem == null || !aWorkitem.getItemValueString("type").startsWith("workitem")) {
			currentId = null;
			return;
		}

		ItemCollection clone = cloneWorkitem(aWorkitem);

		// test if exits
		int iPos = findWorkItem(aWorkitem.getItemValueString("$UniqueID"));
		if (iPos > -1) {
			// update workitem
			workitems.set(iPos, clone);
		} else {
			// add the new workitem into the history
			workitems.add(clone);
		}
		currentId = clone.getItemValueString("$UniqueID");

	}

	/**
	 * This method tests if the WorkItem with the corresponding $UniqueID exists
	 * in the history list and returns the position.
	 * 
	 * @param aID
	 *            - $UniqueID of the workitem
	 * @return - the position in the history list or -1 if not contained.
	 */
	private int findWorkItem(String aID) {
		if (aID == null)
			return -1;
		// try to find the woritem in the history list
		for (int i = 0; i < workitems.size(); i++) {
			ItemCollection historyWorkitem = workitems.get(i);
			String sHistoryUnqiueID = historyWorkitem
					.getItemValueString("$Uniqueid");
			if (sHistoryUnqiueID.equals(aID)) {
				// Found! - remove it and return..
				return i;
			}
		}
		return -1;
	}

	/**
	 * This method clones the given workItem with a minimum of attributes.
	 * 
	 * @param aWorkitem
	 * @return
	 */
	private ItemCollection cloneWorkitem(ItemCollection aWorkitem) {
		ItemCollection clone = new ItemCollection();

		clone.replaceItemValue("$UniqueID", aWorkitem.getItemValue("$UniqueID"));
		clone.replaceItemValue("txtWorkflowSummary",
				aWorkitem.getItemValue("txtWorkflowSummary"));
		clone.replaceItemValue("txtWorkflowAbstract",
				aWorkitem.getItemValue("txtWorkflowAbstract"));

		return clone;

	}
}
