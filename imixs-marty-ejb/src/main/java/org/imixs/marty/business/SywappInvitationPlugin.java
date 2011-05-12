package org.imixs.sywapps.business;

import javax.mail.internet.AddressException;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.jee.plugins.AbstractPlugin;

/**
 * This Plugin handles an acception of a invitation. There fore the plugin makes
 * use of the InvitationProcessService to update read access informations to the
 * corresponding project and workitem.
 * 
 * 
 * The Plugin runns only in Activity 90 !
 * 
 * 
 * @see InvitationServiceBean
 * @author rsoika
 * @version 1.0
 * 
 */
public class SywappInvitationPlugin extends AbstractPlugin {
	InvitationService invitationProcessService = null;;
	public final static int PROJECT_INVITATION_NEW = 300;
	public final static int AUTOACCEPT_PROJECT_INVITATION = 90;

	ItemCollection workitem = null;

	public void init(WorkflowContext actx) throws Exception {
		super.init(actx);

		// lookup profile service EJB
		String jndiName = "ejb/InvitationServiceBean";
		InitialContext ictx = new InitialContext();
		Context ctx = (Context) ictx.lookup("java:comp/env");
		invitationProcessService = (InvitationService) ctx.lookup(jndiName);

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

		// check if acitivity id is 300.90
		int numProcessID = documentActivity.getItemValueInteger("numProcessID");
		int numActivivtyID = documentActivity
				.getItemValueInteger("numActivityID");
		if (numActivivtyID != AUTOACCEPT_PROJECT_INVITATION)
			return Plugin.PLUGIN_OK;

		String sUserName = this.getUserName();
		System.out.println("InvitationPlugin - Current User=" + sUserName);
		workitem = documentContext;

		try {
			/* check if invitation has a workitem reference () */
			String aWorkitemRefID = workitem
					.getItemValueString("$invitationWorkitemID");
			if (!"".equals(aWorkitemRefID)) {

				// Update Workitem.....
				invitationProcessService.updateWorkitemReadAccess(
						aWorkitemRefID, sUserName);

			}
			/* Now update the Project reference */
			aWorkitemRefID = workitem.getItemValueString("$UniqueIDRef");
			// Update Project.....
			invitationProcessService.updateProjectReadAccess(aWorkitemRefID,
					sUserName);

		} catch (Exception ee) {
			ee.printStackTrace();
			// no action
			return Plugin.PLUGIN_ERROR;
		}
		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int status) throws Exception {

	}

}
