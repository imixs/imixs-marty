package org.imixs.sywapps.web.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.imixs.sywapps.business.ConfigService;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.jpa.EntityIndex;
import org.imixs.workflow.xml.EntityCollection;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

/**
 * This Backing Bean acts as a application wide config bean. It holds general
 * config parms like the current historylength. The parameters are stored in a
 * configuration entity.
 * 
 * @author rsoika
 * 
 */
public class SystemSetupMB {

	private boolean setupOk = false;
	private ConfigMB configMB = null;

	@EJB
	org.imixs.workflow.jee.ejb.EntityService epm;
	@EJB
	ConfigService configService;
	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;
	@EJB
	org.imixs.workflow.jee.ejb.ModelService modelService;

	public SystemSetupMB() {
		super();
	}

	/**
	 * This method tries to load the config entity to read default values
	 */
	@PostConstruct
	public void init() {
		setupOk = getConfigBean().getWorkitem().getItemValueBoolean(
				"keySystemSetupCompleted");

	}

	public boolean isSetupOk() {
		return setupOk;
	}

	public void setSetupOk(boolean systemSetupOk) {
		this.setupOk = systemSetupOk;
	}

	/**
	 * starts a ConsistencyCheck without updating values
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doSetup(ActionEvent event) throws Exception {
		System.out.println("Imixs Office Workflow : starting System Setup...");
		// model
		epm.addIndex("numprocessid", EntityIndex.TYP_INT);
		epm.addIndex("numactivityid", EntityIndex.TYP_INT);

		// workflow
		epm.addIndex("type", EntityIndex.TYP_TEXT);
		epm.addIndex("$uniqueidref", EntityIndex.TYP_TEXT);
		epm.addIndex("$workitemid", EntityIndex.TYP_TEXT);
		epm.addIndex("$processid", EntityIndex.TYP_INT);
		epm.addIndex("txtworkflowgroup", EntityIndex.TYP_TEXT);
		epm.addIndex("txtworkflowsummary", EntityIndex.TYP_TEXT);
		epm.addIndex("namcreator", EntityIndex.TYP_TEXT);

		epm.addIndex("$modelversion", EntityIndex.TYP_TEXT);

		// app
		epm.addIndex("txtname", EntityIndex.TYP_TEXT);
		epm.addIndex("txtemail", EntityIndex.TYP_TEXT);
		epm.addIndex("namteam", EntityIndex.TYP_TEXT);
		epm.addIndex("namowner", EntityIndex.TYP_TEXT);
		epm.addIndex("dattermin", EntityIndex.TYP_CALENDAR);

		epm.addIndex("txtProjectName", EntityIndex.TYP_TEXT);
		epm.addIndex("txtUsername", EntityIndex.TYP_TEXT);

		// update System configuration.....
		getConfigBean().getWorkitem().replaceItemValue(
				"keySystemSetupCompleted", true);
		this.getConfigBean().doSave(event);

		// load default models
		loadDefaultModels();

		setupOk = true;

		System.out
				.println("Imixs Office Workflow : starting System Setup completed!");

	}

	/**
	 * This method loads the default model files defined by the configuration
	 * file: /configuration/model.properties
	 * 
	 * 
	 * @param aSkin
	 * @return
	 */
	public void loadDefaultModels() {
		
		System.out.println(" check default Models..");
		try {
			List<String> col = modelService.getAllModelVersions();
			// check if system model is available
			for (String sversion : col) {

				String sModelDomain = sversion.substring(0,
						sversion.indexOf("-"));

				if ("system".equals(sModelDomain)) {

					System.out.println("System model found!");
					return;

				}
			}

			System.out.println(" Loading default Models..");

			ResourceBundle r = ResourceBundle.getBundle("configuration.model");

			Enumeration<String> enkeys = r.getKeys();
			while (enkeys.hasMoreElements()) {
				String sKey = enkeys.nextElement();

				// try to load this model
				String filePath = r.getString(sKey);

				InputStream inputStream = SystemSetupMB.class.getClassLoader()
						.getResourceAsStream(filePath);
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

				this.importXmlModelFile(result);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * this method imports an xml model stream. This is used to provide model
	 * uploads or during the system setup
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void importXmlModelFile(byte[] filestream) throws Exception {
		if (filestream == null)
			return;
		try {
			EntityCollection ecol = null;

			JAXBContext context = JAXBContext
					.newInstance(EntityCollection.class);
			Unmarshaller m = context.createUnmarshaller();

			ByteArrayInputStream input = new ByteArrayInputStream(filestream);
			Object jaxbObject = m.unmarshal(input);
			if (jaxbObject == null) {
				throw new Exception("WARNING - no xml model file!");
			}

			ecol = (EntityCollection) jaxbObject;

			XMLItemCollection entity;
			ItemCollection itemCollection;

			String sModelVersion = null;

			if (ecol.getEntity().length > 0) {
				/*
				 * first we need get model version from first entity
				 */
				entity = ecol.getEntity()[0];
				itemCollection = XMLItemCollectionAdapter
						.getItemCollection(entity);
				sModelVersion = itemCollection
						.getItemValueString("$ModelVersion");

				/*
				 * now we need to delete the old model if available.
				 */

				if (!"".equals(sModelVersion))
					modelService.removeModelVersion(sModelVersion);

				// save new entities into database and update modelversion.....
				for (int i = 0; i < ecol.getEntity().length; i++) {
					entity = ecol.getEntity()[i];
					itemCollection = XMLItemCollectionAdapter
							.getItemCollection(entity);
					// update model version
					itemCollection.replaceItemValue("$modelVersion",
							sModelVersion);
					// save entity
					entityService.save(itemCollection);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private ConfigMB getConfigBean() {
		if (configMB == null)
			configMB = (ConfigMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "configMB");

		return configMB;
	}

}
