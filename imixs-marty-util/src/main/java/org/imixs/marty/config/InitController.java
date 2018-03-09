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
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.imixs.marty.ejb.SetupService;

/**
 * This Marty InitController verifies the init status of userDB and system
 * models and calls the SystemSetup if the system was not yet initialized
 * 
 * The setup mode can be controlled by imixs.property 'setup.mode' which is set
 * to 'auto' | 'model' | 'none'.
 * 
 * The bean can be placed in a jsf page to trigger the service setup.
 * 
 * 
 * @author rsoika
 * 
 */
@Named
@ApplicationScoped
public class InitController implements Serializable {

	@EJB
	SetupService setupService;

	private static final long serialVersionUID = 1L;
	private boolean initMode = false;
	private String initStatus = "";

	private static Logger logger = Logger.getLogger(InitController.class.getName());

	/**
	 * This method initializes the sytem by calling the SystemServcie EJB.
	 * 
	 * The setup mode can be set by the imixs.property 'setup.mode'
	 * 
	 * setup.mode = auto | model | none
	 */
	@PostConstruct
	public void init() {
		// avoid calling twice
		if (!initMode) {
			logger.info("calling System Setup...");
			initMode = true;
			initStatus = setupService.init();
		}
	}

	/**
	 * Returns the initStatus. The variable is set during the init() method. To
	 * trigger the system setup place the following line in your jsf page:
	 * 
	 * <code>
	 * 	<!-- SystemSetupStatus=#{initController.initStatus} -->
	 * </code>
	 * 
	 * @return
	 */
	public String getInitStatus() {
		return initStatus;
	}

}
