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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;

import org.imixs.marty.util.WorkitemHelper;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.plugins.jee.extended.LucenePlugin;

/**
 * The ReferenceController provides suggest-box behavior based on the JSF 2.0
 * ajax capability to add WorkItem references to the curren WorkItem.
 * 
 * All WorkItem references will be stored in the property 'txtworkitemref' Note: @RequestScoped
 * did not work because the ajax request will reset the result during submit
 * 
 * 
 * 
 * @author rsoika
 * @version 1.0
 */

@Named("linkController")
@SessionScoped
public class LinkController implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String LINK_PROPERTY = "txtworkitemref";

	private static Logger logger = Logger.getLogger("org.imixs.marty");

	@Inject
	private WorkflowController workflowController;

	@EJB
	private WorkflowService workflowService;

	private List<ItemCollection> searchResult = null;
	private List<ItemCollection> externalReferences = null;
	private Map<String, List<ItemCollection>> references = null;

	private String input = null;

	public LinkController() {
		super();
		searchResult = new ArrayList<ItemCollection>();
	}

	public void setWorkflowController(WorkflowController workflowController) {
		this.workflowController = workflowController;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public void reset(AjaxBehaviorEvent event) {
		searchResult = new ArrayList<ItemCollection>();
		input="";
	}

	/*
	 * Starts a lucene search to provide searchResult for suggest list
	 */
	public void search(String filter) {

		if (input == null)
			return;

		logger.info("LinkController SearchOption=" + filter);
		searchResult = new ArrayList<ItemCollection>();

		try {
			String sSearchTerm = "";
			if (filter != null && !"".equals(filter)) {
				String sNewFilter = filter;
				sNewFilter = sNewFilter.replace(".", "?");
				sSearchTerm = "(" + sNewFilter + ") AND ";
			}
			if (!"".equals(input)) {
				sSearchTerm += " (*" + input.toLowerCase() + "*)";
			}

			searchResult = LucenePlugin.search(sSearchTerm, workflowService);
			// clone result list
			for (int i = 0; i < searchResult.size(); i++) {
				searchResult.set(i, WorkitemHelper.clone(searchResult.get(i)));
			}

		} catch (Exception e) {
			logger.warning("  lucene error!");
			e.printStackTrace();
		}

	}

	public List<ItemCollection> getSearchResult() {
		return searchResult;
	}

	/**
	 * This methods adds a new workItem reference
	
	 */
	public void add(String aUniqueID, ItemCollection workitem) {

		logger.fine("LinkController add workitem reference: " + aUniqueID);

		@SuppressWarnings("unchecked")
		List<String> refList = workitem.getItemValue(LINK_PROPERTY);

		// clear empty entry if set
		if (refList.size() == 1 && "".equals(refList.get(0)))
			refList.remove(0);

		// test if not yet a member of
		if (refList.indexOf(aUniqueID) == -1) {
			refList.add(aUniqueID);
			workitem.replaceItemValue(LINK_PROPERTY, refList);
		}

		// reset
		reset(null);
		references = null;
	}
	

	/**
	 * This methods removes a workItem reference
	
	 */
	public void remove(String aUniqueID, ItemCollection workitem) {

		logger.fine("LinkController remove workitem reference: " + aUniqueID);

		@SuppressWarnings("unchecked")
		List<String> refList = workitem.getItemValue(LINK_PROPERTY);

		
		// test if a member of
		if (refList.indexOf(aUniqueID) > -1) {
			refList.remove(aUniqueID);			
			workitem.replaceItemValue(LINK_PROPERTY, refList);
		}
		// reset
		reset(null);
		references = null;
	}


	/**
	 * This method returns a list of ItemCollections refered by the current
	 * workItem (txtWorkitemRef).
	 * 
	 * The filter will be applied to the result list. So each WorkItem will be
	 * tested if it matches the filter expression. The results of this method
	 * are cached into the references map. This cache is discarded if the
	 * current workItem changed.
	 * 
	 * @return - list of ItemCollection with matches the current filter
	 */
	public List<ItemCollection> getReferences(String filter) {
		List<ItemCollection> filterResult = null;

		if (references == null)
			references = new HashMap<String, List<ItemCollection>>();

		// lazy loading references by filter?
		filterResult = references.get(filter);
		if (filterResult == null) {
			// build a new workitem list for that filter....
			filterResult = new ArrayList<ItemCollection>();

			logger.fine("lookup references for: " + filter);

			long lTime = System.currentTimeMillis();
			// lookup the references...
			List<String> list = workflowController.getWorkitem().getItemValue(
					LINK_PROPERTY);
			// empty list?
			if (list.size() == 0
					|| (list.size() == 1 && "".equals(list.get(0)))) {
				references.put(filter, filterResult);
				return filterResult;
			}

			// start query and filter the result
			String sQuery = "SELECT entity FROM Entity entity "
					+ " WHERE entity.type = 'workitem' AND entity.id IN (";
			for (String aID : list) {
				sQuery += "'" + aID + "',";
			}
			// cut last ,
			sQuery = sQuery.substring(0, sQuery.length() - 1);
			sQuery += ")";

			Collection<ItemCollection> workitems = workflowService
					.getEntityService().findAllEntities(sQuery, 0, -1);

			logger.fine("  WorkitemRef Lookup:  "
					+ (System.currentTimeMillis() - lTime) + " ms");

			if (workitems.size() == 0) {
				references.put(filter, filterResult);
				return filterResult;
			}

			// now test if filter matches, and clone the workItem
			for (ItemCollection itemcol : workitems) {
				// test
				if (WorkitemHelper.matches(itemcol, filter)) {
					filterResult.add(WorkitemHelper.clone(itemcol));
				}
			}

			references.put(filter, filterResult);
		}
		return filterResult;
	}

	/**
	 * returns a list of all workItems holding a reference to the current
	 * workItem. If the filter is set the processID will be tested for the
	 * filter regex
	 * 
	 * 
	 * @return
	 * @throws NamingException
	 */
	public List<ItemCollection> getExternalReferences() {
		if (externalReferences != null)
			return externalReferences;

		externalReferences = new ArrayList<ItemCollection>();

		long lTime = System.currentTimeMillis();

		String uniqueid = workflowController.getWorkitem().getItemValueString(
				"$uniqueid");

		// return an empty list if still no $uniqueid is defined for the current
		// workitem
		if ("".equals(uniqueid))
			return externalReferences;

		// select all references.....
		String sQuery = "SELECT workitem FROM Entity AS workitem"
				+ " JOIN workitem.textItems AS rnr"
				+ " WHERE workitem.type = 'workitem' "
				+ " AND rnr.itemName = '" + LINK_PROPERTY + "'"
				+ " AND rnr.itemValue='" + uniqueid + "'"
				+ " ORDER BY workitem.created DESC";

		logger.fine("  Incomming Referece Lookup - query:  " + sQuery);
		Collection<ItemCollection> col = null;
		try {
			col = workflowService.getEntityService().findAllEntities(sQuery, 0,
					-1);
			for (ItemCollection itemcol : col) {

				externalReferences.add(itemcol);
			}
		} catch (Exception e) {

			e.printStackTrace();
		}

		logger.fine("  External Referece Lookup:  "
				+ (System.currentTimeMillis() - lTime) + " ms");

		return externalReferences;

	}

	/**
	 * WorkflowEvent listener
	 * 
	 * @param workflowEvent
	 * @throws AccessDeniedException
	 */
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent)
			throws AccessDeniedException {
		if (workflowEvent == null)
			return;

		// skip if not a workItem...
		if (workflowEvent.getWorkitem() != null
				&& !workflowEvent.getWorkitem().getItemValueString("type")
						.startsWith("workitem"))
			return;

		if (WorkflowEvent.WORKITEM_CHANGED == workflowEvent.getEventType()) {
			externalReferences = null;
			references = null;
		}

		if (WorkflowEvent.WORKITEM_AFTER_PROCESS == workflowEvent
				.getEventType()) {
			externalReferences = null;
			references = null;

		}

	}
}
