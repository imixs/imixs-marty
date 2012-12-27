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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.faces.util.SelectItemComparator;

/**
 * This ConfigMB acts as a generic config bean. The bean manages an
 * entity which holds config params in an itemCollection object. The
 * entity is stored with the type property "configuration" and a configurable
 * name property (txtname). The name property is used to select the params by a
 * query. So the bean can be used to provide different config settings in one
 * application.
 * 
 * The name property can be set using the faces-config.xml descriptor:
 * 
 * <code>
  	<managed-bean>
		<managed-bean-name>configMB</managed-bean-name>
		<managed-bean-class>org.imixs.marty.web.util.ConfigMB</managed-bean-class>
		<managed-bean-scope>session</managed-bean-scope>
		<managed-property>
			<property-name>name</property-name>
			<value>REPORT_CONFIGURATION</value>
		</managed-property>
	</managed-bean>
 * </code>
 * 
 * The Bean provides access to the config params from a JSF Page. Example:
 * 
 * <code>
 * <h:inputText value="#{configMB.item['myParam1']}" >
						</h:inputText>
 * </code>
 * 
 * @author rsoika
 * 
 */
@Named("configMB")
@SessionScoped
public class ConfigMB implements Serializable {

	private static final long serialVersionUID = 1L;
	final static public String TYPE = "configuration";

	private String name = "CONFIGURATION";

	private ItemCollection configItemCollection = null;

	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	public ConfigMB() {
		super();
	}

	/**
	 * This method tries to load the config entity. If no Entity exists than the
	 * method creates a new entity
	 * 
	 * */
	@PostConstruct
	public void init() {
		doLoadConfiguration(null);
	}

	/**
	 * Returns the name of the configuration entity
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the configuration entity
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	public ItemCollection getWorkitem() {
		return this.configItemCollection;
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
		configItemCollection.replaceItemValue("type", TYPE);
		configItemCollection.replaceItemValue("txtname", this.getName());
		configItemCollection.replaceItemValue("$writeAccess",
				"org.imixs.ACCESSLEVEL.MANAGERACCESS");
		configItemCollection.replaceItemValue("$readAccess", "");
		// save entity
		configItemCollection = entityService.save(configItemCollection);

	}

	/**
	 * This method returns the config ItemCollection from the corresponding
	 * entity. If no configuration is found for this configuration name the
	 * Method creates an empty configuration object.
	 * 
	 * The method reloads the configuration. This is used by the nav.xhtml to
	 * garantie that the config workitem will be reloaded (even if things like
	 * the sequence number have changed)
	 * 
	 */
	public void doLoadConfiguration(ActionEvent event) {

		String sQuery = "SELECT config FROM Entity AS config "
				+ " JOIN config.textItems AS t2" + " WHERE config.type = '"
				+ TYPE + "'" + " AND t2.itemName = 'txtname'"
				+ " AND t2.itemValue = '" + this.getName() + "'"
				+ " ORDER BY t2.itemValue asc";
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);

		if (col.size() > 0) {
			configItemCollection = col.iterator().next();

		} else {
			// create default values
			configItemCollection = new ItemCollection();
			try {
				configItemCollection.replaceItemValue("type", TYPE);
				configItemCollection
						.replaceItemValue("txtname", this.getName());

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public ArrayList<SelectItem> getLocaleSelection() {
		return null;

	}

	/**
	 * SelectItem getter Method provides a getter method to an ArrayList of
	 * <SelectItem> objects for a specific param stored in the configuration
	 * entity.
	 * 
	 * <code>
	 * <f:selectItems value="#{configMB.selectItems['txtMyParam2']}" />
	 * </code>
	 * 
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public Map getSelectItems() throws Exception {

		SelectItemsAdapter selectItemAdapter = new SelectItemsAdapter(
				configItemCollection);

		return selectItemAdapter;
	}

	@SuppressWarnings("unchecked")
	public Map getSelectItemsSorted() throws Exception {

		SelectItemsAdapter selectItemAdapter = new SelectItemsAdapter(
				configItemCollection, true);

		return selectItemAdapter;
	}

	public ItemCollection getItemCollection() {
		return this.configItemCollection;
	}

	/**
	 * This class helps to adapt the behavior of a singel value item to be used
	 * in a jsf page using a expression language like this:
	 * 
	 * #{mybean.selectItems['txtMyItem']}
	 * 
	 * 
	 * @author rsoika
	 * 
	 */
	@SuppressWarnings({ "serial", "unchecked" })
	class SelectItemsAdapter extends HashMap {
		ItemCollection itemCollection;
		boolean bSorted = false;

		public SelectItemsAdapter() {
			itemCollection = new ItemCollection();
		}

		public SelectItemsAdapter(ItemCollection acol) {
			itemCollection = acol;
		}

		public SelectItemsAdapter(ItemCollection acol, boolean sorted) {
			itemCollection = acol;
			bSorted = sorted;
		}

		/**
		 * returns a ArrayList<SelectItem> out of the ItemCollection if the key
		 * dos not exist the method will return an empty list
		 */
		public Object get(Object key) {
			ArrayList<SelectItem> selection;

			selection = new ArrayList<SelectItem>();

			// check if a value for this key is available...
			// if not create a new empty value
			try {
				if (!itemCollection.hasItem(key.toString()))
					// return empty selection
					return selection;
			} catch (Exception e) {

				e.printStackTrace();
			}

			// get value list first value from vector if size >0
			List valueList = itemCollection.getItemValue(key.toString());

			for (Object aValue : valueList) {

				// test if aValue has a | as an delimiter
				// expected format in this case is: label|value
				String sValue = aValue.toString();
				String sName = sValue;

				if (sValue.indexOf("|") > -1) {
					sName = sValue.substring(0, sValue.indexOf("|"));
					sValue = sValue.substring(sValue.indexOf("|") + 1);
				}
				selection.add(new SelectItem(sValue.trim(), sName.trim()));

			}

			// sort result?
			if (bSorted)
				Collections.sort(selection, new SelectItemComparator(true));

			return selection; 
		}
	}

}
