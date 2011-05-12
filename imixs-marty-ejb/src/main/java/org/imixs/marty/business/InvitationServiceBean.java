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

import java.util.Collection;
import java.util.Vector;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.imixs.workflow.ItemCollection;

/**
 * This bean supports automatic acception of invitations The bean add the
 * current user to the project an workitem the invitation belongs to
 * 
 * This EJB should only run with manager access !
 * 
 * sun-ejb-jar.xml
 * <p>
 * <code>
    <ejb>
		<ejb-name>ScheduledWorkflowServiceImplementation</ejb-name>
		<jndi-name>
			ejb/ReklamationsmanagementScheduledWorkflowServiceImplementation
		</jndi-name>
		<principal><name>Glassfish</name></principal>
	</ejb>

 * </code>
 * 
 * @author rsoika
 * 
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
public class InvitationServiceBean implements InvitationService {

	// Persistence Manager
	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;
	// Workflow Manager
	@EJB
	org.imixs.workflow.jee.ejb.WorkflowService wm;

	/**
	 * this method updates the read Access list of a given workitem and also all
	 * child workitems The mehtod changes the read access only if a read access
	 * restriction exists.
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public void updateWorkitemReadAccess(String aWorkitemID, String aUserName)
			throws Exception {
		ItemCollection workitem = entityService.load(aWorkitemID);
		if (workitem != null) {
			processReadAccessUpdate(workitem, aUserName);
		}
	}
	
	
	
	/**
	 * this method updates the read Access list and Teamlist of a given Project
	 * Child Workitems will be ignored!
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public void updateProjectReadAccess(String aProjectWorkitemID, String aUserName)
			throws Exception {
		ItemCollection workitem = entityService.load(aProjectWorkitemID);
		if (workitem != null) {
			Vector namesList = workitem.getItemValue("$ReadAccess");
			if (namesList.size() > 0
					&& namesList.indexOf(aUserName) == -1) {
				namesList.add(aUserName);
				// update Workitem
				workitem.replaceItemValue("$ReadAccess", namesList);
			}
			// update TeamList
			namesList = workitem.getItemValue("namteam");
			if ( namesList.indexOf(aUserName) == -1) {
				namesList.add(aUserName);
				// update Workitem
				workitem.replaceItemValue("namteam", namesList);
			}
			
			// save project
			entityService.save(workitem);
			
			
		}
	}
	
	

	/**
	 * updates the readaccess of all child workitmes of a workitem - recursive methode call
	 * 
	 * @param parent
	 * @throws Exception
	 */
	private void processReadAccessUpdate(ItemCollection parent, String aUserName)
			throws Exception {

		// first check child worktiems of current parent
		String id = parent.getItemValueString("$uniqueid");
		Collection<ItemCollection> col = wm.getWorkListByRef(id);
		for (ItemCollection aworkitem : col) {

			// recursive method call
			processReadAccessUpdate(aworkitem, aUserName);
			// update workitem
			try {
				Vector readerList = aworkitem.getItemValue("$ReadAccess");
				if (readerList.size() > 0
						&& readerList.indexOf(aUserName) == -1) {
					readerList.add(aUserName);
					// update Workitem
					aworkitem.replaceItemValue("$ReadAccess", readerList);
					entityService.save(aworkitem);
				}
			} catch (Exception e) {
				System.out
						.println("WARNING: Unable to updateWorkitemReadAccess: "
								+ id + " - " + aUserName);
				System.out.println(e.getMessage());
			}

		}
		// now update the current (parent) workitem itself
		try {
			Vector readerList = parent.getItemValue("$ReadAccess");
			if (readerList.size() > 0
					&& readerList.indexOf(aUserName) == -1) {
				readerList.add(aUserName);
				// update Workitem
				parent.replaceItemValue("$ReadAccess", readerList);
				entityService.save(parent);
			}
		} catch (Exception e) {
			System.out
					.println("WARNING: Unable to updateWorkitemReadAccess: "
							+ id + " - " + aUserName);
			System.out.println(e.getMessage());
		}
		
		
	}

	
	

}
