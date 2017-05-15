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

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * This EJB handles a unique Sequence Number over a group of workitems.
 * Therefore the ejb loads the parent Workitem for a given Workitem to load and
 * update a unique sequence number. If the given workitem has no $unqiueIDRef
 * the ejb will throw an exception!
 * 
 * The Method getNextSequenceNumberByGroup computes the sequence number based on
 * a configuration entity with the name "BASIC". The configuration provides a
 * property 'sequencenumbers' with the current number range for each
 * workflowGroup.
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
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
public class SequenceService {

	private static Logger logger = Logger.getLogger(SequenceService.class.getName());

	public final static String SEQUENCE_NAME = "numLastSequenceNummer";
	
	public final static String SEQUENCE_ERROR = "MISSING_UNIQUEIDREF";

	// Persistence Manager
	@EJB
	DocumentService documentService;

	/**
	 * This method computes the sequecne number based on a configuration entity
	 * with the name "BASIC". The configuration provides a property
	 * 'sequencenumbers' with the current number range for each workflowGroup.
	 * If a Workitem have a WorkflowGroup with no corresponding entry the method
	 * will not compute a new number.
	 * 
	 * @throws InvalidWorkitemException
	 * @throws AccessDeniedException
	 * 
	 */
	public long getNextSequenceNumberByGroup(ItemCollection aworkitem)
			throws AccessDeniedException {

		ItemCollection configItemCollection = null;
		
		String sQuery="(type:\"configuration\" AND txtname:\"BASIC\")";
		
		Collection<ItemCollection> col;
		try {
			col = documentService.find(sQuery,1, 0);
		} catch (QueryException e) {
			throw new InvalidAccessException(InvalidAccessException.INVALID_ID,e.getMessage(),e);
		}

		if (col.size() > 0) {
			configItemCollection = col.iterator().next();

			// read configuration and test if a corresponding configuration
			// exists
			String sWorkflowGroup = aworkitem
					.getItemValueString(WorkflowKernel.WORKFLOWGROUP);
			@SuppressWarnings("unchecked")
			List<String> vNumbers = configItemCollection
					.getItemValue("sequencenumbers");
			for (int i = 0; i < vNumbers.size(); i++) {
				String aNumber = vNumbers.get(i);
				if (aNumber.startsWith(sWorkflowGroup + "=")) {
					// we got the next number....
					String sequcenceNumber = aNumber.substring(aNumber
							.indexOf('=') + 1);
					//
					long currentSequenceNumber = Long
							.parseLong(sequcenceNumber);
					long newSequenceNumber = currentSequenceNumber + 1;
					// Save the new Number back into the config entity
					aNumber = sWorkflowGroup + "=" + newSequenceNumber;
					vNumbers.set(i, sWorkflowGroup + "=" + newSequenceNumber);
					configItemCollection.replaceItemValue("sequencenumbers",
							vNumbers);
					documentService.save(configItemCollection);
					// return the new number
					return currentSequenceNumber;
				}

			}

		} else {
			logger.warning("No BASIC configuration found!");
		}

		return 0;
	}

	/**
	 * this method computes the next sequence number and updates the parent
	 * workitem where the last sequence number will be stored.
	 * 
	 * @throws AccessDeniedException
	 * @throws PluginException 
	 */
	public long getNextSequenceNumberByParent(ItemCollection aworkitem)
			throws AccessDeniedException, PluginException {
		// load current Number
		ItemCollection sequenceNumberObject = loadParentWorkitem(aworkitem);
		long currentSequenceNumber = sequenceNumberObject
				.getItemValueLong(SEQUENCE_NAME);

		// skip number 0
		if (currentSequenceNumber == 0)
			currentSequenceNumber = 1;

		long sequenceNumber = currentSequenceNumber;
		sequenceNumber++;
		// Save new Number
		sequenceNumberObject.replaceItemValue(SEQUENCE_NAME, new Long(
				sequenceNumber));
		documentService.save(sequenceNumberObject);
		return currentSequenceNumber;

	}

	/**
	 * load the current Seuqecnce Number from the parent worktiem to the given
	 * aworkitem. The Method throws an exception if the workitem has no parent
	 * workitem!
	 */
	public long getLastSequenceNumber(ItemCollection aworkitem) throws Exception {
		// load current Number
		ItemCollection sequenceNumberObject = loadParentWorkitem(aworkitem);
		long sequenceNumber = sequenceNumberObject
				.getItemValueLong(SEQUENCE_NAME);
		return sequenceNumber;

	}

	/**
	 * sets the current sequence Number
	 */
	public void setLastSequenceNumber(ItemCollection aworkitem, long aNewID)
			throws Exception {
		// load current Number
		ItemCollection sequenceNumberObject = loadParentWorkitem(aworkitem);
		// Save new Number
		sequenceNumberObject.replaceItemValue(SEQUENCE_NAME,
				new Long(aNewID));
		documentService.save(sequenceNumberObject);

	}

	/**
	 * this method loads the parent Workitem of the given workitem
	 * 
	 * @return
	 * @throws PluginException 
	 */
	private ItemCollection loadParentWorkitem(ItemCollection aworkitem) throws PluginException {
		String sParentID = aworkitem.getItemValueString("$UniqueIDRef");

		if ("".equals(sParentID))
			throw new PluginException(SequenceService.class.getName(),SEQUENCE_ERROR,
					"WARNING: SequenceService : No $UniqueIDRef defined");

		ItemCollection parent = documentService.load(sParentID);

		return parent;
	}

}
