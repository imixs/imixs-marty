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

package org.imixs.marty.workflow;

/**
 * This Class is provided as a property of the WorkitemMB to provide informations about EditorSections 
 * defined in the Model (txtWorkflowEditorID) 
 * 
 * <code>
 *     <c:forEach items="#{workitemMB.editorSections}" var="section">
 *         <ui:include src="/pages/workitems/forms/#{section.url}.xhtml" />
 *         .....
 *         
 *         
 *  other Example:   
 *     
 *      rendered="#{! empty workitemMB.editorSection['prototyp/files']}"
 *      
 *      
 * </code>
 * 
 * @see WorkflowController.getEditorSections
 * @see WorkflowController.getEditorSection
 * 
 * @author rsoika
 *
 */
public class EditorSection {
	String url;
	String name;

	public EditorSection(String url, String name) {
		super();
		this.url = url;
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
