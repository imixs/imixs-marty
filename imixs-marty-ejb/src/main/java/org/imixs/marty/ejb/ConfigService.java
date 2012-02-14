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

package org.imixs.marty.ejb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.jpa.EntityIndex;

/**
 * Manik is a subproject of the IX JEE Workflow Server Project. This Project
 * supports different general business objects which are useful building a
 * workflow application. So using the manik components there is no need to
 * implement typical business service object which are used in many simmilar
 * projects. So manik is a kind of DRY 'dont repeat yourself' pattern.
 * 
 * Maven: Each manik component is implemented using the maven artifact concept.
 * So in a maven project it is realy easy to add functionallity to a workflow
 * project using the mani components!
 * 
 * Each manik component consits of a service Interface for the business object
 * and a stateless session ejb as a serviceBean implementation. manik uses the
 * ejb 3.0 model
 * 
 * Individual business objects (WorkItem) should not be implementet by a manik
 * componet. This is because only the applicaiton itself knows how the indidual
 * business object (typical the workitem) makes use of standard business
 * objects. For example you can use manik to implement an Project Workflow
 * Object to manage projects and projectteams. But your workitem from your
 * individual workflow application have to decide which team should bes stored
 * to a worktiem during processing. So this job can not be done by a manik
 * component.
 * 
 * 
 * Config Service<br>
 * This Service Facade encapsulates a configuration service business object A
 * config object is represented by a ItemCollection with the following
 * Attributes:
 * 
 * 
 * txtName - Name of the configuration (required)
 * 
 * 
 * all other value objects can be defined by the individual requirements of a
 * project.
 * 
 * @author rsoika
 * 
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class ConfigService  {

	@Resource
	SessionContext ctx;

	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	// Workflow Manager
	@EJB
	org.imixs.workflow.jee.ejb.WorkflowService wm;
	ItemCollection workItem = null;

	final String TYPE = "configuration";

	

	/**
	 * creates a new configuration object for a specified name
	 * 
	 * @return
	 */
	public ItemCollection createConfiguration(String name) throws Exception {
		ItemCollection aworkitem = new ItemCollection();
		aworkitem.replaceItemValue("type", TYPE);
		aworkitem.replaceItemValue("txtname", name);
		return aworkitem;
	}

	/**
	 * This method deletes an existing Configuration.
	 * 
	 * @param aconfig
	 */
	public void deleteConfiguration(ItemCollection aconfig) throws Exception {
		entityService.remove(aconfig);
	}

	/**
	 * This method returns a config ItemCollection for a specified name. If no
	 * configuration is found for this name the Method creates an empty
	 * configuration object.
	 * 
	 * @param name
	 *            in attribute txtname
	 */
	public ItemCollection loadConfiguration(String name) {
		String sQuery = "SELECT config FROM Entity AS config "
				+ " JOIN config.textItems AS t2" + " WHERE config.type = '"
				+ TYPE + "'" + " AND t2.itemName = 'txtname'"
				+ " AND t2.itemValue = '" + name + "'"
				+ " ORDER BY t2.itemValue asc";
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);

		if (col.size() > 0) {
			ItemCollection aworkitem = col.iterator().next();
			return aworkitem;
		}
		return null;

	}

	/**
	 * This method process a config object using the Imixs WorkflowManager
	 * 
	 * @param aconfig
	 *            ItemCollection representing the issue
	 * @param activityID
	 *            activity ID the issue should be processed
	 */
	public ItemCollection processConfiguration(ItemCollection aorgunit,
			int activityid) throws Exception {

		validateConfiguration(aorgunit);
		int iProcessID = aorgunit.getItemValueInteger("$processID");
		String sUniversalID = aorgunit.getItemValueString("$uniqueid");

		String sCreator = aorgunit.getItemValueString("namcreator");

		workItem = aorgunit;
		workItem.replaceItemValue("$ActivityID", activityid);

		// Process workitem...
		ItemCollection result = wm.processWorkItem(workItem);
		return result;

	}

	/**
	 * Returns a list of all configuration objects
	 * 
	 * @param row
	 * @param count
	 * @return
	 */
	public List<ItemCollection> findAllConfigurations(int row, int count) {
		ArrayList<ItemCollection> configList = new ArrayList<ItemCollection>();
		String sQuery = "SELECT orgunit FROM Entity AS orgunit "
				+ " JOIN orgunit.textItems AS t2" + " WHERE orgunit.type = '"
				+ TYPE + "'" + " AND t2.itemName = 'txtname'"
				+ " ORDER BY t2.itemValue asc";
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
	private void validateConfiguration(ItemCollection aproject)
			throws Exception {
		boolean bvalid = true;

		if (!aproject.hasItem("txtname"))
			bvalid = false;
		if (!bvalid)
			throw new Exception(
					"ConfigServiceBean - invalid project object! Attribute 'txtname' not found");

	}

}
