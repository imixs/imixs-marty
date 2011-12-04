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

package org.imixs.marty.model;

import java.util.Iterator;
import java.util.List;

import javax.faces.context.FacesContext;

import org.imixs.marty.web.project.ProjectMB;
import org.imixs.workflow.ItemCollection;
import org.richfaces.model.TreeNodeImpl;

/**
 * This TreeNodeImplemenation is used to compute the startProcess Notes only if
 * the parent TreeNode was expanded. This is for better performance. To compute
 * the startProcesses multiple lookups via modelService are necessary. But the
 * lookup is only necessary if a node is expanded!
 * 
 * @author rsoika
 * 
 */
public class ProcessGroupsTreeNode extends TreeNodeImpl {
	/* Project Backing Bean */
	private ProjectMB projectBean = null;

	private boolean childsLoaded = false;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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
		return false;
	}

	/**
	 * This method returns a list of SelectItems with the start Process Entity
	 * for each Process Group. The Model Version depends on the current
	 * selection of ModelLanguage and ModelStyle
	 * 
	 * SelectItem.Value = modelversion|processid SelectItem.Lable = Groupname
	 * 
	 * The Method uses a caching mechanism as the lookup for all projectgroups
	 * inside the model can take a long time
	 * 
	 * To select the ProcessEntites the current selection of ModelLanguage and
	 * ModelStyle are relevant! This method is used by the UI element
	 * project.xhtml to show the user the possible processes to be selected for
	 * a project
	 * 
	 * The StratProcess Entity can not be identified by the $unqiueID because
	 * this attriubte can change after a Model Update!
	 * 
	 * 
	 * The method did not load processGrups which are "SubProcessGroups"! A
	 * Subprocess Group is defined by a '~' character contained in the
	 * Groupname.! SubprocessGroups are used in complex workflows where a
	 * Mainprocess supports Child Process Entities defined by subprocess. e.g.
	 * 'Ticketsystem' -> 'Ticketsystem~Knowledebase'
	 * 
	 * @return
	 * @throws Exception
	 */
	public void loadChildren() throws Exception {
		String version = ((ModelData) this.getData()).version;
		// read processlist first time....
		// startProcessSelection = new ArrayList<SelectItem>();
		List<ItemCollection> startProcessList = this.getProjectBean()
				.getModelService().getAllStartProcessEntitiesByVersion(version);

		Iterator<ItemCollection> iter = startProcessList.iterator();
		while (iter.hasNext()) {
			ItemCollection processEntity = iter.next();
			String sGroupName = processEntity
					.getItemValueString("txtWorkflowGroup");

			// the process will not be added if it is a SubprocessGroup indicated
			// by a '~' char
			if (sGroupName.contains("~"))
				continue;
			
			int iProccessID = processEntity.getItemValueInteger("numProcessID");

			// add new language Node
			TreeNodeImpl nodeProcess = new TreeNodeImpl();
			
			// optimize groupName with (version)
			sGroupName=sGroupName+" (" + version.substring(version.lastIndexOf("-")+1) + ")";
			ModelData md = new ModelData(ModelData.MODEL_PROCESS, version,
					sGroupName , iProccessID);
			nodeProcess.setData(md);

			this.addChild(md.getNodeID(), nodeProcess);

		}

	}

}
