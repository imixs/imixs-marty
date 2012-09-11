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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;

/**
 * This bean provides a comment function to history comments entered by a user
 * into the field txtComment. The Bean creates the property txtCommentLog with a
 * Map providing following informations per each comment entry:
 * 
 * <ul>
 * <li>txtComment = comment</li>
 * <li>datComment = date of creation</li>
 * <li>namEditor = userID</li>
 * </ul>
 * 
 * The property log provides a ArrayList with ItemCollection Adapters providing
 * the comment details.
 * 
 * @author rsoika 
 */
@Named("commentController")
@ViewScoped
public class CommentController implements  Serializable {

	private static final long serialVersionUID = 1L;
	
	
	public CommentController() {
		super();
		
	}
	
	
	/**
	 * WorkflowEvent listener
	 * 
	 * updates the comment list. Therefor the method copies the txtComment into
	 * the txtCommentList and clears the txtComment field
	 * 
	 * @param workflowEvent
	 */
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent) {
		if (workflowEvent == null)
			return;
	
		ItemCollection workitem = workflowEvent.getWorkitem();

		if (WorkflowEvent.WORKITEM_BEFORE_PROCESS == workflowEvent.getEventType()) {
			String sComment = workitem.getItemValueString("txtComment");
			if (!"".equals(sComment)) {
				List vCommentList = workitem.getItemValue("txtCommentLog");
				Map log = new HashMap();

				// create new Comment data - important: property names in lower
				// case
				log.put("txtcomment", sComment);
				Date dt = Calendar.getInstance().getTime();
				log.put("datcomment", dt);
				FacesContext context = FacesContext.getCurrentInstance();
				ExternalContext externalContext = context.getExternalContext();
				String remoteUser = externalContext.getUserPrincipal()
						.getName();
				log.put("nameditor", remoteUser);
				vCommentList.add(0, log);

				workitem.replaceItemValue("txtcommentLog", vCommentList);

				// clear comment
				workitem.replaceItemValue("txtComment", "");
		
		}
		}

	}
	

	
}