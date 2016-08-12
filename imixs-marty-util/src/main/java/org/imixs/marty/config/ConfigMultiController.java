/*******************************************************************************
 *  Imixs Workflow Technology
 *  Copyright (C) 2003, 2008 Imixs Software Solutions GmbH,  
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
 *  
 *******************************************************************************/
package org.imixs.marty.config;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.faces.workitem.DataController;
import org.imixs.workflow.jee.faces.workitem.IViewAdapter;
import org.imixs.workflow.jee.faces.workitem.ViewController;

/**
 * This backing bean provides different custom configuration documents. The type
 * of the configuration is defined by the property 'type'. The key of a single
 * configuration entity is stored in the property 'txtName'
 * 
 * The property $WriteAccess is set to 'org.imixs.ACCESSLEVEL.MANAGERACCESS'.
 * 
 * An instance of this bean is defined via faces-config.xml
 * 
 * 
 * txtName is the primary key
 * 
 * faces-config example: <code>
 *  <managed-bean>
		<managed-bean-name>carcompanyMB</managed-bean-name>
		<managed-bean-class>org.imixs.marty.web.util.ConfigMultiMB</managed-bean-class>
		<managed-bean-scope>session</managed-bean-scope>
		<managed-property>
			<property-name>type</property-name>
			<value>carcompany</value>
		</managed-property>
	</managed-bean>
 *  </code>
 * 
 *
 * 
 * @author rsoika
 * 
 */
public class ConfigMultiController extends DataController implements IViewAdapter {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ConfigMultiController.class.getName());

	
	@EJB
	private org.imixs.workflow.jee.ejb.EntityService entityService;
	
	ViewController viewController = null;

	public ConfigMultiController() {
		super();
	}
	
	/**
	 * The init method is used to add necessary indices to the entity index list
	 * if index still exists the method did change any data
	 */
	@PostConstruct
	public void init() {
		viewController = new ViewController();
		viewController.setType(this.getDefaultType());
		viewController.setView("worklist.modified.desc");
	}

	/**
	 * save method tries to load the config entity. if not availabe. the method
	 * will create the entity the first time
	 * 
	 * @return
	 */
	@Override
	public void save() throws AccessDeniedException {
		// update write and read access
		this.getWorkitem().replaceItemValue("$writeAccess", "org.imixs.ACCESSLEVEL.MANAGERACCESS");
		this.getWorkitem().replaceItemValue("$readAccess", "");
		// save entity
		super.save();

	}

	@Override
	public List<ItemCollection> getViewEntries(ViewController controller) {
		return viewController.getWorkitems();
	}

	/**
	 * Selects a singel config ItemCollection by Name
	 * 
	 * @param name
	 * @return
	 */
	public ItemCollection getEntityByName(String name) {
		ItemCollection configItemCollection = null;

		// load / create config entity....
		String sQuery = "SELECT config FROM Entity AS config " + " JOIN config.textItems AS t2"
				+ " WHERE config.type = '" + getDefaultType() + "'" + " AND t2.itemName = 'txtname'" + " AND t2.itemValue = '"
				+ name + "'" + " ORDER BY t2.itemValue asc";
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery, 0, 1);

		if (col.size() > 0) {
			configItemCollection = col.iterator().next();
		} else {
			logger.fine("MultiConfigItem '" + name + "' not found");
		}

		return configItemCollection;

	}

}
