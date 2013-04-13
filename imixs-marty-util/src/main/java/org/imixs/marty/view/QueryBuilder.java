package org.imixs.marty.view;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;

import org.imixs.marty.ejb.ConfigService;
import org.imixs.workflow.ItemCollection;

/**
 * The default implementation of a QueryBuilder. The SearchController can use
 * custom instances of a IQueryBuilder implementation to customize the search
 * queries.
 * 
 * @author rsoika
 * 
 */
@ApplicationScoped
public class QueryBuilder implements IQueryBuilder {

	@EJB
	private ConfigService configService;

	@Override
	public String getSearchQuery(ItemCollection searchFilter) {
		String sSearchTerm = "";

		List<String> typeList = searchFilter.getItemValue("Type");
		if (typeList.isEmpty() || "".equals(typeList.get(0))) {
			typeList = Arrays.asList(new String[] { "workitem",
					"workitemarchive" });
		}

		// convert type list into comma separated list
		String sTypeQuery = "";
		Iterator<String> iterator = typeList.iterator();
		while (iterator.hasNext()) {
			sTypeQuery += "type:\"" + iterator.next() + "\"";
			if (iterator.hasNext())
				sTypeQuery += " OR ";
		}
		sSearchTerm += "(" + sTypeQuery + ") AND";

		List<String> processRefList = searchFilter
				.getItemValue("txtProcessRef");
		List<String> spacesRefList = searchFilter.getItemValue("txtSpaceRef");

		// trim projectlist
		while (processRefList.contains(""))
			processRefList.remove("");
		while (spacesRefList.contains(""))
			spacesRefList.remove("");

		// process ref
		if (!processRefList.isEmpty()) {
			sSearchTerm += " (";
			iterator = processRefList.iterator();
			while (iterator.hasNext()) {
				sSearchTerm += "$uniqueidref:\"" + iterator.next() + "\"";
				if (iterator.hasNext())
					sSearchTerm += " OR ";
			}
			sSearchTerm += " ) AND";
		}

		// Space ref
		if (!spacesRefList.isEmpty()) {
			sSearchTerm += " (";
			iterator = spacesRefList.iterator();
			while (iterator.hasNext()) {
				sSearchTerm += "$uniqueidref:\"" + iterator.next() + "\"";
				if (iterator.hasNext())
					sSearchTerm += " OR ";
			}
			sSearchTerm += " ) AND";
		}

		// Workflow Group...
		List<String> workflowGroups = searchFilter
				.getItemValue("txtWorkflowGroup");
		// trim workflowGroups
		while (workflowGroups.contains(""))
			workflowGroups.remove("");

		if (!workflowGroups.isEmpty()) {
			sSearchTerm += " (";
			iterator = workflowGroups.iterator();
			while (iterator.hasNext()) {
				sSearchTerm += "txtworkflowgroup:\"" + iterator.next() + "\"";
				if (iterator.hasNext())
					sSearchTerm += " OR ";
			}
			sSearchTerm += " ) AND";

		}

		int processID = searchFilter.getItemValueInteger("$ProcessID");
		if (processID > 0)
			sSearchTerm += " ($processid:" + processID + ") AND";

		// Search phrase....
		String searchphrase = searchFilter.getItemValueString("txtSearch");

		if (!"".equals(searchphrase)) {
			sSearchTerm += " (*" + searchphrase.toLowerCase() + "*)";

		} else
		// cut last AND
		if (sSearchTerm.endsWith("AND"))
			sSearchTerm = sSearchTerm.substring(0, sSearchTerm.length() - 3);

		return sSearchTerm;
	}

	@Override
	@SuppressWarnings("unchecked")
	public String getJPQLStatement(ItemCollection searchFilter) {
		int processID = searchFilter.getItemValueInteger("$ProcessID");

		List<String> processRefList = searchFilter
				.getItemValue("txtProcessRef");
		List<String> spacesRefList = searchFilter.getItemValue("txtSpaceRef");

		// trim projectlist
		while (processRefList.contains(""))
			processRefList.remove("");
		while (spacesRefList.contains(""))
			spacesRefList.remove("");

		List<String> workflowGroups = searchFilter
				.getItemValue("txtWorkflowGroup");
		// trim workflowGroups
		while (workflowGroups.contains(""))
			workflowGroups.remove("");

		List<String> typeList = searchFilter.getItemValue("Type");
		if (typeList.isEmpty() || "".equals(typeList.get(0))) {
			typeList = Arrays.asList(new String[] { "workitem",
					"workitemarchive" });
		}

		// construct query
		String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";

		if (!processRefList.isEmpty())
			sQuery += " JOIN wi.textItems as pref ";
		if (!spacesRefList.isEmpty())
			sQuery += " JOIN wi.textItems as sref ";
		if (!workflowGroups.isEmpty())
			sQuery += " JOIN wi.textItems as groups ";
		if (processID > 0)
			sQuery += " JOIN wi.integerItems as processid ";

		// convert type list into comma separated list
		String sType = "";
		for (String aValue : typeList) {
			sType += "'" + aValue + "',";
		}
		sType = sType.substring(0, sType.length() - 1);

		sQuery += " WHERE wi.type IN(" + sType + ")";

		// process Ref...
		if (!processRefList.isEmpty()) {
			sQuery += " AND pref.itemName = '$uniqueidref'";
			sQuery += " AND pref.itemValue IN (";
			for (String aref : processRefList) {
				sQuery += "'" + aref + "',";
			}
			sQuery = sQuery.substring(0, sQuery.length() - 1);
			sQuery += " )";
		}

		// Spaces Ref...
		if (!spacesRefList.isEmpty()) {
			sQuery += " AND sref.itemName = '$uniqueidref'";
			sQuery += " AND sref.itemValue IN (";
			for (String aref : spacesRefList) {
				sQuery += "'" + aref + "',";
			}
			sQuery = sQuery.substring(0, sQuery.length() - 1);
			sQuery += " )";
		}

		if (!workflowGroups.isEmpty()) {
			sQuery += " AND groups.itemName = 'txtworkflowgroup' and groups.itemValue IN (";
			for (String agroup : workflowGroups) {
				sQuery += "'" + agroup + "',";
			}
			sQuery = sQuery.substring(0, sQuery.length() - 1);
			sQuery += " )";

		}

		if (processID > 0)
			sQuery += " AND processid.itemName = '$processid' AND processid.itemValue ='"
					+ processID + "'";

		// add ORDER BY phrase

		// read configuration for the sort order
		ItemCollection config = configService.loadConfiguration("BASIC");
		int sortby = config.getItemValueInteger("Sortby");
		int sortorder = config.getItemValueInteger("Sortorder");
		sQuery += " ORDER BY wi.";
		if (sortby == SearchController.SORT_BY_CREATED)
			sQuery += "created ";
		else
			sQuery += "modified ";
		if (sortorder == SearchController.SORT_ORDER_ASC)
			sQuery += "asc";
		else
			sQuery += "desc";

		return sQuery;
	}

}
