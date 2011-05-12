/*******************************************************************************
 *  Imixs IX Workflow Technology
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
package org.imixs.sywapps.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.jpa.EntityIndex;

/**
 * @see org.imixs.workflow.manik.project.ProjectService
 * @author rsoika
 * 
 */

@DeclareRoles( { "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed( { "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
public class ConfigServiceBean implements ConfigService {

	@Resource
	SessionContext ctx;

	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	// Workflow Manager
	@EJB
	org.imixs.workflow.jee.ejb.WorkflowService wm;
	ItemCollection workItem = null;
	
	final String  TYPE="configuration";

	@PostConstruct
	private void init_index() throws Exception {
		entityService.addIndex("txtname", EntityIndex.TYP_TEXT);
		entityService.addIndex("namteam", EntityIndex.TYP_TEXT);
	}

	/**
	 * creates a emtpy orgunit object
	 * 
	 * @return
	 * @throws Exception
	 */
	public ItemCollection createConfiguration(String name) throws Exception {
		ItemCollection aworkitem = new ItemCollection();
		aworkitem.replaceItemValue("type", TYPE);
		aworkitem.replaceItemValue("txtname", name);
		return aworkitem;
	}

	/**
	 * Deletes a team from the Team list
	 */
	public void deleteConfiguration(ItemCollection aproject) throws Exception {
		entityService.remove(aproject);
	}

	

	/**
	 * This method returns a config ItemCollection for a specified name.
	 * If no configuration is found for this name the Method creates an empty 
	 * configuration object.
	 * 
	 * @param name
	 *            in attribute txtname
	 */
	public ItemCollection loadConfiguration(String name) {
		String sQuery = "SELECT config FROM Entity AS config "
				+ " JOIN config.textItems AS t2"
				+ " WHERE config.type = '" + TYPE + "'"
				+ " AND t2.itemName = 'txtname'" + " AND t2.itemValue = '"
				+ name + "'" + " ORDER BY t2.itemValue asc";
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);

		if (col.size() > 0) {
			ItemCollection aworkitem = col.iterator().next();
			return aworkitem;
		}
		return null;

	}

	
	/**
	 * Processes an configuration Workitem. The method expects an ItemCollection with
	 * the Datastructure for an configuration. 
	 */
	public ItemCollection processConfiguration(ItemCollection aorgunit, int activityid)
			throws Exception {

		validateConfiguration(aorgunit);
		int iProcessID = aorgunit.getItemValueInteger("$processID");
		String sUniversalID = aorgunit.getItemValueString("$uniqueid");

		String sCreator = aorgunit.getItemValueString("namcreator");

		workItem = aorgunit;
		workItem.replaceItemValue("$ActivityID",activityid);

		// Process workitem...
		ItemCollection result = wm.processWorkItem(workItem);
		return result;

	}

	

	public List<ItemCollection> findAllConfigurations(int row, int count) {
		ArrayList<ItemCollection> configList = new ArrayList<ItemCollection>();
		String sQuery = "SELECT orgunit FROM Entity AS orgunit "
				+ " JOIN orgunit.textItems AS t2"
				+ " WHERE orgunit.type = '"+TYPE+"'"
				+ " AND t2.itemName = 'txtname'" + " ORDER BY t2.itemValue asc";
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		for (ItemCollection aworkitem : col) {
			configList.add(aworkitem);
		}
		return configList;
	}

	/**
	 * This method validates if the attributes supported to a map are
	 * corresponding to the team structure
	 */
	private void validateConfiguration(ItemCollection aproject) throws Exception {
		boolean bvalid = true;
		
		if (!aproject.hasItem("txtname"))
			bvalid = false;
		if (!bvalid)
			throw new Exception(
					"ConfigServiceBean - invalid project object! Attribute 'txtname' not found");

	}

	
}
