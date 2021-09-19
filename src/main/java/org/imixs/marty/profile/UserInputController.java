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
import java.util.Vector;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.index.SchemaService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.faces.data.WorkflowEvent;

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
//@SessionScoped
@RequestScoped
public class UserInputController implements Serializable {

	@Inject
	protected UserController userController;

	@EJB
	protected WorkflowService workflowService;
 
	@EJB
	protected SchemaService schemaService;
	
	@EJB
	protected ProfileService profileService;
	

	private List<ItemCollection> searchResult = null;

	
	private static final long serialVersionUID = 1L;
	
	private static Logger logger = Logger.getLogger(UserInputController.class.getName());

	public UserInputController() {
		super();
		searchResult = new ArrayList<ItemCollection>();
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

	    
	    logger.info("-----starte searchProfile - sollte nicht zu oft vorkommen");
		List<ItemCollection> searchResult = new ArrayList<ItemCollection>();

		if (phrase == null || phrase.isEmpty())
			return searchResult;

		// start lucene search
		Collection<ItemCollection> col = null;

		try {
			phrase = phrase.trim().toLowerCase();
			phrase = schemaService.escapeSearchTerm(phrase);
			// issue #170
			phrase = schemaService.normalizeSearchTerm(phrase);

			String sQuery = "(type:profile) AND ($processid:[210 TO 249]) AND  ((txtname:" + phrase
					+ "*) OR (txtusername:" + phrase + "*) OR (txtemail:" + phrase + "*))";

			logger.finest("searchprofile: " + sQuery);

			logger.fine("searchWorkitems: " + sQuery);
			col = workflowService.getDocumentService().find(sQuery, 0, 100);
		} catch (Exception e) {
			logger.warning("Lucene error error: " + e.getMessage());
		}

		if (col != null) {
			for (ItemCollection profile : col) {
				searchResult.add(profileService.cloneWorkitem(profile));
			}
			// sort by username..
			Collections.sort(searchResult, new ItemCollectionComparator("txtusername", true));
		}
		return searchResult;

	}

	public List<ItemCollection> getSearchResult() {
		return searchResult;
	}


	/**
	 * This method tests if a given string is a defined Access Role.
	 * 
	 * @param aName
	 * @return true if the name is a access role
	 * @see DocumentService.getAccessRoles()
	 */
	public boolean isRole(String aName) {
		String accessRoles = workflowService.getDocumentService().getAccessRoles();
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
	 * This method removes duplicates and null values from a vector.
	 * 
	 * @param valueList
	 *            - list of elements
	 */
	public List<?> uniqueList(List<Object> valueList) {
		int iVectorSize = valueList.size();
		Vector<Object> cleanedVector = new Vector<Object>();

		for (int i = 0; i < iVectorSize; i++) {
			Object o = valueList.get(i);
			if (o == null || cleanedVector.indexOf(o) > -1 || "".equals(o.toString()))
				continue;

			// add unique object
			cleanedVector.add(o);
		}
		valueList = cleanedVector;
		// do not work with empty vectors....
		if (valueList.size() == 0)
			valueList.add("");

		return valueList;
	}

	
	
	
	
    /**
     * This method searches a text phrase within the user profile objects (type=profile).
     * <p>
     * JSF Integration:
     * 
     * {@code 
     * 
     * <h:commandScript name="imixsOfficeWorkflow.mlSearch" action=
     * "#{cargosoftController.search()}" rendered="#{cargosoftController!=null}" render=
     * "cargosoft-results" /> }
     * 
     * <p>
     * JavaScript Example:
     * 
     * <pre>
     * {@code
     *  imixsOfficeWorkflow.cargosoftSearch({ item: '_invoicenumber' })
     *  }
     * </pre>
     * 
     */
    public void searchUser() {

        searchResult = new ArrayList<ItemCollection>();
        // get the param from faces context....
        FacesContext fc = FacesContext.getCurrentInstance();
        String phrase = fc.getExternalContext().getRequestParameterMap().get("phrase");
        if (phrase==null) {
            return;
        }
       
        logger.info("user search prase '" + phrase + "'");

     
       searchResult = searchProfile(phrase);
         
        if (searchResult!=null ) {
            logger.info("found " + searchResult.size() + " user profiles...");
        }
        
      

    }
	
    /**
     * Returns the username to a given userid
     * @param userid
     * @return
     */
    public String getUserName(String userid) {
        ItemCollection profile = userController.getProfile(userid);
        if (profile != null) {
            return profile.getItemValueString("txtusername");
        } else {
            // not found
           return userid;
        }
    }
	
	
	
}
