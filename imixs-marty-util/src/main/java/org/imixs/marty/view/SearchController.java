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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.config.SetupController;
import org.imixs.marty.profile.UserController;
import org.imixs.marty.util.WorkitemHelper;
import org.imixs.marty.workflow.WorkflowEvent;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.jee.faces.workitem.IViewAdapter;
import org.imixs.workflow.jee.faces.workitem.ViewController;
import org.imixs.workflow.plugins.jee.extended.LucenePlugin;

/**
 * The Marty SearchController extends the Imixs WorklistController and provides
 * ItemCollection for a search filter. The search filter is used for customized
 * search results.
 * 
 * The SearchController provides a inner class 'ViewAdapter' to provide the
 * result of a query. The ViewAdapter method 'findWorkitems' uses a JQPL
 * statement to return a view result. The ViewAdapter method 'searchWorkitems'
 * performs a Lucene Search to return a search result.
 * 
 * To adapt the view result it is not necessary to override the ViewAdapter
 * itself. To customize the result, the QueryBuilder can be customized.
 * QueryBuilder is implementing the IQueryBuilder interface.
 * 
 * 
 * The SearchController has a set of predefined filter properties:
 * 
 * <ul>
 * <li>
 * txtProcessRef = holds a reference to a core process entity</li>
 * <li>
 * txtSpaceRef = holds a list of project references</li>
 * </ul>
 * 
 * @See QueryBuilder, IQueryBuilder
 * @author rsoika
 * @version 1.0.0
 */

@Named("searchController")
@SessionScoped
public class SearchController extends
		org.imixs.workflow.jee.faces.workitem.WorklistController implements
		Serializable {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(SearchController.class.getName());

	private String viewTitle = null;
	private IViewAdapter viewAdapter = null;

	// private ItemCollection queryFilter = null;
	private ItemCollection searchFilter = null;

	// view filter
	public static final String QUERY_WORKLIST_BY_OWNER = "worklist.owner";
	public static final String QUERY_WORKLIST_BY_CREATOR = "worklist.creator";
	public static final String QUERY_WORKLIST_BY_AUTHOR = "worklist.author";
	public static final String QUERY_WORKLIST_ALL = "worklist";
	public static final String QUERY_WORKLIST_ARCHIVE = "archive";
	public static final String QUERY_WORKLIST_DELETIONS = "deletions";
	public static final int SORT_BY_CREATED = 0;
	public static final int SORT_BY_MODIFIED = 1;
	public static final int SORT_ORDER_DESC = 0;
	public static final int SORT_ORDER_ASC = 1;

	@Inject
	protected UserController userController = null;

	@Inject
	protected SetupController setupController = null;

	@Inject
	protected IQueryBuilder queryBuilder = null;

	@EJB
	protected WorkflowService workflowService;

	@EJB
	protected EntityService entityService;

	/**
	 * Constructor sets the new ViewController
	 */
	public SearchController() {
		super();
		setViewAdapter(new ViewAdapter());
	}

	/**
	 * Resets the search filter and the current result.
	 * 
	 * @param event
	 */
	@Override
	public void doReset(ActionEvent event) {
		searchFilter = new ItemCollection();
		searchFilter.replaceItemValue("type", "workitem");

		super.doReset(event);
	}
	
	/**
	 * Resets the search filter but not the search phrase (txtSearch)
	 * The method reset the current result.
	 * 
	 * @param event
	 */
	public void doResetFilter(ActionEvent event) {
		String searchPhrase=searchFilter.getItemValueString("txtSearch");
		
		searchFilter = new ItemCollection();
		searchFilter.replaceItemValue("type", "workitem");

		super.doReset(event);
		
		// restore search phrase
		searchFilter.replaceItemValue("txtSearch", searchPhrase);
	}

	/**
	 * Searches for the a search phrase. The search phrase is stored in the
	 * search filter property 'txtSearch' which is evaluated by the ViewAdapter.
	 * 
	 * @param phrase
	 *            - search phrase
	 * @param action
	 *            - jsf navigation action
	 */
	public String search(String phrase, String action) {

		searchFilter.replaceItemValue("txtSearch", phrase);

		return action;
	}

	/**
	 * WorkflowEvent listener listens to WORKITEM events and reset the result
	 * list after changing a workitem.
	 * 
	 * @param workflowEvent
	 * 
	 **/
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent) {
		if (workflowEvent == null || workflowEvent.getWorkitem() == null) {
			return;
		}

		// skip if not a workItem...
		if (!workflowEvent.getWorkitem().getItemValueString("type")
				.startsWith("workitem"))
			return;

		if (WorkflowEvent.WORKITEM_AFTER_PROCESS == workflowEvent
				.getEventType()
				|| WorkflowEvent.WORKITEM_AFTER_SOFTDELETE == workflowEvent
						.getEventType()) {
			doRefresh();
		}

	}

	public ItemCollection getSearchFilter() {
		if (searchFilter == null)
			searchFilter = new ItemCollection();
		return searchFilter;
	}

	public void setSearchFilter(ItemCollection searchFilter) {
		this.searchFilter = searchFilter;
	}

	// public ItemCollection getQueryFilter() {
	// if (queryFilter == null)
	// queryFilter = new ItemCollection();
	// return queryFilter;
	// }
	//
	// public void setQueryFilter(ItemCollection queryFilter) {
	// this.queryFilter = queryFilter;
	// }

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
			Locale locale = userController.getLocale();
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

	/*
	 * public IQueryBuilder getQueryBuilder() { if (queryBuilder == null)
	 * queryBuilder = new QueryBuilder();
	 * 
	 * return queryBuilder; }
	 * 
	 * public void setQueryBuilder(IQueryBuilder queryBuilder) {
	 * this.queryBuilder = queryBuilder; }
	 */

	public IViewAdapter getViewAdapter() {
		if (viewAdapter == null)
			viewAdapter = new ViewAdapter();

		return viewAdapter;
	}

	public void setViewAdapter(IViewAdapter viewAdapter) {
		this.viewAdapter = viewAdapter;
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
			if (!queryBuilder.isSearchMode(searchFilter))
				return findWorkitems(controller);
			else
				return searchWorkitems(controller);

		}

		/**
		 * This method computes a search result depending on the current query
		 * filter settings.
		 */
		private List<ItemCollection> findWorkitems(ViewController controller) {

			List<ItemCollection> result = new ArrayList<ItemCollection>();

			if (searchFilter == null)
				return result;

			String sQuery = queryBuilder.getJPQLStatement(searchFilter);

			logger.info("loadWorktiems: " + sQuery);
			Collection<ItemCollection> col = entityService.findAllEntities(
					sQuery, controller.getRow(), controller.getMaxResult());

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

			String sSearchTerm = queryBuilder.getSearchQuery(searchFilter);

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

	}

}
