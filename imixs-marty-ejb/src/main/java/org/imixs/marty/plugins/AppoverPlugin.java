package org.imixs.marty.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.plugins.ResultPlugin;
import org.imixs.workflow.plugins.jee.AbstractPlugin;

/**
 * This plug-in manages multiple lists of approvers. A approver list can be
 * declared within the workflow result by the item name "approvedby":
 * 
 * Example:
 * 
 * <pre>
 * {@code
 *  <item name='approvedby'>SpaceTeam</item>
 * }
 * </pre>
 * 
 * The field name from the source attribute must be prefixed with 'nam'.
 * 
 * The result will be stored in the following attributes:
 * 
 * <pre>
 * {@code
 *  nam[ITEMNAME]Approvers 
 *  nam[ITEMNAME]ApprovedBy
 * }
 * </pre>
 * 
 * If the source list is updated during the approving process, the plugin will
 * add new userIDs if these new UserIDs are not yet listed in the
 * nam[ITEMNAME]ApprovedBy field.
 * 
 * 
 * @author rsoika
 * @version 2.0
 * 
 */
public class AppoverPlugin extends AbstractPlugin {

	private static Logger logger = Logger.getLogger(AppoverPlugin.class.getName());

	public static String APPROVEDBY = "approvedby";

	private WorkflowService workflowService = null;

	/**
	 * Fetch workflowService and entityService from WorkflowContext
	 */
	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
		// check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			workflowService = (WorkflowService) actx;
		}

	}

	/**
	 * computes the approvedBy and appovers name fields.
	 * 
	 * 
	 * @throws PluginException
	 * 
	 **/
	@SuppressWarnings("unchecked")
	@Override
	public int run(ItemCollection workitem, ItemCollection documentActivity) throws PluginException {

		ItemCollection evalItemCollection = ResultPlugin.evaluateWorkflowResult(documentActivity, workitem);

		// 1.) test for items with name subprocess_create and create the
		// defined suprocesses
		if (evalItemCollection != null && evalItemCollection.hasItem(APPROVEDBY)) {
			// extract the groups definitions...
			List<String> groups = evalItemCollection.getItemValue(APPROVEDBY);
			for (String aGroup : groups) {
				List<String> nameList = workitem.getItemValue("nam" + aGroup);
				// create a new instance of a Vector to avoid setting the
				// same vector as reference!
				List<String> newAppoverList = new ArrayList<String>();
				newAppoverList.addAll(nameList);
				if (!workitem.hasItem("nam" + aGroup + "Approvers")) {
					logger.fine("creating new approver list: " + aGroup + "=" + newAppoverList);
					workitem.replaceItemValue("nam" + aGroup + "Approvers", newAppoverList);
				} else {
					// verify if a new member of the existing approvers is available...
					// (issue #150)
					List<String> listApprovedBy = workitem.getItemValue("nam" + aGroup + "ApprovedBy");
					List<String> listApprovers = workitem.getItemValue("nam" + aGroup + "Approvers");
					boolean update = false;
					for (String approver : newAppoverList) {
						if (!listApprovedBy.contains(approver) && !listApprovers.contains(approver)) {
							// add the new member to the existing approver list
							logger.fine("adding new approver to list 'nam" + aGroup + "Approvers'");
							listApprovers.add(approver);
							update = true;
						}
					}
					if (update) {
						logger.fine("updating approver list 'nam" + aGroup + "Approvers'");
						workitem.replaceItemValue("nam" + aGroup + "Approvers", listApprovers);
					}
				}
			}

			// check current approver
			String currentAppover = workflowService.getUserName();
			logger.fine("approved by:  " + currentAppover);
			for (String aGroup : groups) {
				List<String> listApprovers = workitem.getItemValue("nam" + aGroup + "Approvers");
				List<String> listApprovedBy = workitem.getItemValue("nam" + aGroup + "ApprovedBy");

				if (listApprovers.contains(currentAppover) && !listApprovedBy.contains(currentAppover)) {
					listApprovers.remove(currentAppover);
					listApprovedBy.add(currentAppover);
					workitem.replaceItemValue("nam" + aGroup + "Approvers", listApprovers);
					workitem.replaceItemValue("nam" + aGroup + "ApprovedBy", listApprovedBy);
					logger.fine("new list of approvedby: " + aGroup + "=" + listApprovedBy);
				}
			}
		}

		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int arg0) throws PluginException {

	}

}
