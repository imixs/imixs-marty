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

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.faces.fileupload.FileUploadController;

/**
 * This Bean acts a a front controller for the DMS feature. The Bean provides a
 * comment feature for files
 * 
 * 
 * 
 * The DmsController observes WorkflowEvents and manages the file uploads
 * 
 * 
 * @see org.imixs.workflow.jee.faces.fileupload.FileUploadController
 * @author rsoika
 * 
 */
@Named("dmsController")
@SessionScoped
public class DmsController implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private FileUploadController fileUploadController;

	@Inject
	private WorkflowController workflowController;

	@PostConstruct
	public void init() {
		
		FacesContext ctx = FacesContext.getCurrentInstance();
		String path = ctx.getExternalContext().getRequestContextPath();
		
		fileUploadController
				.setRestServiceURI(path+"/RestService/workflow/workitem/");

	}

	public void setFileUploadController(FileUploadController fleUploadController) {
		this.fileUploadController = fleUploadController;

		// update restService URL
		fleUploadController
				.setRestServiceURI("/office/rest/workflow/workitem/");
	}

	public void setWorkflowController(WorkflowController workflowController) {
		this.workflowController = workflowController;
	}

	/**
	 * WorkflowEvent listener
	 * 
	 * @param workflowEvent
	 * @throws AccessDeniedException
	 */
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent)
			throws AccessDeniedException {
		if (workflowEvent == null)
			return;

		if (WorkflowEvent.WORKITEM_CHANGED == workflowEvent.getEventType()) {
			fileUploadController.doClear(null);
			if (workflowEvent.getWorkitem() != null) {
				fileUploadController.setWorkitemID(workflowEvent.getWorkitem()
						.getItemValueString("$BlobWorkitem"));
				fileUploadController.setAttachedFiles(workflowEvent
						.getWorkitem().getFileNames());
			}
		}

		if (WorkflowEvent.WORKITEM_BEFORE_PROCESS == workflowEvent
				.getEventType()) {
			if (fileUploadController.isDirty()) {
				// test if workItem has the property '$BlobWorkitem'
				if (!workflowEvent.getWorkitem().hasItem("$BlobWorkitem")) {
					// create a blob workItem
					ItemCollection blobWorkitem = workflowController
							.loadBlobWorkitem(workflowEvent.getWorkitem());
					// store the $BlobWorkitem
					workflowEvent
							.getWorkitem()
							.replaceItemValue(
									"$BlobWorkitem",
									blobWorkitem
											.getItemValueString(EntityService.UNIQUEID));
					// save the blob workItem (which is still empty)
					workflowController.saveBlobWorkitem(blobWorkitem,
							workflowEvent.getWorkitem());
				}
				// update the file info for the current workitem
				fileUploadController.updateWorkitem(
						workflowEvent.getWorkitem(), true);
			}

		}

		if (WorkflowEvent.WORKITEM_AFTER_PROCESS == workflowEvent
				.getEventType()) {
			if (fileUploadController.isDirty()) {
				// ...save the blobWorkitem after processing the parent!!
				ItemCollection blobWorkitem = workflowController
						.loadBlobWorkitem(workflowEvent.getWorkitem());
				if (blobWorkitem != null) {
					fileUploadController.updateWorkitem(blobWorkitem, false);
					workflowController.saveBlobWorkitem(blobWorkitem,
							workflowEvent.getWorkitem());
				}
			}

			// update the fileuploadController
			fileUploadController.doClear(null);
			fileUploadController.setAttachedFiles(workflowEvent.getWorkitem()
					.getFileNames());

		}

	}
}
