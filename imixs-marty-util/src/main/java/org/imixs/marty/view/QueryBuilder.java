package org.imixs.marty.view;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;

import org.imixs.marty.ejb.ConfigService;
import org.imixs.workflow.ItemCollection;
/**
 * The default implementation of a QueryBuilder. The SearchController can
 * use custom instances of a IQueryBuilder implementation to customize the
 * search queries.
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

		Date datum = searchFilter.getItemValueDate("datdate");
		if (datum != null) {
			SimpleDateFormat dateformat = new SimpleDateFormat(
					"yyyyMMddHHmm");

			// convert calendar to string
			String sDateValue = dateformat.format(datum);
			if (!"".equals(sDateValue))
				sSearchTerm += " (datdate:\"" + sDateValue + "\") AND";
		}

		String searchphrase = searchFilter.getItemValueString("txtSearch");

		if (!"".equals(searchphrase)) {
			sSearchTerm += " (*" + searchphrase.toLowerCase() + "*)";

		} else
		// cut last AND
		if (sSearchTerm.endsWith("AND"))
			sSearchTerm = sSearchTerm
					.substring(0, sSearchTerm.length() - 3);

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

		String type = searchFilter.getItemValueString("Type");
		if ("".equals(type))
			type = "workitem";

		// construct query
		String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";

		if (!uniqueIdRefList.isEmpty())
			sQuery += " JOIN wi.textItems as pref ";
		if (!workflowGroups.isEmpty())
			sQuery += " JOIN wi.textItems as groups ";
		if (processID > 0)
			sQuery += " JOIN wi.integerItems as processid ";

		sQuery += " WHERE wi.type = '" + type + "'";

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
		ItemCollection config=configService.loadConfiguration("BASIC");
		int sortby=config.getItemValueInteger("Sortby");
		int sortorder =config.getItemValueInteger("Sortorder");
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
