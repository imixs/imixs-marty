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
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.config.SetupController;
import org.imixs.marty.profile.UserController;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.faces.workitem.DataController;

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
public class ViewController extends DataController implements Serializable {

	private static final long serialVersionUID = 1L;

	
	// view options
	private int sortby = -1;
	private int sortorder = -1;
	private String viewTitle = "undefined viewtitle";

	// filter options
	private String projectUniqueID = "";
	private String workflowGroup = "";
	private String modelVersion="";
	private int processID = 0;
	private String searchQuery = null;

	
	
	final String QUERY_WORKLIST_BY_OWNER = "worklist.owner";
	final String QUERY_WORKLIST_BY_CREATOR = "worklist.creator";
	final String QUERY_WORKLIST_BY_AUTHOR = "worklist.author";
	final String QUERY_WORKLIST_ALL = "worklist";
	final String QUERY_WORKLIST_ARCHIVE = "archive";
	final String QUERY_WORKLIST_DELETIONS = "deletions";
	
	
	public final static int SORT_BY_CREATED = 0;
	public final static int SORT_BY_MODIFIED = 1;
	public final static int SORT_ORDER_DESC = 0;
	public final static int SORT_ORDER_ASC = 1;


	@Inject
	UserController userController = null;

	@Inject
	private SetupController setupController = null;

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

		
		// read configurations for the max count. This value can be also set via
		// faces-config-custom.xml
		if (getMaxSearchResult() == 0)
			setMaxSearchResult(setupController.getWorkitem().getItemValueInteger(
					"Maxviewentriesperpage"));
		// read configuration for the sort order
		if (sortby == -1)
			sortby = setupController.getWorkitem().getItemValueInteger("Sortby");
		if (sortorder == -1)
			sortorder = setupController.getWorkitem().getItemValueInteger("Sortorder");
	}

	public SetupController getSetupsetupController() {
		return setupController;
	}

	public void setSetupsetupController(SetupController setupMB) {
		this.setupController = setupMB;
	}

	public UserController getUserController() {
		return userController;
	}

	public void setUserController(UserController myProfileMB) {
		this.userController = myProfileMB;
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

	public String getModelVersion() {
		return modelVersion;
	}

	public void setModelVersion(String modelVersion) {
		this.modelVersion = modelVersion;
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

	

	public String getViewTitle() {
		return viewTitle;
	}

	public void setViewTitle(String viewTitle) {
		
		Locale locale = new Locale(userController.getLocale());
		ResourceBundle rb = null;
		rb = ResourceBundle.getBundle("bundle.workitem", locale);

		this.viewTitle = rb.getString(viewTitle);

	}

	
	class MartyViewAdapter extends ViewAdapter {

		
		public List<ItemCollection> getViewEntries() {
			

			if (QUERY_WORKLIST_BY_AUTHOR.equals(getView())) {
				
				return getEntityService().findAllEntities(buildQueryWorkitemsByAuthor(), getRow(),
						getMaxSearchResult());
			}
			
			
			
			
			if (QUERY_WORKLIST_BY_CREATOR.equals(getView())) {
				return getEntityService().findAllEntities(buildQueryWorkitemsByCreator(), getRow(),
						getMaxSearchResult());
			}
			
			
			
			if (QUERY_WORKLIST_BY_OWNER.equals(getView())) {
				return getEntityService().findAllEntities(buildQueryWorkitemsByOwner(), getRow(),
						getMaxSearchResult());
			}
			
			
			
			
			 
			 // default behaivor
			 
			 return super.getViewEntries();
			
			
		
		}
		
		
		
		
		
		
		
		
		/**
		 * Returns a JPQL statement selecting the worklist for the current user
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
		private String buildQueryWorkitemsByAuthor() {
			ArrayList<ItemCollection> teamList = new ArrayList<ItemCollection>();
			
		

			// construct query
			String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";
			sQuery += " JOIN wi.writeAccessList as a1";
			if (!"".equals(getProjectUniqueID()))
				sQuery += " JOIN wi.textItems as t2 ";
			if (!"".equals(workflowGroup))
				sQuery += " JOIN wi.textItems as t3 ";
			if (getProcessID() > 0)
				sQuery += " JOIN wi.integerItems as t4 ";
			if (!"".equals(getModelVersion() ))
				sQuery += " JOIN wi.textItems AS model ";
			sQuery += " WHERE wi.type = 'workitem'";
			sQuery += " AND a1.value = '" + userController.getUserPrincipal() + "'";
			if (!"".equals(getProjectUniqueID()))
				sQuery += " AND t2.itemName = '$uniqueidref' and t2.itemValue = '"
						+ getProjectUniqueID() + "' ";
			if (!"".equals(getModelVersion() ))
				sQuery += " AND model.itemName = '$modelversion' AND model.itemValue ='"
						+ getModelVersion()  + "'";

			if (!"".equals(workflowGroup))
				sQuery += " AND t3.itemName = 'txtworkflowgroup' and t3.itemValue = '"
						+ workflowGroup + "' ";
			// Process ID
			if (getProcessID() > 0)
				sQuery += " AND t4.itemName = '$processid' AND t4.itemValue ='"
						+ getProcessID() + "'";

			// creade ORDER BY phrase
			sQuery += " ORDER BY wi.";
			if (sortby == SORT_BY_CREATED)
				sQuery += "created ";
			else
				sQuery += "modified ";
			if (sortorder == SORT_ORDER_DESC)
				sQuery += "desc";
			else
				sQuery += "asc";

			return sQuery;

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
		private String buildQueryWorkitemsByCreator() {
		

			// construct query
			String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";
			sQuery += " JOIN wi.textItems as a1";
			if (!"".equals(getProjectUniqueID()))
				sQuery += " JOIN wi.textItems as t2 ";
			if (!"".equals(getWorkflowGroup()))
				sQuery += " JOIN wi.textItems as t3 ";
			if (getProcessID() > 0)
				sQuery += " JOIN wi.integerItems as t4 ";
			if (!"".equals(getModelVersion() ))
				sQuery += " JOIN wi.textItems AS model ";
			sQuery += " WHERE wi.type = 'workitem'";
			sQuery += " AND a1.itemName = 'namcreator' and a1.itemValue = '" + getUserController().getUserPrincipal()
					+ "'";
			if (!"".equals(getProjectUniqueID()))
				sQuery += " AND t2.itemName = '$uniqueidref' and t2.itemValue = '"
						+ getProjectUniqueID() + "' ";
			if (!"".equals(getModelVersion() ))
				sQuery += " AND model.itemName = '$modelversion' AND model.itemValue ='"
						+ getModelVersion() + "'";

			if (!"".equals(getWorkflowGroup()))
				sQuery += " AND t3.itemName = 'txtworkflowgroup' and t3.itemValue = '"
						+ getWorkflowGroup() + "' ";
			// Process ID
			if (getProcessID() > 0)
				sQuery += " AND t4.itemName = '$processid' AND t4.itemValue ='"
						+ getProcessID() + "'";

			// creade ORDER BY phrase
			sQuery += " ORDER BY wi.";
			if (sortby == SORT_BY_CREATED)
				sQuery += "created ";
			else
				sQuery += "modified ";
			if (sortorder == SORT_ORDER_DESC)
				sQuery += "desc";
			else
				sQuery += "asc";

		return sQuery;

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
		private String buildQueryWorkitemsByOwner() {
		

			// construct query
			String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";
			sQuery += " JOIN wi.textItems as a1";
			if (!"".equals(getProjectUniqueID()))
				sQuery += " JOIN wi.textItems as t2 ";
			if (!"".equals(getWorkflowGroup()))
				sQuery += " JOIN wi.textItems as t3 ";
			if (getProcessID() > 0)
				sQuery += " JOIN wi.integerItems as t4 ";
			if (!"".equals(getModelVersion()))
				sQuery += " JOIN wi.textItems AS model ";
			sQuery += " WHERE wi.type = 'workitem'";
			sQuery += " AND a1.itemName = 'namowner' and a1.itemValue = '" + getUserController().getUserPrincipal()
					+ "'";
			if (!"".equals(getProjectUniqueID()))
				sQuery += " AND t2.itemName = '$uniqueidref' and t2.itemValue = '"
						+ getProjectUniqueID() + "' ";

			if (!"".equals(getModelVersion()))
				sQuery += " AND model.itemName = '$modelversion' AND model.itemValue ='"
						+ getModelVersion() + "'";

			if (!"".equals(getWorkflowGroup()))
				sQuery += " AND t3.itemName = 'txtworkflowgroup' and t3.itemValue = '"
						+ getWorkflowGroup() + "' ";
			// Process ID
			if (getProcessID() > 0)
				sQuery += " AND t4.itemName = '$processid' AND t4.itemValue ='"
						+ getProcessID() + "'";

			// creade ORDER BY phrase
			sQuery += " ORDER BY wi.";
			if (sortby == SORT_BY_CREATED)
				sQuery += "created ";
			else
				sQuery += "modified ";
			if (sortorder == SORT_ORDER_DESC)
				sQuery += "desc";
			else
				sQuery += "asc";

			return sQuery;

		}

	}
	
	
	

}
