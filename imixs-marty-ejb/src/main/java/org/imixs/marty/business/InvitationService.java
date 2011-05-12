/*******************************************************************************
 *  Imixs IX Workflow Technology
 *  Copyright (C) 2003, 2008 Imixs Software Solutions GmbH,  
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
 *  
 *******************************************************************************/
package org.imixs.marty.business;

import javax.ejb.Remote;

import org.imixs.workflow.ItemCollection;

/**
 * This bean supports automatic acception of invitations The bean add the
 * current user to the project an workitem the invitation belongs to
 * 
 * 
 * @author rsoika
 * 
 */
@Remote
public interface InvitationService {

	/**
	 * This method updates the readacces of a workitem and all child workitems
	 * 
	 * @param aWorkitemID
	 *            ItemCollection representing the worktiem to be updated
	 * @param aUserName
	 *            username should be added to $readAccess
	 */
	public void updateWorkitemReadAccess(String aWorkitemID, String aUserName)
			throws Exception;

	/**
	 * Updates the readAccess and TeamList of a single Project
	 * 
	 * @param aProjectWorkitemID
	 * @param aUserName
	 * @throws Exception
	 */
	public void updateProjectReadAccess(String aProjectWorkitemID,
			String aUserName) throws Exception;

}
