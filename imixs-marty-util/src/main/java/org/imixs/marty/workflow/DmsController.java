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
import java.util.Map;
import java.util.Vector;

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
import org.imixs.workflow.jee.faces.util.LoginController;

/**
 * This Bean acts a a front controller for the DMS feature. The Bean provides
 * additional properties for attached files and can also manage information
 * about file references to external file servers
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

	private List<ItemCollection> dmsList = null;

	@Inject
	private LoginController loginController = null;

	@Inject
	private FileUploadController fileUploadController;

	@Inject
	private WorkflowController workflowController;

	@PostConstruct
	public void init() {

		FacesContext ctx = FacesContext.getCurrentInstance();
		String path = ctx.getExternalContext().getRequestContextPath();

		fileUploadController.setRestServiceURI(path
				+ "/RestService/workflow/workitem/");

	}

	public void setLoginController(LoginController loginController) {
		this.loginController = loginController;
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
			readDmsList(workflowEvent.getWorkitem());
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
			// store the dms list
			storeDmsList(workflowEvent.getWorkitem());

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
	 * This method creates a new dmsList and reads all existing dms meta data
	 * from the current workItem. The metadata is read from the property 'dms'.
	 * If a file contained in the property '$file' is not part of the property
	 * 'dms' the method will automatically create a new dms entry. The dms
	 * property is saved during processing the workiItem.
	 * 
	 */
	private void readDmsList(ItemCollection workitem) {
		String uniqueIdRef = workitem.getItemValueString("$BlobWorkitem");
		List<Map> vDMS = workitem.getItemValue("dms");
		List<String> files = workitem.getFileNames();
		// build a new filelist and test if each file contained in the $files is
		// listed
		dmsList = new ArrayList<ItemCollection>();
		// first we add all existing dms informations
		for (Map aMetadata : vDMS) {
			dmsList.add(new ItemCollection(aMetadata));
		}
		// add files which where not still part of the dms property.
		updateDmsList(workitem);
	}
	
	
	/**
	 * This method stores the dms meta information into the proeprty 'dms' from a workitem.
	 * 
	 * @param workitem
	 */
	private void storeDmsList(ItemCollection workitem) {
		// first update the dmsList with new file entries
		updateDmsList(workitem);
		// get the current dms value....
		Vector<Map> vDMSnew = new Vector<Map>();

		for (ItemCollection aEntry : dmsList) {
			vDMSnew.add(aEntry.getAllItems());
		}
		workitem.replaceItemValue("dms", vDMSnew);
	}

	/**
	 * This method adds empty dms entries for new uploaded files or filesnames
	 * which are still not contained in the dms list.
	 * 
	 * @param workitem
	 */
	private void updateDmsList(ItemCollection workitem) {

		String uniqueIdRef = workitem.getItemValueString("$BlobWorkitem");
		List<String> files = workitem.getFileNames();

		// now we test for each file entry if a dms meta data entry still
		// exists. If not we create a new one...
		for (String aFilename : files) {

			// test filename already exists
			ItemCollection itemCol = findMetadata(aFilename);
			if (itemCol == null) {

				// no meta data exists.... create a new meta object
				ItemCollection dmsEntry = new ItemCollection();

				dmsEntry.replaceItemValue("txtname", aFilename);
				dmsEntry.replaceItemValue("$uniqueidRef", uniqueIdRef);
				dmsEntry.replaceItemValue("$created", new Date());
				dmsEntry.replaceItemValue("namCreator",
						loginController.getUserPrincipal());

				dmsList.add(dmsEntry);
			}

		}
	}

	/**
	 * This method returns the meta data of a specific file in the exiting
	 * filelist.
	 * 
	 * @return
	 */
	private ItemCollection findMetadata(String aFilename) {

		for (ItemCollection itemCol : dmsList) {
			// test if filename matches...
			String sName = itemCol.getItemValueString("txtName");
			if (sName.equals(aFilename))
				return itemCol;

		}

		// no matching meta data found!
		return null;
	}

}
