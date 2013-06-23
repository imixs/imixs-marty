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

package org.imixs.marty.plugins;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This plugin overwrites the Application Plugin and updates marty informations
 * like the subject and the workflowgroup name.
 * 
 * In addition the plugin suports a extended commment feature. Comments are
 * stored in the list property 'txtCommentList' which contains a map for each
 * comment. The map stores the username, the timestamp and the comment.
 * The plugin also stores the last comment in the field 'txtLastComment'.
 * 
 * 
 * @author rsoika
 * @version 2.0
 * 
 */
public class ApplicationPlugin extends
		org.imixs.workflow.plugins.ApplicationPlugin {
	ItemCollection documentContext;
	javax.ejb.SessionContext jeeSessionContext;

	private static Logger logger = Logger.getLogger(ApplicationPlugin.class
			.getName());

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);

		// cast Workflow Session Context to EJB Session Context
		jeeSessionContext = (javax.ejb.SessionContext) ctx.getSessionContext();
	}

	@Override
	public int run(ItemCollection adocumentContext,
			ItemCollection documentActivity) throws PluginException {

		documentContext = adocumentContext;

		// Update Subject
		if (!documentContext.hasItem("txtSubject"))

			documentContext.replaceItemValue("txtSubject", " - no subject - ");

		int iResult = super.run(documentContext, documentActivity);

		// update the comment
		updateComment();

		return iResult;
	}

	@Override
	public void close(int arg0) throws PluginException {
		super.close(arg0);

		// now cut txtworkflowgroup if ~ is available
		String sGroupName = documentContext
				.getItemValueString("txtWorkflowGroup");
		if (sGroupName.indexOf('~') > -1) {
			sGroupName = sGroupName.substring(sGroupName.indexOf('~') + 1);
			logger.fine("[ApplicationPlugin] set workflowGroup=" + sGroupName);
			documentContext.replaceItemValue("txtWorkflowGroup", sGroupName);
		}

	}

	/**
	 * This method updates the comment list. There for the method copies the
	 * txtComment into the txtCommentList and clears the txtComment field
	 * 
	 * @param workflowEvent
	 */
	private void updateComment() {

		String sComment = documentContext.getItemValueString("txtComment");
		if (!sComment.isEmpty()) {
			List vCommentList = documentContext.getItemValue("txtCommentLog");
			Map log = new HashMap();

			// create new Comment data - important: property names in lower
			// case
			log.put("txtcomment", sComment);
			Date dt = Calendar.getInstance().getTime();
			log.put("datcomment", dt);

			String remoteUser = this.jeeSessionContext.getCallerPrincipal()
					.getName();
			log.put("nameditor", remoteUser);
			vCommentList.add(0, log);

			documentContext.replaceItemValue("txtcommentLog", vCommentList);

			// clear comment
			documentContext.replaceItemValue("txtComment", "");

			// save last comment
			documentContext.replaceItemValue("txtLastComment", sComment);

		}
	}

}
