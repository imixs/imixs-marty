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

import javax.mail.internet.AddressException;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.imixs.marty.ejb.SequenceService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.plugins.jee.AbstractPlugin;

/**
 * This Plugin handles a unique sequence number for all workItems from a project
 * based on the WorkflowGroup of the workItem. The current sequenceNumber for a
 * workflowGroup will be stored in the configuration Entity 'BASIC'. The
 * configuration entity provides a property named 'sequencenumbers' with the
 * current number range for each workflowGroup.
 * 
 * If a WorkItem is assigned to a WorklfowGroup with no corresponding entry in
 * the BASIC configuration, the Plugin will not compute a new number for the
 * workitem.
 * 
 * If the WorkItem is a ChildWorkitem (type=childworkitem) then the
 * sequcenceNumber will be computed based on the LastSequenceNumber stored in
 * the parentWorkitem. In this case the parent Workitem is the configuration
 * entity for the next sequenceNumber
 * 
 * If the Workitem still have a sequence number stored the plugin will not run.
 * 
 * The new computed SequenceNumer will be stored into the property
 * 'numsequencenumber'.
 * 
 * To compute the sequence Number the plugin uses the stateless session EJB
 * SequeceService which updates the latest used sequence Number..
 * 
 * @see SequenceService
 * @author rsoika
 * @version 1.0
 * 
 */
public class SequenceNumberPlugin extends AbstractPlugin {
	SequenceService sequenceService = null;;

	int sequenceNumber = -1;
	ItemCollection workitem = null;

	public void init(WorkflowContext actx) throws Exception {
		super.init(actx);

		// lookup profile service EJB
		String jndiName = "ejb/SequenceServiceBean";
		InitialContext ictx = new InitialContext();
		Context ctx = (Context) ictx.lookup("java:comp/env");
		sequenceService = (SequenceService) ctx.lookup(jndiName);

	}

	/**
	 * This method loads a sequence number object and increases the sequence
	 * number in the workItem. If the workItem is form type'workitem' then the
	 * next sequecnenumer is computed based on the workflowGroup. If the
	 * workItem is from type='childworkitem' then the next sequencenumber is
	 * computed on the parent workItem.
	 * 
	 * @return
	 * @throws AddressException
	 */
	@Override
	public int run(ItemCollection documentContext,
			ItemCollection documentActivity) throws Exception {

		workitem = documentContext;

		// check type
		String sType = workitem.getItemValueString("Type");
		if (!("workitem".equals(sType) || "childworkitem".equals(sType)))
			return Plugin.PLUGIN_OK;

		/* check if worktitem still have a sequence number? */
		if (workitem.getItemValueInteger("numsequencenumber") == 0) {
			try {
				// load next Number based on the type of workitem
				if ("workitem".equals(sType))
					sequenceNumber = sequenceService
							.getNextSequenceNumberByGroup(documentContext);
				else
					sequenceNumber = sequenceService
							.getNextSequenceNumberByParent(documentContext);

				if (sequenceNumber > 0)
					workitem.replaceItemValue("numsequencenumber", new Integer(
							sequenceNumber));
				else {
					// to avoid problems with incorrect data values we remove the
					// property numsequencenumber in this case
					workitem.removeItem("numsequencenumber");
				}

			} catch (Exception ee) {
				ee.printStackTrace();
				// no action
				return Plugin.PLUGIN_ERROR;
			}

		} 

		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int status) throws Exception {

	}

}
