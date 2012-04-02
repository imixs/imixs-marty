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

package org.imixs.marty.ejb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.jpa.EntityIndex;

/**
 * This Service Facade encapsulates the magement of project business objects. A
 * Project is represented by a ItemCollection with the following Attributes:
 * 
 * txtName - Name of the Project (required) namteam - List of Teammembers
 * (required)
 * 
 * txtProjectReaders - list of readers (computed) txtProjectAuthors - list of
 * authors (computed)
 * 
 * keyPublic - boolean indicates if Project should be treated as a public
 * project (used by Plugsin)
 * 
 * Projects can be managed in a hirarchical structure. There for a Project can
 * become a SubProject to ParentProject. A ParentProject is referenced by the
 * $UnqiueIDRef.
 * 
 * During the ProcessMethod the attributes Team and Owner will be inherited from
 * an existing parent Project.
 * 
 * @see method processProjcect()
 * 
 * 
 * @see org.imixs.workflow.manik.project.ProjectService
 * @author rsoika
 * 
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS",
		"org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS",
		"org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
public class ProjectService {

	@Resource
	SessionContext ctx;

	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	// Workflow Manager
	@EJB
	org.imixs.workflow.jee.ejb.WorkflowService wm;
	ItemCollection workItem = null;

	private static Logger logger = Logger.getLogger("org.imixs.marty");

	/**
	 * Updates a Team Entity
	 */
	public ItemCollection createProject() throws Exception {

		workItem = new ItemCollection();
		workItem.replaceItemValue("type", "project");
		return workItem;
	}

	/**
	 * Updates a Team Entity
	 */
	public ItemCollection createProject(int initialProcessID) throws Exception {
		workItem = createProject();
		workItem.replaceItemValue("$processID", new Integer(initialProcessID));
		return workItem;
	}

	/**
	 * This method deletes a project and all containing workitems
	 * 
	 * This method can only be called by role
	 * org.imixs.ACCESSLEVEL.MANAGERACCESS!
	 * 
	 */
	@RolesAllowed({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
	public void deleteProject(ItemCollection aproject) throws Exception {
		workItem = aproject;
		deleteChilds(workItem);
		entityService.remove(workItem);
	}

	/**
	 * deletes all child workitmes of a workitem - recursive methode call
	 * 
	 * @param parent
	 */
	private void deleteChilds(ItemCollection parent) {
		try {
			String id = parent.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = wm.getWorkListByRef(id);
			for (ItemCollection aworkitem : col) {

				// recursive method call
				deleteChilds(aworkitem);
				// remove workitem
				entityService.remove(aworkitem);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * This method analysis an existing project and collects count of workitems
	 * This method can only be called by role
	 * org.imixs.ACCESSLEVEL.MANAGERACCESS!
	 * 
	 * @param aProject
	 */
	@RolesAllowed({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
	public void analyseProject(ItemCollection aProject) throws Exception {
		workItem = aProject;
		// reset count of workitems
		aProject.replaceItemValue("summaryWorkitemCount", 0);
		// reset last create date of workitems
		// creation date of project should be oldest date in tree!
		Date created = aProject.getItemValueDate("$created");
		aProject.replaceItemValue("summaryWorkitemLastModified", created);

		analyseChilds(aProject);
	}

	/**
	 * adds count of child workitems to the current project workitem and
	 * compares the last modified date to determine the last access to a
	 * workitem in this project
	 * 
	 * @param parent
	 */
	private void analyseChilds(ItemCollection parent) {

		try {
			int summaryWorkitemCount = workItem
					.getItemValueInteger("summaryWorkitemCount");
			Date summaryWorkitemLastCreation = workItem
					.getItemValueDate("summaryWorkitemLastModified");

			Calendar calLatest = Calendar.getInstance();
			calLatest.setTime(summaryWorkitemLastCreation);

			String id = parent.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = wm.getWorkListByRef(id);
			// update workitemcount
			summaryWorkitemCount += col.size();
			workItem.replaceItemValue("summaryWorkitemCount",
					summaryWorkitemCount);
			for (ItemCollection aworkitem : col) {
				// test if creation date is before last creation date
				Date modifiedCurrent = aworkitem.getItemValueDate("$modified");
				Calendar calCurrent = Calendar.getInstance();
				calCurrent.setTime(modifiedCurrent);
				if (calCurrent.after(calLatest)) {
					workItem.replaceItemValue("summaryWorkitemLastModified",
							modifiedCurrent);
					calLatest.setTime(modifiedCurrent);
				}
				// recursive method call
				analyseChilds(aworkitem);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * This method returns a project ItemCollection for a specified id Method
	 * returns null if no project exists for this uniqueid
	 * 
	 * @param id
	 * @return
	 */
	public ItemCollection findProject(String id) {

		return entityService.load(id);

	}

	/**
	 * This method returns a project ItemCollection for a specified name.
	 * Returns null if no project with the provided name was found
	 * 
	 */
	public ItemCollection findProjectByName(String aName) {
		String sQuery = "SELECT project FROM Entity AS project "
				+ " JOIN project.textItems AS t2"
				+ " WHERE  project.type = 'project' "
				+ " AND t2.itemName = 'txtname' " + " AND t2.itemValue = '"
				+ aName + "'";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				0, 1);
		if (col.size() > 0)
			return col.iterator().next();
		else
			return null;
	}

	/**
	 * Returns all projects
	 */
	public List<ItemCollection> findAllProjects(int row, int count) {
		ArrayList<ItemCollection> teamList = new ArrayList<ItemCollection>();
		String sQuery = "SELECT project FROM Entity AS project "
				+ " JOIN project.textItems AS t2"
				+ " WHERE  project.type = 'project' "
				+ " AND t2.itemName = 'txtname' "
				+ " ORDER BY t2.itemValue ASC";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		for (ItemCollection aworkitem : col) {
			teamList.add(aworkitem);
		}
		return teamList;
	}

	/**
	 * Returns a list of sub projects for the defined UniqueID
	 * 
	 * @param row
	 * @param count
	 * @return
	 */
	public List<ItemCollection> findAllSubProjects(String aID, int row,
			int count) {
		ArrayList<ItemCollection> subList = new ArrayList<ItemCollection>();
		String sQuery = "SELECT project FROM Entity AS project "
				+ " JOIN project.textItems AS r"
				+ " JOIN project.textItems AS n"
				+ " WHERE project.type = 'project'"
				+ " AND n.itemName = 'txtname' "
				+ " AND r.itemName='$uniqueidref'" + " AND r.itemValue = '"
				+ aID + "' " + " ORDER BY n.itemValue asc";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		for (ItemCollection aworkitem : col) {
			subList.add(aworkitem);
		}
		return subList;
	}

	/**
	 * Returns a list of all main projects
	 * 
	 * @param row
	 * @param count
	 * @return
	 */
	public List<ItemCollection> findAllMainProjects(int row, int count) {
		ArrayList<ItemCollection> subList = new ArrayList<ItemCollection>();
		String sQuery = "SELECT project FROM Entity AS project "
				+ " JOIN project.textItems AS n1"
				+ " JOIN project.textItems AS n2"
				+ " WHERE project.type = 'project'"
				+ " AND n1.itemName = 'txtname' "
				+ " AND n2.itemName = 'txtprojectname'"
				+ " AND n1.itemValue = n2.itemValue "
				+ " ORDER BY n1.itemValue asc";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		for (ItemCollection aworkitem : col) {
			subList.add(aworkitem);
		}
		return subList;
	}

	/**
	 * Returns a list of all projects where current user is onwer
	 * 
	 * @param row
	 * @param count
	 * @return
	 */
	public List<ItemCollection> findAllProjectsByOwner(int row, int count) {
		String aname = ctx.getCallerPrincipal().getName();

		ArrayList<ItemCollection> subList = new ArrayList<ItemCollection>();
		String sQuery = "SELECT project FROM Entity AS project "
				+ " JOIN project.textItems as owner"
				+ " JOIN project.textItems AS n1"
				+ " WHERE project.type = 'project'"
				+ " AND owner.itemName = 'namowner' AND owner.itemValue = '"
				+ aname + "'" + " AND n1.itemName = 'txtname' "
				+ " ORDER BY n1.itemValue asc";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		for (ItemCollection aworkitem : col) {
			subList.add(aworkitem);
		}
		return subList;
	}

	/**
	 * Returns a list of all projects where current user is member (listed in
	 * $readacess or $writeaccess)
	 * 
	 * @param row
	 * @param count
	 * @return
	 */
	public List<ItemCollection> findAllProjectsByMember(int row, int count) {
		String aname = ctx.getCallerPrincipal().getName();

		ArrayList<ItemCollection> subList = new ArrayList<ItemCollection>();
		String sQuery = "SELECT project FROM Entity AS project "
				+ " JOIN project.textItems AS n1"
				+ " JOIN project.readAccessList AS readaccess "
				+ " JOIN project.writeAccessList AS writeaccess "
				+ " WHERE project.type = 'project'"
				+ " AND ( readaccess.value IN ( '" + aname + "')"
				+ "      OR writeaccess.value IN ( '" + aname + "')) "
				+ " AND n1.itemName = 'txtname' "
				+ " ORDER BY n1.itemValue asc";

		Collection<ItemCollection> col = entityService.findAllEntities(sQuery,
				row, count);

		for (ItemCollection aworkitem : col) {
			subList.add(aworkitem);
		}
		return subList;
	}

	/**
	 * This Method returns a String Collection of Member names to a requested
	 * project. The Project Name is represented by the unique ID.
	 * 
	 * @param aproject
	 *            - uniqueid of an existing project
	 * @return String collection of Names (Team Members)
	 * 
	 */
	public Collection<String> getProjectTeam(String aproject) {
		Vector<String> team = null;
		ItemCollection aworkitem = entityService.load(aproject);
		if (aworkitem != null)
			team = aworkitem.getItemValue("namteam");

		return team;

	}

	/**
	 * Updates a Porject Entity Caller have to care about read and write access.
	 * 
	 */
	public void saveProject(ItemCollection aproject) throws Exception {
		// validateProject(aproject);
		try {
			aproject.replaceItemValue("type", "project");
			entityService.save(aproject);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Processes a Project. The method expects an ItemCollection with the data
	 * structure for a project. The Method generates new attributes
	 * 
	 * 'namProjectReaders' and 'namProjectAuthors'
	 * 
	 * These fields can be used in a corresponding workflow model to set the
	 * read and write Access. The Values of namProjectAuthors will be mapped to
	 * the namOwner attribute. The Values of namProjectReaders will be mapped to
	 * the namTeam + namOwner attribute.
	 * 
	 * If no namOwner was set - namCreator will be added as default Owner
	 * 
	 * If the Project holds a reference to a ParentProject the properties
	 * namTeam and namOwner of the parent Project will be mapped to the
	 * attributes 'namParentTeam' and 'namParentOwner'
	 * 
	 * A ParentProject is identified by the $UniqueIDRef attribute. The values
	 * of these attributes will also be added to the namProjectAuthors and
	 * namProjectReaders. So a ParentProjectTeam will become reader of the
	 * project and the ParentProjectOwners will become author of the project
	 * 
	 * If the attribute 'txtLocale' is provided by a project the method updates
	 * the $modelVersion to the current locale. With this feature it is possibel
	 * to change the language for a project.
	 * 
	 * If the property namTeam or namOwner is empty this method will fill in the
	 * current UserUame. This is necessary as long as a workflow model uses the
	 * projektTeam or projectOwners from a project to manage read/write access.
	 * If these fields are empty there are situations possible where the
	 * readAccess is empty and everybody can read a workItem
	 * 
	 * 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ItemCollection processProject(ItemCollection aeditproject)
			throws Exception {

		workItem = aeditproject;

		String sParentProjectID = workItem.getItemValueString("$uniqueidRef");
		if (sParentProjectID != null && !"".equals(sParentProjectID))
			updateParentProjectInformations(sParentProjectID);
		else
			workItem.replaceItemValue("txtName",
					workItem.getItemValueString("txtProjectName"));

		// Verify if namTeam or namOwner is empty!
		String remoteUser = ctx.getCallerPrincipal().getName();
		Vector vTeam = workItem.getItemValue("namTeam");
		Vector vOwner = workItem.getItemValue("namOwner");
		if (vTeam.size() == 0) {
			vTeam.add(remoteUser);
			workItem.replaceItemValue("namTeam", vTeam);
		}
		if (vOwner.size() == 0) {
			vOwner.add(remoteUser);
			workItem.replaceItemValue("namOwner", vOwner);
		}

		// set namFields for access handling (txtProjectReaders
		// txtProjectAuthors)
		Vector vProjectReaders = new Vector();
		Vector vProjectAuthors = new Vector();

		vProjectAuthors.addAll(workItem.getItemValue("namOwner"));
		vProjectAuthors.addAll(workItem.getItemValue("namParentOwner"));
		// if now owner is set - namcreator is owner per default
		if (vProjectAuthors.size() == 0)
			vProjectAuthors.add(workItem.getItemValueString("namCreator"));

		// compute ReadAccess
		vProjectReaders.addAll(workItem.getItemValue("namteam"));
		vProjectReaders.addAll(workItem.getItemValue("namParentTeam"));
		vProjectReaders.addAll(workItem.getItemValue("namOwner"));
		vProjectReaders.addAll(workItem.getItemValue("namParentOwner"));

		vProjectReaders.addAll(workItem.getItemValue("namManager"));
		vProjectReaders.addAll(workItem.getItemValue("namAssist"));
		// test if at lease one reader is defined....
		if (vProjectReaders.size() == 0)
			vProjectReaders.add(workItem.getItemValueString("namCreator"));

		workItem.replaceItemValue("namProjectReaders", vProjectReaders);
		workItem.replaceItemValue("namProjectAuthors", vProjectAuthors);
		workItem.replaceItemValue("type", "project");

		// test if the project provides a txtLocale
		String sProjectLocale = workItem.getItemValueString("txtLocale");
		if (!"".equals(sProjectLocale)) {
			try {
				String sModelVersion = workItem
						.getItemValueString("$modelVersion");
				// replace the project locale
				int iStart = sModelVersion.indexOf('-');
				int iEnd = sModelVersion.indexOf('-', iStart+1);

				sModelVersion = sModelVersion.substring(0, iStart+1)
						+ sProjectLocale + sModelVersion.substring(iEnd);
				workItem.replaceItemValue("$ModelVersion", sModelVersion);
			} catch (Exception em) {
				logger.severe("[ProjectService] processProject: unable to compute new $modelVersion based on txtLocale="
						+ sProjectLocale);
			}

		}

		// Process workitem...
		workItem = wm.processWorkItem(workItem);

		// now test if the parentName property of subprojects need to be updated
		// this is only necesarry if subprojects are found.
		List<ItemCollection> childProjectList = findAllSubProjects(
				workItem.getItemValueString("$Uniqueid"), 0, -1);
		String sProjectName = workItem.getItemValueString("txtName");
		for (ItemCollection aSubProject : childProjectList) {
			if (!sProjectName.equals(aSubProject
					.getItemValueString("txtparentname"))) {
				String sSubProjectName = aSubProject
						.getItemValueString("txtprojectname");
				aSubProject.replaceItemValue("txtparentname", sProjectName);
				aSubProject.replaceItemValue("txtname", sProjectName + "."
						+ sSubProjectName);
				try {
					this.entityService.save(aSubProject);
				} catch (Exception sube) {
					System.out.println("Subproject name could not be updated: "
							+ sube);
				}
			}
		}

		return workItem;

	}

	/**
	 * This method updates the Project Informations which are inherited from a
	 * parentProject. A ParenProject is referenced by the $UniqueIDRef
	 * 
	 * The Method adds the attriubtes
	 * 
	 * namParentTeam namParentOwner
	 * 
	 * @throws Exception
	 */
	private void updateParentProjectInformations(String sIDRef)
			throws Exception {
		ItemCollection parentProject = findProject(sIDRef);

		String sName = workItem.getItemValueString("txtProjectName");
		String sParentName = parentProject.getItemValueString("txtName");

		workItem.replaceItemValue("txtName", sParentName + "." + sName);

		workItem.replaceItemValue("namParentTeam",
				parentProject.getItemValue("namTeam"));
		workItem.replaceItemValue("namParentOwner",
				parentProject.getItemValue("namOwner"));
	}

	/**
	 * performs a soft delete
	 * 
	 */
	public ItemCollection moveIntoDeletions(ItemCollection workitem)
			throws Exception {
		if ("project".equals(workitem.getItemValueString("type"))
				|| "workitem".equals(workitem.getItemValueString("type"))
				|| "childworkitem".equals(workitem.getItemValueString("type"))) {

			String id = workitem.getItemValueString("$uniqueid");
			Collection<ItemCollection> col = wm.getWorkListByRef(id);
			for (ItemCollection achildworkitem : col) {
				// recursive method call
				moveIntoDeletions(achildworkitem);
			}
			workitem.replaceItemValue("type",
					workitem.getItemValueString("type") + "deleted");
			workitem = entityService.save(workitem);
		}

		return workitem;
	}
}
