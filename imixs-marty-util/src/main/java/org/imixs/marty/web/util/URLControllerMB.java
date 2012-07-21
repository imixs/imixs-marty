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

package org.imixs.marty.web.util;

import javax.ejb.EJB;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.imixs.marty.ejb.ProfileService;
import org.imixs.marty.ejb.ProjectService;
import org.imixs.marty.web.project.ProjectMB;
import org.imixs.marty.web.workitem.WorkitemMB;
import org.imixs.marty.web.workitem.WorklistMB;
import org.imixs.workflow.ItemCollection;

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
public class URLControllerMB {
	public final String COOKIE_PROJECT = "imixs.marty.project";
	public final String COOKIE_PROCESS = "imixs.marty.process";
	public final String COOKIE_WORKITEM = "imixs.marty.workitem";

	/* Profile Service */
	@EJB
	ProfileService profileService;

	/* Project Service */
	@EJB
	ProjectService projectService;

	
	
	
	@ManagedProperty(value = "#{worklistMB}")
	private WorklistMB worklistMB = null;
	
	@ManagedProperty(value = "#{projectMB}")
	private ProjectMB projectMB = null;
	
	@ManagedProperty(value = "#{workitemMB}")
	private WorkitemMB workitemMB = null;

	private String project = null;
	private String process = null;
	private String workitem = null;

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
		itemColProject = projectService.findProject(project);
		// update the ProjectMB
		if (itemColProject != null) {
			getProjectBean().setWorkitem(itemColProject);

			// reset worklist
			worklistMB.doReset(null);
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
		itemColProject = getProjectBean().getWorkitem();
		// update the ProjectMB
		if (itemColProject != null) {
			// ??
			System.out.println("URLControllerMB - Start Process '" + process
					+ "' ");
			try {
				getWorkitemBean().doCreateWorkitem(process, null);
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
			ItemCollection aworkitem = getWorkitemBean().getEntityService()
					.load(workitem);
			getWorkitemBean().setWorkitem(aworkitem);
			String sIDRef = aworkitem.getItemValueString("$uniqueidRef");
			// try now to update the project if no project is selected
			itemColProject = getProjectBean().getWorkitem();
			// update the ProjectMB
			if (itemColProject == null
					|| !sIDRef.equals(itemColProject
							.getItemValueString("$uniqueid"))) {
				itemColProject = projectService.findProject(sIDRef);
				if (itemColProject != null) {
					System.out.println("URLControllerMB - Select Project '"
							+ sIDRef + "' ");
					getProjectBean().setWorkitem(itemColProject);
					// reset worklist
					worklistMB.doReset(null);
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

	private ProjectMB getProjectBean() {
		if (projectMB == null)
			projectMB = (ProjectMB) FacesContext.getCurrentInstance()
					.getApplication().getELResolver().getValue(
							FacesContext.getCurrentInstance().getELContext(),
							null, "projectMB");
		return projectMB;
	}

	private WorkitemMB getWorkitemBean() {
		if (workitemMB == null)
			workitemMB = (WorkitemMB) FacesContext.getCurrentInstance()
					.getApplication().getELResolver().getValue(
							FacesContext.getCurrentInstance().getELContext(),
							null, "workitemMB");
		return workitemMB;
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
