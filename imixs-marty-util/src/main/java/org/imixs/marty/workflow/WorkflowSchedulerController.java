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
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowSchedulerService;
import org.imixs.workflow.engine.WorkflowService;

/**
 * This Bean acts a a front controller for the WorkflowScheduler Service.
 * 
 * @author rsoika
 * 
 */
@Named("workflowSchedulerController")
@RequestScoped
public class WorkflowSchedulerController implements Serializable {

	private static final long serialVersionUID = 1L;

	private ItemCollection configItemCollection = null;

	/* EJBs */
	@EJB
	WorkflowSchedulerService workflowSchedulerService;

	@EJB
	WorkflowService workflowService;

	private static Logger logger = Logger.getLogger(WorkflowSchedulerController.class.getName());

	public WorkflowSchedulerController() {
		super();
	}

	/**
	 * This method tries to load the config entity. If no Entity exists than the
	 * method creates a new entity
	 * 
	 * */
	@PostConstruct
	public void init() {

		configItemCollection = workflowSchedulerService.loadConfiguration();
		
	}

	public void refresh() {
		configItemCollection = workflowSchedulerService.loadConfiguration();

	}

	/**
	 * 
	 * converts time (in milliseconds) to human-readable format "<dd:>hh:mm:ss"
	 * 
	 * @return
	 */
	public String millisToShortDHMS(int duration) {

		String res = "";
		long days = TimeUnit.MILLISECONDS.toDays(duration);
		long hours = TimeUnit.MILLISECONDS.toHours(duration)
				- TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(duration));
		long minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
				- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS
						.toHours(duration));
		long seconds = TimeUnit.MILLISECONDS.toSeconds(duration)
				- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
						.toMinutes(duration));
		if (days == 0) {
			res = String.format("%d hours, %d minutes, %d seconds", hours,
					minutes, seconds);
		} else {
			res = String.format("%d days, %d hours, %d minutes, %d seconds",
					days, hours, minutes, seconds);
		}
		return res;

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
	public void doSaveConfiguration(ActionEvent event) {
		// save entity
		try {
			configItemCollection = workflowSchedulerService
					.saveConfiguration(configItemCollection);
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(
					null,
					new FacesMessage(FacesMessage.SEVERITY_INFO,
							e.getMessage(), null));
			e.printStackTrace();

		}

	}

	/**
	 * starts the timer service
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doStartScheduler(ActionEvent event) {
		configItemCollection.replaceItemValue("_enabled", true);
		try {
			configItemCollection = workflowSchedulerService
					.saveConfiguration(configItemCollection);
			configItemCollection = workflowSchedulerService.start();
		} catch (Exception e) {
			String message = "";

			if (e.getCause() != null)
				message = e.getCause().getMessage();
			else
				message = e.getMessage();

			FacesContext.getCurrentInstance()
					.addMessage(
							null,
							new FacesMessage(FacesMessage.SEVERITY_INFO,
									message, null));
			e.printStackTrace();

		}

	}

	public void doStopScheduler(ActionEvent event) throws Exception {
		configItemCollection.replaceItemValue("_enabled", false);
		configItemCollection = workflowSchedulerService
				.saveConfiguration(configItemCollection);
		configItemCollection = workflowSchedulerService.stop();
	}

	public void doRestartScheduler(ActionEvent event) throws Exception {
		logger.fine("[WorkflowSchedulerCOntroller] restart timer service");
		doStopScheduler(event);
		doStartScheduler(event);
	}

}
