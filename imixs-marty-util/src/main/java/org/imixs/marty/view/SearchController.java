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
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.util.WorkitemHelper;
import org.imixs.marty.workflow.WorkflowEvent;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.jee.faces.workitem.IViewAdapter;
import org.imixs.workflow.jee.faces.workitem.ViewController;
import org.imixs.workflow.plugins.jee.extended.LuceneSearchService;

/**
 * The Marty SearchController extends the Imixs WorklistController and provides
 * custom filter and search queries to request a individual WorkList result. The
 * ItemCollection search filter defines custom filter criteria for a customized
 * search result.
 * 
 * The SearchController provides a inner class 'ViewAdapter' to compute the
 * WorkList result based on the search filter. The SearchController provides two
 * modes. The JPQL mode and the search mode. The ViewAdapter method
 * 'findWorkitems' uses a JQPL statement to return a view result. The
 * ViewAdapter method 'searchWorkitems' performs a Lucene Search to return a
 * search result. Both statements are computed by a IQuerBuilder instance which
 * can be injected.
 * 
 * To customize the result an alternative CDI IQueryBuilder bean an be injected.
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
 * @version 2.2.0
 */

@Named("searchController")
@SessionScoped
public class SearchController extends
		org.imixs.workflow.jee.faces.workitem.WorklistController implements
		Serializable {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(SearchController.class
			.getName());

	private ItemCollection searchFilter = null;

	@Inject
	protected IQueryBuilder queryBuilder = null;

	@EJB
	protected WorkflowService workflowService;
	
	@EJB
	protected LuceneSearchService luceneSearchService;

	/**
	 * Constructor sets the new ViewController
	 */
	public SearchController() {
		super();
		setViewAdapter(new SearchController.ViewAdapter());
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

	@Override
	public void doReset() {
		searchFilter = new ItemCollection();
		searchFilter.replaceItemValue("type", "workitem");
		super.doReset();
	}

	/**
	 * resets the current result and set the page pointer to 0. The searchFilter
	 * will not be reset.
	 * 
	 * @return
	 */
	public void doResetSearchResult() {
		super.doReset();
	}

	/**
	 * Refresh the result and reset the filter "$processid".
	 * 
	 * This method is called by the SelectBox for WorkflowGroup to reset old
	 * $processID
	 * 
	 * Reset paging to 0
	 * 
	 * @param event
	 */
	public void doRefreshWorkflowGroup(AjaxBehaviorEvent event) {
		getSearchFilter().removeItem("$processid");
		doResetSearchResult();
	}

	/**
	 * Resets the search filter but not the search phrase (txtSearch) The method
	 * reset the current result.
	 * 
	 * @param event
	 */
	public void doResetFilter(ActionEvent event) {
		String searchPhrase = searchFilter.getItemValueString("txtSearch");
		searchFilter = new ItemCollection();
		searchFilter.replaceItemValue("type", "workitem");
		super.doReset(event);
		// restore search phrase
		searchFilter.replaceItemValue("txtSearch", searchPhrase);
	}

	/**
	 * Triggers a lucene search based on a search phrase. The search phrase is
	 * stored in the search filter property 'txtSearch' which is evaluated by
	 * the IQueryBuilder.
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

	public IQueryBuilder getQueryBuilder() {
		return queryBuilder;
	}

	public void setQueryBuilder(IQueryBuilder queryBuilder) {
		this.queryBuilder = queryBuilder;
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
		 * Returns a worklist based on a JQPL statement
		 * 
		 * @param controller
		 *            - the view controller
		 * @return list of workitems
		 */
		private List<ItemCollection> findWorkitems(ViewController controller) {
			List<ItemCollection> result = new ArrayList<ItemCollection>();
			if (searchFilter == null)
				return result;
			String sQuery = queryBuilder.getJPQLStatement(searchFilter,
					getView());
			logger.fine("findWorkitems: " + sQuery);
			Collection<ItemCollection> col = workflowService.getEntityService()
					.findAllEntities(sQuery, controller.getRow(),
							controller.getMaxResult());
			// clone the result list to reduce size of workitems
			for (ItemCollection workitem : col) {
				result.add(WorkitemHelper.clone(workitem));
			}
			return result;
		}

		/**
		 * Returns a worklist based on a Lucene search query. IndexFields will
		 * be search by the keyword search. Keyword search in lucene is case
		 * sensitive and did not allow wildcards!
		 * 
		 * @param controller
		 *            - the view controller
		 * @return list of workitems
		 */
		private List<ItemCollection> searchWorkitems(ViewController controller) {
			List<ItemCollection> result = new ArrayList<ItemCollection>();
			if (searchFilter == null)
				return result;

			String sSearchTerm = queryBuilder.getSearchQuery(searchFilter,
					getView());
			// start lucene search
			Collection<ItemCollection> col = null;
			try {
				logger.fine("searchWorkitems: " + sSearchTerm);
				col = luceneSearchService.search(sSearchTerm, workflowService);
			} catch (Exception e) {
				logger.warning("  lucene error!");
				e.printStackTrace();
			}

			// clone the result list to reduce size of workitems
			for (ItemCollection workitem : col) {
				result.add(WorkitemHelper.clone(workitem));
			}
			return result;
		}

	}

}
