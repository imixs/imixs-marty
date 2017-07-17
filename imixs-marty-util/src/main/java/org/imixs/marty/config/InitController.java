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

import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.imixs.marty.ejb.SetupService;
import org.imixs.marty.ejb.security.UserGroupService;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * This Marty InitController verifies the init status of userDB and system
 * models and calls the method initDatabase if the system was not yet
 * initialized and the imixs.property param 'setup.mode' is set to 'auto'.
 * 
 * The bean is triggered in the index.xhtml page. This guarantees that the
 * database is initialized when the application is triggered the first time,
 * after successful deployment.
 * 
 * 
 * @author rsoika
 * 
 */
@Named
@ApplicationScoped
public class InitController implements Serializable {

	private static final long serialVersionUID = 1L;

	@EJB
	private UserGroupService userGroupService;

	@EJB
	private SetupService setupService;

	private boolean initMode = false;
	private String initStatus = "";

	private static Logger logger = Logger.getLogger(InitController.class.getName());

	public InitController() {
		super();
	}

	/**
	 * This method initializes the userDB and system models.
	 */
	@PostConstruct
	public void init() {

		// init system database...
		Properties properties = loadProperties();
		if (!initMode && properties.containsKey("setup.mode") && "auto".equals(properties.getProperty("setup.mode"))) {
			logger.info("setup.mode=auto -> starting system setup...");
			// avoid calling twice
			initMode = true;

		
			// PHASE-1: init userIDs for user db
			try {
				if (userGroupService != null) {
					logger.info("running userGroupService.initUserIDs...");
					userGroupService.initUserIDs();
				} else {
					logger.warning("userGroupService not initialized!");
				}
			} catch (Exception e) {
				logger.warning("Error during initUserIds: " + e.getMessage());
			}

			
			// PHASE-2: init system indizies and load default models
			try {
				logger.info("running systemService.init...");
				setupService.init();
			} catch (AccessDeniedException e1) {
				logger.severe("Error during init setupService: " + e1.getMessage());
				e1.printStackTrace();
			}

		
			initMode = false;
			initStatus = "OK";
		}
	}

	/**
	 * Returns the initStatus. The variable is set during the init() method.
	 * 
	 * @return
	 */
	public String getInitStatus() {
		return initStatus;
	}

	/**
	 * Helper method which loads a imixs.property file
	 * 
	 * (located at current threads classpath)
	 * 
	 */
	private Properties loadProperties() {
		Properties properties = new Properties();
		try {
			properties
					.load(Thread.currentThread().getContextClassLoader().getResource("imixs.properties").openStream());
		} catch (Exception e) {
			logger.warning("PropertyService unable to find imixs.properties in current classpath");
			if (logger.isLoggable(Level.FINE)) {
				e.printStackTrace();
			}
		}
		return properties;
	}

}
