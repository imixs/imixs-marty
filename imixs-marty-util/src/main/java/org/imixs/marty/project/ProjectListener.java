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

package org.imixs.marty.project;

import java.util.EventListener;

import org.imixs.workflow.ItemCollection;

/**
 * This interface can be implemented by a managed bean to observe the status of
 * the wokitemMB. The WorkitemMB will fire different events on specific program
 * situations.
 *    
 * @author rsoika
 *  
 */
public interface ProjectListener extends EventListener {

	public void onProjectCreated(ItemCollection e);

	public void onProjectChanged(ItemCollection e);

	public void onProjectProcess(ItemCollection e);

	public void onProjectProcessCompleted(ItemCollection e);
	
	public void onProjectDelete(ItemCollection e);

	
	
	
}
