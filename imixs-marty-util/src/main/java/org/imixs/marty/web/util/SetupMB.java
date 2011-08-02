package org.imixs.marty.web.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.event.ActionEvent;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.imixs.marty.business.ConfigService;
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
public class SetupMB {

	/**
	 * Default values for maximum history entries are currently hard coded
	 */
	private int maxProjectHistoryLength = 10;
	private int maxProfileHistoryLength = 10;
	private int maxWorkitemHistoryLength = 10;
	private int sortby = 0;
	private int sortorder = 0;
	private int maxviewentriesperpage = 10;
	private int defaultworklistview=0;
	private boolean createDefaultProject = false;
	private String defaultPage = "pages/notes";

	public final static String SYW_CONFIGURATION = "SYW_CONFIGURATION";

	
	private boolean setupOk = false;

	
	private ItemCollection configItemCollection = null;
	
	@EJB
	ConfigService configService;
	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	@EJB
	org.imixs.workflow.jee.ejb.EntityService epm;
	
	@EJB
	org.imixs.workflow.jee.ejb.ModelService modelService;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	
	public SetupMB() {
		super();
	}

	public ItemCollection getWorkitem() {
		
		return configItemCollection;
	}
	/**
	 * This method tries to load the config entity to read default values
	 */
	@PostConstruct
	public void init() {
		configItemCollection = configService
				.loadConfiguration(SYW_CONFIGURATION);

		if (configItemCollection == null) {
			
			try {
				configItemCollection = configService
				.createConfiguration(SYW_CONFIGURATION);
			} catch (Exception e) {				
				e.printStackTrace();
			}
			
			// set default values
			try {
				configItemCollection.replaceItemValue("maxProjectHistoryLength", maxProjectHistoryLength);
				
				configItemCollection.replaceItemValue("maxProfileHistoryLength", maxProfileHistoryLength);
				configItemCollection.replaceItemValue("maxWorkitemHistoryLength", maxWorkitemHistoryLength);
				configItemCollection.replaceItemValue("createDefaultProject", createDefaultProject);
				
				configItemCollection.replaceItemValue("defaultPage", defaultPage);
				//configItemCollection.replaceItemValue("defaultworklistview", defaultworklistview);
				configItemCollection.replaceItemValue("maxviewentriesperpage", maxviewentriesperpage);
				configItemCollection.replaceItemValue("sortby", sortby);
				configItemCollection.replaceItemValue("sortorder", sortorder);
				configItemCollection.replaceItemValue("maxProjectHistoryLength", maxProjectHistoryLength);
				configItemCollection.replaceItemValue("maxProjectHistoryLength", maxProjectHistoryLength);
				configItemCollection.replaceItemValue("defaultworklistview", defaultworklistview);
					
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			
			
		}
		
		
		setupOk = configItemCollection.getItemValueBoolean(
					"keySystemSetupCompleted");

	
	
	}

	
	public boolean isSetupOk() {
		return setupOk;
	}

	public void setSetupOk(boolean systemSetupOk) {
		this.setupOk = systemSetupOk;
	}
	
	
	/**
	 * save method tries to load the config entity. if not availabe. the method
	 * will create the entity the first time
	 * 
	 * @return
	 * @throws Exception
	 */
	
	public void doSave(ActionEvent event) throws Exception {
		// update write and read access
		//configItemCollection.replaceItemValue("type", TYPE);
		configItemCollection.replaceItemValue("txtname",SYW_CONFIGURATION);
		configItemCollection.replaceItemValue("$writeAccess", "org.imixs.ACCESSLEVEL.MANAGERACCESS");
		configItemCollection.replaceItemValue("$readAccess", "");
		// save entity
		configItemCollection=entityService.save(configItemCollection);
		
		
	}
	
	

	


	/**
	 * starts a ConsistencyCheck without updating values
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doSetup(ActionEvent event) throws Exception {
		logger.info("Imixs Office Workflow : starting System Setup...");
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
		getWorkitem().replaceItemValue(
				"keySystemSetupCompleted", true);
		doSave(event);

		// load default models
		loadDefaultModels();

		setupOk = true;

		logger.info("Imixs Office Workflow : starting System Setup completed!");

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

		logger.info(" check for default models...");
		try {
			List<String> col = modelService.getAllModelVersions();
			// check if system model is available
			for (String sversion : col) {

				String sModelDomain = sversion.substring(0,
						sversion.indexOf("-"));

				if ("system".equals(sModelDomain)) {

					logger.info("System model allready instaled - skip loading default models");
					return;

				}
			}

			logger.info(" Loading default Models..");

			ResourceBundle r = ResourceBundle.getBundle("configuration.model");
			if (r == null)
				logger.warning("SystemSetup: no configuration/model.properties file found!");
			Enumeration<String> enkeys = r.getKeys();
			while (enkeys.hasMoreElements()) {
				String sKey = enkeys.nextElement();
				logger.info(" loading Model: " + sKey);
				// try to load this model
				String filePath = r.getString(sKey);

				InputStream inputStream = SetupMB.class.getClassLoader()
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

				this.importXmlEntityData(result);
			}
		} catch (Exception e) {
			logger.warning("SystemSetup: unable to load default models - please check configuration/model.properties file!");
			e.printStackTrace();
		}

	}

	/**
	 * this method imports an xml entity data stream. This is used to provide
	 * model uploads during the system setup. The method can also import general
	 * entity data like project data.
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void importXmlEntityData(byte[] filestream) throws Exception {
		XMLItemCollection entity;
		ItemCollection itemCollection;
		String sModelVersion = null;
		
		
		if (filestream == null)
			return;
		try {
			EntityCollection ecol = null;
			logger.info("import EntityData from file....");
			JAXBContext context = JAXBContext
					.newInstance(EntityCollection.class);
			Unmarshaller m = context.createUnmarshaller();

			ByteArrayInputStream input = new ByteArrayInputStream(filestream);
			Object jaxbObject = m.unmarshal(input);
			if (jaxbObject == null) {
				throw new Exception("WARNING - wrong xml file format - unable to import!");
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
					logger.info(" Removing old Model Version: " + aModelVersion);
					modelService.removeModelVersion(aModelVersion);

				}

				logger.info(" Starting import...");
				// save new entities into database and update modelversion.....
				for (int i = 0; i < ecol.getEntity().length; i++) {
					entity = ecol.getEntity()[i];
					itemCollection = XMLItemCollectionAdapter
							.getItemCollection(entity);
					// save entity
					entityService.save(itemCollection);
				}

				logger.info(ecol.getEntity().length
						+ " Entities sucessfully imported");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	

}
