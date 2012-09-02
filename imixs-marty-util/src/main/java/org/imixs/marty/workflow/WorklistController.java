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
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.config.SetupController;
import org.imixs.marty.profile.UserController;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.WorkflowService;

/**
 * The ViewController is a generic implementation for all types of views for
 * workitems. A view can be filtered by WorkflowGroup (String), by ProcessID
 * (Integer) and by Project (String). Alternatively the viewController provides
 * a custom searchFilter.
 * 
 * @author rsoika
 * 
 */
@Named("worklistController")
@ViewScoped
public class WorklistController extends
		org.imixs.workflow.jee.faces.workitem.WorklistController implements
		Serializable {

	private static final long serialVersionUID = 1L;

	private ItemCollection viewFilter = null;
	private String viewTitle = null;

	// view filter

	final String QUERY_WORKLIST_BY_OWNER = "worklist.owner";
	final String QUERY_WORKLIST_BY_CREATOR = "worklist.creator";
	final String QUERY_WORKLIST_BY_AUTHOR = "worklist.author";
	final String QUERY_WORKLIST_ALL = "worklist";
	final String QUERY_WORKLIST_ARCHIVE = "archive";
	final String QUERY_WORKLIST_DELETIONS = "deletions";

	@Inject
	UserController userController = null;

	@Inject
	private SetupController setupController = null;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	public WorklistController() {
		super();
		viewFilter = new ItemCollection();
		setViewAdapter(new MartyViewAdapter());
	}

	/**
	 * This method initializes the view type an view settings like sort order
	 * and max entries per page. These properties can be set through the
	 * BeanProperties in the faces-config.xml or controlled by the config
	 * entity.
	 */
	@PostConstruct
	public void init() {
		super.init();
	}

	public ItemCollection getViewFilter() {
		return viewFilter;
	}

	public void setViewFilter(ItemCollection viewFilter) {
		this.viewFilter = viewFilter;
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

	@Override
	public void setView(String view) {
		// reset view title
		viewTitle = null;
		super.setView(view);
	}

	/**
	 * This method computes a internationalized view title from the property
	 * 'view'
	 * 
	 * @param viewTitle
	 */
	public String getViewTitle() {
		if (viewTitle!=null)
			return viewTitle;
		// compute view title
		String viewName=getView();
		try {
			Locale locale = new Locale(userController.getLocale());
			ResourceBundle rb = null;
			rb = ResourceBundle.getBundle("bundle.workitem", locale);
			viewTitle=rb.getString(viewName);	
		} catch (Exception e) {
			logger.warning("no view title defined in resource bundle for view name '"
					+ viewName);

			viewTitle= viewName + ":undefined";
		}
		return viewTitle;
	}

	class MartyViewAdapter extends ViewAdapter {

		public List<ItemCollection> getViewEntries(final WorklistController controller) {

			if (QUERY_WORKLIST_BY_AUTHOR.equals(getView())) {

				return getEntityService().findAllEntities(
						buildQueryWorkitemsByAuthor(), getRow(),
						getMaxResult());
			}

			if (QUERY_WORKLIST_BY_CREATOR.equals(getView())) {
				return getEntityService().findAllEntities(
						buildQueryWorkitemsByCreator(), getRow(),
						getMaxResult());
			}

			if (QUERY_WORKLIST_BY_OWNER.equals(getView())) {
				return getEntityService().findAllEntities(
						buildQueryWorkitemsByOwner(), getRow(),
						getMaxResult());
			}

			// default behaivor

			return super.getViewEntries(controller);

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

			// construct query
			String sQuery = "SELECT DISTINCT wi FROM Entity AS wi ";
			sQuery += " JOIN wi.writeAccessList as a1";
			if (!"".equals(getViewFilter().getItemValueString("project")))
				sQuery += " JOIN wi.textItems as t2 ";
			if (!"".equals(getViewFilter().getItemValueString("workflowGroup")))
				sQuery += " JOIN wi.textItems as t3 ";
			if (getViewFilter().getItemValueInteger("ProcessID") > 0)
				sQuery += " JOIN wi.integerItems as t4 ";

			sQuery += " WHERE wi.type = 'workitem'";
			sQuery += " AND a1.value = '" + userController.getUserPrincipal()
					+ "'";
			if (!"".equals(getViewFilter().getItemValueString("project")))
				sQuery += " AND t2.itemName = '$uniqueidref' and t2.itemValue = '"
						+ getViewFilter().getItemValueString("project") + "' ";

			if (!"".equals(getViewFilter().getItemValueString("workflowGroup")))
				sQuery += " AND t3.itemName = 'txtworkflowgroup' and t3.itemValue = '"
						+ getViewFilter().getItemValueString("workflowGroup")
						+ "' ";
			// Process ID
			if (getViewFilter().getItemValueInteger("ProcessID") > 0)
				sQuery += " AND t4.itemName = '$processid' AND t4.itemValue ='"
						+ getViewFilter().getItemValueInteger("ProcessID")
						+ "'";

			// creade ORDER BY phrase
			sQuery += createSortOrderClause();

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
			if (!"".equals(getViewFilter().getItemValueString("project")))
				sQuery += " JOIN wi.textItems as t2 ";
			if (!"".equals(getViewFilter().getItemValueString("workflowGroup")))
				sQuery += " JOIN wi.textItems as t3 ";
			if (getViewFilter().getItemValueInteger("ProcessID") > 0)
				sQuery += " JOIN wi.integerItems as t4 ";

			sQuery += " WHERE wi.type = 'workitem'";
			sQuery += " AND a1.itemName = 'namcreator' and a1.itemValue = '"
					+ getUserController().getUserPrincipal() + "'";
			if (!"".equals(getViewFilter().getItemValueString("project")))
				sQuery += " AND t2.itemName = '$uniqueidref' and t2.itemValue = '"
						+ getViewFilter().getItemValueString("project") + "' ";

			if (!"".equals(getViewFilter().getItemValueString("workflowGroup")))
				sQuery += " AND t3.itemName = 'txtworkflowgroup' and t3.itemValue = '"
						+ getViewFilter().getItemValueString("workflowGroup")
						+ "' ";
			// Process ID
			if (getViewFilter().getItemValueInteger("ProcessID") > 0)
				sQuery += " AND t4.itemName = '$processid' AND t4.itemValue ='"
						+ getViewFilter().getItemValueInteger("ProcessID")
						+ "'";

			// creade ORDER BY phrase
			sQuery += createSortOrderClause();

			return sQuery;

		}

		/**
		 * Returns a collection of workitems where current user is owner
		 * (namOwner)
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
			if (!"".equals(getViewFilter().getItemValueString("project")))
				sQuery += " JOIN wi.textItems as t2 ";
			if (!"".equals(getViewFilter().getItemValueString("workflowGroup")))
				sQuery += " JOIN wi.textItems as t3 ";
			if (getViewFilter().getItemValueInteger("ProcessID") > 0)
				sQuery += " JOIN wi.integerItems as t4 ";

			sQuery += " WHERE wi.type = 'workitem'";
			sQuery += " AND a1.itemName = 'namowner' and a1.itemValue = '"
					+ getUserController().getUserPrincipal() + "'";
			if (!"".equals(getViewFilter().getItemValueString("project")))
				sQuery += " AND t2.itemName = '$uniqueidref' and t2.itemValue = '"
						+ getViewFilter().getItemValueString("project") + "' ";

			if (!"".equals(getViewFilter().getItemValueString("workflowGroup")))
				sQuery += " AND t3.itemName = 'txtworkflowgroup' and t3.itemValue = '"
						+ getViewFilter().getItemValueString("workflowGroup")
						+ "' ";
			// Process ID
			if (getViewFilter().getItemValueInteger("ProcessID") > 0)
				sQuery += " AND t4.itemName = '$processid' AND t4.itemValue ='"
						+ getViewFilter().getItemValueInteger("ProcessID")
						+ "'";

			// creade ORDER BY phrase
			sQuery += createSortOrderClause();

			return sQuery;

		}

		/**
		 * generates a sort order clause depending on a sororder id
		 * 
		 * @param asortorder
		 * @return
		 */
		private String createSortOrderClause() {
			switch (getSortOrder()) {

			case WorkflowService.SORT_ORDER_CREATED_ASC: {
				return " ORDER BY wi.created asc";
			}
			case WorkflowService.SORT_ORDER_MODIFIED_ASC: {
				return " ORDER BY wi.modified asc";
			}
			case WorkflowService.SORT_ORDER_MODIFIED_DESC: {
				return " ORDER BY wi.modified desc";
			}
			default:
				return " ORDER BY wi.created desc";
			}

		}
	}

}
