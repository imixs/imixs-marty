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
package org.imixs.marty.workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.workflow.WorkflowController;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.lucene.LuceneSearchService;

/**
 * The SuggestInputController can be used to suggest inputs from earlier request
 * within the same workflowGroup.
 * 
 * @author rsoika
 * @version 1.0
 */

@Named
@SessionScoped
public class SuggestImportController implements Serializable {

	private static final long serialVersionUID = 1L;
//	private String input = null;
	private List<ItemCollection> searchResult = null;

//	@Inject
//	WorkflowController workflowController;

	@EJB
	DocumentService documentService = null;

	public static Logger logger = Logger.getLogger(SuggestImportController.class.getName());

	public SuggestImportController() {
		super();

	}

	public List<ItemCollection> getSearchResult() {
		return searchResult;
	}

	/*
	 * 
	 * public String getInput() { return input; }
	 * 
	 * public void setInput(String input) { this.input = input; }
	 * 
	 *
	 */
	/**
	 * This method reset the search and input state.
	 */
	public void reset() {
		searchResult = new ArrayList<ItemCollection>();
		// input = "";
		logger.fine("reset");
	}

	/**
	 * This ajax event method reset the search and input state.
	 * 
	 * @param event
	 */
	public void reset(AjaxBehaviorEvent event) {
		reset();
	}

	/**
	 * This method updates the current workitem with the values defined by teh
	 * itemList from the given suggest workitem.
	 * 
	 * @param suggest - ItemColleciton with data to suggest
	 * @param itemList - item names to be updated. 
	 */
	public void update(ItemCollection workitem,ItemCollection suggest, String itemList) {
		
		logger.info("......update "+ itemList + "...");
		String[] itemNames = itemList.split("[\\s,;]+");
		for (String itemName: itemNames) {
			logger.info("......update item " + itemName);
			workitem.replaceItemValue(itemName, suggest.getItemValue(itemName));
			
			logger.info("......new value=" +suggest.getItemValue(itemName) );
		}
	}

	/**
	 * This method initializes a lucene search. The method is triggered by ajax
	 * events from the userInput.xhtml page. The minimum length of a given input
	 * search phrase have to be at least 3 characters
	 * 
	 */
	public void search(String input,String searchField, String workflowGroup) {
		logger.info("search input= " + input);

		if (input == null || input.length() < 3)
			return;
		logger.fine("search for=" + input);
		searchResult = searchEntity(searchField, workflowGroup, input);

	}

	/**
	 * This method returns a list of ItemCollections matching the search phrase and
	 * workflowgroup.
	 * <p>
	 * The type is restrcited to "workitem" and "workitemarchive"
	 * <p>
	 * The result is filtered for unique entries and is used to display the
	 * suggestion list
	 * 
	 * @param phrase - search phrase
	 * @return - list of matching requests
	 */
	public List<ItemCollection> searchEntity(String searchField, String workflowGroup, String phrase) {

		List<ItemCollection> searchResult = new ArrayList<ItemCollection>();

		if (phrase == null || phrase.isEmpty())
			return searchResult;

		Collection<ItemCollection> col = null;
		try {
			phrase = phrase.trim();
			// phrase = LuceneSearchService.escapeSearchTerm(phrase);
			phrase = LuceneSearchService.normalizeSearchTerm(phrase);
			String sQuery = "(type:\"workitem\" OR type:\"workitearchivem\") AND ($workflowgroup:\"" + workflowGroup
					+ "\") AND (" + searchField + ":" + phrase + "*)";

			logger.info("search: " + sQuery);

			// start lucene search

			logger.fine("searchWorkitems: " + sQuery);
			col = documentService.find(sQuery, 999, 0);
			logger.info("found: " + col.size());
		} catch (Exception e) {
			logger.warning("  lucene error - " + e.getMessage());
		}

		for (ItemCollection kreditor : col) {
			searchResult.add(kreditor);
		}
		// sort by txtname..
		Collections.sort(searchResult, new ItemCollectionComparator("txtname", true));

		return searchResult;

	}

}
