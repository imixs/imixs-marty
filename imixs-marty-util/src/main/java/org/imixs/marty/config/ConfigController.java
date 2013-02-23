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

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.imixs.marty.ejb.ConfigService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.faces.workitem.DataController;

/**
 * This ConfigController acts as a frontend controller for a Config Entity. The
 * entity (itemCollection) holds config params. The entity is stored with the
 * type "configuration" and a configurable name (txtname). The property
 * 'txtname' is used to select the config entity by a query.
 * 
 * The bean interacts with the marty ConfigService EJB which is responsible for
 * creation, loading and saving the entity. This singleton ejb can manage
 * multiple config entities. The ConfigController bean is also
 * ApplicationScoped, so it can be shared in one application. From the backend
 * it is possible to use the ConfigControler or also directly the ConfigService
 * EJB.
 * 
 * The Bean can be overwritten to add additional busines logic (e.g. converting
 * params or providing additional custom getter methods).
 * 
 * 
 * Use multiple instances in one application, bean can be decleared in the faces-config.xml file.
 * The managed-ban-name as the manged property 'name' 
 * can be set to custom values:
 * 
 * <code>
  	<managed-bean>
		<managed-bean-name>myConfigMB</managed-bean-name>
		<managed-bean-class>org.imixs.marty.web.util.ConfigMB</managed-bean-class>
		<managed-property>
			<property-name>name</property-name>
			<value>REPORT_CONFIGURATION</value>
		</managed-property>
	</managed-bean>
 * </code>
 * 
 * The Bean provides easy access to the config params from a JSF Page. Example:
 * 
 * <code>
 * <h:inputText value="#{configController.workitem.item['myParam1']}" >
						</h:inputText>
 * </code>
 * 
 * @author rsoika
 * 
 */
@ApplicationScoped
public class ConfigController implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name = "CONFIGURATION";

	private ItemCollection configItemCollection = null;

	@EJB
	ConfigService configService;

	public ConfigController() {
		super();
	}

	/**
	 * This method load the config entity after postContstruct. If no Entity
	 * exists than the ConfigService EJB creates a new config entity.
	 * 
	 * */
	@PostConstruct
	public void init() {
		configItemCollection = configService.loadConfiguration(getName());
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
	 * save method updates the txtname property and save the config entity
	 * 
	 * @throws AccessDeniedException
	 */
	public void save() throws AccessDeniedException {
		// update name
		configItemCollection.replaceItemValue("txtname", this.getName());
		// save entity
		configItemCollection = configService.save(configItemCollection);

	}

	@Deprecated
	private ArrayList<SelectItem> getLocaleSelection() {
		return null;

	}

}