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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.inject.Named;

import org.imixs.marty.profile.UserController;
import org.imixs.marty.project.ProjectController;
import org.imixs.marty.util.SelectItemComparator;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.ModelService;


/**
 * This backing beans provides informations about the Models provided in the
 * current workflow Instance.
 * 
 * There are two types of models handled. The System Model (used for project
 * management or Profile Settings) and the general Models used in a typical
 * workflow
 * 
 * A latest System Model depends on the current user locale. The general models
 * are language independent.
 * 
 * A ModelVersion is always expected in the format
 * 
 * 'domain-lang-version'
 * 
 * e.g. office-de-0.1, support-en-2.0, system-de-0.0.1
 * 
 * 
 * The Model Format handled by the ModelVersionHanler which is used by the
 * MyProfileMB is different from this format and was used in earlier versions of
 * ShareYourWork. We hope that we can drop the ModelVersionHandler sometimes....
 * 
 * @see org.imixs.marty.profile.UserController
 * @author rsoika
 * 
 */
@Named("modelMB")
@SessionScoped
public class ModelMB implements Serializable {

	private static final long serialVersionUID = 1L;
	private UserController myProfileMB = null;
	
	private ProjectController projectMB = null;
	private String latestSystemModelVersion = null;

	private ArrayList<SelectItem> workflowGroupSelection = null;
	private ArrayList<SelectItem> workflowGroupSelectionByProfile = null;
	private ArrayList<SelectItem> startProcessSelection = null;

	private HashMap modelVersionCache = null;
	private HashMap processEntityCache = null;

	
	/* Model Service */
	@EJB
	ModelService modelService;


	private static Logger logger = Logger.getLogger("org.imixs.workflow");

	/**
	 * The init method is used load all model versions and store the latest
	 * version of a model domain into a list.
	 * <p>
	 * The model Version is either expected in the format:
	 * <p>
	 * DOMAIN-LANGUAGE-VERSIONNUMBER
	 * <p>
	 * or in the format:
	 * <p>
	 * DOMAIN-LANGUAGE-SUBDOMAIN-VERSIONNUMBER
	 * <p>
	 * e.g. office-de-0.0.1, public-de-standard-0.0.1
	 * <p>
	 * where in the second example was used in older implementations.
	 * 
	 * <p>
	 * The modelCache uses the first part (without the version) as a key to find
	 * the latest version of a domain model. So the system can deal with
	 * multiple versions of the same domain.
	 * <p>
	 * The method getStartProcessList reads the cached model versions. This
	 * method can also compare the modelversion to the userprofile settings. In
	 * this case the first part (domain) and the second token (language) are
	 * relevant.
	 * <p>
	 * if a Modelversion did not contains at least 3 tokens an warning will be
	 * thrown.
	 * 
	 * 
	 **/
	@PostConstruct
	public void init() {

		modelVersionCache = new HashMap();
		processEntityCache = new ProcessCache();

		List<String> col;
		try {
			col = modelService.getAllModelVersions();

			for (String aModelVersion : col) {

				// Test if the ModelVersion has the expected format
				// DOMAIN-LANG-VERSION
				StringTokenizer stFormat = new StringTokenizer(aModelVersion,
						"-");
				if (stFormat.countTokens() < 2) {
					// skip invalid format
					System.out.println("[WARNING] Invalid ModelFormat : "
							+ aModelVersion);
					System.out.println("This model can not be used!");
					continue;
				}

				// now we parse the model version to store always the latest
				// version into a cache. We cut the tokens from last to first!
				// This is the reason that we can also support model versions
				// with subdomains (e.g. 'public-de-office-001')
				// all this stuff is because of a unlucky modelversion format
				// from early versions
				String sJunk = aModelVersion;

				String sVersionPart = sJunk
						.substring(sJunk.lastIndexOf("-") + 1);
				String sMainPart = sJunk.substring(0, sJunk.lastIndexOf("-"));
				// the language is now the second token from the main part!
				stFormat = new StringTokenizer(sMainPart, "-");
				String sDomain = stFormat.nextToken();
				String sLanguage = stFormat.nextToken();

				// Check if modelversion is a System Model - latest version will
				// be
				// stored
				if ("system".equals(sDomain)
						&& sLanguage.equals(getProfileBean().getWorkitem()
								.getItemValueString("txtLocale"))) {
					// now store System Model is current version is newer than
					// last
					// stored model version

					if (latestSystemModelVersion == null
							|| latestSystemModelVersion
									.compareTo(aModelVersion) < 0)
						latestSystemModelVersion = aModelVersion;
					continue;

				}

				// system models of another languages should not be cached
				if ("system".equals(sDomain))
					continue;

				/*
				 * Now we have a general Model. So we test her if the Mainpart
				 * is later then the last version stored
				 */
				String lastModel = (String) modelVersionCache.get(sMainPart);
				if (lastModel == null || lastModel.compareTo(aModelVersion) < 0) {
					modelVersionCache.put(sMainPart, aModelVersion);

				}

			}
		} catch (Exception e) {
			// unable to compute model versions
			e.printStackTrace();
		}

	}

	/**
	 * returns a SelctItem Array containing all WorkflowGroups from all general
	 * model files.
	 * The method returns all WorkflowGroups independent if the user is able to start such a process.
	 * @see getWorkflowGroupsByUser
	 * 
	 * @return
	 */
	public ArrayList<SelectItem> getWorkflowGroups() {

		if (workflowGroupSelection == null) {
			// build new groupSelection
			workflowGroupSelection = new ArrayList<SelectItem>();
			Iterator iter = modelVersionCache.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry mapEntry = (Map.Entry) iter.next();
				String sModelVersionName = mapEntry.getValue().toString();
				List<String> groupCol = modelService
						.getAllWorkflowGroupsByVersion(sModelVersionName);
				for (String aGroupName : groupCol) {
					// the process will not be added if it is a SubprocessGroup
					// indicated
					// by a '~' char
					if (aGroupName.contains("~"))
						continue;

					workflowGroupSelection.add(new SelectItem(sModelVersionName
							+ "|" + aGroupName, aGroupName));
				}

			}
		}

		Collections.sort(workflowGroupSelection, new SelectItemComparator(
				FacesContext.getCurrentInstance().getViewRoot().getLocale(),
				true));

		return workflowGroupSelection;

	}

	/**
	 * returns a SelctItem Array containing all StartProcess Ids from all
	 * ProcessGroups from all general model files
	 * 
	 **/
	public ArrayList<SelectItem> getStartProcessList() {
		return getStartProcessList(false);
	}

	/**
	 * returns a SelctItem Array containing all StartProcess Ids from
	 * ProcessGroups containing the current user domain
	 * 
	 **/
	public ArrayList<SelectItem> getStartProcessListByUserProfile() {
		return getStartProcessList(true);
	}

	/**
	 * returns a SelctItem Array containing all StartProcess Ids from all
	 * ProcessGroups from all general model files
	 * 
	 * The Value will be a combination of the locale and the WorkflowGroup Name
	 * 
	 * @param bRestrictToUserProfile
	 *            - boolean value indicating if all models (true) should be
	 *            returned, or only models matching the language and domains form
	 *            the current user profile
	 * 
	 * @return ArrayList<SelectItem>
	 */
	private ArrayList<SelectItem> getStartProcessList(
			boolean bRestrictToUserProfile) {
		if (startProcessSelection == null) {
			// build new groupSelection
			startProcessSelection = new ArrayList<SelectItem>();

			// read userProfile Domain (add public if no domain specified
			List userDomains = this.getProfileBean().getWorkitem()
					.getItemValue("txtmodeldomain");
			if (userDomains.size() == 0
					|| "".equals(this.getProfileBean().getWorkitem()
							.getItemValueString("txtmodeldomain")))
				userDomains.add("public");

			String userLocale = this.getProfileBean().getWorkitem()
					.getItemValueString("txtlocale");

			// interate over all processgroups...
			Iterator iterGroups = modelVersionCache.entrySet().iterator();
			while (iterGroups.hasNext()) {
				Map.Entry mapEntry = (Map.Entry) iterGroups.next();
				String sModelVersionName = mapEntry.getValue().toString();

				// splitt Language and domain
				StringTokenizer st = new StringTokenizer(sModelVersionName,
						"-", false);
				String currentDomain = st.nextToken();
				String currentLanguage = st.nextToken();

				System.out.println("Model=" + sModelVersionName);
				if (bRestrictToUserProfile)
					System.out.println("userdomain=" + userDomains);

				// skip modelversion depending on UserProfile Domain list?
				if (bRestrictToUserProfile
						&& userDomains.indexOf(currentDomain) == -1)
					continue;
				// skip modelversion depending on UserProfile Lokale?
				if (bRestrictToUserProfile
						&& !userLocale.equals(currentLanguage))
					continue;

				// fetch all start process groups
				List<ItemCollection> startProcessList = modelService
						.getAllStartProcessEntitiesByVersion(sModelVersionName);

				Iterator<ItemCollection> iter = startProcessList.iterator();
				while (iter.hasNext()) {
					ItemCollection processEntity = iter.next();
					// the process will not be added if it is a SubprocessGroup
					// indicated
					// by a '~' char
					String sGroupName = processEntity
							.getItemValueString("txtWorkflowGroup");

					if (sGroupName.contains("~"))
						continue;

					int iProccessID = processEntity
							.getItemValueInteger("numProcessID");
					String sProcessName = processEntity
							.getItemValueString("txtName");

					String sValue = sModelVersionName + "|" + iProccessID;

					//String sLabel = sGroupName + " (" + currentLanguage + " / "
					//		+ currentDomain + ")";
					
					
					String sLabel = sGroupName + " (" + sModelVersionName+ ")";
					
					
					startProcessSelection.add(new SelectItem(sValue, sLabel));
				}
			}
		}

		Collections.sort(startProcessSelection, new SelectItemComparator(
				FacesContext.getCurrentInstance().getViewRoot().getLocale(),
				true));

		return startProcessSelection;

	}

	/**
	 * Returns a instance of the MBProfileMB. This ManagedBean can not be find
	 * during the constructor because the referenece of this bean is queried
	 * form the MyProfielMB itself
	 * 
	 * @return
	 */
	public UserController getProfileBean() {
		if (myProfileMB == null)
			myProfileMB = (UserController) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "myProfileMB");

		return myProfileMB;

	}

	
	public ProjectController getProjectMB() {
		if (projectMB == null)
			projectMB = (ProjectController) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "projectMB");
		return projectMB;
	}

	/**
	 * returns the internal cache map to lookup a process entity
	 * 
	 * @return
	 */
	public HashMap getProcessEntity() {
		return processEntityCache;
	}

	/**
	 * This class implements an internal Cache for Process Entities. The Class
	 * overwrites the get() Method and implements an lazy loading mechanism to
	 * load a process entity only once a time the entity was requested. After
	 * the first load the entity is cached internal so further get() calls are
	 * very fast.
	 * 
	 * The key value expected of the get() method is a string with
	 * modelVersion|ProcessID.
	 * 
	 * The Cache size is shrinked to a maximum of 30 process entities to be
	 * cached one time. This mechanism can be optimized later...
	 * 
	 * @author rsoika
	 * 
	 */
	class ProcessCache extends HashMap {
		final int MAX_SIZE = 30;

		/**
		 * returns a single value out of the ItemCollection if the key dos not
		 * exist the method will create a value automatical
		 */
		@SuppressWarnings("unchecked")
		public Object get(Object key) {
			ItemCollection process;
			String sProcessKey;

			// check if a key is a String....
			if (key == null || !(key instanceof String))
				return null;

			sProcessKey = (String) key;

			// 1.) try to get entity out from cache..
			process = (ItemCollection) super.get(key);
			if (process != null)
				// entity already known and loaded into the cache!....
				return process;

			logger.info(" ModelMB loadProcess Entity " + sProcessKey);

			// not yet cached...
			try {
				// now try to separate modelversion from process id ...
				if (sProcessKey.contains("|")) {
					String sProcessModelVersion = sProcessKey.substring(0,
							sProcessKey.indexOf('|'));
					String sProcessID = sProcessKey.substring(sProcessKey
							.indexOf('|') + 1);

					process = modelService.getProcessEntityByVersion(
							Integer.parseInt(sProcessID), sProcessModelVersion);

				} else
					throw new RuntimeException(
							" ModelMB invalid Process Identitfier: "
									+ sProcessKey
									+ " - modellversion|processid expected!");
			} catch (NumberFormatException e) {
				e.printStackTrace();
				process = null;
			} catch (Exception e) {
				e.printStackTrace();
				process = null;
			}

			// now put processEntity first time into the cache

			// if size > MAX_SIZE than remove first entry
			if (this.keySet().size() > MAX_SIZE) {
				Object oldesKey = this.keySet().iterator().next();
				System.out
						.println(" ModelMB maximum ProcessCacheSize exeeded remove : "
								+ oldesKey);
				this.remove(oldesKey);
			}

			this.put(key, process);
			return process;
		}

	}

}
