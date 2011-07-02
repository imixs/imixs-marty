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
package org.imixs.marty.web.news;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIParameter;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.imixs.marty.model.ModelVersionHandler;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.faces.AbstractWorkflowController;

public class NewsMB extends AbstractWorkflowController {

	private ArrayList<ItemCollection> news = null;
	private int count = 30;
	private int row = 0;
	private boolean endOfList = false;

	public final int NEW_NEWS = 500;
	private ModelVersionHandler modelVersionHandler = null;

	/**
	 * creates a new empty news object
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doCreate(ActionEvent event) throws Exception {
		ItemCollection newsItem = new ItemCollection();
		newsItem.replaceItemValue("type", "news");
		newsItem.replaceItemValue("$processID", new Integer(NEW_NEWS));

		Locale userLocale = FacesContext.getCurrentInstance().getViewRoot()
				.getLocale();

		String sModelVersion = this.getModelVersionHandler()
				.getLatestSystemVersion(userLocale.getLanguage());

		newsItem.replaceItemValue("$ModelVersion", sModelVersion);

		this.setWorkitem(newsItem);

	}

	/**
	 * Selects the current profile
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doDelete(ActionEvent event) throws Exception {
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
			if (currentSelection != null) {
				getEntityService().remove(currentSelection);
				// clear inivtations
				news = null;
			}
			break;
		}

	}

	/**
	 * This method is for saving and processing a profile using the
	 * profileService EJB
	 * 
	 * The method changes the workflow step form 10 to 20 if: $processID=200 &&
	 * keyagb="true"
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

		workitemItemCollection.replaceItemValue("$ActivityID", activityID);
		workitemItemCollection = getWorkflowService().processWorkItem(
				workitemItemCollection);

		// Reset View!
		doReset(event);
	}

	public void doReset(ActionEvent event) {

		news = null;

		row = 0;
	}

	public void doLoadNext(ActionEvent event) {
		row = row + count;
		loadNewsList();

	}

	public void doLoadPrev(ActionEvent event) {
		row = row - count;
		if (row < 0)
			row = 0;
		loadNewsList();
	}

	public List<ItemCollection> getNews() {
		if (news == null)
			loadNewsList();
		return news;

	}

	private void loadNewsList() {
		news = new ArrayList<ItemCollection>();
		try {
			System.out.println("...Reload News...");
			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext externalContext = context.getExternalContext();
			String remoteUser = externalContext.getRemoteUser();
			// String sName =
			// workitemItemCollection.getItemValueString("txtName");
			String sQuery = "";
			sQuery = "SELECT wi from Entity as wi " + " WHERE wi.type = 'news'"
					+ " order by wi.created desc";

			Collection<ItemCollection> col = getEntityService()
					.findAllEntities(sQuery, 0, -1);
			// endOfList = col.size() < count;
			for (ItemCollection aworkitem : col) {
				news.add((aworkitem));
			}
		} catch (Exception ee) {
			news = null;
			ee.printStackTrace();
		}
	}

	public int getRow() {
		return row;
	}

	public boolean isEndOfList() {
		return endOfList;
	}

	/**
	 * this method initializes a new modelVersionHandler. The
	 * modelVersionHandler supports all process Types and Languages available
	 * for the current user.
	 * 
	 * The modelVersionHandler is needed only to compute the latest SystemModel Version
	 * 
	 * 
	 * @return
	 * @throws Exception
	 */
	public ModelVersionHandler getModelVersionHandler() throws Exception {

		// initialize modelVersionHandler the first time...
		if (modelVersionHandler == null) {
			modelVersionHandler = new ModelVersionHandler();

			// add available Models...
			List<String> col = getModelService().getAllModelVersions();
			for (String sversion : col) {
				modelVersionHandler.addVersion(sversion);
			}
		}

		return modelVersionHandler;

	}

	/**
	 * This methode updates the member variable workItem with the attributes
	 * supported by aworkitem
	 * 
	 * @param aworkitem
	 */
	private void updateWorkItem(ItemCollection aworkitem) throws Exception {
		Iterator iter = aworkitem.getAllItems().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry mapEntry = (Map.Entry) iter.next();
			String sName = mapEntry.getKey().toString();
			Object o = mapEntry.getValue();
			workitemItemCollection.replaceItemValue(sName, o);
		}
	}
}
