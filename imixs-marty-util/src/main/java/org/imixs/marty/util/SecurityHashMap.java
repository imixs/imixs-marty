/*******************************************************************************
 *  Imixs IX Workflow Technology
 *  Copyright (C) 2001, 2008 Imixs Software Solutions GmbH,  
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
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika
 *******************************************************************************/
package org.imixs.sywapps.util;

import java.util.HashMap;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

/**
 * This is a helper Class to check access level of a user
 * 
 * @author Ralph Soika
 *
 */
public class SecurityHashMap extends HashMap {
	public SecurityHashMap() {
		super();
	}

	public Object get(Object object) {
		ExternalContext ectx = FacesContext.getCurrentInstance()
				.getExternalContext();
		String isMember = "false";
		
		if (object != null) {
			
			if ("anonymous".equals(object.toString().toLowerCase()))
				return isAnonymous();

			isMember = ectx.isUserInRole((String) object) == true ? "true"
					: "false";
		}
		return new Boolean(isMember).booleanValue();
	}
	
	
	private boolean isAnonymous() {
		ExternalContext ectx = FacesContext.getCurrentInstance()
		.getExternalContext();

		String stype=ectx.getAuthType();
		return (stype==null);
	}
	
}
