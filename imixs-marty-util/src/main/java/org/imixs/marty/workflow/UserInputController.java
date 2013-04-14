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

import org.imixs.marty.ejb.ProfileService;
import org.imixs.marty.profile.UserController;
import org.imixs.marty.util.WorkitemComparator;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.WorkflowService;

/**
 * The UserInputController provides suggest-box behavior based on the JSF 2.0
 * Ajax capability to add user names into a ItemValue of a WorkItem.
 * 
 * Usage:
 * 
 * <code>
       <marty:userInput value="#{workflowController.workitem.itemList['namteam']}"
		     editmode="true" />
 * </code>
 * 
 * 
 * @author rsoika
 * @version 1.0
 */

@Named("userInputController")
@SessionScoped
public class UserInputController implements Serializable {

	private static final long serialVersionUID = 1L;
	private int maxSearchCount=30;
	private static Logger logger = Logger.getLogger("org.imixs.marty");

	@Inject
	private WorkflowController workflowController;

	@Inject
	private UserController userController;

	@EJB
	private WorkflowService workflowService;

	private List<ItemCollection> searchResult = null;

	private String input = null;

	public UserInputController() {
		super();
		searchResult = new ArrayList<ItemCollection>();
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public int getMaxSearchCount() {
		return maxSearchCount;
	}

	public void setMaxSearchCount(int maxSearchCount) {
		this.maxSearchCount = maxSearchCount;
	}

	public void reset(AjaxBehaviorEvent event) {
		searchResult = new ArrayList<ItemCollection>();
		input = "";
		logger.fine("userInputController reset");
	}

	/**
	 * JPQL search . minimum search input 2 chars!
	 * 
	 */
	public void search() {
		if (input == null || input.length() < 2)
			return;
		logger.fine("userInputController search for=" + input);
		searchResult = searchProfile(input);

	}
	
	/**
	 * This method returns a list of profile ItemCollections matching the search
	 * phrase. The JQPL joins over txtEmail and txtUserName
	 * 
	 * @param aname
	 * @return
	 */
	public List<ItemCollection> searchProfile(String phrase) {

		List<ItemCollection> searchResult = new ArrayList<ItemCollection>();

		if (phrase == null || phrase.isEmpty())
			return searchResult;

		phrase = "%" + phrase.trim() + "%";

		String sQuery = "SELECT DISTINCT profile FROM Entity as profile "
				+ " JOIN profile.textItems AS t1"
				+ " JOIN profile.textItems AS t2"
				+ " WHERE  profile.type= 'profile' " + " AND "
				+ " ( (t1.itemName = 'txtusername' "
				+ " AND t1.itemValue LIKE  '" + phrase + "') "
				+ " OR (t2.itemName = 'txtemail' "
				+ " AND t2.itemValue LIKE  '" + phrase + "') " + " )";

		logger.finest("searchprofile: " + sQuery);

		Collection<ItemCollection> col = workflowService.getEntityService().findAllEntities(sQuery,
				0, maxSearchCount);

		for (ItemCollection profile : col) {
			searchResult.add(ProfileService.cloneWorkitem(profile));

		}

		// sort by username..
		Collections.sort(searchResult, new WorkitemComparator(
				"txtWorkflowGroup", true));

		return searchResult;

	}

	public List<ItemCollection> getSearchResult() {
		return searchResult;
	}

	/**
	 * This methods adds a new workItem reference
	 */
	public void add(String aName, List<Object> aList) {
		logger.fine("userInputController add: " + aName);
		aList.add(aName);
	}

	/**
	 * This methods removes a workItem reference
	 */
	public void remove(String aName, List<Object> aList) {
		logger.fine("userInputController remove: " + aName);
		aList.remove(aName);
	}

}
