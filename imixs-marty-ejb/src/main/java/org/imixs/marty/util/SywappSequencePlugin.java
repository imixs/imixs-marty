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

package org.imixs.marty.util;

import javax.mail.internet.AddressException;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.imixs.marty.business.SequenceService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.plugins.jee.AbstractPlugin;

/**
 * This Plugin handles a unique sequence number for all worktimes in a workitem Group. The
 * sequencenumber will be stored in the Parent Workitem.
 * This needs that the workitem have a $UniqueIDRef which points to the ParentWorkitem holding
 * the latest sequence name.
 * If a Workitem have no Parent Workitem the Plugin will not compute a new number
 * The new computed SequenceNumer will be stored into the attribute SEQUENCE_NAME
 * if the Workitem still have a sequence number stored the plugin will not run.
 * 
 * To compute the sequence Number the plugin uses the stateless session EJB SequeceService
 * which updates the latest used sequence Number..
 * 
 * @see SequenceService
 * @author rsoika
 * @version 1.0
 * 
 */
public class SywappSequencePlugin extends AbstractPlugin {
	SequenceService sequenceService = null;;
	
	int sequenceNumber = -1;
	ItemCollection workitem = null;

	public void init(WorkflowContext actx) throws Exception {
		super.init(actx);
		
		// lookup profile service EJB
		String jndiName="ejb/SequenceServiceBean";
        InitialContext ictx = new InitialContext();
        Context ctx = (Context) ictx.lookup("java:comp/env"); 
        sequenceService= (SequenceService)ctx.lookup(jndiName);

	}

	/**
	 * This method loads a sequence number object and increases the sequence number
	 * in the workitem
	 * 
	 * 
	 * @return
	 * @throws AddressException
	 */
	@Override
	public int run(ItemCollection documentContext,
			ItemCollection documentActivity) throws Exception {

		workitem = documentContext;
		/* check if worktitem still have a sequence number? */
		if (workitem.getItemValueInteger("numSequenceNummer") == 0) {
			try {
				// load next Number
				sequenceNumber = sequenceService.getNextSequenceNumberByParent(documentContext);

				workitem.replaceItemValue("numSequenceNummer", new Integer(
						sequenceNumber));

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
