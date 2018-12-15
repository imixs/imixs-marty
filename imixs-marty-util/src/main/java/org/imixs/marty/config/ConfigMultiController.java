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
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.inject.Inject;

import org.imixs.marty.profile.UserController;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.faces.data.DocumentController;
import org.imixs.workflow.faces.data.ViewController;

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
public class ConfigMultiController extends DocumentController {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ConfigMultiController.class.getName());

	private List<ItemCollection> workitems = null;
	private String sortBy=null;

	@Inject
	private UserController userController;

	@EJB
	private DocumentService documentService;

	
	public String getSortBy() {
		return sortBy;
	}

	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}

	/**
	 * save method tries to load the config entity. if not availabe. the method will
	 * create the entity the first time
	 * 
	 * @return
	 */
	@Override
	public void save() throws AccessDeniedException {
		// update write and read access
		this.getDocument().replaceItemValue("$writeAccess", "org.imixs.ACCESSLEVEL.MANAGERACCESS");
		this.getDocument().replaceItemValue("$readAccess", "");
		// save entity
		super.save();

	}

	@Override
	public void reset() {
		super.reset();
		workitems=null;
	}

	/**
	 * Selects a single config ItemCollection by Name
	 * 
	 * @param name
	 * @return
	 */
	public ItemCollection getEntityByName(String name) {
		ItemCollection configItemCollection = null;

		String sQuery = "(type:\"" + getDefaultType() + "\" AND txtname:\"" + name + "\")";
		Collection<ItemCollection> col;
		try {
			col = documentService.find(sQuery, 1, 0);

			if (col.size() > 0) {
				configItemCollection = col.iterator().next();
			} else {
				logger.fine("MultiConfigItem '" + name + "' not found");
			}
		} catch (QueryException e) {
			logger.warning("getEntityByName - invalid query: " + e.getMessage());
		}

		return configItemCollection;

	}

	
	
	/**
	 * Default worklist sorted by txtname
	 * 
	 * @return view result
	 * @throws QueryException
	 */
	public List<ItemCollection> getWorkitems() throws QueryException {
		return getEntitiesSortedBy(getSortBy());
	}
	
	
	/**
	 * returns all entities sorted by ItemCollectionComparator
	 * 
	 * @param name
	 * @return
	 */
	public List<ItemCollection> getEntitiesSortedBy(String sortBy) {

		if (workitems == null) {
			String sQuery = "(type:\"" + getDefaultType() + "\")";

			try {
				workitems = documentService.find(sQuery, 999, 0, sortBy, false);

				if (workitems.size() > 0 && sortBy!=null && !sortBy.isEmpty()) {
					// sort by comperator
					Collections.sort(workitems,
							new ItemCollectionComparator(sortBy, true, userController.getLocale()));
				}
			} catch (QueryException e) {
				logger.warning("getEntitiesSortedBy - invalid query: " + e.getMessage());
			}
		}
		return workitems;

	}

}
