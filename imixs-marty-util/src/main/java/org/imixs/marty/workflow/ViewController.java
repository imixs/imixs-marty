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

package org.imixs.marty.workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.config.SetupController;
import org.imixs.marty.profile.UserController;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.faces.workitem.DataController;

/**
 * The ViewController is a generic implementation for all types of views for
 * workitems. A view can be filtered by WorkflowGroup (String), by ProcessID
 * (Integer) and by Project (String). Alternatively the viewController provides
 * a custom searchFilter.
 * 
 * @author rsoika
 * 
 */
@Named("viewController")
@SessionScoped
public class ViewController extends org.imixs.workflow.jee.faces.workitem.WorkflowController implements Serializable {

	private static final long serialVersionUID = 1L;

	
	private String viewTitle = "undefined viewtitle";


	
	@Inject
	UserController userController = null;

	@Inject
	private SetupController setupController = null;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	public ViewController() {
		super();
		// empty constructor
	}

	/**
	 * This method initializes the view type an view settings like sort order
	 * and max entries per page. These properties can be set through the
	 * BeanProperties in the faces-config.xml or controlled by the config
	 * entity.
	 */
	@PostConstruct
	public void init() {

		
		
		
	}

	public SetupController getSetupsetupController() {
		return setupController;
	}

	public void setSetupsetupController(SetupController setupMB) {
		this.setupController = setupMB;
	}

	public UserController getUserController() {
		return userController;
	}

	public void setUserController(UserController myProfileMB) {
		this.userController = myProfileMB;
	}

	


	public String getViewTitle() {
		return viewTitle;
	}

	public void setViewTitle(String viewTitle) {
		
		Locale locale = new Locale(userController.getLocale());
		ResourceBundle rb = null;
		rb = ResourceBundle.getBundle("bundle.workitem", locale);

		this.viewTitle = rb.getString(viewTitle);

	}

	
	
	

}
