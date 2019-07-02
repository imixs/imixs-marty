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
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.marty.ejb.security.UserGroupService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.bpmn.BPMNModel;
import org.imixs.workflow.bpmn.BPMNParser;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.ModelService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDocument;
import org.imixs.workflow.xml.XMLDocumentAdapter;
import org.xml.sax.SAXException;

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
// @Stateless
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
// @LocalBean
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Path("/setup")
@Startup
@Singleton
public class SetupUserDBService {

	public static String USERDB_OK = "USERDB_OK";
	public static String USERDB_DISABLED = "USERDB_DISABLED";

	public static String MODEL_OK = "MODEL_OK";
	public static String MODEL_INITIALIZED = "MODEL_INITIALIZED";
	public static String MODEL_ERROR_MISSING_CONFIGURATION = "MODEL_ERROR_MISSING_CONFIGURATION";
	public static String MODEL_ERROR_MISSING_MODELFILE = "MODEL_ERROR_MISSING_MODELFILE";

	@EJB
	DocumentService documentService;

	@EJB
	ModelService modelService;

//	@EJB
//	PropertyService propertyService;

	
	@Inject
	@ConfigProperty(name = "setup.defaultModel", defaultValue = "")
	String sDefaultModelList;
	
	@EJB
	private UserGroupService userGroupService;

	@Inject
	protected Event<SetupEvent> setupEvents;

	@Inject
	@ConfigProperty(name = "setup.mode", defaultValue = "auto")
	String setupMode;

	
	private static Logger logger = Logger.getLogger(SetupUserDBService.class.getName());

	/**
	 * This method start the system setup during deployment
	 * 
	 * @throws AccessDeniedException
	 */
	@PostConstruct
	public void startup() {
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
		logger.info("...starting System Setup...");

		// read setup mode...
//		Properties properties = loadProperties();
		//String setupMode = properties.getProperty("setup.mode", "auto");
		logger.info("setup.mode = " + setupMode);

		// init userIDs for user db?
		if ("auto".equalsIgnoreCase(setupMode)) {
			try {
				if (userGroupService != null) {
					userGroupService.initUserIDs();
					logger.info("... UserDB OK");
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

		// load default models...?
//		if ("auto".equalsIgnoreCase(setupMode) || "model".equalsIgnoreCase(setupMode)) {
//			try {
//				result += ", " + loadDefaultModels();
//			} catch (AccessDeniedException e1) {
//				logger.severe("Error during init setupService: " + e1.getMessage());
//				e1.printStackTrace();
//			}
//		}

		// To extend UserGroups we fire the CDI Event UserGroupEvent...
		if (setupEvents != null) {
			// create Group Event
			SetupEvent setupEvent = new SetupEvent(result);
			setupEvents.fire(setupEvent);
			if (setupEvent.getResult() != null) {
				result = setupEvent.getResult();
			}

		} else {
			logger.warning("Missing CDI support for Event<SetupEvent> !");
		}

		logger.info("...SystemSetup: " + result);
		return result;

	}

	/**
	 * this method imports an xml entity data stream. This is used to provide model
	 * uploads during the system setup. The method can also import general entity
	 * data like configuration data.
	 * 
	 * @param event
	 * @throws Exception
	 */
//	public void importXmlEntityData(byte[] filestream) {
//		XMLDocument entity;
//		ItemCollection itemCollection;
//		String sModelVersion = null;
//
//		if (filestream == null)
//			return;
//		try {
//
//			XMLDataCollection ecol = null;
//			logger.fine("importXmlEntityData - importModel, verifing file content....");
//
//			JAXBContext context;
//			Object jaxbObject = null;
//			// unmarshall the model file
//			ByteArrayInputStream input = new ByteArrayInputStream(filestream);
//			try {
//				context = JAXBContext.newInstance(XMLDataCollection.class);
//				Unmarshaller m = context.createUnmarshaller();
//				jaxbObject = m.unmarshal(input);
//			} catch (JAXBException e) {
//				throw new ModelException(ModelException.INVALID_MODEL,
//						"error - wrong xml file format - unable to import model file: ", e);
//			}
//			if (jaxbObject == null)
//				throw new ModelException(ModelException.INVALID_MODEL,
//						"error - wrong xml file format - unable to import model file!");
//
//			ecol = (XMLDataCollection) jaxbObject;
//			// import the model entities....
//			if (ecol.getDocument().length > 0) {
//
//				Vector<String> vModelVersions = new Vector<String>();
//				// first iterrate over all enttity and find if model entries are
//				// included
//				for (XMLDocument aentity : ecol.getDocument()) {
//					itemCollection = XMLDocumentAdapter.putDocument(aentity);
//					// test if this is a model entry
//					// (type=WorkflowEnvironmentEntity)
//					if ("WorkflowEnvironmentEntity".equals(itemCollection.getItemValueString("type"))
//							&& "environment.profile".equals(itemCollection.getItemValueString("txtName"))) {
//
//						sModelVersion = itemCollection.getItemValueString("$ModelVersion");
//						if (vModelVersions.indexOf(sModelVersion) == -1)
//							vModelVersions.add(sModelVersion);
//					}
//				}
//				// now remove old model entries....
//				for (String aModelVersion : vModelVersions) {
//					logger.fine("importXmlEntityData - removing existing configuration for model version '"
//							+ aModelVersion + "'");
//					modelService.removeModel(aModelVersion);
//				}
//				// save new entities into database and update modelversion.....
//				for (int i = 0; i < ecol.getDocument().length; i++) {
//					entity = ecol.getDocument()[i];
//					itemCollection = XMLDocumentAdapter.putDocument(entity);
//					// save entity
//					documentService.save(itemCollection);
//				}
//
//				logger.fine("importXmlEntityData - " + ecol.getDocument().length + " entries sucessfull imported");
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//	}

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
//	private String loadDefaultModels() {
//		logger.finest("......load default model...");
//
//		List<String> colModelVersions = modelService.getVersions();
//
//		if (!colModelVersions.isEmpty()) {
//			logger.info("existing models: ");
//			for (String amodel : colModelVersions) {
//				logger.info("................ '" + amodel + "'");
//			}
//			return MODEL_OK;
//		}
//
//		logger.info("uploading default models...");
//		//String sDefaultModelList = propertyService.getProperties().getProperty("setup.defaultModel");
//		if (sDefaultModelList == null || sDefaultModelList.isEmpty()) {
//			logger.warning("setup.defaultModel key is not defined in 'imixs.properties' - no default model found");
//			return MODEL_ERROR_MISSING_CONFIGURATION;
//		}
//
//		logger.fine("......setup.defaultModel=" + sDefaultModelList);
//
//		StringTokenizer stModelList = new StringTokenizer(sDefaultModelList, ",", false);
//
//		while (stModelList.hasMoreElements()) {
//			// try to load this model
//			String filePath = stModelList.nextToken();
//			// test if bpmn model?
//			if (filePath.endsWith(".bpmn") || filePath.endsWith(".xml")) {
//				logger.fine("...uploading default model file: '" + filePath + "'....");
//				InputStream inputStream = SetupService.class.getClassLoader().getResourceAsStream(filePath);
//				if (inputStream != null) {
//					// parse model file....
//					try {
//						ByteArrayOutputStream bos = new ByteArrayOutputStream();
//						int next;
//
//						next = inputStream.read();
//
//						while (next > -1) {
//							bos.write(next);
//							next = inputStream.read();
//						}
//						bos.flush();
//						byte[] result = bos.toByteArray();
//
//						// is BPMN?
//						if (filePath.endsWith(".bpmn")) {
//							BPMNModel model = BPMNParser.parseModel(result, "UTF-8");
//							modelService.saveModel(model);
//						} else {
//							// XML
//							importXmlEntityData(result);
//						}
//					} catch (IOException | ModelException | ParseException | ParserConfigurationException
//							| SAXException e) {
//						logger.severe(
//								"unable to load model configuration - please check imixs.properties file for key 'setup.defaultModel'");
//						throw new RuntimeException(
//								"loadDefaultModels - unable to load model configuration - please check imixs.properties file for key 'setup.defaultModel'");
//					}
//				} else {
//					// unable to open file!
//					logger.warning("File not found: '" + filePath + "' - upload failed, verify configuration!");
//					return MODEL_ERROR_MISSING_MODELFILE;
//				}
//			} else {
//				logger.warning("Wrong model format: '" + filePath + "' - expected *.bpmn or *.xml");
//			}
//
//		}
//		return MODEL_INITIALIZED;
//	}

//	/**
//	 * Helper method which loads a imixs.property file
//	 * 
//	 * (located at current threads classpath)
//	 * 
//	 */
//	private Properties loadProperties() {
//		Properties properties = new Properties();
//		try {
//			properties
//					.load(Thread.currentThread().getContextClassLoader().getResource("imixs.properties").openStream());
//		} catch (Exception e) {
//			logger.warning("PropertyService unable to find imixs.properties in current classpath");
//			if (logger.isLoggable(Level.FINE)) {
//				e.printStackTrace();
//			}
//		}
//		return properties;
//	}

}