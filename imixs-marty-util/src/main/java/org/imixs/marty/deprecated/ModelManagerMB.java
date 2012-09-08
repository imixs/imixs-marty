package org.imixs.marty.deprecated;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.event.ActionEvent;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;


/**
 * This Backingbean provides methods to upload model files from the UI. 
 * The bean also supports the deletion of models.
 * 
 * @author rsoika
 */

@Named("modelManagerMB")
@SessionScoped
public class ModelManagerMB implements Serializable {

	private static final long serialVersionUID = 1L;
	@EJB
	EntityService entityService;
	@EJB
	org.imixs.workflow.jee.ejb.ModelService modelService;

	private final String DEPRECATED_NO_VERSION = "DEPRECATED-NO-VERSION";
	private String currentModelVersion;

	/**
	 * This method register the bean as an workitemListener
	 */
	@PostConstruct
	public void init() {

	}


	/**
	 * This method returns a list of available models. The model descriptions
	 * are provided in Map Objects. The method is called by the page
	 * /page/admin/modellist.xhtml
	 * 
	 * @return
	 */
	public ArrayList<Map> getModels() {
		ArrayList models = new ArrayList<Map>();
		if (entityService != null) {

			// select all Models versions - identified by the
			// environment.profile entity included in each model
			String modelQuery = "SELECT process FROM Entity AS process"
					+ " JOIN process.textItems as t"
					+ " JOIN process.textItems as n"
					+ " WHERE t.itemName = 'type' AND t.itemValue = 'WorkflowEnvironmentEntity'"
					+ " AND n.itemName = 'txtname' AND n.itemValue = 'environment.profile'";

			Collection<ItemCollection> col = entityService.findAllEntities(
					modelQuery, 0, -1);

			for (ItemCollection aworkitem : col) {
				// ad entity creation and modified date
				Map map = aworkitem.getAllItems();
				map.put("$uniqueid", aworkitem.getItemValueString("$uniqueid"));
				map.put("$created", aworkitem.getItemValueDate("$created"));

				String sModel = DEPRECATED_NO_VERSION;
				if (aworkitem.hasItem("$modelversion"))
					sModel = aworkitem.getItemValueString("$modelversion");
				map.put("$modelversion", sModel);

				map.put("$modified", aworkitem.getItemValueDate("$modified"));
				models.add(map);
			}

		}

		return models;
	}

	/**
	 * This Method select the model from the ui
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doSelectModel(ActionEvent event) throws Exception {
		Map currentSelection = null;
		// ItemCollectionAdapter currentSelection = null;
		// seach selected row...
		UIComponent component = event.getComponent();

		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;
			// get current project from row
			currentSelection = (Map) ((UIData) parent).getRowData();
			break;
		}

		currentModelVersion = currentSelection.get("$modelversion").toString();

	}

	/**
	 * This Method Selects the current project and refreshes the Worklist Bean
	 * so wokitems of these project will be displayed after show_worklist
	 * 
	 * Furthermore the method call loadProcessList to support a List of
	 * StartProcess ItemCollections. Forms and Views can use the
	 * getProcessList() method to show a list of StartProcesses
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doDeleteModel(ActionEvent event) throws Exception {

		String sQuery;
		/*
		 * To different methods to support older versions
		 */
		if (DEPRECATED_NO_VERSION.equals(currentModelVersion)) {
			sQuery = "SELECT process FROM Entity AS process "
					+ "	 JOIN process.textItems as t"
					+ "	 WHERE t.itemName = 'type' AND t.itemValue IN('ProcessEntity', 'ActivityEntity', 'WorkflowEnvironmentEntity')";
		} else {
			sQuery = "SELECT process FROM Entity AS process "
					+ "	 JOIN process.textItems as t"
					+ "	 JOIN process.textItems as v"
					+ "	 WHERE t.itemName = 'type' AND t.itemValue IN('ProcessEntity', 'ActivityEntity', 'WorkflowEnvironmentEntity')"
					+ " 	 AND v.itemName = '$modelversion' AND v.itemValue = '"
					+ currentModelVersion + "'";
		}

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, -1);

		for (ItemCollection aworkitem : col) {
			/*
			 * To different methods to support older versions
			 */
			if (DEPRECATED_NO_VERSION.equals(currentModelVersion)) {
				if (!aworkitem.hasItem("$modelversion"))
					entityService.remove(aworkitem);
			} else {
				entityService.remove(aworkitem);
			}
		}

		
	}

	


}