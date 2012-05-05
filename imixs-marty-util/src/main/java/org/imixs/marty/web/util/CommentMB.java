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

package org.imixs.marty.web.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.imixs.marty.web.workitem.WorkitemListener;
import org.imixs.marty.web.workitem.WorkitemMB;
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
public class CommentMB implements WorkitemListener {
	private WorkitemMB workitemMB = null;
	private ItemCollection workitem = null;

	/**
	 * This method register the bean as an workitemListener
	 * 
	 * The method also tries to set the current workitem hold by the workitemMB.
	 * The instance variable worktiem can be null if the commenMB was loaded
	 * after a worktiem was selected (so the onWorkitemChanged method was not
	 * called )
	 * 
	 */
	@PostConstruct
	public void init() {
		// register this Bean as a workitemListener to the current WorktieMB
		this.getWorkitemBean().addWorkitemListener(this);

		// set workitem
		workitem=this.getWorkitemBean().getWorkitem();
	}

	/**
	 * helper method to get the current instance of the WorkitemMB
	 * 
	 * @return
	 */
	private WorkitemMB getWorkitemBean() {
		if (workitemMB == null)
			workitemMB = (WorkitemMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "workitemMB");
		return workitemMB;
	}

	public void onWorkitemChanged(ItemCollection arg0) {
		workitem = arg0;
	}

	/**
	 * updates the comment list Therefor the method copies the txtComment into
	 * the txtCommentList and clears the txtComment field
	 */
	public void onWorkitemProcess(ItemCollection workitem) {
		// take comment and append it to txtCommentList
		try {
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
		} catch (Exception e) {
			// unable to copy comment
			e.printStackTrace();
		}
	}

	/**
	 * This method returns a array list containing all comment entries for the
	 * current workitem.
	 * 
	 * @return
	 */
	public ArrayList<ItemCollection> getLog() {
		ArrayList<ItemCollection> commentList = new ArrayList<ItemCollection>();
		if (workitem != null) {
			try {
				List<Map> vCommentList = workitem
						.getItemValue("txtCommentLog");
				for (Map aworkitem : vCommentList) {
					ItemCollection aComment = new ItemCollection(aworkitem);
					commentList.add((aComment));
				}
			} catch (Exception e) {
				// unable to copy comment
				try {
					workitem.replaceItemValue("txtcommentLog", "");
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}

		return commentList;
	}

	public void onChildProcess(ItemCollection arg0) {
		// nothing
	}

	public void onChildCreated(ItemCollection arg0) {
		// nothing

	}

	public void onChildProcessCompleted(ItemCollection arg0) {
		// nothing
	}

	public void onWorkitemCreated(ItemCollection arg0) {
		// nothing
	}

	public void onWorkitemProcessCompleted(ItemCollection arg0) {

	}

	public void onChildDelete(ItemCollection e) {
		// nothing
	}

	public void onChildDeleteCompleted() {
		// nothing
	}

	public void onChildSoftDelete(ItemCollection e) {
		// nothing
	}

	public void onChildSoftDeleteCompleted(ItemCollection e) {
		// nothing
	}

	public void onWorkitemDelete(ItemCollection e) {
		// nothing
	}

	public void onWorkitemDeleteCompleted() {
		// nothing
	}

	public void onWorkitemSoftDelete(ItemCollection e) {

	}

	public void onWorkitemSoftDeleteCompleted(ItemCollection e) {
		// nothing
	}

}