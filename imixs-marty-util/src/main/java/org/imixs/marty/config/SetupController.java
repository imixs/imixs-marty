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

package org.imixs.marty.config;

import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.model.ModelController;
import org.imixs.marty.model.ProcessController;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.util.PropertyService;

/**
 * This Marty SetupController extends the Marty ConfigController and holds the
 * data from the configuration entity 'BASIC'. This is the general configuration
 * entity.
 * 
 * NOTE: A configuration entity provides a common way to manage application
 * specific complex config data. The configuration entity is database controlled
 * and more flexible as the file based imixs.properties provided by the Imixs
 * Workflow Engine.
 * 
 * 
 * @author rsoika
 * 
 */
@Named("setupController")
@ApplicationScoped
public class SetupController extends ConfigController {

	private static final long serialVersionUID = 1L;

	public final static String CONFIGURATION_NAME = "BASIC";

	private boolean setupOk = false;

	@Resource(lookup="java:module/ModuleName")
	private String moduleName;
	    
	@Resource(lookup="java:app/AppName")
	private String appName;
	

	
	@Inject
	protected ModelController modelController;

	@Inject
	protected ProcessController processController;

	@EJB
	protected org.imixs.workflow.jee.ejb.EntityService entityService;

	@EJB
	protected org.imixs.workflow.jee.ejb.ModelService modelService;

	@EJB
	protected PropertyService propertyService;

	private static Logger logger = Logger.getLogger(SetupController.class
			.getName());

	public SetupController() {
		super();
		// set name
		this.setName(CONFIGURATION_NAME);
	}

	
	/**
	 * Returns the EAR application name. Useful for JNDI lookups
	 * @return
	 */
	public String getAppName() {
		return appName;
	}

	/**
	 * Returns the Web module name. Useful for JNDI lookups
	 * @return
	 */
	public String getModuleName() {
		return moduleName;
	}

	
	/**
	 * This method loads the config entity.
	 * 
	 * Next the method verifies if the system models are available and if the
	 * doSetup() method was triggered once before. The method set the boolean
	 * 'setupOk'. This Booelan indicates if the user need to start a doSetup()
	 * method call (see layout.xhtml)
	 */
	@PostConstruct
	@Override
	public void init() {

		super.init();

		// if the BASIC configuration was not yet saved before we neet to
		// initalize it with a
		// default setup
		if (!getWorkitem().hasItem(EntityService.UNIQUEID)) {
			Vector<String> v=new Vector<String>();
			v.add("IMIXS-WORKFLOW-Manager");
			v.add("IMIXS-WORKFLOW-Author");
			v.add("IMIXS-WORKFLOW-Reader");
			v.add("IMIXS-WORKFLOW-Editor");
			
			getWorkitem().replaceItemValue("usergroups", v);
			getWorkitem().replaceItemValue("keyenableuserdb",true);
			this.save();
			
		}

		// now test if system model exists and if the systemSetup was
		// successfully completed.
		setupOk = (modelController.hasSystemModel() && getWorkitem()
				.getItemValueBoolean("keySystemSetupCompleted") == true);

	}

	public boolean isSetupOk() {
		return setupOk;
	}

	public void setSetupOk(boolean systemSetupOk) {
		this.setupOk = systemSetupOk;
	}

	/**
	 * This method resets the propertyService and modelController
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doSetup(ActionEvent event) throws Exception {
		// reset propertyService
		logger.info("[SetupController] reset property service");
		propertyService.reset();

		// reset modelController
		modelController.reset();

		// reset processController
		processController.reset();
	
	}


	public PropertyService getPropertyService() {
		return propertyService;
	}

	
	
}
