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

package org.imixs.marty.util;

import java.io.Serializable;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.imixs.marty.ejb.ProfileService;
import org.imixs.marty.project.ProjectController;
import org.imixs.marty.workflow.ViewController;
import org.imixs.marty.workflow.WorkflowController;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;

/**
 * This backing bean is used to control the project selection over URI Requests.
 * The bean is used only in request scope to handle request params provided by a
 * URL
 * <p>
 * The url param 'project' can be used to preselect a project. The index.xhtml
 * Page can test this bean if a URL Param is provided to redirect to a
 * preselected project.
 * <p>
 * The Bean can not hold the project information when the user is not still
 * authenticated (because of the request scope). So in such a situation when a
 * project is preselected from out of an unauthenticated situation the bean
 * creates a cookie to store this information.
 * 
 * 
 * @author rsoika
 * 
 */
@Named("urlControlerMB")
@RequestScoped
public class URLControllerMB  implements Serializable {

	private static final long serialVersionUID = 1L;
	private final String COOKIE_PROJECT = "imixs.marty.project";
	private final String COOKIE_PROCESS = "imixs.marty.process";
	private final String COOKIE_WORKITEM = "imixs.marty.workitem";

	/* Profile Service */
	@EJB
	private ProfileService profileService;

	@EJB
	private EntityService entityService;
	
	
	@Inject
	private ViewController viewController = null;
	
	@Inject
	private ProjectController projectMB = null;
	
	@Inject
	private WorkflowController workflowController = null;

	private String project = null;
	private String process = null;
	private String workitem = null;

	
	
	public URLControllerMB() {
		super();
		
	}
	



	public ViewController getViewController() {
		return viewController;
	}




	public void setViewController(ViewController viewController) {
		this.viewController = viewController;
	}




	public ProjectController getProjectMB() {
		return projectMB;
	}


	public void setProjectMB(ProjectController projectMB) {
		this.projectMB = projectMB;
	}


	public WorkflowController getWorkflowController() {
		return workflowController;
	}


	public void setWorkflowController(WorkflowController workitemMB) {
		this.workflowController = workitemMB;
	}


	/**
	 * Property for Project pre selection. Can be used to define a primary
	 * project. The Method reads the preselected project out from a cookie. The
	 * reason is that this bean need to runn in request mode. So we cant use a
	 * simple member variable here.
	 * 
	 * @return
	 */
	public String getProject() {
		if (project == null) {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			String cookieName = null;
			Cookie cookie[] = ((HttpServletRequest) facesContext
					.getExternalContext().getRequest()).getCookies();
			if (cookie != null && cookie.length > 0) {
				for (int i = 0; i < cookie.length; i++) {
					cookieName = cookie[i].getName();
					if (cookieName.equals(COOKIE_PROJECT)) {
						project = cookie[i].getValue();
						break;
					}
				}
			}
		}
		return project;
	}

	/**
	 * the set method selects a project and updates the project ItemCollection
	 * into the procjectMB inside the faces context. So a new project will be
	 * preselected and can be used in different situations like a user have
	 * selected this project form the projectlist. The information about the
	 * selection of a project will also be provided to the skinMB into the
	 * property 'singleProjectMode' which indicates that the user will work with
	 * one project. This information can be used in the GUI to hide project
	 * menues.
	 * 
	 * If the user is not authenticated the method stores the project id into a
	 * cookie.
	 * 
	 * When the user is authenticated and a project was successfully preselected
	 * then the method deletes the cookie - as his work was done ;-)
	 * 
	 * @param aprojectSelection
	 */
	public void setProject(String aprojectSelection) {
		this.project = aprojectSelection;

		if (project == null || "".equals(project))
			return;

		ItemCollection itemColProject = null;

		// if user is still not authenticated - store project info into a cookie
		if (!isAuthenticated()) {
			// update cookie
			HttpServletResponse response = (HttpServletResponse) FacesContext
					.getCurrentInstance().getExternalContext().getResponse();
			HttpServletRequest request = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();

			Cookie cookieSkin = new Cookie(COOKIE_PROJECT, project);
			// cookieSkin.setPath("/");
			cookieSkin.setPath(request.getContextPath());

			// maximum age of the cookie, specified in seconds
			// -1 indicating the cookie will persist until browser shutdown.
			cookieSkin.setMaxAge(-1);
			response.addCookie(cookieSkin);
			// leave now....
			return;
		}

		// user is authenticated....
		// Find project
		itemColProject = entityService.load(project);
		// update the ProjectMB
		if (itemColProject != null) {
			projectMB.setWorkitem(itemColProject);

			// reset worklist
			viewController.doReset(null);
			System.out.println("URLControllerMB - Project '" + project
					+ "' selected");
		} else {
			System.out.println("URLControllerMB - Project '" + project
					+ "' not found!");
		}

		// delete the cookie
		HttpServletResponse response = (HttpServletResponse) FacesContext
				.getCurrentInstance().getExternalContext().getResponse();
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();

		Cookie cookieSkin = new Cookie(COOKIE_PROJECT, project);
		// cookieSkin.setPath("/");
		cookieSkin.setPath(request.getContextPath());

		// maximum age of the cookie, specified in seconds
		// 0 deletes the cookie
		cookieSkin.setMaxAge(0);
		response.addCookie(cookieSkin);
		// leave now....

	}

	/**
	 * Property for Start Process pre selection. Can be used to define a primary
	 * process to be started with. The Method reads the preselected process out
	 * from a cookie. The reason is that this bean need to runn in request mode.
	 * So we cant use a simple member variable here.
	 * 
	 * @return
	 */
	public String getProcess() {
		if (process == null) {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			String cookieName = null;
			Cookie cookie[] = ((HttpServletRequest) facesContext
					.getExternalContext().getRequest()).getCookies();
			if (cookie != null && cookie.length > 0) {
				for (int i = 0; i < cookie.length; i++) {
					cookieName = cookie[i].getName();
					if (cookieName.equals(COOKIE_PROCESS)) {
						process = cookie[i].getValue();
						break;
					}
				}
			}
		}
		return process;
	}

	/**
	 * the set method selects a start process id and updates the project
	 * ItemCollection into the procjectMB inside the faces context. So a new
	 * project will be preselected and can be used in different situations like
	 * a user have selected this project form the projectlist. The information
	 * about the selection of a project will also be provided to the skinMB into
	 * the property 'singleProjectMode' which indicates that the user will work
	 * with one project. This information can be used in the GUI to hide project
	 * menues.
	 * 
	 * If the user is not authenticated the method stores the project id into a
	 * cookie.
	 * 
	 * When the user is authenticated and a project was successfully preselected
	 * then the method deletes the cookie - as his work was done ;-)
	 * 
	 * @param aprojectSelection
	 */
	public void setProcess(String aStartProcess) {
		this.process = aStartProcess;

		if (process == null || "".equals(process))
			return;

		ItemCollection itemColProject = null;

		// if user is still not authenticated - store project info into a cookie
		if (!isAuthenticated()) {
			// update cookie
			HttpServletResponse response = (HttpServletResponse) FacesContext
					.getCurrentInstance().getExternalContext().getResponse();
			HttpServletRequest request = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();

			Cookie cookieSkin = new Cookie(COOKIE_PROCESS, process);
			// cookieSkin.setPath("/");
			cookieSkin.setPath(request.getContextPath());

			// maximum age of the cookie, specified in seconds
			// -1 indicating the cookie will persist until browser shutdown.
			cookieSkin.setMaxAge(-1);
			response.addCookie(cookieSkin);
			// leave now....
			return;
		}

		// user is authenticated....
		// Start a Process ?
		itemColProject =projectMB.getWorkitem();
		// update the ProjectMB
		if (itemColProject != null) {
			// ??
			System.out.println("URLControllerMB - Start Process '" + process
					+ "' ");
			try {
				workflowController.doCreateWorkitem(process, null);
			} catch (Exception e) {

				e.printStackTrace();
			}
		}

		// delete the cookie
		HttpServletResponse response = (HttpServletResponse) FacesContext
				.getCurrentInstance().getExternalContext().getResponse();
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();

		Cookie cookieSkin = new Cookie(COOKIE_PROCESS, process);
		// cookieSkin.setPath("/");
		cookieSkin.setPath(request.getContextPath());

		// maximum age of the cookie, specified in seconds
		// 0 deletes the cookie
		cookieSkin.setMaxAge(0);
		response.addCookie(cookieSkin);
		// leave now....

	}

	/**
	 * Property for a pre selected workitemn. Can be used to define a workitem
	 * to be started with. The Method reads the preselected $uniqueid out from a
	 * cookie. The reason is that this bean need to runn in request mode. So we
	 * cant use a simple member variable here.
	 * 
	 * @return
	 */
	public String getWorkitem() {
		if (workitem == null) {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			String cookieName = null;
			Cookie cookie[] = ((HttpServletRequest) facesContext
					.getExternalContext().getRequest()).getCookies();
			if (cookie != null && cookie.length > 0) {
				for (int i = 0; i < cookie.length; i++) {
					cookieName = cookie[i].getName();
					if (cookieName.equals(COOKIE_WORKITEM)) {
						workitem = cookie[i].getValue();
						break;
					}
				}
			}
		}
		return workitem;
	}

	/**
	 * the set method selects a workitem. If the user is not authenticated the
	 * method stores the project id into a cookie.
	 * 
	 * The Method also updates the Project Reference to the coresponding
	 * $uniqueidRef
	 * 
	 * When the user is authenticated and a workitem was successfully
	 * preselected then the method deletes the cookie - as his work was done ;-)
	 * 
	 * 
	 * @param aprojectSelection
	 */
	public void setWorkitem(String aUniqueid) {
		this.workitem = aUniqueid;

		if (workitem == null || "".equals(workitem))
			return;

		ItemCollection itemColProject = null;

		// if user is still not authenticated - store project info into a cookie
		if (!isAuthenticated()) {
			// update cookie
			HttpServletResponse response = (HttpServletResponse) FacesContext
					.getCurrentInstance().getExternalContext().getResponse();
			HttpServletRequest request = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();

			Cookie cookieSkin = new Cookie(COOKIE_WORKITEM, workitem);
			// cookieSkin.setPath("/");
			cookieSkin.setPath(request.getContextPath());

			// maximum age of the cookie, specified in seconds
			// -1 indicating the cookie will persist until browser shutdown.
			cookieSkin.setMaxAge(-1);
			response.addCookie(cookieSkin);
			// leave now....
			return;
		}

		// user is authenticated.... set the workitem ....
		System.out.println("URLControllerMB - Select Workitem '" + workitem
				+ "' ");
		try {
			ItemCollection aworkitem = workflowController.getEntityService()
					.load(workitem);
			workflowController.setWorkitem(aworkitem);
			String sIDRef = aworkitem.getItemValueString("$uniqueidRef");
			// try now to update the project if no project is selected
			itemColProject = projectMB.getWorkitem();
			// update the ProjectMB
			if (itemColProject == null
					|| !sIDRef.equals(itemColProject
							.getItemValueString("$uniqueid"))) {
				itemColProject = entityService.load(sIDRef);
				if (itemColProject != null) {
					System.out.println("URLControllerMB - Select Project '"
							+ sIDRef + "' ");
					projectMB.setWorkitem(itemColProject);
					// reset worklist
					viewController.doReset(null);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// delete the cookie
		HttpServletResponse response = (HttpServletResponse) FacesContext
				.getCurrentInstance().getExternalContext().getResponse();
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();

		Cookie cookieSkin = new Cookie(COOKIE_WORKITEM, workitem);
		// cookieSkin.setPath("/");
		cookieSkin.setPath(request.getContextPath());

		// maximum age of the cookie, specified in seconds
		// 0 deletes the cookie
		cookieSkin.setMaxAge(0);
		response.addCookie(cookieSkin);
		// leave now....

	}



	/**
	 * indicates if the user is authenticated
	 * 
	 * @return
	 */
	private boolean isAuthenticated() {
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		String user = externalContext.getUserPrincipal() != null ? externalContext
				.getUserPrincipal().toString()
				: null;
		return (user != null);
	}
}
