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

package org.imixs.marty.web.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;

import org.imixs.marty.business.ProfileService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.faces.AbstractWorkflowController;

public class ProfileMB extends AbstractWorkflowController {

	public final static int START_PROFILE_PROCESS_ID = 200;
	public final static int CREATE_PROFILE_ACTIVITY_ID = 5;
	private List<String> userRoles = null;

	/* Profile Service */
	@EJB
	ProfileService profileService;

	@PostConstruct
	public void init() {
		setType("profile");
		doSwitchToWorklistAll(null);
	}

	/**
	 * This method creates a new Profile with the txtName attribute as username
	 * This method is used to allow the manual createion of profiles for
	 * administrative issues
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doCreateProfile(ActionEvent event) throws Exception {

		// provess workitem and verify txtname and txtusername
		try {
			// lowercase email to allow unique lookups
			String sName = workitemItemCollection.getItemValueString("txtName");
			// sName = sName.toLowerCase();

			workitemItemCollection = profileService.createProfile(
					START_PROFILE_PROCESS_ID, sName);

			workitemItemCollection.replaceItemValue("$ActivityID",
					CREATE_PROFILE_ACTIVITY_ID);
			workitemItemCollection=profileService.processProfile(workitemItemCollection);
			//  the Workitem Reference for the ItemCollectionAdapter....
			this.setWorkitem(workitemItemCollection);
			// reset views
			doReset(event);
		} catch (Exception ee) {

			// Generate Error message
			FacesContext context = FacesContext.getCurrentInstance();
			UIViewRoot viewRoot = FacesContext.getCurrentInstance()
					.getViewRoot();
			Locale locale = viewRoot.getLocale();
			ResourceBundle rb = null;
			if (locale != null)
				rb = ResourceBundle.getBundle("bundle.profile", locale);
			else
				rb = ResourceBundle.getBundle("bundle.profile");

			String sMessage = rb.getString("displayname_error");
			FacesMessage message = new FacesMessage("* ", sMessage);
			context.addMessage("profile_form_id:user_id", message);
			//  the Workitem Reference for the ItemCollectionAdapter....
			this.setWorkitem(workitemItemCollection);
			throw new ValidatorException(message);
		}

	}

	/**
	 * This method is for saving and processing a profile using the
	 * profileService EJB
	 * 
	 * The method changes the workflow step form 10 to 20 if: $processID=200 &&
	 * keyagb="true"
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doProcess(ActionEvent event) throws Exception {
		// Activity ID raussuchen und in activityID speichern
		List children = event.getComponent().getChildren();
		int activityID = -1;
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) instanceof UIParameter) {
				UIParameter currentParam = (UIParameter) children.get(i);
				if (currentParam.getName().equals("id")
						&& currentParam.getValue() != null) {
					// activityID = (Integer) currentParam.getValue();

					Object o = currentParam.getValue();
					activityID = Integer.parseInt(o.toString());
					break;
				}
			}
		}

		// provess workitem and verify txtname and txtusername
		try {
			// lowercase email to allow unique lookups
			String sEmail = workitemItemCollection
					.getItemValueString("txtEmail");
			sEmail = sEmail.toLowerCase();
			workitemItemCollection.replaceItemValue("txtEmail", sEmail);
			workitemItemCollection.replaceItemValue("$ActivityID", activityID);
			workitemItemCollection = profileService
					.processProfile(workitemItemCollection);
		} catch (Exception ee) {

			// Generate Error message
			FacesContext context = FacesContext.getCurrentInstance();
			UIViewRoot viewRoot = FacesContext.getCurrentInstance()
					.getViewRoot();
			Locale locale = viewRoot.getLocale();
			ResourceBundle rb = null;
			if (locale != null)
				rb = ResourceBundle.getBundle("bundle.profile", locale);
			else
				rb = ResourceBundle.getBundle("bundle.profile");

			String sMessage = rb.getString("displayname_error");
			FacesMessage message = new FacesMessage("* ", sMessage);
			context.addMessage("profile_form_id:displayname_id", message);
			// context.renderResponse();
			throw new ValidatorException(message);
		}

		// and finally update the Workitem Reference for the
		// ItemCollectionAdapter....
		this.setWorkitem(workitemItemCollection);
		// reset views
		doReset(event);

	}

	public void doSwitchToProfile(ActionEvent event) throws Exception {
		// Activity ID raussuchen und in activityID speichern
		List children = event.getComponent().getChildren();
		String profileID = null;
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) instanceof UIParameter) {
				UIParameter currentParam = (UIParameter) children.get(i);
				if (currentParam.getName().equals("id")
						&& currentParam.getValue() != null) {
					// Value can be provided as String or Integer Object
					profileID = currentParam.getValue().toString();
					break;
				}
			}
		}
		if (profileID != null) {
			workitemItemCollection = this.profileService
					.findProfileByName(profileID);
			this.setWorkitem(workitemItemCollection);
		}
	}

	/**
	 * This method deletes the current selected profile a The Profile needs to
	 * be preselected by "editProfile()"
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doDeleteProfile(ActionEvent event) throws Exception {

		if (workitemItemCollection != null) {
			// delete current selection
			System.out.println("Deleting Profile: "
					+ workitemItemCollection.getItemValueString("$Uniqueid"));
			getEntityService().remove(workitemItemCollection);
		}
		doReset(event);
	}

	/**
	 * This method resets the current selected profile to status 200 and clears
	 * the keyAGB Flag
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doResetProfile(ActionEvent event) throws Exception {
		// Profile raussuchen
		ItemCollection currentSelection = null;
		// suche selektierte Zeile....
		UIComponent component = event.getComponent();

		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {

			if (!(parent instanceof UIData))
				continue;
			// get current project from row
			currentSelection = (ItemCollection) ((UIData) parent)
					.getRowData();

			setWorkitem(currentSelection);
			break;
		}

		if (currentSelection != null) {
			// Set status to 200 and clear AGB Flag
			workitemItemCollection.replaceItemValue("keyagb", false);

			workitemItemCollection.replaceItemValue("$ActivityID", 80);

			getWorkflowService().processWorkItem(workitemItemCollection);

		}
		doReset(event);
	}

	/**
	 * returns the current users roles
	 * 
	 * @return
	 */
	public List<String> getUserRoles() {
		try {
			ExternalContext ctx = FacesContext.getCurrentInstance()
					.getExternalContext();
			userRoles = new ArrayList<String>();

			String[] sRoleList = { "org.imixs.ACCESSLEVEL.NOACCESS",
					"org.imixs.ACCESSLEVEL.READERACCESS",
					"org.imixs.ACCESSLEVEL.AUTHORACCESS",
					"org.imixs.ACCESSLEVEL.EDITORACCESS",
					"org.imixs.ACCESSLEVEL.MANAGERACCESS", "SB", "ADMIN" };

			for (int i = 0; i < sRoleList.length; i++) {
				String aRole = sRoleList[i];
				if (ctx.isUserInRole(aRole))
					userRoles.add(aRole);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return userRoles;
	}

}