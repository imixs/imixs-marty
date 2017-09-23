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
package org.imixs.marty.profile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.ejb.ProfileService;
import org.imixs.marty.workflow.WorkflowEvent;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.lucene.LuceneSearchService;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * The UserInputController provides suggest-box behavior based on the JSF 2.0
 * Ajax capability to add user names into a ItemValue of a WorkItem.
 * 
 * Usage:
 * 
 * <code>
       <marty:userInput value=
"#{workflowController.workitem.itemList['namteam']}"
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

	@Inject
	protected UserController userController;

	@EJB
	protected WorkflowService workflowService;

	private List<ItemCollection> searchResult = null;

	private String input = null;
	private static final long serialVersionUID = 1L;
	private int maxSearchCount = 30;
	private static Logger logger = Logger.getLogger(UserInputController.class.getName());

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

	/**
	 * Set the maximum length of a search result
	 * 
	 * @param maxSearchCount
	 */
	public void setMaxSearchCount(int maxSearchCount) {
		this.maxSearchCount = maxSearchCount;
	}

	/**
	 * This method reset the search and input state.
	 */
	public void reset() {
		searchResult = new ArrayList<ItemCollection>();
		input = "";
		logger.fine("userInputController reset");
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
	 * This method initializes a JPQL search. The method is triggered by ajax
	 * events from the userInput.xhtml page. The minimum length of a given input
	 * search phrase have to be at least 2 characters
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
	 * phrase. The search statement includes the items 'txtName', 'txtEmail' and
	 * 'txtUserName'. The result list is sorted by txtUserName
	 * 
	 * @param phrase
	 *            - search phrase
	 * @return - list of matching profiles
	 */
	public List<ItemCollection> searchProfile(String phrase) {

		List<ItemCollection> searchResult = new ArrayList<ItemCollection>();

		if (phrase == null || phrase.isEmpty())
			return searchResult;

		// start lucene search
		Collection<ItemCollection> col = null;

		try {
			phrase = phrase.trim();
			phrase = LuceneSearchService.escapeSearchTerm(phrase);
			// issue #170
			phrase = LuceneSearchService.normalizeSearchTerm(phrase);

			String sQuery = "(type:\"profile\") AND (" + phrase + "*)";

			logger.finest("searchprofile: " + sQuery);

			logger.fine("searchWorkitems: " + sQuery);
			col = workflowService.getDocumentService().find(sQuery, 0, -1);
		} catch (Exception e) {
			logger.warning("  lucene error - " + e.getMessage());
		}

		for (ItemCollection profile : col) {
			searchResult.add(ProfileService.cloneWorkitem(profile));
		}
		// sort by username..
		Collections.sort(searchResult, new ItemCollectionComparator("txtusername", true));

		return searchResult;

	}

	public List<ItemCollection> getSearchResult() {
		return searchResult;
	}

	/**
	 * This methods adds a new name to a userid list
	 * 
	 */
	public void add(String aName, List<Object> aList) {
		if (aList == null || aName == null || aName.isEmpty())
			return;
		// trim
		aName = aName.trim();
		
		if (!aList.contains(aName)) {
			logger.fine("userInputController add '" + aName + "' from list.");
			aList.add(aName);
		}

		// remove empty entries.....
		Iterator<Object> i = aList.iterator();
		while (i.hasNext()) {
			Object oentry = i.next();
			if (oentry == null || oentry.toString().isEmpty()) {
				i.remove();
			}
		}
	}

	/**
	 * This method tests if a given string is a defined Access Role. 
	 * @param aName
	 * @return true if the name is a access role
	 * @see DocumentService.getAccessRoles()
	 */
	public boolean isRole(String aName) {
		String accessRoles=workflowService.getDocumentService().getAccessRoles();
		String roleList = "org.imixs.ACCESSLEVEL.READERACCESS,org.imixs.ACCESSLEVEL.AUTHORACCESS,org.imixs.ACCESSLEVEL.EDITORACCESS,org.imixs.ACCESSLEVEL.MANAGERACCESS,"
				+ accessRoles;
		List<String> roles = Arrays.asList(roleList.split("\\s*,\\s*"));
		if (roles.stream().anyMatch(aName::equalsIgnoreCase)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * This methods removes a name from the userid list
	 */
	public void remove(String aName, List<Object> aList) {
		if (aList == null || aName == null)
			return;
		logger.fine("userInputController remove '" + aName + "' from list.");

		// in some cases the username can be stored in wrong upper/lower case.
		// This is the reason for a special double check
		if (aList.contains(aName)) {
			aList.remove(aName);
		} else {
			// here we try to find the entry ignoring upper/lower case ....
			for (Object aEntry : aList) {
				if (aEntry != null) {
					String aValue = aEntry.toString().toLowerCase();
					if (aValue.equals(aName.toLowerCase())) {
						logger.warning(
								"userInputController remove '" + aName + "' from list ignoring upper/lower case!");
						aList.remove(aEntry);
						break;
					}
				}
			}
		}
	}

	/**
	 * This method returns a sorted list of profiles for a given userId list
	 * 
	 * @param aNameList
	 *            - string list with user ids
	 * @return - list of profiles
	 */
	public List<ItemCollection> getSortedProfilelist(List<Object> aNameList) {
		List<ItemCollection> profiles = new ArrayList<ItemCollection>();

		if (aNameList == null)
			return profiles;

		// add all profile objects....
		for (Object aentry : aNameList) {
			if (aentry != null && !aentry.toString().isEmpty()) {
				ItemCollection profile = userController.getProfile(aentry.toString());
				if (profile != null) {
					profiles.add(profile);
				} else {
					// create a dummy entry
					profile = new ItemCollection();
					profile.replaceItemValue("txtName", aentry.toString());
					profile.replaceItemValue("txtUserName", aentry.toString());
					profiles.add(profile);
				}
			}
		}

		// sort by username..
		Collections.sort(profiles, new ItemCollectionComparator("txtUserName", true));

		return profiles;
	}

	/**
	 * WorkflowEvent listener to reset state
	 * 
	 * @param workflowEvent
	 * @throws AccessDeniedException
	 */
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent) throws AccessDeniedException {

		// reset state
		reset();

	}

}
