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
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.imixs.marty.config.SetupMB;
import org.imixs.marty.ejb.ProjectService;
import org.imixs.marty.profile.MyProfileMB;
import org.imixs.marty.workflow.WorkflowController;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.ModelService;

/**
 * The ProjectlistMB is the Backing Bean for the sywapps UI Frontend. The class
 * did support methods to load Projects for the current user and also it
 * supports methods to load the StartProcessList for each project the user
 * selects.
 * 
 * The StartProcessLists for a current Project can be lookuped by the Ajax
 * Frontend for each project displayed in the UI. But the StartProcessList for a
 * project is lazy loaded. See inner class StartProcessCache
 * 
 * @author rsoika
 * 
 */
@ManagedBean
@SessionScoped
public class ProjectlistMB implements Serializable{

	
	private static final long serialVersionUID = 1L;

	private ArrayList<SelectItem> myProjectSelection = null;

	private StartProcessCache startProcessList;
	private SubProcessCache subProcessList;
	private ArrayList<ItemCollection> projects = null;
	private ArrayList<ItemCollection> startProjects = null;
	private int count = 10;
	private int row = 0;
	private boolean endOfList = false;
	private boolean selectMainProjects = false;

	/* EJBs */
	@EJB
	ProjectService projectService;
	@EJB
	ModelService modelService;
	@EJB
	EntityService entityService;

	/* Backing Beans */
	@ManagedProperty(value = "#{workflowController}")
	private WorkflowController workflowController = null;

	@ManagedProperty(value = "#{projectMB}")
	private ProjectMB projectMB = null;

	@ManagedProperty(value = "#{myProfileMB}")
	private MyProfileMB myProfileMB = null;

	@ManagedProperty(value = "#{setupMB}")
	private SetupMB setupMB = null;

	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	public ProjectlistMB() {
		super();
	}

	@PostConstruct
	public void init() {
		startProcessList = new StartProcessCache();
		subProcessList = new SubProcessCache();

		count = this.setupMB.getWorkitem().getItemValueInteger(
				"MaxviewEntriesPerPage");
	}

	public WorkflowController getWorkflowController() {
		return workflowController;
	}

	public void setWorkflowController(WorkflowController workitemMB) {
		this.workflowController = workitemMB;
	}

	public ProjectMB getProjectMB() {
		return projectMB;
	}

	public void setProjectMB(ProjectMB projectMB) {
		this.projectMB = projectMB;
	}

	public MyProfileMB getMyProfileMB() {
		return myProfileMB;
	}

	public void setMyProfileMB(MyProfileMB myProfileMB) {
		this.myProfileMB = myProfileMB;
	}

	public SetupMB getSetupMB() {
		return setupMB;
	}

	public void setSetupMB(SetupMB setupMB) {
		this.setupMB = setupMB;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public void setRow(int row) {
		this.row = row;
	}

	/**
	 * This Method Selects the current project and refreshes the Worklist Bean
	 * so wokitems of this project can be displayed
	 * 
	 * @return
	 */
	public void doSwitchToProject(ActionEvent event) {

		if (event == null) {
			projectMB.setWorkitem(null);
			return;
		}

		ItemCollection currentSelection = null;
		// find current data row....
		UIComponent component = event.getComponent();

		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {

			if (!(parent instanceof UIData))
				continue;

			try {
				// get current project from row
				currentSelection = (ItemCollection) ((UIData) parent)
						.getRowData();

				if (currentSelection.getItemValueString("type").equals(
						"project")) {
					projectMB.setWorkitem(currentSelection);
					break;
				} else {
					projectMB.setWorkitem(null);
					break;
				}
			} catch (Exception e) {
				// unable to select data
			}
		}

	}

	/**
	 * This Method Selects the current project and creates a list of start
	 * ProcessIDs. This ids will be displayed inside the ViewEntry of a single
	 * Project of the myprojects.xhtml
	 * 
	 * The Method uses a caching mechanism as the lookup for all projectgroups
	 * inside the model can take a long time
	 * 
	 * 
	 * 
	 * @return
	 * @throws Exception
	 */

	public void doToggleProcessList(ActionEvent event) throws Exception {

		ItemCollection currentSelection = null;
		// suche selektierte Zeile....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// get current project from row
			currentSelection = (ItemCollection) ((UIData) parent).getRowData();

			// now try to read current toogle state and switch state
			boolean bTogle = false;
			if (currentSelection.hasItem("a4j:showProcessList")) {
				try {
					boolean bTogleCurrent = (Boolean) currentSelection
							.getItemValue("a4j:showProcessList").get(0);
					bTogle = !bTogleCurrent;
				} catch (Exception e) {
					bTogle = true;
				}
			} else
				// item did not exist yet....
				bTogle = true;

			// update projectMB
			projectMB.setWorkitem(currentSelection);

			currentSelection.replaceItemValue("a4j:showprocesslist", bTogle);
			break;
		}

	}

	/**
	 * this method toogles the ajax attribute a4j:showTeam. This Attriubte is
	 * used to display the Teamlist inside the Porject View.
	 * 
	 * @return
	 */
	public void doToggleTeam(ActionEvent event) throws Exception {
		ItemCollection currentSelection = null;
		// suche selektierte Zeile....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// workitem found
			currentSelection = (ItemCollection) ((UIData) parent).getRowData();

			// now try to read current toogle state and switch state
			boolean bTogle = false;
			if (currentSelection.hasItem("a4j:showTeam")) {
				try {
					boolean bTogleCurrent = (Boolean) currentSelection
							.getItemValue("a4j:showTeam").get(0);
					bTogle = !bTogleCurrent;
				} catch (Exception e) {
					bTogle = true;
				}
			} else
				// item did not exist yet....
				bTogle = true;
			currentSelection.replaceItemValue("a4j:showteam", bTogle);
			break;

		}
	}

	/**
	 * Selects the current project and opens the Project Form for editing
	 * 
	 * The Method also updates the $modelVersion of the Project to the latest
	 * System Version in the user Language. So Workflow Activities will always
	 * be displayed in the user language
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doEdit(ActionEvent event) throws Exception {
		ItemCollection currentSelection = null;
		// suche selektierte Zeile....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// Zeile gefunden
			currentSelection = (ItemCollection) ((UIData) parent).getRowData();

			// remove a4j: attributes generated inside the viewentries by the UI
			currentSelection.getAllItems().remove("a4j:showteam");
			currentSelection.getAllItems().remove("a4j:showprocesslist");

			Locale userLocale = FacesContext.getCurrentInstance().getViewRoot()
					.getLocale();

			// set default language if not set - only necessary during migration
			String sModelLanguage = currentSelection
					.getItemValueString("txtModelLanguage");
			if ("".equals(sModelLanguage)) {
				currentSelection.replaceItemValue("txtModelLanguage",
						userLocale.getLanguage());
			}

			// determine user language and set Modelversion depending on the
			// selected user locale
			String sModelVersion = myProfileMB.getModelVersionHandler()
					.getLatestSystemVersion(userLocale.getLanguage());
			currentSelection.replaceItemValue("$modelversion", sModelVersion);

			projectMB.setWorkitem(currentSelection);
			break;

		}

	}

	/**
	 * Selects the current project and deletes the Project and its subprojects
	 * and workitems by changing the attribute type' into 'workitemdeleted'
	 * 
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doSoftDelete(ActionEvent event) throws Exception {
		ItemCollection currentSelection = null;
		// search current row....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// Zeile gefunden
			currentSelection = (ItemCollection) ((UIData) parent).getRowData();

			this.projectMB.getProjectService().moveIntoDeletions(
					currentSelection);

			this.doReset(event);

			break;

		}

	}

	/**
	 * Selects the current project and creates a subproject based on the
	 * selected. A Subproject begins with the name of the parent project
	 * followed by a '.'
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doCreateSubproject(ActionEvent event) throws Exception {
		ItemCollection currentSelection = null;
		// suche selektierte Zeile....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// Zeile gefunden
			currentSelection = (ItemCollection) ((UIData) parent).getRowData();
			projectMB.setWorkitem(currentSelection);
			projectMB.doCreateSubproject(event);
			break;

		}

	}

	/**
	 * resets the current project list and projectMB and reset the Row count
	 * back to 0
	 * 
	 * @return
	 */
	public void doReset(ActionEvent event) {
		doRefresh(event);
		row = 0;
	}

	/**
	 * resets the current project list and projectMB but start pos will not be
	 * changed!
	 * 
	 * called by the ProjectMB after a process Action event. So the goal is that
	 * the user will stay on last projectlist page.
	 * 
	 * @return
	 */
	public void doRefresh(ActionEvent event) {
		projectMB.setWorkitem(null);
		projects = null;
		myProjectSelection = null;

	}

	public void doLoadNext(ActionEvent event) {
		row = row + count;
		// loadProjectList();
		projects = null;
	}

	public void doLoadPrev(ActionEvent event) {
		row = row - count;
		if (row < 0)
			row = 0;

		// loadProjectList();
		projects = null;
	}

	public List<ItemCollection> getProjects() {
		selectMainProjects = false;
		if (projects == null)
			loadProjectList();
		return projects;

	}

	public List<ItemCollection> getMainProjects() {
		selectMainProjects = true;
		if (projects == null)
			loadProjectList();
		return projects;
	}

	/**
	 * returns a project list where the current user is owner
	 * 
	 * @return
	 */
	public List<ItemCollection> getMyProjects() {
		if (projects == null)
			loadMyProjectList();
		return projects;
	}

	/**
	 * returns a project list where the current user is member
	 * 
	 * @return
	 */
	public List<ItemCollection> getMemberProjects() {
		if (projects == null)
			loadMemberProjectList();
		return projects;
	}

	/**
	 * returns a project list where the type is 'projectdeleted'
	 * 
	 * @return
	 */
	public List<ItemCollection> getDeletedProjects() {
		ArrayList projectsDelted = new ArrayList<ItemCollection>();
		try {
			Collection<ItemCollection> col = null;
			long l = System.currentTimeMillis();

			String query = "SELECT project FROM Entity AS project "
					+ " WHERE project.type IN ('projectdeleted' ) "
					+ " ORDER BY project.modified DESC";
			col = entityService.findAllEntities(query, row, count);

			logger.fine("  loadDeletedProjectList ("
					+ (System.currentTimeMillis() - l) + " ms) ");

			endOfList = col.size() < count;
			for (ItemCollection aworkitem : col) {
				projectsDelted.add((aworkitem));
			}
		} catch (Exception ee) {
			projectsDelted = null;
			ee.printStackTrace();
		}
		return projectsDelted;

	}

	/**
	 * This method returns a list of all Projects with ProcessIDs defined and
	 * where the current User is a Member of. So these Projects represent the
	 * collection of Projects where the User can start a new task.
	 * 
	 * @return
	 */
	public List<ItemCollection> getStartProjects() {
		if (startProjects == null) {
			startProjects = new ArrayList<ItemCollection>();
			try {
				List<ItemCollection> col = null;
				long l = System.currentTimeMillis();
				col = projectService.findAllProjects(0, -1);

				logger.fine("  loadStartProjectList ("
						+ (System.currentTimeMillis() - l) + " ms) ");

				endOfList = col.size() < count;
				for (ItemCollection aworkitem : col) {
					// test if Project contains a ProcessList
					List<String> vprojectList = aworkitem
							.getItemValue("txtprocesslist");
					if (vprojectList.size() > 0
							&& this.projectMB.isMember(aworkitem))
						startProjects.add((aworkitem));
				}
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}
		return startProjects;

	}

	/**
	 * Loads the project list
	 * 
	 * boolean selectMainProjects indicates if only main projects should be
	 * loaded or all projects need to be loaded. The user frontend typically
	 * shows only the main projects
	 * 
	 * @return
	 */
	private List<ItemCollection> loadProjectList() {
		projects = new ArrayList<ItemCollection>();
		try {
			List<ItemCollection> col = null;
			long l = System.currentTimeMillis();
			if (selectMainProjects)
				col = projectService.findAllMainProjects(row, count);
			else
				col = projectService.findAllProjects(row, count);

			logger.fine("  loadProjectList ("
					+ (System.currentTimeMillis() - l) + " ms) ");

			endOfList = col.size() < count;
			for (ItemCollection aworkitem : col) {
				projects.add((aworkitem));
			}
		} catch (Exception ee) {
			projects = null;
			ee.printStackTrace();
		}
		return projects;
	}

	/**
	 * Loads the project list where the current user is owner
	 * 
	 * @return
	 */
	private List<ItemCollection> loadMyProjectList() {
		projects = new ArrayList<ItemCollection>();
		try {
			List<ItemCollection> col = null;
			long l = System.currentTimeMillis();
			col = projectService.findAllProjectsByOwner(row, count);

			logger.fine("  loadMyProjectList ("
					+ (System.currentTimeMillis() - l) + " ms) ");

			endOfList = col.size() < count;
			for (ItemCollection aworkitem : col) {
				projects.add((aworkitem));
			}
		} catch (Exception ee) {
			projects = null;
			ee.printStackTrace();
		}
		return projects;
	}

	/**
	 * Loads the project list where the current user is member of without public
	 * projects
	 * 
	 * @return
	 */
	private List<ItemCollection> loadMemberProjectList() {
		projects = new ArrayList<ItemCollection>();
		try {
			List<ItemCollection> col = null;
			long l = System.currentTimeMillis();
			col = projectService.findAllProjectsByMember(row, count);

			logger.fine("  loadMyProjectList ("
					+ (System.currentTimeMillis() - l) + " ms) ");

			endOfList = col.size() < count;
			for (ItemCollection aworkitem : col) {
				projects.add((aworkitem));
			}
		} catch (Exception ee) {
			projects = null;
			ee.printStackTrace();
		}
		return projects;
	}

	public int getRow() {
		return row;
	}

	public boolean isEndOfList() {
		return endOfList;
	}

	/**
	 * returns the startProcessList cache. This is a Map with the
	 * StartProcessList for a specific $uniqueID of a project The cache is build
	 * during the method call 'doToggleProcessList'
	 * 
	 * @return
	 */
	public Map getProcessList() throws Exception {
		return startProcessList;
	}

	/**
	 * resets the current ProcessList cache. this Method is called from the
	 * ProjectMB in method doProcess. The Process Cache is now invalid for other
	 * user sessions! Maybe this could be a problem if a team is online changing
	 * the process list. For other users in this case a logout is necessary
	 * 
	 * @throws Exception
	 */
	public void resetProcessList() throws Exception {
		startProjects = null;
		startProcessList = new StartProcessCache();
		subProcessList = new SubProcessCache();

	}

	/**
	 * returns the subProcessList cache. This is a Map with the StartProcessList
	 * for a specific modelversion|groupName.
	 * 
	 * @return
	 */
	public Map getSubProcessList() throws Exception {
		return subProcessList;
	}

	/**
	 * returns the full list of Porjects available to the current user
	 * 
	 * @return
	 */
	public ArrayList<SelectItem> getMyProjectSelection() throws Exception {

		if (myProjectSelection != null)
			return myProjectSelection;

		// load Project list only once...

		myProjectSelection = new ArrayList<SelectItem>();
		long l = System.currentTimeMillis();

		List<ItemCollection> col = projectService.findAllProjects(0, -1);

		logger.fine(" -------------- loadMyProjectList : "
				+ (System.currentTimeMillis() - l) + " ----------------- ");
		for (ItemCollection aworkitem : col) {

			String sID = aworkitem.getItemValueString("$uniqueID");
			String sName = aworkitem.getItemValueString("txtName");

			myProjectSelection.add(new SelectItem(sID, sName));

		}
		return myProjectSelection;
	}

	/**
	 * This class implements an internal Cache for the StartProcess Lists
	 * assigned to a project. The Class overwrites the get() Method and
	 * implements an lazy loading mechanism to load a startprocess List for
	 * project the first time the list was forced. After the first load the list
	 * is cached internal so further get() calls are very fast.
	 * 
	 * The key value expected of the get() method is a string with the $uniqueID
	 * of the corresponding project. The class uses the projectService EJB to
	 * load the informations of a project.
	 * 
	 * The Cache size is shrinked to a maximum of 30 projects to be cached one
	 * time. This mechanism can be optimized later...
	 * 
	 * @author rsoika
	 * 
	 */
	class StartProcessCache extends HashMap {
		HashMap processEntityCache;
		final int MAX_SIZE = 30;

		/**
		 * returns a single value out of the ItemCollection if the key dos not
		 * exist the method will create a value automatical
		 */
		@SuppressWarnings("unchecked")
		public Object get(Object key) {
			List<ItemCollection> startProcessList;

			// check if a key is a String....
			if (!(key instanceof String))
				return null;

			// 1.) try to get list out from cache..
			startProcessList = (List<ItemCollection>) super.get(key);
			if (startProcessList != null)
				// list already known and loaded into the cache!....
				return startProcessList;

			logger.fine(" -------------- loadProcessList for Project " + key
					+ "----------------- ");

			if (processEntityCache == null)
				processEntityCache = new HashMap();

			startProcessList = new ArrayList<ItemCollection>();
			// first load Project
			ItemCollection aProject = projectService
					.findProject(key.toString());

			if (aProject == null)
				return startProcessList;

			List<String> vprojectList = aProject.getItemValue("txtprocesslist");

			// load ModelVersion
			// String sProcessModelVersion = aProject
			// .getItemValueString("txtProcessModelVersion");
			// sProcessModelVersion = "public-de-general-0.0.1";
			// get StartProcessList first time and store result into cache...

			for (String aProcessIdentifier : vprojectList) {
				// try to get ProcessEntity form ProcessEntity cache
				ItemCollection itemColProcessEntity = (ItemCollection) processEntityCache
						.get(aProcessIdentifier);

				if (itemColProcessEntity == null) {
					// not yet cached...
					try {
						// now try to separate modelversion from process id ...
						if (aProcessIdentifier.contains("|")) {
							logger.fine(" -------------- loadProcessEntity into cache ----------------- ");

							String sProcessModelVersion = aProcessIdentifier
									.substring(0,
											aProcessIdentifier.indexOf('|'));
							String sProcessID = aProcessIdentifier
									.substring(aProcessIdentifier.indexOf('|') + 1);

							logger.fine(" -------------- Modelversion:"
									+ sProcessModelVersion
									+ " ----------------- ");
							logger.fine(" -------------- ProcessID:"
									+ sProcessID + " ----------------- ");

							itemColProcessEntity = modelService
									.getProcessEntityByVersion(
											Integer.parseInt(sProcessID),
											sProcessModelVersion);

						}
					} catch (NumberFormatException e) {
						e.printStackTrace();
						itemColProcessEntity = null;
					} catch (Exception e) {
						e.printStackTrace();
						itemColProcessEntity = null;
					}
					// put processEntity into cache
					if (itemColProcessEntity != null)
						processEntityCache.put(aProcessIdentifier,
								itemColProcessEntity);
				}
				if (itemColProcessEntity != null)
					startProcessList.add((itemColProcessEntity));
			}

			// now put startProcessList first time into the cache

			// if size > MAX_SIZE than remove first entry
			if (this.keySet().size() > MAX_SIZE) {
				Object oldesKey = this.keySet().iterator().next();

				System.out
						.println(" -------------- maximum CacheSize exeeded remove : "
								+ oldesKey);

				this.remove(oldesKey);
			}

			this.put(key, startProcessList);

			return startProcessList;
		}

	}

	/**
	 * This class implements an internal Cache for the SubProcess Lists assigned
	 * to a project. A Subprocess is indeicated by the '~' character in its
	 * group name. The SubProcessCache is similar to the StartProcessCache class
	 * 
	 * The key value expected of the get() method is a string with the $unqiueid
	 * of a workitem. From this worktiem the modelversion and a specific
	 * txtWorkflowGroup name of the the main process will be taken
	 * <p>
	 * e.g. public-standard-de-0.0.1 Ticketservice
	 * <p>
	 * The Method searches all start processIDs with txtWorkflowGroup names
	 * started with the given Groupname + '~'
	 * 
	 * 
	 * 
	 * @author rsoika
	 * 
	 */
	class SubProcessCache extends HashMap {
		// HashMap processEntityCache;
		final int MAX_SIZE = 30;

		/**
		 * returns a single value out of the ItemCollection if the key dos not
		 * exist the method will create a value automatical
		 */
		@SuppressWarnings("unchecked")
		public Object get(Object key) {
			List<ItemCollection> startProcessList;

			// check if a key is a String....
			if (!(key instanceof String))
				return null;

			// find modelversion and group name from workitem proivded by key
			// ($uniqueid)
			// if $uniueid is equals to current workitem than no lookup is
			// neede.

			// find workitem
			ItemCollection workitem = workflowController.getWorkitem();
			if (workitem == null
					|| !key.toString().equals(
							workitem.getItemValueString("$uniqueid"))) {
				// lookup of workitem is needed
				workitem = entityService.load(key.toString());
			}

			if (workitem == null)
				return null;
			// get modelversio and group name from given workitem
			String sModelVersion = workitem.getItemValueString("$modelVersion");
			String sGroupName = workitem.getItemValueString("txtWorkflowGroup");

			// now update the key
			key = sModelVersion + "|" + sGroupName;

			// 1.) try to get list out from cache..
			startProcessList = (List<ItemCollection>) super.get(key);
			if (startProcessList != null)
				// list already known and loaded into the cache!....
				return startProcessList;

			startProcessList = new ArrayList<ItemCollection>();

			System.out
					.println(" -------------- loadSubProcessList for ModelVersion "
							+ sModelVersion
							+ " and ProcessGroup "
							+ sGroupName
							+ "----------------- ");

			List<ItemCollection> aProcessList = modelService
					.getAllStartProcessEntitiesByVersion(sModelVersion);

			Iterator<ItemCollection> iter = aProcessList.iterator();
			while (iter.hasNext()) {
				ItemCollection processEntity = iter.next();
				String sSubGroupName = processEntity
						.getItemValueString("txtWorkflowGroup");

				// the process will not be added if it is a SubprocessGroup
				// Indicated by a '~' char
				if (!sSubGroupName.startsWith(sGroupName + "~"))
					continue;
				// subprocess maches!
				// add txtWorkflowSubGroup property
				try {
					processEntity
							.replaceItemValue("txtWorkflowSubGroup",
									sSubGroupName.substring(sSubGroupName
											.indexOf("~") + 1));
				} catch (Exception e) {

					e.printStackTrace();
				}

				startProcessList.add((processEntity));

			}

			// now put startProcessList first time into the cache

			// if size > MAX_SIZE than remove first entry
			if (this.keySet().size() > MAX_SIZE) {
				Object oldesKey = this.keySet().iterator().next();

				System.out
						.println(" -------------- maximum CacheSize exeeded remove : "
								+ oldesKey);

				this.remove(oldesKey);
			}

			this.put(key, startProcessList);

			return startProcessList;
		}

	}

}
