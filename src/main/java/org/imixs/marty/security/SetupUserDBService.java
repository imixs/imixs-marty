/*******************************************************************************
 *  Imixs IX Workflow Technology
 *  Copyright (C) 2001, 2008 Imixs Software Solutions GmbH,  
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
 *******************************************************************************/
package org.imixs.marty.security;

import java.util.logging.Logger;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.engine.ModelService;
import org.imixs.workflow.engine.SetupEvent;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * The SetupService EJB initializes the system settings by its method 'init()'.
 * 
 * The setup mode can be controlled by imixs.property 'setup.mode' which is set
 * to 'auto' | 'none'.
 * 
 * 
 * @author rsoika
 * 
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Path("/setup")
@Singleton
public class SetupUserDBService {

    public static String USERDB_OK = "USERDB_OK";
    public static String USERDB_DISABLED = "USERDB_DISABLED";

    @EJB
    private UserGroupService userGroupService;

    @EJB
    private ModelService modelService;

    @Inject
    @ConfigProperty(name = "security.setup.mode", defaultValue = "auto")
    String setupMode;

    private static Logger logger = Logger.getLogger(SetupUserDBService.class.getName());

    /**
     * This method start the system setup during deployment
     * 
     * @throws AccessDeniedException
     */
    public void onSetupEvent(@Observes SetupEvent setupEvent) throws AccessDeniedException {
        init();
    }

    /**
     * This method performs the system setup. After the setup is completed the CDI
     * event 'SetupEvent' will be fired. An observer of this CDI event can extend
     * the setup process.
     * 
     * @return
     */
    @GET
    public String init() {
        String result = "";
        logger.info("...starting UserDB Setup...");

        // read setup mode...
        logger.info("...setup.mode = " + setupMode);

        // init userIDs for user db?
        if ("auto".equalsIgnoreCase(setupMode)) {
            try {
                if (userGroupService != null) {
                    userGroupService.initUserIDs();
                    logger.info("...UserDB OK");
                    result = USERDB_OK;
                } else {
                    logger.warning("userGroupService not initialized!");
                }
            } catch (Exception e) {
                logger.warning("Error during initializing UserDB: " + e.getMessage());
            }

        } else {
            result = USERDB_DISABLED;
            logger.finest("......UserDB is disabled.");
        }

        logger.info("...SystemSetup: " + result);
        return result;

    }

}