package org.imixs.marty.util;

import javax.mail.internet.AddressException;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.imixs.marty.business.SequenceService;
import org.imixs.marty.business.SequenceService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.plugins.jee.AbstractPlugin;

/**
 * This Plugin handles a unique sequence number for all worktimes in a
 * WorkflowGroup. The sequencenumber will be stored in the configuration Entity
 * 'SYW_CONFIGURATION'. The configuration provides a property 'sequencenumbers'
 * with the current number range for each workflowGroup
 * 
 * If a Workitem have a WorklfowGroup with no corresponding entry the Plugin
 * will not compute a new number. The new computed SequenceNumer will be stored
 * into the property 'numsequencenumber'. If the Workitem still have a sequence number
 * stored the plugin will not run.
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
	 * number in the workitem
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
		if (workitem.getItemValueInteger("numsequencenumber") == 0) {
			try {
				// load next Number
				sequenceNumber = sequenceService
						.getNextSequenceNumberByGroup(documentContext);

				if (sequenceNumber>0)
					workitem.replaceItemValue("numsequencenumber", new Integer(
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
