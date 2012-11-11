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
import javax.inject.Named;

import org.imixs.marty.util.WorkitemHelper;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.ejb.EntityService;


/**
 ** This Bean acts a a front controller for child workitems.
 * 
 * @author rsoika
 * 
 */
@Named("childWorkitemController")
@SessionScoped
public class ChildWorkitemController extends org.imixs.workflow.jee.faces.workitem.WorkflowController implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<ItemCollection> childList = null;
	private String uniqueIdRef = null;

	@EJB
	org.imixs.workflow.jee.ejb.WorkflowService workflowService;

	// @Inject
	// private WorkflowController workflowController;

	public static Logger logger = Logger.getLogger("org.imixs.marty");

	
	
	
	
	public String getUniqueIdRef() {
		return uniqueIdRef;
	}

	/**
	 * WorkflowEvent listener to update the child list of a workitem
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

		// if workItem has changed, then update the dms list
		if (WorkflowEvent.WORKITEM_CHANGED == workflowEvent.getEventType()) {
			reset();
			if (workflowEvent.getWorkitem() == null)
				uniqueIdRef = null;
			else
				uniqueIdRef = workflowEvent.getWorkitem().getItemValueString(
						EntityService.UNIQUEID);
		}

	}

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
					.getWorkListByRef(uniqueIdRef);
			for (ItemCollection aWorkitem : result) {
				childList.add(cloneWorkitem(aWorkitem));
			}
		}

		return childList;

	}

	public void reset() {
		childList = null;
		this.setWorkitem(null);

	}

	
	
	public ItemCollection cloneWorkitem(ItemCollection aWorkitem) {
		return WorkitemHelper.clone(aWorkitem);		
	}

}
