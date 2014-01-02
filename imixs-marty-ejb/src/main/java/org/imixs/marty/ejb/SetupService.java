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
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.ejb.ModelService;
import org.imixs.workflow.jee.jpa.EntityIndex;
import org.imixs.workflow.jee.util.PropertyService;
import org.imixs.workflow.xml.EntityCollection;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

/**
 * The SetupService EJB initializes the system settings. The service metho init
 * is called by the SetupServlet.
 * 
 * @author rsoika
 * 
 */
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
//@Local
public class SetupService {

	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	@EJB
	ModelService modelService;
	
	@EJB
	PropertyService propertyService;
	
	
	private static Logger logger = Logger.getLogger(SetupService.class
			.getName());

	/**
	 * This method verifies the default configuration and initiates a import of
	 * default data
	 * @throws AccessDeniedException 
	 */
	public void init() throws AccessDeniedException  {
		
		logger.info("[SetupService] starting System Setup...");
		updateIndexList();
		loadDefaultModels();
		
		logger.info("[SetupService] system setup completed");

	}
	
	
	/**
	 * update indexies by adding or removing current setup of indexies.
	 * can be overwritten by subclasses
	 */
	//@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void updateIndexList() throws AccessDeniedException  {
		logger.info("[SetupService] starting System Setup...");
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
		entityService.addIndex("namowner", EntityIndex.TYP_TEXT);
		entityService.addIndex("datdate", EntityIndex.TYP_CALENDAR);
		entityService.addIndex("datfrom", EntityIndex.TYP_CALENDAR);
		entityService.addIndex("datto", EntityIndex.TYP_CALENDAR);
		entityService.addIndex("numsequencenumber", EntityIndex.TYP_INT);
		entityService.addIndex("txtUsername", EntityIndex.TYP_TEXT);

		
		/* !!Remove deprecated indexies!! */
		/* for some reason it is not possible to remove index during deployment.... */
//		entityService.removeIndex("txtProjectName");
//		entityService.removeIndex("namteam");
		
		
		
		
		logger.info("[SetupService] index configuration - ok");

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
	private void loadDefaultModels() {

		
		try {
			List<String> colModelVersions = modelService.getAllModelVersions();
			
			if (!colModelVersions.isEmpty()) {
				logger.info("[SetupService] system model - ok");
				return;
			}

			logger.info("[SetupService] check system model...");
			String sDefaultModelList=propertyService.getProperties().getProperty("setup.defaultModel");
			if (sDefaultModelList== null) {
				logger.warning("[SetupService] setup.defaultModel key is not defined in 'imixs.properties' - no default model imported!");
				return;
			}
			
			logger.fine("[SetupService] setup.defaultModel=" + sDefaultModelList);
			
			StringTokenizer stModelList=new StringTokenizer(sDefaultModelList,",",false);
		
			while (stModelList.hasMoreElements()) {
				// try to load this model
				String filePath = stModelList.nextToken();
				logger.info("[SetupService] loading default model file: '" + filePath + "'....");

				InputStream inputStream = SetupService.class
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

				importXmlEntityData(result);
			}
		} catch (Exception e) {
			logger.severe("[SetupService] unable to load model configuration - please check imixs.properties file for key 'setup.defaultModel'");
			throw new RuntimeException(
					"[SetupService] unable to load model configuration - please check imixs.properties file for key 'setup.defaultModel'");
		}

	}

	
	
	/**
	 * this method imports an xml entity data stream. This is used to provide
	 * model uploads during the system setup. The method can also import general
	 * entity data like configuration data.
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
			EntityCollection ecol = null;
			logger.info("[SetupService] importXmlEntityData - verifing file content....");
			JAXBContext context = JAXBContext
					.newInstance(EntityCollection.class);
			Unmarshaller m = context.createUnmarshaller();

			ByteArrayInputStream input = new ByteArrayInputStream(filestream);
			Object jaxbObject = m.unmarshal(input);
			if (jaxbObject == null) {
				throw new RuntimeException(
						"[SetupService] error - wrong xml file format - unable to import file!");
			}

			ecol = (EntityCollection) jaxbObject;

			// import entities....
			if (ecol.getEntity().length > 0) {

				Vector<String> vModelVersions = new Vector<String>();
				// first iterrate over all enttity and find if model entries are
				// included
				for (XMLItemCollection aentity : ecol.getEntity()) {
					itemCollection = XMLItemCollectionAdapter
							.getItemCollection(aentity);
					// test if this is a model entry
					// (type=WorkflowEnvironmentEntity)
					if ("WorkflowEnvironmentEntity".equals(itemCollection
							.getItemValueString("type"))
							&& "environment.profile".equals(itemCollection
									.getItemValueString("txtName"))) {

						sModelVersion = itemCollection
								.getItemValueString("$ModelVersion");
						if (vModelVersions.indexOf(sModelVersion) == -1)
							vModelVersions.add(sModelVersion);
					}
				}
				// now remove old model entries....
				for (String aModelVersion : vModelVersions) {
					logger.info("[SetupService] removing existing configuration for model version '"
							+ aModelVersion + "'");
					modelService.removeModelVersion(aModelVersion);
				}
				// save new entities into database and update modelversion.....
				for (int i = 0; i < ecol.getEntity().length; i++) {
					entity = ecol.getEntity()[i];
					itemCollection = XMLItemCollectionAdapter
							.getItemCollection(entity);
					// save entity
					entityService.save(itemCollection);
				}

				logger.info("[SetupService] " + ecol.getEntity().length
						+ " entries sucessfull imported");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		

	}

}