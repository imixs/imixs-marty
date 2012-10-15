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

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;

import org.imixs.marty.config.SetupController;
import org.imixs.marty.profile.UserController;
import org.imixs.marty.util.WorkitemHelper;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.faces.workitem.IViewAdapter;
import org.imixs.workflow.jee.faces.workitem.ViewController;

/**
 * The Marty WorklistController extends the Imixs WorklistController and
 * provides custom search results depending on the
 * org.imixs.marty.view.SearchController.
 * 
 * The Controller is ViewScoped and used as a ManagedBean instead of a CDI bean.
 * 
 * @author rsoika
 * @version 1.0.0
 */
@ManagedBean(name = "worklistController")
@ViewScoped
public class WorklistController extends
		org.imixs.workflow.jee.faces.workitem.WorklistController implements
		Serializable {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger("org.imixs.marty");

	private String viewTitle = null;

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

	@Inject
	private SearchController searchController = null;

	public WorklistController() {
		super();
	}

	public void setUserController(UserController userController) {
		this.userController = userController;
	}

	public void setSetupController(SetupController setupController) {
		this.setupController = setupController;
	}

	public void setSearchController(SearchController searchController) {
		this.searchController = searchController;
	}

	@Override
	public IViewAdapter getViewAdapter() {
		if (searchController != null)
			return searchController;
		else
			return super.getViewAdapter();
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

	@Override
	public void doReset(ActionEvent event) {
		if (searchController != null)
			searchController.reset(event);
		super.doReset(event);
	}

	/**
	 * test if a search phrase was entered
	 * 
	 * @return
	 */
	private boolean isSearchMode() {

		return false;

	}

}
