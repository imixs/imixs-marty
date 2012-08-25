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

package org.imixs.marty.profile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;
import javax.inject.Named;

import org.imixs.marty.ejb.ProfileService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.faces.workitem.WorkflowController;


@Named("profileController")
@SessionScoped
public class ProfileController extends org.imixs.workflow.jee.faces.workitem.WorkflowController  implements Serializable {

	private static final long serialVersionUID = 1L;

	public final static int START_PROFILE_PROCESS_ID = 200;
	public final static int CREATE_PROFILE_ACTIVITY_ID = 5;

	/* Profile Service */
	@EJB
	ProfileService profileService;

	@PostConstruct
	public void init() {
		setType("profile");
		
	}
	

	/**
	 * This method creates a new Profile with the txtName attribute as username
	 * This method is used to allow the manual creation of profiles for
	 * administrative issues
	 * 
	 * @param event
	 * @throws Exception
	 */
	public String create()  {

		// prove workitem and verify txtname and txtusername
		try {
			// lowercase email to allow unique lookups
			String sName = getWorkitem().getItemValueString("txtName");
		
			ItemCollection newProfile= profileService.createProfile(
					START_PROFILE_PROCESS_ID, sName);

			newProfile.replaceItemValue("$ActivityID",
					CREATE_PROFILE_ACTIVITY_ID);
			newProfile=profileService.processProfile(newProfile);
			//  the Workitem Reference for the ItemCollectionAdapter....
			this.setWorkitem(newProfile);
			// reset views
			doReset(null);
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
			
			throw new ValidatorException(message);
		}
		
		return "";

	}

	

	
}