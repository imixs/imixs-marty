package org.imixs.marty.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.engine.plugins.ResultPlugin;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This plugin manages the approver lists of process and space managers and
 * teams
 * 
 * The result will be stored in the following attributes:
 * 
 * <code>
 * namProcessManagerApprovers 
 * namProcessManagerApprovedBy
 * namProcessTeamApprovers 
 * namProcessTeamApprovedBy
 * 
 * namSpaceManagerApprovers 
 * namSpaceManagerApprovedBy 
 * namSpaceTeamApprovers
 * namSpaceTeamApprovedBy
 * 
 * </code>
 * 
 * 
 * Plugin can be activated via the following result code:
 * 
 * Example:
 * 
 * <code>
 *  <item name='approvedby'>SpaceTeam</item> 
 *  
 *  or: 
 *  
 *  <item name='approvedby'>ProcessManager</item> 
 *  
 * </code>
 * 
 * @author rsoika
 * 
 */
public class AppoverPlugin extends AbstractPlugin {

	private static Logger logger = Logger.getLogger(AppoverPlugin.class.getName());

	public static String APPROVEDBY = "approvedby";


	/**
	 * computes the approvedBy and appovers name fields.
	 * 
	 * 
	 * @throws PluginException
	 * 
	 **/
	@SuppressWarnings("unchecked")
	@Override
	public ItemCollection run(ItemCollection workitem, ItemCollection documentActivity) throws PluginException {

		ItemCollection evalItemCollection = ResultPlugin.evaluateWorkflowResult(documentActivity, workitem);

		// 1.) test for items with name subprocess_create and create the
		// defined suprocesses
		if (evalItemCollection!=null && evalItemCollection.hasItem(APPROVEDBY)) {
			// extract the groups definitions...
			List<String> groups = evalItemCollection.getItemValue(APPROVEDBY);
			for (String aGroup : groups) {
				if (!workitem.hasItem("nam" + aGroup + "Approvers")) {
					List<String> nameList = workitem.getItemValue("nam" + aGroup);
					// create a new instance of a Vector to avoid setting the same vector as reference!
					List<String> approvers = new ArrayList<String>();
					approvers.addAll(nameList);
					logger.fine("creating approver list: " + aGroup + "=" + approvers);
					workitem.replaceItemValue("nam" + aGroup + "Approvers", approvers);
				}
			}

			// check current approver
			String currentAppover = getWorkflowService().getUserName();
			logger.fine("approved by:  " + currentAppover);
			for (String aGroup : groups) {
				List<String> listApprovers = workitem.getItemValue("nam" + aGroup + "Approvers");
				List<String> listApprovedBy = workitem.getItemValue("nam" + aGroup + "ApprovedBy");

				if (listApprovers.contains(currentAppover) && !listApprovedBy.contains(currentAppover) ) {
					listApprovers.remove(currentAppover);
					listApprovedBy.add(currentAppover);
					workitem.replaceItemValue("nam" + aGroup + "Approvers", listApprovers);
					workitem.replaceItemValue("nam" + aGroup + "ApprovedBy", listApprovedBy);
					logger.fine("new list of approvedby: " + aGroup + "=" + listApprovedBy);
				}
			}
		}

		return workitem;
	}

	

}
