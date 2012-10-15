/*******************************************************************************
 *  Imixs Workflow Technology
 *  Copyright (C) 2003, 2008 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika
 *  
 *******************************************************************************/
package org.imixs.marty.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.util.WorkitemHelper;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.faces.workitem.IViewAdapter;
import org.imixs.workflow.jee.faces.workitem.ViewController;

/**
 * The SerachController provides a search and query filter to be used to provide
 * custom workList results depending on the current users selections.
 * 
 * 
 * @author rsoika
 * 
 */

@Named("searchController")
@SessionScoped
public class SearchController implements IViewAdapter, Serializable {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger("org.imixs.marty");

	
	
	private ItemCollection queryFilter = null;
	private ItemCollection searchFilter = null;
	
	@EJB
	private EntityService entityService;

	
	
	
	public ItemCollection getSearchFilter() {
		if (searchFilter==null)
			searchFilter=new ItemCollection();
		return searchFilter;
	}

	public void setSearchFilter(ItemCollection searchFilter) {
		this.searchFilter = searchFilter;
	}

	public ItemCollection getQueryFilter() {
		if (queryFilter == null)
			queryFilter = new ItemCollection();
		return queryFilter;
	}

	public void setQueryFilter(ItemCollection queryFilter) {
		this.queryFilter = queryFilter;
	}
	
	
	
	
	public void reset(ActionEvent event) {
		queryFilter = new ItemCollection();
		searchFilter=new ItemCollection();
	}

	
	/**
	 * This method computes a search result depending on the current query and search filter settings.
	 */
	@Override
	public List<ItemCollection> getViewEntries(ViewController controller) {

		List<ItemCollection> result = new ArrayList<ItemCollection>();
		
		if (queryFilter==null || searchFilter==null)
			return result;
		
		
		int processID = getQueryFilter().getItemValueInteger("$ProcessID");

		List<String> projectList = queryFilter.getItemValue(
				"$UniqueIDRef");

		// trim projectlist
		while (projectList.contains(""))
			projectList.remove("");

		List<String> workflowGroups = queryFilter.getItemValue(
				"txtWorkflowGroup");
		// trim workflowGroups
		while (workflowGroups.contains(""))
			workflowGroups.remove("");

		String type = queryFilter.getItemValueString("Type");
		if ("".equals(type))
			type = "workitem";

		// construct query
		String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";

		if (!projectList.isEmpty())
			sQuery += " JOIN wi.textItems as pref ";
		if (!workflowGroups.isEmpty())
			sQuery += " JOIN wi.textItems as groups ";
		if (processID > 0)
			sQuery += " JOIN wi.integerItems as processid ";

		sQuery += " WHERE wi.type = '" + type + "'";

		if (!projectList.isEmpty()) {
			sQuery += " AND pref.itemName = '$uniqueidref' and pref.itemValue IN (";
			for (String aref : projectList) {
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

		logger.info("loadWorktiems: " + sQuery);
		Collection<ItemCollection> col = entityService.findAllEntities(
				sQuery, controller.getRow(), controller.getMaxResult());

		for (ItemCollection workitem : col) {
			result.add(WorkitemHelper.clone(workitem));
		}

		return result;

	
	}
}
