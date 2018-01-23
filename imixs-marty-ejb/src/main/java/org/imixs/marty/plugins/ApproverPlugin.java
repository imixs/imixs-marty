package org.imixs.marty.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.PluginException;

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
public class ApproverPlugin extends AbstractPlugin {

	private static Logger logger = Logger.getLogger(ApproverPlugin.class.getName());

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

		ItemCollection evalItemCollection = this.getWorkflowService().evalWorkflowResult(documentActivity, workitem);

		// 1.) test for items with name subprocess_create and create the
		// defined suprocesses
		if (evalItemCollection != null && evalItemCollection.hasItem(APPROVEDBY)) {
			// extract the groups definitions...
			List<String> groups = evalItemCollection.getItemValue(APPROVEDBY);
			for (String aGroup : groups) {
				List<String> nameList = workitem.getItemValue("nam" + aGroup);

				// remove empty entries...
				nameList.removeIf(item -> item == null || "".equals(item));

				// create a new instance of a Vector to avoid setting the
				// same vector as reference!
				List<String> newAppoverList = new ArrayList<String>();
				newAppoverList.addAll(nameList);
				if (!workitem.hasItem("nam" + aGroup + "Approvers")) {
					logger.fine("creating new approver list: " + aGroup + "=" + newAppoverList);
					workitem.replaceItemValue("nam" + aGroup + "Approvers", newAppoverList);
				} else {
					// verify if a new member of the existing approvers is
					// available...
					// (issue #150)
					List<String> listApprovedBy = workitem.getItemValue("nam" + aGroup + "ApprovedBy");
					List<String> listApprovers = workitem.getItemValue("nam" + aGroup + "Approvers");
					boolean update = false;
					for (String approver : newAppoverList) {
						if (!listApprovedBy.contains(approver) && !listApprovers.contains(approver)) {
							// add the new member to the existing approver list
							logger.fine("adding new approver to list 'nam" + aGroup + "Approvers'");
							listApprovers.add(approver);
							// remove empty entries...
							listApprovers.removeIf(item -> item == null || "".equals(item));

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
			String currentAppover = getWorkflowService().getUserName();
			logger.fine("approved by:  " + currentAppover);
			for (String aGroup : groups) {
				List<String> listApprovers = workitem.getItemValue("nam" + aGroup + "Approvers");
				List<String> listApprovedBy = workitem.getItemValue("nam" + aGroup + "ApprovedBy");

				if (listApprovers.contains(currentAppover) && !listApprovedBy.contains(currentAppover)) {
					listApprovers.remove(currentAppover);
					listApprovedBy.add(currentAppover);

					// remove empty entries...
					listApprovers.removeIf(item -> item == null || "".equals(item));
					listApprovedBy.removeIf(item -> item == null || "".equals(item));

					workitem.replaceItemValue("nam" + aGroup + "Approvers", listApprovers);
					workitem.replaceItemValue("nam" + aGroup + "ApprovedBy", listApprovedBy);
					logger.fine("new list of approvedby: " + aGroup + "=" + listApprovedBy);
				}
			}
		}

		return workitem;
	}

}
