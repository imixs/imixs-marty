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
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.plugins.jee.extended.LucenePlugin;

/**
 * The SuggestController provides suggest-box behavior based on the JSF 2.0 ajax
 * capability.
 * 
 * 
 * 
 * @author rsoika
 * @version 1.0
 */

@Named("suggestController")
@RequestScoped
public class SuggestController implements Serializable {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger("org.imixs.office");

	@EJB
	private WorkflowService workflowService;
	@Inject
	private WorkflowController workflowController;

	private List<ItemCollection> result = null;
	private String query = null;
	private String input = null;

	public SuggestController() {
		super();

		// result = new ArrayList<ItemCollection>();
		result = new ArrayList<ItemCollection>();
	}

	public void setWorkflowController(WorkflowController workflowController) {
		this.workflowController = workflowController;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public void reset(AjaxBehaviorEvent event) {
		logger.info("reset result");
		result = new ArrayList<ItemCollection>();
	}

	public void search(String query) {

		logger.info("SearchOption=" + query);
		result = new ArrayList<ItemCollection>();

		try {

			String sSearchTerm = "";

			if (query != null && !"".equals(query)) {
				String sNewFilter = query;

				sNewFilter = sNewFilter.replace(".", "?");
				sSearchTerm = "(" + sNewFilter + ") AND ";

			}
			if (!"".equals(input)) {
				sSearchTerm += " (*" + input.toLowerCase() + "*)";

			}

			System.out.println("Suggest: " + sSearchTerm);
			result = LucenePlugin.search(sSearchTerm, workflowService);

		} catch (Exception e) {
			logger.warning("  lucene error!");
			e.printStackTrace();
		}

	}

	public List<ItemCollection> getResult() {
		return result;

	}

}
