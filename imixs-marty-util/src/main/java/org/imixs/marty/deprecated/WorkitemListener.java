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

package org.imixs.marty.deprecated;

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
public interface WorkitemListener extends EventListener {

	public void onWorkitemCreated(ItemCollection e);

	public void onWorkitemChanged(ItemCollection e);

	public void onWorkitemProcess(ItemCollection e);

	public void onWorkitemProcessCompleted(ItemCollection e);
	
	public void onWorkitemDelete(ItemCollection e);

	public void onWorkitemDeleteCompleted();
	
	public void onWorkitemSoftDelete(ItemCollection e);

	public void onWorkitemSoftDeleteCompleted(ItemCollection e);

	public void onChildProcess(ItemCollection e);

	public void onChildProcessCompleted(ItemCollection e);

	public void onChildCreated(ItemCollection e);

	public void onChildDelete(ItemCollection e);

	public void onChildDeleteCompleted();
	
	public void onChildSoftDelete(ItemCollection e);

	public void onChildSoftDeleteCompleted(ItemCollection e);
}
