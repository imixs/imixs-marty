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
package org.imixs.marty.web.workitem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.imixs.marty.util.LoginMB;
import org.imixs.marty.web.project.ProjectMB;
import org.imixs.marty.web.util.ConfigMB;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.util.ItemCollectionAdapter;

public class WorklistMB {

	ItemCollection currentProjectSelection = null; // current selected project

	private ArrayList<ItemCollectionAdapter> workitems = null;
	private int count = 10;
	private int row = 0;
	private int sortby=0;
	private int sortorder=0;
	private boolean endOfList = false;
	private String viewTitle = "undefined viewtitle";
	private String processFilter = "";

	private int queryType = 0;
	final int QUERY_WORKLIST_BY_OWNER = 0;
	final int QUERY_WORKLIST_BY_CREATOR = 1;
	final int QUERY_WORKLIST_BY_AUTHOR = 2;
	final int QUERY_WORKLIST_ALL = 3;
	final int QUERY_WORKLIST_ARCHIVE = 4;
	final int QUERY_WORKLIST_DELETIONS = 7;
	final int QUERY_SEARCH = 5;
	private String searchQuery=null;

	java.util.EventObject eo;
	@EJB
	org.imixs.sywapps.business.WorkitemService workitemService;

	/* Backing Beans */
	private WorkitemMB workitemBean = null;
	private ProjectMB projectBean = null;
	private ConfigMB configMB = null;
	private LoginMB loginMB = null;
	
	
	@PostConstruct
	public void init() {
		
		// set default view
		this.doSwitchToWorklistAll(null);
	
		
		// read configurations for count and sort order
		count=this.getConfigBean().getWorkitem().getItemValueInteger("Maxviewentriesperpage");
		
		sortby=this.getConfigBean().getWorkitem().getItemValueInteger("Sortby");
		sortorder=this.getConfigBean().getWorkitem().getItemValueInteger("Sortorder");
	}

	
	public ProjectMB getProjectMB() {
		if (projectBean==null) {
		projectBean = (ProjectMB) FacesContext.getCurrentInstance()
		.getApplication().getELResolver().getValue(
				FacesContext.getCurrentInstance().getELContext(), null,
				"projectMB");
		}
		
		return projectBean;

	}
	
	public WorkitemMB getWorkitemMB() {
		if (workitemBean==null) {
			workitemBean = (WorkitemMB) FacesContext.getCurrentInstance()
			.getApplication().getELResolver().getValue(
					FacesContext.getCurrentInstance().getELContext(), null,
					"workitemMB");
		}
		return workitemBean;

	}

	private ConfigMB getConfigBean() {
		if (configMB == null)
			configMB = (ConfigMB) FacesContext.getCurrentInstance()
					.getApplication().getELResolver().getValue(
							FacesContext.getCurrentInstance().getELContext(),
							null, "configMB");
		return configMB;
	}
	
	
	private LoginMB getLoginBean() {
		if (loginMB == null)
			loginMB = (LoginMB) FacesContext.getCurrentInstance()
					.getApplication().getELResolver().getValue(
							FacesContext.getCurrentInstance().getELContext(),
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
	 * @param searchQuery
	 */
	public void setSearchQuery(String searchQuery) {
		this.searchQuery = searchQuery;
	}







	public String getViewTitle() {
		return viewTitle;
	}

	public void setViewTitle(String viewTitle) {
		Locale locale = new Locale(this.getLoginBean().getLocale());
		ResourceBundle rb = null;
		if (locale != null)
			rb = ResourceBundle.getBundle("bundle.workitem", locale);
		else
			rb = ResourceBundle.getBundle("bundle.workitem");

		this.viewTitle = rb.getString(viewTitle);

	}

	public String getProcessFilter() {
		return processFilter;
	}

	public void setProcessFilter(String processFilter) {
		this.processFilter = processFilter;
	}

	public List<ItemCollectionAdapter> getWorkitems() {
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
		ItemCollectionAdapter currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {

			// remove a4j: attributes generated inside the viewentries by the UI
			currentSelection.getItemCollection().getAllItems().remove(
					"a4j:showhistory");
			currentSelection.getItemCollection().getAllItems().remove(
					"a4j:showdetails");

			getWorkitemMB().setWorkitem(currentSelection.getItemCollection());

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
		ItemCollectionAdapter currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {

			// now try to read current toogle state and switch state

			boolean bTogle = false;
			if (currentSelection.getItemCollection().hasItem("a4j:showDetails")) {
				try {
					boolean bTogleCurrent = (Boolean) currentSelection
							.getItemCollection()
							.getItemValue("a4j:showDetails").firstElement();
					bTogle = !bTogleCurrent;
				} catch (Exception e) {
					bTogle = true;
				}
			} else
				// item did not exist yet....
				bTogle = true;
			currentSelection.getItemCollection().replaceItemValue(
					"a4j:showDetails", bTogle);

		}
	}

	/**
	 * this method toogles the ajax attribute a4j:showHistory. This Attribute is
	 * used to display the WorkitemHistory inside the Worklist View.
	 * 
	 * @return
	 */
	public void doToggleHistory(ActionEvent event) throws Exception {
		ItemCollectionAdapter currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {
			// now try to read current toogle state and switch state

			boolean bTogle = false;
			if (currentSelection.getItemCollection().hasItem("a4j:showHistory")) {
				try {
					boolean bTogleCurrent = (Boolean) currentSelection
							.getItemCollection()
							.getItemValue("a4j:showHistory").firstElement();
					bTogle = !bTogleCurrent;
				} catch (Exception e) {
					bTogle = true;
				}
			} else
				// item did not exist yet....
				bTogle = true;
			currentSelection.getItemCollection().replaceItemValue(
					"a4j:showHistory", bTogle);

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

	public void doSwitchToWorklistByAuthor(ActionEvent event) {
		queryType = this.QUERY_WORKLIST_BY_AUTHOR;
		setViewTitle("title_worklist_by_author");
		doReset(event);
	}

	public void doSwitchToWorklistByOwner(ActionEvent event) {
		queryType = this.QUERY_WORKLIST_BY_OWNER;
		setViewTitle("title_worklist_by_owner");
		doReset(event);
	}

	public void doSwitchToWorklistByCreator(ActionEvent event) {
		queryType = this.QUERY_WORKLIST_BY_CREATOR;
		setViewTitle("title_worklist_by_creator");
		doReset(event);
	}

	public void doSwitchToWorklistArchive(ActionEvent event) {
		queryType = this.QUERY_WORKLIST_ARCHIVE;
		setViewTitle("title_worklist_archive");
		doReset(event);
	}

	public void doSwitchToWorklistDeletions(ActionEvent event) {
		queryType = this.QUERY_WORKLIST_DELETIONS;
		setViewTitle("title_worklist_deletions");
		doReset(event);
	}

	public void doSwitchToWorklistAll(ActionEvent event) {
		queryType = this.QUERY_WORKLIST_ALL;
		setViewTitle("title_worklist_all");
		doReset(event);
	}
	
	
	public void doSwitchToWorklistSearch(ActionEvent event) {
		queryType = this.QUERY_SEARCH;
		setViewTitle("title_worklist_all");
		doReset(event);
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

		ItemCollectionAdapter currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {

			// remove a4j: attributes generated inside the viewentries by the UI
			currentSelection.getItemCollection().getAllItems().remove(
					"a4j:showhistory");
			currentSelection.getItemCollection().getAllItems().remove(
					"a4j:showdetails");

			workitemService.moveIntoArchive(currentSelection
					.getItemCollection());

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
		ItemCollectionAdapter currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {

			// remove a4j: attributes generated inside the viewentries by the UI
			currentSelection.getItemCollection().getAllItems().remove(
					"a4j:showhistory");
			currentSelection.getItemCollection().getAllItems().remove(
					"a4j:showdetails");

			workitemService.moveIntoDeletions(currentSelection
					.getItemCollection());

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
		ItemCollectionAdapter currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {

			// remove a4j: attributes generated inside the viewentries by the UI
			currentSelection.getItemCollection().getAllItems().remove(
					"a4j:showhistory");
			currentSelection.getItemCollection().getAllItems().remove(
					"a4j:showdetails");

			workitemService.restoreFromArchive(currentSelection
					.getItemCollection());

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
		ItemCollectionAdapter currentSelection = getCurrentSelection(event);
		if (currentSelection != null) {
			// remove a4j: attributes generated inside the viewentries by the UI
			currentSelection.getItemCollection().getAllItems().remove(
					"a4j:showhistory");
			currentSelection.getItemCollection().getAllItems().remove(
					"a4j:showdetails");
			workitemService.restoreFromDeletions(currentSelection
					.getItemCollection());

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
	private ItemCollectionAdapter getCurrentSelection(ActionEvent event)
			throws Exception {

		ItemCollectionAdapter currentSelection = null;
		// suche selektierte Zeile....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// Zeile gefunden
			currentSelection = (ItemCollectionAdapter) ((UIData) parent)
					.getRowData();

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
	 * @see org.imixs.shareyouwork.business.WorkitemServiceBean
	 */
	private void loadWorkItemList() {
		workitems = new ArrayList<ItemCollectionAdapter>();
		Collection<ItemCollection> col = null;
		try {
			long lTime = System.currentTimeMillis();

			ItemCollection project = getProjectMB().getWorkitem();

			String sProjectUniqueID = project.getItemValueString("$uniqueid");

			switch (queryType) {
			case QUERY_WORKLIST_BY_OWNER:
				col = workitemService.findWorkitemsByOwner(sProjectUniqueID,
						this.getProcessFilter(), row, count,sortby,sortorder);
				break;
			case QUERY_WORKLIST_BY_CREATOR:
				col = workitemService.findWorkitemsByCreator(sProjectUniqueID,
						this.getProcessFilter(), row, count,sortby,sortorder);
				break;

			case QUERY_WORKLIST_BY_AUTHOR:
				col = workitemService.findWorkitemsByAuthor(sProjectUniqueID,
						this.getProcessFilter(), row, count,sortby,sortorder);
				break;

			case QUERY_WORKLIST_ALL:
				col = workitemService.findAllWorkitems(sProjectUniqueID, this
						.getProcessFilter(), row, count,sortby,sortorder);
				break;

			case QUERY_WORKLIST_ARCHIVE:
				col = workitemService.findArchive(sProjectUniqueID, this
						.getProcessFilter(), row, count,sortby,sortorder);
				break;

			case QUERY_WORKLIST_DELETIONS:
				col = workitemService.findDeletions(sProjectUniqueID, this
						.getProcessFilter(), row, count,sortby,sortorder);
				break;
				
			case QUERY_SEARCH:
				if (searchQuery!=null || "".equals(searchQuery))
					col=this.workitemService.findWorkitemsByQuery(searchQuery, row,count);
				else 
					// return empty result if no filter is defined!
					col=new Vector<ItemCollection>();
				break;
				
			default:
				col = workitemService.findAllWorkitems(sProjectUniqueID, this
						.getProcessFilter(), row, count,sortby,sortorder);
			}

			lTime = System.currentTimeMillis() - lTime;
			System.out.println("  loadWorkItemList (" + lTime + " ms)");

			endOfList = col.size() < count;
			for (ItemCollection aworkitem : col) {
				workitems.add(new ItemCollectionAdapter(aworkitem));
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

}
