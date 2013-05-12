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
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.model.ModelController;
import org.imixs.marty.util.WorkitemHelper;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.ejb.EntityService;

/**
 * This Bean acts as a front controller for child workitems. A child workitem
 * references another workitem. Each workitem can have a list of child
 * workitems.
 * 
 * The default type of a new child worktiem is 'workitemchild'. The type can be
 * changed and controlled by the workflow model
 * 
 * @author rsoika
 * 
 */
@Named("childWorkitemController")
@SessionScoped
public class ChildWorkitemController extends
		org.imixs.workflow.jee.faces.workitem.WorkflowController implements
		Serializable {

	private static final long serialVersionUID = 1L;

	private List<ItemCollection> childList = null;
	private String uniqueIdRef = null;
	private ItemCollection parentWorkitem = null;
	private String childType="childworkitem";
	private int sortOrder=1;

	@EJB
	org.imixs.workflow.jee.ejb.WorkflowService workflowService;

	@Inject
	ModelController modelController;

	public static Logger logger = Logger.getLogger("org.imixs.marty");

	
	
	
	
	/**
	 * Default type for new created child workitems
	 * @return
	 */
	public String getChildType() {
		return childType;
	}

	public void setChildType(String childType) {
		this.childType = childType;
	}

	/**
	 * Sort order for child workitem list
	 * @return
	 */
	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	/**
	 * Returns the $UniqueID of the parentWorkitem
	 * 
	 * @return - string
	 */
	public String getUniqueIdRef() {
		return uniqueIdRef;
	}

	/**
	 * Returns the parentWorkitem
	 * 
	 * @return - itemCollection
	 */
	public ItemCollection getParentWorkitem() {
		return parentWorkitem;
	}
	
	
	

	/**
	 * WorkflowEvent listener to update the child list of the current parent
	 * workitem. The method also updates the uniqueIDRef to identify the parent
	 * workitem
	 * 
	 * @param workflowEvent
	 * @throws AccessDeniedException
	 */
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent)
			throws AccessDeniedException {
		if (workflowEvent == null)
			return;

		// skip if not a workItem...
		if (workflowEvent.getWorkitem() != null
				&& !workflowEvent.getWorkitem().getItemValueString("type")
						.startsWith("workitem"))
			return;

		// if workItem has changed or was processed, then reset the child list
		// and update the UniqeIDRef
		if (WorkflowEvent.WORKITEM_CHANGED == workflowEvent.getEventType()
				|| WorkflowEvent.WORKITEM_AFTER_PROCESS == workflowEvent
						.getEventType()) {
			// clear current child workitem
			reset(null);
			// check if parent workitem is available
			if (workflowEvent.getWorkitem() == null) {
				uniqueIdRef = null;
				parentWorkitem = null;
			} else {
				uniqueIdRef = workflowEvent.getWorkitem().getItemValueString(
						EntityService.UNIQUEID);
				parentWorkitem = workflowEvent.getWorkitem();
			}
		}

	}

	/**
	 * this method returns a list of all sub process entities for the workflow
	 * group assigend to the current parent workitem.
	 * 
	 * @return - list process entities
	 */
//	public List<ItemCollection> getWxxorkitems() {
//		List<ItemCollection> subInitialProcessList = new ArrayList<ItemCollection>();
//
//		String workflowGroup = parentWorkitem
//				.getItemValueString("txtWorkflowGroup");
//
//		List<String> allGroups = modelController
//				.getSubWorkflowGroups(workflowGroup);
//		for (String aGroupName : allGroups) {
//			subInitialProcessList.add(modelController
//					.getInitialProcessEntityByGroup(aGroupName));
//
//		}
//
//		return subInitialProcessList;
//	}

	/**
	 * this method returns a list of all child workitems for the current
	 * workitem
	 * 
	 * @return - list of file meta data objects
	 */
	public List<ItemCollection> getWorkitems() {

		if (childList != null)
			return childList;

		childList = new ArrayList<ItemCollection>();

		if (uniqueIdRef != null) {
			List<ItemCollection> result = workflowService
					.getWorkListByRef(uniqueIdRef,0,-1,null, getSortOrder());
			for (ItemCollection aWorkitem : result) {
				childList.add(cloneWorkitem(aWorkitem));
			}
		}

		return childList;

	}

	/**
	 * reset the current childlist
	 */
	@Override
	public void reset(ActionEvent event) {
		super.reset(event);
		childList = null;
	}

	/**
	 * create a new childWorkItem with type='childworkitem'
	 */
	@Override
	public void create(ActionEvent event) {
		super.create(event);
		// update type property
		this.getWorkitem().replaceItemValue("type", getChildType());
	}

	public ItemCollection cloneWorkitem(ItemCollection aWorkitem) {
		return WorkitemHelper.clone(aWorkitem);
	}

}
