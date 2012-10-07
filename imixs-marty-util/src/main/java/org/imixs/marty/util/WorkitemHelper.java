package org.imixs.marty.util;

import org.imixs.workflow.ItemCollection;

/**
 * The WorkitemHelper provides methods to clone, compare and sort workitems.
 * 
 * @author rsoika
 * 
 */
public class WorkitemHelper {

	/**
	 * This method clones the given workItem with a minimum of attributes.
	 * 
	 * @param aWorkitem
	 * @return
	 */
	public static ItemCollection clone(ItemCollection aWorkitem) {
		ItemCollection clone = new ItemCollection();

		clone.replaceItemValue("$UniqueID", aWorkitem.getItemValue("$UniqueID"));
		clone.replaceItemValue("$UniqueIDRef",
				aWorkitem.getItemValue("$UniqueIDRef"));
		clone.replaceItemValue("$ModelVersion",
				aWorkitem.getItemValue("$ModelVersion"));
		clone.replaceItemValue("$ProcessID",
				aWorkitem.getItemValue("$ProcessID"));
		clone.replaceItemValue("$Created", aWorkitem.getItemValue("$Created"));
		clone.replaceItemValue("$Modified", aWorkitem.getItemValue("$Modified"));
		clone.replaceItemValue("txtName", aWorkitem.getItemValue("txtName"));

		clone.replaceItemValue("txtWorkflowStatus",
				aWorkitem.getItemValue("txtWorkflowStatus"));
		clone.replaceItemValue("txtWorkflowGroup",
				aWorkitem.getItemValue("txtWorkflowGroup"));
		clone.replaceItemValue("namOwner", aWorkitem.getItemValue("namOwner"));

		clone.replaceItemValue("txtWorkflowSummary",
				aWorkitem.getItemValue("txtWorkflowSummary"));
		clone.replaceItemValue("txtWorkflowAbstract",
				aWorkitem.getItemValue("txtWorkflowAbstract"));

		return clone;

	}
}
