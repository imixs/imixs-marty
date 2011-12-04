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

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.plugins.AbstractPlugin;

/**
 * This Plugin generates a uniqueid for new workitems.
 * This is usefull if a worklfow activity needs a $unqiueID also for a new Workitem.
 * Normaly the $unqiueid is generated by the workflowmanager after plugins are finished.
 * 
 * 
 * @author rsoika
 *
 */
public class SywappUniqueIDPlugin extends AbstractPlugin {

	public int run(ItemCollection documentContext, ItemCollection arg1) throws Exception {

		// püfen ob $UniqueID exisitiert. Falls nicht wird durch dieses Plugin eine erzeugt
		String auniqueid = documentContext.getItemValueString("$UniqueID");
		if ("".equals(auniqueid)) {
			documentContext.replaceItemValue("$UniqueID", this.generateUniqueID());
		}
		
		return Plugin.PLUGIN_OK;
	}

	/**
	 * generates a uniquid 
	 * @see http://www.imixs.org/jee/xref/org/imixs/workflow/jee/jpa/Entity.html
	 * @return
	 */
	private String generateUniqueID() {
		/*
		 * Generate uniqueid
		 */
		String sIDPart1 = Long.toHexString(System.currentTimeMillis());
			double d = Math.random() * 900000000;
		int i = new Double(d).intValue();
		String sIDPart2 = Integer.toHexString(i);
		return  sIDPart1 + "-" + sIDPart2;
	}

	public void close(int status) throws Exception {
		
	}

}
