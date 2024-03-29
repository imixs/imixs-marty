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
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import org.imixs.marty.profile.ProfileService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.xml.XMLDataCollection;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;

/**
 * The UserRestService provides methods to access the marty user profile The
 * Service extends the imixs-workflow-jaxrs api.
 * 
 * 
 * @author rsoika
 * 
 */
@Named("userService")
@RequestScoped
@Path("/user")
@Produces({ "text/html", "application/xml", "application/json" })
public class UserRestService implements Serializable {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(UserRestService.class.getSimpleName());

	@EJB
	DocumentService documentService;

	@EJB
	ProfileService profileService;

	/**
	 * Returns the users favorite workitems.
	 * 
	 * @return
	 */
	@GET
	@Path("/favorites")
	public XMLDataCollection getFavorites(
			@Context HttpServletRequest servletRequest) {
		Collection<ItemCollection> col = null;
		try {

			List<String> favorites = getFavoriteIds(servletRequest);
			if (favorites.size() <= 0)
				return new XMLDataCollection();

			// create IN list
			String inStatement = "(";
			for (String aID : favorites) {
				inStatement = inStatement + "\"$uniqueid:" + aID + "\" OR ";
			}
			// cut last ,
			inStatement = inStatement.substring(0, inStatement.length() - 3) + ")";

			String sQuery="( (type:\"workitem\" OR type:\"workitem\") AND " + inStatement + ")";

			col = documentService.find(sQuery,0,-1);

			return XMLDataCollectionAdapter.getDataCollection(col);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new XMLDataCollection();
	}

	@GET
	@Path("/favorites.xml")
	@Produces(MediaType.TEXT_XML)
	public XMLDataCollection getFavoritesXML(
			@Context HttpServletRequest servletRequest) {
		try {
			return getFavorites(servletRequest);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new XMLDataCollection();
	}

	@GET
	@Path("/favorites.json")
	@Produces(MediaType.APPLICATION_JSON)
	public XMLDataCollection getFavoritesJSON(
			@Context HttpServletRequest servletRequest) {
		try {
			return getFavorites(servletRequest);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new XMLDataCollection();
	}

	/**
	 * Returns a list with all uniqueids stored in the profile favorites
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<String> getFavoriteIds(HttpServletRequest servletRequest) {
		ItemCollection profile=null;
		Principal user = servletRequest.getUserPrincipal();
		String userid = null;
		if (user != null)
			userid = user.getName();

		String sQuery="(type:\"profile\" AND txtname:\"" + userid + "\")";

		Collection<ItemCollection> col;
		try {
			col = documentService.find(sQuery,1,0);
			if (col.size() > 0) {
				profile= col.iterator().next();
			}
		} catch (QueryException e) {
			logger.warning("getFavoriteIds - invalid query: " + e.getMessage());
		}
		if (profile == null)
			return new ArrayList<String>();
		return profile.getItemValue("$WorkitemRef");
	}

	
}
