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
package org.imixs.sywapps.business;

import javax.ejb.Remote;

import org.imixs.workflow.ItemCollection;

@Remote
public interface SequenceService {

	/**
	 * This method process a issue object using the IX WorkflowManager
	 * 
	 * @param aIssue ItemCollection representing the issue
	 * @param activityID activity ID the issue should be processed
	 */
	public int getNextSequenceNumber(ItemCollection aworkitem) throws Exception;
	
	
	public int getLastSequenceNumber(ItemCollection aworkitem) throws Exception;

	public void setLastSequenceNumber(ItemCollection aworkitem,int aID) throws Exception;

}
