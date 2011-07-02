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
package org.imixs.marty.web.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIParameter;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.imixs.marty.web.profile.MyProfileMB;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.faces.AbstractWorkflowController;

/**
 * Eine Project Note ist eine Workitem mit einer Nachricht innerhalb eines
 * Projektes ProjetNotes sind Responsdokumente zu einem Projekt. In den Feldern
 * namworkflowWriteAccess,namworkflowReadAccess stehen alle Empfänger der
 * Nachricht.
 * 
 * Wen Ein user die Nachricht löschen will wird er aus dem Feldern
 * namworkflowWriteAccess,namworkflowReadAccess gelöscht. Ist der aktuelle
 * Username der letzte in der noch verbleibenden liste wird der datensatz
 * komplet entfernt.
 * 
 * 
 * @author rsoika
 * 
 */
public class ProjectNotesMB extends AbstractWorkflowController {

	public final static int NEW_NOTE = 400;
	// public final static int SEND_NOTE= 10;
	public final static int REMOVE_USER = 80;

	private ArrayList<ItemCollection> notes = null;

	private long lastFetchNotes = -1; // last load of notes
	private final int REFRESH_NOTES = 10; // timeout in seconds

	
	private MyProfileMB myProfileMB = null;
	ProjectMB projectBean = null;
	private ArrayList<ItemCollection> invitations = null;

	public ProjectNotesMB() {
	}

	/**
	 * Returns a instance of the MBProfileMB. This ManagedBean can not be find
	 * during the constructor because the referenece of this bean is queried
	 * form the MyProfielMB itself
	 * 
	 * @return
	 */
	public MyProfileMB getProfileBean() {
		if (myProfileMB == null)
			myProfileMB = (MyProfileMB) FacesContext.getCurrentInstance()
					.getApplication().getELResolver().getValue(
							FacesContext.getCurrentInstance().getELContext(),
							null, "myProfileMB");

		return myProfileMB;
	}

	public ProjectMB getProjectBean() {
		if (projectBean == null)
			projectBean = (ProjectMB) FacesContext.getCurrentInstance()
					.getApplication().getELResolver().getValue(
							FacesContext.getCurrentInstance().getELContext(),
							null, "projectMB");
		return projectBean;
	}

	/**
	 * This method creates an emtpy project instanct. The method sets the
	 * modelversion to the curren user language selection
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public void doCreate(ActionEvent event) throws Exception {

		workitemItemCollection = new ItemCollection();
		workitemItemCollection.replaceItemValue("$processID", NEW_NOTE);

		workitemItemCollection.replaceItemValue("type", "note");

		// determine user language and set Modelversion depending on the
		// selected user locale
		Locale userLocale = FacesContext.getCurrentInstance().getViewRoot()
				.getLocale();
		workitemItemCollection.replaceItemValue("txtModelLanguage", userLocale
				.getLanguage());

		String sModelVersion = this.getProfileBean().getModelVersionHandler()
				.getLatestSystemVersion(userLocale.getLanguage());
		workitemItemCollection.replaceItemValue("$modelversion", sModelVersion);

		// Response zu aktuellem Projekt herstellen
		String sIDRef = getProjectBean().getWorkitem().getItemValueString(
				"$uniqueID");
		workitemItemCollection.replaceItemValue("$UniqueIDRef", sIDRef);

		// project name
		workitemItemCollection.replaceItemValue("txtProjectName",
				getProjectBean().getWorkitem().getItemValue("txtProjectName"));

		// create receivers
		workitemItemCollection.replaceItemValue("namTeam",  getProjectBean().getMemberList());

		
	}

	/**
	 * This method is for saving and processing a single project. The method
	 * generates the attribute 'txtprocesslist' containing a list of
	 * ModelVersions+ProcessIDs. This list will be used to start a new Process
	 * by a Workitem according to this project.
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doProcess(ActionEvent event) throws Exception {
		// Activity ID raussuchen und in activityID speichern
		List children = event.getComponent().getChildren();
		int activityID = -1;

		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) instanceof UIParameter) {
				UIParameter currentParam = (UIParameter) children.get(i);
				if (currentParam.getName().equals("id")
						&& currentParam.getValue() != null) {
					activityID = (Integer) currentParam.getValue();
					break;
				}
			}
		}

		workitemItemCollection.replaceItemValue("$ActivityID",activityID);
		workitemItemCollection = getWorkflowService().processWorkItem(
				workitemItemCollection);
		
		// clear Notes list
		doReset(event);

	}

	/**
	 * This method uses an internel timer-counter to refresh the Notes Selection
	 * after 10 seconds
	 * 
	 * @return
	 */
	public List<ItemCollection> getNotes() {

		long lNow = System.currentTimeMillis() / 1000;
		if (notes == null || lastFetchNotes < lNow - REFRESH_NOTES) {
			loadNotes();
			lastFetchNotes = System.currentTimeMillis() / 1000;
		}

		return notes;

	}

	/**
	 * This method loads all ProjectNotes for the current user.
	 * 
	 * Change: 19.01.2011
	 * The Method read explicit only notes where current user is in $readAccess.
	 * So also for a manager this feature works correctly.
	 * Otherwise managers would see always also 'deleted' notes 
	 */
	private void loadNotes() {
		notes = new ArrayList<ItemCollection>();
		try {
			System.out.println("...Reload Project notes...");
			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext externalContext = context.getExternalContext();
			String remoteUser = externalContext.getRemoteUser();
			// String sName =
			// workitemItemCollection.getItemValueString("txtName");
			String sQuery = "";
			sQuery = "SELECT wi from Entity as wi "
				    + " JOIN wi.readAccess readeraccess "
					+ " JOIN wi.integerItems as p "
					+ " WHERE wi.type = 'note'"
					+ " AND readeraccess.value IN ('"+remoteUser+"')"
					+ " AND p.itemName = '$processid' AND p.itemValue = 410"
					+ " ORDER BY wi.created desc";

			Collection<ItemCollection> col = getEntityService().findAllEntities(
					sQuery, 0, -1);
			// endOfList = col.size() < count;
			for (ItemCollection aworkitem : col) {
				notes.add((aworkitem));
			}
		} catch (Exception ee) {
			notes=null;
			ee.printStackTrace();
		}

	}

	/**
	 * This method removes the current user form the selected Project note.
	 * There for the remote username will be deleted form the field namTeam.
	 * 
	 * It the user is the last name listed in this fields the note will be
	 * deleted completely
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doRemoveUserFromNote(ActionEvent event) throws Exception {

		ItemCollection note = null;

		ItemCollection currentSelection = null;
		// suche selektierte Zeile....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// Zeile gefunden
			currentSelection = (ItemCollection) ((UIData) parent)
					.getRowData();

			note = currentSelection;

			break;

		}

		if (note != null) {
			// now remove username
			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext externalContext = context.getExternalContext();
			String remoteUser = externalContext.getRemoteUser();

			Vector<String> vTeam = note.getItemValue("namTeam");
			while (vTeam.indexOf(remoteUser) > -1) {
				int iPos = vTeam.indexOf(remoteUser);
				vTeam.remove(iPos);
			}

			// remove note completely?
			if (vTeam.size() == 0) {
				getEntityService().remove(note);
			} else {
				// update notes
				note.replaceItemValue("namTeam", vTeam);
				note.replaceItemValue("$ActivityID", REMOVE_USER);
				getWorkflowService().processWorkItem(note);
			}
		}

		// clear Notes list
		doReset(event);
	}

	@Override
	public void doReset(ActionEvent event) {
		
		super.doReset(event);
		notes=null;
		
		
	}
	
	
	

}
