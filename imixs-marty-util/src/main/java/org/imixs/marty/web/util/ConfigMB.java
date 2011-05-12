package org.imixs.sywapps.web.util;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.event.ActionEvent;

import org.imixs.sywapps.business.ConfigService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.util.ItemCollectionAdapter;

/**
 * This Backing Bean acts as a application wide config bean. It holds general
 * config parms like the current historylength. The parameters are stored in a
 * configuration entity.
 * 
 * @author rsoika
 * 
 */
public class ConfigMB {

	/**
	 * Default values for maximum history entries are currently hard coded
	 */
	private int maxProjectHistoryLength = 10;
	private int maxProfileHistoryLength = 10;
	private int maxWorkitemHistoryLength = 10;
	private int sortby = 0;
	private int sortorder = 0;
	//private int defaultworklistview = 0;
	private int maxviewentriesperpage = 10;
	private boolean createDefaultProject = false;
	private String defaultPage = "pages/notes";

	public final static String SYW_CONFIGURATION = "SYW_CONFIGURATION";

	protected ItemCollectionAdapter workitemAdapter = null;
	private ItemCollection configItemCollection = null;
	
	@EJB
	ConfigService configService;
	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	public ConfigMB() {
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
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			
			
		}
		
		workitemAdapter = new ItemCollectionAdapter(configItemCollection);

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
		
		workitemAdapter = new ItemCollectionAdapter(configItemCollection);
	}
	
	

	
	/**
	 * WorkitemAdapter getter Methods
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public Map getItem() throws Exception {
		return workitemAdapter.getItem();
	}

	@SuppressWarnings("unchecked")
	public Map getItemList() throws Exception {
		return workitemAdapter.getItemList();
	}

	@SuppressWarnings("unchecked")
	public Map getItemListArray() throws Exception {
		return workitemAdapter.getItemListArray();
	}
	
	

}
