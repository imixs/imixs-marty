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

package org.imixs.marty.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.imixs.marty.team.TeamService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.ModelService;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.openbpmn.bpmn.BPMNModel;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The TeamRestService provides methods to access the marty process and space
 * entities. The Service extends the imixs-workflow-jaxrs api.
 * 
 * Additional the service provides a list of all workflow groups
 * 
 * 
 * @author rsoika
 * 
 */
@Named
@RequestScoped
@Path("/team")
@Produces({ "text/html", "application/xml", "application/json" })
public class TeamRestService implements Serializable {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(TeamRestService.class.getSimpleName());

	@EJB
	DocumentService entityService;

	@EJB
	ModelService modelService;

	@EJB
	TeamService teamService;

	@GET
	@Path("/processlist")
	public XMLDataCollection getProcessList() {
		Collection<ItemCollection> col = null;
		try {
			col = teamService.getProcessList();
			logger.fine("getProcessList - " + col.size() + " entries found");
			return XMLDataCollectionAdapter.getDataCollection(col);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new XMLDataCollection();
	}

	@GET
	@Path("/processlist.xml")
	@Produces(MediaType.TEXT_XML)
	public XMLDataCollection getProcessListXML() {
		Collection<ItemCollection> col = null;
		try {
			col = teamService.getProcessList();
			logger.fine("getProcessList - " + col.size() + " entries found");
			return XMLDataCollectionAdapter.getDataCollection(col);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new XMLDataCollection();
	}

	@GET
	@Path("/processlist.json")
	@Produces(MediaType.APPLICATION_JSON)
	public XMLDataCollection getProcessListJSON() {
		Collection<ItemCollection> col = null;
		try {
			col = teamService.getProcessList();
			return XMLDataCollectionAdapter.getDataCollection(col);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new XMLDataCollection();
	}

	@GET
	@Path("/spaces")
	public XMLDataCollection getSpaces() {
		Collection<ItemCollection> col = null;
		try {
			col = teamService.getSpaces();
			return XMLDataCollectionAdapter.getDataCollection(col);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new XMLDataCollection();
	}

	@GET
	@Path("/spaces.xml")
	@Produces(MediaType.TEXT_XML)
	public XMLDataCollection getSpacesXML() {
		Collection<ItemCollection> col = null;
		try {
			col = teamService.getSpaces();
			return XMLDataCollectionAdapter.getDataCollection(col);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new XMLDataCollection();
	}

	@GET
	@Path("/spaces.json")
	@Produces(MediaType.APPLICATION_JSON)
	public XMLDataCollection getSpacesJSON() {
		Collection<ItemCollection> col = null;
		try {
			col = teamService.getProcessList();
			return XMLDataCollectionAdapter.getDataCollection(col);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new XMLDataCollection();
	}

	/**
	 * Returns a string list of all workflow groups
	 * 
	 * @return
	 * @throws ModelException
	 */
	@GET
	@Path("/workflowgroups.json")
	@Produces(MediaType.APPLICATION_JSON)
	public XMLDataCollection getWorkflowGroupsJSON() throws ModelException {
		List<ItemCollection> col = new ArrayList<ItemCollection>();
		List<String> result = new ArrayList<String>();

		List<String> modelVersions = modelService.getModelManager().getVersions();
		for (String modelVersion : modelVersions) {
			// if not a system model
			if (!modelVersion.startsWith("system")) {
				BPMNModel model = modelService.getModelManager().getModel(modelVersion);
				Set<String> groups = modelService.getModelManager().findAllGroupsByModel(model);

				for (String group : groups) {
					if (!result.contains(group)) {
						result.add(group);
						ItemCollection itemCol = new ItemCollection();
						itemCol.replaceItemValue("$WorkflowGroup", group);
						itemCol.replaceItemValue("txtName", group);
						col.add(itemCol);
					}
				}
			}

		}

		try {
			return XMLDataCollectionAdapter.getDataCollection(col);
		} catch (Exception e) {

			e.printStackTrace();
		}

		return new XMLDataCollection();
	}

}
