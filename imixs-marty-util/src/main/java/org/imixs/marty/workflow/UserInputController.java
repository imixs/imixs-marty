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
import javax.enterprise.context.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.profile.UserController;
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
		searchResult = userController.searchProfile(input);

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
