package org.imixs.sywapps.util;

import javax.mail.internet.AddressException;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.imixs.sywapps.business.SequenceService;
import org.imixs.sywapps.business.SequenceServiceBean;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.jee.plugins.AbstractPlugin;

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
 * @see SequenceServiceBean
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
				sequenceNumber = sequenceService.getNextSequenceNumber(documentContext);

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
