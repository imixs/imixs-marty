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

package org.imixs.marty.workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.config.SetupMB;
import org.imixs.marty.profile.UserController;
import org.imixs.workflow.ItemCollection;

/**
 * The ViewController is a generic implementation for all types of views for
 * workitems. A view can be filtered by WorkflowGroup (String), by ProcessID
 * (Integer) and by Project (String). Alternatively the viewController provides
 * a custom searchFilter.
 * 
 * @author rsoika
 * 
 */
@Named("viewController")
@SessionScoped
public class ViewController implements Serializable {

	private static final long serialVersionUID = 1L;

	private ArrayList<ItemCollection> workitems = null;

	// view options
	private int count = 0;
	private int row = 0;
	private int sortby = -1;
	private int sortorder = -1;
	private boolean endOfList = false;
	private String viewTitle = "undefined viewtitle";

	// filter options
	private String projectUniqueID = "";
	private String workflowGroup = "";
	private int processID = 0;
	private String searchQuery = null;

	// view types
	private int queryType = -1;
	final int QUERY_WORKLIST_BY_OWNER = 0;
	final int QUERY_WORKLIST_BY_CREATOR = 1;
	final int QUERY_WORKLIST_BY_AUTHOR = 2;
	final int QUERY_WORKLIST_ALL = 3;
	final int QUERY_WORKLIST_ARCHIVE = 4;
	final int QUERY_WORKLIST_DELETIONS = 7;
	final int QUERY_SEARCH = 5;

	@EJB
	org.imixs.marty.ejb.WorklistService worklistService;

	@Inject
	UserController myProfileMB = null;

	@Inject
	private SetupMB setupMB = null;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	public ViewController() {
		super();
		// empty constructor
	}

	/**
	 * This method initializes the view type an view settings like sort order
	 * and max entries per page. These properties can be set through the
	 * BeanProperties in the faces-config.xml or controlled by the config
	 * entity.
	 */
	@PostConstruct
	public void init() {

		// set default view if not set
		if (queryType == -1)
			setQueryType(setupMB.getWorkitem().getItemValueInteger(
					"defaultworklistview"));

		// read configurations for the max count. This value can be also set via
		// faces-config-custom.xml
		if (count == 0)
			count = setupMB.getWorkitem().getItemValueInteger(
					"Maxviewentriesperpage");
		// read configuration for the sort order
		if (sortby == -1)
			sortby = setupMB.getWorkitem().getItemValueInteger("Sortby");
		if (sortorder == -1)
			sortorder = setupMB.getWorkitem().getItemValueInteger("Sortorder");
	}

	public SetupMB getSetupMB() {
		return setupMB;
	}

	public void setSetupMB(SetupMB setupMB) {
		this.setupMB = setupMB;
	}

	public UserController getMyProfileMB() {
		return myProfileMB;
	}

	public void setMyProfileMB(UserController myProfileMB) {
		this.myProfileMB = myProfileMB;
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

	public void setSearchQuery(String searchQuery) {
		this.searchQuery = searchQuery;
	}

	public String getProjectUniqueID() {
		return projectUniqueID;
	}

	public void setProjectUniqueID(String projectUniqueID) {
		this.projectUniqueID = projectUniqueID;
	}

	public String getWorkflowGroup() {
		return workflowGroup;
	}

	public void setWorkflowGroup(String workflowGroup) {
		this.workflowGroup = workflowGroup;
	}

	public int getProcessID() {
		return processID;
	}

	public void setProcessID(int processID) {
		this.processID = processID;
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
	}

	public String getViewTitle() {
		return viewTitle;
	}

	public void setViewTitle(String viewTitle) {
		if (myProfileMB == null)
			return;

		Locale locale = new Locale(myProfileMB.getLocale());
		ResourceBundle rb = null;
		rb = ResourceBundle.getBundle("bundle.workitem", locale);

		this.viewTitle = rb.getString(viewTitle);

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
			String sProjectUniqueID = getProjectUniqueID();

			if (this.getWorkflowGroup().indexOf('|') > -1) {
				sModel = this.getWorkflowGroup().substring(0,
						this.getWorkflowGroup().indexOf("|") - 0);
				sWorkflowGroup = this.getWorkflowGroup().substring(
						this.getWorkflowGroup().indexOf("|") + 1);
			}

			switch (getQueryType()) {
			case QUERY_WORKLIST_BY_OWNER:
				col = worklistService.findWorkitemsByOwner(sProjectUniqueID,
						sModel, sWorkflowGroup, this.getProcessID(), row,
						count, sortby, sortorder);
				break;
			case QUERY_WORKLIST_BY_CREATOR:
				col = worklistService.findWorkitemsByCreator(sProjectUniqueID,
						sModel, sWorkflowGroup, this.getProcessID(), row,
						count, sortby, sortorder);
				break;

			case QUERY_WORKLIST_BY_AUTHOR:
				col = worklistService.findWorkitemsByAuthor(sProjectUniqueID,
						sModel, sWorkflowGroup, this.getProcessID(), row,
						count, sortby, sortorder);
				break;

			case QUERY_WORKLIST_ALL:
				col = worklistService.findAllWorkitems(sProjectUniqueID,
						sModel, sWorkflowGroup, this.getProcessID(), row,
						count, sortby, sortorder);
				break;

			case QUERY_WORKLIST_ARCHIVE:
				col = worklistService.findArchive(sProjectUniqueID, sModel,
						sWorkflowGroup, this.getProcessID(), row, count,
						sortby, sortorder);
				break;

			case QUERY_WORKLIST_DELETIONS:
				col = worklistService.findDeletions(sProjectUniqueID, sModel,
						sWorkflowGroup, this.getProcessID(), row, count,
						sortby, sortorder);
				break;

			case QUERY_SEARCH:
				if (searchQuery != null || "".equals(searchQuery))
					col = this.worklistService.findWorkitemsByQuery(
							searchQuery, row, count);
				else
					// return empty result if no filter is defined!
					col = new Vector<ItemCollection>();
				break;

			default:
				col = worklistService.findAllWorkitems(sProjectUniqueID,
						sModel, sWorkflowGroup, this.getProcessID(), row,
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

}
