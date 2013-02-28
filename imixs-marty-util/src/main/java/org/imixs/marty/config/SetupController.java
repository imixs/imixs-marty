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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.model.ModelController;
import org.imixs.workflow.jee.jpa.EntityIndex;
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
 * The SetupController extends the ConfigController and provides additional methods to 
 * setup and initialize a marty application. 
 * The method doSetup() creates and updates index fields. The method loadDefaultModels checks if
 * default (file based) models need to be initialized. 
 * 
 *
 * 
 * @author rsoika
 * 
 */
@Named("setupController")
@ApplicationScoped
public class SetupController extends ConfigController {

	private static final long serialVersionUID = 1L;

	private final static String CONFIGURATION_NAME = "BASIC";

	private boolean setupOk = false;

	@Inject
	private ModelController modelController;

	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	@EJB
	org.imixs.workflow.jee.ejb.ModelService modelService;

	@EJB
	PropertyService propertyService;
	
	private static Logger logger = Logger.getLogger(SetupController.class.getName());

	public SetupController() {
		super();
		// set name
		this.setName(CONFIGURATION_NAME);
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
	 * This method starts a ConsistencyCheck without updating values.
	 * 
	 * Also the modelController (application scoped) will be reinitialized.
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doSetup(ActionEvent event) throws Exception {

		logger.info("[SetupController starting System Setup...");
		// model
		entityService.addIndex("numprocessid", EntityIndex.TYP_INT);
		entityService.addIndex("numactivityid", EntityIndex.TYP_INT);

		// workflow
		entityService.addIndex("type", EntityIndex.TYP_TEXT);
		entityService.addIndex("$uniqueidref", EntityIndex.TYP_TEXT);
		entityService.addIndex("$workitemid", EntityIndex.TYP_TEXT);
		entityService.addIndex("$processid", EntityIndex.TYP_INT);
		entityService.addIndex("txtworkflowgroup", EntityIndex.TYP_TEXT);
		entityService.addIndex("namcreator", EntityIndex.TYP_TEXT);
		entityService.addIndex("$modelversion", EntityIndex.TYP_TEXT);

		// app
		entityService.addIndex("txtworkitemref", EntityIndex.TYP_TEXT);
		entityService.addIndex("txtname", EntityIndex.TYP_TEXT);
		entityService.addIndex("txtemail", EntityIndex.TYP_TEXT);
		entityService.addIndex("namteam", EntityIndex.TYP_TEXT);
		entityService.addIndex("namowner", EntityIndex.TYP_TEXT);
		entityService.addIndex("datdate", EntityIndex.TYP_CALENDAR);
		entityService.addIndex("datfrom", EntityIndex.TYP_CALENDAR);
		entityService.addIndex("datto", EntityIndex.TYP_CALENDAR);
		entityService.addIndex("numsequencenumber", EntityIndex.TYP_INT);

		entityService.addIndex("txtProjectName", EntityIndex.TYP_TEXT);
		entityService.addIndex("txtUsername", EntityIndex.TYP_TEXT);

		logger.info("[SetupController] index configuration - ok");

		// update System configuration.....
		getWorkitem().replaceItemValue("keySystemSetupCompleted", true);
		save();
		
		// reset propertyService
		logger.info("[SetupController] reset property service");
		propertyService.reset();

		// initialize modelController
		modelController.init();

		// load default models
		loadDefaultModels();

		setupOk = true;

		logger.info("[SetupController] system setup completed");

	}

	/**
	 * This method loads the default model files defined by the configuration
	 * file: /configuration/model.properties
	 * 
	 * The method returns without any action if a system model still exists.
	 * 
	 * 
	 * @param aSkin
	 * @return
	 */
	public void loadDefaultModels() {

		logger.info("[SetupController] searching system model...");
		try {
			if (modelController.hasSystemModel()) {
				logger.info("[SetupController] system model found - skip loading default models");
				return;
			}

			logger.info("[SetupController] loading default model configuration from 'configuration/model.properties'...");

			ResourceBundle r = ResourceBundle.getBundle("configuration.model");
			if (r == null)
				logger.warning("[SetupController] configuration/model.properties - file found");
			Enumeration<String> enkeys = r.getKeys();
			while (enkeys.hasMoreElements()) {
				String sKey = enkeys.nextElement();
				// try to load this model
				String filePath = r.getString(sKey);
				logger.info("[SetupController] loading model configuration '"
						+ sKey + "=" + filePath + "'");

				InputStream inputStream = SetupController.class
						.getClassLoader().getResourceAsStream(filePath);
				// byte[] bytes = IOUtils.toByteArray(inputStream);

				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				int next;

				next = inputStream.read();

				while (next > -1) {
					bos.write(next);
					next = inputStream.read();
				}
				bos.flush();
				byte[] result = bos.toByteArray();

				modelController.importXmlEntityData(result);
			}
		} catch (Exception e) {
			logger.severe("[SetupController] unable to load model configuration - please check configuration/model.properties file!");
			throw new RuntimeException(
					"[SetupController] unable to load model configuration - please check configuration/model.properties file!");
		}

	}

}
