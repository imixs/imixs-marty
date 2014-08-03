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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.marty.plugins.DMSPlugin;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.faces.fileupload.FileUploadController;
import org.imixs.workflow.jee.faces.util.LoginController;

/**
 * This Bean acts a a front controller for the DMS feature. The Bean provides
 * additional properties for attached files and can also manage information
 * about file references to external file servers
 * 
 * 
 * The DmsController observes WorkflowEvents and manages the file uploads during
 * the Processing events.
 * 
 * NOTE: if a plug-in adds a new file (like the reportPlugIn), and the plug-in
 * also updates the $file information of the parent WorkItem, then the DMS
 * property will be updated by the DmsController.
 * 
 * 
 * @see org.imixs.workflow.jee.faces.fileupload.FileUploadController
 * @author rsoika
 * 
 */
@Named("dmsController")
@SessionScoped
public class DmsController implements Serializable {

	@Inject
	protected LoginController loginController = null;

	@Inject
	protected FileUploadController fileUploadController;

	@Inject
	protected WorkflowController workflowController;

	private static final long serialVersionUID = 1L;

	private List<ItemCollection> dmsList = null;
	private ItemCollection blobWorkitem = null;
	private String link = null;

	@PostConstruct
	public void init() {

		FacesContext ctx = FacesContext.getCurrentInstance();
		String path = ctx.getExternalContext().getRequestContextPath();

		fileUploadController.setRestServiceURI(path
				+ "/rest-service/workflow/workitem/");

	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public void setFileUploadController(FileUploadController fleUploadController) {
		this.fileUploadController = fleUploadController;

		init();
	}

	/**
	 * WorkflowEvent listener to update the DMS property if a WorkItem has
	 * changed, processed or saved.
	 * 
	 * Newly attached files will be transfered into the BlobWorkitem. The
	 * BlobWorkitem will be saved before the workItem is processed.
	 * 
	 * The read and write access for a BlobWorkitem will be updated by the
	 * org.imixs.marty.plugins.BlobPlugin.
	 * 
	 * The DMSController also updates the file Properties after a workItem was
	 * processed. This is because a plug-in can add a new file (like the
	 * reportPlugIn), and so the plug-in also updates the $file information of
	 * the parent WorkItem. For that reason the DMS property will be updated by
	 * the DmsController on the WorkflowEvent WORKITEM_AFTER_PROCESS
	 * 
	 * @param workflowEvent
	 * @throws AccessDeniedException
	 */
	public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent)
			throws AccessDeniedException {
		if (workflowEvent == null)
			return;

		// skip if not a workItem...
		if (workflowEvent.getWorkitem() != null
				&& !workflowEvent.getWorkitem().getItemValueString("type")
						.startsWith("workitem"))
			return;

		int eventType = workflowEvent.getEventType();

		if (WorkflowEvent.WORKITEM_BEFORE_PROCESS == eventType
				|| WorkflowEvent.WORKITEM_BEFORE_SAVE == eventType) {

			// load/create the blobWorkitem.....
			blobWorkitem = workflowController.loadBlobWorkitem(workflowEvent
					.getWorkitem());

			// if the BlobWorkitem was just created than we did not have yet set
			// the field $BlobWorkitem
			// so we update this property here
			workflowEvent.getWorkitem().replaceItemValue("$BlobWorkitem",
					blobWorkitem.getItemValueString(EntityService.UNIQUEID));

			// add new attachmetns
			fileUploadController.updateWorkitem(blobWorkitem, false);
			// save blob workitem for further processing through plugins
			blobWorkitem = workflowController.saveBlobWorkitem(blobWorkitem,
					workflowEvent.getWorkitem());

			// update the file info for the current workitem
			fileUploadController.updateWorkitem(workflowEvent.getWorkitem(),
					true);

			// store the dms list
			DMSPlugin.setDmsList(workflowEvent.getWorkitem(), dmsList,
					loginController.getUserPrincipal(), "");

		}

		// if workItem has changed, then update the dms list
		if (WorkflowEvent.WORKITEM_CHANGED == eventType
				|| WorkflowEvent.WORKITEM_AFTER_PROCESS == eventType
				|| WorkflowEvent.WORKITEM_AFTER_SAVE == eventType) {
			fileUploadController.doClear(null);
			if (workflowEvent.getWorkitem() != null) {
				fileUploadController.setWorkitemID(workflowEvent.getWorkitem()
						.getItemValueString("$BlobWorkitem"));
				fileUploadController.setAttachedFiles(workflowEvent
						.getWorkitem().getFileNames());
			}
			// reset blobWorkitem
			blobWorkitem = null;
			// load dms list

			dmsList = DMSPlugin.getDmsList(workflowEvent.getWorkitem());
		}

	}

	/**
	 * this method returns a list of all attached files and the file meta data
	 * provided in a list of ItemCollection.
	 * 
	 * @return - list of file meta data objects
	 */
	public List<ItemCollection> getDmsList() {
		if (dmsList == null)
			dmsList = new ArrayList<ItemCollection>();
		return dmsList;

	}

	/**
	 * This method removes a file form the current dms list and also from the
	 * workitem
	 * 
	 * @param aFile
	 */
	public void removeFile(String aFile) {

		// remove file from dms list
		for (ItemCollection aEntry : dmsList) {
			if (aFile.equals(aEntry.getItemValueString("txtname"))) {
				dmsList.remove(aEntry);
				break;
			}
		}

		// now remove the entry also from the $file property
		fileUploadController.removeAttachmentAction(aFile);
		workflowController.getWorkitem().removeFile(aFile);

	}

	/**
	 * This Method adds a new Link (url) into the DMS list.
	 * 
	 * @param event
	 */
	public void addLink(ActionEvent event) {
		String sLink = getLink();

		if (sLink != null && !"".equals(sLink)) {

			// test for protocoll
			if (!sLink.contains("://"))
				sLink = "http://" + sLink;

			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext externalContext = context.getExternalContext();
			String remoteUser = externalContext.getRemoteUser();

			ItemCollection itemCol = new ItemCollection();
			itemCol.replaceItemValue("url", sLink);

			itemCol.replaceItemValue("$created", new Date());
			itemCol.replaceItemValue("$modified", new Date());
			itemCol.replaceItemValue("namCreator", remoteUser);
			itemCol.replaceItemValue("txtName", sLink);
			dmsList.add(itemCol);

			DMSPlugin.setDmsList(workflowController.getWorkitem(), dmsList);

			// clear link
			setLink("");

		}

	}

}
