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
		sSearchTerm += "(" +sTypeQuery + ") AND";

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

		List<String> aRefList = searchFilter.getItemValue("txtCoreProcessRef");
		aRefList.addAll(searchFilter.getItemValue("txtProjectRef"));
		// create a Set!...
		Set<String> uniqueIdRefList = new HashSet<String>(aRefList);

		// trim projectlist
		while (uniqueIdRefList.contains(""))
			uniqueIdRefList.remove("");

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

		if (!uniqueIdRefList.isEmpty())
			sQuery += " JOIN wi.textItems as pref ";
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

		if (!uniqueIdRefList.isEmpty()) {
			sQuery += " AND pref.itemName = '$uniqueidref' and pref.itemValue IN (";
			for (String aref : uniqueIdRefList) {
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
