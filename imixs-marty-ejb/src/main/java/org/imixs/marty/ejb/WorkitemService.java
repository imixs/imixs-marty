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
import org.imixs.workflow.plugins.jee.extended.LucenePlugin;

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
public class WorkitemService {

	public final static int SORT_BY_CREATED = 0;
	public final static int SORT_BY_MODIFIED = 1;
	public final static int SORT_ORDER_DESC = 0;
	public final static int SORT_ORDER_ASC = 1;

	// Persistence Manager
	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	// Workflow Manager
	@EJB
	org.imixs.workflow.jee.ejb.WorkflowService wm;

	// ModelServcie
	@EJB
	org.imixs.workflow.jee.ejb.ModelService modelService;

	// ProjectService
	@EJB
	ProjectService projectService;

	@Resource
	SessionContext ctx;

	ItemCollection workItem = null;


	/**
	 * This method creates a new workItem. The workItem becomes a response
	 * object to the provided parent ItemCollection. The workItem will also be
	 * assigned to the ProcessEntity specified by the provided modelVersion and
	 * ProcessID. The method throws an exception if the ProcessEntity did not
	 * exist in the model.
	 * 
	 * The Method creates an new property 'txtProjectName' which holds the
	 * property 'txtName' from the parent WOrkitem. This is normally the project
	 * name where the workItem is assigned to. But in cases where the WorkItem
	 * is created as a subprocess to another workItem the property can be mapped
	 * to an individual value provided by the Parent workItem.
	 * 
	 * The Attributes txtWorkflowEditor, numProcessID, $modelVersion and
	 * txtWrofklwoGroup will be set to the corresponding values of processEntity
	 * 
	 * @param parent
	 *            ItemCollection representing the parent where the workItem is
	 *            assigned to. This is typical a project entity but can also be
	 *            an other workItem
	 * @param processEntity
	 *            ItemCollection representing the ProcessEntity where the
	 *            workItem is assigned to
	 */
	public ItemCollection createWorkItem(ItemCollection parent,
			String sProcessModelVersion, int aProcessID) throws Exception {

		// lookup ProcessEntiy from the model
		ItemCollection processEntity = modelService.getProcessEntityByVersion(
				aProcessID, sProcessModelVersion);
		if (processEntity == null)
			throw new Exception(
					"error createWorkItem: Process Entity can not be found ("
							+ sProcessModelVersion + "|" + aProcessID + ")");

		String sEditorID = processEntity.getItemValueString("txteditorid");
		if ("".equals(sEditorID))
			sEditorID = "default";
		int processID = processEntity.getItemValueInteger("numProcessID");
		String sModelVersion = processEntity
				.getItemValueString("$modelversion");
		String sWorkflowGroup = processEntity
				.getItemValueString("txtworkflowgroup");

		// create empty workitem
		workItem = new ItemCollection();
		workItem.replaceItemValue("$processID", processID);

		// assign project name and reference
		workItem.replaceItemValue("$uniqueidRef",
				parent.getItemValueString("$uniqueid"));
		workItem.replaceItemValue("txtProjectName",
				parent.getItemValueString("txtname"));

		// assign ModelVersion, group and editor
		workItem.replaceItemValue("$modelversion", sModelVersion);
		workItem.replaceItemValue("txtworkflowgroup", sWorkflowGroup);
		workItem.replaceItemValue("txtworkfloweditorid", sEditorID);
		
		
		
		// set $WriteAccess with namCreator!
		workItem.replaceItemValue("$WriteAccess", ctx.getCallerPrincipal().getName());
		

		return workItem;

	}

	/**
	 * This method changes the ProeccEntity assigned to the Workitem.
	 * 
	 * The Attributes txtEditor, numProcessID, $modelVersion and
	 * txtWrofklwoGroup will be set to the corresponding values of processEntity
	 * 
	 * 
	 * @param processEntity
	 *            ItemCollection representing the ProcessEntity where the
	 *            workitem is assigned to
	 */
	public ItemCollection changeProcess(ItemCollection aworkitem,
			ItemCollection processEntity) throws Exception {

		workItem = aworkitem;
		String sEditorID = processEntity.getItemValueString("txteditorid");
		if ("".equals(sEditorID))
			sEditorID = "default";
		int processID = processEntity.getItemValueInteger("numProcessID");
		String sModelVersion = processEntity
				.getItemValueString("$modelversion");
		String sWorkflowGroup = processEntity
				.getItemValueString("txtworkflowgroup");

		// assigen ModelVersion, group and editor
		workItem.replaceItemValue("$ProcessID", processID);
		workItem.replaceItemValue("$modelversion", sModelVersion);
		workItem.replaceItemValue("txtworkflowgroup", sWorkflowGroup);
		workItem.replaceItemValue("txtworkfloweditorid", sEditorID);

		return workItem;
	}

	/**
	 * Processes a Workitem. The method updates the type attribute. If the
	 * Workitem is attached to a project then the type will be set to
	 * 'workitem'. If the woritem is a child process form another workitem then
	 * teh type will be set to 'childworkitem'
	 * 
	 * 
	 * Also the Method test the workflow property txtWorkflowResultMessage for
	 * the String "parent=". If such a string element is found the method tries
	 * to update the project reference for the workitem to the new project with
	 * the name provided in the txtWorkflowResultMessage (e.g.
	 * ...parent=purchase). The project reference will be only updated if a
	 * project with the name provided by the model is provided
	 * 
	 * Optional SywApp Plugins are managing the team lists and project name
	 * properties.
	 * 
	 * @see SywappApplicationPlugin, SywappTeamPlugin
	 * 
	 * 
	 */
	public ItemCollection processWorkItem(ItemCollection aworkitem)
			throws Exception {
		String sResult = null;
		String sDefaultType = "workitem";

		if (validateIssue(aworkitem) == false)
			throw new Exception(
					"WorkitemServiceBean - invalid object! no $uniqueidref or $process property found");

		workItem = aworkitem;
		// test the if the workitem is a child process
		// typcially a workitem is a child to a project
		String sUniqueidRef = workItem.getItemValueString("$uniqueidref");
		if (!"".equals(sUniqueidRef)) {
			// test parent type
			try {
				ItemCollection aParentWorkitem = entityService
						.load(sUniqueidRef);
				if (aParentWorkitem != null) {
					String parenttype = aParentWorkitem
							.getItemValueString("type");
					if ("workitem".equals(parenttype))
						// type becomes a child
						sDefaultType = "childworkitem";
				}
			} catch (Exception esearch) {
				sDefaultType = "workitem";
			}
		}

		// replace type only if no type was still set!
		if ("".equals(workItem.getItemValueString("type")))
			workItem.replaceItemValue("type", sDefaultType);

		// Process workitem...
		workItem = wm.processWorkItem(workItem);

		return workItem;
	}

	/**
	 * This method delete a workitem. The method checks for child processes and
	 * runs a recursive deletion ...
	 */
	public void deleteWorkItem(ItemCollection workitem) throws Exception {
		String id = workitem.getItemValueString("$uniqueid");
		Collection<ItemCollection> col = wm.getWorkListByRef(id);
		for (ItemCollection achildworkitem : col) {
			// recursive method call
			deleteWorkItem(achildworkitem);
		}
		entityService.remove(workitem);
	}

	public ItemCollection moveIntoArchive(ItemCollection workitem)
			throws Exception {
		if ("workitem".equals(workitem.getItemValueString("type"))) {

			String id = workitem.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = wm.getWorkListByRef(id);
			for (ItemCollection achildworkitem : col) {
				// recursive method call
				moveIntoArchive(achildworkitem);
			}
			workitem.replaceItemValue("type",
					workitem.getItemValueString("type") + "archive");

			workitem = entityService.save(workitem);
			
			// update search index
			try {
				LucenePlugin.addWorkitem(workitem);
			} catch (Exception e) {
				// no op
			}
		}

		return workitem;
	}

	public ItemCollection restoreFromArchive(ItemCollection workitem)
			throws Exception {

		if ("workitemarchive".equals(workitem.getItemValueString("type"))
				|| "childworkitemarchive".equals(workitem
						.getItemValueString("type"))) {

			String id = workitem.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = wm.getWorkListByRef(id);
			for (ItemCollection achildworkitem : col) {
				// recursive method call
				restoreFromArchive(achildworkitem);
			}
			if ("workitemarchive".equals(workitem.getItemValueString("type")))
				workitem.replaceItemValue("type", "workitem");
			if ("childworkitemarchive".equals(workitem
					.getItemValueString("type")))
				workitem.replaceItemValue("type", "childworkitem");
			workitem = entityService.save(workitem);
			
			// update search index
			try {
				LucenePlugin.addWorkitem(workitem);
			} catch (Exception e) {
				// no op
			}
		}
		return workitem;
	}

	/**
	 * performs a soft delete
	 * 
	 */
	public ItemCollection moveIntoDeletions(ItemCollection workitem)
			throws Exception {
		if ("workitem".equals(workitem.getItemValueString("type"))
				|| "childworkitem".equals(workitem.getItemValueString("type"))) {

			String id = workitem.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = wm.getWorkListByRef(id);
			for (ItemCollection achildworkitem : col) {
				// recursive method call
				moveIntoDeletions(achildworkitem);
			}
			workitem.replaceItemValue("type",
					workitem.getItemValueString("type") + "deleted");
			workitem = entityService.save(workitem);
			
			// update search index
			try {
				LucenePlugin.addWorkitem(workitem);
			} catch (Exception e) {
				// no op
			}
		}

		return workitem;
	}

	public ItemCollection restoreFromDeletions(ItemCollection workitem)
			throws Exception {

		if ("workitemdeleted".equals(workitem.getItemValueString("type"))
				|| "childworkitemdeleted".equals(workitem
						.getItemValueString("type"))) {

			String id = workitem.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = wm.getWorkListByRef(id);
			for (ItemCollection achildworkitem : col) {
				// recursive method call
				restoreFromDeletions(achildworkitem);
			}
			if ("workitemdeleted".equals(workitem.getItemValueString("type")))
				workitem.replaceItemValue("type", "workitem");
			if ("childworkitemdeleted".equals(workitem
					.getItemValueString("type")))
				workitem.replaceItemValue("type", "childworkitem");
			workitem = entityService.save(workitem);
			

			// update search index
			try {
				LucenePlugin.addWorkitem(workitem);
			} catch (Exception e) {
				// no op
				
			}
		}

		return workitem;
	}

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
		if (sortby == WorkitemService.SORT_BY_CREATED)
			sQuery += "created ";
		else
			sQuery += "modified ";
		if (sortorder == WorkitemService.SORT_ORDER_DESC)
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
		if (sortby == WorkitemService.SORT_BY_CREATED)
			sQuery += "created ";
		else
			sQuery += "modified ";
		if (sortorder == WorkitemService.SORT_ORDER_DESC)
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
		if (sortby == WorkitemService.SORT_BY_CREATED)
			sQuery += "created ";
		else
			sQuery += "modified ";
		if (sortorder == WorkitemService.SORT_ORDER_DESC)
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
		if (sortby == WorkitemService.SORT_BY_CREATED)
			sQuery += "created ";
		else
			sQuery += "modified ";
		if (sortorder == WorkitemService.SORT_ORDER_DESC)
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
		if (sortby == WorkitemService.SORT_BY_CREATED)
			sQuery += "created ";
		else
			sQuery += "modified ";
		if (sortorder == WorkitemService.SORT_ORDER_DESC)
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
		if (sortby == WorkitemService.SORT_BY_CREATED)
			sQuery += "created ";
		else
			sQuery += "modified ";
		if (sortorder == WorkitemService.SORT_ORDER_DESC)
			sQuery += "desc";
		else
			sQuery += "asc";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		teamList.addAll(col);

		return teamList;
	}

	public ItemCollection findWorkItem(String id) {
		return entityService.load(id);
	}

	/**
	 * This method validates if the attributes supported to a map are
	 * corresponding to the team structure
	 */
	private boolean validateIssue(ItemCollection aWorkitem) {

		try {
			if (!aWorkitem.hasItem("$processid"))
				return false;
			// test Project reference
			if (!aWorkitem.hasItem("$uniqueIDref"))
				return false;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
