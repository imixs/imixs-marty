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
package org.imixs.marty.ejb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.imixs.marty.ejb.security.UserGroupService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.ModelService;
import org.imixs.workflow.engine.PropertyService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.xml.DocumentCollection;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

/**
 * The SetupService EJB initializes the system settings by its method 'init()'.
 * 
 * The setup mode can be controlled by imixs.property 'setup.mode' which is set
 * to 'auto' | 'model' | 'none'.
 * 
 * 
 * @author rsoika
 * 
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
@LocalBean
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Path("/setup")
public class SetupService {

	@EJB
	DocumentService documentService;

	@EJB
	ModelService modelService;

	@EJB
	PropertyService propertyService;

	@EJB
	private UserGroupService userGroupService;

	private static Logger logger = Logger.getLogger(SetupService.class.getName());

	/**
	 * This method verifies the default configuration and initiates a import of
	 * default data
	 * 
	 * @throws AccessDeniedException
	 */
	@GET
	public String init() {
		logger.info("starting System Setup...");

		// read setup mode...
		Properties properties = loadProperties();
		String setupMode = properties.getProperty("setup.mode", "auto");
		logger.info("setup.mode = " + setupMode);

		// init userIDs for user db?
		if ("auto".equalsIgnoreCase(setupMode)) {
			try {
				if (userGroupService != null) {
					userGroupService.initUserIDs();
					logger.info("... UserDB OK");
				} else {
					logger.warning("userGroupService not initialized!");
				}
			} catch (Exception e) {
				logger.warning("Error during initializing UserDB: " + e.getMessage());
			}

		} else {
			logger.finest("......UserDB is disabled.");
		}

		// load default models...?
		if ("auto".equalsIgnoreCase(setupMode) || "model".equalsIgnoreCase(setupMode)) {
			try {

				loadDefaultModels();
			} catch (AccessDeniedException e1) {
				logger.severe("Error during init setupService: " + e1.getMessage());
				e1.printStackTrace();
			}
		}

		logger.info("System Setup OK");
		return "OK";

	}

	/**
	 * this method imports an xml entity data stream. This is used to provide model
	 * uploads during the system setup. The method can also import general entity
	 * data like configuration data.
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void importXmlEntityData(byte[] filestream) {
		XMLItemCollection entity;
		ItemCollection itemCollection;
		String sModelVersion = null;

		if (filestream == null)
			return;
		try {

			DocumentCollection ecol = null;
			logger.fine("importXmlEntityData - importModel, verifing file content....");

			JAXBContext context;
			Object jaxbObject = null;
			// unmarshall the model file
			ByteArrayInputStream input = new ByteArrayInputStream(filestream);
			try {
				context = JAXBContext.newInstance(DocumentCollection.class);
				Unmarshaller m = context.createUnmarshaller();
				jaxbObject = m.unmarshal(input);
			} catch (JAXBException e) {
				throw new ModelException(ModelException.INVALID_MODEL,
						"error - wrong xml file format - unable to import model file: ", e);
			}
			if (jaxbObject == null)
				throw new ModelException(ModelException.INVALID_MODEL,
						"error - wrong xml file format - unable to import model file!");

			ecol = (DocumentCollection) jaxbObject;
			// import the model entities....
			if (ecol.getDocument().length > 0) {

				Vector<String> vModelVersions = new Vector<String>();
				// first iterrate over all enttity and find if model entries are
				// included
				for (XMLItemCollection aentity : ecol.getDocument()) {
					itemCollection = XMLItemCollectionAdapter.getItemCollection(aentity);
					// test if this is a model entry
					// (type=WorkflowEnvironmentEntity)
					if ("WorkflowEnvironmentEntity".equals(itemCollection.getItemValueString("type"))
							&& "environment.profile".equals(itemCollection.getItemValueString("txtName"))) {

						sModelVersion = itemCollection.getItemValueString("$ModelVersion");
						if (vModelVersions.indexOf(sModelVersion) == -1)
							vModelVersions.add(sModelVersion);
					}
				}
				// now remove old model entries....
				for (String aModelVersion : vModelVersions) {
					logger.fine("importXmlEntityData - removing existing configuration for model version '"
							+ aModelVersion + "'");
					modelService.removeModel(aModelVersion);
				}
				// save new entities into database and update modelversion.....
				for (int i = 0; i < ecol.getDocument().length; i++) {
					entity = ecol.getDocument()[i];
					itemCollection = XMLItemCollectionAdapter.getItemCollection(entity);
					// save entity
					documentService.save(itemCollection);
				}

				logger.fine("importXmlEntityData - " + ecol.getDocument().length + " entries sucessfull imported");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * This method loads the default model files defined by the configuration file:
	 * /configuration/model.properties
	 * 
	 * The method returns without any action if a system model still exists.
	 * 
	 * 
	 * @param aSkin
	 * @return
	 */
	private void loadDefaultModels() {
		logger.finest("......load default model...");
		try {
			List<String> colModelVersions = modelService.getVersions();

			if (!colModelVersions.isEmpty()) {
				logger.info("models available: ");
				for (String amodel : colModelVersions) {
					logger.warning("................ '" + amodel + "'");
				}
				return;
			}

			logger.info("uploading default models...");
			String sDefaultModelList = propertyService.getProperties().getProperty("setup.defaultModel");
			if (sDefaultModelList == null || sDefaultModelList.isEmpty()) {
				logger.warning("setup.defaultModel key is not defined in 'imixs.properties' - no default model found");
				return;
			}

			logger.fine("......setup.defaultModel=" + sDefaultModelList);

			StringTokenizer stModelList = new StringTokenizer(sDefaultModelList, ",", false);

			while (stModelList.hasMoreElements()) {
				// try to load this model
				String filePath = stModelList.nextToken();
				// test if bpmn model?
				if (filePath.endsWith(".bpmn") || filePath.endsWith(".xml")) {
					logger.fine("...uploading default model file: '" + filePath + "'....");
					InputStream inputStream = SetupService.class.getClassLoader().getResourceAsStream(filePath);
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

					// is BPMN?
					if (filePath.endsWith(".bpmn")) {
						BPMNModel model = BPMNParser.parseModel(result, "UTF-8");
						modelService.saveModel(model);
					} else {
						// XML
						importXmlEntityData(result);
					}
				} else {
					logger.warning("Wrong model format: '" + filePath + "' - expected *.bpmn or *.xml");
				}

			}
		} catch (Exception e) {
			logger.severe(
					"unable to load model configuration - please check imixs.properties file for key 'setup.defaultModel'");
			throw new RuntimeException(
					"loadDefaultModels - unable to load model configuration - please check imixs.properties file for key 'setup.defaultModel'");
		}

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