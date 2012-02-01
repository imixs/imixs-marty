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

package org.imixs.marty.web.workitem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.imixs.marty.util.LoginMB;
import org.imixs.marty.web.project.ProjectMB;
import org.imixs.marty.web.util.SetupMB;
import org.imixs.workflow.ItemCollection;

public class WorklistMB implements WorkitemListener {

	ItemCollection currentProjectSelection = null; // current selected project

	private ArrayList<ItemCollection> workitems = null;

	// view options
	private int count = 0;
	private int row = 0;
	private int sortby = -1;
	private int sortorder = -1;
	private boolean endOfList = false;
	private String viewTitle = "undefined viewtitle";

	// filter options
	private String projectFilter="";
	private String workflowGroupFilter = "";
	private int processFilter = 0;
	

	// view types
	private int queryType = -1;
	final int QUERY_WORKLIST_BY_OWNER = 0;
	final int QUERY_WORKLIST_BY_CREATOR = 1;
	final int QUERY_WORKLIST_BY_AUTHOR = 2;
	final int QUERY_WORKLIST_ALL = 3;
	final int QUERY_WORKLIST_ARCHIVE = 4;
	final int QUERY_WORKLIST_DELETIONS = 7;
	final int QUERY_SEARCH = 5;
	private String searchQuery = null;

	java.util.EventObject eo;
	@EJB
	org.imixs.marty.business.WorkitemService workitemService;

	/* Backing Beans */
	private WorkitemMB workitemBean = null;
	private ProjectMB projectBean = null;
	private SetupMB setupMB = null;
	private LoginMB loginMB = null;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	/**
	 * This method initializes the view type an view settings like sort order
	 * and max entries per page. These properties can be set through the
	 * BeanProperties in the faces-config.xml or controlled by the config
	 * entity.
	 */
	@PostConstruct
	public void init() {

		// register this Bean as a workitemListener to the current WorktieMB
		this.getWorkitemMB().addWorkitemListener(this);

		// set default view if not set
		if (queryType == -1)
			setQueryType(this.getConfigBean().getWorkitem()
					.getItemValueInteger("defaultworklistview"));

		// read configurations for the max count. This value can be also set via
		// faces-config-custom.xml
		if (count == 0)
			count = this.getConfigBean().getWorkitem()
					.getItemValueInteger("Maxviewentriesperpage");
		// read configuration for the sort order
		if (sortby == -1)
			sortby = this.getConfigBean().getWorkitem()
					.getItemValueInteger("Sortby");
		if (sortorder == -1)
			sortorder = this.getConfigBean().getWorkitem()
					.getItemValueInteger("Sortorder");
	}

	public ProjectMB getProjectMB() {
		if (projectBean == null) {
			projectBean = (ProjectMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "projectMB");
		}

		return projectBean;

	}

	public WorkitemMB getWorkitemMB() {
		if (workitemBean == null) {
			workitemBean = (WorkitemMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "workitemMB");
		}
		return workitemBean;

	}

	private SetupMB getConfigBean() {
		if (setupMB == null)
			setupMB = (SetupMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "setupMB");
		return setupMB;
	}

	private LoginMB getLoginBean() {
		if (loginMB == null)
			loginMB = (LoginMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "loginMB");
		return loginMB;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getSortby() {
		return sortby;
	}

	public void setSortby(int sortby) {
		this.sortby = sortby;
	}

	public int getSortorder() {
		return sortorder;
	}

	public void setSortorder(int sortorder) {
		this.sortorder = sortorder;
	}

	public String getSearchQuery() {
		return searchQuery;
	}

	/**
	 * set a search query to be used for the query-type QUERY_SEARCH
	 * 
	 * @param searchQuery
	 */
	public void setSearchQuery(String searchQuery) {
		this.searchQuery = searchQuery;
	}

	public int getQueryType() {
		return queryType;
	}

	public void setQueryType(int queryType) {
		this.queryType = queryType;
		
		

		switch (queryType) {
		case QUERY_WORKLIST_BY_AUTHOR:
			setViewTitle("title_worklist_by_author");
			break;
		case QUERY_WORKLIST_BY_OWNER:
			setViewTitle("title_worklist_by_owner");
			break;
		case QUERY_WORKLIST_BY_CREATOR:
			setViewTitle("title_worklist_by_creator");
			break;
		case QUERY_WORKLIST_ARCHIVE:
			setViewTitle("title_worklist_archive");
			break;
		case QUERY_WORKLIST_DELETIONS:
			setViewTitle("title_worklist_deletions");
			break;

		default:
			setViewTitle("title_worklist_all");
			break;

		}

		// reset view
//		doReset(null);
		
//		this.setProcessFilter(0);
//		this.setWorkflowGroupFilter(null);
	}

	public String getViewTitle() {
		return viewTitle;
	}

	public void setViewTitle(String viewTitle) {
		Locale locale = new Locale(this.getLoginBean().getLocale());
		ResourceBundle rb = null;
		rb = ResourceBundle.getBundle("bundle.workitem", locale);

		this.viewTitle = rb.getString(viewTitle);

	}

	public String getProjectFilter() {
		return projectFilter;
	}

	public void setProjectFilter(String projectFilter) {
		this.projectFilter = projectFilter;
	}

	public String getWorkflowGroupFilter() {
		if (workflowGroupFilter==null)
			workflowGroupFilter="-";
		return workflowGroupFilter;
	}

	public void setWorkflowGroupFilter(String workflowGroupFilter) {
		this.workflowGroupFilter = workflowGroupFilter;
		// reset process filter now
		setProcessFilter(0);
	}
	
	/**
	 * returns the name of the current workflowGroupFileter (right part of |)
	 * @return
	 */
	public String getWorkflowGroupFilterName() {
		if (workflowGroupFilter==null || !workflowGroupFilter.contains("|"))
			return "";
		else  
			return workflowGroupFilter.substring(workflowGroupFilter.indexOf('|')+1);
	}
	
	

	public int getProcessFilter() {
		if (workflowGroupFilter==null || "-".equals(workflowGroupFilter))
			processFilter=0;
		return processFilter;
	}

	public void setProcessFilter(int processFilter) {
		this.processFilter = processFilter;
		//this.doReset(null);
	}

	/**
	 * returns a SelctItem Array containing all Process Ids for the current
	 * workflowGroupFilter. The workflowGroupFilter property has the format: 'offic-de-1.0.0|Purchase'
	 * 
	 * the method searches only for processIDs in the same ModelVerson and Group
	 * 
	 * 
	 * @return a SelectItem array list with the corresponding ProcessIDs (int)
	 */
	public ArrayList<SelectItem> getProcessFilterSelection() {
		String sWorkflowGroup = null;
		String sModel = null;
		// build new groupSelection
		ArrayList<SelectItem> processSelection = new ArrayList<SelectItem>();

		if (workflowGroupFilter==null || "-".equals(workflowGroupFilter))
			return processSelection;
		
		if (workflowGroupFilter.indexOf('|')>-1) {
			// cut model and version
			sModel = workflowGroupFilter.substring(0,
					workflowGroupFilter.indexOf("|") - 0);
			sWorkflowGroup = workflowGroupFilter.substring(workflowGroupFilter
					.indexOf("|") + 1);

			List<ItemCollection> processList = this.getWorkitemMB()
					.getModelService()
					.getAllProcessEntitiesByGroupByVersion(sWorkflowGroup,sModel);
			for (ItemCollection process : processList) {
				//String sModelVersion = process
				//		.getItemValueString("$modelVersion");
				//if (sModel.equals(sModelVersion)) {
				//	String sValue = sModelVersion + "|"
					//		+ process.getItemValueInteger("numprocessid");
					processSelection.add(new SelectItem(process.getItemValueInteger("numprocessid"), process
							.getItemValueString("txtname")));
				//}
			}

		}
		return processSelection;

	}

	
	/**
	 * loads the current workitem collection based on the selected view-type
	 * 
	 * @return
	 */
	public List<ItemCollection> getWorkitems() {
		if (workitems == null)
			loadWorkItemList();
		return workitems;
	}

	/**
	 * this method is called by the datatables to select a workitem
	 * 
	 * @return
	 */
	public void doEdit(ActionEvent event) throws Exception {
		ItemCollection currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {

			// remove a4j: attributes generated inside the viewentries by the UI
			currentSelection.getAllItems().remove("a4j:showhistory");
			currentSelection.getAllItems().remove("a4j:showdetails");

			getWorkitemMB().setWorkitem(currentSelection);

			// update projectMB if necessary
			getWorkitemMB().updateProjectMB();

		}
	}

	/**
	 * this method toogles the ajax attribute a4j:showDetails This Attriubte is
	 * used to display the WorkitemDetails inside the Worklist View.
	 * 
	 * @return
	 */
	public void doToggleDetails(ActionEvent event) throws Exception {
		ItemCollection currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {

			// now try to read current toogle state and switch state

			boolean bTogle = false;
			if (currentSelection.hasItem("a4j:showDetails")) {
				try {
					boolean bTogleCurrent = (Boolean) currentSelection

					.getItemValue("a4j:showDetails").firstElement();
					bTogle = !bTogleCurrent;
				} catch (Exception e) {
					bTogle = true;
				}
			} else
				// item did not exist yet....
				bTogle = true;
			currentSelection.replaceItemValue("a4j:showDetails", bTogle);

		}
	}

	/**
	 * this method toogles the ajax attribute a4j:showHistory. This Attribute is
	 * used to display the WorkitemHistory inside the Worklist View.
	 * 
	 * @return
	 */
	public void doToggleHistory(ActionEvent event) throws Exception {
		ItemCollection currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {
			// now try to read current toogle state and switch state

			boolean bTogle = false;
			if (currentSelection.hasItem("a4j:showHistory")) {
				try {
					boolean bTogleCurrent = (Boolean) currentSelection

					.getItemValue("a4j:showHistory").firstElement();
					bTogle = !bTogleCurrent;
				} catch (Exception e) {
					bTogle = true;
				}
			} else
				// item did not exist yet....
				bTogle = true;
			currentSelection.replaceItemValue("a4j:showHistory", bTogle);

		}
	}

	/**
	 * resets the current project list and projectMB
	 * 
	 * @return
	 */
	public void doReset(ActionEvent event) {
		workitems = null;
		row = 0;
	}

	/**
	 * refreshes the current workitem list. so the list will be loaded again.
	 * but start pos will not be changed!
	 */
	public void doRefresh(ActionEvent event) {
		workitems = null;
	}

	
	/**
	 * selects the task-list for a specific workflow group
	 * 
	 * @param event - containging the model and workflowGroup
	 */
	public void doSwitchToWorklistByWorkflowGroup(ActionEvent event) {
		ItemCollection currentSelection = null;
		
		// reset current project selection
		this.getProjectMB().setWorkitem(null);
		setProjectFilter(null);
		
			// find current data row....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
						.getParent()) {

			if (!(parent instanceof UIData))
						continue;

					try {
						// get current project from row
						currentSelection = (ItemCollection) ((UIData) parent)
								.getRowData();

						setWorkflowGroupFilter(currentSelection.getItemValueString("txtName"));
						
						break;
					} catch (Exception e) {
						// unable to select data
					}
				}

				
				
		//setWorkflowGroupFilter(workflowGroupFilter)
		
		//setQueryType(QUERY_WORKLIST_BY_AUTHOR);
	}

	
	
	/**
	 * selects the task-list for the current user
	 * 
	 * @param event
	 */
	public void doSwitchToWorklistByAuthor(ActionEvent event) {
		// reset project selection
		this.getProjectMB().setWorkitem(null);
		setProjectFilter(null);
		setQueryType(QUERY_WORKLIST_BY_AUTHOR);
	}

	/**
	 * select all workitems assigned by owner to the current user
	 * 
	 * @param event
	 */
	public void doSwitchToWorklistByOwner(ActionEvent event) {
		// reset project selection
		this.getProjectMB().setWorkitem(null);
		setProjectFilter(null);

		setQueryType(QUERY_WORKLIST_BY_OWNER);
	}

	/**
	 * selects all workitems created by the current user
	 * 
	 * @param event
	 */
	public void doSwitchToWorklistByCreator(ActionEvent event) {
		// reset project selection
		this.getProjectMB().setWorkitem(null);
		setProjectFilter(null);

		setQueryType(QUERY_WORKLIST_BY_CREATOR);
	}

	/**
	 * select the archive list
	 * 
	 * @param event
	 */
	public void doSwitchToWorklistArchive(ActionEvent event) {
		// reset project selection
		this.getProjectMB().setWorkitem(null);
		setProjectFilter(null);

		setQueryType(QUERY_WORKLIST_ARCHIVE);
	}

	/**
	 * select all deleted workitems
	 * 
	 * @param event
	 */
	public void doSwitchToWorklistDeletions(ActionEvent event) {
		// reset project selection
		this.getProjectMB().setWorkitem(null);
		setProjectFilter(null);

		setQueryType(QUERY_WORKLIST_DELETIONS);
	}

	/**
	 * selects all workitems
	 * 
	 * @param event
	 */
	public void doSwitchToWorklistAll(ActionEvent event) {
		// reset project selection
		this.getProjectMB().setWorkitem(null);
		setProjectFilter(null);

		setQueryType(QUERY_WORKLIST_ALL);
	}

	/**
	 * selects workitms based on the current search query.
	 * 
	 * @param event
	 */
	public void doSwitchToWorklistSearch(ActionEvent event) {
		// reset project selection
		this.getProjectMB().setWorkitem(null);
		setProjectFilter(null);

		setQueryType(QUERY_SEARCH);
	}

	/**
	 * selects the task-list for the current user assigned to the current
	 * project
	 * 
	 * @param event
	 */
	public void doSwitchToProjectWorklistByAuthor(ActionEvent event) {
		// switch to project
		this.getProjectMB().getProjectListMB().doSwitchToProject(event);
		
		if (getProjectMB().getWorkitem()!=null)
			setProjectFilter(getProjectMB().getWorkitem().getItemValueString("$uniqueid"));
		else
			setProjectFilter(null);
		
		setQueryType(QUERY_WORKLIST_BY_AUTHOR);
	}

	public void doSwitchToProjectWorklistByOwner(ActionEvent event) {
		// switch to project
		this.getProjectMB().getProjectListMB().doSwitchToProject(event);
		if (getProjectMB().getWorkitem()!=null)
			setProjectFilter(getProjectMB().getWorkitem().getItemValueString("$uniqueid"));
		else
			setProjectFilter(null);

		setQueryType(QUERY_WORKLIST_BY_OWNER);
	}

	public void doSwitchToProjectWorklistByCreator(ActionEvent event) {
		// switch to project
		this.getProjectMB().getProjectListMB().doSwitchToProject(event);
		if (getProjectMB().getWorkitem()!=null)
			setProjectFilter(getProjectMB().getWorkitem().getItemValueString("$uniqueid"));
		else
			setProjectFilter(null);

		setQueryType(QUERY_WORKLIST_BY_CREATOR);
	}

	public void doSwitchToProjectWorklistAll(ActionEvent event) {
		// switch to project
		this.getProjectMB().getProjectListMB().doSwitchToProject(event);
		if (getProjectMB().getWorkitem()!=null)
			setProjectFilter(getProjectMB().getWorkitem().getItemValueString("$uniqueid"));
		else
			setProjectFilter(null);

		setQueryType(QUERY_WORKLIST_ALL);
	}

	/**
	 * moves a workitem into the archive by changing the attribute type to
	 * 'workitemarchive'
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doMoveIntoArchive(ActionEvent event) throws Exception {

		ItemCollection currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {

			// remove a4j: attributes generated inside the viewentries by the UI
			currentSelection.getAllItems().remove("a4j:showhistory");
			currentSelection.getAllItems().remove("a4j:showdetails");

			workitemService.moveIntoArchive(currentSelection);

			this.doRefresh(event);
		}
	}

	/**
	 * moves a workitem into the archive by changing the attribute type to
	 * 'workitemdeleted'
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doMoveIntoDeletions(ActionEvent event) throws Exception {
		ItemCollection currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {

			// remove a4j: attributes generated inside the viewentries by the UI
			currentSelection.getAllItems().remove("a4j:showhistory");
			currentSelection.getAllItems().remove("a4j:showdetails");

			workitemService.moveIntoDeletions(currentSelection);

			this.doRefresh(event);
		}
	}

	/**
	 * restores a workitem from the archive by changing the attribute type to
	 * 'workitemarchive'
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doRestoreFromArchive(ActionEvent event) throws Exception {
		ItemCollection currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {

			// remove a4j: attributes generated inside the viewentries by the UI
			currentSelection.getAllItems().remove("a4j:showhistory");
			currentSelection.getAllItems().remove("a4j:showdetails");

			workitemService.restoreFromArchive(currentSelection);

			this.doRefresh(event);
		}
	}

	/**
	 * restores a workitem from the archive by changing the attribute type to
	 * 'workitemarchive'
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doRestoreFromDeletions(ActionEvent event) throws Exception {
		ItemCollection currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {
			// remove a4j: attributes generated inside the viewentries by the UI
			currentSelection.getAllItems().remove("a4j:showhistory");
			currentSelection.getAllItems().remove("a4j:showdetails");
			workitemService.restoreFromDeletions(currentSelection);

			this.doRefresh(event);
		}
	}

	/**
	 * returns the current selected row as a ItemCollectionAdapter
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	private ItemCollection getCurrentSelection(ActionEvent event)
			throws Exception {

		ItemCollection currentSelection = null;
		// suche selektierte Zeile....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// Zeile gefunden
			currentSelection = (ItemCollection) ((UIData) parent).getRowData();

			return currentSelection;
		}
		return null;
	}

	public void doLoadNext(ActionEvent event) {
		row = row + count;
		workitems = null;
	}

	public void doLoadPrev(ActionEvent event) {
		row = row - count;
		if (row < 0)
			row = 0;
		workitems = null;
	}

	/**
	 * change selection widgets form search page. the new value is be set during
	 * the corresponding setter method. after this the loadIssueList method will
	 * be called.
	 * 
	 * @param v
	 */
	public void onChangeSelection(ValueChangeEvent v) throws Exception {
		workitems = null;
		row = 0;
	}

	/**
	 * this method loads the workitems depending to the current query type The
	 * queryType is set during the getter Methods getWorkList, getStatusList and
	 * getIssueList For each query type a coresponding method is implmented by
	 * the issueService.
	 * 
	 * Caching: ======== The isDirty Flag is diabled because we recognize that
	 * in cases where people working online on share workitems the worklist is
	 * not uptodate. The only way to go back to a caching mechanism would be to
	 * place a refresh-button into the worklist pages.
	 * 
	 * @see org.imixs.WorkitemService.business.WorkitemServiceBean
	 */
	private void loadWorkItemList() {
		workitems = new ArrayList<ItemCollection>();
		Collection<ItemCollection> col = null;
		String sModel = null;
		String sWorkflowGroup = null;
		try {
			long lTime = System.currentTimeMillis();
			String sProjectUniqueID =getProjectFilter();

			if (this.getWorkflowGroupFilter().indexOf('|')>-1) {
				sModel = this.getWorkflowGroupFilter().substring(0,
						this.getWorkflowGroupFilter().indexOf("|") - 0);
				sWorkflowGroup = this.getWorkflowGroupFilter().substring(
						this.getWorkflowGroupFilter().indexOf("|") + 1);
			}

			switch (getQueryType()) {
			case QUERY_WORKLIST_BY_OWNER:
				col = workitemService.findWorkitemsByOwner(sProjectUniqueID,
						sModel, sWorkflowGroup, this.getProcessFilter(), row,
						count, sortby, sortorder);
				break;
			case QUERY_WORKLIST_BY_CREATOR:
				col = workitemService.findWorkitemsByCreator(sProjectUniqueID,
						sModel, sWorkflowGroup, this.getProcessFilter(), row,
						count, sortby, sortorder);
				break;

			case QUERY_WORKLIST_BY_AUTHOR:
				col = workitemService.findWorkitemsByAuthor(sProjectUniqueID,
						sModel, sWorkflowGroup, this.getProcessFilter(), row,
						count, sortby, sortorder);
				break;

			case QUERY_WORKLIST_ALL:
				col = workitemService.findAllWorkitems(sProjectUniqueID,
						sModel, sWorkflowGroup, this.getProcessFilter(), row,
						count, sortby, sortorder);
				break;

			case QUERY_WORKLIST_ARCHIVE:
				col = workitemService.findArchive(sProjectUniqueID, sModel,
						sWorkflowGroup, this.getProcessFilter(), row, count,
						sortby, sortorder);
				break;

			case QUERY_WORKLIST_DELETIONS:
				col = workitemService.findDeletions(sProjectUniqueID, sModel,
						sWorkflowGroup, this.getProcessFilter(), row, count,
						sortby, sortorder);
				break;

			case QUERY_SEARCH:
				if (searchQuery != null || "".equals(searchQuery))
					col = this.workitemService.findWorkitemsByQuery(
							searchQuery, row, count);
				else
					// return empty result if no filter is defined!
					col = new Vector<ItemCollection>();
				break;

			default:
				col = workitemService.findAllWorkitems(sProjectUniqueID,
						sModel, sWorkflowGroup, this.getProcessFilter(), row,
						count, sortby, sortorder);
			}

			lTime = System.currentTimeMillis() - lTime;
			logger.fine("  loadWorkItemList (" + lTime + " ms)");

			endOfList = col.size() < count;
			for (ItemCollection aworkitem : col) {
				workitems.add((aworkitem));
			}
		} catch (Exception ee) {
			workitems = null;
			ee.printStackTrace();
		}

		// reset current workitem for detail views
		getWorkitemMB().setWorkitem(null);
	}

	/***************************************************************************
	 * Navigation
	 */

	public int getRow() {
		return row;
	}

	public boolean isEndOfList() {
		return endOfList;
	}

	/***
	 * Workitem listener methods
	 * 
	 * 
	 */

	public void onWorkitemProcessCompleted(ItemCollection arg0) {

		doReset(null);
	}

	public void onWorkitemCreated(ItemCollection e) {
		// TODO Auto-generated method stub

	}

	public void onWorkitemChanged(ItemCollection e) {
		// TODO Auto-generated method stub

	}

	public void onWorkitemProcess(ItemCollection e) {
		// TODO Auto-generated method stub

	}

	public void onWorkitemDelete(ItemCollection e) {
		// TODO Auto-generated method stub

	}

	public void onWorkitemDeleteCompleted() {
		doReset(null);
	}

	public void onWorkitemSoftDelete(ItemCollection e) {
		// TODO Auto-generated method stub

	}

	public void onWorkitemSoftDeleteCompleted(ItemCollection e) {
		doReset(null);
	}

	public void onChildProcess(ItemCollection e) {
		// TODO Auto-generated method stub

	}

	public void onChildProcessCompleted(ItemCollection e) {
		// TODO Auto-generated method stub

	}

	public void onChildCreated(ItemCollection e) {
		// TODO Auto-generated method stub

	}

	public void onChildDelete(ItemCollection e) {
		// TODO Auto-generated method stub

	}

	public void onChildDeleteCompleted() {
		// TODO Auto-generated method stub

	}

	public void onChildSoftDelete(ItemCollection e) {
		// TODO Auto-generated method stub

	}

	public void onChildSoftDeleteCompleted(ItemCollection e) {
		// TODO Auto-generated method stub

	}

}
