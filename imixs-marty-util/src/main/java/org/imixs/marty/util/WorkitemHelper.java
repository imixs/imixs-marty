package org.imixs.marty.util;

import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;

/**
 * The WorkitemHelper provides methods to clone, compare and sort workitems.
 * 
 * @author rsoika
 * 
 */
public class WorkitemHelper {
	private static Logger logger = Logger.getLogger("org.imixs.marty");

	/**
	 * This method clones the given workItem with a minimum of attributes.
	 * 
	 * @param aWorkitem
	 * @return
	 */
	public static ItemCollection clone(ItemCollection aWorkitem) {
		ItemCollection clone = new ItemCollection();

		// clone the standard WorkItem properties
		clone.replaceItemValue("Type", aWorkitem.getItemValue("Type"));
		clone.replaceItemValue("$UniqueID", aWorkitem.getItemValue("$UniqueID"));
		clone.replaceItemValue("$UniqueIDRef",
				aWorkitem.getItemValue("$UniqueIDRef"));
		clone.replaceItemValue("$ModelVersion",
				aWorkitem.getItemValue("$ModelVersion"));
		clone.replaceItemValue("$ProcessID",
				aWorkitem.getItemValue("$ProcessID"));
		clone.replaceItemValue("$Created", aWorkitem.getItemValue("$Created"));
		clone.replaceItemValue("$Modified", aWorkitem.getItemValue("$Modified"));
		clone.replaceItemValue("$isAuthor", aWorkitem.getItemValue("$isAuthor"));

		clone.replaceItemValue("txtName", aWorkitem.getItemValue("txtName"));

		clone.replaceItemValue("txtWorkflowStatus",
				aWorkitem.getItemValue("txtWorkflowStatus"));
		clone.replaceItemValue("txtWorkflowGroup",
				aWorkitem.getItemValue("txtWorkflowGroup"));
		clone.replaceItemValue("namCreator",
				aWorkitem.getItemValue("namCreator"));
		clone.replaceItemValue("namCurrentEditor",
				aWorkitem.getItemValue("namCurrentEditor"));
		clone.replaceItemValue("namOwner", aWorkitem.getItemValue("namOwner"));

		clone.replaceItemValue("txtWorkflowSummary",
				aWorkitem.getItemValue("txtWorkflowSummary"));
		clone.replaceItemValue("txtWorkflowAbstract",
				aWorkitem.getItemValue("txtWorkflowAbstract"));
		clone.replaceItemValue("txtWorkflowImageURL",
				aWorkitem.getItemValue("txtWorkflowImageURL"));

		// clone the marty WorkItem properties....
		if (aWorkitem.hasItem("txtName"))
			clone.replaceItemValue("txtName", aWorkitem.getItemValue("txtName"));

		if (aWorkitem.hasItem("txtProjectName"))
			clone.replaceItemValue("txtProjectName",
					aWorkitem.getItemValue("txtProjectName"));

		if (aWorkitem.hasItem("datdate"))
			clone.replaceItemValue("datdate", aWorkitem.getItemValue("datdate"));

		if (aWorkitem.hasItem("datFrom"))
			clone.replaceItemValue("datFrom", aWorkitem.getItemValue("datFrom"));

		if (aWorkitem.hasItem("datTo"))
			clone.replaceItemValue("datTo", aWorkitem.getItemValue("datTo"));

		if (aWorkitem.hasItem("numsequencenumber"))
			clone.replaceItemValue("numsequencenumber",
					aWorkitem.getItemValue("numsequencenumber"));

		return clone;

	}

	/**
	 * This method tests if a given WorkItem matches a filter expression. The
	 * expression is expected in a column separated list of reg expressions for
	 * Multiple properties. - e.g.:
	 * 
	 * <code>(txtWorkflowGroup:Invoice)($ProcessID:1...)</code>
	 * 
	 * @param workitem
	 *            - workItem to be tested
	 * @param filter
	 *            - combined regex to test different fields
	 * @return - true if filter matches filter expression.
	 */
	public static boolean matches(ItemCollection workitem, String filter) {

		if (filter == null || "".equals(filter.trim()))
			return true;

		// split columns
		StringTokenizer regexTokens = new StringTokenizer(filter, ")");
		while (regexTokens.hasMoreElements()) {
			String regEx = regexTokens.nextToken();
			// remove columns
			regEx.replace("(", "");
			regEx.replace(")", "");
			regEx.replace(",", "");
			// test if ':' found
			if (regEx.indexOf(':') > -1) {
				regEx = regEx.trim();
				String itemName = regEx.substring(0, regEx.indexOf(':'));
				regEx = regEx.substring(regEx.indexOf(':') + 1);
				List<Object> itemValues = workitem.getItemValue(itemName);
				for (Object aValue : itemValues) {
					if (!aValue.toString().matches(regEx)) {
						logger.fine("Value '" + aValue + "' did not match : "
								+ regEx);
						return false;
					}
				}

			}
		}
		// workitem matches criteria
		return true;
	}
}
