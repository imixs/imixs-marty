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
package org.imixs.marty.business;

import java.util.Collection;
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
 * Project Management ================== This Service Facade encapsulates the
 * magement of project business objects A Project is represented by a
 * ItemCollection with the following Attributes:
 * 
 * 
 * txtName - Name of the Project (required) namteam - List of Teammembers
 * (required)
 * 
 * txtProjectReaders - list of readers (computed) txtProjectAuthors - list of
 * authors (computed)
 * 
 * keypublic - boolean indicates if readaccess is required (false = yes)
 * keymanagement - 1|0 indicates how writeaccess is restricted '0' = namcreator
 * '1' = namteam
 * 
 * 
 * @author rsoika
 * 
 */

public interface ProjectService {

	/**
	 * This method create an empty project object
	 */
	public ItemCollection createProject() throws Exception;

	/**
	 * This method create an empty project object with the defined initial
	 * $processID
	 */
	public ItemCollection createProject(int initialProcessID) throws Exception;

	/**
	 * This method process a project object using the IX WorkflowManager
	 * 
	 * @param aIssue
	 *            ItemCollection representing the issue
	 * @param activityID
	 *            activity ID the issue should be processed
	 */
	public ItemCollection processProject(ItemCollection aIssue)
			throws Exception;

	/**
	 * This method deletes an existing project.
	 * 
	 * @param aProject
	 */
	public void deleteProject(ItemCollection aProject) throws Exception;

	/**
	 * This method analysis an existing project and collects count of workitems
	 * 
	 * @param aProject
	 */
	public void analyseProject(ItemCollection aProject) throws Exception;

	/**
	 * This method returns a project ItemCollection for a specified id
	 * 
	 * @param id
	 * @return
	 */
	public ItemCollection findProject(String id);

	/**
	 * This method returns a project ItemCollection for a specified name.
	 * Returns null if no project with the provided name was found
	 * 
	 * @param name
	 * @return
	 */
	public ItemCollection findProjectByName(String name);

	/**
	 * Returns a list of sub projects for the defined UniqueID
	 * 
	 * @param row
	 * @param count
	 * @return
	 */
	public List<ItemCollection> findAllSubProjects(String aID, int row,
			int count);

	/**
	 * Returns a list of all Main projects
	 * 
	 * @param row
	 * @param count
	 * @return
	 */
	public List<ItemCollection> findAllMainProjects(int row, int count);

	/**
	 * Returns a list of projects
	 * 
	 * @param row
	 * @param count
	 * @return
	 */
	public List<ItemCollection> findAllProjects(int row, int count);

	/**
	 * Returns a list of projects where current user is owner
	 * 
	 * @param row
	 * @param count
	 * @return
	 */
	public List<ItemCollection> findAllProjectsByOwner(int row, int count);

	public List<ItemCollection> findAllProjectsByMember(int row, int count);
	
	/**
	 * This Method returns the teamlist for a specific Project ID
	 * 
	 * @param aproject
	 *            - uniqueid of an existing project
	 * @return collection of Names
	 */
	public Collection<String> getProjectTeam(String aproject);

	
	public ItemCollection moveIntoDeletions(ItemCollection workitem)
	throws Exception;
}
