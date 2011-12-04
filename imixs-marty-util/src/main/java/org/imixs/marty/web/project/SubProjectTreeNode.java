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

package org.imixs.marty.web.project;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.imixs.workflow.ItemCollection;
import org.richfaces.model.TreeNodeImpl;

/**
 * This TreeNodeImplemenation is used to compute the subproject to a project
 * node. This is for better performance. SubProjects will be computed only if the
 * node is expanded!
 * 
 * The constructor expects a ItemCollection of a project
 * 
 * @author rsoika
 * 
 */
public class SubProjectTreeNode extends TreeNodeImpl {
	/* Project Backing Bean */
	private ProjectMB projectBean = null;

	public static final int ROOT_PROJECT = 0;
	public static final int SUB_PROJECT = 1;
	
	private boolean childsLoaded = false;

	private int projectType = 0;
	
	private static final long serialVersionUID = 1L;

	private ItemCollection project = null;
	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	
	public SubProjectTreeNode(ItemCollection parent, int aprojecttype) {
		super();
		
		try {
			parent.replaceItemValue("project_node_type" ,aprojecttype);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		project = (parent);

		
		this.setData(project);
		this.projectType = aprojecttype;
	}


	/**
	 * returns the project type (ROOT_PROJECT SUB_PROJECT)
	 * 
	 * @return
	 */
	public int getProjectType() {
		return projectType;
	}

	/**
	 * returns the Project Data as a ItemCollectionAdapter
	 * @return
	 */
	public ItemCollection getProject() {
		return project;
	}

	

	/**
	 * returns an instance of the PorjectMB
	 * 
	 * @return
	 */
	public ProjectMB getProjectBean() {
		if (projectBean == null)
			projectBean = (ProjectMB) FacesContext.getCurrentInstance()
					.getApplication().getELResolver().getValue(
							FacesContext.getCurrentInstance().getELContext(),
							null, "projectMB");
		return projectBean;
	}

	/**
	 * Lazy loding for childs
	 */
	@Override
	public Iterator getChildren() {
		if (!childsLoaded) {
			try {
				loadChildren();
			} catch (Exception e) {
				e.printStackTrace();
			}
			childsLoaded = true;
		}

		return super.getChildren();
	}

	/**
	 * a processGoup can't be a leaf.
	 */
	@Override
	public boolean isLeaf() {
		return !getChildren().hasNext();
		//return false;
	}

	/**
	 * This method returns a list of subprojects containd by the parent project.
	 * 
	 * @return
	 * @throws Exception
	 */
	public void loadChildren() throws Exception {
		long l = System.currentTimeMillis();
		// get unique id
		String sProjectID = project.getItemValueString(
				"$UniqueID");
		Collection<ItemCollection> col = this.getProjectBean()
				.getProjectService().findAllSubProjects(sProjectID, 0, -1);

		for (ItemCollection aworkitem : col) {
			String sID = aworkitem.getItemValueString("$uniqueid");
			this.addChild(sID, new SubProjectTreeNode(aworkitem,SUB_PROJECT));

		}
		
		logger.fine(" ProjectTree loadChildren ("
				+ (System.currentTimeMillis() - l) + " ms) ");

	}

}
