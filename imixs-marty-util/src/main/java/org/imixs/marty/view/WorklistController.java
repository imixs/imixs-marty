/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
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
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.marty.view;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.config.SetupController;
import org.imixs.marty.profile.UserController;
import org.imixs.marty.util.WorkitemHelper;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.jee.faces.workitem.IViewAdapter;
import org.imixs.workflow.jee.faces.workitem.ViewController;
import org.imixs.workflow.plugins.jee.extended.LucenePlugin;

/**
 * The Marty WorklistController extends the Imixs WorklistController and
 * provides ItemCollections for a search and a query filter. These
 * ItemCollections can be used for customized search results.
 * 
 * The method findWorkitems uses a JQPL statement to return a view result. The
 * method searchWorkitems performs a Lucene Search to return a search result.
 * 
 * Both statements are provided by an IQueryBuilder. The IQueryBuilder interface
 * can be adapte to customize search results
 * 
 * 
 * The Controller is ViewScoped and used as a ManagedBean instead of a CDI bean.
 * 
 * @author rsoika
 * @version 1.0.0
 */
// @ManagedBean(name = "worklistController")
// @ViewScoped

@Named("worklistController")
@SessionScoped
public class WorklistController extends
		org.imixs.workflow.jee.faces.workitem.WorklistController implements
		Serializable {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger("org.imixs.marty");

	private String viewTitle = null;
	private IViewAdapter viewAdapter = null;
	private IQueryBuilder queryBuilder = null;
	private ItemCollection queryFilter = null;
	private ItemCollection searchFilter = null;

	// view filter
	final String QUERY_WORKLIST_BY_OWNER = "worklist.owner";
	final String QUERY_WORKLIST_BY_CREATOR = "worklist.creator";
	final String QUERY_WORKLIST_BY_AUTHOR = "worklist.author";
	final String QUERY_WORKLIST_ALL = "worklist";
	final String QUERY_WORKLIST_ARCHIVE = "archive";
	final String QUERY_WORKLIST_DELETIONS = "deletions";

	@Inject
	private UserController userController = null;

	@Inject
	private SetupController setupController = null;

	@EJB
	private WorkflowService workflowService;

	@EJB
	private EntityService entityService;

	/**
	 * Constructor sets the new ViewController
	 */
	public WorklistController() {
		super();
		setViewAdapter(new ViewAdapter());
	}

	/**
	 * Resets the search and query filter and also the worklist.
	 * 
	 * @param event
	 */
	public void doResetFilter(ActionEvent event) {
		queryFilter = new ItemCollection();
		searchFilter = new ItemCollection();

		doReset(event);
	}

	public ItemCollection getSearchFilter() {
		if (searchFilter == null)
			searchFilter = new ItemCollection();
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

	@Override
	public void setView(String view) {
		// reset view title
		viewTitle = null;
		super.setView(view);
	}

	/**
	 * This method computes a internationalized view title from the property
	 * 'view'
	 * 
	 * @param viewTitle
	 */
	public String getViewTitle() {
		if (viewTitle != null)
			return viewTitle;
		// compute view title
		String viewName = getView();
		try {
			Locale locale = new Locale(userController.getLocale());
			ResourceBundle rb = null;
			rb = ResourceBundle.getBundle("bundle.workitem", locale);
			viewTitle = rb.getString(viewName);
		} catch (Exception e) {
			logger.warning("no view title defined in resource bundle for view name '"
					+ viewName);

			viewTitle = viewName + ":undefined";
		}
		return viewTitle;
	}

	public IQueryBuilder getQueryBuilder() {
		if (queryBuilder == null)
			queryBuilder = new QueryBuilder();

		return queryBuilder;
	}

	public void setQueryBuilder(IQueryBuilder queryBuilder) {
		this.queryBuilder = queryBuilder;
	}

	public IViewAdapter getViewAdapter() {
		if (viewAdapter == null)
			viewAdapter = new ViewAdapter();

		return viewAdapter;
	}

	public void setViewAdapter(IViewAdapter viewAdapter) {
		this.viewAdapter = viewAdapter;
	}

	/**
	 * This method computes a search result depending on the current query
	 * filter settings.
	 */
	private List<ItemCollection> findWorkitems(ViewController controller) {

		List<ItemCollection> result = new ArrayList<ItemCollection>();

		if (queryFilter == null)
			return result;

		String sQuery = this.getQueryBuilder().getJPQLStatement(queryFilter);

		logger.info("loadWorktiems: " + sQuery);
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				controller.getRow(), controller.getMaxResult());

		for (ItemCollection workitem : col) {
			result.add(WorkitemHelper.clone(workitem));
		}

		return result;

	}

	/**
	 * Creates a search term depending on the provided search fields.
	 * IndexFields will be search by the keyword search. Keyword search in
	 * lucene is case sensitive and did not allow wildcards!
	 * 
	 * 
	 * @param event
	 */
	private List<ItemCollection> searchWorkitems(ViewController controller) {

		List<ItemCollection> result = new ArrayList<ItemCollection>();

		if (searchFilter == null)
			return result;

		String sSearchTerm = getQueryBuilder().getSearchQuery(searchFilter);

		// start lucene search
		Collection<ItemCollection> col = null;
		try {

			logger.fine("SearchQuery=" + sSearchTerm);
			col = LucenePlugin.search(sSearchTerm, workflowService);

		} catch (Exception e) {
			logger.warning("  lucene error!");
			e.printStackTrace();
		}

		// clone result
		for (ItemCollection workitem : col) {
			result.add(WorkitemHelper.clone(workitem));
		}

		return result;

	}

	protected class ViewAdapter implements IViewAdapter {
		/**
		 * This method computes a search result depending on the current query
		 * and search filter settings. If a search phrase exists (txtSearch)
		 * then the method searchWorkitems is called. Otherwise the result list
		 * is computed by a JQPL statement
		 */
		@Override
		public List<ItemCollection> getViewEntries(ViewController controller) {
			// test if a search phrase exists
			if (getSearchFilter().getItemValueString("txtSearch").isEmpty())
				return findWorkitems(controller);
			else
				return searchWorkitems(controller);

		}
	}

	/**
	 * The default implementation of a QueryBuilder. The SearchController can
	 * use custom instances of a IQueryBuilder implementation to customize the
	 * search queries.
	 * 
	 * @author rsoika
	 * 
	 */
	protected class QueryBuilder implements IQueryBuilder {

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
		public String getJPQLStatement(ItemCollection queryFilter) {
			int processID = getQueryFilter().getItemValueInteger("$ProcessID");

			List<String> projectList = queryFilter.getItemValue("$UniqueIDRef");

			// trim projectlist
			while (projectList.contains(""))
				projectList.remove("");

			List<String> workflowGroups = queryFilter
					.getItemValue("txtWorkflowGroup");
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

			return sQuery;
		}

	}

}
