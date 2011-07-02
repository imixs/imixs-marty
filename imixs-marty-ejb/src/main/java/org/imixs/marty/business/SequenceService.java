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

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.imixs.workflow.ItemCollection;

/**
 * This EJB handles a unique Sequence Number over a group of workitems.
 * Therefore the ejb loads the parent Workitem for a given Workitem to load and
 * update a unique sequence number. If the given workitem has no $unqiueIDRef
 * the ejb will throw an exception!
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

@DeclareRoles( { "org.imixs.ACCESSLEVEL.NOACCESS",
	"org.imixs.ACCESSLEVEL.READERACCESS",
	"org.imixs.ACCESSLEVEL.AUTHORACCESS",
	"org.imixs.ACCESSLEVEL.EDITORACCESS",
	"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed( { "org.imixs.ACCESSLEVEL.NOACCESS",
	"org.imixs.ACCESSLEVEL.READERACCESS",
	"org.imixs.ACCESSLEVEL.AUTHORACCESS",
	"org.imixs.ACCESSLEVEL.EDITORACCESS",
	"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Stateless
@LocalBean
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
public class SequenceService  {

	private final static String SEQUENCE_NAME = "numLastSequenceNummer";
	

	// Persistence Manager
	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;
	


	/**
	 * this method computes the next sequence number and updates the parent
	 * workitem where the last sequence number will be stored.
	 */
	//@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public int getNextSequenceNumber(ItemCollection aworkitem) throws Exception {
		// load current Number
		ItemCollection sequenceNumberObject = loadSequenceNumberItemCollection(aworkitem);
		int sequenceNumber = sequenceNumberObject
				.getItemValueInteger(SEQUENCE_NAME);
		sequenceNumber++;
		// Save new Number
		sequenceNumberObject.replaceItemValue(SEQUENCE_NAME,
				new Integer(sequenceNumber));
		entityService.save(sequenceNumberObject);
		return sequenceNumber;

	}

	/**
	 * load the current Seuqecnce Number from the parent worktiem to the given
	 * aworkitem. The Method throws an exception if the workitem has no parent
	 * workitem!
	 */
	public int getLastSequenceNumber(ItemCollection aworkitem) throws Exception {
		// load current Number
		ItemCollection sequenceNumberObject = loadSequenceNumberItemCollection(aworkitem);
		int sequenceNumber = sequenceNumberObject
				.getItemValueInteger(SEQUENCE_NAME);
		return sequenceNumber;

	}

	/**
	 * sets the current sequence Number
	 */
	public void setLastSequenceNumber(ItemCollection aworkitem, int aNewID)
			throws Exception {
		// load current Number
		ItemCollection sequenceNumberObject = loadSequenceNumberItemCollection(aworkitem);
		// Save new Number
		sequenceNumberObject.replaceItemValue(SEQUENCE_NAME,
				new Integer(aNewID));
		entityService.save(sequenceNumberObject);

	}

	/**
	 * this method loads the SequenceNumber Entity form the parent workitem of
	 * the given aworkitem
	 * 
	 * @return
	 * @throws Exception
	 */
	private ItemCollection loadSequenceNumberItemCollection(
			ItemCollection aworkitem) throws Exception {
		String sParentID = aworkitem.getItemValueString("$UniqueIDRef");

		if ("".equals(sParentID))
			throw new Exception(
					"WARNING: SequenceServiceBean : No $UniqueIDRef defined");

		ItemCollection parent = entityService.load(sParentID);

		return parent;
	}

}
