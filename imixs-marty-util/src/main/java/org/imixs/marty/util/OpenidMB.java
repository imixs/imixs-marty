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

import java.util.List;

import javax.faces.component.UIParameter;
import javax.faces.event.ActionEvent;

/**
 * This Backing Bean acts as a OpenID Helper Class. 
 * This Bean is used to identify the OpenID Provider selected during the 
 * login process. This state is only for ui representation. It is not for authenticating user or processing the login throught the openID Provider
 * 
 *   @author rsoika
 * 
 */
public class OpenidMB {
	
	private String provider;

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	
	/**
	 * This method selects a provider 
	 * The mehtod expects a parameter 'provider' with the name (String) of the selected
	 * Provider.
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doSelectProvider(ActionEvent event) throws Exception {
		//provider="verisign";
		
		List children = event.getComponent().getChildren();
		String aProvider="";

		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) instanceof UIParameter) {
				UIParameter currentParam = (UIParameter) children.get(i);
				if (currentParam.getName().equals("provider")
						&& currentParam.getValue() != null) {
					aProvider =  currentParam.getValue().toString();
					provider= aProvider;
					break;
				}
			}
		}
		
	}
}
