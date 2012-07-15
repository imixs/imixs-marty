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

package org.imixs.marty.web.util;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.event.ActionEvent;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;

/**
 * This Bean acts a a front controller for the WorkflowScheduler Service.
 * 
 * @author rsoika
 * 
 */
public class WorkflowSchedulerMB  {

	private ItemCollection configItemCollection = null;
	
	

	/* EJBs */
	@EJB
	org.imixs.marty.ejb.WorkflowSchedulerService workflowSchedulerService;
	@EJB
	EntityService entityService;
	@EJB
	WorkflowService workflowService;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	public WorkflowSchedulerMB() {
		super();
	}

	/**
	 * This method tries to load the config entity. If no Entity exists than the
	 * method creates a new entity
	 * 
	 * */
	@PostConstruct
	public void init() {
		
		doLoadConfiguration(null);
	}

	/**
	 * returns the configuration workitem.
	 * 
	 * @return
	 */
	public ItemCollection getConfiguration() {
		return this.configItemCollection;
	}


	/**
	 * save method tries to load the config entity. if not availabe. the method
	 * will create the entity the first time
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doSaveConfiguration(ActionEvent event) throws Exception {
		// save entity
		configItemCollection = workflowSchedulerService
				.saveConfiguration(configItemCollection);

	}

	/**
	 * This method reloads the configuration entity for the dms scheduler
	 * service
	 */
	public void doLoadConfiguration(ActionEvent event) {
		configItemCollection = workflowSchedulerService.findConfiguration();
	}

	/**
	 * starts the timer service
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doStartScheduler(ActionEvent event) throws Exception {
		configItemCollection.replaceItemValue("_enabled", true);
		configItemCollection = workflowSchedulerService
				.saveConfiguration(configItemCollection);
		configItemCollection = workflowSchedulerService.start();
	}

	public void doStopScheduler(ActionEvent event) throws Exception {
		configItemCollection.replaceItemValue("_enabled", false);
		configItemCollection = workflowSchedulerService
				.saveConfiguration(configItemCollection);
		configItemCollection = workflowSchedulerService.stop();
	}

	public void doRestartScheduler(ActionEvent event) throws Exception {
		doStopScheduler(event);
		doStartScheduler(event);
	}

	


	


	

	

}
