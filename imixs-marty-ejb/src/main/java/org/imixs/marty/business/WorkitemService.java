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

public interface WorkitemService {

	final static int SORT_BY_CREATED = 0;
	final static int SORT_BY_MODIFIED = 1;
	final static int SORT_ORDER_DESC = 0;
	final static int SORT_ORDER_ASC = 1;

	/**
	 * This method creates a new WorkItem which will become a response object to
	 * the supported parent WOrktiem. The new WorkItem will be assigned to the
	 * ProcessEntity defined by the provided ModelVersion and ProcessID
	 * 
	 * @param parent
	 *            ItemCollection representing the parent where the workItem is
	 *            assigned to. This is typical a project entity but can also be
	 *            an other workItem
	 * @param processEntity
	 *            ItemCollection representing the ProcessEntity where the
	 *            workItem is assigned to
	 */
	public ItemCollection createWorkItem(ItemCollection project,
			String sProcessModelVersion, int aProcessID) throws Exception;

	/**
	 * changes the ProcessEntity assigned to a WorkItem
	 * 
	 * @param aworkitem
	 * @param processEntity
	 * @return
	 * @throws Exception
	 */
	public ItemCollection changeProcess(ItemCollection aworkitem,
			ItemCollection processEntity) throws Exception;

	/**
	 * This method process a workitem
	 * 
	 * @param workitem
	 *            ItemCollection representing the issue
	 * @param activityID
	 *            activity ID the issue should be processed
	 */
	public ItemCollection processWorkItem(ItemCollection workitem)
			throws Exception;

	/**
	 * This method deletes a workitem form the database. The method deletes also
	 * attached child workitems
	 * 
	 * @param workitem
	 *            ItemCollection representing the issue
	 */
	public void deleteWorkItem(ItemCollection workitem) throws Exception;

	/**
	 * Moves a workitem into the archive. changes the type attriubte to
	 * "workitemarchive" The method updates also attached child workitems
	 * 
	 * @param workitem
	 * @return
	 * @throws Exception
	 */
	public ItemCollection moveIntoArchive(ItemCollection workitem)
			throws Exception;

	/**
	 * Performse a soft delete for a workitem. changes the type attriubte to
	 * "workitemdeleted" The method updates also attached child workitems
	 * 
	 * @param workitem
	 * @return
	 * @throws Exception
	 */
	public ItemCollection moveIntoDeletions(ItemCollection workitem)
			throws Exception;

	/**
	 * chages the type attriubte to "workitem" The method updates also attached
	 * child workitems
	 * 
	 * @param aIssue
	 * @return
	 * @throws Exception
	 */
	public ItemCollection restoreFromArchive(ItemCollection aIssue)
			throws Exception;

	/**
	 * chages the type attriubte to "workitem" The method updates also attached
	 * child workitems
	 * 
	 * @param aIssue
	 * @return
	 * @throws Exception
	 */
	public ItemCollection restoreFromDeletions(ItemCollection aIssue)
			throws Exception;

	/**
	 * Searches for workitems by different attributes
	 * 
	 * @param project
	 * @param process
	 * @param type
	 *            - workitem or workitemarchive expected
	 * @param row
	 * @param count
	 * @return
	 */
	public List<ItemCollection> findWorkitemsByQuery(String query, int row,
			int count);

	public List<ItemCollection> findWorkitemsByAuthor(String ref,
			String processgroup, int row, int count, int sortby, int sortorder);

	public List<ItemCollection> findWorkitemsByCreator(String ref,
			String processgroup, int row, int count, int sortby, int sortorder);

	public List<ItemCollection> findWorkitemsByOwner(String ref,
			String processgroup, int row, int count, int sortby, int sortorder);

	public List<ItemCollection> findAllWorkitems(String ref,
			String processgroup, int row, int count, int sortby, int sortorder);

	public List<ItemCollection> findArchive(String ref, String processgroup,
			int row, int count, int sortby, int sortorder);

	public List<ItemCollection> findDeletions(String ref, String processgroup,
			int row, int count, int sortby, int sortorder);

	public ItemCollection findWorkItem(String id);

}
