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

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.imixs.workflow.ItemCollection;

/**
 * The WorklistService provides methods to select workItems from different context. 
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
public class WorklistService {

	public final static int SORT_BY_CREATED = 0;
	public final static int SORT_BY_MODIFIED = 1;
	public final static int SORT_ORDER_DESC = 0;
	public final static int SORT_ORDER_ASC = 1;

	// Persistence Manager
	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	
	
	
	@Resource
	SessionContext ctx;


	

	public List<ItemCollection> findWorkitemsByQuery(String query, int row,
			int count) {
		ArrayList<ItemCollection> workitemList = new ArrayList<ItemCollection>();

		if (query == null || "".equals(query))
			return workitemList;
		Collection<ItemCollection> col = entityService.findAllEntities(query,
				row, count);
		workitemList.addAll(col);
		return workitemList;
	}

	/**
	 * Returns a collection of workitems where current user is owner (namOwner)
	 * 
	 * @param model
	 *            - an optional model version to filter workitems
	 * @param processgroup
	 *            - an optional processgroup to filter workitems
	 * @param processid
	 *            - an optional processID to filter workitems
	 * @param row
	 *            - start position
	 * @param count
	 *            - max count of selected worktiems
	 * @return list of workitems 
	 */
	public List<ItemCollection> findWorkitemsByOwner(String ref, String model,
			String processgroup, int processid, int row, int count, int sortby,
			int sortorder) {
		ArrayList<ItemCollection> teamList = new ArrayList<ItemCollection>();
		if (ref == null)
			ref = "";
		if (processgroup == null)
			processgroup = "";
		if (model==null)
			model="";

		String name = ctx.getCallerPrincipal().getName();

		// construct query
		String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";
		sQuery += " JOIN wi.textItems as a1";
		if (!"".equals(ref))
			sQuery += " JOIN wi.textItems as t2 ";
		if (!"".equals(processgroup))
			sQuery += " JOIN wi.textItems as t3 ";
		if (processid > 0)
			sQuery += " JOIN wi.integerItems as t4 ";
		if (!"".equals(model))
			sQuery += " JOIN wi.textItems AS model ";
		sQuery += " WHERE wi.type = 'workitem'";
		sQuery += " AND a1.itemName = 'namowner' and a1.itemValue = '" + name
				+ "'";
		if (!"".equals(ref))
			sQuery += " AND t2.itemName = '$uniqueidref' and t2.itemValue = '"
					+ ref + "' ";

		if (!"".equals(model))
			sQuery += " AND model.itemName = '$modelversion' AND model.itemValue ='"
					+ model + "'";

		if (!"".equals(processgroup))
			sQuery += " AND t3.itemName = 'txtworkflowgroup' and t3.itemValue = '"
					+ processgroup + "' ";
		// Process ID
		if (processid > 0)
			sQuery += " AND t4.itemName = '$processid' AND t4.itemValue ='"
					+ processid + "'";

		// creade ORDER BY phrase
		sQuery += " ORDER BY wi.";
		if (sortby == WorklistService.SORT_BY_CREATED)
			sQuery += "created ";
		else
			sQuery += "modified ";
		if (sortorder == WorklistService.SORT_ORDER_DESC)
			sQuery += "desc";
		else
			sQuery += "asc";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		teamList.addAll(col);

		return teamList;

	}

	/**
	 * Returns a collection representing the worklist for the current user
	 * 
	 * @param model
	 *            - an optional model version to filter workitems
	 * @param processgroup
	 *            - an optional processgroup to filter workitems
	 * @param processid
	 *            - an optional processID to filter workitems
	 * @param row
	 *            - start position
	 * @param count
	 *            - max count of selected worktiems
	 * @return list of workitems 
	 */
	public List<ItemCollection> findWorkitemsByAuthor(String ref, String model,
			String processgroup, int processid, int row, int count, int sortby,
			int sortorder) {
		ArrayList<ItemCollection> teamList = new ArrayList<ItemCollection>();
		if (ref == null)
			ref = "";
		if (processgroup == null)
			processgroup = "";	
		if (model==null)
				model="";


		String name = ctx.getCallerPrincipal().getName();

		// construct query
		String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";
		sQuery += " JOIN wi.writeAccessList as a1";
		if (!"".equals(ref))
			sQuery += " JOIN wi.textItems as t2 ";
		if (!"".equals(processgroup))
			sQuery += " JOIN wi.textItems as t3 ";
		if (processid > 0)
			sQuery += " JOIN wi.integerItems as t4 ";
		if (!"".equals(model))
			sQuery += " JOIN wi.textItems AS model ";
		sQuery += " WHERE wi.type = 'workitem'";
		sQuery += " AND a1.value = '" + name + "'";
		if (!"".equals(ref))
			sQuery += " AND t2.itemName = '$uniqueidref' and t2.itemValue = '"
					+ ref + "' ";
		if (!"".equals(model))
			sQuery += " AND model.itemName = '$modelversion' AND model.itemValue ='"
					+ model + "'";

		if (!"".equals(processgroup))
			sQuery += " AND t3.itemName = 'txtworkflowgroup' and t3.itemValue = '"
					+ processgroup + "' ";
		// Process ID
		if (processid > 0)
			sQuery += " AND t4.itemName = '$processid' AND t4.itemValue ='"
					+ processid + "'";

		// creade ORDER BY phrase
		sQuery += " ORDER BY wi.";
		if (sortby == WorklistService.SORT_BY_CREATED)
			sQuery += "created ";
		else
			sQuery += "modified ";
		if (sortorder == WorklistService.SORT_ORDER_DESC)
			sQuery += "desc";
		else
			sQuery += "asc";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		teamList.addAll(col);

		return teamList;

	}

	/**
	 * Returns a collection representing the worklist for the current user
	 * 
	 * @param model
	 *            - an optional model version to filter workitems
	 * @param processgroup
	 *            - an optional processgroup to filter workitems
	 * @param processid
	 *            - an optional processID to filter workitems
	 * @param row
	 *            - start position
	 * @param count
	 *            - max count of selected worktiems
	 * @return list of workitems 
	 */
	public List<ItemCollection> findWorkitemsByCreator(String ref,
			String model, String processgroup, int processid, int row,
			int count, int sortby, int sortorder) {
		ArrayList<ItemCollection> teamList = new ArrayList<ItemCollection>();
		if (ref == null)
			ref = "";
		if (processgroup == null)
			processgroup = "";
		if (model==null)
			model="";

		String name = ctx.getCallerPrincipal().getName();

		// construct query
		String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";
		sQuery += " JOIN wi.textItems as a1";
		if (!"".equals(ref))
			sQuery += " JOIN wi.textItems as t2 ";
		if (!"".equals(processgroup))
			sQuery += " JOIN wi.textItems as t3 ";
		if (processid > 0)
			sQuery += " JOIN wi.integerItems as t4 ";
		if (!"".equals(model))
			sQuery += " JOIN wi.textItems AS model ";
		sQuery += " WHERE wi.type = 'workitem'";
		sQuery += " AND a1.itemName = 'namcreator' and a1.itemValue = '" + name
				+ "'";
		if (!"".equals(ref))
			sQuery += " AND t2.itemName = '$uniqueidref' and t2.itemValue = '"
					+ ref + "' ";
		if (!"".equals(model))
			sQuery += " AND model.itemName = '$modelversion' AND model.itemValue ='"
					+ model + "'";

		if (!"".equals(processgroup))
			sQuery += " AND t3.itemName = 'txtworkflowgroup' and t3.itemValue = '"
					+ processgroup + "' ";
		// Process ID
		if (processid > 0)
			sQuery += " AND t4.itemName = '$processid' AND t4.itemValue ='"
					+ processid + "'";

		// creade ORDER BY phrase
		sQuery += " ORDER BY wi.";
		if (sortby == WorklistService.SORT_BY_CREATED)
			sQuery += "created ";
		else
			sQuery += "modified ";
		if (sortorder == WorklistService.SORT_ORDER_DESC)
			sQuery += "desc";
		else
			sQuery += "asc";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		teamList.addAll(col);

		return teamList;

	}

	/**
	 * Returns a collection of all Workitems independent of the current user
	 * name! The ref defines an optional project or parentworkitem reference
	 * where the workitems belongs to. If not Ref is defined the method returns
	 * only workitems from type='workitem'. In other cases the method also
	 * returns workitems from type='childworkitem'. So the method can be used to
	 * select childprocesses inside a form.
	 * 
	 * @param model
	 *            - an optional model version to filter workitems
	 * @param processgroup
	 *            - an optional processgroup to filter workitems
	 * @param processid
	 *            - an optional processID to filter workitems
	 * @param row
	 *            - start position
	 * @param count
	 *            - max count of selected workitems
	 * @return list of workitems 
	 */
	public List<ItemCollection> findAllWorkitems(String ref, String model,
			String processgroup, int processid, int row, int count, int sortby,
			int sortorder) {
		ArrayList<ItemCollection> teamList = new ArrayList<ItemCollection>();
		if (ref == null)
			ref = "";
		if (processgroup == null)
			processgroup = "";
		if (model==null)
			model="";

		// construct query
		String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";
		if (!"".equals(ref))
			sQuery += " JOIN wi.textItems as t2 ";
		if (!"".equals(processgroup))
			sQuery += " JOIN wi.textItems as t3 ";
		if (processid > 0)
			sQuery += " JOIN wi.integerItems as t4 ";
		if (!"".equals(model))
			sQuery += " JOIN wi.textItems AS model ";

		// restrict type depending of a supporte ref id
		if (!"".equals(ref))
			sQuery += " WHERE wi.type IN ('workitem','childworkitem') ";
		else
			sQuery += " WHERE wi.type IN ('workitem') ";

		if (!"".equals(ref))
			sQuery += " AND t2.itemName = '$uniqueidref' and t2.itemValue = '"
					+ ref + "' ";
		if (!"".equals(model))
			sQuery += " AND model.itemName = '$modelversion' AND model.itemValue ='"
					+ model + "'";

		if (!"".equals(processgroup))
			sQuery += " AND t3.itemName = 'txtworkflowgroup' and t3.itemValue = '"
					+ processgroup + "' ";
		// Process ID
		if (processid > 0)
			sQuery += " AND t4.itemName = '$processid' AND t4.itemValue ='"
					+ processid + "'";

		// creade ORDER BY phrase
		sQuery += " ORDER BY wi.";
		if (sortby == WorklistService.SORT_BY_CREATED)
			sQuery += "created ";
		else
			sQuery += "modified ";
		if (sortorder == WorklistService.SORT_ORDER_DESC)
			sQuery += "desc";
		else
			sQuery += "asc";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		teamList.addAll(col);

		return teamList;
	}

	/**
	 * Returns a collection of all Woritems independent of the current user
	 * name!
	 * 
	 * @param model
	 *            - an optional model version to filter workitems
	 * @param processgroup
	 *            - an optional processgroup to filter workitems
	 * @param processid
	 *            - an optional processID to filter workitems
	 * @param row
	 *            - start position
	 * @param count
	 *            - max count of selected worktiems
	 * @return list of workitems 
	 */
	public List<ItemCollection> findArchive(String project, String model,
			String processgroup, int processid, int row, int count, int sortby,
			int sortorder) {
		ArrayList<ItemCollection> teamList = new ArrayList<ItemCollection>();
		if (project == null)
			project = "";
		if (processgroup == null)
			processgroup = "";
		if (model==null)
			model="";

		// construct query
		String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";
		if (!"".equals(project))
			sQuery += " JOIN wi.textItems as t2 ";
		if (!"".equals(processgroup))
			sQuery += " JOIN wi.textItems as t3 ";
		if (processid > 0)
			sQuery += " JOIN wi.integerItems as t4 ";
		if (!"".equals(model))
			sQuery += " JOIN wi.textItems AS model ";
		sQuery += " WHERE wi.type = 'workitemarchive'";
		if (!"".equals(project))
			sQuery += " AND t2.itemName = '$uniqueidref' and t2.itemValue = '"
					+ project + "' ";
		if (!"".equals(model))
			sQuery += " AND model.itemName = '$modelversion' AND model.itemValue ='"
					+ model + "'";

		if (!"".equals(processgroup))
			sQuery += " AND t3.itemName = 'txtworkflowgroup' and t3.itemValue = '"
					+ processgroup + "' ";
		// Process ID
		if (processid > 0)
			sQuery += " AND t4.itemName = '$processid' AND t4.itemValue ='"
					+ processid + "'";

		// creade ORDER BY phrase
		sQuery += " ORDER BY wi.";
		if (sortby == WorklistService.SORT_BY_CREATED)
			sQuery += "created ";
		else
			sQuery += "modified ";
		if (sortorder == WorklistService.SORT_ORDER_DESC)
			sQuery += "desc";
		else
			sQuery += "asc";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		teamList.addAll(col);

		return teamList;
	}

	/**
	 * Returns a collection of all Woritems independent of the current user
	 * name!
	 * 
	 * @param model
	 *            - an optional model version to filter workitems
	 * @param processgroup
	 *            - an optional processgroup to filter workitems
	 * @param processid
	 *            - an optional processID to filter workitems
	 * @param row
	 *            - start position
	 * @param count
	 *            - max count of selected worktiems
	 * @return list of workitems 
	 */
	public List<ItemCollection> findDeletions(String ref, String model,
			String processgroup, int processid, int row, int count, int sortby,
			int sortorder) {
		ArrayList<ItemCollection> teamList = new ArrayList<ItemCollection>();
		if (ref == null)
			ref = "";
		if (processgroup == null)
			processgroup = "";
		if (model==null)
			model="";

		// construct query
		String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";
		if (!"".equals(ref))
			sQuery += " JOIN wi.textItems as t2 ";
		if (!"".equals(processgroup))
			sQuery += " JOIN wi.textItems as t3 ";
		if (processid > 0)
			sQuery += " JOIN wi.integerItems as t4 ";
		if (!"".equals(model))
			sQuery += " JOIN wi.textItems AS model ";
		sQuery += " WHERE wi.type = 'workitemdeleted'";
		if (!"".equals(ref))
			sQuery += " AND t2.itemName = '$uniqueidref' and t2.itemValue = '"
					+ ref + "' ";
		if (!"".equals(model))
			sQuery += " AND model.itemName = '$modelversion' AND model.itemValue ='"
					+ model + "'";

		if (!"".equals(processgroup))
			sQuery += " AND t3.itemName = 'txtworkflowgroup' and t3.itemValue = '"
					+ processgroup + "' ";
		// Process ID
		if (processid > 0)
			sQuery += " AND t4.itemName = '$processid' AND t4.itemValue ='"
					+ processid + "'";

		// creade ORDER BY phrase
		sQuery += " ORDER BY wi.";
		if (sortby == WorklistService.SORT_BY_CREATED)
			sQuery += "created ";
		else
			sQuery += "modified ";
		if (sortorder == WorklistService.SORT_ORDER_DESC)
			sQuery += "desc";
		else
			sQuery += "asc";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		teamList.addAll(col);

		return teamList;
	}

	
	

}
