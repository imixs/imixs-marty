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

import java.util.List;

import org.imixs.workflow.ItemCollection;

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
 * Config Service ================== This Service Facade encapsulates a
 * configuration service business object A config object is represented by a
 * ItemCollection with the following Attributes:
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

public interface ConfigService {

	
	
	/**
	 * This method process a config object using the Imixs WorkflowManager
	 * 
	 * @param aconfig
	 *            ItemCollection representing the issue
	 * @param activityID
	 *            activity ID the issue should be processed
	 */
	public ItemCollection processConfiguration(ItemCollection aconfig,
			int activityID) throws Exception;

	/**
	 * creates a new configuration object for a specified name
	 * 
	 * @return
	 */
	public ItemCollection createConfiguration(String name) throws Exception;

	/**
	 * This method deletes an existing project.
	 * 
	 * @param aTeamName
	 */
	public void deleteConfiguration(ItemCollection aProject) throws Exception;

	/**
	 * This method loads an configuration object with a spcified name. If no
	 * Configuraiton object exists with this name the method returns null
	 * 
	 * @param name
	 *            in attribute txtname
	 * @return
	 */
	public ItemCollection loadConfiguration(String aname);

	/**
	 * Returns a list of all configuration objects
	 * 
	 * @param row
	 * @param count
	 * @return
	 */
	public List<ItemCollection> findAllConfigurations(int row, int count);

}
