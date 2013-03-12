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

import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This plugin overwrites the Application Plugin and updates marty informations
 * like the subject and the workflowgroup name.
 * 
 * @author rsoika
 * 
 */
public class ApplicationPlugin extends
		org.imixs.workflow.plugins.ApplicationPlugin {
	ItemCollection documentContext;
	private static Logger logger = Logger.getLogger(ApplicationPlugin.class.getName());

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
	}

	@Override
	public int run(ItemCollection adocumentContext,
			ItemCollection documentActivity) throws PluginException {

		documentContext = adocumentContext;

		// Update Subject
		if (!documentContext.hasItem("txtSubject"))

			documentContext.replaceItemValue("txtSubject", " - no subject - ");

		int iResult = super.run(documentContext, documentActivity);

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
}
