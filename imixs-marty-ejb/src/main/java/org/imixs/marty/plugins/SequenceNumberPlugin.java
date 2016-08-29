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

import javax.mail.internet.AddressException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.imixs.marty.ejb.SequenceService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Plugin handles a unique sequence number for all workItems. The current
 * sequenceNumber for a workitem is based on the workflowGroup. The next free
 * sequence number will be stored in the configuration Entity 'BASIC'. The
 * configuration entity provides a property named 'sequencenumbers' with the
 * current number range for each workflowGroup.
 * 
 * If a WorkItem is assigned to a WorkflowGroup with no corresponding entry in
 * the BASIC configuration, the Plugin will not compute a new number for the
 * workitem.
 * 
 * If the Workitem still have a sequence number stored the plugin will not run.
 * The new computed SequenceNumer will be stored into the property
 * 'numsequencenumber'. To compute the sequence Number the plugin uses the
 * stateless session EJB SequeceService which updates the latest used sequence
 * Number.
 * 
 * --- Optimistic Locking---------
 * <p>
 * If the WorkItem is a ChildWorkitem (type=childworkitem) then the mechanism is
 * stopped. In earlier version the sequcenceNumber was computed based on the
 * LastSequenceNumber stored in the parentWorkitem. In this case the parent
 * Workitem was the configuration entity for the next sequenceNumber. But
 * because of Optmistic locking this leads into a exception in the frontend
 * (because of the fact, that the frontend controller holds a local copy of the
 * parent workitem) This is the reason why the mechanism is disabled here!
 * <p>
 * --- Solution-------------------
 * <p>
 * The 'Optimistic Locking' problem could be solved if the client removes the
 * property '$version' from the parent workitem.
 * 
 * 
 * @see SequenceService
 * @author rsoika
 * @version 1.0
 * 
 */
public class SequenceNumberPlugin extends AbstractPlugin {
	SequenceService sequenceService = null;

	int sequenceNumber = -1;
	ItemCollection workitem = null;
	private static Logger logger = Logger.getLogger(SequenceNumberPlugin.class.getName());
	public static String NO_SEQUENCE_SERVICE_FOUND = "NO_SEQUENCE_SERVICE_FOUND";

	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);

		// lookup profile service EJB
		String jndiName = "ejb/SequenceService";
		InitialContext ictx;
		try {
			ictx = new InitialContext();

			Context ctx = (Context) ictx.lookup("java:comp/env");
			sequenceService = (SequenceService) ctx.lookup(jndiName);
		} catch (NamingException e) {
			throw new PluginException(SequenceNumberPlugin.class.getSimpleName(), NO_SEQUENCE_SERVICE_FOUND,
					"[SequenceNumberPlugin] unable to lookup sequenceService: ", e);
		}
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
	public int run(ItemCollection documentContext, ItemCollection documentActivity) throws PluginException {

		workitem = documentContext;

		// check type

		/*
		 * The plugin will only run for type='worktiem'. Childworkitems are no
		 * longer supported as a optimistic locking exception will be forced in
		 * the frontend (because of updated version id)
		 * 
		 * || "childworkitem".equals(sType)
		 */
		String sType = workitem.getItemValueString("Type");
		// also give a squence number for archived workitems
		if (!sType.startsWith("workitem") || sType.endsWith("deleted"))
			return Plugin.PLUGIN_OK;

		/* check if worktitem still have a sequence number? */
		if (workitem.getItemValueInteger("numsequencenumber") == 0) {
			logger.fine(
					"SequenceNumberPlugin calculating next sequencenumber: '" + documentContext.getUniqueID() + "'");
			try {
				// load next Number based on the type of workitem
				if (sType.startsWith("workitem"))
					sequenceNumber = sequenceService.getNextSequenceNumberByGroup(documentContext);
				else
					sequenceNumber = sequenceService.getNextSequenceNumberByParent(documentContext);

			} catch (AccessDeniedException e) {
				throw new PluginException(e.getErrorContext(), e.getErrorCode(), "[SequenceNumberPlugin] error ", e);
			}
			if (sequenceNumber > 0)
				workitem.replaceItemValue("numsequencenumber", new Integer(sequenceNumber));
			else {
				// to avoid problems with incorrect data values we remove the
				// property numsequencenumber in this case
				workitem.removeItem("numsequencenumber");
			}

		}

		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int status) throws PluginException {

	}

}
