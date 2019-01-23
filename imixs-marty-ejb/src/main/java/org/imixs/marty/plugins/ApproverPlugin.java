package org.imixs.marty.plugins;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
 * <p>
 * If the attribute 'refresh' is set to true, the list nam[ITEMNAME]Approvers
 * will be updated (default is true).
 * <p>
 * If the attribute 'reset' is set to true, the list nam[ITEMNAME]Approvers will
 * be reseted and nam[ITEMNAME]ApprovedBy will be empty.
 * 
 * 
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
		boolean refresh = false;
		boolean reset = false;

		ItemCollection evalItemCollection = this.getWorkflowService().evalWorkflowResult(documentActivity, workitem);

		// test for items with name 'approvedby'
		if (evalItemCollection != null && evalItemCollection.hasItem(APPROVEDBY)) {

			// test refresh
			refresh = true;
			if ("false".equals(evalItemCollection.getItemValueString(APPROVEDBY + ".refresh"))) {
				refresh = false;
			}
			logger.fine("refresh=" + refresh);

			// test reset
			reset = false;
			if ("true".equals(evalItemCollection.getItemValueString(APPROVEDBY + ".reset"))) {
				reset = true;
			}
			logger.fine("reset=" + reset);

			// 1.) extract the groups definitions
			List<String> groups = evalItemCollection.getItemValue(APPROVEDBY);

			// 2.) iterate over all definitions
			for (String aGroup : groups) {

				// fetch name list...
				List<String> nameList = workitem.getItemValue("nam" + aGroup);
				// remove empty entries...
				nameList.removeIf(item -> item == null || "".equals(item));
				// create a new instance of a Vector to avoid setting the
				// same vector as reference! We also distinct the List here.
				List<String> newAppoverList = nameList.stream().distinct().collect(Collectors.toList());

				if (!workitem.hasItem("nam" + aGroup + "Approvers") || reset) {
					logger.fine("creating new approver list: " + aGroup + "=" + newAppoverList);
					workitem.replaceItemValue("nam" + aGroup + "Approvers", newAppoverList);
					workitem.removeItem("nam" + aGroup + "ApprovedBy");
				} else {

					// refresh approver list.....
					if (refresh) {
						refreshApprovers(workitem, aGroup);
					}

					// 2.) add current approver to approvedBy.....
					String currentAppover = getWorkflowService().getUserName();
					List<String> listApprovedBy = workitem.getItemValue("nam" + aGroup + "ApprovedBy");
					List<String> listApprovers = workitem.getItemValue("nam" + aGroup + "Approvers");

					logger.fine("approved by:  " + currentAppover);
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

		}

		return workitem;
	}

	/**
	 * This method verify if a new member of the existing approvers is available and
	 * adds new member into the field 'nam" + aGroup + "Approvers'. (issue #150)
	 */
	@SuppressWarnings("unchecked")
	void refreshApprovers(ItemCollection workitem, String aGroup) {
		List<String> nameList = workitem.getItemValue("nam" + aGroup);
		// remove empty entries...
		nameList.removeIf(item -> item == null || "".equals(item));

		// create a new instance of a Vector to avoid setting the
		// same vector as reference! We also distinct the List here.
		List<String> newAppoverList = nameList.stream().distinct().collect(Collectors.toList());

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
