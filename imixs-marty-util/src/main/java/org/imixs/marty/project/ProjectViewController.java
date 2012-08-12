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

package org.imixs.marty.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.config.SetupController;
import org.imixs.marty.ejb.ProfileService;
import org.imixs.marty.profile.UserController;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;

@Named("projectViewController")
@SessionScoped
public class ProjectViewController extends
		org.imixs.workflow.jee.faces.workitem.ViewController implements
		Serializable {

	private static final long serialVersionUID = 1L;

	/* Profile Service */
	@EJB
	ProfileService profileService;
	
	@EJB
	EntityService entityService;

	@Inject
	private UserController userController = null;

	@Inject
	private SetupController setupController = null;

	@Inject
	private ProjectController projectController = null;

	
	List<ItemCollection> projects;
	private static Logger logger = Logger.getLogger("org.imixs.marty");


	public ProjectViewController() {
		super();

	}

	/**
	 * The ProjectController provides additional views which were added in the
	 * init() method call
	 */
	@PostConstruct
	public void init() {
		super.init();
		// add custom views
		getViews().put(
				"projectlist.deleted.modified.desc",
				"SELECT project FROM Entity AS project "
						+ " WHERE project.type IN ('projectdeleted' ) "
						+ " ORDER BY project.modified DESC");
		this.setMaxSearchResult(this.setupController.getWorkitem()
				.getItemValueInteger("MaxviewEntriesPerPage"));

	}

	public UserController getUserController() {
		return userController;
	}

	public void setUserController(UserController userController) {
		this.userController = userController;
	}

	public SetupController getSetupsetupController() {
		return setupController;
	}

	public void setSetupsetupController(SetupController setupMB) {
		this.setupController = setupMB;
	}

	public ProfileService getProfileService() {
		return profileService;
	}


	public ProjectController getProjectController() {
		return projectController;
	}

	public void setProjectController(ProjectController projectController) {
		this.projectController = projectController;
	}







	class ProjectViewAdapter extends ViewAdapter {

		public List<ItemCollection> getViewEntries() {
			if ("subprojectlist".equals(getView())) {

				String sIDRef = projectController.getWorkitem()
						.getItemValueString("$uniqueIDRef");

				String sQuery = "SELECT project FROM Entity AS project "
						+ " JOIN project.textItems AS r"
						+ " JOIN project.textItems AS n"
						+ " WHERE project.type = 'project'"
						+ " AND n.itemName = 'txtname' "
						+ " AND r.itemName='$uniqueidref'"
						+ " AND r.itemValue = '" + sIDRef + "' "
						+ " ORDER BY n.itemValue asc";

				return getEntityService().findAllEntities(sQuery, getRow(),
						getMaxSearchResult());

			}

			if ("mainprojectlist".equals(getView())) {
				String sQuery = "SELECT project FROM Entity AS project "
						+ " JOIN project.textItems AS n1"
						+ " JOIN project.textItems AS n2"
						+ " WHERE project.type = 'project'"
						+ " AND n1.itemName = 'txtname' "
						+ " AND n2.itemName = 'txtprojectname'"
						+ " AND n1.itemValue = n2.itemValue "
						+ " ORDER BY n1.itemValue asc";
				return getEntityService().findAllEntities(sQuery, getRow(),
						getMaxSearchResult());
			}

			if ("projectlistbyowner".equals(getView())) {

				String aname = userController.getUserPrincipal();

				String sQuery = "SELECT project FROM Entity AS project "
						+ " JOIN project.textItems as owner"
						+ " JOIN project.textItems AS n1"
						+ " WHERE project.type = 'project'"
						+ " AND owner.itemName = 'namowner' AND owner.itemValue = '"
						+ aname + "'" + " AND n1.itemName = 'txtname' "
						+ " ORDER BY n1.itemValue asc";

				return getEntityService().findAllEntities(sQuery, getRow(),
						getMaxSearchResult());
			}

			if ("projectlistbymember".equals(getView())) {

				String aname = userController.getUserPrincipal();

				String sQuery = "SELECT project FROM Entity AS project "
						+ " JOIN project.textItems AS n1"
						+ " JOIN project.readAccessList AS readaccess "
						+ " JOIN project.writeAccessList AS writeaccess "
						+ " WHERE project.type = 'project'"
						+ " AND ( readaccess.value IN ( '" + aname + "')"
						+ "      OR writeaccess.value IN ( '" + aname + "')) "
						+ " AND n1.itemName = 'txtname' "
						+ " ORDER BY n1.itemValue asc";
				return getEntityService().findAllEntities(sQuery, getRow(),
						getMaxSearchResult());
			}
			// default behaivor

			return super.getViewEntries();

		}
	}

}
